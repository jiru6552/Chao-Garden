package com.chaogarden.core;

import java.util.random.RandomGenerator;

/** Six source illness meters; rarity and post-treatment immunity use the Minecraft garden clock. */
public final class ChaoIllness {
  public static final int COUGH=0,COLD=1,RASH=2,RUNNY_NOSE=3,HICCUPS=4,STOMACH_ACHE=5,COUNT=6;
  public static final String[] NAMES={"Cough","Cold","Rash","Runny nose","Hiccups","Stomach ache"};
  public static final int IMMUNITY_TICKS=24000;
  private ChaoIllness(){}
  public static boolean susceptible(ChaoState s,int i){
    if(i<0||i>=COUNT||s.dead||s.immortal||OmochaoRules.isOmochao(s))return false;
    if(s.adult&&s.primaryType==ChaoState.SWIM_TYPE&&(i==COLD||i==RUNNY_NOSE))return false;
    if(s.adult&&s.primaryType==ChaoState.POWER_TYPE&&(i==HICCUPS||i==STOMACH_ACHE))return false;
    return i!=STOMACH_ACHE||!ChaoPreferences.hasPersonality(s,ChaoPreferences.BIG_EATER);
  }
  public static boolean risk(ChaoState s,int i,int denominator,RandomGenerator r){
    if(!susceptible(s,i)||s.illnessValues[i]<0||s.illnessCooldown[i]>0)return false;
    if(r.nextInt(Math.max(1,denominator))!=0)return false;s.illnessValues[i]=-100;return true;
  }
  public static void tick(ChaoState s,RandomGenerator r){
    for(int i=0;i<COUNT;i++)if(s.illnessCooldown[i]>0&&--s.illnessCooldown[i]==0&&s.illnessValues[i]>0)s.illnessValues[i]=0;
    if(s.ageTicks%1200==0&&!s.sleeping&&s.cocoonTicks<=0){
      int i=r.nextInt(COUNT);risk(s,i,s.hunger<10||s.energy<10?48:480,r);
    }
  }
}
