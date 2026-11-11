package com.chaogarden.client;
import java.nio.file.*;
import java.util.*;
import com.mojang.logging.LogUtils;
import net.minecraft.client.*;
import net.minecraft.client.gui.screens.*;
import net.minecraft.client.multiplayer.*;
import net.minecraft.client.multiplayer.resolver.ServerAddress;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.*;
import net.neoforged.neoforge.client.event.sound.PlaySoundEvent;
@EventBusSubscriber(modid="chao_garden",value=Dist.CLIENT)
public final class Playable02536DedicatedVisualSmoke {
 static boolean started,connected;static int ticks,frames,sounds;static Set<String> realms=new HashSet<>(),toys=new HashSet<>();
 static int pass(){return Integer.getInteger("chao.playable.client",0);}
 @SubscribeEvent public static void sound(PlaySoundEvent e){if(pass()>0&&e.getSound()!=null&&e.getSound().getLocation().getNamespace().equals("chao_garden"))sounds++;}
 @SubscribeEvent public static void tick(ClientTickEvent.Post e){if(pass()==0)return;var mc=Minecraft.getInstance();ticks++;
  if(mc.screen instanceof AccessibilityOnboardingScreen)mc.setScreen(new TitleScreen());
  if(!started&&mc.screen instanceof TitleScreen){started=true;mc.options.renderDistance().set(4);mc.options.simulationDistance().set(5);mc.options.framerateLimit().set(25);mc.options.pauseOnLostFocus=false;mc.options.tutorialStep=net.minecraft.client.tutorial.TutorialSteps.NONE;ConnectScreen.startConnecting(mc.screen,mc,ServerAddress.parseString("127.0.0.1:25586"),new ServerData("Playable smoke","127.0.0.1:25586",ServerData.Type.LAN),false,null);}
  if(mc.player!=null&&mc.level!=null){connected=true;realms.add(mc.level.dimension().location().toString());for(var entity:mc.level.entitiesForRendering())if(entity instanceof com.chaogarden.entity.ChaoEntity c&&c.toyPlayingTicks()>0)toys.add(c.toyPlayingKind());if(ticks%40==0){frames++;Screenshot.grab(mc.gameDirectory,"02536-dedicated-"+pass()+"-"+ticks+".png",mc.getMainRenderTarget(),x->{});}}
  if(connected&&mc.screen instanceof DisconnectedScreen){try{var dir=Path.of("/tmp/chao-playable/evidence/dedicated");Files.createDirectories(dir);Files.writeString(dir.resolve("client-"+pass()+".txt"),"actual client: realms="+realms+" toys="+toys+" renderedCaptures="+frames+" dispatchedModSounds="+sounds+"; null audio backend, not a listening pass\n");}catch(Exception ex){throw new RuntimeException(ex);}LogUtils.getLogger().info("PLAYABLE02536_CLIENT_DONE realms={} toys={} sounds={}",realms,toys,sounds);mc.stop();}
  if(ticks>15000)throw new IllegalStateException("dedicated client timeout");
 }
}
