package com.chaogarden.core;

import java.util.Arrays;
import java.util.Random;

/** Saved Neutral/Fly identity, real training and independent opposed axes.
 * Geometry and actual appearance are checked separately on the production rig. */
public final class NeutralFlyMorphology061Test {
  private static int checks;
  private static void check(boolean value,String why){checks++;if(!value)throw new AssertionError(why);}
  private static void near(float actual,float expected,String why){check(Float.isFinite(actual)&&Math.abs(actual-expected)<.000004f,why+": "+actual+" vs "+expected);}
  public static int run(){
    checks=0;blendScopeGuards();untouchedFirstEvolution();sixCombinations();realMutationRoutes();creativeCommandAndMaturity();currentLabels();oppositionAndAge();identityAndPersistence();neutralRegression();scopeAndReadOnly();
    System.out.println("Neutral Fly morphology061: "+checks+" checks passed; six combinations, real training, independent age/opposed axes, immutable first identity and exact persistence");
    return checks;
  }
  private static void blendScopeGuards(){
    near(NeutralFlyMorphology061.SPECIALIZATION_PROGRESS,960,"Declared provisional threshold remains 960 actual bar progress");
    for(var sample:new NeutralFlyMorphology061.Sample[]{null,new NeutralFlyMorphology061.Sample(false,1,1,1)}){var weights=NeutralFlyMorphology061.weights(sample);near(weights[0],1,"Disabled input retains fresh surface");for(int i=1;i<10;i++)near(weights[i],0,"Disabled input emits no mature corner");}
    for(float rp:new float[]{-3,-1,0,.375f,1,3,Float.NaN,Float.POSITIVE_INFINITY})for(float sf:new float[]{-3,-1,0,.375f,1,3,Float.NaN,Float.NEGATIVE_INFINITY})for(float age:new float[]{-1,0,.375f,1,2,Float.NaN}){
      var weights=NeutralFlyMorphology061.weights(new NeutralFlyMorphology061.Sample(true,age,rp,sf));float total=0;for(float w:weights){check(Float.isFinite(w)&&w>=0&&w<=1,"Explicit blend inputs stay finite and convex");total+=w;}near(total,1,"Explicit blend inputs conserve total mass");for(int i=1;i<=3;i++)near(weights[i],0,"Unused negative-Swim guard row never contributes to Fly-first morphology");
    }
  }
  private static ChaoState adult(){
    var s=new ChaoState();s.adult=true;s.primaryType=ChaoState.FLY_TYPE;s.align=0;s.ageTicks=400;s.adultAgeTicks=90;
    s.hunger=80;s.energy=100;s.setEvolutionStrength(1.2f);return ChaoState.decode(s.encode());
  }
  private static void setAxis(ChaoState s,int runPower,int swimFly){
    for(int i=0;i<4;i++)DriveRules.setTotal(s,i,0,new Random(61));
    if(runPower!=0)DriveRules.setTotal(s,runPower>0?ChaoState.POWER:ChaoState.RUN,Math.abs(runPower),new Random(61));
    if(swimFly!=0)DriveRules.setTotal(s,swimFly>0?ChaoState.FLY:ChaoState.SWIM,Math.abs(swimFly),new Random(61));
  }
  private static void untouchedFirstEvolution(){
    var s=new ChaoState();s.ageTicks=19;s.align=.125f;s.points[ChaoState.STAMINA]=9999;s.intelligence=4000;s.luck=4000;
    s.train(ChaoState.FLY,960,new Random(61));
    check(s.prospectivePrimaryType()==ChaoState.FLY_TYPE,"Highest Fly earned points select the first Fly type despite Stamina");
    check(s.tick(20,100000,new Random(61))==ChaoState.Transition.EVOLVED,"Actual ordinary clock completes first Fly evolution");
    check(s.adult&&s.lockedPrimaryType()==ChaoState.FLY_TYPE&&s.lockedAlignment()==.125f,"Natural first evolution locks Neutral alignment and Fly first identity");
    var fresh=NeutralFlyMorphology061.sample(s);check(fresh.enabled(),"Naturally obtained Neutral Fly uses the new path");
    near(fresh.maturity(),0,"Training carried into adulthood cannot skip fresh biological growth");near(fresh.swimFly(),1,"Existing Fly training carries into the adult target without a reset");
    while(s.cocoonTicks>0)s.tick(20,100000,new Random(61));
    for(int i=0;i<40;i++)s.giveDrive(ChaoState.RUN);
    check(s.lockedPrimaryType()==ChaoState.FLY_TYPE,"Later Run training cannot rewrite first Fly identity");near(NeutralFlyMorphology061.sample(s).runPower(),-1,"Forty ordinary drives reach provisional Run target");
    for(int stat=0;stat<4;stat++){
      var trained=new ChaoState();trained.ageTicks=19;trained.align=0;trained.points[stat]=20;trained.levels[(stat+1)%4]=99;trained.points[ChaoState.STAMINA]=9999;
      trained.tick(20,100000,new Random(61));check(trained.lockedPrimaryType()==stat+1,"First type uses earned points instead of unrelated higher levels");
      check(NeutralFlyMorphology061.sample(trained).enabled()==(stat==ChaoState.FLY),"Only first Fly enters the scoped new family");
    }
    for(int a=0;a<4;a++)for(int b=0;b<4;b++)if(a!=b){
      var tied=new ChaoState();tied.train(a,1);tied.train(b,1);tied.points[a]=tied.points[b]=100;tied.points[ChaoState.STAMINA]=9999;
      check(tied.prospectivePrimaryType()==b+1,"A positive earned-point tie uses latest actual training");
      var copy=ChaoState.decode(tied.encode());check(copy.prospectivePrimaryType()==b+1,"Tie history survives existing save format");
      copy.ageTicks=19;copy.tick(20,100000,new Random(61));check(copy.lockedPrimaryType()==b+1,"Actual tied first evolution retains saved recency");
    }
    var untouched=new ChaoState();untouched.ageTicks=19;untouched.tick(20,100000,new Random(61));
    check(untouched.lockedPrimaryType()==ChaoState.NORMAL&&!NeutralFlyMorphology061.sample(untouched).enabled(),"Untouched Normal first evolution remains outside the new path");
  }
  private static void sixCombinations(){
    for(int rp=-1;rp<=1;rp++)for(int sf=0;sf<=1;sf++)for(int quarter=0;quarter<=4;quarter++){
      var s=adult();setAxis(s,rp*960,sf*960);s.setEvolutionStrength(quarter*.3f);String encoded=s.encode();
      var p=NeutralFlyMorphology061.sample(s);check(p.enabled(),"All six requested axis combinations remain on Neutral/Fly");
      near(p.runPower(),rp,"Current Run/Power bars select the intended signed axis");near(p.swimFly(),sf,"Current Swim/Fly bars select the other signed axis");
      near(p.maturity(),quarter*.25f,"The same training state has independent young/intermediate/mature development");
      var weights=NeutralFlyMorphology061.weights(p);check(weights.length==10,"Ten target storage slots retain shared grid layout");
      float sum=0;for(float w:weights){check(Float.isFinite(w)&&w>=0&&w<=1,"All blend weights are finite, convex and bounded");sum+=w;}
      near(sum,1,"No unsupported combination creates missing or overlapping weight mass");near(weights[0],1-quarter*.25f,"Fresh identity recedes continuously with biological maturity");
      int expected=1+(sf+1)*3+(rp+1);
      for(int i=1;i<weights.length;i++)near(weights[i],i==expected?quarter*.25f:0,"At an exact corner only that mature combination contributes");
      check(encoded.equals(s.encode()),"Sampling and blending are completely read-only");
      var restored=ChaoState.decode(encoded);check(restored.encode().equals(encoded)&&NeutralFlyMorphology061.sample(restored).equals(p),"Exact save/reload preserves each combination and independent maturity");
    }
    for(int rp=-4;rp<=4;rp++)for(int sf=-4;sf<=4;sf++){
      var s=adult();setAxis(s,rp*120,sf*120);s.setEvolutionStrength(.6f);var p=NeutralFlyMorphology061.sample(s);var w=NeutralFlyMorphology061.weights(p);
      float sum=0;int used=0;for(float f:w){sum+=f;if(f>0)used++;}
      near(p.runPower(),rp*.125f,"Intermediate Run/Power input remains continuous");near(p.swimFly(),Math.max(0,sf*.125f),"Intermediate Swim/Fly input remains continuous");
      near(sum,1,"Interior combinations remain convex");check(used<=5,"At most four mature neighbors plus the fresh corner blend together");
    }
  }
  private static void realMutationRoutes(){
    for(int stat=0;stat<4;stat++){
      var direct=adult();var ordinary=adult();var combined=adult();var analyzer=adult();
      direct.train(stat,240,new Random(61));for(int i=0;i<10;i++)ordinary.giveDrive(stat);
      DriveRules.apply(combined,DriveRules.Tier.SOLID,stat,new Random(61));DriveRules.change(combined,stat,140,new Random(61));
      AnalyzerStatRules.apply(analyzer,stat,AnalyzerStatRules.LEVEL,1);AnalyzerStatRules.apply(analyzer,stat,AnalyzerStatRules.LEVEL,1);AnalyzerStatRules.apply(analyzer,stat,AnalyzerStatRules.PROGRESS,40);
      for(var s:new ChaoState[]{direct,ordinary,combined,analyzer}){
        var p=NeutralFlyMorphology061.sample(s);near(stat<2?p.swimFly():p.runPower(),stat==ChaoState.SWIM?0:stat==ChaoState.RUN?-.25f:.25f,"Equal actual training totals agree across ordinary, typed combined, direct and Analyzer changes");
        near(p.maturity(),1,"None of the ordinary training routes fabricates biological youth or age");
        check(s.lockedPrimaryType()==ChaoState.FLY_TYPE&&s.lockedAlignment()==0,"Every route preserves the first evolution identity");
      }
      AnalyzerStatRules.apply(analyzer,stat,AnalyzerStatRules.PROGRESS,0);
      near(Math.abs(stat<2?NeutralFlyMorphology061.sample(analyzer).swimFly():NeutralFlyMorphology061.sample(analyzer).runPower()),(stat==ChaoState.SWIM?0:200f/960),"Partial-bar Analyzer reductions recede the correct axis");
      AnalyzerStatRules.apply(analyzer,stat,AnalyzerStatRules.LEVEL,-1);
      near(Math.abs(stat<2?NeutralFlyMorphology061.sample(analyzer).swimFly():NeutralFlyMorphology061.sample(analyzer).runPower()),(stat==ChaoState.SWIM?0:100f/960),"Level reductions recede the correct axis");
    }
    var balanced=adult();DriveRules.apply(balanced,DriveRules.Tier.CHAOTIC,-1,new Random(61));
    near(NeutralFlyMorphology061.sample(balanced).runPower(),0,"A balanced multi-stat transaction invents no Run/Power advantage");near(NeutralFlyMorphology061.sample(balanced).swimFly(),0,"A balanced multi-stat transaction invents no Swim/Fly advantage");
    var perfect=adult();Arrays.fill(perfect.levels,99);perfect.levels[ChaoState.STAMINA]=0;DriveRules.apply(perfect,DriveRules.Tier.PERFECT,-1,new Random(61));
    near(NeutralFlyMorphology061.sample(perfect).runPower(),0,"Equally capped opponents remain balanced after a Perfect drive");near(NeutralFlyMorphology061.sample(perfect).swimFly(),0,"Perfect drive preserves the other independent balance");
  }
  private static void creativeCommandAndMaturity(){
    for(int rp=-1;rp<=1;rp++)for(int sf=0;sf<=1;sf++){
      var s=adult();int age=s.ageTicks,adultAge=s.adultAgeTicks;
      for(int stat:new int[]{rp<0?ChaoState.RUN:rp>0?ChaoState.POWER:-1,sf<0?ChaoState.SWIM:sf>0?ChaoState.FLY:-1})if(stat>=0)
        check(CreativeChaoEditRules056.apply(s,0,stat,10),"Actual absolute Creative edit accepts each compatible selected stat");
      var target=NeutralFlyMorphology061.sample(s);near(target.runPower(),rp,"Absolute command levels derive the correct current Run/Power axis");near(target.swimFly(),sf,"Absolute command levels derive the correct current Swim/Fly axis");
      check(s.lockedPrimaryType()==ChaoState.FLY_TYPE&&s.lockedAlignment()==0,"Adult command cannot convert the first Neutral/Fly identity");
      String before=s.encode();check(!CreativeChaoEditRules056.apply(s,-100,ChaoState.RUN,99)&&s.encode().equals(before),"Conflicting adult alignment rejects the entire command transaction");
      check(MaturityEditRules056.applyMilli(s,250),"Existing stage-safe maturity control accepts a younger adult growth fraction");
      var young=NeutralFlyMorphology061.sample(s);near(young.maturity(),.25f,"Maturity control changes biological growth independently of training");near(young.runPower(),rp,"Maturity editing retains current training");near(young.swimFly(),sf,"Maturity editing retains the compatible axis");
      check(s.ageTicks==age&&s.adultAgeTicks==adultAge&&s.adult&&!s.dead&&s.cocoonTicks==0,"Explicit growth edits never reset lifespan or invoke another first cocoon");
      check(MaturityEditRules056.applyMilli(s,1000),"The full adult maturity endpoint is reachable for Creative testing");near(NeutralFlyMorphology061.sample(s).maturity(),1,"Full adult control reaches the mature corner");
      var copy=ChaoState.decode(s.encode());check(copy.encode().equals(s.encode())&&NeutralFlyMorphology061.sample(copy).equals(NeutralFlyMorphology061.sample(s)),"Combined command and maturity inputs persist exactly");
      check(s.tick(100000,100000,new Random(61))==ChaoState.Transition.NONE&&s.adult&&s.lockedPrimaryType()==ChaoState.FLY_TYPE,"Normal ticking after a growth edit cannot duplicate first evolution");
    }
  }
  private static void oppositionAndAge(){
    var s=adult();setAxis(s,-960,960);s.setEvolutionStrength(.6f);int age=s.ageTicks,adultAge=s.adultAgeTicks;float strength=s.evolutionStrength();
    for(int i=0;i<40;i++)s.giveDrive(ChaoState.POWER);
    near(NeutralFlyMorphology061.sample(s).runPower(),0,"Opposing Power cancels Run");near(NeutralFlyMorphology061.sample(s).swimFly(),1,"Compatible Fly survives Run/Power reversal");
    for(int i=0;i<40;i++)s.giveDrive(ChaoState.SWIM);
    near(NeutralFlyMorphology061.sample(s).swimFly(),0,"Opposing Swim cancels later Fly specialization without changing first Fly identity");
    for(int i=0;i<40;i++)s.giveDrive(ChaoState.SWIM);
    near(NeutralFlyMorphology061.sample(s).swimFly(),0,"Excess Swim remains on the balanced Fly-first surface, not an unrequested Swim secondary");
    DriveRules.change(s,ChaoState.RUN,-960,new Random(61));DriveRules.change(s,ChaoState.SWIM,-1920,new Random(61));
    near(NeutralFlyMorphology061.sample(s).runPower(),1,"Run reduction exposes stored opposing Power");near(NeutralFlyMorphology061.sample(s).swimFly(),1,"Swim reduction restores independent later Fly");
    check(s.ageTicks==age&&s.adultAgeTicks==adultAge&&s.evolutionStrength()==strength&&s.lockedPrimaryType()==ChaoState.FLY_TYPE,"Reversals change neither chronology, biological maturity nor first identity");
    near(NeutralFlyMorphology061.weights(NeutralFlyMorphology061.sample(s))[9],.5f,"Half-mature Power/Fly retains correct mature corner mass");
    for(int stat:new int[]{ChaoState.RUN,ChaoState.POWER}){var a=adult();var b=adult();for(int i=0;i<40;i++)a.giveDrive(stat);for(int i=0;i<40;i++)a.giveDrive(ChaoState.FLY);for(int i=0;i<40;i++)b.giveDrive(ChaoState.FLY);for(int i=0;i<40;i++)b.giveDrive(stat);check(NeutralFlyMorphology061.sample(a).equals(NeutralFlyMorphology061.sample(b)),"Compatible pair order is one form");}
  }
  private static void currentLabels(){
    String[] rpNames={"Run","Normal","Power"};
    for(int rp=-1;rp<=1;rp++)for(int sf=0;sf<=1;sf++){
      var s=adult();s.runPower=-rp;s.swimFly=-1;
      for(int stat:new int[]{rp<0?ChaoState.RUN:rp>0?ChaoState.POWER:-1,sf>0?ChaoState.FLY:-1})if(stat>=0){AnalyzerStatRules.apply(s,stat,AnalyzerStatRules.LEVEL,10);AnalyzerStatRules.apply(s,stat,AnalyzerStatRules.LEVEL,-1);AnalyzerStatRules.apply(s,stat,AnalyzerStatRules.PROGRESS,60);}
      String label=sf==0?rpNames[rp+1]:rp==0?"Fly":"Fly+"+rpNames[rp+1];
      check(s.typeName().equals("Fly/"+label),"Analyzer labels describe current-bar Fly-first combination despite stale legacy axes");
      check(s.secondaryFormKey().equals(label.toLowerCase(java.util.Locale.ROOT).replace('+','_')),"Combination key matches display and is independent of train order");
      var copy=ChaoState.decode(s.encode());check(copy.typeName().equals(s.typeName())&&copy.secondaryFormKey().equals(s.secondaryFormKey()),"Current labels persist without rewriting old axes");
      for(int stat=0;stat<4;stat++)DriveRules.setTotal(s,stat,0,new Random(61));DriveRules.setTotal(s,ChaoState.SWIM,960,new Random(61));
      check(s.typeName().equals("Fly/Normal")&&s.secondaryFormKey().equals("normal"),"Excess Swim reports balanced later Fly-first form");
    }
  }
  private static void identityAndPersistence(){
    var s=adult();setAxis(s,-480,720);s.setTrait("neutralfly61.foreign","unrelated earned data");s.expressedColor=2;s.shiny=true;s.expressedJewel=3;
    s.animalMask=ChaoState.ALL_ANIMALS;String encoded=s.encode(),lineage=s.lineageId();var p=NeutralFlyMorphology061.sample(s);
    var restored=ChaoState.decode(encoded);check(restored.encode().equals(encoded)&&restored.lineageId().equals(lineage)&&restored.trait("neutralfly61.foreign").equals("unrelated earned data"),"Round trip preserves genetics, DNA, lineage and unrelated traits");
    check(NeutralFlyMorphology061.sample(restored).equals(p),"Color, jewel and shiny finish do not change morphology inputs");
    for(int color=0;color<ChaoState.COLORS.length;color++)for(boolean shine:new boolean[]{false,true})for(boolean twoTone:new boolean[]{false,true}){
      restored.expressedColor=color;restored.shiny=shine;restored.twoTone=twoTone;
      check(NeutralFlyMorphology061.sample(restored).equals(p),"Named, monocolor and shiny finishes share the same Neutral Fly shape inputs");
    }
    for(int jewel=0;jewel<ChaoState.JEWELS.length;jewel++){
      restored.expressedJewel=jewel;check(NeutralFlyMorphology061.sample(restored).equals(p),"Every jewel finish retains the same morphology policy");
    }
    restored.align=-1;restored.primaryType=ChaoState.RUN_TYPE;
    check(NeutralFlyMorphology061.sample(restored).enabled()&&restored.lockedPrimaryType()==ChaoState.FLY_TYPE&&restored.lockedAlignment()==0,"Stale public fields cannot turn an established Neutral Fly adult into a Dark or Run first form");
    restored=ChaoState.decode(restored.encode());check(restored.align==0&&restored.primaryType==ChaoState.FLY_TYPE,"Persistence restores the same first-evolution lock");
    var hero=new ChaoState();hero.adult=true;hero.primaryType=ChaoState.RUN_TYPE;hero.align=1;hero.setEvolutionStrength(1.2f);hero=ChaoState.decode(hero.encode());setAxis(hero,-960,960);
    var h=HeroRunMorphology052.sample(hero);check(h.enabled()&&!NeutralFlyMorphology061.sample(hero).enabled(),"Working Hero Run still uses its existing production path");near(h.adultGrowth(),1,"Compatible Fly cannot erase the approved mature Hero/Run/Run development");
  }
  private static void neutralRegression(){
    for(float align:new float[]{0,1}){var s=new ChaoState();s.adult=true;s.align=align;s.primaryType=ChaoState.NORMAL;s.setEvolutionStrength(.6f);s=ChaoState.decode(s.encode());setAxis(s,-960,960);
      check(!NeutralFlyMorphology061.sample(s).enabled(),"Existing Normal-first families remain outside new Fly-first path");
      if(align==0){var p=NeutralMorphology056.sample(s);check(p.enabled(),"Delivered Neutral Normal stays independently selected");near(p.swimFly(),1,"Delivered Neutral Fly axis stays unchanged");}
      else{var p=HeroNormalMorphology057.sample(s);check(p.enabled(),"Delivered Hero Normal stays independently selected");near(p.swimFly(),1,"Delivered Hero Fly axis stays unchanged");}
    }
  }
  private static void scopeAndReadOnly(){
    var s=adult();s.swimFly=Float.NaN;s.runPower=Float.POSITIVE_INFINITY;Arrays.fill(s.points,9999);Arrays.fill(s.levels,99);s.progress[0]=99;s.levels[1]=-10;
    int[] points=s.points.clone(),levels=s.levels.clone();long entropy=s.entropy;var p=NeutralFlyMorphology061.sample(s);
    check(p.enabled()&&Float.isFinite(p.runPower())&&Float.isFinite(p.swimFly()),"Corrupt legacy visual axes cannot poison current-bar morphology");
    check(Arrays.equals(s.points,points)&&Arrays.equals(s.levels,levels)&&s.entropy==entropy&&Float.isNaN(s.swimFly)&&Float.isInfinite(s.runPower),"The read-only policy does not normalize, consume entropy or repair public fields");
    for(float align:new float[]{-.5f,.5f,-1,1}){var excluded=new ChaoState();excluded.adult=true;excluded.primaryType=ChaoState.FLY_TYPE;excluded.align=align;excluded=ChaoState.decode(excluded.encode());check(!NeutralFlyMorphology061.sample(excluded).enabled(),"Exact Hero/Dark boundaries remain outside Neutral Fly");}
    for(float align:new float[]{-.49999f,0,.49999f}){var admitted=new ChaoState();admitted.adult=true;admitted.primaryType=ChaoState.FLY_TYPE;admitted.align=align;admitted=ChaoState.decode(admitted.encode());check(NeutralFlyMorphology061.sample(admitted).enabled(),"Interior Neutral alignments admit locked Fly-first path");}
    for(int gate=0;gate<6;gate++){
      var excluded=adult();if(gate==0)excluded.adult=false;if(gate==1)excluded.dead=true;if(gate==2)excluded.immortal=true;if(gate==3)excluded.character="tails";if(gate==4)excluded.variant="nether";if(gate==5)excluded.primaryType=ChaoState.RUN_TYPE;
      // Editing the public first type after a lock is deliberately ignored;
      // the non-Fly scope fixture needs a fresh, then saved identity.
      if(gate==5){excluded=new ChaoState();excluded.adult=true;excluded.primaryType=ChaoState.RUN_TYPE;excluded.align=1;excluded=ChaoState.decode(excluded.encode());}
      check(!NeutralFlyMorphology061.sample(excluded).enabled(),"Child/dead/Chaos/character/theme/other-first forms keep their existing paths");
    }
    check(!NeutralFlyMorphology061.sample(null).enabled(),"Missing state is safely disabled");
  }
}
