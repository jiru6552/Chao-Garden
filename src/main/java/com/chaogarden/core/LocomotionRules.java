package com.chaogarden.core;

/** SA2 GameCube/HD movement milestones use earned stat points, not stat levels. */
public final class LocomotionRules {
  public static final int WALK_POINTS = 50;
  public static final int RUN_POINTS = 667;
  public static final int DASH_POINTS = 1334;
  public static final int SWIM_POINTS = 100;
  public static final int ADVANCED_SWIM_POINTS = 667;
  public static final int FLY_POINTS = 135;
  /** Minecraft ledge flight is a short glide, never sustained powered flight. */
  public static final int MAX_GLIDE_TICKS = 60;

  public enum GroundGait { CRAWL, WALK, RUN, DASH }

  private LocomotionRules() {}

  private static int points(ChaoState state, int stat) {
    return state == null ? 0 : Math.max(0, Math.min(9999, state.points[stat]));
  }

  public static boolean canWalk(ChaoState state) {
    return state!=null&&state.adult&&!state.dead&&OmochaoRules.isOmochao(state)
        ||points(state, ChaoState.RUN)>=WALK_POINTS;
  }
  public static boolean canRun(ChaoState state) { return points(state, ChaoState.RUN) >= RUN_POINTS; }
  public static boolean canDash(ChaoState state) { return points(state, ChaoState.RUN) >= DASH_POINTS; }
  public static boolean canSwim(ChaoState state) { return points(state, ChaoState.SWIM) >= SWIM_POINTS; }
  public static boolean advancedSwim(ChaoState state) { return points(state, ChaoState.SWIM) >= ADVANCED_SWIM_POINTS; }
  public static boolean canFly(ChaoState state) { return points(state, ChaoState.FLY) >= FLY_POINTS; }

  public static GroundGait groundGait(ChaoState state) {
    if (canDash(state)) return GroundGait.DASH;
    if (canRun(state)) return GroundGait.RUN;
    return canWalk(state) ? GroundGait.WALK : GroundGait.CRAWL;
  }

  /** Bounded Minecraft speed attributes; navigation still chooses pace/destination. */
  public static double movementSpeed(ChaoState state) {
    int run = points(state, ChaoState.RUN);
    return switch (groundGait(state)) {
      case CRAWL -> .075 + run * .0002;
      case WALK -> .17 + Math.max(0,run - WALK_POINTS) * .00007;
      case RUN -> .25 + (run - RUN_POINTS) * .00009;
      case DASH -> .34 + Math.min(.10, (run - DASH_POINTS) / 30000.0);
    };
  }

  /** Water propulsion follows Swim independently of a Chao's land training. */
  public static double waterMovementSpeed(ChaoState state) {
    int swim = points(state, ChaoState.SWIM);
    if (!canSwim(state)) return .055;
    if (!advancedSwim(state)) return .16 + (swim - SWIM_POINTS) * .00012;
    return .24 + Math.min(.12, (swim - ADVANCED_SWIM_POINTS) / 20000.0);
  }
}
