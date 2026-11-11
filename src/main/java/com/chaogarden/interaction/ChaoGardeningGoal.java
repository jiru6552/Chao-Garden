package com.chaogarden.interaction;

import com.chaogarden.activities.ActivityManager;
import com.chaogarden.core.ChaoAnimationRules;
import com.chaogarden.companion.CompanionContent;
import com.chaogarden.entity.ChaoEntity;
import com.chaogarden.entity.ChaoEntity.Behavior;
import com.chaogarden.entity.ChaoEntity.ItemAction;
import com.chaogarden.fruit.*;
import java.util.EnumSet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.phys.Vec3;

/** Walk to reachable soil, keep the real seed until planting succeeds, and water
 * only a nearby real tree. Neither navigation nor animation can create items. */
public final class ChaoGardeningGoal extends Goal {
  private final ChaoEntity chao;
  private BlockPos target,stand;
  private boolean planting,working,committed;
  private long nextWater,expires,started;
  public ChaoGardeningGoal(ChaoEntity chao){this.chao=chao;setFlags(EnumSet.of(Flag.MOVE,Flag.LOOK,Flag.JUMP));}
  private boolean safe() {
    return chao.isAlive()&&!chao.isNoAi()&&!chao.isStaying()&&!chao.isCocooned()
        &&!chao.isResting()&&!chao.isInWater()&&chao.toyPlayingTicks()==0
        &&chao.getAffectionTicks()==0&&!ActivityManager.isBusy(chao)&&!CompanionContent.isManaged(chao)
        &&!com.chaogarden.care.ChaoCareServices.hasLesson(chao)
        &&(!chao.isBehaving()||chao.getBehavior()==Behavior.WATER);
  }
  @Override public boolean canUse() {
    if(!(chao.level() instanceof ServerLevel level)||!safe()||!chao.onGround())return false;
    planting=chao.getItemAction()==ItemAction.GARDEN;
    if(!planting&&(chao.isHandlingItem()||level.getGameTime()<nextWater
        ||chao.state().intTrait("reward.watering_can")==0||chao.state().hunger<35||chao.state().energy<30))return false;
    if(!planting)nextWater=level.getGameTime()+60;
    String fruit=chao.getMainHandItem().getItem() instanceof FruitSeedItem s?s.fruit:"";
    if(planting&&(fruit.isEmpty()||!GardenTools.canPlant(chao,fruit))){chao.cancelPlanting();return false;}
    target=stand=null;double best=Double.MAX_VALUE;BlockPos center=chao.blockPosition();
    for(BlockPos mutable:BlockPos.betweenClosed(center.offset(-5,-1,-5),center.offset(5,1,5))) {
      BlockPos at=mutable.immutable();if(!level.isAreaLoaded(at,3))continue;
      if(planting) {if(!GardenTools.plantingSpot(level,at,fruit))continue;}
      else {
        BlockPos root=GardenTools.root(level,at);
        if(root==null||!root.equals(at)||!GardenTreeData.get(level).mayWater(root,level.getGameTime()))continue;
      }
      for(Direction side:Direction.Plane.HORIZONTAL) {
        BlockPos near=at.relative(side);
        if(!level.getBlockState(near).isAir()||!level.getFluidState(near).isEmpty()
            ||!level.getBlockState(near.below()).isFaceSturdy(level,near.below(),Direction.UP))continue;
        double distance=chao.distanceToSqr(Vec3.atBottomCenterOf(near));if(distance>=best)continue;
        var path=chao.getNavigation().createPath(near,0);
        if(path!=null&&path.canReach()){best=distance;target=at;stand=near;}
      }
    }
    if(target==null&&planting)chao.cancelPlanting();
    if(target!=null&&!planting)nextWater=level.getGameTime()+600+chao.getRandom().nextInt(601);
    return target!=null;
  }
  @Override public void start(){working=committed=false;expires=chao.level().getGameTime()+190;navigate();}
  // Coordinate moveTo defaults to a one-block path tolerance, which stops
  // outside the work radius. Use the same exact target as the reachability probe.
  private void navigate(){if(stand!=null)chao.getNavigation().moveTo(chao.getNavigation().createPath(stand,0),.8);}
  @Override public boolean canContinueToUse() {
    return target!=null&&safe()&&chao.level().getGameTime()<expires&&(!committed||!planting)
        &&(planting?(chao.getItemAction()==ItemAction.GARDEN||chao.getItemAction()==ItemAction.PLANT):!chao.isHandlingItem());
  }
  @Override public void tick() {
    if(target==null)return;long now=chao.level().getGameTime();
    chao.getLookControl().setLookAt(target.getX()+.5,target.getY()+.5,target.getZ()+.5,30,30);
    if(!working) {
      if(chao.distanceToSqr(Vec3.atBottomCenterOf(stand))<.8) {
        working=true;started=now;expires=now+ChaoAnimationRules.WATER_TICKS;chao.getNavigation().stop();
        if(planting)chao.beginPlanting();else chao.startBehavior(Behavior.WATER,ChaoAnimationRules.WATER_TICKS);
      } else if(chao.getNavigation().isDone())navigate();
      return;
    }
    chao.getNavigation().stop();
    // A shove or teleport during the gesture must not water a remote plant.
    if(chao.distanceToSqr(Vec3.atBottomCenterOf(target))>9) {
      if(planting)chao.cancelPlanting();else chao.clearBehavior();
      expires=now;return;
    }
    if(!committed&&now-started>=ChaoAnimationRules.WATER_COMMIT_TICK) {
      if(planting) {if(!chao.finishPlanting(target))chao.cancelPlanting();}
      else GardenTools.water((ServerLevel)chao.level(),target);
      committed=true;
    }
  }
  @Override public void stop() {
    chao.getNavigation().stop();if(planting&&!committed)chao.cancelPlanting();
    if(chao.getBehavior()==Behavior.WATER)chao.clearBehavior();target=stand=null;
  }
  @Override public boolean requiresUpdateEveryTick(){return true;}
}
