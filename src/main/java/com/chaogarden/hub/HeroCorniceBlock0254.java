package com.chaogarden.hub;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;

/** A solid marble cornice with native straight / mitred-corner soffit orientation. */
public final class HeroCorniceBlock0254 extends HorizontalDirectionalBlock {
  public static final BooleanProperty CORNER=BooleanProperty.create("corner");
  public static final MapCodec<HeroCorniceBlock0254> CODEC=simpleCodec(HeroCorniceBlock0254::new);
  public HeroCorniceBlock0254(Properties properties){super(properties);registerDefaultState(stateDefinition.any().setValue(FACING,Direction.NORTH).setValue(CORNER,false));}
  @Override protected MapCodec<? extends HorizontalDirectionalBlock> codec(){return CODEC;}
  @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> builder){builder.add(FACING,CORNER);}
  @Override public BlockState getStateForPlacement(BlockPlaceContext context){return defaultBlockState().setValue(FACING,context.getHorizontalDirection().getOpposite());}
  @Override protected BlockState rotate(BlockState state,Rotation rotation){return state.setValue(FACING,rotation.rotate(state.getValue(FACING)));}
  @Override protected BlockState mirror(BlockState state,Mirror mirror){
    Direction facing=state.getValue(FACING);
    // A corner's direction names its clockwise outer edge pair (N+W, E+N, S+E, W+S).
    Direction reflected=mirror.mirror(facing);
    if(state.getValue(CORNER)&&mirror!=Mirror.NONE)reflected=reflected.getClockWise();
    return state.setValue(FACING,reflected);
  }
}
