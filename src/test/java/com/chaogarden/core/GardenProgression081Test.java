package com.chaogarden.core;

import java.util.Arrays;
import java.util.Random;
import com.chaogarden.core.GardenProgressionRules081.Garden;

/** Observable location, saved-clock and identity invariants rather than a second life simulation. */
public final class GardenProgression081Test {
  private static int checks;
  private static void check(boolean value,String why){checks++;if(!value)throw new AssertionError(why);}
  private static void near(double actual,double expected,String why){check(Math.abs(actual-expected)<.000002,why+": "+actual+" expected "+expected);}
  private static ChaoState adult(float alignment){
    var s=new ChaoState();s.adult=true;s.primaryType=ChaoState.RUN_TYPE;s.align=alignment;s.hunger=80;s.energy=100;s.setEvolutionStrength(.2f);
    return ChaoState.decode(s.encode());
  }
  private static String withoutStrength(ChaoState s){
    var fields=s.encode().split(";",-1);var extras=fields[21].split(",",-1);extras[21]=extras[22]="0";fields[21]=String.join(",",extras);return String.join(";",fields);
  }
  public static int run(){checks=0;adultRates();fruit();children();moveAndLocks();equippedAndStored();migrationAndPrecision();pauseAndSleep();return checks;}
  private static void adultRates(){
    for(float alignment:new float[]{-.9f,0,.9f}){
      var seed=adult(alignment);var normal=ChaoState.decode(seed.encode());var hero=ChaoState.decode(seed.encode());var dark=ChaoState.decode(seed.encode());
      for(int tick=0;tick<120;tick++){
        normal.tick(10000,100000,new Random(tick),Garden.NORMAL);hero.tick(10000,100000,new Random(tick),Garden.HERO);dark.tick(10000,100000,new Random(tick),Garden.DARK);
      }
      near(normal.evolutionStrength(),.2+120/40000.0,"Normal retains the configured passive baseline");
      near(hero.evolutionStrength(),.2+240/40000.0,"Hero doubles only future passive visual maturity for every alignment");
      near(dark.evolutionStrength(),.2+60/40000.0,"Dark half-rate remains positive for every alignment");
      check(withoutStrength(normal).equals(withoutStrength(hero))&&withoutStrength(normal).equals(withoutStrength(dark)),"All other encoded adult care, DNA, direction, XP, levels and life counters remain identical");
    }
    check(GardenProgressionRules081.adultMultiplier(null)==1,"Unknown location is an ordinary-rate fallback");
  }
  private static void fruit(){
    for(Garden garden:Garden.values()){
      var adult=adult(-.8f);adult.tick(10000,100000,new Random(1),garden);float before=adult.evolutionStrength();var stats=adult.points.clone();
      adult.onFruitEaten();near(adult.evolutionStrength()-before,.005,"Whole adult fruit strength is independent of garden multiplier");
      check(Arrays.equals(stats,adult.points),"Strength-only fruit completion cannot invent stat training");
      var baby=new ChaoState();baby.onFruitEaten();near(baby.evolutionStrength(),.02,"Child fruit strength is unchanged in all locations");
    }
  }
  private static void children(){
    for(Garden garden:Garden.values()){
      var s=new ChaoState();for(int tick=0;tick<40;tick++)s.tick(1000,100000,new Random(tick),garden);
      near(s.evolutionStrength(),.04,"Garden multiplier does not accelerate first evolution");
      near(s.align,garden==Garden.HERO?.04:garden==Garden.DARK?-.04:0,"Child alignment follows actual eligible care time and garden direction");
      check(s.ageTicks==40&&!s.adult&&s.points[0]==0&&s.levels[0]==0,"Child alignment care changes no time, stat level or XP");
    }
    var tied=new ChaoState();tied.ageTicks=99;tied.align=.49f;tied.points[ChaoState.RUN]=tied.points[ChaoState.FLY]=100;
    tied.train(ChaoState.FLY,1);check(tied.tick(100,100000,new Random(1),Garden.HERO)==ChaoState.Transition.EVOLVED,"A real first evolution still uses the configured child threshold");
    check(tied.primaryType==ChaoState.FLY_TYPE&&tied.lockedAlignment()>=.5f&&tied.evolutionStrength()==0,"The latest highest-stat tie wins and actual alignment is locked at the cocoon boundary");
  }
  private static void moveAndLocks(){
    var s=adult(.9f);for(Garden garden:new Garden[]{Garden.HERO,Garden.DARK,Garden.NORMAL})for(int tick=0;tick<40;tick++)s.tick(10000,100000,new Random(tick),garden);
    near(s.evolutionStrength(),.2+(80+20+40)/40000.0,"Moving gardens uses each future interval once, without retroactive garden credit");
    check(s.align==.9f&&s.lockedPrimaryType()==ChaoState.RUN_TYPE&&s.adultAgeTicks==120,"Garden changes retain biological age, adult alignment and first identity");
    for(int i=0;i<80;i++)s.giveDrive(ChaoState.POWER);
    for(int i=0;i<80;i++)s.giveDrive(ChaoState.FLY);
    check(Arrays.equals(s.secondaryTypes(),new int[]{ChaoState.FLY_TYPE,ChaoState.POWER_TYPE}),"Run-first adults can develop the opposite Power specialization plus Fly");
    for(int i=0;i<160;i++)s.giveDrive(ChaoState.RUN);
    for(int i=0;i<160;i++)s.giveDrive(ChaoState.SWIM);
    check(Arrays.equals(s.secondaryTypes(),new int[]{ChaoState.SWIM_TYPE,ChaoState.RUN_TYPE})&&s.primaryType==ChaoState.RUN_TYPE,"Both secondary axes remain reversible while first identity stays locked");
  }
  private static void equippedAndStored(){
    for(Garden garden:Garden.values()){
      var s=adult(0);for(int tick=1;tick<=120;tick++)s.tickEquippedCare(tick,10000,garden);
      near(s.evolutionStrength(),.2+120*GardenProgressionRules081.adultMultiplier(garden)/40000.0,"Exclusive equipped-body care applies location multiplier exactly once");
      check(s.ageTicks==0&&s.adultAgeTicks==0,"Equipped care retains paused lifespan and birthday counters");
      var baby=new ChaoState();for(int tick=1;tick<=40;tick++)baby.tickEquippedCare(tick,1000,garden);
      check(baby.ageTicks==0&&!baby.adult&&baby.evolutionStrength()==0,"Equipped baby's childhood and first-evolution strength remain paused");
      near(baby.align,garden==Garden.HERO?.04:garden==Garden.DARK?-.04:0,"Existing equipped care can nudge child alignment without resuming its age");
      String stored=s.encode();for(int i=0;i<20;i++)check(ChaoState.decode(stored).encode().equals(stored),"A stored snapshot receives no implicit clock or location effect on reload");
    }
  }
  private static void migrationAndPrecision(){
    var original=adult(.8f);original.adultAgeTicks=80;original.setEvolutionStrength(.4f);
    var fields=original.encode().split(";",-1);var extras=fields[21].split(",",-1);
    check(extras.length==43,"Garden rounding appends two fields after the exact41-field prior layout");
    fields[21]=String.join(",",Arrays.copyOf(extras,41));var old080=ChaoState.decode(String.join(";",fields));
    old080.tick(100,100000,new Random(1),Garden.HERO);near(old080.evolutionStrength(),.405,"An initialized .80 save receives only one new Hero tick, not multiplied historical age");
    fields[0]="5";fields[21]=String.join(",",Arrays.copyOf(extras,21));var legacy=ChaoState.decode(String.join(";",fields));
    legacy.tick(100,100000,new Random(1),Garden.HERO);near(legacy.evolutionStrength(),82/400.0,"Old age-only migration initializes80 neutral history ticks then one doubled future tick");
    var seed=new ChaoState();seed.align=.75f;var continuous=ChaoState.decode(seed.encode());var resumed=ChaoState.decode(seed.encode());
    for(int tick=0;tick<400;tick++){
      continuous.tick(Integer.MAX_VALUE,Integer.MAX_VALUE,new Random(tick),Garden.HERO);
      resumed.tick(Integer.MAX_VALUE,Integer.MAX_VALUE,new Random(tick),Garden.HERO);resumed=ChaoState.decode(resumed.encode());
    }
    check(continuous.align>.75f&&continuous.encode().equals(resumed.encode()),"Tiny valid configured increments survive float rounding and per-tick save/reload without drift or rerolls");
    for(int i=0;i<256;i++)continuous.setTrait("full."+i,i);
    var before=continuous.align;for(int tick=0;tick<200;tick++)continuous.tick(Integer.MAX_VALUE,Integer.MAX_VALUE,new Random(tick),Garden.HERO);
    check(continuous.align>before,"A saturated optional trait map cannot suppress alignment progression");
  }
  private static void pauseAndSleep(){
    var sleeping=adult(.8f);sleeping.sleeping=true;for(int tick=0;tick<20;tick++)sleeping.tick(10000,100000,new Random(tick),Garden.HERO);
    near(sleeping.evolutionStrength(),.201,"Normal sleeping care retains passive visual development");
    var cocoon=adult(.8f);cocoon.cocoonTicks=20;for(int tick=0;tick<20;tick++)cocoon.tick(10000,100000,new Random(tick),Garden.HERO);
    near(cocoon.evolutionStrength(),.2,"Cocoon time is not counted as extra adult development");check(cocoon.adultAgeTicks==0,"Cocoon lifespan remains paused");
    var dead=adult(.8f);dead.dead=true;String stored=dead.encode();dead.tick(10000,100000,new Random(1),Garden.HERO);dead.tickEquippedCare(1,10000,Garden.HERO);check(dead.encode().equals(stored),"Dead Chao receive no care-clock effects");
  }
}
