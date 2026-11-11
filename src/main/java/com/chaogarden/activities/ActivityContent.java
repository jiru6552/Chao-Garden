package com.chaogarden.activities;

import com.chaogarden.ChaoGarden;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Blocks and tools for physical gardens and player-built Chao competitions. */
public final class ActivityContent {
  public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(ChaoGarden.MODID);
  public static final DeferredBlock<ActivityBlock> GARDEN = block("garden_block", ActivityBlock.Kind.GARDEN);
  public static final DeferredBlock<ActivityBlock> START = block("race_start", ActivityBlock.Kind.START);
  public static final DeferredBlock<ActivityBlock> FINISH = block("race_finish", ActivityBlock.Kind.FINISH);
  public static final DeferredBlock<ActivityBlock> FLAG = block("race_marker", ActivityBlock.Kind.FLAG);
  public static final DeferredBlock<ActivityBlock> MAT = block("karate_mat", ActivityBlock.Kind.MAT);

  private static DeferredBlock<ActivityBlock> block(String name, ActivityBlock.Kind kind) {
    return BLOCKS.register(name, () -> {
      var properties = BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_LIGHT_BLUE)
          .strength(1.5f).noOcclusion();
      return kind == ActivityBlock.Kind.GARDEN
          ? new GardenTransporterBlock(properties.pushReaction(net.minecraft.world.level.material.PushReaction.BLOCK)) : kind == ActivityBlock.Kind.MAT ? new KarateMatBlock(properties.pushReaction(net.minecraft.world.level.material.PushReaction.BLOCK)) : new ActivityBlock(kind, properties);
    });
  }

  public static void bootstrap() {
    ChaoGarden.add("garden_block", () -> new BlockItem(GARDEN.get(), new Item.Properties()));
    ChaoGarden.add("race_start", () -> new BlockItem(START.get(), new Item.Properties()));
    ChaoGarden.add("race_finish", () -> new BlockItem(FINISH.get(), new Item.Properties()));
    ChaoGarden.add("race_marker", () -> new RaceMarkerItem(FLAG.get(), new Item.Properties()));
    ChaoGarden.add("karate_mat", () -> new BlockItem(MAT.get(), new Item.Properties()));
    ChaoGarden.ITEMS.addAlias(net.minecraft.resources.ResourceLocation.parse("chao_garden:garden_bell"),net.minecraft.resources.ResourceLocation.parse("chao_garden:garden_block"));
  }

  public static void register(IEventBus bus) {
    BLOCKS.register(bus);
    ActivityNetwork.register(bus);
  }

  private ActivityContent() {}
}
