package com.chaogarden.hub;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Slim native lavender column; model and collision use the same finite members. */
public final class HeroColumn0259 extends Block {
  public enum Part implements StringRepresentable {
    BASE, SHAFT, CAPITAL;
    @Override public String getSerializedName(){return name().toLowerCase(java.util.Locale.ROOT);}
  }
  public static final EnumProperty<Part> PART=EnumProperty.create("part",Part.class);
  public static final MapCodec<HeroColumn0259> CODEC=simpleCodec(HeroColumn0259::new);
  private static final VoxelShape SHAFT=Shapes.or(Block.box(5,0,4,11,16,12),Block.box(4,0,5,12,16,11));
  private static final VoxelShape BASE=Shapes.or(SHAFT,Block.box(2,0,2,14,3,14),Block.box(3,3,3,13,5,13));
  private static final VoxelShape CAPITAL=Shapes.or(SHAFT,Block.box(3,11,3,13,13,13),Block.box(2,13,2,14,16,14));
  public HeroColumn0259(Properties properties){super(properties);registerDefaultState(stateDefinition.any().setValue(PART,Part.SHAFT));}
  @Override protected MapCodec<? extends Block> codec(){return CODEC;}
  @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> builder){builder.add(PART);}
  @Override protected VoxelShape getShape(BlockState state,BlockGetter level,BlockPos at,CollisionContext context){
    return switch(state.getValue(PART)){case BASE->BASE;case SHAFT->SHAFT;case CAPITAL->CAPITAL;};
  }
}
