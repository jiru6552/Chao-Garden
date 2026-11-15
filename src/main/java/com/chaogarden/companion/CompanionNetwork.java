package com.chaogarden.companion;

import net.minecraft.network.RegistryFriendlyByteBuf;
import java.util.UUID;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

/** Eligibility, rank, force and cooldown remain server-owned. The cycle identity
 * is only a stale-intent guard against the authenticated current slot. */
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
  public record AbilityCycle080(int sequence,ResourceLocation dimension,UUID expectedChao) implements CustomPacketPayload {
    public static final Type<AbilityCycle080> TYPE=new Type<>(ResourceLocation.fromNamespaceAndPath("chao_garden","equipped_cycle080"));
    public static final StreamCodec<RegistryFriendlyByteBuf,AbilityCycle080> CODEC=StreamCodec.of((buf,message)->{
      buf.writeVarInt(message.sequence());buf.writeResourceLocation(message.dimension());buf.writeUUID(message.expectedChao());
    },buf->new AbilityCycle080(buf.readVarInt(),buf.readResourceLocation(),buf.readUUID()));
    @Override public Type<AbilityCycle080> type(){return TYPE;}
  }
  public record FullImpulse080(ResourceLocation dimension,int entityId,UUID entityUuid,long sequence,double x,double y,double z) implements CustomPacketPayload {
    public static final Type<FullImpulse080> TYPE=new Type<>(ResourceLocation.fromNamespaceAndPath("chao_garden","equipped_impulse080"));
    public static final StreamCodec<RegistryFriendlyByteBuf,FullImpulse080> CODEC=StreamCodec.of((buf,message)->{
      buf.writeResourceLocation(message.dimension());buf.writeVarInt(message.entityId());buf.writeUUID(message.entityUuid());buf.writeLong(message.sequence());
      buf.writeDouble(message.x());buf.writeDouble(message.y());buf.writeDouble(message.z());
    },buf->new FullImpulse080(buf.readResourceLocation(),buf.readVarInt(),buf.readUUID(),buf.readLong(),buf.readDouble(),buf.readDouble(),buf.readDouble()));
    @Override public Type<FullImpulse080> type(){return TYPE;}
    public Vec3 vector(){return new Vec3(x,y,z);}
  }
  public static void register(IEventBus bus){bus.addListener(CompanionNetwork::payloads);}
  private static void payloads(RegisterPayloadHandlersEvent event){
    var registrar=event.registrar("companion_3");
    registrar.playToServer(AbilityCycle080.TYPE,AbilityCycle080.CODEC,(message,context)->{
      if(context.player() instanceof ServerPlayer player)CompanionContent.requestAbilityCycle080(player,message.sequence(),message.dimension(),message.expectedChao());
    });
    registrar.playToClient(FullImpulse080.TYPE,FullImpulse080.CODEC,(message,context)->EquippedKnockback080.receive(context.player(),message));
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
  public static void requestAbilityCycle080(int sequence,ResourceLocation dimension,UUID expectedChao){PacketDistributor.sendToServer(new AbilityCycle080(sequence,dimension,expectedChao));}
  public static void sendImpulse080(Entity entity,Vec3 vector,long sequence){
    if(!(entity.level() instanceof net.minecraft.server.level.ServerLevel))return;
    PacketDistributor.sendToPlayersTrackingEntityAndSelf(entity,new FullImpulse080(entity.level().dimension().location(),entity.getId(),entity.getUUID(),sequence,vector.x,vector.y,vector.z));
  }
  public static void sendTrackedImpulse080(Entity entity,Vec3 vector,long sequence){
    if(!(entity.level() instanceof net.minecraft.server.level.ServerLevel))return;
    PacketDistributor.sendToPlayersTrackingEntity(entity,new FullImpulse080(entity.level().dimension().location(),entity.getId(),entity.getUUID(),sequence,vector.x,vector.y,vector.z));
  }
  public static void requestBoost(){PacketDistributor.sendToServer(new Boost(true));}
  public static void requestEnderChest(){PacketDistributor.sendToServer(new OpenEnderChest(true));}
}
