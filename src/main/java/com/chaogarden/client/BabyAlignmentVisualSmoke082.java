package com.chaogarden.client;

import com.chaogarden.ChaoGarden;
import com.chaogarden.GardenConfig;
import com.chaogarden.companion.AccessoriesBridge;
import com.chaogarden.companion.CompanionContent;
import com.chaogarden.core.ChaoState;
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
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.portal.DimensionTransition;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.*;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.*;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.*;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/** Excluded opt-in verification. Real TCP clients, production accessory body,
 * actual accepted native deaths and ordinary entity-data replication. Fixture
 * setup creates equipment/ground, but never calls the alignment award helper. */
@EventBusSubscriber(modid=ChaoGarden.MODID,value=Dist.CLIENT)
public final class BabyAlignmentVisualSmoke082 {
  private static final Gson G=new GsonBuilder().setPrettyPrinting().create();
  private static final String ROLE=System.getenv().getOrDefault("CHAO_BABY082_ROLE","");
  private static final Path DIR=Path.of(System.getenv().getOrDefault("CHAO_BABY082_SHARED","/workspace/.setup/baby082/unconfigured/shared"));
  private static final int PORT=Integer.parseInt(System.getenv().getOrDefault("CHAO_BABY082_PORT","25584"));
  private static volatile String fatal;
  private static boolean started,setup,serverReady,completed,clientDone,screenshotPending,observerPosed;
  private static int localTicks,serverStage=-1,clientStage=-1,renderedFrames;
  private static long stageAt,clientStageAt;
  private static String worldName;private static UUID actorId,babyId,adultId;
  private static double plotX=0,plotY=81.1,plotZ=0;
  private static ResourceKey<Level> heroKey;
  private static long passiveSeedTick,lastReadinessDiagnostic;private static float passiveBefore,passiveStrength;private static int passiveAge,passiveAdultAge;
  private static int[] passiveLevels,passivePoints,passiveProgress;
  private static ArmorStand camera;
  private static Map<String,Object> renderProof=Map.of();
  private static final List<Map<String,Object>> serverProofs=new ArrayList<>();
  private BabyAlignmentVisualSmoke082(){}

  private static boolean enabled(){return ROLE.equals("host")||ROLE.equals("observer");}
  private static String freeze(){return System.getenv().getOrDefault("CHAO_BABY082_SOURCE_FREEZE","unrecorded");}
  private static void check(boolean ok,String reason){if(!ok)throw new IllegalStateException("BABY082 "+reason);}
  private static String sha(byte[] bytes){try{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));}catch(Exception e){throw new IllegalStateException(e);}}
  private static String sha(String text){return sha(text.getBytes(StandardCharsets.UTF_8));}
  private static String fileSha(Path p){try{return sha(Files.readAllBytes(p));}catch(Exception e){throw new IllegalStateException(e);}}
  private static synchronized void write(String name,Object value){try{Files.createDirectories(DIR);var p=DIR.resolve(name);var tmp=DIR.resolve(name+"."+ROLE+".tmp");Files.writeString(tmp,G.toJson(value)+"\n");Files.move(tmp,p,StandardCopyOption.ATOMIC_MOVE,StandardCopyOption.REPLACE_EXISTING);}catch(Exception e){throw new IllegalStateException(e);}}
  private static boolean exists(String name){return Files.isRegularFile(DIR.resolve(name));}
  private static JsonObject read(String name){try{return JsonParser.parseString(Files.readString(DIR.resolve(name))).getAsJsonObject();}catch(NoSuchFileException e){return null;}catch(Exception e){throw new IllegalStateException(e);}}
  private static String profile(String role){return role.equals("host")?"Baby82Host":"Baby82Observer";}
  private static ServerPlayer player(MinecraftServer server,String role){return server.getPlayerList().getPlayers().stream().filter(p->p.getGameProfile().getName().equals(profile(role))).findFirst().orElse(null);}
  private static void pose(ServerPlayer p,boolean host){p.setDeltaMovement(Vec3.ZERO);p.fallDistance=0;p.connection.teleport(plotX+(host?.5:4.5),plotY,plotZ+(host?.5:4.5),0,0);}
  private static ChaoEntity body(MinecraftServer server){var found=CompanionContent.findEntity(server,actorId);return found instanceof ChaoEntity c&&!c.isRemoved()?c:null;}
  private static void keepFixtureBody(ChaoEntity c){c.setNoAi(true);c.setNoGravity(true);c.setDeltaMovement(Vec3.ZERO);c.moveTo(plotX+1.5,plotY,plotZ+1.5,145,0);c.setYHeadRot(145);c.yBodyRot=145;}
  private static boolean done(int stage){return exists("host-stage-"+stage+"-done.json")&&exists("observer-stage-"+stage+"-done.json");}
  private static void closeTo(double actual,double want,String reason){check(Double.isFinite(actual)&&Math.abs(actual-want)<.000002,reason+" actual="+actual+" expected="+want);}
  private static void equip(MinecraftServer server,boolean adult){
    var host=player(server,"host");var s=new ChaoState();s.hunger=100;s.energy=100;s.align=adult?.8f:.20f;
    if(adult){s.adult=true;s.primaryType=ChaoState.RUN_TYPE;s.setEvolutionStrength(.4f);s=ChaoState.decode(s.encode());}
    var carrier=CompanionVisualSmokeSupport.equip(host,s);actorId=ChaoCarrierItem.storedUUID(carrier);check(actorId!=null,"real carrier retains its creature UUID");if(!adult)babyId=actorId;else adultId=actorId;
  }
  private static void kill(MinecraftServer server,EntityType<?> type){
    var host=player(server,"host");var created=type.create(host.serverLevel());check(created instanceof LivingEntity,"create real living victim");var victim=(LivingEntity)created;
    victim.moveTo(-3.5,81.1,1.5,0,0);victim.setNoGravity(true);if(victim instanceof Mob mob)mob.setNoAi(true);
    check(host.serverLevel().addFreshEntity(victim),"publish real victim before native damage");
    check(victim.hurt(victim.damageSources().playerAttack(host),Float.MAX_VALUE)&&victim.isDeadOrDying(),"actual player damage enters an accepted native death");
    // Replaying the native pipeline must not credit this accepted death again.
    for(int i=0;i<3;i++)victim.die(victim.damageSources().playerAttack(host));
    victim.discard();
  }
  private static void phase(MinecraftServer server,int stage,String label){
    var actor=body(server);check(actor!=null,"current production companion exists for stage");var host=player(server,"host");var observer=player(server,"observer");
    check(observer!=null&&!observer.getUUID().equals(host.getUUID()),"distinct actual TCP observer connected");
    var item=CompanionVisualSmokeSupport.held(host);check(!item.isEmpty()&&actor.getUUID().equals(ChaoCarrierItem.storedUUID(item)),"authoritative body matches the actual accessory item");
    closeTo(ChaoCarrierItem.state(item).align,actor.state().align,"carrier alignment matches the real body");
    serverStage=stage;stageAt=System.nanoTime();var p=new LinkedHashMap<String,Object>();p.put("stage",stage);p.put("label",label);p.put("actor_uuid",actorId.toString());p.put("baby_uuid",babyId.toString());p.put("owner_uuid",host.getUUID().toString());p.put("observer_uuid",observer.getUUID().toString());p.put("alignment",actor.state().align);p.put("adult",actor.state().adult);p.put("locked_alignment",actor.state().lockedAlignment());p.put("first_type",actor.state().lockedPrimaryType());p.put("server_state",actor.state().encode());p.put("server_state_sha256",sha(actor.state().encode()));p.put("carrier_state",ChaoCarrierItem.state(item).encode());p.put("source_freeze",freeze());p.put("dimension",actor.level().dimension().location().toString());p.put("aether_loaded",net.neoforged.fml.ModList.get().isLoaded("aether"));p.put("actor_position",List.of(actor.getX(),actor.getY(),actor.getZ()));p.put("scope","Server snapshot of real slot/body; clients compare identity/alignment/age category/first identity. Full care state may continue to advance normally.");write("server-stage-"+stage+"-snapshot.json",p);write("phase.json",p);
  }
  private static void equipRetained(ServerPlayer host,ItemStack carrier){
    check(babyId.equals(ChaoCarrierItem.storedUUID(carrier))&&CompanionContent.canEquip(carrier,host),"retained original baby passes normal accessory admission");
    try{
      var capType=Class.forName("io.wispforest.accessories.api.AccessoriesCapability");var cap=capType.getMethod("get",LivingEntity.class).invoke(null,host);
      var container=((Map<?,?>)capType.getMethod("getContainers").invoke(cap)).get("chao_companion");check(container!=null,"actual companion slot exists for restored baby");
      var type=Class.forName("io.wispforest.accessories.api.AccessoriesContainer");var inventory=(Container)type.getMethod("getAccessories").invoke(container);check(inventory.getItem(0).isEmpty(),"restored baby has a real empty slot destination");inventory.setItem(0,carrier);type.getMethod("markChanged",boolean.class).invoke(container,false);actorId=babyId;
    }catch(ReflectiveOperationException e){throw new IllegalStateException(e);}
  }
  private static void prepareHero(MinecraftServer server,ServerPlayer host,ServerPlayer observer){
    var retainedAdult=CompanionVisualSmokeSupport.held(host);CompanionVisualSmokeSupport.clear(host);
    int babySlot=-1;for(int i=0;i<host.getInventory().getContainerSize();i++)if(babyId.equals(ChaoCarrierItem.storedUUID(host.getInventory().getItem(i)))){check(babySlot<0,"only one original baby carrier exists");babySlot=i;}
    check(babySlot>=0,"original baby was retained in actual inventory during adult check");var retainedBaby=host.getInventory().removeItemNoUpdate(babySlot);check(!retainedBaby.isEmpty(),"take original baby from its real slot");check(host.getInventory().add(retainedAdult),"retain the real adult carrier while restoring baby");equipRetained(host,retainedBaby);
    heroKey=ResourceKey.create(Registries.DIMENSION,ResourceLocation.parse(net.neoforged.fml.ModList.get().isLoaded("aether")?"aether:the_aether":"minecraft:the_end"));var hero=server.getLevel(heroKey);check(hero!=null,"real optional/substitute Hero dimension exists");
    plotX=2048;plotY=240.1;plotZ=2048;
    // Finite, new fixture footing only. Existing natural footing is accepted;
    // occupied blocks/headroom are never cleared to force a test to pass.
    for(int x=2040;x<=2060;x++)for(int z=2040;z<=2060;z++){
      var floor=new BlockPos(x,239,z);var old=hero.getBlockState(floor);
      check(old.isAir()||old.isFaceSturdy(hero,floor,net.minecraft.core.Direction.UP),"new fixture footprint has air or already-solid natural footing at "+floor);
      for(int y=240;y<=244;y++)check(hero.getBlockState(new BlockPos(x,y,z)).getCollisionShape(hero,new BlockPos(x,y,z)).isEmpty(),"new fixture headroom is naturally clear");
      if(old.isAir())hero.setBlock(floor,Blocks.STONE.defaultBlockState(),3);
    }
    for(var p:List.of(host,observer)){boolean keeper=p==host;var at=new Vec3(plotX+(keeper?.5:4.5),plotY,plotZ+(keeper?.5:4.5));p.changeDimension(new DimensionTransition(hero,at,Vec3.ZERO,0,0,DimensionTransition.DO_NOTHING));p.hasChangedDimension();}
    serverStage=40;stageAt=System.nanoTime();write("phase.json",Map.of("stage",40,"label","awaiting-real-hero-dimension-readiness","hero_dimension",heroKey.location().toString(),"actor_uuid",babyId.toString(),"source_freeze",freeze()));
  }
  private static void finish(MinecraftServer server){
    var host=player(server,"host");server.getPlayerList().saveAll();check(server.saveEverything(true,true,true),"normal player/world save succeeds");
    write("world-details.json",Map.of("world",worldName,"game_directory",Minecraft.getInstance().gameDirectory.toString(),"baby_uuid",babyId.toString(),"adult_uuid",adultId.toString(),"owner_uuid",host.getUUID().toString(),"source_freeze",freeze(),"scope","Normal actual world/player save only; no separate-process reopen is claimed by this helper."));
    write("server-complete.json",Map.of("status","PASS","stages",List.copyOf(serverProofs),"source_freeze",freeze(),"hostile_points",GardenConfig.BABY_HOSTILE_KILL_ALIGNMENT_POINTS.get(),"peaceful_points",GardenConfig.BABY_PEACEFUL_KILL_ALIGNMENT_POINTS.get(),"aether_loaded",net.neoforged.fml.ModList.get().isLoaded("aether"),"passive_hero_dimension",heroKey.location().toString()));
    write("phase.json",Map.of("stage",99,"label","complete","source_freeze",freeze()));completed=true;
  }
  @SubscribeEvent public static void server(ServerTickEvent.Post event){
    if(!enabled()||!ROLE.equals("host")||!serverReady||completed||fatal!=null)return;var server=event.getServer();
    try{
      check(System.nanoTime()-stageAt<(serverStage==40?240_000_000_000L:90_000_000_000L),"actual server stage timeout "+serverStage);var host=player(server,"host");check(host!=null,"real host stays connected");var observer=player(server,"observer");
      if(observer==null)return;if(!observerPosed){pose(observer,false);observerPosed=true;}
      var actor=body(server);if(actor==null){
        if(serverStage==40&&System.nanoTime()-lastReadinessDiagnostic>3_000_000_000L){lastReadinessDiagnostic=System.nanoTime();var hero=server.getLevel(heroKey);write("hero-readiness-wait.json",Map.of("scope","Read-only actual player-ticket readiness diagnostic, not a pass or substitute actor.","target_dimension",heroKey.location().toString(),"host_dimension",host.level().dimension().location().toString(),"observer_dimension",observer.level().dimension().location().toString(),"body_uuid",actorId.toString(),"body_accessible",false,"host_position_entity_ticking",hero!=null&&hero.isPositionEntityTicking(host.blockPosition()),"host_destination_ack",exists("host-hero-dimension-ready.json"),"observer_destination_ack",exists("observer-hero-dimension-ready.json"),"source_freeze",freeze()));}return;
      }
      if(serverStage<0){keepFixtureBody(actor);phase(server,0,"equipped-baby-baseline");return;}
      if(serverStage==0&&done(0)){
        float before=actor.state().align;kill(server,EntityType.ZOMBIE);closeTo(actor.state().align,before+GardenConfig.BABY_HOSTILE_KILL_ALIGNMENT_POINTS.get()/100.0,"accepted repeated native hostile death changes baby once");serverProofs.add(Map.of("stage","accepted_hostile_death_once","before",before,"after",actor.state().align,"actor_uuid",actorId.toString()));phase(server,1,"baby-after-hostile-kill");return;
      }
      if(serverStage==1&&done(1)){
        float before=actor.state().align;kill(server,EntityType.COW);closeTo(actor.state().align,before-GardenConfig.BABY_PEACEFUL_KILL_ALIGNMENT_POINTS.get()/100.0,"accepted repeated native peaceful death changes baby once");serverProofs.add(Map.of("stage","accepted_peaceful_death_once","before",before,"after",actor.state().align,"actor_uuid",actorId.toString()));phase(server,2,"baby-after-peaceful-kill");return;
      }
      if(serverStage==2&&done(2)){
        var retained=CompanionVisualSmokeSupport.held(host);CompanionVisualSmokeSupport.clear(host);check(!retained.isEmpty()&&babyId.equals(ChaoCarrierItem.storedUUID(retained)),"unequip retains the exact baby carrier");check(host.getInventory().add(retained),"move this fixture's real baby carrier to inventory before adult fixture");equip(server,true);serverStage=30;stageAt=System.nanoTime();return;
      }
      if(serverStage==30){
        if(!actor.state().adult)return;keepFixtureBody(actor);float before=actor.state().align,locked=actor.state().lockedAlignment();int first=actor.state().lockedPrimaryType();
        kill(server,EntityType.ZOMBIE);closeTo(actor.state().align,before,"adult public alignment does not change after hostile death");kill(server,EntityType.COW);closeTo(actor.state().align,before,"adult public alignment does not change after peaceful death");check(actor.state().lockedAlignment()==locked&&actor.state().lockedPrimaryType()==first,"adult evolved identity remains locked");serverProofs.add(Map.of("stage","adult_identity_and_mirror_unchanged","actor_uuid",actorId.toString(),"alignment",before,"first_type",first));phase(server,3,"adult-identity-stays-locked");return;
      }
      if(serverStage==3&&done(3)){
        prepareHero(server,host,observer);return;
      }
      if(serverStage==40){
        var hero=server.getLevel(heroKey);check(hero!=null,"actual selected Hero dimension exists");
        if(host.level()!=hero||observer.level()!=hero||actor.level()!=hero||!hero.isPositionEntityTicking(actor.blockPosition())
            ||!exists("host-hero-dimension-ready.json")||!exists("observer-hero-dimension-ready.json"))return;
        var hostAck=read("host-hero-dimension-ready.json");var observerAck=read("observer-hero-dimension-ready.json");check(hostAck.get("actual_dimension").getAsString().equals(heroKey.location().toString())&&observerAck.get("actual_dimension").getAsString().equals(heroKey.location().toString())&&hostAck.get("peer_uuid").getAsString().equals(host.getUUID().toString())&&observerAck.get("peer_uuid").getAsString().equals(observer.getUUID().toString()),"both actual destination acknowledgements belong to the correct peers and selected dimension");
        check(actor.getUUID().equals(babyId)&&!actor.state().adult&&CompanionContent.isManaged(actor),"same original baby is the actual current equipped body in the selected dimension");
        check(hero.getEntity(babyId)==actor,"real server entity lookup exposes the current body before seeding");
        check(AccessoriesBridge.equipped(host).stream().anyMatch(stack->babyId.equals(ChaoCarrierItem.storedUUID(stack))),"original baby remains in the real accessory slot");
        keepFixtureBody(actor);passiveBefore=actor.state().align;check(passiveBefore<.99f,"original fixture baby has room for one visible Hero point");passiveAge=actor.state().ageTicks;passiveAdultAge=actor.state().adultAgeTicks;passiveStrength=actor.state().evolutionStrength();
        passiveLevels=actor.state().levels.clone();passivePoints=actor.state().points.clone();passiveProgress=actor.state().progress.clone();passiveSeedTick=server.getTickCount();
        actor.getPersistentData().putInt("ChaoBabyAlignmentHeroTicks082",1199);CompanionContent.itemStateChanged(actor);
        var seed=new LinkedHashMap<String,Object>();seed.put("scope","Fixture seeds only1199 earned-time residual after actual player/dimension/entity-ticking readiness. Production accessory ticks apply the final point; this is a boundary test, not an unaccelerated minute claim.");seed.put("dimension",heroKey.location().toString());seed.put("aether_loaded",net.neoforged.fml.ModList.get().isLoaded("aether"));seed.put("baby_uuid",babyId.toString());seed.put("server_tick",passiveSeedTick);seed.put("alignment_before",passiveBefore);seed.put("age_before",passiveAge);seed.put("adult_age_before",passiveAdultAge);seed.put("strength_before",passiveStrength);seed.put("hero_ticks_seeded",1199);seed.put("actual_entity_ticking",hero.isPositionEntityTicking(actor.blockPosition()));seed.put("source_freeze",freeze());write("passive-hero-seed.json",seed);
        serverStage=41;stageAt=System.nanoTime();return;
      }
      if(serverStage==41){
        if(actor.state().align==passiveBefore)return;
        long elapsed=server.getTickCount()-passiveSeedTick;check(elapsed>=1&&actor.getPersistentData().getLong("ChaoBabyAlignmentLastTick082")>passiveSeedTick,"at least one actual production accessory tick runs after seeding");closeTo(actor.state().align,passiveBefore+.01,"selected Hero dimension produces exactly one passive alignment point");
        check(actor.getUUID().equals(babyId)&&!actor.state().adult&&actor.state().ageTicks==passiveAge&&actor.state().adultAgeTicks==passiveAdultAge&&actor.state().evolutionStrength()==passiveStrength,"passive equipped influence preserves baby identity, paused age and visual strength");
        check(Arrays.equals(actor.state().levels,passiveLevels)&&Arrays.equals(actor.state().points,passivePoints)&&Arrays.equals(actor.state().progress,passiveProgress),"passive alignment does not invent stat training");
        closeTo(ChaoCarrierItem.state(CompanionVisualSmokeSupport.held(host)).align,actor.state().align,"real carrier immediately commits the passive point");
        var proof=new LinkedHashMap<String,Object>();proof.put("stage","actual_passive_hero_dimension");proof.put("dimension",heroKey.location().toString());proof.put("aether_loaded",net.neoforged.fml.ModList.get().isLoaded("aether"));proof.put("before",passiveBefore);proof.put("after",actor.state().align);proof.put("actor_uuid",actorId.toString());proof.put("seed_server_tick",passiveSeedTick);proof.put("last_passive_tick",actor.getPersistentData().getLong("ChaoBabyAlignmentLastTick082"));proof.put("elapsed_server_ticks",elapsed);proof.put("hero_ticks_after",actor.getPersistentData().getInt("ChaoBabyAlignmentHeroTicks082"));proof.put("actual_entity_ticking",((ServerLevel)actor.level()).isPositionEntityTicking(actor.blockPosition()));proof.put("paused_age",actor.state().ageTicks);proof.put("paused_adult_age",actor.state().adultAgeTicks);proof.put("scope","Actual client-ticketed dimension and production tick at seeded minute boundary, without a direct award-helper call.");serverProofs.add(proof);phase(server,4,"baby-passive-hero-dimension");return;
      }
      if(serverStage==4&&done(4))finish(server);
    }catch(Throwable e){fatal=e.toString();if(!exists("server-failure.json"))write("server-failure.json",Map.of("error",fatal,"stage",serverStage));LogUtils.getLogger().error("BABY082_SERVER_FAILURE",e);}
  }
  private static ChaoEntity clientActor(Minecraft mc){if(mc.level==null||actorId==null)return null;for(var e:mc.level.entitiesForRendering())if(e.getUUID().equals(actorId)&&e instanceof ChaoEntity c)return c;return null;}
  private static boolean matches(ChaoEntity actor,JsonObject phase){return actor!=null&&actor.level().dimension().location().toString().equals(phase.get("dimension").getAsString())&&actor.getUUID().toString().equals(phase.get("actor_uuid").getAsString())&&actor.state().adult==phase.get("adult").getAsBoolean()&&Math.abs(actor.state().align-phase.get("alignment").getAsDouble())<.000002&&Math.abs(actor.state().lockedAlignment()-phase.get("locked_alignment").getAsDouble())<.000002&&actor.state().lockedPrimaryType()==phase.get("first_type").getAsInt();}
  @SubscribeEvent public static void rendered(RenderLivingEvent.Post<?,?> event){
    if(!enabled()||clientStage<0||screenshotPending||fatal!=null)return;var mc=Minecraft.getInstance();if(!(event.getEntity() instanceof ChaoEntity actor)||actorId==null||!actor.getUUID().equals(actorId))return;
    try{var p=read("phase.json");if(p==null||p.get("stage").getAsInt()!=clientStage||clientStage>4||!matches(actor,p))return;renderedFrames++;renderProof=Map.of("actual_production_living_render",true,"renderer",event.getRenderer().getClass().getName(),"actor_uuid",actor.getUUID().toString(),"state_alignment",actor.state().align,"adult",actor.state().adult,"locked_alignment",actor.state().lockedAlignment(),"first_type",actor.state().lockedPrimaryType());}catch(Throwable e){fatal=e.toString();}
  }
  private static void view(Minecraft mc,ChaoEntity actor){double tx=actor.getX(),ty=actor.getY()+.6,tz=actor.getZ(),x=tx+4.5,y=ty+2.3,z=tz+6.5;float yaw=(float)Math.toDegrees(Math.atan2(x-tx,tz-z)),pitch=(float)-Math.toDegrees(Math.atan2(ty-y,Math.hypot(tx-x,tz-z)));if(camera==null||camera.level()!=mc.level)camera=new ArmorStand(mc.level,x,y,z);camera.moveTo(x,y-camera.getEyeHeight(),z,yaw,pitch);camera.xo=camera.getX();camera.yo=camera.getY();camera.zo=camera.getZ();camera.yRotO=yaw;camera.xRotO=pitch;camera.setYHeadRot(yaw);camera.yHeadRotO=yaw;mc.setCameraEntity(camera);}
  private static void capture(Minecraft mc,JsonObject p,ChaoEntity actor){
    int stage=clientStage;String name="baby082-"+ROLE+"-stage"+stage+".png";var d=new LinkedHashMap<String,Object>();d.put("role",ROLE);d.put("stage",stage);d.put("peer_uuid",mc.player.getUUID().toString());d.put("actor_uuid",actorId.toString());d.put("alignment",actor.state().align);d.put("adult",actor.state().adult);d.put("locked_alignment",actor.state().lockedAlignment());d.put("first_type",actor.state().lockedPrimaryType());d.put("actual_client_state",actor.state().encode());d.put("actual_client_state_sha256",sha(actor.state().encode()));d.put("expected_fields",p);d.put("actual_render_proof",new LinkedHashMap<>(renderProof));d.put("native_render_frames",renderedFrames);d.put("source_freeze",freeze());d.put("dimension",mc.level.dimension().location().toString());d.put("scope","Actual tracked client entity fields + production renderer/native framebuffer. Full care-state equality is not asserted.");screenshotPending=true;
    Screenshot.grab(mc.gameDirectory,name,mc.getMainRenderTarget(),message->{try{var image=mc.gameDirectory.toPath().resolve("screenshots").resolve(name);check(Files.isRegularFile(image),"actual native screenshot completed");d.put("png_path",image.toString());d.put("png_sha256",fileSha(image));write(ROLE+"-stage-"+stage+"-view.json",d);write(ROLE+"-stage-"+stage+"-done.json",Map.of("status","PASS","stage",stage,"peer_uuid",mc.player.getUUID().toString(),"source_freeze",freeze()));screenshotPending=false;}catch(Throwable e){fatal=e.toString();}});
  }
  private static void phaseClient(Minecraft mc,JsonObject p){
    int stage=p.get("stage").getAsInt();
    if(stage==99){if(!clientDone){mc.setCameraEntity(mc.player);write(ROLE+"-complete.json",Map.of("status","PASS","peer_uuid",mc.player.getUUID().toString(),"source_freeze",freeze()));clientDone=true;}if(exists("host-complete.json")&&exists("observer-complete.json"))mc.stop();return;}
    if(stage==40){
      mc.setCameraEntity(mc.player);String expected=p.get("hero_dimension").getAsString();
      if(mc.level.dimension().location().toString().equals(expected)&&!exists(ROLE+"-hero-dimension-ready.json"))write(ROLE+"-hero-dimension-ready.json",Map.of("actual_dimension",expected,"peer_uuid",mc.player.getUUID().toString(),"source_freeze",freeze(),"scope","Actual destination client level acknowledgement after native server dimension transition."));
      return;
    }
    if(stage<0||stage>4)return;
    if(clientStage!=stage){clientStage=stage;actorId=UUID.fromString(p.get("actor_uuid").getAsString());renderedFrames=0;renderProof=Map.of();clientStageAt=System.nanoTime();screenshotPending=false;}
    if(exists(ROLE+"-stage-"+stage+"-done.json"))return;
    check(System.nanoTime()-clientStageAt<45_000_000_000L,"tracked peer state failed to synchronize stage "+stage);var actor=clientActor(mc);if(actor!=null)view(mc,actor);if(mc.screen!=null||mc.getOverlay()!=null||screenshotPending)return;
    if(!matches(actor,p)||renderedFrames<25||System.nanoTime()-clientStageAt<1_200_000_000L)return;capture(mc,p,actor);
  }
  @SubscribeEvent public static void client(ClientTickEvent.Post event){
    if(!enabled())return;var mc=Minecraft.getInstance();localTicks++;
    try{
      check(localTicks<12000,"managed actual-client timeout");check(fatal==null,"local/server error "+fatal);for(String peer:List.of("server-failure.json","host-failure.json","observer-failure.json"))if(exists(peer))throw new IllegalStateException("Recorded peer failure at "+DIR.resolve(peer));
      if(mc.screen instanceof AccessibilityOnboardingScreen)mc.setScreen(new TitleScreen());
      if(!started&&mc.screen instanceof TitleScreen){
        mc.options.renderDistance().set(6);mc.options.simulationDistance().set(6);mc.options.framerateLimit().set(40);mc.options.fov().set(70);mc.options.pauseOnLostFocus=false;mc.options.hideGui=true;
        if(ROLE.equals("host")){check(!exists("host-ready.json")&&!exists("phase.json"),"fresh evidence directory required");started=true;worldName="Baby082Remote-"+System.currentTimeMillis();mc.createWorldOpenFlows().createFreshLevel(worldName,new LevelSettings(worldName,GameType.CREATIVE,false,Difficulty.NORMAL,true,new GameRules(),WorldDataConfiguration.DEFAULT),new WorldOptions(828282L,false,false),a->a.registryOrThrow(Registries.WORLD_PRESET).getHolderOrThrow(WorldPresets.FLAT).value().createWorldDimensions(),mc.screen);}
        else if(exists("observer-connect.json")){started=true;ConnectScreen.startConnecting(mc.screen,mc,ServerAddress.parseString("127.0.0.1:"+PORT),new ServerData("Baby082 actual LAN","127.0.0.1:"+PORT,ServerData.Type.LAN),false,null);}
      }
      if(ROLE.equals("host")&&!setup&&mc.player!=null&&mc.getSingleplayerServer()!=null){setup=true;mc.setScreen(null);var server=mc.getSingleplayerServer();server.execute(()->{try{
        check(AccessoriesBridge.available(),"actual Accessories runtime is required");var host=player(server,"host");check(host!=null,"actual named host connected");
        var level=host.serverLevel();for(int x=-12;x<=12;x++)for(int z=-12;z<=12;z++)level.setBlock(new BlockPos(x,80,z),Blocks.STONE.defaultBlockState(),3);level.setDayTime(6000);level.setWeatherParameters(0,100000,false,false);pose(host,true);equip(server,false);
        check(server.publishServer(GameType.SURVIVAL,false,PORT),"actual integrated server publishes to TCP");stageAt=System.nanoTime();serverReady=true;write("host-ready.json",Map.of("port",PORT,"world",worldName,"source_freeze",freeze()));write("observer-connect.json",Map.of("port",PORT,"scope","Actual late TCP peer joins after real host accessory was equipped."));
      }catch(Throwable e){fatal=e.toString();if(!exists("server-failure.json"))write("server-failure.json",Map.of("error",fatal));}});}
      if(mc.level!=null&&mc.player!=null&&!(mc.screen instanceof ReceivingLevelScreen)){var p=read("phase.json");if(p!=null)phaseClient(mc,p);}
    }catch(Throwable e){if(mc.player!=null)mc.setCameraEntity(mc.player);if(!exists(ROLE+"-failure.json"))write(ROLE+"-failure.json",Map.of("error",e.toString(),"stage",clientStage));LogUtils.getLogger().error("BABY082_CLIENT_FAILURE",e);mc.stop();}
  }
}
