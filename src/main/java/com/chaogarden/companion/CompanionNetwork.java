package com.chaogarden.companion;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

/** Requests contain no supplied rank, movement vector, cooldown or companion identity. */
public final class CompanionNetwork {
  private CompanionNetwork() {}
  public record Boost(boolean pressed) implements CustomPacketPayload {
    public static final Type<Boost> TYPE=new Type<>(ResourceLocation.fromNamespaceAndPath("chao_garden","companion_boost"));
    public static final StreamCodec<RegistryFriendlyByteBuf,Boost> CODEC=StreamCodec.composite(ByteBufCodecs.BOOL,Boost::pressed,Boost::new);
    @Override public Type<Boost> type(){return TYPE;}
  }
  public record OpenEnderChest(boolean pressed) implements CustomPacketPayload {
    public static final Type<OpenEnderChest> TYPE=new Type<>(ResourceLocation.fromNamespaceAndPath("chao_garden","companion_ender_chest"));
    public static final StreamCodec<RegistryFriendlyByteBuf,OpenEnderChest> CODEC=StreamCodec.composite(ByteBufCodecs.BOOL,OpenEnderChest::pressed,OpenEnderChest::new);
    @Override public Type<OpenEnderChest> type(){return TYPE;}
  }
  public record Status(boolean active,boolean elytra,boolean robot,int glideRemaining,int boostCooldown) implements CustomPacketPayload {
    public static final Type<Status> TYPE=new Type<>(ResourceLocation.fromNamespaceAndPath("chao_garden","companion_status"));
    public static final StreamCodec<RegistryFriendlyByteBuf,Status> CODEC=StreamCodec.composite(
        ByteBufCodecs.BOOL,Status::active,ByteBufCodecs.BOOL,Status::elytra,ByteBufCodecs.BOOL,Status::robot,
        ByteBufCodecs.VAR_INT,Status::glideRemaining,ByteBufCodecs.VAR_INT,Status::boostCooldown,Status::new);
    @Override public Type<Status> type(){return TYPE;}
  }
  public static void register(IEventBus bus){bus.addListener(CompanionNetwork::payloads);}
  private static void payloads(RegisterPayloadHandlersEvent event){
    var registrar=event.registrar("companion_3");
    registrar.playToServer(Boost.TYPE,Boost.CODEC,(message,context)->{
      if(message.pressed()&&context.player() instanceof ServerPlayer player)CompanionContent.requestBoost(player);
    });
    registrar.playToServer(OpenEnderChest.TYPE,OpenEnderChest.CODEC,(message,context)->{
      if(message.pressed()&&context.player() instanceof ServerPlayer player)CompanionContent.requestEnderChest(player);
    });
    registrar.playToClient(Status.TYPE,Status.CODEC,(message,context)->
        CompanionFlight.receive(context.player().getUUID(),message.active(),message.elytra(),message.robot(),message.glideRemaining(),message.boostCooldown()));
  }
  public static void send(ServerPlayer player,Status status){
    if(player.connection!=null&&player.connection.getConnection().isConnected())PacketDistributor.sendToPlayer(player,status);
  }
  public static void requestBoost(){PacketDistributor.sendToServer(new Boost(true));}
  public static void requestEnderChest(){PacketDistributor.sendToServer(new OpenEnderChest(true));}
}
