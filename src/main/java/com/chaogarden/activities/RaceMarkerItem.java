package com.chaogarden.activities;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.Block;

public final class RaceMarkerItem extends BlockItem {
  public RaceMarkerItem(Block block, Properties properties) { super(block,properties); }
  @Override public InteractionResult useOn(UseOnContext context) {
    var level=context.getLevel(); var pos=context.getClickedPos(); var clicked=level.getBlockState(pos).getBlock();
    if(level.isClientSide) return InteractionResult.SUCCESS;
    if(!(context.getPlayer() instanceof ServerPlayer player))return InteractionResult.FAIL;
    if(clicked instanceof ActivityBlock block && (block.kind==ActivityBlock.Kind.START || block.kind==ActivityBlock.Kind.FLAG)) {
      BlockPos source=ActivityManager.selectLink(player,pos);
      if(source==null)return InteractionResult.FAIL;
      var link=new CompoundTag();link.putString("Dimension",level.dimension().location().toString());link.putLong("Pos",source.asLong());
      CustomData.update(DataComponents.CUSTOM_DATA,context.getItemInHand(),tag->tag.put("RaceLink",link));
      player.displayClientMessage(Component.literal("Start selected. Place the next flag within 32 blocks, or link to a finish line."),true);
      return InteractionResult.CONSUME;
    }
    var data=context.getItemInHand().getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag();
    if(!data.contains("RaceLink")){player.displayClientMessage(Component.literal("First right-click a starting line or an existing flag."),true);return InteractionResult.FAIL;}
    var link=data.getCompound("RaceLink");
    if(!link.getString("Dimension").equals(level.dimension().location().toString()))return InteractionResult.FAIL;
    BlockPos from=BlockPos.of(link.getLong("Pos"));
    if(!level.hasChunkAt(from))return InteractionResult.FAIL;
    var sourceBlock=level.getBlockState(from).getBlock();
    if(!(sourceBlock instanceof ActivityBlock source)||(source.kind!=ActivityBlock.Kind.START&&source.kind!=ActivityBlock.Kind.FLAG)){
      CustomData.update(DataComponents.CUSTOM_DATA,context.getItemInHand(),tag->tag.remove("RaceLink"));
      player.displayClientMessage(Component.literal("The course start is no longer here. Select the starting line again."),true);
      return InteractionResult.FAIL;
    }
    if(clicked instanceof ActivityBlock block && block.kind==ActivityBlock.Kind.FINISH){
      BlockPos target=ActivityManager.controller(player.serverLevel(),pos);
      if(!ActivityManager.connect(player,from,target))return InteractionResult.FAIL;
      CustomData.update(DataComponents.CUSTOM_DATA,context.getItemInHand(),tag->tag.remove("RaceLink"));
      player.displayClientMessage(Component.literal("Course connected to the finish line."),true);return InteractionResult.CONSUME;
    }
    var placement=new BlockPlaceContext(context);BlockPos target=placement.getClickedPos();
    if(!ActivityManager.canConnect(player,from,target,true))return InteractionResult.FAIL;
    InteractionResult result=super.useOn(context);
    if(result.consumesAction() && level.getBlockState(target).is(ActivityContent.FLAG.get())){
      if(!ActivityManager.connect(player,from,target)){
        level.destroyBlock(target,true);return InteractionResult.FAIL;
      }
      var next=new CompoundTag();next.putString("Dimension",level.dimension().location().toString());next.putLong("Pos",target.asLong());
      CustomData.update(DataComponents.CUSTOM_DATA,context.getItemInHand(),tag->tag.put("RaceLink",next));
      player.displayClientMessage(Component.literal("Flag linked. Continue within 32 blocks; right-click a finish line to complete."),true);
    }
    return result;
  }
}
