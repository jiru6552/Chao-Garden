package com.chaogarden.companion;

import com.chaogarden.core.EquippedMovementRules080;
import com.chaogarden.core.EquippedRunRules084;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

/** Run predicts from the exact server-synchronized modifier on clients, while
 * the server always rechecks its real current accessory and locked identity. */
public final class EquippedRun084 {
  public record MovementCheck(long tick,int burst,String mode,double multiplier,double speedEnvelope,double ordinary,double authorized) {}
  private static final Map<ServerPlayer,MovementCheck> CHECKS=new WeakHashMap<>();
  private EquippedRun084() {}
  public static double multiplier(Player player) {
    if(player instanceof ServerPlayer server)return EquippedRunRules084.multiplier(CompanionContent.secretRunBonus084(server));
    if(!player.level().isClientSide)return 1;
    var attribute=player.getAttribute(Attributes.MOVEMENT_SPEED);
    var modifier=attribute==null?null:attribute.getModifier(CompanionContent.RUN_SPEED);
    return modifier==null?1:EquippedRunRules084.multiplier(modifier.amount());
  }
  public static float flightSpeed(Player player,float ordinary) {
    return player.getAbilities().flying&&!player.isPassenger()
        ?EquippedRunRules084.flightSpeed(ordinary,multiplier(player)):ordinary;
  }
  /** Only the fall-flying travel move is wrapped. The delta stored by vanilla
   * remains aerodynamic speed, so repeated ticks never compound the bonus. */
  public static Vec3 glideMove(LivingEntity entity,Vec3 ordinary) {
    if(!(entity instanceof Player player)||!player.isFallFlying()||player.isPassenger()||player.getAbilities().flying)return ordinary;
    double factor=multiplier(player);
    return factor==1?ordinary:ordinary.scale(factor);
  }
  public static double movementExpected(ServerPlayer player,Vec3 nativeVelocity,double ordinary,int burst) {
    double factor=multiplier(player);
    if(factor<=1||player.isPassenger()||!Double.isFinite(ordinary)||ordinary<0){CHECKS.remove(player);return ordinary;}
    double envelope;String mode;
    if(player.getAbilities().flying){
      mode="creative";envelope=EquippedRunRules084.creativeEnvelope(player.getAbilities().getFlyingSpeed(),factor);
    }else if(player.isFallFlying()){
      mode="glide";envelope=EquippedRunRules084.glideEnvelope(nativeVelocity.length(),player.getAttributeValue(Attributes.GRAVITY),factor);
    }else{
      mode="ground_air";var at=player.getBlockPosBelowThatAffectsMyMovement();
      if(!player.level().hasChunkAt(at)){CHECKS.remove(player);return ordinary;}
      var block=player.level().getBlockState(at);
      envelope=EquippedRunRules084.groundEnvelope(player.getAttributeValue(Attributes.MOVEMENT_SPEED),block.getFriction(player.level(),at,player));
    }
    double authorized=EquippedMovementRules080.expectedSquared(ordinary,envelope*envelope,burst);
    CHECKS.put(player,new MovementCheck(player.server.getTickCount(),EquippedMovementRules080.nativeBurst(burst),mode,factor,envelope,ordinary,authorized));
    return authorized;
  }
  public static MovementCheck movementCheck(ServerPlayer player){return multiplier(player)>1?CHECKS.get(player):null;}
}
