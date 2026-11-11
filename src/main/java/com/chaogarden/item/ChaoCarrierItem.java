package com.chaogarden.item;

import com.chaogarden.ChaoGarden;
import com.chaogarden.activities.ActivityManager;
import com.chaogarden.companion.AccessoriesBridge;
import com.chaogarden.companion.CompanionContent;
import com.chaogarden.companion.CompanionRules;
import com.chaogarden.core.ChaoState;
import com.chaogarden.entity.ChaoEntity;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.DoubleTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

/** Saved held Chao. The legacy registry ID remains so existing Chao are never lost. */
public class ChaoCarrierItem extends Item {
  public ChaoCarrierItem() { super(new Item.Properties().stacksTo(1)); }

  public static CompoundTag data(ItemStack stack) {
    return stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
  }

  public static void setData(ItemStack stack, CompoundTag data) {
    if (data.isEmpty()) stack.remove(DataComponents.CUSTOM_DATA);
    else stack.set(DataComponents.CUSTOM_DATA, CustomData.of(data));
  }

  public static boolean filled(ItemStack stack) { return data(stack).contains("ChaoEntity"); }
  public static CompoundTag entityData(ItemStack stack) { return data(stack).getCompound("ChaoEntity"); }
  public static ChaoState state(ItemStack stack) { return com.chaogarden.ChaoVariants.normalizeRetired(ChaoState.decode(entityData(stack).getString("Chao"))); }
  public static UUID storedUUID(ItemStack stack) {
    CompoundTag entity = entityData(stack);
    return entity.hasUUID("UUID") ? entity.getUUID("UUID") : null;
  }

  public static boolean canRelease(ItemStack stack, Player player) {
    if (!filled(stack)) return false;
    if(player.level() instanceof ServerLevel level && !com.chaogarden.recovery.ChaoRecovery.itemAllowed(level.getServer(),stack))return false;
    CompoundTag entity = entityData(stack);
    return !entity.hasUUID("Keeper") || entity.getUUID("Keeper").equals(player.getUUID())
        || player.getAbilities().instabuild;
  }

  public static void storeEntity(ItemStack stack, CompoundTag entity) {
    CompoundTag data = data(stack); data.put("ChaoEntity", entity.copy()); setData(stack, data);
  }

  public static void snapshot(ItemStack stack, ChaoEntity chao) {
    CompoundTag entity = new CompoundTag(); chao.saveWithoutId(entity);
    if(CompanionContent.isManaged(chao)) {
      // Body-only follower/flight physics must not escape into a garden release.
      entity.putBoolean("NoGravity",false);entity.putFloat("FallDistance",0);
      ListTag motion=new ListTag();
      for(int i=0;i<3;i++)motion.add(DoubleTag.valueOf(0));
      entity.put("Motion",motion);
    }
    // A summoned body is an unsaved projection of the item. Never carry its
    // management marker back into an ordinary released garden Chao.
    for (String key : new String[] {"NeoForgeData", "ForgeData"}) {
      if (!entity.contains(key)) continue;
      CompoundTag persistent = entity.getCompound(key);
      persistent.remove(CompanionContent.MARKER);
      if (persistent.isEmpty()) entity.remove(key); else entity.put(key, persistent);
    }
    storeEntity(stack, entity);
  }

  public static boolean capture(ItemStack stack, ChaoEntity chao, Player player) {
    if (filled(stack) || com.chaogarden.kindergarten.KindergartenContent.isBoarded(chao) || !chao.canCare(player) || CompanionContent.isManaged(chao)
        || ActivityManager.isBusy(chao) || chao.isCocooned() || chao.isRemoved()) return false;
    if (chao.owner == null) chao.owner = player.getUUID();
    snapshot(stack, chao);
    stack.set(DataComponents.CUSTOM_NAME, chao.getName().copy());
    CompanionContent.token(stack, true);
    com.chaogarden.recovery.ChaoRecovery.captured(chao);
    chao.discard();
    return true;
  }

  @Override
  public InteractionResult useOn(UseOnContext context) {
    ItemStack stack = context.getItemInHand();
    if (!filled(stack)) return InteractionResult.PASS;
    Player player = context.getPlayer();
    if (player == null || !canRelease(stack, player)) return InteractionResult.FAIL;
    if (context.getLevel().isClientSide) return InteractionResult.SUCCESS;
    ServerLevel world = (ServerLevel) context.getLevel();
    if (CompanionContent.itemIsSummoned(stack)) return message(player, "Unequip this Chao before releasing it.");
    long remaining = CompanionContent.remaining(stack, world.getServer());
    if (remaining > 0) return message(player, "Your Chao is recovering: " + ((remaining+19)/20) + " seconds.");
    UUID uuid = storedUUID(stack);
    if (CompanionContent.findEntity(world.getServer(), uuid) != null)
      return message(player, "This Chao is already present in the world.");
    var position = context.getClickedPos().relative(context.getClickedFace());
    if (!world.getWorldBorder().isWithinBounds(position)
        || !world.getBlockState(position).getCollisionShape(world, position).isEmpty()
        || !player.mayUseItemAt(position, context.getClickedFace(), stack)) return InteractionResult.FAIL;
    ChaoEntity chao = ChaoGarden.CHAO.get().create(world);
    if (chao == null) return InteractionResult.FAIL;
    chao.load(entityData(stack));
    if (chao.owner == null) chao.owner = player.getUUID();
    chao.moveTo(position.getX()+.5, position.getY(), position.getZ()+.5, player.getYRot(), 0);
    if (!world.noCollision(chao) || !world.addFreshEntity(chao)) return InteractionResult.FAIL;
    com.chaogarden.recovery.ChaoRecovery.released(chao);
    CompoundTag data = data(stack);
    data.remove("ChaoEntity"); data.remove(CompanionContent.TOKEN); data.remove(CompanionContent.DISABLED_UNTIL);
    setData(stack, data); stack.remove(DataComponents.CUSTOM_NAME);
    // The stack represents the creature itself, not a reusable container.
    // Consume it in creative too: leaving a filled copy would duplicate its UUID.
    stack.setCount(0);
    com.chaogarden.activities.GardenRegistry.get(world.getServer()).placed(chao);
    return InteractionResult.CONSUME;
  }

  @Override
  public void inventoryTick(ItemStack stack, Level level, Entity entity, int slot, boolean selected) {
    // Compatibility for empty containers left behind by older releases.
    // A filled legacy stack remains the same saved Chao and can still be released.
    if (!level.isClientSide && !filled(stack)) stack.setCount(0);
  }

  private static InteractionResult message(Player player, String text) {
    player.displayClientMessage(Component.literal(text), true); return InteractionResult.FAIL;
  }

}
