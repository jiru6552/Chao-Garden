package com.chaogarden.client;

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

/** An isolated purple storm-cloud sphere; garden care retains ordinary daylight simulation. */
@EventBusSubscriber(modid="chao_garden",bus=EventBusSubscriber.Bus.MOD,value=Dist.CLIENT)
public final class DarkGardenEffects0255 extends DimensionSpecialEffects {
  private static final ResourceLocation SKY=ResourceLocation.fromNamespaceAndPath("chao_garden","textures/scene/dark_twilight_sky.png");
  public DarkGardenEffects0255(){super(Float.NaN,false,SkyType.NONE,false,false);}
  @Override public Vec3 getBrightnessDependentFogColor(Vec3 color,float sun){return new Vec3(.21,.14,.32);}
  @Override public boolean isFoggyAt(int x,int z){return false;}
  @SubscribeEvent public static void register(RegisterDimensionSpecialEffectsEvent event){
    event.register(ResourceLocation.fromNamespaceAndPath("chao_garden","dark_sky"),new DarkGardenEffects0255());
  }
  private static final class SkyMaterial extends RenderType {
    private SkyMaterial(String name,VertexFormat format,VertexFormat.Mode mode,int size,boolean crumble,boolean sort,Runnable setup,Runnable clear){super(name,format,mode,size,crumble,sort,setup,clear);}
    private static final RenderType TYPE=create("chao_dark_cloud_sphere",DefaultVertexFormat.POSITION_TEX_COLOR,VertexFormat.Mode.QUADS,262144,false,false,
        CompositeState.builder().setShaderState(new ShaderStateShard(GameRenderer::getPositionTexColorShader))
            .setTextureState(new TextureStateShard(SKY,false,false))
            .setTransparencyState(NO_TRANSPARENCY).setCullState(NO_CULL)
            .setDepthTestState(LEQUAL_DEPTH_TEST).setWriteMaskState(COLOR_DEPTH_WRITE).createCompositeState(false));
  }
  private static float[] point(double latitude,double longitude){
    double r=180,ring=r*Math.sin(latitude);
    return new float[]{(float)(ring*Math.cos(longitude)),(float)(80+r*Math.cos(latitude)),(float)(ring*Math.sin(longitude))};
  }
  private record SkyVertex(float x,float y,float z,float u,float v){}
  private static final java.util.List<SkyVertex> DOME=dome();
  private static java.util.List<SkyVertex> dome(){
    var result=new java.util.ArrayList<SkyVertex>(5120);
    for(int row=0;row<20;row++)for(int col=0;col<64;col++){
      float u0=col/64f,u1=(col+1)/64f,v0=row/20f,v1=(row+1)/20f;
      float[][] points={point(v0*Math.PI,u0*Math.PI*2),point(v0*Math.PI,u1*Math.PI*2),point(v1*Math.PI,u1*Math.PI*2),point(v1*Math.PI,u0*Math.PI*2)};
      float[][] uv={{u0,v0},{u1,v0},{u1,v1},{u0,v1}};
      for(int i=0;i<4;i++)result.add(new SkyVertex(points[i][0],points[i][1],points[i][2],uv[i][0],uv[i][1]));
    }
    return java.util.List.copyOf(result);
  }
  @EventBusSubscriber(modid="chao_garden",value=Dist.CLIENT)
  public static final class SkyPass {
    @SubscribeEvent public static void render(RenderLevelStageEvent event){
      var mc=Minecraft.getInstance();
      if(event.getStage()!=RenderLevelStageEvent.Stage.AFTER_SKY||mc.level==null||!mc.level.dimension().equals(HubContent.DARK_GARDEN))return;
      var poses=event.getPoseStack();var camera=event.getCamera().getPosition();poses.pushPose();
      try{
        poses.mulPose(event.getModelViewMatrix());poses.translate(-camera.x,-camera.y,-camera.z);
        var buffers=mc.renderBuffers().bufferSource();var out=buffers.getBuffer(SkyMaterial.TYPE);var matrix=poses.last().pose();
        for(var v:DOME)out.addVertex(matrix,v.x,v.y,v.z).setUv(v.u,v.v).setColor(0xffffffff);
        buffers.endBatch(SkyMaterial.TYPE);
      }finally{poses.popPose();}
    }
  }
}
