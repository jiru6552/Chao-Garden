package com.chaogarden.mixin;

import com.chaogarden.companion.EquippedKnockback080;
import com.chaogarden.core.EquippedMovementRules080;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

/** Only the native expected-motion term changes, for a recent server-authored hit. */
@Mixin(ServerGamePacketListenerImpl.class)
public abstract class EquippedMovementValidationMixin080 {
  @Shadow public ServerPlayer player;
  @Shadow private int receivedMovePacketCount;
  @Shadow private int knownMovePacketCount;
  @WrapOperation(method="handleMovePlayer",at=@At(value="INVOKE",target="Lnet/minecraft/world/phys/Vec3;lengthSqr()D"))
  private double chaoGarden$expectedMotion080(Vec3 velocity,Operation<Double> original){
    // The native count increments immediately after this term is computed.
    // Its >5 overflow rule resets to1; do not turn packet floods into a budget.
    int burst=EquippedMovementRules080.nativeBurst(receivedMovePacketCount-knownMovePacketCount+1);
    double expected=EquippedKnockback080.movementExpected080(player,velocity,original.call(velocity),burst);
    return com.chaogarden.companion.EquippedRun084.movementExpected(player,velocity,expected,burst);
  }
}
