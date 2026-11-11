package com.chaogarden.client;

import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderGuiEvent;

/** Hide the gameplay HUD only while the room's own conversation overlay is visible. */
@EventBusSubscriber(modid="chao_garden",value=Dist.CLIENT)
public final class KindergartenSceneHud025 {
  private KindergartenSceneHud025(){}
  @SubscribeEvent public static void beforeHud(RenderGuiEvent.Pre event){if(Minecraft.getInstance().screen instanceof KindergartenScreen)event.setCanceled(true);}
}
