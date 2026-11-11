package com.chaogarden.client;
import com.github.L_Ender.lionfishapi.client.model.AdvancedAnimations.*;
import com.github.L_Ender.lionfishapi.client.model.tools.*;
import java.util.*;
import net.minecraft.world.entity.Entity;
import org.joml.Vector3f;
final class LionfishDrumClip extends AdvancedEntityModel<Entity>{
 private final AdvancedModelBox root=new AdvancedModelBox(this,"root"),right=new AdvancedModelBox(this,"arm_right"),left=new AdvancedModelBox(this,"arm_left");
 private final Vector3f scratch=new Vector3f();
 private static AdvancedKeyframe key(int tick,float yaw){return new AdvancedKeyframe(tick/20f,AdvancedKeyframeAnimations.degreeVec(0,yaw,0),AdvancedAnimationChannel.Interpolations.CATMULLROM);}
 private static AdvancedAnimationDefinition definition(){
  var b=AdvancedAnimationDefinition.Builder.withLength(8);
  var strike=new ArrayList<AdvancedKeyframe>();strike.add(key(0,0));strike.add(key(12,105));strike.add(key(20,105));
  for(int t=24;t<=144;t+=8){strike.add(key(t,125));strike.add(key(t+2,105));}
  strike.add(key(160,0));
  b.addAnimation("arm_left",new AdvancedAnimationChannel(AdvancedAnimationChannel.Targets.ROTATION,strike.toArray(AdvancedKeyframe[]::new)));
  b.addAnimation("arm_right",new AdvancedAnimationChannel(AdvancedAnimationChannel.Targets.ROTATION,new AdvancedKeyframe[]{key(0,0),key(12,-110),key(146,-110),key(160,0)}));
  return b.build();
 }
 private static final AdvancedAnimationDefinition CLIP=definition();
 LionfishDrumClip(){root.addChild(right);root.addChild(left);updateDefaultPose();}
 Vector3f sample(float ticks,boolean isLeft){resetToDefaultPose();if(!Float.isFinite(ticks))return new Vector3f();AdvancedKeyframeAnimations.animate(this,CLIP,Math.round(Math.max(0,Math.min(160,ticks))*50),1,scratch);var a=isLeft?left:right;return new Vector3f(a.rotateAngleX,a.rotateAngleY,a.rotateAngleZ);}
 float tap(float ticks){return Math.max(0,Math.min(1,(sample(ticks,true).y-(float)Math.toRadians(105))/(float)Math.toRadians(20)));}
 @Override public AdvancedModelBox root(){return root;}
 @Override public Iterable<AdvancedModelBox> getAllParts(){return List.of(root,right,left);}
 @Override public void setupAnim(Entity e,float swing,float amount,float age,float yaw,float pitch){}
}
