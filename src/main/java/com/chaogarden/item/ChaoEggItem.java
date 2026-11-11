package com.chaogarden.item;

import com.chaogarden.ChaoGarden;
import com.chaogarden.ChaoVariants;
import com.chaogarden.GardenConfig;
import com.chaogarden.core.ChaoState;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;
import com.chaogarden.entity.ChaoEggEntity;

public class ChaoEggItem extends Item {
  public static final int USE_DURATION=72000, ROCK_TICKS=160, MIN_THROW_TICKS=4;
  public static final String NATIVE_COAT="NativeEggCoat";
  public final int color, jewel;
  public final boolean shiny;
  public final String variant;

  public ChaoEggItem(int color, boolean shiny, int jewel) {
    this(color, shiny, jewel, ChaoVariants.NORMAL);
  }

  public ChaoEggItem(int color, boolean shiny, int jewel, String variant) {
    super(new Item.Properties().stacksTo(1));
    this.color = color;
    this.shiny = shiny;
    this.jewel = jewel;
    this.variant = variant;
  }

  @Override
  public boolean isFoil(ItemStack stack) {
    var data = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
    if (data.contains("Chao")) {
      ChaoState inherited = ChaoState.decode(data.getString("Chao"));
      return inherited.isShiny() || inherited.jewel() > 0;
    }
    if(data.contains(NATIVE_COAT)) {
      var coat=data.getCompound(NATIVE_COAT);return coat.getBoolean("Shiny")||coat.getInt("Jewel")>0;
    }
    return shiny || jewel > 0;
  }

  private static CompoundTag data(ItemStack stack) {
    return stack.getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag();
  }
  private static void data(ItemStack stack,CompoundTag tag){stack.set(DataComponents.CUSTOM_DATA,CustomData.of(tag));}
  private boolean admitted(ItemStack stack,Player player) {
    CompoundTag tag=data(stack);
    ChaoState saved=tag.contains("Chao")?ChaoVariants.normalizeRetired(ChaoState.decode(tag.getString("Chao"))):null;
    String heritage=saved==null?variant:saved.variant;
    if((!ChaoVariants.obtainable(variant)||!ChaoVariants.available(variant))||(!ChaoVariants.obtainable(heritage)||!ChaoVariants.available(heritage))) {
      if(!player.level().isClientSide)player.displayClientMessage(Component.literal(ChaoVariants.lockedMessage(
          (!ChaoVariants.obtainable(variant)||!ChaoVariants.available(variant))?variant:heritage)),true);
      return false;
    }
    if(tag.hasUUID("Keeper")&&!tag.getUUID("Keeper").equals(player.getUUID())&&!player.getAbilities().instabuild) {
      if(!player.level().isClientSide)player.displayClientMessage(Component.literal("This egg belongs to another keeper."),true);
      return false;
    }
    return true;
  }

  /** Fix an individual egg before it is handled, rather than rerolling it on release. */
  private CompoundTag prepare(ItemStack stack,Level level,Player player) {
    CompoundTag tag=data(stack);
    if(!tag.contains("Chao")) {
      var coat=tag.getCompound(NATIVE_COAT);
      int originColor=tag.contains(NATIVE_COAT)?coat.getInt("Color"):color;
      boolean originShiny=tag.contains(NATIVE_COAT)?coat.getBoolean("Shiny"):shiny;
      int originJewel=tag.contains(NATIVE_COAT)?coat.getInt("Jewel"):jewel;
      ChaoState state=ChaoState.egg(originColor,originShiny,originColor==0,originJewel,
          new java.util.Random(level.random.nextLong()),GardenConfig.NEWBORN_EYE_CHANCE.get());
      state.variant=variant;tag.putString("Chao",state.encode());
    }
    tag.remove(NATIVE_COAT);
    if(!tag.hasUUID("EggId"))tag.putUUID("EggId",UUID.randomUUID());
    if(!tag.hasUUID("Keeper"))tag.putUUID("Keeper",player.getUUID());
    data(stack,tag);return tag;
  }

  @Override public InteractionResultHolder<ItemStack> use(Level level,Player player,InteractionHand hand) {
    ItemStack stack=player.getItemInHand(hand);
    if(!admitted(stack,player))return InteractionResultHolder.fail(stack);
    if(!level.isClientSide) {
      CompoundTag tag=prepare(stack,level,player);
      // Latch the mode at the start of the action, so pressing sneak midway
      // through a gentle rock cannot accidentally throw the individual egg.
      tag.putBoolean("ThrowCharging",player.isShiftKeyDown());data(stack,tag);
    }
    player.startUsingItem(hand);return InteractionResultHolder.consume(stack);
  }
  @Override public int getUseDuration(ItemStack stack,LivingEntity holder){return USE_DURATION;}
  @Override public UseAnim getUseAnimation(ItemStack stack){return UseAnim.NONE;}
  @Override public void onUseTick(Level level,LivingEntity holder,ItemStack stack,int remaining) {
    if(level.isClientSide||!(holder instanceof Player player))return;
    CompoundTag tag=data(stack);
    if(tag.getBoolean("ThrowCharging")||tag.getInt("HatchTreatment")==ChaoEggEntity.GENTLE)return;
    int rocks=Math.min(ROCK_TICKS,Math.min(ROCK_TICKS,Math.max(0,tag.getInt("RockTicks")))+1);tag.putInt("RockTicks",rocks);
    if(rocks==ROCK_TICKS) {
      tag.putInt("HatchTreatment",ChaoEggEntity.GENTLE);
      tag.putInt("HatchTicks",Math.max(tag.getInt("HatchTicks"),GardenConfig.HATCH_TICKS.get()-ChaoEggEntity.GENTLE_CRACK_TICKS));
      player.displayClientMessage(Component.literal("The egg is ready. Set it down gently to let it hatch."),true);
      level.playSound(null,player.blockPosition(),net.minecraft.sounds.SoundEvents.TURTLE_EGG_CRACK,
          net.minecraft.sounds.SoundSource.PLAYERS,.35f,1.3f);
    }
    data(stack,tag);
  }

  @Override public void releaseUsing(ItemStack stack,Level level,LivingEntity holder,int remaining) {
    if(level.isClientSide||!(holder instanceof Player player))return;
    CompoundTag tag=data(stack);boolean throwing=tag.getBoolean("ThrowCharging");
    tag.remove("ThrowCharging");data(stack,tag);
    int elapsed=USE_DURATION-remaining;
    if(!throwing||elapsed<MIN_THROW_TICKS||!admitted(stack,player))return;
    ChaoEggEntity egg=fromStack(stack,level,player);if(egg==null)return;
    Vec3 look=player.getLookAngle();Vec3 at=player.getEyePosition().add(look.scale(.35));
    egg.setPos(at);if(!level.noCollision(egg)){egg.setPos(player.getEyePosition());if(!level.noCollision(egg))return;}
    float charge=Math.min(1,elapsed/20f);
    egg.throwWithVelocity(look.scale(.35+.95*charge).add(0,.12,0),player);
    if(!level.addFreshEntity(egg))return;
    // A handled stack is an individual egg. Consume it even in creative.
    stack.shrink(1);player.getCooldowns().addCooldown(this,8);
    level.playSound(null,player.blockPosition(),net.minecraft.sounds.SoundEvents.EGG_THROW,
        net.minecraft.sounds.SoundSource.PLAYERS,.6f,1f);
  }

  private ChaoEggEntity fromStack(ItemStack stack,Level level,Player player) {
    CompoundTag tag=prepare(stack,level,player);
    if(tag.hasUUID("EggId")&&level instanceof ServerLevel server) {
      var existing=server.getEntity(tag.getUUID("EggId"));
      if(existing!=null&&!existing.isRemoved()) {
        player.displayClientMessage(Component.literal("This egg is already present in the world."),true);return null;
      }
    }
    ChaoEggEntity egg=ChaoGarden.CHAO_EGG.get().create(level);if(egg==null)return null;
    egg.setUUID(tag.getUUID("EggId"));egg.setState(ChaoState.decode(tag.getString("Chao")));
    egg.setIncubation(tag.getInt("HatchTicks"));egg.setHatchTreatment(tag.getInt("HatchTreatment"));
    egg.owner=tag.getUUID("Keeper");egg.setSourceItem(stack);
    egg.setRebornEquipment(tag.getCompound("RebornEquipment"));
    egg.setRebornRelationships(tag.getCompound("RebornRelationships"));
    egg.setRebornChaoId(tag.hasUUID("RebornChaoId")?tag.getUUID("RebornChaoId"):null);
    egg.setRebornOriginEggCoat(tag.getString("RebornOriginEggCoat"));
    if(tag.contains("ChaoName"))egg.chaoName=Component.Serializer.fromJson(tag.getString("ChaoName"),level.registryAccess());
    Component label=stack.get(DataComponents.CUSTOM_NAME);
    if(label!=null) {
      boolean generated=tag.contains("DefaultEggName")?label.getString().equals(tag.getString("DefaultEggName"))
          :label.getString().startsWith("Chao Egg • ");
      if(!generated)egg.chaoName=label.copy();
    }
    return egg;
  }

  @Override
  public InteractionResult useOn(UseOnContext c) {
    var data =
        c.getItemInHand().getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
    ChaoState saved = data.contains("Chao") ? ChaoState.decode(data.getString("Chao")) : null;
    if (saved != null) ChaoVariants.normalizeRetired(saved);
    String heritage = saved == null ? variant : saved.variant;
    // Check both the registered shell and its embedded state, so a normal egg
    // carrying custom data cannot bypass an absent integration's gate.
    if ((!ChaoVariants.obtainable(variant)||!ChaoVariants.available(variant)) || (!ChaoVariants.obtainable(heritage)||!ChaoVariants.available(heritage))) {
      if (!c.getLevel().isClientSide && c.getPlayer() != null)
        c.getPlayer().displayClientMessage(Component.literal(ChaoVariants.lockedMessage(
            (!ChaoVariants.obtainable(variant)||!ChaoVariants.available(variant)) ? variant : heritage)), true);
      return InteractionResult.FAIL;
    }
    if (c.getLevel().isClientSide) return InteractionResult.SUCCESS;
    var pos = c.getClickedPos().relative(c.getClickedFace());
    if (!c.getLevel().getBlockState(pos).getCollisionShape(c.getLevel(), pos).isEmpty())
      return InteractionResult.FAIL;
    if (c.getPlayer() != null
        && !c.getPlayer().mayUseItemAt(pos, c.getClickedFace(), c.getItemInHand()))
      return InteractionResult.FAIL;
    if(c.getPlayer()==null||!admitted(c.getItemInHand(),c.getPlayer()))return InteractionResult.FAIL;
    boolean individual=data.contains("Chao")||data.hasUUID("EggId");
    var egg=fromStack(c.getItemInHand(),c.getLevel(),c.getPlayer());
    if(egg==null)return InteractionResult.FAIL;
    egg.setPos(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
    if(!c.getLevel().noCollision(egg)||!c.getLevel().addFreshEntity(egg))return InteractionResult.FAIL;
    if(individual||!c.getPlayer().getAbilities().instabuild)
      c.getItemInHand().shrink(1);
    else {
      // Infinite creative catalogue eggs create new identities each time.
      // Recovered/rocked eggs above are consumed as individual creatures.
      CompoundTag reset=data(c.getItemInHand());
      reset.remove("Chao");reset.remove("EggId");reset.remove("Keeper");data(c.getItemInHand(),reset);
    }
    return InteractionResult.CONSUME;
  }

  public static ItemStack containing(ChaoState state, int elapsed) {
    if (ChaoVariants.retired(state.variant)) {
      state = ChaoVariants.normalizeRetired(ChaoState.decode(state.encode()));
    }
    String shell=ChaoVariants.find(state.variant).isPresent()?ChaoVariants.itemId(state.variant)
        :state.jewel()>0?ChaoState.JEWELS[state.jewel()]+"_jewel_egg"
        :state.isShiny()?(state.baseColor()==0?"shiny_egg":"shiny_"+ChaoState.COLORS[state.baseColor()]+"_egg")
        :ChaoState.COLORS[state.baseColor()]+"_egg";
    if(!ChaoGarden.CONTENT.containsKey(shell))shell="normal_egg";
    var stack = new ItemStack(ChaoGarden.item(shell));
    var tag = new CompoundTag();
    tag.putString("Chao", state.encode());
    tag.putInt("HatchTicks", elapsed);
    tag.putUUID("EggId",UUID.randomUUID());
    Component label=Component.literal("Chao Egg • " + (ChaoVariants.NORMAL.equals(state.variant)
        ?state.baseColorName():ChaoVariants.name(state.variant))+(state.isShiny()?" • Shiny":""));
    tag.putString("DefaultEggName",label.getString());
    stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
    stack.set(
        DataComponents.CUSTOM_NAME,
        label);
    return stack;
  }

}
