package com.chaogarden.client;

import com.chaogarden.hub.HubContent;
import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.AccessibilityOnboardingScreen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.level.*;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

/** Disposable real-lobby comparisons, including both sides of the actual authored stairs. */
@EventBusSubscriber(modid="chao_garden",value=Dist.CLIENT)
public final class LobbyFixVisualSmoke046 {
  // Eye xyz, then focus xyz. Keep these identical across vanilla and shader captures.
  private static final double[][] VIEWS={
      {8,84,6,0,82,0}, {16,85,-1,21,85,-10}, {20,80,-6,20,86,-11},
      {19,77,1,21,83,-6}, {-17,76,1,-22,75,9}, {-19,68,8,-22,73,10},
      {6,82,9,0,82,0}, {1,82,7,3,82,3}, {5,79,5,3,83,3}, {1,85,4,3,82,3}
  };
  private static final int[] VIEW_INDICES=selectedViews();
  private static int[] selectedViews(){
    String selected=System.getProperty("chao_garden.lobbyFixViews","");
    if(selected.isBlank())return java.util.stream.IntStream.range(0,VIEWS.length).toArray();
    return java.util.Arrays.stream(selected.split(",")).mapToInt(s->{int i=Integer.parseInt(s.trim());if(i<0||i>=VIEWS.length)throw new IllegalArgumentException("Lobby view "+i);return i;}).toArray();
  }
  private static boolean started,prepared,finished;
  private static volatile boolean ready;
  private static int ticks,stage,frames;
  private static long viewBegan;
  private static ArmorStand camera;
  @SubscribeEvent(priority=net.neoforged.bus.api.EventPriority.LOWEST)
  public static void materialProbe(net.neoforged.neoforge.client.event.RenderFrameEvent.Pre event)throws ReflectiveOperationException{
    // Excluded development fixture only: exercise the shader-compatible vertex
    // formats/material/depth states without claiming that Iris or Solas is loaded.
    // The first ten views remain ordinary shaders-off rendering.
    String mode=System.getProperty("chao_garden.lobbyFixStudy","");
    if(mode.equals("material")||mode.equals("vanilla-material")&&stage>=VIEW_INDICES.length){
      var field=LobbySceneRenderer025.class.getDeclaredField("shaderPack");field.setAccessible(true);field.setBoolean(null,true);
    }
  }
  @SubscribeEvent public static void frame(net.neoforged.neoforge.client.event.RenderLevelStageEvent event)throws ReflectiveOperationException{
    var mc=Minecraft.getInstance();
    if(viewBegan==0||mc.screen!=null||mc.getOverlay()!=null||SceneShaderPack.shadows()
        ||event.getStage()!=net.neoforged.neoforge.client.event.RenderLevelStageEvent.Stage.AFTER_LEVEL)return;
    var submitted=LobbySceneRenderer025.class.getDeclaredField("mainSceneSubmitted");submitted.setAccessible(true);
    if(submitted.getBoolean(null))frames++;
  }
  @SubscribeEvent public static void tick(ClientTickEvent.Post event){
    String mode=System.getProperty("chao_garden.lobbyFixStudy","");if(mode.isBlank()||finished)return;
    var mc=Minecraft.getInstance();if(++ticks>16000)throw new IllegalStateException("Lobby046 timeout");
    if(mc.screen instanceof AccessibilityOnboardingScreen)mc.setScreen(new TitleScreen());
    if(!started&&mc.screen instanceof TitleScreen){
      started=true;mc.options.renderDistance().set(5);mc.options.simulationDistance().set(5);
      mc.options.fov().set(65);mc.options.framerateLimit().set(30);mc.options.hideGui=true;mc.options.pauseOnLostFocus=false;
      mc.options.tutorialStep=net.minecraft.client.tutorial.TutorialSteps.NONE;
      String existing=System.getProperty("chao_garden.lobbyFixSave","");
      if(!existing.isBlank())mc.createWorldOpenFlows().openWorld(existing,()->{throw new IllegalStateException("Existing save failed to open");});
      else mc.createWorldOpenFlows().createFreshLevel("Lobby046-"+System.currentTimeMillis(),new LevelSettings("Lobby046",GameType.CREATIVE,false,Difficulty.PEACEFUL,true,new GameRules(),WorldDataConfiguration.DEFAULT),new WorldOptions(2546L,false,false),r->r.registryOrThrow(Registries.WORLD_PRESET).getHolderOrThrow(WorldPresets.FLAT).value().createWorldDimensions(),mc.screen);
    }
    if(!prepared&&mc.player!=null&&mc.getSingleplayerServer()!=null){
      prepared=true;mc.setScreen(null);var server=mc.getSingleplayerServer();
      server.execute(()->{var p=server.getPlayerList().getPlayers().getFirst();
        if(!p.level().dimension().equals(HubContent.HUB)&&!HubContent.enterHub(p))throw new IllegalStateException("Actual lobby entry failed");
        LogUtils.getLogger().info("LOBBY046_WORLD existingSave={} dimension={}",System.getProperty("chao_garden.lobbyFixSave",""),p.level().dimension().location());
        p.setGameMode(GameType.SPECTATOR);p.setNoGravity(true);p.teleportTo(10,80,10);ready=true;
      });
    }
    if(!ready||mc.level==null||mc.player==null||!mc.level.dimension().equals(HubContent.HUB))return;
    if(mc.screen!=null||mc.getOverlay()!=null){frames=0;viewBegan=0;return;}
    int total=mode.equals("vanilla-material")?VIEW_INDICES.length*2:VIEW_INDICES.length;
    if(stage==total){if(frames>8){finished=true;LogUtils.getLogger().info("LOBBY046_COMPLETE mode={} views={} irisActive={}",mode,stage,SceneShaderPack.active());mc.stop();}return;}
    if(SceneShaderPack.active()!=mode.startsWith("solas"))throw new IllegalStateException("Lobby046 requested shader state "+mode+" actual="+SceneShaderPack.active());
    int viewIndex=VIEW_INDICES[stage%VIEW_INDICES.length];var v=VIEWS[viewIndex];double x=v[0],y=v[1],z=v[2];
    float yaw=(float)Math.toDegrees(Math.atan2(x-v[3],v[5]-z));
    float pitch=(float)-Math.toDegrees(Math.atan2(v[4]-y,Math.hypot(v[3]-x,v[5]-z)));
    if(camera==null)camera=new ArmorStand(mc.level,x,y,z);
    camera.moveTo(x,y-camera.getEyeHeight(),z,yaw,pitch);camera.xo=camera.getX();camera.yo=camera.getY();camera.zo=camera.getZ();
    camera.yRotO=yaw;camera.xRotO=pitch;camera.setYHeadRot(yaw);camera.yHeadRotO=yaw;mc.setCameraEntity(camera);
    if(viewBegan==0)viewBegan=System.nanoTime();
    if(frames>45&&System.nanoTime()-viewBegan>8_000_000_000L){
      String name=String.format("lobby046-%s-%02d.png",mode,stage);
      boolean probe=mode.equals("material")||mode.equals("vanilla-material")&&stage>=VIEW_INDICES.length;
      Screenshot.grab(mc.gameDirectory,name,mc.getMainRenderTarget(),message->LogUtils.getLogger().info("LOBBY046_CAPTURE {} view={} nativeMaterialProbe={} irisActive={} {}",name,viewIndex,probe,SceneShaderPack.active(),message.getString()));
      stage++;frames=0;viewBegan=System.nanoTime();
    }
  }
}
