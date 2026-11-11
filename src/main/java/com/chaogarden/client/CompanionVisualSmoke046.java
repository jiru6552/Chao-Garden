package com.chaogarden.client;

import com.chaogarden.ChaoGarden;
import com.chaogarden.companion.AccessoriesBridge;
import com.chaogarden.companion.CompanionContent;
import com.chaogarden.companion.CarryRules;
import com.chaogarden.core.ChaoState;
import com.chaogarden.entity.ChaoEntity;
import com.chaogarden.item.ChaoCarrierItem;
import com.mojang.logging.LogUtils;
import java.util.Map;
import java.util.UUID;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.AccessibilityOnboardingScreen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.WorldDataConfiguration;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/** Disposable actual-slot study, or explicitly labelled attachment-only render study; excluded from production JARs. */
@EventBusSubscriber(modid=ChaoGarden.MODID,value=Dist.CLIENT)
public final class CompanionVisualSmoke046 {
  private static final String[] CASES={"baby_native_elytra","adult_maximum_companion","adult_finite_companion",
      "baby_no_flight_and_creative","baby_swimming","baby_ground_return","hero_companion"};
  private static final String[] CARRY_CASES={"baby_native_elytra","adult_creative_flight","adult_native_elytra",
      "baby_creative_transition","baby_swimming","baby_ground_return","hero_native_elytra"};
  private static boolean started,setup;
  private static int ticks,shots;
  private static volatile int phase=-1,elapsed;
  private static long began;
  private static Container slot;
  private static Object accessoryContainer;
  private static Class<?> containerType;
  private static UUID chaoId;
  private static ArmorStand observer;
  private static boolean renderOnly(){return Boolean.getBoolean("chao_garden.companion046CarryStudy");}
  private static boolean enabled(){return Boolean.getBoolean("chao_garden.companion046Study")||renderOnly();}
  private static void require(boolean result,String message){if(!result)throw new IllegalStateException("COMPANION046 "+message);}
  private static ChaoEntity body(MinecraftServer server){
    var entity=CompanionContent.findEntity(server,chaoId);require(entity instanceof ChaoEntity,"actual slot lost its body");return (ChaoEntity)entity;
  }
  private static void changed(){try{containerType.getMethod("markChanged",boolean.class).invoke(accessoryContainer,false);}catch(Exception error){throw new RuntimeException(error);}}
  private static void equip(ServerPlayer player){
    if(renderOnly()){
      // This fixture exercises real synced carry flags and the full native layer
      // without pretending to validate an unavailable optional accessory mod.
      var creature=ChaoGarden.CHAO.get().create(player.serverLevel());var state=new ChaoState();state.energy=100;
      creature.owner=player.getUUID();creature.setState(state);creature.setNoAi(true);creature.setInvulnerable(true);
      creature.moveTo(player.position());player.serverLevel().addFreshEntity(creature);chaoId=creature.getUUID();return;
    }
    require(AccessoriesBridge.available(),"requires real optional Accessories and owo runtime jars");
    try{
      var state=new ChaoState();state.energy=100;state.hunger=100;state.points[ChaoState.RUN]=667;state.points[ChaoState.SWIM]=100;
      var creature=ChaoGarden.CHAO.get().create(player.serverLevel());creature.owner=player.getUUID();creature.setState(state);
      var carrier=new ItemStack(ChaoGarden.item("chao_carrier"));require(ChaoCarrierItem.capture(carrier,creature,player),"capture failed");
      var capabilityType=Class.forName("io.wispforest.accessories.api.AccessoriesCapability");
      containerType=Class.forName("io.wispforest.accessories.api.AccessoriesContainer");
      var capability=capabilityType.getMethod("get",LivingEntity.class).invoke(null,player);
      accessoryContainer=((Map<?,?>)capabilityType.getMethod("getContainers").invoke(capability)).get("chao_companion");
      require(accessoryContainer!=null,"missing actual companion slot");
      slot=(Container)containerType.getMethod("getAccessories").invoke(accessoryContainer);slot.setItem(0,carrier);changed();chaoId=ChaoCarrierItem.storedUUID(carrier);
    }catch(ReflectiveOperationException error){throw new RuntimeException(error);}
  }
  private static void next(MinecraftServer server,int next){
    phase=next;elapsed=0;began=server.overworld().getGameTime();var player=server.getPlayerList().getPlayers().getFirst();
    player.stopFallFlying();player.getAbilities().flying=false;player.getAbilities().mayfly=false;player.onUpdateAbilities();player.setNoGravity(true);player.setDeltaMovement(Vec3.ZERO);
    player.teleportTo(.5,next==4||next==5?100:110,.5);player.setOnGround(false);player.setXRot(0);player.getFoodData().setFoodLevel(20);
    var creature=body(server);var state=creature.state();state.adult=next==1||next==2||next==6;state.energy=100;
    state.align=next==6?1:0;state.primaryType=next==6?2:0;state.levels[ChaoState.FLY]=next==2?0:99;creature.sync();
    player.setItemSlot(EquipmentSlot.CHEST,new ItemStack(next==0||renderOnly()&&(next==2||next==6)?Items.ELYTRA:Items.DIAMOND_CHESTPLATE));
    player.getPersistentData().remove(CompanionContent.AIR_STARTED);player.getPersistentData().putInt(CompanionContent.AIR_USED,0);
    if(next==4){
      for(var at:BlockPos.betweenClosed(-8,97,-8,8,104,16))server.overworld().setBlock(at,Blocks.WATER.defaultBlockState(),3);
      player.setNoGravity(false);player.setSprinting(true);
    }
    if(next==5){
      for(var at:BlockPos.betweenClosed(-8,97,-8,8,104,16))server.overworld().setBlock(at,at.getY()==99?Blocks.SMOOTH_STONE.defaultBlockState():Blocks.AIR.defaultBlockState(),3);
      player.setNoGravity(false);player.teleportTo(.5,100,.5);player.setOnGround(true);
    }
    LogUtils.getLogger().info("COMPANION046_VISUAL_CASE {} {} mode={}",next,(renderOnly()?CARRY_CASES:CASES)[next],renderOnly()?"attachment-render-only":"actual-Accessories-slot");
  }
  @SubscribeEvent public static void server(ServerTickEvent.Post event){
    if(!enabled()||phase<0||phase>=CASES.length)return;var server=event.getServer();elapsed=(int)(server.overworld().getGameTime()-began);
    var player=server.getPlayerList().getPlayers().getFirst();var creature=body(server);
    if(renderOnly()){
      if(phase==1&&elapsed==20){player.getAbilities().flying=true;player.getAbilities().mayfly=true;player.onUpdateAbilities();}
      int position=CarryRules.position(creature.state(),player.isFallFlying()||player.getAbilities().flying,player.isInWater()||player.isSwimming());
      creature.setCompanionCarry(player,position);creature.setPos(player.getX(),player.getY()+.35,player.getZ());creature.setDeltaMovement(Vec3.ZERO);
    }
    if(elapsed==40&&(phase==0||!renderOnly()&&phase==1||phase==2||phase==6)) {
      require(player.isFallFlying(),CASES[phase]+" real jump input did not activate server flight");
      require(creature.isCompanionGripping(),CASES[phase]+" did not attach to keeper");
      require(renderOnly()||phase==0||player.getItemBySlot(EquipmentSlot.CHEST).is(Items.DIAMOND_CHESTPLATE),"companion replaced armor");
    }
    if(!renderOnly()&&phase==2&&elapsed==90)require(!player.isFallFlying()&&!CompanionContent.flightEligible(player),"finite glide did not expire");
    if(phase==3&&elapsed==40)require(!player.isFallFlying()&&!CompanionContent.flightEligible(player),"baby granted flight");
    if(phase==3&&elapsed==55){player.getAbilities().flying=true;player.getAbilities().mayfly=true;player.onUpdateAbilities();}
    if(phase==3&&elapsed==80)require(creature.isCompanionGripping(),"baby did not grip during other flight");
    if(phase==4&&elapsed==70)require(player.isInWater()&&creature.isCompanionGripping(),"baby did not attach in actual water");
    if(phase==5&&elapsed==70)require(creature.companionCarryPosition()==1,"ground return did not restore head perch");
    if(elapsed>=115){if(phase+1<CASES.length)next(server,phase+1);else phase=CASES.length;}
  }
  @SubscribeEvent public static void restoreInputCamera(ClientTickEvent.Pre event){
    if(!enabled()||!renderOnly())return;var mc=Minecraft.getInstance();
    // LocalPlayer only sends movement and applies controls while it owns the
    // camera. Restore it for the entire native tick; use the observer solely
    // between ticks, when rendering, so this fixture cannot alter physics.
    if(mc.player!=null&&mc.getCameraEntity()!=mc.player)mc.setCameraEntity(mc.player);
  }
  private static void inspectionCamera(Minecraft mc){
    if(observer==null||observer.level()!=mc.level){observer=new ArmorStand(mc.level,0,0,0);observer.setInvisible(true);observer.setNoGravity(true);}
    var keeper=mc.player;var forward=Vec3.directionFromRotation(0,keeper.yBodyRot);var side=new Vec3(forward.z,0,-forward.x);
    boolean swimming=keeper.getSwimAmount(1)>.5f,gliding=keeper.isFallFlying();
    boolean head=false;for(var entity:mc.level.entitiesForRendering())if(entity instanceof ChaoEntity c&&c.getUUID().equals(chaoId)){head=c.companionCarryPosition()==CarryRules.HEAD;break;}
    var focus=keeper.position().add(forward.scale(gliding?.85:swimming?.25:0)).add(0,gliding?.35:swimming?.5:head?1.95:1.35,0);
    boolean threeQuarter=(elapsed/30)%2==0;
    var eye=focus.add(forward.scale(threeQuarter?-2.0:-.1)).add(side.scale(threeQuarter?1.0:1.8)).add(0,threeQuarter?.55:.2,0);
    var toward=focus.subtract(eye);float yaw=(float)(Math.toDegrees(Math.atan2(toward.z,toward.x))-90);
    float pitch=(float)-Math.toDegrees(Math.atan2(toward.y,Math.hypot(toward.x,toward.z)));
    observer.moveTo(eye.x,eye.y-observer.getEyeHeight(),eye.z,yaw,pitch);
    observer.yRotO=yaw;observer.xRotO=pitch;observer.setYHeadRot(yaw);observer.yHeadRotO=yaw;
    // Match the keeper's interpolation, keeping fast flight framed at every partial tick.
    observer.xo=observer.xOld=observer.getX()+keeper.xo-keeper.getX();
    observer.yo=observer.yOld=observer.getY()+keeper.yo-keeper.getY();
    observer.zo=observer.zOld=observer.getZ()+keeper.zo-keeper.getZ();
    mc.options.setCameraType(CameraType.FIRST_PERSON);mc.setCameraEntity(observer);
  }
  @SubscribeEvent public static void client(ClientTickEvent.Post event){
    if(!enabled())return;var mc=Minecraft.getInstance();ticks++;
    if(mc.screen instanceof AccessibilityOnboardingScreen)mc.setScreen(new TitleScreen());
    if(!started&&mc.screen instanceof TitleScreen){
      started=true;mc.options.renderDistance().set(3);mc.options.simulationDistance().set(5);mc.options.framerateLimit().set(30);mc.options.fov().set(45);mc.options.pauseOnLostFocus=false;mc.options.hideGui=true;
      mc.createWorldOpenFlows().createFreshLevel("Companion046-"+System.currentTimeMillis(),new LevelSettings("Companion046",GameType.SURVIVAL,false,Difficulty.PEACEFUL,true,new GameRules(),WorldDataConfiguration.DEFAULT),new net.minecraft.world.level.levelgen.WorldOptions(2546L,false,false),a->a.registryOrThrow(net.minecraft.core.registries.Registries.WORLD_PRESET).getHolderOrThrow(net.minecraft.world.level.levelgen.presets.WorldPresets.FLAT).value().createWorldDimensions(),mc.screen);
    }
    if(!setup&&mc.player!=null&&mc.getSingleplayerServer()!=null){
      setup=true;mc.setScreen(null);mc.options.setCameraType(CameraType.THIRD_PERSON_BACK);ChaoRenderer.suppressNames=true;
      mc.getSingleplayerServer().execute(()->{
        var server=mc.getSingleplayerServer();server.overworld().setDayTime(6000);server.overworld().getGameRules().getRule(GameRules.RULE_DAYLIGHT).set(false,server);
        equip(server.getPlayerList().getPlayers().getFirst());
      });
    }
    if(setup&&phase<0&&mc.getSingleplayerServer()!=null){
      mc.getSingleplayerServer().execute(()->{if(phase<0&&CompanionContent.findEntity(mc.getSingleplayerServer(),chaoId) instanceof ChaoEntity)next(mc.getSingleplayerServer(),0);});
    }
    if(mc.player!=null&&phase>=0&&phase<CASES.length){
      mc.options.keyJump.setDown(phase!=4&&phase!=5&&elapsed>=25&&elapsed<28);
      mc.options.keyUp.setDown(phase==4);mc.options.keySprint.setDown(phase==4);
      mc.player.setXRot(phase==4?12:0);
      if(renderOnly())inspectionCamera(mc);else mc.options.setCameraType(elapsed<65?CameraType.THIRD_PERSON_BACK:CameraType.THIRD_PERSON_FRONT);
      if(elapsed>=20&&ticks%15==0)Screenshot.grab(mc.gameDirectory,String.format("companion046-%d-t%03d-%s-%04d.png",phase,elapsed,renderOnly()?((elapsed/30)%2==0?"threequarter":"side"):"thirdperson",shots++),mc.getMainRenderTarget(),text->{});
    }
    if(phase==CASES.length){require(CompanionBackLayer.renders()>0,"native attached layer never rendered");LogUtils.getLogger().info("COMPANION046_VISUAL_COMPLETE shots={} carryRenders={} mode={}",shots,CompanionBackLayer.renders(),renderOnly()?"attachment-render-only":"actual-Accessories-slot");mc.stop();}
    if(ticks>9000)throw new IllegalStateException("Companion client study timed out");
  }
}
