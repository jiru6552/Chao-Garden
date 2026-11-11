package com.chaogarden.client;
import com.github.L_Ender.lionfishapi.client.model.AdvancedAnimations.*;
import com.github.L_Ender.lionfishapi.client.model.tools.*;
import com.chaogarden.toys.TrianglePlayRules;
import java.util.*;
import net.minecraft.world.entity.Entity;
import org.joml.Vector3f;
/** The beater reaches the rail at the same authored ticks used by server sound cues. */
final class LionfishTriangleClip extends AdvancedEntityModel<Entity>{
 private final AdvancedModelBox root=new AdvancedModelBox(this,"root"),beater=new AdvancedModelBox(this,"beater");
 private final Vector3f scratch=new Vector3f();
 private static AdvancedKeyframe key(int tick,float degrees){return new AdvancedKeyframe(tick/20f,AdvancedKeyframeAnimations.degreeVec(degrees,0,0),AdvancedAnimationChannel.Interpolations.CATMULLROM);}
 private static AdvancedAnimationDefinition definition(){
  var keys=new ArrayList<AdvancedKeyframe>();keys.add(key(0,0));keys.add(key(12,0));
  for(int t=0;t<160;t++)if(TrianglePlayRules.soundDue(t)){keys.add(key(t-5,0));keys.add(key(t,30));keys.add(key(t+3,0));}
  keys.add(key(160,0));
  return AdvancedAnimationDefinition.Builder.withLength(8).addAnimation("beater",new AdvancedAnimationChannel(AdvancedAnimationChannel.Targets.ROTATION,keys.toArray(AdvancedKeyframe[]::new))).build();
 }
 private static final AdvancedAnimationDefinition CLIP=definition();
 LionfishTriangleClip(){root.addChild(beater);updateDefaultPose();}
 float tap(float ticks){resetToDefaultPose();if(!Float.isFinite(ticks))return 0;AdvancedKeyframeAnimations.animate(this,CLIP,Math.round(Math.max(0,Math.min(160,ticks))*50),1,scratch);return Math.max(0,Math.min(1,beater.rotateAngleX/(float)Math.toRadians(30)));}
 @Override public AdvancedModelBox root(){return root;}
 @Override public Iterable<AdvancedModelBox> getAllParts(){return List.of(root,beater);}
 @Override public void setupAnim(Entity e,float swing,float amount,float age,float yaw,float pitch){}
}
