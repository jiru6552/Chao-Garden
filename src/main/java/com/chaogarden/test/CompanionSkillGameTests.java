package com.chaogarden.test;

import com.chaogarden.ChaoGarden;
import com.chaogarden.companion.*;
import com.chaogarden.core.ChaoState;
import com.chaogarden.core.OmochaoRules;
import com.chaogarden.entity.ChaoEntity;
import com.chaogarden.item.ChaoCarrierItem;
import java.util.Map;
import java.util.Set;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Random;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.protocol.common.ServerboundCustomPayloadPacket;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.network.protocol.game.ServerboundAcceptTeleportationPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Cow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForgeMod;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/** Actual optional-slot, native-elytra and inbound payload checks; identity fixtures cannot supply these callbacks. */
@GameTestHolder(ChaoGarden.MODID)
@PrefixGameTestTemplate(false)
@EventBusSubscriber(modid=ChaoGarden.MODID)
public final class CompanionSkillGameTests {
  private static final BlockPos CENTER=new BlockPos(2,2,2);
  private static final Set<ServerPlayer> PHYSICS=Collections.newSetFromMap(new IdentityHashMap<>());
  record Equipped(ServerPlayer keeper,Container inventory,ItemStack carrier,UUID chao){}
  /** Embedded channels are not in the server network listener's tick list. Run
   * the real vanilla living-player step, without changing any water/flight flags.
   * A real registered connection already does this and must never be ticked twice. */
  @SubscribeEvent public static void physics(ServerTickEvent.Pre event){
    var iterator=PHYSICS.iterator();while(iterator.hasNext()){
      var player=iterator.next();
      if(player.isRemoved()||event.getServer().getPlayerList().getPlayer(player.getUUID())!=player){iterator.remove();continue;}
      if(!event.getServer().getConnection().getConnections().contains(player.connection.getConnection())){
        player.doTick();player.serverLevel().getChunkSource().move(player);
      }
    }
  }
  private static String diagnostic(Equipped e){
    var p=e.keeper();var entity=CompanionContent.findEntity(p.server,e.chao());
    int live=entity instanceof ChaoEntity c?c.state().levels[ChaoState.FLY]:-1;
    return " [y="+p.getY()+",motion="+p.getDeltaMovement()+",ground="+p.onGround()+",water="+p.isInWater()
        +",flight="+p.isFallFlying()+",eligible="+CompanionContent.flightEligible(p)+",liveFly="+live
        +",savedFly="+ChaoCarrierItem.state(e.inventory().getItem(0)).levels[ChaoState.FLY]
        +",used="+p.getPersistentData().getInt(CompanionContent.AIR_USED)
        +",started="+p.getPersistentData().getLong(CompanionContent.AIR_STARTED)+",time="+p.server.overworld().getGameTime()+"]";
  }
  private static ChaoState adult(){var s=ChaoState.egg(4,false,true,0,new Random(211));s.adult=true;s.energy=100;return s;}
  static Equipped equip(GameTestHelper helper,ChaoState state){
    return equip(helper,state,TestConnections.makeServerPlayer(helper));
  }
  static Equipped equip(GameTestHelper helper,ChaoState state,ServerPlayer keeper){
    try{
      var pos=helper.absolutePos(CENTER);
      PHYSICS.add(keeper);
      keeper.teleportTo(pos.getX()+.5,pos.getY()+.1,pos.getZ()+.5);keeper.setNoGravity(true);keeper.getAbilities().flying=false;
      var chao=ChaoGarden.CHAO.get().create(helper.getLevel());chao.owner=keeper.getUUID();chao.setState(state);
      var carrier=new ItemStack(ChaoGarden.item("chao_carrier"));helper.assertTrue(ChaoCarrierItem.capture(carrier,chao,keeper),"Actual-slot fixture could not capture Chao");
      var capabilityType=Class.forName("io.wispforest.accessories.api.AccessoriesCapability");
      var containerType=Class.forName("io.wispforest.accessories.api.AccessoriesContainer");
      var capability=capabilityType.getMethod("get",LivingEntity.class).invoke(null,keeper);
      var containers=(Map<?,?>)capabilityType.getMethod("getContainers").invoke(capability);var container=containers.get("chao_companion");
      helper.assertTrue(container!=null,"Actual Accessories companion container missing");
      var inventory=(Container)containerType.getMethod("getAccessories").invoke(container);inventory.setItem(0,carrier);
      containerType.getMethod("markChanged",boolean.class).invoke(container,false);
      return new Equipped(keeper,inventory,carrier,ChaoCarrierItem.storedUUID(carrier));
    }catch(ReflectiveOperationException e){throw new RuntimeException(e);}
  }
  static ChaoEntity body(GameTestHelper helper,Equipped e){
    var entity=helper.getLevel().getEntity(e.chao());helper.assertTrue(entity instanceof ChaoEntity,"Actual slot did not summon its saved UUID");return (ChaoEntity)entity;
  }
  static void finish(GameTestHelper helper,Equipped e){
    CompanionContent.unequip(e.keeper(),e.inventory().getItem(0));e.inventory().setItem(0,ItemStack.EMPTY);
    e.keeper().server.getPlayerList().remove(e.keeper());helper.succeed();
  }

  @GameTest(template="empty",timeoutTicks=60)
  public static void nativeElytraPreservedAndArmorAloneCannotFly(GameTestHelper helper){
    var keeper=TestConnections.makeServerPlayer(helper);var pos=helper.absolutePos(CENTER);
    PHYSICS.add(keeper);
    keeper.teleportTo(pos.getX()+.5,pos.getY()+6,pos.getZ()+.5);keeper.setNoGravity(true);keeper.setOnGround(false);keeper.getAbilities().flying=false;
    keeper.setItemSlot(EquipmentSlot.CHEST,new ItemStack(Items.DIAMOND_CHESTPLATE));
    helper.assertTrue(!keeper.tryToStartFallFlying(),"Companion flight hook granted flight without any Chao or elytra");
    keeper.setItemSlot(EquipmentSlot.CHEST,new ItemStack(Items.ELYTRA));
    helper.assertTrue(keeper.tryToStartFallFlying(),"Native chest elytra no longer starts normally");
    helper.runAfterDelay(3,()->{
      helper.assertTrue(keeper.isFallFlying(),"Native elytra continuation was broken by the companion hook");
      keeper.stopFallFlying();keeper.setItemSlot(EquipmentSlot.CHEST,new ItemStack(Items.DIAMOND_CHESTPLATE));
      helper.assertTrue(!keeper.tryToStartFallFlying(),"Armor-only player inherited another player's flight permit");
      keeper.server.getPlayerList().remove(keeper);helper.succeed();
    });
  }

  @GameTest(template="empty",timeoutTicks=100)
  public static void actualSlotFlightUsesVanillaPhysicsAndTransitionsToFiniteBudget(GameTestHelper helper){
    if(!AccessoriesBridge.available()){helper.succeed();return;}
    var s=adult();s.levels[ChaoState.FLY]=99;var e=equip(helper,s);
    helper.runAfterDelay(12,()->{
      var c=body(helper,e);var keeper=e.keeper();var pos=helper.absolutePos(CENTER);
      keeper.teleportTo(pos.getX()+.5,pos.getY()+12,pos.getZ()+.5);keeper.setOnGround(false);
      keeper.setItemSlot(EquipmentSlot.CHEST,new ItemStack(Items.DIAMOND_CHESTPLATE));keeper.setDeltaMovement(0,0,.15);
      keeper.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,new ItemStack(Items.DIRT));
      c.offerItem(keeper,net.minecraft.world.InteractionHand.MAIN_HAND);
      helper.assertTrue(c.isHandlingItem(),"Pending-gift flight fixture failed to hold its real item");
      helper.assertTrue(keeper.tryToStartFallFlying(),"MaximumFly actual companion could not start native elytra flight");
      helper.runAfterDelay(4,()->{
        helper.assertTrue(keeper.isFallFlying(),"Vanilla flight update rejected the live companion permit");
        helper.assertTrue(c.isCompanionGripping()&&c.noPhysics&&c.isHandlingItem()&&c.distanceToSqr(keeper)<.5,
            "A pending gift prevented the real body from gripping and following its flying keeper");
        helper.assertTrue(keeper.getItemBySlot(EquipmentSlot.CHEST).is(Items.DIAMOND_CHESTPLATE),"Companion replaced or consumed equipped armor");
        c.state().levels[ChaoState.FLY]=98;c.sync();
        helper.runAfterDelay(3,()->{
          helper.assertTrue(keeper.isFallFlying()&&keeper.getPersistentData().contains(CompanionContent.AIR_STARTED),
              "Level98 did not continue through the real timed elytra path"+diagnostic(e));
          keeper.getPersistentData().putLong(CompanionContent.AIR_STARTED,keeper.server.overworld().getGameTime()-240);
          helper.runAfterDelay(3,()->{
            helper.assertTrue(!keeper.isFallFlying()&&!keeper.tryToStartFallFlying(),"Expired finite permission kept elytra flying"+diagnostic(e));
            keeper.setItemSlot(EquipmentSlot.CHEST,new ItemStack(Items.ELYTRA));
            helper.assertTrue(keeper.tryToStartFallFlying(),"Expired companion budget disabled a real chest elytra");
            helper.runAfterDelay(3,()->{
              helper.assertTrue(keeper.isFallFlying()&&!c.isCompanionGripping(),"Independent chest elytra did not continue without Chao support");finish(helper,e);
            });
          });
        });
      });
    });
  }

  @GameTest(template="empty",timeoutTicks=110)
  public static void actualWaterBonusesPreservePotionsAndClearOnExit(GameTestHelper helper){
    if(!AccessoriesBridge.available()){helper.succeed();return;}
    var s=adult();s.levels[ChaoState.RUN]=1;s.levels[ChaoState.SWIM]=99;var e=equip(helper,s);
    helper.runAfterDelay(12,()->{
      body(helper,e);var player=e.keeper();
      var run=player.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED).getModifier(CompanionContent.RUN_SPEED);
      helper.assertTrue(run!=null&&run.amount()>0&&run.amount()<.01,"Level-one runner did not give a slight bounded boost");
      helper.assertTrue(!player.hasEffect(MobEffects.NIGHT_VISION)&&!player.hasEffect(MobEffects.WATER_BREATHING),"Water-only effects were granted on dry land");
      player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION,1000,0));
      helper.setBlock(CENTER,Blocks.WATER);helper.setBlock(CENTER.above(),Blocks.WATER);
      var pos=helper.absolutePos(CENTER);player.teleportTo(pos.getX()+.5,pos.getY()+.1,pos.getZ()+.5);
      helper.runAfterDelay(5,()->{
        helper.assertTrue(player.isInWater(),"Water context fixture did not enter actual fluid"+diagnostic(e));
        var swim=player.getAttribute(NeoForgeMod.SWIM_SPEED).getModifier(CompanionContent.SWIM_SPEED);
        helper.assertTrue(swim!=null&&Math.abs(swim.amount()-2)<.00001,"MaximumSwim did not give its actual swim attribute bonus");
        helper.assertTrue(player.hasEffect(MobEffects.WATER_BREATHING),"MaximumSwim did not provide water breathing");
        helper.assertTrue(player.getEffect(MobEffects.NIGHT_VISION).getDuration()>900,"Companion replaced or shortened an existing night-vision potion");
        // Removing the external effect while swimming permits the companion's own vision to take over.
        player.removeEffect(MobEffects.NIGHT_VISION);
        helper.runAfterDelay(2,()->{
          helper.assertTrue(player.hasEffect(MobEffects.NIGHT_VISION),"HighSwim did not grant its own water vision");
          player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION,80,0));
          helper.assertTrue(player.getEffect(MobEffects.NIGHT_VISION).getDuration()<=80,"An added shorter potion was swallowed by a companion refresh");
          helper.setBlock(CENTER,Blocks.AIR);helper.setBlock(CENTER.above(),Blocks.AIR);
          player.teleportTo(pos.getX()+.5,pos.getY()+4,pos.getZ()+.5);
          helper.runAfterDelay(5,()->{
            helper.assertTrue(!player.isInWater(),"Exit context fixture still submerged");
            helper.assertTrue(!player.hasEffect(MobEffects.WATER_BREATHING),"Companion water breathing lingered on dry land");
            helper.assertTrue(player.hasEffect(MobEffects.NIGHT_VISION)&&player.getEffect(MobEffects.NIGHT_VISION).getDuration()>60,"Water exit removed a real potion applied during companion support");
            helper.assertTrue(player.getAttribute(NeoForgeMod.SWIM_SPEED).getModifier(CompanionContent.SWIM_SPEED)==null,"Water-speed attribute survived leaving water");
            player.addEffect(new MobEffectInstance(MobEffects.WATER_BREATHING,1000,0));
            CompanionContent.unequip(player,e.inventory().getItem(0));e.inventory().setItem(0,ItemStack.EMPTY);
            helper.assertTrue(player.hasEffect(MobEffects.NIGHT_VISION),"Unequip stripped external night vision");
            helper.assertTrue(player.hasEffect(MobEffects.WATER_BREATHING)&&player.getEffect(MobEffects.WATER_BREATHING).getDuration()>900,"Unequip stripped an external water-breathing potion");
            helper.assertTrue(player.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED).getModifier(CompanionContent.RUN_SPEED)==null,"Land-speed attribute survived unequip");
            player.server.getPlayerList().remove(player);helper.succeed();
          });
        });
      });
    });
  }

  @GameTest(template="empty",timeoutTicks=190)
  public static void actualSRobotPayloadBoostRejectsReplayAndRetainsCooldown(GameTestHelper helper){
    if(!AccessoriesBridge.available()){helper.succeed();return;}
    var s=adult();OmochaoRules.initialize(s,adult(),"Robot Builder",5);var e=equip(helper,s);
    helper.runAfterDelay(12,()->{
      var c=body(helper,e);var player=e.keeper();var pos=helper.absolutePos(CENTER);
      player.teleportTo(pos.getX()+.5,pos.getY()+30,pos.getZ()+.5);player.setOnGround(false);player.setDeltaMovement(0,0,.1);
      helper.assertTrue(c.state().levels[ChaoState.FLY]==0&&CompanionRules.robotBoost(c.state()),"CoherentallS robot fixture lost its inherited grades");
      helper.assertTrue(player.tryToStartFallFlying(),"AllS robot could not grant elytra at freshFlylevel0");
      int before=c.state().energy;
      player.connection.handleCustomPayload(new ServerboundCustomPayloadPacket(new CompanionNetwork.Boost(true)));
      helper.runAfterDelay(2,()->{
        helper.assertTrue(CompanionContent.boostCooldown(player,e.inventory().getItem(0))>=97,"Registered inbound boost payload did not start a five-second cooldown");
        helper.assertTrue(c.state().energy==before-2,"Actual payload did not charge exactly one boost");
        long until=ChaoCarrierItem.data(e.inventory().getItem(0)).getLong(CompanionContent.BOOST_UNTIL);
        for(int n=0;n<12;n++)player.connection.handleCustomPayload(new ServerboundCustomPayloadPacket(new CompanionNetwork.Boost(true)));
        var saved=ItemStack.parseOptional(helper.getLevel().registryAccess(),(net.minecraft.nbt.CompoundTag)e.inventory().getItem(0).save(helper.getLevel().registryAccess()));
        e.inventory().setItem(0,saved);
        e.carrier().setCount(0);
        for(int n=0;n<12;n++)player.connection.handleCustomPayload(new ServerboundCustomPayloadPacket(new CompanionNetwork.Boost(true)));
        helper.runAfterDelay(3,()->{
          helper.assertTrue(ChaoCarrierItem.data(saved).getLong(CompanionContent.BOOST_UNTIL)==until,"Save/reload changed or reset the boost deadline");
          helper.assertTrue(c.state().energy>=before-3,"Repeated network payloads charged multiple boosts during cooldown");
          helper.assertTrue(!CompanionContent.requestBoost(player),"A copied/reloaded slot bypassed boost cooldown");
          CompanionContent.unequip(player,saved);
          helper.runAfterDelay(4,()->{
            body(helper,e);player.setOnGround(false);player.tryToStartFallFlying();
            helper.assertTrue(CompanionContent.boostCooldown(player,e.inventory().getItem(0))>0,"Re-equipping erased the boost deadline");
            helper.assertTrue(!CompanionContent.requestBoost(player),"Re-equipping bypassed the same companion's cooldown");
            helper.runAfterDelay(96,()->{
              var recovered=body(helper,e);player.teleportTo(pos.getX()+.5,pos.getY()+30,pos.getZ()+.5);player.setOnGround(false);player.tryToStartFallFlying();
              helper.assertTrue(CompanionContent.boostCooldown(player,e.inventory().getItem(0))==0,"Boost remained disabled past its exact five-second deadline");
              helper.assertTrue(CompanionContent.requestBoost(player),"A valid second boost was not accepted after cooldown");
              helper.assertTrue(recovered.state().energy>5,"Fixture exhausted unexpectedly");finish(helper,e);
            });
          });
        });
      });
    });
  }

  @GameTest(template="empty",timeoutTicks=110)
  public static void actualFiniteGlideDoesNotRenewMidairAndResetsOnLanding(GameTestHelper helper){
    if(!AccessoriesBridge.available()){helper.succeed();return;}
    var s=adult();s.levels[ChaoState.FLY]=0;var e=equip(helper,s);
    helper.runAfterDelay(12,()->{
      var c=body(helper,e);var player=e.keeper();var pos=helper.absolutePos(CENTER);
      player.teleportTo(pos.getX()+.5,pos.getY()+80,pos.getZ()+.5);player.setNoGravity(false);player.setOnGround(false);player.setDeltaMovement(0,-.3,.15);
      helper.assertTrue(player.tryToStartFallFlying(),"An untrained living adult could not start timed vanilla elytra flight");
      helper.runAfterDelay(3,()->helper.assertTrue(player.isFallFlying()&&c.isCompanionGripping()&&c.noPhysics,
          "Real timed elytra did not attach the companion body"));
      helper.runAfterDelay(50,()->{
        helper.assertTrue(!player.onGround()&&CompanionContent.glideRemaining(player,c.state())==0,"LowFly glide did not exhaust its finite airborne budget"+diagnostic(e));
        helper.assertTrue(!player.isFallFlying(),"Expired finite budget retained the vanilla elytra flag");
        helper.assertTrue(player.getDeltaMovement().y<-.12,"Expired finite elytra kept the old slow-fall cap");
        helper.assertTrue(!c.isCompanionGripping()&&!c.noPhysics,"Expired finite glide retained its back attachment");
        CompanionContent.unequip(player,e.inventory().getItem(0));
        helper.runAfterDelay(4,()->{
          var resumed=body(helper,e);
          helper.assertTrue(CompanionContent.glideRemaining(player,resumed.state())==0&&!player.tryToStartFallFlying(),"Re-equipping renewed limited elytra in the same airborne period");
          helper.setBlock(CENTER.below(),Blocks.STONE);player.teleportTo(pos.getX()+.5,pos.getY(),pos.getZ()+.5);player.setDeltaMovement(Vec3.ZERO);
          helper.runAfterDelay(4,()->{
            helper.assertTrue(player.onGround(),"Landing fixture did not contact its actual floor");
            helper.assertTrue(!resumed.isCompanionGripping()&&!resumed.noPhysics,"Landing retained companion attachment or collision suppression");
            helper.assertTrue(CompanionContent.glideRemaining(player,resumed.state())==CompanionRules.glideTicks(0),"A real landing did not restore the next glide budget");finish(helper,e);
          });
        });
      });
    });
  }

  private static void acknowledgeTeleport(ServerPlayer player){
    try{
      var field=net.minecraft.server.network.ServerGamePacketListenerImpl.class.getDeclaredField("awaitingTeleport");field.setAccessible(true);
      player.connection.handleAcceptTeleportPacket(new ServerboundAcceptTeleportationPacket(field.getInt(player.connection)));
    }catch(ReflectiveOperationException e){throw new IllegalStateException("Could not acknowledge the actual test teleport",e);}
  }
  private static void fallingPackets(GameTestHelper helper,ServerPlayer player,int count,Runnable done){
    if(count==0){done.run();return;}
    helper.runAfterDelay(1,()->{
      // The real network tick resets firstGood/lastGood and runs doTick, then
      // a client move supplies position. Its reported server velocity is zero.
      player.connection.tick();player.setDeltaMovement(Vec3.ZERO);
      player.connection.handleMovePlayer(new ServerboundMovePlayerPacket.Pos(player.getX(),player.getY()-.16,player.getZ(),false));
      fallingPackets(helper,player,count-1,done);
    });
  }
  @GameTest(template="empty",timeoutTicks=100)
  public static void actualZeroVelocityMovementPacketsCannotRenewFiniteGlide(GameTestHelper helper){
    if(!AccessoriesBridge.available()){helper.succeed();return;}
    var s=adult();s.levels[ChaoState.FLY]=0;var e=equip(helper,s);
    helper.runAfterDelay(12,()->{
      var c=body(helper,e);var player=e.keeper();var pos=helper.absolutePos(CENTER);
      player.teleportTo(pos.getX()+.5,pos.getY()+60,pos.getZ()+.5);player.setNoGravity(true);player.setOnGround(false);acknowledgeTeleport(player);
      helper.assertTrue(player.tryToStartFallFlying(),"Actual packet fixture failed to launch timed vanilla elytra");
      PHYSICS.remove(player);
      fallingPackets(helper,player,48,()->{
        helper.assertTrue(player.getY()<pos.getY()+56,"Actual inbound movement packets did not move the player");
        helper.assertTrue(player.getPersistentData().contains(CompanionContent.AIR_STARTED),"Observed falling positions never began finite glide"+diagnostic(e));
        helper.assertTrue(CompanionContent.glideRemaining(player,c.state())==0,"Zero server velocity renewed finite glide instead of consuming elapsed ticks"+diagnostic(e));
        helper.assertTrue(player.getPersistentData().getInt(CompanionContent.AIR_USED)>=40,"Capped/zero velocity stalled the authoritative glide clock"+diagnostic(e));
        helper.assertTrue(!player.isFallFlying(),"Expired packet-driven timed elytra kept flying");finish(helper,e);
      });
    });
  }

  private static void toggleFiniteFlight(GameTestHelper helper,Equipped e,int count,long started,Runnable done) {
    if(count==0){done.run();return;}
    helper.runAfterDelay(1,()->{
      var player=e.keeper();player.stopFallFlying();player.setOnGround(false);
      helper.assertTrue(player.tryToStartFallFlying(),"Available finite budget stopped a legal restart"+diagnostic(e));
      helper.assertTrue(player.getPersistentData().getLong(CompanionContent.AIR_STARTED)==started,
          "Repeated starts renewed the authoritative deadline");
      toggleFiniteFlight(helper,e,count-1,started,done);
    });
  }
  @GameTest(template="empty",timeoutTicks=110)
  public static void twentyMidairLaunchCyclesCannotExtendFiniteElytra(GameTestHelper helper) {
    if(!AccessoriesBridge.available()){helper.succeed();return;}
    var s=adult();s.levels[ChaoState.FLY]=0;var e=equip(helper,s);
    helper.runAfterDelay(12,()->{
      var c=body(helper,e);var player=e.keeper();var pos=helper.absolutePos(CENTER);
      player.teleportTo(pos.getX()+.5,pos.getY()+100,pos.getZ()+.5);player.setNoGravity(true);player.setOnGround(false);
      helper.assertTrue(player.tryToStartFallFlying(),"Finite launch did not start");
      long started=player.getPersistentData().getLong(CompanionContent.AIR_STARTED);
      toggleFiniteFlight(helper,e,20,started,()->helper.runAfterDelay(25,()->{
        helper.assertTrue(!player.isFallFlying()&&CompanionContent.glideRemaining(player,c.state())==0,
            "Twenty start/stop cycles extended the finite elapsed window"+diagnostic(e));
        for(int i=0;i<20;i++)helper.assertTrue(!player.tryToStartFallFlying(),"Exhausted finite flight restarted");
        helper.assertTrue(player.getPersistentData().getLong(CompanionContent.AIR_STARTED)==started,
            "Rejected restarts moved the deadline");finish(helper,e);
      }));
    });
  }

  @GameTest(template="empty",timeoutTicks=80)
  public static void actualTwoSRobotsCannotCyclePastPlayerBoostDeadline(GameTestHelper helper){
    if(!AccessoriesBridge.available()){helper.succeed();return;}
    var firstState=adult();OmochaoRules.initialize(firstState,adult(),"First Builder",5);var first=equip(helper,firstState);
    helper.runAfterDelay(12,()->{
      body(helper,first);var player=first.keeper();var pos=helper.absolutePos(CENTER);
      player.teleportTo(pos.getX()+.5,pos.getY()+30,pos.getZ()+.5);player.setOnGround(false);player.tryToStartFallFlying();
      helper.assertTrue(CompanionContent.requestBoost(player),"First valid robot boost failed");
      long deadline=player.getPersistentData().getLong(CompanionContent.PLAYER_BOOST_UNTIL);
      var secondState=adult();OmochaoRules.initialize(secondState,adult(),"Second Builder",5);
      var secondChao=ChaoGarden.CHAO.get().create(helper.getLevel());secondChao.owner=player.getUUID();secondChao.setState(secondState);
      var secondCarrier=new ItemStack(ChaoGarden.item("chao_carrier"));
      helper.assertTrue(ChaoCarrierItem.capture(secondCarrier,secondChao,player),"Second distinct robot fixture capture failed");
      CompanionContent.unequip(player,first.inventory().getItem(0));player.getInventory().setItem(0,first.carrier());first.inventory().setItem(0,secondCarrier);
      var second=new Equipped(player,first.inventory(),secondCarrier,ChaoCarrierItem.storedUUID(secondCarrier));
      helper.runAfterDelay(5,()->{
        body(helper,second);player.setOnGround(false);player.tryToStartFallFlying();
        helper.assertTrue(!first.chao().equals(second.chao()),"Rotation fixture did not use two distinct robots");
        helper.assertTrue(ChaoCarrierItem.data(secondCarrier).getLong(CompanionContent.BOOST_UNTIL)==0,"Second robot unexpectedly had its own boost timer");
        helper.assertTrue(CompanionContent.boostCooldown(player,secondCarrier)>0&&!CompanionContent.requestBoost(player),"Alternating twoS robots bypassed the player's five-second ability limit");
        helper.assertTrue(player.getPersistentData().getLong(CompanionContent.PLAYER_BOOST_UNTIL)==deadline,"Rejected rotation altered the deadline");
        helper.assertTrue(ChaoCarrierItem.filled(first.carrier()),"Slot rotation lost the first held robot");finish(helper,second);
      });
    });
  }

  @GameTest(template="empty",timeoutTicks=80)
  public static void actualUntrainedAndMaximumCompanionCombatDamage(GameTestHelper helper){
    if(!AccessoriesBridge.available()){helper.succeed();return;}
    var s=adult();s.levels[ChaoState.POWER]=0;var e=equip(helper,s);
    helper.runAfterDelay(12,()->{
      var c=body(helper,e);var cow=new Cow(EntityType.COW,helper.getLevel());cow.setNoAi(true);
      cow.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH).setBaseValue(40);cow.setHealth(40);
      cow.moveTo(c.getX()+.4,c.getY(),c.getZ(),0,0);helper.getLevel().addFreshEntity(cow);
      e.keeper().setLastHurtMob(cow);CompanionContent.drive(c);
      helper.assertTrue(Math.abs(cow.getHealth()-39)<.001,"An untrained companion did not land an ordinary one-damage punch");
      c.state().levels[ChaoState.POWER]=99;cow.setHealth(40);cow.invulnerableTime=0;
      for(int n=0;n<24;n++)CompanionContent.drive(c);
      helper.assertTrue(Math.abs(cow.getHealth()-29)<.001,"MaximumPower did not land the fully charged eleven-damage NetheriteSharpnessV equivalent");
      cow.discard();finish(helper,e);
    });
  }
}
