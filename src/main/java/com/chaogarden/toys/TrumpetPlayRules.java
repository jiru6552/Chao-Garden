package com.chaogarden.toys;
/** Two four-note breath phrases on the shared eight-second performance clock. */
public final class TrumpetPlayRules {
 public static final String KIND="chao_trumpet";
 public static final int DURATION=ToyInteractionRules.SESSION_TICKS;
 private static final int[] NOTES={24,32,44,60,88,96,108,124};
 private static final int[] SEMITONES={4,4,7,11,9,7,4,2};
 public static final double PITCH_OFFSET_SEMITONES=.5;
 private TrumpetPlayRules(){}
 public static boolean soundDue(int tick){for(int n:NOTES)if(n==tick)return true;return false;}
 public static float pitch(int tick){for(int i=0;i<NOTES.length;i++)if(NOTES[i]==tick)return (float)Math.pow(2,(SEMITONES[i]+PITCH_OFFSET_SEMITONES)/12.0);return 1;}
 public static boolean blowing(float tick){return tick>=20&&tick<70||tick>=84&&tick<134;}
 public static boolean happy(float tick){return tick>=12&&tick<146;}
 public static float presence(float tick){return MaracasPlayRules.presence(tick);}
}
