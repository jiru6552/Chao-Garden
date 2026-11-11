package com.chaogarden.test;

import com.chaogarden.ChaoGarden;
import com.chaogarden.core.ChaoState;
import com.chaogarden.entity.ChaoEntity;
import com.chaogarden.interaction.ChaoPickupItemGoal;
import com.chaogarden.item.ChaoCarrierItem;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** Real tick-driven transfers, decisions, saves and returns; not simulated item copies. */
@GameTestHolder(ChaoGarden.MODID)
@PrefixGameTestTemplate(false)
public final class ItemHandlingGameTests {
  private static final BlockPos AT=new BlockPos(2,2,2);
  private static Player keeper(GameTestHelper h) {
    var p=h.makeMockPlayer(GameType.SURVIVAL);p.getAbilities().instabuild=false;p.getAbilities().mayBuild=true;return p;
  }
  private static ChaoEntity chao(GameTestHelper h,Player p,int fullness) {
    h.setBlock(AT.below(),Blocks.STONE);
    var c=ChaoGarden.CHAO.get().create(h.getLevel());
    var state=new ChaoState();state.hunger=fullness;c.setState(state);c.owner=p.getUUID();
    c.setNoAi(true);c.setNoGravity(true);
    var at=h.absolutePos(AT);c.moveTo(at.getX()+.5,at.getY(),at.getZ()+.5,0,0);
    h.getLevel().addFreshEntity(c);return c;
  }
  private static ItemStack namedSword(String label) {
    var stack=new ItemStack(Items.DIAMOND_SWORD);stack.setDamageValue(17);
    stack.set(DataComponents.CUSTOM_NAME,Component.literal(label));
    var data=new CompoundTag();data.putString("KeepThisPayload","component-preservation");
    stack.set(DataComponents.CUSTOM_DATA,CustomData.of(data));return stack;
  }
  private static List<ItemEntity> dropped(GameTestHelper h,ItemStack expected) {
    return h.getLevel().getEntitiesOfClass(ItemEntity.class,new AABB(h.absolutePos(AT)).inflate(6),
        e->e.isAlive() && ItemStack.isSameItemSameComponents(e.getItem(),expected));
  }

  @GameTest(template="empty",timeoutTicks=180)
  public static void hungryFruitIsHeldThenEatenAndCommittedOnceAcrossReload(GameTestHelper h) {
    var p=keeper(h);var c=chao(h,p,20);var fruit=new ItemStack(ChaoGarden.item("garden_fruit"),2);
    p.setItemInHand(InteractionHand.MAIN_HAND,fruit);c.mobInteract(p,InteractionHand.MAIN_HAND);
    h.assertTrue(fruit.getCount()==1 && c.getMainHandItem().getCount()==1 && c.state().fruitCount==0,
        "Offer must transfer one fruit without granting instant benefits");
    var live=new ChaoEntity[]{c};var saved=new CompoundTag();
    h.runAtTickTime(20,()->h.assertTrue(c.getItemAction()==ChaoEntity.ItemAction.THINK
        && c.state().fruitCount==0 && c.state().hunger==20,"Fruit applied during the thinking phase"));
    h.runAtTickTime(50,()->{
      h.assertTrue(c.getItemAction()==ChaoEntity.ItemAction.EAT && c.state().fruitCount==0,
          "Chao did not hold its fruit during the eating animation");
      c.saveWithoutId(saved);c.discard();
    });
    h.runAtTickTime(51,()->{
      var restored=ChaoGarden.CHAO.get().create(h.getLevel());restored.load(saved);
      h.assertTrue(restored.getItemAction()==ChaoEntity.ItemAction.EAT && restored.getMainHandItem().getCount()==1,
          "Pending real fruit was lost or duplicated on load");
      h.getLevel().addFreshEntity(restored);live[0]=restored;
    });
    h.runAtTickTime(110,()->{
      h.assertTrue(live[0].state().fruitCount==1 && live[0].state().hunger>20 && live[0].getMainHandItem().isEmpty(),
          "Eating did not consume exactly one fruit and commit once");
      for(String key:List.copyOf(saved.getAllKeys()))saved.remove(key);
      live[0].saveWithoutId(saved);live[0].discard();
    });
    h.runAtTickTime(111,()->{var restored=ChaoGarden.CHAO.get().create(h.getLevel());restored.load(saved);
      h.getLevel().addFreshEntity(restored);live[0]=restored;});
    h.runAtTickTime(150,()->{
      h.assertTrue(live[0].state().fruitCount==1 && live[0].getMainHandItem().isEmpty() && !live[0].isHandlingItem(),
          "A completed meal was replayed after reload");live[0].discard();h.succeed();
    });
  }

  @GameTest(template="empty",timeoutTicks=120)
  public static void fullChaoConsidersAndReturnsFruitWithoutBenefits(GameTestHelper h) {
    var p=keeper(h);var c=chao(h,p,100);var fruit=new ItemStack(ChaoGarden.item("garden_fruit"));
    var expected=fruit.copy();p.setItemInHand(InteractionHand.MAIN_HAND,fruit);c.mobInteract(p,InteractionHand.MAIN_HAND);
    h.assertTrue(fruit.isEmpty() && c.isHandlingItem() && c.state().fruitCount==0,"Full Chao did not initially hold its offer");
    h.runAtTickTime(40,()->h.assertTrue(c.getItemAction()==ChaoEntity.ItemAction.REJECT,
        "Full Chao did not decide to refuse the fruit"));
    h.runAtTickTime(80,()->{
      h.assertTrue(c.state().fruitCount==0 && c.state().hunger==100 && c.getMainHandItem().isEmpty(),
          "Refused fruit changed stats or remained in the hands");
      h.assertTrue(dropped(h,expected).stream().mapToInt(e->e.getItem().getCount()).sum()==1,
          "Refused fruit was lost or returned more than once");c.discard();h.succeed();
    });
  }

  @GameTest(template="empty",timeoutTicks=160)
  public static void unknownGiftSurvivesCarrierAndComparisonFlagsPersist(GameTestHelper h) {
    var p=keeper(h);var c=chao(h,p,70);var gift=namedSword("Unknown carrier gift");var expected=gift.copy();
    c.setOfficialComparison(true);c.setSourceReferenceKey("golden");
    p.setItemInHand(InteractionHand.MAIN_HAND,gift);c.mobInteract(p,InteractionHand.MAIN_HAND);
    var carrier=new ItemStack(ChaoGarden.item("chao_carrier"));var uuid=c.getUUID();
    h.runAtTickTime(20,()->{
      h.assertTrue(ChaoCarrierItem.capture(carrier,c,p),"Could not carry Chao holding a real item");
      var data=ChaoCarrierItem.entityData(carrier);
      h.assertTrue(data.getBoolean("OfficialComparison") && data.getString("SourceReferenceRig").equals("golden"),
          "Comparison choice was lost from the carrier snapshot");
    });
    h.runAtTickTime(21,()->{
      var floor=h.absolutePos(AT.below());
      p.setItemInHand(InteractionHand.MAIN_HAND,carrier);
      var hit=new BlockHitResult(Vec3.atCenterOf(floor).add(0,.5,0),Direction.UP,floor,false);
      h.assertTrue(carrier.getItem().useOn(new UseOnContext(p,InteractionHand.MAIN_HAND,hit)).consumesAction()
          && carrier.isEmpty(),"Carrier release failed or duplicated the captured Chao");
      var released=(ChaoEntity)h.getLevel().getEntity(uuid);
      h.assertTrue(released!=null && released.officialComparison() && released.sourceReferenceKey().equals("golden")
          && ItemStack.matches(released.getMainHandItem(),expected),"Released Chao lost gift components or comparison mode");
    });
    h.runAtTickTime(120,()->{
      h.assertTrue(dropped(h,expected).stream().mapToInt(e->e.getItem().getCount()).sum()==1,
          "Unknown gift was lost, modified or duplicated by carrying/refusal");
      var released=h.getLevel().getEntity(uuid);if(released!=null)released.discard();h.succeed();
    });
  }

  @GameTest(template="empty",timeoutTicks=180)
  public static void droppedItemsRespectPickupDelayAndAreInspectedWithoutDeletion(GameTestHelper h) {
    var p=keeper(h);var c=chao(h,p,70);c.setNoAi(false);c.setNoGravity(false);
    c.goalSelector.removeAllGoals(g->!(g instanceof ChaoPickupItemGoal));
    // ItemEntity's ordinary constructor launches drops with random horizontal
    // velocity. A one-block pedestal lets that item fall away before its
    // pickup delay expires, which would test the fixture rather than the AI.
    for(int x=-2;x<=2;x++)for(int z=-2;z<=2;z++)h.setBlock(AT.offset(x,-1,z),Blocks.STONE);
    var expected=namedSword("Unknown dropped item");
    var drop=new ItemEntity(h.getLevel(),c.getX(),c.getY()+.1,c.getZ(),expected.copy());
    drop.setDeltaMovement(Vec3.ZERO);
    drop.setPickUpDelay(40);drop.setTarget(c.getUUID());h.getLevel().addFreshEntity(drop);
    h.runAtTickTime(20,()->h.assertTrue(c.getMainHandItem().isEmpty() && drop.isAlive(),"Pickup delay was bypassed"));
    h.runAtTickTime(65,()->h.assertTrue(!drop.isAlive() && c.getItemAction()==ChaoEntity.ItemAction.INSPECT
        && ItemStack.matches(c.getMainHandItem(),expected),"Dropped item was not transferred into inspection: chao="+c.position()
        +" ground="+c.onGround()+" noAi="+c.isNoAi()+" phase="+c.getItemAction()+" held="+c.getMainHandItem()
        +" dropAlive="+drop.isAlive()+" drop="+drop.position()+" distance="+c.distanceToSqr(drop)
        +" eligible="+c.canInspectNearbyItems()+" itemEligible="+c.mayInspectDroppedItem(drop)
        +" sight="+c.hasLineOfSight(drop)+" goals="+c.goalSelector.getAvailableGoals().stream()
            .map(g->g.getGoal().getClass().getSimpleName()+":"+g.isRunning()).toList()));
    h.runAtTickTime(150,()->{
      var returns=dropped(h,expected);
      h.assertTrue(returns.stream().mapToInt(e->e.getItem().getCount()).sum()==1 && c.getMainHandItem().isEmpty(),
          "Unknown ground item was deleted, duplicated or immediately picked up again");
      h.assertTrue(returns.getFirst().getPersistentData().getLong("ChaoRejectedUntil")>h.getLevel().getGameTime(),
          "Rejected item lacked the shared no-repickup grace period");c.discard();h.succeed();
    });
  }

  @GameTest(template="empty",timeoutTicks=60)
  public static void heldItemsRecoverOnceOnDeathEvenWithMobLootDisabled(GameTestHelper h) {
    var p=TestConnections.makeSurvivalServerPlayer(h);var c=chao(h,p,70);
    var gift=namedSword("Gift survives death");var expected=gift.copy();
    p.setItemInHand(InteractionHand.MAIN_HAND,gift);c.mobInteract(p,InteractionHand.MAIN_HAND);
    h.assertTrue(gift.isEmpty() && ItemStack.isSameItemSameComponents(c.getMainHandItem(),expected),
        "Death test must begin with the real gift held by the Chao");
    boolean old=h.getLevel().getGameRules().getBoolean(GameRules.RULE_DOMOBLOOT);
    try {
      h.getLevel().getGameRules().getRule(GameRules.RULE_DOMOBLOOT).set(false,h.getLevel().getServer());
      c.kill();
    } finally {
      h.getLevel().getGameRules().getRule(GameRules.RULE_DOMOBLOOT).set(old,h.getLevel().getServer());
    }
    h.runAtTickTime(3,()->{
      try {
        var recovery=com.chaogarden.recovery.ChaoRecovery.get(p.server);
        recovery.retry(p.server);recovery.retry(p.server);
        var carriers=p.getInventory().items.stream()
            .filter(s->c.getUUID().equals(ChaoCarrierItem.storedUUID(s))).toList();
        h.assertTrue(c.isRemoved() && carriers.size()==1 && carriers.getFirst().getCount()==1,
            "Damage death must recover exactly one Chao, including after repeated delivery attempts");
        var restored=ChaoGarden.CHAO.get().create(h.getLevel());
        restored.load(ChaoCarrierItem.entityData(carriers.getFirst()));
        h.assertTrue(restored.getHealth()>0 && ItemStack.matches(restored.getMainHandItem(),expected),
            "Recovered Chao must retain the exact held item, durability and components");
        h.assertTrue(dropped(h,expected).isEmpty(),"Recovery must not also drop a loose copy of the held item");
        h.succeed();
      } finally {p.server.getPlayerList().remove(p);}
    });
  }

  @GameTest(template="empty",timeoutTicks=60)
  public static void terminalHeldItemsDropOnceWithMobLootDisabled(GameTestHelper h) {
    var p=keeper(h);var c=chao(h,p,70);var gift=namedSword("Terminal held gift");var expected=gift.copy();
    p.setItemInHand(InteractionHand.MAIN_HAND,gift);c.mobInteract(p,InteractionHand.MAIN_HAND);
    h.assertTrue(gift.isEmpty() && ItemStack.matches(c.getMainHandItem(),expected),"Chao must hold the real gift");
    // Terminal lifecycle outcomes intentionally bypass damage recovery.
    c.state().dead=true;
    boolean old=h.getLevel().getGameRules().getBoolean(GameRules.RULE_DOMOBLOOT);
    try {
      h.getLevel().getGameRules().getRule(GameRules.RULE_DOMOBLOOT).set(false,h.getLevel().getServer());
      c.kill();
    } finally {
      h.getLevel().getGameRules().getRule(GameRules.RULE_DOMOBLOOT).set(old,h.getLevel().getServer());
    }
    h.runAtTickTime(3,()->{
      h.assertTrue(c.isDeadOrDying() && c.getMainHandItem().isEmpty(),"Terminal death must release the held item");
      int returned=dropped(h,expected).stream().mapToInt(e->e.getItem().getCount()).sum();
      h.assertTrue(returned==1,"Terminal death must return exactly one unchanged held item; found "+returned);
      c.discard();h.succeed();
    });
  }

  @GameTest(template="empty",timeoutTicks=130)
  public static void driveWithNoRemainingBenefitsIsReturnedInsteadOfConsumed(GameTestHelper h) {
    var p=keeper(h);var c=chao(h,p,70);c.state().levels[ChaoState.RUN]=99;
    c.state().runPower=-1f;c.state().luck=4000;c.state().intelligence=4000;c.state().happiness=100;
    var drive=new ItemStack(ChaoGarden.item("run_drive"));var expected=drive.copy();
    p.setItemInHand(InteractionHand.MAIN_HAND,drive);c.mobInteract(p,InteractionHand.MAIN_HAND);
    h.runAtTickTime(85,()->{
      h.assertTrue(c.state().driveCount==0 && dropped(h,expected).stream().mapToInt(e->e.getItem().getCount()).sum()==1,
          "Chao consumed a drive after experience, influence and care rewards were capped");c.discard();h.succeed();
    });
  }

  @GameTest(template="empty",timeoutTicks=160)
  public static void cappedDriveStillRebalancesEvolutionAndCommitsOnlyOnce(GameTestHelper h) {
    var p=keeper(h);var c=chao(h,p,70);c.state().adult=true;
    c.state().levels[ChaoState.RUN]=99;c.state().runPower=1f;
    c.state().luck=4000;c.state().happiness=100;
    int points=c.state().points[ChaoState.RUN];
    var drive=new ItemStack(ChaoGarden.item("run_drive"));
    p.setItemInHand(InteractionHand.MAIN_HAND,drive);c.mobInteract(p,InteractionHand.MAIN_HAND);
    h.assertTrue(drive.isEmpty() && c.state().driveCount==0 && c.state().runPower==1f,
        "A capped drive must transfer without instantly changing evolution");
    h.runAtTickTime(55,()->h.assertTrue(c.getItemAction()==ChaoEntity.ItemAction.TRAIN
        && c.state().runPower==1f && c.state().driveCount==0,
        "A useful capped drive was refused or applied before its training animation finished"));
    var live=new ChaoEntity[]{c};var saved=new CompoundTag();
    h.runAtTickTime(90,()->{
      h.assertTrue(c.state().driveCount==1 && Math.abs(c.state().runPower-.975f)<.00001f
          && c.state().levels[ChaoState.RUN]==99 && c.state().points[ChaoState.RUN]==points
          && c.getMainHandItem().isEmpty(),
          "A capped skill's drive failed to apply its independent adult evolution influence once");
      c.saveWithoutId(saved);c.discard();
    });
    h.runAtTickTime(91,()->{
      var restored=ChaoGarden.CHAO.get().create(h.getLevel());restored.load(saved);
      h.getLevel().addFreshEntity(restored);live[0]=restored;
    });
    h.runAtTickTime(135,()->{
      h.assertTrue(live[0].state().driveCount==1 && Math.abs(live[0].state().runPower-.975f)<.00001f
          && live[0].getMainHandItem().isEmpty() && !live[0].isHandlingItem(),
          "The completed evolution adjustment replayed after reload");
      live[0].discard();h.succeed();
    });
  }

  @GameTest(template="empty",timeoutTicks=120)
  public static void refusingChaoReturnsHatWithoutReplacingExistingEquipment(GameTestHelper h) {
    var p=keeper(h);var c=chao(h,p,70);c.state().happiness=-20;
    var old=new ItemStack(ChaoGarden.item("apple_hat"));
    old.set(DataComponents.CUSTOM_NAME,Component.literal("Keep my original hat"));
    c.setItemSlot(net.minecraft.world.entity.EquipmentSlot.HEAD,old.copy());
    var offered=new ItemStack(ChaoGarden.item("pan_hat"));
    offered.set(DataComponents.CUSTOM_NAME,Component.literal("Refused new hat"));var expected=offered.copy();
    p.setItemInHand(InteractionHand.MAIN_HAND,offered);c.mobInteract(p,InteractionHand.MAIN_HAND);
    WardrobeGameTests.decided(c,false);
    h.assertTrue(offered.isEmpty() && ItemStack.matches(c.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.HEAD),old),
        "Hat gift replaced existing equipment before consideration");
    h.runAtTickTime(85,()->{
      h.assertTrue(ItemStack.matches(c.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.HEAD),old)
          && dropped(h,expected).stream().mapToInt(e->e.getItem().getCount()).sum()==1,
          "Refused hat changed or duplicated either hat");c.discard();h.succeed();
    });
  }
}
