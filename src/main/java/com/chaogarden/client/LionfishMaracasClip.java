package com.chaogarden.client;

import com.chaogarden.toys.MaracasPlayRules;
import com.github.L_Ender.lionfishapi.client.model.AdvancedAnimations.*;
import com.github.L_Ender.lionfishapi.client.model.tools.*;
import java.util.*;
import net.minecraft.world.entity.Entity;
import org.joml.Vector3f;

/** Alternating two-arm pose sampling; the current Chao rig remains the renderer. */
final class LionfishMaracasClip extends AdvancedEntityModel<Entity> {
  private final AdvancedModelBox root=new AdvancedModelBox(this,"root");
  private final AdvancedModelBox right=new AdvancedModelBox(this,"arm_right"),left=new AdvancedModelBox(this,"arm_left");
  private final Vector3f scratch=new Vector3f();
  private static AdvancedKeyframe key(int tick,float yaw,float roll){return new AdvancedKeyframe(tick/20f,
      AdvancedKeyframeAnimations.degreeVec(0,yaw,roll),AdvancedAnimationChannel.Interpolations.CATMULLROM);}
  private static AdvancedAnimationDefinition definition(){
    var builder=AdvancedAnimationDefinition.Builder.withLength(8);
    for(boolean left:new boolean[]{false,true}){
      float side=left?1:-1;var keys=new ArrayList<AdvancedKeyframe>();
      keys.add(key(0,0,0));keys.add(key(12,side*52,-side*22));keys.add(key(16,side*52,-side*22));
      for(int tick=20;tick<=140;tick+=4){boolean up=((tick-20)/4%2==0)^left;
        keys.add(key(tick,side*(up?62:43),-side*(up?35:10)));}
      keys.add(key(146,side*52,-side*22));keys.add(key(160,0,0));
      builder.addAnimation(left?"arm_left":"arm_right",new AdvancedAnimationChannel(AdvancedAnimationChannel.Targets.ROTATION,keys.toArray(AdvancedKeyframe[]::new)));
    }return builder.build();
  }
  private static final AdvancedAnimationDefinition CLIP=definition();
  LionfishMaracasClip(){root.addChild(right);root.addChild(left);updateDefaultPose();}
  Vector3f sample(float ticks,boolean isLeft){resetToDefaultPose();if(!Float.isFinite(ticks))return new Vector3f();
    AdvancedKeyframeAnimations.animate(this,CLIP,Math.round(Math.max(0,Math.min(160,ticks))*50),1,scratch);
    var arm=isLeft?left:right;return new Vector3f(arm.rotateAngleX,arm.rotateAngleY,arm.rotateAngleZ);}
  @Override public AdvancedModelBox root(){return root;}
  @Override public Iterable<AdvancedModelBox> getAllParts(){return List.of(root,right,left);}
  @Override public void setupAnim(Entity e,float swing,float amount,float age,float yaw,float pitch){}
}
