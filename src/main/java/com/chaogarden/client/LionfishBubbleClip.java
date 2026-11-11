package com.chaogarden.client;

import com.github.L_Ender.lionfishapi.client.model.AdvancedAnimations.*;
import com.github.L_Ender.lionfishapi.client.model.tools.*;
import java.util.*;
import net.minecraft.world.entity.Entity;
import org.joml.Vector3f;

/** One hand overlay only; the existing renderer still owns all geometry and materials. */
final class LionfishBubbleClip extends AdvancedEntityModel<Entity> {
  private final AdvancedModelBox arm=new AdvancedModelBox(this,"arm_right");
  private final Vector3f scratch=new Vector3f();
  private static AdvancedKeyframe key(int t,float yaw,float roll){return new AdvancedKeyframe(t/20f,
      AdvancedKeyframeAnimations.degreeVec(0,yaw,roll),AdvancedAnimationChannel.Interpolations.LINEAR);}
  private static AdvancedAnimationDefinition definition(){
    var keys=new ArrayList<AdvancedKeyframe>();keys.add(key(0,0,0));keys.add(key(12,-35,20));
    for(int t:new int[]{26,74,122}){keys.add(key(t,-92,4));keys.add(key(t+20,-92,4));keys.add(key(t+30,-35,20));}
    keys.add(key(160,0,0));
    return AdvancedAnimationDefinition.Builder.withLength(8).addAnimation("arm_right",
        new AdvancedAnimationChannel(AdvancedAnimationChannel.Targets.ROTATION,keys.toArray(AdvancedKeyframe[]::new))).build();
  }
  private static final AdvancedAnimationDefinition CLIP=definition();
  LionfishBubbleClip(){updateDefaultPose();}
  Vector3f sample(float elapsed){resetToDefaultPose();if(!Float.isFinite(elapsed))return new Vector3f();
    AdvancedKeyframeAnimations.animate(this,CLIP,Math.round(Math.max(0,Math.min(160,elapsed))*50),1,scratch);
    return new Vector3f(arm.rotateAngleX,arm.rotateAngleY,arm.rotateAngleZ);}
  @Override public AdvancedModelBox root(){return arm;}
  @Override public Iterable<AdvancedModelBox> getAllParts(){return List.of(arm);}
  @Override public void setupAnim(Entity e,float s,float a,float t,float y,float p){}
}
