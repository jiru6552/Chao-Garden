package com.chaogarden.client;

import com.chaogarden.fruit.FruitContent;
import com.chaogarden.hub.LobbyDecor025;
import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

/** Visual fixture for comparing the same two BERs with and without a real shaderpack. */
@EventBusSubscriber(modid="chao_garden", value=Dist.CLIENT)
public final class PalmShaderVisualSmoke {
  private static boolean started, prepared;
  private static volatile boolean ready;
  private static int frames, stage;
  private static long startedView;
  @SubscribeEvent public static void frame(net.neoforged.neoforge.client.event.RenderFrameEvent.Post event) {
    if (startedView != 0) frames++;
  }
  private static ArmorStand camera;

  @SubscribeEvent public static void tick(ClientTickEvent.Post event) throws ReflectiveOperationException {
    String mode = System.getProperty("chao.palmShader");
    if (mode == null) return;
    var mc = Minecraft.getInstance();
    if (mc.screen instanceof AccessibilityOnboardingScreen) mc.setScreen(new TitleScreen());
    if (!started && mc.screen instanceof TitleScreen) {
      started = true;
      mc.options.renderDistance().set(4); mc.options.simulationDistance().set(5);
      mc.options.fov().set(62); mc.options.framerateLimit().set(30);
      mc.options.pauseOnLostFocus=false; mc.options.hideGui=true;
      mc.options.tutorialStep=net.minecraft.client.tutorial.TutorialSteps.NONE;
      mc.createWorldOpenFlows().createFreshLevel("PalmShader", new LevelSettings("PalmShader", GameType.CREATIVE,
          false, Difficulty.PEACEFUL,true,new GameRules(),WorldDataConfiguration.DEFAULT),
          new WorldOptions(38038L,false,false), r->r.registryOrThrow(Registries.WORLD_PRESET)
              .getHolderOrThrow(WorldPresets.FLAT).value().createWorldDimensions(),mc.screen);
    }
    if (!prepared && mc.player!=null && mc.getSingleplayerServer()!=null) {
      prepared=true; mc.setScreen(null);
      var server=mc.getSingleplayerServer();
      server.execute(()->{
        var level=server.overworld();
        level.setDayTime(5000);
        level.getGameRules().getRule(GameRules.RULE_DAYLIGHT).set(false,server);
        level.getGameRules().getRule(GameRules.RULE_DOMOBSPAWNING).set(false,server);
        var player=server.getPlayerList().getPlayers().getFirst();
        player.setNoGravity(true); player.teleportTo(125,77,138);
        for(var at:BlockPos.betweenClosed(106,75,110,148,85,145))
          level.setBlock(at, (at.getY()==75?Blocks.GRASS_BLOCK:Blocks.AIR).defaultBlockState(),2);
        var garden=new BlockPos(120,76,125); var planted=garden.east(11);
        for(int y=0;y<4;y++)level.setBlock(garden.above(y),LobbyDecor025.PALM_TRUNK.get().defaultBlockState(),3);
        level.setBlock(garden.above(4),LobbyDecor025.PALM_CROWN.get().defaultBlockState(),3);
        if(!FruitContent.growTree(level,planted,"garden_fruit")) throw new AssertionError("Planted palm fixture");
        ready=true;
      });
    }
    if (!ready || mc.level==null || mc.player==null) return;
    if (!mc.level.getBlockState(new BlockPos(120,80,125)).is(LobbyDecor025.PALM_CROWN.get())
        || !mc.level.getBlockState(new BlockPos(131,80,125)).is(FruitContent.treeForFruit("garden_fruit"))) return;
    if (startedView == 0) startedView = System.nanoTime();
    boolean below = stage == 1;
    double x=126, y=below?77.1:85.0, z=below?136:141, tx=126, ty=80.15, tz=125;
    if(camera==null)camera=new ArmorStand(mc.level,x,y,z);
    float yaw=(float)Math.toDegrees(Math.atan2(x-tx,tz-z));
    float pitch=(float)-Math.toDegrees(Math.atan2(ty-y,Math.hypot(tx-x,tz-z)));
    camera.moveTo(x,y-camera.getEyeHeight(),z,yaw,pitch);
    camera.xo=camera.getX();camera.yo=camera.getY();camera.zo=camera.getZ();
    camera.yRotO=yaw;camera.xRotO=pitch;camera.setYHeadRot(yaw);camera.yHeadRotO=yaw;
    mc.setCameraEntity(camera);
    if (stage == 2) {
      if (frames > 8) mc.stop();
      return;
    }
    if (frames >= 40 && System.nanoTime() - startedView > 12_000_000_000L) {
      var api=Class.forName("net.irisshaders.iris.api.v0.IrisApi");
      boolean active=(boolean)api.getMethod("isShaderPackInUse").invoke(api.getMethod("getInstance").invoke(null));
      if(active!=!mode.equals("off"))throw new AssertionError("Unexpected Iris shader state: "+active);
      String name=mode+(below?"-below.png":"-above.png");
      Screenshot.grab(mc.gameDirectory,name,mc.getMainRenderTarget(),message->LogUtils.getLogger().info("PALM38 {} shaderActive={} {}",name,active,message.getString()));
      stage++;
      frames=0;
      startedView=System.nanoTime();
    }
  }
}
