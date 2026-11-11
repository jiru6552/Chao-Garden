package com.chaogarden.test;

import com.chaogarden.ChaoGarden;
import com.chaogarden.activities.ActivityContent;
import com.chaogarden.activities.GardenRegistry;
import com.chaogarden.core.ChaoState;
import com.chaogarden.fruit.FruitContent;
import com.chaogarden.fruit.HangingFruitBlock;
import com.chaogarden.hub.GardenRooms025;
import com.chaogarden.hub.HubContent;
import com.chaogarden.hub.HubWorldData;
import com.chaogarden.hub.LobbyDecor025;
import com.chaogarden.toys.ChaoToyBlock;
import com.chaogarden.toys.ToyContent;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** The western expansion must preserve a real revision-28 keeper's garden. */
@GameTestHolder(ChaoGarden.MODID)
@PrefixGameTestTemplate(false)
public final class GardenLandscapeMigrationGameTests0254 {
  private static final int FLAGS=Block.UPDATE_CLIENTS|Block.UPDATE_KNOWN_SHAPE;
  private GardenLandscapeMigrationGameTests0254(){}
  private static void put(ServerLevel level,BlockPos at,BlockState state){level.setBlock(at,state,FLAGS);}
  private static void oldCell(ServerLevel level,Map<BlockPos,BlockState> saved,BlockPos at,BlockState state){saved.putIfAbsent(at,level.getBlockState(at));put(level,at,state);}
  private static ServerLevel garden(GameTestHelper h){var level=h.getLevel().getServer().getLevel(HubContent.GARDEN);h.assertTrue(level!=null&&HubContent.ensure(level),"Neutral garden unavailable");return level;}

  @GameTest(template="empty",batch="garden0254_owned_west",timeoutTicks=400)
  public static void revision28WestExtensionMovesActualOwnedPurchasesAndBodiesLosslessly(GameTestHelper h){
    var level=garden(h);var server=level.getServer();var data=GardenRegistry.get(server);
    var keeper=TestConnections.makeSurvivalServerPlayer(h);keeper.setNoGravity(true);keeper.setGameMode(GameType.ADVENTURE);
    var providerAt=new BlockPos(21,81,13);var tvAt=providerAt.west();var duckAt=new BlockPos(-5,79,-8);
    var provider=GardenRegistry.Place.at(level,providerAt);
    h.assertTrue(!data.gardens.containsKey(provider),"Revision-28 fixture refuses an existing keeper's provider");
    var tv=ToyContent.block("chao_tv").defaultBlockState().setValue(ChaoToyBlock.FACING,Direction.EAST).setValue(ChaoToyBlock.FRAME,2);
    var duck=ToyContent.block("chao_rubber_duck").defaultBlockState().setValue(ChaoToyBlock.WATERLOGGED,true).setValue(ChaoToyBlock.FACING,Direction.WEST).setValue(ChaoToyBlock.FRAME,3);
    var saved=new LinkedHashMap<BlockPos,BlockState>();
    // Exact revision-28 eastern lawn hummock and the old source-water pond.
    // New enclosing eastern rock buries these genuine supported purchases;
    // moving the pool west dries the previous waterlogged-toy position.
    for(var at:BlockPos.betweenClosed(19,80,11,22,84,14))oldCell(level,saved,at.immutable(),at.getY()==80?LobbyDecor025.TURF.get().defaultBlockState():Blocks.AIR.defaultBlockState());
    oldCell(level,saved,providerAt,ActivityContent.GARDEN.get().defaultBlockState());oldCell(level,saved,tvAt,tv);
    oldCell(level,saved,duckAt.below(2),LobbyDecor025.SAND.get().defaultBlockState());oldCell(level,saved,duckAt.below(),Blocks.WATER.defaultBlockState());oldCell(level,saved,duckAt,duck);oldCell(level,saved,duckAt.above(),Blocks.AIR.defaultBlockState());
    keeper.teleportTo(level,19.5,81,13.5,0,0);keeper.hasChangedDimension();var registration=data.register(keeper,providerAt);
    var bell=new ItemStack(TestConnections.legacyGardenBell());keeper.setItemInHand(InteractionHand.MAIN_HAND,bell);
    // Link at normal use distance, then place the keeper back on the saved eastern lawn.
    keeper.setPos(20.5,81,12.5);
    h.assertTrue(keeper.gameMode.useItemOn(keeper,level,bell,InteractionHand.MAIN_HAND,new BlockHitResult(Vec3.atCenterOf(providerAt),Direction.UP,providerAt,false)).consumesAction(),"Actual Adventure Garden Bell failed to link to revision-28 provider");
    var bellSaved=bell.save(keeper.registryAccess()).copy();keeper.setItemInHand(InteractionHand.MAIN_HAND,ItemStack.EMPTY);keeper.setPos(19.5,81,13.5);
    var child=ChaoGarden.CHAO.get().create(level);child.owner=keeper.getUUID();child.setState(ChaoState.egg(3,true,false,0,new Random(253)));child.setNoAi(true);child.setNoGravity(true);child.moveTo(22.5,81,11.5,0,0);
    var egg=ChaoGarden.CHAO_EGG.get().create(level);egg.owner=keeper.getUUID();egg.setState(ChaoState.egg(5,false,true,0,new Random(254)));egg.setNoGravity(true);egg.moveTo(19.5,81,11.5,0,0);
    var goods=new ItemStack(Items.DIAMOND,3);var item=new ItemEntity(level,21.5,81,14.5,goods.copy());item.setNoGravity(true);item.setPickUpDelay(32767);item.setDeltaMovement(Vec3.ZERO);
    var forced=new ArrayList<net.minecraft.world.level.ChunkPos>();
    for(int x=-2;x<=1;x++)for(int z=-1;z<=1;z++){var chunk=new net.minecraft.world.level.ChunkPos(x,z);if(!level.getForcedChunks().contains(chunk.toLong())){level.setChunkForced(x,z,true);forced.add(chunk);}}
    var originalRoomData=HubWorldData.get(server);var relocated=new ArrayList<BlockPos>();boolean[] closed={false};
    Runnable cleanup=()->{if(closed[0])return;closed[0]=true;child.discard();egg.discard();item.discard();
      data.entries.values().removeIf(entry->keeper.getUUID().equals(entry.owner));
      for(var at:new ArrayList<>(data.gardens.keySet()))if(keeper.getUUID().equals(data.gardens.get(at).owner())){data.removeGarden(at);if(!at.block().equals(GardenRooms025.TRANSPORTER))relocated.add(at.block());}
      for(var at:BlockPos.betweenClosed(-39,79,-22,24,94,18))if(level.getBlockState(at).equals(tv)||level.getBlockState(at).equals(duck))relocated.add(at.immutable());
      var authored=GardenRooms025.plan();for(var at:relocated)put(level,at,authored.blocks.getOrDefault(at,Blocks.AIR.defaultBlockState()));
      saved.forEach((at,state)->put(level,at,state));data.setDirty();server.overworld().getDataStorage().set("chao_hub_visits",originalRoomData);
      for(var chunk:forced)level.setChunkForced(chunk.x,chunk.z,false);keeper.server.getPlayerList().remove(keeper);
    };h.runAtTickTime(399,cleanup);
    h.assertTrue(level.addFreshEntity(child)&&level.addFreshEntity(egg)&&level.addFreshEntity(item),"Could not publish real revision-28 owned bodies");
    h.startSequence().thenWaitUntil(()->h.assertTrue(level.getEntity(child.getUUID())==child&&level.getEntity(egg.getUUID())==egg&&level.getEntity(item.getUUID())==item,"Revision-28 fixture awaits actual entity publication"))
      .thenExecute(()->{try{
        keeper.setPos(19.5,81,13.5);child.setPos(22.5,81,11.5);egg.setPos(19.5,81,11.5);item.setPos(21.5,81,14.5);
        h.assertTrue(level.noCollision(child)&&level.noCollision(item)&&level.getFluidState(item.blockPosition()).isEmpty()&&level.getFluidState(item.blockPosition().below()).isEmpty(),"Saved revision-28 eastern lawn actors were not initially dry and clear");
        h.assertTrue(data.bind(keeper,child,registration),"Real owned Chao could not bind before west extension");var entry=data.entries.get(child.getUUID());var childState=child.state().encode();var eggState=egg.state().encode();
        var childPosition=child.position();var itemPosition=item.position();
        var oldRooms=originalRoomData.save(new CompoundTag(),level.registryAccess());oldRooms.getCompound("Revisions").putInt(level.dimension().location().toString(),28);
        server.overworld().getDataStorage().set("chao_hub_visits",HubWorldData.load(oldRooms,level.registryAccess()));
        h.assertTrue(HubContent.ROOM_REVISION>28&&HubContent.ensure(level),"Actual saved revision-28 ensure did not commit the west-expansion upgrade");
        h.assertTrue(HubWorldData.get(server).save(new CompoundTag(),level.registryAccess()).getCompound("Revisions").getInt(level.dimension().location().toString())==HubContent.ROOM_REVISION,"West-expansion marker did not commit after successful physical migration");
        var destination=data.resolvePlace(provider);var offset=destination.block().subtract(providerAt);var movedTv=tvAt.offset(offset);relocated.add(movedTv);
        h.assertTrue(!destination.equals(provider)&&level.getBlockState(destination.block()).is(ActivityContent.GARDEN.get())&&level.getBlockState(destination.block().below()).is(LobbyDecor025.TURF.get()),"Eastern-lawn provider was buried, lost or left unsupported");
        h.assertTrue(level.getBlockState(movedTv).equals(tv)&&level.getBlockState(movedTv.below()).is(LobbyDecor025.TURF.get()),"Adjacent owned TV lost its relative assembly offset, orientation/frame or dry support");
        var moved=data.gardens.get(destination);
        h.assertTrue(moved.owner().equals(registration.owner())&&moved.name().equals(registration.name())&&data.entries.get(child.getUUID())==entry&&entry.garden.equals(destination),"West extension changed provider identity, owner or actual membership entry");
        h.assertTrue(level.getEntity(child.getUUID())==child&&keeper.getUUID().equals(child.owner)&&childState.equals(child.state().encode())&&level.getEntity(egg.getUUID())==egg&&keeper.getUUID().equals(egg.owner)&&eggState.equals(egg.state().encode())&&level.getEntity(item.getUUID())==item&&ItemStack.isSameItemSameComponents(item.getItem(),goods)&&item.getItem().getCount()==3,"West extension changed an actor UUID, Chao state/owner or item payload");
        h.assertTrue(!child.position().equals(childPosition)&&!item.position().equals(itemPosition),"New enclosing cliff body was not relocated");
        for(Entity actor:List.of(keeper,child,egg,item))h.assertTrue(level.noCollision(actor)&&level.getFluidState(actor.blockPosition()).isEmpty()&&level.getBlockState(actor.blockPosition().below()).is(LobbyDecor025.TURF.get()),"West extension did not give an actor a clear dry supported landing: "+actor.position());
        h.assertTrue(!keeper.getBoundingBox().intersects(child.getBoundingBox())&&!child.getBoundingBox().intersects(item.getBoundingBox())&&!egg.getBoundingBox().intersects(child.getBoundingBox())&&!egg.getBoundingBox().intersects(item.getBoundingBox())&&!egg.getBoundingBox().intersects(keeper.getBoundingBox()),"West extension stacked actors on a single landing");
        var ducks=new ArrayList<BlockPos>();int tvs=0,pods=0;
        for(var at:BlockPos.betweenClosed(-39,79,-22,24,94,18)){
          var state=level.getBlockState(at);if(state.equals(duck))ducks.add(at.immutable());if(state.equals(tv))tvs++;if(state.is(FruitContent.podForFruit("garden_fruit")))pods++;
        }
        h.assertTrue(ducks.size()==1&&tvs==1,"Owned water toy/TV was lost or duplicated: "+ducks.size()+" / "+tvs);var movedDuck=ducks.getFirst();relocated.add(movedDuck);
        h.assertTrue(pods==32,"Native palm fruit count changed while preserving real purchases: "+pods+" pods instead of eight palms'32");
        h.assertTrue(!movedDuck.equals(duckAt)&&GardenRooms025.pondCell(movedDuck.getX(),movedDuck.getZ())&&movedDuck.getY()==79&&level.getFluidState(movedDuck).isSource(),"Waterlogged duck lost exact state or did not reach actual shifted pond source water");
        var offline=ItemStack.parse(keeper.registryAccess(),bellSaved).orElseThrow();keeper.setItemInHand(InteractionHand.MAIN_HAND,offline);
        h.assertTrue(keeper.interactOn(child,InteractionHand.MAIN_HAND).consumesAction()&&data.entries.get(child.getUUID()).garden.equals(destination),"Offline Garden Bell no longer resolves the saved revision-28 provider alias");keeper.setItemInHand(InteractionHand.MAIN_HAND,ItemStack.EMPTY);
        var ledger=data.save(new CompoundTag(),level.registryAccess());GardenRooms025.ensure(level);
        h.assertTrue(ledger.equals(data.save(new CompoundTag(),level.registryAccess()))&&level.getBlockState(movedTv).equals(tv)&&level.getBlockState(movedDuck).equals(duck),"Repeated ensure changed aliases, membership or exact purchased block states");
        com.mojang.logging.LogUtils.getLogger().info("CHAO_LANDSCAPE0254_MIGRATION fromRevision=28 toRevision={} provider={} tv={} duck={} cliffBody={} formerLawnBody={} uuidAndPayloadPreserved=true",HubContent.ROOM_REVISION,destination.block(),movedTv,movedDuck,child.position(),item.position());
        cleanup.run();h.succeed();
      }catch(RuntimeException failure){cleanup.run();throw failure;}});
  }
}
