package com.chaogarden.core;

import java.util.Arrays;
import java.util.Random;

/** No game bootstrap is needed to verify the inheritance and life simulation. */
public final class ChaoStateTest {
  private static int checks;

  private static void check(boolean value, String message) {
    checks++;
    if (!value) throw new AssertionError(message);
  }

  public static void main(String[] args) {
    persistence();
    variantPersistence();
    variantInheritance();
    inheritance();
    training();
    evolution();
    expressedGradeEvolution();
    normalAndChaosStaminaGrade();
    alignment();
    favoriteFruit();
    appendedGenes();
    reincarnation();
    chaos();
    animalParts();
    checks += ChaoPreferencesTest.run();
    checks += ChaoLifeSystem019Test.run();
    checks += RetiredCharacterState020Test.run();
    checks += ChaoFamilyPresentation020Test.run();
    checks += GradualEvolutionRulesTest.run();
    checks += HeroRunMorphology052Test.run();
    checks += OmochaoRulesTest.run();
    checks += LifecycleRules022Test.run();
    checks += ChaoVariantAvailabilityTest.run();
    checks += AnimalGeneStateTest.run();
    checks += ChaosDnaRequirements050Test.run();
    System.out.println("ChaoState: " + checks + " checks passed");
  }

  private static void variantPersistence() {
    // Literal version-2 fixture: changing the current encoder cannot silently update this save.
    String legacy =
        "2;2,14;1,0;0,1;0,12;0,1,2,3,4;5,4,3,2,1;5,1,3,2,4;99,3,5,6,7;9,8,7,6,5;"
            + "9999,321,456,222,987;14,12,1,1,2,320,411,6178,2222,3,1,0,0,2,3,88,66,77,55,1,"
            + "294912,32768,1,0,1,11,12,13,44;15,-1,15,-1,-1,-1,-1,-1;"
            + "0x1.8p-1,-0x1.0p-2,0x1.0p-1;-123456789012345678";
    ChaoState migrated = ChaoState.decode(legacy);
    check(migrated.variant.equals("normal"), "Version-2 saves acquire the normal theme");
    check(
        Arrays.equals(migrated.colorGenes, new int[] {2, 14})
            && Arrays.equals(migrated.toneGenes, new int[] {1, 0})
            && Arrays.equals(migrated.shinyGenes, new int[] {0, 1})
            && Arrays.equals(migrated.jewelGenes, new int[] {0, 12}),
        "Version-2 migration preserves all hidden appearance genes");
    check(
        Arrays.equals(migrated.gradeA, new int[] {0, 1, 2, 3, 4})
            && Arrays.equals(migrated.gradeB, new int[] {5, 4, 3, 2, 1})
            && Arrays.equals(migrated.grades, new int[] {5, 1, 3, 2, 4})
            && Arrays.equals(migrated.levels, new int[] {99, 3, 5, 6, 7})
            && Arrays.equals(migrated.progress, new int[] {9, 8, 7, 6, 5})
            && Arrays.equals(migrated.points, new int[] {9999, 321, 456, 222, 987}),
        "Version-2 migration preserves stat alleles and training");
    check(
        migrated.adult && migrated.primaryType == 2 && migrated.secondaryType == 3
            && migrated.expressedColor == 14 && migrated.expressedJewel == 12
            && migrated.twoTone && migrated.shiny && migrated.favoriteFruit == 2
            && migrated.ageTicks == 6178 && migrated.adultAgeTicks == 2222
            && migrated.reincarnations == 3 && migrated.entropy == -123456789012345678L,
        "Version-2 migration preserves phenotype, lifecycle and entropy");
    check(
        migrated.happiness == 88 && migrated.bond == 66 && migrated.hunger == 77
            && migrated.energy == 55 && migrated.sleeping && migrated.intelligence == 320
            && migrated.luck == 411 && migrated.fruitCount == 11 && migrated.driveCount == 12
            && migrated.petCount == 13 && migrated.cocoonTicks == 44,
        "Version-2 migration preserves every care counter");
    check(
        migrated.animalMask == 294912 && migrated.animalPartsMask == 32768
            && migrated.animalParts[0] == 15 && migrated.animalParts[2] == 15
            && migrated.flameOrb && !migrated.ghostBody && migrated.hatsUnlocked
            && migrated.align == .75f && migrated.swimFly == -.25f && migrated.runPower == .5f,
        "Version-2 migration preserves animal history and evolution axes");
    String normal = migrated.encode();
    check(normal.startsWith("6;") && normal.endsWith(";normal"), "Saves upgrade to version 6");
    ChaoState version3=ChaoState.decode("3"+legacy.substring(1)+";normal");
    check(version3.encode().equals(normal),"Literal version3 preserves all old state with None personality");
    check(version3.personality==ChaoPreferences.NONE && version3.boredom==0
        && Arrays.equals(version3.favoriteFruitGenes,new int[]{2,2}),"Old saves acquire safe default traits and their existing preference");
    migrated.variant = "future_mod_123";
    String themed = migrated.encode();
    check(themed.equals(ChaoState.decode(themed).encode()), "Unknown valid theme survives a complete round-trip");
    check(
        themed.substring(0, themed.lastIndexOf(';'))
            .equals(normal.substring(0, normal.lastIndexOf(';'))),
        "Theme identity does not alter existing saved fields");

    String prefix = themed.substring(0, themed.lastIndexOf(';') + 1);
    for (String invalid : new String[] {"", "Create", "1create", "has space", "../create", "a:b", "a;b", "a".repeat(49)}) {
      ChaoState decoded = ChaoState.decode(prefix + invalid);
      check(decoded.variant.equals("normal"), "Malformed theme becomes normal: " + invalid);
      check(decoded.encode().equals(normal), "Malformed theme preserves all other save data");
    }
    String maxId = "a".repeat(48);
    check(ChaoState.decode(prefix + maxId).variant.equals(maxId), "Maximum-length valid theme remains saved");
    migrated.variant = null;
    check(migrated.encode().equals(normal), "Null in-memory theme cannot corrupt the save format");
    migrated.variant = "create;broken";
    check(migrated.encode().equals(normal), "In-memory separator injection cannot alter save fields");
    migrated.variant = "twilightforest";
    migrated.reincarnate();
    check(migrated.variant.equals("twilightforest"), "Reincarnation keeps the themed identity");
    check(
        ChaoState.decode(migrated.encode()).variant.equals("twilightforest") && !migrated.adult,
        "The reincarnation egg retains its theme after persistence");
  }

  private static void variantInheritance() {
    ChaoState a = ChaoState.egg(2, true, false, 3, new Random(7));
    ChaoState b = ChaoState.egg(14, false, true, 12, new Random(8));
    a.adult = b.adult = true;
    a.variant = "create";
    b.variant = "botania";
    int create = 0, botania = 0;
    for (int seed = 0; seed < 1000; seed++) {
      ChaoState child = ChaoState.breed(a, b, new Random(seed));
      if (child.variant.equals("create")) create++;
      if (child.variant.equals("botania")) botania++;
      check(
          child.variant.equals("create") || child.variant.equals("botania"),
          "Offspring inherits one of the two themed identities");
      check(!child.adult && child.reincarnations == 0, "Inherited theme does not inherit adult lifecycle");
    }
    check(create > 350 && botania > 350 && create + botania == 1000, "Both parents' themes inherit without strong bias");

    class CountingRandom extends Random {
      int booleans;
      CountingRandom(long seed) { super(seed); }
      @Override public boolean nextBoolean() { booleans++; return super.nextBoolean(); }
    }
    a.variant = b.variant = "normal";
    CountingRandom normalRandom = new CountingRandom(77);
    String normalChild = ChaoState.breed(a, b, normalRandom).encode();
    a.variant = b.variant = "create";
    CountingRandom themedRandom = new CountingRandom(77);
    ChaoState themedChild = ChaoState.breed(a, b, themedRandom);
    String themedSave = themedChild.encode();
    check(themedChild.variant.equals("create"), "Matching themed parents always pass on their theme");
    check(normalRandom.booleans == themedRandom.booleans, "Matching themes consume no new random draw");
    check(
        normalChild.substring(0, normalChild.lastIndexOf(';'))
            .equals(themedSave.substring(0, themedSave.lastIndexOf(';'))),
        "Matching themes leave existing seeded gene outcomes unchanged");
  }

  private static void persistence() {
    ChaoState a = ChaoState.egg(8, true, false, 3, new Random(111));
    a.giveAnimal(15);
    a.giveAnimal(18);
    a.giveDrive(2);
    a.pet(.15f);
    a.feed(4, 450, 0);
    a.ageTicks = 6178;
    a.adult = true;
    a.primaryType = 2;
    a.secondaryType = 3;
    a.reincarnations = 2;
    String encoded = a.encode();
    check(encoded.equals(ChaoState.decode(encoded).encode()), "Exact persistence round-trip");
    check(!ChaoState.decode("malformed").adult, "Malformed save is bounded default");
    check(!ChaoState.decode("2;" + "0,".repeat(10000)).adult, "Oversized save is rejected");
  }

  private static void inheritance() {
    ChaoState a = ChaoState.egg(2, true, true, 0, new Random(1));
    ChaoState b = ChaoState.egg(0, false, false, 4, new Random(2));
    a.adult = b.adult = true;
    a.gradeA = new int[] {0, 1, 2, 3, 4};
    a.gradeB = new int[] {1, 2, 3, 4, 5};
    b.gradeA = new int[] {2, 3, 4, 5, 0};
    b.gradeB = new int[] {3, 4, 5, 0, 1};
    for (int seed = 0; seed < 1000; seed++) {
      ChaoState child = ChaoState.breed(a, b, new Random(seed));
      check(child.expressedColor == 2, "Normal colour is recessive");
      check(child.shiny, "Shiny allele is dominant");
      check(child.expressedJewel == 4, "Non-jewel allele is recessive");
      check(
          child.animalMask == 0 && !child.adult && child.ageTicks == 0,
          "Offspring starts a new life");
      for (int stat = 0; stat < 5; stat++) {
        check(
            child.gradeA[stat] == a.gradeA[stat] || child.gradeA[stat] == a.gradeB[stat],
            "Maternal grade allele");
        check(
            child.gradeB[stat] == b.gradeA[stat] || child.gradeB[stat] == b.gradeB[stat],
            "Paternal grade allele");
        check(
            child.grades[stat] == child.gradeA[stat] || child.grades[stat] == child.gradeB[stat],
            "Expressed inherited grade");
      }
    }
    b.immortal = true;
    boolean rejected = false;
    try {
      ChaoState.breed(a, b, new Random(1));
    } catch (IllegalArgumentException expected) {
      rejected = true;
    }
    check(rejected, "Immortal Chaos Chao cannot breed");
  }

  private static void training() {
    ChaoState s = new ChaoState();
    s.grades[0] = 5;
    s.train(0, 100, new Random(1));
    check(s.levels[0] == 1 && s.progress[0] == 0, "100 progress gives a level");
    check(s.points[0] >= 26 && s.points[0] <= 30, "S-grade level-up range");
    s.train(0, 1_000_000, new Random(1));
    check(s.levels[0] == 99 && s.progress[0] == 0, "Training caps at level99");
    s.train(1, -20, new Random(1));
    check(s.progress[1] == 0, "Negative animal gains do not underflow");
  }

  private static void evolution() {
    ChaoState s = new ChaoState();
    s.points[1] = 9999;
    for (int i = 0; i < 10; i++) s.giveDrive(ChaoState.SWIM);
    check(
        s.tendencyType() == ChaoState.SWIM_TYPE,
        "Tendency selects evolution independently from stat points");
    s.ageTicks = 19;
    int oldGrade = s.grades[ChaoState.FLY];
    check(s.tick(20, 100, new Random(1)) == ChaoState.Transition.EVOLVED, "Child matures");
    check(
        s.adult && s.primaryType == ChaoState.FLY_TYPE && s.grades[ChaoState.FLY] == oldGrade + 1,
        "Requested highest-stat primary selection raises its expressed grade");
    s.cocoonTicks = 0;
    for (int i = 0; i < 20; i++) s.giveDrive(ChaoState.RUN);
    s.adultAgeTicks = 40;
    s.tick(20, 100, new Random(1));
    check(s.secondaryType == ChaoState.RUN_TYPE, "Second evolution follows post-evolution care");
  }

  private static void expressedGradeEvolution() {
    ChaoState displayedB = new ChaoState();
    displayedB.gradeA[0] = 3;
    displayedB.gradeB[0] = 1;
    displayedB.grades[0] = 3;
    displayedB.swimFly = -1;
    displayedB.points[ChaoState.SWIM]=1;
    displayedB.ageTicks = 19;
    displayedB.tick(20, 100, new Random(1));
    check(
        displayedB.grades[0] == 4 && displayedB.gradeA[0] == 4 && displayedB.gradeB[0] == 1,
        "Displayed B/hidden D evolves to A/D");
    ChaoState displayedD = new ChaoState();
    displayedD.gradeA[0] = 3;
    displayedD.gradeB[0] = 1;
    displayedD.grades[0] = 1;
    displayedD.swimFly = -1;
    displayedD.points[ChaoState.SWIM]=1;
    displayedD.ageTicks = 19;
    displayedD.tick(20, 100, new Random(1));
    check(
        displayedD.grades[0] == 2 && displayedD.gradeA[0] == 3 && displayedD.gradeB[0] == 2,
        "Hidden B/displayed D evolves to B/C");
    String upgraded = displayedD.encode();
    check(
        upgraded.equals(ChaoState.decode(upgraded).encode()),
        "Expressed allele improvement persists exactly");
    displayedD.reincarnate();
    check(
        displayedD.grades[0] == 2 && displayedD.gradeA[0] == 3 && displayedD.gradeB[0] == 2,
        "Reincarnation preserves expressed and hidden upgraded genes");
  }

  private static void alignment() {
    ChaoState s = new ChaoState();
    s.feed(4, 40, .15f);
    s.pet(.05f);
    check(Math.abs(s.align - .2f) < .0001f, "Child care changes alignment");
    s.adult = true;
    s.align = .8f;
    s.feed(4, 40, -.15f);
    s.pet(-.05f);
    check(Math.abs(s.align - .8f) < .0001f, "First evolution fixes adult alignment");
    s.reincarnate();
    check(!s.adult && s.align == 1, "Reincarnation retains the previous Hero alignment category");
    s.feed(4, 40, -.15f);
    check(
        !s.adult && Math.abs(s.align - .85f) < .0001f,
        "Child care changes alignment from the retained Hero category");
  }

  private static void normalAndChaosStaminaGrade() {
    for (boolean chaos : new boolean[] {false, true}) {
      for (boolean secondAllele : new boolean[] {false, true}) {
        ChaoState state = new ChaoState();
        int displayed = secondAllele ? 1 : 3;
        state.gradeA[ChaoState.STAMINA] = 3;
        state.gradeB[ChaoState.STAMINA] = 1;
        state.grades[ChaoState.STAMINA] = displayed;
        state.ageTicks = 19;
        state.happiness = 90;
        if (chaos) {
          state.reincarnations = 2;
          state.animalMask = ChaoState.ALL_ANIMALS;
        }
        check(state.tick(20, 100, new Random(1)) == ChaoState.Transition.EVOLVED,
            "Balanced Normal/Chaos reaches first evolution");
        check(state.immortal == chaos && state.primaryType == ChaoState.NORMAL,
            "Only eligible Normal evolution becomes Chaos");
        check(state.grades[ChaoState.STAMINA] == displayed + 1
                && state.gradeA[ChaoState.STAMINA] == (secondAllele ? 3 : 4)
                && state.gradeB[ChaoState.STAMINA] == (secondAllele ? 2 : 1),
            "Normal/Chaos raises only the expressed Stamina allele");
        for (int stat = 0; stat < ChaoState.STAMINA; stat++)
          check(state.grades[stat] == 2 && state.gradeA[stat] == 2 && state.gradeB[stat] == 2,
              "Normal/Chaos grade bonus does not train another ability");
        check(state.encode().equals(ChaoState.decode(state.encode()).encode()),
            "Normal/Chaos expressed Stamina bonus persists");
      }
    }
    ChaoState capped = new ChaoState();
    capped.ageTicks = 19;
    capped.gradeA[ChaoState.STAMINA] = capped.grades[ChaoState.STAMINA] = 5;
    capped.gradeB[ChaoState.STAMINA] = 1;
    capped.tick(20, 100, new Random(1));
    check(capped.grades[ChaoState.STAMINA] == 5 && capped.gradeA[ChaoState.STAMINA] == 5
        && capped.gradeB[ChaoState.STAMINA] == 1,"S-rank Stamina remains capped with its hidden allele intact");
  }

  private static void favoriteFruit() {
    ChaoState s = new ChaoState();
    s.favoriteFruit = 1;
    s.happiness = 20;
    s.feedFruit(1);
    check(s.happiness == 25, "Favorite shaped fruit raises happiness");
    s.feedFruit(0);
    check(s.happiness == 25, "Other shaped fruit has no bonus");
    s.happiness = 99;
    s.feedFruit(1);
    check(s.happiness == 100, "Preference bonus obeys happiness cap");
    s.dead = true;
    s.happiness = 20;
    s.feedFruit(1);
    check(s.happiness == 20, "Passed Chao does not receive care");
    s.adult = true;
    s.dead = false;
    s.cocoonTicks = 10;
    check(!s.canBreed(), "Cocooned Chao cannot breed");
  }

  private static void appendedGenes() {
    ChaoState a = ChaoState.egg(14, false, true, 11, new Random(100));
    ChaoState b = ChaoState.egg(15, true, false, 12, new Random(200));
    a.adult = b.adult = true;
    check(a.baseColorName().equals("Sky blue"), "Appended Sky Blue colour index is stable");
    check(
        ChaoState.JEWELS[a.jewel()].equals("peridot")
            && ChaoState.JEWELS[b.jewel()].equals("topaz"),
        "Appended jewel indices are stable");
    for (int seed = 0; seed < 100; seed++) {
      ChaoState child = ChaoState.breed(a, b, new Random(seed));
      check(child.baseColor() == 14 || child.baseColor() == 15, "Appended colours inherit");
      check(child.jewel() == 11 || child.jewel() == 12, "Appended jewels inherit");
      check(
          child.encode().equals(ChaoState.decode(child.encode()).encode()),
          "Appended gene indices persist");
    }
  }

  private static void reincarnation() {
    ChaoState s = new ChaoState();
    s.adult = true;
    s.happiness = 60;
    s.points[0] = 1255;
    s.levels[0] = 50;
    s.giveAnimal(1);
    s.adultAgeTicks = 99;
    int grade = s.grades[0];
    check(
        s.tick(20, 100, new Random(1)) == ChaoState.Transition.REINCARNATED,
        "Happy Chao reincarnates");
    check(
        !s.adult
            && s.reincarnations == 1
            && s.points[0] == 125
            && s.levels[0] == 1
            && s.grades[0] == grade,
        "Reincarnation retains grades and ten percent points");
    check(
        s.animalMask == 0 && (s.animalPartsMask & ~((1 << 1))) == 0, "Animal checklist resets while only previously acquired parts can remain");
    s.adult = true;
    s.cocoonTicks = 0;
    s.adultAgeTicks = 99;
    s.happiness = 0;
    check(
        s.tick(20, 100, new Random(1)) == ChaoState.Transition.DIED && s.dead,
        "Unhappy Chao passes away");
  }

  private static void chaos() {
    ChaoState s = new ChaoState();
    s.reincarnations = 2;
    s.happiness = 80;
    for (int i = 0; i < ChaoState.ANIMALS.length; i++) s.giveAnimal(i);
    s.train(ChaoState.STAMINA,10000,new Random(17));
    s.swimFly = s.runPower = 0;
    s.ageTicks = 19;
    s.align = 1;
    check(
        s.tick(20, 100, new Random(1)) == ChaoState.Transition.EVOLVED && s.immortal,
        "Every registered DNA species and two reincarnations unlock Chaos");
    check(
        s.typeName().equals("Angel") && s.animalPartsMask == 0 && !s.canBreed(),
        "Angel phenotype and immortality restrictions");
    s.cocoonTicks = 0;
    for (int i = 0; i < 500; i++) s.tick(20, 100, new Random(1));
    check(s.immortal && !s.dead, "Chaos Chao never ages out");
    ChaoState incomplete = new ChaoState();
    incomplete.reincarnations = 2;
    incomplete.happiness = 90;
    incomplete.animalMask = ChaoState.ALL_ANIMALS & ~1;
    incomplete.ageTicks = 19;
    incomplete.tick(20, 100, new Random(1));
    check(!incomplete.immortal, "One missing animal prevents Chaos");
  }

  private static void animalParts() {
    ChaoState s = new ChaoState();
    for (int i = 0; i < 4; i++) s.giveAnimal(6);
    check(s.animalParts[ChaoState.EARS] == 6, "Rabbit ears applied");
    for (int i = 0; i < 6; i++) s.giveAnimal(15);
    check(
        s.animalParts[ChaoState.HORNS] == 15 && s.animalParts[ChaoState.EARS] == 15,
        "Dragon's matching ears and horns replace the rabbit ear set");
    s.giveAnimal(19);
    check(s.animalPartsMask == 0 && s.hatsUnlocked, "Skeleton dog removes parts");
    check(
        Integer.bitCount(s.animalMask) == 3,
        "Acquisition history remains separate from visible parts");
  }
}
