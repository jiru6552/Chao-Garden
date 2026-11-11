package com.chaogarden.market.client;

import com.chaogarden.market.BlackMarketTrader;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;

public final class MarketRenderer extends MobRenderer<BlackMarketTrader, MarketModel> {
  private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(
      "chao_garden", "textures/entity/chao/official/black_market.png");
  private static final ResourceLocation ACCESSORIES = ResourceLocation.fromNamespaceAndPath(
      "chao_garden", "textures/entity/chao/market_accessories.png");

  public MarketRenderer(EntityRendererProvider.Context context) {
    super(context, new MarketModel(context.bakeLayer(MarketModel.LAYER)), .3f);
    addLayer(new RenderLayer<BlackMarketTrader, MarketModel>(this) {
      @Override public void render(PoseStack pose, MultiBufferSource buffers, int light,
                                   BlackMarketTrader trader, float swing, float amount,
                                   float partialTick, float age, float yaw, float pitch) {
        if (!trader.isInvisible()) getParentModel().renderAccessories(pose,
            buffers.getBuffer(RenderType.entityCutoutNoCull(ACCESSORIES)), light,
            OverlayTexture.NO_OVERLAY);
      }
    });
  }

  @Override protected boolean shouldShowName(BlackMarketTrader trader){
    return !(trader.resident()&&net.minecraft.client.Minecraft.getInstance().screen instanceof com.chaogarden.client.KindergartenScreen)&&super.shouldShowName(trader);
  }

  @Override public ResourceLocation getTextureLocation(BlackMarketTrader trader) { return TEXTURE; }
}
