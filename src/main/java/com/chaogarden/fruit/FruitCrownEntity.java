package com.chaogarden.fruit;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
public final class FruitCrownEntity extends BlockEntity {
  public FruitCrownEntity(BlockPos pos,BlockState state){super(FruitContent.CROWN_ENTITY.get(),pos,state);}
}
