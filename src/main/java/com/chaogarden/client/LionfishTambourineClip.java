package com.chaogarden.client;
import com.github.L_Ender.lionfishapi.client.model.AdvancedAnimations.*;
import com.github.L_Ender.lionfishapi.client.model.tools.*;
import java.util.*;
import net.minecraft.world.entity.Entity;
import org.joml.Vector3f;
/** Right hand supports the rim; the left approaches, taps, and recoils. */
final class LionfishTambourineClip extends AdvancedEntityModel<Entity>{
  private final AdvancedModelBox root=new AdvancedModelBox(this,"root");
  private final AdvancedModelBox right=new AdvancedModelBox(this,"arm_right"),left=new AdvancedModelBox(this,"arm_left");
  private final Vector3f scratch=new Vector3f();
  private static AdvancedKeyframe key(int t,float yaw,float roll){return new AdvancedKeyframe(t/20f,AdvancedKeyframeAnimations.degreeVec(0,yaw,roll),AdvancedAnimationChannel.Interpolations.CATMULLROM);}
  private static AdvancedAnimationDefinition definition(){
    var b=AdvancedAnimationDefinition.Builder.withLength(8);
    for(boolean left:new boolean[]{false,true}){
      var keys=new ArrayList<AdvancedKeyframe>();keys.add(key(0,0,0));keys.add(key(12,left?55:-55,left?-30:30));keys.add(key(16,left?55:-55,left?-30:30));
      for(int t=24;t<=136;t+=16){keys.add(key(t,left?75:-57,left?-35:32));keys.add(key(t+5,left?55:-55,left?-30:30));if(t<136)keys.add(key(t+10,left?55:-55,left?-30:30));}
      keys.add(key(146,left?55:-55,left?-30:30));keys.add(key(160,0,0));
      b.addAnimation(left?"arm_left":"arm_right",new AdvancedAnimationChannel(AdvancedAnimationChannel.Targets.ROTATION,keys.toArray(AdvancedKeyframe[]::new)));
    }return b.build();
  }
  private static final AdvancedAnimationDefinition CLIP=definition();
  LionfishTambourineClip(){root.addChild(right);root.addChild(left);updateDefaultPose();}
  Vector3f sample(float ticks,boolean isLeft){resetToDefaultPose();if(!Float.isFinite(ticks))return new Vector3f();AdvancedKeyframeAnimations.animate(this,CLIP,Math.round(Math.max(0,Math.min(160,ticks))*50),1,scratch);var a=isLeft?left:right;return new Vector3f(a.rotateAngleX,a.rotateAngleY,a.rotateAngleZ);}
  float tap(float ticks){return Math.max(0,Math.min(1,(sample(ticks,true).y-(float)Math.toRadians(55))/(float)Math.toRadians(20)));}
  @Override public AdvancedModelBox root(){return root;}
  @Override public Iterable<AdvancedModelBox> getAllParts(){return List.of(root,right,left);}
  @Override public void setupAnim(Entity e,float swing,float amount,float age,float yaw,float pitch){}
}
