package com.chaogarden.activities;

import com.chaogarden.entity.ChaoEntity;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.context.UseOnContext;

/** A deliberate assignment tool avoids claiming another keeper's wandering Chao. */
public final class GardenBellItem extends Item {
  public GardenBellItem(Properties properties){super(properties);}
  @Override public InteractionResult useOn(UseOnContext context){
    if(!context.getLevel().getBlockState(context.getClickedPos()).is(ActivityContent.GARDEN.get()))return InteractionResult.PASS;
    if(context.getPlayer() instanceof ServerPlayer player){
      var registry=GardenRegistry.get(player.server);var garden=registry.register(player,context.getClickedPos());
      if(!garden.owner().equals(player.getUUID()))return InteractionResult.FAIL;
      var link=garden.place().tag();CustomData.update(DataComponents.CUSTOM_DATA,context.getItemInHand(),n->n.put("GardenLink",link));
      player.displayClientMessage(Component.literal("Bell linked to "+garden.name()+". Use it on your Chao to assign it."),true);
    }
    return InteractionResult.sidedSuccess(context.getLevel().isClientSide);
  }
  @Override public InteractionResult interactLivingEntity(ItemStack stack,Player player,LivingEntity target,InteractionHand hand){
    if(!(target instanceof ChaoEntity chao))return InteractionResult.PASS;
    if(player.level().isClientSide)return InteractionResult.SUCCESS;
    if(!(player instanceof ServerPlayer keeper)||GardenRegistry.constructing(chao))return InteractionResult.FAIL;
    if(player.isShiftKeyDown()){
      if(!keeper.getUUID().equals(chao.owner)||ActivityManager.isBusy(chao)||GardenRegistry.managed(chao))return InteractionResult.FAIL;
      var registry=GardenRegistry.get(keeper.server);
      if(registry.entries.remove(chao.getUUID())!=null){registry.setDirty();chao.setStaying(false);}
      else chao.setStaying(!chao.isStaying());
      keeper.displayClientMessage(Component.literal(chao.isStaying()?"Your Chao will stay here.":"Your Chao is free to follow you."),true);
      return InteractionResult.CONSUME;
    }
    var n=stack.getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag();if(!n.contains("GardenLink",10))return InteractionResult.FAIL;
    var raw=GardenRegistry.Place.readLink(n.getCompound("GardenLink"));if(raw==null)return InteractionResult.FAIL;
    var registry=GardenRegistry.get(keeper.server);var place=registry.resolvePlace(raw);var garden=registry.gardens.get(place);
    if(garden==null||!registry.bind(keeper,chao,garden)){keeper.displayClientMessage(Component.literal("Use a linked bell on your available Chao; each garden holds six."),true);return InteractionResult.FAIL;}
    ActivityManager.callHome(chao,garden.place());
    keeper.displayClientMessage(Component.literal(chao.getName().getString()+" assigned to "+garden.name()),true);return InteractionResult.CONSUME;
  }
}
