package com.chaogarden.client;

import com.chaogarden.ChaoGarden;
import com.chaogarden.core.*;
import com.chaogarden.entity.ChaoEntity;
import com.chaogarden.interaction.ChaoBehaviorGoal;
import com.chaogarden.toys.*;
import com.google.gson.*;
import com.mojang.logging.LogUtils;
import java.nio.file.*;
import java.security.MessageDigest;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import net.minecraft.client.*;
import net.minecraft.client.gui.screens.AccessibilityOnboardingScreen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
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

/** Disposable normal-20TPS study: native commands and the actual production idle goal/renderer. */
@EventBusSubscriber(modid=ChaoGarden.MODID,value=Dist.CLIENT)
public final class IdleVisualSmoke078 {
  private record Shot(int tick,String label,String view,String expected) {}
  private record Case(String name,String id,String form,String coat,boolean rabbit,int eyes,int mouth,int iris,String mode) {
    boolean natural(){return mode.equals("natural");}
    boolean loop(){return mode.equals("loop");}
    boolean damage(){return mode.equals("damage");}
    boolean adult(){return !form.startsWith("baby_");}
    float alignment(){return form.contains("hero")?1:form.contains("dark")?-1:0;}
    int type(){return switch(form.split("_")[1]){case "swim"->ChaoState.SWIM_TYPE;case "fly"->ChaoState.FLY_TYPE;case "run"->ChaoState.RUN_TYPE;case "power"->ChaoState.POWER_TYPE;default->ChaoState.NORMAL;};}
    List<String> later(){var tokens=form.split("_");return Arrays.asList(tokens).subList(Math.min(2,tokens.length),tokens.length);}
    float rp(){return later().contains("run")?-.75f:later().contains("power")?.75f:0;}
    float sf(){return later().contains("swim")?-.75f:later().contains("fly")?.75f:0;}
    int interruptAt(){return loop()?160+Math.round(ChaoIdleRules078.duration(id)*.70f):Math.round(ChaoIdleRules078.duration(id)*.50f);}
  }
  private static List<Case> cases(){
    var all=List.of(
        new Case("baby_look_around","look_around","baby_neutral","natural",false,0,0,0,"once"),
        new Case("baby_look_up","look_up","baby_hero","natural",false,1,1,0,"once"),
        new Case("baby_think_chaos_lens","think","baby_dark","natural",false,4,3,11,"once"),
        new Case("baby_bashful_shiny_rabbit","bashful","baby_neutral","red_shiny",true,3,0,0,"once"),
        new Case("baby_chuckle_hero_lens","chuckle","baby_hero","natural",false,2,2,9,"once"),
        new Case("baby_tired_dark_lens","tired","baby_dark","natural",false,0,3,10,"once"),
        new Case("baby_ground_play","ground_play","baby_neutral","natural",false,0,1,0,"once"),
        new Case("adult_neutral_swim_fly_look_up","look_up","neutral_swim_fly","natural_shiny",false,0,0,0,"once"),
        new Case("adult_hero_power_fly_think_rabbit","think","hero_power_fly","natural",true,3,3,0,"once"),
        new Case("adult_dark_fly_swim_ground_play","ground_play","dark_fly_swim","red_shiny",false,4,4,0,"once"),
        new Case("adult_dark_run_fly_loop_stop","chuckle","dark_run_fly","natural_shiny",false,0,1,0,"loop"),
        new Case("adult_hero_swim_run_real_damage","tired","hero_swim_run","natural",false,0,3,0,"damage"),
        new Case("natural_curious_adult","","neutral_normal","natural",false,0,0,0,"natural"),
        new Case("natural_moderate_fatigue_baby","","baby_dark","natural",false,1,0,0,"natural"));
    String selected=System.getenv("CHAO_IDLE078_CASES");if(selected==null||selected.isBlank())return all;
    var out=new ArrayList<Case>();var used=new HashSet<Integer>();for(String value:selected.split(",")){int i=Integer.parseInt(value.trim());require(i>=0&&i<all.size()&&used.add(i),"invalid/duplicate case selection");out.add(all.get(i));}return List.copyOf(out);
  }
  private static final List<Case> CASES=cases();
  private static final Path OUTPUT=Path.of(System.getenv().getOrDefault("CHAO_IDLE078_OUTPUT","/workspace/.setup/idle078-client-final")).toAbsolutePath().normalize();
  private static final AtomicInteger SAVED=new AtomicInteger();
  private static volatile int phase=-1;
  private static volatile UUID actorId;
  private static volatile long elapsed=-1,caseAge;
  private static volatile String episode="",fatal;
  private static volatile boolean armed,complete,interrupted;
  private static ChaoEntity actor;
  private static ArmorStand observer;
  private static long spawned,began,lastTick,wallStart,playbackWall;
  private static String savedState,savedIdentity;
  private static ItemStack savedHat,savedOffhand;
  private static boolean originalNoAi,previewPreserved;
  private static int previewChecks,cancelChecks;
  private static boolean started,setup,finished,armRequested,renderedFrame,worldFrame;
  private static int clientPhase=-1,pending=-1,shotIndex,viewFrames,shots,worldFrames,actorFrames,activeFrames,recoveryFrames,damageFrames;
  private static String view="front",observedId="",observedGeometry="",observedState="";
  private static long observedTick;
  private static float observedWeight;
  private static boolean observedPreview;
  private static ChaoFaceRules076.Resolved observedFace;

  private static boolean enabled(){return "true".equalsIgnoreCase(System.getenv("CHAO_IDLE078_SMOKE"));}
  private static void require(boolean okay,String message){if(!okay)throw new IllegalStateException("IDLE078 "+message);}
  private static int parameter(String name,int fallback){try{return Integer.parseInt(System.getenv().getOrDefault(name,Integer.toString(fallback)));}catch(NumberFormatException e){return fallback;}}
  private static String sha(byte[] bytes){try{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));}catch(Exception e){throw new IllegalStateException(e);}}
  private static String sha(String text){return sha(text.getBytes(java.nio.charset.StandardCharsets.UTF_8));}
  private static String sha(Path path){try{return sha(Files.readAllBytes(path));}catch(Exception e){throw new IllegalStateException(e);}}
  /** Stable saved identity only. Natural care/age clocks and genuine damage are allowed to advance. */
  private static String identity(String encoded){
    var f=encoded.split(";",-1);var core=f[11].split(",");var extra=f[21].split(",");var out=new StringBuilder();
    for(int i=1;i<=10;i++)out.append(f[i]).append(';');
    for(int i:new int[]{0,1,2,3,4,5,6,9,10,11,12,13,14,20,21,22,23,24,25,26,27,28,29,31,32,33,34,35,36})out.append(core[i]).append(',');
    for(int i=12;i<=17;i++)out.append(';').append(f[i]);
    for(int i:new int[]{0,1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19,20,23,24,25,26,27,28,29,30,31,32,38,39,40})out.append(',').append(extra[i]);
    for(int i=22;i<=24;i++)out.append(';').append(f[i]);return out.toString();
  }
  private static void onServer(MinecraftServer server,Runnable task){server.execute(()->{try{task.run();}catch(Throwable e){fatal=e.toString();LogUtils.getLogger().error("IDLE078_ASYNC_FAILURE",e);}});}
  private record Morphology(boolean enabled,float runPower,float swimFly,float maturity) {}
  /** Read the actual production sampler for each first-evolution family in this catalog. */
  private static Morphology morphology(ChaoState s){
    if(!s.adult)return new Morphology(false,0,0,0);
    if(s.lockedAlignment()>=.5f&&s.lockedPrimaryType()==ChaoState.POWER_TYPE){var m=HeroPowerMorphology067.sample(s);return new Morphology(m.enabled(),m.runPower(),m.swimFly(),m.maturity());}
    if(s.lockedAlignment()>=.5f&&s.lockedPrimaryType()==ChaoState.SWIM_TYPE){var m=HeroSwimMorphology064.sample(s);return new Morphology(m.enabled(),m.runPower(),m.swimFly(),m.maturity());}
    if(s.lockedAlignment()<=-.5f&&s.lockedPrimaryType()==ChaoState.FLY_TYPE){var m=DarkFlyMorphology069.sample(s);return new Morphology(m.enabled(),m.runPower(),m.swimFly(),m.maturity());}
    if(s.lockedAlignment()<=-.5f&&s.lockedPrimaryType()==ChaoState.RUN_TYPE){var m=DarkRunMorphology070.sample(s);return new Morphology(m.enabled(),m.runPower(),m.swimFly(),m.maturity());}
    if(Math.abs(s.lockedAlignment())<.5f&&s.lockedPrimaryType()==ChaoState.SWIM_TYPE){var m=NeutralSwimMorphology059.sample(s);return new Morphology(m.enabled(),m.runPower(),m.swimFly(),m.maturity());}
    if(Math.abs(s.lockedAlignment())<.5f&&s.lockedPrimaryType()==ChaoState.NORMAL){var m=NeutralMorphology056.sample(s);return new Morphology(m.enabled(),m.runPower(),m.swimFly(),m.maturity());}
    throw new IllegalStateException("IDLE078 unsupported catalog morphology");
  }
  private static void verifyForm(Case c,ChaoState s,boolean initial){
    require(s.adult==c.adult(),"fixture life stage changed");if(!s.adult)return;
    require(s.lockedPrimaryType()==c.type()&&s.lockedAlignment()==c.alignment(),"fixture first identity changed");var m=morphology(s);
    require(m.enabled()&&Math.abs(m.runPower()-c.rp())<.0001f&&Math.abs(m.swimFly()-c.sf())<.0001f,"actual current-stat morphology does not match suffix axes: "+c.form()+" "+m);
    require(initial?Math.abs(m.maturity()-.65f)<.0001f:m.maturity()>=.6499f&&m.maturity()<=1,"fixture biological maturity changed unexpectedly");
  }
  private static ChaoState fixture(Case c){
    var s=new ChaoState();s.adult=c.adult();s.align=c.alignment();s.primaryType=c.type();s.hunger=100;s.energy=100;s.happiness=20;s.boredom=0;
    s.setBaselineFace076(c.eyes(),c.mouth());s.eyeColor=c.iris();s.setEvolutionStrength(s.adult?GradualEvolutionRules.ADULT_STRENGTH_MAX*.65f:0);
    if(s.adult){
      s.train(ChaoState.RUN,1200,new Random(7801));s.train(ChaoState.POWER,1200,new Random(7802));
      if(c.rp()!=0)s.train(c.rp()<0?ChaoState.RUN:ChaoState.POWER,720,new Random(7803));
      if(c.sf()!=0)s.train(c.sf()<0?ChaoState.SWIM:ChaoState.FLY,720,new Random(7804));
    }
    for(int i=0;i<ChaoIllness.COUNT;i++)s.setIllness(i,100);
    if(c.coat().contains("shiny")){s.shiny=true;s.shinyGenes=new int[]{1,1};}
    if(c.coat().equals("red_shiny")){s.expressedColor=2;s.colorGenes=new int[]{2,2};}
    if(c.rabbit()){int a=Arrays.asList(ChaoState.ANIMALS).indexOf("rabbit");require(a>=0,"rabbit missing");s.animalMask=s.animalPartsMask=1<<a;for(int i=0;i<s.animalParts.length;i++)if((ChaoState.animalSlots(a)&1<<i)!=0)s.animalParts[i]=a;}
    if(c.natural()){s.setPersonalitySlider(ChaoPersonality.CURIOUS,80);s.setPersonalitySlider(ChaoPersonality.SKILLFUL,60);if(!c.adult())s.energy=30;}
    String encoded=s.encode();var result=ChaoState.decode(encoded);require(encoded.equals(result.encode()),"fixture codec mismatch");verifyForm(c,result,true);return result;
  }
  private static void command(MinecraftServer server,String mode){
    var p=server.getPlayerList().getPlayers().getFirst();p.teleportTo(actor.getX(),actor.getY()+2,actor.getZ());p.setDeltaMovement(Vec3.ZERO);actor.setOnGround(true);
    String text="chao testidle "+CASES.get(phase).id()+" "+mode;
    try{require(server.getCommands().getDispatcher().execute(text,p.createCommandSourceStack().withPermission(2))==1,"native command rejected: "+text);}catch(com.mojang.brigadier.exceptions.CommandSyntaxException e){throw new IllegalStateException(e);}
    LogUtils.getLogger().info("IDLE078_COMMAND case={} command={} epochMillis={}",phase,text,System.currentTimeMillis());
  }
  private static void next(MinecraftServer server,int index){
    server.tickRateManager().setFrozen(true);var level=server.overworld();if(actor!=null){ToyPreview.stop(actor);ToyContent.endPlaying(actor,false);actor.discard();}
    var c=CASES.get(index);actor=ChaoGarden.CHAO.get().create(level);require(actor!=null,"actor create failed");actor.setState(fixture(c));originalNoAi=!c.natural()&&!c.loop();actor.setNoAi(originalNoAi);actor.getRandom().setSeed(7800+index);
    actor.moveTo(.5,80,3.5,180,0);actor.yBodyRot=actor.yBodyRotO=actor.yHeadRot=actor.yHeadRotO=180;actor.setOnGround(true);actor.setItemSlot(EquipmentSlot.OFFHAND,new ItemStack(Items.STICK));
    level.addFreshEntity(actor);actor.goalSelector.removeAllGoals(g->true);actor.targetSelector.removeAllGoals(g->true);
    if(c.natural())actor.goalSelector.addGoal(7,ChaoBehaviorGoal.idle(actor));else actor.deferBehavior(100000);
    actor.getPersistentData().putLong("ChaoToyNextPlay",Long.MAX_VALUE);
    savedState=actor.state().encode();savedIdentity=identity(savedState);savedHat=actor.getItemBySlot(EquipmentSlot.HEAD).copy();savedOffhand=actor.getItemBySlot(EquipmentSlot.OFFHAND).copy();
    spawned=lastTick=level.getGameTime();began=-1;elapsed=-1;caseAge=0;episode=c.id();previewChecks=cancelChecks=0;previewPreserved=interrupted=armed=complete=false;actorId=actor.getUUID();phase=index;
    var p=server.getPlayerList().getPlayers().getFirst();p.setGameMode(GameType.SPECTATOR);p.teleportTo(6.5,82,7.5);p.setNoGravity(true);p.setDeltaMovement(Vec3.ZERO);playbackWall=System.nanoTime();
    var morph=morphology(actor.state());LogUtils.getLogger().info("IDLE078_CASE {} name={} form={} mode={} rabbit={} coat={} originalNoAi={} firstType={} lockedAlignment={} runPower={} swimFly={} maturity={} state={} stateSha256={} identitySha256={} epochMillis={}",index,c.name(),c.form(),c.mode(),c.rabbit(),c.coat(),originalNoAi,actor.state().lockedPrimaryType(),actor.state().lockedAlignment(),morph.runPower(),morph.swimFly(),morph.maturity(),savedState,sha(savedState),sha(savedIdentity),System.currentTimeMillis());
  }
  private static void arm(MinecraftServer server,int expected){
    if(phase!=expected||armed)return;require(Math.abs(server.tickRateManager().tickrate()-20)<.00001f,"normal20TPS required");var c=CASES.get(phase);
    armed=true;playbackWall=System.nanoTime();server.tickRateManager().setFrozen(false);LogUtils.getLogger().info("IDLE078_ARMED case={} productionGoalFixture={} normal20TPS=true epochMillis={}",phase,c.natural(),System.currentTimeMillis());
  }
  private static int finish(Case c){return c.loop()?c.interruptAt()+40:c.natural()?ChaoIdleRules078.duration(episode)+40:194;}
  private static List<Shot> schedule(Case c){
    int d=ChaoIdleRules078.duration(episode);if(d==0)return List.of();
    if(c.damage())return List.of(new Shot(4,"entry","front","ENTRY"),new Shot(d/3,"action_front","front","ACTIVE"),new Shot(c.interruptAt()+2,"damage","side","DAMAGE"),new Shot(c.interruptAt()+30,"canceled_back","back","RECOVERED"),new Shot(180,"recovered","front","RECOVERED"));
    var out=new ArrayList<Shot>();out.add(new Shot(4,"entry","front","ENTRY"));out.add(new Shot(Math.round(d*.30f),"action_front","front","ACTIVE"));out.add(new Shot(d/2,"action_side","side","ACTIVE"));out.add(new Shot(Math.round(d*.72f),"action_back","back","ACTIVE"));out.add(new Shot(d+16,"calm_after_clip","front","CALM"));
    if(c.loop()){out.add(new Shot(160+d/2,"second_cycle","side","ACTIVE"));out.add(new Shot(c.interruptAt()+20,"stopped_recovery","back","RECOVERED"));}
    else out.add(new Shot(c.natural()?d+30:180,"recovered","front","RECOVERED"));return List.copyOf(out);
  }
  @SubscribeEvent public static void server(ServerTickEvent.Post event){
    if(!enabled()||phase<0||phase>=CASES.size()||!armed||complete)return;
    try{
      var server=event.getServer();long now=server.overworld().getGameTime();if(now==lastTick)return;lastTick=now;caseAge=now-spawned;var c=CASES.get(phase);
      require(actor!=null&&actor.isAlive()&&!actor.isRemoved(),"live actor disappeared");require(caseAge<2400,"natural admission/session deadline");
      if(began<0){
        if(!c.natural()){
          if(caseAge<20)return; // Let real gravity establish solid-floor contact before the native admission.
          savedState=actor.state().encode();require(savedIdentity.equals(identity(savedState)),"identity changed during initial settling");
          command(server,c.loop()?"loop":"once");require(actor.toyPlayingKind().equals("preview_idle_"+c.id()),"native idle token mismatch");began=now;
          LogUtils.getLogger().info("IDLE078_PREVIEW_STARTED case={} selected={} settledTicks={} careBaseline={} careBaselineSha256={} epochMillis={}",phase,episode,caseAge,savedState,sha(savedState),System.currentTimeMillis());
        }else {
          if(actor.getBehavior()!=ChaoEntity.Behavior.IDLE){if(caseAge%80==0)LogUtils.getLogger().info("IDLE078_NATURAL_WAIT case={} age={} position={} stateSha256={}",phase,caseAge,actor.position(),sha(actor.state().encode()));return;}
          episode=actor.behaviorDetail();require(ChaoIdleRules078.valid(episode),"natural goal selected unknown detail");began=now-(long)actor.getBehaviorElapsed(0);
          LogUtils.getLogger().info("IDLE078_NATURAL_STARTED case={} selected={} waitedTicks={} productionGoal=true controlledGoalFixture=true epochMillis={}",phase,episode,caseAge,System.currentTimeMillis());
        }
        playbackWall=System.nanoTime();
      }
      elapsed=now-began;
      if(!c.natural()&&!interrupted){if(ToyPreview.active(actor)){require(savedState.equals(actor.state().encode()),"active preview changed saved care/genes/age/lessons");previewChecks++;}else {require(!c.loop()&&elapsed>=160,"unexpected preview cancellation");previewPreserved=true;}}
      if(!interrupted&&(c.loop()||c.damage())&&elapsed>=c.interruptAt()){
        require(savedState.equals(actor.state().encode()),"saved state changed before explicit interruption");previewPreserved=true;
        if(c.loop())command(server,"stop");else require(actor.hurt(server.overworld().damageSources().generic(),.1f),"real damage refused");interrupted=true;
        LogUtils.getLogger().info("IDLE078_INTERRUPT case={} kind={} tick={} realDamage={} epochMillis={}",phase,c.loop()?"stop":"damage",elapsed,c.damage(),System.currentTimeMillis());
      }
      if(interrupted&&elapsed>=c.interruptAt()+2){require(!ToyPreview.active(actor)&&actor.toyPlayingTicks()==0,"interruption did not clear preview clock");cancelChecks++;}
      if(elapsed<finish(c))return;
      require(!ToyPreview.active(actor)&&actor.toyPlayingTicks()==0&&actor.toyTarget()==null,"session left preview/clock/target");require(actor.isNoAi()==originalNoAi&&!actor.isNoGravity(),"AI/gravity not restored");require(actor.getMainHandItem().isEmpty()&&actor.getHeldChaoItem().isEmpty(),"real item minted");
      require(ItemStack.matches(savedHat,actor.getItemBySlot(EquipmentSlot.HEAD))&&ItemStack.matches(savedOffhand,actor.getItemBySlot(EquipmentSlot.OFFHAND)),"real equipment changed");require(savedIdentity.equals(identity(actor.state().encode())),"saved identity changed");require(actor.getPersistentData().getLong("ChaoToyNextPlay")==Long.MAX_VALUE,"preview cooldown changed");
      if(!c.natural())require(previewPreserved&&previewChecks>5,"exact preview state preservation not witnessed");if(c.loop()||c.damage())require(cancelChecks>0,"server cancellation not observed");
      complete=true;server.tickRateManager().setFrozen(true);LogUtils.getLogger().info("IDLE078_NATIVE_COMPLETE case={} name={} selected={} elapsed={} previewStatePreserved={} preservationChecks={} originalNoAi={} restoredNoAi={} cancelChecks={} baselineEyes={} baselineMouth={} state={} stateSha256={} identitySha256={} epochMillis={}",phase,c.name(),episode,elapsed,previewPreserved,previewChecks,originalNoAi,actor.isNoAi(),cancelChecks,actor.state().baselineEyes(),actor.state().baselineMouth(),actor.state().encode(),sha(actor.state().encode()),sha(savedIdentity),System.currentTimeMillis());
    }catch(Throwable e){fatal=e.toString();LogUtils.getLogger().error("IDLE078_ASYNC_FAILURE",e);}
  }
  private static ChaoEntity observed(Minecraft mc){if(mc.level!=null&&actorId!=null)for(var e:mc.level.entitiesForRendering())if(e instanceof ChaoEntity c&&actorId.equals(c.getUUID()))return c;return null;}
  private static void camera(Minecraft mc){
    var c=observed(mc);if(c==null)return;if(observer==null||observer.level()!=mc.level){observer=new ArmorStand(mc.level,0,0,0);observer.setInvisible(true);observer.setNoGravity(true);}
    var moments=schedule(CASES.get(phase));String wanted=shotIndex<moments.size()?moments.get(shotIndex).view():"front";if(!wanted.equals(view)){view=wanted;viewFrames=0;}
    Vec3 focus=c.position().add(0,c.state().adult?.57:.42,0),forward=Vec3.directionFromRotation(0,c.yBodyRot),side=new Vec3(-forward.z,0,forward.x);double a=Math.toRadians(view.equals("side")?90:view.equals("back")?180:0);
    Vec3 eye=focus.add(forward.scale(Math.cos(a)*2.65)).add(side.scale(Math.sin(a)*2.65)).add(0,.30,0),delta=focus.subtract(eye);float yaw=(float)(Math.toDegrees(Math.atan2(delta.z,delta.x))-90),pitch=(float)-Math.toDegrees(Math.atan2(delta.y,Math.hypot(delta.x,delta.z)));
    observer.moveTo(eye.x,eye.y-observer.getEyeHeight(),eye.z,yaw,pitch);observer.xo=observer.xOld=observer.getX();observer.yo=observer.yOld=observer.getY();observer.zo=observer.zOld=observer.getZ();observer.yRotO=yaw;observer.xRotO=pitch;observer.setYHeadRot(yaw);observer.yHeadRotO=yaw;mc.options.setCameraType(CameraType.FIRST_PERSON);mc.setCameraEntity(observer);
  }
  @SubscribeEvent public static void restoreCamera(ClientTickEvent.Pre e){if(enabled()){var mc=Minecraft.getInstance();if(mc.player!=null&&mc.getCameraEntity()!=mc.player)mc.setCameraEntity(mc.player);}}
  @SubscribeEvent public static void frameBegin(RenderFrameEvent.Pre e){if(enabled())renderedFrame=worldFrame=false;}
  @SubscribeEvent public static void rendered(RenderLivingEvent.Post<?,?> event){
    if(!enabled()||phase<0||phase>=CASES.size()||SceneShaderPack.shadows()||!(event.getEntity()instanceof ChaoEntity c)||!c.getUUID().equals(actorId))return;var mc=Minecraft.getInstance();if(mc.screen!=null||mc.getOverlay()!=null||mc.getCameraEntity()!=observer)return;
    var m=(ChaoModel)event.getRenderer().getModel();var f=m.resolvedFace076();var entry=CASES.get(phase);require(m.officialPosesFinite()&&f!=null,"actual pose/face missing or nonfinite");require(!SceneShaderPack.active(),"neutral-light study requires no shader pack");verifyForm(entry,c.state(),false);
    require(!f.nativeExcluded()&&f.baselineEyes()==entry.eyes()&&f.baselineMouth()==entry.mouth()&&f.irisColor()==entry.iris(),"rendered baseline/iris changed");require(c.state().baselineEyes()==entry.eyes()&&c.state().baselineMouth()==entry.mouth(),"saved baseline changed");
    String style=entry.iris()==9?"HERO_LENS":entry.iris()==10?"DARK_LENS":entry.iris()==11?"CHAOS_LENS":entry.adult()&&entry.alignment()>0?"HERO":entry.adult()&&entry.alignment()<0?"DARK":"NEUTRAL";require(f.style().name().equals(style),"rendered family/lens changed");
    observedId=ChaoIdleAnimation078.id(c);observedWeight=ChaoIdleRules078.weight(observedId,ChaoIdleAnimation078.elapsed(c,event.getPartialTick()));observedFace=f;observedTick=elapsed;observedPreview=c.toyPlayingTicks()>0;observedGeometry=m.gameplayFormKey();observedState=c.state().encode();renderedFrame=true;actorFrames++;
    if(observedWeight>.5f)activeFrames++;if(elapsed>=0&&observedWeight==0&&(!observedPreview||elapsed>ChaoIdleRules078.duration(episode)))recoveryFrames++;if(f.eyeSource()==ChaoFaceRules076.Source.DAMAGE){require(f.mouthSource()==ChaoFaceRules076.Source.DAMAGE,"damage did not own both face channels");damageFrames++;}
    if(observedWeight>.9f&&Set.of("tired","chuckle").contains(observedId))require(f.eyeSource()==ChaoFaceRules076.Source.IDLE&&f.mouthSource()==ChaoFaceRules076.Source.IDLE,"production idle face not rendered");
    if(observedWeight>.5f)require(m.officialIdleGestureWeight()==0,"imported torso/arm sway overlaps authored idle");
  }
  @SubscribeEvent public static void world(RenderLevelStageEvent e){if(enabled()&&phase>=0&&phase<CASES.size()&&e.getStage()==RenderLevelStageEvent.Stage.AFTER_LEVEL&&renderedFrame&&!SceneShaderPack.shadows()){var mc=Minecraft.getInstance();worldFrame=mc.screen==null&&mc.getOverlay()==null&&mc.getCameraEntity()==observer;}}
  private static boolean matches(Shot shot){return switch(shot.expected()){case "ACTIVE"->observedId.equals(episode)&&observedWeight>.5f;case "DAMAGE"->observedFace.eyeSource()==ChaoFaceRules076.Source.DAMAGE&&observedWeight==0;case "RECOVERED"->observedWeight==0&&!observedPreview&&observedFace.eyeSource()!=ChaoFaceRules076.Source.IDLE&&observedFace.mouthSource()!=ChaoFaceRules076.Source.IDLE;case "CALM"->observedWeight==0&&observedFace.eyeSource()!=ChaoFaceRules076.Source.IDLE;default->true;};}
  private static void capture(Minecraft mc,Shot shot){
    int index=phase;var c=CASES.get(index);long tick=observedTick;String state=observedState,id=episode,geometry=observedGeometry;var face=observedFace;float weight=observedWeight;boolean preview=observedPreview;var observed=ChaoState.decode(state);verifyForm(c,observed,false);var morph=morphology(observed);
    String name=String.format("idle078-%02d-%s-%s-%s-t%03d.png",index,c.name(),shot.label(),shot.view(),tick);Path file=OUTPUT.resolve("screenshots").resolve(name);try{Files.createDirectories(file.getParent());}catch(Exception e){throw new IllegalStateException(e);}
    Screenshot.grab(OUTPUT.toFile(),name,mc.getMainRenderTarget(),message->{require(Files.isRegularFile(file),"PNG did not save");SAVED.incrementAndGet();LogUtils.getLogger().info("IDLE078_CAPTURE {} case={} selected={} label={} view={} tick={} weight={} preview={} eyes={} mouth={} style={} eyeSource={} mouthSource={} baselineEyes={} baselineMouth={} iris={} geometry={} firstType={} lockedAlignment={} runPower={} swimFly={} maturity={} observerState={} observerStateSha256={} sha256={} epochMillis={}",name,index,id,shot.label(),shot.view(),tick,weight,preview,face.eyes(),face.mouth(),face.style(),face.eyeSource(),face.mouthSource(),face.baselineEyes(),face.baselineMouth(),face.irisColor(),geometry,observed.lockedPrimaryType(),observed.lockedAlignment(),morph.runPower(),morph.swimFly(),morph.maturity(),state,sha(state),sha(file),System.currentTimeMillis());});shots++;shotIndex++;viewFrames=0;
  }
  @SubscribeEvent public static void frameEnd(RenderFrameEvent.Post e){
    if(!enabled()||phase<0||phase>=CASES.size()||clientPhase!=phase||!worldFrame)return;var mc=Minecraft.getInstance();worldFrames++;viewFrames++;
    if(!armed){if(viewFrames>=12&&!armRequested){armRequested=true;int i=phase;onServer(mc.getSingleplayerServer(),()->arm(mc.getSingleplayerServer(),i));}return;}
    if(elapsed<0)return;var c=CASES.get(phase);var moments=schedule(c);
    if(shotIndex<moments.size()){var shot=moments.get(shotIndex);if(elapsed>=shot.tick()&&viewFrames>=3){if(matches(shot))capture(mc,shot);else require(elapsed<shot.tick()+12,"required rendered state absent at "+c.name()+"/"+shot.label()+" face="+observedFace+" weight="+observedWeight);}return;}
    if(!complete)return;require(actorFrames>20&&worldFrames>20&&activeFrames>10&&recoveryFrames>3,"actual active/recovery render coverage missing");if(c.damage())require(damageFrames>0,"genuine damage face not observed");require((System.nanoTime()-playbackWall)/1e9>=finish(c)/20.0-.8,"study ran faster than20TPS");
    LogUtils.getLogger().info("IDLE078_RENDER_COMPLETE case={} name={} selected={} worldFrames={} actorFrames={} activeFrames={} recoveryFrames={} damageFrames={} captures={} normal20TPS=true epochMillis={}",phase,c.name(),episode,worldFrames,actorFrames,activeFrames,recoveryFrames,damageFrames,moments.size(),System.currentTimeMillis());
    int done=phase;pending=phase;clientPhase=-2;onServer(mc.getSingleplayerServer(),()->{if(phase==done){if(phase+1<CASES.size())next(mc.getSingleplayerServer(),phase+1);else phase=CASES.size();}});
  }
  private static void catalog(){
    var root=new JsonObject();root.addProperty("kind","Actual production idle commands/goal and renderer; no alternate poses");root.addProperty("normal_tps",20);root.addProperty("audio_listening",false);root.addProperty("independent_multiplayer",false);root.addProperty("natural_fixture","Production ChaoBehaviorGoal.idle at priority7; unrelated goals removed, no forced behavior/selection");var rows=new JsonArray();
    for(int i=0;i<CASES.size();i++){var c=CASES.get(i);var proof=fixture(c);var morph=morphology(proof);var r=new JsonObject();r.addProperty("index",i);r.addProperty("name",c.name());r.addProperty("id",c.id());r.addProperty("form",c.form());r.addProperty("mode",c.mode());r.addProperty("coat",c.coat());r.addProperty("rabbit",c.rabbit());r.addProperty("eyes",c.eyes());r.addProperty("mouth",c.mouth());r.addProperty("iris",c.iris());r.addProperty("locked_first_type",c.adult()?proof.lockedPrimaryType():ChaoState.NORMAL);r.addProperty("locked_alignment",proof.lockedAlignment());r.addProperty("requested_run_power",c.rp());r.addProperty("requested_swim_fly",c.sf());r.addProperty("actual_run_power",morph.runPower());r.addProperty("actual_swim_fly",morph.swimFly());r.addProperty("biological_maturity",morph.maturity());r.addProperty("axis_training_progress",720);r.addProperty("production_clip_ticks",ChaoIdleRules078.duration(c.id()));r.addProperty("preview_ticks",160);r.addProperty("native_command",c.natural()?"none: actual production goal":"chao testidle "+c.id()+" "+(c.loop()?"loop":"once"));rows.add(r);}root.add("cases",rows);
    try{Files.createDirectories(OUTPUT);Files.writeString(OUTPUT.resolve("case-catalog.json"),new GsonBuilder().setPrettyPrinting().create().toJson(root)+"\n");}catch(Exception e){throw new IllegalStateException(e);}
  }
  @SubscribeEvent public static void client(ClientTickEvent.Post e){
    if(!enabled()||finished)return;var mc=Minecraft.getInstance();if(fatal!=null)throw new IllegalStateException("IDLE078 server failure: "+fatal);if(wallStart==0)wallStart=System.nanoTime();require(System.nanoTime()-wallStart<2_400_000_000_000L,"study wall deadline");
    if(mc.screen instanceof AccessibilityOnboardingScreen)mc.setScreen(new TitleScreen());
    if(!started&&mc.screen instanceof TitleScreen){started=true;catalog();mc.options.renderDistance().set(3);mc.options.simulationDistance().set(5);mc.options.framerateLimit().set(45);mc.options.fov().set(40);mc.options.pauseOnLostFocus=false;mc.options.hideGui=true;mc.options.tutorialStep=net.minecraft.client.tutorial.TutorialSteps.NONE;mc.getWindow().setWindowed(parameter("CHAO_IDLE078_WIDTH",1440),parameter("CHAO_IDLE078_HEIGHT",900));mc.resizeDisplay();mc.createWorldOpenFlows().createFreshLevel("Idle078-"+System.currentTimeMillis(),new LevelSettings("Idle078",GameType.CREATIVE,false,Difficulty.PEACEFUL,true,new GameRules(),WorldDataConfiguration.DEFAULT),new WorldOptions(2578L,false,false),r->r.registryOrThrow(Registries.WORLD_PRESET).getHolderOrThrow(WorldPresets.FLAT).value().createWorldDimensions(),mc.screen);}
    if(!setup&&mc.player!=null&&mc.getSingleplayerServer()!=null){setup=true;mc.setScreen(null);ChaoRenderer.suppressNames=true;onServer(mc.getSingleplayerServer(),()->{var server=mc.getSingleplayerServer();var level=server.overworld();level.setDayTime(6000);level.setWeatherParameters(0,100000,false,false);level.getGameRules().getRule(GameRules.RULE_DAYLIGHT).set(false,server);level.getGameRules().getRule(GameRules.RULE_WEATHER_CYCLE).set(false,server);level.getGameRules().getRule(GameRules.RULE_DOMOBSPAWNING).set(false,server);for(var p:BlockPos.betweenClosed(-14,79,-12,14,86,16))level.setBlock(p,p.getY()==79?Blocks.SMOOTH_STONE.defaultBlockState():Blocks.AIR.defaultBlockState(),3);next(server,0);});}
    if(phase>=0&&phase<CASES.size()&&mc.level!=null){if(clientPhase==-2&&phase==pending)return;if(clientPhase!=phase){clientPhase=phase;shotIndex=0;viewFrames=worldFrames=actorFrames=activeFrames=recoveryFrames=damageFrames=0;armRequested=false;observedFace=null;view="front";}camera(mc);}
    if(phase==CASES.size()&&SAVED.get()==shots){finished=true;onServer(mc.getSingleplayerServer(),()->mc.getSingleplayerServer().tickRateManager().setFrozen(false));LogUtils.getLogger().info("IDLE078_VISUAL_COMPLETE requestedCases={} renderedCases={} skippedCases=0 captures={} saved={} normal20TPS=true audioListening=false independentMultiplayer=false epochMillis={} output={}",CASES.size(),CASES.size(),shots,SAVED.get(),System.currentTimeMillis(),OUTPUT);mc.stop();}
  }
}
