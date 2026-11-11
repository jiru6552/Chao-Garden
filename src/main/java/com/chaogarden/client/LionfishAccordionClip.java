package com.chaogarden.client;

import com.github.L_Ender.lionfishapi.client.model.AdvancedAnimations.*;
import com.github.L_Ender.lionfishapi.client.model.tools.*;
import java.util.*;
import net.minecraft.world.entity.Entity;
import org.joml.Vector3f;

/** Four smooth bellows breaths, with key contact beginning at each sustained reed attack. */
final class LionfishAccordionClip extends AdvancedEntityModel<Entity> {
  private final AdvancedModelBox root=new AdvancedModelBox(this,"root"),
      bellows=new AdvancedModelBox(this,"bellows"),keys=new AdvancedModelBox(this,"keys"),
      bass=new AdvancedModelBox(this,"bass"),register=new AdvancedModelBox(this,"register");
  private final Vector3f scratch=new Vector3f();
  private static AdvancedKeyframe key(int tick,float value){
    return new AdvancedKeyframe(tick/20f,AdvancedKeyframeAnimations.degreeVec(value*30,0,0),AdvancedAnimationChannel.Interpolations.CATMULLROM);
  }
  private static AdvancedAnimationChannel channel(List<AdvancedKeyframe> keys){
    return new AdvancedAnimationChannel(AdvancedAnimationChannel.Targets.ROTATION,keys.toArray(AdvancedKeyframe[]::new));
  }
  private static AdvancedAnimationDefinition definition(){
    var breaths=new ArrayList<AdvancedKeyframe>();breaths.add(key(0,0));breaths.add(key(16,0));
    for(int tick=24;tick<=144;tick+=8)breaths.add(key(tick,tick%16==8?.5f:(tick/16)%2==0?1:0));
    breaths.add(key(160,0));
    var fingers=new ArrayList<AdvancedKeyframe>();fingers.add(key(0,0));fingers.add(key(20,0));
    var basses=new ArrayList<AdvancedKeyframe>();basses.add(key(0,0));basses.add(key(20,0));
    for(int tick=24;tick<=136;tick+=16){
      fingers.add(key(tick,1));fingers.add(key(tick+10,1));fingers.add(key(tick+12,0));
      basses.add(key(tick,1));basses.add(key(tick+8,.35f));basses.add(key(tick+12,0));
    }
    fingers.add(key(160,0));basses.add(key(160,0));
    var positions=new ArrayList<AdvancedKeyframe>();positions.add(key(0,0));
    float[] notes={-.55f,-.25f,.05f,.55f,.25f,.05f,-.25f,-.55f};
    for(int i=0;i<8;i++)positions.add(key(20+16*i,notes[i]));
    positions.add(key(152,0));positions.add(key(160,0));
    return AdvancedAnimationDefinition.Builder.withLength(8)
        .addAnimation("bellows",channel(breaths)).addAnimation("keys",channel(fingers))
        .addAnimation("bass",channel(basses)).addAnimation("register",channel(positions)).build();
  }
  private static final AdvancedAnimationDefinition CLIP=definition();
  LionfishAccordionClip(){root.addChild(bellows);root.addChild(keys);root.addChild(bass);root.addChild(register);updateDefaultPose();}
  private void sample(float tick){
    resetToDefaultPose();
    if(Float.isFinite(tick))AdvancedKeyframeAnimations.animate(this,CLIP,Math.round(Math.clamp(tick,0,160)*50),1,scratch);
  }
  private float value(AdvancedModelBox part){return part.rotateAngleX/(float)Math.toRadians(30);}
  float bellows(float tick){sample(tick);return Math.clamp(value(bellows),0,1);}
  float keys(float tick){sample(tick);return Math.clamp(value(keys),0,1);}
  float bass(float tick){sample(tick);return Math.clamp(value(bass),0,1);}
  float keyHeight(float tick){sample(tick);return .53f+.075f*Math.clamp(value(register),-1,1);}
  @Override public AdvancedModelBox root(){return root;}
  @Override public Iterable<AdvancedModelBox> getAllParts(){return List.of(root,bellows,keys,bass,register);}
  @Override public void setupAnim(Entity entity,float swing,float amount,float age,float yaw,float pitch){}
}
