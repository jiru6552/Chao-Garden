package com.chaogarden.toys;

import com.chaogarden.entity.ChaoEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;

/** One production contact/chase controller, used by ordinary play and operator playback. */
public final class BallController074 {
  private final ChaoEntity chao;
  private BlockPos toy,stand;
  private long nextAction;
  private boolean contactApplied;
  private Vec3 actionAnchor;
  private String lastFailure="";
  public BallController074(ChaoEntity chao,BlockPos toy,BlockPos stand){this.chao=chao;this.toy=toy.immutable();this.stand=stand.immutable();}
  public BlockPos target(){return toy;}
  public BlockPos stand(){return stand;}
  public String lastFailure(){return lastFailure;}
  private boolean fail(String reason){lastFailure=reason;return false;}
  public boolean tick(){
    if(!(chao.level() instanceof ServerLevel level)||!level.hasChunkAt(toy)||!level.getBlockState(toy).is(ToyContent.block(BallPlayRules074.KIND))
        ||!ToyContent.available(level,toy,chao)||!level.getFluidState(toy).isEmpty()
        ||chao.position().subtract(Vec3.atBottomCenterOf(toy)).lengthSqr()>16)return fail("source/reservation/fluid/range");
    ToyContent.retainBallClearance(chao,toy);
    chao.getLookControl().setLookAt(toy.getX()+.5,toy.getY()+BallPlayRules074.RADIUS,toy.getZ()+.5,180,30);
    int action=chao.toyKickElapsed();
    if(action>=0){
      chao.getNavigation().stop();chao.getMoveControl().setWantedPosition(chao.getX(),chao.getY(),chao.getZ(),0);chao.setDeltaMovement(new Vec3(0,chao.getDeltaMovement().y,0));
      if(actionAnchor==null||chao.distanceToSqr(actionAnchor)>.01)return fail("action displaced from "+actionAnchor);
      ToyContent.face(chao,Direction.getNearest(toy.getX()+.5-chao.getX(),0,toy.getZ()+.5-chao.getZ()));
      if(!contactApplied&&action>=BallPlayRules074.CONTACT_TICK){
        contactApplied=true;
        var anchor=ToyContent.ballAnchor(chao,toy,stand);
        if(chao.distanceToSqr(anchor)>.0016||Math.abs(chao.getY()-toy.getY())>.05)return fail("contact anchor changed "+anchor);
        var delta=Vec3.atBottomCenterOf(toy).subtract(chao.position());var direction=Direction.getNearest(delta.x,0,delta.z);
        var next=ToyContent.rollBall074(chao,toy,direction);
        if(next!=null){toy=next;var side=ToyContent.standAt(level,toy,chao);if(side!=null)stand=side;}
        else nextAction=Math.max(nextAction,level.getGameTime()+60); // A blocked, quiet nudge is never a remote kick.
      }
      return true;
    }
    var anchor=ToyContent.ballAnchor(chao,toy,stand);
    if(!level.noCollision(chao,chao.getBoundingBox().move(anchor.subtract(chao.position()))))return fail("contact destination collision "+anchor);
    if(chao.distanceToSqr(anchor)>.0009){approach(anchor);return true;}
    chao.getNavigation().stop();chao.setDeltaMovement(new Vec3(0,chao.getDeltaMovement().y,0));
    // The final contact handoff is at most0.03blocks and has a clear swept actor volume.
    if(!level.noCollision(chao,chao.getBoundingBox().expandTowards(anchor.subtract(chao.position()))))return fail("contact handoff collision "+anchor);
    chao.setPos(anchor.x,anchor.y,anchor.z);
    // A zero-speed MOVE_TO still turns toward its wanted point. Reset that
    // point after the tiny handoff so vanilla cannot turn back to the old root.
    chao.getMoveControl().setWantedPosition(anchor.x,anchor.y,anchor.z,0);
    ToyContent.face(chao,Direction.getNearest(toy.getX()+.5-anchor.x,0,toy.getZ()+.5-anchor.z));
    if(level.getGameTime()>=nextAction&&(!(level.getBlockEntity(toy) instanceof BallToyBlockEntity074 ball)||!ball.rolling(0))){
      nextAction=level.getGameTime()+BallPlayRules074.ACTION_INTERVAL;contactApplied=false;actionAnchor=anchor;chao.startToyKick();
    }
    return true;
  }
  private void approach(Vec3 anchor){
    if(chao.distanceToSqr(Vec3.atBottomCenterOf(stand))<.7){
      chao.getNavigation().stop();chao.getMoveControl().setWantedPosition(anchor.x,anchor.y,anchor.z,.9);
    }else if(chao.getNavigation().isDone()){
      var path=chao.getNavigation().createPath(stand,0);if(path!=null&&path.canReach())chao.getNavigation().moveTo(path,.9);
    }
  }
}
