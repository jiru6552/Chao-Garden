package com.chaogarden.toys;
/** One palm tap per sixteen ticks; all modes share this authored beat. */
public final class TambourinePlayRules {
  public static final String KIND="chao_tambourine";
  public static final int DURATION=ToyInteractionRules.SESSION_TICKS;
  private TambourinePlayRules(){}
  public static boolean soundDue(int tick){return tick>=24&&tick<=136&&(tick-24)%16==0;}
  public static boolean happy(float tick){return tick>=12&&tick<146;}
  public static float presence(float tick){return MaracasPlayRules.presence(tick);}
}
