package com.chaogarden.toys;

import com.chaogarden.ChaoGarden;
import com.chaogarden.activities.ActivityManager;
import com.chaogarden.companion.CompanionContent;
import com.chaogarden.core.ChaoFaceRules076;
import com.chaogarden.core.ChaoIdleRules078;
import com.chaogarden.entity.ChaoEntity;
import com.chaogarden.kindergarten.KindergartenContent;
import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;
import net.minecraft.commands.Commands;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.EntityLeaveLevelEvent;

/** Temporary, reward-free operator playback. Reuses the normal synced toy clock and renderer. */
@EventBusSubscriber(modid=ChaoGarden.MODID)
public final class ToyPreview {
  public static final String SITTING_KIND="preview_sit";
  private static final java.util.List<String> POSES=java.util.List.of("sit","crawl","paddle","swim","glide","flail");
  /** These temporary tracked tokens select the existing production locomotion poses. */
  public static String poseId(String kind){
    for(String id:POSES)if(!id.equals("sit")&&("preview_"+id).equals(kind))return id;
    return "";
  }
  private static String poseKind(String id){return POSES.contains(id)?"preview_"+id:"";}
  private static String faceKind(String id){
    String kind=id.startsWith("face_")?"preview_"+id:"";
    return ChaoFaceRules076.isFacePreviewKind(kind)?kind:"";
  }
  /** Temporary toy metadata selects the same short idle clip as a natural IDLE detail. */
  public static String idleId(String kind){
    if(kind==null||!kind.startsWith("preview_idle_"))return "";
    String id=kind.substring("preview_idle_".length());return ChaoIdleRules078.valid(id)?id:"";
  }
  private static String idleKind(String id){
    return id.startsWith("idle_")&&ChaoIdleRules078.valid(id.substring(5))?"preview_"+id:"";
  }
  private static final String RESTORE_AI="ChaoToyPreviewRestoreNoAi";
  private static final Map<ChaoEntity,Session> ACTIVE=new WeakHashMap<>();
  private static final class Session {
    final String kind;final boolean loop;final UUID keeper;long start,lastTick=Long.MIN_VALUE;
    final float originalYaw,originalPitch,originalHeadYaw,originalBodyYaw;
    BallController074 ball;net.minecraft.world.entity.ai.goal.Goal ballGoal;FloatToyEntity080 vessel;
    Session(String kind,boolean loop,ServerPlayer keeper,long now,ChaoEntity c){this.kind=kind;this.loop=loop;this.keeper=keeper.getUUID();start=now;originalYaw=c.getYRot();originalPitch=c.getXRot();originalHeadYaw=c.getYHeadRot();originalBodyYaw=c.yBodyRot;}
  }
  private ToyPreview() {}
  public static boolean active(ChaoEntity c){return ACTIVE.containsKey(c);}
  private static boolean controlledFloat(ChaoEntity c){
    var s=ACTIVE.get(c);return s!=null&&s.vessel!=null&&!s.vessel.isRemoved()&&s.vessel.rider()==c;
  }
  public static String unavailable(ChaoEntity c){
    if(c.getType()!=ChaoGarden.CHAO.get()||KindergartenContent.isBoarded(c))return "School residents and enrolled students cannot be previewed. Retrieve your student first.";
    if(!c.isAlive()||c.isCocooned()||c.isResting()||c.isEating()||c.isHandlingItem()||c.isPassenger()&&!controlledFloat(c)||c.isVehicle()
        ||c.isBeingPetted()||c.hurtTime>0||c.isBehaving()||ActivityManager.isBusy(c)||CompanionContent.isManaged(c)
        ||c.getPersistentData().getBoolean("ChaoToyDismountWait")||c.state().intTrait("content.construction")>0)
      return "This Chao is busy. Let its current activity finish first.";
    if(!c.getHeldChaoItem().isEmpty()||!c.getMainHandItem().isEmpty())return "Let the Chao put down its item first.";
    if(c.officialComparison())return "Use /chao appearance garden first to enable gameplay animations.";
    if(!controlledFloat(c)&&(c.isInWater()||!c.onGround()&&!(active(c)&&(ToyContent.mounted(ACTIVE.get(c).kind)
        ||BallPlayRules074.KIND.equals(ACTIVE.get(c).kind)&&supportedBallFloor(c)))))return "Place the Chao on dry, solid ground first.";
    if(!active(c)&&(c.toyPlayingTicks()>0||c.getPersistentData().getLong("ChaoToyApproachUntil")>c.level().getGameTime()))return "This Chao is already approaching or using a placed toy.";
    return "";
  }
  /** A first zero-vertical-motion physics tick can clear onGround before gravity meets this same floor. */
  private static boolean supportedBallFloor(ChaoEntity c){
    var floor=net.minecraft.core.BlockPos.containing(c.getX(),c.getY()-.01,c.getZ());double height=floor.getY()+1;
    return c.getY()>=height-.001&&c.getY()<=height+.02&&c.getDeltaMovement().y<=.0001&&c.getDeltaMovement().y>=-.10
        &&c.level().hasChunkAt(floor)&&c.level().getBlockState(floor).isFaceSturdy(c.level(),floor,net.minecraft.core.Direction.UP)
        &&!c.level().containsAnyLiquid(c.getBoundingBox().deflate(.0001))&&c.level().noCollision(c,c.getBoundingBox());
  }
  public static String start(ChaoEntity c,ServerPlayer keeper,String id,boolean loop){
    String idle=idleKind(id);if(!idle.isEmpty())return startKind(c,keeper,idle,loop);
    String face=faceKind(id);
    if(!face.isEmpty()){
      if(c.state().immortal||!"normal".equals(c.state().character))
        return "Native Chaos and character faces keep their own artwork; face previews apply to ordinary Chao.";
      return startKind(c,keeper,face,loop);
    }
    if(id.equals("lifesaver")||id.equals("rubber_duck")){
      String problem=unavailable(c);if(!problem.isEmpty())return problem;
      stop(c);String kind="chao_"+id;
      var vessel=FloatToyContent080.nearby(c,1.1).stream().filter(f->f.kind().equals(kind)&&f.rider()==null
          &&(f.claimant().isEmpty()||f.claimedBy(c))&&c.distanceToSqr(f)<=1.21)
          .min(java.util.Comparator.comparingDouble(c::distanceToSqr)).orElse(null);
      if(vessel==null)return "Place the matching float and bring the Chao to a clear supported boarding edge within one block.";
      problem=startKind(c,keeper,kind,loop);if(!problem.isEmpty())return problem;
      ACTIVE.get(c).vessel=vessel;vessel.previewHeld(true);
      if(!vessel.board(c)){stop(c);return "The float's local entry is blocked, occupied, or no longer available.";}
      return "";
    }
    if(id.equals("ball")){
      String problem=unavailable(c);if(!problem.isEmpty())return problem;
      stop(c);var at=ToyContent.previewBall(c);
      if(at==null)return "Place an available ball within three blocks with a reachable clear side and solid floor.";
      c.sync(); // Refresh the ordinary derived locomotion speed even before a first AI tick.
      problem=startKind(c,keeper,BallPlayRules074.KIND,loop);
      if(!problem.isEmpty())return problem;
      var session=ACTIVE.get(c);session.ball=ToyContent.beginBallPreview(c,at);
      if(session.ball==null){stop(c);return "The ball no longer has a reachable, available play position.";}
      session.ballGoal=new BallPreviewGoal(c);c.goalSelector.addGoal(-100,session.ballGoal);c.setNoAi(false);
      return "";
    }
    if(id.equals("tv")){
      String problem=unavailable(c);if(!problem.isEmpty())return problem;
      stop(c);var at=ToyContent.previewTv(c);
      if(at==null)return "Place a TV and bring the Chao to its clear screen-side seat (within 0.4 blocks).";
      problem=startKind(c,keeper,TvWatchRules073.KIND,loop);
      if(problem.isEmpty()&&!ToyContent.beginTvPreview(c,at)){stop(c);return "The TV seat is blocked, reserved or no longer available.";}
      return problem;
    }
    if(id.equals("jack_in_box")){
      String problem=unavailable(c);if(!problem.isEmpty())return problem;
      stop(c);var at=ToyContent.previewJack(c);
      if(at==null)return "Place a jack-in-the-box and bring the Chao to its clear front seat (within 0.4 blocks).";
      problem=startKind(c,keeper,JackPlayRules080.KIND,loop);
      if(problem.isEmpty()&&!ToyContent.beginJackPreview(c,at)){stop(c);return "The jack-in-the-box seat is blocked, reserved or no longer available.";}
      return problem;
    }
    if(id.equals("radio")){
      String problem=unavailable(c);if(!problem.isEmpty())return problem;
      stop(c);var at=ToyContent.previewRadio(c);
      if(at==null)return "Place a radio and bring the Chao to its clear front listening seat (within 0.4 blocks).";
      problem=startKind(c,keeper,RadioPlayRules075.KIND,loop);
      if(problem.isEmpty()&&!ToyContent.beginRadioPreview(c,at)){stop(c);return "The radio seat is blocked, reserved or no longer available.";}
      return problem;
    }
    if(id.equals("rocking_horse")||id.equals("pogo_stick")){
      String kind="chao_"+id;String problem=unavailable(c);if(!problem.isEmpty())return problem;
      stop(c);var at=ToyContent.previewMounted(c,kind);if(at==null)return "Place a "+id+" nearby with clear supported ground and room to ride.";
      problem=startKind(c,keeper,kind,loop);
      if(problem.isEmpty()&&!ToyContent.beginMountedPreview(c,at)){stop(c);return "The toy no longer has clear mounting space.";}
      return problem;
    }
    if(id.equals("piano")||id.equals("organ")){
      String kind="chao_"+id;
      String problem=unavailable(c);if(!problem.isEmpty())return problem;
      stop(c);var at=ToyContent.previewKeyboard(c,kind);
      if(at==null)return "Place a "+id+" within three blocks with clear, solid ground at its keys.";
      problem=startKind(c,keeper,kind,loop);
      if(problem.isEmpty())ToyContent.beginKeyboardPreview(c,at);return problem;
    }
    var clip=HandToyEffects.find(id);if(clip==null)return "Unknown preview. Use /chao testtoy list.";
    return startKind(c,keeper,clip.kind(),loop);
  }
  public static String startSitting(ChaoEntity c,ServerPlayer keeper,boolean loop){return startKind(c,keeper,SITTING_KIND,loop);}
  private static String startKind(ChaoEntity c,ServerPlayer keeper,String kind,boolean loop){
    String problem=unavailable(c);if(!problem.isEmpty())return problem;
    stop(c);
    problem=unavailable(c);if(!problem.isEmpty())return problem;
    if(!ToyContent.proneSpaceReady(c,kind,c.position(),c.getYRot(),null))return "Give the Chao clear, supported floor space for this toy.";
    c.getPersistentData().putBoolean(RESTORE_AI,c.isNoAi());c.getNavigation().stop();c.setDeltaMovement(Vec3.ZERO);c.setNoAi(true);
    ACTIVE.put(c,new Session(kind,loop,keeper,c.level().getGameTime(),c));c.setToyPlaying(kind,ToyContent.PLAY_TICKS);
    return "";
  }
  /** Invoked before keeper simulation. Vanilla physics and damage still tick; care/age and rewards do not. */
  public static boolean tick(ChaoEntity c){
    var s=ACTIVE.get(c);if(s==null)return false;
    var keeper=c.getServer().getPlayerList().getPlayer(s.keeper);
    String unavailableReason=unavailable(c);
    if(keeper==null||keeper.level()!=c.level()||keeper.distanceToSqr(c)>64*64||!unavailableReason.isEmpty()
        ||!ToyContent.proneSpaceReady(c,s.kind,c.position(),c.getYRot(),null)){
      if(s.kind.equals(BallPlayRules074.KIND))com.mojang.logging.LogUtils.getLogger().debug("BALL074_PREVIEW_CANCEL elapsed={} keeper={} sameLevel={} distance={} unavailable={} onGround={} noAi={} pos={} velocity={} target={} remaining={} state={}",c.level().getGameTime()-s.start,keeper!=null,keeper!=null&&keeper.level()==c.level(),keeper==null?-1:keeper.distanceToSqr(c),unavailableReason,c.onGround(),c.isNoAi(),c.position(),c.getDeltaMovement(),c.toyTarget(),c.toyPlayingTicks(),c.state().encode());
      stop(c);return false;
    }
    long now=c.level().getGameTime();if(s.lastTick==now)return true;s.lastTick=now;
    long elapsed=now-s.start;
    if(elapsed>=ToyContent.PLAY_TICKS){
      if(!s.loop){stop(c);return true;}
      s.start=now;elapsed=0;
      if(s.vessel!=null){c.setToyPlaying(s.kind,ToyContent.PLAY_TICKS);}
      else if(BallPlayRules074.KIND.equals(s.kind)){
        if(!ToyContent.renewBallPreview(c)){stop(c);return true;}
      }else if(ToyInteractionRules.keyboard(s.kind)){
        var at=c.toyTarget();if(at==null||!ToyContent.holdKeyboard(c,at)){stop(c);return true;}
        ToyContent.beginKeyboardPreview(c,at);
      }else if(ToyContent.mounted(s.kind)){
        var at=c.toyTarget();if(at==null||!ToyContent.holdMounted(c,at,160)||!ToyContent.beginMountedPreview(c,at)){stop(c);return true;}
      }else if(TvWatchRules073.KIND.equals(s.kind)){
        var at=c.toyTarget();if(at==null||!ToyContent.beginTvPreview(c,at)){stop(c);return true;}
      }else if(RadioPlayRules075.KIND.equals(s.kind)){
        var at=c.toyTarget();if(at==null||!ToyContent.beginRadioPreview(c,at)){stop(c);return true;}
      }else if(JackPlayRules080.KIND.equals(s.kind)){
        var at=c.toyTarget();if(at==null||!ToyContent.beginJackPreview(c,at)){stop(c);return true;}
      }else c.setToyPlaying(s.kind,ToyContent.PLAY_TICKS);
    }
    if(s.vessel!=null){
      if(s.vessel.isRemoved()||s.vessel.rider()!=c){stop(c);return true;}
      s.vessel.previewHeld(true);return true;
    }
    if(BallPlayRules074.KIND.equals(s.kind)){
      if(s.ball==null||!s.ball.tick()){
        com.mojang.logging.LogUtils.getLogger().debug("BALL074_CONTROLLER_CANCEL elapsed={} reason={} onGround={} pos={} velocity={} target={} action={} navDone={} state={}",elapsed,s.ball==null?"missing controller":s.ball.lastFailure(),c.onGround(),c.position(),c.getDeltaMovement(),c.toyTarget(),c.toyKickElapsed(),c.getNavigation().isDone(),c.state().encode());stop(c);return true;
      }
      return true;
    }
    c.getNavigation().stop();c.setDeltaMovement(new Vec3(0,c.getDeltaMovement().y,0));
    if(ToyInteractionRules.keyboard(s.kind)){
      var at=c.toyTarget();
      if(at==null||!c.level().getBlockState(at).is(ToyContent.block(s.kind))||!ToyContent.holdKeyboard(c,at)){stop(c);return true;}
      if(s.kind.equals(OrganPlayRules.KIND))OrganPlayRules.cue(c,(int)elapsed);else PianoPlayRules.cue(c,(int)elapsed);
    }else if(ToyContent.mounted(s.kind)){
      var at=c.toyTarget();if(at==null||!ToyContent.holdMounted(c,at,elapsed)){stop(c);return true;}
      HandToyEffects.tick(c,s.kind,(int)elapsed);
    }else if(TvWatchRules073.KIND.equals(s.kind)){
      var at=c.toyTarget();if(at==null||!ToyContent.holdTv(c,at)){stop(c);return true;}
    }else if(RadioPlayRules075.KIND.equals(s.kind)){
      var at=c.toyTarget();if(at==null||!ToyContent.holdRadio(c,at)){stop(c);return true;}
    }else if(JackPlayRules080.KIND.equals(s.kind)){
      var at=c.toyTarget();if(at==null||!ToyContent.holdJack(c,at)){stop(c);return true;}
    }else HandToyEffects.tick(c,s.kind,(int)elapsed);return true;
  }
  public static boolean stop(ChaoEntity c){
    var session=ACTIVE.remove(c);boolean was=session!=null;
    if(was||c.getPersistentData().contains(RESTORE_AI)){
      if(session!=null&&session.vessel!=null){session.vessel.previewHeld(false);session.vessel.requestLand();if(session.vessel.rider()!=c)session.vessel.release(c);}
      if(session!=null&&session.ballGoal!=null){
        c.goalSelector.removeGoal(session.ballGoal);c.getMoveControl().setWantedPosition(c.getX(),c.getY(),c.getZ(),0);c.setSpeed(0);c.setXxa(0);c.setZza(0);c.setDeltaMovement(new Vec3(0,c.getDeltaMovement().y,0));
      }
      if(session!=null&&(TvWatchRules073.KIND.equals(session.kind)||RadioPlayRules075.KIND.equals(session.kind)||JackPlayRules080.KIND.equals(session.kind))){
        c.setYRot(session.originalYaw);c.setXRot(session.originalPitch);c.setYHeadRot(session.originalHeadYaw);c.setYBodyRot(session.originalBodyYaw);
      }
      if(c.getPersistentData().contains(RESTORE_AI))c.setNoAi(c.getPersistentData().getBoolean(RESTORE_AI));
      if(c.toyTarget()!=null)ToyContent.endPlaying(c,false);
      c.getPersistentData().remove(RESTORE_AI);c.setToyPlaying("",0);c.getNavigation().stop();return true;
    }
    return false;
  }
  /** A saved preview never resumes or leaves its temporary NoAI flag behind. */
  public static void restoreAfterLoad(ChaoEntity c){stop(c);}
  /** Only this temporary goal is inserted/removed; ordinary goals and their definitions survive. */
  private static final class BallPreviewGoal extends net.minecraft.world.entity.ai.goal.Goal {
    private final ChaoEntity chao;
    BallPreviewGoal(ChaoEntity chao){this.chao=chao;setFlags(java.util.EnumSet.allOf(Flag.class));}
    @Override public boolean canUse(){var s=ACTIVE.get(chao);return s!=null&&s.kind.equals(BallPlayRules074.KIND);}
    @Override public boolean canContinueToUse(){return canUse();}
    @Override public boolean isInterruptable(){return false;}
    @Override public boolean requiresUpdateEveryTick(){return true;}
  }
  /** Save ordinary state even if the world is subsequently opened with an older fallback JAR. */
  public static void prepareSave(ChaoEntity c,net.minecraft.nbt.CompoundTag tag){
    if(active(c)){
      tag.putBoolean("NoAI",c.getPersistentData().getBoolean(RESTORE_AI));
      tag.remove("ToyPlayingKind");tag.remove("ToyPlayingRemaining");
      var session=ACTIVE.get(c);
      if(session!=null&&(TvWatchRules073.KIND.equals(session.kind)||RadioPlayRules075.KIND.equals(session.kind)||JackPlayRules080.KIND.equals(session.kind))){
        var rotation=tag.getList("Rotation",net.minecraft.nbt.Tag.TAG_FLOAT);
        if(rotation.size()==2){rotation.set(0,net.minecraft.nbt.FloatTag.valueOf(session.originalYaw));rotation.set(1,net.minecraft.nbt.FloatTag.valueOf(session.originalPitch));}
      }
    }
  }
  @SubscribeEvent public static void left(EntityLeaveLevelEvent e){if(!e.getLevel().isClientSide&&e.getEntity() instanceof ChaoEntity c)stop(c);}
  @SubscribeEvent public static void commands(RegisterCommandsEvent e){
    var root=Commands.literal("testtoy").requires(s->s.hasPermission(2)).executes(c->help(c.getSource()));
    root.then(Commands.literal("list").executes(c->help(c.getSource())));
    root.then(Commands.literal("stop").executes(c->run(c.getSource(),"","stop")));
    for(var clip:HandToyEffects.clips()){
      var toy=Commands.literal(clip.id()).executes(c->run(c.getSource(),clip.id(),"once"));
      for(String mode:java.util.List.of("once","loop","stop"))toy.then(Commands.literal(mode).executes(c->run(c.getSource(),clip.id(),mode)));
      root.then(toy);
    }
    var organ=Commands.literal("organ").executes(c->run(c.getSource(),"organ","once"));
    for(String mode:java.util.List.of("once","loop","stop"))organ.then(Commands.literal(mode).executes(c->run(c.getSource(),"organ",mode)));
    root.then(organ);
    var piano=Commands.literal("piano").executes(c->run(c.getSource(),"piano","once"));
    for(String mode:java.util.List.of("once","loop","stop"))piano.then(Commands.literal(mode).executes(c->run(c.getSource(),"piano",mode)));
    root.then(piano);
    var tv=Commands.literal("tv").executes(c->run(c.getSource(),"tv","once"));
    for(String mode:java.util.List.of("once","loop","stop"))tv.then(Commands.literal(mode).executes(c->run(c.getSource(),"tv",mode)));
    root.then(tv);
    var ball=Commands.literal("ball").executes(c->run(c.getSource(),"ball","once"));
    for(String mode:java.util.List.of("once","loop","stop"))ball.then(Commands.literal(mode).executes(c->run(c.getSource(),"ball",mode)));
    root.then(ball);
    var jack=Commands.literal("jack_in_box").executes(c->run(c.getSource(),"jack_in_box","once"));
    for(String mode:java.util.List.of("once","loop","stop"))jack.then(Commands.literal(mode).executes(c->run(c.getSource(),"jack_in_box",mode)));
    root.then(jack);
    var radio=Commands.literal("radio").executes(c->run(c.getSource(),"radio","once"));
    for(String mode:java.util.List.of("once","loop","stop"))radio.then(Commands.literal(mode).executes(c->run(c.getSource(),"radio",mode)));
    root.then(radio);
    for(String id:java.util.List.of("lifesaver","rubber_duck")){
      var toy=Commands.literal(id).executes(c->run(c.getSource(),id,"once"));
      for(String mode:java.util.List.of("once","loop","stop"))toy.then(Commands.literal(mode).executes(c->run(c.getSource(),id,mode)));
      root.then(toy);
    }
    for(String id:ChaoFaceRules076.FACE_PREVIEW_IDS){
      String alias="face_"+id;
      var face=Commands.literal(alias).executes(c->run(c.getSource(),alias,"once"));
      for(String mode:java.util.List.of("once","loop","stop"))face.then(Commands.literal(mode).executes(c->run(c.getSource(),alias,mode)));
      root.then(face);
    }
    for(String id:ChaoIdleRules078.IDS){
      String alias="idle_"+id;
      var idle=Commands.literal(alias).executes(c->run(c.getSource(),alias,"once"));
      for(String mode:java.util.List.of("once","loop","stop"))idle.then(Commands.literal(mode).executes(c->run(c.getSource(),alias,mode)));
      root.then(idle);
    }
    e.getDispatcher().register(Commands.literal("chao").then(root));
    var poseRoot=Commands.literal("testpose").requires(s->s.hasPermission(2)).executes(c->poseHelp(c.getSource()));
    for(String id:POSES){
      var pose=Commands.literal(id).executes(c->run(c.getSource(),id,"once"));
      for(String mode:java.util.List.of("once","loop","stop"))pose.then(Commands.literal(mode).executes(c->run(c.getSource(),id,mode)));
      poseRoot.then(pose);
    }
    poseRoot.then(Commands.literal("stop").executes(c->run(c.getSource(),"","stop")));
    poseRoot.then(Commands.literal("list").executes(c->poseHelp(c.getSource())));
    e.getDispatcher().register(Commands.literal("chao").then(poseRoot));
    var faceRoot=Commands.literal("testface").requires(s->s.hasPermission(2)).executes(c->faceHelp(c.getSource()));
    faceRoot.then(Commands.literal("list").executes(c->faceHelp(c.getSource())));
    faceRoot.then(Commands.literal("stop").executes(c->run(c.getSource(),"","stop")));
    for(String id:ChaoFaceRules076.FACE_PREVIEW_IDS){
      var face=Commands.literal(id).executes(c->run(c.getSource(),"face_"+id,"once"));
      for(String mode:java.util.List.of("once","loop","stop"))face.then(Commands.literal(mode).executes(c->run(c.getSource(),"face_"+id,mode)));
      faceRoot.then(face);
    }
    e.getDispatcher().register(Commands.literal("chao").then(faceRoot));
    var idleRoot=Commands.literal("testidle").requires(s->s.hasPermission(2)).executes(c->idleHelp(c.getSource()));
    idleRoot.then(Commands.literal("list").executes(c->idleHelp(c.getSource())));
    idleRoot.then(Commands.literal("stop").executes(c->run(c.getSource(),"","stop")));
    for(String id:ChaoIdleRules078.IDS){
      var idle=Commands.literal(id).executes(c->run(c.getSource(),"idle_"+id,"once"));
      for(String mode:java.util.List.of("once","loop","stop"))idle.then(Commands.literal(mode).executes(c->run(c.getSource(),"idle_"+id,mode)));
      idleRoot.then(idle);
    }
    e.getDispatcher().register(Commands.literal("chao").then(idleRoot));
  }
  private static int idleHelp(CommandSourceStack source){
    source.sendSuccess(()->Component.literal("/chao testidle <"+String.join("|",ChaoIdleRules078.IDS)+"> [once|loop|stop] — nearest Chao within 12 blocks on dry solid ground; busy Chao must finish their activity first. Reuses each production idle pose and face, then rests calmly until tick 160; loops restart every 160 ticks. Tired previews an expression without changing real fatigue. No saved care, genes, age, items or rewards change. /chao testidle stop or /chao testtoy stop ends playback; interaction, damage, unload, logout and leaving 64 blocks/dimension also stop it."),false);return 1;
  }
  private static int faceHelp(CommandSourceStack source){
    source.sendSuccess(()->Component.literal("/chao testface <"+String.join("|",ChaoFaceRules076.FACE_PREVIEW_IDS)+"> [once|loop|stop] — ordinary Chao within 12 blocks on dry solid ground. Reuses production facial expressions without changing mood, illness, genes or care. Once lasts 160 ticks; loops and stop use the existing preview controls. /chao testface stop or /chao testtoy stop ends the nearest active preview. Native Chaos and character artwork is preserved."),false);return 1;
  }
  private static int poseHelp(CommandSourceStack source){
    source.sendSuccess(()->Component.literal("/chao testpose <"+String.join("|",POSES)+"> [once|loop|stop] — existing production poses and clock on the nearest Chao within 12 blocks, with standing entry/exit. No rewards or saved changes. /chao testpose stop ends playback; interaction, damage, unload, logout and leaving range also stop it."),false);return 1;
  }
  private static int help(CommandSourceStack source){
    source.sendSuccess(()->Component.literal("/chao testtoy <toy> [once|loop|stop] — nearest Chao within 12 blocks. Toys: "+String.join(", ",HandToyEffects.clips().stream().map(HandToyEffects.Clip::id).toList())+", piano, organ, tv, ball, radio, jack_in_box, lifesaver, rubber_duck. Floats use the real local vessel and its safe boarding/landing controller; stop releases the preview immediately and may retain the passenger until safe land is reachable. Hand toys need no lessons/items; keyboards and riding toys need their placed toy. TV/radio/jack-in-the-box need their matching front seat within 0.4 blocks; ball needs a reachable ball within three blocks and keeps real moved positions."),false);
    source.sendSuccess(()->Component.literal("Idles: "+String.join(", ",ChaoIdleRules078.IDS.stream().map(id->"idle_"+id).toList())+"; also /chao testidle <idle> [once|loop|stop]. Each short production idle returns to calm until tick 160; loops restart every 160 ticks. Tired never changes real fatigue. Faces: "+String.join(", ",ChaoFaceRules076.FACE_PREVIEW_IDS.stream().map(id->"face_"+id).toList())+"; also /chao testface <expression> [once|loop|stop]."),false);
    source.sendSuccess(()->Component.literal("Production poses: /chao testpose <"+String.join("|",POSES)+"> [once|loop|stop]. Carrying: /chao testcarry once|loop|stop with an equipped Chao. No rewards or saved care, genes, age or item changes. /chao testtoy stop ends playback; interaction, damage, unload, logout and leaving 64 blocks/dimension also stop it."),false);return 1;
  }
  private static int run(CommandSourceStack source,String id,String mode) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
    var player=source.getPlayerOrException();
    var c=source.getLevel().getEntitiesOfClass(ChaoEntity.class,new AABB(source.getPosition(),source.getPosition()).inflate(12),
        chao->chao.getType()==ChaoGarden.CHAO.get()&&(!mode.equals("stop")||active(chao)))
        .stream().filter(chao->chao.distanceToSqr(source.getPosition())<=144)
        .min(java.util.Comparator.comparingDouble(chao->chao.distanceToSqr(source.getPosition()))).orElse(null);
    if(c==null){source.sendFailure(Component.literal(mode.equals("stop")?"No active Chao preview within 12 blocks.":"No Chao within 12 blocks."));return 0;}
    if(mode.equals("stop")){
      String expected=!poseKind(id).isEmpty()?poseKind(id):!faceKind(id).isEmpty()?faceKind(id):!idleKind(id).isEmpty()?idleKind(id):"chao_"+id;
      if(!id.isEmpty()&&!ACTIVE.get(c).kind.equals(expected)){source.sendFailure(Component.literal("The nearest preview is using a different toy. Use /chao testtoy stop."));return 0;}
      stop(c);source.sendSuccess(()->Component.literal("Stopped preview for "+c.getName().getString()+"."),false);return 1;
    }
    String problem=!poseKind(id).isEmpty()?startKind(c,player,poseKind(id),mode.equals("loop")):start(c,player,id,mode.equals("loop"));
    if(!problem.isEmpty()){source.sendFailure(Component.literal(problem));return 0;}
    source.sendSuccess(()->Component.literal("Previewing "+id+" "+mode+" on "+c.getName().getString()+". /chao testtoy stop ends playback."),false);return 1;
  }
}
