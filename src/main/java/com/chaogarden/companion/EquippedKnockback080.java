package com.chaogarden.companion;

import com.chaogarden.ChaoGarden;
import com.chaogarden.core.EquippedMovementRules080;
import com.chaogarden.entity.ChaoEntity;
import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/** Narrow melee-call context, never a guessed last attacker or permanent attribute.
 * Actual impulse vectors are captured after vanilla resistance and event cancellation.
 * Only a short-lived out-of-packet-range impulse requires tracker corrections. */
@EventBusSubscriber(modid=ChaoGarden.MODID)
public final class EquippedKnockback080 {
  public static final double VANILLA_PACKET_LIMIT=3.9;
  public static final int TRACK_TICKS=80;
  private static final ThreadLocal<ArrayDeque<Scope>> SCOPES=ThreadLocal.withInitial(ArrayDeque::new);
  private static final Map<UUID,Impact> IMPACTS=new HashMap<>();
  private static final Map<UUID,MovementWindow> MOVEMENT=new HashMap<>();
  public record MovementCheck080(long tick,int burst,double nativeExpectedSquared,double authorizedExpectedSquared){}
  private static final class MovementWindow {
    final ServerPlayer victim;
    final net.minecraft.resources.ResourceKey<net.minecraft.world.level.Level> dimension;
    final EquippedMovementRules080.History history;
    MovementCheck080 lastCheck;
    MovementWindow(ServerPlayer player,Vec3 velocity){victim=player;dimension=player.level().dimension();history=new EquippedMovementRules080.History(now(player),velocity.lengthSqr());}
    boolean current(ServerPlayer player){return victim==player&&player.isAlive()&&!player.isRemoved()&&dimension.equals(player.level().dimension())&&history.active(now(player));}
  }
  private record ClientSeen(long sequence,long expires,net.minecraft.resources.ResourceLocation dimension,int entityId){}
  private static final Map<UUID,ClientSeen> CLIENT_SEEN=new HashMap<>();
  private static long sequence;
  public static final class Scope {
    final ChaoEntity attacker;
    final LivingEntity target;
    final DamageSource source;
    final Map<UUID,Impact> victims=new LinkedHashMap<>();
    Scope(ChaoEntity chao,LivingEntity victim,DamageSource damage){attacker=chao;target=victim;source=damage;}
  }
  private static final class Impact {
    final LivingEntity victim;
    final Vec3 vector;
    final long expires,order;
    final net.minecraft.resources.ResourceKey<net.minecraft.world.level.Level> dimension;
    boolean armed;
    Impact(LivingEntity entity,Vec3 motion,long until){victim=entity;vector=motion;expires=until;order=++sequence;dimension=entity.level().dimension();}
  }
  private EquippedKnockback080() {}
  private static long now(Entity entity){return ((ServerLevel)entity.level()).getServer().overworld().getGameTime();}
  private static boolean finite(Vec3 vector){return Double.isFinite(vector.x)&&Double.isFinite(vector.y)&&Double.isFinite(vector.z);}
  public static boolean outsidePacket(Vec3 vector){return Math.max(Math.abs(vector.x),Math.max(Math.abs(vector.y),Math.abs(vector.z)))>VANILLA_PACKET_LIMIT;}
  private static Scope scope(){return SCOPES.get().peekLast();}
  public static int scopeDepth(){return SCOPES.get().size();}

  /** Retained hook compatibility: this ability never augments player melee. */
  public static Scope begin(Player player) {
    return null;
  }
  /** Open only around an actual authorized companion's own direct mob hurt call.
   * Neither an owner, a saved marker, a projectile nor an unrelated nested
   * damage callback can borrow that companion's Power. */
  public static Scope beginMob(LivingEntity victim,DamageSource source) {
    if(!(victim.level() instanceof ServerLevel)||!source.is(DamageTypes.MOB_ATTACK)
        ||!(source.getEntity() instanceof ChaoEntity chao)||source.getDirectEntity()!=chao
        ||chao==victim||chao.level()!=victim.level()||!chao.isAlive()||chao.isRemoved()
        ||CompanionContent.secretPowerBonus084(chao)<=0)return null;
    var scope=new Scope(chao,victim,source);SCOPES.get().addLast(scope);return scope;
  }
  public static void finish(Scope scope,boolean completed) {
    if(scope==null)return;
    var stack=SCOPES.get();
    if(stack.peekLast()!=scope){SCOPES.remove();return;}
    stack.removeLast();if(stack.isEmpty())SCOPES.remove();
    for(var impact:scope.victims.values()){
      // A genuinely newer nested attack must never be overwritten by an older
      // outer call snapshot. A canceled/throwing attack cannot arm stale data.
      if(IMPACTS.get(impact.victim.getUUID())!=impact)continue;
      if(!completed||impact.victim.isRemoved()||!impact.dimension.equals(impact.victim.level().dimension())||!finite(impact.vector)||!outsidePacket(impact.vector)){
        IMPACTS.remove(impact.victim.getUUID(),impact);continue;
      }
      impact.armed=true;
      if(impact.victim instanceof ServerPlayer) {
        // Preserve the real already-computed native impulse for the victim,
        // its full-vector correction and native movement validation.
        impact.victim.setDeltaMovement(impact.vector);
        impact.victim.hasImpulse=true;
        var player=(ServerPlayer)impact.victim;var previous=MOVEMENT.get(player.getUUID());
        if(previous!=null&&previous.current(player))previous.history.authorize(now(player),impact.vector.lengthSqr());
        else MOVEMENT.put(player.getUUID(),new MovementWindow(player,impact.vector));
      }
      CompanionNetwork.sendImpulse080(impact.victim,impact.vector,++sequence);
    }
  }

  public static double hurtStrength(LivingEntity victim,DamageSource source,double strength) {
    var scope=scope();
    if(scope==null||scope.source!=source||scope.target!=victim
        ||source.getEntity()!=scope.attacker||source.getDirectEntity()!=scope.attacker
        ||!source.is(DamageTypes.MOB_ATTACK)||victim==scope.attacker)return strength;
    return scaled(scope,strength);
  }
  public static double attackStrength(Player attacker,LivingEntity victim,double strength) {
    return strength;
  }
  private static double scaled(Scope scope,double strength) {
    return com.chaogarden.core.EquippedAbilityRules080.strength(strength,CompanionContent.secretPowerBonus084(scope.attacker));
  }
  public static void afterKnockback(LivingEntity victim,Vec3 before,boolean boosted) {
    var scope=scope();if(!boosted||scope==null||victim.level().isClientSide||victim.isRemoved())return;
    Vec3 after=victim.getDeltaMovement();
    if(!finite(after)||after.equals(before))return; // Includes canceled / fully resisted calls.
    var impact=new Impact(victim,after,now(victim)+TRACK_TICKS);
    scope.victims.put(victim.getUUID(),impact);IMPACTS.put(victim.getUUID(),impact);
  }

  /** Invoked after native short-vector packets. Above the packet limit,
   * corrections include the victim; marked players then receive tracker-only
   * settling updates and a final current-vector/cache refresh within the
   * original80-tick window. No new impulse or world/player scan is installed. */
  public static Vec3 afterTracker(Entity entity) {
    if(entity instanceof ServerPlayer player){
      var movement=MOVEMENT.get(player.getUUID());
      if(movement!=null){
        if(!movement.current(player))MOVEMENT.remove(player.getUUID(),movement);
        else movement.history.sample(now(player),player.getDeltaMovement().lengthSqr());
      }
    }
    var impact=IMPACTS.get(entity.getUUID());if(impact==null||!impact.armed)return null;
    if(impact.victim!=entity||entity.isRemoved()||!(entity.level() instanceof ServerLevel)||!impact.dimension.equals(entity.level().dimension())
        ||!impact.victim.isAlive()||!finite(entity.getDeltaMovement())){
      IMPACTS.remove(entity.getUUID(),impact);return null;
    }
    Vec3 velocity=entity.getDeltaMovement();
    if(entity instanceof ServerPlayer){
      boolean terminal=now(entity)>=impact.expires||velocity.x==0&&velocity.z==0;
      if(terminal||!outsidePacket(velocity))CompanionNetwork.sendTrackedImpulse080(entity,velocity,++sequence);
      else CompanionNetwork.sendImpulse080(entity,velocity,++sequence);
      if(terminal)IMPACTS.remove(entity.getUUID(),impact);
      // The native PLAYER tracker does not normally track velocity deltas.
      // Keep its late-pairing cache current only for this bounded marked hit.
      return velocity;
    }
    if(now(entity)>=impact.expires||!outsidePacket(velocity)){IMPACTS.remove(entity.getUUID(),impact);return null;}
    CompanionNetwork.sendImpulse080(entity,velocity,++sequence);return null;
  }
  /** No movement or collision check is bypassed. The native speed check can
   * account for recent real impulses despite friction and batched client moves.
   * Only finish() above can create/renew this short, identity-bound window. */
  public static double movementExpected080(ServerPlayer player,Vec3 velocity,double ordinary,int burst){
    var movement=MOVEMENT.get(player.getUUID());if(movement==null)return ordinary;
    if(!movement.current(player)||!finite(velocity)||!Double.isFinite(ordinary)){
      MOVEMENT.remove(player.getUUID(),movement);return ordinary;
    }
    long tick=now(player);movement.history.sample(tick,velocity.lengthSqr());
    double expected=EquippedMovementRules080.expectedSquared(ordinary,movement.history.maximum(tick),burst);
    movement.lastCheck=new MovementCheck080(tick,EquippedMovementRules080.nativeBurst(burst),ordinary,expected);
    return expected;
  }
  /** Bounded read-only diagnostic of the actual native term, not an input API. */
  public static MovementCheck080 movementCheck080(ServerPlayer player){var movement=MOVEMENT.get(player.getUUID());return movement!=null&&movement.current(player)?movement.lastCheck:null;}
  /** The server supplies the full vector; a client cannot request an impulse.
   * Numeric entity ids alone are insufficient after unload/reuse or dimension travel. */
  public static boolean receive(Player local,CompanionNetwork.FullImpulse080 message) {
    if(local==null||!local.level().isClientSide||!local.level().dimension().location().equals(message.dimension())
        ||message.sequence()<=0||!finite(message.vector()))return false;
    Entity entity=local.level().getEntity(message.entityId());
    if(!(entity instanceof LivingEntity)||entity.isRemoved()||!entity.getUUID().equals(message.entityUuid()))return false;
    var seen=CLIENT_SEEN.get(message.entityUuid());
    if(seen!=null&&seen.dimension().equals(message.dimension())&&seen.entityId()==message.entityId()&&message.sequence()<=seen.sequence())return false;
    // RemotePlayer keeps a native interpolation target separate from delta.
    // Align it too, so an older clamped packet cannot pull this value back.
    var velocity=message.vector();entity.lerpMotion(velocity.x,velocity.y,velocity.z);
    entity.setDeltaMovement(velocity);entity.hasImpulse=true;
    CLIENT_SEEN.put(message.entityUuid(),new ClientSeen(message.sequence(),local.level().getGameTime()+100,message.dimension(),message.entityId()));
    return true;
  }
  public static void clientTick(Player local) {
    if(local.tickCount%20!=0)return;
    long time=local.level().getGameTime();var dimension=local.level().dimension().location();
    CLIENT_SEEN.values().removeIf(s->s.expires()<time||!s.dimension().equals(dimension));
  }
  public static void clearClient(){CLIENT_SEEN.clear();}
  @SubscribeEvent public static void cleanup(ServerTickEvent.Post event){
    if(event.getServer().getTickCount()%20!=0)return;
    long time=event.getServer().overworld().getGameTime();
    IMPACTS.values().removeIf(i->i.victim.isRemoved()||!i.victim.isAlive()||!i.dimension.equals(i.victim.level().dimension())||time>i.expires||!finite(i.victim.getDeltaMovement())
        ||!(i.victim instanceof ServerPlayer)&&!outsidePacket(i.victim.getDeltaMovement()));
    MOVEMENT.values().removeIf(m->!m.current(m.victim));
  }
  @SubscribeEvent public static void stop(ServerStoppingEvent event){IMPACTS.clear();MOVEMENT.clear();SCOPES.remove();sequence=0;}
}
