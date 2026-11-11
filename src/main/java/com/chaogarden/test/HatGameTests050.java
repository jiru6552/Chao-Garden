package com.chaogarden.test;

import com.chaogarden.ChaoGarden;
import com.chaogarden.care.ChaoCareServices;
import com.chaogarden.core.ChaoState;
import com.chaogarden.entity.ChaoEntity;
import com.chaogarden.item.ChaoCarrierItem;
import com.chaogarden.market.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.*;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.gametest.*;

/** Real server item/component, consent, return and old-save transactions. */
@GameTestHolder("chao_hats50") @PrefixGameTestTemplate(false)
public final class HatGameTests050 {
  private static final BlockPos AT=new BlockPos(3,2,3);
  private static final List<Item> HATS=List.of(Items.CARVED_PUMPKIN,Items.MELON,Items.SKELETON_SKULL);
  private static void cleanup(GameTestHelper h,Runnable action){h.testInfo.addListener(new GameTestListener(){
    public void testStructureLoaded(GameTestInfo i){}
    public void testPassed(GameTestInfo i,GameTestRunner r){action.run();}
    public void testFailed(GameTestInfo i,GameTestRunner r){action.run();}
    public void testAddedForRerun(GameTestInfo a,GameTestInfo b,GameTestRunner r){action.run();}
  });}
  private static ServerPlayer keeper(GameTestHelper h){
    var p=TestConnections.makeSurvivalServerPlayer(h);p.getInventory().clearContent();
    p.teleportTo(h.absolutePos(AT).getX()+.5,h.absolutePos(AT).getY(),h.absolutePos(AT).getZ()+1.5);p.setNoGravity(true);
    cleanup(h,()->p.server.getPlayerList().remove(p));return p;
  }
  private static ChaoEntity actor(GameTestHelper h,ServerPlayer p,boolean adult){
    h.setBlock(AT.below(),Blocks.STONE);var c=ChaoGarden.CHAO.get().create(h.getLevel());
    var s=ChaoState.egg(0,false,true,0,new Random(24));s.adult=adult;s.happiness=100;s.hunger=80;s.energy=100;s.setTrait("foreign.saved","keep me");
    c.setState(s);c.owner=p.getUUID();c.setNoAi(true);c.setNoGravity(true);c.setStaying(true);
    c.moveTo(Vec3.atBottomCenterOf(h.absolutePos(AT)));h.getLevel().addFreshEntity(c);cleanup(h,c::discard);return c;
  }
  private static ItemStack named(Item item,int count){
    var result=new ItemStack(item,count);result.set(DataComponents.CUSTOM_NAME,Component.literal("A real named hat"));
    var opaque=new CompoundTag();opaque.putString("ForeignMod","preserve this");opaque.putIntArray("OpaqueNumbers",new int[]{4,-7,0,900});
    result.set(DataComponents.CUSTOM_DATA,CustomData.of(opaque));return result;
  }
  private static int count(ServerPlayer p,Item item){return p.getInventory().countItem(item);}
  private static void accepted(ChaoEntity c){WardrobeGameTests.finishAccepted(c);}
  private static ChaoEntity reload(GameTestHelper h,ChaoEntity original){
    var saved=new CompoundTag();original.saveWithoutId(saved);var c=ChaoGarden.CHAO.get().create(h.getLevel());c.load(saved);return c;
  }
  @GameTestGenerator public static Collection<TestFunction> cases(){
    var result=new ArrayList<TestFunction>();
    for(var item:HATS)for(boolean adult:new boolean[]{false,true})for(var hand:InteractionHand.values()) {
      String id=BuiltInRegistries.ITEM.getKey(item).getPath();
      result.add(new TestFunction("hat050_lifecycle",id+"_"+(adult?"adult":"baby")+"_"+hand.name().toLowerCase(Locale.ROOT),
          "chao_hats50:activity_test",80,0,true,h->lifecycle(h,item,adult,hand)));
    }
    return result;
  }
  private static void lifecycle(GameTestHelper h,Item item,boolean adult,InteractionHand hand){
    var p=keeper(h);var c=actor(h,p,adult);var old=named(ChaoGarden.item("apple_hat"),1);c.setItemSlot(EquipmentSlot.HEAD,old.copy());
    var source=named(item,3);p.setItemInHand(hand,source);var expected=source.copyWithCount(1);
    h.assertTrue(ChaoHatItem.interact(c,p,hand)==InteractionResult.CONSUME,"vanilla hat is offered through production care");
    h.assertTrue(source.getCount()==2&&ItemStack.matches(c.getMainHandItem(),expected),"only one actual vanilla item transfers");
    h.assertTrue(ItemStack.matches(c.getItemBySlot(EquipmentSlot.HEAD),old),"old hat remains during consideration");
    accepted(c);
    h.assertTrue(ItemStack.matches(c.getItemBySlot(EquipmentSlot.HEAD),expected)&&!c.isHandlingItem(),"normal consent and wear phases equip the actual vanilla item");
    h.assertTrue(count(p,old.getItem())==1,"swap returns exactly one old hat");
    var loaded=reload(h,c);h.assertTrue(ItemStack.matches(loaded.getItemBySlot(EquipmentSlot.HEAD),expected),"vanilla equipment/components survive actual entity NBT load");
    h.assertTrue(loaded.getChaoHat().equals(c.getChaoHat())&&!c.getChaoHat().isEmpty(),"canonical vanilla headgear retains its rendering key");
    var carrier=new ItemStack(ChaoGarden.item("chao_carrier"));ChaoCarrierItem.snapshot(carrier,c);
    var released=ChaoGarden.CHAO.get().create(h.getLevel());released.load(ChaoCarrierItem.entityData(carrier));
    h.assertTrue(ItemStack.matches(released.getItemBySlot(EquipmentSlot.HEAD),expected),"carrier snapshot preserves vanilla hat and all components");
    int before=count(p,item);p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Items.SHEARS));
    // A MAIN_HAND source stack is replaced by shears here; count after replacing it.
    before=count(p,item);
    h.assertTrue(ChaoHatItem.interact(c,p,InteractionHand.MAIN_HAND)==InteractionResult.CONSUME,"shears remove vanilla Chao hat");
    h.assertTrue(c.getItemBySlot(EquipmentSlot.HEAD).isEmpty()&&count(p,item)==before+1,"removal returns precisely one vanilla hat");
    int once=count(p,item);h.assertTrue(ChaoHatItem.interact(c,p,InteractionHand.MAIN_HAND)==InteractionResult.PASS&&count(p,item)==once,"repeat removal cannot duplicate the hat");
    h.assertTrue(c.state().adult==adult&&c.state().trait("foreign.saved").equals("keep me"),"age and unrelated saved care remain intact");h.succeed();
  }
  @GameTest(template="activity_test",timeoutTicks=60,batch="hat050_migration")
  public static void legacyAliasIsHiddenAndTopLevelComponentsCountsMigrateExactly(GameTestHelper h){
    var legacy=ChaoGarden.item("watermelon_hat");h.assertTrue(legacy instanceof LegacyMelonHatItem&&!ChaoGarden.visibleInCreative(legacy),"old item ID is hidden compatibility data");
    h.assertFalse(MarketStock.HATS.contains("watermelon_hat")||MarketStock.catalog().containsKey("watermelon_hat"),"retired hat is not sold");
    for(String missing:List.of("pumpkin_hat","melon_hat","skull_hat"))h.assertFalse(BuiltInRegistries.ITEM.containsKey(ResourceLocation.fromNamespaceAndPath(ChaoGarden.MODID,missing)),"do not invent old IDs absent in 49: "+missing);
    var source=named(legacy,7);var nested=named(Items.DIAMOND_SWORD,1);nested.setDamageValue(137);
    source.set(DataComponents.CONTAINER,ItemContainerContents.fromItems(List.of(new ItemStack(Items.DIAMOND,3),nested,new ItemStack(legacy))));
    var converted=ChaoHats.canonicalCopy(source);
    h.assertTrue(converted.is(Items.MELON)&&converted.getCount()==7&&source.getItem()==legacy&&source.getCount()==7,"copy conversion preserves original and exact count");
    h.assertTrue(converted.getComponentsPatch().equals(source.getComponentsPatch()),"every explicit data component, including nested opaque inventory, survives unchanged");
    var decoded=ItemStack.parse(h.getLevel().registryAccess(),converted.save(h.getLevel().registryAccess())).orElseThrow();
    h.assertTrue(ItemStack.matches(converted,decoded),"converted vanilla stack round-trips through actual component codec");
    var contents=decoded.get(DataComponents.CONTAINER).stream().toList();h.assertTrue(contents.size()==3&&contents.get(2).is(legacy),"nested legacy stacks are not recursively rewritten");
    var damageable=named(legacy,1);damageable.set(DataComponents.MAX_DAMAGE,200);damageable.setDamageValue(137);
    h.assertTrue(ItemStack.validateComponents(damageable.getComponents()).isSuccess(),"old custom damageable hat is a valid saved item");
    var damagedMelon=ChaoHats.canonicalCopy(damageable);
    h.assertTrue(damagedMelon.is(Items.MELON)&&damagedMelon.getMaxStackSize()==1&&damagedMelon.getDamageValue()==137
        &&damagedMelon.getMaxDamage()==200&&ItemStack.validateComponents(damagedMelon.getComponents()).isSuccess(),"damageable legacy hat retains its necessary unstackable default and all damage data");
    var damagedReload=ItemStack.parse(h.getLevel().registryAccess(),damagedMelon.save(h.getLevel().registryAccess())).orElseThrow();
    h.assertTrue(ItemStack.matches(damagedMelon,damagedReload),"custom damageable migration survives actual item codec rather than becoming invalid stackable damageable data");
    h.assertTrue(ChaoHats.canonicalCopy(new ItemStack(legacy)).getMaxStackSize()==64,"ordinary legacy hats become ordinary stackable vanilla melons");
    h.succeed();
  }
  @GameTest(template="activity_test",timeoutTicks=60,batch="hat050_migration")
  public static void oldEquippedAndPendingGiftsMigrateThroughSaveAndCarrier(GameTestHelper h){
    var p=keeper(h);var c=actor(h,p,false);var legacy=named(ChaoGarden.item("watermelon_hat"),1);
    c.setItemSlot(EquipmentSlot.HEAD,legacy.copy());var loaded=reload(h,c);var expected=legacy.transmuteCopy(Items.MELON);
    h.assertTrue(ItemStack.matches(loaded.getItemBySlot(EquipmentSlot.HEAD),expected)&&loaded.getChaoHat().equals("watermelon_hat"),"old equipped save becomes vanilla melon without changing its old fitted appearance");
    var carrier=new ItemStack(ChaoGarden.item("chao_carrier"));ChaoCarrierItem.snapshot(carrier,c);
    var released=ChaoGarden.CHAO.get().create(h.getLevel());released.load(ChaoCarrierItem.entityData(carrier));
    h.assertTrue(ItemStack.matches(released.getItemBySlot(EquipmentSlot.HEAD),expected),"old carrier-held equipment migrates when actual Chao data loads");
    c.setItemSlot(EquipmentSlot.HEAD,ItemStack.EMPTY);c.setItemSlot(EquipmentSlot.MAINHAND,legacy.copy());
    var saved=new CompoundTag();c.saveWithoutId(saved);saved.putInt("ItemAction",ChaoEntity.ItemAction.THINK.ordinal());saved.putInt("ItemActionDuration",30);saved.putInt("ItemActionRemaining",30);saved.putUUID("ItemOfferedBy",p.getUUID());
    c.load(saved);h.assertTrue(ItemStack.matches(c.getMainHandItem(),expected),"in-progress old held gift migrates without losing its giver or decision");accepted(c);
    h.assertTrue(ItemStack.matches(c.getItemBySlot(EquipmentSlot.HEAD),expected)&&c.getMainHandItem().isEmpty(),"old pending gift completes once as actual vanilla melon");h.succeed();
  }
  @GameTest(template="activity_test",timeoutTicks=60,batch="hat050_migration")
  public static void legacyInventoryAndDroppedItemsMigrateWithoutTouchingUnrelatedItems(GameTestHelper h){
    var p=keeper(h);var legacy=named(ChaoGarden.item("watermelon_hat"),5);var sword=named(Items.DIAMOND_SWORD,1);sword.setDamageValue(120);
    p.getInventory().setItem(10,sword.copy());p.getInventory().setItem(40,legacy);
    legacy.inventoryTick(h.getLevel(),p,0,false);
    h.assertTrue(ItemStack.matches(p.getInventory().getItem(40),legacy.transmuteCopy(Items.MELON)),"offhand converts by inventory identity rather than ambiguous tick index");
    h.assertTrue(ItemStack.matches(p.getInventory().getItem(10),sword),"unrelated inventory is untouched");
    var drop=new ItemEntity(h.getLevel(),p.getX()+2,p.getY(),p.getZ(),legacy.copy());drop.setPickUpDelay(1000);
    h.assertTrue(h.getLevel().addFreshEntity(drop),"legacy dropped stack joins native level");cleanup(h,drop::discard);
    h.assertTrue(ItemStack.matches(drop.getItem(),legacy.transmuteCopy(Items.MELON)),"loaded/spawned item entity becomes vanilla with exact metadata/count");
    var outer=named(Items.SHULKER_BOX,1);outer.set(DataComponents.CONTAINER,ItemContainerContents.fromItems(List.of(legacy.copy(),sword.copy())));
    var parsed=ItemStack.parse(h.getLevel().registryAccess(),outer.save(h.getLevel().registryAccess())).orElseThrow();
    h.assertTrue(ItemStack.matches(outer,parsed)&&parsed.get(DataComponents.CONTAINER).stream().findFirst().orElseThrow().is(legacy.getItem()),"closed nested containers decode safely without a bulk rewrite");h.succeed();
  }
  @GameTest(template="activity_test",timeoutTicks=60,batch="hat050_consent")
  public static void legacyDirectGiftAndWardrobeBothConsumeOneVanillaMelon(GameTestHelper h){
    var p=keeper(h);var c=actor(h,p,true);var legacy=named(ChaoGarden.item("watermelon_hat"),3);p.setItemInHand(InteractionHand.MAIN_HAND,legacy.copy());
    h.assertTrue(ChaoHatItem.interact(c,p,InteractionHand.MAIN_HAND).consumesAction(),"old compatibility item can be offered before its next inventory tick");
    h.assertTrue(p.getMainHandItem().is(Items.MELON)&&p.getMainHandItem().getCount()==2&&c.getMainHandItem().is(Items.MELON),"direct gift migrates and transfers only one actual vanilla melon");accepted(c);
    h.assertTrue(c.getItemBySlot(EquipmentSlot.HEAD).is(Items.MELON),"old direct gift equips canonical item");
    p.getInventory().setItem(1,named(Items.SKELETON_SKULL,2));h.assertTrue(ChaoCareServices.equip(p,c,1),"Analyzer wardrobe accepts the vanilla skull");accepted(c);
    h.assertTrue(c.getItemBySlot(EquipmentSlot.HEAD).is(Items.SKELETON_SKULL)&&p.getInventory().getItem(1).getCount()==1&&count(p,Items.MELON)==3,"wardrobe swap returns only one canonical melon");h.succeed();
  }
  @GameTest(template="activity_test",timeoutTicks=80,batch="hat050_consent")
  public static void declinedVanillaHatAndForeignKeeperCannotReplaceEquipment(GameTestHelper h){
    var p=keeper(h);var c=actor(h,p,false);var old=named(Items.CARVED_PUMPKIN,1);c.setItemSlot(EquipmentSlot.HEAD,old.copy());
    var skull=named(Items.SKELETON_SKULL,1);p.setItemInHand(InteractionHand.MAIN_HAND,skull.copy());ChaoHatItem.interact(c,p,InteractionHand.MAIN_HAND);
    WardrobeGameTests.decided(c,false);WardrobeGameTests.expire(c);WardrobeGameTests.expire(c);
    var drops=h.getLevel().getEntitiesOfClass(ItemEntity.class,c.getBoundingBox().inflate(3),i->ItemStack.matches(i.getItem(),skull));
    h.assertTrue(drops.size()==1&&ItemStack.matches(c.getItemBySlot(EquipmentSlot.HEAD),old),"refusal returns the actual skull and keeps the old hat");drops.forEach(i->cleanup(h,i::discard));
    var stranger=keeper(h);stranger.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Items.SHEARS));
    h.assertTrue(ChaoHatItem.interact(c,stranger,InteractionHand.MAIN_HAND)==InteractionResult.FAIL&&ItemStack.matches(c.getItemBySlot(EquipmentSlot.HEAD),old),"another keeper cannot shear off the hat");h.succeed();
  }
  @GameTest(template="activity_test",timeoutTicks=80,batch="hat050_returns")
  public static void fullInventoryAndCancelledDropKeepHeadgearUntilSuccessfulRetry(GameTestHelper h){
    var p=keeper(h);var c=actor(h,p,true);var hat=named(Items.MELON,1);c.setItemSlot(EquipmentSlot.HEAD,hat.copy());
    for(int i=0;i<36;i++)p.getInventory().setItem(i,new ItemStack(Items.COBBLESTONE,64));p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Items.SHEARS));
    var blocked=new AtomicBoolean(true);Consumer<EntityJoinLevelEvent> listener=event->{if(event.getEntity() instanceof ItemEntity item&&ItemStack.matches(item.getItem(),hat)&&blocked.get())event.setCanceled(true);};
    NeoForge.EVENT_BUS.addListener(listener);cleanup(h,()->NeoForge.EVENT_BUS.unregister(listener));
    h.assertTrue(ChaoHatItem.interact(c,p,InteractionHand.MAIN_HAND)==InteractionResult.FAIL&&ItemStack.matches(c.getItemBySlot(EquipmentSlot.HEAD),hat),"failed world publication restores exact original headgear");
    blocked.set(false);h.assertTrue(ChaoHatItem.interact(c,p,InteractionHand.MAIN_HAND)==InteractionResult.CONSUME&&c.getItemBySlot(EquipmentSlot.HEAD).isEmpty(),"a later valid removal succeeds");
    var drops=h.getLevel().getEntitiesOfClass(ItemEntity.class,c.getBoundingBox().inflate(3),i->ItemStack.matches(i.getItem(),hat));h.assertTrue(drops.size()==1,"one successful removal emits exactly one actual vanilla melon");drops.forEach(i->cleanup(h,i::discard));h.succeed();
  }
  @GameTest(template="activity_test",timeoutTicks=60,batch="hat050_noninterference")
  public static void melonFoodAndOtherVanillaItemsRemainOrdinary(GameTestHelper h){
    for(var item:List.of(Items.MELON_SLICE,Items.GLISTERING_MELON_SLICE,Items.PUMPKIN,Items.JACK_O_LANTERN,Items.WITHER_SKELETON_SKULL,Items.PLAYER_HEAD,Items.CREEPER_HEAD)) {
      var stack=named(item,2);h.assertFalse(ChaoHats.isHat(stack)||ChaoCareServices.accessorySlot(stack)!=null,"unrequested vanilla item is not repurposed as headwear: "+item);
      h.assertTrue(ItemStack.matches(stack,ChaoHats.canonicalCopy(stack)),"ordinary vanilla item and all its data are unchanged");
    }
    h.assertTrue(new ItemStack(Items.MELON_SLICE).has(DataComponents.FOOD),"ordinary melon-slice food component survives");
    for(var item:HATS)h.assertTrue(ChaoCareServices.accessorySlot(new ItemStack(item))==EquipmentSlot.HEAD,"each requested vanilla item appears in the existing wardrobe UI");h.succeed();
  }
}
