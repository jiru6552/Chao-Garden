package com.chaogarden.test;

import com.chaogarden.ChaoGarden;
import com.chaogarden.hub.DarkGardenRooms0255;
import com.chaogarden.hub.HubContent;
import com.chaogarden.hub.HubRooms;
import com.chaogarden.hub.HubWorldData;
import java.util.function.Consumer;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.EntityTravelToDimensionEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** Real lower-gate travel, Adventure enforcement and original-world identity. */
@GameTestHolder(ChaoGarden.MODID)
@PrefixGameTestTemplate(false)
public final class DarkIntegrationGameTests0255 {
  private static void adventure(GameTestHelper h,ServerPlayer p){
    h.assertTrue(p.gameMode.getGameModeForPlayer()==GameType.ADVENTURE&&!p.mayBuild(),"Dark Garden visitor gained ordinary build permission");
  }
  @GameTest(template="empty",batch="dark0255_origin",timeoutTicks=180)
  public static void darkAdmissionDeniedTravelAndRoundTripKeepEveryOriginalModeAndInventory(GameTestHelper h){
    for(var original:GameType.values()){
      var p=GardenWorldGameTests025.player(h,original);var home=p.serverLevel();var origin=p.position();
      var keepsake=new ItemStack(Items.EMERALD,7);p.getInventory().setItem(8,keepsake.copy());
      try{
        h.assertTrue(HubContent.enterHub(p),"Hub admission failed for "+original);p.hasChangedDimension();
        var journal=HubWorldData.get(p.server).visit(p.getUUID()).copy();
        h.assertTrue(journal.getInt("GameMode")==original.getId(),"Original mode missing from Dark journey journal");
        if(original==GameType.SURVIVAL){
          Consumer<EntityTravelToDimensionEvent> deny=e->{if(e.getEntity()==p&&e.getDimension().equals(HubContent.DARK_GARDEN))e.setCanceled(true);};
          NeoForge.EVENT_BUS.addListener(deny);
          try{h.assertTrue(!HubContent.visitDarkGarden(p),"Dark entry ignored denied native travel");}
          finally{NeoForge.EVENT_BUS.unregister(deny);}
          h.assertTrue(p.level().dimension().equals(HubContent.HUB)&&journal.equals(HubWorldData.get(p.server).visit(p.getUUID())),"Denied Dark travel altered the visitor or return journal");
        }
        h.assertTrue(HubContent.visitDarkGarden(p)&&p.level().dimension().equals(HubContent.DARK_GARDEN),"Dark admission failed for "+original);p.hasChangedDimension();adventure(h,p);
        p.setGameMode(GameType.SURVIVAL);HubContent.tickPlayer(p);adventure(h,p);
        h.assertTrue(journal.equals(HubWorldData.get(p.server).visit(p.getUUID())),"Dark visit overwrote the true home journal");
        h.assertTrue(HubContent.returnToHub(p)&&p.level().dimension().equals(HubContent.HUB),"Dark return missed the lobby");p.hasChangedDimension();adventure(h,p);
        var at=p.blockPosition();h.assertTrue(p.position().distanceTo(HubRooms.DARK_RETURN_LANDING)<.01&&p.getY()<79
            &&p.serverLevel().getBlockState(at.below()).isFaceSturdy(p.serverLevel(),at.below(),Direction.UP)
            &&p.serverLevel().noCollision(p)&&!HubContent.inDarkDoor(p),"Dark return did not reach clear supported lower landing");
        HubContent.tickPlayer(p);
        h.assertTrue(p.level().dimension().equals(HubContent.HUB)&&p.position().distanceTo(HubRooms.DARK_RETURN_LANDING)<.05,"Lower landing was mistaken for an out-of-bounds fall");
        h.assertTrue(HubContent.returnHome(p)&&p.level()==home&&p.position().distanceTo(origin)<5
            &&p.gameMode.getGameModeForPlayer()==original&&!HubContent.hasReturnJournal(p)
            &&ItemStack.matches(p.getInventory().getItem(8),keepsake),"Dark round trip changed origin, mode, inventory or journal");
      }finally{GardenWorldGameTests025.finish(p);}
    }
    h.succeed();
  }
  @GameTest(template="empty",batch="dark0255_walking",timeoutTicks=200)
  public static void eastFacingLowerGateAndGardenReturnUseRealMovementAndCooldown(GameTestHelper h){
    var p=GardenWorldGameTests025.player(h,GameType.SURVIVAL);var home=p.serverLevel();var origin=p.position();
    boolean[] done={false};Runnable cleanup=()->{if(!done[0]){done[0]=true;GardenWorldGameTests025.finish(p);}};h.runAtTickTime(199,cleanup);
    try{
      h.assertTrue(HubContent.enterHub(p),"Hub unavailable for Dark walking route");p.hasChangedDimension();
      var journal=HubWorldData.get(p.server).visit(p.getUUID()).copy();var gate=HubRooms.DARK_DOOR;
      p.teleportTo(gate.getX()+.5,gate.getY()+.1,gate.getZ()-2.0);
      h.assertTrue(!HubContent.inDarkDoor(p),"Lower gate accepts traffic two blocks before its plane");HubContent.tickPlayer(p);
      h.startSequence().thenIdle(45).thenExecute(()->{try{
        p.move(MoverType.SELF,new Vec3(0,0,2.2));h.assertTrue(HubContent.inDarkDoor(p),"Physical movement did not enter the lower Dark aperture");HubContent.tickPlayer(p);p.hasChangedDimension();
        h.assertTrue(p.level().dimension().equals(HubContent.DARK_GARDEN),"Lower gate did not enter the Dark Garden");adventure(h,p);
        h.assertTrue(journal.equals(HubWorldData.get(p.server).visit(p.getUUID())),"Walking admission replaced original journal");
        var exit=DarkGardenRooms0255.EXIT_PORTAL;
        p.teleportTo(exit.getX()+2.5,exit.getY()+.1,exit.getZ()+.5);HubContent.tickPlayer(p);
        h.assertTrue(!HubContent.inDarkReturn(p),"Dark return aperture has oversized depth");
      }catch(RuntimeException failure){cleanup.run();throw failure;}}).thenIdle(45).thenExecute(()->{try{
        p.move(MoverType.SELF,new Vec3(-1.7,0,0));h.assertTrue(HubContent.inDarkReturn(p),"Real movement could not enter the Dark return");HubContent.tickPlayer(p);p.hasChangedDimension();
        h.assertTrue(p.level().dimension().equals(HubContent.HUB)&&p.position().distanceTo(HubRooms.DARK_RETURN_LANDING)<.01&&!HubContent.inDarkDoor(p),"Walking Dark return missed its lower platform");
        HubContent.tickPlayer(p);h.assertTrue(p.level().dimension().equals(HubContent.HUB)&&journal.equals(HubWorldData.get(p.server).visit(p.getUUID())),"Dark arrival bounced or changed home journal");
        h.assertTrue(HubContent.returnHome(p)&&p.level()==home&&p.position().distanceTo(origin)<5&&!HubContent.hasReturnJournal(p),"Dark walking route could not finish at home");cleanup.run();h.succeed();
      }catch(RuntimeException failure){cleanup.run();throw failure;}});
    }catch(RuntimeException failure){cleanup.run();throw failure;}
  }
  private DarkIntegrationGameTests0255(){}
}
