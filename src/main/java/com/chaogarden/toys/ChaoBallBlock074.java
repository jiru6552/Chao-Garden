package com.chaogarden.toys;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.EntityCollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Same recoverable ball block; only this toy has transient rolling presentation data. */
public final class ChaoBallBlock074 extends ChaoToyBlock implements EntityBlock {
  public static final BooleanProperty ROLLING=BooleanProperty.create("rolling");
  public ChaoBallBlock074(Properties properties){super(BallPlayRules074.KIND,properties);registerDefaultState(defaultBlockState().setValue(ROLLING,false));}
  @Override protected MapCodec<? extends net.minecraft.world.level.block.Block> codec(){return simpleCodec(ChaoBallBlock074::new);}
  @Override protected void createBlockStateDefinition(StateDefinition.Builder<net.minecraft.world.level.block.Block,BlockState> builder){super.createBlockStateDefinition(builder);builder.add(ROLLING);}
  @Override public BlockEntity newBlockEntity(BlockPos pos,BlockState state){return new BallToyBlockEntity074(pos,state);}
  @Override protected VoxelShape getCollisionShape(BlockState state,BlockGetter level,BlockPos pos,CollisionContext context){
    if(context instanceof EntityCollisionContext entity&&entity.getEntity() instanceof com.chaogarden.entity.ChaoEntity c&&ToyContent.ownsBallContact(c,pos))return Shapes.empty();
    return super.getCollisionShape(state,level,pos,context);
  }
  @Override protected boolean isRandomlyTicking(BlockState state){return state.getValue(PLAYING)||state.getValue(ROLLING);}
  @Override protected void randomTick(BlockState state,ServerLevel level,BlockPos pos,RandomSource random){if(isRandomlyTicking(state))level.scheduleTick(pos,this,1);}
  @Override protected void tick(BlockState state,ServerLevel level,BlockPos pos,RandomSource random){
    if(state.getValue(ROLLING)){
      var blockEntity=level.getBlockEntity(pos);
      if(!(blockEntity instanceof BallToyBlockEntity074 ball)||!ball.presenting()){
        if(blockEntity instanceof BallToyBlockEntity074 ball)ball.finishRoll();
        state=state.setValue(ROLLING,false);level.setBlock(pos,state,3);
      }
    }
    super.tick(state,level,pos,random);
    var current=level.getBlockState(pos);
    if(current.is(this)&&(current.getValue(PLAYING)||current.getValue(ROLLING)))level.scheduleTick(pos,this,1);
  }
}
