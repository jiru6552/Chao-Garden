package com.chaogarden.core;

/** Location changes visual maturity and a living child's alignment, never training or life time. */
public final class GardenProgressionRules081 {
  public enum Garden { NORMAL, HERO, DARK }
  private GardenProgressionRules081() {}
  public static double adultMultiplier(Garden garden) {
    return garden==Garden.HERO?2: garden==Garden.DARK?.5:1;
  }
  /** One alignment unit per configured childhood of eligible loaded care time. */
  public static double childAlignmentDelta(Garden garden,int configuredChildTicks) {
    return (garden==Garden.HERO?1.0:garden==Garden.DARK?-1.0:0)/GradualEvolutionRules.durationTicks(configuredChildTicks);
  }
  /** Only float-rounding residue is saved; malformed data cannot become an alignment jump. */
  public static double residue(double value) {
    return Double.isFinite(value)&&Math.abs(value)<=Math.ulp(1.0f)?value:0;
  }
}
