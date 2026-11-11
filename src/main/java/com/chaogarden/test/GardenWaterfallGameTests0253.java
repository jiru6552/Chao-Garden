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

/** The western water turn must preserve a real revision-27 keeper's garden. */
@GameTestHolder(ChaoGarden.MODID)
@PrefixGameTestTemplate(false)
public final class GardenWaterfallGameTests0253 {
  private static final int FLAGS=Block.UPDATE_CLIENTS|Block.UPDATE_KNOWN_SHAPE;
  private GardenWaterfallGameTests0253(){}
  private static void put(ServerLevel level,BlockPos at,BlockState state){level.setBlock(at,state,FLAGS);}
  private static void oldCell(ServerLevel level,Map<BlockPos,BlockState> saved,BlockPos at,BlockState state){saved.putIfAbsent(at,level.getBlockState(at));put(level,at,state);}
  private static ServerLevel garden(GameTestHelper h){var level=h.getLevel().getServer().getLevel(HubContent.GARDEN);h.assertTrue(level!=null&&HubContent.ensure(level),"Neutral garden unavailable");return level;}

  @GameTest(template="empty",batch="garden0253_water_turn",timeoutTicks=100)
  public static void westCascadeStaysInsideCompactSafetyWallAndFacesAcrossNorthCave(GameTestHelper h){
    var level=garden(h);var plan=GardenRooms025.plan();int pond=0;
    for(var entry:plan.blocks.entrySet())if(!entry.getValue().getFluidState().isEmpty()){
      var at=entry.getKey();
      h.assertTrue(GardenRooms025.insideField(at.getX(),at.getZ()),"Quarter-turn placed water inside/outside the safety wall: "+at);
      h.assertTrue(entry.getValue().getFluidState().isSource(),"Authored cascade/pond source is flowing or partial: "+at);
      if(at.getY()==79&&GardenRooms025.pondCell(at.getX(),at.getZ()))pond++;
    }
    h.assertTrue(pond==179,"Quarter-turn changed the compact receiving pool's coverage: "+pond);
    var cave=level.getBlockState(GardenRooms025.EXIT_PORTAL);
    h.assertTrue(cave.getValue(LobbyDecor025.SceneAnchor.FACING)==Direction.SOUTH&&GardenRooms025.WATERFALL_FLOW==Direction.EAST,
        "Western cascade and unchanged north-wall cave do not face perpendicular directions");
    h.assertTrue(level.getFluidState(new BlockPos(-36,91,-6)).isSource()&&level.getBlockState(new BlockPos(-15,91,-18)).getFluidState().isEmpty(),
        "Water wall stayed beside the north-wall exit instead of actually turning west");
    h.assertTrue(level.getBlockState(new BlockPos(5,79,-12)).is(LobbyDecor025.TURF.get())&&level.getFluidState(new BlockPos(5,80,-12)).isEmpty(),
        "Rotated pool submerged or removed the existing garden arrival walk");
    com.mojang.logging.LogUtils.getLogger().info("CHAO_WATER0253_GEOMETRY flow=EAST frontsX=-26,-29,-32,-35 pondCenter=-20,-8 pondColumns={} caveFacing=SOUTH mask=2298 box=64x41",pond);
    h.succeed();
  }

  @GameTest(template="empty",batch="garden0253_owned_turn",timeoutTicks=400)
  public static void revision27TurnMovesWestPurchasesWetToyAndNewPoolBodiesLosslessly(GameTestHelper h){
    var level=garden(h);var server=level.getServer();var data=GardenRegistry.get(server);
    var keeper=TestConnections.makeSurvivalServerPlayer(h);keeper.setNoGravity(true);keeper.setGameMode(GameType.ADVENTURE);
    var providerAt=new BlockPos(-21,80,-5);var tvAt=providerAt.east();var duckAt=new BlockPos(-18,79,3);var oldPalm=new BlockPos(-1,80,-8);
    var provider=GardenRegistry.Place.at(level,providerAt);
    h.assertTrue(!data.gardens.containsKey(provider),"Revision-27 fixture refuses an existing keeper's provider");
    var tv=ToyContent.block("chao_tv").defaultBlockState().setValue(ChaoToyBlock.FACING,Direction.EAST).setValue(ChaoToyBlock.FRAME,2);
    var duck=ToyContent.block("chao_rubber_duck").defaultBlockState().setValue(ChaoToyBlock.WATERLOGGED,true).setValue(ChaoToyBlock.FACING,Direction.WEST).setValue(ChaoToyBlock.FRAME,3);
    var saved=new LinkedHashMap<BlockPos,BlockState>();
    // Real 0.25.2 terrain here was dry western lawn, with the pond farther south.
    // Restore the exact source support/body cells before exercising the upgrade.
    for(var at:BlockPos.betweenClosed(-23,79,-7,-18,83,-1))oldCell(level,saved,at.immutable(),at.getY()==79?LobbyDecor025.TURF.get().defaultBlockState():Blocks.AIR.defaultBlockState());
    var newPoolBody=new BlockPos(-18,80,-11);
    oldCell(level,saved,newPoolBody.below(),LobbyDecor025.TURF.get().defaultBlockState());
    oldCell(level,saved,newPoolBody,Blocks.AIR.defaultBlockState());oldCell(level,saved,newPoolBody.above(),Blocks.AIR.defaultBlockState());
    oldCell(level,saved,providerAt,ActivityContent.GARDEN.get().defaultBlockState());oldCell(level,saved,tvAt,tv);
    oldCell(level,saved,duckAt.below(),LobbyDecor025.SAND.get().defaultBlockState());oldCell(level,saved,duckAt,duck);oldCell(level,saved,duckAt.above(),Blocks.AIR.defaultBlockState());
    // A genuinely authored native palm, including the four live ripe pods, was
    // on the old dry pond shore. Its old scenery must not become owned fruit.
    oldCell(level,saved,oldPalm.below(),LobbyDecor025.TURF.get().defaultBlockState());
    for(int y=0;y<4;y++)oldCell(level,saved,oldPalm.above(y),LobbyDecor025.PALM_TRUNK.get().defaultBlockState());
    oldCell(level,saved,oldPalm.above(4),LobbyDecor025.PALM_CROWN.get().defaultBlockState());
    for(var direction:Direction.Plane.HORIZONTAL){
      oldCell(level,saved,oldPalm.above(4).relative(direction),LobbyDecor025.PALM_SUPPORT.get().defaultBlockState());
      oldCell(level,saved,oldPalm.above(3).relative(direction),FruitContent.podForFruit("garden_fruit").defaultBlockState().setValue(HangingFruitBlock.FACING,direction.getOpposite()).setValue(HangingFruitBlock.AGE,3));
    }
    keeper.teleportTo(level,-22.5,80,-2.5,0,0);keeper.hasChangedDimension();var registration=data.register(keeper,providerAt);
    var bell=new ItemStack(TestConnections.legacyGardenBell());keeper.setItemInHand(InteractionHand.MAIN_HAND,bell);
    // Link at normal use distance, then place the keeper back on old western lawn.
    keeper.setPos(-21.5,80,-3.5);
    h.assertTrue(keeper.gameMode.useItemOn(keeper,level,bell,InteractionHand.MAIN_HAND,new BlockHitResult(Vec3.atCenterOf(providerAt),Direction.UP,providerAt,false)).consumesAction(),"Actual Adventure Garden Bell failed to link to revision-27 provider");
    var bellSaved=bell.save(keeper.registryAccess()).copy();keeper.setItemInHand(InteractionHand.MAIN_HAND,ItemStack.EMPTY);keeper.setPos(-22.5,80,-2.5);
    var child=ChaoGarden.CHAO.get().create(level);child.owner=keeper.getUUID();child.setState(ChaoState.egg(3,true,false,0,new Random(253)));child.setNoAi(true);child.setNoGravity(true);child.moveTo(-19.5,80,-2.5,0,0);
    var goods=new ItemStack(Items.DIAMOND,3);var item=new ItemEntity(level,-17.5,80,-10.5,goods.copy());item.setNoGravity(true);item.setPickUpDelay(32767);item.setDeltaMovement(Vec3.ZERO);
    var forced=new ArrayList<net.minecraft.world.level.ChunkPos>();
    for(int x=-2;x<=1;x++)for(int z=-1;z<=1;z++){var chunk=new net.minecraft.world.level.ChunkPos(x,z);if(!level.getForcedChunks().contains(chunk.toLong())){level.setChunkForced(x,z,true);forced.add(chunk);}}
    var originalRoomData=HubWorldData.get(server);var relocated=new ArrayList<BlockPos>();boolean[] closed={false};
    Runnable cleanup=()->{if(closed[0])return;closed[0]=true;child.discard();item.discard();
      data.entries.values().removeIf(entry->keeper.getUUID().equals(entry.owner));
      for(var at:new ArrayList<>(data.gardens.keySet()))if(keeper.getUUID().equals(data.gardens.get(at).owner())){data.removeGarden(at);if(!at.block().equals(GardenRooms025.TRANSPORTER))relocated.add(at.block());}
      for(var at:BlockPos.betweenClosed(-39,79,-22,24,94,18))if(level.getBlockState(at).equals(tv)||level.getBlockState(at).equals(duck))relocated.add(at.immutable());
      var authored=GardenRooms025.plan();for(var at:relocated)put(level,at,authored.blocks.getOrDefault(at,Blocks.AIR.defaultBlockState()));
      saved.forEach((at,state)->put(level,at,state));data.setDirty();server.overworld().getDataStorage().set("chao_hub_visits",originalRoomData);
      for(var chunk:forced)level.setChunkForced(chunk.x,chunk.z,false);keeper.server.getPlayerList().remove(keeper);
    };h.runAtTickTime(399,cleanup);
    h.assertTrue(level.addFreshEntity(child)&&level.addFreshEntity(item),"Could not publish real revision-27 owned bodies");
    h.startSequence().thenWaitUntil(()->h.assertTrue(level.getEntity(child.getUUID())==child&&level.getEntity(item.getUUID())==item,"Revision-27 fixture awaits actual entity publication"))
      .thenExecute(()->{try{
        keeper.setPos(-22.5,80,-2.5);child.setPos(-19.5,80,-2.5);item.setPos(-17.5,80,-10.5);
        h.assertTrue(level.noCollision(child)&&level.noCollision(item)&&level.getFluidState(item.blockPosition()).isEmpty()&&level.getFluidState(item.blockPosition().below()).isEmpty(),"Old western lawn/new pond actors were not initially dry and clear");
        h.assertTrue(data.bind(keeper,child,registration),"Real owned Chao could not bind before water turn");var entry=data.entries.get(child.getUUID());var childState=child.state().encode();
        var childPosition=child.position();var itemPosition=item.position();
        var oldRooms=originalRoomData.save(new CompoundTag(),level.registryAccess());oldRooms.getCompound("Revisions").putInt(level.dimension().location().toString(),27);
        server.overworld().getDataStorage().set("chao_hub_visits",HubWorldData.load(oldRooms,level.registryAccess()));
        h.assertTrue(HubContent.ROOM_REVISION>27&&HubContent.ensure(level),"Actual saved revision-27 ensure did not commit the quarter-turn upgrade");
        h.assertTrue(HubWorldData.get(server).save(new CompoundTag(),level.registryAccess()).getCompound("Revisions").getInt(level.dimension().location().toString())==HubContent.ROOM_REVISION,"Water-turn marker did not commit after successful physical migration");
        var destination=data.resolvePlace(provider);var offset=destination.block().subtract(providerAt);var movedTv=tvAt.offset(offset);relocated.add(movedTv);
        h.assertTrue(!destination.equals(provider)&&level.getBlockState(destination.block()).is(ActivityContent.GARDEN.get())&&level.getBlockState(destination.block().below()).is(LobbyDecor025.TURF.get()),"West-wall provider was buried, lost or left unsupported");
        h.assertTrue(level.getBlockState(movedTv).equals(tv)&&level.getBlockState(movedTv.below()).is(LobbyDecor025.TURF.get()),"Adjacent owned TV lost its relative assembly offset, orientation/frame or dry support");
        var moved=data.gardens.get(destination);
        h.assertTrue(moved.owner().equals(registration.owner())&&moved.name().equals(registration.name())&&data.entries.get(child.getUUID())==entry&&entry.garden.equals(destination),"Water turn changed provider identity, owner or actual membership entry");
        h.assertTrue(level.getEntity(child.getUUID())==child&&keeper.getUUID().equals(child.owner)&&childState.equals(child.state().encode())&&level.getEntity(item.getUUID())==item&&ItemStack.isSameItemSameComponents(item.getItem(),goods)&&item.getItem().getCount()==3,"Water turn changed an actor UUID, Chao state/owner or item payload");
        h.assertTrue(!child.position().equals(childPosition)&&!item.position().equals(itemPosition),"New cliff body or standing body above newly authored pool was not relocated");
        for(Entity actor:List.of(keeper,child,item))h.assertTrue(level.noCollision(actor)&&level.getFluidState(actor.blockPosition()).isEmpty()&&level.getBlockState(actor.blockPosition().below()).is(LobbyDecor025.TURF.get()),"Water turn did not give an actor a clear dry supported landing: "+actor.position());
        h.assertTrue(!keeper.getBoundingBox().intersects(child.getBoundingBox())&&!child.getBoundingBox().intersects(item.getBoundingBox()),"Water turn stacked actors on a single landing");
        var ducks=new ArrayList<BlockPos>();int tvs=0,pods=0;
        for(var at:BlockPos.betweenClosed(-39,79,-22,24,94,18)){
          var state=level.getBlockState(at);if(state.equals(duck))ducks.add(at.immutable());if(state.equals(tv))tvs++;if(state.is(FruitContent.podForFruit("garden_fruit")))pods++;
        }
        h.assertTrue(ducks.size()==1&&tvs==1,"Owned water toy/TV was lost or duplicated: "+ducks.size()+" / "+tvs);var movedDuck=ducks.getFirst();relocated.add(movedDuck);
        h.assertTrue(pods==32,"Old native palm fruit was incorrectly preserved elsewhere as a purchase: "+pods+" pods instead of eight palms'32");
        h.assertTrue(!movedDuck.equals(duckAt)&&GardenRooms025.pondCell(movedDuck.getX(),movedDuck.getZ())&&movedDuck.getY()==79&&level.getFluidState(movedDuck).isSource(),"Waterlogged duck lost exact state or did not reach actual rotated pond source water");
        for(var direction:Direction.Plane.HORIZONTAL)h.assertTrue(!level.getBlockState(oldPalm.above(3).relative(direction)).is(FruitContent.podForFruit("garden_fruit")),"Old authored native palm pod was incorrectly migrated as a purchase");
        var offline=ItemStack.parse(keeper.registryAccess(),bellSaved).orElseThrow();keeper.setItemInHand(InteractionHand.MAIN_HAND,offline);
        h.assertTrue(keeper.interactOn(child,InteractionHand.MAIN_HAND).consumesAction()&&data.entries.get(child.getUUID()).garden.equals(destination),"Offline Garden Bell no longer resolves the saved revision-27 provider alias");keeper.setItemInHand(InteractionHand.MAIN_HAND,ItemStack.EMPTY);
        var ledger=data.save(new CompoundTag(),level.registryAccess());GardenRooms025.ensure(level);
        h.assertTrue(ledger.equals(data.save(new CompoundTag(),level.registryAccess()))&&level.getBlockState(movedTv).equals(tv)&&level.getBlockState(movedDuck).equals(duck),"Repeated ensure changed aliases, membership or exact purchased block states");
        com.mojang.logging.LogUtils.getLogger().info("CHAO_WATER0253_MIGRATION fromRevision=27 toRevision={} provider={} tv={} duck={} cliffBody={} formerLawnBody={} uuidAndPayloadPreserved=true",HubContent.ROOM_REVISION,destination.block(),movedTv,movedDuck,child.position(),item.position());
        cleanup.run();h.succeed();
      }catch(RuntimeException failure){cleanup.run();throw failure;}});
  }
}
