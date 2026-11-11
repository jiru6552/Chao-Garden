package com.chaogarden.client;

import com.chaogarden.ChaoGarden;
import com.chaogarden.entity.ChaoEntity;
import com.chaogarden.test.ToyVariantCases02522;
import com.chaogarden.toys.*;
import com.mojang.logging.LogUtils;
import java.nio.file.*;
import net.minecraft.client.*;
import net.minecraft.client.gui.screens.*;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Difficulty;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/** Fixed-camera footage of actual placed-toy AI, including entry, exit and cancellation. Never packaged. */
@EventBusSubscriber(modid=ChaoGarden.MODID,value=Dist.CLIENT)
public final class Toy02522MotionVisualSmoke {
  private static final String[] CASES={"animal_rabbit_baby","chaos_2","animal_scorpion_baby","omochao_adult","animal_dragon_baby","tails_adult"};
  private static final BlockPos TOY=new BlockPos(0,80,2);
  private static boolean started,setup;private static int ticks,frame;
  private static volatile int phase=-1;private static volatile long elapsed=-1;
  private static ChaoEntity actor;private static long began=-1,spawned;private static boolean cancelled;
  private static boolean enabled(){return Boolean.getBoolean("chao_garden.toy02522Motion");}
  private static void check(boolean ok,String why){if(!ok)throw new IllegalStateException("TOY02522_MOTION "+why);}
  private static void next(net.minecraft.server.MinecraftServer server,int index){
    if(actor!=null)actor.discard();var l=server.overworld();phase=index;began=-1;elapsed=-1;cancelled=false;spawned=l.getGameTime();
    var s=ToyVariantCases02522.all().stream().filter(c->c.name().equals(CASES[index])).findFirst().orElseThrow().state();
    actor=ChaoGarden.CHAO.get().create(l);actor.setState(s);actor.setInvulnerable(true);actor.getRandom().setSeed(18);actor.moveTo(.5,80,.5,0,0);
    actor.setCustomName(Component.literal("Motion "+index));l.addFreshEntity(actor);
    actor.goalSelector.removeAllGoals(g->true);actor.targetSelector.removeAllGoals(g->true);actor.goalSelector.addGoal(4,new ToyContent.PlayGoal(actor));
    l.setBlock(TOY,ToyContent.block(index==2||index==3?BubblePlayRules.KIND:RattlePlayRules.KIND).defaultBlockState(),3);
    LogUtils.getLogger().info("TOY02522_MOTION_CASE {} {}",index,CASES[index]);
  }
  @SubscribeEvent public static void server(ServerTickEvent.Post e){
    if(!enabled()||actor==null||phase>=CASES.length)return;var l=e.getServer().overworld();
    check(l.getGameTime()-spawned<2000,"actual AI session timed out");
    if(ToyContent.isPlaying(actor)&&began<0)began=actor.getPersistentData().getLong("ChaoToyUntil")-160;
    if(began<0)return;elapsed=l.getGameTime()-began;
    if(phase>=4&&elapsed>=60&&!cancelled){cancelled=true;actor.setStaying(true);}
    if(elapsed>(phase>=4?100:190)){
      check(!ToyContent.isPlaying(actor),"session stopped");
      check(actor.state().happiness==(phase>=4?20:phase==0?27:24),"one completion reward; none on cancellation");
      check(!l.getBlockState(TOY).getValue(ChaoToyBlock.PLAYING),"placed source restored");
      LogUtils.getLogger().info("TOY02522_MOTION_CASE_PASS {}",phase);
      if(phase+1<CASES.length)next(e.getServer(),phase+1);else phase=CASES.length;
    }
  }
  @SubscribeEvent public static void client(ClientTickEvent.Post e){
    if(!enabled())return;var mc=Minecraft.getInstance();ticks++;
    if(mc.screen instanceof AccessibilityOnboardingScreen)mc.setScreen(new TitleScreen());
    if(!started&&mc.screen instanceof TitleScreen){
      started=true;mc.options.renderDistance().set(3);mc.options.simulationDistance().set(5);mc.options.framerateLimit().set(30);mc.options.fov().set(42);mc.options.pauseOnLostFocus=false;mc.options.hideGui=true;
      mc.createWorldOpenFlows().createFreshLevel("Toy02522Motion-"+System.currentTimeMillis(),
          new LevelSettings("Toy02522Motion",GameType.CREATIVE,false,Difficulty.PEACEFUL,true,new GameRules(),WorldDataConfiguration.DEFAULT),
          new net.minecraft.world.level.levelgen.WorldOptions(2522L,false,false),access->access.registryOrThrow(net.minecraft.core.registries.Registries.WORLD_PRESET)
          .getHolderOrThrow(net.minecraft.world.level.levelgen.presets.WorldPresets.FLAT).value().createWorldDimensions(),mc.screen);
    }
    if(!setup&&mc.player!=null&&mc.getSingleplayerServer()!=null){
      setup=true;mc.setScreen(null);ChaoRenderer.suppressNames=true;
      mc.getSingleplayerServer().execute(()->{
        var server=mc.getSingleplayerServer();var l=server.overworld();l.setDayTime(6000);l.getGameRules().getRule(GameRules.RULE_DAYLIGHT).set(false,server);l.getGameRules().getRule(GameRules.RULE_DOMOBSPAWNING).set(false,server);
        for(var p:BlockPos.betweenClosed(-10,79,-10,10,86,10))l.setBlock(p,p.getY()==79?Blocks.SMOOTH_STONE.defaultBlockState():Blocks.AIR.defaultBlockState(),3);
        server.getPlayerList().getPlayers().forEach(p->{p.setGameMode(GameType.SPECTATOR);p.connection.teleport(1.15,79.25,3.2,163,12);});next(server,0);
      });
    }
    // A real AI approach changes the stand position. Frame the rendered actor
    // at a constant angle/distance; never move or freeze the subject to film it.
    if(mc.player!=null&&mc.level!=null&&phase>=0&&phase<CASES.length){
      for(var subject:mc.level.entitiesForRendering())if(subject instanceof ChaoEntity c&&c.hasCustomName()&&c.getName().getString().equals("Motion "+phase)){
        var forward=net.minecraft.world.phys.Vec3.directionFromRotation(0,c.getYRot());
        var at=c.position().add(forward.scale(2.6)).add(-forward.z*.65,0,forward.x*.65);
        float yaw=(float)(Math.toDegrees(Math.atan2(c.getZ()-at.z,c.getX()-at.x))-90);
        float pitch=(float)Math.toDegrees(Math.atan2(.40,Math.hypot(c.getX()-at.x,c.getZ()-at.z)));
        mc.player.moveTo(at.x,c.getY()+.85-mc.player.getEyeHeight(),at.z,yaw,pitch);mc.player.yRotO=yaw;mc.player.xRotO=pitch;break;
      }
    }
    if(mc.player!=null&&mc.level!=null&&phase>=0&&phase<CASES.length&&elapsed>=0&&ticks%2==0){
      String name=String.format("02522-motion-%d-%04d-t%03d.png",phase,frame++,elapsed);
      Screenshot.grab(mc.gameDirectory,name,mc.getMainRenderTarget(),text->{});
    }
    if(phase==CASES.length){
      try{var dir=Path.of("/tmp/chao-lionfish-variants/motion");Files.createDirectories(dir);Files.writeString(dir.resolve("pass.txt"),"6 actual placed-toy sessions; four completions, two cancellations; captured frames="+frame+"\n");}catch(Exception ex){throw new RuntimeException(ex);}
      LogUtils.getLogger().info("TOY02522_MOTION_PASS frames={}",frame);mc.stop();
    }
    check(ticks<18000,"client timed out");
  }
}
