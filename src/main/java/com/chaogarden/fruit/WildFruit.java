package com.chaogarden.fruit;

import com.chaogarden.ChaoGarden;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.functions.LootItemFunction;
import net.minecraft.world.level.storage.loot.entries.LootPoolEntryType;
import net.minecraft.world.level.storage.loot.entries.LootPoolSingletonContainer;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import java.util.function.Consumer;
import net.neoforged.fml.ModList;

import javax.annotation.Nullable;

/** A single dimension rule for wild trees and the mod's two bonus fruit rolls. */
public final class WildFruit extends LootPoolSingletonContainer {

  private static final ResourceKey<Level> AETHER = ResourceKey.create(Registries.DIMENSION,
      ResourceLocation.fromNamespaceAndPath("aether", "the_aether"));
  static final MapCodec<WildFruit> CODEC = RecordCodecBuilder.mapCodec(
      fields -> singletonFields(fields).apply(fields, WildFruit::new));

  private WildFruit(int weight, int quality, List<LootItemCondition> conditions, List<LootItemFunction> functions) {
    super(weight, quality, conditions, functions);
  }

  @Nullable
  public static String forDimension(ResourceKey<Level> dimension) {
    if (Level.OVERWORLD.equals(dimension)) return "garden_fruit";
    if (Level.NETHER.equals(dimension)) return "dark_garden_fruit";
    if (AETHER.equals(dimension) && ModList.get().isLoaded("aether")) return "hero_garden_fruit";
    return null;
  }

  @Override protected void createItemStack(Consumer<ItemStack> output, LootContext context) {
    var fruit = forDimension(context.getLevel().dimension());
    if (fruit != null) output.accept(new ItemStack(ChaoGarden.item(fruit)));
  }

  @Override public LootPoolEntryType getType() {
    return FruitContent.REGIONAL_FRUIT.get();
  }
}
