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
  public static final DeferredRegister.Blocks BLOCKS=DeferredRegister.createBlocks(ChaoGarden.MODID);
  private static final Map<String,DeferredBlock<ChaoToyBlock>> TOYS=new LinkedHashMap<>();
  private static final Set<ChaoEntity> GOALS=Collections.newSetFromMap(new WeakHashMap<>());
  private static final Set<BlockPos> MOVING_BALLS=new HashSet<>();
  static {
    for(String id:java.util.stream.Stream.of(MarketStock.TOYS,MarketStock.LEGACY_TOYS,MarketStock.LESSON_TOYS,List.of("chao_shovel"))
        .flatMap(List::stream).toList()) {
      TOYS.put(id,BLOCKS.register(id,()->new ChaoToyBlock(id,BlockBehaviour.Properties.of()
          .mapColor(MapColor.WOOD).strength(.4f).sound(SoundType.WOOD).noOcclusion().randomTicks()
          .lightLevel(state->state.getValue(ChaoToyBlock.PLAYING)
              && (id.equals("chao_tv")||id.equals("chao_radio"))?4:0))));
    }
  }
  private ToyContent() {}
  public static void register(IEventBus bus){BLOCKS.register(bus);ToyCueNetwork.register(bus);}
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
        &&!chao.isBehaving()&&!chao.state().isIll()
        // A stationary pose can clear vanilla's onGround flag. Keep the server-owned
        // session eligible through its terminal tick; its environment/contact is checked below.
        &&((!chao.isInWater()&&chao.onGround())||chao.getPersistentData().getLong(UNTIL)>0
            ||chao.getPersistentData().getBoolean(MOUNT))&&!chao.isStaying()
        &&chao.state().energy>=25&&chao.state().hunger>=30
        &&!ActivityManager.isBusy(chao)&&!CompanionContent.isManaged(chao);
  }
  public static boolean isWaterToy(String kind){return kind.equals("chao_rubber_duck")||kind.equals("chao_lifesaver");}
  private static boolean mounted(String kind){return kind.equals("chao_rocking_horse")||kind.equals("chao_coffin")||kind.equals("chao_lifesaver");}
  private static boolean knowsToy(ChaoEntity chao,String kind) {
    if(kind.equals("chao_shovel"))return chao.state().traitInt("reward.shovel",0)>0;
    String id=kind.substring("chao_".length());
    return !List.of("trumpet","drum","maracas","triangle","guitar","accordion","bell","castanets",
        "cymbals","flute","tambourine").contains(id)||ChaoCareServices.knowsLesson(chao,id);
  }
  private static boolean available(ServerLevel level,BlockPos pos,ChaoEntity candidate) {
    return level.hasChunkAt(pos)&&level.getBlockState(pos).getBlock() instanceof ChaoToyBlock
        &&level.getEntitiesOfClass(ChaoEntity.class,new AABB(pos).inflate(10),other->
            other!=candidate&&other.isAlive()&&!other.isRemoved()
                &&other.getPersistentData().contains(TARGET)&&target(other).equals(pos)
                && (isPlaying(other)||other.getPersistentData().getBoolean(DISMOUNT_WAIT)||other.getNavigation().isInProgress()
                    ||other.getPersistentData().getLong(APPROACH)>level.getGameTime())).isEmpty();
  }
  private static BlockPos standAt(ServerLevel level,BlockPos pos,ChaoEntity chao) {
    BlockPos best=null;double distance=Double.MAX_VALUE;
    var state=level.getBlockState(pos);
    String kind=((ChaoToyBlock)state.getBlock()).toy;
    // Screens, keyboard keys and the coffin opening have an authored front.
    // Round/percussive toys can be approached from any clear side.
    Iterable<Direction> sides=ToyInteractionRules.frontOnly(kind)
        ?List.of(state.getValue(ChaoToyBlock.FACING)):Direction.Plane.HORIZONTAL;
    for(Direction direction:sides) {
      var at=pos.relative(direction);
      if(isWaterToy(kind)&&state.getValue(ChaoToyBlock.WATERLOGGED))at=at.above();
      if(!level.hasChunkAt(at)||!level.getBlockState(at.below()).isFaceSturdy(level,at.below(),Direction.UP)
          ||!level.getBlockState(at).getCollisionShape(level,at).isEmpty()
          ||!level.getBlockState(at).getFluidState().isEmpty())continue;
      if(ToyInteractionRules.stationary(kind)&&!supportedStationaryAt(chao,stationaryAnchor(chao,pos,at)))continue;
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
    if(block.toy.equals(OrganPlayRules.KIND)&&chao.level() instanceof ServerLevel server&&standAt(server,pos,chao)==null)return false;
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
      if(kind.equals(OrganPlayRules.KIND)&&standAt(level,at,c)==null)continue;
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
  public static Vec3 stationaryAnchor(ChaoEntity chao,BlockPos pos,BlockPos stand) {
    var state=chao.level().getBlockState(pos);String kind=((ChaoToyBlock)state.getBlock()).toy;
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
  public static Vec3 ballAnchor(ChaoEntity chao,BlockPos pos,BlockPos stand) {
    Direction side=Direction.getNearest(stand.getX()-pos.getX(),0,stand.getZ()-pos.getZ());
    double reach=ToyInteractionRules.ballContactDistance(chao.getBbWidth());
    return Vec3.atBottomCenterOf(pos).add(side.getStepX()*reach,0,side.getStepZ()*reach);
  }
  public static Vec3 mountedAnchor(BlockPos pos,net.minecraft.world.level.block.state.BlockState state,int frame) {
    String kind=((ChaoToyBlock)state.getBlock()).toy;
    if(kind.equals("chao_lifesaver"))return Vec3.atBottomCenterOf(pos).add(0,.78+ToyInteractionRules.ringBob(frame),0);
    Direction facing=state.getValue(ChaoToyBlock.FACING);double z=ToyInteractionRules.horseRiderZ(frame);
    return Vec3.atBottomCenterOf(pos).add(-facing.getStepX()*z,ToyInteractionRules.horseRiderY(frame),-facing.getStepZ()*z);
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
  private static void face(ChaoEntity chao,Direction direction) {
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
    level.playSound(null,pos,sound,SoundSource.BLOCKS,toy.equals("chao_tv")?.18f:.35f,pitch);
    if(toy.equals("chao_bubble_wand")||isWaterToy(toy))
      level.sendParticles(ParticleTypes.BUBBLE_POP,pos.getX()+.5,pos.getY()+.7,pos.getZ()+.5,4,.25,.2,.25,.01);
    else if(!List.of("chao_ball","chao_coffin","chao_shovel","chao_rocking_horse").contains(toy))
      level.sendParticles(ParticleTypes.NOTE,pos.getX()+.5,pos.getY()+1.1,
        pos.getZ()+.5,1,.1,0,.1,0);
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
    if(mounted(block.toy)) {
      tag.putBoolean(MOUNT,true);tag.putLong(RETURN,stand.asLong());tag.putBoolean(GRAVITY,chao.isNoGravity());
      chao.setNoGravity(true);chao.setDeltaMovement(Vec3.ZERO);
    }
    var state=level.getBlockState(pos);
    level.setBlock(pos,state.setValue(ChaoToyBlock.PLAYING,true).setValue(ChaoToyBlock.FRAME,
        BubblePlayRules.handToy(block.toy)&&RattlePlayRules.handheld(chao.state())?1:0),3);
    level.scheduleTick(pos,block,1);chao.playSound(ChaoSounds.HAPPY.get(),.3f,chao.getVoicePitch());
  }
  public static void endPlaying(ChaoEntity chao,boolean finished) {
    var tag=chao.getPersistentData();long until=tag.getLong(UNTIL);
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
    private long nextKick;
    private int lastHandToyCue=-1;
    private boolean surprised;
    private boolean engaged;
    private boolean kickContactApplied;
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
            ||!knowsToy(chao,block.toy)
            ||(block.toy.equals("chao_shovel")&&!level.getBlockState(mutable.below()).is(net.minecraft.tags.BlockTags.SAND))
            ||(block.toy.equals("chao_lifesaver")&&!level.getBlockState(mutable).getValue(ChaoToyBlock.WATERLOGGED))
            ||!available(level,mutable,chao))continue;
        BlockPos at=standAt(level,mutable,chao);if(at==null)continue;
        if(block.toy.equals("chao_rocking_horse")||block.toy.equals("chao_lifesaver")) {
          boolean clear=true;for(int frame=0;frame<4;frame++)
            if(!mountedClear(chao,mutable,mountedAnchor(mutable,level.getBlockState(mutable),frame))){clear=false;break;}
          if(!clear)continue;
        }
        double distance=chao.distanceToSqr(Vec3.atBottomCenterOf(at));
        if(distance>=best)continue;
        var path=chao.getNavigation().createPath(at,0);
        if(path!=null&&path.canReach()){best=distance;toy=mutable.immutable();stand=at.immutable();}
      }
      return toy!=null;
    }
    @Override public void start() {
      engaged=false;surprised=false;nextKick=0;kickContactApplied=false;lastHandToyCue=-1;
      var selected=chao.level().getBlockState(toy);
      selectedKind=((ChaoToyBlock)selected.getBlock()).toy;selectedWaterlogged=selected.getValue(ChaoToyBlock.WATERLOGGED);
      long now=chao.level().getGameTime();
      approachExpiry=now+240;approachHardExpiry=now+2400;approachProgress=chao.position();
      chao.getPersistentData().putLong(TARGET,toy.asLong());
      chao.getPersistentData().putLong(APPROACH,approachExpiry);
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
      if(ToyInteractionRules.stationary(block.toy)&&ToyInteractionRules.frontOnly(block.toy)) {
        Direction side=Direction.getNearest(stand.getX()-toy.getX(),0,stand.getZ()-toy.getZ());
        if(side!=state.getValue(ChaoToyBlock.FACING))return false;
      }
      return true;
    }
    private boolean stationaryValid(net.minecraft.world.level.block.state.BlockState state) {
      String kind=((ChaoToyBlock)state.getBlock()).toy;if(!ToyInteractionRules.stationary(kind))return true;
      Vec3 anchor=stationaryAnchor(chao,toy,stand);
      return ToyInteractionRules.stationaryWithinReach(kind,chao.distanceToSqr(anchor))&&supportedStationaryAt(chao,anchor);
    }
    @Override public boolean canContinueToUse() {
      if(!(chao.level() instanceof ServerLevel level)||toy==null||!mayPlay(chao)
          ||!(level.getBlockState(toy).getBlock() instanceof ChaoToyBlock)||!available(level,toy,chao))return false;
      var state=level.getBlockState(toy);String kind=((ChaoToyBlock)state.getBlock()).toy;
      if(!environmentValid(state)||(engaged&&!stationaryValid(state)))return false;
      if(engaged&&(kind.equals("chao_ball")||kind.equals("chao_rocking_horse")||kind.equals("chao_lifesaver"))
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
          ||((block.toy.equals("chao_ball")||block.toy.equals("chao_rocking_horse")||block.toy.equals("chao_lifesaver"))
              &&chao.distanceToSqr(Vec3.atCenterOf(toy))>16))) {
        endPlaying(chao,false);approachExpiry=0;return;
      }
      if(engaged) {
        chao.getLookControl().setLookAt(toy.getX()+.5,toy.getY()+(ToyInteractionRules.keyboard(block.toy)?.18:.6),toy.getZ()+.5,30,20);
        if(mounted(block.toy)) {
          chao.getNavigation().stop();chao.setDeltaMovement(Vec3.ZERO);chao.fallDistance=0;
          if(block.toy.equals("chao_coffin")) {
            int elapsed=(int)(PLAY_TICKS-(chao.getPersistentData().getLong(UNTIL)-now));
            if(elapsed>=128&&!safeStand(chao,stand)){endPlaying(chao,false);approachExpiry=0;return;}
            double occupied=ToyInteractionRules.coffinOccupancy(elapsed);
            Direction along=blockState.getValue(ChaoToyBlock.FACING).getClockWise();
            // Reclining around the feet puts the head behind the actor; center that span over the bed.
            Vec3 start=Vec3.atBottomCenterOf(stand),inside=Vec3.atBottomCenterOf(toy)
                .add(along.getStepX()*.18,.12,along.getStepZ()*.18);
            Vec3 at=start.lerp(inside,occupied);chao.setPos(at.x,at.y,at.z);
            // The imported coffin's long axis is local X, not its front/back axis.
            face(chao,along);
            chao.getLookControl().setLookAt(chao.getX()+along.getStepX(),chao.getEyeY(),chao.getZ()+along.getStepZ(),180,180);
          } else {
            int elapsed=(int)(PLAY_TICKS-(chao.getPersistentData().getLong(UNTIL)-now));
            if(elapsed>=148&&!safeStand(chao,stand)){endPlaying(chao,false);approachExpiry=0;return;}
            int frame=ToyInteractionRules.mountedFrame(elapsed);
            Direction facing=blockState.getValue(ChaoToyBlock.FACING);
            Vec3 inside=mountedAnchor(toy,blockState,frame);
            // Source muzzle points +Z, opposite the authored north-facing block front.
            if(block.toy.equals("chao_rocking_horse"))facing=facing.getOpposite();
            double occupied=ToyInteractionRules.mountOccupancy(elapsed);
            Vec3 at=Vec3.atBottomCenterOf(stand).lerp(inside,occupied);
            if(block.toy.equals("chao_lifesaver"))at=at.add(0,ToyInteractionRules.ringEntryLift(occupied),0);
            if(!mountedClear(chao,toy,at)){endPlaying(chao,false);approachExpiry=0;return;}
            chao.setPos(at.x,at.y,at.z);face(chao,facing);
            chao.getLookControl().setLookAt(chao.getX()+facing.getStepX(),chao.getEyeY(),chao.getZ()+facing.getStepZ(),180,180);
          }
        } else if(ToyInteractionRules.keyboard(block.toy)) {
          if(!holdKeyboard(chao,toy)){endPlaying(chao,false);approachExpiry=0;return;}
        } else if(block.toy.equals("chao_ball")) {
          int kick=chao.toyKickElapsed();
          if(kick>=0) {
            chao.getNavigation().stop();chao.setDeltaMovement(Vec3.ZERO);
            if(!kickContactApplied&&kick>=ToyInteractionRules.BALL_CONTACT_TICK) {
              kickContactApplied=true;
              Vec3 delta=chao.position().subtract(Vec3.atBottomCenterOf(toy));
              if(ToyInteractionRules.ballWithinReach(delta.horizontalDistanceSqr(),delta.y,chao.getBbWidth()))rollBall(level);
            }
          } else {
            Vec3 anchor=ballAnchor(chao,toy,stand);
            if(chao.distanceToSqr(anchor)>.0064)approachContact(anchor);
            else if(level.noCollision(chao,chao.getBoundingBox().move(anchor.subtract(chao.position())))) {
              chao.getNavigation().stop();chao.setDeltaMovement(Vec3.ZERO);chao.setPos(anchor.x,anchor.y,anchor.z);
              face(chao,Direction.getNearest(toy.getX()+.5-chao.getX(),0,toy.getZ()+.5-chao.getZ()));
              if(now>=nextKick){nextKick=now+35;kickContactApplied=false;chao.startToyKick();}
            }
          }
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
        Vec3 anchor=ToyInteractionRules.keyboard(block.toy)?keyboardAnchor(chao,toy)
            :block.toy.equals("chao_ball")?ballAnchor(chao,toy,stand)
            :stationary?stationaryAnchor(chao,toy,stand):Vec3.atBottomCenterOf(stand);
        // Vanilla MoveControl tests the current block's full player collision
        // shape when deciding to jump. Finish walking just outside the organ
        // cell, then use the same short collision-checked contact handoff as
        // other toys; do not ask MoveControl to climb into the low manual.
        boolean organ=block.toy.equals(OrganPlayRules.KIND);
        Vec3 approach=organ&&chao.distanceToSqr(anchor)>.0064?Vec3.atBottomCenterOf(toy).add(blockState.getValue(ChaoToyBlock.FACING).getStepX()*.55,0,blockState.getValue(ChaoToyBlock.FACING).getStepZ()*.55):anchor;
        if(stationary&&!supportedStationaryAt(chao,anchor)){endPlaying(chao,false);approachExpiry=0;return;}
        if(organ&&standAt(level,toy,chao)==null){endPlaying(chao,false);approachExpiry=0;return;}
        if(precise&&chao.distanceToSqr(approach)>.0064) {
          // Final short approach stays in the clear stand cell; don't accept the path's broad arrival radius.
          chao.getNavigation().stop();
          if(organ){
            var velocity=chao.getDeltaMovement();double horizontal=velocity.horizontalDistance();
            if(horizontal>.08)chao.setDeltaMovement(velocity.x*.08/horizontal,velocity.y,velocity.z*.08/horizontal);
          }
          chao.getMoveControl().setWantedPosition(approach.x,approach.y,approach.z,organ?.3:.9);
        } else if(!precise||level.noCollision(chao,chao.getBoundingBox().move(anchor.subtract(chao.position())))) {
          if(precise){chao.setPos(anchor.x,anchor.y,anchor.z);chao.setDeltaMovement(Vec3.ZERO);}
          beginPlaying(chao,toy,stand,block);engaged=true;
        }
      } else if(chao.getNavigation().isDone())navigateToStand();
    }
    private void approachContact(Vec3 anchor) {
      if(chao.distanceToSqr(Vec3.atBottomCenterOf(stand))<.7) {
        chao.getNavigation().stop();chao.getMoveControl().setWantedPosition(anchor.x,anchor.y,anchor.z,.9);
      } else if(chao.getNavigation().isDone())navigateToStand();
    }
    /** Move the one real block; removing it here never creates an item copy. */
    private void rollBall(ServerLevel level) {
      Vec3 away=Vec3.atCenterOf(toy).subtract(chao.position());
      Direction direction=Direction.getNearest(away.x,0,away.z);
      if(direction.getAxis()==Direction.Axis.Y)direction=Direction.NORTH;
      BlockPos next=toy.relative(direction);
      if(!level.hasChunkAt(next)||!level.getBlockState(next).isAir()
          ||!level.getBlockState(next.below()).isFaceSturdy(level,next.below(),Direction.UP))return;
      var state=level.getBlockState(toy);BlockPos old=toy;
      var destination=state.getCollisionShape(level,next).move(next.getX(),next.getY(),next.getZ()).bounds();
      if(!level.noCollision(null,destination)||!level.getEntitiesOfClass(net.minecraft.world.entity.LivingEntity.class,destination,
          entity->entity.isAlive()&&!entity.isSpectator()).isEmpty())return;
      if(!level.setBlock(next,state,3))return;
      MOVING_BALLS.add(old);
      try {
        if(!level.setBlock(old,Blocks.AIR.defaultBlockState(),3)) {level.setBlock(next,Blocks.AIR.defaultBlockState(),3);return;}
        toy=next;chao.getPersistentData().putLong(TARGET,toy.asLong());
        BlockPos at=standAt(level,toy,chao);if(at!=null)stand=at;
        level.scheduleTick(toy,state.getBlock(),1);
        chao.playSound(SoundEvents.SLIME_BLOCK_STEP,.4f,.8f);
      } finally {MOVING_BALLS.remove(old);}
    }
    @Override public void stop(){endPlaying(chao,false);chao.getNavigation().stop();toy=stand=null;}
    @Override public boolean requiresUpdateEveryTick(){return true;}
  }
}
