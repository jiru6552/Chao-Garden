package com.chaogarden.core;

/** Source-scale emotion storage; garden-clock rates are documented Minecraft adaptations. */
public final class ChaoEmotion {
  public static final int JOY=0,ANGER=1,CRY=2,FEAR=3,SURPRISE=4,RELAX=5,TOTAL=6,
      SLEEPINESS=7,TIREDNESS=8,HUNGRY=9,MATE=10,BOREDOM=11,LONELY=12,TIRE=13,
      STRESS=14,NOURISH=15,CONDITION=16,ENERGY=17,COUNT=18;
  public static final String[] NAMES={"Joy","Anger","Urge to cry","Fear","Surprise","Relax","Total (unused)",
      "Sleepiness","Tiredness","Hunger","Desire to mate","Boredom","Loneliness","Fatigue",
      "Stress (unused)","Nourish (unknown purpose)","Condition","Energy"};
  private ChaoEmotion(){}
  public static int maximum(int i){return i>=0&&i<=TOTAL?200:10000;}
  public static int get(ChaoState s,int i){s.syncLegacyMeters();return i>=0&&i<COUNT?s.emotions[i]:0;}
  public static void set(ChaoState s,int i,int value){
    if(i<0||i>=COUNT)return;s.syncLegacyMeters();s.emotions[i]=Math.max(0,Math.min(maximum(i),value));s.mirrorLegacyMeters();
  }
  public static void add(ChaoState s,int i,int amount){set(s,i,get(s,i)+amount);}
  public static void tick(ChaoState s){
    s.syncLegacyMeters();if(s.ageTicks%20!=0)return;
    int regain=s.personalitySlider(ChaoPersonality.REGAIN),naive=s.personalitySlider(ChaoPersonality.NAIVE);
    int energetic=s.personalitySlider(ChaoPersonality.ENERGETIC),carefree=s.personalitySlider(ChaoPersonality.CAREFREE);
    for(int i:new int[]{JOY,ANGER,FEAR})add(s,i,-1);
    if(energetic>=-54||s.ageTicks%40==0)add(s,CRY,-1);
    add(s,SURPRISE,-Math.max(1,3+regain/30));
    add(s,RELAX,-Math.max(1,2+s.personalitySlider(ChaoPersonality.FICKLE)/40+regain/50));
    if(s.sleeping){
      add(s,SLEEPINESS,-Math.max(8,40+Math.min(0,naive)/4+regain/10));
      if(get(s,SLEEPINESS)>1500)add(s,TIREDNESS,20);else add(s,TIREDNESS,-100);
      add(s,TIRE,-Math.max(15,50+regain/4));add(s,ENERGY,100);
    }else{
      add(s,SLEEPINESS,Math.max(2,8-Math.min(0,naive)/7));
      add(s,TIREDNESS,-Math.max(1,3+regain/30));
      add(s,TIRE,Math.max(1,4+s.personalitySlider(ChaoPersonality.VITALITY)/25));
      add(s,BOREDOM,Math.max(1,10+s.personalitySlider(ChaoPersonality.CURIOUS)/5-carefree/5));
      add(s,LONELY,Math.max(1,10-s.personalitySlider(ChaoPersonality.SOLITUDE)/10));
      if(s.adult&&!s.immortal)add(s,MATE,Math.max(1,4+s.personalitySlider(ChaoPersonality.VITALITY)/10));
      if(s.hunger<10){add(s,CRY,Math.max(1,3-energetic/25));if(s.align<=-.5f)add(s,ANGER,2);}
    }
    // These source values have known storage/decay but no verified gameplay effect.
    add(s,NOURISH,-1);
  }
}
