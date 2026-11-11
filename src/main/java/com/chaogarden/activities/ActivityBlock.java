package com.chaogarden.activities;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public class ActivityBlock extends Block {
  public enum Kind { GARDEN, START, FINISH, FLAG, MAT }
  public static final EnumProperty<Direction.Axis> AXIS = BlockStateProperties.HORIZONTAL_AXIS;
  public static final BooleanProperty OPEN = BlockStateProperties.OPEN;
  private static final VoxelShape FLAG_X = flagShape(Direction.Axis.X);
  private static final VoxelShape FLAG_Z = flagShape(Direction.Axis.Z);
  public final Kind kind;

  private static VoxelShape flagShape(Direction.Axis axis) {
    // Match the pole and stepped pennant in race_marker.json. The outline
    // follows its placed rotation; flags deliberately remain non-colliding.
    VoxelShape shape = axis == Direction.Axis.X
        ? box(4, 0, 7, 6, 16, 9) : box(7, 0, 4, 9, 16, 6);
    double[] ends = {7, 10, 13, 16, 13, 10, 7};
    for (int row = 0; row < ends.length; row++) {
      shape = Shapes.or(shape, axis == Direction.Axis.X
          ? box(5.75, 9+row, 7.625, ends[row], 10+row, 8.375)
          : box(7.625, 9+row, 5.75, 8.375, 10+row, ends[row]));
    }
    return shape.optimize();
  }

  public ActivityBlock(Kind kind, Properties properties) {
    super(properties);
    this.kind = kind;
    registerDefaultState(stateDefinition.any().setValue(AXIS, Direction.Axis.X).setValue(OPEN,false));
  }

  @Override protected MapCodec<? extends Block> codec() {
    return simpleCodec(properties -> new ActivityBlock(kind, properties));
  }
  @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
    builder.add(AXIS,OPEN);
  }
  @Override public BlockState getStateForPlacement(BlockPlaceContext context) {
    return defaultBlockState().setValue(AXIS, context.getHorizontalDirection().getClockWise().getAxis());
  }
  @Override protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
    return switch (kind) {
      case MAT -> box(0, 0, 0, 16, 1, 16);
      case FLAG -> state.getValue(AXIS) == Direction.Axis.X ? FLAG_X : FLAG_Z;
      case GARDEN -> Shapes.or(box(2, 0, 2, 14, 9, 14), box(6, 9, 6, 10, 16, 10));
      default -> state.getValue(AXIS) == Direction.Axis.X
          ? Shapes.or(box(6, 0, 6, 10, 16, 10), box(0, 5, 7, 16, 8, 9), box(0, 12, 7, 16, 15, 9))
          : Shapes.or(box(6, 0, 6, 10, 16, 10), box(7, 5, 0, 9, 8, 16), box(7, 12, 0, 9, 15, 16));
    };
  }
  @Override protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
    return kind == Kind.FLAG || ((kind == Kind.START || kind == Kind.FINISH) && state.getValue(OPEN))
        ? Shapes.empty() : getShape(state, level, pos, context);
  }
  @Override protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
                                                       Player player, BlockHitResult hit) {
    if (player.getMainHandItem().getItem() instanceof RaceMarkerItem
        || player.getMainHandItem().getItem() instanceof GardenBellItem) return InteractionResult.PASS;
    if (!level.isClientSide && player instanceof ServerPlayer keeper)
      ActivityManager.open(keeper, pos);
    return InteractionResult.sidedSuccess(level.isClientSide);
  }
  @Override protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState next, boolean moving) {
    if (!(this instanceof GardenTransporterBlock) && !state.is(next.getBlock()) && level instanceof ServerLevel server)
      ActivityManager.blockRemoved(server, pos, kind);
    super.onRemove(state, level, pos, next, moving);
  }
}
