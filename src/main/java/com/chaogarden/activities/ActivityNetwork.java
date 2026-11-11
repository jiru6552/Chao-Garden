package com.chaogarden.activities;

import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

/** Bounded activity messages. The server resolves every participant and permission itself. */
public final class ActivityNetwork {
  private ActivityNetwork() {}

  public record Action(long position, String action, String chaoId) implements CustomPacketPayload {
    public static final Type<Action> TYPE = new Type<>(id("activity_action"));
    public static final StreamCodec<RegistryFriendlyByteBuf, Action> CODEC =
        StreamCodec.composite(
            ByteBufCodecs.VAR_LONG,
            Action::position,
            ByteBufCodecs.stringUtf8(24),
            Action::action,
            ByteBufCodecs.stringUtf8(40),
            Action::chaoId,
            Action::new);

    @Override
    public Type<Action> type() {
      return TYPE;
    }
  }

  public record Register(long position,int inventorySlot,int slot,UUID expected) implements CustomPacketPayload {
    public static final Type<Register> TYPE=new Type<>(id("garden_register"));
    public static final StreamCodec<RegistryFriendlyByteBuf,Register> CODEC=StreamCodec.composite(
      ByteBufCodecs.VAR_LONG,Register::position,ByteBufCodecs.VAR_INT,Register::inventorySlot,
      ByteBufCodecs.VAR_INT,Register::slot,net.minecraft.core.UUIDUtil.STREAM_CODEC,Register::expected,Register::new);
    @Override public Type<Register> type(){return TYPE;}
  }
  public static void registerCarried(BlockPos pos,int inventorySlot,int slot,UUID expected) {
    PacketDistributor.sendToServer(new Register(pos.asLong(),inventorySlot,slot,expected));
  }

  public record Snapshot(long position, String json) implements CustomPacketPayload {
    public static final Type<Snapshot> TYPE = new Type<>(id("activity_snapshot"));
    public static final StreamCodec<RegistryFriendlyByteBuf, Snapshot> CODEC =
        StreamCodec.composite(
            ByteBufCodecs.VAR_LONG,
            Snapshot::position,
            ByteBufCodecs.stringUtf8(200_000),
            Snapshot::json,
            Snapshot::new);

    @Override
    public Type<Snapshot> type() {
      return TYPE;
    }
  }

  private static ResourceLocation id(String name) {
    return ResourceLocation.fromNamespaceAndPath("chao_garden", name);
  }

  public static void register(IEventBus bus) {
    bus.addListener(ActivityNetwork::payloads);
  }

  private static void payloads(RegisterPayloadHandlersEvent event) {
    var registrar = event.registrar("activities_2");
    registrar.playToServer(Register.TYPE,Register.CODEC,(m,c)->{if(c.player() instanceof ServerPlayer p)ActivityManager.registerCarried(p,BlockPos.of(m.position()),m.inventorySlot(),m.slot(),m.expected());});
    registrar.playToServer(
        Action.TYPE,
        Action.CODEC,
        (message, context) -> {
          if (!(context.player() instanceof ServerPlayer player)) return;
          UUID chao = null;
          if (!message.chaoId().isBlank()) {
            try {
              chao = UUID.fromString(message.chaoId());
            } catch (IllegalArgumentException ignored) {
              return;
            }
          }
          ActivityManager.handleAction(
              player, BlockPos.of(message.position()), message.action(), chao);
        });
    // Client classes are referenced only inside this handler's body; a
    // dedicated server registers the protocol without loading client screens.
    registrar.playToClient(
        Snapshot.TYPE,
        Snapshot.CODEC,
        (message, context) ->
            com.chaogarden.client.activities.ActivityScreen.receive(
                BlockPos.of(message.position()), message.json()));
  }

  public static void sendScreen(ServerPlayer player, BlockPos position, String json) {
    if (json.length() > 200_000) throw new IllegalArgumentException("Activity roster is too large");
    PacketDistributor.sendToPlayer(player, new Snapshot(position.asLong(), json));
  }

  public static void request(BlockPos position, String action, UUID chao) {
    PacketDistributor.sendToServer(
        new Action(position.asLong(), action, chao == null ? "" : chao.toString()));
  }
}
