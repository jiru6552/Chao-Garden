package com.chaogarden.care;

import com.chaogarden.entity.ChaoEntity;
import com.mojang.serialization.MapCodec;
import java.util.Comparator;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public final class ChaoSchoolBlock extends Block {
  public enum Kind { CLASSROOM,DOCTOR }
  public static final DirectionProperty FACING=BlockStateProperties.HORIZONTAL_FACING;
  public final Kind kind;
  public ChaoSchoolBlock(Kind kind,Properties properties) {
    super(properties);this.kind=kind;registerDefaultState(stateDefinition.any().setValue(FACING,Direction.NORTH));
  }
  @Override protected MapCodec<? extends Block> codec() {
    return simpleCodec(properties->new ChaoSchoolBlock(kind,properties));
  }
  @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> builder) {builder.add(FACING);}
  @Override public BlockState getStateForPlacement(BlockPlaceContext c) {return defaultBlockState().setValue(FACING,c.getHorizontalDirection().getOpposite());}
  @Override protected BlockState rotate(BlockState s,Rotation r) {return s.setValue(FACING,r.rotate(s.getValue(FACING)));}
  @Override protected BlockState mirror(BlockState s,Mirror m) {return rotate(s,m.getRotation(s.getValue(FACING)));}
  @Override protected VoxelShape getShape(BlockState s,BlockGetter world,BlockPos pos,CollisionContext c) {
    VoxelShape shape=kind==Kind.DOCTOR ? Shapes.or(box(2,0,2,14,10,14),box(3,10,4,13,16,12))
        : Shapes.or(box(0,9,1,16,11,15),box(1,0,2,3,9,4),box(13,0,2,15,9,4),
            box(1,0,12,3,9,14),box(13,0,12,15,9,14),box(4,11,5,12,13,12));
    int turns=switch(s.getValue(FACING)){case EAST->1;case SOUTH->2;case WEST->3;default->0;};
    for(int i=0;i<turns;i++)shape=turn(shape);return shape;
  }
  private static VoxelShape turn(VoxelShape original) {
    VoxelShape result=Shapes.empty();for(var a:original.toAabbs())result=Shapes.or(result,
        box(16-a.maxZ*16,a.minY*16,a.minX*16,16-a.minZ*16,a.maxY*16,a.maxX*16));return result;
  }
  @Override protected InteractionResult useWithoutItem(BlockState state,Level world,BlockPos pos,
      Player player,BlockHitResult hit) {
    if(kind==Kind.CLASSROOM){
      if(!world.isClientSide)player.displayClientMessage(Component.literal("Lessons are taught at Kindergarten daycare."),true);
      return InteractionResult.sidedSuccess(world.isClientSide);
    }
    if(!world.isClientSide && player instanceof ServerPlayer keeper) {
      var nearest=world.getEntitiesOfClass(ChaoEntity.class,new AABB(pos).inflate(8),
          c->c.isAlive() && c.canCare(player) && c.distanceToSqr(player)<=64 && c.distanceToSqr(pos.getCenter())<=64).stream()
          .min(Comparator.comparingDouble(c->c.distanceToSqr(player)));
      if(nearest.isPresent())ChaoCareNetwork.open(keeper,nearest.get(),4,kind==Kind.CLASSROOM?1:0);
      else player.displayClientMessage(Component.literal("Bring one of your Chao within eight blocks, then use this station."),true);
    }
    return InteractionResult.sidedSuccess(world.isClientSide);
  }
}
