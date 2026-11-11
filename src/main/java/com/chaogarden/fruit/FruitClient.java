package com.chaogarden.fruit;

import com.chaogarden.ChaoGarden;
import net.minecraft.client.renderer.BiomeColors;
import net.minecraft.world.level.FoliageColor;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;

/** Uses the same biome foliage tint as vanilla oak leaves. */
@EventBusSubscriber(modid = ChaoGarden.MODID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class FruitClient {
  private FruitClient() {}

  @SubscribeEvent
  public static void blocks(RegisterColorHandlersEvent.Block event) {
    event.register((state, level, pos, tint) -> level != null && pos != null
        ? BiomeColors.getAverageFoliageColor(level, pos) : FoliageColor.getDefaultColor(),
        FruitContent.LEAVES.get());
  }

  @SubscribeEvent
  public static void items(RegisterColorHandlersEvent.Item event) {
    event.register((stack, tint) -> FoliageColor.getDefaultColor(), FruitContent.LEAVES.get());
  }
}
