package com.chaogarden.core;

import com.chaogarden.ChaoVariants;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Exercises catalog gates without loading a Minecraft server or optional mod classes. */
public final class ChaoVariantAvailabilityTest {
  private static int checks;

  private static void check(boolean value, String message) {
    checks++;
    if (!value) throw new AssertionError(message);
  }

  public static int run() {
    checks = 0;
    Set<String> expected = Set.of("create", "thaumcraft", "astral_sorcery", "blood_magic",
        "refined_storage", "cataclysm", "ice_fire", "aether", "twilight_forest", "nether", "end",
        "ars_nouveau", "occultism", "iron_spells", "projecte", "malum", "cobblemon",
        "draconic_evolution", "born_in_chaos");
    check(ChaoVariants.all().stream().map(ChaoVariants.Definition::id)
        .collect(java.util.stream.Collectors.toSet()).equals(expected),
        "The active catalog contains exactly the retained 19 themes");
    check(ChaoVariants.retiredIds().size() == 29, "Exactly 29 integrations are retired");
    check(ChaoVariants.name("blood_magic").equals("Neo Vitae")
            && ChaoVariants.name("neovitae").equals("Neo Vitae"),
        "Old save ID and current command alias display Neo Vitae");
    check(ChaoVariants.itemId("neovitae").equals("blood_magic_chao_egg"),
        "Current command alias resolves the stable registered egg");
    check(ChaoVariants.commandId("blood_magic").equals("neovitae"),
        "Command suggestions expose the current Neo Vitae identity");
    check(ChaoVariants.available("blood_magic", "neovitae"::equals)
            && ChaoVariants.available("neovitae", "neovitae"::equals),
        "The actual Neo Vitae mod ID enables its theme");
    check(!ChaoVariants.available("blood_magic", "bloodmagic"::equals),
        "The old unrelated Blood Magic mod ID no longer unlocks Neo Vitae");
    for (String retired : ChaoVariants.retiredIds()) {
      check(ChaoVariants.find(retired).isEmpty(), "A retired theme is absent from the active catalog");
      check(!ChaoVariants.available(retired, mod -> true), "No loaded-mod combination can enable a retired theme");
      ChaoState saved = ChaoState.egg(8, true, false, 3, new java.util.Random(1400));
      saved.variant = retired; saved.reincarnations = 2; saved.train(ChaoState.RUN, 412, new java.util.Random(1401));
      check(ChaoVariants.activeId(saved, mod -> true).equals("normal"), "Retired visual fallback is ordinary");
      ChaoState ordinary = ChaoState.decode(saved.encode()); ordinary.variant = "normal";
      check(ChaoVariants.normalizeRetired(saved).encode().equals(ordinary.encode()),
          "Retirement changes only the appearance, preserving genes/care/training/random state");
      check(ChaoVariants.normalizeRetired(saved) == saved, "Normalization is in-place and idempotent");
    }
    check(ChaoVariants.available("normal", mod -> false), "Base Chao needs no integration");
    check(!ChaoVariants.available("not_a_registered_theme", mod -> true),
        "Loading all mods must not admit an unknown appearance");
    check(ChaoVariants.available("nether", mod -> false), "Nether theme is standalone");
    check(ChaoVariants.available("end", mod -> false), "End theme is standalone");

    var aliasSource = new ArrayList<>(List.of("first_mod", "second_mod"));
    var alias = new ChaoVariants.Definition("alias_probe", "Alias Probe", aliasSource);
    aliasSource.clear();
    check(!ChaoVariants.available(alias, mod -> false), "Aliases do not unlock without a mod");
    check(ChaoVariants.available(alias, mod -> mod.equals("first_mod")), "First alias unlocks");
    check(ChaoVariants.available(alias, mod -> mod.equals("second_mod")), "Second alias unlocks");
    check(!ChaoVariants.available(alias, mod -> mod.equals("unrelated_mod")),
        "An unrelated mod does not unlock an alias group");
    check(alias.mods().size() == 2, "Definition defensively copies its alias list");

    Set<String> ids = new HashSet<>();
    Set<String> modIds = new HashSet<>();
    for (var definition : ChaoVariants.all()) {
      check(ids.add(definition.id()), "Appearance ids are unique");
      modIds.addAll(definition.mods());
      check(ChaoVariants.find(definition.id()).orElseThrow().equals(definition),
          "Every catalog entry is addressable");
      check(ChaoVariants.available(definition, mod -> true), "Loaded integrations unlock their theme");
      check(ChaoVariants.available(definition, mod -> false) == definition.mods().isEmpty(),
          "Absent integrations cannot leak a gated theme");
      ChaoState state = new ChaoState();
      state.variant = definition.id();
      check(ChaoVariants.activeId(state, mod -> true).equals(definition.id()),
          "An enabled heritage renders its own appearance");
      String absentId = ChaoVariants.activeId(state, mod -> false);
      check(absentId.equals(definition.mods().isEmpty() ? definition.id() : "normal"),
          "Missing integration uses a normal visual fallback");
      check(state.variant.equals(definition.id()), "Visual fallback cannot erase saved heritage");
      check(ChaoVariants.obtainable(definition)==definition.mods().isEmpty(),"Addon acquisition remains paused regardless of loaded mods");
      ChaoState child=ChaoState.decode(state.encode());
      check(ChaoVariants.normalizeNewCreature(child).variant.equals(definition.mods().isEmpty()?definition.id():"normal"),"New creature cannot inherit a paused addon");
      check(state.variant.equals(definition.id()),"Creation normalization must not alter a parent's heritage");
    }
    // Evaluate the complete matrix: one installed mod may unlock only its own
    // catalog integration (including explicitly declared compatible aliases).
    for (String installed : modIds)
      for (var definition : ChaoVariants.all())
        check(ChaoVariants.available(definition, installed::equals)
                == (definition.mods().isEmpty() || definition.mods().contains(installed)),
            "Single-mod isolation failed: " + installed + " / " + definition.id());
    ChaoState future = new ChaoState();
    future.variant = "future_appearance";
    check(ChaoVariants.activeId(future, mod -> true).equals("normal"),
        "Unknown future appearance renders normal safely");
    check(future.variant.equals("future_appearance"), "Unknown heritage is preserved");
    check(ChaoVariants.normalizeRetired(future).variant.equals("future_appearance"),
        "Retiring known integrations cannot erase an unknown future appearance");
    return checks;
  }
}
