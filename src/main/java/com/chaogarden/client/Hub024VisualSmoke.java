package com.chaogarden.client;

import com.chaogarden.ChaoGarden;
import com.chaogarden.entity.ChaoEntity;
import com.chaogarden.hub.ChaoTeleporterBlock;
import com.chaogarden.hub.HubContent;
import com.chaogarden.hub.HubRooms;
import com.google.gson.JsonParser;
import com.mojang.logging.LogUtils;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.screens.BackupConfirmScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderFrameEvent;

/** Complete opt-in room, item, wardrobe, service and finite-flight client study.
 * Cameras, inventory contents and actors are disposable study fixtures. All
 * travel, gifts, services and flight use the production admission paths. */
@EventBusSubscriber(modid=ChaoGarden.MODID,value=Dist.CLIENT)
public final class Hub024VisualSmoke {
  private static final String[] KEYS={
      "teleporter-outside","hub-overview","hub-transporter","hub-kindergarten-door","hub-hero-stairs",
      "kindergarten-lobby","kindergarten-classroom","kindergarten-health","kindergarten-principal-room",
      "kindergarten-fortune-room","kindergarten-market","music-disc-inventory",
      "wardrobe-accept","wardrobe-reject","shell-normal","shell-shiny","shell-jewel","shell-nether","shell-end",
      "kindergarten-teacher","kindergarten-teacher-active","kindergarten-doctor","kindergarten-doctor-profile",
      "kindergarten-doctor-records","kindergarten-fortune","kindergarten-principal",
      "finite-elytra-active","finite-elytra-expired"};
  private static final Vec3[] CAMERAS={new Vec3(3.8,65.8,5.4),new Vec3(13,91,17),new Vec3(8,83,10),
      new Vec3(0,82,-9),new Vec3(6,84,16),new Vec3(0,82,12),new Vec3(-12,83,-5),
      new Vec3(12,83,-5),new Vec3(0,80.4,-16),new Vec3(12,82,2),new Vec3(-11.5,82,1)};
  private static final Vec3[] TARGETS={new Vec3(.5,65,.5),new Vec3(0,83,0),new Vec3(0,85,0),
      new Vec3(0,84.5,-22),new Vec3(18,87,8),new Vec3(0,82,-8),new Vec3(-20,81,-20),
      new Vec3(22,82,-20),new Vec3(0,80.8,-22),new Vec3(23,82,8),new Vec3(-23,82,6)};
  private static final int[] FOV={52,70,58,60,66,78,66,66,62,62,62};
  private static final AtomicInteger SAVES=new AtomicInteger();
  private static int tick,stage=-1,started,frames,captured,pressedTick;
  private static boolean shooting,stopping,probed,uiPressed;
  private static volatile int ready=-1,actorId=-1;
  private static volatile RuntimeException studyFailure;
  private static UUID observer;
  private static UUID wardrobeActor;
  private static String startupScreen="";
  private Hub024VisualSmoke(){}

  @SubscribeEvent public static void tick(ClientTickEvent.Post event) {
    if(!Boolean.getBoolean("chao_garden.hub024Study"))return;
    var mc=Minecraft.getInstance();
    if(mc.level==null&&mc.screen!=null&&!mc.screen.getClass().getName().equals(startupScreen)) {
      startupScreen=mc.screen.getClass().getName();
      LogUtils.getLogger().info("CHAO_HUB024_STARTUP {}",startupScreen);
    }
    // This opt-in harness only opens its disposable ChaoVisual test world. A
    // previous fixture version can trigger NeoForge's ordinary backup prompt.
    if(mc.level==null&&mc.screen instanceof BackupConfirmScreen) {
      String backup=Component.translatable("selectWorld.backupJoinConfirmButton").getString();
      for(var child:mc.screen.children())if(child instanceof AbstractButton button&&button.getMessage().getString().equals(backup)) {
        LogUtils.getLogger().info("CHAO_HUB024_FIXTURE_BACKUP {}",button.getMessage().getString());button.onPress();return;
      }
    }
    if(mc.level==null||mc.player==null||mc.getSingleplayerServer()==null||stopping)return;
    if(studyFailure!=null)throw new IllegalStateException("Hub024 server fixture preparation failed",studyFailure);
    if(mc.player.isDeadOrDying()){mc.player.respawn();mc.setScreen(null);return;}
    if(!probed){
      probed=true;
      LogUtils.getLogger().info("CHAO_HUB024_WARDROBE_PROBE {}",Wardrobe024VisualSmokeSupport.inspectClient());
    }
    tick++;
    if(tick==25){
      observer=mc.player.getUUID();int first=Integer.getInteger("chao_garden.hub024StartStage",0);
      if(first<0||first>=KEYS.length)throw new IllegalArgumentException("Invalid Hub024 diagnostic start stage: "+first);
      LogUtils.getLogger().info("CHAO_HUB024_START_STAGE {}",first);
      mc.getSingleplayerServer().execute(()->Kindergarten024VisualSmokeSupport.cleanup(mc.getSingleplayerServer()));
      advance(mc,first);
    }
    if(stage<0)return;
    if(tick-started>1800)throw new IllegalStateException("Hub024 study stalled: "+KEYS[stage]);
    mc.getSingleplayerServer().execute(()->maintain(mc.getSingleplayerServer()));
    mc.options.setCameraType(CameraType.FIRST_PERSON);
    mc.options.fov().set(stage<11?FOV[stage]:wardrobe()?32:flight()?40:60);
    mc.options.hideGui=!ui()&&stage!=11;
    if(!ui()&&stage!=11)mc.setScreen(null);
    mc.getToasts().clear();
    if(ready==stage&&stage==11&&tick-started>15&&!(mc.screen instanceof InventoryScreen))mc.setScreen(new InventoryScreen(mc.player));
    // A late resident snapshot may replace the first nonce while its menu is
    // being tracked. Retry the real button, retaining every server admission
    // check, until the actual owned classroom participant becomes visible.
    if(stage==20&&uiPressed&&tick-pressedTick>40&&!studentVisible(mc))uiPressed=false;
    if(ready==stage&&ui()&&Kindergarten024VisualSmokeSupport.clientReady(serviceKey())&&!uiPressed) {
      String wanted=stage==20?"LEAVE CHAO":stage==22?"PROFILE":stage==23?"RECORDS":"";
      if(!wanted.isEmpty())for(var child:mc.screen.children())if(child instanceof AbstractButton button&&button.getMessage().getString().equals(wanted)){
        LogUtils.getLogger().info("CHAO_HUB024_UI_PRESS scene={} label={} selected={}",KEYS[stage],wanted,
            mc.screen instanceof KindergartenScreen screen?screen.selectedKey():"none");
        button.onPress();uiPressed=true;pressedTick=tick;break;
      }
      if(wanted.isEmpty()){uiPressed=true;pressedTick=tick;}
    }
    if(shooting&&SAVES.get()==captured&&tick-started>30) {
      if(stage+1==KEYS.length) {
        stopping=true;ChaoRenderer.suppressNames=false;
        LogUtils.getLogger().info("CHAO_HUB024_COMPLETE {} captures / {} saves",captured,SAVES.get());
        mc.getSingleplayerServer().execute(()->Flight024VisualSmokeSupport.cleanup(mc.getSingleplayerServer()));
        mc.stop();return;
      }
      advance(mc,stage+1);
    }
  }

  private static boolean wardrobe(){return stage>=12&&stage<=18;}
  private static boolean ui(){return stage>=19&&stage<=25;}
  private static boolean flight(){return stage>=26;}
  private static String serviceKey(){return stage<=20?"kindergarten_teacher":stage<=23?"kindergarten_doctor":
      stage==24?"kindergarten_fortune":"kindergarten_principal";}

  private static void advance(Minecraft mc,int next) {
    stage=next;ready=-1;actorId=-1;started=tick;frames=0;shooting=false;uiPressed=false;pressedTick=0;
    ChaoRenderer.suppressNames=true;mc.setScreen(null);
    mc.getSingleplayerServer().execute(()->{
      try{prepare(mc.getSingleplayerServer(),next);ready=next;}
      catch(RuntimeException failure){studyFailure=failure;LogUtils.getLogger().error("CHAO_HUB024_PREPARE_FAILED {}",KEYS[next],failure);}
    });
  }

  private static ServerPlayer keeper(MinecraftServer server){return server.getPlayerList().getPlayer(observer);}
  private static void look(ServerPlayer player,Vec3 at) {
    Vec3 d=at.subtract(player.getEyePosition());float yaw=(float)(Math.atan2(d.z,d.x)*180/Math.PI)-90;
    float pitch=(float)(-Math.atan2(d.y,Math.hypot(d.x,d.z))*180/Math.PI);
    player.setYRot(yaw);player.setXRot(pitch);player.yHeadRot=player.yBodyRot=yaw;
  }
  private static void camera(ServerPlayer player,ServerLevel level,Vec3 position,Vec3 target) {
    if(player.serverLevel()!=level)player.teleportTo(level,position.x,position.y,position.z,0,0);
    player.teleportTo(position.x,position.y,position.z);player.setNoGravity(true);player.setDeltaMovement(Vec3.ZERO);
    player.stopFallFlying();player.fallDistance=0;look(player,target);
    player.connection.teleport(player.getX(),player.getY(),player.getZ(),player.getYRot(),player.getXRot());
  }
  private static void arena(ServerLevel level) {
    for(int x=-7;x<=7;x++)for(int z=-7;z<=7;z++) {
      level.setBlock(new BlockPos(x,63,z),(Math.floorMod(x+z,2)==0?Blocks.SMOOTH_QUARTZ:Blocks.LIGHT_GRAY_CONCRETE).defaultBlockState(),2);
      for(int y=64;y<=71;y++)level.setBlock(new BlockPos(x,y,z),Blocks.AIR.defaultBlockState(),2);
    }
    for(int x=-4;x<=4;x++)for(int y=64;y<=68;y++)level.setBlock(new BlockPos(x,y,-3),
        ((x+y&1)==0?Blocks.WHITE_CONCRETE:Blocks.GRAY_CONCRETE).defaultBlockState(),2);
    level.setDayTime(6000);level.setWeatherParameters(100000,0,false,false);
  }
  private static void cleanupWardrobeFixtures(ServerLevel level) {
    for(var fixture:level.getEntitiesOfClass(ChaoEntity.class,new AABB(-7,63,-7,8,72,8),
        c->c.getPersistentData().contains("Wardrobe024Scene")))fixture.discard();
    for(var fixture:level.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,new AABB(-7,63,-7,8,72,8),
        e->e.getPersistentData().contains("ChaoRejectedUntil")))fixture.discard();
  }

  private static void inspectRuntimePreset(MinecraftServer server) {
    var location=ResourceLocation.withDefaultNamespace("worldgen/world_preset/flat.json");
    try {
      var resource=server.getResourceManager().getResourceOrThrow(location);
      java.util.Set<String> keys;
      try(var reader=resource.openAsReader()) {
        keys=new java.util.TreeSet<>(JsonParser.parseReader(reader).getAsJsonObject().getAsJsonObject("dimensions").keySet());
      }
      LogUtils.getLogger().info("CHAO_HUB024_PRESET_RESOLVED resource={} source={} keys={}",location,resource.sourcePackId(),keys);
      if(!keys.equals(java.util.Set.of("minecraft:overworld","minecraft:the_nether","minecraft:the_end")))
        throw new IllegalStateException("Development flat preset leaked into normal runtime from "+resource.sourcePackId()+": "+keys);
      if(server.getLevel(HubContent.HUB)==null||server.getLevel(HubContent.KINDERGARTEN)==null)
        throw new IllegalStateException("Normal datapack dimensions were not loaded");
      LogUtils.getLogger().info("CHAO_HUB024_RUNTIME_PRESET resource={} source={} keys={} hubLoaded=true kindergartenLoaded=true",
          location,resource.sourcePackId(),keys);
    } catch(java.io.IOException failure) {throw new IllegalStateException("Cannot inspect actual runtime flat preset",failure);}
  }

  private static void prepare(MinecraftServer server,int next) {
    var player=keeper(server);if(player==null)throw new IllegalStateException("Missing study player");
    player.closeContainer();player.clearFire();player.setHealth(player.getMaxHealth());player.removeAllEffects();player.setNoGravity(true);
    if(next==0) {
      inspectRuntimePreset(server);
      CompanionVisualSmokeSupport.clear(player);player.getInventory().clearContent();
      cleanupWardrobeFixtures(server.overworld());
      camera(player,server.overworld(),CAMERAS[0],TARGETS[0]);player.setGameMode(GameType.SURVIVAL);
      arena(server.overworld());
      server.overworld().setBlock(new BlockPos(0,64,0),HubContent.TELEPORTER.get().defaultBlockState().setValue(ChaoTeleporterBlock.HALF,DoubleBlockHalf.LOWER),2);
      server.overworld().setBlock(new BlockPos(0,65,0),HubContent.TELEPORTER.get().defaultBlockState().setValue(ChaoTeleporterBlock.HALF,DoubleBlockHalf.UPPER),2);
    } else if(next==1) {
      player.setPos(.5,64.1,.5);player.setOnGround(true);HubContent.tickPlayer(player);
      if(!player.level().dimension().equals(HubContent.HUB))throw new IllegalStateException("Physical tube failed to enter hub");
      if(player.gameMode.getGameModeForPlayer()!=GameType.ADVENTURE)throw new IllegalStateException("Hub did not select adventure mode");
      camera(player,player.serverLevel(),CAMERAS[next],TARGETS[next]);
    } else if(next<5) {
      camera(player,server.getLevel(HubContent.HUB),CAMERAS[next],TARGETS[next]);
    } else if(next==5) {
      player.setPos(.5,80.1,-22);HubContent.tickPlayer(player);
      if(!player.level().dimension().equals(HubContent.KINDERGARTEN))throw new IllegalStateException("Kindergarten doorway failed");
      camera(player,player.serverLevel(),CAMERAS[next],TARGETS[next]);
    } else if(next<11) {
      camera(player,server.getLevel(HubContent.KINDERGARTEN),CAMERAS[next],TARGETS[next]);
    } else if(next==11) {
      if(!HubContent.returnHome(player))throw new IllegalStateException("Return tube journal failed");
      player.getInventory().clearContent();
      player.getInventory().setItem(0,new ItemStack(ChaoGarden.item("chao_teleporter")));
      player.getInventory().setItem(1,new ItemStack(ChaoGarden.item("chao_garden_music_disc")));
      for(int i=0;i<4;i++)player.getInventory().setItem(3+i,new ItemStack(ChaoGarden.item(new String[]{"swim_drive","fly_drive","run_drive","power_drive"}[i])));
      player.getInventory().selected=1;player.inventoryMenu.broadcastChanges();
      camera(player,server.overworld(),new Vec3(3.8,65.8,5.4),new Vec3(.5,65,.5));
    } else if(next<=18) {
      if(next==12)arena(server.overworld());
      if(wardrobeActor!=null&&server.overworld().getEntity(wardrobeActor) instanceof ChaoEntity previous)previous.discard();
      for(var item:server.overworld().getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,
          new AABB(-2,63,-2,3,67,3),e->e.getPersistentData().contains("ChaoRejectedUntil")))item.discard();
      var actor=Wardrobe024VisualSmokeSupport.prepare(player,KEYS[next],new Vec3(.5,64,.5));actorId=actor.getId();wardrobeActor=actor.getUUID();
      camera(player,server.overworld(),new Vec3(3.6,65.5,4.7),new Vec3(.5,64.65,.5));
    } else if(next<=25) {
      if(next==19&&wardrobeActor!=null&&server.overworld().getEntity(wardrobeActor) instanceof ChaoEntity previous)previous.discard();
      Kindergarten024VisualSmokeSupport.prepare(player,serviceKey());
    } else {
      Flight024VisualSmokeSupport.prepare(player,KEYS[next]);
    }
    LogUtils.getLogger().info("CHAO_HUB024_PREPARED {} dimension={} mode={}",KEYS[next],player.level().dimension().location(),player.gameMode.getGameModeForPlayer());
  }

  private static void maintain(MinecraftServer server) {
    if(stage<0||ready!=stage)return;var player=keeper(server);if(player==null)return;
    player.setNoGravity(true);player.setDeltaMovement(Vec3.ZERO);player.fallDistance=0;
    if(stage<11) {
      if(stage==0)cleanupWardrobeFixtures(server.overworld());
      player.setPos(CAMERAS[stage].x,CAMERAS[stage].y,CAMERAS[stage].z);look(player,TARGETS[stage]);
    } else if(wardrobe()) {
      if(player.serverLevel().getEntity(actorId) instanceof ChaoEntity actor)Wardrobe024VisualSmokeSupport.maintain(actor,KEYS[stage]);
    } else if(ui())Kindergarten024VisualSmokeSupport.maintain(player,serviceKey());
    else if(flight())Flight024VisualSmokeSupport.maintain(player,KEYS[stage]);
  }

  private static boolean studentVisible(Minecraft mc) {
    if(!(mc.screen instanceof KindergartenScreen screen)||!screen.serviceRole().equals("TEACHER"))return false;
    // Keeper is server-owned save data, not a client entity-data field. The
    // authorized teacher selection supplies the exact owned participant UUID.
    String selected=screen.selectedKey();
    return !mc.level.getEntitiesOfClass(ChaoEntity.class,new AABB(-26,79,-24,-10,85,-6),
        c->selected.endsWith(c.getUUID().toString())&&c.state().traitInt("lesson.remaining",0)>0).isEmpty();
  }

  private static boolean roomMeshesReady(Minecraft mc) {
    var bounds=HubRooms.bounds(stage>=5);
    for(int x=((int)bounds.minX)>>4;x<=(((int)bounds.maxX-1)>>4);x++)
      for(int z=((int)bounds.minZ)>>4;z<=(((int)bounds.maxZ-1)>>4);z++)
        if(!mc.level.getChunkSource().hasChunk(x,z))return false;
    // Receipt of block data precedes asynchronous section mesh compilation and
    // upload. Wait for both, then keep the view stable for a full frame window.
    return mc.levelRenderer.hasRenderedAllSections();
  }

  @SubscribeEvent public static void frame(RenderFrameEvent.Post event) {
    if(!Boolean.getBoolean("chao_garden.hub024Study")||stopping||shooting||stage<0||ready!=stage)return;
    var mc=Minecraft.getInstance();if(mc.level==null||mc.player==null)return;
    if(flight()) {
      if(!Flight024VisualSmokeSupport.ready(mc,KEYS[stage]))return;
    } else if(wardrobe()) {
      if(!(mc.level.getEntity(actorId) instanceof ChaoEntity actor)||!Wardrobe024VisualSmokeSupport.ready(actor,KEYS[stage]))return;
      if(tick-started<100)return;
    } else if(ui()) {
      if(!Kindergarten024VisualSmokeSupport.clientReady(serviceKey())||!uiPressed||tick-pressedTick<15)return;
      if(stage==20&&!studentVisible(mc))return;
    } else if(stage==11) {
      if(!(mc.screen instanceof InventoryScreen)||tick-started<30)return;
    } else {
      if(tick-started<50||mc.screen!=null)return;
      if(stage>=1&&stage<=4&&!mc.level.dimension().equals(HubContent.HUB))return;
      if(stage>=5&&!mc.level.dimension().equals(HubContent.KINDERGARTEN))return;
      if(stage>=1&&!roomMeshesReady(mc)){frames=0;return;}
    }
    if(++frames<(flight()?4:stage>=1&&stage<=10?40:12))return;
    if(stage==10){
      var residents=mc.level.getEntitiesOfClass(com.chaogarden.market.BlackMarketTrader.class,new AABB(-27,79,0,-16,84,11));
      if(residents.isEmpty())return;
      if(residents.size()!=1||!residents.getFirst().resident()||residents.getFirst().isPushable())
        throw new IllegalStateException("Actual client resident merchant identity/collision did not synchronize");
      LogUtils.getLogger().info("CHAO_HUB024_CLIENT_RESIDENT id={} resident={} pushable={} persistentTag={}",
          residents.getFirst().getUUID(),residents.getFirst().resident(),residents.getFirst().isPushable(),
          residents.getFirst().getPersistentData().getBoolean("ChaoKindergartenResident"));
    }
    String file="chao024-"+KEYS[stage]+".png";
    Screenshot.grab(mc.gameDirectory,file,mc.getMainRenderTarget(),message->{
      if(!(message.getContents() instanceof net.minecraft.network.chat.contents.TranslatableContents contents)
          ||!contents.getKey().equals("screenshot.success"))throw new IllegalStateException("Study screenshot was not saved: "+message.getString());
      try {
        var path=new java.io.File(new java.io.File(mc.gameDirectory,"screenshots"),file).toPath();
        String digest=java.util.HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256").digest(java.nio.file.Files.readAllBytes(path)));
        LogUtils.getLogger().info("CHAO_HUB024_SAVED {} sha256={}",file,digest);SAVES.incrementAndGet();
      }catch(Exception failure){throw new IllegalStateException("Cannot verify saved study frame "+file,failure);}
    });
    LogUtils.getLogger().info("CHAO_HUB024_CAPTURE {} stage={} dimension={} mode={} actor={} ui={} facts={}",
        file,stage,mc.level.dimension().location(),mc.gameMode.getPlayerMode(),actorId,mc.screen==null?"none":mc.screen.getClass().getSimpleName(),
        flight()?Flight024VisualSmokeSupport.facts():stage==20?"actualClassroomStudent="+studentVisible(mc)
            +" selected="+((KindergartenScreen)mc.screen).selectedKey():stage>=1&&stage<=10?
                "roomChunksReceived=true renderQueueEmpty=true "+mc.levelRenderer.getSectionStatistics():"controlled-camera");
    captured++;shooting=true;
  }
}
