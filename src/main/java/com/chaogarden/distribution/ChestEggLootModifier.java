package com.chaogarden.distribution;

import com.chaogarden.ChaoGarden;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.neoforged.neoforge.common.loot.IGlobalLootModifier;
import net.neoforged.neoforge.common.loot.LootModifier;

/** One additive policy handles chest tables in all namespaces and dimensions. */
public final class ChestEggLootModifier extends LootModifier {
  public static final MapCodec<ChestEggLootModifier> CODEC=RecordCodecBuilder.mapCodec(instance->
      codecStart(instance).and(instance.group(
          Codec.doubleRange(0,1).optionalFieldOf("base_chance",EggDistributionRules.BASE_CHANCE).forGetter(m->m.baseChance),
          Codec.doubleRange(0,1).optionalFieldOf("shiny_chance",EggDistributionRules.SHINY_CHANCE).forGetter(m->m.shinyChance),
          Codec.doubleRange(0,1).optionalFieldOf("gem_chance",EggDistributionRules.GEM_CHANCE).forGetter(m->m.gemChance),
          Codec.doubleRange(0,1).optionalFieldOf("dimension_chance",EggDistributionRules.DIMENSION_CHANCE).forGetter(m->m.dimensionChance)))
          .apply(instance,ChestEggLootModifier::new));
  private final double baseChance,shinyChance,gemChance,dimensionChance;
  public ChestEggLootModifier(LootItemCondition[] conditions,double baseChance,double shinyChance,double gemChance,double dimensionChance) {
    super(conditions);this.baseChance=baseChance;this.shinyChance=shinyChance;this.gemChance=gemChance;this.dimensionChance=dimensionChance;
  }
  public static boolean isChest(LootContext context) {
    ResourceLocation id=context.getQueriedLootTableId();
    if(id==null||id.equals(ResourceLocation.withDefaultNamespace("empty")))return false;
    var table=context.getLevel().getServer().reloadableRegistries().getLootTable(ResourceKey.create(Registries.LOOT_TABLE,id));
    return table.getParamSet()==LootContextParamSets.CHEST||ChestFillWitness.matches(context,table);
  }
  @Override protected ObjectArrayList<ItemStack> doApply(ObjectArrayList<ItemStack> loot,LootContext context) {
    if(!isChest(context))return loot;
    String table=context.getQueriedLootTableId().toString(),dimension=context.getLevel().dimension().location().toString();
    var random=context.getRandom();
    if(random.nextDouble()<baseChance)add(loot,EggDistributionRules.baseEgg(random::nextInt));
    if(EggDistributionRules.shinyEligible(dimension,table)&&random.nextDouble()<shinyChance)add(loot,EggDistributionRules.shinyEgg(random::nextInt));
    boolean space=EggDistribution.spaceModLoaded(),moon=space&&EggDistribution.isMoon(dimension);
    if((EggDistributionRules.gemSite(table)||moon)&&random.nextDouble()<gemChance)add(loot,EggDistributionRules.gemEgg(space,moon,random::nextInt));
    if(dimension.equals("minecraft:the_nether")&&random.nextDouble()<dimensionChance)add(loot,"nether_chao_egg");
    if(dimension.equals("minecraft:the_end")&&random.nextDouble()<dimensionChance)add(loot,"end_chao_egg");
    return loot;
  }
  private static void add(ObjectArrayList<ItemStack> loot,String id){loot.add(new ItemStack(ChaoGarden.item(id)));}
  @Override public MapCodec<? extends IGlobalLootModifier> codec(){return CODEC;}
}
