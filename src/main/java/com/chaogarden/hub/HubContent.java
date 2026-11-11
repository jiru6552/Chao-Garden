package com.chaogarden.hub;

import com.chaogarden.ChaoGarden;
import com.chaogarden.care.ChaoSchoolBlock;
import com.chaogarden.kindergarten.KindergartenContent;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;
import java.util.UUID;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.portal.DimensionTransition;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.entity.player.UseItemOnBlockEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.level.ExplosionEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Shared Chao World realms. Return journals store identity/location, never creature copies. */
@EventBusSubscriber(modid=ChaoGarden.MODID)
public final class HubContent {
  public static final ResourceKey<Level> HUB=ResourceKey.create(Registries.DIMENSION,id("hub"));
  public static final ResourceKey<Level> KINDERGARTEN=ResourceKey.create(Registries.DIMENSION,id("kindergarten"));
  public static final ResourceKey<Level> GARDEN=ResourceKey.create(Registries.DIMENSION,id("garden"));
  public static final ResourceKey<Level> HERO_GARDEN=ResourceKey.create(Registries.DIMENSION,id("hero_garden"));
  public static final ResourceKey<Level> DARK_GARDEN=ResourceKey.create(Registries.DIMENSION,id("dark_garden"));
  public static final DeferredRegister.Blocks BLOCKS=DeferredRegister.createBlocks(ChaoGarden.MODID);
  public static final DeferredBlock<ChaoTeleporterBlock> TELEPORTER=BLOCKS.register("chao_teleporter",()->
      new ChaoTeleporterBlock(BlockBehaviour.Properties.of().mapColor(MapColor.DIAMOND)
          .strength(2.5f).sound(SoundType.GLASS).noOcclusion().lightLevel(state->9)));
  public static final int ROOM_REVISION=HubRooms.REVISION, PORTAL_COOLDOWN_TICKS=40;
  public static final Vec3 HUB_SPAWN=new Vec3(.5,80.1,.5), KINDERGARTEN_SPAWN=new Vec3(.5,80.1,40.5), GARDEN_SPAWN=new Vec3(5.5,80.1,-12.5);
  private static final String VISIT="ChaoHubReturn", ARMED="ChaoHubPortalArmed", READY="ChaoHubPortalReady";
  private static final Set<ServerLevel> BUILDING=Collections.newSetFromMap(new IdentityHashMap<>());
  private static final Set<UUID> TRANSFERRING=new java.util.HashSet<>();
  private static final ThreadLocal<Set<Player>> GARDEN_USING=ThreadLocal.withInitial(()->Collections.newSetFromMap(new IdentityHashMap<>()));
  private HubContent(){}
  private static ResourceLocation id(String path){return ResourceLocation.fromNamespaceAndPath(ChaoGarden.MODID,path);}
  public static void bootstrap(){ChaoGarden.add("chao_teleporter",()->new BlockItem(TELEPORTER.get(),new Item.Properties()));}
  public static void register(IEventBus bus){BLOCKS.register(bus);}
  public static boolean isHubDimension(Level level){return isHubDimension(level.dimension());}
  public static boolean isHubDimension(ResourceKey<Level> level){return level.equals(HUB)||level.equals(KINDERGARTEN)||isGardenDimension(level);}
  public static boolean isGardenDimension(Level level){return isGardenDimension(level.dimension());}
  public static boolean isGardenDimension(ResourceKey<Level> level){return level.equals(GARDEN)||level.equals(HERO_GARDEN)||level.equals(DARK_GARDEN);}
  public static AABB gardenBounds(ResourceKey<Level> realm){
    if(realm.equals(HERO_GARDEN))return HeroGardenRooms0253.ACTIVE_BOUNDS;
    if(realm.equals(DARK_GARDEN))return DarkGardenRooms0255.ACTIVE_BOUNDS;
    if(realm.equals(GARDEN))return GardenRooms025.ACTIVE_BOUNDS;
    throw new IllegalArgumentException("Not a Chao garden realm: "+realm.location());
  }
  public static boolean insideGarden(ResourceKey<Level> realm,int x,int z){
    if(realm.equals(HERO_GARDEN))return HeroGardenRooms0253.insideField(x,z);
    if(realm.equals(DARK_GARDEN))return DarkGardenRooms0255.insideField(x,z);
    return realm.equals(GARDEN)&&GardenRooms025.insideField(x,z);
  }
  private static boolean gardenBreakable(Level level,BlockPos pos,net.minecraft.world.level.block.state.BlockState state){
    if(level.dimension().equals(HERO_GARDEN))return HeroGardenRooms0253.breakable(pos,state);
    if(level.dimension().equals(DARK_GARDEN))return DarkGardenRooms0255.breakable(pos,state);
    return GardenRooms025.breakable(pos,state);
  }
  private static boolean rescueGardenBody(ServerLevel level,net.minecraft.world.entity.Entity body){
    if(level.dimension().equals(HERO_GARDEN))return HeroGardenRooms0253.rescueBody(level,body);
    if(level.dimension().equals(DARK_GARDEN))return DarkGardenRooms0255.rescueBody(level,body);
    return GardenRooms025.rescueBody(level,body);
  }
  private static BlockPos roomProbe(ResourceKey<Level> realm){
    if(realm.equals(HERO_GARDEN))return BlockPos.containing(HeroGardenRooms0253.SPAWN).below();
    if(realm.equals(DARK_GARDEN))return BlockPos.containing(DarkGardenRooms0255.SPAWN).below();
    return new BlockPos(0,79,0);
  }

  /** The marker commits only after the full structure succeeds. No shared gamerule is changed. */
  public static boolean ensure(ServerLevel level){
    if(!isHubDimension(level))return false;
    var data=HubWorldData.get(level.getServer());String dimension=level.dimension().location().toString();
    if(BUILDING.contains(level))return false;
    var probe=roomProbe(level.dimension());
    if(data.revisions.getOrDefault(dimension,0)<ROOM_REVISION
        ||!level.getBlockState(probe).isFaceSturdy(level,probe,Direction.UP)){
      BUILDING.add(level);
      try{
        HubRooms.ensure(level);
        if(!level.getBlockState(probe).isFaceSturdy(level,probe,Direction.UP))return false;
        data.revisions.put(dimension,ROOM_REVISION);data.setDirty();
      }catch(RuntimeException failure){
        com.mojang.logging.LogUtils.getLogger().error("Could not finish the Chao room; visitor remains in the original world",failure);
        return false;
      }finally{BUILDING.remove(level);}
    }
    try{if(level.dimension().equals(KINDERGARTEN)){
      if(data.revisions.getOrDefault("kindergarten_dormer_windows",0)<1){
        com.chaogarden.kindergarten.KindergartenRooms025.repairDormerWindows(level);
        data.revisions.put("kindergarten_dormer_windows",1);data.setDirty();
      }
      KindergartenContent.ensureServices(level);
    }}
    catch(RuntimeException failure){com.mojang.logging.LogUtils.getLogger().error("Could not prepare Kindergarten services",failure);return false;}
    return true;
  }

  private static long now(ServerPlayer player){return player.server.overworld().getGameTime();}
  private static CompoundTag persisted(ServerPlayer player){
    var root=player.getPersistentData();
    if(!root.contains(Player.PERSISTED_NBT_TAG))root.put(Player.PERSISTED_NBT_TAG,new CompoundTag());
    return root.getCompound(Player.PERSISTED_NBT_TAG);
  }
  private static CompoundTag visit(ServerPlayer player){
    var ledger=HubWorldData.get(player.server);var tag=ledger.visit(player.getUUID());
    if(tag==null&&persisted(player).contains(VISIT)){tag=persisted(player).getCompound(VISIT).copy();ledger.put(player.getUUID(),tag);}
    return tag;
  }
  private static void storeVisit(ServerPlayer player,CompoundTag tag){
    HubWorldData.get(player.server).put(player.getUUID(),tag);persisted(player).put(VISIT,tag.copy());
  }
  private static void clearVisit(ServerPlayer player){HubWorldData.get(player.server).remove(player.getUUID());persisted(player).remove(VISIT);}
  public static boolean hasReturnJournal(ServerPlayer player){return visit(player)!=null;}
  private static CompoundTag captureOrigin(ServerPlayer player){
    var tag=new CompoundTag();tag.putString("Dimension",player.level().dimension().location().toString());
    tag.putDouble("X",player.getX());tag.putDouble("Y",player.getY());tag.putDouble("Z",player.getZ());
    tag.putFloat("Yaw",player.getYRot());tag.putFloat("Pitch",player.getXRot());
    tag.putInt("GameMode",player.gameMode.getGameModeForPlayer().getId());tag.putBoolean("Flying",player.getAbilities().flying);
    return tag;
  }
  private static CompoundTag fallbackOrigin(ServerPlayer player){
    var tag=captureOrigin(player);var level=player.server.overworld();var spawn=level.getSharedSpawnPos();
    tag.putString("Dimension",Level.OVERWORLD.location().toString());tag.putDouble("X",spawn.getX()+.5);
    tag.putDouble("Y",spawn.getY());tag.putDouble("Z",spawn.getZ()+.5);
    if(player.gameMode.getGameModeForPlayer()==GameType.ADVENTURE)tag.putInt("GameMode",player.server.getDefaultGameType().getId());
    return tag;
  }
  private static boolean adventure(ServerPlayer player){
    if(player.gameMode.getGameModeForPlayer()!=GameType.ADVENTURE)player.setGameMode(GameType.ADVENTURE);
    return player.gameMode.getGameModeForPlayer()==GameType.ADVENTURE;
  }
  private static boolean modeFor(ServerPlayer player,ResourceKey<Level> dimension){
    return adventure(player);
  }
  private static Vec3 roomSpawn(ResourceKey<Level> dimension){return dimension.equals(HUB)?HUB_SPAWN:dimension.equals(HERO_GARDEN)?HeroGardenRooms0253.SPAWN:dimension.equals(DARK_GARDEN)?DarkGardenRooms0255.SPAWN:dimension.equals(GARDEN)?GARDEN_SPAWN:KINDERGARTEN_SPAWN;}
  private static boolean outsideRoom(ResourceKey<Level> dimension,Vec3 p){
    if(!Double.isFinite(p.x)||!Double.isFinite(p.y)||!Double.isFinite(p.z))return true;
    boolean garden=isGardenDimension(dimension);
    var bounds=dimension.equals(HUB)?HubRooms.HUB_BOUNDS:garden?gardenBounds(dimension):HubRooms.KINDERGARTEN_BOUNDS;
    if(garden&&!insideGarden(dimension,net.minecraft.util.Mth.floor(p.x),net.minecraft.util.Mth.floor(p.z)))return true;
    return p.x<bounds.minX-2||p.x>bounds.maxX+2||p.z<bounds.minZ-2||p.z>bounds.maxZ+2||p.y<(garden?69:dimension.equals(HUB)?HubRooms.MIN_VISITOR_Y:72)||p.y>(garden?142:bounds.maxY+5);
  }
  private static boolean restoreMode(ServerPlayer player,CompoundTag tag){
    int saved=tag.getInt("GameMode");
    GameType mode=tag.contains("GameMode",3)&&saved>=0&&saved<=3?GameType.byId(saved):player.server.getDefaultGameType();
    if(player.gameMode.getGameModeForPlayer()!=mode)player.setGameMode(mode);
    if(player.gameMode.getGameModeForPlayer()!=mode)return false;
    player.getAbilities().flying=tag.getBoolean("Flying")&&player.getAbilities().mayfly;player.onUpdateAbilities();return true;
  }
  private static void arrived(ServerPlayer player){
    var data=player.getPersistentData();data.putBoolean(ARMED,false);
    long time=now(player);data.putLong(READY,time>Long.MAX_VALUE-PORTAL_COOLDOWN_TICKS?Long.MAX_VALUE:time+PORTAL_COOLDOWN_TICKS);
    player.stopFallFlying();player.setDeltaMovement(Vec3.ZERO);player.fallDistance=0;player.clearFire();
    player.invulnerableTime=Math.max(player.invulnerableTime,20);
  }
  private static boolean transfer(ServerPlayer player,ServerLevel level,Vec3 position,float yaw,float pitch){
    if(!player.containerMenu.getCarried().isEmpty())return false;
    player.closeContainer();
    TRANSFERRING.add(player.getUUID());
    try{
      var result=player.changeDimension(new DimensionTransition(level,position,Vec3.ZERO,yaw,pitch,DimensionTransition.DO_NOTHING));
      return result==player&&player.level()==level;
    }finally{TRANSFERRING.remove(player.getUUID());}
  }

  /** Entry is server-owned; no client-supplied coordinates, mode or inventory is accepted. */
  public static boolean enterHub(ServerPlayer player){
    if(!player.isAlive()||player.isRemoved()||!player.containerMenu.getCarried().isEmpty()||isHubDimension(player.level()))return false;
    var outstanding=visit(player);
    if(outstanding!=null){if(!restoreMode(player,outstanding))return false;clearVisit(player);}
    var destination=player.server.getLevel(HUB);if(destination==null||!ensure(destination))return false;
    // Build present destinations before showing the lobby. Building a garden
    // during a gate crossing stalls the server while the client still predicts
    // movement through the aperture and beyond the lobby's physical edge.
    for(var key:java.util.List.of(KINDERGARTEN,GARDEN,HERO_GARDEN,DARK_GARDEN)){
      var scenic=player.server.getLevel(key);if(scenic!=null&&!ensure(scenic))return false;
    }
    var origin=captureOrigin(player);storeVisit(player,origin);
    if(!adventure(player)){clearVisit(player);return false;}
    if(!transfer(player,destination,HUB_SPAWN,180,0)){
      if(restoreMode(player,origin))clearVisit(player);return false;
    }
    arrived(player);player.displayClientMessage(Component.literal("Welcome to Chao World. The center transporter returns you home."),true);return true;
  }
  public static boolean visitKindergarten(ServerPlayer player){
    if(!player.isAlive()||!player.containerMenu.getCarried().isEmpty()||!player.level().dimension().equals(HUB)||visit(player)==null)return false;
    var destination=player.server.getLevel(KINDERGARTEN);if(destination==null||!ensure(destination)||!adventure(player))return false;
    if(!transfer(player,destination,KINDERGARTEN_SPAWN,180,0))return false;
    arrived(player);return true;
  }
  public static boolean visitGarden(ServerPlayer player){
    if(!player.isAlive()||!player.containerMenu.getCarried().isEmpty()||!player.level().dimension().equals(HUB)||visit(player)==null)return false;
    var destination=player.server.getLevel(GARDEN);if(destination==null||!ensure(destination)||!modeFor(player,GARDEN))return false;
    if(!transfer(player,destination,GARDEN_SPAWN,45,0)){adventure(player);return false;}
    arrived(player);return true;
  }
  public static boolean visitHeroGarden(ServerPlayer player){
    if(!player.isAlive()||!player.containerMenu.getCarried().isEmpty()||!player.level().dimension().equals(HUB)||visit(player)==null)return false;
    var destination=player.server.getLevel(HERO_GARDEN);if(destination==null||!ensure(destination)||!modeFor(player,HERO_GARDEN))return false;
    if(!transfer(player,destination,HeroGardenRooms0253.SPAWN,HeroGardenRooms0253.SPAWN_YAW,0)){adventure(player);return false;}
    arrived(player);return true;
  }
  public static boolean visitDarkGarden(ServerPlayer player){
    if(!player.isAlive()||!player.containerMenu.getCarried().isEmpty()||!player.level().dimension().equals(HUB)||visit(player)==null)return false;
    var destination=player.server.getLevel(DARK_GARDEN);if(destination==null||!ensure(destination)||!modeFor(player,DARK_GARDEN))return false;
    if(!transfer(player,destination,DarkGardenRooms0255.SPAWN,DarkGardenRooms0255.SPAWN_YAW,0)){adventure(player);return false;}
    arrived(player);return true;
  }
  public static boolean returnToHub(ServerPlayer player){
    var realm=player.level().dimension();boolean garden=isGardenDimension(realm),hero=realm.equals(HERO_GARDEN),dark=realm.equals(DARK_GARDEN);
    if(!player.isAlive()||!player.containerMenu.getCarried().isEmpty()||!(garden||player.level().dimension().equals(KINDERGARTEN))||visit(player)==null)return false;
    var destination=player.server.getLevel(HUB);if(destination==null||!ensure(destination)||!adventure(player))return false;
    var target=hero?HubRooms.HERO_RETURN_LANDING:dark?HubRooms.DARK_RETURN_LANDING:garden?new Vec3(.5,80.1,18.5):new Vec3(.5,80.1,-18.5);
    if(!transfer(player,destination,target,hero?HubRooms.HERO_FACING.toYRot():dark?-90:garden?180:0,0)){modeFor(player,realm);return false;}
    arrived(player);return true;
  }

  private static boolean clearLanding(ServerLevel level,ServerPlayer player,Vec3 position){
    if(!Double.isFinite(position.x)||!Double.isFinite(position.y)||!Double.isFinite(position.z)
        ||position.y<level.getMinBuildHeight()+1||position.y>level.getMaxBuildHeight()-3)return false;
    var feet=BlockPos.containing(position);if(!level.getWorldBorder().isWithinBounds(feet))return false;
    level.getChunk(feet);
    var floor=feet.below();var floorState=level.getBlockState(floor);
    if(!floorState.isFaceSturdy(level,floor,Direction.UP)||floorState.is(TELEPORTER.get())||!level.getFluidState(feet).isEmpty()
        ||!level.getFluidState(feet.above()).isEmpty()||floorState.is(Blocks.MAGMA_BLOCK)||floorState.is(Blocks.CAMPFIRE)
        ||floorState.is(Blocks.SOUL_CAMPFIRE)||floorState.is(Blocks.CACTUS))return false;
    for(BlockPos at:BlockPos.betweenClosed(feet,feet.above())){
      var state=level.getBlockState(at);
      if(state.is(TELEPORTER.get())||state.is(Blocks.FIRE)||state.is(Blocks.SOUL_FIRE)||state.is(Blocks.WITHER_ROSE)
          ||state.is(Blocks.SWEET_BERRY_BUSH)||state.is(Blocks.POWDER_SNOW)||state.is(Blocks.NETHER_PORTAL)
          ||state.is(Blocks.END_PORTAL))return false;
    }
    return level.noCollision(player,new AABB(position.x-.31,position.y,position.z-.31,position.x+.31,position.y+1.81,position.z+.31));
  }
  private static Vec3 search(ServerLevel level,ServerPlayer player,Vec3 origin){
    if(!Double.isFinite(origin.x)||!Double.isFinite(origin.y)||!Double.isFinite(origin.z)
        ||Math.abs(origin.x)>29_999_984||Math.abs(origin.z)>29_999_984
        ||origin.y<level.getMinBuildHeight()-8||origin.y>level.getMaxBuildHeight()+8
        ||!level.getWorldBorder().isWithinBounds(BlockPos.containing(origin)))return null;
    if(clearLanding(level,player,origin))return origin;
    var center=BlockPos.containing(origin);
    // Prefer nearby ground on the original level before any overhead roof.
    // Feet belong on the block top, not half a block into a two-high room.
    for(int dy=0;dy<=8;dy++)for(int sign:dy==0?new int[]{1}:new int[]{1,-1})for(int radius=0;radius<=8;radius++){
      int y=center.getY()+dy*sign;
      for(int dx=-radius;dx<=radius;dx++)for(int dz=-radius;dz<=radius;dz++){
        if(Math.max(Math.abs(dx),Math.abs(dz))!=radius)continue;
        var candidate=new Vec3(center.getX()+dx+.5,y,center.getZ()+dz+.5);
        if(clearLanding(level,player,candidate))return candidate;
      }
    }
    return null;
  }
  private static Vec3 emergencyPlatform(ServerLevel level,ServerPlayer player){
    var spawn=level.getSharedSpawnPos();int start=Math.max(level.getMinBuildHeight()+5,Math.min(level.getMaxBuildHeight()-8,spawn.getY()));
    for(int y=start;y<level.getMaxBuildHeight()-4;y++){
      var at=new BlockPos(spawn.getX(),y,spawn.getZ());if(!level.getWorldBorder().isWithinBounds(at))continue;
      boolean clear=true;
      for(BlockPos p:BlockPos.betweenClosed(at.offset(-1,0,-1),at.offset(1,3,1)))if(!level.getWorldBorder().isWithinBounds(p)||!level.getBlockState(p).isAir()){clear=false;break;}
      if(!clear)continue;
      for(BlockPos p:BlockPos.betweenClosed(at.offset(-1,0,-1),at.offset(1,0,1)))level.setBlockAndUpdate(p,Blocks.SMOOTH_QUARTZ.defaultBlockState());
      var target=new Vec3(at.getX()+.5,at.getY()+1,at.getZ()+.5);if(clearLanding(level,player,target))return target;
    }
    return null;
  }
  public static boolean returnHome(ServerPlayer player){
    if(!player.isAlive()||!player.containerMenu.getCarried().isEmpty()||!isHubDimension(player.level()))return false;
    var origin=visit(player);if(origin==null){origin=fallbackOrigin(player);storeVisit(player,origin);}
    var id=ResourceLocation.tryParse(origin.getString("Dimension"));
    var level=id==null?null:player.server.getLevel(ResourceKey.create(Registries.DIMENSION,id));
    if(level!=null&&isHubDimension(level))level=null;
    Vec3 target=level==null?null:search(level,player,new Vec3(origin.getDouble("X"),origin.getDouble("Y"),origin.getDouble("Z")));
    if(target==null){
      level=player.server.overworld();var spawn=level.getSharedSpawnPos();target=search(level,player,new Vec3(spawn.getX()+.5,spawn.getY(),spawn.getZ()+.5));
      if(target==null)target=emergencyPlatform(level,player);
    }
    if(target==null){player.displayClientMessage(Component.literal("The return landing area is blocked. Your return journal is safe; try again shortly."),true);return false;}
    float yaw=origin.getFloat("Yaw"),pitch=origin.getFloat("Pitch");
    yaw=Float.isFinite(yaw)?net.minecraft.util.Mth.wrapDegrees(yaw):0;
    pitch=Float.isFinite(pitch)?Math.max(-90,Math.min(90,pitch)):0;
    if(!transfer(player,level,target,yaw,pitch))return false;
    arrived(player);if(restoreMode(player,origin))clearVisit(player);return true;
  }

  private static boolean onTube(ServerPlayer player){
    for(BlockPos pos:BlockPos.betweenClosed(BlockPos.containing(player.getBoundingBox().minX,player.getBoundingBox().minY,player.getBoundingBox().minZ),
        BlockPos.containing(player.getBoundingBox().maxX,player.getBoundingBox().maxY,player.getBoundingBox().maxZ)))
      if(player.level().getBlockState(pos).is(TELEPORTER.get())
          &&player.getX()>=pos.getX()+.15&&player.getX()<=pos.getX()+.85
          &&player.getZ()>=pos.getZ()+.15&&player.getZ()<=pos.getZ()+.85)return true;
    return false;
  }
  private static boolean inFramedDoor(ServerPlayer player,ResourceKey<Level> realm,BlockPos anchor,int style){
    return inFramedDoor(player,realm,anchor,style,Direction.NORTH);
  }
  private static boolean inFramedDoor(ServerPlayer player,ResourceKey<Level> realm,BlockPos anchor,int style,Direction facing){
    double dx=player.getX()-(anchor.getX()+.5),dz=player.getZ()-(anchor.getZ()+.5);
    double lateral=facing.getAxis()==Direction.Axis.X?dz:dx,depth=facing.getAxis()==Direction.Axis.X?dx:dz;
    return player.level().dimension().equals(realm)&&Math.abs(lateral)<=com.chaogarden.core.PortalGeometry0251.width(style)/2-.15
        &&Math.abs(depth)<=.7&&player.getY()>=anchor.getY()-.5
        &&com.chaogarden.core.PortalGeometry0251.contains(style,lateral,Math.max(0,player.getY()-anchor.getY()));
  }
  public static boolean inKindergartenDoor(ServerPlayer player){return inFramedDoor(player,HUB,PortalScenes0251.HUB_SCHOOL,PortalScenes0251.SCHOOL);}
  public static boolean inReturnDoor(ServerPlayer player){return inFramedDoor(player,KINDERGARTEN,PortalScenes0251.SCHOOL_RETURN,PortalScenes0251.RETURN);}
  public static boolean inGardenDoor(ServerPlayer player){return inFramedDoor(player,HUB,PortalScenes0251.HUB_GARDEN,PortalScenes0251.GARDEN);}
  public static boolean inGardenCave(ServerPlayer player){return inFramedDoor(player,GARDEN,GardenRooms025.EXIT_PORTAL,PortalScenes0251.TUNNEL);}
  public static boolean inHeroDoor(ServerPlayer player){return inFramedDoor(player,HUB,HubRooms.HERO_DOOR,PortalScenes0251.HERO,HubRooms.HERO_FACING);}
  public static boolean inHeroReturn(ServerPlayer player){return inFramedDoor(player,HERO_GARDEN,HeroGardenRooms0253.EXIT_PORTAL,PortalScenes0251.RETURN,HeroGardenRooms0253.EXIT_FACING);}
  public static boolean inDarkDoor(ServerPlayer player){return inFramedDoor(player,HUB,HubRooms.DARK_DOOR,PortalScenes0251.DARK,HubRooms.DARK_FACING);}
  public static boolean inDarkReturn(ServerPlayer player){return inFramedDoor(player,DARK_GARDEN,DarkGardenRooms0255.EXIT_PORTAL,DarkGardenRooms0255.EXIT_STYLE,DarkGardenRooms0255.EXIT_FACING);}
  private static boolean centralExit(ServerPlayer player){return player.level().dimension().equals(HUB)
      &&Math.abs(player.getX()-.5)<.65&&Math.abs(player.getZ()-.5)<.65&&player.getY()>=79.5&&player.getY()<83;}
  private static boolean portalZone(ServerPlayer player){return onTube(player)||centralExit(player)||inKindergartenDoor(player)||inReturnDoor(player)||inGardenDoor(player)||inGardenCave(player)||inHeroDoor(player)||inHeroReturn(player)||inDarkDoor(player)||inDarkReturn(player);}
  public static void tickPlayer(ServerPlayer player){
    if(!player.isAlive()||player.isRemoved()||TRANSFERRING.contains(player.getUUID()))return;
    if(isHubDimension(player.level())){
      if(visit(player)==null)storeVisit(player,fallbackOrigin(player));
      if(!modeFor(player,player.level().dimension()))returnHome(player);
      // Only scenic-room coordinates are playable. Escaping the void never causes a fatal fall.
      if(outsideRoom(player.level().dimension(),player.position())){
        var target=player.serverLevel();if(ensure(target)){
          if(isGardenDimension(target)){if(rescueGardenBody(target,player))arrived(player);return;}
          var spawn=roomSpawn(player.level().dimension());player.teleportTo(target,spawn.x,spawn.y,spawn.z,Set.of(),180,0);arrived(player);
        }
        return;
      }
    }else{
      var origin=visit(player);if(origin!=null&&restoreMode(player,origin))clearVisit(player);
    }
    boolean zone=portalZone(player);var latch=player.getPersistentData();
    if(!zone){if(latch.contains(ARMED))latch.putBoolean(ARMED,true);return;}
    long time=now(player),ready=latch.getLong(READY);
    if(ready<0||ready>time+PORTAL_COOLDOWN_TICKS){ready=time+PORTAL_COOLDOWN_TICKS;latch.putLong(READY,ready);}
    if(latch.contains(ARMED)&&!latch.getBoolean(ARMED)||time<ready)return;
    if(inKindergartenDoor(player))visitKindergarten(player);
    else if(inReturnDoor(player)||inGardenCave(player)||inHeroReturn(player)||inDarkReturn(player))returnToHub(player);
    else if(inGardenDoor(player))visitGarden(player);
    else if(inHeroDoor(player))visitHeroGarden(player);
    else if(inDarkDoor(player))visitDarkGarden(player);
    else if(isHubDimension(player.level()))returnHome(player);
    else enterHub(player);
  }
  @SubscribeEvent public static void serverTick(ServerTickEvent.Post event){
    for(var player:event.getServer().getPlayerList().getPlayers())tickPlayer(player);
    if(event.getServer().overworld().getGameTime()%10==0)for(var key:java.util.List.of(HUB,KINDERGARTEN,GARDEN,HERO_GARDEN,DARK_GARDEN)){
      var level=event.getServer().getLevel(key);if(level==null)continue;
      for(var entity:com.google.common.collect.ImmutableList.copyOf(level.getAllEntities())){
        if(entity instanceof net.minecraft.world.entity.Mob&&!permittedResident(entity)){entity.discard();continue;}
        if(!(entity instanceof com.chaogarden.entity.ChaoEntity||entity instanceof com.chaogarden.entity.ChaoEggEntity
            ||entity instanceof net.minecraft.world.entity.item.ItemEntity)||entity.isRemoved())continue;
        if(outsideRoom(key,entity.position())){
          if(isGardenDimension(key)){rescueGardenBody(level,entity);continue;}
          var spawn=key.equals(HUB)?new Vec3(3.5,80.1,3.5):roomSpawn(key);
          entity.teleportTo(spawn.x,spawn.y,spawn.z);entity.setDeltaMovement(Vec3.ZERO);entity.fallDistance=0;
        }
      }
    }
  }
  @SubscribeEvent public static void login(PlayerEvent.PlayerLoggedInEvent event){
    if(!(event.getEntity() instanceof ServerPlayer player))return;
    if(isHubDimension(player.level())){
      if(visit(player)==null)storeVisit(player,fallbackOrigin(player));
      if(ensure(player.serverLevel())){modeFor(player,player.level().dimension());arrived(player);}else returnHome(player);
    }else{var origin=visit(player);if(origin!=null&&restoreMode(player,origin))clearVisit(player);}
  }
  @SubscribeEvent public static void respawn(PlayerEvent.PlayerRespawnEvent event){
    if(event.getEntity() instanceof ServerPlayer player){
      var origin=visit(player);if(origin!=null&&!isHubDimension(player.level())&&restoreMode(player,origin))clearVisit(player);
      if(isHubDimension(player.level())){modeFor(player,player.level().dimension());arrived(player);}
    }
  }
  @SubscribeEvent public static void clonePlayer(PlayerEvent.Clone event){
    if(event.getEntity() instanceof ServerPlayer player&&event.getOriginal() instanceof ServerPlayer old){
      var origin=visit(old);if(origin!=null)storeVisit(player,origin);
    }
  }
  @SubscribeEvent public static void changedDimension(PlayerEvent.PlayerChangedDimensionEvent event){
    if(!(event.getEntity() instanceof ServerPlayer player)||TRANSFERRING.contains(player.getUUID()))return;
    if(isHubDimension(player.level())){
      if(visit(player)==null)storeVisit(player,fallbackOrigin(player));
      if(ensure(player.serverLevel())){modeFor(player,player.level().dimension());arrived(player);}else returnHome(player);
    }else{var origin=visit(player);if(origin!=null&&restoreMode(player,origin))clearVisit(player);}
  }
  @SubscribeEvent public static void commands(RegisterCommandsEvent event){
    event.getDispatcher().register(Commands.literal("chao").then(Commands.literal("return")
        .executes(context->returnHome(context.getSource().getPlayerOrException())?1:0)));
  }
  private static boolean administrator(Player player){return player.isCreative()&&player.hasPermissions(2);}
  @SubscribeEvent(priority=EventPriority.HIGHEST) public static void breakRoom(BlockEvent.BreakEvent event){
    if(event.getLevel() instanceof Level level&&isHubDimension(level)&&!administrator(event.getPlayer())
        &&!(isGardenDimension(level)&&gardenBreakable(level,event.getPos(),level.getBlockState(event.getPos()))))event.setCanceled(true);
  }
  @SubscribeEvent(priority=EventPriority.HIGHEST) public static void placeRoom(BlockEvent.EntityPlaceEvent event){
    if(event.getLevel() instanceof Level level&&isHubDimension(level)
        &&(!(event.getEntity() instanceof Player p)||!administrator(p))
        &&!(isGardenDimension(level)&&event.getEntity() instanceof Player&&GardenRooms025.placeable(event.getPlacedBlock().getBlock())))event.setCanceled(true);
  }
  @SubscribeEvent(priority=EventPriority.HIGHEST) public static void fluidRoom(BlockEvent.FluidPlaceBlockEvent event){
    if(event.getLevel() instanceof Level level&&isHubDimension(level))event.setCanceled(true);
  }
  @SubscribeEvent(priority=EventPriority.HIGHEST) public static void interactRoom(PlayerInteractEvent.RightClickBlock event){
    if(!isHubDimension(event.getLevel())||administrator(event.getEntity()))return;
    Block block=event.getLevel().getBlockState(event.getPos()).getBlock();
    if(block instanceof ChaoTeleporterBlock||block instanceof ChaoSchoolBlock
        ||block instanceof com.chaogarden.kindergarten.KindergartenDecor025.SchoolDoorBlock)return;
    if(isGardenDimension(event.getLevel())){
      var item=event.getItemStack();
      if(event.getHand()==net.minecraft.world.InteractionHand.MAIN_HAND&&event.getEntity().isShiftKeyDown()
          &&event.getEntity().getMainHandItem().isEmpty()&&event.getEntity().getOffhandItem().isEmpty()
          &&gardenBreakable(event.getLevel(),event.getPos(),event.getLevel().getBlockState(event.getPos()))){
        // Adventure deliberately has no normal digging. Recover a placed garden
        // prop through native harvesting, while all authored terrain stays fixed.
        if(!event.getEntity().canInteractWithBlock(event.getPos(),0)||!event.getLevel().getWorldBorder().isWithinBounds(event.getPos())){
          event.setCanceled(true);event.setCancellationResult(InteractionResult.FAIL);return;
        }
        var recovery=event.getPos();
        if(block instanceof com.chaogarden.activities.GardenTransporterBlock){
          recovery=com.chaogarden.activities.GardenTransporterBlock.root(event.getLevel(),event.getPos());
          var rootState=event.getLevel().getBlockState(recovery);
          if(!rootState.is(block)||!gardenBreakable(event.getLevel(),recovery,rootState)
              ||!com.chaogarden.activities.GardenTransporterBlock.legacy(event.getLevel().getBlockState(event.getPos()))
                  &&(com.chaogarden.activities.GardenTransporterBlock.legacy(rootState)
                      ||rootState.getValue(com.chaogarden.activities.GardenTransporterBlock.PART)!=0
                      ||rootState.getValue(com.chaogarden.activities.GardenTransporterBlock.FACING)!=event.getLevel().getBlockState(event.getPos()).getValue(com.chaogarden.activities.GardenTransporterBlock.FACING))
              ||!event.getLevel().getWorldBorder().isWithinBounds(recovery)){
            event.setCanceled(true);event.setCancellationResult(InteractionResult.FAIL);return;
          }
        }
        final var harvest=recovery;
        boolean recovered=event.getLevel().isClientSide||event.getEntity() instanceof ServerPlayer keeper
            &&gardenPermission(keeper,()->keeper.gameMode.destroyBlock(harvest));
        event.setCanceled(true);event.setCancellationResult(recovered?InteractionResult.sidedSuccess(event.getLevel().isClientSide):InteractionResult.FAIL);return;
      }
      if(GardenRooms025.placeable(block)||block instanceof LobbyDecor025.PalmTrunk)return;
      if(item.getItem() instanceof BlockItem placed){if(GardenRooms025.placeable(placed.getBlock()))return;}
      else if(net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(item.getItem()).getNamespace().equals(ChaoGarden.MODID))return;
    }
    event.setCanceled(true);event.setCancellationResult(InteractionResult.FAIL);
  }
  private static <T> T gardenPermission(Player player,java.util.function.Supplier<T> action){
    boolean previous=player.getAbilities().mayBuild;
    try{player.getAbilities().mayBuild=true;return action.get();}
    finally{player.getAbilities().mayBuild=previous;}
  }
  /** The item phase runs after ordinary block interaction and other cancellation
   * hooks. Permission exists only for this synchronous, whitelisted native use;
   * it never changes game mode, sends build abilities or edits item components. */
  @SubscribeEvent(priority=EventPriority.LOWEST) public static void gardenAdventureUse(UseItemOnBlockEvent event){
    var player=event.getPlayer();var stack=event.getItemStack();
    if(event.getUsePhase()!=UseItemOnBlockEvent.UsePhase.ITEM_AFTER_BLOCK||player==null||player.isSpectator()
        ||player.getAbilities().mayBuild||!isGardenDimension(event.getLevel())||stack.isEmpty())return;
    var item=stack.getItem();
    boolean gardenBell=item instanceof com.chaogarden.activities.GardenBellItem
        &&event.getLevel().getBlockState(event.getPos()).is(com.chaogarden.activities.ActivityContent.GARDEN.get());
    if(!(item instanceof com.chaogarden.item.ChaoEggItem||item instanceof com.chaogarden.item.ChaoCarrierItem
        ||gardenBell||item instanceof BlockItem placed&&GardenRooms025.placeable(placed.getBlock())))return;
    // Linking an existing owned provider is a data-only recovery action. Older
    // saves can legitimately keep it outside the new compact field until the
    // lossless scenery migration succeeds; this grants no outside placement.
    boolean ownedLegacyBell=false;
    if(gardenBell&&event.getLevel().dimension().equals(GARDEN)&&GardenRooms025.BOUNDS.contains(Vec3.atCenterOf(event.getPos()))){
      if(player instanceof ServerPlayer keeper){
        var registry=com.chaogarden.activities.GardenRegistry.get(keeper.server);
        var claim=registry.gardens.get(com.chaogarden.activities.GardenRegistry.Place.at(keeper.serverLevel(),event.getPos()));
        ownedLegacyBell=claim!=null&&claim.owner().equals(keeper.getUUID());
      }else ownedLegacyBell=true; // Ordinary client prediction; the server checks ownership.
    }
    if(!player.canInteractWithBlock(event.getPos(),0)||!event.getLevel().getWorldBorder().isWithinBounds(event.getPos())
        ||(!ownedLegacyBell&&(!gardenBounds(event.getLevel().dimension()).contains(Vec3.atCenterOf(event.getPos()))
            ||!insideGarden(event.getLevel().dimension(),event.getPos().getX(),event.getPos().getZ())))||player.getCooldowns().isOnCooldown(item))return;
    var using=GARDEN_USING.get();if(!using.add(player))return;
    InteractionResult result;
    try{result=gardenPermission(player,()->stack.useOn(event.getUseOnContext()));}
    finally{using.remove(player);if(using.isEmpty())GARDEN_USING.remove();}
    event.cancelWithResult(switch(result){
      case SUCCESS,SUCCESS_NO_ITEM_USED->net.minecraft.world.ItemInteractionResult.SUCCESS;
      case CONSUME->net.minecraft.world.ItemInteractionResult.CONSUME;
      case CONSUME_PARTIAL->net.minecraft.world.ItemInteractionResult.CONSUME_PARTIAL;
      case FAIL->net.minecraft.world.ItemInteractionResult.FAIL;
      case PASS->net.minecraft.world.ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    });
  }
  @SubscribeEvent(priority=EventPriority.HIGHEST) public static void gardenBuckets(PlayerInteractEvent.RightClickItem event){
    if(!isGardenDimension(event.getLevel())||administrator(event.getEntity()))return;
    var item=event.getItemStack().getItem();
    if(item instanceof net.minecraft.world.item.BucketItem||item instanceof net.minecraft.world.item.FlintAndSteelItem||item instanceof net.minecraft.world.item.FireChargeItem){event.setCanceled(true);event.setCancellationResult(InteractionResult.FAIL);}
  }
  @SubscribeEvent(priority=EventPriority.HIGHEST) public static void attackRoom(AttackEntityEvent event){
    if(isHubDimension(event.getEntity().level())&&!administrator(event.getEntity()))event.setCanceled(true);
  }
  @SubscribeEvent(priority=EventPriority.HIGHEST) public static void damageRoom(LivingIncomingDamageEvent event){
    if(isHubDimension(event.getEntity().level())&&!event.getSource().is(DamageTypeTags.BYPASSES_INVULNERABILITY))event.setCanceled(true);
  }
  @SubscribeEvent(priority=EventPriority.HIGHEST) public static void hostileRoom(EntityJoinLevelEvent event){
    // The server admits wildlife/residents. A client's spawn packet arrives
    // before tracked RESIDENT data, so it must not reject that unsynced actor.
    if(!event.getLevel().isClientSide()&&isHubDimension(event.getLevel())&&event.getEntity() instanceof net.minecraft.world.entity.Mob&&!permittedResident(event.getEntity()))event.setCanceled(true);
  }
  private static boolean permittedResident(net.minecraft.world.entity.Entity entity){
    return entity instanceof com.chaogarden.entity.ChaoEntity||entity instanceof com.chaogarden.kindergarten.KindergartenNpc
        ||entity instanceof com.chaogarden.market.BlackMarketTrader trader&&trader.resident();
  }
  @SubscribeEvent(priority=EventPriority.HIGHEST) public static void explosionRoom(ExplosionEvent.Detonate event){
    if(isHubDimension(event.getLevel())){event.getAffectedBlocks().clear();event.getAffectedEntities().clear();}
  }
  @SubscribeEvent public static void stop(ServerStoppingEvent event){BUILDING.clear();TRANSFERRING.clear();}
}
