package com.chaogarden.core;

/** Read-only specialization policy for the ordinary Hero Power-first family.
 * Current bars make gains, reductions and absolute Creative edits agree.
 * Locked first identity and biological maturity are independent inputs.
 */
public final class HeroPowerMorphology067 {
  /** Provisional threshold shared with the existing adult morphology profiles. */
  public static final float SPECIALIZATION_PROGRESS=960f;
  private HeroPowerMorphology067() {}

  /** Run(-)/Power(+) and Swim(-)/Fly(+) retain both signed directions.
   * Later opposing Run may cross balance without replacing the Power first type.
   * One tendency on each axis combines into one shared corner in either order.
   */
  public record Sample(boolean enabled,float maturity,float runPower,float swimFly) {}

  public static Sample sample(ChaoState state) {
    if(state==null)return new Sample(false,0,0,0);
    float alignment=state.lockedAlignment();
    boolean ordinary=!state.dead&&!state.immortal&&"normal".equals(state.character)&&"normal".equals(state.variant);
    boolean enabled=ordinary&&state.adult&&state.lockedPrimaryType()==ChaoState.POWER_TYPE
        &&alignment>=.5f;
    float rp=(HeroRunMorphology052.totalTraining(state,ChaoState.POWER)
        -HeroRunMorphology052.totalTraining(state,ChaoState.RUN))/SPECIALIZATION_PROGRESS;
    float sf=(HeroRunMorphology052.totalTraining(state,ChaoState.FLY)
        -HeroRunMorphology052.totalTraining(state,ChaoState.SWIM))/SPECIALIZATION_PROGRESS;
    return new Sample(enabled,unit(state.gradualVisualStrength()),axis(rp),axis(sf));
  }

  /** Common fresh Hero Power surface plus the mature 3×3 signed-axis grid.
   * Biological maturity gates development separately from reversible training.
   * Carried baby training changes the target, never the age or saved first type.
   * This sampler changes no saved data; the approved baby route stays separate.
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
