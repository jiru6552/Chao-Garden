package com.chaogarden.core;

import java.util.List;
import java.util.random.RandomGenerator;

/**
 * Read-only choices and production timing for short, optional garden gestures.
 * Weights are Minecraft adaptations of the existing saved personality sliders
 * and emotions. No idle grants care benefits or changes expressed personality.
 */
public final class ChaoIdleRules078 {
  public static final List<String> IDS=List.of("look_around","look_up","think","bashful","chuckle","tired","ground_play");
  private ChaoIdleRules078() {}

  public static boolean valid(String id){return id!=null&&IDS.contains(id);}
  public static int duration(String id) {
    if(id==null)return 0;
    return switch(id) {
      case "look_around"->72;case "look_up"->64;case "think"->80;
      case "bashful","chuckle"->68;case "tired"->84;case "ground_play"->104;
      default->0;
    };
  }
  public static boolean seated(String id){return "ground_play".equals(id);}

  /** Shared entry/exit envelope, sampled with elapsed production ticks. */
  public static float weight(String id,float elapsed) {
    int ticks=duration(id);
    if(ticks<=0||!Float.isFinite(elapsed)||elapsed<=0||elapsed>=ticks)return 0;
    float fade=Math.min(14,ticks*.2f);
    return smooth(elapsed/fade)*smooth((ticks-elapsed)/fade);
  }

  /** Recheck during playback so real needs can interrupt before a clip ends. */
  public static boolean eligible(ChaoState state) {
    if(state==null||state.dead||state.sleeping||state.cocoonTicks>0||state.hunger<35||state.energy<15)return false;
    for(int illness:state.illnessValues)if(illness<0)return false;
    return emotion(state,ChaoEmotion.FEAR)<115&&emotion(state,ChaoEmotion.CRY)<100
        &&emotion(state,ChaoEmotion.ANGER)<105&&emotion(state,ChaoEmotion.SURPRISE)<=120
        &&emotion(state,ChaoEmotion.TIREDNESS)<=6000&&emotion(state,ChaoEmotion.JOY)<=115
        &&emotion(state,ChaoEmotion.RELAX)<70&&emotion(state,ChaoEmotion.SLEEPINESS)<8000
        &&emotion(state,ChaoEmotion.TIRE)<9000;
  }

  /** Low vitality or a quiet personality alone never invents exhaustion. */
  private static boolean fatigued(ChaoState state) {
    return state.energy<=35||emotion(state,ChaoEmotion.SLEEPINESS)>=5000
        ||emotion(state,ChaoEmotion.TIREDNESS)>=3000||emotion(state,ChaoEmotion.TIRE)>=5000;
  }

  /** allowSeated means the host can admit the clip's temporary seated posture. */
  public static boolean allowed(ChaoState state,String id,boolean allowSeated) {
    if(!valid(id)||!eligible(state)||seated(id)&&!allowSeated)return false;
    if(id.equals("tired"))return fatigued(state);
    if(id.equals("chuckle"))return !fatigued(state)
        &&(emotion(state,ChaoEmotion.JOY)>=55||state.happiness>=45)
        &&emotion(state,ChaoEmotion.ANGER)<55&&emotion(state,ChaoEmotion.CRY)<50&&emotion(state,ChaoEmotion.FEAR)<60;
    return true;
  }

  /** Individual quiet gaps are independent of game time and other Chao. */
  public static int nextGap(RandomGenerator random){return 400+random.nextInt(501);}

  /**
   * A fresh weighted draw keeps several gestures possible even at extreme
   * sliders. Empty means that care, illness or an urgent mood owns the moment.
   * State is only read: even unsynchronized legacy meters are left untouched.
   */
  public static String choose(ChaoState state,RandomGenerator random,boolean allowSeated) {
    if(!eligible(state))return "";
    double total=0;
    for(String id:IDS)if(allowed(state,id,allowSeated))total+=selectionWeight(state,id);
    double draw=random.nextDouble()*total;
    String last="";
    for(String id:IDS)if(allowed(state,id,allowSeated)) {
      last=id;draw-=selectionWeight(state,id);if(draw<0)return id;
    }
    return last;
  }

  private static double selectionWeight(ChaoState state,String id) {
    double curious=positive(state,ChaoPersonality.CURIOUS),energetic=positive(state,ChaoPersonality.ENERGETIC);
    double solitude=positive(state,ChaoPersonality.SOLITUDE),chatty=positive(state,ChaoPersonality.CHATTY);
    return switch(id) {
      case "look_around"->3+3*curious;
      case "look_up"->2+2.5*curious;
      case "think"->2+2*positive(state,ChaoPersonality.SKILLFUL)+1.5*solitude;
      case "bashful"->1.2+2*solitude+1.5*negative(state,ChaoPersonality.CHATTY)+negative(state,ChaoPersonality.ENERGETIC);
      case "chuckle"->.8+energetic+1.5*chatty+.7*positive(state,ChaoPersonality.CAREFREE);
      case "tired"->1.5+2.5*Math.max(Math.max((100-state.energy)/100d,emotion(state,ChaoEmotion.SLEEPINESS)/10000d),
          Math.max(emotion(state,ChaoEmotion.TIREDNESS)/10000d,emotion(state,ChaoEmotion.TIRE)/10000d));
      case "ground_play"->2+1.5*positive(state,ChaoPersonality.CAREFREE)+negative(state,ChaoPersonality.NAIVE)
          +Math.max(0,Math.min(1,state.boredom/100d));
      default->0;
    };
  }
  private static double positive(ChaoState state,int slider){return Math.max(0,state.personalitySlider(slider))/100d;}
  private static double negative(ChaoState state,int slider){return Math.max(0,-state.personalitySlider(slider))/100d;}
  private static int emotion(ChaoState state,int index){return Math.max(0,Math.min(ChaoEmotion.maximum(index),state.emotions[index]));}
  private static float smooth(float value){float t=Math.max(0,Math.min(1,value));return t*t*(3-2*t);}
}
