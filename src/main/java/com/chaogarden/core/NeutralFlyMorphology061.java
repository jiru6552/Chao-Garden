package com.chaogarden.core;

/** Read-only specialization policy for the ordinary Neutral Fly-first family.
 * Current level bars make drives, reductions and absolute Creative edits agree.
 * The first evolution identity and biological maturity remain separate inputs.
 */
public final class NeutralFlyMorphology061 {
  /** Provisional threshold shared with the existing adult morphology prototypes. */
  public static final float SPECIALIZATION_PROGRESS=960f;
  private NeutralFlyMorphology061() {}

  /** Negative Run/Power means Run; positive means Power.
   * Swim/Fly starts at zero: excess Swim restores the balanced Fly-first form,
   * rather than selecting an unimplemented Swim specialization or changing type.
   */
  public record Sample(boolean enabled,float maturity,float runPower,float swimFly) {}

  public static Sample sample(ChaoState state) {
    if(state==null)return new Sample(false,0,0,0);
    float alignment=state.lockedAlignment();
    boolean ordinary=!state.dead&&!state.immortal&&"normal".equals(state.character)&&"normal".equals(state.variant);
    boolean enabled=ordinary&&state.adult&&state.lockedPrimaryType()==ChaoState.FLY_TYPE
        &&alignment>-.5f&&alignment<.5f;
    float rp=(HeroRunMorphology052.totalTraining(state,ChaoState.POWER)
        -HeroRunMorphology052.totalTraining(state,ChaoState.RUN))/SPECIALIZATION_PROGRESS;
    float sf=(HeroRunMorphology052.totalTraining(state,ChaoState.FLY)
        -HeroRunMorphology052.totalTraining(state,ChaoState.SWIM))/SPECIALIZATION_PROGRESS;
    return new Sample(enabled,unit(state.gradualVisualStrength()),axis(rp),flyAxis(sf));
  }

  /** Fresh Fly-first surface plus the existing rigid-box 3×3 target layout.
   * Only the balanced and positive-Fly rows contribute. The unused negative-Swim
   * row in this asset duplicates balanced targets as an authoring guard.
   * Existing training carries forward; maturity alone gates a fresh adult's
   * progression. Compatible pairs have one shared corner, independent of order.
   */
  public static float[] weights(Sample sample) {
    float[] weights=new float[10];
    if(sample==null||!sample.enabled()){weights[0]=1;return weights;}
    float age=unit(sample.maturity()),rp=axis(sample.runPower()),sf=flyAxis(sample.swimFly());
    weights[0]=1-age;
    for(int y=0;y<3;y++)for(int x=0;x<3;x++)
      weights[1+y*3+x]=age*Math.max(0,1-Math.abs(rp-(x-1)))*Math.max(0,1-Math.abs(sf-(y-1)));
    return weights;
  }

  private static float unit(float value){return Float.isFinite(value)?Math.clamp(value,0,1):0;}
  private static float axis(float value){return Float.isFinite(value)?Math.clamp(value,-1,1):0;}
  private static float flyAxis(float value){return Float.isFinite(value)?Math.clamp(value,0,1):0;}
}
