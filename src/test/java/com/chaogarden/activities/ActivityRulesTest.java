package com.chaogarden.activities;

import com.chaogarden.core.ChaoState;

public final class ActivityRulesTest {
  private static int checks;
  private static void check(boolean value,String message){checks++;if(!value)throw new AssertionError(message);}
  public static int run(){
    checks=0;
    check(ActivityRules.linkLength(32,0,0),"32-block link must be accepted");
    check(!ActivityRules.linkLength(32.01,0,0),"Overlong link must be rejected");
    check(!ActivityRules.linkLength(0,0,0),"Self-link must be rejected");
    check(!ActivityRules.linkLength(24,0,24),"Diagonal distance must use actual length");
    check(ActivityRules.linkLength(16,16,16),"Valid three-dimensional link rejected");
    var base=new ChaoState();var runner=new ChaoState();runner.points[ChaoState.RUN]=2400;
    check(ActivityRules.raceSpeed(runner,false,false,false,50)>ActivityRules.raceSpeed(base,false,false,false,50),"Run points must affect ground speed");
    check(ActivityRules.raceSpeed(runner,false,false,false,0)<=ActivityRules.raceSpeed(runner,false,false,false,50)*.201,"Exhaustion must remove at least80% speed");
    var swimmer=new ChaoState();swimmer.points[ChaoState.SWIM]=2400;
    check(ActivityRules.raceSpeed(swimmer,true,false,false,50)>ActivityRules.raceSpeed(runner,true,false,false,50),"Water must favor swim rather thanrun");
    var flyer=new ChaoState();flyer.points[ChaoState.FLY]=2400;
    check(ActivityRules.raceSpeed(flyer,false,true,false,50)>ActivityRules.raceSpeed(runner,false,true,false,50),"Descents must favor flight");
    var power=new ChaoState();power.points[ChaoState.POWER]=2400;
    check(ActivityRules.raceSpeed(power,false,false,true,50)>ActivityRules.raceSpeed(runner,false,false,true,50),"Climbs must favor power");
    check(ActivityRules.karateDamage(power,base)>ActivityRules.karateDamage(base,base),"Power must increasekaratedamage");
    check(ActivityRules.karateDamage(power,swimmer)<ActivityRules.karateDamage(power,base),"Swim mustdefendinkarate");
    check(ActivityRules.evasion(flyer)>ActivityRules.evasion(base),"Fly mustgiveevasion");
    check(ActivityRules.attackInterval(runner)<ActivityRules.attackInterval(base),"Run mustincreaseattackfrequency");
    var stamina=new ChaoState();stamina.points[ChaoState.STAMINA]=2400;
    check(ActivityRules.karateHealth(stamina)>ActivityRules.karateHealth(base),"Stamina mustincreasevirtualhealth");
    for(int value=0;value<=9999;value+=37){
      for(int stat=0;stat<5;stat++)base.points[stat]=value;
      check(Double.isFinite(ActivityRules.raceSpeed(base,true,true,true,1)),"Race speed must stayfinite");
      check(ActivityRules.attackInterval(base)>=7,"Attackinterval bounded");
      check(ActivityRules.evasion(base)<=.4,"Evasion bounded");
      check(ActivityRules.karateDamage(base,base)>=1,"Damage remainspositive");
    }
    // Exhaust every valid persisted point value; these invariants do not
    // duplicate the production curves or their rounding expressions.
    var varied=new ChaoState();var untrained=new ChaoState();var hardHitter=new ChaoState();
    hardHitter.points[ChaoState.POWER]=9999;
    double groundBefore=-1,waterBefore=-1,fallBefore=-1,climbBefore=-1,healthBefore=-1,damageBefore=-1,defenseBefore=Double.POSITIVE_INFINITY,evasionBefore=-1;
    int intervalBefore=Integer.MAX_VALUE;
    for(int points=0;points<=9999;points++) {
      java.util.Arrays.fill(varied.points,points);
      double ground=ActivityRules.raceSpeed(varied,false,false,false,100);
      double water=ActivityRules.raceSpeed(varied,true,false,false,100);
      double fall=ActivityRules.raceSpeed(varied,false,true,false,100);
      double climb=ActivityRules.raceSpeed(varied,false,false,true,100);
      double health=ActivityRules.karateHealth(varied),damage=ActivityRules.karateDamage(varied,untrained);
      double defended=ActivityRules.karateDamage(hardHitter,varied),evasion=ActivityRules.evasion(varied);
      int interval=ActivityRules.attackInterval(varied);
      check(Double.isFinite(ground)&&ground>=groundBefore&&ground>=.8&&ground<=2.100001,"Ground race curve bounds/monotonicity");
      check(Double.isFinite(water)&&water>=waterBefore&&water>=.44&&water<=2.100001,"Water race unlock and curve monotonicity");
      check(Double.isFinite(fall)&&fall>=fallBefore&&fall>=.8&&fall<=2.100001,"Descending race curve monotonicity");
      check(Double.isFinite(climb)&&climb>=climbBefore&&climb>=.8&&climb<=2.100001,"Climbing race curve monotonicity");
      check(Double.isFinite(health)&&health>=healthBefore&&health>=40&&health<=180,"Virtual karate health bounds");
      check(Double.isFinite(damage)&&damage>=damageBefore&&damage>=3&&damage<=23,"Karate attacker strength bounds");
      check(Double.isFinite(defended)&&defended<=defenseBefore&&defended>=1&&defended<=23,"Karate defense cannot increase received damage");
      check(Double.isFinite(evasion)&&evasion>=evasionBefore&&evasion>=0&&evasion<=.4,"Evasion probability bounds");
      check(interval<=intervalBefore&&interval>=7&&interval<=26,"Attack interval remains bounded and improves monotonically");
      check(Math.abs(ActivityRules.raceSpeed(varied,false,false,false,0)-ground*.2)<1e-12,"Exhaustion applies one80percent reduction");
      groundBefore=ground;waterBefore=water;fallBefore=fall;climbBefore=climb;healthBefore=health;damageBefore=damage;defenseBefore=defended;evasionBefore=evasion;intervalBefore=interval;
    }
    for(double hp:new double[]{-1,0,1})for(double opponent:new double[]{-1,0,1})for(boolean out:new boolean[]{false,true})for(boolean opponentOut:new boolean[]{false,true})
      check(ActivityRules.canAttack(hp,opponent,out,opponentOut)==(hp>0&&opponent>0&&!out&&!opponentOut),"Only two active fighters can exchange another blow");
    check(!ActivityRules.linkLength(Double.NaN,0,0)&&!ActivityRules.linkLength(Double.POSITIVE_INFINITY,0,0),"Nonfinite marker links rejected");
    check(ActivityRules.addLifetimeExperience(Integer.MAX_VALUE-2,24)==Integer.MAX_VALUE,"Activity XP saturates instead of wrapping");
    check(ActivityRules.addLifetimeExperience(Integer.MAX_VALUE,Integer.MAX_VALUE)==Integer.MAX_VALUE,"Maximum XP addition remains bounded");
    check(ActivityRules.addLifetimeExperience(-90,24)==24&&ActivityRules.addLifetimeExperience(20,-10)==20,"Malformed XP values cannot subtract or wrap");
    return checks;
  }
}
