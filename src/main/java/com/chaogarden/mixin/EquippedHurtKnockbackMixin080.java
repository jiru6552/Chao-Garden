package com.chaogarden.mixin;

import com.chaogarden.companion.EquippedKnockback080;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(LivingEntity.class)
public abstract class EquippedHurtKnockbackMixin080 {
  @WrapMethod(method="hurt")
  private boolean chaoGarden$mobAttack080(DamageSource source,float amount,Operation<Boolean> original){
    var scope=EquippedKnockback080.beginMob((LivingEntity)(Object)this,source);boolean accepted=false;
    try{accepted=original.call(source,amount);return accepted;}
    finally{EquippedKnockback080.finish(scope,accepted);}
  }
  @WrapOperation(method="hurt",at=@At(value="INVOKE",target="Lnet/minecraft/world/entity/LivingEntity;knockback(DDD)V"))
  private void chaoGarden$hurtKnockback080(LivingEntity victim,double strength,double x,double z,Operation<Void> original,
      @Local(argsOnly=true) DamageSource source){
    var before=victim.getDeltaMovement();double adjusted=EquippedKnockback080.hurtStrength(victim,source,strength);
    original.call(victim,adjusted,x,z);EquippedKnockback080.afterKnockback(victim,before,adjusted!=strength);
  }
}
