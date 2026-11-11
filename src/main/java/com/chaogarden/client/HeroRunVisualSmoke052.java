package com.chaogarden.client;

import com.chaogarden.ChaoGarden;
import com.chaogarden.GardenConfig;
import com.chaogarden.companion.AccessoriesBridge;
import com.chaogarden.companion.CompanionContent;
import com.chaogarden.core.ChaoState;
import com.chaogarden.core.AnalyzerStatRules;
import com.chaogarden.core.HeroRunMorphology052;
import com.chaogarden.entity.ChaoEntity;
import com.chaogarden.item.ChaoCarrierItem;
import com.chaogarden.toys.GardenToyPerformance049;
import com.chaogarden.toys.QuietToyPlayRules049;
import com.chaogarden.toys.ToyPreview;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.logging.LogUtils;
import java.nio.file.Files;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import net.minecraft.client.*;
import net.minecraft.client.gui.screens.AccessibilityOnboardingScreen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.*;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import org.joml.Vector3f;

/** Opt-in production morphology, native movement/toys and actual Accessories-slot captures.
 * Fixture training is deliberately accelerated; playback retains the normal 20 TPS clock.
 * This entire helper and its nested classes are excluded from the production JAR. */
@EventBusSubscriber(modid=ChaoGarden.MODID,value=Dist.CLIENT)
public final class HeroRunVisualSmoke052 {
  private record Case(String name,boolean adult,int percent,String action,String hat,boolean rabbit) {
    boolean still(){return action.startsWith("still")||action.equals("fresh");}
    boolean carry(){return action.startsWith("carry");}
    boolean toy(){return action.equals("broom")||action.equals("picture_book");}
    boolean walk(){return action.contains("walk")||action.equals("maturity");}
    boolean gain(){return action.equals("baby_gain_walk")||action.equals("adult_gain_walk");}
    boolean loss(){return action.equals("adult_loss_walk");}
    int duration(){return toy()?180:carry()?140:action.equals("evolution")?260:gain()||loss()?280:240;}
  }
  private static List<Case> cases(){
    var result=new ArrayList<Case>();
    for(boolean adult:new boolean[]{false,true})for(int percent:new int[]{0,25,50,75,100})
      result.add(new Case((adult?"adult":"baby")+"_p"+String.format("%03d",percent),adult,percent,"still","",false));
    result.add(new Case("first_evolved_adult",true,100,"fresh","",false));
    result.add(new Case("baby_p075_rabbit_skull",false,75,"still","skull",true));
    result.add(new Case("adult_p050_rabbit_pumpkin",true,50,"still","pumpkin",true));
    result.add(new Case("adult_p100_melon",true,100,"still","melon",false));
    result.add(new Case("baby_training_walk",false,0,"baby_gain_walk","",false));
    result.add(new Case("adult_training_walk",true,0,"adult_gain_walk","",false));
    result.add(new Case("adult_p100_broom",true,100,"broom","",false));
    result.add(new Case("adult_p100_picture_book",true,100,"picture_book","",false));
    result.add(new Case("baby_p075_equipped_ground",false,75,"carry_ground","",false));
    result.add(new Case("baby_p075_equipped_flying",false,75,"carry_flying","",false));
    result.add(new Case("adult_p100_equipped_flying",true,100,"carry_flying","",false));
    result.add(new Case("adult_maturity_clock",true,100,"maturity","",false));
    result.add(new Case("adult_training_decrease_walk",true,100,"adult_loss_walk","",false));
    result.add(new Case("adult_opposite_alignment",true,100,"locked_alignment","",false));
    result.add(new Case("first_evolution_cocoon",false,100,"evolution","",false));
    result.add(new Case("baby_p075_red_shiny_rabbit_wings",false,75,"still_red_shiny","",true));
    result.add(new Case("adult_p100_ruby_rabbit_wings_glasses",true,100,"still_ruby_glasses","",true));
    return List.copyOf(result);
  }
  private static final List<Case> CASES=cases();
  private static final int[][] SHOTS=new int[CASES.size()][3];
  private static final int[] WORLD=new int[CASES.size()],RENDERED=new int[CASES.size()],MOVING=new int[CASES.size()],COCOON=new int[CASES.size()],BEFORE=new int[CASES.size()],AFTER=new int[CASES.size()],CONTACTS=new int[CASES.size()];
  private static final long[] SIGNATURES=new long[CASES.size()];
  private static final float[] MAX_CONTACT=new float[CASES.size()];
  private static final AtomicInteger SAVED=new AtomicInteger();
  private static final LionfishQuietToyClip049 QUIET=new LionfishQuietToyClip049();
  private static boolean enabled(){return Boolean.getBoolean("chao_garden.heroRun052Study");}
  private static int limit(){return Math.clamp(Integer.getInteger("chao_garden.heroRun052Limit",CASES.size()),1,CASES.size());}
  private static int start(){return Math.clamp(Integer.getInteger("chao_garden.heroRun052Start",0),0,limit()-1);}
  private static volatile int phase=-1;
  private static volatile UUID actorId;
  private static volatile boolean armed,complete;
  private static volatile long elapsed,serverTime;
  private static volatile String expectedState="",fatal;
  private static ChaoEntity actor;
  private static ArmorStand observer;
  private static Container slot;
  private static Object accessoryContainer;
  private static Class<?> containerType;
  private static long began,lastTick,wallStarted,playbackWall,carryBefore;
  private static Vec3 previousPosition;
  private static double walked;
  private static float initialMaturity;
  private static boolean started,setup,finished,armRequested,renderedThisFrame,worldThisFrame;
  private static int clientPhase=-1,pendingPhase=-1,view,viewFrames,ticks,shots;
  private static long lastMotionCapture=-1;

  private static void require(boolean okay,String message){if(!okay)throw new IllegalStateException("MORPH052 "+message);}
  private static void onServer(MinecraftServer server,Runnable task){server.execute(()->{try{task.run();}catch(Throwable error){fatal=error.toString();LogUtils.getLogger().error("MORPH052_ASYNC_FAILURE",error);}});}
  private static void markSlot(){try{containerType.getMethod("markChanged",boolean.class).invoke(accessoryContainer,false);}catch(ReflectiveOperationException error){throw new IllegalStateException(error);}}
  private static void clearSlot(){if(slot!=null){slot.setItem(0,ItemStack.EMPTY);markSlot();slot=null;accessoryContainer=null;}}

  /** Exact stage seeds use the real training operation and existing maturity setter.
   * No mesh, animation pose, skin or attachment is injected by this helper. */
  private static ChaoState fixture(Case entry){
    var state=new ChaoState();state.energy=state.hunger=100;state.happiness=80;state.boredom=0;
    state.adult=entry.adult();state.align=entry.adult()?1:entry.percent()*.005f;
    if(entry.adult())state.primaryType=ChaoState.RUN_TYPE;
    state.setEvolutionStrength(entry.adult()?1.2f:.8f);
    int total=Math.round((entry.adult()?HeroRunMorphology052.ADULT_RUN_PROGRESS:HeroRunMorphology052.BABY_RUN_PROGRESS)*entry.percent()/100f);
    state.train(ChaoState.RUN,total,new Random(5252));
    if(entry.action().equals("fresh")){
      state=new ChaoState();state.align=1;state.energy=state.hunger=100;state.happiness=80;
      state.train(ChaoState.RUN,480,new Random(5252));state.ageTicks=999;state.setEvolutionStrength(.99f);
      require(state.tick(1000,Integer.MAX_VALUE,new Random(5252))==ChaoState.Transition.EVOLVED,"fresh fixture did not use first evolution");
      while(state.cocoonTicks>0)state.tick(1000,Integer.MAX_VALUE,new Random(5252));
      require(state.adult&&state.primaryType==ChaoState.RUN_TYPE&&state.lockedAlignment()>=.5,"fresh evolution selected a different identity");
    }
    if(entry.action().equals("maturity"))state.setEvolutionStrength(.6f);
    if(entry.action().equals("evolution")){
      state.ageTicks=GardenConfig.CHILD_TICKS.get()-12;state.setEvolutionStrength(.99f);
    }
    if(entry.rabbit()){
      int rabbit=Arrays.asList(ChaoState.ANIMALS).indexOf("rabbit");require(rabbit>=0,"registered rabbit DNA absent");
      state.animalMask=1<<rabbit;
      for(int i=0;i<state.animalParts.length;i++)if((ChaoState.animalSlots(rabbit)&(1<<i))!=0)state.animalParts[i]=rabbit;
      state.animalPartsMask=1<<rabbit;
    }
    if(entry.action().equals("still_red_shiny")){
      state.expressedColor=2;state.colorGenes=new int[]{2,2};state.shiny=true;state.shinyGenes=new int[]{1,1};
    }
    if(entry.action().equals("still_ruby_glasses")){
      state.expressedJewel=3;state.jewelGenes=new int[]{3,3};
    }
    if(entry.action().equals("still_red_shiny")||entry.action().equals("still_ruby_glasses")){
      int phoenix=Arrays.asList(ChaoState.ANIMALS).indexOf("phoenix");require(phoenix>=0,"registered phoenix DNA absent");
      state.animalParts[ChaoState.WINGS]=phoenix;state.animalMask|=1<<phoenix;state.animalPartsMask|=1<<phoenix;
    }
    // Serialization initializes the genuine adult identity lock and proves no new schema is needed.
    String encoded=state.encode();var decoded=ChaoState.decode(encoded);require(decoded.encode().equals(encoded),"stage state did not round trip exactly");return decoded;
  }
  private static void equip(ServerPlayer player){
    require(AccessoriesBridge.available(),"carry scenes require actual Accessories/owo runtime jars");
    try{
      var carrier=new ItemStack(ChaoGarden.item("chao_carrier"));require(ChaoCarrierItem.capture(carrier,actor,player),"actual carrier capture failed");
      var capabilityType=Class.forName("io.wispforest.accessories.api.AccessoriesCapability");containerType=Class.forName("io.wispforest.accessories.api.AccessoriesContainer");
      var capability=capabilityType.getMethod("get",LivingEntity.class).invoke(null,player);
      accessoryContainer=((Map<?,?>)capabilityType.getMethod("getContainers").invoke(capability)).get("chao_companion");
      require(accessoryContainer!=null,"actual companion slot missing");slot=(Container)containerType.getMethod("getAccessories").invoke(accessoryContainer);
      slot.setItem(0,carrier);markSlot();actorId=ChaoCarrierItem.storedUUID(carrier);actor=null;
    }catch(ReflectiveOperationException error){throw new IllegalStateException(error);}
  }
  private static void next(MinecraftServer server,int index){
    server.tickRateManager().setFrozen(true);clearSlot();
    if(actor!=null){ToyPreview.stop(actor);actor.setCompanionCarry(null,0);actor.discard();actor=null;}
    var level=server.overworld();var entry=CASES.get(index);var player=server.getPlayerList().getPlayers().getFirst();
    player.stopFallFlying();player.getAbilities().flying=false;player.getAbilities().mayfly=false;player.onUpdateAbilities();player.setNoGravity(true);player.setDeltaMovement(Vec3.ZERO);
    player.teleportTo(entry.carry()?.5:4.5,entry.carry()?80:82,entry.carry()?.5:5.5);player.setYRot(0);player.yBodyRot=player.yHeadRot=0;player.setXRot(0);
    player.setGameMode(entry.carry()?GameType.CREATIVE:GameType.SPECTATOR);
    if(entry.action().equals("carry_flying")){player.getAbilities().mayfly=true;player.getAbilities().flying=true;player.onUpdateAbilities();player.teleportTo(.5,82,.5);}
    actor=ChaoGarden.CHAO.get().create(level);require(actor!=null,"Chao fixture creation failed");actor.owner=player.getUUID();actor.setState(fixture(entry));
    actor.setNoAi(!entry.walk());actor.setInvulnerable(true);actor.setOnGround(true);actor.setNoGravity(false);actor.deferBehavior(100000);
    actor.moveTo(.5,80,.5,0,0);actor.yBodyRot=actor.yBodyRotO=actor.yHeadRot=actor.yHeadRotO=0;
    actor.goalSelector.removeAllGoals(goal->true);actor.targetSelector.removeAllGoals(goal->true);
    if(!entry.hat().isEmpty())actor.setItemSlot(EquipmentSlot.HEAD,new ItemStack(switch(entry.hat()){case "pumpkin"->Items.CARVED_PUMPKIN;case "melon"->Items.MELON;default->Items.SKELETON_SKULL;}));
    if(entry.action().equals("still_ruby_glasses"))actor.setItemSlot(EquipmentSlot.OFFHAND,new ItemStack(ChaoGarden.item("round_glasses_accessory")));
    var saved=new CompoundTag();actor.saveWithoutId(saved);String before=actor.state().encode();actor.load(saved);
    require(actor.state().encode().equals(before),"entity save/reload changed prototype state");
    require(level.addFreshEntity(actor),"stage Chao did not enter actual level");actorId=actor.getUUID();expectedState=before;
    if(entry.carry())equip(player);
    phase=index;elapsed=0;serverTime=began=lastTick=level.getGameTime();previousPosition=new Vec3(.5,80,.5);walked=0;
    armed=complete=false;playbackWall=System.nanoTime();initialMaturity=HeroRunMorphology052.sample(fixture(entry)).maturity();
    if(entry.carry())server.tickRateManager().setFrozen(false);
    LogUtils.getLogger().info("MORPH052_CASE {} name={} adult={} percent={} action={} sample={} state={} epochMillis={} actualAccessories={}",index,entry.name(),entry.adult(),entry.percent(),entry.action(),HeroRunMorphology052.sample(ChaoState.decode(expectedState)),expectedState,System.currentTimeMillis(),entry.carry());
  }
  private static void command(MinecraftServer server,String toy,String mode){
    try{var player=server.getPlayerList().getPlayers().getFirst();require(server.getCommands().getDispatcher().execute("chao testtoy "+toy+" "+mode,player.createCommandSourceStack().withPermission(2))==1,"native toy preview dispatcher rejected "+toy);}
    catch(com.mojang.brigadier.exceptions.CommandSyntaxException error){throw new IllegalStateException(error);}
  }
  private static void arm(MinecraftServer server,int index){
    if(index!=phase||armed)return;var entry=CASES.get(index);
    if(entry.carry()){
      var actual=CompanionContent.findEntity(server,actorId);require(actual instanceof ChaoEntity,"actual equipped slot did not project a body");actor=(ChaoEntity)actual;expectedState=actor.state().encode();
    }
    require(actor!=null&&!actor.isRemoved(),"active fixture missing");initialMaturity=HeroRunMorphology052.sample(actor.state()).maturity();
    if(entry.toy())command(server,entry.action(),"once");
    began=lastTick=serverTime=server.overworld().getGameTime();elapsed=0;playbackWall=System.nanoTime();armed=true;
    require(Math.abs(server.tickRateManager().tickrate()-20)<.00001,"study must retain normal 20 TPS");
    if(!entry.still())server.tickRateManager().setFrozen(false);
    LogUtils.getLogger().info("MORPH052_ARMED {} epochMillis={} normal20TPS=true fixtureTrainingAccelerated={}",phase,System.currentTimeMillis(),entry.gain()||entry.loss());
  }
  @SubscribeEvent public static void server(ServerTickEvent.Post event){
    if(!enabled()||phase<0||phase>=limit()||!armed||complete)return;
    var server=event.getServer();var level=server.overworld();long now=level.getGameTime();if(now==lastTick)return;lastTick=serverTime=now;elapsed=now-began;
    var entry=CASES.get(phase);require(actor!=null&&!actor.isRemoved(),"actual running actor disappeared");
    if(entry.walk()){
      actor.getNavigation().moveTo(40.5,80,.5,.7);
      walked+=actor.position().subtract(previousPosition).horizontalDistance();previousPosition=actor.position();
    }
    if(entry.gain()&&elapsed<=240&&elapsed%2==0){
      actor.state().train(ChaoState.RUN,entry.adult()?8:4,new Random(5252+elapsed));
      if(!entry.adult())actor.state().pet(.5f/120);actor.sync();
    }
    if(entry.loss()&&elapsed<=240&&elapsed%2==0){
      int target=Math.max(0,960-(int)(elapsed/2)*8),targetLevel=target/100,progress=target%100;
      while(actor.state().levels[ChaoState.RUN]>targetLevel)require(AnalyzerStatRules.apply(actor.state(),ChaoState.RUN,AnalyzerStatRules.LEVEL,-1),"actual Analyzer level decrease rejected");
      require(AnalyzerStatRules.apply(actor.state(),ChaoState.RUN,AnalyzerStatRules.PROGRESS,progress),"actual Analyzer progress decrease rejected");actor.sync();
    }
    if(entry.action().equals("locked_alignment")&&elapsed%4==0){actor.state().pet(-.25f);actor.sync();require(actor.state().lockedAlignment()>=.5f&&actor.state().align>=.5f,"opposite petting changed adult Hero alignment");}
    if(entry.carry()){
      var player=server.getPlayerList().getPlayers().getFirst();
      require(CompanionContent.isManaged(actor),"actual Accessories body stopped being managed");
      if(elapsed>30)require(actor.companionCarryPosition()==(entry.action().equals("carry_flying")?2:1),"actual companion selected wrong carry attachment");
      require(player.getAbilities().flying==entry.action().equals("carry_flying"),"player flight fixture changed");
    }
    if(elapsed>=entry.duration()){
      if(entry.walk())require(walked>.8,"motion fixture did not really navigate");
      if(entry.toy())require(!ToyPreview.active(actor)&&actor.toyPlayingTicks()==0&&actor.toyTarget()==null,"actual toy preview failed cleanup");
      var sample=HeroRunMorphology052.sample(actor.state());
      if(entry.gain())require(entry.adult()?sample.specialization()>.99f:sample.babyRun()>.99f&&sample.babyHero()>.99f,"accelerated real training did not reach endpoint");
      if(entry.loss())require(sample.specialization()<.001f&&sample.maturity()>=initialMaturity,"decrease left specialization or reduced biological maturity");
      if(entry.action().equals("maturity"))require(sample.maturity()>initialMaturity,"biological maturity clock did not advance");
      if(entry.action().equals("evolution"))require(actor.state().adult&&actor.state().primaryType==ChaoState.RUN_TYPE&&actor.state().lockedAlignment()>=.5f&&!actor.isCocooned(),"actual first evolution did not finish as Hero Run");
      complete=true;server.tickRateManager().setFrozen(true);
      LogUtils.getLogger().info("MORPH052_NATIVE_COMPLETE {} name={} elapsed={} walked={} sample={} state={} epochMillis={}",phase,entry.name(),elapsed,walked,sample,actor.state().encode(),System.currentTimeMillis());
    }
  }
  private static ChaoEntity observed(Minecraft mc){
    if(mc.level==null||actorId==null)return null;
    for(var entity:mc.level.entitiesForRendering())if(entity instanceof ChaoEntity c&&c.getUUID().equals(actorId))return c;return null;
  }
  @SubscribeEvent public static void restoreCamera(ClientTickEvent.Pre event){if(enabled()){
    var mc=Minecraft.getInstance();if(mc.player!=null&&mc.getCameraEntity()!=mc.player)mc.setCameraEntity(mc.player);
  }}
  private static void camera(Minecraft mc){
    var c=observed(mc);if(c==null)return;var entry=CASES.get(phase);
    if(observer==null||observer.level()!=mc.level){observer=new ArmorStand(mc.level,0,0,0);observer.setInvisible(true);observer.setNoGravity(true);}
    int selected=entry.still()?view:Math.min(2,(int)elapsed*3/Math.max(1,entry.duration()));
    var source=entry.carry()?mc.player:c;
    float heading=entry.carry()?mc.player.yBodyRot:c.yBodyRot;
    var forward=Vec3.directionFromRotation(0,heading);var side=new Vec3(forward.z,0,-forward.x);
    Vec3 focus=source.position().add(0,entry.carry()?entry.action().equals("carry_ground")?1.75:1.05:entry.adult()?.55:.45,0);
    double distance=entry.carry()?3.6:2.0;
    Vec3 eye=focus.add(selected==0?forward.scale(distance):selected==1?side.scale(distance):forward.scale(-distance*.82).add(side.scale(distance*.72)));
    if(selected==2)eye=eye.add(0,entry.carry()?.2:.3,0);
    var delta=focus.subtract(eye);float yaw=(float)(Math.toDegrees(Math.atan2(delta.z,delta.x))-90),pitch=(float)-Math.toDegrees(Math.atan2(delta.y,Math.hypot(delta.x,delta.z)));
    observer.moveTo(eye.x,eye.y-observer.getEyeHeight(),eye.z,yaw,pitch);
    observer.xo=observer.xOld=observer.getX()+source.xo-source.getX();observer.yo=observer.yOld=observer.getY()+source.yo-source.getY();observer.zo=observer.zOld=observer.getZ()+source.zo-source.getZ();
    observer.yRotO=yaw;observer.xRotO=pitch;observer.setYHeadRot(yaw);observer.yHeadRotO=yaw;
    mc.options.setCameraType(CameraType.FIRST_PERSON);mc.setCameraEntity(observer);
    if(selected!=view){view=selected;viewFrames=0;}
  }
  private static long signature(ChaoModel model){
    try{var field=ChaoModel.class.getDeclaredField("official");field.setAccessible(true);return ((OfficialChaoRig)field.get(model)).shapeSignature();}
    catch(ReflectiveOperationException error){throw new IllegalStateException(error);}
  }
  private static void toyContact(ChaoEntity c,ChaoModel model,float partial){
    if(!ChaoQuietToyAnimation049.active(c))return;float t=GardenToyPerformance049.phase(c,partial),w=GardenToyPerformance049.presence(c,partial);if(t<16||t>=144)return;
    String kind=c.toyPlayingKind();float scale=c.state().adult?.8f:.64f;
    for(boolean left:new boolean[]{false,true}){
      var palm=model.toyPalm(left);require(palm!=null,"morph toy lost real palm");var actual=ChaoTriangleAnimation.palm(model::transformAccessoryAnchor,palm,left);
      var target=kind.equals(QuietToyPlayRules049.BROOM)?ChaoQuietToyAnimation049.broomTarget(ChaoQuietToyAnimation049.broomMatrix(w,QUIET.sweep(t),scale),left)
          :ChaoQuietToyAnimation049.bookTarget(ChaoQuietToyAnimation049.bookMatrix(model::transformAccessoryAnchor,w,ChaoProneToyPose051.runtimeBounds(c,model)),left);
      float error=actual.distance(target);MAX_CONTACT[phase]=Math.max(MAX_CONTACT[phase],error);CONTACTS[phase]++;require(error<.025f,"morphed hand lost actual toy contact: "+kind+" error="+error);
    }
  }
  @SubscribeEvent public static void frameBegin(RenderFrameEvent.Pre event){if(enabled())renderedThisFrame=worldThisFrame=false;}
  @SubscribeEvent public static void rendered(RenderLivingEvent.Post<?,?> event){
    if(!enabled()||phase<0||phase>=limit()||SceneShaderPack.shadows()||!(event.getEntity() instanceof ChaoEntity c)||!c.getUUID().equals(actorId))return;
    var mc=Minecraft.getInstance();if(mc.screen!=null||mc.getOverlay()!=null||mc.getCameraEntity()!=observer)return;
    var entry=CASES.get(phase);var model=(ChaoModel)event.getRenderer().getModel();require(model.officialPosesFinite(),"actual morphology pose is not finite");
    if(!entry.action().equals("evolution"))require(c.state().adult==entry.adult()&&!c.isCocooned(),"stage unexpectedly evolved or cocooned");
    if(entry.still()&&!c.state().encode().equals(expectedState))return;
    renderedThisFrame=true;RENDERED[phase]++;
    if(c.isCocooned())COCOON[phase]++;else if(c.state().adult)AFTER[phase]++;else BEFORE[phase]++;
    if(!c.isCocooned()){
      for(String bone:List.of("root","body","skull","arm_left","arm_right","foot_left","foot_right")){
        var at=new PoseStack();model.transformAccessoryAnchor(bone,at);require(at.last().pose().isFinite(),"morph semantic anchor invalid: "+bone);
      }
      var eyes=model.accessoryEyes();if(eyes!=null)for(float value:eyes)require(Float.isFinite(value),"morph detached actual eye landmark");
      long current=signature(model);if(SIGNATURES[phase]==0){SIGNATURES[phase]=current;LogUtils.getLogger().info("MORPH052_RENDER_MODEL {} shapeSignature={} geometry={} gameplay={} texture={} sample={}",phase,current,model.officialGeometryKey(),model.gameplayFormKey(),OfficialChaoRig.texture(c.state()),HeroRunMorphology052.sample(c.state()));}
      if(c.walkAnimation.speed()>.01f)MOVING[phase]++;
      if(entry.toy())toyContact(c,model,Math.clamp(event.getPartialTick(),0,1));
    }
  }
  @SubscribeEvent public static void renderedWorld(RenderLevelStageEvent event){
    if(!enabled()||phase<0||phase>=limit()||event.getStage()!=RenderLevelStageEvent.Stage.AFTER_LEVEL||SceneShaderPack.shadows())return;
    var mc=Minecraft.getInstance();if(mc.screen==null&&mc.getOverlay()==null&&mc.getCameraEntity()==observer&&renderedThisFrame)worldThisFrame=true;
  }
  private static void capture(Minecraft mc){
    var entry=CASES.get(phase);String label=switch(view){case 0->"front";case 1->"side";default->"rear";};
    String name=String.format("hero052-%02d-%s-%s-t%03d-f%04d.png",phase,entry.name(),label,elapsed,WORLD[phase]);int index=phase;long at=elapsed;
    Screenshot.grab(mc.gameDirectory,name,mc.getMainRenderTarget(),message->{require(Files.isRegularFile(mc.gameDirectory.toPath().resolve("screenshots").resolve(name)),"native screenshot missing");SAVED.incrementAndGet();LogUtils.getLogger().info("MORPH052_CAPTURE {} case={} tick={} wallNanos={} epochMillis={}",name,index,at,System.nanoTime(),System.currentTimeMillis());});shots++;SHOTS[phase][view]++;
  }
  private static void finishCase(Minecraft mc){
    var entry=CASES.get(phase);require(RENDERED[phase]>25&&WORLD[phase]>25,"case lacked actual world/actor rendering");
    for(int i=0;i<3;i++)require(SHOTS[phase][i]>=3,"case missed a camera angle");
    if(entry.walk())require(MOVING[phase]>20,"production walk animation was not observed");
    if(entry.toy())require(CONTACTS[phase]>30,"actual toy grip was never measured");
    if(entry.carry())require(CompanionBackLayer.renders()>carryBefore+20,"actual player carry layer never rendered");
    if(entry.action().equals("evolution"))require(BEFORE[phase]>3&&COCOON[phase]>20&&AFTER[phase]>10,"actual before/cocoon/after transition was not rendered");
    if(!entry.still())require((System.nanoTime()-playbackWall)/1e9>=entry.duration()/20.0-.65,"real playback ran faster than normal20TPS");
    LogUtils.getLogger().info("MORPH052_RENDER_COMPLETE {} name={} worldFrames={} actorFrames={} captures={} movingFrames={} toyContacts={} maxToyError={} beforeFrames={} cocoonFrames={} afterFrames={} epochMillis={}",phase,entry.name(),WORLD[phase],RENDERED[phase],Arrays.stream(SHOTS[phase]).sum(),MOVING[phase],CONTACTS[phase],MAX_CONTACT[phase],BEFORE[phase],COCOON[phase],AFTER[phase],System.currentTimeMillis());
    int done=phase;pendingPhase=phase;clientPhase=-2;onServer(mc.getSingleplayerServer(),()->{if(phase==done){if(entry.still()){require(actor.state().encode().equals(expectedState),"frozen stage changed server state");LogUtils.getLogger().info("MORPH052_NATIVE_COMPLETE {} name={} frozenStatePreserved=true state={}",phase,entry.name(),actor.state().encode());}if(phase+1<limit())next(mc.getSingleplayerServer(),phase+1);else{clearSlot();if(actor!=null){actor.discard();actor=null;}phase=limit();mc.getSingleplayerServer().tickRateManager().setFrozen(false);}}});
  }
  @SubscribeEvent public static void frameEnd(RenderFrameEvent.Post event){
    if(!enabled()||phase<0||phase>=limit()||clientPhase!=phase||!worldThisFrame)return;
    var mc=Minecraft.getInstance();var entry=CASES.get(phase);WORLD[phase]++;viewFrames++;
    if(!armed){if(!armRequested&&viewFrames>=12){armRequested=true;int index=phase;onServer(mc.getSingleplayerServer(),()->arm(mc.getSingleplayerServer(),index));}return;}
    if(entry.still()){
      if(viewFrames>=16&&(viewFrames-16)%4==0&&SHOTS[phase][view]<3)capture(mc);
      if(viewFrames>=27&&SHOTS[phase][view]>=3){if(view<2){view++;viewFrames=0;}else finishCase(mc);}return;
    }
    if(elapsed>=6&&elapsed!=lastMotionCapture&&elapsed%4==0&&viewFrames>=3){lastMotionCapture=elapsed;capture(mc);}
    if(complete&&viewFrames>=10)finishCase(mc);
  }
  @SubscribeEvent public static void client(ClientTickEvent.Post event){
    if(!enabled()||finished)return;var mc=Minecraft.getInstance();ticks++;
    if(fatal!=null)throw new IllegalStateException("MORPH052 server fixture failed: "+fatal);
    if(wallStarted==0)wallStarted=System.nanoTime();require(ticks<60000&&System.nanoTime()-wallStarted<2_400_000_000_000L,"actual client study timed out");
    if(mc.screen instanceof AccessibilityOnboardingScreen)mc.setScreen(new TitleScreen());
    if(!started&&mc.screen instanceof TitleScreen){
      started=true;mc.options.renderDistance().set(3);mc.options.simulationDistance().set(5);mc.options.framerateLimit().set(30);mc.options.fov().set(45);mc.options.pauseOnLostFocus=false;mc.options.hideGui=true;mc.options.tutorialStep=net.minecraft.client.tutorial.TutorialSteps.NONE;
      mc.createWorldOpenFlows().createFreshLevel("HeroRun052-"+System.currentTimeMillis(),new LevelSettings("HeroRun052",GameType.CREATIVE,false,Difficulty.PEACEFUL,true,new GameRules(),WorldDataConfiguration.DEFAULT),new WorldOptions(2552L,false,false),r->r.registryOrThrow(Registries.WORLD_PRESET).getHolderOrThrow(WorldPresets.FLAT).value().createWorldDimensions(),mc.screen);
    }
    if(!setup&&mc.player!=null&&mc.getSingleplayerServer()!=null){
      setup=true;mc.setScreen(null);ChaoRenderer.suppressNames=true;
      var window=mc.getWindow();LogUtils.getLogger().info("MORPH052_WINDOW x={} y={} screenWidth={} screenHeight={} framebufferWidth={} framebufferHeight={} epochMillis={}",window.getX(),window.getY(),window.getScreenWidth(),window.getScreenHeight(),window.getWidth(),window.getHeight(),System.currentTimeMillis());
      onServer(mc.getSingleplayerServer(),()->{var server=mc.getSingleplayerServer();var level=server.overworld();level.setDayTime(6000);level.getGameRules().getRule(GameRules.RULE_DAYLIGHT).set(false,server);level.getGameRules().getRule(GameRules.RULE_DOMOBSPAWNING).set(false,server);
        for(var at:BlockPos.betweenClosed(-12,79,-12,48,86,12))level.setBlock(at,at.getY()==79?Blocks.SMOOTH_STONE.defaultBlockState():Blocks.AIR.defaultBlockState(),3);next(server,start());});
    }
    if(mc.level!=null&&mc.player!=null&&phase>=0&&phase<limit()){
      if(clientPhase==-2&&phase==pendingPhase)return;
      if(clientPhase!=phase){clientPhase=phase;view=viewFrames=0;armRequested=false;lastMotionCapture=-1;carryBefore=CompanionBackLayer.renders();}
      camera(mc);
    }
    if(phase==limit()&&SAVED.get()==shots){
      for(int i=start();i<limit();i++)require(WORLD[i]>25,"missing actual client case");
      if(start()==0&&limit()>=10){for(int first:new int[]{0,5}){var distinct=new HashSet<Long>();for(int i=first;i<first+5;i++)distinct.add(SIGNATURES[i]);require(distinct.size()==5,"development stages reused one actual mesh shape");}}
      finished=true;LogUtils.getLogger().info("MORPH052_VISUAL_COMPLETE cases={} start={} limit={} captures={} saved={} epochMillis={} normal20TPS=true audioListening=false",limit()-start(),start(),limit(),shots,SAVED.get(),System.currentTimeMillis());mc.stop();
    }
  }
}
