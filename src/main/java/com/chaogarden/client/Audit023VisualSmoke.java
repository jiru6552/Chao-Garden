package com.chaogarden.client;

import com.chaogarden.ChaoGarden;
import com.chaogarden.companion.CompanionContent;
import com.chaogarden.core.ChaoState;
import com.chaogarden.entity.ChaoEntity;
import com.mojang.authlib.GameProfile;
import com.mojang.logging.LogUtils;
import io.netty.channel.embedded.EmbeddedChannel;
import java.util.Arrays;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.core.BlockPos;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderFrameEvent;
import net.neoforged.neoforge.network.registration.NetworkRegistry;

/** Opt-in real-slot, actual-client flight study. Never part of the release JAR. */
@EventBusSubscriber(modid=ChaoGarden.MODID,value=Dist.CLIENT)
public final class Audit023VisualSmoke {
  private static final String[] KEYS={"back-finite-normal","back-elytra-shiny","back-elytra-side",
      "back-elytra-nether","back-elytra-end","back-elytra-chaos","back-elytra-omochao",
      "back-official-comparison","back-release-land","back-release-unequip",
      "back-local-thirdperson","back-local-firstperson"};
  private static final AtomicInteger saves=new AtomicInteger();
  private static final UUID REMOTE=new UUID(23,23023);
  private static int ticks,stage=-1,started,frames,captured;
  private static boolean shooting,stopping,probed;
  private static volatile int ready=-1;
  private static volatile int keeperId=-1,bodyId=-1;
  private static long firstDraw;
  private static UUID observer;
  private static String startupScreen="";
  private Audit023VisualSmoke() {}

  @SubscribeEvent public static void tick(ClientTickEvent.Post event) {
    if(!Boolean.getBoolean("chao_garden.audit023Study"))return;
    var mc=Minecraft.getInstance();
    if(mc.level==null&&mc.screen!=null&&!mc.screen.getClass().getName().equals(startupScreen)) {
      startupScreen=mc.screen.getClass().getName();
      LogUtils.getLogger().info("CHAO_AUDIT023_STARTUP screen={} title={}",startupScreen,mc.screen.getTitle().getString());
    }
    if(mc.level==null||mc.player==null||mc.getSingleplayerServer()==null||stopping)return;
    if(mc.player.isDeadOrDying()){mc.player.respawn();mc.setScreen(null);return;}
    if(!probed){probed=true;LogUtils.getLogger().info("CHAO_AUDIT023_GRIP_POSES {}",BackGripVisualSmokePoseProbe.inspect());}
    ticks++;
    if(ticks==25){observer=mc.player.getUUID();advance(mc,0);}
    if(stage<0)return;
    if(ticks-started>900)throw new IllegalStateException("Back study stalled: "+KEYS[stage]);
    mc.getSingleplayerServer().execute(()->maintain(mc.getSingleplayerServer()));
    mc.options.hideGui=true;mc.options.fov().set(stage==2?34:stage>=10?46:36);
    mc.options.setCameraType(stage==10?CameraType.THIRD_PERSON_BACK:CameraType.FIRST_PERSON);
    if(ticks-started<3)firstDraw=CompanionBackLayer.renders();
    // A controlled interpolation gap proves lookup does not depend on the
    // projection hitbox remaining within a tiny radius during fast flight.
    if(stage==2&&mc.level.getEntity(bodyId) instanceof ChaoEntity lagged
        &&mc.level.getEntity(keeperId) instanceof Player keeper) {
      lagged.setPos(keeper.getX()+10,keeper.getY(),keeper.getZ());
    }
    mc.setScreen(null);mc.getToasts().clear();mc.particleEngine.setLevel(mc.level);
    if(shooting&&saves.get()==captured&&ticks-started>30) {
      if(stage+1==KEYS.length){
        stopping=true;ChaoRenderer.suppressNames=false;
        LogUtils.getLogger().info("CHAO_AUDIT023_COMPLETE {} captures / {} saves",captured,saves.get());
        mc.getSingleplayerServer().execute(()->{
          var remote=mc.getSingleplayerServer().getPlayerList().getPlayer(REMOTE);
          if(remote!=null){CompanionVisualSmokeSupport.clear(remote);mc.getSingleplayerServer().getPlayerList().remove(remote);}
        });
        mc.stop();return;
      }
      advance(mc,stage+1);
    }
  }

  private static void advance(Minecraft mc,int next) {
    stage=next;ready=-1;started=ticks;shooting=false;frames=0;firstDraw=CompanionBackLayer.renders();
    ChaoRenderer.suppressNames=true;
    mc.getSingleplayerServer().execute(()->{setup(mc.getSingleplayerServer(),next);ready=next;});
  }

  private static ServerPlayer remote(MinecraftServer server) {
    var existing=server.getPlayerList().getPlayer(REMOTE);if(existing!=null)return existing;
    var cookie=CommonListenerCookie.createInitial(new GameProfile(REMOTE,"BackStudy023"),false);
    var player=new ServerPlayer(server,server.overworld(),cookie.gameProfile(),cookie.clientInformation());
    var connection=new Connection(PacketFlow.SERVERBOUND);new EmbeddedChannel(connection);
    NetworkRegistry.configureMockConnection(connection);
    server.getPlayerList().placeNewPlayer(connection,player,cookie);
    return player;
  }

  private static ChaoState state(int scene) {
    boolean shiny=scene==1||scene==2||scene>=10;
    var s=ChaoState.egg(shiny?8:0,shiny,true,0,new Random(23023+scene));
    s.adult=true;s.setEvolutionStrength(1.2f);s.energy=s.hunger=100;s.happiness=s.bond=90;
    s.levels[ChaoState.FLY]=scene==0?70:99;
    if(scene==3)s.variant="nether";if(scene==4)s.variant="end";
    if(scene==5){s.immortal=true;s.align=1;}
    if(scene==6){s.character="omochao";Arrays.fill(s.grades,5);}
    return s;
  }

  private static void setup(MinecraftServer server,int next) {
    var world=server.overworld();var observerPlayer=server.getPlayerList().getPlayer(observer);
    var keeper=next>=10?observerPlayer:remote(server);
    if(next==0){
      world.getGameRules().getRule(GameRules.RULE_DOMOBSPAWNING).set(false,server);
      world.getGameRules().getRule(GameRules.RULE_RANDOMTICKING).set(0,server);
      world.setDayTime(6000);world.setWeatherParameters(100000,0,false,false);
      world.getEntitiesOfClass(Entity.class,new AABB(-24,60,-24,24,96,24),e->!(e instanceof Player)).forEach(Entity::discard);
      for(int x=-14;x<=14;x++)for(int z=-14;z<=14;z++) {
        world.setBlock(new BlockPos(x,63,z),Blocks.GRASS_BLOCK.defaultBlockState(),2);
        for(int y=64;y<=84;y++)world.setBlock(new BlockPos(x,y,z),Blocks.AIR.defaultBlockState(),2);
      }
    }
    if(next<10){
      observerPlayer.setGameMode(GameType.SPECTATOR);observerPlayer.stopFallFlying();observerPlayer.setPose(Pose.STANDING);
      observerPlayer.setNoGravity(true);observerPlayer.setDeltaMovement(Vec3.ZERO);
      if(next==2)observerPlayer.teleportTo(world,4.4,70.8,0,90,0);
      else if(next>=8)observerPlayer.teleportTo(world,3.0,65.1,-4.2,35,15);
      else observerPlayer.teleportTo(world,3.0,73.8,4.4,145,26);
    } else {
      CompanionVisualSmokeSupport.clear(remote(server));
      remote(server).teleportTo(world,10,64,10,0,0);
    }
    keeper.setGameMode(GameType.SURVIVAL);keeper.getAbilities().flying=false;keeper.getAbilities().mayfly=false;
    keeper.getAbilities().invulnerable=true;keeper.onUpdateAbilities();keeper.setNoGravity(true);
    keeper.setShiftKeyDown(false);keeper.setDeltaMovement(Vec3.ZERO);
    if(next<8||next==10){
      CompanionVisualSmokeSupport.equip(keeper,state(next));
      keeper.teleportTo(world,0,72,0,180,next>=10?10:0);keeper.setOnGround(false);
      if(next==0)keeper.stopFallFlying();else keeper.startFallFlying();
    } else if(next==8){
      keeper.stopFallFlying();keeper.setPose(Pose.STANDING);keeper.teleportTo(world,0,64,0,180,0);keeper.setOnGround(true);
      var body=CompanionVisualSmokeSupport.body(keeper);
      if(body!=null){body.setCompanionGrip(keeper,false);body.setPos(-1.2,64,.4);}
    } else if(next==9){CompanionVisualSmokeSupport.clear(keeper);keeper.stopFallFlying();keeper.setOnGround(true);}
    keeperId=keeper.getId();var body=CompanionVisualSmokeSupport.body(keeper);bodyId=body==null?-1:body.getId();
  }

  private static void maintain(MinecraftServer server) {
    if(stage<0||ready!=stage)return;
    var keeper=server.getPlayerList().getPlayer(stage>=10?observer:REMOTE);if(keeper==null)return;
    if(stage<8||stage>=10){
      keeper.setNoGravity(true);keeper.setOnGround(false);keeper.fallDistance=0;
      keeper.setPos(0,72,0);keeper.setYRot(180);keeper.yBodyRot=keeper.yHeadRot=180;
      keeper.setXRot(stage>=10?10:0);
      keeper.setDeltaMovement(0,stage==0?-.2:0,stage>=10?-.02:0);
      if(stage!=0&&!keeper.isFallFlying())keeper.startFallFlying();
      var body=CompanionVisualSmokeSupport.body(keeper);
      if(body!=null){
        body.state().energy=100;bodyId=body.getId();
        if(stage==7&&!body.officialComparison()){body.setOfficialComparison(true);body.setSourceReferenceKey("baby");}
      }
    } else {
      keeper.stopFallFlying();keeper.setPose(Pose.STANDING);keeper.setDeltaMovement(Vec3.ZERO);keeper.setOnGround(true);
      var body=CompanionVisualSmokeSupport.body(keeper);bodyId=body==null?-1:body.getId();
      if(stage==8&&body!=null){body.setPos(-1.2,64,.4);body.setDeltaMovement(Vec3.ZERO);}
    }
  }

  @SubscribeEvent public static void frame(RenderFrameEvent.Post event) {
    if(!Boolean.getBoolean("chao_garden.audit023Study")||stopping||shooting||ready!=stage||stage<0||ticks-started<25)return;
    var mc=Minecraft.getInstance();if(mc.level==null||mc.player==null)return;
    if(!(mc.level.getEntity(keeperId) instanceof Player keeper))return;
    var entity=bodyId<0?null:mc.level.getEntity(bodyId);var body=entity instanceof ChaoEntity c?c:null;
    boolean flight=stage>0&&stage<8||stage>=10;
    if(stage==9){if(body!=null||!mc.level.getEntitiesOfClass(ChaoEntity.class,keeper.getBoundingBox().inflate(4),CompanionBackLayer::attached).isEmpty())return;}
    else if(body==null||stage!=8&&!CompanionBackLayer.attached(body)||stage==8&&body.isCompanionGripping())return;
    if(flight&&(!keeper.isFallFlying()||keeper.getFallFlyingTicks()<16))return;
    if(stage==2&&body.distanceToSqr(keeper)<64)return;
    long draws=CompanionBackLayer.renders()-firstDraw;
    if(stage!=8&&stage!=9&&stage!=11&&draws==0)return;
    if(stage==11&&draws!=0)throw new IllegalStateException("First person drew the local back attachment");
    if(body!=null&&body.isCompanionGripping()){
      var renderer=mc.getEntityRenderDispatcher().getRenderer(body);
      var frustum=new net.minecraft.client.renderer.culling.Frustum(new org.joml.Matrix4f(),new org.joml.Matrix4f());
      if(renderer.shouldRender(body,frustum,0,0,0))throw new IllegalStateException("Attached body admitted duplicate world rendering");
    }
    if(++frames<4)return;
    String file="chao023-"+KEYS[stage]+".png";
    Screenshot.grab(mc.gameDirectory,file,mc.getMainRenderTarget(),msg->{saves.incrementAndGet();LogUtils.getLogger().info("CHAO_AUDIT023_SAVED {}",file);});
    LogUtils.getLogger().info("CHAO_AUDIT023_CAPTURE {} keeper={} body={} grip={} flight={} flightTicks={} backRenders={} camera={} projectionGap={}",
        file,keeperId,bodyId,body!=null&&body.isCompanionGripping(),keeper.isFallFlying(),keeper.getFallFlyingTicks(),draws,mc.options.getCameraType(),body==null?0:Math.sqrt(body.distanceToSqr(keeper)));
    captured++;shooting=true;
  }
}
