package com.chaogarden.client;

import com.chaogarden.ChaoGarden;
import com.chaogarden.core.ChaoState;
import com.chaogarden.entity.ChaoEntity;
import com.mojang.logging.LogUtils;
import net.minecraft.client.*;
import net.minecraft.client.gui.screens.*;
import net.minecraft.core.*;
import net.minecraft.world.Difficulty;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/** Opt-in production navigation and gait comparison; excluded from the mod JAR. */
@EventBusSubscriber(modid=ChaoGarden.MODID,value=Dist.CLIENT)
public final class RunningVisualStudy046 {
  private static final int[] POINTS={80,1000,4500};
  private static final String[] GAITS={"walk","run","dash"};
  private static boolean started,setup;
  private static int ticks,shots,movingFrames,stationaryFrames;
  private static final int[] movingByScene=new int[6],stoppedByScene=new int[6],renderedByScene=new int[6];
  private static volatile int scene=-1;
  private static volatile long elapsed;
  private static volatile int actorId=-1;
  private static ChaoEntity actor;
  private static long began;
  private static float priorSwing,minRate=Float.POSITIVE_INFINITY,maxRate;
  private static double travelled,lastX;
  private static boolean enabled(){return Boolean.getBoolean("chao_garden.runningVisualStudy");}
  private static void verifyAge(ChaoEntity chao,int index,String side) {
    if(chao.state()==null||chao.state().adult!=(index>=3)||chao.isCocooned()||chao.state().dead)
      throw new IllegalStateException("running "+side+" fixture changed age or entered a cocoon: scene="+index
          +" adult="+(chao.state()!=null&&chao.state().adult)+" cocoon="+chao.isCocooned());
  }
  private static void next(net.minecraft.server.MinecraftServer server,int index) {
    if(actor!=null) {
      if(travelled<1)throw new IllegalStateException("running fixture did not actually navigate");
      LogUtils.getLogger().info("RUNNING046_SERVER_CASE_COMPLETE {} distance={}",scene,travelled);
      actor.discard();
    }
    scene=index;elapsed=0;began=server.overworld().getGameTime();travelled=0;
    var state=new ChaoState();state.adult=index>=3;state.points[ChaoState.RUN]=POINTS[index%3];
    state.energy=100;state.hunger=100;state.boredom=100;state.happiness=100;
    // A fully aged baby is ready for its first evolution, not a baby gait fixture.
    state.setEvolutionStrength(state.adult?1.2f:.2f);
    if(index==4){state.primaryType=ChaoState.RUN_TYPE;state.align=1;}
    if(index==5){state.primaryType=ChaoState.POWER_TYPE;state.align=-1;}
    actor=ChaoGarden.CHAO.get().create(server.overworld());actor.setState(state);actor.setInvulnerable(true);
    actor.goalSelector.removeAllGoals(g->true);actor.targetSelector.removeAllGoals(g->true);actor.deferBehavior(10000);
    actor.moveTo(-12,80,0,-90,0);actor.yBodyRot=actor.yBodyRotO=actor.yHeadRot=actor.yHeadRotO=-90;
    server.overworld().addFreshEntity(actor);actorId=actor.getId();lastX=actor.getX();
    LogUtils.getLogger().info("RUNNING046_VISUAL_CASE {} adult={} Run={} gait={}",index,state.adult,POINTS[index%3],GAITS[index%3]);
  }
  @SubscribeEvent public static void server(ServerTickEvent.Post event) {
    if(!enabled()||actor==null||scene>=6)return;
    var server=event.getServer();elapsed=server.overworld().getGameTime()-began;
    verifyAge(actor,scene,"server");
    travelled+=Math.abs(actor.getX()-lastX);lastX=actor.getX();
    if(elapsed<110)actor.getNavigation().moveTo(55,80,0,1);
    else {actor.getNavigation().stop();actor.getMoveControl().setWantedPosition(actor.getX(),actor.getY(),actor.getZ(),0);}
    if(elapsed>150) {
      if(scene<5)next(server,scene+1);
      else {
        if(travelled<1)throw new IllegalStateException("last running fixture did not navigate");
        LogUtils.getLogger().info("RUNNING046_SERVER_CASE_COMPLETE {} distance={}",scene,travelled);
        actor.discard();scene=6;
      }
    }
  }
  @SubscribeEvent public static void rendered(net.neoforged.neoforge.client.event.RenderLivingEvent.Post<?,?> event) {
    if(!enabled()||scene<0||scene>=6||elapsed<=10||!(event.getEntity() instanceof ChaoEntity c)||c.getId()!=actorId)return;
    verifyAge(c,scene,"rendered");renderedByScene[scene]++;
  }
  @SubscribeEvent public static void client(ClientTickEvent.Post event) {
    if(!enabled())return;var mc=Minecraft.getInstance();ticks++;
    if(mc.screen instanceof AccessibilityOnboardingScreen)mc.setScreen(new TitleScreen());
    if(!started&&mc.screen instanceof TitleScreen) {
      started=true;mc.options.renderDistance().set(3);mc.options.simulationDistance().set(5);mc.options.framerateLimit().set(30);mc.options.fov().set(45);mc.options.pauseOnLostFocus=false;mc.options.hideGui=true;
      mc.createWorldOpenFlows().createFreshLevel("Running046-"+System.currentTimeMillis(),new LevelSettings("Running046",GameType.CREATIVE,false,Difficulty.PEACEFUL,true,new GameRules(),WorldDataConfiguration.DEFAULT),new net.minecraft.world.level.levelgen.WorldOptions(2546L,false,false),a->a.registryOrThrow(net.minecraft.core.registries.Registries.WORLD_PRESET).getHolderOrThrow(net.minecraft.world.level.levelgen.presets.WorldPresets.FLAT).value().createWorldDimensions(),mc.screen);
    }
    if(!setup&&mc.player!=null&&mc.getSingleplayerServer()!=null) {
      setup=true;mc.setScreen(null);ChaoRenderer.suppressNames=true;
      mc.getSingleplayerServer().execute(()->{
        var server=mc.getSingleplayerServer();var level=server.overworld();level.setDayTime(6000);level.getGameRules().getRule(GameRules.RULE_DAYLIGHT).set(false,server);level.getGameRules().getRule(GameRules.RULE_DOMOBSPAWNING).set(false,server);
        for(var p:BlockPos.betweenClosed(-20,79,-5,65,84,5))level.setBlock(p,p.getY()==79?Blocks.SMOOTH_STONE.defaultBlockState():Blocks.AIR.defaultBlockState(),3);
        server.getPlayerList().getPlayers().forEach(p->p.setGameMode(GameType.SPECTATOR));next(server,0);
      });
    }
    if(mc.level!=null&&mc.player!=null&&scene>=0&&scene<6&&mc.level.getEntity(actorId) instanceof ChaoEntity c) {
      if(elapsed>10)verifyAge(c,scene,"client");
      var at=c.position().add(elapsed<65?2.2:0,.9,elapsed<65?1.8:2.8);
      var focus=c.position().add(0,.40,0);
      float yaw=(float)(Math.toDegrees(Math.atan2(focus.z-at.z,focus.x-at.x))-90);
      float pitch=(float)-Math.toDegrees(Math.atan2(focus.y-at.y,Math.hypot(focus.x-at.x,focus.z-at.z)));
      mc.player.moveTo(at.x,at.y-mc.player.getEyeHeight(),at.z,yaw,pitch);mc.player.yRotO=yaw;mc.player.xRotO=pitch;
      float swing=c.walkAnimation.position(),rate=swing-priorSwing;priorSwing=swing;
      if(elapsed>20&&elapsed<105&&rate>0) {movingFrames++;movingByScene[scene]++;minRate=Math.min(minRate,rate);maxRate=Math.max(maxRate,rate);}
      if(elapsed>135&&c.walkAnimation.speed()<.004f){stationaryFrames++;stoppedByScene[scene]++;}
      if(elapsed>10&&ticks%4==0)Screenshot.grab(mc.gameDirectory,String.format("running046-%d-%s-t%03d-%04d.png",scene,GAITS[scene%3],elapsed,shots++),mc.getMainRenderTarget(),text->{});
      if(elapsed==108)LogUtils.getLogger().info("RUNNING046_CLIENT_RATE scene={} speed={} swing={}",scene,c.walkAnimation.speed(),swing);
    }
    if(scene==6) {
      if(movingFrames<100||stationaryFrames<30)throw new IllegalStateException("running study did not observe moving/stopped cases");
      for(int index=0;index<6;index++) {
        if(movingByScene[index]<20||stoppedByScene[index]<5||renderedByScene[index]<20)
          throw new IllegalStateException("running study missed a valid age/speed scene="+index+" moving="+movingByScene[index]
              +" stopped="+stoppedByScene[index]+" rendered="+renderedByScene[index]);
        LogUtils.getLogger().info("RUNNING046_AGE_VERIFIED scene={} adult={} cocoon=false movingTicks={} stoppedTicks={} renderedFrames={}",
            index,index>=3,movingByScene[index],stoppedByScene[index],renderedByScene[index]);
      }
      LogUtils.getLogger().info("RUNNING046_VISUAL_COMPLETE shots={} movingTicks={} stoppedTicks={} observedRate={}..{}",shots,movingFrames,stationaryFrames,minRate,maxRate);mc.stop();
    }
    if(ticks>7000)throw new IllegalStateException("running client study timed out");
  }
}
