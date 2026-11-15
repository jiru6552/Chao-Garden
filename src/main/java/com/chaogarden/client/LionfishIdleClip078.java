package com.chaogarden.client;

import com.chaogarden.core.ChaoIdleRules078;
import com.github.L_Ender.lionfishapi.client.model.AdvancedAnimations.*;
import com.github.L_Ender.lionfishapi.client.model.tools.*;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.world.entity.Entity;
import org.joml.Vector3f;

/** Short, authored Lionfish joint tracks. The registered Chao rig renders the skin. */
final class LionfishIdleClip078 extends AdvancedEntityModel<Entity> {
  private final AdvancedModelBox root=new AdvancedModelBox(this,"root");
  private final AdvancedModelBox head=new AdvancedModelBox(this,"skull");
  private final AdvancedModelBox left=new AdvancedModelBox(this,"arm_left");
  private final AdvancedModelBox right=new AdvancedModelBox(this,"arm_right");
  private final Vector3f scratch=new Vector3f();
  record Sample(Vector3f head,Vector3f left,Vector3f right) {}

  // Each row is normalized episode time followed by XYZ degrees. Authored
  // holds and uneven accents distinguish a glance, thought and little laugh.
  private static AdvancedAnimationChannel track(String id,float[][] values) {
    var keys=new AdvancedKeyframe[values.length];
    for(int i=0;i<values.length;i++){
      var v=values[i];keys[i]=new AdvancedKeyframe(v[0]*ChaoIdleRules078.duration(id)/20f,
          AdvancedKeyframeAnimations.degreeVec(v[1],v[2],v[3]),AdvancedAnimationChannel.Interpolations.CATMULLROM);
    }
    return new AdvancedAnimationChannel(AdvancedAnimationChannel.Targets.ROTATION,keys);
  }
  private static AdvancedAnimationDefinition clip(String id,float[][] head,float[][] left,float[][] right) {
    var builder=AdvancedAnimationDefinition.Builder.withLength(ChaoIdleRules078.duration(id)/20f)
        .addAnimation("skull",track(id,head));
    if(left!=null)builder.addAnimation("arm_left",track(id,left));
    if(right!=null)builder.addAnimation("arm_right",track(id,right));
    return builder.build();
  }
  private static Map<String,AdvancedAnimationDefinition> definitions() {
    var clips=new LinkedHashMap<String,AdvancedAnimationDefinition>();
    clips.put("look_around",clip("look_around",new float[][]{
        {0,0,0,0},{.18f,-2,-18,-3},{.37f,-2,-18,-3},{.66f,-1,21,3},{.81f,-1,21,3},{1,0,0,0}},null,null));
    clips.put("look_up",clip("look_up",new float[][]{
        {0,0,0,0},{.24f,-15,-3,-2},{.57f,-15,3,1},{.76f,-10,1,0},{1,0,0,0}},null,null));
    clips.put("think",clip("think",new float[][]{
        {0,0,0,0},{.23f,4,-7,4},{.51f,4,-7,4},{.67f,1,-3,3},{.80f,3,-5,4},{1,0,0,0}},new float[][]{
        {0,0,0,0},{.24f,-2,8,-8},{.64f,-2,8,-8},{.78f,-1,6,-6},{1,0,0,0}},null));
    clips.put("bashful",clip("bashful",new float[][]{
        {0,0,0,0},{.23f,11,-9,7},{.53f,11,-9,7},{.72f,9,2,5},{1,0,0,0}},new float[][]{
        {0,0,0,0},{.24f,-1,3,-8},{.65f,-1,3,-8},{1,0,0,0}},new float[][]{
        {0,0,0,0},{.24f,-1,-3,8},{.65f,-1,-3,8},{1,0,0,0}}));
    clips.put("chuckle",clip("chuckle",new float[][]{
        {0,0,0,0},{.23f,3,0,-4},{.35f,7,0,-3},{.43f,3,0,-4},{.54f,7,0,-3},{.64f,3,0,-4},{.77f,5,0,-2},{1,0,0,0}},new float[][]{
        {0,0,0,0},{.24f,-1,6,-8},{.36f,-2,7,-10},{.44f,-1,6,-8},{.55f,-2,7,-10},{.66f,-1,6,-8},{1,0,0,0}},new float[][]{
        {0,0,0,0},{.25f,0,0,5},{.40f,0,0,7},{.56f,0,0,5},{.70f,0,0,6},{1,0,0,0}}));
    clips.put("tired",clip("tired",new float[][]{
        {0,0,0,0},{.20f,7,0,-4},{.39f,14,0,-5},{.51f,9,0,-4},{.70f,15,0,-5},{.84f,9,0,-3},{1,0,0,0}},null,null));
    clips.put("ground_play",clip("ground_play",new float[][]{
        {0,0,0,0},{.18f,10,-5,2},{.38f,10,-5,2},{.57f,12,4,-1},{.78f,10,-2,1},{1,0,0,0}},new float[][]{
        {0,0,0,0},{.19f,-2,3,-12},{.34f,-1,9,-15},{.48f,-2,3,-12},{.63f,-1,9,-15},{.79f,-2,4,-12},{1,0,0,0}},new float[][]{
        {0,0,0,0},{.20f,0,0,20},{.77f,0,0,20},{1,0,0,0}}));
    return Map.copyOf(clips);
  }
  private static final Map<String,AdvancedAnimationDefinition> CLIPS=definitions();
  LionfishIdleClip078(){root.addChild(head);root.addChild(left);root.addChild(right);updateDefaultPose();}
  Sample sample(String id,float elapsed) {
    resetToDefaultPose();var clip=CLIPS.get(id);
    if(clip!=null&&Float.isFinite(elapsed))AdvancedKeyframeAnimations.animate(this,clip,
        Math.round(Math.clamp(elapsed,0,ChaoIdleRules078.duration(id))*50),1,scratch);
    return new Sample(rotation(head),rotation(left),rotation(right));
  }
  private static Vector3f rotation(AdvancedModelBox node){return new Vector3f(node.rotateAngleX,node.rotateAngleY,node.rotateAngleZ);}
  @Override public AdvancedModelBox root(){return root;}
  @Override public Iterable<AdvancedModelBox> getAllParts(){return List.of(root,head,left,right);}
  @Override public void setupAnim(Entity entity,float swing,float amount,float age,float yaw,float pitch){}
}
