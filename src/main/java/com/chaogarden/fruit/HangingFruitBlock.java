package com.chaogarden.fruit;

import com.chaogarden.ChaoGarden;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public final class HangingFruitBlock extends Block implements BonemealableBlock {
  public static final MapCodec<HangingFruitBlock> CODEC = RecordCodecBuilder.mapCodec(instance ->
      instance.group(Codec.STRING.fieldOf("fruit").forGetter(block -> block.fruit), propertiesCodec())
          .apply(instance, HangingFruitBlock::new));
  public static final IntegerProperty AGE = BlockStateProperties.AGE_3;
  public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
  private static final net.minecraft.tags.TagKey<Block> PALM_SUPPORTS=net.minecraft.tags.TagKey.create(
      net.minecraft.core.registries.Registries.BLOCK,net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(ChaoGarden.MODID,"garden_palm_fruit_supports"));
  private static final VoxelShape SHAPE = Block.box(4, 3, 4, 12, 20, 12);
  private static final VoxelShape DARK_SHAPE = Block.box(4, 3, 4, 12, 22, 12);
  public final String fruit;
  public HangingFruitBlock(String fruit, Properties properties) {
    super(properties); this.fruit = fruit;
    registerDefaultState(stateDefinition.any().setValue(AGE, 0).setValue(FACING, Direction.NORTH));
  }
  @Override public MapCodec<? extends HangingFruitBlock> codec() { return CODEC; }
  @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) { b.add(AGE, FACING); }
  @Override protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) { return fruit.equals("dark_garden_fruit") ? DARK_SHAPE : SHAPE; }
  @Override protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
    Direction towardTree = state.getValue(FACING);
    // The authored Neutral Garden palms retain the same real ripening/harvest
    // item as ordinary Garden Fruit trees. Only the tagged crown support and
    // adjacent palm trunk qualify, so a floating vial or arbitrary block cannot.
    if(fruit.equals("garden_fruit") && level.getBlockState(pos.above()).is(PALM_SUPPORTS)
        && level.getBlockState(pos.relative(towardTree).above()).is(com.chaogarden.hub.LobbyDecor025.PALM_CROWN.get())
        && level.getBlockState(pos.relative(towardTree)).is(com.chaogarden.hub.LobbyDecor025.PALM_TRUNK.get()))return true;
    // The Dark Garden's sparse bare fruit trunks use the same real pods,
    // ripening and single-item harvest. A real horizontal arm must join the
    // central wood joint and its underside must meet the actual pod stem.
    // Decorative floating fruit and a wrongly oriented arm cannot qualify.
    BlockState darkArm=level.getBlockState(pos.above());
    BlockState darkJoint=level.getBlockState(pos.relative(towardTree).above());
    if(fruit.equals("dark_garden_fruit")
        && darkArm.is(com.chaogarden.hub.DarkGardenDecor0255.block("dark_garden_deadwood"))
        && !darkArm.getValue(com.chaogarden.hub.DarkGardenDecor0255.Deadwood.BRANCHED)
        && darkArm.getValue(BlockStateProperties.AXIS)==towardTree.getAxis()
        && level.getBlockState(pos.relative(towardTree)).is(com.chaogarden.hub.DarkGardenDecor0255.block("dark_garden_deadwood"))
        && darkJoint.is(com.chaogarden.hub.DarkGardenDecor0255.block("dark_garden_deadwood"))
        && darkJoint.getValue(com.chaogarden.hub.DarkGardenDecor0255.Deadwood.BRANCHED))return true;
    BlockPos oldCrown = pos.relative(towardTree).above();
    BlockState old = level.getBlockState(oldCrown);
    if (FruitContent.isTrunk(level.getBlockState(oldCrown.below()))
        && old.is(FruitContent.treeForFruit(fruit))
        && (!old.getValue(ChaoTreeCrownBlock.EXPANDED) || old.getValue(ChaoTreeCrownBlock.GARDEN) && !old.getValue(ChaoTreeCrownBlock.WITHERED) && (level.getBlockState(pos.above()).is(FruitContent.PALM_SUPPORT.get())||fruit.equals("dark_garden_fruit")&&darkArm.is(FruitContent.DARK_TRUNK.get())&&!darkArm.getValue(com.chaogarden.hub.DarkGardenDecor0255.Deadwood.BRANCHED)&&darkArm.getValue(BlockStateProperties.AXIS)==towardTree.getAxis()))) return true;
    BlockPos crown = pos.relative(towardTree, 2).above();
    BlockState anchor = level.getBlockState(crown), leaves = level.getBlockState(pos.above());
    return anchor.is(FruitContent.treeForFruit(fruit))
        && anchor.getValue(ChaoTreeCrownBlock.EXPANDED)
        && level.getBlockState(crown.below()).is(FruitContent.TRUNK.get())
        && level.getBlockState(crown.relative(towardTree.getOpposite())).is(FruitContent.TRUNK.get())
        && leaves.is(FruitContent.LEAVES.get()) && leaves.getValue(LeavesBlock.DISTANCE) < 7;
  }
  @Override protected BlockState updateShape(BlockState state, Direction direction, BlockState neighbor,
      LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
    return !state.canSurvive(level, pos) ? Blocks.AIR.defaultBlockState()
        : super.updateShape(state, direction, neighbor, level, pos, neighborPos);
  }
  @Override protected boolean isRandomlyTicking(BlockState state) { return state.getValue(AGE) < 3; }
  @Override protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
    if (level.getRawBrightness(pos, 0) >= 8 && random.nextInt(5) == 0)
      level.setBlock(pos, state.setValue(AGE, Math.min(3, state.getValue(AGE)+1)), 3);
  }
  public boolean harvest(Level level, BlockPos pos, BlockState state) {
    if (state.getValue(AGE) != 3) return false;
    if (!level.isClientSide) {
      popResource(level, pos, new ItemStack(ChaoGarden.item(fruit), 1));
      level.setBlock(pos, state.setValue(AGE, 0), 3);
      level.playSound(null, pos, SoundEvents.SWEET_BERRY_BUSH_PICK_BERRIES, SoundSource.BLOCKS, .8f, 1.1f);
    }
    return true;
  }
  @Override protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level,
      BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
    if (stack.is(Items.BONE_MEAL) && state.getValue(AGE) < 3) return ItemInteractionResult.SKIP_DEFAULT_BLOCK_INTERACTION;
    return harvest(level, pos, state) ? ItemInteractionResult.sidedSuccess(level.isClientSide)
        : ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
  }
  @Override protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
      Player player, BlockHitResult hit) {
    return harvest(level, pos, state) ? InteractionResult.sidedSuccess(level.isClientSide) : InteractionResult.PASS;
  }
  @Override public boolean isValidBonemealTarget(LevelReader level, BlockPos pos, BlockState state) { return state.getValue(AGE) < 3; }
  @Override public boolean isBonemealSuccess(Level level, RandomSource random, BlockPos pos, BlockState state) { return true; }
  @Override public void performBonemeal(ServerLevel level, RandomSource random, BlockPos pos, BlockState state) {
    level.setBlock(pos, state.setValue(AGE, Math.min(3, state.getValue(AGE)+1)), 3);
  }
}
