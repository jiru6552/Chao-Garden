package com.chaogarden.client;

import com.chaogarden.entity.ChaoEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.world.entity.player.Player;
import com.chaogarden.ChaoGarden;
import java.util.HashMap;
import java.util.Map;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;

/** The real companion follows its keeper's torso in both player skin models. */
public final class CompanionBackLayer extends RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {
  // The player torso spans Y=0..12 and Z=-2..2 pixels. A Chao's chest
  // projects two pixels towards the keeper, placing it against the back skin
  // at this offset instead of leaving the old ~three-pixel air gap.
  static final float FOOT_Y=.48f, BACK_Z=.27f;
  private static long renders;
  private static ClientLevel indexedLevel;
  private static final Map<Integer,ChaoEntity> bodies=new HashMap<>();
  CompanionBackLayer(RenderLayerParent<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> parent) { super(parent); }
  static long renders() { return renders; }

  /** Replaced once per client tick, so memory follows currently tracked bodies.
   * A position query would lose the local companion during fast flight because
   * entity interpolation and network latency can place its hitbox behind us. */
  private static void rebuildIndex() {
    var level=Minecraft.getInstance().level;indexedLevel=level;bodies.clear();
    if(level!=null)for(var entity:level.entitiesForRendering())
      if(entity instanceof ChaoEntity chao&&attached(chao))bodies.putIfAbsent(chao.companionKeeperId(),chao);
  }

  @EventBusSubscriber(modid=ChaoGarden.MODID,value=Dist.CLIENT)
  public static final class Tracking {
    @SubscribeEvent public static void tick(ClientTickEvent.Post event) { rebuildIndex(); }
    @SubscribeEvent public static void leave(ClientPlayerNetworkEvent.LoggingOut event) {
      bodies.clear();indexedLevel=null;
    }
  }

  static boolean attached(ChaoEntity chao) {
    return chao.isAlive() && chao.isCompanionAttached()
        && chao.level().getEntity(chao.companionKeeperId()) instanceof Player keeper
        && keeper.isAlive() && !keeper.isRemoved();
  }

  @Override public void render(PoseStack stack, MultiBufferSource buffers, int light,
      AbstractClientPlayer keeper, float swing, float amount, float partialTick, float age, float yaw, float pitch) {
    if (keeper.isSpectator() || keeper.isInvisible()) return;
    if(indexedLevel!=keeper.level())rebuildIndex();
    ChaoEntity chao=bodies.get(keeper.getId());
    if(chao==null||chao.level()!=keeper.level()||!attached(chao)||chao.isInvisible())return;
      var renderer=Minecraft.getInstance().getEntityRenderDispatcher().getRenderer(chao);
      if (!(renderer instanceof ChaoRenderer chaoRenderer)) return;
      stack.pushPose();
      try {
        float back=chao.companionBackBlend(partialTick);
        getParentModel().body.translateAndRotate(stack);
        // The head perch follows torso movement, so looking down cannot bury the Chao in the keeper's face.
        carryTransform(stack,back);
        chaoRenderer.renderOnBack(chao,partialTick,stack,buffers,light);renders++;
      } finally { stack.popPose(); }
  }

  /** Torso-local placement also follows vanilla swimming and flight rotations. */
  static void carryTransform(PoseStack stack,float back) {
    float arc=net.minecraft.util.Mth.sin(back*(float)Math.PI);
    stack.translate(0,net.minecraft.util.Mth.lerp(back,-.50f,FOOT_Y)-arc*.35f,
        net.minecraft.util.Mth.lerp(back,0,BACK_Z)+arc*.35f);
    stack.mulPose(Axis.XP.rotationDegrees(180));
  }
}
