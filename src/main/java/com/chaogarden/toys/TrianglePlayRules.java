package com.chaogarden.toys;
/** Eight measured triangle taps with a short middle rest on the shared clock. */
public final class TrianglePlayRules {
 public static final String KIND="chao_triangle";
 public static final int DURATION=ToyInteractionRules.SESSION_TICKS;
 private TrianglePlayRules(){}
 public static boolean soundDue(int tick){return tick==24||tick==40||tick==56||tick==72||tick==96||tick==112||tick==128||tick==144;}
 public static boolean happy(float tick){return tick>=12&&tick<146;}
 public static float presence(float tick){return MaracasPlayRules.presence(tick);}
}
