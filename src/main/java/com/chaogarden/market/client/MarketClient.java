package com.chaogarden.market.client;

import com.chaogarden.ChaoGarden;
import com.chaogarden.market.MarketContent;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

@EventBusSubscriber(modid = ChaoGarden.MODID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class MarketClient {
  @SubscribeEvent
  public static void layers(EntityRenderersEvent.RegisterLayerDefinitions event) {
    event.registerLayerDefinition(MarketModel.LAYER, MarketModel::createLayer);
  }

  @SubscribeEvent
  public static void renderers(EntityRenderersEvent.RegisterRenderers event) {
    event.registerEntityRenderer(MarketContent.TRADER.get(), MarketRenderer::new);
  }
}
