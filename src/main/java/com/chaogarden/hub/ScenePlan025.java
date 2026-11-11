package com.chaogarden.hub;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiPredicate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.WallSignBlock;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.entity.SignText;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

/** A finite authored scenery plan. It never deletes entities or changes world rules. */
public final class ScenePlan025 {
  public final Map<BlockPos,BlockState> blocks=new LinkedHashMap<>();
  private final List<Notice> notices=new ArrayList<>();
  private record Notice(BlockPos position,Direction facing,DyeColor color,String[] lines){}
  public void put(int x,int y,int z,Block block){put(x,y,z,block.defaultBlockState());}
  public void put(int x,int y,int z,BlockState state){blocks.put(new BlockPos(x,y,z),state);}
  public void box(int x0,int y0,int z0,int x1,int y1,int z1,Block block){box(x0,y0,z0,x1,y1,z1,block.defaultBlockState());}
  public void box(int x0,int y0,int z0,int x1,int y1,int z1,BlockState state){
    for(int x=x0;x<=x1;x++)for(int y=y0;y<=y1;y++)for(int z=z0;z<=z1;z++)put(x,y,z,state);
  }
  public void notice(int x,int y,int z,Direction face,DyeColor color,String... lines){
    var position=new BlockPos(x,y,z);var support=position.relative(face.getOpposite());
    if(blocks.getOrDefault(support,Blocks.AIR.defaultBlockState()).isAir())blocks.put(support,Blocks.OAK_PLANKS.defaultBlockState());
    notices.add(new Notice(position,face,color,lines));
  }
  public void light(int x,int y,int z){put(x,y,z,Blocks.SEA_LANTERN);}
  public void apply(ServerLevel level,AABB area){apply(level,area,(p,s)->false);}
  public void apply(ServerLevel level,AABB area,BiPredicate<BlockPos,BlockState> preserve){
    for(int x=((int)Math.floor(area.minX))>>4;x<=(((int)Math.ceil(area.maxX)-1)>>4);x++)
      for(int z=((int)Math.floor(area.minZ))>>4;z<=(((int)Math.ceil(area.maxZ)-1)>>4);z++)level.getChunk(x,z);
    var cursor=new BlockPos.MutableBlockPos();
    for(int y=(int)Math.floor(area.minY);y<(int)Math.ceil(area.maxY);y++)
      for(int x=(int)Math.floor(area.minX);x<(int)Math.ceil(area.maxX);x++)
        for(int z=(int)Math.floor(area.minZ);z<(int)Math.ceil(area.maxZ);z++){
          cursor.set(x,y,z);var old=level.getBlockState(cursor);var next=blocks.getOrDefault(cursor,Blocks.AIR.defaultBlockState());
          if(!old.equals(next)&&!preserve.test(cursor,old))level.setBlock(cursor,next,Block.UPDATE_CLIENTS|Block.UPDATE_KNOWN_SHAPE);
        }
    for(var notice:notices){
      var state=Blocks.OAK_WALL_SIGN.defaultBlockState().setValue(WallSignBlock.FACING,notice.facing);
      level.setBlock(notice.position,state,Block.UPDATE_CLIENTS|Block.UPDATE_KNOWN_SHAPE);
      if(level.getBlockEntity(notice.position) instanceof SignBlockEntity sign){
        SignText text=new SignText().setColor(notice.color).setHasGlowingText(true);
        for(int i=0;i<Math.min(4,notice.lines.length);i++)text=text.setMessage(i,Component.literal(notice.lines[i]));
        sign.setText(text,true);sign.setText(text,false);sign.setWaxed(true);sign.setChanged();level.sendBlockUpdated(notice.position,state,state,Block.UPDATE_CLIENTS);
      }
    }
  }
}
