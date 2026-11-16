package com.chaogarden.core;

/** Stateless numerical bounds for the authenticated Run ability. No save or
 * input values are accepted here; collision and coordinates remain vanilla. */
public final class EquippedRunRules084 {
  public static final double MAX_MULTIPLIER=1001;
  private EquippedRunRules084() {}
  public static double multiplier(double bonus) {
    // The same modifier ID also serves ordinary trained perks in0..1. Only
    // the discrete secret tiers100..1000 are a flight prediction permit.
    return Double.isFinite(bonus)&&bonus>=0&&bonus<=1000&&bonus%100==0?1+bonus:1;
  }
  public static float flightSpeed(float ordinary,double multiplier) {
    if(!Float.isFinite(ordinary)||ordinary<0||!valid(multiplier))return ordinary;
    double speed=ordinary*multiplier;
    return speed<=Float.MAX_VALUE?(float)speed:ordinary;
  }
  /** Vanilla ground acceleration is speed*.216/friction^3 and ground drag
   * is friction*.91. The air recurrence retains a previous ground launch. */
  public static double groundEnvelope(double speed,double friction) {
    if(!Double.isFinite(speed)||speed<0||!Double.isFinite(friction)||friction<=0||friction>=1.0/.91)return 0;
    double acceleration=speed*.21600002/(friction*friction*friction);
    return Math.max(speed*3,acceleration/(1-friction*.91))+.2/(1-.91);
  }
  /** Max sprinted horizontal flight input and independent jump/crouch input
   * follow the native .91 horizontal and .6 vertical drag recurrences. */
  public static double creativeEnvelope(float ordinary,double multiplier) {
    if(!Float.isFinite(ordinary)||ordinary<0||!valid(multiplier))return 0;
    double h=ordinary*2*multiplier/(1-.91),v=ordinary*3*multiplier/(1-.6);
    return Math.hypot(h,v);
  }
  /** Stored aerodynamic velocity stays unscaled; allow one bounded native
   * steering/gravity update before scaling the physical glide displacement. */
  public static double glideEnvelope(double nativeSpeed,double gravity,double multiplier) {
    if(!Double.isFinite(nativeSpeed)||nativeSpeed<0||!Double.isFinite(gravity)||!valid(multiplier))return 0;
    return (nativeSpeed*1.25+Math.max(.5,Math.abs(gravity)*2))*multiplier;
  }
  private static boolean valid(double value){return Double.isFinite(value)&&value>=1&&value<=MAX_MULTIPLIER;}
}
