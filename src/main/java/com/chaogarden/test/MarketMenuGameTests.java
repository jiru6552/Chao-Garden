package com.chaogarden.test;

import com.chaogarden.ChaoGarden;
import com.chaogarden.market.*;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.inventory.MerchantMenu;
import net.minecraft.world.item.*;
import net.neoforged.neoforge.gametest.*;

/** Real MerchantMenu payment/result transactions behind the filtered market UI. */
@GameTestHolder(ChaoGarden.MODID)
@PrefixGameTestTemplate(false)
public final class MarketMenuGameTests {
  @GameTest(template="empty",timeoutTicks=60)
  public static void everyOriginalOfferIndexConsumesExactPaymentAndAwardsOneProduct(GameTestHelper h){
    var trader=h.spawn(MarketContent.TRADER.get(),new BlockPos(2,2,2));
    trader.setNoAi(true);trader.setNoGravity(true);trader.beginVisit(h.getLevel().getGameTime());
    var player=TestConnections.makeServerPlayer(h);trader.setTradingPlayer(player);
    var menu=new MerchantMenu(19,player.getInventory(),trader);
    h.assertTrue((menu.getOffers().size()==MarketStock.OFFER_COUNT || menu.getOffers().size()==MarketStock.OFFER_COUNT+1),"Actual permanent and rotating offer count");
    for(int index=menu.getOffers().size()-1;index>=0;index--){
      var offer=menu.getOffers().get(index);var result=offer.getResult().copy();int uses=offer.getUses();
      player.getInventory().clearContent();
      player.getInventory().setItem(9,offer.getCostA().copy());
      player.getInventory().setItem(10,offer.getCostB().copy());
      menu.setSelectionHint(index);menu.tryMoveItems(index);
      h.assertTrue(ItemStack.isSameItemSameComponents(menu.getSlot(2).getItem(),result),"Filtered original index "+index+" selected another product");
      h.assertTrue(!menu.quickMoveStack(player,2).isEmpty(),"Real merchant result could not be purchased at "+index);
      int count=0;for(var stack:player.getInventory().items){
        if(ItemStack.isSameItemSameComponents(stack,result))count+=stack.getCount();
        h.assertTrue(!stack.is(Items.EMERALD)&&!stack.is(Items.EMERALD_BLOCK),"Exact payment was not consumed at "+index);
      }
      h.assertTrue(count==result.getCount(),"Purchase duplicated or lost product at "+index);
      String id=net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(result.getItem()).getPath();
      h.assertTrue(offer.getUses()==(MarketStock.permanent(id)?0:uses+1),"Purchase did not update finite/permanent stock at "+index);
      h.assertTrue(menu.getSlot(0).getItem().isEmpty()&&menu.getSlot(1).getItem().isEmpty(),"Payment remainder with exact currency");
      h.assertTrue(menu.quickMoveStack(player,2).isEmpty(),"Empty payment purchased a second product");
    }
    menu.removed(player);h.getLevel().getServer().getPlayerList().remove(player);trader.discard();h.succeed();
  }

  @GameTest(template="empty",timeoutTicks=80)
  public static void everyBaseColorAndDriveCanBeBoughtRepeatedlyAndReloadRestoresExhaustedStock(GameTestHelper h){
    var trader=h.spawn(MarketContent.TRADER.get(),new BlockPos(2,2,2));
    trader.setNoAi(true);trader.setNoGravity(true);trader.beginVisit(h.getLevel().getGameTime());
    var player=TestConnections.makeServerPlayer(h);trader.setTradingPlayer(player);
    var menu=new MerchantMenu(23,player.getInventory(),trader);
    for(String id:java.util.stream.Stream.concat(MarketStock.BASE_EGGS.stream(),MarketStock.DRIVES.stream()).toList()){
      int index=java.util.stream.IntStream.range(0,menu.getOffers().size()).filter(i->
          menu.getOffers().get(i).getResult().is(ChaoGarden.item(id))).findFirst().orElseThrow();
      var offer=menu.getOffers().get(index);
      for(int repeat=0;repeat<4;repeat++){
        player.getInventory().clearContent();
        player.getInventory().setItem(9,offer.getCostA().copy());player.getInventory().setItem(10,offer.getCostB().copy());
        menu.setSelectionHint(index);menu.tryMoveItems(index);
        h.assertTrue(!menu.quickMoveStack(player,2).isEmpty(),"Repeated actual base purchase failed: "+id);
        h.assertTrue(player.getInventory().countItem(ChaoGarden.item(id))==1,"Repeated purchase did not award exactly one "+id);
        h.assertTrue(offer.getUses()==0&&!offer.isOutOfStock(),"Base color became exhausted in an open menu: "+id);
        h.assertTrue(player.getInventory().countItem(Items.EMERALD)==0&&player.getInventory().countItem(Items.EMERALD_BLOCK)==0,
            "Repeated base purchase did not consume exact emerald payment");
      }
      offer.setToOutOfStock();
    }
    menu.removed(player);var saved=new net.minecraft.nbt.CompoundTag();trader.saveWithoutId(saved);
    var loaded=MarketContent.TRADER.get().create(h.getLevel());loaded.load(saved);
    for(var offer:loaded.getOffers()){
      String id=net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(offer.getResult().getItem()).getPath();
      if(MarketStock.permanent(id))h.assertTrue(offer.getUses()==0&&!offer.isOutOfStock(),"Loaded base offer retained exhausted stock: "+id);
    }
    h.getLevel().getServer().getPlayerList().remove(player);trader.discard();h.succeed();
  }

  @GameTest(template="empty",timeoutTicks=80)
  public static void animalDnaStockExhaustsAtItsLimitAndStaysSoldAfterReload(GameTestHelper h){
    var trader=h.spawn(MarketContent.TRADER.get(),new BlockPos(2,2,2));trader.setNoAi(true);trader.setNoGravity(true);trader.beginVisit(h.getLevel().getGameTime());
    var player=TestConnections.makeServerPlayer(h);int tested=0;
    for(int index=0;index<trader.getOffers().size();index++){
      var offer=trader.getOffers().get(index);String id=net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(offer.getResult().getItem()).getPath();
      if(!MarketStock.ORDINARY_GENES.contains(id)&&!MarketStock.MYTHICAL_GENES.contains(id))continue;
      tested++;trader.setTradingPlayer(player);var menu=new MerchantMenu(24+tested,player.getInventory(),trader);
      int limit=MarketStock.MYTHICAL_GENES.contains(id)?2:4;
      for(int purchase=0;purchase<limit;purchase++){
        player.getInventory().clearContent();player.getInventory().setItem(9,offer.getCostA().copy());
        menu.setSelectionHint(index);menu.tryMoveItems(index);
        h.assertTrue(!menu.quickMoveStack(player,2).isEmpty()&&player.getInventory().countItem(ChaoGarden.item(id))==1,"Finite DNA purchase failed before its limit: "+id);
        h.assertTrue(offer.getUses()==purchase+1,"DNA use count did not advance: "+id);
      }
      h.assertTrue(offer.isOutOfStock(),"DNA did not sell out at its advertised stock limit");
      player.getInventory().clearContent();player.getInventory().setItem(9,offer.getCostA().copy());
      menu.setSelectionHint(index);menu.tryMoveItems(index);
      h.assertTrue(menu.quickMoveStack(player,2).isEmpty()&&player.getInventory().countItem(ChaoGarden.item(id))==0,"Sold-out DNA could still be purchased");
      menu.removed(player);
      h.assertTrue(player.getInventory().countItem(Items.EMERALD)==offer.getCostA().getCount(),"Failed sold-out DNA purchase consumed its currency");
    }
    h.assertTrue(tested==4,"Expected three ordinary and one mythical DNA offer");
    var saved=new net.minecraft.nbt.CompoundTag();trader.saveWithoutId(saved);var loaded=MarketContent.TRADER.get().create(h.getLevel());loaded.load(saved);
    for(var offer:loaded.getOffers())if(offer.getResult().getItem() instanceof com.chaogarden.genes.GeneItem)
      h.assertTrue(offer.isOutOfStock(),"Reload reset finite DNA stock");
    h.getLevel().getServer().getPlayerList().remove(player);trader.discard();h.succeed();
  }

  @GameTest(template="empty",timeoutTicks=60)
  public static void closingAnUnboughtOfferRefundsBothPaymentSlots(GameTestHelper h){
    var trader=h.spawn(MarketContent.TRADER.get(),new BlockPos(2,2,2));trader.setNoAi(true);
    var player=TestConnections.makeServerPlayer(h);trader.setTradingPlayer(player);
    var menu=new MerchantMenu(20,player.getInventory(),trader);player.getInventory().clearContent();
    player.getInventory().setItem(9,new ItemStack(Items.EMERALD,37));
    player.getInventory().setItem(10,new ItemStack(Items.EMERALD_BLOCK,9));
    menu.getSlot(0).setByPlayer(player.getInventory().removeItemNoUpdate(9));
    menu.getSlot(1).setByPlayer(player.getInventory().removeItemNoUpdate(10));
    menu.removed(player);
    h.assertTrue(player.getInventory().countItem(Items.EMERALD)==37,"Close lost or duplicated loose emeralds");
    h.assertTrue(player.getInventory().countItem(Items.EMERALD_BLOCK)==9,"Close lost or duplicated emerald blocks");
    h.assertTrue(trader.getTradingPlayer()==null,"Close left merchant reserved");
    h.assertTrue(menu.getSlot(0).getItem().isEmpty()&&menu.getSlot(1).getItem().isEmpty(),"Closed payments remained duplicable");
    h.getLevel().getServer().getPlayerList().remove(player);trader.discard();h.succeed();
  }
}
