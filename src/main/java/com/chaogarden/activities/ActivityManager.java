package com.chaogarden.activities;

import com.chaogarden.core.ChaoState;
import com.chaogarden.entity.ChaoEntity;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.TicketType;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.RelativeMovement;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.EntityLeaveLevelEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/** Server-owned lobbies, physical races, nonlethal karate and durable return-home journals. */
@EventBusSubscriber(modid="chao_garden")
public final class ActivityManager {
  private static final TicketType<BlockPos> TICKET=TicketType.create("chao_activity",Comparator.comparingLong(BlockPos::asLong),120);
  private static final Map<GardenRegistry.Place,Lobby> LOBBIES=new LinkedHashMap<>();
  private static final Map<UUID,Contest> ACTIVE=new HashMap<>();
  private static final Map<GardenRegistry.Place,Long> RECOVERY_TICKETS=new HashMap<>();
  private static final Map<UUID,Integer> RECOVERY_ATTEMPTS=new HashMap<>();
  private static final int MAX_PARTICIPANTS=8;
  private static final String MARKER="ChaoActivity";

  private static final class Lobby {
    GardenRegistry.Place place; UUID owner; String mode; int capacity;
    final LinkedHashSet<UUID> selected=new LinkedHashSet<>(); String status="Choose Chao from your gardens.";
    long touched;
    Contest contest;
    Lobby(GardenRegistry.Place place,UUID owner,String mode,int capacity){this.place=place;this.owner=owner;this.mode=mode;this.capacity=capacity;}
  }
  private static final class Runner {
    UUID id; int waypoint; double stamina, walked; int waterTicks,airTicks,climbTicks;
    boolean finished; double hp,zeal; long attackAt;
    Runner(UUID id){this.id=id;}
  }
  private static final class Contest {
    Lobby lobby; ServerLevel level; List<BlockPos> route=List.of(); List<BlockPos> arena=List.of();
    final LinkedHashMap<UUID,Runner> runners=new LinkedHashMap<>();
    final Map<GardenRegistry.Place,Long> tickets=new HashMap<>();
    final List<UUID> finishOrder=new ArrayList<>(); final ArrayDeque<UUID> bracket=new ArrayDeque<>();
    UUID left,right; long preparedAt,startedAt,boutAt; boolean running;
    Contest(Lobby lobby,ServerLevel level){this.lobby=lobby;this.level=level;this.preparedAt=level.getGameTime();}
  }

  public static boolean isBusy(ChaoEntity chao){return ACTIVE.containsKey(chao.getUUID())||chao.getPersistentData().contains(MARKER);}

  /** Called by the Chao host after its AI phase while contests temporarily own its care tick. */
  public static void tickParticipant(ChaoEntity chao){
    chao.setAirSupply(chao.getMaxAirSupply());
    Contest contest=ACTIVE.get(chao.getUUID());
    if(contest==null)return;
    Runner runner=contest.runners.get(chao.getUUID());
    if(!contest.running||runner==null)return;
    if(contest.lobby.mode.equals("race")&&!runner.finished){
      BlockPos target=contest.route.get(Math.min(runner.waypoint,contest.route.size()-1));
      double dy=target.getY()-chao.getY();
      double speed=ActivityRules.raceSpeed(chao.state(),chao.isInWater(),dy<-.8,dy>.6,runner.stamina);
      chao.getNavigation().setCanFloat(true);
      if(chao.tickCount%8==0)chao.getNavigation().moveTo(target.getX()+.5,target.getY(),target.getZ()+.5,speed);
      if(chao.onGround()&&dy>.4&&chao.horizontalCollision)chao.getJumpControl().jump();
      if(chao.state().levels[ChaoState.FLY]>=10&&chao.getDeltaMovement().y<-.12)
        chao.setDeltaMovement(chao.getDeltaMovement().multiply(1,.72,1));
      if(chao.isInWater()){runner.waterTicks++;chao.setDeltaMovement(chao.getDeltaMovement().add(0,.012,0));}
      if(!chao.onGround()&&!chao.isInWater())runner.airTicks++;
      if(dy>.5)runner.climbTicks++;
    }else if(contest.lobby.mode.equals("karate")&&(runner.id.equals(contest.left)||runner.id.equals(contest.right))){
      UUID rival=runner.id.equals(contest.left)?contest.right:contest.left;
      ChaoEntity other=GardenRegistry.find(contest.level.getServer(),rival);
      if(other!=null&&chao.distanceToSqr(other)>2.4)chao.getNavigation().moveTo(other,1.0+Math.min(.5,chao.state().points[ChaoState.RUN]/3000.0));
      else chao.getNavigation().stop();
    }else chao.getNavigation().stop();
  }

  public static Goal goalFor(ChaoEntity chao){
    return new Goal(){
      {setFlags(EnumSet.of(Flag.MOVE,Flag.LOOK));}
      @Override public boolean canUse(){return isBusy(chao)||needsHome(chao);}
      @Override public boolean canContinueToUse(){return canUse();}
      @Override public boolean requiresUpdateEveryTick(){return true;}
      @Override public void start(){chao.getNavigation().stop();}
      @Override public void tick(){
        if(isBusy(chao))return; // The entity host invokes tickParticipant once after AI.
        if(chao.level() instanceof ServerLevel level){var entry=GardenRegistry.get(level.getServer()).entries.get(chao.getUUID());
          if(entry!=null&&entry.garden.dimension().equals(level.dimension().location().toString())){
            BlockPos home=entry.garden.block();
            if(chao.tickCount%10==0)chao.getNavigation().moveTo(home.getX()+.5,home.getY()+.2,home.getZ()+.5,1.0);
          }
        }
      }
      @Override public void stop(){chao.getNavigation().stop();}
    };
  }

  private static boolean needsHome(ChaoEntity chao){
    if(!(chao.level() instanceof ServerLevel level)||GardenRegistry.managed(chao)||GardenRegistry.constructing(chao))return false;
    var entry=GardenRegistry.get(level.getServer()).entries.get(chao.getUUID());
    if(GardenRegistry.get(level.getServer()).areas.containsKey(entry==null?null:entry.garden))return false;
    return entry!=null&&entry.garden.dimension().equals(level.dimension().location().toString())
        &&chao.distanceToSqr(Vec3.atCenterOf(entry.garden.block()))>12*12;
  }

  public static void callHome(ChaoEntity chao,GardenRegistry.Place place){
    if(!(chao.level() instanceof ServerLevel level)||isBusy(chao)||GardenRegistry.constructing(chao)
        ||com.chaogarden.kindergarten.KindergartenContent.isBoarded(chao))return;
    ServerLevel target=place.level(level.getServer());if(target==null)return;
    target.getChunkAt(place.block()); Vec3 point=safeSpot(target,place.block().offset(2,0,0));
    chao.setStaying(false);
    chao.teleportTo(target,point.x,point.y,point.z,Set.<RelativeMovement>of(),chao.getYRot(),0);
  }

  public static void open(ServerPlayer player,BlockPos clicked){
    try{
      var level=player.serverLevel();var kind=kind(level,clicked);if(kind==null)return;
      BlockPos control=controller(level,clicked);var place=GardenRegistry.Place.at(level,control);
      var data=GardenRegistry.get(player.server);
      if(kind==ActivityBlock.Kind.GARDEN){
        var garden=data.register(player,control);
        if(!garden.owner().equals(player.getUUID())){message(player,"This garden belongs to another keeper.");return;}
        if(com.chaogarden.hub.HubContent.isGardenDimension(level)&&!data.areas.containsKey(place))data.activate(player,place);
        send(player,place,null,true);return;
      }
      if(kind==ActivityBlock.Kind.FLAG||kind==ActivityBlock.Kind.FINISH){message(player,"Use the starting line to enter its race. Hold flags to edit links.");return;}
      List<BlockPos> structure=kind==ActivityBlock.Kind.MAT?mat(level,control):line(level,control,ActivityBlock.Kind.START);
      if(kind==ActivityBlock.Kind.MAT&&structure.size()!=49)throw new IllegalArgumentException("Build one complete 7×7 karate mat.");
      if(kind==ActivityBlock.Kind.START&&(structure.size()<2||structure.size()>8))throw new IllegalArgumentException("Build a straight row of 2–8 starting-line sections.");
      UUID owner=data.nodeOwners.computeIfAbsent(place,p->player.getUUID());data.setDirty();
      Lobby lobby=LOBBIES.computeIfAbsent(place,p->new Lobby(place,owner,kind==ActivityBlock.Kind.MAT?"karate":"race",kind==ActivityBlock.Kind.MAT?8:structure.size()));
      lobby.capacity=kind==ActivityBlock.Kind.MAT?8:structure.size();lobby.touched=level.getGameTime();send(player,place,lobby,true);
    }catch(IllegalArgumentException error){message(player,error.getMessage());}
  }

  public static void handleAction(ServerPlayer player,BlockPos clicked,String action,UUID id){
    if(!Set.of("refresh","toggle","start","cancel","care","activate").contains(action)||!player.isAlive()
        ||player.distanceToSqr(Vec3.atCenterOf(clicked))>144||!player.serverLevel().hasChunkAt(clicked))return;
    var kind=kind(player.serverLevel(),clicked);if(kind!=ActivityBlock.Kind.GARDEN&&kind!=ActivityBlock.Kind.START&&kind!=ActivityBlock.Kind.MAT)return;
    BlockPos control=controller(player.serverLevel(),clicked);var place=GardenRegistry.Place.at(player.serverLevel(),control);
    var data=GardenRegistry.get(player.server);Lobby lobby=LOBBIES.get(place);
    try{
      if(action.equals("care")){
        var entry=id==null?null:data.entries.get(id);
        if(entry==null||!entry.owner.equals(player.getUUID()))return;
        ChaoEntity chao=GardenRegistry.find(player.server,id);
        if(chao==null||chao.level()!=player.level()||player.distanceToSqr(chao)>64||isBusy(chao)||GardenRegistry.managed(chao)||GardenRegistry.constructing(chao)){
          message(player,"Bring that Chao within eight blocks before opening its care chart.");return;
        }
        com.chaogarden.care.ChaoCareNetwork.open(player,chao,4);return;
      }
      if(kind==ActivityBlock.Kind.GARDEN){
        var garden=data.gardens.get(place);if(garden==null||!garden.owner().equals(player.getUUID()))return;
        if(action.equals("activate"))data.activate(player,place);
        if(action.equals("toggle")&&id!=null){
          var entry=data.entries.get(id);
          if(entry!=null&&entry.owner.equals(player.getUUID())&&entry.garden.equals(place)) {
            var chao=GardenRegistry.find(player.server,id);
            if(chao==null||!isBusy(chao)){data.dismiss(id,place);if(chao!=null)chao.setStaying(false);}
          }
        }
        send(player,place,null,false);return;
      }
      if(lobby==null)return;
      lobby.touched=player.serverLevel().getGameTime();
      if(action.equals("toggle")&&id!=null&&lobby.contest==null){
        var entry=data.entries.get(id);if(entry==null||!entry.owner.equals(player.getUUID()))return;
        if(lobby.selected.contains(id))lobby.selected.remove(id);
        else{
          ChaoEntity chao=GardenRegistry.find(player.server,id);
          if(ACTIVE.containsKey(id)||(chao!=null&&(isBusy(chao)||GardenRegistry.managed(chao)||GardenRegistry.constructing(chao)||chao.isCocooned())))throw new IllegalArgumentException("That Chao is busy or accompanying a keeper.");
          if(lobby.selected.size()>=lobby.capacity)throw new IllegalArgumentException("All participant slots are occupied.");
          for(Lobby other:LOBBIES.values())if(other!=lobby&&other.selected.contains(id))throw new IllegalArgumentException("That Chao is already entered in another lobby.");
          lobby.selected.add(id);lobby.status="Waiting for entrants. The host can start with two or more Chao.";
        }
      }else if(action.equals("start")&&lobby.owner.equals(player.getUUID())&&lobby.contest==null)prepare(player,lobby);
      else if(action.equals("cancel")&&lobby.owner.equals(player.getUUID())){
        if(lobby.contest!=null)finish(lobby.contest,"Contest canceled; Chao returned safely.",false);
        else{lobby.selected.clear();lobby.status="Entries cleared.";}
      }
    }catch(IllegalArgumentException error){message(player,error.getMessage());if(lobby!=null)lobby.status=error.getMessage();}
    send(player,place,lobby,false);
  }

  public static void registerCarried(ServerPlayer player,BlockPos clicked,int inventorySlot,int slot,UUID expected) {
    if(!player.isAlive()||player.distanceToSqr(Vec3.atCenterOf(clicked))>144||!player.serverLevel().hasChunkAt(clicked)
        ||!player.serverLevel().getBlockState(clicked).is(ActivityContent.GARDEN.get()))return;
    var place=GardenRegistry.Place.at(player.serverLevel(),controller(player.serverLevel(),clicked));
    var data=GardenRegistry.get(player.server);
    if(!data.registerCarried(player,place,inventorySlot,slot,expected))data.messages.put(place,"Choose your carried Chao and a free slot in an active garden.");
    else data.messages.put(place,"Registered. Your Chao stays in your inventory; release it inside this garden.");
    send(player,place,null,false);
  }

  private static void send(ServerPlayer player,GardenRegistry.Place place,Lobby lobby,boolean open){
    var data=GardenRegistry.get(player.server);var json=new JsonObject();boolean garden=lobby==null;
    json.addProperty("open",open);json.addProperty("mode",garden?"garden":lobby.mode);
    json.addProperty("title",garden?"Chao Garden":lobby.mode.equals("race")?"Chao Race":"Chao Karate");
    json.addProperty("capacity",garden?6:lobby.capacity);json.addProperty("running",!garden&&lobby.contest!=null);
    json.addProperty("active",garden&&data.areas.containsKey(place));
    json.addProperty("status",garden?data.messages.getOrDefault(place,data.areas.containsKey(place)?"Release your Chao inside, or drag a carried Chao into a free slot. Left-click its icon to unregister.":"Activate to mark the surrounding 3 × 3 chunks."):lobby.status);
    json.addProperty("canStart",!garden&&lobby.owner.equals(player.getUUID())&&lobby.contest==null&&lobby.selected.size()>=2);
    json.addProperty("canCancel",!garden&&lobby.owner.equals(player.getUUID()));
    var ids=new LinkedHashSet<UUID>();if(lobby!=null)ids.addAll(lobby.selected);
    data.entries.values().stream().filter(e->e.owner.equals(player.getUUID())&&(!garden||e.garden.equals(place))).limit(64).forEach(e->ids.add(e.id));
    var list=new JsonArray();int count=0;
    for(UUID id:ids){if(count++>=64)break;var entry=data.entries.get(id);if(entry==null)continue;
      ChaoEntity chao=GardenRegistry.find(player.server,id);if(chao!=null&&!isBusy(chao))data.update(chao);
      var row=new JsonObject();var home=data.gardens.get(entry.garden);
      row.addProperty("slot",entry.slot);if(garden)row.addProperty("appearance",entry.appearance);row.addProperty("id",id.toString());row.addProperty("name",shortText(entry.name,64));
      row.addProperty("garden",home==null?"Unregistered garden":home.name()+" · "+entry.garden.dimension());
      row.addProperty("labelstats",entry.stats+(chao==null?" · Unloaded or carried":com.chaogarden.kindergarten.KindergartenContent.isBoarded(chao)?" · In class":GardenRegistry.managed(chao)?" · Companion":""));
      row.addProperty("mine",entry.owner.equals(player.getUUID()));
      row.addProperty("selected",garden?entry.garden.equals(place):lobby.selected.contains(id));
      row.addProperty("available",chao==null?!garden:!isBusy(chao)&&!GardenRegistry.managed(chao)&&!GardenRegistry.constructing(chao)&&!chao.isCocooned());list.add(row);
      row.addProperty("careAvailable",chao!=null&&entry.owner.equals(player.getUUID())&&chao.level()==player.level()
          &&player.distanceToSqr(chao)<=64&&!isBusy(chao)&&!GardenRegistry.managed(chao)&&!GardenRegistry.constructing(chao));
    }
    json.add("entries",list);ActivityNetwork.sendScreen(player,place.block(),json.toString());
  }

  public static BlockPos controller(ServerLevel level,BlockPos pos){
    if(level.getBlockState(pos).is(ActivityContent.GARDEN.get())) return GardenTransporterBlock.root(level,pos);
    ActivityBlock.Kind kind=kind(level,pos);if(kind==ActivityBlock.Kind.MAT){var tiles=mat(level,pos);return tiles.size()==49?tiles.getFirst():pos;}
    if(kind==ActivityBlock.Kind.START||kind==ActivityBlock.Kind.FINISH){var parts=line(level,pos,kind);if(parts.isEmpty())return pos;
      var data=GardenRegistry.get(level.getServer());
      for(BlockPos p:parts)if(data.nodeOwners.containsKey(GardenRegistry.Place.at(level,p)))return p;
      return parts.getFirst();
    }
    return pos;
  }
  public static List<BlockPos> line(ServerLevel level,BlockPos pos,ActivityBlock.Kind kind){
    if(kind(level,pos)!=kind)return List.of();var state=level.getBlockState(pos);Direction.Axis axis=state.getValue(ActivityBlock.AXIS);
    Direction forward=axis==Direction.Axis.X?Direction.EAST:Direction.SOUTH;BlockPos first=pos;
    for(int i=0;i<8&&kind(level,first.relative(forward.getOpposite()))==kind;i++)first=first.relative(forward.getOpposite());
    var out=new ArrayList<BlockPos>();
    for(int i=0;i<9&&kind(level,first)==kind;i++,first=first.relative(forward)){
      if(level.getBlockState(first).getValue(ActivityBlock.AXIS)!=axis)return List.of();
      Direction across=forward.getClockWise();if(kind(level,first.relative(across))==kind||kind(level,first.relative(across.getOpposite()))==kind)return List.of();
      out.add(first.immutable());
    }
    return out.size()<=8?out:List.of();
  }
  public static List<BlockPos> mat(ServerLevel level,BlockPos pos){
    if(kind(level,pos)!=ActivityBlock.Kind.MAT)return List.of();
    var state=level.getBlockState(pos);
    if(state.getValue(KarateMatBlock.TILE)!=KarateMatBlock.LEGACY){
      var center=KarateMatBlock.center(pos,state);var out=new ArrayList<BlockPos>(49);
      // Keep the historical north-west controller and x-major arena order, but
      // resolve only this item's assembly, not a touching neighboring carpet.
      for(int x=-3;x<=3;x++)for(int z=-3;z<=3;z++){
        var at=center.offset(x,0,z);if(!level.hasChunkAt(at))return List.of();
        var tile=level.getBlockState(at);
        if(!tile.is(ActivityContent.MAT.get())||tile.getValue(KarateMatBlock.TILE)!=(z+3)*7+x+3)return List.of();
        out.add(at);
      }
      return out;
    }
    var found=new HashSet<BlockPos>();var queue=new ArrayDeque<BlockPos>();queue.add(pos);
    while(!queue.isEmpty()&&found.size()<=49){BlockPos p=queue.remove();if(!found.add(p))continue;
      for(Direction d:Direction.Plane.HORIZONTAL){
        var next=p.relative(d);var tile=level.getBlockState(next);
        if(tile.is(ActivityContent.MAT.get())&&tile.getValue(KarateMatBlock.TILE)==KarateMatBlock.LEGACY&&!found.contains(next))queue.add(next);
      }
    }
    if(found.size()!=49)return List.of();int minX=found.stream().mapToInt(BlockPos::getX).min().orElse(0),minZ=found.stream().mapToInt(BlockPos::getZ).min().orElse(0);
    var out=new ArrayList<BlockPos>();for(int x=0;x<7;x++)for(int z=0;z<7;z++){BlockPos p=new BlockPos(minX+x,pos.getY(),minZ+z);if(!found.contains(p))return List.of();out.add(p);}
    return out;
  }
  private static ActivityBlock.Kind kind(ServerLevel level,BlockPos pos){return level.getBlockState(pos).getBlock() instanceof ActivityBlock block?block.kind:null;}

  public static BlockPos selectLink(ServerPlayer player,BlockPos clicked){
    BlockPos pos=controller(player.serverLevel(),clicked);var kind=kind(player.serverLevel(),pos);
    if(kind!=ActivityBlock.Kind.START&&kind!=ActivityBlock.Kind.FLAG)return null;
    if(kind==ActivityBlock.Kind.START&&line(player.serverLevel(),pos,kind).size()<2){message(player,"Build a straight starting line of 2–8 sections first.");return null;}
    var place=GardenRegistry.Place.at(player.serverLevel(),pos);var data=GardenRegistry.get(player.server);
    UUID owner=data.nodeOwners.get(place);if(owner!=null&&!owner.equals(player.getUUID())){message(player,"Only the course owner can edit its route.");return null;}
    if(LOBBIES.containsKey(place)&&LOBBIES.get(place).contest!=null)return null;
    data.nodeOwners.put(place,player.getUUID());data.setDirty();return pos;
  }
  public static boolean canConnect(ServerPlayer player,BlockPos from,BlockPos target,boolean fresh){
    var level=player.serverLevel();var data=GardenRegistry.get(player.server);var source=GardenRegistry.Place.at(level,from);var dest=GardenRegistry.Place.at(level,target);
    if(!player.mayBuild()||!player.getUUID().equals(data.nodeOwners.get(source))||!(kind(level,from)==ActivityBlock.Kind.START||kind(level,from)==ActivityBlock.Kind.FLAG))return false;
    if(!ActivityRules.linkLength(target.getX()-from.getX(),target.getY()-from.getY(),target.getZ()-from.getZ())){message(player,"Race links must be at most 32 blocks long.");return false;}
    if(!fresh&&kind(level,target)!=ActivityBlock.Kind.FLAG&&kind(level,target)!=ActivityBlock.Kind.FINISH)return false;
    UUID owner=data.nodeOwners.get(dest);if(owner!=null&&!owner.equals(player.getUUID()))return false;
    GardenRegistry.Place cursor=dest;for(int i=0;i<65&&cursor!=null;i++){if(cursor.equals(source)){message(player,"The race route cannot contain a loop.");return false;}cursor=data.links.get(cursor);}
    for(Lobby lobby:LOBBIES.values())if(lobby.contest!=null&&lobby.place.dimension().equals(source.dimension())){
      if(lobby.place.equals(source)||lobby.contest.route.contains(from)||lobby.contest.route.contains(target)){message(player,"Wait until this course's contest ends before editing it.");return false;}
    }
    return true;
  }
  public static boolean connect(ServerPlayer player,BlockPos from,BlockPos target){
    if(!canConnect(player,from,target,false))return false;var data=GardenRegistry.get(player.server);var a=GardenRegistry.Place.at(player.serverLevel(),from);var b=GardenRegistry.Place.at(player.serverLevel(),target);
    if(kind(player.serverLevel(),target)==ActivityBlock.Kind.FINISH&&line(player.serverLevel(),target,ActivityBlock.Kind.FINISH).size()<2){message(player,"Build a straight ending line of 2–8 sections.");return false;}
    data.nodeOwners.put(b,player.getUUID());data.links.put(a,b);data.setDirty();return true;
  }

  private static List<BlockPos> route(ServerLevel level,BlockPos start){
    var data=GardenRegistry.get(level.getServer());var out=new ArrayList<BlockPos>();var seen=new HashSet<GardenRegistry.Place>();
    var cursor=GardenRegistry.Place.at(level,start);double length=0;
    for(int i=0;i<64;i++){
      if(!seen.add(cursor))throw new IllegalArgumentException("Course contains a loop.");
      var next=data.links.get(cursor);if(next==null||!next.dimension().equals(cursor.dimension()))throw new IllegalArgumentException("Connect every flag through to an ending line.");
      BlockPos a=cursor.block(),b=next.block();if(!ActivityRules.linkLength(b.getX()-a.getX(),b.getY()-a.getY(),b.getZ()-a.getZ()))throw new IllegalArgumentException("A course link exceeds 32 blocks.");
      length+=Math.sqrt(a.distSqr(b));out.add(b);ServerLevel world=next.level(level.getServer());if(world==null)throw new IllegalArgumentException("Missing race dimension.");
      world.getChunkAt(b);
      if(kind(world,b)==ActivityBlock.Kind.FINISH){if(line(world,b,ActivityBlock.Kind.FINISH).size()<2)throw new IllegalArgumentException("Ending line is incomplete.");if(length<8)throw new IllegalArgumentException("Build a course at least eight blocks long.");return out;}
      if(kind(world,b)!=ActivityBlock.Kind.FLAG)throw new IllegalArgumentException("A course flag is missing.");cursor=next;
    }
    throw new IllegalArgumentException("Courses support at most 64 linked sections.");
  }

  private static void prepare(ServerPlayer player,Lobby lobby){
    if(lobby.selected.size()<2||lobby.selected.size()>lobby.capacity)throw new IllegalArgumentException("Enter two or more Chao, within this arena's capacity.");
    var contest=new Contest(lobby,player.serverLevel());
    if(lobby.mode.equals("race")){contest.arena=line(contest.level,lobby.place.block(),ActivityBlock.Kind.START);if(contest.arena.size()<2)throw new IllegalArgumentException("Starting line is incomplete.");contest.route=route(contest.level,lobby.place.block());}
    else{contest.arena=mat(contest.level,lobby.place.block());if(contest.arena.size()!=49)throw new IllegalArgumentException("Karate requires one complete 7×7 mat.");}
    var data=GardenRegistry.get(player.server);
    for(UUID id:lobby.selected){var entry=data.entries.get(id);if(entry==null||player.server.getPlayerList().getPlayer(entry.owner)==null||ACTIVE.containsKey(id))throw new IllegalArgumentException("Each entered Chao needs an online keeper and an available garden record.");
      contest.runners.put(id,new Runner(id));
    }
    for(UUID id:contest.runners.keySet()){ticket(contest,data.entries.get(id).last);ACTIVE.put(id,contest);}
    lobby.contest=contest;lobby.status="Calling Chao from their gardens…";
  }

  private static void begin(Contest contest){
    var server=contest.level.getServer();var data=GardenRegistry.get(server);int index=0;
    for(Runner runner:contest.runners.values()){
      ChaoEntity chao=GardenRegistry.find(server,runner.id);var entry=data.entries.get(runner.id);
      if(chao==null||entry==null||!entry.owner.equals(chao.owner)||GardenRegistry.managed(chao)||GardenRegistry.constructing(chao)||chao.isCocooned()||chao.getPersistentData().contains(MARKER))throw new IllegalArgumentException("A selected Chao is carried, busy or unavailable; no copies were created.");
      var home=new CompoundTag();home.putString("Dimension",chao.level().dimension().location().toString());home.putDouble("X",chao.getX());home.putDouble("Y",chao.getY());home.putDouble("Z",chao.getZ());home.putFloat("Yaw",chao.getYRot());home.putBoolean("NoAi",chao.isNoAi());home.putBoolean("Invulnerable",chao.isInvulnerable());home.putBoolean("Stay",chao.isStaying());home.putBoolean("Sleeping",chao.state().sleeping);
      home.putString("AtDimension",contest.level.dimension().location().toString());home.putLong("AtPos",contest.lobby.place.pos());
      home.put("Arena",contest.lobby.place.tag());
      if(!contest.route.isEmpty())home.putLong("Finish",contest.route.getLast().asLong());
      data.recoveries.put(runner.id,home);
    }
    data.setDirty();server.overworld().getDataStorage().save();
    for(Runner runner:contest.runners.values()){
      ChaoEntity chao=GardenRegistry.find(server,runner.id);chao.getPersistentData().putString(MARKER,contest.lobby.mode);chao.getPersistentData().put("ChaoActivityHome",data.recoveries.get(runner.id).copy());chao.setNoAi(false);chao.setStaying(false);chao.setInvulnerable(true);chao.prepareForActivity();
      Vec3 at=contest.lobby.mode.equals("race")?Vec3.atBottomCenterOf(contest.arena.get(index%contest.arena.size())):
          Vec3.atBottomCenterOf(contest.lobby.place.block()).add(1+(index%4)*1.3,.12,1+(index/4)*3.5);
      ticket(contest,GardenRegistry.Place.at(contest.level,BlockPos.containing(at)));
      if(!chao.teleportTo(contest.level,at.x,at.y+.06,at.z,Set.<RelativeMovement>of(),0,0))throw new IllegalArgumentException("Could not bring a Chao into the arena.");
      runner.stamina=100+Math.min(300,chao.state().points[ChaoState.STAMINA]/12.0);index++;
    }
    if(contest.lobby.mode.equals("race")){setLineOpen(contest.level,contest.arena,true);setLineOpen(contest.level,line(contest.level,contest.route.getLast(),ActivityBlock.Kind.FINISH),true);}
    contest.running=true;contest.startedAt=contest.level.getGameTime();
    if(contest.lobby.mode.equals("karate")){contest.bracket.addAll(contest.runners.keySet());nextBout(contest);}
    contest.lobby.status=contest.lobby.mode.equals("race")?"Race in progress — entrants follow the linked flags.":"Karate tournament in progress — nonlethal bouts.";
  }

  private static void nextBout(Contest contest){
    if(contest.bracket.size()<2){UUID winner=contest.bracket.peek();if(winner!=null)award(contest,contest.runners.get(winner),true);finish(contest,"Karate complete! Champion: "+name(contest,winner),true);return;}
    contest.left=contest.bracket.removeFirst();contest.right=contest.bracket.removeFirst();contest.boutAt=contest.level.getGameTime();
    Vec3 middle=Vec3.atBottomCenterOf(contest.lobby.place.block()).add(3,.12,3);
    int index=0;for(UUID id:List.of(contest.left,contest.right)){
      ChaoEntity chao=GardenRegistry.find(contest.level.getServer(),id);Runner runner=contest.runners.get(id);
      if(chao==null)throw new IllegalArgumentException("A karate entrant went missing.");
      runner.hp=ActivityRules.karateHealth(chao.state());runner.zeal=100;runner.attackAt=contest.boutAt+20;
      chao.teleportTo(middle.x+(index++==0?-1.25:1.25),middle.y,middle.z);chao.setDeltaMovement(Vec3.ZERO);
    }
  }

  private static void tickRace(Contest contest){
    boolean all=true;long now=contest.level.getGameTime();
    for(Runner runner:contest.runners.values()){
      if(runner.finished)continue;all=false;ChaoEntity chao=GardenRegistry.find(contest.level.getServer(),runner.id);
      if(chao==null)throw new IllegalArgumentException("An entrant went missing; returning the remaining Chao.");
      BlockPos target=contest.route.get(runner.waypoint);ticket(contest,GardenRegistry.Place.at(contest.level,target));ticket(contest,GardenRegistry.Place.at(contest.level,chao.blockPosition()));
      runner.stamina=Math.max(0,runner.stamina-.045);runner.walked+=chao.getDeltaMovement().horizontalDistance();
      if(chao.distanceToSqr(Vec3.atBottomCenterOf(target))<2.25){
        runner.waypoint++;
        if(runner.waypoint>=contest.route.size()){
          runner.finished=true;contest.finishOrder.add(runner.id);chao.getNavigation().stop();award(contest,runner,contest.finishOrder.size()==1);
          contest.lobby.status="Finished "+contest.finishOrder.size()+"/"+contest.runners.size()+" · Leader: "+name(contest,contest.finishOrder.getFirst());
        }
      }
    }
    if(all||contest.finishOrder.size()==contest.runners.size())finish(contest,"Race complete! Winner: "+name(contest,contest.finishOrder.getFirst()),true);
    else if(now-contest.startedAt>6000){for(Runner runner:contest.runners.values())if(!runner.finished&&runner.walked>=8)award(contest,runner,false);finish(contest,"Race time expired; active runners keep their training.",false);}
  }

  private static void tickKarate(Contest contest){
    long now=contest.level.getGameTime();ChaoEntity a=GardenRegistry.find(contest.level.getServer(),contest.left),b=GardenRegistry.find(contest.level.getServer(),contest.right);
    if(a==null||b==null)throw new IllegalArgumentException("A karate entrant went missing.");
    Vec3 middle=Vec3.atBottomCenterOf(contest.lobby.place.block()).add(3,.12,3);
    a.getLookControl().setLookAt(b,30,30);b.getLookControl().setLookAt(a,30,30);
    for(var pair:List.of(new ChaoEntity[]{a,b},new ChaoEntity[]{b,a})){
      ChaoEntity attacker=pair[0],defender=pair[1];Runner ar=contest.runners.get(attacker.getUUID()),dr=contest.runners.get(defender.getUUID());
      boolean attackerOut=Math.abs(attacker.getX()-middle.x)>3.25||Math.abs(attacker.getZ()-middle.z)>3.25;
      boolean defenderOut=Math.abs(defender.getX()-middle.x)>3.25||Math.abs(defender.getZ()-middle.z)>3.25;
      if(!ActivityRules.canAttack(ar.hp,dr.hp,attackerOut,defenderOut))break;
      ticket(contest,GardenRegistry.Place.at(contest.level,attacker.blockPosition()));
      ar.zeal=Math.max(0,ar.zeal-Math.max(.004,.025-attacker.state().intelligence/10000.0));
      if(ar.zeal<20){ar.zeal=Math.min(30,ar.zeal+.4);continue;}
      if(now>=ar.attackAt&&attacker.distanceToSqr(defender)<=3.2){
        ar.attackAt=now+ActivityRules.attackInterval(attacker.state());attacker.swing(InteractionHand.MAIN_HAND,true);
        if(contest.level.random.nextDouble()<ActivityRules.evasion(defender.state())){ar.zeal=Math.max(0,ar.zeal-2);continue;}
        dr.hp-=ActivityRules.karateDamage(attacker.state(),defender.state());ar.zeal=Math.min(100,ar.zeal+.8);dr.zeal=Math.max(0,dr.zeal-1);
        Vec3 away=defender.position().subtract(attacker.position()).multiply(1,0,1).normalize();
        double force=.08+Math.min(.18,attacker.state().points[ChaoState.POWER]/16000.0);
        defender.push(away.x*force,.025,away.z*force);defender.hurtMarked=true;
        contest.level.sendParticles(ParticleTypes.CRIT,defender.getX(),defender.getY()+.55,defender.getZ(),3,.12,.1,.12,.02);
      }
    }
    Runner ra=contest.runners.get(contest.left),rb=contest.runners.get(contest.right);
    boolean outA=Math.abs(a.getX()-middle.x)>3.25||Math.abs(a.getZ()-middle.z)>3.25;
    boolean outB=Math.abs(b.getX()-middle.x)>3.25||Math.abs(b.getZ()-middle.z)>3.25;
    if(ra.hp<=0||rb.hp<=0||outA||outB||now-contest.boutAt>=1800){
      UUID winner=(ra.hp<=0||outA)?contest.right:(rb.hp<=0||outB)?contest.left:ra.hp>=rb.hp?contest.left:contest.right;
      UUID loser=winner.equals(contest.left)?contest.right:contest.left;award(contest,contest.runners.get(loser),false);
      contest.bracket.addLast(winner);ChaoEntity defeated=GardenRegistry.find(contest.level.getServer(),loser);
      if(defeated!=null){defeated.teleportTo(middle.x-2,middle.y,middle.z-2);defeated.setDeltaMovement(Vec3.ZERO);}
      contest.lobby.status="Bout winner: "+name(contest,winner);nextBout(contest);
    }
  }

  private static void award(Contest contest,Runner runner,boolean winner){
    if(runner==null)return;ChaoEntity chao=GardenRegistry.find(contest.level.getServer(),runner.id);if(chao==null)return;var state=chao.state();
    if(contest.lobby.mode.equals("race")){
      int amount=12+(winner?12:0)+Math.min(40,(int)(runner.walked/8));state.train(ChaoState.RUN,amount);state.train(ChaoState.STAMINA,8+(winner?4:0));
      if(runner.waterTicks>10)state.train(ChaoState.SWIM,Math.min(40,runner.waterTicks/15));
      if(runner.airTicks>10)state.train(ChaoState.FLY,Math.min(32,runner.airTicks/15));
      if(runner.climbTicks>10)state.train(ChaoState.POWER,Math.min(32,runner.climbTicks/25));
    }else{for(int stat=0;stat<5;stat++)state.train(stat,(stat==ChaoState.POWER||stat==ChaoState.STAMINA?12:6)+(winner?6:0));}
    state.happiness=Math.min(100,state.happiness+(winner?4:2));
    String key=contest.lobby.mode.equals("race")?"RaceExperience":"KarateExperience";var n=chao.getPersistentData();n.putInt(key,ActivityRules.addLifetimeExperience(n.getInt(key),winner?24:12));
    if(winner){
      // Rebirth transfers the lifetime state journal into a fresh entity. Its
      // legacy Forge counter may start at zero; it must not rewind saved wins.
      int prior=Math.max(n.getInt(key+"Wins"),state.intTrait("contest."+contest.lobby.mode+".wins"));
      n.putInt(key+"Wins",Math.min(1_000_000,Math.max(0,Math.min(1_000_000,prior))+1));
    }
    if(winner){
      var prizes=ActivityRewards.recordVictory(state,contest.lobby.mode,n.getInt(key+"Wins"));
      ServerPlayer keeper=chao.owner==null?null:contest.level.getServer().getPlayerList().getPlayer(chao.owner);
      for(String prize:prizes){
        var holder=com.chaogarden.ChaoGarden.CONTENT.get(prize);if(holder==null)throw new IllegalStateException("Missing contest reward "+prize);
        var stack=new net.minecraft.world.item.ItemStack(holder.get());
        if(keeper==null||!keeper.addItem(stack)){
          var dropped=new net.minecraft.world.entity.item.ItemEntity(chao.level(),chao.getX(),chao.getY()+.3,chao.getZ(),stack);
          dropped.setPickUpDelay(40);if(chao.owner!=null)dropped.setTarget(chao.owner);chao.level().addFreshEntity(dropped);
        }
      }
      if(keeper!=null)message(keeper,chao.getName().getString()+" won the "+contest.lobby.mode+"."
          +(prizes.isEmpty()?"":" Garden prizes were added to your inventory."));
    }
    chao.sync();
  }

  private static void finish(Contest contest,String status,boolean completed){
    var server=contest.level.getServer();var data=GardenRegistry.get(server);
    for(UUID id:contest.runners.keySet()){ACTIVE.remove(id);ChaoEntity chao=GardenRegistry.find(server,id);if(chao!=null)restore(chao,data);}
    if(contest.lobby.mode.equals("race")){setLineOpen(contest.level,contest.arena,false);if(!contest.route.isEmpty())setLineOpen(contest.level,line(contest.level,contest.route.getLast(),ActivityBlock.Kind.FINISH),false);}
    for(var place:new ArrayList<>(contest.tickets.keySet()))unticket(server,place);
    contest.tickets.clear();contest.lobby.contest=null;contest.lobby.selected.clear();contest.lobby.status=status;
    for(ServerPlayer player:server.getPlayerList().getPlayers())if(player.serverLevel()==contest.level&&player.distanceToSqr(Vec3.atCenterOf(contest.lobby.place.block()))<4096)message(player,status);
  }

  private static void restore(ChaoEntity chao,GardenRegistry data){
    CompoundTag n=data.recoveries.get(chao.getUUID());
    if(n==null){chao.getPersistentData().remove(MARKER);return;}
    var place=new GardenRegistry.Place(n.getString("Dimension"),BlockPos.containing(n.getDouble("X"),n.getDouble("Y"),n.getDouble("Z")).asLong());
    ServerLevel home=place.level(chao.getServer());if(home==null)return;home.getChunkAt(place.block());
    if(n.contains("Arena")){var arena=GardenRegistry.Place.load(n.getCompound("Arena"));ServerLevel race=arena.level(chao.getServer());if(race!=null){setLineOpen(race,line(race,arena.block(),ActivityBlock.Kind.START),false);if(n.contains("Finish"))setLineOpen(race,line(race,BlockPos.of(n.getLong("Finish")),ActivityBlock.Kind.FINISH),false);}}
    Vec3 safe=safeSpot(home,place.block());
    boolean moved=chao.teleportTo(home,safe.x,safe.y,safe.z,Set.<RelativeMovement>of(),n.getFloat("Yaw"),0);
    ChaoEntity restored=GardenRegistry.find(home.getServer(),chao.getUUID());
    if(moved&&restored!=null){restored.setNoAi(n.getBoolean("NoAi"));restored.setInvulnerable(n.getBoolean("Invulnerable"));restored.setStaying(n.getBoolean("Stay"));restored.state().sleeping=n.getBoolean("Sleeping");restored.sync();restored.getPersistentData().remove(MARKER);restored.getPersistentData().remove("ChaoActivityHome");restored.getNavigation().stop();restored.setDeltaMovement(Vec3.ZERO);data.recoveries.remove(chao.getUUID());RECOVERY_ATTEMPTS.remove(chao.getUUID());data.update(restored);data.setDirty();}
  }

  private static Vec3 safeSpot(ServerLevel level,BlockPos origin){
    for(int y=0;y<5;y++)for(int radius=0;radius<=2;radius++)for(int dx=-radius;dx<=radius;dx++)for(int dz=-radius;dz<=radius;dz++){
      BlockPos p=origin.offset(dx,y,dz);
      if(level.getBlockState(p).getCollisionShape(level,p).isEmpty()&&level.getBlockState(p.above()).getCollisionShape(level,p.above()).isEmpty()
          &&!level.getBlockState(p.below()).getCollisionShape(level,p.below()).isEmpty())return Vec3.atBottomCenterOf(p);
    }
    return Vec3.atBottomCenterOf(origin).add(0,1,0);
  }
  private static String name(Contest contest,UUID id){var chao=id==null?null:GardenRegistry.find(contest.level.getServer(),id);return chao==null?"Chao":shortText(chao.getName().getString(),48);}
  private static String shortText(String text,int points){if(text.codePointCount(0,text.length())<=points)return text;return text.substring(0,text.offsetByCodePoints(0,points));}
  private static void ticket(Contest contest,GardenRegistry.Place place){ServerLevel level=place.level(contest.level.getServer());if(level==null)return;level.getChunkSource().addRegionTicket(TICKET,new ChunkPos(place.block()),2,place.block());contest.tickets.put(place,contest.level.getGameTime());level.getChunkAt(place.block());}
  private static void unticket(MinecraftServer server,GardenRegistry.Place place){ServerLevel level=place.level(server);if(level!=null)level.getChunkSource().removeRegionTicket(TICKET,new ChunkPos(place.block()),2,place.block());}
  private static void message(ServerPlayer player,String text){player.displayClientMessage(Component.literal(text),true);}
  private static void setLineOpen(ServerLevel level,List<BlockPos> tiles,boolean open){for(BlockPos p:tiles){var state=level.getBlockState(p);if(state.getBlock() instanceof ActivityBlock block&&(block.kind==ActivityBlock.Kind.START||block.kind==ActivityBlock.Kind.FINISH))level.setBlock(p,state.setValue(ActivityBlock.OPEN,open),3);}}

  public static void blockRemoved(ServerLevel level,BlockPos pos,ActivityBlock.Kind kind){
    var data=GardenRegistry.get(level.getServer());var place=GardenRegistry.Place.at(level,pos);
    for(Lobby lobby:new ArrayList<>(LOBBIES.values()))if(lobby.contest!=null&&lobby.place.dimension().equals(place.dimension())
        &&(lobby.contest.arena.contains(pos)||lobby.contest.route.contains(pos)))finish(lobby.contest,"Arena or course changed; Chao returned safely.",false);
    if(kind==ActivityBlock.Kind.GARDEN)data.removeGarden(place);
    data.nodeOwners.remove(place);data.links.remove(place);data.links.values().removeIf(place::equals);data.setDirty();
    LOBBIES.remove(place);
  }

  @SubscribeEvent public static void entityTick(EntityTickEvent.Post event){
    if(event.getEntity() instanceof ChaoEntity recovering&&recovering.level() instanceof ServerLevel world&&recovering.tickCount%20==0&&!ACTIVE.containsKey(recovering.getUUID())&&recovering.getPersistentData().contains("ChaoActivityHome")){
      var data=GardenRegistry.get(world.getServer());if(!data.recoveries.containsKey(recovering.getUUID())){data.recoveries.put(recovering.getUUID(),recovering.getPersistentData().getCompound("ChaoActivityHome").copy());data.setDirty();}
    }
    if(event.getEntity() instanceof ChaoEntity chao&&chao.level() instanceof ServerLevel level&&!isBusy(chao)&&!GardenRegistry.managed(chao)){
      var data=GardenRegistry.get(level.getServer());var entry=data.entries.get(chao.getUUID());
      if(!chao.isAlive()){
        // DISCARDED also means safely captured or entering a rebirth egg.
        // Reserve that living Chao's garden slot; only actual death removes it.
        if(chao.state().dead||chao.getRemovalReason()==Entity.RemovalReason.KILLED){
          data.entries.remove(chao.getUUID());data.setDirty();
        }
        return;
      }
      if(chao.tickCount==1 || chao.tickCount%20==0){
        data.detect(chao,chao.tickCount==1);
        entry=data.entries.get(chao.getUUID());
      }
      if(entry!=null&&data.areas.containsKey(entry.garden)){data.contain(chao);if(chao.tickCount%40==0)data.update(chao);return;}
      if(chao.tickCount%40!=0)return;
      if(entry!=null&&(!entry.garden.dimension().equals(level.dimension().location().toString())||chao.distanceToSqr(Vec3.atCenterOf(entry.garden.block()))>32*32))callHome(chao,entry.garden);
      ChaoEntity actual=GardenRegistry.find(level.getServer(),chao.getUUID());if(actual!=null)data.update(actual);
    }
  }
  @SubscribeEvent public static void protect(LivingIncomingDamageEvent event){if(event.getEntity() instanceof ChaoEntity chao&&isBusy(chao))event.setCanceled(true);}
  @SubscribeEvent public static void departed(EntityLeaveLevelEvent event){
    if(event.getEntity() instanceof ChaoEntity chao && event.getLevel() instanceof ServerLevel level)
      GardenRegistry.get(level.getServer()).departed(chao,level);
  }
  @SubscribeEvent public static void serverTick(ServerTickEvent.Post event){
    var server=event.getServer();long now=server.overworld().getGameTime();
    for(Lobby lobby:new ArrayList<>(LOBBIES.values())){
      Contest contest=lobby.contest;
      if(contest==null){
        if(now%20==0){var data=GardenRegistry.get(server);lobby.selected.removeIf(id->{var e=data.entries.get(id);return e==null||server.getPlayerList().getPlayer(e.owner)==null;});
          if(server.getPlayerList().getPlayer(lobby.owner)==null||now-lobby.touched>6000)lobby.selected.clear();}
        continue;
      }
      try{
        if(server.getPlayerList().getPlayer(lobby.owner)==null)throw new IllegalArgumentException("Host disconnected; Chao returned safely.");
        for(UUID id:contest.runners.keySet()){var entry=GardenRegistry.get(server).entries.get(id);if(entry==null||server.getPlayerList().getPlayer(entry.owner)==null)throw new IllegalArgumentException("An entrant's keeper disconnected; Chao returned safely.");}
        if(!contest.running){
          for(UUID id:contest.runners.keySet()){var entry=GardenRegistry.get(server).entries.get(id);if(entry!=null)ticket(contest,entry.last);}
          if(contest.level.getGameTime()-contest.preparedAt>=40)begin(contest);
        }else{
          if(contest.level.getGameTime()%20==0){
            if(lobby.mode.equals("race")&&line(contest.level,lobby.place.block(),ActivityBlock.Kind.START).size()!=contest.arena.size())throw new IllegalArgumentException("Starting line changed.");
            if(lobby.mode.equals("karate")&&mat(contest.level,lobby.place.block()).size()!=49)throw new IllegalArgumentException("Karate mat changed.");
          }
          ticket(contest,lobby.place);
          if(lobby.mode.equals("race"))tickRace(contest);else tickKarate(contest);
          if(contest.level.getGameTime()%20==0){var data=GardenRegistry.get(server);for(UUID id:contest.runners.keySet()){var n=data.recoveries.get(id);var chao=GardenRegistry.find(server,id);if(n!=null&&chao!=null){n.putString("AtDimension",chao.level().dimension().location().toString());n.putLong("AtPos",chao.blockPosition().asLong());data.setDirty();}}if(now%100==0)server.overworld().getDataStorage().save();}
        }
        for(var entry:new ArrayList<>(contest.tickets.entrySet()))if(entry.getValue()<contest.level.getGameTime()-40){unticket(server,entry.getKey());contest.tickets.remove(entry.getKey());}
      }catch(IllegalArgumentException error){finish(contest,error.getMessage(),false);}
    }
    if(now%20==0){
      var data=GardenRegistry.get(server);int loaded=0;
      for(var pair:new ArrayList<>(data.recoveries.entrySet())){
        if(ACTIVE.containsKey(pair.getKey()))continue;
        ChaoEntity chao=GardenRegistry.find(server,pair.getKey());
        if(chao!=null){restore(chao,data);continue;}
        if(loaded++>=8)break;
        int attempts=RECOVERY_ATTEMPTS.merge(pair.getKey(),1,Integer::sum);
        if(attempts>120){data.recoveries.remove(pair.getKey());RECOVERY_ATTEMPTS.remove(pair.getKey());data.setDirty();continue;}
        var entry=data.entries.get(pair.getKey());var n=pair.getValue();
        var places=new ArrayList<GardenRegistry.Place>();if(entry!=null)places.add(entry.last);
        if(n.contains("AtDimension"))places.add(new GardenRegistry.Place(n.getString("AtDimension"),n.getLong("AtPos")));
        for(var at:places){ServerLevel level=at.level(server);if(level!=null){level.getChunkSource().addRegionTicket(TICKET,new ChunkPos(at.block()),2,at.block());level.getChunkAt(at.block());RECOVERY_TICKETS.put(at,now);}}
      }
      for(var pair:new ArrayList<>(RECOVERY_TICKETS.entrySet()))if(pair.getValue()<now-40){unticket(server,pair.getKey());RECOVERY_TICKETS.remove(pair.getKey());}
    }
  }
  @SubscribeEvent public static void stopping(ServerStoppingEvent event){
    for(Lobby lobby:new ArrayList<>(LOBBIES.values()))if(lobby.contest!=null)finish(lobby.contest,"Server stopping; Chao returned.",false);
    for(var place:RECOVERY_TICKETS.keySet())unticket(event.getServer(),place);
    LOBBIES.clear();ACTIVE.clear();RECOVERY_TICKETS.clear();RECOVERY_ATTEMPTS.clear();
  }
  private ActivityManager(){}
}
