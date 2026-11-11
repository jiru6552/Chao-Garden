package com.chaogarden.test;

import com.chaogarden.ChaoGarden;
import com.chaogarden.core.ChaoState;
import com.chaogarden.entity.ChaoEntity;
import com.chaogarden.fruit.*;
import com.chaogarden.market.MarketStock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(ChaoGarden.MODID)
@PrefixGameTestTemplate(false)
public final class FruitConsolidationGameTests02510 {
  private static ResourceLocation id(String name){return ResourceLocation.fromNamespaceAndPath(ChaoGarden.MODID,name);}
  @GameTest(template="empty",timeoutTicks=40)
  public static void oldFruitStacksAndOrchardPalettesResolveWithoutDuplicateEntries(GameTestHelper h){
    for(String side:new String[]{"hero","dark"}){
      String old=side+"_fruit",current=side+"_garden_fruit";
      for(String suffix:new String[]{"","_seed"}){
        var expected=new ItemStack(ChaoGarden.item(current+suffix),17);
        expected.set(DataComponents.CUSTOM_NAME,Component.literal("Saved orchard harvest"));
        var payload=new CompoundTag();payload.putInt("Preserve",314159);
        expected.set(DataComponents.CUSTOM_DATA,CustomData.of(payload));
        var tag=(CompoundTag)expected.save(h.getLevel().registryAccess());tag.putString("id",id(old+suffix).toString());
        var restored=ItemStack.parseOptional(h.getLevel().registryAccess(),tag);
        h.assertTrue(ItemStack.matches(restored,expected),"Old stack lost count, components or canonical identity: "+old+suffix);
        h.assertTrue(BuiltInRegistries.ITEM.get(id(old+suffix))==expected.getItem(),"Old registry alias is unresolved");
        h.assertTrue(!BuiltInRegistries.ITEM.keySet().contains(id(old+suffix)),"Duplicate fruit remains a real registry entry");
        h.assertTrue(!MarketStock.catalog().containsKey(old+suffix),"Retired fruit/seed remains purchasable");
      }
      var states=java.util.List.of(
          FruitContent.podForFruit(current).defaultBlockState().setValue(HangingFruitBlock.AGE,2).setValue(HangingFruitBlock.FACING,Direction.WEST),
          FruitContent.treeForFruit(current).defaultBlockState().setValue(ChaoTreeCrownBlock.EXPANDED,true),
          FruitContent.saplingForFruit(current).defaultBlockState().setValue(ChaoSaplingBlock.STAGE,1));
      String[] suffixes={"_pod","_tree_crown","_seedling"};
      for(int i=0;i<states.size();i++){
        var tag=NbtUtils.writeBlockState(states.get(i));tag.putString("Name",id(old+suffixes[i]).toString());
        var restored=NbtUtils.readBlockState(h.getLevel().holderLookup(Registries.BLOCK),tag);
        h.assertTrue(restored.equals(states.get(i)),"Saved orchard lost ripeness, facing, expanded crown or growth stage");
        h.assertTrue(!BuiltInRegistries.BLOCK.keySet().contains(id(old+suffixes[i])),"Retired orchard has a duplicate registry entry");
      }
    }
    h.succeed();
  }
  @GameTest(template="empty",timeoutTicks=180)
  public static void gardenFruitChangesChildAffinityOnlyAfterOneRealMeal(GameTestHelper h){
    var keeper=h.makeMockPlayer(GameType.SURVIVAL);keeper.getAbilities().instabuild=false;
    var children=new java.util.ArrayList<ChaoEntity>();
    for(int i=0;i<2;i++){
      var at=new BlockPos(2+i*2,2,2);h.setBlock(at.below(),Blocks.STONE);
      var child=ChaoGarden.CHAO.get().create(h.getLevel());var state=new ChaoState();state.hunger=20;
      child.setState(state);child.owner=keeper.getUUID();child.setNoAi(true);child.setNoGravity(true);
      child.moveTo(net.minecraft.world.phys.Vec3.atBottomCenterOf(h.absolutePos(at)));h.getLevel().addFreshEntity(child);
      var stack=new ItemStack(ChaoGarden.item(i==0?"hero_garden_fruit":"dark_garden_fruit"),2);
      keeper.setItemInHand(InteractionHand.MAIN_HAND,stack);child.mobInteract(keeper,InteractionHand.MAIN_HAND);
      h.assertTrue(stack.getCount()==1&&child.isHandlingItem()&&child.state().align==0,"Offer granted an immediate effect or consumed multiple fruit");
      children.add(child);
    }
    h.runAtTickTime(20,()->children.forEach(c->h.assertTrue(c.state().fruitCount==0&&c.state().align==0,"Affinity changed while thinking")));
    h.runAtTickTime(150,()->{
      for(int i=0;i<children.size();i++){
        var c=children.get(i);float expected=i==0?.15f:-.15f;
        h.assertTrue(c.state().fruitCount==1&&Math.abs(c.state().align-expected)<.00001f&&c.getMainHandItem().isEmpty(),"Garden fruit meal failed to commit exactly one matching affinity shift");
        c.discard();
      }
      h.succeed();
    });
  }
}
