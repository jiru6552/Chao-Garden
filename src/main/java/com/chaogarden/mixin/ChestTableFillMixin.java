package com.chaogarden.mixin;

import com.chaogarden.distribution.ChestFillWitness;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.world.Container;
import net.minecraft.world.RandomizableContainer;
import net.minecraft.world.entity.vehicle.ContainerEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Covers real modded loot chests that leave their root loot table type generic. */
@Mixin(LootTable.class)
public abstract class ChestTableFillMixin {
  @Shadow private ObjectArrayList<ItemStack> getRandomItems(LootContext context){throw new AssertionError("Mixin shadow");}
  @Redirect(method="fill",at=@At(value="INVOKE",target="Lnet/minecraft/world/level/storage/loot/LootTable;getRandomItems(Lnet/minecraft/world/level/storage/loot/LootContext;)Lit/unimi/dsi/fastutil/objects/ObjectArrayList;"))
  private ObjectArrayList<ItemStack> chaoGarden$physicalChest(LootTable table,LootContext context,Container container,LootParams params,long seed) {
    if(!(container instanceof RandomizableContainer)&&!(container instanceof ContainerEntity))return getRandomItems(context);
    return ChestFillWitness.generate(table,context,()->getRandomItems(context));
  }
}
