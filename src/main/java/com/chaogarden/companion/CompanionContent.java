package com.chaogarden.companion;

import com.chaogarden.ChaoGarden;
import com.chaogarden.activities.ActivityManager;
import com.chaogarden.core.ChaoState;
import com.chaogarden.entity.ChaoEntity;
import com.chaogarden.item.ChaoCarrierItem;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.living.EnderManAngerEvent;
import net.neoforged.neoforge.event.entity.living.MobEffectEvent;
import net.neoforged.neoforge.common.NeoForgeMod;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/** Inventory-owned companions. Temporary world bodies are never a second saved Chao. */
@EventBusSubscriber(modid = ChaoGarden.MODID)
public final class CompanionContent {
  public static final String MARKER = "ChaoCompanion";
  public static final String TOKEN = "CompanionToken";
  public static final String DISABLED_UNTIL = "CompanionDisabledUntil";
  public static final String BOOST_UNTIL = "CompanionBoostUntil";
  public static final String PLAYER_BOOST_UNTIL = "ChaoCompanionBoostUntil";
  public static final String AIR_USED = "ChaoCompanionGlideUsed";
  public static final String AIR_STARTED = "ChaoCompanionGlideStarted";
  private static final String CHEST_OPEN_UNTIL = "ChaoCompanionEnderChestUntil";
  public static final ResourceLocation RUN_SPEED=ResourceLocation.fromNamespaceAndPath("chao_garden","companion_run");
  public static final ResourceLocation SWIM_SPEED=ResourceLocation.fromNamespaceAndPath("chao_garden","companion_swim");
  private static final String OWN_VISION="ChaoCompanionWaterVision",OWN_BREATH="ChaoCompanionWaterBreath";
  private static MobEffectInstance writingWaterEffect;
  private static final Map<UUID, Active> ACTIVE = new HashMap<>();
  private static final List<PendingSnapshot> PENDING = new ArrayList<>();

  private static final class Active {
    final UUID keeper, token;
    ItemStack stack;
    Object slotReference;
    ChaoEntity body;
    long lastSeen;
    int attackCooldown;
    MobEffectInstance vision,breath;
    boolean flightUsed;
    CompanionNetwork.Status status;
    long statusSent;
    Active(ServerPlayer player, ItemStack stack, UUID token, Object reference) {
      keeper = player.getUUID(); this.stack = stack; this.token = token; slotReference = reference;
    }
  }

  private record PendingSnapshot(UUID keeper, UUID token, CompoundTag entity, long expires, long disabledUntil) {}

  private CompanionContent() {}

  /** Held Chao retain the legacy registry ID for lossless saved-item compatibility. */
  public static void bootstrap() {}

  public static void register(IEventBus bus) {
    bus.addListener((FMLCommonSetupEvent event) -> event.enqueueWork(AccessoriesBridge::register));
    CompanionNetwork.register(bus);
  }

  public static boolean isManaged(ChaoEntity chao) {
    return chao.getPersistentData().contains(MARKER);
  }

  public static boolean isAdult(ItemStack stack) {
    return ChaoCarrierItem.filled(stack) && CompanionRules.adult(ChaoCarrierItem.state(stack));
  }

  public static boolean isEquippable(ItemStack stack) {
    return ChaoCarrierItem.filled(stack) && CompanionRules.equippable(ChaoCarrierItem.state(stack));
  }

  public static boolean canEquip(ItemStack stack, Player player) {
    if (!isEquippable(stack) || !ChaoCarrierItem.canRelease(stack, player)) return false;
    UUID token = token(stack, false);
    for (ItemStack equipped : AccessoriesBridge.equipped(player)) {
      if (equipped == stack) continue;
      UUID other = token(equipped, false);
      // A copied view of this same slot is harmless; a distinct companion is not.
      if (ChaoCarrierItem.filled(equipped) && (token == null || !token.equals(other))) return false;
    }
    if (!player.level().isClientSide && player instanceof ServerPlayer serverPlayer) {
      UUID chaoId = ChaoCarrierItem.storedUUID(stack);
      Entity existing = findEntity(serverPlayer.server, chaoId);
      if (existing != null && (!(existing instanceof ChaoEntity chao) || !isManaged(chao))) return false;
      for (Active active : ACTIVE.values())
        if (token != null && token.equals(active.token) && !active.keeper.equals(player.getUUID())) return false;
    }
    return true;
  }

  public static InteractionResult onInteractChao(ChaoEntity chao, Player player, InteractionHand hand) {
    if (!player.isShiftKeyDown() || !player.getItemInHand(hand).isEmpty()) return InteractionResult.PASS;
    if (!chao.canCare(player) || isManaged(chao) || ActivityManager.isBusy(chao) || chao.isCocooned()) {
      if (!player.level().isClientSide) player.displayClientMessage(Component.literal(
          isManaged(chao) ? "Unequip this companion to hold it."
              : "This Chao cannot be picked up right now."), true);
      return InteractionResult.FAIL;
    }
    if (player.level().isClientSide) return InteractionResult.SUCCESS;
    ItemStack carrier = ChaoGarden.item("chao_carrier").getDefaultInstance();
    if (!ChaoCarrierItem.capture(carrier, chao, player)) return InteractionResult.FAIL;
    player.setItemInHand(hand, carrier);
    return InteractionResult.CONSUME;
  }

  public static UUID token(ItemStack stack, boolean create) {
    CompoundTag data = ChaoCarrierItem.data(stack);
    if (data.hasUUID(TOKEN)) return data.getUUID(TOKEN);
    if (!create) return null;
    UUID token = UUID.randomUUID(); data.putUUID(TOKEN, token); ChaoCarrierItem.setData(stack, data);
    return token;
  }

  public static long remaining(ItemStack stack, MinecraftServer server) {
    return Math.max(0, ChaoCarrierItem.data(stack).getLong(DISABLED_UNTIL) - now(server));
  }

  public static boolean itemIsSummoned(ItemStack stack) {
    UUID token = token(stack, false);
    return token != null && ACTIVE.values().stream().anyMatch(a -> a.token.equals(token) && a.body != null);
  }

  private static long now(MinecraftServer server) { return server.overworld().getGameTime(); }

  public static Entity findEntity(MinecraftServer server, UUID uuid) {
    if (uuid == null) return null;
    for (ServerLevel level : server.getAllLevels()) {
      Entity entity = level.getEntity(uuid);
      if (entity != null && !entity.isRemoved()) return entity;
    }
    return null;
  }

  public static void accessoryTick(ServerPlayer player, ItemStack stack, Object reference) {
    if (!player.isAlive() || !isEquippable(stack) || !ChaoCarrierItem.canRelease(stack, player)) return;
    UUID token = token(stack, true);
    // Once the same creature is equipped again, its current item/body owns the
    // state. An earlier removal snapshot must never overwrite newer training.
    PENDING.removeIf(pending -> {
      if (!pending.keeper().equals(player.getUUID()) || !pending.token().equals(token)) return false;
      // A defeat that followed a slot split still belongs to this item, even
      // if it was equipped again before the normal pending-destination pass.
      if (pending.disabledUntil() > 0) restorePending(stack, pending);
      return true;
    });
    Active active = ACTIVE.get(player.getUUID());
    if (active != null && !active.token.equals(token)) { recall(player, active); ACTIVE.remove(player.getUUID()); active = null; }
    if (active == null) {
      if (ACTIVE.values().stream().anyMatch(a -> a.token.equals(token))) return;
      active = new Active(player, stack, token, reference); ACTIVE.put(player.getUUID(), active);
    }
    active.stack = stack; active.slotReference = reference; active.lastSeen = now(player.server);
    if (active.body != null && (active.body.isRemoved() || active.body.level() != player.level())) {
      recall(player, active); active.body = null;
    }
    if (remaining(stack, player.server) > 0) return;
    if (active.body == null) summon(player, active);
  }

  private static void summon(ServerPlayer player, Active active) {
    UUID id = ChaoCarrierItem.storedUUID(active.stack);
    if (findEntity(player.server, id) != null) return; // A copied carrier never duplicates a living Chao.
    ChaoEntity body = ChaoGarden.CHAO.get().create(player.serverLevel());
    if (body == null) return;
    body.load(ChaoCarrierItem.entityData(active.stack));
    body.owner = player.getUUID();
    body.setNoAi(false);
    CompoundTag marker = new CompoundTag(); marker.putUUID("Keeper", active.keeper); marker.putUUID("Token", active.token);
    body.getPersistentData().put(MARKER, marker);
    body.state().sleeping = false;
    body.getAttribute(Attributes.MAX_HEALTH).setBaseValue(16 + body.state().points[ChaoState.STAMINA] / 250.0);
    body.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(body.isInWater()
        ? com.chaogarden.core.LocomotionRules.waterMovementSpeed(body.state())
        : com.chaogarden.core.LocomotionRules.movementSpeed(body.state()));
    body.getAttribute(Attributes.ARMOR).setBaseValue(Math.min(12, body.state().points[ChaoState.POWER] / 250.0));
    body.setHealth(Math.max(1, Math.min(body.getHealth(), body.getMaxHealth())));
    if (!placeNear(body, player) || !player.serverLevel().addFreshEntity(body)) return;
    active.body = body; body.sync();
    AccessoriesBridge.changed(active.slotReference);
  }

  private static boolean placeNear(ChaoEntity body, ServerPlayer player) {
    for (int[] offset : new int[][] {{1,0,1},{-1,0,1},{1,0,-1},{-1,0,-1},{0,1,0},{0,0,0}}) {
      body.moveTo(player.getX()+offset[0], player.getY()+offset[1], player.getZ()+offset[2], player.getYRot(), 0);
      BlockPos position = body.blockPosition();
      if (player.serverLevel().getWorldBorder().isWithinBounds(position)
          && player.serverLevel().noCollision(body)) return true;
    }
    return false;
  }

  /** Only the summoned body runs care. Its independent clock travels in the carrier snapshot. */
  public static boolean tickChao(ChaoEntity chao) {
    if (!isManaged(chao)) return false;
    Active active = active(chao);
    if (active == null) { chao.discard(); return true; }
    if(CarryPreview.active(chao))return true;
    var data = chao.getPersistentData();
    int careTick = Math.clamp(data.getInt("ChaoEquippedCareTicks"), 0, 5999) + 1;
    chao.state().tickEquippedCare(careTick, com.chaogarden.GardenConfig.CHILD_TICKS.get());
    data.putInt("ChaoEquippedCareTicks", careTick % 6000);
    if (chao.tickCount % 20 == 0) {
      chao.sync(); ChaoCarrierItem.snapshot(active.stack, chao); AccessoriesBridge.changed(active.slotReference);
    }
    return true;
  }

  private static Active active(ChaoEntity chao) {
    for (Active active : ACTIVE.values()) if (active.body == chao) return active;
    return null;
  }

  /** Real offered items cannot wait for the periodic projection snapshot. */
  public static void itemStateChanged(ChaoEntity chao) {
    Active active=active(chao);
    if (active==null || active.stack.isEmpty()) return;
    ChaoCarrierItem.snapshot(active.stack,chao);
    AccessoriesBridge.changed(active.slotReference);
  }

  /** Navigation and combat run as a highest-priority movement goal. */
  public static void drive(ChaoEntity chao) {
    Active active = active(chao);
    if (active == null || !(chao.level() instanceof ServerLevel level)) return;
    ServerPlayer keeper = level.getServer().getPlayerList().getPlayer(active.keeper);
    if (keeper == null || keeper.level() != level || !keeper.isAlive()) {
      chao.setCompanionGrip(null,false);chao.getNavigation().stop();return;
    }
    if(!current(keeper,active)){
      chao.setCompanionGrip(null,false);chao.getNavigation().stop();chao.setTarget(null);chao.setNoGravity(false);return;
    }
    if(chao.isCompanionAttached()) {
      chao.getNavigation().stop();chao.setTarget(null);chao.setNoGravity(true);
      chao.setDeltaMovement(Vec3.ZERO);chao.setPos(keeper.getX(),keeper.getY()+(chao.companionCarryPosition()==CarryRules.HEAD?keeper.getBbHeight():.35),keeper.getZ());
      chao.setYRot(keeper.getYRot());chao.yBodyRot=keeper.yBodyRot;chao.yHeadRot=keeper.yHeadRot;
      return;
    }
    // A pending gift retains its real held-item transaction. Flight attachment
    // above takes priority so carrying food cannot detach the body mid-glide.
    if (chao.isHandlingItem()) {
      chao.setCompanionGrip(null,false);chao.getNavigation().stop();chao.setTarget(null);chao.setNoGravity(false);return;
    }
    if (active.attackCooldown > 0) active.attackCooldown--;
    ChaoState state = chao.state();
    LivingEntity target = !CarryPreview.active(chao) && state.adult && state.energy > 5
        ? target(keeper) : null;
    if (target != null && chao.distanceToSqr(target) < 256 && keeper.distanceToSqr(target) < 400) {
      chao.setNoGravity(false); chao.setTarget(target); chao.getLookControl().setLookAt(target, 30, 30);
      chao.getNavigation().moveTo(target, 1.2);
      if (chao.distanceToSqr(target) < 2.8 && active.attackCooldown == 0) {
        boolean hit = target.hurt(chao.damageSources().mobAttack(chao), CompanionRules.powerDamage(state.levels[ChaoState.POWER]));
        chao.swing(InteractionHand.MAIN_HAND);
        active.attackCooldown = CompanionRules.attackCooldown(state.levels[ChaoState.RUN]);
        if (hit) { state.train(ChaoState.POWER, 3); state.energy = Math.max(0, state.energy-1); }
      }
      return;
    }
    chao.setTarget(null);
    if (chao.distanceToSqr(keeper) > 576) { placeNear(chao, keeper); chao.getNavigation().stop(); }
    else if (!keeper.onGround() && !keeper.isInWater()) {
      chao.setNoGravity(true); chao.getNavigation().stop();
      Vec3 delta = keeper.position().add(1, .4, .6).subtract(chao.position());
      chao.setDeltaMovement(delta.lengthSqr() > .01 ? delta.normalize().scale(Math.min(.35, delta.length()*.12)) : Vec3.ZERO);
    } else {
      chao.setNoGravity(false);
      if (chao.distanceToSqr(keeper) > 6.25) chao.getNavigation().moveTo(keeper, 1.1);
      else chao.getNavigation().stop();
    }
    chao.getLookControl().setLookAt(keeper, 25, 25);
    // Rest beside a stationary keeper. Walking on solid ground used to refill energy even in transit.
    if (chao.tickCount % 40 == 0 && keeper.onGround() && chao.distanceToSqr(keeper) <= 6.25
        && keeper.walkDist == keeper.walkDistO && chao.onGround() && chao.getNavigation().isDone()
        && (state.energy < 20 || keeper.isCrouching())) state.energy = Math.min(100, state.energy+1);
  }

  private static LivingEntity target(ServerPlayer player) {
    LivingEntity[] choices = {player.getLastHurtMob(), player.getLastHurtByMob()};
    int[] ages = {player.tickCount-player.getLastHurtMobTimestamp(),
                  player.tickCount-player.getLastHurtByMobTimestamp()};
    for (int i = 0; i < choices.length; i++) {
      LivingEntity entity = choices[i];
      if (ages[i] > 200) continue;
      if (!(entity instanceof Mob mob) || !entity.isAlive() || entity instanceof ChaoEntity || entity.level()!=player.level()) continue;
      if (entity instanceof TamableAnimal tame && player.getUUID().equals(tame.getOwnerUUID())) continue;
      if (entity instanceof Enemy || mob.getTarget() == player || player.getLastHurtMob() == entity) return entity;
    }
    return null;
  }

  public static void unequip(ServerPlayer player, ItemStack stack) {
    Active active = ACTIVE.get(player.getUUID());
    if (active != null && active.token.equals(token(stack, false))) { recall(player, active); ACTIVE.remove(player.getUUID()); }
  }

  /** Accessories asks before its slot is split/copied into the menu cursor. */
  public static void beforeUnequip(ServerPlayer player, ItemStack stack) {
    Active active = ACTIVE.get(player.getUUID());
    if (active == null || !active.token.equals(token(stack, false))
        || active.body == null || active.body.isRemoved()) return;
    ChaoCarrierItem.snapshot(stack, active.body);
    if (active.stack != stack && !active.stack.isEmpty()) ChaoCarrierItem.snapshot(active.stack, active.body);
    AccessoriesBridge.changed(active.slotReference);
  }

  private static void recall(ServerPlayer player, Active active) {
    clearBenefits(player,active);
    if (active.body != null && !active.body.isRemoved()) {
      // Container.removeItem splits the one-item source to count zero. Its
      // components are then unreadable through ItemStack#get, so serialize the
      // live body independently of that source and find the actual destination.
      ItemStack snapshot = ChaoGarden.item("chao_carrier").getDefaultInstance();
      ChaoCarrierItem.snapshot(snapshot, active.body);
      CompoundTag entity = ChaoCarrierItem.entityData(snapshot);
      if (!active.stack.isEmpty()) ChaoCarrierItem.storeEntity(active.stack, entity);
      active.body.discard();
      PENDING.add(new PendingSnapshot(active.keeper, active.token, entity, now(player.server)+10, 0));
    }
    active.body = null; AccessoriesBridge.changed(active.slotReference);
  }

  @SubscribeEvent(priority = EventPriority.HIGHEST)
  public static void death(LivingDeathEvent event) {
    if (event.getEntity() instanceof ChaoEntity chao && isManaged(chao)) {
      event.setCanceled(true);
      Active active = active(chao);
      if (active == null) { chao.discard(); return; }
      if(chao.state().adult) CompanionRules.defeat(chao.state(), new java.util.Random(chao.getRandom().nextLong()));
      chao.setHealth(chao.getMaxHealth()); chao.sync();
      // The slot can have been split into a cursor stack earlier this tick.
      // Serialize independently, so a count-zero source cannot erase defeat
      // state or the recovery timer before Accessories sends onUnequip.
      MinecraftServer server = ((ServerLevel)chao.level()).getServer();
      long time = now(server), disabledUntil = time + CompanionRules.RECOVERY_TICKS;
      ItemStack snapshot = ChaoGarden.item("chao_carrier").getDefaultInstance();
      ChaoCarrierItem.snapshot(snapshot, chao);
      PendingSnapshot defeated = new PendingSnapshot(active.keeper, active.token,
          ChaoCarrierItem.entityData(snapshot), time+10, disabledUntil);
      PENDING.removeIf(pending -> pending.keeper().equals(active.keeper) && pending.token().equals(active.token));
      if (!restorePending(active.stack, defeated)) {
        PENDING.add(defeated);
        ACTIVE.remove(active.keeper);
      }
      ((ServerLevel)chao.level()).sendParticles(ParticleTypes.CLOUD, chao.getX(), chao.getY()+.5, chao.getZ(), 15, .25,.3,.25,.02);
      chao.discard(); active.body = null; AccessoriesBridge.changed(active.slotReference);
      ServerPlayer player = ((ServerLevel)chao.level()).getServer().getPlayerList().getPlayer(active.keeper);
      if (player != null) {clearBenefits(player,active);player.displayClientMessage(Component.literal("Your Chao is recovering for one minute."), true);}
    } else if (event.getEntity() instanceof ServerPlayer player) {
      Active active = ACTIVE.remove(player.getUUID()); if (active != null) recall(player, active);
    }
  }

  @SubscribeEvent
  public static void staleBody(EntityJoinLevelEvent event) {
    if (!event.getLevel().isClientSide && event.loadedFromDisk()
        && event.getEntity() instanceof ChaoEntity chao && isManaged(chao)) event.setCanceled(true);
  }

  @SubscribeEvent
  public static void serverTick(ServerTickEvent.Post event) {
    MinecraftServer server = event.getServer(); long time = now(server);
    for(var player:server.getPlayerList().getPlayers()){
      if(player.onGround()||player.isInWater()){
        player.getPersistentData().putInt(AIR_USED,0);player.getPersistentData().remove(AIR_STARTED);
      }else updateGlideClock(player);
    }
    Iterator<Active> iterator = ACTIVE.values().iterator();
    while (iterator.hasNext()) {
      Active active = iterator.next(); ServerPlayer player = server.getPlayerList().getPlayer(active.keeper);
      if (player == null || active.lastSeen < time-2) {
        if (player != null) recall(player, active); else if (active.body != null) active.body.discard();
        iterator.remove(); continue;
      }
      if (active.body != null && player.isAlive()) benefits(player,active);
      else clearBenefits(player,active);
    }
    Iterator<PendingSnapshot> snapshots = PENDING.iterator();
    while (snapshots.hasNext()) {
      PendingSnapshot pending = snapshots.next(); ServerPlayer player = server.getPlayerList().getPlayer(pending.keeper());
      boolean resolved = false;
      if (player != null) {
        Active current = ACTIVE.get(player.getUUID());
        if (current != null && current.token.equals(pending.token())) {
          snapshots.remove(); continue;
        }
        resolved = restorePending(player.containerMenu.getCarried(), pending);
        for (var slot : player.containerMenu.slots) resolved |= restorePending(slot.getItem(), pending);
        for (int i = 0; i < player.getInventory().getContainerSize(); i++)
          resolved |= restorePending(player.getInventory().getItem(i), pending);
        if (resolved) { player.getInventory().setChanged(); player.containerMenu.broadcastChanges(); }
      }
      if (resolved || time >= pending.expires()) snapshots.remove();
    }
  }

  private static boolean restorePending(ItemStack stack, PendingSnapshot pending) {
    if (stack.isEmpty() || !ChaoCarrierItem.filled(stack) || !pending.token().equals(token(stack, false))) return false;
    ChaoCarrierItem.storeEntity(stack, pending.entity());
    if (pending.disabledUntil() > 0) {
      CompoundTag data = ChaoCarrierItem.data(stack);
      data.putLong(DISABLED_UNTIL, pending.disabledUntil());
      ChaoCarrierItem.setData(stack, data);
    }
    return true;
  }

  private static boolean current(ServerPlayer player,Active active) {
    if(!player.isAlive()||active==null||active.body==null||active.body.isRemoved()||!active.body.isAlive()
        ||active.body.level()!=player.level()||!player.getUUID().equals(active.body.owner)
        ||active.lastSeen<now(player.server)-2
        ||!CompanionRules.equippable(active.body.state()))return false;
    for(var stack:AccessoriesBridge.equipped(player))if(!stack.isEmpty()&&active.token.equals(token(stack,false))
        &&active.body.getUUID().equals(ChaoCarrierItem.storedUUID(stack))&&ChaoCarrierItem.canRelease(stack,player)){
      // The real slot can already contain a replacement/cursor copy while the
      // previous one-item source has countzero. Never read or write that stale source.
      active.stack=stack;return remaining(stack,player.server)==0;
    }
    return false;
  }

  /** Special traits come only from the current authenticated Accessories body. */
  public static boolean hasVariantCompanion(ServerPlayer player,String variant) {
    Active active=ACTIVE.get(player.getUUID());
    return current(player,active)&&CompanionRules.adult(active.body.state())&&variant.equals(active.body.state().variant);
  }

  @SubscribeEvent(priority=EventPriority.HIGHEST)
  public static void fireProtection(LivingIncomingDamageEvent event) {
    if(!event.getSource().is(DamageTypeTags.IS_FIRE))return;
    if(event.getEntity() instanceof ServerPlayer player&&hasVariantCompanion(player,"nether")) {
      event.setCanceled(true);player.clearFire();
    }else if(event.getEntity() instanceof ChaoEntity chao&&chao.level() instanceof ServerLevel level) {
      Active active=active(chao);
      ServerPlayer keeper=active==null?null:level.getServer().getPlayerList().getPlayer(active.keeper);
      if(keeper!=null&&current(keeper,active)&&"nether".equals(chao.state().variant)) {
        event.setCanceled(true);chao.clearFire();
      }
    }
  }

  /** NeoForge's gaze-only hook leaves attack retaliation unchanged. */
  @SubscribeEvent(priority=EventPriority.HIGHEST)
  public static void suppressEnderGaze(EnderManAngerEvent event) {
    if(event.getPlayer() instanceof ServerPlayer player&&hasVariantCompanion(player,"end"))event.setCanceled(true);
  }

  @SubscribeEvent(priority=EventPriority.LOWEST)
  public static void rejectLevitation(MobEffectEvent.Applicable event) {
    if(event.getEntity() instanceof ServerPlayer player&&event.getEffectInstance().getEffect().equals(MobEffects.LEVITATION)
        &&hasVariantCompanion(player,"end"))event.setResult(MobEffectEvent.Applicable.Result.DO_NOT_APPLY);
  }

  /** The packet supplies no owner, item or inventory. Vanilla owns this saved inventory. */
  public static boolean requestEnderChest(ServerPlayer player) {
    if(!hasVariantCompanion(player,"end")||player.containerMenu!=player.inventoryMenu
        ||!player.inventoryMenu.getCarried().isEmpty()
        ||now(player.server)<player.getPersistentData().getLong(CHEST_OPEN_UNTIL))return false;
    player.getPersistentData().putLong(CHEST_OPEN_UNTIL,now(player.server)+10);
    var chest=player.getEnderChestInventory();
    chest.setActiveChest(null);
    return player.openMenu(new SimpleMenuProvider((id,inventory,keeper)->
        new ChestMenu(MenuType.GENERIC_9x3,id,inventory,chest,3) {
          @Override public boolean stillValid(Player viewer) {
            return viewer==player&&hasVariantCompanion(player,"end")&&super.stillValid(viewer);
          }
        },Component.translatable("container.enderchest"))).isPresent();
  }

  public static boolean flightEligible(ServerPlayer player) {
    Active active=ACTIVE.get(player.getUUID());
    return current(player,active)&&CompanionRules.adult(active.body.state())&&active.body.state().energy>5
        &&(CompanionRules.canElytra(active.body.state())||glideRemaining(player,active.body.state())>0);
  }
  public static void flightStarted(ServerPlayer player) {
    Active active=ACTIVE.get(player.getUUID());
    if(!current(player,active)||!CompanionRules.adult(active.body.state())||active.body.state().energy<=5)return;
    active.flightUsed=true;
    // Both vanilla hooks can visit this every tick. Only the first accepted
    // launch establishes the deadline; toggling flight or slots cannot renew it.
    if(!CompanionRules.canElytra(active.body.state())) {
      var data=player.getPersistentData();
      if(!data.contains(AIR_STARTED))data.putLong(AIR_STARTED,now(player.server));
      updateGlideClock(player);
    }
  }
  public static int glideRemaining(ServerPlayer player,ChaoState state) {
    updateGlideClock(player);
    return Math.max(0,CompanionRules.glideTicks(state.levels[ChaoState.FLY])-Math.max(0,player.getPersistentData().getInt(AIR_USED)));
  }
  private static void updateGlideClock(ServerPlayer player){
    var data=player.getPersistentData();if(!data.contains(AIR_STARTED))return;
    long time=now(player.server),start=data.getLong(AIR_STARTED);
    if(start<0||start>time){start=time;data.putLong(AIR_STARTED,start);}
    int elapsed=(int)Math.min(241L,Math.max(0,time-start))+1;
    data.putInt(AIR_USED,Math.max(Math.max(0,data.getInt(AIR_USED)),elapsed));
  }
  public static int boostCooldown(ServerPlayer player,ItemStack stack) {
    var entity=ChaoCarrierItem.entityData(stack);
    long until=Math.max(player.getPersistentData().getLong(PLAYER_BOOST_UNTIL),Math.max(ChaoCarrierItem.data(stack).getLong(BOOST_UNTIL),
        Math.max(entity.getCompound("NeoForgeData").getLong(BOOST_UNTIL),entity.getCompound("ForgeData").getLong(BOOST_UNTIL))));
    Active active=ACTIVE.get(player.getUUID());
    if(active!=null&&active.body!=null&&(active.stack==stack||active.token.equals(token(stack,false))))
      until=Math.max(until,active.body.getPersistentData().getLong(BOOST_UNTIL));
    return (int)Math.min(CompanionRules.BOOST_COOLDOWN_TICKS,Math.max(0,until-now(player.server)));
  }

  /** The request only expresses a keypress. Eligibility, active slot and elapsed ticks come from the server. */
  public static boolean requestBoost(ServerPlayer player) {
    Active active=ACTIVE.get(player.getUUID());
    if(!current(player,active)||!CompanionFlight.environment(player)||!player.isFallFlying()
        ||!CompanionRules.robotBoost(active.body.state())||active.body.state().energy<=5
        ||boostCooldown(player,active.stack)>0)return false;
    Vec3 motion=player.getDeltaMovement().add(player.getLookAngle().scale(.9)).add(0,.12,0);
    if(motion.lengthSqr()>2.25*2.25)motion=motion.normalize().scale(2.25);
    player.setDeltaMovement(motion);player.hurtMarked=true;
    long until=now(player.server)+CompanionRules.BOOST_COOLDOWN_TICKS;
    player.getPersistentData().putLong(PLAYER_BOOST_UNTIL,until);
    active.body.getPersistentData().putLong(BOOST_UNTIL,until);
    var data=ChaoCarrierItem.data(active.stack);data.putLong(BOOST_UNTIL,until);
    active.body.state().energy=Math.max(0,active.body.state().energy-2);
    active.body.sync();ChaoCarrierItem.setData(active.stack,data);ChaoCarrierItem.snapshot(active.stack,active.body);AccessoriesBridge.changed(active.slotReference);
    player.serverLevel().sendParticles(ParticleTypes.FIREWORK,player.getX(),player.getY()+.4,player.getZ(),8,.15,.15,.15,.03);
    player.displayClientMessage(Component.literal("Omochao boost — ready again in 5 seconds"),true);
    sendStatus(player,active,true);return true;
  }

  private static void modifier(ServerPlayer player,Holder<net.minecraft.world.entity.ai.attributes.Attribute> attribute,
      ResourceLocation id,double amount) {
    var instance=player.getAttribute(attribute);if(instance==null)return;
    var existing=instance.getModifier(id);
    if(amount<=0){if(existing!=null)instance.removeModifier(id);}
    else if(existing==null||Math.abs(existing.amount()-amount)>1e-9)
      instance.addOrUpdateTransientModifier(new AttributeModifier(id,amount,AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
  }

  private static void benefits(ServerPlayer player,Active active) {
    if(!current(player,active)){clearBenefits(player,active);return;}
    ChaoState state=active.body.state();
    int carrying=CarryPreview.position(active.body,player,carryPosition(player,state));
    if (CarryPreview.active(active.body) || !state.adult || state.energy<=5) {
      clearBenefits(player,active);
      active.body.setCompanionCarry(player,carrying);
      return;
    }
    if("nether".equals(state.variant)){player.clearFire();active.body.clearFire();}
    if("end".equals(state.variant)&&player.hasEffect(MobEffects.LEVITATION))player.removeEffect(MobEffects.LEVITATION);
    boolean water=player.isInWater();
    boolean swimming=water&&com.chaogarden.core.LocomotionRules.canSwim(state);
    modifier(player,Attributes.MOVEMENT_SPEED,RUN_SPEED,water||player.isFallFlying()||!com.chaogarden.core.LocomotionRules.canRun(state)?0:CompanionRules.runSpeedBonus(state.levels[ChaoState.RUN]));
    modifier(player,NeoForgeMod.SWIM_SPEED,SWIM_SPEED,swimming?CompanionRules.swimSpeedBonus(state.levels[ChaoState.SWIM]):0);
    active.vision=waterEffect(player,active.vision,MobEffects.NIGHT_VISION,OWN_VISION,swimming&&CompanionRules.waterNightVision(state.levels[ChaoState.SWIM]));
    active.breath=waterEffect(player,active.breath,MobEffects.WATER_BREATHING,OWN_BREATH,swimming&&CompanionRules.waterBreathing(state.levels[ChaoState.SWIM]));
    boolean support=CompanionFlight.environment(player);
    boolean nativeWings=player.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.CHEST).canElytraFly(player);
    if(support&&player.isFallFlying()&&(!nativeWings||active.flightUsed)) {
      flightStarted(player);
      if(CompanionRules.canElytra(state)||glideRemaining(player,state)>0) {
        // Vanilla handles lift, pitch, steering, collisions and fall distance.
        // A finite companion differs only by the authoritative elapsed budget.
        if(player.tickCount%40==0){state.energy=Math.max(0,state.energy-1);state.train(ChaoState.FLY,1);}
      }else {
        if(!nativeWings)player.stopFallFlying();
        active.flightUsed=false;
      }
    }else if(active.flightUsed&&player.isFallFlying()&&!support&&!nativeWings) {
      player.stopFallFlying();
    }
    if(!player.isFallFlying())active.flightUsed=false;
    active.body.setCompanionCarry(player,carryPosition(player,state));
    sendStatus(player,active,false);
  }

  public static int carryPosition(ServerPlayer player,ChaoState state) {
    boolean flying=!player.isSpectator() && !player.isPassenger()
        && (player.isFallFlying() || player.getAbilities().flying);
    return CarryRules.position(state,flying,player.isInWater()||player.isSwimming());
  }

  private static MobEffectInstance waterEffect(ServerPlayer player,MobEffectInstance owned,Holder<MobEffect> effect,String marker,boolean enabled){
    var current=player.getEffect(effect);
    if(!enabled){if(current!=null&&current==owned)player.removeEffect(effect);player.getPersistentData().remove(marker);return null;}
    if(current!=null&&current!=owned){player.getPersistentData().remove(marker);return null;}
    if(current==null||current.getDuration()<220){
      var addition=new MobEffectInstance(effect,240,0,true,false,true);
      var expected=current==null?addition:current;var previousWrite=writingWaterEffect;
      writingWaterEffect=addition;
      try{player.addEffect(addition);}
      finally{writingWaterEffect=previousWrite;}
      current=player.getEffect(effect);
      if(current!=expected){player.getPersistentData().remove(marker);return null;}
      if(current!=null)player.getPersistentData().putBoolean(marker,true);
    }
    return current;
  }

  /** Hand ownership to a new potion before vanilla merges effects, even if it has a shorter duration. */
  @SubscribeEvent(priority=EventPriority.HIGHEST)
  public static void externalEffect(MobEffectEvent.Applicable event){
    if(event.getEffectInstance()==writingWaterEffect||!(event.getEntity() instanceof ServerPlayer player))return;
    Active active=ACTIVE.get(player.getUUID());if(active==null)return;
    var effect=event.getEffectInstance().getEffect();
    var current=player.getEffect(effect);
    if(effect.equals(MobEffects.NIGHT_VISION)&&current!=null&&(current==active.vision||current==writingWaterEffect)){
      player.removeEffect(effect);active.vision=null;player.getPersistentData().remove(OWN_VISION);
    }else if(effect.equals(MobEffects.WATER_BREATHING)&&current!=null&&(current==active.breath||current==writingWaterEffect)){
      player.removeEffect(effect);active.breath=null;player.getPersistentData().remove(OWN_BREATH);
    }
  }

  private static void sendStatus(ServerPlayer player,Active active,boolean force){
    boolean present=current(player,active)&&CompanionRules.adult(active.body.state())&&active.body.state().energy>5;
    var status=present?new CompanionNetwork.Status(true,CompanionRules.canElytra(active.body.state()),CompanionRules.robotBoost(active.body.state()),
        glideRemaining(player,active.body.state()),boostCooldown(player,active.stack)):new CompanionNetwork.Status(false,false,false,0,0);
    if(force||!status.equals(active.status)||now(player.server)-active.statusSent>=10){
      CompanionNetwork.send(player,status);active.status=status;active.statusSent=now(player.server);
    }
  }

  private static void clearBenefits(ServerPlayer player,Active active){
    clearBenefits(player,active,false);
  }

  private static void clearBenefits(ServerPlayer player,Active active,boolean carriedInFlight){
    if(active.body!=null)active.body.setCompanionGrip(player,carriedInFlight);
    modifier(player,Attributes.MOVEMENT_SPEED,RUN_SPEED,0);modifier(player,NeoForgeMod.SWIM_SPEED,SWIM_SPEED,0);
    active.vision=waterEffect(player,active.vision,MobEffects.NIGHT_VISION,OWN_VISION,false);
    active.breath=waterEffect(player,active.breath,MobEffects.WATER_BREATHING,OWN_BREATH,false);
    if(active.flightUsed&&player.isFallFlying()&&!player.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.CHEST).canElytraFly(player))player.stopFallFlying();
    active.flightUsed=false;
    var status=new CompanionNetwork.Status(false,false,false,0,0);
    if(!status.equals(active.status)){CompanionNetwork.send(player,status);active.status=status;active.statusSent=now(player.server);}
  }

  @SubscribeEvent public static void login(PlayerEvent.PlayerLoggedInEvent event){
    if(event.getEntity() instanceof ServerPlayer player){
      modifier(player,Attributes.MOVEMENT_SPEED,RUN_SPEED,0);modifier(player,NeoForgeMod.SWIM_SPEED,SWIM_SPEED,0);
      if(player.getPersistentData().getBoolean(OWN_VISION))player.removeEffect(MobEffects.NIGHT_VISION);
      if(player.getPersistentData().getBoolean(OWN_BREATH))player.removeEffect(MobEffects.WATER_BREATHING);
      player.getPersistentData().remove(OWN_VISION);player.getPersistentData().remove(OWN_BREATH);
    }
  }

  @SubscribeEvent public static void clonePlayer(PlayerEvent.Clone event){
    long until=event.getOriginal().getPersistentData().getLong(PLAYER_BOOST_UNTIL);
    if(until>0)event.getEntity().getPersistentData().putLong(PLAYER_BOOST_UNTIL,until);
  }

  @SubscribeEvent
  public static void logout(PlayerEvent.PlayerLoggedOutEvent event) {
    if (event.getEntity() instanceof ServerPlayer player) {
      Active active = ACTIVE.remove(player.getUUID()); if (active != null) recall(player, active);
      // PlayerList.save is protected; saveAll also writes the Accessories
      // inventory after the final item-owned snapshot has been taken.
      player.server.getPlayerList().saveAll();
    }
  }

  @SubscribeEvent
  public static void dimension(PlayerEvent.PlayerChangedDimensionEvent event) {
    if (event.getEntity() instanceof ServerPlayer player) {
      Active active = ACTIVE.remove(player.getUUID()); if (active != null) recall(player, active);
    }
  }

  @SubscribeEvent
  public static void stop(ServerStoppingEvent event) {
    for (Active active : ACTIVE.values()) {
      ServerPlayer player = event.getServer().getPlayerList().getPlayer(active.keeper);
      if (player != null) recall(player, active); else if (active.body != null) active.body.discard();
    }
    ACTIVE.clear(); PENDING.clear(); event.getServer().getPlayerList().saveAll();
  }
}
