package com.chaogarden.toys;

/** Finite listening and a quiet authored note phrase on the ordinary160-tick toy clock.
 * These timings are a Minecraft adaptation, not measured original-game choreography. */
public final class RadioPlayRules075 {
  public static final String KIND="chao_radio";
  public static final int SETTLE_TICKS=20, RECOVER_TICK=140, EXCLUSIVE_TICKS=32;
  public static final double LISTEN_DISTANCE=.60, AUDIO_RADIUS=12;
  /** Averaged centers of the actual textured front speakers in the shipped north-facing OBJ. */
  public static final double SPEAKER_HEIGHT=.128193231, SPEAKER_FORWARD=.1095644955;
  public static final float VOLUME=.06f;
  private RadioPlayRules075() {}

  public static float presence(float elapsed) {
    if(!Float.isFinite(elapsed)||elapsed<=0||elapsed>=ToyInteractionRules.SESSION_TICKS)return 0;
    if(elapsed<SETTLE_TICKS)return smooth(elapsed/SETTLE_TICKS);
    if(elapsed<=RECOVER_TICK)return 1;
    return smooth((ToyInteractionRules.SESSION_TICKS-elapsed)/20f);
  }
  public static boolean seated(float elapsed){return Float.isFinite(elapsed)&&elapsed>=12&&elapsed<RECOVER_TICK;}
  public static boolean happy(float elapsed){return elapsed>=44&&elapsed<60||elapsed>=84&&elapsed<100;}
  /** Four separate notes, then a quiet recovery. Missed ticks do not play a catch-up burst. */
  public static int cueIndex(int elapsed){return switch(elapsed){case 20->0;case 52->1;case 84->2;case 116->3;default->-1;};}
  public static float cuePitch(int index){return switch(index){case 0->1f;case 1,3->1.122462f;case 2->1.259921f;default->0f;};}
  public static int frame(int elapsed){return elapsed<20||elapsed>=140?0:elapsed<60?1:elapsed<100?2:3;}
  public static float headYaw(float elapsed){return (.030f*attention(elapsed,38,70)-.030f*attention(elapsed,90,122))*presence(elapsed);}
  public static float headPitch(float elapsed){return .060f*beat(elapsed);}
  public static float headRoll(float elapsed){return .030f*beat(elapsed);}
  public static float armPulse(float elapsed){return .018f*beat(elapsed);}
  private static float beat(float elapsed){return Float.isFinite(elapsed)?(float)Math.sin((elapsed-20)*Math.PI/16)*presence(elapsed):0;}
  private static float attention(float elapsed,float start,float end){
    if(!Float.isFinite(elapsed)||elapsed<=start||elapsed>=end)return 0;
    return (float)Math.pow(Math.sin(Math.PI*(elapsed-start)/(end-start)),2);
  }
  private static float smooth(float x){return x*x*(3-2*x);}
}
