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
import net.minecraft.client.*;
import net.minecraft.client.gui.screens.AccessibilityOnboardingScreen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.decoration.ArmorStand;
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

/** Disposable real-client quality passes. Excluded from the released JAR by *VisualSmoke*. */
@EventBusSubscriber(modid=ChaoGarden.MODID,value=Dist.CLIENT)
public final class GardenToyVisualSmoke049 {
  private record Case(String id,String variant,Direction facing,String mode,String interrupt) {
    String kind(){return "chao_"+id;}
    boolean floor(){return mode.equals("floor");}
    boolean preview(){return !floor()&&!mode.equals("natural");}
    boolean loop(){return mode.equals("loop");}
    boolean mounted(){return id.equals("rocking_horse")||id.equals("pogo_stick");}
    boolean source(){return floor()||!preview()||mounted();}
    int finish(){return !interrupt.isEmpty()?102:loop()?206:180;}
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
    int cues(){return switch(id){case "toy_car"->4;case "sonic_doll","shadow_doll"->3;case "picture_book"->2;default->8;};}
    int interruptedCues(){return switch(id){case "toy_car","sonic_doll","shadow_doll","picture_book"->1;default->3;};}
  }
  private static List<Case> cases(){
    var result=new ArrayList<Case>();
    for(String toy:List.of("accordion","guitar","triangle","bubble_wand","castanets"))
      for(Direction direction:new Direction[]{Direction.NORTH,Direction.EAST,Direction.SOUTH,Direction.WEST})
        result.add(new Case(toy,"",direction,"floor",""));
    int index=0;
    for(String toy:List.of("rocking_horse","toy_car","sonic_doll","shadow_doll","broom","picture_book","pogo_stick","crayons")){
      result.add(new Case(toy,"baby_neutral",Direction.NORTH,"natural",""));
      result.add(new Case(toy,"adult_neutral_type0",Direction.EAST,"natural",""));
      result.add(new Case(toy,"animal_rabbit_baby",Direction.SOUTH,"once",""));
      result.add(new Case(toy,"chaos_2",Direction.WEST,"loop",""));
      result.add(new Case(toy,"adult_neutral_type0",Direction.NORTH,"natural",index++%2==0?"remove":"damage"));
    }
    return List.copyOf(result);
  }
  private static final List<Case> CASES=cases();
  private static int limit(){return Math.clamp(Integer.getInteger("chao_garden.gardenToy049Limit",CASES.size()),1,CASES.size());}
  private static int start(){return Math.clamp(Integer.getInteger("chao_garden.gardenToy049Start",0),0,limit()-1);}
  private static boolean enabled(){return Boolean.getBoolean("chao_garden.gardenToy049Study");}
  private static final BlockPos TOY=new BlockPos(0,80,2);
  private static final LionfishActiveToyClip049 ACTIVE_CLIP=new LionfishActiveToyClip049();
  private static final LionfishQuietToyClip049 QUIET_CLIP=new LionfishQuietToyClip049();
  private static final int[] WORLD_FRAMES=new int[CASES.size()],ACTIVE_FRAMES=new int[CASES.size()],SOUNDS=new int[CASES.size()];
  private static final int[][] VIEW_SHOTS=new int[CASES.size()][3],TIMING_FRAMES=new int[CASES.size()][3];
  private static final float[] CONTACT_ERROR=new float[CASES.size()];
  private static final long[] AUDITED=new long[CASES.size()];
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
  private static long spawned,began=-1,lastTick,playbackWall;
  private static Vec3 previousPosition;
  private static double walked;
  private static boolean interrupted,previewProof;
  private static String savedState;
  private static long savedCooldown;
  private static float interruptedHappiness;

  private static void require(boolean result,String message){if(!result)throw new IllegalStateException("GARDEN049 "+message);}
  private static void onServer(MinecraftServer server,Runnable task){server.execute(()->{
    try{task.run();}catch(Throwable failure){fatal=failure.toString();LogUtils.getLogger().error("GARDEN049_ASYNC_FAILURE",failure);}
  });}
  private static void command(MinecraftServer server,String mode){
    var player=server.getPlayerList().getPlayers().getFirst();
    try{require(server.getCommands().getDispatcher().execute("chao testtoy "+CASES.get(phase).id()+" "+mode,
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
      actor=ChaoGarden.CHAO.get().create(level);require(actor!=null,"study Chao did not create");actor.setState(state);
      actor.setInvulnerable(true);actor.setNoAi(entry.loop());actor.getRandom().setSeed(18);
      // No block-contact helper is used for a free-standing hand-toy preview.
      Vec3 origin=Vec3.atBottomCenterOf(entry.preview()?(entry.mounted()?TOY.relative(entry.facing()):TOY):TOY.relative(entry.facing(),3));
      float yaw=entry.facing().getOpposite().toYRot();actor.moveTo(origin.x,origin.y,origin.z,yaw,0);
      actor.yBodyRot=actor.yBodyRotO=actor.yHeadRot=actor.yHeadRotO=yaw;
      level.addFreshEntity(actor);actor.setOnGround(true);actor.goalSelector.removeAllGoals(g->true);actor.targetSelector.removeAllGoals(g->true);
      if(entry.preview())actor.getPersistentData().putLong("ChaoToyNextPlay",Long.MAX_VALUE);
      else actor.goalSelector.addGoal(4,new ToyContent.PlayGoal(actor));
      expectedAdult=state.adult;savedState=state.encode();savedCooldown=actor.getPersistentData().getLong("ChaoToyNextPlay");
      previousPosition=actor.position();actorId=actor.getUUID();interruptedHappiness=state.happiness;
    }
    var player=server.getPlayerList().getPlayers().getFirst();player.teleportTo(4.5,82,6.5);player.setNoGravity(true);player.setDeltaMovement(Vec3.ZERO);
    spawned=lastTick=serverTime=level.getGameTime();playbackWall=System.nanoTime();began=-1;elapsed=-1;caseAge=0;walked=0;
    interrupted=previewProof=armed=caseComplete=false;phase=index;
    LogUtils.getLogger().info("GARDEN049_CASE {} toy={} variant={} facing={} mode={} interrupt={} expectedAdult={}",index,entry.id(),entry.variant(),entry.facing(),entry.mode(),entry.interrupt(),expectedAdult);
  }
  private static void arm(MinecraftServer server,int expected){
    if(phase!=expected||armed)return;var entry=CASES.get(phase);
    if(entry.floor()){armed=true;return;}
    require(actor.state().adult==expectedAdult&&!actor.isCocooned(),"fixture changed age before interaction");actor.setOnGround(true);
    if(entry.preview()){command(server,entry.loop()?"loop":"once");began=server.overworld().getGameTime();elapsed=0;}
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
    }else{
      require(walked>.35,"natural interaction did not actually approach from the starting point");
      require(actor.state().happiness==(entry.interrupt().isEmpty()?24:interruptedHappiness),"natural reward was not applied exactly once or interruption granted a reward");
    }
    var state=server.overworld().getBlockState(TOY);
    if(entry.interrupt().equals("remove")||!entry.source())require(state.isAir(),"source-free/removed toy reappeared");
    else require(state.is(ToyContent.block(entry.kind()))&&!state.getValue(ChaoToyBlock.PLAYING)&&state.getValue(ChaoToyBlock.FRAME)==0,"source did not return idle");
    caseComplete=true;server.tickRateManager().setFrozen(true);
    LogUtils.getLogger().info("GARDEN049_NATIVE_COMPLETE {} toy={} elapsed={} walked={} previewStatePreserved={}",phase,entry.id(),elapsed,walked,previewProof);
  }
  @SubscribeEvent public static void server(ServerTickEvent.Post event){
    if(!enabled()||phase<0||phase>=limit()||!armed||caseComplete||CASES.get(phase).floor())return;
    var server=event.getServer();var level=server.overworld();long now=level.getGameTime();if(now==lastTick)return;
    lastTick=serverTime=now;caseAge=now-spawned;require(caseAge<1400,"natural approach or interaction timed out");
    require(actor.state().adult==expectedAdult&&!actor.isCocooned(),"fixture changed age or cocooned during interaction");
    if(began<0){
      if(ToyContent.isPlaying(actor)){
        began=actor.getPersistentData().getLong("ChaoToyUntil")-160;playbackWall=System.nanoTime();
        LogUtils.getLogger().info("GARDEN049_NATURAL_STARTED {} walked={} waitTicks={}",phase,walked,now-spawned);
      }else{walked+=actor.position().subtract(previousPosition).horizontalDistance();previousPosition=actor.position();}
    }
    if(began<0)return;elapsed=now-began;var entry=CASES.get(phase);
    if(!interrupted&&!entry.interrupt().isEmpty()&&elapsed>=60){
      interrupted=true;
      if(entry.interrupt().equals("damage")){actor.setInvulnerable(false);require(actor.hurt(level.damageSources().generic(),1),"real damage was rejected");}
      else require(level.destroyBlock(TOY,false),"placed source removal was rejected");
      interruptedHappiness=actor.state().happiness;
      LogUtils.getLogger().info("GARDEN049_INTERRUPT {} {} elapsed={}",phase,entry.interrupt(),elapsed);
    }
    if(entry.loop()&&!interrupted&&elapsed>=180){require(ToyPreview.active(actor),"loop did not cross its phrase boundary");command(server,"stop");interrupted=true;}
    if(entry.preview()&&!previewProof){
      require(savedState.equals(actor.state().encode()),"preview changed saved care, age, genes or lessons before its terminal tick");
      require(actor.getPersistentData().getLong("ChaoToyNextPlay")==savedCooldown,"preview changed cooldown");
      if(!ToyPreview.active(actor))previewProof=true;
    }
    if(elapsed>=entry.finish())complete(server);
  }
  @SubscribeEvent public static void restoreCamera(ClientTickEvent.Pre event){if(enabled()){
    var mc=Minecraft.getInstance();if(mc.player!=null&&mc.getCameraEntity()!=mc.player)mc.setCameraEntity(mc.player);
  }}
  private static ChaoEntity observed(Minecraft mc){
    if(mc.level==null||actorId==null)return null;
    for(var e:mc.level.entitiesForRendering())if(e instanceof ChaoEntity c&&c.getUUID().equals(actorId))return c;
    return null;
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
      boolean ride=entry.mounted();double distance=ride?2.1:1.7;
      eye=focus.add(forward.scale(view==0?distance:view==1?.02:-.35)).add(side.scale(view==0?.9:view==1?distance:-distance)).add(0,view==1?.35:view==2?.7:.65,0);
    }
    var delta=focus.subtract(eye);float yaw=(float)(Math.toDegrees(Math.atan2(delta.z,delta.x))-90),pitch=(float)-Math.toDegrees(Math.atan2(delta.y,Math.hypot(delta.x,delta.z)));
    observer.moveTo(eye.x,eye.y-observer.getEyeHeight(),eye.z,yaw,pitch);observer.xo=observer.xOld=observer.getX();observer.yo=observer.yOld=observer.getY();observer.zo=observer.zOld=observer.getZ();
    observer.yRotO=yaw;observer.xRotO=pitch;observer.setYHeadRot(yaw);observer.yHeadRotO=yaw;
    mc.options.setCameraType(CameraType.FIRST_PERSON);mc.setCameraEntity(observer);
    if(cameraView!=view){cameraView=view;viewFrames=0;}
  }
  private static boolean active(ChaoEntity c){return c.toyPlayingKind().equals(RockingHorsePlayRules.KIND)?ChaoRockingHorseAnimation.active(c):
      ActiveToyPlayRules049.supports(c.toyPlayingKind())?ChaoActiveToyAnimation049.active(c):ChaoQuietToyAnimation049.active(c);}
  private static void contact(ChaoEntity c,ChaoModel m,float partial){
    float t=GardenToyPerformance049.phase(c,partial);if(t<16||t>=144)return;
    float w=GardenToyPerformance049.presence(c,partial),scale=c.state().immortal?.86f:c.state().adult?.80f:.64f;
    String kind=c.toyPlayingKind();
    for(boolean left:new boolean[]{false,true}){
      var palm=m.toyPalm(left);require(palm!=null,"live active toy has no visible hand");
      var actual=ChaoTriangleAnimation.palm(m::transformAccessoryAnchor,palm,left);Vector3f target;
      // The broom deliberately uses one sweeping hand; the other balances freely.
      if(kind.equals(QuietToyPlayRules049.BROOM)&&left){require(actual.isFinite(),"free balancing hand is not finite");continue;}
      if(ActiveToyPlayRules049.supports(kind))target=ChaoActiveToyAnimation049.gripTarget(ChaoActiveToyAnimation049.propMatrix(m::transformAccessoryAnchor,kind,t,w,ACTIVE_CLIP),kind,left);
      else if(kind.equals(QuietToyPlayRules049.BROOM))target=ChaoQuietToyAnimation049.broomTarget(ChaoQuietToyAnimation049.broomMatrix(w,QUIET_CLIP.sweep(t),scale),left);
      else if(kind.equals(QuietToyPlayRules049.BOOK))target=ChaoQuietToyAnimation049.bookTarget(ChaoQuietToyAnimation049.bookMatrix(m::transformAccessoryAnchor,w),left);
      else if(kind.equals(QuietToyPlayRules049.CRAYONS)){
        var paper=ChaoQuietToyAnimation049.paperMatrix(m::transformAccessoryAnchor,w);
        target=left?ChaoQuietToyAnimation049.paperSupport(paper):ChaoQuietToyAnimation049.drawingHand(paper,t,QUIET_CLIP.stroke(t),QUIET_CLIP.pen(t));
      }else target=horseTarget(c,t,partial,scale,left);
      float error=actual.distance(target);CONTACT_ERROR[phase]=Math.max(CONTACT_ERROR[phase],error);
      require(actual.isFinite()&&error<.065f,"live hand lost toy contact: "+kind+" left="+left+" error="+error);
      var arm=m.officialPoses().get(left?"arm_left":"arm_right");require(arm.reach<=3.401f,"live toy forearm is too long");
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
  @SubscribeEvent public static void frameBegin(RenderFrameEvent.Pre event){if(enabled()){renderedThisFrame=worldThisFrame=false;}}
  @SubscribeEvent public static void rendered(RenderLivingEvent.Post<?,?> event){
    if(!enabled()||phase<0||phase>=limit()||CASES.get(phase).floor()||SceneShaderPack.shadows()||!(event.getEntity() instanceof ChaoEntity c)||!c.getUUID().equals(actorId))return;
    var mc=Minecraft.getInstance();if(mc.screen!=null||mc.getOverlay()!=null||mc.getCameraEntity()!=observer)return;
    require(c.state().adult==expectedAdult&&!c.isCocooned(),"rendered fixture changed age");var model=(ChaoModel)event.getRenderer().getModel();require(model.officialPosesFinite(),"rendered toy pose is not finite");
    renderedThisFrame=true;
    if(active(c)){
      ACTIVE_FRAMES[phase]++;
      if(AUDITED[phase]!=mc.level.getGameTime()){AUDITED[phase]=mc.level.getGameTime();contact(c,model,Math.clamp(event.getPartialTick(),0,1));}
    }
    if(CASES.get(phase).interrupt().equals("damage")&&c.hurtTime>0){require(model.officialEmotion()==1&&!active(c),"hurt did not replace toy playing");hurtFrames++;}
  }
  @SubscribeEvent public static void renderedWorld(RenderLevelStageEvent event){
    if(!enabled()||phase<0||phase>=limit()||event.getStage()!=RenderLevelStageEvent.Stage.AFTER_LEVEL||SceneShaderPack.shadows())return;
    var mc=Minecraft.getInstance();if(mc.screen==null&&mc.getOverlay()==null&&mc.level!=null&&mc.getCameraEntity()==observer&&(CASES.get(phase).floor()||renderedThisFrame))worldThisFrame=true;
  }
  @SubscribeEvent public static void heard(net.neoforged.neoforge.client.event.sound.PlaySoundEvent event){
    if(!enabled()||phase<0||phase>=limit()||!armed||elapsed<0)return;
    var sound=event.getOriginalSound();if(sound!=null&&sound.getLocation().toString().equals(CASES.get(phase).sound()))SOUNDS[phase]++;
  }
  private static void checkFloor(Minecraft mc){
    var entry=CASES.get(phase);var state=mc.level.getBlockState(TOY);
    require(state.is(ToyContent.block(entry.kind()))&&state.getRenderShape()==RenderShape.MODEL&&!state.getValue(ChaoToyBlock.PLAYING),"floor source is missing or hidden");
    var shape=state.getShape(mc.level,TOY).bounds();require(Math.abs(shape.minY)<1e-7,"floor collision hovers");
    var quads=mc.getBlockRenderer().getBlockModel(state).getQuads(state,null,RandomSource.create(49));require(!quads.isEmpty(),"floor model has no actual baked faces");
    float min=Float.POSITIVE_INFINITY,max=Float.NEGATIVE_INFINITY;
    for(var quad:quads){int[] data=quad.getVertices();int stride=data.length/4;for(int i=0;i<4;i++){float y=Float.intBitsToFloat(data[i*stride+1]);min=Math.min(min,y);max=Math.max(max,y);}}
    require(Math.abs(min)<.00001f&&max<=shape.maxY+.00001,"actual baked floor geometry floats or leaves collision bounds: "+min+".."+max);
    LogUtils.getLogger().info("GARDEN049_FLOOR_BOUND {} toy={} facing={} actualY={}..{} shapeY={}..{} quads={}",phase,entry.id(),entry.facing(),min,max,shape.minY,shape.maxY,quads.size());
  }
  private static void capture(Minecraft mc,boolean approach){
    var entry=CASES.get(phase);String name=String.format("garden049-%02d-%s-%s-%s-%s-v%d.png",phase,entry.id(),entry.floor()?"floor":entry.variant(),entry.facing().getSerializedName(),entry.floor()?"static":approach?String.format("approach%03d",caseAge):String.format("t%03d",elapsed),cameraView);
    int index=phase,view=cameraView;long t=elapsed;
    Screenshot.grab(mc.gameDirectory,name,mc.getMainRenderTarget(),message->{require(Files.isRegularFile(mc.gameDirectory.toPath().resolve("screenshots").resolve(name)),"capture did not save");SAVED_SHOTS.incrementAndGet();LogUtils.getLogger().info("GARDEN049_CAPTURE {} case={} tick={} view={} approach={}",name,index,t,view,approach);});
    shots++;if(!approach)VIEW_SHOTS[phase][cameraView]++;viewFrames=0;
  }
  private static void finishCase(Minecraft mc){
    var entry=CASES.get(phase);if(!entry.floor()){
      require(ACTIVE_FRAMES[phase]>=20,"production interaction was not actually rendered");
      for(int i=0;i<3;i++){require(VIEW_SHOTS[phase][i]>=3,"interaction missed a useful camera angle");require(TIMING_FRAMES[phase][i]>=3,"entry/play/exit were not all rendered");}
      int expected=entry.interrupt().isEmpty()?entry.cues():entry.interruptedCues();
      require(SOUNDS[phase]==expected,"actual client cues differ from phrase or continued after interruption: "+SOUNDS[phase]+" expected "+expected);
    }
    LogUtils.getLogger().info("GARDEN049_RENDER_COMPLETE {} toy={} worldFrames={} activeFrames={} captures={} sounds={} maxPalmError={} normalSpeed={} wallSeconds={}",phase,entry.id(),WORLD_FRAMES[phase],ACTIVE_FRAMES[phase],Arrays.stream(VIEW_SHOTS[phase]).sum(),SOUNDS[phase],CONTACT_ERROR[phase],!entry.floor(),(System.nanoTime()-playbackWall)/1_000_000_000.0);
    int done=phase;pendingPhase=phase;clientPhase=-2;onServer(mc.getSingleplayerServer(),()->{if(phase==done){if(phase+1<limit())next(mc.getSingleplayerServer(),phase+1);else phase=limit();}});
  }
  @SubscribeEvent public static void frameEnd(RenderFrameEvent.Post event){
    if(!enabled()||phase<0||phase>=limit()||clientPhase!=phase||!worldThisFrame)return;
    var mc=Minecraft.getInstance();var entry=CASES.get(phase);WORLD_FRAMES[phase]++;viewFrames++;
    if(!armRequested&&viewFrames>=10){armRequested=true;int index=phase;onServer(mc.getSingleplayerServer(),()->arm(mc.getSingleplayerServer(),index));return;}
    if(!armed)return;
    if(entry.floor()){
      if(viewFrames<4)return;checkFloor(mc);capture(mc,false);
      if(cameraView<2){cameraView++;viewFrames=0;}else finishCase(mc);return;
    }
    if(elapsed<0){if(caseAge-lastApproachCapture>=20&&viewFrames>=3){capture(mc,true);lastApproachCapture=caseAge;}return;}
    int segment=!entry.interrupt().isEmpty()?(elapsed<16?0:elapsed<60?1:2):(elapsed<16?0:elapsed<148?1:2);TIMING_FRAMES[phase][segment]++;
    if(elapsed>=nextCapture&&elapsed<entry.finish()&&viewFrames>=3){capture(mc,false);nextCapture=elapsed+4;}
    if(caseComplete)finishCase(mc);
  }
  @SubscribeEvent public static void client(ClientTickEvent.Post event){
    if(!enabled()||finished)return;var mc=Minecraft.getInstance();ticks++;
    if(fatal!=null)throw new IllegalStateException("GARDEN049 server helper failed: "+fatal);
    if(wallStarted==0)wallStarted=System.nanoTime();require(ticks<60000&&System.nanoTime()-wallStarted<2_400_000_000_000L,"client study timed out");
    if(mc.screen instanceof AccessibilityOnboardingScreen)mc.setScreen(new TitleScreen());
    if(!started&&mc.screen instanceof TitleScreen){
      started=true;mc.options.renderDistance().set(3);mc.options.simulationDistance().set(5);mc.options.framerateLimit().set(30);mc.options.fov().set(45);mc.options.pauseOnLostFocus=false;mc.options.hideGui=true;mc.options.tutorialStep=net.minecraft.client.tutorial.TutorialSteps.NONE;
      mc.createWorldOpenFlows().createFreshLevel("Garden049-"+System.currentTimeMillis(),new LevelSettings("Garden049",GameType.CREATIVE,false,Difficulty.PEACEFUL,true,new GameRules(),WorldDataConfiguration.DEFAULT),new WorldOptions(2549L,false,false),r->r.registryOrThrow(Registries.WORLD_PRESET).getHolderOrThrow(WorldPresets.FLAT).value().createWorldDimensions(),mc.screen);
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
      camera(mc);
    }
    if(phase==limit()&&SAVED_SHOTS.get()==shots){
      for(int i=start();i<limit();i++)require(WORLD_FRAMES[i]>10,"case had no actual world rendering");
      finished=true;onServer(mc.getSingleplayerServer(),()->mc.getSingleplayerServer().tickRateManager().setFrozen(false));
      LogUtils.getLogger().info("GARDEN049_VISUAL_COMPLETE cases={} start={} limit={} screenshots={} saved={} hurtFrames={} audioListening=false",limit()-start(),start(),limit(),shots,SAVED_SHOTS.get(),hurtFrames);mc.stop();
    }
  }
}
