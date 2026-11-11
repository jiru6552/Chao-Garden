package com.chaogarden.client;

import com.chaogarden.ChaoGarden;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

@EventBusSubscriber(modid = "chao_garden", bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class ChaoClient {
  @SubscribeEvent
  public static void layers(EntityRenderersEvent.RegisterLayerDefinitions event) {
    event.registerLayerDefinition(ChaoModel.LAYER, ChaoModel::createBodyLayer);
    event.registerLayerDefinition(ChaoEggModel.LAYER, ChaoEggModel::createBodyLayer);
  }

  @SubscribeEvent
  public static void renderers(EntityRenderersEvent.RegisterRenderers event) {
    event.registerEntityRenderer(ChaoGarden.CHAO.get(), ChaoRenderer::new);
    event.registerEntityRenderer(ChaoGarden.CHAO_EGG.get(), ChaoEggRenderer::new);
  }

  @SubscribeEvent public static void playerLayers(EntityRenderersEvent.AddLayers event) {
    for (var skin : event.getSkins()) {
      if (event.getSkin(skin) instanceof net.minecraft.client.renderer.entity.player.PlayerRenderer renderer)
        renderer.addLayer(new CompanionBackLayer(renderer));
    }
  }
}
