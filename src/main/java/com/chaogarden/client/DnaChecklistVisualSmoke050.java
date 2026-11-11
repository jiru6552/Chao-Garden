package com.chaogarden.client;

import com.chaogarden.ChaoGarden;
import com.chaogarden.core.ChaoState;
import com.chaogarden.entity.ChaoEntity;
import com.mojang.logging.LogUtils;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.screens.AccessibilityOnboardingScreen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;

/** Real Analyzer pagination/filter/counter regression; opt-in and excluded from production JARs. */
@EventBusSubscriber(modid=ChaoGarden.MODID,value=Dist.CLIENT)
public final class DnaChecklistVisualSmoke050 {
  private static boolean started,setup;
  private static volatile boolean ready;
  private static volatile RuntimeException failure;
  private static volatile int entity;
  private static int ticks,renderedFrames;
  private static long studyStartedAt,lastStatusAt;
  private static final long TIMEOUT_NANOS=java.util.concurrent.TimeUnit.MINUTES.toNanos(5);
  private static final AtomicInteger captures=new AtomicInteger();

  private static void check(boolean ok,String message){if(!ok)throw new IllegalStateException("DNA050 "+message);}
  private static ChaoEntity chao(){var level=Minecraft.getInstance().level;return level==null?null:(ChaoEntity)level.getEntity(entity);}
  private static ChaoInspectionScreen screen(){return (ChaoInspectionScreen)Minecraft.getInstance().screen;}
  private static void server(java.util.function.Consumer<net.minecraft.server.level.ServerPlayer> action){
    var mc=Minecraft.getInstance();var playerId=mc.player.getUUID();
    mc.getSingleplayerServer().execute(()->{try{action.accept(mc.getSingleplayerServer().getPlayerList().getPlayer(playerId));}catch(RuntimeException e){failure=e;}});
  }
  private static void mask(int mask){server(p->{var c=(ChaoEntity)p.serverLevel().getEntity(entity);c.state().animalMask=mask;c.sync();});}
  private static void press(String label){
    var screen=screen();
    for(var widget:screen.children())if(widget instanceof AbstractButton button&&button.visible&&button.getMessage().getString().equals(label)){
      check(button.active,"Inactive button "+label);
      check(screen.mouseClicked(screen.screenX(button.getX()+button.getWidth()/2f),screen.screenY(button.getY()+button.getHeight()/2f),0),"Button did not consume click "+label);
      return;
    }
    throw new IllegalStateException("DNA050 missing button "+label);
  }
  private static boolean active(String label){return screen().children().stream().anyMatch(w->w instanceof AbstractButton b&&b.visible&&b.active&&b.getMessage().getString().equals(label));}
  private static void page(int index,List<Integer> ids){
    check(screen().dnaPageIndex()==index,"Wrong page "+screen().dnaPageIndex()+" expected "+index);
    check(screen().visibleDnaIds().equals(ids),"Wrong displayed species "+screen().visibleDnaIds()+" expected "+ids);
  }
  private static List<Integer> range(int begin,int end){return IntStream.range(begin,end).boxed().toList();}
  private static void capture(String label){
    var mc=Minecraft.getInstance();check(mc.screen instanceof ChaoInspectionScreen&&renderedFrames>0,"No rendered Analyzer for capture");
    Screenshot.grab(mc.gameDirectory,"dna050-"+label+".png",mc.getMainRenderTarget(),message->{captures.incrementAndGet();LogUtils.getLogger().info("DNA050_CAPTURE {}",label);});
  }
  @SubscribeEvent public static void rendered(ScreenEvent.Render.Post event){
    if(Boolean.getBoolean("chao_garden.dna050Study")&&event.getScreen() instanceof ChaoInspectionScreen)renderedFrames++;
  }
  @SubscribeEvent public static void tick(ClientTickEvent.Post event){
    if(!Boolean.getBoolean("chao_garden.dna050Study"))return;
    var mc=Minecraft.getInstance();
    long now=System.nanoTime();
    if(studyStartedAt==0)studyStartedAt=lastStatusAt=now;
    if(failure!=null)throw failure;
    if(now-studyStartedAt>TIMEOUT_NANOS)throw new IllegalStateException("DNA050 timed out after five minutes: screen="
        +(mc.screen==null?"none":mc.screen.getClass().getSimpleName())+" started="+started+" setup="+setup
        +" ready="+ready+" player="+(mc.player!=null)+" level="+(mc.level!=null)+" ticks="+ticks);
    if(now-lastStatusAt>java.util.concurrent.TimeUnit.SECONDS.toNanos(30)){
      lastStatusAt=now;LogUtils.getLogger().info("DNA050_PROGRESS screen={} started={} setup={} ready={} ticks={}",
          mc.screen==null?"none":mc.screen.getClass().getSimpleName(),started,setup,ready,ticks);
    }
    // Fresh disposable game directories show this before the first title screen.
    if(mc.screen instanceof AccessibilityOnboardingScreen)mc.setScreen(new TitleScreen());
    if(!started&&mc.screen instanceof TitleScreen){
      started=true;mc.getWindow().setWindowed(1280,720);mc.options.renderDistance().set(5);mc.options.simulationDistance().set(5);mc.options.guiScale().set(2);mc.options.pauseOnLostFocus=false;
      mc.createWorldOpenFlows().createFreshLevel("DnaChecklist050-"+System.currentTimeMillis(),
          new net.minecraft.world.level.LevelSettings("DNA checklist050",GameType.CREATIVE,false,net.minecraft.world.Difficulty.PEACEFUL,true,new net.minecraft.world.level.GameRules(),net.minecraft.world.level.WorldDataConfiguration.DEFAULT),
          new net.minecraft.world.level.levelgen.WorldOptions(2550L,false,false),a->a.registryOrThrow(net.minecraft.core.registries.Registries.WORLD_PRESET).getHolderOrThrow(net.minecraft.world.level.levelgen.presets.WorldPresets.FLAT).value().createWorldDimensions(),mc.screen);
      return;
    }
    if(mc.player==null||mc.level==null||mc.getSingleplayerServer()==null)return;
    if(!setup){
      setup=true;mc.setScreen(null);server(p->{
        var level=p.serverLevel();level.setDayTime(6000);
        level.getGameRules().getRule(net.minecraft.world.level.GameRules.RULE_DAYLIGHT).set(false,p.server);
        for(var at:BlockPos.betweenClosed(4,79,4,12,84,12))level.setBlock(at,at.getY()==79?Blocks.GRASS_BLOCK.defaultBlockState():Blocks.AIR.defaultBlockState(),3);
        p.connection.teleport(8.5,80,10.5,180,10);p.setNoGravity(true);
        var c=ChaoGarden.CHAO.get().create(level);c.owner=p.getUUID();c.setNoAi(true);c.setNoGravity(true);c.moveTo(8.5,80,8.5,0,0);
        var state=new ChaoState();state.reincarnations=2;state.happiness=90;state.hunger=90;state.energy=100;state.animalMask=(1<<21)-1;
        c.setState(state);c.setCustomName(Component.literal("DNA checklist"));level.addFreshEntity(c);entity=c.getId();ready=true;
      });return;
    }
    if(!ready||chao()==null)return;
    ticks++;
    if(ticks==20){var screen=new ChaoInspectionScreen(chao());screen.showAnimals(1);mc.setScreen(screen);}
    if(ticks==40){page(0,range(0,21));check(!active("PREV")&&active("NEXT"),"First-page navigation");capture("original21-page1");press("NEXT");}
    if(ticks==60){page(1,range(21,ChaoState.ANIMALS.length));check(active("PREV")&&!active("NEXT"),"Last-page navigation");capture("extra10-page2");press("MISSING ONLY");}
    if(ticks==80){page(0,range(21,ChaoState.ANIMALS.length));check(screen().dnaPageCount()==1&&!active("NEXT"),"Missing filter did not reset pagination");capture("missing-extra10");press("SHOW ALL");}
    if(ticks==100){page(0,range(0,21));press("MISSING ONLY");mask(0);}
    if(ticks==120){page(0,range(0,21));check(screen().dnaPageCount()==2,"Live new missing DNA not reflected");press("NEXT");}
    if(ticks==140){page(1,range(21,ChaoState.ANIMALS.length));capture("all-missing-page2");mask((1<<21)-1);}
    if(ticks==160){page(0,range(21,ChaoState.ANIMALS.length));check(screen().dnaPageCount()==1,"Live filter did not clamp removed page");capture("live-clamped-missing");mask(ChaoState.ALL_ANIMALS);}
    if(ticks==180){page(0,List.of());check(!active("PREV")&&!active("NEXT"),"Completed checklist navigation");capture("complete31");mask(ChaoState.ALL_ANIMALS&~(1<<(ChaoState.ANIMALS.length-1)));}
    if(ticks==200){page(0,List.of(ChaoState.ANIMALS.length-1));capture("last-species-missing");press("SHOW ALL");}
    if(ticks==220){page(0,range(0,21));press("NEXT");}
    if(ticks==240){page(1,range(21,ChaoState.ANIMALS.length));press("PREV");}
    if(ticks==260){page(0,range(0,21));check(captures.get()==7&&renderedFrames>50,"Incomplete real-client render/capture coverage");LogUtils.getLogger().info("DNA050_VISUAL_PASS screenshots={} renderedFrames={} species={} nativeButtons=true bothPages=true missingFilter=true liveClamp=true completion=true",captures.get(),renderedFrames,ChaoState.ANIMALS.length);mc.setScreen(null);mc.stop();}
  }
}
