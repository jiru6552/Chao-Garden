package com.chaogarden.market;

import com.chaogarden.ChaoGarden;
import com.chaogarden.entity.ChaoEntity;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;

/** Chao-specific headwear; vanilla item behavior outside Chao care is unchanged. */
@EventBusSubscriber(modid=ChaoGarden.MODID)
public final class ChaoHats {
  public static final String PUMPKIN="vanilla_carved_pumpkin",SKULL="vanilla_skeleton_skull";
  private ChaoHats() {}

  public static String kind(ItemStack stack) {
    if(stack.isEmpty())return "";
    if(stack.is(Items.CARVED_PUMPKIN))return PUMPKIN;
    if(stack.is(Items.MELON)||stack.getItem() instanceof LegacyMelonHatItem)return "watermelon_hat";
    if(stack.is(Items.SKELETON_SKULL))return SKULL;
    return stack.getItem() instanceof ChaoHatItem hat?hat.hat:"";
  }
  public static boolean isHat(ItemStack stack) {return !kind(stack).isEmpty();}

  /** The item ID changes, while count and every explicit component patch survive.
   * Nested container/custom-data payloads are deliberately opaque, never traversed. */
  public static ItemStack canonicalCopy(ItemStack stack) {
    if(!(stack.getItem() instanceof LegacyMelonHatItem))return stack.copy();
    var converted=stack.transmuteCopy(Items.MELON);
    // A legal foreign damage component relied on the old hat's unstackable
    // prototype. Melon's default64 would invalidate that saved component set.
    if(converted.has(DataComponents.MAX_DAMAGE)&&converted.getMaxStackSize()>1&&stack.getMaxStackSize()==1)
      converted.set(DataComponents.MAX_STACK_SIZE,1);
    return converted;
  }
  public static void migrateEquipment(ChaoEntity chao) {
    if(chao.level().isClientSide)return;
    for(var slot:EquipmentSlot.values()) {
      var old=chao.getItemBySlot(slot);
      if(old.getItem() instanceof LegacyMelonHatItem)chao.setItemSlot(slot,canonicalCopy(old));
    }
  }
  @SubscribeEvent public static void joined(EntityJoinLevelEvent event) {
    if(event.getLevel().isClientSide)return;
    if(event.getEntity() instanceof ItemEntity item&&item.getItem().getItem() instanceof LegacyMelonHatItem)
      item.setItem(canonicalCopy(item.getItem()));
    if(event.getEntity() instanceof ChaoEntity chao)migrateEquipment(chao);
  }
}
