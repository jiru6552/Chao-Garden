package com.chaogarden.entity;

import com.chaogarden.toys.BallPlayRules074;
import com.chaogarden.toys.ToyContent;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.MoveControl;
import net.minecraft.world.phys.Vec3;

/** Vanilla movement everywhere except the claimed ball's low, empty actor-contact space. */
public final class BallContactMoveControl074 extends MoveControl {
  private final ChaoEntity chao;
  public BallContactMoveControl074(ChaoEntity chao){super(chao);this.chao=chao;}
  @Override public void tick(){
    var at=chao.blockPosition();
    if(operation==Operation.MOVE_TO&&chao.onGround()&&!chao.isInWater()&&!chao.isBehaving()
        &&Math.abs(wantedY-chao.getY())<.05&&chao.level().getBlockState(at).is(ToyContent.block(BallPlayRules074.KIND))&&ToyContent.ownsBallContact(chao,at)){
      // MoveControl normally asks to jump whenever a mob's feet enter any
      // nonempty block cell. The active owner's collision is intentionally
      // clear here: use the same yaw/speed/WAIT branch without that jump.
      operation=Operation.WAIT;double dx=wantedX-chao.getX(),dy=wantedY-chao.getY(),dz=wantedZ-chao.getZ();
      var step=new Vec3(dx,0,dz);double distance=step.length();
      if(dx*dx+dy*dy+dz*dz<2.5000003E-7F||!chao.level().noCollision(chao,chao.getBoundingBox().expandTowards(distance<.00001?Vec3.ZERO:step.scale(Math.min(.08,distance)/distance)))){chao.setSpeed(0);return;}
      float yaw=(float)(Mth.atan2(dz,dx)*180/Math.PI)-90;
      chao.setYRot(rotlerp(chao.getYRot(),yaw,90));chao.setSpeed((float)(speedModifier*chao.getAttributeValue(Attributes.MOVEMENT_SPEED)));return;
    }
    super.tick();
  }
}
