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
public final class Toy02528MotionVisualSmoke {
  private static final String[] CASES={"animal_rabbit_baby","chaos_2","animal_scorpion_baby","omochao_adult","animal_dragon_baby","tails_adult","baby_neutral","adult_neutral_type0","baby_neutral","adult_neutral_type0"};
  private static final BlockPos TOY=new BlockPos(0,80,2);
  private static boolean started,setup;private static int ticks,frame;
  private static volatile int phase=-1;private static volatile long elapsed=-1;
  private static ChaoEntity actor;private static long began=-1,spawned;private static boolean cancelled;private static int hurtFrames,expectedHappiness;
  private static boolean placed(int i){return i==0||i==1||i==4||i==6||i>=8;}
  private static boolean performing(ChaoEntity c){return ToyContent.isPlaying(c)||c.getBehavior()==ChaoEntity.Behavior.INSTRUMENT;}
  private static boolean enabled(){return Boolean.getBoolean("chao_garden.toy02528Motion");}
  private static void check(boolean ok,String why){if(!ok)throw new IllegalStateException("TOY02528_MOTION "+why);}
  private static void next(net.minecraft.server.MinecraftServer server,int index){
    if(actor!=null)actor.discard();var l=server.overworld();phase=index;began=-1;elapsed=-1;cancelled=false;expectedHappiness=20;spawned=l.getGameTime();
    var s=ToyVariantCases02522.all().stream().filter(c->c.name().equals(CASES[index])).findFirst().orElseThrow().state();
    actor=ChaoGarden.CHAO.get().create(l);s.setTraitInt("learned."+(index>=8?"tambourine":"castanets"),1);actor.setState(s);actor.setInvulnerable(true);actor.getRandom().setSeed(18);actor.moveTo(.5,80,.5,0,0);
    actor.setCustomName(Component.literal("Motion "+index));l.addFreshEntity(actor);
    actor.goalSelector.removeAllGoals(g->true);actor.targetSelector.removeAllGoals(g->true);actor.goalSelector.addGoal(4,new ToyContent.PlayGoal(actor));
    l.setBlock(TOY,placed(index)?ToyContent.block(index>=8?TambourinePlayRules.KIND:CastanetsPlayRules.KIND).defaultBlockState():Blocks.AIR.defaultBlockState(),3);
    if(!placed(index))com.chaogarden.test.InstrumentPerformanceGameTests02528.prepare(actor,"castanets");
    LogUtils.getLogger().info("TOY02528_MOTION_CASE {} {}",index,CASES[index]);
  }
  @SubscribeEvent public static void server(ServerTickEvent.Post e){
    if(!enabled()||actor==null||phase>=CASES.length)return;var l=e.getServer().overworld();
    check(l.getGameTime()-spawned<2000,"actual AI session timed out");
    if(performing(actor)&&began<0)began=placed(phase)?actor.getPersistentData().getLong("ChaoToyUntil")-160:l.getGameTime()-(int)actor.getBehaviorElapsed(0);
    if(began<0)return;elapsed=l.getGameTime()-began;
    if(phase>=4&&phase<8&&elapsed>=60&&!cancelled){
      cancelled=true;
      if(phase<6)actor.setStaying(true);
      else {
        var keeper=e.getServer().getPlayerList().getPlayers().getFirst();keeper.setGameMode(GameType.SURVIVAL);
        keeper.getInventory().clearContent();keeper.teleportTo(actor.getX()+1,actor.getY(),actor.getZ());actor.owner=keeper.getUUID();
        check(actor.mobInteract(keeper,net.minecraft.world.InteractionHand.MAIN_HAND).consumesAction(),"real pet accepted");
        check(actor.getAffectionTicks()>0,"pet overlap present");
        actor.setInvulnerable(false);check(actor.hurt(l.damageSources().generic(),1),"real damage accepted");
        expectedHappiness=actor.state().happiness;keeper.setGameMode(GameType.SPECTATOR);
      }
    }
    if(elapsed>(phase>=4&&phase<8?100:190)){
      check(!performing(actor),"session stopped");
      check(actor.state().happiness==(phase>=4&&phase<8?expectedHappiness:placed(phase)?24:20),"one completion reward; none on cancellation");
      if(placed(phase))check(!l.getBlockState(TOY).getValue(ChaoToyBlock.PLAYING),"placed source restored");
      LogUtils.getLogger().info("TOY02528_MOTION_CASE_PASS {}",phase);
      if(phase+1<CASES.length)next(e.getServer(),phase+1);else phase=CASES.length;
    }
  }
  @SubscribeEvent public static void render(net.neoforged.neoforge.client.event.RenderLivingEvent.Post<?,?> event){
    if(!enabled()||!(event.getEntity() instanceof ChaoEntity c)||(phase<6||phase>=8)||!c.hasCustomName()||!c.getName().getString().equals("Motion "+phase))return;
    if(c.hurtTime>0){
      var model=(ChaoModel)event.getRenderer().getModel();
      check(model.officialEmotion()==1&&model.eyeExpression()==2,"hurt face beats pet happiness and blink");
      check(!ChaoMaracasAnimation.happy(c,0)&&!ChaoMaracasAnimation.active(c)&&!ChaoCastanetsAnimation.happy(c,0)&&!ChaoCastanetsAnimation.active(c),"hurt suppresses toy presentation immediately");hurtFrames++;
      Screenshot.grab(Minecraft.getInstance().gameDirectory,"02528-hurt-"+phase+"-"+hurtFrames+".png",Minecraft.getInstance().getMainRenderTarget(),text->{});
    }
  }
  @SubscribeEvent public static void client(ClientTickEvent.Post e){
    if(!enabled())return;var mc=Minecraft.getInstance();ticks++;
    if(mc.screen instanceof AccessibilityOnboardingScreen)mc.setScreen(new TitleScreen());
    if(!started&&mc.screen instanceof TitleScreen){
      started=true;mc.options.renderDistance().set(3);mc.options.simulationDistance().set(5);mc.options.framerateLimit().set(30);mc.options.fov().set(42);mc.options.pauseOnLostFocus=false;mc.options.hideGui=true;
      mc.createWorldOpenFlows().createFreshLevel("Toy02528Motion-"+System.currentTimeMillis(),
          new LevelSettings("Toy02528Motion",GameType.CREATIVE,false,Difficulty.PEACEFUL,true,new GameRules(),WorldDataConfiguration.DEFAULT),
          new net.minecraft.world.level.levelgen.WorldOptions(2523L,false,false),access->access.registryOrThrow(net.minecraft.core.registries.Registries.WORLD_PRESET)
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
      String name=String.format("02528-motion-%d-%04d-t%03d.png",phase,frame++,elapsed);
      Screenshot.grab(mc.gameDirectory,name,mc.getMainRenderTarget(),text->{});
    }
    if(phase==CASES.length){
      check(hurtFrames>2,"both hurt sessions rendered");
      try{var dir=Path.of("/tmp/chao-cymbals/motion");Files.createDirectories(dir);Files.writeString(dir.resolve("pass.txt"),"10 actual AI sessions (4 placed castanets / 4 learned castanets / 2 rotated tambourine); six completions, two cancellations, two pet+hurt interruptions; captured frames="+frame+" hurtFrames="+hurtFrames+"\n");}catch(Exception ex){throw new RuntimeException(ex);}
      LogUtils.getLogger().info("TOY02528_MOTION_PASS frames={}",frame);mc.stop();
    }
    check(ticks<18000,"client timed out");
  }
}
