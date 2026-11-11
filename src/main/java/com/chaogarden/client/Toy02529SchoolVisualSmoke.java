package com.chaogarden.client;
import com.chaogarden.*;
import com.chaogarden.entity.ChaoEntity;
import com.chaogarden.care.ChaoCareServices;
import com.chaogarden.kindergarten.*;
import com.chaogarden.hub.HubContent;
import com.chaogarden.test.InstrumentSchoolFixture02525;
import com.chaogarden.toys.InstrumentPerformance;
import com.mojang.logging.LogUtils;
import java.nio.file.*;
import java.util.*;
import net.minecraft.client.*;
import net.minecraft.client.gui.screens.*;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Difficulty;
import net.minecraft.world.level.*;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.*;
import net.neoforged.neoforge.client.event.sound.PlaySoundEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
/** Real teacher enrollment and ambient classroom goals; only course/lesson clock shortened. Never packaged. */
@EventBusSubscriber(modid=ChaoGarden.MODID,value=Dist.CLIENT)
public final class Toy02529SchoolVisualSmoke {
  private static net.minecraft.world.entity.decoration.ArmorStand freeCamera;
  private static boolean started,setup;private static volatile int phase=-1;private static volatile long elapsed;
  private static long began;private static int ticks,frames,renders,sounds;private static final List<ChaoEntity> students=new ArrayList<>();
  private static final Set<String> seen=new HashSet<>();private static final Set<Integer> cameraViews=new HashSet<>();private static boolean paused,resumed;
  private static boolean enabled(){return Boolean.getBoolean("chao_garden.toy02529School");}
  private static void check(boolean b,String s){if(!b)throw new IllegalStateException("TOY02529_SCHOOL "+s);}
  private static void begin(net.minecraft.server.MinecraftServer server,int index){
    phase=index;paused=false;resumed=false;students.clear();var keeper=server.getPlayerList().getPlayers().getFirst();var level=server.getLevel(KindergartenContent.DIMENSION);
    String lesson=index==0?"cymbals":"tambourine";int period=Math.max(20,GardenConfig.LESSON_OFFER_TICKS.get());long next=(level.getGameTime()/period+1)*period;int attempts=0;while(!ChaoCareServices.currentLesson(next).equals(lesson)){check(++attempts<100,"lesson exists in the actual offered cycle");next+=period;}
    server.getWorldData().overworldData().setGameTime(next);check(ChaoCareServices.currentLesson(level.getGameTime()).equals(lesson),"actual offered class selected");InstrumentSchoolFixture02525.visit(keeper);keeper.setGameMode(GameType.CREATIVE);keeper.getInventory().clearContent();
    for(int i=0;i<2;i++){var c=InstrumentSchoolFixture02525.enroll(keeper,i,lesson,i==1,335);c.setCustomName(Component.literal("School "+lesson+" "+(i==1?"adult":"baby")));students.add(c);}
    keeper.setGameMode(GameType.SPECTATOR);began=level.getGameTime();elapsed=0;
    LogUtils.getLogger().info("TOY02529_SCHOOL_BEGIN {} teacherEnrollment=true schedule={}",lesson,ChaoCareServices.currentLesson(level.getGameTime()));
  }
  @SubscribeEvent public static void server(ServerTickEvent.Post e){
    if(!enabled()||!setup||phase>=2)return;var server=e.getServer();var level=server.getLevel(KindergartenContent.DIMENSION);
    if(phase<0){KindergartenContent.ensureServices(level);if(level.isPositionEntityTicking(KindergartenContent.Role.TEACHER.anchor))begin(server,0);return;}
    elapsed=level.getGameTime()-began;var c=students.getFirst();
    if(elapsed>=70&&!paused){paused=true;c.state().hunger=10;ChaoCareServices.tickLearning(c);check(!InstrumentPerformance.active(c,"chao_"+(phase==0?"cymbals":"tambourine")),"hunger pause removes props");LogUtils.getLogger().info("TOY02529_SCHOOL_PAUSE");}
    if(elapsed>=95&&!resumed){resumed=true;c.state().hunger=83;LogUtils.getLogger().info("TOY02529_SCHOOL_RESUME");}
    if(elapsed>=490){
      var keeper=server.getPlayerList().getPlayers().getFirst();InstrumentSchoolFixture02525.visit(keeper);keeper.setGameMode(GameType.CREATIVE);
      for(var body:students){check(!ChaoCareServices.hasLesson(body),"course finished");check(body.state().traitInt("learned."+(phase==0?"cymbals":"tambourine"),0)==1,"learned exactly once");check(KindergartenContent.retrieve(keeper,body.getUUID()),"teacher returns student");}
      LogUtils.getLogger().info("TOY02529_SCHOOL_PHASE_PASS {}",phase);if(phase==0)begin(server,1);else phase=2;
    }
  }
  @SubscribeEvent public static void render(RenderLivingEvent.Post<?,?> e){
    if(!enabled()||!(e.getEntity() instanceof ChaoEntity c))return;String kind=InstrumentPerformance.kind(c);
    if(!kind.isEmpty()&&InstrumentPerformance.active(c,kind)){
      var model=(ChaoModel)e.getRenderer().getModel();check(model.officialPosesFinite()&&model.toyPalm(false)!=null,"finite current renderer and right hand");
      if(kind.equals("chao_cymbals")||kind.equals("chao_tambourine"))check(model.toyPalm(true)!=null,"paired classroom cymbals hands");renders++;
      String key=(c instanceof KindergartenPupil0257?"ambient":"student-"+c.state().adult)+"-"+kind;seen.add(key);
    }
  }
  @SubscribeEvent public static void sound(PlaySoundEvent e){if(enabled()&&e.getSound()!=null&&(e.getSound().getLocation().equals(ChaoSounds.CYMBALS.getId())||e.getSound().getLocation().equals(ChaoSounds.TAMBOURINE.getId())))sounds++;}
  @SubscribeEvent public static void client(ClientTickEvent.Post e){
    if(!enabled())return;var mc=Minecraft.getInstance();ticks++;
    if(mc.screen instanceof AccessibilityOnboardingScreen)mc.setScreen(new TitleScreen());
    if(!started&&mc.screen instanceof TitleScreen){started=true;mc.options.renderDistance().set(4);mc.options.simulationDistance().set(5);mc.options.framerateLimit().set(30);mc.options.fov().set(42);mc.options.pauseOnLostFocus=false;mc.options.hideGui=true;
      mc.createWorldOpenFlows().createFreshLevel("Toy02529School-"+System.currentTimeMillis(),new LevelSettings("Toy02529School",GameType.CREATIVE,false,Difficulty.PEACEFUL,true,new GameRules(),WorldDataConfiguration.DEFAULT),new net.minecraft.world.level.levelgen.WorldOptions(2525L,false,false),a->a.registryOrThrow(net.minecraft.core.registries.Registries.WORLD_PRESET).getHolderOrThrow(net.minecraft.world.level.levelgen.presets.WorldPresets.FLAT).value().createWorldDimensions(),mc.screen);
    }
    if(!setup&&mc.player!=null&&mc.getSingleplayerServer()!=null){mc.setScreen(null);ChaoRenderer.suppressNames=true;mc.getSingleplayerServer().execute(()->{if(setup)return;var server=mc.getSingleplayerServer();var school=server.getLevel(KindergartenContent.DIMENSION);check(HubContent.ensure(school),"actual school builds");InstrumentSchoolFixture02525.visit(server.getPlayerList().getPlayers().getFirst());setup=true;});}
    if(mc.player!=null&&mc.level!=null&&phase>=0&&phase<2){
      int camera=elapsed<160?0:elapsed<320?1:2;ChaoEntity subject=null;
      for(var entity:mc.level.entitiesForRendering())if(entity instanceof ChaoEntity c){
        if(camera<2&&c.hasCustomName()&&c.getName().getString().startsWith("School ")&&c.state().adult==(camera==1)){subject=c;break;}
        if(camera==2&&c instanceof KindergartenPupil0257 p&&p.getUUID().equals(KindergartenPupils0257.identity(2))){subject=c;break;}
      }
      if(subject!=null){cameraViews.add(phase*3+camera);var f=net.minecraft.world.phys.Vec3.directionFromRotation(0,subject.getYRot());var at=subject.position().add(f.scale(2.4)).add(-f.z*.65,0,f.x*.65);float yaw=(float)(Math.toDegrees(Math.atan2(subject.getZ()-at.z,subject.getX()-at.x))-90);float pitch=12;if(freeCamera==null||freeCamera.level()!=mc.level)freeCamera=new net.minecraft.world.entity.decoration.ArmorStand(mc.level,at.x,subject.getY(),at.z);freeCamera.moveTo(at.x,subject.getY()+.85-freeCamera.getEyeHeight(),at.z,yaw,pitch);freeCamera.xo=freeCamera.getX();freeCamera.yo=freeCamera.getY();freeCamera.zo=freeCamera.getZ();freeCamera.yRotO=yaw;freeCamera.xRotO=pitch;freeCamera.setYHeadRot(yaw);freeCamera.yHeadRotO=yaw;freeCamera.setYBodyRot(yaw);freeCamera.yBodyRotO=yaw;mc.setCameraEntity(freeCamera);}
      if(ticks%2==0)Screenshot.grab(mc.gameDirectory,String.format("02529-school-%d-%04d-t%03d.png",phase,frames++,elapsed),mc.getMainRenderTarget(),x->{});
    }
    if(phase==2){check(cameraViews.size()==6,"all six closeup targets selected");check(seen.size()==6&&renders>100&&sounds>20,"all six student/ambient instrument presentations rendered: "+seen);
      try{var dir=Path.of("/tmp/chao-cymbals/school");Files.createDirectories(dir);Files.writeString(dir.resolve("pass.txt"),"actual teacher enrollment; baby/adult cymbals and tambourine; ambient pupils; two cycles; hunger pause/resume; retrieval; frames="+frames+" renders="+renders+" soundEvents="+sounds+" seen="+seen+"; listening not performed\n");}catch(Exception ex){throw new RuntimeException(ex);}LogUtils.getLogger().info("TOY02529_SCHOOL_PASS frames={} renders={} sounds={}",frames,renders,sounds);mc.stop();}
    check(ticks<18000,"client timeout");
  }
}
