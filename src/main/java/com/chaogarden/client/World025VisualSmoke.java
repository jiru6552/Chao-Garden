package com.chaogarden.client;

import com.chaogarden.ChaoGarden;
import com.chaogarden.core.ChaoState;
import com.chaogarden.entity.ChaoEntity;
import com.chaogarden.entity.ChaoEggEntity;
import com.chaogarden.hub.HubContent;
import com.chaogarden.hub.HubRooms;
import com.chaogarden.item.ChaoCarrierItem;
import com.chaogarden.kindergarten.KindergartenContent;
import com.chaogarden.kindergarten.KindergartenMarket025;
import com.chaogarden.kindergarten.KindergartenNpc;
import com.chaogarden.kindergarten.KindergartenRoster;
import com.chaogarden.market.BlackMarketTrader;
import com.mojang.logging.LogUtils;
import com.google.gson.JsonParser;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.screens.BackupConfirmScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderFrameEvent;

/** Opt-in integrated scenery/service study. Excluded from installable JARs.
 * Every image waits for complete chunk receipt and an empty section render queue
 * for forty consecutive frames, then verifies its exact saved-file digest.
 * Test cleanup is restricted to actors/ledger rows bearing this study's tag. */
@EventBusSubscriber(modid=ChaoGarden.MODID,value=Dist.CLIENT)
public final class World025VisualSmoke {
  public static final String FIXTURE="ChaoWorld025VisualStudy";
  private record Scene(String key,String space,Vec3 eye,Vec3 target,int fov,String role,List<String> actions) {
    boolean ui(){return role.startsWith("UI:");}
    boolean staff(){return role.startsWith("STAFF:");}
    String service(){return role.contains(":")?role.substring(role.indexOf(':')+1):role;}
  }
  private static Vec3 v(double x,double y,double z){return new Vec3(x,y,z);}
  private static Scene world(String key,String space,Vec3 eye,Vec3 target,int fov){return new Scene(key,space,eye,target,fov,"",List.of());}
  private static Scene staff(String key,String role,Vec3 eye,Vec3 target){return new Scene(key,"school",eye,target,34,"STAFF:"+role,List.of());}
  private static Scene ui(String key,String role,Vec3 eye,Vec3 target,String... actions){return new Scene(key,"school",eye,target,62,"UI:"+role,List.of(actions));}
  private static final Scene[] SCENES={
    world("hub-wide","hub",v(-14,91,18),v(0,82,0),74),
    world("hub-glass-floor","hub",v(-13,82,16),v(0,76,0),72),
    world("hub-underfloor","hub",v(10,76.2,12),v(0,80,0),72),
    world("hub-transporter","hub",v(7,84,10),v(0,83,0),60),
    world("hub-kindergarten-gate","hub",v(0,83,-10),v(0,84,-23),64),
    world("garden-wide","garden",v(22,100,32),v(0,82,-5),76),
    world("garden-pond","garden",v(33,86,18),v(24,80,-4),66),
    world("garden-waterfall","garden",v(15,86,-12),v(26,91,-31),64),
    world("garden-cave","garden",v(-25,83.5,-15),v(-26.5,82,-32),64),
    world("garden-transporter","garden",v(-27,84.2,21),v(-32.5,82.9,15.5),48),
    world("garden-egg-release","garden",v(5,82,11),v(0,80.5,5),48),
    world("garden-toy-play","garden",v(5,82,11),v(1.5,80.5,7),48),
    world("school-facade","school",v(0,88,46),v(0,87,17),76),
    world("school-facade-oblique","school",v(29,91,42),v(0,87,17),76),
    world("school-foyer","school",v(0,82.2,8),v(0,81.8,-3),84),
    world("school-foyer-doors","school",v(3.5,82.2,3),v(-6,82,-1),78),
    world("school-classroom","school",v(-20,82.5,-10.5),v(-20.5,81,-18.5),72),
    world("school-clinic","school",v(17.3,82.2,-11.3),v(20.3,81,-16.5),72),
    world("school-principal","school",v(0,82,-17.2),v(.5,81,-21.5),70),
    world("school-fortune","school",v(19.4,82,8.5),v(22.5,81,5.5),68),
    world("school-market","school",v(-18,82,5.5),v(-23,81,5.5),64),
    world("school-bulletin","school",v(-2,82.5,8),v(-5,82.5,8),58),
    staff("staff-teacher-front","TEACHER",v(-17.5,81.05,-11.5),v(-17.5,80.45,-14.5)),
    staff("staff-teacher-oblique","TEACHER",v(-15.8,81.05,-11.8),v(-17.5,80.45,-14.5)),
    staff("staff-doctor-front","DOCTOR",v(18.5,81.45,-11.5),v(18.5,80.84,-14.5)),
    staff("staff-doctor-oblique","DOCTOR",v(20.2,81.45,-11.8),v(18.5,80.84,-14.5)),
    staff("staff-principal-front","PRINCIPAL",v(.5,81.55,-17.5),v(.5,80.94,-20.5)),
    staff("staff-principal-oblique","PRINCIPAL",v(2.1,81.55,-17.8),v(.5,80.94,-20.5)),
    staff("staff-fortune-front","FORTUNE",v(19.5,81.45,5.5),v(22.5,80.84,5.5)),
    staff("staff-fortune-oblique","FORTUNE",v(19.8,81.45,7.2),v(22.5,80.84,5.5)),
    world("staff-adult-baby-comparison","school",v(-17,81.25,-10),v(-17,80.5,-14.5),45),
    ui("service-teacher-empty","TEACHER",v(-17.5,81.8,-11.5),v(-17.5,80.8,-14.5)),
    ui("service-teacher-enrolled","TEACHER",v(-17.5,81.8,-11.5),v(-17.5,80.8,-14.5),"LEAVE CHAO","YES"),
    ui("service-doctor-menu","DOCTOR",v(18.5,82.2,-11.5),v(20,81,-14.5)),
    ui("service-doctor-stats","DOCTOR",v(18.5,82.2,-11.5),v(20,81,-14.5),"MEDICAL CHART"),
    ui("service-doctor-profile","DOCTOR",v(18.5,82.2,-11.5),v(20,81,-14.5),"MEDICAL CHART","PROFILE"),
    ui("service-doctor-records","DOCTOR",v(18.5,82.2,-11.5),v(20,81,-14.5),"MEDICAL CHART","RECORDS"),
    ui("service-fortune","FORTUNE",v(19.5,82.2,5.5),v(22.5,81.2,5.5)),
    ui("service-principal","PRINCIPAL",v(.5,82.3,-17.5),v(.5,81.3,-20.5),"TIPS"),
    ui("service-market-menu","MARKET",v(-18,81.8,5.5),v(-21.5,80.8,5.5)),
    ui("service-market-shopping","MARKET",v(-18,81.8,5.5),v(-21.5,80.8,5.5),"SHOPPING"),
    ui("service-market-sell","MARKET",v(-18,81.8,5.5),v(-21.5,80.8,5.5),"SELL ITEMS")
  };
  private static final AtomicInteger SAVES=new AtomicInteger();
  private static int tick,stage=-1,started,stable,captured,actionIndex,lastActionTick,interactTick,actionRetries;
  private static boolean shooting,stopping,opened,configuration,sceneryWarmed,careActivationRequested;
  private static volatile boolean careActivated;
  private static volatile int ready=-1;
  private static volatile RuntimeException failure;
  private static UUID observer,expectedChao;
  private static long startupWall;
  private static String startupScreen="";
  private static int comparisonId=-1;
  private World025VisualSmoke(){}
  /** Client bootstrap has an explicit hook; event delivery remains opt-in. */
  public static void register(){}

  @SubscribeEvent public static void tick(ClientTickEvent.Post event){
    if(!Boolean.getBoolean("chao_garden.world025Study")||stopping)return;
    var mc=Minecraft.getInstance();
    if(startupWall==0)startupWall=System.currentTimeMillis();
    if(mc.level==null||mc.player==null||mc.getSingleplayerServer()==null){
      if(mc.screen!=null&&!mc.screen.getClass().getName().equals(startupScreen)){
        startupScreen=mc.screen.getClass().getName();
        LogUtils.getLogger().info("CHAO_WORLD025_STARTUP screen={} title={} wallClockMillis={}",startupScreen,mc.screen.getTitle().getString(),System.currentTimeMillis());
      }
      // This flag is launched only against the disposable ChaoVisual save.
      // Use its real localized vanilla load/backup button, never an unrelated
      // screen action or automatic world creation/deletion.
      if(mc.level==null&&mc.screen instanceof BackupConfirmScreen&&new java.io.File(mc.gameDirectory,"saves/ChaoVisual/level.dat").isFile()){
        String backup=Component.translatable("selectWorld.backupJoinConfirmButton").getString();
        for(var child:mc.screen.children())if(child instanceof AbstractButton button&&button.getMessage().getString().equals(backup)){
          LogUtils.getLogger().info("CHAO_WORLD025_FIXTURE_BACKUP {}",button.getMessage().getString());button.onPress();break;
        }
      }
      if(System.currentTimeMillis()-startupWall>150000)throw new IllegalStateException("World025 fixture failed to load within 150 seconds; last screen="+startupScreen);
      return;
    }
    if(failure!=null)throw new IllegalStateException("World025 study failed",failure);
    if(!configuration){configuration=true;mc.options.renderDistance().set(12);mc.options.entityDistanceScaling().set(1.5);}
    tick++;if(tick==25){observer=mc.player.getUUID();advance(mc,Integer.getInteger("chao_garden.world025StartStage",0));}
    if(stage<0)return;
    if(tick-started>1800)throw new IllegalStateException("World025 bounded scene failure: "+scene().key()+" ready="+ready+" queue="+mc.levelRenderer.getSectionStatistics()+" screen="+(mc.screen==null?"none":mc.screen.getClass().getSimpleName())+" actions="+actionIndex);
    // Service views use normal production GUI/hand settings. Any scoped HUD
    // policy belongs to the actual Kindergarten UI, not a prettier fixture.
    mc.options.setCameraType(CameraType.FIRST_PERSON);mc.options.fov().set(scene().fov());mc.options.hideGui=!scene().ui();mc.getToasts().clear();
    if(!scene().ui())mc.setScreen(null);
    if(ready==stage){
      mc.getSingleplayerServer().execute(()->maintain(mc.getSingleplayerServer()));
      if(scene().ui()&&clientServiceReady(mc)){
        opened=true;
        if(scene().key().equals("service-teacher-enrolled")&&actionIndex==scene().actions().size()&&tick-lastActionTick>40&&!studentVisible(mc)){
          if(++actionRetries>3)throw new IllegalStateException("Teacher did not admit the selected study Chao after three current-session attempts");
          actionIndex=0;stable=0;sceneryWarmed=false;
          LogUtils.getLogger().info("CHAO_WORLD025_ACTION_RETRY scene={} retry={} selected={}",scene().key(),actionRetries,((KindergartenScreen)mc.screen).selectedKey());
        }
        if(actionIndex<scene().actions().size()&&tick-lastActionTick>=8){
          String wanted=scene().actions().get(actionIndex);
          for(var child:mc.screen.children())if(child instanceof AbstractButton button&&button.active&&button.visible&&button.getMessage().getString().equalsIgnoreCase(wanted)){
            button.onPress();actionIndex++;lastActionTick=tick;stable=0;sceneryWarmed=false;
            LogUtils.getLogger().info("CHAO_WORLD025_ACTION scene={} label={} actionIndex={}",scene().key(),wanted,actionIndex);break;
          }
        }
      }
    }
    if(shooting&&SAVES.get()==captured){
      if(stage+1==SCENES.length){stopping=true;ChaoRenderer.suppressNames=false;
        mc.getSingleplayerServer().execute(()->{
          cleanup(mc.getSingleplayerServer());
          LogUtils.getLogger().info("CHAO_WORLD025_COMPLETE {} captures / {} saves",captured,SAVES.get());mc.execute(mc::stop);
        });
      }else advance(mc,stage+1);
    }
  }
  private static Scene scene(){return SCENES[stage];}
  private static ServerPlayer player(MinecraftServer server){return server.getPlayerList().getPlayer(observer);}
  private static ServerLevel level(MinecraftServer server,String space){return server.getLevel(space.equals("hub")?HubContent.HUB:space.equals("school")?HubContent.KINDERGARTEN:HubContent.GARDEN);}
  private static void advance(Minecraft mc,int next){
    if(next<0||next>=SCENES.length)throw new IllegalArgumentException("Invalid World025 stage "+next);
    stage=next;ready=-1;started=tick;stable=0;shooting=false;opened=false;actionIndex=0;lastActionTick=tick;interactTick=0;actionRetries=0;
    sceneryWarmed=false;careActivationRequested=false;careActivated=false;
    ChaoRenderer.suppressNames=true;mc.setScreen(null);
    mc.getSingleplayerServer().execute(()->{try{prepare(mc.getSingleplayerServer(),next);ready=next;}catch(RuntimeException problem){failure=problem;LogUtils.getLogger().error("CHAO_WORLD025_PREPARE_FAILED {}",SCENES[next].key(),problem);}});
  }
  private static void look(ServerPlayer p,Vec3 target){
    Vec3 d=target.subtract(p.getEyePosition());float yaw=(float)(Math.atan2(d.z,d.x)*180/Math.PI)-90;
    float pitch=(float)(-Math.atan2(d.y,Math.hypot(d.x,d.z))*180/Math.PI);p.setYRot(yaw);p.setXRot(pitch);p.yHeadRot=p.yBodyRot=yaw;
  }
  private static void camera(ServerPlayer p,ServerLevel level,Scene scene){
    double y=scene.eye().y-p.getEyeHeight();
    if(p.serverLevel()!=level)p.teleportTo(level,scene.eye().x,y,scene.eye().z,0,0);
    p.teleportTo(scene.eye().x,y,scene.eye().z);p.setNoGravity(true);p.setDeltaMovement(Vec3.ZERO);p.stopFallFlying();p.fallDistance=0;look(p,scene.target());
    p.connection.teleport(p.getX(),p.getY(),p.getZ(),p.getYRot(),p.getXRot());
  }
  private static AABB bounds(){return scene().space().equals("hub")?HubRooms.HUB_BOUNDS:scene().space().equals("school")?HubRooms.KINDERGARTEN_BOUNDS:new AABB(-53,66,-51,54,119,48);}
  private static void inspectRuntimePreset(MinecraftServer server){
    var location=ResourceLocation.withDefaultNamespace("worldgen/world_preset/flat.json");
    try{
      var resource=server.getResourceManager().getResourceOrThrow(location);java.util.Set<String> keys;
      try(var reader=resource.openAsReader()){keys=new java.util.TreeSet<>(JsonParser.parseReader(reader).getAsJsonObject().getAsJsonObject("dimensions").keySet());}
      if(!keys.equals(java.util.Set.of("minecraft:overworld","minecraft:the_nether","minecraft:the_end")))throw new IllegalStateException("Development flat preset leaked into normal runtime from "+resource.sourcePackId()+": "+keys);
      if(server.getLevel(HubContent.HUB)==null||server.getLevel(HubContent.KINDERGARTEN)==null||server.getLevel(HubContent.GARDEN)==null)throw new IllegalStateException("Normal datapack Chao dimensions were not loaded");
      LogUtils.getLogger().info("CHAO_WORLD025_RUNTIME_PRESET resource={} source={} keys={} hubLoaded=true kindergartenLoaded=true gardenLoaded=true",location,resource.sourcePackId(),keys);
    }catch(java.io.IOException problem){throw new IllegalStateException("Cannot inspect the actual runtime flat preset",problem);}
  }
  private static void prepare(MinecraftServer server,int index){
    var p=player(server);if(p==null)throw new IllegalStateException("Missing study keeper");
    p.closeContainer();p.clearFire();p.setHealth(p.getMaxHealth());p.removeAllEffects();p.setNoGravity(true);
    if(index==0){
      inspectRuntimePreset(server);
      cleanup(server);
      // This is explicitly the ordinary keeper admission study. An operator
      // starting in Creative correctly retains that build mode in the garden,
      // so establish Survival before the production journal captures origin.
      if(HubContent.isHubDimension(p.level())&&!HubContent.returnHome(p))throw new IllegalStateException("Cannot reset only the study observer's prior visit");
      p.setGameMode(GameType.SURVIVAL);
      if(!HubContent.enterHub(p))throw new IllegalStateException("Actual hub admission refused");
    }
    if(scene().space().equals("garden")&&p.level().dimension().equals(HubContent.HUB)&&!HubContent.visitGarden(p))throw new IllegalStateException("Actual garden admission refused");
    if(scene().space().equals("school")&&p.level().dimension().equals(HubContent.GARDEN)&&!HubContent.returnToHub(p))throw new IllegalStateException("Actual garden return refused");
    if(scene().space().equals("school")&&p.level().dimension().equals(HubContent.HUB)&&!HubContent.visitKindergarten(p))throw new IllegalStateException("Actual school admission refused");
    if(scene().space().equals("garden")&&p.gameMode.getGameModeForPlayer()!=GameType.SURVIVAL)throw new IllegalStateException("Garden did not select protected Survival mode");
    var destination=level(server,scene().space());if(destination==null||!HubContent.ensure(destination))throw new IllegalStateException("Destination failed to build: "+scene().space());
    var b=bounds();for(int x=((int)b.minX)>>4;x<=(((int)b.maxX-1)>>4);x++)for(int z=((int)b.minZ)>>4;z<=(((int)b.maxZ-1)>>4);z++)destination.getChunk(x,z);
    // The comparison child has finished its one saved view. Leaving another
    // owned nearby Chao here would legitimately become the teacher's first
    // fallback selection after the held student is enrolled.
    if(scene().ui()&&comparisonId>=0){
      var comparison=server.getLevel(HubContent.KINDERGARTEN).getEntity(comparisonId);
      if(comparison instanceof ChaoEntity c&&c.getPersistentData().getBoolean(FIXTURE))c.discard();
      comparisonId=-1;
    }
    if(!scene().key().equals("service-teacher-enrolled")){cleanupFixtureStudents(server);if(scene().ui())prepareHeld(p,scene().service());}
    camera(p,destination,scene());
    if(scene().key().equals("staff-adult-baby-comparison")){
      var c=ChaoGarden.CHAO.get().create(destination);if(c==null)throw new IllegalStateException("Missing comparison child");
      var s=new ChaoState();s.hunger=90;s.energy=100;c.setState(s);c.owner=p.getUUID();c.getPersistentData().putBoolean(FIXTURE,true);c.setNoAi(true);c.moveTo(-16,80,-14.5,0,0);destination.addFreshEntity(c);comparisonId=c.getId();
    }
    if(scene().key().startsWith("garden-egg"))World025VisualSmokeCare.prepare(p,scene().key());
    LogUtils.getLogger().info("CHAO_WORLD025_PREPARED {} stage={} dimension={} eye={} target={} mode={}",scene().key(),index,p.level().dimension().location(),scene().eye(),scene().target(),p.gameMode.getGameModeForPlayer());
  }
  private static void prepareHeld(ServerPlayer p,String service){
    p.getInventory().clearContent();
    if(service.equals("MARKET")){p.getInventory().setItem(0,new ItemStack(net.minecraft.world.item.Items.EMERALD,64));p.getInventory().setItem(1,new ItemStack(ChaoGarden.item("swim_drive"),8));p.inventoryMenu.broadcastChanges();expectedChao=null;return;}
    var state=ChaoState.egg(0,false,true,0,new Random(25));state.adult=false;state.happiness=87;state.hunger=81;state.energy=94;
    state.reincarnations=2;state.ageTicks=3800;
    for(int i=0;i<5;i++){state.grades[i]=i+1;state.levels[i]=12+i*9;state.points[i]=400+i*173;}
    if(service.equals("DOCTOR")){state.setIllness(2,-100);state.setTraitInt("medal.karate_gold",1);state.setTraitInt("contest.karate.wins",3);}
    var c=ChaoGarden.CHAO.get().create(p.serverLevel());if(c==null)throw new IllegalStateException("No held study Chao");
    c.owner=p.getUUID();c.setState(state);c.setCustomName(Component.literal("Melody"));c.getPersistentData().putBoolean(FIXTURE,true);
    var held=new ItemStack(ChaoGarden.item("chao_carrier"));if(!ChaoCarrierItem.capture(held,c,p))throw new IllegalStateException("Actual study capture refused");
    expectedChao=ChaoCarrierItem.storedUUID(held);p.getInventory().setItem(0,held);p.getInventory().selected=0;p.inventoryMenu.broadcastChanges();
  }
  private static void maintain(MinecraftServer server){
    if(ready!=stage)return;var p=player(server);if(p==null)return;p.setNoGravity(true);p.setDeltaMovement(Vec3.ZERO);p.fallDistance=0;
    p.setPos(scene().eye().x,scene().eye().y-p.getEyeHeight(),scene().eye().z);look(p,scene().target());
    if(scene().key().equals("garden-toy-play")&&careActivated)World025VisualSmokeCare.diagnose(p);
    if(!scene().ui()||opened||p.tickCount-interactTick<20)return;
    interactTick=p.tickCount;KindergartenContent.ensureServices(p.serverLevel());
    if(scene().service().equals("MARKET")){
      var merchants=p.serverLevel().getEntitiesOfClass(BlackMarketTrader.class,new AABB(-25,79,1,-18,85,10),BlackMarketTrader::resident);
      if(merchants.size()==1)KindergartenMarket025.open(p,merchants.getFirst());
    }else{
      var role=KindergartenContent.Role.valueOf(scene().service());
      var npcs=p.serverLevel().getEntitiesOfClass(KindergartenNpc.class,new AABB(role.anchor).inflate(4),n->n.role()==role);
      if(npcs.size()==1)npcs.getFirst().mobInteract(p,InteractionHand.MAIN_HAND);
    }
  }
  private static boolean clientServiceReady(Minecraft mc){
    if(scene().service().equals("MARKET"))return mc.screen instanceof KindergartenScreen s&&s.serviceRole().equals("MARKET")
        ||scene().key().equals("service-market-shopping")&&mc.screen instanceof com.chaogarden.market.client.CweMarketScreen;
    return mc.screen instanceof KindergartenScreen screen&&screen.serviceRole().equals(scene().service())&&expectedChao!=null&&screen.selectedKey().endsWith(expectedChao.toString());
  }
  private static boolean studentVisible(Minecraft mc){return expectedChao!=null&&mc.level.getEntitiesOfClass(ChaoEntity.class,new AABB(-29,79,-28,-12,85,-4),c->c.getUUID().equals(expectedChao)&&c.state().traitInt("lesson.remaining",0)>0).size()==1;}
  private static boolean meshesReady(Minecraft mc){
    var expected=scene().space().equals("hub")?HubContent.HUB:scene().space().equals("school")?HubContent.KINDERGARTEN:HubContent.GARDEN;
    if(!mc.level.dimension().equals(expected))return false;var b=bounds();
    for(int x=((int)b.minX)>>4;x<=(((int)b.maxX-1)>>4);x++)for(int z=((int)b.minZ)>>4;z<=(((int)b.maxZ-1)>>4);z++)if(!mc.level.getChunkSource().hasChunk(x,z))return false;
    return mc.levelRenderer.hasRenderedAllSections();
  }
  @SubscribeEvent public static void frame(RenderFrameEvent.Post event){
    if(!Boolean.getBoolean("chao_garden.world025Study")||stopping||shooting||stage<0||ready!=stage)return;
    var mc=Minecraft.getInstance();if(mc.level==null||mc.player==null)return;
    if(tick-started<25||!meshesReady(mc)){
      if(!scene().key().equals("garden-toy-play")||!careActivationRequested){stable=0;sceneryWarmed=false;}
      return;
    }
    if(!sceneryWarmed){
      if(++stable<40)return;
      sceneryWarmed=true;
      LogUtils.getLogger().info("CHAO_WORLD025_SCENERY_STABLE scene={} stableFrames={} allBoundsChunksReceived=true renderQueueEmpty=true",scene().key(),stable);
    }
    // Animated physical toys update their block mesh every four server ticks.
    // Warm the whole unchanged scenery for forty frames FIRST, then admit the
    // actual placed toy and capture voluntary play when its current mesh queue
    // is empty. This does not freeze/forge the production play animation.
    if(scene().key().equals("garden-toy-play")&&!careActivationRequested){
      careActivationRequested=true;mc.getSingleplayerServer().execute(()->{try{World025VisualSmokeCare.prepare(player(mc.getSingleplayerServer()),scene().key());careActivated=true;}catch(RuntimeException problem){failure=problem;}});return;
    }
    if(scene().key().equals("garden-toy-play")&&!careActivated)return;
    if(scene().ui()){
      if(!clientServiceReady(mc)||actionIndex!=scene().actions().size()||tick-lastActionTick<12){stable=0;sceneryWarmed=false;return;}
      if(scene().key().equals("service-teacher-enrolled")&&!studentVisible(mc)){stable=0;sceneryWarmed=false;return;}
      if(mc.screen instanceof KindergartenScreen s){
        String expected=scene().key().startsWith("service-doctor-")&&!scene().key().equals("service-doctor-menu")?"chart":scene().key().equals("service-principal")?"topics":scene().key().equals("service-market-sell")?"sell":"home";
        if(!s.viewMode().equals(expected)){stable=0;sceneryWarmed=false;return;}
        int chart=scene().key().endsWith("-profile")?1:scene().key().endsWith("-records")?2:0;
        if(expected.equals("chart")&&s.chartPage()!=chart){stable=0;sceneryWarmed=false;return;}
      }else if(mc.screen instanceof com.chaogarden.market.client.CweMarketScreen s&&s.getMenu().getOffers().isEmpty()){stable=0;sceneryWarmed=false;return;}
    }else if(mc.screen!=null){stable=0;sceneryWarmed=false;return;}
    if(scene().key().startsWith("garden-egg")||scene().key().startsWith("garden-toy"))if(!World025VisualSmokeCare.clientReady(mc,scene().key())){if(!scene().key().equals("garden-toy-play")){stable=0;sceneryWarmed=false;}return;}
    if(scene().staff()){
      var role=KindergartenContent.Role.valueOf(scene().service());var npc=mc.level.getEntitiesOfClass(KindergartenNpc.class,new AABB(role.anchor).inflate(4),n->n.role()==role).stream().findFirst().orElse(null);
      if(npc==null||!npc.state().adult){stable=0;sceneryWarmed=false;return;}
      var renderer=mc.getEntityRenderDispatcher().getRenderer(npc);
      if(!(renderer instanceof StaffChaoRenderer025 staff)||!staff.getModel().uprightAdultPose())throw new IllegalStateException("Staff is not rendering with its upright adult profession mesh: "+role);
      LogUtils.getLogger().info("CHAO_WORLD025_STAFF scene={} role={} adult=true entityYaw={} bodyYaw={} headYaw={} uprightAdultPose=true",scene().key(),role,npc.getYRot(),npc.yBodyRot,npc.yHeadRot);
    }
    if(mc.getWindow().getWidth()!=1280||mc.getWindow().getHeight()!=720)throw new IllegalStateException("World025 framebuffer must be1280x720");
    final String file="chao025-"+scene().key()+".png";final int capturedStage=stage;
    LogUtils.getLogger().info("CHAO_WORLD025_CAPTURE {} stage={} dimension={} mode={} ui={} stableFrames={} facts={}",file,stage,mc.level.dimension().location(),mc.gameMode.getPlayerMode(),mc.screen==null?"none":mc.screen.getClass().getSimpleName(),stable,
      "allBoundsChunksReceived=true renderQueueEmpty=true "+mc.levelRenderer.getSectionStatistics()+" hideGui="+mc.options.hideGui+" fov="+mc.options.fov().get()+" selected="+(mc.screen instanceof KindergartenScreen s?s.selectedKey()+" view="+s.viewMode()+" chart="+s.chartPage():"none")+" classroomStudent="+studentVisible(mc));
    Screenshot.grab(mc.gameDirectory,file,mc.getMainRenderTarget(),message->{try{
      if(!(message.getContents() instanceof net.minecraft.network.chat.contents.TranslatableContents contents)||!contents.getKey().equals("screenshot.success"))throw new IllegalStateException("Screenshot failed: "+message.getString());
      var path=new java.io.File(new java.io.File(mc.gameDirectory,"screenshots"),file).toPath();var image=javax.imageio.ImageIO.read(path.toFile());
      if(image==null||image.getWidth()!=1280||image.getHeight()!=720)throw new IllegalStateException("Invalid actual saved framebuffer "+file);
      String sha=java.util.HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256").digest(java.nio.file.Files.readAllBytes(path)));
      LogUtils.getLogger().info("CHAO_WORLD025_SAVED {} stage={} sha256={}",file,capturedStage,sha);SAVES.incrementAndGet();
    }catch(Exception problem){failure=new IllegalStateException("Cannot verify actual saved frame "+file,problem);}});
    captured++;shooting=true;
  }
  private static boolean tagged(net.minecraft.nbt.CompoundTag data){return data.getCompound("NeoForgeData").getBoolean(FIXTURE)||data.getCompound("ForgeData").getBoolean(FIXTURE);}
  private static void cleanupFixtureStudents(MinecraftServer server){
    var roster=KindergartenRoster.get(server);
    for(var entry:List.copyOf(roster.students.values()))if(tagged(entry.entity)){
      var body=com.chaogarden.companion.CompanionContent.findEntity(server,entry.id);if(body!=null&&body.getPersistentData().getBoolean(FIXTURE))body.discard();roster.students.remove(entry.id);roster.setDirty();
    }
  }
  private static void cleanup(MinecraftServer server){
    cleanupFixtureStudents(server);var discard=new ArrayList<net.minecraft.world.entity.Entity>();
    for(var level:server.getAllLevels())for(var e:level.getAllEntities())if((e instanceof ChaoEntity||e instanceof ChaoEggEntity)&&e.getPersistentData().getBoolean(FIXTURE))discard.add(e);
    discard.forEach(net.minecraft.world.entity.Entity::discard);World025VisualSmokeCare.cleanup(server);
    LogUtils.getLogger().info("CHAO_WORLD025_CLEANUP ownTaggedActors={} unrelatedActorsPreserved=true",discard.size());
  }
}
