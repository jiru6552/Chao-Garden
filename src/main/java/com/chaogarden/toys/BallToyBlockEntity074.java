package com.chaogarden.toys;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/** Cosmetic motion of the one committed ball block; no inventory or entity copy is stored. */
public final class BallToyBlockEntity074 extends BlockEntity {
  private BlockPos from;
  private long started=Long.MIN_VALUE;
  private Direction direction=Direction.NORTH;
  public BallToyBlockEntity074(BlockPos pos,BlockState state){super(ToyContent.BALL_ENTITY.get(),pos,state);}
  public long rollStarted(){return started;}
  public Direction rollDirection(){return direction;}
  public double progress(float partial){return started==Long.MIN_VALUE||level==null?1:BallPlayRules074.rollProgress(level.getGameTime()-started+Math.max(0,Math.min(1,partial)));}
  public boolean rolling(float partial){return from!=null&&started!=Long.MIN_VALUE&&level!=null&&progress(partial)<1;}
  public boolean presenting(){return from!=null&&started!=Long.MIN_VALUE&&level!=null&&level.getGameTime()-started<BallPlayRules074.PRESENTATION_TICKS;}
  public Vec3 offset(float partial){return from==null?Vec3.ZERO:Vec3.atLowerCornerOf(from).subtract(Vec3.atLowerCornerOf(worldPosition)).scale(1-progress(partial));}
  public void rollFrom(BlockPos previous,long now,Direction movement){
    if(level==null||level.isClientSide||previous.getY()!=worldPosition.getY()||previous.distManhattan(worldPosition)!=1||movement.getAxis().isVertical())return;
    from=previous.immutable();started=now;direction=movement;setChanged();
    var old=getBlockState();var next=old.setValue(ChaoBallBlock074.ROLLING,true);level.setBlock(worldPosition,next,3);level.sendBlockUpdated(worldPosition,old,next,3);
    level.scheduleTick(worldPosition,next.getBlock(),1);
  }
  public void finishRoll(){from=null;started=Long.MIN_VALUE;setChanged();}
  @Override public void onLoad(){
    super.onLoad();
    if(level!=null&&!level.isClientSide){
      var state=getBlockState();
      // Disk loads contain only the committed block, never an unfinished cosmetic transfer.
      if(state.getValue(ChaoBallBlock074.ROLLING)&&!rolling(0))level.setBlock(worldPosition,state.setValue(ChaoBallBlock074.ROLLING,false),3);
      if(state.getValue(ChaoToyBlock.PLAYING)||state.getValue(ChaoBallBlock074.ROLLING))level.scheduleTick(worldPosition,state.getBlock(),1);
    }
  }
  @Override protected void saveAdditional(CompoundTag tag,HolderLookup.Provider lookup){super.saveAdditional(tag,lookup);}
  @Override protected void loadAdditional(CompoundTag tag,HolderLookup.Provider lookup){super.loadAdditional(tag,lookup);readMotion(tag);}
  private void readMotion(CompoundTag tag){
    from=null;started=Long.MIN_VALUE;direction=Direction.NORTH;
    if(!tag.getBoolean("RollNetwork074")||!tag.contains("RollFrom")||!tag.contains("RollStarted"))return;
    var old=BlockPos.of(tag.getLong("RollFrom"));var face=Direction.from3DDataValue(tag.getInt("RollDirection"));
    if(old.getY()!=worldPosition.getY()||old.distManhattan(worldPosition)!=1||face.getAxis().isVertical())return;
    from=old;started=tag.getLong("RollStarted");direction=face;
  }
  @Override public CompoundTag getUpdateTag(HolderLookup.Provider lookup){
    var tag=new CompoundTag();tag.putBoolean("RollNetwork074",true);
    if(from!=null){tag.putLong("RollFrom",from.asLong());tag.putLong("RollStarted",started);tag.putInt("RollDirection",direction.get3DDataValue());}
    return tag;
  }
  @Override public void handleUpdateTag(CompoundTag tag,HolderLookup.Provider lookup){readMotion(tag);}
  @Override public ClientboundBlockEntityDataPacket getUpdatePacket(){return ClientboundBlockEntityDataPacket.create(this);}
}
