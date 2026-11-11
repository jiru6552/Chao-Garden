package com.chaogarden.mixin;

import com.chaogarden.companion.CompanionFlight;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Keep vanilla elytra travel, animation, collision and steering for a companion permit. */
@Mixin(LivingEntity.class)
public abstract class CompanionLivingFlightMixin {
  @Redirect(method="updateFallFlying",at=@At(value="INVOKE",target="Lnet/minecraft/world/item/ItemStack;canElytraFly(Lnet/minecraft/world/entity/LivingEntity;)Z"))
  private boolean chaoGarden$canContinue(ItemStack chest,LivingEntity wearer){
    if(chest.canElytraFly(wearer))return true;
    boolean allowed=CompanionFlight.allowed(wearer);if(allowed)CompanionFlight.used(wearer);return allowed;
  }
  @Redirect(method="updateFallFlying",at=@At(value="INVOKE",target="Lnet/minecraft/world/item/ItemStack;elytraFlightTick(Lnet/minecraft/world/entity/LivingEntity;I)Z"))
  private boolean chaoGarden$flightTick(ItemStack chest,LivingEntity wearer,int ticks){
    return chest.canElytraFly(wearer)?chest.elytraFlightTick(wearer,ticks):CompanionFlight.allowed(wearer);
  }
}
