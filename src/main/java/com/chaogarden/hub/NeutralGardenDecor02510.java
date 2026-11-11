package com.chaogarden.hub;

import com.chaogarden.ChaoGarden;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Internal neutral-garden scenery, with ordinary native models and collision. */
public final class NeutralGardenDecor02510 {
  public static final DeferredRegister.Blocks BLOCKS=DeferredRegister.createBlocks(ChaoGarden.MODID);
  public static final DeferredBlock<Block> CASCADE_ROCK=BLOCKS.register("neutral_cascade_rock",()->new Block(
      BlockBehaviour.Properties.of().mapColor(MapColor.TERRACOTTA_WHITE).strength(-1,3600000).sound(SoundType.STONE).noLootTable()));
  public static final DeferredBlock<Block> BROADLEAF=BLOCKS.register("neutral_broadleaf",()->new Block(
      BlockBehaviour.Properties.of().mapColor(MapColor.PLANT).strength(-1,3600000).sound(SoundType.GRASS).noLootTable().noCollission().noOcclusion()));
  private NeutralGardenDecor02510(){}
  public static void register(IEventBus bus){BLOCKS.register(bus);}
}
