package com.chaogarden.hub;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/** A walk-through two-block tube: the middle is genuinely open to a player. */
public final class ChaoTeleporterBlock extends Block {
  public static final MapCodec<ChaoTeleporterBlock> CODEC=simpleCodec(ChaoTeleporterBlock::new);
  public static final EnumProperty<DoubleBlockHalf> HALF=BlockStateProperties.DOUBLE_BLOCK_HALF;
  private static final VoxelShape POSTS=Shapes.or(box(0,0,0,2,16,2),box(14,0,0,16,16,2),
      box(0,0,14,2,16,16),box(14,0,14,16,16,16));
  private static final VoxelShape LOWER=Shapes.or(POSTS,box(0,0,0,16,1,16));
  private static final VoxelShape UPPER=Shapes.or(POSTS,box(0,15,0,16,16,16));
  public ChaoTeleporterBlock(BlockBehaviour.Properties properties) {
    super(properties);registerDefaultState(stateDefinition.any().setValue(HALF,DoubleBlockHalf.LOWER));
  }
  @Override protected MapCodec<? extends Block> codec(){return CODEC;}
  @Override protected VoxelShape getShape(BlockState state,net.minecraft.world.level.BlockGetter level,BlockPos pos,CollisionContext context){
    return state.getValue(HALF)==DoubleBlockHalf.LOWER?LOWER:UPPER;
  }
  @Override protected VoxelShape getCollisionShape(BlockState state,net.minecraft.world.level.BlockGetter level,BlockPos pos,CollisionContext context){
    return state.getValue(HALF)==DoubleBlockHalf.LOWER?LOWER:UPPER;
  }
  @Override public BlockState getStateForPlacement(BlockPlaceContext context) {
    var pos=context.getClickedPos();var level=context.getLevel();
    return pos.getY()<level.getMaxBuildHeight()-1&&level.getBlockState(pos.above()).canBeReplaced(context)
        &&level.getBlockState(pos.below()).isFaceSturdy(level,pos.below(),Direction.UP)?defaultBlockState():null;
  }
  @Override public void setPlacedBy(Level level,BlockPos pos,BlockState state,LivingEntity placer,ItemStack stack){
    level.setBlock(pos.above(),defaultBlockState().setValue(HALF,DoubleBlockHalf.UPPER),3);
  }
  @Override protected boolean canSurvive(BlockState state,LevelReader level,BlockPos pos){
    if(state.getValue(HALF)==DoubleBlockHalf.UPPER){var below=level.getBlockState(pos.below());return below.is(this)&&below.getValue(HALF)==DoubleBlockHalf.LOWER;}
    return level.getBlockState(pos.below()).isFaceSturdy(level,pos.below(),Direction.UP);
  }
  @Override protected BlockState updateShape(BlockState state,Direction side,BlockState neighbor,LevelAccessor level,BlockPos pos,BlockPos neighborPos){
    var half=state.getValue(HALF);
    if(side==(half==DoubleBlockHalf.LOWER?Direction.UP:Direction.DOWN)
        &&(!neighbor.is(this)||neighbor.getValue(HALF)==half))return Blocks.AIR.defaultBlockState();
    if(half==DoubleBlockHalf.LOWER&&side==Direction.DOWN&&!canSurvive(state,level,pos))return Blocks.AIR.defaultBlockState();
    return super.updateShape(state,side,neighbor,level,pos,neighborPos);
  }
  @Override public BlockState playerWillDestroy(Level level,BlockPos pos,BlockState state,Player player){
    if(!level.isClientSide&&state.getValue(HALF)==DoubleBlockHalf.UPPER){
      var base=pos.below();var lower=level.getBlockState(base);
      if(lower.is(this)&&lower.getValue(HALF)==DoubleBlockHalf.LOWER){
        if(!player.isCreative())dropResources(lower,level,base,null,player,player.getMainHandItem());
        level.setBlock(base,Blocks.AIR.defaultBlockState(),35);
      }
    }
    return super.playerWillDestroy(level,pos,state,player);
  }
  @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> builder){builder.add(HALF);}
}
