package com.chaogarden.client;

import com.chaogarden.core.HeroCloudGeometry0253;
import com.chaogarden.hub.HubContent;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.DimensionSpecialEffects;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterDimensionSpecialEffectsEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;

/** Pale gold open sky and a pink cloud sea, isolated to the Hero dimension. */
@EventBusSubscriber(modid="chao_garden",bus=EventBusSubscriber.Bus.MOD,value=Dist.CLIENT)
public final class HeroGardenEffects0253 extends DimensionSpecialEffects {
  public HeroGardenEffects0253(){super(Float.NaN,false,SkyType.NORMAL,false,false);}
  @Override public Vec3 getBrightnessDependentFogColor(Vec3 color,float sun){return color.scale(.86+.14*sun);}
  @Override public boolean isFoggyAt(int x,int z){return false;}
  @SubscribeEvent public static void register(RegisterDimensionSpecialEffectsEvent event){
    event.register(ResourceLocation.fromNamespaceAndPath("chao_garden","hero_sky"),new HeroGardenEffects0253());
  }
  private static final class CloudMaterial extends RenderType {
    private CloudMaterial(String name,VertexFormat format,VertexFormat.Mode mode,int size,boolean crumble,boolean sort,Runnable setup,Runnable clear){super(name,format,mode,size,crumble,sort,setup,clear);}
    private static final RenderType TYPE=create("chao_hero_cloud_sea",DefaultVertexFormat.POSITION_COLOR,VertexFormat.Mode.QUADS,262144,false,false,
        CompositeState.builder().setShaderState(new ShaderStateShard(GameRenderer::getPositionColorShader))
            .setTransparencyState(NO_TRANSPARENCY).setCullState(NO_CULL).setDepthTestState(LEQUAL_DEPTH_TEST)
            .setWriteMaskState(COLOR_DEPTH_WRITE).createCompositeState(false));
  }
  @EventBusSubscriber(modid="chao_garden",value=Dist.CLIENT)
  public static final class CloudPass {
    @SubscribeEvent public static void render(RenderLevelStageEvent event){
      var mc=Minecraft.getInstance();
      if(event.getStage()!=RenderLevelStageEvent.Stage.AFTER_SKY||mc.level==null||!mc.level.dimension().equals(HubContent.HERO_GARDEN))return;
      var poses=event.getPoseStack();var camera=event.getCamera().getPosition();
      poses.pushPose();
      try{
        // NeoForge dispatches AFTER_SKY with a null PoseStack, so the event
        // supplies an identity stack. Use the actual camera view matrix before
        // world translation; otherwise a downward view leaves a screen-fixed
        // cloud horizon rather than the sea beneath the floating island.
        poses.mulPose(event.getModelViewMatrix());
        poses.translate(-camera.x,-camera.y,-camera.z);
        var buffers=mc.renderBuffers().bufferSource();var out=buffers.getBuffer(CloudMaterial.TYPE);var matrix=poses.last().pose();
        for(var v:HeroCloudGeometry0253.vertices())out.addVertex(matrix,v.x(),v.y(),v.z()).setColor(v.color());
        buffers.endBatch(CloudMaterial.TYPE);
      }finally{poses.popPose();}
    }
  }
}
