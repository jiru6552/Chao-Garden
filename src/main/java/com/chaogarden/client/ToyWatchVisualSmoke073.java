package com.chaogarden.client;

import com.chaogarden.ChaoGarden;
import com.chaogarden.core.*;
import com.chaogarden.entity.ChaoEntity;
import com.chaogarden.toys.*;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.logging.LogUtils;
import java.nio.file.*;
import java.security.MessageDigest;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import net.minecraft.client.*;
import net.minecraft.client.gui.screens.AccessibilityOnboardingScreen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.EquipmentSlot;
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
import org.joml.Matrix4f;
import org.joml.Vector3f;

/** Disposable actual-client TV/prone study. Uses production poses, clocks, AI and commands only. */
@EventBusSubscriber(modid=ChaoGarden.MODID,value=Dist.CLIENT)
public final class ToyWatchVisualSmoke073 {
  private record Case(String name,String toy,String variant,String mode,Direction startSide,String interrupt,String coat,String hat,boolean rabbit){
    String kind(){return "chao_"+toy;}
    boolean tv(){return toy.equals("tv");}
    float initialYaw(){return name.equals("tv_dark_run_preview_loop_stop")?270f:180f;}
    boolean natural(){return mode.equals("natural");}
    boolean loop(){return mode.equals("loop");}
    boolean preview(){return !natural();}
    int interruptAt(){return loop()?200:90;}
    int finish(){return loop()?224:184;}
    int[] shotTicks(){return loop()?new int[]{8,40,74,120,150,180,196,218}:!interrupt.isEmpty()?new int[]{8,40,74,88,96,120,176}:new int[]{8,40,74,120,150,176};}
    String sound(){return tv()?"minecraft:block.note_block.bit":toy.equals("toy_car")?"chao_garden:toy.car":"";}
    int expectedSounds(){return tv()?(loop()?2:1):toy.equals("toy_car")?(loop()?5:4):0;}
  }
  private static List<Case> cases(){
    var all=new ArrayList<Case>();
    all.add(new Case("tv_baby_from_front","tv","baby_neutral","natural",Direction.SOUTH,"","natural","",false));
    all.add(new Case("tv_baby_from_back","tv","baby_neutral","natural",Direction.NORTH,"","natural","",false));
    all.add(new Case("tv_baby_from_east","tv","baby_neutral","natural",Direction.EAST,"","natural","",false));
    all.add(new Case("tv_baby_from_west","tv","baby_neutral","natural",Direction.WEST,"","natural","",false));
    all.add(new Case("tv_dark_run_from_front","tv","dark_run_run_fly","natural",Direction.SOUTH,"","natural_shiny","",false));
    all.add(new Case("tv_dark_run_from_back","tv","dark_run_run_fly","natural",Direction.NORTH,"","natural_shiny","",false));
    all.add(new Case("tv_dark_run_from_east","tv","dark_run_run_fly","natural",Direction.EAST,"","natural_shiny","",false));
    all.add(new Case("tv_dark_run_from_west","tv","dark_run_run_fly","natural",Direction.WEST,"","natural_shiny","",false));
    all.add(new Case("tv_hero_swim_from_back","tv","hero_swim_run_swim","natural",Direction.NORTH,"","natural","",false));
    all.add(new Case("tv_baby_preview_once","tv","baby_neutral","once",Direction.SOUTH,"","natural","",false));
    all.add(new Case("tv_hero_power_hat_preview_once","tv","hero_power_power_fly","once",Direction.SOUTH,"","natural","pumpkin",false));
    all.add(new Case("tv_dark_run_preview_loop_stop","tv","dark_run_run_fly","loop",Direction.SOUTH,"stop","natural_shiny","",false));
    all.add(new Case("tv_swim_fly_preview_damage","tv","neutral_swim_fly","once",Direction.SOUTH,"damage","natural","",false));
    all.add(new Case("tv_fly_swim_preview_remove","tv","neutral_fly_swim","once",Direction.SOUTH,"remove","natural","",false));
    all.add(new Case("car_baby_neutral","toy_car","baby_neutral","once",Direction.SOUTH,"","natural","",false));
    all.add(new Case("crayons_baby_neutral","crayons","baby_neutral","once",Direction.SOUTH,"","natural","",false));
    all.add(new Case("car_baby_shiny_rabbit","toy_car","baby_neutral","once",Direction.SOUTH,"","red_shiny","",true));
    all.add(new Case("crayons_baby_shiny_rabbit","crayons","baby_neutral","once",Direction.SOUTH,"","red_shiny","",true));
    all.add(new Case("car_neutral_swim_fly","toy_car","neutral_swim_fly","once",Direction.SOUTH,"","natural","",false));
    all.add(new Case("crayons_neutral_swim_fly","crayons","neutral_swim_fly","once",Direction.SOUTH,"","natural","",false));
    all.add(new Case("car_neutral_fly_swim","toy_car","neutral_fly_swim","once",Direction.SOUTH,"","natural","",false));
    all.add(new Case("crayons_neutral_fly_swim","crayons","neutral_fly_swim","once",Direction.SOUTH,"","natural","",false));
    all.add(new Case("car_hero_power_power_fly","toy_car","hero_power_power_fly","once",Direction.SOUTH,"","natural","",false));
    all.add(new Case("crayons_hero_power_power_fly","crayons","hero_power_power_fly","once",Direction.SOUTH,"","natural","",false));
    all.add(new Case("car_dark_fly_run_swim","toy_car","dark_fly_run_swim","once",Direction.SOUTH,"","green_mono","",false));
    all.add(new Case("crayons_dark_fly_run_swim","crayons","dark_fly_run_swim","once",Direction.SOUTH,"","green_mono","",false));
    all.add(new Case("car_dark_run_loop_stop","toy_car","dark_run_run_fly","loop",Direction.SOUTH,"stop","natural","",false));
    all.add(new Case("crayons_hero_power_damage","crayons","hero_power_power_fly","once",Direction.SOUTH,"damage","natural","",false));
    require(all.size()==28,"authored catalog count changed");
    String selected=System.getenv("CHAO_TOYS073_CASES");
    if(selected!=null&&!selected.isBlank()){
      var chosen=new ArrayList<Case>();var used=new HashSet<Integer>();
      for(String text:selected.split(",")){int index=Integer.parseInt(text.trim());require(index>=0&&index<all.size()&&used.add(index),"invalid/duplicate authored case");chosen.add(all.get(index));}
      return List.copyOf(chosen);
    }
    return List.copyOf(all);
  }
  private static final List<Case> CASES=cases();
  private static final BlockPos TOY=new BlockPos(0,80,2);
  private static final Direction FACING=Direction.SOUTH;
  private static final Path OUTPUT=Path.of(System.getenv().getOrDefault("CHAO_TOYS073_OUTPUT","/workspace/.setup/toys073-client-final")).toAbsolutePath().normalize();
  private static final int[] WORLD=new int[CASES.size()],RENDER=new int[CASES.size()],ACTIVE=new int[CASES.size()],IDLE=new int[CASES.size()],PRONE_CHECKS=new int[CASES.size()],FRAME_MASK=new int[CASES.size()],EXIT_FLOOR=new int[CASES.size()],CANCEL_SERVER=new int[CASES.size()],CANCEL_RENDER=new int[CASES.size()];
  private static final float[] MAX_NECK_GAP=new float[CASES.size()];
  private static final boolean[][] SHOTS=new boolean[CASES.size()][];
  private static final int[] APPROACH_SHOTS=new int[CASES.size()];
  private static final AtomicInteger SAVED=new AtomicInteger();
  private static final java.util.concurrent.atomic.AtomicIntegerArray SOUNDS=new java.util.concurrent.atomic.AtomicIntegerArray(CASES.size());
  private record SoundRequest(int index,long elapsed){}
  private static final Map<net.minecraft.client.resources.sounds.SoundInstance,SoundRequest> SOUND_REQUESTS=Collections.synchronizedMap(new IdentityHashMap<>());
  private static final LionfishActiveToyClip049 ACTIVE_CLIP=new LionfishActiveToyClip049();
  private static final LionfishQuietToyClip049 QUIET_CLIP=new LionfishQuietToyClip049();
  private static volatile int phase=-1;
  private static volatile UUID actorId;
  private static volatile long elapsed=-1,caseAge;
  private static volatile boolean armed,complete;
  private static volatile String fatal;
  private static ChaoEntity actor;
  private static ToyContent.PlayGoal naturalGoal;
  private static ArmorStand observer;
  private static long spawned,began,lastTick,wallStart,playbackWall;
  private static Vec3 previousPosition,initialPosition;
  private static volatile double walked;
  private static String savedState;
  private static ItemStack savedHat,savedOffhand;
  private static long savedCooldown;
  private static int savedFirst;
  private static float savedAlign,interruptHappiness;
  private static boolean interrupted,previewProof,originalNoAi;
  private static boolean started,setup,finished,armRequested,renderedThisFrame,worldThisFrame;
  private static int clientPhase=-1,pendingPhase=-1,ticks,shots,view,viewFrames;
  private static long lastCaptureNanos;

  private static boolean enabled(){return "true".equalsIgnoreCase(System.getenv("CHAO_TOYS073_SMOKE"));}
  private static int parameter(String name,int fallback){try{return Integer.parseInt(System.getenv().getOrDefault(name,Integer.toString(fallback)));}catch(NumberFormatException ignored){return fallback;}}
  private static int limit(){return Math.clamp(parameter("CHAO_TOYS073_LIMIT",CASES.size()),1,CASES.size());}
  private static int start(){return Math.clamp(parameter("CHAO_TOYS073_START",0),0,limit()-1);}
  private static void require(boolean okay,String message){if(!okay)throw new IllegalStateException("TOYS073 "+message);}
  private static void onServer(MinecraftServer server,Runnable task){server.execute(()->{try{task.run();}catch(Throwable error){fatal=error.toString();LogUtils.getLogger().error("TOYS073_ASYNC_FAILURE",error);}});}
  private static String sha(Path file){try{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(file)));}catch(Exception error){throw new IllegalStateException(error);}}
  private static int first(Case c){return switch(c.variant()){case "neutral_swim_fly","hero_swim_run_swim"->ChaoState.SWIM_TYPE;case "neutral_fly_swim","dark_fly_run_swim"->ChaoState.FLY_TYPE;case "hero_power_power_fly"->ChaoState.POWER_TYPE;case "dark_run_run_fly"->ChaoState.RUN_TYPE;default->ChaoState.NORMAL;};}
  private static float alignment(Case c){return c.variant().startsWith("hero_")?1:c.variant().startsWith("dark_")?-1:0;}
  private static boolean adult(Case c){return !c.variant().startsWith("baby_");}
  private static int rp(Case c){return c.variant().contains("run_")||c.variant().endsWith("_run_swim")||c.variant().endsWith("_run_fly")?-1:c.variant().contains("power_power")?1:0;}
  private static int sf(Case c){return c.variant().endsWith("_swim")?-1:c.variant().endsWith("_fly")?1:0;}
  /** Build only ordinary saved state; the real renderer chooses every model and attachment. */
  private static ChaoState fixture(Case entry){
    var s=new ChaoState();s.adult=adult(entry);s.align=alignment(entry);s.primaryType=first(entry);s.hunger=100;s.energy=100;s.happiness=20;s.boredom=100;s.personality=ChaoPreferences.EASILY_BORED;
    s.setEvolutionStrength(s.adult?GradualEvolutionRules.ADULT_STRENGTH_MAX:0);
    if(s.adult){s.train(ChaoState.RUN,1200,new Random(7301));s.train(ChaoState.POWER,1200,new Random(7302));}
    if(rp(entry)!=0)s.train(rp(entry)<0?ChaoState.RUN:ChaoState.POWER,960,new Random(7303));
    if(sf(entry)!=0)s.train(sf(entry)<0?ChaoState.SWIM:ChaoState.FLY,960,new Random(7304));
    for(int illness=0;illness<ChaoIllness.COUNT;illness++)s.setIllness(illness,100);
    if(entry.rabbit()){
      int animal=Arrays.asList(ChaoState.ANIMALS).indexOf("rabbit");require(animal>=0,"rabbit missing");s.animalMask=s.animalPartsMask=1<<animal;
      for(int part=0;part<s.animalParts.length;part++)if((ChaoState.animalSlots(animal)&(1<<part))!=0)s.animalParts[part]=animal;
    }
    if(entry.coat().equals("red_shiny")){s.expressedColor=2;s.colorGenes=new int[]{2,2};s.twoTone=true;s.toneGenes=new int[]{1,1};}
    if(entry.coat().equals("green_mono")){s.expressedColor=10;s.colorGenes=new int[]{10,10};s.twoTone=false;s.toneGenes=new int[]{0,0};}
    if(entry.coat().contains("shiny")){s.shiny=true;s.shinyGenes=new int[]{1,1};}
    String encoded=s.encode();var out=ChaoState.decode(encoded);require(encoded.equals(out.encode()),"fixture codec changed state");
    if(s.adult)require(out.lockedPrimaryType()==first(entry)&&out.lockedAlignment()==alignment(entry),"fixture identity is not locked");return out;
  }
  private static void command(MinecraftServer server,String mode){
    var p=server.getPlayerList().getPlayers().getFirst();try{require(server.getCommands().getDispatcher().execute("chao testtoy "+CASES.get(phase).toy()+" "+mode,p.createCommandSourceStack().withPermission(2))==1,"preview dispatcher rejected "+mode);}
    catch(com.mojang.brigadier.exceptions.CommandSyntaxException error){throw new IllegalStateException(error);}
  }
  private static void next(MinecraftServer server,int index){
    server.tickRateManager().setFrozen(true);var level=server.overworld();
    if(actor!=null){ToyPreview.stop(actor);ToyContent.endPlaying(actor,false);actor.discard();actor=null;}
    var c=CASES.get(index);level.setBlock(TOY,c.tv()?ToyContent.block(c.kind()).defaultBlockState().setValue(ChaoToyBlock.FACING,FACING):Blocks.AIR.defaultBlockState(),3);
    actor=ChaoGarden.CHAO.get().create(level);require(actor!=null,"actor create failed");actor.setState(fixture(c));actor.setInvulnerable(true);actor.deferBehavior(100000);originalNoAi=c.loop();actor.setNoAi(originalNoAi);actor.getRandom().setSeed(18);
    Vec3 origin=c.natural()?Vec3.atBottomCenterOf(TOY.relative(c.startSide(),3)):c.tv()?Vec3.atBottomCenterOf(TOY.relative(FACING)):new Vec3(.5,80,3.5);
    float yaw=c.initialYaw();actor.moveTo(origin.x,origin.y,origin.z,yaw,0);actor.yBodyRot=actor.yBodyRotO=actor.yHeadRot=actor.yHeadRotO=yaw;actor.setOnGround(true);
    if(!c.hat().isEmpty())actor.setItemSlot(EquipmentSlot.HEAD,new ItemStack(Items.CARVED_PUMPKIN));
    level.addFreshEntity(actor);actor.goalSelector.removeAllGoals(g->true);actor.targetSelector.removeAllGoals(g->true);
    naturalGoal=c.natural()?new ToyContent.PlayGoal(actor):null;if(naturalGoal!=null)actor.goalSelector.addGoal(4,naturalGoal);
    else actor.getPersistentData().putLong("ChaoToyNextPlay",Long.MAX_VALUE);
    savedState=actor.state().encode();savedCooldown=actor.getPersistentData().getLong("ChaoToyNextPlay");savedHat=actor.getItemBySlot(EquipmentSlot.HEAD).copy();savedOffhand=actor.getItemBySlot(EquipmentSlot.OFFHAND).copy();savedFirst=actor.state().lockedPrimaryType();savedAlign=actor.state().lockedAlignment();
    initialPosition=previousPosition=actor.position();walked=0;spawned=lastTick=level.getGameTime();began=-1;elapsed=-1;caseAge=0;interrupted=previewProof=armed=complete=false;
    SHOTS[index]=new boolean[c.shotTicks().length];actorId=actor.getUUID();phase=index;
    var p=server.getPlayerList().getPlayers().getFirst();p.teleportTo(7.5,82,7.5);p.setNoGravity(true);p.setDeltaMovement(Vec3.ZERO);
    playbackWall=System.nanoTime();LogUtils.getLogger().info("TOYS073_CASE {} name={} toy={} variant={} mode={} startSide={} interrupt={} firstType={} lockedAlignment={} adult={} maturity={} rp={} sf={} coat={} hat={} rabbit={} state={} epochMillis={}",index,c.name(),c.kind(),c.variant(),c.mode(),c.startSide(),c.interrupt(),savedFirst,savedAlign,actor.state().adult,actor.state().gradualVisualStrength(),rp(c),sf(c),c.coat(),c.hat(),c.rabbit(),savedState,System.currentTimeMillis());
  }
  private static void arm(MinecraftServer server,int expected){
    if(phase!=expected||armed)return;require(Math.abs(server.tickRateManager().tickrate()-20)<.00001f,"normal20TPS required");
    var c=CASES.get(phase);actor.setOnGround(true);if(c.preview()){command(server,c.loop()?"loop":"once");began=server.overworld().getGameTime();elapsed=0;}
    armed=true;playbackWall=System.nanoTime();server.tickRateManager().setFrozen(false);LogUtils.getLogger().info("TOYS073_ARMED {} name={} natural={} epochMillis={} normal20TPS=true",phase,c.name(),c.natural(),System.currentTimeMillis());
  }
  private static void naturalRoute(){
    if(actor==null||naturalGoal==null)return;var path=actor.getNavigation().getPath();
    if(path!=null)for(int i=0;i<path.getNodeCount();i++){var node=path.getNodePos(i);require(!node.equals(TOY)&&!node.equals(TOY.above()),"natural path enters cabinet/jump node "+node);}
    walked+=actor.position().subtract(previousPosition).horizontalDistance();previousPosition=actor.position();
  }
  @SubscribeEvent public static void server(ServerTickEvent.Post event){
    if(!enabled()||phase<0||phase>=limit()||!armed||complete)return;var server=event.getServer();var level=server.overworld();long now=level.getGameTime();if(now==lastTick)return;lastTick=now;caseAge=now-spawned;var c=CASES.get(phase);
    require(actor!=null&&actor.isAlive()&&!actor.isRemoved(),"live actor disappeared");require(actor.state().adult==adult(c)&&!actor.isCocooned(),"live fixture changed real life stage");
    if(adult(c))require(actor.state().lockedPrimaryType()==savedFirst&&actor.state().lockedAlignment()==savedAlign,"adult identity changed");
    require(caseAge<2800,"approach/session deadline exceeded pos="+actor.position()+" walked="+walked+" nav="+actor.getNavigation().getTargetPos());
    if(began<0){
      naturalRoute();if(ToyContent.isPlaying(actor)){
        began=actor.getPersistentData().getLong("ChaoToyUntil")-160;playbackWall=System.nanoTime();
        Vec3 wanted=ToyContent.stationaryAnchor(actor,TOY,TOY.relative(FACING));require(actor.position().distanceToSqr(wanted)<.01,"natural TV did not arrive at screen anchor");
        require(walked>.35,"natural route was bypassed");LogUtils.getLogger().info("TOYS073_NATURAL_STARTED {} name={} waitedTicks={} walked={} start={} arrived={} expectedAnchor={} epochMillis={}",phase,c.name(),now-spawned,walked,initialPosition,actor.position(),wanted,System.currentTimeMillis());
      }else if(caseAge%40==0)LogUtils.getLogger().info("TOYS073_APPROACH case={} age={} pos={} walked={} target={} pathDone={} epochMillis={}",phase,caseAge,actor.position(),walked,actor.getNavigation().getTargetPos(),actor.getNavigation().isDone(),System.currentTimeMillis());
    }
    if(began<0)return;elapsed=now-began;
    if(c.preview()&&!previewProof){
      require(savedState.equals(actor.state().encode()),"preview changed saved care/genes/age/lessons before interruption or terminal tick");require(actor.getPersistentData().getLong("ChaoToyNextPlay")==savedCooldown,"preview changed ordinary cooldown");
      if(!ToyPreview.active(actor))previewProof=true;
    }
    if(!interrupted&&!c.interrupt().isEmpty()&&elapsed>=c.interruptAt()){
      require(!c.preview()||savedState.equals(actor.state().encode()),"state changed before deliberate interruption");previewProof=c.preview();
      if(c.interrupt().equals("stop"))command(server,"stop");
      else if(c.interrupt().equals("damage")){actor.setInvulnerable(false);require(actor.hurt(level.damageSources().generic(),1),"real damage refused");}
      else require(level.destroyBlock(TOY,false),"TV source removal refused");
      interrupted=true;interruptHappiness=actor.state().happiness;LogUtils.getLogger().info("TOYS073_INTERRUPT case={} kind={} tick={} state={} epochMillis={}",phase,c.interrupt(),elapsed,actor.state().encode(),System.currentTimeMillis());
    }
    if(interrupted&&elapsed>=c.interruptAt()+2){
      require(!ToyPreview.active(actor)&&actor.toyPlayingTicks()==0&&actor.toyTarget()==null,"interrupted session did not clear on next authoritative ticks");
      if(c.tv()){var source=level.getBlockState(TOY);if(c.interrupt().equals("remove"))require(source.isAir(),"removed TV returned during cancellation");else require(source.is(ToyContent.block(c.kind()))&&!source.getValue(ChaoToyBlock.PLAYING)&&source.getValue(ChaoToyBlock.FRAME)==0,"interrupted TV remained playing");}
      CANCEL_SERVER[phase]++;
    }
    if(c.tv()&&ToyContent.isPlaying(actor)&&!interrupted){
      var source=level.getBlockState(TOY);require(source.is(ToyContent.block(c.kind()))&&source.getValue(ChaoToyBlock.FACING)==FACING,"playing TV changed source/facing");
      Vec3 target=Vec3.atBottomCenterOf(TOY).add(0,.65,0).subtract(actor.position()).multiply(1,0,1).normalize();
      require(Vec3.directionFromRotation(0,actor.getYRot()).dot(target)>.985,"watcher is not facing screen");
      require(actor.position().distanceToSqr(Vec3.atBottomCenterOf(TOY.relative(FACING)))<.02,"watcher drifted from front seat");
    }
    if(elapsed<c.finish())return;
    if(!c.interrupt().isEmpty())require(CANCEL_SERVER[phase]>0,"bounded server cancellation was not observed");
    require(!ToyPreview.active(actor)&&actor.toyPlayingTicks()==0&&actor.toyTarget()==null,"session left preview/clock/reservation");
    require(actor.getHeldChaoItem().isEmpty()&&actor.getMainHandItem().isEmpty(),"real toy item minted");require(ItemStack.matches(savedHat,actor.getItemBySlot(EquipmentSlot.HEAD))&&ItemStack.matches(savedOffhand,actor.getItemBySlot(EquipmentSlot.OFFHAND)),"equipment changed");
    require(actor.isNoAi()==originalNoAi,"original NoAI not restored");require(!actor.isNoGravity(),"toy left gravity flag");
    if(c.preview()){require(previewProof,"preview terminal state was not witnessed");require(actor.getPersistentData().getLong("ChaoToyNextPlay")==savedCooldown,"preview cooldown changed after cleanup");require(actor.state().happiness==(c.interrupt().equals("damage")?interruptHappiness:20),"preview granted happiness reward");}
    else require(actor.state().happiness==24,"natural reward was not exactly once");
    if(c.tv()){
      var source=level.getBlockState(TOY);if(c.interrupt().equals("remove"))require(source.isAir(),"removed TV returned");
      else require(source.is(ToyContent.block(c.kind()))&&!source.getValue(ChaoToyBlock.PLAYING)&&source.getValue(ChaoToyBlock.FRAME)==0&&source.getValue(ChaoToyBlock.FACING)==FACING,"TV did not return idle in place");
    }else require(level.getBlockState(TOY).isAir(),"hand preview created placed toy");
    var tag=new CompoundTag();actor.saveWithoutId(tag);var copy=ChaoGarden.CHAO.get().create(level);copy.load(tag);require(copy.state().encode().equals(actor.state().encode()),"completed actor NBT reload changed state");copy.discard();
    complete=true;server.tickRateManager().setFrozen(true);LogUtils.getLogger().info("TOYS073_NATIVE_COMPLETE {} name={} elapsed={} walked={} previewPlaybackStatePreserved={} originalNoAi={} cancelServerChecks={} state={} epochMillis={}",phase,c.name(),elapsed,walked,previewProof,originalNoAi,CANCEL_SERVER[phase],actor.state().encode(),System.currentTimeMillis());
  }

  @SubscribeEvent public static void frameBegin(RenderFrameEvent.Pre e){if(enabled())renderedThisFrame=worldThisFrame=false;}
  private static ChaoEntity observed(Minecraft mc){if(mc.level!=null&&actorId!=null)for(var e:mc.level.entitiesForRendering())if(e instanceof ChaoEntity c&&c.getUUID().equals(actorId))return c;return null;}
  private static boolean sourceReady(Minecraft mc){if(mc.level==null)return false;var c=CASES.get(phase);var s=mc.level.getBlockState(TOY);return c.tv()?s.is(ToyContent.block(c.kind()))&&s.getValue(ChaoToyBlock.FACING)==FACING&&!s.getValue(ChaoToyBlock.PLAYING):s.isAir();}
  private static int shotView(Case c,int tick){if(tick==74)return 0;if(tick==120||tick==196)return 2;return 1;}
  private static void camera(Minecraft mc){
    var c=observed(mc);if(c==null)return;if(observer==null||observer.level()!=mc.level){observer=new ArmorStand(mc.level,0,0,0);observer.setInvisible(true);observer.setNoGravity(true);}
    var entry=CASES.get(phase);int wanted=1;
    if(elapsed>=0){var moments=entry.shotTicks();for(int n=0;n<moments.length;n++)if(!SHOTS[phase][n]){wanted=shotView(entry,moments[n]);break;}}
    if(wanted!=view){view=wanted;viewFrames=0;}
    Vec3 focus,eye;Vec3 forward=Vec3.directionFromRotation(0,FACING.getOpposite().toYRot()),side=new Vec3(-forward.z,0,forward.x);
    if(entry.tv()){
      focus=Vec3.atBottomCenterOf(TOY).add(0,.52,.50);if(elapsed<0)focus=focus.lerp(c.position().add(0,.35,0),.50);
      eye=focus.add(side.scale(view==0?2.25:view==1?2.5:-1.45)).add(forward.scale(view==0?2.6:view==1?-.30:-3.25)).add(0,view==0?1.5:view==1?.60:1.20,0);
    }else{
      focus=c.position().add(forward.scale(.17)).add(0,.25,0);
      eye=focus.add(forward.scale(view==0?1.8:view==1?.08:-.35)).add(side.scale(view==0?.55:view==1?1.85:-1.85)).add(0,view==1?.12:view==2?.60:.66,0);
    }
    var d=focus.subtract(eye);float yaw=(float)(Math.toDegrees(Math.atan2(d.z,d.x))-90),pitch=(float)-Math.toDegrees(Math.atan2(d.y,Math.hypot(d.x,d.z)));
    observer.moveTo(eye.x,eye.y-observer.getEyeHeight(),eye.z,yaw,pitch);observer.xo=observer.xOld=observer.getX();observer.yo=observer.yOld=observer.getY();observer.zo=observer.zOld=observer.getZ();observer.yRotO=yaw;observer.xRotO=pitch;observer.setYHeadRot(yaw);observer.yHeadRotO=yaw;mc.options.setCameraType(CameraType.FIRST_PERSON);mc.setCameraEntity(observer);
  }
  @SubscribeEvent public static void restoreCamera(ClientTickEvent.Pre e){if(enabled()){var mc=Minecraft.getInstance();if(mc.player!=null&&mc.getCameraEntity()!=mc.player)mc.setCameraEntity(mc.player);}}
  @SubscribeEvent public static void rendered(RenderLivingEvent.Post<?,?> event){
    if(!enabled()||phase<0||phase>=limit()||SceneShaderPack.shadows()||!(event.getEntity()instanceof ChaoEntity c)||!c.getUUID().equals(actorId))return;
    var mc=Minecraft.getInstance();if(mc.screen!=null||mc.getOverlay()!=null||mc.getCameraEntity()!=observer)return;var m=(ChaoModel)event.getRenderer().getModel();require(m.officialPosesFinite(),"rendered pose not finite");renderedThisFrame=true;RENDER[phase]++;
    var entry=CASES.get(phase);if(elapsed<0)return;
    boolean productionActive=entry.tv()?ChaoTvWatchAnimation073.active(c):entry.toy().equals("toy_car")?ChaoActiveToyAnimation049.active(c):ChaoQuietToyAnimation049.active(c);
    boolean cancellationExpected=!entry.interrupt().isEmpty()&&elapsed>=entry.interruptAt();
    if(elapsed>=28&&elapsed<=132&&!cancellationExpected)require(productionActive,"uninterrupted production toy pose ended early");
    if(cancellationExpected&&elapsed>=entry.interruptAt()+6&&elapsed<=entry.interruptAt()+40){
      require(!ChaoTvWatchAnimation073.active(c)&&!ChaoActiveToyAnimation049.active(c)&&!ChaoQuietToyAnimation049.active(c)&&c.toyPlayingTicks()==0,"canceled pose/clock survived bounded client synchronization");
      if(entry.interrupt().equals("damage"))require(c.hurtTime>0||c.getBehavior()==ChaoEntity.Behavior.COWER,"real damage reaction was not rendered");
      if(entry.interrupt().equals("remove"))require(mc.level.getBlockState(TOY).isAir(),"removed TV still visible after bounded synchronization");
      CANCEL_RENDER[phase]++;
    }
    if(c.toyPlayingKind().equals(entry.kind())&&c.toyPlayingTicks()>0&&productionActive){
      ACTIVE[phase]++;
      if(entry.tv()){
        float t=160-c.toyPlayingTicks()+event.getPartialTick();var source=mc.level.getBlockState(TOY);
        if(source.is(ToyContent.block(entry.kind()))&&source.getValue(ChaoToyBlock.PLAYING))FRAME_MASK[phase]|=1<<source.getValue(ChaoToyBlock.FRAME);
        if(t>=28&&t<=132)require(m.officialMotion()==OfficialChaoRig.Motion.SIT,"TV did not select real sitting motion");
      }else{
        float w=GardenToyPerformance049.presence(c,event.getPartialTick());var bounds=ChaoProneToyPose051.runtimeBounds(c,m);
        for(String bone:List.of("body","skull","foot_left",m.officialPoses().containsKey("foot_right")?"foot_right":"feet_bone_right")){
          var box=ChaoProneToyPose051.worldBounds(m::transformAccessoryAnchor,bounds,bone);if(box!=null)require(box[4]<=ChaoProneToyPose051.FLOOR_Y+.025f,"actual prone part enters floor "+bone);
        }
        if(w>.999f){
          require(Math.abs(m.officialPoses().get("body").rx)>1.15f,"car/crayons not stomach lying");float gap=ChaoProneToyPose051.separation(m::transformAccessoryAnchor,bounds);require(gap>=-.0001f,"head/torso overlap");
          // Preserve the strictly tested hand/prop clearance while reducing the old neck gap.
          float maximumGap=(entry.toy().equals("toy_car")?.50f:.25f)/16;
          require(Float.isFinite(gap),"head/body separation is not finite");if(entry.hat().isEmpty())require(gap<=maximumGap,"head remains too far from torso: "+gap+" limit="+maximumGap);MAX_NECK_GAP[phase]=Math.max(MAX_NECK_GAP[phase],gap);PRONE_CHECKS[phase]++;
          if(entry.toy().equals("toy_car")){var palm=ChaoProneToyPose051.worldBounds(m::transformAccessoryAnchor,bounds,"hand_left");require(palm!=null&&Math.abs(palm[4]-ChaoProneToyPose051.FLOOR_Y)<.011f,"car support palm left floor");}
        }
      }
    }else if(elapsed>=166&&c.getBehavior()==ChaoEntity.Behavior.NONE){IDLE[phase]++;require(!ChaoTvWatchAnimation073.active(c)&&!ChaoActiveToyAnimation049.active(c)&&!ChaoQuietToyAnimation049.active(c)&&c.toyPlayingTicks()==0,"old toy pose/clock survived cleanup");}
    long ending=entry.loop()?200:!entry.interrupt().isEmpty()?90:160;
    if(!entry.tv()&&elapsed>=ending&&elapsed<=ending+12){
      var bounds=ChaoProneToyPose051.runtimeBounds(c,m);
      for(String bone:List.of("body","skull","foot_left",m.officialPoses().containsKey("foot_right")?"foot_right":"feet_bone_right")){
        var box=ChaoProneToyPose051.worldBounds(m::transformAccessoryAnchor,bounds,bone);if(box!=null)require(box[4]<=ChaoProneToyPose051.FLOOR_Y+.025f,"outgoing prone pose enters floor "+bone);
      }
      EXIT_FLOOR[phase]++;
    }
  }
  @SubscribeEvent public static void world(RenderLevelStageEvent event){
    if(!enabled()||phase<0||phase>=limit()||event.getStage()!=RenderLevelStageEvent.Stage.AFTER_LEVEL||SceneShaderPack.shadows())return;var mc=Minecraft.getInstance();if(mc.screen==null&&mc.getOverlay()==null&&mc.getCameraEntity()==observer&&renderedThisFrame)worldThisFrame=true;
  }
  /** This pre-resolve event only binds an instance to its real originating scene. */
  @SubscribeEvent public static void soundRequested(net.neoforged.neoforge.client.event.sound.PlaySoundEvent event){
    if(!enabled()||phase<0||phase>=limit()||!armed)return;var sound=event.getOriginalSound();if(sound==null)return;String id=sound.getLocation().toString();
    if(id.equals(CASES.get(phase).sound()))SOUND_REQUESTS.put(sound,new SoundRequest(phase,elapsed));
    if(CASES.get(phase).tv())require(!id.startsWith("chao_garden:toy."),"TV used unrelated repeating toy sound");
    else if(CASES.get(phase).toy().equals("crayons"))require(!id.equals("chao_garden:toy.crayon"),"silent crayons emitted cue");
  }
  /** Actual static-buffer channel playback, after resolution; runs on the sound thread. */
  @SubscribeEvent public static void soundStarted(net.neoforged.neoforge.client.event.sound.PlaySoundSourceEvent event){
    if(!enabled())return;var sound=event.getSound();var request=SOUND_REQUESTS.remove(sound);if(request==null)return;
    try{
      int index=request.index();var entry=CASES.get(index);float volume=sound.getVolume(),pitch=sound.getPitch();
      if(entry.tv())require(volume<=.071f&&Math.abs(pitch-1)<.0001f,"TV cue is unexpectedly loud/pitched");
      SOUNDS.incrementAndGet(index);LogUtils.getLogger().info("TOYS073_SOUND case={} id={} elapsed={} volume={} pitch={} postResolve=true channelStarted=true epochMillis={}",index,sound.getLocation(),request.elapsed(),volume,pitch,System.currentTimeMillis());
    }catch(Throwable error){fatal=error.toString();LogUtils.getLogger().error("TOYS073_ASYNC_FAILURE",error);}
  }
  private static void capture(Minecraft mc,String label,boolean approach){
    var entry=CASES.get(phase);int index=phase,angle=view;long tick=elapsed,age=caseAge;double distanceWalked=walked;String name=String.format("toys073-%02d-%s-%s-%s-t%03d-a%04d.png",index,entry.name(),label,new String[]{"front","side","back"}[angle],tick,age);Path file=OUTPUT.resolve("screenshots").resolve(name);
    try{Files.createDirectories(file.getParent());}catch(Exception error){throw new IllegalStateException(error);}
    Screenshot.grab(OUTPUT.toFile(),name,mc.getMainRenderTarget(),message->{require(Files.isRegularFile(file),"PNG did not save");SAVED.incrementAndGet();LogUtils.getLogger().info("TOYS073_CAPTURE {} case={} label={} angle={} tick={} caseAge={} approach={} walked={} sha256={} epochMillis={}",name,index,label,new String[]{"front","side","back"}[angle],tick,age,approach,distanceWalked,sha(file),System.currentTimeMillis());});shots++;lastCaptureNanos=System.nanoTime();viewFrames=0;
  }
  @SubscribeEvent public static void frameEnd(RenderFrameEvent.Post event){
    if(!enabled()||phase<0||phase>=limit()||clientPhase!=phase||!worldThisFrame)return;var mc=Minecraft.getInstance();WORLD[phase]++;viewFrames++;
    if(!armed){if(viewFrames>=12&&!armRequested){armRequested=true;int i=phase;onServer(mc.getSingleplayerServer(),()->arm(mc.getSingleplayerServer(),i));}return;}
    if(elapsed<0){if(APPROACH_SHOTS[phase]<2&&viewFrames>=6&&(APPROACH_SHOTS[phase]==0||walked>.75)){capture(mc,"approach"+APPROACH_SHOTS[phase],true);APPROACH_SHOTS[phase]++;}return;}
    var entry=CASES.get(phase);int[] moments=entry.shotTicks();
    for(int n=0;n<moments.length;n++)if(!SHOTS[phase][n]){
      if(elapsed>=moments[n]&&viewFrames>=3&&System.nanoTime()-lastCaptureNanos>100_000_000L){capture(mc,"phase"+moments[n],false);SHOTS[phase][n]=true;}break;
    }
    if(!complete)return;for(boolean shot:SHOTS[phase])if(!shot)return;
    require(ACTIVE[phase]>=30&&IDLE[phase]>=3,"active and return-to-idle were not rendered");require(SOUNDS.get(phase)==entry.expectedSounds(),"client cue count "+SOUNDS.get(phase)+" expected "+entry.expectedSounds());
    if(!entry.interrupt().isEmpty())require(CANCEL_RENDER[phase]>0,"bounded client cancellation was not rendered");
    if(entry.tv()){int expected=entry.interrupt().equals("damage")||entry.interrupt().equals("remove")?7:15;require((FRAME_MASK[phase]&expected)==expected,"TV screen frames not all rendered: "+FRAME_MASK[phase]);}
    else{require(PRONE_CHECKS[phase]>10,"full-presence head connection not actually checked");require(EXIT_FLOOR[phase]>0,"outgoing prone floor recovery not rendered");}
    require((System.nanoTime()-playbackWall)/1e9>=entry.finish()/20.0-.8,"study ran faster than normal20TPS");
    LogUtils.getLogger().info("TOYS073_RENDER_COMPLETE {} name={} worldFrames={} actorFrames={} activeFrames={} idleFrames={} sounds={} frameMask={} proneChecks={} maxNeckGap={} outgoingFloorFrames={} cancelRenderFrames={} captures={} normal20TPS=true epochMillis={}",phase,entry.name(),WORLD[phase],RENDER[phase],ACTIVE[phase],IDLE[phase],SOUNDS.get(phase),FRAME_MASK[phase],PRONE_CHECKS[phase],MAX_NECK_GAP[phase],EXIT_FLOOR[phase],CANCEL_RENDER[phase],moments.length+APPROACH_SHOTS[phase],System.currentTimeMillis());
    int done=phase;pendingPhase=phase;clientPhase=-2;onServer(mc.getSingleplayerServer(),()->{if(phase==done){if(phase+1<limit())next(mc.getSingleplayerServer(),phase+1);else phase=limit();}});
  }
  private static void catalog(){
    try{
      Files.createDirectories(OUTPUT);var root=new com.google.gson.JsonObject();root.addProperty("kind","Actual production TV/prone client study, no alternate pose or renderer");root.addProperty("normal_tps",20);root.addProperty("audio_listening",false);root.addProperty("start",start());root.addProperty("limit_exclusive",limit());var rows=new com.google.gson.JsonArray();
      for(int i=0;i<CASES.size();i++){var c=CASES.get(i);var r=new com.google.gson.JsonObject();r.addProperty("index",i);r.addProperty("name",c.name());r.addProperty("toy",c.kind());r.addProperty("variant",c.variant());r.addProperty("mode",c.mode());r.addProperty("natural",c.natural());r.addProperty("start_side",c.startSide().getSerializedName());r.addProperty("toy_facing",FACING.getSerializedName());r.addProperty("initial_yaw",c.initialYaw());r.addProperty("interrupt",c.interrupt());r.addProperty("interrupt_tick",c.interruptAt());r.addProperty("finish_tick",c.finish());r.addProperty("first_type",first(c));r.addProperty("alignment",alignment(c));r.addProperty("adult",adult(c));r.addProperty("maturity",adult(c)?1:0);r.addProperty("run_power",rp(c));r.addProperty("swim_fly",sf(c));r.addProperty("coat",c.coat());r.addProperty("hat",c.hat());r.addProperty("rabbit",c.rabbit());r.addProperty("expected_client_sound",c.sound());r.addProperty("expected_client_sound_count",c.expectedSounds());var moments=new com.google.gson.JsonArray();for(int t:c.shotTicks())moments.add(t);r.add("shot_ticks",moments);rows.add(r);}root.add("cases",rows);Files.writeString(OUTPUT.resolve("case-catalog.json"),new com.google.gson.GsonBuilder().setPrettyPrinting().create().toJson(root)+"\n");
    }catch(Exception error){throw new IllegalStateException(error);}
  }
  @SubscribeEvent public static void client(ClientTickEvent.Post event){
    if(!enabled()||finished)return;var mc=Minecraft.getInstance();ticks++;if(fatal!=null)throw new IllegalStateException("TOYS073 asynchronous server failure: "+fatal);if(wallStart==0)wallStart=System.nanoTime();require(System.nanoTime()-wallStart<2_400_000_000_000L,"study wall timeout");
    if(mc.screen instanceof AccessibilityOnboardingScreen)mc.setScreen(new TitleScreen());
    if(!started&&mc.screen instanceof TitleScreen){started=true;catalog();mc.options.renderDistance().set(3);mc.options.simulationDistance().set(5);mc.options.framerateLimit().set(30);mc.options.fov().set(45);mc.options.pauseOnLostFocus=false;mc.options.hideGui=true;mc.options.tutorialStep=net.minecraft.client.tutorial.TutorialSteps.NONE;mc.getWindow().setWindowed(Math.clamp(parameter("CHAO_TOYS073_WIDTH",1440),854,2560),Math.clamp(parameter("CHAO_TOYS073_HEIGHT",900),480,1440));mc.resizeDisplay();mc.createWorldOpenFlows().createFreshLevel("ToyWatch073-"+System.currentTimeMillis(),new LevelSettings("ToyWatch073",GameType.CREATIVE,false,Difficulty.PEACEFUL,true,new GameRules(),WorldDataConfiguration.DEFAULT),new WorldOptions(2573L,false,false),r->r.registryOrThrow(Registries.WORLD_PRESET).getHolderOrThrow(WorldPresets.FLAT).value().createWorldDimensions(),mc.screen);}
    if(!setup&&mc.player!=null&&mc.getSingleplayerServer()!=null){setup=true;mc.setScreen(null);ChaoRenderer.suppressNames=true;onServer(mc.getSingleplayerServer(),()->{var server=mc.getSingleplayerServer();var level=server.overworld();level.setDayTime(6000);level.getGameRules().getRule(GameRules.RULE_DAYLIGHT).set(false,server);level.getGameRules().getRule(GameRules.RULE_DOMOBSPAWNING).set(false,server);for(var p:BlockPos.betweenClosed(-9,79,-9,9,86,11))level.setBlock(p,p.getY()==79?Blocks.SMOOTH_STONE.defaultBlockState():Blocks.AIR.defaultBlockState(),3);server.getPlayerList().getPlayers().forEach(p->p.setGameMode(GameType.SPECTATOR));next(server,start());});}
    if(phase>=0&&phase<limit()&&mc.level!=null){if(clientPhase==-2&&phase==pendingPhase)return;if(clientPhase!=phase){clientPhase=phase;view=1;viewFrames=0;armRequested=false;lastCaptureNanos=0;}if(!armed&&!sourceReady(mc)){require(System.nanoTime()-playbackWall<30_000_000_000L,"source packet did not arrive");return;}camera(mc);}
    if(phase==limit()&&SAVED.get()==shots){finished=true;onServer(mc.getSingleplayerServer(),()->mc.getSingleplayerServer().tickRateManager().setFrozen(false));LogUtils.getLogger().info("TOYS073_VISUAL_COMPLETE requestedCases={} renderedCases={} skippedCases=0 captures={} saved={} normal20TPS=true audioListening=false epochMillis={} output={}",limit()-start(),limit()-start(),shots,SAVED.get(),System.currentTimeMillis(),OUTPUT);mc.stop();}
  }
}
