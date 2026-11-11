package com.chaogarden.client;

import com.chaogarden.core.ChaoState;
import com.chaogarden.ChaoVariants;
import com.chaogarden.entity.ChaoEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.culling.Frustum;

public final class ChaoRenderer extends MobRenderer<ChaoEntity, ChaoModel> {
  static boolean suppressNames;
  private boolean backPass;

  @Override
  protected boolean shouldShowName(ChaoEntity entity) {
    return !suppressNames && !backPass && super.shouldShowName(entity);
  }

  @Override public boolean shouldRender(ChaoEntity entity, Frustum frustum, double x, double y, double z) {
    if(CompanionBackLayer.attached(entity))return false;
    if(super.shouldRender(entity,frustum,x,y,z))return true;
    // The reserved horse can remain in view during its rider's side approach.
    // Extend only presentation culling; physical entity dimensions stay intact.
    var target=entity.toyTarget();String kind=entity.toyPlayingKind();
    return target!=null&&(kind.equals("chao_rocking_horse")||kind.equals("chao_pogo_stick"))
        &&entity.shouldRender(x,y,z)&&frustum.isVisible(new net.minecraft.world.phys.AABB(target).inflate(.5,1.5,.5));
  }

  @Override public void render(ChaoEntity entity, float yaw, float partialTick, PoseStack pose,
      MultiBufferSource buffers, int light) {
    if (!backPass && CompanionBackLayer.attached(entity)) return;
    super.render(entity, yaw, partialTick, pose, buffers, light);
  }

  /** Reuse all ordinary coat, shine, glass and wardrobe layers in torso space. */
  void renderOnBack(ChaoEntity entity, float partialTick, PoseStack pose,
      MultiBufferSource buffers, int light) {
    float body=entity.yBodyRot, oldBody=entity.yBodyRotO, head=entity.yHeadRot, oldHead=entity.yHeadRotO;
    float yaw=entity.getYRot(), oldYaw=entity.yRotO, pitch=entity.getXRot(), oldPitch=entity.xRotO;
    boolean previous=backPass;backPass=true;
    try {
      entity.yBodyRot=entity.yBodyRotO=entity.yHeadRot=entity.yHeadRotO=0;
      entity.setYRot(0);entity.yRotO=0;entity.setXRot(0);entity.xRotO=0;
      render(entity,0,partialTick,pose,buffers,light);
    } finally {
      entity.yBodyRot=body;entity.yBodyRotO=oldBody;entity.yHeadRot=head;entity.yHeadRotO=oldHead;
      entity.setYRot(yaw);entity.yRotO=oldYaw;entity.setXRot(pitch);entity.xRotO=oldPitch;backPass=previous;
    }
  }

  private static final String[] COLORS = {
    "normal",
    "white",
    "red",
    "yellow",
    "blue",
    "aqua",
    "pink",
    "orange",
    "purple",
    "brown",
    "green",
    "grey",
    "lime",
    "black",
    "sky_blue",
    "cyan"
  };
  private static final String[] JEWELS = {
    "none",
    "silver",
    "gold",
    "ruby",
    "sapphire",
    "emerald",
    "amethyst",
    "aquamarine",
    "garnet",
    "onyx",
    "diamond",
    "peridot",
    "topaz",
    "moon"
  };

  public ChaoRenderer(EntityRendererProvider.Context ctx) {
    super(ctx, new ChaoModel(ctx.bakeLayer(ChaoModel.LAYER)), .25f);
    addLayer(new ChaoShineLayer(this));
    addLayer(new ChaoOverlayLayer(this));
    addLayer(new ChaoChaosGlassLayer(this));
    addLayer(new ChaoExpressionLayer025(this));
    addLayer(new ChaoHatLayer(this));
    addLayer(new CweAccessoryLayer(this));
    addLayer(new ChaoPartyLayer(this));
    addLayer(new ChaoHeldItemLayer(this));
  }

  public static String textureKey(ChaoState s) {
    // Evolution changes the anatomy and family markings, while inherited coat
    // genes remain visible. Preserve the old canonical resource for ordinary
    // two-tone, non-shiny Chaos Chao; other heritage has its own fitted coat.
    if (s.immortal && s.character.equals("normal")) {
      String family = s.align >= .5f ? "chaos_angel" : s.align <= -.5f ? "chaos_devil" : "chaos_light";
      if (s.baseColor() == 0 && s.jewel() == 0 && !s.isShiny() && s.isTwoTone()) return family;
      return family + "_" + geneticCoatKey(s);
    }
    String variant = ChaoVariants.activeId(s);
    if (!ChaoVariants.NORMAL.equals(variant)) return "variant_" + variant;
    String name;
    if (s.jewel() > 0) name = "jewel_" + JEWELS[Math.min(s.jewel(), JEWELS.length - 1)];
    else if (s.baseColor() == 0) {
      // Children visibly lean toward their cared-for alignment before first evolution.
      String a = s.align >= .5f ? "hero" : s.align <= -.5f ? "dark" : "neutral";
      name = "normal_" + a + "_" + (s.adult ? Math.min(4, s.primaryType) : 0);
    } else name = COLORS[Math.max(0, Math.min(COLORS.length - 1, s.baseColor()))];
    return name + (s.isTwoTone() ? "_two" : "_mono") + (s.isShiny() ? "_shiny" : "");
  }

  /** Coat heritage without alignment/type anatomy; shared by Chaos families. */
  public static String geneticCoatKey(ChaoState s) {
    String coat = s.jewel() > 0
        ? "jewel_" + JEWELS[Math.min(s.jewel(), JEWELS.length - 1)]
        : COLORS[Math.max(0, Math.min(COLORS.length - 1, s.baseColor()))];
    return coat + (s.isTwoTone() ? "_two" : "_mono") + (s.isShiny() ? "_shiny" : "");
  }

  @Override
  public ResourceLocation getTextureLocation(ChaoEntity e) {
    if(e.isCocooned())return ResourceLocation.fromNamespaceAndPath("chao_garden",
        "textures/entity/chao/cocoon_"+(e.isDeathCocoon()?"death":e.isRebirthCocoon()?"rebirth":"evolution")+".png");
    if (e.officialComparison())
      return OfficialChaoRig.referenceTexture(e.state(), e.sourceReferenceKey());
    return OfficialChaoRig.texture(e.state());
  }

  @Override
  protected net.minecraft.client.renderer.RenderType getRenderType(ChaoEntity entity,boolean visible,boolean translucent,boolean glowing) {
    if(!entity.isCocooned()&&!entity.officialComparison()&&visible&&entity.state().monsterAlpha()<1)
      return net.minecraft.client.renderer.RenderType.entityTranslucent(getTextureLocation(entity));
    return super.getRenderType(entity,visible,translucent,glowing);
  }

  @Override
  protected void scale(ChaoEntity entity, PoseStack pose, float partialTick) {
    float size = entity.isCocooned()||entity.state().adult ? .80f : .64f;
    if (!entity.isCocooned()&&entity.state().immortal) size = .86f;
    pose.scale(size, size, size);
  }
}
