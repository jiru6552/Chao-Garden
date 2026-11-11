package com.chaogarden.entity;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.Level;

/** Publishes the actual destination before the Chao commits a real inventory transfer. */
final class ChaoItemDrops {
  private ChaoItemDrops() {}

  static boolean publish(ItemEntity original, ChaoEntity chao, boolean thrown) {
    var stack = original.getItem();
    var item = stack.getItem();
    Entity destination = original;
    if (item.hasCustomEntity(stack)) {
      // NeoForge's normal join hook cancels the vanilla entity and queues a second spawn.
      // Resolve the item factory here so we observe acceptance/cancellation of the final entity.
      var replacement = item.createEntity(original.level(), original, stack);
      if (replacement != null) destination = replacement;
      if (destination.getClass() == ItemEntity.class) {
        // A factory can decline or return a configured vanilla item. NeoForge checks
        // the exact class, so keep its complete state without dispatching the factory twice.
        var resolved = new ResolvedVanillaItem(original.level());
        resolved.load(destination.saveWithoutId(new CompoundTag()));
        destination = resolved;
      }
    }
    if (original.getPersistentData().contains("ChaoRejectedUntil")) {
      destination.getPersistentData().putLong("ChaoRejectedUntil",
          original.getPersistentData().getLong("ChaoRejectedUntil"));
    }
    if (thrown) {
      destination.setDeltaMovement(original.getDeltaMovement());
      if (destination instanceof ItemEntity drop) {
        drop.setThrower(chao);
        if (!drop.hasPickUpDelay()) drop.setPickUpDelay(20);
      }
    }
    return original.level().addFreshEntity(destination);
  }

  private static final class ResolvedVanillaItem extends ItemEntity {
    ResolvedVanillaItem(Level level) { super(EntityType.ITEM, level); }
  }
}
