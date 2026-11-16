package com.chaogarden.core;

/** Distinct rank, level, locked first type and exact live specialization tests. */
public final class EquippedAbility080Test {
  private static int checks;
  private static void check(boolean condition,String message){checks++;if(!condition)throw new AssertionError(message);}
  private static void near(double actual,double expected,String message){check(Math.abs(actual-expected)<1e-8,message+" got="+actual);}
  private static ChaoState state(int stat,float alignment,float strength){
    var s=new ChaoState();s.adult=true;s.align=alignment;s.primaryType=stat+1;s.grades[stat]=5;s.gradeA[stat]=s.gradeB[stat]=5;
    if(!s.useHyperFruit(stat))throw new AssertionError("Native expressed X fixture failed");
    s.levels[stat]=99;s.points[stat]=9900;s.energy=100;s.hunger=100;s.setEvolutionStrength(strength);
    return ChaoState.decode(s.encode());
  }
  public static int run(){
    for(int stat:new int[]{ChaoState.RUN,ChaoState.POWER})for(float alignment:new float[]{-1,-.5f,-.49f,0,.49f,.5f,1})
      for(float strength:new float[]{0,.3f,.6f,1.2f}){
        var s=state(stat,alignment,strength);String before=s.encode();
        check(EquippedAbilityRules080.eligible(s,stat),"Every alignment/age strength has matching exact eligible form");
        check(EquippedAbilityRules080.percent(s,stat)==1000,"Absent settings have a stable maximum default");
        near(EquippedAbilityRules080.bonus(s,stat),1000,"Maximum .84 tier is +100000%, total1001");
        check(s.encode().equals(before),"Eligibility/default reads preserve established Chao data");
        s.energy=0;check(EquippedAbilityRules080.eligible(s,stat),"Fatigue is not a new secret eligibility requirement");
        for(int setting=900;setting>=0;setting-=100){
          check(EquippedAbilityRules080.cycle(s,stat),"Cycle persists each selected setting");
          check(EquippedAbilityRules080.percent(s,stat)==setting,"Exactly one100point step");
          var reloaded=ChaoState.decode(s.encode());
          check(EquippedAbilityRules080.percent(reloaded,stat)==setting,"Selected setting survives actual Chao codec");
          near(EquippedAbilityRules080.bonus(reloaded,stat),setting,"Codec preserves legacy tiers with .84 bonus units including zero");
        }
        check(EquippedAbilityRules080.cycle(s,stat)&&EquippedAbilityRules080.percent(s,stat)==1000,"Zero wraps to1000");
        check(!s.traitEntries().containsKey(stat==ChaoState.RUN?EquippedAbilityRules080.RUN_SETTING:EquippedAbilityRules080.POWER_SETTING),"Default wrap releases its optional trait slot");
        s.grades[stat]=5;check(!EquippedAbilityRules080.eligible(s,stat),"S is not expressed X");
        s.gradeA[stat]=6;s.gradeB[stat]=6;check(!EquippedAbilityRules080.eligible(s,stat),"Unexpressed inherited data cannot grant X ability");
        s.grades[stat]=6;s.levels[stat]=98;check(!EquippedAbilityRules080.eligible(s,stat),"98 is not99");
        s.levels[stat]=99;s.adult=false;check(!EquippedAbilityRules080.eligible(s,stat),"A highly trained baby remains ineligible");
        s.adult=true;s.cocoonTicks=1;check(!EquippedAbilityRules080.eligible(s,stat),"Cocoon has no active adult ability");
        s.cocoonTicks=0;s.dead=true;check(!EquippedAbilityRules080.eligible(s,stat),"Dead Chao cannot grant ability");
      }
    for(int stat:new int[]{ChaoState.RUN,ChaoState.POWER}){
      int opposite=stat==ChaoState.RUN?ChaoState.POWER:ChaoState.RUN;
      var s=state(stat,0,.5f);s.levels[opposite]=94;s.progress[opposite]=20;
      check(EquippedAbilityRules080.eligible(s,stat),"Exact current label threshold480 remains eligible");
      s.progress[opposite]=21;check(!EquippedAbilityRules080.eligible(s,stat),"Training toward balance removes the ability at479");
      s.progress[opposite]=20;s.levels[ChaoState.FLY]=5;
      check(!EquippedAbilityRules080.eligible(s,stat),"Fly mixed specialization is not exact Run/Power");
      s.levels[ChaoState.FLY]=0;s.levels[ChaoState.SWIM]=5;
      check(!EquippedAbilityRules080.eligible(s,stat),"Swim mixed specialization is not exact Run/Power");
      s.levels[ChaoState.SWIM]=0;
      check(EquippedAbilityRules080.eligible(s,stat),"Stat decreases restore exact specialization without resetting age");
      s.secondaryType=opposite+1;check(EquippedAbilityRules080.eligible(s,stat),"Stale legacy secondary cache cannot override live label");
      var wrong=state(opposite,0,.5f);wrong.grades[stat]=6;wrong.levels[stat]=99;
      check(!EquippedAbilityRules080.eligible(wrong,stat),"Highest current stat never replaces the locked first evolution identity");
    }
    for(String value:new String[]{"bad","-100","1100","450","900.0"}){
      var s=state(ChaoState.RUN,0,0);s.setTrait(EquippedAbilityRules080.RUN_SETTING,value);String saved=s.encode();
      check(EquippedAbilityRules080.percent(s,ChaoState.RUN)==1000,"Invalid optional old data uses stable default");
      check(s.encode().equals(saved),"Reading malformed optional settings does not rewrite unrelated saves");
    }
    var full=state(ChaoState.RUN,0,0);
    for(int i=0;i<256;i++)check(full.setTrait("foreign."+i,"keep"),"Fixture fills supported trait map");
    String saved=full.encode();check(!EquippedAbilityRules080.cycle(full,ChaoState.RUN),"A full existing trait map declines unsavable edits");
    check(full.encode().equals(saved),"Failed persistence leaves Chao state untouched");
    near(EquippedAbilityRules080.strength(.4,10),4.4,"A normal0.4 attack impulse receives +1000%=4.4");
    near(EquippedAbilityRules080.strength(.4,0),.4,"Zero preserves vanilla force");
    double old=.6,base=.4,extra=.5;
    double normal=(old/2+base)/2+extra,boosted=(old/2+EquippedAbilityRules080.strength(base,10))/2+EquippedAbilityRules080.strength(extra,10);
    near(boosted-old/4,(normal-old/4)*11,"Distinct base/sprint calls scale impulses once; preexisting velocity is not multiplied");
    near(EquippedAbilityRules080.strength(.4,1000),400.4,"Maximum .84 bonus means1001 times the normal impulse");
    return checks+EquippedRun084Test.run();
  }
  public static void main(String[] args){System.out.println("Hidden equipped080: "+run()+" checks passed");}
}
