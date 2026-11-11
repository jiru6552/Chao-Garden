package com.chaogarden.kindergarten;

import com.chaogarden.entity.ChaoEntity;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

/** A protected school pupil, separate from a keeper's actual boarded student. */
public final class KindergartenPupil0257 extends ChaoEntity {
  private int slot;
  long nextAction;
  net.minecraft.world.phys.Vec3 destination;
  public KindergartenPupil0257(EntityType<? extends KindergartenPupil0257> type,Level level){
    super(type,level);setPersistenceRequired();setCanPickUpLoot(false);
  }
  public int schoolSlot(){return slot;}
  public void schoolSlot(int index){slot=Math.max(0,Math.min(KindergartenPupils0257.COUNT-1,index));}
  @Override protected void registerGoals(){
    goalSelector.addGoal(0,new Goal(){
      {setFlags(java.util.EnumSet.of(Flag.MOVE,Flag.LOOK,Flag.JUMP));}
      @Override public boolean canUse(){return true;}
      @Override public boolean canContinueToUse(){return true;}
      @Override public boolean requiresUpdateEveryTick(){return true;}
      @Override public void tick(){KindergartenPupils0257.tick(KindergartenPupil0257.this);}
    });
  }
  @Override public boolean canCare(Player player){return false;}
  @Override public boolean canBeginSocial(){return false;}
  @Override public boolean mayBreed(){return false;}
  @Override public boolean hurt(DamageSource source,float amount){return false;}
  @Override public boolean canBeLeashed(){return false;}
  @Override public boolean isPushable(){return false;}
  @Override public void push(double x,double y,double z){}
  @Override public InteractionResult mobInteract(Player player,InteractionHand hand){return InteractionResult.sidedSuccess(level().isClientSide);}
  @Override public void addAdditionalSaveData(CompoundTag tag){super.addAdditionalSaveData(tag);tag.putInt("SchoolPupilSlot",slot);}
  @Override public void readAdditionalSaveData(CompoundTag tag){
    super.readAdditionalSaveData(tag);schoolSlot(tag.getInt("SchoolPupilSlot"));owner=null;
    setNoAi(false);setNoGravity(false);setInvulnerable(true);setCanPickUpLoot(false);
    destination=null;nextAction=0;
  }
}
