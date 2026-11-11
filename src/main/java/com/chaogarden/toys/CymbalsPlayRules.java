package com.chaogarden.toys;
/** Four deliberate paired clashes on the shared eight-second performance clock. */
public final class CymbalsPlayRules {
 public static final String KIND="chao_cymbals";
 public static final int DURATION=ToyInteractionRules.SESSION_TICKS;
 private CymbalsPlayRules(){}
 public static boolean soundDue(int tick){return tick>=32&&tick<=128&&tick%32==0;}
 public static boolean happy(float tick){return tick>=12&&tick<146;}
 public static float presence(float tick){return MaracasPlayRules.presence(tick);}
}
