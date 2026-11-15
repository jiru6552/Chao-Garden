package com.chaogarden.client;

import com.chaogarden.ChaoGarden;
import com.chaogarden.core.*;
import com.chaogarden.entity.ChaoEntity;
import com.chaogarden.toys.*;
import com.google.gson.*;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.logging.LogUtils;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.MessageDigest;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicIntegerArray;
import net.minecraft.client.*;
import net.minecraft.client.gui.screens.AccessibilityOnboardingScreen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.*;
import net.neoforged.neoforge.client.model.IQuadTransformer;
import net.neoforged.neoforge.client.model.data.ModelData;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/** Development-only actual actors, world light and production toy models/clocks.
 * No alternate animation or renderer; excluded from the installable JAR. */
@EventBusSubscriber(modid=ChaoGarden.MODID,value=Dist.CLIENT)
public final class ToyVisualSmoke080 {
  private record Case(int id,String name,String toy,String context,String form,boolean dark,
      Direction startSide,String interrupt,boolean gear) {
    String kind(){return "chao_"+toy;}
    boolean natural(){return context.equals("natural");}
    boolean loop(){return context.equals("loop");}
    boolean floor(){return context.equals("placed");}
    boolean jack(){return toy.equals("jack_in_box");}
    boolean preview(){return !natural()&&!floor();}
    int end(){return loop()?280:natural()?244:190;}
  }
  private record Shot(int tick,String label,int angle) {}
  private record SoundRequest(int index,long elapsed,String id) {}
  private static final String MODE=System.getenv().getOrDefault("CHAO_TOYS080_MODE","jack");
  private static final int APPROACH_TIMEOUT=Integer.parseInt(System.getenv().getOrDefault("CHAO_TOYS080_APPROACH_TIMEOUT","1000"));
  private static final Path OUTPUT=Path.of(System.getenv().getOrDefault("CHAO_TOYS080_OUTPUT","/workspace/.setup/toy080-"+MODE)).toAbsolutePath().normalize();
  private static final Gson GSON=new GsonBuilder().setPrettyPrinting().create();
  private static final BlockPos TOY=new BlockPos(0,80,2);
  private static final List<String> VIEWS=List.of("front","side","back","above");
  private static final List<Case> CASES=cases();
  private static final AtomicInteger SAVED=new AtomicInteger();
  private static final AtomicIntegerArray REQUESTED=new AtomicIntegerArray(CASES.size()),SOUNDS=new AtomicIntegerArray(CASES.size());
  private static final Map<net.minecraft.client.resources.sounds.SoundInstance,SoundRequest> SOUND_REQUESTS=Collections.synchronizedMap(new IdentityHashMap<>());
  private static final Map<String,JsonObject> QUAD_PROOFS=new HashMap<>();
  private static volatile int phase=-1;
  private static volatile UUID actorId;
  private static volatile long elapsed,caseAge;
  private static volatile boolean armed,complete,settled;
  private static volatile String fatal;
  private static volatile double walked;
  private static ChaoEntity actor;
  private static ArmorStand observer;
  private static Vec3 previousPosition;
  private static long spawned,began,lastTick,wallStart;
  private static String savedState;
  private static Map<EquipmentSlot,ItemStack> savedGear;
  private static boolean stopped,interrupted,started,setup,finished,renderedFrame,worldFrame,armRequested;
  private static int clientPhase=-1,pending=-1,shotIndex,viewFrames,actorFrames,worldFrames,shots,completedCases;
  private static JsonObject observedProof;

  private static boolean enabled(){return "true".equalsIgnoreCase(System.getenv("CHAO_TOYS080_SMOKE"));}
  private static List<Case> cases(){
    var all=new ArrayList<Case>();
    if(MODE.equals("jack")){
      all.add(new Case(0,"jack_baby_neutral_front","jack_in_box","natural","baby_neutral",false,Direction.SOUTH,"",false));
      all.add(new Case(1,"jack_hero_power_rear","jack_in_box","natural","adult_hero_power",false,Direction.NORTH,"",false));
      all.add(new Case(2,"jack_dark_run_once_mask_glasses","jack_in_box","once","adult_dark_run",false,Direction.SOUTH,"",true));
      all.add(new Case(3,"jack_hero_baby_loop_stop","jack_in_box","loop","baby_hero",false,Direction.SOUTH,"stop",true));
      all.add(new Case(4,"jack_neutral_swim_damage","jack_in_box","once","adult_neutral_swim",false,Direction.SOUTH,"damage",false));
      all.add(new Case(5,"jack_dark_fly_remove","jack_in_box","once","adult_dark_fly",false,Direction.SOUTH,"remove",false));
    }else if(MODE.equals("lighting")){
      String[] forms={"baby_neutral","adult_hero_power","adult_dark_run"};int n=0,group=0;
      for(String toy:List.of("pogo_stick","toy_car","shadow_doll","sonic_doll"))for(String context:List.of("placed","held")){
        String form=forms[group++%forms.length];
        for(boolean dark:new boolean[]{false,true})all.add(new Case(n++,toy+"_"+context+"_"+(dark?"dark":"day"),toy,context,form,dark,Direction.SOUTH,"",group%2==0));
      }
    }else throw new IllegalArgumentException("Unknown CHAO_TOYS080_MODE "+MODE);
    String selection=System.getenv("CHAO_TOYS080_CASES");if(selection==null||selection.isBlank())return List.copyOf(all);
    var chosen=new ArrayList<Case>();var used=new HashSet<Integer>();
    for(String token:selection.split(",")){int i=Integer.parseInt(token.trim());require(i>=0&&i<all.size()&&used.add(i),"Invalid/duplicate case "+token);chosen.add(all.get(i));}
    return List.copyOf(chosen);
  }
  private static List<Shot> schedule(Case c){
    if(!MODE.equals("jack"))return List.of(new Shot(0,"static_front",0),new Shot(0,"static_side",1),new Shot(0,"static_back",2),new Shot(0,"static_above",3));
    var out=new ArrayList<Shot>();if(c.natural()){out.add(new Shot(-1,"start_front",0));out.add(new Shot(-1,"start_side",1));}out.add(new Shot(20,"watch_front",0));out.add(new Shot(44,"reveal_front",0));
    out.add(new Shot(54,"reaction_side",1));out.add(new Shot(94,"later_front",0));out.add(new Shot(134,"retreat_back",2));out.add(new Shot(150,"closed_above",3));
    if(c.loop())out.add(new Shot(204,"second_cycle_front",0));
    out.add(new Shot(c.loop()?270:c.natural()?230:184,"recovered_front",0));return List.copyOf(out);
  }
  private static void require(boolean okay,String message){if(!okay)throw new IllegalStateException("TOYS080 "+message);}
  private static String sha(byte[] value){try{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value));}catch(Exception e){throw new IllegalStateException(e);}}
  private static String sha(String value){return sha(value.getBytes(StandardCharsets.UTF_8));}
  private static String sha(Path value){try{return sha(Files.readAllBytes(value));}catch(Exception e){throw new IllegalStateException(e);}}
  private static void write(Path path,Object data){try{Files.createDirectories(path.getParent());Files.writeString(path,GSON.toJson(data)+"\n");}catch(Exception e){throw new IllegalStateException(e);}}
  private static void onServer(MinecraftServer server,Runnable work){server.execute(()->{try{work.run();}catch(Throwable e){fatal=e.toString();LogUtils.getLogger().error("TOYS080_ASYNC_FAILURE",e);}});}
  private static ChaoState fixture(Case c){
    var state=new ChaoState();state.adult=c.form().startsWith("adult");state.align=c.form().contains("hero")?1:c.form().contains("dark")?-1:0;
    state.primaryType=c.form().endsWith("power")?ChaoState.POWER_TYPE:c.form().endsWith("run")?ChaoState.RUN_TYPE:c.form().endsWith("swim")?ChaoState.SWIM_TYPE:c.form().endsWith("fly")?ChaoState.FLY_TYPE:ChaoState.NORMAL;
    state.hunger=100;state.energy=100;state.happiness=20;state.boredom=100;state.personality=ChaoPreferences.EASILY_BORED;state.setBaselineFace076(0,0);
    for(int i=0;i<ChaoIllness.COUNT;i++)state.setIllness(i,100);
    state.setEvolutionStrength(state.adult?1.2f:0);
    if(state.adult){state.levels[ChaoState.RUN]=state.primaryType==ChaoState.RUN_TYPE?15:0;state.levels[ChaoState.POWER]=state.primaryType==ChaoState.POWER_TYPE?15:0;state.levels[ChaoState.SWIM]=state.primaryType==ChaoState.SWIM_TYPE?15:0;state.levels[ChaoState.FLY]=state.primaryType==ChaoState.FLY_TYPE?15:0;}
    String encoded=state.encode();var result=ChaoState.decode(encoded);require(encoded.equals(result.encode()),"Fixture codec mismatch");return result;
  }
  private static void gear(ChaoEntity c,Case entry){
    if(!entry.gear())return;
    c.setItemSlot(EquipmentSlot.OFFHAND,new ItemStack(ChaoGarden.item("round_glasses_accessory")));
    if(MODE.equals("jack")&&entry.id()==2)c.setItemSlot(EquipmentSlot.HEAD,new ItemStack(ChaoGarden.item("blue_wool_beanie")));
    else c.setItemSlot(EquipmentSlot.FEET,new ItemStack(ChaoGarden.item("baseball_cap_accessory")));
  }
  private static Map<EquipmentSlot,ItemStack> gearSnapshot(ChaoEntity c){var m=new EnumMap<EquipmentSlot,ItemStack>(EquipmentSlot.class);for(var slot:List.of(EquipmentSlot.HEAD,EquipmentSlot.FEET,EquipmentSlot.OFFHAND,EquipmentSlot.LEGS,EquipmentSlot.CHEST))m.put(slot,c.getItemBySlot(slot).copy());return m;}
  private static boolean sameGear(ChaoEntity c){for(var e:savedGear.entrySet())if(!ItemStack.matches(e.getValue(),c.getItemBySlot(e.getKey())))return false;return true;}
  private static JsonObject gearProof(ChaoEntity c){var data=new JsonObject();for(var slot:List.of(EquipmentSlot.HEAD,EquipmentSlot.FEET,EquipmentSlot.OFFHAND,EquipmentSlot.LEGS,EquipmentSlot.CHEST)){var item=c.getItemBySlot(slot);data.addProperty(slot.name(),item.isEmpty()?"empty":item.save(c.registryAccess()).toString());}return data;}
  private static void room(net.minecraft.server.level.ServerLevel level,boolean dark){
    level.setDayTime(dark?18000:6000);level.setWeatherParameters(0,100000,false,false);
    for(var p:BlockPos.betweenClosed(-8,79,-6,8,89,12)){
      boolean edge=p.getX()==-8||p.getX()==8||p.getZ()==-6||p.getZ()==12||p.getY()==89;
      level.setBlock(p,p.getY()==79||dark&&edge?Blocks.SMOOTH_STONE.defaultBlockState():Blocks.AIR.defaultBlockState(),3);
    }
  }
  private static void next(MinecraftServer server,int index){
    server.tickRateManager().setFrozen(true);var level=server.overworld();
    actorId=null;if(actor!=null){ToyPreview.stop(actor);ToyContent.endPlaying(actor,false);actor.discard();}
    var c=CASES.get(index);room(level,c.dark());
    boolean source=c.jack()||c.floor()||c.toy().equals("pogo_stick");
    if(source)level.setBlock(TOY,ToyContent.block(c.kind()).defaultBlockState().setValue(ChaoToyBlock.FACING,Direction.SOUTH),3);
    actor=ChaoGarden.CHAO.get().create(level);require(actor!=null,"Cannot create actual Chao");
    String identity=MODE+"-"+(MODE.equals("lighting")?c.id()/2:c.id());actor.setUUID(UUID.nameUUIDFromBytes(("toys080-"+identity).getBytes(StandardCharsets.UTF_8)));
    actor.setState(fixture(c));actor.setOriginEggCoat(fixture(c));actor.owner=server.getPlayerList().getPlayers().getFirst().getUUID();actor.setNoAi(true);actor.setInvulnerable(true);actor.deferBehavior(100000);actor.getRandom().setSeed(8000+c.id());
    Vec3 at=Vec3.atBottomCenterOf(TOY).add(c.floor()?3:0,0,c.floor()?3:1);
    if(c.natural())at=Vec3.atBottomCenterOf(TOY.relative(c.startSide(),3));
    float yaw=c.startSide().getOpposite().toYRot();actor.moveTo(at.x,at.y,at.z,yaw,0);actor.yBodyRot=actor.yBodyRotO=actor.yHeadRot=actor.yHeadRotO=yaw;actor.setOnGround(true);gear(actor,c);
    actor.getPersistentData().putLong("ChaoToyNextPlay",Long.MAX_VALUE);require(level.addFreshEntity(actor),"Actor admission failed");
    savedGear=gearSnapshot(actor);savedState="";previousPosition=actor.position();walked=0;
    spawned=lastTick=level.getGameTime();began=-1;caseAge=elapsed=0;armed=complete=settled=stopped=interrupted=false;actorId=actor.getUUID();phase=index;
    var keeper=server.getPlayerList().getPlayers().getFirst();keeper.setGameMode(GameType.SPECTATOR);keeper.getInventory().clearContent();keeper.setNoGravity(true);keeper.teleportTo(6.5,82,8.5);keeper.setDeltaMovement(Vec3.ZERO);
    server.tickRateManager().setFrozen(false);
    LogUtils.getLogger().info("TOYS080_CASE index={} sourceIndex={} name={} mode={} context={} form={} dark={} uuid={} gear={}",index,c.id(),c.name(),MODE,c.context(),c.form(),c.dark(),actorId,GSON.toJson(gearProof(actor)));
  }
  private static void command(MinecraftServer server,String action){
    var c=CASES.get(phase);var keeper=server.getPlayerList().getPlayers().getFirst();keeper.teleportTo(actor.getX()+3,actor.getY()+2,actor.getZ()+3);actor.setOnGround(true);
    String text="chao testtoy "+c.toy()+" "+action;
    try{require(server.getCommands().getDispatcher().execute(text,keeper.createCommandSourceStack().withPermission(2))==1,"Native dispatcher rejected "+text);}catch(com.mojang.brigadier.exceptions.CommandSyntaxException e){throw new IllegalStateException(e);}
    LogUtils.getLogger().info("TOYS080_COMMAND index={} command={} elapsed={}",phase,text,elapsed);
  }
  @SubscribeEvent public static void server(ServerTickEvent.Post event){
    if(!enabled()||phase<0||phase>=CASES.size()||complete)return;
    try{
      var server=event.getServer();var level=server.overworld();long now=level.getGameTime();if(now==lastTick)return;lastTick=now;
      var c=CASES.get(phase);require(actor!=null&&actor.isAlive(),"Actor disappeared");require(sameGear(actor),"Actual equipment changed");
      caseAge=now-spawned;walked+=actor.position().distanceTo(previousPosition);previousPosition=actor.position();
      if(!settled){
        if(caseAge<32)return;require(actor.onGround(),"Actor did not reach supported floor");
        actor.setState(fixture(c));actor.setDeltaMovement(Vec3.ZERO);savedState=actor.state().encode();settled=true;server.tickRateManager().setFrozen(true);
        if(c.floor()){began=now;armed=complete=true;}
        LogUtils.getLogger().info("TOYS080_ARMED index={} realPhysicsTicks=32 preview={} natural={} stateSha256={}",phase,c.preview(),c.natural(),sha(savedState));return;
      }
      if(!armed)return;
      if(c.natural()&&began<0&&ToyContent.isPlaying(actor)){
        began=actor.getPersistentData().getLong("ChaoToyUntil")-ToyInteractionRules.SESSION_TICKS;
        LogUtils.getLogger().info("TOYS080_NATURAL_STARTED index={} walked={} position={} target={}",phase,walked,actor.position(),actor.toyTarget());
      }
      if(began<0){
        if(caseAge%40==0||caseAge>=APPROACH_TIMEOUT)logWaitingApproach(now,caseAge>=APPROACH_TIMEOUT);
        require(caseAge<APPROACH_TIMEOUT,"Natural production goal did not reach toy within "+APPROACH_TIMEOUT+" ticks");return;
      }
      elapsed=now-began;
      if(c.preview()&&ToyPreview.active(actor))require(actor.state().encode().equals(savedState),"Preview changed saved state");
      if(!MODE.equals("jack")){
        if(elapsed>=60){require(actor.toyPlayingKind().equals(c.kind())&&actor.toyPlayingTicks()>0,"Lighting fixture lost actual active prop");complete=true;server.tickRateManager().setFrozen(true);}return;
      }
      if(!interrupted&&!c.interrupt().isEmpty()&&elapsed>=(c.loop()?244:58)){
        interrupted=true;
        if(c.interrupt().equals("stop")){command(server,"stop");stopped=true;}
        else if(c.interrupt().equals("damage")){actor.setInvulnerable(false);require(actor.hurt(level.damageSources().generic(),1),"Damage interruption did not occur");}
        else if(c.interrupt().equals("remove"))level.destroyBlock(TOY,true);
        LogUtils.getLogger().info("TOYS080_INTERRUPTED index={} reason={} elapsed={}",phase,c.interrupt(),elapsed);
      }
      if(elapsed>=c.end()){
        require(!ToyPreview.active(actor)&&actor.toyPlayingTicks()==0&&actor.toyTarget()==null,"Toy metadata survived cleanup");
        require(c.natural()||actor.isNoAi(),"Preview failed to restore NoAI");require(!actor.isNoGravity(),"Preview left gravity disabled");
        require(actor.state().adult==fixture(c).adult,"Life stage changed");
        if(fixture(c).adult)require(actor.state().lockedPrimaryType()==fixture(c).lockedPrimaryType()&&actor.state().lockedAlignment()==fixture(c).lockedAlignment(),"Adult identity changed");
        complete=true;server.tickRateManager().setFrozen(true);
        LogUtils.getLogger().info("TOYS080_NATIVE_COMPLETE index={} natural={} elapsed={} walked={} stopped={} removed={} gearPreserved=true",phase,c.natural(),elapsed,walked,stopped,c.interrupt().equals("remove"));
      }
    }catch(Throwable e){fatal=e.toString();LogUtils.getLogger().error("TOYS080_ASYNC_FAILURE",e);}
  }
  /** Development-only observation: no navigation, AI, timing or fixture mutation. */
  private static void logWaitingApproach(long now,boolean terminal){
    var info=new JsonObject();var state=actor.state();var nav=actor.getNavigation();var path=nav.getPath();var move=actor.getMoveControl();var data=actor.getPersistentData();
    info.addProperty("case_index",phase);info.addProperty("source_index",CASES.get(phase).id());info.addProperty("case_age",caseAge);info.addProperty("world_tick",now);info.addProperty("terminal",terminal);
    info.add("position",GSON.toJsonTree(new double[]{actor.getX(),actor.getY(),actor.getZ()}));info.add("velocity",GSON.toJsonTree(new double[]{actor.getDeltaMovement().x,actor.getDeltaMovement().y,actor.getDeltaMovement().z}));
    info.addProperty("walked",walked);info.addProperty("front_anchor_distance",actor.position().distanceTo(ToyContent.jackAnchor(actor,TOY)));info.addProperty("may_play",ToyContent.mayPlay(actor));
    info.addProperty("on_ground",actor.onGround());info.addProperty("horizontal_collision",actor.horizontalCollision);info.addProperty("in_water",actor.isInWater());info.addProperty("no_ai",actor.isNoAi());info.addProperty("behavior",actor.getBehavior().name());info.addProperty("behavior_ticks",actor.getBehaviorTicks());
    info.addProperty("adult",state.adult);info.addProperty("gait",LocomotionRules.groundGait(state).name());info.addProperty("run_points",state.points[ChaoState.RUN]);info.addProperty("movement_attribute",actor.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED));info.addProperty("mob_speed",actor.getSpeed());
    info.addProperty("energy",state.energy);info.addProperty("hunger",state.hunger);info.addProperty("boredom",state.boredom);info.addProperty("ill",state.isIll());info.addProperty("preview",ToyPreview.active(actor));
    for(String key:List.of("ChaoToyTarget","ChaoToyApproachUntil","ChaoToyNextPlay","ChaoToyUntil"))if(data.contains(key))info.addProperty(key,data.getLong(key));
    info.addProperty("nav_done",nav.isDone());info.addProperty("path_present",path!=null);
    if(path!=null){info.addProperty("path_can_reach",path.canReach());info.addProperty("path_done",path.isDone());info.addProperty("path_target",path.getTarget().toShortString());info.addProperty("path_next_index",path.getNextNodeIndex());info.addProperty("path_node_count",path.getNodeCount());
      var nodes=new JsonArray();for(int i=0;i<path.getNodeCount();i++)nodes.add(path.getNodePos(i).toShortString());info.add("path_nodes",nodes);if(!path.isDone())info.addProperty("path_next_node",path.getNextNodePos().toShortString());}
    info.addProperty("move_has_wanted",move.hasWanted());info.addProperty("move_speed_modifier",move.getSpeedModifier());info.add("move_wanted",GSON.toJsonTree(new double[]{move.getWantedX(),move.getWantedY(),move.getWantedZ()}));
    var goals=new JsonArray();for(var goal:actor.goalSelector.getAvailableGoals())if(goal.isRunning())goals.add(goal.getPriority()+":"+goal.getGoal().getClass().getName()+":"+goal.getFlags());info.add("running_goals",goals);
    LogUtils.getLogger().info("TOYS080_APPROACH_DIAGNOSTIC {}",GSON.toJson(info).replace("\n",""));
  }
  private static void arm(MinecraftServer server,int expected){
    if(phase!=expected||armed||!settled)return;var c=CASES.get(phase);long now=server.overworld().getGameTime();
    require(Math.abs(server.tickRateManager().tickrate()-20)<.0001f,"Normal 20TPS required");
    if(c.natural()){actor.setNoAi(false);actor.getPersistentData().remove("ChaoToyNextPlay");began=-1;elapsed=-1;}
    else{command(server,c.loop()?"loop":"once");require(ToyPreview.active(actor),"Preview not active");began=now;elapsed=0;}
    armed=true;server.tickRateManager().setFrozen(false);LogUtils.getLogger().info("TOYS080_PLAYBACK_ARMED index={} natural={} actualClientFramesReady=true",phase,c.natural());
  }
  @SubscribeEvent public static void frameStart(RenderFrameEvent.Pre event){if(enabled()){renderedFrame=false;worldFrame=false;}}
  private static ChaoEntity observed(Minecraft mc){if(mc.level!=null&&actorId!=null)for(var entity:mc.level.entitiesForRendering())if(entity instanceof ChaoEntity c&&c.getUUID().equals(actorId))return c;return null;}
  private static Shot wanted(){var list=schedule(CASES.get(phase));return list.get(Math.min(shotIndex,list.size()-1));}
  private static void camera(Minecraft mc){
    var c=observed(mc);if(c==null)return;if(observer==null||observer.level()!=mc.level){observer=new ArmorStand(mc.level,0,0,0);observer.setInvisible(true);observer.setNoGravity(true);}
    if(MODE.equals("lighting"))c.tickCount=60;
    var entry=CASES.get(phase);var front=Vec3.directionFromRotation(0,entry.floor()?0:entry.jack()?180:c.yBodyRot);var side=new Vec3(-front.z,0,front.x);
    Vec3 focus=entry.floor()?Vec3.atBottomCenterOf(TOY).add(0,.43,0):c.position().add(front.scale(.20)).add(0,.37,0);
    if(entry.jack())focus=focus.lerp(Vec3.atBottomCenterOf(TOY).add(0,.60,0),.45);
    int angle=wanted().angle();Vec3 eye=angle==3?focus.add(.1,2.8,.2):focus.add(front.scale(angle==0?(entry.jack()?1.9:2.65):angle==2?-(entry.jack()?2.2:2.6):.08)).add(side.scale(angle==1?2.65:angle==0?(entry.jack()?2.0:.40):(entry.jack()?-1.9:-.35))).add(0,angle==1?.55:entry.jack()?1.1:.85,0);
    var direction=focus.subtract(eye);float yaw=(float)(Math.toDegrees(Math.atan2(direction.z,direction.x))-90),pitch=(float)-Math.toDegrees(Math.atan2(direction.y,Math.hypot(direction.x,direction.z)));
    observer.moveTo(eye.x,eye.y-observer.getEyeHeight(),eye.z,yaw,pitch);observer.xo=observer.xOld=observer.getX();observer.yo=observer.yOld=observer.getY();observer.zo=observer.zOld=observer.getZ();observer.yRotO=yaw;observer.xRotO=pitch;observer.yHeadRotO=yaw;observer.setYHeadRot(yaw);
    mc.options.setCameraType(CameraType.FIRST_PERSON);mc.setCameraEntity(observer);
  }
  @SubscribeEvent public static void restoreCamera(ClientTickEvent.Pre event){if(enabled()){var mc=Minecraft.getInstance();if(mc.player!=null&&mc.getCameraEntity()!=mc.player)mc.setCameraEntity(mc.player);}}
  private static final class LightCapture implements VertexConsumer {
    final Set<Integer> lights=new TreeSet<>();int vertices;
    public VertexConsumer addVertex(float x,float y,float z){vertices++;return this;}
    public VertexConsumer setColor(int r,int g,int b,int a){return this;}
    public VertexConsumer setUv(float u,float v){return this;}
    public VertexConsumer setUv1(int u,int v){return this;}
    public VertexConsumer setUv2(int u,int v){lights.add(u|(v<<16));return this;}
    public VertexConsumer setNormal(float x,float y,float z){return this;}
  }
  private static JsonObject quads(Minecraft mc,Case entry,int inputLight){
    String key=phase+":"+inputLight;var cached=QUAD_PROOFS.get(key);if(cached!=null)return cached.deepCopy();
    BlockState state=mc.level.getBlockState(TOY);BakedModel model;String modelId;
    if(entry.floor()){
      require(state.is(ToyContent.block(entry.kind())),"Actual placed source missing");model=mc.getBlockRenderer().getBlockModel(state);modelId="block/"+entry.kind()+"_idle";
    }else{
      modelId="item/"+entry.kind()+"_hand";model=mc.getModelManager().getModel(ModelResourceLocation.standalone(ResourceLocation.fromNamespaceAndPath(ChaoGarden.MODID,modelId)));state=null;
    }
    var faces=new ArrayList<BakedQuad>();faces.addAll(model.getQuads(state,null,RandomSource.create(80),ModelData.EMPTY,null));
    for(var direction:Direction.values())faces.addAll(model.getQuads(state,direction,RandomSource.create(80),ModelData.EMPTY,null));
    require(!faces.isEmpty(),"Actual baked model has no faces: "+modelId);var bakedLights=new TreeSet<Integer>();var emitted=new LightCapture();int shaded=0;
    for(var q:faces){if(q.isShade())shaded++;for(int v=0;v<4;v++)bakedLights.add(q.getVertices()[v*IQuadTransformer.STRIDE+IQuadTransformer.UV2]);emitted.putBulkData(new PoseStack().last(),q,1,1,1,1,inputLight,0);}
    require(shaded==faces.size()&&bakedLights.equals(Set.of(0)),"Unexpected baked emissivity in "+modelId+" "+bakedLights);
    require(emitted.lights.equals(Set.of(inputLight)),"Actual bulk-light evaluation replaced observed scene light");
    var data=new JsonObject();data.addProperty("actual_loaded_model",modelId);data.addProperty("baked_model_class",model.getClass().getName());data.addProperty("actual_baked_quads",faces.size());data.addProperty("shaded_quads",shaded);data.add("baked_vertex_uv2",GSON.toJsonTree(bakedLights));data.add("engine_bulk_vertex_uv2",GSON.toJsonTree(emitted.lights));data.addProperty("engine_bulk_vertex_count",emitted.vertices);
    data.addProperty("scope","Loaded production baked quads and engine bulk evaluation using observed input light; this does not intercept the submitted GPU buffer.");QUAD_PROOFS.put(key,data);return data.deepCopy();
  }
  @SubscribeEvent public static void rendered(RenderLivingEvent.Post<?,?> event){
    if(!enabled()||phase<0||phase>=CASES.size()||SceneShaderPack.shadows()||!(event.getEntity() instanceof ChaoEntity c)||!c.getUUID().equals(actorId))return;
    var mc=Minecraft.getInstance();if(mc.screen!=null||mc.getOverlay()!=null||mc.getCameraEntity()!=observer||savedGear!=null&&!sameGear(c))return;var entry=CASES.get(phase);
    require(event.getRenderer().getModel() instanceof ChaoModel,"Production renderer/model missing");var model=(ChaoModel)event.getRenderer().getModel();require(model.officialPosesFinite(),"Nonfinite production pose");
    var data=new JsonObject();data.addProperty("actual_living_entity",true);data.addProperty("actual_production_model",true);data.addProperty("renderer",event.getRenderer().getClass().getName());data.addProperty("uuid",c.getUUID().toString());data.addProperty("state",c.state().encode());data.addProperty("state_sha256",sha(c.state().encode()));data.addProperty("fixture_state_sha256",sha(savedState==null?"":savedState));data.add("equipment",gearProof(c));data.addProperty("equipment_sha256",sha(GSON.toJson(gearProof(c))));data.addProperty("gameplay_geometry",model.gameplayFormKey());data.addProperty("official_geometry",model.officialGeometryKey());data.addProperty("motion",model.officialMotion().name());data.addProperty("finite_poses",true);
    data.add("skull_bounds",GSON.toJsonTree(model.accessoryBounds("skull")));data.addProperty("preview_kind",c.toyPlayingKind());data.addProperty("preview_remaining",c.toyPlayingTicks());data.addProperty("render_observed_server_elapsed",elapsed);data.addProperty("render_partial_tick",event.getPartialTick());data.addProperty("behavior",c.getBehavior().name());data.addProperty("no_ai",c.isNoAi());data.addProperty("no_gravity",c.isNoGravity());data.add("position",GSON.toJsonTree(new double[]{c.getX(),c.getY(),c.getZ()}));data.addProperty("source_distance",c.position().distanceTo(Vec3.atCenterOf(TOY)));data.addProperty("shaders_active",SceneShaderPack.active());data.addProperty("incoming_entity_packed_light",event.getPackedLight());
    int sky=mc.level.getBrightness(LightLayer.SKY,c.blockPosition()),block=mc.level.getBrightness(LightLayer.BLOCK,c.blockPosition());data.addProperty("sky_light",sky);data.addProperty("block_light",block);
    if(!MODE.equals("jack")){
      if(!settled||entry.dark()&&(sky!=0||block!=0))return;
      int input=entry.floor()?LevelRenderer.getLightColor(mc.level,TOY):event.getPackedLight();data.addProperty("observed_model_input_light",input);data.add("quad_light_observation",quads(mc,entry,input));
      if(!entry.floor()){
        boolean propReady=c.toyPlayingKind().equals(entry.kind())&&c.toyPlayingTicks()>0&&ChaoActiveToyAnimation049.active(c);
        data.addProperty("held_production_prop_ready",propReady);
        // Static pre-command frames arm the study; a queued server command
        // and its tracked client metadata need not arrive in that same frame.
        // Admit captures only after the actual production prop becomes active.
        if(armed&&!propReady)return;
      }
    }else{
      BlockState state=mc.level.getBlockState(TOY);data.addProperty("actual_source_present",state.is(ToyContent.block(entry.kind())));
      if(state.is(ToyContent.block(entry.kind()))){data.addProperty("toy_frame",state.getValue(ChaoToyBlock.FRAME));data.addProperty("toy_playing",state.getValue(ChaoToyBlock.PLAYING));}
      data.addProperty("jack_production_active",ChaoJackAnimation080.active(c));var face=model.resolvedFace076();if(face!=null){data.addProperty("eyes",face.eyes().name());data.addProperty("mouth",face.mouth().name());data.addProperty("eye_source",face.eyeSource().name());}
    }
    observedProof=data;renderedFrame=true;actorFrames++;
  }
  @SubscribeEvent public static void world(RenderLevelStageEvent event){
    if(!enabled()||phase<0||phase>=CASES.size()||event.getStage()!=RenderLevelStageEvent.Stage.AFTER_LEVEL||SceneShaderPack.shadows())return;
    var mc=Minecraft.getInstance();if(mc.screen!=null||mc.getOverlay()!=null||mc.getCameraEntity()!=observer)return;var entry=CASES.get(phase);
    if(entry.floor()&&settled){
      var c=observed(mc);if(c==null||savedGear!=null&&!sameGear(c))return;var state=mc.level.getBlockState(TOY);require(state.is(ToyContent.block(entry.kind())),"Placed toy disappeared");
      int sky=mc.level.getBrightness(LightLayer.SKY,TOY),block=mc.level.getBrightness(LightLayer.BLOCK,TOY);if(entry.dark()&&(sky!=0||block!=0))return;
      int light=LevelRenderer.getLightColor(mc.level,TOY);var data=new JsonObject();
      data.addProperty("actual_placed_world_source",true);data.addProperty("actual_world_render_stage","AFTER_LEVEL");data.addProperty("source_render_shape",state.getRenderShape().name());data.addProperty("source_block",BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString());data.addProperty("actual_actor_rendered_this_frame",renderedFrame);
      data.addProperty("uuid",c.getUUID().toString());data.addProperty("state",c.state().encode());data.addProperty("state_sha256",sha(c.state().encode()));data.add("equipment",gearProof(c));data.addProperty("equipment_sha256",sha(GSON.toJson(gearProof(c))));
      data.addProperty("sky_light",sky);data.addProperty("block_light",block);data.addProperty("observed_model_input_light",light);data.addProperty("shaders_active",SceneShaderPack.active());data.add("quad_light_observation",quads(mc,entry,light));
      data.addProperty("render_observed_server_elapsed",elapsed);data.addProperty("preview_kind",c.toyPlayingKind());data.addProperty("preview_remaining",c.toyPlayingTicks());data.addProperty("world_source_scope","Ordinary loaded block in real AFTER_LEVEL world stage and actual PNG; no chunk draw-call interception.");observedProof=data;worldFrame=true;return;
    }
    if(renderedFrame)worldFrame=true;
  }
  @SubscribeEvent public static void requested(net.neoforged.neoforge.client.event.sound.PlaySoundEvent event){
    if(!enabled()||phase<0||phase>=CASES.size()||!armed)return;var sound=event.getOriginalSound();
    if(sound!=null&&sound.getLocation().toString().equals("minecraft:block.note_block.pling")){SOUND_REQUESTS.put(sound,new SoundRequest(phase,elapsed,sound.getLocation().toString()));REQUESTED.incrementAndGet(phase);}
  }
  @SubscribeEvent public static void soundStarted(net.neoforged.neoforge.client.event.sound.PlaySoundSourceEvent event){
    if(!enabled())return;var sound=event.getSound();var request=SOUND_REQUESTS.remove(sound);if(request==null)return;
    try{require(sound.getVolume()<=.221f&&Math.abs(sound.getPitch()-1.35f)<.001f,"Jack cue changed level/pitch");SOUNDS.incrementAndGet(request.index());LogUtils.getLogger().info("TOYS080_SOUND index={} id={} requestedElapsed={} volume={} pitch={} channelStarted=true audioListening=false",request.index(),request.id(),request.elapsed(),sound.getVolume(),sound.getPitch());}
    catch(Throwable e){fatal=e.toString();LogUtils.getLogger().error("TOYS080_ASYNC_FAILURE",e);}
  }
  private static void capture(Minecraft mc){
    int index=phase,number=shotIndex;var entry=CASES.get(index);var shot=schedule(entry).get(number);var proof=observedProof.deepCopy();
    proof.addProperty("mode",MODE);proof.addProperty("source_index",entry.id());proof.addProperty("name",entry.name());proof.addProperty("form",entry.form());proof.addProperty("context",entry.context());proof.addProperty("dark",entry.dark());proof.addProperty("label",shot.label());proof.addProperty("view",VIEWS.get(shot.angle()));proof.addProperty("scheduled_tick",shot.tick());proof.addProperty("frame_admission_server_elapsed",elapsed);proof.addProperty("case_age",caseAge);proof.addProperty("walked_blocks",walked);proof.addProperty("sound_requests",REQUESTED.get(index));proof.addProperty("sound_channels_started",SOUNDS.get(index));
    var camera=mc.gameRenderer.getMainCamera();var pos=camera.getPosition();proof.add("actual_camera_position",GSON.toJsonTree(new double[]{pos.x,pos.y,pos.z}));proof.addProperty("actual_camera_yaw",camera.getYRot());proof.addProperty("actual_camera_pitch",camera.getXRot());proof.addProperty("fov",45);proof.addProperty("exact_capture_tick_guaranteed",false);
    if(shot.label().startsWith("recovered"))require(proof.get("preview_remaining").getAsInt()==0,"Recovery image still contains toy token");
    String name=String.format("toys080-%02d-%s-%s.png",entry.id(),entry.name(),shot.label());Path file=OUTPUT.resolve("screenshots").resolve(name);try{Files.createDirectories(file.getParent());}catch(Exception e){throw new IllegalStateException(e);}
    Screenshot.grab(OUTPUT.toFile(),name,mc.getMainRenderTarget(),message->{require(Files.isRegularFile(file),"PNG missing");proof.addProperty("png",file.toString());proof.addProperty("png_sha256",sha(file));write(OUTPUT.resolve("frames").resolve(name.replace(".png",".json")),proof);SAVED.incrementAndGet();LogUtils.getLogger().info("TOYS080_CAPTURE mode={} index={} name={} label={} sha256={}",MODE,index,name,shot.label(),proof.get("png_sha256"));});shots++;shotIndex++;viewFrames=0;
  }
  @SubscribeEvent public static void frameEnd(RenderFrameEvent.Post event){
    if(!enabled()||phase<0||phase>=CASES.size()||clientPhase!=phase||!worldFrame)return;var mc=Minecraft.getInstance();worldFrames++;viewFrames++;
    var c=CASES.get(phase);var list=schedule(c);
    if(!armed&&!c.floor()){
      if(settled&&c.natural()&&shotIndex<2){if(viewFrames>=4)capture(mc);return;}
      if(settled&&viewFrames>=12&&!armRequested){armRequested=true;int expected=phase;onServer(mc.getSingleplayerServer(),()->arm(mc.getSingleplayerServer(),expected));}return;
    }
    if(shotIndex<list.size()){
      if(settled&&(!MODE.equals("jack")&&complete||MODE.equals("jack")&&armed&&elapsed>=list.get(shotIndex).tick())&&viewFrames>=4)capture(mc);return;
    }
    if(!complete)return;require((c.floor()||actorFrames>=16)&&worldFrames>=16,"Actual renderer coverage incomplete");
    if(c.jack())require(SOUNDS.get(phase)==(c.loop()?2:1),"Unexpected jack cue count "+SOUNDS.get(phase));
    var done=new JsonObject();done.addProperty("mode",MODE);done.addProperty("source_index",c.id());done.addProperty("name",c.name());done.addProperty("captures",list.size());done.addProperty("actor_frames",actorFrames);done.addProperty("world_frames",worldFrames);done.addProperty("sound_requests",REQUESTED.get(phase));done.addProperty("sound_channels_started",SOUNDS.get(phase));done.addProperty("actual_production_actor",!c.floor());done.addProperty("actual_placed_world_source",c.floor());done.addProperty("complete",true);write(OUTPUT.resolve("complete").resolve(String.format("%02d-%s.json",c.id(),c.name())),done);completedCases++;
    LogUtils.getLogger().info("TOYS080_RENDER_COMPLETE mode={} index={} sourceIndex={} captures={} actorFrames={} worldFrames={} sounds={}",MODE,phase,c.id(),list.size(),actorFrames,worldFrames,SOUNDS.get(phase));int finishedIndex=phase;pending=phase;clientPhase=-2;
    onServer(mc.getSingleplayerServer(),()->{if(phase==finishedIndex){if(phase+1<CASES.size())next(mc.getSingleplayerServer(),phase+1);else phase=CASES.size();}});
  }
  private static void catalog(){
    require(!Files.exists(OUTPUT.resolve("case-catalog.json")),"Refuse existing capture output; use a fresh directory");
    var data=new JsonObject();data.addProperty("mode",MODE);data.addProperty("capture_kind","Actual production actors and toy world models, no alternate animation");data.addProperty("normal_tps",20);data.addProperty("audio_listening",false);data.addProperty("independent_remote_multiplayer",false);data.addProperty("source_freeze",System.getenv().getOrDefault("CHAO_TOYS080_SOURCE_FREEZE","unrecorded"));data.addProperty("quad_observation_scope","Loaded actual baked quads plus engine bulk-light evaluation; not a GPU buffer interception.");
    var rows=new JsonArray();for(var c:CASES){var row=new JsonObject();row.addProperty("source_index",c.id());row.addProperty("name",c.name());row.addProperty("toy",c.kind());row.addProperty("context",c.context());row.addProperty("form",c.form());row.addProperty("dark",c.dark());row.addProperty("start_side",c.startSide().getSerializedName());row.addProperty("interrupt",c.interrupt());row.addProperty("real_079_equipment",c.gear());row.addProperty("fixture_state",fixture(c).encode());row.addProperty("fixture_sha256",sha(fixture(c).encode()));row.add("shots",GSON.toJsonTree(schedule(c)));rows.add(row);}data.add("cases",rows);write(OUTPUT.resolve("case-catalog.json"),data);
  }
  @SubscribeEvent public static void client(ClientTickEvent.Post event){
    if(!enabled()||finished)return;var mc=Minecraft.getInstance();if(fatal!=null)throw new IllegalStateException("TOYS080 server failure: "+fatal);if(wallStart==0)wallStart=System.nanoTime();require(System.nanoTime()-wallStart<2_400_000_000_000L,"Client study wall deadline");
    if(mc.screen instanceof AccessibilityOnboardingScreen)mc.setScreen(new TitleScreen());
    if(!started&&mc.screen instanceof TitleScreen){started=true;catalog();mc.options.renderDistance().set(3);mc.options.simulationDistance().set(5);mc.options.framerateLimit().set(45);mc.options.fov().set(45);mc.options.gamma().set(.5);mc.options.pauseOnLostFocus=false;mc.options.hideGui=true;mc.options.tutorialStep=net.minecraft.client.tutorial.TutorialSteps.NONE;mc.getWindow().setWindowed(1440,1000);mc.resizeDisplay();
      mc.createWorldOpenFlows().createFreshLevel("Toys080-"+MODE+"-"+System.currentTimeMillis(),new LevelSettings("Toys080",GameType.CREATIVE,false,Difficulty.PEACEFUL,true,new GameRules(),WorldDataConfiguration.DEFAULT),new WorldOptions(2580L,false,false),r->r.registryOrThrow(Registries.WORLD_PRESET).getHolderOrThrow(WorldPresets.FLAT).value().createWorldDimensions(),mc.screen);
    }
    if(!setup&&mc.player!=null&&mc.getSingleplayerServer()!=null){setup=true;mc.setScreen(null);ChaoRenderer.suppressNames=true;onServer(mc.getSingleplayerServer(),()->{var server=mc.getSingleplayerServer();var level=server.overworld();level.getGameRules().getRule(GameRules.RULE_DAYLIGHT).set(false,server);level.getGameRules().getRule(GameRules.RULE_WEATHER_CYCLE).set(false,server);level.getGameRules().getRule(GameRules.RULE_DOMOBSPAWNING).set(false,server);next(server,0);});}
    if(phase>=0&&phase<CASES.size()&&mc.level!=null){if(clientPhase==-2&&phase==pending)return;if(clientPhase!=phase){clientPhase=phase;shotIndex=viewFrames=actorFrames=worldFrames=0;observedProof=null;armRequested=false;}camera(mc);}
    if(phase==CASES.size()&&SAVED.get()==shots){require(completedCases==CASES.size()&&shots==CASES.stream().mapToInt(c->schedule(c).size()).sum(),"Missing case/image completion");finished=true;var done=new JsonObject();done.addProperty("mode",MODE);done.addProperty("completed_cases",completedCases);done.addProperty("saved_pngs",SAVED.get());done.addProperty("actual_production_sources",true);done.addProperty("actor_required_for_every_case",MODE.equals("jack"));done.addProperty("audio_listening",false);write(OUTPUT.resolve("visual-complete.json"),done);onServer(mc.getSingleplayerServer(),()->mc.getSingleplayerServer().tickRateManager().setFrozen(false));LogUtils.getLogger().info("TOYS080_VISUAL_COMPLETE mode={} cases={} captures={} saved={} actualProduction=true",MODE,completedCases,shots,SAVED.get());mc.stop();}
  }
}
