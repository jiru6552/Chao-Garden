package com.chaogarden.activities;

import com.chaogarden.core.ChaoState;

/** Explicit Minecraft adaptations of the race/karate roles documented by Chao Island. */
public final class ActivityRules {
  public static final double MAX_LINK=32;
  public static boolean linkLength(double dx,double dy,double dz){double sq=dx*dx+dy*dy+dz*dz;return sq>=1&&sq<=MAX_LINK*MAX_LINK;}
  public static double raceSpeed(ChaoState s,boolean water,boolean descending,boolean climbing,double stamina){
    int stat=water?ChaoState.SWIM:climbing?ChaoState.POWER:descending?ChaoState.FLY:ChaoState.RUN;
    double speed=.8+Math.min(1.3,s.points[stat]/1600.0);
    if(water&&s.points[ChaoState.SWIM]<100)speed*=.55;
    return speed*(stamina<=0?.2:1);
  }
  public static double karateHealth(ChaoState s){return 40+Math.min(140,s.points[ChaoState.STAMINA]/20.0);}
  public static double karateDamage(ChaoState attacker,ChaoState defender){
    return Math.max(1,(3+Math.min(20,attacker.points[ChaoState.POWER]/140.0))/(1+defender.points[ChaoState.SWIM]/1200.0));
  }
  public static double evasion(ChaoState s){return Math.min(.4,s.points[ChaoState.FLY]/5000.0);}
  public static int attackInterval(ChaoState s){return Math.max(7,26-s.points[ChaoState.RUN]/180);}
  /** A defeated/ring-out fighter cannot land a later counterattack in this tick. */
  public static boolean canAttack(double attackerHp,double defenderHp,boolean attackerOut,boolean defenderOut){
    return attackerHp>0&&defenderHp>0&&!attackerOut&&!defenderOut;
  }
  public static int addLifetimeExperience(int previous,int amount){
    return (int)Math.min(Integer.MAX_VALUE,Math.max(0L,previous)+Math.max(0L,amount));
  }
  private ActivityRules(){}
}
