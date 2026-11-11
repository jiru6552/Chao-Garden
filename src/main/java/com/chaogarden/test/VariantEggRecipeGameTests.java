package com.chaogarden.test;

import com.chaogarden.ChaoGarden;
import com.chaogarden.ChaoVariants;
import com.chaogarden.GardenConfig;
import com.chaogarden.core.ChaoState;
import com.chaogarden.entity.ChaoEggEntity;
import com.chaogarden.item.ChaoEggItem;
import com.chaogarden.item.VariantEggRecipe;
import java.util.List;
import java.util.Random;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.ShapelessRecipe;
import net.minecraft.world.level.GameType;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** Distribution boundaries preserve old individual eggs without creating new addons. */
@GameTestHolder(ChaoGarden.MODID)
@PrefixGameTestTemplate(false)
public final class VariantEggRecipeGameTests {
  @GameTest(template="empty",timeoutTicks=40)
  public static void noBuiltInRecipeProducesAnyChaoEgg(GameTestHelper h) {
    var manager=h.getLevel().getRecipeManager();
    h.assertTrue(manager.byKey(ResourceLocation.fromNamespaceAndPath(ChaoGarden.MODID,"normal_egg")).isEmpty(),"Normal egg remains craftable");
    for(var definition:ChaoVariants.all())h.assertTrue(manager.byKey(ResourceLocation.fromNamespaceAndPath(ChaoGarden.MODID,ChaoVariants.itemId(definition.id()))).isEmpty(),"Themed conversion returned");
    for(var holder:manager.getRecipes())h.assertTrue(!(holder.value().getResultItem(h.getLevel().registryAccess()).getItem() instanceof ChaoEggItem),"Recipe produces a Chao egg: "+holder.id());
    h.succeed();
  }

  @GameTest(template="empty",timeoutTicks=40)
  public static void externalLegacyConversionCannotConsumeOrChangeNativeCoats(GameTestHelper h) {
    var ingredient=NonNullList.of(Ingredient.EMPTY,Ingredient.of(ChaoGarden.item("normal_egg")));
    for(var definition:ChaoVariants.all()) {
      var recipe=new VariantEggRecipe(definition.id(),new ShapelessRecipe("",CraftingBookCategory.MISC,
          new ItemStack(ChaoGarden.item(ChaoVariants.itemId(definition.id()))),ingredient));
      for(var holder:ChaoGarden.CONTENT.values()) {
        if(!(holder.get() instanceof ChaoEggItem nativeEgg)||!nativeEgg.variant.equals("normal"))continue;
        var source=new ItemStack(holder.get());var before=source.copy();var input=CraftingInput.of(1,1,List.of(source));
        h.assertTrue(!recipe.matches(input,h.getLevel())&&recipe.assemble(input,h.getLevel().registryAccess()).isEmpty(),"External datapack recipe bypassed acquisition");
        h.assertTrue(ItemStack.matches(before,source),"Rejected conversion changed its native source");
      }
    }
    h.succeed();
  }

  @GameTest(template="empty",timeoutTicks=40)
  public static void previouslyPlacedAddonEggPausesAndRemainsRecoverable(GameTestHelper h) {
    var keeper=h.makeMockPlayer(GameType.SURVIVAL);var egg=ChaoGarden.CHAO_EGG.get().create(h.getLevel());
    var state=ChaoState.egg(4,true,false,3,new Random(23023));state.variant="create";
    egg.setState(state);egg.setIncubation(GardenConfig.HATCH_TICKS.get()-1);int before=egg.incubation();
    egg.owner=keeper.getUUID();var pos=h.absolutePos(new BlockPos(2,2,2));egg.moveTo(pos.getX()+.5,pos.getY(),pos.getZ()+.5);egg.setNoGravity(true);
    h.getLevel().addFreshEntity(egg);
    h.runAfterDelay(3,()->{
      h.assertTrue(!egg.isRemoved()&&egg.incubation()==before&&egg.state().variant.equals("create"),"Saved addon egg hatched or lost its pending state");
      var snapshot=egg.snapshotItem();h.assertTrue(snapshot.getItem() instanceof ChaoEggItem&&snapshot.get(DataComponents.CUSTOM_DATA).copyTag().getString("Chao").equals(state.encode()),"Recovery changed saved genes or heritage");
      h.assertTrue(egg.interact(keeper,InteractionHand.MAIN_HAND).consumesAction()&&egg.isRemoved(),"Paused egg was not recoverable");
      h.assertTrue(keeper.getInventory().contains(snapshot),"Recovery lost the individual egg");
      keeper.getInventory().clearContent();h.succeed();
    });
  }
}
