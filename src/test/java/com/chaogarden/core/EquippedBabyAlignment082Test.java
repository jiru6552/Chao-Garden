package com.chaogarden.core;

import java.util.Arrays;
import com.chaogarden.core.EquippedBabyAlignmentRules082.Clock;
import com.chaogarden.core.EquippedBabyAlignmentRules082.Influence;

/** Actual Chao state invariants plus deterministic eligible-time accounting. */
public final class EquippedBabyAlignment082Test {
  private static int checks;
  private static void check(boolean value,String why){checks++;if(!value)throw new AssertionError(why);}
  private static void near(double actual,double expected,String why){check(Math.abs(actual-expected)<.000002,why+": "+actual+" expected "+expected);}
  private static ChaoState baby(){var state=new ChaoState();state.align=.23f;state.hunger=70;state.energy=91;state.giveDrive(ChaoState.RUN);return ChaoState.decode(state.encode());}
  private static void onlyAlignmentChanged(ChaoState state,String original,float originalAlignment,String why){float alignment=state.align;state.align=originalAlignment;check(state.encode().equals(original),why);state.align=alignment;}
  public static int run(){checks=0;pointsAndBounds();identityLocks();dimensions();minuteAndPause();independentClocks();saveAndReload();gardenCoexistence();return checks;}

  private static void pointsAndBounds(){
    var state=baby();float initial=state.align;String original=state.encode();
    check(EquippedBabyAlignmentRules082.applyPoints(state,1),"One configured Hero point changes a baby");near(state.align,initial+.01,"Displayed points convert to saved units");
    check(EquippedBabyAlignmentRules082.applyPoints(state,-1),"One configured Dark point changes a baby");near(state.align,initial,"Opposite kill influences remain reversible");
    onlyAlignmentChanged(state,original,initial,"Kill arithmetic leaves age, life, face, DNA, stats, specialization and strength byte-exact");
    for(double invalid:new double[]{0,Double.NaN,Double.POSITIVE_INFINITY,Double.NEGATIVE_INFINITY}){
      String before=state.encode();check(!EquippedBabyAlignmentRules082.applyPoints(state,invalid)&&state.encode().equals(before),"Disabled/nonfinite kill delta has no side effects");
    }
    state.align=.999f;check(EquippedBabyAlignmentRules082.applyPoints(state,1)&&state.align==1,"Hero influence clamps exactly at full Hero");
    check(!EquippedBabyAlignmentRules082.applyPoints(state,100)&&state.align==1,"A clamped endpoint cannot overflow");
    state.align=-.999f;check(EquippedBabyAlignmentRules082.applyPoints(state,-1)&&state.align==-1,"Dark influence clamps exactly at full Dark");
    state.align=0;EquippedBabyAlignmentRules082.applyPoints(state,100);check(state.align==1,"Maximum configured100point kill delta uses the documented scale");
    state.align=0;EquippedBabyAlignmentRules082.applyPoints(state,-100);check(state.align==-1,"Negative100point delta uses the same scale");
    state.align=0;EquippedBabyAlignmentRules082.applyPoints(state,.25);near(state.align,.0025,"Fractional points remain correctly scaled");
    state.align=0;EquippedBabyAlignmentRules082.applyPoints(state,10000);check(state.align==1,"Malformed excessive configuration is bounded");
  }

  private static void identityLocks(){
    for(float alignment:new float[]{-.9f,0,.9f})for(int first=0;first<=4;first++){
      var state=baby();state.adult=true;state.align=alignment;state.primaryType=first;state=ChaoState.decode(state.encode());
      // Test the public mirror too: this feature must not normalize an adult
      // and accidentally disguise a mutation of the locked identity.
      String before=state.encode();state.align=-alignment;float mirror=state.align;
      check(!EquippedBabyAlignmentRules082.applyPoints(state,100),"Adult kill alignment is rejected");
      var result=EquippedBabyAlignmentRules082.tickPassive(state,new Clock(1199,1199),Influence.HERO,true);
      check(!result.alignmentChanged()&&result.clock().equals(new Clock(1199,1199)),"Adult time cannot consume or apply a baby minute");
      check(state.align==mirror&&state.lockedAlignment()==alignment&&state.lockedPrimaryType()==first,"Both adult alignment mirror and locked identity remain unchanged");
      state.align=alignment;check(state.encode().equals(before),"Rejected adult effects leave the complete saved state byte-exact");
    }
    for(int blocked=0;blocked<4;blocked++){
      var state=baby();switch(blocked){case 0->state.dead=true;case 1->state.cocoonTicks=20;case 2->state.character="omochao";case 3->state.setTrait("content.construction",1);}
      String before=state.encode();var result=EquippedBabyAlignmentRules082.tickPassive(state,new Clock(1199,1199),Influence.DARK,true);
      check(!EquippedBabyAlignmentRules082.applyPoints(state,-1)&&!result.alignmentChanged()&&state.encode().equals(before),"Inactive and special-form states cannot receive new alignment");
    }
    var immortalBaby=baby();immortalBaby.immortal=true;float alignment=immortalBaby.align;
    check(EquippedBabyAlignmentRules082.applyPoints(immortalBaby,1)&&!immortalBaby.adult&&immortalBaby.immortal,"A real baby is eligible regardless of an independent immortality variant flag");
    near(immortalBaby.align,alignment+.01,"Immortal baby variants use the same one-point scale");
    check(!EquippedBabyAlignmentRules082.applyPoints(null,1),"Missing body state is not an acquisition or alignment target");
  }

  private static void dimensions(){
    for(boolean aether:new boolean[]{false,true}){
      check(EquippedBabyAlignmentRules082.passiveInfluence("minecraft:the_nether",aether)==Influence.DARK,"Nether remains the Dark source with either mod selection");
      check(EquippedBabyAlignmentRules082.passiveInfluence("minecraft:the_end",aether)==(aether?Influence.NONE:Influence.HERO),"Installed Aether removes the End Hero source");
      check(EquippedBabyAlignmentRules082.passiveInfluence("aether:the_aether",aether)==(aether?Influence.HERO:Influence.NONE),"Aether receives Hero time only when installed");
      for(String other:new String[]{"minecraft:overworld","chao_garden:hero_garden","chao_garden:dark_garden","another_mod:the_aether","aether:other_dimension",""})
        check(EquippedBabyAlignmentRules082.passiveInfluence(other,aether)==Influence.NONE,"Unrelated dimensions do not accrue this passive influence");
      check(EquippedBabyAlignmentRules082.passiveInfluence(null,aether)==Influence.NONE,"Missing dimension is harmless");
    }
  }

  private static void minuteAndPause(){
    var state=baby();float initial=state.align;String original=state.encode();Clock clock=Clock.EMPTY;
    for(int tick=1;tick<1200;tick++){var result=EquippedBabyAlignmentRules082.tickPassive(state,clock,Influence.HERO,true);clock=result.clock();check(!result.alignmentChanged(),"No fractional minute is awarded early");}
    check(clock.heroTicks()==1199&&clock.darkTicks()==0&&state.align==initial,"Only actual Hero equipped time accrues");
    for(int tick=0;tick<1400;tick++)clock=EquippedBabyAlignmentRules082.tickPassive(state,clock,Influence.HERO,false).clock();
    for(int tick=0;tick<1400;tick++)clock=EquippedBabyAlignmentRules082.tickPassive(state,clock,Influence.NONE,true).clock();
    check(clock.heroTicks()==1199&&state.align==initial,"Unequipped/invalid context and ordinary dimensions pause rather than reset or catch up");
    var result=EquippedBabyAlignmentRules082.tickPassive(state,clock,Influence.HERO,true);near(state.align,initial+.01,"The1200th eligible tick grants exactly one Hero point");
    check(result.alignmentChanged()&&result.clock().equals(Clock.EMPTY),"A completed minute clears only its residual counter");
    onlyAlignmentChanged(state,original,initial,"Passive minutes do not alter biological age, strength, stats, face, DNA or equipment traits");
    state.align=1;result=EquippedBabyAlignmentRules082.tickPassive(state,new Clock(0,1199),Influence.HERO,true);
    check(!result.alignmentChanged()&&result.clock().equals(Clock.EMPTY),"A capped minute is consumed, preventing banked bonuses after an edit");
    check(new Clock(Integer.MAX_VALUE,-1).equals(Clock.EMPTY)&&new Clock(1200,1200).equals(Clock.EMPTY),"Malformed saved counters cannot manufacture almost-complete minutes");
  }

  private static void independentClocks(){
    var state=baby();float initial=state.align;Clock clock=Clock.EMPTY;
    for(int tick=0;tick<1199;tick++)clock=EquippedBabyAlignmentRules082.tickPassive(state,clock,Influence.DARK,true).clock();
    for(int tick=0;tick<1199;tick++)clock=EquippedBabyAlignmentRules082.tickPassive(state,clock,Influence.HERO,true).clock();
    check(clock.equals(new Clock(1199,1199))&&state.align==initial,"Switching opposite dimensions never joins partial Dark and Hero minutes");
    var dark=EquippedBabyAlignmentRules082.tickPassive(state,clock,Influence.DARK,true);near(state.align,initial-.01,"Nether completes only its own minute");
    check(dark.clock().equals(new Clock(0,1199)),"A Dark completion preserves partial Hero time");
    var hero=EquippedBabyAlignmentRules082.tickPassive(state,dark.clock(),Influence.HERO,true);near(state.align,initial,"Opposite complete minutes can cancel gradually");
    check(hero.clock().equals(Clock.EMPTY),"Both separate minute clocks settle without duplicate credit");
  }

  private static void saveAndReload(){
    var continuous=baby();var resumed=ChaoState.decode(continuous.encode());Clock a=Clock.EMPTY,b=Clock.EMPTY;
    for(int tick=0;tick<5000;tick++){
      var influence=tick%1800<900?Influence.DARK:Influence.HERO;
      a=EquippedBabyAlignmentRules082.tickPassive(continuous,a,influence,true).clock();
      b=EquippedBabyAlignmentRules082.tickPassive(resumed,b,influence,true).clock();
      // The server persists precisely these integer residuals with the body;
      // reconstructing them never reads a wall timestamp or grants offline time.
      b=new Clock(b.darkTicks(),b.heroTicks());resumed=ChaoState.decode(resumed.encode());
    }
    check(a.equals(b)&&continuous.encode().equals(resumed.encode()),"Repeated saved-state reloads preserve exact partial time and alignment without catchup");
  }

  private static void gardenCoexistence(){
    var normal=baby();var garden=ChaoState.decode(normal.encode());Clock normalClock=Clock.EMPTY,gardenClock=Clock.EMPTY;float initial=normal.align;
    for(int tick=1;tick<=1200;tick++){
      normal.tickEquippedCare(tick,1000000,GardenProgressionRules081.Garden.NORMAL);
      garden.tickEquippedCare(tick,1000000,GardenProgressionRules081.Garden.HERO);
      normalClock=EquippedBabyAlignmentRules082.tickPassive(normal,normalClock,Influence.HERO,true).clock();
      gardenClock=EquippedBabyAlignmentRules082.tickPassive(garden,gardenClock,Influence.NONE,true).clock();
    }
    near(normal.align,initial+.01,"End/Aether minute contributes one point independently of ordinary equipped care");
    near(garden.align,initial+.0012,"Hero Garden care is not mistaken for the End/Aether minute");
    check(normalClock.equals(Clock.EMPTY)&&gardenClock.equals(Clock.EMPTY),"Garden-only care cannot accrue a second dimension clock");
    check(normal.ageTicks==0&&garden.ageTicks==0&&!normal.adult&&!garden.adult&&normal.evolutionStrength()==0&&garden.evolutionStrength()==0,"Both sources respect existing paused equipped childhood");
    check(Arrays.equals(normal.levels,garden.levels)&&Arrays.equals(normal.points,garden.points),"Neither environmental source invents training rewards");
  }
}
