package com.chaogarden.market;

import com.chaogarden.ChaoGarden;
import com.chaogarden.activities.ActivityManager;
import com.chaogarden.entity.ChaoEntity;
import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;

/** Cosmetic headwear uses vanilla equipment synchronization and entity save data. */
public final class ChaoHatItem extends Item {
  public final String hat;
  public ChaoHatItem(String hat) { super(new Item.Properties().stacksTo(1)); this.hat=hat; }
  @Override public InteractionResult interactLivingEntity(ItemStack stack, Player player, LivingEntity target, InteractionHand hand) {
    return target instanceof ChaoEntity chao ? interact(chao,player,hand) : InteractionResult.PASS;
  }
  public static InteractionResult interact(ChaoEntity chao, Player player, InteractionHand hand) {
    ItemStack held=player.getItemInHand(hand), old=chao.getItemBySlot(EquipmentSlot.HEAD);
    boolean remove=held.is(Items.SHEARS) && old.getItem() instanceof ChaoHatItem;
    if (!(held.getItem() instanceof ChaoHatItem) && !remove) return InteractionResult.PASS;
    if (!remove) return chao.offerItem(player,hand);
    if (!chao.canCare(player)) return InteractionResult.FAIL;
    if (ActivityManager.isBusy(chao) || chao.isHandlingItem() || chao.isCocooned() || chao.isResting()) return InteractionResult.FAIL;
    if (chao.level().isClientSide) return InteractionResult.SUCCESS;
    ItemStack returned=old.copy();
    chao.setItemSlot(EquipmentSlot.HEAD,ItemStack.EMPTY);
    if (!returned.isEmpty() && !player.addItem(returned)) player.drop(returned,false);
    chao.playSound(net.minecraft.sounds.SoundEvents.ARMOR_EQUIP_LEATHER.value(),.4f,1.3f);
    return InteractionResult.CONSUME;
  }
}
