package com.chaogarden.mixin;

import com.chaogarden.companion.CompanionFlight;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Admit the server permit at the vanilla double-jump gate, before its chest-item check short-circuits. */
@Mixin(LocalPlayer.class)
public abstract class CompanionLocalPlayerFlightMixin {
  @Redirect(method="aiStep",at=@At(value="INVOKE",
      target="Lnet/minecraft/world/item/ItemStack;canElytraFly(Lnet/minecraft/world/entity/LivingEntity;)Z"))
  private boolean chaoGarden$allowCompanionLaunch(ItemStack chest,LivingEntity wearer) {
    return chest.canElytraFly(wearer)||CompanionFlight.allowed(wearer);
  }
}
