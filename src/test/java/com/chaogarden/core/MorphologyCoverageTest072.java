package com.chaogarden.core;

import java.util.Arrays;
import java.util.Random;

/** Cross-family contract: all 15 saved first identities can reach the full
 * later 3x3 grid without changing age or identity. These are policy checks,
 * not proof of distinct geometry, palettes, rendering or multiplayer. */
public final class MorphologyCoverageTest072 {
  private static int checks;
  private static int endpoints;
  private static final int SPAN=960;
  private record Family(String name,float alignment,int first,Class<?> policy) {}
  private record View(Object sample,boolean enabled,float age,float rp,float sf,float[] weights) {}
  private static final Family[] FAMILIES={
      new Family("Neutral/Neutral",0,ChaoState.NORMAL,NeutralMorphology056.class),
      new Family("Neutral/Swim",0,ChaoState.SWIM_TYPE,NeutralSwimMorphology059.class),
      new Family("Neutral/Fly",0,ChaoState.FLY_TYPE,NeutralFlyMorphology061.class),
      new Family("Neutral/Run",0,ChaoState.RUN_TYPE,NeutralRunMorphology062.class),
      new Family("Neutral/Power",0,ChaoState.POWER_TYPE,NeutralPowerMorphology063.class),
      new Family("Hero/Neutral",1,ChaoState.NORMAL,HeroNormalMorphology057.class),
      new Family("Hero/Swim",1,ChaoState.SWIM_TYPE,HeroSwimMorphology064.class),
      new Family("Hero/Fly",1,ChaoState.FLY_TYPE,HeroFlyMorphology065.class),
      new Family("Hero/Run",1,ChaoState.RUN_TYPE,HeroRunMorphology066.class),
      new Family("Hero/Power",1,ChaoState.POWER_TYPE,HeroPowerMorphology067.class),
      new Family("Dark/Neutral",-1,ChaoState.NORMAL,DarkNormalMorphology060.class),
      new Family("Dark/Swim",-1,ChaoState.SWIM_TYPE,DarkSwimMorphology068.class),
      new Family("Dark/Fly",-1,ChaoState.FLY_TYPE,DarkFlyMorphology069.class),
      new Family("Dark/Run",-1,ChaoState.RUN_TYPE,DarkRunMorphology070.class),
      new Family("Dark/Power",-1,ChaoState.POWER_TYPE,DarkPowerMorphology071.class)
  };
  private MorphologyCoverageTest072() {}
  private static void check(boolean value,String why){checks++;if(!value)throw new AssertionError(why);}
  private static void near(float actual,float expected,String why){
    check(Float.isFinite(actual)&&Math.abs(actual-expected)<.000004f,why+": "+actual+" vs "+expected);
  }
  private static ChaoState adult(Family family){
    var s=new ChaoState();s.adult=true;s.primaryType=family.first;s.align=family.alignment;
    s.ageTicks=2345;s.adultAgeTicks=432;s.hunger=80;s.energy=100;s.happiness=70;
    s.setEvolutionStrength(1.2f);return ChaoState.decode(s.encode());
  }
  private static View view(Family family,ChaoState state){
    try{
      Object sample=family.policy.getMethod("sample",ChaoState.class).invoke(null,state);
      Class<?> type=sample.getClass();
      return new View(sample,(boolean)type.getMethod("enabled").invoke(sample),
          ((Number)type.getMethod("maturity").invoke(sample)).floatValue(),
          ((Number)type.getMethod("runPower").invoke(sample)).floatValue(),
          ((Number)type.getMethod("swimFly").invoke(sample)).floatValue(),
          (float[])family.policy.getMethod("weights",type).invoke(null,sample));
    }catch(ReflectiveOperationException error){
      throw new AssertionError("Missing current morphology policy contract: "+family.name,error);
    }
  }
  private static void axis(ChaoState state,int rp,int sf){
    for(int stat=0;stat<4;stat++)DriveRules.setTotal(state,stat,0,new Random(720));
    if(rp!=0)DriveRules.setTotal(state,rp<0?ChaoState.RUN:ChaoState.POWER,Math.abs(rp),new Random(721));
    if(sf!=0)DriveRules.setTotal(state,sf<0?ChaoState.SWIM:ChaoState.FLY,Math.abs(sf),new Random(722));
  }
  private static String later(int rp,int sf){
    String a=sf<0?"Swim":sf>0?"Fly":"",b=rp<0?"Run":rp>0?"Power":"";
    return a.isEmpty()?(b.isEmpty()?"Normal":b):b.isEmpty()?a:a+"+"+b;
  }
  private static void same(View a,View b,String why){
    check(a.sample.equals(b.sample)&&Arrays.equals(a.weights,b.weights),why);
  }
  private static void identity(Family f,ChaoState s,int age,int life,float maturity,String why){
    check(s.adult&&s.lockedPrimaryType()==f.first&&s.lockedAlignment()==f.alignment,why+": first identity changed");
    check(s.ageTicks==age&&s.adultAgeTicks==life&&s.evolutionStrength()==maturity,why+": chronology or maturity changed");
  }
  private static void corners(Family family){
    for(int rp=-1;rp<=1;rp++)for(int sf=-1;sf<=1;sf++){
      endpoints++;String where=family.name+"/"+later(rp,sf);
      var s=adult(family);axis(s,rp*SPAN,sf*SPAN);int age=s.ageTicks,life=s.adultAgeTicks;
      for(int quarter=0;quarter<=4;quarter++){
        float maturity=quarter*.25f;s.setEvolutionStrength(quarter*.3f);String encoded=s.encode();
        View v=view(family,s);check(v.enabled,where+": current family is unreachable");
        near(v.rp,rp,where+": full later Run/Power coordinate is clamped away");
        near(v.sf,sf,where+": full later Swim/Fly coordinate is clamped away");
        near(v.age,maturity,where+": biological maturity is not independent");
        check(v.weights.length==10,where+": fresh and mature grid slots are missing");
        int slot=1+(sf+1)*3+(rp+1);
        for(int i=0;i<10;i++)near(v.weights[i],i==0?1-maturity:i==slot?maturity:0,where+": wrong corner contribution at age "+maturity);
        check(encoded.equals(s.encode()),where+": sampling mutates saved data");
        same(v,view(family,ChaoState.decode(encoded)),where+": save/reload changed current shape inputs");
        identity(family,s,age,life,quarter*.3f,where);
      }
      check(s.typeName().equals(ChaoState.TYPES[family.first]+"/"+later(rp,sf)),where+": current label hides or misidentifies the later corner");
      View expected=view(family,s);s.expressedColor=2;s.shiny=true;s.twoTone=!s.twoTone;s.expressedJewel=3;
      same(expected,view(family,s),where+": coat or finish changes the underlying policy");
      s.align=family.alignment==0?1:-family.alignment;s.primaryType=(family.first+1)%5;
      same(expected,view(family,s),where+": stale public mirrors override the saved first identity");
      var restored=ChaoState.decode(s.encode());
      check(restored.primaryType==family.first&&restored.align==family.alignment,where+": save normalization lost the existing lock");
    }
  }
  private static void interiors(Family family){
    for(int rp=-2;rp<=2;rp++)for(int sf=-2;sf<=2;sf++)for(float strength:new float[]{.3f,.9f}){
      var s=adult(family);axis(s,rp*240,sf*240);s.setEvolutionStrength(strength);
      String encoded=s.encode();View v=view(family,s);
      near(v.rp,rp*.25f,family.name+": intermediate Run/Power coordinate changes");
      near(v.sf,sf*.25f,family.name+": intermediate Swim/Fly coordinate changes");
      near(v.age,strength/1.2f,family.name+": intermediate age changes with training");
      float sum=0;int active=0;for(float weight:v.weights){check(Float.isFinite(weight)&&weight>=0&&weight<=1,family.name+": invalid interior weight");sum+=weight;if(weight>0)active++;}
      near(sum,1,family.name+": interior blend loses mass");check(active<=5,family.name+": interior activates non-neighboring grid corners");
      check(encoded.equals(s.encode()),family.name+": interior blend changes saved state");
    }
  }
  private static void reversals(Family family){
    for(int axis=0;axis<2;axis++)for(int direction:new int[]{-1,1}){
      var s=adult(family);axis(s,axis==0?direction*SPAN:0,axis==1?direction*SPAN:0);s.setEvolutionStrength(.6f);
      int age=s.ageTicks,life=s.adultAgeTicks;
      int counter=axis==0?(direction<0?ChaoState.POWER:ChaoState.RUN):(direction<0?ChaoState.FLY:ChaoState.SWIM);
      for(int step=0;step<=4;step++){
        if(step>0)for(int i=0;i<20;i++)s.giveDrive(counter);
        View v=view(family,s);float target=direction*(1-step*.5f);
        near(axis==0?v.rp:v.sf,target,family.name+": ordinary drives cannot cross an opposing axis through balance");
        near(axis==0?v.sf:v.rp,0,family.name+": an opposing drive changes the other independent axis");
        near(v.age,.5f,family.name+": opposing drives make the adult younger");identity(family,s,age,life,.6f,family.name);
      }
      for(int step=1;step<=4;step++){
        DriveRules.change(s,counter,-480,new Random(723));View v=view(family,s);
        near(axis==0?v.rp:v.sf,direction*(-1+step*.5f),family.name+": reductions cannot retrace the opposing axis");
        identity(family,s,age,life,.6f,family.name);
      }
    }
  }
  public static int run(){
    checks=0;endpoints=0;
    for(Family family:FAMILIES){corners(family);interiors(family);reversals(family);}
    check(FAMILIES.length==15&&endpoints==135,"The coverage inventory must include all 15 families and 135 mature endpoints");
    System.out.println("Morphology coverage072: "+checks+" checks passed; all 135 mature policy corners across 15 locked first identities, five ages, interior blends, finishes, saves and bidirectional drive/reduction paths. Geometry and visual checks are separate.");
    return checks;
  }
}
