package com.chaogarden.core;

public final class EquippedMovement080Test {
  private static void check(boolean ok,String message){if(!ok)throw new AssertionError(message);}
  private static void near(double a,double b,String message){check(Double.isFinite(a)&&Math.abs(a-b)<.000001,message+" actual="+a+" expected="+b);}
  public static void run(){
    double impulse=18.7*18.7+.4*.4,decayed=10.2102*10.2102;
    var history=new EquippedMovementRules080.History(100,impulse);
    history.sample(101,decayed);
    near(EquippedMovementRules080.expectedSquared(decayed,history.maximum(101),1),impulse,"First legitimate18.7 step remains explainable after server friction");
    near(EquippedMovementRules080.expectedSquared(decayed,history.maximum(101),2),impulse*4,"Two native batched moves share a bounded real-impulse envelope");
    check(20.28259*20.28259-EquippedMovementRules080.expectedSquared(decayed,history.maximum(101),2)<=200,"Observed legitimate two-step burst fits the native check");
    check(1000*1000-EquippedMovementRules080.expectedSquared(decayed,history.maximum(101),5)>500,"Huge displacement remains rejected by the native threshold");
    history.sample(102,1_000_000);
    near(history.maximum(102),impulse,"Unrelated later movement cannot enlarge the authorized impulse envelope");
    for(int tick=103;tick<=119;tick++){
      history.sample(tick,9);history.sample(tick,4);check(history.size()<=8,"Per-victim history stays bounded and coalesces same-tick records");
    }
    near(history.maximum(119),9,"Old initial impulse ages out after eight ticks");
    check(!history.active(120)&&history.maximum(120)==0,"Movement allowance expires after20 ticks without another authorized hit");
    history.sample(121,1_000_000);check(!history.active(121),"Movement packets cannot renew an expired authorization");
    near(EquippedMovementRules080.expectedSquared(25,0,5),25,"No trusted history leaves ordinary expected velocity unchanged");
    check(EquippedMovementRules080.nativeBurst(0)==1&&EquippedMovementRules080.nativeBurst(6)==1,"Native invalid/overflow burst counts reset to1");
    check(Double.isNaN(EquippedMovementRules080.expectedSquared(Double.NaN,100,1)),"Do not turn a nonfinite native term into a new allowance");
  }
  public static void main(String[] args){run();}
}
