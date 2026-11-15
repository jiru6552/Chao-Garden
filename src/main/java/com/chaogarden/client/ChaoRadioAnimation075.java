package com.chaogarden.client;

import com.chaogarden.entity.ChaoEntity;
import com.chaogarden.toys.RadioPlayRules075;

/** A restrained authored listening gesture on the existing grounded sitting rig.
 * The timing is shared with production radio playback, not a separate demo. */
final class ChaoRadioAnimation075 {
  private ChaoRadioAnimation075() {}

  static boolean active(ChaoEntity c) {
    return RadioPlayRules075.KIND.equals(c.toyPlayingKind())&&c.toyPlayingTicks()>0
        &&c.isAlive()&&c.hurtTime<=0&&!c.isCocooned()&&!c.isCompanionGripping()
        &&!com.chaogarden.companion.CompanionContent.isManaged(c)
        &&!c.officialComparison()&&!c.isResting()&&!c.isEating()&&!c.isBeingPetted()
        &&!c.isHandlingItem()&&!c.isPassenger()&&c.getHeldChaoItem().isEmpty()
        &&c.getMainHandItem().isEmpty()&&c.getBehavior()==ChaoEntity.Behavior.NONE;
  }

  // The cabinet's speaker is much lower than a seated Chao's face. Keep a
  // readable downward attention angle rather than burying its blocky face in
  // its torso while LookControl aims at that exact point. Ordinary gaze is
  // returned unchanged once the shared activity transition ends.
  static float gazeYaw(boolean active,float yaw){return active?Math.clamp(yaw,-10,10):yaw;}
  static float gazePitch(boolean active,float pitch){return active?Math.clamp(pitch,-8,8):pitch;}

  static void pose(OfficialChaoRig rig,float elapsed) {
    float weight=RadioPlayRules075.presence(elapsed);
    if(weight<=0)return;
    var skull=rig.pose("skull");
    if(skull!=null){
      skull.rx+=RadioPlayRules075.headPitch(elapsed);
      skull.ry+=RadioPlayRules075.headYaw(elapsed);
      skull.rz+=RadioPlayRules075.headRoll(elapsed);
    }
    // Low, relaxed hands respond to the same beat as the head. Only rotate
    // their existing joints; shoulder placement, reach and limb length stay put.
    float pulse=RadioPlayRules075.armPulse(elapsed);
    for(String name:java.util.List.of("arm_left","arm_right")){
      var arm=rig.pose(name);if(arm==null)continue;
      float side=name.endsWith("left")?1:-1;
      arm.rx-=.06f*weight+side*pulse;
      arm.rz+=side*.035f*weight;
    }
  }
}
