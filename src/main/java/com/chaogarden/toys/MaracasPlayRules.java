package com.chaogarden.toys;
/** Shared placed-maracas session clock. The existing learned-maracas gate still applies. */
public final class MaracasPlayRules {
  public static final String KIND="chao_maracas";
  public static final int DURATION=160, RAISE_END=12, SHAKE_START=20, SHAKE_END=140, LOWER_START=146;
  private MaracasPlayRules(){}
  public static boolean soundDue(int tick){return tick>=SHAKE_START&&tick<=SHAKE_END&&(tick-SHAKE_START)%4==0;}
  public static boolean happy(float tick){return tick>=RAISE_END&&tick<LOWER_START;}
  public static float presence(float tick){return Float.isFinite(tick)?Math.max(0,Math.min(1,Math.min(tick/RAISE_END,(DURATION-tick)/(DURATION-LOWER_START)))):0;}
}
