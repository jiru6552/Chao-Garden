package com.chaogarden.item;

import com.chaogarden.ChaoGarden;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.NonNullList;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.*;

/** Vanilla shapeless crafting and recipe-book behavior, with a recipe-local key remainder. */
public final class KeeperJournalRecipe extends ShapelessRecipe {
  public KeeperJournalRecipe(ShapelessRecipe base){
    super(base.getGroup(),base.category(),base.getResultItem(null).copy(),base.getIngredients());
  }
  @Override public NonNullList<ItemStack> getRemainingItems(CraftingInput input){
    var remaining=super.getRemainingItems(input);
    for(int i=0;i<input.size();i++)if(input.getItem(i).is(ChaoGarden.item("chao_key")))
      remaining.set(i,input.getItem(i).copyWithCount(1));
    return remaining;
  }
  @Override public RecipeSerializer<?> getSerializer(){return ChaoGarden.JOURNAL_RECIPE.get();}
  public static final class Serializer implements RecipeSerializer<KeeperJournalRecipe> {
    private static final MapCodec<KeeperJournalRecipe> CODEC=new ShapelessRecipe.Serializer().codec()
        .xmap(KeeperJournalRecipe::new,recipe->recipe);
    private static final StreamCodec<RegistryFriendlyByteBuf,KeeperJournalRecipe> STREAM=
        ShapelessRecipe.Serializer.STREAM_CODEC.map(KeeperJournalRecipe::new,recipe->recipe);
    @Override public MapCodec<KeeperJournalRecipe> codec(){return CODEC;}
    @Override public StreamCodec<RegistryFriendlyByteBuf,KeeperJournalRecipe> streamCodec(){return STREAM;}
  }
}
