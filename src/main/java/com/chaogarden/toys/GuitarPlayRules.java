package com.chaogarden.toys;
/** Sixteen alternating strums, shared by placed, learned, school and operator performances. */
public final class GuitarPlayRules {
 public static final String KIND="chao_guitar";
 private static final int[] CHORDS={0,5,7,5,0,2,-3,0};
 private GuitarPlayRules(){}
 public static boolean soundDue(int t){return t>=24&&t<=144&&(t-24)%8==0;}
 public static float pitch(int t){return (float)Math.pow(2,CHORDS[Math.clamp((t-24)/16,0,7)]/12.0);}
 public static float presence(float t){return MaracasPlayRules.presence(t);}
 public static boolean happy(float t){return t>=12&&t<146;}
}
