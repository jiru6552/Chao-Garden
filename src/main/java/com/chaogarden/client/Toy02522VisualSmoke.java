package com.chaogarden.client;

import com.chaogarden.*;
import com.chaogarden.core.*;
import com.chaogarden.entity.ChaoEntity;
import com.chaogarden.toys.*;
import com.mojang.logging.LogUtils;
import java.nio.file.*;
import java.util.*;
import net.minecraft.client.*;
import net.minecraft.client.gui.screens.*;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.resolver.ServerAddress;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.*;
import net.minecraft.world.Difficulty;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.*;
import net.neoforged.neoforge.client.event.sound.PlaySoundEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/** Opt-in real two-process multiplayer play, interruption and reconnect study. Never packaged. */
@EventBusSubscriber(modid=ChaoGarden.MODID,value=Dist.CLIENT)
public final class Toy02522VisualSmoke {
  private static final Path DIR=Path.of(System.getProperty("chao_garden.toy02522Dir","/tmp/chao-lionfish-variants/visual"));
  private static String mode(){return System.getProperty("chao_garden.toy02522","");}
  private static boolean started,setup,disconnected,rejoined;private static long reconnectAt;private static int ticks,soundCount,renderFrames;
  private static volatile boolean ready;private static int phase=-1;private static long roundAt;
  private static final List<ChaoEntity> actors=new ArrayList<>();
  private static final Map<UUID,Long> starts=new HashMap<>();private static final Set<UUID> cancelled=new HashSet<>();
  private static final Set<String> shots=new HashSet<>(),seen=new HashSet<>();
  private static final Set<String> checkedItems=new HashSet<>();
  private static final Map<UUID,org.joml.Vector3f> lastArms=new HashMap<>();
  private static final Set<UUID> playingLastFrame=new HashSet<>(),recoveries=new HashSet<>();
  private static String pendingShot;private static int shotAt;
  private static void check(boolean value,String message){if(!value)throw new IllegalStateException("TOY02522 "+message);}
  private static void file(String name,String text){try{Files.createDirectories(DIR);Files.writeString(DIR.resolve(name),text);}catch(Exception e){throw new RuntimeException(e);}}
  private static void capture(String name){var mc=Minecraft.getInstance();if(shots.add(name))Screenshot.grab(mc.gameDirectory,"02522-"+mode()+"-"+name+".png",mc.getMainRenderTarget(),c->LogUtils.getLogger().info("TOY02522_CAPTURE {} {}",mode(),name));}
  private static void closeup(ChaoEntity c,String name){
    if(pendingShot!=null||shots.contains(name))return;var p=Minecraft.getInstance().player;
    var forward=net.minecraft.world.phys.Vec3.directionFromRotation(0,c.getYRot());
    var at=c.position().add(forward.scale(2.1)).add(-forward.z*.65,0,forward.x*.65);
    double eye=c.getY()+.85,dx=c.getX()-at.x,dz=c.getZ()-at.z;
    float yaw=(float)(Math.toDegrees(Math.atan2(dz,dx))-90);
    float pitch=(float)Math.toDegrees(Math.atan2(eye-(c.getY()+.38),Math.hypot(dx,dz)));
    p.moveTo(at.x,eye-p.getEyeHeight(),at.z,yaw,pitch);p.yRotO=yaw;p.xRotO=pitch;
    pendingShot=name;shotAt=ticks+2;
  }
  @SubscribeEvent public static void sound(PlaySoundEvent event){if(!mode().isEmpty()&&event.getSound()!=null&&event.getSound().getLocation().equals(ChaoSounds.RATTLE.getId()))soundCount++;}
  @SubscribeEvent public static void rendered(RenderLivingEvent.Post<?,?> event){
    if(mode().isEmpty()||!(event.getEntity() instanceof ChaoEntity c)||!c.hasCustomName()||!c.getName().getString().startsWith("P"))return;
    check(event.getRenderer().getModel() instanceof ChaoModel,"current Chao renderer/model retained");
    var model=(ChaoModel)event.getRenderer().getModel();var arm=model.officialPoses().get("arm_right");
    var angles=new org.joml.Vector3f(arm.rx,arm.ry,arm.rz);check(angles.isFinite()&&model.officialPosesFinite(),"variant pose finite");
    if(ToyContent.isPlaying(c)){
      check(model.rattlePalm()!=null,"actual variant hand socket exists "+c.getName().getString());
      playingLastFrame.add(c.getUUID());renderFrames++;
    }else if(playingLastFrame.remove(c.getUUID())&&c.getName().getString().startsWith("P1")){
      var previous=lastArms.get(c.getUUID());check(previous!=null&&previous.distance(angles)<.16f,"cancellation returns smoothly from the visible Lionfish pose");
      recoveries.add(c.getUUID());LogUtils.getLogger().info("TOY02522_RECOVERY {} delta={}",c.getName().getString(),previous.distance(angles));
    }
    lastArms.put(c.getUUID(),angles);
  }
  private static void round(net.minecraft.server.MinecraftServer server,int next){
    var level=server.overworld();actors.forEach(ChaoEntity::discard);actors.clear();starts.clear();cancelled.clear();phase=next;roundAt=level.getGameTime();
    for(int i=0;i<4;i++){
      int x=2+i*4;var pos=new BlockPos(x,80,8);String kind=i<2?RattlePlayRules.KIND:BubblePlayRules.KIND;
      level.setBlock(pos,ToyContent.block(kind).defaultBlockState(),3);
      var c=ChaoGarden.CHAO.get().create(level);
      String[][] names={{"animal_rabbit_baby","chaos_2","animal_scorpion_baby","omochao_adult"},
          {"animal_dragon_baby","tails_adult","animal_deer_baby","chaos_0"},
          {"nether_baby","adult_dark_type4","animal_mole_baby","end_adult"}};
      String variantCase=names[next][i];
      var s=com.chaogarden.test.ToyVariantCases02522.all().stream().filter(v->v.name().equals(variantCase)).findFirst().orElseThrow().state();
      LogUtils.getLogger().info("TOY02522_CASE phase={} slot={} variant={}",next,i,variantCase);
      c.setState(s);c.setInvulnerable(true);c.getRandom().setSeed(18);c.moveTo(x+.5,80,6.5,0,0);
      c.setCustomName(Component.literal("P"+next+" "+(s.adult?"Adult":"Baby")+" "+(i<2?"Rattle":"Bubbles")));
      level.addFreshEntity(c);c.goalSelector.removeAllGoals(g->true);c.targetSelector.removeAllGoals(g->true);c.goalSelector.addGoal(4,new ToyContent.PlayGoal(c));actors.add(c);
    }
    file("phase.txt",Integer.toString(phase));LogUtils.getLogger().info("TOY02522_ROUND {}",next);
  }
  @SubscribeEvent public static void server(ServerTickEvent.Post event){
    if(!mode().equals("host")||!ready)return;var server=event.getServer();var level=server.overworld();
    for(var p:server.getPlayerList().getPlayers())if(!p.getPersistentData().getBoolean("Toy02522Camera")){
      p.getPersistentData().putBoolean("Toy02522Camera",true);
      p.setGameMode(GameType.SPECTATOR);p.connection.teleport(8.5,81.1,13.8,180,12);
    }
    if(phase<0){if(server.getPlayerList().getPlayerCount()>=2)round(server,0);return;}
    if(phase==3)return;
    check(level.getGameTime()-roundAt<2400,"round timed out phase="+phase);
    for(int i=0;i<actors.size();i++){
      var c=actors.get(i);if(ToyContent.isPlaying(c))starts.putIfAbsent(c.getUUID(),c.getPersistentData().getLong("ChaoToyUntil")-160);
      Long start=starts.get(c.getUUID());if(start==null)continue;
      long elapsed=level.getGameTime()-start;
      if(phase==1&&elapsed>=60&&cancelled.add(c.getUUID())){
        if(i<2)level.destroyBlock(new BlockPos(2+i*4,80,8),true);else c.setStaying(true);
        LogUtils.getLogger().info("TOY02522_CANCEL {} elapsed={}",c.getName().getString(),elapsed);
      }
    }
    int wait=phase==1?90:190;
    if(starts.size()==4&&actors.stream().allMatch(c->!ToyContent.isPlaying(c)&&level.getGameTime()-starts.get(c.getUUID())>=wait)){
      for(int i=0;i<4;i++){
        var c=actors.get(i);check(c.state().happiness==(phase==1?20:i==0?27:24),"one correct reward "+c.getName().getString());
        check(c.getHeldChaoItem().isEmpty(),"no real hand copy");
      }
      LogUtils.getLogger().info("TOY02522_ROUND_PASS {} players={}",phase,server.getPlayerList().getPlayerCount());
      if(phase<2)round(server,phase+1);else{phase=3;file("phase.txt","3");file("server-pass.txt","3 rounds; baby/adult rattle and bubbles; cancellation; rewards; two real clients\n");}
    }
  }
  @SubscribeEvent public static void client(ClientTickEvent.Post event){
    if(mode().isEmpty())return;var mc=Minecraft.getInstance();ticks++;
    if(mc.screen instanceof AccessibilityOnboardingScreen)mc.setScreen(new TitleScreen());
    if(!started&&mc.screen instanceof TitleScreen){
      mc.options.renderDistance().set(4);mc.options.simulationDistance().set(5);mc.options.guiScale().set(2);mc.options.pauseOnLostFocus=false;mc.options.hideGui=true;
      mc.options.framerateLimit().set(30);
      if(mode().equals("host")){
        started=true;mc.createWorldOpenFlows().createFreshLevel("Toy02522-"+System.currentTimeMillis(),
          new LevelSettings("Toy02522",GameType.CREATIVE,false,Difficulty.PEACEFUL,true,new GameRules(),WorldDataConfiguration.DEFAULT),
          new net.minecraft.world.level.levelgen.WorldOptions(2521L,false,false),access->access.registryOrThrow(net.minecraft.core.registries.Registries.WORLD_PRESET)
            .getHolderOrThrow(net.minecraft.world.level.levelgen.presets.WorldPresets.FLAT).value().createWorldDimensions(),mc.screen);
      }else if(Files.exists(DIR.resolve("host-ready.txt"))&&System.currentTimeMillis()>=reconnectAt){
        started=true;ConnectScreen.startConnecting(mc.screen,mc,ServerAddress.parseString("127.0.0.1:25581"),
          new ServerData("Toy study","127.0.0.1:25581",ServerData.Type.LAN),false,null);
      }
    }
    if(mode().equals("host")&&!setup&&mc.player!=null&&mc.getSingleplayerServer()!=null){
      setup=true;mc.setScreen(null);mc.getSingleplayerServer().execute(()->{
        var server=mc.getSingleplayerServer();var l=server.overworld();
        l.setDayTime(6000);l.getGameRules().getRule(GameRules.RULE_DAYLIGHT).set(false,server);l.getGameRules().getRule(GameRules.RULE_DOMOBSPAWNING).set(false,server);
        for(var at:BlockPos.betweenClosed(-3,79,1,21,88,18))l.setBlock(at,at.getY()==79?Blocks.SMOOTH_STONE.defaultBlockState():Blocks.AIR.defaultBlockState(),3);
        check(server.publishServer(GameType.SPECTATOR,false,25581),"LAN opens");ready=true;file("host-ready.txt","25581");
      });
    }
    if(mc.player!=null&&mc.level!=null){
      if(pendingShot!=null&&ticks>=shotAt){capture(pendingShot);pendingShot=null;}
      for(var e:mc.level.entitiesForRendering())if(e instanceof ChaoEntity c&&c.hasCustomName()&&c.getName().getString().startsWith("P")){
        String name=c.getName().getString();if(!ToyContent.isPlaying(c))continue;
        int elapsed=160-c.toyPlayingTicks();
        if(checkedItems.add(c.toyPlayingKind())){
          var item=new net.minecraft.world.item.ItemStack(ChaoGarden.item(c.toyPlayingKind()));
          var model=mc.getItemRenderer().getModel(item,mc.level,c,0).applyTransform(net.minecraft.world.item.ItemDisplayContext.NONE,new com.mojang.blaze3d.vertex.PoseStack(),false);
          check(!model.getParticleIcon().contents().name().equals(net.minecraft.client.renderer.texture.MissingTextureAtlasSprite.getLocation()),"hand prop model and material bake "+c.toyPlayingKind());
        }
        if(seen.add(name))LogUtils.getLogger().info("TOY02522_CLIENT_SEES {} {} elapsed={}",mode(),name,elapsed);
        check(RattlePlayRules.KIND.equals(c.toyPlayingKind())?ChaoRattleAnimation.active(c):ChaoBubbleAnimation.active(c),"live synced hand animation "+name);
        if(elapsed>=30&&elapsed<=45)closeup(c,name.replace(' ','-')+"-play");
        if(elapsed>=90&&elapsed<=105)closeup(c,name.replace(' ','-')+"-late");
        if(mode().equals("observer")&&!disconnected&&name.startsWith("P2")&&elapsed>=35){
          disconnected=true;reconnectAt=System.currentTimeMillis()+2500;started=false;
          LogUtils.getLogger().info("TOY02522_OBSERVER_DISCONNECT elapsed={}",elapsed);mc.level.disconnect();mc.disconnect(new TitleScreen());return;
        }
        if(mode().equals("observer")&&disconnected&&!rejoined&&name.startsWith("P2 Adult")&&elapsed>40){
          rejoined=true;file("rejoin-pass.txt","First active adult phase after reconnect: "+elapsed+"\n");
          LogUtils.getLogger().info("TOY02522_OBSERVER_REJOIN {} elapsed={}",name,elapsed);
        }
      }
    }
    if(Files.exists(DIR.resolve("server-pass.txt"))&&mc.player!=null){
      if(mode().equals("observer")){
        check(disconnected&&Files.exists(DIR.resolve("rejoin-pass.txt")),"real observer rejoined active play");
        check(seen.size()>=12&&renderFrames>40&&soundCount>20,"all cases render and receive sound "+seen.size()+" / "+renderFrames+" / "+soundCount);
        check(!recoveries.isEmpty(),"observer saw a smooth interrupted recovery");
        file("observer-pass.txt","recoveries="+recoveries.size()+" seen="+seen.size()+" rendered="+renderFrames+" rattleSounds="+soundCount+"\n");
        LogUtils.getLogger().info("TOY02522_OBSERVER_PASS seen={} rendered={} sounds={}",seen.size(),renderFrames,soundCount);mc.stop();
      }else if(Files.exists(DIR.resolve("observer-pass.txt"))){
        check(renderFrames>40&&soundCount>20,"host renders and hears sessions");
        check(!recoveries.isEmpty(),"host saw a smooth interrupted recovery");
        file("host-pass.txt","recoveries="+recoveries.size()+" rendered="+renderFrames+" rattleSounds="+soundCount+"\n");
        LogUtils.getLogger().info("TOY02522_HOST_PASS rendered={} sounds={}",renderFrames,soundCount);mc.stop();
      }
    }
    check(ticks<18000,"client study timeout");
  }
}
