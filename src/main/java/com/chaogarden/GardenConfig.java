package com.chaogarden;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class GardenConfig {
  public static final ModConfigSpec SPEC;
  public static final ModConfigSpec.IntValue HATCH_TICKS, CHILD_TICKS, ADULT_TICKS;
  public static final ModConfigSpec.IntValue LESSON_TICKS, LESSON_OFFER_TICKS, CLASSROOM_CAPACITY, NEWBORN_EYE_CHANCE, BIRTHDAY_YEAR_TICKS;
  public static final ModConfigSpec.BooleanValue NATURAL_DEATH, EXPANDED_RARE_ACTIONS, BULLY_CHAO, CWE_FRUIT_REBALANCE, MONSTER_EVOLUTION, MONSTER_TRANSPARENCY_FIX;

  public static final ModConfigSpec.ConfigValue<java.util.List<? extends String>> SPACE_MOD_IDS, MOON_DIMENSION_IDS;

  static {
    var b = new ModConfigSpec.Builder();
    b.comment(
            "Chao time advances only while their entity is loaded. 1200 ticks = one minute at 20"
                + " TPS.")
        .push("lifecycle");
    HATCH_TICKS =
        b.comment("Five minutes to hatch a placed egg.")
            .defineInRange("hatchTicks", 6000, 20, Integer.MAX_VALUE);
    CHILD_TICKS =
        b.comment("Three active hours before first evolution. Minecraft adaptation, configurable.")
            .defineInRange("childTicks", 216000, 200, Integer.MAX_VALUE);
    ADULT_TICKS =
        b.comment("Twelve active adult hours before reincarnation/death.")
            .defineInRange("adultTicks", 864000, 200, Integer.MAX_VALUE);
    NATURAL_DEATH =
        b.comment("When false, unhappy Chao also reincarnate instead of dying of old age.")
            .define("naturalDeath", true);
    b.pop();
    b.comment("Original-game rules remain the default for rare Chao; selected CWE behavior can be enabled here.")
        .push("gardenBehavior");
    EXPANDED_RARE_ACTIONS =
        b.comment("Allow rare Chaos and character Chao to breed and acquire animal parts, as in CWE's optional rare actions.")
            .define("expandedRareActions", false);
    BULLY_CHAO =
        b.comment("Enable optional, nonlethal bullying and intervention between Chao. Friendly social behavior is always available.")
            .define("bullyChao", false);
    CWE_FRUIT_REBALANCE = b.comment("Use CWE's optional stronger food training table instead of the ordinary SA2/Battle food profiles.")
        .define("cweFruitRebalance", false);
    MONSTER_EVOLUTION = b.comment("Optional CWE monster development: adult stats at2000/3000 points can extend influence beyond its ordinary maximum. Shapes/opacity are Minecraft adaptations.")
        .define("monsterEvolution", false);
    MONSTER_TRANSPARENCY_FIX = b.comment("Keep ordinary-colour monster forms opaque, matching CWE's optional transparency fix. Only has an effect with monsterEvolution.")
        .define("fixMonsterTransparency", false);
    NEWBORN_EYE_CHANCE = b.comment("Chance0..100percent to select an ordinary iris color0..8 once for a new Chao. Zero disables the optional CWE birth code; exact source odds are unspecified. Special/alignment eyes remain lens-only.")
        .defineInRange("newbornEyeChancePercent", 0, 0, 100);
    BIRTHDAY_YEAR_TICKS = b.comment("Loaded adult ticks per birthday year. One Minecraft day by default; this calendar is a Minecraft adaptation, not a claimed SA2 year length. Zero disables birthday parties.")
        .defineInRange("birthdayYearTicks", 24000, 0, Integer.MAX_VALUE);
    b.pop();
    b.comment("Lessons advance while the Chao and its classroom are loaded. Modern Wiki lesson timings take precedence over older guides.")
        .push("classroom");
    LESSON_TICKS = b.comment("Length of one lesson: ten minutes by default.")
        .defineInRange("lessonTicks", 12000, 20, Integer.MAX_VALUE);
    LESSON_OFFER_TICKS = b.comment("Length of each rotating lesson offer: thirty minutes by default.")
        .defineInRange("lessonOfferTicks", 36000, 20, Integer.MAX_VALUE);
    CLASSROOM_CAPACITY = b.comment("Maximum simultaneous Chao assigned to one lesson desk.")
        .defineInRange("classroomCapacity", 4, 1, 16);
    b.pop();
    b.push("distribution");
    SPACE_MOD_IDS = b.comment("Loaded space-mod IDs reserve Moon eggs for configured surface Moon dimensions. Add custom integration IDs here.")
        .defineListAllowEmpty(java.util.List.of("spaceModIds"), com.chaogarden.distribution.EggDistributionRules.DEFAULT_SPACE_MODS,
            value -> value instanceof String id && id.matches("[a-z][a-z0-9_]{1,63}"));
    MOON_DIMENSION_IDS = b.comment("Exact surface Moon dimension IDs. Orbit, stations and other planets are not implicit Moon sources.")
        .defineListAllowEmpty(java.util.List.of("moonDimensionIds"), com.chaogarden.distribution.EggDistributionRules.DEFAULT_MOON_DIMENSIONS,
            value -> value instanceof String id && id.matches("[a-z0-9_.-]+:[a-z0-9/._-]+"));
    b.pop();
    SPEC = b.build();
  }
}
