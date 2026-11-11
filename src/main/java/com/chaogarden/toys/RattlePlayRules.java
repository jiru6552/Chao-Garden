package com.chaogarden.toys;

import com.chaogarden.core.ChaoState;

/** One session clock for the Lionfish arm, expression, prop and server shake sound. */
public final class RattlePlayRules {
  public static final String KIND="chao_rattle";
  public static final int DURATION=ToyInteractionRules.SESSION_TICKS;
  public static final int RAISE_END=10, SHAKE_START=20, SHAKE_END=132, SHAKE_STEP=4, LOWER_START=146;
  private RattlePlayRules() {}
  public static boolean handheld(ChaoState state) {
    // All completed forms have a real arm tip or replacement hand. During
    // Omochao assembly the arms are deliberately absent until stage three.
    int construction=state.intTrait("content.construction");
    return !"omochao".equals(state.character)||construction==0||construction>=3;
  }
  public static boolean soundDue(int elapsed) {
    return elapsed>=SHAKE_START&&elapsed<=SHAKE_END&&(elapsed-SHAKE_START)%SHAKE_STEP==0;
  }
  public static boolean happy(float elapsed) {return elapsed>=RAISE_END&&elapsed<LOWER_START;}
  public static float presence(float elapsed) {
    if(!Float.isFinite(elapsed))return 0;
    return Math.max(0,Math.min(1,Math.min(elapsed/RAISE_END,(DURATION-elapsed)/(DURATION-LOWER_START))));
  }
}
