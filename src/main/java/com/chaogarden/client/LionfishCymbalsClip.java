package com.chaogarden.client;
import com.github.L_Ender.lionfishapi.client.model.AdvancedAnimations.*;
import com.github.L_Ender.lionfishapi.client.model.tools.*;
import java.util.*;
import net.minecraft.world.entity.Entity;
import org.joml.Vector3f;
/** Both hands approach, meet at the rim and recoil before the next clash. */
final class LionfishCymbalsClip extends AdvancedEntityModel<Entity>{
 private final AdvancedModelBox root=new AdvancedModelBox(this,"root"),right=new AdvancedModelBox(this,"arm_right"),left=new AdvancedModelBox(this,"arm_left");
 private final Vector3f scratch=new Vector3f();
 private static AdvancedKeyframe key(int tick,float yaw,float roll){return new AdvancedKeyframe(tick/20f,AdvancedKeyframeAnimations.degreeVec(0,yaw,roll),AdvancedAnimationChannel.Interpolations.CATMULLROM);}
 private static AdvancedAnimationDefinition definition(){
  var b=AdvancedAnimationDefinition.Builder.withLength(8);
  for(boolean left:new boolean[]{false,true}){
   float sign=left?1:-1;var keys=new ArrayList<AdvancedKeyframe>();keys.add(key(0,0,0));keys.add(key(12,sign*85,0));keys.add(key(20,sign*85,0));
   for(int t=32;t<=128;t+=32){keys.add(key(t-8,sign*85,0));keys.add(key(t-3,sign*105,0));keys.add(key(t,sign*125,0));keys.add(key(t+3,sign*100,0));keys.add(key(t+8,sign*85,0));}
   keys.add(key(146,sign*85,0));keys.add(key(160,0,0));
   b.addAnimation(left?"arm_left":"arm_right",new AdvancedAnimationChannel(AdvancedAnimationChannel.Targets.ROTATION,keys.toArray(AdvancedKeyframe[]::new)));
  }return b.build();
 }
 private static final AdvancedAnimationDefinition CLIP=definition();
 LionfishCymbalsClip(){root.addChild(right);root.addChild(left);updateDefaultPose();}
 Vector3f sample(float ticks,boolean isLeft){resetToDefaultPose();if(!Float.isFinite(ticks))return new Vector3f();AdvancedKeyframeAnimations.animate(this,CLIP,Math.round(Math.max(0,Math.min(160,ticks))*50),1,scratch);var a=isLeft?left:right;return new Vector3f(a.rotateAngleX,a.rotateAngleY,a.rotateAngleZ);}
 float contact(float ticks){return Math.max(0,Math.min(1,(sample(ticks,true).y-(float)Math.toRadians(85))/(float)Math.toRadians(40)));}
 @Override public AdvancedModelBox root(){return root;}
 @Override public Iterable<AdvancedModelBox> getAllParts(){return List.of(root,right,left);}
 @Override public void setupAnim(Entity e,float swing,float amount,float age,float yaw,float pitch){}
}
