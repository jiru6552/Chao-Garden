package com.chaogarden.entity;

import com.chaogarden.*;
import com.chaogarden.core.ChaoState;
import com.chaogarden.core.LocomotionRules;
import com.chaogarden.item.*;
import java.util.*;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.*;
import net.minecraft.world.*;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.Level;

/** The entity is the authoritative host; all inherited data lives in ChaoState. */
public class ChaoEntity extends Animal {
  private static final EntityDataAccessor<String> ORIGIN_EGG_COAT =
      SynchedEntityData.defineId(ChaoEntity.class, EntityDataSerializers.STRING);
  private static final EntityDataAccessor<String> STATE =
      SynchedEntityData.defineId(ChaoEntity.class, EntityDataSerializers.STRING);
  private static final EntityDataAccessor<Boolean> RESTING =
      SynchedEntityData.defineId(ChaoEntity.class, EntityDataSerializers.BOOLEAN);
  private static final EntityDataAccessor<Integer> EATING =
      SynchedEntityData.defineId(ChaoEntity.class, EntityDataSerializers.INT);
  private static final EntityDataAccessor<Integer> AFFECTION =
      SynchedEntityData.defineId(ChaoEntity.class, EntityDataSerializers.INT);
  private static final EntityDataAccessor<Boolean> GLIDING =
      SynchedEntityData.defineId(ChaoEntity.class, EntityDataSerializers.BOOLEAN);
  private static final EntityDataAccessor<Integer> COMPANION_KEEPER =
      SynchedEntityData.defineId(ChaoEntity.class, EntityDataSerializers.INT);
  private static final EntityDataAccessor<Integer> COMPANION_GRIP =
      SynchedEntityData.defineId(ChaoEntity.class, EntityDataSerializers.INT);
  private static final EntityDataAccessor<Integer> ITEM_ACTION =
      SynchedEntityData.defineId(ChaoEntity.class, EntityDataSerializers.INT);
  private static final EntityDataAccessor<Long> ITEM_STARTED =
      SynchedEntityData.defineId(ChaoEntity.class, EntityDataSerializers.LONG);
  private static final EntityDataAccessor<Integer> ITEM_DURATION =
      SynchedEntityData.defineId(ChaoEntity.class, EntityDataSerializers.INT);
  private static final EntityDataAccessor<Boolean> OFFICIAL_COMPARISON =
      SynchedEntityData.defineId(ChaoEntity.class, EntityDataSerializers.BOOLEAN);
  private static final EntityDataAccessor<String> SOURCE_REFERENCE_RIG =
      SynchedEntityData.defineId(ChaoEntity.class, EntityDataSerializers.STRING);
  private static final EntityDataAccessor<Optional<net.minecraft.core.BlockPos>> TOY_TARGET =
      SynchedEntityData.defineId(ChaoEntity.class,EntityDataSerializers.OPTIONAL_BLOCK_POS);
  private static final EntityDataAccessor<String> TOY_KIND =
      SynchedEntityData.defineId(ChaoEntity.class, EntityDataSerializers.STRING);
  private static final EntityDataAccessor<Long> TOY_UNTIL =
      SynchedEntityData.defineId(ChaoEntity.class, EntityDataSerializers.LONG);
  private static final EntityDataAccessor<Long> TOY_KICK_STARTED =
      SynchedEntityData.defineId(ChaoEntity.class, EntityDataSerializers.LONG);
  private static final EntityDataAccessor<Integer> BEHAVIOR =
      SynchedEntityData.defineId(ChaoEntity.class, EntityDataSerializers.INT);
  private static final EntityDataAccessor<Long> BEHAVIOR_STARTED =
      SynchedEntityData.defineId(ChaoEntity.class, EntityDataSerializers.LONG);
  private static final EntityDataAccessor<Integer> BEHAVIOR_DURATION =
      SynchedEntityData.defineId(ChaoEntity.class, EntityDataSerializers.INT);
  private static final EntityDataAccessor<String> BEHAVIOR_PARTNER =
      SynchedEntityData.defineId(ChaoEntity.class, EntityDataSerializers.STRING);
  private static final EntityDataAccessor<String> BEHAVIOR_DETAIL =
      SynchedEntityData.defineId(ChaoEntity.class, EntityDataSerializers.STRING);
  public enum ItemAction { NONE, THINK, INSPECT, EAT, TRAIN, REJECT, DELIGHT, GARDEN, PLANT }
  public enum Behavior { NONE, GREET, NUZZLE, CHEER, CRY, COWER, TANTRUM, DIZZY, GROGGY,
    COUGH, SNEEZE, SCRATCH, HICCUP, STOMACH_ACHE, RUNNY_NOSE, TALK, GOSSIP, HUG,
    HOLD_HANDS, SHAKE_TREE, SANDCASTLE, DANCE, LEARN, FLEE, BULLY, INTERVENE,
    BREAK_DANCE, DRAW, SING, INSTRUMENT, RELAX, BIRTHDAY, PARTY, HATCH, WATER }
  private static final Set<String> REFERENCE_RIGS = Set.of("baby","swim","fly","run","power",
      "hero","hero_fly","hero_power","dark","dark_run","dark_swim","golden","baghead",
      "dad","dragon","ender","tails","knuckles","emerald","creeper");
  public UUID owner;
  private ChaoState state;
  private String clientEncoded = "";
  private boolean stay;
  private int petCooldown, feedCooldown, breedingCooldown;
  private boolean publishingBredEgg;
  private int gardenGlideTicks;
  private boolean usedGardenGlide;
  private boolean pendingRebirth,pendingNaturalDeath;
  private boolean resolvingLifecycle,returningLifecycleItems,returningHeldItem;
  private boolean wardrobeChoiceMade,wardrobeChoiceAccepted,committingWardrobe;
  private boolean pendingDeathItems;
  private long wakeGraceUntil;
  private UUID offeredBy;
  private long nextItemPickup;
  private final LinkedHashMap<UUID,Integer> conversations = new LinkedHashMap<>();
  private UUID friendIdentity;
  private long nextSocial, nextBehavior;
  private final java.util.Random simulationRandom = new java.util.Random();

  public ChaoEntity(EntityType<? extends ChaoEntity> type, Level level) {
    super(type, level);
    state =
        level.isClientSide
            ? ChaoState.decode(entityData.get(STATE))
            : ChaoState.random(simulationRandom,GardenConfig.NEWBORN_EYE_CHANCE.get());
    setPersistenceRequired();
    if (!level.isClientSide) sync();
  }

  public static AttributeSupplier.Builder attributes() {
    return Animal.createMobAttributes()
        .add(Attributes.MAX_HEALTH, 16)
        .add(Attributes.MOVEMENT_SPEED, .20)
        .add(Attributes.ATTACK_DAMAGE, 2)
        .add(Attributes.FOLLOW_RANGE, 16);
  }

  @Override
  protected void defineSynchedData(SynchedEntityData.Builder b) {
    super.defineSynchedData(b);
    b.define(STATE, new ChaoState().encode());
    b.define(ORIGIN_EGG_COAT, "");
    b.define(RESTING, false);
    b.define(EATING, 0);
    b.define(AFFECTION, 0);
    b.define(GLIDING, false);
    b.define(COMPANION_KEEPER, -1);
    b.define(COMPANION_GRIP, 0);
    b.define(ITEM_ACTION, 0);
    b.define(ITEM_STARTED, 0L);
    b.define(ITEM_DURATION, 0);
    b.define(OFFICIAL_COMPARISON, false);
    b.define(SOURCE_REFERENCE_RIG, "");
    b.define(TOY_KIND, "");
    b.define(TOY_TARGET,Optional.empty());
    b.define(TOY_UNTIL, 0L);
    b.define(TOY_KICK_STARTED, -1L);
    b.define(BEHAVIOR, 0);
    b.define(BEHAVIOR_STARTED, 0L);
    b.define(BEHAVIOR_DURATION, 0);
    b.define(BEHAVIOR_PARTNER, "");
    b.define(BEHAVIOR_DETAIL, "");
  }

  public ChaoState state() {
    if (level().isClientSide) {
      String s = entityData.get(STATE);
      if (!s.equals(clientEncoded)) {
        clientEncoded = s;
        state = ChaoState.decode(s);
      }
    }
    return state;
  }

  public void setState(ChaoState s) {
    com.chaogarden.ChaoVariants.normalizeRetired(s);
    state = s;
    sync();
  }

  /** The actual shell snapshot is separate from evolving body genes and coats. */
  public void setOriginEggCoat(ChaoState coat) { entityData.set(ORIGIN_EGG_COAT,coat==null?"":coat.encode()); }
  public ChaoState originEggCoat() {
    String encoded=entityData.get(ORIGIN_EGG_COAT);
    return encoded.isEmpty()?ChaoState.decode(state().encode()):ChaoState.decode(encoded);
  }

  public void sync() {
    if (state != null) {
      entityData.set(STATE, state.encode());
      if (!level().isClientSide) updateMovementSpeed();
    }
  }

  /** Vanilla head equipment is synchronized and survives held/companion snapshots. */
  public String getChaoHat() {
    return com.chaogarden.market.ChaoHats.kind(getItemBySlot(EquipmentSlot.HEAD));
  }

  public boolean isResting() {
    return entityData.get(RESTING);
  }

  public boolean isEating() {
    return getItemAction()==ItemAction.EAT || entityData.get(EATING) > 0;
  }

  /** A short, synchronized reaction to actual petting, independent of inherited/save data. */
  public int getAffectionTicks() {
    return entityData.get(AFFECTION);
  }

  public boolean isBeingPetted() {
    return getAffectionTicks() > 0;
  }

  public ItemAction getItemAction() {
    return ItemAction.values()[Math.max(0, Math.min(ItemAction.values().length-1, entityData.get(ITEM_ACTION)))];
  }
  public boolean isHandlingItem() { return getItemAction()!=ItemAction.NONE; }
  public ItemStack getHeldChaoItem() { return getMainHandItem(); }
  public int getItemActionTicks() {
    return Math.max(0, entityData.get(ITEM_DURATION)-(int)(level().getGameTime()-entityData.get(ITEM_STARTED)));
  }
  public float getItemActionProgress(float partialTick) {
    int duration=entityData.get(ITEM_DURATION);
    return duration<=0 ? 0 : net.minecraft.util.Mth.clamp(
        ((float)(level().getGameTime()-entityData.get(ITEM_STARTED))+partialTick)/duration,0,1);
  }
  private void itemAction(ItemAction action,int duration) {
    entityData.set(ITEM_ACTION,action.ordinal());
    entityData.set(ITEM_STARTED,level().getGameTime());
    entityData.set(ITEM_DURATION,Math.max(0,duration));
  }
  public boolean officialComparison() { return entityData.get(OFFICIAL_COMPARISON); }
  public void setOfficialComparison(boolean value) { entityData.set(OFFICIAL_COMPARISON,value); }
  public String sourceReferenceKey() { return entityData.get(SOURCE_REFERENCE_RIG); }
  public void setSourceReferenceKey(String value) {
    if (value==null || (!value.isEmpty() && !REFERENCE_RIGS.contains(value)))
      throw new IllegalArgumentException("Unknown official Chao reference rig: "+value);
    entityData.set(SOURCE_REFERENCE_RIG,value);
  }
  public net.minecraft.core.BlockPos toyTarget(){return entityData.get(TOY_TARGET).orElse(null);}
  public void setToyTarget(net.minecraft.core.BlockPos pos){entityData.set(TOY_TARGET,Optional.ofNullable(pos==null?null:pos.immutable()));}
  public String toyPlayingKind() { return toyPlayingTicks()>0 ? entityData.get(TOY_KIND) : ""; }
  public long toyPlayingUntil() { return entityData.get(TOY_UNTIL); }
  public int toyPlayingTicks() {
    return com.chaogarden.toys.ToyInteractionRules.remainingTicks(entityData.get(TOY_KIND),entityData.get(TOY_UNTIL),level().getGameTime());
  }
  public int toyKickElapsed() {
    long start=entityData.get(TOY_KICK_STARTED),elapsed=level().getGameTime()-start;
    return !toyPlayingKind().equals("chao_ball")||start<0||elapsed<0
        ||elapsed>=com.chaogarden.toys.ToyInteractionRules.BALL_KICK_TICKS?-1:(int)elapsed;
  }
  public void startToyKick(){entityData.set(TOY_KICK_STARTED,level().getGameTime());}
  public void setToyPlaying(String kind,int remainingTicks) {
    entityData.set(TOY_KICK_STARTED,-1L);
    boolean active=remainingTicks>0&&!kind.isEmpty();
    entityData.set(TOY_KIND,active ? kind : "");
    if(!active)setToyTarget(null);
    // An empty session has no deadline. Clients can be a tick behind server game time.
    entityData.set(TOY_UNTIL,active ? level().getGameTime()+remainingTicks : 0L);
  }

  private Behavior storedBehavior() {
    return Behavior.values()[Math.max(0,Math.min(Behavior.values().length-1,entityData.get(BEHAVIOR)))];
  }
  public Behavior getBehavior() { return getBehaviorTicks()>0 ? storedBehavior() : Behavior.NONE; }
  public int getBehaviorTicks() {
    return Math.max(0,entityData.get(BEHAVIOR_DURATION)-(int)(level().getGameTime()-entityData.get(BEHAVIOR_STARTED)));
  }
  private int lastInstrumentCueElapsed=-1;
  public boolean claimInstrumentCue(int elapsed){if(lastInstrumentCueElapsed==elapsed)return false;lastInstrumentCueElapsed=elapsed;return true;}
  public int getBehaviorDuration(){return entityData.get(BEHAVIOR_DURATION);}
  public float getBehaviorElapsed(float partial){return Math.max(0,Math.min(getBehaviorDuration(),level().getGameTime()-entityData.get(BEHAVIOR_STARTED)+partial));}
  public float getBehaviorProgress(float partialTick) {
    int duration=entityData.get(BEHAVIOR_DURATION);
    return duration<=0?0:net.minecraft.util.Mth.clamp(
        ((float)(level().getGameTime()-entityData.get(BEHAVIOR_STARTED))+partialTick)/duration,0,1);
  }
  public UUID behaviorPartner() {
    String key=entityData.get(BEHAVIOR_PARTNER);
    try{return key.isEmpty()?null:UUID.fromString(key);}catch(IllegalArgumentException ignored){return null;}
  }
  public boolean isBehaving() { return getBehavior()!=Behavior.NONE; }
  public String behaviorDetail() { return entityData.get(BEHAVIOR_DETAIL); }
  public void setBehaviorDetail(String detail) {
    entityData.set(BEHAVIOR_DETAIL,detail==null?"":detail.substring(0,Math.min(64,detail.length())));
  }
  public void startBehavior(Behavior action,int duration) {
    if(level().isClientSide || action==null)return;
    lastInstrumentCueElapsed=-1;
    if(isSocialBehavior(storedBehavior()))clearBehavior();
    entityData.set(BEHAVIOR,action.ordinal());entityData.set(BEHAVIOR_STARTED,level().getGameTime());
    entityData.set(BEHAVIOR_DURATION,Math.max(0,Math.min(24000,duration)));
    entityData.set(BEHAVIOR_PARTNER,"");entityData.set(BEHAVIOR_DETAIL,"");getNavigation().stop();
  }
  public void setBehaviorPartner(UUID partner) {
    entityData.set(BEHAVIOR_PARTNER,partner==null?"":partner.toString());
  }
  public void clearBehavior() {
    UUID partner=behaviorPartner();Behavior previous=storedBehavior();
    clearBehaviorLocal();
    if(!level().isClientSide&&isSocialBehavior(previous)&&partner!=null
        &&((ServerLevel)level()).getEntity(partner) instanceof ChaoEntity other
        &&getUUID().equals(other.behaviorPartner()))other.clearBehaviorLocal();
  }
  private void clearBehaviorLocal() {
    entityData.set(BEHAVIOR,0);entityData.set(BEHAVIOR_DURATION,0);
    entityData.set(BEHAVIOR_PARTNER,"");
    entityData.set(BEHAVIOR_DETAIL,"");
  }
  /** Hatching changes are applied by the egg once; this hook is only the visible reaction. */
  public void hatchReaction(boolean rough,Player player) {
    if(rough)startBehavior(Behavior.DIZZY,80);
    else if(!state().adult)startBehavior(Behavior.HATCH,com.chaogarden.core.ChaoAnimationRules.HATCH_TICKS);
  }
  private void disturbSleep() {
    state.onSleepDisturbed();entityData.set(RESTING,false);
    wakeGraceUntil=level().getGameTime()+com.chaogarden.core.ChaoPreferences.recoveryTicks(state,200);
  }
  public UUID stableFriendIdentity() { if(friendIdentity==null)friendIdentity=getUUID();return friendIdentity; }
  public int completedConversations(UUID other) {
    if(level() instanceof ServerLevel world&&world.getEntity(other) instanceof ChaoEntity friend)
      other=friend.stableFriendIdentity();
    return conversations.getOrDefault(other,0);
  }
  public CompoundTag exportRelationships() {
    var data=new CompoundTag();data.putUUID("FriendIdentity",stableFriendIdentity());
    data.putLong("NextSocial",Math.max(0,nextSocial-level().getGameTime()));
    var friends=new net.minecraft.nbt.ListTag();
    for(var entry:conversations.entrySet()) {
      var friend=new CompoundTag();friend.putUUID("Friend",entry.getKey());friend.putInt("Talks",entry.getValue());friends.add(friend);
    }
    data.put("ChaoFriends",friends);return data;
  }
  public void restoreRelationships(CompoundTag data) {
    friendIdentity=data.hasUUID("FriendIdentity")?data.getUUID("FriendIdentity"):getUUID();
    nextSocial=level().getGameTime()+Math.max(0,Math.min(24000,data.getLong("NextSocial")));
    conversations.clear();var friends=data.getList("ChaoFriends",net.minecraft.nbt.Tag.TAG_COMPOUND);
    for(int i=0;i<Math.min(32,friends.size());i++) {
      var friend=friends.getCompound(i);
      if(friend.hasUUID("Friend")&&!friendIdentity.equals(friend.getUUID("Friend")))
        conversations.put(friend.getUUID("Friend"),Math.max(0,Math.min(65535,friend.getInt("Talks"))));
    }
  }
  public long nextSocialTime() { return nextSocial; }
  public int birthdayYear() {
    int interval=GardenConfig.BIRTHDAY_YEAR_TICKS.get();
    return interval<=0||!state.adult?0:state.adultAgeTicks/interval;
  }
  public boolean canStartBirthday() {
    return birthdayYear()>state.traitInt("birthday.year",0)&&canBeginSocial();
  }
  public boolean startBirthdayParty() {
    if(level().isClientSide||!canStartBirthday()||!state.setTraitInt("birthday.year",birthdayYear()))return false;
    // Spend the calendar credit before starting. Interrupting or saving cannot
    // mint another serving for this adult year.
    startBehavior(Behavior.BIRTHDAY,500);sync();return true;
  }
  public String birthdayPartyKey(){return stableFriendIdentity()+":"+state.traitInt("birthday.year",0);}
  public boolean joinBirthdayParty(ChaoEntity host) {
    if(host==null||host==this||!canBeginSocial()||host.getBehavior()!=Behavior.BIRTHDAY
        ||host.getBehaviorTicks()<40||distanceToSqr(host)>16||host.birthdayPartyKey().equals(state.trait("birthday.party"))
        ||!state.setTrait("birthday.party",host.birthdayPartyKey()))return false;
    startBehavior(Behavior.PARTY,host.getBehaviorTicks());setBehaviorPartner(host.getUUID());return true;
  }
  public static boolean isBirthdayServing(ItemStack stack) {
    return stack.is(Items.CAKE)&&stack.getOrDefault(net.minecraft.core.component.DataComponents.CUSTOM_DATA,
        net.minecraft.world.item.component.CustomData.EMPTY).copyTag().getBoolean("ChaoBirthdayServing");
  }
  private void receiveBirthdayServing() {
    var cake=new ItemStack(Items.CAKE);var data=new CompoundTag();data.putBoolean("ChaoBirthdayServing",true);
    cake.set(net.minecraft.core.component.DataComponents.CUSTOM_DATA,net.minecraft.world.item.component.CustomData.of(data));
    cake.set(net.minecraft.core.component.DataComponents.CUSTOM_NAME,Component.literal("Chao Birthday Cake"));
    if(getMainHandItem().isEmpty()&&!isHandlingItem())beginHeldItem(cake,null);else spawnAtLocation(cake);
  }
  public long nextBehaviorTime() { return nextBehavior; }
  public void deferBehavior(int ticks) { nextBehavior=level().getGameTime()+Math.max(1,ticks); }
  public static boolean isSocialBehavior(Behavior b) {
    return b==Behavior.TALK||b==Behavior.GOSSIP||b==Behavior.HUG||b==Behavior.HOLD_HANDS;
  }
  public boolean canBeginSocial() {
    return isAlive()&&!isRemoved()&&!state.dead&&!state.sleeping&&!state.isIll()
        &&state.hunger>=25&&state.energy>=20&&!isHandlingItem()&&!isBehaving()
        &&!isCocooned()&&!stay&&toyPlayingTicks()==0&&getAffectionTicks()==0
        &&!com.chaogarden.activities.ActivityManager.isBusy(this)
        &&!com.chaogarden.companion.CompanionContent.isManaged(this)
        &&level().getGameTime()>=nextSocial;
  }
  public boolean startSocialSession(ChaoEntity other,Behavior action,int duration) {
    if(level().isClientSide||other==null||other==this||other.level()!=level()
        ||!isSocialBehavior(action)||!canBeginSocial()||!other.canBeginSocial()
        ||distanceToSqr(other)>9)return false;
    if(action==Behavior.GOSSIP&&completedConversations(other.getUUID())<5)return false;
    if((action==Behavior.HUG||action==Behavior.HOLD_HANDS)&&completedConversations(other.getUUID())<10)return false;
    startBehavior(action,duration);other.startBehavior(action,duration);
    setBehaviorPartner(other.getUUID());other.setBehaviorPartner(getUUID());
    nextSocial=other.nextSocial=level().getGameTime()+Math.max(20,duration)+600;
    return true;
  }
  private void recordConversation(UUID other) {
    conversations.put(other,Math.min(65535,completedConversations(other)+1));
    while(conversations.size()>32)conversations.remove(conversations.keySet().iterator().next());
  }
  private void tickBehavior() {
    Behavior action=storedBehavior();if(action==Behavior.NONE)return;
    if(lifecyclePending()||isCocooned()||isHandlingItem()||state.dead||state.sleeping){clearBehavior();return;}
    if(getBehaviorTicks()>0)return;
    if(isSocialBehavior(action)&&level() instanceof ServerLevel world) {
      UUID partner=behaviorPartner();
      if(partner!=null&&world.getEntity(partner) instanceof ChaoEntity other
          &&other.isAlive()&&other.storedBehavior()==action&&getUUID().equals(other.behaviorPartner())
          &&entityData.get(BEHAVIOR_STARTED).equals(other.entityData.get(BEHAVIOR_STARTED))) {
        // Both bodies expire on the same game tick. The lower UUID commits the
        // shared result once; the other waits a short grace for its tick order.
        if(getUUID().compareTo(other.getUUID())>0
            &&level().getGameTime()<entityData.get(BEHAVIOR_STARTED)+entityData.get(BEHAVIOR_DURATION)+4)return;
        if(getUUID().compareTo(other.getUUID())<0&&distanceToSqr(other)<=16) {
          if(action==Behavior.TALK||action==Behavior.GOSSIP) {
            recordConversation(other.stableFriendIdentity());other.recordConversation(stableFriendIdentity());
          }
          state.onSocialInteraction(true);
          other.state.onSocialInteraction(true);
          sync();other.sync();other.clearBehaviorLocal();other.deferBehavior(80);
        }
      }
    } else {
      switch(action) {
        case CRY -> state.addEmotion(com.chaogarden.core.ChaoEmotion.CRY,-55);
        case COWER,FLEE -> state.addEmotion(com.chaogarden.core.ChaoEmotion.FEAR,-55);
        case TANTRUM -> state.addEmotion(com.chaogarden.core.ChaoEmotion.ANGER,-45);
        case DIZZY -> state.addEmotion(com.chaogarden.core.ChaoEmotion.SURPRISE,-60);
        case CHEER -> state.addEmotion(com.chaogarden.core.ChaoEmotion.JOY,-40);
        case RELAX -> state.addEmotion(com.chaogarden.core.ChaoEmotion.RELAX,-25);
        case BIRTHDAY -> {state.onSocialInteraction(true);receiveBirthdayServing();}
        case PARTY -> {state.onSocialInteraction(true);state.happiness=Math.min(100,state.happiness+1);}
        case GROGGY -> state.addEmotion(com.chaogarden.core.ChaoEmotion.TIREDNESS,-2000);
        case GREET,NUZZLE -> {state.onSocialInteraction(true);state.bond=Math.min(100,state.bond+1);}
        case DRAW,SING,DANCE,BREAK_DANCE,SANDCASTLE,INSTRUMENT -> {
          state.addEmotion(com.chaogarden.core.ChaoEmotion.BOREDOM,-2500);
          state.addEmotion(com.chaogarden.core.ChaoEmotion.JOY,20);
        }
        default -> { }
      }
    }
    clearBehaviorLocal();deferBehavior(60+getRandom().nextInt(100));
  }

  /** Synced only during actual short ledge flight, not a pose inferred from Fly level. */
  public boolean isChaoGliding() {
    return entityData.get(GLIDING);
  }

  public boolean isCocooned() {
    return state() != null && state().cocoonTicks > 0;
  }
  public boolean isDeathCocoon() { return isCocooned()&&state().dead; }
  public boolean isRebirthCocoon() { return isCocooned()&&!state().adult&&state().reincarnations>0&&!state().dead; }

  @Override
  public float getAgeScale() {
    return state() != null && !state().adult ? .8f : 1f;
  }

  @Override
  public void onSyncedDataUpdated(EntityDataAccessor<?> key) {
    super.onSyncedDataUpdated(key);
    if (STATE.equals(key)) refreshDimensions();
    if (COMPANION_GRIP.equals(key)) noPhysics = isCompanionAttached();
  }

  /** Attachment identity is tracked to every observing client. It is temporary
   * projection state, never a second saved creature or an inventory-side clone. */
  public int companionKeeperId() { return entityData.get(COMPANION_KEEPER); }
  public int companionCarryPosition() { return entityData.get(COMPANION_GRIP); }
  public boolean isCompanionGripping() { return companionCarryPosition()==2; }
  public boolean isCompanionAttached() { return companionCarryPosition()!=0; }
  private float carryBack,oldCarryBack;
  private boolean carryInitialized;
  public float companionBackBlend(float partial) { return net.minecraft.util.Mth.lerp(partial,oldCarryBack,carryBack); }
  public void setCompanionGrip(Player keeper,boolean gripping) { setCompanionCarry(keeper,gripping?2:0); }
  public void setCompanionCarry(Player keeper,int position) {
    boolean active=position>0 && position<=2 && keeper!=null && keeper.isAlive() && keeper.level()==level();
    entityData.set(COMPANION_KEEPER,active?keeper.getId():-1);
    entityData.set(COMPANION_GRIP,active?position:0);
    noPhysics=active;setNoGravity(active);
    if(active)fallDistance=0;
  }

  /** Damage recovery must never intercept a natural cocoon, rebirth or robot outcome. */
  public boolean mayRecoverFromDamage() {
    return !isRemoved() && !state.dead && !isCocooned() && !lifecyclePending()
        && !committingWardrobe && !pendingDeathItems;
  }

  @Override public void moveTo(double x,double y,double z,float yaw,float pitch) {
    boolean published=isAddedToLevel();
    super.moveTo(x,y,z,yaw,pitch);
    // Both vanilla teleport entry points use moveTo. Walking still follows the garden's normal containment rule.
    if(published && level() instanceof net.minecraft.server.level.ServerLevel server)
      com.chaogarden.activities.GardenRegistry.get(server.getServer()).detect(this,true);
  }

  @Override public boolean canBeLeashed(){return !com.chaogarden.kindergarten.KindergartenContent.isBoarded(this)&&super.canBeLeashed();}

  public boolean canCare(Player p) {
    return !committingWardrobe && !com.chaogarden.kindergarten.KindergartenContent.isBoarded(this) && !lifecyclePending() && !state.dead && state.intTrait("content.construction")==0
        &&(owner == null || owner.equals(p.getUUID()) || p.getAbilities().instabuild);
  }

  private boolean hasActiveGarden() {
    return level() instanceof net.minecraft.server.level.ServerLevel server && com.chaogarden.activities.GardenRegistry.get(server.getServer()).activeResident(this);
  }

  public boolean isStaying() {
    return stay;
  }

  public void setStaying(boolean value) {
    stay = value;
    if (value) getNavigation().stop();
  }

  public void prepareForActivity() {
    returnHeldItem(false);
    clearBehavior();
    state.sleeping = false;
    entityData.set(RESTING, false);
    entityData.set(EATING, 0);
    entityData.set(AFFECTION, 0);
    entityData.set(GLIDING, false);
    getNavigation().stop();
    sync();
  }

  @Override
  public boolean isBaby() {
    return state() != null && !state().adult;
  }

  @Override
  public boolean removeWhenFarAway(double distance) {
    return false;
  }

  @Override
  public boolean shouldBeSaved() {
    return !com.chaogarden.companion.CompanionContent.isManaged(this)
        && !com.chaogarden.kindergarten.KindergartenContent.isBoarded(this) && super.shouldBeSaved();
  }

  @Override
  public boolean isFood(ItemStack s) {
    return s.getItem() instanceof CareItem c && c.kind == CareItem.Kind.FRUIT;
  }

  @Override
  protected void registerGoals() {
    goalSelector.addGoal(0, com.chaogarden.activities.ActivityManager.goalFor(this));
    goalSelector.addGoal(0, new com.chaogarden.companion.CompanionGoal(this));
    goalSelector.addGoal(1, new FloatGoal(this) {
      @Override public boolean canUse() {
        return !toyPlayingKind().equals("chao_lifesaver")&&super.canUse();
      }
      @Override public void tick() {
        // Untrained Chao retain the emergency flail/escape behavior. Trained
        // swimmers paddle up only while submerged rather than continually hop.
        if (!isInWater() || !LocomotionRules.canSwim(state())) super.tick();
        else if (isUnderWater()) getJumpControl().jump();
      }
    });
    goalSelector.addGoal(
        1,
        new Goal() {
          {
            setFlags(EnumSet.of(Flag.MOVE, Flag.JUMP));
          }

          @Override
          public boolean canUse() {
            return isResting() || isEating() || isBeingPetted() || (isHandlingItem()&&getItemAction()!=ItemAction.GARDEN&&getItemAction()!=ItemAction.PLANT) || isCocooned() || stay
                || getBehavior()==Behavior.LEARN || getBehavior()==Behavior.HATCH;
          }

          @Override
          public void start() {
            getNavigation().stop();
          }

          @Override
          public void tick() {
            getNavigation().stop();
          }
        });
    goalSelector.addGoal(1, new com.chaogarden.interaction.ChaoGardeningGoal(this));
    goalSelector.addGoal(2, new BreedGoal(this, .75));
    goalSelector.addGoal(2, new com.chaogarden.interaction.ChaoBehaviorGoal(this));
    goalSelector.addGoal(3, new com.chaogarden.interaction.ChaoFruitSeekingGoal(this));
    goalSelector.addGoal(
        3,
        new TemptGoal(
            this,
            1.0,
            Ingredient.of(
                ChaoGarden.item("garden_fruit"),
                ChaoGarden.item("chao_fruit"),
                ChaoGarden.item("heart_fruit")),
            false));
    goalSelector.addGoal(
        4,
        new Goal() {
          private Player keeper;

          {
            setFlags(EnumSet.of(Flag.MOVE));
          }

          @Override
          public boolean canUse() {
            keeper = owner == null ? null : level().getPlayerByUUID(owner);
            return !stay
                && !hasActiveGarden()
                && state().bond >= 20
                && keeper != null
                && distanceToSqr(keeper) > 36
                && distanceToSqr(keeper) < 400;
          }

          @Override
          public boolean canContinueToUse() {
            return keeper != null
                && !hasActiveGarden()
                && !stay
                && distanceToSqr(keeper) > 9
                && distanceToSqr(keeper) < 576;
          }

          @Override
          public void tick() {
            getNavigation().moveTo(keeper, 1.15);
          }
        });
    goalSelector.addGoal(6, new WaterAvoidingRandomStrollGoal(this, .65));
    goalSelector.addGoal(5, new com.chaogarden.interaction.ChaoPickupItemGoal(this));
    goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 6));
    goalSelector.addGoal(8, new RandomLookAroundGoal(this));
  }

  @Override
  public void aiStep() {
    if(level().isClientSide) {
      float target=isCompanionGripping()?1:0;
      if(!carryInitialized){carryBack=oldCarryBack=target;carryInitialized=true;}
      oldCarryBack=carryBack;
      carryBack=net.minecraft.util.Mth.approach(carryBack,target,.125f);
    }
    if(!level().isClientSide&&com.chaogarden.toys.ToyPreview.tick(this)){super.aiStep();return;}
    // Ambient school children use real vanilla navigation/physics, while their
    // controlled goal owns the activity. They never run keeper care/life simulation.
    if(this instanceof com.chaogarden.kindergarten.KindergartenPupil0257){super.aiStep();return;}
    if(com.chaogarden.kindergarten.KindergartenContent.tickStudent(this))return;
    if(committingWardrobe || pendingDeathItems)return;
    if(state().intTrait("content.construction")>0) {
      getNavigation().stop();setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO);
      if(!level().isClientSide&&tickCount%20==0)sync();return;
    }
    if (!level().isClientSide) {updateMovementSpeed();tickBehavior();}
    super.aiStep();
    if (level().isClientSide) return;
    // Chao flail when they cannot swim; this is not a drowning/death penalty.
    if (isInWater()) setAirSupply(getMaxAirSupply());
    // Care gestures/cooldowns run on the companion body too; only its long
    // life simulation is paused. Otherwise one meal would lock gifts forever.
    if (petCooldown > 0) petCooldown--;
    if (feedCooldown > 0) feedCooldown--;
    if (entityData.get(EATING) > 0) entityData.set(EATING, entityData.get(EATING) - 1);
    if (getAffectionTicks() > 0) entityData.set(AFFECTION, getAffectionTicks() - 1);
    // Pending outcomes own their held/worn stacks. Resolve retries before a
    // resumed gift can consume, equip or collect anything else.
    if(com.chaogarden.cwe.OmochaoLifecycle.resumePending(this))return;
    if(tickPendingLifecycle())return;
    tickItemHandling();
    if (com.chaogarden.activities.ActivityManager.isBusy(this)) {
      clearBehavior();
      entityData.set(GLIDING, false);
      com.chaogarden.activities.ActivityManager.tickParticipant(this);
      return;
    }
    if (com.chaogarden.companion.CompanionContent.tickChao(this)) {
      clearBehavior();
      entityData.set(GLIDING, false);
      return;
    }
    if (breedingCooldown > 0) breedingCooldown--;
    state.refreshMonsterEvolution(GardenConfig.MONSTER_EVOLUTION.get(),GardenConfig.MONSTER_TRANSPARENCY_FIX.get());
    if (state.wantsSleep()&&level().getGameTime()>=wakeGraceUntil) state.sleeping = true;
    if (state.readyToWake() || isInWater()) state.sleeping = false;
    entityData.set(RESTING, state.sleeping);
    // The untrained baby's water reaction is distress and kicking, not damage.
    // These small splashes accompany its actual FloatGoal escape attempts.
    if(isInWater()&&!LocomotionRules.canSwim(state)&&!isCocooned()
        &&!com.chaogarden.toys.ToyInteractionRules.supportedFloat(toyPlayingKind())&&tickCount%8==0
        &&level() instanceof ServerLevel water) {
      water.sendParticles(ParticleTypes.SPLASH,getX(),getY()+getBbHeight()*.62,getZ(),
          2,.12,.04,.12,.015);
    }
    var change =
        state.tick(
            GardenConfig.CHILD_TICKS.get(), GardenConfig.ADULT_TICKS.get(), simulationRandom);
    state.onGardenMotion(getDeltaMovement().horizontalDistanceSqr()>.002);
    if("omochao".equals(state.character)
        && (change==ChaoState.Transition.DIED||change==ChaoState.Transition.REINCARNATED)) {
      com.chaogarden.cwe.OmochaoLifecycle.finish(this,change);
      return;
    }
    if (change == ChaoState.Transition.DIED && !GardenConfig.NATURAL_DEATH.get()) {
      state.reincarnate();
      change = ChaoState.Transition.REINCARNATED;
    }
    if (change == ChaoState.Transition.REINCARNATED) {
      pendingRebirth=true;returnHeldItem(false);clearBehavior();getNavigation().stop();sync();
      return;
    }
    if (change == ChaoState.Transition.DIED) {
      pendingNaturalDeath=true;state.cocoonTicks=200;
      returnHeldItem(false);clearBehavior();getNavigation().stop();sync();
      return;
    }
    if (change == ChaoState.Transition.EVOLVED) {
      com.chaogarden.cwe.CweContent.onFirstEvolution(this);
      particles(ParticleTypes.HAPPY_VILLAGER, 24);
      playSound(ChaoSounds.EVOLVE.get(), .5f, getVoicePitch());
      sync();
    }
    if (state.cocoonTicks > 0) {
      if (tickCount % 10 == 0) particles(ParticleTypes.ENCHANT, 3);
      getNavigation().stop();
    }
    if (tickCount % 200 == 0 && !state.sleeping && !isCocooned()) {
      if (isInWater()) state.train(ChaoState.SWIM, 4);
      else if (getDeltaMovement().horizontalDistanceSqr() > .002) state.train(ChaoState.RUN, 2);
      if (!onGround() && !isInWater()) state.train(ChaoState.FLY, 2);
    }
    // Stats have practical effects as well as evolution appearances.
    if (tickCount % 100 == 0) {
      getAttribute(Attributes.MAX_HEALTH)
          .setBaseValue(16 + state.points[ChaoState.STAMINA] / 250.0);
      getAttribute(Attributes.ARMOR)
          .setBaseValue(Math.min(12, state.points[ChaoState.POWER] / 250.0));
      if (state.hunger > 25 && getHealth() < getMaxHealth()) heal(1);
    }
    tickGardenGlide();
    // Shiny coats use a reflective surface rather than detached white sparkle
    // particles. Chaos forms retain their separate occasional aura.
    if (state.immortal && tickCount % 80 == 0) particles(ParticleTypes.END_ROD, 1);
    if (tickCount % 20 == 0) sync();
  }

  private boolean tickPendingLifecycle() {
    if(!pendingRebirth&&!pendingNaturalDeath)return false;
    if(resolvingLifecycle)return true;
    resolvingLifecycle=true;
    try {
    getNavigation().stop();
    if(state.cocoonTicks>0)state.cocoonTicks--;
    if(state.cocoonTicks>0) {
      if(tickCount%10==0)particles(ParticleTypes.ENCHANT,2);
      if(tickCount%20==0)sync();return true;
    }
    if(pendingNaturalDeath) {
      if(!releaseLifecycleItems()){state.cocoonTicks=20;sync();return true;}
      particles(ParticleTypes.CLOUD,24);com.chaogarden.recovery.ChaoRecovery.lifecycleFinished(this);discard();return true;
    }
    if(!returnHeldItem(false)){state.cocoonTicks=20;sync();return true;}
    var egg=ChaoGarden.CHAO_EGG.get().create(level());
    if(egg==null){state.cocoonTicks=20;return true;}
    egg.setState(state);egg.owner=owner;egg.chaoName=getCustomName();
    egg.setRebornChaoId(getUUID());
    egg.setRebornOriginEggCoat(entityData.get(ORIGIN_EGG_COAT));
    var equipment=new CompoundTag();
    for(var slot:retainedEquipmentSlots()) {
      ItemStack worn=getItemBySlot(slot);
      if(!worn.isEmpty())equipment.put(slot.getName(),worn.copyWithCount(1).save(registryAccess()));
    }
    egg.setRebornEquipment(equipment);egg.setRebornRelationships(exportRelationships());
    egg.moveTo(getX(),getY(),getZ(),getYRot(),0);
    // Only commit removal after the egg enters the world. A failed creation
    // retains both the creature and every real equipment stack for a retry.
    if(!level().addFreshEntity(egg)){state.cocoonTicks=20;return true;}
    for(var slot:retainedEquipmentSlots())setItemSlot(slot,ItemStack.EMPTY);
    pendingRebirth=false;particles(ParticleTypes.HEART,20);com.chaogarden.recovery.ChaoRecovery.lifecycleFinished(this);discard();return true;
    } finally {resolvingLifecycle=false;}
  }

  private boolean lifecyclePending() {
    return pendingRebirth||pendingNaturalDeath||resolvingLifecycle||returningLifecycleItems||returningHeldItem
        ||com.chaogarden.core.OmochaoRules.isOmochao(state)
            &&(state.robotPendingOutcome()>0||state.robotOutputCommitted()||state.intTrait("robot.expired")>0);
  }

  private void updateMovementSpeed() {
    if (state == null) return;
    var attribute=getAttribute(Attributes.MOVEMENT_SPEED);
    if (attribute == null) return;
    double speed=isInWater()?LocomotionRules.waterMovementSpeed(state):LocomotionRules.movementSpeed(state);
    if (attribute.getBaseValue()!=speed) attribute.setBaseValue(speed);
  }

  private void tickGardenGlide() {
    if (onGround() || isInWater()) {
      usedGardenGlide=false;gardenGlideTicks=0;entityData.set(GLIDING,false);return;
    }
    var velocity=getDeltaMovement();
    boolean ready=LocomotionRules.canFly(state) && state.energy>5
        && !isResting() && !isEating() && !isHandlingItem() && !isCocooned() && !stay;
    if (!ready) {entityData.set(GLIDING,false);return;}
    if (!usedGardenGlide && velocity.y<-.08 && velocity.horizontalDistanceSqr()>.0001) {
      usedGardenGlide=true;gardenGlideTicks=LocomotionRules.MAX_GLIDE_TICKS;
    }
    if (gardenGlideTicks>0 && velocity.y<0) {
      gardenGlideTicks--;
      entityData.set(GLIDING,true);
      setDeltaMovement(velocity.x*.99,Math.max(-.12,velocity.y*.6),velocity.z*.99);
      fallDistance=0;
    } else entityData.set(GLIDING,false);
  }

  private static EquipmentSlot[] retainedEquipmentSlots() {
    return new EquipmentSlot[]{EquipmentSlot.HEAD,EquipmentSlot.OFFHAND,EquipmentSlot.CHEST,
        EquipmentSlot.LEGS,EquipmentSlot.FEET};
  }
  /** Natural death bypasses Mob drops; real hats/accessories return once. Rebirth transfers them to its egg. */
  private boolean returnLifecycleEquipment() {
    boolean complete=true;
    for(var slot:retainedEquipmentSlots()) {
      ItemStack worn=getItemBySlot(slot);if(worn.isEmpty())continue;
      setItemSlot(slot,ItemStack.EMPTY);
      var returned=new ItemEntity(level(),getX(),getY()+.15,getZ(),worn.copy());returned.setDefaultPickUpDelay();
      if(!publishReservedItem(slot,worn,returned,false))complete=false;
    }
    return complete;
  }

  /** A completed robotic breakdown returns real held and worn items once. */
  public boolean releaseLifecycleItems() {
    if(level().isClientSide||committingWardrobe||pendingRebirth||returningLifecycleItems||returningHeldItem)return false;
    returningLifecycleItems=true;
    try {
      boolean held=returnHeldItem(false);
      boolean worn=returnLifecycleEquipment();
      return held&&worn;
    } finally {returningLifecycleItems=false;}
  }

  private void particles(net.minecraft.core.particles.SimpleParticleType particle, int count) {
    ((ServerLevel) level())
        .sendParticles(particle, getX(), getY() + .5, getZ(), count, .3, .35, .3, .015);
  }

  @Override
  public InteractionResult mobInteract(Player p, InteractionHand hand) {
    if(!level().isClientSide){com.chaogarden.toys.ToyPreview.stop(this);com.chaogarden.companion.CarryPreview.stop(this);}
    if(com.chaogarden.kindergarten.KindergartenContent.isBoarded(this)){
      if(!level().isClientSide)p.displayClientMessage(Component.literal("Speak to the teacher to collect this student after class."),true);
      return InteractionResult.sidedSuccess(level().isClientSide);
    }
    var stack = p.getItemInHand(hand);
    if (stack.is(ChaoGarden.item("chao_analyzer")))
      return InteractionResult.sidedSuccess(level().isClientSide);
    if (com.chaogarden.activities.ActivityManager.isBusy(this)) {
      if (!level().isClientSide)
        p.displayClientMessage(Component.literal("This Chao is taking part in an activity."), true);
      return InteractionResult.sidedSuccess(level().isClientSide);
    }
    var hat = com.chaogarden.market.ChaoHatItem.interact(this, p, hand);
    if (hat != InteractionResult.PASS) return hat;
    var companion = com.chaogarden.companion.CompanionContent.onInteractChao(this, p, hand);
    if (companion != InteractionResult.PASS) return companion;
    var toy = com.chaogarden.market.MarketContent.interact(this, p, hand);
    if (toy != InteractionResult.PASS) return toy;
    if (!stack.isEmpty()) {
      // Equipment/tools keep their explicit interactions. Every other gift is
      // transferred to the Chao first and considered by the server over time.
      if (stack.is(ChaoGarden.item("garden_guide"))
          ||stack.getItem() instanceof com.chaogarden.activities.GardenBellItem) return InteractionResult.PASS;
      return offerItem(p,hand);
    }
    if (stack.isEmpty()) {
      if (!level().isClientSide) {
        if (!canCare(p)) {
          p.displayClientMessage(Component.literal("This Chao belongs to another keeper."), true);
          return InteractionResult.FAIL;
        }
        if (isHandlingItem()) return InteractionResult.CONSUME;
        if (owner == null) owner = p.getUUID();
        if (p.isShiftKeyDown()) {
          stay = !stay;
          getNavigation().stop();
          p.displayClientMessage(
              Component.literal(
                  stay ? "Chao will stay nearby." : "Chao may wander and follow you."),
              true);
        } else if (petCooldown == 0) {
          boolean disturbed=state.sleeping;
          clearBehavior();
          if(disturbed)disturbSleep();
          state.pet(0);
          petCooldown = 60;
          entityData.set(AFFECTION, com.chaogarden.core.ChaoAnimationRules.PET_TICKS);
          getNavigation().stop();
          state.sleeping = false;
          entityData.set(RESTING,false);
          if(disturbed)startBehavior(Behavior.GROGGY,60);
          sync();
          playSound(ChaoSounds.HAPPY.get(), .45f, getVoicePitch());
        }
      }
      return InteractionResult.sidedSuccess(level().isClientSide);
    }
    return super.mobInteract(p, hand);
  }

  /** Commit point: the held stack has already been removed, never an instant interaction. */
  private void applyCare(CareItem care, Player player) {
    if (care.kind == CareItem.Kind.ANIMAL) state.giveAnimal(care.index,GardenConfig.EXPANDED_RARE_ACTIONS.get(),GardenConfig.MONSTER_EVOLUTION.get());
    else if (care instanceof com.chaogarden.item.CombinedDriveItem drive)
      com.chaogarden.core.DriveRules.apply(state,drive.tier,drive.index,new java.util.Random(random.nextLong()));
    else if (care.kind == CareItem.Kind.DRIVE) state.giveDrive(care.index,GardenConfig.MONSTER_EVOLUTION.get());
    else {
      com.chaogarden.fruit.FruitEffects.apply(state, care.effect,GardenConfig.CWE_FRUIT_REBALANCE.get());
      state.onFruitEaten();
      if (care.effect.equals("heart_fruit")) setInLove(player);
    }
    state.sleeping = false;
    entityData.set(EATING, 30);
    feedCooldown = 20;
    particles(ParticleTypes.HAPPY_VILLAGER, 4);
    sync();
    playSound(care.kind==CareItem.Kind.FRUIT ? SoundEvents.GENERIC_EAT
        : SoundEvents.AMETHYST_BLOCK_CHIME, .4f, 1.5f);
  }

  private boolean canBeginItemHandling() {
    return !lifecyclePending() && isAlive() && !isRemoved() && !state.dead && !isHandlingItem() && !isBehaving() && getMainHandItem().isEmpty()
        && !isCocooned() && !isResting() && toyPlayingTicks()==0
        && !com.chaogarden.care.ChaoCareServices.hasLesson(this)
        && !com.chaogarden.activities.ActivityManager.isBusy(this);
  }

  /** An offer owns one actual item; benefit checks happen only after pondering it. */
  public InteractionResult offerItem(Player player,InteractionHand hand) {
    if (!canCare(player)) {
      if (!level().isClientSide) player.displayClientMessage(Component.literal("This Chao belongs to another keeper."),true);
      return InteractionResult.FAIL;
    }
    if(com.chaogarden.care.ChaoCareServices.hasLesson(this))return InteractionResult.CONSUME;
    if(isBehaving())clearBehavior();
    if (!canBeginItemHandling() || feedCooldown>0) return InteractionResult.CONSUME;
    ItemStack offered=player.getItemInHand(hand);
    if (offered.isEmpty()) return InteractionResult.PASS;
    if (level().isClientSide) return InteractionResult.SUCCESS;
    ItemStack transferred=offered.copyWithCount(1);
    // Assign the real stack before shrinking the source. No callback can
    // accept a second gift once MAINHAND or the action state is occupied.
    beginHeldItem(transferred,player.getUUID());
    if (!player.getAbilities().instabuild) offered.shrink(1);
    if (owner==null) owner=player.getUUID();
    return InteractionResult.CONSUME;
  }

  /** Wardrobe UI offers a real inventory item through the same voluntary sequence. */
  public boolean offerWardrobeItem(Player player,ItemStack source) {
    if(level().isClientSide || player==null || !canCare(player) || source.isEmpty()
        || com.chaogarden.care.ChaoCareServices.accessorySlot(source)==null)return false;
    boolean present=false;
    for(int i=0;i<player.getInventory().getContainerSize();i++)if(player.getInventory().getItem(i)==source){present=true;break;}
    if(!present||com.chaogarden.care.ChaoCareServices.hasLesson(this))return false;
    if(isBehaving())clearBehavior();
    if(!canBeginItemHandling() || feedCooldown>0)return false;
    var slot=com.chaogarden.care.ChaoCareServices.accessorySlot(source);
    if(ItemStack.isSameItemSameComponents(getItemBySlot(slot),source))return false;
    beginHeldItem(source.copyWithCount(1),player.getUUID());source.shrink(1);
    player.getInventory().setChanged();if(owner==null)owner=player.getUUID();return true;
  }

  private void beginHeldItem(ItemStack stack,UUID giver) {
    clearBehavior();
    setItemSlot(EquipmentSlot.MAINHAND,com.chaogarden.market.ChaoHats.canonicalCopy(stack).copyWithCount(1));
    setDropChance(EquipmentSlot.MAINHAND,0); // Our guaranteed drop path preserves all components.
    offeredBy=giver;wardrobeChoiceMade=false;wardrobeChoiceAccepted=false;
    entityData.set(AFFECTION,0);
    getNavigation().stop();
    boolean familiar=com.chaogarden.fruit.GardenTools.isTool(stack)
        || stack.getItem() instanceof com.chaogarden.fruit.FruitSeedItem
        || stack.getItem() instanceof CareItem
        || isBirthdayServing(stack)
        || com.chaogarden.market.ChaoHats.isHat(stack)
        || stack.getItem() instanceof com.chaogarden.care.ChaoMedicineItem
        || stack.getItem() instanceof com.chaogarden.cwe.CweGiftItem;
    itemAction(familiar ? ItemAction.THINK : ItemAction.INSPECT,
        familiar ? com.chaogarden.core.ChaoPreferences.thinkingTicks(state()) : 60);
    com.chaogarden.companion.CompanionContent.itemStateChanged(this);
  }

  public boolean canInspectNearbyItems() {
    return canBeginItemHandling() && !stay && !isInWater() && onGround()
        && !com.chaogarden.companion.CompanionContent.isManaged(this)
        && level().getGameTime()>=nextItemPickup;
  }

  public boolean mayInspectDroppedItem(ItemEntity item) {
    if (item==null || !item.isAlive() || item.getItem().isEmpty() || item.hasPickUpDelay()
        || item.level()!=level() || item.getPersistentData().getLong("ChaoRejectedUntil")>level().getGameTime()) return false;
    UUID target=item.getTarget();
    return target==null || target.equals(getUUID());
  }

  public boolean pickUpForInspection(ItemEntity item) {
    if (level().isClientSide || !canInspectNearbyItems() || !mayInspectDroppedItem(item)
        || distanceToSqr(item)>2.25) return false;
    ItemStack source=item.getItem();
    ItemStack taken=source.copyWithCount(1),remaining=source.copy();remaining.shrink(1);
    onItemPickup(item);take(item,1);
    item.setItem(remaining);
    if (remaining.isEmpty()) item.discard();
    beginHeldItem(taken,null);
    return true;
  }

  private boolean acceptsHeldCare(CareItem care) {
    if (!isAlive() || state.dead || isCocooned() || state.sleeping) return false;
    if (care.kind==CareItem.Kind.FRUIT) {
      if(!com.chaogarden.fruit.FruitEffects.canUse(state,care.effect))return false;
      if (care.effect.equals("heart_fruit") && (!mayBreed() || breedingCooldown>0)) return false;
      return state.hunger<com.chaogarden.core.ChaoPreferences.foodAcceptanceThreshold(state,care.effect);
    }
    if (!com.chaogarden.core.ChaoPreferences.acceptsTraining(state)) return false;
    // Chaos forms do not grow animal parts, but the established gene benefits
    // still train their abilities, so immortality alone is not a refusal.
    if (care.kind==CareItem.Kind.ANIMAL) return !"omochao".equals(state.character);
    if (care instanceof com.chaogarden.item.CombinedDriveItem drive)
      return com.chaogarden.core.DriveRules.canUse(state,drive.tier,drive.index);
    if (care.index<ChaoState.SWIM || care.index>ChaoState.POWER) return false;
    // Core owns both hidden-stat progress and evolution influence eligibility.
    return state.hasDriveBenefit(care.index,GardenConfig.MONSTER_EVOLUTION.get());
  }

  private static boolean wearable(ItemStack gift) {
    return com.chaogarden.care.ChaoCareServices.accessorySlot(gift)!=null;
  }

  private static boolean fashion(ItemStack gift) {
    return wearable(gift)||gift.getItem() instanceof com.chaogarden.cwe.CweGiftItem item
        &&(item.gift.kind()==com.chaogarden.cwe.CweCatalog.Kind.LENS
            ||item.gift.kind()==com.chaogarden.cwe.CweCatalog.Kind.WINGS);
  }

  private boolean acceptsHeldGift(ItemStack gift) {
    if(com.chaogarden.fruit.GardenTools.isTool(gift))return com.chaogarden.fruit.GardenTools.canLearn(this,gift);
    if(gift.getItem() instanceof com.chaogarden.fruit.FruitSeedItem seed)return com.chaogarden.fruit.GardenTools.canPlant(this,seed.fruit);
    if(isBirthdayServing(gift))return com.chaogarden.core.ChaoPreferences.acceptsFood(state,"birthday_cake");
    if(gift.getItem() instanceof CareItem care)return acceptsHeldCare(care);
    if(gift.getItem() instanceof com.chaogarden.care.ChaoMedicineItem medicine)return medicine.canTreat(state);
    if(wearable(gift)) {
      var slot=com.chaogarden.care.ChaoCareServices.accessorySlot(gift);
      return !state.dead&&!state.sleeping&&!isCocooned()
          && !ItemStack.isSameItemSameComponents(getItemBySlot(slot),gift);
    }
    return gift.getItem() instanceof com.chaogarden.cwe.CweGiftItem cwe&&cwe.canUse(this,gift);
  }

  /** Return replaced gear before consuming the accepted gift. Failed insertion retries. */
  private boolean equipHeldWearable(ItemStack gift,Player giver) {
    var slot=com.chaogarden.care.ChaoCareServices.accessorySlot(gift);
    if(slot==null||committingWardrobe)return false;
    committingWardrobe=true;
    try {
      if (!returnWornStack(slot,giver)) return false;
      setItemSlot(EquipmentSlot.MAINHAND,ItemStack.EMPTY);
      setItemSlot(slot,gift.copyWithCount(1));setDropChance(slot,1f);offeredBy=null;
      playSound(SoundEvents.ARMOR_EQUIP_LEATHER.value(),.4f,1.3f);sync();return true;
    } finally {committingWardrobe=false;}
  }

  /** Detach the authoritative slot before inventory or world callbacks can re-enter care. */
  private boolean returnWornStack(EquipmentSlot slot, Player giver) {
    ItemStack old = getItemBySlot(slot);
    if(old.getItem() instanceof com.chaogarden.market.LegacyMelonHatItem)
      old=com.chaogarden.market.ChaoHats.canonicalCopy(old);
    if (old.isEmpty()) return true;
    setItemSlot(slot,ItemStack.EMPTY);
    boolean committed = false;
    try {
      ItemStack copy = old.copy();
      // A larger foreign stack could be partly inserted, then partly rejected. Return it intact.
      if (copy.getCount() == 1 && giver != null && giver.addItem(copy)) committed = true;
      else {
        var drop = new ItemEntity(level(),getX(),getY()+.45,getZ(),old.copy());
        drop.getPersistentData().putLong("ChaoRejectedUntil",level().getGameTime()+200);
        drop.setDefaultPickUpDelay();
        committed = ChaoItemDrops.publish(drop,this,false);
      }
      return committed;
    } finally {
      if (!committed) setItemSlot(slot,old);
    }
  }

  public boolean removeWearable(Player keeper, EquipmentSlot slot) {
    if (level().isClientSide || !canCare(keeper) || !com.chaogarden.care.ChaoCareServices.wardrobeSlot(slot)
        || getItemBySlot(slot).isEmpty()) return false;
    committingWardrobe = true;
    try {
      return returnWornStack(slot,keeper);
    } finally { committingWardrobe = false; }
  }

  /** Wall-clock phases are synced, while their effects are committed by this server tick only. */
  private void tickItemHandling() {
    if (!isHandlingItem() || committingWardrobe) return;
    if (isCocooned() || com.chaogarden.activities.ActivityManager.isBusy(this)) {
      returnHeldItem(false);return;
    }
    ItemAction action=getItemAction();
    if (action==ItemAction.EAT && com.chaogarden.core.ChaoAnimationRules.mealSoundAt(
        entityData.get(ITEM_DURATION)-getItemActionTicks()))
      playSound(SoundEvents.GENERIC_EAT,.18f,1.4f);
    if (action==ItemAction.REJECT && getItemActionTicks()>0 && getItemActionProgress(0)>=.55f
        && !getMainHandItem().isEmpty() && !throwHeldItem()) {
      itemAction(ItemAction.REJECT,20);
      return;
    }
    if (getItemActionTicks()>0) return;
    switch(action) {
      case THINK -> {
        ItemStack gift=getMainHandItem();
        boolean willing=acceptsHeldGift(gift);
        if(willing&&fashion(gift)) {
          if(!wardrobeChoiceMade) {
            wardrobeChoiceMade=true;
            wardrobeChoiceAccepted=com.chaogarden.core.WardrobeChoice.accepts(state,random.nextInt(100));
          }
          willing=wardrobeChoiceAccepted;
        }
        if(willing&&gift.getItem() instanceof com.chaogarden.fruit.FruitSeedItem) {
          itemAction(ItemAction.GARDEN,200);break;
        }
        if (willing) {
          boolean fruit=isBirthdayServing(gift)||gift.getItem() instanceof CareItem care && care.kind==CareItem.Kind.FRUIT;
          itemAction(fruit ? ItemAction.EAT : ItemAction.TRAIN,fruit ? 60 : com.chaogarden.core.ChaoPreferences.trainingTicks(state));
          if (fruit) entityData.set(EATING,60);
        } else {if(!fashion(gift))state.onFoodRejected();itemAction(ItemAction.REJECT,18);}
      }
      case GARDEN, PLANT -> itemAction(ItemAction.REJECT,18);
      case INSPECT -> itemAction(ItemAction.REJECT,18);
      case EAT, TRAIN -> {
        ItemStack held=getMainHandItem();
        if (!acceptsHeldGift(held)) {
          entityData.set(EATING,0);itemAction(ItemAction.REJECT,18);break;
        }
        Player wardrobeGiver=offeredBy==null?null:level().getPlayerByUUID(offeredBy);
        if(fashion(held)) {
          if(!wardrobeChoiceMade){wardrobeChoiceMade=true;wardrobeChoiceAccepted=com.chaogarden.core.WardrobeChoice.accepts(state,random.nextInt(100));}
          if(!wardrobeChoiceAccepted){itemAction(ItemAction.REJECT,18);break;}
        }
        if(wearable(held)) {
          if(!equipHeldWearable(held,wardrobeGiver)){itemAction(ItemAction.TRAIN,20);break;}
          itemAction(ItemAction.DELIGHT,24);break;
        }
        // Clearing first makes reload, death and inventory snapshots unable to
        // duplicate a completed benefit. A pending saved stack resumes instead.
        setItemSlot(EquipmentSlot.MAINHAND,ItemStack.EMPTY);
        Player giver=offeredBy==null ? null : level().getPlayerByUUID(offeredBy);
        offeredBy=null;
        if(com.chaogarden.fruit.GardenTools.isTool(held)) {
          com.chaogarden.fruit.GardenTools.learn(this,held);playSound(SoundEvents.AMETHYST_BLOCK_CHIME,.4f,1.4f);
        } else if(isBirthdayServing(held)) {
          state.feed(ChaoState.STAMINA,0,0);state.onFruitEaten();sync();playSound(SoundEvents.GENERIC_EAT,.4f,1.3f);
        } else if (held.getItem() instanceof CareItem care) applyCare(care,giver);
        else if(held.getItem() instanceof com.chaogarden.care.ChaoMedicineItem medicine) {
          medicine.treat(state);sync();playSound(SoundEvents.PLAYER_BURP,.35f,1.4f);
        } else if(held.getItem() instanceof com.chaogarden.cwe.CweGiftItem cwe) {
          cwe.apply(this,held,giver);sync();
        }
        itemAction(ItemAction.DELIGHT,24);
      }
      case REJECT -> { if(throwHeldItem())itemAction(ItemAction.NONE,0);else itemAction(ItemAction.REJECT,20); }
      case DELIGHT -> { entityData.set(EATING,0);itemAction(ItemAction.NONE,0); }
      default -> itemAction(ItemAction.NONE,0);
    }
    if (getItemAction()==ItemAction.NONE) {offeredBy=null;wardrobeChoiceMade=false;wardrobeChoiceAccepted=false;nextItemPickup=level().getGameTime()+120;}
    com.chaogarden.companion.CompanionContent.itemStateChanged(this);
  }

  public void beginPlanting() {if(getItemAction()==ItemAction.GARDEN)itemAction(ItemAction.PLANT,60);}
  public boolean finishPlanting(net.minecraft.core.BlockPos at) {
    if(!(level() instanceof ServerLevel server)||getItemAction()!=ItemAction.PLANT
        ||!(getMainHandItem().getItem() instanceof com.chaogarden.fruit.FruitSeedItem seed)
        ||!com.chaogarden.fruit.GardenTools.canPlant(this,seed.fruit)
        ||distanceToSqr(net.minecraft.world.phys.Vec3.atCenterOf(at))>9
        ||!com.chaogarden.fruit.GardenTools.plant(server,at,seed.fruit))return false;
    getMainHandItem().shrink(1);offeredBy=null;itemAction(ItemAction.DELIGHT,24);sync();return true;
  }
  public void cancelPlanting() {
    if(getItemAction()==ItemAction.GARDEN||getItemAction()==ItemAction.PLANT)itemAction(ItemAction.REJECT,18);
  }
  private boolean throwHeldItem() {
    if(!ejectHeldItem(true))return false;
    entityData.set(EATING,0);offeredBy=null;
    com.chaogarden.companion.CompanionContent.itemStateChanged(this);return true;
  }

  private boolean ejectHeldItem(boolean thrown) {
    if(level().isClientSide||returningHeldItem)return false;
    ItemStack held=getMainHandItem();if(held.isEmpty())return true;
    returningHeldItem=true;
    try {
      setItemSlot(EquipmentSlot.MAINHAND,ItemStack.EMPTY);
      var look=getLookAngle();
      var dropped=new ItemEntity(level(),getX()+(thrown?look.x*.35:0),getY()+.45,getZ()+(thrown?look.z*.35:0),held.copy());
      if(thrown){dropped.setDeltaMovement(look.x*.18,.16,look.z*.18);dropped.setPickUpDelay(20);dropped.setThrower(this);}
      else dropped.setDefaultPickUpDelay();
      dropped.getPersistentData().putLong("ChaoRejectedUntil",level().getGameTime()+200);
      if(!publishReservedItem(EquipmentSlot.MAINHAND,held,dropped,thrown))return false;
      nextItemPickup=level().getGameTime()+200;return true;
    } finally {returningHeldItem=false;}
  }

  private boolean publishReservedItem(EquipmentSlot slot, ItemStack original, ItemEntity drop, boolean thrown) {
    boolean published = false;
    try {
      published = ChaoItemDrops.publish(drop, this, thrown);
      return published;
    } finally {
      if (!published) setItemSlot(slot, original);
    }
  }

  private boolean returnHeldItem(boolean thrown) {
    if(!ejectHeldItem(thrown))return false;
    itemAction(ItemAction.NONE,0);entityData.set(EATING,0);offeredBy=null;
    com.chaogarden.companion.CompanionContent.itemStateChanged(this);return true;
  }

  @Override protected float getEquipmentDropChance(EquipmentSlot slot) {
    // Our guaranteed returns preserve durability. Vanilla's random equipment roll can damage gear.
    if (isDeadOrDying() && slot != EquipmentSlot.BODY) return 0;
    return super.getEquipmentDropChance(slot);
  }

  @Override protected void tickDeath() {
    if (!level().isClientSide && pendingDeathItems && deathTime >= 19) {
      // The dead body keeps the original stacks on disk if another mod vetoes a drop.
      // Do not let the normal removal tick delete its last copy.
      if (tickCount % 20 != 0 || !releaseLifecycleItems()) return;
      pendingDeathItems = false;
    }
    super.tickDeath();
  }

  @Override protected void dropEquipment() {
    pendingDeathItems = !releaseLifecycleItems();
    clearBehavior();
    super.dropEquipment();
  }

  @Override
  public boolean canMate(Animal other) {
    return other != this
        && other instanceof ChaoEntity c
        && !publishingBredEgg && !c.publishingBredEgg
        && mayBreed()
        && c.mayBreed()
        && breedingCooldown == 0
        && c.breedingCooldown == 0
        && isInLove()
        && c.isInLove();
  }
  public boolean mayBreed() { return state.canBreed(GardenConfig.EXPANDED_RARE_ACTIONS.get()); }

  @Override
  public AgeableMob getBreedOffspring(ServerLevel world, AgeableMob other) {
    // Vanilla spawn eggs call this with the same entity as both parents.
    if (other == this) return ChaoGarden.CHAO.get().create(world);
    if (!(other instanceof ChaoEntity c) || !mayBreed() || !c.mayBreed()) return null;
    var baby = ChaoGarden.CHAO.get().create(world);
    if (baby != null) {
      var child=ChaoState.breed(state,c.state,simulationRandom,GardenConfig.EXPANDED_RARE_ACTIONS.get(),GardenConfig.NEWBORN_EYE_CHANCE.get());
      com.chaogarden.ChaoVariants.normalizeNewCreature(child);
      child.recordParentNames(getName().getString(),c.getName().getString());baby.setState(child);
    }
    return baby;
  }

  @Override
  public void spawnChildFromBreeding(ServerLevel world, Animal mate) {
    if (!(mate instanceof ChaoEntity other) || !canMate(other)) return;
    // Entity-join and reward callbacks may reenter with either parent or another
    // partner. Reserve both parents until publication and its commit finish.
    publishingBredEgg = other.publishingBredEgg = true;
    try {
      var egg = ChaoGarden.CHAO_EGG.get().create(world);
      if (egg == null) return;
      var child=ChaoState.breed(state,other.state,simulationRandom,GardenConfig.EXPANDED_RARE_ACTIONS.get(),GardenConfig.NEWBORN_EYE_CHANCE.get());
      com.chaogarden.ChaoVariants.normalizeNewCreature(child);
      child.recordParentNames(getName().getString(),other.getName().getString());egg.setState(child);
      egg.owner = owner;
      egg.moveTo(getX(), getY(), getZ(), 0, 0);
      if (!world.addFreshEntity(egg)) return;
      // A rejected egg leaves love and cooldown intact so the pair can retry.
      resetLove();
      other.resetLove();
      breedingCooldown = other.breedingCooldown = 6000;
      particles(ParticleTypes.HEART, 12);
      world.addFreshEntity(new ExperienceOrb(world, getX(), getY(), getZ(), random.nextInt(7) + 1));
    } finally {
      publishingBredEgg = other.publishingBredEgg = false;
    }
  }

  @Override
  public boolean hurt(DamageSource source, float amount) {
    if(com.chaogarden.kindergarten.KindergartenContent.isBoarded(this))return false;
    if(committingWardrobe||resolvingLifecycle||returningLifecycleItems||returningHeldItem)return false;
    if (source.is(net.minecraft.tags.DamageTypeTags.IS_FALL) && com.chaogarden.companion.CompanionContent.isManaged(this)) return false;
    if (state != null
        && (state.immortal || isCocooned()||lifecyclePending())
        && !source.is(net.minecraft.tags.DamageTypeTags.BYPASSES_INVULNERABILITY)
        && !(source.getEntity() instanceof Player p && p.getAbilities().instabuild)) return false;
    boolean result=super.hurt(source, amount);
    if(result&&!isRemoved()&&!level().isClientSide&&!com.chaogarden.companion.CompanionContent.isManaged(this)) {
      if(state.sleeping)disturbSleep();
      state.onHurt();state.sleeping=false;clearBehavior();
      if(isHandlingItem())returnHeldItem(true);
      if(isAlive()&&!isCocooned())startBehavior(Behavior.COWER,60);
      sync();
    }
    if (result && isAlive() && !isRemoved() && !level().isClientSide
        && com.chaogarden.companion.CompanionContent.isManaged(this)
        && source.getEntity() instanceof Player keeper && keeper.getUUID().equals(owner)) {
      state.onHurt();
      sync();
      com.chaogarden.companion.CompanionContent.itemStateChanged(this);
    }
    return result;
  }

  @Override
  protected int calculateFallDamage(float distance, float multiplier) {
    // Slower descent alone does not change vanilla distance-based fall damage.
    int damage = super.calculateFallDamage(distance, multiplier);
    if (com.chaogarden.companion.CompanionContent.isManaged(this)) return 0;
    int reduction=state!=null && LocomotionRules.canFly(state) ? state.levels[ChaoState.FLY]/5 : 0;
    return Math.max(0, damage - reduction);
  }

  @Override
  protected SoundEvent getAmbientSound() {
    if (isResting() || isCocooned() || isBeingPetted() || isEating() || isBehaving()) return null;
    return ChaoSounds.AMBIENT.get();
  }

  @Override
  protected SoundEvent getHurtSound(DamageSource s) {
    return ChaoSounds.HURT.get();
  }

  @Override
  protected SoundEvent getDeathSound() {
    return ChaoSounds.DEATH.get();
  }

  @Override
  protected float getSoundVolume() {
    return .4f;
  }

  @Override
  public float getVoicePitch() {
    // The recordings already have the Chao timbre; keep Minecraft's baby pitch doubling off.
    return .98f + random.nextFloat() * .04f;
  }

  @Override
  public int getAmbientSoundInterval() {
    return 180;
  }

  @Override
  public void addAdditionalSaveData(CompoundTag t) {
    super.addAdditionalSaveData(t);
    t.putString("Chao", state.encode());
    if (pendingDeathItems) t.putBoolean("PendingDeathItems", true);
    if(!entityData.get(ORIGIN_EGG_COAT).isEmpty())t.putString("OriginEggCoat",entityData.get(ORIGIN_EGG_COAT));
    t.putBoolean("WardrobeChoiceMade",wardrobeChoiceMade);t.putBoolean("WardrobeChoiceAccepted",wardrobeChoiceAccepted);
    t.putBoolean("Stay", stay);
    t.putInt("BreedingCooldown", breedingCooldown);
    t.putBoolean("PendingRebirth",pendingRebirth);t.putBoolean("PendingNaturalDeath",pendingNaturalDeath);
    t.putLong("WakeGrace",Math.max(0,wakeGraceUntil-level().getGameTime()));
    t.putInt("ItemAction",getItemAction().ordinal());
    t.putInt("ItemActionDuration",entityData.get(ITEM_DURATION));
    t.putInt("ItemActionRemaining",getItemActionTicks());
    t.putLong("NextItemPickup",Math.max(0,nextItemPickup-level().getGameTime()));
    t.put("ChaoRelationships",exportRelationships());
    t.putLong("NextBehavior",Math.max(0,nextBehavior-level().getGameTime()));
    t.putInt("Behavior",storedBehavior().ordinal());t.putInt("BehaviorRemaining",getBehaviorTicks());
    t.putString("BehaviorDetail",behaviorDetail());
    if(getBehavior()==Behavior.INSTRUMENT&&com.chaogarden.toys.InstrumentPerformance.supports(behaviorDetail()))
      {t.putInt("InstrumentElapsed",(int)getBehaviorElapsed(0));t.putInt("InstrumentLastCue",lastInstrumentCueElapsed);}
    if (offeredBy!=null) t.putUUID("ItemOfferedBy",offeredBy);
    t.putBoolean("OfficialComparison",officialComparison());
    if (!sourceReferenceKey().isEmpty()) t.putString("SourceReferenceRig",sourceReferenceKey());
    if (toyPlayingTicks()>0) {t.putString("ToyPlayingKind",toyPlayingKind());t.putInt("ToyPlayingRemaining",toyPlayingTicks());}
    if (owner != null) t.putUUID("Keeper", owner);
    com.chaogarden.toys.ToyPreview.prepareSave(this,t);
  }

  @Override
  public void readAdditionalSaveData(CompoundTag t) {
    super.readAdditionalSaveData(t);
    com.chaogarden.market.ChaoHats.migrateEquipment(this);
    pendingDeathItems = t.getBoolean("PendingDeathItems");
    if (t.contains("Chao")) setState(ChaoState.decode(t.getString("Chao")));
    if(t.contains("OriginEggCoat"))setOriginEggCoat(ChaoState.decode(t.getString("OriginEggCoat")));
    stay = t.getBoolean("Stay");
    breedingCooldown = Math.max(0, t.getInt("BreedingCooldown"));
    pendingRebirth=t.getBoolean("PendingRebirth");pendingNaturalDeath=t.getBoolean("PendingNaturalDeath");
    wakeGraceUntil=level().getGameTime()+Math.max(0,Math.min(1200,t.getLong("WakeGrace")));
    int action=Math.max(0,Math.min(ItemAction.values().length-1,t.getInt("ItemAction")));
    ItemAction resumed=ItemAction.values()[action];
    int remaining=Math.max(0,Math.min(200,t.getInt("ItemActionRemaining")));
    if(resumed==ItemAction.GARDEN||resumed==ItemAction.PLANT){resumed=ItemAction.THINK;remaining=30;}
    if (getMainHandItem().isEmpty() && resumed!=ItemAction.DELIGHT && resumed!=ItemAction.REJECT) resumed=ItemAction.NONE;
    if (!getMainHandItem().isEmpty() && resumed==ItemAction.NONE) {resumed=ItemAction.INSPECT;remaining=60;}
    int duration=Math.max(1,Math.min(200,t.contains("ItemActionDuration") ? t.getInt("ItemActionDuration") : remaining));
    duration=Math.max(duration,remaining);
    itemAction(resumed,resumed==ItemAction.NONE ? 0 : duration);
    if (resumed!=ItemAction.NONE) entityData.set(ITEM_STARTED,level().getGameTime()-(duration-Math.max(1,remaining)));
    if (resumed==ItemAction.EAT) entityData.set(EATING,remaining);
    setDropChance(EquipmentSlot.MAINHAND,0);
    offeredBy=t.hasUUID("ItemOfferedBy") ? t.getUUID("ItemOfferedBy") : null;
    wardrobeChoiceMade=t.getBoolean("WardrobeChoiceMade");wardrobeChoiceAccepted=t.getBoolean("WardrobeChoiceAccepted");
    if(resumed==ItemAction.TRAIN&&fashion(getMainHandItem())&&!t.contains("WardrobeChoiceMade")) {wardrobeChoiceMade=true;wardrobeChoiceAccepted=true;}
    nextItemPickup=level().getGameTime()+Math.max(0,Math.min(1200,t.getLong("NextItemPickup")));
    restoreRelationships(t.contains("ChaoRelationships")?t.getCompound("ChaoRelationships"):t);
    nextBehavior=level().getGameTime()+Math.max(0,Math.min(24000,t.getLong("NextBehavior")));
    int behavior=Math.max(0,Math.min(Behavior.values().length-1,t.getInt("Behavior")));
    var resumedBehavior=Behavior.values()[behavior];
    // A saved partner may be absent or separately loaded. Cancel shared actions
    // without committing them; completed friendship counters are already saved.
    if(isSocialBehavior(resumedBehavior)||resumedBehavior==Behavior.LEARN||resumedBehavior==Behavior.PARTY||resumedBehavior==Behavior.WATER)resumedBehavior=Behavior.NONE;
    startBehavior(resumedBehavior,Math.max(0,Math.min(24000,t.getInt("BehaviorRemaining"))));
    if(resumedBehavior!=Behavior.NONE)setBehaviorDetail(t.getString("BehaviorDetail"));
    if(resumedBehavior==Behavior.INSTRUMENT&&com.chaogarden.toys.InstrumentPerformance.supports(behaviorDetail())) {
      int elapsed=Math.max(0,Math.min(24000-getBehaviorTicks(),t.getInt("InstrumentElapsed")));
      entityData.set(BEHAVIOR_DURATION,getBehaviorTicks()+elapsed);
      entityData.set(BEHAVIOR_STARTED,level().getGameTime()-elapsed);
      lastInstrumentCueElapsed=t.contains("InstrumentLastCue")?Math.max(-1,Math.min(elapsed,t.getInt("InstrumentLastCue"))):-1;
    }
    setOfficialComparison(t.getBoolean("OfficialComparison"));
    String reference=t.getString("SourceReferenceRig");
    setSourceReferenceKey(REFERENCE_RIGS.contains(reference) ? reference : "");
    setToyPlaying(t.getString("ToyPlayingKind"),Math.min(200,Math.max(0,t.getInt("ToyPlayingRemaining"))));
    if (t.hasUUID("Keeper")) owner = t.getUUID("Keeper");
  }
}
