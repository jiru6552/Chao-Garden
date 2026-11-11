package com.chaogarden.kindergarten;

import com.chaogarden.market.BlackMarketTrader;
import com.chaogarden.market.MarketStock;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

/** A resident-only scene menu and atomic inventory buyback, preserving the real shop's own stock. */
@EventBusSubscriber(modid="chao_garden")
public final class KindergartenMarket025 {
  private static final Map<UUID,Session> SESSIONS=new HashMap<>();
  private static final Set<UUID> COMMITTING=new HashSet<>();
  private static final class Session {
    final int actor;final UUID actorUuid,nonce=UUID.randomUUID();long opened,lastRead=Long.MIN_VALUE,lastWrite=Long.MIN_VALUE;
    String mode="home",token="";int slot=-1,price;ItemStack quoted=ItemStack.EMPTY;
    Session(BlackMarketTrader trader,long time){actor=trader.getId();actorUuid=trader.getUUID();opened=time;}
  }
  private record Offer(int slot,ItemStack stack,int emeralds){}
  private KindergartenMarket025(){}
  public static boolean authorized(ServerPlayer p,BlackMarketTrader t){return p.isAlive()&&!p.isSpectator()&&KindergartenContent.inKindergarten(p.level())&&t.level()==p.level()&&t.isAlive()&&t.resident()
      &&p.distanceToSqr(t)<=64&&p.containerMenu==p.inventoryMenu&&p.inventoryMenu.getCarried().isEmpty();}
  @SubscribeEvent(priority=net.neoforged.bus.api.EventPriority.HIGHEST)
  public static void interact(net.neoforged.neoforge.event.entity.player.PlayerInteractEvent.EntityInteract e){
    if(!(e.getTarget() instanceof BlackMarketTrader trader)||!trader.resident()||!KindergartenContent.inKindergarten(trader.level()))return;
    if(e.getEntity() instanceof ServerPlayer p&&e.getHand()==InteractionHand.MAIN_HAND)open(p,trader);
    e.setCanceled(true);e.setCancellationResult(InteractionResult.SUCCESS);
  }
  public static void open(ServerPlayer p,BlackMarketTrader trader){if(!authorized(p,trader))return;var s=new Session(trader,p.level().getGameTime());SESSIONS.put(p.getUUID(),s);send(p,trader,s,"");}
  private static Set<net.minecraft.world.item.Item> recipeOutputs(ServerPlayer p){
    var result=new HashSet<net.minecraft.world.item.Item>();for(var recipe:p.server.getRecipeManager().getRecipes()){var item=recipe.value().getResultItem(p.registryAccess());if(!item.isEmpty())result.add(item.getItem());}return result;
  }
  private static Offer quote(ServerPlayer p,int slot,Set<net.minecraft.world.item.Item> crafted){
    if(slot<0||slot>=36)return null;var stack=p.getInventory().getItem(slot);if(stack.isEmpty()||stack.getCount()>stack.getMaxStackSize())return null;
    var key=BuiltInRegistries.ITEM.getKey(stack.getItem());if(!key.getNamespace().equals("chao_garden")||MarketBuybackRules025.forbidden(key.getPath()))return null;
    var product=MarketStock.catalog().get(key.getPath());if(product==null||!ItemStack.isSameItemSameComponents(stack,stack.getItem().getDefaultInstance()))return null;
    // Refuse recipe outputs, including fruit-to-seed and crafted-cosmetic conversions. Recheck on every quote/commit after datapack reloads.
    if(crafted.contains(stack.getItem()))return null;
    int value=MarketBuybackRules025.emeralds(product.listing().emeralds(),stack.getCount());return value>0?new Offer(slot,stack,value):null;
  }
  private static List<Offer> offers(ServerPlayer p){var result=new ArrayList<Offer>();var crafted=recipeOutputs(p);for(int i=0;i<36;i++){var offer=quote(p,i,crafted);if(offer!=null)result.add(offer);}return result;}
  private static void send(ServerPlayer p,BlackMarketTrader trader,Session s,String result){
    var offers=offers(p);String options=String.join("\n",offers.stream().map(o->"s:"+o.slot+"|"+o.stack.getCount()+" × "+o.stack.getHoverName().getString().codePoints().limit(40).collect(StringBuilder::new,StringBuilder::appendCodePoint,StringBuilder::append)+" — "+o.emeralds+" emeralds").toList());
    String message=result.isEmpty()?s.mode.equals("home")?"Welcome to the Black Market. Would you like to go shopping, or sell something?":offers.isEmpty()?"You don't have anything to sell. Bring plain market goods in a large enough lot; crafted items, eggs and Chao are not accepted.":s.mode.equals("quote")?"Sell "+s.quoted.getCount()+" × "+s.quoted.getHoverName().getString()+" for "+s.price+" emeralds?":"Choose an inventory lot to sell. The quote is one quarter of its catalog purchase value, rounded down for the whole lot.":result;
    KindergartenNetwork.send(p,new KindergartenNetwork.Open(trader.getId(),s.nonce.toString(),"MARKET",s.slot<0?"":"s:"+s.slot+":"+s.token,"Black Market","",options,message,s.mode+"|"+s.token));
  }
  public static void handle(ServerPlayer p,KindergartenNetwork.Action r){
    Session s=SESSIONS.get(p.getUUID());long now=p.level().getGameTime();
    if(s==null||s.actor!=r.npc()||!s.nonce.toString().equals(r.nonce())||now-s.opened>12000
        ||!(p.serverLevel().getEntity(s.actor) instanceof BlackMarketTrader t)||!t.getUUID().equals(s.actorUuid)||!authorized(p,t))return;
    if(r.action().equals("exit")){SESSIONS.remove(p.getUUID());return;}
    boolean read=r.action().equals("select")||r.action().equals("refresh")||r.action().equals("market_sell")||r.action().equals("market_back");
    long last=read?s.lastRead:s.lastWrite;if(last!=Long.MIN_VALUE&&now-last<4)return;if(read)s.lastRead=now;else s.lastWrite=now;s.opened=now;
    if(!COMMITTING.add(p.getUUID()))return;
    String result="";
    try{
      switch(r.action()){
        case "market_shop"->{SESSIONS.remove(p.getUUID());t.mobInteract(p,InteractionHand.MAIN_HAND);return;}
        case "market_sell"->{s.mode="sell";s.quoted=ItemStack.EMPTY;s.token="";s.slot=-1;}
        case "market_back"->{s.mode="home";s.quoted=ItemStack.EMPTY;s.token="";s.slot=-1;}
        case "select"->{int slot=-1;try{String[] key=r.selection().split(":");if(key.length>=2&&key[0].equals("s"))slot=Integer.parseInt(key[1]);}catch(NumberFormatException ignored){}var q=quote(p,slot,recipeOutputs(p));if(q==null)result="That inventory lot is no longer available.";else{s.mode="quote";s.slot=slot;s.quoted=q.stack.copy();s.price=q.emeralds;s.token=UUID.randomUUID().toString();}}
        case "market_confirm"->{
          if(!s.mode.equals("quote")||!s.token.equals(r.value())||s.quoted.isEmpty())return;
          var q=quote(p,s.slot,recipeOutputs(p));
          if(q==null||q.emeralds!=s.price||q.stack.getCount()!=s.quoted.getCount()||!ItemStack.isSameItemSameComponents(q.stack,s.quoted)){s.quoted=ItemStack.EMPTY;s.token="";s.mode="sell";result="The item changed. Please choose it again.";break;}
          var plan=plan(p,s.slot,s.price);if(plan==null){result="Make room for the emerald payment, then confirm again. Nothing has been sold.";break;}
          // No drops or external insertion callbacks: publish a fully planned inventory exactly once.
          s.quoted=ItemStack.EMPTY;s.token="";s.mode="sell";s.slot=-1;
          for(int i=0;i<36;i++)if(!ItemStack.matches(p.getInventory().getItem(i),plan.get(i)))p.getInventory().setItem(i,plan.get(i));p.getInventory().setChanged();p.containerMenu.broadcastChanges();result="Thank you. The emerald payment is in your inventory.";
        }
        case "refresh"->{}
        default->{return;}
      }
    }finally{COMMITTING.remove(p.getUUID());}
    send(p,t,s,result);
  }
  /** Public test seam is read-only; no quote or component comes from the client. */
  public static int quoteEmeralds(ServerPlayer p,int slot){var q=quote(p,slot,recipeOutputs(p));return q==null?0:q.emeralds;}
  private static List<ItemStack> plan(ServerPlayer p,int slot,int emeralds){
    var inventory=new ArrayList<ItemStack>();for(int i=0;i<36;i++)inventory.add(p.getInventory().getItem(i).copy());inventory.set(slot,ItemStack.EMPTY);
    int blocks=emeralds<=64?0:emeralds/9,coins=emeralds<=64?emeralds:emeralds%9;
    while(blocks>0){int n=Math.min(64,blocks);if(!insert(inventory,new ItemStack(Items.EMERALD_BLOCK,n)))return null;blocks-=n;}
    if(coins>0&&!insert(inventory,new ItemStack(Items.EMERALD,coins)))return null;return inventory;
  }
  private static boolean insert(List<ItemStack> inventory,ItemStack stack){
    for(var target:inventory)if(ItemStack.isSameItemSameComponents(target,stack)&&target.getCount()<target.getMaxStackSize()){int n=Math.min(stack.getCount(),target.getMaxStackSize()-target.getCount());target.grow(n);stack.shrink(n);if(stack.isEmpty())return true;}
    for(int i=0;i<inventory.size();i++)if(inventory.get(i).isEmpty()){inventory.set(i,stack.copy());return true;}return false;
  }
  @SubscribeEvent public static void logout(net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerLoggedOutEvent e){SESSIONS.remove(e.getEntity().getUUID());}
  @SubscribeEvent public static void stop(net.neoforged.neoforge.event.server.ServerStoppingEvent e){SESSIONS.clear();COMMITTING.clear();}
}
