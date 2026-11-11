package com.chaogarden.test;

import com.chaogarden.ChaoGarden;
import com.chaogarden.market.BlackMarketTrader;
import com.chaogarden.market.MarketContent;
import com.chaogarden.market.MarketStock;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.GameTestGenerator;
import net.minecraft.gametest.framework.TestFunction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.inventory.MerchantMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.ItemCost;
import net.minecraft.world.item.trading.MerchantOffer;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** Actual merchants, saved offers, restocking and finite menu purchases. */
@GameTestHolder("chao_market50") @PrefixGameTestTemplate(false)
public final class MarketRotationGameTests050 {
  private static final String HALF="half_fish_animal_gene";
  private static String id(MerchantOffer offer){return BuiltInRegistries.ITEM.getKey(offer.getResult().getItem()).getPath();}
  private static List<String> ids(BlackMarketTrader trader){return trader.getOffers().stream().map(MarketRotationGameTests050::id).toList();}
  private static BlackMarketTrader create(GameTestHelper h,long seed){
    var trader=MarketContent.TRADER.get().create(h.getLevel());
    h.assertTrue(trader!=null,"native trader creates");
    trader.getRandom().setSeed(seed);trader.setNoAi(true);trader.setNoGravity(true);
    trader.beginVisit(h.getLevel().getGameTime());return trader;
  }
  private static CompoundTag save(BlackMarketTrader trader){var saved=new CompoundTag();trader.saveWithoutId(saved);return saved;}
  private static BlackMarketTrader load(GameTestHelper h,CompoundTag saved){var trader=create(h,801);trader.load(saved);return trader;}
  private static void valid(GameTestHelper h,BlackMarketTrader trader){
    var stock=ids(trader);
    h.assertTrue(MarketStock.validStock(stock,com.chaogarden.distribution.EggDistribution.spaceModLoaded()),"actual stock obeys current catalog and space policy");
    h.assertTrue(stock.stream().filter(MarketStock.SPECIAL_GENES::contains).count()==1,"one special DNA slot");
    h.assertTrue(stock.stream().filter(MarketStock.ORDINARY_GENES::contains).count()==3,"three ordinary DNA slots");
    h.assertTrue(stock.stream().filter(HALF::equals).count()<=1&&stock.stream().distinct().count()==stock.size(),"no duplicate Half-fish or other product");
    h.assertTrue(stock.stream().noneMatch(s->List.of("watermelon_hat","melon_hat","pumpkin_hat","skull_hat").contains(s)),"no retired vanilla-replacement hat is sold");
    for(var offer:trader.getOffers()){
      var listing=MarketStock.catalog().get(id(offer)).listing();
      h.assertTrue(offer.getResult().getCount()==listing.count(),"unchanged actual offer quantity");
      h.assertTrue(offer.getMaxUses()==listing.uses(),"unchanged actual finite offer limit");
    }
  }
  private static BlackMarketTrader withSpecial(GameTestHelper h,String special){
    for(int seed=0;seed<200;seed++){
      var trader=create(h,seed);
      if(ids(trader).contains(special))return trader;
      trader.discard();
    }
    throw new AssertionError("No actual trader selected "+special);
  }

  @GameTest(template="empty",timeoutTicks=100,batch="market050_stock")
  public static void repeatedNewVisitorsAndResidentsRotateEverySpecialWithoutDuplicateHalfFish(GameTestHelper h){
    var seen=new HashSet<String>();
    for(int seed=0;seed<128;seed++){
      var trader=create(h,seed);trader.setResident(seed%2==0);valid(h,trader);
      ids(trader).stream().filter(MarketStock.SPECIAL_GENES::contains).forEach(seen::add);
      var loaded=load(h,save(trader));valid(h,loaded);
      h.assertTrue(ids(loaded).equals(ids(trader)),"current stock survives reload without rerolling");
      h.assertTrue(loaded.resident()==trader.resident()&&loaded.visitExpiry()==trader.visitExpiry(),"resident/visitor lifetime survives stock reload");
      loaded.discard();trader.discard();
    }
    h.assertTrue(seen.equals(new HashSet<>(MarketStock.SPECIAL_GENES)),"Dragon, Unicorn, Phoenix and Half-fish all rotate in actual merchants");h.succeed();
  }

  @GameTest(template="empty",timeoutTicks=100,batch="market050_stock")
  public static void dueRestockWaitsForAnOpenMenuThenRotatesAtTheNativeAiStep(GameTestHelper h){
    var player=TestConnections.makeServerPlayer(h);
    try{
      var seen=new HashSet<String>();
      for(int seed=0;seed<64;seed++){
        var original=create(h,seed);var saved=save(original);saved.putLong("MarketRestockAt",1);
        var trader=load(h,saved);trader.getRandom().setSeed(seed);
        var before=trader.getOffers();trader.setTradingPlayer(player);trader.aiStep();
        h.assertTrue(trader.getOffers()==before,"due restock does not invalidate an open merchant menu");
        trader.setTradingPlayer(null);trader.aiStep();valid(h,trader);
        h.assertTrue(trader.getOffers()!=before&&trader.restockTicksRemaining()>=3600&&trader.restockTicksRemaining()<=8400,"native restock replaces offers and schedules the ordinary next interval");
        ids(trader).stream().filter(MarketStock.SPECIAL_GENES::contains).forEach(seen::add);
        trader.discard();original.discard();
      }
      h.assertTrue(seen.equals(new HashSet<>(MarketStock.SPECIAL_GENES)),"all four special choices reach timed restocks");h.succeed();
    }finally{player.server.getPlayerList().remove(player);}
  }

  @GameTest(template="empty",timeoutTicks=100,batch="market050_stock")
  public static void oldVersionStockRefreshesOnceAndPreservesUnrelatedTraderData(GameTestHelper h){
    for(boolean resident:new boolean[]{false,true}){
      var trader=withSpecial(h,"dragon_animal_gene");trader.setResident(resident);
      trader.getPersistentData().putString("ForeignData","leave this alone");
      var saved=save(trader);saved.putInt("MarketStockVersion",36);
      var loaded=load(h,saved);valid(h,loaded);
      h.assertTrue(loaded.resident()==resident&&loaded.visitExpiry()==trader.visitExpiry(),"stock migration does not change visit/resident lifetime");
      h.assertTrue(loaded.getPersistentData().getString("ForeignData").equals("leave this alone"),"stock migration preserves unrelated persistent trader data");
      var newSave=save(loaded);h.assertTrue(newSave.getInt("MarketStockVersion")==BlackMarketTrader.STOCK_VERSION,"new stock version becomes durable");
      var reloaded=load(h,newSave);h.assertTrue(ids(reloaded).equals(ids(loaded)),"migration does not repeat on every reload");
      reloaded.discard();loaded.discard();trader.discard();
    }
    h.succeed();
  }

  @GameTest(template="empty",timeoutTicks=100,batch="market050_stock")
  public static void currentVersionRetiredMelonHatOfferIsRejected(GameTestHelper h){
    var trader=create(h,9);var offers=trader.getOffers();
    int hat=java.util.stream.IntStream.range(0,offers.size()).filter(i->MarketStock.HATS.contains(id(offers.get(i)))).findFirst().orElseThrow();
    offers.set(hat,new MerchantOffer(new ItemCost(Items.EMERALD,60),new ItemStack(ChaoGarden.item("watermelon_hat")),2,1,0));
    var saved=save(trader);h.assertTrue(saved.getInt("MarketStockVersion")==BlackMarketTrader.STOCK_VERSION,"fixture uses current stock version");
    var loaded=load(h,saved);valid(h,loaded);
    h.assertTrue(ids(loaded).stream().noneMatch("watermelon_hat"::equals),"retired item cannot survive a forged/current version tag");
    loaded.discard();trader.discard();h.succeed();
  }

  @GameTest(template="empty",timeoutTicks=100,batch="market050_stock")
  public static void currentVersionOldOrdinaryHalfFishPlusMythicalOfferIsRejected(GameTestHelper h){
    var trader=withSpecial(h,"unicorn_animal_gene");var offers=trader.getOffers();
    int ordinary=java.util.stream.IntStream.range(0,offers.size()).filter(i->MarketStock.ORDINARY_GENES.contains(id(offers.get(i)))).findFirst().orElseThrow();
    offers.set(ordinary,new MerchantOffer(new ItemCost(Items.EMERALD,1),new ItemStack(ChaoGarden.item(HALF)),4,1,0));
    var loaded=load(h,save(trader));valid(h,loaded);
    loaded.discard();trader.discard();h.succeed();
  }

  @GameTestGenerator public static Collection<TestFunction> specialPurchases(){
    var cases=new ArrayList<TestFunction>();
    for(String special:MarketStock.SPECIAL_GENES)cases.add(new TestFunction("market050_purchase",special+"_finite_payment_reload","chao_market50:empty",100,0,true,h->purchase(h,special)));
    return cases;
  }
  private static void purchase(GameTestHelper h,String special){
    var trader=withSpecial(h,special);var pos=h.absolutePos(new BlockPos(2,2,2));trader.moveTo(pos.getX()+.5,pos.getY(),pos.getZ()+.5,0,0);h.getLevel().addFreshEntity(trader);
    var player=TestConnections.makeServerPlayer(h);MerchantMenu menu=null;
    try{
      trader.setTradingPlayer(player);menu=new MerchantMenu(50,player.getInventory(),trader);
      var offers=menu.getOffers();int index=java.util.stream.IntStream.range(0,offers.size()).filter(i->id(offers.get(i)).equals(special)).findFirst().orElseThrow();
      var offer=offers.get(index);boolean half=special.equals(HALF);int limit=half?4:2,price=half?1:2;
      h.assertTrue(offer.getCostA().is(Items.EMERALD)&&offer.getCostA().getCount()==price&&offer.getCostB().isEmpty()&&offer.getResult().getCount()==1&&offer.getMaxUses()==limit,"special rotation preserves its source price and quantity");
      for(int purchase=0;purchase<limit;purchase++){
        player.getInventory().clearContent();player.getInventory().setItem(9,new ItemStack(Items.EMERALD,price));
        menu.setSelectionHint(index);menu.tryMoveItems(index);
        h.assertTrue(!menu.quickMoveStack(player,2).isEmpty(),"real purchase succeeds before finite stock limit");
        h.assertTrue(player.getInventory().countItem(ChaoGarden.item(special))==1&&player.getInventory().countItem(Items.EMERALD)==0,"one DNA sample and exact consumed payment");
      }
      h.assertTrue(offer.isOutOfStock(),"special offer stops at its unchanged limit");
      player.getInventory().clearContent();player.getInventory().setItem(9,new ItemStack(Items.EMERALD,price));menu.setSelectionHint(index);menu.tryMoveItems(index);
      h.assertTrue(menu.quickMoveStack(player,2).isEmpty(),"sold-out sample cannot be duplicated");menu.removed(player);menu=null;
      var loaded=load(h,save(trader));valid(h,loaded);
      var retained=loaded.getOffers().stream().filter(o->id(o).equals(special)).findFirst().orElseThrow();
      h.assertTrue(retained.isOutOfStock()&&retained.getUses()==limit,"current saved stock does not replenish purchased DNA on reload");
      loaded.discard();h.succeed();
    }finally{if(menu!=null)menu.removed(player);player.server.getPlayerList().remove(player);trader.discard();}
  }
}
