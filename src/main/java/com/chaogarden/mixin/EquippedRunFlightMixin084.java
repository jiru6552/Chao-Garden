package com.chaogarden.mixin;

import com.chaogarden.companion.EquippedRun084;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.world.entity.player.Abilities;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Multiply flight input, never the saved abilities field or permission. */
@Mixin(Player.class)
public abstract class EquippedRunFlightMixin084 {
  @WrapOperation(method="getFlyingSpeed",at=@At(value="INVOKE",target="Lnet/minecraft/world/entity/player/Abilities;getFlyingSpeed()F"))
  private float chaoGarden$runFlight084(Abilities abilities,Operation<Float> original){
    return EquippedRun084.flightSpeed((Player)(Object)this,original.call(abilities));
  }
}
