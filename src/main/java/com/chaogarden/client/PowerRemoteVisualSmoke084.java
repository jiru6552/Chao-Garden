package com.chaogarden.client;

import com.chaogarden.ChaoGarden;
import com.chaogarden.companion.*;
import com.chaogarden.core.*;
import com.chaogarden.entity.ChaoEntity;
import com.chaogarden.item.ChaoCarrierItem;
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
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerEntity;
import net.minecraft.world.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.Cow;
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
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/** Excluded opt-in evidence helper. Two actual client processes: integrated
 * host/local connection and one independent TCP observer, not a dedicated
 * server or two TCP connections. No impulse/cycle receiver is invoked here. */
@EventBusSubscriber(modid=ChaoGarden.MODID,value=Dist.CLIENT)
public final class PowerRemoteVisualSmoke084 {
  private record Case(String name,int stat,int tier,String mode) {
    boolean run(){return stat==ChaoState.RUN;}
    boolean remote(){return mode.equals("remote_direct");}
    boolean ai(){return mode.equals("cow_ai");}
    boolean flying(){return mode.equals("run_creative");}
  }
  private static final List<Case> CASES=List.of(
      new Case("companion_ai_cow_max",ChaoState.POWER,1000,"cow_ai"),
      new Case("companion_ai_cow_zero",ChaoState.POWER,0,"cow_ai"),
      new Case("eligible_owner_player_ordinary",ChaoState.POWER,1000,"owner_control"),
      new Case("controlled_companion_remote_max",ChaoState.POWER,1000,"remote_direct"),
      new Case("controlled_companion_remote_zero",ChaoState.POWER,0,"remote_direct"),
      new Case("remote_run_ground_max",ChaoState.RUN,1000,"run_ground"),
      new Case("remote_run_creative_vertical_max",ChaoState.RUN,1000,"run_creative"));
  private static final List<Integer> SELECTED=selected();
  private static final Gson G=new GsonBuilder().setPrettyPrinting().create();
  private static final String ROLE=System.getenv().getOrDefault("CHAO_POWER084_ROLE","");
  private static final Path DIR=Path.of(System.getenv().getOrDefault("CHAO_POWER084_SHARED","/workspace/.setup/fixes084/unconfigured-power/shared"));
  private static final int PORT=Integer.parseInt(System.getenv().getOrDefault("CHAO_POWER084_PORT","25594"));
  private static boolean started,setup,published,finished,serverPrepared,armed,settled,clientComplete,pending,inputSent,revokeInputSent;
  private static volatile String fatal;
  private static int index=-1,selectionOffset,clientCase=-1,localTicks,renderFrames,inputReleaseAt=-1,revokeReleaseAt=-1,preparedGatesAt=-1;
  private static long joinedAt=-1,phaseAt,hitTick=-1,settledTick,seenBaseline;
  private static String stage="boot",world;
  private static UUID actorId,victimId,clientActorId,clientVictimId;
  private static ChaoEntity actor;
  private static LivingEntity victim;
  private static Vec3 firstVector;
  private static Vec3 runOrigin;
  private static net.minecraft.world.entity.decoration.ArmorStand camera;
  private static Map<String,Object> hitProof=Map.of(),renderProof=Map.of(),incoming=Map.of();
  private static final List<Map<String,Object>> serverRows=new ArrayList<>(),clientRows=new ArrayList<>(),results=new ArrayList<>();
  private static final List<Map<String,Object>> preparedGateRows=new ArrayList<>();
  private static double clientPeak,clientActiveHorizontalPeak;
  private static Vec3 clientActiveOrigin;
  private static double clientActiveDistance;
  private PowerRemoteVisualSmoke084(){}
  private static boolean enabled(){return ROLE.equals("host")||ROLE.equals("observer");}
  private static List<Integer> selected(){var out=new ArrayList<Integer>();for(String token:System.getenv().getOrDefault("CHAO_POWER084_CASES","0,1,2,3,4,5,6").split(",")){int i=Integer.parseInt(token.trim());if(i<0||i>=CASES.size()||out.contains(i))throw new IllegalArgumentException("Invalid Power084 context "+token);out.add(i);}if(out.isEmpty())throw new IllegalArgumentException("Empty Power084 context selection");return List.copyOf(out);}
  private static String freeze(){return System.getenv().getOrDefault("CHAO_POWER084_SOURCE_FREEZE","unrecorded");}
  private static String profile(String role){return role.equals("host")?"Power84Host":"Power84Observer";}
  private static void check(boolean value,String reason){if(!value)throw new IllegalStateException("POWER084 "+reason);}
  private static String sha(byte[] value){try{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value));}catch(Exception e){throw new IllegalStateException(e);}}
  private static synchronized void write(String name,Object value){try{Files.createDirectories(DIR);var tmp=DIR.resolve(name+"."+ROLE+".tmp");Files.writeString(tmp,G.toJson(value)+"\n");Files.move(tmp,DIR.resolve(name),StandardCopyOption.REPLACE_EXISTING,StandardCopyOption.ATOMIC_MOVE);}catch(Exception e){throw new IllegalStateException(e);}}
  private static JsonObject read(String name){try{return JsonParser.parseString(Files.readString(DIR.resolve(name))).getAsJsonObject();}catch(NoSuchFileException e){return null;}catch(Exception e){throw new IllegalStateException(e);}}
  private static boolean exists(String name){return Files.isRegularFile(DIR.resolve(name));}
  private static ServerPlayer player(MinecraftServer server,String role){return server.getPlayerList().getPlayers().stream().filter(p->p.getGameProfile().getName().equals(profile(role))).findFirst().orElse(null);}
  private static List<Double> vector(Vec3 v){return List.of(v.x,v.y,v.z);}
  private static Map<String,Object> sample(Entity entity){if(entity==null)return Map.of("tracked",false);var out=new LinkedHashMap<String,Object>();out.put("tracked",true);out.put("uuid",entity.getUUID().toString());out.put("id",entity.getId());out.put("position",vector(entity.position()));out.put("velocity",vector(entity.getDeltaMovement()));out.put("dimension",entity.level().dimension().location().toString());out.put("on_ground",entity.onGround());if(entity instanceof LivingEntity l)out.put("health",l.getHealth());if(entity instanceof net.minecraft.world.entity.player.Player p){var modifier=p.getAttribute(Attributes.MOVEMENT_SPEED).getModifier(CompanionContent.RUN_SPEED);out.put("run_modifier",modifier==null?0:modifier.amount());out.put("run_multiplier",EquippedRun084.multiplier(p));out.put("raw_flying_speed",p.getAbilities().getFlyingSpeed());out.put("flying",p.getAbilities().flying);}return out;}
  private static java.lang.reflect.Field field(Class<?> type,String name)throws ReflectiveOperationException{var f=type.getDeclaredField(name);f.setAccessible(true);return f;}
  /** Read-only production acceptance/impact metadata, never a packet injection. */
  private static Map<String,Object> seen(UUID id){try{var map=(Map<?,?>)field(EquippedKnockback080.class,"CLIENT_SEEN").get(null);var s=map.get(id);if(s==null)return Map.of("present",false,"sequence",0L);var t=s.getClass();return Map.of("present",true,"sequence",field(t,"sequence").getLong(s),"expires",field(t,"expires").getLong(s),"dimension",field(t,"dimension").get(s).toString(),"entity_id",field(t,"entityId").getInt(s));}catch(ReflectiveOperationException e){throw new IllegalStateException(e);}}
  private static Map<String,Object> impact(){try{var map=(Map<?,?>)field(EquippedKnockback080.class,"IMPACTS").get(null);var s=map.get(victimId);if(s==null)return Map.of("present",false);var t=s.getClass();return Map.of("present",true,"same_victim",field(t,"victim").get(s)==victim,"vector",vector((Vec3)field(t,"vector").get(s)),"armed",field(t,"armed").getBoolean(s),"expires",field(t,"expires").getLong(s),"order",field(t,"order").getLong(s));}catch(ReflectiveOperationException e){throw new IllegalStateException(e);}}
  private static Map<String,Object> tracker(){try{var source=((ServerLevel)victim.level()).getChunkSource();var chunkMap=field(source.getClass(),"chunkMap").get(source);var entries=(it.unimi.dsi.fastutil.ints.Int2ObjectMap<?>)field(chunkMap.getClass(),"entityMap").get(chunkMap);var tracked=entries.get(victim.getId());if(tracked==null)return Map.of("present",false);var serverEntity=(ServerEntity)field(tracked.getClass(),"serverEntity").get(tracked);return Map.of("present",true,"last_sent_movement",vector(serverEntity.getLastSentMovement()),"source","actual ChunkMap tracker, read only");}catch(ReflectiveOperationException e){return Map.of("present",false,"reflection_error",e.toString());}}
  private static Map<String,Object> nativeConnection(ServerPlayer p){var out=new LinkedHashMap<String,Object>();for(String name:List.of("receivedMovePacketCount","knownMovePacketCount","firstGoodX","firstGoodY","firstGoodZ","lastGoodX","lastGoodY","lastGoodZ","awaitingPositionFromClient","awaitingTeleport"))try{var value=field(p.connection.getClass(),name).get(p.connection);out.put(name,value==null?"NONE":value instanceof Vec3 v?vector(v):value);}catch(ReflectiveOperationException e){out.put(name,"UNAVAILABLE");}out.put("scope","Read-only actual packet/position/teleport fields; root compares traces and native log for accepted versus rollback, not inferred PASS.");return out;}
  private static ChaoState fixture(Case c){var s=ChaoState.egg(0,false,true,0,new Random(84084));s.adult=true;s.align=0;s.primaryType=c.stat()+1;s.grades[c.stat()]=5;s.gradeA[c.stat()]=s.gradeB[c.stat()]=5;check(s.useHyperFruit(c.stat()),"expressed X fixture");s.levels[c.stat()]=99;s.points[c.stat()]=9900;s.hunger=s.energy=100;s.setEvolutionStrength(1.2f);if(c.run()){s.levels[ChaoState.FLY]=s.levels[ChaoState.SWIM]=99;s.points[ChaoState.FLY]=s.points[ChaoState.SWIM]=9900;}if(c.tier()!=1000)s.setTrait(c.run()?EquippedAbilityRules080.RUN_SETTING:EquippedAbilityRules080.POWER_SETTING,c.tier());s=ChaoState.decode(s.encode());check(EquippedAbilityRules080.eligible(s,c.stat()),"exact locked first and current specialization");return s;}
  private static void pose(ServerPlayer p,boolean creative,double x,double y,double z){p.setGameMode(creative?GameType.CREATIVE:GameType.SURVIVAL);p.getInventory().clearContent();p.setSprinting(false);p.setShiftKeyDown(false);p.stopUsingItem();p.setDeltaMovement(Vec3.ZERO);p.fallDistance=0;p.getAttribute(Attributes.MAX_HEALTH).setBaseValue(1000);p.setHealth(1000);p.getFoodData().setFoodLevel(20);p.getAbilities().flying=creative;p.getAbilities().setFlyingSpeed(.05f);p.onUpdateAbilities();p.connection.teleport(x,y,z,0,0);p.serverLevel().getChunkSource().broadcastAndSend(p,new ClientboundSetEntityMotionPacket(p));}
  private static void phase(String next){stage=next;phaseAt=System.nanoTime();var data=new LinkedHashMap<String,Object>();data.put("case",index);data.put("stage",next);data.put("source_freeze",freeze());if(index>=0&&index<CASES.size()){data.put("context",CASES.get(index));data.put("actor_uuid",actorId==null?"":actorId.toString());data.put("victim_uuid",victimId==null?"":victimId.toString());data.put("actor",sample(actor));data.put("victim",sample(victim));}write("phase.json",data);}
  private static boolean both(String suffix){return exists("host-case-"+index+"-"+suffix+".json")&&exists("observer-case-"+index+"-"+suffix+".json");}
  private static void prepare(MinecraftServer server){
    var host=player(server,"host");var remote=player(server,"observer");if(victim instanceof Cow cow)cow.discard();CompanionVisualSmokeSupport.clear(host);CompanionVisualSmokeSupport.clear(remote);
    actor=null;actorId=null;victim=null;victimId=null;serverPrepared=armed=settled=false;hitTick=-1;firstVector=null;hitProof=Map.of();incoming=Map.of();serverRows.clear();
    var c=CASES.get(index);pose(host,false,0,80,2.5);pose(remote,c.flying(),4,c.flying()?128:80,c.run()?.5:4.5);
    var carrier=CompanionVisualSmokeSupport.equip(c.run()?remote:host,fixture(c));actorId=ChaoCarrierItem.storedUUID(carrier);
    if(c.run()){victim=remote;victimId=remote.getUUID();}else if(c.remote()){pose(remote,false,2,80,.5);victim=remote;victimId=remote.getUUID();}else{var cow=EntityType.COW.create(server.overworld());cow.setNoAi(true);cow.moveTo(2,80,.5);cow.getAttribute(Attributes.MAX_HEALTH).setBaseValue(1000);cow.setHealth(1000);check(server.overworld().addFreshEntity(cow),"publish real cow");victim=cow;victimId=cow.getUUID();}
    phase("preparing");
  }
  private static void ready(MinecraftServer server){
    var c=CASES.get(index);var host=player(server,"host");var owner=player(server,c.run()?"observer":"host");actor=CompanionVisualSmokeSupport.body(owner);if(actor==null||!actorId.equals(actor.getUUID()))return;
    if(c.run()){var m=owner.getAttribute(Attributes.MOVEMENT_SPEED).getModifier(CompanionContent.RUN_SPEED);if(m==null||m.amount()!=1000)return;}
    actor.setNoAi(!c.run());actor.setNoGravity(c.run());actor.setPos(c.run()?owner.getX()+1:.5,c.flying()?128:80,.5);actor.setDeltaMovement(Vec3.ZERO);
    check(EquippedAbilityRules080.eligible(actor.state(),c.stat()),"real current body remains eligible");
    if(c.ai()){
      // Accepted keeper attack establishes only the existing Mob target policy.
      // Health/motion/cooldown reset is explicit PREPARATION, before both ready snapshots.
      try{field(LivingEntity.class,"attackStrengthTicker").setInt(host,1000);}catch(ReflectiveOperationException e){throw new IllegalStateException(e);}
      host.attack(victim);check(host.getLastHurtMob()==victim,"real accepted keeper hit selects existing Mob-only companion target");
      victim.setHealth(1000);victim.invulnerableTime=0;victim.hurtTime=0;victim.setPos(2,80,.5);victim.setDeltaMovement(Vec3.ZERO);victim.setOnGround(true);
    }
    ((ServerLevel)victim.level()).getChunkSource().broadcastAndSend(victim,new ClientboundSetEntityMotionPacket(victim));serverPrepared=true;phase("prepared");
    write("server-case-"+index+"-prepared.json",Map.of("context",c,"actor",sample(actor),"victim",sample(victim),"power_bonus",CompanionContent.secretPowerBonus084(actor),"run_multiplier",EquippedRun084.multiplier(owner),"raw_flying_speed",owner.getAbilities().getFlyingSpeed(),"preparation_only",true,"source_freeze",freeze()));
  }
  @SubscribeEvent public static void incoming(LivingIncomingDamageEvent event){if(!enabled()||!ROLE.equals("host")||!armed||hitTick>=0||event.getEntity()!=victim)return;var source=event.getSource();incoming=Map.of("causing_uuid",source.getEntity()==null?"":source.getEntity().getUUID().toString(),"direct_uuid",source.getDirectEntity()==null?"":source.getDirectEntity().getUUID().toString(),"mob_attack",source.is(net.minecraft.world.damagesource.DamageTypes.MOB_ATTACK),"before_velocity",vector(victim.getDeltaMovement()),"health_before",victim.getHealth(),"source_scope","actual incoming production damage source, not inferred owner credit");}
  @SubscribeEvent public static void damaged(LivingDamageEvent.Post event){
    if(!enabled()||!ROLE.equals("host")||!armed||hitTick>=0||event.getEntity()!=victim||CASES.get(index).run())return;
    var c=CASES.get(index);check(!incoming.isEmpty(),"accepted damage has captured actual source");
    boolean own=actorId.toString().equals(incoming.get("causing_uuid"))&&actorId.toString().equals(incoming.get("direct_uuid"));
    if(c.ai()&&!own)return;
    check(c.mode().equals("owner_control")?actor.owner.toString().equals(incoming.get("causing_uuid"))&&actor.owner.toString().equals(incoming.get("direct_uuid"))&&!Boolean.TRUE.equals(incoming.get("mob_attack")):own&&Boolean.TRUE.equals(incoming.get("mob_attack")),"correct actual direct attack actor/type");check(Math.abs(event.getNewDamage()-(own?11:1))<.001,"actual unchanged accepted damage amount");
    hitTick=((ServerLevel)victim.level()).getServer().getTickCount();var map=new LinkedHashMap<String,Object>();map.putAll(incoming);map.put("accepted_damage",event.getNewDamage());map.put("server_tick",hitTick);map.put("actual_companion_ai",c.ai());map.put("controlled_direct_dispatch",c.remote());hitProof=map;
  }
  private static void serverSample(MinecraftServer server){var row=new LinkedHashMap<String,Object>();row.put("server_tick",server.getTickCount());row.put("stage",stage);row.put("elapsed_since_hit",server.getTickCount()-hitTick);row.put("actor",sample(actor));row.put("victim",sample(victim));row.put("impact",impact());row.put("tracker",tracker());if(victim instanceof ServerPlayer p){row.put("power_native_movement_check",EquippedKnockback080.movementCheck080(p));row.put("run_native_movement_check",EquippedRun084.movementCheck(p));row.put("actual_native_connection",nativeConnection(p));}serverRows.add(row);write("server-case-"+index+"-trace.json",Map.of("rows",List.copyOf(serverRows),"hit",hitProof,"source_freeze",freeze()));}
  private static void activeGates(MinecraftServer server){
    if(server.getTickCount()%20!=0)return;var host=player(server,"host");var row=new LinkedHashMap<String,Object>();row.put("server_tick",server.getTickCount());row.put("stage",stage);row.put("case",index);row.put("actor",sample(actor));row.put("victim",sample(victim));row.put("source_freeze",freeze());
    if(actor!=null){row.put("no_ai",actor.isNoAi());row.put("no_gravity",actor.isNoGravity());row.put("actor_tick",actor.tickCount);row.put("on_ground",actor.onGround());row.put("energy",actor.state().energy);row.put("adult",actor.state().adult);row.put("dead",actor.state().dead);row.put("cocoon_ticks",actor.state().cocoonTicks);row.put("attached",actor.isCompanionAttached());row.put("carry_position",actor.companionCarryPosition());row.put("handling_item",actor.isHandlingItem());row.put("carry_preview",CarryPreview.active(actor));row.put("authorized_power_bonus",CompanionContent.secretPowerBonus084(actor));row.put("position_entity_ticking",((ServerLevel)actor.level()).isPositionEntityTicking(actor.blockPosition()));row.put("current_target",sample(actor.getTarget()));row.put("victim_distance_squared",actor.distanceToSqr(victim));
      try{var all=(Map<?,?>)field(CompanionContent.class,"ACTIVE").get(null);var active=all.get(actor.owner);row.put("active_exists",active!=null);if(active!=null){row.put("active_body_identity",field(active.getClass(),"body").get(active)==actor);row.put("attack_cooldown",field(active.getClass(),"attackCooldown").get(active));row.put("last_seen",field(active.getClass(),"lastSeen").get(active));}}catch(ReflectiveOperationException e){row.put("active_reflection_error",e.toString());}
      try{var goals=(net.minecraft.world.entity.ai.goal.GoalSelector)field(Mob.class,"goalSelector").get(actor);row.put("running_goals",goals.getAvailableGoals().stream().filter(g->g.isRunning()).map(g->g.getGoal().getClass().getName()).toList());}catch(ReflectiveOperationException e){row.put("goal_reflection_error",e.toString());}
    }
    if(host!=null){row.put("keeper",sample(host));row.put("keeper_tick",host.tickCount);row.put("keeper_last_hurt_mob",sample(host.getLastHurtMob()));row.put("keeper_target_age",host.tickCount-host.getLastHurtMobTimestamp());row.put("keeper_distance_squared",host.distanceToSqr(victim));}if(victim instanceof ServerPlayer p){row.put("actual_native_connection",nativeConnection(p));row.put("run_native_movement_check",EquippedRun084.movementCheck(p));row.put("power_native_movement_check",EquippedKnockback080.movementCheck080(p));row.put("run_authority",CompanionContent.secretRunBonus084(p));row.put("run_multiplier",EquippedRun084.multiplier(p));}write("server-case-"+index+"-active-gates.json",row);
  }
  private static void settle(MinecraftServer server){
    var c=CASES.get(index);serverSample(server);var pre=sample(victim);actor.setNoAi(true);actor.getNavigation().stop();
    if(c.run()){
      var p=(ServerPlayer)victim;var diagnostic=EquippedRun084.movementCheck(p);
      check(diagnostic!=null&&diagnostic.multiplier()==1001&&diagnostic.authorized()>=diagnostic.ordinary(),"actual remote movement handler records authenticated Run envelope");
      check((c.flying()?p.getY()-runOrigin.y:p.getX()-runOrigin.x)>1,"remote one-tick native input actually changes server position without baseline rollback");
    }
    CompanionVisualSmokeSupport.clear(player(server,c.run()?"observer":"host"));
    if(!c.run()){victim.setDeltaMovement(Vec3.ZERO);victim.hasImpulse=true;((ServerLevel)victim.level()).getChunkSource().broadcastAndSend(victim,new ClientboundSetEntityMotionPacket(victim));}
    settled=true;settledTick=server.getTickCount();write("server-case-"+index+"-intervention.json",Map.of("stage","AFTER_TWO_TICK_MEASUREMENT","before",pre,"source_freeze",freeze(),"intervention",c.run()?"unequip only; no velocity/position forcing during Run revoke observation":"unequip and one deliberate zero-velocity/native packet stop; not natural full-flight decay proof"));phase("settle");
  }
  @SubscribeEvent public static void server(ServerTickEvent.Post event){
    if(!enabled()||!ROLE.equals("host")||!published||finished||fatal!=null)return;var server=event.getServer();
    try{
      var host=player(server,"host");var remote=player(server,"observer");if(host==null||remote==null)return;
      if(joinedAt<0){joinedAt=server.getTickCount();return;}if(server.getTickCount()-joinedAt<80)return;
      check(System.nanoTime()-phaseAt<120_000_000_000L,"phase timeout "+stage);
      if(index<0){selectionOffset=0;index=SELECTED.get(0);prepare(server);return;}
      var c=CASES.get(index);if(!serverPrepared){ready(server);return;}
      if(stage.equals("prepared")&&both("ready")){phase("armed");return;}
      if(stage.equals("armed")&&both("armed")){
        armed=true;runOrigin=victim.position();phase("active");
        if(c.ai()){actor.setNoAi(false);check(host.getLastHurtMob()==victim,"accepted existing Mob target retained until AI starts");}
        else if(c.remote())check(victim.hurt(actor.damageSources().mobAttack(actor),CompanionRules.powerDamage(actor.state().levels[ChaoState.POWER])),"controlled real companion damage accepted by remote player");
        else if(!c.run()){try{field(LivingEntity.class,"attackStrengthTicker").setInt(host,1000);}catch(ReflectiveOperationException e){throw new IllegalStateException(e);}host.attack(victim);}
        return;
      }
      if(stage.equals("active")){
        activeGates(server);
        if(c.run()&&hitTick<0&&exists("observer-case-"+index+"-input.json")&&victim.position().distanceTo(runOrigin)>1&&EquippedRun084.movementCheck((ServerPlayer)victim)!=null){hitTick=server.getTickCount();hitProof=Map.of("native_input",read("observer-case-"+index+"-input.json"),"first_accepted_server_position",sample(victim),"measurement_origin",vector(runOrigin),"source","actual remote one-tick KeyMapping and changed server position with native validation diagnostic; not a server teleport");}
        if(hitTick<0)return;
        if(firstVector==null){firstVector=victim.getDeltaMovement();if(!c.run()){var nativeImpact=impact();if(c.tier()>0&&!c.mode().equals("owner_control")){check(Boolean.TRUE.equals(nativeImpact.get("present"))&&Boolean.TRUE.equals(nativeImpact.get("armed"))&&Boolean.TRUE.equals(nativeImpact.get("same_victim")),"actual current victim has armed immutable native impulse");var force=(List<?>)nativeImpact.get("vector");var before=(List<?>)incoming.get("before_velocity");double dx=((Number)force.get(0)).doubleValue()-((Number)before.get(0)).doubleValue()/2,dz=((Number)force.get(2)).doubleValue()-((Number)before.get(2)).doubleValue()/2;check(Math.abs(Math.hypot(dx,dz)-400.4)<.02,"actual native companion horizontal impulse is400.4 after subtracting prior half motion");}var data=new LinkedHashMap<String,Object>();data.putAll(hitProof);data.put("first_server_observed_velocity",vector(firstVector));data.put("impact",nativeImpact);data.put("caveat","Post tick vector may already include native physics; immutable Impact.vector is the exact boosted after-knockback snapshot.");hitProof=data;}}
        serverSample(server);if(server.getTickCount()-hitTick>=2)settle(server);return;
      }
      if(stage.equals("settle")){
        serverSample(server);if(server.getTickCount()-settledTick<20||!both("done"))return;
        var owner=player(server,c.run()?"observer":"host");check(c.run()?CompanionContent.secretRunBonus084(owner)==0:CompanionContent.secretPowerBonus084(actor)==0,"unequip revokes authority");
        var result=new LinkedHashMap<String,Object>();result.put("context",c);result.put("hit",hitProof);result.put("server_rows",List.copyOf(serverRows));result.put("revoke_authority",true);result.put("source_freeze",freeze());results.add(result);write("server-case-"+index+"-result.json",result);
        if(++selectionOffset<SELECTED.size()){index=SELECTED.get(selectionOffset);prepare(server);return;}
        index=CASES.size();
        server.getPlayerList().saveAll();server.saveEverything(true,true,true);finished=true;write("server-complete.json",Map.of("status","COMPLETE","cases",results,"selected_context_ids",SELECTED,"unselected_context_ids",java.util.stream.IntStream.range(0,CASES.size()).filter(i->!SELECTED.contains(i)).boxed().toList(),"unselected_status","NOT_RUN_IN_THIS_SELECTED_COHORT","source_freeze",freeze(),"topology","two actual client processes; integrated local host and one remote TCP peer; not a dedicated server","measurement_ticks",2));phase("complete");
      }
    }catch(Throwable e){fatal=e.toString();write("server-failure.json",Map.of("stage",stage,"case",index,"error",fatal));LogUtils.getLogger().error("POWER084_SERVER_FAILURE",e);}
  }
  private static Entity clientEntity(Minecraft mc,UUID id){if(mc.player!=null&&mc.player.getUUID().equals(id))return mc.player;for(var e:mc.level.entitiesForRendering())if(e.getUUID().equals(id))return e;return null;}
  private static void preparedView(Minecraft mc,Entity a){
    double tx=a.getX(),ty=a.getY()+.6,tz=a.getZ(),x=tx+4.5,y=ty+2.3,z=tz+6.5;
    float yaw=(float)Math.toDegrees(Math.atan2(x-tx,tz-z)),pitch=(float)-Math.toDegrees(Math.atan2(ty-y,Math.hypot(tx-x,tz-z)));
    if(camera==null||camera.level()!=mc.level)camera=new net.minecraft.world.entity.decoration.ArmorStand(mc.level,x,y,z);
    camera.moveTo(x,y-camera.getEyeHeight(),z,yaw,pitch);camera.xo=camera.getX();camera.yo=camera.getY();camera.zo=camera.getZ();camera.yRotO=yaw;camera.xRotO=pitch;camera.setYHeadRot(yaw);camera.yHeadRotO=yaw;mc.setCameraEntity(camera);
  }
  private static void release(Minecraft mc){mc.options.keyUp.setDown(false);mc.options.keyJump.setDown(false);mc.options.keyAttack.setDown(false);mc.options.keyUse.setDown(false);mc.options.keyShift.setDown(false);mc.options.keySprint.setDown(false);}
  private static void clientSample(Minecraft mc,String hook){
    if(mc.level==null||mc.player==null||clientCase<0||clientCase>=CASES.size()||clientVictimId==null)return;var p=read("phase.json");if(p==null||p.get("case").getAsInt()!=clientCase)return;
    var v=clientEntity(mc,clientVictimId);if(v!=null){clientPeak=Math.max(clientPeak,v.getDeltaMovement().length());if(p.get("stage").getAsString().equals("active")){clientActiveHorizontalPeak=Math.max(clientActiveHorizontalPeak,v.getDeltaMovement().horizontalDistance());if(clientActiveOrigin==null)clientActiveOrigin=v.position();clientActiveDistance=Math.max(clientActiveDistance,v.position().distanceTo(clientActiveOrigin));}}var row=new LinkedHashMap<String,Object>();row.put("hook",hook);row.put("client_tick",localTicks);row.put("observed_stage",p.get("stage").getAsString());row.put("victim",sample(v));row.put("local_player",sample(mc.player));row.put("full_impulse_accepted",seen(clientVictimId));row.put("source_freeze",freeze());clientRows.add(row);
    if(hook.equals("post"))write(ROLE+"-case-"+clientCase+"-trace.json",Map.of("rows",List.copyOf(clientRows),"peak_actual_speed",clientPeak,"baseline_accepted_sequence",seenBaseline,"scope","actual native client samples; untracked is not zero. CLIENT_SEEN proves accepted production packet identity/sequence, not a server vector substituted for client observation."));
  }
  @SubscribeEvent public static void pre(ClientTickEvent.Pre event){if(enabled()){var mc=Minecraft.getInstance();if(mc.level!=null)clientSample(mc,"pre");}}
  @SubscribeEvent public static void frame(RenderFrameEvent.Pre event){if(enabled()){var mc=Minecraft.getInstance();if(mc.level!=null)clientSample(mc,"render_pre");}}
  @SubscribeEvent public static void rendered(RenderLivingEvent.Post<?,?> event){if(!enabled()||clientActorId==null||!event.getEntity().getUUID().equals(clientActorId))return;renderFrames++;renderProof=Map.of("actual_production_render",true,"actor_uuid",clientActorId.toString(),"renderer",event.getRenderer().getClass().getName());}
  private static void screenshot(Minecraft mc,String label,Runnable complete){pending=true;String name="power084-"+ROLE+"-case"+clientCase+"-"+label+".png";int selected=clientCase;Screenshot.grab(mc.gameDirectory,name,mc.getMainRenderTarget(),ignored->{try{var file=mc.gameDirectory.toPath().resolve("screenshots").resolve(name);check(Files.isRegularFile(file),"actual screenshot callback completes");write(ROLE+"-case-"+selected+"-"+label+"-view.json",Map.of("file",file.toString(),"sha256",sha(Files.readAllBytes(file)),"source_freeze",freeze(),"native_render_frames",renderFrames,"render_proof",renderProof));pending=false;complete.run();}catch(Throwable e){fatal=e.toString();}});}
  /** Diagnostic only: observes every existing prepared gate without changing it. */
  private static void preparedGates(Minecraft mc,JsonObject phase,Case context,Entity actualActor,Entity actualVictim){
    if(preparedGatesAt>=0&&localTicks-preparedGatesAt<20)return;
    preparedGatesAt=localTicks;
    var row=new LinkedHashMap<String,Object>();
    row.put("role",ROLE);row.put("case",clientCase);row.put("local_tick",localTicks);row.put("observed_phase",phase.get("stage").getAsString());row.put("context",context);row.put("source_freeze",freeze());
    var expectedActor=phase.getAsJsonObject("actor");var expectedVictim=phase.getAsJsonObject("victim");
    row.put("expected_actor",expectedActor.deepCopy());row.put("expected_victim",expectedVictim.deepCopy());
    row.put("expected_actor_uuid",phase.get("actor_uuid").getAsString());row.put("expected_victim_uuid",phase.get("victim_uuid").getAsString());
    row.put("client_actor_uuid",clientActorId.toString());row.put("client_victim_uuid",clientVictimId.toString());row.put("expected_stat",context.stat());row.put("expected_tier",context.tier());row.put("expected_locked_primary_type",context.stat()+1);
    row.put("actual_actor",sample(actualActor));row.put("actual_victim",sample(actualVictim));row.put("local_player",sample(mc.player));
    if(actualActor!=null){row.put("actual_actor_class",actualActor.getClass().getName());row.put("actual_actor_type",net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getKey(actualActor.getType()).toString());row.put("actual_actor_removed",actualActor.isRemoved());row.put("actual_actor_invisible",actualActor.isInvisible());}
    if(actualVictim!=null){row.put("actual_victim_class",actualVictim.getClass().getName());row.put("actual_victim_type",net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getKey(actualVictim.getType()).toString());row.put("actual_victim_removed",actualVictim.isRemoved());}
    boolean chaoActor=actualActor instanceof ChaoEntity;
    boolean eligible=false,tierMatches=false;
    if(actualActor instanceof ChaoEntity actual){
      var state=actual.state();eligible=EquippedAbilityRules080.eligible(state,context.stat());int tier=EquippedAbilityRules080.percent(state,context.stat());tierMatches=tier==context.tier();
      row.put("actual_actor_state_sha256",sha(G.toJson(state).getBytes(StandardCharsets.UTF_8)));row.put("actual_actor_state_hash_encoding","Gson field snapshot; does not invoke the normalizing ChaoState.encode method");row.put("actual_actor_state_eligible",eligible);row.put("actual_actor_state_tier",tier);row.put("actual_actor_state_adult",state.adult);row.put("actual_actor_state_dead",state.dead);row.put("actual_actor_state_cocoon_ticks",state.cocoonTicks);row.put("actual_actor_state_grade",state.grades[context.stat()]);row.put("actual_actor_state_level",state.levels[context.stat()]);row.put("actual_actor_state_primary_type",state.primaryType);row.put("actual_actor_state_locked_primary_type",state.lockedPrimaryType());row.put("actual_actor_state_secondary_types",state.secondaryTypes());
    }
    var expectedPosition=expectedVictim.getAsJsonArray("position");var at=new Vec3(expectedPosition.get(0).getAsDouble(),expectedPosition.get(1).getAsDouble(),expectedPosition.get(2).getAsDouble());
    row.put("expected_victim_position",vector(at));
    double distance=actualVictim==null?Double.NaN:actualVictim.position().distanceTo(at),horizontalSpeed=actualVictim==null?Double.NaN:actualVictim.getDeltaMovement().horizontalDistance();
    if(actualVictim!=null){row.put("victim_position_distance",distance);row.put("victim_horizontal_speed",horizontalSpeed);row.put("actual_victim_velocity",vector(actualVictim.getDeltaMovement()));}
    row.put("position_tolerance",.25);row.put("horizontal_speed_tolerance",.05);row.put("render_frames",renderFrames);row.put("required_render_frames",context.run()?0:8);row.put("render_proof",new LinkedHashMap<>(renderProof));
    row.put("screen",mc.screen==null?"NONE":mc.screen.getClass().getName());row.put("overlay",mc.getOverlay()==null?"NONE":mc.getOverlay().getClass().getName());row.put("screenshot_pending",pending);row.put("ready_receipt_exists",exists(ROLE+"-case-"+clientCase+"-ready.json"));
    var viewed=mc.getCameraEntity();row.put("camera",sample(viewed));row.put("camera_is_local_player",viewed==mc.player);
    if(viewed!=null){row.put("camera_class",viewed.getClass().getName());row.put("camera_body_yaw",viewed.getYRot());row.put("camera_pitch",viewed.getXRot());row.put("camera_view_yaw",viewed.getViewYRot(1));row.put("camera_view_pitch",viewed.getViewXRot(1));if(viewed instanceof LivingEntity living){row.put("camera_head_yaw",living.getYHeadRot());row.put("camera_previous_head_yaw",living.yHeadRotO);}}
    double runMultiplier=EquippedRun084.multiplier(mc.player);row.put("actual_local_run_multiplier",runMultiplier);row.put("actual_local_flying",mc.player.getAbilities().flying);
    var gates=new LinkedHashMap<String,Boolean>();
    gates.put("actor_is_chao",chaoActor);gates.put("actor_eligible",eligible);gates.put("actor_tier_matches",tierMatches);gates.put("victim_present",actualVictim!=null);gates.put("render_frames_ready",context.run()||renderFrames>=8);gates.put("screen_clear",mc.screen==null);gates.put("overlay_clear",mc.getOverlay()==null);gates.put("screenshot_not_pending",!pending);
    gates.put("run_multiplier_ready",!context.run()||!ROLE.equals("observer")||runMultiplier==1001);gates.put("creative_flying_ready",!context.run()||!ROLE.equals("observer")||!context.flying()||mc.player.getAbilities().flying);gates.put("victim_position_ready",actualVictim!=null&&!(distance>.25));gates.put("victim_horizontal_motion_ready",actualVictim!=null&&!(horizontalSpeed>.05));
    row.put("existing_readiness_gates",gates);row.put("blocking_existing_gates",gates.entrySet().stream().filter(entry->!entry.getValue()).map(Map.Entry::getKey).toList());
    preparedGateRows.add(row);write(ROLE+"-case-"+clientCase+"-prepared-gates.json",Map.of("scope","Periodic read-only observations of unchanged prepared readiness gates; neither a readiness override nor a runtime verdict.","interval_local_ticks",20,"rows",List.copyOf(preparedGateRows),"source_freeze",freeze()));
  }
  private static void clientPhase(Minecraft mc,JsonObject p){
    String next=p.get("stage").getAsString();int selected=p.get("case").getAsInt();
    if(next.equals("complete")){if(!clientComplete&&!pending){release(mc);clientComplete=true;write(ROLE+"-complete.json",Map.of("status","COMPLETE","peer_uuid",mc.player.getUUID().toString(),"source_freeze",freeze()));}if(exists("host-complete.json")&&exists("observer-complete.json"))mc.stop();return;}
    if(next.equals("preparing"))return;
    if(clientCase!=selected){release(mc);clientCase=selected;clientActorId=UUID.fromString(p.get("actor_uuid").getAsString());clientVictimId=UUID.fromString(p.get("victim_uuid").getAsString());clientRows.clear();preparedGateRows.clear();preparedGatesAt=-1;clientPeak=clientActiveHorizontalPeak=clientActiveDistance=0;clientActiveOrigin=null;renderFrames=0;renderProof=Map.of();seenBaseline=((Number)seen(clientVictimId).get("sequence")).longValue();inputSent=revokeInputSent=false;inputReleaseAt=revokeReleaseAt=-1;}
    var c=CASES.get(clientCase);
    if(next.equals("prepared")){
      var a=clientEntity(mc,clientActorId);var v=clientEntity(mc,clientVictimId);if(a!=null)preparedView(mc,a);
      preparedGates(mc,p,c,a,v);
      if(!(a instanceof ChaoEntity actual)||!EquippedAbilityRules080.eligible(actual.state(),c.stat())||EquippedAbilityRules080.percent(actual.state(),c.stat())!=c.tier()
          ||v==null||(!c.run()&&renderFrames<8)||mc.screen!=null||mc.getOverlay()!=null||pending)return;
      if(c.run()&&ROLE.equals("observer")&&(EquippedRun084.multiplier(mc.player)!=1001||c.flying()&&!mc.player.getAbilities().flying))return;
      var expected=p.getAsJsonObject("victim").getAsJsonArray("position");var at=new Vec3(expected.get(0).getAsDouble(),expected.get(1).getAsDouble(),expected.get(2).getAsDouble());
      if(v.position().distanceTo(at)>.25||v.getDeltaMovement().horizontalDistance()>.05)return;
      // Ready snapshots are real tracked fields, not a server-side substitution.
      if(!exists(ROLE+"-case-"+clientCase+"-ready.json"))screenshot(mc,"prepared",()->write(ROLE+"-case-"+clientCase+"-ready.json",Map.of("peer_uuid",mc.player.getUUID().toString(),"actor",sample(a),"victim",sample(v),"full_impulse_baseline",seen(clientVictimId),"source_freeze",freeze())));return;
    }
    mc.setCameraEntity(mc.player);
    if(next.equals("armed")){if(c.run()){mc.player.setYRot(-90);mc.player.yRotO=-90;mc.player.setXRot(0);mc.player.xRotO=0;}write(ROLE+"-case-"+clientCase+"-armed.json",Map.of("peer_uuid",mc.player.getUUID().toString(),"source_freeze",freeze()));return;}
    if(next.equals("active")&&c.run()&&ROLE.equals("observer")&&!inputSent){inputSent=true;inputReleaseAt=localTicks+1;(c.flying()?mc.options.keyJump:mc.options.keyUp).setDown(true);write("observer-case-"+clientCase+"-input.json",Map.of("key",c.flying()?"native_keyJump":"native_keyUp","start_client_tick",localTicks,"local_before",sample(mc.player),"raw_flying_speed",mc.player.getAbilities().getFlyingSpeed(),"run_multiplier",EquippedRun084.multiplier(mc.player),"source_freeze",freeze()));}
    if(inputReleaseAt>=0&&localTicks>=inputReleaseAt){release(mc);inputReleaseAt=-1;write(ROLE+"-case-"+clientCase+"-input-released.json",Map.of("client_tick",localTicks,"one_native_input_tick",true,"source_freeze",freeze()));}
    clientSample(mc,"post");
    if(next.equals("settle")&&!exists(ROLE+"-case-"+clientCase+"-done.json")){
      long sequence=((Number)seen(clientVictimId).get("sequence")).longValue();boolean boost=!c.run()&&!c.mode().equals("owner_control")&&c.tier()>0;
      if(boost&&sequence<=seenBaseline)return;
      if(c.run()&&ROLE.equals("observer")&&EquippedRun084.multiplier(mc.player)!=1)return;
      if(c.run()&&ROLE.equals("observer")){
        if(!revokeInputSent){revokeInputSent=true;revokeReleaseAt=localTicks+2;(c.flying()?mc.options.keyJump:mc.options.keyUp).setDown(true);write("observer-case-"+clientCase+"-revoke-input.json",Map.of("client_tick",localTicks,"native_input_ticks",2,"local_before",sample(mc.player),"no_velocity_or_position_reset",true,"source_freeze",freeze(),"scope","Actual non-owner residual-momentum input after authoritative Run revocation; rollback/acceptance must be audited separately."));return;}
        if(localTicks<revokeReleaseAt)return;
        if(revokeReleaseAt>=0){release(mc);write("observer-case-"+clientCase+"-revoke-input-released.json",Map.of("client_tick",localTicks,"local_after",sample(mc.player),"native_input_ticks",2,"source_freeze",freeze()));revokeReleaseAt=-1;}
      }
      if(!boost&&!c.run())check(sequence<=seenBaseline,"zero/owner control receives no new Power correction; prior entries may expire normally");
      release(mc);var result=new LinkedHashMap<String,Object>();result.put("status",boost&&clientActiveHorizontalPeak<=3.9?"PACKET_ACCEPTED_ACTIVE_MOTION_SAMPLE_MISSING":"OBSERVED");result.put("case",clientCase);result.put("peer_uuid",mc.player.getUUID().toString());result.put("peak_actual_speed_all_stages",clientPeak);result.put("active_horizontal_peak",clientActiveHorizontalPeak);result.put("active_observed_distance",clientActiveDistance);result.put("full_impulse_after",seen(clientVictimId));result.put("baseline_sequence",seenBaseline);result.put("source_freeze",freeze());result.put("scope","Collection only. A packet-acceptance record is not a measured400.4 client vector. Audit actual active trace, missing tracking, first vector, positions and native handler separately; missing motion is not PASS.");write(ROLE+"-case-"+clientCase+"-done.json",result);
    }
  }
  @SubscribeEvent public static void client(ClientTickEvent.Post event){
    if(!enabled())return;var mc=Minecraft.getInstance();localTicks++;
    try{
      check(localTicks<16000,"overall actual-client timeout");check(fatal==null,"recorded local failure "+fatal);for(String role:List.of("server","host","observer"))check(!exists(role+"-failure.json"),"recorded peer failure "+role);
      if(mc.screen instanceof AccessibilityOnboardingScreen)mc.setScreen(new TitleScreen());
      if(!started&&mc.screen instanceof TitleScreen){mc.options.renderDistance().set(8);mc.options.simulationDistance().set(6);mc.options.framerateLimit().set(45);mc.options.pauseOnLostFocus=false;mc.options.hideGui=true;
        if(ROLE.equals("host")){check(!exists("phase.json")&&!exists("host-ready.json"),"fresh shared folder");started=true;world="Power084Remote-"+System.currentTimeMillis();mc.createWorldOpenFlows().createFreshLevel(world,new LevelSettings(world,GameType.CREATIVE,false,Difficulty.NORMAL,true,new GameRules(),WorldDataConfiguration.DEFAULT),new WorldOptions(848484L,false,false),a->a.registryOrThrow(Registries.WORLD_PRESET).getHolderOrThrow(WorldPresets.FLAT).value().createWorldDimensions(),mc.screen);}
        else if(exists("observer-connect.json")){started=true;ConnectScreen.startConnecting(mc.screen,mc,ServerAddress.parseString("127.0.0.1:"+PORT),new ServerData("Power084 actual remote","127.0.0.1:"+PORT,ServerData.Type.LAN),false,null);}
      }
      if(ROLE.equals("host")&&!setup&&mc.player!=null&&mc.getSingleplayerServer()!=null){setup=true;mc.setScreen(null);var server=mc.getSingleplayerServer();server.execute(()->{try{
        check(AccessoriesBridge.available(),"actual Accessories runtime");var level=server.overworld();level.setDayTime(6000);level.setWeatherParameters(0,100000,false,false);level.getGameRules().getRule(GameRules.RULE_DAYLIGHT).set(false,server);level.getGameRules().getRule(GameRules.RULE_DOMOBSPAWNING).set(false,server);
        for(int x=-16;x<=1216;x+=16)for(int z=-1;z<=0;z++)level.setChunkForced(x>>4,z,true);
        for(var at:BlockPos.betweenClosed(-8,79,-8,1208,83,8))level.setBlock(at,at.getY()==79?Blocks.SMOOTH_STONE.defaultBlockState():Blocks.AIR.defaultBlockState(),3);
        check(server.publishServer(GameType.SURVIVAL,false,PORT),"publish actual observer TCP server");published=true;phaseAt=System.nanoTime();write("case-catalog.json",Map.of("cases",CASES,"selected_context_ids",SELECTED,"unselected_context_ids",java.util.stream.IntStream.range(0,CASES.size()).filter(i->!SELECTED.contains(i)).boxed().toList(),"source_freeze",freeze(),"measurement_ticks",2,"limits",List.of("host local connection+one real TCP observer","remote victim direct dispatch is not natural AI PvP","prepared screenshots do not depict full post-hit flight","post-window stop is explicit fixture intervention")));write("host-ready.json",Map.of("port",PORT,"world",world,"profile",profile("host"),"source_freeze",freeze()));write("observer-connect.json",Map.of("port",PORT));
      }catch(Throwable e){fatal=e.toString();write("server-failure.json",Map.of("error",fatal));}});}
      if(mc.level!=null&&mc.player!=null&&!(mc.screen instanceof ReceivingLevelScreen)){var p=read("phase.json");if(p!=null)clientPhase(mc,p);}
    }catch(Throwable e){release(mc);fatal=e.toString();write(ROLE+"-failure.json",Map.of("error",fatal,"case",clientCase));LogUtils.getLogger().error("POWER084_CLIENT_FAILURE",e);mc.stop();}
  }
}
