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
final class ChaoBellAnimation {
  static final float[] GRIP={.5f,.89f,.5f};
  static boolean active(ChaoEntity c){return InstrumentPerformance.active(c,BellPlayRules.KIND);}
  static boolean happy(ChaoEntity c,float partial){return active(c)&&BellPlayRules.happy(InstrumentPerformance.phase(c,partial));}
  static void applyArm(ChaoMeshAsset.Pose arm,LionfishBellClip clip,float tick,float weight){
    var v=clip.sample(tick);arm.rx=arm.rx*(1-weight)+v.x*weight;arm.ry=arm.ry*(1-weight)+v.y*weight;arm.rz=arm.rz*(1-weight)+v.z*weight;
    arm.x-=.35f*weight;arm.z-=.85f*weight;
  }
  static void pose(ChaoEntity c,ChaoModel model,LionfishBellClip clip,float partial){if(active(c)&&model.rattlePalm()!=null)
    applyArm(model.officialPoses().get("arm_right"),clip,InstrumentPerformance.phase(c,partial),InstrumentPerformance.presence(c,partial));}
  static void propTransform(PoseStack stack,float[] palm,float presence){
    stack.translate(palm[0]/16f,palm[1]/16f,palm[2]/16f);stack.mulPose(Axis.ZP.rotationDegrees(-15));stack.mulPose(Axis.XP.rotationDegrees(180));
    float scale=.30f*presence;stack.scale(scale,scale,scale);stack.translate(.5f-GRIP[0],.5f-GRIP[1],.5f-GRIP[2]);
  }
  static boolean render(ChaoEntity c,ChaoModel model,PoseStack stack,MultiBufferSource buffers,int light,float partial){
    if(!active(c)||c.isInvisible()||!model.officialActive()||model.rattlePalm()==null)return false;
    float weight=InstrumentPerformance.presence(c,partial);if(weight<=0)return true;
    stack.pushPose();try{model.transformAccessoryAnchor("arm_right",stack);propTransform(stack,model.rattlePalm(),weight);
      Minecraft.getInstance().getItemRenderer().renderStatic(c,new ItemStack(ChaoGarden.item(BellPlayRules.KIND)),ItemDisplayContext.NONE,false,stack,buffers,c.level(),light,OverlayTexture.NO_OVERLAY,c.getId());
    }finally{stack.popPose();}return true;
  }
}
