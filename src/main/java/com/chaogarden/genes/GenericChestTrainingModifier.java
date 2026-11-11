package com.chaogarden.genes;

import com.chaogarden.ChaoGarden;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.neoforged.neoforge.common.loot.IGlobalLootModifier;
import net.neoforged.neoforge.common.loot.LootModifier;

/** Additive training supplies for declared or proven physical chest loot in every namespace. */
public final class GenericChestTrainingModifier extends LootModifier {
  public static final MapCodec<GenericChestTrainingModifier> CODEC =
      RecordCodecBuilder.mapCodec(instance -> codecStart(instance).and(instance.group(
          Codec.floatRange(0, 1).optionalFieldOf("drive_chance", .40f).forGetter(m -> m.driveChance),
          Codec.floatRange(0, 1).optionalFieldOf("gene_chance", .12f).forGetter(m -> m.geneChance)))
          .apply(instance, GenericChestTrainingModifier::new));
  private final float driveChance, geneChance;

  public GenericChestTrainingModifier(LootItemCondition[] conditions, float driveChance, float geneChance) {
    super(conditions);
    this.driveChance = driveChance;
    this.geneChance = geneChance;
  }


  @Override
  protected ObjectArrayList<ItemStack> doApply(ObjectArrayList<ItemStack> generated, LootContext context) {
    if (!com.chaogarden.distribution.ChestEggLootModifier.isChest(context)) return generated;
    var random = context.getRandom();
    if (random.nextFloat() < driveChance) {
      String[] drives = {"swim_drive", "fly_drive", "run_drive", "power_drive"};
      generated.add(new ItemStack(ChaoGarden.item(drives[random.nextInt(drives.length)]),
          1 + random.nextInt(2)));
    }
    if (random.nextFloat() < geneChance)
      generated.add(new ItemStack(GeneContent.gene(TrainingSupplyRules.geneIndex(random::nextInt))));
    return generated;
  }

  @Override
  public MapCodec<? extends IGlobalLootModifier> codec() {
    return CODEC;
  }
}
