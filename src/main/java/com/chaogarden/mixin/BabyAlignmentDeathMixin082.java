package com.chaogarden.mixin;

import com.chaogarden.companion.CompanionContent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.neoforge.common.CommonHooks;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Observe the final cancellation result, including listeners registered after
 * our ordinary event handlers. Never cancel a death or change its rewards. */
@Mixin(value=CommonHooks.class,remap=false)
public abstract class BabyAlignmentDeathMixin082 {
  @Inject(method="onLivingDeath",at=@At("RETURN"))
  private static void chaoGarden$acceptedBabyAlignment082(LivingEntity victim,DamageSource source,CallbackInfoReturnable<Boolean> result){
    if(!result.getReturnValueZ())CompanionContent.acceptedBabyAlignmentDeath082(victim);
  }
}
