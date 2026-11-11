package com.chaogarden.client;
import com.github.L_Ender.lionfishapi.client.model.AdvancedAnimations.*;
import com.github.L_Ender.lionfishapi.client.model.tools.*;
import java.util.*;
import net.minecraft.world.entity.Entity;
import org.joml.Vector3f;
/** A raised hand bell, with a ringing stroke followed by a quieter backswing. */
final class LionfishBellClip extends AdvancedEntityModel<Entity>{
  private final AdvancedModelBox arm=new AdvancedModelBox(this,"arm_right");private final Vector3f scratch=new Vector3f();
  private static AdvancedKeyframe key(int tick,float yaw,float roll){return new AdvancedKeyframe(tick/20f,AdvancedKeyframeAnimations.degreeVec(0,yaw,roll),AdvancedAnimationChannel.Interpolations.CATMULLROM);}
  private static AdvancedAnimationDefinition definition(){
    var keys=new ArrayList<AdvancedKeyframe>();keys.add(key(0,0,0));keys.add(key(12,-55,50));keys.add(key(16,-55,50));
    for(int tick=24;tick<=136;tick+=8){boolean ring=(tick-24)%16==0;keys.add(key(tick,ring?-65:-45,ring?62:42));}
    keys.add(key(146,-55,50));keys.add(key(160,0,0));
    return AdvancedAnimationDefinition.Builder.withLength(8).addAnimation("arm_right",new AdvancedAnimationChannel(AdvancedAnimationChannel.Targets.ROTATION,keys.toArray(AdvancedKeyframe[]::new))).build();
  }
  private static final AdvancedAnimationDefinition CLIP=definition();
  LionfishBellClip(){updateDefaultPose();}
  Vector3f sample(float ticks){resetToDefaultPose();if(!Float.isFinite(ticks))return new Vector3f();AdvancedKeyframeAnimations.animate(this,CLIP,Math.round(Math.max(0,Math.min(160,ticks))*50),1,scratch);return new Vector3f(arm.rotateAngleX,arm.rotateAngleY,arm.rotateAngleZ);}
  @Override public AdvancedModelBox root(){return arm;}
  @Override public Iterable<AdvancedModelBox> getAllParts(){return List.of(arm);}
  @Override public void setupAnim(Entity e,float swing,float amount,float age,float yaw,float pitch){}
}
