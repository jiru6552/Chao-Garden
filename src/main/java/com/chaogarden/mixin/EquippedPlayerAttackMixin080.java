package com.chaogarden.mixin;

import com.chaogarden.companion.EquippedKnockback080;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(Player.class)
public abstract class EquippedPlayerAttackMixin080 {
  @WrapMethod(method="attack")
  private void chaoGarden$attack080(Entity target,Operation<Void> original){
    var scope=EquippedKnockback080.begin((Player)(Object)this);boolean completed=false;
    try{original.call(target);completed=true;}
    finally{EquippedKnockback080.finish(scope,completed);}
  }
  // Retain the established hook/registration without augmenting player melee.
  // Both ordinary sprint/enchantment and sweep strengths remain vanilla.
  @WrapOperation(method="attack",at=@At(value="INVOKE",target="Lnet/minecraft/world/entity/LivingEntity;knockback(DDD)V"))
  private void chaoGarden$attackKnockback080(LivingEntity victim,double strength,double x,double z,Operation<Void> original){
    var before=victim.getDeltaMovement();double adjusted=EquippedKnockback080.attackStrength((Player)(Object)this,victim,strength);
    original.call(victim,adjusted,x,z);EquippedKnockback080.afterKnockback(victim,before,adjusted!=strength);
  }
}
