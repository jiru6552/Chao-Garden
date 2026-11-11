package com.chaogarden.toys;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.*;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Neutral, non-colliding save marker for already placed coffins. This is not a
 * ChaoToyBlock and cannot be selected by the toy goal. Keeping its old state
 * names avoids missing-palette entries; no chunks or inventories are rewritten.
 */
public final class RetiredCoffinBlock extends Block implements SimpleWaterloggedBlock {
  public static final DirectionProperty FACING=BlockStateProperties.HORIZONTAL_FACING;
  public static final BooleanProperty PLAYING=BooleanProperty.create("playing");
  public static final IntegerProperty FRAME=IntegerProperty.create("frame",0,3);
  public static final BooleanProperty WATERLOGGED=BlockStateProperties.WATERLOGGED;
  public RetiredCoffinBlock(){this(BlockBehaviour.Properties.of().mapColor(MapColor.STONE)
      .strength(.1f).sound(SoundType.STONE).noCollission().noOcclusion().noLootTable());}
  private RetiredCoffinBlock(BlockBehaviour.Properties properties){
    super(properties);
    registerDefaultState(stateDefinition.any().setValue(FACING,Direction.NORTH)
        .setValue(PLAYING,false).setValue(FRAME,0).setValue(WATERLOGGED,false));
  }
  @Override protected MapCodec<? extends Block> codec(){return simpleCodec(RetiredCoffinBlock::new);}
  @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> builder){
    builder.add(FACING,PLAYING,FRAME,WATERLOGGED);
  }
  @Override protected VoxelShape getShape(BlockState state,BlockGetter level,BlockPos pos,CollisionContext context){
    return box(4,0,4,12,1,12);
  }
  @Override protected FluidState getFluidState(BlockState state){
    return state.getValue(WATERLOGGED)?Fluids.WATER.getSource(false):super.getFluidState(state);
  }
  @Override protected BlockState updateShape(BlockState state,Direction side,BlockState neighbor,
      LevelAccessor level,BlockPos pos,BlockPos neighborPos){
    if(state.getValue(WATERLOGGED))level.scheduleTick(pos,Fluids.WATER,Fluids.WATER.getTickDelay(level));
    return super.updateShape(state,side,neighbor,level,pos,neighborPos);
  }
}
