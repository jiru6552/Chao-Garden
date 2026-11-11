package com.chaogarden.core;

/** Cadence, frame-rate independence, stops and activity changes of the real gait clock. */
public final class ChaoGaitClockTest {
  private static int checks;
  private static void check(boolean condition, String message) {
    checks++; if (!condition) throw new AssertionError(message);
  }
  private static double advance(float before, float after) {
    return after >= before ? after - before : 1 + after - before;
  }
  private static double distance(float rate, int framesPerTick) {
    var clock = new ChaoGaitClock();
    float previous = clock.sample(0, 0, rate, true);
    double result = 0;
    for (int frame = 1; frame <= 240 * framesPerTick; frame++) {
      float age = frame / (float) framesPerTick;
      float current = clock.sample(age, age * rate, rate, true);
      double delta = advance(previous, current);
      check(delta >= 0 && delta <= ChaoGaitClock.MAX_SECONDS_PER_TICK / framesPerTick + .000002,
          "A frame must never skip through an excessively fast stride");
      result += delta;
      check(clock.sample(age, age * rate, rate, true) == current,
          "A second render pass must not advance gait phase");
      previous = current;
    }
    return result;
  }
  public static int run() {
    checks = 0;
    double low = distance(.08f, 3), middle = distance(.32f, 3), high = distance(1, 3);
    check(low < middle && middle < high, "Faster travel must still have a faster readable cadence");
    check(high / middle < 1.3 && high / low < 2.5,
        "High Run speed must compress cadence instead of multiplying the walking cycle");
    for (float rate : new float[]{.08f, .32f, 1, 1.25f}) {
      double a = distance(rate, 1), b = distance(rate, 3), c = distance(rate, 6);
      check(Math.abs(a - b) < .00001 && Math.abs(b - c) < .00001,
          "Frame rate must not change travelled animation phase, including the baby stride scale");
    }
    var clock = new ChaoGaitClock();
    float phase = clock.sample(0, 0, .2f, true);
    for (int tick = 1; tick <= 30; tick++) phase = clock.sample(tick, tick * .2f, .2f, true);
    float stopped = clock.sample(31, 6, 0, true);
    for (int tick = 32; tick <= 60; tick++)
      check(clock.sample(tick, 6, 0, true) == stopped, "Stationary feet must hold their phase");
    check(phase == stopped, "Stopping must not add an artificial stride");
    float before = stopped;
    float resumed = clock.sample(61, 6.2f, .2f, true);
    check(advance(before, resumed) < .01, "Movement resumes with a short cadence ease-in");
    for (int tick = 62; tick <= 80; tick++) clock.sample(tick, 6.2f + (tick - 61) * .2f, .2f, true);
    before = clock.sample(81, 10.2f, .2f, true);
    float accelerated = clock.sample(81.25f, 10.45f, 1, true);
    check(advance(before, accelerated) <= ChaoGaitClock.MAX_SECONDS_PER_TICK * .25f,
        "A speed milestone must not rescale the accumulated phase");
    before = accelerated;
    check(clock.sample(81.25f, 10.45f, 1, false) == before,
        "A same-frame activity change must not shift the feet");
    float inactive = clock.sample(82, 11, 1, false);
    check(inactive == before && clock.sample(83, 12, 1, false) == inactive,
        "Toy, swimming and held animations must not accumulate hidden strides");
    check(clock.sample(120, 100, 1, true) == inactive,
        "Returning after culling must rebase rather than replay the offscreen distance");
    check(clock.sample(1, 0, .2f, true) == inactive,
        "Entity time resets must preserve a finite pose and rebase safely");
    float teleport = clock.sample(2, 1000, 1, true);
    check(advance(inactive, teleport) < ChaoGaitClock.MAX_SECONDS_PER_TICK,
        "A teleport must never turn into a burst of strides");
    check(clock.sample(Float.NaN, 0, 1, true) == teleport
        && clock.sample(3, Float.POSITIVE_INFINITY, 1, true) == teleport,
        "Invalid input must not poison subsequent gait frames");
    check(Float.isFinite(clock.sample(3, 1000.2f, .2f, true)),
        "Valid input must recover after an invalid render pass");
    return checks;
  }
  public static void main(String[] args) { System.out.println("Readable running cadence: " + run() + " checks passed"); }
}
