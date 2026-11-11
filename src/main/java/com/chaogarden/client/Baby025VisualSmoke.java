package com.chaogarden.client;

import com.chaogarden.ChaoGarden;
import com.chaogarden.core.ChaoEmotion;
import com.chaogarden.core.ChaoState;
import com.chaogarden.entity.ChaoEntity;
import com.chaogarden.item.ChaoCarrierItem;
import com.mojang.logging.LogUtils;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.screens.BackupConfirmScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderFrameEvent;

/** Opt-in timed sequences of real production actions; omitted from the release JAR. */
@EventBusSubscriber(modid=ChaoGarden.MODID,value=Dist.CLIENT)
public final class Baby025VisualSmoke {
  private static final String[] KEYS={"newborn","quiet","crawl","pet","question","meal","water","sleep","held"};
  private static final AtomicInteger SAVES=new AtomicInteger();
  private static int tick,stage=-1,preparedAt,shots,lastShotTick=-1;
  private static boolean requested,stopping,probed;
  private static volatile int ready=-1,actorId=-1;
  private static volatile long activated=-1;
  private static volatile RuntimeException failure;
  private static UUID observer;
  private static long startupWall;
  private static String startupScreen="";
  private Baby025VisualSmoke(){}

  @SubscribeEvent public static void tick(ClientTickEvent.Post event) {
    if(!Boolean.getBoolean("chao_garden.animation025Study")||stopping)return;
    var mc=Minecraft.getInstance();
    if(startupWall==0)startupWall=System.currentTimeMillis();
    if(mc.level==null||mc.player==null||mc.getSingleplayerServer()==null) {
      if(mc.screen!=null&&!mc.screen.getClass().getName().equals(startupScreen)) {
        startupScreen=mc.screen.getClass().getName();
        LogUtils.getLogger().info("CHAO_BABY025_STARTUP screen={} title={} wallClockMillis={}",
            startupScreen,mc.screen.getTitle().getString(),System.currentTimeMillis());
      }
      // The opt-in launcher always quick-plays the disposable ChaoVisual world.
      // Use only its real vanilla backup/load button, as in the 0.24 study.
      if(mc.level==null&&mc.screen instanceof BackupConfirmScreen
          &&new java.io.File(mc.gameDirectory,"saves/ChaoVisual/level.dat").isFile()) {
        String backup=Component.translatable("selectWorld.backupJoinConfirmButton").getString();
        for(var child:mc.screen.children())if(child instanceof AbstractButton button
            &&button.getMessage().getString().equals(backup)) {
          LogUtils.getLogger().info("CHAO_BABY025_FIXTURE_BACKUP {}",button.getMessage().getString());
          button.onPress();break;
        }
      }
      if(System.currentTimeMillis()-startupWall>150000)
        throw new IllegalStateException("Baby025 fixture failed to load within150 seconds; last screen="+startupScreen);
      return;
    }
    if(failure!=null)throw new IllegalStateException("Baby025 actual-trigger study failed",failure);
    if(!probed){probed=true;LogUtils.getLogger().info("CHAO_BABY025_POSES {}",Baby025VisualSmokePoseProbe.inspect());}
    tick++;if(tick==25){observer=mc.player.getUUID();advance(mc,0);}
    if(stage<0)return;
    if(tick-preparedAt>700)throw new IllegalStateException("Baby025 sequence stalled: "+KEYS[stage]);
    // F1 also hides first-person hands, so the real held-item sequence keeps
    // the ordinary HUD enabled while all world-action sequences hide it.
    mc.options.hideGui=stage!=8;mc.options.setCameraType(CameraType.FIRST_PERSON);
    mc.options.fov().set(stage==8?42:30);mc.setScreen(null);mc.getToasts().clear();
    if(ready==stage) {
      mc.getSingleplayerServer().execute(()->maintain(mc.getSingleplayerServer()));
      if(!requested&&tick-preparedAt>=25&&mc.level.getEntity(actorId) instanceof ChaoEntity) {
        requested=true;mc.getSingleplayerServer().execute(()->activate(mc.getSingleplayerServer()));
      }
    }
    if(shots==12&&SAVES.get()==stage*12+12&&tick-lastShotTick>=40) {
      LogUtils.getLogger().info("CHAO_BABY025_SEQUENCE_END scene={} gameTick={} wallClockMillis={}",
          KEYS[stage],mc.level.getGameTime(),System.currentTimeMillis());
      if(stage+1==KEYS.length) {
        stopping=true;ChaoRenderer.suppressNames=false;
        LogUtils.getLogger().info("CHAO_BABY025_COMPLETE {} timed frames wallClockMillis={}",SAVES.get(),System.currentTimeMillis());mc.stop();
      } else advance(mc,stage+1);
    }
  }

  private static void advance(Minecraft mc,int next) {
    stage=next;ready=-1;activated=-1;requested=false;shots=0;lastShotTick=-1;preparedAt=tick;
    ChaoRenderer.suppressNames=true;
    mc.getSingleplayerServer().execute(()->{
      try{prepare(mc.getSingleplayerServer());ready=next;}
      catch(RuntimeException problem){failure=problem;}
    });
  }
  private static ServerPlayer player(MinecraftServer server){return server.getPlayerList().getPlayer(observer);}
  private static ChaoEntity actor(MinecraftServer server){return server.overworld().getEntity(actorId) instanceof ChaoEntity c?c:null;}
  private static void prepare(MinecraftServer server) {
    var world=server.overworld();var p=player(server);
    for(var old:world.getEntitiesOfClass(ChaoEntity.class,new AABB(-10,60,-10,20,80,10),
        c->c.getPersistentData().getBoolean("Baby025Fixture")))old.discard();
    for(int x=-5;x<=15;x++)for(int z=-5;z<=5;z++) {
      world.setBlock(new BlockPos(x,63,z),Blocks.GRASS_BLOCK.defaultBlockState(),2);
      for(int y=64;y<=69;y++)world.setBlock(new BlockPos(x,y,z),Blocks.AIR.defaultBlockState(),2);
    }
    world.setDayTime(6000);world.setWeatherParameters(100000,0,false,false);
    if(p.serverLevel()!=world)p.teleportTo(world,2.8,63.6,3,0,0);
    p.setGameMode(GameType.SPECTATOR);p.setNoGravity(true);p.getInventory().clearContent();
    var c=ChaoGarden.CHAO.get().create(world);if(c==null)throw new IllegalStateException("No baby entity");
    var s=new ChaoState();s.hunger=80;s.energy=100;s.happiness=45;s.setEmotion(ChaoEmotion.SLEEPINESS,0);
    c.setState(s);c.owner=p.getUUID();c.setNoAi(true);c.setNoGravity(false);
    c.moveTo(.5,64,.5,0,0);c.yHeadRot=c.yHeadRotO=c.yBodyRot=c.yBodyRotO=0;
    c.getPersistentData().putBoolean("Baby025Fixture",true);world.addFreshEntity(c);actorId=c.getId();
    maintain(server);
  }
  private static void activate(MinecraftServer server) {
    try {
      var c=actor(server);var p=player(server);var world=server.overworld();
      if(c==null)throw new IllegalStateException("Missing actual baby");
      switch(stage) {
        case 0 -> c.hatchReaction(false,p);
        case 2 -> {
          c.setNoAi(false);c.goalSelector.removeAllGoals(g->true);c.targetSelector.removeAllGoals(g->true);
          c.goalSelector.addGoal(1,new Goal(){
            {setFlags(java.util.EnumSet.of(Flag.MOVE));}
            @Override public boolean canUse(){return activated<0||world.getGameTime()-activated<62;}
            @Override public boolean canContinueToUse(){return canUse();}
            @Override public void stop(){c.getNavigation().stop();}
            @Override public void tick(){c.getNavigation().moveTo(13.5,64,.5,.65);}
          });
        }
        case 3 -> {p.setItemInHand(InteractionHand.MAIN_HAND,ItemStack.EMPTY);c.mobInteract(p,InteractionHand.MAIN_HAND);}
        case 4 -> {p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Items.STICK));c.offerItem(p,InteractionHand.MAIN_HAND);}
        case 5 -> {
          c.state().hunger=0;c.sync();
          p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(ChaoGarden.item("garden_fruit")));
          c.offerItem(p,InteractionHand.MAIN_HAND);
        }
        case 6 -> {
          for(int x=-4;x<=5;x++)for(int z=-4;z<=5;z++)world.setBlock(new BlockPos(x,64,z),
              (x==-4||x==5||z==-4||z==5?Blocks.GRASS_BLOCK:Blocks.WATER).defaultBlockState(),2);
          c.setNoGravity(true);c.setPos(.5,64.75,.5);
        }
        case 7 -> {
          c.state().energy=5;c.state().sleeping=true;c.state().setEmotion(ChaoEmotion.SLEEPINESS,9500);c.sync();
        }
        case 8 -> {
          var held=new ItemStack(ChaoGarden.item("chao_carrier"));
          if(!ChaoCarrierItem.capture(held,c,p))throw new IllegalStateException("Actual held-item capture rejected");
          p.getInventory().selected=0;p.getInventory().setItem(0,held);p.setGameMode(GameType.CREATIVE);
          p.setNoGravity(true);p.teleportTo(2.8,65,3);p.setYRot(180);p.setXRot(0);
          p.connection.teleport(p.getX(),p.getY(),p.getZ(),180,0);
        }
        default -> { }
      }
      activated=world.getGameTime();
      LogUtils.getLogger().info("CHAO_BABY025_TRIGGER scene={} gameTick={} actor={} wallClockMillis={}",
          KEYS[stage],activated,actorId,System.currentTimeMillis());
    }catch(RuntimeException problem){failure=problem;}
  }
  private static void maintain(MinecraftServer server) {
    var p=player(server);if(p==null||stage==8)return;
    var c=actor(server);if(c==null)return;
    Vec3 camera=c.position().add(2.3,-.38,2.7),target=c.position().add(0,.43,0);
    p.teleportTo(camera.x,camera.y,camera.z);p.setDeltaMovement(Vec3.ZERO);
    Vec3 d=target.subtract(p.getEyePosition());
    float yaw=(float)(Math.atan2(d.z,d.x)*180/Math.PI)-90;
    float pitch=(float)(-Math.atan2(d.y,Math.hypot(d.x,d.z))*180/Math.PI);
    p.setYRot(yaw);p.setXRot(pitch);p.yHeadRot=p.yBodyRot=yaw;
    p.connection.teleport(p.getX(),p.getY(),p.getZ(),yaw,pitch);
  }

  @SubscribeEvent public static void frame(RenderFrameEvent.Post event) {
    if(!Boolean.getBoolean("chao_garden.animation025Study")||stopping||activated<0||shots>=12)return;
    var mc=Minecraft.getInstance();if(mc.level==null||mc.player==null)return;
    ChaoEntity c=stage==8?HeldChaoRenderer.preparedEntity(mc.player.getMainHandItem())
        :mc.level.getEntity(actorId) instanceof ChaoEntity found?found:null;
    if(c==null)return;
    long elapsed=mc.level.getGameTime()-activated;
    if(stage==4&&c.getItemAction()!=ChaoEntity.ItemAction.INSPECT)return;
    if(stage==5&&c.getItemAction()!=ChaoEntity.ItemAction.EAT)return;
    if(stage==6&&!c.isInWater()||stage==7&&!c.isResting())return;
    // Gifts first complete their actual consideration phase. Frame times are
    // recorded against both the server trigger and the synced action progress.
    if(elapsed<4||tick==lastShotTick)return;
    if(lastShotTick>=0&&tick-lastShotTick<5)return;
    if(stage==2&&c.walkAnimation.speed()<.006f)return;
    lastShotTick=tick;int shot=shots++;
    String name=String.format(java.util.Locale.ROOT,"chao025-baby-%s-%02d.png",KEYS[stage],shot);
    var renderer=mc.getEntityRenderDispatcher().getRenderer(c);
    String motion=renderer instanceof ChaoRenderer r?r.getModel().officialMotion().name():"unknown";
    LogUtils.getLogger().info("CHAO_BABY025_FRAME file={} triggerTick={} frameTick={} elapsed={} action={} progress={} behavior={} motion={} speed={} wallClockMillis={}",
        name,activated,mc.level.getGameTime(),elapsed,c.getItemAction(),c.getItemActionProgress(0),c.getBehavior(),motion,c.walkAnimation.speed(),System.currentTimeMillis());
    Screenshot.grab(mc.gameDirectory,name,mc.getMainRenderTarget(),message->{
      if(!(message.getContents() instanceof net.minecraft.network.chat.contents.TranslatableContents text)
          ||!text.getKey().equals("screenshot.success"))throw new IllegalStateException("Baby025 screenshot failed: "+message);
      try {
        var file=new java.io.File(new java.io.File(mc.gameDirectory,"screenshots"),name).toPath();
        String digest=java.util.HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256")
            .digest(java.nio.file.Files.readAllBytes(file)));
        LogUtils.getLogger().info("CHAO_BABY025_SAVED {} sha256={}",name,digest);SAVES.incrementAndGet();
      }catch(Exception problem){throw new IllegalStateException("Cannot verify baby sequence image "+name,problem);}
    });
  }
}
