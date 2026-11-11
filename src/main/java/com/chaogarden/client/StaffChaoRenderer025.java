package com.chaogarden.client;

import com.chaogarden.kindergarten.KindergartenNpc;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

/** Distinct adult school professions; normal Chao wardrobe layers do not duplicate their faces. */
public final class StaffChaoRenderer025 extends MobRenderer<KindergartenNpc,StaffChaoModel025> {
  public static final ResourceLocation TEXTURE=ResourceLocation.fromNamespaceAndPath("chao_garden","textures/entity/chao/staff025.png");
  public StaffChaoRenderer025(EntityRendererProvider.Context context){
    super(context,new StaffChaoModel025(context.bakeLayer(StaffChaoModel025.LAYER)),.25f);
  }
  @Override protected boolean shouldShowName(KindergartenNpc staff){
    return !(Minecraft.getInstance().screen instanceof KindergartenScreen)
        &&!ChaoRenderer.suppressNames&&super.shouldShowName(staff);
  }
  @Override public ResourceLocation getTextureLocation(KindergartenNpc staff){return TEXTURE;}
  @Override protected void scale(KindergartenNpc staff,PoseStack pose,float partial){pose.scale(.8f,.8f,.8f);}
}
