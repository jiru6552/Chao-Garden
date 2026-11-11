package com.chaogarden.client;

import com.chaogarden.ChaoGarden;
import com.chaogarden.entity.ChaoEntity;
import com.chaogarden.test.ToyVariantCases02522;
import com.chaogarden.toys.ChaoToyBlock;
import com.chaogarden.toys.InstrumentPerformance;
import com.chaogarden.toys.PianoPlayRules;
import com.chaogarden.toys.ToyContent;
import com.chaogarden.toys.ToyPreview;
import com.mojang.logging.LogUtils;
import java.nio.file.Files;
import java.util.UUID;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.AccessibilityOnboardingScreen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.WorldDataConfiguration;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderFrameEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.client.event.RenderLivingEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/**
 * Opt-in actual-client evidence, excluded from release JARs by *VisualSmoke*.
 * Native tick freeze/step waits for visible world frames before advancing the
 * real production clock. It is a pose/transition study, not an audio-speed test.
 */
@EventBusSubscriber(modid=ChaoGarden.MODID,value=Dist.CLIENT)
public final class InstrumentVisualSmoke048 {
  private record Case(String variant,String id,Direction facing,boolean placed,String mode,String interrupt) {
    String kind(){return "chao_"+id;}
    boolean preview(){return !placed;}
    boolean realtime(){return mode.equals("realtime");}
    int finish(){return interrupt.isEmpty()?(mode.equals("loop")?350:180):102;}
    int[] captures(){
      if(!interrupt.isEmpty())return new int[]{0,12,24,56,62,76,94};
      if(id.equals("piano"))return new int[]{0,12,23,24,31,32,56,88,120,144,152,164,176};
      if(mode.equals("loop"))return new int[]{0,12,24,56,88,120,144,152,164,184,216,248,280,312,326,334,346};
      return new int[]{0,12,24,56,88,120,144,152,164,176};
    }
  }
  private static final Case[] CASES={
      new Case("baby_neutral","piano",Direction.NORTH,false,"once",""),
      new Case("adult_neutral_type0","piano",Direction.EAST,true,"once",""),
      new Case("animal_rabbit_baby","piano",Direction.SOUTH,false,"once",""),
      new Case("chaos_2","piano",Direction.WEST,true,"once",""),
      new Case("baby_neutral","accordion",Direction.NORTH,true,"once",""),
      new Case("adult_neutral_type0","accordion",Direction.EAST,false,"once",""),
      new Case("animal_rabbit_baby","accordion",Direction.SOUTH,true,"once",""),
      new Case("chaos_2","accordion",Direction.WEST,false,"loop",""),
      new Case("adult_neutral_type0","piano",Direction.NORTH,false,"once","damage"),
      new Case("baby_neutral","accordion",Direction.EAST,true,"once","remove"),
      new Case("adult_neutral_type0","piano",Direction.NORTH,false,"realtime",""),
      new Case("baby_neutral","accordion",Direction.NORTH,false,"realtime","")
  };
  private static final LionfishAccordionClip CONTACT_CLIP=new LionfishAccordionClip();
  private static int caseStart(){return Math.clamp(Integer.getInteger("chao_garden.instrument048Start",0),0,caseCount()-1);}
  private static int caseCount(){return Math.clamp(Integer.getInteger("chao_garden.instrument048Limit",CASES.length),1,CASES.length);}
  private static final BlockPos TOY=new BlockPos(0,80,2);
  private static final int[] WORLD_FRAMES=new int[CASES.length],ACTIVE_FRAMES=new int[CASES.length];
  private static final int[][] VIEW_SHOTS=new int[CASES.length][3];
  private static final int[][] TIMING_FRAMES=new int[CASES.length][3];
  private static final int[] CLIENT_SOUND_EVENTS=new int[CASES.length];
  private static boolean started,setup,finished,renderedThisFrame,levelThisFrame,armRequested;
  private static int ticks,shots,savedShots,clientPhase=-1,captureIndex,captureView,cameraView=-1,viewFrames,stepFrames,hurtFrames;
  private static long wallBegan,nextRealtimeCapture;
  private static volatile long playbackWall;
  private static ArmorStand observer;
  private static volatile int phase=-1;
  private static volatile long elapsed=-1,publishedTime;
  private static volatile UUID actorId;
  private static volatile boolean armed,stepOutstanding,stepCompleted,caseComplete;
  // The following state belongs exclusively to the integrated-server thread.
  private static ChaoEntity actor;
  private static long began=-1,spawned,lastServerTime;
  private static boolean interrupted,expectedAdult,previewPreserved;
  private static String savedState;
  private static long savedCooldown;

  private static boolean enabled(){return Boolean.getBoolean("chao_garden.instrument048Study");}
  private static void require(boolean value,String message){if(!value)throw new IllegalStateException("INSTRUMENT048 "+message);}
  private static void command(MinecraftServer server,String mode){
    var player=server.getPlayerList().getPlayers().getFirst();
    try{require(server.getCommands().getDispatcher().execute("chao testtoy "+CASES[phase].id()+" "+mode,
        player.createCommandSourceStack().withPermission(2))==1,"native dispatcher rejected "+CASES[phase].id()+" "+mode);}
    catch(com.mojang.brigadier.exceptions.CommandSyntaxException error){throw new IllegalStateException(error);}
  }
  private static void next(MinecraftServer server,int index){
    if(actor!=null){ToyPreview.stop(actor);actor.discard();}
    var entry=CASES[index];var level=server.overworld();
    var state=ToyVariantCases02522.all().stream().filter(c->c.name().equals(entry.variant())).findFirst().orElseThrow().state();
    // Use the actual shared variant, including its age. Raising a baby's
    // evolution strength here would silently substitute an evolution cocoon.
    state.setTraitInt("learned."+entry.id(),1);
    actor=ChaoGarden.CHAO.get().create(level);require(actor!=null,"could not create study Chao");
    actor.setState(state);actor.setInvulnerable(true);actor.setNoAi(entry.preview());actor.getRandom().setSeed(18);
    level.setBlock(TOY,entry.preview()&&entry.id().equals("accordion")?Blocks.AIR.defaultBlockState():ToyContent.block(entry.kind()).defaultBlockState().setValue(ChaoToyBlock.FACING,entry.facing()),3);
    Vec3 anchor=entry.id().equals("piano")?ToyContent.keyboardAnchor(actor,TOY):
        entry.preview()?Vec3.atBottomCenterOf(TOY):ToyContent.stationaryAnchor(actor,TOY,TOY.relative(entry.facing()));
    float yaw=entry.facing().getOpposite().toYRot();actor.moveTo(anchor.x,anchor.y,anchor.z,yaw,0);
    actor.yBodyRot=actor.yBodyRotO=actor.yHeadRot=actor.yHeadRotO=yaw;actor.setOnGround(true);
    level.addFreshEntity(actor);actor.goalSelector.removeAllGoals(goal->true);actor.targetSelector.removeAllGoals(goal->true);
    if(entry.placed())actor.goalSelector.addGoal(4,new ToyContent.PlayGoal(actor));
    else actor.getPersistentData().putLong("ChaoToyNextPlay",Long.MAX_VALUE);
    var player=server.getPlayerList().getPlayers().getFirst();
    player.teleportTo(anchor.x+4,anchor.y+2,anchor.z+3);player.setNoGravity(true);player.setDeltaMovement(Vec3.ZERO);
    expectedAdult=state.adult;savedState=state.encode();savedCooldown=actor.getPersistentData().getLong("ChaoToyNextPlay");
    server.tickRateManager().setFrozen(true);
    spawned=publishedTime=lastServerTime=level.getGameTime();began=-1;elapsed=-1;interrupted=previewPreserved=false;
    armed=false;stepOutstanding=false;stepCompleted=false;caseComplete=false;actorId=actor.getUUID();phase=index;
    LogUtils.getLogger().info("INSTRUMENT048_CASE {} toy={} variant={} adult={} facing={} source={} mode={} interrupt={}",
        index,entry.id(),entry.variant(),expectedAdult,entry.facing(),entry.placed()?"actual-placed-goal":"native-preview",entry.mode(),entry.interrupt());
  }
  private static void arm(MinecraftServer server,int expectedPhase){
    if(phase!=expectedPhase||armed)return;
    require(!actor.isCocooned()&&actor.state().adult==expectedAdult,"fixture changed age before playback");
    actor.setOnGround(true);
    if(CASES[phase].preview()){
      command(server,CASES[phase].realtime()?"once":CASES[phase].mode());began=server.overworld().getGameTime();elapsed=0;
    }
    playbackWall=System.nanoTime();armed=true;
    if(CASES[phase].realtime())server.tickRateManager().setFrozen(false);
  }
  private static void checkComplete(MinecraftServer server){
    var entry=CASES[phase];
    require(!ToyPreview.active(actor)&&actor.toyTarget()==null&&actor.toyPlayingTicks()==0,"native session did not clean up");
    require(actor.getHeldChaoItem().isEmpty()&&actor.getMainHandItem().isEmpty(),"session left a real held item");
    require(actor.state().adult==expectedAdult&&!actor.isCocooned(),"fixture changed age or entered a cocoon");
    require(actor.isNoAi()==entry.preview(),"session did not restore its original NoAI flag");
    if(entry.preview()&&entry.interrupt().isEmpty()){
      require(previewPreserved,"preview did not preserve saved care, lessons, genetics or age before ordinary care resumed");
      require(actor.getPersistentData().getLong("ChaoToyNextPlay")==savedCooldown,"preview changed placed-toy cooldown");
    }
    var source=server.overworld().getBlockState(TOY);
    if(entry.interrupt().equals("remove"))require(!source.is(ToyContent.block(entry.kind())),"removed source returned");
    else if(entry.preview()&&entry.id().equals("accordion"))require(source.isAir(),"handheld preview created a placed source");
    else require(source.is(ToyContent.block(entry.kind()))&&!source.getValue(ChaoToyBlock.PLAYING)
        &&source.getValue(ChaoToyBlock.FRAME)==0,"source did not restore its idle presentation");
    caseComplete=true;
    if(entry.realtime())server.tickRateManager().setFrozen(true);
    LogUtils.getLogger().info("INSTRUMENT048_NATIVE_COMPLETE {} elapsed={} sourceRestored={} previewStatePreserved={}",
        phase,elapsed,!entry.interrupt().equals("remove"),entry.preview()&&entry.interrupt().isEmpty());
  }
  @SubscribeEvent public static void server(ServerTickEvent.Post event){
    if(!enabled()||actor==null||phase<0||phase>=caseCount()||caseComplete)return;
    var server=event.getServer();var level=server.overworld();long now=level.getGameTime();
    if(now==lastServerTime)return;lastServerTime=publishedTime=now;
    require(now-spawned<1800,"native placed interaction did not start");
    require(actor.state().adult==expectedAdult&&!actor.isCocooned(),"fixture changed age or cocooned during study");
    if(armed&&began<0&&ToyContent.isPlaying(actor))began=actor.getPersistentData().getLong("ChaoToyUntil")-160;
    if(began>=0){
      elapsed=now-began;var entry=CASES[phase];
      if(!interrupted&&elapsed>=60&&!entry.interrupt().isEmpty()){
        interrupted=true;
        if(entry.interrupt().equals("damage")){
          actor.setInvulnerable(false);require(actor.hurt(level.damageSources().generic(),1),"native damage was rejected");
        }else require(level.destroyBlock(TOY,false),"native source removal was rejected");
        LogUtils.getLogger().info("INSTRUMENT048_INTERRUPT {} {} elapsed={}",phase,entry.interrupt(),elapsed);
      }
      if(entry.mode().equals("loop")&&!interrupted&&elapsed>=330){
        require(ToyPreview.active(actor),"loop did not survive its phrase boundary");command(server,"stop");interrupted=true;
      }
      if(entry.preview()&&entry.interrupt().isEmpty()&&!previewPreserved){
        require(savedState.equals(actor.state().encode()),"native preview changed saved care, lessons, genetics or age");
        require(actor.getPersistentData().getLong("ChaoToyNextPlay")==savedCooldown,"native preview changed placed-toy cooldown");
        // Check on its actual terminal tick, before ordinary age/care ticks
        // legitimately resume during the subsequently captured idle transition.
        if(!ToyPreview.active(actor))previewPreserved=true;
      }
      if(elapsed>=entry.finish())checkComplete(server);
    }
    stepCompleted=true;
  }
  @SubscribeEvent public static void restoreInputCamera(ClientTickEvent.Pre event){
    if(!enabled())return;var mc=Minecraft.getInstance();
    // LocalPlayer must own the native simulation tick so observation cannot
    // suppress ordinary movement packets or change the keeper's physics.
    if(mc.player!=null&&mc.getCameraEntity()!=mc.player)mc.setCameraEntity(mc.player);
  }
  private static ChaoEntity clientActor(Minecraft mc){
    if(mc.level==null||actorId==null)return null;
    for(var entity:mc.level.entitiesForRendering())if(entity instanceof ChaoEntity c&&c.getUUID().equals(actorId))return c;
    return null;
  }
  private static void camera(Minecraft mc,ChaoEntity c){
    if(observer==null||observer.level()!=mc.level){observer=new ArmorStand(mc.level,0,0,0);observer.setInvisible(true);observer.setNoGravity(true);}
    int wanted=CASES[phase].realtime()?(elapsed<60?0:elapsed<120?1:2):captureView;
    if(CASES[phase].realtime())captureView=wanted;
    var entry=CASES[phase];var forward=Vec3.directionFromRotation(0,entry.facing().getOpposite().toYRot());
    var side=new Vec3(-forward.z,0,forward.x);
    // Three-quarter views on either shoulder and a side view expose both
    // keyboard contact and the actual accordion's front and moving bellows.
    var focus=c.position().add(forward.scale(entry.id().equals("piano")?.20:.13)).add(0,expectedAdult?.40:.34,0);
    boolean piano=entry.id().equals("piano");
    double depth=piano?(wanted==0?-1.55:.02):(wanted==1?.02:1.7);
    double lateral=wanted==0?.95:wanted==1?1.85:piano?-1.85:-1.10;
    var eye=focus.add(forward.scale(depth)).add(side.scale(lateral)).add(0,wanted==1||piano&&wanted==2?.36:.64,0);
    var toward=focus.subtract(eye);float yaw=(float)(Math.toDegrees(Math.atan2(toward.z,toward.x))-90);
    float pitch=(float)-Math.toDegrees(Math.atan2(toward.y,Math.hypot(toward.x,toward.z)));
    observer.moveTo(eye.x,eye.y-observer.getEyeHeight(),eye.z,yaw,pitch);
    observer.xo=observer.xOld=observer.getX();observer.yo=observer.yOld=observer.getY();observer.zo=observer.zOld=observer.getZ();
    observer.yRotO=yaw;observer.xRotO=pitch;observer.setYHeadRot(yaw);observer.yHeadRotO=yaw;
    mc.options.setCameraType(CameraType.FIRST_PERSON);mc.setCameraEntity(observer);
    if(cameraView!=wanted){cameraView=wanted;viewFrames=0;}
  }
  @SubscribeEvent public static void renderBeginning(RenderFrameEvent.Pre event){
    if(enabled()){renderedThisFrame=false;levelThisFrame=false;}
  }
  @SubscribeEvent public static void rendered(RenderLivingEvent.Post<?,?> event){
    if(!enabled()||phase<0||phase>=caseCount()||SceneShaderPack.shadows()
        ||!(event.getEntity() instanceof ChaoEntity c)||!c.getUUID().equals(actorId))return;
    var mc=Minecraft.getInstance();if(mc.screen!=null||mc.getOverlay()!=null||mc.getCameraEntity()!=observer)return;
    require(c.state().adult==expectedAdult&&!c.isCocooned(),"rendered fixture is the wrong age or a cocoon");
    var model=(ChaoModel)event.getRenderer().getModel();require(model.officialPosesFinite(),"rendered native pose is not finite");
    renderedThisFrame=true;
    boolean active=CASES[phase].id().equals("piano")?ChaoPianoAnimation.active(c):InstrumentPerformance.active(c,CASES[phase].kind());
    if(active)ACTIVE_FRAMES[phase]++;
    if(CASES[phase].interrupt().equals("damage")&&c.hurtTime>0){
      require(model.officialEmotion()==1&&model.eyeExpression()==2&&!ChaoPianoAnimation.active(c),"damage did not replace the piano pose and face");hurtFrames++;
    }
  }
  @SubscribeEvent public static void worldRendered(RenderLevelStageEvent event){
    if(!enabled()||event.getStage()!=RenderLevelStageEvent.Stage.AFTER_LEVEL||SceneShaderPack.shadows())return;
    var mc=Minecraft.getInstance();
    if(mc.screen==null&&mc.getOverlay()==null&&mc.level!=null&&mc.getCameraEntity()==observer&&renderedThisFrame)levelThisFrame=true;
  }
  @SubscribeEvent public static void clientSound(net.neoforged.neoforge.client.event.sound.PlaySoundEvent event){
    if(!enabled()||phase<0||phase>=caseCount()||!armed)return;
    var sound=event.getOriginalSound();if(sound==null)return;
    String expected=CASES[phase].id().equals("piano")?"minecraft:block.note_block.harp":"chao_garden:toy.accordion";
    if(sound.getLocation().toString().equals(expected))CLIENT_SOUND_EVENTS[phase]++;
  }
  private static void capture(Minecraft mc){
    var observed=clientActor(mc);
    if(observed!=null&&CASES[phase].id().equals("piano")&&ChaoPianoAnimation.active(observed)){
      var model=(ChaoModel)((ChaoRenderer)mc.getEntityRenderDispatcher().getRenderer(observed)).getModel();
      float scale=observed.state().immortal?.86f:observed.state().adult?.80f:.64f;
      var source=observed.toyTarget();var facing=mc.level.getBlockState(source).getValue(ChaoToyBlock.FACING);
      float distance=(float)((observed.getX()-source.getX()-.5)*facing.getStepX()+(observed.getZ()-source.getZ()-.5)*facing.getStepZ());
      float clock=ChaoPianoAnimation.phase(observed,0);
      for(boolean left:new boolean[]{false,true}){
        var palm=model.toyPalm(left);require(palm!=null,"active piano has no visible palm");
        var at=ChaoTriangleAnimation.palm(model::transformAccessoryAnchor,palm,left);
        float keyY=(float)(observed.getY()-source.getY())+(1.501f-at.y)*scale;
        float keyZ=.5f-(distance+at.z*scale);
        float keyX=.5f+at.x*scale;
        var arm=model.officialPoses().get(left?"arm_left":"arm_right");
        if(clock>=16&&clock<148){
          require(keyY>.18f&&keyY<.31f&&keyZ>.13f&&keyZ<.19f&&keyX>.07f&&keyX<.93f,"live piano palm missed measured key surface: "+keyX+","+keyY+","+keyZ);
          require(arm.reach<=2.001f,"live piano elongated its forearm");
        }
        LogUtils.getLogger().info("INSTRUMENT048_PIANO_PALM case={} serverTick={} clientPhase={} left={} sourceX={} sourceY={} sourceZ={} reach={}",phase,elapsed,clock,left,keyX,keyY,keyZ,arm.reach);
      }
    }
    if(observed!=null&&CASES[phase].id().equals("accordion")&&ChaoAccordionAnimation.active(observed)){
      var model=(ChaoModel)((ChaoRenderer)mc.getEntityRenderDispatcher().getRenderer(observed)).getModel();
      float clock=InstrumentPerformance.phase(observed,1),weight=InstrumentPerformance.presence(observed,1);
      if(clock>=16&&clock<148&&weight==1){
        for(boolean left:new boolean[]{false,true}){
          var palm=model.toyPalm(left);require(palm!=null,"active accordion has no visible palm");
          var at=ChaoTriangleAnimation.palm(model::transformAccessoryAnchor,palm,left);
          var matrix=ChaoAccordionAnimation.propMatrix(model::transformAccessoryAnchor,weight,CONTACT_CLIP.bellows(clock),left?"bass":"keys");
          var target=left?ChaoAccordionAnimation.bassTarget(matrix,CONTACT_CLIP.bass(clock)):ChaoAccordionAnimation.keyTarget(matrix,CONTACT_CLIP.keyHeight(clock),CONTACT_CLIP.keys(clock));
          require(at.distance(target)<.035f,"live accordion hand lost its real end section");
          require(model.officialPoses().get(left?"arm_left":"arm_right").reach<=ChaoAccordionAnimation.MAX_REACH+.001f,"live accordion elongated its forearm");
        }
      }
    }
    String name=String.format("instrument048-%02d-%s-%s-%s-t%03d-v%d.png",phase,CASES[phase].id(),CASES[phase].variant(),
        CASES[phase].facing().getSerializedName(),elapsed,captureView);
    int capturedPhase=phase,capturedView=captureView;long capturedTick=elapsed;
    Screenshot.grab(mc.gameDirectory,name,mc.getMainRenderTarget(),message->{
      require(Files.isRegularFile(mc.gameDirectory.toPath().resolve("screenshots").resolve(name)),"screenshot was not saved: "+message.getString());savedShots++;
      LogUtils.getLogger().info("INSTRUMENT048_CAPTURE {} case={} tick={} view={} renderedWorldFrames={} shader={} realtime={}",
          name,capturedPhase,capturedTick,capturedView,WORLD_FRAMES[capturedPhase],SceneShaderPack.active(),CASES[capturedPhase].realtime());
    });
    VIEW_SHOTS[phase][captureView]++;shots++;
  }
  private static void finishRenderedCase(Minecraft mc){
    require(ACTIVE_FRAMES[phase]>=20,"actual production animation did not render enough frames");
    int completePhase=phase;
    LogUtils.getLogger().info("INSTRUMENT048_RENDER_COMPLETE {} worldFrames={} activeFrames={} captures={} views=3 realtime={} wallSeconds={} clientSoundEvents={}",
        phase,WORLD_FRAMES[phase],ACTIVE_FRAMES[phase],java.util.Arrays.stream(VIEW_SHOTS[phase]).sum(),CASES[phase].realtime(),
        (System.nanoTime()-playbackWall)/1_000_000_000.0,CLIENT_SOUND_EVENTS[phase]);
    // Block repeated render callbacks until the server publishes the next actor.
    clientPhase=-2;
    mc.getSingleplayerServer().execute(()->{if(phase==completePhase){if(phase+1<caseCount())next(mc.getSingleplayerServer(),phase+1);else phase=caseCount();}});
  }
  private static void step(Minecraft mc){
    if(stepOutstanding||caseComplete)return;
    stepOutstanding=true;stepCompleted=false;stepFrames=0;int expectedPhase=phase;
    mc.getSingleplayerServer().execute(()->{
      if(phase!=expectedPhase)return;
      require(mc.getSingleplayerServer().tickRateManager().stepGameIfPaused(1),"native tick manager rejected one-tick step");
    });
  }
  @SubscribeEvent public static void frameFinished(RenderFrameEvent.Post event){
    if(!enabled()||!levelThisFrame||!renderedThisFrame||phase<0||phase>=caseCount()||clientPhase!=phase)return;
    var mc=Minecraft.getInstance();WORLD_FRAMES[phase]++;viewFrames++;stepFrames++;
    if(!armRequested&&viewFrames>=10){
      armRequested=true;int expectedPhase=phase;mc.getSingleplayerServer().execute(()->arm(mc.getSingleplayerServer(),expectedPhase));return;
    }
    if(!armed)return;
    if(CASES[phase].realtime()){
      if(elapsed>=0)TIMING_FRAMES[phase][elapsed<24?0:elapsed<148?1:2]++;
      if(elapsed>=nextRealtimeCapture&&elapsed<CASES[phase].finish()&&viewFrames>=3){capture(mc);nextRealtimeCapture=elapsed+4;}
      if(caseComplete){
        for(int view=0;view<3;view++)require(VIEW_SHOTS[phase][view]>=3,"realtime phrase missed a camera angle");
        for(int part=0;part<3;part++)require(TIMING_FRAMES[phase][part]>=3,"realtime phrase did not actually render entry, play and exit");
        require(CLIENT_SOUND_EVENTS[phase]==(CASES[phase].id().equals("piano")?16:8),"realtime phrase did not deliver its actual client sound events");
        finishRenderedCase(mc);
      }
      return;
    }
    if(stepOutstanding){
      // Both the server and the actual client world must have consumed the
      // native step. Menu/shadow frames can never satisfy this acknowledgement.
      if(!stepCompleted||stepFrames<3||mc.level.getGameTime()<publishedTime)return;
      stepOutstanding=false;viewFrames=0;
    }
    var captures=CASES[phase].captures();
    if(captureIndex<captures.length&&elapsed>=captures[captureIndex]){
      if(viewFrames<3||cameraView!=captureView)return;
      capture(mc);viewFrames=0;
      if(++captureView==3){captureView=0;captureIndex++;}
      return;
    }
    if(caseComplete){
      require(captureIndex==captures.length,"native session ended before every actual pose capture");
      for(int view=0;view<3;view++)require(VIEW_SHOTS[phase][view]==captures.length,"one camera angle missed pose captures");
      finishRenderedCase(mc);
      return;
    }
    if(viewFrames>=2)step(mc);
  }
  @SubscribeEvent public static void client(ClientTickEvent.Post event){
    if(!enabled()||finished)return;var mc=Minecraft.getInstance();ticks++;
    if(wallBegan==0)wallBegan=System.nanoTime();
    require(ticks<24000&&System.nanoTime()-wallBegan<1_200_000_000_000L,"actual-client study timed out");
    if(mc.screen instanceof AccessibilityOnboardingScreen)mc.setScreen(new TitleScreen());
    if(!started&&mc.screen instanceof TitleScreen){
      started=true;mc.options.renderDistance().set(3);mc.options.simulationDistance().set(5);mc.options.framerateLimit().set(30);
      mc.options.fov().set(45);mc.options.pauseOnLostFocus=false;mc.options.hideGui=true;
      mc.options.tutorialStep=net.minecraft.client.tutorial.TutorialSteps.NONE;
      mc.createWorldOpenFlows().createFreshLevel("Instrument048-"+System.currentTimeMillis(),
          new LevelSettings("Instrument048",GameType.CREATIVE,false,Difficulty.PEACEFUL,true,new GameRules(),WorldDataConfiguration.DEFAULT),
          new WorldOptions(2548L,false,false),r->r.registryOrThrow(Registries.WORLD_PRESET).getHolderOrThrow(WorldPresets.FLAT).value().createWorldDimensions(),mc.screen);
    }
    if(!setup&&mc.player!=null&&mc.getSingleplayerServer()!=null){
      setup=true;mc.setScreen(null);ChaoRenderer.suppressNames=true;
      mc.getSingleplayerServer().execute(()->{
        var server=mc.getSingleplayerServer();var level=server.overworld();level.setDayTime(6000);
        level.getGameRules().getRule(GameRules.RULE_DAYLIGHT).set(false,server);level.getGameRules().getRule(GameRules.RULE_DOMOBSPAWNING).set(false,server);
        for(var pos:BlockPos.betweenClosed(-7,79,-7,7,85,9))level.setBlock(pos,pos.getY()==79?Blocks.SMOOTH_STONE.defaultBlockState():Blocks.AIR.defaultBlockState(),3);
        server.getPlayerList().getPlayers().forEach(player->player.setGameMode(GameType.SPECTATOR));
        server.tickRateManager().setFrozen(true);next(server,caseStart());
      });
    }
    if(phase>=0&&phase<caseCount()&&mc.level!=null&&mc.player!=null){
      if(clientPhase!=phase&&clientPhase!=-2){
        clientPhase=phase;captureIndex=captureView=viewFrames=stepFrames=0;cameraView=-1;armRequested=false;nextRealtimeCapture=0;
      }else if(clientPhase==-2&&actorId!=null){
        // next() publishes a different phase only after its new actor is ready.
        if(caseComplete)return;clientPhase=phase;captureIndex=captureView=viewFrames=stepFrames=0;cameraView=-1;armRequested=false;nextRealtimeCapture=0;
      }
      var c=clientActor(mc);if(c!=null)camera(mc,c);
    }
    if(phase==caseCount()&&savedShots==shots){
      require(caseCount()<9||hurtFrames>0,"damage interruption never rendered");
      for(int index=caseStart();index<caseCount();index++)require(WORLD_FRAMES[index]>20&&ACTIVE_FRAMES[index]>20,"case has no actual world/actor rendering evidence");
      finished=true;mc.getSingleplayerServer().execute(()->mc.getSingleplayerServer().tickRateManager().setFrozen(false));
      LogUtils.getLogger().info("INSTRUMENT048_VISUAL_COMPLETE cases={} start={} shots={} saved={} hurtFrames={} audioListening=false",
          caseCount()-caseStart(),caseStart(),shots,savedShots,hurtFrames);mc.stop();
    }
  }
}
