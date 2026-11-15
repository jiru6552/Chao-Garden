package com.chaogarden.client;

import com.github.L_Ender.lionfishapi.client.model.AdvancedAnimations.*;
import com.github.L_Ender.lionfishapi.client.model.tools.*;
import java.util.List;
import net.minecraft.world.entity.Entity;
import org.joml.Vector3f;

/** Joint-only anticipation and startle. The production rig still renders the Chao. */
final class LionfishJackClip080 extends AdvancedEntityModel<Entity> {
  private final AdvancedModelBox root=new AdvancedModelBox(this,"root"),head=new AdvancedModelBox(this,"skull"),
      left=new AdvancedModelBox(this,"arm_left"),right=new AdvancedModelBox(this,"arm_right");
  private final Vector3f scratch=new Vector3f();
  record Sample(Vector3f head,Vector3f left,Vector3f right) {}
  private static AdvancedAnimationChannel channel(float[][] rows) {
    var keys=new AdvancedKeyframe[rows.length];
    for(int i=0;i<rows.length;i++){var r=rows[i];keys[i]=new AdvancedKeyframe(r[0]/20f,
        AdvancedKeyframeAnimations.degreeVec(r[1],r[2],r[3]),AdvancedAnimationChannel.Interpolations.CATMULLROM);}
    return new AdvancedAnimationChannel(AdvancedAnimationChannel.Targets.ROTATION,keys);
  }
  private static final AdvancedAnimationDefinition CLIP=AdvancedAnimationDefinition.Builder.withLength(8)
      .addAnimation("skull",channel(new float[][]{
          {0,0,0,0},{16,4,-3,-2},{30,6,3,2},{38,5,0,0},{44,-9,0,-3},{51,-7,0,-2},
          {65,1,0,0},{84,4,-2,2},{108,2,2,-1},{128,2,0,0},{144,0,0,0},{160,0,0,0}}))
      .addAnimation("arm_left",channel(new float[][]{
          {0,0,0,0},{18,-2,0,-3},{36,-3,0,-4},{40,-3,0,-4},{46,-10,0,-10},
          {58,-7,0,-7},{74,-2,0,-2},{114,-2,0,-2},{140,0,0,0},{160,0,0,0}}))
      .addAnimation("arm_right",channel(new float[][]{
          {0,0,0,0},{18,-2,0,3},{36,-3,0,4},{40,-3,0,4},{46,-10,0,10},
          {58,-7,0,7},{74,-2,0,2},{114,-2,0,2},{140,0,0,0},{160,0,0,0}})).build();
  LionfishJackClip080(){root.addChild(head);root.addChild(left);root.addChild(right);updateDefaultPose();}
  Sample sample(float elapsed) {
    resetToDefaultPose();
    if(Float.isFinite(elapsed))AdvancedKeyframeAnimations.animate(this,CLIP,Math.round(Math.clamp(elapsed,0,160)*50),1,scratch);
    return new Sample(rotation(head),rotation(left),rotation(right));
  }
  private static Vector3f rotation(AdvancedModelBox p){return new Vector3f(p.rotateAngleX,p.rotateAngleY,p.rotateAngleZ);}
  @Override public AdvancedModelBox root(){return root;}
  @Override public Iterable<AdvancedModelBox> getAllParts(){return List.of(root,head,left,right);}
  @Override public void setupAnim(Entity entity,float swing,float amount,float age,float yaw,float pitch){}
}
