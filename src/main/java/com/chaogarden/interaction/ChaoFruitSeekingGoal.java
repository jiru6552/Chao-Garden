package com.chaogarden.interaction;

import com.chaogarden.activities.ActivityManager;
import com.chaogarden.companion.CompanionContent;
import com.chaogarden.entity.ChaoEntity;
import com.chaogarden.entity.ChaoEntity.Behavior;
import com.chaogarden.fruit.FruitContent;
import com.chaogarden.fruit.HangingFruitBlock;
import java.util.EnumSet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.phys.Vec3;

/** Hungry Chao walk to a real ripe orchard, shake its trunk and release fruit.
 * Harvests use the block's ordinary one-item transaction; eating still requires
 * the normal dropped-item pickup and held-item decision pipeline.
 */
public final class ChaoFruitSeekingGoal extends Goal {
  private final ChaoEntity chao;
  private BlockPos tree,stand;
  private long nextCheck,expires,shakeStarted;
  private boolean shaking,harvested;
  public ChaoFruitSeekingGoal(ChaoEntity chao) {
    this.chao=chao;setFlags(EnumSet.of(Flag.MOVE,Flag.LOOK,Flag.JUMP));
  }
  private boolean eligible() {
    return chao.isAlive()&&!chao.isNoAi()&&!chao.isStaying()&&!chao.isCocooned()
        &&!chao.isResting()&&!chao.isHandlingItem()&&chao.toyPlayingTicks()==0
        &&chao.getAffectionTicks()==0&&!chao.isInWater()&&chao.state().hunger<35
        &&(!chao.isBehaving()||chao.getBehavior()==Behavior.SHAKE_TREE)
        &&!ActivityManager.isBusy(chao)&&!CompanionContent.isManaged(chao);
  }
  @Override public boolean canUse() {
    if(!(chao.level() instanceof ServerLevel world)||!eligible()||world.getGameTime()<nextCheck)return false;
    nextCheck=world.getGameTime()+100+chao.getRandom().nextInt(101);tree=stand=null;
    double best=Double.MAX_VALUE;var center=chao.blockPosition();
    for(var mutable:BlockPos.betweenClosed(center.offset(-7,0,-7),center.offset(7,8,7))) {
      if(!world.hasChunkAt(mutable))continue;
      var state=world.getBlockState(mutable);
      if(!(state.getBlock() instanceof HangingFruitBlock)||state.getValue(HangingFruitBlock.AGE)!=3)continue;
      BlockPos trunk=trunkFor(world,mutable);if(trunk==null)continue;
      for(var side:Direction.Plane.HORIZONTAL) {
        var at=trunk.relative(side);
        if(!world.getBlockState(at.below()).isFaceSturdy(world,at.below(),Direction.UP)
            ||!world.getBlockState(at).getCollisionShape(world,at).isEmpty()
            ||!world.getFluidState(at).isEmpty())continue;
        double distance=chao.distanceToSqr(Vec3.atBottomCenterOf(at));if(distance>=best)continue;
        var path=chao.getNavigation().createPath(at,0);
        if(path!=null&&path.canReach()){best=distance;tree=trunk.immutable();stand=at.immutable();}
      }
    }
    return tree!=null;
  }
  private static BlockPos trunkFor(ServerLevel world,BlockPos pod) {
    BlockPos best=null;
    for(var p:BlockPos.betweenClosed(pod.offset(-3,-7,-3),pod.offset(3,0,3))) {
      if(!world.hasChunkAt(p)||!FruitContent.isTrunk(world.getBlockState(p)))continue;
      var at=p.immutable();while(FruitContent.isTrunk(world.getBlockState(at.below())))at=at.below();
      if(world.getBlockState(at.below()).isFaceSturdy(world,at.below(),Direction.UP)
          &&(best==null||at.getY()<best.getY()))best=at;
    }
    return best;
  }
  @Override public void start() {
    shaking=harvested=false;expires=chao.level().getGameTime()+400;navigate();
  }
  private void navigate() {
    var path=chao.getNavigation().createPath(stand,0);
    if(path!=null&&path.canReach())chao.getNavigation().moveTo(path,.85);
  }
  @Override public boolean canContinueToUse() {
    return tree!=null&&eligible()&&FruitContent.isTrunk(chao.level().getBlockState(tree))
        &&chao.level().getGameTime()<expires&&(!shaking||chao.getBehavior()==Behavior.SHAKE_TREE);
  }
  @Override public void tick() {
    if(tree==null)return;long now=chao.level().getGameTime();
    chao.getLookControl().setLookAt(tree.getX()+.5,tree.getY()+1.5,tree.getZ()+.5,30,25);
    if(!shaking) {
      if(chao.distanceToSqr(Vec3.atBottomCenterOf(stand))<.7) {
        shaking=true;shakeStarted=now;chao.startBehavior(Behavior.SHAKE_TREE,80);
      } else if(chao.getNavigation().isDone())navigate();
      return;
    }
    chao.getNavigation().stop();var world=(ServerLevel)chao.level();
    if(!harvested&&now-shakeStarted>=40) {
      harvested=true;
      for(var p:BlockPos.betweenClosed(tree.offset(-4,1,-4),tree.offset(4,8,4))) {
        if(!world.hasChunkAt(p))continue;var state=world.getBlockState(p);
        if(state.getBlock() instanceof HangingFruitBlock pod&&state.getValue(HangingFruitBlock.AGE)==3)
          pod.harvest(world,p.immutable(),state);
      }
      world.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK,FruitContent.LEAVES.get().defaultBlockState()),
          tree.getX()+.5,tree.getY()+4,tree.getZ()+.5,12,1.4,.7,1.4,.02);
      chao.playSound(SoundEvents.AZALEA_LEAVES_HIT,.5f,1.2f);
    }
  }
  @Override public void stop() {
    chao.getNavigation().stop();if(chao.getBehavior()==Behavior.SHAKE_TREE)chao.clearBehavior();
    tree=stand=null;nextCheck=chao.level().getGameTime()+200;
  }
  @Override public boolean requiresUpdateEveryTick(){return true;}
}
