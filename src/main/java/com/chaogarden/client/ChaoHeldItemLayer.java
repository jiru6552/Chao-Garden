package com.chaogarden.client;

import com.chaogarden.entity.ChaoEntity;
import com.chaogarden.ChaoGarden;
import net.minecraft.world.item.ItemStack;
import com.chaogarden.item.CareItem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.world.item.ItemDisplayContext;

/** The actual saved item stack stays visible while a Chao considers or uses it. */
public final class ChaoHeldItemLayer extends RenderLayer<ChaoEntity,ChaoModel> {
  public ChaoHeldItemLayer(RenderLayerParent<ChaoEntity,ChaoModel> parent) { super(parent); }
  @Override public void render(PoseStack stack,MultiBufferSource buffers,int light,ChaoEntity chao,
      float swing,float amount,float partialTick,float age,float yaw,float pitch) {
    if(ChaoRattleAnimation.render(chao,getParentModel(),stack,buffers,light,partialTick))return;
    if(ChaoBubbleAnimation.render(chao,getParentModel(),stack,buffers,light,partialTick))return;
    if(ChaoMaracasAnimation.render(chao,getParentModel(),stack,buffers,light,partialTick))return;
    if(ChaoTambourineAnimation.render(chao,getParentModel(),stack,buffers,light,partialTick))return;
    if(ChaoTriangleAnimation.render(chao,getParentModel(),stack,buffers,light,partialTick))return;
    if(ChaoTrumpetAnimation.render(chao,getParentModel(),stack,buffers,light,partialTick))return;
    if(ChaoFluteAnimation.render(chao,getParentModel(),stack,buffers,light,partialTick))return;
    if(ChaoDrumAnimation.render(chao,getParentModel(),stack,buffers,light,partialTick))return;
    if(ChaoCymbalsAnimation.render(chao,getParentModel(),stack,buffers,light,partialTick))return;
    if(ChaoCastanetsAnimation.render(chao,getParentModel(),stack,buffers,light,partialTick))return;
    if(ChaoBellAnimation.render(chao,getParentModel(),stack,buffers,light,partialTick))return;
    // These instruments own their presentation even when a hurt/pet packet
    // suppresses it. Do not fall back to the old generic floating prop.
    if(chao.getHeldChaoItem().isEmpty()&&chao.getBehavior()==ChaoEntity.Behavior.INSTRUMENT&&com.chaogarden.toys.InstrumentPerformance.supports(chao.behaviorDetail()))return;
    var item=chao.getHeldChaoItem();
    // Learned music displays a client-only prop. No stack enters inventory or
    // the world, and an actual held item always takes precedence.
    if(item.isEmpty() && chao.getBehavior()==ChaoEntity.Behavior.INSTRUMENT) {
      var instrument=ChaoGarden.item("chao_"+chao.behaviorDetail());
      if(instrument!=null)item=new ItemStack(instrument);
    }
    if(item.isEmpty()&&chao.getBehavior()==ChaoEntity.Behavior.WATER)
      item=new ItemStack(ChaoGarden.item("chao_watering_can"));
    var model=getParentModel();
    if (item.isEmpty() || chao.isInvisible() || chao.isCocooned() || chao.officialComparison()
        || !model.officialActive()) return;
    stack.pushPose();
    model.transformHeldItem(stack);
    // GROUND silently adds a display translation and scale. Use one explicit
    // hand-sized frame, positioned in front of the face instead of inside it.
    stack.translate(0,-1.2f/16f,-.9f/16f);
    // Source mesh coordinates point downward. Item renderer uses an upright
    // local item frame; the attachment already follows the current body pose.
    stack.mulPose(Axis.XP.rotationDegrees(180));
    if(item.getItem() instanceof com.chaogarden.fruit.WateringCanItem) {
      // The mesh spout runs along +X. Face it ahead of the Chao (+Z in
      // this upright item frame), then tip down through the synced pour.
      if(chao.getBehavior()==ChaoEntity.Behavior.WATER) {
        float pour=com.chaogarden.core.ChaoAnimationRules.wateringPour(chao.getBehaviorProgress(partialTick));
        stack.mulPose(Axis.XP.rotationDegrees(48*pour));
      }
      stack.mulPose(Axis.YP.rotationDegrees(-90));
    }
    // Native fruits occupy less than a full item frame.
    // Keep their bite-sized shape readable between the Chao's hands.
    float scale=item.getItem() instanceof CareItem care && care.kind==CareItem.Kind.FRUIT
        ? .30f : item.getItem() instanceof com.chaogarden.fruit.FruitSeedItem ? .26f
        : item.is(ChaoGarden.item("chao_shovel")) ? .30f
        : item.getItem() instanceof net.minecraft.world.item.BlockItem ? .105f
        : item.getItem() instanceof com.chaogarden.fruit.WateringCanItem ? .30f : .21f;
    stack.scale(scale,scale,scale);
    Minecraft.getInstance().getItemRenderer().renderStatic(chao,item,ItemDisplayContext.NONE,
        false,stack,buffers,chao.level(),light,net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY,
        chao.getId());
    stack.popPose();
  }
}
