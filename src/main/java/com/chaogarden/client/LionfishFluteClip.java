package com.chaogarden.client;
import com.github.L_Ender.lionfishapi.client.model.AdvancedAnimations.*;
import com.github.L_Ender.lionfishapi.client.model.tools.*;
import java.util.List;
import net.minecraft.world.entity.Entity;
import org.joml.Vector3f;
/** A small breath/note sway, sampled from Lionfish and shared by mouth and hand targets. */
final class LionfishFluteClip extends AdvancedEntityModel<Entity> {
 private final AdvancedModelBox root=new AdvancedModelBox(this,"root"),flute=new AdvancedModelBox(this,"flute");
 private final Vector3f scratch=new Vector3f();
 private static AdvancedKeyframe key(int tick,float lean){return new AdvancedKeyframe(tick/20f,AdvancedKeyframeAnimations.degreeVec(lean,0,0),AdvancedAnimationChannel.Interpolations.CATMULLROM);}
 private static final AdvancedAnimationDefinition CLIP=AdvancedAnimationDefinition.Builder.withLength(8)
  .addAnimation("flute",new AdvancedAnimationChannel(AdvancedAnimationChannel.Targets.ROTATION,new AdvancedKeyframe[]{key(0,0),key(12,0),key(24,1),key(36,-1),key(48,1),key(60,-1),key(72,0),key(80,0),key(88,1),key(100,-1),key(112,1),key(124,-1),key(140,0),key(160,0)})).build();
 LionfishFluteClip(){root.addChild(flute);updateDefaultPose();}
 float lean(float ticks){resetToDefaultPose();if(!Float.isFinite(ticks))return 0;AdvancedKeyframeAnimations.animate(this,CLIP,Math.round(Math.max(0,Math.min(160,ticks))*50),1,scratch);return flute.rotateAngleX;}
 @Override public AdvancedModelBox root(){return root;}
 @Override public Iterable<AdvancedModelBox> getAllParts(){return List.of(root,flute);}
 @Override public void setupAnim(Entity e,float swing,float amount,float age,float yaw,float pitch){}
}
