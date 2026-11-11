package com.chaogarden.test;

import com.mojang.authlib.GameProfile;
import io.netty.channel.embedded.EmbeddedChannel;
import java.util.UUID;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.neoforged.neoforge.network.registration.NetworkRegistry;

/** Real mod payload negotiation for the in-memory connections used by GameTests. */
public final class TestConnections {
  private TestConnections() {}

  /** Historical migration fixture only. The Bell ID now aliases the Garden block in production. */
  public static net.minecraft.world.item.Item legacyGardenBell() {
    return LegacyGardenFixture.ITEM;
  }

  public static void configure(ServerPlayer player) {
    NetworkRegistry.configureMockConnection(player.connection.getConnection());
  }

  /** Configure before login: Accessories sends its actual data payload during placeNewPlayer. */
  public static ServerPlayer makeServerPlayer(GameTestHelper helper) {
    var level = helper.getLevel();
    var cookie = CommonListenerCookie.createInitial(
        new GameProfile(UUID.randomUUID(), "test-mock-player"), false);
    ServerPlayer player = new ServerPlayer(level.getServer(), level, cookie.gameProfile(), cookie.clientInformation()) {
      @Override public boolean isSpectator() { return false; }
      @Override public boolean isCreative() { return true; }
    };
    Connection connection = new Connection(PacketFlow.SERVERBOUND);
    new EmbeddedChannel(connection);
    NetworkRegistry.configureMockConnection(connection);
    level.getServer().getPlayerList().placeNewPlayer(connection, player, cookie);
    return player;
  }
  /** Damage and effect regressions must use real survival abilities. */
  public static ServerPlayer makeSurvivalServerPlayer(GameTestHelper helper) {
    return makeSurvivalServerPlayer(helper,UUID.randomUUID());
  }
  static ServerPlayer makeSurvivalServerPlayer(GameTestHelper helper, UUID id) {
    var level=helper.getLevel();
    var cookie=CommonListenerCookie.createInitial(new GameProfile(id,"test-survival-player"),false);
    // Use actual game-mode predicates: tests may later switch this actor to creative.
    ServerPlayer player=new ServerPlayer(level.getServer(),level,cookie.gameProfile(),cookie.clientInformation());
    Connection connection=new Connection(PacketFlow.SERVERBOUND);
    new EmbeddedChannel(connection);
    NetworkRegistry.configureMockConnection(connection);
    level.getServer().getPlayerList().placeNewPlayer(connection,player,cookie);
    player.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
    player.getAbilities().invulnerable=false;player.getAbilities().instabuild=false;
    player.getAbilities().mayfly=false;player.getAbilities().flying=false;
    return player;
  }

}
