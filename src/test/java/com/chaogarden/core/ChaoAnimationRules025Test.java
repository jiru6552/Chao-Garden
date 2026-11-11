package com.chaogarden.core;

/** Food reaches the mouth before chewing; idle and blink timing remain bounded. */
public final class ChaoAnimationRules025Test {
  private static int checks;
  private static void check(boolean condition,String message) {
    checks++;if(!condition)throw new AssertionError(message);
  }
  public static int run() {
    checks=0;int peaks=0;float previous=0;
    for(int i=0;i<=600;i++) {
      float p=i/600f,lift=ChaoAnimationRules.nibbleLift(p),chew=ChaoAnimationRules.chew(p);
      check(Float.isFinite(lift)&&lift>=0&&lift<=1,"Meal lift range");
      check(Float.isFinite(chew)&&Math.abs(chew)<=1,"Chewing range");
      if(Math.abs(chew)>.01f)check(lift>.99f,"Chewing before fruit reaches the mouth");
      if(lift>.99f&&previous<=.99f)peaks++;
      previous=lift;
    }
    check(peaks==3,"One sixty-tick meal must contain three distinct held bites");
    int sounds=0;
    for(int elapsed=0;elapsed<=80;elapsed++)if(ChaoAnimationRules.mealSoundAt(elapsed)) {
      sounds++;check(ChaoAnimationRules.nibbleLift(elapsed/60f)>.99f
          &&Math.abs(ChaoAnimationRules.chew(elapsed/60f))>.1f,"Eating sound outside an actual visible bite");
    }
    check(sounds==3&&!ChaoAnimationRules.mealSoundAt(-13),"Exactly one sound per bite");
    check(ChaoAnimationRules.nibbleLift(0)==0&&ChaoAnimationRules.nibbleLift(1)==0,
        "Meal should start and finish with lowered hands");
    for(long seed=0;seed<32;seed++) {
      int closed=0;
      for(int age=0;age<3000;age++)if(ChaoAnimationRules.blinking(age,seed*0x9e3779b97f4a7c15L))closed++;
      check(closed>0&&closed<110,"Blink should be brief and occasional");
    }
    int simultaneous=0;
    for(long seed=0;seed<64;seed++)if(ChaoAnimationRules.blinking(70,seed*0x9e3779b97f4a7c15L))simultaneous++;
    check(simultaneous>0&&simultaneous<10,"Individual blinks should not synchronize");
    check(!ChaoAnimationRules.crawling(0)&&!ChaoAnimationRules.crawling(.003f)
        &&ChaoAnimationRules.crawling(.022f),"Stationary baby sits; its real small stride crawls");
    check(!ChaoAnimationRules.crawling(Float.NaN)&&!ChaoAnimationRules.blinking(Float.NaN,0),
        "Invalid render input must not create an unbounded pose");
    check(ChaoAnimationRules.wateringPour(0)==0&&ChaoAnimationRules.wateringPour(1)==0,"Watering must start and finish lowered");
    check(ChaoAnimationRules.wateringPour(ChaoAnimationRules.WATER_COMMIT_TICK/(float)ChaoAnimationRules.WATER_TICKS)>.99f,"Water must commit with spout tipped");
    for(int i=0;i<=1000;i++) {
      float pour=ChaoAnimationRules.wateringPour(i/1000f);
      check(Float.isFinite(pour)&&pour>=0&&pour<=1,"Watering pose range");
    }
    return checks + ChaoGaitClockTest.run();
  }
  public static void main(String[] args){System.out.println("Baby animation timing: "+run()+" checks passed");}
}
