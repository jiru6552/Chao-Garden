package com.chaogarden.core;

import java.util.random.RandomGenerator;

/** Combined-drive arithmetic uses the existing 100-progress level and earned-point systems. */
public final class DriveRules {
  public enum Tier { SOLID, CHAOTIC, SUPER_CHAOTIC, UNSTABLE_CHAOTIC, SUPER_SOLID, UNSTABLE_SOLID, PERFECT }
  public static final int MAX_TOTAL=9900;
  private DriveRules() {}
  public static boolean typed(Tier tier){return tier==Tier.SOLID||tier==Tier.SUPER_SOLID||tier==Tier.UNSTABLE_SOLID;}
  public static boolean canUse(ChaoState s,Tier tier,int stat){
    if(s.dead||tier==null||typed(tier)&&(stat<0||stat>3))return false;
    if(tier==Tier.PERFECT){for(int level:s.levels)if(level<99)return true;return false;}
    if(tier==Tier.UNSTABLE_CHAOTIC||tier==Tier.UNSTABLE_SOLID||tier==Tier.SUPER_CHAOTIC)return true;
    if(s.happiness<100||s.canTrainHidden(ChaoState.HIDDEN_LUCK)||s.canTrainHidden(ChaoState.HIDDEN_INTELLIGENCE))return true;
    if(typed(tier))return s.levels[stat]<99;
    for(int i=0;i<4;i++)if(s.levels[i]<99)return true;
    return false;
  }
  /** Returns false without consuming randomness or changing state when no benefit is possible. */
  public static boolean apply(ChaoState s,Tier tier,int stat,RandomGenerator random){
    if(!canUse(s,tier,stat))return false;
    int[] previousHistory=s.trainingHistorySnapshot(),beforePoints=s.points.clone(),beforeTotals=new int[5];
    for(int i=0;i<5;i++)beforeTotals[i]=total(s,i);
    if(tier==Tier.PERFECT){
      int[] candidates=new int[5];int count=0;
      for(int i=0;i<5;i++)if(s.levels[i]<99)candidates[count++]=i;
      setTotal(s,candidates[random.nextInt(count)],MAX_TOTAL,random);
    }else if(tier==Tier.UNSTABLE_CHAOTIC){
      int selected=random.nextInt(4);int roll=random.nextInt(1000);
      // Explicit endpoints are rare. Interior targets form a triangle peaked at level 49.50.
      int target=roll<5?0:roll<10?MAX_TOTAL:1+random.nextInt(4950)+random.nextInt(4950);
      if(roll>=10&&target==total(s,selected))target=target==9899?9898:target+1;
      setTotal(s,selected,target,random);
      s.happiness=Math.clamp(s.happiness+random.nextInt(21)-10,-100,100);
      adjustHidden(s,ChaoState.HIDDEN_INTELLIGENCE,random.nextInt(201)-100,random);
      if(random.nextInt(100)<2)s.grades[selected]=Math.clamp(s.grades[selected]+(random.nextBoolean()?1:-1),0,6);
    }else if(tier==Tier.UNSTABLE_SOLID){
      setTotal(s,stat,random.nextInt(100)<5?MAX_TOTAL:0,random);
      s.happiness=Math.clamp(s.happiness+(random.nextBoolean()?1:-1),-100,100);
      adjustHidden(s,ChaoState.HIDDEN_LUCK,random.nextBoolean()?40:-40,random);
      adjustHidden(s,ChaoState.HIDDEN_INTELLIGENCE,random.nextBoolean()?20:-20,random);
    }else{
      switch(tier){
        case SOLID -> change(s,stat,100,random);
        case SUPER_SOLID -> change(s,stat,1000,random);
        case CHAOTIC -> {for(int i=0;i<4;i++)change(s,i,24,random);}
        case SUPER_CHAOTIC -> {
          int[] order={0,1,2,3};
          for(int i=3;i>0;i--){int j=random.nextInt(i+1),v=order[i];order[i]=order[j];order[j]=v;}
          int[] amounts={24,100,500,-200};
          for(int i=0;i<4;i++)change(s,order[i],amounts[i],random);
        }
        default -> throw new IllegalStateException("Unexpected drive tier");
      }
      s.happiness=Math.clamp(s.happiness+1,-100,100);
      adjustHidden(s,ChaoState.HIDDEN_LUCK,40,random);
      adjustHidden(s,ChaoState.HIDDEN_INTELLIGENCE,20,random);
    }
    s.recordTrainingTransaction(previousHistory,beforeTotals,beforePoints);
    if(s.driveCount<Integer.MAX_VALUE)s.driveCount++;
    return true;
  }
  public static int total(ChaoState s,int stat){return Math.clamp(s.levels[stat]*100+s.progress[stat],0,MAX_TOTAL);}
  public static void change(ChaoState s,int stat,int amount,RandomGenerator random){
    setTotal(s,stat,(int)Math.clamp((long)total(s,stat)+amount,0,MAX_TOTAL),random);
  }
  public static void setTotal(ChaoState s,int stat,int target,RandomGenerator random){
    target=Math.clamp(target,0,MAX_TOTAL);int before=total(s,stat);
    if(target>=before){s.train(stat,target-before,random);return;}
    int level=target/100,lost=s.levels[stat]-level;
    s.points[stat]=level==0?0:Math.clamp(s.points[stat]-lost*(13+3*s.grades[stat]),0,9999);
    s.levels[stat]=level;s.progress[stat]=target%100;
  }
  public static void adjustHidden(ChaoState s,int stat,int amount,RandomGenerator random){
    if(amount>=0){s.trainHidden(stat,amount,random);return;}
    int target=Math.clamp(s.hiddenLevels[stat]*100+s.hiddenProgress[stat]+amount,0,MAX_TOTAL);
    int level=target/100,lost=s.hiddenLevels[stat]-level;
    int grade=stat==ChaoState.HIDDEN_LUCK?s.luckGrade:s.intelligenceGrade;
    int points=stat==ChaoState.HIDDEN_LUCK?s.luck:s.intelligence;
    points=level==0?0:Math.clamp(points-lost*(13+3*Math.clamp(grade,0,255)),0,4000);
    if(stat==ChaoState.HIDDEN_LUCK)s.luck=points;else s.intelligence=points;
    s.hiddenLevels[stat]=level;s.hiddenProgress[stat]=target%100;
  }
}
