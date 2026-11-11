package com.chaogarden.client;
import com.github.L_Ender.lionfishapi.client.model.AdvancedAnimations.*;
import com.github.L_Ender.lionfishapi.client.model.tools.*;
import java.util.List;
import net.minecraft.world.entity.Entity;
import org.joml.Vector3f;
/** A small breath/note sway, sampled from Lionfish and shared by mouth and hand targets. */
final class LionfishTrumpetClip extends AdvancedEntityModel<Entity> {
 private final AdvancedModelBox root=new AdvancedModelBox(this,"root"),trumpet=new AdvancedModelBox(this,"trumpet");
 private final Vector3f scratch=new Vector3f();
 private static AdvancedKeyframe key(int tick,float lean){return new AdvancedKeyframe(tick/20f,AdvancedKeyframeAnimations.degreeVec(lean,0,0),AdvancedAnimationChannel.Interpolations.CATMULLROM);}
 private static final AdvancedAnimationDefinition CLIP=AdvancedAnimationDefinition.Builder.withLength(8)
  .addAnimation("trumpet",new AdvancedAnimationChannel(AdvancedAnimationChannel.Targets.ROTATION,new AdvancedKeyframe[]{key(0,0),key(12,0),key(24,.7f),key(32,-.7f),key(44,.7f),key(60,-.7f),key(72,0),key(80,0),key(88,.7f),key(96,-.7f),key(108,.7f),key(124,-.7f),key(140,0),key(160,0)})).build();
 LionfishTrumpetClip(){root.addChild(trumpet);updateDefaultPose();}
 float lean(float ticks){resetToDefaultPose();if(!Float.isFinite(ticks))return 0;AdvancedKeyframeAnimations.animate(this,CLIP,Math.round(Math.max(0,Math.min(160,ticks))*50),1,scratch);return trumpet.rotateAngleX;}
 @Override public AdvancedModelBox root(){return root;}
 @Override public Iterable<AdvancedModelBox> getAllParts(){return List.of(root,trumpet);}
 @Override public void setupAnim(Entity e,float swing,float amount,float age,float yaw,float pitch){}
}
