package com.chaogarden.client;

import com.chaogarden.toys.ChaoToyBlock;
import com.chaogarden.toys.FloatToyContent080;
import com.chaogarden.toys.FloatToyEntity080;
import com.chaogarden.toys.ToyContent;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

/** Existing blocky appearances, one actual vehicle position and ordinary world light. */
@EventBusSubscriber(modid="chao_garden",bus=EventBusSubscriber.Bus.MOD,value=Dist.CLIENT)
public final class FloatToyRenderer080 extends EntityRenderer<FloatToyEntity080> {
  public FloatToyRenderer080(EntityRendererProvider.Context context){super(context);shadowRadius=.35f;}
  @SubscribeEvent public static void register(EntityRenderersEvent.RegisterRenderers event){event.registerEntityRenderer(FloatToyContent080.FLOAT.get(),FloatToyRenderer080::new);}
  @Override public ResourceLocation getTextureLocation(FloatToyEntity080 e){return TextureAtlas.LOCATION_BLOCKS;}
  @Override public void render(FloatToyEntity080 e,float yaw,float partial,PoseStack stack,MultiBufferSource buffers,int light){
    if(!e.committed())return;
    stack.pushPose();
    try{
      // The existing duck points along local+X. Rotate its beak into the same
      // forward direction as its passenger; the symmetric ring needs no offset.
      stack.mulPose(Axis.YP.rotationDegrees(-yaw-(e.kind().equals("chao_rubber_duck")?90:0)));
      stack.translate(-.5,0,-.5);
      var idle=ToyContent.block(e.kind()).defaultBlockState().setValue(ChaoToyBlock.FACING,Direction.NORTH)
          .setValue(ChaoToyBlock.PLAYING,false).setValue(ChaoToyBlock.FRAME,0).setValue(ChaoToyBlock.WATERLOGGED,false);
      Minecraft.getInstance().getBlockRenderer().renderSingleBlock(idle,stack,buffers,light,OverlayTexture.NO_OVERLAY);
    }finally{stack.popPose();}
    super.render(e,yaw,partial,stack,buffers,light);
  }
}
