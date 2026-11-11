package com.chaogarden.client;

import com.chaogarden.toys.RattlePlayRules;
import com.github.L_Ender.lionfishapi.client.model.AdvancedAnimations.*;
import com.github.L_Ender.lionfishapi.client.model.tools.*;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.world.entity.Entity;
import org.joml.Vector3f;

/** Pose-only bridge: Lionfish samples one joint; the existing rig renders every vertex. */
final class LionfishRattleClip extends AdvancedEntityModel<Entity> {
  private final AdvancedModelBox arm = new AdvancedModelBox(this,"arm_right");
  private final Vector3f scratch = new Vector3f();
  private static AdvancedKeyframe key(int tick,float yaw,float roll) {
    return new AdvancedKeyframe(tick/20f,AdvancedKeyframeAnimations.degreeVec(0,yaw,roll),
        AdvancedAnimationChannel.Interpolations.LINEAR);
  }
  private static AdvancedAnimationDefinition definition() {
    var keys=new ArrayList<AdvancedKeyframe>();
    keys.add(key(0,0,0));keys.add(key(RattlePlayRules.RAISE_END,-60,22));keys.add(key(16,-60,22));
    for(int tick=RattlePlayRules.SHAKE_START;tick<=RattlePlayRules.SHAKE_END;tick+=RattlePlayRules.SHAKE_STEP) {
      boolean out=((tick-RattlePlayRules.SHAKE_START)/RattlePlayRules.SHAKE_STEP)%2==0;
      keys.add(key(tick,out?-74:-48,out?30:14));
    }
    keys.add(key(136,-60,22));keys.add(key(RattlePlayRules.LOWER_START,-60,22));keys.add(key(RattlePlayRules.DURATION,0,0));
    return AdvancedAnimationDefinition.Builder.withLength(RattlePlayRules.DURATION/20f)
        .addAnimation("arm_right",new AdvancedAnimationChannel(AdvancedAnimationChannel.Targets.ROTATION,keys.toArray(AdvancedKeyframe[]::new))).build();
  }
  private static final AdvancedAnimationDefinition CLIP=definition();
  LionfishRattleClip(){updateDefaultPose();}
  Vector3f sample(float elapsedTicks) {
    resetToDefaultPose();
    if(!Float.isFinite(elapsedTicks))return new Vector3f();
    long millis=Math.round(Math.max(0,Math.min(RattlePlayRules.DURATION,elapsedTicks))*50);
    AdvancedKeyframeAnimations.animate(this,CLIP,millis,1,scratch);
    return new Vector3f(arm.rotateAngleX,arm.rotateAngleY,arm.rotateAngleZ);
  }
  @Override public AdvancedModelBox root(){return arm;}
  @Override public Iterable<AdvancedModelBox> getAllParts(){return List.of(arm);}
  @Override public void setupAnim(Entity entity,float swing,float amount,float age,float yaw,float pitch){}
}
