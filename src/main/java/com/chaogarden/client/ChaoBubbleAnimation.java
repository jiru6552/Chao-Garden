package com.chaogarden.client;

import com.chaogarden.ChaoGarden;
import com.chaogarden.entity.ChaoEntity;
import com.chaogarden.toys.*;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.item.*;
import org.joml.*;

final class ChaoBubbleAnimation {
  private ChaoBubbleAnimation(){}
  static boolean active(ChaoEntity c){return BubblePlayRules.KIND.equals(c.toyPlayingKind())&&ChaoRattleAnimation.canPresent(c)&&RattlePlayRules.handheld(c.state());}
  static boolean happy(ChaoEntity c,float partial){
    return active(c)&&!c.isInWater()&&c.getAttackAnim(partial)<=0
        &&BubblePlayRules.presence(160-c.toyPlayingTicks()+partial)>.25f;
  }
  static float blowWeight(float elapsed){
    if(elapsed<12||elapsed>=152)return 0;
    float cycle=(elapsed-26)%48;if(cycle<0)cycle+=48;
    return cycle<20?1:cycle<30?1-(cycle-20)/10:cycle>=34?(cycle-34)/14:0;
  }
  static void pose(ChaoEntity c,ChaoModel model,LionfishBubbleClip clip,float partial){
    if(!active(c)||model.rattlePalm()==null)return;float elapsed=160-c.toyPlayingTicks()+partial;
    var arm=model.officialPoses().get("arm_right");applyArm(arm,clip,elapsed);
    float w=blowWeight(elapsed);float[] mouth=model.toyMouth();if(mouth==null)return;
    var skull=new PoseStack();model.transformAccessoryAnchor("skull",skull);
    var target=skull.last().pose().transformPosition(new Vector3f(mouth[0]-4.8f*(1-w),mouth[1]+.8f*(1-w),mouth[2]-.85f).div(16));
    var wand=new PoseStack();model.transformAccessoryAnchor("arm_right",wand);alignedPropTransform(wand,model,1);
    // ItemRenderer centers OBJ coordinates at (.5,.5,.5); ring center is (.5,.83,.5).
    var ring=wand.last().pose().transformPosition(new Vector3f(0,.33f,0));
    var body=new PoseStack();model.transformAccessoryAnchor("body",body);
    var delta=body.last().pose().invert(new Matrix4f()).transformDirection(target.sub(ring)).mul(16*BubblePlayRules.presence(elapsed));
    // Fit the hand and prop together; never float the wand away from its grip.
    arm.x+=delta.x;arm.y+=delta.y;arm.z+=delta.z;
  }
  static void applyArm(ChaoMeshAsset.Pose arm,LionfishBubbleClip clip,float elapsed){
    var value=clip.sample(elapsed);float w=BubblePlayRules.presence(elapsed);
    arm.rx=arm.rx*(1-w)+value.x;arm.ry=arm.ry*(1-w)+value.y;arm.rz=arm.rz*(1-w)+value.z;
  }
  static void propTransform(PoseStack stack,float[] palm,float presence){
    stack.translate(palm[0]/16f,palm[1]/16f,palm[2]/16f);
    stack.mulPose(Axis.ZP.rotationDegrees(-22));stack.mulPose(Axis.XP.rotationDegrees(180));
    float scale=.40f*presence;stack.scale(scale,scale,scale);stack.translate(0,.22f,0);
  }
  static void alignedPropTransform(PoseStack stack,ChaoModel model,float presence){
    float[] palm=model.rattlePalm();stack.translate(palm[0]/16f,palm[1]/16f,palm[2]/16f);
    var arm=new PoseStack();model.transformAccessoryAnchor("arm_right",arm);
    var skull=new PoseStack();model.transformAccessoryAnchor("skull",skull);
    var rotation=arm.last().pose().getNormalizedRotation(new Quaternionf()).invert()
        .mul(skull.last().pose().getNormalizedRotation(new Quaternionf()));
    stack.mulPose(rotation);stack.mulPose(Axis.XP.rotationDegrees(180));
    stack.mulPose(Axis.ZP.rotationDegrees(12));
    float scale=.40f*presence;stack.scale(scale,scale,scale);stack.translate(0,.22f,0);
  }
  static boolean render(ChaoEntity c,ChaoModel model,PoseStack stack,MultiBufferSource buffers,int light,float partial){
    if(!active(c)||c.isInvisible()||!model.officialActive()||model.rattlePalm()==null)return false;
    float presence=BubblePlayRules.presence(160-c.toyPlayingTicks()+partial);if(presence<=0)return true;
    stack.pushPose();try{
      model.transformAccessoryAnchor("arm_right",stack);alignedPropTransform(stack,model,presence);
      ChaoBubbleCues.ring(c,stack);
      Minecraft.getInstance().getItemRenderer().renderStatic(c,new ItemStack(ChaoGarden.item(BubblePlayRules.KIND)),ItemDisplayContext.NONE,
          false,stack,buffers,c.level(),light,OverlayTexture.NO_OVERLAY,c.getId());
    }finally{stack.popPose();}return true;
  }
}
