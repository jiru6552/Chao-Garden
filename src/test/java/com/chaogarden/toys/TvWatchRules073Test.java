package com.chaogarden.toys;

/** Timing invariants independent of Minecraft renderer or entity lifetime. */
public final class TvWatchRules073Test {
  private static int checks;
  private static void check(boolean value,String why){checks++;if(!value)throw new AssertionError(why);}
  public static void main(String[] args) {
    checks=0;
    check(ToyInteractionRules.SESSION_TICKS==160,"Keep the established voluntary session and reward deadline");
    int cues=0,changes=0,prior=TvWatchRules073.screenFrame(0);
    for(int tick=0;tick<=160;tick++){
      int frame=TvWatchRules073.screenFrame(tick);
      check(frame>=0&&frame<=3,"Use only the four existing placed-TV model frames");
      if(frame!=prior){changes++;if(TvWatchRules073.startCue(prior,frame,tick))cues++;}
      prior=frame;
    }
    check(cues==1&&changes==4,"One quiet startup cue and one finite program cycle, without repeating chimes");
    check(TvWatchRules073.screenFrame(-1)==0&&TvWatchRules073.screenFrame(160)==0,"Quiet screen timeline outside the session");
    check(!TvWatchRules073.startCue(0,1,100)&&!TvWatchRules073.startCue(3,0,140),"Late resumed ticks and recovery never retrigger audio");
    for(int n=-80;n<=720;n++){
      float t=n*.25f,w=TvWatchRules073.presence(t);
      check(Float.isFinite(w)&&w>=0&&w<=1,"Bounded entry and recovery");
      check(Math.abs(TvWatchRules073.headYaw(t))<=.035001f,"Small slow gaze, not an endlessly bobbing dance");
      check(Math.abs(TvWatchRules073.headPitch(t))<=.035001f&&Math.abs(TvWatchRules073.headRoll(t))<=.012001f,"Bounded head articulation");
      if(n>-80){
        check(Math.abs(w-TvWatchRules073.presence(t-.25f))<.019,"Continuous quarter-tick entry and exit");
        check(Math.abs(TvWatchRules073.headYaw(t)-TvWatchRules073.headYaw(t-.25f))<.002,"Gaze smoothly eases to and from screen attention");
      }
      if(t<=0||t>=160)check(w==0&&!TvWatchRules073.seated(t)&&!TvWatchRules073.happy(t),"No pose or expression leakage after stop");
    }
    for(float t:new float[]{Float.NaN,Float.NEGATIVE_INFINITY,Float.POSITIVE_INFINITY}){
      check(TvWatchRules073.presence(t)==0&&TvWatchRules073.headYaw(t)==0&&TvWatchRules073.headPitch(t)==0&&TvWatchRules073.headRoll(t)==0,"Invalid phase safely declines presentation");
      check(!TvWatchRules073.seated(t)&&!TvWatchRules073.happy(t),"Invalid phase has no seated/expression window");
    }
    System.out.println("TV watch rules073: "+checks+" checks passed");
  }
}
