package com.chaogarden.test;

import com.chaogarden.ChaoGarden;
import com.chaogarden.hub.HubContent;
import com.chaogarden.hub.HubWorldData;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.UseItemOnBlockEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** Native Adventure permission boundaries, including denied item/placement hooks. */
@GameTestHolder(ChaoGarden.MODID)
@PrefixGameTestTemplate(false)
public final class GardenAdventureGameTests0251 {
  @GameTest(template="empty",batch="garden0252_legacy_bell",timeoutTicks=100)
  public static void legacyOwnedProviderAllowsBellRecoveryWithoutOpeningOutsidePlacement(GameTestHelper h){
    var p=GardenWorldGameTests025.player(h,GameType.SURVIVAL);
    var at=new BlockPos(43,80,25);var free=at.west(2);var floor=at.south(2).below();
    net.minecraft.server.level.ServerLevel level=null;var before=new java.util.LinkedHashMap<BlockPos,net.minecraft.world.level.block.state.BlockState>();
    com.chaogarden.activities.GardenRegistry registry=null;
    try{
      h.assertTrue(HubContent.enterHub(p)&&HubContent.visitGarden(p),"Garden unavailable for owned legacy Bell recovery");
      level=p.serverLevel();registry=com.chaogarden.activities.GardenRegistry.get(p.server);
      for(var pos:java.util.List.of(at,free,floor))before.put(pos,level.getBlockState(pos));
      level.setBlock(at,com.chaogarden.activities.ActivityContent.GARDEN.get().defaultBlockState(),3);
      level.setBlock(free,com.chaogarden.activities.ActivityContent.GARDEN.get().defaultBlockState(),3);
      level.setBlock(floor,net.minecraft.world.level.block.Blocks.STONE.defaultBlockState(),3);
      var place=com.chaogarden.activities.GardenRegistry.Place.at(level,at);registry.register(p,at);
      p.teleportTo(43.5,80.1,27.5);
      var bell=new ItemStack(TestConnections.legacyGardenBell());
      h.assertTrue(GardenWorldGameTests025.use(p,bell,at).consumesAction(),"An existing owned old provider could not be linked before safe migration");
      h.assertTrue(bell.getOrDefault(net.minecraft.core.component.DataComponents.CUSTOM_DATA,net.minecraft.world.item.component.CustomData.EMPTY).copyTag().getCompound("GardenLink").getLong("Pos")==at.asLong(),"Bell recovery did not retain the real legacy provider");
      var stranger=java.util.UUID.randomUUID();registry.gardens.put(place,new com.chaogarden.activities.GardenRegistry.Garden(place,stranger,"Other keeper"));
      var denied=new ItemStack(TestConnections.legacyGardenBell());
      h.assertTrue(!GardenWorldGameTests025.use(p,denied,at).consumesAction()&&!denied.has(net.minecraft.core.component.DataComponents.CUSTOM_DATA),"Recovery bypass linked another keeper's old provider");
      h.assertTrue(!GardenWorldGameTests025.use(p,new ItemStack(TestConnections.legacyGardenBell()),free).consumesAction()
          &&!registry.gardens.containsKey(com.chaogarden.activities.GardenRegistry.Place.at(level,free)),"Recovery bypass created a fresh outside-field garden claim");
      var toy=new ItemStack(ChaoGarden.item("chao_tv"));
      h.assertTrue(!GardenWorldGameTests025.use(p,toy,floor).consumesAction()&&toy.getCount()==1&&level.getBlockState(floor.above()).isAir()&&!p.mayBuild(),"Legacy Bell recovery allowed outside-field item placement or leaked build permission");
    }finally{
      if(level!=null){for(var entry:before.entrySet())level.setBlock(entry.getKey(),entry.getValue(),3);}
      if(registry!=null){registry.gardens.remove(com.chaogarden.activities.GardenRegistry.Place.at(level,at));registry.gardens.remove(com.chaogarden.activities.GardenRegistry.Place.at(level,free));registry.setDirty();}
      GardenWorldGameTests025.finish(p);
    }
    h.succeed();
  }
  @GameTest(template="empty",timeoutTicks=100)
  public static void returnFromTubeLandsBesideItInsteadOfOnItsRoof(GameTestHelper h){
    var p=GardenWorldGameTests025.player(h,GameType.SURVIVAL);var home=p.serverLevel();var base=p.blockPosition();
    try{
      for(var ground:BlockPos.betweenClosed(base.offset(-2,-1,-2),base.offset(2,-1,2)))home.setBlockAndUpdate(ground,net.minecraft.world.level.block.Blocks.STONE.defaultBlockState());
      home.setBlockAndUpdate(base,HubContent.TELEPORTER.get().defaultBlockState());
      home.setBlockAndUpdate(base.above(),HubContent.TELEPORTER.get().defaultBlockState().setValue(
          com.chaogarden.hub.ChaoTeleporterBlock.HALF,net.minecraft.world.level.block.state.properties.DoubleBlockHalf.UPPER));
      p.teleportTo(base.getX()+.5,base.getY()+.1,base.getZ()+.5);HubContent.tickPlayer(p);
      h.assertTrue(p.level().dimension().equals(HubContent.HUB),"Actual tube entry did not enter hub");
      var revisions=HubWorldData.get(p.server).save(new net.minecraft.nbt.CompoundTag(),p.registryAccess()).getCompound("Revisions");
      for(var key:java.util.List.of(HubContent.HUB,HubContent.KINDERGARTEN,HubContent.GARDEN))
        h.assertTrue(revisions.getInt(key.location().toString())==HubContent.ROOM_REVISION,
            "Actual tube entry exposed the lobby before its destination scenery was ready: "+key);
      var recorded=HubWorldData.get(p.server).visit(p.getUUID()).copy();
      h.assertTrue(HubContent.returnHome(p)&&p.level()==home,"Tube return failed");
      String landing=" base="+base+" journal="+recorded+" returned="+p.position()+" feet="+home.getBlockState(p.blockPosition())+" floor="+home.getBlockState(p.blockPosition().below());
      com.mojang.logging.LogUtils.getLogger().info("CHAO_TUBE_RETURN{}",landing);
      h.assertTrue(p.getY()<base.getY()+1&&p.position().distanceTo(net.minecraft.world.phys.Vec3.atCenterOf(base))<4
          &&!home.getBlockState(p.blockPosition()).is(HubContent.TELEPORTER.get())
          &&!home.getBlockState(p.blockPosition().below()).is(HubContent.TELEPORTER.get()),"Return landing selected the tube interior or roof instead of adjacent ground"+landing);
      HubContent.tickPlayer(p);h.assertTrue(p.level()==home&&!HubContent.hasReturnJournal(p),"Side landing immediately reentered the tube");
    }finally{GardenWorldGameTests025.finish(p);home.removeBlock(base.above(),false);home.removeBlock(base,false);}
    h.succeed();
  }

  @GameTest(template="empty",timeoutTicks=100)
  public static void scenicRealmsRejectWildlifeWithoutChangingOrdinaryWorldSpawns(GameTestHelper h){
    var server=h.getLevel().getServer();
    for(var key:java.util.List.of(HubContent.HUB,HubContent.KINDERGARTEN,HubContent.GARDEN)){
      var level=server.getLevel(key);h.assertTrue(level!=null&&HubContent.ensure(level),"Scenic realm unavailable");
      for(var type:java.util.List.of(net.minecraft.world.entity.EntityType.SHEEP,net.minecraft.world.entity.EntityType.PIG,net.minecraft.world.entity.EntityType.ZOMBIE)){
        var mob=type.create(level);h.assertTrue(mob!=null,"Could not construct native spawn fixture");mob.setPos(.5,80.1,5.5);
        h.assertTrue(!level.addFreshEntity(mob),"Scenic realm admitted vanilla wildlife/hostile "+type+" into "+key);
      }
      var settings=level.getBiome(new BlockPos(0,80,0)).value().getMobSettings();
      h.assertTrue(settings.getMobs(net.minecraft.world.entity.MobCategory.CREATURE).isEmpty(),"Authored realm biome still has passive spawn entries");
    }
    var ordinary=net.minecraft.world.entity.EntityType.SHEEP.create(h.getLevel());
    h.assertTrue(ordinary!=null&&h.getLevel().addFreshEntity(ordinary),"Scenery spawn restriction leaked into the ordinary world");ordinary.discard();h.succeed();
  }

  @GameTest(template="empty",timeoutTicks=100)
  public static void everyOriginModeVisitsInAdventureAndReturnsUnchanged(GameTestHelper h){
    for(var original:GameType.values()){
      var p=GardenWorldGameTests025.player(h,original);var home=p.serverLevel();var at=p.position();
      try{
        h.assertTrue(HubContent.enterHub(p)&&HubContent.visitGarden(p),"Garden visit failed for original mode "+original);
        h.assertTrue(p.gameMode.getGameModeForPlayer()==GameType.ADVENTURE&&!p.mayBuild(),"Garden did not enforce Adventure for "+original);
        h.assertTrue(HubWorldData.get(p.server).visit(p.getUUID()).getInt("GameMode")==original.getId(),"Garden overwrote original mode "+original);
        p.setGameMode(GameType.SURVIVAL);HubContent.tickPlayer(p);
        h.assertTrue(p.gameMode.getGameModeForPlayer()==GameType.ADVENTURE&&!p.mayBuild(),"Garden tick failed to repair an existing visitor mode");
        h.assertTrue(HubContent.returnToHub(p)&&HubContent.returnHome(p)&&p.level()==home&&p.position().distanceTo(at)<5
            &&p.gameMode.getGameModeForPlayer()==original&&!HubContent.hasReturnJournal(p),"Garden lost original mode/location/journal "+original);
      }finally{GardenWorldGameTests025.finish(p);}
    }
    h.succeed();
  }

  @GameTest(template="empty",timeoutTicks=100)
  public static void nativeCancellationPreservesItemsAndAdventurePermissions(GameTestHelper h){
    var p=GardenWorldGameTests025.player(h,GameType.SURVIVAL);
    try{
      h.assertTrue(HubContent.enterHub(p)&&HubContent.visitGarden(p),"Garden unavailable for native cancellation checks");
      p.teleportTo(3.5,80.1,5.5);var ground=new BlockPos(5,79,5);var garden=p.serverLevel();
      h.assertTrue(garden.getBlockState(ground.above()).isAir(),"Native placement cancellation needs clear test ground");
      var toy=new ItemStack(ChaoGarden.item("chao_tv"));var before=toy.copy();boolean[] itemSeen={false},placeSeen={false};
      Consumer<UseItemOnBlockEvent> denyUse=e->{if(e.getPlayer()==p&&e.getUsePhase()==UseItemOnBlockEvent.UsePhase.ITEM_AFTER_BLOCK){
        itemSeen[0]=true;e.cancelWithResult(net.minecraft.world.ItemInteractionResult.FAIL);
      }};
      NeoForge.EVENT_BUS.addListener(denyUse);
      try{h.assertTrue(!GardenWorldGameTests025.use(p,toy,ground).consumesAction(),"Garden bypassed a native item-use cancellation");}
      finally{NeoForge.EVENT_BUS.unregister(denyUse);}
      h.assertTrue(itemSeen[0]&&ItemStack.matches(toy,before)&&garden.getBlockState(ground.above()).isAir()&&!p.mayBuild(),"Item cancellation consumed goods or leaked Adventure permission");
      Consumer<BlockEvent.EntityPlaceEvent> denyPlace=e->{if(e.getEntity()==p){
        placeSeen[0]=true;h.assertTrue(p.mayBuild(),"Actual placement did not run inside scoped permission");e.setCanceled(true);
      }};
      NeoForge.EVENT_BUS.addListener(denyPlace);
      try{h.assertTrue(!GardenWorldGameTests025.use(p,toy,ground).consumesAction(),"Garden bypassed a native block-place cancellation");}
      finally{NeoForge.EVENT_BUS.unregister(denyPlace);}
      h.assertTrue(placeSeen[0]&&ItemStack.matches(toy,before)&&garden.getBlockState(ground.above()).isAir()
          &&p.gameMode.getGameModeForPlayer()==GameType.ADVENTURE&&!p.mayBuild(),"Failed native placement changed item/world/mode/permission");
    }finally{GardenWorldGameTests025.finish(p);}
    h.succeed();
  }
}
