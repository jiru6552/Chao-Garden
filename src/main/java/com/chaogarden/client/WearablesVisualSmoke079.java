package com.chaogarden.client;

import com.chaogarden.ChaoGarden;
import com.chaogarden.core.*;
import com.chaogarden.entity.ChaoEntity;
import com.chaogarden.toys.*;
import com.google.gson.*;
import com.mojang.logging.LogUtils;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.MessageDigest;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import net.minecraft.client.*;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.*;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
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

/** Opt-in disposable actual-client wardrobe study. Excluded from release by *VisualSmoke*. */
@EventBusSubscriber(modid=ChaoGarden.MODID,value=Dist.CLIENT)
public final class WearablesVisualSmoke079 {
  private record Wear(String id,EquipmentSlot slot) {}
  private record Case(int sourceIndex,String name,String form,List<Wear> gear,String clip,boolean toy,boolean loop) {}
  private record MotionShot(int tick,String label,int view) {}
  private static final List<Wear> WEAR=List.of(
      new Wear("chao_garden:apple_hat",EquipmentSlot.HEAD),
      new Wear("chao_garden:cardboard_box_hat",EquipmentSlot.HEAD),
      new Wear("chao_garden:pan_hat",EquipmentSlot.HEAD),
      new Wear("chao_garden:paper_bag_hat",EquipmentSlot.HEAD),
      new Wear("chao_garden:empty_can_hat",EquipmentSlot.HEAD),
      new Wear("chao_garden:stump_hat",EquipmentSlot.HEAD),
      new Wear("chao_garden:flower_pot_hat",EquipmentSlot.HEAD),
      new Wear("chao_garden:bucket_hat",EquipmentSlot.HEAD),
      new Wear("chao_garden:red_wool_beanie",EquipmentSlot.HEAD),
      new Wear("chao_garden:blue_wool_beanie",EquipmentSlot.HEAD),
      new Wear("chao_garden:black_white_wool_beanie",EquipmentSlot.HEAD),
      new Wear("chao_garden:eggshell_hat",EquipmentSlot.HEAD),
      new Wear("minecraft:carved_pumpkin",EquipmentSlot.HEAD),
      new Wear("minecraft:melon",EquipmentSlot.HEAD),
      new Wear("minecraft:skeleton_skull",EquipmentSlot.HEAD),
      new Wear("chao_garden:baseball_cap_accessory",EquipmentSlot.FEET),
      new Wear("chao_garden:bowtie_accessory",EquipmentSlot.LEGS),
      new Wear("chao_garden:round_glasses_accessory",EquipmentSlot.OFFHAND),
      new Wear("chao_garden:aviators_accessory",EquipmentSlot.OFFHAND),
      new Wear("chao_garden:headphones_accessory",EquipmentSlot.FEET),
      new Wear("chao_garden:pacifier_accessory",EquipmentSlot.OFFHAND),
      new Wear("chao_garden:press_hat_accessory",EquipmentSlot.FEET),
      new Wear("chao_garden:blue_hoodie_accessory",EquipmentSlot.CHEST),
      new Wear("chao_garden:pink_hoodie_accessory",EquipmentSlot.CHEST),
      new Wear("chao_garden:cheese_bowtie_accessory",EquipmentSlot.LEGS));
  private static final List<String> FORMS=List.of("baby_neutral","baby_hero","baby_dark",
      "adult_neutral_type0","adult_neutral_type1","adult_neutral_type2","adult_neutral_type3","adult_neutral_type4",
      "adult_hero_type0","adult_hero_type1","adult_hero_type2","adult_hero_type3","adult_hero_type4",
      "adult_dark_type0","adult_dark_type1","adult_dark_type2","adult_dark_type3","adult_dark_type4");
  private static final List<String> VIEWS=List.of("front","left","right","back","above");
  private static final String MODE=System.getenv().getOrDefault("CHAO_WEAR079_MODE","catalog");
  private static final String PHASE=System.getenv().getOrDefault("CHAO_WEAR079_PHASE","before");
  private static final Path OUTPUT=Path.of(System.getenv().getOrDefault("CHAO_WEAR079_OUTPUT","/workspace/.setup/wear079-"+PHASE+"-"+MODE)).toAbsolutePath().normalize();
  private static final Gson GSON=new GsonBuilder().setPrettyPrinting().create();
  private static final AtomicInteger SAVED=new AtomicInteger();
  private static final List<Case> CASES=cases();
  private static volatile int phase=-1;
  private static volatile UUID actorId;
  private static volatile String fatal,expectedState="";
  private static volatile boolean settled,complete,stopped;
  private static volatile long elapsed;
  private static ChaoEntity actor;
  private static ItemEntity dropped;
  private static ArmorStand observer;
  private static long spawned,began,lastTick,wallStart;
  private static int clientPhase=-1,pending=-1,viewIndex,viewFrames,actorFrames,worldFrames,shots,completedCases,iconFrames;
  private static boolean started,setup,finished,renderedFrame,worldFrame;
  private static String observedState="",observedEquipment="",observedGeometry="",observedMotion="";
  private static long observedTick;
  private static JsonObject observedProof;
  private static IconScreen iconScreen;

  private static boolean enabled(){return "true".equalsIgnoreCase(System.getenv("CHAO_WEAR079_SMOKE"));}
  private static boolean motion(){return MODE.equals("motion");}
  private static boolean worldItems(){return MODE.equals("worlditems");}
  private static int stopAt(Case c){return 160+Math.round((c.toy()?160:ChaoIdleRules078.duration(c.clip()))*.65f);}
  private static List<MotionShot> motionShots(Case c){
    int duration=c.toy()?160:ChaoIdleRules078.duration(c.clip());var out=new ArrayList<MotionShot>();
    for(int i=0;i<5;i++)out.add(new MotionShot(Math.round(duration*new float[]{.22f,.36f,.50f,.68f,.83f}[i]),"active_"+VIEWS.get(i),i));
    if(c.loop())out.add(new MotionShot(160+Math.round(duration*.4f),"second_cycle",0));
    out.add(new MotionShot(c.loop()?stopAt(c)+20:176,"recovered_front",0));return List.copyOf(out);
  }
  private static int captureCount(Case c){return motion()?motionShots(c).size():5;}
  private static String currentView(){return motion()?VIEWS.get(motionShots(CASES.get(phase)).get(Math.min(viewIndex,captureCount(CASES.get(phase))-1)).view()):VIEWS.get(Math.min(viewIndex,4));}
  private static void require(boolean okay,String message){if(!okay)throw new IllegalStateException("WEAR079 "+message);}
  private static String sha(byte[] bytes){try{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));}catch(Exception e){throw new IllegalStateException(e);}}
  private static String sha(String s){return sha(s.getBytes(StandardCharsets.UTF_8));}
  private static String sha(Path p){try{return sha(Files.readAllBytes(p));}catch(Exception e){throw new IllegalStateException(e);}}
  private static void write(Path path,Object data){try{Files.createDirectories(path.getParent());Files.writeString(path,GSON.toJson(data)+"\n");}catch(Exception e){throw new IllegalStateException(e);}}
  private static void onServer(MinecraftServer server,Runnable task){server.execute(()->{try{task.run();}catch(Throwable e){fatal=e.toString();LogUtils.getLogger().error("WEAR079_ASYNC_FAILURE",e);}});}
  private static List<Wear> gear(int... indices){var out=new ArrayList<Wear>();for(int i:indices)out.add(WEAR.get(i));return List.copyOf(out);}
  private static List<Case> cases(){
    var all=new ArrayList<Case>();
    if(MODE.equals("motion")){
      String[] clips={"look_around","think","ground_play","crayons","toy_car"};
      for(int i=0;i<10;i++){int k=i%5;var items=switch(k){case 0->gear(12,17);case 1->gear(8,18);case 2->gear(15,22,17);case 3->gear(21,23,20);default->gear(11,24,18);};
        all.add(new Case(i,clips[k]+(i>=5?"_loop_stop":"_once"),FORMS.get(new int[]{0,12,16,2,10}[k]),items,clips[k],k>=3,i>=5));}
    }else if(MODE.equals("fit")){
      for(int i=0;i<15;i++)all.add(new Case(i,"mask_glasses_"+WEAR.get(i).id().replace(':','_'),FORMS.get(new int[]{0,12,17,1,4,10,2,8,15,3,9,16,6,11,14}[i]),gear(i,17),"",false,false));
    }else for(int i=0;i<WEAR.size();i++)all.add(new Case(i,WEAR.get(i).id().replace(':','_'),FORMS.get(i%FORMS.size()),gear(i),"",false,false));
    String selected=System.getenv("CHAO_WEAR079_CASES");if(selected==null||selected.isBlank())return List.copyOf(all);
    var out=new ArrayList<Case>();var used=new HashSet<Integer>();for(String token:selected.split(",")){String s=token.trim();int index=-1;try{index=Integer.parseInt(s);}catch(NumberFormatException ignored){for(int i=0;i<all.size();i++)if(all.get(i).name().equals(s)||all.get(i).gear().getFirst().id().equals(s))index=i;}
      require(index>=0&&index<all.size()&&used.add(index),"invalid/duplicate selected case "+s);out.add(all.get(index));}return List.copyOf(out);
  }
  private static ItemStack stack(Wear w){var key=ResourceLocation.parse(w.id());require(BuiltInRegistries.ITEM.containsKey(key),"unregistered real item "+w.id());return new ItemStack(BuiltInRegistries.ITEM.get(key));}
  private static ChaoState fixture(Case c){
    var s=new ChaoState();s.adult=c.form().startsWith("adult");s.align=c.form().contains("hero")?1:c.form().contains("dark")?-1:0;s.primaryType=s.adult?Integer.parseInt(c.form().substring(c.form().length()-1)):ChaoState.NORMAL;
    s.hunger=100;s.energy=100;s.happiness=20;s.boredom=0;s.setBaselineFace076(0,0);s.setEvolutionStrength(s.adult?GradualEvolutionRules.ADULT_STRENGTH_MAX*.65f:0);
    if(s.adult){s.train(ChaoState.RUN,1200,new Random(7901));s.train(ChaoState.POWER,1200,new Random(7902));if(c.sourceIndex()%3==1)s.train(ChaoState.FLY,720,new Random(7903));if(c.sourceIndex()%3==2)s.train(ChaoState.POWER,720,new Random(7904));}
    for(int i=0;i<ChaoIllness.COUNT;i++)s.setIllness(i,100);
    if(c.sourceIndex()%5==3||motion()&&c.sourceIndex()%5==2){s.shiny=true;s.shinyGenes=new int[]{1,1};}
    if(c.sourceIndex()%7==5||motion()&&c.sourceIndex()%5==1){int a=Arrays.asList(ChaoState.ANIMALS).indexOf("rabbit");require(a>=0,"rabbit missing");s.animalMask=s.animalPartsMask=1<<a;for(int i=0;i<s.animalParts.length;i++)if((ChaoState.animalSlots(a)&1<<i)!=0)s.animalParts[i]=a;}
    String encoded=s.encode();var copy=ChaoState.decode(encoded);require(encoded.equals(copy.encode()),"fixture codec mismatch");return copy;
  }
  private static String equipment(ChaoEntity c){var out=new StringBuilder();for(var slot:List.of(EquipmentSlot.HEAD,EquipmentSlot.FEET,EquipmentSlot.OFFHAND,EquipmentSlot.LEGS,EquipmentSlot.CHEST)){var worn=c.getItemBySlot(slot);out.append(slot).append('=').append(BuiltInRegistries.ITEM.getKey(worn.getItem())).append('*').append(worn.getCount()).append(':').append(worn.getComponents()).append(';');}return out.toString();}
  private static boolean hasGear(ChaoEntity c,Case entry){for(var w:entry.gear())if(!ItemStack.matches(stack(w),c.getItemBySlot(w.slot())))return false;return true;}
  private static void next(MinecraftServer server,int index){
    if(worldItems()){nextDropped(server,index);return;}
    server.tickRateManager().setFrozen(true);var level=server.overworld();actorId=null;if(actor!=null){ToyPreview.stop(actor);ToyContent.endPlaying(actor,false);actor.discard();}
    var c=CASES.get(index);actor=ChaoGarden.CHAO.get().create(level);require(actor!=null,"actor creation failed");
    actor.setUUID(UUID.nameUUIDFromBytes(("wear079-"+MODE+"-"+c.sourceIndex()).getBytes(StandardCharsets.UTF_8)));actor.setState(fixture(c));actor.setOriginEggCoat(fixture(c));actor.setNoAi(true);actor.deferBehavior(100000);actor.getRandom().setSeed(7900+c.sourceIndex());actor.setInvulnerable(true);
    actor.moveTo(.5,80,3.5,180,0);actor.yBodyRot=actor.yBodyRotO=actor.yHeadRot=actor.yHeadRotO=180;actor.setOnGround(true);
    for(var w:c.gear())actor.setItemSlot(w.slot(),stack(w));actor.goalSelector.removeAllGoals(g->true);actor.targetSelector.removeAllGoals(g->true);actor.getPersistentData().putLong("ChaoToyNextPlay",Long.MAX_VALUE);
    require(level.addFreshEntity(actor),"real level admission failed");spawned=lastTick=level.getGameTime();began=-1;elapsed=0;settled=complete=stopped=false;expectedState="";actorId=actor.getUUID();phase=index;
    var p=server.getPlayerList().getPlayers().getFirst();p.setGameMode(GameType.SPECTATOR);p.teleportTo(6.5,82,7.5);p.setNoGravity(true);p.setDeltaMovement(Vec3.ZERO);server.tickRateManager().setFrozen(false);
    LogUtils.getLogger().info("WEAR079_CASE phase={} mode={} index={} sourceIndex={} name={} form={} uuid={} realEquipment={} stateSha256={}",PHASE,MODE,index,c.sourceIndex(),c.name(),c.form(),actorId,equipment(actor),sha(actor.state().encode()));
  }
  private static void command(MinecraftServer server,String action){
    var c=CASES.get(phase);var p=server.getPlayerList().getPlayers().getFirst();p.teleportTo(actor.getX(),actor.getY()+2,actor.getZ());p.setDeltaMovement(Vec3.ZERO);actor.setOnGround(true);
    String text="chao "+(c.toy()?"testtoy":"testidle")+" "+c.clip()+" "+action;
    try{require(server.getCommands().getDispatcher().execute(text,p.createCommandSourceStack().withPermission(2))==1,"native dispatcher rejected "+text);}catch(com.mojang.brigadier.exceptions.CommandSyntaxException e){throw new IllegalStateException(e);}
    LogUtils.getLogger().info("WEAR079_COMMAND index={} command={} elapsed={}",phase,text,elapsed);
  }
  @SubscribeEvent public static void server(ServerTickEvent.Post e){
    if(!enabled()||MODE.equals("items")||worldItems()||phase<0||phase>=CASES.size()||complete)return;
    try{var server=e.getServer();long now=server.overworld().getGameTime();if(now==lastTick)return;lastTick=now;require(actor!=null&&actor.isAlive(),"actor disappeared");var c=CASES.get(phase);
      if(!settled){if(now-spawned<20)return;require(actor.onGround(),"20 physics ticks did not reach floor");actor.setState(fixture(c));actor.setDeltaMovement(Vec3.ZERO);require(hasGear(actor,c),"settling changed gear");expectedState=actor.state().encode();settled=true;began=now;
        if(motion()){command(server,c.loop()?"loop":"once");require(ToyPreview.active(actor),"native preview did not start");require(actor.toyPlayingKind().equals(c.toy()?"chao_"+c.clip():"preview_idle_"+c.clip()),"native preview token mismatch");}else {server.tickRateManager().setFrozen(true);complete=true;}
        LogUtils.getLogger().info("WEAR079_SETTLED index={} realPhysicsTicks=20 noAi={} noGravity={} state={} stateSha256={} realEquipment={}",phase,actor.isNoAi(),actor.isNoGravity(),expectedState,sha(expectedState),equipment(actor));return;}
      elapsed=now-began;
      if(motion()){require(hasGear(actor,c),"preview changed real gear");if(ToyPreview.active(actor))require(actor.state().encode().equals(expectedState),"preview changed saved state");if(c.loop()&&!stopped&&elapsed>=stopAt(c)){command(server,"stop");stopped=true;}
        int end=c.loop()?stopAt(c)+30:190;if(elapsed>=end){require(!ToyPreview.active(actor)&&actor.toyPlayingTicks()==0&&actor.toyTarget()==null,"preview cleanup missing");require(actor.isNoAi()&&!actor.isNoGravity(),"preview did not restore AI/gravity");complete=true;server.tickRateManager().setFrozen(true);LogUtils.getLogger().info("WEAR079_NATIVE_COMPLETE index={} elapsed={} loopStopped={} realGearPreserved=true restoredNoAi=true",phase,elapsed,stopped);}}
    }catch(Throwable failure){fatal=failure.toString();LogUtils.getLogger().error("WEAR079_ASYNC_FAILURE",failure);}
  }
  private static ChaoEntity observed(Minecraft mc){if(mc.level!=null&&actorId!=null)for(var e:mc.level.entitiesForRendering())if(e instanceof ChaoEntity c&&c.getUUID().equals(actorId))return c;return null;}
  private static ItemEntity observedDropped(Minecraft mc){if(mc.level!=null&&actorId!=null)for(var e:mc.level.entitiesForRendering())if(e instanceof ItemEntity item&&item.getUUID().equals(actorId))return item;return null;}
  private static void nextDropped(MinecraftServer server,int index){
    server.tickRateManager().setFrozen(true);var level=server.overworld();actorId=null;if(dropped!=null)dropped.discard();var c=CASES.get(index);
    dropped=new ItemEntity(level,.5,80.2,3.5,stack(c.gear().getFirst()),0,0,0);dropped.setUUID(UUID.nameUUIDFromBytes(("wear079-worlditems-"+c.sourceIndex()).getBytes(StandardCharsets.UTF_8)));dropped.setNoGravity(true);dropped.setNeverPickUp();dropped.setUnlimitedLifetime();dropped.setInvulnerable(true);dropped.setDeltaMovement(Vec3.ZERO);
    require(level.addFreshEntity(dropped),"real dropped ItemEntity admission failed");expectedState="actual dropped "+c.gear().getFirst().id();settled=complete=true;elapsed=0;actorId=dropped.getUUID();phase=index;
    var p=server.getPlayerList().getPlayers().getFirst();p.setGameMode(GameType.SPECTATOR);p.teleportTo(6.5,82,7.5);p.setNoGravity(true);p.setDeltaMovement(Vec3.ZERO);
    LogUtils.getLogger().info("WEAR079_WORLD_ITEM_CASE phase={} index={} sourceIndex={} item={} uuid={} actualDroppedEntity=true",PHASE,index,c.sourceIndex(),c.gear().getFirst().id(),actorId);
  }
  private static void camera(Minecraft mc){
    var c=observed(mc);var item=worldItems()?observedDropped(mc):null;if(worldItems()?item==null:c==null)return;if(observer==null||observer.level()!=mc.level){observer=new ArmorStand(mc.level,0,0,0);observer.setInvisible(true);observer.setNoGravity(true);}
    if(!worldItems()&&!motion()&&settled)c.tickCount=20;
    String view=currentView();Vec3 focus=worldItems()?item.position().add(0,.33,0):c.position().add(0,c.state().adult?.57:.43,0);Vec3 forward=Vec3.directionFromRotation(0,180),side=new Vec3(-forward.z,0,forward.x);
    double a=Math.toRadians(view.equals("left")?-90:view.equals("right")?90:view.equals("back")?180:0);Vec3 eye=worldItems()?(view.equals("above")?focus.add(forward.scale(.35)).add(0,1.20,0):focus.add(forward.scale(Math.cos(a)*1.25)).add(side.scale(Math.sin(a)*1.25)).add(0,.15,0)):(view.equals("above")?focus.add(forward.scale(.65)).add(0,2.35,0):focus.add(forward.scale(Math.cos(a)*2.4)).add(side.scale(Math.sin(a)*2.4)).add(0,.22,0));
    var d=focus.subtract(eye);float yaw=(float)(Math.toDegrees(Math.atan2(d.z,d.x))-90),pitch=(float)-Math.toDegrees(Math.atan2(d.y,Math.hypot(d.x,d.z)));
    observer.moveTo(eye.x,eye.y-observer.getEyeHeight(),eye.z,yaw,pitch);observer.xo=observer.xOld=observer.getX();observer.yo=observer.yOld=observer.getY();observer.zo=observer.zOld=observer.getZ();observer.yRotO=yaw;observer.xRotO=pitch;observer.yHeadRotO=yaw;observer.setYHeadRot(yaw);mc.options.setCameraType(CameraType.FIRST_PERSON);mc.setCameraEntity(observer);
  }
  @SubscribeEvent public static void restoreCamera(ClientTickEvent.Pre e){if(enabled()&&!MODE.equals("items")){var mc=Minecraft.getInstance();if(mc.player!=null&&mc.getCameraEntity()!=mc.player)mc.setCameraEntity(mc.player);}}
  @SubscribeEvent public static void frameBegin(RenderFrameEvent.Pre e){if(enabled())renderedFrame=worldFrame=false;}
  @SubscribeEvent public static void rendered(RenderLivingEvent.Post<?,?> e){
    if(!enabled()||MODE.equals("items")||phase<0||phase>=CASES.size()||!settled||SceneShaderPack.shadows()||!(e.getEntity()instanceof ChaoEntity c)||!c.getUUID().equals(actorId))return;var mc=Minecraft.getInstance();if(mc.screen!=null||mc.getOverlay()!=null||mc.getCameraEntity()!=observer)return;
    var entry=CASES.get(phase);if(!hasGear(c,entry)||!motion()&&!c.state().encode().equals(expectedState))return;
    var model=(ChaoModel)e.getRenderer().getModel();require(model.officialActive()&&model.officialPosesFinite(),"finite production renderer missing");require(!c.officialComparison(),"comparison projection is forbidden");require(c.state().adult==entry.form().startsWith("adult")&&!c.isCocooned(),"life stage changed");
    observedState=c.state().encode();observedEquipment=equipment(c);observedGeometry=model.gameplayFormKey();observedMotion=String.valueOf(model.officialMotion());observedTick=motion()?elapsed:20;
    var proof=new JsonObject();proof.addProperty("actual_living_entity",true);proof.addProperty("actual_production_model",true);proof.addProperty("finite_poses",true);proof.addProperty("actual_gear_verified",true);proof.addProperty("renderer",e.getRenderer().getClass().getName());proof.addProperty("geometry",observedGeometry);proof.addProperty("official_geometry",model.officialGeometryKey());proof.addProperty("hat_key",c.getChaoHat());proof.addProperty("motion",observedMotion);proof.addProperty("shaders_active",SceneShaderPack.active());proof.addProperty("uuid",c.getUUID().toString());proof.addProperty("state_sha256",sha(observedState));proof.addProperty("equipment",observedEquipment);proof.addProperty("equipment_sha256",sha(observedEquipment));proof.add("skull_bounds",GSON.toJsonTree(model.accessoryBounds("skull")));proof.add("body_bounds",GSON.toJsonTree(model.accessoryBounds("body")));observedProof=proof;
    var stableGear=new JsonObject();for(var slot:List.of(EquipmentSlot.HEAD,EquipmentSlot.FEET,EquipmentSlot.OFFHAND,EquipmentSlot.LEGS,EquipmentSlot.CHEST)){var worn=c.getItemBySlot(slot);stableGear.addProperty(slot.name(),worn.isEmpty()?"empty":worn.save(c.registryAccess()).toString());}proof.add("stable_equipment_nbt",stableGear);proof.addProperty("stable_equipment_sha256",sha(GSON.toJson(stableGear)));
    var coat=new JsonObject();coat.addProperty("expressed_color",c.state().expressedColor);coat.addProperty("expressed_jewel",c.state().expressedJewel);coat.addProperty("two_tone",c.state().twoTone);coat.addProperty("shiny",c.state().shiny);coat.addProperty("negative_palette",c.state().negativePalette);coat.addProperty("alignment",c.state().lockedAlignment());coat.addProperty("first_type",c.state().lockedPrimaryType());coat.addProperty("adult",c.state().adult);coat.addProperty("animal_parts_mask",c.state().animalPartsMask);proof.add("observed_phenotype",coat);
    if(motion()){String id=ChaoIdleAnimation078.id(c);proof.addProperty("preview_kind",c.toyPlayingKind());proof.addProperty("preview_remaining",c.toyPlayingTicks());proof.addProperty("idle_id",id);proof.addProperty("idle_elapsed",ChaoIdleAnimation078.elapsed(c,e.getPartialTick()));proof.addProperty("idle_weight",ChaoIdleRules078.weight(id,ChaoIdleAnimation078.elapsed(c,e.getPartialTick())));}
    renderedFrame=true;actorFrames++;
  }
  /** Vanilla ItemEntityRenderer invokes the base name-tag event after submitting item geometry. */
  @SubscribeEvent public static void droppedRendered(RenderNameTagEvent e){
    if(!enabled()||!worldItems()||phase<0||phase>=CASES.size()||SceneShaderPack.shadows()||!(e.getEntity()instanceof ItemEntity item)||!item.getUUID().equals(actorId))return;
    var mc=Minecraft.getInstance();if(mc.screen!=null||mc.getOverlay()!=null||mc.getCameraEntity()!=observer)return;var c=CASES.get(phase);require(ItemStack.matches(stack(c.gear().getFirst()),item.getItem()),"actual dropped item stack changed");require(e.getEntityRenderer() instanceof net.minecraft.client.renderer.entity.ItemEntityRenderer,"actual production ItemEntityRenderer missing");e.setCanRender(net.neoforged.neoforge.common.util.TriState.FALSE);
    var model=mc.getItemRenderer().getModel(item.getItem(),mc.level,null,item.getId());var ground=model.applyTransform(ItemDisplayContext.GROUND,new com.mojang.blaze3d.vertex.PoseStack(),false);var gui=model.applyTransform(ItemDisplayContext.GUI,new com.mojang.blaze3d.vertex.PoseStack(),false);
    var proof=new JsonObject();proof.addProperty("actual_dropped_item_entity",true);proof.addProperty("actual_item_geometry_render_observed",true);proof.addProperty("actual_gear_verified",true);proof.addProperty("renderer",e.getEntityRenderer().getClass().getName());proof.addProperty("uuid",item.getUUID().toString());proof.addProperty("registry_item",c.gear().getFirst().id());proof.addProperty("display_context","GROUND");proof.addProperty("baked_model_class",model.getClass().getName());proof.addProperty("ground_model_class",ground.getClass().getName());proof.addProperty("gui_model_class",gui.getClass().getName());proof.addProperty("ground_and_gui_same_model",ground==gui);proof.addProperty("ground_quads",ground.getQuads(null,null,net.minecraft.util.RandomSource.create(79),net.neoforged.neoforge.client.model.data.ModelData.EMPTY,null).size());proof.addProperty("gui_quads",gui.getQuads(null,null,net.minecraft.util.RandomSource.create(79),net.neoforged.neoforge.client.model.data.ModelData.EMPTY,null).size());proof.addProperty("is_gui_3d",ground.isGui3d());proof.addProperty("item_age",item.getAge());proof.addProperty("item_bob_offset",item.bobOffs);proof.addProperty("shaders_active",SceneShaderPack.active());proof.addProperty("state_sha256",sha(expectedState));proof.addProperty("equipment_sha256",sha(c.gear().getFirst().id()+"*1"));
    observedProof=proof;observedState=expectedState;observedEquipment=c.gear().getFirst().id()+"*1";observedTick=item.getAge();renderedFrame=true;actorFrames++;
  }
  @SubscribeEvent public static void world(RenderLevelStageEvent e){if(enabled()&&e.getStage()==RenderLevelStageEvent.Stage.AFTER_LEVEL&&renderedFrame&&!SceneShaderPack.shadows()){var mc=Minecraft.getInstance();worldFrame=mc.screen==null&&mc.getOverlay()==null&&mc.getCameraEntity()==observer;}}
  private static void capture(Minecraft mc){
    int index=phase,vi=viewIndex;var c=CASES.get(index);String view=currentView();String label=motion()?motionShots(c).get(vi).label():view;String name=String.format("wear079-%02d-%s-%s.png",c.sourceIndex(),c.name(),label);Path file=OUTPUT.resolve("screenshots").resolve(name);
    var proof=observedProof.deepCopy();proof.addProperty("phase",PHASE);proof.addProperty("mode",MODE);proof.addProperty("source_index",c.sourceIndex());proof.addProperty("name",c.name());proof.addProperty("form",c.form());proof.addProperty("view",view);proof.addProperty("display_tick",observedTick);proof.addProperty("fov",40);proof.addProperty("distance",view.equals("above")?Math.hypot(.65,2.35):Math.hypot(2.4,.22));proof.addProperty("state",observedState);proof.addProperty("saved_state_sha256",sha(expectedState));
    if(worldItems())proof.addProperty("distance",view.equals("above")?Math.hypot(.35,1.20):Math.hypot(1.25,.15));
    if(motion()){proof.addProperty("moment",label);proof.addProperty("scheduled_tick",motionShots(c).get(vi).tick());if(label.equals("recovered_front")){require(proof.get("preview_remaining").getAsInt()==0&&proof.get("idle_weight").getAsFloat()==0,"rendered recovery still contains preview/idle motion");}else {String expected=c.toy()?"chao_"+c.clip():"preview_idle_"+c.clip();require(proof.get("preview_kind").getAsString().equals(expected)&&proof.get("preview_remaining").getAsInt()>0,"rendered active native preview token absent");if(!c.toy())require(proof.get("idle_id").getAsString().equals(c.clip())&&proof.get("idle_weight").getAsFloat()>.5f,"rendered authored idle action absent");}}
    try{Files.createDirectories(file.getParent());}catch(Exception ex){throw new IllegalStateException(ex);}
    Screenshot.grab(OUTPUT.toFile(),name,mc.getMainRenderTarget(),message->{require(Files.isRegularFile(file),"PNG did not save");proof.addProperty("png",file.toString());proof.addProperty("png_sha256",sha(file));write(OUTPUT.resolve("frames").resolve(name.replace(".png",".json")),proof);SAVED.incrementAndGet();LogUtils.getLogger().info("WEAR079_CAPTURE phase={} mode={} index={} name={} view={} tick={} stateSha256={} gearSha256={} sha256={}",PHASE,MODE,index,name,view,proof.get("display_tick"),proof.get("state_sha256"),proof.get("equipment_sha256"),proof.get("png_sha256"));});shots++;viewIndex++;viewFrames=0;
  }
  @SubscribeEvent public static void frameEnd(RenderFrameEvent.Post e){
    if(!enabled()||MODE.equals("items")||phase<0||phase>=CASES.size()||clientPhase!=phase||!worldFrame)return;worldFrames++;viewFrames++;var mc=Minecraft.getInstance();
    if(viewIndex<captureCount(CASES.get(phase))){int threshold=motion()?motionShots(CASES.get(phase)).get(viewIndex).tick():0;if(settled&&elapsed>=threshold&&viewFrames>=3)capture(mc);return;}
    if(!complete)return;require(actorFrames>=15&&worldFrames>=15,"real five-view render coverage missing");var c=CASES.get(phase);var doneProof=new JsonObject();doneProof.addProperty("phase",PHASE);doneProof.addProperty("mode",MODE);doneProof.addProperty("source_index",c.sourceIndex());doneProof.addProperty("name",c.name());doneProof.addProperty("views",5);doneProof.addProperty("captures",captureCount(c));doneProof.addProperty("actor_frames",actorFrames);doneProof.addProperty("world_frames",worldFrames);doneProof.addProperty("state_sha256",sha(expectedState));doneProof.addProperty("actual_entity_equipment",true);write(OUTPUT.resolve("complete").resolve(String.format("%02d-%s.json",c.sourceIndex(),c.name())),doneProof);completedCases++;
    LogUtils.getLogger().info("WEAR079_RENDER_COMPLETE phase={} mode={} index={} sourceIndex={} name={} views=5 actorFrames={} worldFrames={} actualWorldActor=true",PHASE,MODE,phase,c.sourceIndex(),c.name(),actorFrames,worldFrames);
    int done=phase;pending=phase;clientPhase=-2;onServer(mc.getSingleplayerServer(),()->{if(phase==done){if(phase+1<CASES.size())next(mc.getSingleplayerServer(),phase+1);else phase=CASES.size();}});
  }
  private static void catalog(){
    require(Set.of("catalog","motion","items","worlditems","fit").contains(MODE),"unsupported mode "+MODE);require(Set.of("before","after").contains(PHASE),"unsupported phase "+PHASE);
    var root=new JsonObject();root.addProperty("phase",PHASE);root.addProperty("mode",MODE);root.addProperty("canonical_wearable_count",25);root.addProperty("accepted_identity_count_including_legacy_alias",26);root.addProperty("hidden_legacy_alias","chao_garden:watermelon_hat -> minecraft:melon");root.addProperty("source_inventory","/workspace/.setup/wear079-inventory/inventory.json");root.addProperty("source_csv","/workspace/.setup/wear079-inventory/wearables.csv");
    for(String name:List.of("inventory.json","wearables.csv")){Path file=Path.of("/workspace/.setup/wear079-inventory",name);if(Files.isRegularFile(file))root.addProperty(name+"_sha256",sha(file));}
    root.addProperty("render_path","actual synchronized ChaoEntity + actual EquipmentSlots + production renderer layers");root.addProperty("static_protocol","20 real physics ticks, NoAI retained, freeze; fixed UUID/state/seed/display tick20; five cameras");root.addProperty("fov",40);root.add("views",GSON.toJsonTree(VIEWS));var rows=new JsonArray();
    if(worldItems()){root.addProperty("render_path","actual ItemEntity + vanilla ItemEntityRenderer + production GROUND item model, observed after item geometry submission");root.addProperty("static_protocol","frozen actual dropped ItemEntity, no gravity/pickup; five cameras, after-only 3D base-model review");root.addProperty("matched_baseline_available",false);}
    root.addProperty("birthday_party_hat_actual_tested",false);root.addProperty("npc_merchant_actual_tested",false);root.addProperty("medal_tiers_actual_tested",false);
    for(var c:CASES){var row=new JsonObject();row.addProperty("source_index",c.sourceIndex());row.addProperty("name",c.name());row.addProperty("form",c.form());row.addProperty("uuid",UUID.nameUUIDFromBytes(("wear079-"+MODE+"-"+c.sourceIndex()).getBytes(StandardCharsets.UTF_8)).toString());row.add("real_equipment",GSON.toJsonTree(c.gear()));row.addProperty("fixture_state",fixture(c).encode());row.addProperty("fixture_sha256",sha(fixture(c).encode()));if(motion())row.addProperty("command","chao "+(c.toy()?"testtoy":"testidle")+" "+c.clip()+" "+(c.loop()?"loop":"once"));rows.add(row);}root.add("cases",rows);write(OUTPUT.resolve("case-catalog.json"),root);
  }
  private static final class IconScreen extends Screen {
    IconScreen(){super(Component.literal("Wear079 actual inventory item models"));}
    @Override public void render(GuiGraphics g,int x,int y,float partial){
      g.fill(0,0,width,height,0xff1c2531);g.drawCenteredString(font,"ACTUAL INVENTORY ITEM MODELS · 25 CANONICAL WEARABLES",width/2,8,0xffeef3fa);
      int cols=5,cellW=width/cols,cellH=(height-26)/5;
      for(int i=0;i<WEAR.size();i++){int cx=(i%cols)*cellW,cy=24+(i/cols)*cellH;g.fill(cx+2,cy+2,cx+cellW-2,cy+cellH-2,0xff2a3748);var w=WEAR.get(i);var item=stack(w);g.pose().pushPose();g.pose().translate(cx+cellW/2f-24,cy+5,0);g.pose().scale(3,3,1);g.renderItem(item,0,0);g.pose().popPose();String label=w.id().substring(w.id().indexOf(':')+1).replace("_accessory","");g.drawCenteredString(font,label,cx+cellW/2,cy+cellH-20,0xffedf5ff);g.drawCenteredString(font,w.slot().name(),cx+cellW/2,cy+cellH-10,0xffa6bdd8);}
    }
  }
  @SubscribeEvent public static void itemRendered(ScreenEvent.Render.Post e){if(enabled()&&MODE.equals("items")&&e.getScreen()==iconScreen)iconFrames++;}
  private static void items(Minecraft mc){
    if(iconScreen==null){mc.options.hideGui=false;mc.options.guiScale().set(2);iconScreen=new IconScreen();mc.setScreen(iconScreen);return;}
    if(iconFrames<12||shots>0)return;String name="wear079-items-grid.png";Path file=OUTPUT.resolve("screenshots").resolve(name);try{Files.createDirectories(file.getParent());}catch(Exception e){throw new IllegalStateException(e);}
    Screenshot.grab(OUTPUT.toFile(),name,mc.getMainRenderTarget(),message->{require(Files.isRegularFile(file),"item grid PNG did not save");var proof=new JsonObject();proof.addProperty("phase",PHASE);proof.addProperty("actual_gui_renderItem",true);proof.addProperty("actual_item_models",25);proof.addProperty("rendered_frames",iconFrames);proof.addProperty("png_sha256",sha(file));proof.add("registry_items",GSON.toJsonTree(WEAR));write(OUTPUT.resolve("items-complete.json"),proof);SAVED.incrementAndGet();LogUtils.getLogger().info("WEAR079_ITEMS_COMPLETE phase={} actualGuiRenderItem=true canonicalItems=25 sha256={}",PHASE,sha(file));});shots++;
  }
  @SubscribeEvent public static void client(ClientTickEvent.Post e){
    if(!enabled()||finished)return;var mc=Minecraft.getInstance();if(fatal!=null)throw new IllegalStateException("WEAR079 server failure: "+fatal);if(wallStart==0)wallStart=System.nanoTime();require(System.nanoTime()-wallStart<2_400_000_000_000L,"study wall deadline");
    if(mc.screen instanceof AccessibilityOnboardingScreen)mc.setScreen(new TitleScreen());
    if(!started&&mc.screen instanceof TitleScreen){started=true;catalog();mc.options.renderDistance().set(3);mc.options.simulationDistance().set(5);mc.options.framerateLimit().set(45);mc.options.fov().set(40);mc.options.pauseOnLostFocus=false;mc.options.hideGui=true;mc.options.tutorialStep=net.minecraft.client.tutorial.TutorialSteps.NONE;mc.getWindow().setWindowed(1440,1000);mc.resizeDisplay();
      if(!MODE.equals("items"))mc.createWorldOpenFlows().createFreshLevel("Wear079-"+PHASE+"-"+System.currentTimeMillis(),new LevelSettings("Wear079",GameType.CREATIVE,false,Difficulty.PEACEFUL,true,new GameRules(),WorldDataConfiguration.DEFAULT),new WorldOptions(2579L,false,false),r->r.registryOrThrow(Registries.WORLD_PRESET).getHolderOrThrow(WorldPresets.FLAT).value().createWorldDimensions(),mc.screen);
    }
    if(!started)return;
    if(MODE.equals("items")){items(mc);if(shots==1&&SAVED.get()==1){finished=true;mc.stop();}return;}
    if(!setup&&mc.player!=null&&mc.getSingleplayerServer()!=null){setup=true;mc.setScreen(null);ChaoRenderer.suppressNames=true;onServer(mc.getSingleplayerServer(),()->{var server=mc.getSingleplayerServer();var level=server.overworld();level.setDayTime(6000);level.setWeatherParameters(0,100000,false,false);level.getGameRules().getRule(GameRules.RULE_DAYLIGHT).set(false,server);level.getGameRules().getRule(GameRules.RULE_WEATHER_CYCLE).set(false,server);level.getGameRules().getRule(GameRules.RULE_DOMOBSPAWNING).set(false,server);for(var p:BlockPos.betweenClosed(-14,79,-12,14,86,16))level.setBlock(p,p.getY()==79?Blocks.SMOOTH_STONE.defaultBlockState():Blocks.AIR.defaultBlockState(),3);next(server,0);});}
    if(phase>=0&&phase<CASES.size()&&mc.level!=null){if(clientPhase==-2&&phase==pending)return;if(clientPhase!=phase){clientPhase=phase;viewIndex=viewFrames=actorFrames=worldFrames=0;observedProof=null;}camera(mc);}
    if(phase==CASES.size()&&SAVED.get()==shots){int requested=CASES.stream().mapToInt(WearablesVisualSmoke079::captureCount).sum();require(completedCases==CASES.size()&&shots==requested,"missing case or capture");finished=true;var proof=new JsonObject();proof.addProperty("phase",PHASE);proof.addProperty("mode",MODE);proof.addProperty("completed_cases",completedCases);proof.addProperty("saved_pngs",SAVED.get());proof.addProperty("actual_world_entity_equipment",true);write(OUTPUT.resolve("visual-complete.json"),proof);onServer(mc.getSingleplayerServer(),()->mc.getSingleplayerServer().tickRateManager().setFrozen(false));LogUtils.getLogger().info("WEAR079_VISUAL_COMPLETE phase={} mode={} cases={} captures={} saved={} actualWorldActor=true output={}",PHASE,MODE,completedCases,shots,SAVED.get(),OUTPUT);mc.stop();}
  }
}
