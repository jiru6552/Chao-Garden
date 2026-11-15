package com.chaogarden.client;

import com.chaogarden.ChaoGarden;
import com.chaogarden.companion.*;
import com.chaogarden.core.*;
import com.chaogarden.item.ChaoCarrierItem;
import com.chaogarden.entity.ChaoEntity;
import com.chaogarden.toys.*;
import com.google.gson.*;
import com.mojang.logging.LogUtils;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.MessageDigest;
import java.util.*;
import net.minecraft.client.*;
import net.minecraft.client.gui.screens.*;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.resolver.ServerAddress;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.Cow;
import net.minecraft.world.item.*;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.*;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.*;
import net.neoforged.neoforge.event.entity.living.LivingKnockBackEvent;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/** PRIVATE DEVELOPMENT PROTOTYPE. Actual two ordinary clients and a LAN server.
 * Native key mappings exercise the shipping input handler and attack packets.
 * Fixture setup may equip/teleport; it NEVER calls the cycle or impulse handler.
 * No player-visible explanatory text, unlocks or percentages are introduced.
 * Name deliberately matches the existing *VisualSmoke* release exclusion. */
@EventBusSubscriber(modid=ChaoGarden.MODID,value=Dist.CLIENT)
public final class ToyRemoteVisualSmoke080 {
  private record Case(int id,String name,String kind,int before,int expected) {
    boolean input(){return kind.startsWith("input_");}
    boolean movement(){return kind.equals("travel");}
    boolean power(){return kind.startsWith("power_");}
    boolean rejoin(){return kind.equals("rejoin");}
    boolean floating(){return kind.startsWith("float_");}
    String controller(){return power()?"host":"observer";}
    int stat(){return power()?ChaoState.POWER:ChaoState.RUN;}
  }
  private static final Gson G=new GsonBuilder().setPrettyPrinting().create();
  private static final String ROLE=System.getenv().getOrDefault("CHAO_TOYS080_REMOTE_ROLE","");
  private static final Path DIR=Path.of(System.getenv().getOrDefault("CHAO_TOYS080_REMOTE_SHARED","/workspace/.setup/toy080-remote-unconfigured/shared"));
  private static final int PORT=Integer.parseInt(System.getenv().getOrDefault("CHAO_TOYS080_REMOTE_PORT","25582"));
  private static final List<Case> CASES=cases();
  private static int localTicks,clientCase=-1,activeTicks,index=-1;private static long prepared,began=-1,reconnectAt;
  private static boolean started,setup,published,done,disconnected,reconnected,shieldSeen,attacked;
  private static volatile String fatal;
  private static UUID runChao,victimUuid;private static Cow target;
  private static double serverPeak,serverMaxDx,lastVictimX,startVictimX,serverMinAfterPeak=Double.POSITIVE_INFINITY,serverWorstBackward,runMaxDistance,serverRunMaxDistance;
  private static int serverAttacks,knockbackCalls;private static long hitAt=-1;
  private static Vec3 capturedNativeImpulse;private static Map<String,Object> capturedNativeImpulseProof;
  private static double clientPeak,clientOtherPeak,clientStartX,clientEndX,clientMaxDx,clientLastX,clientWorstSignedDx;
  private static final List<Map<String,Object>> serverTrace=new ArrayList<>(),clientTrace=new ArrayList<>();
  private static Map<String,Object> catalog;
  private record MotionSample(long tick,double x,double speed) {}
  private static ChaoEntity floatActor;private static FloatToyEntity080 floatToy;private static UUID floatActorId,floatToyId;
  private static boolean floatRode,floatExited,floatPickupOffered,floatFinished;private static int pickupOfferedAt=-1;
  private static final Set<Integer> clientFloatPhases=new TreeSet<>();private static boolean clientFloatPassenger;private static int clientPickupAt=-1;
  private static int clientPickupSentAt=-1,clientFloatRenderFrames;private static JsonObject clientFloatRenderProof;
  private static boolean baselineSent;private static long baselineSentAt;
  private static final List<JsonObject> baselineObservations=new ArrayList<>();

  private static boolean enabled(){return ROLE.equals("host")||ROLE.equals("observer");}
  private static List<Case> cases(){
    var cases=new ArrayList<Case>();int id=0;
    cases.add(new Case(id++,"run_press_hold_40_ticks","input_empty",1000,900));
    cases.add(new Case(id++,"run_release_then_repress","input_empty",900,800));
    cases.add(new Case(id++,"offhand_shield_ordinary_use","input_shield",800,800));
    cases.add(new Case(id++,"not_crouching","input_standing",800,800));
    cases.add(new Case(id++,"block_use","input_block",800,800));
    cases.add(new Case(id++,"entity_use","input_entity",800,800));
    for(int before=800;before>=0;before-=100)cases.add(new Case(id++,"run_cycle_"+before,"input_empty",before,before==0?1000:before-100));
    cases.add(new Case(id++,"run_rejoin_prepare","input_empty",1000,900));
    cases.add(new Case(id++,"actual_remote_disconnect_rejoin","rejoin",900,900));
    cases.add(new Case(id++,"run_native_ground_movement_max","travel",1000,1000));
    cases.add(new Case(id++,"run_native_ground_movement_zero","travel",0,0));
    cases.add(new Case(id++,"power_normal_real_player_attack","power_normal",1000,1000));
    cases.add(new Case(id++,"power_sprint_knockback_ii_real_player_attack","power_enchanted",1000,1000));
    cases.add(new Case(id++,"power_zero_real_player_attack","power_zero",0,0));
    cases.add(new Case(id++,"float_duck_natural_rescue_remote_empty_pickup","float_empty",0,0));
    cases.add(new Case(id++,"float_ring_native_ride_remote_occupied_pickup","float_occupied",0,0));
    return List.copyOf(cases);
  }
  private static void check(boolean condition,String message){if(!condition)throw new IllegalStateException("REMOTE080 "+message);}
  private static String sha(String text){try{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(text.getBytes(StandardCharsets.UTF_8)));}catch(Exception e){throw new IllegalStateException(e);}}
  private static synchronized void write(String name,Object value){try{Files.createDirectories(DIR);Path path=DIR.resolve(name),tmp=DIR.resolve(name+"."+ROLE+".tmp");Files.writeString(tmp,G.toJson(value)+"\n");Files.move(tmp,path,StandardCopyOption.REPLACE_EXISTING,StandardCopyOption.ATOMIC_MOVE);}catch(Exception e){throw new IllegalStateException(e);}}
  private static JsonObject read(String name){try{return JsonParser.parseString(Files.readString(DIR.resolve(name))).getAsJsonObject();}catch(java.nio.file.NoSuchFileException missing){return null;}catch(Exception e){throw new IllegalStateException(e);}}
  private static boolean exists(String name){return Files.isRegularFile(DIR.resolve(name));}
  private static String roleName(String role){return role.equals("host")?"Toy80Host":"Toy80Observer";}
  private static ServerPlayer player(MinecraftServer server,String role){return server.getPlayerList().getPlayers().stream().filter(p->p.getGameProfile().getName().equals(roleName(role))).findFirst().orElse(null);}
  private static ItemStack carrier(ServerPlayer player){return CompanionVisualSmokeSupport.held(player);}
  private static int percent(ServerPlayer player,int stat){var item=carrier(player);check(ChaoCarrierItem.filled(item),"actual server slot lost its carrier");return EquippedAbilityRules080.percent(ChaoCarrierItem.state(item),stat);}
  private static ChaoState fixture(int stat,int value){
    var state=ChaoState.egg(0,false,true,0,new Random(808000+stat));state.adult=true;state.align=stat==ChaoState.RUN?-1:1;state.primaryType=stat+1;
    state.grades[stat]=5;state.gradeA[stat]=state.gradeB[stat]=5;check(state.useHyperFruit(stat),"actual expressed X fixture");
    state.levels[stat]=99;state.points[stat]=9900;state.energy=100;state.hunger=100;state.happiness=60;state.setEvolutionStrength(1.2f);
    if(value!=1000)check(state.setTrait(stat==ChaoState.RUN?EquippedAbilityRules080.RUN_SETTING:EquippedAbilityRules080.POWER_SETTING,value),"persist explicit fixture setting");
    var restored=ChaoState.decode(state.encode());check(EquippedAbilityRules080.eligible(restored,stat),"fixture exact locked specialization and rank");return restored;
  }
  private static void pose(ServerPlayer p,boolean spectator,double x,double z,float yaw,float pitch){
    p.setGameMode(spectator?GameType.SPECTATOR:GameType.SURVIVAL);p.getInventory().clearContent();p.setItemInHand(InteractionHand.MAIN_HAND,ItemStack.EMPTY);p.setItemInHand(InteractionHand.OFF_HAND,ItemStack.EMPTY);
    p.stopUsingItem();p.setSprinting(false);p.setShiftKeyDown(false);p.setDeltaMovement(Vec3.ZERO);p.fallDistance=0;p.invulnerableTime=0;
    // A fixture teleport resets position, but remote player trackers can
    // retain the prior native motion field. Publish this deliberate baseline
    // zero to real tracking/self clients before the24-tick ready window.
    // This is only fixture preparation: no custom ability packet/handler and
    // no post-hit correction are used to satisfy the motion assertions.
    p.serverLevel().getChunkSource().broadcastAndSend(p,new net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket(p));
    p.getAttribute(Attributes.MAX_HEALTH).setBaseValue(1000);p.setHealth(1000);p.getFoodData().setFoodLevel(20);p.connection.teleport(x,80,z,yaw,pitch);
  }
  private static Map<String,Object> sample(Entity e){var out=new LinkedHashMap<String,Object>();out.put("uuid",e.getUUID().toString());out.put("id",e.getId());out.put("position",List.of(e.getX(),e.getY(),e.getZ()));out.put("velocity",List.of(e.getDeltaMovement().x,e.getDeltaMovement().y,e.getDeltaMovement().z));out.put("on_ground",e.onGround());return out;}
  /** Excluded helper-only read of the production immutable after-knockback
   * snapshot. No field or game state is written; reflection is cached lazily. */
  private static final class ImpactRead080 {
    static final java.lang.reflect.Field IMPACTS,VICTIM,VECTOR,EXPIRES,DIMENSION,ARMED,ORDER;
    static {try{var owner=EquippedKnockback080.class;var impact=Class.forName(owner.getName()+"$Impact");
      IMPACTS=field(owner,"IMPACTS");VICTIM=field(impact,"victim");VECTOR=field(impact,"vector");EXPIRES=field(impact,"expires");DIMENSION=field(impact,"dimension");ARMED=field(impact,"armed");ORDER=field(impact,"order");
    }catch(ReflectiveOperationException e){throw new ExceptionInInitializerError(e);}}
    private static java.lang.reflect.Field field(Class<?> owner,String name)throws ReflectiveOperationException{var field=owner.getDeclaredField(name);field.setAccessible(true);return field;}
  }
  private static void captureNativeImpulse(ServerPlayer victim){
    if(capturedNativeImpulse!=null||hitAt<0)return;
    try{
      var impacts=(Map<?,?>)ImpactRead080.IMPACTS.get(null);var impact=impacts.get(victim.getUUID());if(impact==null)return;
      long time=victim.server.overworld().getGameTime(),expires=ImpactRead080.EXPIRES.getLong(impact),created=expires-EquippedKnockback080.TRACK_TICKS;
      Object actualVictim=ImpactRead080.VICTIM.get(impact),dimension=ImpactRead080.DIMENSION.get(impact);boolean armed=ImpactRead080.ARMED.getBoolean(impact);
      if(actualVictim!=victim||!victim.isAlive()||victim.isRemoved()||!victim.getUUID().equals(victimUuid)||!victim.level().dimension().equals(dimension)||!armed||time>expires||created>time||created<hitAt)return;
      var vector=(Vec3)ImpactRead080.VECTOR.get(impact);check(Double.isFinite(vector.x)&&Double.isFinite(vector.y)&&Double.isFinite(vector.z),"immutable native impulse is finite");
      capturedNativeImpulse=vector;
      var proof=new LinkedHashMap<String,Object>();proof.put("source","excluded helper read-only cached reflection of immutable production Impact.vector after native knockback");proof.put("victim_uuid",victim.getUUID().toString());proof.put("same_actual_victim_object",true);proof.put("dimension",victim.level().dimension().location().toString());proof.put("armed",armed);proof.put("created_tick",created);proof.put("expires_tick",expires);proof.put("observed_server_tick",time);proof.put("case_hit_tick",hitAt);proof.put("order",ImpactRead080.ORDER.getLong(impact));proof.put("vector",List.of(vector.x,vector.y,vector.z));
      capturedNativeImpulseProof=Collections.unmodifiableMap(proof);serverTrace.add(Map.of("server_tick",time,"captured_native_impulse",capturedNativeImpulseProof));
    }catch(ReflectiveOperationException e){throw new IllegalStateException("Excluded native-impulse diagnostic reflection failed",e);}
  }
  private static JsonObject decayAudit(JsonArray rows,String entityKey,String tickKey,double exact,boolean velocityExpected){
    var samples=new ArrayList<MotionSample>();
    for(var element:rows){var row=element.getAsJsonObject();if(!row.has(entityKey)||!row.has(tickKey))continue;var entity=row.getAsJsonObject(entityKey);var position=entity.getAsJsonArray("position");var velocity=entity.getAsJsonArray("velocity");samples.add(new MotionSample(row.get(tickKey).getAsLong(),position.get(0).getAsDouble(),Math.hypot(velocity.get(0).getAsDouble(),velocity.get(2).getAsDouble())));}
    check(samples.size()>=30,"actual single-impulse trace has sufficient post-hit coverage");
    int peakAt=0;for(int i=1;i<samples.size();i++)if(samples.get(i).speed()>samples.get(peakAt).speed())peakAt=i;
    var first=samples.getFirst();var last=samples.getLast();var peak=samples.get(peakAt);double total=last.x()-first.x(),tailMin=Double.POSITIVE_INFINITY,tailMax=Double.NEGATIVE_INFINITY;
    for(int i=Math.max(0,samples.size()-20);i<samples.size();i++){tailMin=Math.min(tailMin,samples.get(i).x());tailMax=Math.max(tailMax,samples.get(i).x());}
    // .91 is the maximum ordinary horizontal retention here (air). The
    // smooth-stone ground runway decays faster. Two ticks admit packet/physics
    // ordering; sustained retransmission or renewed acceleration still fails.
    double largestExcess=Double.NEGATIVE_INFINITY;
    if(velocityExpected)for(int i=peakAt;i<samples.size();i++){var current=samples.get(i);double envelope=peak.speed()*Math.pow(.91,Math.max(0,current.tick()-peak.tick()-2))+.08;largestExcess=Math.max(largestExcess,current.speed()-envelope);}
    check(!velocityExpected||largestExcess<=.000001,"single impulse follows a decaying upper envelope instead of repeated acceleration; excess="+largestExcess);
    double totalLimit=exact/.09+2*exact+2;
    check(total>0&&total<totalLimit,"bounded authoritative/native total travel after one impulse, actual="+total+" upper="+totalLimit);
    check(last.speed()<.08&&tailMax-tailMin<.15,"impulse settles: last horizontal speed="+last.speed()+" final20-tick drift="+(tailMax-tailMin));
    var result=new JsonObject();result.addProperty("samples",samples.size());result.addProperty("peak_horizontal_speed",peak.speed());result.addProperty("total_x_displacement",total);result.addProperty("total_displacement_upper_bound",totalLimit);result.addProperty("ending_horizontal_speed",last.speed());result.addProperty("final20_tick_x_drift",tailMax-tailMin);result.addProperty("decay_envelope_checked",velocityExpected);if(velocityExpected)result.addProperty("largest_decay_envelope_excess",largestExcess);result.addProperty("envelope_tick_allowance",2);return result;
  }
  private static void phase(String stage){var c=CASES.get(index);var data=new LinkedHashMap<String,Object>();data.put("case",index);data.put("name",c.name());data.put("kind",c.kind());data.put("stage",stage);data.put("controller",c.controller());data.put("before",c.before());data.put("expected",c.expected());data.put("baseline_epoch",baselineSent?1:0);data.put("run_chao_uuid",runChao==null?"":runChao.toString());data.put("victim_uuid",victimUuid==null?"":victimUuid.toString());if(c.floating()){data.put("float_actor_uuid",floatActorId.toString());data.put("float_toy_uuid",floatToyId.toString());data.put("pickup_offered",floatPickupOffered);}write("phase.json",data);}
  private static JsonObject tracked(JsonObject ready,UUID id){if(ready==null||!ready.has("tracked_players"))return null;for(var item:ready.getAsJsonArray("tracked_players")){var entity=item.getAsJsonObject();if(entity.get("uuid").getAsString().equals(id.toString()))return entity;}return null;}
  private static boolean baselineEntity(JsonObject entity,Vec3 expected){
    if(entity==null)return false;var p=entity.getAsJsonArray("position");var v=entity.getAsJsonArray("velocity");
    var actual=new Vec3(p.get(0).getAsDouble(),p.get(1).getAsDouble(),p.get(2).getAsDouble());double speed=Math.hypot(v.get(0).getAsDouble(),v.get(2).getAsDouble());
    return actual.distanceTo(expected)<=.05&&speed<=.05;
  }
  private static boolean baselinePosition(JsonObject entity,Vec3 expected){if(entity==null)return false;var p=entity.getAsJsonArray("position");return new Vec3(p.get(0).getAsDouble(),p.get(1).getAsDouble(),p.get(2).getAsDouble()).distanceTo(expected)<=.05;}
  private static boolean powerBaseline(MinecraftServer server,Case c,long time){
    var host=player(server,"host");var remote=player(server,"observer");var h=read("host-case-"+index+"-ready.json");var o=read("observer-case-"+index+"-ready.json");
    var hostExpected=new Vec3(0,80,0);var victimExpected=new Vec3(c.kind().equals("power_enchanted")?2.8:1.8,80,0);
    if(!baselineSent){
      // Both peers have actually tracked both player entities before this one
      // ordinary reset wave. A pre-teleport tracking-range broadcast can miss
      // the observer, leaving a prior RemotePlayer motion field.
      boolean returned=h!=null&&o!=null&&baselinePosition(h.getAsJsonObject("position"),hostExpected)&&baselinePosition(o.getAsJsonObject("position"),victimExpected)&&baselinePosition(tracked(h,remote.getUUID()),victimExpected)&&baselinePosition(tracked(o,host.getUUID()),hostExpected);
      write("baseline-case-"+index+"-tracking.json",Map.of("case",index,"stage","PREPARED","server_tick",time,"actual_both_peers_returned_to_nominal",returned,"host_ready",h==null?new JsonObject():h,"observer_ready",o==null?new JsonObject():o));
      if(!returned)return false;
      for(var receiver:List.of(host,remote))for(var targetPlayer:List.of(host,remote))receiver.connection.send(new net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket(targetPlayer.getId(),Vec3.ZERO));
      baselineSent=true;baselineSentAt=time;phase("prepared");write("baseline-case-"+index+"-reset.json",Map.of("case",index,"stage","PREPARED","server_tick",time,"ordinary_native_reset_wave",true,"custom_ability_packet",false,"host_before",h,"observer_before",o));return false;
    }
    if(h==null||o==null||!h.has("baseline_epoch")||!o.has("baseline_epoch")||h.get("baseline_epoch").getAsInt()!=1||o.get("baseline_epoch").getAsInt()!=1)return false;
    boolean ownHost=baselineEntity(h.getAsJsonObject("position"),hostExpected),ownVictim=baselineEntity(o.getAsJsonObject("position"),victimExpected),trackedVictim=baselineEntity(tracked(h,remote.getUUID()),victimExpected),trackedHost=baselineEntity(tracked(o,host.getUUID()),hostExpected);
    var observation=new JsonObject();observation.addProperty("server_tick",time);observation.addProperty("ticks_since_reset",time-baselineSentAt);observation.addProperty("host_own_ready",ownHost);observation.addProperty("victim_own_ready",ownVictim);observation.addProperty("host_tracked_victim_ready",trackedVictim);observation.addProperty("victim_tracked_host_ready",trackedHost);observation.add("host_ready",h.deepCopy());observation.add("observer_ready",o.deepCopy());baselineObservations.add(observation);
    write("baseline-case-"+index+"-observations.json",Map.of("case",index,"stage","PREPARED","position_tolerance",.05,"horizontal_speed_tolerance",.05,"observations",List.copyOf(baselineObservations)));
    boolean ready=time-baselineSentAt>=8&&ownHost&&ownVictim&&trackedVictim&&trackedHost;
    if(ready)write("baseline-case-"+index+"-pass.json",Map.of("case",index,"stage","PREPARED","status","PASS","ordinary_reset_only",true,"position_tolerance",.05,"horizontal_speed_tolerance",.05,"final_observation",observation));
    return ready;
  }
  private static void prepareFloat(MinecraftServer server,Case c){
    var level=server.overworld();var host=player(server,"host");var remote=player(server,"observer");CompanionVisualSmokeSupport.clear(host);CompanionVisualSmokeSupport.clear(remote);
    pose(host,true,c.kind().equals("float_occupied")?-7:3,c.kind().equals("float_occupied")?3:5,145,25);pose(remote,true,-3,5,-145,25);
    for(var at:BlockPos.betweenClosed(-9,75,-9,9,84,9)){boolean pool=at.getX()>=-5&&at.getX()<=5&&at.getZ()>=-5&&at.getZ()<=5&&at.getY()==79;level.setBlock(at,at.getY()>=80?Blocks.AIR.defaultBlockState():pool?Blocks.WATER.defaultBlockState():Blocks.SMOOTH_STONE.defaultBlockState(),3);}
    var state=new ChaoState();state.adult=c.kind().equals("float_occupied");state.align=state.adult?-1:1;state.primaryType=state.adult?ChaoState.RUN_TYPE:ChaoState.NORMAL;state.hunger=100;state.energy=100;state.happiness=30;state.boredom=100;state.personality=ChaoPreferences.EASILY_BORED;state.points[ChaoState.RUN]=800;state.shiny=state.adult;state.expressedColor=state.adult?4:0;if(state.adult)state.setEvolutionStrength(1.2f);
    floatActor=ChaoGarden.CHAO.get().create(level);check(floatActor!=null,"create actual LAN float Chao");floatActor.setState(ChaoState.decode(state.encode()));floatActor.setInvulnerable(true);floatActor.setNoAi(true);floatActor.deferBehavior(100000);floatActor.getRandom().setSeed(18);floatActor.owner=host.getUUID();floatActor.moveTo(state.adult?-5.5:-2.5,state.adult?80:79.65,.5,0,0);floatActor.setItemSlot(EquipmentSlot.HEAD,new ItemStack(ChaoGarden.item("red_wool_beanie")));floatActor.setItemSlot(EquipmentSlot.OFFHAND,new ItemStack(ChaoGarden.item("round_glasses_accessory")));check(level.addFreshEntity(floatActor),"spawn actual Chao with default goals");floatActorId=floatActor.getUUID();
    if(state.adult){check(FloatToyRoutes080.safeFeet(level,floatActor,floatActor.position()),"actual dry bank support before initializing native-preview contact");floatActor.setOnGround(true);}
    String kind=state.adult?"chao_lifesaver":"chao_rubber_duck";floatToy=FloatToyContent080.newCandidate(level,kind,new ItemStack(ChaoGarden.item(kind)),new Vec3(state.adult?-4.5:1.5,79.95,.5),0);check(floatToy!=null&&level.addFreshEntity(floatToy),"publish actual LAN float once");floatToy.commit();floatToyId=floatToy.getUUID();floatRode=floatExited=floatPickupOffered=floatFinished=false;pickupOfferedAt=-1;
  }
  private static void offerPickup(MinecraftServer server,Case c){
    var p=player(server,"observer");Vec3 at;
    if(c.kind().equals("float_occupied"))at=new Vec3(-5.5,80,2);
    else{
      floatActor.setStaying(true);at=null;for(var d:net.minecraft.core.Direction.Plane.HORIZONTAL){var candidate=floatActor.position().add(d.getStepX(),0,d.getStepZ());if(FloatToyRoutes080.safeFeet(server.overworld(),floatActor,candidate)&&candidate.distanceTo(floatToy.position())<3){at=candidate;break;}}
      check(at!=null,"safe dry operator fixture exists after the real natural landing");
    }
    pose(p,false,at.x,at.z,-90,30);floatPickupOffered=true;pickupOfferedAt=server.getTickCount();phase("active");
  }
  private static void floatTick(MinecraftServer server,Case c,long elapsed){
    if(floatToy.rider()==floatActor){floatRode=true;check(floatToy.claimedBy(floatActor)&&floatToy.getPassengers().size()==1,"LAN real passenger has sole claim");}
    if(floatToy.phase()==FloatToyEntity080.EXIT)floatExited=true;
    if(!floatPickupOffered&&(c.kind().equals("float_occupied")?floatRode&&floatToy.phase()==FloatToyEntity080.RIDE&&elapsed>=40:floatRode&&!floatActor.isPassenger()&&FloatToyRoutes080.safeFeet(server.overworld(),floatActor,floatActor.position())))offerPickup(server,c);
    var row=new LinkedHashMap<String,Object>();row.put("elapsed",elapsed);row.put("float_actor",sample(floatActor));row.put("float_toy",sample(floatToy));row.put("float_phase",floatToy.phase());row.put("transition",floatToy.transition());row.put("claim",floatToy.claimant().map(UUID::toString).orElse(""));row.put("removed",floatToy.isRemoved());row.put("pickup_offered",floatPickupOffered);serverTrace.add(row);
    if(floatPickupOffered&&floatToy.isRemoved()&&!floatActor.isPassenger()){
      check(floatRode&&floatExited&&FloatToyRoutes080.safeFeet(server.overworld(),floatActor,floatActor.position()),"real LAN float leaves actor only after safe exit");
      // The vehicle may finish its exit later in the tick than the Chao's
      // preview-manager update. Wait for the next ordinary cleanup tick.
      if(ToyPreview.active(floatActor)||floatActor.toyPlayingTicks()>0)return;
      int inventory=0;var p=player(server,"observer");for(int n=0;n<p.getInventory().getContainerSize();n++){var stack=p.getInventory().getItem(n);if(stack.is(ChaoGarden.item(floatToy.kind())))inventory+=stack.getCount();}
      int drops=server.overworld().getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,new AABB(-10,75,-10,10,85,10),e->e.getItem().is(ChaoGarden.item(floatToy.kind()))).stream().mapToInt(e->e.getItem().getCount()).sum();
      check(inventory+drops==1,"actual remote pickup/break preserves exactly one item, inventory="+inventory+" world="+drops);
      if(c.kind().equals("float_empty"))check(inventory==1&&drops==0,"empty actual pickup returns the original item directly");else check(inventory==0&&drops==1,"occupied request defers its one drop until safe landing");
      floatFinished=true;phase("float_complete");
    }
  }
  private static void prepare(MinecraftServer server,int next){
    index=next;var c=CASES.get(next);var host=player(server,"host");var remote=player(server,"observer");check(host!=null&&remote!=null,"both real profiles connected for case "+next);
    if(target!=null){target.discard();target=null;}
    // Power fixtures have one final, separated native pose each below.
    // Avoid publishing an intermediate remote-at-origin teleport that can
    // overlap the host while independent clients consume the packet sequence.
    if(!c.power()){pose(host,true,-4,6,-90,10);pose(remote,false,0,0,-90,c.kind().equals("input_block")?90:c.kind().equals("input_entity")?20:0);}
    if(c.floating())prepareFloat(server,c);
    else if(c.power()){
      CompanionVisualSmokeSupport.clear(remote);CompanionVisualSmokeSupport.equip(host,fixture(ChaoState.POWER,c.before()));
      pose(host,false,0,0,-90,0);pose(remote,false,c.kind().equals("power_enchanted")?2.8:1.8,0,90,0);victimUuid=remote.getUUID();
      if(c.kind().equals("power_enchanted")){var sword=new ItemStack(Items.NETHERITE_SWORD);sword.enchant(host.registryAccess().registryOrThrow(Registries.ENCHANTMENT).getHolderOrThrow(Enchantments.KNOCKBACK),2);host.setItemInHand(InteractionHand.MAIN_HAND,sword);}
    }else{
      CompanionVisualSmokeSupport.clear(host);
      if(next==0||c.movement()){
        var stack=CompanionVisualSmokeSupport.equip(remote,fixture(ChaoState.RUN,c.before()));runChao=ChaoCarrierItem.storedUUID(stack);
      }else check(percent(remote,ChaoState.RUN)==c.before(),"previous actual carrier selected setting persisted between cases");
      if(c.kind().equals("input_shield"))remote.setItemInHand(InteractionHand.OFF_HAND,new ItemStack(Items.SHIELD));
      if(c.kind().equals("input_entity")){target=EntityType.COW.create(server.overworld());check(target!=null,"create native use target");target.setNoAi(true);target.moveTo(2,80,0,0,0);server.overworld().addFreshEntity(target);}
      victimUuid=remote.getUUID();
    }
    serverPeak=serverMaxDx=serverWorstBackward=0;lastVictimX=startVictimX=remote.getX();serverMinAfterPeak=Double.POSITIVE_INFINITY;serverAttacks=knockbackCalls=0;hitAt=-1;capturedNativeImpulse=null;capturedNativeImpulseProof=null;serverTrace.clear();shieldSeen=false;
    prepared=server.overworld().getGameTime();began=-1;baselineSent=false;baselineSentAt=-1;baselineObservations.clear();phase("prepared");LogUtils.getLogger().info("REMOTE080_CASE {} {}",index,c.name());
  }
  private static void finishCase(MinecraftServer server){
    var c=CASES.get(index);var subject=player(server,c.controller());var remote=player(server,"observer");check(subject!=null&&remote!=null,"real subject disconnected at final assertion");
    if(c.floating()){
      check(floatFinished,"natural/preview float phase and actual network pickup completed");for(String role:List.of("host","observer")){var report=read(role+"-case-"+index+"-done.json");check(report!=null&&report.get("float_passenger_seen").getAsBoolean(),"both real clients saw the same occupied float");}
      write("server-case-"+index+"-pass.json",Map.of("case",c,"status","PASS","actor_uuid",floatActorId.toString(),"float_uuid",floatToyId.toString(),"real_passenger",floatRode,"safe_exit_seen",floatExited,"actual_client_pickup",true,"trace",List.copyOf(serverTrace)));
      floatActor.remove(Entity.RemovalReason.UNLOADED_TO_CHUNK);
      if(index+1<CASES.size()){prepare(server,index+1);return;}
      write("server-complete.json",Map.of("status","PASS","cases",CASES.size(),"scope","Real LAN clients: native ability input/attacks and tracked float/passenger/pickup; dedicated server remains separate.","source_freeze",System.getenv().getOrDefault("CHAO_TOYS080_SOURCE_FREEZE","unrecorded")));phase("complete");return;
    }
    check(percent(subject,c.stat())==c.expected(),"actual carrier selected setting expected "+c.expected()+" in "+c.name());
    var body=CompanionVisualSmokeSupport.body(subject);check(body!=null&&EquippedAbilityRules080.percent(body.state(),c.stat())==c.expected(),"live managed Chao matches saved carrier setting");
    if(c.kind().equals("input_shield"))check(shieldSeen,"ordinary off-hand shield use actually began");
    var client=read("observer-case-"+index+"-done.json");check(client!=null,"remote client receipt exists");
    if(c.rejoin()){check(exists("observer-disconnected.json")&&exists("observer-rejoined.json"),"actual remote leave/rejoin completed");check(runChao.equals(ChaoCarrierItem.storedUUID(carrier(remote))),"rejoin restores the same Chao identity");}
    if(c.movement()){
      double distance=client.get("distance").getAsDouble();check(distance>.3,"real forward input produced motion");
      double authoritativeDistance=remote.getX()-startVictimX;check(authoritativeDistance>.3,"authoritative server accepted native forward travel");
      if(c.before()==1000){runMaxDistance=distance;serverRunMaxDistance=authoritativeDistance;}else{
        check(runMaxDistance/distance>8&&runMaxDistance/distance<14,"native client maximum-versus-zero travel ratio near11, actual="+runMaxDistance/distance);
        check(serverRunMaxDistance/authoritativeDistance>8&&serverRunMaxDistance/authoritativeDistance<14,"authoritative server maximum-versus-zero travel ratio near11, actual="+serverRunMaxDistance/authoritativeDistance);
      }
    }
    JsonObject serverDecay=null,victimDecay=null;
    if(c.power()){
      check(serverAttacks==1,"exactly one real ServerPlayer attack event, actual="+serverAttacks);check(knockbackCalls>=1,"native knockback event observed");
      double peak=client.get("peak_own_abs_x_velocity").getAsDouble(),maxDx=client.get("max_own_abs_dx").getAsDouble();
      var tracker=read("host-case-"+index+"-done.json");check(tracker!=null,"attacker/tracker client receipt exists");double trackedPeak=tracker.get("peak_other_abs_x_velocity").getAsDouble();
      double limit=c.kind().equals("power_enchanted")?10:c.kind().equals("power_normal")?3.9:0;
      if(c.kind().equals("power_zero")){
        // Vanilla PvP sends motion only to the victim and restores the server's
        // old delta. Zero deliberately adds no custom full-vector correction.
        check(peak>.1&&maxDx>.1&&serverPeak<1&&peak<1&&maxDx<1&&trackedPeak<1,"zero bonus retains ordinary victim motion without inventing a nonvanilla server/tracker velocity");
      }else{
        check(serverMaxDx>limit&&peak>limit&&maxDx>limit&&trackedPeak>limit,"accepted authority movement AND actual victim/tracker clients exceed required packet/validation boundary "+limit+"; sampledServerVelocity="+serverPeak+" serverMovement="+serverMaxDx+" client="+peak+" movement="+maxDx+" tracker="+trackedPeak);
        double exact=c.kind().equals("power_enchanted")?18.7:4.4;
        check(capturedNativeImpulse!=null&&Math.abs(capturedNativeImpulse.x-exact)<.15,"read-only immutable native impulse snapshot preserves the exact actual server force; expected="+exact+" captured="+capturedNativeImpulse);
        // ServerTick.Post and queued tasks may sample only after friction.
        // Exact native force is tested at the handler in native GameTests; here
        // require the exact real victim/tracker impulse and victim step plus
        // accepted authority travel, while bounding all sampled server velocity.
        check(Math.abs(peak-exact)<.15&&Math.abs(maxDx-exact)<.15&&Math.abs(trackedPeak-exact)<.15&&serverMaxDx>=exact-.15&&Double.isFinite(serverPeak)&&serverPeak>=0&&serverPeak<=exact*1.10,"actual victim/tracker impulse and victim step are exact, authority accepts the movement, and sampled velocity never exceeds it; expected="+exact+" sampledServer="+serverPeak+" serverMovement="+serverMaxDx+" victim="+peak+" victimMovement="+maxDx+" tracker="+trackedPeak);
      }
      check(serverMinAfterPeak>=startVictimX-.5,"no backward teleport behind the pre-hit origin");
      check(serverWorstBackward>=-.5,"no midflight authoritative backwards rollback; worst signed dx="+serverWorstBackward);
      double victimWorst=client.get("worst_own_signed_dx").getAsDouble();
      check(victimWorst>=-.5,"actual victim client must not rewind a rejected impulse even if the server never advanced; worst signed dx="+victimWorst);
      if(!c.kind().equals("power_zero"))check(tracker.has("ending_tracked_victim_present")&&tracker.get("ending_tracked_victim_present").getAsBoolean()&&tracker.get("ending_tracked_victim_horizontal_speed").getAsDouble()<.05,
          "actual host-tracked boosted victim must remain loaded and settle before the next fixture teleport/reset");
      check(remote.getX()-startVictimX>(c.kind().equals("power_enchanted")?35:c.kind().equals("power_normal")?8:.3),"authoritative server retains real displacement after the impulse instead of rolling back to its launch point");
      double exact=c.kind().equals("power_enchanted")?18.7:c.kind().equals("power_normal")?4.4:.4;
      serverDecay=decayAudit(G.toJsonTree(serverTrace).getAsJsonArray(),"victim","server_tick",exact,!c.kind().equals("power_zero"));victimDecay=decayAudit(client.getAsJsonArray("trace"),"local","phase_tick",exact,true);
    }
    var result=new LinkedHashMap<String,Object>();result.put("case",c);result.put("server_source","actual integrated LAN server; two real client connections");result.put("subject",sample(subject));result.put("victim",sample(remote));result.put("body_uuid",body.getUUID().toString());result.put("state_sha256",sha(body.state().encode()));result.put("carrier_setting",percent(subject,c.stat()));result.put("movement_attribute",subject.getAttributeValue(Attributes.MOVEMENT_SPEED));result.put("server_attack_events",serverAttacks);result.put("knockback_events",knockbackCalls);result.put("server_peak_abs_x_velocity",serverPeak);result.put("server_velocity_sample_scope","ServerTick.Post/queued sample may occur after friction; exact native force is captured separately");if(capturedNativeImpulseProof!=null)result.put("captured_native_impulse",capturedNativeImpulseProof);result.put("server_max_abs_dx",serverMaxDx);result.put("server_worst_signed_backward_dx",serverWorstBackward);result.put("server_total_x_displacement",remote.getX()-startVictimX);if(serverDecay!=null){result.put("server_decay_audit",serverDecay);result.put("victim_decay_audit",victimDecay);}result.put("server_trace",List.copyOf(serverTrace));result.put("status","PASS");write("server-case-"+index+"-pass.json",result);
    if(index+1<CASES.size())prepare(server,index+1);else{
      CompanionVisualSmokeSupport.clear(player(server,"host"));CompanionVisualSmokeSupport.clear(remote);
      check(player(server,"host").getAttribute(Attributes.MOVEMENT_SPEED).getModifier(CompanionContent.RUN_SPEED)==null&&remote.getAttribute(Attributes.MOVEMENT_SPEED).getModifier(CompanionContent.RUN_SPEED)==null,"both real player Run modifiers removed on unequip");
      write("server-complete.json",Map.of("status","PASS","cases",CASES.size(),"scope","Real LAN clients; physical key mappings, ordinary attacks, actual slots and trajectories. Dedicated server and listening are separate checks.","source_freeze",System.getenv().getOrDefault("CHAO_TOYS080_SOURCE_FREEZE","unrecorded")));phase("complete");
    }
  }
  @SubscribeEvent public static void server(ServerTickEvent.Post event){
    if(!enabled()||!ROLE.equals("host")||!published||fatal!=null||exists("server-complete.json"))return;
    try{
      var server=event.getServer();var level=server.overworld();
      if(index<0){if(server.getPlayerList().getPlayerCount()>=2)prepare(server,0);return;}
      var c=CASES.get(index);var remote=player(server,"observer");long time=level.getGameTime();
      check(time-prepared<(c.floating()?2400:1200),"case timed out "+c.name());
      if(began<0){
        if(c.power())check(time-prepared<200,"pre-attack native baseline readiness timed out; see baseline observations; no attack armed");
        if(time-prepared>=(c.floating()?40:24)&&exists("host-case-"+index+"-ready.json")&&exists("observer-case-"+index+"-ready.json")){
          if(c.power()&&!powerBaseline(server,c,time))return;
          began=time;if(c.floating()){if(c.kind().equals("float_occupied")){var keeper=player(server,"host");check(server.getCommands().getDispatcher().execute("chao testtoy lifesaver loop",keeper.createCommandSourceStack().withPermission(2))==1,"native actual float preview starts");}else floatActor.setNoAi(false);}phase("active");
        }return;
      }
      long elapsed=time-began;
      if(c.floating()){
        if(!floatFinished)floatTick(server,c,elapsed);
        if(floatFinished&&exists("host-case-"+index+"-done.json")&&exists("observer-case-"+index+"-done.json"))finishCase(server);return;
      }
      if(remote!=null){
        if(c.power())captureNativeImpulse(remote);
        Vec3 motion=remote.getDeltaMovement();serverPeak=Math.max(serverPeak,Math.abs(motion.x));double signedDx=remote.getX()-lastVictimX;serverMaxDx=Math.max(serverMaxDx,Math.abs(signedDx));if(hitAt>=0)serverWorstBackward=Math.min(serverWorstBackward,signedDx);lastVictimX=remote.getX();
        if(hitAt>=0)serverMinAfterPeak=Math.min(serverMinAfterPeak,remote.getX());if(remote.isUsingItem()&&remote.getOffhandItem().is(Items.SHIELD))shieldSeen=true;
        var row=new LinkedHashMap<String,Object>();row.put("server_tick",time);row.put("elapsed",elapsed);row.put("victim",sample(remote));
        var motionCheck=EquippedKnockback080.movementCheck080(remote);
        if(motionCheck!=null)row.put("native_movement_check",Map.of("tick",motionCheck.tick(),"burst",motionCheck.burst(),"native_expected_squared",motionCheck.nativeExpectedSquared(),"authorized_expected_squared",motionCheck.authorizedExpectedSquared(),"source","read-only observation of the actual native movement-validation calculation"));
        var subject=player(server,c.controller());if(subject!=null){row.put("subject",sample(subject));if(ChaoCarrierItem.filled(carrier(subject)))row.put("setting",percent(subject,c.stat()));row.put("using_item",subject.isUsingItem());row.put("crouching",subject.isShiftKeyDown());row.put("sprinting",subject.isSprinting());}serverTrace.add(row);
      }
      int duration=c.power()?110:c.rejoin()?140:100;
      if(elapsed>=duration&&exists("host-case-"+index+"-done.json")&&exists("observer-case-"+index+"-done.json"))finishCase(server);
    }catch(Throwable error){fatal=error.toString();write("server-failure.json",Map.of("case",index,"error",fatal,"trace",serverTrace));LogUtils.getLogger().error("REMOTE080_SERVER_FAILURE",error);}
  }
  @SubscribeEvent(priority=EventPriority.LOWEST) public static void attack(AttackEntityEvent event){
    if(!enabled()||!ROLE.equals("host")||index<0||index>=CASES.size()||!CASES.get(index).power()||!(event.getEntity() instanceof ServerPlayer p))return;
    if(p.getGameProfile().getName().equals(roleName("host"))&&event.getTarget().getUUID().equals(victimUuid)){
      serverAttacks++;hitAt=p.server.overworld().getGameTime();int expected=index;Entity target=event.getTarget();
      // tell() queues explicitly even on the server thread. This diagnostic
      // samples after the native interact/Player.attack stack returns; the
      // LivingKnockBackEvent itself occurs before force is applied.
      p.server.tell(new net.minecraft.server.TickTask(p.server.getTickCount(),()->{if(index!=expected)return;if(target instanceof ServerPlayer victim)captureNativeImpulse(victim);serverPeak=Math.max(serverPeak,Math.abs(target.getDeltaMovement().x));serverTrace.add(Map.of("source","queued task; may observe after server physics rather than exact attack return","server_tick",p.server.overworld().getGameTime(),"victim",sample(target)));}));
    }
  }
  @SubscribeEvent(priority=EventPriority.LOWEST) public static void knockback(LivingKnockBackEvent event){if(enabled()&&ROLE.equals("host")&&index>=0&&index<CASES.size()&&CASES.get(index).power()&&!event.getEntity().level().isClientSide&&event.getEntity().getUUID().equals(victimUuid))knockbackCalls++;}

  private static void release(Minecraft mc){mc.options.keyUse.setDown(false);mc.options.keyAttack.setDown(false);mc.options.keyShift.setDown(false);mc.options.keyUp.setDown(false);mc.options.keyDown.setDown(false);mc.options.keySprint.setDown(false);}
  private static void click(KeyMapping key){key.setDown(true);KeyMapping.click(key.getKey());}
  private static void snapshot(Minecraft mc,String label){Screenshot.grab(mc.gameDirectory,"remote080-"+ROLE+"-"+clientCase+"-"+label+".png",mc.getMainRenderTarget(),ignored->{});}
  private static int clientPercent(Minecraft mc,int stat){for(var item:AccessoriesBridge.equipped(mc.player))if(ChaoCarrierItem.filled(item))return EquippedAbilityRules080.percent(ChaoCarrierItem.state(item),stat);return -1;}
  private static void clientSample(Minecraft mc,String hook){
    if(mc.player==null||clientCase<0||activeTicks<0)return;var me=mc.player;clientPeak=Math.max(clientPeak,Math.abs(me.getDeltaMovement().x));
    for(var other:mc.level.players())if(!other.getUUID().equals(me.getUUID()))clientOtherPeak=Math.max(clientOtherPeak,Math.abs(other.getDeltaMovement().x));
    if(hook.equals("post")){double signed=me.getX()-clientLastX;clientMaxDx=Math.max(clientMaxDx,Math.abs(signed));clientWorstSignedDx=Math.min(clientWorstSignedDx,signed);clientLastX=me.getX();var row=new LinkedHashMap<String,Object>();row.put("client_tick",localTicks);row.put("phase_tick",activeTicks);row.put("signed_local_dx",signed);row.put("local",sample(me));row.put("others",mc.level.players().stream().filter(p->p!=me).map(ToyRemoteVisualSmoke080::sample).toList());row.put("hit",mc.hitResult==null?"null":mc.hitResult.getType().name());row.put("using_item",me.isUsingItem());row.put("crouching",me.isShiftKeyDown());row.put("sprinting",me.isSprinting());clientTrace.add(row);}
  }
  @SubscribeEvent public static void before(ClientTickEvent.Pre event){if(enabled()){var mc=Minecraft.getInstance();if(mc.level!=null)clientSample(mc,"pre");}}
  @SubscribeEvent public static void frame(RenderFrameEvent.Pre event){if(enabled()){var mc=Minecraft.getInstance();if(mc.level!=null)clientSample(mc,"frame");}}
  private static void leaveAndRejoin(Minecraft mc){
    disconnected=true;write("observer-disconnected.json",Map.of("case",clientCase,"player",mc.player.getUUID().toString(),"carrier_setting",clientPercent(mc,ChaoState.RUN)));
    release(mc);started=false;reconnectAt=System.currentTimeMillis()+3500;mc.level.disconnect();mc.disconnect(new TitleScreen());
  }
  private static FloatToyEntity080 findFloat(Minecraft mc,JsonObject phase){if(mc.level==null)return null;UUID id=UUID.fromString(phase.get("float_toy_uuid").getAsString());for(var entity:mc.level.entitiesForRendering())if(entity instanceof FloatToyEntity080 f&&f.getUUID().equals(id))return f;return null;}
  private static ChaoEntity findFloatActor(Minecraft mc,JsonObject phase){if(mc.level==null)return null;UUID id=UUID.fromString(phase.get("float_actor_uuid").getAsString());for(var entity:mc.level.entitiesForRendering())if(entity instanceof ChaoEntity c&&c.getUUID().equals(id))return c;return null;}
  private static void aim(Minecraft mc,Vec3 target){var delta=target.subtract(mc.player.getEyePosition());float yaw=(float)(Math.toDegrees(Math.atan2(delta.z,delta.x))-90),pitch=(float)-Math.toDegrees(Math.atan2(delta.y,Math.hypot(delta.x,delta.z)));mc.player.setYRot(yaw);mc.player.setXRot(pitch);mc.player.yRotO=yaw;mc.player.xRotO=pitch;}
  @SubscribeEvent public static void floatRendered(RenderLivingEvent.Post<?,?> event){
    if(!enabled()||clientCase<0||clientCase>=CASES.size()||!CASES.get(clientCase).floating()||!(event.getEntity() instanceof ChaoEntity c))return;
    var phase=read("phase.json");if(phase==null||!phase.has("float_actor_uuid")||!c.getUUID().toString().equals(phase.get("float_actor_uuid").getAsString()))return;
    check(event.getRenderer().getModel() instanceof ChaoModel,"real LAN production Chao renderer");var model=(ChaoModel)event.getRenderer().getModel();check(model.officialPosesFinite(),"real LAN finite float rider poses");
    var mc=Minecraft.getInstance();var f=findFloat(mc,phase);if(f==null)return;var data=new JsonObject();data.addProperty("actor_uuid",c.getUUID().toString());data.addProperty("float_uuid",f.getUUID().toString());data.addProperty("actor_renderer",event.getRenderer().getClass().getName());data.addProperty("actual_living_render",true);data.addProperty("float_registered_renderer",mc.getEntityRenderDispatcher().getRenderer(f).getClass().getName());data.addProperty("gameplay_geometry",model.gameplayFormKey());data.addProperty("finite_poses",true);data.addProperty("float_phase",f.phase());data.addProperty("transition",f.transition());data.add("actor",G.toJsonTree(sample(c)));data.add("float",G.toJsonTree(sample(f)));data.addProperty("entity_packed_light",event.getPackedLight());data.addProperty("passenger",c.getVehicle()==f);clientFloatRenderProof=data;clientFloatRenderFrames++;
  }
  private static void runFloatClient(Minecraft mc,JsonObject phase,Case c,String stage){
    if(exists(ROLE+"-case-"+clientCase+"-done.json"))return;activeTicks++;var f=findFloat(mc,phase);var actor=findFloatActor(mc,phase);
    if(f!=null&&actor!=null){
      clientFloatPhases.add(f.phase());if(actor.getVehicle()==f){clientFloatPassenger=true;check(f.getPassengers().size()==1&&f.claimant().filter(actor.getUUID()::equals).isPresent(),"actual remote passenger/claim/seat sync");if(f.phase()==FloatToyEntity080.RIDE&&f.transition()>=1)check(actor.position().distanceTo(f.boardPoint(actor))<.35,"actual remote rider stays connected to the native seat");}
      var row=new LinkedHashMap<String,Object>();row.put("tick",activeTicks);row.put("float_phase",f.phase());row.put("transition",f.transition());row.put("actor",sample(actor));row.put("float",sample(f));row.put("claim",f.claimant().map(UUID::toString).orElse(""));row.put("passenger",actor.getVehicle()==f);row.put("hit",mc.hitResult==null?"null":mc.hitResult.getType().name());clientTrace.add(row);
      boolean offered=phase.get("pickup_offered").getAsBoolean();
      if(ROLE.equals("observer")&&offered){
        if(clientPickupAt<0)clientPickupAt=activeTicks;mc.options.keyShift.setDown(true);
        // Aim at the actual near rim, below its kind-specific box top. A
        // center/top ray on the short ring correctly selects its rider instead.
        var toOperator=mc.player.position().subtract(f.position());var rim=new Vec3(toOperator.x,0,toOperator.z).normalize().scale(.40);var pickupAim=f.position().add(rim).add(0,Math.min(.10,f.getBbHeight()*.4),0);aim(mc,pickupAim);
        if(!attacked&&activeTicks-clientPickupAt>=20&&mc.hitResult instanceof EntityHitResult hit&&hit.getEntity().getUUID().equals(f.getUUID())){
          check(mc.player.isShiftKeyDown()&&mc.player.getMainHandItem().isEmpty()&&mc.player.getOffhandItem().isEmpty(),"native float pickup uses crouch and actual empty hands");click(mc.options.keyUse);attacked=true;clientPickupSentAt=activeTicks;write(ROLE+"-case-"+clientCase+"-native-float-pickup.json",Map.of("tick",activeTicks,"float_uuid",f.getUUID().toString(),"occupied",f.rider()!=null,"hit","actual native entity ray and use KeyMapping","float_box_height",f.getBbHeight(),"aim",List.of(pickupAim.x,pickupAim.y,pickupAim.z)));snapshot(mc,"native-pickup");
        }
        if(clientPickupSentAt>=0&&activeTicks-clientPickupSentAt>=3)mc.options.keyUse.setDown(false);
        check(activeTicks-clientPickupAt<100||attacked,"native float pickup fixture never resolved the actual float entity ray");
      }else aim(mc,actor.position().add(0,.4,0).lerp(f.position().add(0,.3,0),.4));
      if(activeTicks%30==0&&actor.getVehicle()==f)snapshot(mc,"float-phase-"+f.phase()+"-t"+activeTicks);
    }
    if(stage.equals("float_complete")){
      check(clientFloatPassenger&&clientFloatPhases.contains(FloatToyEntity080.BOARD)&&clientFloatPhases.contains(FloatToyEntity080.RIDE)&&clientFloatPhases.contains(FloatToyEntity080.EXIT),"both actual clients saw synchronized entry, connected ride and safe exit");check(clientFloatRenderFrames>=4&&clientFloatRenderProof!=null,"production LAN rider rendered repeatedly");if(ROLE.equals("observer"))check(attacked,"actual remote pickup input happened");
      if(f!=null)return;release(mc);write(ROLE+"-case-"+clientCase+"-done.json",Map.of("status","PASS","case",c,"role",ROLE,"float_uuid",phase.get("float_toy_uuid").getAsString(),"float_passenger_seen",clientFloatPassenger,"phases",List.copyOf(clientFloatPhases),"production_actor_frames",clientFloatRenderFrames,"last_render_proof",clientFloatRenderProof,"trace",List.copyOf(clientTrace)));
    }
  }
  private static void runClient(Minecraft mc,JsonObject phase){
    int next=phase.get("case").getAsInt();String stage=phase.get("stage").getAsString();
    if(stage.equals("complete")){
      if(!done){release(mc);done=true;write(ROLE+"-complete.json",Map.of("status","PASS","cases",CASES.size(),"genuine_remote_rejoined",ROLE.equals("observer")&&reconnected,"source_freeze",System.getenv().getOrDefault("CHAO_TOYS080_SOURCE_FREEZE","unrecorded")));}
      if(exists("host-complete.json")&&exists("observer-complete.json"))mc.stop();return;
    }
    var c=CASES.get(next);
    if(clientCase!=next){release(mc);clientCase=next;activeTicks=-1;clientPeak=clientOtherPeak=clientMaxDx=clientWorstSignedDx=0;clientStartX=clientEndX=clientLastX=mc.player.getX();clientTrace.clear();attacked=false;clientFloatPhases.clear();clientFloatPassenger=false;clientPickupAt=clientPickupSentAt=-1;clientFloatRenderFrames=0;clientFloatRenderProof=null;}
    if(stage.equals("prepared")){
      boolean controlling=ROLE.equals(c.controller());int actual=controlling&&!c.floating()?clientPercent(mc,c.stat()):c.before();
      if(actual!=c.before())return;
      if(controlling&&!c.floating()){float pitch=c.kind().equals("input_block")?90:c.kind().equals("input_entity")?20:0;mc.player.setYRot(-90);mc.player.setXRot(pitch);mc.player.yRotO=-90;mc.player.xRotO=pitch;}
      if(c.floating()&&findFloat(mc,phase)==null)return;
      if(c.power()&&mc.level.players().size()<2)return;
      write(ROLE+"-case-"+next+"-ready.json",Map.of("uuid",mc.player.getUUID().toString(),"local_tick",localTicks,"setting",actual,"position",sample(mc.player),"baseline_epoch",phase.get("baseline_epoch").getAsInt(),"tracked_players",mc.level.players().stream().filter(p->p!=mc.player).map(ToyRemoteVisualSmoke080::sample).toList()));return;
    }
    if(c.floating()){runFloatClient(mc,phase,c,stage);return;}
    if(!stage.equals("active")||exists(ROLE+"-case-"+next+"-done.json"))return;
    activeTicks++;boolean controlling=ROLE.equals(c.controller());
    if(c.rejoin()){
      if(ROLE.equals("observer")&&!disconnected&&activeTicks>=20){leaveAndRejoin(mc);return;}
      if(ROLE.equals("observer")&&disconnected&&!reconnected&&activeTicks>20&&clientPercent(mc,ChaoState.RUN)==900){reconnected=true;write("observer-rejoined.json",Map.of("case",next,"uuid",mc.player.getUUID().toString(),"setting",900));}
    }else if(c.input()&&controlling){
      mc.options.keyShift.setDown(!c.kind().equals("input_standing"));
      if(activeTicks==12){
        var expected=c.kind().equals("input_block")?HitResult.Type.BLOCK:c.kind().equals("input_entity")?HitResult.Type.ENTITY:HitResult.Type.MISS;
        check(mc.hitResult!=null&&mc.hitResult.getType()==expected,"actual native input ray context "+c.name()+", actual="+(mc.hitResult==null?"null":mc.hitResult.getType()));click(mc.options.keyUse);
        write(ROLE+"-case-"+next+"-native-use.json",Map.of("local_tick",localTicks,"hit",mc.hitResult.getType().name(),"key","native Minecraft use KeyMapping click+down; release at52","crouching",mc.player.isShiftKeyDown()));
      }
      if(activeTicks==52)mc.options.keyUse.setDown(false);
    }else if(c.movement()&&controlling){
      if(activeTicks==10){clientStartX=mc.player.getX();mc.options.keyUp.setDown(true);}
      if(activeTicks==42){clientEndX=mc.player.getX();mc.options.keyUp.setDown(false);}
    }else if(c.power()&&controlling){
      if(c.kind().equals("power_enchanted")&&activeTicks>=10&&activeTicks<17){mc.options.keySprint.setDown(true);mc.options.keyUp.setDown(true);}
      int at=c.kind().equals("power_enchanted")?16:16;
      if(activeTicks==at){check(mc.hitResult instanceof EntityHitResult hit&&hit.getEntity().getUUID().toString().equals(phase.get("victim_uuid").getAsString()),"native attack ray resolves the real remote player");check(!c.kind().equals("power_enchanted")||mc.player.isSprinting(),"native client sprint actually active before enchanted attack");click(mc.options.keyAttack);attacked=true;write(ROLE+"-case-"+next+"-native-attack.json",Map.of("local_tick",localTicks,"sprinting",mc.player.isSprinting(),"target_uuid",phase.get("victim_uuid").getAsString(),"hit_type",mc.hitResult.getType().name(),"source","actual attack KeyMapping through normal game mode and packet"));}
      if(activeTicks==17){mc.options.keyAttack.setDown(false);mc.options.keySprint.setDown(false);mc.options.keyUp.setDown(false);}
    }
    clientSample(mc,"post");
    if(activeTicks==8||activeTicks==19||activeTicks==45||activeTicks==75)snapshot(mc,"t"+activeTicks);
    int end=c.rejoin()?110:c.power()?92:82;
    if(activeTicks>=end&&(!c.rejoin()||ROLE.equals("host")||reconnected)){
      if(controlling)check(clientPercent(mc,c.stat())==c.expected(),"actual synced client carrier setting matches expected after interaction");
      release(mc);var result=new LinkedHashMap<String,Object>();result.put("status","PASS");result.put("role",ROLE);result.put("case",c);result.put("local_profile_uuid",mc.player.getUUID().toString());result.put("selected_setting",controlling?clientPercent(mc,c.stat()):-1);result.put("peak_own_abs_x_velocity",clientPeak);result.put("peak_other_abs_x_velocity",clientOtherPeak);result.put("max_own_abs_dx",clientMaxDx);result.put("worst_own_signed_dx",clientWorstSignedDx);result.put("distance",Math.abs(clientEndX-clientStartX));result.put("native_attack_clicked",attacked);
      if(c.power()&&ROLE.equals("host")){
        UUID expected=UUID.fromString(phase.get("victim_uuid").getAsString());var trackedVictim=mc.level.players().stream().filter(p->p.getUUID().equals(expected)).findFirst().orElse(null);result.put("ending_tracked_victim_present",trackedVictim!=null);
        if(trackedVictim!=null){result.put("ending_tracked_victim",sample(trackedVictim));result.put("ending_tracked_victim_horizontal_speed",trackedVictim.getDeltaMovement().horizontalDistance());}
      }
      result.put("trace",List.copyOf(clientTrace));write(ROLE+"-case-"+next+"-done.json",result);
    }
  }
  @SubscribeEvent public static void client(ClientTickEvent.Post event){
    if(!enabled())return;
    var mc=Minecraft.getInstance();localTicks++;
    try{
      check(localTicks<36000,"client wall-tick timeout");if(fatal!=null)throw new IllegalStateException(fatal);
      var failure=read("server-failure.json");if(failure!=null)throw new IllegalStateException(failure.toString());
      if(mc.screen instanceof AccessibilityOnboardingScreen)mc.setScreen(new TitleScreen());
      if(!started&&mc.screen instanceof TitleScreen){
        mc.options.renderDistance().set(12);mc.options.simulationDistance().set(6);mc.options.framerateLimit().set(45);mc.options.fov().set(65);mc.options.pauseOnLostFocus=false;mc.options.hideGui=true;mc.options.gamma().set(.5);
        if(ROLE.equals("host")){
          check(!exists("host-ready.json")&&!exists("case-catalog.json"),"fresh shared output directory required");started=true;
          catalog=new LinkedHashMap<>();catalog.put("scope","PRIVATE prepared real LAN study, no execution result inferred until completion receipts");catalog.put("cases",CASES);catalog.put("source_freeze",System.getenv().getOrDefault("CHAO_TOYS080_SOURCE_FREEZE","unrecorded"));catalog.put("input_source","native KeyMapping clicks and held state, never direct cycle/impulse handler");write("case-catalog.json",catalog);
          mc.createWorldOpenFlows().createFreshLevel("Toy080Remote-"+System.currentTimeMillis(),new LevelSettings("Toy080Remote",GameType.CREATIVE,false,Difficulty.PEACEFUL,true,new GameRules(),WorldDataConfiguration.DEFAULT),new net.minecraft.world.level.levelgen.WorldOptions(808080L,false,false),access->access.registryOrThrow(Registries.WORLD_PRESET).getHolderOrThrow(net.minecraft.world.level.levelgen.presets.WorldPresets.FLAT).value().createWorldDimensions(),mc.screen);
        }else if(exists("host-ready.json")&&System.currentTimeMillis()>=reconnectAt){started=true;ConnectScreen.startConnecting(mc.screen,mc,ServerAddress.parseString("127.0.0.1:"+PORT),new ServerData("Local development study","127.0.0.1:"+PORT,ServerData.Type.LAN),false,null);}
      }
      if(ROLE.equals("host")&&!setup&&mc.player!=null&&mc.getSingleplayerServer()!=null){
        setup=true;mc.setScreen(null);ChaoRenderer.suppressNames=true;var server=mc.getSingleplayerServer();server.execute(()->{try{
          var level=server.overworld();level.setDayTime(6000);level.setWeatherParameters(0,100000,false,false);level.getGameRules().getRule(GameRules.RULE_DAYLIGHT).set(false,server);level.getGameRules().getRule(GameRules.RULE_DOMOBSPAWNING).set(false,server);
          // Broad uninterrupted runway; no artificial motion or no-gravity victim.
          for(var at:BlockPos.betweenClosed(-12,79,-7,340,83,7))level.setBlock(at,at.getY()==79?Blocks.SMOOTH_STONE.defaultBlockState():Blocks.AIR.defaultBlockState(),3);
          check(server.publishServer(GameType.SURVIVAL,false,PORT),"publish actual LAN server");published=true;write("host-ready.json",Map.of("port",PORT,"profile",mc.player.getGameProfile().getName(),"scope","Actual LAN; not a dedicated server"));
        }catch(Throwable error){fatal=error.toString();write("server-failure.json",Map.of("error",fatal));}});
      }
      if(mc.level!=null&&mc.player!=null){if(mc.screen instanceof ReceivingLevelScreen) return;var phase=read("phase.json");if(phase!=null)runClient(mc,phase);}
    }catch(Throwable error){release(mc);write(ROLE+"-failure.json",Map.of("case",clientCase,"error",error.toString(),"client_trace",clientTrace));LogUtils.getLogger().error("REMOTE080_CLIENT_FAILURE",error);mc.stop();}
  }
}
