package com.chaogarden.client;

import com.chaogarden.ChaoGarden;
import com.chaogarden.core.ChaoState;
import com.chaogarden.entity.ChaoEntity;
import com.mojang.logging.LogUtils;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderFrameEvent;

/** Opt-in client regression using the real equipped slot and player back layer. */
@EventBusSubscriber(modid=ChaoGarden.MODID,value=Dist.CLIENT)
public final class EquippedFlightVisualSmoke {
  private static int stage=-1, ticks;
  private static String lastScreen="";
  private static volatile boolean ready, captured;
  private static long renders;
  private static volatile int bodyId=-1;
  private static final String[] NAMES={"grounded","creative-flight","landed"};
  @SubscribeEvent public static void tick(ClientTickEvent.Post event) {
    if(!Boolean.getBoolean("chao_garden.creativeGripStudy"))return;
    var mc=Minecraft.getInstance();
    if (mc.level==null && mc.screen!=null) {
      String screen=mc.screen.getClass().getSimpleName();
      if (!screen.equals(lastScreen)) {
        lastScreen=screen;
        LogUtils.getLogger().info("CHAO039_CLIENT_SCREEN {} {}",screen,mc.screen.getTitle().getString());
      }
      if (mc.screen instanceof net.minecraft.client.gui.screens.BackupConfirmScreen
          && new java.io.File(mc.gameDirectory,"saves/ChaoVisual/level.dat").isFile()) {
        var backup=net.minecraft.network.chat.Component.translatable("selectWorld.backupJoinConfirmButton").getString();
        for (var child:mc.screen.children()) if(child instanceof net.minecraft.client.gui.components.AbstractButton button
            && button.getMessage().getString().equals(backup)) {button.onPress();break;}
      }
    }
    if(mc.level==null||mc.player==null||mc.getSingleplayerServer()==null)return;
    if(++ticks==30 && (stage<0||captured)){
      if(++stage==3){mc.stop();return;}
      captured=false;ready=false;renders=CompanionBackLayer.renders();
      var id=mc.player.getUUID();
      var server=mc.getSingleplayerServer();
      int scene=stage;
      server.execute(()->setup(server.getPlayerList().getPlayer(id),scene));
    }
    if(ticks>700)throw new IllegalStateException("Creative grip visual fixture stalled at "+stage);
    if(stage<0)return;
    mc.options.hideGui=true;mc.options.setCameraType(CameraType.THIRD_PERSON_BACK);mc.options.fov().set(44);
    mc.setScreen(null);mc.getToasts().clear();
    if(bodyId<0){var p=mc.player.getUUID();var srv=mc.getSingleplayerServer();srv.execute(()->{
      var keeper=srv.getPlayerList().getPlayer(p);if(keeper!=null){var body=CompanionVisualSmokeSupport.body(keeper);if(body!=null)bodyId=body.getId();}
    });}
  }

  private static void setup(ServerPlayer p,int scene) {
    var world=p.serverLevel();
    if(scene==0){
      world.setDayTime(6000);world.setWeatherParameters(12000,0,false,false);
      for(int x=-7;x<=7;x++)for(int z=-7;z<=7;z++){
        world.setBlock(new BlockPos(x,94,z),Blocks.GRASS_BLOCK.defaultBlockState(),2);
        for(int y=95;y<=103;y++)world.setBlock(new BlockPos(x,y,z),Blocks.AIR.defaultBlockState(),2);
      }
      p.setGameMode(GameType.CREATIVE);p.teleportTo(world,.5,95,.5,0,3);
      var s=new ChaoState();s.adult=true;s.energy=100;s.setEvolutionStrength(.5f);
      CompanionVisualSmokeSupport.equip(p,s);
    }
    p.teleportTo(world,.5,scene==1?98:95,.5,0,3);
    p.getAbilities().flying=scene==1;p.onUpdateAbilities();
    ready=true;
  }

  @SubscribeEvent public static void frame(RenderFrameEvent.Post event) {
    if(!Boolean.getBoolean("chao_garden.creativeGripStudy")||!ready||captured||stage<0||stage>=3||ticks<55)return;
    var mc=Minecraft.getInstance();if(mc.level==null||mc.player==null||!(mc.level.getEntity(bodyId) instanceof ChaoEntity body))return;
    boolean attached=CompanionBackLayer.attached(body);
    if(attached!=(stage==1)||mc.player.isFallFlying()||mc.player.getAbilities().flying!=(stage==1))return;
    if(attached && (CompanionBackLayer.renders()<=renders || body.companionKeeperId()!=mc.player.getId()))return;
    int scene=stage;
    ready=false;
    Screenshot.grab(mc.gameDirectory,"chao039-"+NAMES[scene]+".png",mc.getMainRenderTarget(),msg->{
      LogUtils.getLogger().info("CHAO039_CAPTURE {} backLayer={} fallFlying={} {}",NAMES[scene],attached,false,msg.getString());
      mc.execute(()->{captured=true;ticks=0;});
    });
  }
}
