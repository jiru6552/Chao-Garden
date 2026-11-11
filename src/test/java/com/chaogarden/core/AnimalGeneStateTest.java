package com.chaogarden.core;

import java.util.Arrays;

/** Independent training/appearance fixtures for the Info Center modern profile. */
public final class AnimalGeneStateTest {
  private static int checks;

  private static void check(boolean condition, String message) {
    checks++;
    if (!condition) throw new AssertionError(message);
  }

  public static int run() {
    checks = 0;
    int[][] expected = {
      {36,12,8,-24},{40,-16,0,8},{44,-4,8,-16},
      {12,48,-16,-12},{0,48,-20,4},{20,60,-24,16},
      {0,8,40,-16},{-8,-8,40,8},{-4,-12,32,16},
      {8,-16,4,36},{-8,-16,20,36},{-4,-8,4,40},
      {8,12,12,8},{8,12,20,20},{20,12,4,8},
      {20,4,8,32},{16,12,36,0},{12,32,4,16},
      {32,0,8,24},{8,8,32,16},{8,40,8,8}
    };
    for (int animal = 0; animal < expected.length; animal++) {
      ChaoState state = new ChaoState();
      Arrays.fill(state.progress, 50);
      int intelligence = state.intelligence, luck = state.luck;
      state.giveAnimal(animal);
      check((state.animalMask & (1 << animal)) != 0, "Every gene records its Chaos checklist bit");
      check(Arrays.equals(ChaoState.animalTraining(animal), expected[animal]),
          "Published modern stat profile differs for " + animal);
      for (int stat = 0; stat < 4; stat++) {
        int sum = 50 + expected[animal][stat];
        check(state.levels[stat] == sum/100 && state.progress[stat] == sum%100,
            "Gene progress/negative cost differs for " + animal + "/" + stat);
      }
      check(state.intelligence == intelligence && state.luck == luck && state.hiddenProgress[0]==20 && state.hiddenProgress[1]==20,
          "Gene hidden training must be explicit and bounded");
      check(state.encode().equals(ChaoState.decode(state.encode()).encode()),
          "Gene parts/special flags must survive a save roundtrip");
      int[] defensive = ChaoState.animalTraining(animal); defensive[0] = 999;
      check(ChaoState.animalTraining(animal)[0] == expected[animal][0],
          "Tooltip callers cannot mutate training tables");
    }
    progressiveParts();
    ChaoState mixed = new ChaoState();
    complete(mixed, 6); // rabbit ears, arms, feet, tail
    complete(mixed, 3); // peacock leaves rabbit ears/arms but replaces feet/tail/wings
    check(mixed.animalParts[ChaoState.EARS] == 6 && mixed.animalParts[ChaoState.ARMS] == 6,
        "Different genes preserve slots they do not provide");
    check(mixed.animalParts[ChaoState.FEET] == 3 && mixed.animalParts[ChaoState.WINGS] == 3,
        "New parts replace matching slots");
    mixed.giveAnimal(18);
    check(mixed.flameOrb && mixed.animalParts[ChaoState.EARS] == 6,
        "Half Fish changes the marker without wiping animal parts");
    mixed.giveAnimal(20);
    check(mixed.ghostBody && mixed.animalParts[ChaoState.FEET] == -1
        && mixed.animalParts[ChaoState.TAIL] == -1 && mixed.animalParts[ChaoState.WINGS] == -1,
        "Bat hides feet, tail and wings");
    int[] before = mixed.animalParts.clone();
    for (int stat = 0; stat < 4; stat++) mixed.giveDrive(stat);
    check(Arrays.equals(before, mixed.animalParts) && mixed.flameOrb && mixed.ghostBody,
        "Drives cannot add or remove appearance features");
    mixed.giveAnimal(19);
    check(!mixed.ghostBody && mixed.hatsUnlocked && mixed.animalPartsMask == 0,
        "Skeleton Dog clears parts and restores feet");
    check(Arrays.stream(mixed.animalParts).allMatch(value -> value == -1),
        "All acquired slots clear together");
    complete(mixed, 13);
    check(mixed.animalParts[ChaoState.HORNS] == 13 && mixed.animalParts[ChaoState.EARS] == 13,
        "Sheep's matching ears and horns coexist");
    complete(mixed, 6);
    check(mixed.animalParts[ChaoState.EARS] == 6 && mixed.animalParts[ChaoState.HORNS] == -1,
        "Another animal's ears replace incompatible horns");
    complete(mixed, 15);
    check(mixed.animalParts[ChaoState.HORNS] == 15 && mixed.animalParts[ChaoState.EARS] == 15,
        "Dragon completes its matching ears and horns without oscillating");
    mixed.immortal = true; mixed.clearParts(); mixed.flameOrb = mixed.ghostBody = false;
    for (int animal = 0; animal < ChaoState.ANIMALS.length; animal++) mixed.giveAnimal(animal);
    check(mixed.animalMask == ChaoState.ALL_ANIMALS && mixed.animalPartsMask == 0
        && !mixed.flameOrb && !mixed.ghostBody,
        "Chaos forms train and record genes without changing their own appearance");
    return checks;
  }

  private static void complete(ChaoState state, int animal) {
    for (int i = 0; i < Integer.bitCount(ChaoState.animalSlots(animal)); i++)
      state.giveAnimal(animal);
  }

  private static void progressiveParts() {
    // Exercise the user-visible contract across animal families, both ages,
    // and independent saved random histories. There is no whole-set first use.
    for (boolean adult : new boolean[] {false, true}) {
      for (int animal = 0; animal < 18; animal++) {
        int supplied = ChaoState.animalSlots(animal);
        int count = Integer.bitCount(supplied), firstPartsSeen = 0;
        for (int seed = 0; seed < 32; seed++) {
          ChaoState state = new ChaoState();
          state.adult = adult;
          state.entropy = 0x5041525453L + seed * 11731L;
          for (int sample = 1; sample <= count; sample++) {
            state.giveAnimal(animal);
            int expressed = 0;
            for (int slot = 0; slot < state.animalParts.length; slot++) {
              if (state.animalParts[slot] != animal) continue;
              check((supplied & (1 << slot)) != 0, "Animal fabricated an unsupported part");
              expressed++;
              if (sample == 1) firstPartsSeen |= 1 << slot;
            }
            check(expressed == sample,
                "Each repeated sample must add one remaining animal part: " + animal);
            check((state.animalMask & (1 << animal)) != 0,
                "The first sample must record the Chaos checklist independently of full appearance");
            String saved = state.encode();
            ChaoState restored = ChaoState.decode(saved);
            check(saved.equals(restored.encode()), "Partial appearance failed to persist");
            restored.giveAnimal(animal);
            state.giveAnimal(animal);
            check(restored.encode().equals(state.encode()),
                "Reload rerolled the next acquired part or training");
            state = ChaoState.decode(saved);
          }
          int[] full = state.animalParts.clone();
          state.giveAnimal(animal);
          check(Arrays.equals(full, state.animalParts),
              "A completed same-animal set removed or rerolled a part");
        }
        check(Integer.bitCount(firstPartsSeen) > 1,
            "Acquisition must vary the first part across saved random histories");
      }
    }

    // Switching animals should alter one selected slot while preserving the
    // remainder. The only extra removal is the documented ear/horn conflict.
    for (boolean adult : new boolean[] {false, true}) {
      ChaoState state = new ChaoState(); state.adult = adult;
      complete(state, 6);
      for (int sample = 0; sample < 4; sample++) {
        int[] before = state.animalParts.clone();
        state.giveAnimal(3);
        int changed = 0;
        for (int slot = 0; slot < before.length; slot++) if (before[slot] != state.animalParts[slot]) changed++;
        check(changed == 1, "A new bird sample replaced more than its selected body slot");
        check(state.animalParts[ChaoState.EARS] == 6 && state.animalParts[ChaoState.ARMS] == 6,
            "Bird parts erased unrelated rabbit ears or arms");
      }
      complete(state, 13);
      check(state.animalParts[ChaoState.EARS] == 13 && state.animalParts[ChaoState.HORNS] == 13,
          "Same-animal ear/horn pair is not stable in a mixed appearance");
      complete(state, 6);
      check(state.animalParts[ChaoState.EARS] == 6 && state.animalParts[ChaoState.HORNS] == -1,
          "Different-animal ears and horns survived together");
      String saved = state.encode();
      check(Arrays.equals(state.animalParts, ChaoState.decode(saved).animalParts),
          "Existing mixed full and partial sets changed during save migration");
    }
  }
}
