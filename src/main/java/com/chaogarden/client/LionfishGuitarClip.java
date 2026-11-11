package com.chaogarden.client;
import com.github.L_Ender.lionfishapi.client.model.AdvancedAnimations.*;
import com.github.L_Ender.lionfishapi.client.model.tools.*;
import java.util.*;
import net.minecraft.world.entity.Entity;
import org.joml.Vector3f;
/** Actual Lionfish stroke crossings, plus the fretting hand's small chord changes. */
final class LionfishGuitarClip extends AdvancedEntityModel<Entity> {
 private final AdvancedModelBox root=new AdvancedModelBox(this,"root"),strum=new AdvancedModelBox(this,"strum"),fret=new AdvancedModelBox(this,"fret");
 private final Vector3f scratch=new Vector3f();
 private static AdvancedKeyframe key(int t,float v){return new AdvancedKeyframe(t/20f,AdvancedKeyframeAnimations.degreeVec(v*30,0,0),AdvancedAnimationChannel.Interpolations.CATMULLROM);}
 private static AdvancedAnimationDefinition definition(){
  var strokes=new ArrayList<AdvancedKeyframe>();strokes.add(key(0,0));strokes.add(key(16,0));
  for(int t=20;t<=148;t+=4){int step=(t-20)/4;strokes.add(key(t,switch(step%4){case 0->-1;case 2->1;default->0;}));}
  strokes.add(key(156,0));strokes.add(key(160,0));
  var frets=new ArrayList<AdvancedKeyframe>();frets.add(key(0,-1));int[] changes={-10,-3,6,0,8,2,-6,-10};
  for(int i=0;i<8;i++)frets.add(key(20+16*i,changes[i]/10f));frets.add(key(160,-1));
  return AdvancedAnimationDefinition.Builder.withLength(8)
   .addAnimation("strum",new AdvancedAnimationChannel(AdvancedAnimationChannel.Targets.ROTATION,strokes.toArray(AdvancedKeyframe[]::new)))
   .addAnimation("fret",new AdvancedAnimationChannel(AdvancedAnimationChannel.Targets.ROTATION,frets.toArray(AdvancedKeyframe[]::new))).build();
 }
 private static final AdvancedAnimationDefinition CLIP=definition();
 LionfishGuitarClip(){root.addChild(strum);root.addChild(fret);updateDefaultPose();}
 private void sample(float t){resetToDefaultPose();if(Float.isFinite(t))AdvancedKeyframeAnimations.animate(this,CLIP,Math.round(Math.clamp(t,0,160)*50),1,scratch);}
 float strum(float t){sample(t);return Math.clamp(strum.rotateAngleX/(float)Math.toRadians(30),-1,1);}
 float fret(float t){sample(t);return .60f+.04f*Math.clamp(fret.rotateAngleX/(float)Math.toRadians(30),-1,1);}
 @Override public AdvancedModelBox root(){return root;}
 @Override public Iterable<AdvancedModelBox> getAllParts(){return List.of(root,strum,fret);}
 @Override public void setupAnim(Entity e,float swing,float amount,float age,float yaw,float pitch){}
}
