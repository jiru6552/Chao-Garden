package com.chaogarden.mixin;

import com.chaogarden.companion.EquippedKnockback080;
import net.minecraft.server.level.ServerEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerEntity.class)
public abstract class EquippedTrackerMotionMixin080 {
  @Shadow @Final private Entity entity;
  @Shadow private Vec3 lastSentMovement;
  @Inject(method="sendChanges",at=@At("RETURN"))
  private void chaoGarden$afterMotion080(CallbackInfo callback){
    var current=EquippedKnockback080.afterTracker(entity);
    if(current!=null)lastSentMovement=current;
  }
}
