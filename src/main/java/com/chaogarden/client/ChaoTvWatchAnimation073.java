package com.chaogarden.client;

import com.chaogarden.entity.ChaoEntity;
import com.chaogarden.toys.TvWatchRules073;

/** Relaxed viewing layered on the existing sitting pose and session clock. */
final class ChaoTvWatchAnimation073 {
  private ChaoTvWatchAnimation073() {}
  static boolean active(ChaoEntity c) {
    return TvWatchRules073.KIND.equals(c.toyPlayingKind())&&c.isAlive()&&c.hurtTime<=0
        &&!c.isCocooned()&&!c.isCompanionGripping()&&!c.officialComparison()&&!c.isResting()
        &&!c.isEating()&&!c.isBeingPetted()&&!c.isHandlingItem()&&!c.isPassenger()
        &&c.getHeldChaoItem().isEmpty()&&c.getMainHandItem().isEmpty()
        &&c.getBehavior()==ChaoEntity.Behavior.NONE;
  }
  static void pose(OfficialChaoRig rig,float elapsed) {
    float weight=TvWatchRules073.presence(elapsed);
    var skull=rig.pose("skull");
    if(skull!=null){skull.rx+=TvWatchRules073.headPitch(elapsed);skull.ry+=TvWatchRules073.headYaw(elapsed);skull.rz+=TvWatchRules073.headRoll(elapsed);}
    var body=rig.pose("body");if(body!=null)body.rx+=.025f*weight;
    for(String name:java.util.List.of("arm_left","arm_right")){
      var arm=rig.pose(name);if(arm==null)continue;
      arm.rx+=.025f*weight;arm.rz+=(name.endsWith("left")?.03f:-.03f)*weight;
    }
  }
}
