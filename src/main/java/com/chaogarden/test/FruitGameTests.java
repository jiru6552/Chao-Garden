package com.chaogarden.test;

import com.chaogarden.ChaoGarden;
import com.chaogarden.fruit.ChaoTreeCrownBlock;
import com.chaogarden.fruit.FruitContent;
import com.chaogarden.fruit.HangingFruitBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(ChaoGarden.MODID)
@PrefixGameTestTemplate(false)
public final class FruitGameTests {
  private static final BlockPos BASE = new BlockPos(3, 2, 3);

  private static void clear(GameTestHelper helper) {
    for (int x = -3; x <= 3; x++) for (int z = -3; z <= 3; z++)
      for (int y = 0; y <= 7; y++) helper.setBlock(BASE.offset(x,y,z), Blocks.AIR);
    helper.setBlock(BASE.below(), Blocks.DIRT);
  }

  private static int fruitDrops(GameTestHelper helper, BlockPos base, String fruit) {
    return helper.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(base).inflate(4),
        e -> e.getItem().is(ChaoGarden.item(fruit))).stream().mapToInt(e -> e.getItem().getCount()).sum();
  }

  @GameTest(template="empty", timeoutTicks=40)
  public static void wildFruitFeatureOnlyIntroducesNaturalCrops(GameTestHelper helper) {
    var level = helper.getLevel(); var base = helper.absolutePos(BASE);
    clear(helper);
    for (String fruit : FruitContent.FRUITS) {
      if (fruit.equals("garden_fruit")) continue;
      helper.assertTrue(!FruitContent.growNaturalTree(level, base, fruit), "Overworld accepted " + fruit);
      helper.assertTrue(level.getBlockState(base).isAir(), "Rejected fruit modified the world");
    }
    var key = net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(ChaoGarden.MODID, "wild_fruit_trees");
    var configured = level.registryAccess().registryOrThrow(Registries.CONFIGURED_FEATURE).get(key);
    helper.assertTrue(configured != null && configured.place(level, level.getChunkSource().getGenerator(), level.random, base),
        "Overworld tree failed on clear dirt");
    var crown = FruitContent.crownPosition(base);
    helper.assertTrue(level.getBlockState(crown).is(FruitContent.treeForFruit("garden_fruit")), "Wrong Overworld fruit");
    for (var at : FruitContent.podPositions(level, crown, "garden_fruit"))
      helper.assertTrue(level.getBlockState(at).getValue(HangingFruitBlock.AGE) == 3, "Wild pod must be ripe");
    helper.succeed();
  }

  @GameTest(template="empty", timeoutTicks=40)
  public static void obtainedFruitCraftsIntoItsMatchingSeed(GameTestHelper helper) {
    var level = helper.getLevel();
    for (String fruit : com.chaogarden.fruit.FruitCatalog.ACTIVE) {
      var input = net.minecraft.world.item.crafting.CraftingInput.of(1, 1,
          java.util.List.of(new ItemStack(ChaoGarden.item(fruit))));
      var recipe = level.getRecipeManager().getRecipeFor(net.minecraft.world.item.crafting.RecipeType.CRAFTING, input, level);
      if (com.chaogarden.fruit.FruitCatalog.isMushroom(fruit)) {
        helper.assertTrue(recipe.isEmpty(), "Mushrooms must not craft into seeds");
        continue;
      }
      helper.assertTrue(recipe.isPresent(), "Obtained fruit has no seed recipe: " + fruit);
      var result = recipe.get().value().assemble(input, level.registryAccess());
      helper.assertTrue(result.is(ChaoGarden.item(FruitContent.seedId(fruit))) && result.getCount()==1,
          "Fruit did not yield its matching single seed: " + fruit);
    }
    helper.succeed();
  }

  @GameTest(template="empty", timeoutTicks=40)
  public static void fruitTreesGrowHarvestAndRegrow(GameTestHelper helper) {
    var level = helper.getLevel();
    var base = helper.absolutePos(BASE);
    for (String fruit : FruitContent.FRUITS) {
      clear(helper);
      var sapling = FruitContent.saplingForFruit(fruit);
      level.setBlock(base, sapling.defaultBlockState(), 3);
      helper.assertTrue(sapling.advance(level, base, level.getBlockState(base)), "Seedling did not advance");
      helper.assertTrue(sapling.advance(level, base, level.getBlockState(base)), "Unobstructed tree did not grow: "+fruit);
      for (int y=0;y<4;y++) helper.assertTrue(level.getBlockState(base.above(y)).is(FruitContent.trunkForFruit(fruit)), "Missing full-height trunk");
      var crown=FruitContent.crownPosition(base);
      helper.assertTrue(level.getBlockState(crown).is(FruitContent.treeForFruit(fruit))
          && level.getBlockState(crown).getValue(ChaoTreeCrownBlock.EXPANDED), "Wrong expanded orchard anchor");
      helper.assertTrue(level.getBlockState(crown).getValue(ChaoTreeCrownBlock.GARDEN) && level.getBlockEntity(crown) instanceof com.chaogarden.fruit.FruitCrownEntity, "Garden palm mesh anchor missing");
      for (Direction side : Direction.Plane.HORIZONTAL) {
        var leaf=crown.relative(side);
        helper.assertTrue(level.getBlockState(leaf).is(FruitContent.isDark(fruit) ? FruitContent.DARK_TRUNK.get() : FruitContent.PALM_SUPPORT.get()), "Garden palm needs real fruit support beneath its shared frond mesh");
        var at=leaf.below();
        helper.assertTrue(level.getBlockState(at).is(FruitContent.podForFruit(fruit)), "Missing fruit hanging beneath foliage");
        helper.assertTrue(level.getBlockState(at).getValue(HangingFruitBlock.FACING)==side.getOpposite(), "Pod stem faces away from tree");
        var pod=FruitContent.podForFruit(fruit);
        helper.assertTrue(!pod.harvest(level,at,level.getBlockState(at)), "Unripe pod produced fruit");
        for(int i=0;i<3;i++) pod.performBonemeal(level,level.random,at,level.getBlockState(at));
        var mature=level.getBlockState(at);
        var encoded=NbtUtils.writeBlockState(mature);
        var decoded=NbtUtils.readBlockState(level.holderLookup(Registries.BLOCK),encoded);
        helper.assertTrue(decoded.equals(mature), "Ripeness or fruit orientation did not persist");
        int before=fruitDrops(helper,base,fruit);
        helper.assertTrue(pod.harvest(level,at,mature), "Ripe pod refused harvest");
        helper.assertTrue(level.getBlockState(at).getValue(HangingFruitBlock.AGE)==0, "Harvest did not reset ripening");
        helper.assertTrue(fruitDrops(helper,base,fruit)==before+1,"Harvest did not produce exactly one matching fruit");
        helper.assertTrue(!pod.harvest(level,at,level.getBlockState(at)), "Repeated click duplicated fruit");
      }
      var lost=crown.below().north();
      level.removeBlock(lost,false);
      FruitContent.replenishPods(level,crown,fruit);
      helper.assertTrue(level.getBlockState(lost).is(FruitContent.podForFruit(fruit)),"Supported canopy did not replace missing fruit stem");
      // A fruit must not float or grow again after its actual foliage is removed.
      level.removeBlock(lost.above(),false);
      helper.assertTrue(!level.getBlockState(lost).is(FruitContent.podForFruit(fruit)),"Fruit floated after its supporting leaves were removed");
      FruitContent.replenishPods(level,crown,fruit);
      helper.assertTrue(level.getBlockState(lost).isAir(),"Fruit regenerated without supporting foliage");
      level.removeBlock(crown,false);
      for (var at:FruitContent.podPositions(crown,false))
        helper.assertTrue(!level.getBlockState(at).is(FruitContent.podForFruit(fruit)),"Fruit floated after orchard anchor was removed");
    }
    helper.succeed();
  }

  @GameTest(template="empty", timeoutTicks=40)
  public static void fruitTreeGrowthProtectsExistingBlocks(GameTestHelper helper) {
    clear(helper);
    var level=helper.getLevel();var base=helper.absolutePos(BASE);
    var sapling=FruitContent.saplingForFruit("garden_fruit");
    level.setBlock(base,sapling.defaultBlockState(),3);
    sapling.advance(level,base,level.getBlockState(base));
    BlockPos obstruction=base.above(5).east(2);
    level.setBlock(obstruction,Blocks.OBSIDIAN.defaultBlockState(),3);
    helper.assertTrue(!sapling.advance(level,base,level.getBlockState(base)),"Growth ignored the wide, tall canopy's obstruction");
    helper.assertTrue(level.getBlockState(base).is(sapling),"Failed growth consumed seedling");
    helper.assertTrue(level.getBlockState(obstruction).is(Blocks.OBSIDIAN),"Growth overwrote a neighboring build");
    helper.succeed();
  }

  @GameTest(template="empty", timeoutTicks=40)
  public static void legacyOrchardsUpgradeWithoutDroppingOrDuplicatingTheirCrop(GameTestHelper helper) {
    clear(helper);
    var level=helper.getLevel();var base=helper.absolutePos(BASE);String fruit="garden_fruit";
    for(int y=0;y<3;y++) level.setBlock(base.above(y),FruitContent.TRUNK.get().defaultBlockState(),3);
    BlockPos oldCrown=base.above(3);
    var tree=FruitContent.treeForFruit(fruit);
    level.setBlock(oldCrown,tree.defaultBlockState(),3);
    FruitContent.replenishPods(level,oldCrown,fruit);
    for(Direction side:Direction.Plane.HORIZONTAL) {
      BlockPos pod=oldCrown.below().relative(side);
      helper.assertTrue(level.getBlockState(pod).is(FruitContent.podForFruit(fruit)),"Saved small palm lost its original fruit positions");
      level.setBlock(pod,level.getBlockState(pod).setValue(HangingFruitBlock.AGE,side==Direction.NORTH?3:1),3);
    }
    BlockPos obstruction=base.above(5).east(2);
    level.setBlock(obstruction,Blocks.OBSIDIAN.defaultBlockState(),3);
    helper.assertTrue(!FruitContent.expandLegacyTree(level,oldCrown,fruit),"Legacy upgrade overwrote a build");
    helper.assertTrue(level.getBlockState(oldCrown).is(tree)
        && !level.getBlockState(oldCrown).getValue(ChaoTreeCrownBlock.EXPANDED),"Blocked upgrade consumed the old crown");
    helper.assertTrue(level.getBlockState(oldCrown.below().north()).getValue(HangingFruitBlock.AGE)==3,"Blocked upgrade lost crop ripeness");
    level.removeBlock(obstruction,false);
    int before=fruitDrops(helper,base,fruit);
    tree.performBonemeal(level,level.random,oldCrown,level.getBlockState(oldCrown));
    var crown=FruitContent.crownPosition(base);
    helper.assertTrue(level.getBlockState(crown).is(tree)
        && level.getBlockState(crown).getValue(ChaoTreeCrownBlock.EXPANDED),"Bonemeal did not upgrade the old palm");
    for(Direction side:Direction.Plane.HORIZONTAL) {
      BlockPos pod=crown.below().relative(side);
      helper.assertTrue(level.getBlockState(pod).is(FruitContent.podForFruit(fruit)),"Upgrade lost a fruit stem");
      helper.assertTrue(level.getBlockState(pod).getValue(HangingFruitBlock.AGE)==(side==Direction.NORTH?3:2),"Upgrade failed to preserve and advance crop ripeness");
      helper.assertTrue(level.getBlockState(oldCrown.below().relative(side)).isAir(),"Upgrade left a duplicate old fruit stem");
    }
    helper.assertTrue(fruitDrops(helper,base,fruit)==before,"Upgrading copied a ripe crop and also dropped the same fruit");
    helper.succeed();
  }

  @GameTest(template="empty", timeoutTicks=80)
  public static void orchardLeavesUseVanillaSupportShearsAndDecay(GameTestHelper helper) {
    clear(helper);
    var level=helper.getLevel();var base=helper.absolutePos(BASE);
    helper.assertTrue(FruitContent.growTree(level,base,"garden_fruit"),"Could not grow foliage fixture");
    var crown=FruitContent.crownPosition(base);var leaf=crown.north(2);
    level.setBlock(crown.north(),FruitContent.TRUNK.get().defaultBlockState(),3);
    // Existing/player-placed legacy leaf blocks retain vanilla mechanics even though new palms use the garden mesh.
    level.setBlock(leaf,FruitContent.LEAVES.get().defaultBlockState().setValue(LeavesBlock.DISTANCE,1),3);
    var supported=level.getBlockState(leaf);
    helper.assertTrue(supported.getBlock() instanceof LeavesBlock && supported.is(BlockTags.LEAVES),"Canopy is not genuine tagged Minecraft foliage");
    helper.assertTrue(level.getBlockState(crown).is(BlockTags.LOGS)
        && level.getBlockState(base).is(BlockTags.LOGS),"Custom orchard wood cannot support leaf distances");
    var sheared=Block.getDrops(supported,level,leaf,null,null,new ItemStack(Items.SHEARS));
    helper.assertTrue(sheared.stream().anyMatch(stack->stack.is(FruitContent.LEAVES.get().asItem())&&stack.getCount()==1),"Shears did not recover the actual leaf block");
    BlockPos persistent=crown.above(2).east(2);
    var placed=FruitContent.LEAVES.get().defaultBlockState().setValue(LeavesBlock.PERSISTENT,true);
    level.setBlock(persistent,placed,3);
    placed.randomTick(level,persistent,level.random);
    helper.assertTrue(level.getBlockState(persistent).is(FruitContent.LEAVES.get()),"Persistent player foliage decayed");
    for(int y=0;y<4;y++)level.removeBlock(base.above(y),false);
    level.removeBlock(crown,false);
    for(Direction side:Direction.Plane.HORIZONTAL)level.removeBlock(crown.relative(side),false);
    helper.startSequence().thenIdle(12).thenExecute(()->{
      var unsupported=level.getBlockState(leaf);
      helper.assertTrue(unsupported.is(FruitContent.LEAVES.get())
          && unsupported.getValue(LeavesBlock.DISTANCE)==7,"Felled orchard foliage retained a false log connection");
      unsupported.randomTick(level,leaf,level.random);
      helper.assertTrue(level.getBlockState(leaf).isAir(),"Unsupported orchard foliage did not decay");
      helper.assertTrue(level.getBlockState(persistent).is(FruitContent.LEAVES.get()),"Player-placed persistent leaves did not survive felling");
    }).thenSucceed();
  }
}
