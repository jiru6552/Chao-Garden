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

/** Disposable actual-client radio study. Uses production poses, clocks, AI and commands only. */
@EventBusSubscriber(modid=ChaoGarden.MODID,value=Dist.CLIENT)
public final class RadioToyVisualSmoke075 {
  private record Case(String name,String toy,String variant,String mode,Direction startSide,Direction facing,String interrupt,String coat,String hat,boolean rabbit,float maturity){
    String kind(){return "chao_"+toy;}
    boolean radio(){return toy.equals("radio");}
    boolean tv(){return toy.equals("tv");}
    boolean ball(){return toy.equals("ball");}
    boolean placed(){return radio()||tv()||ball();}
    boolean prone(){return toy.equals("toy_car")||toy.equals("crayons");}
    boolean group(){return mode.equals("group");}
    float initialYaw(){return facing.getOpposite().toYRot();}
    boolean natural(){return mode.equals("natural");}
    boolean loop(){return mode.equals("loop");}
    boolean preview(){return !natural();}
    int interruptAt(){return loop()?200:90;}
    int finish(){return loop()?224:184;}
    int[] shotTicks(){return loop()?new int[]{8,40,74,120,150,180,196,218}:!interrupt.isEmpty()?new int[]{8,40,74,88,96,120,176}:new int[]{8,40,74,120,150,176};}
    String sound(){return radio()?"minecraft:block.note_block.harp":tv()?"minecraft:block.note_block.bit":ball()?"minecraft:block.slime_block.step":toy.equals("toy_car")?"chao_garden:toy.car":"";}
    int expectedSounds(){return radio()?(loop()?5:!interrupt.isEmpty()?3:4):tv()?1:ball()?-1:toy.equals("toy_car")?4:0;}
  }
  private static List<Case> cases(){
    var all=new ArrayList<Case>();
    all.add(new Case("radio_baby_from_front","radio","baby_neutral","natural",Direction.SOUTH,Direction.SOUTH,"","natural","",false,0f));
    all.add(new Case("radio_baby_from_back","radio","baby_neutral","natural",Direction.NORTH,Direction.SOUTH,"","natural","",false,0f));
    all.add(new Case("radio_baby_from_east","radio","baby_neutral","natural",Direction.EAST,Direction.SOUTH,"","natural","",false,0f));
    all.add(new Case("radio_baby_from_west","radio","baby_neutral","natural",Direction.WEST,Direction.SOUTH,"","natural","",false,0f));
    all.add(new Case("radio_dark_run_from_front","radio","dark_run_run_fly","natural",Direction.SOUTH,Direction.SOUTH,"","natural_shiny","",false,1f));
    all.add(new Case("radio_dark_run_from_back","radio","dark_run_run_fly","natural",Direction.NORTH,Direction.SOUTH,"","natural_shiny","",false,1f));
    all.add(new Case("radio_dark_run_from_east","radio","dark_run_run_fly","natural",Direction.EAST,Direction.SOUTH,"","natural_shiny","",false,1f));
    all.add(new Case("radio_dark_run_from_west","radio","dark_run_run_fly","natural",Direction.WEST,Direction.SOUTH,"","natural_shiny","",false,1f));
    all.add(new Case("radio_hero_swim_facing_east","radio","hero_swim_run_swim","natural",Direction.NORTH,Direction.EAST,"","natural","",false,1f));
    all.add(new Case("radio_neutral_fly_facing_west","radio","neutral_fly_swim","natural",Direction.EAST,Direction.WEST,"","natural","",false,1f));
    all.add(new Case("radio_hero_baby_once","radio","baby_hero","once",Direction.SOUTH,Direction.SOUTH,"","natural","",false,0f));
    all.add(new Case("radio_hero_power_hat","radio","hero_power_power_fly","once",Direction.NORTH,Direction.NORTH,"","natural","pumpkin",false,1f));
    all.add(new Case("radio_shiny_rabbit_baby","radio","baby_neutral","once",Direction.SOUTH,Direction.SOUTH,"","red_shiny","",true,0f));
    all.add(new Case("radio_hero_run_loop_stop","radio","hero_run_power_fly","loop",Direction.SOUTH,Direction.SOUTH,"stop","natural","",false,.5f));
    all.add(new Case("radio_neutral_swim_damage","radio","neutral_swim_fly","once",Direction.SOUTH,Direction.SOUTH,"damage","natural","",false,1f));
    all.add(new Case("radio_dark_fly_remove","radio","dark_fly_run_swim","once",Direction.SOUTH,Direction.SOUTH,"remove","green_mono","",false,1f));
    all.add(new Case("radio_dark_power_displace","radio","dark_power_run_swim","once",Direction.SOUTH,Direction.SOUTH,"displace","natural","",false,1f));
    all.add(new Case("radio_nearby_pair","radio","hero_power_power_fly","group",Direction.SOUTH,Direction.SOUTH,"","natural","",false,1f));
    all.add(new Case("regression_tv_hero_swim","tv","hero_swim_run_swim","once",Direction.SOUTH,Direction.SOUTH,"","natural","",false,1f));
    all.add(new Case("regression_car_baby","toy_car","baby_neutral","once",Direction.SOUTH,Direction.SOUTH,"","natural","",false,0f));
    all.add(new Case("regression_crayons_hero_power","crayons","hero_power_power_fly","once",Direction.SOUTH,Direction.SOUTH,"","natural","",false,1f));
    all.add(new Case("regression_ball_dark_run","ball","dark_run_run_fly","once",Direction.SOUTH,Direction.SOUTH,"","natural_shiny","",false,1f));
    require(all.size()==22,"authored catalog count changed");
    String selected=System.getenv("CHAO_RADIO075_CASES");
    if(selected!=null&&!selected.isBlank()){
      var chosen=new ArrayList<Case>();var used=new HashSet<Integer>();
      for(String text:selected.split(",")){int index=Integer.parseInt(text.trim());require(index>=0&&index<all.size()&&used.add(index),"invalid/duplicate authored case");chosen.add(all.get(index));}
      return List.copyOf(chosen);
    }
    return List.copyOf(all);
  }
  private static final List<Case> CASES=cases();
  private static final BlockPos TOY=new BlockPos(0,80,2);
  private static final BlockPos SECOND_TOY=TOY.east(3);
  private static final Path OUTPUT=Path.of(System.getenv().getOrDefault("CHAO_RADIO075_OUTPUT","/workspace/.setup/radio075-client-final")).toAbsolutePath().normalize();
  private static final int[] WORLD=new int[CASES.size()],RENDER=new int[CASES.size()],ACTIVE=new int[CASES.size()],IDLE=new int[CASES.size()],PRONE_CHECKS=new int[CASES.size()],FRAME_MASK=new int[CASES.size()],EXIT_FLOOR=new int[CASES.size()],CANCEL_SERVER=new int[CASES.size()],CANCEL_RENDER=new int[CASES.size()];
  private static final int[] EXTRA_RENDER=new int[CASES.size()],EXTRA_ACTIVE=new int[CASES.size()],EXTRA_IDLE=new int[CASES.size()],MOVES=new int[CASES.size()];
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
  private static volatile UUID actorId,extraId;
  private static volatile long elapsed=-1,caseAge;
  private static volatile boolean armed,complete;
  private static volatile String fatal;
  private static ChaoEntity actor,extra;
  private static String extraSavedState;
  private static BlockPos ballPos=TOY;
  private static Vec3 sessionAnchor;
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

  private static boolean enabled(){return "true".equalsIgnoreCase(System.getenv("CHAO_RADIO075_SMOKE"));}
  private static int parameter(String name,int fallback){try{return Integer.parseInt(System.getenv().getOrDefault(name,Integer.toString(fallback)));}catch(NumberFormatException ignored){return fallback;}}
  private static int limit(){return Math.clamp(parameter("CHAO_RADIO075_LIMIT",CASES.size()),1,CASES.size());}
  private static int start(){return Math.clamp(parameter("CHAO_RADIO075_START",0),0,limit()-1);}
  private static void require(boolean okay,String message){if(!okay)throw new IllegalStateException("RADIO075 "+message);}
  private static void onServer(MinecraftServer server,Runnable task){server.execute(()->{try{task.run();}catch(Throwable error){fatal=error.toString();LogUtils.getLogger().error("RADIO075_ASYNC_FAILURE",error);}});}
  private static String sha(Path file){try{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(file)));}catch(Exception error){throw new IllegalStateException(error);}}
  private static int first(Case c){return switch(c.variant()){case "neutral_swim_fly","hero_swim_run_swim"->ChaoState.SWIM_TYPE;case "neutral_fly_swim","dark_fly_run_swim"->ChaoState.FLY_TYPE;case "hero_power_power_fly","dark_power_run_swim"->ChaoState.POWER_TYPE;case "dark_run_run_fly","hero_run_power_fly"->ChaoState.RUN_TYPE;default->ChaoState.NORMAL;};}
  private static float alignment(Case c){return c.variant().startsWith("hero_")||c.variant().equals("baby_hero")?1:c.variant().startsWith("dark_")?-1:0;}
  private static boolean adult(Case c){return !c.variant().startsWith("baby_");}
  private static int rp(Case c){return c.variant().contains("power_fly")?1:c.variant().endsWith("_run_swim")||c.variant().endsWith("_run_fly")?-1:0;}
  private static int sf(Case c){return c.variant().endsWith("_swim")?-1:c.variant().endsWith("_fly")?1:0;}
  /** Build only ordinary saved state; the real renderer chooses every model and attachment. */
  private static ChaoState fixture(Case entry){
    var s=new ChaoState();s.adult=adult(entry);s.align=alignment(entry);s.primaryType=first(entry);s.hunger=100;s.energy=100;s.happiness=20;s.boredom=100;s.personality=ChaoPreferences.EASILY_BORED;
    s.setEvolutionStrength(s.adult?entry.maturity()*GradualEvolutionRules.ADULT_STRENGTH_MAX:0);
    if(s.adult){s.train(ChaoState.RUN,1200,new Random(7501));s.train(ChaoState.POWER,1200,new Random(7502));}
    if(rp(entry)!=0)s.train(rp(entry)<0?ChaoState.RUN:ChaoState.POWER,960,new Random(7503));
    if(sf(entry)!=0)s.train(sf(entry)<0?ChaoState.SWIM:ChaoState.FLY,960,new Random(7504));
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
  private static void command(MinecraftServer server,String mode){command(server,mode,actor);}
  private static void command(MinecraftServer server,String mode,ChaoEntity target){
    var p=server.getPlayerList().getPlayers().getFirst();
    // Move only the operator fixture, so native nearest-selection/12b permission checks remain real.
    p.teleportTo(target.getX(),target.getY()+2,target.getZ());p.setDeltaMovement(Vec3.ZERO);
    try{require(server.getCommands().getDispatcher().execute("chao testtoy "+CASES.get(phase).toy()+" "+mode,p.createCommandSourceStack().withPermission(2))==1,"preview dispatcher rejected "+mode);}
    catch(com.mojang.brigadier.exceptions.CommandSyntaxException error){throw new IllegalStateException(error);}
  }
  private static Case secondaryCase(){return new Case("radio_pair_secondary","radio","dark_run_run_fly","once",Direction.SOUTH,Direction.SOUTH,"","natural_shiny","",false,1f);}
  private static void next(MinecraftServer server,int index){
    server.tickRateManager().setFrozen(true);var level=server.overworld();
    if(actor!=null){ToyPreview.stop(actor);ToyContent.endPlaying(actor,false);actor.discard();actor=null;}
    if(extra!=null){ToyPreview.stop(extra);ToyContent.endPlaying(extra,false);extra.discard();extra=null;}extraId=null;
    for(var pos:BlockPos.betweenClosed(-12,80,-10,12,82,14))if(level.getBlockState(pos).getBlock()instanceof ChaoToyBlock)level.setBlock(pos,Blocks.AIR.defaultBlockState(),3);
    for(var item:level.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,new net.minecraft.world.phys.AABB(-12,79,-10,13,85,15)))item.discard();
    var c=CASES.get(index);if(c.placed())level.setBlock(TOY,ToyContent.block(c.kind()).defaultBlockState().setValue(ChaoToyBlock.FACING,c.facing()),3);
    actor=ChaoGarden.CHAO.get().create(level);require(actor!=null,"actor create failed");actor.setState(fixture(c));actor.setInvulnerable(true);actor.deferBehavior(100000);originalNoAi=c.loop()||c.group();actor.setNoAi(originalNoAi);actor.getRandom().setSeed(18);
    Vec3 origin=c.natural()?Vec3.atBottomCenterOf(TOY.relative(c.startSide(),3)):c.ball()?Vec3.atBottomCenterOf(TOY).add(0,0,BallPlayRules074.contactDistance(actor.state())+.10):c.placed()?ToyContent.stationaryAnchor(actor,TOY,TOY.relative(c.facing())):new Vec3(.5,80,3.5);
    float yaw=c.initialYaw();actor.moveTo(origin.x,origin.y,origin.z,yaw,0);actor.yBodyRot=actor.yBodyRotO=actor.yHeadRot=actor.yHeadRotO=yaw;actor.setOnGround(true);
    if(!c.hat().isEmpty())actor.setItemSlot(EquipmentSlot.HEAD,new ItemStack(Items.CARVED_PUMPKIN));
    level.addFreshEntity(actor);actor.goalSelector.removeAllGoals(g->true);actor.targetSelector.removeAllGoals(g->true);
    naturalGoal=c.natural()?new ToyContent.PlayGoal(actor):null;if(naturalGoal!=null)actor.goalSelector.addGoal(4,naturalGoal);else actor.getPersistentData().putLong("ChaoToyNextPlay",Long.MAX_VALUE);
    savedState=actor.state().encode();savedCooldown=actor.getPersistentData().getLong("ChaoToyNextPlay");savedHat=actor.getItemBySlot(EquipmentSlot.HEAD).copy();savedOffhand=actor.getItemBySlot(EquipmentSlot.OFFHAND).copy();savedFirst=actor.state().lockedPrimaryType();savedAlign=actor.state().lockedAlignment();
    initialPosition=previousPosition=actor.position();walked=0;spawned=lastTick=level.getGameTime();began=-1;elapsed=-1;caseAge=0;interrupted=previewProof=armed=complete=false;ballPos=TOY;sessionAnchor=null;
    SHOTS[index]=new boolean[c.shotTicks().length];actorId=actor.getUUID();phase=index;
    var p=server.getPlayerList().getPlayers().getFirst();p.setGameMode(GameType.SPECTATOR);p.teleportTo(7.5,82,7.5);p.setNoGravity(true);p.setDeltaMovement(Vec3.ZERO);
    if(c.group()){
      level.setBlock(SECOND_TOY,ToyContent.block("chao_radio").defaultBlockState().setValue(ChaoToyBlock.FACING,c.facing()),3);
      extra=ChaoGarden.CHAO.get().create(level);require(extra!=null,"secondary actor create failed");extra.setState(fixture(secondaryCase()));extra.setInvulnerable(true);extra.deferBehavior(100000);extra.setNoAi(true);extra.getRandom().setSeed(19);
      var at=ToyContent.stationaryAnchor(extra,SECOND_TOY,SECOND_TOY.relative(c.facing()));extra.moveTo(at.x,at.y,at.z,yaw,0);extra.yBodyRot=extra.yBodyRotO=extra.yHeadRot=extra.yHeadRotO=yaw;extra.setOnGround(true);level.addFreshEntity(extra);extra.goalSelector.removeAllGoals(g->true);extra.targetSelector.removeAllGoals(g->true);extra.getPersistentData().putLong("ChaoToyNextPlay",Long.MAX_VALUE);extraSavedState=extra.state().encode();extraId=extra.getUUID();
      LogUtils.getLogger().info("RADIO075_EXTRA_CASE {} state={} epochMillis={}",index,extraSavedState,System.currentTimeMillis());
    }
    playbackWall=System.nanoTime();LogUtils.getLogger().info("RADIO075_CASE {} name={} toy={} variant={} mode={} startSide={} facing={} interrupt={} firstType={} lockedAlignment={} adult={} maturity={} rp={} sf={} coat={} hat={} rabbit={} group={} state={} epochMillis={}",index,c.name(),c.kind(),c.variant(),c.mode(),c.startSide(),c.facing(),c.interrupt(),savedFirst,savedAlign,actor.state().adult,actor.state().gradualVisualStrength(),rp(c),sf(c),c.coat(),c.hat(),c.rabbit(),c.group(),savedState,System.currentTimeMillis());
  }
  private static void arm(MinecraftServer server,int expected){
    if(phase!=expected||armed)return;require(Math.abs(server.tickRateManager().tickrate()-20)<.00001f,"normal20TPS required");
    var c=CASES.get(phase);actor.setOnGround(true);if(c.preview()){command(server,c.loop()?"loop":"once");if(c.group()){extra.setOnGround(true);command(server,"once",extra);}began=server.overworld().getGameTime();elapsed=0;sessionAnchor=actor.position();}
    armed=true;playbackWall=System.nanoTime();server.tickRateManager().setFrozen(false);LogUtils.getLogger().info("RADIO075_ARMED {} name={} natural={} epochMillis={} normal20TPS=true",phase,c.name(),c.natural(),System.currentTimeMillis());
  }
  private static void naturalRoute(){
    if(actor==null||naturalGoal==null)return;var path=actor.getNavigation().getPath();
    if(path!=null)for(int i=0;i<path.getNodeCount();i++){var node=path.getNodePos(i);require(!node.equals(TOY)&&!node.equals(TOY.above()),"natural path enters cabinet/jump node "+node);}
    walked+=actor.position().subtract(previousPosition).horizontalDistance();previousPosition=actor.position();
  }
  @SubscribeEvent public static void server(ServerTickEvent.Post event){
    if(!enabled()||phase<0||phase>=limit()||!armed||complete)return;var server=event.getServer();var level=server.overworld();long now=level.getGameTime();if(now==lastTick)return;lastTick=now;caseAge=now-spawned;var c=CASES.get(phase);
    require(actor!=null&&actor.isAlive()&&!actor.isRemoved(),"live actor disappeared");require(actor.state().adult==adult(c)&&!actor.isCocooned(),"fixture changed real life stage");
    if(adult(c))require(actor.state().lockedPrimaryType()==savedFirst&&actor.state().lockedAlignment()==savedAlign,"adult identity changed");
    require(caseAge<2800,"approach/session deadline exceeded pos="+actor.position()+" walked="+walked+" nav="+actor.getNavigation().getTargetPos());
    if(began<0){
      naturalRoute();if(ToyContent.isPlaying(actor)){
        began=actor.getPersistentData().getLong("ChaoToyUntil")-160;playbackWall=System.nanoTime();
        Vec3 wanted=c.radio()?ToyContent.radioAnchor(actor,TOY):ToyContent.stationaryAnchor(actor,TOY,TOY.relative(c.facing()));require(actor.position().distanceToSqr(wanted)<.0025,"natural actor did not arrive at front anchor");sessionAnchor=actor.position();
        require(walked>.35,"natural route bypassed");LogUtils.getLogger().info("RADIO075_NATURAL_STARTED {} name={} waitedTicks={} walked={} start={} arrived={} expectedAnchor={} epochMillis={}",phase,c.name(),now-spawned,walked,initialPosition,actor.position(),wanted,System.currentTimeMillis());
      }else if(caseAge%40==0)LogUtils.getLogger().info("RADIO075_APPROACH case={} age={} pos={} walked={} target={} pathDone={} epochMillis={}",phase,caseAge,actor.position(),walked,actor.getNavigation().getTargetPos(),actor.getNavigation().isDone(),System.currentTimeMillis());
    }
    if(began<0)return;elapsed=now-began;
    if(c.preview()&&!previewProof){require(savedState.equals(actor.state().encode()),"preview changed saved state before terminal/interruption");require(actor.getPersistentData().getLong("ChaoToyNextPlay")==savedCooldown,"preview changed ordinary cooldown");if(!ToyPreview.active(actor))previewProof=true;}
    if(c.group()){
      require(extra!=null&&extra.isAlive(),"group actor missing");if(ToyPreview.active(extra))require(extraSavedState.equals(extra.state().encode()),"secondary preview changed saved state");
      require(extra.state().lockedPrimaryType()==ChaoState.RUN_TYPE&&extra.state().lockedAlignment()==-1,"secondary locked identity changed");
    }
    if(c.ball()&&actor.toyTarget()!=null&&!actor.toyTarget().equals(ballPos)){
      var next=actor.toyTarget();require(ballPos.distManhattan(next)==1&&level.getBlockState(next).is(ToyContent.block("chao_ball"))&&level.getBlockState(ballPos).isAir(),"ball regression did not commit one real cell");ballPos=next;MOVES[phase]++;LogUtils.getLogger().info("RADIO075_MOVE case={} move={} target={} elapsed={} epochMillis={}",phase,MOVES[phase],next.asLong(),elapsed,System.currentTimeMillis());
    }
    if(!interrupted&&!c.interrupt().isEmpty()&&elapsed>=c.interruptAt()){
      require(!c.preview()||savedState.equals(actor.state().encode()),"state changed before deliberate interruption");previewProof=c.preview();
      if(c.interrupt().equals("stop"))command(server,"stop");else if(c.interrupt().equals("damage")){actor.setInvulnerable(false);require(actor.hurt(level.damageSources().generic(),1),"real damage refused");}else if(c.interrupt().equals("displace"))actor.setPos(actor.getX()+6,actor.getY(),actor.getZ());else require(level.destroyBlock(TOY,false),"real radio removal refused");
      interrupted=true;interruptHappiness=actor.state().happiness;LogUtils.getLogger().info("RADIO075_INTERRUPT case={} kind={} tick={} state={} epochMillis={}",phase,c.interrupt(),elapsed,actor.state().encode(),System.currentTimeMillis());
    }
    if(interrupted&&elapsed>=c.interruptAt()+2){
      require(!ToyPreview.active(actor)&&actor.toyPlayingTicks()==0&&actor.toyTarget()==null,"interrupted session did not clear");
      var source=level.getBlockState(TOY);if(c.interrupt().equals("remove"))require(source.isAir(),"removed radio returned");else require(source.is(ToyContent.block(c.kind()))&&!source.getValue(ChaoToyBlock.PLAYING)&&source.getValue(ChaoToyBlock.FRAME)==0,"interrupted radio remained playing");CANCEL_SERVER[phase]++;
    }
    if((c.radio()||c.tv())&&ToyContent.isPlaying(actor)&&!interrupted){
      var source=level.getBlockState(TOY);require(source.is(ToyContent.block(c.kind()))&&source.getValue(ChaoToyBlock.FACING)==c.facing(),"playing source/facing changed");
      Vec3 target=(c.radio()?ToyContent.radioSpeaker(actor,TOY):Vec3.atBottomCenterOf(TOY).add(0,.65,0)).subtract(actor.position()).multiply(1,0,1).normalize();require(Vec3.directionFromRotation(0,actor.getYRot()).dot(target)>.985,"listener not facing source");
      Vec3 wanted=c.radio()?ToyContent.radioAnchor(actor,TOY):ToyContent.stationaryAnchor(actor,TOY,TOY.relative(c.facing()));require(actor.position().distanceToSqr(wanted)<.0025,"listener drifted from front seat");
    }
    if(elapsed<c.finish())return;if(!c.interrupt().isEmpty())require(CANCEL_SERVER[phase]>0,"bounded cancellation not observed");
    require(!ToyPreview.active(actor)&&actor.toyPlayingTicks()==0&&actor.toyTarget()==null,"session left preview/clock/reservation");require(actor.getHeldChaoItem().isEmpty()&&actor.getMainHandItem().isEmpty(),"real item minted");require(ItemStack.matches(savedHat,actor.getItemBySlot(EquipmentSlot.HEAD))&&ItemStack.matches(savedOffhand,actor.getItemBySlot(EquipmentSlot.OFFHAND)),"equipment changed");require(actor.isNoAi()==originalNoAi&&!actor.isNoGravity(),"temporary AI/gravity not restored");
    if(c.preview()){require(previewProof,"preview terminal state not witnessed");require(actor.getPersistentData().getLong("ChaoToyNextPlay")==savedCooldown,"preview cooldown changed after cleanup");require(actor.state().happiness==(c.interrupt().equals("damage")?interruptHappiness:20),"preview granted happiness reward");}else require(actor.state().happiness==24,"natural reward not exactly once");
    if(c.placed()){
      var source=level.getBlockState(c.ball()?ballPos:TOY);if(c.interrupt().equals("remove"))require(source.isAir(),"removed source returned");else require(source.is(ToyContent.block(c.kind()))&&!source.getValue(ChaoToyBlock.PLAYING)&&source.getValue(ChaoToyBlock.FRAME)==0,"placed toy did not return idle");
      if(c.ball()){require(MOVES[phase]>0,"ball regression never moved");require(!source.getValue(ChaoBallBlock074.ROLLING),"ball regression did not park");}
    }else require(level.getBlockState(TOY).isAir(),"hand preview created placed toy");
    require(level.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,new net.minecraft.world.phys.AABB(-12,79,-10,13,85,15)).isEmpty(),"interaction minted dropped item");
    var tag=new CompoundTag();actor.saveWithoutId(tag);var copy=ChaoGarden.CHAO.get().create(level);copy.load(tag);require(copy.state().encode().equals(actor.state().encode()),"completed actor NBT changed");copy.discard();
    if(c.group()){
      require(!ToyPreview.active(extra)&&extra.toyPlayingTicks()==0&&extra.toyTarget()==null&&extra.isNoAi()&&!extra.isNoGravity(),"group secondary cleanup/AI failed");require(extra.state().happiness==20&&extra.getPersistentData().getLong("ChaoToyNextPlay")==Long.MAX_VALUE&&extra.getHeldChaoItem().isEmpty()&&extra.getMainHandItem().isEmpty(),"group secondary granted reward/item or changed cooldown");var second=level.getBlockState(SECOND_TOY);require(second.is(ToyContent.block("chao_radio"))&&!second.getValue(ChaoToyBlock.PLAYING)&&second.getValue(ChaoToyBlock.FRAME)==0,"second radio not idle");var t=new CompoundTag();extra.saveWithoutId(t);var other=ChaoGarden.CHAO.get().create(level);other.load(t);require(other.state().encode().equals(extra.state().encode()),"secondary NBT changed");other.discard();LogUtils.getLogger().info("RADIO075_EXTRA_NATIVE_COMPLETE {} state={} epochMillis={}",phase,extra.state().encode(),System.currentTimeMillis());
    }
    complete=true;server.tickRateManager().setFrozen(true);LogUtils.getLogger().info("RADIO075_NATIVE_COMPLETE {} name={} elapsed={} walked={} previewPlaybackStatePreserved={} originalNoAi={} cancelServerChecks={} moves={} group={} state={} epochMillis={}",phase,c.name(),elapsed,walked,previewProof,originalNoAi,CANCEL_SERVER[phase],MOVES[phase],c.group(),actor.state().encode(),System.currentTimeMillis());
  }

  @SubscribeEvent public static void frameBegin(RenderFrameEvent.Pre e){if(enabled())renderedThisFrame=worldThisFrame=false;}
  private static ChaoEntity observed(Minecraft mc){return observed(mc,actorId);}
  private static ChaoEntity observed(Minecraft mc,UUID id){if(mc.level!=null&&id!=null)for(var e:mc.level.entitiesForRendering())if(e instanceof ChaoEntity c&&c.getUUID().equals(id))return c;return null;}
  private static boolean sourceReady(Minecraft mc){if(mc.level==null)return false;var c=CASES.get(phase);var state=mc.level.getBlockState(TOY);return c.placed()?state.is(ToyContent.block(c.kind()))&&state.getValue(ChaoToyBlock.FACING)==c.facing()&&!state.getValue(ChaoToyBlock.PLAYING):state.isAir();}
  private static int shotView(Case c,int tick){return tick==74?0:tick==120||tick==196?2:1;}
  private static void camera(Minecraft mc){
    var c=observed(mc);if(c==null)return;if(observer==null||observer.level()!=mc.level){observer=new ArmorStand(mc.level,0,0,0);observer.setInvisible(true);observer.setNoGravity(true);}
    var entry=CASES.get(phase);int wanted=1;if(elapsed>=0){var moments=entry.shotTicks();for(int n=0;n<moments.length;n++)if(!SHOTS[phase][n]){wanted=shotView(entry,moments[n]);break;}}if(wanted!=view){view=wanted;viewFrames=0;}
    Vec3 forward=Vec3.directionFromRotation(0,entry.facing().getOpposite().toYRot()),side=new Vec3(-forward.z,0,forward.x),focus=c.position().add(0,.35,0),eye;
    if(entry.group()){
      var other=observed(mc,extraId);if(other!=null)focus=focus.lerp(other.position().add(0,.35,0),.5);
      else focus=focus.add(1.5,0,0);
      eye=focus.add(side.scale(view==1?4.4:view==0?.3:-1.6)).add(forward.scale(view==0?4:view==1?.4:-4)).add(0,view==1?1.2:1.6,0);
    }else if(entry.interrupt().equals("displace")&&elapsed>=entry.interruptAt()){
      // After intentional displacement, frame the actor rather than the remote toy midpoint.
      eye=focus.add(side.scale(view==0?.55:view==1?2.5:-1.65)).add(forward.scale(view==0?2.4:view==2?-2.6:.08)).add(0,view==1?.55:1.1,0);
    }else if(entry.natural()&&elapsed<0){
      // Observe the real walk broadside: a fixed close side camera can end up inside an east/west approach.
      Vec3 source=Vec3.atBottomCenterOf(TOY).add(0,.18,0),route=source.subtract(c.position()).multiply(1,0,1);
      double span=route.length();Vec3 broadside=span>.01?new Vec3(-route.z,0,route.x).scale(1/span):side;
      if(broadside.dot(side)<0)broadside=broadside.scale(-1);
      focus=focus.lerp(source,.5);eye=focus.add(broadside.scale(3.2+span*.8)).add(0,1.25+span*.12,0);
    }else if(entry.placed()){
      Vec3 source=Vec3.atBottomCenterOf(entry.ball()?ballPos:TOY).add(0,entry.radio()?.18:.42,0);if(entry.ball()&&mc.level.getBlockEntity(ballPos)instanceof BallToyBlockEntity074 ball)source=source.add(ball.offset(0));focus=focus.lerp(source,.5);
      eye=focus.add(side.scale(view==0?.65:view==1?2.5:-1.4)).add(forward.scale(view==0?2.4:view==1?.06:-2.8)).add(0,view==1?.55:1.2,0);
    }else{
      focus=focus.add(forward.scale(.17));eye=focus.add(forward.scale(view==0?1.8:view==1?.08:-.35)).add(side.scale(view==0?.55:view==1?1.85:-1.85)).add(0,view==1?.12:view==2?.60:.66,0);
    }
    var d=focus.subtract(eye);float yaw=(float)(Math.toDegrees(Math.atan2(d.z,d.x))-90),pitch=(float)-Math.toDegrees(Math.atan2(d.y,Math.hypot(d.x,d.z)));observer.moveTo(eye.x,eye.y-observer.getEyeHeight(),eye.z,yaw,pitch);observer.xo=observer.xOld=observer.getX();observer.yo=observer.yOld=observer.getY();observer.zo=observer.zOld=observer.getZ();observer.yRotO=yaw;observer.xRotO=pitch;observer.setYHeadRot(yaw);observer.yHeadRotO=yaw;mc.options.setCameraType(CameraType.FIRST_PERSON);mc.setCameraEntity(observer);
  }
  @SubscribeEvent public static void restoreCamera(ClientTickEvent.Pre e){if(enabled()){var mc=Minecraft.getInstance();if(mc.player!=null&&mc.getCameraEntity()!=mc.player)mc.setCameraEntity(mc.player);}}
  @SubscribeEvent public static void rendered(RenderLivingEvent.Post<?,?> event){
    if(!enabled()||phase<0||phase>=limit()||SceneShaderPack.shadows()||!(event.getEntity()instanceof ChaoEntity c))return;
    boolean secondary=extraId!=null&&c.getUUID().equals(extraId);if(!secondary&&!c.getUUID().equals(actorId))return;var mc=Minecraft.getInstance();if(mc.screen!=null||mc.getOverlay()!=null||mc.getCameraEntity()!=observer)return;
    var m=(ChaoModel)event.getRenderer().getModel();require(m.officialPosesFinite(),"rendered pose not finite");renderedThisFrame=true;
    if(secondary){EXTRA_RENDER[phase]++;if(elapsed>=0&&ChaoRadioAnimation075.active(c)){EXTRA_ACTIVE[phase]++;float t=160-c.toyPlayingTicks()+event.getPartialTick();if(t>=28&&t<=132)require(m.officialMotion()==OfficialChaoRig.Motion.SIT,"secondary radio lost seated pose");}else if(elapsed>=166&&c.getBehavior()==ChaoEntity.Behavior.NONE){EXTRA_IDLE[phase]++;require(c.toyPlayingTicks()==0&&!ChaoRadioAnimation075.active(c),"secondary radio pose survived cleanup");}return;}
    RENDER[phase]++;var entry=CASES.get(phase);if(elapsed<0)return;
    boolean productionActive=entry.radio()?ChaoRadioAnimation075.active(c):entry.tv()?ChaoTvWatchAnimation073.active(c):entry.ball()?ChaoBallAnimation074.active(c):entry.toy().equals("toy_car")?ChaoActiveToyAnimation049.active(c):ChaoQuietToyAnimation049.active(c);
    boolean cancellationExpected=!entry.interrupt().isEmpty()&&elapsed>=entry.interruptAt();if(elapsed>=28&&elapsed<=132&&!cancellationExpected)require(productionActive,"uninterrupted production toy pose ended early");
    if(cancellationExpected&&elapsed>=entry.interruptAt()+6&&elapsed<=entry.interruptAt()+40){
      require(!ChaoRadioAnimation075.active(c)&&!ChaoTvWatchAnimation073.active(c)&&!ChaoActiveToyAnimation049.active(c)&&!ChaoQuietToyAnimation049.active(c)&&!ChaoBallAnimation074.active(c)&&c.toyPlayingTicks()==0,"canceled pose/clock survived client sync");
      if(entry.interrupt().equals("damage"))require(c.hurtTime>0||c.getBehavior()==ChaoEntity.Behavior.COWER,"real damage reaction not rendered");if(entry.interrupt().equals("remove"))require(mc.level.getBlockState(TOY).isAir(),"removed radio still visible");CANCEL_RENDER[phase]++;
    }
    if(c.toyPlayingKind().equals(entry.kind())&&c.toyPlayingTicks()>0&&productionActive){
      ACTIVE[phase]++;
      if(entry.radio()||entry.tv()){
        float t=160-c.toyPlayingTicks()+event.getPartialTick();var source=mc.level.getBlockState(TOY);if(source.is(ToyContent.block(entry.kind()))&&source.getValue(ChaoToyBlock.PLAYING))FRAME_MASK[phase]|=1<<source.getValue(ChaoToyBlock.FRAME);
        if(t>=28&&t<=132)require(m.officialMotion()==OfficialChaoRig.Motion.SIT,"listener did not select sitting motion");
      }else if(entry.prone()){
        float w=GardenToyPerformance049.presence(c,event.getPartialTick());var bounds=ChaoProneToyPose051.runtimeBounds(c,m);
        for(String bone:List.of("body","skull","foot_left",m.officialPoses().containsKey("foot_right")?"foot_right":"feet_bone_right")){var box=ChaoProneToyPose051.worldBounds(m::transformAccessoryAnchor,bounds,bone);if(box!=null)require(box[4]<=ChaoProneToyPose051.FLOOR_Y+.025f,"prone part enters floor "+bone);}
        if(w>.999f){require(Math.abs(m.officialPoses().get("body").rx)>1.15f,"regression not stomach lying");float gap=ChaoProneToyPose051.separation(m::transformAccessoryAnchor,bounds);require(gap>=-.0001f&&Float.isFinite(gap),"head/torso overlap or nonfinite");float cap=(entry.toy().equals("toy_car")?.50f:.25f)/16;if(entry.hat().isEmpty())require(gap<=cap,"prone neck regression "+gap);MAX_NECK_GAP[phase]=Math.max(MAX_NECK_GAP[phase],gap);PRONE_CHECKS[phase]++;}
      }
    }else if(elapsed>=166&&c.getBehavior()==ChaoEntity.Behavior.NONE){IDLE[phase]++;require(!ChaoRadioAnimation075.active(c)&&!ChaoTvWatchAnimation073.active(c)&&!ChaoActiveToyAnimation049.active(c)&&!ChaoQuietToyAnimation049.active(c)&&!ChaoBallAnimation074.active(c)&&c.toyPlayingTicks()==0,"old toy pose survived cleanup");}
    long ending=entry.loop()?200:!entry.interrupt().isEmpty()?90:160;
    if(entry.prone()&&elapsed>=ending&&elapsed<=ending+12){var bounds=ChaoProneToyPose051.runtimeBounds(c,m);for(String bone:List.of("body","skull","foot_left",m.officialPoses().containsKey("foot_right")?"foot_right":"feet_bone_right")){var box=ChaoProneToyPose051.worldBounds(m::transformAccessoryAnchor,bounds,bone);if(box!=null)require(box[4]<=ChaoProneToyPose051.FLOOR_Y+.025f,"outgoing prone enters floor "+bone);}EXIT_FLOOR[phase]++;}
  }
  @SubscribeEvent public static void world(RenderLevelStageEvent event){
    if(!enabled()||phase<0||phase>=limit()||event.getStage()!=RenderLevelStageEvent.Stage.AFTER_LEVEL||SceneShaderPack.shadows())return;var mc=Minecraft.getInstance();if(mc.screen==null&&mc.getOverlay()==null&&mc.getCameraEntity()==observer&&renderedThisFrame)worldThisFrame=true;
  }
  /** Bind the requested instance; success is counted only at actual resolved channel start. */
  @SubscribeEvent public static void soundRequested(net.neoforged.neoforge.client.event.sound.PlaySoundEvent event){
    if(!enabled()||phase<0||phase>=limit()||!armed)return;var sound=event.getOriginalSound();if(sound==null)return;String id=sound.getLocation().toString();var entry=CASES.get(phase);
    if(id.equals(entry.sound()))SOUND_REQUESTS.put(sound,new SoundRequest(phase,elapsed));
    if(entry.radio())require(!id.equals("chao_garden:chao.happy"),"radio emitted session-start happy shout");
  }
  @SubscribeEvent public static void soundStarted(net.neoforged.neoforge.client.event.sound.PlaySoundSourceEvent event){
    if(!enabled())return;var sound=event.getSound();var request=SOUND_REQUESTS.remove(sound);if(request==null)return;
    try{int index=request.index();var entry=CASES.get(index);float volume=sound.getVolume(),pitch=sound.getPitch();int count=SOUNDS.get(index);
      if(entry.radio()){
        require(volume<=.061f&&Math.abs(pitch-RadioPlayRules075.cuePitch(count%4))<.0001f,"radio cue volume/pitch order changed");
        boolean main=Math.abs(sound.getX()-(TOY.getX()+.5))<.01&&Math.abs(sound.getZ()-(TOY.getZ()+.5))<.01;
        boolean second=entry.group()&&Math.abs(sound.getX()-(SECOND_TOY.getX()+.5))<.01&&Math.abs(sound.getZ()-(SECOND_TOY.getZ()+.5))<.01;require(main||second,"radio cue did not originate at actual placed source");
      }else if(entry.tv())require(volume<=.071f&&Math.abs(pitch-1)<.0001f,"TV cue regression");else if(entry.ball())require(volume<=.081f&&Math.abs(pitch-1)<.0001f,"ball cue regression");
      SOUNDS.incrementAndGet(index);LogUtils.getLogger().info("RADIO075_SOUND case={} id={} elapsed={} volume={} pitch={} x={} y={} z={} postResolve=true channelStarted=true epochMillis={}",index,sound.getLocation(),request.elapsed(),volume,pitch,sound.getX(),sound.getY(),sound.getZ(),System.currentTimeMillis());
    }catch(Throwable error){fatal=error.toString();LogUtils.getLogger().error("RADIO075_ASYNC_FAILURE",error);}
  }
  private static void capture(Minecraft mc,String label,boolean approach){
    var entry=CASES.get(phase);int index=phase,angle=view;long tick=elapsed,age=caseAge;double distanceWalked=walked;String name=String.format("radio075-%02d-%s-%s-%s-t%03d-a%04d.png",index,entry.name(),label,new String[]{"front","side","back"}[angle],tick,age);Path file=OUTPUT.resolve("screenshots").resolve(name);
    try{Files.createDirectories(file.getParent());}catch(Exception error){throw new IllegalStateException(error);}
    Screenshot.grab(OUTPUT.toFile(),name,mc.getMainRenderTarget(),message->{require(Files.isRegularFile(file),"PNG did not save");SAVED.incrementAndGet();LogUtils.getLogger().info("RADIO075_CAPTURE {} case={} label={} angle={} tick={} caseAge={} approach={} walked={} sha256={} epochMillis={}",name,index,label,new String[]{"front","side","back"}[angle],tick,age,approach,distanceWalked,sha(file),System.currentTimeMillis());});shots++;lastCaptureNanos=System.nanoTime();viewFrames=0;
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
    require(ACTIVE[phase]>=30&&IDLE[phase]>=3,"active and return-to-idle were not rendered");int expected=entry.ball()?MOVES[phase]:entry.expectedSounds();require(SOUNDS.get(phase)==expected,"client cue count "+SOUNDS.get(phase)+" expected "+expected);
    if(!entry.interrupt().isEmpty())require(CANCEL_RENDER[phase]>0,"bounded client cancellation was not rendered");
    if(entry.radio()||entry.tv()){int mask=!entry.interrupt().isEmpty()&&!entry.loop()?7:15;require((FRAME_MASK[phase]&mask)==mask,"stationary source frames not rendered: "+FRAME_MASK[phase]);}
    else if(entry.prone()){require(PRONE_CHECKS[phase]>10&&EXIT_FLOOR[phase]>0,"prone regression not checked");}
    if(entry.group())require(EXTRA_RENDER[phase]>30&&EXTRA_ACTIVE[phase]>=30&&EXTRA_IDLE[phase]>=3,"secondary active/idle not actually rendered");
    require((System.nanoTime()-playbackWall)/1e9>=entry.finish()/20.0-.8,"study ran faster than normal20TPS");
    LogUtils.getLogger().info("RADIO075_RENDER_COMPLETE {} name={} worldFrames={} actorFrames={} activeFrames={} idleFrames={} sounds={} frameMask={} proneChecks={} maxNeckGap={} outgoingFloorFrames={} cancelRenderFrames={} extraActorFrames={} extraActiveFrames={} extraIdleFrames={} moves={} captures={} normal20TPS=true epochMillis={}",phase,entry.name(),WORLD[phase],RENDER[phase],ACTIVE[phase],IDLE[phase],SOUNDS.get(phase),FRAME_MASK[phase],PRONE_CHECKS[phase],MAX_NECK_GAP[phase],EXIT_FLOOR[phase],CANCEL_RENDER[phase],EXTRA_RENDER[phase],EXTRA_ACTIVE[phase],EXTRA_IDLE[phase],MOVES[phase],moments.length+APPROACH_SHOTS[phase],System.currentTimeMillis());
    int done=phase;pendingPhase=phase;clientPhase=-2;onServer(mc.getSingleplayerServer(),()->{if(phase==done){if(phase+1<limit())next(mc.getSingleplayerServer(),phase+1);else phase=limit();}});
  }
  private static void catalog(){
    try{
      Files.createDirectories(OUTPUT);var root=new com.google.gson.JsonObject();root.addProperty("kind","Actual production radio, regression toys and nearby-radio study; no alternate pose or renderer");root.addProperty("normal_tps",20);root.addProperty("audio_listening",false);root.addProperty("start",start());root.addProperty("limit_exclusive",limit());var rows=new com.google.gson.JsonArray();
      for(int i=0;i<CASES.size();i++){var c=CASES.get(i);var r=new com.google.gson.JsonObject();r.addProperty("index",i);r.addProperty("name",c.name());r.addProperty("toy",c.kind());r.addProperty("variant",c.variant());r.addProperty("mode",c.mode());r.addProperty("natural",c.natural());r.addProperty("start_side",c.startSide().getSerializedName());r.addProperty("toy_facing",c.facing().getSerializedName());r.addProperty("initial_yaw",c.initialYaw());r.addProperty("interrupt",c.interrupt());r.addProperty("interrupt_tick",c.interruptAt());r.addProperty("finish_tick",c.finish());r.addProperty("first_type",first(c));r.addProperty("alignment",alignment(c));r.addProperty("adult",adult(c));r.addProperty("maturity",c.maturity());r.addProperty("run_power",rp(c));r.addProperty("swim_fly",sf(c));r.addProperty("coat",c.coat());r.addProperty("hat",c.hat());r.addProperty("rabbit",c.rabbit());r.addProperty("group",c.group());r.addProperty("expected_client_sound",c.sound());r.addProperty("expected_client_sound_count",c.expectedSounds());var moments=new com.google.gson.JsonArray();for(int t:c.shotTicks())moments.add(t);r.add("shot_ticks",moments);rows.add(r);}root.add("cases",rows);Files.writeString(OUTPUT.resolve("case-catalog.json"),new com.google.gson.GsonBuilder().setPrettyPrinting().create().toJson(root)+"\n");
    }catch(Exception error){throw new IllegalStateException(error);}
  }
  @SubscribeEvent public static void client(ClientTickEvent.Post event){
    if(!enabled()||finished)return;var mc=Minecraft.getInstance();ticks++;if(fatal!=null)throw new IllegalStateException("RADIO075 asynchronous server failure: "+fatal);if(wallStart==0)wallStart=System.nanoTime();require(System.nanoTime()-wallStart<2_400_000_000_000L,"study wall timeout");
    if(mc.screen instanceof AccessibilityOnboardingScreen)mc.setScreen(new TitleScreen());
    if(!started&&mc.screen instanceof TitleScreen){started=true;catalog();mc.options.renderDistance().set(3);mc.options.simulationDistance().set(5);mc.options.framerateLimit().set(45);mc.options.fov().set(45);mc.options.pauseOnLostFocus=false;mc.options.hideGui=true;mc.options.tutorialStep=net.minecraft.client.tutorial.TutorialSteps.NONE;mc.getWindow().setWindowed(Math.clamp(parameter("CHAO_RADIO075_WIDTH",1440),854,2560),Math.clamp(parameter("CHAO_RADIO075_HEIGHT",900),480,1440));mc.resizeDisplay();mc.createWorldOpenFlows().createFreshLevel("RadioToy075-"+System.currentTimeMillis(),new LevelSettings("RadioToy075",GameType.CREATIVE,false,Difficulty.PEACEFUL,true,new GameRules(),WorldDataConfiguration.DEFAULT),new WorldOptions(2575L,false,false),r->r.registryOrThrow(Registries.WORLD_PRESET).getHolderOrThrow(WorldPresets.FLAT).value().createWorldDimensions(),mc.screen);}
    if(!setup&&mc.player!=null&&mc.getSingleplayerServer()!=null){setup=true;mc.setScreen(null);ChaoRenderer.suppressNames=true;onServer(mc.getSingleplayerServer(),()->{var server=mc.getSingleplayerServer();var level=server.overworld();level.setDayTime(6000);level.getGameRules().getRule(GameRules.RULE_DAYLIGHT).set(false,server);level.getGameRules().getRule(GameRules.RULE_DOMOBSPAWNING).set(false,server);for(var p:BlockPos.betweenClosed(-13,79,-11,13,86,15))level.setBlock(p,p.getY()==79?Blocks.SMOOTH_STONE.defaultBlockState():Blocks.AIR.defaultBlockState(),3);server.getPlayerList().getPlayers().forEach(p->p.setGameMode(GameType.SPECTATOR));next(server,start());});}
    if(phase>=0&&phase<limit()&&mc.level!=null){if(clientPhase==-2&&phase==pendingPhase)return;if(clientPhase!=phase){clientPhase=phase;view=1;viewFrames=0;armRequested=false;lastCaptureNanos=0;}if(!armed&&!sourceReady(mc)){require(System.nanoTime()-playbackWall<30_000_000_000L,"source packet did not arrive");return;}camera(mc);}
    if(phase==limit()&&SAVED.get()==shots){finished=true;onServer(mc.getSingleplayerServer(),()->mc.getSingleplayerServer().tickRateManager().setFrozen(false));LogUtils.getLogger().info("RADIO075_VISUAL_COMPLETE requestedCases={} renderedCases={} skippedCases=0 captures={} saved={} normal20TPS=true audioListening=false epochMillis={} output={}",limit()-start(),limit()-start(),shots,SAVED.get(),System.currentTimeMillis(),OUTPUT);mc.stop();}
  }
}
