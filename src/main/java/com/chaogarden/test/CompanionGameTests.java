package com.chaogarden.test;

import com.chaogarden.ChaoGarden;
import com.chaogarden.companion.AccessoriesBridge;
import com.chaogarden.companion.CompanionContent;
import com.chaogarden.core.ChaoState;
import com.chaogarden.entity.ChaoEntity;
import com.chaogarden.item.ChaoCarrierItem;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(ChaoGarden.MODID)
@PrefixGameTestTemplate(false)
public final class CompanionGameTests {
  private static final BlockPos CENTER = new BlockPos(2, 2, 2);

  @GameTest(template = "empty", timeoutTicks = 60)
  public static void emptyHandPickupKeepsIdentityAndFitsOccupiedInventory(GameTestHelper helper) {
    var world = helper.getLevel();
    var keeper = helper.makeMockPlayer(GameType.SURVIVAL);
    for (int i = 0; i < keeper.getInventory().items.size(); i++)
      keeper.getInventory().items.set(i, new ItemStack(Items.STONE, 64));
    keeper.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
    keeper.setShiftKeyDown(true);
    ChaoEntity chao = ChaoGarden.CHAO.get().create(world);
    ChaoState state = ChaoState.egg(4, true, true, 0, new Random(910));
    state.levels[ChaoState.FLY] = 12; state.variant = "nether";
    chao.setState(state); chao.owner = keeper.getUUID(); chao.setNoAi(true);
    chao.getPersistentData().putString("ForeignCreatureData", "keep this");
    UUID uuid = chao.getUUID();
    var result = CompanionContent.onInteractChao(chao, keeper, InteractionHand.MAIN_HAND);
    ItemStack held = keeper.getMainHandItem();
    helper.assertTrue(result.consumesAction() && chao.isRemoved() && ChaoCarrierItem.filled(held),
        "Sneaking empty-hand pickup did not become the selected Chao item");
    helper.assertTrue(uuid.equals(ChaoCarrierItem.storedUUID(held)), "Pickup changed the stable garden UUID");
    helper.assertTrue(ChaoCarrierItem.state(held).encode().equals(state.encode()), "Pickup changed Chao genes or care data");
    helper.assertTrue(held.getCount() == 1 && keeper.getInventory().items.stream()
        .filter(stack -> stack.is(Items.STONE) && stack.getCount() == 64).count() == 35,
        "Pickup displaced items from an otherwise full inventory");
    ChaoEntity another = ChaoGarden.CHAO.get().create(world); another.owner = UUID.randomUUID();
    ItemStack empty = new ItemStack(ChaoGarden.item("chao_carrier"));
    helper.assertTrue(!ChaoCarrierItem.capture(empty, another, keeper) && !another.isRemoved()
        && !ChaoCarrierItem.filled(empty), "Another keeper's Chao could be captured");
    another.discard(); keeper.getInventory().clearContent(); helper.succeed();
  }

  @GameTest(template = "empty", timeoutTicks = 60)
  public static void releaseKeepsUUIDAndRejectsCopiedCarrier(GameTestHelper helper) {
    var world = helper.getLevel(); var keeper = helper.makeMockPlayer(GameType.SURVIVAL);
    helper.setBlock(CENTER.below(), Blocks.STONE); var position = helper.absolutePos(CENTER);
    ChaoEntity chao = ChaoGarden.CHAO.get().create(world); chao.owner = keeper.getUUID();
    chao.setNoAi(true); chao.moveTo(position.getX()+.5, position.getY(), position.getZ()+.5, 0, 0);
    world.addFreshEntity(chao); UUID uuid = chao.getUUID();
    ItemStack carrier = new ItemStack(ChaoGarden.item("chao_carrier"));
    helper.assertTrue(ChaoCarrierItem.capture(carrier, chao, keeper), "Capture failed");
    CompoundTag data = ChaoCarrierItem.data(carrier); data.putString("ForeignItemData", "untouched");
    ChaoCarrierItem.setData(carrier, data); ItemStack copied = carrier.copy();
    keeper.setItemInHand(InteractionHand.MAIN_HAND, carrier);
    var hit = new BlockHitResult(Vec3.atCenterOf(position.below()), Direction.UP, position.below(), false);
    helper.assertTrue(carrier.getItem().useOn(new UseOnContext(keeper, InteractionHand.MAIN_HAND, hit)).consumesAction(),
        "Original carrier failed to release its Chao");
    helper.assertTrue(carrier.isEmpty(),
        "Release left an empty carrier instead of consuming the held Chao item");
    EntityCheck.assertOne(helper, uuid);
    keeper.setItemInHand(InteractionHand.MAIN_HAND, copied);
    helper.assertTrue(copied.getItem().useOn(new UseOnContext(keeper, InteractionHand.MAIN_HAND, hit)) == InteractionResult.FAIL
        && ChaoCarrierItem.filled(copied), "A copied carrier duplicated the same Chao UUID");
    var restored = (ChaoEntity) world.getEntity(uuid); restored.discard(); keeper.getInventory().clearContent();
    helper.succeed();
  }

  @GameTest(template = "empty", timeoutTicks = 60)
  public static void creativeReleaseConsumesHeldChaoAndBlockedReleaseKeepsIt(GameTestHelper helper) {
    var world = helper.getLevel(); var keeper = helper.makeMockPlayer(GameType.CREATIVE);
    helper.setBlock(CENTER.below(), Blocks.STONE);
    var position = helper.absolutePos(CENTER);
    ChaoEntity chao = ChaoGarden.CHAO.get().create(world); chao.owner = keeper.getUUID();
    chao.setNoAi(true); chao.setCustomName(net.minecraft.network.chat.Component.literal("Still My Chao"));
    UUID uuid = chao.getUUID(); String state = chao.state().encode();
    ItemStack held = new ItemStack(ChaoGarden.item("chao_carrier"));
    helper.assertTrue(ChaoCarrierItem.capture(held, chao, keeper), "Creative pickup failed");
    keeper.setItemInHand(InteractionHand.MAIN_HAND, held);
    var hit = new BlockHitResult(Vec3.atCenterOf(position.below()), Direction.UP, position.below(), false);
    helper.setBlock(CENTER, Blocks.STONE);
    helper.assertTrue(held.getItem().useOn(new UseOnContext(keeper, InteractionHand.MAIN_HAND, hit)) == InteractionResult.FAIL
        && ChaoCarrierItem.filled(held) && held.getCount() == 1,
        "A blocked release consumed or cleared the saved Chao");
    helper.setBlock(CENTER, Blocks.AIR);
    helper.assertTrue(held.getItem().useOn(new UseOnContext(keeper, InteractionHand.MAIN_HAND, hit)).consumesAction()
        && held.isEmpty(), "Creative release left a reusable container or filled copy");
    var restored = (ChaoEntity) world.getEntity(uuid);
    helper.assertTrue(restored != null && restored.state().encode().equals(state)
        && restored.getName().getString().equals("Still My Chao"),
        "Creative release lost the Chao identity, name or saved state");
    restored.discard(); keeper.getInventory().clearContent(); helper.succeed();
  }

  @GameTest(template = "empty", timeoutTicks = 60)
  public static void legacyEmptyItemsDisappearWhileSavedChaoRemain(GameTestHelper helper) {
    var keeper = helper.makeMockPlayer(GameType.SURVIVAL); var world = helper.getLevel();
    ItemStack empty = new ItemStack(ChaoGarden.item("chao_carrier"));
    empty.getItem().inventoryTick(empty, world, keeper, 0, false);
    helper.assertTrue(empty.isEmpty(), "An old empty carrier remained in the inventory");
    ChaoEntity chao = ChaoGarden.CHAO.get().create(world); chao.owner = keeper.getUUID();
    UUID uuid = chao.getUUID(); ItemStack filled = new ItemStack(ChaoGarden.item("chao_carrier"));
    helper.assertTrue(ChaoCarrierItem.capture(filled, chao, keeper), "Legacy saved-item setup failed");
    filled.getItem().inventoryTick(filled, world, keeper, 0, false);
    helper.assertTrue(!filled.isEmpty() && ChaoCarrierItem.filled(filled)
        && uuid.equals(ChaoCarrierItem.storedUUID(filled)),
        "Compatibility cleanup removed a saved Chao");
    helper.assertTrue(!ChaoGarden.visibleInCreative(filled.getItem()), "A standalone carrier remains in creative entries");
    helper.succeed();
  }

  private static final class EntityCheck {
    static void assertOne(GameTestHelper helper, UUID uuid) {
      helper.assertTrue(helper.getLevel().getEntity(uuid) instanceof ChaoEntity,
          "Released Chao did not preserve its original UUID");
    }
  }

  @GameTest(template = "empty", timeoutTicks = 160)
  public static void realAccessoriesEquipDefeatRecoveryAndUnequip(GameTestHelper helper) throws Exception {
    if (!AccessoriesBridge.available()) { helper.succeed(); return; }
    var world = helper.getLevel(); ServerPlayer keeper = TestConnections.makeServerPlayer(helper);
    var position = helper.absolutePos(CENTER); keeper.teleportTo(position.getX(), position.getY(), position.getZ());
    ChaoEntity chao = ChaoGarden.CHAO.get().create(world); chao.owner = keeper.getUUID();
    chao.state().adult = true; chao.state().happiness = 80;
    chao.state().levels[ChaoState.POWER] = chao.state().levels[ChaoState.FLY] = 15;
    chao.state().points[ChaoState.POWER] = chao.state().points[ChaoState.FLY] = 300;
    chao.sync(); UUID uuid = chao.getUUID();
    ItemStack carrier = new ItemStack(ChaoGarden.item("chao_carrier"));
    helper.assertTrue(ChaoCarrierItem.capture(carrier, chao, keeper), "Accessories test capture failed");
    Class<?> capabilityType = Class.forName("io.wispforest.accessories.api.AccessoriesCapability");
    Class<?> containerType = Class.forName("io.wispforest.accessories.api.AccessoriesContainer");
    Class<?> slotType = Class.forName("io.wispforest.accessories.api.slot.SlotReference");
    Object capability = capabilityType.getMethod("get", LivingEntity.class).invoke(null, keeper);
    Map<?, ?> containers = (Map<?, ?>) capabilityType.getMethod("getContainers").invoke(capability);
    Object container = containers.get("chao_companion");
    helper.assertTrue(container != null, "Real Accessories did not load the Chao companion slot/entity binding");
    Container inventory = (Container) containerType.getMethod("getAccessories").invoke(container);
    Object reference = slotType.getMethod("of", LivingEntity.class, String.class, int.class)
        .invoke(null, keeper, "chao_companion", 0);
    Class<?> accessoryType = Class.forName("io.wispforest.accessories.api.Accessory");
    Object accessory = Class.forName("io.wispforest.accessories.api.AccessoriesAPI")
        .getMethod("getAccessory", Item.class).invoke(null, carrier.getItem());
    Method canEquip = accessoryType.getMethod("canEquip", ItemStack.class, slotType);
    helper.assertTrue(Boolean.TRUE.equals(canEquip.invoke(accessory, carrier, reference)), "Actual Accessory callback rejected an adult");
    ItemStack young = carrier.copy(); CompoundTag entity = ChaoCarrierItem.entityData(young);
    ChaoState youngState = ChaoState.decode(entity.getString("Chao")); youngState.adult = false;
    entity.putString("Chao", youngState.encode()); ChaoCarrierItem.storeEntity(young, entity);
    helper.assertTrue(Boolean.TRUE.equals(canEquip.invoke(accessory, young, reference)), "Actual Accessory callback rejected an eligible child");
    helper.assertTrue(!com.chaogarden.companion.CompanionRules.adult(youngState)
        &&!com.chaogarden.companion.CompanionRules.canElytra(youngState),"Equipping a child granted adult flight eligibility");
    inventory.setItem(0, carrier); containerType.getMethod("markChanged", boolean.class).invoke(container, false);
    ItemStack pendingGift=new ItemStack(Items.DIAMOND_SWORD);
    pendingGift.setDamageValue(11);
    pendingGift.set(DataComponents.CUSTOM_NAME,net.minecraft.network.chat.Component.literal("Pending companion gift"));
    helper.runAfterDelay(12, () -> {
      ChaoEntity body = (ChaoEntity) world.getEntity(uuid);
      helper.assertTrue(body != null && CompanionContent.isManaged(body) && !body.shouldBeSaved(),
          "Actual Accessories tick did not summon exactly one temporary companion");
      keeper.getAbilities().instabuild=false;
      keeper.setItemInHand(InteractionHand.MAIN_HAND,pendingGift.copy());
      body.offerItem(keeper,InteractionHand.MAIN_HAND);
      helper.assertTrue(keeper.getMainHandItem().isEmpty() && body.isHandlingItem(),"Companion did not hold its real offered gift");
      assertPendingGift(helper,carrier,pendingGift,"Immediate gift transfer");
      int before = Arrays.stream(body.state().levels).sum();
      body.hurt(body.damageSources().genericKill(), Float.MAX_VALUE);
      helper.assertTrue(body.isRemoved() && CompanionContent.remaining(carrier, world.getServer()) > 1180,
          "Companion defeat did not disable the body for one minute");
      helper.assertTrue(Arrays.stream(ChaoCarrierItem.state(carrier).levels).sum() == before-1,
          "Companion defeat did not lose exactly one trained level");
      assertPendingGift(helper,carrier,pendingGift,"Defeat snapshot");
      helper.runAfterDelay(4, () -> {
        helper.assertTrue(world.getEntity(uuid) == null || world.getEntity(uuid).isRemoved(),
            "A disabled companion respawned before its recovery time");
        CompoundTag cooldown = ChaoCarrierItem.data(carrier);
        cooldown.putLong(CompanionContent.DISABLED_UNTIL, world.getServer().overworld().getGameTime()-1);
        ChaoCarrierItem.setData(carrier, cooldown);
        helper.runAfterDelay(8, () -> {
          ChaoEntity recovered = (ChaoEntity) world.getEntity(uuid);
          helper.assertTrue(recovered != null && !recovered.isRemoved(), "Actual accessory did not resummon after recovery");
          helper.assertTrue(Arrays.stream(recovered.state().levels).sum() == before-1, "Recovery applied another defeat penalty");
          helper.assertTrue(ItemStack.matches(recovered.getMainHandItem(),pendingGift)
              && recovered.isHandlingItem(),"Recovered body lost its pending gift or inspection phase");
          recovered.state().happiness = 42;
          try {
            accessoryType.getMethod("canUnequip", ItemStack.class, slotType).invoke(accessory, carrier, reference);
          } catch (ReflectiveOperationException exception) { throw new RuntimeException(exception); }
          helper.assertTrue(ChaoCarrierItem.state(carrier).happiness == 42,
              "Actual canUnequip did not snapshot into the live stack before a cursor copy");
          // A direct container removal bypasses canUnequip. Its copied cursor
          // stack still needs the final state taken by the real onUnequip.
          recovered.state().happiness = 43;
          ItemStack removed = inventory.removeItem(0, 1);
          keeper.containerMenu.setCarried(removed);
          try { containerType.getMethod("markChanged", boolean.class).invoke(container, false); }
          catch (ReflectiveOperationException exception) { throw new RuntimeException(exception); }
          helper.runAfterDelay(6, () -> {
            helper.assertTrue(recovered.isRemoved() && ChaoCarrierItem.filled(removed),
                "Unequipping did not recall the world body into its existing item");
            helper.assertTrue(ChaoCarrierItem.state(removed).happiness == 43,
                "A copied Chao left on the menu cursor lost its final unequip snapshot");
            assertPendingGift(helper,removed,pendingGift,"Recalled cursor snapshot");
            helper.assertTrue(!AccessoriesBridge.equipped(keeper).contains(removed), "Actual slot still held removed companion");
            helper.runAfterDelay(12, () -> {
              helper.assertTrue(ChaoCarrierItem.state(keeper.containerMenu.getCarried()).happiness == 43,
                  "An older pending snapshot overwrote the recalled cursor item");
              keeper.containerMenu.setCarried(ItemStack.EMPTY);
              inventory.setItem(0, removed);
              try { containerType.getMethod("markChanged", boolean.class).invoke(container, false); }
              catch (ReflectiveOperationException exception) { throw new RuntimeException(exception); }
              helper.runAfterDelay(8, () -> {
                ChaoEntity lastBody = (ChaoEntity) world.getEntity(uuid);
                helper.assertTrue(lastBody != null && !lastBody.isRemoved(), "Cursor Chao could not be equipped again");
                int previousLevels = Arrays.stream(lastBody.state().levels).sum();
                ItemStack cursor = inventory.removeItem(0, 1);
                keeper.containerMenu.setCarried(cursor);
                lastBody.hurt(lastBody.damageSources().genericKill(), Float.MAX_VALUE);
                helper.runAfterDelay(4, () -> {
                  helper.assertTrue(lastBody.isRemoved() && CompanionContent.remaining(cursor, world.getServer()) > 1180,
                      "Defeat after a slot split lost the item recovery timer");
                  helper.assertTrue(ChaoCarrierItem.state(cursor).happiness == 33
                      && Arrays.stream(ChaoCarrierItem.state(cursor).levels).sum() == previousLevels-1,
                      "Defeat after a slot split lost or repeated its happiness/level penalty");
                  helper.assertTrue(world.getEntity(uuid) == null || world.getEntity(uuid).isRemoved(),
                      "A split-and-defeated Chao was duplicated or resummoned early");
                  assertPendingGift(helper,cursor,pendingGift,"Split and disabled snapshot");
                  helper.assertTrue(world.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,
                      new net.minecraft.world.phys.AABB(position).inflate(12),drop->ItemStack.isSameItemSameComponents(drop.getItem(),pendingGift)).isEmpty(),
                      "Projection defeat or recall duplicated a pending gift into the world");
                  keeper.containerMenu.setCarried(ItemStack.EMPTY); keeper.getInventory().setItem(0, cursor);
                  keeper.server.getPlayerList().remove(keeper); helper.succeed();
                });
              });
            });
          });
        });
      });
    });
  }

  private static void assertPendingGift(GameTestHelper helper,ItemStack carrier,ItemStack expected,String stage) {
    var saved=ChaoGarden.CHAO.get().create(helper.getLevel());
    saved.load(ChaoCarrierItem.entityData(carrier));
    helper.assertTrue(ItemStack.matches(saved.getMainHandItem(),expected) && saved.isHandlingItem(),
        stage+" lost or modified the pending real item");saved.discard();
  }
}
