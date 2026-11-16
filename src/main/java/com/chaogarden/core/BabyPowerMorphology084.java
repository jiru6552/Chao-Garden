package com.chaogarden.core;

/** Read-only Power development on the existing ordinary baby rig. */
public final class BabyPowerMorphology084 {
  /** Same level-bar units and provisional baby range as the approved Run path. */
  public static final float POWER_PROGRESS=HeroRunMorphology052.BABY_RUN_PROGRESS;
  private BabyPowerMorphology084() {}

  public record Sample(boolean enabled,float training,float maturity,float growth) {}

  public static Sample sample(ChaoState state) {
    if(state==null||state.adult||state.dead||state.immortal
        ||!"normal".equals(state.character)||!"normal".equals(state.variant))
      return new Sample(false,0,0,0);
    float net=HeroRunMorphology052.totalTraining(state,ChaoState.POWER)
        -HeroRunMorphology052.totalTraining(state,ChaoState.RUN);
    float training=unit(net/POWER_PROGRESS),maturity=unit(state.evolutionStrength());
    return new Sample(true,training,maturity,training*maturity);
  }

  private static float unit(float value) {
    return Float.isFinite(value)?Math.clamp(value,0,1):0;
  }
}
