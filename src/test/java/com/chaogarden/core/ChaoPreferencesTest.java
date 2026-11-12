package com.chaogarden.core;

import java.util.Arrays;
import java.util.Random;

/** Save migration, observable acceptance choices and inherited grade distributions. */
public final class ChaoPreferencesTest {
  private static int checks;
  private static void check(boolean value,String message){checks++;if(!value)throw new AssertionError(message);}
  public static int run(){
    checks=0;
    for(int seed=0;seed<128;seed++){
      var state=ChaoState.random(new Random(seed));
      check(state.personality>=0&&state.personality<15,"Birth personality is bounded");
      for(int i=0;i<5;i++)check(state.gradeA[i]==state.gradeB[i]&&state.grades[i]==state.gradeA[i],"Purchased/generated egg stat genes are pure");
      check(state.favoriteFruitGenes[0]==state.favoriteFruit&&state.favoriteFruitGenes[1]==state.favoriteFruit,"Fresh preference is homozygous");
      state.boredom=88;state.curiosityTendency=54;state.naiveTendency=-71;
      state.favoriteFruitGenes=new int[]{2,0};
      check(state.encode().equals(ChaoState.decode(state.encode()).encode()),"All new traits and hidden preference alleles persist");
    }
    var a=new ChaoState();var b=new ChaoState();a.adult=b.adult=true;
    Arrays.fill(a.gradeA,0);Arrays.fill(a.gradeB,0);Arrays.fill(b.gradeA,5);Arrays.fill(b.gradeB,5);
    a.favoriteFruitGenes=new int[]{0,1};b.favoriteFruitGenes=new int[]{1,2};
    int high=0;boolean hiddenRound=false,hiddenTriangle=false;
    for(int seed=0;seed<3000;seed++){
      var child=ChaoState.breed(a,b,new Random(seed));
      check(child.gradeA[0]==0&&child.gradeB[0]==5,"Child retains both parental grade alleles");
      check(child.grades[0]==0||child.grades[0]==5,"No intermediate or invented grade");
      if(child.grades[0]==5)high++;
      check(child.favoriteFruitGenes[0]==0||child.favoriteFruitGenes[0]==1,"First parental hidden preference inherited");
      check(child.favoriteFruitGenes[1]==1||child.favoriteFruitGenes[1]==2,"Second parental hidden preference inherited");
      check(child.favoriteFruit==child.favoriteFruitGenes[0]||child.favoriteFruit==child.favoriteFruitGenes[1],"Preference phenotype comes from inherited alleles");
      hiddenRound|=child.favoriteFruit==1&&child.favoriteFruitGenes[0]==0;
      hiddenTriangle|=child.favoriteFruit==1&&child.favoriteFruitGenes[1]==2;
    }
    check(high>1900&&high<2300,"Higher stat grade expresses near70percent, not50 or100");
    check(hiddenRound&&hiddenTriangle,"Unexpressed fruit preferences can be carried");
    var s=new ChaoState();s.hunger=80;s.favoriteFruit=0;
    check(ChaoPreferences.acceptsFood(s,"garden_fruit"),"Hungry ordinary Chao accepts food");
    s.hunger=100;check(!ChaoPreferences.acceptsFood(s,"garden_fruit"),"Full Chao refuses food");
    s.hunger=90;check(ChaoPreferences.acceptsFood(s,"round_fruit")&&!ChaoPreferences.acceptsFood(s,"square_fruit"),"Favorite shape affects considered acceptance");
    s.personality=ChaoPreferences.BIG_EATER;check(ChaoPreferences.acceptsFood(s,"square_fruit"),"Big eater has a larger appetite");
    s.hunger=100;check(ChaoPreferences.acceptsFood(s,"square_fruit"),"Documented Big eater exception can eat even while full");
    s.hunger=30;s.adult=true;s.align=-1;check(!ChaoPreferences.acceptsFood(s,"rare_fruit")&&!ChaoPreferences.acceptsFood(s,"wonder_fruit"),"Dark Chao rejects documented disliked CA2fruit");
    s.align=1;check(!ChaoPreferences.acceptsFood(s,"creepy_fruit")&&!ChaoPreferences.acceptsFood(s,"rough_fruit")&&!ChaoPreferences.acceptsFood(s,"forbidden_fruit"),"Hero Chao rejects documented disliked CA2fruit");
    check(ChaoPreferences.acceptsFood(s,"strong_fruit"),"Modern Strong Fruit keeps its neutral SA2B profile");
    for(float oldAlignment:new float[]{-1,1}){
      var reborn=new ChaoState();reborn.adult=true;reborn.align=oldAlignment;reborn.reincarnate();
      reborn.cocoonTicks=0;reborn.hunger=30;
      String opposite=oldAlignment<0?"hero_garden_fruit":"dark_garden_fruit";
      check(ChaoPreferences.acceptsFood(reborn,opposite),"Reborn aligned child can accept the opposing alignment fruit");
      reborn.feed(ChaoState.STAMINA,40,oldAlignment<0?.15f:-.15f);
      check(Math.abs(reborn.align)==.85f,"Accepted opposing fruit can move reborn child alignment toward Neutral");
    }
    s.energy=0;check(!ChaoPreferences.acceptsTraining(s)&&ChaoPreferences.playChanceDenominator(s)==0,"Exhausted Chao does not choose training or toys");
    s.energy=100;s.boredom=0;int calm=ChaoPreferences.playChanceDenominator(s);s.boredom=100;
    check(ChaoPreferences.playChanceDenominator(s)<calm,"Boredom makes voluntary play more likely");
    s.personality=ChaoPreferences.CURIOUS;int curious=ChaoPreferences.thinkingTicks(s);s.personality=ChaoPreferences.SMART;
    check(ChaoPreferences.thinkingTicks(s)<curious,"Personality changes inspection timing");
    for(float align:new float[]{-1,0,1}){
      s=new ChaoState();s.adult=true;s.happiness=51;s.align=align;s.adultAgeTicks=99;
      check(s.tick(20,100,new Random(1))==ChaoState.Transition.REINCARNATED,"Above50 happiness reincarnates");
      check(s.align==align,"Reincarnation preserves Hero/Dark alignment category");
    }
    s=new ChaoState();s.adult=true;s.happiness=50;s.adultAgeTicks=99;
    check(s.tick(20,100,new Random(1))==ChaoState.Transition.DIED,"Exactly50 is below SA2 reincarnation threshold");
    // Non-Normal first forms retain their saved influence policy. The new
    // Neutral/Normal current-stat policy has its own nine-combination suite.
    s=new ChaoState();s.adult=true;s.primaryType=ChaoState.FLY_TYPE;s.swimFly=-.8f;s.runPower=.7f;
    check(s.secondaryFormKey().equals("swim_power")&&s.secondaryTypes().length==2,"Both adult axes form a mixed secondary");
    s.swimFly=.8f;s.runPower=-.8f;check(s.secondaryFormKey().equals("fly_run"),"Adult secondary can reverse without cocoon");
    s.swimFly=s.runPower=0;check(s.secondaryFormKey().equals("normal"),"Balancing adult axes returns Normal secondary");
    s=new ChaoState();s.hunger=100;s.ageTicks=ChaoPreferences.FULLNESS_DECAY_TICKS-1;
    s.tick(216000,864000,new Random(1));check(s.hunger==99,"Fullness drains on the adapted loaded-garden clock");
    return checks;
  }
  public static void main(String[] args){System.out.println("Chao preferences: "+run()+" checks passed");}
}
