package com.chaogarden.toys;

/** Three deliberate blows, with a pause to lower/reposition the wand between them. */
public final class BubblePlayRules {
  public static final String KIND="chao_bubble_wand";
  private BubblePlayRules() {}
  public static boolean puffDue(int elapsed) {
    return elapsed>=30&&elapsed<=138&&(elapsed-30)%48<=12&&(elapsed-30)%4==0;
  }
  public static boolean blowing(float elapsed) {
    return elapsed>=26&&elapsed<144&&(elapsed-26)%48<22;
  }
  public static float presence(float elapsed){return RattlePlayRules.presence(elapsed);}
  public static boolean handToy(String kind){return RattlePlayRules.KIND.equals(kind)||KIND.equals(kind)||MaracasPlayRules.KIND.equals(kind)||BellPlayRules.KIND.equals(kind)||TambourinePlayRules.KIND.equals(kind)||CastanetsPlayRules.KIND.equals(kind)||CymbalsPlayRules.KIND.equals(kind)||DrumPlayRules.KIND.equals(kind)||FlutePlayRules.KIND.equals(kind)||TrianglePlayRules.KIND.equals(kind)||TrumpetPlayRules.KIND.equals(kind)||GuitarPlayRules.KIND.equals(kind);}
}
