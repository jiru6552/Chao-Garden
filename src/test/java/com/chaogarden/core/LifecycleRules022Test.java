package com.chaogarden.core;

import java.util.Arrays;
import java.util.Random;

/** Independent boundary/oracle audit for ordinary and mechanical repeated lives. */
public final class LifecycleRules022Test {
  private static int checks;
  private LifecycleRules022Test() {}
  private static void require(boolean ok,String why){checks++;if(!ok)throw new AssertionError(why);}
  private static ChaoState life(){var s=new ChaoState();s.happiness=90;s.bond=80;return s;}
  public static int run(){
    checks=0;carryEveryVisibleValue();hiddenCarryAndMalformedBounds();gradesAndRepeatedLives();
    durationBoundaries();careCountersAndIdentity();chaosCareBoundary();naturalChaosFeasibility();return checks;
  }

  private static void carryEveryVisibleValue(){
    for(int input=0;input<10000;input++){
      var s=life();int[] old=new int[5];
      for(int stat=0;stat<5;stat++){old[stat]=(input+stat*1723)%10000;s.points[stat]=old[stat];s.levels[stat]=99;s.progress[stat]=97;}
      s.reincarnate();
      for(int stat=0;stat<5;stat++)require(s.points[stat]==old[stat]/10,"Exact visible ten-percent carry: "+old[stat]);
      require(Arrays.equals(s.levels,new int[]{1,1,1,1,1})&&Arrays.equals(s.progress,new int[5]),"New life starts level1/progress0");
    }
  }
  private static void hiddenCarryAndMalformedBounds(){
    for(int input=0;input<10000;input++){
      var s=life();s.luck=input;s.intelligence=4000-input;
      s.hiddenLevels=new int[]{99,76};s.hiddenProgress=new int[]{99,83};s.reincarnate();
      require(s.luck==Math.min(4000,input)/10,"Hidden luck carry/clamp");
      require(s.intelligence==Math.max(0,4000-input)/10,"Hidden intelligence carry/clamp");
      require(Arrays.equals(s.hiddenLevels,new int[]{1,1})&&Arrays.equals(s.hiddenProgress,new int[2]),"Hidden level/progress reset");
    }
    for(int invalid:new int[]{Integer.MIN_VALUE,-1,10000,Integer.MAX_VALUE}){
      var s=life();Arrays.fill(s.points,invalid);s.luck=s.intelligence=invalid;s.reincarnations=invalid;s.reincarnate();
      int visible=Math.max(0,Math.min(9999,invalid))/10,hidden=Math.max(0,Math.min(4000,invalid))/10;
      require(Arrays.stream(s.points).allMatch(v->v==visible)&&s.luck==hidden&&s.intelligence==hidden,"Malformed carry is bounded before division");
      require(s.reincarnations>=1&&s.reincarnations<=9999&&ChaoState.decode(s.encode()).reincarnations==s.reincarnations,"Life count is bounded and reload stable");
    }
  }
  private static void gradesAndRepeatedLives(){
    for(int grade=0;grade<=5;grade++)for(int outcome=0;outcome<5;outcome++)for(int stat=0;stat<5;stat++){
      var s=life();s.grades[stat]=grade;final int roll=outcome;
      s.train(stat,100,new Random(1){@Override public int nextInt(int bound){if(bound!=5)throw new AssertionError("Unexpected gain roll");return roll;}});
      require(s.levels[stat]==1&&s.progress[stat]==0&&s.points[stat]==11+3*grade+outcome,"E-S gain is the uniform five-point range");
    }
    for(int type=0;type<=4;type++)for(int alignment=-1;alignment<=1;alignment++)for(int grade=0;grade<=5;grade++){
      var s=life();int stat=type==0?4:type-1;Arrays.fill(s.grades,grade);Arrays.fill(s.gradeA,grade);Arrays.fill(s.gradeB,grade);
      s.points[stat]=100;s.align=alignment;s.ageTicks=19;
      require(s.tick(20,1000,new Random(7))==ChaoState.Transition.EVOLVED,"Child enters first cocoon");
      int expected=Math.min(5,grade+1);
      require(s.primaryType==type&&s.grades[stat]==expected&&s.gradeA[stat]==expected&&s.gradeB[stat]==grade,"First evolution upgrades one expressed allele and caps S");
      var copy=ChaoState.decode(s.encode());for(int tick=0;tick<200;tick++)copy.tick(20,1000,new Random(tick));
      require(copy.cocoonTicks==0&&copy.grades[stat]==expected&&copy.adultAgeTicks==0,"Cocoon reload never repeats grade bonus or ages the adult");
      int[] grades=copy.grades.clone();copy.reincarnate();require(Arrays.equals(grades,copy.grades),"Reincarnation retains grades");
    }
    int carry=0;int[] independentlyExpected={2970,3237,3263,3266,3266};
    for(int generation=0;generation<independentlyExpected.length;generation++){
      var s=life();Arrays.fill(s.grades,5);s.points[0]=carry;s.levels[0]=generation==0?0:1;
      s.train(0,10000,new Random(0){@Override public int nextInt(int n){return n-1;}});
      require(s.points[0]==independentlyExpected[generation]&&s.levels[0]==99,"Perfect-S repeated-life convergence");
      s.reincarnate();carry=s.points[0];
    }
    for(int care:new int[]{-100,0,50,51,100}){
      var s=life();s.adult=true;s.adultAgeTicks=19;s.happiness=care;
      var result=s.tick(1000,20,new Random(1));require(result==(care>50?ChaoState.Transition.REINCARNATED:ChaoState.Transition.DIED),"Terminal care strict50/51 boundary");
    }
    var immortal=life();immortal.adult=immortal.immortal=true;immortal.adultAgeTicks=Integer.MAX_VALUE-2;
    require(immortal.tick(20,20,new Random(1))==ChaoState.Transition.NONE&&!immortal.dead,"Chaos immortality bypasses natural death");
  }
  private static void durationBoundaries(){
    for(int value:new int[]{Integer.MIN_VALUE,-1,0,19,20,21,200,Integer.MAX_VALUE-1,Integer.MAX_VALUE}){
      int expected=Math.max(20,Math.min(Integer.MAX_VALUE-1,value));
      require(GradualEvolutionRules.durationTicks(value)==expected,"Reachable duration clamp");
      var child=life();child.ageTicks=expected-1;
      require(child.tick(value,Integer.MAX_VALUE,new Random(1))==ChaoState.Transition.EVOLVED,"Every child duration is reachable");
      var adult=life();adult.adult=true;adult.adultAgeTicks=expected-1;
      require(adult.tick(Integer.MAX_VALUE,value,new Random(1))==ChaoState.Transition.REINCARNATED,"Every ordinary lifespan is reachable");
      var robot=life();OmochaoRules.initialize(robot,life(),"Builder",2);robot.adultAgeTicks=expected-1;
      require(OmochaoRules.tickRobot(robot,value)==ChaoState.Transition.REINCARNATED,"Every mechanical lifespan is reachable");
    }
    var s=life();s.adult=true;s.setEvolutionStrength(0);s.adultAgeTicks=100;
    s.tick(Integer.MAX_VALUE,Integer.MAX_VALUE,new Random(1));
    require(s.evolutionStrength()>0&&s.evolutionStrength()<.00001f,"Quarter-rate divisor never overflows at maximum child duration");
  }
  private static void careCountersAndIdentity(){
    var s=life();s.fruitCount=s.driveCount=s.petCount=Integer.MAX_VALUE;s.reincarnations=9999;
    s.feed(4,0,0);s.giveDrive(0);s.pet(0);
    require(s.fruitCount==Integer.MAX_VALUE&&s.driveCount==Integer.MAX_VALUE&&s.petCount==Integer.MAX_VALUE,"Care histories saturate without wrapping");
    s.setTrait("reward.race",7);s.variant="create";String lineage=s.lineageId();int[] genes=s.gradeA.clone();s.reincarnate();var saved=ChaoState.decode(s.encode());
    require(saved.reincarnations==9999&&saved.lineageId().equals(lineage)&&saved.variant.equals("create")&&saved.intTrait("reward.race")==7&&Arrays.equals(saved.gradeA,genes),"Bounded reincarnation preserves identity, theme, rewards and hereditary genes");
  }
  private static void chaosCareBoundary(){
    for(int alignment=-1;alignment<=1;alignment++)for(int happiness:new int[]{79,80,81}){
      var s=life();s.align=alignment;s.happiness=happiness;s.reincarnations=2;s.animalMask=ChaoState.ALL_ANIMALS;s.points[4]=100;s.ageTicks=19;
      require(s.tick(20,1000,new Random(1))==ChaoState.Transition.EVOLVED,"Eligible child enters actual first evolution");
      require(s.immortal==(happiness>80),"SA2B Chaos requires strictly over80 happiness, across all alignments");
    }
    for(int missing=0;missing<21;missing++){
      var s=life();s.happiness=100;s.reincarnations=2;s.animalMask=ChaoState.ALL_ANIMALS&~(1<<missing);s.points[4]=100;s.ageTicks=19;s.tick(20,1000,new Random(1));
      require(!s.immortal,"Every current-life animal independently required for Chaos");
    }
    for(int first=1;first<=4;first++){
      var s=life();s.happiness=100;s.reincarnations=2;s.animalMask=ChaoState.ALL_ANIMALS;s.points[first-1]=100;s.ageTicks=19;s.tick(20,1000,new Random(1));
      require(!s.immortal&&s.primaryType==first,"Ordinary skill first type cannot become Chaos");
    }
  }
  private static void naturalChaosFeasibility(){
    var s=life();
    for(int generation=0;generation<2;generation++){
      staminaMeals(s);s.ageTicks=19;
      require(s.tick(20,20,new Random(generation))==ChaoState.Transition.EVOLVED&&!s.immortal,"Earlier fruit-trained Normal life evolves ordinarily");
      for(int i=0;i<200;i++)s.tick(20,20,new Random(i));
      s.adultAgeTicks=19;s.happiness=90;
      require(s.tick(20,20,new Random(generation))==ChaoState.Transition.REINCARNATED&&s.reincarnations==generation+1,"Actual positive-care lifespan creates the next biological life");
      for(int i=0;i<200;i++)s.tick(20,20,new Random(i));
    }
    for(int animal=0;animal<21;animal++)s.giveAnimal(animal);
    staminaMeals(s);s.happiness=81;s.align=1;s.ageTicks=19;
    require(s.animalMask==ChaoState.ALL_ANIMALS&&Arrays.stream(Arrays.copyOf(s.points,4)).allMatch(v->v<s.points[ChaoState.STAMINA]),"Current-life21genes and fruit-trained Stamina satisfy the actual Minecraft Normal rule");
    require(s.tick(20,20,new Random(22))==ChaoState.Transition.EVOLVED&&s.immortal&&s.primaryType==ChaoState.NORMAL&&s.typeName().equals("Angel"),"Third natural life at81care can actually attain Chaos");
    require(s.grades[ChaoState.STAMINA]==5&&s.animalPartsMask==0,"Third Normal evolution reaches S Stamina and canonical Chaos anatomy");
  }
  private static void staminaMeals(ChaoState s){
    // Actual default item credits; only waiting for appetite is accelerated.
    for(int meal=0;meal<20;meal++){
      s.hunger=20;com.chaogarden.fruit.FruitEffects.apply(s,"mushroom_alt",false);s.onFruitEaten();
    }
  }
}
