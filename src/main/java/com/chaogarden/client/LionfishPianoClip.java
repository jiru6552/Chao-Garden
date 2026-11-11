package com.chaogarden.client;
import com.github.L_Ender.lionfishapi.client.model.AdvancedAnimations.*;
import com.github.L_Ender.lionfishapi.client.model.tools.*;
import com.chaogarden.toys.PianoPlayRules;
import java.util.*;
import net.minecraft.world.entity.Entity;
import org.joml.Vector3f;
final class LionfishPianoClip extends AdvancedEntityModel<Entity>{
 private final AdvancedModelBox root=new AdvancedModelBox(this,"root"),left=new AdvancedModelBox(this,"left"),right=new AdvancedModelBox(this,"right");
 private final Vector3f scratch=new Vector3f();
 private static AdvancedKeyframe key(int t,float amount){return new AdvancedKeyframe(t/20f,AdvancedKeyframeAnimations.degreeVec(amount*30,0,0),AdvancedAnimationChannel.Interpolations.CATMULLROM);}
 private static AdvancedAnimationDefinition definition(){
  var clip=AdvancedAnimationDefinition.Builder.withLength(8);
  for(boolean left:new boolean[]{false,true}){
   var keys=new ArrayList<AdvancedKeyframe>();keys.add(key(0,0));keys.add(key(16,0));
   for(int t=24;t<=144;t+=8)if(PianoPlayRules.left(t)==left){keys.add(key(t-4,0));keys.add(key(t,1));keys.add(key(t+3,0));}
   keys.add(key(160,0));clip.addAnimation(left?"left":"right",new AdvancedAnimationChannel(AdvancedAnimationChannel.Targets.ROTATION,keys.toArray(AdvancedKeyframe[]::new)));
  }return clip.build();
 }
 private static final AdvancedAnimationDefinition CLIP=definition();
 LionfishPianoClip(){root.addChild(left);root.addChild(right);updateDefaultPose();}
 float tap(float t,boolean l){resetToDefaultPose();if(!Float.isFinite(t))return 0;AdvancedKeyframeAnimations.animate(this,CLIP,Math.round(Math.clamp(t,0,160)*50),1,scratch);return Math.clamp((l?left:right).rotateAngleX/(float)Math.toRadians(30),0,1);}
 @Override public AdvancedModelBox root(){return root;}
 @Override public Iterable<AdvancedModelBox> getAllParts(){return List.of(root,left,right);}
 @Override public void setupAnim(Entity e,float swing,float amount,float age,float yaw,float pitch){}
}
