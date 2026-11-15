package com.chaogarden.toys;

/** Quiet authored watching on the existing160-tick placed-toy clock.
 * These timings are a Minecraft interpretation, not measured original-game timings. */
public final class TvWatchRules073 {
  public static final String KIND="chao_tv";
  public static final int SETTLE_TICKS=20, RECOVER_TICK=140, SOUND_TICK=20;
  /** Measured center of the shipped OBJ screen faces; orientation is the north-facing block model. */
  public static final double SCREEN_HEIGHT=.43533915, SCREEN_FORWARD=.2174175, SCREEN_RIGHT=.0529181;
  private TvWatchRules073() {}

  public static float presence(float elapsed) {
    if(!Float.isFinite(elapsed)||elapsed<=0||elapsed>=ToyInteractionRules.SESSION_TICKS)return 0;
    if(elapsed<SETTLE_TICKS)return smooth(elapsed/SETTLE_TICKS);
    if(elapsed<=RECOVER_TICK)return 1;
    return smooth((ToyInteractionRules.SESSION_TICKS-elapsed)/20f);
  }
  public static boolean seated(float elapsed){return Float.isFinite(elapsed)&&elapsed>=12&&elapsed<RECOVER_TICK;}
  /** One slow sequence, rather than endlessly cycling and chiming every16ticks. */
  public static int screenFrame(int elapsed) {
    if(elapsed<SOUND_TICK||elapsed>=RECOVER_TICK)return 0;
    if(elapsed<60)return 1;
    return elapsed<100?2:3;
  }
  /** A single quiet startup cue; late block ticks cannot produce a cue loop. */
  public static boolean startCue(int previousFrame,int nextFrame,int elapsed) {
    return previousFrame==0&&nextFrame==1&&elapsed>=SOUND_TICK&&elapsed<60;
  }
  public static boolean happy(float elapsed){return elapsed>=66&&elapsed<82;}
  public static float headYaw(float elapsed) {
    return .035f*attention(elapsed,42,74)-.030f*attention(elapsed,96,128);
  }
  public static float headPitch(float elapsed){return (-.035f+.020f*attention(elapsed,66,90))*presence(elapsed);}
  public static float headRoll(float elapsed){return .012f*attention(elapsed,66,90);}
  private static float attention(float elapsed,float start,float end) {
    if(!Float.isFinite(elapsed)||elapsed<=start||elapsed>=end)return 0;
    return (float)Math.pow(Math.sin(Math.PI*(elapsed-start)/(end-start)),2);
  }
  private static float smooth(float x){return x*x*(3-2*x);}
}
