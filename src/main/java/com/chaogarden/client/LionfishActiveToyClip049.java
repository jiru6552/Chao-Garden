package com.chaogarden.client;

import com.github.L_Ender.lionfishapi.client.model.AdvancedAnimations.*;
import com.github.L_Ender.lionfishapi.client.model.tools.*;
import java.util.*;
import net.minecraft.world.entity.Entity;
import org.joml.Vector3f;

/** Purposeful push/look-up, soothe/lift and landing-flex tracks on the real session clock. */
final class LionfishActiveToyClip049 extends AdvancedEntityModel<Entity> {
  private final AdvancedModelBox root=new AdvancedModelBox(this,"root"),push=new AdvancedModelBox(this,"push"),lift=new AdvancedModelBox(this,"lift"),flex=new AdvancedModelBox(this,"flex");
  private final Vector3f scratch=new Vector3f();
  private static AdvancedKeyframe key(int tick,float value){return new AdvancedKeyframe(tick/20f,AdvancedKeyframeAnimations.degreeVec(value*30,0,0),AdvancedAnimationChannel.Interpolations.CATMULLROM);}
  private static AdvancedAnimationChannel track(List<AdvancedKeyframe> keys){return new AdvancedAnimationChannel(AdvancedAnimationChannel.Targets.ROTATION,keys.toArray(AdvancedKeyframe[]::new));}
  private static AdvancedAnimationDefinition definition(){
    var p=new ArrayList<AdvancedKeyframe>();p.add(key(0,0));p.add(key(16,0));
    for(int tick=32;tick<=128;tick+=32){p.add(key(tick,1));p.add(key(tick+12,0));}p.add(key(160,0));
    var l=new ArrayList<AdvancedKeyframe>();l.add(key(0,0));l.add(key(20,0));
    for(int tick=36;tick<=132;tick+=48){l.add(key(tick,1));l.add(key(tick+12,0));}l.add(key(160,0));
    var f=new ArrayList<AdvancedKeyframe>();f.add(key(0,0));f.add(key(16,0));
    for(int tick=24;tick<=136;tick+=16){f.add(key(tick,1));f.add(key(tick+6,0));}f.add(key(152,0));f.add(key(160,0));
    return AdvancedAnimationDefinition.Builder.withLength(8).addAnimation("push",track(p)).addAnimation("lift",track(l)).addAnimation("flex",track(f)).build();
  }
  private static final AdvancedAnimationDefinition CLIP=definition();
  LionfishActiveToyClip049(){root.addChild(push);root.addChild(lift);root.addChild(flex);updateDefaultPose();}
  private float sample(AdvancedModelBox part,float tick){resetToDefaultPose();if(Float.isFinite(tick))AdvancedKeyframeAnimations.animate(this,CLIP,Math.round(Math.clamp(tick,0,160)*50),1,scratch);return Math.clamp(part.rotateAngleX/(float)Math.toRadians(30),0,1);}
  float push(float tick){return sample(push,tick);}float lift(float tick){return sample(lift,tick);}float flex(float tick){return sample(flex,tick);}
  @Override public AdvancedModelBox root(){return root;}@Override public Iterable<AdvancedModelBox> getAllParts(){return List.of(root,push,lift,flex);}
  @Override public void setupAnim(Entity e,float s,float a,float t,float y,float p){}
}
