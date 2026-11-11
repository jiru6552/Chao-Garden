package com.chaogarden.test;

import com.chaogarden.ChaoGarden;
import com.chaogarden.hub.HubContent;
import com.chaogarden.kindergarten.KindergartenContent;
import com.chaogarden.kindergarten.KindergartenMarket025;
import com.chaogarden.kindergarten.KindergartenNetwork;
import com.chaogarden.market.BlackMarketTrader;
import io.netty.channel.embedded.EmbeddedChannel;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.common.ClientboundCustomPayloadPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** Actual resident sessions and inventory commits, rather than a parallel buyback implementation. */
@GameTestHolder(ChaoGarden.MODID)
@PrefixGameTestTemplate(false)
public final class KindergartenMarketBuybackGameTests025 {
  private static final int RESIDENT_WARMUP_TICKS=240;
  private static final int TRANSACTION_TICKS=80;
  private static final class Fixture {
    final ServerPlayer player;BlackMarketTrader trader;
    Fixture(ServerPlayer player){this.player=player;}
  }
  private static Fixture fixture(GameTestHelper h){
    var p=TestConnections.makeSurvivalServerPlayer(h);p.getInventory().clearContent();
    var f=new Fixture(p);
    // A failed or timed-out test must not leave a player in the shared room.
    h.testInfo.addListener(new net.minecraft.gametest.framework.GameTestListener(){
      @Override public void testStructureLoaded(net.minecraft.gametest.framework.GameTestInfo info){}
      @Override public void testPassed(net.minecraft.gametest.framework.GameTestInfo info,net.minecraft.gametest.framework.GameTestRunner runner){finish(f);}
      @Override public void testFailed(net.minecraft.gametest.framework.GameTestInfo info,net.minecraft.gametest.framework.GameTestRunner runner){finish(f);}
      @Override public void testAddedForRerun(net.minecraft.gametest.framework.GameTestInfo oldInfo,net.minecraft.gametest.framework.GameTestInfo newInfo,net.minecraft.gametest.framework.GameTestRunner runner){finish(f);}
    });
    h.runAtTickTime(RESIDENT_WARMUP_TICKS,()->h.assertTrue(f.trader!=null,"Shared resident did not become ready within its startup budget"));
    var level=p.server.getLevel(KindergartenContent.DIMENSION);
    h.assertTrue(level!=null&&HubContent.ensure(level),"Kindergarten dimension is not registered/built");
    // Exercise the actual ticketed resident. A private actor in an unforced
    // section can remain untracked and is not a valid physical interaction.
    p.teleportTo(level,-21.5,80,5.5,0,0);p.hasChangedDimension();p.setNoGravity(true);
    drain(p);return f;
  }
  private static void finish(Fixture f){
    // The canonical shared resident and its shop stock belong to the room.
    if(f.player.server.getPlayerList().getPlayer(f.player.getUUID())==f.player)
      f.player.server.getPlayerList().remove(f.player);
  }
  private static net.minecraft.gametest.framework.GameTestSequence ready(GameTestHelper h,Fixture f){
    return h.startSequence().thenWaitUntil(()->{
      var level=f.player.serverLevel();KindergartenContent.ensureServices(level);
      var residents=level.getEntitiesOfClass(BlackMarketTrader.class,new AABB(new BlockPos(-22,80,5)).inflate(4),BlackMarketTrader::resident);
      h.assertTrue(residents.size()==1,"The real shared resident is not loaded exactly once yet");
      var trader=residents.getFirst();h.assertTrue(level.getEntity(trader.getId())==trader,"The real shared resident is not available to network actor lookup yet");
      // A freshly loaded resident can expose yesterday's cached offers until
      // its first AI tick restocks. Observe a real stable stock window instead
      // of mistaking that scheduled refresh for a buyback side effect.
      h.assertTrue(!trader.getOffers().isEmpty()&&trader.restockTicksRemaining()>TRANSACTION_TICKS,
          "The real shared resident awaits its next normal stock window");
      f.trader=trader;
    }).thenExecute(()->h.runAtTickTime(h.getTick()+TRANSACTION_TICKS,()->h.fail("Resident transaction exceeded its original 80-tick execution budget")));
  }
  private static void ready(GameTestHelper h,Fixture f,Runnable task){ready(h,f).thenExecute(task);}
  private static KindergartenNetwork.Open drain(ServerPlayer p){
    var channel=(EmbeddedChannel)p.connection.getConnection().channel();
    channel.runPendingTasks();channel.flushOutbound();channel.runPendingTasks();
    KindergartenNetwork.Open latest=null;Object out;
    while((out=channel.readOutbound())!=null)
      if(out instanceof ClientboundCustomPayloadPacket packet&&packet.payload() instanceof KindergartenNetwork.Open view)latest=view;
    return latest;
  }
  private static KindergartenNetwork.Open open(GameTestHelper h,Fixture f){
    KindergartenMarket025.open(f.player,f.trader);var view=drain(f.player);
    h.assertTrue(view!=null&&view.role().equals("MARKET")&&view.proposal().equals("home|"),"Resident did not send its real scene menu");return view;
  }
  private static void action(ServerPlayer p,KindergartenNetwork.Open view,String selection,String action,String value){
    KindergartenContent.handle(p,new KindergartenNetwork.Action(view.npc(),view.nonce(),selection,action,value));
  }
  private static KindergartenNetwork.Open quote(GameTestHelper h,Fixture f,KindergartenNetwork.Open view,int slot){
    action(f.player,view,"s:"+slot,"select","");var result=drain(f.player);
    h.assertTrue(result!=null&&result.proposal().startsWith("quote|")&&token(result).length()==36,"Real server selection did not return an opaque quote token: received="+(result==null?"none":result.proposal())+" actorTracked="+(f.player.serverLevel().getEntity(f.trader.getId())==f.trader)+" authorized="+KindergartenMarket025.authorized(f.player,f.trader)+" value="+KindergartenMarket025.quoteEmeralds(f.player,slot));return result;
  }
  private static String token(KindergartenNetwork.Open view){return view.proposal().split("\\|",2)[1];}
  private static int currency(ServerPlayer p){return p.getInventory().countItem(Items.EMERALD)+9*p.getInventory().countItem(Items.EMERALD_BLOCK);}
  private static List<ItemStack> inventory(ServerPlayer p){var result=new ArrayList<ItemStack>();for(int i=0;i<36;i++)result.add(p.getInventory().getItem(i).copy());return result;}
  private static boolean sameInventory(ServerPlayer p,List<ItemStack> before){for(int i=0;i<36;i++)if(!ItemStack.matches(p.getInventory().getItem(i),before.get(i)))return false;return true;}

  // Asynchronous dimension/resident startup has its own bounded allowance;
  // the transaction retains its original 80-tick budget and all assertions.
  @GameTest(template="empty",timeoutTicks=RESIDENT_WARMUP_TICKS+TRANSACTION_TICKS,batch="kindergarten_buyback_transaction")
  public static void residentSaleConsumesOneQuotedLotAndRejectsForgedAndReplayedCommits(GameTestHelper h){
    var f=fixture(h);var p=f.player;
    var quoted=new java.util.concurrent.atomic.AtomicReference<KindergartenNetwork.Open>();
    var stock=new java.util.concurrent.atomic.AtomicReference<List<String>>();
    var paid=new java.util.concurrent.atomic.AtomicReference<List<ItemStack>>();
    ready(h,f).thenExecute(()->{
      com.mojang.logging.LogUtils.getLogger().info("CHAO_BUYBACK_TRANSACTION_READY testTick={} restockTicks={}",h.getTick(),f.trader.restockTicksRemaining());
      p.getInventory().setItem(0,new ItemStack(ChaoGarden.item("swim_drive"),4));
      p.getInventory().setItem(1,new ItemStack(ChaoGarden.item("fly_drive"),4));
      h.assertTrue(KindergartenMarket025.quoteEmeralds(p,0)==1,"Four one-emerald drives should sell for one emerald");
      stock.set(f.trader.getOffers().stream().map(o->o.getResult().getItem().toString()+":"+o.getUses()).toList());
      var interaction=new net.neoforged.neoforge.event.entity.player.PlayerInteractEvent.EntityInteract(p,InteractionHand.MAIN_HAND,f.trader);
      net.neoforged.neoforge.common.NeoForge.EVENT_BUS.post(interaction);var home=drain(p);
      h.assertTrue(interaction.isCanceled()&&home!=null&&home.role().equals("MARKET")&&p.containerMenu==p.inventoryMenu,"Registered resident interaction did not open the in-scene market menu");
      KindergartenContent.handle(p,new KindergartenNetwork.Action(home.npc(),UUID.randomUUID().toString(),"s:0","select",""));
      KindergartenContent.handle(p,new KindergartenNetwork.Action(Integer.MAX_VALUE,home.nonce(),"s:0","select",""));
      h.assertTrue(drain(p)==null&&currency(p)==0,"Forged actor/nonce opened a valid quote or paid currency");
      var view=quote(h,f,home,0);quoted.set(view);var before=inventory(p);
      action(p,view,view.selected(),"market_confirm",UUID.randomUUID().toString());
      h.assertTrue(sameInventory(p,before),"A forged quote token changed inventory");
    }).thenExecuteAfter(5,()->{
      var view=quoted.get();action(p,view,view.selected(),"market_confirm",token(view));var receipt=drain(p);
      h.assertTrue(p.getInventory().countItem(ChaoGarden.item("swim_drive"))==0&&p.getInventory().countItem(ChaoGarden.item("fly_drive"))==4&&currency(p)==1,"Authorized sale did not consume only the exact quoted lot/pay once");
      h.assertTrue(receipt!=null&&receipt.proposal().equals("sell|"),"Commit did not consume its quote token");
      h.assertTrue(f.trader.getOffers().stream().map(o->o.getResult().getItem().toString()+":"+o.getUses()).toList().equals(stock.get()),"Selling rerolled or consumed the real merchant's finite shop stock");
      paid.set(inventory(p));
    }).thenExecuteAfter(5,()->{
      var view=quoted.get();h.assertTrue(paid.get()!=null,"Authorized commit stage completed before replay");
      action(p,view,view.selected(),"market_confirm",token(view));
      h.assertTrue(sameInventory(p,paid.get()),"Replaying a spent nonce/token sold another lot or paid twice");
      finish(f);
    }).thenSucceed();
  }

  @GameTest(template="empty",timeoutTicks=RESIDENT_WARMUP_TICKS+TRANSACTION_TICKS)
  public static void insufficientPaymentSpacePreservesTheLotAndLetsTheSameQuoteRetry(GameTestHelper h){
    var f=fixture(h);ready(h,f,()->{var p=f.player;
    for(int i=0;i<36;i++)p.getInventory().setItem(i,new ItemStack(Items.COBBLESTONE,64));
    p.getInventory().setItem(0,new ItemStack(ChaoGarden.item("suspicious_potion")));
    h.assertTrue(KindergartenMarket025.quoteEmeralds(p,0)==125,"High-value fixture must require both emerald blocks and loose emeralds");
    var quoted=quote(h,f,open(h,f),0);var before=inventory(p);
    action(p,quoted,quoted.selected(),"market_confirm",token(quoted));var denied=drain(p);
    h.assertTrue(sameInventory(p,before)&&currency(p)==0,"Full inventory consumed a lot or partially delivered its two-stack payment");
    h.assertTrue(denied!=null&&denied.proposal().equals(quoted.proposal()),"Capacity denial discarded a retryable quote");
    p.getInventory().setItem(1,ItemStack.EMPTY);
    h.runAfterDelay(5,()->{
      action(p,quoted,quoted.selected(),"market_confirm",token(quoted));drain(p);
      h.assertTrue(p.getInventory().countItem(ChaoGarden.item("suspicious_potion"))==0&&p.getInventory().countItem(Items.EMERALD_BLOCK)==13&&p.getInventory().countItem(Items.EMERALD)==8,"Retry did not atomically replace the sold lot with exact125-emerald payment");
      for(int i=2;i<36;i++)h.assertTrue(p.getInventory().getItem(i).is(Items.COBBLESTONE)&&p.getInventory().getItem(i).getCount()==64,"Sale changed an unrelated occupied inventory slot");
      finish(f);h.succeed();
    });
    });
  }

  @GameTest(template="empty",timeoutTicks=RESIDENT_WARMUP_TICKS+TRANSACTION_TICKS)
  public static void customCraftedEggAndCarrierGoodsAreExcludedAndChangedQuotesCannotCommit(GameTestHelper h){
    var f=fixture(h);ready(h,f,()->{var p=f.player;
    p.getInventory().setItem(0,new ItemStack(ChaoGarden.item("run_drive"),4));
    var named=new ItemStack(ChaoGarden.item("run_drive"),4);named.set(DataComponents.CUSTOM_NAME,Component.literal("My keepsake"));p.getInventory().setItem(1,named);
    var customized=new ItemStack(ChaoGarden.item("run_drive"),4);var data=new CompoundTag();data.putString("OwnerNote","preserve");customized.set(DataComponents.CUSTOM_DATA,CustomData.of(data));p.getInventory().setItem(2,customized);
    // A jewel egg would have a positive resale value if the egg ban failed.
    p.getInventory().setItem(3,new ItemStack(ChaoGarden.item("ruby_jewel_egg")));
    var carrier=new ItemStack(ChaoGarden.item("chao_carrier"));
    var child=ChaoGarden.CHAO.get().create(p.serverLevel());h.assertTrue(child!=null,"Cannot create carried Chao fixture");child.owner=p.getUUID();
    h.assertTrue(com.chaogarden.item.ChaoCarrierItem.capture(carrier,child,p),"Cannot build a real filled-carrier exclusion fixture");
    p.getInventory().setItem(4,carrier);
    p.getInventory().setItem(5,new ItemStack(ChaoGarden.item("tasty_fruit_seed"),64));
    p.getInventory().setItem(6,new ItemStack(Items.DIAMOND,64));
    p.getInventory().setItem(7,new ItemStack(ChaoGarden.item("omochao_parts_box")));
    p.getInventory().setItem(8,new ItemStack(ChaoGarden.item("run_drive"),3));
    p.getInventory().setItem(9,new ItemStack(ChaoGarden.item("run_drive"),65));
    h.assertTrue(p.server.getRecipeManager().getRecipes().stream().anyMatch(r->r.value().getResultItem(p.registryAccess()).is(ChaoGarden.item("tasty_fruit_seed"))),"Crafted-seed exclusion fixture lacks a loaded recipe");
    for(int i=1;i<=9;i++)h.assertTrue(KindergartenMarket025.quoteEmeralds(p,i)==0,"Ineligible inventory slot was accepted: "+i);
    var quoted=quote(h,f,open(h,f),0);
    p.getInventory().getItem(0).grow(4);var changed=inventory(p);
    action(p,quoted,quoted.selected(),"market_confirm",token(quoted));var denied=drain(p);
    h.assertTrue(sameInventory(p,changed)&&currency(p)==0,"An inventory lot changed after quoting was still sold");
    h.assertTrue(denied!=null&&denied.proposal().equals("sell|"),"Changed-lot rejection did not invalidate the stale quote");
    h.runAfterDelay(5,()->{
      action(p,quoted,quoted.selected(),"market_confirm",token(quoted));
      h.assertTrue(sameInventory(p,changed),"An invalidated quote became usable after its cooldown");
      finish(f);h.succeed();
    });
    });
  }

  @GameTest(template="empty",timeoutTicks=RESIDENT_WARMUP_TICKS+TRANSACTION_TICKS)
  public static void everyCommitRechecksRangeCursorResidencyAndTheCurrentSessionNonce(GameTestHelper h){
    var f=fixture(h);ready(h,f,()->{var p=f.player;p.getInventory().setItem(0,new ItemStack(ChaoGarden.item("power_drive"),4));
    var quoted=quote(h,f,open(h,f),0);var before=inventory(p);
    p.inventoryMenu.setCarried(new ItemStack(Items.STONE));
    action(p,quoted,quoted.selected(),"market_confirm",token(quoted));
    h.assertTrue(sameInventory(p,before)&&p.inventoryMenu.getCarried().is(Items.STONE),"A nonempty cursor did not block the sale or was consumed");
    p.inventoryMenu.setCarried(ItemStack.EMPTY);var near=p.position();p.setPos(near.add(20,0,0));
    action(p,quoted,quoted.selected(),"market_confirm",token(quoted));
    h.assertTrue(sameInventory(p,before),"An out-of-range quote could still commit");p.setPos(near);
    f.trader.setResident(false);action(p,quoted,quoted.selected(),"market_confirm",token(quoted));
    h.assertTrue(sameInventory(p,before),"A wandering/nonresident merchant accepted resident buyback");f.trader.setResident(true);
    var reopened=open(h,f);h.assertTrue(!reopened.nonce().equals(quoted.nonce()),"Reopening did not replace the old session nonce");
    action(p,quoted,quoted.selected(),"market_confirm",token(quoted));
    h.assertTrue(sameInventory(p,before),"A replaced session's valid old quote could still commit");
    var current=quote(h,f,reopened,0);action(p,current,current.selected(),"market_confirm",token(current));
    h.assertTrue(p.getInventory().countItem(ChaoGarden.item("power_drive"))==0&&currency(p)==1,"Rejected stale/unauthorized attempts corrupted the new authorized session");
    finish(f);h.succeed();
    });
  }
}
