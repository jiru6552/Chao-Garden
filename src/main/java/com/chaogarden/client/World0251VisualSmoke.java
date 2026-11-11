package com.chaogarden.client;

import com.chaogarden.ChaoGarden;
import com.chaogarden.hub.GardenRooms025;
import com.chaogarden.hub.HubContent;
import com.chaogarden.hub.HubRooms;
import com.chaogarden.hub.HubWorldData;
import com.chaogarden.kindergarten.KindergartenContent;
import com.chaogarden.kindergarten.KindergartenNpc;
import com.chaogarden.kindergarten.KindergartenDecor025;
import com.chaogarden.kindergarten.KindergartenRooms025;
import com.chaogarden.kindergarten.KindergartenDecor025.SchoolDoorBlock;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.logging.LogUtils;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.screens.BackupConfirmScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundBlockChangedAckPacket;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderFrameEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

/** Disposable 0.25.1 ordinary-client scenery and genuine keyboard travel study.
 * Excluded from production JARs. Plan/cameras are reviewed outside the app; no
 * scene geometry, portal latches, gameplay permissions or creature state changes.
 * Moving phases use the real local player's forward input and normal networking.
 */
@EventBusSubscriber(modid=ChaoGarden.MODID,value=Dist.CLIENT)
public final class World0251VisualSmoke {
  private record Scene(String key,String space,Vec3 eye,Vec3 target,int fov,boolean motion,String walkTo,int holdTicks,BlockPos door,boolean expectOpen,boolean denied,String probe,boolean serviceGesture) {
    boolean walk(){return !walkTo.isEmpty();}
    boolean action(){return door!=null;}
    int images(){return motion||walk()?2:1;}
  }
  private static List<Scene> scenes;
  private static final AtomicInteger SAVED=new AtomicInteger();
  private static final AtomicInteger ACK_RECEIPTS=new AtomicInteger();
  private static volatile int latestNativeAck;
  private static volatile boolean ackObserverReady,actionDeniedByServer;
  private static volatile int actionSequence;
  private static int ackBaseline,doorRenderFrames,predictedMaskValue,predictionTick;
  private static long predictionWall;
  private static boolean predictedAllSix,predictionLogged,ackVerified,rollbackVerified;
  private static int tick,stage=-1,started,stable,images,captured,firstTick,endTick=-1,lastTrace;
  private static int probeFrames,probeGestureFrames;
  private static int hubTravelFloorSamples;
  private static double minHubTravelFeetY;
  private static float probeFirstAge,probeLastAge,probeMinScale,probeMaxScale;
  private static UUID probeActor;
  private static volatile int ready=-1;
  private static volatile RuntimeException failure;
  private static volatile boolean actionServerVerified;
  private static boolean moving,arrived,stopping,configured,begun,actionSent,actionVerified,serviceRequested,serviceUiVerified;
  private static UUID observer;
  private static long startupWall;
  private static String startupScreen="";
  private World0251VisualSmoke(){}
  private static boolean enabled(){return Boolean.getBoolean("chao_garden.world0251Study");}
  private static Vec3 vector(JsonArray a){return new Vec3(a.get(0).getAsDouble(),a.get(1).getAsDouble(),a.get(2).getAsDouble());}
  private static void loadPlan(Minecraft mc){
    if(Boolean.getBoolean("chao_garden.world025Study")||Boolean.getBoolean("chao_garden.animation025Study"))throw new IllegalStateException("Enable only one actual client study");
    Path file=mc.gameDirectory.toPath().toAbsolutePath().normalize().getParent().resolve("docs/release-scenes-0.25.1.json");
    try(var reader=Files.newBufferedReader(file)){
      var plan=JsonParser.parseReader(reader).getAsJsonObject();if(!plan.get("release").getAsString().equals("0.25.1"))throw new IllegalArgumentException("Wrong0251plan release");
      var parsed=new ArrayList<Scene>();var keys=new java.util.HashSet<String>();
      for(var item:plan.getAsJsonArray("scenes")){
        JsonObject row=item.getAsJsonObject();String key=row.get("key").getAsString();if(!key.matches("[a-z0-9-]+")||!keys.add(key))throw new IllegalArgumentException("Invalid/duplicate0251scene "+key);
        BlockPos door=null;if(row.has("action")){
          if(!java.util.Set.of("door-toggle","door-denied-toggle").contains(row.get("action").getAsString()))throw new IllegalArgumentException("Unknown0251native action");
          var at=row.getAsJsonArray("block");door=new BlockPos(at.get(0).getAsInt(),at.get(1).getAsInt(),at.get(2).getAsInt());
          if(!row.has("motion")||!row.get("motion").getAsBoolean()||row.has("walk_to"))throw new IllegalArgumentException("0251door actions require a stationary live before/after pair");
        }
        parsed.add(new Scene(key,row.get("space").getAsString(),vector(row.getAsJsonArray("eye")),vector(row.getAsJsonArray("target")),row.get("fov").getAsInt(),row.has("motion")&&row.get("motion").getAsBoolean(),row.has("walk_to")?row.get("walk_to").getAsString():"",row.has("hold_ticks")?row.get("hold_ticks").getAsInt():60,door,row.has("expect_open")&&row.get("expect_open").getAsBoolean(),row.has("action")&&row.get("action").getAsString().equals("door-denied-toggle"),row.has("probe")?row.get("probe").getAsString():"",row.has("service_gesture")&&row.get("service_gesture").getAsBoolean()));
      }
      if(parsed.isEmpty())throw new IllegalArgumentException("Empty0251scene plan");scenes=List.copyOf(parsed);
      String digest=java.util.HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(file)));
      LogUtils.getLogger().info("CHAO_WORLD0251_PLAN scenes={} expectedFrames={} sha256={}",scenes.size(),scenes.stream().mapToInt(Scene::images).sum(),digest);
    }catch(Exception e){throw new IllegalStateException("Cannot load reviewed0251plan "+file,e);}
  }
  private static Scene scene(){return scenes.get(stage);}
  private static void observeNativeAcks(Minecraft mc){
    var channel=mc.getConnection().getConnection().channel();
    channel.eventLoop().execute(()->{
      if(channel.pipeline().get("packet_handler")==null){failure=new IllegalStateException("0251actual client packet handler is missing");return;}
      channel.pipeline().addBefore("packet_handler","chao0251_read_only_ack",new io.netty.channel.ChannelDuplexHandler(){
        @Override public void channelRead(io.netty.channel.ChannelHandlerContext context,Object message)throws Exception{recordNativeAck(message);super.channelRead(context,message);}
        @Override public void write(io.netty.channel.ChannelHandlerContext context,Object message,io.netty.channel.ChannelPromise promise)throws Exception{
          if(message instanceof net.minecraft.network.protocol.game.ServerboundUseItemOnPacket packet&&stage>=0&&scene().action()&&actionSent&&packet.getHitResult().getBlockPos().equals(scene().door())){
            actionSequence=packet.getSequence();LogUtils.getLogger().info("CHAO_WORLD0251_ACTION_PACKET scene={} sequence={} actualNativePacket=true readOnlyObserver=true wallClockMillis={}",scene().key(),actionSequence,System.currentTimeMillis());
          }super.write(context,message,promise);
        }
      });ackObserverReady=true;
    });
  }
  private static void recordNativeAck(Object packet){
    if(packet instanceof ClientboundBlockChangedAckPacket ack){latestNativeAck=ack.sequence();ACK_RECEIPTS.incrementAndGet();LogUtils.getLogger().info("CHAO_WORLD0251_NATIVE_ACK sequence={} wallClockMillis={} readOnlyObserver=true",ack.sequence(),System.currentTimeMillis());}
    else if(packet instanceof net.minecraft.network.protocol.BundlePacket<?> bundle)for(var nested:bundle.subPackets())recordNativeAck(nested);
  }
  private static KindergartenRooms025.DoorPair doorPair(Scene scene){
    for(var pair:KindergartenRooms025.doorPairs())for(var base:List.of(pair.leftBase(),pair.rightBase()))for(int i=0;i<3;i++)if(base.above(i).equals(scene.door()))return pair;
    throw new IllegalStateException("0251native action is outside canonical door assemblies: "+scene.key());
  }
  private static int doorMask(Level level,Scene scene){
    var pair=doorPair(scene);var block=KindergartenDecor025.block(pair.id());int mask=0,index=0;
    for(var hinge:net.minecraft.world.level.block.state.properties.DoorHingeSide.values())for(int i=0;i<3;i++){
      var at=(hinge==net.minecraft.world.level.block.state.properties.DoorHingeSide.LEFT?pair.leftBase():pair.rightBase()).above(i);var state=level.getBlockState(at);
      if(!state.is(block)||state.getValue(SchoolDoorBlock.SEGMENT)!=i||state.getValue(SchoolDoorBlock.HINGE)!=hinge||state.getValue(SchoolDoorBlock.FACING)!=pair.facing())throw new IllegalStateException("0251native door assembly changed identity/hinge/segment: "+at+" "+state);
      if(state.getValue(SchoolDoorBlock.OPEN))mask|=1<<index;index++;
    }return mask;
  }
  @SubscribeEvent(priority=EventPriority.LOWEST) public static void denyStudyDoor(PlayerInteractEvent.RightClickBlock event){
    if(!enabled()||stage<0||ready!=stage||!scene().denied()||!actionSent||event.getLevel().isClientSide||event.getHand()!=InteractionHand.MAIN_HAND||!(event.getEntity() instanceof ServerPlayer p)||!p.getUUID().equals(observer)||!event.getPos().equals(scene().door()))return;
    if(doorMask(p.serverLevel(),scene())!=(scene().expectOpen()?63:0)||p.gameMode.getGameModeForPlayer()!=GameType.ADVENTURE||p.getAbilities().mayBuild)throw new IllegalStateException("0251denial fixture found unexpected authoritative state/permissions");
    event.setCanceled(true);event.setCancellationResult(net.minecraft.world.InteractionResult.FAIL);actionDeniedByServer=true;
    LogUtils.getLogger().info("CHAO_WORLD0251_ACTION_DENIED scene={} serverMask={} parts=6 nativeEvent=true mode=ADVENTURE mayBuild=false wallClockMillis={}",scene().key(),doorMask(p.serverLevel(),scene()),System.currentTimeMillis());
  }
  private static ResourceKey<Level> dimension(String space){return switch(space){case "hub"->HubContent.HUB;case "garden"->HubContent.GARDEN;case "school"->HubContent.KINDERGARTEN;default->throw new IllegalArgumentException(space);};}
  private static AABB bounds(String space){return switch(space){case "hub"->HubRooms.HUB_BOUNDS;case "garden"->GardenRooms025.BOUNDS;case "school"->HubRooms.KINDERGARTEN_BOUNDS;default->throw new IllegalArgumentException(space);};}
  private static ServerPlayer player(MinecraftServer server){return server.getPlayerList().getPlayer(observer);}
  private static String currentSpace(){return scene().walk()&&arrived?scene().walkTo():scene().space();}
  private static void input(Minecraft mc,boolean forward){mc.options.keyUp.setDown(forward);mc.options.keyDown.setDown(false);mc.options.keyLeft.setDown(false);mc.options.keyRight.setDown(false);mc.options.keyJump.setDown(false);mc.options.keyShift.setDown(false);mc.options.keySprint.setDown(false);}
  @SubscribeEvent public static void tick(ClientTickEvent.Post event){
    if(!enabled()||stopping)return;var mc=Minecraft.getInstance();if(startupWall==0)startupWall=System.currentTimeMillis();
    if(mc.level==null||mc.player==null||mc.getSingleplayerServer()==null){
      if(mc.screen!=null&&!mc.screen.getClass().getName().equals(startupScreen)){
        startupScreen=mc.screen.getClass().getName();LogUtils.getLogger().info("CHAO_WORLD0251_STARTUP screen={} title={} wallClockMillis={}",startupScreen,mc.screen.getTitle().getString(),System.currentTimeMillis());
      }
      if(mc.level==null&&mc.screen instanceof BackupConfirmScreen&&new java.io.File(mc.gameDirectory,"saves/ChaoVisual/level.dat").isFile()){
        String backup=Component.translatable("selectWorld.backupJoinConfirmButton").getString();for(var c:mc.screen.children())if(c instanceof AbstractButton b&&b.getMessage().getString().equals(backup)){b.onPress();break;}
      }
      if(System.currentTimeMillis()-startupWall>150000)throw new IllegalStateException("0251disposable client startup deadline; last="+startupScreen);return;
    }
    if(failure!=null)throw new IllegalStateException("0251native study failed",failure);
    if(!configured){configured=true;loadPlan(mc);observeNativeAcks(mc);mc.options.renderDistance().set(12);mc.options.entityDistanceScaling().set(1.5);mc.options.pauseOnLostFocus=false;}
    tick++;if(tick==25){observer=mc.player.getUUID();advance(mc,0);}if(stage<0)return;
    if(tick-started>1400)throw new IllegalStateException("0251scene deadline "+scene().key()+" ready="+ready+" moving="+moving+" arrived="+arrived+" dimension="+mc.level.dimension().location());
    if(scene().serviceGesture()&&serviceRequested&&mc.screen instanceof KindergartenScreen screen&&screen.serviceRole().equals("FORTUNE")){
      serviceUiVerified=true;LogUtils.getLogger().info("CHAO_WORLD0251_FORTUNE_UI scene={} role=FORTUNE nativeInteraction=true wallClockMillis={}",scene().key(),System.currentTimeMillis());screen.onClose();
    }else mc.setScreen(null);
    mc.options.hideGui=true;mc.options.setCameraType(CameraType.FIRST_PERSON);mc.options.fov().set(scene().fov());mc.getToasts().clear();
    if(ready!=stage)return;
    if(scene().serviceGesture()&&begun&&!serviceRequested&&tick-firstTick>=180){
      var staff=mc.level.getEntitiesOfClass(KindergartenNpc.class,new AABB(KindergartenContent.Role.FORTUNE.anchor).inflate(4),n->n.role()==KindergartenContent.Role.FORTUNE);
      if(staff.size()!=1)throw new IllegalStateException("0251native Fortune service target is not unique");
      serviceRequested=true;var result=mc.gameMode.interact(mc.player,staff.getFirst(),InteractionHand.MAIN_HAND);
      LogUtils.getLogger().info("CHAO_WORLD0251_FORTUNE_INTERACT scene={} actor={} result={} input=native-client-entity-interact wallClockMillis={} clientTick={}",scene().key(),staff.getFirst().getUUID(),result,System.currentTimeMillis(),tick);
    }
    if(scene().action()&&begun&&!actionSent){
      if(!mc.player.getMainHandItem().isEmpty())throw new IllegalStateException("0251native door click requires the actual empty main hand");
      if(!ackObserverReady)throw new IllegalStateException("0251native sequence-ack observer was not installed");
      actionSent=true;
      ackBaseline=ACK_RECEIPTS.get();
      var result=mc.gameMode.useItemOn(mc.player,InteractionHand.MAIN_HAND,new BlockHitResult(Vec3.atCenterOf(scene().door()),Direction.SOUTH,scene().door(),false));
      int predictedMask=doorMask(mc.level,scene());int expectedMask=(scene().denied()?!scene().expectOpen():scene().expectOpen())?63:0;
      if(predictedMask!=expectedMask)throw new IllegalStateException("0251ordinary native prediction did not update all six parts: "+scene().key()+" mask="+predictedMask+" expected="+expectedMask);
      predictedAllSix=true;predictedMaskValue=predictedMask;predictionWall=System.currentTimeMillis();predictionTick=tick;
      LogUtils.getLogger().info("CHAO_WORLD0251_ACTION_SENT scene={} action=native-right-click-door expectedOpen={} block={} result={} wallClockMillis={} clientTick={}",scene().key(),scene().expectOpen(),scene().door(),result,System.currentTimeMillis(),tick);
    }
    if(scene().action()&&actionSent&&!actionVerified){
      if(!predictionLogged&&actionSequence>=0){predictionLogged=true;LogUtils.getLogger().info("CHAO_WORLD0251_ACTION_PREDICTED scene={} mask={} parts=6 sequence={} denied={} wallClockMillis={} clientTick={}",scene().key(),predictedMaskValue,actionSequence,scene().denied(),predictionWall,predictionTick);}
      final int actionStage=stage;final Scene actionScene=scene();
      mc.getSingleplayerServer().execute(()->{
        if(stage!=actionStage||ready!=actionStage||actionServerVerified||actionScene.denied()&&!actionDeniedByServer)return;
        var p=player(mc.getSingleplayerServer());var actual=p.serverLevel().getBlockState(actionScene.door());
        if(actual.hasProperty(BlockStateProperties.OPEN)&&actual.getValue(BlockStateProperties.OPEN)==actionScene.expectOpen()&&doorMask(p.serverLevel(),actionScene)==(actionScene.expectOpen()?63:0)){
          if(p.gameMode.getGameModeForPlayer()!=GameType.ADVENTURE||p.getAbilities().mayBuild){failure=new IllegalStateException("0251door action leaked Adventure build permission");return;}
          actionServerVerified=true;LogUtils.getLogger().info("CHAO_WORLD0251_ACTION_SERVER scene={} actualOpen={} mode=ADVENTURE mayBuild=false wallClockMillis={}",actionScene.key(),actual.getValue(BlockStateProperties.OPEN),System.currentTimeMillis());
        }
      });
      if(!ackVerified&&actionSequence>=0&&ACK_RECEIPTS.get()>ackBaseline&&latestNativeAck>=actionSequence){ackVerified=true;LogUtils.getLogger().info("CHAO_WORLD0251_ACTION_ACK scene={} sequence={} incomingSequence={} actualNativePacket=true wallClockMillis={} clientTick={}",scene().key(),actionSequence,latestNativeAck,System.currentTimeMillis(),tick);}
      var state=mc.level.getBlockState(scene().door());
      if(actionServerVerified&&ackVerified&&tick-firstTick>=8&&state.hasProperty(BlockStateProperties.OPEN)&&state.getValue(BlockStateProperties.OPEN)==scene().expectOpen()&&doorMask(mc.level,scene())==(scene().expectOpen()?63:0)){
        if(scene().denied()){rollbackVerified=true;LogUtils.getLogger().info("CHAO_WORLD0251_ACTION_ROLLBACK scene={} parts=6 mask={} sequence={} serverCanceled=true nativeAck=true forcedClientWrites=false wallClockMillis={} clientTick={}",scene().key(),doorMask(mc.level,scene()),actionSequence,System.currentTimeMillis(),tick);}
        actionVerified=true;LogUtils.getLogger().info("CHAO_WORLD0251_ACTION_VERIFIED scene={} action=native-right-click-door actualOpen={} mode={} wallClockMillis={} clientTick={}",scene().key(),state.getValue(BlockStateProperties.OPEN),mc.gameMode.getPlayerMode(),System.currentTimeMillis(),tick);
      }else if(tick-firstTick>100)throw new IllegalStateException("0251native door click was denied or state did not synchronize: "+scene().key()+" state="+state);
    }
    if(scene().walk()&&begun&&!moving&&!arrived){
      moving=true;stable=0;mc.getSingleplayerServer().execute(()->{var p=player(mc.getSingleplayerServer());p.setNoGravity(false);p.setDeltaMovement(Vec3.ZERO);});
      LogUtils.getLogger().info("CHAO_WORLD0251_TRAVEL_BEGIN scene={} from={} to={} input=vanilla-key-forward wallClockMillis={} clientTick={} position={}",scene().key(),scene().space(),scene().walkTo(),System.currentTimeMillis(),tick,mc.player.position());
    }
    if(moving){
      if(mc.level.dimension().equals(HubContent.HUB)){
        minHubTravelFeetY=Math.min(minHubTravelFeetY,mc.player.getY());hubTravelFloorSamples++;
        if(mc.player.getY()<HubRooms.FLOOR_Y+.95)throw new IllegalStateException("0251ordinary portal travel fell below the actual Hub floor: scene="+scene().key()+" feet="+mc.player.position()+" minimum="+minHubTravelFeetY+" floorTop="+(HubRooms.FLOOR_Y+1));
      }
      input(mc,true);
      if(mc.level.dimension().equals(dimension(scene().walkTo()))){
        input(mc,false);moving=false;arrived=true;stable=0;mc.player.setDeltaMovement(Vec3.ZERO);
        mc.getSingleplayerServer().execute(()->{var p=player(mc.getSingleplayerServer());if(!p.level().dimension().equals(dimension(scene().walkTo()))||p.gameMode.getGameModeForPlayer()!=GameType.ADVENTURE||p.getAbilities().mayBuild)failure=new IllegalStateException("Genuine0251travel did not retain Adventure restriction");p.setNoGravity(true);p.setDeltaMovement(Vec3.ZERO);});
        LogUtils.getLogger().info("CHAO_WORLD0251_TRAVEL_ARRIVED scene={} dimension={} mode={} wallClockMillis={} clientTick={} position={}",scene().key(),mc.level.dimension().location(),mc.gameMode.getPlayerMode(),System.currentTimeMillis(),tick,mc.player.position());
      }else if(!mc.level.dimension().equals(dimension(scene().space())))throw new IllegalStateException("0251walk entered unexpected realm "+mc.level.dimension().location());
      if(tick-lastTrace>=10){lastTrace=tick;LogUtils.getLogger().info("CHAO_WORLD0251_TRAVEL_TRACE scene={} dimension={} clientTick={} position={} velocity={}",scene().key(),mc.level.dimension().location(),tick,mc.player.position(),mc.player.getDeltaMovement());}
    }else input(mc,false);
    if(!scene().walk())mc.getSingleplayerServer().execute(()->maintain(mc.getSingleplayerServer()));
    if(images==scene().images()&&SAVED.get()==captured){
      if(endTick<0)endTick=tick;
      if(tick-endTick>=20){
        if(scene().action()){
          if(!predictedAllSix||!predictionLogged||!ackVerified||doorRenderFrames<20||scene().denied()&&!rollbackVerified)throw new IllegalStateException("0251incomplete actual paired-door prediction/reconciliation proof");
          LogUtils.getLogger().info("CHAO_WORLD0251_DOOR_RENDER_SUMMARY scene={} frames={} mixedFrames=0 parts=6 denied={} nativeAck=true rollback={} wallClockMillis={}",scene().key(),doorRenderFrames,scene().denied(),rollbackVerified,System.currentTimeMillis());
        }
        if(scene().walk()){
          if(hubTravelFloorSamples<1)throw new IllegalStateException("0251actual Hub travel had no physical floor samples");
          LogUtils.getLogger().info("CHAO_WORLD0251_TRAVEL_FLOOR scene={} minHubFeetY={} hubFloorTop={} samples={} admissible=true movement=ordinary-local-player",scene().key(),minHubTravelFeetY,HubRooms.FLOOR_Y+1,hubTravelFloorSamples);
        }
        if(scene().probe().equals("fortune-lids")){
          if(probeFrames<100||probeLastAge-probeFirstAge<346)throw new IllegalStateException("0251Fortune native rendered-pose probe did not span two original173tick blink cycles: frames="+probeFrames+" ages="+probeFirstAge+".."+probeLastAge);
          if(scene().serviceGesture()&&(!serviceUiVerified||probeGestureFrames<10))throw new IllegalStateException("0251Fortune service gesture was not genuinely witnessed: ui="+serviceUiVerified+" gestureFrames="+probeGestureFrames);
          LogUtils.getLogger().info("CHAO_WORLD0251_FORTUNE_PROBE scene={} actor={} frames={} firstRenderAge={} lastRenderAge={} minScale={} maxScale={} actualMeshSubmitted=true gestureFrames={} serviceUiVerified={}",scene().key(),probeActor,probeFrames,probeFirstAge,probeLastAge,probeMinScale,probeMaxScale,probeGestureFrames,serviceUiVerified);
        }
        LogUtils.getLogger().info("CHAO_WORLD0251_SCENE_END scene={} wallClockMillis={} clientTick={} dimension={}",scene().key(),System.currentTimeMillis(),tick,mc.level.dimension().location());
        if(stage+1<scenes.size())advance(mc,stage+1);else complete(mc);
      }
    }
  }
  private static void advance(Minecraft mc,int next){
    input(mc,false);stage=next;started=tick;stable=0;images=0;firstTick=0;endTick=-1;ready=-1;moving=false;arrived=false;begun=false;actionSent=false;actionVerified=false;actionServerVerified=false;
    probeFrames=0;probeGestureFrames=0;serviceRequested=false;serviceUiVerified=false;probeFirstAge=Float.NaN;probeLastAge=Float.NaN;probeMinScale=Float.POSITIVE_INFINITY;probeMaxScale=Float.NEGATIVE_INFINITY;probeActor=null;
    hubTravelFloorSamples=0;minHubTravelFeetY=Double.POSITIVE_INFINITY;
    actionDeniedByServer=false;actionSequence=-1;ackBaseline=ACK_RECEIPTS.get();doorRenderFrames=0;predictedAllSix=false;predictionLogged=false;ackVerified=false;rollbackVerified=false;
    mc.getSingleplayerServer().execute(()->{try{prepare(mc.getSingleplayerServer(),next);ready=next;}catch(RuntimeException e){failure=e;}});
  }
  private static void look(ServerPlayer p,Vec3 target){Vec3 d=target.subtract(p.getEyePosition());float yaw=(float)(Math.atan2(d.z,d.x)*180/Math.PI)-90;float pitch=(float)(-Math.atan2(d.y,Math.hypot(d.x,d.z))*180/Math.PI);p.setYRot(yaw);p.setXRot(pitch);p.yHeadRot=p.yBodyRot=yaw;}
  private static void route(ServerPlayer p,String space){
    var target=dimension(space);if(p.level().dimension().equals(target))return;
    if(!p.level().dimension().equals(HubContent.HUB)&&!HubContent.returnToHub(p))throw new IllegalStateException("0251setup could not return through production route");
    if(target.equals(HubContent.GARDEN)&&!HubContent.visitGarden(p)||target.equals(HubContent.KINDERGARTEN)&&!HubContent.visitKindergarten(p))throw new IllegalStateException("0251setup production route refused "+space);
    if(!p.level().dimension().equals(target))throw new IllegalStateException("0251setup wrong realm "+space);
  }
  private static void warm(ServerLevel level,String space){var b=bounds(space);for(int x=((int)b.minX)>>4;x<=(((int)b.maxX-1)>>4);x++)for(int z=((int)b.minZ)>>4;z<=(((int)b.maxZ-1)>>4);z++)level.getChunk(x,z);}
  private static void prepare(MinecraftServer server,int index){
    var p=player(server);if(p==null)throw new IllegalStateException("Missing0251study observer");p.closeContainer();p.removeAllEffects();p.clearFire();p.setHealth(p.getMaxHealth());
    if(index==0){
      inspectPreset(server);if(HubContent.isHubDimension(p.level())&&!HubContent.returnHome(p))throw new IllegalStateException("0251observer prior journal reset failed");
      var before=HubWorldData.get(server).save(new net.minecraft.nbt.CompoundTag(),server.registryAccess()).getCompound("Revisions");
      for(var realm:List.of(HubContent.HUB,HubContent.KINDERGARTEN,HubContent.GARDEN))
        if(before.getInt(realm.location().toString())!=0)throw new IllegalStateException("0251fresh initial-entry proof requires unbuilt scenic realms: "+before);
      p.setGameMode(GameType.SURVIVAL);if(!HubContent.enterHub(p))throw new IllegalStateException("0251actual hub entry failed");
      var revisions=HubWorldData.get(server).save(new net.minecraft.nbt.CompoundTag(),server.registryAccess()).getCompound("Revisions");
      for(var realm:List.of(HubContent.HUB,HubContent.KINDERGARTEN,HubContent.GARDEN))
        if(revisions.getInt(realm.location().toString())!=HubContent.ROOM_REVISION)throw new IllegalStateException("0251initial genuine entry left a scenic realm unprepared: "+revisions);
      LogUtils.getLogger().info("CHAO_WORLD0251_INITIAL_READY hubRevision={} kindergartenRevision={} gardenRevision={} expectedRevision={} freshBefore=true actualEntry=true beforeSceneEnsure=true wallClockMillis={}",revisions.getInt(HubContent.HUB.location().toString()),revisions.getInt(HubContent.KINDERGARTEN.location().toString()),revisions.getInt(HubContent.GARDEN.location().toString()),HubContent.ROOM_REVISION,System.currentTimeMillis());
    }
    route(p,scene().space());if(p.gameMode.getGameModeForPlayer()!=GameType.ADVENTURE||p.getAbilities().mayBuild)throw new IllegalStateException("0251all scenic realms must use Adventure without build permission");
    var level=p.serverLevel();if(!HubContent.ensure(level))throw new IllegalStateException("0251scene build failed");warm(level,scene().space());if(scene().walk())warm(server.getLevel(dimension(scene().walkTo())),scene().walkTo());
    p.setNoGravity(true);p.stopFallFlying();p.fallDistance=0;p.setDeltaMovement(Vec3.ZERO);p.teleportTo(scene().eye().x,scene().eye().y-p.getEyeHeight(),scene().eye().z);look(p,scene().target());p.connection.teleport(p.getX(),p.getY(),p.getZ(),p.getYRot(),p.getXRot());
    LogUtils.getLogger().info("CHAO_WORLD0251_PREPARED scene={} stage={} dimension={} eye={} target={} mode={} mayBuild={}",scene().key(),index,p.level().dimension().location(),scene().eye(),scene().target(),p.gameMode.getGameModeForPlayer(),p.getAbilities().mayBuild);
  }
  private static void maintain(MinecraftServer server){if(ready!=stage)return;var p=player(server);if(p==null)return;p.setNoGravity(true);p.setDeltaMovement(Vec3.ZERO);p.fallDistance=0;p.setPos(scene().eye().x,scene().eye().y-p.getEyeHeight(),scene().eye().z);look(p,scene().target());}
  private static boolean meshReady(Minecraft mc){
    String space=currentSpace();if(!mc.level.dimension().equals(dimension(space)))return false;var b=bounds(space);
    for(int x=((int)b.minX)>>4;x<=(((int)b.maxX-1)>>4);x++)for(int z=((int)b.minZ)>>4;z<=(((int)b.maxZ-1)>>4);z++)if(!mc.level.getChunkSource().hasChunk(x,z))return false;
    return mc.levelRenderer.hasRenderedAllSections();
  }
  @SubscribeEvent public static void frame(RenderFrameEvent.Post event){
    if(!enabled()||stopping||stage<0||ready!=stage)return;var mc=Minecraft.getInstance();if(mc.level==null||mc.player==null)return;
    probeRenderedFortune(mc);
    if(scene().action()&&begun){int mask=doorMask(mc.level,scene());if(mask!=0&&mask!=63)throw new IllegalStateException("0251actual rendered frame has mixed paired-door state: "+scene().key()+" mask="+mask);doorRenderFrames++;LogUtils.getLogger().info("CHAO_WORLD0251_DOOR_RENDER scene={} frame={} mask={} parts=6 renderQueueEmpty={} wallClockMillis={} clientTick={}",scene().key(),doorRenderFrames,mask,mc.levelRenderer.hasRenderedAllSections(),System.currentTimeMillis(),tick);}
    if(moving||images==scene().images())return;
    if(tick-started<25||!meshReady(mc)){stable=0;return;}if(++stable<40)return;
    if(images==1&&!scene().walk()&&tick-firstTick<scene().holdTicks())return;
    if(images==1&&scene().walk()&&!arrived)return;
    if(scene().action()){
      var state=mc.level.getBlockState(scene().door());
      if(!state.hasProperty(BlockStateProperties.OPEN))throw new IllegalStateException("0251native door target has no OPEN state: "+scene().door()+" "+state);
      if(images==0&&(scene().denied()?state.getValue(BlockStateProperties.OPEN)!=scene().expectOpen():state.getValue(BlockStateProperties.OPEN)==scene().expectOpen()))throw new IllegalStateException("0251door before-state would not exercise the intended genuine toggle/denial");
      if(images==1&&(!actionVerified||state.getValue(BlockStateProperties.OPEN)!=scene().expectOpen()))return;
    }
    if(mc.getWindow().getWidth()!=1280||mc.getWindow().getHeight()!=720)throw new IllegalStateException("0251native framebuffer must1280x720");
    String phase=scene().walk()?(images==0?"start":"arrived"):scene().motion()?(images==0?"start":"end"):"still";
    final String file="chao0251-"+scene().key()+(phase.equals("still")?"":"-"+phase)+".png";final int captureStage=stage;
    if(images==0){firstTick=tick;begun=true;LogUtils.getLogger().info("CHAO_WORLD0251_SCENE_BEGIN scene={} wallClockMillis={} clientTick={} dimension={}",scene().key(),System.currentTimeMillis(),tick,mc.level.dimension().location());}
    LogUtils.getLogger().info("CHAO_WORLD0251_CAPTURE file={} stage={} scene={} phase={} dimension={} mode={} stableFrames={} wallClockMillis={} clientTick={} facts=allBoundsChunksReceived=true renderQueueEmpty=true",file,stage,scene().key(),phase,mc.level.dimension().location(),mc.gameMode.getPlayerMode(),stable,System.currentTimeMillis(),tick);
    Screenshot.grab(mc.gameDirectory,file,mc.getMainRenderTarget(),message->{try{
      if(!(message.getContents() instanceof net.minecraft.network.chat.contents.TranslatableContents c)||!c.getKey().equals("screenshot.success"))throw new IllegalStateException(message.getString());
      var path=mc.gameDirectory.toPath().resolve("screenshots").resolve(file);var image=javax.imageio.ImageIO.read(path.toFile());if(image==null||image.getWidth()!=1280||image.getHeight()!=720)throw new IllegalStateException("Invalid0251saved framebuffer");
      String digest=java.util.HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(path)));
      LogUtils.getLogger().info("CHAO_WORLD0251_SAVED file={} stage={} sha256={}",file,captureStage,digest);SAVED.incrementAndGet();
    }catch(Exception e){failure=new IllegalStateException("0251actual screenshot callback failed",e);}});
    images++;captured++;stable=0;
  }
  private static void probeRenderedFortune(Minecraft mc){
    if(!scene().probe().equals("fortune-lids"))return;
    var actors=mc.level.getEntitiesOfClass(KindergartenNpc.class,new AABB(KindergartenContent.Role.FORTUNE.anchor).inflate(4),n->n.role()==KindergartenContent.Role.FORTUNE);
    if(actors.size()!=1)return;var npc=actors.getFirst();var renderer=mc.getEntityRenderDispatcher().getRenderer(npc);
    if(!(renderer instanceof StaffChaoRenderer025 staff))throw new IllegalStateException("0251Fortune is not using its actual staff renderer");
    var observed=staff.getModel().lastRenderedFortuneEyelids();if(observed==null||!observed.entityUuid().equals(npc.getUUID()))return;
    if(!observed.preserved()||Math.abs(observed.leftScale()-1)>1.0e-5f||Math.abs(observed.rightScale()-1)>1.0e-5f)throw new IllegalStateException("0251actual Fortune mesh blinked/distorted during native rendering: "+observed);
    if(probeActor!=null&&!probeActor.equals(observed.entityUuid()))throw new IllegalStateException("0251Fortune changed actor identity during its native interval");probeActor=observed.entityUuid();
    if(Float.isNaN(probeFirstAge))probeFirstAge=observed.renderAge();probeLastAge=observed.renderAge();probeFrames++;
    if(npc.serviceGesture()==3)probeGestureFrames++;
    probeMinScale=Math.min(probeMinScale,Math.min(observed.leftScale(),observed.rightScale()));probeMaxScale=Math.max(probeMaxScale,Math.max(observed.leftScale(),observed.rightScale()));
  }
  private static void inspectPreset(MinecraftServer server){
    var id=ResourceLocation.withDefaultNamespace("worldgen/world_preset/flat.json");
    try{var resource=server.getResourceManager().getResourceOrThrow(id);java.util.Set<String> keys;try(var reader=resource.openAsReader()){keys=new java.util.TreeSet<>(JsonParser.parseReader(reader).getAsJsonObject().getAsJsonObject("dimensions").keySet());}
      if(!keys.equals(java.util.Set.of("minecraft:overworld","minecraft:the_end","minecraft:the_nether"))||server.getLevel(HubContent.HUB)==null||server.getLevel(HubContent.GARDEN)==null||server.getLevel(HubContent.KINDERGARTEN)==null)throw new IllegalStateException("0251normal dimension bake/fixture boundary failed");
      LogUtils.getLogger().info("CHAO_WORLD0251_RUNTIME_PRESET source={} keys={} hubLoaded=true kindergartenLoaded=true gardenLoaded=true",resource.sourcePackId(),keys);
      var garden=server.getLevel(HubContent.GARDEN).getBiome(new BlockPos(0,80,0)).unwrapKey().orElseThrow().location();
      var school=server.getLevel(HubContent.KINDERGARTEN).getBiome(new BlockPos(0,80,0)).unwrapKey().orElseThrow().location();
      var expected=ResourceLocation.fromNamespaceAndPath(ChaoGarden.MODID,"scenic_daylight");
      if(!garden.equals(expected)||!school.equals(expected)||!server.overworld().getGameRules().getBoolean(net.minecraft.world.level.GameRules.RULE_DOMOBSPAWNING))throw new IllegalStateException("0251fresh custom biome bake/normal Overworld spawn-rule fixture failed");
      LogUtils.getLogger().info("CHAO_WORLD0251_RUNTIME_BIOMES garden={} school={} overworldMobSpawning=true",garden,school);
    }catch(java.io.IOException e){throw new IllegalStateException(e);}
  }
  private static void complete(Minecraft mc){
    stopping=true;input(mc,false);mc.getSingleplayerServer().execute(()->{var p=player(mc.getSingleplayerServer());boolean restored=p!=null&&HubContent.returnHome(p)&&p.gameMode.getGameModeForPlayer()==GameType.SURVIVAL&&!HubContent.hasReturnJournal(p);if(!restored){failure=new IllegalStateException("0251original Survival/location journal restoration failed");mc.execute(()->{throw failure;});return;}
      p.setNoGravity(false);LogUtils.getLogger().info("CHAO_WORLD0251_COMPLETE scenes={} frames={} saves={} returnRestored=true wallClockMillis={}",scenes.size(),captured,SAVED.get(),System.currentTimeMillis());mc.execute(mc::stop);});
  }
}
