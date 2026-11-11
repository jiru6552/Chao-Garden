package com.chaogarden.toys;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.level.pathfinder.PathType;
import net.minecraft.world.entity.Mob;

/** A recoverable physical toy. Its short animation is driven by a real nearby play session. */
public final class ChaoToyBlock extends Block implements SimpleWaterloggedBlock {
  public static final DirectionProperty FACING=BlockStateProperties.HORIZONTAL_FACING;
  public static final BooleanProperty PLAYING=BooleanProperty.create("playing");
  public static final IntegerProperty FRAME=IntegerProperty.create("frame",0,3);
  public static final BooleanProperty WATERLOGGED=BlockStateProperties.WATERLOGGED;
  public final String toy;

  public ChaoToyBlock(String toy, Properties properties) {
    super(properties);
    this.toy=toy;
    registerDefaultState(stateDefinition.any().setValue(FACING,Direction.NORTH)
        .setValue(PLAYING,false).setValue(FRAME,0).setValue(WATERLOGGED,false));
  }
  @Override protected MapCodec<? extends Block> codec() {
    return simpleCodec(properties->new ChaoToyBlock(toy,properties));
  }
  @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> builder) {
    builder.add(FACING,PLAYING,FRAME,WATERLOGGED);
  }
  @Override public BlockState getStateForPlacement(BlockPlaceContext context) {
    return defaultBlockState().setValue(FACING,context.getHorizontalDirection().getOpposite())
        .setValue(WATERLOGGED,ToyContent.isWaterToy(toy)&&context.getLevel().getFluidState(context.getClickedPos()).getType()==Fluids.WATER);
  }
  @Override protected FluidState getFluidState(BlockState state) {
    return state.getValue(WATERLOGGED)?Fluids.WATER.getSource(false):super.getFluidState(state);
  }
  @Override protected BlockState updateShape(BlockState state,Direction side,BlockState neighbor,
      LevelAccessor level,BlockPos pos,BlockPos neighborPos) {
    if(state.getValue(WATERLOGGED))level.scheduleTick(pos,Fluids.WATER,Fluids.WATER.getTickDelay(level));
    return super.updateShape(state,side,neighbor,level,pos,neighborPos);
  }
  @Override protected BlockState rotate(BlockState state, Rotation rotation) {
    return state.setValue(FACING,rotation.rotate(state.getValue(FACING)));
  }
  @Override protected BlockState mirror(BlockState state, Mirror mirror) {
    return rotate(state,mirror.getRotation(state.getValue(FACING)));
  }
  @Override protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos,
                                         CollisionContext context) {
    double minX=3,maxX=13,minZ=3,maxZ=13,minY=0,height=8;
    switch(toy) {
      case "chao_trumpet" -> {minX=1;maxX=15;minZ=4;maxZ=12;height=7;}
      case "chao_tv" -> {minX=2;maxX=14;minZ=3;maxZ=13;height=16;}
      case "chao_ball" -> {minX=4;maxX=12;minZ=4;maxZ=12;height=state.getValue(PLAYING)?10:8;}
      case "chao_maracas" -> height=state.getValue(PLAYING)?6:4;
      case "chao_rocking_horse" -> {minX=5.3;maxX=10.7;minZ=.9;maxZ=15.1;height=14;}
      case "chao_piano" -> {minX=1;maxX=15;minZ=1.9;maxZ=14.1;height=9.6;}
      case "chao_organ" -> {minX=1.8;maxX=14.2;minZ=5;maxZ=11;height=15;}
      case "chao_lifesaver" -> {
        minX=3.4;maxX=12.6;minZ=3.4;maxZ=12.6;height=3;
        if(state.getValue(WATERLOGGED)){minY=12.4;height=16.2;}
      }
      case "chao_shovel" -> height=state.getValue(PLAYING)?10:4;
      case "chao_accordion" -> height=12;
    }
    double[] envelope=ToyInteractionRules.collisionEnvelope(toy);
    if(envelope==null)envelope=new double[]{minX,minY,minZ,maxX,height,maxZ};
    int turns=switch(state.getValue(FACING)){case EAST->1;case SOUTH->2;case WEST->3;default->0;};
    double[] b=ToyInteractionRules.rotateEnvelope(envelope,turns);
    return box(b[0],b[1],b[2],b[3],b[4],b[5]);
  }
  @Override protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos,
                                                   CollisionContext context) {
    if(ToyInteractionRules.keyboard(toy) && context instanceof net.minecraft.world.phys.shapes.EntityCollisionContext entity
        && entity.getEntity() instanceof com.chaogarden.entity.ChaoEntity) {
      // A Chao's single rectangular body box includes the empty space behind
      // its feet/torso. Let that box enter the clearance below the low manual;
      // the rendered body remains in front of the cabinet. Keep the
      // full player collision and interaction outline, and the rear wall here.
      double[] rear=toy.equals("chao_organ")?new double[]{1.8,0,8,14.2,15,11}:new double[]{1,0,4,15,9.6,14.1};
      int turns=switch(state.getValue(FACING)){case EAST->1;case SOUTH->2;case WEST->3;default->0;};
      double[] b=ToyInteractionRules.rotateEnvelope(rear,turns);
      return box(b[0],b[1],b[2],b[3],b[4],b[5]);
    }
    return getShape(state,level,pos,context);
  }
  @Override protected boolean isPathfindable(BlockState state,PathComputationType type) {
    // A non-full collision box is OPEN under BlockBehaviour's default rule.
    // These are physical toys: plan around their occupied cell, as for a chest,
    // instead of walking into the back of a TV on the way to its screen.
    return false;
  }
  @Override public PathType getBlockPathType(BlockState state,BlockGetter level,BlockPos pos,Mob mob) {
    // BLOCKED alone still permits a planned jump onto a cabinet. A crawling
    // Chao cannot carry its body forward far enough during that one-block
    // jump. Treat furniture above the ordinary0.6-block step as a fence-like
    // path obstacle, while preserving its actual collision/interaction shape.
    // The minicar is low enough to be a tempting stepping stone. A crawling
    // baby cannot reliably step down across it on its way to the rear contact.
    // The path cache also asks without a Mob, so this classification must not
    // depend on an entity being supplied. Keep the real collision for everyone.
    boolean carRoute=toy.equals("chao_toy_car");
    return carRoute||ToyInteractionRules.keyboard(toy)||state.getCollisionShape(level,pos).max(Direction.Axis.Y)>.6?PathType.FENCE:null;
  }
  @Override protected net.minecraft.world.level.block.RenderShape getRenderShape(BlockState state) {
    // FRAME=1 is hand presentation of the one reserved physical toy.
    return (toy.equals("chao_rocking_horse")&&state.getValue(PLAYING))
        ||BubblePlayRules.handToy(toy)&&state.getValue(PLAYING)&&state.getValue(FRAME)==1
        ?net.minecraft.world.level.block.RenderShape.INVISIBLE:super.getRenderShape(state);
  }
  @Override protected boolean isRandomlyTicking(BlockState state) { return state.getValue(PLAYING); }
  @Override protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
    // Scheduled block ticks persist normally. This also repairs a stale active
    // state after a crash/import without granting an item or a care reward.
    if(state.getValue(PLAYING))level.scheduleTick(pos,this,1);
  }
  @Override protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
    if(!state.getValue(PLAYING))return;
    if(!ToyContent.hasPlayerAt(level,pos)) {
      level.setBlock(pos,state.setValue(PLAYING,false).setValue(FRAME,0),3);
      return;
    }
    if(toy.equals(PianoPlayRules.KIND)){
      int frame=PianoPlayRules.frame(ToyContent.sessionElapsed(level,pos));
      if(frame!=state.getValue(FRAME))level.setBlock(pos,state.setValue(FRAME,frame),3);
      level.scheduleTick(pos,this,1);return;
    }
    if(toy.equals(OrganPlayRules.KIND)){
      int frame=OrganPlayRules.frame(ToyContent.sessionElapsed(level,pos));
      if(frame!=state.getValue(FRAME))level.setBlock(pos,state.setValue(FRAME,frame),3);
      level.scheduleTick(pos,this,1);return;
    }
    if(BubblePlayRules.handToy(toy)) {
      // The owning PlayGoal emits sound on the same timeline as the arm clip.
      // Do not also emit the old block chime or run its floating model frames.
      level.scheduleTick(pos,this,1);return;
    }
    if(toy.equals("chao_rocking_horse")||toy.equals("chao_lifesaver")) {
      int frame=ToyInteractionRules.mountedFrame(ToyContent.sessionElapsed(level,pos));
      if(frame!=state.getValue(FRAME))level.setBlock(pos,state.setValue(FRAME,frame),3);
      level.scheduleTick(pos,this,1);return;
    }
    if(toy.equals("chao_jack_in_box")) {
      int elapsed=ToyContent.sessionElapsed(level,pos),frame=ToyInteractionRules.jackFrame(elapsed);
      if(frame!=state.getValue(FRAME)) {
        level.setBlock(pos,state.setValue(FRAME,frame),3);
        if(state.getValue(FRAME)==0&&elapsed>=ToyInteractionRules.JACK_POP_TICK&&elapsed<128)
          ToyContent.playPulse(level,pos,toy);
      }
      level.scheduleTick(pos,this,1);return;
    }
    int frame=Math.floorMod(ToyContent.sessionElapsed(level,pos)/4,4);
    if(frame!=state.getValue(FRAME)) {
      level.setBlock(pos,state.setValue(FRAME,frame),3);
      if(frame==0)ToyContent.playPulse(level,pos,toy);
    }
    level.scheduleTick(pos,this,1);
  }
  @Override protected void onRemove(BlockState state, Level level, BlockPos pos,
                                     BlockState replacement, boolean moving) {
    if(!state.is(replacement.getBlock())&&level instanceof ServerLevel server)
      ToyContent.toyRemoved(server,pos);
    super.onRemove(state,level,pos,replacement,moving);
  }
}
