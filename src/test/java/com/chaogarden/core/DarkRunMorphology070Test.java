package com.chaogarden.core;

import java.util.Arrays;
import java.util.Random;

/** Saved Dark/Run identity, real training and independent opposed axes.
 * Geometry and actual appearance are checked separately on the production rig. */
public final class DarkRunMorphology070Test {
  private static int checks;
  private static void check(boolean value,String why){checks++;if(!value)throw new AssertionError(why);}
  private static void near(float actual,float expected,String why){check(Float.isFinite(actual)&&Math.abs(actual-expected)<.000004f,why+": "+actual+" vs "+expected);}
  public static int run(){
    checks=0;blendScopeGuards();untouchedFirstEvolution();nineCombinations();realMutationRoutes();allCombinedDriveTiers();creativeCommandAndMaturity();currentLabels();oppositionAndAge();identityAndPersistence();neutralRegression();flyAndOtherFamilyRegression();scopeAndReadOnly();heroSwimRegression();heroFlyRegression();heroRunRegression();heroPowerRegression();darkSwimRegression();darkFlyRegression();babyPrototypePreserved();
    System.out.println("Dark Run morphology070: "+checks+" checks passed; nine combinations, real training, independent age/opposed axes, immutable first identity and exact persistence");
    return checks;
  }
  private static void blendScopeGuards(){
    near(DarkRunMorphology070.SPECIALIZATION_PROGRESS,960,"Declared provisional threshold remains 960 actual bar progress");
    for(var sample:new DarkRunMorphology070.Sample[]{null,new DarkRunMorphology070.Sample(false,1,1,1)}){
      var weights=DarkRunMorphology070.weights(sample);near(weights[0],1,"Disabled input retains fresh surface");
      for(int i=1;i<10;i++)near(weights[i],0,"Disabled input emits no mature corner");
    }
    for(float rp:new float[]{-3,-1,0,.375f,1,3,Float.NaN,Float.POSITIVE_INFINITY})for(float sf:new float[]{-3,-1,0,.375f,1,3,Float.NaN,Float.NEGATIVE_INFINITY})for(float age:new float[]{-1,0,.375f,1,2,Float.NaN}){
      var weights=DarkRunMorphology070.weights(new DarkRunMorphology070.Sample(true,age,rp,sf));
      float total=0,actualRp=0,actualSf=0;for(int i=0;i<weights.length;i++){
        float w=weights[i];check(Float.isFinite(w)&&w>=0&&w<=1,"Explicit blend inputs stay finite and convex");total+=w;
        if(i>0){actualRp+=w*((i-1)%3-1);actualSf+=w*((i-1)/3-1);}
      }
      float a=Float.isFinite(age)?Math.clamp(age,0,1):0;
      near(total,1,"Explicit blend inputs conserve total mass");
      near(actualRp,a*(Float.isFinite(rp)?Math.clamp(rp,-1,1):0),"Mature target mass reproduces the full signed Run/Power coordinate");
      near(actualSf,a*(Float.isFinite(sf)?Math.clamp(sf,-1,1):0),"Mature target mass reproduces both Fly and Swim coordinates");
    }
  }
  private static ChaoState adult(){
    var s=new ChaoState();s.adult=true;s.primaryType=ChaoState.RUN_TYPE;s.align=-1;s.ageTicks=400;s.adultAgeTicks=90;
    s.hunger=80;s.energy=100;s.setEvolutionStrength(1.2f);return ChaoState.decode(s.encode());
  }
  private static void setAxis(ChaoState s,int runPower,int swimFly){
    for(int i=0;i<4;i++)DriveRules.setTotal(s,i,0,new Random(64));
    if(runPower!=0)DriveRules.setTotal(s,runPower>0?ChaoState.POWER:ChaoState.RUN,Math.abs(runPower),new Random(64));
    if(swimFly!=0)DriveRules.setTotal(s,swimFly>0?ChaoState.FLY:ChaoState.SWIM,Math.abs(swimFly),new Random(64));
  }
  private static void untouchedFirstEvolution(){
    var s=new ChaoState();s.ageTicks=19;s.align=-.8f;s.points[ChaoState.STAMINA]=9999;s.intelligence=4000;s.luck=4000;
    s.train(ChaoState.RUN,960,new Random(70));
    check(s.prospectivePrimaryType()==ChaoState.RUN_TYPE,"Highest Run earned points select first Run despite Stamina");
    check(s.tick(20,100000,new Random(70))==ChaoState.Transition.EVOLVED,"Actual ordinary clock completes first Run evolution");
    check(s.adult&&s.lockedPrimaryType()==ChaoState.RUN_TYPE&&s.lockedAlignment()==-.8f,"Natural evolution locks Dark alignment and Run first identity");
    var fresh=DarkRunMorphology070.sample(s);check(fresh.enabled(),"Naturally obtained Dark Run uses the expanded path");
    near(fresh.maturity(),0,"Carried baby training cannot skip fresh biological growth");near(fresh.runPower(),-1,"Baby Run training carries into targeting without resetting");near(fresh.swimFly(),0,"Fresh Run identity does not invent an unrelated Fly or Swim tendency");
    while(s.cocoonTicks>0)s.tick(20,100000,new Random(70));
    for(int i=0;i<40;i++)s.giveDrive(ChaoState.POWER);
    near(DarkRunMorphology070.sample(s).runPower(),0,"Forty opposite Power drives reach balance while first Run stays locked");
    for(int i=0;i<40;i++)s.giveDrive(ChaoState.POWER);
    check(s.lockedPrimaryType()==ChaoState.RUN_TYPE,"Later opposite Power cannot rewrite first Run identity");near(DarkRunMorphology070.sample(s).runPower(),1,"Excess Power develops the positive secondary axis while remaining Run-first");
    for(int stat=0;stat<4;stat++){
      var trained=new ChaoState();trained.ageTicks=19;trained.align=-1;trained.points[stat]=20;trained.levels[(stat+1)%4]=99;trained.points[ChaoState.STAMINA]=9999;
      trained.tick(20,100000,new Random(70));check(trained.lockedPrimaryType()==stat+1,"First type uses earned points instead of unrelated higher levels");
      check(DarkRunMorphology070.sample(trained).enabled()==(stat==ChaoState.RUN),"Only first Run enters the scoped new Dark family");
    }
    for(int a=0;a<4;a++)for(int b=0;b<4;b++)if(a!=b){
      var tied=new ChaoState();tied.align=-1;tied.train(a,1);tied.train(b,1);tied.points[a]=tied.points[b]=100;tied.points[ChaoState.STAMINA]=9999;
      check(tied.prospectivePrimaryType()==b+1,"A positive earned-point tie uses latest actual training");
      var copy=ChaoState.decode(tied.encode());check(copy.prospectivePrimaryType()==b+1,"Tie history survives existing save format");
      copy.ageTicks=19;copy.tick(20,100000,new Random(70));check(copy.lockedPrimaryType()==b+1,"Actual tied first evolution retains saved recency");
      check(DarkRunMorphology070.sample(copy).enabled()==(b==ChaoState.RUN),"Tied first identity is scoped by the saved result");
    }
    var untouched=new ChaoState();untouched.align=-1;untouched.ageTicks=19;untouched.tick(20,100000,new Random(70));
    check(untouched.lockedPrimaryType()==ChaoState.NORMAL&&!DarkRunMorphology070.sample(untouched).enabled(),"Untouched Dark Normal stays outside the new path");
  }
  private static void nineCombinations(){
    for(int rp=-1;rp<=1;rp++)for(int sf=-1;sf<=1;sf++)for(int quarter=0;quarter<=4;quarter++){
      var s=adult();setAxis(s,rp*960,sf*960);s.setEvolutionStrength(quarter*.3f);String encoded=s.encode();
      var p=DarkRunMorphology070.sample(s);check(p.enabled(),"All nine requested axis combinations remain on Dark/Run");
      near(p.runPower(),rp,"Current Run/Power bars select the intended signed axis");near(p.swimFly(),sf,"Current Swim/Fly bars select the other signed axis");
      near(p.maturity(),quarter*.25f,"The same training state has independent young/intermediate/mature development");
      var weights=DarkRunMorphology070.weights(p);check(weights.length==10,"Ten target storage slots retain shared grid layout");
      float sum=0;for(float w:weights){check(Float.isFinite(w)&&w>=0&&w<=1,"All blend weights are finite, convex and bounded");sum+=w;}
      near(sum,1,"No unsupported combination creates missing or overlapping weight mass");near(weights[0],1-quarter*.25f,"Fresh identity recedes continuously with biological maturity");
      int expected=1+(sf+1)*3+(rp+1);
      for(int i=1;i<weights.length;i++)near(weights[i],i==expected?quarter*.25f:0,"At an exact corner only that mature combination contributes");
      check(encoded.equals(s.encode()),"Sampling and blending are completely read-only");
      var restored=ChaoState.decode(encoded);check(restored.encode().equals(encoded)&&DarkRunMorphology070.sample(restored).equals(p),"Exact save/reload preserves each combination and independent maturity");
    }
    for(int rp=-4;rp<=4;rp++)for(int sf=-4;sf<=4;sf++){
      var s=adult();setAxis(s,rp*120,sf*120);s.setEvolutionStrength(.6f);var p=DarkRunMorphology070.sample(s);var w=DarkRunMorphology070.weights(p);
      float sum=0;int used=0;for(float f:w){sum+=f;if(f>0)used++;}
      near(p.runPower(),rp*.125f,"Intermediate Run/Power input remains continuous");near(p.swimFly(),sf*.125f,"Intermediate Swim/Fly input remains continuous");
      near(sum,1,"Interior combinations remain convex");check(used<=5,"At most four mature neighbors plus the fresh corner blend together");
    }
  }
  private static void realMutationRoutes(){
    for(int stat=0;stat<4;stat++){
      var direct=adult();var ordinary=adult();var combined=adult();var analyzer=adult();
      direct.train(stat,240,new Random(64));for(int i=0;i<10;i++)ordinary.giveDrive(stat);
      DriveRules.apply(combined,DriveRules.Tier.SOLID,stat,new Random(64));DriveRules.change(combined,stat,140,new Random(64));
      AnalyzerStatRules.apply(analyzer,stat,AnalyzerStatRules.LEVEL,1);AnalyzerStatRules.apply(analyzer,stat,AnalyzerStatRules.LEVEL,1);AnalyzerStatRules.apply(analyzer,stat,AnalyzerStatRules.PROGRESS,40);
      for(var s:new ChaoState[]{direct,ordinary,combined,analyzer}){
        var p=DarkRunMorphology070.sample(s);near(stat<2?p.swimFly():p.runPower(),stat==ChaoState.SWIM||stat==ChaoState.RUN?-.25f:.25f,"Equal actual training totals agree across ordinary, typed combined, direct and Analyzer changes");
        near(p.maturity(),1,"None of the ordinary training routes fabricates biological youth or age");
        check(s.lockedPrimaryType()==ChaoState.RUN_TYPE&&s.lockedAlignment()==-1,"Every route preserves the first evolution identity");
      }
      AnalyzerStatRules.apply(analyzer,stat,AnalyzerStatRules.PROGRESS,0);
      near(Math.abs(stat<2?DarkRunMorphology070.sample(analyzer).swimFly():DarkRunMorphology070.sample(analyzer).runPower()),200f/960,"Partial-bar Analyzer reductions recede the correct axis");
      AnalyzerStatRules.apply(analyzer,stat,AnalyzerStatRules.LEVEL,-1);
      near(Math.abs(stat<2?DarkRunMorphology070.sample(analyzer).swimFly():DarkRunMorphology070.sample(analyzer).runPower()),100f/960,"Level reductions recede the correct axis");
    }
    var balanced=adult();DriveRules.apply(balanced,DriveRules.Tier.CHAOTIC,-1,new Random(64));
    near(DarkRunMorphology070.sample(balanced).runPower(),0,"A balanced multi-stat transaction invents no Run/Power advantage");near(DarkRunMorphology070.sample(balanced).swimFly(),0,"A balanced multi-stat transaction invents no Swim/Fly advantage");
    var perfect=adult();Arrays.fill(perfect.levels,99);perfect.levels[ChaoState.STAMINA]=0;DriveRules.apply(perfect,DriveRules.Tier.PERFECT,-1,new Random(64));
    near(DarkRunMorphology070.sample(perfect).runPower(),0,"Equally capped opponents remain balanced after a Perfect drive");near(DarkRunMorphology070.sample(perfect).swimFly(),0,"Perfect drive preserves the other independent balance");
  }
  private static void allCombinedDriveTiers(){
    for(var tier:DriveRules.Tier.values())for(int selected=0;selected<4;selected++){
      var s=adult();setAxis(s,480,-240);int age=s.ageTicks,adultAge=s.adultAgeTicks;float maturity=s.evolutionStrength();String lineage=s.lineageId();
      check(DriveRules.apply(s,tier,DriveRules.typed(tier)?selected:-1,new Random(6400+tier.ordinal()*4+selected)),"Actual combined-drive tier applies: "+tier);
      var p=DarkRunMorphology070.sample(s);
      near(p.runPower(),Math.clamp((DriveRules.total(s,ChaoState.POWER)-DriveRules.total(s,ChaoState.RUN))/960f,-1,1),"Every actual combined tier uses current opposed Run/Power bars: "+tier);
      near(p.swimFly(),Math.clamp((DriveRules.total(s,ChaoState.FLY)-DriveRules.total(s,ChaoState.SWIM))/960f,-1,1),"Every actual combined tier uses current opposed Swim/Fly bars: "+tier);
      for(int stat=0;stat<4;stat++)check(DriveRules.total(s,stat)>=0&&DriveRules.total(s,stat)<=9900,"Combined mutation remains in the actual level-bar range");
      check(s.ageTicks==age&&s.adultAgeTicks==adultAge&&s.evolutionStrength()==maturity&&s.lockedPrimaryType()==ChaoState.RUN_TYPE&&s.lockedAlignment()==-1&&s.lineageId().equals(lineage),"Every combined tier preserves age and first Run identity");
      var copy=ChaoState.decode(s.encode());check(copy.encode().equals(s.encode())&&DarkRunMorphology070.sample(copy).equals(p),"Every actual combined tier persists its exact target");
    }
  }
  private static void creativeCommandAndMaturity(){
    for(int rp=-1;rp<=1;rp++)for(int sf=-1;sf<=1;sf++){
      var s=adult();int age=s.ageTicks,adultAge=s.adultAgeTicks;
      for(int stat:new int[]{rp<0?ChaoState.RUN:rp>0?ChaoState.POWER:-1,sf<0?ChaoState.SWIM:sf>0?ChaoState.FLY:-1})if(stat>=0)
        check(CreativeChaoEditRules056.apply(s,-100,stat,10),"Actual absolute Creative edit accepts each compatible selected stat");
      var target=DarkRunMorphology070.sample(s);near(target.runPower(),rp,"Absolute command levels derive the correct current Run/Power axis");near(target.swimFly(),sf,"Absolute command levels derive the correct current Swim/Fly axis");
      check(s.lockedPrimaryType()==ChaoState.RUN_TYPE&&s.lockedAlignment()==-1,"Adult command cannot convert the first Dark/Run identity");
      String before=s.encode();check(!CreativeChaoEditRules056.apply(s,100,ChaoState.RUN,99)&&s.encode().equals(before),"Conflicting adult alignment rejects the entire command transaction");
      check(MaturityEditRules056.applyMilli(s,250),"Existing stage-safe maturity control accepts a younger adult growth fraction");
      var young=DarkRunMorphology070.sample(s);near(young.maturity(),.25f,"Maturity control changes biological growth independently of training");near(young.runPower(),rp,"Maturity editing retains current training");near(young.swimFly(),sf,"Maturity editing retains the compatible axis");
      check(s.ageTicks==age&&s.adultAgeTicks==adultAge&&s.adult&&!s.dead&&s.cocoonTicks==0,"Explicit growth edits never reset lifespan or invoke another first cocoon");
      check(MaturityEditRules056.applyMilli(s,1000),"The full adult maturity endpoint is reachable for Creative testing");near(DarkRunMorphology070.sample(s).maturity(),1,"Full adult control reaches the mature corner");
      var copy=ChaoState.decode(s.encode());check(copy.encode().equals(s.encode())&&DarkRunMorphology070.sample(copy).equals(DarkRunMorphology070.sample(s)),"Combined command and maturity inputs persist exactly");
      check(s.tick(100000,100000,new Random(64))==ChaoState.Transition.NONE&&s.adult&&s.lockedPrimaryType()==ChaoState.RUN_TYPE,"Normal ticking after a growth edit cannot duplicate first evolution");
    }
  }
  private static void oppositionAndAge(){
    var s=adult();setAxis(s,960,960);s.setEvolutionStrength(.6f);int age=s.ageTicks,adultAge=s.adultAgeTicks;float strength=s.evolutionStrength();String lineage=s.lineageId();
    for(int i=0;i<80;i++){
      s.giveDrive(ChaoState.RUN);var p=DarkRunMorphology070.sample(s);
      near(p.runPower(),Math.clamp((960-(i+1)*24)/960f,-1,1),"Actual opposing Run crosses Power through balance into negative Run");
      near(p.swimFly(),1,"Compatible Fly survives the complete Run/Power reversal");
      check(s.ageTicks==age&&s.adultAgeTicks==adultAge&&s.evolutionStrength()==strength&&s.lockedPrimaryType()==ChaoState.RUN_TYPE&&s.lockedAlignment()==-1&&s.lineageId().equals(lineage),"Opposite training changes neither chronology, biological maturity nor locked lineage/identity");
    }
    near(DarkRunMorphology070.sample(s).runPower(),-1,"Excess Run produces a real Run specialization within Run-first identity");
    for(int i=0;i<80;i++){
      s.giveDrive(ChaoState.SWIM);var p=DarkRunMorphology070.sample(s);
      near(p.swimFly(),Math.clamp((960-(i+1)*24)/960f,-1,1),"Actual opposing Swim crosses Fly through balance into Swim");
      near(p.runPower(),-1,"Run survives independent Fly/Swim reversal");
    }
    near(DarkRunMorphology070.weights(DarkRunMorphology070.sample(s))[1],.5f,"Half-mature Run/Swim uses the distinct negative-Run negative-Swim corner");
    DriveRules.change(s,ChaoState.RUN,-1920,new Random(64));DriveRules.change(s,ChaoState.SWIM,-1920,new Random(64));
    near(DarkRunMorphology070.sample(s).runPower(),1,"Run reduction reveals the saved Power training again");near(DarkRunMorphology070.sample(s).swimFly(),1,"Swim reduction restores the independent Fly target");
    check(s.ageTicks==age&&s.adultAgeTicks==adultAge&&s.evolutionStrength()==strength&&s.lockedPrimaryType()==ChaoState.RUN_TYPE,"Reductions reverse specialization without biological youth or another first evolution");
    for(int a:new int[]{ChaoState.RUN,ChaoState.POWER})for(int b:new int[]{ChaoState.FLY,ChaoState.SWIM}){
      var first=adult();var reverse=adult();for(int i=0;i<40;i++)first.giveDrive(a);for(int i=0;i<40;i++)first.giveDrive(b);for(int i=0;i<40;i++)reverse.giveDrive(b);for(int i=0;i<40;i++)reverse.giveDrive(a);
      check(DarkRunMorphology070.sample(first).equals(DarkRunMorphology070.sample(reverse)),"Compatible pair order is one form on either side of both opposed axes");
    }
  }
  private static void currentLabels(){
    var empty=adult();empty.runPower=1;empty.swimFly=-1;
    check(empty.typeName().equals("Run/Normal")&&empty.secondaryFormKey().equals("normal"),"No current training retains balanced labels despite stale legacy axes");
    near(DarkRunMorphology070.sample(empty).runPower(),0,"Empty current bars contain no Run/Power influence");near(DarkRunMorphology070.sample(empty).swimFly(),0,"Empty current bars contain no Swim/Fly influence");
    String[] rpNames={"Run","Normal","Power"},sfNames={"Swim","Normal","Fly"};
    for(int rp=-1;rp<=1;rp++)for(int sf=-1;sf<=1;sf++){
      var s=adult();s.runPower=-rp;s.swimFly=-sf;
      for(int stat:new int[]{rp<0?ChaoState.RUN:rp>0?ChaoState.POWER:-1,sf<0?ChaoState.SWIM:sf>0?ChaoState.FLY:-1})if(stat>=0){AnalyzerStatRules.apply(s,stat,AnalyzerStatRules.LEVEL,10);AnalyzerStatRules.apply(s,stat,AnalyzerStatRules.LEVEL,-1);AnalyzerStatRules.apply(s,stat,AnalyzerStatRules.PROGRESS,60);}
      String label=sf==0?rpNames[rp+1]:rp==0?sfNames[sf+1]:sfNames[sf+1]+"+"+rpNames[rp+1];
      check(s.typeName().equals("Run/"+label),"Analyzer labels describe current-bar Run-first combination despite stale legacy axes");
      check(s.secondaryFormKey().equals(label.toLowerCase(java.util.Locale.ROOT).replace('+','_')),"Combination key matches display and is independent of train order");
      var copy=ChaoState.decode(s.encode());check(copy.typeName().equals(s.typeName())&&copy.secondaryFormKey().equals(s.secondaryFormKey()),"Current labels persist without rewriting legacy axes");
      for(int stat=0;stat<4;stat++)DriveRules.setTotal(s,stat,0,new Random(64));DriveRules.setTotal(s,ChaoState.POWER,960,new Random(64));
      check(s.typeName().equals("Run/Power")&&s.secondaryFormKey().equals("power"),"Opposite Power retains Run first identity and reports the actual later specialization");
    }
  }
  private static void identityAndPersistence(){
    var s=adult();setAxis(s,-480,720);s.setTrait("darkrun70.foreign","unrelated earned data");s.expressedColor=2;s.shiny=true;s.expressedJewel=3;
    s.animalMask=ChaoState.ALL_ANIMALS;String encoded=s.encode(),lineage=s.lineageId();var p=DarkRunMorphology070.sample(s);
    var restored=ChaoState.decode(encoded);check(restored.encode().equals(encoded)&&restored.lineageId().equals(lineage)&&restored.trait("darkrun70.foreign").equals("unrelated earned data"),"Round trip preserves genetics, DNA, lineage and unrelated traits");
    check(DarkRunMorphology070.sample(restored).equals(p),"Color, jewel and shiny finish do not change morphology inputs");
    for(int color=0;color<ChaoState.COLORS.length;color++)for(boolean shine:new boolean[]{false,true})for(boolean twoTone:new boolean[]{false,true}){
      restored.expressedColor=color;restored.shiny=shine;restored.twoTone=twoTone;
      check(DarkRunMorphology070.sample(restored).equals(p),"Named, monocolor and shiny finishes share the same Dark Run shape inputs");
    }
    for(int jewel=0;jewel<ChaoState.JEWELS.length;jewel++){
      restored.expressedJewel=jewel;check(DarkRunMorphology070.sample(restored).equals(p),"Every jewel finish retains the same morphology policy");
    }
    restored.align=1;restored.primaryType=ChaoState.SWIM_TYPE;
    check(DarkRunMorphology070.sample(restored).enabled()&&restored.lockedPrimaryType()==ChaoState.RUN_TYPE&&restored.lockedAlignment()==-1,"Stale public fields cannot turn an established Dark Run adult into a Hero or Swim first form");
    restored=ChaoState.decode(restored.encode());check(restored.align==-1&&restored.primaryType==ChaoState.RUN_TYPE,"Persistence restores the same first-evolution lock");
    var hero=new ChaoState();hero.adult=true;hero.primaryType=ChaoState.RUN_TYPE;hero.align=1;hero.setEvolutionStrength(1.2f);hero=ChaoState.decode(hero.encode());setAxis(hero,-960,960);
    var h=HeroRunMorphology052.sample(hero);check(h.enabled()&&!DarkRunMorphology070.sample(hero).enabled(),"Working Hero Run still uses its existing production path");near(h.adultGrowth(),1,"Compatible Fly cannot erase the approved mature Hero/Run/Run development");
  }
  private static void neutralRegression(){
    for(float align:new float[]{0,1}){var s=new ChaoState();s.adult=true;s.align=align;s.primaryType=ChaoState.NORMAL;s.setEvolutionStrength(.6f);s=ChaoState.decode(s.encode());setAxis(s,-960,960);
      check(!DarkRunMorphology070.sample(s).enabled(),"Existing Normal-first families remain outside new Run-first path");
      if(align==0){var p=NeutralMorphology056.sample(s);check(p.enabled(),"Delivered Neutral Normal stays independently selected");near(p.swimFly(),1,"Delivered Neutral Fly axis stays unchanged");}
      else{var p=HeroNormalMorphology057.sample(s);check(p.enabled(),"Delivered Hero Normal stays independently selected");near(p.swimFly(),1,"Delivered Hero Fly axis stays unchanged");}
    }
  }
  private static void flyAndOtherFamilyRegression(){
    for(float rp:new float[]{-1,0,1})for(float sf:new float[]{-1,0,1}){
      var s=new ChaoState();s.adult=true;s.align=0;s.primaryType=ChaoState.FLY_TYPE;s.setEvolutionStrength(.6f);s=ChaoState.decode(s.encode());setAxis(s,Math.round(rp*960),Math.round(sf*960));
      check(!DarkRunMorphology070.sample(s).enabled(),"Completed Neutral Fly-first family stays outside new Run-first selection");
      var p=NeutralFlyMorphology061.sample(s);check(p.enabled(),"Completed Neutral Fly-first family remains selected");near(p.maturity(),.5f,"Existing Fly age remains separate");near(p.runPower(),rp,"Existing Fly Run/Power policy remains unchanged");near(p.swimFly(),sf,"Neutral Fly-first retains its complete signed Swim/Fly coordinate");
      var run=new ChaoState();run.adult=true;run.align=0;run.primaryType=ChaoState.RUN_TYPE;run.setEvolutionStrength(.6f);run=ChaoState.decode(run.encode());setAxis(run,Math.round(rp*960),Math.round(sf*960));
      check(!DarkRunMorphology070.sample(run).enabled(),"Completed Neutral Run-first stays outside new Run-first selection");
      var oldRun=NeutralRunMorphology062.sample(run);check(oldRun.enabled(),"Completed Neutral Run-first remains selected");near(oldRun.maturity(),.5f,"Existing Run age remains separate");near(oldRun.runPower(),rp,"Existing Run signed Run/Power remains exact");near(oldRun.swimFly(),sf,"Existing Run signed Swim/Fly remains exact");
      var swim=new ChaoState();swim.adult=true;swim.align=0;swim.primaryType=ChaoState.SWIM_TYPE;swim.setEvolutionStrength(.6f);swim=ChaoState.decode(swim.encode());setAxis(swim,Math.round(rp*960),Math.round(sf*960));
      check(!DarkRunMorphology070.sample(swim).enabled()&&NeutralSwimMorphology059.sample(swim).enabled(),"Completed Neutral Swim-first path remains independently selected");
      var power=new ChaoState();power.adult=true;power.align=0;power.primaryType=ChaoState.POWER_TYPE;power.setEvolutionStrength(.6f);power=ChaoState.decode(power.encode());setAxis(power,Math.round(rp*960),Math.round(sf*960));
      check(!DarkRunMorphology070.sample(power).enabled(),"Completed Neutral Power-first remains outside new Dark Run selection");
      var oldPower=NeutralPowerMorphology063.sample(power);check(oldPower.enabled(),"Completed Neutral Power-first remains selected");near(oldPower.maturity(),.5f,"Existing Power age remains separate");near(oldPower.runPower(),rp,"Existing Power full signed Run/Power remains exact");near(oldPower.swimFly(),sf,"Existing Power full signed Swim/Fly remains exact");
      var dark=new ChaoState();dark.adult=true;dark.align=-1;dark.primaryType=ChaoState.NORMAL;dark.setEvolutionStrength(.6f);dark=ChaoState.decode(dark.encode());setAxis(dark,Math.round(rp*960),Math.round(sf*960));
      check(!DarkRunMorphology070.sample(dark).enabled()&&DarkNormalMorphology060.sample(dark).enabled(),"Completed Dark Normal-first path remains independently selected");
    }
  }

  private static void scopeAndReadOnly(){
    var s=adult();s.swimFly=Float.NaN;s.runPower=Float.POSITIVE_INFINITY;Arrays.fill(s.points,9999);Arrays.fill(s.levels,99);s.progress[0]=99;s.levels[1]=-10;
    int[] points=s.points.clone(),levels=s.levels.clone();long entropy=s.entropy;var p=DarkRunMorphology070.sample(s);
    check(p.enabled()&&Float.isFinite(p.runPower())&&Float.isFinite(p.swimFly()),"Corrupt legacy visual axes cannot poison current-bar morphology");
    check(Arrays.equals(s.points,points)&&Arrays.equals(s.levels,levels)&&s.entropy==entropy&&Float.isNaN(s.swimFly)&&Float.isInfinite(s.runPower),"The read-only policy does not normalize, consume entropy or repair public fields");
    for(float align:new float[]{1,.5f,0,-.49999f}){var excluded=new ChaoState();excluded.adult=true;excluded.primaryType=ChaoState.RUN_TYPE;excluded.align=align;excluded=ChaoState.decode(excluded.encode());check(!DarkRunMorphology070.sample(excluded).enabled(),"Neutral and Hero alignments remain outside Dark Run");}
    for(float align:new float[]{-.5f,-.8f,-1}){var admitted=new ChaoState();admitted.adult=true;admitted.primaryType=ChaoState.RUN_TYPE;admitted.align=align;admitted=ChaoState.decode(admitted.encode());check(DarkRunMorphology070.sample(admitted).enabled(),"Exact Dark boundary and stronger Dark alignments admit locked Run-first path");}
    for(int gate=0;gate<6;gate++){
      var excluded=adult();if(gate==0)excluded.adult=false;if(gate==1)excluded.dead=true;if(gate==2)excluded.immortal=true;if(gate==3)excluded.character="tails";if(gate==4)excluded.variant="nether";if(gate==5)excluded.primaryType=ChaoState.SWIM_TYPE;
      // Editing the public first type after a lock is deliberately ignored;
      // the non-Run scope fixture needs a fresh, then saved identity.
      if(gate==5){excluded=new ChaoState();excluded.adult=true;excluded.primaryType=ChaoState.SWIM_TYPE;excluded.align=-1;excluded=ChaoState.decode(excluded.encode());}
      check(!DarkRunMorphology070.sample(excluded).enabled(),"Child/dead/Chaos/character/theme/other-first forms keep their existing paths");
    }
    check(!DarkRunMorphology070.sample(null).enabled(),"Missing state is safely disabled");
  }

  private static void heroSwimRegression(){
    for(int rp=-1;rp<=1;rp++)for(int sf=-1;sf<=1;sf++)for(float age:new float[]{0,.5f,1}){
      var s=new ChaoState();s.adult=true;s.align=1;s.primaryType=ChaoState.SWIM_TYPE;s.setEvolutionStrength(age*1.2f);s=ChaoState.decode(s.encode());setAxis(s,rp*960,sf*960);
      String saved=s.encode();var old=HeroSwimMorphology064.sample(s);
      check(old.enabled()&&!DarkRunMorphology070.sample(s).enabled(),"Latest delivered Hero Swim remains on its own production path");
      near(old.runPower(),rp,"Hero Swim signed Run/Power is preserved");near(old.swimFly(),sf,"Hero Swim signed Swim/Fly is preserved");near(old.maturity(),age,"Hero Swim independent age is preserved");
      check(saved.equals(s.encode()),"New family admission/sampling does not mutate an existing saved Hero Swim");
      check(HeroSwimMorphology064.sample(ChaoState.decode(saved)).equals(old),"Existing Hero Swim inputs survive exact reload");
    }
  }

  private static void heroFlyRegression(){
    for(int rp=-1;rp<=1;rp++)for(int sf=-1;sf<=1;sf++)for(float age:new float[]{0,.5f,1}){
      var s=new ChaoState();s.adult=true;s.align=1;s.primaryType=ChaoState.FLY_TYPE;s.setEvolutionStrength(age*1.2f);s=ChaoState.decode(s.encode());setAxis(s,rp*960,sf*960);
      String saved=s.encode();var old=HeroFlyMorphology065.sample(s);
      check(old.enabled()&&!DarkRunMorphology070.sample(s).enabled(),"Latest delivered Hero Fly remains on its own production path");
      near(old.runPower(),rp,"Hero Fly signed Run/Power preserved");near(old.swimFly(),sf,"Hero Fly signed Swim/Fly preserved");near(old.maturity(),age,"Hero Fly independent age preserved");
      check(saved.equals(s.encode())&&HeroFlyMorphology065.sample(ChaoState.decode(saved)).equals(old),"Previous Hero Fly record remains exact and readonly");
    }
  }

  private static void heroRunRegression(){
    for(int rp=-1;rp<=1;rp++)for(int sf=-1;sf<=1;sf++)for(float age:new float[]{0,.5f,1}){
      var s=new ChaoState();s.adult=true;s.align=1;s.primaryType=ChaoState.RUN_TYPE;s.setEvolutionStrength(age*1.2f);s=ChaoState.decode(s.encode());setAxis(s,rp*960,sf*960);
      String saved=s.encode();var old=HeroRunMorphology066.sample(s);
      check(old.enabled()&&!DarkRunMorphology070.sample(s).enabled(),"Completed Hero Run066 remains independently selected");
      near(old.runPower(),rp,"Hero Run066 signed Run/Power preserved");near(old.swimFly(),sf,"Hero Run066 signed Swim/Fly preserved");near(old.maturity(),age,"Hero Run066 age remains independent");
      check(saved.equals(s.encode())&&HeroRunMorphology066.sample(ChaoState.decode(saved)).equals(old),"Current Hero Power save and sampling stay exact");
    }
  }

  private static void babyPrototypePreserved(){
    for(float align:new float[]{-1,0,.25f,.5f,1})for(float age:new float[]{0,.25f,.5f,.99f})for(int run:new int[]{0,120,240,480,960}){
      var s=new ChaoState();s.align=align;s.setEvolutionStrength(age);DriveRules.setTotal(s,ChaoState.RUN,run,new Random(66));
      String saved=s.encode();var before=HeroRunMorphology052.sample(s);var fresh=DarkRunMorphology070.sample(s);
      check(before.enabled()&&!fresh.enabled(),"Every ordinary baby remains on the approved052/055 route");
      near(before.babyRun(),Math.min(1,run/480f),"Baby training rule retains its existing480scale");near(before.maturity(),age,"Baby normalized strength remains unchanged");near(before.babyGrowth(),Math.min(1,run/480f)*age,"Baby age times training is not replaced by adult070weights");
      check(saved.equals(s.encode())&&HeroRunMorphology052.sample(ChaoState.decode(saved)).equals(before),"Sampling adult070 changes no baby saved data or prototype target");
    }
  }

  private static void heroPowerRegression(){
    for(int rp=-1;rp<=1;rp++)for(int sf=-1;sf<=1;sf++)for(float age:new float[]{0,.5f,1}){
      var s=new ChaoState();s.adult=true;s.align=1;s.primaryType=ChaoState.POWER_TYPE;s.setEvolutionStrength(age*1.2f);s=ChaoState.decode(s.encode());setAxis(s,rp*960,sf*960);
      String saved=s.encode();var old=HeroPowerMorphology067.sample(s);
      check(old.enabled()&&!DarkRunMorphology070.sample(s).enabled(),"Completed Hero Power067 remains independently selected");
      near(old.runPower(),rp,"Hero Power067 signed Run/Power preserved");near(old.swimFly(),sf,"Hero Power067 signed Swim/Fly preserved");near(old.maturity(),age,"Hero Power067 age remains independent");
      check(saved.equals(s.encode())&&HeroPowerMorphology067.sample(ChaoState.decode(saved)).equals(old),"Current Hero Power save and sampling stay exact");
    }
  }

  private static void darkSwimRegression(){
    for(int rp=-1;rp<=1;rp++)for(int sf=-1;sf<=1;sf++)for(float age:new float[]{0,.5f,1}){
      var s=new ChaoState();s.adult=true;s.align=-1;s.primaryType=ChaoState.SWIM_TYPE;s.setEvolutionStrength(age*1.2f);s=ChaoState.decode(s.encode());setAxis(s,rp*960,sf*960);
      String saved=s.encode();var old=DarkSwimMorphology068.sample(s);var added=DarkRunMorphology070.sample(s);
      check(old.enabled()&&!added.enabled(),"Completed Dark Swim068 stays separately selected even when later Fly dominates");
      near(old.runPower(),rp,"Dark Swim signed Run/Power preserved");near(old.swimFly(),sf,"Dark Swim signed Swim/Fly preserved");near(old.maturity(),age,"Dark Swim age remains independent");
      check(saved.equals(s.encode())&&DarkSwimMorphology068.sample(ChaoState.decode(saved)).equals(old),"Previous Dark Swim state and sampling stay exact and read-only");
    }
  }
  private static void darkFlyRegression(){
    for(int rp=-1;rp<=1;rp++)for(int sf=-1;sf<=1;sf++)for(float age:new float[]{0,.5f,1}){
      var s=new ChaoState();s.adult=true;s.align=-1;s.primaryType=ChaoState.FLY_TYPE;s.setEvolutionStrength(age*1.2f);s=ChaoState.decode(s.encode());setAxis(s,rp*960,sf*960);
      String saved=s.encode();var old=DarkFlyMorphology069.sample(s);var added=DarkRunMorphology070.sample(s);
      check(old.enabled()&&!added.enabled(),"Completed Dark Fly069 remains separately selected even when later Run dominates");
      near(old.runPower(),rp,"Dark Fly signed Run/Power preserved");near(old.swimFly(),sf,"Dark Fly signed Swim/Fly preserved");near(old.maturity(),age,"Dark Fly maturity remains independent");
      check(saved.equals(s.encode())&&DarkFlyMorphology069.sample(ChaoState.decode(saved)).equals(old),"Previous Dark Fly record remains exact and read-only");
    }
  }

}
