package com.chaogarden.client;

import com.chaogarden.ChaoGarden;
import com.chaogarden.core.*;
import com.chaogarden.entity.ChaoEntity;
import com.chaogarden.toys.*;
import com.google.gson.*;
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
import org.joml.Vector3f;

/** Disposable actual-client face study. All visible faces, poses and clocks are production code. */
@EventBusSubscriber(modid=ChaoGarden.MODID,value=Dist.CLIENT)
public final class FaceVisualSmoke076 {
  private record Shot(int tick,String label,String view,String expected) {}
  private static final List<Shot> BASE=List.of(
      new Shot(8,"baseline","front","BASELINE"),new Shot(24,"baseline_left","left","BASELINE"),
      new Shot(40,"baseline_right","right","BASELINE"),new Shot(56,"baseline_profile","profile","BASELINE"),
      new Shot(72,"baseline_context","context","BASELINE"));
  private static final List<Shot> EPISODE=List.of(
      new Shot(8,"healthy","front","BASELINE"),new Shot(40,"active","front","ACTIVE"),
      new Shot(60,"active_left","left","ACTIVE"),new Shot(80,"active_profile","profile","ACTIVE"),
      new Shot(142,"recovered","front","BASELINE"),new Shot(162,"recovered_right","right","BASELINE"),
      new Shot(182,"recovered_context","context","BASELINE"));
  private static final List<Shot> TOY=List.of(
      new Shot(8,"healthy","front","BASELINE"),new Shot(68,"active","front","ACTIVE"),
      new Shot(112,"active_left","left","ACTIVE"),new Shot(144,"active_profile","profile","ACTIVE"),
      new Shot(196,"recovered","front","BASELINE"),new Shot(214,"recovered_context","context","BASELINE"));
  private static final List<Shot> LOOP=List.of(
      new Shot(8,"healthy","front","BASELINE"),new Shot(68,"active","front","ACTIVE"),
      new Shot(112,"active_left","left","ACTIVE"),new Shot(224,"second_cycle","profile","ACTIVE"),
      new Shot(276,"recovered","front","BASELINE"),new Shot(294,"recovered_context","context","BASELINE"));
  private static final List<Shot> DAMAGE=List.of(
      new Shot(8,"healthy","front","BASELINE"),new Shot(48,"happy_preview","front","ACTIVE"),
      new Shot(64,"real_damage","left","DAMAGE"),new Shot(88,"after_damage","profile","REACTION"),
      new Shot(142,"recovered","front","BASELINE"),new Shot(182,"recovered_context","context","BASELINE"));
  private record Case(String name,String form,int eyes,int mouth,int iris,String coat,String mode,String detail,String schedule) {
    List<Shot> shots(){return switch(schedule){case "EPISODE"->EPISODE;case "TOY"->TOY;case "LOOP"->LOOP;case "DAMAGE"->DAMAGE;default->BASE;};}
    int finish(){return switch(schedule){case "EPISODE","DAMAGE"->194;case "TOY"->226;case "LOOP"->306;default->86;};}
    boolean preview(){return mode.equals("preview")||mode.equals("toy")||mode.equals("damage");}
    boolean loop(){return schedule.equals("LOOP");}
    boolean adult(){return !form.startsWith("baby_");}
    float alignment(){return form.contains("hero")?1:form.contains("dark")?-1:0;}
    int type(){return form.contains("swim")?ChaoState.SWIM_TYPE:form.contains("fly")?ChaoState.FLY_TYPE:form.contains("run")?ChaoState.RUN_TYPE:form.contains("power")?ChaoState.POWER_TYPE:ChaoState.NORMAL;}
    boolean nativeFace(){return form.startsWith("chaos_")||form.equals("omochao")||form.equals("tails");}
  }
  private static List<Case> cases(){
    var all=new ArrayList<Case>();
    all.add(new Case("adult_neutral_normal","neutral_normal",0,0,0,"natural","baseline","","BASE"));
    all.add(new Case("adult_neutral_half","neutral_swim",1,1,0,"natural","baseline","","BASE"));
    all.add(new Case("adult_neutral_happy","neutral_fly",2,2,0,"natural","baseline","","BASE"));
    all.add(new Case("adult_neutral_mean","neutral_run",3,3,0,"natural","baseline","","BASE"));
    all.add(new Case("adult_neutral_chaos","neutral_power",4,4,0,"natural","baseline","","BASE"));
    all.add(new Case("adult_hero_normal","hero_normal",0,0,0,"natural","baseline","","BASE"));
    all.add(new Case("adult_hero_half","hero_swim",1,1,0,"natural","baseline","","BASE"));
    all.add(new Case("adult_hero_happy","hero_fly",2,2,0,"natural","baseline","","BASE"));
    all.add(new Case("adult_hero_mean","hero_run",3,3,0,"natural","baseline","","BASE"));
    all.add(new Case("adult_hero_chaos","hero_power",4,4,0,"natural","baseline","","BASE"));
    all.add(new Case("adult_dark_normal","dark_normal",0,0,0,"natural","baseline","","BASE"));
    all.add(new Case("adult_dark_half","dark_swim",1,1,0,"natural","baseline","","BASE"));
    all.add(new Case("adult_dark_happy","dark_fly",2,2,0,"natural","baseline","","BASE"));
    all.add(new Case("adult_dark_mean","dark_run",3,3,0,"natural","baseline","","BASE"));
    all.add(new Case("adult_dark_chaos","dark_power",4,4,0,"natural","baseline","","BASE"));
    all.add(new Case("baby_normal_grin","baby_neutral",0,3,0,"natural","baseline","","BASE"));
    all.add(new Case("baby_half_none","baby_hero",1,0,0,"natural","baseline","","BASE"));
    all.add(new Case("baby_happy_wavy","baby_dark",2,4,0,"natural","baseline","","BASE"));
    all.add(new Case("baby_mean_smile","baby_neutral",3,1,0,"natural","baseline","","BASE"));
    all.add(new Case("baby_chaos_frown","baby_hero",4,2,0,"natural","baseline","","BASE"));
    all.add(new Case("iris_blue","neutral_normal",0,1,1,"natural","baseline","","BASE"));
    all.add(new Case("iris_green","hero_normal",0,2,2,"white","baseline","","BASE"));
    all.add(new Case("iris_red","dark_normal",0,3,3,"red_shiny","baseline","","BASE"));
    all.add(new Case("iris_yellow","baby_neutral",1,4,4,"green_mono","baseline","","BASE"));
    all.add(new Case("iris_purple","hero_swim",3,1,5,"natural","baseline","","BASE"));
    all.add(new Case("iris_pink","dark_fly",0,0,6,"natural","baseline","","BASE"));
    all.add(new Case("iris_orange","neutral_run",0,1,7,"natural","baseline","","BASE"));
    all.add(new Case("iris_aqua","hero_power",1,3,8,"natural","baseline","","BASE"));
    all.add(new Case("special_hero_lens","dark_normal",0,1,9,"natural","baseline","","BASE"));
    all.add(new Case("special_dark_lens","hero_normal",3,2,10,"natural","baseline","","BASE"));
    all.add(new Case("special_chaos_lens","baby_neutral",4,3,11,"natural","baseline","","BASE"));
    all.add(new Case("native_chaos_neutral","chaos_neutral",4,0,0,"natural","baseline","","BASE"));
    all.add(new Case("native_chaos_hero","chaos_hero",4,0,0,"natural","baseline","","BASE"));
    all.add(new Case("native_chaos_dark","chaos_dark",4,0,0,"natural","baseline","","BASE"));
    all.add(new Case("native_omochao","omochao",0,1,0,"natural","baseline","","BASE"));
    all.add(new Case("native_tails","tails",0,1,0,"natural","baseline","","BASE"));
    all.add(new Case("actual_joy_recovery","baby_neutral",0,3,0,"natural","mood","joy","EPISODE"));
    all.add(new Case("actual_fear_recovery","hero_normal",0,3,0,"natural","mood","fear","EPISODE"));
    all.add(new Case("actual_cry_recovery","dark_normal",0,3,0,"natural","mood","cry","EPISODE"));
    all.add(new Case("actual_anger_recovery","neutral_run",0,3,0,"natural","mood","anger","EPISODE"));
    all.add(new Case("actual_tired_recovery","hero_swim",0,3,0,"natural","mood","tired","EPISODE"));
    all.add(new Case("actual_relax_recovery","dark_fly",0,3,0,"natural","mood","relax","EPISODE"));
    all.add(new Case("actual_lonely_recovery","baby_dark",0,3,0,"natural","mood","lonely","EPISODE"));
    all.add(new Case("actual_cough_cure","baby_neutral",0,3,0,"natural","illness","cough","EPISODE"));
    all.add(new Case("actual_cold_cure","hero_normal",0,3,0,"natural","illness","cold","EPISODE"));
    all.add(new Case("actual_rash_cure","dark_run",0,3,0,"natural","illness","rash","EPISODE"));
    all.add(new Case("actual_runny_nose_cure","neutral_fly",0,3,0,"natural","illness","runny_nose","EPISODE"));
    all.add(new Case("actual_hiccups_cure","hero_swim",0,3,0,"natural","illness","hiccups","EPISODE"));
    all.add(new Case("actual_stomach_ache_cure","dark_normal",0,3,0,"natural","illness","stomach_ache","EPISODE"));
    all.add(new Case("actual_sleep_wake","baby_hero",0,3,0,"natural","sleep","","EPISODE"));
    all.add(new Case("actual_damage_over_happy","dark_power",0,3,0,"natural","damage","happy","DAMAGE"));
    all.add(new Case("face_happy_once","neutral_normal",3,2,0,"natural","preview","happy","TOY"));
    all.add(new Case("face_tired_loop_stop","hero_run",0,3,0,"natural","preview","tired","LOOP"));
    all.add(new Case("toy_radio_once","hero_swim",3,3,0,"natural","toy","radio","TOY"));
    all.add(new Case("toy_flute_once","baby_neutral",0,3,0,"natural","toy","flute","TOY"));
    all.add(new Case("toy_trumpet_once","dark_fly",0,3,0,"natural","toy","trumpet","TOY"));
    all.add(new Case("toy_ball_once","dark_run",0,3,0,"natural","toy","ball","TOY"));
    all.add(new Case("toy_crayons_once","hero_power",0,3,0,"natural","toy","crayons","TOY"));
    all.add(new Case("hat_preserves_face","neutral_normal",3,1,0,"natural","baseline","pumpkin","BASE"));
    all.add(new Case("rabbit_preserves_face","baby_neutral",1,3,0,"red_shiny","baseline","rabbit","BASE"));
    String select=System.getenv("CHAO_FACE076_CASES");if(select==null||select.isBlank())return List.copyOf(all);
    var chosen=new ArrayList<Case>();var used=new HashSet<Integer>();for(String value:select.split(",")){int i=Integer.parseInt(value.trim());require(i>=0&&i<all.size()&&used.add(i),"invalid/duplicate selection");chosen.add(all.get(i));}return List.copyOf(chosen);
  }
  private static final List<Case> CASES=cases();
  private static final Path OUTPUT=Path.of(System.getenv().getOrDefault("CHAO_FACE076_OUTPUT","/workspace/.setup/face076-client-final01")).toAbsolutePath().normalize();
  private static final BlockPos TOY_POS=new BlockPos(0,80,2);
  private static final AtomicInteger SAVED=new AtomicInteger();
  private static volatile int phase=-1;
  private static volatile long elapsed;
  private static volatile UUID actorId;
  private static volatile boolean armed,complete;
  private static volatile String fatal;
  private static ChaoEntity actor;
  private static ArmorStand observer;
  private static long began,wallStart,caseWall;
  private static String baselineState,previewState,savedIdentity;
  private static int previewChecks;
  private static ItemStack savedHat,savedOffhand;
  private static boolean changed,restored,hurt,previewPreserved;
  private static boolean started,setup,finished,armRequested,renderedFrame,worldFrame;
  private static int clientPhase=-1,pending=-1,shotIndex,viewFrames,shots,worldFrames,actorFrames,activeFrames,recoveryFrames,observerChecks;
  private static String view="front";
  private static Vec3 skullFocus;
  private static ChaoFaceRules076.Resolved observedFace;
  private static String observedState,observedGeometry;
  private static long observedTick;
  private static boolean observedRest,observedToy;
  private static boolean enabled(){return "true".equalsIgnoreCase(System.getenv("CHAO_FACE076_SMOKE"));}
  private static int parameter(String name,int fallback){try{return Integer.parseInt(System.getenv().getOrDefault(name,Integer.toString(fallback)));}catch(NumberFormatException e){return fallback;}}
  private static void require(boolean okay,String message){if(!okay)throw new IllegalStateException("FACE076 "+message);}
  private static String sha(byte[] bytes){try{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));}catch(Exception e){throw new IllegalStateException(e);}}
  private static String sha(String text){return sha(text.getBytes(java.nio.charset.StandardCharsets.UTF_8));}
  private static String sha(Path path){try{return sha(Files.readAllBytes(path));}catch(Exception e){throw new IllegalStateException(e);}}
  /** Stable saved genes/identity only: ordinary age, care, emotion and illness clocks may advance after preview. */
  private static String identity(String encoded){
    var fields=encoded.split(";",-1);var core=fields[11].split(",");var extra=fields[21].split(",");var out=new StringBuilder();
    for(int i=1;i<=7;i++)out.append(fields[i]).append(';');
    for(int i:new int[]{0,1,2,3,4,5,6,9,10,11,12,13,14,20,21,22,23,24,25,26,27,28,29,30,31,32,33,34,35,36})out.append(core[i]).append(',');
    for(int i=12;i<=17;i++)out.append(';').append(fields[i]);
    for(int i:new int[]{0,1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19,20,28,29,30,31,32,38,39,40})out.append(',').append(extra[i]);
    for(int i=22;i<=24;i++)out.append(';').append(fields[i]);return out.toString();
  }
  private static void onServer(MinecraftServer server,Runnable task){server.execute(()->{try{task.run();}catch(Throwable e){fatal=e.toString();LogUtils.getLogger().error("FACE076_ASYNC_FAILURE",e);}});}
  private static ChaoState fixture(Case c){
    var s=new ChaoState();s.adult=c.adult();s.align=c.alignment();s.primaryType=c.type();s.hunger=100;s.energy=100;s.happiness=20;s.boredom=0;
    s.setBaselineFace076(c.eyes(),c.mouth());s.eyeColor=c.iris();s.setEvolutionStrength(s.adult?GradualEvolutionRules.ADULT_STRENGTH_MAX*.5f:0);
    if(s.adult){s.train(ChaoState.RUN,1200,new Random(7601));s.train(ChaoState.POWER,1200,new Random(7602));}
    if(c.form().startsWith("chaos_"))s.immortal=true;
    if(c.form().equals("omochao")||c.form().equals("tails"))s.character=c.form();
    for(int i=0;i<ChaoIllness.COUNT;i++)s.setIllness(i,100);
    if(c.coat().equals("white")){s.expressedColor=1;s.colorGenes=new int[]{1,1};}
    if(c.coat().equals("red_shiny")){s.expressedColor=2;s.colorGenes=new int[]{2,2};s.shiny=true;s.shinyGenes=new int[]{1,1};}
    if(c.coat().equals("green_mono")){s.expressedColor=10;s.colorGenes=new int[]{10,10};s.twoTone=false;s.toneGenes=new int[]{0,0};}
    if(c.detail().equals("rabbit")){int a=Arrays.asList(ChaoState.ANIMALS).indexOf("rabbit");require(a>=0,"rabbit missing");s.animalMask=s.animalPartsMask=1<<a;for(int i=0;i<s.animalParts.length;i++)if((ChaoState.animalSlots(a)&1<<i)!=0)s.animalParts[i]=a;}
    String encoded=s.encode();var out=ChaoState.decode(encoded);require(encoded.equals(out.encode()),"fixture codec mismatch");require(out.baselineEyes()==c.eyes()&&out.baselineMouth()==c.mouth(),"fixture baseline lost");return out;
  }
  private static void dispatch(MinecraftServer server,String command){
    var p=server.getPlayerList().getPlayers().getFirst();p.teleportTo(actor.getX(),actor.getY()+2,actor.getZ());p.setDeltaMovement(Vec3.ZERO);actor.setOnGround(true);
    try{require(server.getCommands().getDispatcher().execute(command,p.createCommandSourceStack().withPermission(2))==1,"native command rejected: "+command);}catch(com.mojang.brigadier.exceptions.CommandSyntaxException e){throw new IllegalStateException(e);}
    LogUtils.getLogger().info("FACE076_COMMAND case={} command={} epochMillis={}",phase,command.replace(' ','_'),System.currentTimeMillis());
  }
  private static String command(Case c,String mode){return "chao "+(c.mode().equals("toy")?"testtoy":"testface")+" "+c.detail()+" "+mode;}
  private static void roundtrip(MinecraftServer server,String when){
    var tag=new CompoundTag();actor.saveWithoutId(tag);var copy=ChaoGarden.CHAO.get().create(server.overworld());require(copy!=null,"NBT copy create failed");copy.load(tag);
    require(copy.state().encode().equals(actor.state().encode()),"NBT state mismatch "+when);require(copy.state().baselineEyes()==CASES.get(phase).eyes()&&copy.state().baselineMouth()==CASES.get(phase).mouth(),"NBT baseline lost");
    require(ItemStack.matches(copy.getItemBySlot(EquipmentSlot.HEAD),actor.getItemBySlot(EquipmentSlot.HEAD)),"NBT hat lost");copy.discard();
    LogUtils.getLogger().info("FACE076_NBT case={} when={} stateSha256={} baselineEyes={} baselineMouth={} epochMillis={}",phase,when,sha(actor.state().encode()),actor.state().baselineEyes(),actor.state().baselineMouth(),System.currentTimeMillis());
  }
  private static void next(MinecraftServer server,int index){
    server.tickRateManager().setFrozen(true);var level=server.overworld();if(actor!=null){ToyPreview.stop(actor);ToyContent.endPlaying(actor,false);actor.discard();}
    for(var p:BlockPos.betweenClosed(-12,80,-10,12,83,14))if(level.getBlockState(p).getBlock()instanceof ChaoToyBlock)level.setBlock(p,Blocks.AIR.defaultBlockState(),3);
    var c=CASES.get(index);phase=index;actor=ChaoGarden.CHAO.get().create(level);require(actor!=null,"actor create failed");actor.setState(fixture(c));actor.setNoAi(true);actor.deferBehavior(100000);actor.getRandom().setSeed(7600+index);
    if(c.mode().equals("toy")&&(c.detail().equals("radio")||c.detail().equals("ball")))level.setBlock(TOY_POS,ToyContent.block("chao_"+c.detail()).defaultBlockState().setValue(ChaoToyBlock.FACING,Direction.SOUTH),3);
    Vec3 at=c.detail().equals("radio")?ToyContent.radioAnchor(actor,TOY_POS):c.detail().equals("ball")?Vec3.atBottomCenterOf(TOY_POS).add(0,0,BallPlayRules074.contactDistance(actor.state())+.10):new Vec3(.5,80,3.5);
    actor.moveTo(at.x,at.y,at.z,180,0);actor.yBodyRot=actor.yBodyRotO=actor.yHeadRot=actor.yHeadRotO=180;actor.setOnGround(true);
    if(c.detail().equals("pumpkin"))actor.setItemSlot(EquipmentSlot.HEAD,new ItemStack(Items.CARVED_PUMPKIN));
    level.addFreshEntity(actor);actor.goalSelector.removeAllGoals(g->true);actor.targetSelector.removeAllGoals(g->true);actor.getPersistentData().putLong("ChaoToyNextPlay",Long.MAX_VALUE);
    baselineState=actor.state().encode();savedIdentity=identity(baselineState);savedHat=actor.getItemBySlot(EquipmentSlot.HEAD).copy();savedOffhand=actor.getItemBySlot(EquipmentSlot.OFFHAND).copy();previewState=null;previewChecks=0;previewPreserved=false;changed=restored=hurt=armed=complete=false;elapsed=0;actorId=actor.getUUID();
    var p=server.getPlayerList().getPlayers().getFirst();p.setGameMode(GameType.SPECTATOR);p.teleportTo(at.x+4,82,at.z+4);p.setNoGravity(true);p.setDeltaMovement(Vec3.ZERO);
    roundtrip(server,"initial");caseWall=System.nanoTime();
    LogUtils.getLogger().info("FACE076_CASE {} name={} form={} mode={} detail={} baselineEyes={} baselineMouth={} iris={} state={} stateSha256={} originalNoAi=true epochMillis={}",index,c.name(),c.form(),c.mode(),c.detail().isEmpty()?"none":c.detail(),c.eyes(),c.mouth(),c.iris(),baselineState,sha(baselineState),System.currentTimeMillis());
  }
  private static void arm(MinecraftServer server,int expected){if(phase!=expected||armed)return;require(Math.abs(server.tickRateManager().tickrate()-20)<.00001f,"normal 20 TPS required");began=server.overworld().getGameTime();armed=true;caseWall=System.nanoTime();server.tickRateManager().setFrozen(false);}
  private static int illness(Case c){return switch(c.detail()){case "cough"->ChaoIllness.COUGH;case "cold"->ChaoIllness.COLD;case "rash"->ChaoIllness.RASH;case "runny_nose"->ChaoIllness.RUNNY_NOSE;case "hiccups"->ChaoIllness.HICCUPS;default->ChaoIllness.STOMACH_ACHE;};}
  private static String style(Case c){return c.nativeFace()?"NATIVE":c.iris()==9?"HERO_LENS":c.iris()==10?"DARK_LENS":c.iris()==11?"CHAOS_LENS":c.adult()&&c.alignment()>0?"HERO":c.adult()&&c.alignment()<0?"DARK":"NEUTRAL";}
  private static ChaoEntity.Behavior symptom(Case c){return switch(c.detail()){case "cough"->ChaoEntity.Behavior.COUGH;case "cold"->ChaoEntity.Behavior.SNEEZE;case "rash"->ChaoEntity.Behavior.SCRATCH;case "runny_nose"->ChaoEntity.Behavior.RUNNY_NOSE;case "hiccups"->ChaoEntity.Behavior.HICCUP;default->ChaoEntity.Behavior.STOMACH_ACHE;};}
  private static void activate(MinecraftServer server,Case c){
    var s=actor.state();
    if(c.preview()){previewState=s.encode();dispatch(server,command(c,c.loop()?"loop":"once"));}
    else if(c.mode().equals("illness")){s.setIllness(illness(c),-100);actor.startBehavior(symptom(c),90);}
    else if(c.mode().equals("sleep")){s.sleeping=true;s.setEmotion(ChaoEmotion.SLEEPINESS,9000);s.setEmotion(ChaoEmotion.TIREDNESS,8000);s.energy=30;}
    else if(c.mode().equals("mood"))switch(c.detail()){
      case "joy"->s.setEmotion(ChaoEmotion.JOY,190);case "fear"->s.setEmotion(ChaoEmotion.FEAR,190);case "cry"->s.setEmotion(ChaoEmotion.CRY,190);case "anger"->s.setEmotion(ChaoEmotion.ANGER,190);case "relax"->s.setEmotion(ChaoEmotion.RELAX,190);case "lonely"->s.setEmotion(ChaoEmotion.LONELY,9000);case "tired"->s.setEmotion(ChaoEmotion.TIREDNESS,9000);default->throw new IllegalStateException("unknown mood");
    }
    actor.sync();changed=true;roundtrip(server,"active");LogUtils.getLogger().info("FACE076_STATE case={} stage=active tick={} state={} stateSha256={} actualState={} epochMillis={}",phase,elapsed,s.encode(),sha(s.encode()),!c.preview(),System.currentTimeMillis());
  }
  private static void provePreview(String boundary){
    require(!previewPreserved,"preview terminal proof duplicated");String state=actor.state().encode();require(previewState.equals(state),"preview changed saved care/identity at "+boundary);previewPreserved=true;
    LogUtils.getLogger().info("FACE076_PREVIEW_PRESERVED case={} boundary={} tick={} activeChecks={} state={} expectedSha256={} stateSha256={} epochMillis={}",phase,boundary,elapsed,previewChecks,state,sha(previewState),sha(state),System.currentTimeMillis());
  }
  private static void restore(MinecraftServer server,Case c){
    if(c.preview()){if(c.loop()){dispatch(server,command(c,"stop"));require(!ToyPreview.active(actor),"explicit stop left preview active");provePreview("explicit_stop");}else if(c.mode().equals("damage"))ToyPreview.stop(actor);}
    else if(c.mode().equals("illness")){require(actor.state().cureIllness(illness(c)),"real illness cure rejected");LogUtils.getLogger().info("FACE076_CURE case={} illness={} value={} immunity={} tick={} epochMillis={}",phase,illness(c),actor.state().illness(illness(c)),actor.state().illnessImmunityTicks(illness(c)),elapsed,System.currentTimeMillis());}
    if(!c.preview()||c.mode().equals("damage")){actor.clearBehavior();actor.setState(ChaoState.decode(baselineState));actor.deferBehavior(100000);}
    actor.sync();restored=true;LogUtils.getLogger().info("FACE076_STATE case={} stage=recovered tick={} state={} stateSha256={} actualState={} epochMillis={}",phase,elapsed,actor.state().encode(),sha(actor.state().encode()),!c.preview(),System.currentTimeMillis());
  }
  @SubscribeEvent public static void server(ServerTickEvent.Post event){
    if(!enabled()||phase<0||phase>=CASES.size()||!armed||complete)return;var server=event.getServer();if(server.tickRateManager().isFrozen())return;
    try{
      elapsed=server.overworld().getGameTime()-began;var c=CASES.get(phase);require(elapsed<c.finish()+300,"scene deadline exceeded");
      require(actor.state().baselineEyes()==c.eyes()&&actor.state().baselineMouth()==c.mouth(),"saved identity rerolled");
      if(!changed&&!c.mode().equals("baseline")&&elapsed>=20)activate(server,c);
      if(c.preview()&&changed&&!hurt){if(ToyPreview.active(actor)){require(previewState.equals(actor.state().encode()),"active preview changed saved care/identity");previewChecks++;}else if(!previewPreserved){require(!c.loop()&&!c.mode().equals("damage")&&elapsed>=180&&elapsed<=181,"unexpected early/late preview expiry");provePreview("expiry");}}
      if(c.mode().equals("damage")&&!hurt&&elapsed>=60){provePreview("before_damage");require(actor.hurt(server.overworld().damageSources().generic(),.1f),"real damage refused");hurt=true;LogUtils.getLogger().info("FACE076_DAMAGE case={} tick={} real=true epochMillis={}",phase,elapsed,System.currentTimeMillis());}
      if(!restored&&changed&&elapsed>=(c.loop()?240:c.preview()&&!c.mode().equals("damage")?184:110))restore(server,c);
      if(elapsed<c.finish())return;
      require(!ToyPreview.active(actor)&&actor.toyPlayingTicks()==0&&actor.toyTarget()==null,"preview/target survived cleanup");require(actor.isNoAi()&&!actor.isNoGravity(),"AI/gravity restoration failed");
      require(ItemStack.matches(savedHat,actor.getItemBySlot(EquipmentSlot.HEAD))&&ItemStack.matches(savedOffhand,actor.getItemBySlot(EquipmentSlot.OFFHAND)),"real equipment changed");require(actor.getMainHandItem().isEmpty()&&actor.getHeldChaoItem().isEmpty(),"preview minted items");
      require(actor.getPersistentData().getLong("ChaoToyNextPlay")==Long.MAX_VALUE,"cooldown changed");if(c.preview())require(previewPreserved,"preview exact-state terminal not observed");
      require(savedIdentity.equals(identity(actor.state().encode())),"saved genes/locked identity changed after recovery");
      roundtrip(server,"complete");complete=true;server.tickRateManager().setFrozen(true);
      LogUtils.getLogger().info("FACE076_NATIVE_COMPLETE {} name={} elapsed={} baselineEyes={} baselineMouth={} previewPreserved={} originalNoAi=true restoredNoAi={} state={} stateSha256={} stableIdentitySha256={} epochMillis={}",phase,c.name(),elapsed,c.eyes(),c.mouth(),previewPreserved,actor.isNoAi(),actor.state().encode(),sha(actor.state().encode()),sha(savedIdentity),System.currentTimeMillis());
    }catch(Throwable e){fatal=e.toString();LogUtils.getLogger().error("FACE076_ASYNC_FAILURE",e);}
  }
  private static ChaoEntity observed(Minecraft mc){if(mc.level!=null&&actorId!=null)for(var e:mc.level.entitiesForRendering())if(e instanceof ChaoEntity c&&actorId.equals(c.getUUID()))return c;return null;}
  private static Vec3 point(ChaoEntity c,ChaoModel model,float[] p){
    var frame=new PoseStack();model.transformAccessoryAnchor("skull",frame);var v=frame.last().pose().transformPosition(new Vector3f(p[0]/16,p[1]/16,p[2]/16));float size=c.state().immortal?.86f:c.state().adult?.8f:.64f;
    Vec3 forward=Vec3.directionFromRotation(0,c.yBodyRot),side=new Vec3(-forward.z,0,forward.x);return c.position().add(side.scale(v.x*size)).add(forward.scale(-v.z*size)).add(0,(1.5-v.y)*size,0);
  }
  private static void camera(Minecraft mc){
    var c=observed(mc);if(c==null)return;if(observer==null||observer.level()!=mc.level){observer=new ArmorStand(mc.level,0,0,0);observer.setInvisible(true);observer.setNoGravity(true);}
    String wanted=shotIndex<CASES.get(phase).shots().size()?CASES.get(phase).shots().get(shotIndex).view():"context";if(!wanted.equals(view)){view=wanted;viewFrames=0;}
    Vec3 focus=skullFocus==null?c.position().add(0,.6,0):skullFocus;double angle=switch(view){case "left"->-35;case "right"->35;case "profile"->75;default->0;};
    Vec3 forward=Vec3.directionFromRotation(0,c.yBodyRot),side=new Vec3(-forward.z,0,forward.x);double radians=Math.toRadians(angle),distance=view.equals("context")?3.5:2.6;
    Vec3 eye=focus.add(forward.scale(Math.cos(radians)*distance)).add(side.scale(Math.sin(radians)*distance)).add(0,.025,0),delta=focus.subtract(eye);
    float yaw=(float)(Math.toDegrees(Math.atan2(delta.z,delta.x))-90),pitch=(float)-Math.toDegrees(Math.atan2(delta.y,Math.hypot(delta.x,delta.z)));
    observer.moveTo(eye.x,eye.y-observer.getEyeHeight(),eye.z,yaw,pitch);observer.xo=observer.xOld=observer.getX();observer.yo=observer.yOld=observer.getY();observer.zo=observer.zOld=observer.getZ();observer.yRotO=yaw;observer.xRotO=pitch;observer.setYHeadRot(yaw);observer.yHeadRotO=yaw;mc.options.setCameraType(CameraType.FIRST_PERSON);mc.setCameraEntity(observer);
  }
  @SubscribeEvent public static void restoreCamera(ClientTickEvent.Pre e){if(enabled()){var mc=Minecraft.getInstance();if(mc.player!=null&&mc.getCameraEntity()!=mc.player)mc.setCameraEntity(mc.player);}}
  @SubscribeEvent public static void frameBegin(RenderFrameEvent.Pre e){if(enabled())renderedFrame=worldFrame=false;}
  @SubscribeEvent public static void rendered(RenderLivingEvent.Post<?,?> event){
    if(!enabled()||phase<0||phase>=CASES.size()||SceneShaderPack.shadows()||!(event.getEntity()instanceof ChaoEntity c)||!c.getUUID().equals(actorId))return;var mc=Minecraft.getInstance();if(mc.screen!=null||mc.getOverlay()!=null||mc.getCameraEntity()!=observer)return;
    var m=(ChaoModel)event.getRenderer().getModel();require(m.officialPosesFinite(),"nonfinite actual pose");require(!SceneShaderPack.active(),"neutral-light study requires no active shader pack");var f=m.resolvedFace076();require(f!=null,"resolver observation missing");var entry=CASES.get(phase);
    require(f.baselineEyes()==entry.eyes()&&f.baselineMouth()==entry.mouth()&&c.state().baselineEyes()==entry.eyes()&&c.state().baselineMouth()==entry.mouth(),"observer baseline mismatch");require(f.nativeExcluded()==entry.nativeFace(),"native face exclusion mismatch");require(f.style().name().equals(style(entry))&&f.irisColor()==entry.iris(),"observer family/lens style mismatch");
    float[] b=m.accessoryBounds("skull");if(b!=null)skullFocus=point(c,m,new float[]{(b[0]+b[3])*.5f,(b[1]+b[4])*.5f,(b[2]+b[5])*.5f});
    observedFace=f;observedState=c.state().encode();observedGeometry=m.gameplayFormKey();observedTick=elapsed;observedRest=c.isResting();observedToy=c.toyPlayingTicks()>0;renderedFrame=true;actorFrames++;observerChecks++;
    if(elapsed>=32&&elapsed<=100&&changed)activeFrames++;if(restored||entry.mode().equals("baseline"))recoveryFrames++;
  }
  @SubscribeEvent public static void world(RenderLevelStageEvent e){if(enabled()&&phase>=0&&phase<CASES.size()&&e.getStage()==RenderLevelStageEvent.Stage.AFTER_LEVEL&&renderedFrame&&!SceneShaderPack.shadows()){var mc=Minecraft.getInstance();worldFrame=mc.screen==null&&mc.getOverlay()==null&&mc.getCameraEntity()==observer;}}
  private static boolean matches(Case c,Shot shot,ChaoFaceRules076.Resolved f){
    if(f==null)return false;if(c.nativeFace())return f.nativeExcluded()&&f.eyeSource()==ChaoFaceRules076.Source.NATIVE&&f.mouthSource()==ChaoFaceRules076.Source.NATIVE;
    if(shot.expected().equals("BASELINE"))return f.eyeSource()==ChaoFaceRules076.Source.BASELINE&&f.mouthSource()==ChaoFaceRules076.Source.BASELINE&&f.eyes()==ChaoFaceRules076.baselineEyes(c.eyes())&&f.mouth()==ChaoFaceRules076.baselineMouth(c.mouth())&&!observedRest&&!observedToy;
    if(shot.expected().equals("DAMAGE"))return f.eyeSource()==ChaoFaceRules076.Source.DAMAGE&&f.mouthSource()==ChaoFaceRules076.Source.DAMAGE&&f.eyes()==ChaoFaceRules076.Eyes.DISTRESS&&f.mouth()==ChaoFaceRules076.Mouth.FROWN;
    if(shot.expected().equals("REACTION"))return !observedToy&&f.eyeSource()!=ChaoFaceRules076.Source.DAMAGE;
    if(c.mode().equals("illness")){boolean acute=Set.of("cough","cold","hiccups").contains(c.detail());return f.eyeSource()==ChaoFaceRules076.Source.SYMPTOM&&f.mouthSource()==ChaoFaceRules076.Source.SYMPTOM&&f.eyes()==(acute?ChaoFaceRules076.Eyes.DISTRESS:ChaoFaceRules076.Eyes.HALF_CLOSED)&&f.mouth()==(acute?ChaoFaceRules076.Mouth.OPEN:ChaoFaceRules076.Mouth.WAVY);}
    if(c.mode().equals("sleep"))return observedRest&&f.eyeSource()==ChaoFaceRules076.Source.SLEEP&&f.mouthSource()==ChaoFaceRules076.Source.SLEEP&&f.eyes()==ChaoFaceRules076.Eyes.SLEEP&&f.mouth()==ChaoFaceRules076.Mouth.NONE;
    if(c.mode().equals("mood")){var eyes=switch(c.detail()){case "joy"->ChaoFaceRules076.Eyes.HAPPY_CLOSED;case "fear"->ChaoFaceRules076.Eyes.SURPRISED;case "cry"->ChaoFaceRules076.Eyes.DISTRESS;case "anger"->ChaoFaceRules076.Eyes.MEAN;default->ChaoFaceRules076.Eyes.HALF_CLOSED;};var mouth=switch(c.detail()){case "joy","relax"->ChaoFaceRules076.Mouth.SMILE;case "fear"->ChaoFaceRules076.Mouth.OPEN;case "tired"->ChaoFaceRules076.Mouth.WAVY;default->ChaoFaceRules076.Mouth.FROWN;};return f.eyeSource()==ChaoFaceRules076.Source.MOOD&&f.mouthSource()==ChaoFaceRules076.Source.MOOD&&f.eyes()==eyes&&f.mouth()==mouth;}
    if(c.mode().equals("preview")||c.mode().equals("damage"))return observedToy&&f.eyeSource()==(c.detail().equals("tired")?ChaoFaceRules076.Source.MOOD:ChaoFaceRules076.Source.ACTIVITY)&&f.mouthSource()==f.eyeSource()&&f.eyes()==(c.detail().equals("tired")?ChaoFaceRules076.Eyes.HALF_CLOSED:ChaoFaceRules076.Eyes.HAPPY_CLOSED)&&f.mouth()==(c.detail().equals("tired")?ChaoFaceRules076.Mouth.WAVY:ChaoFaceRules076.Mouth.SMILE);
    if(!observedToy)return false;
    if(c.detail().equals("flute")||c.detail().equals("trumpet"))return f.eyeSource()==ChaoFaceRules076.Source.ACTIVITY&&f.mouthSource()==ChaoFaceRules076.Source.ACTIVITY&&f.eyes()==ChaoFaceRules076.Eyes.HAPPY_CLOSED&&f.mouth()==ChaoFaceRules076.Mouth.OPEN;
    if(c.detail().equals("radio")&&!shot.label().equals("active_profile")||c.detail().equals("crayons"))return f.eyeSource()==ChaoFaceRules076.Source.ACTIVITY&&f.mouthSource()==ChaoFaceRules076.Source.ACTIVITY&&f.eyes()==ChaoFaceRules076.Eyes.HAPPY_CLOSED&&f.mouth()==ChaoFaceRules076.Mouth.SMILE;
    return (f.eyeSource()==ChaoFaceRules076.Source.BASELINE||f.eyeSource()==ChaoFaceRules076.Source.ACTIVITY)&&f.eyeSource()==f.mouthSource();
  }
  private static void capture(Minecraft mc,Shot shot){
    var c=CASES.get(phase);int index=phase;long tick=observedTick;var face=observedFace;String state=observedState,stateHash=sha(state),geometry=observedGeometry;boolean resting=observedRest,toyActive=observedToy;
    String name=String.format("face076-%02d-%s-%s-%s-t%03d.png",index,c.name(),shot.label(),shot.view(),tick);Path file=OUTPUT.resolve("screenshots").resolve(name);try{Files.createDirectories(file.getParent());}catch(Exception e){throw new IllegalStateException(e);}
    Screenshot.grab(OUTPUT.toFile(),name,mc.getMainRenderTarget(),message->{require(Files.isRegularFile(file),"PNG did not save");SAVED.incrementAndGet();LogUtils.getLogger().info("FACE076_CAPTURE {} case={} label={} angle={} tick={} caseAge={} baselineEyes={} baselineMouth={} eyes={} mouth={} style={} eyeSource={} mouthSource={} nativeExcluded={} iris={} geometry={} resting={} toyActive={} observerState={} observerStateSha256={} crop=600,350,840,550 fov=35 sha256={} epochMillis={}",name,index,shot.label(),shot.view(),tick,tick,face.baselineEyes(),face.baselineMouth(),face.eyes(),face.mouth(),face.style(),face.eyeSource(),face.mouthSource(),face.nativeExcluded(),face.irisColor(),geometry,resting,toyActive,state,stateHash,sha(file),System.currentTimeMillis());});shots++;shotIndex++;viewFrames=0;
  }
  @SubscribeEvent public static void frameEnd(RenderFrameEvent.Post e){
    if(!enabled()||phase<0||phase>=CASES.size()||clientPhase!=phase||!worldFrame)return;var mc=Minecraft.getInstance();worldFrames++;viewFrames++;
    if(!armed){if(viewFrames>=12&&!armRequested){armRequested=true;int i=phase;onServer(mc.getSingleplayerServer(),()->arm(mc.getSingleplayerServer(),i));}return;}
    var c=CASES.get(phase);if(shotIndex<c.shots().size()){var shot=c.shots().get(shotIndex);if(elapsed>=shot.tick()&&viewFrames>=4){if(matches(c,shot,observedFace))capture(mc,shot);else require(elapsed<shot.tick()+18,"required expression absent at "+c.name()+"/"+shot.label()+" got "+observedFace);}return;}
    if(!complete)return;require(actorFrames>20&&worldFrames>20&&observerChecks>20&&recoveryFrames>3,"actual observer/render coverage missing");if(!c.mode().equals("baseline"))require(activeFrames>10,"actual active frames missing");require((System.nanoTime()-caseWall)/1e9>=c.finish()/20.0-.8,"study ran faster than normal 20 TPS");
    LogUtils.getLogger().info("FACE076_RENDER_COMPLETE {} name={} worldFrames={} actorFrames={} activeFrames={} recoveryFrames={} observerChecks={} captures={} normal20TPS=true epochMillis={}",phase,c.name(),worldFrames,actorFrames,activeFrames,recoveryFrames,observerChecks,c.shots().size(),System.currentTimeMillis());
    int done=phase;pending=phase;clientPhase=-2;onServer(mc.getSingleplayerServer(),()->{if(phase==done){if(phase+1<CASES.size())next(mc.getSingleplayerServer(),phase+1);else phase=CASES.size();}});
  }
  private static void catalog(){
    var root=new JsonObject();root.addProperty("kind","Actual production face and activity study; temporary fixture state is explicit, no alternate renderer");root.addProperty("normal_tps",20);root.addProperty("fov",35);root.addProperty("audio_listening",false);root.addProperty("independent_multiplayer",false);var rows=new JsonArray();
    for(int i=0;i<CASES.size();i++){var c=CASES.get(i);var r=new JsonObject();r.addProperty("index",i);r.addProperty("name",c.name());r.addProperty("form",c.form());r.addProperty("eyes",c.eyes());r.addProperty("mouth",c.mouth());r.addProperty("iris",c.iris());r.addProperty("coat",c.coat());r.addProperty("mode",c.mode());r.addProperty("detail",c.detail());r.addProperty("schedule",c.schedule());r.addProperty("finish_tick",c.finish());r.addProperty("native_excluded",c.nativeFace());var moments=new JsonArray();for(var s:c.shots()){var j=new JsonObject();j.addProperty("tick",s.tick());j.addProperty("label",s.label());j.addProperty("view",s.view());j.addProperty("expected",s.expected());moments.add(j);}r.add("shots",moments);rows.add(r);}root.add("cases",rows);
    try{Files.createDirectories(OUTPUT);Files.writeString(OUTPUT.resolve("case-catalog.json"),new GsonBuilder().setPrettyPrinting().create().toJson(root)+"\n");}catch(Exception e){throw new IllegalStateException(e);}
  }
  @SubscribeEvent public static void client(ClientTickEvent.Post e){
    if(!enabled()||finished)return;var mc=Minecraft.getInstance();if(fatal!=null)throw new IllegalStateException("FACE076 server failure: "+fatal);if(wallStart==0)wallStart=System.nanoTime();require(System.nanoTime()-wallStart<2_400_000_000_000L,"study wall deadline");
    if(mc.screen instanceof AccessibilityOnboardingScreen)mc.setScreen(new TitleScreen());
    if(!started&&mc.screen instanceof TitleScreen){started=true;catalog();mc.options.renderDistance().set(3);mc.options.simulationDistance().set(5);mc.options.framerateLimit().set(45);mc.options.fov().set(35);mc.options.pauseOnLostFocus=false;mc.options.hideGui=true;mc.options.tutorialStep=net.minecraft.client.tutorial.TutorialSteps.NONE;mc.getWindow().setWindowed(parameter("CHAO_FACE076_WIDTH",1440),parameter("CHAO_FACE076_HEIGHT",900));mc.resizeDisplay();mc.createWorldOpenFlows().createFreshLevel("Face076-"+System.currentTimeMillis(),new LevelSettings("Face076",GameType.CREATIVE,false,Difficulty.PEACEFUL,true,new GameRules(),WorldDataConfiguration.DEFAULT),new WorldOptions(2576L,false,false),r->r.registryOrThrow(Registries.WORLD_PRESET).getHolderOrThrow(WorldPresets.FLAT).value().createWorldDimensions(),mc.screen);}
    if(!setup&&mc.player!=null&&mc.getSingleplayerServer()!=null){setup=true;mc.setScreen(null);ChaoRenderer.suppressNames=true;onServer(mc.getSingleplayerServer(),()->{var server=mc.getSingleplayerServer();var level=server.overworld();level.setDayTime(6000);level.setWeatherParameters(0,100000,false,false);level.getGameRules().getRule(GameRules.RULE_DAYLIGHT).set(false,server);level.getGameRules().getRule(GameRules.RULE_WEATHER_CYCLE).set(false,server);level.getGameRules().getRule(GameRules.RULE_DOMOBSPAWNING).set(false,server);for(var p:BlockPos.betweenClosed(-14,79,-12,14,86,16))level.setBlock(p,p.getY()==79?Blocks.SMOOTH_STONE.defaultBlockState():Blocks.AIR.defaultBlockState(),3);next(server,0);});}
    if(phase>=0&&phase<CASES.size()&&mc.level!=null){if(clientPhase==-2&&phase==pending)return;if(clientPhase!=phase){clientPhase=phase;shotIndex=0;viewFrames=worldFrames=actorFrames=activeFrames=recoveryFrames=observerChecks=0;armRequested=false;skullFocus=null;observedFace=null;view="front";}camera(mc);}
    if(phase==CASES.size()&&SAVED.get()==shots){finished=true;onServer(mc.getSingleplayerServer(),()->mc.getSingleplayerServer().tickRateManager().setFrozen(false));LogUtils.getLogger().info("FACE076_VISUAL_COMPLETE requestedCases={} renderedCases={} skippedCases=0 captures={} saved={} normal20TPS=true audioListening=false independentMultiplayer=false epochMillis={} output={}",CASES.size(),CASES.size(),shots,SAVED.get(),System.currentTimeMillis(),OUTPUT);mc.stop();}
  }
}
