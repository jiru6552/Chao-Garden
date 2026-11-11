package com.chaogarden.market;

import com.chaogarden.ChaoGarden;
import com.chaogarden.ChaoSounds;
import com.chaogarden.distribution.EggDistribution;
import net.minecraft.core.registries.BuiltInRegistries;
import java.util.Optional;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.ai.goal.UseItemGoal;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.npc.WanderingTrader;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.ItemCost;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.level.Level;

/** A real merchant with saved stock, normal emerald payment and a finite visit. */
public final class BlackMarketTrader extends WanderingTrader {
  private static final EntityDataAccessor<Boolean> RESIDENT=SynchedEntityData.defineId(BlackMarketTrader.class,EntityDataSerializers.BOOLEAN);
  public static final int STOCK_VERSION = 36;
  private long restockAt;
  private long visitExpiry;

  public BlackMarketTrader(EntityType<? extends WanderingTrader> type, Level level) {
    super(type, level);
    setAge(0);
  }

  public static AttributeSupplier.Builder attributes() {
    return Villager.createAttributes().add(Attributes.MAX_HEALTH, 24)
        .add(Attributes.MOVEMENT_SPEED, .3).add(Attributes.FOLLOW_RANGE, 24);
  }

  public void beginVisit(long now) {
    visitExpiry = now + 24000;
    setDespawnDelay(24000);
  }

  @Override protected void defineSynchedData(SynchedEntityData.Builder builder){super.defineSynchedData(builder);builder.define(RESIDENT,false);}

  /** Kindergarten residents share the actual catalog but never end their visit. */
  public boolean resident(){
    // The old saved tag remains authoritative on the server. Only tracked data
    // reaches clients, where collision prediction needs the same identity.
    if(!level().isClientSide){
      boolean saved=getPersistentData().getBoolean("ChaoKindergartenResident");
      if(entityData.get(RESIDENT)!=saved)entityData.set(RESIDENT,saved);
    }
    return entityData.get(RESIDENT);
  }
  public void setResident(boolean resident){
    getPersistentData().putBoolean("ChaoKindergartenResident",resident);
    entityData.set(RESIDENT,resident);
    if(resident){visitExpiry=0;setDespawnDelay(0);}
    else beginVisit(level().getGameTime());
  }
  // NoAI and invulnerability do not prevent vanilla collision forces.
  // Moving the fixed resident would expose another independently stocked actor.
  @Override public boolean isPushable(){return !resident()&&super.isPushable();}
  public long visitExpiry() { return visitExpiry; }
  public int restockTicksRemaining() {
    return (int)Math.min(Integer.MAX_VALUE, Math.max(0, restockAt-level().getGameTime()));
  }

  @Override
  protected void registerGoals() {
    super.registerGoals();
    // Vanilla wandering traders drink invisibility potions at dusk. Chao remain visible.
    goalSelector.getAvailableGoals().stream().map(wrapped -> wrapped.getGoal())
        .filter(goal -> goal instanceof UseItemGoal<?>).toList().forEach(goalSelector::removeGoal);
  }

  @Override
  protected void updateTrades() {
    for (var listing : MarketStock.select(random.nextLong(), java.util.List.of(), java.util.List.of(), EggDistribution.spaceModLoaded())) {
      var holder = ChaoGarden.CONTENT.get(listing.itemId());
      if (holder == null) continue;
      var payment = MarketStock.payment(listing.emeralds());
      var a = payment.getFirst();
      ItemCost first = new ItemCost(a.blocks() ? Items.EMERALD_BLOCK : Items.EMERALD, a.count());
      Optional<ItemCost> second = payment.size() == 1 ? Optional.empty()
          : Optional.of(new ItemCost(payment.get(1).blocks() ? Items.EMERALD_BLOCK : Items.EMERALD, payment.get(1).count()));
      getOffers().add(new MerchantOffer(first, second, new ItemStack(holder.get(), listing.count()), listing.uses(), 1, 0));
    }
    restockAt = level().getGameTime() + 3600 + random.nextInt(4801);
  }

  /** Permanent base colors and drives remain purchasable in the same open vanilla menu. */
  @Override
  public void notifyTrade(MerchantOffer offer) {
    super.notifyTrade(offer);
    if(isPermanent(offer))offer.resetUses();
  }
  private static boolean isPermanent(MerchantOffer offer) {
    var key=BuiltInRegistries.ITEM.getKey(offer.getResult().getItem());
    return key.getNamespace().equals(ChaoGarden.MODID)&&MarketStock.permanent(key.getPath());
  }

  @Override
  public void aiStep() {
    if(resident()){visitExpiry=0;setDespawnDelay(0);}
    super.aiStep();
    if (level().isClientSide || !isAlive()) return;
    // Also clear a potion effect retained in an older saved trader.
    if (hasEffect(MobEffects.INVISIBILITY)) removeEffect(MobEffects.INVISIBILITY);
    long now = level().getGameTime();
    if (!resident() && visitExpiry > 0 && now >= visitExpiry && !isTrading()) {
      discard();
      return;
    }
    if (restockAt > 0 && now >= restockAt && !isTrading()) {
      offers = null;
      getOffers();
    }
  }

  @Override
  public void addAdditionalSaveData(CompoundTag tag) {
    super.addAdditionalSaveData(tag);
    tag.putInt("MarketStockVersion", STOCK_VERSION);
    tag.putLong("MarketRestockAt", restockAt);
    tag.putLong("MarketVisitExpiry", visitExpiry);
  }

  @Override
  public void readAdditionalSaveData(CompoundTag tag) {
    super.readAdditionalSaveData(tag);
    restockAt = tag.getLong("MarketRestockAt");
    visitExpiry = tag.getLong("MarketVisitExpiry");
    if(resident()){visitExpiry=0;setDespawnDelay(0);}
    boolean valid=offers!=null && MarketStock.validStock(offers.stream().map(offer->{
      var key=BuiltInRegistries.ITEM.getKey(offer.getResult().getItem());
      return key.getNamespace().equals(ChaoGarden.MODID)?key.getPath():"";
    }).toList(),EggDistribution.spaceModLoaded());
    if(tag.getInt("MarketStockVersion")!=STOCK_VERSION || !valid){offers=null;restockAt=0;}
    else offers.stream().filter(BlackMarketTrader::isPermanent).forEach(MerchantOffer::resetUses);
  }

  @Override protected SoundEvent getAmbientSound() { return ChaoSounds.AMBIENT.get(); }
  @Override protected SoundEvent getHurtSound(DamageSource source) { return ChaoSounds.HURT.get(); }
  @Override protected SoundEvent getDeathSound() { return ChaoSounds.DEATH.get(); }
  @Override protected SoundEvent getTradeUpdatedSound(boolean yes) { return yes ? ChaoSounds.HAPPY.get() : resident() ? ChaoSounds.AMBIENT.get() : ChaoSounds.HURT.get(); }
  @Override public SoundEvent getNotifyTradeSound() { return ChaoSounds.HAPPY.get(); }
  @Override public float getVoicePitch() { return 1.05f; }
}
