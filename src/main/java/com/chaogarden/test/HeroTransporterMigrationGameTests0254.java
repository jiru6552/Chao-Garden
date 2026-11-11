package com.chaogarden.test;

import com.chaogarden.ChaoGarden;
import com.chaogarden.activities.ActivityContent;
import com.chaogarden.activities.GardenRegistry;
import com.chaogarden.activities.GardenTransporterBlock;
import com.chaogarden.hub.HeroGardenRooms0253;
import com.chaogarden.hub.HubContent;
import java.util.LinkedHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** Genuine Hero scene upgrade: same ledger key, no lost purchase, atomic obstruction refusal. */
@GameTestHolder(ChaoGarden.MODID)
@PrefixGameTestTemplate(false)
public final class HeroTransporterMigrationGameTests0254 {
  private static final int FLAGS=Block.UPDATE_CLIENTS|Block.UPDATE_KNOWN_SHAPE;
  @GameTest(template="empty",batch="hero0254_transporter",timeoutTicks=180)
  public static void legacyHeroRootExpandsSouthWithoutChangingSavedLedgerOrPurchasedAssembly(GameTestHelper h){
    var level=h.getLevel().getServer().getLevel(HubContent.HERO_GARDEN);
    h.assertTrue(level!=null&&HubContent.ensure(level),"Hero scene unavailable");
    var keeper=TestConnections.makeSurvivalServerPlayer(h);keeper.teleportTo(level,12.5,80,8.5,0,0);keeper.hasChangedDimension();keeper.setNoGravity(true);
    var root=HeroGardenRooms0253.TRANSPORTER;var purchased=new BlockPos(8,80,-18);
    var data=GardenRegistry.get(level.getServer());var key=GardenRegistry.Place.at(level,root);var purchasedKey=GardenRegistry.Place.at(level,purchased);
    var original=new LinkedHashMap<BlockPos,BlockState>();
    GardenTransporterBlock.footprint(root).forEach(at->original.put(at,level.getBlockState(at)));
    GardenTransporterBlock.footprint(purchased).forEach(at->original.put(at,level.getBlockState(at)));
    var child=ChaoGarden.CHAO.get().create(level);child.owner=keeper.getUUID();child.moveTo(13.5,80,8.5,0,0);
    try{
      h.assertTrue(!data.gardens.containsKey(key)&&!data.gardens.containsKey(purchasedKey)
          &&!data.nodeOwners.containsKey(key)&&!data.nodeOwners.containsKey(purchasedKey)
          &&!data.links.containsKey(key)&&!data.links.containsKey(purchasedKey),"Fixture refuses to replace an existing keeper registration/link");
      for(var at:GardenTransporterBlock.footprint(purchased))h.assertTrue(level.getBlockState(at).isAir(),"Purchased assembly fixture needs clear native lawn");
      // Retire unregistered authored child slices first. The same legacy root
      // is then registered so no removal callback can erase the test payload.
      level.setBlock(root,ActivityContent.GARDEN.get().defaultBlockState(),FLAGS);
      for(int part=1;part<8;part++)level.setBlock(root.offset(GardenTransporterBlock.offset(part)),Blocks.AIR.defaultBlockState(),FLAGS);
      var scene=data.register(keeper,root);h.assertTrue(data.bind(keeper,child,scene),"Offline roster identity could not bind to its legacy provider");
      for(int part=0;part<8;part++)level.setBlock(purchased.offset(GardenTransporterBlock.offset(part)),GardenTransporterBlock.assembledState(Direction.EAST,part),FLAGS);
      data.register(keeper,purchased);
      data.nodeOwners.put(key,keeper.getUUID());data.links.put(purchasedKey,key);
      var recovery=new CompoundTag();recovery.putString("Fixture","Hero migration retains recovery payload");data.recoveries.put(child.getUUID(),recovery);
      var ledger=data.save(new CompoundTag(),level.registryAccess()).copy();
      var obstruction=root.offset(1,1,1);var probe=new BlockPos(14,80,-17);var probeBefore=level.getBlockState(probe);
      h.assertTrue(probeBefore.isAir(),"Atomic refusal probe needs clear authored air");
      original.put(probe,probeBefore);
      level.setBlock(obstruction,Blocks.CHEST.defaultBlockState(),FLAGS);var chest=level.getBlockEntity(obstruction);
      level.setBlock(probe,Blocks.DIAMOND_BLOCK.defaultBlockState(),FLAGS);
      boolean refused=false;
      try{HeroGardenRooms0253.ensure(level);}catch(IllegalStateException expected){refused=true;}
      h.assertTrue(refused&&GardenTransporterBlock.legacy(level.getBlockState(root))
          &&level.getBlockEntity(obstruction)==chest&&level.getBlockState(probe).is(Blocks.DIAMOND_BLOCK),"Blocked Hero expansion partially changed terrain, provider or existing block entity");
      h.assertTrue(data.save(new CompoundTag(),level.registryAccess()).equals(ledger),"Blocked Hero upgrade changed saved roster/provider payload");
      level.setBlock(obstruction,Blocks.AIR.defaultBlockState(),FLAGS);level.setBlock(probe,probeBefore,FLAGS);
      HeroGardenRooms0253.ensure(level);
      h.assertTrue(data.save(new CompoundTag(),level.registryAccess()).equals(ledger),"Hero upgrade changed root key, owner, roster, links or recovery payload");
      for(int part=0;part<8;part++){
        var at=root.offset(GardenTransporterBlock.offset(part));var state=level.getBlockState(at);
        h.assertTrue(state.equals(GardenTransporterBlock.assembledState(Direction.SOUTH,part))&&!HeroGardenRooms0253.breakable(at,state),"Authored Hero terminal part missing, backwards or recoverable");
        var owned=level.getBlockState(purchased.offset(GardenTransporterBlock.offset(part)));
        h.assertTrue(owned.equals(GardenTransporterBlock.assembledState(Direction.EAST,part)),"Hero scene replaced an independently purchased terminal part");
        if(part<4)h.assertTrue(level.getBlockState(at.below()).isFaceSturdy(level,at.below(),Direction.UP),"Hero terminal does not have its full two-by-two footing");
      }
      HeroGardenRooms0253.ensure(level);
      h.assertTrue(data.save(new CompoundTag(),level.registryAccess()).equals(ledger),"Repeated Hero scene preparation changed provider metadata");
    }finally{
      data.entries.remove(child.getUUID());data.recoveries.remove(child.getUUID());
      for(var at:java.util.List.of(key,purchasedKey)){
        var garden=data.gardens.get(at);
        if(garden!=null&&garden.owner().equals(keeper.getUUID())){data.removeGarden(at);data.links.remove(at);}
        if(keeper.getUUID().equals(data.nodeOwners.get(at)))data.nodeOwners.remove(at);
      }
      data.setDirty();original.forEach((at,state)->level.setBlock(at,state,FLAGS));
      child.discard();keeper.server.getPlayerList().remove(keeper);
    }
    h.succeed();
  }
}
