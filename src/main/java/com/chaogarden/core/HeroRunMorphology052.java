package com.chaogarden.core;

/**
 * Read-only policy for the ordinary Hero Run prototype. Training is measured
 * from the current earned level bars, so every gain and loss uses one rule,
 * regardless of the item, activity or Creative tool which changed the stats.
 */
public final class HeroRunMorphology052 {
  /** Provisional equivalents of twenty/forty ordinary 24-progress drives. */
  public static final float BABY_RUN_PROGRESS=480f,ADULT_RUN_PROGRESS=960f;
  private HeroRunMorphology052() {}

  /** Biological maturity and the reversible training target are separate. */
  public record Sample(boolean enabled,boolean adult,float babyHero,float babyRun,
                       float specialization,float maturity,float adultGrowth) {}

  public static int totalTraining(ChaoState state,int stat) {
    if(state==null||stat<0||stat>=5)return 0;
    return (int)Math.clamp((long)state.levels[stat]*100+state.progress[stat],0,9900);
  }

  public static Sample sample(ChaoState state) {
    if(state==null)return new Sample(false,false,0,0,0,0,0);
    float alignment=state.lockedAlignment();
    float net=totalTraining(state,ChaoState.RUN)-totalTraining(state,ChaoState.POWER);
    float hero=unit(alignment/.5f),babyRun=unit(net/BABY_RUN_PROGRESS);
    float target=unit(net/ADULT_RUN_PROGRESS);
    float maturity=state.adult?state.gradualVisualStrength():unit(state.evolutionStrength());
    boolean ordinary=!state.dead&&!state.immortal&&"normal".equals(state.character)&&"normal".equals(state.variant);
    // Do not gate babies by a discrete prospective type: crossing an earned-
    // point tie must not switch their continuous developing surface on/off.
    boolean enabled=ordinary&&(!state.adult
        ||alignment>=.5f&&state.lockedPrimaryType()==ChaoState.RUN_TYPE);
    return new Sample(enabled,state.adult,hero,babyRun,target,maturity,state.adult?target*maturity:0);
  }

  private static float unit(float value) {
    return Float.isFinite(value)?Math.clamp(value,0,1):0;
  }
}
