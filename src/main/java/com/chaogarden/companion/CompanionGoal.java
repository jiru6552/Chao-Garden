package com.chaogarden.companion;

import com.chaogarden.entity.ChaoEntity;
import java.util.EnumSet;
import net.minecraft.world.entity.ai.goal.Goal;

/** Gives a summoned companion exclusive navigation without disabling vanilla pathfinding. */
public final class CompanionGoal extends Goal {
  private final ChaoEntity chao;

  public CompanionGoal(ChaoEntity chao) {
    this.chao = chao;
    // FloatGoal may keep the companion above water at the same time.
    setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
  }

  @Override public boolean canUse() { return CompanionContent.isManaged(chao); }
  @Override public boolean canContinueToUse() { return canUse(); }
  @Override public void start() { chao.getNavigation().stop(); }
  @Override public void stop() { chao.getNavigation().stop(); chao.setTarget(null); }
  @Override public void tick() { CompanionContent.drive(chao); }
}
