package com.chaogarden.client;

import com.chaogarden.core.ChaoFaceRules076;
import com.chaogarden.entity.ChaoEntity;
import com.chaogarden.toys.JackPlayRules080;
import org.joml.Vector3f;

/** Natural play and previews share the same tracked clock, production clip and face. */
final class ChaoJackAnimation080 {
  private static final LionfishJackClip080 CLIP=new LionfishJackClip080();
  private ChaoJackAnimation080() {}
  static boolean active(ChaoEntity c) {
    return JackPlayRules080.KIND.equals(c.toyPlayingKind())&&c.toyPlayingTicks()>0&&c.isAlive()
        &&c.hurtTime<=0&&!c.isCocooned()&&!c.isCompanionGripping()&&!c.officialComparison()
        &&!c.isResting()&&!c.isEating()&&!c.isBeingPetted()&&!c.isHandlingItem()&&!c.isPassenger()
        &&c.getHeldChaoItem().isEmpty()&&c.getMainHandItem().isEmpty()
        &&c.getBehavior()==ChaoEntity.Behavior.NONE;
  }
  static void pose(OfficialChaoRig rig,float elapsed) {
    float weight=JackPlayRules080.presence(elapsed);if(weight<=0)return;
    var sample=CLIP.sample(elapsed);add(rig,"skull",sample.head(),weight);
    add(rig,"arm_left",sample.left(),weight);add(rig,"arm_right",sample.right(),weight);
  }
  static float gazeYaw(boolean active,float value){return active?JackPlayRules080.gazeYaw(value):value;}
  static float gazePitch(boolean active,float value){return active?JackPlayRules080.gazePitch(value):value;}
  private static void add(OfficialChaoRig rig,String name,Vector3f r,float weight){
    var p=rig.pose(name);if(p!=null){p.rx+=r.x*weight;p.ry+=r.y*weight;p.rz+=r.z*weight;}
  }
  /** Never supersede injury, drowning, sleep, illness, care, or an owned mouth action. */
  static ChaoFaceRules076.Resolved face(ChaoFaceRules076.Resolved original,boolean active,float elapsed) {
    if(!active||!JackPlayRules080.startled(elapsed)||original.nativeExcluded()||!ordinary(original.eyeSource()))return original;
    boolean mouth=ordinary(original.mouthSource());
    return new ChaoFaceRules076.Resolved(original.baselineEyes(),original.baselineMouth(),
        ChaoFaceRules076.Eyes.SURPRISED,mouth?ChaoFaceRules076.Mouth.OPEN:original.mouth(),original.style(),
        ChaoFaceRules076.Source.ACTIVITY,mouth?ChaoFaceRules076.Source.ACTIVITY:original.mouthSource(),false,original.irisColor());
  }
  private static boolean ordinary(ChaoFaceRules076.Source source){return switch(source){
    case BASELINE,BLINK,MOOD,IDLE->true;default->false;};}
}
