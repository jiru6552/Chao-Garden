package com.chaogarden.item;

import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.*;

/** The journal recipe alone returns the key; teleporter crafting still consumes it. */
public final class ChaoKeyItem extends Item {
  public ChaoKeyItem(){super(new Properties());}
  @Override public void appendHoverText(ItemStack stack,TooltipContext context,List<Component> tips,TooltipFlag flag){
    tips.add(Component.literal("Combine with a book to learn more about caring for a Chao!"));
  }
}
