package com.chaogarden.client;

import com.chaogarden.entity.ChaoEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;

/** Equipped garden hats follow head tracking, crawling, swimming and held poses. */
public final class ChaoHatLayer extends RenderLayer<ChaoEntity, ChaoModel> {
  private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(
      "chao_garden", "textures/entity/chao/market_accessories.png");
  private final ChaoHeadwear headwear = new ChaoHeadwear();
  private final EggshellMask shell=new EggshellMask();

  public ChaoHatLayer(RenderLayerParent<ChaoEntity, ChaoModel> parent) { super(parent); }

  @Override
  public void render(PoseStack stack, MultiBufferSource buffers, int light,
                     ChaoEntity chao, float swing, float amount, float partialTick,
                     float age, float yaw, float pitch) {
    String hat = chao.getChaoHat();
    ChaoModel model = getParentModel();
    if (chao.officialComparison() || hat.isBlank() || !model.officialActive()
        || chao.isCocooned() || chao.isInvisible()) return;
    if(hat.equals("eggshell_hat")){shell.render(model,chao,chao.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.HEAD),stack,buffers,light);return;}
    headwear.renderHat(model, hat, stack,
        buffers.getBuffer(RenderType.entityCutoutNoCull(TEXTURE)), light,
        OverlayTexture.NO_OVERLAY);
  }
}
