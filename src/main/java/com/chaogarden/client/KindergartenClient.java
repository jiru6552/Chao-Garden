package com.chaogarden.client;

import com.chaogarden.ChaoGarden;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

@EventBusSubscriber(modid=ChaoGarden.MODID,value=Dist.CLIENT,bus=EventBusSubscriber.Bus.MOD)
public final class KindergartenClient {
  private KindergartenClient(){}
  @SubscribeEvent public static void layers(EntityRenderersEvent.RegisterLayerDefinitions event){
    StaffChaoClient025.registerLayers(event);
  }
  @SubscribeEvent public static void renderers(EntityRenderersEvent.RegisterRenderers event){
    StaffChaoClient025.registerRenderer(event);
    event.registerEntityRenderer(com.chaogarden.kindergarten.KindergartenContent.PUPIL.get(),ChaoRenderer::new);
  }
}
