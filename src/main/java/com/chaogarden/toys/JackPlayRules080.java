package com.chaogarden.toys;

/** A single watch, surprise and recovery on the existing placed-toy clock.
 * Timing is authored for this mod; it is not an original-game timing claim. */
public final class JackPlayRules080 {
  public static final String KIND="chao_jack_in_box";
  public static final float POP_VOLUME=.22f,POP_PITCH=1.35f;
  private JackPlayRules080() {}
  public static float presence(float elapsed) {
    if(!Float.isFinite(elapsed)||elapsed<=0||elapsed>=ToyInteractionRules.SESSION_TICKS)return 0;
    if(elapsed<16)return smooth(elapsed/16);
    return elapsed<=140?1:smooth((ToyInteractionRules.SESSION_TICKS-elapsed)/20);
  }
  public static boolean seated(float elapsed){return Float.isFinite(elapsed)&&elapsed>=12&&elapsed<140;}
  public static boolean startled(float elapsed){return Float.isFinite(elapsed)&&elapsed>=ToyInteractionRules.JACK_POP_TICK&&elapsed<65;}
  public static boolean happy(float elapsed){return Float.isFinite(elapsed)&&elapsed>=84&&elapsed<112;}
  public static float reaction(float elapsed){return (float)ToyInteractionRules.jackReaction(elapsed);}
  public static double lookHeight(float elapsed) {
    if(!Float.isFinite(elapsed))return .40;
    float reveal=elapsed<40?0:elapsed<48?smooth((elapsed-40)/8):elapsed<=126?1:elapsed<144?smooth((144-elapsed)/18):0;
    return .40+.43*reveal;
  }
  public static float gazeYaw(float value){return Math.clamp(value,-12,12);}
  public static float gazePitch(float value){return Math.clamp(value,-12,12);}
  private static float smooth(float x){return x*x*(3-2*x);}
}
