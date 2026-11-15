package com.chaogarden.test;

import com.chaogarden.hub.GardenUnlocks081;
import com.chaogarden.hub.HubContent;
import com.chaogarden.hub.HubWorldData;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.MinecraftServer;

/** Explicit admission setup for inherited tests of travel, care and stair collision.
 * Acquisition/locked-state tests use their separate fresh namespace instead.
 * No fixture clears permanent unlocks or changes the general player helper. */
final class GardenAccessFixtures081 {
  private GardenAccessFixtures081() {}
  static void hero(GameTestHelper helper){open(helper,true,false);}
  static void dark(GameTestHelper helper){open(helper,false,true);}
  static void both(GameTestHelper helper){open(helper,true,true);}
  private static void open(GameTestHelper helper,boolean hero,boolean dark){
    var server=helper.getLevel().getServer();
    if(hero)GardenUnlocks081.unlock(server,true);
    if(dark)GardenUnlocks081.unlock(server,false);
    var lobby=server.getLevel(HubContent.HUB);
    helper.assertTrue(lobby!=null&&HubContent.ensure(lobby),"Legacy open-garden fixture requires a safely installed lobby");
    helper.assertTrue((!hero||GardenUnlocks081.canUsePortal(server,true))
        &&(!dark||GardenUnlocks081.canUsePortal(server,false)),"Legacy open-garden fixture requires its real unlocked and installed portal");
  }
  /** Old landscape fixtures replace the room journal temporarily. Preserve any
   * permanent acquisitions observed while that temporary journal was active. */
  static void restoreRoomData(MinecraftServer server,HubWorldData original){
    var observed=HubWorldData.get(server);
    if(observed.heroGardenUnlocked())original.unlockGarden(true);
    if(observed.darkGardenUnlocked())original.unlockGarden(false);
    server.overworld().getDataStorage().set("chao_hub_visits",original);
  }
}
