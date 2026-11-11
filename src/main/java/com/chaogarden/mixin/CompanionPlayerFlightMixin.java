package com.chaogarden.mixin;

import com.chaogarden.companion.CompanionFlight;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Add a validated companion permit without moving or replacing the player's chest armor. */
@Mixin(Player.class)
public abstract class CompanionPlayerFlightMixin {
  @Inject(method="tryToStartFallFlying",at=@At("HEAD"),cancellable=true)
  private void chaoGarden$startCompanionFlight(CallbackInfoReturnable<Boolean> result){
    Player player=(Player)(Object)this;
    if(!player.isFallFlying()&&CompanionFlight.allowed(player)
        &&!player.getItemBySlot(EquipmentSlot.CHEST).canElytraFly(player)){
      player.startFallFlying();CompanionFlight.used(player);result.setReturnValue(true);
    }
  }
}
