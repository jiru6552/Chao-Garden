package com.chaogarden.item;

import com.chaogarden.ChaoGarden;
import com.chaogarden.ChaoVariants;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.HolderLookup;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.ShapelessRecipe;
import net.minecraft.world.level.Level;

/** Disabled legacy conversion serializer retained for external data compatibility. */
public final class VariantEggRecipe extends ShapelessRecipe {
  private final String variant;

  public VariantEggRecipe(String variant, ShapelessRecipe base) {
    // Vanilla's shapeless getResultItem simply returns its stored result and
    // does not use the lookup provider. Reuse that codec's validated recipe.
    super(base.getGroup(), base.category(), base.getResultItem(null).copy(), base.getIngredients());
    if (ChaoVariants.find(variant).isEmpty())
      throw new IllegalArgumentException("Unknown themed egg appearance: " + variant);
    ItemStack expected = base.getResultItem(null);
    if (!expected.is(ChaoGarden.item(ChaoVariants.itemId(variant))) || expected.getCount() != 1)
      throw new IllegalArgumentException("Themed egg result must be its single registered egg");
    this.variant = variant;
  }

  public String variant() {
    return variant;
  }

  /** Retained only for saved/external recipe codec compatibility. All conversions are paused. */
  @Override public boolean matches(CraftingInput input,Level level) {return false;}
  @Override public ItemStack assemble(CraftingInput input,HolderLookup.Provider registries) {return ItemStack.EMPTY;}

  @Override
  public RecipeSerializer<?> getSerializer() {
    return ChaoGarden.THEMED_EGG_RECIPE.get();
  }

  public static final class Serializer implements RecipeSerializer<VariantEggRecipe> {
    private static final MapCodec<VariantEggRecipe> CODEC = RecordCodecBuilder.mapCodec(instance ->
        instance.group(
            Codec.STRING.fieldOf("variant").forGetter(VariantEggRecipe::variant),
            new ShapelessRecipe.Serializer().codec().forGetter((VariantEggRecipe recipe) -> recipe))
            .apply(instance, VariantEggRecipe::new));

    private static final StreamCodec<RegistryFriendlyByteBuf, VariantEggRecipe> STREAM_CODEC =
        StreamCodec.of((buffer, recipe) -> {
          buffer.writeUtf(recipe.variant);
          ShapelessRecipe.Serializer.STREAM_CODEC.encode(buffer, recipe);
        }, buffer -> {
          String variant = buffer.readUtf();
          return new VariantEggRecipe(variant, ShapelessRecipe.Serializer.STREAM_CODEC.decode(buffer));
        });

    @Override
    public MapCodec<VariantEggRecipe> codec() {
      return CODEC;
    }

    @Override
    public StreamCodec<RegistryFriendlyByteBuf, VariantEggRecipe> streamCodec() {
      return STREAM_CODEC;
    }
  }
}
