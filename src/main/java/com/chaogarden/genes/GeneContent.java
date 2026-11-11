package com.chaogarden.genes;

import com.chaogarden.ChaoGarden;
import com.chaogarden.core.ChaoState;
import com.mojang.serialization.MapCodec;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.loot.IGlobalLootModifier;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

public final class GeneContent {
  private static final DeferredRegister<MapCodec<? extends IGlobalLootModifier>> LOOT =
      DeferredRegister.create(NeoForgeRegistries.Keys.GLOBAL_LOOT_MODIFIER_SERIALIZERS,
          ChaoGarden.MODID);
  private static boolean bootstrapped;

  static {
    LOOT.register("generic_chest_training", () -> GenericChestTrainingModifier.CODEC);
    LOOT.register("chest_eggs", () -> com.chaogarden.distribution.ChestEggLootModifier.CODEC);
  }

  private GeneContent() {}

  public static void bootstrap() {
    if (bootstrapped) return;
    bootstrapped = true;
    for (int index = 0; index < ChaoGarden.ANIMALS.length; index++) {
      final int animal = index;
      String name = ChaoGarden.ANIMALS[index];
      ChaoGarden.add(name + "_animal_gene", () -> new GeneItem(name, animal));
    }
  }

  public static void register(IEventBus bus) {
    LOOT.register(bus);
  }

  public static List<String> ids() {
    return java.util.Arrays.stream(ChaoGarden.ANIMALS).map(name -> name + "_animal_gene").toList();
  }

  public static Item gene(int index) {
    return ChaoGarden.item(ChaoGarden.ANIMALS[index] + "_animal_gene");
  }

  public static String animalName(int index) {
    if (index < 0 || index >= ChaoGarden.ANIMALS.length) return "Unknown animal";
    if (index == 8) return "Boar (Warthog)";
    String text = ChaoGarden.ANIMALS[index].replace('_', ' ');
    return text.substring(0, 1).toUpperCase(Locale.ROOT) + text.substring(1);
  }

  public static void appendBenefits(int index, List<Component> tips) {
    tips.add(Component.literal(animalName(index) + " animal genes").withStyle(ChatFormatting.AQUA));
    int[] training = ChaoState.animalTraining(index);
    for (int stat = 0; stat < 4; stat++) {
      int amount = training[stat];
      if (amount != 0) tips.add(Component.literal(ChaoState.STATS[stat] + " "
          + (amount > 0 ? "+" : "") + amount + "% training progress")
          .withStyle(amount > 0 ? ChatFormatting.GREEN : ChatFormatting.RED));
    }
    int slots = ChaoState.animalSlots(index);
    var labels = new String[] {"arms", "ears", "crest", "muzzle", "feet", "horns", "tail", "wings"};
    List<String> parts = new ArrayList<>();
    for (int slot = 0; slot < labels.length; slot++)
      if ((slots & (1 << slot)) != 0) parts.add(labels[slot]);
    if (!parts.isEmpty()) {
      tips.add(Component.literal("Possible parts: " + String.join(", ", parts))
          .withStyle(ChatFormatting.GRAY));
      tips.add(Component.literal("One missing part per sample; animal parts can mix.")
          .withStyle(ChatFormatting.GRAY));
    }
    if (index == 18) tips.add(Component.literal("Replaces the floating marker with a green ghost flame.")
        .withStyle(ChatFormatting.LIGHT_PURPLE));
    if (index == 19) tips.add(Component.literal("Removes animal parts and restores hidden feet.")
        .withStyle(ChatFormatting.LIGHT_PURPLE));
    if (index == 20) tips.add(Component.literal("Hides feet, tail and wings until replaced or cleared.")
        .withStyle(ChatFormatting.LIGHT_PURPLE));
    tips.add(Component.literal(index<21?"+2 happiness; +20% intelligence and luck; Chaos checklist.":"+2 happiness; +20% intelligence and luck; bonus animal parts.")
        .withStyle(ChatFormatting.DARK_AQUA));

  }
}
