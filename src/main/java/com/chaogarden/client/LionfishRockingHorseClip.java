package com.chaogarden.client;

import com.chaogarden.toys.RockingHorsePlayRules;
import com.github.L_Ender.lionfishapi.client.model.AdvancedAnimations.*;
import com.github.L_Ender.lionfishapi.client.model.tools.*;
import java.util.*;
import net.minecraft.world.entity.Entity;
import org.joml.Vector3f;

/** A continuous fore/aft rocking track, shared by the rider and its original horse mesh. */
final class LionfishRockingHorseClip extends AdvancedEntityModel<Entity> {
  private final AdvancedModelBox root=new AdvancedModelBox(this,"root"),rock=new AdvancedModelBox(this,"rock");
  private final Vector3f scratch=new Vector3f();
  private static AdvancedAnimationDefinition definition(){
    var keys=new ArrayList<AdvancedKeyframe>();
    for(int tick=0;tick<=160;tick+=4)keys.add(new AdvancedKeyframe(tick/20f,
        AdvancedKeyframeAnimations.degreeVec(RockingHorsePlayRules.key(tick)*6,0,0),AdvancedAnimationChannel.Interpolations.CATMULLROM));
    return AdvancedAnimationDefinition.Builder.withLength(8).addAnimation("rock",new AdvancedAnimationChannel(
        AdvancedAnimationChannel.Targets.ROTATION,keys.toArray(AdvancedKeyframe[]::new))).build();
  }
  private static final AdvancedAnimationDefinition CLIP=definition();
  LionfishRockingHorseClip(){root.addChild(rock);updateDefaultPose();}
  float angle(float tick){
    resetToDefaultPose();if(!Float.isFinite(tick))return 0;
    AdvancedKeyframeAnimations.animate(this,CLIP,Math.round(Math.clamp(tick,0,160)*50),1,scratch);
    return rock.rotateAngleX*RockingHorsePlayRules.rockWeight(tick);
  }
  @Override public AdvancedModelBox root(){return root;}
  @Override public Iterable<AdvancedModelBox> getAllParts(){return List.of(root,rock);}
  @Override public void setupAnim(Entity entity,float swing,float amount,float age,float yaw,float pitch){}
}
