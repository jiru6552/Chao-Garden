package com.chaogarden.toys;
/** Sixteen skin strikes on the shared eight-second performance clock. */
public final class DrumPlayRules {
 public static final String KIND="chao_drum";
 public static final int DURATION=ToyInteractionRules.SESSION_TICKS;
 private DrumPlayRules(){}
 public static boolean soundDue(int tick){return tick>=24&&tick<=144&&tick%8==0;}
 public static boolean happy(float tick){return tick>=12&&tick<146;}
 public static float presence(float tick){return MaracasPlayRules.presence(tick);}
}
