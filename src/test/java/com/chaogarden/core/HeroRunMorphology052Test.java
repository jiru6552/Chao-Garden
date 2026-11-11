package com.chaogarden.core;

import java.util.Arrays;
import java.util.Random;

/** Behavioral boundaries for the one-path morphology prototype. */
public final class HeroRunMorphology052Test {
  private static int checks;
  private static void check(boolean value,String why){checks++;if(!value)throw new AssertionError(why);}
  private static void near(float actual,float expected,String why){check(Math.abs(actual-expected)<.000002f,why+": "+actual+" vs "+expected);}
  public static int run(){checks=0;firstEvolution();simultaneousHistory();trainingAndLoss();allMutationRoutes();identityAndSaves();readOnlyAndScope();System.out.println("Hero Run morphology052: "+checks+" checks passed; four-stat ties, simultaneous drives, reversible training, independent maturity and saved identity");return checks;}

  private static ChaoState heroAdult(){
    var s=new ChaoState();s.adult=true;s.primaryType=ChaoState.RUN_TYPE;s.align=1;
    s.ageTicks=450;s.adultAgeTicks=200;s.setEvolutionStrength(1.2f);
    return ChaoState.decode(s.encode());
  }
  private static void firstEvolution(){
    for(int stat=0;stat<4;stat++){
      var s=new ChaoState();s.align=1;s.points[stat]=100;s.levels[(stat+1)%4]=99;s.points[4]=9999;s.ageTicks=19;
      check(s.prospectivePrimaryType()==stat+1,"Earned four-stat points win despite another skill's higher level and Stamina");
      check(s.tick(20,10000,new Random(stat))==ChaoState.Transition.EVOLVED&&s.primaryType==stat+1&&!s.immortal,"Actual ordinary first cocoon uses the four-stat decision");
    }
    int[][] orders=permutations();
    for(int mask=1;mask<16;mask++)for(int[] order:orders){
      var s=new ChaoState();int expected=-1;
      for(int stat:order){s.train(stat,1,new Random(stat));if((mask&(1<<stat))!=0)expected=stat;}
      for(int stat=0;stat<4;stat++)s.points[stat]=(mask&(1<<stat))!=0?100:10;
      s.points[4]=9999;
      check(s.prospectivePrimaryType()==expected+1,"Latest actually raised member of the tied highest subset wins");
      var saved=ChaoState.decode(s.encode());
      check(saved.prospectivePrimaryType()==expected+1,"All tie histories survive an exact state round trip");
    }
    var untouched=new ChaoState();untouched.train(4,1);untouched.points[4]=9999;
    check(untouched.prospectivePrimaryType()==ChaoState.NORMAL,"Stamina-only history does not invent a four-ability preference");
    untouched.train(2,1);check(untouched.prospectivePrimaryType()==ChaoState.RUN_TYPE,"Real partial Run training breaks an all-zero earned-point tie");
    var fallback=new ChaoState();Arrays.fill(fallback.points,100);
    check(fallback.prospectivePrimaryType()==ChaoState.SWIM_TYPE,"A positive tied save with no history uses the declared stable four-stat order");
    var rare=new ChaoState();rare.points[2]=100;rare.points[4]=101;rare.align=1;rare.reincarnations=2;rare.happiness=90;rare.animalMask=ChaoState.ALL_ANIMALS;rare.ageTicks=19;
    check(rare.prospectivePrimaryType()==ChaoState.RUN_TYPE&&rare.chaosNormalCandidate(),"Ordinary decision and preserved rare Normal admission are explicitly separate");
    rare.tick(20,1000,new Random(1));check(rare.immortal&&rare.primaryType==ChaoState.NORMAL,"The existing all-DNA rare path remains obtainable and keeps its Normal grade/identity behavior");
    var zero=new ChaoState();zero.train(ChaoState.RUN,1);
    check(!zero.chaosNormalCandidate(),"Legacy rare predicate retains its own latest-increase rule even at zero earned points");
  }

  private static int[][] permutations(){
    int[][] values=new int[24][4];int at=0;
    for(int a=0;a<4;a++)for(int b=0;b<4;b++)for(int c=0;c<4;c++)for(int d=0;d<4;d++)
      if(a!=b&&a!=c&&a!=d&&b!=c&&b!=d&&c!=d)values[at++]=new int[]{a,b,c,d};
    return values;
  }
  private static void simultaneousHistory(){
    for(int[] order:permutations()){
      var s=new ChaoState();for(int stat:order)s.train(stat,1,new Random(stat));Arrays.fill(s.points,100);
      int previous=s.lastTrainedStat();
      check(DriveRules.apply(s,DriveRules.Tier.CHAOTIC,-1,new Random(3)),"Balanced combined drive applies");
      check(s.lastTrainedStat()==previous&&s.prospectivePrimaryType()==previous+1,"Simultaneous equal rises preserve previous latest tied member instead of loop order");
    }
    var first=new ChaoState();DriveRules.apply(first,DriveRules.Tier.CHAOTIC,-1,new Random(3));
    check(first.lastTrainedStat()==0&&first.prospectivePrimaryType()==ChaoState.SWIM_TYPE,"New simultaneous history has the documented canonical fallback");
    var capped=new ChaoState();capped.train(2,1);Arrays.fill(capped.levels,99);Arrays.fill(capped.progress,0);
    String before=capped.encode();check(!DriveRules.apply(capped,DriveRules.Tier.PERFECT,-1,new Random(4))&&before.equals(capped.encode()),"A capped/no-benefit drive cannot manufacture history or a training change");
    var single=new ChaoState();single.train(1,1);single.train(3,1);
    DriveRules.apply(single,DriveRules.Tier.SOLID,2,new Random(2));
    check(single.lastTrainedStat()==2,"A typed combined drive remains a real latest single increase");
    int latest=single.lastTrainedStat();DriveRules.setTotal(single,2,0,new Random(2));
    check(single.lastTrainedStat()==latest,"A decrease changes appearance without claiming a new positive training event");
  }

  private static void trainingAndLoss(){
    var baby=new ChaoState();baby.setEvolutionStrength(.7f);
    for(int i=0;i<=20;i++){
      baby.align=i/40f;var p=HeroRunMorphology052.sample(baby);
      near(p.babyHero(),i/20f,"Hero alignment develops continuously up to its real first-evolution threshold");
      near(p.babyRun(),i/20f,"Twenty actual ordinary Run drives span the declared baby training range");
      near(p.maturity(),.7f,"Training and alignment do not fake baby maturity");
      if(i<20)baby.giveDrive(ChaoState.RUN);
    }
    var adult=heroAdult();
    for(int i=0;i<=40;i++){
      var p=HeroRunMorphology052.sample(adult);
      near(p.specialization(),i/40f,"Forty actual ordinary Run drives span the adult target");
      near(p.adultGrowth(),i/40f,"Mature Hero Run expresses its reversible target");
      if(i<40)adult.giveDrive(ChaoState.RUN);
    }
    int age=adult.adultAgeTicks;float strength=adult.evolutionStrength();
    for(int i=0;i<40;i++)adult.giveDrive(ChaoState.POWER);
    near(HeroRunMorphology052.sample(adult).specialization(),0,"Equal opposing Power training returns specialization to the fixed base Hero Run");
    check(adult.adultAgeTicks==age&&adult.evolutionStrength()==strength&&adult.primaryType==ChaoState.RUN_TYPE,"Reversal does not turn the adult young or change its first type");
    DriveRules.setTotal(adult,ChaoState.POWER,0,new Random(4));near(HeroRunMorphology052.sample(adult).specialization(),1,"Removing opposing training restores the current Run advantage");
    DriveRules.change(adult,ChaoState.RUN,-480,new Random(4));near(HeroRunMorphology052.sample(adult).specialization(),.5f,"Actual Run loss reverses the displayed target");
    adult.setEvolutionStrength(.6f);var p=HeroRunMorphology052.sample(adult);
    near(p.maturity(),.5f,"Biological development remains an independent scalar");near(p.specialization(),.5f,"Changing maturity does not rewrite training");near(p.adultGrowth(),.25f,"Visible specialization combines the separate age and training gates");
  }

  private static void allMutationRoutes(){
    var direct=heroAdult();direct.train(2,240,new Random(1));
    var drive=heroAdult();for(int i=0;i<10;i++)drive.giveDrive(2);
    var solid=heroAdult();DriveRules.apply(solid,DriveRules.Tier.SOLID,2,new Random(1));DriveRules.change(solid,2,140,new Random(1));
    var analyzer=heroAdult();AnalyzerStatRules.apply(analyzer,2,AnalyzerStatRules.LEVEL,1);AnalyzerStatRules.apply(analyzer,2,AnalyzerStatRules.LEVEL,1);AnalyzerStatRules.apply(analyzer,2,AnalyzerStatRules.PROGRESS,40);
    for(var s:new ChaoState[]{direct,drive,solid,analyzer})near(HeroRunMorphology052.sample(s).specialization(),.25f,"Equal actual training totals agree across ordinary/combined/direct/Creative routes");
    AnalyzerStatRules.apply(analyzer,2,AnalyzerStatRules.PROGRESS,16);near(HeroRunMorphology052.sample(analyzer).specialization(),.225f,"Creative partial-bar decreases are reflected immediately in the target");
    AnalyzerStatRules.apply(analyzer,2,AnalyzerStatRules.LEVEL,-1);near(HeroRunMorphology052.sample(analyzer).specialization(),116f/960,"Creative level decreases also reflect");
    var balanced=heroAdult();DriveRules.apply(balanced,DriveRules.Tier.CHAOTIC,-1,new Random(2));near(HeroRunMorphology052.sample(balanced).specialization(),0,"Balanced four-stat gain creates no invented Run-over-Power advantage");
    var perfect=heroAdult();Arrays.fill(perfect.levels,99);perfect.levels[2]=0;perfect.progress[2]=0;
    DriveRules.apply(perfect,DriveRules.Tier.PERFECT,-1,new Random(1));near(HeroRunMorphology052.sample(perfect).specialization(),0,"All equally maxed opposing stats remain balanced even after Perfect training");
    var maxBaby=new ChaoState();maxBaby.align=1;maxBaby.levels[2]=99;maxBaby.points[2]=3000;maxBaby.ageTicks=19;maxBaby.tick(20,10000,new Random(1));
    maxBaby.cocoonTicks=0;maxBaby.setEvolutionStrength(1.2f);near(HeroRunMorphology052.sample(maxBaby).adultGrowth(),1,"A baby already at the level cap can develop its adult morphology without an impossible post-evolution baseline");
    var fed=heroAdult();fed.levels[2]=10;fed.setEvolutionStrength(1);int adultAge=fed.adultAgeTicks;
    for(int i=0;i<40;i++)fed.onFruitEaten();
    near(HeroRunMorphology052.sample(fed).adultGrowth(),1,"The existing forty-whole-fruit maturity credit closes the age-only 1.0-to-1.2 gap without a lifespan overhaul");
    check(fed.adultAgeTicks==adultAge,"Fruit maturity credit never fabricates biological clock ticks");
  }

  private static void identityAndSaves(){
    var s=heroAdult();s.giveDrive(2);s.setTrait("reward.example",7);String identity=s.lineageId(),encoded=s.encode();
    var copy=ChaoState.decode(encoded);check(copy.encode().equals(encoded)&&copy.lineageId().equals(identity)&&copy.intTrait("reward.example")==7,"No schema or save-content migration is needed for the stat-derived policy");
    near(HeroRunMorphology052.sample(copy).adultGrowth(),HeroRunMorphology052.sample(s).adultGrowth(),"Reload retains exactly the same specialization");
    copy.align=-1;copy.primaryType=ChaoState.POWER_TYPE;
    check(HeroRunMorphology052.sample(copy).enabled()&&copy.lockedAlignment()==1&&copy.lockedPrimaryType()==ChaoState.RUN_TYPE,"Appearance reads the immutable adult identity despite stale public-field edits");
    copy=ChaoState.decode(copy.encode());check(copy.align==1&&copy.primaryType==ChaoState.RUN_TYPE,"Save normalization preserves the same Hero Run lock");
    int age=copy.adultAgeTicks;float strength=copy.evolutionStrength();copy.pet(-1);copy.feed(4,1,-1);
    check(copy.align==1&&copy.adultAgeTicks==age&&copy.evolutionStrength()==strength,"Adult care never changes alignment or biological age");
    copy.reincarnate();check(!copy.adult&&copy.primaryType==ChaoState.NORMAL&&copy.adultAgeTicks==0&&copy.evolutionStrength()==0,"A real new life resets maturity and first-evolution identity normally");
  }
  private static void readOnlyAndScope(){
    var raw=new ChaoState();raw.adult=true;raw.primaryType=ChaoState.RUN_TYPE;raw.align=.8f;raw.swimFly=2;raw.runPower=-2;raw.setEvolutionStrength(.6f);
    int[] points=raw.points.clone(),levels=raw.levels.clone();long entropy=raw.entropy;
    for(int i=0;i<20;i++)HeroRunMorphology052.sample(raw);
    check(raw.swimFly==2&&raw.runPower==-2&&raw.align==.8f&&Arrays.equals(points,raw.points)&&Arrays.equals(levels,raw.levels)&&raw.entropy==entropy,"Appearance sampling does not repair axes, create history or consume randomness");
    raw.primaryType=ChaoState.FLY_TYPE;check(raw.lockedPrimaryType()==ChaoState.FLY_TYPE,"Read-only sampling does not create an identity lock on an unsaved test fixture");
    check(!HeroRunMorphology052.sample(raw).enabled(),"Other adult primary types stay outside the prototype");
    for(int excluded=0;excluded<4;excluded++){
      var s=heroAdult();if(excluded==0)s.dead=true;if(excluded==1)s.immortal=true;if(excluded==2)s.character="tails";if(excluded==3)s.variant="nether";
      check(!HeroRunMorphology052.sample(s).enabled(),"Dead, Chaos, character and themed forms retain their own renderer paths");
    }
    var baby=new ChaoState();baby.align=.2f;baby.points[1]=100;
    check(HeroRunMorphology052.sample(baby).enabled(),"A baby's discrete prospective type does not gate its continuous developing surface");
    baby.points[2]=101;check(HeroRunMorphology052.sample(baby).enabled(),"Crossing a prospective first-type tie cannot pop the prototype switch");
  }
}
