package com.chaogarden.client;

import com.chaogarden.ChaoGarden;
import com.chaogarden.core.ChaoState;
import com.chaogarden.entity.ChaoEntity;
import com.chaogarden.hub.*;
import com.google.gson.*;
import com.mojang.logging.LogUtils;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;
import net.minecraft.client.*;
import net.minecraft.client.gui.screens.*;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.resolver.ServerAddress;
import net.minecraft.core.*;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.*;
import net.minecraft.world.*;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.item.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.*;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.*;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/** Excluded development harness: real integrated LAN, late peer and production lobby renderer.
 * Acquisition fixtures use published owned Chao and normal sync, never unlock-flag setters.
 * The only restored block is this fresh test world's own deliberate chest-conflict cell.
 * A separate process can reopen the actual saved world without creating new Chao.
 */
@EventBusSubscriber(modid=ChaoGarden.MODID,value=Dist.CLIENT)
public final class GardenRemoteVisualSmoke081 {
  private static final Gson G=new GsonBuilder().setPrettyPrinting().create();
  private static final String ROLE=System.getenv().getOrDefault("CHAO_GARDEN081_ROLE","");
  private static final Path DIR=Path.of(System.getenv().getOrDefault("CHAO_GARDEN081_SHARED","/workspace/.setup/garden081/unconfigured/shared"));
  private static final int PORT=Integer.parseInt(System.getenv().getOrDefault("CHAO_GARDEN081_PORT","25583"));
  private static final String REOPEN=System.getenv().getOrDefault("CHAO_GARDEN081_REOPEN_WORLD","");
  private static final String EXPECT=System.getenv().getOrDefault("CHAO_GARDEN081_REOPEN_EXPECTATIONS","");
  private static final BlockPos ANCHOR=new BlockPos(0,78,0);
  // Identical cameras across all access states; detached local ArmorStand is never published.
  private static final double[][] VIEWS={{0,101,31,0,79,0},{22,94,11,18,84,-8},{-23,86,-8,-18,77,10},{-17.5,74,4,-17.5,72,12}};
  private static final String[] VIEW_NAMES={"overview","hero-side","dark-side","dark-door"};
  private static volatile String fatal;
  private static boolean started,setup,serverReady,published,completed,clientDone;
  private static volatile boolean screenshotPending;
  private static int localTicks,serverStage=-1,clientStage=-1,view,frames;
  private static long stageAt,viewAt;
  private static String worldName;private static Path gameDir;
  private static ArmorStand camera;
  private static List<BlockPos> serverCritical=List.of(),clientCritical=List.of();
  private static BlockState conflictOriginal;private static String conflictBeforeHash,conflictItemHash;
  private static UUID heroId,darkId;
  private static final List<Map<String,Object>> serverProofs=new ArrayList<>();
  private static boolean enabled(){return ROLE.equals("host")||ROLE.equals("observer");}
  private static boolean reopen(){return !REOPEN.isBlank();}
  private static void check(boolean test,String message){if(!test)throw new IllegalStateException("GARDEN081 "+message);}
  private static String sha(String text){try{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(text.getBytes(StandardCharsets.UTF_8)));}catch(Exception e){throw new IllegalStateException(e);}}
  private static String fileSha(Path p){try{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(p)));}catch(Exception e){throw new IllegalStateException(e);}}
  private static synchronized void write(String name,Object object){try{Files.createDirectories(DIR);var p=DIR.resolve(name);var tmp=DIR.resolve(name+"."+ROLE+".tmp");Files.writeString(tmp,G.toJson(object)+"\n");Files.move(tmp,p,StandardCopyOption.ATOMIC_MOVE,StandardCopyOption.REPLACE_EXISTING);}catch(Exception e){throw new IllegalStateException(e);}}
  private static JsonObject read(Path p){try{return JsonParser.parseString(Files.readString(p)).getAsJsonObject();}catch(java.nio.file.NoSuchFileException e){return null;}catch(Exception e){throw new IllegalStateException(e);}}
  private static JsonObject read(String name){return read(DIR.resolve(name));}
  private static boolean exists(String name){return Files.isRegularFile(DIR.resolve(name));}
  private static String freeze(){return System.getenv().getOrDefault("CHAO_GARDEN081_SOURCE_FREEZE","unrecorded");}
  private static String profile(String role){return role.equals("host")?"Garden81Host":"Garden81Observer";}
  private static ServerPlayer player(MinecraftServer server,String role){return server.getPlayerList().getPlayers().stream().filter(p->p.getGameProfile().getName().equals(profile(role))).findFirst().orElse(null);}
  private static void safeBodyPose(ServerPlayer player,boolean host){double x=host?9.5:-9.5;player.setDeltaMovement(Vec3.ZERO);player.fallDistance=0;player.connection.teleport(x,80.1,x,0,0);}
  private static int access(Level level){var state=level.getBlockState(ANCHOR);return state.is(LobbyDecor025.SCENE_ANCHOR.get())&&state.getValue(LobbyDecor025.SceneAnchor.GARDEN_LAYOUT81)?state.getValue(LobbyDecor025.SceneAnchor.GARDEN_ACCESS81):-1;}
  private static String stateHash(Level level){var b=new StringBuilder();for(var p:level.isClientSide?clientCritical:serverCritical)b.append(p.getX()).append(',').append(p.getY()).append(',').append(p.getZ()).append('=').append(level.getBlockState(p)).append('\n');return sha(b.toString());}
  private static List<List<Integer>> positions(){return serverCritical.stream().map(p->List.of(p.getX(),p.getY(),p.getZ())).toList();}
  private static void initCritical(){var cells=new HashSet<BlockPos>();cells.addAll(HubRooms.plan081(false,false).blocks.keySet());cells.addAll(HubRooms.plan081(true,true).blocks.keySet());serverCritical=cells.stream().sorted(Comparator.comparingInt((BlockPos p)->p.getY()).thenComparingInt(p->p.getZ()).thenComparingInt(p->p.getX())).toList();}
  private static void checkPlan(ServerLevel level,int mask){var plan=HubRooms.plan081((mask&1)!=0,(mask&2)!=0);for(var p:serverCritical){var want=plan.blocks.getOrDefault(p,Blocks.AIR.defaultBlockState());check(level.getBlockState(p).equals(want),"actual lobby differs from complete authored state at "+p+" actual="+level.getBlockState(p)+" expected="+want);}check(access(level)==mask,"server native scene access marker matches "+mask);}
  private static void phase(MinecraftServer server,int stage,int mask,String label){serverStage=stage;stageAt=server.overworld().getGameTime();var hub=server.getLevel(HubContent.HUB);var p=new LinkedHashMap<String,Object>();p.put("stage",stage);p.put("label",label);p.put("mask",mask);p.put("server_tick",stageAt);p.put("critical_positions",positions());p.put("actual_block_state_sha256",stateHash(hub));p.put("roles",reopen()?List.of("host"):stage==0?List.of("host"):List.of("host","observer"));p.put("source_freeze",freeze());write("server-stage-"+stage+"-snapshot.json",p);write("phase.json",p);}
  private static boolean clientsDone(int stage){return exists("host-stage-"+stage+"-done.json")&&(reopen()||stage==0||exists("observer-stage-"+stage+"-done.json"));}
  private static void owned(MinecraftServer server,boolean hero){var host=player(server,"host");var hub=server.getLevel(HubContent.HUB);var c=ChaoGarden.CHAO.get().create(hub);check(c!=null,"create actual owned fixture");var s=new ChaoState();s.align=hero?1:-1;s.hunger=90;s.energy=90;c.owner=host.getUUID();c.setState(s);c.setCustomName(net.minecraft.network.chat.Component.literal(hero?"Garden81HeroFixture":"Garden81DarkFixture"));c.setNoAi(true);c.moveTo(hero?5.5:-5.5,80.1,5.5,0,0);check(hub.addFreshEntity(c),"publish actual owned Chao");c.sync();if(hero)heroId=c.getUUID();else darkId=c.getUUID();}
  private static String chestItem(ServerLevel level){check(level.getBlockEntity(HubRooms.DARK_DOOR) instanceof ChestBlockEntity,"protected chest remains a real block entity");var chest=(ChestBlockEntity)level.getBlockEntity(HubRooms.DARK_DOOR);return sha(chest.getItem(0).save(level.registryAccess()).toString());}
  /** Move this fresh fixture's real contents before manually removing its chest.
   * Native chest removal drops inventory, and the production occupancy guard
   * correctly rejects those item entities. Never bypass that guard. */
  private static void retainFixtureContents(MinecraftServer server,ServerLevel hub){
    var host=player(server,"host");var chest=(ChestBlockEntity)hub.getBlockEntity(HubRooms.DARK_DOOR);check(chest!=null&&chestItem(hub).equals(conflictItemHash),"all seven protected diamonds survive before fixture cleanup");
    int slot=host.getInventory().getFreeSlot();check(slot>=0,"fresh fixture has a real empty destination slot before any inventory mutation");
    int before=0;for(int i=0;i<host.getInventory().getContainerSize();i++)if(host.getInventory().getItem(i).is(Items.DIAMOND))before+=host.getInventory().getItem(i).getCount();
    var retained=chest.removeItemNoUpdate(0);check(retained.is(Items.DIAMOND)&&retained.getCount()==7,"take exactly the original seven-diamond fixture payload");
    host.getInventory().setItem(slot,retained);host.getInventory().setChanged();host.containerMenu.broadcastChanges();chest.setChanged();
    int after=0;for(int i=0;i<host.getInventory().getContainerSize();i++)if(host.getInventory().getItem(i).is(Items.DIAMOND))after+=host.getInventory().getItem(i).getCount();
    check(chest.isEmpty()&&after==before+7&&sha(host.getInventory().getItem(slot).save(hub.registryAccess()).toString()).equals(conflictItemHash),"fixture cleanup conserves the exact protected stack in the host inventory");
    serverProofs.add(Map.of("stage","fixture_only_inventory_transfer_before_manual_cell_restore","chest_before",7,"transferred",7,"host_before",before,"host_after",after,"destination_slot",slot,"original_stack_sha256",conflictItemHash,"scope","Test fixture cleanup only; production geometry never moves or drops player inventories."));
  }
  private static int command(MinecraftServer server,String text){try{return server.getCommands().getDispatcher().execute(text,player(server,"host").createCommandSourceStack().withPermission(2));}catch(Exception e){throw new IllegalStateException(e);}}
  private static Map<String,Object> diskProof(){try{var file=gameDir.resolve("saves").resolve(worldName).resolve("data/chao_hub_visits.dat");check(Files.isRegularFile(file),"actual server SavedData file exists");var data=NbtIo.readCompressed(file,NbtAccounter.unlimitedHeap()).getCompound("data").getCompound("GardenUnlocks081");check(data.getBoolean("Hero")&&data.getBoolean("Dark"),"actual compressed disk SavedData retains both independent unlocks");return Map.of("path",file.toString(),"sha256",fileSha(file),"Hero",true,"Dark",true,"scope","Native NbtIo read of actual compressed SavedData; no file edits or unlock observations.");}catch(java.io.IOException e){throw new IllegalStateException(e);}}
  private static void savedProof(MinecraftServer server){server.getPlayerList().saveAll();check(server.saveEverything(true,true,true),"actual test world saves successfully");write("world-details.json",Map.of("world",worldName,"game_directory",gameDir.toString(),"hero_chao_uuid",heroId.toString(),"dark_chao_uuid",darkId.toString(),"owner_uuid",player(server,"host").getUUID().toString(),"access_mask",3,"disk_unlock_proof",diskProof(),"source_freeze",freeze(),"scope","Actual world saved through normal server save, for an optional separate-process reopen check."));}
  private static void finishServer(MinecraftServer server){if(!reopen())savedProof(server);write("server-complete.json",Map.of("status","PASS","mode",reopen()?"actual_saved_world_reopen":"fresh_unlock_late_join_conflict_portals","stages",List.copyOf(serverProofs),"source_freeze",freeze()));write("phase.json",Map.of("stage",99,"label","complete","mask",3,"roles",reopen()?List.of("host"):List.of("host","observer"),"source_freeze",freeze()));completed=true;}
  @SubscribeEvent public static void server(ServerTickEvent.Post event){
    if(!enabled()||!ROLE.equals("host")||!serverReady||fatal!=null||completed)return;var server=event.getServer();var hub=server.getLevel(HubContent.HUB);long time=server.overworld().getGameTime();
    try{
      check(time-stageAt<1800,"server stage timeout "+serverStage);var host=player(server,"host");check(host!=null,"real host stays connected");
      if(serverStage<0){
        if(reopen()){
          check(GardenUnlocks081.isUnlocked(server,true)&&GardenUnlocks081.isUnlocked(server,false),"saved server unlock flags persist before any new acquisition");
          var expect=read(Path.of(EXPECT));check(expect!=null,"read prior actual saved-world identity proof");heroId=UUID.fromString(expect.get("hero_chao_uuid").getAsString());darkId=UUID.fromString(expect.get("dark_chao_uuid").getAsString());
          if(hub.getEntity(heroId)==null||hub.getEntity(darkId)==null)return;
          check(hub.getEntity(heroId) instanceof ChaoEntity&&hub.getEntity(darkId) instanceof ChaoEntity,"same actual saved Chao UUIDs reload");checkPlan(hub,3);phase(server,10,3,"reopened-both-unlocked");return;
        }
        check(!GardenUnlocks081.isUnlocked(server,true)&&!GardenUnlocks081.isUnlocked(server,false),"fresh server starts independently locked");checkPlan(hub,0);
        check(!HubContent.visitHeroGarden(host)&&!HubContent.visitDarkGarden(host)&&host.level()==hub,"native locked portal entry refuses both without dimension transfer");phase(server,0,0,"fresh-locked");return;
      }
      if(reopen()){if(clientsDone(10)){serverProofs.add(Map.of("stage","actual_restart","flags",3,"same_saved_chao_uuids",List.of(heroId.toString(),darkId.toString()),"scene_sha256",stateHash(hub)));finishServer(server);}return;}
      if(serverStage==0&&clientsDone(0)){owned(server,true);phase(server,11,0,"awaiting-owned-hero-observation");return;}
      if(serverStage==11){
        if(!GardenUnlocks081.isUnlocked(server,true)||access(hub)!=1)return;check(!GardenUnlocks081.isUnlocked(server,false),"Hero acquisition does not unlock Dark");checkPlan(hub,1);check(!HubContent.visitDarkGarden(host),"Dark native portal remains blocked after Hero-only unlock");serverProofs.add(Map.of("stage","independent_hero_unlock","mask",1,"hero_uuid",heroId.toString()));write("observer-connect.json",Map.of("port",PORT,"after_hero_unlocked",true));phase(server,1,1,"hero-only-late-join");return;
      }
      var observer=player(server,"observer");
      if(serverStage==1&&observer!=null&&!observer.level().dimension().equals(HubContent.HUB)){check(HubContent.enterHub(observer),"late observer enters actual existing lobby");safeBodyPose(observer,false);return;}
      if(serverStage==1&&clientsDone(1)){
        check(observer!=null&&!observer.getUUID().equals(host.getUUID()),"late peer has distinct actual network identity");boolean denied=false;try{denied=server.getCommands().getDispatcher().execute("chao gardens unlock all",observer.createCommandSourceStack().withPermission(0))<=0;}catch(com.mojang.brigadier.exceptions.CommandSyntaxException expected){denied=true;}check(denied&&!GardenUnlocks081.isUnlocked(server,false),"actual late peer permission0 cannot unlock gardens");serverProofs.add(Map.of("stage","actual_peer_admin_authorization","observer_uuid",observer.getUUID().toString(),"permission0_unlock_denied",true));conflictOriginal=hub.getBlockState(HubRooms.DARK_DOOR);check(conflictOriginal.isAir(),"closed Dark fixture cell is authored AIR");hub.setBlock(HubRooms.DARK_DOOR,Blocks.CHEST.defaultBlockState(),3);var chest=(ChestBlockEntity)hub.getBlockEntity(HubRooms.DARK_DOOR);check(chest!=null,"place deliberate protected test chest");chest.setItem(0,new ItemStack(Items.DIAMOND,7));chest.setChanged();conflictItemHash=chestItem(hub);conflictBeforeHash=stateHash(hub);owned(server,false);phase(server,12,1,"awaiting-owned-dark-conflict");return;
      }
      if(serverStage==12){
        if(!GardenUnlocks081.isUnlocked(server,false))return;var result=HubRooms.refreshUnlocks081(hub);check(!result.applied()&&result.conflicts().contains(HubRooms.DARK_DOOR),"guarded atomic patch reports protected custom cell");check(access(hub)==1&&stateHash(hub).equals(conflictBeforeHash)&&chestItem(hub).equals(conflictItemHash),"blocked patch preserves every critical block and chest item");check(!HubContent.visitDarkGarden(host)&&host.level()==hub,"logical unlock with pending geometry does not bypass native portal gate");serverProofs.add(Map.of("stage","protected_cell_atomic_abort","scene_sha256",conflictBeforeHash,"chest_item_sha256",conflictItemHash,"reported_conflicts",result.conflicts().stream().map(BlockPos::toShortString).toList()));phase(server,2,1,"dark-unlocked-protected-cell-blocks-layout");return;
      }
      if(serverStage==2&&clientsDone(2)){
        check(chestItem(hub).equals(conflictItemHash),"protected chest survives actual clients' observation");retainFixtureContents(server,hub);hub.setBlock(HubRooms.DARK_DOOR,conflictOriginal,3);check(command(server,"chao gardens rescan")>0,"permission2 native administrative rescan succeeds after manual restoration of this fixture-only cell");phase(server,13,1,"awaiting-safe-retry");return;
      }
      if(serverStage==13){if(access(hub)!=3)return;checkPlan(hub,3);serverProofs.add(Map.of("stage","guarded_retry_after_manual_fixture_cell_restore","mask",3,"scene_sha256",stateHash(hub)));phase(server,3,3,"both-unlocked");return;}
      if(serverStage==3&&clientsDone(3)){check(HubContent.visitHeroGarden(host),"unlocked native Hero portal transfers");phase(server,4,3,"portal-hero");return;}
      if(serverStage==4&&exists("host-portal-hero-seen.json")){check(host.level().dimension().equals(HubContent.HERO_GARDEN),"actual Hero destination");check(HubContent.returnToHub(host),"native Hero return succeeds");phase(server,5,3,"portal-hero-return");return;}
      if(serverStage==5&&exists("host-portal-hero-return-seen.json")){check(HubContent.visitDarkGarden(host),"unlocked native Dark portal transfers");phase(server,6,3,"portal-dark");return;}
      if(serverStage==6&&exists("host-portal-dark-seen.json")){check(host.level().dimension().equals(HubContent.DARK_GARDEN),"actual Dark destination");check(HubContent.returnToHub(host),"native Dark return succeeds");phase(server,7,3,"portal-dark-return");return;}
      if(serverStage==7&&exists("host-portal-dark-return-seen.json")){serverProofs.add(Map.of("stage","native_portal_transfers","client_confirmed_dimensions",List.of("hero_garden","hub","dark_garden","hub")));finishServer(server);}
    }catch(Throwable e){fatal=e.toString();write("server-failure.json",Map.of("stage",serverStage,"error",fatal,"proofs",List.copyOf(serverProofs)));LogUtils.getLogger().error("GARDEN081_SERVER_FAILURE",e);}
  }
  private static final class RenderRead081 {
    static final java.lang.reflect.Field MAIN,PORTALS;
    static {try{MAIN=LobbySceneRenderer025.class.getDeclaredField("mainSceneSubmitted");MAIN.setAccessible(true);PORTALS=LobbySceneRenderer025.class.getDeclaredField("renderedPortals");PORTALS.setAccessible(true);}catch(Exception e){throw new ExceptionInInitializerError(e);}}
  }
  private static Map<String,Object> lastRender=Map.of();
  @SubscribeEvent(priority=EventPriority.LOWEST) public static void rendered(RenderLevelStageEvent event){
    if(!enabled()||event.getStage()!=RenderLevelStageEvent.Stage.AFTER_LEVEL||clientStage<0||view>=VIEWS.length)return;var mc=Minecraft.getInstance();if(mc.level==null||!mc.level.dimension().equals(HubContent.HUB)||mc.screen!=null||mc.getOverlay()!=null)return;
    try{if(!RenderRead081.MAIN.getBoolean(null))return;var portals=(Set<?>)RenderRead081.PORTALS.get(null);var p=new LinkedHashMap<String,Object>();p.put("native_scene_submitted",true);p.put("native_rendered_portals",portals.stream().map(Object::toString).toList());p.put("hero_portal_submitted",portals.contains(HubRooms.HERO_DOOR));p.put("dark_portal_submitted",portals.contains(HubRooms.DARK_DOOR));p.put("native_main_anchor",mc.level.getBlockState(ANCHOR).toString());p.put("renderer",mc.getBlockEntityRenderDispatcher().getRenderer((LobbyDecor025.SceneEntity)mc.level.getBlockEntity(ANCHOR)).getClass().getName());lastRender=p;frames++;}catch(Exception e){fatal=e.toString();if(!exists(ROLE+"-failure.json"))write(ROLE+"-failure.json",Map.of("error",fatal,"stage",clientStage,"view",view,"source","actual production render proof"));LogUtils.getLogger().error("GARDEN081_RENDER_FAILURE",e);}
  }
  private static void view(Minecraft mc,int at){var v=VIEWS[at];double x=v[0],y=v[1],z=v[2];float yaw=(float)Math.toDegrees(Math.atan2(x-v[3],v[5]-z));float pitch=(float)-Math.toDegrees(Math.atan2(v[4]-y,Math.hypot(v[3]-x,v[5]-z)));if(camera==null||camera.level()!=mc.level)camera=new ArmorStand(mc.level,x,y,z);camera.moveTo(x,y-camera.getEyeHeight(),z,yaw,pitch);camera.xo=camera.getX();camera.yo=camera.getY();camera.zo=camera.getZ();camera.yRotO=yaw;camera.xRotO=pitch;camera.setYHeadRot(yaw);camera.yHeadRotO=yaw;mc.setCameraEntity(camera);}
  private static void capture(Minecraft mc,JsonObject p){
    int mask=p.get("mask").getAsInt();check(access(mc.level)==mask,"actual peer marker receives requested access mask");check(Boolean.TRUE.equals(lastRender.get("native_scene_submitted")),"actual production renderer submitted this view");check((mask&1)!=0||!Boolean.TRUE.equals(lastRender.get("hero_portal_submitted")),"locked Hero portal is never submitted");check((mask&2)!=0||!Boolean.TRUE.equals(lastRender.get("dark_portal_submitted")),"locked Dark portal is never submitted");if((mask&1)!=0&&(view==0||view==1))check(Boolean.TRUE.equals(lastRender.get("hero_portal_submitted")),"visible unlocked Hero portal uses actual production submission");if((mask&2)!=0&&(view==0||view>=2))check(Boolean.TRUE.equals(lastRender.get("dark_portal_submitted")),"visible unlocked Dark portal uses actual production submission");check(stateHash(mc.level).equals(p.get("actual_block_state_sha256").getAsString()),"all critical physical lobby blocks match authoritative snapshot on peer");
    int capturedView=view;String name="garden081-"+(reopen()?"reopen-":"")+ROLE+"-stage"+clientStage+"-"+VIEW_NAMES[view]+".png";var metadata=new LinkedHashMap<String,Object>();metadata.put("role",ROLE);metadata.put("stage",clientStage);metadata.put("label",p.get("label").getAsString());metadata.put("view",VIEW_NAMES[view]);metadata.put("camera",VIEWS[view]);metadata.put("access_mask",mask);metadata.put("peer_uuid",mc.player.getUUID().toString());metadata.put("critical_cell_count",clientCritical.size());metadata.put("actual_block_state_sha256",stateHash(mc.level));metadata.put("actual_render_proof",new LinkedHashMap<>(lastRender));metadata.put("production_rendered_frames",frames);metadata.put("source_freeze",freeze());screenshotPending=true;
    Screenshot.grab(mc.gameDirectory,name,mc.getMainRenderTarget(),message->{try{var image=mc.gameDirectory.toPath().resolve("screenshots").resolve(name);check(Files.isRegularFile(image),"actual native screenshot completed");metadata.put("png_path",image.toString());metadata.put("png_sha256",fileSha(image));write(ROLE+"-stage-"+clientStage+"-view-"+capturedView+".json",metadata);view++;frames=0;viewAt=System.nanoTime();screenshotPending=false;}catch(Throwable e){fatal=e.toString();}});
  }
  private static void clientStage(Minecraft mc,JsonObject p){
    int stage=p.get("stage").getAsInt();String label=p.get("label").getAsString();
    if(stage==99){if(!clientDone){mc.setCameraEntity(mc.player);write(ROLE+"-complete.json",Map.of("status","PASS","mode",reopen()?"actual_saved_world_reopen":"fresh_unlock_late_join_conflict_portals","source_freeze",freeze(),"peer_uuid",mc.player.getUUID().toString()));clientDone=true;}if(exists("host-complete.json")&&(reopen()||exists("observer-complete.json")))mc.stop();return;}
    if(stage>=4&&stage<=7){mc.setCameraEntity(mc.player);if(ROLE.equals("host")){var want=stage==4?HubContent.HERO_GARDEN:stage==6?HubContent.DARK_GARDEN:HubContent.HUB;if(mc.level.dimension().equals(want))write("host-"+label+"-seen.json",Map.of("actual_dimension",mc.level.dimension().location().toString(),"peer_uuid",mc.player.getUUID().toString()));}return;}
    if(!(stage==0||stage==1||stage==2||stage==3||stage==10)||!mc.level.dimension().equals(HubContent.HUB))return;
    if(clientStage!=stage){clientStage=stage;view=0;frames=0;viewAt=System.nanoTime();screenshotPending=false;lastRender=Map.of();clientCritical=new ArrayList<>();for(var q:p.getAsJsonArray("critical_positions")){var a=q.getAsJsonArray();clientCritical.add(new BlockPos(a.get(0).getAsInt(),a.get(1).getAsInt(),a.get(2).getAsInt()));}}
    if(view>=VIEWS.length){if(!exists(ROLE+"-stage-"+stage+"-done.json"))write(ROLE+"-stage-"+stage+"-done.json",Map.of("status","PASS","stage",stage,"views",VIEWS.length,"mask",p.get("mask").getAsInt(),"peer_uuid",mc.player.getUUID().toString()));return;}
    view(mc,view);if(mc.screen!=null||mc.getOverlay()!=null){frames=0;viewAt=System.nanoTime();return;}if(screenshotPending)return;
    // Permit ordinary chunk/block-state replication before asserting the view.
    if(frames<35||System.nanoTime()-viewAt<1_500_000_000L)return;
    if(access(mc.level)!=p.get("mask").getAsInt()||!stateHash(mc.level).equals(p.get("actual_block_state_sha256").getAsString())){check(System.nanoTime()-viewAt<30_000_000_000L,"peer physical state failed to synchronize");return;}capture(mc,p);
  }
  @SubscribeEvent public static void client(ClientTickEvent.Post event){
    if(!enabled())return;var mc=Minecraft.getInstance();localTicks++;
    try{
      check(localTicks<18000,"managed client tick timeout");check(fatal==null,"local/server error "+fatal);for(String peer:List.of("server-failure.json","host-failure.json","observer-failure.json")){var failure=read(peer);if(failure!=null)throw new IllegalStateException("Recorded peer failure at "+DIR.resolve(peer));}
      if(mc.screen instanceof AccessibilityOnboardingScreen)mc.setScreen(new TitleScreen());
      if(!started&&mc.screen instanceof TitleScreen){
        mc.options.renderDistance().set(6);mc.options.simulationDistance().set(6);mc.options.framerateLimit().set(40);mc.options.fov().set(70);mc.options.pauseOnLostFocus=false;mc.options.hideGui=true;gameDir=mc.gameDirectory.toPath();
        if(ROLE.equals("host")){
          check(!exists("host-ready.json")&&!exists("phase.json"),"fresh evidence output required");started=true;
          if(reopen()){check(REOPEN.matches("Garden081Remote-[0-9]+")&&!EXPECT.isBlank(),"reopen uses known prior fixture world and identity proof");worldName=REOPEN;write("pre-open-disk-proof.json",diskProof());mc.createWorldOpenFlows().openWorld(REOPEN,()->{throw new IllegalStateException("actual saved world could not open");});}
          else{worldName="Garden081Remote-"+System.currentTimeMillis();mc.createWorldOpenFlows().createFreshLevel(worldName,new LevelSettings(worldName,GameType.CREATIVE,false,Difficulty.PEACEFUL,true,new GameRules(),WorldDataConfiguration.DEFAULT),new WorldOptions(818181L,false,false),a->a.registryOrThrow(Registries.WORLD_PRESET).getHolderOrThrow(WorldPresets.FLAT).value().createWorldDimensions(),mc.screen);}
        }else if(exists("observer-connect.json")){started=true;ConnectScreen.startConnecting(mc.screen,mc,ServerAddress.parseString("127.0.0.1:"+PORT),new ServerData("Garden081 real LAN","127.0.0.1:"+PORT,ServerData.Type.LAN),false,null);}
      }
      if(ROLE.equals("host")&&!setup&&mc.player!=null&&mc.getSingleplayerServer()!=null){setup=true;mc.setScreen(null);var server=mc.getSingleplayerServer();server.execute(()->{try{
        initCritical();var host=player(server,"host");check(host!=null,"actual named host connected");if(!host.level().dimension().equals(HubContent.HUB))check(HubContent.enterHub(host),"native lobby entry succeeds");else check(HubContent.ensure(host.serverLevel()),"existing native lobby prepares safely");if(!reopen())safeBodyPose(host,true);
        if(!reopen()){check(server.publishServer(GameType.SURVIVAL,false,PORT),"actual LAN server publishes");published=true;}var level=server.getLevel(HubContent.HUB);level.setDayTime(6000);level.setWeatherParameters(0,100000,false,false);stageAt=server.overworld().getGameTime();serverReady=true;write("host-ready.json",Map.of("port",PORT,"world",worldName,"reopen",reopen(),"scope","Actual production lobby and real LAN peer; no fabricated scene."));
      }catch(Throwable e){fatal=e.toString();write("server-failure.json",Map.of("error",fatal));}});}
      if(mc.level!=null&&mc.player!=null&&!(mc.screen instanceof ReceivingLevelScreen)){var phase=read("phase.json");if(phase!=null)clientStage(mc,phase);}
    }catch(Throwable e){if(mc.player!=null)mc.setCameraEntity(mc.player);if(!exists(ROLE+"-failure.json"))write(ROLE+"-failure.json",Map.of("error",e.toString(),"stage",clientStage,"view",view,"render",lastRender));LogUtils.getLogger().error("GARDEN081_CLIENT_FAILURE",e);mc.stop();}
  }
}
