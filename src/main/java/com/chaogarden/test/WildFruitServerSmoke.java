package com.chaogarden.test;

import com.chaogarden.ChaoGarden;
import com.chaogarden.fruit.*;
import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.server.ServerStartedEvent;

/** Opt-in dedicated server check. Use a disposable normal-generation world. Excluded from the mod jar. */
@EventBusSubscriber(modid = ChaoGarden.MODID)
public final class WildFruitServerSmoke {
  private static void check(boolean value, String message) {
    if (!value) throw new AssertionError(message);
  }
  @SubscribeEvent public static void started(ServerStartedEvent event) {
    String mode = System.getProperty("chao.wildFruitSmoke");
    if (mode == null) return;
    var server = event.getServer();
    server.execute(() -> {
      try {
        var aether = ResourceKey.create(Registries.DIMENSION, ResourceLocation.parse("aether:the_aether"));
        boolean installed = mode.equals("aether");
        check(ModList.get().isLoaded("aether") == installed, "Aether install state");
        check((server.getLevel(aether) != null) == installed, "Aether level registration");
        if (!installed) check(WildFruit.forDimension(aether) == null, "Aether absent gate");
        for (var level : server.getAllLevels()) {
          var id = level.dimension();
          String expected = Level.OVERWORLD.equals(id) ? "garden_fruit" : Level.NETHER.equals(id) ? "dark_garden_fruit"
              : aether.equals(id) ? "hero_garden_fruit" : null;
          check(Objects.equals(expected, WildFruit.forDimension(id)), "Rule for " + id);
          loot(level, expected);
          if (expected != null) terrain(level, expected);
          // Drive the configured feature even in excluded dimensions with otherwise suitable soil.
          var base = new BlockPos(4104, 88, 4104);
          for (var at : BlockPos.betweenClosed(base.offset(-2,0,-2),base.offset(2,7,2))) level.setBlock(at, Blocks.AIR.defaultBlockState(), 2);
          level.setBlock(base.below(), (Level.NETHER.equals(id) ? Blocks.NETHERRACK : Blocks.DIRT).defaultBlockState(), 2);
          for (var fruit : FruitContent.FRUITS) if (!fruit.equals(expected)) {
            check(!FruitContent.growNaturalTree(level, base, fruit), id + " accepted " + fruit);
          }
          var feature = level.registryAccess().registryOrThrow(Registries.CONFIGURED_FEATURE).get(ResourceLocation.parse("chao_garden:wild_fruit_trees"));
          check(feature.place(level, level.getChunkSource().getGenerator(), level.random, base) == (expected != null), "Feature " + id);
          if (expected != null) verifyPods(level, FruitContent.crownPosition(base), expected);
          else check(level.isEmptyBlock(base), "Rejected feature wrote a tree");
          if (Level.OVERWORLD.equals(id)) {
            var other = base.east(12);
            for (var at : BlockPos.betweenClosed(other.offset(-2,0,-2),other.offset(2,7,2))) level.setBlock(at, Blocks.AIR.defaultBlockState(), 2);
            level.setBlock(other.below(), Blocks.DIRT.defaultBlockState(), 2);
            for (var fruit : List.of("strong_fruit","tasty_fruit")) {
              var sapling = FruitContent.saplingForFruit(fruit);
              level.setBlock(other, sapling.defaultBlockState(), 3);
              check(sapling.advance(level,other,level.getBlockState(other)) && sapling.advance(level,other,level.getBlockState(other)), "Planted " + fruit);
              check(level.getBlockState(FruitContent.crownPosition(other)).is(FruitContent.treeForFruit(fruit)), "Planted crown");
              for (var at : BlockPos.betweenClosed(other.offset(-2,0,-2),other.offset(2,7,2))) level.setBlock(at, Blocks.AIR.defaultBlockState(), 2);
            }
          }
        }
        System.out.println("FRUIT38 PASS " + mode);
      } catch (Throwable error) {
        error.printStackTrace(); System.out.println("FRUIT38 FAIL " + mode + " " + error);
      } finally { server.halt(false); }
    });
  }
  private static void verifyPods(ServerLevel level, BlockPos crown, String expected) {
    check(level.getBlockState(crown).is(FruitContent.treeForFruit(expected)), "Wrong crown");
    for (var at : FruitContent.podPositions(level,crown,expected)) {
      var pod = level.getBlockState(at);
      check(pod.is(FruitContent.podForFruit(expected)) && pod.getValue(HangingFruitBlock.AGE) == 3, "Wrong or unripe pod " + at);
    }
  }
  private static void terrain(ServerLevel level, String expected) {
    int trees = 0, chunks = 0;
    for (int z=0; z<24 && trees<3; z++) for (int x=0; x<24 && trees<3; x++) {
      var chunk = level.getChunk(25+x, 25+z); chunks++;
      for (var at : Set.copyOf(chunk.getBlockEntitiesPos()))
        if (level.getBlockState(at).getBlock() instanceof ChaoTreeCrownBlock crown) {
          check(expected.equals(crown.fruit), "Fresh terrain spawned " + crown.fruit);
          verifyPods(level, at, expected);
          if (level.dimension().equals(Level.NETHER)) check(at.getY() < level.getSeaLevel()+96, "Nether roof");
          trees++;
        }
    }
    System.out.println("FRUIT38 natural " + level.dimension().location() + " " + expected + " trees=" + trees + " chunks=" + chunks);
    check(trees >= 3, "No regional wild trees in actual new terrain");
  }
  private static void loot(ServerLevel level, String expected) {
    var params = new LootParams.Builder(level).withParameter(LootContextParams.ORIGIN, Vec3.ZERO).create(LootContextParamSets.CHEST);
    for (String name : List.of("enemy_supplies", "chest_supplies")) {
      int hits = 0, maxCount = 0, drives = 0;
      var key = ResourceKey.create(Registries.LOOT_TABLE, ResourceLocation.parse("chao_garden:bonus/" + name));
      var table = level.getServer().reloadableRegistries().getLootTable(key);
      var seeds = new Random(38017L);
      for (int i=0; i<1024; i++) for (var stack : table.getRandomItems(params, seeds.nextLong())) {
        var item = BuiltInRegistries.ITEM.getKey(stack.getItem());
        if (item.getPath().endsWith("_drive")) { drives++; continue; }
        check(item.getNamespace().equals("chao_garden") && item.getPath().equals(expected), "Wrong supply " + item + " in " + level.dimension());
        hits++; maxCount = Math.max(maxCount,stack.getCount());
      }
      check((hits>0) == (expected!=null), "Regional fruit roll " + name);
      if (expected!=null) check(maxCount==(name.equals("chest_supplies")?3:1), "Fruit stack count");
      if (name.equals("chest_supplies")) check(drives>0, "Drive pool lost");
      System.out.println("FRUIT38 loot " + level.dimension().location() + " " + name + " fruit rolls="+hits+" max="+maxCount);
    }
  }
}
