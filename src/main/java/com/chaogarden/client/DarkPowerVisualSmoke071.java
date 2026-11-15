package com.chaogarden.client;

import com.chaogarden.ChaoGarden;
import com.chaogarden.GardenConfig;
import com.chaogarden.companion.AccessoriesBridge;
import com.chaogarden.companion.CompanionContent;
import com.chaogarden.core.ChaoState;
import com.chaogarden.core.AnalyzerStatRules;
import com.chaogarden.core.HeroRunMorphology052;
import com.chaogarden.core.NeutralMorphology056;
import com.chaogarden.core.HeroNormalMorphology057;
import com.chaogarden.core.NeutralRunMorphology062;
import com.chaogarden.core.DarkPowerMorphology071;
import com.chaogarden.core.DarkRunMorphology070;
import com.chaogarden.core.DarkFlyMorphology069;
import com.chaogarden.core.DarkSwimMorphology068;
import com.chaogarden.core.HeroPowerMorphology067;
import com.chaogarden.core.HeroRunMorphology066;
import com.chaogarden.core.HeroFlyMorphology065;
import com.chaogarden.core.HeroSwimMorphology064;
import com.chaogarden.core.NeutralPowerMorphology063;
import com.chaogarden.core.NeutralFlyMorphology061;
import com.chaogarden.core.NeutralSwimMorphology059;
import com.chaogarden.core.DarkNormalMorphology060;
import com.chaogarden.entity.ChaoEntity;
import com.chaogarden.item.ChaoCarrierItem;
import com.chaogarden.toys.GardenToyPerformance049;
import com.chaogarden.toys.ActiveToyPlayRules049;
import com.chaogarden.toys.QuietToyPlayRules049;
import com.chaogarden.toys.ToyPreview;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.logging.LogUtils;
import java.nio.file.Files;
import java.security.MessageDigest;
import com.chaogarden.core.GradualEvolutionRules;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import net.minecraft.client.*;
import net.minecraft.client.gui.screens.AccessibilityOnboardingScreen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.Difficulty;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
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
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.minecraft.client.gui.components.AbstractButton;
import com.chaogarden.care.ChaoCareNetwork;
import org.joml.Vector3f;

/** Opt-in blocky production morphology, native movement/toys and
 * actual Accessories-slot captures. Fixture training is deliberately accelerated;
 * playback retains the normal 20 TPS clock. No morphology rig is injected.
 * This entire helper and its nested classes are excluded from the production JAR. */
@EventBusSubscriber(modid=ChaoGarden.MODID,value=Dist.CLIENT)
public final class DarkPowerVisualSmoke071 {
  /** Axes and locked first type are explicit fixture data, independent of names/actions. */
  private record Case(String name,boolean adult,int percent,float alignment,float maturity,String action,
                      String hat,boolean rabbit,String coat,int firstType,int runAxis,int flyAxis,int counterPercent) {
    int runStat(){return runAxis<0?ChaoState.RUN:ChaoState.POWER;}
    int flyStat(){return flyAxis<0?ChaoState.SWIM:ChaoState.FLY;}
    // Equal current Run/Power training unlocks the ordinary upright idle
    // for matched adult photos without changing net morphology or injecting poses.
    int mobilityBaseline(){return adult&&photo()?1200:walk()?action.equals("run_walk")?4000:1200:0;}
    boolean photo(){return action.startsWith("still");}
    boolean still(){return photo()||ui();}
    boolean ui(){return action.startsWith("ui_");}
    boolean carry(){return action.startsWith("carry");}
    boolean pose(){return action.startsWith("pose_");}
    String poseId(){return pose()?action.substring(5):"";}
    boolean floorToy(){return action.equals("toy_car")||action.equals("crayons");}
    boolean toy(){return action.startsWith("broom")||action.startsWith("picture_book")||floorToy();}
    String toyId(){return floorToy()?action:action.startsWith("broom")?"broom":"picture_book";}
    String toyKind(){return "chao_"+toyId();}
    boolean swim(){return action.startsWith("water_swim");}
    boolean phoenix(){return action.contains("wings_glasses")||action.equals("water_swim_wings");}
    boolean walk(){return action.contains("walk");}
    boolean gain(){return action.equals("gain_walk")||action.equals("pair_walk");}
    boolean loss(){return action.equals("loss_walk");}
    boolean pair(){return action.equals("pair_walk");}
    boolean opposite(){return action.equals("opposite_walk");}
    String oppositionAxis(){return !opposite()?"":runAxis!=0?"runPower":"swimFly";}
    int oppositionInitial(){return !opposite()?0:runAxis!=0?runAxis:flyAxis;}
    int[] trainedStats(){
      if(runAxis!=0&&flyAxis!=0)return new int[]{runStat(),flyStat()};
      if(runAxis!=0)return new int[]{runStat()};
      if(flyAxis!=0)return new int[]{flyStat()};
      return new int[0];
    }
    int angleCount(){return photo()?6:3;}
    int duration(){return pose()?400:pair()?640:opposite()?560:toy()?180:carry()?240:action.equals("evolution")?300:gain()||loss()?280:240;}
  }
  private static Case scene(String name,String form,boolean adult,int firstType,float alignment,float maturity,
                            int percent,String action,String hat,boolean rabbit,String coat,int counterPercent){
    int rp=switch(form){case "run","run_swim","run_fly"->-1;case "power","power_swim","power_fly"->1;default->0;};
    int sf=switch(form){case "swim","run_swim","power_swim"->-1;case "fly","run_fly","power_fly"->1;default->0;};
    return new Case(name,adult,percent,alignment,maturity,action,hat,rabbit,coat,firstType,rp,sf,counterPercent);
  }
  private static Case darkPowerScene(String name,String form,float maturity,int percent,String action){
    return scene(name,form,true,ChaoState.POWER_TYPE,-1,maturity,percent,action,"",false,"natural",0);
  }
  private static List<Case> cases(){
    var result=new ArrayList<Case>();
    // 0..26: nine full-signed mature targets, independently gated by age.
    for(String form:new String[]{"normal","run","power","fly","swim","run_fly","run_swim","power_fly","power_swim"})
      for(float age:new float[]{0,.5f,1})result.add(darkPowerScene("dark_power_"+form+"_age"+Math.round(age*100),form,age,100,"still"));
    // 27..50: independent current-training quarters at full biological maturity.
    for(String form:new String[]{"run","power","fly","swim","run_fly","run_swim","power_fly","power_swim"})for(int percent:new int[]{25,50,75})
      result.add(darkPowerScene("dark_power_"+form+"_influence"+percent,form,1,percent,"still"));
    // 51..54: opposing Swim cancels Fly, then continues through balance into Swim.
    for(int counter:new int[]{50,100,150,200})result.add(scene("dark_power_swim_counter"+counter,"fly",true,ChaoState.POWER_TYPE,-1,1,100,"still","",false,"natural",counter));
    // 55..59: inherited pigment/finish with the same underlying morph sampler.
    result.add(scene("dark_power_run_red_twotone","run",true,ChaoState.POWER_TYPE,-1,1,100,"still","",false,"red_twotone",0));
    result.add(scene("dark_power_power_red_shiny","power",true,ChaoState.POWER_TYPE,-1,1,100,"still","",false,"red_shiny",0));
    result.add(scene("dark_power_swim_green_mono","swim",true,ChaoState.POWER_TYPE,-1,1,100,"still","",false,"green_mono",0));
    result.add(scene("dark_power_run_fly_ruby","run_fly",true,ChaoState.POWER_TYPE,-1,1,100,"still","",false,"ruby",0));
    result.add(scene("dark_power_power_swim_natural_shiny","power_swim",true,ChaoState.POWER_TYPE,-1,1,100,"still","",false,"natural_shiny",0));
    // 60..65: completed Hero/Fly065 and five other retained adult families; further regressions follow below.
    result.add(scene("hero_fly_run_fly_mature_regression","run_fly",true,ChaoState.FLY_TYPE,1,1,100,"still","",false,"natural",0));
    result.add(scene("hero_normal_power_fly_regression","power_fly",true,ChaoState.NORMAL,1,1,100,"still","",false,"natural",0));
    result.add(scene("neutral_normal_power_swim_regression","power_swim",true,ChaoState.NORMAL,0,1,100,"still","",false,"natural",0));
    result.add(scene("neutral_swim_run_swim_regression","run_swim",true,ChaoState.SWIM_TYPE,0,1,100,"still","",false,"natural",0));
    result.add(scene("dark_normal_run_fly_regression","run_fly",true,ChaoState.NORMAL,-1,1,100,"still","",false,"natural",0));
    result.add(scene("neutral_fly_power_fly_regression","power_fly",true,ChaoState.FLY_TYPE,0,1,100,"still","",false,"natural",0));
    // 66..68: actual hats, replacement animal parts and glasses.
    result.add(scene("dark_power_power_pumpkin","power",true,ChaoState.POWER_TYPE,-1,1,100,"still","pumpkin",false,"natural",0));
    result.add(scene("dark_power_run_fly_rabbit","run_fly",true,ChaoState.POWER_TYPE,-1,1,100,"still","",true,"natural",0));
    result.add(scene("dark_power_fly_phoenix_glasses","fly",true,ChaoState.POWER_TYPE,-1,1,100,"still_wings_glasses","",false,"natural",0));
    // 69..71: native mouse handlers, forged edit authorization, sync and entity NBT.
    result.add(scene("creative_dark_power_adult_lock_maturity","run_fly",true,ChaoState.POWER_TYPE,-1,.8f,100,"ui_adult","",false,"natural",0));
    result.add(scene("survival_dark_power_adult_readonly","power_swim",true,ChaoState.POWER_TYPE,-1,.8f,100,"ui_survival","",false,"natural",0));
    result.add(scene("creative_baby_alignment_maturity_regression","run",false,ChaoState.RUN_TYPE,0,.8f,100,"ui_creative","",false,"natural",0));
    // 72..77: actual movement, water, production toy grips and equipped carrying.
    result.add(darkPowerScene("dark_power_balanced_walk","normal",1,0,"balanced_walk"));
    result.add(darkPowerScene("dark_power_run_running","run",1,100,"run_walk"));
    result.add(darkPowerScene("dark_power_run_swim_water_motion","run_swim",1,100,"water_swim"));
    result.add(darkPowerScene("dark_power_run_fly_broom","run_fly",1,100,"broom"));
    result.add(darkPowerScene("dark_power_power_swim_book","power_swim",1,100,"picture_book"));
    result.add(darkPowerScene("dark_power_run_fly_equipped_flying","run_fly",1,100,"carry_flying"));
    // 78..82: live gain/loss, held pair reversal and Power-to-Run crossing;99 covers Fly-to-Swim.
    result.add(darkPowerScene("dark_power_run_gain_walk","run",1,0,"gain_walk"));
    result.add(darkPowerScene("dark_power_power_fly_gain_walk","power_fly",1,0,"gain_walk"));
    result.add(darkPowerScene("dark_power_fly_loss_walk","fly",1,100,"loss_walk"));
    result.add(darkPowerScene("dark_power_run_fly_pair_gain_hold_reverse_walk","run_fly",1,0,"pair_walk"));
    result.add(darkPowerScene("dark_power_power_to_run_opposition_walk","power",1,100,"opposite_walk"));
    // 83: genuine Power-trained first evolution into the fresh Dark Power adult.
    result.add(scene("dark_power_actual_first_evolution","power",false,ChaoState.POWER_TYPE,-1,.99f,100,"evolution","",false,"natural",0));
    // 84..91: quarter/three-quarter age with half training in all four compatible corners.
    for(String form:new String[]{"run_fly","run_swim","power_fly","power_swim"})for(float age:new float[]{.25f,.75f})
      result.add(darkPowerScene("dark_power_"+form+"_age"+Math.round(age*100)+"_influence50",form,age,50,"still"));
    // 92: newest completed family remains a genuine Neutral / Run-first regression.
    result.add(scene("neutral_run_run_mature_regression","run",true,ChaoState.RUN_TYPE,0,1,100,"still","",false,"natural",0));
    // 93: newest completed Neutral / Power-first family stays on its genuine .63 renderer.
    result.add(scene("neutral_power_power_mature_regression","power",true,ChaoState.POWER_TYPE,0,1,100,"still","",false,"natural",0));
    // 94: previously completed Hero / Swim-first family keeps its exact .64 route.
    result.add(scene("hero_swim_run_swim_mature_regression","run_swim",true,ChaoState.SWIM_TYPE,1,1,100,"still","",false,"natural",0));
    // 95..96: Fly-specialized Dark Power in actual water, then real Phoenix replacement wings.
    result.add(darkPowerScene("dark_power_fly_native_fan_water_motion","fly",1,100,"water_swim"));
    result.add(darkPowerScene("dark_power_fly_phoenix_replacement_water_motion","fly",1,100,"water_swim_wings"));
    // 97..98: the approved original055 baby construction remains its own route.
    result.add(scene("baby_normal055_regression","normal",false,ChaoState.NORMAL,0,.8f,0,"still","",false,"natural",0));
    result.add(scene("baby_hero_run055_regression","run",false,ChaoState.RUN_TYPE,1,.99f,100,"still","",false,"natural",0));
    // 99: independently reverse Fly into Swim while first Power remains locked.
    result.add(darkPowerScene("dark_power_fly_to_swim_opposition_walk","fly",1,100,"opposite_walk"));
    // 100: explicit neutral-aligned max-Run baby witness for both brow nubs.
    result.add(scene("baby_neutral_run055_regression","run",false,ChaoState.RUN_TYPE,0,.99f,100,"still","",false,"natural",0));
    // 101: completed Hero Run066 remains a genuine locked first Run3.
    result.add(scene("hero_run_run_fly_mature_regression","run_fly",true,ChaoState.RUN_TYPE,1,1,100,"still","",false,"natural",0));
    // 102: immediately preceding Hero Power067 stays locked first Power4 / Hero+1.
    result.add(scene("hero_power_power_fly_mature_regression","power_fly",true,ChaoState.POWER_TYPE,1,1,100,"still","",false,"natural",0));
    // 103..108: actual operator adapters use the same production motion and temporary160tick clock.
    // Each later tendency is explicit fixture data, distinct from the requested production motion.
    result.add(darkPowerScene("dark_power_fly_crawl_production_pose","fly",1,100,"pose_crawl"));
    result.add(darkPowerScene("dark_power_power_paddle_production_pose","power",1,100,"pose_paddle"));
    // Fly-specialized actor exercises swimming motion; motion name does not select its specialization.
    result.add(darkPowerScene("dark_power_swim_production_pose","fly",1,100,"pose_swim"));
    result.add(darkPowerScene("dark_power_glide_production_pose","run_fly",1,100,"pose_glide"));
    result.add(darkPowerScene("dark_power_flail_production_pose","power_fly",1,100,"pose_flail"));
    // 108: opposite Swim-specialized first-Power target in actual production CRAWL.
    result.add(darkPowerScene("dark_power_swim_crawl_production_pose","swim",1,100,"pose_crawl"));
    // 109: immediately preceding Dark Swim068 stays on its original locked Swim1 route.
    result.add(scene("dark_swim_swim_mature_regression","swim",true,ChaoState.SWIM_TYPE,-1,1,100,"still","",false,"natural",0));
    // 110..111: actual floor-toy production path after WALK, including once cleanup.
    result.add(darkPowerScene("dark_power_balanced_floor_car","normal",1,100,"toy_car"));
    result.add(darkPowerScene("dark_power_fly_floor_crayons","fly",1,100,"crayons"));
    // 112..113: immediately preceding Dark Fly069 retains its own locked Fly2 route.
    result.add(scene("dark_fly_fly_mature_regression","fly",true,ChaoState.FLY_TYPE,-1,1,100,"still","",false,"natural",0));
    result.add(scene("dark_fly_swim_mature_regression","swim",true,ChaoState.FLY_TYPE,-1,1,100,"still","",false,"natural",0));
    // 114..115: later Run and Power endpoints on the locked first-Power actor in actual floor play.
    result.add(darkPowerScene("dark_power_run_floor_car","run",1,100,"toy_car"));
    result.add(darkPowerScene("dark_power_power_floor_crayons","power",1,100,"crayons"));
    // 116..142: fresh071 game views of every completed070 Dark/Run target and stage.
    // These remain genuine locked firstRUN3 on the retained070 sampler/asset.
    // Their viewer references are retained immutable070 source images, not old game screenshots.
    for(String form:new String[]{"normal","run","power","fly","swim","run_fly","run_swim","power_fly","power_swim"})
      for(float age:new float[]{0,.5f,1})result.add(scene("retained_dark_run_"+form+"_age"+Math.round(age*100)+"_regression",form,true,ChaoState.RUN_TYPE,-1,age,100,"still","",false,"natural",0));
    require(result.size()==143,"unexpected scene catalog size");
    String selection=System.getenv("CHAO_DARKPOWER071_CASES");
    if(selection!=null&&!selection.isBlank()) {
      var chosen=new ArrayList<Case>();var used=new HashSet<Integer>();
      for(String token:selection.split(",")){int selected=Integer.parseInt(token);require(selected>=0&&selected<result.size()&&used.add(selected),"invalid or duplicate preflight scene selection");chosen.add(result.get(selected));}
      return List.copyOf(chosen);
    }
    return List.copyOf(result);
  }
  private static final List<Case> CASES=cases();
  private static final int[][] SHOTS=new int[CASES.size()][6];
  private static final int[] WORLD=new int[CASES.size()],RENDERED=new int[CASES.size()],MOVING=new int[CASES.size()],COCOON=new int[CASES.size()],BEFORE=new int[CASES.size()],AFTER=new int[CASES.size()],CONTACTS=new int[CASES.size()];
  private static final long[] SIGNATURES=new long[CASES.size()];
  private static final float[] MAX_CONTACT=new float[CASES.size()];
  private static final int[] SWIMMING=new int[CASES.size()],RUNNING=new int[CASES.size()];
  private static final int[] UI_FRAMES=new int[CASES.size()],UI_SHOTS=new int[CASES.size()];
  private static final boolean[] UI_VERIFIED=new boolean[CASES.size()];
  private static final boolean[] POSE_ONCE_DONE=new boolean[CASES.size()],POSE_LOOP_WRAPPED=new boolean[CASES.size()],POSE_STOP_DONE=new boolean[CASES.size()],POSE_SAVE_CHECKED=new boolean[CASES.size()];
  private static final int[] POSE_FRAMES=new int[CASES.size()],POSE_ENTRY_FRAMES=new int[CASES.size()],POSE_EXIT_FRAMES=new int[CASES.size()];
  private static final int[][] POSE_HOLD_VIEWS=new int[CASES.size()][3],POSE_HOLD_CAPTURES=new int[CASES.size()][3];
  private static String poseState;
  private static boolean poseOriginalNoAi,poseOriginalNoGravity,poseOriginalStay,poseRenderedHoldThisFrame;
  private static java.util.List<ItemStack> poseInventory,poseEquipment;
  private static UUID poseEntityId,poseKeeperId;
  private static final boolean[] FLOOR_TOY_ONCE_DONE=new boolean[CASES.size()];
  private static final int[] FLOOR_TOY_FRAMES=new int[CASES.size()];
  private static final int[][] FLOOR_TOY_HOLD_VIEWS=new int[CASES.size()][3],FLOOR_TOY_HOLD_CAPTURES=new int[CASES.size()][3];
  private static String floorToyState;
  private static boolean floorToyOriginalNoAi,floorToyOriginalNoGravity,floorToyOriginalStay,floorToyRenderedHoldThisFrame;
  private static float floorToyRenderedPhaseThisFrame;
  private static String floorToyRenderedMotionThisFrame="NONE";
  private static List<ItemStack> floorToyInventory,floorToyEquipment;
  private static UUID floorToyEntityId,floorToyKeeperId;
  private static final AtomicInteger SAVED=new AtomicInteger();
  private static final LionfishQuietToyClip049 QUIET=new LionfishQuietToyClip049();
  private static final LionfishActiveToyClip049 ACTIVE_TOY=new LionfishActiveToyClip049();
  private static boolean enabled(){return "true".equalsIgnoreCase(System.getenv("CHAO_DARKPOWER071_SMOKE"));}
  private static final java.nio.file.Path OUTPUT=java.nio.file.Path.of(System.getenv().getOrDefault("CHAO_DARKPOWER071_OUTPUT","/workspace/.setup/dark071-client-final")).toAbsolutePath().normalize();
  private static int parameter(String name,int fallback){String value=System.getenv(name);return value==null?fallback:Integer.parseInt(value);}
  private static ChaoMeshAsset expectedMesh,expectedNeutralMesh,expectedHeroNormalMesh,expectedNeutralPowerMesh,expectedNeutralRunMesh,expectedDarkPowerMesh,expectedDarkRunMesh,expectedDarkFlyMesh,expectedDarkSwimMesh,expectedHeroPowerMesh,expectedHeroRunMesh,expectedHeroFlyMesh,expectedHeroSwimMesh,expectedNeutralFlyMesh,expectedNeutralSwimMesh,expectedDarkNormalMesh;
  private static final boolean[] SKIPPED=new boolean[CASES.size()];
  private static final long[] LAST_TRACE=new long[CASES.size()];
  private static final boolean[] PAIR_DISPLAY_SETTLED=new boolean[CASES.size()];
  private static boolean baseline(){return false;}
  private static final String[] STATIC_ANGLES={"front","left","right","back","above","below"};
  private static final String[] MOTION_ANGLES={"front","left","back"};
  private static int limit(){return Math.clamp(parameter("CHAO_DARKPOWER071_LIMIT",CASES.size()),1,CASES.size());}
  private static int start(){return Math.clamp(parameter("CHAO_DARKPOWER071_START",0),0,limit()-1);}
  private static volatile int phase=-1;
  private static volatile UUID actorId;
  private static volatile boolean armed,complete;
  private static volatile long elapsed,serverTime;
  private static volatile String expectedState="",fatal;
  private static ChaoEntity actor;
  private static ArmorStand observer;
  private static Container slot;
  private static Object accessoryContainer;
  private static Class<?> containerType;
  private static long began,lastTick,wallStarted,playbackWall,carryBefore;
  private static Vec3 previousPosition;
  private static double walked;
  private static float initialMaturity;
  private static boolean started,setup,finished,armRequested,renderedThisFrame,worldThisFrame;
  private static int clientPhase=-1,pendingPhase=-1,view,viewFrames,ticks,shots;
  private static long lastMotionCapture=-1;
  private static final Vec3 ORIGIN=new Vec3(.5,80,.5);
  private static final int STATIC_EPOCH=120;
  private static int[] controlledTotals;
  private static int uiStep,uiTicks,uiBaseFrames;
  private static boolean uiOpened,uiFinished,uiFreezeRequested;
  private static volatile boolean uiWorldReady;
  private static float uiLockedAlignment;

  private static void require(boolean okay,String message){if(!okay)throw new IllegalStateException("DARKPOWER071 "+message);}
  private static void onServer(MinecraftServer server,Runnable task){server.execute(()->{try{task.run();}catch(Throwable error){fatal=error.toString();LogUtils.getLogger().error("DARKPOWER071_ASYNC_FAILURE",error);}});}
  private static void markSlot(){try{containerType.getMethod("markChanged",boolean.class).invoke(accessoryContainer,false);}catch(ReflectiveOperationException error){throw new IllegalStateException(error);}}
  private static void clearSlot(){if(slot!=null){slot.setItem(0,ItemStack.EMPTY);markSlot();slot=null;accessoryContainer=null;}}

  private static String sha256(java.nio.file.Path file){try{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(file)));}catch(Exception error){throw new IllegalStateException(error);}}

  /** Exact stage seeds use the real training operation and existing maturity setter.
   * No mesh, animation pose, skin or attachment is injected by this helper. */
  private static ChaoState fixture(Case entry){
    var state=new ChaoState();state.energy=state.hunger=100;state.happiness=80;state.boredom=0;
    state.adult=entry.adult();state.align=entry.alignment();
    if(entry.adult())state.primaryType=entry.firstType();
    state.setEvolutionStrength(entry.adult()?entry.maturity()*GradualEvolutionRules.ADULT_STRENGTH_MAX:entry.maturity());
    int total=Math.round((entry.adult()?DarkPowerMorphology071.SPECIALIZATION_PROGRESS:HeroRunMorphology052.BABY_RUN_PROGRESS)*entry.percent()/100f);
    if(entry.mobilityBaseline()>0){
      state.train(ChaoState.RUN,entry.mobilityBaseline(),new Random(5957));
      state.train(ChaoState.POWER,entry.mobilityBaseline(),new Random(5958));
    }
    if(entry.swim()){
      // Actual advanced-swimming permission uses earned points, independently
      // of morphology. Equal Fly/Swim mobility bars preserve the net target.
      state.train(ChaoState.SWIM,4000,new Random(5959));
      state.train(ChaoState.FLY,4000,new Random(5960));
    }
    for(int stat:entry.trainedStats())state.train(stat,total,new Random(5956+stat));
    if(entry.counterPercent()>0)state.train(ChaoState.SWIM,Math.round(DarkPowerMorphology071.SPECIALIZATION_PROGRESS*entry.counterPercent()/100f),new Random(5961));
    if(entry.action().equals("evolution")){
      state.ageTicks=GardenConfig.CHILD_TICKS.get()-12;state.setEvolutionStrength(.99f);
      require(state.prospectivePrimaryType()==ChaoState.POWER_TYPE,"Dark Power evolution fixture has the wrong trained first Power type");
    }
    if(entry.rabbit()){
      int rabbit=Arrays.asList(ChaoState.ANIMALS).indexOf("rabbit");require(rabbit>=0,"registered rabbit DNA absent");
      state.animalMask=1<<rabbit;
      for(int i=0;i<state.animalParts.length;i++)if((ChaoState.animalSlots(rabbit)&(1<<i))!=0)state.animalParts[i]=rabbit;
      state.animalPartsMask=1<<rabbit;
    }
    switch(entry.coat()){
      case "red_twotone","red_shiny"->{state.expressedColor=2;state.colorGenes=new int[]{2,2};state.twoTone=true;state.toneGenes=new int[]{1,1};}
      case "ruby"->{state.expressedJewel=3;state.jewelGenes=new int[]{3,3};}
      case "green_mono"->{state.expressedColor=10;state.colorGenes=new int[]{10,10};state.twoTone=false;state.toneGenes=new int[]{0,0};}
      default->{}
    }
    if(entry.coat().equals("red_shiny")||entry.coat().equals("natural_shiny")){state.shiny=true;state.shinyGenes=new int[]{1,1};}
    if(entry.phoenix()){
      int phoenix=Arrays.asList(ChaoState.ANIMALS).indexOf("phoenix");require(phoenix>=0,"registered phoenix DNA absent");
      state.animalParts[ChaoState.WINGS]=phoenix;state.animalMask|=1<<phoenix;state.animalPartsMask|=1<<phoenix;
    }
    // Serialization initializes the genuine adult identity lock and proves no new schema is needed.
    // Static stages stay server-frozen. Natural first evolution is inspected
    // in its own ticking before/cocoon/after scene, not inferred from this seed.
    String encoded=state.encode();var decoded=ChaoState.decode(encoded);require(decoded.encode().equals(encoded),"stage state did not round trip exactly");
    if(entry.adult())require(decoded.lockedPrimaryType()==entry.firstType()&&decoded.primaryType==entry.firstType()&&Math.abs(decoded.lockedAlignment()-entry.alignment())<.0001f,"fixture did not lock its explicit first identity");
    return decoded;
  }
  private static void equip(ServerPlayer player){
    require(AccessoriesBridge.available(),"carry scenes require actual Accessories/owo runtime jars");
    try{
      var carrier=new ItemStack(ChaoGarden.item("chao_carrier"));require(ChaoCarrierItem.capture(carrier,actor,player),"actual carrier capture failed");
      var capabilityType=Class.forName("io.wispforest.accessories.api.AccessoriesCapability");containerType=Class.forName("io.wispforest.accessories.api.AccessoriesContainer");
      var capability=capabilityType.getMethod("get",LivingEntity.class).invoke(null,player);
      accessoryContainer=((Map<?,?>)capabilityType.getMethod("getContainers").invoke(capability)).get("chao_companion");
      require(accessoryContainer!=null,"actual companion slot missing");slot=(Container)containerType.getMethod("getAccessories").invoke(accessoryContainer);
      slot.setItem(0,carrier);markSlot();actorId=ChaoCarrierItem.storedUUID(carrier);actor=null;
    }catch(ReflectiveOperationException error){throw new IllegalStateException(error);}
  }
  private static void next(MinecraftServer server,int index){
    server.tickRateManager().setFrozen(true);clearSlot();
    if(actor!=null){ToyPreview.stop(actor);actor.setCompanionCarry(null,0);actor.discard();actor=null;}
    while(index<limit()&&CASES.get(index).carry()&&!AccessoriesBridge.available()){
      SKIPPED[index]=true;LogUtils.getLogger().warn("DARKPOWER071_SKIP case={} name={} reason=Actual Accessories runtime unavailable; equipped scene was not rendered or verified",index,CASES.get(index).name());index++;
    }
    if(index>=limit()){phase=limit();server.tickRateManager().setFrozen(false);return;}
    var level=server.overworld();var entry=CASES.get(index);var player=server.getPlayerList().getPlayers().getFirst();
    player.stopFallFlying();player.getAbilities().flying=false;player.getAbilities().mayfly=false;player.onUpdateAbilities();player.setNoGravity(true);player.setDeltaMovement(Vec3.ZERO);
    player.teleportTo(entry.carry()?.5:4.5,entry.carry()?80:82,entry.carry()?.5:5.5);player.setYRot(0);player.yBodyRot=player.yHeadRot=0;player.setXRot(0);
    player.setGameMode(entry.carry()||entry.ui()&&!entry.action().equals("ui_survival")?GameType.CREATIVE:entry.ui()?GameType.SURVIVAL:GameType.SPECTATOR);
    if(entry.ui()){
      player.teleportTo(1.5,80,2.5);player.getInventory().selected=0;
      player.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(ChaoGarden.item("chao_analyzer")));player.inventoryMenu.broadcastChanges();
    }
    if(entry.action().startsWith("carry_flying")){player.getAbilities().mayfly=true;player.getAbilities().flying=true;player.onUpdateAbilities();player.teleportTo(.5,82,.5);}
    for(var at:BlockPos.betweenClosed(-4,80,-4,9,82,4)){
      boolean wall=at.getX()==-4||at.getX()==9||at.getZ()==-4||at.getZ()==4;
      level.setBlock(at,entry.swim()?(wall?Blocks.SMOOTH_STONE:Blocks.WATER).defaultBlockState():Blocks.AIR.defaultBlockState(),3);
    }
    actor=ChaoGarden.CHAO.get().create(level);require(actor!=null,"Chao fixture creation failed");actor.owner=player.getUUID();actor.setState(fixture(entry));
    actor.setNoAi(!entry.walk());actor.setInvulnerable(true);actor.setOnGround(true);actor.setNoGravity(entry.photo());actor.deferBehavior(100000);
    Vec3 initialActorAt=entry.photo()?ORIGIN.add(0,2,0):entry.swim()?ORIGIN.add(0,.2,0):ORIGIN;
    actor.moveTo(initialActorAt.x,initialActorAt.y,initialActorAt.z,0,0);actor.yBodyRot=actor.yBodyRotO=actor.yHeadRot=actor.yHeadRotO=0;
    actor.goalSelector.removeAllGoals(goal->true);actor.targetSelector.removeAllGoals(goal->true);
    if(!entry.hat().isEmpty())actor.setItemSlot(EquipmentSlot.HEAD,new ItemStack(switch(entry.hat()){case "pumpkin"->Items.CARVED_PUMPKIN;case "melon"->Items.MELON;default->Items.SKELETON_SKULL;}));
    if(entry.action().contains("wings_glasses"))actor.setItemSlot(EquipmentSlot.OFFHAND,new ItemStack(ChaoGarden.item("round_glasses_accessory")));
    var saved=new CompoundTag();actor.saveWithoutId(saved);String before=actor.state().encode();actor.load(saved);
    require(actor.state().encode().equals(before),"entity save/reload changed prototype state");
    if(entry.photo())actor.tickCount=STATIC_EPOCH;
    require(level.addFreshEntity(actor),"stage Chao did not enter actual level");actorId=actor.getUUID();expectedState=before;
    if(entry.carry())equip(player);
    phase=index;elapsed=0;serverTime=began=lastTick=level.getGameTime();previousPosition=new Vec3(.5,80,.5);walked=0;
    armed=complete=false;playbackWall=System.nanoTime();initialMaturity=visibleMaturity(fixture(entry));
    uiStep=uiTicks=uiBaseFrames=0;uiOpened=uiFinished=uiFreezeRequested=uiWorldReady=false;uiLockedAlignment=actor==null?entry.alignment():actor.state().lockedAlignment();
    if(entry.carry())server.tickRateManager().setFrozen(false);
    LogUtils.getLogger().info("DARKPOWER071_CASE {} name={} baseline={} adult={} percent={} alignment={} maturity={} action={} coat={} suspendedStatic={} sample={} babyVisibleGrowth={} state={} epochMillis={} actualAccessories={}",index,entry.name(),baseline(),entry.adult(),entry.percent(),entry.alignment(),entry.maturity(),entry.action(),entry.coat(),entry.photo(),sample(ChaoState.decode(expectedState)),HeroRunMorphology052.sample(ChaoState.decode(expectedState)).babyGrowth(),expectedState,System.currentTimeMillis(),entry.carry());
  }
  private static void command(MinecraftServer server,String toy,String mode){
    try{var player=server.getPlayerList().getPlayers().getFirst();require(server.getCommands().getDispatcher().execute("chao testtoy "+toy+" "+mode,player.createCommandSourceStack().withPermission(2))==1,"native toy preview dispatcher rejected "+toy);}
    catch(com.mojang.brigadier.exceptions.CommandSyntaxException error){throw new IllegalStateException(error);}
  }
  private static void poseCommand(MinecraftServer server,String id,String mode){
    try{var player=server.getPlayerList().getPlayers().getFirst();require(server.getCommands().getDispatcher().execute("chao testpose "+id+" "+mode,player.createCommandSourceStack().withPermission(2))==1,"native production pose dispatcher rejected "+id+" "+mode);}
    catch(com.mojang.brigadier.exceptions.CommandSyntaxException error){throw new IllegalStateException(error);}
  }
  private static java.util.List<ItemStack> poseEquipment(){
    var items=new ArrayList<ItemStack>();for(var slot:java.util.List.of(EquipmentSlot.MAINHAND,EquipmentSlot.OFFHAND,EquipmentSlot.HEAD,EquipmentSlot.CHEST,EquipmentSlot.LEGS,EquipmentSlot.FEET))items.add(actor.getItemBySlot(slot).copy());return List.copyOf(items);
  }
  private static void poseItems(MinecraftServer server){
    var player=server.getPlayerList().getPlayers().getFirst();
    require(actor.getUUID().equals(poseEntityId)&&Objects.equals(actor.owner,poseKeeperId),"operator pose changed entity or keeper identity");
    require(player.getInventory().getContainerSize()==poseInventory.size(),"operator pose resized keeper inventory");
    for(int i=0;i<poseInventory.size();i++)require(ItemStack.matches(player.getInventory().getItem(i),poseInventory.get(i)),"operator pose changed a real keeper item");
    var equipment=poseEquipment();for(int i=0;i<equipment.size();i++)require(ItemStack.matches(equipment.get(i),poseEquipment.get(i)),"operator pose changed actual Chao equipment");
    require(actor.getHeldChaoItem().isEmpty(),"operator pose minted a real prop");
  }
  private static void poseRestored(MinecraftServer server){
    require(!ToyPreview.active(actor)&&actor.toyPlayingTicks()==0&&actor.toyPlayingKind().isEmpty()&&actor.toyTarget()==null&&!actor.getPersistentData().contains("ChaoToyPreviewRestoreNoAi"),"operator pose left temporary session state");
    require(actor.isNoAi()==poseOriginalNoAi&&actor.isNoGravity()==poseOriginalNoGravity&&actor.isStaying()==poseOriginalStay,"operator pose failed to restore original control flags");
    require(actor.state().encode().equals(poseState),"operator pose changed saved care, genes, lessons, chronology or training");poseItems(server);
  }
  private static void poseTick(MinecraftServer server,Case entry){
    require(actor.state().encode().equals(poseState),"operator pose changed frozen care during playback");poseItems(server);
    if(elapsed==80&&!POSE_SAVE_CHECKED[phase]){
      var tag=new CompoundTag();actor.saveWithoutId(tag);
      require(tag.getString("Chao").equals(poseState)&&tag.getBoolean("NoAI")==poseOriginalNoAi&&!tag.contains("ToyPlayingKind")&&!tag.contains("ToyPlayingRemaining"),"active pose snapshot did not retain ordinary state and original NoAI");
      require(ToyPreview.active(actor),"saving an operator pose interrupted playback");POSE_SAVE_CHECKED[phase]=true;
      LogUtils.getLogger().info("DARKPOWER071_POSE_SAVE case={} pose={} tick={} savedStateExact=true originalNoAi={} temporaryClockOmitted=true epochMillis={}",phase,entry.poseId(),elapsed,poseOriginalNoAi,System.currentTimeMillis());
    }
    if(elapsed==160&&!POSE_ONCE_DONE[phase]){
      ToyPreview.tick(actor);poseRestored(server);POSE_ONCE_DONE[phase]=true;
      LogUtils.getLogger().info("DARKPOWER071_POSE_ONCE_END case={} pose={} tick={} stateExact=true controlsRestored=true itemsExact=true firstType={} epochMillis={}",phase,entry.poseId(),elapsed,actor.state().lockedPrimaryType(),System.currentTimeMillis());
      poseCommand(server,entry.poseId(),"loop");require(ToyPreview.active(actor)&&actor.toyPlayingTicks()==160&&ToyPreview.poseId(actor.toyPlayingKind()).equals(entry.poseId()),"actual loop command did not restart the production clock");
    }
    if(elapsed==320&&!POSE_LOOP_WRAPPED[phase]){
      ToyPreview.tick(actor);require(POSE_ONCE_DONE[phase]&&ToyPreview.active(actor)&&actor.toyPlayingTicks()==160&&ToyPreview.poseId(actor.toyPlayingKind()).equals(entry.poseId()),"actual production pose loop failed its full160tick wrap");POSE_LOOP_WRAPPED[phase]=true;
      LogUtils.getLogger().info("DARKPOWER071_POSE_LOOP_WRAP case={} pose={} tick={} remaining={} stateExact=true firstType={} epochMillis={}",phase,entry.poseId(),elapsed,actor.toyPlayingTicks(),actor.state().lockedPrimaryType(),System.currentTimeMillis());
    }
    if(elapsed==400&&!POSE_STOP_DONE[phase]){
      require(POSE_ONCE_DONE[phase]&&POSE_LOOP_WRAPPED[phase]&&ToyPreview.active(actor),"actual pose once or looping observation is missing");poseCommand(server,entry.poseId(),"stop");poseRestored(server);POSE_STOP_DONE[phase]=true;
      LogUtils.getLogger().info("DARKPOWER071_POSE_STOP case={} pose={} tick={} stateExact=true controlsRestored=true itemsExact=true originalNoAi={} firstType={} epochMillis={}",phase,entry.poseId(),elapsed,poseOriginalNoAi,actor.state().lockedPrimaryType(),System.currentTimeMillis());
    }
    if(elapsed<400)require(ToyPreview.active(actor)&&ToyPreview.poseId(actor.toyPlayingKind()).equals(entry.poseId()),"operator pose stopped or changed before its scheduled end");
  }
  private static void arm(MinecraftServer server,int index){
    if(index!=phase||armed)return;var entry=CASES.get(index);
    if(entry.carry()){
      var actual=CompanionContent.findEntity(server,actorId);require(actual instanceof ChaoEntity,"actual equipped slot did not project a body");actor=(ChaoEntity)actual;expectedState=actor.state().encode();
    }
    require(actor!=null&&!actor.isRemoved(),"active fixture missing");initialMaturity=visibleMaturity(actor.state());
    controlledTotals=new int[5];for(int stat=0;stat<5;stat++)controlledTotals[stat]=HeroRunMorphology052.totalTraining(actor.state(),stat);
    if(entry.floorToy()){
      floorToyState=actor.state().encode();floorToyOriginalNoAi=actor.isNoAi();floorToyOriginalNoGravity=actor.isNoGravity();floorToyOriginalStay=actor.isStaying();floorToyEntityId=actor.getUUID();floorToyKeeperId=actor.owner;
      var player=server.getPlayerList().getPlayers().getFirst();var inventory=new ArrayList<ItemStack>();for(int i=0;i<player.getInventory().getContainerSize();i++)inventory.add(player.getInventory().getItem(i).copy());floorToyInventory=List.copyOf(inventory);floorToyEquipment=poseEquipment();
    }
    if(entry.toy())command(server,entry.toyId(),"once");
    if(entry.floorToy()){
      require(ToyPreview.active(actor)&&actor.toyPlayingTicks()==160&&actor.toyPlayingKind().equals(entry.toyKind()),"actual floor toy once command did not start the production160tick clock");
      require(actor.state().encode().equals(floorToyState),"starting floor toy changed saved Chao state");
      LogUtils.getLogger().info("DARKPOWER071_FLOOR_TOY_START case={} toy={} kind={} remaining={} stateExact=true firstType={} epochMillis={}",phase,entry.toyId(),actor.toyPlayingKind(),actor.toyPlayingTicks(),actor.state().lockedPrimaryType(),System.currentTimeMillis());
    }
    if(entry.pose()){
      poseState=actor.state().encode();poseOriginalNoAi=actor.isNoAi();poseOriginalNoGravity=actor.isNoGravity();poseOriginalStay=actor.isStaying();poseEntityId=actor.getUUID();poseKeeperId=actor.owner;
      var player=server.getPlayerList().getPlayers().getFirst();var inventory=new ArrayList<ItemStack>();for(int i=0;i<player.getInventory().getContainerSize();i++)inventory.add(player.getInventory().getItem(i).copy());poseInventory=List.copyOf(inventory);poseEquipment=poseEquipment();
      poseCommand(server,entry.poseId(),"once");require(ToyPreview.active(actor)&&actor.toyPlayingTicks()==160&&ToyPreview.poseId(actor.toyPlayingKind()).equals(entry.poseId()),"actual once command did not start the production pose clock");
      LogUtils.getLogger().info("DARKPOWER071_POSE_START case={} pose={} mode=once remaining={} originalNoAi={} stateExact=true firstType={} epochMillis={}",phase,entry.poseId(),actor.toyPlayingTicks(),poseOriginalNoAi,actor.state().lockedPrimaryType(),System.currentTimeMillis());
    }
    began=lastTick=serverTime=server.overworld().getGameTime();elapsed=0;playbackWall=System.nanoTime();armed=true;
    require(Math.abs(server.tickRateManager().tickrate()-20)<.00001,"study must retain normal 20 TPS");
    if(!entry.still()||entry.ui())server.tickRateManager().setFrozen(false);
    LogUtils.getLogger().info("DARKPOWER071_ARMED {} epochMillis={} normal20TPS=true fixtureTrainingAccelerated={}",phase,System.currentTimeMillis(),entry.gain()||entry.loss()||entry.opposite());
  }
  private static void floorToyOnceEnd(MinecraftServer server,Case entry){
    ToyPreview.tick(actor);
    require(!ToyPreview.active(actor)&&actor.toyPlayingTicks()==0&&actor.toyPlayingKind().isEmpty()&&actor.toyTarget()==null&&!actor.getPersistentData().contains("ChaoToyPreviewRestoreNoAi"),"actual floor toy once left temporary session state");
    require(actor.isNoAi()==floorToyOriginalNoAi&&actor.isNoGravity()==floorToyOriginalNoGravity&&actor.isStaying()==floorToyOriginalStay,"actual floor toy failed to restore original controls");
    require(actor.state().encode().equals(floorToyState),"actual floor toy changed saved care, genes, lessons, age or training");
    require(actor.getUUID().equals(floorToyEntityId)&&Objects.equals(actor.owner,floorToyKeeperId),"actual floor toy changed entity or keeper identity");
    var player=server.getPlayerList().getPlayers().getFirst();require(player.getInventory().getContainerSize()==floorToyInventory.size(),"floor toy resized keeper inventory");
    for(int i=0;i<floorToyInventory.size();i++)require(ItemStack.matches(player.getInventory().getItem(i),floorToyInventory.get(i)),"floor toy changed a real keeper item");
    var equipment=poseEquipment();for(int i=0;i<equipment.size();i++)require(ItemStack.matches(equipment.get(i),floorToyEquipment.get(i)),"floor toy changed Chao equipment");
    require(actor.getHeldChaoItem().isEmpty(),"floor toy minted a real prop");FLOOR_TOY_ONCE_DONE[phase]=true;
    LogUtils.getLogger().info("DARKPOWER071_FLOOR_TOY_ONCE_END case={} toy={} tick={} remaining=0 kindEmpty=true temporaryStateCleared=true stateExact=true controlsRestored=true itemsExact=true firstType={} epochMillis={}",phase,entry.toyId(),elapsed,actor.state().lockedPrimaryType(),System.currentTimeMillis());
  }
  @SubscribeEvent public static void server(ServerTickEvent.Post event){
    if(!enabled()||phase<0||phase>=limit())return;
    var server=event.getServer();var level=server.overworld();long now=level.getGameTime();
    if(!armed||complete||now==lastTick)return;lastTick=serverTime=now;elapsed=now-began;
    var entry=CASES.get(phase);require(actor!=null&&!actor.isRemoved(),"actual running actor disappeared");
    if(entry.ui())return;
    if(entry.pose()){poseTick(server,entry);if(elapsed<entry.duration())return;}
    if(entry.walk()){
      actor.getNavigation().moveTo(40.5,80,.5,.7);
      walked+=actor.position().subtract(previousPosition).horizontalDistance();previousPosition=actor.position();
    }
    // Correct incidental movement/water training through the actual Analyzer
    // rules so the photographed input schedule remains controlled. Production
    // navigation, animation and water detection still run at ordinary 20 TPS.
    boolean corrected=false;
    if(!entry.action().equals("evolution"))for(int stat=0;stat<4;stat++)
      if(HeroRunMorphology052.totalTraining(actor.state(),stat)!=controlledTotals[stat]){setTraining(actor.state(),stat,controlledTotals[stat]);corrected=true;}
    if(corrected)actor.sync();
    if(entry.floorToy()&&elapsed<160)require(ToyPreview.active(actor)&&actor.toyPlayingKind().equals(entry.toyKind()),"actual floor toy stopped or changed before its production160tick once end");
    if(entry.floorToy()&&elapsed==160)floorToyOnceEnd(server,entry);
    if(entry.gain()&&elapsed<=240&&elapsed%2==0){
      for(int stat:entry.trainedStats()){
        actor.state().train(stat,8,new Random(5957+elapsed+stat));
        controlledTotals[stat]=HeroRunMorphology052.totalTraining(actor.state(),stat);
      }actor.sync();
      require(Math.abs(actor.state().lockedAlignment()-entry.alignment())<.0001,"training changed independent alignment");
    }
    if((entry.loss()&&elapsed<=240||entry.pair()&&elapsed>320&&elapsed<=560)&&elapsed%2==0){
      long since=entry.pair()?elapsed-320:elapsed;
      int remaining=Math.max(0,960-(int)(since/2)*8);
      for(int stat:entry.trainedStats()){
        int target=remaining+(stat==ChaoState.RUN||stat==ChaoState.POWER?entry.mobilityBaseline():0);
        setTraining(actor.state(),stat,target);controlledTotals[stat]=target;
      }actor.sync();
    }
    if(entry.opposite()&&elapsed<=480&&elapsed%2==0){
      boolean runPowerAxis=entry.runAxis()!=0;
      int opposing=runPowerAxis?(entry.runAxis()<0?ChaoState.POWER:ChaoState.RUN):(entry.flyAxis()<0?ChaoState.FLY:ChaoState.SWIM);
      actor.state().train(opposing,8,new Random(6666+elapsed));
      controlledTotals[opposing]=HeroRunMorphology052.totalTraining(actor.state(),opposing);actor.sync();
      if(elapsed%120==0)LogUtils.getLogger().info("DARKPOWER071_OPPOSITION_RAW case={} tick={} axis={} initial={} changingStat={} expected={} sample={} firstType={} epochMillis={}",phase,elapsed,runPowerAxis?"runPower":"swimFly",runPowerAxis?entry.runAxis():entry.flyAxis(),opposing,(runPowerAxis?entry.runAxis():entry.flyAxis())*(1-elapsed/240f),DarkPowerMorphology071.sample(actor.state()),actor.state().lockedPrimaryType(),System.currentTimeMillis());
    }
    if(entry.pair()&&elapsed==240){
      var peak=DarkPowerMorphology071.sample(actor.state());
      require(peak.enabled()&&peak.runPower()<-.99f&&peak.swimFly()>.99f,"compatible pair did not reach its true peak before reversal");
      LogUtils.getLogger().info("DARKPOWER071_PAIR_PEAK case={} tick={} sample={} firstType={} epochMillis={}",phase,elapsed,peak,actor.state().lockedPrimaryType(),System.currentTimeMillis());
    }
    if(entry.carry()){
      var player=server.getPlayerList().getPlayers().getFirst();
      require(CompanionContent.isManaged(actor),"actual Accessories body stopped being managed");
      if(elapsed>30)require(actor.companionCarryPosition()==(entry.action().startsWith("carry_flying")?2:1),"actual companion selected wrong carry attachment");
      require(player.getAbilities().flying==entry.action().startsWith("carry_flying"),"player flight fixture changed");
    }
    if(elapsed>=entry.duration()){
      if(entry.walk())require(walked>.8,"motion fixture did not really navigate");
      if(entry.pose())require(POSE_ONCE_DONE[phase]&&POSE_LOOP_WRAPPED[phase]&&POSE_STOP_DONE[phase]&&POSE_SAVE_CHECKED[phase],"actual production pose command lifecycle proof missing");
      if(entry.toy())require(!ToyPreview.active(actor)&&actor.toyPlayingTicks()==0&&actor.toyTarget()==null,"actual toy preview failed cleanup");
      if(entry.floorToy())require(FLOOR_TOY_ONCE_DONE[phase],"actual floor toy once completion was not observed");
      var sample=DarkPowerMorphology071.sample(actor.state());
      if(!entry.action().equals("evolution"))require(actor.state().lockedPrimaryType()==entry.firstType()&&Math.abs(actor.state().lockedAlignment()-entry.alignment())<.0001f,"live scene changed locked identity");
      if(entry.gain()&&!entry.pair())require(sample.enabled()&&(entry.runAxis()==0||sample.runPower()*entry.runAxis()>.99f)&&(entry.flyAxis()==0||sample.swimFly()*entry.flyAxis()>.99f),"accelerated training did not reach its independent endpoints");
      if(entry.loss()||entry.pair())require(sample.enabled()&&Math.abs(sample.runPower())<.001f&&Math.abs(sample.swimFly())<.001f&&sample.maturity()>=initialMaturity,"decrease left specialized traits or reduced biological maturity");
      if(entry.opposite()){
        float crossed=entry.runAxis()!=0?sample.runPower()*entry.runAxis():sample.swimFly()*entry.flyAxis();
        float retained=entry.runAxis()!=0?sample.swimFly():sample.runPower();
        require(sample.enabled()&&crossed<-.99f&&Math.abs(retained)<.001f&&sample.maturity()>=initialMaturity,"opposing training did not cross the selected axis through balance with age intact");
      }
      if(entry.swim())require(actor.isInWater(),"native swimming fixture left actual water");
      if(entry.action().equals("evolution"))require(actor.state().adult&&actor.state().lockedPrimaryType()==ChaoState.POWER_TYPE&&Math.abs(actor.state().lockedAlignment()+1)<.0001f&&!actor.isCocooned(),"actual first evolution did not finish as locked Dark Power");
      complete=true;server.tickRateManager().setFrozen(true);
      LogUtils.getLogger().info("DARKPOWER071_NATIVE_COMPLETE {} name={} baseline={} elapsed={} walked={} sample={} state={} epochMillis={}",phase,entry.name(),baseline(),elapsed,walked,sample,actor.state().encode(),System.currentTimeMillis());
    }
  }
  private static void setTraining(ChaoState state,int stat,int target){
    int targetLevel=target/100;
    while(state.levels[stat]<targetLevel)require(AnalyzerStatRules.apply(state,stat,AnalyzerStatRules.LEVEL,1),"actual Analyzer level increase rejected");
    while(state.levels[stat]>targetLevel)require(AnalyzerStatRules.apply(state,stat,AnalyzerStatRules.LEVEL,-1),"actual Analyzer level decrease rejected");
    require(AnalyzerStatRules.apply(state,stat,AnalyzerStatRules.PROGRESS,target%100),"actual Analyzer progress edit rejected");
    require(HeroRunMorphology052.totalTraining(state,stat)==target,"controlled Analyzer edit reached the wrong training total");
  }
  private static float visibleMaturity(ChaoState state){return state.adult?state.gradualVisualStrength():state.evolutionStrength();}
  private static Object sample(ChaoState state){
    if(NeutralPowerMorphology063.sample(state).enabled())return NeutralPowerAppearance063.sample(state);
    if(DarkPowerMorphology071.sample(state).enabled())return DarkPowerAppearance071.sample(state);
    if(DarkRunMorphology070.sample(state).enabled())return DarkRunAppearance070.sample(state);
    if(DarkFlyMorphology069.sample(state).enabled())return DarkFlyAppearance069.sample(state);
    if(DarkSwimMorphology068.sample(state).enabled())return DarkSwimAppearance068.sample(state);
    if(HeroPowerMorphology067.sample(state).enabled())return HeroPowerAppearance067.sample(state);
    if(HeroRunMorphology066.sample(state).enabled())return HeroRunAppearance066.sample(state);
    if(HeroFlyMorphology065.sample(state).enabled())return HeroFlyAppearance065.sample(state);
    if(HeroSwimMorphology064.sample(state).enabled())return HeroSwimAppearance064.sample(state);
    if(NeutralFlyMorphology061.sample(state).enabled())return NeutralFlyAppearance061.sample(state);
    if(NeutralSwimMorphology059.sample(state).enabled())return NeutralSwimAppearance059.sample(state);
    if(DarkNormalMorphology060.sample(state).enabled())return DarkNormalAppearance060.sample(state);
    if(NeutralRunMorphology062.sample(state).enabled())return NeutralRunAppearance062.sample(state);
    if(HeroNormalMorphology057.sample(state).enabled())return HeroNormalAppearance057.sample(state);
    if(NeutralMorphology056.sample(state).enabled())return NeutralAppearance056.sample(state);
    return HeroRunAppearance052.sample(state);
  }
  private static ChaoEntity observed(Minecraft mc){
    if(mc.level==null||actorId==null)return null;
    for(var entity:mc.level.entitiesForRendering())if(entity instanceof ChaoEntity c&&c.getUUID().equals(actorId))return c;return null;
  }
  private static void press(ChaoInspectionScreen screen,String label){
    for(var widget:screen.children())if(widget instanceof AbstractButton button&&button.visible&&button.getMessage().getString().equals(label)){
      require(button.active,"inactive Analyzer button "+label);
      double x=screen.screenX(button.getX()+button.getWidth()/2f),y=screen.screenY(button.getY()+button.getHeight()/2f);
      require(screen.mouseClicked(x,y,0),"Analyzer navigation did not consume "+label);
      screen.mouseReleased(x,y,0);return;
    }throw new IllegalStateException("DARKPOWER071 missing Analyzer button "+label);
  }
  private static double barX(ChaoInspectionScreen screen,float alignment){return screen.screenX(398+162*(alignment+1)/2);}
  private static double barY(ChaoInspectionScreen screen){return screen.screenY(282);}
  private static java.lang.reflect.Method mouseMoveCallback;
  private static double requestedMouseX=Double.NaN,requestedMouseY=Double.NaN;
  private static int renderedMouseX=Integer.MIN_VALUE,renderedMouseY=Integer.MIN_VALUE;
  private static void cursor(Minecraft mc,double x,double y){
    requestedMouseX=x;requestedMouseY=y;
    long window=mc.getWindow().getWindow();
    double px=x*mc.getWindow().getScreenWidth()/mc.getWindow().getGuiScaledWidth(),py=y*mc.getWindow().getScreenHeight()/mc.getWindow().getGuiScaledHeight();
    org.lwjgl.glfw.GLFW.glfwSetCursorPos(window,px,py);
    // On an unfocused software/X11 window, a programmatic GLFW move need not
    // deliver its queued callback before the next screenshot. Exercise that
    // same native MouseHandler callback explicitly; never suppress tooltips or
    // paint an alternate UI. This developer-only helper is absent from the JAR.
    cursorCallback(mc,x,y);
  }
  private static void cursorCallback(Minecraft mc,double x,double y){
    long window=mc.getWindow().getWindow();
    double px=x*mc.getWindow().getScreenWidth()/mc.getWindow().getGuiScaledWidth(),py=y*mc.getWindow().getScreenHeight()/mc.getWindow().getGuiScaledHeight();
    try{
      if(mouseMoveCallback==null){mouseMoveCallback=MouseHandler.class.getDeclaredMethod("onMove",long.class,double.class,double.class);mouseMoveCallback.setAccessible(true);}
      mouseMoveCallback.invoke(mc.mouseHandler,window,px,py);
      require(Math.abs(mc.mouseHandler.xpos()-px)<.01&&Math.abs(mc.mouseHandler.ypos()-py)<.01,"native pointer callback did not move to requested GUI location");
    }catch(ReflectiveOperationException error){throw new IllegalStateException("Cannot drive actual MouseHandler cursor callback",error);}
  }
  private static void beginDrag(Minecraft mc,ChaoInspectionScreen screen,float alignment,boolean editable){
    double x=barX(screen,alignment),y=barY(screen);cursor(mc,x,y);
    boolean consumed=screen.mouseClicked(x,y,0);if(editable)require(consumed,"Creative baby alignment bar did not start drag");
    else{screen.mouseReleased(x,y,0);cursor(mc,screen.screenX(590),screen.screenY(60));}
  }
  private static void drag(Minecraft mc,ChaoInspectionScreen screen,float from,float to,int tick){
    float fraction=Math.clamp(tick/8f,0,1),value=from+(to-from)*fraction;
    double x=barX(screen,value),y=barY(screen);cursor(mc,x,y);screen.mouseDragged(x,y,0,(to-from)*162/16,0);
  }
  private static void release(Minecraft mc,ChaoInspectionScreen screen,float target){
    double x=barX(screen,target),y=barY(screen);cursor(mc,x,y);screen.mouseReleased(x,y,0);cursor(mc,screen.screenX(590),screen.screenY(60));
  }
  private static void captureUi(Minecraft mc,String label){
    require(mc.screen instanceof ChaoInspectionScreen&&UI_FRAMES[phase]>5,"Analyzer was not actually rendered");
    if(label.equals("maturity-drag-intermediate")){
      try {
        var dragging=ChaoInspectionScreen.class.getDeclaredField("maturityDragging");dragging.setAccessible(true);
        require(dragging.getBoolean(mc.screen),"intermediate maturity capture was not an active native drag");
      }catch(ReflectiveOperationException error){throw new IllegalStateException("Cannot verify actual maturity drag state",error);}
    }else{
      var screen=(ChaoInspectionScreen)mc.screen;
      require(Math.abs(renderedMouseX-screen.screenX(590))<=1&&Math.abs(renderedMouseY-screen.screenY(60))<=1,
          "settled UI capture still has a hover pointer: "+renderedMouseX+","+renderedMouseY);
    }
    LogUtils.getLogger().info("DARKPOWER071_UI_POINTER_RENDER case={} label={} actualGuiMouseX={} actualGuiMouseY={} requestedGuiMouseX={} requestedGuiMouseY={}",phase,label,renderedMouseX,renderedMouseY,requestedMouseX,requestedMouseY);
    String name=String.format("dark071-%s-%02d-%s-ui-%s.png","live",phase,CASES.get(phase).name(),label);int index=phase;
    Screenshot.grab(OUTPUT.toFile(),name,mc.getMainRenderTarget(),message->{require(Files.isRegularFile(OUTPUT.resolve("screenshots").resolve(name)),"Analyzer screenshot missing");LogUtils.getLogger().info("DARKPOWER071_UI_CAPTURE {} case={} sha256={} epochMillis={}",name,index,sha256(OUTPUT.resolve("screenshots").resolve(name)),System.currentTimeMillis());SAVED.incrementAndGet();});shots++;UI_SHOTS[phase]++;
  }
  private static double maturityX(ChaoInspectionScreen screen,float value){return screen.screenX(318+244*value);}
  private static double maturityY(ChaoInspectionScreen screen){return screen.screenY(297);}
  private static float maturityDragFrom,maturityDragTo;
  private static boolean maturityDragActive;
  private static void maturityDrag(Minecraft mc,ChaoInspectionScreen screen,float value,boolean editable){
    var chao=observed(mc);require(chao!=null,"maturity pointer study lost its Chao");
    maturityDragFrom=AnalyzerMaturityBar056.milli(chao.state().evolutionStrength(),chao.state().adult)/1000f;
    maturityDragTo=value;maturityDragActive=editable;
    double x=maturityX(screen,editable?maturityDragFrom:value),y=maturityY(screen);cursor(mc,x,y);
    boolean consumed=screen.mouseClicked(x,y,0);if(editable)require(consumed,"Creative maturity bar did not consume touch-sized click");
    if(!editable){screen.mouseReleased(x,y,0);cursor(mc,screen.screenX(590),screen.screenY(60));}
  }
  /** Traverse the real scrubber with a twelve-tick active midpoint hold so actual pointer/server feedback can render. */
  private static void continueMaturityDrag(Minecraft mc,ChaoInspectionScreen screen,int tick){
    if(!maturityDragActive)return;
    float fraction=tick<=8?tick/16f:tick<=20?.5f:Math.clamp(.5f+(tick-20)/16f,0,1),value=maturityDragFrom+(maturityDragTo-maturityDragFrom)*fraction;
    double x=maturityX(screen,value),y=maturityY(screen);cursor(mc,x,y);
    screen.mouseDragged(x,y,0,(maturityDragTo-maturityDragFrom)*244*screen.canvasScale/8,0);
    if(tick>=28){screen.mouseReleased(x,y,0);maturityDragActive=false;cursor(mc,screen.screenX(590),screen.screenY(60));}
  }
  private static void freezeUiResult(Minecraft mc){
    if(uiFreezeRequested)return;uiFreezeRequested=true;mc.setScreen(null);mc.options.hideGui=true;view=viewFrames=0;
    onServer(mc.getSingleplayerServer(),()->{
      var entry=CASES.get(phase);var player=mc.getSingleplayerServer().getPlayerList().getPlayers().getFirst();
      boolean survival=entry.action().equals("ui_survival");float alignment=actor.state().lockedAlignment();
      if(!survival&&!entry.adult())require(alignment>.75f&&alignment<.85f,"Creative baby alignment request failed");
      else require(Math.abs(alignment-uiLockedAlignment)<.0001f,"adult/Survival alignment lock failed");
      require(player.isCreative()!=survival,"actual permission changed");
      float expected=survival?(entry.adult()?.96f:.8f):entry.adult()?1.2f:.99f;
      require(Math.abs(actor.state().evolutionStrength()-expected)<.005f,"maturity control did not preserve safe value");
      require(actor.state().adult==entry.adult()&&!actor.state().dead&&!actor.isCocooned(),"maturity drag changed life stage");
      if(entry.adult())require(actor.state().lockedPrimaryType()==entry.firstType()&&actor.state().primaryType==entry.firstType(),"maturity drag changed the locked first type");
      mc.getSingleplayerServer().tickRateManager().setFrozen(true);String before=actor.state().encode();
      var tag=new CompoundTag();actor.saveWithoutId(tag);actor.load(tag);require(actor.state().encode().equals(before),"controls failed NBT reload");
      actor.sync();expectedState=before;UI_VERIFIED[phase]=true;uiWorldReady=true;
      LogUtils.getLogger().info("DARKPOWER071_UI_NATIVE_VERIFIED case={} creative={} adult={} alignment={} maturity={} saveReloadExact=true stagePreserved=true epochMillis={}",phase,!survival,entry.adult(),alignment,actor.state().evolutionStrength(),System.currentTimeMillis());
    });
  }
  /** Calls the actual screen's native mouse handlers and server payload path. */
  private static void uiControl(Minecraft mc){
    var entry=CASES.get(phase);if(!entry.ui()||!armed||uiWorldReady||uiFreezeRequested)return;
    var c=observed(mc);if(c==null||mc.player==null)return;
    boolean survival=entry.action().equals("ui_survival");
    if(!uiOpened){
      if(mc.player.isCreative()==survival||!mc.player.getMainHandItem().is(ChaoGarden.item("chao_analyzer")))return;
      NeoForge.EVENT_BUS.post(new PlayerInteractEvent.EntityInteract(mc.player,InteractionHand.MAIN_HAND,c));
      require(mc.screen instanceof ChaoInspectionScreen,"real Analyzer did not open");
      press((ChaoInspectionScreen)mc.screen,"MIND");mc.options.hideGui=false;
      cursor(mc,((ChaoInspectionScreen)mc.screen).screenX(590),((ChaoInspectionScreen)mc.screen).screenY(60));
      uiOpened=true;uiTicks=0;uiBaseFrames=UI_FRAMES[phase];return;
    }
    require(mc.screen instanceof ChaoInspectionScreen,"Analyzer closed during real edit study");
    if(UI_FRAMES[phase]-uiBaseFrames<10)return;uiTicks++;require(uiTicks<220,"Analyzer step timed out "+uiStep);
    var screen=(ChaoInspectionScreen)mc.screen;
    if(uiStep==0){
      captureUi(mc,survival?"survival-readonly":entry.adult()?"adult-alignment-locked":"baby-alignment");
      if(!survival&&!entry.adult()){beginDrag(mc,screen,0,true);uiStep=1;}
      else{
        beginDrag(mc,screen,entry.alignment(),false);
        int rejectedTarget=c.state().lockedAlignment()>=.5f?-1000:1000;
        require(Math.abs(c.state().lockedAlignment()-rejectedTarget/1000f)>.1f,"forged alignment request would be a no-op");
        ChaoCareNetwork.requestAlignment(c,rejectedTarget);uiStep=3;
        LogUtils.getLogger().info("DARKPOWER071_UI_FORGED_ALIGNMENT case={} creative={} adult={} from={} requestedMilli={}",phase,!survival,entry.adult(),c.state().lockedAlignment(),rejectedTarget);
      }uiTicks=0;
    }else if(uiStep==1){drag(mc,screen,0,.8f,uiTicks);if(uiTicks>=8){release(mc,screen,.8f);uiStep=2;uiTicks=0;}}
    else if(uiStep==2){if(uiTicks>=35&&c.state().lockedAlignment()>.75f){captureUi(mc,"baby-hero-alignment");uiStep=3;uiTicks=0;}}
    else if(uiStep==3){if(uiTicks>=35){if(entry.adult()||survival){require(Math.abs(c.state().lockedAlignment()-uiLockedAlignment)<.0001f,"forged alignment accepted");captureUi(mc,survival?"survival-alignment-tap-feedback":"adult-lock-tap-feedback");}press(screen,"STATS");uiStep=4;uiTicks=0;}}
    else if(uiStep==4){if(uiTicks>=20){captureUi(mc,"maturity-before");maturityDrag(mc,screen,.25f,!survival);if(survival)ChaoCareNetwork.requestMaturity(c,250);uiStep=5;uiTicks=0;}}
    else if(uiStep==5){continueMaturityDrag(mc,screen,uiTicks);if(!survival&&uiTicks==16)captureUi(mc,"maturity-drag-intermediate");if(uiTicks>=35){if(survival){require(Math.abs(c.state().evolutionStrength()-(entry.adult()?.96f:.8f))<.005,"Survival maturity request accepted");captureUi(mc,"maturity-survival-denied");freezeUiResult(mc);}else if(Math.abs(c.state().evolutionStrength()-(entry.adult()?.3f:.25f))<.005){captureUi(mc,"maturity-young");maturityDrag(mc,screen,1,true);uiStep=6;uiTicks=0;}}}
    else if(uiStep==6){continueMaturityDrag(mc,screen,uiTicks);float expected=entry.adult()?1.2f:.99f;if(uiTicks>=35&&Math.abs(c.state().evolutionStrength()-expected)<.005){captureUi(mc,"maturity-safe-endpoint");freezeUiResult(mc);}}
  }
  @SubscribeEvent public static void uiRendered(ScreenEvent.Render.Post event){
    if(enabled()&&phase>=0&&phase<limit()&&CASES.get(phase).ui()&&event.getScreen() instanceof ChaoInspectionScreen){
      UI_FRAMES[phase]++;renderedMouseX=event.getMouseX();renderedMouseY=event.getMouseY();
    }
  }
  @SubscribeEvent public static void restoreCamera(ClientTickEvent.Pre event){if(enabled()){
    var mc=Minecraft.getInstance();if(mc.player!=null&&mc.getCameraEntity()!=mc.player)mc.setCameraEntity(mc.player);
  }}
  private static void camera(Minecraft mc){
    var c=observed(mc);if(c==null)return;var entry=CASES.get(phase);
    if(observer==null||observer.level()!=mc.level){observer=new ArmorStand(mc.level,0,0,0);observer.setInvisible(true);observer.setNoGravity(true);}
    int selected=entry.still()?view:Math.min(2,(int)elapsed*3/Math.max(1,entry.duration()));
    var source=entry.carry()?mc.player:c;Vec3 focus,eye;
    if(entry.photo()){
      focus=ORIGIN.add(0,entry.adult()?2.55:2.45,0);
      Vec3 offset=switch(selected){case 0->new Vec3(0,0,2.1);case 1->new Vec3(2.1,0,0);case 2->new Vec3(-2.1,0,0);case 3->new Vec3(0,0,-2.1);case 4->new Vec3(0,1.85,.85);default->new Vec3(0,-1.65,1.10);};
      eye=focus.add(offset);require(eye.y>80,"underside camera was occluded by floor");
    }else{
      float heading=entry.carry()?mc.player.yBodyRot:c.yBodyRot;var forward=Vec3.directionFromRotation(0,heading);var side=new Vec3(forward.z,0,-forward.x);
      focus=source.position().add(0,entry.carry()?entry.action().equals("carry_ground")?1.75:1.05:entry.adult()?.55:.45,0);
      double distance=entry.carry()?3.6:2.0;
      eye=focus.add(selected==0?forward.scale(distance):selected==1?side.scale(distance):forward.scale(-distance*.82).add(side.scale(distance*.72)));
      if(selected==2)eye=eye.add(0,entry.carry()?.2:.3,0);
    }
    var delta=focus.subtract(eye);float yaw=(float)(Math.toDegrees(Math.atan2(delta.z,delta.x))-90),pitch=(float)-Math.toDegrees(Math.atan2(delta.y,Math.hypot(delta.x,delta.z)));
    observer.moveTo(eye.x,eye.y-observer.getEyeHeight(),eye.z,yaw,pitch);
    if(entry.photo()){observer.xo=observer.xOld=observer.getX();observer.yo=observer.yOld=observer.getY();observer.zo=observer.zOld=observer.getZ();}
    else{observer.xo=observer.xOld=observer.getX()+source.xo-source.getX();observer.yo=observer.yOld=observer.getY()+source.yo-source.getY();observer.zo=observer.zOld=observer.getZ()+source.zo-source.getZ();}
    observer.yRotO=yaw;observer.xRotO=pitch;observer.setYHeadRot(yaw);observer.yHeadRotO=yaw;
    mc.options.setCameraType(CameraType.FIRST_PERSON);mc.setCameraEntity(observer);if(selected!=view){view=selected;viewFrames=0;}
  }
  private static void catalog(){
    try{
      Files.createDirectories(OUTPUT);var rows=new com.google.gson.JsonArray();
      for(int i=0;i<CASES.size();i++){
        var c=CASES.get(i);var row=new com.google.gson.JsonObject();
        row.addProperty("index",i);row.addProperty("name",c.name());row.addProperty("adult",c.adult());
        var fixture=fixture(c);var target=DarkPowerMorphology071.sample(fixture);
        row.addProperty("first_type",c.firstType());row.addProperty("first_type_locked",fixture.adult);
        row.addProperty("locked_first_type",fixture.adult?fixture.lockedPrimaryType():-1);
        row.addProperty("prospective_first_type",fixture.adult?-1:fixture.prospectivePrimaryType());
        row.addProperty("locked_alignment",fixture.lockedAlignment());row.addProperty("dark_power_enabled",target.enabled());
        row.addProperty("expected_run_power",target.runPower());row.addProperty("expected_swim_fly",target.swimFly());
        var totals=new com.google.gson.JsonArray();for(int stat=0;stat<5;stat++)totals.add(HeroRunMorphology052.totalTraining(fixture,stat));row.add("current_training_totals",totals);
        row.addProperty("run_power_axis",c.runAxis());row.addProperty("swim_fly_axis",c.flyAxis());row.addProperty("swim_counter_percent",c.counterPercent());
        row.addProperty("static_native_epoch",c.photo()?STATIC_EPOCH:-1);row.addProperty("controlled_motion_training",!c.still()&&!c.action().equals("evolution")&&!c.pose());
        row.addProperty("training_percent",c.percent());row.addProperty("alignment",c.alignment());row.addProperty("maturity",c.maturity());
        row.addProperty("opposition_axis",c.oppositionAxis());row.addProperty("opposition_initial",c.oppositionInitial());
        row.addProperty("action",c.action());row.addProperty("hat",c.hat());row.addProperty("rabbit",c.rabbit());row.addProperty("coat",c.coat());row.addProperty("animal_wings",c.phoenix()?"phoenix":"");
        row.addProperty("suspended_static_photo",c.photo());row.addProperty("actual_accessories_required",c.carry());
        if(c.pose()){
          row.addProperty("pose_id",c.poseId());row.addProperty("preview_once_ticks",160);row.addProperty("preview_loop_start_tick",160);row.addProperty("preview_loop_wrap_tick",320);row.addProperty("preview_stop_tick",400);row.addProperty("preview_original_noai",true);
        }
        row.addProperty("angles",c.angleCount());row.addProperty("motion_ticks",c.still()?0:c.duration());rows.add(row);
      }
      var root=new com.google.gson.JsonObject();root.addProperty("kind","Actual production renderer in private Minecraft world; no alternate model or animation");
      root.addProperty("start",start());root.addProperty("limit_exclusive",limit());root.addProperty("audio_listening",false);root.add("cases",rows);
      Files.writeString(OUTPUT.resolve("case-catalog.json"),new com.google.gson.GsonBuilder().setPrettyPrinting().create().toJson(root)+"\n");
      LogUtils.getLogger().info("DARKPOWER071_CATALOG path={} start={} limitExclusive={} scenes={} productionRenderer=true normal20TPS=true",OUTPUT.resolve("case-catalog.json"),start(),limit(),CASES.size());
    }catch(java.io.IOException error){throw new IllegalStateException(error);}
  }
  private static long expectedSignature(ChaoState state){
    if(NeutralPowerMorphology063.sample(state).enabled()){
      if(expectedNeutralPowerMesh==null)expectedNeutralPowerMesh=ChaoMeshAsset.load("gameplay/chao_neutral_power_proto063");
      expectedNeutralPowerMesh.neutralPowerDevelopment(NeutralPowerAppearance063.sample(state));return expectedNeutralPowerMesh.shapeSignature();
    }
    if(NeutralFlyMorphology061.sample(state).enabled()){
      if(expectedNeutralFlyMesh==null)expectedNeutralFlyMesh=ChaoMeshAsset.load("gameplay/chao_neutral_fly_proto061");
      expectedNeutralFlyMesh.neutralFlyDevelopment(NeutralFlyAppearance061.sample(state));return expectedNeutralFlyMesh.shapeSignature();
    }
    if(NeutralSwimMorphology059.sample(state).enabled()){
      if(expectedNeutralSwimMesh==null)expectedNeutralSwimMesh=ChaoMeshAsset.load("gameplay/chao_neutral_swim_proto059");
      expectedNeutralSwimMesh.neutralSwimDevelopment(NeutralSwimAppearance059.sample(state));return expectedNeutralSwimMesh.shapeSignature();
    }
    if(DarkNormalMorphology060.sample(state).enabled()){
      if(expectedDarkNormalMesh==null)expectedDarkNormalMesh=ChaoMeshAsset.load("gameplay/chao_dark_normal_proto060");
      expectedDarkNormalMesh.darkNormalDevelopment(DarkNormalAppearance060.sample(state));return expectedDarkNormalMesh.shapeSignature();
    }
    if(HeroSwimMorphology064.sample(state).enabled()){
      if(expectedHeroSwimMesh==null)expectedHeroSwimMesh=ChaoMeshAsset.load("gameplay/chao_hero_swim_proto064");
      expectedHeroSwimMesh.heroSwimDevelopment(HeroSwimAppearance064.sample(state));return expectedHeroSwimMesh.shapeSignature();
    }
    if(HeroFlyMorphology065.sample(state).enabled()){
      if(expectedHeroFlyMesh==null)expectedHeroFlyMesh=ChaoMeshAsset.load("gameplay/chao_hero_fly_proto065");
      expectedHeroFlyMesh.heroFlyDevelopment(HeroFlyAppearance065.sample(state));return expectedHeroFlyMesh.shapeSignature();
    }
    if(HeroRunMorphology066.sample(state).enabled()){
      if(expectedHeroRunMesh==null)expectedHeroRunMesh=ChaoMeshAsset.load("gameplay/chao_hero_run_proto066");
      expectedHeroRunMesh.heroRunDevelopment066(HeroRunAppearance066.sample(state));return expectedHeroRunMesh.shapeSignature();
    }
    if(HeroPowerMorphology067.sample(state).enabled()){
      if(expectedHeroPowerMesh==null)expectedHeroPowerMesh=ChaoMeshAsset.load("gameplay/chao_hero_power_proto067");
      expectedHeroPowerMesh.heroPowerDevelopment067(HeroPowerAppearance067.sample(state));return expectedHeroPowerMesh.shapeSignature();
    }
    if(DarkPowerMorphology071.sample(state).enabled()){
      if(expectedDarkPowerMesh==null)expectedDarkPowerMesh=ChaoMeshAsset.load("gameplay/chao_dark_power_proto071");
      expectedDarkPowerMesh.darkPowerDevelopment071(DarkPowerAppearance071.sample(state));return expectedDarkPowerMesh.shapeSignature();
    }
    if(DarkRunMorphology070.sample(state).enabled()){
      if(expectedDarkRunMesh==null)expectedDarkRunMesh=ChaoMeshAsset.load("gameplay/chao_dark_run_proto070");
      expectedDarkRunMesh.darkRunDevelopment070(DarkRunAppearance070.sample(state));return expectedDarkRunMesh.shapeSignature();
    }
    if(DarkFlyMorphology069.sample(state).enabled()){
      if(expectedDarkFlyMesh==null)expectedDarkFlyMesh=ChaoMeshAsset.load("gameplay/chao_dark_fly_proto069");
      expectedDarkFlyMesh.darkFlyDevelopment069(DarkFlyAppearance069.sample(state));return expectedDarkFlyMesh.shapeSignature();
    }
    if(DarkSwimMorphology068.sample(state).enabled()){
      if(expectedDarkSwimMesh==null)expectedDarkSwimMesh=ChaoMeshAsset.load("gameplay/chao_dark_swim_proto068");
      expectedDarkSwimMesh.darkSwimDevelopment068(DarkSwimAppearance068.sample(state));return expectedDarkSwimMesh.shapeSignature();
    }
    if(NeutralRunMorphology062.sample(state).enabled()){
      if(expectedNeutralRunMesh==null)expectedNeutralRunMesh=ChaoMeshAsset.load("gameplay/chao_neutral_run_proto062");
      expectedNeutralRunMesh.neutralRunDevelopment(NeutralRunAppearance062.sample(state));return expectedNeutralRunMesh.shapeSignature();
    }
    if(HeroNormalMorphology057.sample(state).enabled()){
      if(expectedHeroNormalMesh==null)expectedHeroNormalMesh=ChaoMeshAsset.load("gameplay/chao_hero_normal_proto057");
      expectedHeroNormalMesh.heroNormalDevelopment(HeroNormalAppearance057.sample(state));return expectedHeroNormalMesh.shapeSignature();
    }
    if(NeutralMorphology056.sample(state).enabled()){
      if(expectedNeutralMesh==null)expectedNeutralMesh=ChaoMeshAsset.load("gameplay/chao_neutral_proto056");
      expectedNeutralMesh.neutralDevelopment(NeutralAppearance056.sample(state));return expectedNeutralMesh.shapeSignature();
    }
    if(expectedMesh==null)expectedMesh=ChaoMeshAsset.load("gameplay/chao_hero_run_proto055");
    float growth=state.gradualVisualStrength();expectedMesh.development(state.swimFly,state.runPower,state.adult?growth:growth*.14f);
    expectedMesh.heroRunDevelopment(HeroRunAppearance052.sample(state),state.swimFly,state.runPower);
    expectedMesh.heroRunTwoTone(state.baseColor()==0&&state.jewel()==0&&state.isTwoTone());
    return expectedMesh.shapeSignature();
  }
  private static OfficialChaoRig rig(ChaoModel model){
    try{var field=ChaoModel.class.getDeclaredField("official");field.setAccessible(true);return (OfficialChaoRig)field.get(model);}
    catch(ReflectiveOperationException error){throw new IllegalStateException(error);}
  }
  private static long signature(ChaoModel model){return rig(model).shapeSignature();}
  private static void toyContact(ChaoEntity c,ChaoModel model,float partial){
    var entry=CASES.get(phase);
    if(entry.floorToy()){
      if(!GardenToyPerformance049.active(c,entry.toyKind()))return;
    }else if(!ChaoQuietToyAnimation049.active(c))return;
    float t=GardenToyPerformance049.phase(c,partial),w=GardenToyPerformance049.presence(c,partial);if(t<16||t>=144)return;
    String kind=c.toyPlayingKind();float scale=c.state().adult?.8f:.64f;
    for(boolean left:new boolean[]{false,true}){
      var palm=model.toyPalm(left);require(palm!=null,"morph toy lost real palm");var actual=ChaoTriangleAnimation.palm(model::transformAccessoryAnchor,palm,left);
      Vector3f target;
      if(kind.equals(ActiveToyPlayRules049.CAR))target=left?ChaoActiveToyAnimation049.carSupportTarget(model::transformAccessoryAnchor,ChaoProneToyPose051.runtimeBounds(c,model))
          :ChaoActiveToyAnimation049.gripTarget(ChaoActiveToyAnimation049.propMatrix(model::transformAccessoryAnchor,kind,t,w,ACTIVE_TOY),kind,false);
      else if(kind.equals(QuietToyPlayRules049.CRAYONS)){
        var paper=ChaoQuietToyAnimation049.paperMatrix(model::transformAccessoryAnchor,w);
        target=left?ChaoQuietToyAnimation049.paperSupport(paper):ChaoQuietToyAnimation049.drawingHand(paper,t,QUIET.stroke(t),QUIET.pen(t));
      }else target=kind.equals(QuietToyPlayRules049.BROOM)?ChaoQuietToyAnimation049.broomTarget(ChaoQuietToyAnimation049.broomMatrix(w,QUIET.sweep(t),scale),left)
          :ChaoQuietToyAnimation049.bookTarget(ChaoQuietToyAnimation049.bookMatrix(model::transformAccessoryAnchor,w,ChaoProneToyPose051.runtimeBounds(c,model)),left);
      float error=actual.distance(target);MAX_CONTACT[phase]=Math.max(MAX_CONTACT[phase],error);CONTACTS[phase]++;require(error<(kind.equals(ActiveToyPlayRules049.CAR)?.01f:.025f),"morphed hand lost actual toy contact: "+kind+" error="+error);
    }
  }
  @SubscribeEvent public static void frameBegin(RenderFrameEvent.Pre event){if(enabled()){
    renderedThisFrame=worldThisFrame=poseRenderedHoldThisFrame=false;
    floorToyRenderedHoldThisFrame=false;floorToyRenderedPhaseThisFrame=-1;floorToyRenderedMotionThisFrame="NONE";
    var mc=Minecraft.getInstance();
    if(phase>=0&&phase<limit()&&CASES.get(phase).photo()){var frozen=observed(mc);if(frozen!=null)frozen.tickCount=STATIC_EPOCH;}
    // Native OS callbacks may have been queued after the tick-time request.
    // Replay the requested physical pointer just before rendering and observe
    // the real screen coordinates afterward; settled captures must be clear.
    if(mc.screen instanceof ChaoInspectionScreen&&Double.isFinite(requestedMouseX)&&Double.isFinite(requestedMouseY))cursorCallback(mc,requestedMouseX,requestedMouseY);
  }}
  @SubscribeEvent public static void rendered(RenderLivingEvent.Post<?,?> event){
    if(!enabled()||phase<0||phase>=limit()||SceneShaderPack.shadows()||!(event.getEntity() instanceof ChaoEntity c))return;
    var mc=Minecraft.getInstance();if(mc.screen!=null||mc.getOverlay()!=null||mc.getCameraEntity()!=observer)return;
    if(!c.getUUID().equals(actorId))return;
    var entry=CASES.get(phase);var model=(ChaoModel)event.getRenderer().getModel();require(model.officialPosesFinite(),"actual morphology pose is not finite");
    if(!entry.action().equals("evolution"))require(c.state().adult==entry.adult()&&!c.isCocooned(),"stage unexpectedly evolved or cocooned");
    if(entry.still()&&!c.state().encode().equals(expectedState))return;
    if(entry.adult())require(c.state().lockedPrimaryType()==entry.firstType()&&Math.abs(c.state().lockedAlignment()-entry.alignment())<.0001f,"actual client render changed locked first identity");
    renderedThisFrame=true;RENDERED[phase]++;
    if(c.isCocooned())COCOON[phase]++;else if(c.state().adult)AFTER[phase]++;else BEFORE[phase]++;
    if(!c.isCocooned()){
      for(String bone:List.of("root","body","skull","arm_left","arm_right","foot_left","foot_right")){
        var at=new PoseStack();model.transformAccessoryAnchor(bone,at);require(at.last().pose().isFinite(),"morph semantic anchor invalid: "+bone);
      }
      var eyes=model.accessoryEyes();if(eyes!=null)for(float value:eyes)require(Float.isFinite(value),"morph detached actual eye landmark");
      if(!entry.rabbit()&&!entry.action().equals("evolution")){
        for(boolean left:new boolean[]{false,true}){
          float[] hand=model.toyHandBounds(c.state(),left);require(hand!=null,"ordinary baby lost actual visible hand");
          compact(hand,"hand_"+(left?"left":"right"));
        }
        for(String bone:List.of("foot_left","foot_right")){var foot=model.accessoryBounds(bone);require(foot!=null,"ordinary baby lost foot "+bone);compact(foot,bone);}
      }
      long current=signature(model);
      if(entry.pair()&&elapsed>=280&&elapsed<=320&&!PAIR_DISPLAY_SETTLED[phase]){
        var displayed=DarkPowerAppearance071.sample(c.state());
        if(displayed.enabled()&&displayed.runPower()<-.98f&&displayed.swimFly()>.98f){
          PAIR_DISPLAY_SETTLED[phase]=true;LogUtils.getLogger().info("DARKPOWER071_PAIR_SETTLED case={} tick={} sample={} firstType={} epochMillis={}",phase,elapsed,displayed,c.state().lockedPrimaryType(),System.currentTimeMillis());
        }
      }
      if(DarkRunMorphology070.sample(c.state()).enabled()||DarkFlyMorphology069.sample(c.state()).enabled()||NeutralPowerMorphology063.sample(c.state()).enabled()||HeroSwimMorphology064.sample(c.state()).enabled()||HeroPowerMorphology067.sample(c.state()).enabled()||DarkPowerMorphology071.sample(c.state()).enabled()||DarkSwimMorphology068.sample(c.state()).enabled()||HeroRunMorphology066.sample(c.state()).enabled()||HeroFlyMorphology065.sample(c.state()).enabled()||NeutralFlyMorphology061.sample(c.state()).enabled()||NeutralSwimMorphology059.sample(c.state()).enabled()||DarkNormalMorphology060.sample(c.state()).enabled()||NeutralRunMorphology062.sample(c.state()).enabled()||HeroNormalMorphology057.sample(c.state()).enabled()||NeutralMorphology056.sample(c.state()).enabled()||HeroRunMorphology052.sample(c.state()).enabled())
        require(current==expectedSignature(c.state()),"production renderer did not sample the expected first-type-specific blocky asset");
      if(!entry.still()&&elapsed!=LAST_TRACE[phase]&&elapsed%60==0){LAST_TRACE[phase]=elapsed;LogUtils.getLogger().info("DARKPOWER071_MOTION_SAMPLE case={} tick={} shapeSignature={} sample={} babyVisibleGrowth={} epochMillis={}",phase,elapsed,current,sample(c.state()),HeroRunAppearance052.sample(c.state()).babyGrowth(),System.currentTimeMillis());}
      if(SIGNATURES[phase]==0){SIGNATURES[phase]=current;LogUtils.getLogger().info("DARKPOWER071_RENDER_MODEL {} shapeSignature={} geometry={} gameplay={} texture={} sample={}",phase,current,model.officialGeometryKey(),model.gameplayFormKey(),OfficialChaoRig.texture(c.state()),sample(c.state()));}
      if(c.walkAnimation.speed()>.01f)MOVING[phase]++;
      var actualRig=rig(model);
      if(actualRig.motion()==OfficialChaoRig.Motion.SWIM&&c.isInWater())SWIMMING[phase]++;
      if(actualRig.motion()==OfficialChaoRig.Motion.RUN||actualRig.motion()==OfficialChaoRig.Motion.DASH)RUNNING[phase]++;
      if(DarkPowerMorphology071.sample(c.state()).enabled())require(actualRig.geometryKey().equals("dark_power")&&actualRig.sourceGeometryKey().equals("baby"),"locked Dark Power identity lost canonical baby UV/attachment family");
      if(DarkPowerMorphology071.sample(c.state()).enabled())require(OfficialChaoRig.texture(c.state()).getPath().startsWith("dynamic/dark_power071/"),"new Dark Power material did not reach its scoped palette protection route");
      if(entry.pose()&&ToyPreview.poseId(c.toyPlayingKind()).equals(entry.poseId())){
        float posePhase=160-c.toyPlayingTicks()+Math.clamp(event.getPartialTick(),0,1);
        var expected=switch(entry.poseId()){case "crawl"->OfficialChaoRig.Motion.CRAWL;case "paddle"->OfficialChaoRig.Motion.PADDLE;case "swim"->OfficialChaoRig.Motion.SWIM;case "glide"->OfficialChaoRig.Motion.GLIDE;default->OfficialChaoRig.Motion.FLAIL;};
        if(posePhase>=22&&posePhase<138){
          require(actualRig.motion()==expected,"actual operator preview did not select its production motion");POSE_FRAMES[phase]++;POSE_HOLD_VIEWS[phase][view]++;poseRenderedHoldThisFrame=true;
          if(POSE_HOLD_VIEWS[phase][view]==1)LogUtils.getLogger().info("DARKPOWER071_POSE_RENDER case={} pose={} tick={} angle={} motion={} phase={} production=true epochMillis={}",phase,entry.poseId(),elapsed,MOTION_ANGLES[view],actualRig.motion(),posePhase,System.currentTimeMillis());
        }else if(posePhase<18){require(actualRig.motion()==OfficialChaoRig.Motion.WALK,"operator pose entry did not reuse standing motion");POSE_ENTRY_FRAMES[phase]++;}
        else if(posePhase>=142){require(actualRig.motion()==OfficialChaoRig.Motion.WALK,"operator pose exit did not reuse standing motion");POSE_EXIT_FRAMES[phase]++;}
      }
      if(entry.floorToy()&&GardenToyPerformance049.active(c,entry.toyKind())){
        float floorPhase=GardenToyPerformance049.phase(c,Math.clamp(event.getPartialTick(),0,1));
        require(Float.isFinite(floorPhase)&&floorPhase>=0&&floorPhase<=161,"actual floor toy phase is invalid");
        require(actualRig.motion()==OfficialChaoRig.Motion.WALK,"actual floor toy did not use its production WALK selection");
        floorToyRenderedPhaseThisFrame=floorPhase;floorToyRenderedMotionThisFrame=actualRig.motion().name();
        if(floorPhase>=22&&floorPhase<138){
          require(Math.abs(actualRig.pose("body").rx-ChaoProneToyPose051.BODY_ANGLE)<.0002f,"actual floor toy did not reach its production belly-down profile");
          FLOOR_TOY_FRAMES[phase]++;FLOOR_TOY_HOLD_VIEWS[phase][view]++;floorToyRenderedHoldThisFrame=true;
          if(FLOOR_TOY_HOLD_VIEWS[phase][view]==1)LogUtils.getLogger().info("DARKPOWER071_FLOOR_TOY_RENDER case={} toy={} kind={} tick={} angle={} motion={} phase={} bodyRx={} production=true epochMillis={}",phase,entry.toyId(),c.toyPlayingKind(),elapsed,MOTION_ANGLES[view],actualRig.motion(),floorPhase,actualRig.pose("body").rx,System.currentTimeMillis());
        }
      }
      if(entry.phoenix())require(c.state().animalParts[ChaoState.WINGS]>=0&&ChaoState.ANIMALS[c.state().animalParts[ChaoState.WINGS]].equals("phoenix"),"actual client lost Phoenix replacement-wing state");
      if(entry.photo())require(c.tickCount==STATIC_EPOCH,"static native rendering epoch changed");
      if(entry.toy())toyContact(c,model,Math.clamp(event.getPartialTick(),0,1));
    }
  }
  private static void compact(float[] bounds,String part){
    float min=Float.POSITIVE_INFINITY,max=0;
    for(int axis=0;axis<3;axis++){float extent=bounds[axis+3]-bounds[axis];require(Float.isFinite(extent)&&extent>.2f,"visible "+part+" flattened to "+Arrays.toString(bounds));min=Math.min(min,extent);max=Math.max(max,extent);}
    require(max/min<8,"visible "+part+" stretched disproportionately "+Arrays.toString(bounds));
  }
  @SubscribeEvent public static void renderedWorld(RenderLevelStageEvent event){
    if(!enabled()||phase<0||phase>=limit()||event.getStage()!=RenderLevelStageEvent.Stage.AFTER_LEVEL||SceneShaderPack.shadows())return;
    var mc=Minecraft.getInstance();if(mc.screen==null&&mc.getOverlay()==null&&mc.getCameraEntity()==observer&&renderedThisFrame)worldThisFrame=true;
  }
  private static void capture(Minecraft mc){
    // The server clock is shared: snapshot all capture identity exactly once.
    // Filename, pose-hold event and asynchronous PNG proof use this same tick.
    final int index=phase,captureView=view;final long at=elapsed;final int frame=WORLD[index];
    final boolean held=poseRenderedHoldThisFrame;var entry=CASES.get(index);
    final boolean floorHeld=floorToyRenderedHoldThisFrame;
    final float floorPhase=floorToyRenderedPhaseThisFrame;final String floorMotion=floorToyRenderedMotionThisFrame;
    final String label=(entry.photo()?STATIC_ANGLES:MOTION_ANGLES)[captureView];
    final String name=String.format("dark071-%s-%02d-%s-%s-t%03d-f%04d.png","live",index,entry.name(),label,at,frame);
    if(entry.pose()){
      if(held)POSE_HOLD_CAPTURES[index][captureView]++;
      LogUtils.getLogger().info("DARKPOWER071_POSE_CAPTURE case={} pose={} tick={} frame={} angle={} productionHold={} epochMillis={}",index,entry.poseId(),at,frame,label,held,System.currentTimeMillis());
    }
    if(entry.floorToy()){
      if(floorHeld)FLOOR_TOY_HOLD_CAPTURES[index][captureView]++;
      LogUtils.getLogger().info("DARKPOWER071_FLOOR_TOY_CAPTURE case={} toy={} tick={} frame={} angle={} productionHold={} productionPhase={} productionMotion={} epochMillis={}",index,entry.toyId(),at,frame,label,floorHeld,floorPhase,floorMotion,System.currentTimeMillis());
    }
    Screenshot.grab(OUTPUT.toFile(),name,mc.getMainRenderTarget(),message->{var file=OUTPUT.resolve("screenshots").resolve(name);require(Files.isRegularFile(file),"native screenshot missing");LogUtils.getLogger().info("DARKPOWER071_CAPTURE {} case={} angle={} tick={} sha256={} wallNanos={} epochMillis={}",name,index,label,at,sha256(file),System.nanoTime(),System.currentTimeMillis());SAVED.incrementAndGet();});shots++;SHOTS[index][captureView]++;
  }
  private static void finishCase(Minecraft mc){
    var entry=CASES.get(phase);require(RENDERED[phase]>25&&WORLD[phase]>25,"case lacked actual world/actor rendering");
    for(int i=0;i<entry.angleCount();i++)require(SHOTS[phase][i]>=(entry.still()?1:3),"case missed a camera angle");
    if(entry.pose()){
      require(POSE_ONCE_DONE[phase]&&POSE_LOOP_WRAPPED[phase]&&POSE_STOP_DONE[phase]&&POSE_SAVE_CHECKED[phase]&&POSE_FRAMES[phase]>30&&POSE_ENTRY_FRAMES[phase]>5&&POSE_EXIT_FRAMES[phase]>5,"actual production pose lifecycle, hold or standing transitions were not observed");
      for(int i=0;i<3;i++)require(POSE_HOLD_VIEWS[phase][i]>5&&POSE_HOLD_CAPTURES[phase][i]>=2,"actual production pose missed a rendered/captured held camera angle");
    }
    if(entry.floorToy()){
      require(FLOOR_TOY_ONCE_DONE[phase]&&FLOOR_TOY_FRAMES[phase]>30,"actual floor toy production hold or once cleanup was not observed");
      for(int i=0;i<3;i++)require(FLOOR_TOY_HOLD_VIEWS[phase][i]>=4&&FLOOR_TOY_HOLD_CAPTURES[phase][i]>=1,"actual floor toy missed a rendered/captured belly-down camera angle");
      LogUtils.getLogger().info("DARKPOWER071_FLOOR_TOY_RENDER_COMPLETE case={} toy={} heldFrames={} heldViews={} heldCaptures={} contacts={} maxToyError={} onceEndObserved=true epochMillis={}",phase,entry.toyId(),FLOOR_TOY_FRAMES[phase],Arrays.toString(FLOOR_TOY_HOLD_VIEWS[phase]).replace(" ",""),Arrays.toString(FLOOR_TOY_HOLD_CAPTURES[phase]).replace(" ",""),CONTACTS[phase],MAX_CONTACT[phase],System.currentTimeMillis());
    }
    if(entry.walk())require(MOVING[phase]>20,"production walk animation was not observed");
    if(entry.action().equals("run_walk"))require(RUNNING[phase]>20,"actual running gait was not observed");
    if(entry.swim())require(SWIMMING[phase]>20,"production advanced swimming was not observed in actual water");
    if(entry.toy())require(CONTACTS[phase]>30,"actual toy grip was never measured");
    if(entry.carry())require(CompanionBackLayer.renders()>carryBefore+20,"actual player carry layer never rendered");
    if(entry.ui())require(UI_VERIFIED[phase]&&UI_SHOTS[phase]>=2,"actual Analyzer drag/read-only/save checks missing");
    if(entry.pair())require(PAIR_DISPLAY_SETTLED[phase],"held pair endpoint was not actually rendered settled before reversal");
    if(entry.action().equals("evolution"))require(BEFORE[phase]>3&&COCOON[phase]>20&&AFTER[phase]>10,"actual before/cocoon/after transition not rendered");
    if(!entry.still())require((System.nanoTime()-playbackWall)/1e9>=entry.duration()/20.0-.65,"real playback ran faster than normal20TPS");
    LogUtils.getLogger().info("DARKPOWER071_RENDER_COMPLETE {} name={} baseline={} angles={} worldFrames={} actorFrames={} captures={} movingFrames={} toyContacts={} maxToyError={} uiFrames={} uiCaptures={} epochMillis={}",phase,entry.name(),baseline(),entry.angleCount(),WORLD[phase],RENDERED[phase],Arrays.stream(SHOTS[phase]).sum(),MOVING[phase],CONTACTS[phase],MAX_CONTACT[phase],UI_FRAMES[phase],UI_SHOTS[phase],System.currentTimeMillis());
    int done=phase;pendingPhase=phase;clientPhase=-2;onServer(mc.getSingleplayerServer(),()->{if(phase==done){if(entry.still()){require(actor.state().encode().equals(expectedState),"frozen stage changed server state");LogUtils.getLogger().info("DARKPOWER071_NATIVE_COMPLETE {} name={} baseline={} frozenStatePreserved=true state={} epochMillis={}",phase,entry.name(),baseline(),actor.state().encode(),System.currentTimeMillis());}if(phase+1<limit())next(mc.getSingleplayerServer(),phase+1);else{clearSlot();if(actor!=null){actor.discard();actor=null;}phase=limit();mc.getSingleplayerServer().tickRateManager().setFrozen(false);}}});
  }
  @SubscribeEvent public static void frameEnd(RenderFrameEvent.Post event){
    if(!enabled()||phase<0||phase>=limit()||clientPhase!=phase||!worldThisFrame)return;
    var mc=Minecraft.getInstance();var entry=CASES.get(phase);
    if(entry.ui()&&armed&&!uiWorldReady)return;WORLD[phase]++;viewFrames++;
    if(!armed){if(!armRequested&&viewFrames>=12){armRequested=true;int index=phase;onServer(mc.getSingleplayerServer(),()->arm(mc.getSingleplayerServer(),index));}return;}
    if(entry.still()){
      if(viewFrames>=16&&SHOTS[phase][view]<1)capture(mc);
      if(viewFrames>=23&&SHOTS[phase][view]>=1){if(view<entry.angleCount()-1){view++;viewFrames=0;}else finishCase(mc);}return;
    }
    if(elapsed>=6&&elapsed!=lastMotionCapture&&elapsed-lastMotionCapture>=12&&viewFrames>=3){lastMotionCapture=elapsed;capture(mc);}
    if(complete&&viewFrames>=10)finishCase(mc);
  }
  @SubscribeEvent public static void client(ClientTickEvent.Post event){
    if(!enabled()||finished)return;var mc=Minecraft.getInstance();ticks++;
    if(fatal!=null)throw new IllegalStateException("DARKPOWER071 server fixture failed: "+fatal);
    if(wallStarted==0)wallStarted=System.nanoTime();require(ticks<90000&&System.nanoTime()-wallStarted<3_600_000_000_000L,"actual client study timed out");
    if(mc.screen instanceof AccessibilityOnboardingScreen)mc.setScreen(new TitleScreen());
    if(!started&&mc.screen instanceof TitleScreen){
      started=true;mc.options.renderDistance().set(3);mc.options.simulationDistance().set(5);mc.options.framerateLimit().set(30);mc.options.fov().set(45);mc.options.pauseOnLostFocus=false;mc.options.hideGui=true;mc.options.tutorialStep=net.minecraft.client.tutorial.TutorialSteps.NONE;
      mc.options.guiScale().set(2);mc.getWindow().setWindowed(Math.clamp(parameter("CHAO_DARKPOWER071_WIDTH",1440),854,2560),Math.clamp(parameter("CHAO_DARKPOWER071_HEIGHT",900),480,1440));mc.resizeDisplay();
      mc.createWorldOpenFlows().createFreshLevel("DarkPower071-"+System.currentTimeMillis(),new LevelSettings("DarkPower071",GameType.CREATIVE,false,Difficulty.PEACEFUL,true,new GameRules(),WorldDataConfiguration.DEFAULT),new WorldOptions(2559L,false,false),r->r.registryOrThrow(Registries.WORLD_PRESET).getHolderOrThrow(WorldPresets.FLAT).value().createWorldDimensions(),mc.screen);
    }
    if(!setup&&mc.player!=null&&mc.getSingleplayerServer()!=null){
      // Evolution fixtures read server settings, which are loaded by player join.
      catalog();
      setup=true;mc.setScreen(null);ChaoRenderer.suppressNames=true;
      var window=mc.getWindow();LogUtils.getLogger().info("DARKPOWER071_WINDOW x={} y={} screenWidth={} screenHeight={} framebufferWidth={} framebufferHeight={} baseline={} epochMillis={}",window.getX(),window.getY(),window.getScreenWidth(),window.getScreenHeight(),window.getWidth(),window.getHeight(),baseline(),System.currentTimeMillis());
      onServer(mc.getSingleplayerServer(),()->{var server=mc.getSingleplayerServer();var level=server.overworld();level.setDayTime(6000);level.getGameRules().getRule(GameRules.RULE_DAYLIGHT).set(false,server);level.getGameRules().getRule(GameRules.RULE_DOMOBSPAWNING).set(false,server);
        for(var at:BlockPos.betweenClosed(-12,79,-12,48,86,12))level.setBlock(at,at.getY()==79?Blocks.SMOOTH_STONE.defaultBlockState():Blocks.AIR.defaultBlockState(),3);next(server,start());});
    }
    if(mc.level!=null&&mc.player!=null&&phase>=0&&phase<limit()){
      if(clientPhase==-2&&phase==pendingPhase)return;
      if(clientPhase!=phase){
        // Clear leftovers once at the fixture boundary: frozen water particles
        // must not persist over a later baby photo. Production effects run
        // normally throughout every motion, evolution and toy scene.
        mc.particleEngine.setLevel(mc.level);
        clientPhase=phase;view=viewFrames=0;armRequested=false;lastMotionCapture=-1;carryBefore=CompanionBackLayer.renders();
      }
      if(CASES.get(phase).ui())uiControl(mc);
      camera(mc);
    }
    if(phase==limit()&&SAVED.get()==shots){
      for(int i=start();i<limit();i++)require(SKIPPED[i]||WORLD[i]>25,"missing actual client case");
      int skipped=0;for(int i=start();i<limit();i++)if(SKIPPED[i])skipped++;
      finished=true;LogUtils.getLogger().info("DARKPOWER071_VISUAL_COMPLETE requestedCases={} renderedCases={} skippedCases={} start={} limit={} baseline={} captures={} saved={} epochMillis={} normal20TPS=true audioListening=false output={}",limit()-start(),limit()-start()-skipped,skipped,start(),limit(),baseline(),shots,SAVED.get(),System.currentTimeMillis(),OUTPUT);mc.stop();
    }
  }
}
