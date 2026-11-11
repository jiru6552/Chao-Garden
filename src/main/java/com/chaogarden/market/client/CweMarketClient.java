package com.chaogarden.market.client;

import com.chaogarden.ChaoGarden;
import com.chaogarden.market.MarketNetwork;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.MerchantScreen;
import net.minecraft.world.inventory.MerchantMenu;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

/** Only our server-confirmed merchant menu receives the custom presentation. */
@EventBusSubscriber(modid=ChaoGarden.MODID,value=Dist.CLIENT)
public final class CweMarketClient {
  private static MarketNetwork.StockClock pending;
  private static long expires,receivedAt;
  private static net.minecraft.client.multiplayer.ClientLevel pendingLevel;
  private CweMarketClient() {}
  public static void receive(MarketNetwork.StockClock clock){
    var mc=Minecraft.getInstance();if(mc.player==null||mc.level==null||clock.remaining()<0)return;
    pending=clock;pendingLevel=mc.level;receivedAt=mc.level.getGameTime();expires=receivedAt+200;
    open(mc);
  }
  private static void open(Minecraft mc){
    if(pending==null)return;
    if(mc.player==null||mc.level==null||mc.level!=pendingLevel){pending=null;pendingLevel=null;return;}
    if(mc.level.getGameTime()>expires){pending=null;pendingLevel=null;return;}
    if(mc.screen instanceof MerchantScreen screen&&mc.player.containerMenu instanceof MerchantMenu menu
        &&menu.containerId==pending.container()){
      mc.setScreen(new CweMarketScreen(menu,mc.player.getInventory(),screen.getTitle(),
          receivedAt+pending.remaining()));pending=null;pendingLevel=null;
    }
  }
  @SubscribeEvent public static void tick(ClientTickEvent.Post event){open(Minecraft.getInstance());}
}
