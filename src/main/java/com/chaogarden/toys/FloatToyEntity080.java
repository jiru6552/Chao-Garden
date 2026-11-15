package com.chaogarden.toys;

import com.chaogarden.entity.ChaoEntity;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.Rotations;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

/** One persistent, physical float. The stored stack is the sole recoverable toy.
 * Both appearances share this controller; no invisible block/item copy follows it. */
public final class FloatToyEntity080 extends Entity {
  public static final int IDLE=0,PUSH=1,BOARD=2,RIDE=3,EXIT=4,LAUNCH_REPOSITION=5;
  private static final EntityDataAccessor<Byte> KIND=SynchedEntityData.defineId(FloatToyEntity080.class,EntityDataSerializers.BYTE);
  private static final EntityDataAccessor<Boolean> COMMITTED=SynchedEntityData.defineId(FloatToyEntity080.class,EntityDataSerializers.BOOLEAN);
  private static final EntityDataAccessor<Boolean> WATER=SynchedEntityData.defineId(FloatToyEntity080.class,EntityDataSerializers.BOOLEAN);
  private static final EntityDataAccessor<Optional<UUID>> CLAIM=SynchedEntityData.defineId(FloatToyEntity080.class,EntityDataSerializers.OPTIONAL_UUID);
  private static final EntityDataAccessor<Byte> PHASE=SynchedEntityData.defineId(FloatToyEntity080.class,EntityDataSerializers.BYTE);
  private static final EntityDataAccessor<Float> PROGRESS=SynchedEntityData.defineId(FloatToyEntity080.class,EntityDataSerializers.FLOAT);
  private static final EntityDataAccessor<Rotations> FROM=SynchedEntityData.defineId(FloatToyEntity080.class,EntityDataSerializers.ROTATIONS);
  private static final EntityDataAccessor<Rotations> TO=SynchedEntityData.defineId(FloatToyEntity080.class,EntityDataSerializers.ROTATIONS);
  private ItemStack payload=ItemStack.EMPTY;
  private long claimUntil,nextPlan,rideStarted,nextRemoval;
  private int restoredLease,blockedTicks,rewardTicks;
  private boolean wantsLand,pendingRemoval,restorePendingRemoval,transferring,previewHeld,returningExit,rewardEligible,rewardPaid;
  private FloatToyRoutes080.Plan plan;
  private int waypoint;
  private double lerpX,lerpY,lerpZ;private float lerpYaw,lerpPitch;private int lerpSteps;

  public FloatToyEntity080(EntityType<? extends FloatToyEntity080> type,net.minecraft.world.level.Level level){super(type,level);blocksBuilding=true;refreshDimensions();}
  @Override public net.minecraft.world.entity.EntityDimensions getDimensions(net.minecraft.world.entity.Pose pose){
    if(entityData==null)return super.getDimensions(pose);
    // The ring's low visible band must not form an invisible wall across the
    // rider's face. Duck head height, width, buoyancy and both seats stay intact.
    return net.minecraft.world.entity.EntityDimensions.scalable(.98f,entityData.get(KIND)==0?.20f:.68f);
  }
  @Override public void onSyncedDataUpdated(EntityDataAccessor<?> key){
    super.onSyncedDataUpdated(key);if(KIND.equals(key))refreshDimensions();
  }
  @Override protected void defineSynchedData(SynchedEntityData.Builder b){
    b.define(KIND,(byte)0);b.define(COMMITTED,false);b.define(WATER,false);b.define(CLAIM,Optional.empty());b.define(PHASE,(byte)IDLE);
    b.define(PROGRESS,0f);b.define(FROM,new Rotations(0,0,0));b.define(TO,new Rotations(0,0,0));
  }
  public String kind(){return entityData.get(KIND)==1?"chao_rubber_duck":"chao_lifesaver";}
  public boolean committed(){return entityData.get(COMMITTED);}
  public int phase(){return entityData.get(PHASE);}
  public float transition(){return entityData.get(PROGRESS);}
  public Optional<UUID> claimant(){return entityData.get(CLAIM);}
  public ItemStack storedItem(){return payload.copy();}
  public void configure(String kind,ItemStack item){
    if(committed())throw new IllegalStateException("Already committed float");
    if(!FloatToyContent080.kind(kind)||item.isEmpty())throw new IllegalArgumentException("Invalid float payload");
    entityData.set(KIND,(byte)(kind.equals("chao_rubber_duck")?1:0));payload=item.copyWithCount(1);refreshDimensions();
  }
  public void commit(){if(payload.isEmpty())throw new IllegalStateException("Empty float payload");entityData.set(COMMITTED,true);if(level() instanceof ServerLevel s)entityData.set(WATER,FloatToyRoutes080.surfaceAt(s,position())!=null);}
  /** Rollback only an unpublished/noncollectible candidate; never drops its borrowed payload. */
  public void discardUncommitted(){if(committed())throw new IllegalStateException("Committed float rollback");payload=ItemStack.EMPTY;transferring=true;try{super.remove(RemovalReason.DISCARDED);}finally{transferring=false;}}
  public boolean claimedBy(ChaoEntity c){return claimant().filter(c.getUUID()::equals).isPresent();}
  public ChaoEntity rider(){return getFirstPassenger() instanceof ChaoEntity c?c:null;}
  public boolean claim(ChaoEntity c){
    if(level().isClientSide||!committed()||transferring||payload.isEmpty()||isRemoved()||!c.isAlive()||c.level()!=level()||(pendingRemoval||restorePendingRemoval)&&rider()!=c)return false;
    if(rider()!=null&&rider()!=c)return false;
    if(claimant().isPresent()&&!claimedBy(c)&&claimUntil>level().getGameTime())return false;
    entityData.set(CLAIM,Optional.of(c.getUUID()));claimUntil=level().getGameTime()+80;return true;
  }
  public void release(ChaoEntity c){
    if(level().isClientSide||!claimedBy(c)||rider()==c)return;
    entityData.set(CLAIM,Optional.empty());claimUntil=0;plan=null;entityData.set(PHASE,(byte)IDLE);setDeltaMovement(Vec3.ZERO);
  }
  public boolean afloat(){return level() instanceof ServerLevel s&&FloatToyRoutes080.surfaceAt(s,position())!=null;}
  public void pushing(ChaoEntity c,Vec3 destination){
    if(!claim(c)||rider()!=null)return;
    var delta=destination.subtract(position());
    if(delta.horizontalDistance()<.003){setDeltaMovement(0,getDeltaMovement().y,0);return;}
    entityData.set(PHASE,(byte)PUSH);
    var step=new Vec3(delta.x,0,delta.z).normalize().scale(Math.min(.035,delta.horizontalDistance()));
    if(FloatToyRoutes080.swept((ServerLevel)level(),this,c,position(),position().add(step)))setDeltaMovement(step.x,getDeltaMovement().y,step.z);
    else setDeltaMovement(0,getDeltaMovement().y,0);
    face(delta);
  }
  /** A launch turn still belongs to its pusher, but must not keep moving the
   * prop while the actor walks around it or climb its low model as a step. */
  public void pauseLaunch(ChaoEntity c){
    if(!claim(c)||rider()!=null)return;
    entityData.set(PHASE,(byte)LAUNCH_REPOSITION);setDeltaMovement(0,getDeltaMovement().y,0);
  }
  public void stopPush(){if(phase()==PUSH||phase()==LAUNCH_REPOSITION){entityData.set(PHASE,(byte)IDLE);setDeltaMovement(0,getDeltaMovement().y,0);}}
  public boolean board(ChaoEntity c){
    if(!claim(c)||c.isPassenger()||rider()!=null||c.distanceToSqr(this)>1.21||!(level() instanceof ServerLevel s))return false;
    boolean rescue=c.isInWater()&&!com.chaogarden.core.LocomotionRules.canSwim(c.state());
    Vec3 destination=seat(c);if(!FloatToyRoutes080.exitClear(s,c,c.position(),destination,this))return false;
    entityData.set(FROM,relative(c.position()));entityData.set(PROGRESS,0f);entityData.set(PHASE,(byte)BOARD);
    setDeltaMovement(Vec3.ZERO);c.getNavigation().stop();c.setDeltaMovement(Vec3.ZERO);
    if(!c.startRiding(this)){entityData.set(PHASE,(byte)IDLE);release(c);return false;}
    wantsLand=rescue;rideStarted=level().getGameTime();
    rewardEligible=!wantsLand&&!previewHeld&&!ToyPreview.active(c);rewardPaid=false;rewardTicks=0;
    plan=null;blockedTicks=0;nextPlan=0;returningExit=false;return true;
  }
  public void previewHeld(boolean value){previewHeld=value;if(value){rewardEligible=false;plan=null;setDeltaMovement(Vec3.ZERO);}}
  public void requestLand(){rewardEligible=false;if(!wantsLand){wantsLand=true;nextPlan=0;}}
  /** Removal interrupts operator playback immediately, but its real passenger
   * stays aboard until the ordinary controller has a safe physical exit. */
  private void requestRemoval(){
    pendingRemoval=true;var c=rider();
    if(c!=null&&ToyPreview.active(c))ToyPreview.stop(c);
    previewHeld=false;requestLand();
  }
  public boolean safeToDetach(ChaoEntity c){
    if(!(level() instanceof ServerLevel s)||c.isRemoved()||isRemoved())return true;
    if(phase()==EXIT&&transition()>=1)return FloatToyRoutes080.safeFeet(s,c,absolute(entityData.get(TO)));
    return FloatToyRoutes080.safeFeet(s,c,c.position());
  }
  private Rotations relative(Vec3 p){var v=p.subtract(position());return new Rotations((float)v.x,(float)v.y,(float)v.z);}
  private Vec3 absolute(Rotations r){return position().add(r.getX(),r.getY(),r.getZ());}
  public Vec3 boardPoint(ChaoEntity c){return seat(c);}
  private Vec3 seat(ChaoEntity c){return position().add(Vec3.directionFromRotation(0,getYRot()).scale(kind().equals("chao_rubber_duck")?-.12:0)).add(0,kind().equals("chao_rubber_duck")?.28:entityData.get(WATER)?-.10:.02,0);}
  private Vec3 transitionPoint(ChaoEntity c,float progress){
    Vec3 start=absolute(entityData.get(FROM)),end=phase()==EXIT?absolute(entityData.get(TO)):seat(c);
    return FloatToyRoutes080.exitPoint(start,end,progress);
  }
  @Override protected void positionRider(Entity passenger,Entity.MoveFunction move){
    if(!(passenger instanceof ChaoEntity c)||!hasPassenger(c))return;
    var p=phase()==BOARD||phase()==EXIT?transitionPoint(c,transition()):seat(c);
    move.accept(c,p.x,p.y,p.z);c.setDeltaMovement(Vec3.ZERO);c.fallDistance=0;
    c.setYRot(getYRot());c.setYBodyRot(getYRot());c.setYHeadRot(getYRot());c.setXRot(Mth.approach(c.getXRot(),0,3));
  }
  @Override protected boolean couldAcceptPassenger(){
    // ClientPacketListener force-mounts the server's passenger packet before
    // tracked commitment/claim data may arrive. The payload exists only on the
    // server: using it as a client gate rejects every authoritative mount.
    if(level().isClientSide)return !isRemoved()&&getPassengers().isEmpty();
    return committed()&&!transferring&&!pendingRemoval&&(!restorePendingRemoval||!isAddedToLevel())
        &&!payload.isEmpty()&&getPassengers().isEmpty();
  }
  @Override protected boolean canAddPassenger(Entity c){return c instanceof ChaoEntity chao&&claimedBy(chao)&&getPassengers().isEmpty();}
  @Override public Vec3 getDismountLocationForPassenger(LivingEntity passenger){
    if(passenger instanceof ChaoEntity c&&level() instanceof ServerLevel s&&phase()==EXIT&&transition()>=1){
      var destination=absolute(entityData.get(TO));if(FloatToyRoutes080.safeFeet(s,c,destination))return destination;
    }
    return passenger.position();
  }
  @Override protected void removePassenger(Entity passenger){
    super.removePassenger(passenger);rewardEligible=false;
    if(level().isClientSide||isRemoved()||!(passenger instanceof ChaoEntity c)||c.isRemoved()||!c.isAlive()||c.level()!=level())return;
    // LivingEntity asks getDismountLocationForPassenger after this callback.
    // Retain EXIT/TO through that call; the next vehicle tick clears the phase.
    if(phase()==EXIT&&transition()>=1){entityData.set(CLAIM,Optional.empty());plan=null;return;}
    // Unloading/capture/removal are handled by vanilla. A live garden actor must
    // not be ejected into the water simply because its activity was interrupted.
    if(level() instanceof ServerLevel s&&!FloatToyRoutes080.safeFeet(s,c,c.position())){
      c.startRiding(this,true);requestLand();
    }else {entityData.set(CLAIM,Optional.empty());entityData.set(PHASE,(byte)IDLE);}
  }
  @Override public void tick(){
    super.tick();
    if(level().isClientSide){if(lerpSteps>0){double t=1.0/lerpSteps;setPos(Mth.lerp(t,getX(),lerpX),Mth.lerp(t,getY(),lerpY),Mth.lerp(t,getZ(),lerpZ));setYRot(getYRot()+(float)Mth.wrapDegrees(lerpYaw-getYRot())/lerpSteps);setXRot(Mth.lerp((float)t,getXRot(),lerpPitch));lerpSteps--;}return;}
    if(!committed()){if(tickCount>20)discardUncommitted();return;}
    // EntityType reconstructs the saved passenger tree through forced mounts
    // before publication. Only that unpublished load window may pass the outer
    // mount gate; newly claiming actors are denied throughout. Restore normal
    // pending-removal restrictions before the first production controller tick.
    if(restorePendingRemoval){restorePendingRemoval=false;pendingRemoval=true;requestLand();}
    boolean wasWater=entityData.get(WATER),waterNow=FloatToyRoutes080.surfaceAt((ServerLevel)level(),position())!=null;
    entityData.set(WATER,waterNow);
    if(restoredLease>0){claimUntil=level().getGameTime()+restoredLease;restoredLease=0;}
    var c=rider();
    if(c!=null){
      if(wasWater&&!waterNow)requestLand();
      entityData.set(CLAIM,Optional.of(c.getUUID()));claimUntil=level().getGameTime()+80;
      if(c.isCocooned()||c.hurtTime>0||c.isBeingPetted()||c.isHandlingItem()||c.isResting()
          ||c.state().sleeping||c.state().hunger<25||c.state().energy<25)requestLand();
      if(phase()==BOARD||phase()==EXIT)tickTransition(c);
      else {entityData.set(PHASE,(byte)RIDE);tickRiderRoute(c);}
    }else if(phase()==EXIT&&transition()>=1){entityData.set(PHASE,(byte)IDLE);entityData.set(PROGRESS,0f);
    }else if(claimant().isPresent()&&claimUntil<=level().getGameTime()){
      entityData.set(CLAIM,Optional.empty());entityData.set(PHASE,(byte)IDLE);setDeltaMovement(0,getDeltaMovement().y,0);
    }
    if(phase()==BOARD||phase()==EXIT||previewHeld)setDeltaMovement(0,getDeltaMovement().y,0);
    tickPhysics(c);
    if(pendingRemoval&&rider()==null&&level().getGameTime()>=nextRemoval)remove(RemovalReason.KILLED);
  }
  private void tickTransition(ChaoEntity c){
    setDeltaMovement(Vec3.ZERO);if(phase()==BOARD&&level().getGameTime()<nextPlan)return;
    float next=returningExit?Math.max(0,transition()-1/16f):Math.min(1,transition()+1/16f);
    Vec3 from=transitionPoint(c,transition()),to=transitionPoint(c,next);
    if(phase()==EXIT&&!returningExit&&next>=1){
      var endpoint=absolute(entityData.get(TO));
      if(plan==null||!FloatToyRoutes080.landingSafe((ServerLevel)level(),c,plan.landing(),this)
          ||!FloatToyRoutes080.safeFeet((ServerLevel)level(),c,endpoint)){
        returningExit=true;return;
      }
    }
    if(FloatToyRoutes080.sweptRider((ServerLevel)level(),this,c,from,to)){
      entityData.set(PROGRESS,next);blockedTicks=0;
      if(returningExit&&next<=0){
        returningExit=false;
        if(phase()==BOARD&&FloatToyRoutes080.safeFeet((ServerLevel)level(),c,to)){
          c.setPos(to.x,to.y,to.z);c.stopRiding();if(c.getVehicle()!=this){entityData.set(PHASE,(byte)IDLE);release(c);return;}
        }
        // If an entry's original support vanished too, keep the passenger safe
        // and retry the same short physical entry instead of ejecting or snapping.
        if(phase()==BOARD){nextPlan=level().getGameTime()+40;blockedTicks=0;}
        else {entityData.set(PHASE,(byte)RIDE);plan=null;nextPlan=level().getGameTime()+80;}
      }
      else if(next>=1){if(phase()==EXIT){c.setPos(to.x,to.y,to.z);c.stopRiding();setDeltaMovement(Vec3.ZERO);}else{entityData.set(PHASE,(byte)RIDE);rideStarted=level().getGameTime();}}
    }else if(++blockedTicks>=20){returningExit=true;requestLand();if(phase()==EXIT)nextPlan=level().getGameTime()+40;}
  }
  private void tickRiderRoute(ChaoEntity c){
    long now=level().getGameTime();
    if(rewardEligible&&!rewardPaid&&!previewHeld&&!ToyPreview.active(c)&&!wantsLand&&++rewardTicks>=160){
      // Preserve the ordinary completed-toy benefit once per genuine natural
      // ride, before marking it consumed. Preview and safe-wait ticks never pay.
      rewardPaid=true;rewardEligible=false;var state=c.state();state.happiness=Math.min(100,state.happiness+4);
      state.boredom=Math.max(0,state.boredom-35);state.addEmotion(com.chaogarden.core.ChaoEmotion.JOY,20);state.energy=Math.max(0,state.energy-2);c.sync();
    }
    if(previewHeld){setDeltaMovement(Vec3.ZERO);return;}
    if(!wantsLand&&now-rideStarted>=240+Math.floorMod(c.getUUID().hashCode(),241))wantsLand=true;
    if(!wantsLand)return;
    if((plan==null||waypoint>=plan.points().size())&&now>=nextPlan){
      if(!afloat()){
        var dry=FloatToyRoutes080.directLanding((ServerLevel)level(),this,c);
        plan=dry==null?null:new FloatToyRoutes080.Plan(List.of(position()),dry);
      }else plan=FloatToyRoutes080.waterRoute((ServerLevel)level(),this,c);
      waypoint=1;nextPlan=now+80+Math.floorMod(getUUID().hashCode(),31);
    }
    if(plan==null){setDeltaMovement(0,getDeltaMovement().y,0);return;}
    if(waypoint<plan.points().size()){
      Vec3 to=plan.points().get(waypoint),delta=to.subtract(position());
      if(delta.horizontalDistance()<.08){waypoint++;return;}
      var step=new Vec3(delta.x,0,delta.z).normalize().scale(Math.min(.04,delta.horizontalDistance()));
      if(FloatToyRoutes080.swept((ServerLevel)level(),this,c,position(),position().add(step))){setDeltaMovement(step.x,getDeltaMovement().y,step.z);face(delta);blockedTicks=0;}
      else {setDeltaMovement(0,getDeltaMovement().y,0);if(++blockedTicks>20){plan=null;nextPlan=now+40;}}return;
    }
    var landing=plan.landing();
    if(landing!=null&&distanceToSqr(landing.waterDock())<.16&&FloatToyRoutes080.landingSafe((ServerLevel)level(),c,landing,this)&&FloatToyRoutes080.exitClear((ServerLevel)level(),c,c.position(),landing.feet(),this)){
      entityData.set(FROM,relative(c.position()));entityData.set(TO,relative(landing.feet()));entityData.set(PROGRESS,0f);entityData.set(PHASE,(byte)EXIT);returningExit=false;setDeltaMovement(Vec3.ZERO);
    }else {plan=null;nextPlan=now+40;setDeltaMovement(Vec3.ZERO);}
  }
  private void face(Vec3 delta){if(delta.horizontalDistanceSqr()>.000001)setYRot((float)(Math.atan2(-delta.x,delta.z)*180/Math.PI));}
  private void tickPhysics(ChaoEntity c){
    // Entry/exit endpoints are tied to this real vehicle position. Holding it
    // still for the short transition prevents a settling waterline from moving
    // a previously validated dry landing after it was checked.
    if(phase()==BOARD||phase()==EXIT){setDeltaMovement(Vec3.ZERO);return;}
    var level=(ServerLevel)level();Double surface=FloatToyRoutes080.surfaceAt(level,position());var v=getDeltaMovement();
    double dy;
    if(surface!=null)dy=Mth.clamp((surface-.05-getY())*.3,-.08,.08);
    else if(level.getFluidState(blockPosition()).is(net.minecraft.tags.FluidTags.WATER))dy=.06;
    else dy=Math.max(-.24,v.y-.04);
    var step=new Vec3(v.x,dy,v.z);
    if(c!=null&&!FloatToyRoutes080.swept(level,this,c,position(),position().add(step)))step=Vec3.ZERO;
    move(MoverType.SELF,step);setDeltaMovement(step.x*.65,verticalCollision?0:step.y*.8,step.z*.65);
  }
  @Override public void lerpTo(double x,double y,double z,float yaw,float pitch,int steps){lerpX=x;lerpY=y;lerpZ=z;lerpYaw=yaw;lerpPitch=pitch;lerpSteps=Math.max(1,steps);}
  @Override protected net.minecraft.network.chat.Component getTypeName(){return net.minecraft.network.chat.Component.translatable("item.chao_garden."+kind());}
  @Override public ItemStack getPickResult(){return committed()?payload.isEmpty()?new ItemStack(com.chaogarden.ChaoGarden.item(kind())):payload.copy():ItemStack.EMPTY;}
  @Override public boolean isPickable(){return committed()&&!isRemoved();}
  @Override public boolean isPushable(){return false;}
  @Override public boolean canBeCollidedWith(){return committed()&&!isRemoved();}
  @Override public boolean hurt(DamageSource source,float amount){
    if(level().isClientSide||!committed()||transferring)return false;
    if(source.getEntity() instanceof Player player&&player.getAbilities().instabuild&&rider()==null){payload=ItemStack.EMPTY;transferring=true;try{super.remove(RemovalReason.KILLED);}finally{transferring=false;}return true;}
    requestRemoval();if(rider()==null)remove(RemovalReason.KILLED);return true;
  }
  @Override public InteractionResult interact(Player player,InteractionHand hand){
    if(!committed()||!player.isSecondaryUseActive()||!player.getItemInHand(hand).isEmpty())return InteractionResult.PASS;
    if(level().isClientSide)return InteractionResult.SUCCESS;
    if(rider()!=null){requestRemoval();return InteractionResult.CONSUME;}
    if(transferring)return InteractionResult.FAIL;transferring=true;
    try{var returned=payload.copy();
      // Vanilla discards overflow for Creative players while reporting success.
      // Picking up a real world toy must still transfer to an actual slot.
      if(player.getInventory().getFreeSlot()<0&&player.getInventory().getSlotWithRemainingSpace(returned)<0)return InteractionResult.FAIL;
      if(!player.getInventory().add(returned))return InteractionResult.FAIL;payload=ItemStack.EMPTY;super.remove(RemovalReason.DISCARDED);return InteractionResult.CONSUME;}finally{transferring=false;}
  }
  @Override public void remove(RemovalReason reason){
    if(!level().isClientSide&&transferring)return;
    if(!level().isClientSide&&reason.shouldDestroy()&&committed()&&!transferring&&!payload.isEmpty()){
      if(rider()!=null){requestRemoval();return;}
      transferring=true;var reserved=payload;payload=ItemStack.EMPTY;
      try{
        var item=new ItemEntity(level(),getX(),getY()+.1,getZ(),reserved.copy());item.setDefaultPickUpDelay();
        if(!level().addFreshEntity(item)||item.isRemoved()){payload=reserved;pendingRemoval=true;nextRemoval=level().getGameTime()+40;return;}
      }finally{transferring=false;}
    }
    super.remove(reason);
  }
  @Override protected void addAdditionalSaveData(CompoundTag tag){
    tag.putBoolean("Committed080",committed());tag.putBoolean("FloatWater080",entityData.get(WATER));tag.putString("FloatKind080",kind());
    if(!payload.isEmpty())tag.put("FloatItem080",payload.save(registryAccess()));
    claimant().ifPresent(uuid->tag.putUUID("Claim080",uuid));tag.putInt("ClaimLease080",(int)Math.max(0,Math.min(80,claimUntil-level().getGameTime())));
    tag.putByte("FloatPhase080",(byte)phase());tag.putFloat("Transition080",transition());
    var a=entityData.get(FROM);var b=entityData.get(TO);tag.putFloat("FromX080",a.getX());tag.putFloat("FromY080",a.getY());tag.putFloat("FromZ080",a.getZ());tag.putFloat("ToX080",b.getX());tag.putFloat("ToY080",b.getY());tag.putFloat("ToZ080",b.getZ());
    tag.putBoolean("RewardEligible080",rewardEligible);tag.putBoolean("RewardPaid080",rewardPaid);tag.putInt("RewardTicks080",rewardTicks);
    tag.putBoolean("LandWanted080",wantsLand);tag.putBoolean("RemovalWanted080",pendingRemoval||restorePendingRemoval);tag.putBoolean("ReturningExit080",returningExit);tag.putLong("RideAge080",Math.max(0,level().getGameTime()-rideStarted));
  }
  @Override protected void readAdditionalSaveData(CompoundTag tag){
    String kind=tag.getString("FloatKind080");entityData.set(KIND,(byte)(kind.equals("chao_rubber_duck")?1:0));
    entityData.set(WATER,tag.contains("FloatWater080")?tag.getBoolean("FloatWater080"):level() instanceof ServerLevel s&&FloatToyRoutes080.surfaceAt(s,position())!=null);
    payload=ItemStack.parse(registryAccess(),tag.getCompound("FloatItem080")).orElse(ItemStack.EMPTY);if(!payload.isEmpty())payload.setCount(1);
    entityData.set(COMMITTED,tag.getBoolean("Committed080")&&!payload.isEmpty());entityData.set(CLAIM,tag.hasUUID("Claim080")?Optional.of(tag.getUUID("Claim080")):Optional.empty());restoredLease=Math.max(0,Math.min(80,tag.getInt("ClaimLease080")));
    entityData.set(PHASE,(byte)Mth.clamp(tag.getByte("FloatPhase080"),0,LAUNCH_REPOSITION));entityData.set(PROGRESS,Mth.clamp(tag.getFloat("Transition080"),0,1));
    entityData.set(FROM,new Rotations(tag.getFloat("FromX080"),tag.getFloat("FromY080"),tag.getFloat("FromZ080")));entityData.set(TO,new Rotations(tag.getFloat("ToX080"),tag.getFloat("ToY080"),tag.getFloat("ToZ080")));
    rewardEligible=tag.getBoolean("RewardEligible080");rewardPaid=tag.getBoolean("RewardPaid080");rewardTicks=Mth.clamp(tag.getInt("RewardTicks080"),0,160);
    wantsLand=tag.getBoolean("LandWanted080");pendingRemoval=false;restorePendingRemoval=tag.getBoolean("RemovalWanted080");returningExit=tag.getBoolean("ReturningExit080");rideStarted=level().getGameTime()-Math.max(0,Math.min(2400,tag.getLong("RideAge080")));plan=null;refreshDimensions();
  }
}
