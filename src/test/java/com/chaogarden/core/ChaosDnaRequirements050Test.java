package com.chaogarden.core;

import java.util.Arrays;
import java.util.Random;

/** Actual lifecycle boundaries with independent species/save fixtures for the 31-DNA rule. */
public final class ChaosDnaRequirements050Test {
  private static int checks;
  private static final int ORIGINAL_SA2_MASK = 0x1fffff;
  private static final int REGISTERED_050_MASK = 0x7fffffff;
  // Emitted by the delivered 0.25.49 JAR, not the encoder under test. Includes
  // original DNA plus Deer and Swallow; no new eligibility bit is fabricated.
  private static final String LEGACY049 =
      "6;0,0;1,1;0,0;0,0;2,2,2,2,2;2,2,2,2,2;2,2,2,2,2;0,0,0,0,0;0,0,0,0,0;0,0,0,0,300;0,0,1,0,0,0,0,19,0,2,0,0,0,0,0,90,0,80,100,0,1092616191,1090519040,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0;24,-1,-1,-1,-1,-1,-1,30;0x0.0p0,0x0.0p0,0x0.0p0;507050;0,0,0,0,0,0,0,0,0,0,0,0,0;2,2,2,2,2,2,2,2,2,2,2,2,2;2,2,2,2,2,2,2,2,2,2,2,2,2;0,0,0,0,0,0,0,0,0,2000,0,0,0,0,0,10000,0,10000;0,0,0,0,0,0;0,0,0,0,0,0;0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,1279803727,1195463236,0,0,0,0,-1,-1,-1,-1,-1,0,0,0,0,0,0,1,0,0,0;foreign.unrelated=a2VwdA;normal;normal";

  private static void check(boolean ok, String message) {
    checks++;
    if (!ok) throw new AssertionError(message);
  }

  public static int run() {
    checks = 0;
    var expected = new String[] {"penguin", "seal", "otter", "peacock", "parrot", "condor",
        "rabbit", "cheetah", "boar", "bear", "tiger", "gorilla", "skunk", "sheep", "raccoon",
        "dragon", "unicorn", "phoenix", "half_fish", "skeleton_dog", "bat", "bee", "ladybug",
        "scorpion", "deer", "elephant", "kangaroo", "koala", "lion", "mole", "swallow"};
    check(Arrays.equals(ChaoState.ANIMALS, expected), "Every current registered species has its existing saved bit index");
    check(ChaoState.ALL_ANIMALS == REGISTERED_050_MASK, "Chaos requires all 31 existing positive saved bits");
    for (int id = 0; id < expected.length; id++) {
      var s = new ChaoState();
      s.giveAnimal(id);
      check(s.animalMask == 1 << id, "A DNA gift records exactly its stable species bit: " + expected[id]);
      check(ChaoState.decode(s.encode()).animalMask == 1 << id, "Each individual DNA bit persists: " + expected[id]);
      for (int alignment = -1; alignment <= 1; alignment++) for (boolean reload : new boolean[] {false, true}) {
        var missing = eligible(REGISTERED_050_MASK & ~(1 << id));
        missing.align = alignment;
        if (reload) missing = ChaoState.decode(missing.encode());
        check(missing.tick(20, 1000, new Random(50)) == ChaoState.Transition.EVOLVED,
            "Missing DNA still permits ordinary first evolution");
        check(!missing.immortal && missing.primaryType == ChaoState.NORMAL,
            "Every DNA species independently blocks Chaos, including after reload: " + expected[id]);
        check(missing.animalMask == (REGISTERED_050_MASK & ~(1 << id)),
            "Evolution does not fabricate an omitted DNA bit");
      }
    }
    for (int alignment = -1; alignment <= 1; alignment++) {
      var full = eligible(REGISTERED_050_MASK); full.align = alignment;
      full = ChaoState.decode(full.encode());
      check(full.tick(20, 1000, new Random(50)) == ChaoState.Transition.EVOLVED && full.immortal,
          "Complete registered DNA permits Chaos across all alignments after reload");
      check(full.typeName().equals(alignment < 0 ? "Devil" : alignment > 0 ? "Angel" : "Light"),
          "Full DNA preserves the original Chaos alignment families");
    }
    preserveOtherRequirements();
    legacySaves();
    nextLife();
    return checks;
  }

  private static ChaoState eligible(int mask) {
    var s = new ChaoState();
    s.happiness = 90; s.reincarnations = 2; s.points[ChaoState.STAMINA] = 300;
    s.ageTicks = 19; s.animalMask = mask;
    return s;
  }

  private static void preserveOtherRequirements() {
    for (int lives = 0; lives < 2; lives++) {
      var s = eligible(REGISTERED_050_MASK); s.reincarnations = lives;
      s.tick(20, 1000, new Random(50));
      check(!s.immortal, "Complete DNA cannot bypass the third-life requirement");
    }
    for (int happiness : new int[] {79, 80, 81}) {
      var s = eligible(REGISTERED_050_MASK); s.happiness = happiness;
      s.tick(20, 1000, new Random(50));
      check(s.immortal == (happiness > 80), "The strictly-over-80 care requirement remains unchanged");
    }
    for (int type = ChaoState.SWIM; type <= ChaoState.POWER; type++) {
      var s = eligible(REGISTERED_050_MASK); s.points[type] = 301;
      s.tick(20, 1000, new Random(50));
      check(!s.immortal && s.primaryType == type + 1, "All DNA cannot bypass the Normal first-type rule");
    }
    var adult = eligible(REGISTERED_050_MASK); adult.adult = true;
    adult.tick(20, 1000, new Random(50));
    check(!adult.immortal, "Adult DNA completion does not retroactively evolve a Chao");
  }

  private static void legacySaves() {
    var old = ChaoState.decode(LEGACY049);
    check(old.encode().equals(LEGACY049), "Literal 0.25.49 save remains byte-identical after roundtrip");
    check(old.animalMask == 1092616191 && old.animalPartsMask == 1090519040,
        "Original and additional acquired DNA bits are preserved without auto-completion");
    check(old.animalParts[0] == 24 && old.animalParts[7] == 30 && old.trait("foreign.unrelated").equals("kept"),
        "Saved mixed parts and unrelated traits survive the requirement update");
    old.tick(20, 1000, new Random(50));
    check(!old.immortal && old.adult, "A legacy partial checklist evolves ordinarily rather than receiving free new DNA");
    var original = eligible(ORIGINAL_SA2_MASK);
    original = ChaoState.decode(original.encode());
    original.tick(20, 1000, new Random(50));
    check(!original.immortal, "An old complete 21-species checklist now needs the ten remaining species");
    var restored = ChaoState.decode(LEGACY049);
    for (int id = 21; id < ChaoState.ANIMALS.length; id++) restored.giveAnimal(id);
    restored.points[ChaoState.STAMINA] = 9999;
    check(restored.animalMask == REGISTERED_050_MASK, "Offering missing samples completes the existing saved checklist");
    restored.tick(20, 1000, new Random(50));
    check(restored.immortal, "A migrated child can still attain Chaos after acquiring the missing species");
    for (int alignment = -1; alignment <= 1; alignment++) {
      var chaos = eligible(ORIGINAL_SA2_MASK); chaos.adult = chaos.immortal = true;
      chaos.align = alignment; chaos = ChaoState.decode(chaos.encode());
      for (int i = 0; i < 1000; i++) chaos.tick(20, 20, new Random(i));
      check(chaos.immortal && !chaos.dead && chaos.animalMask == ORIGINAL_SA2_MASK,
          "Existing Chaos remains immortal without demanding new DNA or mutating its saved history");
    }
  }

  private static void nextLife() {
    var s = eligible(REGISTERED_050_MASK); s.adult = true; s.adultAgeTicks = 19;
    s.variant = "create"; s.setTrait("foreign.kept", "yes");
    check(s.tick(20, 20, new Random(50)) == ChaoState.Transition.REINCARNATED,
        "Ordinary reincarnation still follows the existing lifecycle");
    s = ChaoState.decode(s.encode());
    check(s.reincarnations == 3 && s.animalMask == 0 && s.variant.equals("create") && s.trait("foreign.kept").equals("yes"),
        "Reincarnation preserves identity/other data and resets only current-life DNA as before");
    s.cocoonTicks = 0; s.ageTicks = 19; s.points[ChaoState.STAMINA] = 9999;
    s.tick(20, 1000, new Random(50));
    check(!s.immortal, "Previous-life DNA does not leak into the next life eligibility check");
  }
}
