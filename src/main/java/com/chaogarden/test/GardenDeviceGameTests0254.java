package com.chaogarden.test;

import com.chaogarden.ChaoGarden;
import com.chaogarden.activities.ActivityContent;
import com.chaogarden.activities.ActivityManager;
import com.chaogarden.activities.GardenDeviceGeometry0254;
import com.chaogarden.activities.GardenRegistry;
import com.chaogarden.activities.GardenTransporterBlock;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** Real item placement/cancellation, canonical provider ownership, harvesting and legacy expansion. */
@GameTestHolder(ChaoGarden.MODID)
@PrefixGameTestTemplate(false)
public final class GardenDeviceGameTests0254 {
  private static BlockPos fixture(GameTestHelper h) {
    var root=h.absolutePos(new BlockPos(4,1,4));var level=h.getLevel();
    for(var at:GardenTransporterBlock.footprint(root))level.setBlockAndUpdate(at,Blocks.AIR.defaultBlockState());
    for(int x=0;x<2;x++)for(int z=0;z<2;z++)level.setBlockAndUpdate(root.offset(x,-1,z),Blocks.STONE.defaultBlockState());
    return root;
  }
  @GameTest(template="activity_test",batch="device0254",timeoutTicks=80)
  public static void nativeEightCellPlacementKeepsOneProviderAndOneHarvest(GameTestHelper h) {
    var player=TestConnections.makeSurvivalServerPlayer(h);var level=h.getLevel();var root=fixture(h);
    var data=GardenRegistry.get(player.server);var place=GardenRegistry.Place.at(level,root);
    try {
      for(var facing:Direction.Plane.HORIZONTAL) {
        player.teleportTo(root.getX()+.5,root.getY(),root.getZ()-2.5);player.setYRot(facing.getOpposite().toYRot());
        var stack=new ItemStack(ChaoGarden.item("garden_block"),2);
        h.assertTrue(GardenWorldGameTests025.use(player,stack,root.below()).consumesAction(),"Native terminal placement failed for "+facing);
        h.assertTrue(stack.getCount()==1,"Eight-cell terminal must consume exactly one native item");
        double minX=2,minY=2,minZ=2,maxX=0,maxY=0,maxZ=0;int drops=0;
        var garden=data.register(player,root);
        for(int part=0;part<8;part++) {
          var at=root.offset(GardenTransporterBlock.offset(part));var state=level.getBlockState(at);
          h.assertTrue(state.is(ActivityContent.GARDEN.get())&&state.getValue(GardenTransporterBlock.PART)==part&&state.getValue(GardenTransporterBlock.FACING)==facing,"Native part/facing missing: "+part+" "+facing);
          h.assertTrue(ActivityManager.controller(level,at).equals(root)&&data.register(player,at)==garden,"A child cell created a second provider identity");
          var shape=state.getShape(level,at,CollisionContext.empty());var collision=state.getCollisionShape(level,at,CollisionContext.empty());
          h.assertTrue(shape.equals(collision)&&shape.equals(GardenDeviceGeometry0254.shape(facing,part)),"Selection/collision differs from generated part mesh");
          for(var box:shape.toAabbs()) {
            h.assertTrue(box.minX>=0&&box.minY>=0&&box.minZ>=0&&box.maxX<=1&&box.maxY<=1&&box.maxZ<=1,"Part geometry leaks into another native cell");
            var offset=GardenTransporterBlock.offset(part);minX=Math.min(minX,box.minX+offset.getX());minY=Math.min(minY,box.minY+offset.getY());minZ=Math.min(minZ,box.minZ+offset.getZ());
            maxX=Math.max(maxX,box.maxX+offset.getX());maxY=Math.max(maxY,box.maxY+offset.getY());maxZ=Math.max(maxZ,box.maxZ+offset.getZ());
          }
          drops+=Block.getDrops(state,level,at,null,player,ItemStack.EMPTY).stream().filter(item->item.is(ChaoGarden.item("garden_block"))).mapToInt(ItemStack::getCount).sum();
        }
        h.assertTrue(minX==0&&minY==0&&minZ==0&&maxX==2&&maxY==2&&maxZ==2,"Terminal does not physically span 2x2x2");
        h.assertTrue(drops==1,"Native loot duplicated a provider item across its parts");
        var bell=new ItemStack(TestConnections.legacyGardenBell());
        h.assertTrue(GardenWorldGameTests025.use(player,bell,root.offset(1,1,1)).consumesAction(),"Bell could not link through an upper device part");
        var link=bell.getOrDefault(net.minecraft.core.component.DataComponents.CUSTOM_DATA,net.minecraft.world.item.component.CustomData.EMPTY).copyTag().getCompound("GardenLink");
        h.assertTrue(link.getLong("Pos")==root.asLong()&&link.getString("Dimension").equals(level.dimension().location().toString()),"Bell persisted child-cell identity instead of canonical root");
        h.assertTrue(player.gameMode.destroyBlock(root.offset(1,1,1)),"Survival could not harvest an upper child");
        for(var at:GardenTransporterBlock.footprint(root))h.assertTrue(level.getBlockState(at).isAir(),"Harvest left an orphan device part");
        h.assertTrue(!data.gardens.containsKey(place),"Harvest kept removed canonical provider membership");
      }
    } finally {level.removeBlock(root,false);data.removeGarden(place);player.server.getPlayerList().remove(player);}
    h.succeed();
  }
  @GameTest(template="activity_test",batch="device0254",timeoutTicks=80)
  public static void obstructedAndCanceledPlacementCannotConsumeOrLeavePartialDevice(GameTestHelper h) {
    var player=TestConnections.makeSurvivalServerPlayer(h);var level=h.getLevel();var root=fixture(h);
    try {
      player.teleportTo(root.getX()+.5,root.getY(),root.getZ()-2.5);
      level.setBlockAndUpdate(root.offset(1,1,1),Blocks.CHEST.defaultBlockState());
      var stack=new ItemStack(ChaoGarden.item("garden_block"),2);
      h.assertTrue(!GardenWorldGameTests025.use(player,stack,root.below()).consumesAction()&&stack.getCount()==2,"Obstructed placement consumed a terminal");
      for(var at:GardenTransporterBlock.footprint(root))h.assertTrue(!level.getBlockState(at).is(ActivityContent.GARDEN.get()),"Obstructed placement wrote a partial terminal");
      h.assertTrue(level.getBlockEntity(root.offset(1,1,1))!=null,"Placement erased the obstructing chest block entity");
      level.removeBlock(root.offset(1,1,1),false);
      boolean[] seen={false};
      Consumer<BlockEvent.EntityPlaceEvent> cancel=event->{if(event.getEntity()==player){seen[0]=event instanceof BlockEvent.EntityMultiPlaceEvent;event.setCanceled(true);}};
      NeoForge.EVENT_BUS.addListener(cancel);
      try {h.assertTrue(!GardenWorldGameTests025.use(player,stack,root.below()).consumesAction(),"Canceled native multipiece placement consumed action");}
      finally {NeoForge.EVENT_BUS.unregister(cancel);}
      h.assertTrue(seen[0]&&stack.getCount()==2,"Terminal did not use atomic native multi-place capture or refund its item");
      for(var at:GardenTransporterBlock.footprint(root))h.assertTrue(level.getBlockState(at).isAir(),"Canceled multi-place left a part behind");
    } finally {level.removeBlock(root,false);player.server.getPlayerList().remove(player);}
    h.succeed();
  }
  @GameTest(template="activity_test",batch="device0254",timeoutTicks=80)
  public static void legacyExpansionRetainsCanonicalOwnershipAndDefersAroundPurchases(GameTestHelper h) {
    var player=TestConnections.makeSurvivalServerPlayer(h);var level=h.getLevel();var root=fixture(h);
    var data=GardenRegistry.get(player.server);var place=GardenRegistry.Place.at(level,root);
    try {
      player.teleportTo(root.getX()+.5,root.getY(),root.getZ()-2.5);
      var legacy=ActivityContent.GARDEN.get().defaultBlockState();h.assertTrue(GardenTransporterBlock.legacy(legacy),"Old states no longer decode as one-cell providers");
      level.setBlockAndUpdate(root,legacy);var garden=data.register(player,root);
      var chao=ChaoGarden.CHAO.get().create(level);chao.owner=player.getUUID();chao.setState(com.chaogarden.core.ChaoState.egg(3,false,true,0,new java.util.Random(254)));
      h.assertTrue(data.bind(player,chao,garden),"Legacy membership fixture could not enroll its owned Chao");
      var member=data.entries.get(chao.getUUID());
      var foreign=new GardenRegistry.Garden(place,java.util.UUID.randomUUID(),"Other keeper");data.gardens.put(place,foreign);
      GardenWorldGameTests025.use(player,ItemStack.EMPTY,root);
      h.assertTrue(level.getBlockState(root).equals(legacy)&&data.gardens.get(place)==foreign,"A foreign keeper could expand or claim another legacy provider");
      data.gardens.put(place,garden);
      level.setBlockAndUpdate(root.offset(1,1,1),Blocks.CHEST.defaultBlockState());
      GardenWorldGameTests025.use(player,ItemStack.EMPTY,root);
      h.assertTrue(level.getBlockState(root).equals(legacy)&&data.gardens.get(place)==garden&&level.getBlockEntity(root.offset(1,1,1))!=null,"Legacy expansion erased an obstruction or changed ownership");
      level.removeBlock(root.offset(1,1,1),false);GardenWorldGameTests025.use(player,ItemStack.EMPTY,root);
      h.assertTrue(!GardenTransporterBlock.legacy(level.getBlockState(root))&&data.gardens.get(place)==garden,"Safe legacy expansion changed canonical provider/owner identity");
      h.assertTrue(data.entries.get(chao.getUUID())==member&&member.garden.equals(place)&&member.owner.equals(player.getUUID()),"Legacy expansion rewrote Chao membership or ownership");
      for(int part=0;part<8;part++)h.assertTrue(level.getBlockState(root.offset(GardenTransporterBlock.offset(part))).getValue(GardenTransporterBlock.PART)==part,"Safe legacy expansion omitted a native part");
    } finally {level.removeBlock(root,false);data.removeGarden(place);player.server.getPlayerList().remove(player);}
    h.succeed();
  }
  private GardenDeviceGameTests0254() {}
}
