package com.chaogarden.core;

import java.util.Arrays;
import java.util.Random;

/** Actual edits/drives, independent age and training, and unchanged saved identity. */
public final class BabyPowerMorphology084Test {
  private static int checks;
  private static void check(boolean value,String why){checks++;if(!value)throw new AssertionError(why);}
  private static void near(float actual,float expected,String why){check(Math.abs(actual-expected)<.00001f,why+": "+actual+" vs "+expected);}
  private static ChaoState baby(float maturity){var s=new ChaoState();s.ageTicks=1234;s.setEvolutionStrength(maturity);return s;}
  public static int run(){
    checks=0;
    for(int alignment:new int[]{-100,-50,0,50,100})for(float age:new float[]{0,.25f,.5f,.75f,.99f,1}){
      var s=baby(age);String before=s.encode();
      check(CreativeChaoEditRules056.apply(s,alignment,ChaoState.POWER,99),"Actual Creative level edit applies");
      var p=BabyPowerMorphology084.sample(s);near(p.training(),1,"Power99 reaches training endpoint");near(p.growth(),age,"Power99 still waits for baby visual age");
      check(s.ageTicks==1234&&s.evolutionStrength()==age&&!s.adult,"Editing training never advances age or evolves a baby");
      String saved=s.encode();check(BabyPowerMorphology084.sample(s).equals(p)&&s.encode().equals(saved),"Sampling is read-only");
      check(BabyPowerMorphology084.sample(ChaoState.decode(saved)).equals(p),"Encoded reload derives the same age/training input");
      check(!before.equals(saved),"Creative fixture really changed training");
      check(CreativeChaoEditRules056.apply(s,alignment,ChaoState.POWER,0),"Creative decrease applies");
      near(BabyPowerMorphology084.sample(s).growth(),0,"Lowering Power reverses development without reducing age");near(s.evolutionStrength(),age,"Decreasing training preserves maturity");
    }
    for(int total:new int[]{0,60,120,240,360,480,9900}){
      var s=baby(.75f);DriveRules.setTotal(s,ChaoState.POWER,total,new Random(84));
      near(BabyPowerMorphology084.sample(s).growth(),.75f*Math.min(1,total/480f),"Drives use actual current level bars");
      DriveRules.setTotal(s,ChaoState.RUN,total,new Random(85));
      near(BabyPowerMorphology084.sample(s).growth(),0,"Matching opposing Run training cancels Power direction");
      near(HeroRunMorphology052.sample(s).babyGrowth(),0,"Balance does not invent the opposite Run form");
      DriveRules.change(s,ChaoState.RUN,240,new Random(86));
      near(BabyPowerMorphology084.sample(s).growth(),0,"Moving across balance cannot retain Power features");
      if(total<=9660)near(HeroRunMorphology052.sample(s).babyGrowth(),.375f,"Crossing toward Run keeps the approved Run age rule");
      near(s.evolutionStrength(),.75f,"Opposing drives do not make biological age younger");
    }
    var combined=baby(.6f);DriveRules.apply(combined,DriveRules.Tier.CHAOTIC,-1,new Random(84));
    near(BabyPowerMorphology084.sample(combined).growth(),0,"Equal combined drives remain balanced");
    DriveRules.setTotal(combined,ChaoState.POWER,480,new Random(84));
    float growth=BabyPowerMorphology084.sample(combined).growth();
    for(int stat:new int[]{ChaoState.SWIM,ChaoState.FLY}){
      DriveRules.setTotal(combined,stat,9900,new Random(84));near(BabyPowerMorphology084.sample(combined).growth(),growth,"Other axis cannot erase actual Power tendency");
    }
    for(float alignment:new float[]{-1,0,1})for(int type=0;type<=4;type++){
      var adult=baby(1);adult.adult=true;adult.align=alignment;adult.primaryType=type;adult.setEvolutionStrength(1.2f);
      Arrays.fill(adult.levels,99);adult=ChaoState.decode(adult.encode());String before=adult.encode();
      check(!BabyPowerMorphology084.sample(adult).enabled(),"Adult families never enter this baby-only path");near(BabyPowerMorphology084.sample(adult).growth(),0,"Adult baby feature weight stays zero");
      check(before.equals(adult.encode())&&adult.lockedAlignment()==alignment&&adult.lockedPrimaryType()==type,"Sampling preserves precise locked adult identity");
    }
    for(int color=0;color<ChaoState.COLORS.length;color++)for(boolean shiny:new boolean[]{false,true}){
      var s=baby(.8f);s.levels[ChaoState.POWER]=99;s.colorGenes=new int[]{color,color};s.expressedColor=color;s.shiny=shiny;s.shinyGenes=new int[]{shiny?1:0,shiny?1:0};
      near(BabyPowerMorphology084.sample(s).growth(),.8f,"Color and shiny genes retain identical form");
    }
    for(String gate:new String[]{"dead","chaos","robot","theme"}){
      var s=baby(1);s.levels[ChaoState.POWER]=99;
      switch(gate){case "dead"->s.dead=true;case "chaos"->s.immortal=true;case "robot"->s.character="omochao";default->s.variant="nether";}
      check(!BabyPowerMorphology084.sample(s).enabled(),"Special existing anatomy is preserved: "+gate);
    }
    check(!BabyPowerMorphology084.sample(null).enabled(),"Missing state is safe");
    var evolve=baby(.4f);check(CreativeChaoEditRules056.apply(evolve,80,ChaoState.POWER,99),"Power99 evolution fixture applies");
    check(evolve.prospectivePrimaryType()==ChaoState.POWER_TYPE,"Appearance does not replace earned-point first evolution selection");
    evolve.ageTicks=19;check(evolve.tick(20,10000,new Random(84))==ChaoState.Transition.EVOLVED,"Actual first evolution occurs through normal lifecycle");
    check(evolve.lockedPrimaryType()==ChaoState.POWER_TYPE&&evolve.lockedAlignment()==.8f,"Power first type and Hero alignment lock normally");
    check(!BabyPowerMorphology084.sample(evolve).enabled(),"Normal adult transition releases baby sidecar");
    return checks;
  }
}
