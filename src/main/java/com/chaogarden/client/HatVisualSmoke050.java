package com.chaogarden.client;

import com.chaogarden.ChaoGarden;
import com.chaogarden.core.ChaoState;
import com.chaogarden.entity.ChaoEntity;
import com.chaogarden.test.ToyVariantCases02522;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.logging.LogUtils;
import java.nio.file.Files;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import net.minecraft.client.*;
import net.minecraft.client.gui.screens.AccessibilityOnboardingScreen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.item.Item;
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
import org.joml.Vector3f;

/** Opt-in actual-client fit/contact sheets. Excluded from the release JAR by *VisualSmoke*. */
@EventBusSubscriber(modid=ChaoGarden.MODID,value=Dist.CLIENT)
public final class HatVisualSmoke050 {
  private record Hat(String id,Item item,String key) {}
  private record Case(Hat hat,String variant) {}
  private static final List<Hat> HATS=List.of(
      new Hat("carved_pumpkin",Items.CARVED_PUMPKIN,"vanilla_carved_pumpkin"),
      new Hat("melon",Items.MELON,"watermelon_hat"),
      new Hat("skeleton_skull",Items.SKELETON_SKULL,"vanilla_skeleton_skull"));
  private static final List<String> FORMS=List.of("baby_neutral","adult_neutral_type0",
      "adult_hero_type4","adult_dark_type3","chaos_0","chaos_1","chaos_2",
      "tails_adult","knuckles_baby","amy_adult","animal_dragon_baby","animal_sheep_adult");
  private static List<Case> cases(){
    var result=new ArrayList<Case>();
    HATS.forEach(h->FORMS.forEach(v->result.add(new Case(h,v))));
    // Append rather than renumber the original sheets. The source parrot beak
    // occupies FOREHEAD; the boar muzzle independently exercises FACE.
    for(String variant:List.of("animal_parrot_baby","animal_boar_baby"))
      for(Hat hat:List.of(HATS.get(0),HATS.get(2)))result.add(new Case(hat,variant));
    return List.copyOf(result);
  }
  private static final List<Case> CASES=cases();
  private static boolean enabled(){return Boolean.getBoolean("chao_garden.hat050Study");}
  private static int limit(){return Math.clamp(Integer.getInteger("chao_garden.hat050Limit",CASES.size()),1,CASES.size());}
  private static int start(){return Math.clamp(Integer.getInteger("chao_garden.hat050Start",0),0,limit()-1);}
  private static final int[][] CAPTURES=new int[CASES.size()][3];
  private static final int[] WORLD_FRAMES=new int[CASES.size()],ACTOR_FRAMES=new int[CASES.size()];
  private static final String[] EXPECTED_STATE=new String[CASES.size()];
  private static final AtomicInteger SAVED_SHOTS=new AtomicInteger();
  private static volatile int phase=-1;
  private static volatile UUID actorId;
  private static volatile String fatal;
  private static volatile boolean expectedAdult;
  private static ChaoEntity actor;
  private static ArmorStand observer;
  private static int clientPhase=-1,pendingPhase=-1,view,viewFrames,ticks,shots;
  private static boolean started,setup,finished,renderedThisFrame,worldThisFrame;
  private static long wallStarted;
  private static void require(boolean result,String message){if(!result)throw new IllegalStateException("HAT050 "+message);}
  private static void onServer(MinecraftServer server,Runnable task){server.execute(()->{
    try{task.run();}catch(Throwable failure){fatal=failure.toString();LogUtils.getLogger().error("HAT050_ASYNC_FAILURE",failure);}
  });}
  private static ChaoState fixtureState(String variant){
    if(!variant.equals("animal_parrot_baby"))return ToyVariantCases02522.all().stream().filter(c->c.name().equals(variant)).findFirst().orElseThrow().state();
    // The shared catalogue selects animals with arm parts, so it omits parrot.
    var result=ToyVariantCases02522.all().stream().filter(c->c.name().equals("baby_neutral")).findFirst().orElseThrow().state();
    int animal=Arrays.asList(ChaoState.ANIMALS).indexOf("parrot");require(animal>=0,"registered parrot DNA missing");
    result.animalMask=1<<animal;
    for(int slot=0;slot<result.animalParts.length;slot++)if((ChaoState.animalSlots(animal)&(1<<slot))!=0)result.animalParts[slot]=animal;
    return result;
  }
  private static void next(MinecraftServer server,int index){
    var level=server.overworld();server.tickRateManager().setFrozen(true);
    actorId=null;if(actor!=null){actor.discard();actor=null;}
    var entry=CASES.get(index);
    ChaoState state=fixtureState(entry.variant());
    // The shared visual catalogue sets part slots directly, without rebuilding
    // their derived mask. Start from a consistent fixture before checking that
    // the real entity/hat reload preserves every encoded care/genetic field.
    state.animalPartsMask=0;
    for(int animal:state.animalParts)if(animal>=0&&animal<ChaoState.ANIMALS.length)state.animalPartsMask|=1<<animal;
    actor=ChaoGarden.CHAO.get().create(level);require(actor!=null,"Chao fixture did not create");
    actor.setState(state);actor.setNoAi(true);actor.setNoGravity(true);actor.setInvulnerable(true);actor.setOnGround(true);
    actor.moveTo(.5,80,.5,0,0);actor.yBodyRot=actor.yBodyRotO=actor.yHeadRot=actor.yHeadRotO=0;
    var head=new ItemStack(entry.hat().item());head.set(DataComponents.CUSTOM_NAME,Component.literal("Hat050 preserved "+index));
    actor.setItemSlot(EquipmentSlot.HEAD,head);
    var saved=new CompoundTag();actor.saveWithoutId(saved);String originalState=actor.state().encode();
    actor.load(saved);
    require(ItemStack.isSameItemSameComponents(head,actor.getItemBySlot(EquipmentSlot.HEAD)),"serialized actual vanilla stack/components changed");
    require(originalState.equals(actor.state().encode()),"hat reload changed Chao care/genetics");
    require(actor.getChaoHat().equals(entry.hat().key()),"server did not recognize canonical vanilla hat "+entry.hat().id());
    actor.goalSelector.removeAllGoals(g->true);actor.targetSelector.removeAllGoals(g->true);
    require(level.addFreshEntity(actor),"fixture failed to enter the real level");
    expectedAdult=state.adult;EXPECTED_STATE[index]=actor.state().encode();actorId=actor.getUUID();phase=index;
    LogUtils.getLogger().info("HAT050_CASE {} item={} variant={} key={} savedComponents=true expectedAdult={}",index,entry.hat().id(),entry.variant(),entry.hat().key(),expectedAdult);
  }
  private static ChaoEntity observed(Minecraft mc){
    if(mc.level==null||actorId==null)return null;
    for(var entity:mc.level.entitiesForRendering())if(entity instanceof ChaoEntity c&&c.getUUID().equals(actorId))return c;
    return null;
  }
  @SubscribeEvent public static void restoreCamera(ClientTickEvent.Pre event){if(enabled()){
    var mc=Minecraft.getInstance();if(mc.player!=null&&mc.getCameraEntity()!=mc.player)mc.setCameraEntity(mc.player);
  }}
  private static void camera(Minecraft mc){
    var c=observed(mc);if(c==null)return;
    if(observer==null||observer.level()!=mc.level){observer=new ArmorStand(mc.level,0,0,0);observer.setInvisible(true);observer.setNoGravity(true);}
    Vec3 focus=c.position().add(0,expectedAdult?.54:.46,0);
    Vec3 offset=switch(view){case 0->new Vec3(0,.32,2.2);case 1->new Vec3(1.7,.55,1.7);default->new Vec3(-1.7,.6,-1.7);};
    var eye=focus.add(offset);var delta=focus.subtract(eye);
    float yaw=(float)(Math.toDegrees(Math.atan2(delta.z,delta.x))-90),pitch=(float)-Math.toDegrees(Math.atan2(delta.y,Math.hypot(delta.x,delta.z)));
    observer.moveTo(eye.x,eye.y-observer.getEyeHeight(),eye.z,yaw,pitch);
    observer.xo=observer.xOld=observer.getX();observer.yo=observer.yOld=observer.getY();observer.zo=observer.zOld=observer.getZ();
    observer.yRotO=yaw;observer.xRotO=pitch;observer.setYHeadRot(yaw);observer.yHeadRotO=yaw;
    mc.options.setCameraType(CameraType.FIRST_PERSON);mc.setCameraEntity(observer);
  }
  @SubscribeEvent public static void frameBegin(RenderFrameEvent.Pre event){if(enabled())renderedThisFrame=worldThisFrame=false;}
  @SuppressWarnings("unchecked")
  private static void auditOverlay(Object renderer,ChaoModel model,ChaoEntity chao){
    try{
      var layers=net.minecraft.client.renderer.entity.LivingEntityRenderer.class.getDeclaredField("layers");layers.setAccessible(true);
      ChaoOverlayLayer overlay=null;
      for(Object layer:(List<?>)layers.get(renderer))if(layer instanceof ChaoOverlayLayer found){require(overlay==null,"duplicate actual overlay layer");overlay=found;}
      require(overlay!=null,"actual renderer has no animal overlay layer");
      int checked=0,covered=0,kept=0;
      var state=chao.state();String hat=chao.getChaoHat();boolean closed=VanillaHatRenderer050.supports(hat);
      for(String cache:List.of("rigs","cweRigs")){
        if(cache.equals("cweRigs")&&!state.allowsAnimalParts())continue;
        var field=ChaoOverlayLayer.class.getDeclaredField(cache);field.setAccessible(true);
        var rigs=(Map<String,ChaoOverlayLayer.Rig>)field.get(overlay);
        var rig=rigs.get((cache.equals("rigs")?"chao_":"")+model.officialGeometryKey());
        require(rig!=null,"actual rendered overlay did not prepare "+cache+" for "+model.officialGeometryKey());
        for(var entry:rig.poses().entrySet())if(entry.getKey().startsWith("animal_")){
          String[] values=entry.getKey().split("_");int slot=Integer.parseInt(values[1]),animal=Integer.parseInt(values[2]);
          boolean expressed=state.allowsAnimalParts()&&state.animalParts[slot]==animal&&(ChaoState.animalSlots(animal)&(1<<slot))!=0;
          boolean head=slot==ChaoState.EARS||slot==ChaoState.FOREHEAD||slot==ChaoState.HORNS||slot==ChaoState.FACE;
          boolean expected=expressed&&!(closed&&head);
          require(entry.getValue().visible==expected,"actual "+cache+" visibility differs: "+entry.getKey()+" hat="+hat+" visible="+entry.getValue().visible+" expected="+expected);
          checked++;if(expressed&&closed&&head)covered++;else if(expressed)kept++;
        }
      }
      if(CASES.get(phase).variant().startsWith("animal_")){
        require(closed?covered>0:kept>0,"animal fixture did not exercise visible source parts");
        require(kept>0,"closed hat hid unrelated animal limbs");
      }
      if(ACTOR_FRAMES[phase]==0)LogUtils.getLogger().info("HAT050_OVERLAY_VISIBILITY {} actualPoseChecks={} coveredHeadNodes={} retainedAnimalNodes={} closedMask={} savedDNAUnchanged=true",phase,checked,covered,kept,closed);
    }catch(ReflectiveOperationException failure){throw new IllegalStateException("HAT050 cannot inspect actual rendered overlay poses",failure);}
  }
  @SubscribeEvent public static void rendered(RenderLivingEvent.Post<?,?> event){
    if(!enabled()||phase<0||phase>=limit()||SceneShaderPack.shadows()||!(event.getEntity() instanceof ChaoEntity c)||!c.getUUID().equals(actorId))return;
    var mc=Minecraft.getInstance();if(mc.screen!=null||mc.getOverlay()!=null||mc.getCameraEntity()!=observer)return;
    var entry=CASES.get(phase);if(!c.getItemBySlot(EquipmentSlot.HEAD).is(entry.hat().item())||!c.getChaoHat().equals(entry.hat().key()))return;
    require(c.state().adult==expectedAdult&&!c.isCocooned(),"rendered Chao changed age/cocoon state");
    require(EXPECTED_STATE[phase].equals(c.state().encode()),"visible hat/overlay changed the saved Chao care/genetics");
    var model=(ChaoModel)event.getRenderer().getModel();require(model.officialActive()&&model.officialPosesFinite(),"hat owner did not render with finite production model");
    var bounds=model.accessoryBounds("skull");require(bounds!=null&&bounds.length==6,"rendered hat has no actual skull bounds");
    for(float value:bounds)require(Float.isFinite(value),"non-finite skull fit input");
    float[] fitted=null;
    if(VanillaHatRenderer050.supports(entry.hat().key())){
      fitted=VanillaHatRenderer050.fittedBounds(bounds);
      for(int axis=0;axis<3;axis++)require(fitted[axis]<bounds[axis]&&fitted[axis+3]>bounds[axis+3],"live vanilla hat does not surround its actual skull on axis "+axis);
    }
    var anchor=new PoseStack();model.transformAccessoryAnchor("skull",anchor);
    for(int corner=0;corner<8;corner++)require(anchor.last().pose().transformPosition(new Vector3f(bounds[(corner&1)==0?0:3],bounds[(corner&2)==0?1:4],bounds[(corner&4)==0?2:5]).div(16)).isFinite(),"head attachment matrix lost a real skull corner");
    auditOverlay(event.getRenderer(),model,c);
    if(ACTOR_FRAMES[phase]++==0)LogUtils.getLogger().info("HAT050_RENDER_FIT {} item={} geometry={} skullBounds={} fittedVanillaBounds={} motion={}",phase,entry.hat().id(),model.officialGeometryKey(),Arrays.toString(bounds),Arrays.toString(fitted),model.officialMotion());
    renderedThisFrame=true;
  }
  @SubscribeEvent public static void renderedWorld(RenderLevelStageEvent event){
    if(!enabled()||phase<0||phase>=limit()||event.getStage()!=RenderLevelStageEvent.Stage.AFTER_LEVEL||SceneShaderPack.shadows())return;
    var mc=Minecraft.getInstance();if(renderedThisFrame&&mc.level!=null&&mc.screen==null&&mc.getOverlay()==null&&mc.getCameraEntity()==observer)worldThisFrame=true;
  }
  private static void capture(Minecraft mc){
    var entry=CASES.get(phase);String name=String.format("hat050-%02d-%s-%s-v%d.png",phase,entry.hat().id(),entry.variant(),view);
    int index=phase,camera=view;
    Screenshot.grab(mc.gameDirectory,name,mc.getMainRenderTarget(),message->{
      require(Files.isRegularFile(mc.gameDirectory.toPath().resolve("screenshots").resolve(name)),"capture did not save");
      SAVED_SHOTS.incrementAndGet();LogUtils.getLogger().info("HAT050_CAPTURE {} case={} view={}",name,index,camera);
    });
    shots++;CAPTURES[phase][view]++;
  }
  @SubscribeEvent public static void frameEnd(RenderFrameEvent.Post event){
    if(!enabled()||phase<0||phase>=limit()||clientPhase!=phase||!worldThisFrame)return;
    WORLD_FRAMES[phase]++;if(++viewFrames<12)return;
    capture(Minecraft.getInstance());viewFrames=0;
    if(view<2){view++;return;}
    require(ACTOR_FRAMES[phase]>=36,"hat owner was not actually rendered through three views");
    for(int count:CAPTURES[phase])require(count==1,"missing or duplicate camera view");
    var entry=CASES.get(phase);LogUtils.getLogger().info("HAT050_RENDER_COMPLETE {} item={} variant={} worldFrames={} actorFrames={} views=3 saveReload=true",phase,entry.hat().id(),entry.variant(),WORLD_FRAMES[phase],ACTOR_FRAMES[phase]);
    int done=phase;pendingPhase=phase;clientPhase=-2;
    var mc=Minecraft.getInstance();onServer(mc.getSingleplayerServer(),()->{if(phase==done){if(phase+1<limit())next(mc.getSingleplayerServer(),phase+1);else phase=limit();}});
  }
  @SubscribeEvent public static void client(ClientTickEvent.Post event){
    if(!enabled()||finished)return;var mc=Minecraft.getInstance();ticks++;
    if(fatal!=null)throw new IllegalStateException("HAT050 server helper failed: "+fatal);
    if(wallStarted==0)wallStarted=System.nanoTime();require(ticks<16000&&System.nanoTime()-wallStarted<900_000_000_000L,"client study timed out");
    if(mc.screen instanceof AccessibilityOnboardingScreen)mc.setScreen(new TitleScreen());
    if(!started&&mc.screen instanceof TitleScreen){
      started=true;mc.options.renderDistance().set(3);mc.options.simulationDistance().set(5);mc.options.framerateLimit().set(30);mc.options.fov().set(35);
      mc.options.pauseOnLostFocus=false;mc.options.hideGui=true;mc.options.tutorialStep=net.minecraft.client.tutorial.TutorialSteps.NONE;
      mc.createWorldOpenFlows().createFreshLevel("Hat050-"+System.currentTimeMillis(),new LevelSettings("Hat050",GameType.CREATIVE,false,Difficulty.PEACEFUL,true,new GameRules(),WorldDataConfiguration.DEFAULT),new WorldOptions(2550L,false,false),r->r.registryOrThrow(Registries.WORLD_PRESET).getHolderOrThrow(WorldPresets.FLAT).value().createWorldDimensions(),mc.screen);
    }
    if(!setup&&mc.player!=null&&mc.getSingleplayerServer()!=null){
      setup=true;mc.setScreen(null);ChaoRenderer.suppressNames=true;
      onServer(mc.getSingleplayerServer(),()->{
        var server=mc.getSingleplayerServer();var level=server.overworld();level.setDayTime(6000);
        level.getGameRules().getRule(GameRules.RULE_DAYLIGHT).set(false,server);level.getGameRules().getRule(GameRules.RULE_DOMOBSPAWNING).set(false,server);
        for(var pos:BlockPos.betweenClosed(-6,79,-6,6,85,6))level.setBlock(pos,pos.getY()==79?Blocks.SMOOTH_STONE.defaultBlockState():Blocks.AIR.defaultBlockState(),3);
        server.getPlayerList().getPlayers().forEach(p->{p.setGameMode(GameType.SPECTATOR);p.teleportTo(4.5,82,4.5);p.setNoGravity(true);});
        next(server,start());
      });
    }
    if(phase>=0&&phase<limit()&&mc.level!=null&&mc.player!=null){
      if(clientPhase==-2&&phase==pendingPhase)return;
      if(clientPhase!=phase){clientPhase=phase;view=0;viewFrames=0;}
      camera(mc);
    }
    if(phase==limit()&&SAVED_SHOTS.get()==shots){
      for(int i=start();i<limit();i++)require(WORLD_FRAMES[i]>=36,"case had no actual world rendering");
      finished=true;onServer(mc.getSingleplayerServer(),()->mc.getSingleplayerServer().tickRateManager().setFrozen(false));
      LogUtils.getLogger().info("HAT050_VISUAL_COMPLETE cases={} start={} limit={} screenshots={} saved={} actualWorldActor=true staticFitStudy=true",limit()-start(),start(),limit(),shots,SAVED_SHOTS.get());mc.stop();
    }
  }
}
