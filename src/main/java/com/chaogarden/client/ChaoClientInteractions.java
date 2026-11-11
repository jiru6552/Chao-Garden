package com.chaogarden.client;

import com.chaogarden.entity.ChaoEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.InteractionResult;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

@EventBusSubscriber(modid = "chao_garden", value = Dist.CLIENT)
public final class ChaoClientInteractions {
  @SubscribeEvent
  public static void inspect(PlayerInteractEvent.EntityInteract event) {
    if (!event.getLevel().isClientSide() || !(event.getTarget() instanceof ChaoEntity chao)) return;
    var id = BuiltInRegistries.ITEM.getKey(event.getItemStack().getItem());
    if (id.getNamespace().equals("chao_garden") && id.getPath().equals("chao_analyzer")) {
      Minecraft.getInstance().setScreen(new ChaoInspectionScreen(chao));
      event.setCancellationResult(InteractionResult.SUCCESS);
      event.setCanceled(true);
    }
  }
}
