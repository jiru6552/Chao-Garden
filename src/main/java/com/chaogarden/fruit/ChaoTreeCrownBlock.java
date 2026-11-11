package com.chaogarden.fruit;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Fruit-specific wood anchor; legacy palms retain their old blockstate until upgraded. */
public final class ChaoTreeCrownBlock extends Block implements BonemealableBlock, net.minecraft.world.level.block.EntityBlock {
  public static final MapCodec<ChaoTreeCrownBlock> CODEC = RecordCodecBuilder.mapCodec(instance ->
      instance.group(Codec.STRING.fieldOf("fruit").forGetter(block -> block.fruit), propertiesCodec())
          .apply(instance, ChaoTreeCrownBlock::new));
  private static final VoxelShape SHAPE = Shapes.or(Block.box(5, 0, 5, 11, 9, 11),
      Block.box(-8, 3, 6, 24, 7, 10), Block.box(6, 3, -8, 10, 7, 24),
      Block.box(-8, 7, -8, 24, 13, 24));
  public static final BooleanProperty EXPANDED = BooleanProperty.create("expanded");
  public static final BooleanProperty GARDEN=BooleanProperty.create("garden"), WITHERED=BooleanProperty.create("withered");
  public static final BooleanProperty BARE=BooleanProperty.create("bare");
  public final String fruit;
  public ChaoTreeCrownBlock(String fruit, Properties properties) {
    super(properties); this.fruit = fruit;
    registerDefaultState(stateDefinition.any().setValue(EXPANDED, false).setValue(GARDEN,false).setValue(WITHERED,false).setValue(BARE,false));
  }
  @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
    builder.add(EXPANDED,GARDEN,WITHERED,BARE);
  }
  @Override public net.minecraft.world.level.block.entity.BlockEntity newBlockEntity(BlockPos pos,BlockState state){return new FruitCrownEntity(pos,state);}
  @Override public MapCodec<? extends ChaoTreeCrownBlock> codec() { return CODEC; }
  @Override protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos,
      CollisionContext context) { return state.getValue(BARE)?Shapes.or(Block.box(6,0,6,10,16,10),Block.box(0,6,6,16,10,10),Block.box(6,6,0,10,10,16)):state.getValue(GARDEN) ? Block.box(4,0,4,12,9,12) : state.getValue(EXPANDED) ? Shapes.block() : SHAPE; }
  @Override protected BlockState updateShape(BlockState state, Direction direction, BlockState neighbor,
      LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
    return direction == Direction.DOWN && !FruitContent.isTrunk(neighbor) ? Blocks.AIR.defaultBlockState()
        : super.updateShape(state, direction, neighbor, level, pos, neighborPos);
  }
  @Override protected void onRemove(BlockState state, Level level, BlockPos pos,
      BlockState replacement, boolean moving) {
    if (!replacement.is(this) && !level.isClientSide) {
      for (BlockPos pod : FruitContent.podPositions(pos, state.getValue(EXPANDED) && !state.getValue(GARDEN))) {
        if (level.getBlockState(pod).is(FruitContent.podForFruit(fruit))) level.destroyBlock(pod, true);
      }
    }
    if (!replacement.is(this) && state.getValue(GARDEN)) for(Direction side:Direction.Plane.HORIZONTAL) {
      var at=pos.relative(side);if(level.getBlockState(at).is(FruitContent.PALM_SUPPORT.get()))level.removeBlock(at,false);
    }
    super.onRemove(state, level, pos, replacement, moving);
  }
  @Override protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
    if (level.isAreaLoaded(pos, 2) && random.nextInt(6) == 0) FruitContent.replenishPods(level, pos, fruit);
  }
  @Override public boolean isValidBonemealTarget(LevelReader level, BlockPos pos, BlockState state) {
    if (!state.getValue(EXPANDED) && FruitContent.canExpandLegacyTree(level, pos, fruit)) return true;
    for (BlockPos at : FruitContent.podPositions(level, pos, fruit)) {
      BlockState pod = level.getBlockState(at);
      if (pod.isAir() || pod.is(FruitContent.podForFruit(fruit)) && pod.getValue(HangingFruitBlock.AGE) < 3) return true;
    }
    return false;
  }
  @Override public boolean isBonemealSuccess(Level level, RandomSource random, BlockPos pos, BlockState state) { return true; }
  @Override public void performBonemeal(ServerLevel level, RandomSource random, BlockPos pos, BlockState state) {
    if (!state.getValue(EXPANDED) && FruitContent.expandLegacyTree(level, pos, fruit))
      pos = FruitContent.crownPosition(pos.below(3));
    FruitContent.replenishPods(level, pos, fruit);
    for (BlockPos at : FruitContent.podPositions(level, pos, fruit)) {
      BlockState pod = level.getBlockState(at);
      if (pod.is(FruitContent.podForFruit(fruit)))
        level.setBlock(at, pod.setValue(HangingFruitBlock.AGE, Math.min(3, pod.getValue(HangingFruitBlock.AGE) + 1)), 3);
    }
  }
}
