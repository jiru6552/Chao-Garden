package com.chaogarden.core;

/** Small deterministic timing rules shared by the synced care actions and their poses. */
public final class ChaoAnimationRules {
  public static final int PET_TICKS = 60;
  public static final int HATCH_TICKS = 64;
  public static final int WATER_TICKS = 55;
  public static final int WATER_COMMIT_TICK = 40;
  private ChaoAnimationRules() {}

  /** Raise, hold the pour through the real watering tick, then lower the can. */
  public static float wateringPour(float progress) {
    float p=unit(progress);
    return smooth(p/.25f)*(1-smooth((p-.76f)/.24f));
  }

  private static float unit(float value) {
    return Float.isFinite(value) ? Math.max(0, Math.min(1, value)) : 0;
  }

  public static float smooth(float value) {
    float t=unit(value);return t*t*(3-2*t);
  }

  /** Three deliberate lifts, bites and lowers during the actual sixty-tick meal. */
  public static float nibbleLift(float progress) {
    float p=unit(progress);
    if(p==1)return 0;
    float cycle=(p*3)%1;
    return smooth(cycle/.20f)*(1-smooth((cycle-.68f)/.30f));
  }

  /** Chewing happens at the mouth, rather than while the fruit is being raised. */
  public static float chew(float progress) {
    float p=unit(progress),cycle=(p*3)%1;
    if(cycle<.24f||cycle>.68f)return 0;
    return (float)Math.sin((cycle-.24f)/.44f*Math.PI*4)
        *smooth((cycle-.24f)/.06f)*(1-smooth((cycle-.61f)/.07f));
  }

  /** One eating sound inside each raised-fruit chewing interval. */
  public static boolean mealSoundAt(int elapsedTicks) {
    return elapsedTicks>=0&&elapsedTicks<60&&elapsedTicks%20==6;
  }

  /** A short natural blink with an individual phase, never a world-wide blink. */
  public static boolean blinking(float age,long seed) {
    if(!Float.isFinite(age))return false;
    int period=86+(int)Math.floorMod(seed,63L);
    float phase=(age+Math.floorMod(seed>>>11,(long)period))%period;
    if(phase<0)phase+=period;
    return phase<3;
  }

  /** Newborns sit while idle; even their small real crawling strides remain visible. */
  public static boolean crawling(float limbAmount) {
    return Float.isFinite(limbAmount)&&limbAmount>.004f;
  }
}
