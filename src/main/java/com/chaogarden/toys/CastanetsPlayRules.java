package com.chaogarden.toys;
/** Fifteen shell contacts on the shared eight-second performance clock. */
public final class CastanetsPlayRules {
  public static final String KIND="chao_castanets";
  public static final int DURATION=ToyInteractionRules.SESSION_TICKS;
  private CastanetsPlayRules(){}
  public static boolean soundDue(int tick){return tick>=24&&tick<=136&&(tick-24)%8==0;}
  public static boolean happy(float tick){return tick>=12&&tick<146;}
  public static float presence(float tick){return MaracasPlayRules.presence(tick);}
}
