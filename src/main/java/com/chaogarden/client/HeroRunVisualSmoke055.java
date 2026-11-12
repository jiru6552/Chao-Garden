package com.chaogarden.client;

import com.chaogarden.ChaoGarden;
import com.chaogarden.GardenConfig;
import com.chaogarden.companion.AccessoriesBridge;
import com.chaogarden.companion.CompanionContent;
import com.chaogarden.core.ChaoState;
import com.chaogarden.core.AnalyzerStatRules;
import com.chaogarden.core.HeroRunMorphology052;
import com.chaogarden.entity.ChaoEntity;
import com.chaogarden.item.ChaoCarrierItem;
import com.chaogarden.toys.GardenToyPerformance049;
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

/** Opt-in approved blocky production morphology, native movement/toys and
 * actual Accessories-slot captures. Fixture training is deliberately accelerated;
 * playback retains the normal 20 TPS clock. No morphology rig is injected.
 * This entire helper and its nested classes are excluded from the production JAR. */
@EventBusSubscriber(modid=ChaoGarden.MODID,value=Dist.CLIENT)
public final class HeroRunVisualSmoke055 {
  private record Case(String name,boolean adult,int percent,float alignment,float maturity,String action,String hat,boolean rabbit,String coat) {
    boolean photo(){return action.startsWith("still")||action.equals("fresh");}
    boolean still(){return photo()||ui();}
    boolean ui(){return action.startsWith("ui_");}
    boolean carry(){return action.startsWith("carry");}
    boolean toy(){return action.equals("broom")||action.equals("picture_book");}
    boolean walk(){return action.contains("walk");}
    boolean gain(){return action.equals("gain_walk");}
    boolean loss(){return action.equals("loss_walk");}
    int angleCount(){return photo()?6:3;}
    int duration(){return toy()?180:carry()?140:action.equals("evolution")?260:gain()||loss()?280:240;}
  }
  private static List<Case> cases(){
    var result=new ArrayList<Case>();
    for(float alignment:new float[]{0,1})for(int percent:new int[]{0,25,50,75,100})
      result.add(new Case("baby_"+(alignment==0?"neutral":"hero")+"_run_p"+String.format("%03d",percent),false,percent,alignment,1,"still","",false,"natural"));
    result.add(new Case("baby_hero_run100_age000",false,100,1,0,"still","",false,"natural"));
    result.add(new Case("adult_fresh_actual_first_evolution",true,50,1,0,"fresh","",false,"natural"));
    for(int percent:new int[]{0,25,50,75,100})
      result.add(new Case("adult_hero_run_p"+String.format("%03d",percent)+"_age100",true,percent,1,1,"still","",false,"natural"));
    result.add(new Case("adult_hero_run100_age050",true,100,1,.5f,"still","",false,"natural"));
    result.add(new Case("adult_hero_run100_compatible_swim",true,100,1,1,"still_swim","",false,"natural"));
    result.add(new Case("adult_hero_run100_compatible_fly",true,100,1,1,"still_fly","",false,"natural"));
    result.add(new Case("creative_alignment_drag_and_reload",false,100,0,.8f,"ui_creative","",false,"natural"));
    result.add(new Case("baby_red_shiny_rabbit_skull",false,100,1,1,"still","skull",true,"red_shiny"));
    result.add(new Case("adult_ruby_phoenix_glasses",true,100,1,1,"still_wings_glasses","",true,"ruby"));
    result.add(new Case("baby_hero_training_walk",false,0,1,.8f,"gain_walk","",false,"natural"));
    result.add(new Case("adult_hero_training_walk",true,0,1,1,"gain_walk","",false,"natural"));
    result.add(new Case("adult_hero_receding_walk",true,100,1,1,"loss_walk","",false,"natural"));
    result.add(new Case("baby_hero_broom",false,100,1,.8f,"broom","",false,"natural"));
    result.add(new Case("adult_hero_picture_book",true,100,1,1,"picture_book","",false,"natural"));
    result.add(new Case("baby_equipped_flying",false,100,1,.8f,"carry_flying","",false,"natural"));
    result.add(new Case("adult_equipped_flying",true,100,1,1,"carry_flying","",false,"natural"));
    return List.copyOf(result);
  }
  private static final List<Case> CASES=cases();
  private static final int[][] SHOTS=new int[CASES.size()][6];
  private static final int[] WORLD=new int[CASES.size()],RENDERED=new int[CASES.size()],MOVING=new int[CASES.size()],COCOON=new int[CASES.size()],BEFORE=new int[CASES.size()],AFTER=new int[CASES.size()],CONTACTS=new int[CASES.size()];
  private static final long[] SIGNATURES=new long[CASES.size()];
  private static final float[] MAX_CONTACT=new float[CASES.size()];
  private static final int[] UI_FRAMES=new int[CASES.size()],UI_SHOTS=new int[CASES.size()];
  private static final boolean[] UI_VERIFIED=new boolean[CASES.size()];
  private static final AtomicInteger SAVED=new AtomicInteger();
  private static final LionfishQuietToyClip049 QUIET=new LionfishQuietToyClip049();
  private static boolean enabled(){return "true".equalsIgnoreCase(System.getenv("CHAO_RUN055_SMOKE"));}
  private static final java.nio.file.Path OUTPUT=java.nio.file.Path.of(System.getenv().getOrDefault("CHAO_RUN055_OUTPUT","/workspace/.setup/run055-client")).toAbsolutePath().normalize();
  private static int parameter(String name,int fallback){String value=System.getenv(name);return value==null?fallback:Integer.parseInt(value);}
  private static ChaoMeshAsset expectedMesh;
  private static final boolean[] SKIPPED=new boolean[CASES.size()];
  private static final long[] LAST_TRACE=new long[CASES.size()];
  private static boolean baseline(){return false;}
  private static final String[] STATIC_ANGLES={"front","left","right","back","above","below"};
  private static final String[] MOTION_ANGLES={"front","left","back"};
  private static int limit(){return Math.clamp(parameter("CHAO_RUN055_LIMIT",CASES.size()),1,CASES.size());}
  private static int start(){return Math.clamp(parameter("CHAO_RUN055_START",0),0,limit()-1);}
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
  private static int uiStep,uiTicks,uiBaseFrames;
  private static boolean uiOpened,uiFinished,uiFreezeRequested;
  private static volatile boolean uiWorldReady;
  private static float uiLockedAlignment;

  private static void require(boolean okay,String message){if(!okay)throw new IllegalStateException("HERO055 "+message);}
  private static void onServer(MinecraftServer server,Runnable task){server.execute(()->{try{task.run();}catch(Throwable error){fatal=error.toString();LogUtils.getLogger().error("HERO055_ASYNC_FAILURE",error);}});}
  private static void markSlot(){try{containerType.getMethod("markChanged",boolean.class).invoke(accessoryContainer,false);}catch(ReflectiveOperationException error){throw new IllegalStateException(error);}}
  private static void clearSlot(){if(slot!=null){slot.setItem(0,ItemStack.EMPTY);markSlot();slot=null;accessoryContainer=null;}}

  private static String sha256(java.nio.file.Path file){try{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(file)));}catch(Exception error){throw new IllegalStateException(error);}}

  /** Exact stage seeds use the real training operation and existing maturity setter.
   * No mesh, animation pose, skin or attachment is injected by this helper. */
  private static ChaoState fixture(Case entry){
    var state=new ChaoState();state.energy=state.hunger=100;state.happiness=80;state.boredom=0;
    state.adult=entry.adult();state.align=entry.alignment();
    if(entry.adult())state.primaryType=ChaoState.RUN_TYPE;
    state.setEvolutionStrength(entry.adult()?entry.maturity()*GradualEvolutionRules.ADULT_STRENGTH_MAX:entry.maturity());
    int total=Math.round((entry.adult()?HeroRunMorphology052.ADULT_RUN_PROGRESS:HeroRunMorphology052.BABY_RUN_PROGRESS)*entry.percent()/100f);
    state.train(ChaoState.RUN,total,new Random(5353));
    if(entry.action().equals("fresh")){
      state=new ChaoState();state.energy=state.hunger=100;state.happiness=80;state.align=1;
      state.train(ChaoState.RUN,Math.round(HeroRunMorphology052.BABY_RUN_PROGRESS),new Random(5252));state.ageTicks=999;state.setEvolutionStrength(.99f);
      require(state.tick(1000,Integer.MAX_VALUE,new Random(5252))==ChaoState.Transition.EVOLVED,"fresh fixture did not use actual first evolution");
      while(state.cocoonTicks>0)state.tick(1000,Integer.MAX_VALUE,new Random(5252));
      require(state.adult&&state.lockedPrimaryType()==ChaoState.RUN_TYPE&&state.lockedAlignment()>=.5f,"fresh evolution selected another identity");
    }
    if(entry.action().equals("evolution")){state.ageTicks=GardenConfig.CHILD_TICKS.get()-12;state.setEvolutionStrength(.99f);}
    if(entry.action().equals("still_swim"))state.swimFly=-1;
    if(entry.action().equals("still_fly"))state.swimFly=1;
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
    if(entry.coat().equals("red_shiny")){state.shiny=true;state.shinyGenes=new int[]{1,1};}
    if(entry.action().equals("still_wings_glasses")){
      int phoenix=Arrays.asList(ChaoState.ANIMALS).indexOf("phoenix");require(phoenix>=0,"registered phoenix DNA absent");
      state.animalParts[ChaoState.WINGS]=phoenix;state.animalMask|=1<<phoenix;state.animalPartsMask|=1<<phoenix;
    }
    // Serialization initializes the genuine adult identity lock and proves no new schema is needed.
    // Photo fixtures at baby maturity1 stay server-frozen; they do not claim
    // that a naturally ticking baby can remain unevolved at that endpoint.
    String encoded=state.encode();var decoded=ChaoState.decode(encoded);require(decoded.encode().equals(encoded),"stage state did not round trip exactly");return decoded;
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
      SKIPPED[index]=true;LogUtils.getLogger().warn("HERO055_SKIP case={} name={} reason=Actual Accessories runtime unavailable; equipped scene was not rendered or verified",index,CASES.get(index).name());index++;
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
    if(entry.action().equals("carry_flying")){player.getAbilities().mayfly=true;player.getAbilities().flying=true;player.onUpdateAbilities();player.teleportTo(.5,82,.5);}
    actor=ChaoGarden.CHAO.get().create(level);require(actor!=null,"Chao fixture creation failed");actor.owner=player.getUUID();actor.setState(fixture(entry));
    actor.setNoAi(!entry.walk());actor.setInvulnerable(true);actor.setOnGround(true);actor.setNoGravity(entry.photo());actor.deferBehavior(100000);
    Vec3 initialActorAt=entry.photo()?ORIGIN.add(0,2,0):ORIGIN;
    actor.moveTo(initialActorAt.x,initialActorAt.y,initialActorAt.z,0,0);actor.yBodyRot=actor.yBodyRotO=actor.yHeadRot=actor.yHeadRotO=0;
    actor.goalSelector.removeAllGoals(goal->true);actor.targetSelector.removeAllGoals(goal->true);
    if(!entry.hat().isEmpty())actor.setItemSlot(EquipmentSlot.HEAD,new ItemStack(switch(entry.hat()){case "pumpkin"->Items.CARVED_PUMPKIN;case "melon"->Items.MELON;default->Items.SKELETON_SKULL;}));
    if(entry.action().equals("still_wings_glasses"))actor.setItemSlot(EquipmentSlot.OFFHAND,new ItemStack(ChaoGarden.item("round_glasses_accessory")));
    var saved=new CompoundTag();actor.saveWithoutId(saved);String before=actor.state().encode();actor.load(saved);
    require(actor.state().encode().equals(before),"entity save/reload changed prototype state");
    require(level.addFreshEntity(actor),"stage Chao did not enter actual level");actorId=actor.getUUID();expectedState=before;
    if(entry.carry())equip(player);
    phase=index;elapsed=0;serverTime=began=lastTick=level.getGameTime();previousPosition=new Vec3(.5,80,.5);walked=0;
    armed=complete=false;playbackWall=System.nanoTime();initialMaturity=HeroRunMorphology052.sample(fixture(entry)).maturity();
    uiStep=uiTicks=uiBaseFrames=0;uiOpened=uiFinished=uiFreezeRequested=uiWorldReady=false;uiLockedAlignment=actor==null?entry.alignment():actor.state().lockedAlignment();
    if(entry.carry())server.tickRateManager().setFrozen(false);
    LogUtils.getLogger().info("HERO055_CASE {} name={} baseline={} adult={} percent={} alignment={} maturity={} action={} coat={} suspendedStatic={} sample={} babyVisibleGrowth={} state={} epochMillis={} actualAccessories={}",index,entry.name(),baseline(),entry.adult(),entry.percent(),entry.alignment(),entry.maturity(),entry.action(),entry.coat(),entry.photo(),HeroRunMorphology052.sample(ChaoState.decode(expectedState)),HeroRunMorphology052.sample(ChaoState.decode(expectedState)).babyGrowth(),expectedState,System.currentTimeMillis(),entry.carry());
  }
  private static void command(MinecraftServer server,String toy,String mode){
    try{var player=server.getPlayerList().getPlayers().getFirst();require(server.getCommands().getDispatcher().execute("chao testtoy "+toy+" "+mode,player.createCommandSourceStack().withPermission(2))==1,"native toy preview dispatcher rejected "+toy);}
    catch(com.mojang.brigadier.exceptions.CommandSyntaxException error){throw new IllegalStateException(error);}
  }
  private static void arm(MinecraftServer server,int index){
    if(index!=phase||armed)return;var entry=CASES.get(index);
    if(entry.carry()){
      var actual=CompanionContent.findEntity(server,actorId);require(actual instanceof ChaoEntity,"actual equipped slot did not project a body");actor=(ChaoEntity)actual;expectedState=actor.state().encode();
    }
    require(actor!=null&&!actor.isRemoved(),"active fixture missing");initialMaturity=HeroRunMorphology052.sample(actor.state()).maturity();
    if(entry.toy())command(server,entry.action(),"once");
    began=lastTick=serverTime=server.overworld().getGameTime();elapsed=0;playbackWall=System.nanoTime();armed=true;
    require(Math.abs(server.tickRateManager().tickrate()-20)<.00001,"study must retain normal 20 TPS");
    if(!entry.still()||entry.ui())server.tickRateManager().setFrozen(false);
    LogUtils.getLogger().info("HERO055_ARMED {} epochMillis={} normal20TPS=true fixtureTrainingAccelerated={}",phase,System.currentTimeMillis(),entry.gain()||entry.loss());
  }
  @SubscribeEvent public static void server(ServerTickEvent.Post event){
    if(!enabled()||phase<0||phase>=limit())return;
    var server=event.getServer();var level=server.overworld();long now=level.getGameTime();
    if(!armed||complete||now==lastTick)return;lastTick=serverTime=now;elapsed=now-began;
    var entry=CASES.get(phase);require(actor!=null&&!actor.isRemoved(),"actual running actor disappeared");
    if(entry.ui())return;
    if(entry.walk()){
      actor.getNavigation().moveTo(40.5,80,.5,.7);
      walked+=actor.position().subtract(previousPosition).horizontalDistance();previousPosition=actor.position();
    }
    if(entry.gain()&&elapsed<=240&&elapsed%2==0){
      actor.state().train(ChaoState.RUN,entry.adult()?8:4,new Random(5353+elapsed));actor.sync();
      require(Math.abs(actor.state().lockedAlignment()-entry.alignment())<.0001,"Run training changed independent alignment");
    }
    if(entry.loss()&&elapsed<=240&&elapsed%2==0){
      int target=Math.max(0,(entry.adult()?960:480)-(int)(elapsed/2)*(entry.adult()?8:4)),targetLevel=target/100,progress=target%100;
      while(actor.state().levels[ChaoState.RUN]>targetLevel)require(AnalyzerStatRules.apply(actor.state(),ChaoState.RUN,AnalyzerStatRules.LEVEL,-1),"actual Analyzer level decrease rejected");
      require(AnalyzerStatRules.apply(actor.state(),ChaoState.RUN,AnalyzerStatRules.PROGRESS,progress),"actual Analyzer progress decrease rejected");actor.sync();
    }
    if(entry.action().equals("expression_walk")&&(elapsed==1||elapsed==80||elapsed==160)){
      for(int emotion:new int[]{com.chaogarden.core.ChaoEmotion.JOY,com.chaogarden.core.ChaoEmotion.ANGER,com.chaogarden.core.ChaoEmotion.SURPRISE})actor.state().setEmotion(emotion,0);
      int emotion=elapsed<80?com.chaogarden.core.ChaoEmotion.JOY:elapsed<160?com.chaogarden.core.ChaoEmotion.SURPRISE:com.chaogarden.core.ChaoEmotion.ANGER;
      actor.state().setEmotion(emotion,200);actor.sync();LogUtils.getLogger().info("HERO055_EXPRESSION case={} tick={} emotion={}",phase,elapsed,emotion);
    }
    if(entry.carry()){
      var player=server.getPlayerList().getPlayers().getFirst();
      require(CompanionContent.isManaged(actor),"actual Accessories body stopped being managed");
      if(elapsed>30)require(actor.companionCarryPosition()==(entry.action().equals("carry_flying")?2:1),"actual companion selected wrong carry attachment");
      require(player.getAbilities().flying==entry.action().equals("carry_flying"),"player flight fixture changed");
    }
    if(elapsed>=entry.duration()){
      if(entry.walk())require(walked>.8,"motion fixture did not really navigate");
      if(entry.toy())require(!ToyPreview.active(actor)&&actor.toyPlayingTicks()==0&&actor.toyTarget()==null,"actual toy preview failed cleanup");
      var sample=HeroRunMorphology052.sample(actor.state());
      if(entry.gain())require((entry.adult()?sample.specialization():sample.babyRun())>.99f&&Math.abs(actor.state().lockedAlignment()-entry.alignment())<.0001,"accelerated Run training did not reach independent endpoint");
      if(entry.loss())require((entry.adult()?sample.specialization():sample.babyRun())<.001f&&sample.maturity()>=initialMaturity,"decrease left Run traits or reduced biological maturity");
      if(entry.action().equals("maturity_walk"))require(sample.maturity()>initialMaturity,"independent biological maturity clock did not advance");
      if(entry.action().equals("evolution"))require(actor.state().adult&&actor.state().lockedPrimaryType()==ChaoState.RUN_TYPE&&actor.state().lockedAlignment()>=.5f&&!actor.isCocooned(),"actual first evolution did not finish as Hero Run");
      complete=true;server.tickRateManager().setFrozen(true);
      LogUtils.getLogger().info("HERO055_NATIVE_COMPLETE {} name={} baseline={} elapsed={} walked={} sample={} state={} epochMillis={}",phase,entry.name(),baseline(),elapsed,walked,sample,actor.state().encode(),System.currentTimeMillis());
    }
  }
  private static ChaoEntity observed(Minecraft mc){
    if(mc.level==null||actorId==null)return null;
    for(var entity:mc.level.entitiesForRendering())if(entity instanceof ChaoEntity c&&c.getUUID().equals(actorId))return c;return null;
  }
  private static void press(ChaoInspectionScreen screen,String label){
    for(var widget:screen.children())if(widget instanceof AbstractButton button&&button.visible&&button.getMessage().getString().equals(label)){
      require(button.active,"inactive Analyzer button "+label);
      require(screen.mouseClicked(screen.screenX(button.getX()+button.getWidth()/2f),screen.screenY(button.getY()+button.getHeight()/2f),0),"Analyzer navigation did not consume "+label);return;
    }throw new IllegalStateException("HERO055 missing Analyzer button "+label);
  }
  private static double barX(ChaoInspectionScreen screen,float alignment){return screen.screenX(398+162*(alignment+1)/2);}
  private static double barY(ChaoInspectionScreen screen){return screen.screenY(282);}
  private static void cursor(Minecraft mc,double x,double y){
    org.lwjgl.glfw.GLFW.glfwSetCursorPos(mc.getWindow().getWindow(),x*mc.getWindow().getScreenWidth()/mc.getWindow().getGuiScaledWidth(),y*mc.getWindow().getScreenHeight()/mc.getWindow().getGuiScaledHeight());
  }
  private static void beginDrag(Minecraft mc,ChaoInspectionScreen screen,float alignment,boolean editable){
    double x=barX(screen,alignment),y=barY(screen);cursor(mc,x,y);
    boolean consumed=screen.mouseClicked(x,y,0);if(editable)require(consumed,"Creative baby alignment bar did not start drag");
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
    String name=String.format("hero055-%s-%02d-%s-ui-%s.png","live",phase,CASES.get(phase).name(),label);int index=phase;
    Screenshot.grab(OUTPUT.toFile(),name,mc.getMainRenderTarget(),message->{require(Files.isRegularFile(OUTPUT.resolve("screenshots").resolve(name)),"Analyzer screenshot missing");SAVED.incrementAndGet();LogUtils.getLogger().info("HERO055_UI_CAPTURE {} case={} sha256={} epochMillis={}",name,index,sha256(OUTPUT.resolve("screenshots").resolve(name)),System.currentTimeMillis());});shots++;UI_SHOTS[phase]++;
  }
  private static void freezeUiResult(Minecraft mc){
    if(uiFreezeRequested)return;uiFreezeRequested=true;mc.setScreen(null);mc.options.hideGui=true;view=viewFrames=0;
    onServer(mc.getSingleplayerServer(),()->{
      var entry=CASES.get(phase);var player=mc.getSingleplayerServer().getPlayerList().getPlayers().getFirst();
      require(player.getMainHandItem().is(ChaoGarden.item("chao_analyzer")),"real Analyzer was lost during edit");
      float alignment=actor.state().lockedAlignment();boolean creative=entry.action().equals("ui_creative");
      if(creative)require(!actor.state().adult&&alignment>.75f&&alignment<.85f,"actual Creative drag did not arrive authoritatively");
      else require(Math.abs(alignment-uiLockedAlignment)<.00001f,"read-only or forged alignment request changed server state");
      require(player.isCreative()!=entry.action().equals("ui_survival"),"server player permission fixture changed");
      require(HeroRunMorphology052.totalTraining(actor.state(),ChaoState.RUN)==(entry.adult()?960:480),"alignment control changed Run training");
      if(entry.adult())require(actor.state().lockedPrimaryType()==ChaoState.RUN_TYPE&&alignment>=.5f,"adult first evolution identity changed");
      mc.getSingleplayerServer().tickRateManager().setFrozen(true);
      String before=actor.state().encode();var saved=new CompoundTag();actor.saveWithoutId(saved);actor.load(saved);
      require(actor.state().encode().equals(before),"alignment edit did not survive actual entity save/reload");actor.sync();expectedState=before;UI_VERIFIED[phase]=true;uiWorldReady=true;
      LogUtils.getLogger().info("HERO055_UI_NATIVE_VERIFIED case={} creative={} adult={} alignment={} runTotal={} saveReloadExact=true forgedDenied={} epochMillis={}",phase,creative,entry.adult(),alignment,HeroRunMorphology052.totalTraining(actor.state(),ChaoState.RUN),!creative,System.currentTimeMillis());
    });
  }
  /** Exercise the real entity-interaction event, screen navigation and drag request path. */
  private static void uiControl(Minecraft mc){
    var entry=CASES.get(phase);if(!entry.ui()||!armed||uiWorldReady||uiFreezeRequested)return;
    var c=observed(mc);if(c==null||mc.player==null)return;
    boolean creative=entry.action().equals("ui_creative"),survival=entry.action().equals("ui_survival");
    if(!uiOpened){
      if(mc.player.isCreative()==survival||!mc.player.getMainHandItem().is(ChaoGarden.item("chao_analyzer")))return;
      var interaction=new PlayerInteractEvent.EntityInteract(mc.player,InteractionHand.MAIN_HAND,c);NeoForge.EVENT_BUS.post(interaction);
      require(mc.screen instanceof ChaoInspectionScreen,"real Analyzer interaction did not open its screen");
      var screen=(ChaoInspectionScreen)mc.screen;press(screen,"MIND");mc.options.hideGui=false;
      uiOpened=true;uiTicks=0;uiBaseFrames=UI_FRAMES[phase];
      LogUtils.getLogger().info("HERO055_UI_OPEN case={} actualCreative={} adult={} realAnalyzer=true realEntityInteract=true epochMillis={}",phase,mc.player.isCreative(),entry.adult(),System.currentTimeMillis());return;
    }
    require(mc.screen instanceof ChaoInspectionScreen,"Analyzer closed before its real drag checks finished");
    if(UI_FRAMES[phase]-uiBaseFrames<10)return;uiTicks++;
    require(uiTicks<180,"Analyzer request/sync wait timed out at step "+uiStep);
    var screen=(ChaoInspectionScreen)mc.screen;
    if(creative){
      switch(uiStep){
        case 0->{captureUi(mc,"initial-neutral");beginDrag(mc,screen,0,true);uiStep=1;uiTicks=0;}
        case 1->{drag(mc,screen,0,-.8f,uiTicks);if(uiTicks>=8){release(mc,screen,-.8f);uiStep=2;uiTicks=0;}}
        case 2->{if(uiTicks>=30&&c.state().lockedAlignment()<-.75f){captureUi(mc,"dark");beginDrag(mc,screen,-.8f,true);uiStep=3;uiTicks=0;}}
        case 3->{drag(mc,screen,-.8f,0,uiTicks);if(uiTicks>=8){release(mc,screen,0);uiStep=4;uiTicks=0;}}
        case 4->{if(uiTicks>=30&&Math.abs(c.state().lockedAlignment())<.04f){captureUi(mc,"neutral");beginDrag(mc,screen,0,true);uiStep=5;uiTicks=0;}}
        case 5->{drag(mc,screen,0,.8f,uiTicks);if(uiTicks>=8){release(mc,screen,.8f);uiStep=6;uiTicks=0;}}
        case 6->{if(uiTicks>=35&&c.state().lockedAlignment()>.75f){captureUi(mc,"hero");freezeUiResult(mc);}}
        default->throw new IllegalStateException("HERO055 invalid UI step");
      }
    }else{
      switch(uiStep){
        case 0->{captureUi(mc,entry.adult()?"adult-locked":"survival-readonly");beginDrag(mc,screen,entry.alignment(),false);uiStep=1;uiTicks=0;}
        case 1->{drag(mc,screen,entry.alignment(),entry.adult()?-1:1,uiTicks);if(uiTicks>=8){release(mc,screen,entry.adult()?-1:1);ChaoCareNetwork.requestAlignment(c,entry.adult()?-1000:1000);uiStep=2;uiTicks=0;LogUtils.getLogger().info("HERO055_UI_FORGED_REQUEST case={} creative={} adult={} target={}",phase,mc.player.isCreative(),entry.adult(),entry.adult()?-1000:1000);}}
        case 2->{if(uiTicks>=40){require(Math.abs(c.state().lockedAlignment()-uiLockedAlignment)<.00001f,"server rejection was not synchronized");captureUi(mc,"after-forged-rejection");freezeUiResult(mc);}}
        default->throw new IllegalStateException("HERO055 invalid read-only UI step");
      }
    }
  }
  @SubscribeEvent public static void uiRendered(ScreenEvent.Render.Post event){
    if(enabled()&&phase>=0&&phase<limit()&&CASES.get(phase).ui()&&event.getScreen() instanceof ChaoInspectionScreen)UI_FRAMES[phase]++;
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
        row.addProperty("training_percent",c.percent());row.addProperty("alignment",c.alignment());row.addProperty("maturity",c.maturity());
        row.addProperty("action",c.action());row.addProperty("hat",c.hat());row.addProperty("rabbit",c.rabbit());row.addProperty("coat",c.coat());
        row.addProperty("suspended_static_photo",c.photo());row.addProperty("actual_accessories_required",c.carry());
        row.addProperty("angles",c.angleCount());row.addProperty("motion_ticks",c.still()?0:c.duration());rows.add(row);
      }
      var root=new com.google.gson.JsonObject();root.addProperty("kind","Actual production renderer in private Minecraft world; no alternate model or animation");
      root.addProperty("start",start());root.addProperty("limit_exclusive",limit());root.addProperty("audio_listening",false);root.add("cases",rows);
      Files.writeString(OUTPUT.resolve("case-catalog.json"),new com.google.gson.GsonBuilder().setPrettyPrinting().create().toJson(root)+"\n");
      LogUtils.getLogger().info("HERO055_CATALOG path={} start={} limitExclusive={} scenes={} productionRenderer=true normal20TPS=true",OUTPUT.resolve("case-catalog.json"),start(),limit(),CASES.size());
    }catch(java.io.IOException error){throw new IllegalStateException(error);}
  }
  private static long expectedSignature(ChaoState state){
    if(expectedMesh==null)expectedMesh=ChaoMeshAsset.load("gameplay/chao_hero_run_proto055");
    float growth=state.gradualVisualStrength();expectedMesh.development(state.swimFly,state.runPower,state.adult?growth:growth*.14f);
    expectedMesh.heroRunDevelopment(HeroRunAppearance052.sample(state),state.swimFly,state.runPower);
    expectedMesh.heroRunTwoTone(state.baseColor()==0&&state.jewel()==0&&state.isTwoTone());
    return expectedMesh.shapeSignature();
  }
  private static long signature(ChaoModel model){
    try{var field=ChaoModel.class.getDeclaredField("official");field.setAccessible(true);return ((OfficialChaoRig)field.get(model)).shapeSignature();}
    catch(ReflectiveOperationException error){throw new IllegalStateException(error);}
  }
  private static void toyContact(ChaoEntity c,ChaoModel model,float partial){
    if(!ChaoQuietToyAnimation049.active(c))return;float t=GardenToyPerformance049.phase(c,partial),w=GardenToyPerformance049.presence(c,partial);if(t<16||t>=144)return;
    String kind=c.toyPlayingKind();float scale=c.state().adult?.8f:.64f;
    for(boolean left:new boolean[]{false,true}){
      var palm=model.toyPalm(left);require(palm!=null,"morph toy lost real palm");var actual=ChaoTriangleAnimation.palm(model::transformAccessoryAnchor,palm,left);
      var target=kind.equals(QuietToyPlayRules049.BROOM)?ChaoQuietToyAnimation049.broomTarget(ChaoQuietToyAnimation049.broomMatrix(w,QUIET.sweep(t),scale),left)
          :ChaoQuietToyAnimation049.bookTarget(ChaoQuietToyAnimation049.bookMatrix(model::transformAccessoryAnchor,w,ChaoProneToyPose051.runtimeBounds(c,model)),left);
      float error=actual.distance(target);MAX_CONTACT[phase]=Math.max(MAX_CONTACT[phase],error);CONTACTS[phase]++;require(error<.025f,"morphed hand lost actual toy contact: "+kind+" error="+error);
    }
  }
  @SubscribeEvent public static void frameBegin(RenderFrameEvent.Pre event){if(enabled())renderedThisFrame=worldThisFrame=false;}
  @SubscribeEvent public static void rendered(RenderLivingEvent.Post<?,?> event){
    if(!enabled()||phase<0||phase>=limit()||SceneShaderPack.shadows()||!(event.getEntity() instanceof ChaoEntity c))return;
    var mc=Minecraft.getInstance();if(mc.screen!=null||mc.getOverlay()!=null||mc.getCameraEntity()!=observer)return;
    if(!c.getUUID().equals(actorId))return;
    var entry=CASES.get(phase);var model=(ChaoModel)event.getRenderer().getModel();require(model.officialPosesFinite(),"actual morphology pose is not finite");
    if(!entry.action().equals("evolution"))require(c.state().adult==entry.adult()&&!c.isCocooned(),"stage unexpectedly evolved or cocooned");
    if(entry.still()&&!c.state().encode().equals(expectedState))return;
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
      long current=signature(model);require(current==expectedSignature(c.state()),"production renderer did not sample blocky055 asset");
      if(!entry.still()&&elapsed!=LAST_TRACE[phase]&&elapsed%60==0){LAST_TRACE[phase]=elapsed;LogUtils.getLogger().info("HERO055_MOTION_SAMPLE case={} tick={} shapeSignature={} sample={} babyVisibleGrowth={} epochMillis={}",phase,elapsed,current,HeroRunAppearance052.sample(c.state()),HeroRunAppearance052.sample(c.state()).babyGrowth(),System.currentTimeMillis());}
      if(SIGNATURES[phase]==0){SIGNATURES[phase]=current;LogUtils.getLogger().info("HERO055_RENDER_MODEL {} shapeSignature={} geometry={} gameplay={} texture={} sample={}",phase,current,model.officialGeometryKey(),model.gameplayFormKey(),OfficialChaoRig.texture(c.state()),HeroRunMorphology052.sample(c.state()));}
      if(c.walkAnimation.speed()>.01f)MOVING[phase]++;
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
    var entry=CASES.get(phase);String label=(entry.photo()?STATIC_ANGLES:MOTION_ANGLES)[view];
    String name=String.format("hero055-%s-%02d-%s-%s-t%03d-f%04d.png","live",phase,entry.name(),label,elapsed,WORLD[phase]);int index=phase;long at=elapsed;
    Screenshot.grab(OUTPUT.toFile(),name,mc.getMainRenderTarget(),message->{var file=OUTPUT.resolve("screenshots").resolve(name);require(Files.isRegularFile(file),"native screenshot missing");SAVED.incrementAndGet();LogUtils.getLogger().info("HERO055_CAPTURE {} case={} angle={} tick={} sha256={} wallNanos={} epochMillis={}",name,index,label,at,sha256(file),System.nanoTime(),System.currentTimeMillis());});shots++;SHOTS[phase][view]++;
  }
  private static void finishCase(Minecraft mc){
    var entry=CASES.get(phase);require(RENDERED[phase]>25&&WORLD[phase]>25,"case lacked actual world/actor rendering");
    for(int i=0;i<entry.angleCount();i++)require(SHOTS[phase][i]>=(entry.still()?1:3),"case missed a camera angle");
    if(entry.walk())require(MOVING[phase]>20,"production walk animation was not observed");
    if(entry.toy())require(CONTACTS[phase]>30,"actual toy grip was never measured");
    if(entry.carry())require(CompanionBackLayer.renders()>carryBefore+20,"actual player carry layer never rendered");
    if(entry.ui())require(UI_VERIFIED[phase]&&UI_SHOTS[phase]>=2,"actual Analyzer drag/read-only/save checks missing");
    if(entry.action().equals("evolution"))require(BEFORE[phase]>3&&COCOON[phase]>20&&AFTER[phase]>10,"actual before/cocoon/after transition not rendered");
    if(!entry.still())require((System.nanoTime()-playbackWall)/1e9>=entry.duration()/20.0-.65,"real playback ran faster than normal20TPS");
    LogUtils.getLogger().info("HERO055_RENDER_COMPLETE {} name={} baseline={} angles={} worldFrames={} actorFrames={} captures={} movingFrames={} toyContacts={} maxToyError={} uiFrames={} uiCaptures={} epochMillis={}",phase,entry.name(),baseline(),entry.angleCount(),WORLD[phase],RENDERED[phase],Arrays.stream(SHOTS[phase]).sum(),MOVING[phase],CONTACTS[phase],MAX_CONTACT[phase],UI_FRAMES[phase],UI_SHOTS[phase],System.currentTimeMillis());
    int done=phase;pendingPhase=phase;clientPhase=-2;onServer(mc.getSingleplayerServer(),()->{if(phase==done){if(entry.still()){require(actor.state().encode().equals(expectedState),"frozen stage changed server state");LogUtils.getLogger().info("HERO055_NATIVE_COMPLETE {} name={} baseline={} frozenStatePreserved=true state={} epochMillis={}",phase,entry.name(),baseline(),actor.state().encode(),System.currentTimeMillis());}if(phase+1<limit())next(mc.getSingleplayerServer(),phase+1);else{clearSlot();if(actor!=null){actor.discard();actor=null;}phase=limit();mc.getSingleplayerServer().tickRateManager().setFrozen(false);}}});
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
    if(elapsed>=6&&elapsed!=lastMotionCapture&&elapsed%12==0&&viewFrames>=3){lastMotionCapture=elapsed;capture(mc);}
    if(complete&&viewFrames>=10)finishCase(mc);
  }
  @SubscribeEvent public static void client(ClientTickEvent.Post event){
    if(!enabled()||finished)return;var mc=Minecraft.getInstance();ticks++;
    if(fatal!=null)throw new IllegalStateException("HERO055 server fixture failed: "+fatal);
    if(wallStarted==0)wallStarted=System.nanoTime();require(ticks<60000&&System.nanoTime()-wallStarted<2_400_000_000_000L,"actual client study timed out");
    if(mc.screen instanceof AccessibilityOnboardingScreen)mc.setScreen(new TitleScreen());
    if(!started&&mc.screen instanceof TitleScreen){
      catalog();started=true;mc.options.renderDistance().set(3);mc.options.simulationDistance().set(5);mc.options.framerateLimit().set(30);mc.options.fov().set(45);mc.options.pauseOnLostFocus=false;mc.options.hideGui=true;mc.options.tutorialStep=net.minecraft.client.tutorial.TutorialSteps.NONE;
      mc.options.guiScale().set(2);mc.getWindow().setWindowed(Math.clamp(parameter("CHAO_RUN055_WIDTH",1440),854,2560),Math.clamp(parameter("CHAO_RUN055_HEIGHT",900),480,1440));mc.resizeDisplay();
      mc.createWorldOpenFlows().createFreshLevel("HeroRun055-"+System.currentTimeMillis(),new LevelSettings("HeroRun055",GameType.CREATIVE,false,Difficulty.PEACEFUL,true,new GameRules(),WorldDataConfiguration.DEFAULT),new WorldOptions(2555L,false,false),r->r.registryOrThrow(Registries.WORLD_PRESET).getHolderOrThrow(WorldPresets.FLAT).value().createWorldDimensions(),mc.screen);
    }
    if(!setup&&mc.player!=null&&mc.getSingleplayerServer()!=null){
      setup=true;mc.setScreen(null);ChaoRenderer.suppressNames=true;
      var window=mc.getWindow();LogUtils.getLogger().info("HERO055_WINDOW x={} y={} screenWidth={} screenHeight={} framebufferWidth={} framebufferHeight={} baseline={} epochMillis={}",window.getX(),window.getY(),window.getScreenWidth(),window.getScreenHeight(),window.getWidth(),window.getHeight(),baseline(),System.currentTimeMillis());
      onServer(mc.getSingleplayerServer(),()->{var server=mc.getSingleplayerServer();var level=server.overworld();level.setDayTime(6000);level.getGameRules().getRule(GameRules.RULE_DAYLIGHT).set(false,server);level.getGameRules().getRule(GameRules.RULE_DOMOBSPAWNING).set(false,server);
        for(var at:BlockPos.betweenClosed(-12,79,-12,48,86,12))level.setBlock(at,at.getY()==79?Blocks.SMOOTH_STONE.defaultBlockState():Blocks.AIR.defaultBlockState(),3);next(server,start());});
    }
    if(mc.level!=null&&mc.player!=null&&phase>=0&&phase<limit()){
      if(clientPhase==-2&&phase==pendingPhase)return;
      if(clientPhase!=phase){clientPhase=phase;view=viewFrames=0;armRequested=false;lastMotionCapture=-1;carryBefore=CompanionBackLayer.renders();}
      if(CASES.get(phase).ui())uiControl(mc);
      camera(mc);
    }
    if(phase==limit()&&SAVED.get()==shots){
      for(int i=start();i<limit();i++)require(SKIPPED[i]||WORLD[i]>25,"missing actual client case");
      if(start()==0&&limit()>=10){for(int first:new int[]{0,5}){var distinct=new HashSet<Long>();for(int i=first;i<first+5;i++)distinct.add(SIGNATURES[i]);require(distinct.size()==5,"development stages reused one actual mesh shape");}}
      int skipped=0;for(int i=start();i<limit();i++)if(SKIPPED[i])skipped++;
      finished=true;LogUtils.getLogger().info("HERO055_VISUAL_COMPLETE requestedCases={} renderedCases={} skippedCases={} start={} limit={} baseline={} captures={} saved={} epochMillis={} normal20TPS=true audioListening=false output={}",limit()-start(),limit()-start()-skipped,skipped,start(),limit(),baseline(),shots,SAVED.get(),System.currentTimeMillis(),OUTPUT);mc.stop();
    }
  }
}
