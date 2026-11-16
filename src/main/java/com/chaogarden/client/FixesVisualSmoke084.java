package com.chaogarden.client;

import com.chaogarden.ChaoGarden;
import com.chaogarden.GardenConfig;
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
import net.minecraft.client.gui.screens.AccessibilityOnboardingScreen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.decoration.ArmorStand;
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
import net.neoforged.neoforge.client.model.IQuadTransformer;
import net.neoforged.neoforge.client.model.data.ModelData;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/** Excluded disposable native study. Real block/Chao renderers and existing native
 * preview; no alternate geometry, poses, UI projection or gameplay rewards. */
@EventBusSubscriber(modid=ChaoGarden.MODID,value=Dist.CLIENT)
public final class FixesVisualSmoke084 {
  private record Case(int id,String name,String kind,int alignment,float maturity,boolean trained,boolean gear,boolean shiny) {
    boolean actor(){return kind.equals("power")||preview();}
    boolean preview(){return kind.equals("tambourine_preview");}
    String toy(){return kind.equals("duck")?"chao_rubber_duck":"chao_tambourine";}
  }
  private record Shot(int tick,String label,String view){}
  private static final Gson G=new GsonBuilder().setPrettyPrinting().create();
  private static final Path OUT=Path.of(System.getenv().getOrDefault("CHAO_FIXES084_OUTPUT","/workspace/.setup/fixes084-unconfigured")).toAbsolutePath().normalize();
  private static final String FREEZE=System.getenv().getOrDefault("CHAO_FIXES084_SOURCE_FREEZE","");
  private static final List<Case> CASES=cases();
  private static final BlockPos TOY=new BlockPos(0,80,3);
  private static final AtomicInteger SAVED=new AtomicInteger();
  private static final Map<String,JsonObject> BAKED=new HashMap<>();
  private static volatile int phase=-1;
  private static volatile UUID actorId;
  private static volatile long elapsed;
  private static volatile boolean settled,complete;
  private static volatile String fatal,expectedState="",expectedGear="",nativeCompleteState="";
  private static ChaoEntity actor;
  private static ArmorStand camera;
  private static long spawned,lastTick,began,wallStart;
  private static boolean started,setup,finished,renderedFrame,worldFrame;
  private static int clientPhase=-1,pending=-1,shotIndex,viewFrames,actorFrames,worldFrames,shots,completed;
  private static JsonObject observed;

  private static boolean enabled(){return "true".equalsIgnoreCase(System.getenv("CHAO_FIXES084_SMOKE"));}
  private static void require(boolean okay,String why){if(!okay)throw new IllegalStateException("FIXES084 "+why);}
  private static String sha(byte[] b){try{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(b));}catch(Exception e){throw new IllegalStateException(e);}}
  private static String sha(String text){return sha(text.getBytes(StandardCharsets.UTF_8));}
  private static String sha(Path p){try{return sha(Files.readAllBytes(p));}catch(Exception e){throw new IllegalStateException(e);}}
  private static void write(Path p,Object value){try{Files.createDirectories(p.getParent());Files.writeString(p,G.toJson(value)+"\n");}catch(Exception e){throw new IllegalStateException(e);}}
  private static void onServer(MinecraftServer server,Runnable work){server.execute(()->{try{work.run();}catch(Throwable e){fatal=e.toString();write(OUT.resolve("failure.json"),Map.of("phase",phase,"error",fatal));LogUtils.getLogger().error("FIXES084_ASYNC_FAILURE",e);}});}
  private static List<Case> cases(){
    var all=new ArrayList<Case>();all.add(new Case(0,"duck_two_eyes","duck",0,0,false,false,false));
    all.add(new Case(1,"tambourine_ground","tambourine_ground",0,0,false,false,false));
    all.add(new Case(2,"tambourine_native_once","tambourine_preview",0,.5f,false,false,false));
    int id=3;
    for(int alignment:new int[]{0,1,-1})for(int stage=0;stage<4;stage++){
      String align=alignment==0?"neutral":alignment>0?"hero":"dark",age=stage==0?"young":stage==1?"mid":stage==2?"late":"late_untrained";
      all.add(new Case(id++,"baby_"+align+"_power_"+age,"power",alignment,stage==0?0:stage==1?.5f:.99f,stage!=3,alignment==1&&stage==1,alignment==-1&&stage==2));
    }
    String select=System.getenv("CHAO_FIXES084_CASES");if(select==null||select.isBlank())return List.copyOf(all);
    var chosen=new ArrayList<Case>();var unique=new HashSet<Integer>();for(String token:select.split(",")){int index=Integer.parseInt(token.trim());require(index>=0&&index<all.size()&&unique.add(index),"invalid/duplicate selected case "+token);chosen.add(all.get(index));}return List.copyOf(chosen);
  }
  private static List<Shot> schedule(Case c){
    if(c.preview())return List.of(new Shot(45,"active_front","front"),new Shot(70,"active_right","right"),new Shot(95,"active_back","back"),new Shot(176,"recovered_front","front"));
    var result=new ArrayList<Shot>();for(String view:c.actor()?List.of("front","right","back"):List.of("front","right","back","left","above"))result.add(new Shot(0,"static_"+view,view));return List.copyOf(result);
  }
  private static ChaoState fixture(Case c,int childTicks){
    var s=new ChaoState();s.hunger=100;s.energy=100;s.happiness=20;s.boredom=0;s.setBaselineFace076(0,0);for(int n=0;n<ChaoIllness.COUNT;n++)s.setIllness(n,100);
    if(c.preview()){s.adult=true;s.primaryType=ChaoState.NORMAL;s.levels[ChaoState.RUN]=15;s.setEvolutionStrength(.6f);}
    else{
      require(CreativeChaoEditRules056.apply(s,c.alignment()*100,ChaoState.POWER,c.trained()?99:0),"production testing edit rejected fixture");
      s.ageTicks=Math.round(childTicks*c.maturity());s.setEvolutionStrength(c.maturity());s.shiny=c.shiny();if(c.shiny())s.shinyGenes=new int[]{1,1};
    }
    String encoded=s.encode();var copy=ChaoState.decode(encoded);require(encoded.equals(copy.encode()),"fixture state codec mismatch");return copy;
  }
  private static JsonObject gear(ChaoEntity c){
    var result=new JsonObject();for(var slot:List.of(EquipmentSlot.HEAD,EquipmentSlot.FEET,EquipmentSlot.OFFHAND,EquipmentSlot.LEGS,EquipmentSlot.CHEST)){
      var item=c.getItemBySlot(slot);result.addProperty(slot.name(),item.isEmpty()?"empty":item.save(c.registryAccess()).toString());}return result;
  }
  private static void next(MinecraftServer server,int index){
    server.tickRateManager().setFrozen(true);var level=server.overworld();if(actor!=null){ToyPreview.stop(actor);actor.discard();actor=null;}actorId=null;level.setBlock(TOY,Blocks.AIR.defaultBlockState(),3);
    var c=CASES.get(index);settled=complete=false;expectedState=expectedGear=nativeCompleteState="";phase=index;elapsed=0;
    if(c.actor()){
      actor=ChaoGarden.CHAO.get().create(level);require(actor!=null,"actual Chao creation failed");actor.setUUID(UUID.nameUUIDFromBytes(("fixes084-"+c.id()).getBytes(StandardCharsets.UTF_8)));actor.setState(fixture(c,GardenConfig.CHILD_TICKS.get()));actor.setNoAi(true);actor.deferBehavior(100000);actor.setInvulnerable(true);actor.getRandom().setSeed(84000+c.id());actor.moveTo(.5,80,3.5,180,0);actor.yBodyRot=actor.yBodyRotO=actor.yHeadRot=actor.yHeadRotO=180;actor.setOnGround(true);
      if(c.gear()){actor.setItemSlot(EquipmentSlot.OFFHAND,new ItemStack(ChaoGarden.item("round_glasses_accessory")));actor.setItemSlot(EquipmentSlot.LEGS,new ItemStack(ChaoGarden.item("bowtie_accessory")));}
      require(level.addFreshEntity(actor),"actual Chao admission failed");actorId=actor.getUUID();
    }else require(level.setBlock(TOY,ToyContent.block(c.toy()).defaultBlockState().setValue(ChaoToyBlock.FACING,Direction.NORTH).setValue(ChaoToyBlock.PLAYING,false).setValue(ChaoToyBlock.FRAME,0).setValue(ChaoToyBlock.WATERLOGGED,false),3),"actual placed toy failed");
    var p=server.getPlayerList().getPlayers().getFirst();p.setGameMode(GameType.SPECTATOR);p.teleportTo(6.5,82,7.5);p.setDeltaMovement(Vec3.ZERO);spawned=lastTick=level.getGameTime();began=-1;server.tickRateManager().setFrozen(false);
    LogUtils.getLogger().info("FIXES084_CASE index={} sourceIndex={} name={} kind={} uuid={}",index,c.id(),c.name(),c.kind(),actorId);
  }
  @SubscribeEvent public static void server(ServerTickEvent.Post e){
    if(!enabled()||phase<0||phase>=CASES.size()||complete)return;
    try{
      var server=e.getServer();long now=server.overworld().getGameTime();if(now==lastTick)return;lastTick=now;var c=CASES.get(phase);
      if(!settled){
        if(now-spawned<20)return;var baseline=new JsonObject();baseline.addProperty("source_index",c.id());baseline.addProperty("name",c.name());baseline.addProperty("source_freeze",FREEZE);
        if(c.actor()){
          require(actor!=null&&actor.isAlive()&&actor.onGround(),"actual actor did not settle on ground");actor.setState(fixture(c,GardenConfig.CHILD_TICKS.get()));actor.setDeltaMovement(Vec3.ZERO);actor.setYRot(180);actor.yRotO=actor.yBodyRot=actor.yBodyRotO=actor.yHeadRot=actor.yHeadRotO=180;
          expectedState=actor.state().encode();expectedGear=G.toJson(gear(actor));baseline.addProperty("state",expectedState);baseline.addProperty("state_sha256",sha(expectedState));baseline.add("equipment",gear(actor));baseline.addProperty("equipment_sha256",sha(expectedGear));baseline.addProperty("child_ticks",GardenConfig.CHILD_TICKS.get());
          if(c.id()==5){
            UUID beforeId=actor.getUUID();actor.setOfficialComparison(true);actor.setSourceReferenceKey("power");
            require(actor.officialComparison()&&actor.sourceReferenceKey().equals("power"),"refresh fixture override missing");
            var p=server.getPlayerList().getPlayers().getFirst();p.teleportTo(actor.getX(),actor.getY()+2,actor.getZ());
            int result=server.getCommands().getDispatcher().execute("chao refresh ~",p.createCommandSourceStack().withPermission(2));
            require(result==1&&!actor.officialComparison()&&actor.sourceReferenceKey().isEmpty(),"native refresh did not clear reference overrides");
            require(actor.getUUID().equals(beforeId)&&actor.state().encode().equals(expectedState)&&G.toJson(gear(actor)).equals(expectedGear),"native refresh changed creature/state/equipment");
            var refresh=new JsonObject();refresh.addProperty("native_command","chao refresh ~");refresh.addProperty("operator_permission",2);refresh.addProperty("dispatcher_result",result);refresh.addProperty("before_official_comparison",true);refresh.addProperty("before_source_reference","power");refresh.addProperty("after_official_comparison",actor.officialComparison());refresh.addProperty("after_source_reference",actor.sourceReferenceKey());refresh.addProperty("identity_state_equipment_preserved",true);refresh.addProperty("uuid",beforeId.toString());refresh.addProperty("state_sha256",sha(expectedState));refresh.addProperty("equipment_sha256",sha(expectedGear));baseline.add("native_refresh_proof",refresh);
            LogUtils.getLogger().info("FIXES084_REFRESH index={} sourceIndex={} command=chao refresh ~ result={} officialAfter=false sourceAfter=empty stateGearUuidPreserved=true",phase,c.id(),result);
          }
          if(c.preview()){
            var p=server.getPlayerList().getPlayers().getFirst();p.teleportTo(actor.getX(),actor.getY()+2,actor.getZ());
            require(server.getCommands().getDispatcher().execute("chao testtoy tambourine once",p.createCommandSourceStack().withPermission(2))==1,"native tambourine command rejected");require(ToyPreview.active(actor),"native preview not active");
            LogUtils.getLogger().info("FIXES084_COMMAND index={} command=chao testtoy tambourine once nativeDispatcher=true",phase);
          }
        }
        write(OUT.resolve("case-start").resolve(String.format("%02d-%s.json",c.id(),c.name())),baseline);settled=true;began=now;if(!c.preview()){complete=true;server.tickRateManager().setFrozen(true);}return;
      }
      elapsed=now-began;
      if(c.preview()){
        require(actor!=null&&actor.isAlive()&&G.toJson(gear(actor)).equals(expectedGear),"native preview lost actor/equipment");if(ToyPreview.active(actor))require(actor.state().encode().equals(expectedState),"native preview changed saved state");
        if(elapsed>=190){require(!ToyPreview.active(actor)&&actor.toyPlayingTicks()==0&&actor.toyTarget()==null,"native preview cleanup missing");require(actor.isNoAi()&&!actor.isNoGravity(),"native preview did not restore AI/gravity");nativeCompleteState=actor.state().encode();complete=true;server.tickRateManager().setFrozen(true);LogUtils.getLogger().info("FIXES084_NATIVE_COMPLETE index={} elapsed={} gearPreserved=true restoredNoAi=true",phase,elapsed);}
      }
    }catch(Throwable error){fatal=error.toString();write(OUT.resolve("failure.json"),Map.of("phase",phase,"error",fatal));LogUtils.getLogger().error("FIXES084_ASYNC_FAILURE",error);}
  }
  private static ChaoEntity observedActor(Minecraft mc){if(mc.level!=null&&actorId!=null)for(var entity:mc.level.entitiesForRendering())if(entity instanceof ChaoEntity c&&c.getUUID().equals(actorId))return c;return null;}
  private static void camera(Minecraft mc){
    var c=CASES.get(phase);var entity=c.actor()?observedActor(mc):null;if(c.actor()&&entity==null)return;if(camera==null||camera.level()!=mc.level){camera=new ArmorStand(mc.level,0,0,0);camera.setInvisible(true);camera.setNoGravity(true);}
    if(entity!=null&&!c.preview())entity.tickCount=60;var shot=schedule(c).get(Math.min(shotIndex,schedule(c).size()-1));String view=shot.view();
    Vec3 focus=entity!=null?entity.position().add(0,entity.state().adult?.57:.43,0):Vec3.atLowerCornerOf(TOY).add(.5,c.kind().equals("duck")?.35:.14,.5);
    Vec3 forward=c.kind().equals("duck")?new Vec3(1,0,0):Vec3.directionFromRotation(0,180),side=new Vec3(-forward.z,0,forward.x);
    double distance=entity!=null?2.0:1.55,angle=Math.toRadians(view.equals("right")?90:view.equals("left")?-90:view.equals("back")?180:0);
    Vec3 eye=view.equals("above")?focus.add(forward.scale(.35)).add(0,distance,0):focus.add(forward.scale(Math.cos(angle)*distance)).add(side.scale(Math.sin(angle)*distance)).add(0,.18,0);
    var d=focus.subtract(eye);float yaw=(float)(Math.toDegrees(Math.atan2(d.z,d.x))-90),pitch=(float)-Math.toDegrees(Math.atan2(d.y,Math.hypot(d.x,d.z)));
    camera.moveTo(eye.x,eye.y-camera.getEyeHeight(),eye.z,yaw,pitch);camera.xo=camera.xOld=camera.getX();camera.yo=camera.yOld=camera.getY();camera.zo=camera.zOld=camera.getZ();camera.yRotO=yaw;camera.xRotO=pitch;camera.yHeadRotO=yaw;camera.setYHeadRot(yaw);mc.options.setCameraType(CameraType.FIRST_PERSON);mc.setCameraEntity(camera);
  }
  private static boolean admitted(Minecraft mc){return mc.screen==null&&mc.getOverlay()==null&&mc.getCameraEntity()==camera&&!SceneShaderPack.shadows();}
  @SubscribeEvent public static void restoreCamera(ClientTickEvent.Pre e){if(enabled()){var mc=Minecraft.getInstance();if(mc.player!=null&&mc.getCameraEntity()!=mc.player)mc.setCameraEntity(mc.player);}}
  @SubscribeEvent public static void frameBegin(RenderFrameEvent.Pre e){if(enabled())renderedFrame=worldFrame=false;}
  @SubscribeEvent public static void rendered(RenderLivingEvent.Post<?,?> e){
    if(!enabled()||phase<0||phase>=CASES.size()||!settled||!CASES.get(phase).actor()||!(e.getEntity() instanceof ChaoEntity c)||!c.getUUID().equals(actorId))return;
    var mc=Minecraft.getInstance();if(!admitted(mc)||!c.state().encode().equals(expectedState)||!G.toJson(gear(c)).equals(expectedGear))return;var entry=CASES.get(phase);require(e.getRenderer().getModel() instanceof ChaoModel,"actual production ChaoModel missing");var model=(ChaoModel)e.getRenderer().getModel();require(model.officialActive()&&model.officialPosesFinite()&&!c.officialComparison(),"production poses missing/nonfinite/comparison");
    var data=new JsonObject();data.addProperty("actual_living_entity",true);data.addProperty("actual_production_model",true);data.addProperty("renderer",e.getRenderer().getClass().getName());data.addProperty("uuid",c.getUUID().toString());data.addProperty("state",c.state().encode());data.addProperty("state_sha256",sha(c.state().encode()));data.add("equipment",gear(c));data.addProperty("equipment_sha256",sha(G.toJson(gear(c))));data.addProperty("finite_poses",true);data.addProperty("gameplay_geometry",model.gameplayFormKey());data.addProperty("official_geometry",model.officialGeometryKey());data.addProperty("motion",String.valueOf(model.officialMotion()));
    var h=model.accessoryBounds("skull");require(h!=null&&h.length==6&&Math.abs(h[3]-h[0]-7)<.0001f&&Math.abs(h[4]-h[1]-6)<.0001f&&Math.abs(h[5]-h[2]-6)<.0001f,"original head box changed");data.add("skull_bounds",G.toJsonTree(h));data.add("body_bounds",G.toJsonTree(model.accessoryBounds("body")));data.add("eyes",G.toJsonTree(model.accessoryEyes()));data.addProperty("preview_kind",c.toyPlayingKind());data.addProperty("preview_remaining",c.toyPlayingTicks());data.addProperty("render_observed_server_elapsed",elapsed);
    if(!entry.preview()){
      var power=BabyPowerMorphology084.sample(c.state());require(power.enabled()&&!c.state().adult&&power.training()==(entry.trained()?1:0)&&Math.abs(power.maturity()-entry.maturity())<.0001,"actual baby training/maturity input mismatch");
      float display=HeroRunAppearance052.babyPowerGrowth084(c.state());require(Math.abs(display-power.growth())<.0001,"displayed Power frame did not settle to expected target");data.add("baby_power_sample",G.toJsonTree(power));data.addProperty("displayed_power_growth",display);data.addProperty("locked_alignment",c.state().lockedAlignment());data.addProperty("prospective_first_type",c.state().lockedPrimaryType());data.addProperty("adult",c.state().adult);
    }
    if(entry.id()==5)require(!c.officialComparison()&&c.sourceReferenceKey().isEmpty(),"client did not receive current garden reference flags");
    data.addProperty("official_comparison",c.officialComparison());data.addProperty("source_reference",c.sourceReferenceKey());
    data.addProperty("incoming_entity_packed_light",e.getPackedLight());observed=data;renderedFrame=true;actorFrames++;
  }
  private static JsonObject baked(Minecraft mc,Case c){
    var cached=BAKED.get(c.kind());if(cached!=null)return cached.deepCopy();var state=mc.level.getBlockState(TOY);require(state.is(ToyContent.block(c.toy())),"placed source missing");var model=mc.getBlockRenderer().getBlockModel(state);var quads=new ArrayList<BakedQuad>();quads.addAll(model.getQuads(state,null,RandomSource.create(84),ModelData.EMPTY,null));for(var side:Direction.values())quads.addAll(model.getQuads(state,side,RandomSource.create(84),ModelData.EMPTY,null));require(!quads.isEmpty(),"actual placed model empty");
    double[] bounds={Double.POSITIVE_INFINITY,Double.POSITIVE_INFINITY,Double.POSITIVE_INFINITY,Double.NEGATIVE_INFINITY,Double.NEGATIVE_INFINITY,Double.NEGATIVE_INFINITY};int northEye=0,southEye=0,skinQuads=0;double skinMin=Double.POSITIVE_INFINITY,skinMax=Double.NEGATIVE_INFINITY;
    for(var q:quads){
      int[] v=q.getVertices();boolean north=true,south=true,skin=q.getSprite().contents().name().getPath().contains("sa2_star01_52");
      for(int k=0;k<4;k++){
        double x=Float.intBitsToFloat(v[k*IQuadTransformer.STRIDE]),y=Float.intBitsToFloat(v[k*IQuadTransformer.STRIDE+1]),z=Float.intBitsToFloat(v[k*IQuadTransformer.STRIDE+2]);require(Double.isFinite(x)&&Double.isFinite(y)&&Double.isFinite(z),"actual baked vertex nonfinite");double[] point={x,y,z};for(int a=0;a<3;a++){bounds[a]=Math.min(bounds[a],point[a]);bounds[a+3]=Math.max(bounds[a+3],point[a]);}
        boolean area=x>=12.5/16-.00001&&x<=13.2/16+.00001&&y>=8./16-.00001&&y<=8.8/16+.00001;north&=area&&Math.abs(z-4.9/16)<.00001;south&=area&&Math.abs(z-11.1/16)<.00001;if(skin){skinMin=Math.min(skinMin,y);skinMax=Math.max(skinMax,y);}
      }
      if(north)northEye++;if(south)southEye++;if(skin)skinQuads++;
    }
    var data=new JsonObject();data.addProperty("baked_model_class",model.getClass().getName());data.addProperty("actual_baked_quad_count",quads.size());data.addProperty("actual_baked_vertices_finite",true);data.add("actual_baked_model_bounds",G.toJsonTree(bounds));
    if(c.kind().equals("duck")){require(northEye>0&&southEye>0,"actual model lacks opposite exposed eye faces");data.addProperty("north_eye_surface_quads",northEye);data.addProperty("south_eye_surface_quads",southEye);}
    else{require(Math.abs(bounds[1]-.005)<.00001&&bounds[4]<.15&&skinQuads>0&&skinMax-skinMin<.00001,"actual placed tambourine not flat/floor supported");data.addProperty("decorated_skin_quad_count",skinQuads);data.addProperty("decorated_skin_y_span",skinMax-skinMin);data.addProperty("floor_clearance_blocks",bounds[1]);}
    var id=ResourceLocation.fromNamespaceAndPath(ChaoGarden.MODID,"models/block/"+c.toy()+"_idle.json");try(var stream=mc.getResourceManager().open(id)){data.addProperty("actual_model_json",id.toString());data.addProperty("actual_model_json_sha256",sha(stream.readAllBytes()));}catch(Exception error){throw new IllegalStateException(error);}
    data.addProperty("scope","Actual loaded baked source coordinates; ordinary AFTER_LEVEL scene, not a GPU draw-call interception.");BAKED.put(c.kind(),data);return data.deepCopy();
  }
  @SubscribeEvent public static void world(RenderLevelStageEvent e){
    if(!enabled()||phase<0||phase>=CASES.size()||!settled||e.getStage()!=RenderLevelStageEvent.Stage.AFTER_LEVEL)return;var mc=Minecraft.getInstance();if(!admitted(mc))return;var c=CASES.get(phase);
    if(c.actor()){if(renderedFrame)worldFrame=true;return;}
    var state=mc.level.getBlockState(TOY);require(state.is(ToyContent.block(c.toy())),"ordinary placed source disappeared");var proof=new JsonObject();proof.addProperty("actual_placed_world_source",true);proof.addProperty("actual_world_render_stage","AFTER_LEVEL");proof.addProperty("source_block",BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString());proof.addProperty("source_render_shape",state.getRenderShape().name());proof.add("baked_source",baked(mc,c));proof.add("source_block_position",G.toJsonTree(new int[]{TOY.getX(),TOY.getY(),TOY.getZ()}));proof.addProperty("support_block",BuiltInRegistries.BLOCK.getKey(mc.level.getBlockState(TOY.below()).getBlock()).toString());observed=proof;worldFrame=true;
  }
  private static void capture(Minecraft mc,Shot shot){
    int index=phase;var c=CASES.get(index);var proof=observed.deepCopy();proof.addProperty("source_index",c.id());proof.addProperty("name",c.name());proof.addProperty("kind",c.kind());proof.addProperty("label",shot.label());proof.addProperty("view",shot.view());proof.addProperty("scheduled_tick",shot.tick());proof.addProperty("frame_admission_server_elapsed",elapsed);proof.addProperty("source_freeze",FREEZE);proof.addProperty("shaders_active",SceneShaderPack.active());proof.addProperty("fov",40);
    var pos=mc.gameRenderer.getMainCamera().getPosition();proof.add("actual_camera_position",G.toJsonTree(new double[]{pos.x,pos.y,pos.z}));proof.addProperty("actual_camera_yaw",mc.gameRenderer.getMainCamera().getYRot());proof.addProperty("actual_camera_pitch",mc.gameRenderer.getMainCamera().getXRot());
    if(c.preview()){if(shot.label().startsWith("recovered"))require(proof.get("preview_remaining").getAsInt()==0,"recovery still has preview token");else require(proof.get("preview_remaining").getAsInt()>0&&proof.get("preview_kind").getAsString().equals(TambourinePlayRules.KIND),"actual production tambourine preview absent");}
    String name=String.format("fixes084-%02d-%s-%s.png",c.id(),c.name(),shot.label());Path file=OUT.resolve("screenshots").resolve(name);try{Files.createDirectories(file.getParent());}catch(Exception e){throw new IllegalStateException(e);}
    Screenshot.grab(OUT.toFile(),name,mc.getMainRenderTarget(),message->{require(Files.isRegularFile(file),"native PNG missing");proof.addProperty("png",file.toString());proof.addProperty("png_sha256",sha(file));write(OUT.resolve("frames").resolve(name.replace(".png",".json")),proof);SAVED.incrementAndGet();LogUtils.getLogger().info("FIXES084_CAPTURE index={} sourceIndex={} name={} view={} pngSha256={}",index,c.id(),name,shot.view(),proof.get("png_sha256"));});shots++;shotIndex++;viewFrames=0;
  }
  @SubscribeEvent public static void frameEnd(RenderFrameEvent.Post e){
    if(!enabled()||phase<0||phase>=CASES.size()||clientPhase!=phase)return;var mc=Minecraft.getInstance();var c=CASES.get(phase);var schedule=schedule(c);
    // Captures still require the exact saved fixture and a real admitted world frame.
    // Once every requested image exists, native server cleanup can finish the case
    // without a new pre-care-matching render: ordinary care resumes after preview.
    if(worldFrame&&observed!=null){worldFrames++;viewFrames++;}
    if(shotIndex<schedule.size()){if(!worldFrame||observed==null)return;var shot=schedule.get(shotIndex);if(settled&&elapsed>=shot.tick()&&viewFrames>=4)capture(mc,shot);return;}
    if(!complete)return;require(worldFrames>=12&&(!c.actor()||actorFrames>=12),"actual frame coverage missing");
    var done=new JsonObject();done.addProperty("source_index",c.id());done.addProperty("name",c.name());done.addProperty("kind",c.kind());done.addProperty("captures",schedule.size());done.addProperty("actor_frames",actorFrames);done.addProperty("world_frames",worldFrames);done.addProperty("source_freeze",FREEZE);done.addProperty("complete",true);
    if(c.preview()){
      require(!nativeCompleteState.isEmpty(),"native cleanup state receipt missing");done.addProperty("preview_exact_state_asserted_while_active",true);done.addProperty("native_complete_state",nativeCompleteState);done.addProperty("native_complete_state_sha256",sha(nativeCompleteState));done.addProperty("before_preview_state_sha256",sha(expectedState));done.addProperty("normal_care_may_resume_after_preview",true);done.addProperty("new_matching_render_frame_required_for_completion",false);
    }
    write(OUT.resolve("complete").resolve(String.format("%02d-%s.json",c.id(),c.name())),done);completed++;
    LogUtils.getLogger().info("FIXES084_RENDER_COMPLETE index={} sourceIndex={} name={} captures={} actorFrames={} worldFrames={}",phase,c.id(),c.name(),schedule.size(),actorFrames,worldFrames);
    int old=phase;pending=phase;clientPhase=-2;onServer(mc.getSingleplayerServer(),()->{if(phase==old){if(phase+1<CASES.size())next(mc.getSingleplayerServer(),phase+1);else phase=CASES.size();}});
  }
  private static void catalog(){
    require(FREEZE.matches("[0-9a-f]{64}"),"set exact final CHAO_FIXES084_SOURCE_FREEZE");require(!Files.exists(OUT.resolve("case-catalog.json")),"fresh output directory required");
    var data=new JsonObject();data.addProperty("source_freeze",FREEZE);data.addProperty("scope","Actual world blocks and synchronized Chao, production rendering and existing native tambourine preview. Static cases settle20physics ticks then pause; preview uses ordinary20TPS.");data.addProperty("shader_pack_verified",false);data.addProperty("independent_remote_multiplayer",false);data.addProperty("audio_listening",false);
    var rows=new JsonArray();for(var c:CASES){var row=G.toJsonTree(c).getAsJsonObject();row.add("shots",G.toJsonTree(schedule(c)));rows.add(row);}data.add("cases",rows);data.addProperty("expected_cases",CASES.size());data.addProperty("expected_native_pngs",CASES.stream().mapToInt(c->schedule(c).size()).sum());write(OUT.resolve("case-catalog.json"),data);
  }
  @SubscribeEvent public static void client(ClientTickEvent.Post e){
    if(!enabled()||finished)return;var mc=Minecraft.getInstance();if(fatal!=null)throw new IllegalStateException("FIXES084 server failure: "+fatal);if(wallStart==0)wallStart=System.nanoTime();require(System.nanoTime()-wallStart<2_400_000_000_000L,"study wall deadline");
    if(mc.screen instanceof AccessibilityOnboardingScreen)mc.setScreen(new TitleScreen());
    if(!started&&mc.screen instanceof TitleScreen){
      started=true;catalog();mc.options.renderDistance().set(3);mc.options.simulationDistance().set(5);mc.options.framerateLimit().set(45);mc.options.fov().set(40);mc.options.gamma().set(.5);mc.options.pauseOnLostFocus=false;mc.options.hideGui=true;mc.options.tutorialStep=net.minecraft.client.tutorial.TutorialSteps.NONE;mc.getWindow().setWindowed(1440,1000);mc.resizeDisplay();
      mc.createWorldOpenFlows().createFreshLevel("Fixes084-"+System.currentTimeMillis(),new LevelSettings("Fixes084",GameType.CREATIVE,false,Difficulty.PEACEFUL,true,new GameRules(),WorldDataConfiguration.DEFAULT),new WorldOptions(2584L,false,false),r->r.registryOrThrow(Registries.WORLD_PRESET).getHolderOrThrow(WorldPresets.FLAT).value().createWorldDimensions(),mc.screen);
    }
    if(!setup&&mc.player!=null&&mc.getSingleplayerServer()!=null){
      setup=true;mc.setScreen(null);ChaoRenderer.suppressNames=true;onServer(mc.getSingleplayerServer(),()->{
        var server=mc.getSingleplayerServer();var level=server.overworld();level.setDayTime(6000);level.setWeatherParameters(0,100000,false,false);level.getGameRules().getRule(GameRules.RULE_DAYLIGHT).set(false,server);level.getGameRules().getRule(GameRules.RULE_WEATHER_CYCLE).set(false,server);level.getGameRules().getRule(GameRules.RULE_DOMOBSPAWNING).set(false,server);
        for(var pos:BlockPos.betweenClosed(-10,79,-8,10,86,15))level.setBlock(pos,pos.getY()==79?Blocks.SMOOTH_STONE.defaultBlockState():Blocks.AIR.defaultBlockState(),3);next(server,0);
      });
    }
    if(phase>=0&&phase<CASES.size()&&mc.level!=null){if(clientPhase==-2&&phase==pending)return;if(clientPhase!=phase){clientPhase=phase;shotIndex=viewFrames=actorFrames=worldFrames=0;observed=null;}camera(mc);}
    if(phase==CASES.size()&&SAVED.get()==shots){
      require(completed==CASES.size()&&shots==CASES.stream().mapToInt(c->schedule(c).size()).sum(),"missing case/PNG");finished=true;write(OUT.resolve("visual-complete.json"),Map.of("source_freeze",FREEZE,"completed_cases",completed,"saved_pngs",SAVED.get(),"actual_production_sources",true,"audio_listening",false,"independent_remote_multiplayer",false));onServer(mc.getSingleplayerServer(),()->mc.getSingleplayerServer().tickRateManager().setFrozen(false));LogUtils.getLogger().info("FIXES084_VISUAL_COMPLETE cases={} captures={} saved={} actualProduction=true output={}",completed,shots,SAVED.get(),OUT);mc.stop();
    }
  }
}
