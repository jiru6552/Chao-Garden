package com.chaogarden.companion;

import java.util.UUID;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

/** Shared eligibility used by vanilla flight hooks; clients only predict a server-issued permit. */
public final class CompanionFlight {
  private static UUID keeper;
  private static boolean active,elytra,boost;
  private static boolean finiteStarted;
  private static int remaining,cooldown,permitTicks,lastServerRemaining;
  private CompanionFlight() {}

  public static boolean environment(Player player) {
    return player.isAlive()&&!player.isSpectator()&&!player.onGround()&&!player.isInWater()
        &&!player.isPassenger()&&!player.getAbilities().flying&&!player.hasEffect(MobEffects.LEVITATION);
  }

  public static boolean allowed(LivingEntity entity) {
    if(!(entity instanceof Player player)||!environment(player))return false;
    if(player instanceof ServerPlayer server)return CompanionContent.flightEligible(server);
    return player.level().isClientSide&&player.getUUID().equals(keeper)&&permitTicks>0&&active&&(elytra||remaining>0);
  }

  public static void used(LivingEntity entity) {
    if(entity instanceof ServerPlayer player)CompanionContent.flightStarted(player);
    else if(entity instanceof Player player&&player.getUUID().equals(keeper)&&!elytra)finiteStarted=true;
  }

  public static void receive(UUID player,boolean enabled,boolean wing,boolean robot,int ticks,int delay) {
    int budget=Math.max(0,Math.min(242,ticks));
    boolean ongoing=player.equals(keeper)&&finiteStarted&&!wing;
    // A repeated older/full budget cannot restart local prediction midair.
    // A genuinely larger server budget still permits a newly trained helper.
    remaining=ongoing&&budget<=lastServerRemaining?Math.min(remaining,budget):budget;
    if(!player.equals(keeper))finiteStarted=false;
    keeper=player;active=enabled;elytra=enabled&&wing;boost=enabled&&robot;lastServerRemaining=budget;
    if(elytra)finiteStarted=false;
    cooldown=Math.max(0,Math.min(100,delay));permitTicks=40;
  }

  public static boolean canRequestBoost() {return permitTicks>0&&active&&boost&&cooldown==0;}
  public static void tickClient(Player player) {
    if(permitTicks>0)permitTicks--;if(cooldown>0)cooldown--;
    if(player==null||!player.getUUID().equals(keeper))return;
    if(player.onGround()||player.isInWater()){finiteStarted=false;return;}
    // Prediction never injects a slow-fall velocity. Normal elytra physics runs
    // while allowed(), and its usual update ends flight after this permit ends.
    if(!elytra&&finiteStarted&&remaining>0)remaining--;
  }
  public static void clear() {keeper=null;active=elytra=boost=finiteStarted=false;remaining=cooldown=permitTicks=lastServerRemaining=0;}
}
