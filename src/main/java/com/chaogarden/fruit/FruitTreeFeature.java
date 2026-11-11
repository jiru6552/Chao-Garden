package com.chaogarden.fruit;

import com.mojang.serialization.Codec;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

/** Wild trees use the actual dimension, including when a biome is shared between dimensions. */
public final class FruitTreeFeature extends Feature<NoneFeatureConfiguration> {
  public FruitTreeFeature(Codec<NoneFeatureConfiguration> codec) { super(codec); }
  @Override public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
    var fruit = WildFruit.forDimension(context.level().getLevel().dimension());
    if (fruit == null) return false;
    boolean grown = FruitContent.growNaturalTree(context.level(), context.origin(), fruit);
    if (grown) for (var pos : FruitContent.podPositions(context.level(),FruitContent.crownPosition(context.origin()),fruit)) {
      var state = context.level().getBlockState(pos);
      if (state.is(FruitContent.podForFruit(fruit)))
        context.level().setBlock(pos, state.setValue(HangingFruitBlock.AGE, 3), 2);
    }
    return grown;
  }
}
