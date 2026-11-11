package com.chaogarden.client;

import com.chaogarden.ChaoGarden;
import com.chaogarden.ChaoVariants;
import com.chaogarden.GardenConfig;
import com.chaogarden.care.*;
import com.chaogarden.core.ChaoPersonality;
import com.chaogarden.core.ChaoEmotion;
import com.chaogarden.core.OmochaoRules;
import com.chaogarden.cwe.CweRobotPartItem;
import com.chaogarden.cwe.OmochaoLifecycle;
import com.chaogarden.companion.CompanionContent;
import com.chaogarden.companion.AccessoriesBridge;
import net.minecraft.world.effect.MobEffects;
import com.chaogarden.cwe.CweCatalog;
import com.chaogarden.market.client.CweMarketScreen;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.ItemCost;
import net.minecraft.network.chat.Component;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.component.DyedItemColor;
import com.chaogarden.core.ChaoPreferences;
import com.chaogarden.core.ChaoState;
import com.chaogarden.entity.ChaoEntity;
import com.chaogarden.entity.ChaoEggEntity;
import com.chaogarden.fruit.FruitCatalog;
import com.chaogarden.market.MarketContent;
import com.chaogarden.market.MarketStock;
import com.chaogarden.toys.ChaoToyBlock;
import com.chaogarden.toys.ToyContent;
import com.google.gson.*;
import com.mojang.logging.LogUtils;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderFrameEvent;
import org.lwjgl.glfw.GLFW;

/** Opt-in actual-game study driven by the same metadata as the offline gallery. */
@EventBusSubscriber(modid=ChaoGarden.MODID,value=Dist.CLIENT)
public final class Audit022VisualSmoke {
  private record Shot(String key,String scene,int frame,int total) {}
  private static final AABB AREA=new AABB(-28,60,-18,28,88,32);
  private static final AtomicInteger saved=new AtomicInteger();
  private static List<Shot> shots;
  private static int ticks,stage=-1,ready=-1,earliest,frames,stageStarted,completed;
  private static boolean prepared,captured,stopping;
  private static volatile UUID subject;
  private static volatile ItemEggVisualSmokeSupport022.InventoryPage inventoryPage;
  private static volatile String expectedInspectionStats;
  private static volatile Map<UUID,String> expected=Map.of();
  private static volatile Map<UUID,Vec3> routes=Map.of();
  private static volatile UUID routePet;
  private static String prefix;
  private static volatile Vec3 observerPosition;
  private static volatile UUID recoveredOutput;
  private static volatile String companionScene="";
  private static volatile boolean companionReady;
  private static boolean flightStarted,boostAccepted,waterSeen;
  private Audit022VisualSmoke() {}

  private static void load(Minecraft mc) {
    Path path=mc.gameDirectory.toPath().resolve("../docs/release-scenes-0.22.0.json").normalize();
    try(var reader=Files.newBufferedReader(path)) {
      var plan=JsonParser.parseReader(reader).getAsJsonObject();
      if(!plan.get("release").getAsString().equals("0.22.0"))throw new IllegalStateException("Wrong capture plan");
      prefix=plan.get("filename_prefix").getAsString();
      Map<String,Shot> entries=new LinkedHashMap<>();
      for(var f:plan.getAsJsonArray("features")) {
        var j=f.getAsJsonObject();String key=j.get("key").getAsString();
        entries.put(key,new Shot(key,j.get("scene").getAsString(),0,1));
      }
      for(var m:plan.getAsJsonArray("motions")) {
        var j=m.getAsJsonObject();var keys=j.getAsJsonArray("frames");
        for(int i=0;i<keys.size();i++) {
          String key=keys.get(i).getAsString();entries.put(key,new Shot(key,j.get("scene").getAsString(),i,keys.size()));
        }
      }
      var ordered=new ArrayList<Shot>();
      for(var key:plan.getAsJsonArray("capture_order")) {
        Shot s=entries.remove(key.getAsString());if(s==null)throw new IllegalStateException("Duplicate or missing capture key");ordered.add(s);
      }
      if(!entries.isEmpty())throw new IllegalStateException("Unordered capture keys");shots=List.copyOf(ordered);
    }catch(Exception e){throw new IllegalStateException("Cannot read actual-client capture plan",e);}
  }

  @SubscribeEvent public static void tick(ClientTickEvent.Post event) {
    if(!Boolean.getBoolean("chao_garden.audit022Study"))return;
    var mc=Minecraft.getInstance();
    if(mc.level==null||mc.player==null||mc.getSingleplayerServer()==null||stopping)return;
    if(mc.player.isDeadOrDying()){mc.player.respawn();mc.setScreen(null);return;}
    if(shots==null)load(mc);
    ticks++;
    if(ticks==1) {
      long planned=shots.stream().filter(x->x.scene.startsWith("catalog-all-")).count();
      int actual=ItemEggVisualSmokeSupport022.pageCount();
      LogUtils.getLogger().info("CHAO_AUDIT022_CATALOG_PREFLIGHT items={} pages={} planned={}",ItemEggVisualSmokeSupport022.itemCount(),actual,planned);
      if(planned!=actual)throw new IllegalStateException("Inventory plan pages "+planned+" != actual "+actual);
    }
    mc.getSingleplayerServer().execute(()->{
      var world=mc.getSingleplayerServer().overworld();
      if(stage>=0)tickCompanion(mc.getSingleplayerServer(),shots.get(stage));

      for(var goal:routes.entrySet())if(world.getEntity(goal.getKey()) instanceof ChaoEntity c) {
        var p=goal.getValue();c.getNavigation().moveTo(p.x,p.y,p.z,1);
      }
      if(routePet!=null&&world.getEntity(routePet) instanceof ChaoEntity c) {
        var player=mc.getSingleplayerServer().getPlayerList().getPlayers().getFirst();
        player.setGameMode(GameType.SURVIVAL);c.mobInteract(player,InteractionHand.MAIN_HAND);
        player.setGameMode(GameType.SPECTATOR);routePet=null;
      }
    });
    if(ticks==30)start(mc,0);
    if(captured&&ticks>=earliest) {
      if(stage+1==shots.size()) {
        if(saved.get()!=completed)return;
        stopping=true;ChaoRenderer.suppressNames=false;
        LogUtils.getLogger().info("CHAO_AUDIT022_STUDY_COMPLETE: {} actual Minecraft views",completed);
        mc.stop();return;
      }
      start(mc,stage+1);
    }
    if(stage<0||captured||ready!=stage||ticks<earliest)return;
    if(ticks-stageStarted>800)throw new IllegalStateException("Capture stalled at "+shots.get(stage));
    if(!prepared) {
      var s=shots.get(stage);mc.options.setCameraType(CameraType.FIRST_PERSON);
      if(!s.scene.startsWith("companion-")) {
        mc.player.stopFallFlying();mc.player.setSwimming(false);mc.player.setPose(Pose.STANDING);
      }
      mc.options.hideGui=!(s.scene.startsWith("catalog-")||inspectionScene(s.scene)||s.scene.startsWith("market-")||s.scene.equals("fruit-catalog")||s.scene.equals("personality-chart")||s.scene.startsWith("companion-"));
      mc.options.fov().set(s.scene.startsWith("forms-")?34:s.scene.startsWith("growth-")||s.scene.startsWith("mixed-")||s.scene.startsWith("reverse-")||s.scene.startsWith("opposite-")||s.scene.startsWith("baby-")?34:36);
      ChaoRenderer.suppressNames=true;
      if(s.scene.startsWith("companion-water-")||s.scene.equals("companion-land-cleanup")) {
        if(!companionReady)return;
        mc.options.guiScale().set(2);mc.resizeDisplay();mc.setScreen(new InventoryScreen(mc.player));
      } else if(s.scene.startsWith("companion-flight-")||s.scene.equals("companion-robot-boost")) {
        if(!companionReady)return;mc.options.setCameraType(CameraType.THIRD_PERSON_BACK);
      }
      if(s.scene.startsWith("catalog-")||s.scene.equals("fruit-catalog")) {
        if(s.scene.startsWith("catalog-all-")?!ItemEggVisualSmokeSupport022.inventoryMatches(mc.player,inventoryPage):mc.player.getInventory().getItem(9).isEmpty())return;
        mc.options.guiScale().set(3);mc.resizeDisplay();mc.setScreen(new InventoryScreen(mc.player));
      }
      if(inspectionScene(s.scene)) {
        var c=clientSubject(mc);if(c==null)return;
        mc.options.guiScale().set(2);mc.resizeDisplay();
        var chart=new ChaoInspectionScreen(c,s.scene.equals("chart-genes")?1:s.scene.equals("chart-parts")?2:0);
        if(s.scene.startsWith("chart-"))switch(s.scene.substring(6)) {
          case "status","genes","parts"->{}case "dna"->chart.showAnimals(1);case "profile"->chart.showMind(0);case "feelings"->chart.showMind(1);case "health"->chart.showMind(2);case "family","family-legacy"->chart.showMind(3);
          case "doctor","treated","grade"->chart.showCare(0);case "lessons","class-complete"->chart.showCare(1);case "wardrobe"->chart.showCare(2);
          default->{
            if(s.scene.startsWith("chart-robot-family-"))chart.showMind(3);
            else if(s.scene.equals("chart-robot-health"))chart.showMind(2);
            else if(s.scene.equals("chart-robot-genes"))chart.showAnimals(0);
            else if(!(s.scene.startsWith("chart-growth-")||s.scene.startsWith("chart-companion-")))throw new IllegalStateException("Unknown chart");
          }
        }
        mc.setScreen(chart);
      }
      if(s.scene.startsWith("market-")) {
        if(!(mc.screen instanceof CweMarketScreen market))return;
        market.selectCategory(Integer.parseInt(s.scene.substring(7)));
      }
      if(!mc.options.hideGui) {
        if(s.scene.startsWith("chart-companion-")&&mc.screen instanceof ChaoInspectionScreen chart) {
          String[] parts=s.scene.split("-");int stat=Integer.parseInt(parts[2]);
          double factor=mc.getWindow().getGuiScale();moveMouse(mc,chart.screenX(280)*(double)factor,chart.screenY(114+stat*26)*(double)factor);
        } else if(s.scene.equals("chart-status")&&mc.screen instanceof ChaoInspectionScreen chart) {
          double factor=mc.getWindow().getGuiScale();moveMouse(mc,chart.screenX(400)*factor,chart.screenY(294)*factor);
        } else moveMouse(mc,4,4);
      }
      prepared=true;
    }
    if(clearFixtureParticles(shots.get(stage).scene))mc.particleEngine.setLevel(mc.level);
    mc.getToasts().clear();
  }

  private static void start(Minecraft mc,int next) {
    Shot s=shots.get(next);
    if(s.frame==0){if(mc.screen!=null)mc.player.closeContainer();mc.setScreen(null);}
    stage=next;ready=-1;frames=0;captured=prepared=false;stageStarted=ticks;
    // Subsequent motion frames can be sampled on the next game tick; only
    // the first frame needs setup/sync time. Preserve every recorded interval.
    earliest=ticks+(s.frame>0?0:s.scene.startsWith("robot-")||s.scene.equals("motion-robot")?60:s.total>1||s.scene.startsWith("offer-")||s.scene.equals("inspect-item")?2:25);
    mc.getSingleplayerServer().execute(()->{setup(mc.getSingleplayerServer(),s);ready=next;});
  }
  private static ChaoEntity clientSubject(Minecraft mc) {
    return subject==null?null:mc.level.getEntitiesOfClass(ChaoEntity.class,AREA,c->c.getUUID().equals(subject)).stream().findFirst().orElse(null);
  }
  private static String signature(Entity e) {
    if(e instanceof ChaoEntity c){var s=c.state();return s.variant+":"+s.adult+":"+s.dead+":"+c.isCocooned()+":"+s.baseColor()+":"+s.jewel()+":"+s.isShiny()+":"+s.twoTone+":"+s.character+":"+s.align+":"+s.primaryType+":"+Math.round(s.evolutionStrength()*100)+":"+s.swimFly+":"+s.runPower+":"+s.negativePalette+":"+s.brightCoat+":"+s.intTrait("content.construction")+":"+c.officialComparison()+":"+c.sourceReferenceKey();}
    if(e instanceof ChaoEggEntity c)return c.state().variant+":"+c.state().baseColor()+":"+c.state().jewel()+":"+c.state().isShiny();
    return e.getType().toShortString();
  }
  @SubscribeEvent public static void frame(RenderFrameEvent.Post event) {
    if(!Boolean.getBoolean("chao_garden.audit022Study")||stopping||captured||!prepared||ready!=stage||ticks<earliest)return;
    var mc=Minecraft.getInstance();if(mc.level==null||mc.player==null)return;
    boolean synced=true;
    for(var entry:expected.entrySet())if(mc.level.getEntitiesOfClass(Entity.class,AREA,
        e->e.getUUID().equals(entry.getKey())&&signature(e).equals(entry.getValue())).size()!=1)synced=false;
    var shot=shots.get(stage);var focus=clientSubject(mc);
    if(!shot.scene.startsWith("companion-")&&observerPosition!=null) {
      var eye=mc.gameRenderer.getMainCamera().getPosition();
      synced&=mc.player.getPose()==Pose.STANDING&&!mc.player.isFallFlying()&&!mc.player.isSwimming()
          &&mc.player.position().distanceToSqr(observerPosition)<.004
          &&eye.y-mc.player.getY()>=1.5;
    }
    if(shot.scene.equals("robot-component-drop")) {
      var items=mc.level.getEntitiesOfClass(ItemEntity.class,AREA,item->item.getUUID().equals(recoveredOutput));
      synced&=items.size()==1&&Math.abs(Math.cos(items.getFirst().getSpin(0)+Math.PI/4))>.9;
    }
    if(shot.scene.equals("offer-think"))synced&=focus!=null&&focus.getItemAction()==ChaoEntity.ItemAction.THINK;
    if(shot.scene.equals("offer-eat"))synced&=focus!=null&&focus.getItemAction()==ChaoEntity.ItemAction.EAT&&focus.getItemActionTicks()<=42;
    if(shot.scene.equals("offer-reject"))synced&=focus!=null&&focus.getItemAction()==ChaoEntity.ItemAction.REJECT&&focus.getItemActionTicks()<=13;
    if(shot.scene.equals("inspect-item"))synced&=focus!=null&&focus.getItemAction()==ChaoEntity.ItemAction.INSPECT&&focus.getItemActionTicks()<=42;
    if(shot.scene.equals("toy-autonomous")||shot.scene.equals("motion-toy")&&shot.frame==0)
      synced&=focus!=null&&focus.toyPlayingTicks()>80;
    if(shot.scene.startsWith("companion-")) {
      synced&=companionReady;
      boolean vision=mc.player.hasEffect(MobEffects.NIGHT_VISION),breath=mc.player.hasEffect(MobEffects.WATER_BREATHING);
      if(shot.scene.equals("companion-water-70"))synced&=vision&&!breath;
      if(shot.scene.equals("companion-water-99"))synced&=vision&&breath;
      if(shot.scene.equals("companion-water-0")||shot.scene.equals("companion-land-cleanup"))synced&=!vision&&!breath;
      if(shot.scene.startsWith("companion-flight-")||shot.scene.equals("companion-robot-boost"))synced&=mc.player.isFallFlying();
    }
    if(shot.scene.startsWith("robot-stage-"))synced&=focus!=null&&Math.abs(focus.getX()-.85)<.02;
    if(shot.scene.startsWith("robot-coat-"))synced&=focus!=null&&Math.abs(focus.getX()+.85)<.02;
    if(inspectionScene(shot.scene))synced&=mc.screen instanceof ChaoInspectionScreen;
    if(shot.scene.startsWith("market-"))synced&=mc.screen instanceof CweMarketScreen;
    if(shot.scene.startsWith("catalog-"))synced&=mc.screen instanceof InventoryScreen;
    if(shot.scene.startsWith("catalog-all-"))synced&=ItemEggVisualSmokeSupport022.inventoryMatches(mc.player,inventoryPage);
    if(inspectionScene(shot.scene))synced&=focus!=null&&Objects.equals(expectedInspectionStats,inspectionStats(focus));
    if(shot.scene.equals("fruit-catalog"))synced&=mc.screen instanceof InventoryScreen;
    if(!synced){frames=0;return;}
    if(clearFixtureParticles(shot.scene))mc.particleEngine.setLevel(mc.level);
    if(++frames<(shot.total>1?1:4))return;
    String file=prefix+shot.key+".png";
    Screenshot.grab(mc.gameDirectory,file,mc.getMainRenderTarget(),message->{
      LogUtils.getLogger().info("CHAO_AUDIT022_SCREENSHOT {}: {}",file,message.getString());saved.incrementAndGet();
    });
    LogUtils.getLogger().info("CHAO_AUDIT022_CAPTURED {} stage={} gameTick={} entity={} x={} y={} z={} action={} actionTicks={} toy={}",
      file,stage,mc.level.getGameTime(),subject,focus==null?0:focus.getX(),focus==null?0:focus.getY(),focus==null?0:focus.getZ(),
      focus==null?"none":focus.getItemAction(),focus==null?0:focus.getItemActionTicks(),focus==null?"":focus.toyPlayingKind());
    var camera=mc.gameRenderer.getMainCamera().getPosition();
    LogUtils.getLogger().info("CHAO_AUDIT022_CAMERA file={} pose={} player={} eye={} eyeOffset={} fallFlying={} swimming={}",file,mc.player.getPose(),mc.player.position(),camera,camera.y-mc.player.getY(),mc.player.isFallFlying(),mc.player.isSwimming());
    completed++;captured=true;earliest=ticks+(shot.total>1?3:2);
  }

  private static ChaoState state(int color,boolean two,boolean shiny,int jewel) {
    var s=ChaoState.egg(color,shiny,two,jewel,new Random(1800));s.adult=true;s.points[ChaoState.RUN]=80;s.setEvolutionStrength(1.2f);
    s.happiness=90;s.bond=90;s.energy=100;s.hunger=65;s.boredom=100;s.personality=ChaoPreferences.CURIOUS;return s;
  }
  private static ChaoEntity chao(MinecraftServer server,ChaoState s,double x,double z,float yaw,boolean active,Map<UUID,String> waiting) {
    var c=ChaoGarden.CHAO.get().create(server.overworld());if(c==null)throw new IllegalStateException("No Chao");
    c.setState(s);c.owner=server.getPlayerList().getPlayers().getFirst().getUUID();c.setNoAi(!active);c.setInvulnerable(true);
    if(!active)c.goalSelector.removeAllGoals(g->true);
    else c.goalSelector.removeAllGoals(g->g.getClass().getName().contains("Follow")||g.getClass().getName().contains("RandomStroll"));
    c.targetSelector.removeAllGoals(g->true);c.moveTo(x,64,z,yaw,0);c.setYHeadRot(yaw);server.overworld().addFreshEntity(c);
    waiting.put(c.getUUID(),signature(c));return c;
  }
  private static void egg(MinecraftServer server,ChaoState s,double x,Map<UUID,String> waiting) {
    var e=ChaoGarden.CHAO_EGG.get().create(server.overworld());if(e==null)throw new IllegalStateException("No egg");
    e.setState(s);e.setIncubation(0);e.setNoGravity(true);e.setPos(x,64,0);server.overworld().addFreshEntity(e);waiting.put(e.getUUID(),signature(e));
  }
  private static void camera(ServerPlayer p,MinecraftServer s,double x,double y,double z,float yaw,float pitch) {
    // Observer feet must clear the floor. The earlier low-eye flight pose could
    // survive a previous real elytra scene and put this camera inside a block.
    double raised=Math.max(64.05,y),horizontal=Math.max(.1,Math.sqrt(x*x+z*z));
    float adjusted=(float)Math.toDegrees(Math.atan(Math.tan(Math.toRadians(pitch))+(raised-y)/horizontal));
    p.stopFallFlying();p.setSwimming(false);p.setPose(Pose.STANDING);p.setDeltaMovement(Vec3.ZERO);
    p.teleportTo(s.overworld(),x,raised,z,yaw,adjusted);observerPosition=new Vec3(x,raised,z);
  }
  private static void development(ChaoState s,int type,float amount) {
    s.swimFly=type==1?-amount:type==2?amount:0;
    s.runPower=type==3?-amount:type==4?amount:0;
  }
  private static void gear(ChaoEntity c) {
    c.setItemSlot(EquipmentSlot.FEET,new ItemStack(ChaoGarden.item("baseball_cap_accessory")));
    c.setItemSlot(EquipmentSlot.OFFHAND,new ItemStack(ChaoGarden.item("round_glasses_accessory")));
    c.setItemSlot(EquipmentSlot.LEGS,new ItemStack(ChaoGarden.item("cheese_bowtie_accessory")));
    c.setItemSlot(EquipmentSlot.CHEST,new ItemStack(ChaoGarden.item("blue_hoodie_accessory")));
    c.state().setTraitInt("medal.race_gold",1);c.state().setTrait("medal.selected","race_gold");c.sync();
  }
  private static void view(ServerPlayer p,MinecraftServer server,String angle,double distance) {
    switch(angle) {
      case "side"->camera(p,server,distance,63.35,0,90,7);
      case "back"->camera(p,server,0,63.35,distance,180,7);
      case "oblique"->camera(p,server,distance*.707,63.6,-distance*.707,45,11);
      default->camera(p,server,0,63.35,-distance,0,7);
    }
  }
  private static ChaoState coat(int number) {
    var s=switch(number) {
      case 1->state(2,false,false,0);case 2->state(4,true,false,0);
      case 3->state(6,false,true,0);case 4->state(0,false,false,1);
      case 5->state(0,false,false,13);default->state(0,true,false,0);
    };
    if(number==6)s.negativePalette=true;
    if(number==7){s.expressedColor=4;s.shiny=true;s.brightCoat=true;}
    return s;
  }
  private static ChaoEntity assemble(MinecraftServer server,ChaoEntity builder,int boxes,Map<UUID,String> waiting) {
    builder.state().setTraitInt("reward.wrench",1);
    for(int i=0;i<boxes;i++)if(!com.chaogarden.cwe.CweContent.assembleOmochao(builder))throw new IllegalStateException("Actual Omochao assembly failed at box "+i);
    var e=server.overworld().getEntity(UUID.fromString(builder.state().trait("content.omochao_uuid")));
    if(!(e instanceof ChaoEntity robot))throw new IllegalStateException("Assembly did not retain its prototype UUID");
    robot.setNoAi(true);robot.setInvulnerable(true);robot.goalSelector.removeAllGoals(g->true);robot.targetSelector.removeAllGoals(g->true);
    if(boxes==4) {
      var a=builder.state();var b=robot.state();
      if(a.baseColor()!=b.baseColor()||a.jewel()!=b.jewel()||a.isShiny()!=b.isShiny()||a.twoTone!=b.twoTone||a.negativePalette!=b.negativePalette||a.brightCoat!=b.brightCoat)
        throw new IllegalStateException("Completed Omochao failed builder appearance copy");
      LogUtils.getLogger().info("CHAO_AUDIT022_ROBOT_COAT builder={} robot={} color={} jewel={} shiny={} two={} negative={} bright={}",builder.getUUID(),robot.getUUID(),b.baseColor(),b.jewel(),b.isShiny(),b.twoTone,b.negativePalette,b.brightCoat);
    }
    waiting.put(robot.getUUID(),signature(robot));return robot;
  }
  private record RobotPair(ChaoEntity creator,ChaoEntity robot) {}
  private static RobotPair robotGeneration(MinecraftServer server,int generation,Map<UUID,String> waiting) {
    String[] names={"Mochi","Luna","Sora","Aster"};ItemStack heart=ItemStack.EMPTY;RobotPair pair=null;
    for(int step=0;step<=generation;step++) {
      var creator=chao(server,coat(step%4),.05,0,0,false,new LinkedHashMap<>());
      creator.setCustomName(Component.literal(names[step]));creator.state().setTraitInt("reward.wrench",1);
      for(int part=0;part<4;part++) {
        ItemStack offered=part==0&&!heart.isEmpty()?heart:new ItemStack(ChaoGarden.item("omochao_parts_box"));
        if(!com.chaogarden.cwe.CweContent.assembleOmochao(creator,offered))throw new IllegalStateException("Actual robot reconstruction failed at generation "+step+" part "+part);
        offered.shrink(1);
      }
      var built=server.overworld().getEntity(UUID.fromString(creator.state().trait("content.omochao_uuid")));
      if(!(built instanceof ChaoEntity robot))throw new IllegalStateException("Missing actual reconstructed robot");
      robot.setNoAi(true);robot.setInvulnerable(true);robot.goalSelector.removeAllGoals(g->true);robot.targetSelector.removeAllGoals(g->true);
      for(int grade:robot.state().grades)if(grade!=Math.min(5,2+step))throw new IllegalStateException("Heart did not raise all robot grades");
      if(!robot.state().familyName("family.parent.0").equals(names[step]))throw new IllegalStateException("New creator missing from actual parent ledger");
      if(step>0&&!robot.state().familyName("family.grandparent.0").equals(names[step-1]))throw new IllegalStateException("Old creator missing from actual grandparent ledger");
      if(step==generation) {
        creator.moveTo(-.85,64,0,0,0);creator.setYHeadRot(0);pair=new RobotPair(creator,robot);
        waiting.put(creator.getUUID(),signature(creator));waiting.put(robot.getUUID(),signature(robot));
      } else {
        UUID oldId=robot.getUUID();robot.state().happiness=100;robot.state().adultAgeTicks=19;
        var result=OmochaoRules.tickRobot(robot.state(),20);
        if(result!=ChaoState.Transition.REINCARNATED||!OmochaoLifecycle.finish(robot,result))throw new IllegalStateException("Actual cared-for lifecycle failed");
        var outputs=server.overworld().getEntitiesOfClass(ItemEntity.class,AREA,item->item.getItem().is(ChaoGarden.item("robo_heart"))&&CweRobotPartItem.origin(item.getItem()).hasUUID("Robot")&&oldId.equals(CweRobotPartItem.origin(item.getItem()).getUUID("Robot")));
        if(outputs.size()!=1)throw new IllegalStateException("Reincarnation must produce exactly one real heart");
        heart=outputs.getFirst().getItem().copy();outputs.getFirst().discard();creator.discard();
      }
    }
    LogUtils.getLogger().info("CHAO_AUDIT022_ROBOT_GENERATION generation={} rank={} parent={} grandparent={} robot={}",generation,OmochaoRules.rank(pair.robot.state()),pair.robot.state().familyName("family.parent.0"),pair.robot.state().familyName("family.grandparent.0"),pair.robot.getUUID());
    return pair;
  }
  private static void tickCompanion(MinecraftServer server,Shot shot) {
    if(!shot.scene.startsWith("companion-")||!shot.scene.equals(companionScene))return;
    var p=server.getPlayerList().getPlayers().getFirst();var body=CompanionVisualSmokeSupport.body(p);
    if(body==null)return;subject=body.getUUID();
    boolean vision=p.hasEffect(MobEffects.NIGHT_VISION),breath=p.hasEffect(MobEffects.WATER_BREATHING);
    if(shot.scene.equals("companion-land-cleanup")) {
      if(!waterSeen&&vision&&breath) {waterSeen=true;p.teleportTo(server.overworld(),0,64,-5,180,0);p.setDeltaMovement(Vec3.ZERO);}
      companionReady=waterSeen&&!p.isInWater()&&!vision&&!breath;
    } else if(shot.scene.startsWith("companion-water-")) {
      int level=Integer.parseInt(shot.scene.substring(16));
      companionReady=p.isInWater()&&vision==(level>=70)&&breath==(level>=99);
    } else {
      if(!flightStarted&&p.tryToStartFallFlying())flightStarted=true;
      if(shot.scene.equals("companion-robot-boost")&&flightStarted&&!boostAccepted) {
        if(CompanionContent.requestBoost(p)) {
          boostAccepted=true;
          if(CompanionContent.requestBoost(p)||CompanionContent.boostCooldown(p,CompanionVisualSmokeSupport.held(p))!=100)throw new IllegalStateException("Actual robot boost accepted replay or wrong cooldown");
          LogUtils.getLogger().info("CHAO_AUDIT022_BOOST accepted=true immediateReplay=false cooldown=100 velocity={}",p.getDeltaMovement());
        }
      }
      companionReady=p.isFallFlying()&&(!shot.scene.equals("companion-robot-boost")||boostAccepted);
    }
    if(companionReady) {
      expected=Map.of(body.getUUID(),signature(body));
      LogUtils.getLogger().info("CHAO_AUDIT022_COMPANION scene={} water={} vision={} breathing={} elytra={} boost={} run={} swim={} fly={} power={}",shot.scene,p.isInWater(),vision,breath,p.isFallFlying(),CompanionContent.boostCooldown(p,CompanionVisualSmokeSupport.held(p)),body.state().levels[2],body.state().levels[0],body.state().levels[1],body.state().levels[3]);
    }
  }
  private static boolean inspectionScene(String scene) {
    return scene.startsWith("chart-")||scene.startsWith("lifecycle-stats-")||scene.startsWith("grade-audit-");
  }
  private static String inspectionStats(ChaoEntity c) {
    var s=c.state();return Arrays.toString(s.points)+Arrays.toString(s.levels)+Arrays.toString(s.progress)+Arrays.toString(s.grades);
  }
  private static boolean clearFixtureParticles(String scene) {
    return scene.startsWith("lifecycle-")&&!inspectionScene(scene)||scene.equals("motion-chaos-orbit")
      ||scene.startsWith("forms-")||scene.startsWith("growth-")||scene.startsWith("mixed-")
      ||scene.startsWith("opposite-")||scene.startsWith("reverse-")||scene.startsWith("baby-")
      ||scene.startsWith("theme")||scene.startsWith("proportion-")||scene.startsWith("eggaudit-");
  }
  private static void trackWalker(ServerPlayer p,MinecraftServer server) {
    if(subject==null||!(server.overworld().getEntity(subject) instanceof ChaoEntity c))return;
    double y=Math.max(64.05,c.getY()+.05),distance=6.2;
    p.stopFallFlying();p.setSwimming(false);p.setPose(Pose.STANDING);p.setDeltaMovement(Vec3.ZERO);
    float pitch=(float)Math.toDegrees(Math.atan2(y+1.62-(c.getY()+.85),distance));
    p.teleportTo(server.overworld(),c.getX(),y,c.getZ()-distance,0,pitch);
    observerPosition=new Vec3(c.getX(),y,c.getZ()-distance);
  }
  private static ChaoState themeState(String id,int alignment,int primary,float strength,boolean child) {
    var s=state(0,true,false,0);s.variant=id;s.adult=!child;s.align=alignment==0?0:alignment==1?1:-1;s.primaryType=primary;
    development(s,primary,1);if(child){s.swimFly=1;s.runPower=1;}s.setEvolutionStrength(strength);return s;
  }
  private static int themeIndex(String id) {
    var all=ChaoVariants.all();for(int i=0;i<all.size();i++)if(all.get(i).id().equals(id))return i;
    throw new IllegalStateException("Unknown fixture theme "+id);
  }
  private static boolean auditScene(MinecraftServer server,String scene,Map<UUID,String> waiting) {
    var w=server.overworld();var p=server.getPlayerList().getPlayers().getFirst();
    if(scene.startsWith("themegrid-")) {
      String[] bits=scene.split("-");String id=bits[1];int group=Integer.parseInt(bits[2]);
      if(!ChaoVariants.available(id))throw new IllegalStateException("Themed capture needs identity fixture/mod: "+id);
      for(int row=0;row<3;row++)for(int col=0;col<3;col++) {
        boolean child=group==1&&row==2;int primary=group==0?row:row==0?ChaoState.RUN_TYPE:row==1?ChaoState.POWER_TYPE:ChaoState.NORMAL;
        float age=child?(col==0?0:col==1?.45f:.95f):col*.6f;
        var form=themeState(id,row,primary,age,child);double x=(1-col)*1.45,z=row*1.6;int height=64+row;
        for(int y=64;y<height;y++)w.setBlock(BlockPos.containing(x,y,z),Blocks.POLISHED_ANDESITE.defaultBlockState(),2);
        var c=chao(server,form,x,z,155,false,waiting);c.moveTo(x,height,z,155,0);c.setYHeadRot(155);
        LogUtils.getLogger().info("CHAO_AUDIT022_THEME scene={} row={} age={} child={} alignment={} primary={} geometry={}",scene,row,age,child,row,primary,OfficialChaoRig.geometryKey(form));
      }
      camera(p,server,0,63.8,-6.4,0,3.5f);return true;
    }
    if(scene.startsWith("themeclose-")||scene.startsWith("themeaudit-")) {
      String[] bits=scene.split("-");String id=bits[1];int profile=themeIndex(id)%15,alignment=profile/5,primary=profile%5;String angle=bits[bits.length-1];
      if(bits[0].equals("themeaudit")){alignment=0;primary=bits[2].equals("power")?ChaoState.POWER_TYPE:ChaoState.FLY_TYPE;}
      var c=chao(server,themeState(id,alignment,primary,1.2f,false),0,0,180,false,waiting);subject=c.getUUID();view(p,server,angle,2.85);return true;
    }
    if(scene.startsWith("catalog-all-")) {
      inventoryPage=ItemEggVisualSmokeSupport022.prepareInventory(p,Integer.parseInt(scene.substring(12)));return true;
    }
    if(scene.startsWith("eggaudit-")) {
      var eggs=ItemEggVisualSmokeSupport022.prepareEggScene(p,scene);for(var e:eggs)waiting.put(e.getUUID(),signature(e));
      if(eggs.size()==1){subject=eggs.getFirst().getUUID();view(p,server,"oblique",2.4);}
      else {double distance=scene.contains("themes")?7.8:8.2;float pitch=(float)Math.toDegrees(Math.atan2(69.12-64.6,distance));camera(p,server,0,67.5,-distance,0,pitch);}
      return true;
    }
    if(scene.startsWith("lifecycle-")||scene.startsWith("grade-audit-")) {
      var actor=LifecycleVisualSmokeSupport022.buildStage(server,scene);actor.moveTo(0,64,0,180,0);subject=actor.getUUID();
      w.getEntitiesOfClass(ItemEntity.class,AREA).forEach(Entity::discard);
      if(actor instanceof ChaoEntity c){c.setYHeadRot(180);c.setInvulnerable(true);c.sync();}
      waiting.put(actor.getUUID(),signature(actor));view(p,server,"front",actor instanceof ChaoEggEntity?2.4:3.1);
      if(actor instanceof ChaoEntity c)LogUtils.getLogger().info("CHAO_AUDIT022_LIFECYCLE scene={} adult={} dead={} cocoon={} rebirth={} points={} levels={} grades={}",scene,c.state().adult,c.state().dead,c.isCocooned(),c.isRebirthCocoon(),Arrays.toString(c.state().points),Arrays.toString(c.state().levels),Arrays.toString(c.state().grades));
      return true;
    }
    if(scene.startsWith("proportion-")) {
      String kind=scene.substring(11);int count=kind.equals("special")?4:3;
      for(int i=0;i<count;i++) {
        var form=state(0,true,false,0);form.setEvolutionStrength(0);
        if(kind.equals("hero")){form.align=1;form.primaryType=new int[]{0,2,4}[i];}
        else if(kind.equals("dark")){form.align=-1;form.primaryType=new int[]{0,1,3}[i];}
        else if(kind.equals("chaos")){form.align=i==0?0:i==1?1:-1;form.immortal=true;}
        else {form.character=new String[]{"tails","amy","knuckles","omochao"}[i];form.immortal=!form.character.equals("omochao");}
        chao(server,form,(count-1)*.72-i*1.44,0,180,false,waiting);
      }
      view(p,server,"front",count==4?6:4.8);return true;
    }
    if(scene.startsWith("merchant-audit-")) {
      var trader=MarketContent.TRADER.get().create(w);if(trader==null)throw new IllegalStateException("Missing trader type");
      trader.setNoAi(true);trader.setInvulnerable(true);trader.moveTo(0,64,0,180,0);trader.setYHeadRot(180);w.addFreshEntity(trader);
      waiting.put(trader.getUUID(),signature(trader));subject=trader.getUUID();view(p,server,scene.endsWith("side")?"side":"front",3.1);return true;
    }
    return false;
  }
  private static void setup(MinecraftServer server,Shot shot) {
    var w=server.overworld();var p=server.getPlayerList().getPlayers().getFirst();
    if(shot.frame>0) {
      if(shot.scene.equals("motion-accessory-walk")||shot.scene.equals("motion-amy-walk"))trackWalker(p,server);
      if(shot.scene.equals("motion-gradual")||shot.scene.equals("motion-secondary")) {
        if(subject==null||!(w.getEntity(subject) instanceof ChaoEntity c))throw new IllegalStateException("Missing live evolution subject");
        if(shot.scene.equals("motion-gradual"))c.state().setEvolutionStrength(shot.frame*1.2f/(shot.total-1));
        else c.state().swimFly=1-2f*shot.frame/(shot.total-1);
        c.sync();expected=Map.of(c.getUUID(),signature(c));
      }
      if(shot.scene.equals("motion-chaos-orbit")) {
        double a=shot.frame*2*Math.PI/shot.total;camera(p,server,Math.sin(a)*3.6,63.4,-Math.cos(a)*3.6,(float)Math.toDegrees(a),8);
      }
      return;
    }
    routes=Map.of();subject=routePet=null;inventoryPage=null;expectedInspectionStats=null;GardenConfig.MONSTER_EVOLUTION.set(false);GardenConfig.NATURAL_DEATH.set(true);
    if(AccessoriesBridge.available())CompanionVisualSmokeSupport.clear(p);
    companionScene="";companionReady=false;flightStarted=boostAccepted=waterSeen=false;observerPosition=null;recoveredOutput=null;
    p.stopFallFlying();p.setSwimming(false);p.setPose(Pose.STANDING);
    w.getGameRules().getRule(GameRules.RULE_DOBLOCKDROPS).set(false,server);
    w.getGameRules().getRule(GameRules.RULE_DAYLIGHT).set(false,server);
    w.getGameRules().getRule(GameRules.RULE_DOMOBSPAWNING).set(false,server);
    w.setDayTime(6000);w.setWeatherParameters(6000,0,false,false);
    if(stage==0)for(int x=-20;x<=20;x++)for(int z=-10;z<=24;z++) {
      w.setBlock(new BlockPos(x,63,z),Blocks.GRASS_BLOCK.defaultBlockState(),2);
      for(int y=64;y<=78;y++)w.setBlock(new BlockPos(x,y,z),Blocks.AIR.defaultBlockState(),2);
    }
    w.getEntitiesOfClass(Entity.class,AREA,e->!(e instanceof ServerPlayer)).forEach(Entity::discard);
    for(int x=-9;x<=9;x++)for(int z=-6;z<=10;z++)for(int y=64;y<=74;y++)w.setBlock(new BlockPos(x,y,z),Blocks.AIR.defaultBlockState(),2);
    p.getInventory().clearContent();p.getInventory().selected=8;p.setGameMode(GameType.SPECTATOR);
    p.setInvulnerable(true);p.setNoGravity(true);p.setDeltaMovement(Vec3.ZERO);p.fallDistance=0;
    var waiting=new LinkedHashMap<UUID,String>();var goals=new LinkedHashMap<UUID,Vec3>();String scene=shot.scene;
    view(p,server,"front",3.8);
    if(auditScene(server,scene,waiting)) {}
    else if(scene.startsWith("forms-")) {
      String[] part=scene.split("-");int align=Integer.parseInt(part[1]),primary=Integer.parseInt(part[2]);
      for(int row=0;row<3;row++)for(int col=0;col<3;col++) {
        var form=state(0,true,false,0);form.align=align==0?0:align==1?1:-1;form.primaryType=primary;form.setEvolutionStrength(1.2f);form.swimFly=row-1;form.runPower=col-1;
        double x=(1-col)*1.45,z=row*1.6;int height=64+row;
        for(int y=64;y<height;y++)w.setBlock(BlockPos.containing(x,y,z),Blocks.POLISHED_ANDESITE.defaultBlockState(),2);
        var c=chao(server,form,x,z,155,false,waiting);c.moveTo(x,height,z,155,0);c.setYHeadRot(155);
      }
      camera(p,server,0,63.8,-6.4,0,3.5f);
    } else if(scene.startsWith("growth-")||scene.startsWith("mixed-")||scene.startsWith("reverse-")||scene.startsWith("opposite-")||scene.startsWith("baby-")) {
      String[] part=scene.split("-");boolean baby=part[0].equals("baby");
      int align=baby?0:Integer.parseInt(part[1]),primary=Integer.parseInt(part[baby?1:2]);
      String angle=part[part.length-1];boolean side=angle.equals("side");
      int count=part[0].equals("opposite")?2:3;
      for(int i=0;i<count;i++) {
        var s=state(0,true,false,0);s.adult=!baby;s.align=align==0?0:align==1?1:-1;s.primaryType=primary;
        float strength=baby?(i==0?0:i==1?.45f:.95f):(i==0?0:i==1?.6f:1.2f);
        if(part[0].equals("reverse")) {s.primaryType=ChaoState.FLY_TYPE;s.swimFly=i-1;s.runPower=0;strength=1.2f;}
        else if(part[0].equals("opposite")) {development(s,primary==1?2:primary==2?1:primary==3?4:3,i);strength=i==0?0:1.2f;}
        else if(part[0].equals("mixed")) {int combination=Integer.parseInt(part[3]);s.swimFly=combination<2?-1:1;s.runPower=combination%2==0?-1:1;}
        else development(s,primary,1);
        s.setEvolutionStrength(strength);
        double at=(count==2?.7:1.4)-i*1.4;
        chao(server,s,side?0:at,side?at:0,180,false,waiting);
        LogUtils.getLogger().info("CHAO_AUDIT022_EVOLUTION scene={} column={} alignment={} first={} secondary={} strength={} swimFly={} runPower={}",scene,i,s.lockedAlignment(),s.lockedPrimaryType(),s.secondaryFormKey(),s.evolutionStrength(),s.swimFly,s.runPower);
      }
      if(side)camera(p,server,4.8,63.15,0,90,5);else camera(p,server,0,63.15,-4.8,0,5);
    } else if(scene.startsWith("companion-")) {
      var s=state(0,true,false,0);s.energy=100;s.hunger=100;
      boolean water=scene.startsWith("companion-water-")||scene.equals("companion-land-cleanup");
      if(water) {
        int level=scene.equals("companion-land-cleanup")?99:Integer.parseInt(scene.substring(16));s.levels[ChaoState.SWIM]=level;
        for(int x=-2;x<=2;x++)for(int z=-2;z<=2;z++)for(int y=64;y<=67;y++)w.setBlock(new BlockPos(x,y,z),Blocks.WATER.defaultBlockState(),2);
        for(int x=-3;x<=3;x++)for(int z=-3;z<=3;z++)if(Math.abs(x)==3||Math.abs(z)==3)for(int y=64;y<=67;y++)w.setBlock(new BlockPos(x,y,z),Blocks.GLASS.defaultBlockState(),2);
        p.setGameMode(GameType.SURVIVAL);p.setNoGravity(true);p.teleportTo(w,0,64.15,0,180,0);
      } else {
        s.levels[ChaoState.FLY]=99;
        if(scene.equals("companion-robot-boost")){s.character="omochao";s.immortal=false;OmochaoRules.initialize(s,state(0,true,false,0),"Aster",5);}
        p.setGameMode(GameType.SURVIVAL);p.setNoGravity(false);p.teleportTo(w,0,75,8,180,0);p.setDeltaMovement(0,-.05,-.1);
      }
      p.getAbilities().flying=false;p.getAbilities().mayfly=false;p.onUpdateAbilities();
      CompanionVisualSmokeSupport.equip(p,s);companionScene=scene;
    } else if(scene.startsWith("chart-robot-")) {
      int generation=scene.startsWith("chart-robot-family-")?Integer.parseInt(scene.substring(19)):0;
      var pair=robotGeneration(server,generation,waiting);subject=pair.robot.getUUID();view(p,server,"front",3.8);
    } else if(scene.startsWith("robot-generation-")) {
      var pair=robotGeneration(server,Integer.parseInt(scene.substring(17)),waiting);subject=pair.robot.getUUID();camera(p,server,-3.4,63.6,3.4,225,11);
    } else if(scene.equals("robot-component-drop")||scene.equals("robot-heart-drop")) {
      var pair=robotGeneration(server,0,new LinkedHashMap<>());var robot=pair.robot;var creator=pair.creator;
      boolean heart=scene.equals("robot-heart-drop");robot.state().happiness=heart?100:0;robot.state().adultAgeTicks=19;
      var transition=OmochaoRules.tickRobot(robot.state(),20);
      if(transition!=(heart?ChaoState.Transition.REINCARNATED:ChaoState.Transition.DIED)||!OmochaoLifecycle.finish(robot,transition))throw new IllegalStateException("Actual robot terminal output failed");
      var items=w.getEntitiesOfClass(ItemEntity.class,AREA,item->item.getItem().is(ChaoGarden.item(heart?"robo_heart":"robot_component")));
      if(items.size()!=1)throw new IllegalStateException("Terminal robot output must be one real item");recoveredOutput=items.getFirst().getUUID();
      waiting.put(creator.getUUID(),signature(creator));camera(p,server,-2.2,63.0,2.2,225,16);
    } else if(scene.equals("catalog-robot-parts")) {
      var pair=robotGeneration(server,0,new LinkedHashMap<>());
      p.getInventory().setItem(9,new ItemStack(ChaoGarden.item("omochao_parts_box")));
      p.getInventory().setItem(10,new ItemStack(ChaoGarden.item("robot_component")));
      p.getInventory().setItem(11,CweRobotPartItem.heart(pair.robot.state()));
      p.getInventory().setItem(12,new ItemStack(ChaoGarden.item("fly_drive")));
    } else if(scene.startsWith("chart-")) {
      var profile=state(4,false,true,0);profile.hunger=58;profile.energy=76;profile.animalMask=(1<<21)|(1<<22)|(1<<24);
      for(int i=0;i<ChaoPersonality.COUNT;i++)profile.setPersonalitySlider(i,(i%3-1)*80);
      for(int i=0;i<ChaoEmotion.COUNT;i++)profile.setEmotion(i,(i+1)*ChaoEmotion.maximum(i)/20);
      String[] names={"Mochi","Luna","Sora","Poppy","Aster","Café"};
      for(int i=0;i<6;i++) {
        var a=ChaoState.egg(i%2==0?2:4,i%2==1,i%2==0,i%2==0?1:4,new Random(20001+i));
        String k=i<2?"family.parent."+i:"family.grandparent."+(i-2);profile.setTrait(k+".id",a.lineageId());profile.setTrait(k+".dna",a.dnaSummary());
        if(!scene.equals("chart-family-legacy"))profile.setTrait(k+".name",ChaoState.encodeFamilyName(names[i]));
        if(scene.equals("chart-family-legacy")&&i==0){var relative=chao(server,a,6,6,180,false,waiting);relative.setCustomName(Component.literal("Mochi"));}
      }
      for(int i=0;i<6;i++)if(i%2==0)profile.setIllness(i,-75);
      if(scene.equals("chart-status")) {
        profile.adult=false;profile.points[ChaoState.FLY]=200;profile.swimFly=1;profile.runPower=0;profile.setEvolutionStrength(.75f);
      }
      if(scene.startsWith("chart-growth-")) {
        int maturity=Integer.parseInt(scene.substring(13));profile.primaryType=ChaoState.FLY_TYPE;profile.swimFly=-1;profile.runPower=1;profile.setEvolutionStrength(maturity*.6f);
      }
      if(scene.startsWith("chart-companion-")) {
        String[] parts=scene.split("-");int stat=Integer.parseInt(parts[2]),level=Integer.parseInt(parts[3]);profile.levels[stat]=level;profile.points[stat]=level*30;profile.cureAllIllnesses();
      }
      var c=chao(server,profile,0,0,180,false,waiting);c.setCustomName(Component.literal("Nova"));subject=c.getUUID();
      w.setBlock(new BlockPos(2,64,0),DoctorContent.DOCTOR_STATION.get().defaultBlockState(),3);
      w.setBlock(new BlockPos(-2,64,0),DoctorContent.LESSON_DESK.get().defaultBlockState(),3);
      p.setGameMode(GameType.SURVIVAL);p.getInventory().setItem(0,new ItemStack(ChaoGarden.item("chao_medicine"),2));p.getInventory().setItem(1,new ItemStack(Items.EMERALD_BLOCK,64));
      String[] gear={"baseball_cap_accessory","round_glasses_accessory","cheese_bowtie_accessory","blue_hoodie_accessory"};
      for(int i=0;i<gear.length;i++)p.getInventory().setItem(9+i,new ItemStack(ChaoGarden.item(gear[i])));
      if(scene.equals("chart-lessons")) {
        c.state().cureAllIllnesses();c.state().sleeping=false;c.state().hunger=80;
        for(var l:ChaoCareServices.LESSONS)c.state().setTraitInt("learned."+l.id(),l.id().equals("song")||l.id().equals("drawing")?3:1);
      }
      if(scene.equals("chart-wardrobe")) {
        c.state().cureAllIllnesses();for(int i=9;i<=12;i++)if(!ChaoCareServices.equip(p,c,i))throw new IllegalStateException("Actual equipment transaction failed");
      }
      c.sync();waiting.put(c.getUUID(),signature(c));view(p,server,"front",3.8);
    } else if(scene.startsWith("accessoryspecial-")) {
      String[] part=scene.split("-");var s=state(0,true,false,0);s.immortal=true;
      if(part[1].startsWith("chaos")){int a=Integer.parseInt(part[1].substring(5));s.align=a==0?0:a==1?1:-1;}
      else {s.character=part[1];if(s.character.equals("omochao"))s.immortal=false;}
      var c=chao(server,s,0,0,180,false,waiting);gear(c);subject=c.getUUID();view(p,server,part[2],3.1);
    } else if(scene.startsWith("accessory-")) {
      String[] part=scene.split("-");boolean child=part[1].equals("child");int align=child?0:Integer.parseInt(part[1]),primary=child?-1:Integer.parseInt(part[2]);
      var s=state(0,true,false,0);s.adult=primary>=0;s.align=align==0?0:align==1?1:-1;s.primaryType=Math.max(0,primary);
      if(primary>=0){s.swimFly=1;s.runPower=1;s.setEvolutionStrength(1.2f);}else s.setEvolutionStrength(0);
      var c=chao(server,s,0,0,180,false,waiting);gear(c);subject=c.getUUID();view(p,server,part[child?2:3],3.1);
    } else if(scene.startsWith("robot-coat-")) {
      int index=Integer.parseInt(scene.substring(11));var builder=chao(server,coat(index),.85,0,180,false,waiting);builder.setCustomName(Component.literal("Builder"));
      var robot=assemble(server,builder,4,waiting);robot.moveTo(-.85,64,0,180,0);robot.setYHeadRot(180);subject=robot.getUUID();view(p,server,"front",4.7);
    } else if(scene.startsWith("robot-stage-")) {
      // Frozen construction intentionally skips client interpolation. Spawn
      // the actual prototype in its final fixture position and face its native
      // initial yaw, then move only the ordinary builder into the paired view.
      int boxes=Integer.parseInt(scene.substring(12));var builder=chao(server,state(4,true,true,0),.05,0,0,false,waiting);
      var robot=assemble(server,builder,boxes,waiting);builder.moveTo(-.85,64,0,0,0);builder.setYHeadRot(0);subject=robot.getUUID();
      camera(p,server,-3.4,63.6,3.4,225,11);
    } else if(scene.startsWith("amy-")) {
      var s=state(6,true,false,0);s.character="amy";s.immortal=true;var c=chao(server,s,0,0,180,false,waiting);subject=c.getUUID();view(p,server,scene.substring(4),3.1);
    } else if(scene.startsWith("chaos-")) {
      String[] part=scene.split("-");int align=Integer.parseInt(part[1]);var s=state(0,true,false,0);s.immortal=true;s.align=align==0?0:align==1?1:-1;
      var c=chao(server,s,0,0,180,false,waiting);subject=c.getUUID();
      // Opaque checker surfaces behind the model expose real partial alpha at
      // hand, foot and crest tips, including from the reverse camera.
      for(int x=-3;x<=3;x++)for(int z=-3;z<=3;z++) {
        w.setBlock(new BlockPos(x,63,z),(Math.floorMod(x+z,2)==0?Blocks.WHITE_CONCRETE:Blocks.GRAY_CONCRETE).defaultBlockState(),2);
        if(Math.abs(x)==3||Math.abs(z)==3)for(int y=64;y<=66;y++)w.setBlock(new BlockPos(x,y,z),(Math.floorMod(x+z+y,2)==0?Blocks.WHITE_CONCRETE:Blocks.GRAY_CONCRETE).defaultBlockState(),2);
      }
      view(p,server,part[2],2.65);
    } else if(scene.startsWith("chaoscoat-")) {
      int index=Integer.parseInt(scene.substring(10));var s=coat(index);s.immortal=true;var c=chao(server,s,0,0,180,false,waiting);subject=c.getUUID();view(p,server,"oblique",3.2);
    } else if(scene.equals("characters-retained")) {
      String[] names={"tails","amy","knuckles","omochao"};
      for(int i=0;i<names.length;i++){var s=state(0,true,false,0);s.character=names[i];s.immortal=!names[i].equals("omochao");chao(server,s,(1.5-i)*1.3,0,180,false,waiting);}
      view(p,server,"front",6.1);
    } else if(scene.equals("motion-gradual")||scene.equals("motion-secondary")) {
      var s=state(0,true,false,0);s.primaryType=ChaoState.FLY_TYPE;s.swimFly=1;
      s.setEvolutionStrength(scene.equals("motion-gradual")?0:1.2f);
      subject=chao(server,s,0,0,180,false,waiting).getUUID();view(p,server,"oblique",3.1);
    } else if(scene.equals("motion-accessory-walk")||scene.equals("motion-amy-walk")) {
      var s=state(0,true,false,0);if(scene.equals("motion-amy-walk")){s.character="amy";s.immortal=true;}else{s.primaryType=3;s.runPower=-1;}
      var c=chao(server,s,-1.9,0,90,true,waiting);if(!s.character.equals("amy"))gear(c);subject=c.getUUID();goals.put(subject,new Vec3(2.3,64,0));trackWalker(p,server);
    } else if(scene.equals("motion-robot")) {
      var builder=chao(server,coat(3),.85,0,180,false,waiting);var robot=assemble(server,builder,4,waiting);robot.moveTo(-.85,64,0,180,0);robot.setYHeadRot(180);subject=robot.getUUID();view(p,server,"oblique",4.7);
    } else if(scene.equals("motion-chaos-orbit")) {
      var s=state(0,true,false,0);s.immortal=true;subject=chao(server,s,0,0,180,false,waiting).getUUID();
      for(int x=-3;x<=3;x++)for(int z=-3;z<=3;z++)w.setBlock(new BlockPos(x,63,z),(Math.floorMod(x+z,2)==0?Blocks.WHITE_CONCRETE:Blocks.GRAY_CONCRETE).defaultBlockState(),2);
      view(p,server,"front",3.6);
    } else throw new IllegalStateException("Unknown refinement scene "+scene);
    if(inspectionScene(scene)&&subject!=null&&w.getEntity(subject) instanceof ChaoEntity c)expectedInspectionStats=inspectionStats(c);
    expected=Map.copyOf(waiting);routes=Map.copyOf(goals);p.inventoryMenu.broadcastChanges();
  }
  private static void moveMouse(Minecraft mc,double x,double y) {
    GLFW.glfwSetCursorPos(mc.getWindow().getWindow(),x,y);
    try{var m=net.minecraft.client.MouseHandler.class.getDeclaredMethod("onMove",long.class,double.class,double.class);m.setAccessible(true);m.invoke(mc.mouseHandler,mc.getWindow().getWindow(),x,y);}catch(Exception e){throw new IllegalStateException(e);}
  }
}
