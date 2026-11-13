package com.chaogarden.core;

import java.util.Arrays;
import java.util.Random;

/** Saved Dark/Normal identity, real training and independent opposed axes.
 * Geometry and actual appearance are checked separately on the production rig. */
public final class DarkNormalMorphology060Test {
  private static int checks;
  private static void check(boolean value,String why){checks++;if(!value)throw new AssertionError(why);}
  private static void near(float actual,float expected,String why){check(Float.isFinite(actual)&&Math.abs(actual-expected)<.000004f,why+": "+actual+" vs "+expected);}
  public static int run(){
    checks=0;untouchedFirstEvolution();nineCombinations();realMutationRoutes();creativeCommandAndMaturity();currentLabels();oppositionAndAge();identityAndPersistence();neutralRegression();scopeAndReadOnly();
    System.out.println("Dark Normal morphology060: "+checks+" checks passed; nine combinations, real training, independent age/opposed axes, immutable first identity and exact persistence");
    return checks;
  }
  private static ChaoState adult(){
    var s=new ChaoState();s.adult=true;s.primaryType=ChaoState.NORMAL;s.align=-1;s.ageTicks=400;s.adultAgeTicks=90;
    s.hunger=80;s.energy=100;s.setEvolutionStrength(1.2f);return ChaoState.decode(s.encode());
  }
  private static void setAxis(ChaoState s,int runPower,int swimFly){
    for(int i=0;i<4;i++)DriveRules.setTotal(s,i,0,new Random(60));
    if(runPower!=0)DriveRules.setTotal(s,runPower>0?ChaoState.POWER:ChaoState.RUN,Math.abs(runPower),new Random(60));
    if(swimFly!=0)DriveRules.setTotal(s,swimFly>0?ChaoState.FLY:ChaoState.SWIM,Math.abs(swimFly),new Random(60));
  }
  private static void untouchedFirstEvolution(){
    var s=new ChaoState();s.ageTicks=19;s.align=-.75f;s.points[ChaoState.STAMINA]=9999;
    check(s.prospectivePrimaryType()==ChaoState.NORMAL,"An untouched four-stat child has no invented skill preference");
    check(s.tick(20,100000,new Random(60))==ChaoState.Transition.EVOLVED,"The actual ordinary clock completes first evolution");
    check(s.adult&&s.lockedPrimaryType()==ChaoState.NORMAL&&s.lockedAlignment()==-.75f,"Untouched child locks its Dark alignment and Normal first identity");
    check(DarkNormalMorphology060.sample(s).enabled(),"A naturally obtained Dark/Normal adult enters the new path");
    while(s.cocoonTicks>0)s.tick(20,100000,new Random(60));
    for(int i=0;i<40;i++)s.giveDrive(ChaoState.RUN);
    check(s.lockedPrimaryType()==ChaoState.NORMAL&&s.primaryType==ChaoState.NORMAL,"Later Run training cannot rewrite the first type");
    near(DarkNormalMorphology060.sample(s).runPower(),-1,"Forty real post-evolution drives reach the provisional Run target");
    near(DarkNormalMorphology060.sample(s).maturity(),0,"Training immediately after first evolution does not skip biological maturity");
    for(int stat=0;stat<4;stat++){
      var trained=new ChaoState();trained.ageTicks=19;trained.align=-1;trained.points[stat]=20;trained.points[ChaoState.STAMINA]=9999;
      trained.tick(20,100000,new Random(60));
      check(trained.lockedPrimaryType()==stat+1&&!DarkNormalMorphology060.sample(trained).enabled(),"The new Dark/Normal renderer does not hijack a trained first type");
    }
    for(int a=0;a<4;a++)for(int b=0;b<4;b++)if(a!=b){
      var tied=new ChaoState();tied.train(a,1);tied.train(b,1);tied.points[a]=tied.points[b]=100;tied.points[ChaoState.STAMINA]=9999;
      check(tied.prospectivePrimaryType()==b+1,"A positive earned-point tie keeps latest actual training, not a new Normal admission");
      var copy=ChaoState.decode(tied.encode());check(copy.prospectivePrimaryType()==b+1,"Tie history still survives the existing save format");
    }
  }
  private static void nineCombinations(){
    for(int rp=-1;rp<=1;rp++)for(int sf=-1;sf<=1;sf++)for(int quarter=0;quarter<=4;quarter++){
      var s=adult();setAxis(s,rp*960,sf*960);s.setEvolutionStrength(quarter*.3f);String encoded=s.encode();
      var p=DarkNormalMorphology060.sample(s);check(p.enabled(),"All nine independent axis combinations remain on Dark/Normal");
      near(p.runPower(),rp,"Current Run/Power bars select the intended signed axis");near(p.swimFly(),sf,"Current Swim/Fly bars select the other signed axis");
      near(p.maturity(),quarter*.25f,"The same training state has independent young/intermediate/mature development");
      var weights=DarkNormalMorphology060.weights(p);check(weights.length==10,"Ten shared shape corners cover fresh and the mature nine-grid");
      float sum=0;for(float w:weights){check(Float.isFinite(w)&&w>=0&&w<=1,"All blend weights are finite, convex and bounded");sum+=w;}
      near(sum,1,"No unsupported combination creates missing or overlapping weight mass");near(weights[0],1-quarter*.25f,"Fresh identity recedes continuously with biological maturity");
      int expected=1+(sf+1)*3+(rp+1);
      for(int i=1;i<weights.length;i++)near(weights[i],i==expected?quarter*.25f:0,"At an exact corner only that mature combination contributes");
      check(encoded.equals(s.encode()),"Sampling and blending are completely read-only");
      var restored=ChaoState.decode(encoded);check(restored.encode().equals(encoded)&&DarkNormalMorphology060.sample(restored).equals(p),"Exact save/reload preserves each combination and independent maturity");
    }
    for(int rp=-4;rp<=4;rp++)for(int sf=-4;sf<=4;sf++){
      var s=adult();setAxis(s,rp*120,sf*120);s.setEvolutionStrength(.6f);var p=DarkNormalMorphology060.sample(s);var w=DarkNormalMorphology060.weights(p);
      float sum=0;int used=0;for(float f:w){sum+=f;if(f>0)used++;}
      near(p.runPower(),rp*.125f,"Intermediate Run/Power input remains continuous");near(p.swimFly(),sf*.125f,"Intermediate Swim/Fly input remains continuous");
      near(sum,1,"Interior combinations remain convex");check(used<=5,"At most four mature neighbors plus the fresh corner blend together");
    }
  }
  private static void realMutationRoutes(){
    for(int stat=0;stat<4;stat++){
      var direct=adult();var ordinary=adult();var combined=adult();var analyzer=adult();
      direct.train(stat,240,new Random(60));for(int i=0;i<10;i++)ordinary.giveDrive(stat);
      DriveRules.apply(combined,DriveRules.Tier.SOLID,stat,new Random(60));DriveRules.change(combined,stat,140,new Random(60));
      AnalyzerStatRules.apply(analyzer,stat,AnalyzerStatRules.LEVEL,1);AnalyzerStatRules.apply(analyzer,stat,AnalyzerStatRules.LEVEL,1);AnalyzerStatRules.apply(analyzer,stat,AnalyzerStatRules.PROGRESS,40);
      for(var s:new ChaoState[]{direct,ordinary,combined,analyzer}){
        var p=DarkNormalMorphology060.sample(s);near(stat<2?p.swimFly():p.runPower(),(stat==ChaoState.FLY||stat==ChaoState.POWER)?.25f:-.25f,"Equal actual training totals agree across ordinary, typed combined, direct and Analyzer changes");
        near(p.maturity(),1,"None of the ordinary training routes fabricates biological youth or age");
        check(s.lockedPrimaryType()==ChaoState.NORMAL&&s.lockedAlignment()==-1,"Every route preserves the first evolution identity");
      }
      AnalyzerStatRules.apply(analyzer,stat,AnalyzerStatRules.PROGRESS,0);
      near(Math.abs(stat<2?DarkNormalMorphology060.sample(analyzer).swimFly():DarkNormalMorphology060.sample(analyzer).runPower()),200f/960,"Partial-bar Analyzer reductions recede the correct axis");
      AnalyzerStatRules.apply(analyzer,stat,AnalyzerStatRules.LEVEL,-1);
      near(Math.abs(stat<2?DarkNormalMorphology060.sample(analyzer).swimFly():DarkNormalMorphology060.sample(analyzer).runPower()),100f/960,"Level reductions recede the correct axis");
    }
    var balanced=adult();DriveRules.apply(balanced,DriveRules.Tier.CHAOTIC,-1,new Random(60));
    near(DarkNormalMorphology060.sample(balanced).runPower(),0,"A balanced multi-stat transaction invents no Run/Power advantage");near(DarkNormalMorphology060.sample(balanced).swimFly(),0,"A balanced multi-stat transaction invents no Swim/Fly advantage");
    var perfect=adult();Arrays.fill(perfect.levels,99);perfect.levels[ChaoState.STAMINA]=0;DriveRules.apply(perfect,DriveRules.Tier.PERFECT,-1,new Random(60));
    near(DarkNormalMorphology060.sample(perfect).runPower(),0,"Equally capped opponents remain balanced after a Perfect drive");near(DarkNormalMorphology060.sample(perfect).swimFly(),0,"Perfect drive preserves the other independent balance");
  }
  private static void creativeCommandAndMaturity(){
    for(int rp=-1;rp<=1;rp++)for(int sf=-1;sf<=1;sf++){
      var s=adult();int age=s.ageTicks,adultAge=s.adultAgeTicks;
      for(int stat:new int[]{rp<0?ChaoState.RUN:rp>0?ChaoState.POWER:-1,sf<0?ChaoState.SWIM:sf>0?ChaoState.FLY:-1})if(stat>=0)
        check(CreativeChaoEditRules056.apply(s,-100,stat,10),"Actual absolute Creative edit accepts each compatible selected stat");
      var target=DarkNormalMorphology060.sample(s);near(target.runPower(),rp,"Absolute command levels derive the correct current Run/Power axis");near(target.swimFly(),sf,"Absolute command levels derive the correct current Swim/Fly axis");
      check(s.lockedPrimaryType()==ChaoState.NORMAL&&s.lockedAlignment()==-1,"Adult command cannot convert the first Dark/Normal identity");
      String before=s.encode();check(!CreativeChaoEditRules056.apply(s,100,ChaoState.RUN,99)&&s.encode().equals(before),"Conflicting adult alignment rejects the entire command transaction");
      check(MaturityEditRules056.applyMilli(s,250),"Existing stage-safe maturity control accepts a younger adult growth fraction");
      var young=DarkNormalMorphology060.sample(s);near(young.maturity(),.25f,"Maturity control changes biological growth independently of training");near(young.runPower(),rp,"Maturity editing retains current training");near(young.swimFly(),sf,"Maturity editing retains the compatible axis");
      check(s.ageTicks==age&&s.adultAgeTicks==adultAge&&s.adult&&!s.dead&&s.cocoonTicks==0,"Explicit growth edits never reset lifespan or invoke another first cocoon");
      check(MaturityEditRules056.applyMilli(s,1000),"The full adult maturity endpoint is reachable for Creative testing");near(DarkNormalMorphology060.sample(s).maturity(),1,"Full adult control reaches the mature corner");
      var copy=ChaoState.decode(s.encode());check(copy.encode().equals(s.encode())&&DarkNormalMorphology060.sample(copy).equals(DarkNormalMorphology060.sample(s)),"Combined command and maturity inputs persist exactly");
      check(s.tick(100000,100000,new Random(60))==ChaoState.Transition.NONE&&s.adult&&s.lockedPrimaryType()==ChaoState.NORMAL,"Normal ticking after a growth edit cannot duplicate first evolution");
    }
  }
  private static void oppositionAndAge(){
    var s=adult();setAxis(s,-960,960);s.setEvolutionStrength(.6f);int age=s.ageTicks,adultAge=s.adultAgeTicks;float strength=s.evolutionStrength();
    for(int i=0;i<40;i++)s.giveDrive(ChaoState.POWER);
    near(DarkNormalMorphology060.sample(s).runPower(),0,"Opposing Power cancels Run without changing the independent Fly axis");near(DarkNormalMorphology060.sample(s).swimFly(),1,"Compatible Fly survives the Run/Power reversal");
    for(int i=0;i<40;i++)s.giveDrive(ChaoState.SWIM);
    near(DarkNormalMorphology060.sample(s).swimFly(),0,"Opposing Swim cancels Fly");
    DriveRules.change(s,ChaoState.RUN,-960,new Random(60));DriveRules.change(s,ChaoState.FLY,-960,new Random(60));
    near(DarkNormalMorphology060.sample(s).runPower(),1,"Run reduction exposes the stored opposing Power influence");near(DarkNormalMorphology060.sample(s).swimFly(),-1,"Fly reduction exposes the stored opposing Swim influence");
    check(s.ageTicks==age&&s.adultAgeTicks==adultAge&&s.evolutionStrength()==strength,"Repeated reversals never change chronological age or biological maturity");
    near(DarkNormalMorphology060.weights(DarkNormalMorphology060.sample(s))[3],.5f,"Half-maturity Power/Swim retains half of the correct mature corner (Swim row, Power column)");
  }
  private static void currentLabels(){
    String[] rpNames={"Run","Normal","Power"},sfNames={"Swim","Normal","Fly"};
    for(int rp=-1;rp<=1;rp++)for(int sf=-1;sf<=1;sf++){
      var s=adult();s.runPower=-rp;s.swimFly=-sf;
      for(int stat:new int[]{rp<0?ChaoState.RUN:rp>0?ChaoState.POWER:-1,sf<0?ChaoState.SWIM:sf>0?ChaoState.FLY:-1})if(stat>=0){
        AnalyzerStatRules.apply(s,stat,AnalyzerStatRules.LEVEL,10);AnalyzerStatRules.apply(s,stat,AnalyzerStatRules.LEVEL,-1);AnalyzerStatRules.apply(s,stat,AnalyzerStatRules.PROGRESS,60);
      }
      String label=sf==0?rpNames[rp+1]:rp==0?sfNames[sf+1]:sfNames[sf+1]+"+"+rpNames[rp+1];
      check(s.typeName().equals("Normal/"+label),"Analyzer type label describes the same current-stat combination as morphology, despite stale legacy axes");
      check(s.secondaryFormKey().equals(label.toLowerCase(java.util.Locale.ROOT).replace('+','_')),"Combination key remains order-independent and agrees with the displayed type");
      var copy=ChaoState.decode(s.encode());check(copy.typeName().equals(s.typeName())&&copy.secondaryFormKey().equals(s.secondaryFormKey()),"Current labels survive save/reload without rewriting the legacy influence fields");
      for(int stat=0;stat<4;stat++)DriveRules.setTotal(s,stat,0,new Random(60));
      check(s.typeName().equals("Normal/Normal")&&s.secondaryFormKey().equals("normal"),"Actual stat loss clears both current labels, independent of stale trained axes");
    }
    // Hero Fly stays outside both the Dark/Normal060 and new Neutral/Fly061 current-bar policies.
    var legacy=new ChaoState();legacy.adult=true;legacy.primaryType=ChaoState.FLY_TYPE;legacy.align=1;legacy.swimFly=1;legacy.runPower=-1;legacy=ChaoState.decode(legacy.encode());
    DriveRules.setTotal(legacy,ChaoState.POWER,960,new Random(60));legacy.swimFly=1;legacy.runPower=-1;
    check(legacy.typeName().equals("Fly/Fly+Run")&&legacy.secondaryFormKey().equals("fly_run"),"Other first-evolution types retain their previous influence labels instead of adopting the Dark/Normal policy");
  }
  private static void identityAndPersistence(){
    var s=adult();setAxis(s,-480,720);s.setTrait("darknormal60.foreign","unrelated earned data");s.expressedColor=2;s.shiny=true;s.expressedJewel=3;
    s.animalMask=ChaoState.ALL_ANIMALS;String encoded=s.encode(),lineage=s.lineageId();var p=DarkNormalMorphology060.sample(s);
    var restored=ChaoState.decode(encoded);check(restored.encode().equals(encoded)&&restored.lineageId().equals(lineage)&&restored.trait("darknormal60.foreign").equals("unrelated earned data"),"Round trip preserves genetics, DNA, lineage and unrelated traits");
    check(DarkNormalMorphology060.sample(restored).equals(p),"Color, jewel and shiny finish do not change morphology inputs");
    for(int color=0;color<ChaoState.COLORS.length;color++)for(boolean shine:new boolean[]{false,true})for(boolean twoTone:new boolean[]{false,true}){
      restored.expressedColor=color;restored.shiny=shine;restored.twoTone=twoTone;
      check(DarkNormalMorphology060.sample(restored).equals(p),"Named, monocolor and shiny finishes share the same Dark Normal shape inputs");
    }
    for(int jewel=0;jewel<ChaoState.JEWELS.length;jewel++){
      restored.expressedJewel=jewel;check(DarkNormalMorphology060.sample(restored).equals(p),"Every jewel finish retains the same morphology policy");
    }
    restored.align=1;restored.primaryType=ChaoState.RUN_TYPE;
    check(DarkNormalMorphology060.sample(restored).enabled()&&restored.lockedPrimaryType()==ChaoState.NORMAL&&restored.lockedAlignment()==-1,"Stale public fields cannot turn an established Dark Normal adult into a Hero or Run first form");
    restored=ChaoState.decode(restored.encode());check(restored.align==-1&&restored.primaryType==ChaoState.NORMAL,"Persistence restores the same first-evolution lock");
    var hero=new ChaoState();hero.adult=true;hero.primaryType=ChaoState.RUN_TYPE;hero.align=1;hero.setEvolutionStrength(1.2f);hero=ChaoState.decode(hero.encode());setAxis(hero,-960,960);
    var h=HeroRunMorphology052.sample(hero);check(h.enabled()&&!DarkNormalMorphology060.sample(hero).enabled(),"Working Hero Run still uses its existing production path");near(h.adultGrowth(),1,"Compatible Fly cannot erase the approved mature Hero/Run/Run development");
  }
  private static void neutralRegression(){
    var s=new ChaoState();s.adult=true;s.align=0;s.primaryType=ChaoState.NORMAL;s.setEvolutionStrength(.6f);s=ChaoState.decode(s.encode());setAxis(s,-960,960);
    var p=NeutralMorphology056.sample(s);check(p.enabled()&&!DarkNormalMorphology060.sample(s).enabled(),"Existing Neutral Normal remains on its separate production path");near(p.maturity(),.5f,"Dark expansion does not reinterpret Neutral biological maturity");near(p.runPower(),-1,"Dark expansion does not reinterpret Neutral opposing axes");near(p.swimFly(),1,"Dark expansion preserves compatible Neutral Fly");
  }
  private static void scopeAndReadOnly(){
    var s=adult();s.swimFly=Float.NaN;s.runPower=Float.POSITIVE_INFINITY;Arrays.fill(s.points,9999);Arrays.fill(s.levels,99);s.progress[0]=99;s.levels[1]=-10;
    int[] points=s.points.clone(),levels=s.levels.clone();long entropy=s.entropy;var p=DarkNormalMorphology060.sample(s);
    check(p.enabled()&&Float.isFinite(p.runPower())&&Float.isFinite(p.swimFly()),"Corrupt legacy visual axes cannot poison current-bar morphology");
    check(Arrays.equals(s.points,points)&&Arrays.equals(s.levels,levels)&&s.entropy==entropy&&Float.isNaN(s.swimFly)&&Float.isInfinite(s.runPower),"The read-only policy does not normalize, consume entropy or repair public fields");
    for(float align:new float[]{-.49999f,0,.5f,1}){var excluded=new ChaoState();excluded.adult=true;excluded.align=align;excluded=ChaoState.decode(excluded.encode());check(!DarkNormalMorphology060.sample(excluded).enabled(),"Non-Dark boundaries stay on their locked Neutral/Hero paths");}
    for(float align:new float[]{-.5f,-.50001f,-.75f,-1}){var admitted=new ChaoState();admitted.adult=true;admitted.align=align;admitted=ChaoState.decode(admitted.encode());check(DarkNormalMorphology060.sample(admitted).enabled(),"The exact Dark threshold and its interior admit the locked Normal path");}
    for(int gate=0;gate<6;gate++){
      var excluded=adult();if(gate==0)excluded.adult=false;if(gate==1)excluded.dead=true;if(gate==2)excluded.immortal=true;if(gate==3)excluded.character="tails";if(gate==4)excluded.variant="nether";if(gate==5)excluded.primaryType=ChaoState.RUN_TYPE;
      // Editing the public first type after a lock is deliberately ignored;
      // the non-Normal scope fixture needs a fresh, then saved identity.
      if(gate==5){excluded=new ChaoState();excluded.adult=true;excluded.primaryType=ChaoState.RUN_TYPE;excluded.align=-1;excluded=ChaoState.decode(excluded.encode());}
      check(!DarkNormalMorphology060.sample(excluded).enabled(),"Child/dead/Chaos/character/theme/other-first forms keep their existing paths");
    }
    check(!DarkNormalMorphology060.sample(null).enabled(),"Missing state is safely disabled");
  }
}
