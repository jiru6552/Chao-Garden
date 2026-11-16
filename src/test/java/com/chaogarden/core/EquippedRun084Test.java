package com.chaogarden.core;

/** Numerical engine-recurrence checks; real hooks/movement need native tests. */
public final class EquippedRun084Test {
  private static int checks;
  private static void check(boolean value,String message){checks++;if(!value)throw new AssertionError(message);}
  private static void near(double a,double b,double tolerance,String message){check(Double.isFinite(a)&&Math.abs(a-b)<=tolerance,message+" actual="+a+" expected="+b);}
  public static int run(){
    for(int tier=0;tier<=1000;tier+=100){
      double factor=EquippedRunRules084.multiplier(tier);
      near(factor,1+tier,0,"Saved eleven control tiers retain exact .84 total multipliers");
      near(EquippedRunRules084.flightSpeed(.05f,factor),.05f*factor,.00001,"Creative input scales once from its original base");
      for(float base:new float[]{0,.01f,.05f,.1f}){
        float encoded=base;
        EquippedRunRules084.flightSpeed(base,factor);
        check(Float.floatToIntBits(base)==Float.floatToIntBits(encoded),"Flight scaling changes no stored/custom abilities value");
      }
    }
    for(double invalid:new double[]{Double.NaN,Double.POSITIVE_INFINITY,-1,1001,.1,1,99,150})
      near(EquippedRunRules084.multiplier(invalid),1,0,"Malformed/outside synchronized bonus never enables an unlimited multiplier");
    for(double friction:new double[]{.6,.8,.98,.989})for(double speed:new double[]{.1,100.1,130.13}){
      // Independently integrate native maximal unit-input acceleration/drag.
      double velocity=0,maximumMove=0;
      for(int tick=0;tick<2000;tick++){
        velocity+=speed*.21600002/(friction*friction*friction);
        maximumMove=Math.max(maximumMove,velocity);
        velocity*=friction*.91;
      }
      check(EquippedRunRules084.groundEnvelope(speed,friction)>=maximumMove,"Ground envelope admits native terminal recurrence on vanilla friction");
    }
    double fly=.05f*1001,vertical=0,horizontal=0,max=0;
    for(int tick=0;tick<2000;tick++){
      horizontal+=fly*2;vertical+=fly*3;
      max=Math.max(max,Math.hypot(horizontal,vertical));
      horizontal*=.91;vertical*=.6;
    }
    check(EquippedRunRules084.creativeEnvelope(.05f,1001)>=max-.00001,"Creative envelope admits combined sprint/jump native recurrences");
    near(EquippedRunRules084.flightSpeed(.05f,Double.POSITIVE_INFINITY),.05f,0,"Invalid flight factor preserves native input");
    near(EquippedRunRules084.glideEnvelope(1,.08,1001),1751.75,.00001,"Glide envelope scales a bounded native update once");
    double vanilla=4,authorized=EquippedMovementRules080.expectedSquared(vanilla,302*302,5);
    check(authorized>=302*302*25,"At most five native pending movement packets share the bounded Run allowance");
    near(EquippedMovementRules080.expectedSquared(vanilla,302*302,99),302*302,0,"Packet flood does not multiply the Run allowance");
    return checks;
  }
}
