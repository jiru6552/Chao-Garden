package com.chaogarden.toys;

/** One eight-second push/pull phrase, shared by real sessions and operator previews. */
public final class AccordionPlayRules {
  public static final String KIND="chao_accordion";
  private static final int[] NOTES={0,2,4,7,5,4,2,0};
  private AccordionPlayRules(){}
  public static boolean soundDue(int tick){return tick>=24&&tick<=136&&(tick-24)%16==0;}
  public static float pitch(int tick){return (float)Math.pow(2,NOTES[Math.clamp((tick-24)/16,0,7)]/12.0);}
  public static float presence(float tick){return MaracasPlayRules.presence(tick);}
  public static boolean happy(float tick){return tick>=12&&tick<148;}
}
