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

  /** Ordinary first evolution: highest earned four-ability points, then latest actual increase. */
  public static int firstType(int[] points,int[] recent) {
    int best=0;for(int stat=0;stat<4;stat++)best=Math.max(best,points[stat]);
    for(int stat:recent)if(stat>=0&&stat<4&&points[stat]==best)return type(stat);
    // An untouched record has no invented preference. Stamina never competes.
    if(best==0)return ChaoState.NORMAL;
    for(int stat=0;stat<4;stat++)if(points[stat]==best)return type(stat);
    return ChaoState.NORMAL;
  }

  /** Preserve the pre-prototype Normal admission predicate for rare Chaos Chao.
   * Removing Stamina from ordinary selection must not make that unrelated path
   * unobtainable after the mandatory DNA has trained all four abilities. */
  public static boolean legacyNormalForChaos(int[] points,int[] recent) {
    int best=0;for(int value:points)best=Math.max(best,value);
    for(int stat:recent)if(stat>=0&&stat<5&&points[stat]==best)return stat==ChaoState.STAMINA;
    return points[ChaoState.STAMINA]==best;
  }

  private static int type(int stat){return stat==ChaoState.STAMINA?ChaoState.NORMAL:stat+1;}

  public static int[] recordIncrease(int[] recent,int stat) {
    int[] next={-1,-1,-1,-1,-1};next[0]=stat;int at=1;
    for(int previous:recent)if(previous>=0&&previous<5&&previous!=stat&&at<5)next[at++]=previous;
    return next;
  }

  /** One multi-stat transaction has no artificial last member. Promote its
   * actual increases as a group, preserving their prior relative history.
   * Previously unrecorded members use Swim/Fly/Run/Power/Stamina order. */
  public static int[] recordIncreases(int[] recent,boolean[] increased) {
    int[] next={-1,-1,-1,-1,-1};boolean[] seen=new boolean[5];int at=0;
    for(int stat:recent)if(stat>=0&&stat<5&&increased[stat]&&!seen[stat]){next[at++]=stat;seen[stat]=true;}
    for(int stat=0;stat<5;stat++)if(increased[stat]&&!seen[stat]){next[at++]=stat;seen[stat]=true;}
    for(int stat:recent)if(stat>=0&&stat<5&&!seen[stat]){next[at++]=stat;seen[stat]=true;}
    return next;
  }
}
