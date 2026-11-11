package com.chaogarden.client;

import com.chaogarden.ChaoGarden;
import com.chaogarden.entity.ChaoEntity;
import com.chaogarden.test.ToyVariantCases02522;
import com.chaogarden.toys.*;
import com.mojang.logging.LogUtils;
import java.nio.file.*;
import java.util.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.*;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Difficulty;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.*;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/** Pose gallery only; live sessions and networking have a separate two-client test. */
@EventBusSubscriber(modid=ChaoGarden.MODID,value=Dist.CLIENT)
public final class Toy02522GalleryVisualSmoke {
  private static final Path DIR=Path.of("/tmp/chao-lionfish-variants/gallery");
  private static final List<ToyVariantCases02522.Case> CASES=ToyVariantCases02522.all();
  private static boolean enabled(){return Boolean.getBoolean("chao_garden.toy02522Gallery");}
  private static boolean started,setup;private static int ticks,seenAt,frames;private static volatile int step=-1;
  private static int requested=-1,observed=-1;private static volatile ChaoEntity actor;
  private static void check(boolean ok,String why){if(!ok)throw new IllegalStateException("TOY02522_GALLERY "+why);}
  private static String label(int i){return String.format("%03d-%s-%s-%d",i,CASES.get(i/4).name(),i%4<2?"rattle":"bubbles",i%2==0?40:100);}
  private static void next(int i){
    requested=i;Minecraft.getInstance().getSingleplayerServer().execute(()->{
      var level=Minecraft.getInstance().getSingleplayerServer().overworld();if(actor!=null)actor.discard();
      var c=ChaoGarden.CHAO.get().create(level);c.setState(CASES.get(i/4).state());c.setNoAi(true);c.setNoGravity(true);c.setInvulnerable(true);c.moveTo(.5,80,.5,0,0);
      c.setCustomName(Component.literal(label(i)));level.addFreshEntity(c);actor=c;step=i;
    });
  }
  @SubscribeEvent public static void server(ServerTickEvent.Post event){
    if(!enabled()||actor==null)return;int elapsed=step%2==0?40:100;
    actor.setToyPlaying(step%4<2?RattlePlayRules.KIND:BubblePlayRules.KIND,160-elapsed);
  }
  @SubscribeEvent public static void render(RenderLivingEvent.Post<?,?> event){
    if(!enabled()||!(event.getEntity() instanceof ChaoEntity c)||!c.hasCustomName())return;
    var model=(ChaoModel)event.getRenderer().getModel();check(model.officialPosesFinite(),"finite pose "+c.getName().getString());
    if(c.toyPlayingTicks()>0)check(model.rattlePalm()!=null,"visible grip "+c.getName().getString());frames++;
  }
  @SubscribeEvent public static void tick(ClientTickEvent.Post event){
    if(!enabled())return;var mc=Minecraft.getInstance();ticks++;
    if(mc.screen instanceof AccessibilityOnboardingScreen)mc.setScreen(new TitleScreen());
    if(!started&&mc.screen instanceof TitleScreen){
      started=true;mc.options.renderDistance().set(3);mc.options.simulationDistance().set(5);mc.options.framerateLimit().set(30);mc.options.fov().set(42);mc.options.pauseOnLostFocus=false;mc.options.hideGui=true;
      mc.createWorldOpenFlows().createFreshLevel("Toy02522Gallery-"+System.currentTimeMillis(),
          new LevelSettings("Toy02522Gallery",GameType.CREATIVE,false,Difficulty.PEACEFUL,true,new GameRules(),WorldDataConfiguration.DEFAULT),
          new net.minecraft.world.level.levelgen.WorldOptions(2522L,false,false),access->access.registryOrThrow(net.minecraft.core.registries.Registries.WORLD_PRESET)
          .getHolderOrThrow(net.minecraft.world.level.levelgen.presets.WorldPresets.FLAT).value().createWorldDimensions(),mc.screen);
    }
    if(!setup&&mc.player!=null&&mc.getSingleplayerServer()!=null){
      setup=true;mc.setScreen(null);ChaoRenderer.suppressNames=true;
      mc.getSingleplayerServer().execute(()->{
        var server=mc.getSingleplayerServer();var l=server.overworld();l.setDayTime(6000);l.getGameRules().getRule(GameRules.RULE_DAYLIGHT).set(false,server);l.getGameRules().getRule(GameRules.RULE_DOMOBSPAWNING).set(false,server);
        for(var p:BlockPos.betweenClosed(-10,79,-10,10,86,10))l.setBlock(p,p.getY()==79?Blocks.SMOOTH_STONE.defaultBlockState():Blocks.AIR.defaultBlockState(),3);
        server.getPlayerList().getPlayers().forEach(p->{p.setGameMode(GameType.SPECTATOR);p.connection.teleport(1.15,79.25,2.6,163,12);});
      });next(0);
    }
    if(mc.player!=null&&mc.level!=null&&step==requested&&step>=0){
      for(var e:mc.level.entitiesForRendering())if(e instanceof ChaoEntity c&&c.hasCustomName()&&c.getName().getString().equals(label(step))&&c.toyPlayingTicks()>0){
        if(observed!=step){observed=step;seenAt=ticks;}
        if(ticks-seenAt<(step==0?60:10))break;
        Screenshot.grab(mc.gameDirectory,"02522-gallery-"+label(step)+".png",mc.getMainRenderTarget(),text->LogUtils.getLogger().info("TOY02522_GALLERY_CAPTURE {}",text.getString()));
        if(step+1==CASES.size()*4){
          try{Files.createDirectories(DIR);Files.writeString(DIR.resolve("pass.txt"),"cases="+CASES.size()+" captures="+(step+1)+" rendered="+frames+"; pose gallery, not live AI proof\n");}catch(Exception ex){throw new RuntimeException(ex);}
          LogUtils.getLogger().info("TOY02522_GALLERY_PASS cases={} captures={} frames={}",CASES.size(),step+1,frames);mc.stop();
        }else next(step+1);break;
      }
    }
    check(ticks<18000,"timeout");
  }
}
