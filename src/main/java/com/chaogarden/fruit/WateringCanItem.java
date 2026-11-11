package com.chaogarden.fruit;

import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;

public final class WateringCanItem extends Item {
  public WateringCanItem(){super(new Properties().stacksTo(1));}
  @Override public InteractionResult useOn(UseOnContext context) {
    if(context.getPlayer()==null)return InteractionResult.PASS;
    if(context.getLevel().isClientSide)return InteractionResult.SUCCESS;
    if(!GardenTools.water((ServerLevel)context.getLevel(),context.getClickedPos()))return InteractionResult.PASS;
    context.getPlayer().getCooldowns().addCooldown(this,40);
    return InteractionResult.CONSUME;
  }
}
