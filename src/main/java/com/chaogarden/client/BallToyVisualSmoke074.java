package com.chaogarden.client;

import com.chaogarden.ChaoGarden;
import com.chaogarden.core.*;
import com.chaogarden.entity.ChaoEntity;
import com.chaogarden.toys.*;
import com.mojang.logging.LogUtils;
import java.nio.file.*;
import java.security.MessageDigest;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import net.minecraft.client.*;
import net.minecraft.client.gui.screens.AccessibilityOnboardingScreen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.*;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import org.joml.Vector3f;

/** Disposable actual-client study. All motion, contact, items and cues use production code. */
@EventBusSubscriber(modid=ChaoGarden.MODID,value=Dist.CLIENT)
public final class BallToyVisualSmoke074 {
  private record Case(String name,String toy,String variant,String mode,Direction startSide,String interrupt,String coat,String hat,boolean rabbit,float maturity){
    String kind(){return "chao_"+toy;}
    boolean ball(){return toy.equals("ball");}
    boolean tv(){return toy.equals("tv");}
    boolean natural(){return mode.equals("natural");}
    boolean loop(){return mode.equals("loop");}
    boolean item(){return mode.equals("item");}
    boolean preview(){return !natural()&&!item();}
    int finish(){return loop()?224:184;}
    String sound(){return ball()&&!item()?"minecraft:block.slime_block.step":tv()?"minecraft:block.note_block.bit":toy.equals("toy_car")?"chao_garden:toy.car":"";}
    String[] labels(){
      if(item())return new String[]{"item-world","item-held","item-gui"};
      if(!ball())return new String[]{"phase8","phase40","phase74","phase120","phase150","phase176"};
      if(!interrupt.isEmpty()&&!loop())return new String[]{"windup","contact","released","recovery","after-interrupt-back","idle"};
      return loop()?new String[]{"windup","contact","roll","recover","follow-front","follow-back","loop-again","idle"}:new String[]{"windup","contact","roll","recover","follow-front","follow-back","idle"};
    }
  }
  private static List<Case> cases(){
    var all=new ArrayList<Case>();
    all.add(new Case("ball_baby_from_front","ball","baby_neutral","natural",Direction.SOUTH,"","natural","",false,0));
    all.add(new Case("ball_baby_from_back","ball","baby_neutral","natural",Direction.NORTH,"","natural","",false,0));
    all.add(new Case("ball_baby_from_east","ball","baby_neutral","natural",Direction.EAST,"","natural","",false,0));
    all.add(new Case("ball_baby_from_west","ball","baby_neutral","natural",Direction.WEST,"","natural","",false,0));
    all.add(new Case("ball_dark_run_from_front","ball","dark_run_run_fly","natural",Direction.SOUTH,"","natural_shiny","",false,1));
    all.add(new Case("ball_dark_run_from_back","ball","dark_run_run_fly","natural",Direction.NORTH,"","natural_shiny","",false,1));
    all.add(new Case("ball_dark_run_from_east","ball","dark_run_run_fly","natural",Direction.EAST,"","natural_shiny","",false,1));
    all.add(new Case("ball_dark_run_from_west","ball","dark_run_run_fly","natural",Direction.WEST,"","natural_shiny","",false,1));
    all.add(new Case("ball_hero_power_hat","ball","hero_power_power_fly","natural",Direction.EAST,"","natural","pumpkin",false,1));
    all.add(new Case("ball_neutral_swim_fly","ball","neutral_swim_fly","natural",Direction.WEST,"","natural","",false,1));
    all.add(new Case("ball_neutral_fly_swim","ball","neutral_fly_swim","natural",Direction.SOUTH,"","natural","",false,1));
    all.add(new Case("ball_shiny_rabbit_baby","ball","baby_neutral","natural",Direction.EAST,"","red_shiny","",true,0));
    all.add(new Case("ball_hero_baby_once","ball","baby_hero","once",Direction.SOUTH,"","natural","",false,0));
    all.add(new Case("ball_dark_fly_color_once","ball","dark_fly_run_swim","once",Direction.SOUTH,"","green_mono","",false,1));
    all.add(new Case("ball_hero_run_intermediate_loop_stop","ball","hero_run_power_fly","loop",Direction.SOUTH,"stop","natural","",false,.5f));
    all.add(new Case("ball_neutral_swim_damage","ball","neutral_swim_fly","once",Direction.SOUTH,"damage","natural","",false,1));
    all.add(new Case("ball_baby_remove","ball","baby_neutral","once",Direction.SOUTH,"remove","natural","",false,0));
    all.add(new Case("ball_dark_power_displace","ball","dark_power_run_swim","once",Direction.SOUTH,"displace","natural","",false,1));
    all.add(new Case("ball_neutral_power_once","ball","neutral_power_power_fly","once",Direction.SOUTH,"","natural","",false,1));
    all.add(new Case("ball_dark_baby_once","ball","baby_dark","once",Direction.SOUTH,"","natural","",false,0));
    all.add(new Case("regression_tv_hero_swim","tv","hero_swim_run_swim","once",Direction.SOUTH,"","natural","",false,1));
    all.add(new Case("regression_car_baby","toy_car","baby_neutral","once",Direction.SOUTH,"","natural","",false,0));
    all.add(new Case("regression_crayons_hero_power","crayons","hero_power_power_fly","once",Direction.SOUTH,"","natural","",false,1));
    all.add(new Case("ball_item_world_held_gui","ball","neutral_power_power_fly","item",Direction.SOUTH,"","natural","",false,1));
    all.add(new Case("ball_adult_rabbit_feet","ball","dark_run_run_fly","natural",Direction.EAST,"","natural_shiny","",true,1));
    require(all.size()==25,"authored catalog count changed");
    String selected=System.getenv("CHAO_BALL074_CASES");
    if(selected!=null&&!selected.isBlank()){
      var chosen=new ArrayList<Case>();var used=new HashSet<Integer>();
      for(String raw:selected.split(",")){int i=Integer.parseInt(raw.trim());require(i>=0&&i<all.size()&&used.add(i),"invalid/duplicate case selection");chosen.add(all.get(i));}
      return List.copyOf(chosen);
    }
    return List.copyOf(all);
  }
  private static final List<Case> CASES=cases();
  private static final BlockPos TOY=new BlockPos(0,80,2);
  private static final AABB ARENA=new AABB(-12,79,-10,13,85,15);
  private static final Path OUTPUT=Path.of(System.getenv().getOrDefault("CHAO_BALL074_OUTPUT","/workspace/.setup/ball074-client-final")).toAbsolutePath().normalize();
  private static final int[] WORLD=new int[CASES.size()],RENDER=new int[CASES.size()],ACTIVE=new int[CASES.size()],IDLE=new int[CASES.size()],MOVES=new int[CASES.size()],ACTION=new int[CASES.size()],CONTACT=new int[CASES.size()],ROLL_RENDER=new int[CASES.size()],ROLL_FLOOR=new int[CASES.size()],PRONE=new int[CASES.size()],CANCEL_SERVER=new int[CASES.size()],CANCEL_RENDER=new int[CASES.size()];
  private static final float[] NEAREST_CONTACT=new float[CASES.size()];
  static {Arrays.fill(NEAREST_CONTACT,Float.POSITIVE_INFINITY);}
  private static final boolean[][] SHOTS=new boolean[CASES.size()][];
  private static final int[] APPROACH_SHOTS=new int[CASES.size()];
  private static final float[] MAX_NECK=new float[CASES.size()];
  private static final AtomicInteger SAVED=new AtomicInteger();
  private static final java.util.concurrent.atomic.AtomicIntegerArray SOUNDS=new java.util.concurrent.atomic.AtomicIntegerArray(CASES.size());
  private record SoundRequest(int index,long elapsed){}
  private static final Map<net.minecraft.client.resources.sounds.SoundInstance,SoundRequest> SOUND_REQUESTS=Collections.synchronizedMap(new IdentityHashMap<>());
  private static volatile int phase=-1;
  private static volatile UUID actorId;
  private static volatile long elapsed=-1,caseAge,interruptElapsed=-1;
  private static volatile BlockPos ballPos=TOY;
  private static volatile boolean armed,complete;
  private static volatile String fatal;
  private static volatile double walked,pursued;
  private static volatile float renderedAction=-1;
  private static long firstActionClient=Long.MIN_VALUE;
  private static ChaoEntity actor;
  private static ToyContent.PlayGoal naturalGoal;
  private static ArmorStand observer;
  private static long spawned,began,lastTick,wallStart,playbackWall,firstMoveTick=-1;
  private static Vec3 previousPosition,initialPosition;
  private static String savedState;
  private static ItemStack savedHat,savedOffhand;
  private static long savedCooldown;
  private static int savedFirst;
  private static float savedAlign,interruptHappiness;
  private static boolean interrupted,previewProof,originalNoAi;
  private static boolean started,setup,finished,armRequested,renderedThisFrame,worldThisFrame;
  private static int clientPhase=-1,pendingPhase=-1,ticks,shots,view,viewFrames,guiFrames;
  private static long lastCaptureNanos;
  private static float[][] ballVertices;

  private static boolean enabled(){return "true".equalsIgnoreCase(System.getenv("CHAO_BALL074_SMOKE"));}
  private static int parameter(String name,int fallback){try{return Integer.parseInt(System.getenv().getOrDefault(name,Integer.toString(fallback)));}catch(NumberFormatException e){return fallback;}}
  private static int limit(){return Math.clamp(parameter("CHAO_BALL074_LIMIT",CASES.size()),1,CASES.size());}
  private static int start(){return Math.clamp(parameter("CHAO_BALL074_START",0),0,limit()-1);}
  private static void require(boolean okay,String message){if(!okay)throw new IllegalStateException("BALL074 "+message);}
  private static void onServer(MinecraftServer server,Runnable task){server.execute(()->{try{task.run();}catch(Throwable e){fatal=e.toString();LogUtils.getLogger().error("BALL074_ASYNC_FAILURE",e);}});}
  private static String sha(Path file){try{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(file)));}catch(Exception e){throw new IllegalStateException(e);}}
  private static int first(Case c){return switch(c.variant()){case "neutral_swim_fly","hero_swim_run_swim"->ChaoState.SWIM_TYPE;case "neutral_fly_swim","dark_fly_run_swim"->ChaoState.FLY_TYPE;case "hero_power_power_fly","neutral_power_power_fly","dark_power_run_swim"->ChaoState.POWER_TYPE;case "dark_run_run_fly","hero_run_power_fly"->ChaoState.RUN_TYPE;default->ChaoState.NORMAL;};}
  private static float alignment(Case c){return c.variant().startsWith("hero_")||c.variant().equals("baby_hero")?1:c.variant().startsWith("dark_")||c.variant().equals("baby_dark")?-1:0;}
  private static boolean adult(Case c){return !c.variant().startsWith("baby_");}
  private static int rp(Case c){return c.variant().contains("power_fly")?1:c.variant().endsWith("_run_swim")||c.variant().endsWith("_run_fly")?-1:0;}
  private static int sf(Case c){return c.variant().endsWith("_swim")?-1:c.variant().endsWith("_fly")?1:0;}
  private static ChaoState fixture(Case c){
    var s=new ChaoState();s.adult=adult(c);s.align=alignment(c);s.primaryType=first(c);s.hunger=100;s.energy=100;s.happiness=20;s.boredom=100;s.personality=ChaoPreferences.EASILY_BORED;s.setEvolutionStrength(s.adult?c.maturity()*GradualEvolutionRules.ADULT_STRENGTH_MAX:0);
    if(s.adult){s.train(ChaoState.RUN,1200,new Random(7401));s.train(ChaoState.POWER,1200,new Random(7402));}
    if(rp(c)!=0)s.train(rp(c)<0?ChaoState.RUN:ChaoState.POWER,960,new Random(7403));
    if(sf(c)!=0)s.train(sf(c)<0?ChaoState.SWIM:ChaoState.FLY,960,new Random(7404));
    for(int illness=0;illness<ChaoIllness.COUNT;illness++)s.setIllness(illness,100);
    if(c.rabbit()){
      int animal=Arrays.asList(ChaoState.ANIMALS).indexOf("rabbit");require(animal>=0,"rabbit missing");s.animalMask=s.animalPartsMask=1<<animal;
      for(int part=0;part<s.animalParts.length;part++)if((ChaoState.animalSlots(animal)&(1<<part))!=0)s.animalParts[part]=animal;
    }
    if(c.coat().equals("red_shiny")){s.expressedColor=2;s.colorGenes=new int[]{2,2};s.twoTone=true;s.toneGenes=new int[]{1,1};}
    if(c.coat().equals("green_mono")){s.expressedColor=10;s.colorGenes=new int[]{10,10};s.twoTone=false;s.toneGenes=new int[]{0,0};}
    if(c.coat().contains("shiny")){s.shiny=true;s.shinyGenes=new int[]{1,1};}
    String encoded=s.encode();var out=ChaoState.decode(encoded);require(encoded.equals(out.encode()),"fixture codec changed");
    if(s.adult)require(out.lockedPrimaryType()==first(c)&&out.lockedAlignment()==alignment(c),"fixture adult identity not locked");return out;
  }
  private static void command(MinecraftServer server,String mode){
    var p=server.getPlayerList().getPlayers().getFirst();
    if(mode.equals("stop")){
      // The real ball can travel beyond the operator command's 12-block range.
      // Move only the spectator fixture; preserve real Chao/ball pursuit and dispatcher checks.
      var before=p.position();p.teleportTo(actor.getX()+3,actor.getY()+2,actor.getZ()+3);p.setDeltaMovement(Vec3.ZERO);
      LogUtils.getLogger().info("BALL074_OPERATOR_NEAR_STOP case={} before={} after={} actor={} epochMillis={}",phase,before,p.position(),actor.position(),System.currentTimeMillis());
    }
    try{require(server.getCommands().getDispatcher().execute("chao testtoy "+CASES.get(phase).toy()+" "+mode,p.createCommandSourceStack().withPermission(2))==1,"preview dispatcher rejected "+mode);}catch(com.mojang.brigadier.exceptions.CommandSyntaxException e){throw new IllegalStateException(e);}
  }
  private static void next(MinecraftServer server,int index){
    server.tickRateManager().setFrozen(true);var level=server.overworld();
    if(actor!=null){ToyPreview.stop(actor);ToyContent.endPlaying(actor,false);actor.discard();actor=null;}
    for(var p:BlockPos.betweenClosed(-12,80,-10,12,82,14))if(level.getBlockState(p).getBlock() instanceof ChaoToyBlock)level.setBlock(p,Blocks.AIR.defaultBlockState(),3);
    for(var item:level.getEntitiesOfClass(ItemEntity.class,ARENA))item.discard();
    var c=CASES.get(index);if(c.ball()||c.tv())level.setBlock(TOY,ToyContent.block(c.kind()).defaultBlockState().setValue(ChaoToyBlock.FACING,Direction.SOUTH),3);
    actor=ChaoGarden.CHAO.get().create(level);require(actor!=null,"actor create failed");actor.setState(fixture(c));actor.setInvulnerable(true);actor.deferBehavior(100000);originalNoAi=c.loop()||c.item();actor.setNoAi(originalNoAi);actor.getRandom().setSeed(18);
    double distance=c.ball()?BallPlayRules074.contactDistance(actor.state())+.10:1;
    Vec3 origin=c.natural()?Vec3.atBottomCenterOf(TOY.relative(c.startSide(),3)):Vec3.atBottomCenterOf(TOY).add(c.startSide().getStepX()*distance,0,c.startSide().getStepZ()*distance);
    float yaw=c.startSide().getOpposite().toYRot();actor.moveTo(origin.x,origin.y,origin.z,yaw,0);actor.yBodyRot=actor.yBodyRotO=actor.yHeadRot=actor.yHeadRotO=yaw;actor.setOnGround(true);
    if(!c.hat().isEmpty())actor.setItemSlot(EquipmentSlot.HEAD,new ItemStack(Items.CARVED_PUMPKIN));
    level.addFreshEntity(actor);actor.goalSelector.removeAllGoals(g->true);actor.targetSelector.removeAllGoals(g->true);naturalGoal=c.natural()?new ToyContent.PlayGoal(actor):null;if(naturalGoal!=null)actor.goalSelector.addGoal(4,naturalGoal);else actor.getPersistentData().putLong("ChaoToyNextPlay",Long.MAX_VALUE);
    savedState=actor.state().encode();savedCooldown=actor.getPersistentData().getLong("ChaoToyNextPlay");savedHat=actor.getItemBySlot(EquipmentSlot.HEAD).copy();savedOffhand=actor.getItemBySlot(EquipmentSlot.OFFHAND).copy();savedFirst=actor.state().lockedPrimaryType();savedAlign=actor.state().lockedAlignment();
    initialPosition=previousPosition=actor.position();walked=pursued=0;spawned=lastTick=level.getGameTime();began=-1;elapsed=-1;caseAge=0;interruptElapsed=-1;firstMoveTick=-1;interrupted=previewProof=armed=complete=false;ballPos=TOY;SHOTS[index]=new boolean[c.labels().length];actorId=actor.getUUID();phase=index;
    var p=server.getPlayerList().getPlayers().getFirst();p.setGameMode(c.item()?GameType.CREATIVE:GameType.SPECTATOR);p.getInventory().clearContent();p.setYRot(180);p.setXRot(38);p.teleportTo(c.item()?.5:7.5,c.item()?81:82,c.item()?5.5:7.5);p.setNoGravity(true);p.setDeltaMovement(Vec3.ZERO);
    if(c.item()){
      var stack=new ItemStack(ChaoGarden.item("chao_ball"));p.getInventory().setItem(0,stack.copy());p.getInventory().selected=0;p.containerMenu.broadcastChanges();
      level.addFreshEntity(new ItemEntity(level,-1.2,80.3,4.0,stack.copy()));
    }
    playbackWall=System.nanoTime();LogUtils.getLogger().info("BALL074_CASE {} name={} toy={} variant={} mode={} startSide={} interrupt={} firstType={} lockedAlignment={} adult={} maturity={} rp={} sf={} coat={} hat={} rabbit={} itemFixture={} state={} epochMillis={}",index,c.name(),c.kind(),c.variant(),c.mode(),c.startSide(),c.interrupt(),savedFirst,savedAlign,actor.state().adult,actor.state().gradualVisualStrength(),rp(c),sf(c),c.coat(),c.hat(),c.rabbit(),c.item(),savedState,System.currentTimeMillis());
  }
  private static void arm(MinecraftServer server,int expected){
    if(phase!=expected||armed)return;require(Math.abs(server.tickRateManager().tickrate()-20)<.00001f,"normal20TPS required");var c=CASES.get(phase);actor.setOnGround(true);
    if(c.preview()){command(server,c.loop()?"loop":"once");began=server.overworld().getGameTime();elapsed=0;}else if(c.item()){began=server.overworld().getGameTime();elapsed=0;}
    armed=true;playbackWall=System.nanoTime();server.tickRateManager().setFrozen(false);LogUtils.getLogger().info("BALL074_ARMED {} name={} natural={} epochMillis={} normal20TPS=true",phase,c.name(),c.natural(),System.currentTimeMillis());
  }
  private static BlockPos realBall(net.minecraft.server.level.ServerLevel level,boolean removed){
    BlockPos found=null;int count=0;for(var p:BlockPos.betweenClosed(-12,80,-10,12,80,14))if(level.getBlockState(p).is(ToyContent.block("chao_ball"))){found=p.immutable();count++;}
    require(count==(removed?0:1),"physical ball count "+count+" expected "+(removed?0:1));return found;
  }
  @SubscribeEvent public static void server(ServerTickEvent.Post event){
    if(!enabled()||phase<0||phase>=limit()||!armed||complete)return;var server=event.getServer();var level=server.overworld();long now=level.getGameTime();if(now==lastTick)return;lastTick=now;caseAge=now-spawned;var c=CASES.get(phase);
    require(actor!=null&&actor.isAlive()&&!actor.isRemoved(),"actor disappeared");require(actor.state().adult==adult(c)&&!actor.isCocooned(),"life stage changed");if(adult(c))require(actor.state().lockedPrimaryType()==savedFirst&&actor.state().lockedAlignment()==savedAlign,"adult identity changed");require(caseAge<2800,"approach deadline pos="+actor.position()+" walked="+walked);
    double step=actor.position().subtract(previousPosition).horizontalDistance();walked+=step;if(firstMoveTick>=0&&actor.toyKickElapsed()<0&&!interrupted)pursued+=step;previousPosition=actor.position();
    if(c.ball()){
      var current=realBall(level,interrupted&&c.interrupt().equals("remove"));
      if(current!=null&&!current.equals(ballPos)){
        require(current.distManhattan(ballPos)==1,"ball jumped more than one cell");int action=actor.toyKickElapsed();require(action>=BallPlayRules074.CONTACT_TICK&&action<BallPlayRules074.ACTION_TICKS,"ball moved outside actual contact");require(current.equals(actor.toyTarget()),"synced actor target did not follow real ball");
        var old=ballPos;ballPos=current;MOVES[phase]++;if(firstMoveTick<0)firstMoveTick=now;
        require(level.getBlockEntity(current) instanceof BallToyBlockEntity074,"moved ball has no presentation entity");var be=(BallToyBlockEntity074)level.getBlockEntity(current);
        LogUtils.getLogger().info("BALL074_MOVE case={} move={} from={} to={} elapsed={} actionTick={} rollStarted={} direction={} epochMillis={}",phase,MOVES[phase],old.asLong(),current.asLong(),began<0?-1:now-began,action,be.rollStarted(),be.rollDirection().getSerializedName(),System.currentTimeMillis());
      }
    }
    if(began<0){
      if(ToyContent.isPlaying(actor)){began=actor.getPersistentData().getLong("ChaoToyUntil")-160;playbackWall=System.nanoTime();require(walked>.35,"natural approach bypassed");LogUtils.getLogger().info("BALL074_NATURAL_STARTED {} name={} waitedTicks={} walked={} start={} arrived={} epochMillis={}",phase,c.name(),now-spawned,walked,initialPosition,actor.position(),System.currentTimeMillis());}
      else if(caseAge%40==0)LogUtils.getLogger().info("BALL074_APPROACH case={} age={} pos={} walked={} target={} pathDone={} epochMillis={}",phase,caseAge,actor.position(),walked,actor.getNavigation().getTargetPos(),actor.getNavigation().isDone(),System.currentTimeMillis());
    }
    if(began<0)return;elapsed=now-began;
    if(c.preview()&&!previewProof){require(savedState.equals(actor.state().encode()),"preview mutated saved state");require(actor.getPersistentData().getLong("ChaoToyNextPlay")==savedCooldown,"preview changed prior cooldown");if(!ToyPreview.active(actor))previewProof=true;}
    boolean due=c.loop()?elapsed>=200:firstMoveTick>=0&&now>=firstMoveTick+2;
    if(!interrupted&&!c.interrupt().isEmpty()&&due){
      require(!c.preview()||savedState.equals(actor.state().encode()),"state changed before deliberate interruption");previewProof=c.preview();
      if(c.interrupt().equals("stop"))command(server,"stop");else if(c.interrupt().equals("damage")){actor.setInvulnerable(false);require(actor.hurt(level.damageSources().generic(),1),"real damage refused");}else if(c.interrupt().equals("remove"))require(level.destroyBlock(ballPos,false),"real ball removal refused");else if(c.interrupt().equals("displace"))actor.setPos(actor.getX()+6,actor.getY(),actor.getZ());
      interrupted=true;interruptElapsed=elapsed;interruptHappiness=actor.state().happiness;LogUtils.getLogger().info("BALL074_INTERRUPT case={} kind={} elapsed={} moveCount={} epochMillis={}",phase,c.interrupt(),elapsed,MOVES[phase],System.currentTimeMillis());
    }
    if(interrupted&&elapsed>=interruptElapsed+2){require(!ToyPreview.active(actor)&&actor.toyPlayingTicks()==0&&actor.toyTarget()==null,"cancellation left session/target");CANCEL_SERVER[phase]++;}
    if(!c.item())require(level.getEntitiesOfClass(ItemEntity.class,ARENA).isEmpty(),"gameplay minted a real item");
    if(elapsed<c.finish())return;
    if(!c.item()){
      require(!ToyPreview.active(actor)&&actor.toyPlayingTicks()==0&&actor.toyTarget()==null,"session left clock/reservation");require(actor.getHeldChaoItem().isEmpty()&&actor.getMainHandItem().isEmpty(),"preview minted held item");require(ItemStack.matches(savedHat,actor.getItemBySlot(EquipmentSlot.HEAD))&&ItemStack.matches(savedOffhand,actor.getItemBySlot(EquipmentSlot.OFFHAND)),"equipment changed");require(actor.isNoAi()==originalNoAi,"original NoAI not restored");require(!actor.isNoGravity(),"gravity flag left behind");
      if(c.preview()){require(previewProof,"preview terminal state not witnessed");require(actor.getPersistentData().getLong("ChaoToyNextPlay")==savedCooldown,"prior cooldown not restored");require(actor.state().happiness==(c.interrupt().equals("damage")?interruptHappiness:20),"preview rewarded care");}else require(actor.state().happiness==24,"natural reward not exactly once");
      if(!c.interrupt().isEmpty())require(CANCEL_SERVER[phase]>0,"bounded cancellation not observed");
      if(c.ball()){require(MOVES[phase]>0,"ball never moved at real contact");if(c.natural()||c.interrupt().isEmpty())require(pursued>.025,"no actual pursuit after contact");var at=realBall(level,c.interrupt().equals("remove"));if(at!=null){var state=level.getBlockState(at);require(!state.getValue(ChaoToyBlock.PLAYING)&&!state.getValue(ChaoBallBlock074.ROLLING),"ball did not park after play");}}
      if(c.tv()){var state=level.getBlockState(TOY);require(state.is(ToyContent.block(c.kind()))&&!state.getValue(ChaoToyBlock.PLAYING)&&state.getValue(ChaoToyBlock.FRAME)==0,"TV regression did not return idle");}
    }else require(level.getEntitiesOfClass(ItemEntity.class,ARENA).size()==1&&level.getBlockState(TOY).is(ToyContent.block("chao_ball")),"item fixture lost its actual item/block");
    var tag=new CompoundTag();actor.saveWithoutId(tag);var copy=ChaoGarden.CHAO.get().create(level);copy.load(tag);require(copy.state().encode().equals(actor.state().encode()),"NBT reload changed state");copy.discard();
    complete=true;server.tickRateManager().setFrozen(true);LogUtils.getLogger().info("BALL074_NATIVE_COMPLETE {} name={} elapsed={} walked={} pursued={} moves={} previewPlaybackStatePreserved={} originalNoAi={} cancelServerChecks={} interruptElapsed={} itemFixture={} state={} epochMillis={}",phase,c.name(),elapsed,walked,pursued,MOVES[phase],previewProof,originalNoAi,CANCEL_SERVER[phase],interruptElapsed,c.item(),actor.state().encode(),System.currentTimeMillis());
  }
  @SubscribeEvent public static void frameBegin(RenderFrameEvent.Pre e){if(enabled())renderedThisFrame=worldThisFrame=false;}
  private static ChaoEntity observed(Minecraft mc){if(mc.level!=null&&actorId!=null)for(var e:mc.level.entitiesForRendering())if(e instanceof ChaoEntity c&&c.getUUID().equals(actorId))return c;return null;}
  private static boolean sourceReady(Minecraft mc){if(mc.level==null)return false;var c=CASES.get(phase);var state=mc.level.getBlockState(TOY);return c.ball()||c.tv()?state.is(ToyContent.block(c.kind()))&&!state.getValue(ChaoToyBlock.PLAYING):state.isAir();}
  private static int labelView(String label){return label.equals("follow-front")||label.equals("released")||label.equals("phase74")?0:label.equals("follow-back")||label.equals("after-interrupt-back")||label.equals("phase120")?2:label.equals("item-held")?3:label.equals("item-gui")?4:1;}
  private static String wantedLabel(){var labels=CASES.get(phase).labels();for(int i=0;i<labels.length;i++)if(!SHOTS[phase][i])return labels[i];return "idle";}
  private static void camera(Minecraft mc){
    var actor=observed(mc);if(actor==null)return;var c=CASES.get(phase);String label=wantedLabel();int wanted=labelView(label);if(elapsed<0)wanted=1;if(wanted!=view){view=wanted;viewFrames=0;}
    mc.options.hideGui=!(c.item()&&view>=3);
    if(c.item()&&view>=3){mc.player.setYRot(180);mc.player.yRotO=180;mc.player.setXRot(38);mc.player.xRotO=38;mc.options.setCameraType(CameraType.FIRST_PERSON);mc.setCameraEntity(mc.player);return;}
    if(observer==null||observer.level()!=mc.level){observer=new ArmorStand(mc.level,0,0,0);observer.setInvisible(true);observer.setNoGravity(true);}
    Vec3 forward=Vec3.directionFromRotation(0,c.startSide().getOpposite().toYRot()),side=new Vec3(-forward.z,0,forward.x);Vec3 focus=actor.position().add(0,.28,0),eye;
    if(c.ball()){
      Vec3 ball=Vec3.atBottomCenterOf(ballPos).add(0,.18,0);if(mc.level.getBlockEntity(ballPos) instanceof BallToyBlockEntity074 be)ball=ball.add(be.offset(0));focus=focus.lerp(ball,.5);
      eye=focus.add(side.scale(view==1?2.75:view==0?.75:-1)).add(forward.scale(view==0?2.5:view==2?-2.6:.08)).add(0,view==1?.50:1.1,0);
    }else if(c.tv()){
      focus=Vec3.atBottomCenterOf(TOY).add(0,.52,.5);eye=focus.add(side.scale(view==0?2.25:view==1?2.5:-1.45)).add(forward.scale(view==0?2.6:view==1?-.30:-3.25)).add(0,view==0?1.5:view==1?.6:1.2,0);
    }else{focus=focus.add(forward.scale(.17));eye=focus.add(forward.scale(view==0?1.8:view==1?.08:-.35)).add(side.scale(view==0?.55:view==1?1.85:-1.85)).add(0,view==1?.12:view==2?.60:.66,0);}
    var d=focus.subtract(eye);float yaw=(float)(Math.toDegrees(Math.atan2(d.z,d.x))-90),pitch=(float)-Math.toDegrees(Math.atan2(d.y,Math.hypot(d.x,d.z)));observer.moveTo(eye.x,eye.y-observer.getEyeHeight(),eye.z,yaw,pitch);observer.xo=observer.xOld=observer.getX();observer.yo=observer.yOld=observer.getY();observer.zo=observer.zOld=observer.getZ();observer.yRotO=yaw;observer.xRotO=pitch;observer.setYHeadRot(yaw);observer.yHeadRotO=yaw;mc.options.setCameraType(CameraType.FIRST_PERSON);mc.setCameraEntity(observer);
  }
  @SubscribeEvent public static void restoreCamera(ClientTickEvent.Pre e){if(enabled()){var mc=Minecraft.getInstance();if(mc.player!=null&&mc.getCameraEntity()!=mc.player)mc.setCameraEntity(mc.player);}}
  private static float[][] ballVertices(Minecraft mc){
    if(ballVertices!=null)return ballVertices;
    try(var reader=mc.getResourceManager().openAsReader(ResourceLocation.fromNamespaceAndPath("chao_garden","models/block/chao_ball_idle.json"))){
      var json=com.google.gson.JsonParser.parseReader(reader).getAsJsonObject();var points=new ArrayList<float[]>();
      for(var element:json.getAsJsonArray("elements")){var part=element.getAsJsonObject();var from=part.getAsJsonArray("from");var to=part.getAsJsonArray("to");for(int n=0;n<8;n++)points.add(new float[]{((n&1)==0?from:to).get(0).getAsFloat()/16,((n&2)==0?from:to).get(1).getAsFloat()/16,((n&4)==0?from:to).get(2).getAsFloat()/16});}
      require(!points.isEmpty(),"actual ball resource has no vertices");return ballVertices=points.toArray(float[][]::new);
    }catch(Exception e){throw new IllegalStateException("Cannot read actual ball resource for floor check",e);}
  }
  /** Convex OBB separation in real world blocks; both the Chao part and ball rotate. */
  private static float separation(Vector3f[] a,Vector3f[] b){
    require(a.length==8&&b.length==8,"contact volume lacks eight corners");var axes=new ArrayList<Vector3f>();
    var ae=new Vector3f[]{new Vector3f(a[1]).sub(a[0]),new Vector3f(a[2]).sub(a[0]),new Vector3f(a[4]).sub(a[0])};var be=new Vector3f[]{new Vector3f(b[1]).sub(b[0]),new Vector3f(b[2]).sub(b[0]),new Vector3f(b[4]).sub(b[0])};
    for(int n=0;n<3;n++){axes.add(new Vector3f(ae[n]).cross(ae[(n+1)%3]));axes.add(new Vector3f(be[n]).cross(be[(n+1)%3]));}
    for(var x:ae)for(var y:be)axes.add(new Vector3f(x).cross(y));float gap=Float.NEGATIVE_INFINITY;
    for(var axis:axes){if(axis.lengthSquared()<1e-12f)continue;axis.normalize();float al=Float.POSITIVE_INFINITY,ah=Float.NEGATIVE_INFINITY,bl=Float.POSITIVE_INFINITY,bh=Float.NEGATIVE_INFINITY;for(var x:a){float q=x.dot(axis);al=Math.min(al,q);ah=Math.max(ah,q);}for(var x:b){float q=x.dot(axis);bl=Math.min(bl,q);bh=Math.max(bh,q);}gap=Math.max(gap,Math.max(bl-ah,al-bh));}
    return gap;
  }
  private static void contact(Minecraft mc,ChaoEntity c,ChaoModel model,float partial){
    int kick=c.toyKickElapsed();float action=kick+partial;if(kick<0||action<BallPlayRules074.CONTACT_TICK-1.5f||action>BallPlayRules074.CONTACT_TICK+.5f||!ChaoBallAnimation074.active(c))return;
    if(!(mc.level.getBlockEntity(ballPos) instanceof BallToyBlockEntity074 ball))return;
    var part=model.ballContactVertices074(c.state(),c.getChaoHat());require(part.length==8,"rendered contact part missing");float yaw=net.minecraft.util.Mth.rotLerp(partial,c.yBodyRotO,c.yBodyRot);double angle=Math.toRadians(yaw),cos=Math.cos(angle),sin=Math.sin(angle);var root=c.getPosition(partial);var worldPart=new Vector3f[8];
    for(int n=0;n<8;n++){var q=part[n];worldPart[n]=new Vector3f((float)(root.x+cos*q.x-sin*q.z),(float)(root.y+q.y),(float)(root.z+sin*q.x+cos*q.z));}
    var matrix=BallToyRenderer074.modelTransform(ball,partial);var vertices=ballVertices(mc);float nearest=Float.POSITIVE_INFINITY;
    for(int i=0;i<vertices.length;i+=8){var box=new Vector3f[8];for(int n=0;n<8;n++){var q=matrix.transformPosition(new Vector3f(vertices[i+n]));box[n]=q.add(ballPos.getX(),ballPos.getY(),ballPos.getZ());}nearest=Math.min(nearest,separation(worldPart,box));}
    require(Float.isFinite(nearest)&&nearest>=-.005f,"actual rendered contact penetrates ball: "+nearest);NEAREST_CONTACT[phase]=Math.min(NEAREST_CONTACT[phase],Math.abs(nearest));CONTACT[phase]++;
  }
  @SubscribeEvent public static void rendered(RenderLivingEvent.Post<?,?> event){
    if(!enabled()||phase<0||phase>=limit()||SceneShaderPack.shadows()||!(event.getEntity()instanceof ChaoEntity c)||!c.getUUID().equals(actorId))return;var mc=Minecraft.getInstance();if(mc.screen!=null||mc.getOverlay()!=null)return;var m=(ChaoModel)event.getRenderer().getModel();require(m.officialPosesFinite(),"nonfinite actual rendered pose");renderedThisFrame=true;RENDER[phase]++;
    var entry=CASES.get(phase);if(elapsed<0||entry.item())return;
    boolean live=entry.ball()?ChaoBallAnimation074.active(c):entry.tv()?ChaoTvWatchAnimation073.active(c):entry.toy().equals("toy_car")?ChaoActiveToyAnimation049.active(c):ChaoQuietToyAnimation049.active(c);
    if(entry.ball()){
      int action=c.toyKickElapsed();if(action>=0&&firstActionClient==Long.MIN_VALUE)firstActionClient=mc.level.getGameTime()-action;renderedAction=firstActionClient==Long.MIN_VALUE?-1:mc.level.getGameTime()-firstActionClient+event.getPartialTick();
      if(live&&action>=0)ACTION[phase]++;
      contact(mc,c,m,event.getPartialTick());
      if(mc.level.getBlockEntity(ballPos) instanceof BallToyBlockEntity074 be&&be.rolling(event.getPartialTick())){
        double p=be.progress(event.getPartialTick());var off=be.offset(event.getPartialTick());require(p>=0&&p<=1&&Math.abs(off.y)<.00001&&off.horizontalDistance()<=1.00001,"invalid actual rolling offset");
        var matrix=BallToyRenderer074.modelTransform(be,event.getPartialTick());float minimum=Float.POSITIVE_INFINITY,maximum=Float.NEGATIVE_INFINITY;
        for(var vertex:ballVertices(mc)){var point=matrix.transformPosition(new Vector3f(vertex));minimum=Math.min(minimum,point.y);maximum=Math.max(maximum,point.y);}
        require(Float.isFinite(minimum)&&Math.abs(minimum)<.0002f&&maximum<.45f,"rolling geometry floats or enters floor min="+minimum+" max="+maximum);ROLL_FLOOR[phase]++;if(p>.01&&p<.99)ROLL_RENDER[phase]++;
      }
    }
    if(c.toyPlayingKind().equals(entry.kind())&&c.toyPlayingTicks()>0&&live){
      ACTIVE[phase]++;
      if(entry.tv()){float t=160-c.toyPlayingTicks()+event.getPartialTick();if(t>=28&&t<=132)require(m.officialMotion()==OfficialChaoRig.Motion.SIT,"TV regression lost SIT");}
      if(!entry.ball()&&!entry.tv()){
        float w=GardenToyPerformance049.presence(c,event.getPartialTick());var bounds=ChaoProneToyPose051.runtimeBounds(c,m);
        for(String bone:List.of("body","skull","foot_left",m.officialPoses().containsKey("foot_right")?"foot_right":"feet_bone_right")){var box=ChaoProneToyPose051.worldBounds(m::transformAccessoryAnchor,bounds,bone);if(box!=null)require(box[4]<=ChaoProneToyPose051.FLOOR_Y+.025f,"prone regression entered floor");}
        if(w>.999f){float gap=ChaoProneToyPose051.separation(m::transformAccessoryAnchor,bounds);require(gap>=-.0001f&&gap<=(entry.toy().equals("toy_car")?.5f:.25f)/16,"prone regression head separation");MAX_NECK[phase]=Math.max(MAX_NECK[phase],gap);PRONE[phase]++;}
      }
    }else if(elapsed>=166&&c.getBehavior()==ChaoEntity.Behavior.NONE){IDLE[phase]++;require(!live&&c.toyPlayingTicks()==0,"toy pose/clock survived cleanup");}
    if(interruptElapsed>=0&&elapsed>=interruptElapsed+6&&elapsed<=interruptElapsed+40){require(!live&&c.toyPlayingTicks()==0,"cancellation survived bounded client sync");if(entry.interrupt().equals("damage"))require(c.hurtTime>0||c.getBehavior()==ChaoEntity.Behavior.COWER,"damage reaction not rendered");if(entry.interrupt().equals("remove"))require(!mc.level.getBlockState(ballPos).is(ToyContent.block("chao_ball")),"removed ball remains visible");CANCEL_RENDER[phase]++;}
  }
  @SubscribeEvent public static void world(RenderLevelStageEvent e){if(!enabled()||phase<0||phase>=limit()||e.getStage()!=RenderLevelStageEvent.Stage.AFTER_LEVEL||SceneShaderPack.shadows())return;var mc=Minecraft.getInstance();if(mc.screen==null&&mc.getOverlay()==null&&(CASES.get(phase).item()||renderedThisFrame))worldThisFrame=true;}
  @SubscribeEvent public static void soundRequested(net.neoforged.neoforge.client.event.sound.PlaySoundEvent event){
    if(!enabled()||phase<0||phase>=limit()||!armed)return;var sound=event.getOriginalSound();if(sound!=null&&sound.getLocation().toString().equals(CASES.get(phase).sound()))SOUND_REQUESTS.put(sound,new SoundRequest(phase,elapsed));
  }
  @SubscribeEvent public static void soundStarted(net.neoforged.neoforge.client.event.sound.PlaySoundSourceEvent event){
    if(!enabled())return;var sound=event.getSound();var request=SOUND_REQUESTS.remove(sound);if(request==null)return;try{var c=CASES.get(request.index());float volume=sound.getVolume(),pitch=sound.getPitch();if(c.ball())require(volume<=.081f&&Math.abs(pitch-1)<.0001,"ball cue too loud/pitched");if(c.tv())require(volume<=.071f&&Math.abs(pitch-1)<.0001,"TV cue regression");SOUNDS.incrementAndGet(request.index());LogUtils.getLogger().info("BALL074_SOUND case={} id={} elapsed={} volume={} pitch={} postResolve=true channelStarted=true epochMillis={}",request.index(),sound.getLocation(),request.elapsed(),volume,pitch,System.currentTimeMillis());}catch(Throwable e){fatal=e.toString();LogUtils.getLogger().error("BALL074_ASYNC_FAILURE",e);}
  }
  private static void capture(Minecraft mc,String label,boolean approach){
    if(label.equals("windup"))require(renderedAction<=5,"missed first-action windup capture window");
    if(label.equals("contact"))require(renderedAction<=BallPlayRules074.CONTACT_TICK+2,"missed first-action contact capture window");
    if(label.equals("roll"))require(renderedAction<=BallPlayRules074.CONTACT_TICK+5,"missed first-action roll capture window");
    if(label.equals("recover"))require(renderedAction<=BallPlayRules074.ACTION_TICKS+2,"missed first-action recovery capture window");
    int index=phase;var c=CASES.get(index);long tick=elapsed,age=caseAge;float action=renderedAction;double distance=walked;String angle=new String[]{"front","side","back","held","gui"}[view],name=String.format("ball074-%02d-%s-%s-%s-t%03d-a%04d.png",index,c.name(),label,angle,tick,age);Path file=OUTPUT.resolve("screenshots").resolve(name);try{Files.createDirectories(file.getParent());}catch(Exception e){throw new IllegalStateException(e);}
    Screenshot.grab(OUTPUT.toFile(),name,mc.getMainRenderTarget(),message->{require(Files.isRegularFile(file),"PNG did not save");SAVED.incrementAndGet();LogUtils.getLogger().info("BALL074_CAPTURE {} case={} label={} angle={} tick={} caseAge={} actionTick={} approach={} walked={} sha256={} epochMillis={}",name,index,label,angle,tick,age,action,approach,distance,sha(file),System.currentTimeMillis());});shots++;lastCaptureNanos=System.nanoTime();viewFrames=0;
  }
  private static boolean due(String label){
    return switch(label){case "windup"->renderedAction>=2;case "contact"->renderedAction>=BallPlayRules074.CONTACT_TICK;case "roll"->renderedAction>=BallPlayRules074.CONTACT_TICK+3;case "recover"->renderedAction>=BallPlayRules074.ACTION_TICKS-2;case "follow-front"->elapsed>=75;case "follow-back"->elapsed>=120;case "loop-again"->elapsed>=186;case "released"->interruptElapsed>=0&&elapsed>=interruptElapsed+2;case "recovery"->interruptElapsed>=0&&elapsed>=interruptElapsed+12;case "after-interrupt-back"->interruptElapsed>=0&&elapsed>=interruptElapsed+30;case "idle"->elapsed>=(CASES.get(phase).loop()?218:176);case "item-world"->elapsed>=40;case "item-held"->elapsed>=80;case "item-gui"->elapsed>=110;default->label.startsWith("phase")&&elapsed>=Integer.parseInt(label.substring(5));};
  }
  @SubscribeEvent public static void frameEnd(RenderFrameEvent.Post e){
    if(!enabled()||phase<0||phase>=limit()||clientPhase!=phase)return;var mc=Minecraft.getInstance();var c=CASES.get(phase);boolean gui=c.item()&&(mc.screen instanceof InventoryScreen||mc.screen instanceof CreativeModeInventoryScreen);if(!worldThisFrame&&!gui)return;WORLD[phase]++;viewFrames++;
    if(!armed){if(viewFrames>=12&&!armRequested){armRequested=true;int index=phase;onServer(mc.getSingleplayerServer(),()->arm(mc.getSingleplayerServer(),index));}return;}
    if(elapsed<0){if(APPROACH_SHOTS[phase]<2&&viewFrames>=6&&(APPROACH_SHOTS[phase]==0||walked>.75)){capture(mc,"approach"+APPROACH_SHOTS[phase],true);APPROACH_SHOTS[phase]++;}return;}
    String label=wantedLabel();var labels=c.labels();for(int i=0;i<labels.length;i++)if(!SHOTS[phase][i]){
      if(due(label)&&viewFrames>=2&&System.nanoTime()-lastCaptureNanos>45_000_000L){
        if(label.equals("item-held"))require(mc.player.getMainHandItem().is(ChaoGarden.item("chao_ball")),"held fixture lost real ball stack");
        if(label.equals("item-gui")&&!gui){mc.setScreen(new InventoryScreen(mc.player));guiFrames=0;break;}
        if(!label.equals("item-gui")||++guiFrames>=8){capture(mc,label,false);SHOTS[phase][i]=true;if(label.equals("item-gui"))mc.setScreen(null);}
      }
      break;
    }
    if(!complete)return;for(boolean done:SHOTS[phase])if(!done)return;
    if(!c.item()){
      require(ACTIVE[phase]>=10&&IDLE[phase]>=3,"active and return-to-idle not rendered");int expected=c.ball()?MOVES[phase]:c.tv()?1:c.toy().equals("toy_car")?4:0;require(SOUNDS.get(phase)==expected,"client cue count "+SOUNDS.get(phase)+" expected "+expected);
      if(c.ball()){require(ACTION[phase]>=8&&CONTACT[phase]>0&&NEAREST_CONTACT[phase]<=.035f,"actual rendered part never approached ball contact: "+NEAREST_CONTACT[phase]);if(!c.interrupt().equals("remove"))require(ROLL_RENDER[phase]>0&&ROLL_FLOOR[phase]>0,"no intermediate real rolling presentation/floor check observed");}
      else if(!c.tv())require(PRONE[phase]>10,"prone regression not checked");
      if(!c.interrupt().isEmpty())require(CANCEL_RENDER[phase]>0,"bounded cancellation not rendered");
    }
    require((System.nanoTime()-playbackWall)/1e9>=c.finish()/20.0-.8,"study faster than normal20TPS");LogUtils.getLogger().info("BALL074_RENDER_COMPLETE {} name={} worldFrames={} actorFrames={} activeFrames={} actionFrames={} contactChecks={} nearestContact={} idleFrames={} sounds={} moves={} rollFrames={} rollFloorChecks={} proneChecks={} maxNeckGap={} cancelRenderFrames={} captures={} itemFixture={} normal20TPS=true epochMillis={}",phase,c.name(),WORLD[phase],RENDER[phase],ACTIVE[phase],ACTION[phase],CONTACT[phase],NEAREST_CONTACT[phase],IDLE[phase],SOUNDS.get(phase),MOVES[phase],ROLL_RENDER[phase],ROLL_FLOOR[phase],PRONE[phase],MAX_NECK[phase],CANCEL_RENDER[phase],labels.length+APPROACH_SHOTS[phase],c.item(),System.currentTimeMillis());
    int done=phase;pendingPhase=phase;clientPhase=-2;onServer(mc.getSingleplayerServer(),()->{if(phase==done){if(phase+1<limit())next(mc.getSingleplayerServer(),phase+1);else phase=limit();}});
  }
  private static void catalog(){
    try{Files.createDirectories(OUTPUT);var root=new com.google.gson.JsonObject();root.addProperty("kind","Actual production ball/toy/item client study; no alternate pose or renderer");root.addProperty("normal_tps",20);root.addProperty("audio_listening",false);root.addProperty("start",start());root.addProperty("limit_exclusive",limit());root.addProperty("action_ticks",BallPlayRules074.ACTION_TICKS);root.addProperty("contact_tick",BallPlayRules074.CONTACT_TICK);root.addProperty("roll_ticks",BallPlayRules074.ROLL_TICKS);var rows=new com.google.gson.JsonArray();
      for(int i=0;i<CASES.size();i++){var c=CASES.get(i);var r=new com.google.gson.JsonObject();r.addProperty("index",i);r.addProperty("name",c.name());r.addProperty("toy",c.kind());r.addProperty("variant",c.variant());r.addProperty("mode",c.mode());r.addProperty("natural",c.natural());r.addProperty("start_side",c.startSide().getSerializedName());r.addProperty("interrupt",c.interrupt());r.addProperty("interruption_timing",c.interrupt().isEmpty()?"none":c.loop()?"elapsed200":"first_successful_roll_plus2");r.addProperty("finish_tick",c.finish());r.addProperty("first_type",first(c));r.addProperty("alignment",alignment(c));r.addProperty("adult",adult(c));r.addProperty("maturity",c.maturity());r.addProperty("run_power",rp(c));r.addProperty("swim_fly",sf(c));r.addProperty("coat",c.coat());r.addProperty("hat",c.hat());r.addProperty("rabbit",c.rabbit());r.addProperty("item_fixture",c.item());r.addProperty("expected_client_sound",c.sound());r.addProperty("sound_count_policy",c.ball()&&!c.item()?"one_per_successful_physical_move":c.tv()?"one":c.toy().equals("toy_car")?"four":"zero");var labels=new com.google.gson.JsonArray();for(String label:c.labels())labels.add(label);r.add("shot_labels",labels);rows.add(r);}root.add("cases",rows);Files.writeString(OUTPUT.resolve("case-catalog.json"),new com.google.gson.GsonBuilder().setPrettyPrinting().create().toJson(root)+"\n");}catch(Exception e){throw new IllegalStateException(e);}
  }
  @SubscribeEvent public static void client(ClientTickEvent.Post e){
    if(!enabled()||finished)return;var mc=Minecraft.getInstance();ticks++;if(fatal!=null)throw new IllegalStateException("BALL074 asynchronous server failure: "+fatal);if(wallStart==0)wallStart=System.nanoTime();require(System.nanoTime()-wallStart<2_400_000_000_000L,"study wall timeout");if(mc.screen instanceof AccessibilityOnboardingScreen)mc.setScreen(new TitleScreen());
    if(!started&&mc.screen instanceof TitleScreen){started=true;catalog();mc.options.renderDistance().set(3);mc.options.simulationDistance().set(5);mc.options.framerateLimit().set(45);mc.options.fov().set(45);mc.options.guiScale().set(3);mc.options.pauseOnLostFocus=false;mc.options.hideGui=true;mc.options.tutorialStep=net.minecraft.client.tutorial.TutorialSteps.NONE;mc.getWindow().setWindowed(Math.clamp(parameter("CHAO_BALL074_WIDTH",1440),854,2560),Math.clamp(parameter("CHAO_BALL074_HEIGHT",900),480,1440));mc.resizeDisplay();mc.createWorldOpenFlows().createFreshLevel("BallToy074-"+System.currentTimeMillis(),new LevelSettings("BallToy074",GameType.CREATIVE,false,Difficulty.PEACEFUL,true,new GameRules(),WorldDataConfiguration.DEFAULT),new WorldOptions(2574L,false,false),r->r.registryOrThrow(Registries.WORLD_PRESET).getHolderOrThrow(WorldPresets.FLAT).value().createWorldDimensions(),mc.screen);}
    if(!setup&&mc.player!=null&&mc.getSingleplayerServer()!=null){setup=true;mc.setScreen(null);ChaoRenderer.suppressNames=true;onServer(mc.getSingleplayerServer(),()->{var server=mc.getSingleplayerServer();var level=server.overworld();level.setDayTime(6000);level.getGameRules().getRule(GameRules.RULE_DAYLIGHT).set(false,server);level.getGameRules().getRule(GameRules.RULE_DOMOBSPAWNING).set(false,server);for(var p:BlockPos.betweenClosed(-13,79,-11,13,86,15))level.setBlock(p,p.getY()==79?Blocks.SMOOTH_STONE.defaultBlockState():Blocks.AIR.defaultBlockState(),3);next(server,start());});}
    if(phase>=0&&phase<limit()&&mc.level!=null){if(clientPhase==-2&&phase==pendingPhase)return;if(clientPhase!=phase){clientPhase=phase;view=1;viewFrames=guiFrames=0;firstActionClient=Long.MIN_VALUE;renderedAction=-1;armRequested=false;lastCaptureNanos=0;}if(!armed&&!sourceReady(mc)){require(System.nanoTime()-playbackWall<30_000_000_000L,"source packet did not arrive");return;}camera(mc);}
    if(phase==limit()&&SAVED.get()==shots){finished=true;onServer(mc.getSingleplayerServer(),()->mc.getSingleplayerServer().tickRateManager().setFrozen(false));LogUtils.getLogger().info("BALL074_VISUAL_COMPLETE requestedCases={} renderedCases={} skippedCases=0 captures={} saved={} normal20TPS=true audioListening=false epochMillis={} output={}",limit()-start(),limit()-start(),shots,SAVED.get(),System.currentTimeMillis(),OUTPUT);mc.stop();}
  }
}
