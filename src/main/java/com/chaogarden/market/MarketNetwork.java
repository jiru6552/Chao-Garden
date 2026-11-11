package com.chaogarden.market;

import com.chaogarden.ChaoGarden;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.MerchantMenu;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerContainerEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

/** Supplements vanilla merchant transactions with a read-only stock clock. */
public final class MarketNetwork {
  private MarketNetwork() {}
  public record StockClock(int container, int merchant, int remaining) implements CustomPacketPayload {
    public static final Type<StockClock> TYPE=new Type<>(ResourceLocation.fromNamespaceAndPath(ChaoGarden.MODID,"market_clock"));
    public static final StreamCodec<RegistryFriendlyByteBuf,StockClock> CODEC=StreamCodec.composite(
        ByteBufCodecs.VAR_INT,StockClock::container,ByteBufCodecs.VAR_INT,StockClock::merchant,
        ByteBufCodecs.VAR_INT,StockClock::remaining,StockClock::new);
    @Override public Type<StockClock> type(){return TYPE;}
  }
  public static void register(IEventBus bus){
    bus.addListener(MarketNetwork::payloads);
    NeoForge.EVENT_BUS.addListener(MarketNetwork::opened);
  }
  private static void payloads(RegisterPayloadHandlersEvent event){
    event.registrar("market_1").playToClient(StockClock.TYPE,StockClock.CODEC,(message,context)->
        com.chaogarden.market.client.CweMarketClient.receive(message));
  }
  private static void opened(PlayerContainerEvent.Open event){
    if(!(event.getEntity() instanceof ServerPlayer player)||!(event.getContainer() instanceof MerchantMenu menu))return;
    var merchants=player.serverLevel().getEntitiesOfClass(BlackMarketTrader.class,player.getBoundingBox().inflate(12),
        trader->trader.getTradingPlayer()==player);
    if(merchants.size()!=1)return;
    var trader=merchants.getFirst();
    PacketDistributor.sendToPlayer(player,new StockClock(menu.containerId,trader.getId(),trader.restockTicksRemaining()));
  }
}
