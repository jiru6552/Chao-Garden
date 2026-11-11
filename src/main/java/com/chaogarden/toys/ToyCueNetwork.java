package com.chaogarden.toys;

import com.chaogarden.entity.ChaoEntity;
import java.util.UUID;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.*;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

/** Server-authored effects whose precise attachment lives in the client rig. */
public final class ToyCueNetwork {
  private ToyCueNetwork(){}
  public record Bubble(UUID chao,long until,int elapsed) implements CustomPacketPayload {
    public static final Type<Bubble> TYPE=new Type<>(ResourceLocation.fromNamespaceAndPath("chao_garden","bubble_cue"));
    public static final StreamCodec<RegistryFriendlyByteBuf,Bubble> CODEC=StreamCodec.composite(
        UUIDUtil.STREAM_CODEC,Bubble::chao,ByteBufCodecs.VAR_LONG,Bubble::until,ByteBufCodecs.VAR_INT,Bubble::elapsed,Bubble::new);
    @Override public Type<Bubble> type(){return TYPE;}
  }
  public static void register(IEventBus bus){bus.addListener(ToyCueNetwork::payloads);}
  private static void payloads(RegisterPayloadHandlersEvent event){
    event.registrar("toys_1").playToClient(Bubble.TYPE,Bubble.CODEC,
        (cue,context)->com.chaogarden.client.ChaoBubbleCues.receive(cue));
  }
  public static void bubble(ChaoEntity chao,int elapsed){
    if(!(chao.level() instanceof ServerLevel level)||!BubblePlayRules.puffDue(elapsed))return;
    var cue=new Bubble(chao.getUUID(),chao.toyPlayingUntil(),elapsed);
    for(var player:level.players())if(player.distanceToSqr(chao)<32*32)PacketDistributor.sendToPlayer(player,cue);
  }
}
