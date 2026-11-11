package com.chaogarden.test;

import com.chaogarden.ChaoGarden;
import com.chaogarden.GardenConfig;
import com.chaogarden.care.ChaoCareServices;
import com.chaogarden.core.ChaoEmotion;
import com.chaogarden.core.ChaoState;
import com.chaogarden.entity.ChaoEntity;
import com.chaogarden.item.ChaoCarrierItem;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** Real gift/inventory transactions with the normal phase clock accelerated in fixtures. */
@GameTestHolder(ChaoGarden.MODID)
@PrefixGameTestTemplate(false)
public final class WardrobeGameTests {
  private static final BlockPos AT=new BlockPos(3,2,3);
  private static ServerPlayer keeper(GameTestHelper h){
    var p=TestConnections.makeSurvivalServerPlayer(h);p.getInventory().clearContent();
    p.teleportTo(h.absolutePos(AT).getX()+.5,h.absolutePos(AT).getY(),h.absolutePos(AT).getZ()+1.5);
    p.setNoGravity(true);return p;
  }
  private static ChaoEntity chao(GameTestHelper h,ServerPlayer p){
    h.setBlock(AT.below(),Blocks.STONE);var c=ChaoGarden.CHAO.get().create(h.getLevel());
    var s=ChaoState.egg(0,false,true,0,new Random(24));s.happiness=100;s.hunger=80;s.energy=100;
    c.setState(s);c.owner=p.getUUID();c.setNoAi(true);c.setNoGravity(true);c.setStaying(true);
    c.moveTo(Vec3.atBottomCenterOf(h.absolutePos(AT)));h.getLevel().addFreshEntity(c);return c;
  }
  /** Change only the existing phase's elapsed clock, never production behavior or world time. */
  @SuppressWarnings("unchecked")
  static void expire(ChaoEntity c){
    try{
      var field=ChaoEntity.class.getDeclaredField("ITEM_STARTED");field.setAccessible(true);
      c.getEntityData().set((EntityDataAccessor<Long>)field.get(null),c.level().getGameTime()-300);
      c.aiStep();
    }catch(ReflectiveOperationException e){throw new IllegalStateException(e);}
  }
  static void decided(ChaoEntity c,boolean accepted){
    var saved=new CompoundTag();c.saveWithoutId(saved);
    saved.putBoolean("WardrobeChoiceMade",true);saved.putBoolean("WardrobeChoiceAccepted",accepted);
    c.load(saved);
  }
  static void finishAccepted(ChaoEntity c){decided(c,true);expire(c);expire(c);expire(c);}
  private static int count(ServerPlayer p,Item item){int n=0;for(int i=0;i<36;i++)if(p.getInventory().getItem(i).is(item))n+=p.getInventory().getItem(i).getCount();return n;}
  private static void finish(GameTestHelper h,ServerPlayer p){p.server.getPlayerList().remove(p);h.succeed();}

  @GameTest(template="activity_test",timeoutTicks=60)
  public static void wardrobeOffersHoldThenDecideAndAcceptedChoiceSurvivesMoodChangeAndReload(GameTestHelper h){
    var p=keeper(h);var c=chao(h,p);ItemStack old=new ItemStack(ChaoGarden.item("aviators_accessory"));c.setItemSlot(EquipmentSlot.OFFHAND,old);
    var offered=new ItemStack(ChaoGarden.item("round_glasses_accessory"),2);offered.set(DataComponents.CUSTOM_NAME,Component.literal("The real pair"));p.getInventory().setItem(0,offered);
    h.assertTrue(ChaoCareServices.equip(p,c,0),"Wardrobe did not offer a real inventory stack");
    h.assertTrue(offered.getCount()==1&&c.getMainHandItem().getHoverName().getString().equals("The real pair")&&c.getItemAction()==ChaoEntity.ItemAction.THINK,"Wardrobe equipped instantly instead of holding and thinking");
    h.assertTrue(c.getItemBySlot(EquipmentSlot.OFFHAND).is(old.getItem()),"Old outfit disappeared before consent");
    decided(c,true);expire(c);h.assertTrue(c.getItemAction()==ChaoEntity.ItemAction.TRAIN,"Accepted saved choice did not reach wear animation");
    c.state().happiness=-100;ChaoEmotion.set(c.state(),ChaoEmotion.ANGER,200);
    var pending=new CompoundTag();c.saveWithoutId(pending);c.load(pending);expire(c);
    h.assertTrue(c.getItemBySlot(EquipmentSlot.OFFHAND).getHoverName().getString().equals("The real pair")&&c.getMainHandItem().isEmpty(),"Reload/mood change rerolled or lost the accepted outfit");
    h.assertTrue(count(p,old.getItem())==1&&count(p,offered.getItem())==1,"Swapping did not return one exact old item and consume only one offer");
    expire(c);h.assertTrue(ChaoCareServices.remove(p,c,EquipmentSlot.OFFHAND)&&count(p,offered.getItem())==2,"Removing the voluntarily worn item duplicated/lost it");finish(h,p);
  }

  @GameTest(template="activity_test",timeoutTicks=60)
  public static void rejectedHatIsThrownWithItsRealEggMetadataAndOldHatIsKept(GameTestHelper h){
    var p=keeper(h);var c=chao(h,p);var old=new ItemStack(ChaoGarden.item("pan_hat"));c.setItemSlot(EquipmentSlot.HEAD,old);
    var coat=ChaoState.egg(4,true,true,0,new Random(24));var tag=new CompoundTag();tag.putString("EggShellCoat",coat.encode());
    var shell=new ItemStack(ChaoGarden.item("eggshell_hat"));shell.set(DataComponents.CUSTOM_DATA,CustomData.of(tag));shell.set(DataComponents.CUSTOM_NAME,Component.literal("Original blue shell"));
    p.getInventory().setItem(0,shell.copy());h.assertTrue(ChaoCareServices.equip(p,c,0),"Shell was not offered");decided(c,false);expire(c);
    h.assertTrue(c.getItemAction()==ChaoEntity.ItemAction.REJECT&&c.getItemBySlot(EquipmentSlot.HEAD).is(old.getItem()),"Refusal did not keep the existing hat");
    expire(c);var dropped=h.getLevel().getEntitiesOfClass(ItemEntity.class,c.getBoundingBox().inflate(3),i->ItemStack.isSameItemSameComponents(i.getItem(),shell));
    h.assertTrue(dropped.size()==1&&dropped.getFirst().getItem().getCount()==1&&c.getMainHandItem().isEmpty(),"Refusal did not throw exactly the original physical shell");
    h.assertTrue(dropped.getFirst().getPersistentData().getLong("ChaoRejectedUntil")>h.getLevel().getGameTime(),"Rejected gift can immediately be picked up and rerolled");finish(h,p);
  }

  @GameTest(template="activity_test",timeoutTicks=80)
  public static void deniedReplacementReturnRetainsDecisionAndBlocksCaptureDamageAndRecursiveOffers(GameTestHelper h){
    var p=keeper(h);var c=chao(h,p);var old=new ItemStack(Items.GLASS_BOTTLE,3);old.set(DataComponents.CUSTOM_NAME,Component.literal("Foreign stacked headwear"));c.setItemSlot(EquipmentSlot.HEAD,old.copy());
    var gift=new ItemStack(ChaoGarden.item("paper_bag_hat"));var giftItem=gift.getItem();p.getInventory().setItem(0,gift);
    h.assertTrue(ChaoCareServices.equip(p,c,0),"Replacement offer failed");decided(c,true);expire(c);
    for(int i=0;i<36;i++)p.getInventory().setItem(i,new ItemStack(Items.COBBLESTONE,64));p.getInventory().setItem(0,new ItemStack(ChaoGarden.item("paper_bag_hat")));
    var deny=new AtomicBoolean(true);var attempts=new AtomicInteger();
    Consumer<EntityJoinLevelEvent> listener=event->{if(event.getEntity() instanceof ItemEntity item&&ItemStack.isSameItemSameComponents(item.getItem(),old)){
      attempts.incrementAndGet();h.assertTrue(c.getItemBySlot(EquipmentSlot.HEAD).isEmpty(),"Returning old hat remained duplicable in the equipment slot");
      h.assertTrue(!c.canCare(p)&&!ChaoCareServices.remove(p,c,EquipmentSlot.HEAD),"Reentrant wardrobe transaction remained manageable");
      h.assertTrue(!c.releaseLifecycleItems(),"Reentrant lifecycle cleanup duplicated the pending new outfit");
      var held=new ItemStack(ChaoGarden.item("chao_carrier"));h.assertTrue(!ChaoCarrierItem.capture(held,c,p),"Reentrant capture stole pending clothing");
      h.assertTrue(c.offerItem(p,InteractionHand.MAIN_HAND)==net.minecraft.world.InteractionResult.FAIL&&p.getMainHandItem().getCount()==1,"Reentrant gift consumed or replaced inventory");
      h.assertTrue(!c.hurt(c.damageSources().genericKill(),Float.MAX_VALUE),"Reentrant death released partially committed wardrobe");c.aiStep();
      if(deny.get())event.setCanceled(true);
    }};
    NeoForge.EVENT_BUS.addListener(listener);
    try{
      expire(c);h.assertTrue(attempts.get()==1&&ItemStack.matches(c.getItemBySlot(EquipmentSlot.HEAD),old)&&c.getMainHandItem().is(giftItem),"Denied return lost the old gear or pending offer: attempts="+attempts.get()+" action="+c.getItemAction()+" old="+c.getItemBySlot(EquipmentSlot.HEAD)+" held="+c.getMainHandItem());
      var pending=new CompoundTag();c.saveWithoutId(pending);h.assertTrue(pending.getBoolean("WardrobeChoiceAccepted"),"Denied return discarded its accepted choice");c.load(pending);
      c.state().happiness=-100;deny.set(false);expire(c);
      h.assertTrue(attempts.get()==2&&c.getItemBySlot(EquipmentSlot.HEAD).is(giftItem)&&c.getMainHandItem().isEmpty(),"Retry rerolled or failed to commit the same accepted gift");
      var drops=h.getLevel().getEntitiesOfClass(ItemEntity.class,c.getBoundingBox().inflate(3),e->ItemStack.isSameItemSameComponents(e.getItem(),old));h.assertTrue(drops.size()==1&&ItemStack.matches(drops.getFirst().getItem(),old),"Retry duplicated or lost the foreign stacked gear");
    }finally{NeoForge.EVENT_BUS.unregister(listener);}finish(h,p);
  }

  @GameTest(template="activity_test",timeoutTicks=60)
  public static void physicalEggShellsRememberAllNativeCoatsAndOriginalBirthSurvivesRebirth(GameTestHelper h){
    var p=keeper(h);ChaoEntity first=null;String original="";
    for(int i=0;i<6;i++){
      var s=ChaoState.egg(i==0?2:4,i==1,true,i==2?3:i==5?13:0,new Random(240+i));
      if(i==3)s.variant="nether";if(i==4)s.variant="end";
      var egg=ChaoGarden.CHAO_EGG.get().create(h.getLevel());var id=UUID.randomUUID();egg.setState(s);egg.setNoGravity(true);egg.owner=p.getUUID();egg.setRebornChaoId(id);egg.setHatchTreatment(com.chaogarden.entity.ChaoEggEntity.GENTLE);
      var local=new BlockPos(2+i%3*3,2,2+i/3*4);var at=h.absolutePos(local);
      for(int x=-1;x<=1;x++)for(int y=0;y<3;y++)for(int z=-1;z<=1;z++)h.setBlock(local.offset(x,y,z),Blocks.AIR);
      h.getLevel().setBlockAndUpdate(at.below(),Blocks.STONE.defaultBlockState());egg.moveTo(Vec3.atBottomCenterOf(at));h.getLevel().addFreshEntity(egg);
      String expected=s.encode();egg.setIncubation(GardenConfig.HATCH_TICKS.get()-1);egg.tick();
      var born=(ChaoEntity)h.getLevel().getEntity(id);h.assertTrue(egg.isRemoved()&&born!=null,"Real native coat did not hatch: case="+i+" removed="+egg.isRemoved()+" born="+born+" incubation="+egg.incubation()+" pos="+egg.position()+" variant="+egg.state().variant);
      h.assertTrue(born.originEggCoat().encode().equals(expected),"Birth coat was sampled after hatching treatment or lost its native shell");
      var shells=h.getLevel().getEntitiesOfClass(ItemEntity.class,new AABB(at).inflate(1),e->e.getItem().is(ChaoGarden.item("eggshell_hat"))&&e.getItem().getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag().getString("EggShellCoat").equals(expected));
      h.assertTrue(shells.size()==1,"Actual hatch did not leave exactly its own complete patterned shell");
      if(i==0){first=born;original=expected;}else born.discard();
    }
    first.setNoAi(true);first.setNoGravity(true);first.setStaying(true);first.state().adult=true;first.state().happiness=90;first.state().expressedColor=7;
    var carrier=new ItemStack(ChaoGarden.item("chao_carrier"));ChaoCarrierItem.snapshot(carrier,first);
    var restored=ChaoGarden.CHAO.get().create(h.getLevel());restored.load(ChaoCarrierItem.entityData(carrier));h.assertTrue(restored.originEggCoat().encode().equals(original),"Held snapshot recolored the preserved birth shell");
    first.state().adultAgeTicks=GardenConfig.ADULT_TICKS.get()-1;first.aiStep();for(int n=0;n<201;n++)first.aiStep();
    var reborn=h.getLevel().getEntitiesOfClass(com.chaogarden.entity.ChaoEggEntity.class,first.getBoundingBox().inflate(3),e->p.getUUID().equals(e.owner));h.assertTrue(reborn.size()==1,"Lifecycle did not produce a physical rebirth egg");
    var next=reborn.getFirst();var item=next.snapshotItem();h.assertTrue(item.get(DataComponents.CUSTOM_DATA).copyTag().getString("RebornOriginEggCoat").equals(original),"Rebirth egg pickup discarded original birth history");
    next.setNoGravity(true);next.setIncubation(GardenConfig.HATCH_TICKS.get()-1);next.tick();var again=(ChaoEntity)h.getLevel().getEntity(first.getUUID());h.assertTrue(again!=null&&again.originEggCoat().encode().equals(original),"Next physical hatch lost the same creature's original birth history");finish(h,p);
  }
}
