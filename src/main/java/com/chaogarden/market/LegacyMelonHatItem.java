package com.chaogarden.market;

import com.chaogarden.entity.ChaoEntity;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/** Hidden old registry ID decodes untouched containers; loaded use becomes vanilla melon. */
public final class LegacyMelonHatItem extends Item {
  public LegacyMelonHatItem(){super(new Item.Properties().stacksTo(1));}
  @Override public void inventoryTick(ItemStack stack,Level level,Entity holder,int slot,boolean selected) {
    if(level.isClientSide)return;
    if(holder instanceof Player player) {
      var inventory=player.getInventory();
      // Resolve by identity: armor/offhand inventoryTick indices are not main-slot indices.
      for(int i=0;i<inventory.getContainerSize();i++)if(inventory.getItem(i)==stack) {
        inventory.setItem(i,ChaoHats.canonicalCopy(stack));inventory.setChanged();return;
      }
    } else if(holder instanceof LivingEntity living) {
      for(var equipment:EquipmentSlot.values())if(living.getItemBySlot(equipment)==stack) {
        living.setItemSlot(equipment,ChaoHats.canonicalCopy(stack));return;
      }
    }
  }
  @Override public InteractionResult interactLivingEntity(ItemStack stack,Player player,LivingEntity target,InteractionHand hand) {
    return target instanceof ChaoEntity chao?ChaoHatItem.interact(chao,player,hand):InteractionResult.PASS;
  }
}
