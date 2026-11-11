package com.chaogarden.client;

import com.chaogarden.entity.ChaoEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;

/** Shares the actual animated body pose, including the held-item render path. */
final class ChaoShineLayer extends RenderLayer<ChaoEntity, ChaoModel> {
  ChaoShineLayer(RenderLayerParent<ChaoEntity, ChaoModel> parent) { super(parent); }

  @Override public void render(PoseStack stack, MultiBufferSource buffers, int light,
      ChaoEntity entity, float swing, float amount, float partialTick,
      float age, float yaw, float pitch) {
    if (entity.officialComparison() || entity.isInvisible() || entity.isCocooned()
        || !ChaoShine.visible(entity.state())) return;
    getParentModel().renderToBuffer(stack,
        buffers.getBuffer(ChaoShine.surface(OfficialChaoRig.texture(entity.state()))),
        light, OverlayTexture.NO_OVERLAY, -1);
  }
}
