package com.chaogarden.care;

import com.chaogarden.core.ChaoState;
import com.chaogarden.entity.ChaoEntity;
import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

/** A real, consumable remedy; offered doses follow the ordinary held-gift transaction. */
public final class ChaoMedicineItem extends Item {
  public ChaoMedicineItem() { super(new Properties().stacksTo(16)); }
  public boolean canTreat(ChaoState state) {
    return !state.dead && state.cocoonTicks <= 0 && state.isIll();
  }
  public void treat(ChaoState state) { state.cureAllIllnesses(); }
  @Override public InteractionResult interactLivingEntity(ItemStack stack, Player player,
      LivingEntity entity, InteractionHand hand) {
    return entity instanceof ChaoEntity chao ? chao.offerItem(player,hand) : InteractionResult.PASS;
  }
}
