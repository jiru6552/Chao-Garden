package com.chaogarden.core;

/** Read-only specialization policy for the ordinary Neutral Power-first family.
 * Current level bars make gains, reductions and absolute Creative edits agree.
 * Adult first identity and biological maturity remain separate inputs.
 */
public final class NeutralPowerMorphology063 {
  /** Provisional threshold shared with the existing adult morphology profiles. */
  public static final float SPECIALIZATION_PROGRESS=960f;
  private NeutralPowerMorphology063() {}

  /** Both axes retain their complete signed ranges: Run(-)/Power(+) and
   * Swim(-)/Fly(+). Opposing training crosses balance without changing the
   * first Power identity; compatible axes share one corner regardless of order.
   */
  public record Sample(boolean enabled,float maturity,float runPower,float swimFly) {}

  public static Sample sample(ChaoState state) {
    if(state==null)return new Sample(false,0,0,0);
    float alignment=state.lockedAlignment();
    boolean ordinary=!state.dead&&!state.immortal&&"normal".equals(state.character)&&"normal".equals(state.variant);
    boolean enabled=ordinary&&state.adult&&state.lockedPrimaryType()==ChaoState.POWER_TYPE
        &&alignment>-.5f&&alignment<.5f;
    float rp=(HeroRunMorphology052.totalTraining(state,ChaoState.POWER)
        -HeroRunMorphology052.totalTraining(state,ChaoState.RUN))/SPECIALIZATION_PROGRESS;
    float sf=(HeroRunMorphology052.totalTraining(state,ChaoState.FLY)
        -HeroRunMorphology052.totalTraining(state,ChaoState.SWIM))/SPECIALIZATION_PROGRESS;
    return new Sample(enabled,unit(state.gradualVisualStrength()),axis(rp),axis(sf));
  }

  /** One common fresh Power-first surface plus the mature 3×3 signed-axis grid.
   * Maturity gates development independently of reversible training, which can
   * cross either Run/Power direction through balance while first type stays Power.
   * Earlier baby training carries forward; this method changes no saved state.
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
