package com.chaogarden.toys;
/** One deliberate forward ring every sixteen ticks, with a quiet backswing. */
public final class BellPlayRules {
  public static final String KIND="chao_bell";
  public static final int DURATION=ToyInteractionRules.SESSION_TICKS;
  private BellPlayRules(){}
  public static boolean soundDue(int tick){return tick>=24&&tick<=136&&(tick-24)%16==0;}
  public static boolean happy(float tick){return tick>=12&&tick<146;}
  public static float presence(float tick){return MaracasPlayRules.presence(tick);}
}
