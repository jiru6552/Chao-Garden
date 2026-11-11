package com.chaogarden.core;

/** Voluntary clothing acceptance. Percentages are a bounded Minecraft adaptation. */
public final class WardrobeChoice {
  private WardrobeChoice() {}
  public static int percent(ChaoState state) {
    if(state.dead || state.sleeping || state.cocoonTicks>0)return 0;
    int happy=Math.max(-100,Math.min(100,state.happiness));
    int energy=Math.max(0,Math.min(100,state.energy));
    int joy=ChaoEmotion.get(state,ChaoEmotion.JOY),anger=ChaoEmotion.get(state,ChaoEmotion.ANGER);
    int cry=ChaoEmotion.get(state,ChaoEmotion.CRY),fear=ChaoEmotion.get(state,ChaoEmotion.FEAR);
    int chance=50+happy*3/10+(energy-50)/10+joy/20-anger/20-cry/25-fear/25;
    return Math.max(15,Math.min(90,chance));
  }
  public static boolean accepts(ChaoState state,int roll) {
    return roll>=0 && roll<100 && roll<percent(state);
  }
}
