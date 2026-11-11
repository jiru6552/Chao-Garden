package com.chaogarden.client;
import com.chaogarden.fruit.*;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.blockentity.*;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
@EventBusSubscriber(modid="chao_garden",bus=EventBusSubscriber.Bus.MOD,value=Dist.CLIENT)
public final class PlantedPalmRenderer02536 implements BlockEntityRenderer<FruitCrownEntity> {
  public PlantedPalmRenderer02536(BlockEntityRendererProvider.Context c){}
  @SubscribeEvent public static void register(EntityRenderersEvent.RegisterRenderers e){e.registerBlockEntityRenderer(FruitContent.CROWN_ENTITY.get(),PlantedPalmRenderer02536::new);}
  @Override public net.minecraft.world.phys.AABB getRenderBoundingBox(FruitCrownEntity e){return new net.minecraft.world.phys.AABB(e.getBlockPos()).inflate(6,2,6);}
  @Override public void render(FruitCrownEntity e,float p,PoseStack poses,MultiBufferSource b,int light,int overlay){
    var s=e.getBlockState();if(!s.getValue(ChaoTreeCrownBlock.GARDEN)||s.getValue(ChaoTreeCrownBlock.WITHERED)||s.getValue(ChaoTreeCrownBlock.BARE))return;
    poses.pushPose();try{LobbySceneRenderer025.renderPalm(poses,b,light,overlay);}finally{poses.popPose();}
  }
}
