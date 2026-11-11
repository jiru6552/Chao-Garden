package com.chaogarden.fruit;

import com.chaogarden.hub.DarkGardenDecor0255;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/** Same bare wood geometry and physical joints as the authored Dark Garden trees. */
public final class DarkFruitTrunkBlock extends DarkGardenDecor0255.Deadwood {
  public static final MapCodec<DarkFruitTrunkBlock> CODEC=simpleCodec(DarkFruitTrunkBlock::new);
  public DarkFruitTrunkBlock(Properties properties){super(properties);}
  @Override public MapCodec<? extends RotatedPillarBlock> codec(){return CODEC;}
  @Override protected InteractionResult useWithoutItem(BlockState s,Level l,BlockPos p,Player player,BlockHitResult hit){
    return ChaoFruitTrunkBlock.harvestAbove(l,p);
  }
}
