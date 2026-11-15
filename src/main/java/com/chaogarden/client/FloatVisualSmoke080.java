package com.chaogarden.client;

import com.chaogarden.ChaoGarden;
import com.chaogarden.core.*;
import com.chaogarden.entity.ChaoEntity;
import com.chaogarden.toys.*;
import com.google.gson.*;
import com.mojang.logging.LogUtils;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.MessageDigest;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import net.minecraft.client.*;
import net.minecraft.client.gui.screens.*;
import net.minecraft.core.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.decoration.ArmorStand;
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

/** PRIVATE DEVELOPMENT: actual physical floats, default Chao goals and renderer.
 * Natural cases never call board/push or replace GoalSelectors. Short simulation
 * pauses admit matched multiangle views of a real sampled production phase.
 * Native previews are explicitly separate from autonomous behavior evidence. */
@EventBusSubscriber(modid=ChaoGarden.MODID,value=Dist.CLIENT)
public final class FloatVisualSmoke080 {
  private record Case(int id,String name,String kind,String activity,Direction direction,int depth,String form,boolean shiny,boolean gear) {
    boolean push(){return activity.equals("push")||activity.equals("turn");}
    boolean preview(){return activity.startsWith("preview");}
    boolean retry(){return activity.equals("shore_retry");}
    boolean adult(){return form.startsWith("adult");}
    int timeout(){return push()?2800:preview()?1000:2400;}
  }
  private static final Gson G=new GsonBuilder().setPrettyPrinting().create();
  private static final Path OUT=Path.of(System.getenv().getOrDefault("CHAO_FLOAT080_OUTPUT","/workspace/.setup/toy080-float-capture-unconfigured")).toAbsolutePath().normalize();
  private static final List<Case> CASES=cases();private static final List<String> VIEWS=List.of("front","side","back","above");
  private static final AtomicInteger SAVED=new AtomicInteger();
  private static volatile int phase=-1,pauseSerial;private static volatile long elapsed;private static volatile String label,fatal;
  private static volatile boolean settled,armed,paused,complete;
  private static ChaoEntity actor;private static FloatToyEntity080 vessel;private static ArmorStand camera;
  private static volatile UUID actorId,vesselId;private static Vec3 actorPrevious,floatPrevious,floatOrigin;
  private static String stateBefore;private static Map<EquipmentSlot,ItemStack> gearBefore;private static long spawned,began,rideAt=-1;
  private static final Set<String> seen=new LinkedHashSet<>();private static final List<Map<String,Object>> trace=new ArrayList<>();
  private static boolean started,setup,finished,worldFrame,actorFrame,stopRequested,shoreOpened,completionRequested;
  private static int localPhase=-1,localPause=-1,view,viewFrames,shots,completedCases,actorFrames,worldFrames;private static JsonObject proof;
  private static long wallStart;private static double walked,moved;
  private static volatile long pausedAtNanos;
  private static long lastAdmissionDiagnostic;private static int pauseCandidates,pauseAdmittedFrames;
  private static String admissionReason="unobserved";
  private static int viewCandidates,viewAdmittedFrames;private static double framingRadius,framingDistance;
  private static net.minecraft.world.phys.AABB framingBounds;

  private static boolean enabled(){return "true".equalsIgnoreCase(System.getenv("CHAO_FLOAT080_SMOKE"));}
  private static List<Case> cases(){
    var all=new ArrayList<Case>();int i=0;String[] forms={"baby_neutral","adult_hero_swim","adult_dark_run","adult_neutral_fly"};
    for(String kind:List.of("chao_rubber_duck","chao_lifesaver"))for(Direction direction:Direction.Plane.HORIZONTAL){int n=i;all.add(new Case(i++,kind.substring(5)+"_push_"+direction.getSerializedName(),kind,"push",direction,1,forms[n%forms.length],n%3==2,n%2==1));}
    all.add(new Case(i++,"duck_rescue_baby_shallow","chao_rubber_duck","rescue",Direction.EAST,1,"baby_hero",false,true));
    all.add(new Case(i++,"ring_rescue_dark_run_deep","chao_lifesaver","rescue",Direction.EAST,4,"adult_dark_run",true,true));
    all.add(new Case(i++,"duck_turn_around_obstacle","chao_rubber_duck","turn",Direction.EAST,1,"adult_neutral_fly",false,true));
    all.add(new Case(i++,"ring_unsafe_shore_retry","chao_lifesaver","shore_retry",Direction.EAST,1,"adult_hero_power",false,true));
    all.add(new Case(i++,"duck_native_once_baby","chao_rubber_duck","preview_once",Direction.EAST,1,"baby_neutral",false,true));
    all.add(new Case(i++,"ring_native_loop_stop_adult","chao_lifesaver","preview_loop",Direction.EAST,4,"adult_hero_swim",true,true));
    String selection=System.getenv("CHAO_FLOAT080_CASES");if(selection==null||selection.isBlank())return List.copyOf(all);
    var chosen=new ArrayList<Case>();var unique=new HashSet<Integer>();for(String token:selection.split(",")){int n=Integer.parseInt(token.trim());require(n>=0&&n<all.size()&&unique.add(n),"invalid/duplicate capture case "+token);chosen.add(all.get(n));}return List.copyOf(chosen);
  }
  private static void require(boolean okay,String text){if(!okay)throw new IllegalStateException("FLOAT_CAPTURE080 "+text);}
  private static String sha(byte[] bytes){try{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));}catch(Exception e){throw new IllegalStateException(e);}}
  private static String sha(String text){return sha(text.getBytes(StandardCharsets.UTF_8));}
  private static void write(Path path,Object data){try{Files.createDirectories(path.getParent());Files.writeString(path,G.toJson(data)+"\n");}catch(Exception e){throw new IllegalStateException(e);}}
  private static void task(MinecraftServer server,Runnable action){server.execute(()->{try{action.run();}catch(Throwable error){fatal=error.toString();write(OUT.resolve("failure.json"),Map.of("phase",phase,"error",fatal,"trace",trace));LogUtils.getLogger().error("FLOAT_CAPTURE080_ASYNC_FAILURE",error);}});}
  private static ChaoState fixture(Case c){
    var s=new ChaoState();s.adult=c.adult();s.align=c.form().contains("hero")?1:c.form().contains("dark")?-1:0;s.primaryType=c.form().endsWith("swim")?ChaoState.SWIM_TYPE:c.form().endsWith("run")?ChaoState.RUN_TYPE:c.form().endsWith("fly")?ChaoState.FLY_TYPE:c.form().endsWith("power")?ChaoState.POWER_TYPE:ChaoState.NORMAL;
    s.hunger=95;s.energy=100;s.happiness=30;s.boredom=100;s.personality=ChaoPreferences.EASILY_BORED;s.points[ChaoState.RUN]=800;s.points[ChaoState.SWIM]=c.preview()?150:0;
    s.shiny=c.shiny();s.expressedColor=c.shiny()?4:0;s.setBaselineFace076(3,2);s.setTrait("float080.capture.keep","existing fixture history");for(int n=0;n<ChaoIllness.COUNT;n++)s.setIllness(n,100);
    if(c.adult()){s.setEvolutionStrength(1.2f);int stat=s.primaryType-1;if(stat>=0&&stat<4)s.levels[stat]=30;}
    return ChaoState.decode(s.encode());
  }
  private static Map<EquipmentSlot,ItemStack> equipment(ChaoEntity c){var out=new EnumMap<EquipmentSlot,ItemStack>(EquipmentSlot.class);for(var slot:List.of(EquipmentSlot.HEAD,EquipmentSlot.FEET,EquipmentSlot.OFFHAND,EquipmentSlot.LEGS,EquipmentSlot.CHEST))out.put(slot,c.getItemBySlot(slot).copy());return out;}
  private static boolean gearIntact(ChaoEntity c){for(var entry:gearBefore.entrySet())if(!ItemStack.matches(entry.getValue(),c.getItemBySlot(entry.getKey())))return false;return true;}
  private static JsonObject gearProof(ChaoEntity c){var out=new JsonObject();for(var entry:equipment(c).entrySet())out.addProperty(entry.getKey().name(),entry.getValue().isEmpty()?"empty":entry.getValue().save(c.registryAccess()).toString());return out;}
  private static Map<String,Object> sample(Entity e){var out=new LinkedHashMap<String,Object>();out.put("uuid",e.getUUID().toString());out.put("position",List.of(e.getX(),e.getY(),e.getZ()));out.put("velocity",List.of(e.getDeltaMovement().x,e.getDeltaMovement().y,e.getDeltaMovement().z));out.put("yaw",e.getYRot());out.put("water",e.isInWater());out.put("on_ground",e.onGround());out.put("bounds",e.getBoundingBox().toString());out.put("vehicle",e.getVehicle()==null?"":e.getVehicle().getUUID().toString());return out;}
  private static void terrain(ServerLevel level,Case c){
    level.setDayTime(6000);level.setWeatherParameters(0,100000,false,false);
    for(var at:BlockPos.betweenClosed(-9,75,-9,9,84,9)){
      boolean pool=at.getX()>=-5&&at.getX()<=5&&at.getZ()>=-5&&at.getZ()<=5&&at.getY()>=80-c.depth()&&at.getY()<=79;
      level.setBlock(at,at.getY()>=80?Blocks.AIR.defaultBlockState():pool?Blocks.WATER.defaultBlockState():Blocks.SMOOTH_STONE.defaultBlockState(),3);
    }
    if(c.activity().equals("turn"))for(var at:BlockPos.betweenClosed(-6,80,-1,-6,82,1))level.setBlock(at,Blocks.SMOOTH_STONE.defaultBlockState(),3);
    if(c.retry())for(int n=-6;n<=6;n++)for(int y=80;y<=82;y++)for(var at:List.of(new BlockPos(-6,y,n),new BlockPos(6,y,n),new BlockPos(n,y,-6),new BlockPos(n,y,6)))level.setBlock(at,Blocks.SMOOTH_STONE.defaultBlockState(),3);
  }
  private static void pause(MinecraftServer server,String name){label=name;seen.add(name);pausedAtNanos=System.nanoTime();paused=true;pauseSerial++;server.tickRateManager().setFrozen(true);LogUtils.getLogger().info("FLOAT_CAPTURE080_PHASE phase={} case={} label={} elapsed={} vesselPhase={} progress={} actor={} float={}",phase,CASES.get(phase).name(),name,elapsed,vessel.phase(),vessel.transition(),actor.position(),vessel.position());}
  private static void next(MinecraftServer server,int n){
    server.tickRateManager().setFrozen(true);if(actor!=null){ToyPreview.stop(actor);actor.remove(Entity.RemovalReason.UNLOADED_TO_CHUNK);}if(vessel!=null)vessel.remove(Entity.RemovalReason.UNLOADED_TO_CHUNK);
    var c=CASES.get(n);var level=server.overworld();terrain(level,c);var center=new Vec3(.5,80,.5);
    Vec3 floatAt=c.push()?center.add(-c.direction().getStepX()*(c.activity().equals("turn")?7:6),0,-c.direction().getStepZ()*(c.activity().equals("turn")?7:6)):c.preview()?center.add(-5,-.05,0):center.add(c.retry()?0:1,-.05,0);
    Vec3 actorAt=c.push()?floatAt.add(-c.direction().getStepX(),0,-c.direction().getStepZ()):c.preview()?center.add(-6,0,0):center.add(-3,c.depth()==4?-3.8:-.35,0);
    vessel=FloatToyContent080.newCandidate(level,c.kind(),new ItemStack(ChaoGarden.item(c.kind())),floatAt,c.direction().toYRot());require(vessel!=null&&level.addFreshEntity(vessel),"publish one actual float");vessel.commit();vesselId=vessel.getUUID();floatOrigin=floatPrevious=vessel.position();
    actor=ChaoGarden.CHAO.get().create(level);require(actor!=null,"create actual actor");actor.setUUID(UUID.nameUUIDFromBytes(("float080-scene-"+c.id()).getBytes(StandardCharsets.UTF_8)));actor.setState(fixture(c));actor.setOriginEggCoat(fixture(c));actor.setInvulnerable(true);actor.setNoAi(true);actor.deferBehavior(100000);actor.getRandom().setSeed(18);actor.moveTo(actorAt.x,actorAt.y,actorAt.z,c.direction().toYRot(),0);
    var keeper=server.getPlayerList().getPlayers().getFirst();actor.owner=keeper.getUUID();keeper.setGameMode(GameType.SPECTATOR);keeper.connection.teleport(actorAt.x-2,80,actorAt.z-2,0,0);
    if(c.gear()){actor.setItemSlot(EquipmentSlot.HEAD,new ItemStack(ChaoGarden.item("red_wool_beanie")));actor.setItemSlot(EquipmentSlot.OFFHAND,new ItemStack(ChaoGarden.item("aviators_accessory")));}
    require(level.addFreshEntity(actor),"publish actor with production joined goals");actorId=actor.getUUID();actorPrevious=actor.position();stateBefore=actor.state().encode();gearBefore=equipment(actor);
    seen.clear();trace.clear();settled=armed=paused=complete=stopRequested=shoreOpened=completionRequested=false;elapsed=0;walked=moved=0;spawned=level.getGameTime();began=-1;rideAt=-1;label="";phase=n;server.tickRateManager().setFrozen(false);
  }
  private static void command(MinecraftServer server,String verb){var keeper=server.getPlayerList().getPlayers().getFirst();String alias=CASES.get(phase).kind().substring(5);try{int result=server.getCommands().getDispatcher().execute("chao testtoy "+alias+" "+verb,keeper.createCommandSourceStack().withPermission(2));require(result==1,"native float preview "+alias+" "+verb);}catch(com.mojang.brigadier.exceptions.CommandSyntaxException error){throw new IllegalStateException(error);}}
  private static void arm(MinecraftServer server){
    if(armed)return;var c=CASES.get(phase);stateBefore=actor.state().encode();began=server.overworld().getGameTime();
    if(c.preview()){command(server,c.activity().equals("preview_loop")?"loop":"once");require(ToyPreview.active(actor),"native preview manager active");}
    else actor.setNoAi(false);armed=true;paused=false;server.tickRateManager().setFrozen(false);
  }
  private static void integrity(Case c){
    require(actor.isAlive()&&!actor.state().dead&&actor.state().adult==c.adult(),"life stage preserved");
    var original=ChaoState.decode(stateBefore);if(c.adult())require(actor.state().lockedPrimaryType()==original.lockedPrimaryType()&&Math.abs(actor.state().lockedAlignment()-original.lockedAlignment())<.001,"locked adult identity preserved");
    require(actor.state().shiny==original.shiny&&actor.state().expressedColor==original.expressedColor&&actor.state().trait("float080.capture.keep").equals(original.trait("float080.capture.keep")),"color/finish/history preserved");require(gearIntact(actor),"real equipment preserved");
    require(!vessel.isRemoved()&&vessel.getUUID().equals(vesselId)&&vessel.storedItem().getCount()==1,"one persistent physical toy payload/identity");require(vessel.getPassengers().size()<=1,"single occupancy");if(vessel.rider()!=null)require(vessel.rider()==actor&&vessel.claimedBy(actor),"passenger matches real claimant");
  }
  @SubscribeEvent public static void server(ServerTickEvent.Post event){
    if(!enabled()||phase<0||phase>=CASES.size()||paused||fatal!=null)return;
    try{
      var server=event.getServer();var level=server.overworld();var c=CASES.get(phase);long now=level.getGameTime();
      if(!settled){
        if(now-spawned<32)return;
        boolean dry=FloatToyRoutes080.safeFeet(level,actor,actor.position()),needsDry=c.push()||c.preview();
        var initial=new LinkedHashMap<String,Object>();initial.put("case",c.name());initial.put("physical_ticks",now-spawned);initial.put("actor",sample(actor));initial.put("actor_no_ai",actor.isNoAi());initial.put("actor_no_gravity",actor.isNoGravity());initial.put("actual_safe_dry_feet",dry);initial.put("actor_box",actor.getBoundingBox().toString());initial.put("actor_no_collision",level.noCollision(actor,actor.getBoundingBox()));initial.put("block_at_feet",level.getBlockState(actor.blockPosition()).toString());initial.put("block_below_feet",level.getBlockState(actor.blockPosition().below()).toString());initial.put("fluid_at_feet",level.getFluidState(actor.blockPosition()).toString());initial.put("float",sample(vessel));initial.put("float_phase",vessel.phase());initial.put("float_transition",vessel.transition());initial.put("float_claim",vessel.claimant().map(UUID::toString).orElse(""));initial.put("stored_payload_count",vessel.storedItem().getCount());initial.put("default_goal_classes",actor.goalSelector.getAvailableGoals().stream().map(g->g.getGoal().getClass().getName()).toList());
        write(OUT.resolve("settling").resolve(String.format("%02d.json",c.id())),initial);LogUtils.getLogger().info("FLOAT_CAPTURE080_SETTLED_CONTEXT {}",G.toJson(initial));
        require(needsDry?dry:actor.isInWater(),"fixture real physical support/water context; actor="+actor.position()+" groundLatch="+actor.onGround()+" safeFeet="+dry+" water="+actor.isInWater()+" floatPhase="+vessel.phase());
        // NoAI can leave its initial contact latch false without a travel step.
        // Seed it only after the real dry footprint has been proved, as the
        // existing native toy fixtures do. No actor position is changed.
        if(needsDry)actor.setOnGround(true);
        actor.setState(fixture(c));stateBefore=actor.state().encode();gearBefore=equipment(actor);actorPrevious=actor.position();floatPrevious=vessel.position();settled=true;pause(server,"initial");return;
      }
      if(!armed)return;elapsed=now-began;require(elapsed<c.timeout(),"production goal/egress timed out; seen="+seen);
      require(actor.position().distanceTo(actorPrevious)<.75&&vessel.position().distanceTo(floatPrevious)<.35,"bounded continuous physics; no remote snap/move");walked+=actor.position().distanceTo(actorPrevious);moved+=vessel.position().distanceTo(floatPrevious);actorPrevious=actor.position();floatPrevious=vessel.position();integrity(c);
      var row=new LinkedHashMap<String,Object>();row.put("elapsed",elapsed);row.put("actor",sample(actor));row.put("float",sample(vessel));row.put("phase",vessel.phase());row.put("transition",vessel.transition());row.put("claim",vessel.claimant().map(UUID::toString).orElse(""));row.put("toy_token",actor.toyPlayingKind());row.put("behavior",actor.getBehavior().name());trace.add(row);
      if(c.preview()&&ToyPreview.active(actor))require(actor.state().encode().equals(stateBefore),"native preview preserves care/state");
      if(c.activity().equals("preview_loop")&&elapsed>=220&&!stopRequested){command(server,"stop");stopRequested=true;}
      if(c.retry()&&vessel.rider()==actor&&vessel.phase()==FloatToyEntity080.RIDE){
        if(rideAt<0)rideAt=elapsed;
        if(elapsed-rideAt>=180&&!seen.contains("unsafe_shore_wait")){require(vessel.rider()==actor,"blocked shore retains safety");pause(server,"unsafe_shore_wait");return;}
        if(seen.contains("unsafe_shore_wait")&&!shoreOpened){for(var at:BlockPos.betweenClosed(6,80,-1,6,82,1))level.setBlock(at,Blocks.AIR.defaultBlockState(),3);shoreOpened=true;vessel.requestLand();}
      }
      if(c.push()&&vessel.phase()==FloatToyEntity080.PUSH&&vessel.position().subtract(floatOrigin).horizontalDistance()>.20&&!seen.contains("pushing")){pause(server,"pushing");return;}
      if(vessel.phase()==FloatToyEntity080.BOARD&&vessel.transition()>=.35f&&vessel.transition()<.80f&&!seen.contains("boarding")){pause(server,"boarding");return;}
      if(vessel.phase()==FloatToyEntity080.RIDE&&vessel.rider()==actor&&!seen.contains("riding")){pause(server,"riding");return;}
      if(vessel.phase()==FloatToyEntity080.EXIT&&vessel.transition()>=.35f&&vessel.transition()<.80f&&!seen.contains("exiting")){pause(server,"exiting");return;}
      if(seen.contains("riding")&&!actor.isPassenger()&&FloatToyRoutes080.safeFeet(level,actor,actor.position())&&(!c.preview()||!ToyPreview.active(actor))){
        require(!c.push()||seen.contains("pushing"),"natural launch actually pushed from land");require(!c.retry()||shoreOpened&&seen.contains("unsafe_shore_wait"),"unsafe-shore retry actually observed");require(seen.contains("boarding")&&seen.contains("exiting"),"smooth physical entry/egress sampled");require(!c.preview()||actor.toyPlayingTicks()==0&&actor.isNoAi(),"preview flags/AI restored before safe egress completes");complete=true;pause(server,"landed");
      }
    }catch(Throwable error){fatal=error.toString();write(OUT.resolve("failure.json"),Map.of("phase",phase,"error",fatal,"trace",trace));LogUtils.getLogger().error("FLOAT_CAPTURE080_SERVER_FAILURE",error);}
  }
  private static ChaoEntity observedActor(Minecraft mc){if(mc.level==null||actorId==null)return null;for(var entity:mc.level.entitiesForRendering())if(entity instanceof ChaoEntity c&&c.getUUID().equals(actorId))return c;return null;}
  private static FloatToyEntity080 observedFloat(Minecraft mc){if(mc.level==null||vesselId==null)return null;for(var entity:mc.level.entitiesForRendering())if(entity instanceof FloatToyEntity080 f&&f.getUUID().equals(vesselId))return f;return null;}
  private static List<UUID> passengerIds(Entity entity){return entity.getPassengers().stream().map(Entity::getUUID).toList();}
  private static void checkAdmission(Minecraft mc){
    long now=System.nanoTime();if(now-lastAdmissionDiagnostic<2_000_000_000L)return;lastAdmissionDiagnostic=now;
    var c=observedActor(mc);var f=observedFloat(mc);var data=new LinkedHashMap<String,Object>();data.put("case",CASES.get(phase).name());data.put("label",label);data.put("pause_serial",pauseSerial);data.put("paused_wall_seconds",(now-pausedAtNanos)/1e9);data.put("view",view<4?VIEWS.get(view):"complete");data.put("view_frames",viewFrames);data.put("render_candidates_this_pause",pauseCandidates);data.put("admitted_frames_this_pause",pauseAdmittedFrames);data.put("render_candidates_this_view",viewCandidates);data.put("admitted_frames_this_view",viewAdmittedFrames);data.put("last_render_gate",admissionReason);data.put("framing_radius",framingRadius);data.put("camera_distance",framingDistance);data.put("client_actor_present",c!=null);data.put("client_float_present",f!=null);data.put("server_actor",sample(actor));data.put("server_float",sample(vessel));data.put("server_phase",vessel.phase());data.put("server_transition",vessel.transition());data.put("server_passengers",passengerIds(vessel));data.put("camera_matches",mc.getCameraEntity()==camera);data.put("screen",mc.screen==null?"none":mc.screen.getClass().getName());data.put("overlay",mc.getOverlay()==null?"none":mc.getOverlay().getClass().getName());data.put("client_game_paused",mc.isPaused());
    if(c!=null){data.put("client_actor",sample(c));data.put("client_equipment_matches",gearIntact(c));}if(f!=null){data.put("client_float",sample(f));data.put("client_phase",f.phase());data.put("client_transition",f.transition());data.put("client_passengers",passengerIds(f));data.put("client_claim",f.claimant().map(UUID::toString).orElse(""));}
    write(OUT.resolve("admission").resolve(String.format("%02d-%s.json",CASES.get(phase).id(),label)),data);LogUtils.getLogger().info("FLOAT_CAPTURE080_ADMISSION {}",G.toJson(data));
    require(now-pausedAtNanos<30_000_000_000L,"paused capture admission exceeded30s at "+label+"; lastGate="+admissionReason+" candidates="+pauseCandidates+" admitted="+pauseAdmittedFrames+"; see admission diagnostic");
  }
  private static void camera(Minecraft mc){
    var c=observedActor(mc);var f=observedFloat(mc);if(c==null||f==null)return;if(camera==null||camera.level()!=mc.level){camera=new ArmorStand(mc.level,0,0,0);camera.setInvisible(true);camera.setNoGravity(true);}
    var forward=Vec3.directionFromRotation(0,f.getYRot());var side=new Vec3(-forward.z,0,forward.x);var a=c.getBoundingBox();var b=f.getBoundingBox();
    framingBounds=new net.minecraft.world.phys.AABB(Math.min(a.minX,b.minX),Math.min(a.minY,b.minY),Math.min(a.minZ,b.minZ),Math.max(a.maxX,b.maxX),Math.max(a.maxY,b.maxY),Math.max(a.maxZ,b.maxZ));var focus=framingBounds.getCenter();
    framingRadius=new Vec3(framingBounds.maxX-framingBounds.minX,framingBounds.maxY-framingBounds.minY,framingBounds.maxZ-framingBounds.minZ).length()*.5;
    // A sphere of radius r fits a45-degree vertical FOV at r/sin22.5.
    // Include both real boxes for deep rescue approaches, retaining the same
    // close minimum distance when the actor and float are seated together.
    framingDistance=Math.max(2.65,framingRadius/Math.sin(Math.toRadians(22.5))*1.12);
    var direction=switch(view){case 0->forward.add(side.scale(.25)).add(0,.40,0);case 1->side.add(forward.scale(.035)).add(0,.26,0);case 2->forward.scale(-1).add(side.scale(-.17)).add(0,.40,0);default->forward.scale(.04).add(side.scale(.07)).add(0,1,0);};var eye=focus.add(direction.normalize().scale(framingDistance));
    var delta=focus.subtract(eye);float yaw=(float)(Math.toDegrees(Math.atan2(delta.z,delta.x))-90),pitch=(float)-Math.toDegrees(Math.atan2(delta.y,Math.hypot(delta.x,delta.z)));
    camera.moveTo(eye.x,eye.y-camera.getEyeHeight(),eye.z,yaw,pitch);camera.xo=camera.xOld=camera.getX();camera.yo=camera.yOld=camera.getY();camera.zo=camera.zOld=camera.getZ();camera.yRotO=yaw;camera.xRotO=pitch;camera.yHeadRotO=yaw;camera.setYHeadRot(yaw);mc.options.setCameraType(CameraType.FIRST_PERSON);mc.setCameraEntity(camera);
  }
  @SubscribeEvent public static void restore(ClientTickEvent.Pre event){if(enabled()){var mc=Minecraft.getInstance();if(mc.player!=null&&mc.getCameraEntity()!=mc.player)mc.setCameraEntity(mc.player);}}
  @SubscribeEvent public static void frameStart(RenderFrameEvent.Pre event){if(enabled()){worldFrame=actorFrame=false;}}
  @SubscribeEvent public static void rendered(RenderLivingEvent.Post<?,?> event){
    if(!enabled()||!paused||phase<0||phase>=CASES.size()||SceneShaderPack.shadows()||!(event.getEntity() instanceof ChaoEntity c)||!c.getUUID().equals(actorId))return;
    pauseCandidates++;viewCandidates++;var mc=Minecraft.getInstance();var f=observedFloat(mc);
    if(f==null){admissionReason="client float missing";return;}if(mc.screen!=null||mc.getOverlay()!=null||mc.getCameraEntity()!=camera){admissionReason="screen/overlay/camera not ready";return;}
    if(f.phase()!=vessel.phase()){admissionReason="float phase mismatch "+f.phase()+"/"+vessel.phase();return;}
    if(Math.abs(f.transition()-vessel.transition())>.18){admissionReason="transition mismatch "+f.transition()+"/"+vessel.transition();return;}
    if(!passengerIds(f).equals(passengerIds(vessel))){admissionReason="passenger identity mismatch "+passengerIds(f)+"/"+passengerIds(vessel);return;}
    if(!Objects.equals(c.getVehicle()==null?null:c.getVehicle().getUUID(),actor.getVehicle()==null?null:actor.getVehicle().getUUID())){admissionReason="actor vehicle mismatch";return;}
    if(!gearIntact(c)){admissionReason="equipment mismatch";return;}admissionReason="admitted";pauseAdmittedFrames++;viewAdmittedFrames++;
    require(event.getRenderer().getModel() instanceof ChaoModel,"actual production Chao model");var model=(ChaoModel)event.getRenderer().getModel();require(model.officialPosesFinite(),"finite poses");
    var p=new JsonObject();p.addProperty("actual_production_living_render",true);p.addProperty("renderer",event.getRenderer().getClass().getName());p.addProperty("float_renderer",mc.getEntityRenderDispatcher().getRenderer(f).getClass().getName());p.addProperty("float_renderer_scope","Actual loaded tracked entity, registered production dispatcher and AFTER_LEVEL PNG; no nonliving draw-call interception.");p.addProperty("actor_uuid",c.getUUID().toString());p.addProperty("float_uuid",f.getUUID().toString());p.addProperty("actor_state",c.state().encode());p.addProperty("actor_state_sha256",sha(c.state().encode()));p.add("equipment",gearProof(c));p.addProperty("gameplay_geometry",model.gameplayFormKey());p.addProperty("official_geometry",model.officialGeometryKey());p.add("skull_bounds",G.toJsonTree(model.accessoryBounds("skull")));p.addProperty("motion",model.officialMotion().name());p.addProperty("finite_poses",true);p.add("actor",G.toJsonTree(sample(c)));p.add("float",G.toJsonTree(sample(f)));p.addProperty("kind",f.kind());p.addProperty("claim",f.claimant().map(UUID::toString).orElse(""));p.addProperty("float_phase",f.phase());p.addProperty("transition",f.transition());p.addProperty("passengers",f.getPassengers().size());p.addProperty("toy_token",c.toyPlayingKind());p.addProperty("toy_remaining",c.toyPlayingTicks());p.addProperty("partial_tick",event.getPartialTick());p.addProperty("entity_packed_light",event.getPackedLight());p.addProperty("default_goal_selector_retained",true);p.addProperty("shaders_active",SceneShaderPack.active());var face=model.resolvedFace076();if(face!=null){p.addProperty("eyes",face.eyes().name());p.addProperty("mouth",face.mouth().name());p.addProperty("face_source",face.eyeSource().name());}proof=p;actorFrame=true;actorFrames++;
  }
  @SubscribeEvent public static void world(RenderLevelStageEvent event){if(enabled()&&event.getStage()==RenderLevelStageEvent.Stage.AFTER_LEVEL&&actorFrame&&!SceneShaderPack.shadows()){worldFrame=true;worldFrames++;}}
  private static void capture(Minecraft mc){
    var c=CASES.get(phase);var p=proof.deepCopy();p.addProperty("case_index",c.id());p.addProperty("case",c.name());p.addProperty("activity",c.activity());p.addProperty("natural_behavior",!c.preview());p.addProperty("label",label);p.addProperty("view",VIEWS.get(view));p.addProperty("server_elapsed",elapsed);p.addProperty("simulation_paused_for_matched_angles",true);p.addProperty("source_freeze",System.getenv().getOrDefault("CHAO_TOYS080_SOURCE_FREEZE","unrecorded"));p.addProperty("walked_blocks",walked);p.addProperty("float_moved_blocks",moved);p.addProperty("framing_bounds",framingBounds.toString());p.addProperty("framing_radius",framingRadius);p.addProperty("camera_distance",framingDistance);p.addProperty("vertical_fov_degrees",45);var camera=mc.gameRenderer.getMainCamera();var eye=camera.getPosition();p.add("actual_camera_position",G.toJsonTree(List.of(eye.x,eye.y,eye.z)));p.addProperty("actual_camera_yaw",camera.getYRot());p.addProperty("actual_camera_pitch",camera.getXRot());
    String name=String.format("float080-%02d-%s-%s-%s.png",c.id(),c.name(),label,VIEWS.get(view));Path png=OUT.resolve("screenshots").resolve(name);
    Screenshot.grab(OUT.toFile(),name,mc.getMainRenderTarget(),message->{try{require(Files.isRegularFile(png),"actual PNG exists");p.addProperty("png",png.toString());p.addProperty("png_sha256",sha(Files.readAllBytes(png)));write(OUT.resolve("frames").resolve(name.replace(".png",".json")),p);SAVED.incrementAndGet();}catch(Exception error){fatal=error.toString();}});shots++;view++;viewFrames=viewCandidates=viewAdmittedFrames=0;admissionReason="waiting for "+(view<4?VIEWS.get(view):"view completion");
  }
  @SubscribeEvent public static void frameEnd(RenderFrameEvent.Post event){
    if(!enabled()||!paused||phase<0||phase>=CASES.size()||!worldFrame||localPhase!=phase||localPause!=pauseSerial)return;var mc=Minecraft.getInstance();
    viewFrames++;if(view<4){if(viewFrames>=10&&proof!=null)capture(mc);return;}if(completionRequested)return;completionRequested=true;int expected=phase,serial=pauseSerial;
    task(mc.getSingleplayerServer(),()->{if(phase!=expected||pauseSerial!=serial)return;
      if(complete){var result=new LinkedHashMap<String,Object>();result.put("case",CASES.get(phase));result.put("status","PASS");result.put("sampled_phases",List.copyOf(seen));result.put("actor_frames",actorFrames);result.put("world_frames",worldFrames);result.put("walked_blocks",walked);result.put("float_moved_blocks",moved);result.put("default_goals_retained",true);result.put("actual_remote_multiplayer",false);result.put("trace",List.copyOf(trace));write(OUT.resolve("complete").resolve(String.format("%02d.json",CASES.get(phase).id())),result);completedCases++;if(phase+1<CASES.size())next(mc.getSingleplayerServer(),phase+1);else phase=CASES.size();
      }else if(!armed)arm(mc.getSingleplayerServer());else{paused=false;mc.getSingleplayerServer().tickRateManager().setFrozen(false);}completionRequested=false;
    });
  }
  @SubscribeEvent public static void client(ClientTickEvent.Post event){
    if(!enabled()||finished)return;var mc=Minecraft.getInstance();if(fatal!=null)throw new IllegalStateException(fatal);if(wallStart==0)wallStart=System.nanoTime();require(System.nanoTime()-wallStart<5_400_000_000_000L,"bounded 90min wall deadline");
    if(mc.screen instanceof AccessibilityOnboardingScreen)mc.setScreen(new TitleScreen());
    if(!started&&mc.screen instanceof TitleScreen){started=true;require(!Files.exists(OUT.resolve("case-catalog.json")),"fresh capture output required");write(OUT.resolve("case-catalog.json"),Map.of("scope","Actual production floats/default goals; phase-paused matched angles. Not remote multiplayer or a claim about original-game accuracy.","cases",CASES,"source_freeze",System.getenv().getOrDefault("CHAO_TOYS080_SOURCE_FREEZE","unrecorded")));mc.options.renderDistance().set(3);mc.options.simulationDistance().set(5);mc.options.framerateLimit().set(45);mc.options.fov().set(45);mc.options.gamma().set(.5);mc.options.pauseOnLostFocus=false;mc.options.hideGui=true;mc.options.tutorialStep=net.minecraft.client.tutorial.TutorialSteps.NONE;mc.getWindow().setWindowed(1440,1000);mc.resizeDisplay();mc.createWorldOpenFlows().createFreshLevel("Float080-"+System.currentTimeMillis(),new LevelSettings("Float080",GameType.CREATIVE,false,Difficulty.PEACEFUL,true,new GameRules(),WorldDataConfiguration.DEFAULT),new WorldOptions(80804L,false,false),access->access.registryOrThrow(Registries.WORLD_PRESET).getHolderOrThrow(WorldPresets.FLAT).value().createWorldDimensions(),mc.screen);}
    if(!setup&&mc.player!=null&&mc.getSingleplayerServer()!=null){setup=true;mc.setScreen(null);ChaoRenderer.suppressNames=true;task(mc.getSingleplayerServer(),()->{var server=mc.getSingleplayerServer();var level=server.overworld();level.getGameRules().getRule(GameRules.RULE_DAYLIGHT).set(false,server);level.getGameRules().getRule(GameRules.RULE_WEATHER_CYCLE).set(false,server);level.getGameRules().getRule(GameRules.RULE_DOMOBSPAWNING).set(false,server);next(server,0);});}
    if(phase>=0&&phase<CASES.size()&&mc.level!=null){if(localPhase!=phase){localPhase=phase;localPause=-1;actorFrames=worldFrames=0;completionRequested=false;}if(paused){
      // A new pause resets the camera without requiring a prior admitted frame.
      // Otherwise the old phase's terminal view can block the reset itself.
      if(localPause!=pauseSerial){localPause=pauseSerial;view=viewFrames=pauseCandidates=pauseAdmittedFrames=viewCandidates=viewAdmittedFrames=0;proof=null;admissionReason="waiting for first production render";lastAdmissionDiagnostic=0;}
      camera(mc);checkAdmission(mc);
    }}
    if(phase==CASES.size()&&SAVED.get()==shots){require(completedCases==CASES.size(),"all requested physical cases completed");finished=true;write(OUT.resolve("visual-complete.json"),Map.of("cases",completedCases,"pngs",SAVED.get(),"actual_production_models",true,"actual_remote_multiplayer",false,"status","PASS"));task(mc.getSingleplayerServer(),()->mc.getSingleplayerServer().tickRateManager().setFrozen(false));mc.stop();}
  }
}
