package com.chaogarden.companion;

import com.chaogarden.core.ChaoState;
import com.chaogarden.core.LocomotionRules;

/** Presentation depends on earned movement unlocks, never the model or stat level. */
public final class CarryRules {
  public static final int FREE=0, HEAD=1, BACK=2;
  private CarryRules() {}
  public static int position(ChaoState state, boolean flying, boolean water) {
    if (flying) return BACK;
    if (water) return !state.adult || !LocomotionRules.canSwim(state) ? BACK : FREE;
    return !state.adult || !LocomotionRules.canRun(state) ? HEAD : FREE;
  }
}
