package com.chaogarden.client;

import com.chaogarden.ChaoGarden;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.settings.KeyConflictContext;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import org.lwjgl.glfw.GLFW;
import com.chaogarden.companion.CompanionFlight;
import com.chaogarden.companion.CompanionNetwork;

@EventBusSubscriber(modid = ChaoGarden.MODID, value = Dist.CLIENT)
public final class CompanionClientEvents {
  public static final KeyMapping BOOST=new KeyMapping("key.chao_garden.companion_boost",KeyConflictContext.IN_GAME,
      com.mojang.blaze3d.platform.InputConstants.Type.KEYSYM,GLFW.GLFW_KEY_R,"key.categories.chao_garden");
  public static final KeyMapping ENDER_CHEST=new KeyMapping("key.chao_garden.companion_ender_chest",KeyConflictContext.IN_GAME,
      com.mojang.blaze3d.platform.InputConstants.Type.KEYSYM,GLFW.GLFW_KEY_V,"key.categories.chao_garden");
  @SubscribeEvent public static void tick(ClientTickEvent.Post event){
    var mc=Minecraft.getInstance();CompanionFlight.tickClient(mc.player);
    while(BOOST.consumeClick())if(mc.player!=null&&mc.screen==null&&mc.player.isFallFlying()&&CompanionFlight.canRequestBoost())
      CompanionNetwork.requestBoost();
    while(ENDER_CHEST.consumeClick())if(mc.player!=null&&mc.screen==null)CompanionNetwork.requestEnderChest();
  }
  @SubscribeEvent
  public static void leave(ClientPlayerNetworkEvent.LoggingOut event) {
    HeldChaoRenderer.clearCache();ChaoShine.clearSurfaceCache();CompanionFlight.clear();
  }
  @EventBusSubscriber(modid=ChaoGarden.MODID,value=Dist.CLIENT,bus=EventBusSubscriber.Bus.MOD)
  public static final class Keys {
    @SubscribeEvent public static void register(RegisterKeyMappingsEvent event){event.register(BOOST);event.register(ENDER_CHEST);}
  }
}
