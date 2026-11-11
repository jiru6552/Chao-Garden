package com.chaogarden.companion;

import com.chaogarden.core.ChaoState;
import com.chaogarden.core.OmochaoRules;
import java.util.random.RandomGenerator;

/** Level-scaled companion benefits. Genetic grade and trained level are separate. */
public final class CompanionRules {
  public static final int RECOVERY_TICKS = 1200;
  public static final int MAX_LEVEL = 99;
  public static final int WATER_VISION_LEVEL = 70;
  public static final int BOOST_COOLDOWN_TICKS = 100;
  public static final int HAPPINESS_PENALTY = 10;

  private CompanionRules() {}

  public static boolean equippable(ChaoState state) { return !state.dead && state.cocoonTicks <= 0; }

  public static boolean adult(ChaoState state) {
    return state.adult && !state.dead && state.cocoonTicks <= 0;
  }

  private static double fraction(int level) { return Math.max(0,Math.min(MAX_LEVEL,level))/(double)MAX_LEVEL; }

  /** A level-one helper is modest; a fully trained runner doubles land speed. */
  public static double runSpeedBonus(int level) { return Math.pow(fraction(level),1.2); }

  /** The NeoForge swim-speed multiplier reaches three times its normal value. */
  public static double swimSpeedBonus(int level) { return 2*Math.pow(fraction(level),1.15); }

  /** One finite budget per airborne period: two seconds initially, ten at70, twelve at98. */
  public static int glideTicks(int level) {
    int value=Math.max(0,Math.min(MAX_LEVEL,level));
    return value<=70 ? 40+(int)Math.round(value*160.0/70) : 200+(int)Math.round((value-70)*40.0/28);
  }

  public static boolean waterNightVision(int level) { return level>=WATER_VISION_LEVEL; }
  public static boolean waterBreathing(int level) { return level>=MAX_LEVEL; }

  /** Unenchanted punch1 -> fully charged Netherite sword8 + SharpnessV3. */
  public static float powerDamage(int level) { return (float)(1+10*fraction(level)); }

  public static int attackCooldown(int runLevel) { return 24-(int)Math.round(12*fraction(runLevel)); }

  public static boolean robotBoost(ChaoState state) {
    return adult(state)&&OmochaoRules.allSRank(state);
  }

  /** Unlimited companion flight. Other living adults receive a timed vanilla-elytra permit. */
  public static boolean canElytra(ChaoState state) {
    return adult(state)&&(state.levels[ChaoState.FLY]>=MAX_LEVEL||robotBoost(state));
  }

  /** Pick only trained skills, so a trained Chao really loses one level. */
  public static int defeat(ChaoState state, RandomGenerator random) {
    state.happiness = Math.max(-100, state.happiness - HAPPINESS_PENALTY);
    int[] candidates = new int[5];
    int count = 0;
    for (int i = 0; i < 5; i++) if (state.levels[i] > 0) candidates[count++] = i;
    if (count == 0) return -1;
    int stat = candidates[random.nextInt(count)];
    state.levels[stat]--;
    state.progress[stat] = 0;
    state.points[stat] = Math.max(0, state.points[stat] - (13 + state.grades[stat] * 3));
    return stat;
  }
}
