package com.chaogarden.test;

import com.chaogarden.ChaoGarden;
import com.chaogarden.distribution.ChestEggLootModifier;
import com.chaogarden.distribution.ChestFillWitness;
import com.chaogarden.item.ChaoEggItem;
import com.mojang.serialization.JsonOps;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import java.util.Optional;
import java.util.Random;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(ChaoGarden.MODID)
@PrefixGameTestTemplate(false)
public final class EggDistributionGameTests {
  private static final String GENERIC="chao_garden:distribution_test/chests/generic";
  private static ChestEggLootModifier forced(){return new ChestEggLootModifier(new LootItemCondition[0],1,1,1,1);}
  private static LootParams params(net.minecraft.server.level.ServerLevel level){
    return new LootParams.Builder(level).withParameter(LootContextParams.ORIGIN,Vec3.ZERO).create(LootContextParamSets.CHEST);
  }
  private static LootContext context(net.minecraft.server.level.ServerLevel level,String table){
    return new LootContext.Builder(params(level)).withOptionalRandomSeed(23023).withQueriedLootTableId(ResourceLocation.parse(table)).create(Optional.empty());
  }
  private static LootTable table(GameTestHelper h,String id){
    return h.getLevel().getServer().reloadableRegistries().getLootTable(ResourceKey.create(Registries.LOOT_TABLE,ResourceLocation.parse(id)));
  }
  private static boolean containsEgg(Container container){
    for(int i=0;i<container.getContainerSize();i++)if(container.getItem(i).getItem() instanceof ChaoEggItem)return true;
    return false;
  }

  @GameTest(template="empty",timeoutTicks=40)
  public static void chestEggCodecHandlesArbitraryChestIdsAndPreservesExistingLoot(GameTestHelper h){
    var encoded=ChestEggLootModifier.CODEC.codec().encodeStart(JsonOps.INSTANCE,forced()).getOrThrow();
    var modifier=ChestEggLootModifier.CODEC.codec().parse(JsonOps.INSTANCE,encoded).getOrThrow();
    for(String id:new String[]{"minecraft:chests/village/village_tannery","chao_garden:distribution_test/unconventional"}){
      var ctx=context(h.getLevel(),id);h.assertTrue(ChestEggLootModifier.isChest(ctx),"Declared chest not recognized: "+id);
      var loot=new ObjectArrayList<ItemStack>();var original=new ItemStack(Items.DIAMOND,3);loot.add(original);modifier.apply(loot,ctx);
      h.assertTrue(loot.size()==2&&loot.getFirst()==original&&original.getCount()==3,"Original loot altered or rare categories leaked");
      h.assertTrue(loot.get(1).getItem() instanceof ChaoEggItem egg&&!egg.shiny&&egg.jewel==0&&egg.variant.equals("normal"),"Wrong base category");
    }
    for(String id:new String[]{GENERIC,"chao_garden:distribution_test/fishing","minecraft:blocks/stone","minecraft:entities/zombie","minecraft:gameplay/fishing","chao_garden:bonus/chest_supplies"}){
      var ctx=context(h.getLevel(),id);h.assertTrue(!ChestEggLootModifier.isChest(ctx),"Unproven context accepted: "+id);
      var loot=new ObjectArrayList<ItemStack>();loot.add(new ItemStack(Items.STONE));modifier.apply(loot,ctx);
      h.assertTrue(loot.size()==1&&loot.getFirst().is(Items.STONE),"Nonchest loot altered");
    }
    h.succeed();
  }

  @GameTest(template="empty",timeoutTicks=40)
  public static void independentStructureAndDimensionCategories(GameTestHelper h){
    var modifier=forced();
    for(String id:new String[]{"minecraft:chests/buried_treasure","minecraft:chests/jungle_temple","minecraft:chests/trial_chambers/reward"}){
      var loot=new ObjectArrayList<ItemStack>();modifier.apply(loot,context(h.getLevel(),id));
      h.assertTrue(loot.size()==2&&((ChaoEggItem)loot.get(1).getItem()).shiny,"Shiny site missing or leaked a gem");
    }
    var ancient=new ObjectArrayList<ItemStack>();modifier.apply(ancient,context(h.getLevel(),"minecraft:chests/ancient_city"));
    h.assertTrue(ancient.size()==3&&((ChaoEggItem)ancient.get(1).getItem()).shiny&&((ChaoEggItem)ancient.get(2).getItem()).jewel>0,"Ancient city categories are not independent");
    for(var dimension:java.util.List.of(Level.NETHER,Level.END)){
      var level=h.getLevel().getServer().getLevel(dimension);h.assertTrue(level!=null,"Missing dimension");
      var loot=new ObjectArrayList<ItemStack>();modifier.apply(loot,context(level,"chao_garden:distribution_test/unconventional"));
      String theme=dimension==Level.NETHER?"nether":"end";
      h.assertTrue(loot.size()==3&&((ChaoEggItem)loot.get(1).getItem()).shiny&&loot.get(2).is(ChaoGarden.item(theme+"_chao_egg")),"Wrong dimension egg category");
    }
    h.succeed();
  }

  @GameTest(template="empty",timeoutTicks=40)
  public static void actualGenericChestAndMinecartFillQualifyButArbitraryContainerDoesNot(GameTestHelper h){
    var at=new BlockPos(2,2,2);h.setBlock(at,Blocks.CHEST);
    var chest=(Container)h.getLevel().getBlockEntity(h.absolutePos(at));
    var cart=EntityType.CHEST_MINECART.create(h.getLevel());h.assertTrue(cart!=null,"Minecart fixture missing");
    var loot=table(h,GENERIC);var random=new Random(230231);
    for(Container container:java.util.List.of(chest,(Container)cart)){
      boolean egg=false,stone=false;
      for(int sample=0;sample<4096&&!egg;sample++){
        container.clearContent();loot.fill(container,params(h.getLevel()),random.nextLong());
        egg=containsEgg(container);
        for(int i=0;i<container.getContainerSize();i++)stone|=container.getItem(i).is(Items.STONE);
      }
      h.assertTrue(egg&&stone,"Real generic chest/minecart fill missed its additive egg policy");container.clearContent();
    }
    var arbitrary=new SimpleContainer(27);
    for(int sample=0;sample<4096;sample++){
      arbitrary.clearContent();loot.fill(arbitrary,params(h.getLevel()),random.nextLong());
      h.assertTrue(!containsEgg(arbitrary),"Unproven arbitrary container obtained a chest egg");
    }
    cart.discard();h.succeed();
  }

  @GameTest(template="empty",timeoutTicks=40)
  public static void fillWitnessIsExactNestedAndExceptionSafe(GameTestHelper h){
    var loot=table(h,GENERIC);var ctx=context(h.getLevel(),GENERIC);var other=context(h.getLevel(),GENERIC);
    h.assertTrue(!ChestEggLootModifier.isChest(ctx),"Generic outside fill was accepted");
    try {
      ChestFillWitness.generate(loot,ctx,()->{
        h.assertTrue(ChestEggLootModifier.isChest(ctx)&&!ChestEggLootModifier.isChest(other),"Witness leaked to another context");
        ChestFillWitness.generate(loot,other,()->{
          h.assertTrue(ChestEggLootModifier.isChest(other)&&!ChestEggLootModifier.isChest(ctx),"Nested witness retained wrong root");return null;
        });
        h.assertTrue(ChestEggLootModifier.isChest(ctx),"Nested witness failed to restore parent");
        throw new IllegalStateException("intentional fixture");
      });
      h.fail("Exception fixture did not throw");
    }catch(IllegalStateException expected){h.assertTrue(expected.getMessage().equals("intentional fixture"),"Unexpected exception");}
    h.assertTrue(!ChestEggLootModifier.isChest(ctx)&&!ChestEggLootModifier.isChest(other),"Fill witness survived an exception");h.succeed();
  }
}
