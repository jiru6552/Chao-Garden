package com.chaogarden.test;

import com.chaogarden.ChaoGarden;
import com.chaogarden.GardenConfig;
import com.chaogarden.core.ChaoState;
import com.chaogarden.core.GradualEvolutionRules;
import com.chaogarden.core.OmochaoRules;
import com.chaogarden.cwe.CweRobotPartItem;
import com.chaogarden.cwe.OmochaoLifecycle;
import com.chaogarden.entity.ChaoEggEntity;
import com.chaogarden.entity.ChaoEntity;
import com.chaogarden.item.ChaoEggItem;
import java.util.Arrays;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** Actual entity/item/NBT transactions, including denied insertion and recursive callbacks. */
@GameTestHolder(ChaoGarden.MODID)
@PrefixGameTestTemplate(false)
public final class LifecycleAuditGameTests {
  private static final BlockPos AT=new BlockPos(3,2,3);
  private LifecycleAuditGameTests() {}
  private static Player room(GameTestHelper h){
    for(int x=0;x<12;x++)for(int z=0;z<9;z++)h.setBlock(new BlockPos(x,1,z),Blocks.STONE);
    var p=h.makeMockPlayer(GameType.SURVIVAL);p.getAbilities().instabuild=false;p.getAbilities().mayBuild=true;
    p.moveTo(Vec3.atBottomCenterOf(h.absolutePos(new BlockPos(2,2,2))));return p;
  }
  private static AABB area(GameTestHelper h){return new AABB(Vec3.atLowerCornerOf(h.absolutePos(new BlockPos(0,0,0))),Vec3.atLowerCornerOf(h.absolutePos(new BlockPos(12,10,9))));}
  private static void freeze(ChaoEntity c){c.setNoAi(true);c.setNoGravity(false);c.setOnGround(true);c.setDeltaMovement(Vec3.ZERO);}
  private static ChaoState state(){var s=ChaoState.egg(0,false,true,0,new Random(22022));s.happiness=90;s.bond=80;s.hunger=80;s.energy=100;return s;}
  private static ChaoEntity body(GameTestHelper h,Player p,ChaoState s){
    var c=ChaoGarden.CHAO.get().create(h.getLevel());c.setState(s);c.owner=p.getUUID();freeze(c);
    c.moveTo(Vec3.atBottomCenterOf(h.absolutePos(AT)));h.assertTrue(h.getLevel().addFreshEntity(c),"Body insertion");return c;
  }
  private static ChaoEggEntity egg(GameTestHelper h,Player p,ChaoState s){
    var e=ChaoGarden.CHAO_EGG.get().create(h.getLevel());e.setState(s);e.owner=p.getUUID();e.setNoGravity(true);
    e.chaoName=Component.literal("Persistent life");e.moveTo(Vec3.atBottomCenterOf(h.absolutePos(AT)));
    h.assertTrue(h.getLevel().addFreshEntity(e),"Egg insertion");return e;
  }
  private static ChaoEntity hatch(GameTestHelper h,Player p,ChaoEggEntity e){
    e.setIncubation(Math.max(0,GardenConfig.HATCH_TICKS.get()-1));e.tick();
    var bodies=h.getLevel().getEntitiesOfClass(ChaoEntity.class,area(h),c->p.getUUID().equals(c.owner));
    h.assertTrue(e.isRemoved()&&bodies.size()==1,"Physical egg hatches exactly once");var c=bodies.getFirst();freeze(c);return c;
  }
  private static ChaoEntity reload(GameTestHelper h,ChaoEntity c){
    var saved=new CompoundTag();c.saveWithoutId(saved);c.discard();var restored=ChaoGarden.CHAO.get().create(h.getLevel());restored.load(saved);freeze(restored);
    h.assertTrue(h.getLevel().addFreshEntity(restored),"Saved body reinsertion");return restored;
  }
  private static ChaoEggEntity reload(GameTestHelper h,ChaoEggEntity e){
    var saved=new CompoundTag();e.saveWithoutId(saved);e.discard();var restored=ChaoGarden.CHAO_EGG.get().create(h.getLevel());restored.load(saved);restored.setNoGravity(true);
    h.assertTrue(h.getLevel().addFreshEntity(restored),"Saved egg reinsertion");return restored;
  }
  private static void ticks(ChaoEntity c,int count){for(int i=0;i<count;i++)c.aiStep();}
  private static ItemStack hat(){var s=new ItemStack(Items.LEATHER_HELMET);s.set(DataComponents.CUSTOM_NAME,Component.literal("Individual family cap"));s.set(DataComponents.REPAIR_COST,7);s.set(DataComponents.ENCHANTMENT_GLINT_OVERRIDE,true);return s;}
  private static int dropped(GameTestHelper h,ItemStack stack){return h.getLevel().getEntitiesOfClass(ItemEntity.class,area(h),i->ItemStack.isSameItemSameComponents(i.getItem(),stack)).stream().mapToInt(i->i.getItem().getCount()).sum();}
  private static ItemStack pickAndSerialize(GameTestHelper h,Player p,ChaoEggEntity e){
    p.getInventory().clearContent();e.interact(p,InteractionHand.MAIN_HAND);h.assertTrue(e.isRemoved(),"Physical egg pickup");
    ItemStack found=ItemStack.EMPTY;for(int slot=0;slot<p.getInventory().getContainerSize();slot++)if(p.getInventory().getItem(slot).getItem() instanceof ChaoEggItem){found=p.getInventory().getItem(slot).copy();break;}
    h.assertTrue(!found.isEmpty(),"Pickup retained an actual egg item");p.getInventory().clearContent();
    return ItemStack.parse(h.getLevel().registryAccess(),found.save(h.getLevel().registryAccess())).orElseThrow();
  }
  private static ChaoEggEntity place(GameTestHelper h,Player p,ItemStack item){
    p.setItemInHand(InteractionHand.MAIN_HAND,item);var floor=h.absolutePos(AT.below());
    var use=new UseOnContext(p,InteractionHand.MAIN_HAND,new BlockHitResult(Vec3.atCenterOf(floor).add(0,.5,0),Direction.UP,floor,false));
    h.assertTrue(item.getItem() instanceof ChaoEggItem&&item.getItem().useOn(use).consumesAction()&&item.isEmpty(),"Serialized individual egg is placed and consumed once");
    var eggs=h.getLevel().getEntitiesOfClass(ChaoEggEntity.class,area(h),e->p.getUUID().equals(e.owner));
    h.assertTrue(eggs.size()==1,"One placed physical shell");eggs.getFirst().setNoGravity(true);return eggs.getFirst();
  }
  private static void cleanup(GameTestHelper h,Player p){
    h.getLevel().getEntitiesOfClass(ChaoEntity.class,area(h),c->p.getUUID().equals(c.owner)).forEach(ChaoEntity::discard);
    h.getLevel().getEntitiesOfClass(ChaoEggEntity.class,area(h),e->p.getUUID().equals(e.owner)).forEach(ChaoEggEntity::discard);
    h.getLevel().getEntitiesOfClass(ItemEntity.class,area(h)).forEach(ItemEntity::discard);p.getInventory().clearContent();
  }

  @GameTest(template="activity_test",timeoutTicks=100)
  public static void physicalLifecycleRetainsIdentityAndExactCarryAcrossBothCocoons(GameTestHelper h){
    var p=room(h);var s=state();s.points=new int[]{1255,2509,1699,21,1009};s.levels=new int[]{99,90,99,1,70};s.progress=new int[]{97,99,36,12,76};
    s.grades=new int[]{5,4,3,5,3};s.gradeA=s.grades.clone();s.gradeB=s.grades.clone();s.setTrait("reward.race",7);
    var live=new ChaoEntity[]{hatch(h,p,egg(h,p,s))};UUID actor=live[0].getUUID();String lineage=live[0].state().lineageId();ItemStack cap=hat();live[0].setItemSlot(EquipmentSlot.HEAD,cap.copy());
    live[0].state().ageTicks=GradualEvolutionRules.durationTicks(GardenConfig.CHILD_TICKS.get())-1;live[0].aiStep();
    h.assertTrue(live[0].state().adult&&live[0].state().cocoonTicks==200&&live[0].state().grades[1]==5,"Actual first Fly cocoon/rank bonus");
    live[0]=reload(h,live[0]);ticks(live[0],200);
    h.assertTrue(live[0].state().cocoonTicks==0&&live[0].state().adultAgeTicks==0,"First cocoon pauses lifespan for exactly200steps");
    for(int i=0;i<240;i++)live[0].state().onFruitEaten();h.assertTrue(live[0].state().evolutionStrength()>1.199,"Young adult reaches full adulthood without another cocoon");
    live[0].state().adultAgeTicks=GradualEvolutionRules.durationTicks(GardenConfig.ADULT_TICKS.get())-1;live[0].aiStep();
    h.assertTrue(live[0].isRebirthCocoon()&&live[0].state().cocoonTicks==200,"Actual rebirth cocoon");
    ticks(live[0],100);live[0]=reload(h,live[0]);var eggAttempts=new AtomicInteger();
    Consumer<EntityJoinLevelEvent> listener=event->{if(event.getEntity() instanceof ChaoEggEntity e&&p.getUUID().equals(e.owner)){
      eggAttempts.incrementAndGet();h.assertTrue(!live[0].releaseLifecycleItems(),"Reentrant cleanup must preserve gear already copied to the new egg");live[0].aiStep();
    }};
    NeoForge.EVENT_BUS.addListener(listener);
    try{ticks(live[0],100);}finally{NeoForge.EVENT_BUS.unregister(listener);}
    var eggs=h.getLevel().getEntitiesOfClass(ChaoEggEntity.class,area(h),e->p.getUUID().equals(e.owner));
    h.assertTrue(live[0].isRemoved()&&eggs.size()==1&&eggAttempts.get()==1&&dropped(h,cap)==0,"Rebirth commits one egg without replay or separate gear drop");
    var item=pickAndSerialize(h,p,eggs.getFirst());var placed=place(h,p,item);var next=hatch(h,p,placed);var nextState=next.state();
    h.assertTrue(next.getUUID().equals(actor)&&nextState.lineageId().equals(lineage)&&next.getName().getString().equals("Persistent life"),"Physical pickup/serialization/placement preserves body identity/name");
    h.assertTrue(Arrays.equals(nextState.points,new int[]{125,250,169,2,100})&&Arrays.equals(nextState.levels,new int[]{1,1,1,1,1})&&Arrays.equals(nextState.progress,new int[5]),"Exact ten percent, level1 and zero progress");
    h.assertTrue(Arrays.equals(nextState.grades,new int[]{5,5,3,5,3})&&nextState.intTrait("reward.race")==7&&ItemStack.isSameItemSameComponents(next.getItemBySlot(EquipmentSlot.HEAD),cap),"Grades, awards and exact equipment survive the whole physical loop");
    nextState.ageTicks=GradualEvolutionRules.durationTicks(GardenConfig.CHILD_TICKS.get())-1;next.aiStep();
    h.assertTrue(nextState.cocoonTicks==200&&nextState.grades[1]==5,"Next first evolution respects the S cap");cleanup(h,p);h.succeed();
  }

  @GameTest(template="activity_test",timeoutTicks=100)
  public static void deniedNaturalDeathReturnsKeepHeldAndWornItemsAcrossReloadAndReentry(GameTestHelper h){
    var p=room(h);var s=state();s.adult=true;s.happiness=0;s.adultAgeTicks=GradualEvolutionRules.durationTicks(GardenConfig.ADULT_TICKS.get())-1;
    var live=new ChaoEntity[]{body(h,p,s)};ItemStack held=new ItemStack(Items.STICK,3),cap=hat();live[0].setItemSlot(EquipmentSlot.MAINHAND,held.copy());live[0].setItemSlot(EquipmentSlot.HEAD,cap.copy());
    var blocked=new AtomicBoolean(true);var wornAttempts=new AtomicInteger();
    Consumer<EntityJoinLevelEvent> listener=event->{if(event.getEntity() instanceof ItemEntity i&&area(h).contains(i.position())
        &&(ItemStack.isSameItemSameComponents(i.getItem(),held)||ItemStack.isSameItemSameComponents(i.getItem(),cap))){
      if(ItemStack.isSameItemSameComponents(i.getItem(),cap))wornAttempts.incrementAndGet();
      h.assertTrue(!live[0].releaseLifecycleItems(),"Recursive cleanup cannot mutate a second stack or report completion");
      if(blocked.get())event.setCanceled(true);
    }};
    boolean natural=GardenConfig.NATURAL_DEATH.get();NeoForge.EVENT_BUS.addListener(listener);GardenConfig.NATURAL_DEATH.set(true);
    try{
      live[0].aiStep();h.assertTrue(live[0].isDeathCocoon()&&wornAttempts.get()==0,"Initial held-return callback must not eject gear before the gray cocoon");
      ticks(live[0],200);h.assertTrue(!live[0].isRemoved()&&live[0].getMainHandItem().getCount()==3&&ItemStack.isSameItemSameComponents(live[0].getItemBySlot(EquipmentSlot.HEAD),cap),"Denied terminal drops retain all real stacks");
      h.assertTrue(!live[0].canCare(p)&&dropped(h,held)==0&&dropped(h,cap)==0,"Pending death permits neither care nor phantom drops");
      live[0]=reload(h,live[0]);blocked.set(false);ticks(live[0],20);
      h.assertTrue(live[0].isRemoved()&&dropped(h,held)==3&&dropped(h,cap)==1,"Reload retry commits each exact stack once before retirement");
      live[0].releaseLifecycleItems();h.assertTrue(dropped(h,held)==3&&dropped(h,cap)==1,"Repeated cleanup does not replay committed stacks");
    }finally{GardenConfig.NATURAL_DEATH.set(natural);NeoForge.EVENT_BUS.unregister(listener);cleanup(h,p);}
    h.succeed();
  }

  @GameTest(template="activity_test",timeoutTicks=100)
  public static void committedRobotComponentDoesNotReplayWhileItsEquipmentReturnRetries(GameTestHelper h){
    var p=room(h);var s=state();OmochaoRules.initialize(s,state(),"Creator",2);var live=new ChaoEntity[]{body(h,p,s)};
    UUID actor=live[0].getUUID();ItemStack cap=hat();live[0].setItemSlot(EquipmentSlot.HEAD,cap.copy());var blocked=new AtomicBoolean(true);
    Consumer<EntityJoinLevelEvent> listener=event->{if(event.getEntity() instanceof ItemEntity i&&area(h).contains(i.position())&&ItemStack.isSameItemSameComponents(i.getItem(),cap)){
      h.assertTrue(!OmochaoLifecycle.finish(live[0],ChaoState.Transition.DIED)&&!live[0].releaseLifecycleItems(),"Recursive robotic completion is refused");
      if(blocked.get())event.setCanceled(true);
    }};
    NeoForge.EVENT_BUS.addListener(listener);
    try{
      h.assertTrue(!OmochaoLifecycle.finish(live[0],ChaoState.Transition.DIED)&&!live[0].isRemoved()&&live[0].state().robotOutputCommitted(),"One component commits while equipment denial retains the actor");
      live[0]=reload(h,live[0]);OmochaoLifecycle.resumePending(live[0]);
      h.assertTrue(!live[0].isRemoved()&&ItemStack.isSameItemSameComponents(live[0].getItemBySlot(EquipmentSlot.HEAD),cap),"Reload preserves denied equipment holder");
      blocked.set(false);OmochaoLifecycle.resumePending(live[0]);
      var outputs=h.getLevel().getEntitiesOfClass(ItemEntity.class,area(h),i->{var origin=CweRobotPartItem.origin(i.getItem());return origin.hasUUID("Robot")&&origin.getUUID("Robot").equals(actor);});
      h.assertTrue(live[0].isRemoved()&&outputs.size()==1&&outputs.getFirst().getItem().is(ChaoGarden.item("robot_component"))&&dropped(h,cap)==1,"Equipment retry must not duplicate the already committed component");
      OmochaoLifecycle.finish(live[0],ChaoState.Transition.DIED);h.assertTrue(dropped(h,cap)==1,"Discarded robot cleanup cannot duplicate its gear");
    }finally{NeoForge.EVENT_BUS.unregister(listener);cleanup(h,p);}
    h.succeed();
  }

  @GameTest(template="activity_test",timeoutTicks=100)
  public static void maximumIncubationRetriesThroughReloadPickupAndPlacementWithoutOverflow(GameTestHelper h){
    var p=room(h);var s=state();s.kindnessGenes=new int[]{1,2};s.aggressiveGenes=new int[]{2,3};s.points=new int[]{991,1234,2378,7001,9999};
    var expected=ChaoState.decode(s.encode());expected.applyHatchingMethod(true);UUID actor=UUID.randomUUID();ItemStack cap=hat();
    var e=egg(h,p,s);e.setRebornChaoId(actor);e.setHatchTreatment(ChaoEggEntity.GENTLE);var gear=new CompoundTag();gear.put("head",cap.save(h.getLevel().registryAccess()));e.setRebornEquipment(gear);
    var blocked=new AtomicBoolean(true);var attempts=new AtomicInteger();
    Consumer<EntityJoinLevelEvent> listener=event->{if(event.getEntity() instanceof ChaoEntity c&&actor.equals(c.getUUID())){attempts.incrementAndGet();if(blocked.get())event.setCanceled(true);}};
    int incubation=GardenConfig.HATCH_TICKS.get();NeoForge.EVENT_BUS.addListener(listener);GardenConfig.HATCH_TICKS.set(Integer.MAX_VALUE);
    try{
      e.setIncubation(Integer.MAX_VALUE);h.assertTrue(e.incubation()==Integer.MAX_VALUE-1,"Incubation setter clamps to reachable storage limit");
      for(int i=0;i<3;i++)e.tick();h.assertTrue(!e.isRemoved()&&attempts.get()==3&&e.incubation()==Integer.MAX_VALUE-1,"Denied maximum hatches keep retrying without overflow");
      e=reload(h,e);e.tick();var item=pickAndSerialize(h,p,e);var custom=item.get(DataComponents.CUSTOM_DATA).copyTag();custom.putInt("HatchTicks",Integer.MAX_VALUE);item.set(DataComponents.CUSTOM_DATA,CustomData.of(custom));
      e=place(h,p,item);h.assertTrue(e.incubation()==Integer.MAX_VALUE-1&&e.hatchTreatment()==ChaoEggEntity.GENTLE,"Item import preserves treatment and clamps maximum incubation");
      e.tick();e.tick();h.assertTrue(attempts.get()==6&&!e.isRemoved(),"Reload/pickup/placement retain every denied-hatch retry");
      blocked.set(false);e.tick();var babies=h.getLevel().getEntitiesOfClass(ChaoEntity.class,area(h),c->actor.equals(c.getUUID()));
      h.assertTrue(e.isRemoved()&&attempts.get()==7&&babies.size()==1,"Insertion commits exactly one same-UUID hatch");var born=babies.getFirst();
      h.assertTrue(Arrays.equals(born.state().points,s.points)&&Arrays.equals(born.state().kindnessGenes,expected.kindnessGenes)
          &&Arrays.equals(born.state().aggressiveGenes,expected.aggressiveGenes)&&ItemStack.isSameItemSameComponents(born.getItemBySlot(EquipmentSlot.HEAD),cap),"Failed attempts cannot multiply gentle gene changes or lose original stats/gear");
      e.tick();h.assertTrue(attempts.get()==7,"Removed shell never replays its hatch");
    }finally{GardenConfig.HATCH_TICKS.set(incubation);NeoForge.EVENT_BUS.unregister(listener);cleanup(h,p);}
    h.succeed();
  }
}
