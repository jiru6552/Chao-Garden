package com.chaogarden.core;

/** Signed training directions and age strength are independent values. */
public final class GradualEvolutionRules {
  public static final float ADULT_STRENGTH_MAX=1.2f;
  private GradualEvolutionRules() {}

  /** Age counters saturate at MAX-1, so every configured duration stays reachable. */
  public static int durationTicks(int configured) {
    return Math.max(20,Math.min(Integer.MAX_VALUE-1,configured));
  }

  public static float ordinaryAxis(float value) {
    return Float.isFinite(value)?Math.max(-1,Math.min(1,value)):0;
  }

  public static double strength(double value,boolean adult) {
    return Double.isFinite(value)?Math.max(0,Math.min(adult?1.2:1,value)):0;
  }

  /** User-requested Minecraft rule: highest trained points, then latest actual increase. */
  public static int firstType(int[] points,int[] recent) {
    int best=0;for(int value:points)best=Math.max(best,value);
    for(int stat:recent)if(stat>=0&&stat<5&&points[stat]==best)return type(stat);
    // Stamina represents Normal. Untouched/all-equal records have no preferred ability.
    if(points[ChaoState.STAMINA]==best)return ChaoState.NORMAL;
    for(int stat=0;stat<4;stat++)if(points[stat]==best)return type(stat);
    return ChaoState.NORMAL;
  }

  private static int type(int stat){return stat==ChaoState.STAMINA?ChaoState.NORMAL:stat+1;}

  public static int[] recordIncrease(int[] recent,int stat) {
    int[] next={-1,-1,-1,-1,-1};next[0]=stat;int at=1;
    for(int previous:recent)if(previous>=0&&previous<5&&previous!=stat&&at<5)next[at++]=previous;
    return next;
  }
}
