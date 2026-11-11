package com.chaogarden.kindergarten;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Three reusable Ionic members, preserving the two established column BlockItems. */
public final class KindergartenIonicColumn0254 extends HorizontalDirectionalBlock {
  public enum Part implements StringRepresentable {
    BASE("base"), SHAFT("shaft"), CAPITAL("capital");
    private final String name;
    Part(String name){this.name=name;}
    @Override public String getSerializedName(){return name;}
  }
  public static final EnumProperty<Part> PART=EnumProperty.create("part",Part.class);
  public static final MapCodec<KindergartenIonicColumn0254> CODEC=simpleCodec(KindergartenIonicColumn0254::new);
  public KindergartenIonicColumn0254(Properties properties){super(properties);registerDefaultState(stateDefinition.any().setValue(FACING,Direction.SOUTH).setValue(PART,Part.SHAFT));}
  @Override protected MapCodec<? extends HorizontalDirectionalBlock> codec(){return CODEC;}
  @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> builder){builder.add(FACING,PART);}
  @Override public BlockState getStateForPlacement(BlockPlaceContext context){return defaultBlockState().setValue(FACING,context.getHorizontalDirection().getOpposite());}
  @Override protected BlockState rotate(BlockState state,Rotation rotation){return state.setValue(FACING,rotation.rotate(state.getValue(FACING)));}
  @Override protected BlockState mirror(BlockState state,Mirror mirror){return rotate(state,mirror.getRotation(state.getValue(FACING)));}
  @Override protected VoxelShape getShape(BlockState state,BlockGetter level,BlockPos pos,CollisionContext context){return IonicColumnMesh0254.shape(state.getValue(PART),state.getValue(FACING));}
  @Override protected VoxelShape getCollisionShape(BlockState state,BlockGetter level,BlockPos pos,CollisionContext context){return getShape(state,level,pos,context);}
}
