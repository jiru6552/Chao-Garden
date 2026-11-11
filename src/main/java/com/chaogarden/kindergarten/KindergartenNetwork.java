package com.chaogarden.kindergarten;

import com.chaogarden.ChaoGarden;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

/** The client requests actions; selections, ownership, stock and mutations remain server-owned. */
public final class KindergartenNetwork {
  private KindergartenNetwork(){}
  private static ResourceLocation id(String path){return ResourceLocation.fromNamespaceAndPath(ChaoGarden.MODID,path);}
  public record Open(int npc,String nonce,String role,String selected,String name,String state,String options,String message,String proposal) implements CustomPacketPayload {
    public static final Type<Open> TYPE=new Type<>(id("kindergarten_open"));
    public static final StreamCodec<RegistryFriendlyByteBuf,Open> CODEC=StreamCodec.of((b,v)->{
      b.writeVarInt(v.npc);b.writeUtf(v.nonce,36);b.writeUtf(v.role,16);b.writeUtf(v.selected,100);b.writeUtf(v.name,256);
      b.writeUtf(v.state,32767);b.writeUtf(v.options,8192);b.writeUtf(v.message,8192);b.writeUtf(v.proposal,64);
    },b->new Open(b.readVarInt(),b.readUtf(36),b.readUtf(16),b.readUtf(100),b.readUtf(256),b.readUtf(32767),b.readUtf(8192),b.readUtf(8192),b.readUtf(64)));
    @Override public Type<Open> type(){return TYPE;}
  }
  public record Action(int npc,String nonce,String selection,String action,String value) implements CustomPacketPayload {
    public static final Type<Action> TYPE=new Type<>(id("kindergarten_action"));
    public static final StreamCodec<RegistryFriendlyByteBuf,Action> CODEC=StreamCodec.of((b,v)->{
      b.writeVarInt(v.npc);b.writeUtf(v.nonce,36);b.writeUtf(v.selection,100);b.writeUtf(v.action,16);b.writeUtf(v.value,64);
    },b->new Action(b.readVarInt(),b.readUtf(36),b.readUtf(100),b.readUtf(16),b.readUtf(64)));
    @Override public Type<Action> type(){return TYPE;}
  }
  public static void register(IEventBus bus){bus.addListener(KindergartenNetwork::payloads);}
  private static void payloads(RegisterPayloadHandlersEvent event){var r=event.registrar("kindergarten_2");
    r.playToServer(Action.TYPE,Action.CODEC,(m,c)->{if(c.player() instanceof ServerPlayer p)KindergartenContent.handle(p,m);});
    r.playToClient(Open.TYPE,Open.CODEC,(m,c)->com.chaogarden.client.KindergartenScreen.receive(m));
  }
  static void send(ServerPlayer player,Open message){PacketDistributor.sendToPlayer(player,message);}
  public static void request(Open open,String selection,String action,String value){PacketDistributor.sendToServer(new Action(open.npc,open.nonce,selection,action,value));}
}
