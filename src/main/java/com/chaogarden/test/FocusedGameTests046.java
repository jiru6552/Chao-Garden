package com.chaogarden.test;

import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** Existing native behavior probes selected for the 0.25.46 rendering/input changes. */
@GameTestHolder("chao_focused46") @PrefixGameTestTemplate(false)
public final class FocusedGameTests046 {
  @GameTest(template="empty",timeoutTicks=180,batch="lobby46_geometry")
  public static void lobbyCollisionAndRoutes(GameTestHelper h){
    LobbyApproachGameTests0255.realFlightsUseCelestialStairsAndAnOpenLowerFloorWithoutQuartzOrClosedDarkPlaceholder(h);
  }
  @GameTest(template="empty",timeoutTicks=600,batch="lobby46_walk")
  public static void lobbyRealWalking(GameTestHelper h){
    LobbyApproachGameTests0255.aRealKeeperWalksUpAndDownBothFlightsAndReachesTheTwoElevatedGardenLandings(h);
  }
  @GameTest(template="empty",timeoutTicks=200,batch="lobby46_portal")
  public static void exitRoundTrip(GameTestHelper h){HubWorldGameTests.physicalTubeStepLatchAndSameCreatureRoundTrip(h);}
  @GameTest(template="empty",timeoutTicks=60)
  public static void vanillaElytra(GameTestHelper h){CompanionSkillGameTests.nativeElytraPreservedAndArmorAloneCannotFly(h);}
  @GameTest(template="empty",timeoutTicks=30)
  public static void persistedOrdinaryPhysics(GameTestHelper h){CompanionVariantGameTests.ordinarySnapshotPreservesDeliberatePhysics(h);}
}
