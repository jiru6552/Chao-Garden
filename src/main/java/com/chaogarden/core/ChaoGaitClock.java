package com.chaogarden.core;

/**
 * Render-only stride clock. Distance still starts and stops the feet, while a
 * bounded cadence keeps a fast Chao from replaying several strides each tick.
 */
public final class ChaoGaitClock {
  // The imported one-second walk contains two strides. Even the fastest dash
  // stays below 2.2 strides per second instead of scaling cadence with Run points.
  public static final float MAX_SECONDS_PER_TICK = .055f;
  private static final double RATE_REFERENCE = .14;
  private static final double RESPONSE_TICKS = 4;
  private boolean initialized;
  private float lastAge, lastSwing;
  private double phase, cadence;

  /** Source-clip seconds, shared by all passes rendering this individual Chao. */
  public float sample(float ageTicks, float limbSwing, float movement, boolean upright) {
    if (!Float.isFinite(ageTicks) || !Float.isFinite(limbSwing)
        || !Float.isFinite(movement)) return (float) phase;
    if (!initialized) {
      initialized = true;
      rebase(ageTicks, limbSwing);
      return (float) phase;
    }
    double elapsed = ageTicks - lastAge;
    // Shadows and other render passes must observe the same frame, not advance it.
    if (elapsed <= 0 && elapsed >= -1) return (float) phase;
    double distance = limbSwing - lastSwing;
    if (elapsed < 0 || elapsed > 20 || distance < 0) {
      rebase(ageTicks, limbSwing);
      return (float) phase;
    }
    lastAge = ageTicks;
    lastSwing = limbSwing;
    if (!upright || movement <= .004f || distance <= .000001) {
      cadence = 0;
      return (float) phase;
    }
    double rate = distance / elapsed;
    double target = MAX_SECONDS_PER_TICK * rate / (rate + RATE_REFERENCE);
    double decay = Math.exp(-elapsed / RESPONSE_TICKS);
    // Exact integral of the cadence easing makes 30/60/120 FPS agree, and avoids
    // a phase jump when movement speed or WALK/RUN/DASH changes in mid-stride.
    double advance = target * elapsed
        + (cadence - target) * RESPONSE_TICKS * (1 - decay);
    cadence = target + (cadence - target) * decay;
    phase = (phase + advance) % 1;
    return (float) phase;
  }

  private void rebase(float ageTicks, float limbSwing) {
    lastAge = ageTicks;
    lastSwing = limbSwing;
    cadence = 0;
  }
}
