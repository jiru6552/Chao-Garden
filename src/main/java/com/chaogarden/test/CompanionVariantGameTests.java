package com.chaogarden.test;

import com.chaogarden.ChaoGarden;
import com.chaogarden.companion.AccessoriesBridge;
import com.chaogarden.companion.CompanionContent;
import com.chaogarden.companion.CompanionNetwork;
import com.chaogarden.core.ChaoState;
import com.chaogarden.entity.ChaoEntity;
import com.chaogarden.item.ChaoCarrierItem;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.Tag;
import net.minecraft.network.protocol.common.ServerboundCustomPayloadPacket;
import net.minecraft.network.protocol.game.ServerboundContainerClickPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.EnderMan;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** Real optional-slot, event, packet and saved vanilla inventory regressions. */
@GameTestHolder(ChaoGarden.MODID)
@PrefixGameTestTemplate(false)
public final class CompanionVariantGameTests {
  private static final BlockPos CENTER=new BlockPos(2,2,2);
  private record Equipped(ServerPlayer keeper,Container inventory,UUID chao){}

  private static Equipped equip(GameTestHelper helper,String variant,int fly){
    try {
      var keeper=TestConnections.makeSurvivalServerPlayer(helper);var pos=helper.absolutePos(CENTER);
      keeper.teleportTo(pos.getX()+.5,pos.getY(),pos.getZ()+.5);keeper.setNoGravity(true);
      var state=ChaoState.egg(0,false,true,0,new Random(230));state.adult=true;state.variant=variant;state.energy=100;
      state.levels[ChaoState.FLY]=fly;
      var chao=ChaoGarden.CHAO.get().create(helper.getLevel());chao.owner=keeper.getUUID();chao.setState(state);
      var carrier=new ItemStack(ChaoGarden.item("chao_carrier"));
      helper.assertTrue(ChaoCarrierItem.capture(carrier,chao,keeper),"Could not capture variant companion fixture");
      var capType=Class.forName("io.wispforest.accessories.api.AccessoriesCapability");
      var containerType=Class.forName("io.wispforest.accessories.api.AccessoriesContainer");
      var capability=capType.getMethod("get",LivingEntity.class).invoke(null,keeper);
      var containers=(Map<?,?>)capType.getMethod("getContainers").invoke(capability);
      var container=containers.get("chao_companion");helper.assertTrue(container!=null,"Real companion slot missing");
      var inventory=(Container)containerType.getMethod("getAccessories").invoke(container);
      inventory.setItem(0,carrier);containerType.getMethod("markChanged",boolean.class).invoke(container,false);
      return new Equipped(keeper,inventory,ChaoCarrierItem.storedUUID(carrier));
    }catch(ReflectiveOperationException ex){throw new IllegalStateException(ex);}
  }
  private static Equipped equip(GameTestHelper h,String variant){return equip(h,variant,0);}
  private static ChaoEntity body(GameTestHelper helper,Equipped e){
    var found=helper.getLevel().getEntity(e.chao());helper.assertTrue(found instanceof ChaoEntity,"Actual slot did not summon the correct Chao");return (ChaoEntity)found;
  }
  private static void finish(GameTestHelper helper,Equipped e){
    e.keeper().closeContainer();CompanionContent.unequip(e.keeper(),e.inventory().getItem(0));e.inventory().setItem(0,ItemStack.EMPTY);
    e.keeper().server.getPlayerList().remove(e.keeper());helper.succeed();
  }
  private static boolean gaze(EnderMan enderman,ServerPlayer player){
    try{var method=EnderMan.class.getDeclaredMethod("isLookingAtMe",net.minecraft.world.entity.player.Player.class);method.setAccessible(true);return (boolean)method.invoke(enderman,player);}
    catch(ReflectiveOperationException ex){throw new IllegalStateException(ex);}
  }
  private static void alignGaze(GameTestHelper helper,EnderMan enderman,ServerPlayer player){
    enderman.moveTo(player.getX(),player.getY(),player.getZ()+1.5,0,0);
    player.lookAt(EntityAnchorArgument.Anchor.EYES,enderman.getEyePosition());
    Vec3 ray=enderman.getEyePosition().subtract(player.getEyePosition()).normalize();
    helper.assertTrue(player.hasLineOfSight(enderman)&&player.getLookAngle().normalize().dot(ray)>.9999,
        "Gaze fixture must have a clear, precisely aligned eye ray");
  }

  @GameTest(template="empty",timeoutTicks=80)
  public static void absentCompanionCannotOpenRemoteChest(GameTestHelper helper){
    var player=TestConnections.makeSurvivalServerPlayer(helper);
    helper.assertTrue(!CompanionContent.requestEnderChest(player),"No-slot player opened a remote chest");
    player.connection.handleCustomPayload(new ServerboundCustomPayloadPacket(new CompanionNetwork.OpenEnderChest(true)));
    helper.runAfterDelay(2,()->{
      helper.assertTrue(player.containerMenu==player.inventoryMenu,"Untrusted payload opened a chest without a real companion");
      player.server.getPlayerList().remove(player);helper.succeed();
    });
  }

  @GameTest(template="empty",timeoutTicks=140)
  public static void actualNetherSlotProtectsFireAndLavaButNotOrdinaryDamage(GameTestHelper helper){
    if(!AccessoriesBridge.available()){helper.succeed();return;}
    var e=equip(helper,"nether");
    helper.runAfterDelay(72,()->{
      var chao=body(helper,e);var p=e.keeper();
      helper.assertTrue(CompanionContent.hasVariantCompanion(p,"nether")&&!CompanionContent.hasVariantCompanion(p,"end"),"Theme traits leaked across variants");
      p.invulnerableTime=0;p.setHealth(20);float before=p.getHealth();
      p.hurt(p.damageSources().inFire(),5);p.invulnerableTime=0;p.hurt(p.damageSources().lava(),5);
      p.invulnerableTime=0;p.hurt(p.damageSources().onFire(),5);p.invulnerableTime=0;p.hurt(p.damageSources().hotFloor(),5);
      helper.assertTrue(p.getHealth()==before,"Real fire/lava tagged damage harmed Nether keeper");
      p.invulnerableTime=0;p.hurt(p.damageSources().generic(),2);
      helper.assertTrue(p.getHealth()<before,"Nether trait granted immunity to unrelated damage");
      float bodyBefore=chao.getHealth();chao.invulnerableTime=0;chao.hurt(chao.damageSources().lava(),8);
      helper.assertTrue(chao.getHealth()==bodyBefore,"Actual Nether projection burned in its keeper's lava");
      chao.setNoAi(true);chao.setNoGravity(true);chao.setDeltaMovement(Vec3.ZERO);
      var lava=chao.blockPosition();helper.getLevel().setBlockAndUpdate(lava,Blocks.LAVA.defaultBlockState());
      chao.state().energy=0;p.setRemainingFireTicks(100);chao.setRemainingFireTicks(100);
      helper.runAfterDelay(4,()->{
        boolean immersed=chao.isInLava();helper.getLevel().setBlockAndUpdate(lava,Blocks.AIR.defaultBlockState());
        helper.assertTrue(immersed&&chao.isAlive()&&chao.getHealth()==bodyBefore&&CompanionContent.hasVariantCompanion(p,"nether"),"Real lava immersion disabled Nether protection");
        helper.assertTrue(!p.isOnFire()&&!chao.isOnFire(),"Tired living Nether companion did not extinguish keeper and body");
        chao.hurt(chao.damageSources().genericKill(),Float.MAX_VALUE);
        helper.assertTrue(!CompanionContent.hasVariantCompanion(p,"nether"),"Defeated companion retained immunity");
        p.invulnerableTime=0;p.setHealth(20);p.hurt(p.damageSources().lava(),5);
        helper.assertTrue(p.getHealth()<20,"Recovering Nether companion still prevented lava damage");finish(helper,e);
      });
    });
  }

  @GameTest(template="empty",timeoutTicks=100)
  public static void actualEndSlotRejectsLevitationAndGazeButAllowsRetaliation(GameTestHelper helper){
    if(!AccessoriesBridge.available()){helper.succeed();return;}
    var e=equip(helper,"end");e.keeper().addEffect(new MobEffectInstance(MobEffects.LEVITATION,400,1));
    helper.runAfterDelay(12,()->{
      var c=body(helper,e);var p=e.keeper();
      helper.assertTrue(!p.hasEffect(MobEffects.LEVITATION),"End Chao did not clear existing levitation");
      helper.assertTrue(!p.addEffect(new MobEffectInstance(MobEffects.LEVITATION,200,0))&&!p.hasEffect(MobEffects.LEVITATION),"Actual hook admitted levitation");
      helper.assertTrue(p.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING,200,0)),"End trait rejected unrelated effect");
      var enderman=EntityType.ENDERMAN.create(helper.getLevel());enderman.setNoAi(true);enderman.setNoGravity(true);
      alignGaze(helper,enderman,p);helper.getLevel().addFreshEntity(enderman);
      helper.assertTrue(!gaze(enderman,p),"Actual gaze calculation ignored End Chao");
      enderman.hurt(enderman.damageSources().playerAttack(p),1);
      helper.assertTrue(enderman.getLastHurtByMob()==p,"End trait prevented recording a real attack");
      enderman.setTarget(p);helper.assertTrue(enderman.getTarget()==p,"End companion suppressed ordinary retaliation targeting");enderman.setTarget(null);
      c.state().energy=0;helper.assertTrue(!p.addEffect(new MobEffectInstance(MobEffects.LEVITATION,200,0)),"Tired End companion lost innate immunity");
      CompanionContent.unequip(p,e.inventory().getItem(0));e.inventory().setItem(0,ItemStack.EMPTY);
      helper.assertTrue(!CompanionContent.hasVariantCompanion(p,"end"),"Unequipped body retained authorization");
      helper.assertTrue(p.addEffect(new MobEffectInstance(MobEffects.LEVITATION,200,0)),"Unequip retained levitation immunity");
      // A real attack can teleport an Enderman; restore the identical visible ray.
      alignGaze(helper,enderman,p);
      helper.assertTrue(gaze(enderman,p),"Unequipped End companion still suppressed eye contact");
      enderman.discard();finish(helper,e);
    });
  }

  @GameTest(template="empty",timeoutTicks=130)
  public static void endChestPayloadUsesOwnVanillaInventoryAndRevokesWithSlot(GameTestHelper helper){
    if(!AccessoriesBridge.available()){helper.succeed();return;}
    var e=equip(helper,"end");var other=TestConnections.makeSurvivalServerPlayer(helper);
    e.keeper().getEnderChestInventory().setItem(0,new ItemStack(Items.DIAMOND,7));
    other.getEnderChestInventory().setItem(0,new ItemStack(Items.EMERALD,11));
    helper.runAfterDelay(12,()->{
      var c=body(helper,e);var p=e.keeper();
      var cursor=new ItemStack(Items.IRON_INGOT,3);p.inventoryMenu.setCarried(cursor);
      helper.assertTrue(!CompanionContent.requestEnderChest(p)&&p.containerMenu==p.inventoryMenu
          &&p.inventoryMenu.getCarried()==cursor&&cursor.getCount()==3,"Late open request hid or moved an occupied inventory cursor");
      p.inventoryMenu.setCarried(ItemStack.EMPTY);
      p.connection.handleCustomPayload(new ServerboundCustomPayloadPacket(new CompanionNetwork.OpenEnderChest(true)));
      helper.runAfterDelay(2,()->{
        helper.assertTrue(p.containerMenu instanceof ChestMenu,"Registered key payload did not open vanilla chest menu");
        var menu=p.containerMenu;int menuId=menu.containerId;
        helper.assertTrue(menu.getSlot(0).getItem().is(Items.DIAMOND)&&menu.getSlot(0).getItem().getCount()==7,"Menu did not use keeper's actual inventory");
        for(int i=0;i<12;i++)p.connection.handleCustomPayload(new ServerboundCustomPayloadPacket(new CompanionNetwork.OpenEnderChest(true)));
        helper.assertTrue(p.containerMenu==menu&&p.containerMenu.containerId==menuId,"Repeated payload reset an open menu");
        menu.clicked(0,0,ClickType.PICKUP,p);
        helper.assertTrue(menu.getCarried().is(Items.DIAMOND)&&p.getEnderChestInventory().getItem(0).isEmpty(),"Vanilla click did not mutate stored inventory");
        menu.clicked(1,0,ClickType.PICKUP,p);p.closeContainer();
        helper.assertTrue(p.getEnderChestInventory().getItem(1).getCount()==7&&other.getEnderChestInventory().getItem(0).getCount()==11,"Remote chest duplicated items or changed another keeper's inventory");
        var saved=p.getEnderChestInventory().createTag(p.registryAccess());p.getEnderChestInventory().clearContent();p.getEnderChestInventory().fromTag(saved,p.registryAccess());
        helper.assertTrue(p.getEnderChestInventory().getItem(1).is(Items.DIAMOND)&&p.getEnderChestInventory().getItem(1).getCount()==7,"Vanilla save/load lost remote changes");
        helper.assertTrue(!CompanionContent.requestEnderChest(p),"Immediate reopen bypassed cooldown");
        helper.runAfterDelay(10,()->{
          helper.assertTrue(CompanionContent.requestEnderChest(p),"Valid request remained blocked after cooldown");
          var open=p.containerMenu;
          // Check admission directly before a death-cleanup callback can run.
          p.setHealth(0);
          helper.assertTrue(!open.stillValid(p)&&!CompanionContent.hasVariantCompanion(p,"end"),"Dead keeper retained companion authorization before cleanup");
          p.setHealth(20);helper.assertTrue(open.stillValid(p),"Restored fixture lost its valid live slot");
          c.hurt(c.damageSources().genericKill(),Float.MAX_VALUE);
          helper.assertTrue(!open.stillValid(p)&&!CompanionContent.requestEnderChest(p),"Defeat retained remote menu authorization");
          p.connection.handleContainerClick(new ServerboundContainerClickPacket(open.containerId,open.getStateId(),1,0,
              ClickType.PICKUP,ItemStack.EMPTY,new Int2ObjectOpenHashMap<>()));
          helper.assertTrue(open.getCarried().isEmpty()&&p.getEnderChestInventory().getItem(1).getCount()==7,"Stale click bypassed revoked authorization");
          helper.runAfterDelay(2,()->{
            helper.assertTrue(p.containerMenu==p.inventoryMenu,"Revoked menu did not close automatically");
            helper.assertTrue(p.getEnderChestInventory().getItem(1).getCount()==7,"Closing revoked menu changed stored contents");
            other.server.getPlayerList().remove(other);finish(helper,e);
          });
        });
      });
    });
  }

  @GameTest(template="empty",timeoutTicks=100)
  public static void gripRecallReleasesSameChaoWithOrdinaryPhysics(GameTestHelper helper){
    if(!AccessoriesBridge.available()){helper.succeed();return;}
    var e=equip(helper,"normal",99);
    helper.runAfterDelay(12,()->{
      var c=body(helper,e);var p=e.keeper();var center=helper.absolutePos(CENTER);
      p.teleportTo(center.getX()+.5,center.getY()+12,center.getZ()+.5);p.setOnGround(false);p.setDeltaMovement(0,-.2,.1);
      helper.assertTrue(p.tryToStartFallFlying(),"Actual maximum Fly companion could not start flight");
      helper.runAfterDelay(4,()->{
        helper.assertTrue(c.isCompanionGripping()&&c.noPhysics&&c.isNoGravity(),"Actual supported flight did not attach body with temporary physics");
        c.setDeltaMovement(.4,-.8,.6);c.fallDistance=12;
        CompanionContent.beforeUnequip(p,e.inventory().getItem(0));
        var tag=ChaoCarrierItem.entityData(e.inventory().getItem(0));
        helper.assertTrue(!tag.getBoolean("NoGravity")&&tag.getFloat("FallDistance")==0,"Temporary gravity/fall debt leaked into item");
        var motion=tag.getList("Motion",Tag.TAG_DOUBLE);
        helper.assertTrue(motion.size()==3&&motion.getDouble(0)==0&&motion.getDouble(1)==0&&motion.getDouble(2)==0,"Temporary velocity leaked into item");
        var carrier=e.inventory().getItem(0);CompanionContent.unequip(p,carrier);e.inventory().setItem(0,ItemStack.EMPTY);
        var floor=center.offset(1,-1,0);helper.getLevel().setBlockAndUpdate(floor,Blocks.STONE.defaultBlockState());
        p.teleportTo(center.getX()-2,center.getY(),center.getZ());p.setItemInHand(InteractionHand.MAIN_HAND,carrier);
        var result=carrier.useOn(new UseOnContext(p,InteractionHand.MAIN_HAND,new BlockHitResult(Vec3.atCenterOf(floor),Direction.UP,floor,false)));
        helper.assertTrue(result.consumesAction(),"Recalled carrier did not release into clear garden space");
        var released=helper.getLevel().getEntity(e.chao());
        helper.assertTrue(released instanceof ChaoEntity,"Release did not restore same UUID");
        var natural=(ChaoEntity)released;
        helper.assertTrue(!natural.isNoGravity()&&!natural.noPhysics&&!natural.isCompanionGripping()&&!CompanionContent.isManaged(natural),"Flight state leaked into released garden Chao");
        natural.discard();finish(helper,e);
      });
    });
  }

  @GameTest(template="empty",timeoutTicks=30)
  public static void ordinarySnapshotPreservesDeliberatePhysics(GameTestHelper helper){
    var c=ChaoGarden.CHAO.get().create(helper.getLevel());c.setNoGravity(true);c.setDeltaMovement(.4,.2,-.6);c.fallDistance=9;
    var stack=new ItemStack(ChaoGarden.item("chao_carrier"));ChaoCarrierItem.snapshot(stack,c);
    var tag=ChaoCarrierItem.entityData(stack);var motion=tag.getList("Motion",Tag.TAG_DOUBLE);
    helper.assertTrue(tag.getBoolean("NoGravity")&&tag.getFloat("FallDistance")==9&&motion.getDouble(0)==.4&&motion.getDouble(1)==.2&&motion.getDouble(2)==-.6,
        "Managed-only cleanup changed ordinary saved physics");helper.succeed();
  }
}
