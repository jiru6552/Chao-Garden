package com.chaogarden.client;

import com.chaogarden.ChaoGarden;
import com.chaogarden.entity.ChaoEntity;
import com.chaogarden.test.ToyVariantCases02522;
import com.chaogarden.toys.*;
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

/** Opt-in real-client organ study. Disposable world; never packaged in the mod JAR. */
@EventBusSubscriber(modid=ChaoGarden.MODID,value=Dist.CLIENT)
public final class OrganVisualSmoke044 {
 private static final String[] CASES={"baby_neutral","adult_neutral_type0","animal_rabbit_baby","chaos_2","adult_neutral_type0","animal_rabbit_baby"};
 private static final Direction[] FACINGS={Direction.NORTH,Direction.EAST,Direction.SOUTH,Direction.WEST,Direction.NORTH,Direction.EAST};
 private static final BlockPos TOY=new BlockPos(0,80,2);
 private static boolean started,setup;private static int ticks,shots;
 private static volatile int phase=-1;private static volatile long elapsed=-1;
 private static ChaoEntity actor;private static long began=-1,spawned;private static boolean interrupted;private static int hurtFrames;
 private static boolean enabled(){return Boolean.getBoolean("chao_garden.organVisualStudy");}
 private static void next(net.minecraft.server.MinecraftServer server,int index){
  if(actor!=null){ToyPreview.stop(actor);actor.discard();}
  phase=index;elapsed=-1;began=-1;interrupted=false;var l=server.overworld();spawned=l.getGameTime();
  var state=ToyVariantCases02522.all().stream().filter(c->c.name().equals(CASES[index])).findFirst().orElseThrow().state();
  actor=ChaoGarden.CHAO.get().create(l);actor.setState(state);actor.setInvulnerable(true);actor.setNoAi(true);
  actor.getPersistentData().putLong("ChaoToyNextPlay",Long.MAX_VALUE);
  l.setBlock(TOY,ToyContent.block(OrganPlayRules.KIND).defaultBlockState().setValue(ChaoToyBlock.FACING,FACINGS[index]),3);
  var at=ToyContent.keyboardAnchor(actor,TOY);actor.moveTo(at.x,at.y,at.z,0,0);l.addFreshEntity(actor);actor.setOnGround(true);
 }
 @SubscribeEvent public static void server(ServerTickEvent.Post event){
  if(!enabled()||actor==null||phase>=CASES.length)return;var server=event.getServer();var l=server.overworld();
  if(began<0){
   actor.setOnGround(true);
   var player=server.getPlayerList().getPlayers().getFirst();
   player.teleportTo(actor.getX()+1,actor.getY()+1,actor.getZ()+1);
   try{if(server.getCommands().getDispatcher().execute("chao testtoy organ once",player.createCommandSourceStack().withPermission(2))!=1)throw new IllegalStateException("organ native command rejected");}
   catch(Exception e){throw new RuntimeException(e);}began=l.getGameTime();
   LogUtils.getLogger().info("ORGAN044_VISUAL_CASE {} {} facing={}",phase,CASES[phase],FACINGS[phase]);
  }
  if(l.getGameTime()-spawned>600)throw new IllegalStateException("organ client case timed out");
  if(began<0)return;elapsed=l.getGameTime()-began;
  if(phase>=4&&elapsed>=60&&!interrupted){
   interrupted=true;
   if(phase==4){actor.setInvulnerable(false);if(!actor.hurt(l.damageSources().generic(),1))throw new IllegalStateException("client damage was rejected");}
   else actor.setPos(actor.getX()+3,actor.getY(),actor.getZ());
  }
  if(elapsed>(phase>=4?110:180)){
   if(ToyPreview.active(actor)||actor.toyTarget()!=null)throw new IllegalStateException("organ client session failed cleanup");
   if(phase+1<CASES.length)next(server,phase+1);else phase=CASES.length;
  }
 }
 @SubscribeEvent public static void rendered(net.neoforged.neoforge.client.event.RenderLivingEvent.Post<?,?> event){
  if(!enabled()||phase!=4||!(event.getEntity() instanceof ChaoEntity c)||c.hurtTime<=0)return;
  var model=(ChaoModel)event.getRenderer().getModel();
  if(model.officialEmotion()!=1||model.eyeExpression()!=2||ChaoOrganAnimation.active(c))throw new IllegalStateException("hurt must replace organ pose and happy face");
  hurtFrames++;
 }
 @SubscribeEvent public static void client(ClientTickEvent.Post event){
  if(!enabled())return;var mc=Minecraft.getInstance();ticks++;
  if(mc.screen instanceof AccessibilityOnboardingScreen)mc.setScreen(new TitleScreen());
  if(!started&&mc.screen instanceof TitleScreen){
   started=true;mc.options.renderDistance().set(3);mc.options.simulationDistance().set(5);mc.options.framerateLimit().set(30);mc.options.fov().set(45);mc.options.pauseOnLostFocus=false;mc.options.hideGui=true;
   mc.createWorldOpenFlows().createFreshLevel("Organ044-"+System.currentTimeMillis(),new LevelSettings("Organ044",GameType.CREATIVE,false,Difficulty.PEACEFUL,true,new GameRules(),WorldDataConfiguration.DEFAULT),new net.minecraft.world.level.levelgen.WorldOptions(2544L,false,false),a->a.registryOrThrow(net.minecraft.core.registries.Registries.WORLD_PRESET).getHolderOrThrow(net.minecraft.world.level.levelgen.presets.WorldPresets.FLAT).value().createWorldDimensions(),mc.screen);
  }
  if(!setup&&mc.player!=null&&mc.getSingleplayerServer()!=null){
   setup=true;mc.setScreen(null);ChaoRenderer.suppressNames=true;
   mc.getSingleplayerServer().execute(()->{
    var server=mc.getSingleplayerServer();var l=server.overworld();l.setDayTime(6000);l.getGameRules().getRule(GameRules.RULE_DAYLIGHT).set(false,server);l.getGameRules().getRule(GameRules.RULE_DOMOBSPAWNING).set(false,server);
    for(var p:BlockPos.betweenClosed(-7,79,-7,7,85,9))l.setBlock(p,p.getY()==79?Blocks.SMOOTH_STONE.defaultBlockState():Blocks.AIR.defaultBlockState(),3);
    server.getPlayerList().getPlayers().forEach(p->p.setGameMode(GameType.SPECTATOR));next(server,0);
   });
  }
  if(mc.level!=null&&mc.player!=null&&phase>=0&&phase<CASES.length){
   for(var e:mc.level.entitiesForRendering())if(e instanceof ChaoEntity c){
    var forward=net.minecraft.world.phys.Vec3.directionFromRotation(0,c.getYRot());
    int view=phase>=4?1:elapsed<64?0:elapsed<120?1:2;
    var at=c.position().add(forward.scale(view==0?-1.8:view==1?.1:-.7)).add(-forward.z*(view==0?.8:view==1?2.0:-1.5),.85,forward.x*(view==0?.8:view==1?2.0:-1.5));
    var focus=c.position().add(forward.scale(.25)).add(0,.40,0);
    float yaw=(float)(Math.toDegrees(Math.atan2(focus.z-at.z,focus.x-at.x))-90);
    float pitch=(float)-Math.toDegrees(Math.atan2(focus.y-at.y,Math.hypot(focus.x-at.x,focus.z-at.z)));
    mc.player.moveTo(at.x,at.y-mc.player.getEyeHeight(),at.z,yaw,pitch);mc.player.yRotO=yaw;mc.player.xRotO=pitch;break;
   }
   if(elapsed>=0&&ticks%4==0){Screenshot.grab(mc.gameDirectory,String.format("organ044-final-%d-t%03d-%04d.png",phase,elapsed,shots++),mc.getMainRenderTarget(),text->{});}
  }
  if(phase==CASES.length){if(hurtFrames==0)throw new IllegalStateException("no hurt frames rendered");LogUtils.getLogger().info("ORGAN044_VISUAL_COMPLETE shots={} hurtFrames={}",shots,hurtFrames);mc.stop();}
  if(ticks>9000)throw new IllegalStateException("organ client study timed out");
 }
}
