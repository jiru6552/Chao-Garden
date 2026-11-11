package com.chaogarden.cwe;

import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

/** Retains an old inventory ID without advertising or restoring its removed feature. */
public final class RetiredCweGiftItem extends Item {
  public RetiredCweGiftItem(){super(new Properties().stacksTo(16));}
}
