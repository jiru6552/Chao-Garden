package com.chaogarden.fruit;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.BushBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public final class ChaoSaplingBlock extends BushBlock implements BonemealableBlock {
  public static final MapCodec<ChaoSaplingBlock> CODEC = RecordCodecBuilder.mapCodec(instance ->
      instance.group(Codec.STRING.fieldOf("fruit").forGetter(block -> block.fruit), propertiesCodec())
          .apply(instance, ChaoSaplingBlock::new));
  public static final IntegerProperty STAGE = BlockStateProperties.STAGE;
  private static final VoxelShape SHAPE = Block.box(3, 0, 3, 13, 12, 13);
  public final String fruit;
  public ChaoSaplingBlock(String fruit, Properties properties) {
    super(properties);
    this.fruit = fruit;
    registerDefaultState(stateDefinition.any().setValue(STAGE, 0));
  }
  @Override public MapCodec<? extends ChaoSaplingBlock> codec() { return CODEC; }
  @Override protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos,
      CollisionContext context) { return SHAPE; }
  @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) { b.add(STAGE); }
  @Override protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
    if (level.isAreaLoaded(pos, 2) && level.getMaxLocalRawBrightness(pos.above()) >= 9
        && random.nextInt(7) == 0) advance(level, pos, state);
  }
  public boolean advance(ServerLevel level, BlockPos pos, BlockState state) {
    if (state.getValue(STAGE) == 0) { level.setBlock(pos, state.setValue(STAGE, 1), 3); return true; }
    return FruitContent.growTree(level, pos, fruit);
  }
  @Override public boolean isValidBonemealTarget(LevelReader level, BlockPos pos, BlockState state) { return true; }
  @Override public boolean isBonemealSuccess(Level level, RandomSource random, BlockPos pos, BlockState state) { return true; }
  @Override public void performBonemeal(ServerLevel level, RandomSource random, BlockPos pos, BlockState state) { advance(level, pos, state); }
}
