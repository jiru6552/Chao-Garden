package com.chaogarden.interaction;

import com.chaogarden.entity.ChaoEntity;
import com.chaogarden.item.CareItem;
import java.util.Comparator;
import java.util.EnumSet;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.item.ItemEntity;

/** Curious garden Chao approach one available drop, then hold and consider it. */
public final class ChaoPickupItemGoal extends Goal {
  private final ChaoEntity chao;
  private ItemEntity target;
  private long nextSearch;
  private int approachingTicks;
  public ChaoPickupItemGoal(ChaoEntity chao) {
    this.chao=chao;setFlags(EnumSet.of(Flag.MOVE,Flag.LOOK));
  }
  @Override public boolean canUse() {
    if (!chao.canInspectNearbyItems() || chao.level().getGameTime()<nextSearch) return false;
    nextSearch=chao.level().getGameTime()+20;
    target=chao.level().getEntitiesOfClass(ItemEntity.class,chao.getBoundingBox().inflate(4),
        chao::mayInspectDroppedItem).stream()
        .filter(item->chao.hasLineOfSight(item))
        .min(Comparator.<ItemEntity>comparingInt(item->chao.state().hunger<35
            &&item.getItem().getItem() instanceof CareItem care&&care.kind==CareItem.Kind.FRUIT
            &&com.chaogarden.fruit.FruitEffects.canUse(chao.state(),care.effect)
            &&(!care.effect.equals("heart_fruit")||chao.mayBreed())?0:1)
            .thenComparingDouble(chao::distanceToSqr)).orElse(null);
    return target!=null;
  }
  @Override public boolean canContinueToUse() {
    return approachingTicks<200 && chao.canInspectNearbyItems()
        && chao.mayInspectDroppedItem(target) && chao.distanceToSqr(target)<36;
  }
  @Override public void start() { approachingTicks=0; }
  @Override public void tick() {
    approachingTicks++;
    if (target==null) return;
    chao.getLookControl().setLookAt(target,25,25);
    // Let the actual transaction enforce its reach. A second, smaller radius
    // can strand a drop at the far corner of an already-reached path cell.
    if (chao.hasLineOfSight(target)&&chao.pickUpForInspection(target)) {
      chao.getNavigation().stop();
    } else if(chao.getNavigation().isDone()||approachingTicks%10==0) {
      // Entity-target navigation accepts a one-cell reach margin. That leaves
      // a small Chao staring at fruit just outside its real pickup radius.
      // Test and follow the actual item cell instead, updating for its motion.
      var path=chao.getNavigation().createPath(target.blockPosition(),0);
      if(path!=null&&path.canReach())chao.getNavigation().moveTo(path,.75);
    }
  }
  @Override public void stop() { target=null;chao.getNavigation().stop(); }
}
