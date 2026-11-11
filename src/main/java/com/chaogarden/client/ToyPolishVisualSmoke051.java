package com.chaogarden.client;

import com.chaogarden.ChaoGarden;
import com.chaogarden.core.ChaoState;
import com.chaogarden.entity.ChaoEntity;
import com.chaogarden.test.ToyVariantCases02522;
import com.chaogarden.toys.*;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.logging.LogUtils;
import java.nio.file.Files;
import java.util.*;
import java.util.function.Function;
import net.minecraft.client.*;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.AccessibilityOnboardingScreen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.inventory.ContainerScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.network.chat.Component;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.*;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import org.joml.Matrix4f;
import org.joml.Vector3f;

/** Actual production phrases and GUI item models; no separate demonstration pose or sound. */
@EventBusSubscriber(modid=ChaoGarden.MODID,value=Dist.CLIENT)
public final class ToyPolishVisualSmoke051 {
  private record Case(String id,String variant,Direction facing,String mode,String interrupt) {
    String kind(){return "chao_"+id;}
    boolean floor(){return false;}
    boolean preview(){return !floor()&&!mode.equals("natural");}
    boolean loop(){return mode.equals("loop");}
    boolean switching(){return mode.equals("switch");}
    boolean mounted(){return false;}
    boolean source(){return floor()||!preview()||mounted();}
    int finish(){return switching()?188:!interrupt.isEmpty()?102:loop()?206:180;}
    String sound(){return switch(id){
      case "rocking_horse"->"minecraft:block.wood.step";
      case "toy_car"->"chao_garden:toy.car";
      case "sonic_doll","shadow_doll"->"chao_garden:toy.doll";
      case "broom"->"chao_garden:toy.broom";
      case "picture_book"->"chao_garden:toy.book_page";
      case "pogo_stick"->"chao_garden:toy.pogo";
      case "crayons"->"chao_garden:toy.crayon";
      default->"";
    };}
    boolean silent(){return id.equals("picture_book")||id.equals("crayons")||id.endsWith("_doll");}
    int cues(){return silent()?0:id.equals("toy_car")?4:8;}
    int interruptedCues(){return silent()?0:id.equals("toy_car")?1:3;}
  }
  private static List<Case> cases(){
    var result=new ArrayList<Case>();
    for(String toy:List.of("broom","picture_book","crayons","toy_car","sonic_doll","shadow_doll")){
      result.add(new Case(toy,"baby_neutral",Direction.NORTH,"natural",""));
      result.add(new Case(toy,"adult_neutral_type0",Direction.EAST,"natural",""));
      result.add(new Case(toy,"animal_rabbit_baby",Direction.SOUTH,"once",""));
      result.add(new Case(toy,"chaos_2",Direction.WEST,"loop",""));
      result.add(new Case(toy,"adult_hero_type4",Direction.NORTH,"natural","remove"));
      result.add(new Case(toy,"animal_dragon_adult",Direction.EAST,"natural","damage"));
    }
    // Preserve the first 36 indices while adding complete phrases for the
    // long Run feet and the broad Hero Power head seen in real gameplay.
    for(String toy:List.of("broom","picture_book","crayons","toy_car","sonic_doll","shadow_doll")){
      result.add(new Case(toy,"adult_neutral_type3",Direction.SOUTH,"once",""));
      result.add(new Case(toy,"adult_hero_type4",Direction.WEST,"once",""));
    }
    result.add(new Case("broom","adult_neutral_type3",Direction.SOUTH,"switch",""));
    return List.copyOf(result);
  }
  private static final List<Case> CASES=cases();
  private static int limit(){return Math.clamp(Integer.getInteger("chao_garden.toyPolish051Limit",CASES.size()),1,CASES.size());}
  private static int start(){return Math.clamp(Integer.getInteger("chao_garden.toyPolish051Start",0),0,limit()-1);}
  private static boolean enabled(){return Boolean.getBoolean("chao_garden.toyPolish051Study");}
  private static final BlockPos TOY=new BlockPos(0,80,2);
  private static final LionfishActiveToyClip049 ACTIVE_CLIP=new LionfishActiveToyClip049();
  private static final LionfishQuietToyClip049 QUIET_CLIP=new LionfishQuietToyClip049();
  private static final int[] WORLD_FRAMES=new int[CASES.size()],ACTIVE_FRAMES=new int[CASES.size()],SOUNDS=new int[CASES.size()];
  private static final int[][] VIEW_SHOTS=new int[CASES.size()][3],TIMING_FRAMES=new int[CASES.size()][3];
  private static final float[] CONTACT_ERROR=new float[CASES.size()];
  private static final long[] AUDITED=new long[CASES.size()];
  private static final boolean[] PLACED_CHECKED=new boolean[CASES.size()];
  private static final float[] MAX_REACH=new float[CASES.size()];
  private static final int[] FOOT_CHECKS=new int[CASES.size()];
  private static final int[] EXIT_FLOOR_FRAMES=new int[CASES.size()];
  private static final long[] OUTGOING_DIAG_TICK=new long[CASES.size()];
  private static final boolean[] LOW_CAMERA_SHOT=new boolean[CASES.size()];
  private static boolean lowCameraActive;
  private static boolean supportGateProbed;
  private static final Map<String,List<Vector3f[]>> BAKED_FACES=new HashMap<>();
  private static Screen iconScreen;
  private static int iconStep=-1,iconFrames,iconShots;
  private static boolean started,setup,finished,renderedThisFrame,worldThisFrame,armRequested;
  private static int clientPhase=-1,pendingPhase=-1,cameraView=-1,viewFrames,ticks,shots,hurtFrames;
  private static final java.util.concurrent.atomic.AtomicInteger SAVED_SHOTS=new java.util.concurrent.atomic.AtomicInteger();
  private static long wallStarted,nextCapture,lastApproachCapture;
  private static ArmorStand observer;
  private static volatile int phase=-1;
  private static volatile UUID actorId;
  private static volatile long elapsed=-1,serverTime,caseAge;
  private static volatile boolean armed,caseComplete,expectedAdult;
  private static volatile String fatal;
  // Fields below are owned by the integrated-server thread.
  private static ChaoEntity actor;
  private static ToyContent.PlayGoal approachGoal;
  private static long spawned,began=-1,lastTick,playbackWall;
  private static long lastApproachProgress;
  private static Vec3 previousPosition;
  private static double walked;
  private static double progressWalked;
  private static boolean interrupted,previewProof,switched;
  private static String savedState;
  private static long savedCooldown;
  private static float interruptedHappiness;

  private static void require(boolean result,String message){if(!result)throw new IllegalStateException("TOY051 "+message);}
  private static void onServer(MinecraftServer server,Runnable task){server.execute(()->{
    try{task.run();}catch(Throwable failure){fatal=failure.toString();LogUtils.getLogger().error("TOY051_ASYNC_FAILURE",failure);}
  });}
  private static void command(MinecraftServer server,String mode){
    command(server,CASES.get(phase).id(),mode);
  }
  private static void command(MinecraftServer server,String toy,String mode){
    var player=server.getPlayerList().getPlayers().getFirst();
    try{require(server.getCommands().getDispatcher().execute("chao testtoy "+toy+" "+mode,
        player.createCommandSourceStack().withPermission(2))==1,"native dispatcher rejected command "+mode);}
    catch(com.mojang.brigadier.exceptions.CommandSyntaxException failure){throw new IllegalStateException(failure);}
  }
  private static void next(MinecraftServer server,int index){
    server.tickRateManager().setFrozen(true);
    if(actor!=null){ToyPreview.stop(actor);actor.discard();actor=null;}
    var entry=CASES.get(index);var level=server.overworld();
    level.setBlock(TOY,entry.source()?ToyContent.block(entry.kind()).defaultBlockState().setValue(ChaoToyBlock.FACING,entry.facing()):Blocks.AIR.defaultBlockState(),3);
    actorId=null;
    if(!entry.floor()){
      ChaoState state=ToyVariantCases02522.all().stream().filter(c->c.name().equals(entry.variant())).findFirst().orElseThrow().state();
      // Shared visual fixtures set expressed slots directly. Populate their
      // derived mask before the baseline so actual serialization remains exact.
      for(int part:state.animalParts)if(part>=0)state.animalPartsMask|=1<<part;
      actor=ChaoGarden.CHAO.get().create(level);require(actor!=null,"study Chao did not create");actor.setState(state);
      actor.setInvulnerable(true);actor.setNoAi(entry.loop());actor.getRandom().setSeed(18);
      // No block-contact helper is used for a free-standing hand-toy preview.
      Vec3 origin=Vec3.atBottomCenterOf(entry.preview()?(entry.mounted()?TOY.relative(entry.facing()):TOY):TOY.relative(entry.facing(),3));
      float yaw=entry.facing().getOpposite().toYRot();actor.moveTo(origin.x,origin.y,origin.z,yaw,0);
      actor.yBodyRot=actor.yBodyRotO=actor.yHeadRot=actor.yHeadRotO=yaw;
      level.addFreshEntity(actor);actor.setOnGround(true);actor.goalSelector.removeAllGoals(g->true);actor.targetSelector.removeAllGoals(g->true);
      approachGoal=null;
      if(entry.preview())actor.getPersistentData().putLong("ChaoToyNextPlay",Long.MAX_VALUE);
      else {approachGoal=new ToyContent.PlayGoal(actor);actor.goalSelector.addGoal(4,approachGoal);}
      expectedAdult=state.adult;savedState=state.encode();savedCooldown=actor.getPersistentData().getLong("ChaoToyNextPlay");
      previousPosition=actor.position();actorId=actor.getUUID();interruptedHappiness=state.happiness;
    }
    var player=server.getPlayerList().getPlayers().getFirst();player.teleportTo(4.5,82,6.5);player.setNoGravity(true);player.setDeltaMovement(Vec3.ZERO);
    spawned=lastTick=serverTime=level.getGameTime();lastApproachProgress=spawned;playbackWall=System.nanoTime();began=-1;elapsed=-1;caseAge=0;walked=progressWalked=0;
    interrupted=previewProof=switched=armed=caseComplete=false;phase=index;
    LogUtils.getLogger().info("TOY051_CASE {} toy={} variant={} facing={} mode={} interrupt={} expectedAdult={}",index,entry.id(),entry.variant(),entry.facing(),entry.mode(),entry.interrupt(),expectedAdult);
  }
  private static void arm(MinecraftServer server,int expected){
    if(phase!=expected||armed)return;var entry=CASES.get(phase);
    if(entry.floor()){armed=true;return;}
    require(actor.state().adult==expectedAdult&&!actor.isCocooned(),"fixture changed age before interaction");actor.setOnGround(true);
    if(entry.preview()){
      if(entry.switching())command(server,"toy_car","loop");else command(server,entry.loop()?"loop":"once");
      began=server.overworld().getGameTime();elapsed=0;
    }
    require(Math.abs(server.tickRateManager().tickrate()-20)<.00001f,"client study must retain the normal 20 TPS clock");
    armed=true;playbackWall=System.nanoTime();server.tickRateManager().setFrozen(false);
  }
  private static void complete(MinecraftServer server){
    var entry=CASES.get(phase);
    require(!ToyPreview.active(actor)&&actor.toyPlayingTicks()==0&&actor.toyTarget()==null,"session left a clock, reservation or preview");
    require(actor.getHeldChaoItem().isEmpty()&&actor.getMainHandItem().isEmpty(),"session created a real inventory prop");
    require(actor.state().adult==expectedAdult&&!actor.isCocooned(),"fixture changed its real age");
    require(actor.isNoAi()==entry.loop(),"original NoAI was not restored");
    require(!actor.isNoGravity()&&!actor.getPersistentData().getBoolean("ChaoToyDismountWait"),"mount left gravity or dismount recovery behind");
    if(entry.preview()){
      require(previewProof,"preview did not preserve care, age, genetics and lessons on its terminal tick");
      require(actor.getPersistentData().getLong("ChaoToyNextPlay")==savedCooldown,"preview changed the ordinary cooldown");
      require(!entry.switching()||switched,"prone-to-broom switch did not occur");
    }else{
      require(walked>.35,"natural interaction did not actually approach from the starting point");
      require(actor.state().happiness==(entry.interrupt().isEmpty()?24:interruptedHappiness),"natural reward was not applied exactly once or interruption granted a reward");
    }
    var state=server.overworld().getBlockState(TOY);
    if(entry.interrupt().equals("remove")||!entry.source())require(state.isAir(),"source-free/removed toy reappeared");
    else require(state.is(ToyContent.block(entry.kind()))&&!state.getValue(ChaoToyBlock.PLAYING)&&state.getValue(ChaoToyBlock.FRAME)==0,"source did not return idle");
    caseComplete=true;server.tickRateManager().setFrozen(true);
    LogUtils.getLogger().info("TOY051_NATIVE_COMPLETE {} toy={} elapsed={} walked={} previewStatePreserved={}",phase,entry.id(),elapsed,walked,previewProof);
  }
  @SubscribeEvent public static void server(ServerTickEvent.Post event){
    if(!enabled()||phase<0||phase>=limit()||!armed||caseComplete||CASES.get(phase).floor())return;
    var server=event.getServer();var level=server.overworld();long now=level.getGameTime();if(now==lastTick)return;
    lastTick=serverTime=now;caseAge=now-spawned;
    if(caseAge>=2800)require(false,"natural approach or interaction timed out: "+approachDebug());
    require(actor.state().adult==expectedAdult&&!actor.isCocooned(),"fixture changed age or cocooned during interaction");
    if(began<0){
      if(ToyContent.isPlaying(actor)){
        began=actor.getPersistentData().getLong("ChaoToyUntil")-160;playbackWall=System.nanoTime();
        LogUtils.getLogger().info("TOY051_NATURAL_STARTED {} walked={} waitTicks={}",phase,walked,now-spawned);
      }else{
        walked+=actor.position().subtract(previousPosition).horizontalDistance();previousPosition=actor.position();
        if(walked-progressWalked>=.02){lastApproachProgress=now;progressWalked=walked;}
        if(caseAge==1||caseAge%40==0)LogUtils.getLogger().info("TOY051_APPROACH {}",approachDebug());
        if(approachField("toy") instanceof BlockPos&&now-lastApproachProgress>400)
          require(false,"natural approach stopped making horizontal progress: "+approachDebug());
      }
    }
    if(began<0)return;elapsed=now-began;var entry=CASES.get(phase);
    if(entry.switching()&&!switched&&elapsed>=8){
      require(ToyPreview.active(actor),"initial car loop was not active for the switch");command(server,"once");switched=true;
      LogUtils.getLogger().info("TOY051_SWITCH {} from=toy_car to=broom elapsed={}",phase,elapsed);
    }
    if(!interrupted&&!entry.interrupt().isEmpty()&&elapsed>=60){
      interrupted=true;
      if(entry.interrupt().equals("damage")){actor.setInvulnerable(false);require(actor.hurt(level.damageSources().generic(),1),"real damage was rejected");}
      else require(level.destroyBlock(TOY,false),"placed source removal was rejected");
      interruptedHappiness=actor.state().happiness;
      LogUtils.getLogger().info("TOY051_INTERRUPT {} {} elapsed={}",phase,entry.interrupt(),elapsed);
    }
    if(entry.loop()&&!interrupted&&elapsed>=180){require(ToyPreview.active(actor),"loop did not cross its phrase boundary");command(server,"stop");interrupted=true;}
    if(entry.preview()&&!previewProof){
      require(savedState.equals(actor.state().encode()),"preview changed saved care, age, genes or lessons before its terminal tick");
      require(actor.getPersistentData().getLong("ChaoToyNextPlay")==savedCooldown,"preview changed cooldown");
      if(!ToyPreview.active(actor))previewProof=true;
    }
    if(elapsed>=entry.finish())complete(server);
  }
  private static Object approachField(String name){
    if(approachGoal==null)return null;
    try {var field=ToyContent.PlayGoal.class.getDeclaredField(name);field.setAccessible(true);return field.get(approachGoal);}
    catch(ReflectiveOperationException failure){return "unavailable:"+name;}
  }
  /** Read-only diagnostics of the actual production goal; never bypass pathing. */
  private static String approachDebug(){
    if(actor==null)return "case="+phase+" actor=missing";
    var navigation=actor.getNavigation();var path=navigation.getPath();
    Object selected=approachField("toy"),standing=approachField("stand");
    String room="not-selected",anchor="none",distance="none";
    if(selected instanceof BlockPos toy&&standing instanceof BlockPos stand
        &&actor.level().getBlockState(toy).getBlock() instanceof ChaoToyBlock block){
      var target=ToyContent.stationaryAnchor(actor,toy,stand);
      float yaw=Direction.getNearest(toy.getX()+.5-target.x,0,toy.getZ()+.5-target.z).toYRot();
      room=Boolean.toString(ToyContent.proneSpaceReady(actor,block.toy,target,yaw,toy));
      anchor=target.toString();distance=Double.toString(actor.position().distanceTo(target));
    }
    return "case="+phase+" kind="+CASES.get(phase).kind()+" age="+caseAge+" walked="+walked+" lastProgressAge="+(actor.level().getGameTime()-lastApproachProgress)
        +" pos="+actor.position()+" velocity="+actor.getDeltaMovement()+" onGround="+actor.onGround()
        +" noAi="+actor.isNoAi()+" noGravity="+actor.isNoGravity()+" runPoints="+actor.state().points[ChaoState.RUN]
        +" runLevel="+actor.state().levels[ChaoState.RUN]+" moveSpeed="+actor.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED)
        +" mayPlay="+ToyContent.mayPlay(actor)+" navDone="+navigation.isDone()+" navTarget="+navigation.getTargetPos()
        +" path="+(path==null?"none":path.getNextNodeIndex()+"/"+path.getNodeCount()+" next="+(path.isDone()?"done":path.getNextNodePos()))
        +" selected="+selected+" stand="+standing+" anchor="+anchor+" distance="+distance+" roomReady="+room
        +" engaged="+approachField("engaged")+" nextCheck="+approachField("nextCheck")
        +" expiry="+approachField("approachExpiry")+" hardExpiry="+approachField("approachHardExpiry")
        +" progress="+approachField("approachProgress")+" source="+actor.level().getBlockState(TOY)
        +" at="+actor.level().getBlockState(actor.blockPosition())+" below="+actor.level().getBlockState(actor.blockPosition().below());
  }
  @SubscribeEvent public static void restoreCamera(ClientTickEvent.Pre event){if(enabled()){
    var mc=Minecraft.getInstance();if(mc.player!=null&&mc.getCameraEntity()!=mc.player)mc.setCameraEntity(mc.player);
  }}
  private static ChaoEntity observed(Minecraft mc){
    if(mc.level==null||actorId==null)return null;
    for(var e:mc.level.entitiesForRendering())if(e instanceof ChaoEntity c&&c.getUUID().equals(actorId))return c;
    return null;
  }
  private static boolean clientSourceReady(Minecraft mc){
    if(mc.level==null||phase<0||phase>=limit())return false;
    var entry=CASES.get(phase);var block=mc.level.getBlockState(TOY);
    if(!entry.source())return block.isAir();
    return block.is(ToyContent.block(entry.kind()))&&block.getRenderShape()==RenderShape.MODEL
        &&block.getValue(ChaoToyBlock.FACING)==entry.facing()&&!block.getValue(ChaoToyBlock.PLAYING)
        &&block.getValue(ChaoToyBlock.FRAME)==0;
  }
  private static void camera(Minecraft mc){
    var entry=CASES.get(phase);var c=observed(mc);
    if(!entry.floor()&&c==null)return;
    if(observer==null||observer.level()!=mc.level){observer=new ArmorStand(mc.level,0,0,0);observer.setInvisible(true);observer.setNoGravity(true);}
    int view=entry.floor()?Math.clamp(cameraView,0,2):elapsed<0?0:!entry.interrupt().isEmpty()?(elapsed<30?0:elapsed<60?1:2):elapsed<52?0:elapsed<108?1:2;
    Vec3 forward=Vec3.directionFromRotation(0,entry.facing().getOpposite().toYRot()),side=new Vec3(-forward.z,0,forward.x),focus,eye;
    if(entry.floor()){
      focus=Vec3.atBottomCenterOf(TOY).add(0,.08,0);
      eye=focus.add(forward.scale(view==0?1.35:view==1?.15:-1.10)).add(side.scale(view==0?.85:view==1?1.65:-.9)).add(0,view==0?.9:view==1?.18:1.45,0);
    }else{
      focus=c.position().add(forward.scale(.15)).add(0,expectedAdult?.40:.34,0);
      if(elapsed<0)focus=focus.lerp(Vec3.atBottomCenterOf(TOY).add(0,.3,0),.35);
      boolean closeFace=entry.id().equals("shadow_doll")&&elapsed>=28&&elapsed<52;
      boolean ride=entry.mounted();double distance=closeFace?.95:ride?2.1:1.7;
      if(closeFace)focus=c.position().add(forward.scale(.20)).add(0,expectedAdult?.43:.34,0);
      eye=focus.add(forward.scale(view==0?distance:view==1?.02:-.35)).add(side.scale(view==0?.9:view==1?distance:-distance)).add(0,view==1?.35:view==2?.7:.65,0);
      if(closeFace&&view==0)eye=focus.add(forward.scale(distance)).add(side.scale(.24)).add(0,.20,0);
    }
    boolean low=entry.id().equals("toy_car")&&armed&&elapsed>=40&&elapsed<=44&&!LOW_CAMERA_SHOT[phase];
    if(low){focus=c.position().add(forward.scale(.30)).add(0,.08,0);eye=focus.add(side.scale(1.1)).add(forward.scale(.25));}
    var delta=focus.subtract(eye);float yaw=(float)(Math.toDegrees(Math.atan2(delta.z,delta.x))-90),pitch=low?0:(float)-Math.toDegrees(Math.atan2(delta.y,Math.hypot(delta.x,delta.z)));
    observer.moveTo(eye.x,eye.y-observer.getEyeHeight(),eye.z,yaw,pitch);observer.xo=observer.xOld=observer.getX();observer.yo=observer.yOld=observer.getY();observer.zo=observer.zOld=observer.getZ();
    observer.yRotO=yaw;observer.xRotO=pitch;observer.setYHeadRot(yaw);observer.yHeadRotO=yaw;
    mc.options.setCameraType(CameraType.FIRST_PERSON);mc.setCameraEntity(observer);
    if(cameraView!=view||lowCameraActive!=low){cameraView=view;viewFrames=0;}lowCameraActive=low;
  }
  private static boolean active(ChaoEntity c){return c.toyPlayingKind().equals(RockingHorsePlayRules.KIND)?ChaoRockingHorseAnimation.active(c):
      ActiveToyPlayRules049.supports(c.toyPlayingKind())?ChaoActiveToyAnimation049.active(c):ChaoQuietToyAnimation049.active(c);}
  private static void contact(ChaoEntity c,ChaoModel m,float partial){
    float t=GardenToyPerformance049.phase(c,partial);
    float w=GardenToyPerformance049.presence(c,partial),scale=c.state().immortal?.86f:c.state().adult?.80f:.64f;
    String kind=c.toyPlayingKind();
    geometry(c,m,t,w,scale);
    if(kind.equals(ActiveToyPlayRules049.CAR)){if(w<.8f)return;}
    else if(t<16||t>=144)return;
    for(boolean left:new boolean[]{false,true}){
      var palm=m.toyPalm(left);require(palm!=null,"live active toy has no visible hand");
      var actual=ChaoTriangleAnimation.palm(m::transformAccessoryAnchor,palm,left);Vector3f target;
      if(kind.equals(ActiveToyPlayRules049.CAR)&&left)target=ChaoActiveToyAnimation049.carSupportTarget(m::transformAccessoryAnchor,ChaoProneToyPose051.runtimeBounds(c,m));
      else if(ActiveToyPlayRules049.supports(kind))target=ChaoActiveToyAnimation049.gripTarget(ChaoActiveToyAnimation049.propMatrix(m::transformAccessoryAnchor,kind,t,w,ACTIVE_CLIP),kind,left);
      else if(kind.equals(QuietToyPlayRules049.BROOM))target=ChaoQuietToyAnimation049.broomTarget(ChaoQuietToyAnimation049.broomMatrix(w,QUIET_CLIP.sweep(t),scale),left);
      else if(kind.equals(QuietToyPlayRules049.BOOK))target=ChaoQuietToyAnimation049.bookTarget(ChaoQuietToyAnimation049.bookMatrix(m::transformAccessoryAnchor,w,ChaoProneToyPose051.runtimeBounds(c,m)),left);
      else if(kind.equals(QuietToyPlayRules049.CRAYONS)){
        var paper=ChaoQuietToyAnimation049.paperMatrix(m::transformAccessoryAnchor,w);
        target=left?ChaoQuietToyAnimation049.paperSupport(paper):ChaoQuietToyAnimation049.drawingHand(paper,t,QUIET_CLIP.stroke(t),QUIET_CLIP.pen(t));
      }else target=horseTarget(c,t,partial,scale,left);
      float error=actual.distance(target);CONTACT_ERROR[phase]=Math.max(CONTACT_ERROR[phase],error);
      require(actual.isFinite()&&error<.065f,"live hand lost toy contact: "+kind+" left="+left+" error="+error);
      var arm=m.officialPoses().get(left?"arm_left":"arm_right");MAX_REACH[phase]=Math.max(MAX_REACH[phase],arm.reach);require(arm.reach<=3.201f,"live toy forearm is too long");
    }
    if(kind.equals(RockingHorsePlayRules.KIND)){
      var body=m.accessoryBounds("body");if(body==null)return;var frame=new PoseStack();m.transformAccessoryAnchor("body",frame);
      float angle=RockingHorsePlayRules.angle(t);
      var yaw=new Matrix4f().rotateY(ChaoRockingHorseAnimation.yawCorrection(c,partial));
      var up=yaw.transformDirection(new Vector3f(0,-(float)Math.cos(angle),-(float)Math.sin(angle)));
      float minimum=Float.POSITIVE_INFINITY;
      for(int n=0;n<8;n++)minimum=Math.min(minimum,frame.last().pose().transformPosition(new Vector3f(body[(n&1)==0?0:3],body[(n&2)==0?1:4],body[(n&4)==0?2:5]).div(16)).dot(up));
      var target=horsePoint(c,t,partial,scale,0,RockingHorsePlayRules.SEAT_Y+.006f,ChaoRockingHorseAnimation.seatZ(body,scale));
      require(Math.abs(minimum-target.dot(up))<.025f,"live rider no longer rests on the actual saddle");
      var hip=frame.last().pose().transformPosition(new Vector3f((body[0]+body[3])/32,body[4]/16,(body[2]+body[5])/32));
      var forward=yaw.transformDirection(new Vector3f(0,(float)Math.sin(angle),-(float)Math.cos(angle)));
      var across=yaw.transformDirection(new Vector3f(1,0,0));
      require(Math.abs(hip.dot(forward)-target.dot(forward))<.025f&&Math.abs(hip.dot(across)-target.dot(across))<.025f,"live rider slid away from its saddle centre");
    }
  }
  private static Vector3f horsePoint(ChaoEntity c,float t,float partial,float scale,float x,float y,float z){
    var at=c.toyTarget();var state=c.level().getBlockState(at);var forward=state.getValue(ChaoToyBlock.FACING).getOpposite();var p=c.getPosition(partial);
    float dx=(float)(p.x-at.getX()-.5),dz=(float)(p.z-at.getZ()-.5);
    var target=ChaoRockingHorseAnimation.modelPoint(x,y,z,t,scale,dx*forward.getStepZ()-dz*forward.getStepX(),(float)p.y-at.getY(),dx*forward.getStepX()+dz*forward.getStepZ());
    return new Matrix4f().rotateY(ChaoRockingHorseAnimation.yawCorrection(c,partial)).transformPosition(target);
  }
  private static Vector3f horseTarget(ChaoEntity c,float t,float partial,float scale,boolean left){return horsePoint(c,t,partial,scale,left?RockingHorsePlayRules.GRIP_X:-RockingHorsePlayRules.GRIP_X,RockingHorsePlayRules.GRIP_Y+.005f,RockingHorsePlayRules.GRIP_Z-.025f);}

  /** Actual baked NONE-context faces, including the ItemRenderer's centering. */
  private static List<Vector3f[]> faces(String path){
    return BAKED_FACES.computeIfAbsent(path,key->{
      var model=Minecraft.getInstance().getModelManager().getModel(ModelResourceLocation.standalone(
          ResourceLocation.fromNamespaceAndPath(ChaoGarden.MODID,key)));
      var result=new ArrayList<Vector3f[]>();
      var sides=new ArrayList<Direction>();sides.add(null);sides.addAll(List.of(Direction.values()));
      for(var side:sides)for(var quad:model.getQuads(null,side,RandomSource.create(51))){
        int[] raw=quad.getVertices();int stride=raw.length/4;var vertices=new Vector3f[4];
        for(int i=0;i<4;i++)vertices[i]=new Vector3f(Float.intBitsToFloat(raw[i*stride]),
            Float.intBitsToFloat(raw[i*stride+1]),Float.intBitsToFloat(raw[i*stride+2])).sub(.5f,.5f,.5f);
        result.add(new Vector3f[]{vertices[0],vertices[1],vertices[2]});
        if(vertices[2].distanceSquared(vertices[3])>1e-12f)result.add(new Vector3f[]{vertices[0],vertices[2],vertices[3]});
      }
      require(!result.isEmpty(),"actual baked toy faces missing: "+key);return result;
    });
  }
  private static void grounded(String name,Matrix4f transform,float maximumGap){
    float low=Float.POSITIVE_INFINITY,high=Float.NEGATIVE_INFINITY;
    for(var triangle:faces(name))for(var source:triangle){float y=transform.transformPosition(new Vector3f(source)).y;low=Math.min(low,y);high=Math.max(high,y);}
    float floor=ChaoProneToyPose051.FLOOR_Y;
    require(high<=floor+.006f&&floor-high<maximumGap,"actual toy left floor: "+name+" y="+low+".."+high+" floor="+floor);
  }
  /** A separating-axis test in the real foot bone's local coordinates. */
  private static boolean intersects(Vector3f[] source,Matrix4f transform,float[] bounds){
    var centre=new Vector3f((bounds[0]+bounds[3])/32,(bounds[1]+bounds[4])/32,(bounds[2]+bounds[5])/32);
    var half=new Vector3f((bounds[3]-bounds[0])/32,(bounds[4]-bounds[1])/32,(bounds[5]-bounds[2])/32);
    Vector3f[] v={transform.transformPosition(new Vector3f(source[0])).sub(centre),
        transform.transformPosition(new Vector3f(source[1])).sub(centre),transform.transformPosition(new Vector3f(source[2])).sub(centre)};
    Vector3f[] edges={new Vector3f(v[1]).sub(v[0]),new Vector3f(v[2]).sub(v[1]),new Vector3f(v[0]).sub(v[2])};
    var axes=new ArrayList<Vector3f>();axes.add(new Vector3f(1,0,0));axes.add(new Vector3f(0,1,0));axes.add(new Vector3f(0,0,1));
    axes.add(new Vector3f(edges[0]).cross(edges[1]));
    for(var edge:edges)for(var axis:List.of(new Vector3f(1,0,0),new Vector3f(0,1,0),new Vector3f(0,0,1)))axes.add(new Vector3f(edge).cross(axis));
    for(var axis:axes){
      if(axis.lengthSquared()<1e-14f)continue;
      float a=v[0].dot(axis),b=v[1].dot(axis),c=v[2].dot(axis),radius=half.x*Math.abs(axis.x)+half.y*Math.abs(axis.y)+half.z*Math.abs(axis.z);
      float tolerance=.0002f*(float)Math.sqrt(axis.lengthSquared());
      if(Math.min(a,Math.min(b,c))>radius-tolerance||Math.max(a,Math.max(b,c))<-radius+tolerance)return false;
    }
    return true;
  }
  private static void clearOfFeet(ChaoEntity c,ChaoModel m,String name,Matrix4f prop){
    Function<String,float[]> bounds=ChaoProneToyPose051.runtimeBounds(c,m);
    for(String bone:List.of("foot_left",m.officialPoses().containsKey("foot_right")?"foot_right":"feet_bone_right")){
      if(!m.officialPoses().containsKey(bone))continue;var box=bounds.apply(bone);if(box==null)continue;
      var at=new PoseStack();m.transformAccessoryAnchor(bone,at);var matrix=new Matrix4f(at.last().pose()).invert().mul(prop);
      for(var triangle:faces(name))require(!intersects(triangle,matrix,box),"actual held toy clips foot: "+name+" bone="+bone+" case="+phase);
      FOOT_CHECKS[phase]++;
    }
  }
  private static void geometry(ChaoEntity c,ChaoModel m,float t,float weight,float scale){
    String kind=c.toyPlayingKind();boolean full=weight>=.999f;
    if(kind.equals(QuietToyPlayRules049.CRAYONS)||kind.equals(ActiveToyPlayRules049.CAR)){
      var bounds=ChaoProneToyPose051.runtimeBounds(c,m);
      for(String bone:List.of("body","skull","foot_left",m.officialPoses().containsKey("foot_right")?"foot_right":"feet_bone_right")){
        if(!m.officialPoses().containsKey(bone))continue;var box=ChaoProneToyPose051.worldBounds(m::transformAccessoryAnchor,bounds,bone);if(box==null)continue;
        require(box[4]<=ChaoProneToyPose051.FLOOR_Y+.025f,"prone source part penetrates floor: "+bone+" y="+box[4]);
        if(full&&bone.equals("body"))require(ChaoProneToyPose051.FLOOR_Y-box[4]<.03f,"prone belly is not resting at floor");
      }
      if(full)require(Math.abs(m.officialPoses().get("body").rx)>1.15f,"active prone interaction did not turn onto its stomach");
      if(full&&kind.equals(ActiveToyPlayRules049.CAR)){
        var support=ChaoProneToyPose051.worldBounds(m::transformAccessoryAnchor,bounds,"hand_left");
        require(support!=null&&Math.abs(support[4]-ChaoProneToyPose051.FLOOR_Y)<.011f,
            "actual supporting palm is not resting on the floor: "+Arrays.toString(support));
      }
    }
    // Visibility-weighted objects must clear the actual feet during entry and
    // exit as well as during the full phrase. A zero-scale hidden prop is not
    // geometry and is deliberately excluded.
    if(kind.equals(QuietToyPlayRules049.BOOK)){
      var book=ChaoQuietToyAnimation049.bookMatrix(m::transformAccessoryAnchor,weight,ChaoProneToyPose051.runtimeBounds(c,m));
      if(Math.abs(book.determinant3x3())>1e-8f){
        clearOfFeet(c,m,"item/chao_quiet_hand_book_left",ChaoQuietToyAnimation049.bookCover(book,weight,true));
        clearOfFeet(c,m,"item/chao_quiet_hand_book_right",ChaoQuietToyAnimation049.bookCover(book,weight,false));
        if(QuietToyPlayRules049.turning(t))clearOfFeet(c,m,"item/chao_quiet_hand_book_page",new Matrix4f(book).translate(0,-.012f,0).rotateZ(-(float)Math.PI*QUIET_CLIP.page(t)));
      }
    }else if(kind.equals(ActiveToyPlayRules049.SONIC)||kind.equals(ActiveToyPlayRules049.SHADOW)){
      var prop=ChaoActiveToyAnimation049.propMatrix(m::transformAccessoryAnchor,kind,t,weight,ACTIVE_CLIP);
      if(Math.abs(prop.determinant3x3())>1e-8f)clearOfFeet(c,m,"item/"+kind+"_hand",prop);
    }
    if(kind.equals(ActiveToyPlayRules049.CAR))grounded("item/chao_toy_car_hand",ChaoActiveToyAnimation049.propMatrix(m::transformAccessoryAnchor,kind,t,weight,ACTIVE_CLIP),.025f);
    if(!full)return;
    if(kind.equals(QuietToyPlayRules049.BROOM))grounded("item/chao_quiet_hand_broom",ChaoQuietToyAnimation049.broomMatrix(weight,QUIET_CLIP.sweep(t),scale),.045f);
    else if(kind.equals(QuietToyPlayRules049.CRAYONS)){
      var paper=ChaoQuietToyAnimation049.paperMatrix(m::transformAccessoryAnchor,weight);
      grounded("item/chao_quiet_hand_paper_0",paper,.04f);
      grounded("item/chao_quiet_hand_crayon_box",ChaoQuietToyAnimation049.boxMatrix(weight,scale),.04f);
      if(QUIET_CLIP.pen(t)>.99f){
        var crayon=ChaoQuietToyAnimation049.crayonMatrix(m::transformAccessoryAnchor,m.toyPalm(false),paper,t,QUIET_CLIP.stroke(t),QUIET_CLIP.pen(t),weight)
            .translate(new Vector3f(ChaoQuietToyAnimation049.CRAYON_GRIP).negate());
        var actual=crayon.transformPosition(new Vector3f(ChaoQuietToyAnimation049.CRAYON_TIP));
        var wanted=ChaoQuietToyAnimation049.penTip(paper,t,QUIET_CLIP.stroke(t),QUIET_CLIP.pen(t));
        require(actual.distance(wanted)<.04f,"actual crayon tip lost floor-paper contact: "+actual.distance(wanted));
      }
    }
  }
  private static void outgoingFloor(ChaoEntity c,ChaoModel model,float partial){
    var entry=CASES.get(phase);boolean prone=entry.id().equals("toy_car")||entry.id().equals("crayons")||entry.switching();
    if(!prone)return;
    long end=entry.switching()?8:!entry.interrupt().isEmpty()?60:entry.loop()?180:160;
    if(elapsed>=end-2&&elapsed<=end+10&&OUTGOING_DIAG_TICK[phase]!=elapsed){
      OUTGOING_DIAG_TICK[phase]=elapsed;
      LogUtils.getLogger().info("TOY051_OUTGOING {}",floorDebug(c,model,false,partial));
    }
    if(elapsed<end||elapsed>end+12)return;
    var bounds=ChaoProneToyPose051.runtimeBounds(c,model);
    for(String bone:List.of("body","skull","foot_left",model.officialPoses().containsKey("foot_right")?"foot_right":"feet_bone_right")){
      if(!model.officialPoses().containsKey(bone))continue;
      var box=ChaoProneToyPose051.worldBounds(model::transformAccessoryAnchor,bounds,bone);if(box==null)continue;
      if(box[4]>ChaoProneToyPose051.FLOOR_Y+.025f){
        String debug=floorDebug(c,model,true,partial);
        LogUtils.getLogger().error("TOY051_OUTGOING_FLOOR_FAILURE bone={} worldBounds={} {}",bone,Arrays.toString(box),debug);
        require(false,"outgoing prone blend penetrates floor: case="+phase+" bone="+bone+" elapsed="+elapsed+" y="+box[4]+" "+debug);
      }
    }
    EXIT_FLOOR_FRAMES[phase]++;
  }
  private static Object debugField(Object object,String name){
    if(object==null)return null;
    try {var field=object.getClass().getDeclaredField(name);field.setAccessible(true);return field.get(object);}
    catch(ReflectiveOperationException failure){return "unavailable:"+name;}
  }
  private static String poseDebug(ChaoMeshAsset.Pose pose){
    if(pose==null)return "absent";
    return "xyz="+Arrays.toString(new float[]{pose.x,pose.y,pose.z})+" rot="+Arrays.toString(new float[]{pose.rx,pose.ry,pose.rz})
        +" scale="+Arrays.toString(new float[]{pose.sx,pose.sy,pose.sz})+" reach="+pose.reach+" visible="+pose.visible;
  }
  /** Inspect the actual cached state and gating flags without changing them. */
  private static String floorDebug(ChaoEntity c,ChaoModel model,boolean completeState,float partial){
    Object rig=debugField(model,"official"),cache=debugField(rig,"transitions"),transition=cache instanceof Map<?,?> states?states.get(c.getUUID()):null;
    var body=c.getBoundingBox();
    var supportSlab=new net.minecraft.world.phys.AABB(body.minX+.01,body.minY-.025,body.minZ+.01,body.maxX-.01,body.minY+.015,body.maxZ-.01);
    boolean worldSupport=c.level().getBlockCollisions(c,supportSlab).iterator().hasNext();
    Object productionSupport;
    try {var method=ChaoProneToyPose051.class.getDeclaredMethod("floorSupported",ChaoEntity.class);method.setAccessible(true);productionSupport=method.invoke(null,c);}
    catch(ReflectiveOperationException unavailable){productionSupport="unavailable";}
    var text=new StringBuilder("case=").append(phase).append(" elapsed=").append(elapsed)
        .append(" sourceGeometry=").append(model.officialGeometryKey()).append(" currentGeometry=").append(model.gameplayFormKey())
        .append(" motion=").append(model.officialMotion()).append(" emotion=").append(model.officialEmotion())
        .append(" actorTick=").append(c.tickCount).append(" renderPartial=").append(partial).append(" gameTime=").append(c.level().getGameTime())
        .append(" pos=").append(c.position()).append(" delta=").append(c.getDeltaMovement())
        .append(" onGround=").append(c.onGround()).append(" inWater=").append(c.isInWater())
        .append(" attached=").append(c.isCompanionAttached()).append(" noGravity=").append(c.isNoGravity())
        .append(" noAi=").append(c.isNoAi()).append(" hurt=").append(c.hurtTime)
        .append(" toy=").append(c.toyPlayingKind()).append(" toyTicks=").append(c.toyPlayingTicks())
        .append(" behavior=").append(c.getBehavior()).append(" below=").append(c.level().getBlockState(c.blockPosition().below()))
        .append(" worldSupport=").append(worldSupport).append(" productionFloorSupported=").append(productionSupport).append(" supportSlab=").append(supportSlab)
        .append(" slabLiquid=").append(c.level().containsAnyLiquid(supportSlab))
        .append(" rigKey=").append(debugField(rig,"key")).append(" sampledFrames=").append(debugField(rig,"sampledFrames"));
    if(rig instanceof OfficialChaoRig actual)text.append(" recoveringProne=").append(actual.recoveringProne(c.getUUID()));
    for(String field:List.of("mode","emotion","geometry","age","started","lastProne","fromProne"))
      text.append(" cache.").append(field).append('=').append(debugField(transition,field));
    for(String field:List.of("from","last")){
      Object value=debugField(transition,field);text.append(" cache.").append(field).append("Present=").append(value instanceof Map<?,?>);
      if(value instanceof Map<?,?> values)for(String bone:List.of("root","body","foot_left","foot_right","feet_bone_right")){
        var item=values.get(bone);if(item instanceof float[] array)text.append(" cache.").append(field).append('.').append(bone).append('=').append(Arrays.toString(array));
      }
    }
    for(String bone:List.of("root","body","skull","foot_left","foot_right","feet_bone_right"))
      text.append(" pose.").append(bone).append('{').append(poseDebug(model.officialPoses().get(bone))).append('}');
    var volumes=ChaoProneToyPose051.runtimeBounds(c,model);
    for(String bone:List.of("body","skull","foot_left","foot_right","feet_bone_right"))
      if(model.officialPoses().containsKey(bone))text.append(" worldBounds.").append(bone).append('=')
          .append(Arrays.toString(ChaoProneToyPose051.worldBounds(model::transformAccessoryAnchor,volumes,bone)));
    var state=c.state();text.append(" state{adult=").append(state.adult).append(" immortal=").append(state.immortal)
        .append(" type=").append(state.primaryType).append(" second=").append(state.secondaryType)
        .append(" align=").append(state.align).append(" character=").append(state.character)
        .append(" sf=").append(state.swimFly).append(" rp=").append(state.runPower)
        .append(" parts=").append(Arrays.toString(state.animalParts)).append('}');
    if(completeState)text.append(" encodedState=").append(state.encode());
    return text.toString();
  }
  /** An unspawned clone exercises only the support predicate, never the live actor. */
  private static void probeFloorSupport(ChaoEntity actor){
    if(supportGateProbed)return;
    var level=actor.level();var position=actor.position();var originalBox=actor.getBoundingBox();
    String savedState=actor.state().encode();var originalFloor=level.getBlockState(actor.blockPosition().below());
    var probe=ChaoGarden.CHAO.get().create(level);require(probe!=null,"support-gate probe did not create");
    require(level.getEntity(probe.getId())==null,"support-gate clone was unexpectedly in the world");
    double[] offsets={0,.02,.04,1,-.5};boolean[] flags={false,false,true,true,true},expected={true,true,false,false,false};
    for(int i=0;i<offsets.length;i++){
      probe.moveTo(position.x,position.y+offsets[i],position.z);probe.setBoundingBox(originalBox.move(0,offsets[i],0));probe.setOnGround(flags[i]);
      boolean actual=ChaoProneToyPose051.floorSupported(probe);
      LogUtils.getLogger().info("TOY051_SUPPORT_GATE_PROBE offsetY={} onGround={} supported={} expected={} box={}",offsets[i],flags[i],actual,expected[i],probe.getBoundingBox());
      require(actual==expected[i],"actual client support gate differs at offset="+offsets[i]+" onGround="+flags[i]+" supported="+actual);
    }
    require(level.getEntity(probe.getId())==null&&actor.position().equals(position)&&actor.getBoundingBox().equals(originalBox)
        &&actor.state().encode().equals(savedState)&&level.getBlockState(actor.blockPosition().below()).equals(originalFloor),
        "unspawned support-gate probe changed the live actor or world");
    supportGateProbed=true;LogUtils.getLogger().info("TOY051_SUPPORT_GATE_COMPLETE probes=5 actorAndWorldUnchanged=true unspawned=true");
  }
  @SubscribeEvent public static void frameBegin(RenderFrameEvent.Pre event){if(enabled()){renderedThisFrame=worldThisFrame=false;}}
  @SubscribeEvent public static void rendered(RenderLivingEvent.Post<?,?> event){
    if(!enabled()||phase<0||phase>=limit()||CASES.get(phase).floor()||SceneShaderPack.shadows()||!(event.getEntity() instanceof ChaoEntity c)||!c.getUUID().equals(actorId))return;
    var mc=Minecraft.getInstance();if(mc.screen!=null||mc.getOverlay()!=null||mc.getCameraEntity()!=observer)return;
    require(c.state().adult==expectedAdult&&!c.isCocooned(),"rendered fixture changed age");var model=(ChaoModel)event.getRenderer().getModel();require(model.officialPosesFinite(),"rendered toy pose is not finite");
    renderedThisFrame=true;
    probeFloorSupport(c);
    outgoingFloor(c,model,event.getPartialTick());
    if(active(c)){
      ACTIVE_FRAMES[phase]++;
      if(AUDITED[phase]!=mc.level.getGameTime()){AUDITED[phase]=mc.level.getGameTime();contact(c,model,Math.clamp(event.getPartialTick(),0,1));}
    }
    if(CASES.get(phase).interrupt().equals("damage")&&c.hurtTime>0){require(model.officialEmotion()==1&&!active(c),"hurt did not replace toy playing");hurtFrames++;}
  }
  @SubscribeEvent public static void renderedWorld(RenderLevelStageEvent event){
    if(!enabled()||phase<0||phase>=limit()||event.getStage()!=RenderLevelStageEvent.Stage.AFTER_LEVEL||SceneShaderPack.shadows())return;
    var mc=Minecraft.getInstance();if(!armed&&!clientSourceReady(mc))return;
    if(mc.screen==null&&mc.getOverlay()==null&&mc.level!=null&&mc.getCameraEntity()==observer&&(CASES.get(phase).floor()||renderedThisFrame))worldThisFrame=true;
  }
  @SubscribeEvent public static void heard(net.neoforged.neoforge.client.event.sound.PlaySoundEvent event){
    if(!enabled()||phase<0||phase>=limit()||!armed||elapsed<0)return;
    var sound=event.getOriginalSound();if(sound==null)return;String location=sound.getLocation().toString();
    if(CASES.get(phase).silent())require(!location.startsWith("chao_garden:toy.")&&!location.equals("minecraft:item.book.page_turn"),"silent toy dispatched an interaction cue: "+location);
    if(CASES.get(phase).switching())require(!location.equals("chao_garden:toy.car"),"old car clock dispatched a cue after the early switch");
    if(location.equals(CASES.get(phase).sound()))SOUNDS[phase]++;
  }
  private static void checkFloor(Minecraft mc){
    var entry=CASES.get(phase);var state=mc.level.getBlockState(TOY);
    require(state.is(ToyContent.block(entry.kind()))&&state.getRenderShape()==RenderShape.MODEL&&!state.getValue(ChaoToyBlock.PLAYING),"floor source is missing or hidden");
    var shape=state.getShape(mc.level,TOY).bounds();require(Math.abs(shape.minY)<1e-7,"floor collision hovers");
    var quads=mc.getBlockRenderer().getBlockModel(state).getQuads(state,null,RandomSource.create(49));require(!quads.isEmpty(),"floor model has no actual baked faces");
    float min=Float.POSITIVE_INFINITY,max=Float.NEGATIVE_INFINITY;var low=new Vector3f(Float.POSITIVE_INFINITY);var high=new Vector3f(Float.NEGATIVE_INFINITY);
    for(var quad:quads){int[] data=quad.getVertices();int stride=data.length/4;for(int i=0;i<4;i++){var v=new Vector3f(Float.intBitsToFloat(data[i*stride]),Float.intBitsToFloat(data[i*stride+1]),Float.intBitsToFloat(data[i*stride+2]));min=Math.min(min,v.y);max=Math.max(max,v.y);low.min(v);high.max(v);}}
    require(Math.abs(min)<.00001f&&max<=shape.maxY+.00001,"actual baked floor geometry floats or leaves collision bounds: "+min+".."+max);
    if(entry.id().equals("toy_car")){
      // Independently measured from the delivered 0.25.50 OBJ, rather than its
      // approximate collision envelope: source bounds X=.305722753..694277247,
      // Y=0...3, Z=.199528470..800471530. The new asset is exactly 70% of these.
      boolean quarterTurn=entry.facing().getAxis()==Direction.Axis.X;
      var expected=new Vector3f(quarterTurn?.600943060f:.388554494f,.3f,quarterTurn?.388554494f:.600943060f).mul(.7f);
      var size=new Vector3f(high).sub(low);
      require(Math.abs(size.x-expected.x)<.000002f&&Math.abs(size.y-expected.y)<.000002f&&Math.abs(size.z-expected.z)<.000002f,
          "resting car differs from the exact 70% source reduction: actual="+size+" expected="+expected);
    }
    LogUtils.getLogger().info("TOY051_FLOOR_BOUND {} toy={} facing={} actualY={}..{} shapeY={}..{} quads={}",phase,entry.id(),entry.facing(),min,max,shape.minY,shape.maxY,quads.size());
  }
  private static void capture(Minecraft mc,boolean approach){
    var entry=CASES.get(phase);String name=String.format("toy051-%02d-%s-%s-%s-%s-v%d.png",phase,entry.id(),entry.floor()?"floor":entry.variant(),entry.facing().getSerializedName(),entry.floor()?"static":approach?String.format("approach%03d",caseAge):String.format("t%03d",elapsed),cameraView);
    int index=phase,view=cameraView;long t=elapsed;
    Screenshot.grab(mc.gameDirectory,name,mc.getMainRenderTarget(),message->{require(Files.isRegularFile(mc.gameDirectory.toPath().resolve("screenshots").resolve(name)),"capture did not save");SAVED_SHOTS.incrementAndGet();LogUtils.getLogger().info("TOY051_CAPTURE {} case={} tick={} view={} approach={}",name,index,t,view,approach);});
    shots++;if(!approach)VIEW_SHOTS[phase][cameraView]++;viewFrames=0;
  }
  private static void finishCase(Minecraft mc){
    var entry=CASES.get(phase);if(!entry.floor()){
      require(ACTIVE_FRAMES[phase]>=20,"production interaction was not actually rendered");
      for(int i=0;i<3;i++){require(VIEW_SHOTS[phase][i]>=3,"interaction missed a useful camera angle");require(TIMING_FRAMES[phase][i]>=3,"entry/play/exit were not all rendered");}
      int expected=entry.interrupt().isEmpty()?entry.cues():entry.interruptedCues();
      require(SOUNDS[phase]==expected,"actual client cues differ from phrase or continued after interruption: "+SOUNDS[phase]+" expected "+expected);
      if(entry.id().equals("picture_book")||entry.id().endsWith("_doll"))require(FOOT_CHECKS[phase]>0,"held object was never checked against the real feet");
      if(entry.id().equals("toy_car")||entry.id().equals("crayons")||entry.switching())require(EXIT_FLOOR_FRAMES[phase]>0,"outgoing prone floor recovery was never actually rendered");
      require((System.nanoTime()-playbackWall)/1_000_000_000.0>=entry.finish()/20.0-.65,
          "production phrase ran faster than the normal 20 TPS clock");
    }
    LogUtils.getLogger().info("TOY051_RENDER_COMPLETE {} toy={} worldFrames={} activeFrames={} captures={} sounds={} maxPalmError={} maxReachPixels={} footChecks={} outgoingFloorFrames={} normalSpeed={} wallSeconds={}",phase,entry.id(),WORLD_FRAMES[phase],ACTIVE_FRAMES[phase],Arrays.stream(VIEW_SHOTS[phase]).sum(),SOUNDS[phase],CONTACT_ERROR[phase],MAX_REACH[phase],FOOT_CHECKS[phase],EXIT_FLOOR_FRAMES[phase],!entry.floor(),(System.nanoTime()-playbackWall)/1_000_000_000.0);
    int done=phase;pendingPhase=phase;clientPhase=-2;onServer(mc.getSingleplayerServer(),()->{if(phase==done){if(phase+1<limit())next(mc.getSingleplayerServer(),phase+1);else phase=limit();}});
  }
  @SubscribeEvent public static void frameEnd(RenderFrameEvent.Post event){
    if(!enabled()||phase<0||phase>=limit()||clientPhase!=phase||!worldThisFrame)return;
    var mc=Minecraft.getInstance();var entry=CASES.get(phase);WORLD_FRAMES[phase]++;viewFrames++;
    if(!armed&&entry.source()&&!PLACED_CHECKED[phase]){
      if(!clientSourceReady(mc))return;checkFloor(mc);PLACED_CHECKED[phase]=true;
    }
    if(!armRequested&&viewFrames>=10){armRequested=true;int index=phase;onServer(mc.getSingleplayerServer(),()->arm(mc.getSingleplayerServer(),index));return;}
    if(!armed)return;
    if(entry.floor()){
      if(viewFrames<4)return;checkFloor(mc);capture(mc,false);
      if(cameraView<2){cameraView++;viewFrames=0;}else finishCase(mc);return;
    }
    if(elapsed<0){if(caseAge-lastApproachCapture>=20&&viewFrames>=3){capture(mc,true);lastApproachCapture=caseAge;}return;}
    if(lowCameraActive){
      if(!LOW_CAMERA_SHOT[phase]){
        String name=String.format("toy051-%02d-toy_car-%s-low-t%03d.png",phase,entry.variant(),elapsed);
        var actualCamera=mc.gameRenderer.getMainCamera();var eye=actualCamera.getPosition();var observed=observed(mc);
        double relativeEyeY=observed==null?Double.NaN:eye.y-observed.getY();
        String cameraDetails="actualEye="+eye+" relativeEyeY="+relativeEyeY+" actualPitch="+actualCamera.getXRot()+" actualYaw="+actualCamera.getYRot()+" configuredFov="+mc.options.fov().get();
        Screenshot.grab(mc.gameDirectory,name,mc.getMainRenderTarget(),message->{SAVED_SHOTS.incrementAndGet();LogUtils.getLogger().info("TOY051_LOW_CAMERA_CAPTURE {} {}",name,cameraDetails);});
        LOW_CAMERA_SHOT[phase]=true;shots++;
      }
      return;
    }
    int segment=!entry.interrupt().isEmpty()?(elapsed<16?0:elapsed<60?1:2):(elapsed<16?0:elapsed<148?1:2);TIMING_FRAMES[phase][segment]++;
    if(elapsed>=nextCapture&&elapsed<entry.finish()&&viewFrames>=3){capture(mc,false);nextCapture=elapsed+4;}
    if(caseComplete)finishCase(mc);
  }

  private static final List<String> ICONS=List.of("chao_picture_book","chao_crayons","chao_broom");
  private static final class IconScreen extends Screen {
    IconScreen(){super(Component.literal("Toy inventory icons"));}
    @Override public void render(GuiGraphics g,int mouseX,int mouseY,float partial){
      super.render(g,mouseX,mouseY,partial);
      g.fill(0,0,width,height,0xff182432);
      g.drawCenteredString(font,"ACTUAL INVENTORY GUI ITEM MODELS",width/2,15,0xffd9edf9);
      for(int i=0;i<ICONS.size();i++){
        var item=new ItemStack(ChaoGarden.item(ICONS.get(i)));int centre=width*(2*i+1)/6;
        g.drawCenteredString(font,item.getHoverName(),centre,49,0xffffffff);
        g.pose().pushPose();g.pose().translate(centre-48,70,0);g.pose().scale(6,6,1);
        g.renderItem(item,0,0);g.pose().popPose();
      }
      g.drawCenteredString(font,"GUI-only presentation; normal placed/held objects retained",width/2,height-22,0xffc2d8e6);
    }
  }
  @SubscribeEvent public static void iconRendered(ScreenEvent.Render.Post event){
    if(enabled()&&phase==limit()&&iconScreen!=null&&event.getScreen()==iconScreen)iconFrames++;
  }
  private static void iconCapture(Minecraft mc,String label){
    require(mc.screen==iconScreen&&iconFrames>=10,"inventory item screen was not actually rendered");
    String name="toy051-inventory-"+label+".png";
    Screenshot.grab(mc.gameDirectory,name,mc.getMainRenderTarget(),message->{
      require(Files.isRegularFile(mc.gameDirectory.toPath().resolve("screenshots").resolve(name)),"inventory capture did not save");
      SAVED_SHOTS.incrementAndGet();LogUtils.getLogger().info("TOY051_ICON_CAPTURE {} actualGui=true",name);
    });shots++;iconShots++;
  }
  private static void inventoryStudy(Minecraft mc){
    if(iconStep<0){
      require(SAVED_SHOTS.get()==shots,"activity captures did not finish saving");
      mc.options.hideGui=false;mc.options.guiScale().set(3);mc.setCameraEntity(mc.player);
      var contents=new SimpleContainer(27);
      for(int i=0;i<ICONS.size();i++){
        var item=new ItemStack(ChaoGarden.item(ICONS.get(i)));contents.setItem(10+i*3,item);
        var model=mc.getItemRenderer().getModel(item,mc.level,mc.player,51);
        var gui=model.applyTransform(ItemDisplayContext.GUI,new PoseStack(),false);
        var hand=model.applyTransform(ItemDisplayContext.NONE,new PoseStack(),false);
        require(!gui.isGui3d(),"requested upward-facing icon did not select its flat GUI model: "+ICONS.get(i));
        require(!gui.getQuads(null,null,RandomSource.create(51)).isEmpty(),"GUI icon has no actual baked faces: "+ICONS.get(i));
        LogUtils.getLogger().info("TOY051_ICON_MODEL {} gui={} gui3d={} none={} guiRotation={}",ICONS.get(i),gui.getClass().getName(),gui.isGui3d(),hand.getClass().getName(),gui.getTransforms().getTransform(ItemDisplayContext.GUI).rotation);
      }
      iconScreen=new ContainerScreen(new ChestMenu(MenuType.GENERIC_9x3,51,mc.player.getInventory(),contents,3),mc.player.getInventory(),Component.literal("Picture book / crayons / broom"));
      iconFrames=0;iconStep=0;mc.setScreen(iconScreen);return;
    }
    if(iconFrames<12)return;
    if(iconStep==0){
      iconCapture(mc,"normal-slots");iconScreen=new IconScreen();iconFrames=0;iconStep=1;mc.setScreen(iconScreen);return;
    }
    if(iconStep==1){iconCapture(mc,"enlarged-gui");iconStep=2;return;}
    if(SAVED_SHOTS.get()!=shots)return;
    require(iconShots==2,"missing actual inventory icon captures");
    for(int i=start();i<limit();i++)require(WORLD_FRAMES[i]>10,"case had no actual world rendering");
    finished=true;mc.setScreen(null);
    onServer(mc.getSingleplayerServer(),()->mc.getSingleplayerServer().tickRateManager().setFrozen(false));
    LogUtils.getLogger().info("TOY051_VISUAL_COMPLETE cases={} start={} limit={} screenshots={} saved={} inventoryScreens={} hurtFrames={} audioListening=false",limit()-start(),start(),limit(),shots,SAVED_SHOTS.get(),iconShots,hurtFrames);mc.stop();
  }
  @SubscribeEvent public static void client(ClientTickEvent.Post event){
    if(!enabled()||finished)return;var mc=Minecraft.getInstance();ticks++;
    if(fatal!=null)throw new IllegalStateException("TOY051 server helper failed: "+fatal);
    if(wallStarted==0)wallStarted=System.nanoTime();require(ticks<60000&&System.nanoTime()-wallStarted<2_400_000_000_000L,"client study timed out");
    if(mc.screen instanceof AccessibilityOnboardingScreen)mc.setScreen(new TitleScreen());
    if(!started&&mc.screen instanceof TitleScreen){
      started=true;mc.options.renderDistance().set(3);mc.options.simulationDistance().set(5);mc.options.framerateLimit().set(30);mc.options.fov().set(45);mc.options.pauseOnLostFocus=false;mc.options.hideGui=true;mc.options.tutorialStep=net.minecraft.client.tutorial.TutorialSteps.NONE;
      mc.createWorldOpenFlows().createFreshLevel("ToyPolish051-"+System.currentTimeMillis(),new LevelSettings("ToyPolish051",GameType.CREATIVE,false,Difficulty.PEACEFUL,true,new GameRules(),WorldDataConfiguration.DEFAULT),new WorldOptions(2551L,false,false),r->r.registryOrThrow(Registries.WORLD_PRESET).getHolderOrThrow(WorldPresets.FLAT).value().createWorldDimensions(),mc.screen);
    }
    if(!setup&&mc.player!=null&&mc.getSingleplayerServer()!=null){
      setup=true;mc.setScreen(null);ChaoRenderer.suppressNames=true;
      onServer(mc.getSingleplayerServer(),()->{var server=mc.getSingleplayerServer();var level=server.overworld();level.setDayTime(6000);level.getGameRules().getRule(GameRules.RULE_DAYLIGHT).set(false,server);level.getGameRules().getRule(GameRules.RULE_DOMOBSPAWNING).set(false,server);
        for(var p:BlockPos.betweenClosed(-9,79,-9,9,86,11))level.setBlock(p,p.getY()==79?Blocks.SMOOTH_STONE.defaultBlockState():Blocks.AIR.defaultBlockState(),3);
        server.getPlayerList().getPlayers().forEach(p->p.setGameMode(GameType.SPECTATOR));next(server,start());});
    }
    if(phase>=0&&phase<limit()&&mc.level!=null&&mc.player!=null){
      if(clientPhase==-2&&phase==pendingPhase)return;
      if(clientPhase!=phase){clientPhase=phase;cameraView=0;viewFrames=0;armRequested=false;nextCapture=0;lastApproachCapture=-20;}
      if(!armed&&!clientSourceReady(mc))require(System.nanoTime()-playbackWall<30_000_000_000L,
          "matching client source packet did not arrive: case="+phase+" expected="+CASES.get(phase).kind()+" source="+CASES.get(phase).source()+" actual="+mc.level.getBlockState(TOY));
      camera(mc);
    }
    if(phase==limit()&&SAVED_SHOTS.get()==shots){
      for(int i=start();i<limit();i++)require(WORLD_FRAMES[i]>10,"case had no actual world rendering");
      inventoryStudy(mc);
    }
  }
}
