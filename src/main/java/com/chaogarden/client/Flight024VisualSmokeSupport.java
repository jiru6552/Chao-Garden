package com.chaogarden.client;

import com.chaogarden.companion.CompanionContent;
import com.chaogarden.core.ChaoState;
import com.mojang.authlib.GameProfile;
import io.netty.channel.embedded.EmbeddedChannel;
import java.util.Random;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.registration.NetworkRegistry;

/** Actual-slot flight fixtures for the excluded 0.24 integrated visual study. */
public final class Flight024VisualSmokeSupport {
  private static final UUID ID=new UUID(24,24024);
  private static long launched=-1,launchAfter;
  private static volatile int keeperId=-1,bodyId=-1;
  private static volatile long elapsed;
  private static volatile boolean activeReady,expiredReady;
  private Flight024VisualSmokeSupport(){}
  private static ServerPlayer actor(MinecraftServer server) {
    var prior=server.getPlayerList().getPlayer(ID);if(prior!=null)return prior;
    var cookie=CommonListenerCookie.createInitial(new GameProfile(ID,"FlightStudy024"),false);
    var player=new ServerPlayer(server,server.overworld(),cookie.gameProfile(),cookie.clientInformation());
    var connection=new Connection(PacketFlow.SERVERBOUND);new EmbeddedChannel(connection);
    NetworkRegistry.configureMockConnection(connection);server.getPlayerList().placeNewPlayer(connection,player,cookie);return player;
  }
  public static void prepare(ServerPlayer observer,String key) {
    if(!key.equals("finite-elytra-active")&&!key.equals("finite-elytra-expired"))throw new IllegalArgumentException(key);
    var server=observer.server;var player=actor(server);var world=server.overworld();
    if(key.equals("finite-elytra-active")||launched<0) {
      CompanionVisualSmokeSupport.clear(player);player.stopFallFlying();
      player.setGameMode(GameType.SURVIVAL);player.getAbilities().flying=false;player.getAbilities().mayfly=false;
      player.getAbilities().invulnerable=true;player.onUpdateAbilities();
      player.teleportTo(world,80,96,0,180,0);player.setNoGravity(true);player.setOnGround(false);
      player.getPersistentData().remove(CompanionContent.AIR_STARTED);player.getPersistentData().putInt(CompanionContent.AIR_USED,0);
      var state=ChaoState.egg(0,false,true,0,new Random(24024));state.adult=true;state.energy=state.hunger=100;state.levels[ChaoState.FLY]=0;
      CompanionVisualSmokeSupport.equip(player,state);launched=-1;launchAfter=server.overworld().getGameTime()+50;activeReady=expiredReady=false;
      for(int x=72;x<=88;x++)for(int z=-8;z<=8;z++) {
        world.setBlock(new BlockPos(x,90,z),Blocks.GRASS_BLOCK.defaultBlockState(),2);
        for(int y=91;y<=104;y++)world.setBlock(new BlockPos(x,y,z),Blocks.AIR.defaultBlockState(),2);
      }
    }
    observer.teleportTo(world,83.2,96.4,4.8,146,8);observer.setGameMode(GameType.SPECTATOR);
    observer.setNoGravity(true);observer.stopFallFlying();observer.setDeltaMovement(Vec3.ZERO);
    keeperId=player.getId();bodyId=-1;maintain(observer,key);
  }
  /** Camera and position are fixtures; eligibility, start and expiry are production code. */
  public static void maintain(ServerPlayer observer,String key) {
    var player=observer.server.getPlayerList().getPlayer(ID);if(player==null)return;
    player.setNoGravity(true);player.setOnGround(false);player.setShiftKeyDown(false);
    player.setPos(80,96,0);player.setYRot(180);player.yBodyRot=player.yHeadRot=180;
    player.setXRot(0);player.setDeltaMovement(0,-.08,-.1);
    var body=CompanionVisualSmokeSupport.body(player);bodyId=body==null?-1:body.getId();
    if(body==null)return;
    body.state().energy=100;
    long now=player.server.overworld().getGameTime();
    if(launched<0&&now>=launchAfter&&player.tryToStartFallFlying())launched=now;
    elapsed=launched<0?0:Math.max(0,now-launched);
    activeReady=launched>=0&&elapsed>=12&&elapsed<=28&&player.isFallFlying()&&body.isCompanionGripping();
    expiredReady=launched>=0&&elapsed>=45&&!player.isFallFlying()&&!body.isCompanionGripping()
        &&CompanionContent.glideRemaining(player,body.state())==0;
  }
  public static boolean ready(Minecraft mc,String key) {
    if(mc.level==null)return false;
    var keeper=mc.level.getEntity(keeperId);var body=mc.level.getEntity(bodyId);
    if(!(keeper instanceof Player player)||!(body instanceof com.chaogarden.entity.ChaoEntity chao))return false;
    return key.equals("finite-elytra-active")?activeReady&&player.isFallFlying()&&chao.isCompanionGripping()
        :expiredReady&&!player.isFallFlying()&&!chao.isCompanionGripping();
  }
  public static String facts(){return "keeper="+keeperId+" body="+bodyId+" FlyLevel=0 budgetTicks=40 elapsed="+elapsed+" activeReady="+activeReady+" expiredReady="+expiredReady;}
  public static void cleanup(MinecraftServer server) {
    var player=server.getPlayerList().getPlayer(ID);
    if(player!=null){CompanionVisualSmokeSupport.clear(player);server.getPlayerList().remove(player);}
    launched=-1;launchAfter=0;keeperId=bodyId=-1;elapsed=0;activeReady=expiredReady=false;
  }
}
