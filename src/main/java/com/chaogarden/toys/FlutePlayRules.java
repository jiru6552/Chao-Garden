package com.chaogarden.toys;
/** Two four-note breath phrases on the shared eight-second performance clock. */
public final class FlutePlayRules {
 public static final String KIND="chao_flute";
 public static final int DURATION=ToyInteractionRules.SESSION_TICKS;
 private static final int[] NOTES={24,36,48,60,88,100,112,124};
 private static final int[] SEMITONES={0,2,4,7,4,2,0,-3};
 private FlutePlayRules(){}
 public static boolean soundDue(int tick){for(int n:NOTES)if(n==tick)return true;return false;}
 public static float pitch(int tick){for(int i=0;i<NOTES.length;i++)if(NOTES[i]==tick)return (float)Math.pow(2,SEMITONES[i]/12.0);return 1;}
 public static boolean blowing(float tick){return tick>=20&&tick<70||tick>=84&&tick<134;}
 public static boolean happy(float tick){return tick>=12&&tick<146;}
 public static float presence(float tick){return MaracasPlayRules.presence(tick);}
}
