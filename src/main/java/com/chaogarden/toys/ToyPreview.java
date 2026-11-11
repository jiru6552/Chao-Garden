package com.chaogarden.toys;

import com.chaogarden.ChaoGarden;
import com.chaogarden.activities.ActivityManager;
import com.chaogarden.companion.CompanionContent;
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
  private static final String RESTORE_AI="ChaoToyPreviewRestoreNoAi";
  private static final Map<ChaoEntity,Session> ACTIVE=new WeakHashMap<>();
  private static final class Session {
    final String kind;final boolean loop;final UUID keeper;long start,lastTick=Long.MIN_VALUE;
    Session(String kind,boolean loop,ServerPlayer keeper,long now){this.kind=kind;this.loop=loop;this.keeper=keeper.getUUID();start=now;}
  }
  private ToyPreview() {}
  public static boolean active(ChaoEntity c){return ACTIVE.containsKey(c);}
  public static String unavailable(ChaoEntity c){
    if(c.getType()!=ChaoGarden.CHAO.get()||KindergartenContent.isBoarded(c))return "School residents and enrolled students cannot be previewed. Retrieve your student first.";
    if(!c.isAlive()||c.isCocooned()||c.isResting()||c.isEating()||c.isHandlingItem()||c.isPassenger()||c.isVehicle()
        ||c.isBeingPetted()||c.hurtTime>0||c.isBehaving()||ActivityManager.isBusy(c)||CompanionContent.isManaged(c)
        ||c.getPersistentData().getBoolean("ChaoToyDismountWait")||c.state().intTrait("content.construction")>0)
      return "This Chao is busy. Let its current activity finish first.";
    if(!c.getHeldChaoItem().isEmpty()||!c.getMainHandItem().isEmpty())return "Let the Chao put down its item first.";
    if(c.officialComparison())return "Use /chao appearance garden first to enable gameplay animations.";
    if(c.isInWater()||!c.onGround()&&!(active(c)&&ToyContent.mounted(ACTIVE.get(c).kind)))return "Place the Chao on dry, solid ground first.";
    if(!active(c)&&(c.toyPlayingTicks()>0||c.getPersistentData().getLong("ChaoToyApproachUntil")>c.level().getGameTime()))return "This Chao is already approaching or using a placed toy.";
    return "";
  }
  public static String start(ChaoEntity c,ServerPlayer keeper,String id,boolean loop){
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
    ACTIVE.put(c,new Session(kind,loop,keeper,c.level().getGameTime()));c.setToyPlaying(kind,ToyContent.PLAY_TICKS);
    return "";
  }
  /** Invoked before keeper simulation. Vanilla physics and damage still tick; care/age and rewards do not. */
  public static boolean tick(ChaoEntity c){
    var s=ACTIVE.get(c);if(s==null)return false;
    var keeper=c.getServer().getPlayerList().getPlayer(s.keeper);
    if(keeper==null||keeper.level()!=c.level()||keeper.distanceToSqr(c)>64*64||!unavailable(c).isEmpty()
        ||!ToyContent.proneSpaceReady(c,s.kind,c.position(),c.getYRot(),null)){
      stop(c);return false;
    }
    long now=c.level().getGameTime();if(s.lastTick==now)return true;s.lastTick=now;
    long elapsed=now-s.start;
    if(elapsed>=ToyContent.PLAY_TICKS){
      if(!s.loop){stop(c);return true;}
      s.start=now;elapsed=0;
      if(ToyInteractionRules.keyboard(s.kind)){
        var at=c.toyTarget();if(at==null||!ToyContent.holdKeyboard(c,at)){stop(c);return true;}
        ToyContent.beginKeyboardPreview(c,at);
      }else if(ToyContent.mounted(s.kind)){
        var at=c.toyTarget();if(at==null||!ToyContent.holdMounted(c,at,160)||!ToyContent.beginMountedPreview(c,at)){stop(c);return true;}
      }else c.setToyPlaying(s.kind,ToyContent.PLAY_TICKS);
    }
    c.getNavigation().stop();c.setDeltaMovement(new Vec3(0,c.getDeltaMovement().y,0));
    if(ToyInteractionRules.keyboard(s.kind)){
      var at=c.toyTarget();
      if(at==null||!c.level().getBlockState(at).is(ToyContent.block(s.kind))||!ToyContent.holdKeyboard(c,at)){stop(c);return true;}
      if(s.kind.equals(OrganPlayRules.KIND))OrganPlayRules.cue(c,(int)elapsed);else PianoPlayRules.cue(c,(int)elapsed);
    }else if(ToyContent.mounted(s.kind)){
      var at=c.toyTarget();if(at==null||!ToyContent.holdMounted(c,at,elapsed)){stop(c);return true;}
      HandToyEffects.tick(c,s.kind,(int)elapsed);
    }else HandToyEffects.tick(c,s.kind,(int)elapsed);return true;
  }
  public static boolean stop(ChaoEntity c){
    boolean was=ACTIVE.remove(c)!=null;
    if(was||c.getPersistentData().contains(RESTORE_AI)){
      if(c.getPersistentData().contains(RESTORE_AI))c.setNoAi(c.getPersistentData().getBoolean(RESTORE_AI));
      if(c.toyTarget()!=null)ToyContent.endPlaying(c,false);
      c.getPersistentData().remove(RESTORE_AI);c.setToyPlaying("",0);c.getNavigation().stop();return true;
    }
    return false;
  }
  /** A saved preview never resumes or leaves its temporary NoAI flag behind. */
  public static void restoreAfterLoad(ChaoEntity c){stop(c);}
  /** Save ordinary state even if the world is subsequently opened with an older fallback JAR. */
  public static void prepareSave(ChaoEntity c,net.minecraft.nbt.CompoundTag tag){
    if(active(c)){
      tag.putBoolean("NoAI",c.getPersistentData().getBoolean(RESTORE_AI));
      tag.remove("ToyPlayingKind");tag.remove("ToyPlayingRemaining");
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
    e.getDispatcher().register(Commands.literal("chao").then(root));
    var poseRoot=Commands.literal("testpose").requires(s->s.hasPermission(2));
    var sitting=Commands.literal("sit").executes(c->run(c.getSource(),"sit","once"));
    for(String mode:java.util.List.of("once","loop","stop"))sitting.then(Commands.literal(mode).executes(c->run(c.getSource(),"sit",mode)));
    poseRoot.then(sitting).then(Commands.literal("stop").executes(c->run(c.getSource(),"","stop")));
    poseRoot.then(Commands.literal("list").executes(c->{c.getSource().sendSuccess(()->Component.literal("/chao testpose sit [once|loop|stop] — production sitting pose with standing transitions; no saved changes. /chao testpose stop ends playback."),false);return 1;}));
    e.getDispatcher().register(Commands.literal("chao").then(poseRoot));
  }
  private static int help(CommandSourceStack source){
    source.sendSuccess(()->Component.literal("/chao testtoy <toy> [once|loop|stop] — nearest Chao within 12 blocks. Toys: "+String.join(", ",HandToyEffects.clips().stream().map(HandToyEffects.Clip::id).toList())+", piano, organ (require the matching placed keyboard nearby; rocking_horse and pogo_stick require their placed toy). No lessons/items needed for hand toys; no rewards or saved trait changes. Stop: /chao testtoy stop. Carrying: /chao testcarry once|loop|stop with an equipped Chao. Loops also stop on interaction, damage, unload, logout or leaving 64 blocks/dimension."),false);return 1;
  }
  private static int run(CommandSourceStack source,String id,String mode) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
    var player=source.getPlayerOrException();
    var c=source.getLevel().getEntitiesOfClass(ChaoEntity.class,new AABB(source.getPosition(),source.getPosition()).inflate(12),
        chao->chao.getType()==ChaoGarden.CHAO.get()&&(!mode.equals("stop")||active(chao)))
        .stream().filter(chao->chao.distanceToSqr(source.getPosition())<=144)
        .min(java.util.Comparator.comparingDouble(chao->chao.distanceToSqr(source.getPosition()))).orElse(null);
    if(c==null){source.sendFailure(Component.literal(mode.equals("stop")?"No active Chao preview within 12 blocks.":"No Chao within 12 blocks."));return 0;}
    if(mode.equals("stop")){
      if(!id.isEmpty()&&!ACTIVE.get(c).kind.equals(id.equals("sit")?SITTING_KIND:"chao_"+id)){source.sendFailure(Component.literal("The nearest preview is using a different toy. Use /chao testtoy stop."));return 0;}
      stop(c);source.sendSuccess(()->Component.literal("Stopped preview for "+c.getName().getString()+"."),false);return 1;
    }
    String problem=id.equals("sit")?startSitting(c,player,mode.equals("loop")):start(c,player,id,mode.equals("loop"));
    if(!problem.isEmpty()){source.sendFailure(Component.literal(problem));return 0;}
    source.sendSuccess(()->Component.literal("Previewing "+id+" "+mode+" on "+c.getName().getString()+". /chao testtoy stop ends playback."),false);return 1;
  }
}
