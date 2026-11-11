package com.chaogarden.test;

import com.chaogarden.ChaoGarden;
import com.chaogarden.activities.*;
import com.chaogarden.entity.ChaoEntity;
import com.chaogarden.hub.HubContent;
import com.chaogarden.item.ChaoCarrierItem;
import com.google.gson.JsonParser;
import io.netty.channel.embedded.EmbeddedChannel;
import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.network.protocol.common.ClientboundCustomPayloadPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.gametest.*;

/** The upgrade and reload namespaces run in separate JVMs against the same real world. */
@GameTestHolder("chao_garden_checks040") @PrefixGameTestTemplate(false)
public final class GardenDetectionGameTests040 {
  private static final BlockPos OLD=new BlockPos(1030,80,1030);
  private static UUID id(int n){return new UUID(40,n);}
  private static Set<UUID> screen(ServerPlayer p){
    var channel=(EmbeddedChannel)p.connection.getConnection().channel();
    channel.runPendingTasks();channel.flushOutbound();channel.runPendingTasks();
    Set<UUID> last=null;Object packet;
    while((packet=channel.readOutbound())!=null)if(packet instanceof ClientboundCustomPayloadPacket msg
        && msg.payload() instanceof ActivityNetwork.Snapshot snap){
      last=new HashSet<>();
      for(var row:JsonParser.parseString(snap.json()).getAsJsonObject().getAsJsonArray("entries")){
        if(!last.add(UUID.fromString(row.getAsJsonObject().get("id").getAsString())))throw new AssertionError("Duplicate UI row");
      }
    }
    return last;
  }

  @GameTestHolder("chao_garden_upgrade040") @PrefixGameTestTemplate(false)
  public static final class Upgrade {
    @GameTest(template="empty",timeoutTicks=200)
    public static void detectExistingAndReleaseRealCarrier(GameTestHelper h){
      var level=h.getLevel();var p=TestConnections.makeSurvivalServerPlayer(h,id(1));
      p.teleportTo(1032,80,1032);p.setNoGravity(true);level.getChunkAt(OLD);
      var data=GardenRegistry.get(p.server);var place=GardenRegistry.Place.at(level,OLD);
      h.assertTrue(data.areas.containsKey(place)&&data.entries.get(id(3)).slot==4,"0.25.39 saved area or manual slot missing");
      h.succeedWhen(()->{
        h.assertTrue(level.getEntity(id(2)) instanceof ChaoEntity && data.entries.containsKey(id(2)),"Waiting for the legacy resident");
        var c=(ChaoEntity)level.getEntity(id(2));var e=data.entries.get(id(2));
        h.assertTrue(e.automatic&&e.entered&&e.slot==0&&e.garden.equals(place),"Wrong discovered home");
        ActivityManager.open(p,OLD);h.assertTrue(Set.of(id(2),id(3)).equals(screen(p)),"Network snapshot omitted the detected Chao");
        ActivityManager.handleAction(p,OLD,"toggle",c.getUUID());
        for(int i=0;i<4;i++)data.detect(c,false);
        h.assertFalse(data.entries.containsKey(id(2)),"Dismissed resident returned immediately");
        var carrier=new ItemStack(ChaoGarden.item("chao_carrier"));h.assertTrue(ChaoCarrierItem.capture(carrier,c,p),"Pickup failed");
        h.assertTrue(GardenWorldGameTests025.use(p,carrier,OLD.east(4).below()).consumesAction(),"Real carrier release failed");
        h.assertTrue(carrier.isEmpty()&&data.entries.get(id(2)).automatic,"Release must rediscover existing UUID once");
        var newcomer=ChaoGarden.CHAO.get().create(level);newcomer.owner=p.getUUID();newcomer.setUUID(id(5));newcomer.setNoAi(true);newcomer.setNoGravity(true);newcomer.moveTo(1034.5,80,1032.5,0,0);level.addFreshEntity(newcomer);
        data.placed(newcomer);ActivityManager.handleAction(p,OLD,"toggle",id(5));
        p.server.getPlayerList().remove(p);
      });
    }
  }

  @GameTestHolder("chao_garden_reload040") @PrefixGameTestTemplate(false)
  public static final class Reload {
    @GameTest(template="empty",timeoutTicks=250)
    public static void reloadPickupBoundaryAndPortal(GameTestHelper h){
      var l=h.getLevel();var p=TestConnections.makeSurvivalServerPlayer(h,id(1));var other=TestConnections.makeServerPlayer(h);
      p.teleportTo(1032,80,1032);p.setNoGravity(true);other.teleportTo(1033,80,1032);other.setNoGravity(true);
      var data=GardenRegistry.get(p.server);var place=GardenRegistry.Place.at(l,OLD);
      h.runAtTickTime(65,()->{
        var c=(ChaoEntity)l.getEntity(id(2));h.assertTrue(c!=null && data.entries.get(id(2)).slot==0
            && data.entries.get(id(3)).slot==4 && !data.entries.get(id(3)).automatic,"Restart changed identity or manual slots");
        h.assertTrue(l.getEntity(id(5))!=null&&!data.entries.containsKey(id(5)),"Dismissal was lost on restart");
        var foreign=ChaoGarden.CHAO.get().create(l);foreign.owner=other.getUUID();foreign.setNoAi(true);foreign.moveTo(1034.5,80,1033,0,0);l.addFreshEntity(foreign);data.placed(foreign);
        h.assertFalse(data.entries.containsKey(foreign.getUUID()),"Other keeper was auto-claimed");
        ActivityManager.handleAction(other,OLD,"toggle",id(2));h.assertTrue(screen(other)==null,"Other keeper got a private roster");
        ActivityManager.handleAction(p,OLD,"refresh",null);h.assertTrue(Set.of(id(2),id(3)).equals(screen(p)),"Player snapshots disagree with server");
        c.setPos(1052,80,1032);data.contain(c);
        h.assertTrue(data.areas.get(place).contains(l,c.position()),"Normal escape bypassed resident containment");
        c.teleportTo(1052,80,1032);h.assertFalse(data.entries.containsKey(id(2)),"Explicit move left stale presence");
        c.teleportTo(1034.5,80,1032.5);h.assertTrue(data.entries.containsKey(id(2)),"Return did not rediscover body");
        var nether=p.server.getLevel(net.minecraft.world.level.Level.NETHER);var at=new BlockPos(1,90,1);nether.getChunkAt(at);
        var moved=c.changeDimension(new net.minecraft.world.level.portal.DimensionTransition(nether,at.getCenter(),net.minecraft.world.phys.Vec3.ZERO,0,0,net.minecraft.world.level.portal.DimensionTransition.DO_NOTHING));
        h.assertTrue(moved instanceof ChaoEntity && moved.level()==nether && !data.entries.containsKey(id(2)),"Portal transfer kept old slot");
        moved.changeDimension(new net.minecraft.world.level.portal.DimensionTransition(l,OLD.east(4).getCenter(),net.minecraft.world.phys.Vec3.ZERO,0,0,net.minecraft.world.level.portal.DimensionTransition.DO_NOTHING));
        foreign.discard();
      });
      h.runAtTickTime(100,()->{
        var c=(ChaoEntity)l.getEntity(id(2));h.assertTrue(c!=null&&data.entries.get(id(2)).automatic,"Returning across dimensions failed");
        var stash=new ItemStack(ChaoGarden.item("chao_carrier"));h.assertTrue(ChaoCarrierItem.capture(stash,c,p)&&!data.entries.containsKey(id(2)),"Pickup kept live presence");
        ActivityManager.handleAction(p,OLD,"refresh",null);h.assertTrue(Set.of(id(3)).equals(screen(p)),"Pickup left a ghost in the UI");
        h.assertTrue(GardenWorldGameTests025.use(p,stash,OLD.east(4).below()).consumesAction(),"Second release failed");
        var found=(ChaoEntity)l.getEntity(id(2));found.remove(Entity.RemovalReason.KILLED);h.assertFalse(data.entries.containsKey(id(2)),"Killed body stayed registered");
        p.server.getPlayerList().remove(p);other.server.getPlayerList().remove(other);h.succeed();
      });
    }
  }

  @GameTest(template="empty",timeoutTicks=125)
  public static void capacityAndAuthoredBoundaries(GameTestHelper h){
    var p=TestConnections.makeSurvivalServerPlayer(h);var l=p.server.getLevel(HubContent.GARDEN);
    h.assertTrue(HubContent.ensure(l),"Unable to build garden");var root=com.chaogarden.hub.GardenRooms025.TRANSPORTER;
    p.teleportTo(l,root.getX()+3,root.getY()+1,root.getZ(),0,0);
    var data=GardenRegistry.get(p.server);var place=data.register(p,root).place();var residents=new ArrayList<ChaoEntity>();
    for(int i=0;i<7;i++){var c=ChaoGarden.CHAO.get().create(l);c.owner=p.getUUID();c.setNoAi(true);c.setNoGravity(true);
      var at=data.areas.get(place).home;c.moveTo(at.getX()+.5,at.getY(),at.getZ()+.5,0,0);h.assertTrue(l.addFreshEntity(c),"Published body");data.placed(c);residents.add(c);}
    h.assertTrue(residents.stream().filter(c->data.entries.containsKey(c.getUUID())).count()==6,"Authored garden exceeded six slots");
    var first=residents.getFirst();first.discard();data.detect(residents.getLast(),false);
    h.assertTrue(data.entries.get(residents.getLast().getUUID()).slot==0,"Freed slot did not admit the waiting Chao");
    var last=residents.getLast();last.teleportTo(root.getX()+300,200,root.getZ());
    h.assertFalse(data.entries.containsKey(last.getUUID()),"Outside authored bounds still present published="+last.isAddedToLevel()+" live="+(l.getEntity(last.getUUID())==last)+" position="+last.position());
    residents.forEach(ChaoEntity::discard);data.removeGarden(place);p.server.getPlayerList().remove(p);h.succeed();
  }
  @GameTest(template="activity_test",timeoutTicks=100) public static void enclosedActivation(GameTestHelper h){GardenRegistrationGameTests02517.activationNeedsNoEnclosureOrFood(h);}
  @GameTest(template="activity_test",timeoutTicks=100) public static void manualReservations(GameTestHelper h){GardenRegistrationGameTests02517.sixCarriedSlotsPreserveItemsRejectSpoofsAndPersist(h);}
  @GameTest(template="activity_test",timeoutTicks=100) public static void originalContainment(GameTestHelper h){GardenRegistrationGameTests02517.residentStartsOnEntryAndStopsAtSavedBoundaryUntilUnregistered(h);}
  @GameTest(template="empty",timeoutTicks=220) public static void yellowExit(GameTestHelper h){HubWorldGameTests.physicalTubeStepLatchAndSameCreatureRoundTrip(h);}
  @GameTest(template="empty",timeoutTicks=220) public static void schoolPortals(GameTestHelper h){HubWorldGameTests.kindergartenJourneyKeepsOriginAndMultiplayerModes(h);}
}
