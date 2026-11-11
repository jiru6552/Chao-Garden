package com.chaogarden.toys;

import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

/** Compatibility ID only: old saved stacks retain every component without remaining a toy. */
public final class RetiredCoffinItem extends Item {
  public RetiredCoffinItem(){super(new Properties().stacksTo(16));}
  @Override public void appendHoverText(ItemStack stack,TooltipContext context,List<Component> lines,TooltipFlag flag){
    lines.add(Component.translatable("tooltip.chao_garden.retired_toy"));
  }
}
