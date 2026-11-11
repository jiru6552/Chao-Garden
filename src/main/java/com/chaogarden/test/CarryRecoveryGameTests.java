package com.chaogarden.test;

import com.chaogarden.ChaoGarden;
import com.chaogarden.activities.*;
import com.chaogarden.companion.*;
import com.chaogarden.core.*;
import com.chaogarden.entity.ChaoEntity;
import com.chaogarden.item.ChaoCarrierItem;
import com.chaogarden.recovery.ChaoRecovery;
import java.util.*;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.gametest.*;
import net.neoforged.neoforge.items.ItemStackHandler;

@GameTestHolder("chao_carry_tests") @PrefixGameTestTemplate(false)
public final class CarryRecoveryGameTests {
  private static final BlockPos AT=new BlockPos(2,2,2);
  private static ChaoEntity chao(GameTestHelper h,ServerPlayer p,boolean adult) {
    h.setBlock(AT.below(),Blocks.STONE);
    var c=ChaoGarden.CHAO.get().create(h.getLevel());c.owner=p==null?null:p.getUUID();c.state().adult=adult;
    c.state().ageTicks=200;c.state().bond=42;c.state().points[0]=199;c.state().happiness=63;
    c.setNoAi(true);c.moveTo(h.absolutePos(AT).getBottomCenter());
    c.setItemSlot(EquipmentSlot.OFFHAND,new ItemStack(Items.DIAMOND_SWORD));c.getOffhandItem().setDamageValue(37);
    c.setItemSlot(EquipmentSlot.MAINHAND,new ItemStack(Items.IRON_PICKAXE));c.getMainHandItem().setDamageValue(19);
    c.getPersistentData().putString("RecoveryProof","preserve");h.getLevel().addFreshEntity(c);return c;
  }
  private static ItemStack recovered(ServerPlayer p,UUID id){
    for(int i=0;i<36;i++){var s=p.getInventory().getItem(i);if(id.equals(ChaoCarrierItem.storedUUID(s)))return s;}
    return ItemStack.EMPTY;
  }
  private static void check(GameTestHelper h,ItemStack stack,ChaoEntity old){
    h.assertTrue(!stack.isEmpty()&&old.getUUID().equals(ChaoCarrierItem.storedUUID(stack)),"Identity missing");
    var copy=ChaoGarden.CHAO.get().create(h.getLevel());copy.load(ChaoCarrierItem.entityData(stack));
    h.assertTrue(copy.owner.equals(old.owner)&&copy.state().encode().equals(old.state().encode())
        &&ItemStack.matches(copy.getOffhandItem(),old.getOffhandItem())&&ItemStack.matches(copy.getMainHandItem(),old.getMainHandItem())
        &&copy.getPersistentData().getString("RecoveryProof").equals("preserve")&&copy.getHealth()>0,"State or item data changed");
  }
  private static void inventory(GameTestHelper h,boolean adult) {
    var p=TestConnections.makeSurvivalServerPlayer(h);var c=chao(h,p,adult);
    var source=c.damageSources().genericKill();c.hurt(source,10000);
    ChaoRecovery.death(new LivingDeathEvent(c,source));
    ChaoRecovery.get(p.server).retry(p.server);ChaoRecovery.get(p.server).retry(p.server);
    check(h,recovered(p,c.getUUID()),c);
    long count=p.getInventory().items.stream().filter(s->c.getUUID().equals(ChaoCarrierItem.storedUUID(s))).count();
    h.assertTrue(count==1&&c.isRemoved(),"Repeated event made copies");
    // Old saved body is rejected even if a chunk containing it was not loaded during delivery.
    var stale=ChaoGarden.CHAO.get().create(h.getLevel());stale.load(c.saveWithoutId(new CompoundTag()));
    h.assertFalse(h.getLevel().addFreshEntity(stale),"Obsolete body returned");
    var receipt=ChaoRecovery.load(ChaoRecovery.get(p.server).save(new CompoundTag(),h.getLevel().registryAccess()),h.getLevel().registryAccess());
    for(var value:receipt.save(new CompoundTag(),h.getLevel().registryAccess()).getList("Records",10)){
      var tag=(CompoundTag)value;if(tag.getUUID("Id").equals(c.getUUID()))h.assertFalse(tag.contains("Entity"),"Completed recovery retained a second snapshot");
    }
    p.server.getPlayerList().remove(p);h.succeed();
  }
  @GameTest(template="empty") public static void babyInventoryExactlyOnce(GameTestHelper h){inventory(h,false);}
  @GameTest(template="empty") public static void adultInventoryExactlyOnce(GameTestHelper h){inventory(h,true);}
  @GameTest(template="empty") public static void fullInventoryCancelledDropAndRetry(GameTestHelper h){
    var p=TestConnections.makeSurvivalServerPlayer(h);for(int i=0;i<36;i++)p.getInventory().setItem(i,new ItemStack(Items.STONE,64));
    var c=chao(h,p,true);c.hurt(c.damageSources().genericKill(),10000);
    Consumer<EntityJoinLevelEvent> veto=e->{if(e.getEntity() instanceof ItemEntity i&&c.getUUID().equals(ChaoCarrierItem.storedUUID(i.getItem())))e.setCanceled(true);};
    NeoForge.EVENT_BUS.addListener(EntityJoinLevelEvent.class,veto);
    try{ChaoRecovery.get(p.server).retry(p.server);h.assertTrue(ChaoRecovery.get(p.server).pendingCount()>0,"Veto lost snapshot");}
    finally{NeoForge.EVENT_BUS.unregister(veto);}
    ChaoRecovery.get(p.server).retry(p.server);ChaoRecovery.get(p.server).retry(p.server);
    var drops=h.getLevel().getEntitiesOfClass(ItemEntity.class,p.getBoundingBox().inflate(10),i->c.getUUID().equals(ChaoCarrierItem.storedUUID(i.getItem())));
    h.assertTrue(drops.size()==1,"Retry did not publish exactly one drop");check(h,drops.getFirst().getItem(),c);
    drops.getFirst().discard();p.server.getPlayerList().remove(p);h.succeed();
  }
  @GameTest(template="empty") public static void offlineOwnerJournalRoundtrip(GameTestHelper h){
    var p=TestConnections.makeSurvivalServerPlayer(h);var c=chao(h,p,false);p.server.getPlayerList().remove(p);
    c.hurt(c.damageSources().genericKill(),10000);var data=ChaoRecovery.get(p.server);data.retry(p.server);
    var saved=data.save(new CompoundTag(),h.getLevel().registryAccess());
    var reload=ChaoRecovery.load(saved,h.getLevel().registryAccess());
    h.assertTrue(reload.pendingCount()>0,"Offline snapshot not saved");
    reload.retry(p.server);h.assertTrue(reload.save(new CompoundTag(),h.getLevel().registryAccess()).equals(saved),"Offline retry changed or lost snapshot");h.succeed();
  }
  @GameTest(template="empty") public static void gardenMarkSurvivesPickupAndRecovery(GameTestHelper h){
    var p=TestConnections.makeSurvivalServerPlayer(h);var c=chao(h,p,false);var registry=GardenRegistry.get(p.server);
    var place=GardenRegistry.Place.at(h.getLevel(),h.absolutePos(AT.offset(0,0,1)));
    var garden=new GardenRegistry.Garden(place,p.getUUID(),"Recovery test");registry.gardens.put(place,garden);
    registry.areas.put(place,new GardenArea(false,List.of(h.absolutePos(AT).asLong()),h.absolutePos(AT)));
    registry.placed(c);h.assertTrue(registry.entries.containsKey(c.getUUID()),"Automatic detection failed");
    var held=new ItemStack(ChaoGarden.item("chao_carrier"));ChaoCarrierItem.capture(held,c,p);
    h.assertTrue(!registry.entries.containsKey(c.getUUID())&&registry.markedHome(c.getUUID(),p.getUUID())!=null,"Pickup erased recovery mark");
    var restore=GardenRegistry.load(registry.save(new CompoundTag(),h.getLevel().registryAccess()),h.getLevel().registryAccess());
    h.assertTrue(restore.markedHome(c.getUUID(),p.getUUID())!=null,"Mark failed reload");
    var outside=ChaoGarden.CHAO.get().create(h.getLevel());outside.load(ChaoCarrierItem.entityData(held));outside.moveTo(h.absolutePos(AT.offset(5,0,0)).getBottomCenter());h.getLevel().addFreshEntity(outside);
    outside.hurt(outside.damageSources().genericKill(),10000);ChaoRecovery.get(p.server).retry(p.server);
    var returned=GardenRegistry.find(p.server,c.getUUID());
    h.assertTrue(returned!=null&&registry.areas.get(place).contains(h.getLevel(),returned.position())&&returned.owner.equals(p.getUUID()),"Garden destination failed");
    registry.dismiss(c.getUUID(),place);h.assertTrue(registry.markedHome(c.getUUID(),p.getUUID())==null,"Explicit unmark retained home");
    returned.discard();registry.removeGarden(place);p.server.getPlayerList().remove(p);h.succeed();
  }
  @GameTest(template="empty") public static void missingGardenFallsBack(GameTestHelper h){
    var p=TestConnections.makeSurvivalServerPlayer(h);var c=chao(h,p,true);var registry=GardenRegistry.get(p.server);
    var place=GardenRegistry.Place.at(h.getLevel(),h.absolutePos(AT));var garden=new GardenRegistry.Garden(place,p.getUUID(),"Removed");
    registry.gardens.put(place,garden);registry.bind(p,c,garden);registry.removeGarden(place);
    c.hurt(c.damageSources().genericKill(),10000);ChaoRecovery.get(p.server).retry(p.server);check(h,recovered(p,c.getUUID()),c);
    p.server.getPlayerList().remove(p);h.succeed();
  }
  @GameTest(template="empty") public static void storageContract(GameTestHelper h){
    var expected=new ItemStack(Items.DIAMOND_SWORD);expected.setDamageValue(34);
    var storage=new ItemStackHandler(1);h.assertTrue(ChaoRecovery.insert(storage,expected),"Handler did not accept");
    h.assertTrue(ItemStack.matches(storage.getStackInSlot(0),expected),"Handler lost components");
    h.assertFalse(ChaoRecovery.insert(storage,expected),"Full handler accepted another item");
    var reject=new ItemStackHandler(1){@Override public boolean isItemValid(int slot,ItemStack stack){return false;}};
    h.assertFalse(ChaoRecovery.insert(reject,expected),"Filter bypassed");
    var inconsistent=new ItemStackHandler(1){@Override public ItemStack insertItem(int slot,ItemStack stack,boolean simulate){return simulate?ItemStack.EMPTY:stack;}};
    h.assertFalse(ChaoRecovery.insert(inconsistent,expected),"Simulation counted as actual delivery");
    var throwsAfterCommit=new ItemStackHandler(1){@Override public ItemStack insertItem(int slot,ItemStack stack,boolean simulate){
      var left=super.insertItem(slot,stack,simulate);if(!simulate)throw new IllegalStateException("after commit");return left;
    }};
    h.assertTrue(ChaoRecovery.insert(throwsAfterCommit,expected)&&ItemStack.matches(throwsAfterCommit.getStackInSlot(0),expected),"Committed exception was retried");
    var voids=new ItemStackHandler(1){@Override public ItemStack insertItem(int slot,ItemStack stack,boolean simulate){return ItemStack.EMPTY;}};
    boolean caught=false;try{ChaoRecovery.insert(voids,expected);}catch(IllegalStateException error){caught=true;}
    h.assertTrue(caught&&expected.getCount()==1,"Void storage was treated as safe delivery");h.succeed();
  }
  @GameTest(template="empty") public static void naturalCocoonNotIntercepted(GameTestHelper h){
    var p=TestConnections.makeSurvivalServerPlayer(h);var c=chao(h,p,true);c.state().dead=true;c.state().cocoonTicks=100;
    var e=new LivingDeathEvent(c,c.damageSources().genericKill());ChaoRecovery.death(e);
    h.assertFalse(e.isCanceled(),"Natural lifecycle was intercepted");c.discard();p.server.getPlayerList().remove(p);h.succeed();
  }
  @GameTest(template="empty",timeoutTicks=1350,batch="carry_equipped") public static void babyMovementCareAndFullCooldown(GameTestHelper h){
    var state=new ChaoState();state.hunger=80;state.energy=80;state.bond=4;state.ageTicks=200;
    state.levels[ChaoState.FLY]=99;state.levels[ChaoState.RUN]=99;state.points[ChaoState.RUN]=9999;state.variant="nether";
    var e=CompanionSkillGameTests.equip(h,state);var p=e.keeper();
    h.runAtTickTime(15,()->{
      var c=CompanionSkillGameTests.body(h,e);h.assertTrue(c.companionCarryPosition()==CarryRules.HEAD,"Baby not on head");
      h.assertFalse(CompanionContent.flightEligible(p)||CompanionContent.hasVariantCompanion(p,"nether"),"Baby granted benefits");
      h.assertTrue(p.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED).getModifier(CompanionContent.RUN_SPEED)==null,"Baby granted speed");
      c.getPersistentData().putInt("ChaoEquippedCareTicks",5999);p.getAbilities().flying=true;
    });
    h.runAtTickTime(25,()->{
      var c=CompanionSkillGameTests.body(h,e);h.assertTrue(c.isCompanionGripping()&&c.companionKeeperId()==p.getId(),"Baby flight grip missing");
      h.assertTrue(c.state().ageTicks==200&&!c.state().adult&&c.state().hunger==79&&c.state().bond==5,"Baby care or paused age wrong");
      p.getAbilities().flying=false;
    });
    h.runAtTickTime(35,()->{
      var c=CompanionSkillGameTests.body(h,e);h.assertTrue(c.companionCarryPosition()==CarryRules.HEAD,"Landing did not restore head");
      h.assertFalse(c.hurt(c.damageSources().fall(),100),"Equipped fall damage");int happy=c.state().happiness;
      c.hurt(c.damageSources().genericKill(),10000);
      h.assertTrue(c.isRemoved()&&CompanionContent.remaining(e.carrier(),p.server)==1200,"Baby cooldown missing");
      h.assertTrue(ChaoCarrierItem.state(e.carrier()).happiness==happy,"Baby data penalized");
    });
    h.runAtTickTime(1233,()->h.assertTrue(CompanionContent.findEntity(p.server,e.chao())==null,"Baby returned early"));
    h.runAtTickTime(1245,()->{
      var c=CompanionSkillGameTests.body(h,e);h.assertTrue(!c.state().adult&&c.state().ageTicks==200&&c.companionCarryPosition()==CarryRules.HEAD,"Baby recovery reset state");
      CompanionSkillGameTests.finish(h,e);
    });
  }
  @GameTest(template="empty",timeoutTicks=100,batch="carry_equipped") public static void adultUnlocksAndPreview(GameTestHelper h){
    var s=new ChaoState();s.adult=true;s.levels[ChaoState.RUN]=99;s.levels[ChaoState.SWIM]=99;
    var e=CompanionSkillGameTests.equip(h,s);var p=e.keeper();
    h.runAtTickTime(15,()->{
      var c=CompanionSkillGameTests.body(h,e);h.assertTrue(c.companionCarryPosition()==CarryRules.HEAD,"Level mistaken for unlock");
      h.assertTrue(CarryRules.position(c.state(),false,true)==CarryRules.BACK,"Locked swim did not carry");
      c.state().points[ChaoState.RUN]=LocomotionRules.RUN_POINTS;c.state().points[ChaoState.SWIM]=LocomotionRules.SWIM_POINTS;
    });
    h.runAtTickTime(25,()->{
      var c=CompanionSkillGameTests.body(h,e);h.assertTrue(!c.isCompanionAttached()&&CarryRules.position(c.state(),false,true)==CarryRules.FREE,"Unlocked adult behavior lost");
      h.assertTrue(p.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED).getModifier(CompanionContent.RUN_SPEED)!=null,"Unlocked run benefit lost");
      var source=p.createCommandSourceStack().withPermission(2);
      try {h.assertTrue(p.server.getCommands().getDispatcher().execute("chao testcarry loop",source)==1&&CarryPreview.active(c),"Preview command failed");}
      catch(com.mojang.brigadier.exceptions.CommandSyntaxException ex){throw new AssertionError(ex);}
    });
    h.runAtTickTime(40,()->{
      var c=CompanionSkillGameTests.body(h,e);c.hurt(c.damageSources().generic(),1);h.assertFalse(CarryPreview.active(c),"Damage did not stop preview");
      CompanionSkillGameTests.finish(h,e);
    });
  }
  private static void movement(GameTestHelper h,boolean adult,boolean run,boolean swim) {
    var state=new ChaoState();state.adult=adult;state.levels[ChaoState.RUN]=99;state.levels[ChaoState.SWIM]=99;
    state.points[ChaoState.RUN]=run?667:666;state.points[ChaoState.SWIM]=swim?100:99;
    var e=CompanionSkillGameTests.equip(h,state);var p=e.keeper();
    h.runAtTickTime(15,()->{
      var c=CompanionSkillGameTests.body(h,e);p.setSprinting(true);
      h.assertTrue(c.companionCarryPosition()==(!adult||!run?CarryRules.HEAD:CarryRules.FREE),"Wrong ground unlock");
      var at=p.blockPosition();for(int x=-1;x<=1;x++)for(int y=0;y<=2;y++)for(int z=-1;z<=1;z++)p.serverLevel().setBlock(at.offset(x,y,z),Blocks.WATER.defaultBlockState(),3);
    });
    h.runAtTickTime(35,()->{
      var c=CompanionSkillGameTests.body(h,e);h.assertTrue(p.isInWater(),"Fixture did not enter actual water");
      h.assertTrue(c.companionCarryPosition()==(!adult||!swim?CarryRules.BACK:CarryRules.FREE),"Wrong swim unlock");
      h.assertTrue((p.getAttribute(net.neoforged.neoforge.common.NeoForgeMod.SWIM_SPEED).getModifier(CompanionContent.SWIM_SPEED)!=null)==(adult&&swim),"Wrong swim benefit");
      var at=p.blockPosition();for(int x=-2;x<=2;x++)for(int y=-1;y<=3;y++)for(int z=-2;z<=2;z++)p.serverLevel().setBlock(at.offset(x,y,z),Blocks.AIR.defaultBlockState(),3);
      p.teleportTo(p.getX(),p.getY()+5,p.getZ());p.setItemSlot(EquipmentSlot.CHEST,new ItemStack(Items.ELYTRA));p.setOnGround(false);
    });
    h.runAtTickTime(45,()->p.startFallFlying());
    h.runAtTickTime(52,()->{
      var c=CompanionSkillGameTests.body(h,e);h.assertTrue(p.isFallFlying()&&c.isCompanionGripping(),"Real elytra did not carry");p.stopFallFlying();
    });
    h.runAtTickTime(62,()->{
      var c=CompanionSkillGameTests.body(h,e);h.assertTrue(c.companionCarryPosition()==(!adult||!run?CarryRules.HEAD:CarryRules.FREE),"Flight ended with stuck pose");
      CompanionContent.beforeUnequip(p,e.carrier());CompanionContent.unequip(p,e.carrier());e.inventory().setItem(0,ItemStack.EMPTY);
      var released=ChaoGarden.CHAO.get().create(h.getLevel());released.load(ChaoCarrierItem.entityData(e.carrier()));
      h.assertTrue(!released.isCompanionAttached()&&!released.noPhysics&&!released.isNoGravity(),"Attachment escaped into item");
      int age=released.state().ageTicks;released.aiStep();h.assertTrue(released.state().ageTicks==age+1,"Age did not resume");
      p.server.getPlayerList().remove(p);h.succeed();
    });
  }
  @GameTest(template="empty",timeoutTicks=90,batch="carry_water") public static void babySwimmingAndNativeElytra(GameTestHelper h){movement(h,false,true,true);}
  @GameTest(template="empty",timeoutTicks=90,batch="carry_water") public static void adultNeitherUnlocked(GameTestHelper h){movement(h,true,false,false);}
  @GameTest(template="empty",timeoutTicks=90,batch="carry_water") public static void adultRunOnly(GameTestHelper h){movement(h,true,true,false);}
  @GameTest(template="empty",timeoutTicks=90,batch="carry_water") public static void adultSwimOnly(GameTestHelper h){movement(h,true,false,true);}
  @GameTest(template="empty",timeoutTicks=90,batch="carry_water") public static void adultBothUnlocked(GameTestHelper h){movement(h,true,true,true);}

  @GameTest(template="empty") public static void ownerAnotherDimension(GameTestHelper h){
    var p=TestConnections.makeSurvivalServerPlayer(h);var c=chao(h,p,false);
    var nether=p.server.getLevel(net.minecraft.world.level.Level.NETHER);nether.getChunkAt(new BlockPos(0,80,0));
    p.teleportTo(nether,.5,80,.5,0,0);p.hasChangedDimension();c.hurt(c.damageSources().genericKill(),10000);
    ChaoRecovery.get(p.server).retry(p.server);check(h,recovered(p,c.getUUID()),c);
    p.server.getPlayerList().remove(p);h.succeed();
  }
  @GameTest(template="empty") public static void replacementDropPublishesOnce(GameTestHelper h){
    var p=TestConnections.makeSurvivalServerPlayer(h);for(int i=0;i<36;i++)p.getInventory().setItem(i,new ItemStack(Items.STONE,64));
    var c=chao(h,p,true);c.hurt(c.damageSources().genericKill(),10000);boolean[] replacing={false};
    Consumer<EntityJoinLevelEvent> listener=e->{
      if(!replacing[0]&&e.getEntity() instanceof ItemEntity item&&c.getUUID().equals(ChaoCarrierItem.storedUUID(item.getItem()))){
        replacing[0]=true;try{var next=new ItemEntity(h.getLevel(),item.getX(),item.getY(),item.getZ(),item.getItem().copy());
          h.getLevel().addFreshEntity(next);e.setCanceled(true);
        }finally{replacing[0]=false;}
      }
    };NeoForge.EVENT_BUS.addListener(EntityJoinLevelEvent.class,listener);
    try{ChaoRecovery.get(p.server).retry(p.server);ChaoRecovery.get(p.server).retry(p.server);}
    finally{NeoForge.EVENT_BUS.unregister(listener);}
    var drops=h.getLevel().getEntitiesOfClass(ItemEntity.class,p.getBoundingBox().inflate(10),i->c.getUUID().equals(ChaoCarrierItem.storedUUID(i.getItem())));
    h.assertTrue(drops.size()==1,"Replacement duplicated or lost recovery");check(h,drops.getFirst().getItem(),c);drops.getFirst().discard();
    p.server.getPlayerList().remove(p);h.succeed();
  }

  @GameTest(template="empty",timeoutTicks=100) public static void recoveredIdentityCanReincarnate(GameTestHelper h){
    var p=TestConnections.makeSurvivalServerPlayer(h);var c=chao(h,p,true);c.setItemSlot(EquipmentSlot.MAINHAND,ItemStack.EMPTY);
    c.hurt(c.damageSources().genericKill(),10000);ChaoRecovery.get(p.server).retry(p.server);
    var item=recovered(p,c.getUUID());h.assertTrue(!item.isEmpty(),"Recovery item missing");
    p.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,item);
    var floor=h.absolutePos(AT.below());
    var context=new net.minecraft.world.item.context.UseOnContext(p,net.minecraft.world.InteractionHand.MAIN_HAND,
        new net.minecraft.world.phys.BlockHitResult(floor.getCenter(),net.minecraft.core.Direction.UP,floor,false));
    h.assertTrue(item.getItem().useOn(context).consumesAction()&&item.isEmpty(),"Recovered carrier release failed");
    var live=GardenRegistry.find(p.server,c.getUUID());h.assertTrue(live!=null,"Released body missing");
    live.state().happiness=90;live.state().adultAgeTicks=com.chaogarden.core.GradualEvolutionRules.durationTicks(com.chaogarden.GardenConfig.ADULT_TICKS.get())-1;
    live.aiStep();h.assertTrue(live.isRebirthCocoon(),"Recovered adult could not enter natural rebirth");
    for(int n=0;n<200;n++)live.aiStep();
    var eggs=h.getLevel().getEntitiesOfClass(com.chaogarden.entity.ChaoEggEntity.class,live.getBoundingBox().inflate(5),egg->p.getUUID().equals(egg.owner));
    h.assertTrue(live.isRemoved()&&eggs.size()==1,"Rebirth failed to publish one egg");
    var egg=eggs.getFirst();egg.setIncubation(com.chaogarden.GardenConfig.HATCH_TICKS.get()-1);egg.tick();
    var baby=GardenRegistry.find(p.server,c.getUUID());h.assertTrue(egg.isRemoved()&&baby!=null&&!baby.state().adult,"Recovery receipt blocked reincarnated identity");
    h.assertTrue(baby.getOffhandItem().getDamageValue()==37,"Rebirth lost recovered equipment");
    baby.discard();p.server.getPlayerList().remove(p);h.succeed();
  }

}
