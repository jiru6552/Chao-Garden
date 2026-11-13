package com.chaogarden.core;

/** Read-only first-pass Dark/Normal adult specialization policy.
 * Training is deliberately measured from current bars, so drives, losses and
 * Creative edits agree. This provisional threshold matches the Hero Run pass.
 */
public final class DarkNormalMorphology060 {
  public static final float SPECIALIZATION_PROGRESS=960f;
  private DarkNormalMorphology060() {}

  /** Signed axes use the existing convention: negative Run/Swim, positive Power/Fly. */
  public record Sample(boolean enabled,float maturity,float runPower,float swimFly) {}

  public static Sample sample(ChaoState state) {
    if(state==null)return new Sample(false,0,0,0);
    float alignment=state.lockedAlignment();
    boolean ordinary=!state.dead&&!state.immortal&&"normal".equals(state.character)&&"normal".equals(state.variant);
    boolean enabled=ordinary&&state.adult&&state.lockedPrimaryType()==ChaoState.NORMAL
        &&alignment<=-.5f;
    float rp=(HeroRunMorphology052.totalTraining(state,ChaoState.POWER)
        -HeroRunMorphology052.totalTraining(state,ChaoState.RUN))/SPECIALIZATION_PROGRESS;
    float sf=(HeroRunMorphology052.totalTraining(state,ChaoState.FLY)
        -HeroRunMorphology052.totalTraining(state,ChaoState.SWIM))/SPECIALIZATION_PROGRESS;
    return new Sample(enabled,unit(state.gradualVisualStrength()),axis(rp),axis(sf));
  }

  /** Fresh first form plus a complete two-opposing-axis mature 3×3 lattice.
   * Compatible pairs share one corner; swapping training order cannot create
   * a different form. Nonnegative partition weights avoid overshoot/swaps.
   */
  public static float[] weights(Sample sample) {
    float[] weights=new float[10];
    if(sample==null||!sample.enabled()){weights[0]=1;return weights;}
    float age=unit(sample.maturity()),rp=axis(sample.runPower()),sf=axis(sample.swimFly());
    weights[0]=1-age;
    for(int y=0;y<3;y++)for(int x=0;x<3;x++)
      weights[1+y*3+x]=age*Math.max(0,1-Math.abs(rp-(x-1)))*Math.max(0,1-Math.abs(sf-(y-1)));
    return weights;
  }

  private static float unit(float value){return Float.isFinite(value)?Math.clamp(value,0,1):0;}
  private static float axis(float value){return Float.isFinite(value)?Math.clamp(value,-1,1):0;}
}
