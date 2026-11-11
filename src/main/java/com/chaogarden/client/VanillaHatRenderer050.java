package com.chaogarden.client;

import com.chaogarden.market.ChaoHats;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.SkullModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.SkullBlockRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CarvedPumpkinBlock;
import net.minecraft.world.level.block.SkullBlock;

/** Vanilla pumpkin/skull surfaces fitted around the actual animated source head. */
final class VanillaHatRenderer050 {
  private SkullModel skull;
  static boolean supports(String kind){return kind.equals(ChaoHats.PUMPKIN)||kind.equals(ChaoHats.SKULL);}
  static float[] fittedBounds(float[] head) {
    float[] h=head==null?new float[]{-3.5f,-6,-3,3.5f,0,3}:head;
    return new float[]{h[0]-.3f,h[1]-.3f,h[2]-.3f,h[3]+.3f,h[4]+.3f,h[5]+.3f};
  }
  void render(ChaoModel model,String kind,PoseStack stack,MultiBufferSource buffers,int light) {
    float[] b=fittedBounds(model.accessoryBounds("skull"));
    float width=b[3]-b[0],height=b[4]-b[1],depth=b[5]-b[2];
    stack.pushPose();
    try {
      model.transformAccessoryAnchor("skull",stack);
      if(kind.equals(ChaoHats.SKULL)) {
        // Vanilla SkullModel is an 8px cube at [-4,-8,-4 .. 4,0,4],
        // the same down-Y, forward-minus-Z joint convention as the Chao meshes.
        if(skull==null)skull=new SkullModel(Minecraft.getInstance().getEntityModels().bakeLayer(ModelLayers.SKELETON_SKULL));
        stack.translate((b[0]+b[3])/32,b[4]/16,(b[2]+b[5])/32);
        stack.scale(width/8,height/8,depth/8);skull.setupAnim(0,0,0);
        skull.renderToBuffer(stack,buffers.getBuffer(SkullBlockRenderer.getRenderType(SkullBlock.Types.SKELETON,null)),light,OverlayTexture.NO_OVERLAY);
      } else {
        // Convert block-up to model-down with a proper rotation, retaining
        // face winding. SOUTH becomes the Chao's forward (-Z) carved face.
        stack.translate(b[0]/16,b[4]/16,b[5]/16);
        stack.scale(width/16,height/16,depth/16);stack.mulPose(Axis.XP.rotationDegrees(180));
        Minecraft.getInstance().getBlockRenderer().renderSingleBlock(
            Blocks.CARVED_PUMPKIN.defaultBlockState().setValue(CarvedPumpkinBlock.FACING,Direction.SOUTH),
            stack,buffers,light,OverlayTexture.NO_OVERLAY);
      }
    } finally {stack.popPose();}
  }
}
