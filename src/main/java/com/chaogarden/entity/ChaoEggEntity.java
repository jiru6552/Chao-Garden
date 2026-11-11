package com.chaogarden.entity;

import com.chaogarden.*;
import com.chaogarden.core.ChaoState;
import com.chaogarden.item.ChaoEggItem;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.*;
import net.minecraft.util.Mth;
import net.minecraft.world.*;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.DyedItemColor;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public final class ChaoEggEntity extends Entity {
  public static final int NATURAL=0, GENTLE=1, ROUGH=2;
  public static final int GENTLE_CRACK_TICKS=30;
  private static final EntityDataAccessor<String> STATE =
      SynchedEntityData.defineId(ChaoEggEntity.class, EntityDataSerializers.STRING);
  private static final EntityDataAccessor<Integer> INCUBATION =
      SynchedEntityData.defineId(ChaoEggEntity.class, EntityDataSerializers.INT);
  private static final EntityDataAccessor<Boolean> THROWN =
      SynchedEntityData.defineId(ChaoEggEntity.class,EntityDataSerializers.BOOLEAN);
  private static final EntityDataAccessor<Integer> TREATMENT =
      SynchedEntityData.defineId(ChaoEggEntity.class,EntityDataSerializers.INT);
  private static final EquipmentSlot[] REBORN_SLOTS={EquipmentSlot.HEAD,EquipmentSlot.OFFHAND,
      EquipmentSlot.CHEST,EquipmentSlot.LEGS,EquipmentSlot.FEET};
  public UUID owner;
  public Component chaoName;
  private String cached = "";
  private ChaoState cachedState = new ChaoState();
  private ItemStack sourceItem=ItemStack.EMPTY;
  private String rebornOriginEggCoat="";
  private CompoundTag rebornEquipment=new CompoundTag();
  private CompoundTag rebornRelationships=new CompoundTag();
  private UUID rebornChaoId;
  private UUID thrower;
  private int flightTicks;
  private boolean hatching;

  public ChaoEggEntity(EntityType<? extends ChaoEggEntity> type, Level world) {
    super(type, world);
  }

  @Override
  protected void defineSynchedData(SynchedEntityData.Builder b) {
    b.define(STATE, new ChaoState().encode());
    b.define(INCUBATION, 0);
    b.define(THROWN,false);
    b.define(TREATMENT,NATURAL);
  }

  public ChaoState state() {
    String s = entityData.get(STATE);
    if (!s.equals(cached)) {
      cached = s;
      cachedState = ChaoState.decode(s);
    }
    return cachedState;
  }

  public void setState(ChaoState s) {
    com.chaogarden.ChaoVariants.normalizeRetired(s);
    entityData.set(STATE, s.encode());
  }

  public void setIncubation(int v) {
    entityData.set(INCUBATION, Mth.clamp(v,0,Integer.MAX_VALUE-1));
  }

  public float hatchProgress() {
    return Math.min(1f, entityData.get(INCUBATION) / (float) GardenConfig.HATCH_TICKS.get());
  }

  /** A short settling crack for rocked eggs and a longer anticipation for natural hatches. */
  public float crackProgress(float partial) {
    if(isThrown())return 0;
    int total=GardenConfig.HATCH_TICKS.get();
    float duration=hatchTreatment()==GENTLE?Math.min(GENTLE_CRACK_TICKS,total):Math.max(1,total*.06f);
    return Mth.clamp((incubation()+partial-(total-duration))/duration,0,1);
  }

  public int incubation(){return entityData.get(INCUBATION);}
  public boolean isThrown(){return entityData.get(THROWN);}
  public int hatchTreatment(){return entityData.get(TREATMENT);}
  public void setHatchTreatment(int method){entityData.set(TREATMENT,Mth.clamp(method,NATURAL,ROUGH));}
  public void setRebornOriginEggCoat(String coat) {
    rebornOriginEggCoat=coat==null||coat.isEmpty()?"":ChaoState.decode(coat).encode();
  }
  public void setSourceItem(ItemStack stack){sourceItem=stack.copyWithCount(1);}
  public void setRebornEquipment(CompoundTag equipment){rebornEquipment=equipment.copy();}
  public CompoundTag rebornEquipment(){return rebornEquipment.copy();}
  public void setRebornRelationships(CompoundTag relationships){rebornRelationships=relationships.copy();}
  public void setRebornChaoId(UUID identity){rebornChaoId=identity;}
  public void setRebornHat(ItemStack hat) {
    if(hat.isEmpty())rebornEquipment.remove(EquipmentSlot.HEAD.getName());
    else rebornEquipment.put(EquipmentSlot.HEAD.getName(),hat.copyWithCount(1).save(level().registryAccess()));
  }

  /** Only a moving wall impact chooses rough hatching; throwing alone does not. */
  public void throwWithVelocity(Vec3 velocity,Player player) {
    entityData.set(THROWN,true);flightTicks=0;thrower=player==null?null:player.getUUID();
    setDeltaMovement(velocity);setOnGround(false);hasImpulse=true;
    setYRot((float)(Mth.atan2(velocity.x,velocity.z)*Mth.RAD_TO_DEG));
  }

  @Override
  public void tick() {
    super.tick();
    if(isRemoved())return;
    Vec3 incoming=getDeltaMovement();
    if(!isNoGravity())incoming=incoming.add(0,-.04,0);
    Vec3 before=position();
    // The generic collision flag also includes other eggs. Only a block wall
    // reached by this movement should choose rough hatching.
    Vec3 blocksOnly=!level().isClientSide&&isThrown()
        ?Entity.collideBoundingBox(this,incoming,getBoundingBox(),level(),List.of()):incoming;
    setDeltaMovement(incoming);
    move(MoverType.SELF,incoming);
    if(!level().isClientSide&&isThrown()) {
      flightTicks++;
      // SA2/Battle eggs break against walls, not from a floor landing or fall.
      Vec3 travelled=position().subtract(before);
      boolean wall=(Math.abs(blocksOnly.x-incoming.x)>1e-6&&Math.abs(blocksOnly.x-travelled.x)<1e-5)
          ||(Math.abs(blocksOnly.z-incoming.z)>1e-6&&Math.abs(blocksOnly.z-travelled.z)<1e-5);
      if(horizontalCollision&&wall&&incoming.horizontalDistanceSqr()>=.0025) {
        setHatchTreatment(ROUGH);hatch(incoming);return;
      }
      if((onGround()&&incoming.horizontalDistanceSqr()<.0025)||flightTicks>=300) {
        entityData.set(THROWN,false);flightTicks=0;
      }
    }
    setDeltaMovement(getDeltaMovement().multiply(onGround()?.72:.99,.98,onGround()?.72:.99));
    if(!level().isClientSide&&!isThrown()&&ChaoVariants.obtainable(state().variant)) {
      int n=incubation()+1;setIncubation(n);
      if(n>=GardenConfig.HATCH_TICKS.get())hatch(Vec3.ZERO);
    }
  }

  private void hatch(Vec3 impact) {
    if(level().isClientSide||isRemoved()||hatching||!ChaoVariants.obtainable(state().variant))return;
    hatching=true;
    try {
      // A next life keeps its original body identity for the garden ledger.
      // Refuse a replay while that identity exists in any loaded dimension.
      if(rebornChaoId!=null)for(ServerLevel world:((ServerLevel)level()).getServer().getAllLevels()) {
        Entity existing=world.getEntity(rebornChaoId);
        if(existing!=null&&!existing.isRemoved())return;
      }
      var chao=ChaoGarden.CHAO.get().create(level());if(chao==null)return;
      if(rebornChaoId!=null)chao.setUUID(rebornChaoId);
      // Apply to a copy until spawn commits: retries never modify egg genes twice.
      ChaoState shellOrigin=ChaoState.decode(state().encode());
      ChaoState born=ChaoState.decode(state().encode());int method=hatchTreatment();
      // The rebirth cocoon precedes this egg; it must not wrap the newborn again.
      born.cocoonTicks=0;
      if(method!=NATURAL)born.applyHatchingMethod(method==GENTLE);
      if(method==ROUGH)born.bond=Math.max(-100,born.bond-10);
      chao.setState(born);chao.setOriginEggCoat(rebornOriginEggCoat.isEmpty()?shellOrigin:ChaoState.decode(rebornOriginEggCoat));chao.owner=owner;
      chao.restoreRelationships(rebornRelationships);
      Vec3 away=impact.horizontalDistanceSqr()>0?new Vec3(impact.x,0,impact.z).normalize().scale(-.12):Vec3.ZERO;
      chao.moveTo(getX()+away.x,getY(),getZ()+away.z,getYRot(),0);
      if(chaoName!=null)chao.setCustomName(chaoName);
      for(EquipmentSlot slot:REBORN_SLOTS)if(rebornEquipment.contains(slot.getName())) {
        ItemStack hat=ItemStack.parseOptional(level().registryAccess(),rebornEquipment.getCompound(slot.getName()));
        if(!hat.isEmpty()){chao.setItemSlot(slot,hat.copyWithCount(1));chao.setDropChance(slot,1f);}
      }
      // Check blocks, not the egg being replaced: it is deliberately collidable
      // and would otherwise reject its own hatchling forever.
      if(level().getBlockCollisions(chao,chao.getBoundingBox()).iterator().hasNext()) {
        chao.moveTo(getX()+away.x*2,getY()+.1,getZ()+away.z*2,getYRot(),0);
        if(level().getBlockCollisions(chao,chao.getBoundingBox()).iterator().hasNext())return;
      }
      if(!level().addFreshEntity(chao))return;
      // Remove the consumed shell before effects/callbacks can fail.
      discard();sourceItem=ItemStack.EMPTY;rebornEquipment=new CompoundTag();rebornRelationships=new CompoundTag();rebornChaoId=null;
      leaveEggshellHat(shellOrigin);
      chao.hatchReaction(method==ROUGH,thrower==null?null:level().getPlayerByUUID(thrower));
      ((ServerLevel)level()).sendParticles(method==ROUGH?ParticleTypes.POOF:ParticleTypes.HAPPY_VILLAGER,
          getX(),getY()+.4,getZ(),12,.25,.25,.25,.025);
      level().playSound(null,blockPosition(),SoundEvents.TURTLE_EGG_HATCH,SoundSource.NEUTRAL,1,1.3f);
    } finally {hatching=false;}
  }

  private void leaveEggshellHat(ChaoState born) {
    if(!ChaoGarden.CONTENT.containsKey("eggshell_hat"))return;
    ItemStack shell=new ItemStack(ChaoGarden.item("eggshell_hat"));
    int[] colors={0x5bd0eb,0xefeddf,0xdb4945,0xf5d456,0x4372d8,0x59cdd7,0xec9cc3,0xed8f41,
        0x9d6dc6,0x9b6f4f,0x4ea563,0x93a2aa,0xa8d153,0x383e4b,0x8cc1ea,0x34beb2};
    int[] jewels={0x5bd0eb,0xb9d2e1,0xe9b33a,0xc32453,0x3160d2,0x1cae7e,0x9050ce,
        0x40cddc,0x992e42,0x2d2c49,0xb4e4f0,0xa6c53d,0xe6a234,0x4a458f};
    int color=born.jewel()>0?jewels[Math.min(born.jewel(),jewels.length-1)]
        :colors[Math.max(0,Math.min(born.baseColor(),colors.length-1))];
    shell.set(DataComponents.DYED_COLOR,new DyedItemColor(color,true));
    shell.set(DataComponents.ENCHANTMENT_GLINT_OVERRIDE,born.isShiny()||born.jewel()>0);
    CompoundTag coat=new CompoundTag();coat.putString("EggShellCoat",born.encode());
    shell.set(DataComponents.CUSTOM_DATA,CustomData.of(coat));
    var dropped=spawnAtLocation(shell);if(dropped!=null){dropped.setPickUpDelay(20);if(owner!=null)dropped.setTarget(owner);}
  }

  /** Preserve item components and foreign custom data through egg recovery. */
  public ItemStack snapshotItem() {
    ItemStack stack=sourceItem.isEmpty()?ChaoEggItem.containing(state(),incubation()):sourceItem.copyWithCount(1);
    CompoundTag data=stack.getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag();
    data.putString("Chao",state().encode());data.putInt("HatchTicks",incubation());
    if(!rebornOriginEggCoat.isEmpty())data.putString("RebornOriginEggCoat",rebornOriginEggCoat);else data.remove("RebornOriginEggCoat");
    data.putUUID("EggId",getUUID());data.putInt("HatchTreatment",hatchTreatment());
    if(owner!=null)data.putUUID("Keeper",owner);else data.remove("Keeper");
    if(chaoName!=null)data.putString("ChaoName",Component.Serializer.toJson(chaoName,level().registryAccess()));
    if(!rebornEquipment.isEmpty())data.put("RebornEquipment",rebornEquipment.copy());else data.remove("RebornEquipment");
    if(!rebornRelationships.isEmpty())data.put("RebornRelationships",rebornRelationships.copy());else data.remove("RebornRelationships");
    if(rebornChaoId!=null)data.putUUID("RebornChaoId",rebornChaoId);else data.remove("RebornChaoId");
    data.remove("ThrowCharging");stack.set(DataComponents.CUSTOM_DATA,CustomData.of(data));return stack;
  }

  private boolean mayPick(Player p) {
    return owner == null || owner.equals(p.getUUID()) || p.getAbilities().instabuild;
  }

  @Override
  public InteractionResult interact(Player p, InteractionHand hand) {
    if (!mayPick(p)) {
      if (!level().isClientSide)
        p.displayClientMessage(Component.literal("This egg belongs to another keeper."), true);
      return InteractionResult.FAIL;
    }
    if (!level().isClientSide) {
      var stack=snapshotItem();
      // Inventory.add silently deletes overflow in creative. An individual
      // creature must instead survive as one dropped egg when no slot is free.
      int free=p.getInventory().getFreeSlot();
      if(free>=0)p.getInventory().add(free,stack);
      if(!stack.isEmpty()) {
        var overflow=new ItemEntity(level(),p.getX(),p.getY()+.25,p.getZ(),stack);
        overflow.setPickUpDelay(10);overflow.setTarget(owner==null?p.getUUID():owner);
        if(!level().addFreshEntity(overflow))return InteractionResult.FAIL;
      }
      discard();
    }
    return InteractionResult.sidedSuccess(level().isClientSide);
  }

  @Override
  public boolean hurt(DamageSource source, float amount) {
    if(!(source.getEntity() instanceof Player p)||!mayPick(p)||amount<=0)return false;
    if(!level().isClientSide) {
      if(owner==null)owner=p.getUUID();
      throwWithVelocity(p.getLookAngle().scale(.85).add(0,.18,0),p);
      level().playSound(null,blockPosition(),SoundEvents.EGG_THROW,SoundSource.PLAYERS,.5f,.9f);
    }
    return true;
  }

  @Override
  public boolean isPickable() {
    return true;
  }

  @Override
  public boolean canBeCollidedWith() {
    return true;
  }

  @Override
  protected void addAdditionalSaveData(CompoundTag tag) {
    tag.putString("Chao", entityData.get(STATE));
    if(!rebornOriginEggCoat.isEmpty())tag.putString("RebornOriginEggCoat",rebornOriginEggCoat);
    tag.putInt("Incubation", entityData.get(INCUBATION));
    tag.putBoolean("Thrown",isThrown());tag.putInt("FlightTicks",flightTicks);
    tag.putInt("HatchTreatment",hatchTreatment());
    if(thrower!=null)tag.putUUID("Thrower",thrower);
    if(!sourceItem.isEmpty())tag.put("SourceItem",sourceItem.save(level().registryAccess()));
    if(!rebornEquipment.isEmpty())tag.put("RebornEquipment",rebornEquipment.copy());
    if(!rebornRelationships.isEmpty())tag.put("RebornRelationships",rebornRelationships.copy());
    if(rebornChaoId!=null)tag.putUUID("RebornChaoId",rebornChaoId);
    if (owner != null) tag.putUUID("Keeper", owner);
    if (chaoName != null)
      tag.putString("ChaoName", Component.Serializer.toJson(chaoName, level().registryAccess()));
  }

  @Override
  protected void readAdditionalSaveData(CompoundTag tag) {
    setState(ChaoState.decode(tag.getString("Chao")));
    setRebornOriginEggCoat(tag.getString("RebornOriginEggCoat"));
    setIncubation(tag.getInt("Incubation"));
    entityData.set(THROWN,tag.getBoolean("Thrown"));flightTicks=Mth.clamp(tag.getInt("FlightTicks"),0,300);
    setHatchTreatment(tag.getInt("HatchTreatment"));
    thrower=tag.hasUUID("Thrower")?tag.getUUID("Thrower"):null;
    sourceItem=tag.contains("SourceItem")?ItemStack.parseOptional(level().registryAccess(),tag.getCompound("SourceItem")):ItemStack.EMPTY;
    rebornEquipment=tag.getCompound("RebornEquipment").copy();
    rebornRelationships=tag.getCompound("RebornRelationships").copy();
    rebornChaoId=tag.hasUUID("RebornChaoId")?tag.getUUID("RebornChaoId"):null;
    if (tag.hasUUID("Keeper")) owner = tag.getUUID("Keeper");
    if (tag.contains("ChaoName"))
      chaoName = Component.Serializer.fromJson(tag.getString("ChaoName"), level().registryAccess());
  }
}
