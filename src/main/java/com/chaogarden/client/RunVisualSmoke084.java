package com.chaogarden.client;

import com.chaogarden.ChaoGarden;
import com.chaogarden.companion.CompanionContent;
import com.chaogarden.companion.CompanionFlight;
import com.chaogarden.companion.EquippedRun084;
import com.chaogarden.core.ChaoState;
import com.chaogarden.core.EquippedAbilityRules080;
import com.chaogarden.entity.ChaoEntity;
import com.chaogarden.item.ChaoCarrierItem;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.mojang.logging.LogUtils;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.AccessibilityOnboardingScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.*;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/** EXCLUDED opt-in single integrated-client physics study. Real one-tick key
 * input, real slots, packets and production travel; not a TCP/non-owner proof.
 * Pausing immediately after measurement bounds extreme travel for capture. */
@EventBusSubscriber(modid=ChaoGarden.MODID,value=Dist.CLIENT)
public final class RunVisualSmoke084 {
  private record Case(String name,String mode,int tier,boolean ordinary) {}
  private static final List<Case> CASES=List.of(
      new Case("ground_zero","ground",0,false),new Case("ground_max","ground",1000,false),
      new Case("creative_horizontal_zero","horizontal",0,false),new Case("creative_horizontal_max","horizontal",1000,false),
      new Case("creative_vertical_zero","vertical",0,false),new Case("creative_vertical_max","vertical",1000,false),
      new Case("companion_glide_zero","glide",0,false),new Case("companion_glide_max","glide",1000,false),
      new Case("ordinary_run_creative","horizontal",1000,true));
  private static final Gson JSON=new GsonBuilder().setPrettyPrinting().serializeNulls().create();
  private static final Path OUT=Path.of(System.getenv().getOrDefault("CHAO_RUN084_OUTPUT","/workspace/.setup/fixes084/unconfigured-run-client"));
  private static final String FREEZE=System.getenv().getOrDefault("CHAO_RUN084_SOURCE_FREEZE","unrecorded");
  private static final List<Map<String,Object>> RESULTS=new ArrayList<>(),CLIENT_TRACE=new ArrayList<>(),SERVER_TRACE=Collections.synchronizedList(new ArrayList<>());
  private static final List<Map<String,Object>> REVOKE_COAST_CLIENT=new ArrayList<>();
  private static volatile String stage="boot",fatal;
  private static volatile UUID actor;
  private static int ticks,index=-1,readyTicks,measuredTicks,waitTicks;
  private static boolean started,platform,worldRendered,playerRendered,shotPending;
  private static volatile boolean shotDone;
  private static boolean revokeSubcase;
  private static boolean revokeCoasting;
  private static Map<String,Object> revokeClearProof;
  private static List<Map<String,Object>> revokeCoastServer;
  private static CompletableFuture<Map<String,Object>> pending;
  private static Map<String,Object> before,end,prepared,serverEnd;
  private static Map<String,Object> presentationHistory;
  private static Vec3 clientEnd;
  private static int playerTickAtStart;
  private static long stageWall;
  private static final PauseCapture PAUSE=new PauseCapture();
  private RunVisualSmoke084() {}
  private static boolean enabled(){return "true".equalsIgnoreCase(System.getenv("CHAO_RUN084_SMOKE"));}
  private static void check(boolean value,String reason){if(!value)throw new IllegalStateException("RUN084 "+reason);}
  private static String sha(byte[] bytes){try{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));}catch(Exception e){throw new IllegalStateException(e);}}
  private static synchronized void write(String name,Object data){try{Files.createDirectories(OUT);Files.writeString(OUT.resolve(name),JSON.toJson(data)+"\n");}catch(Exception e){throw new IllegalStateException(e);}}
  private static void phase(String next){stage=next;stageWall=System.nanoTime();waitTicks=0;}
  private static void pause(Minecraft mc,boolean value){
    // Minecraft normally recomputes this flag only after rendering. A slow
    // frame can contain several catch-up ticks; stop that batch immediately
    // after the measured native tick, solely in this excluded study.
    try{var field=Minecraft.class.getDeclaredField("pause");field.setAccessible(true);field.setBoolean(mc,value);}
    catch(ReflectiveOperationException error){throw new IllegalStateException("Cannot bound study catch-up ticks",error);}
  }
  private static ServerPlayer keeper(MinecraftServer server){return server.getPlayerList().getPlayers().getFirst();}
  private static Case entry(){return CASES.get(index);}
  private static void input(Minecraft mc,boolean down){
    mc.options.keyUp.setDown(down&&!entry().mode().equals("vertical")&&!entry().mode().equals("glide"));
    mc.options.keyJump.setDown(down&&entry().mode().equals("vertical"));
    mc.options.keyDown.setDown(false);mc.options.keyLeft.setDown(false);mc.options.keyRight.setDown(false);mc.options.keyShift.setDown(false);mc.options.keySprint.setDown(false);
  }
  private static double amount(net.minecraft.world.entity.player.Player p){var a=p.getAttribute(Attributes.MOVEMENT_SPEED);var m=a==null?null:a.getModifier(CompanionContent.RUN_SPEED);return m==null?0:m.amount();}
  private static List<Double> vector(Vec3 value){check(Double.isFinite(value.x)&&Double.isFinite(value.y)&&Double.isFinite(value.z),"nonfinite actual vector");return List.of(value.x,value.y,value.z);}
  private static void alignCaptureHistory(Minecraft mc){
    var p=mc.player;var position=p.position();var velocity=p.getDeltaMovement();var bounds=p.getBoundingBox();
    var proof=new LinkedHashMap<String,Object>();proof.put("scope","Presentation history only after the measured native tick. Prevent a paused partial-tick camera from culling the avatar's already-current bounding box; actual position, collision box and velocity are untouched.");
    proof.put("previous_tick_history",List.of(p.xo,p.yo,p.zo));proof.put("previous_frame_history",List.of(p.xOld,p.yOld,p.zOld));
    proof.put("current_endpoint",vector(position));proof.put("velocity",vector(velocity));proof.put("bounding_box",List.of(bounds.minX,bounds.minY,bounds.minZ,bounds.maxX,bounds.maxY,bounds.maxZ));
    p.xo=p.xOld=p.getX();p.yo=p.yOld=p.getY();p.zo=p.zOld=p.getZ();
    check(p.position().equals(position)&&p.getDeltaMovement().equals(velocity)&&p.getBoundingBox().equals(bounds),"Capture presentation changed actual physics");
    proof.put("aligned_tick_history",List.of(p.xo,p.yo,p.zo));proof.put("aligned_frame_history",List.of(p.xOld,p.yOld,p.zOld));proof.put("actual_position_box_velocity_unchanged",true);presentationHistory=proof;
  }
  private static Map<String,Object> sample(net.minecraft.world.entity.player.Player p){
    var d=new LinkedHashMap<String,Object>();d.put("player_uuid",p.getUUID().toString());d.put("position",vector(p.position()));d.put("velocity",vector(p.getDeltaMovement()));d.put("tick",p.tickCount);d.put("on_ground",p.onGround());d.put("fall_flying",p.isFallFlying());d.put("abilities_flying",p.getAbilities().flying);d.put("abilities_mayfly",p.getAbilities().mayfly);d.put("raw_fly_speed",p.getAbilities().getFlyingSpeed());d.put("run_modifier",amount(p));d.put("run_multiplier",EquippedRun084.multiplier(p));d.put("movement_attribute",p.getAttributeValue(Attributes.MOVEMENT_SPEED));return d;
  }
  private static Map<String,Object> serverSample(MinecraftServer server){
    var p=keeper(server);var d=new LinkedHashMap<>(sample(p));d.put("server_tick",server.getTickCount());d.put("actual_owner_exempt_from_native_too_fast",true);d.put("movement_check",EquippedRun084.movementCheck(p));
    var body=CompanionVisualSmokeSupport.body(p);var held=CompanionVisualSmokeSupport.held(p);
    d.put("actual_slot_filled",!held.isEmpty());d.put("carrier_uuid",String.valueOf(ChaoCarrierItem.storedUUID(held)));
    if(body!=null){d.put("actor_uuid",body.getUUID().toString());d.put("adult",body.state().adult);d.put("first_type",body.state().lockedPrimaryType());d.put("alignment",body.state().lockedAlignment());d.put("body_setting",EquippedAbilityRules080.percent(body.state(),ChaoState.RUN));d.put("body_state_sha256",sha(body.state().encode().getBytes(java.nio.charset.StandardCharsets.UTF_8)));d.put("carrier_setting",EquippedAbilityRules080.percent(ChaoCarrierItem.state(held),ChaoState.RUN));}
    return d;
  }
  private static Map<String,Object> prepare(MinecraftServer server){
    var p=keeper(server);var world=p.serverLevel();var e=entry();CompanionVisualSmokeSupport.clear(p);
    if(!platform){
      platform=true;world.setDayTime(6000);world.getGameRules().getRule(GameRules.RULE_DAYLIGHT).set(false,server);world.getGameRules().getRule(GameRules.RULE_DOMOBSPAWNING).set(false,server);
      // A finite512-block corridor; no view-distance, no-collision, teleport
      // or chunk validation exemption is installed by the ability itself.
      for(int x=-16;x<=544;x+=16)for(int z:new int[]{-16,0,16})world.getChunkAt(new BlockPos(x,96,z));
      for(int x=-8;x<=520;x++)for(int z=-4;z<=4;z++)world.setBlock(new BlockPos(x,96,z),Blocks.SMOOTH_STONE.defaultBlockState(),3);
    }
    p.stopFallFlying();p.setGameMode(e.mode().equals("ground")||e.mode().equals("glide")?GameType.SURVIVAL:GameType.CREATIVE);
    p.getAbilities().flying=!e.mode().equals("ground")&&!e.mode().equals("glide");p.getAbilities().setFlyingSpeed(.05f);p.onUpdateAbilities();p.setNoGravity(!e.mode().equals("ground"));p.setSprinting(false);p.setDeltaMovement(Vec3.ZERO);p.fallDistance=0;
    p.connection.teleport(.5,e.mode().equals("ground")?97:e.mode().equals("glide")?200:128,.5,-90,0);p.setOnGround(e.mode().equals("ground"));
    var state=new ChaoState();state.adult=true;state.primaryType=ChaoState.RUN_TYPE;state.energy=state.hunger=100;state.levels[ChaoState.RUN]=99;state.points[ChaoState.RUN]=9900;state.levels[ChaoState.FLY]=state.levels[ChaoState.SWIM]=99;
    state.grades[ChaoState.RUN]=5;state.gradeA[ChaoState.RUN]=state.gradeB[ChaoState.RUN]=5;
    if(!e.ordinary())check(state.useHyperFruit(ChaoState.RUN),"native X-rank fixture");
    check(state.setTrait(EquippedAbilityRules080.RUN_SETTING,Integer.toString(e.tier())),"save actual tier");
    var held=CompanionVisualSmokeSupport.equip(p,ChaoState.decode(state.encode()));actor=ChaoCarrierItem.storedUUID(held);check(actor!=null,"real slot retained UUID");
    var result=new LinkedHashMap<String,Object>();result.put("case",e);result.put("actor_uuid",actor.toString());result.put("initial_state_sha256",sha(state.encode().getBytes(java.nio.charset.StandardCharsets.UTF_8)));result.put("fixture_teleport_before_measurement",true);result.put("corridor_blocks",512);return result;
  }
  private static Map<String,Object> ready(MinecraftServer server){
    var p=keeper(server);var body=CompanionVisualSmokeSupport.body(p);if(body==null||!actor.equals(body.getUUID()))return null;
    double expected=entry().ordinary()?1:entry().tier();if(Math.abs(amount(p)-expected)>.000001)return null;
    check(body.state().adult&&body.state().lockedPrimaryType()==ChaoState.RUN_TYPE,"locked identity changed");
    check(actor.equals(ChaoCarrierItem.storedUUID(CompanionVisualSmokeSupport.held(p))),"actual slot/body UUID differs");return serverSample(server);
  }
  private static void begin(Minecraft mc){
    var p=mc.player;input(mc,false);CLIENT_TRACE.clear();SERVER_TRACE.clear();measuredTicks=0;p.setSprinting(false);p.setYRot(-90);p.setXRot(0);p.setDeltaMovement(Vec3.ZERO);p.setOnGround(entry().mode().equals("ground"));
    if(entry().mode().equals("glide")){
      p.setNoGravity(false);check(CompanionFlight.allowed(p),"actual synchronized companion flight permit is absent");
      check(p.tryToStartFallFlying(),"ordinary companion launch gate rejected fixture");
      p.connection.send(new ServerboundPlayerCommandPacket(p,ServerboundPlayerCommandPacket.Action.START_FALL_FLYING));
      // Same native initial control momentum in both paired cases; the bonus
      // is not installed via this seed and does not scale stored velocity.
      p.setDeltaMovement(.2,0,0);mc.getSingleplayerServer().execute(()->{var sp=keeper(mc.getSingleplayerServer());sp.setNoGravity(false);sp.setDeltaMovement(.2,0,0);});
    }
    before=sample(p);playerTickAtStart=p.tickCount;phase("measure");mc.setScreen(null);pause(mc,false);
  }
  private static void next(Minecraft mc){
    input(mc,false);mc.setScreen(PAUSE);pause(mc,true);revokeSubcase=false;index++;
    if(index>=CASES.size()){complete(mc);return;}
    LogUtils.getLogger().info("RUN084_CASE index={} name={} mode={} tier={}",index,entry().name(),entry().mode(),entry().tier());
    readyTicks=0;phase("prepare");pending=mc.getSingleplayerServer().submit(()->prepare(mc.getSingleplayerServer()));
  }
  private static void complete(Minecraft mc){
    var ratios=new LinkedHashMap<String,Object>();
    for(String mode:List.of("ground","creative_horizontal","creative_vertical","companion_glide")){
      var a=RESULTS.stream().filter(v->v.get("case_name").equals(mode+"_zero")).findFirst().orElseThrow();var b=RESULTS.stream().filter(v->v.get("case_name").equals(mode+"_max")).findFirst().orElseThrow();
      double ordinary=((Number)a.get("measured_distance")).doubleValue(),boosted=((Number)b.get("measured_distance")).doubleValue();double ratio=boosted/ordinary;
      check(Double.isFinite(ratio)&&Math.abs(ratio-1001)<3.1,mode+" one-tick paired physical ratio differs: "+ratio);ratios.put(mode,ratio);
    }
    write("complete.json",Map.of("status","PASS","source_freeze",FREEZE,"cases",RESULTS,"paired_one_tick_distance_ratios",ratios,"scope","Actual single integrated client with native one-tick key input and ordinary production travel. NOT TCP, not non-owner anti-cheat, not a continuous full-speed soak. Measurement pause bounds extreme travel; preparations teleport/reset only between cases."));
    LogUtils.getLogger().info("RUN084_COMPLETE cases={} ratios={} freeze={}",RESULTS.size(),ratios,FREEZE);
    mc.getSingleplayerServer().execute(()->CompanionVisualSmokeSupport.clear(keeper(mc.getSingleplayerServer())));mc.stop();phase("complete");
  }
  @SubscribeEvent public static void pre(ClientTickEvent.Pre event){
    if(!enabled()||index<0||index>=CASES.size())return;var mc=Minecraft.getInstance();if(mc.player==null)return;
    input(mc,stage.equals("measure")&&measuredTicks==0||stage.equals("revoke_measure")&&measuredTicks==0);
    if(stage.equals("measure")||stage.equals("revoke_measure")){mc.player.setYRot(-90);mc.player.setXRot(0);mc.player.setSprinting(false);}
  }
  @SubscribeEvent public static void serverTick(ServerTickEvent.Post event){
    if(!enabled()||!(stage.equals("measure")||stage.equals("capture")||stage.startsWith("revoke")))return;
    try{SERVER_TRACE.add(serverSample(event.getServer()));}catch(Throwable error){fatal=error.toString();}
  }
  @SubscribeEvent public static void post(ClientTickEvent.Post event){
    if(!enabled()||stage.equals("complete"))return;var mc=Minecraft.getInstance();ticks++;
    try{
      if(fatal!=null)throw new IllegalStateException(fatal);check(ticks<18000,"client study timeout");
      if(mc.screen instanceof AccessibilityOnboardingScreen)mc.setScreen(new TitleScreen());
      if(!started&&mc.screen instanceof TitleScreen){started=true;mc.options.pauseOnLostFocus=false;mc.options.hideGui=true;mc.options.renderDistance().set(20);mc.options.simulationDistance().set(5);mc.options.framerateLimit().set(60);mc.options.setCameraType(CameraType.THIRD_PERSON_BACK);
        check(!FREEZE.equals("unrecorded"),"record the tested source freeze before launch");check(!Files.exists(OUT.resolve("complete.json"))&&!Files.exists(OUT.resolve("failure.json")),"reuse of completed/failed evidence output is forbidden");
        mc.createWorldOpenFlows().createFreshLevel("Run084-"+System.currentTimeMillis(),new LevelSettings("Run084",GameType.SURVIVAL,false,Difficulty.PEACEFUL,true,new GameRules(),WorldDataConfiguration.DEFAULT),new WorldOptions(840084L,false,false),r->r.registryOrThrow(Registries.WORLD_PRESET).getHolderOrThrow(WorldPresets.FLAT).value().createWorldDimensions(),mc.screen);phase("world");return;}
      if(mc.player==null||mc.getSingleplayerServer()==null)return;
      if(stage.equals("world")){mc.setScreen(null);next(mc);return;}
      check(System.nanoTime()-stageWall<180_000_000_000L,"stage timeout "+stage);
      if(stage.equals("prepare")){if(!pending.isDone())return;prepared=pending.join();pending=null;phase("ready");mc.setScreen(null);pause(mc,false);return;}
      if(stage.equals("ready")){
        if(pending==null)pending=mc.getSingleplayerServer().submit(()->ready(mc.getSingleplayerServer()));
        if(!pending.isDone())return;var proof=pending.join();pending=null;
        double expected=entry().ordinary()?1:entry().tier();if(proof==null||Math.abs(amount(mc.player)-expected)>.000001)return;
        boolean flying=!entry().mode().equals("ground")&&!entry().mode().equals("glide");if(mc.player.getAbilities().flying!=flying)return;
        if(++readyTicks<12)return;prepared.put("ready_server",proof);begin(mc);return;
      }
      if(stage.equals("measure")||stage.equals("revoke_measure")){
        CLIENT_TRACE.add(sample(mc.player));if(++measuredTicks<1)return;input(mc,false);end=sample(mc.player);clientEnd=mc.player.position();
        check(mc.player.tickCount-playerTickAtStart==1,"measurement contains exactly one actual LocalPlayer tick");
        check(Math.abs(mc.player.getAbilities().getFlyingSpeed()-.05f)<1e-9,"stored default flight speed was changed");
        if(revokeSubcase){
          double oldX=((Number)((List<?>)before.get("velocity")).get(0)).doubleValue();
          double freshInput=mc.player.getDeltaMovement().x/.91-oldX;
          check(Math.abs(freshInput-.05*.98)<.002,"unequipped actual forward input retained a boost: "+freshInput);
          end.put("fresh_forward_acceleration_after_native_drag",freshInput);
        }
        phase("capture");alignCaptureHistory(mc);worldRendered=playerRendered=false;shotDone=shotPending=false;pending=null;mc.setScreen(PAUSE);pause(mc,true);return;
      }
      if(stage.equals("capture")){
        if(pending==null)pending=mc.getSingleplayerServer().submit(()->{var p=keeper(mc.getSingleplayerServer());return p.position().distanceToSqr(clientEnd)<.0004?serverSample(mc.getSingleplayerServer()):null;});
        if(pending.isDone()){var proof=pending.join();pending=null;if(proof!=null)serverEnd=proof;}
        if(!shotDone||serverEnd==null)return;
        var result=new LinkedHashMap<String,Object>();result.put("case_name",revokeSubcase?"creative_max_unequipped_inertia":entry().name());result.put("source_freeze",FREEZE);result.put("scope","Actual one native client tick; single integrated owner, no TCP anti-cheat claim");result.put("prepared",prepared);result.put("before",before);result.put("after",end);result.put("actual_client_ticks",1);result.put("actual_server_ack",serverEnd);result.put("measured_distance",clientEnd.distanceTo(new Vec3(((Number)((List<?>)before.get("position")).get(0)).doubleValue(),((Number)((List<?>)before.get("position")).get(1)).doubleValue(),((Number)((List<?>)before.get("position")).get(2)).doubleValue())));result.put("client_trace",List.copyOf(CLIENT_TRACE));synchronized(SERVER_TRACE){result.put("server_trace",List.copyOf(SERVER_TRACE));}
        if(revokeSubcase){result.put("actual_server_clear",revokeClearProof);result.put("unpaused_client_coast_until_normal_attribute_sync",List.copyOf(REVOKE_COAST_CLIENT));result.put("unpaused_server_coast_until_normal_attribute_sync",revokeCoastServer);}
        RESULTS.add(result);write(String.format("case-%02d%s.json",index,revokeSubcase?"-revoked":""),result);
        if(entry().name().equals("creative_horizontal_max")&&!revokeSubcase){
          // Preserve actual max-speed residual momentum through unequip. Do
          // not reset velocity or teleport to make revocation appear settled.
          revokeCoasting=false;REVOKE_COAST_CLIENT.clear();phase("revoke_wait");pending=mc.getSingleplayerServer().submit(()->{var p=keeper(mc.getSingleplayerServer());CompanionVisualSmokeSupport.clear(p);check(CompanionContent.secretRunBonus084(p)==0,"server authority survived actual unequip");return serverSample(mc.getSingleplayerServer());});return;
        }
        next(mc);serverEnd=null;return;
      }
      if(stage.equals("revoke_wait")){
        if(!pending.isDone())return;var proof=pending.join();check(((Number)proof.get("run_multiplier")).doubleValue()==1,"server multiplier survived unequip");
        if(!revokeCoasting){
          revokeClearProof=proof;write("revoke-server-clear.json",proof);SERVER_TRACE.clear();
          // Server attribute changes are delivered by normal entity tracking,
          // which the integrated pause deliberately suspends. Resume with no
          // input and retain real residual motion until that packet arrives.
          // No direct packet pump, velocity reset or teleport hides the coast.
          input(mc,false);mc.setScreen(null);pause(mc,false);revokeCoasting=true;return;
        }
        REVOKE_COAST_CLIENT.add(sample(mc.player));if(amount(mc.player)!=0)return;
        synchronized(SERVER_TRACE){revokeCoastServer=List.copyOf(SERVER_TRACE);}
        revokeSubcase=true;CLIENT_TRACE.clear();SERVER_TRACE.clear();measuredTicks=0;before=sample(mc.player);playerTickAtStart=mc.player.tickCount;
        check(mc.player.getDeltaMovement().horizontalDistance()>0,"revoke fixture must retain actual residual momentum");
        check(EquippedRun084.flightSpeed(mc.player,.05f)==.05f,"revoked ordinary input is still amplified");
        serverEnd=null;phase("revoke_measure");mc.setScreen(null);pause(mc,false);return;
      }
    }catch(Throwable error){fatal=error.toString();write("failure.json",Map.of("status","FAIL","error",fatal,"stage",stage,"case_index",index,"source_freeze",FREEZE));LogUtils.getLogger().error("RUN084_CLIENT_FAILURE",error);if(mc.player!=null&&index>=0&&index<CASES.size())input(mc,false);mc.stop();phase("complete");}
  }
  @SubscribeEvent public static void world(RenderLevelStageEvent event){if(enabled()&&stage.equals("capture")&&event.getStage()==RenderLevelStageEvent.Stage.AFTER_LEVEL&&!SceneShaderPack.shadows())worldRendered=true;}
  @SubscribeEvent public static void rendered(RenderLivingEvent.Post<?,?> event){if(enabled()&&stage.equals("capture")&&event.getEntity()==Minecraft.getInstance().player)playerRendered=true;}
  @SubscribeEvent public static void frame(RenderFrameEvent.Post event){
    if(!enabled()||!stage.equals("capture")||shotPending||shotDone||!worldRendered||!playerRendered)return;var mc=Minecraft.getInstance();if(mc.player==null)return;shotPending=true;
    String name=String.format("run084-%02d-%s%s.png",index,entry().name(),revokeSubcase?"-revoked":"");
    Screenshot.grab(OUT.toFile(),name,mc.getMainRenderTarget(),message->{try{var file=OUT.resolve("screenshots").resolve(name);check(Files.isRegularFile(file),"native PNG missing");write(name.replace(".png","-capture.json"),Map.of("actual_native_framebuffer",true,"actual_world_render",worldRendered,"actual_player_render",playerRendered,"paused_after_one_native_tick",true,"presentation_history",presentationHistory,"png",file.toString(),"png_sha256",sha(Files.readAllBytes(file)),"source_freeze",FREEZE));shotDone=true;}catch(Throwable error){fatal=error.toString();}});
  }
  private static final class PauseCapture extends Screen {
    private PauseCapture(){super(Component.literal("Run measurement capture"));}
    @Override public void render(GuiGraphics graphics,int x,int y,float partial){}
    @Override public boolean isPauseScreen(){return true;}
    @Override public boolean shouldCloseOnEsc(){return false;}
  }
}
