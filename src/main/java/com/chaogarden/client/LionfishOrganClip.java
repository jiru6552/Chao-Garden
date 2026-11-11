package com.chaogarden.client;
import com.github.L_Ender.lionfishapi.client.model.AdvancedAnimations.*;
import com.github.L_Ender.lionfishapi.client.model.tools.*;
import com.chaogarden.toys.OrganPlayRules;
import java.util.*;
import net.minecraft.world.entity.Entity;
import org.joml.Vector3f;
final class LionfishOrganClip extends AdvancedEntityModel<Entity>{
 private final AdvancedModelBox root=new AdvancedModelBox(this,"root"),left=new AdvancedModelBox(this,"left"),right=new AdvancedModelBox(this,"right");
 private final Vector3f scratch=new Vector3f();
 private static AdvancedKeyframe key(int t,float amount){return new AdvancedKeyframe(t/20f,AdvancedKeyframeAnimations.degreeVec(amount*30,0,0),AdvancedAnimationChannel.Interpolations.LINEAR);}
 private static AdvancedAnimationDefinition definition(){
  var clip=AdvancedAnimationDefinition.Builder.withLength(8);
  for(boolean left:new boolean[]{false,true}){
   var keys=new java.util.TreeMap<Integer,Float>();keys.put(0,0f);keys.put(16,0f);
   for(int t=24;t<=136;t+=16){keys.put(t-4,0f);keys.put(t,1f);keys.put(t+10,1f);keys.put(t+12,0f);}
   keys.put(160,0f);clip.addAnimation(left?"left":"right",new AdvancedAnimationChannel(AdvancedAnimationChannel.Targets.ROTATION,
       keys.entrySet().stream().map(e->key(e.getKey(),e.getValue())).toArray(AdvancedKeyframe[]::new)));
  }return clip.build();
 }
 private static final AdvancedAnimationDefinition CLIP=definition();
 LionfishOrganClip(){root.addChild(left);root.addChild(right);updateDefaultPose();}
 float tap(float t,boolean l){resetToDefaultPose();if(!Float.isFinite(t))return 0;AdvancedKeyframeAnimations.animate(this,CLIP,Math.round(Math.clamp(t,0,160)*50),1,scratch);return Math.clamp((l?left:right).rotateAngleX/(float)Math.toRadians(30),0,1);}
 @Override public AdvancedModelBox root(){return root;}
 @Override public Iterable<AdvancedModelBox> getAllParts(){return List.of(root,left,right);}
 @Override public void setupAnim(Entity e,float swing,float amount,float age,float yaw,float pitch){}
}
