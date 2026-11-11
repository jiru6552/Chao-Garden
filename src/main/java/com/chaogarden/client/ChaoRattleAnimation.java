package com.chaogarden.client;

import com.chaogarden.ChaoGarden;
import com.chaogarden.entity.ChaoEntity;
import com.chaogarden.toys.RattlePlayRules;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

/** Presentation of the real placed-rattle session; never creates an inventory/world item. */
final class ChaoRattleAnimation {
  static final float OUTWARD_PIXELS=.45f, FORWARD_PIXELS=.45f, WRIST_DEGREES=-55f;
  private ChaoRattleAnimation() {}
  private static boolean playing(ChaoEntity chao) {
    return RattlePlayRules.KIND.equals(chao.toyPlayingKind())&&canPresent(chao);
  }
  static boolean canPresent(ChaoEntity chao) {
    return chao.hurtTime==0&&!chao.isCocooned()&&!chao.officialComparison()&&!chao.isCompanionGripping()
        &&!chao.isResting()&&!chao.isEating()&&!chao.isBeingPetted()&&!chao.isHandlingItem()
        &&!chao.isPassenger()&&chao.getHeldChaoItem().isEmpty()&&chao.getMainHandItem().isEmpty()
        &&chao.getBehavior()==ChaoEntity.Behavior.NONE;
  }
  static boolean active(ChaoEntity chao){return playing(chao)&&RattlePlayRules.handheld(chao.state());}
  static float elapsed(ChaoEntity chao,float partial){return RattlePlayRules.DURATION-chao.toyPlayingTicks()+partial;}
  static boolean happy(ChaoEntity chao,float partial){return playing(chao)&&RattlePlayRules.happy(elapsed(chao,partial));}
  static void applyArm(ChaoMeshAsset.Pose arm,LionfishRattleClip clip,float elapsed) {
    var value=clip.sample(elapsed);float weight=RattlePlayRules.presence(elapsed);
    arm.rx=arm.rx*(1-weight)+value.x;arm.ry=arm.ry*(1-weight)+value.y;arm.rz=arm.rz*(1-weight)+value.z;
    // Move the hand and its prop together, slightly away from the body; no floating grip.
    arm.x-=OUTWARD_PIXELS*weight;
    arm.z-=FORWARD_PIXELS*weight;
  }
  static void pose(ChaoEntity chao,ChaoModel model,LionfishRattleClip clip,float partial) {
    if(!active(chao)||model.rattlePalm()==null)return;
    applyArm(model.officialPoses().get("arm_right"),clip,elapsed(chao,partial));
  }
  static void propTransform(PoseStack stack,float[] palm,float presence) {
    stack.translate(palm[0]/16f,palm[1]/16f,palm[2]/16f);
    // Ten degrees more upright than the prototype, still pointing clear of the face.
    stack.mulPose(Axis.ZP.rotationDegrees(WRIST_DEGREES));
    stack.mulPose(Axis.XP.rotationDegrees(180));
    float scale=.30f*presence;stack.scale(scale,scale,scale);
    // OBJ handle grip=(.5,.30,.5); ItemRenderer applies (-.5,-.5,-.5).
    stack.translate(0,.20f,0);
  }
  static boolean render(ChaoEntity chao,ChaoModel model,PoseStack stack,MultiBufferSource buffers,int light,float partial) {
    if(!active(chao)||chao.isInvisible()||!model.officialActive()||model.rattlePalm()==null)return false;
    float presence=RattlePlayRules.presence(elapsed(chao,partial));if(presence<=0)return true;
    var item=ChaoGarden.item(RattlePlayRules.KIND);if(item==null)return false;
    stack.pushPose();
    try {
      model.transformAccessoryAnchor("arm_right",stack);propTransform(stack,model.rattlePalm(),presence);
      Minecraft.getInstance().getItemRenderer().renderStatic(chao,new ItemStack(item),ItemDisplayContext.NONE,
          false,stack,buffers,chao.level(),light,OverlayTexture.NO_OVERLAY,chao.getId());
    } finally {stack.popPose();}
    return true;
  }
}
