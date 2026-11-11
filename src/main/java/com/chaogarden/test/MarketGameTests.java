package com.chaogarden.test;

import com.chaogarden.ChaoGarden;
import com.chaogarden.entity.ChaoEntity;
import com.chaogarden.item.CareItem;
import com.chaogarden.market.MarketContent;
import com.chaogarden.market.MarketVisitData;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(ChaoGarden.MODID)
@PrefixGameTestTemplate(false)
public final class MarketGameTests {
  @GameTest(template="empty", timeoutTicks=40)
  public static void merchantStockUsesEmeraldsAndSurvivesReload(GameTestHelper helper) {
    var world = helper.getLevel();
    var trader = MarketContent.TRADER.get().create(world);
    helper.assertTrue(trader != null, "Trader is registered");
    trader.beginVisit(world.getGameTime());
    var offers = trader.getOffers();
    helper.assertTrue((offers.size()==com.chaogarden.market.MarketStock.OFFER_COUNT || offers.size()==com.chaogarden.market.MarketStock.OFFER_COUNT+1), "Permanent base catalog plus rotating stock and optional gem");
    for (var offer : offers) {
      helper.assertTrue(offer.getCostA().is(Items.EMERALD) || offer.getCostA().is(Items.EMERALD_BLOCK), "Emerald currency");
      helper.assertTrue(!offer.getResult().isEmpty() && !offer.getResult().is(Items.AIR), "Products resolve");
      String id=net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(offer.getResult().getItem()).getPath();
      helper.assertTrue(com.chaogarden.market.MarketStock.catalog().containsKey(id), "Only explicit source catalog");
      helper.assertTrue(!id.endsWith("_animal_gene") || com.chaogarden.market.MarketStock.ORDINARY_GENES.contains(id) || com.chaogarden.market.MarketStock.MYTHICAL_GENES.contains(id), "Only registered DNA products");
    }
    helper.assertTrue(offers.stream().filter(o -> o.getResult().getItem() instanceof CareItem care
        && care.kind == CareItem.Kind.FRUIT).count()==5, "Five fruit discoveries including portable and CWE fruit");
    int finiteIndex=java.util.stream.IntStream.range(0,offers.size()).filter(i->offers.get(i).getResult().getItem() instanceof com.chaogarden.item.ChaoEggItem egg && egg.shiny).findFirst().orElseThrow();
    var offer = offers.get(finiteIndex);
    var payment = offer.getCostA().copy();
    var paymentB = offer.getCostB().copy();
    helper.assertTrue(offer.take(payment, paymentB) && payment.isEmpty() && paymentB.isEmpty(),
        "Vanilla merchant payment consumes the exact emerald cost");
    offer.increaseUses();
    var saved = new CompoundTag();
    trader.saveWithoutId(saved);
    var loaded = MarketContent.TRADER.get().create(world);
    loaded.load(saved);
    helper.assertTrue(loaded.getOffers().size()==offers.size(), "Stock survives save/load");
    helper.assertTrue(loaded.getOffers().get(finiteIndex).getUses()==1, "Sold stock stays sold after reload");
    helper.assertTrue(loaded.visitExpiry()==trader.visitExpiry(), "Visit expiry survives reload");
    helper.succeed();
  }

  @GameTest(template="empty",timeoutTicks=40)
  public static void oldCharacterLensStockIsRebuiltOnLoad(GameTestHelper helper){
    var trader=MarketContent.TRADER.get().create(helper.getLevel());trader.beginVisit(helper.getLevel().getGameTime());
    trader.getOffers().clear();
    trader.getOffers().add(new net.minecraft.world.item.trading.MerchantOffer(
        new net.minecraft.world.item.trading.ItemCost(Items.EMERALD,10),new ItemStack(ChaoGarden.item("spartoi_lens")),2,1,0));
    var old=new CompoundTag();trader.saveWithoutId(old);old.putInt("MarketStockVersion",19);
    var loaded=MarketContent.TRADER.get().create(helper.getLevel());loaded.load(old);
    helper.assertTrue((loaded.getOffers().size()==com.chaogarden.market.MarketStock.OFFER_COUNT || loaded.getOffers().size()==com.chaogarden.market.MarketStock.OFFER_COUNT+1),"Old character stock did not become the current rotating catalog");
    for(var offer:loaded.getOffers()){
      helper.assertTrue(!offer.getResult().is(ChaoGarden.item("spartoi_lens")),"A saved0.19 offer kept selling the retired lens");
      helper.assertTrue(!(offer.getResult().getItem() instanceof com.chaogarden.cwe.RetiredCweGiftItem),"Retired compatibility products remained tradable");
    }
    var current=new CompoundTag();loaded.saveWithoutId(current);helper.assertTrue(current.getInt("MarketStockVersion")==com.chaogarden.market.BlackMarketTrader.STOCK_VERSION,"Stock migration did not become durable");helper.succeed();
  }

  @GameTest(template="empty",timeoutTicks=60)
  public static void kindergartenResidentKeepsStockAcrossReloadAndCannotExpire(GameTestHelper h){
    var trader=MarketContent.TRADER.get().create(h.getLevel());trader.beginVisit(h.getLevel().getGameTime());trader.setResident(true);
    var offer=trader.getOffers().stream().filter(o->o.getResult().getItem() instanceof com.chaogarden.genes.GeneItem).findFirst().orElseThrow();
    offer.increaseUses();String id=net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(offer.getResult().getItem()).getPath();
    var saved=new CompoundTag();trader.saveWithoutId(saved);saved.putLong("MarketVisitExpiry",1);saved.putInt("DespawnDelay",1);
    var loaded=MarketContent.TRADER.get().create(h.getLevel());loaded.load(saved);loaded.aiStep();
    h.assertTrue(loaded.resident()&&!loaded.isRemoved()&&loaded.visitExpiry()==0,"Kindergarten resident expired after reload");
    h.assertTrue(loaded.getOffers().stream().filter(o->o.getResult().is(ChaoGarden.item(id))).findFirst().orElseThrow().getUses()==1,"Resident reload reset finite sample purchases");
    loaded.setResident(false);h.assertTrue(!loaded.resident()&&loaded.visitExpiry()>h.getLevel().getGameTime(),"Returned wandering trader has no normal visit expiry");
    loaded.discard();trader.discard();h.succeed();
  }

  @GameTest(template="empty",timeoutTicks=40)
  public static void currentSavedMoonOfferIsRemovedWhenSpacePolicyAppears(GameTestHelper helper){
    var original=com.chaogarden.GardenConfig.SPACE_MOD_IDS.get();
    try {
      com.chaogarden.GardenConfig.SPACE_MOD_IDS.set(java.util.List.of());
      var trader=MarketContent.TRADER.get().create(helper.getLevel());trader.beginVisit(helper.getLevel().getGameTime());
      var offers=trader.getOffers();
      offers.removeIf(o->com.chaogarden.market.MarketStock.JEWEL_EGGS.contains(
          net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(o.getResult().getItem()).getPath()));
      offers.add(new net.minecraft.world.item.trading.MerchantOffer(
          new net.minecraft.world.item.trading.ItemCost(Items.EMERALD_BLOCK,16),
          java.util.Optional.of(new net.minecraft.world.item.trading.ItemCost(Items.EMERALD,6)),
          new ItemStack(ChaoGarden.item("moon_jewel_egg")),1,1,0));
      var saved=new CompoundTag();trader.saveWithoutId(saved);
      helper.assertTrue(saved.getInt("MarketStockVersion")==com.chaogarden.market.BlackMarketTrader.STOCK_VERSION,"Fixture must be current stock version");
      com.chaogarden.GardenConfig.SPACE_MOD_IDS.set(java.util.List.of("chao_garden"));
      var loaded=MarketContent.TRADER.get().create(helper.getLevel());loaded.load(saved);
      helper.assertTrue(loaded.getOffers().stream().noneMatch(o->o.getResult().is(ChaoGarden.item("moon_jewel_egg"))),
          "Saved current-version Moon offer bypassed newly enabled space restriction");
      helper.assertTrue(com.chaogarden.market.MarketStock.validStock(loaded.getOffers().stream().map(o->
          net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(o.getResult().getItem()).getPath()).toList(),true),
          "Migrated space stock lost permanent colors or contained forbidden products");
    } finally { com.chaogarden.GardenConfig.SPACE_MOD_IDS.set(original); }
    helper.succeed();
  }

  @GameTest(template="empty", timeoutTicks=40)
  public static void everyCatalogItemResolvesAndLargePaymentsWork(GameTestHelper helper) {
    for (var product : com.chaogarden.market.MarketStock.catalog().values()) {
      helper.assertTrue(ChaoGarden.CONTENT.containsKey(product.itemId()), "Registered product "+product.itemId());
      var listing=product.listing();
      var payments=com.chaogarden.market.MarketStock.payment(listing.emeralds());
      var a=payments.getFirst();
      var first=new net.minecraft.world.item.trading.ItemCost(a.blocks()?Items.EMERALD_BLOCK:Items.EMERALD,a.count());
      var second=payments.size()==1 ? java.util.Optional.<net.minecraft.world.item.trading.ItemCost>empty()
          : java.util.Optional.of(new net.minecraft.world.item.trading.ItemCost(payments.get(1).blocks()?Items.EMERALD_BLOCK:Items.EMERALD,payments.get(1).count()));
      var offer=new net.minecraft.world.item.trading.MerchantOffer(first,second,new ItemStack(ChaoGarden.item(product.itemId())),1,1,0);
      var payA=offer.getCostA().copy();var payB=offer.getCostB().copy();
      helper.assertTrue(offer.take(payA,payB) && payA.isEmpty() && payB.isEmpty(), "Exact real merchant payment "+product.itemId());
    }
    helper.assertTrue(!ChaoGarden.visibleInCreative(ChaoGarden.item("mushroom_seed"))
        && !ChaoGarden.visibleInCreative(ChaoGarden.item("mushroom_alt_seed")), "Old mushroom seeds hidden");
    helper.assertTrue(!com.chaogarden.fruit.FruitContent.seedIds().contains("mushroom_seed"), "Mushroom seed no longer in active catalog");
    helper.succeed();
  }

  @GameTest(template="empty", timeoutTicks=40)
  public static void eggShineFollowsTheSavedInheritedCoat(GameTestHelper helper) {
    helper.assertTrue(!new ItemStack(ChaoGarden.item("normal_egg")).hasFoil(), "Ordinary egg has no foil");
    for (String id : java.util.List.of("shiny_white_egg","ruby_jewel_egg","moon_jewel_egg"))
      helper.assertTrue(new ItemStack(ChaoGarden.item(id)).hasFoil(), "Registered shiny/jewel egg glints: "+id);
    var shiny=com.chaogarden.core.ChaoState.egg(2,true,false,0,new java.util.Random(1));
    var moon=com.chaogarden.core.ChaoState.egg(0,false,false,13,new java.util.Random(1));
    var regular=com.chaogarden.core.ChaoState.egg(0,false,true,0,new java.util.Random(1));
    for (var entry : java.util.Map.of("shiny_red_egg",shiny,"moon_jewel_egg",moon).entrySet()) {
      var state=entry.getValue();
      var recovered=com.chaogarden.item.ChaoEggItem.containing(state,40);
      var saved=recovered.get(net.minecraft.core.component.DataComponents.CUSTOM_DATA).copyTag();
      helper.assertTrue(recovered.is(ChaoGarden.item(entry.getKey())) && recovered.hasFoil()
          &&saved.getString("Chao").equals(state.encode())&&saved.getInt("HatchTicks")==40,
          "Recovered shell must match its preserved native shiny/jewel coat");
    }
    for(String registered:java.util.List.of("shiny_white_egg","moon_jewel_egg")) {
      var disguised=new ItemStack(ChaoGarden.item(registered));
      var tag=new CompoundTag();tag.putString("Chao",regular.encode());
      disguised.set(net.minecraft.core.component.DataComponents.CUSTOM_DATA,net.minecraft.world.item.component.CustomData.of(tag));
      helper.assertTrue(!disguised.hasFoil(), "Saved regular coat overrides registered shiny or jewel shell");
    }
    helper.assertTrue(com.chaogarden.core.ChaoState.decode(moon.encode()).jewel()==13,"Appended Moon allele survives serialization");
    helper.succeed();
  }

  @GameTest(template="empty", timeoutTicks=100)
  public static void traderRemainsVisibleAtNightWithAi(GameTestHelper helper) {
    var world=helper.getLevel();
    var trader=helper.spawn(MarketContent.TRADER.get(),new BlockPos(2,2,2));
    trader.setNoAi(false);
    long daytime=world.getDayTime();world.setDayTime(18000);
    trader.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.INVISIBILITY,600));
    helper.runAfterDelay(60,()->{
      helper.assertTrue(!trader.isNoAi(),"Night test exercises a ticking AI trader");
      helper.assertTrue(!trader.isInvisible() && !trader.hasEffect(net.minecraft.world.effect.MobEffects.INVISIBILITY),"Night and legacy invisibility cleared");
      helper.assertTrue(trader.goalSelector.getAvailableGoals().stream().noneMatch(g->g.getGoal() instanceof net.minecraft.world.entity.ai.goal.UseItemGoal<?>),"No dusk potion goal remains");
      world.setDayTime(daytime);helper.succeed();
    });
  }

  @GameTest(template="empty", timeoutTicks=300)
  public static void hatsSwapSaveAndTravelWithTheChao(GameTestHelper helper) {
    var chao=helper.spawn(ChaoGarden.CHAO.get(),new BlockPos(2,2,2));
    chao.setState(new com.chaogarden.core.ChaoState());chao.setNoAi(true);chao.setNoGravity(true);
    var keeper=TestConnections.makeServerPlayer(helper);keeper.getAbilities().instabuild=false;chao.owner=keeper.getUUID();
    keeper.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(ChaoGarden.item("apple_hat")));
    helper.assertTrue(com.chaogarden.market.ChaoHatItem.interact(chao,keeper,InteractionHand.MAIN_HAND)==InteractionResult.CONSUME,
        "Hat gift was not handed over");
    helper.assertTrue(chao.getChaoHat().isEmpty() && keeper.getMainHandItem().isEmpty()
        && chao.getMainHandItem().is(ChaoGarden.item("apple_hat")),"Hat equipped instantly or gift was duplicated");
    WardrobeGameTests.decided(chao,true);
    helper.runAtTickTime(115,()->{
      helper.assertTrue(chao.getChaoHat().equals("apple_hat") && chao.getMainHandItem().isEmpty(),"Considered hat failed to equip");
      var saved=new CompoundTag();chao.saveWithoutId(saved);
      var loaded=ChaoGarden.CHAO.get().create(helper.getLevel());loaded.load(saved);
      helper.assertTrue(loaded.getChaoHat().equals("apple_hat"),"Hat survives save/load");
      var held=new ItemStack(ChaoGarden.item("chao_carrier"));com.chaogarden.item.ChaoCarrierItem.snapshot(held,chao);
      var released=ChaoGarden.CHAO.get().create(helper.getLevel());released.load(com.chaogarden.item.ChaoCarrierItem.entityData(held));
      helper.assertTrue(released.getChaoHat().equals("apple_hat"),"Held snapshot preserves headgear");
      keeper.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(ChaoGarden.item("pan_hat")));
      com.chaogarden.market.ChaoHatItem.interact(chao,keeper,InteractionHand.MAIN_HAND);
      WardrobeGameTests.decided(chao,true);
      helper.assertTrue(chao.getChaoHat().equals("apple_hat"),"Old hat removed before swap animation finished");
    });
    helper.runAtTickTime(235,()->{
      helper.assertTrue(chao.getChaoHat().equals("pan_hat") && keeper.getInventory().countItem(ChaoGarden.item("apple_hat"))==1,
          "Delayed swap did not return exactly one old hat");
      keeper.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Items.SHEARS));
      com.chaogarden.market.ChaoHatItem.interact(chao,keeper,InteractionHand.MAIN_HAND);
      helper.assertTrue(chao.getChaoHat().isEmpty() && keeper.getInventory().countItem(ChaoGarden.item("pan_hat"))==1,
          "Shears failed to return headwear exactly once");chao.discard();
      keeper.server.getPlayerList().remove(keeper);helper.succeed();
    });
  }

  @GameTest(template="empty", timeoutTicks=260)
  public static void hatsReturnOnceAtActualLifecycleRemoval(GameTestHelper helper) {
    var happy=helper.spawn(ChaoGarden.CHAO.get(),new BlockPos(2,2,2));
    var unhappy=helper.spawn(ChaoGarden.CHAO.get(),new BlockPos(4,2,2));
    for (var chao : java.util.List.of(happy,unhappy)) {
      chao.setNoAi(true);chao.setNoGravity(true);
      var state=new com.chaogarden.core.ChaoState();state.adult=true;
      state.adultAgeTicks=com.chaogarden.GardenConfig.ADULT_TICKS.get()-1;
      state.cocoonTicks=0;state.happiness=chao==happy?100:0;chao.setState(state);
      chao.setItemSlot(net.minecraft.world.entity.EquipmentSlot.HEAD,
          new ItemStack(ChaoGarden.item(chao==happy?"apple_hat":"pan_hat")));
    }
    helper.runAfterDelay(3,()->helper.assertTrue(happy.isRebirthCocoon()&&unhappy.isDeathCocoon()
        &&!happy.getChaoHat().isEmpty()&&!unhappy.getChaoHat().isEmpty(),"Both lifespans first enter their real cocoon with equipment intact"));
    helper.runAfterDelay(215,()->{
      helper.assertTrue(happy.isRemoved() && unhappy.isRemoved(), "Both actual lifespan transitions removed their old bodies");
      helper.assertTrue(happy.getChaoHat().isEmpty() && unhappy.getChaoHat().isEmpty(), "Lifecycle clears headgear before discarding");
      for (String id:java.util.List.of("apple_hat","pan_hat")) {
        int count=helper.getLevel().getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,
            new net.minecraft.world.phys.AABB(helper.absolutePos(new BlockPos(3,2,2))).inflate(8),
            item->item.getItem().is(ChaoGarden.item(id))).stream().mapToInt(item->item.getItem().getCount()).sum();
        helper.assertTrue(count==(id.equals("apple_hat")?0:1),"Rebirth transfers its hat; death returns exactly one "+id);
      }
      var eggs=helper.getLevel().getEntitiesOfClass(com.chaogarden.entity.ChaoEggEntity.class,
          new net.minecraft.world.phys.AABB(helper.absolutePos(new BlockPos(2,2,2))).inflate(5));
      helper.assertTrue(eggs.size()==1,"Happy reincarnation creates exactly one retained-equipment egg");
      var data=eggs.getFirst().snapshotItem().get(net.minecraft.core.component.DataComponents.CUSTOM_DATA).copyTag();
      var retained=ItemStack.parseOptional(helper.getLevel().registryAccess(),data.getCompound("RebornEquipment").getCompound("head"));
      helper.assertTrue(retained.is(ChaoGarden.item("apple_hat")),"The real rebirth egg owns the original hat");
      helper.succeed();
    });
  }

  @GameTest(template="empty", timeoutTicks=40)
  public static void toyItemsPlaceInsteadOfForcingImmediatePlay(GameTestHelper helper) {
    var chao=helper.spawn(ChaoGarden.CHAO.get(),new BlockPos(2,2,2));
    var player=helper.makeMockPlayer(GameType.SURVIVAL);chao.owner=player.getUUID();
    chao.state().happiness=20;chao.state().sleeping=false;
    player.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(ChaoGarden.item("chao_rattle")));
    helper.assertTrue(MarketContent.interact(chao,player,InteractionHand.MAIN_HAND).consumesAction(),
        "Clicking explains physical placement");
    helper.assertTrue(player.getMainHandItem().getCount()==1&&chao.state().happiness==20,
        "Clicking a toy at a Chao consumes no item and grants no instant happiness");
    helper.assertTrue(player.getMainHandItem().getItem() instanceof net.minecraft.world.item.BlockItem,
        "Legacy toy ID is a real placeable block item");
    helper.assertTrue(!com.chaogarden.toys.ToyContent.isPlaying(chao),"The Chao has not been forced to play");
    helper.assertTrue(helper.getLevel().getEntitiesOfClass(net.minecraft.world.entity.Display.ItemDisplay.class,
        chao.getBoundingBox().inflate(4)).isEmpty(),"No temporary item copy is spawned");
    helper.succeed();
  }

  @GameTest(template="empty", timeoutTicks=40)
  public static void marketVisitBudgetPersists(GameTestHelper helper) {
    var data = new MarketVisitData();
    helper.assertTrue(data.mayAttempt(0, 1200), "First visit may be attempted");
    data.attempted(0);
    data.occupied(25000);
    helper.assertTrue(!data.mayAttempt(0, 2400), "No repeat spawn roll the same day");
    helper.assertTrue(!data.mayAttempt(1, 24000), "Active visit prevents another spawn");
    var tag = data.save(new CompoundTag(), helper.getLevel().registryAccess());
    var loaded = MarketVisitData.load(tag, helper.getLevel().registryAccess());
    helper.assertTrue(!loaded.mayAttempt(0, 2400) && !loaded.mayAttempt(1, 24000),
        "Reload cannot reroll a visit or remove the active budget");
    helper.assertTrue(loaded.mayAttempt(2, 48000), "Expired visit permits a later day");
    helper.succeed();
  }
}
