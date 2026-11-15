package com.chaogarden.toys;

import com.chaogarden.ChaoGarden;
import com.chaogarden.ChaoSounds;
import com.chaogarden.activities.ActivityManager;
import com.chaogarden.companion.CompanionContent;
import com.chaogarden.core.ChaoPreferences;
import com.chaogarden.core.ChaoEmotion;
import com.chaogarden.care.ChaoCareServices;
import com.chaogarden.entity.ChaoEntity;
import com.chaogarden.market.MarketStock;
import java.util.Collections;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.HashSet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Display;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Server-owned voluntary play; a placed toy is never duplicated as a real or display item. */
@EventBusSubscriber(modid=ChaoGarden.MODID)
public final class ToyContent {
  public static final int PLAY_TICKS=ToyInteractionRules.SESSION_TICKS;
  private static final String TARGET="ChaoToyTarget", UNTIL="ChaoToyUntil", KIND="ChaoToyKind";
  private static final String NEXT="ChaoToyNextPlay", APPROACH="ChaoToyApproachUntil";
  private static final String MOUNT="ChaoToyMounted",RETURN="ChaoToyReturn",GRAVITY="ChaoToyGravity";
  private static final String DISMOUNT_WAIT="ChaoToyDismountWait", SAVED_AI="ChaoToyDismountNoAi";
  private static final String BALL_CLEARANCE="ChaoBallContactClearance074";
  public static final DeferredRegister.Blocks BLOCKS=DeferredRegister.createBlocks(ChaoGarden.MODID);
  public static final DeferredRegister<net.minecraft.world.level.block.entity.BlockEntityType<?>> BLOCK_ENTITIES=DeferredRegister.create(net.minecraft.core.registries.Registries.BLOCK_ENTITY_TYPE,ChaoGarden.MODID);
  private static final Map<String,DeferredBlock<ChaoToyBlock>> TOYS=new LinkedHashMap<>();
  private static final Set<ChaoEntity> GOALS=Collections.newSetFromMap(new WeakHashMap<>());
  private static final Map<ChaoEntity,BlockPos> CLIENT_BALL_CLEARANCE=Collections.synchronizedMap(new WeakHashMap<>());
  private static final Set<BlockPos> MOVING_BALLS=new HashSet<>();
  static {
    for(String id:java.util.stream.Stream.of(MarketStock.TOYS,MarketStock.LEGACY_TOYS,MarketStock.LESSON_TOYS,List.of("chao_shovel"))
        .flatMap(List::stream).toList()) {
      TOYS.put(id,BLOCKS.register(id,()->createToyBlock(id,BlockBehaviour.Properties.of()
          .mapColor(MapColor.WOOD).strength(.4f).sound(SoundType.WOOD).noOcclusion().randomTicks()
          .lightLevel(state->state.getValue(ChaoToyBlock.PLAYING)
              && (id.equals("chao_tv")||id.equals("chao_radio"))?4:0))));
    }
  }
  public static final net.neoforged.neoforge.registries.DeferredHolder<net.minecraft.world.level.block.entity.BlockEntityType<?>,net.minecraft.world.level.block.entity.BlockEntityType<BallToyBlockEntity074>> BALL_ENTITY=
      BLOCK_ENTITIES.register("chao_ball_motion",()->net.minecraft.world.level.block.entity.BlockEntityType.Builder.of(BallToyBlockEntity074::new,TOYS.get(BallPlayRules074.KIND).get()).build(null));
  public static final DeferredBlock<com.chaogarden.toys.RetiredCoffinBlock> RETIRED_COFFIN=BLOCKS.register("chao_coffin",com.chaogarden.toys.RetiredCoffinBlock::new);
  private static ChaoToyBlock createToyBlock(String id,BlockBehaviour.Properties properties){return id.equals(BallPlayRules074.KIND)?new ChaoBallBlock074(properties):new ChaoToyBlock(id,properties);}
  private ToyContent() {}
  public static void register(IEventBus bus){BLOCKS.register(bus);BLOCK_ENTITIES.register(bus);ToyCueNetwork.register(bus);FloatToyContent080.register(bus);}
  public static List<String> ids(){return List.copyOf(TOYS.keySet());}
  public static ChaoToyBlock block(String id){return TOYS.get(id).get();}
  public static boolean isPlaying(ChaoEntity chao){return chao.toyPlayingTicks()>0;}
  public static String playingKind(ChaoEntity chao){return chao.toyPlayingKind();}
  public static int playRemainingTicks(ChaoEntity chao){return chao.toyPlayingTicks();}
  public static Vec3 bubbleOrigin(ChaoEntity chao) {
    double scale=chao.state().adult?.80:.64;
    // The physical entity eye height is shared by ages, while this rig renders
    // babies at 0.64 and adults at 0.80. Use the rendered mouth/wand height.
    return new Vec3(chao.getX(),chao.getY()+.42*scale,chao.getZ())
        .add(Vec3.directionFromRotation(0,chao.getYRot()).scale(.32*scale));
  }
  public static BlockPos target(ChaoEntity chao){return BlockPos.of(chao.getPersistentData().getLong(TARGET));}
  /** The rendered body is smaller than its rectangular hitbox. Only its claimed ball yields. */
  public static boolean ownsBallContact(ChaoEntity c,BlockPos at){
    if(c.level().isClientSide){
      // Approach target is synchronized before contact; completed sessions clear
      // it. Keep only the previous ball's overlapping hitbox clearance afterward.
      if(at.equals(c.toyTarget())&&(c.toyPlayingKind().isEmpty()||BallPlayRules074.KIND.equals(c.toyPlayingKind()))){CLIENT_BALL_CLEARANCE.put(c,at.immutable());return true;}
      var previous=CLIENT_BALL_CLEARANCE.get(c);
      if(previous!=null&&!overlapsBall(c,previous)){CLIENT_BALL_CLEARANCE.remove(c);previous=null;}
      return at.equals(previous)&&overlapsBall(c,at);
    }
    var tag=c.getPersistentData();long now=c.level().getGameTime();
    if(tag.contains(TARGET)&&target(c).equals(at)&&(tag.getLong(APPROACH)>now||BallPlayRules074.KIND.equals(tag.getString(KIND))&&tag.getLong(UNTIL)>now))return true;
    return tag.contains(BALL_CLEARANCE)&&BlockPos.of(tag.getLong(BALL_CLEARANCE)).equals(at)&&overlapsBall(c,at);
  }
  private static boolean overlapsBall(ChaoEntity c,BlockPos at){
    return c.level().hasChunkAt(at)&&c.level().getBlockState(at).is(block(BallPlayRules074.KIND))
        &&c.getBoundingBox().intersects(new AABB(at.getX()+5.0/16,at.getY(),at.getZ()+5.0/16,at.getX()+11.0/16,at.getY()+6.0/16,at.getZ()+11.0/16));
  }
  static void retainBallClearance(ChaoEntity c,BlockPos at){c.getPersistentData().putLong(BALL_CLEARANCE,at.asLong());}

  @SubscribeEvent public static void joined(EntityJoinLevelEvent event) {
    if(event.getLevel().isClientSide)return;
    if(event.getEntity() instanceof Display.ItemDisplay display
        && display.getPersistentData().contains("ChaoToyPropExpires")) {
      // The old version's temporary, noncollectible copies are not physical toys.
      event.setCanceled(true);display.discard();return;
    }
    if(event.getEntity() instanceof ChaoEntity chao&&GOALS.add(chao)) {
      // A reload cancels an unfinished session; the block remains in the world,
      // no item is returned/created, and the saved rest cooldown is retained.
      ToyPreview.restoreAfterLoad(chao);
      endPlaying(chao,false);
      chao.goalSelector.addGoal(4,new PlayGoal(chao));
    }
  }
  public static boolean mayPlay(ChaoEntity chao) {
    return chao.isAlive()&&!chao.getPersistentData().getBoolean(DISMOUNT_WAIT)&&!chao.isNoAi()&&!chao.isHandlingItem()
        &&!chao.isResting()&&!chao.isEating()&&!chao.isCocooned()&&!chao.isPassenger()
        &&chao.getHeldChaoItem().isEmpty()&&chao.getMainHandItem().isEmpty()&&chao.getAffectionTicks()==0
        &&(!chao.isBehaving()||chao.getBehavior()==ChaoEntity.Behavior.IDLE)&&!chao.state().isIll()
        // A stationary pose can clear vanilla's onGround flag. Keep the server-owned
        // session eligible through its terminal tick; its environment/contact is checked below.
        &&((!chao.isInWater()&&chao.onGround())||chao.getPersistentData().getLong(UNTIL)>0
            ||chao.getPersistentData().getBoolean(MOUNT))&&!chao.isStaying()
        &&chao.state().energy>=25&&chao.state().hunger>=30
        &&!ActivityManager.isBusy(chao)&&!CompanionContent.isManaged(chao);
  }
  public static boolean isWaterToy(String kind){return kind.equals("chao_rubber_duck")||kind.equals("chao_lifesaver");}
  public static boolean mounted(String kind){return kind.equals("chao_rocking_horse")||kind.equals("chao_pogo_stick")||kind.equals("chao_lifesaver");}
  private static boolean knowsToy(ChaoEntity chao,String kind) {
    if(kind.equals("chao_shovel"))return chao.state().traitInt("reward.shovel",0)>0;
    String id=kind.substring("chao_".length());
    return !List.of("trumpet","drum","maracas","triangle","guitar","accordion","bell","castanets",
        "cymbals","flute","tambourine").contains(id)||ChaoCareServices.knowsLesson(chao,id);
  }
  static boolean available(ServerLevel level,BlockPos pos,ChaoEntity candidate) {
    return level.hasChunkAt(pos)&&level.getBlockState(pos).getBlock() instanceof ChaoToyBlock
        &&level.getEntitiesOfClass(ChaoEntity.class,new AABB(pos).inflate(10),other->
            other!=candidate&&other.isAlive()&&!other.isRemoved()
                &&other.getPersistentData().contains(TARGET)&&target(other).equals(pos)
                && (isPlaying(other)||other.getPersistentData().getBoolean(DISMOUNT_WAIT)||other.getNavigation().isInProgress()
                    ||other.getPersistentData().getLong(APPROACH)>level.getGameTime())).isEmpty();
  }
  static BlockPos standAt(ServerLevel level,BlockPos pos,ChaoEntity chao) {
    BlockPos best=null;double distance=Double.MAX_VALUE;
    var state=level.getBlockState(pos);
    String kind=((ChaoToyBlock)state.getBlock()).toy;
    // Screens and keyboard keys have an authored front.
    // Round/percussive toys can be approached from any clear side.
    var front=state.getValue(ChaoToyBlock.FACING);
    Iterable<Direction> sides=kind.equals("chao_toy_car")?List.of(front.getOpposite()):kind.equals("chao_rocking_horse")
        ?List.of(front.getClockWise(),front.getCounterClockWise(),front)
        :ToyInteractionRules.frontOnly(kind)||kind.equals(RadioPlayRules075.KIND)?List.of(front):Direction.Plane.HORIZONTAL;
    for(Direction direction:sides) {
      var at=pos.relative(direction);
      if(isWaterToy(kind)&&state.getValue(ChaoToyBlock.WATERLOGGED))at=at.above();
      if(!level.hasChunkAt(at)||!level.getBlockState(at.below()).isFaceSturdy(level,at.below(),Direction.UP)
          ||!level.getBlockState(at).getCollisionShape(level,at).isEmpty()
          ||!level.getBlockState(at).getFluidState().isEmpty())continue;
      if(ToyInteractionRules.stationary(kind)&&!supportedStationaryAt(chao,stationaryAnchor(chao,pos,at)))continue;
      if((kind.equals("chao_toy_car")||kind.equals("chao_crayons"))
          &&!proneSpaceReady(chao,kind,stationaryAnchor(chao,pos,at),direction.getOpposite().toYRot(),pos))continue;
      if(mounted(kind)&&!mountPathClear(chao,pos,at))continue;
      double d=chao.distanceToSqr(Vec3.atBottomCenterOf(at));
      if(d<distance){best=at;distance=d;}
    }
    return best;
  }
  public static boolean hasPlayerAt(ServerLevel level,BlockPos pos) {
    long now=level.getGameTime();
    return !level.getEntitiesOfClass(ChaoEntity.class,new AABB(pos).inflate(3),c->
        c.isAlive()&&!c.isRemoved()&&isPlaying(c)&&c.getPersistentData().getLong(UNTIL)>now
            &&c.getPersistentData().contains(TARGET)&&target(c).equals(pos)).isEmpty();
  }
  public static int sessionElapsed(ServerLevel level,BlockPos pos) {
    return level.getEntitiesOfClass(ChaoEntity.class,new AABB(pos).inflate(3),c->
        isPlaying(c)&&c.getPersistentData().contains(TARGET)&&target(c).equals(pos))
        .stream().mapToInt(c->(int)(PLAY_TICKS-(c.getPersistentData().getLong(UNTIL)-level.getGameTime())))
        .findFirst().orElse(PLAY_TICKS);
  }
  /** Physical keyboard edge, rather than the center of a whole neighboring cell. */
  public static Vec3 keyboardAnchor(ChaoEntity chao,BlockPos pos) {
    var state=chao.level().getBlockState(pos);
    var facing=state.getValue(ChaoToyBlock.FACING);
    double distance=ToyInteractionRules.keyboardDistance(((ChaoToyBlock)state.getBlock()).toy,chao.getBbWidth());
    return Vec3.atBottomCenterOf(pos).add(facing.getStepX()*distance,0,facing.getStepZ()*distance);
  }
  /** Both natural use and preview hold the same collision-checked front contact. */
  public static boolean holdKeyboard(ChaoEntity chao,BlockPos pos){
    var state=chao.level().getBlockState(pos);
    if(!(state.getBlock() instanceof ChaoToyBlock block)||!ToyInteractionRules.keyboard(block.toy))return false;
    if(chao.level() instanceof ServerLevel server&&standAt(server,pos,chao)==null)return false;
    Vec3 anchor=keyboardAnchor(chao,pos);
    if(chao.distanceToSqr(anchor)>.16||!chao.level().noCollision(chao,chao.getBoundingBox().move(anchor.subtract(chao.position()))))return false;
    chao.getNavigation().stop();chao.setDeltaMovement(Vec3.ZERO);chao.setPos(anchor.x,anchor.y,anchor.z);
    face(chao,state.getValue(ChaoToyBlock.FACING).getOpposite());
    if(ToyInteractionRules.keyboard(block.toy))chao.getLookControl().setLookAt(pos.getX()+.5,chao.getEyeY()-.06,pos.getZ()+.5,180,30);
    return true;
  }
  /** A real placed keyboard is reserved, never copied or rewarded. No care/cooldown changes. */
  public static BlockPos previewPiano(ChaoEntity c){return previewKeyboard(c,PianoPlayRules.KIND);}
  public static BlockPos previewKeyboard(ChaoEntity c,String kind){
    if(!ToyInteractionRules.keyboard(kind))return null;
    if(!(c.level() instanceof ServerLevel level))return null;
    BlockPos best=null;double distance=9;
    for(var at:BlockPos.betweenClosed(c.blockPosition().offset(-3,-1,-3),c.blockPosition().offset(3,1,3))){
      if(!level.getBlockState(at).is(block(kind))||!available(level,at,c))continue;
      if(standAt(level,at,c)==null)continue;
      Vec3 anchor=keyboardAnchor(c,at);double d=c.distanceToSqr(anchor);
      if(d<distance&&level.noCollision(c,c.getBoundingBox().move(anchor.subtract(c.position())))
          &&!level.getBlockState(BlockPos.containing(anchor).below()).getCollisionShape(level,BlockPos.containing(anchor).below()).isEmpty()){
        best=at.immutable();distance=d;
      }
    }
    return best;
  }
  public static void beginPianoPreview(ChaoEntity c,BlockPos at){beginKeyboardPreview(c,at);}
  public static void beginKeyboardPreview(ChaoEntity c,BlockPos at){
    var tag=c.getPersistentData();boolean had=tag.contains(NEXT);long next=tag.getLong(NEXT);
    var anchor=keyboardAnchor(c,at);c.setPos(anchor.x,anchor.y,anchor.z);
    beginPlaying(c,at,BlockPos.containing(anchor),(ChaoToyBlock)c.level().getBlockState(at).getBlock());
    if(had)tag.putLong(NEXT,next);else tag.remove(NEXT);holdKeyboard(c,at);
  }
  /** Keep a viewer at the real screen-side seat. No remote relocation through the cabinet. */
  public static Vec3 tvAnchor(ChaoEntity c,BlockPos at) {
    var state=c.level().getBlockState(at);
    return stationaryAnchor(c,at,at.relative(state.getValue(ChaoToyBlock.FACING)));
  }
  public static Vec3 tvScreen(ChaoEntity c,BlockPos at) {
    var facing=c.level().getBlockState(at).getValue(ChaoToyBlock.FACING);var right=facing.getClockWise();
    return Vec3.atBottomCenterOf(at).add(facing.getStepX()*TvWatchRules073.SCREEN_FORWARD+right.getStepX()*TvWatchRules073.SCREEN_RIGHT,
        TvWatchRules073.SCREEN_HEIGHT,facing.getStepZ()*TvWatchRules073.SCREEN_FORWARD+right.getStepZ()*TvWatchRules073.SCREEN_RIGHT);
  }
  public static boolean holdTv(ChaoEntity c,BlockPos at) {
    var state=c.level().getBlockState(at);
    if(!state.is(block(TvWatchRules073.KIND)))return false;
    var anchor=tvAnchor(c,at);
    if(!ToyInteractionRules.stationaryWithinReach(TvWatchRules073.KIND,c.distanceToSqr(anchor))
        ||!supportedStationaryAt(c,anchor))return false;
    c.getNavigation().stop();c.setDeltaMovement(Vec3.ZERO);c.setPos(anchor.x,anchor.y,anchor.z);
    face(c,state.getValue(ChaoToyBlock.FACING).getOpposite());
    var screen=tvScreen(c,at);c.getLookControl().setLookAt(screen.x,screen.y,screen.z,180,20);
    // Preview deliberately freezes AI, so it cannot rely on LookControl.tick.
    // Use the same bounded screen aim for natural watching and preview.
    double dx=screen.x-c.getX(),dy=screen.y-c.getEyeY(),dz=screen.z-c.getZ();
    float yaw=(float)(Math.toDegrees(Math.atan2(dz,dx))-90);
    c.setYHeadRot(c.getYRot()+net.minecraft.util.Mth.clamp(net.minecraft.util.Mth.wrapDegrees(yaw-c.getYRot()),-20,20));
    c.setXRot(net.minecraft.util.Mth.clamp((float)-Math.toDegrees(Math.atan2(dy,Math.hypot(dx,dz))),-30,30));
    return true;
  }
  /** Preview requires an existing clear TV seat close enough for the ordinary short contact handoff. */
  public static BlockPos previewTv(ChaoEntity c) {
    if(!(c.level() instanceof ServerLevel level))return null;
    BlockPos best=null;double closest=.16;
    for(var at:BlockPos.betweenClosed(c.blockPosition().offset(-3,-1,-3),c.blockPosition().offset(3,1,3))){
      var state=level.getBlockState(at);
      if(!state.is(block(TvWatchRules073.KIND))||!available(level,at,c)||standAt(level,at,c)==null)continue;
      var anchor=tvAnchor(c,at);double distance=c.distanceToSqr(anchor);
      if(distance<=closest&&supportedStationaryAt(c,anchor)){best=at.immutable();closest=distance;}
    }
    return best;
  }
  /** Reserve the one real block and reuse production presentation without changing its cooldown. */
  public static boolean beginTvPreview(ChaoEntity c,BlockPos at) {
    if(!(c.level() instanceof ServerLevel level)||!level.getBlockState(at).is(block(TvWatchRules073.KIND))||!available(level,at,c))return false;
    var anchor=tvAnchor(c,at);var delta=anchor.subtract(c.position());
    if(delta.lengthSqr()>.16||!supportedStationaryAt(c,anchor)||!level.noCollision(c,c.getBoundingBox().expandTowards(delta)))return false;
    c.setPos(anchor.x,anchor.y,anchor.z);if(!holdTv(c,at))return false;
    var tag=c.getPersistentData();boolean had=tag.contains(NEXT);long next=tag.getLong(NEXT);
    var state=level.getBlockState(at);
    beginPlaying(c,at,at.relative(state.getValue(ChaoToyBlock.FACING)),(ChaoToyBlock)state.getBlock());
    if(had)tag.putLong(NEXT,next);else tag.remove(NEXT);
    return true;
  }
  public static Vec3 jackAnchor(ChaoEntity c,BlockPos at) {
    var facing=c.level().getBlockState(at).getValue(ChaoToyBlock.FACING);
    return stationaryAnchor(c,at,at.relative(facing));
  }
  /** The same close front watching position and bounded gaze serve natural play and previews. */
  public static boolean holdJack(ChaoEntity c,BlockPos at) {
    var state=c.level().getBlockState(at);if(!state.is(block(JackPlayRules080.KIND)))return false;
    var anchor=jackAnchor(c,at);
    if(!ToyInteractionRules.stationaryWithinReach(JackPlayRules080.KIND,c.distanceToSqr(anchor))
        ||!supportedStationaryAt(c,anchor))return false;
    c.getNavigation().stop();c.setDeltaMovement(Vec3.ZERO);c.setPos(anchor.x,anchor.y,anchor.z);
    face(c,state.getValue(ChaoToyBlock.FACING).getOpposite());
    double x=at.getX()+.5,z=at.getZ()+.5;
    double y=at.getY()+JackPlayRules080.lookHeight(PLAY_TICKS-c.toyPlayingTicks());
    double dy=y-c.getEyeY(),distance=Math.hypot(x-c.getX(),z-c.getZ());
    float pitch=JackPlayRules080.gazePitch((float)-Math.toDegrees(Math.atan2(dy,distance)));
    // LookControl ticks after the goal. Bound its target as well as the pose,
    // otherwise it replaces the bounded pitch with the full box-floor angle.
    double aimedY=c.getEyeY()-Math.tan(Math.toRadians(pitch))*distance;
    c.getLookControl().setLookAt(x,aimedY,z,30,20);
    c.setYHeadRot(c.getYRot());c.setXRot(pitch);
    return true;
  }
  public static BlockPos previewJack(ChaoEntity c) {
    if(!(c.level() instanceof ServerLevel level))return null;
    BlockPos best=null;double distance=.16;
    for(var at:BlockPos.betweenClosed(c.blockPosition().offset(-3,-1,-3),c.blockPosition().offset(3,1,3))){
      var state=level.getBlockState(at);
      if(!state.is(block(JackPlayRules080.KIND))||!available(level,at,c)||standAt(level,at,c)==null)continue;
      var anchor=jackAnchor(c,at);double next=c.distanceToSqr(anchor);
      if(next<=distance&&supportedStationaryAt(c,anchor)){best=at.immutable();distance=next;}
    }
    return best;
  }
  public static boolean beginJackPreview(ChaoEntity c,BlockPos at) {
    if(!(c.level() instanceof ServerLevel level)||!level.getBlockState(at).is(block(JackPlayRules080.KIND))||!available(level,at,c))return false;
    var anchor=jackAnchor(c,at);var delta=anchor.subtract(c.position());
    if(delta.lengthSqr()>.16||!supportedStationaryAt(c,anchor)||!level.noCollision(c,c.getBoundingBox().expandTowards(delta)))return false;
    c.setPos(anchor.x,anchor.y,anchor.z);if(!holdJack(c,at))return false;
    var tag=c.getPersistentData();boolean had=tag.contains(NEXT);long next=tag.getLong(NEXT);var state=level.getBlockState(at);
    beginPlaying(c,at,at.relative(state.getValue(ChaoToyBlock.FACING)),(ChaoToyBlock)state.getBlock());
    if(had)tag.putLong(NEXT,next);else tag.remove(NEXT);return true;
  }
  /** The front listening seat stays outside the real low radio cabinet. */
  public static Vec3 radioAnchor(ChaoEntity c,BlockPos at) {
    var facing=c.level().getBlockState(at).getValue(ChaoToyBlock.FACING);
    return Vec3.atBottomCenterOf(at).add(facing.getStepX()*RadioPlayRules075.LISTEN_DISTANCE,0,facing.getStepZ()*RadioPlayRules075.LISTEN_DISTANCE);
  }
  public static Vec3 radioSpeaker(ChaoEntity c,BlockPos at){return radioSpeaker(at,c.level().getBlockState(at));}
  public static Vec3 radioSpeaker(BlockPos at,net.minecraft.world.level.block.state.BlockState state) {
    var facing=state.getValue(ChaoToyBlock.FACING);
    return Vec3.atBottomCenterOf(at).add(facing.getStepX()*RadioPlayRules075.SPEAKER_FORWARD,RadioPlayRules075.SPEAKER_HEIGHT,facing.getStepZ()*RadioPlayRules075.SPEAKER_FORWARD);
  }
  /** Natural sessions and previews share the same supported seat, heading and measured speaker aim. */
  public static boolean holdRadio(ChaoEntity c,BlockPos at) {
    var state=c.level().getBlockState(at);if(!state.is(block(RadioPlayRules075.KIND)))return false;
    var anchor=radioAnchor(c,at);
    if(!ToyInteractionRules.stationaryWithinReach(RadioPlayRules075.KIND,c.distanceToSqr(anchor))||!supportedStationaryAt(c,anchor))return false;
    c.getNavigation().stop();c.setDeltaMovement(Vec3.ZERO);c.setPos(anchor.x,anchor.y,anchor.z);
    c.getMoveControl().setWantedPosition(anchor.x,anchor.y,anchor.z,0);
    face(c,state.getValue(ChaoToyBlock.FACING).getOpposite());
    var speaker=radioSpeaker(at,state);c.getLookControl().setLookAt(speaker.x,speaker.y,speaker.z,180,24);
    double dx=speaker.x-c.getX(),dy=speaker.y-c.getEyeY(),dz=speaker.z-c.getZ();
    float yaw=(float)(Math.toDegrees(Math.atan2(dz,dx))-90);
    c.setYHeadRot(c.getYRot()+net.minecraft.util.Mth.clamp(net.minecraft.util.Mth.wrapDegrees(yaw-c.getYRot()),-20,20));
    c.setXRot(net.minecraft.util.Mth.clamp((float)-Math.toDegrees(Math.atan2(dy,Math.hypot(dx,dz))),-24,24));
    return true;
  }
  /** Operator playback requires the same nearby front seat, not a relocation through the radio. */
  public static BlockPos previewRadio(ChaoEntity c) {
    if(!(c.level() instanceof ServerLevel level))return null;
    BlockPos best=null;double closest=.16;
    for(var at:BlockPos.betweenClosed(c.blockPosition().offset(-3,-1,-3),c.blockPosition().offset(3,1,3))){
      var state=level.getBlockState(at);
      if(!state.is(block(RadioPlayRules075.KIND))||!available(level,at,c)||standAt(level,at,c)==null)continue;
      var anchor=radioAnchor(c,at);double distance=c.distanceToSqr(anchor);
      if(distance<=closest&&supportedStationaryAt(c,anchor)&&level.noCollision(c,c.getBoundingBox().expandTowards(anchor.subtract(c.position())))){
        best=at.immutable();closest=distance;
      }
    }
    return best;
  }
  public static boolean beginRadioPreview(ChaoEntity c,BlockPos at) {
    if(!(c.level() instanceof ServerLevel level)||!level.getBlockState(at).is(block(RadioPlayRules075.KIND))||!available(level,at,c))return false;
    var anchor=radioAnchor(c,at);var delta=anchor.subtract(c.position());
    if(delta.lengthSqr()>.16||!supportedStationaryAt(c,anchor)||!level.noCollision(c,c.getBoundingBox().expandTowards(delta)))return false;
    c.setPos(anchor.x,anchor.y,anchor.z);if(!holdRadio(c,at))return false;
    var tag=c.getPersistentData();boolean had=tag.contains(NEXT);long next=tag.getLong(NEXT);
    var state=level.getBlockState(at);
    beginPlaying(c,at,at.relative(state.getValue(ChaoToyBlock.FACING)),(ChaoToyBlock)state.getBlock());
    if(had)tag.putLong(NEXT,next);else tag.remove(NEXT);
    return true;
  }
  public static Vec3 stationaryAnchor(ChaoEntity chao,BlockPos pos,BlockPos stand) {
    var state=chao.level().getBlockState(pos);String kind=((ChaoToyBlock)state.getBlock()).toy;
    if(kind.equals(RadioPlayRules075.KIND))return radioAnchor(chao,pos);
    Direction side=Direction.getNearest(stand.getX()-pos.getX(),0,stand.getZ()-pos.getZ());
    var shape=state.getCollisionShape(chao.level(),pos);
    double extent=shape.isEmpty()?0:switch(side) {
      case NORTH->.5-shape.min(Direction.Axis.Z);case SOUTH->shape.max(Direction.Axis.Z)-.5;
      case WEST->.5-shape.min(Direction.Axis.X);case EAST->shape.max(Direction.Axis.X)-.5;
      default->0;
    };
    double distance=ToyInteractionRules.stationaryDistance(kind,extent,chao.getBbWidth());
    return new Vec3(pos.getX()+.5+side.getStepX()*distance,stand.getY(),pos.getZ()+.5+side.getStepZ()*distance);
  }
  /** Full actor clearance and actual support, including partial contact positions near cell edges. */
  private static boolean supportedStationaryAt(ChaoEntity chao,Vec3 at) {
    var level=chao.level();var body=chao.getBoundingBox().move(at.subtract(chao.position()));
    return level.hasChunkAt(BlockPos.containing(body.minX,body.minY,body.minZ))
        &&level.hasChunkAt(BlockPos.containing(body.maxX,body.maxY,body.maxZ))
        &&!level.containsAnyLiquid(body)&&level.noCollision(chao,body)
        &&level.getBlockCollisions(chao,body.move(0,-.02,0)).iterator().hasNext()
        &&level.getEntitiesOfClass(net.minecraft.world.entity.LivingEntity.class,body,
            other->other!=chao&&other.isAlive()&&!other.isSpectator()).isEmpty();
  }
  /** Room for the visible belly, head, feet and floor props, without changing entity hitboxes. */
  public static boolean proneSpaceReady(ChaoEntity c,String kind,Vec3 at,float yaw,BlockPos ownSource) {
    if(!kind.equals("chao_toy_car")&&!kind.equals("chao_crayons"))return true;
    var level=c.level();double scale=c.state().immortal?.86:c.state().adult?.80:.64;
    // The source-rig union reaches X[-.519,.334], Z[-.616,.604] model units.
    // Include palms, the small car/paper/box, and a margin for head additions.
    double half=.72*scale,forward=1.10*scale,back=.75*scale;
    double angle=Math.toRadians(yaw),fx=-Math.sin(angle),fz=Math.cos(angle),rx=fz,rz=-fx;
    double minX=Double.POSITIVE_INFINITY,minZ=minX,maxX=Double.NEGATIVE_INFINITY,maxZ=maxX;
    for(double x:new double[]{-half,half})for(double z:new double[]{-back,forward}) {
      double px=at.x+rx*x+fx*z,pz=at.z+rz*x+fz*z;
      minX=Math.min(minX,px);maxX=Math.max(maxX,px);minZ=Math.min(minZ,pz);maxZ=Math.max(maxZ,pz);
    }
    var body=new AABB(minX,at.y+.012,minZ,maxX,at.y+Math.max(.45,c.getBbHeight()),maxZ);
    if(level.containsAnyLiquid(body))return false;
    var occupied=net.minecraft.world.phys.shapes.Shapes.create(body);
    for(var cell:BlockPos.betweenClosed(BlockPos.containing(body.minX,body.minY,body.minZ),
        BlockPos.containing(body.maxX,body.maxY,body.maxZ))) {
      if(!level.hasChunkAt(cell))return false;
      var state=level.getBlockState(cell);
      if(ownSource!=null&&cell.equals(ownSource)&&state.getBlock() instanceof ChaoToyBlock toy&&toy.toy.equals(kind))continue;
      var shape=state.getCollisionShape(level,cell,net.minecraft.world.phys.shapes.CollisionContext.of(c));
      if(!shape.isEmpty()&&net.minecraft.world.phys.shapes.Shapes.joinIsNotEmpty(occupied,
          shape.move(cell.getX(),cell.getY(),cell.getZ()),net.minecraft.world.phys.shapes.BooleanOp.AND))return false;
    }
    if(!level.getEntitiesOfClass(net.minecraft.world.entity.LivingEntity.class,body,
        other->other!=c&&other.isAlive()&&!other.isSpectator()).isEmpty())return false;
    for(double x:new double[]{-half,0,half})for(double z:new double[]{-back,0,forward}) {
      double px=at.x+rx*x+fx*z,pz=at.z+rz*x+fz*z;
      var support=new AABB(px-.015,at.y-.035,pz-.015,px+.015,at.y-.001,pz+.015);
      if(!level.getBlockCollisions(c,support).iterator().hasNext())return false;
    }
    return true;
  }
  public static Vec3 ballAnchor(ChaoEntity chao,BlockPos pos,BlockPos stand) {
    Direction side=Direction.getNearest(stand.getX()-pos.getX(),0,stand.getZ()-pos.getZ());
    double reach=BallPlayRules074.contactDistance(chao.state(),chao.getChaoHat());
    return Vec3.atBottomCenterOf(pos).add(side.getStepX()*reach,0,side.getStepZ()*reach);
  }
  /** A real placed ball and a reachable clear side, never a minted demonstration prop. */
  public static BlockPos previewBall(ChaoEntity c){
    if(!(c.level() instanceof ServerLevel level))return null;
    BlockPos best=null;double distance=9;
    for(var at:BlockPos.betweenClosed(c.blockPosition().offset(-3,-1,-3),c.blockPosition().offset(3,1,3))){
      if(!level.hasChunkAt(at)||!level.getBlockState(at).is(block(BallPlayRules074.KIND))||!available(level,at,c))continue;
      var stand=standAt(level,at,c);if(stand==null)continue;
      var path=c.getNavigation().createPath(stand,0);double d=c.distanceToSqr(Vec3.atBottomCenterOf(at));
      if(d<=distance&&path!=null&&path.canReach()){distance=d;best=at.immutable();}
    }
    return best;
  }
  public static BallController074 beginBallPreview(ChaoEntity c,BlockPos at){
    if(!(c.level() instanceof ServerLevel level)||!level.hasChunkAt(at)||!level.getBlockState(at).is(block(BallPlayRules074.KIND))||!available(level,at,c))return null;
    var stand=standAt(level,at,c);if(stand==null)return null;
    var tag=c.getPersistentData();boolean had=tag.contains(NEXT);long next=tag.getLong(NEXT);
    beginPlaying(c,at,stand,(ChaoToyBlock)level.getBlockState(at).getBlock());
    if(had)tag.putLong(NEXT,next);else tag.remove(NEXT);
    // Old chunks may have the original ball state but no new cosmetic block entity yet.
    if(!(level.getBlockEntity(at) instanceof BallToyBlockEntity074))level.setBlockEntity(new BallToyBlockEntity074(at,level.getBlockState(at)));
    return new BallController074(c,at,stand);
  }
  public static boolean renewBallPreview(ChaoEntity c){
    if(!(c.level() instanceof ServerLevel level)||c.toyTarget()==null||!level.hasChunkAt(c.toyTarget())
        ||!level.getBlockState(c.toyTarget()).is(block(BallPlayRules074.KIND))||!available(level,c.toyTarget(),c))return false;
    c.getPersistentData().putLong(UNTIL,level.getGameTime()+PLAY_TICKS);c.renewBallToyPlaying(PLAY_TICKS);return true;
  }
  static boolean ballRollClear(ChaoEntity c,BlockPos at,Direction direction){
    if(!(c.level() instanceof ServerLevel level)||direction.getAxis().isVertical())return false;
    var next=at.relative(direction);
    if(!level.hasChunkAt(next)||!level.getBlockState(next).isAir()
        ||!level.getBlockState(at.below()).isFaceSturdy(level,at.below(),Direction.UP)
        ||!level.getBlockState(next.below()).isFaceSturdy(level,next.below(),Direction.UP))return false;
    double r=BallPlayRules074.SWEEP_RADIUS;
    var swept=new AABB(at.getX()+.5-r,at.getY()+.00001,at.getZ()+.5-r,at.getX()+.5+r,at.getY()+r*2,at.getZ()+.5+r)
        .expandTowards(direction.getStepX(),0,direction.getStepZ());
    var shape=net.minecraft.world.phys.shapes.Shapes.create(swept);
    for(var cell:BlockPos.betweenClosed(BlockPos.containing(swept.minX,swept.minY,swept.minZ),BlockPos.containing(swept.maxX,swept.maxY,swept.maxZ))){
      if(cell.equals(at))continue;
      if(!level.hasChunkAt(cell))return false;
      var solid=level.getBlockState(cell).getCollisionShape(level,cell);
      if(!solid.isEmpty()&&net.minecraft.world.phys.shapes.Shapes.joinIsNotEmpty(shape,solid.move(cell.getX(),cell.getY(),cell.getZ()),net.minecraft.world.phys.shapes.BooleanOp.AND))return false;
    }
    return level.getEntitiesOfClass(net.minecraft.world.entity.LivingEntity.class,swept,other->other!=c&&other.isAlive()&&!other.isSpectator()).isEmpty();
  }
  /** Commit one cell exactly once; interpolation is a cosmetic view of this same block. */
  static BlockPos rollBall074(ChaoEntity c,BlockPos at,Direction direction){
    if(!(c.level() instanceof ServerLevel level)||!level.getBlockState(at).is(block(BallPlayRules074.KIND))||!ballRollClear(c,at,direction))return null;
    var state=level.getBlockState(at).setValue(ChaoBallBlock074.ROLLING,false);var next=at.relative(direction);
    if(!level.setBlock(next,state,3))return null;
    MOVING_BALLS.add(at);
    try{
      if(!level.setBlock(at,Blocks.AIR.defaultBlockState(),3)){level.setBlock(next,Blocks.AIR.defaultBlockState(),3);return null;}
      c.getPersistentData().putLong(TARGET,next.asLong());c.setToyTarget(next);
      if(level.getBlockEntity(next) instanceof BallToyBlockEntity074 ball)ball.rollFrom(at,level.getGameTime(),direction);
      level.scheduleTick(next,state.getBlock(),1);
      level.playSound(null,at,SoundEvents.SLIME_BLOCK_STEP,SoundSource.BLOCKS,.08f,1f);
      return next;
    }finally{MOVING_BALLS.remove(at);}
  }
  public static Vec3 mountedAnchor(BlockPos pos,net.minecraft.world.level.block.state.BlockState state,int frame) {
    String kind=((ChaoToyBlock)state.getBlock()).toy;
    if(kind.equals("chao_lifesaver"))return Vec3.atBottomCenterOf(pos).add(0,.78+ToyInteractionRules.ringBob(frame),0);
    Direction facing=state.getValue(ChaoToyBlock.FACING);double z=ToyInteractionRules.horseRiderZ(frame);
    return Vec3.atBottomCenterOf(pos).add(-facing.getStepX()*z,ToyInteractionRules.horseRiderY(frame),-facing.getStepZ()*z);
  }
  /** Continuous production seat/foot clock shared with mounted operator previews. */
  public static Vec3 mountedAnchor(ChaoEntity c,BlockPos pos,net.minecraft.world.level.block.state.BlockState state,double ticks){
    String kind=((ChaoToyBlock)state.getBlock()).toy;float scale=c.state().immortal?.86f:c.state().adult?.80f:.64f;
    if(kind.equals("chao_pogo_stick"))return Vec3.atBottomCenterOf(pos).add(0,ActiveToyPlayRules049.pogoHeight((float)ticks,scale),0);
    if(!kind.equals("chao_rocking_horse"))return mountedAnchor(pos,state,ToyInteractionRules.mountedFrame((int)ticks));
    var front=state.getValue(ChaoToyBlock.FACING);double z=RockingHorsePlayRules.riderZ(ticks,scale);
    return Vec3.atBottomCenterOf(pos).add(-front.getStepX()*z,RockingHorsePlayRules.riderY(ticks,scale),-front.getStepZ()*z);
  }
  private static boolean mountPathClear(ChaoEntity c,BlockPos at,BlockPos stand){
    var state=c.level().getBlockState(at);
    for(int t=0;t<=160;t+=4){
      double occupied=state.is(block("chao_rocking_horse"))?RockingHorsePlayRules.occupancy(t):ToyInteractionRules.mountOccupancy(t);
      var point=Vec3.atBottomCenterOf(stand).lerp(mountedAnchor(c,at,state,t),occupied);
      String kind=((ChaoToyBlock)state.getBlock()).toy;
      if(kind.equals("chao_lifesaver"))point=point.add(0,ToyInteractionRules.ringEntryLift(occupied),0);
      if(kind.equals("chao_rocking_horse"))point=point.add(0,.15*Math.sin(Math.PI*occupied),0);
      if(!mountedClear(c,at,point))return false;
    }
    return true;
  }
  public static BlockPos previewMounted(ChaoEntity c,String kind){
    if(!mounted(kind)||kind.equals("chao_lifesaver")||!(c.level() instanceof ServerLevel level))return null;
    BlockPos best=null;double nearest=9;
    for(var p:BlockPos.betweenClosed(c.blockPosition().offset(-3,-1,-3),c.blockPosition().offset(3,1,3))){
      if(!level.getBlockState(p).is(block(kind))||!available(level,p,c))continue;
      var stand=standAt(level,p,c);if(stand==null||!mountPathClear(c,p,stand))continue;
      double distance=c.distanceToSqr(Vec3.atCenterOf(p));if(distance<nearest){nearest=distance;best=p.immutable();}
    }
    return best;
  }
  public static boolean beginMountedPreview(ChaoEntity c,BlockPos at){
    if(!(c.level() instanceof ServerLevel level)||!(level.getBlockState(at).getBlock() instanceof ChaoToyBlock b)||!mounted(b.toy))return false;
    var stand=standAt(level,at,c);if(stand==null||!mountPathClear(c,at,stand))return false;
    var tag=c.getPersistentData();boolean had=tag.contains(NEXT);long next=tag.getLong(NEXT);
    var origin=Vec3.atBottomCenterOf(stand);c.setPos(origin.x,origin.y,origin.z);
    beginPlaying(c,at,stand,b);if(had)tag.putLong(NEXT,next);else tag.remove(NEXT);
    return holdMounted(c,at,0);
  }
  public static boolean holdMounted(ChaoEntity c,BlockPos at,double elapsed){
    var state=c.level().getBlockState(at);
    if(!(state.getBlock() instanceof ChaoToyBlock b)||!mounted(b.toy)||!b.toy.equals(c.getPersistentData().getString(KIND))
        ||c.distanceToSqr(Vec3.atCenterOf(at))>16)return false;
    var tag=c.getPersistentData();if(!tag.contains(RETURN))return false;
    BlockPos stand=BlockPos.of(tag.getLong(RETURN));
    if(elapsed>=148&&!safeStand(c,stand))return false;
    double occupied=b.toy.equals("chao_rocking_horse")?RockingHorsePlayRules.occupancy((float)elapsed):ToyInteractionRules.mountOccupancy(elapsed);
    Vec3 point=Vec3.atBottomCenterOf(stand).lerp(mountedAnchor(c,at,state,elapsed),occupied);
    if(b.toy.equals("chao_lifesaver"))point=point.add(0,ToyInteractionRules.ringEntryLift(occupied),0);
    if(b.toy.equals("chao_rocking_horse"))point=point.add(0,.15*Math.sin(Math.PI*occupied),0);
    if(c.distanceToSqr(point)>.25||!mountedClear(c,at,point))return false;
    c.getNavigation().stop();c.setDeltaMovement(Vec3.ZERO);c.fallDistance=0;c.setPos(point.x,point.y,point.z);
    var facing=state.getValue(ChaoToyBlock.FACING);if(b.toy.equals("chao_rocking_horse"))facing=facing.getOpposite();
    face(c,facing);c.getLookControl().setLookAt(c.getX()+facing.getStepX(),c.getEyeY(),c.getZ()+facing.getStepZ(),180,180);
    return true;
  }
  /** Ignore only the owned prop's collision, never nearby blocks, roofs or other living actors. */
  private static boolean mountedClear(ChaoEntity chao,BlockPos toy,Vec3 at) {
    var level=chao.level();var body=chao.getBoundingBox().move(at.subtract(chao.position()));
    var actorShape=net.minecraft.world.phys.shapes.Shapes.create(body);
    for(BlockPos cell:BlockPos.betweenClosed(BlockPos.containing(body.minX-1,body.minY-1,body.minZ-1),
        BlockPos.containing(body.maxX+1,body.maxY+1,body.maxZ+1))) {
      if(cell.equals(toy))continue;
      if(!level.hasChunkAt(cell))return false;
      var shape=level.getBlockState(cell).getCollisionShape(level,cell,net.minecraft.world.phys.shapes.CollisionContext.of(chao));
      if(!shape.isEmpty()&&net.minecraft.world.phys.shapes.Shapes.joinIsNotEmpty(actorShape,
          shape.move(cell.getX(),cell.getY(),cell.getZ()),net.minecraft.world.phys.shapes.BooleanOp.AND))return false;
    }
    return level.getEntitiesOfClass(net.minecraft.world.entity.LivingEntity.class,body,
        other->other!=chao&&other.isAlive()&&!other.isSpectator()).isEmpty();
  }
  static void face(ChaoEntity chao,Direction direction) {
    float yaw=direction.toYRot();
    chao.setYRot(yaw);chao.setYBodyRot(yaw);chao.setYHeadRot(yaw);
  }
  private static boolean safeStand(ChaoEntity chao,BlockPos at) {
    var level=chao.level();var target=Vec3.atBottomCenterOf(at);
    return level.hasChunkAt(at)&&level.getBlockState(at.below()).isFaceSturdy(level,at.below(),Direction.UP)
        &&level.getFluidState(at).isEmpty()
        &&level.noCollision(chao,chao.getBoundingBox().move(target.subtract(chao.position())));
  }
  /** A fractional top needs real collision support, not a full block face. */
  private static boolean safeDismountAt(ChaoEntity chao,Vec3 at) {
    var level=chao.level();var body=chao.getBoundingBox().move(at.subtract(chao.position()));
    return chao.distanceToSqr(at)<36&&level.hasChunkAt(BlockPos.containing(body.minX,body.minY,body.minZ))
        &&level.hasChunkAt(BlockPos.containing(body.maxX,body.maxY,body.maxZ))
        &&!level.containsAnyLiquid(body)&&level.noCollision(chao,body)
        &&level.getBlockCollisions(chao,body.move(0,-.02,0)).iterator().hasNext();
  }
  private static Vec3 dismountPosition(ChaoEntity chao) {
    var tag=chao.getPersistentData();
    // A keeper may rescue/teleport the suspended actor far from its original toy.
    if(tag.getBoolean(DISMOUNT_WAIT)&&safeDismountAt(chao,chao.position()))return chao.position();
    if(tag.contains(TARGET)&&chao.distanceToSqr(Vec3.atCenterOf(target(chao)))>2.25
        &&safeDismountAt(chao,chao.position()))return chao.position();
    BlockPos saved=BlockPos.of(tag.getLong(RETURN));
    if(safeStand(chao,saved)&&safeDismountAt(chao,Vec3.atBottomCenterOf(saved)))return Vec3.atBottomCenterOf(saved);
    BlockPos origin=tag.contains(TARGET)?target(chao):saved;
    for(Direction side:Direction.Plane.HORIZONTAL) {
      BlockPos at=origin.relative(side);
      if(safeStand(chao,at)&&safeDismountAt(chao,Vec3.atBottomCenterOf(at)))return Vec3.atBottomCenterOf(at);
    }
    if(chao.level().hasChunkAt(origin)) {
      var shape=chao.level().getBlockState(origin).getCollisionShape(chao.level(),origin);
      if(!shape.isEmpty()) {
        Vec3 top=Vec3.atBottomCenterOf(origin).add(0,shape.max(Direction.Axis.Y)+.001,0);
        if(safeDismountAt(chao,top))return top;
      }
    }
    // Bounded recovery search: no chunk loading and no unvalidated teleport into walls or air.
    for(int radius=1;radius<=4;radius++)for(int dy:new int[]{0,1,-1,2,-2,3})
      for(int dx=-radius;dx<=radius;dx++)for(int dz=-radius;dz<=radius;dz++) {
        if(Math.max(Math.abs(dx),Math.abs(dz))!=radius)continue;
        BlockPos cell=origin.offset(dx,dy,dz);Vec3 at=Vec3.atBottomCenterOf(cell);
        if(safeStand(chao,cell)&&safeDismountAt(chao,at))return at;
      }
    return null;
  }
  @SubscribeEvent public static void recoverDismount(EntityTickEvent.Post event) {
    if(event.getEntity() instanceof ChaoEntity c&&!c.level().isClientSide&&c.getPersistentData().contains(BALL_CLEARANCE)
        &&!overlapsBall(c,BlockPos.of(c.getPersistentData().getLong(BALL_CLEARANCE))))c.getPersistentData().remove(BALL_CLEARANCE);
    if(event.getEntity() instanceof ChaoEntity chao&&!chao.level().isClientSide&&chao.isAlive()
        &&chao.getPersistentData().getBoolean(DISMOUNT_WAIT)) {
      chao.getNavigation().stop();chao.setDeltaMovement(Vec3.ZERO);chao.fallDistance=0;
      if(chao.tickCount%10==0)endPlaying(chao,false);
    }
  }
  @SubscribeEvent public static void preventRecoverySuffocation(LivingIncomingDamageEvent event) {
    if(event.getEntity() instanceof ChaoEntity chao&&chao.getPersistentData().getBoolean(DISMOUNT_WAIT)
        &&event.getSource().is(DamageTypes.IN_WALL))event.setCanceled(true);
  }
  public static void toyRemoved(ServerLevel level,BlockPos pos) {
    if(MOVING_BALLS.contains(pos))return;
    level.getEntitiesOfClass(ChaoEntity.class,new AABB(pos).inflate(10),c->
        c.getPersistentData().contains(TARGET)&&target(c).equals(pos)).forEach(c->endPlaying(c,false));
  }
  public static void playPulse(ServerLevel level,BlockPos pos,String toy) {
    if(toy.equals(BallPlayRules074.KIND))return; // Successful physical contact owns its one quiet cue.
    if(toy.equals(RadioPlayRules075.KIND))return; // Radio has its finite, proximity-admitted phrase below.
    if(toy.equals(TvWatchRules073.KIND)) {
      level.playSound(null,pos,SoundEvents.NOTE_BLOCK_BIT.value(),SoundSource.BLOCKS,.07f,1f);
      return; // One quiet startup cue; TV viewing does not emit musical-note particles.
    }
    var sound=switch(toy) {
      case "chao_trumpet"->SoundEvents.NOTE_BLOCK_DIDGERIDOO.value();
      case "chao_drum"->SoundEvents.NOTE_BLOCK_BASEDRUM.value();
      case "chao_piano"->SoundEvents.NOTE_BLOCK_HARP.value();
      case "chao_organ"->ChaoSounds.ORGAN.get();
      case "chao_tambourine"->ChaoSounds.TAMBOURINE.get();
      case "chao_bell"->ChaoSounds.HAND_BELL.get();
      case "chao_triangle"->SoundEvents.NOTE_BLOCK_BELL.value();
      case "chao_guitar"->SoundEvents.NOTE_BLOCK_GUITAR.value();
      case "chao_accordion","chao_flute"->SoundEvents.NOTE_BLOCK_FLUTE.value();
      case "chao_castanets"->ChaoSounds.CASTANETS.get();
      case "chao_cymbals"->ChaoSounds.CYMBALS.get();
      case "chao_jack_in_box"->SoundEvents.NOTE_BLOCK_PLING.value();
      case "chao_maracas"->ChaoSounds.MARACAS.get();
      case "chao_radio"->SoundEvents.NOTE_BLOCK_HARP.value();
      case "chao_tv"->SoundEvents.NOTE_BLOCK_BIT.value();
      case "chao_rattle"->ChaoSounds.RATTLE.get();
      default->SoundEvents.SLIME_BLOCK_STEP;
    };
    int beat=(int)(level.getGameTime()/16)%4;
    float pitch=toy.equals("chao_drum")?.9f:toy.equals("chao_ball")?.7f:1f+beat*.12f;
    if(toy.equals(JackPlayRules080.KIND))pitch=JackPlayRules080.POP_PITCH;
    level.playSound(null,pos,sound,SoundSource.BLOCKS,toy.equals(JackPlayRules080.KIND)?JackPlayRules080.POP_VOLUME:toy.equals("chao_tv")?.18f:.35f,pitch);
    if(toy.equals("chao_bubble_wand")||isWaterToy(toy))
      level.sendParticles(ParticleTypes.BUBBLE_POP,pos.getX()+.5,pos.getY()+.7,pos.getZ()+.5,4,.25,.2,.25,.01);
    else if(!List.of("chao_ball","chao_shovel","chao_rocking_horse").contains(toy))
      level.sendParticles(ParticleTypes.NOTE,pos.getX()+.5,pos.getY()+1.1,
        pos.getZ()+.5,1,.1,0,.1,0);
  }
  /** Sole radio sound path: real active owner, exact shared clock, and nearby-source admission. */
  public static boolean radioCue(ServerLevel level,BlockPos pos,int elapsed) {
    var state=level.getBlockState(pos);int cue=RadioPlayRules075.cueIndex(elapsed);
    if(cue<0||!state.is(block(RadioPlayRules075.KIND))||!state.getValue(ChaoToyBlock.PLAYING)
        ||!hasPlayerAt(level,pos)||sessionElapsed(level,pos)!=elapsed||!RadioAudio075.admit(level,pos))return false;
    level.playSound(null,pos,SoundEvents.NOTE_BLOCK_HARP.value(),SoundSource.BLOCKS,RadioPlayRules075.VOLUME,RadioPlayRules075.cuePitch(cue));
    level.sendParticles(ParticleTypes.NOTE,pos.getX()+.5,pos.getY()+.45,pos.getZ()+.5,1,.06,0,.06,0);
    return true;
  }
  /** School-owned pupils animate only their two authored toys and never mint a held item. */
  public static boolean beginSchoolPlay(com.chaogarden.kindergarten.KindergartenPupil0257 pupil,BlockPos pos,BlockPos stand){
    if(!(pupil.level() instanceof ServerLevel level)||!com.chaogarden.kindergarten.KindergartenContent.inKindergarten(level)
        ||!com.chaogarden.kindergarten.KindergartenPupils0257.schoolToy(pos)||!pupil.isAlive()
        ||pupil.position().distanceToSqr(Vec3.atBottomCenterOf(stand))>.5
        ||!(level.getBlockState(pos).getBlock() instanceof ChaoToyBlock toy)
        ||!available(level,pos,pupil))return false;
    beginPlaying(pupil,pos,stand,toy);return true;
  }
  private static void beginPlaying(ChaoEntity chao,BlockPos pos,BlockPos stand,ChaoToyBlock block) {
    var level=(ServerLevel)chao.level();var tag=chao.getPersistentData();long now=level.getGameTime();
    tag.remove(APPROACH);tag.putLong(TARGET,pos.asLong());tag.putString(KIND,block.toy);tag.putLong(UNTIL,now+PLAY_TICKS);
    tag.putLong(NEXT,now+PLAY_TICKS+600+Math.floorMod(chao.getUUID().hashCode(),201));
    chao.setToyPlaying(block.toy,PLAY_TICKS);chao.setToyTarget(pos);chao.getNavigation().stop();
    if(block.toy.equals(TvWatchRules073.KIND))holdTv(chao,pos);
    if(block.toy.equals(JackPlayRules080.KIND))holdJack(chao,pos);
    if(block.toy.equals(RadioPlayRules075.KIND))holdRadio(chao,pos);
    if(mounted(block.toy)) {
      if(!tag.getBoolean(MOUNT)){tag.putLong(RETURN,stand.asLong());tag.putBoolean(GRAVITY,chao.isNoGravity());}
      tag.putBoolean(MOUNT,true);
      chao.setNoGravity(true);chao.setDeltaMovement(Vec3.ZERO);
    }
    var state=level.getBlockState(pos);
    level.setBlock(pos,state.setValue(ChaoToyBlock.PLAYING,true).setValue(ChaoToyBlock.FRAME,
        BubblePlayRules.handToy(block.toy)&&RattlePlayRules.handheld(chao.state())?1:0),3);
    level.scheduleTick(pos,block,1);if(!block.toy.equals(TvWatchRules073.KIND)&&!block.toy.equals(BallPlayRules074.KIND)&&!block.toy.equals(RadioPlayRules075.KIND)&&!block.toy.equals(JackPlayRules080.KIND))chao.playSound(ChaoSounds.HAPPY.get(),.3f,chao.getVoicePitch());
  }
  public static void endPlaying(ChaoEntity chao,boolean finished) {
    var tag=chao.getPersistentData();long until=tag.getLong(UNTIL);
    if(tag.contains(TARGET)&&BallPlayRules074.KIND.equals(tag.getString(KIND))&&overlapsBall(chao,target(chao)))retainBallClearance(chao,target(chao));
    if(finished&&until>0&&chao.level().getGameTime()>=until
        &&chao.level().getBlockState(target(chao)).getBlock() instanceof ChaoToyBlock) {
      var state=chao.state();String kind=tag.getString(KIND);
      state.happiness=Math.min(100,state.happiness+(kind.equals("chao_rattle")&&!state.adult?7:4));
      state.boredom=Math.max(0,state.boredom-35);
      state.addEmotion(ChaoEmotion.JOY,20);
      state.energy=Math.max(0,state.energy-2);chao.sync();
    }
    // The reserved block remains the recoverable source; put its model back immediately.
    if(tag.contains(KIND)&&tag.contains(TARGET)&&chao.level() instanceof ServerLevel level) {
      BlockPos pos=target(chao);var source=level.getBlockState(pos);
      if(source.getBlock() instanceof ChaoToyBlock block&&tag.getString(KIND).equals(block.toy)
          &&source.getValue(ChaoToyBlock.PLAYING))
        level.setBlock(pos,source.setValue(ChaoToyBlock.PLAYING,false).setValue(ChaoToyBlock.FRAME,0),3);
    }
    boolean deferred=false;
    if(tag.getBoolean(MOUNT)) {
      Vec3 destination=dismountPosition(chao);
      chao.getNavigation().stop();chao.setDeltaMovement(Vec3.ZERO);chao.fallDistance=0;
      if(destination==null) {
        // A completely sealed area has no safe teleport. Retain recovery ownership across reload.
        if(!tag.getBoolean(DISMOUNT_WAIT))tag.putBoolean(SAVED_AI,chao.isNoAi());
        tag.putBoolean(DISMOUNT_WAIT,true);chao.setNoAi(true);chao.setNoGravity(true);deferred=true;
      } else {
        chao.setPos(destination.x,destination.y,destination.z);
        chao.setNoGravity(tag.getBoolean(GRAVITY));
        if(tag.getBoolean(DISMOUNT_WAIT))chao.setNoAi(tag.getBoolean(SAVED_AI));
      }
    }
    if(!deferred) {
      tag.remove(MOUNT);tag.remove(RETURN);tag.remove(GRAVITY);tag.remove(TARGET);
      tag.remove(DISMOUNT_WAIT);tag.remove(SAVED_AI);
    }
    // Cancel animation/reward exactly once even when physical dismount must await clear space.
    tag.remove(APPROACH);tag.remove(UNTIL);tag.remove(KIND);
    chao.setToyPlaying("",0);
  }

  /** Selection has a personal random pause and a willingness roll, rather than a player command. */
  public static final class PlayGoal extends Goal {
    private final ChaoEntity chao;
    private BlockPos toy,stand;
    private long nextCheck,approachExpiry,approachHardExpiry;
    private Vec3 approachProgress;
    private BallController074 ballController;
    private int lastHandToyCue=-1;
    private boolean surprised;
    private boolean engaged;
    private String selectedKind="";
    private boolean selectedWaterlogged;
    public PlayGoal(ChaoEntity chao){this.chao=chao;setFlags(EnumSet.of(Flag.MOVE,Flag.LOOK,Flag.JUMP));}
    @Override public boolean canUse() {
      long now=chao.level().getGameTime();
      if(!(chao.level() instanceof ServerLevel level)||now<nextCheck||now<chao.getPersistentData().getLong(NEXT)||!mayPlay(chao))return false;
      nextCheck=now+40+chao.getRandom().nextInt(61);
      int interest=ChaoPreferences.playChanceDenominator(chao.state());
      if(interest<=0||chao.getRandom().nextInt(interest)!=0)return false;
      double best=Double.MAX_VALUE;toy=stand=null;
      var center=chao.blockPosition();
      for(BlockPos mutable:BlockPos.betweenClosed(center.offset(-7,-2,-7),center.offset(7,2,7))) {
        if(!level.hasChunkAt(mutable)||!(level.getBlockState(mutable).getBlock() instanceof ChaoToyBlock block)
            ||isWaterToy(block.toy)||!knowsToy(chao,block.toy)
            ||(block.toy.equals("chao_shovel")&&!level.getBlockState(mutable.below()).is(net.minecraft.tags.BlockTags.SAND))
            ||(block.toy.equals("chao_lifesaver")&&!level.getBlockState(mutable).getValue(ChaoToyBlock.WATERLOGGED))
            ||!available(level,mutable,chao))continue;
        BlockPos at=standAt(level,mutable,chao);if(at==null)continue;
        if(mounted(block.toy)&&!mountPathClear(chao,mutable,at))continue;
        double distance=chao.distanceToSqr(Vec3.atBottomCenterOf(at));
        if(distance>=best)continue;
        var path=chao.getNavigation().createPath(at,0);
        if(path!=null&&path.canReach()){best=distance;toy=mutable.immutable();stand=at.immutable();}
      }
      return toy!=null;
    }
    @Override public void start() {
      engaged=false;surprised=false;ballController=null;lastHandToyCue=-1;
      var selected=chao.level().getBlockState(toy);
      selectedKind=((ChaoToyBlock)selected.getBlock()).toy;selectedWaterlogged=selected.getValue(ChaoToyBlock.WATERLOGGED);
      long now=chao.level().getGameTime();
      approachExpiry=now+240;approachHardExpiry=now+2400;approachProgress=chao.position();
      chao.getPersistentData().putLong(TARGET,toy.asLong());
      chao.getPersistentData().putLong(APPROACH,approachExpiry);
      if(selectedKind.equals(BallPlayRules074.KIND))chao.setToyTarget(toy);
      navigateToStand();
    }
    private void navigateToStand() {
      // The coordinate moveTo overload rebuilds a path with reach accuracy 1.
      // That can stop one block away from this deliberately clear standing
      // cell, leaving the Chao unable to start watching the screen. Follow the
      // exact-cell path we tested instead of silently widening its endpoint.
      var path=chao.getNavigation().createPath(stand,0);
      if(path!=null&&path.canReach())chao.getNavigation().moveTo(path,.9);
    }
    private boolean environmentValid(net.minecraft.world.level.block.state.BlockState state) {
      if(!(state.getBlock() instanceof ChaoToyBlock block)||!block.toy.equals(selectedKind))return false;
      if(!ToyInteractionRules.environmentValid(block.toy,state.getValue(ChaoToyBlock.WATERLOGGED),selectedWaterlogged,
          chao.level().getBlockState(toy.below()).is(net.minecraft.tags.BlockTags.SAND)))return false;
      if(block.toy.equals("chao_toy_car")&&Direction.getNearest(stand.getX()-toy.getX(),0,stand.getZ()-toy.getZ())!=state.getValue(ChaoToyBlock.FACING).getOpposite())return false;
      if(ToyInteractionRules.stationary(block.toy)&&(ToyInteractionRules.frontOnly(block.toy)||block.toy.equals(RadioPlayRules075.KIND))) {
        Direction side=Direction.getNearest(stand.getX()-toy.getX(),0,stand.getZ()-toy.getZ());
        if(side!=state.getValue(ChaoToyBlock.FACING))return false;
      }
      return true;
    }
    private boolean stationaryValid(net.minecraft.world.level.block.state.BlockState state) {
      String kind=((ChaoToyBlock)state.getBlock()).toy;if(!ToyInteractionRules.stationary(kind))return true;
      Vec3 anchor=stationaryAnchor(chao,toy,stand);
      float yaw=Direction.getNearest(toy.getX()+.5-anchor.x,0,toy.getZ()+.5-anchor.z).toYRot();
      return ToyInteractionRules.stationaryWithinReach(kind,chao.distanceToSqr(anchor))&&supportedStationaryAt(chao,anchor)
          &&proneSpaceReady(chao,kind,anchor,yaw,toy);
    }
    @Override public boolean canContinueToUse() {
      if(!(chao.level() instanceof ServerLevel level)||toy==null||!mayPlay(chao)
          ||!(level.getBlockState(toy).getBlock() instanceof ChaoToyBlock)||!available(level,toy,chao))return false;
      var state=level.getBlockState(toy);String kind=((ChaoToyBlock)state.getBlock()).toy;
      if(!environmentValid(state)||(engaged&&!stationaryValid(state)))return false;
      if(engaged&&(kind.equals("chao_ball")||mounted(kind))
          &&chao.distanceToSqr(Vec3.atCenterOf(toy))>16)return false;
      long now=level.getGameTime();
      // Completion belongs to tick(), including the first resumed tick at/after the deadline.
      // stop() is cancellation and must not steal a valid completed session's reward.
      return engaged?chao.getPersistentData().getLong(UNTIL)>0:now<approachExpiry;
    }
    @Override public void tick() {
      if(toy==null)return;
      var level=(ServerLevel)chao.level();var blockState=level.getBlockState(toy);
      if(!(blockState.getBlock() instanceof ChaoToyBlock block))return;
      if(!environmentValid(blockState)||(engaged&&!stationaryValid(blockState))) {
        endPlaying(chao,false);approachExpiry=0;return;
      }
      long now=level.getGameTime();
      // Slow newborns retain a progressing route, without retaining a blocked toy forever.
      if(!engaged&&approachProgress!=null&&chao.distanceToSqr(approachProgress)>=.0016) {
        approachProgress=chao.position();approachExpiry=Math.min(approachHardExpiry,now+240);
        chao.getPersistentData().putLong(APPROACH,approachExpiry);
      }
      if(engaged&&((block.toy.equals("chao_lifesaver")&&!blockState.getValue(ChaoToyBlock.WATERLOGGED))
          ||((block.toy.equals("chao_ball")||mounted(block.toy))
              &&chao.distanceToSqr(Vec3.atCenterOf(toy))>16))) {
        endPlaying(chao,false);approachExpiry=0;return;
      }
      if(engaged) {
        chao.getLookControl().setLookAt(toy.getX()+.5,toy.getY()+(ToyInteractionRules.keyboard(block.toy)?.18:.6),toy.getZ()+.5,30,20);
        if(mounted(block.toy)) {
          int elapsed=(int)(PLAY_TICKS-(chao.getPersistentData().getLong(UNTIL)-now));
          if(!holdMounted(chao,toy,elapsed)){endPlaying(chao,false);approachExpiry=0;return;}
        } else if(ToyInteractionRules.keyboard(block.toy)) {
          if(!holdKeyboard(chao,toy)){endPlaying(chao,false);approachExpiry=0;return;}
        } else if(block.toy.equals(BallPlayRules074.KIND)) {
          if(ballController==null)ballController=new BallController074(chao,toy,stand);
          if(!ballController.tick()){endPlaying(chao,false);approachExpiry=0;return;}
          toy=ballController.target();stand=ballController.stand();
        } else if(block.toy.equals(TvWatchRules073.KIND)) {
          if(!holdTv(chao,toy)){endPlaying(chao,false);approachExpiry=0;return;}
        } else if(block.toy.equals(RadioPlayRules075.KIND)) {
          if(!holdRadio(chao,toy)){endPlaying(chao,false);approachExpiry=0;return;}
        } else if(block.toy.equals(JackPlayRules080.KIND)) {
          if(!holdJack(chao,toy)){endPlaying(chao,false);approachExpiry=0;return;}
        } else {
          Vec3 anchor=stationaryAnchor(chao,toy,stand);
          chao.getNavigation().stop();chao.setDeltaMovement(Vec3.ZERO);chao.setPos(anchor.x,anchor.y,anchor.z);
          face(chao,Direction.getNearest(toy.getX()+.5-anchor.x,0,toy.getZ()+.5-anchor.z));
          chao.getLookControl().setLookAt(toy.getX()+.5,toy.getY()+ToyInteractionRules.stationaryLookHeight(block.toy),toy.getZ()+.5,30,20);
        }
        int elapsed=(int)(PLAY_TICKS-(chao.getPersistentData().getLong(UNTIL)-now));
        if(lastHandToyCue!=elapsed){lastHandToyCue=elapsed;HandToyEffects.tick(chao,block.toy,elapsed);if(block.toy.equals(PianoPlayRules.KIND))PianoPlayRules.cue(chao,elapsed);if(block.toy.equals(OrganPlayRules.KIND))OrganPlayRules.cue(chao,elapsed);}
        if(!surprised&&block.toy.equals("chao_jack_in_box")&&ToyInteractionRules.jackReactionDue((int)(PLAY_TICKS-(chao.getPersistentData().getLong(UNTIL)-now)))) {
          surprised=true;
          if(chao.getRandom().nextFloat()<.3f) {
            endPlaying(chao,false);approachExpiry=0;
            boolean cry=ChaoPreferences.hasPersonality(chao.state(),ChaoPreferences.CRY_BABY);
            chao.state().addEmotion(cry?ChaoEmotion.CRY:ChaoEmotion.FEAR,125);
            chao.startBehavior(cry?ChaoEntity.Behavior.CRY:ChaoEntity.Behavior.FLEE,80);chao.sync();return;
          }
        }
        if(now>=chao.getPersistentData().getLong(UNTIL)){endPlaying(chao,true);approachExpiry=0;}
      } else if(chao.distanceToSqr(Vec3.atBottomCenterOf(stand))<.7) {
        boolean stationary=ToyInteractionRules.stationary(block.toy);
        boolean precise=ToyInteractionRules.keyboard(block.toy)||mounted(block.toy)||block.toy.equals("chao_ball")||stationary;
        double contactTolerance=block.toy.equals(BallPlayRules074.KIND)?.0009:.0064;
        Vec3 anchor=ToyInteractionRules.keyboard(block.toy)?keyboardAnchor(chao,toy)
            :block.toy.equals("chao_ball")?ballAnchor(chao,toy,stand)
            :stationary?stationaryAnchor(chao,toy,stand):Vec3.atBottomCenterOf(stand);
        // Vanilla MoveControl tests the current block's full player collision
        // shape when deciding to jump. Finish walking just outside the keyboard
        // cell, then use the same short collision-checked contact handoff as
        // other toys; do not ask MoveControl to climb into the low manual.
        boolean keyboard=ToyInteractionRules.keyboard(block.toy);
        boolean staged=keyboard||stationary&&BlockPos.containing(anchor).equals(toy);
        var entry=keyboard?blockState.getValue(ChaoToyBlock.FACING):Direction.getNearest(stand.getX()-toy.getX(),0,stand.getZ()-toy.getZ());
        Vec3 approach=staged&&chao.distanceToSqr(anchor)>.0064?Vec3.atBottomCenterOf(toy).add(entry.getStepX()*.55,0,entry.getStepZ()*.55):anchor;
        float facing=Direction.getNearest(toy.getX()+.5-anchor.x,0,toy.getZ()+.5-anchor.z).toYRot();
        if(stationary&&(!supportedStationaryAt(chao,anchor)
            ||!proneSpaceReady(chao,block.toy,anchor,facing,toy))){endPlaying(chao,false);approachExpiry=0;return;}
        if(keyboard&&standAt(level,toy,chao)==null){endPlaying(chao,false);approachExpiry=0;return;}
        if(precise&&chao.distanceToSqr(approach)>contactTolerance) {
          // Final short approach stays in the clear stand cell; don't accept the path's broad arrival radius.
          chao.getNavigation().stop();
          if(staged){
            var velocity=chao.getDeltaMovement();double horizontal=velocity.horizontalDistance();
            if(horizontal>.08)chao.setDeltaMovement(velocity.x*.08/horizontal,velocity.y,velocity.z*.08/horizontal);
          }
          // Below this effective speed, vanilla zeroes a crawling baby's motion.
          // Keep the ordinary movement attribute and unlocks; only the last goal pace changes.
          double pace=staged?Math.min(.9,Math.max(.3,.0675/Math.max(.001,chao.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED)))):.9;
          chao.getMoveControl().setWantedPosition(approach.x,approach.y,approach.z,pace);
        } else if(!precise||level.noCollision(chao,chao.getBoundingBox().move(anchor.subtract(chao.position())))) {
          if(precise){chao.setPos(anchor.x,anchor.y,anchor.z);chao.setDeltaMovement(Vec3.ZERO);}
          beginPlaying(chao,toy,stand,block);engaged=true;
        }
      } else if(chao.getNavigation().isDone())navigateToStand();
    }
    @Override public void stop(){endPlaying(chao,false);chao.getNavigation().stop();toy=stand=null;ballController=null;}
    @Override public boolean requiresUpdateEveryTick(){return true;}
  }
}
