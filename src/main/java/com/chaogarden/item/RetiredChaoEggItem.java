package com.chaogarden.item;

import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

/** Old registry IDs load safely, but now represent ordinary eggs only. */
public final class RetiredChaoEggItem extends ChaoEggItem {
  public RetiredChaoEggItem() {
    super(0, false, 0);
  }

}
