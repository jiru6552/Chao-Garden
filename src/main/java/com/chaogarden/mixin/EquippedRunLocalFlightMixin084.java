package com.chaogarden.mixin;

import com.chaogarden.companion.EquippedRun084;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.player.Abilities;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** The vertical Creative input uses the same multiplier as horizontal input. */
@Mixin(LocalPlayer.class)
public abstract class EquippedRunLocalFlightMixin084 {
  @WrapOperation(method="aiStep",at=@At(value="INVOKE",target="Lnet/minecraft/world/entity/player/Abilities;getFlyingSpeed()F"))
  private float chaoGarden$runVertical084(Abilities abilities,Operation<Float> original){
    return EquippedRun084.flightSpeed((LocalPlayer)(Object)this,original.call(abilities));
  }
}
