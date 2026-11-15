package com.chaogarden.client;

import com.chaogarden.core.ChaoIdleRules078;
import com.chaogarden.entity.ChaoEntity;
import com.chaogarden.toys.ToyContent;
import com.chaogarden.toys.ToyPreview;
import com.mojang.blaze3d.vertex.PoseStack;
import org.joml.Vector3f;

/** Natural episodes and operator previews share this clock and exact pose path. */
final class ChaoIdleAnimation078 {
  private static final LionfishIdleClip078 CLIP=new LionfishIdleClip078();
  private ChaoIdleAnimation078() {}

  static String id(ChaoEntity chao) {
    if(!available(chao))return "";
    String preview=ToyPreview.idleId(chao.toyPlayingKind());
    if(!preview.isEmpty())return chao.getBehavior()==ChaoEntity.Behavior.NONE?preview:"";
    return chao.getBehavior()==ChaoEntity.Behavior.IDLE&&ChaoIdleRules078.valid(chao.behaviorDetail())?chao.behaviorDetail():"";
  }
  private static boolean available(ChaoEntity chao) {
    return chao.isAlive()&&chao.hurtTime<=0&&!chao.isCocooned()&&!chao.isCompanionGripping()
        &&!com.chaogarden.companion.CompanionContent.isManaged(chao)
        &&!chao.officialComparison()&&!chao.isResting()&&!chao.isEating()&&!chao.isBeingPetted()
        &&!chao.isHandlingItem()&&!chao.isPassenger()&&!chao.isInWater()&&chao.onGround()
        &&chao.getHeldChaoItem().isEmpty()&&chao.getMainHandItem().isEmpty();
  }
  static boolean active(ChaoEntity chao){return !id(chao).isEmpty();}
  static float elapsed(ChaoEntity chao,float partial) {
    return !ToyPreview.idleId(chao.toyPlayingKind()).isEmpty()
        ?Math.max(0,ToyContent.PLAY_TICKS-chao.toyPlayingTicks()+partial):chao.getBehaviorElapsed(partial);
  }
  static boolean seated(ChaoEntity chao){return seated(chao,0);}
  static boolean seated(ChaoEntity chao,float partial) {
    String id=id(chao);return ChaoIdleRules078.seated(id)&&ChaoIdleRules078.weight(id,elapsed(chao,partial))>0;
  }
  static void pose(OfficialChaoRig rig,ChaoEntity chao,float partial){pose(rig,id(chao),elapsed(chao,partial));}
  static void pose(OfficialChaoRig rig,String id,float elapsed) {
    float weight=ChaoIdleRules078.weight(id,elapsed);if(weight<=0)return;
    var sampled=CLIP.sample(id,elapsed);
    turn(rig,"skull",sampled.head(),weight);turn(rig,"arm_left",sampled.left(),weight);turn(rig,"arm_right",sampled.right(),weight);
    if("ground_play".equals(id))groundHands(rig);
  }
  /** The broadest mature palms need a slightly shallower seated shoulder
   * angle. Rotate that existing joint only until its actual visible box clears
   * the floor; no reach, translation, scale or per-creature history is added.
   * The production posture transition blends this constrained target too. */
  private static void groundHands(OfficialChaoRig rig) {
    for(boolean left:new boolean[]{false,true}){
      var arm=rig.pose(left?"arm_left":"arm_right");if(arm==null||lowestHand(rig,left)<=23.97f)continue;
      float original=arm.rz;arm.rz=0;
      if(lowestHand(rig,left)>23.97f){arm.rz=original;continue;}
      float lower=0,upper=1;
      for(int step=0;step<16;step++){
        float blend=(lower+upper)*.5f;arm.rz=original*(1-blend);
        if(lowestHand(rig,left)>23.97f)lower=blend;else upper=blend;
      }
      arm.rz=original*(1-upper);
    }
  }
  private static float lowestHand(OfficialChaoRig rig,boolean left) {
    var bounds=rig.baseToyHandBounds(left);if(bounds==null)return Float.NEGATIVE_INFINITY;
    var stack=new PoseStack();rig.transformBone(left?"arm_left":"arm_right",stack);
    float bottom=Float.NEGATIVE_INFINITY;
    for(int i=0;i<8;i++){
      var point=new Vector3f(bounds[(i&1)==0?0:3],bounds[(i&2)==0?1:4],bounds[(i&4)==0?2:5]).div(16);
      bottom=Math.max(bottom,stack.last().pose().transformPosition(point).y*16);
    }
    return bottom;
  }
  private static void turn(OfficialChaoRig rig,String bone,Vector3f rotation,float weight) {
    var joint=rig.pose(bone);if(joint==null)return;
    joint.rx+=rotation.x*weight;joint.ry+=rotation.y*weight;joint.rz+=rotation.z*weight;
  }
}
