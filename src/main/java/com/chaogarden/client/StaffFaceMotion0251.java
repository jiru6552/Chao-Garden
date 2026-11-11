package com.chaogarden.client;

/** Profession-specific eyelid timing, independent of ordinary garden Chao expressions. */
public final class StaffFaceMotion0251 {
  private StaffFaceMotion0251(){}

  public static float eyelidScale(boolean permanentlyClosed,float ageTicks,long seed){
    // A closed violet lid is authored geometry, not an open eye awaiting a blink.
    if(permanentlyClosed||!Float.isFinite(ageTicks))return 1;
    float cycle=(ageTicks+Math.floorMod(seed,151))%173;
    return cycle>168?Math.max(.08f,Math.abs((cycle-170.5f)/2.5f)):1;
  }
}
