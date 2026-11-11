package com.chaogarden.kindergarten;

import com.chaogarden.entity.ChaoEntity;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

/** A protected adult resident with a specialized profession model and real services. */
public final class KindergartenNpc extends ChaoEntity {
  private static final EntityDataAccessor<Integer> GESTURE=SynchedEntityData.defineId(KindergartenNpc.class,EntityDataSerializers.INT);
  private static final EntityDataAccessor<Long> GESTURE_STARTED=SynchedEntityData.defineId(KindergartenNpc.class,EntityDataSerializers.LONG);
  private static final EntityDataAccessor<Integer> ROLE=SynchedEntityData.defineId(KindergartenNpc.class,EntityDataSerializers.INT);
  public KindergartenNpc(EntityType<? extends KindergartenNpc> type,Level level){super(type,level);setNoAi(true);setPersistenceRequired();}
  @Override protected void defineSynchedData(SynchedEntityData.Builder builder){super.defineSynchedData(builder);builder.define(ROLE,0);builder.define(GESTURE,0);builder.define(GESTURE_STARTED,-1000L);}
  public KindergartenContent.Role role(){return KindergartenContent.Role.values()[Math.max(0,Math.min(4,entityData.get(ROLE)))];}
  public void role(KindergartenContent.Role role){entityData.set(ROLE,role.ordinal());}
  public int serviceGesture(){return serviceGestureAge(0)<48?entityData.get(GESTURE):0;}
  public float serviceGestureAge(float partial){return Math.max(0,level().getGameTime()-entityData.get(GESTURE_STARTED)+partial);}
  public void triggerServiceGesture(int code){if(!level().isClientSide){entityData.set(GESTURE,Math.max(0,Math.min(4,code)));entityData.set(GESTURE_STARTED,level().getGameTime());}}
  @Override public boolean canCare(Player player){return false;}
  @Override public boolean hurt(DamageSource source,float amount){return false;}
  @Override public boolean isPushable(){return false;}
  @Override public boolean canBeLeashed(){return false;}
  @Override public void push(double x,double y,double z){}
  @Override public void aiStep(){
    getNavigation().stop();setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO);
    // Resident body control is intentionally stopped; establish its canonical facing on both sides.
    float facing=role()==KindergartenContent.Role.FORTUNE?90:0;setYRot(facing);yRotO=facing;yBodyRot=yBodyRotO=facing;setYHeadRot(facing);yHeadRotO=facing;
  }
  @Override public InteractionResult mobInteract(Player player,InteractionHand hand){
    if(hand==InteractionHand.MAIN_HAND&&!level().isClientSide&&player instanceof ServerPlayer keeper)KindergartenContent.open(keeper,this,"");
    return InteractionResult.sidedSuccess(level().isClientSide);
  }
  @Override public void addAdditionalSaveData(CompoundTag tag){super.addAdditionalSaveData(tag);tag.putInt("KindergartenRole",role().ordinal());}
  @Override public void readAdditionalSaveData(CompoundTag tag){super.readAdditionalSaveData(tag);role(KindergartenContent.Role.values()[Math.max(0,Math.min(4,tag.getInt("KindergartenRole")))]);setNoAi(true);}
}
