package com.chaogarden.mixin;

import com.chaogarden.companion.EquippedRun084;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** NeoForge1.21.1 travel's third direct move is its fall-flying branch;
 * water/lava travel, steering, stored delta and collision handling stay native. */
@Mixin(LivingEntity.class)
public abstract class EquippedRunGlideMixin084 {
  @WrapOperation(method="travel",at=@At(value="INVOKE",target="Lnet/minecraft/world/entity/LivingEntity;move(Lnet/minecraft/world/entity/MoverType;Lnet/minecraft/world/phys/Vec3;)V",ordinal=2))
  private void chaoGarden$runGlide084(LivingEntity entity,MoverType type,Vec3 movement,Operation<Void> original){
    original.call(entity,type,EquippedRun084.glideMove(entity,movement));
  }
}
