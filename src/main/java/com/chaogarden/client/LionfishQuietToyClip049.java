package com.chaogarden.client;

import com.github.L_Ender.lionfishapi.client.model.AdvancedAnimations.*;
import com.github.L_Ender.lionfishapi.client.model.tools.*;
import java.util.*;
import net.minecraft.world.entity.Entity;
import org.joml.Vector3f;

/** Original fitted sweep, page-turn and drawing curves on the production toy clock. */
final class LionfishQuietToyClip049 extends AdvancedEntityModel<Entity> {
  private final AdvancedModelBox root=new AdvancedModelBox(this,"root"),sweep=new AdvancedModelBox(this,"sweep"),page=new AdvancedModelBox(this,"page"),stroke=new AdvancedModelBox(this,"stroke"),pen=new AdvancedModelBox(this,"pen");
  private final Vector3f scratch=new Vector3f();
  private static AdvancedKeyframe key(int tick,float value){return new AdvancedKeyframe(tick/20f,AdvancedKeyframeAnimations.degreeVec(value*30,0,0),AdvancedAnimationChannel.Interpolations.CATMULLROM);}
  private static AdvancedAnimationChannel channel(List<AdvancedKeyframe> points){return new AdvancedAnimationChannel(AdvancedAnimationChannel.Targets.ROTATION,points.toArray(AdvancedKeyframe[]::new));}
  private static AdvancedAnimationDefinition definition(){
    var sweep=new ArrayList<AdvancedKeyframe>();sweep.add(key(0,0));sweep.add(key(16,-.85f));
    for(int tick=24;tick<=144;tick+=8)sweep.add(key(tick,((tick-24)/8)%4==1?.85f:((tick-24)/8)%4==3?-.85f:0));sweep.add(key(160,0));
    var page=new ArrayList<AdvancedKeyframe>();page.add(key(0,0));
    for(int start:new int[]{44,108}){page.add(key(start,0));page.add(key(start+8,.18f));page.add(key(start+16,.72f));page.add(key(start+24,1));page.add(key(start+34,1));page.add(key(start+36,0));}page.add(key(160,0));
    var stroke=new ArrayList<AdvancedKeyframe>();var pen=new ArrayList<AdvancedKeyframe>();stroke.add(key(0,0));pen.add(key(0,0));
    for(int tick=24;tick<=136;tick+=16){if(tick==24)stroke.add(key(tick-2,0));stroke.add(key(tick,0));stroke.add(key(tick+10,1));stroke.add(key(tick+12,1));stroke.add(key(tick+14,0));pen.add(key(tick-2,0));pen.add(key(tick,1));pen.add(key(tick+10,1));pen.add(key(tick+12,0));}
    stroke.add(key(160,0));pen.add(key(160,0));
    return AdvancedAnimationDefinition.Builder.withLength(8).addAnimation("sweep",channel(sweep)).addAnimation("page",channel(page)).addAnimation("stroke",channel(stroke)).addAnimation("pen",channel(pen)).build();
  }
  private static final AdvancedAnimationDefinition CLIP=definition();
  LionfishQuietToyClip049(){root.addChild(sweep);root.addChild(page);root.addChild(stroke);root.addChild(pen);updateDefaultPose();}
  private void sample(float tick){resetToDefaultPose();if(Float.isFinite(tick))AdvancedKeyframeAnimations.animate(this,CLIP,Math.round(Math.clamp(tick,0,160)*50),1,scratch);}
  private float value(AdvancedModelBox node){return node.rotateAngleX/(float)Math.toRadians(30);}
  float sweep(float tick){sample(tick);return Math.clamp(value(sweep),-1,1);}
  float page(float tick){sample(tick);return Math.clamp(value(page),0,1);}
  float stroke(float tick){sample(tick);return Math.clamp(value(stroke),0,1);}
  float pen(float tick){sample(tick);return Math.clamp(value(pen),0,1);}
  @Override public AdvancedModelBox root(){return root;}
  @Override public Iterable<AdvancedModelBox> getAllParts(){return List.of(root,sweep,page,stroke,pen);}
  @Override public void setupAnim(Entity entity,float swing,float amount,float age,float yaw,float pitch){}
}
