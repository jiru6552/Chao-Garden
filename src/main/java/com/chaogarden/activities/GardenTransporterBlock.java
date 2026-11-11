package com.chaogarden.activities;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Garden roster terminal styled after the Chao-shaped SA2 PC transporter. */
public final class GardenTransporterBlock extends ActivityBlock {
  public static final MapCodec<GardenTransporterBlock> CODEC =
      simpleCodec(GardenTransporterBlock::new);
  public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
  /** 0..7 are world-cell offsets (x + 2*z + 4*y); 8 reads old single-cell saves. */
  public static final net.minecraft.world.level.block.state.properties.IntegerProperty PART =
      net.minecraft.world.level.block.state.properties.IntegerProperty.create("part", 0, 8);
  public static final int LEGACY = 8;
  private static final VoxelShape NORTH = northShape();
  private static final VoxelShape EAST = turn(NORTH);
  private static final VoxelShape SOUTH = turn(EAST);
  private static final VoxelShape WEST = turn(SOUTH);

  public GardenTransporterBlock(Properties properties) {
    super(Kind.GARDEN, properties);
    registerDefaultState(defaultBlockState().setValue(FACING, Direction.NORTH).setValue(PART, LEGACY));
  }

  @Override protected MapCodec<? extends GardenTransporterBlock> codec() { return CODEC; }

  @Override protected void createBlockStateDefinition(
      StateDefinition.Builder<net.minecraft.world.level.block.Block, BlockState> builder) {
    super.createBlockStateDefinition(builder);
    builder.add(FACING, PART);
  }

  public static boolean legacy(BlockState state) {
    return state.getBlock() instanceof GardenTransporterBlock && state.getValue(PART) == LEGACY;
  }
  public static BlockPos offset(int part) { return new BlockPos(part & 1, (part >> 2) & 1, (part >> 1) & 1); }
  public static java.util.List<BlockPos> footprint(BlockPos root) {
    var positions = new java.util.ArrayList<BlockPos>(8);
    for (int part=0;part<8;part++) positions.add(root.offset(offset(part)));
    return java.util.List.copyOf(positions);
  }
  public static BlockPos root(BlockGetter level, BlockPos pos) {
    var state=level.getBlockState(pos);
    return state.getBlock() instanceof GardenTransporterBlock && !legacy(state)
        ? pos.subtract(offset(state.getValue(PART))).immutable() : pos.immutable();
  }
  public static boolean compatibleState(BlockState old, BlockState planned) {
    return old.equals(planned) || legacy(old) && planned.is(old.getBlock()) && planned.getValue(PART)==0;
  }
  public static BlockState assembledState(Direction facing, int part) {
    return ActivityContent.GARDEN.get().defaultBlockState().setValue(FACING,facing).setValue(PART,part);
  }
  /** Root never moves: GardenRegistry ownership, members and old Bell links keep the same key. */
  public static void putScene(com.chaogarden.hub.ScenePlan025 plan, BlockPos root, Direction facing) {
    for(int part=0;part<8;part++) plan.blocks.put(root.offset(offset(part)),assembledState(facing,part));
  }
  /** The caller decides which old scenery cells are safe; no block entity is ever replaced. */
  public static boolean canMergeScene(net.minecraft.server.level.ServerLevel level, BlockPos root,
                                      java.util.function.BiPredicate<BlockPos,BlockState> safe) {
    for(int part=0;part<8;part++) {
      var at=root.offset(offset(part));var old=level.getBlockState(at);
      if(level.getBlockEntity(at)!=null || !(part==0 && legacy(old)) && !safe.test(at,old)) return false;
    }
    return true;
  }

  @Override public BlockState getStateForPlacement(BlockPlaceContext context) {
    var facing=context.getHorizontalDirection().getOpposite();
    return canPlace(context.getLevel(),context.getClickedPos(),facing,context,false)
        ? assembledState(facing,0) : null;
  }
  /** Validate every physical cell, including realm boundaries, before native BlockItem capture. */
  public static boolean canPlace(net.minecraft.world.level.Level level, BlockPos root, Direction facing,
                                 BlockPlaceContext context, boolean upgrading) {
    for(int part=0;part<8;part++) {
      var at=root.offset(offset(part));var old=level.getBlockState(at);
      boolean ownLegacy=upgrading && part==0 && legacy(old);
      if(!ownLegacy && com.chaogarden.hub.GardenRooms025.placeable(old.getBlock())) return false;
      if(!level.isInWorldBounds(at) || !level.getWorldBorder().isWithinBounds(at)
          || level.getBlockEntity(at)!=null || !(upgrading && part==0 && legacy(old)) && !old.canBeReplaced(context)) return false;
      if(com.chaogarden.hub.HubContent.isGardenDimension(level)) {
        var realm=level.dimension();
        var bounds=com.chaogarden.hub.HubContent.gardenBounds(realm);
        if(!bounds.contains(at.getCenter()) || !com.chaogarden.hub.HubContent.insideGarden(realm,at.getX(),at.getZ())) return false;
      }
      if(part<4 && !level.getBlockState(at.below()).isFaceSturdy(level,at.below(),Direction.UP)) return false;
      var collision=context.getPlayer()==null?CollisionContext.empty():CollisionContext.of(context.getPlayer());
      if(!level.isUnobstructed(assembledState(facing,part),at,collision)) return false;
    }
    return true;
  }
  @Override public void setPlacedBy(net.minecraft.world.level.Level level, BlockPos pos, BlockState state,
                                   net.minecraft.world.entity.LivingEntity placer, net.minecraft.world.item.ItemStack stack) {
    if(!level.isClientSide&&placer instanceof net.minecraft.server.level.ServerPlayer keeper)GardenRegistry.get(keeper.server).register(keeper,pos);
    if(legacy(state)) return;
    for(int part=1;part<8;part++) level.setBlock(pos.offset(offset(part)),assembledState(state.getValue(FACING),part),3);
  }
  @Override protected net.minecraft.world.InteractionResult useWithoutItem(BlockState state,
      net.minecraft.world.level.Level level, BlockPos pos, net.minecraft.world.entity.player.Player player,
      net.minecraft.world.phys.BlockHitResult hit) {
    if(legacy(state) && !level.isClientSide && player.getMainHandItem().isEmpty()) {
      var context=new BlockPlaceContext(new net.minecraft.world.item.context.UseOnContext(player,net.minecraft.world.InteractionHand.MAIN_HAND,hit));
      var owner=GardenRegistry.get(((net.minecraft.server.level.ServerLevel)level).getServer()).gardens.get(GardenRegistry.Place.at((net.minecraft.server.level.ServerLevel)level,pos));
      if((owner==null||owner.owner().equals(player.getUUID())) && canPlace(level,pos,state.getValue(FACING),context,true)) {
        var next=assembledState(state.getValue(FACING),0);
        // Preflight is complete before the first write, and all seven neighbors
        // remain untouched if any old purchase/body/border obstructs expansion.
        level.setBlock(pos,next,3);setPlacedBy(level,pos,next,player,player.getMainHandItem());
      }
    }
    return super.useWithoutItem(state,level,pos,player,hit);
  }
  @Override protected BlockState updateShape(BlockState state,Direction direction,BlockState neighbor,
      net.minecraft.world.level.LevelAccessor level,BlockPos pos,BlockPos neighborPos) {
    if(!legacy(state)) level.scheduleTick(pos,this,1);
    return state;
  }
  @Override protected void tick(BlockState state,net.minecraft.server.level.ServerLevel level,
      BlockPos pos,net.minecraft.util.RandomSource random) {
    if(legacy(state)) return;
    var base=root(level,pos);var first=level.getBlockState(base);
    boolean complete=first.is(this) && !legacy(first) && first.getValue(PART)==0;
    for(int part=0;complete&&part<8;part++) {
      var at=base.offset(offset(part));var sibling=level.getBlockState(at);
      complete=sibling.is(this) && !legacy(sibling) && sibling.getValue(PART)==part
          && sibling.getValue(FACING)==state.getValue(FACING)
          && (part>=4 || level.getBlockState(at.below()).isFaceSturdy(level,at.below(),Direction.UP));
    }
    if(!complete) {
      // A root normally owns the single loot entry. An orphan has no second
      // item: its root was already harvested by the native removal callback.
      if(first.is(this) && !legacy(first) && first.getValue(PART)==0) level.destroyBlock(base,true);
      else level.removeBlock(pos,false);
    }
  }
  @Override protected BlockState rotate(BlockState state, Rotation rotation) {
    int part=state.getValue(PART);
    if(part!=LEGACY) {
      int x=part&1,z=(part>>1)&1,y=part&4;
      int turns=switch(rotation){case CLOCKWISE_90->1;case CLOCKWISE_180->2;case COUNTERCLOCKWISE_90->3;default->0;};
      for(int i=0;i<turns;i++){int previous=x;x=1-z;z=previous;}
      state=state.setValue(PART,x+2*z+y);
    }
    return state.setValue(FACING,rotation.rotate(state.getValue(FACING)));
  }
  @Override protected BlockState mirror(BlockState state, Mirror mirror) {
    int part=state.getValue(PART);
    if(part!=LEGACY) {
      if(mirror==Mirror.LEFT_RIGHT) part^=2;
      if(mirror==Mirror.FRONT_BACK) part^=1;
      state=state.setValue(PART,part);
    }
    return state.setValue(FACING,mirror.mirror(state.getValue(FACING)));
  }
  @Override protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
    if(!legacy(state)) return GardenDeviceGeometry0254.shape(state.getValue(FACING),state.getValue(PART));
    return switch(state.getValue(FACING)){case EAST->EAST;case SOUTH->SOUTH;case WEST->WEST;default->NORTH;};
  }
  @Override public BlockState playerWillDestroy(net.minecraft.world.level.Level level, BlockPos pos,
                                                BlockState state, net.minecraft.world.entity.player.Player player) {
    if(!level.isClientSide && !legacy(state) && state.getValue(PART)!=0) {
      var base=root(level,pos);var lower=level.getBlockState(base);
      if(lower.is(this) && !legacy(lower) && lower.getValue(PART)==0) {
        if(!player.isCreative()) dropResources(lower,level,base,null,player,player.getMainHandItem());
        level.setBlock(base,net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(),35);
      }
    }
    return super.playerWillDestroy(level,pos,state,player);
  }
  private static final ThreadLocal<java.util.Set<BlockPos>> REMOVING=ThreadLocal.withInitial(java.util.HashSet::new);
  @Override protected void onRemove(BlockState state, net.minecraft.world.level.Level level, BlockPos pos,
                                     BlockState next, boolean moving) {
    if(!state.is(next.getBlock()) && !level.isClientSide) {
      var base=legacy(state)?pos:pos.subtract(offset(state.getValue(PART)));
      var removing=REMOVING.get();
      if(removing.add(base)) {
        try {
          if(!legacy(state)) for(int part=0;part<8;part++) {
            var at=base.offset(offset(part));var sibling=level.getBlockState(at);
            if(!at.equals(pos) && sibling.is(this) && !legacy(sibling)
                && sibling.getValue(PART)==part && sibling.getValue(FACING)==state.getValue(FACING))
              level.setBlock(at,net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(),35);
          }
          if(level instanceof net.minecraft.server.level.ServerLevel server) ActivityManager.blockRemoved(server,base,kind);
        } finally {removing.remove(base);if(removing.isEmpty()) REMOVING.remove();}
      }
    }
    super.onRemove(state,level,pos,next,moving);
  }

  private static VoxelShape scaledBox(double x0, double y0, double z0,
                                      double x1, double y1, double z1) {
    return box(8 + (x0 - 8)*.6, y0*.6, 8 + (z0 - 8)*.6,
               8 + (x1 - 8)*.6, y1*.6, 8 + (z1 - 8)*.6);
  }

  private static VoxelShape northShape() {
    // Same scaled construction space as generate_transporter_assets.py. The
    // whole terminal, including its orb, stays inside a single block cell.
    return Shapes.or(
        scaledBox(-2,0,-3,18,2,17),
        scaledBox(1.75,2,8,14.25,7.5,14),
        scaledBox(5.5,2,3.8,10.5,5,10),
        scaledBox(7,7.5,10.5,9,10.5,12.5),
        scaledBox(2,10,7.2,14,12,15),
        scaledBox(1,12,6,15,16.5,15.5),
        scaledBox(1.8,16.5,6.03,14.2,19.5,15),
        scaledBox(3.1,19.5,7.8,12.9,21,14.5),
        scaledBox(4.5,21,8.5,11.5,22,13.9),
        scaledBox(6,22,9.1,10,23.2,13),
        scaledBox(7.2,23.2,10.2,8.8,23.9,11.8),
        scaledBox(6.7,23.9,9.7,9.3,26.3,12.3),
        scaledBox(4.4,2, -.5,11.6,2.75,4.7)).optimize();
  }

  private static VoxelShape turn(VoxelShape shape) {
    VoxelShape result = Shapes.empty();
    for (var a : shape.toAabbs()) {
      result = Shapes.or(result, box(16-a.maxZ*16, a.minY*16, a.minX*16,
                                    16-a.minZ*16, a.maxY*16, a.maxX*16));
    }
    return result.optimize();
  }
}
