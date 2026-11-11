package com.chaogarden.client;

import com.chaogarden.ChaoVariants;
import com.chaogarden.core.ChaoState;
import com.chaogarden.entity.ChaoEggEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.entity.*;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;

public final class ChaoEggRenderer extends EntityRenderer<ChaoEggEntity> {
  private final ChaoEggModel model;
  private static final ResourceLocation[] CRACKS=java.util.stream.IntStream.range(0,10)
      .mapToObj(i->ResourceLocation.withDefaultNamespace("textures/block/destroy_stage_"+i+".png"))
      .toArray(ResourceLocation[]::new);
  private static final String[] COLORS = {
    "normal", "white", "red", "yellow", "blue", "aqua", "pink", "orange",
    "purple", "brown", "green", "grey", "lime", "black", "sky_blue", "cyan"
  };
  private static final String[] JEWELS = {
    "none", "silver", "gold", "ruby", "sapphire", "emerald", "amethyst",
    "aquamarine", "garnet", "onyx", "diamond", "peridot", "topaz", "moon"
  };

  public ChaoEggRenderer(EntityRendererProvider.Context c) {
    super(c);
    model = new ChaoEggModel(c.bakeLayer(ChaoEggModel.LAYER));
    shadowRadius = .2f;
  }

  @Override
  public ResourceLocation getTextureLocation(ChaoEggEntity e) {
    return ResourceLocation.fromNamespaceAndPath(
        "chao_garden", "textures/entity/eggs/" + textureKey(e.state()) + ".png");
  }

  /** Egg coats use independent shell atlases, rather than a creature rig's UV layout. */
  public static String textureKey(ChaoState state) {
    String variant = ChaoVariants.activeId(state);
    if (!ChaoVariants.NORMAL.equals(variant)) return "variant_" + variant;
    String name;
    if (state.jewel() > 0) name = "jewel_" + JEWELS[Math.min(state.jewel(), JEWELS.length - 1)];
    else if (state.baseColor() == 0) {
      String alignment = state.align >= .5f ? "hero" : state.align <= -.5f ? "dark" : "neutral";
      name = "normal_" + alignment + "_" + (state.adult ? Math.min(4, state.primaryType) : 0);
    } else name = COLORS[Math.max(0, Math.min(COLORS.length - 1, state.baseColor()))];
    return name + (state.isTwoTone() ? "_two" : "_mono") + (state.isShiny() ? "_shiny" : "");
  }

  @Override
  public void render(
      ChaoEggEntity e,
      float yaw,
      float partial,
      PoseStack p,
      MultiBufferSource buffers,
      int light) {
    p.pushPose();
    p.scale(.85f, -.85f, -.85f);
    p.mulPose(Axis.YP.rotationDegrees(yaw));
    model.setupAnim(e, 0, 0, e.tickCount + partial, 0, 0);
    // Vanilla's portal shader projects its animated layers from each vertex's
    // position. It covers the closed shell continuously, without a painted rim
    // or a longitude UV seam. Its position-only buffer ignores unused entity
    // color, UV, normal and lighting attributes emitted by the shared mesh.
    RenderType surface = "end".equals(ChaoVariants.activeId(e.state()))
        ? RenderType.endPortal()
        : RenderType.entityCutoutNoCull(getTextureLocation(e));
    model.renderToBuffer(
        p,
        buffers.getBuffer(surface),
        light,
        OverlayTexture.NO_OVERLAY,
        0xffffffff);
    // The portal egg retains its own animated material. Other shiny or jewel
    // shells receive the same continuously moving reflection as shiny Chao.
    if (!"end".equals(ChaoVariants.activeId(e.state())) && ChaoShine.visible(e.state()))
      model.renderToBuffer(p, buffers.getBuffer(ChaoShine.surface(getTextureLocation(e))), light,
          OverlayTexture.NO_OVERLAY, -1);
    float crack=e.crackProgress(partial);
    if(crack>0) {
      p.pushPose();p.scale(1.002f,1.002f,1.002f);
      int stage=Math.min(9,(int)(crack*10));
      int alpha=(int)(Math.min(1,crack*5)*210);
      model.renderToBuffer(p,buffers.getBuffer(RenderType.entityTranslucent(CRACKS[stage])),light,
          OverlayTexture.NO_OVERLAY,(alpha<<24)|0x404040);
      p.popPose();
    }
    p.popPose();
    super.render(e, yaw, partial, p, buffers, light);
  }
}
