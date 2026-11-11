package com.chaogarden.test;

import com.chaogarden.ChaoGarden;
import com.chaogarden.activities.ActivityBlock;
import com.chaogarden.activities.ActivityContent;
import com.chaogarden.activities.GardenRegistry;
import com.chaogarden.core.ChaoState;
import com.chaogarden.hub.GardenRooms025;
import com.chaogarden.hub.HubContent;
import com.chaogarden.hub.LobbyDecor025;
import com.chaogarden.toys.ChaoToyBlock;
import com.chaogarden.toys.ToyContent;
import java.util.ArrayList;
import java.util.HashSet;
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

/** Shrinking a saved garden must preserve actual purchases, actors and ownership. */
@GameTestHolder(ChaoGarden.MODID)
@PrefixGameTestTemplate(false)
public final class GardenCompactGameTests0252 {
  private static final int FLAGS=Block.UPDATE_CLIENTS|Block.UPDATE_KNOWN_SHAPE;
  private GardenCompactGameTests0252(){}
  private static ServerLevel garden(GameTestHelper h){var level=h.getLevel().getServer().getLevel(HubContent.GARDEN);h.assertTrue(level!=null&&HubContent.ensure(level),"Compact garden unavailable");return level;}
  private static void put(ServerLevel l,BlockPos p,BlockState s){l.setBlock(p,s,FLAGS);}
  private static void removeKeeperClaims(GardenRegistry data,java.util.UUID owner){for(var at:new ArrayList<>(data.gardens.keySet()))if(owner.equals(data.gardens.get(at).owner()))data.removeGarden(at);}
  @GameTest(template="empty",batch="garden0252_scale",timeoutTicks=100)
  public static void compactMaskAndLandmarksUsePlayerScaleWithoutOldOuterLawn(GameTestHelper h){
    var l=garden(h);var plan=GardenRooms025.plan();int mask=0,emittedMask=0,lawn=0;
    for(int x=-53;x<=53;x++)for(int z=-51;z<=47;z++){
      if(GardenRooms025.insideField(x,z))mask++;
      if(plan.blocks.getOrDefault(new BlockPos(x,68,z),Blocks.AIR.defaultBlockState()).is(LobbyDecor025.CLIFF.get()))emittedMask++;
      if(plan.blocks.getOrDefault(new BlockPos(x,79,z),Blocks.AIR.defaultBlockState()).is(LobbyDecor025.TURF.get()))lawn++;
    }
    h.assertTrue(mask==2298&&emittedMask==mask&&mask<8279*.28,"Compact footprint lost its exact west-extended compact footprint or at least72% reduction of the old8279-column field: "+mask);
    h.assertTrue(lawn>500,"Compact garden lost its continuous usable central lawn: "+lawn);
    h.assertTrue(GardenRooms025.EXIT_PORTAL.equals(new BlockPos(5,80,-18))&&GardenRooms025.TRANSPORTER.equals(new BlockPos(17,81,9)),"Compact landmark anchors changed without reference study");
    h.assertTrue(l.getBlockState(new BlockPos(34,79,15)).isAir()&&l.getBlockState(new BlockPos(-45,79,15)).isAir()&&l.getBlockState(new BlockPos(0,79,40)).isAir(),"Old giant outer lawn was left behind the compact barriers");
    h.assertTrue(l.getBlockState(new BlockPos(-33,79,15)).is(Blocks.BARRIER),"Western extension left old lawn instead of the new invisible safety ring");
    for(int z=-12;z<=-6;z++){
      h.assertTrue(l.getBlockState(new BlockPos(5,79,z)).is(LobbyDecor025.TURF.get()),"Arrival walk lacks actual level turf support");
      for(int y=80;y<=82;y++)h.assertTrue(l.getBlockState(new BlockPos(5,y,z)).getCollisionShape(l,new BlockPos(5,y,z)).isEmpty(),"Player-height arrival walk is blocked");
    }
    var visitor=TestConnections.makeSurvivalServerPlayer(h);visitor.setNoGravity(true);
    try{
      visitor.teleportTo(l,5.5,80,-12.5,0,0);visitor.hasChangedDimension();double originZ=visitor.getZ();
      for(int step=1;step<=7;step++){
        visitor.move(net.minecraft.world.entity.MoverType.SELF,new Vec3(0,0,1));
        h.assertTrue(Math.abs(visitor.getZ()-(originZ+step))<.000001&&Math.abs(visitor.getY()-80)<.000001
            &&l.noCollision(visitor)&&l.getBlockState(visitor.blockPosition().below()).is(LobbyDecor025.TURF.get()),
            "Real arrival walk failed at metre "+step+" position="+visitor.position()+" body="+visitor.getBoundingBox());
      }
      com.mojang.logging.LogUtils.getLogger().info("CHAO_WATER0253_ARRIVAL_WALK turfCells=7 levelY=79 distance={} actualMoverType=SELF collisionClear=true",visitor.getZ()-originZ);
    }finally{if(HubContent.hasReturnJournal(visitor))HubContent.returnHome(visitor);visitor.server.getPlayerList().remove(visitor);}
    for(var palm:GardenRooms025.PALMS)h.assertTrue(palm[2]>=4&&palm[2]<=5,"Unscaled tall palm remains in compact garden");
    h.assertTrue(l.getBlockState(GardenRooms025.TRANSPORTER).is(ActivityContent.GARDEN.get())&&l.getBlockState(GardenRooms025.TRANSPORTER.below()).is(LobbyDecor025.TURF.get()),"Departure machine has no real raised lawn support");
    com.mojang.logging.LogUtils.getLogger().info("CHAO_COMPACT0252_GEOMETRY oldMask=8279 newMask={} oldVoxelBox=101x91 newVoxelBox=64x41 lawnY79={} pondColumns=179",mask,lawn);h.succeed();
  }

  @GameTest(template="empty",batch="garden0252_owned_shrink",timeoutTicks=400)
  public static void shrinkRelocates0251ProviderPurchasesCourseAndBodiesWithoutChangingIdentity(GameTestHelper h){
    var l=garden(h);var data=GardenRegistry.get(l.getServer());var keeper=TestConnections.makeSurvivalServerPlayer(h);keeper.setNoGravity(true);keeper.setGameMode(GameType.ADVENTURE);
    var oldProvider=new BlockPos(34,82,15);var oldToy=new BlockPos(31,83,12);var oldStart=new BlockPos(27,80,6);var oldFinish=new BlockPos(35,80,6);
    var provider=GardenRegistry.Place.at(l,oldProvider);var start=GardenRegistry.Place.at(l,oldStart);var finish=GardenRegistry.Place.at(l,oldFinish);
    h.assertTrue(!data.gardens.containsKey(provider)&&!data.nodeOwners.containsKey(start)&&!data.nodeOwners.containsKey(finish),"Fixture refuses existing keeper claims");
    var toy=ToyContent.block("chao_tv").defaultBlockState().setValue(ChaoToyBlock.FACING,Direction.EAST).setValue(ChaoToyBlock.FRAME,2);
    var startState=ActivityContent.START.get().defaultBlockState().setValue(ActivityBlock.AXIS,Direction.Axis.Z);var finishState=ActivityContent.FINISH.get().defaultBlockState().setValue(ActivityBlock.AXIS,Direction.Axis.Z);
    var before=new LinkedHashMap<BlockPos,BlockState>();for(var at:List.of(oldProvider,oldToy,oldStart,oldStart.south(),oldFinish,oldFinish.south()))before.put(at,l.getBlockState(at));
    put(l,oldProvider,ActivityContent.GARDEN.get().defaultBlockState());put(l,oldToy,toy);put(l,oldStart,startState);put(l,oldStart.south(),startState);put(l,oldFinish,finishState);put(l,oldFinish.south(),finishState);
    h.assertTrue(com.chaogarden.activities.ActivityManager.line(l,oldStart,ActivityBlock.Kind.START).size()==2&&com.chaogarden.activities.ActivityManager.line(l,oldFinish,ActivityBlock.Kind.FINISH).size()==2,"Source course must have genuine complete two-part start/finish lines");
    keeper.teleportTo(l,34.5,82,15.5,0,0);keeper.hasChangedDimension();var registration=data.register(keeper,oldProvider);
    data.nodeOwners.put(start,keeper.getUUID());data.nodeOwners.put(finish,keeper.getUUID());data.links.put(start,finish);data.setDirty();
    var bell=new ItemStack(TestConnections.legacyGardenBell());keeper.setItemInHand(InteractionHand.MAIN_HAND,bell);
    h.assertTrue(keeper.gameMode.useItemOn(keeper,l,bell,InteractionHand.MAIN_HAND,new BlockHitResult(Vec3.atCenterOf(oldProvider),Direction.UP,oldProvider,false)).consumesAction(),"Genuine Adventure Garden Bell could not link before shrink");
    var bellSaved=bell.save(keeper.registryAccess()).copy();keeper.setItemInHand(InteractionHand.MAIN_HAND,ItemStack.EMPTY);
    var child=ChaoGarden.CHAO.get().create(l);child.owner=keeper.getUUID();child.setState(ChaoState.egg(3,true,false,0,new Random(252)));child.setNoAi(true);child.setNoGravity(true);child.moveTo(32.5,83,16.5,0,0);
    var goods=new ItemStack(Items.DIAMOND,3);var item=new ItemEntity(l,36.5,83,16.5,goods.copy());item.setNoGravity(true);item.setPickUpDelay(32767);
    var forced=new ArrayList<net.minecraft.world.level.ChunkPos>();for(var at:List.of(oldProvider,child.blockPosition(),item.blockPosition())){var chunk=new net.minecraft.world.level.ChunkPos(at);if(!l.getForcedChunks().contains(chunk.toLong())){l.setChunkForced(chunk.x,chunk.z,true);forced.add(chunk);}}
    h.assertTrue(l.addFreshEntity(child)&&l.addFreshEntity(item),"Could not publish actual owned shrink bodies");
    var relocatedCourse=new ArrayList<BlockPos>();
    boolean[] closed={false};Runnable cleanup=()->{if(closed[0])return;closed[0]=true;child.discard();item.discard();
      for(var at:relocatedCourse)put(l,at,Blocks.AIR.defaultBlockState());
      data.entries.values().removeIf(e->keeper.getUUID().equals(e.owner));
      for(var at:new ArrayList<>(data.gardens.keySet()))if(keeper.getUUID().equals(data.gardens.get(at).owner())){data.removeGarden(at);if(!at.block().equals(GardenRooms025.TRANSPORTER))put(l,at.block(),Blocks.AIR.defaultBlockState());}
      for(var at:new ArrayList<>(data.nodeOwners.keySet()))if(keeper.getUUID().equals(data.nodeOwners.get(at))){data.nodeOwners.remove(at);data.links.remove(at);put(l,at.block(),Blocks.AIR.defaultBlockState());}
      for(var at:BlockPos.betweenClosed(-39,80,-22,24,94,18))if(l.getBlockState(at).equals(toy))put(l,at,Blocks.AIR.defaultBlockState());
      before.forEach((at,state)->put(l,at,state));data.setDirty();for(var c:forced)l.setChunkForced(c.x,c.z,false);keeper.server.getPlayerList().remove(keeper);
    };h.runAtTickTime(399,cleanup);
    h.startSequence().thenWaitUntil(()->h.assertTrue(l.getEntity(child.getUUID())==child&&l.getEntity(item.getUUID())==item,"Awaiting actual shrink actor sections"))
      .thenExecute(()->{try{
        keeper.setPos(34.5,82,15.5);child.setPos(32.5,83,16.5);item.setPos(36.5,83,16.5);
        h.assertTrue(data.bind(keeper,child,registration),"Owned Chao could not bind to original provider");var entry=data.entries.get(child.getUUID());var childState=child.state().encode();var id=child.getUUID();
        GardenRooms025.ensure(l);var destination=data.resolvePlace(provider);
        h.assertTrue(!destination.equals(provider)&&GardenRooms025.insideField(destination.block().getX(),destination.block().getZ()),"Old0251 provider did not move into compact garden");
        h.assertTrue(l.getBlockState(destination.block()).is(ActivityContent.GARDEN.get())&&l.getBlockState(destination.block().below()).is(LobbyDecor025.TURF.get()),"Migrated provider is not physically supported");
        var moved=data.gardens.get(destination);h.assertTrue(moved.owner().equals(registration.owner())&&moved.name().equals(registration.name())&&data.entries.get(id)==entry&&entry.garden.equals(destination),"Shrink changed provider identity, member entry or keeper");
        h.assertTrue(l.getEntity(id)==child&&keeper.getUUID().equals(child.owner)&&childState.equals(child.state().encode())&&ItemStack.isSameItemSameComponents(item.getItem(),goods)&&item.getItem().getCount()==3,"Shrink changed creature UUID/state/owner or purchased goods");
        for(var actor:List.of(keeper,child,item))h.assertTrue(GardenRooms025.insideField(actor.blockPosition().getX(),actor.blockPosition().getZ())&&l.noCollision(actor)&&l.getFluidState(actor.blockPosition()).isEmpty()&&l.getBlockState(actor.blockPosition().below()).is(LobbyDecor025.TURF.get()),"Old outer-area actor lacks distinct supported dry landing: "+actor.position());
        h.assertTrue(!keeper.getBoundingBox().intersects(child.getBoundingBox())&&!child.getBoundingBox().intersects(item.getBoundingBox()),"Shrink stacked actors on one landing");
        var toyAt=new ArrayList<BlockPos>();for(var at:BlockPos.betweenClosed(-39,80,-22,24,94,18))if(l.getBlockState(at).equals(toy))toyAt.add(at.immutable());
        h.assertTrue(toyAt.size()==1&&l.getBlockState(toyAt.getFirst().below()).is(LobbyDecor025.TURF.get()),"Purchased oriented/frame-bearing toy was lost, copied or unsupported");
        var starts=data.nodeOwners.keySet().stream().filter(at->keeper.getUUID().equals(data.nodeOwners.get(at))&&l.getBlockState(at.block()).equals(startState)).toList();
        h.assertTrue(starts.size()==1&&data.links.containsKey(starts.getFirst()),"Completed course lost its owned start/link");var ending=data.links.get(starts.getFirst());
        h.assertTrue(keeper.getUUID().equals(data.nodeOwners.get(ending))&&l.getBlockState(ending.block()).equals(finishState)&&!data.nodeOwners.containsKey(start)&&!data.nodeOwners.containsKey(finish),"Course ownership/end state or old-node cleanup is wrong");
        h.assertTrue(ending.block().subtract(starts.getFirst().block()).equals(oldFinish.subtract(oldStart)),"Completed course edge geometry/length changed during shrink");
        relocatedCourse.addAll(com.chaogarden.activities.ActivityManager.line(l,starts.getFirst().block(),ActivityBlock.Kind.START));relocatedCourse.addAll(com.chaogarden.activities.ActivityManager.line(l,ending.block(),ActivityBlock.Kind.FINISH));
        h.assertTrue(relocatedCourse.size()==4,"Moved course lost complete straight starting/ending lines");
        var offline=ItemStack.parse(keeper.registryAccess(),bellSaved).orElseThrow();keeper.setItemInHand(InteractionHand.MAIN_HAND,offline);
        h.assertTrue(keeper.interactOn(child,InteractionHand.MAIN_HAND).consumesAction()&&data.entries.get(id).garden.equals(destination),"Offline saved Garden Bell lost its moved provider alias");keeper.setItemInHand(InteractionHand.MAIN_HAND,ItemStack.EMPTY);
        CompoundTag ledger=data.save(new CompoundTag(),keeper.registryAccess());GardenRooms025.ensure(l);
        h.assertTrue(ledger.equals(data.save(new CompoundTag(),keeper.registryAccess()))&&l.getBlockState(toyAt.getFirst()).equals(toy),"Second compact ensure changed saved graph, aliases, purchases or memberships");
        cleanup.run();h.succeed();
      }catch(RuntimeException failure){cleanup.run();throw failure;}});
  }

  @GameTest(template="empty",batch="garden0252_abort",timeoutTicks=120)
  public static void insufficientAssemblyCapacityAbortsBeforeTerrainOrRegistryChanges(GameTestHelper h){
    var l=garden(h);var keeper=TestConnections.makeSurvivalServerPlayer(h);var data=GardenRegistry.get(l.getServer());var before=new LinkedHashMap<BlockPos,BlockState>();
    var providerAt=new BlockPos(34,82,15);var provider=GardenRegistry.Place.at(l,providerAt);
    h.assertTrue(!data.gardens.containsKey(provider),"Abort fixture refuses an existing claim");
    for(int x=-40;x<=40;x++){var at=new BlockPos(x,118,-40);before.put(at,l.getBlockState(at));put(l,at,ActivityContent.FLAG.get().defaultBlockState());}
    before.put(providerAt,l.getBlockState(providerAt));put(l,providerAt,ActivityContent.GARDEN.get().defaultBlockState());keeper.teleportTo(l,5.5,80.1,-12.5,0,0);keeper.hasChangedDimension();data.register(keeper,providerAt);
    h.assertTrue(data.gardens.containsKey(provider)&&data.gardens.get(provider).owner().equals(keeper.getUUID()),"Fixture must register the real Garden-dimension legacy provider");
    var ledger=data.save(new CompoundTag(),keeper.registryAccess());var canonical=l.getBlockState(GardenRooms025.TRANSPORTER);var lawn=l.getBlockState(new BlockPos(0,79,0));
    try{
      boolean failed=false;try{GardenRooms025.ensure(l);}catch(IllegalStateException expected){failed=expected.getMessage().contains("No lossless compact garden location");}
      h.assertTrue(failed,"Oversized81-cell connected assembly was silently deleted or squeezed into compact field");
      h.assertTrue(ledger.equals(data.save(new CompoundTag(),keeper.registryAccess()))&&l.getBlockState(providerAt).is(ActivityContent.GARDEN.get())&&l.getBlockState(GardenRooms025.TRANSPORTER).equals(canonical)&&l.getBlockState(new BlockPos(0,79,0)).equals(lawn),"Failed compact preflight changed terrain/provider/registration");
      for(int x=-40;x<=40;x++)h.assertTrue(l.getBlockState(new BlockPos(x,118,-40)).is(ActivityContent.FLAG.get()),"Failed preflight lost part of the saved assembly");
    }finally{removeKeeperClaims(data,keeper.getUUID());before.forEach((at,state)->put(l,at,state));keeper.server.getPlayerList().remove(keeper);}
    h.succeed();
  }
  @GameTest(template="empty",batch="garden0252_be_abort",timeoutTicks=100)
  public static void unknownInventoryAbortsBeforeAnySceneryOrProviderWrite(GameTestHelper h){
    var l=garden(h);var at=new BlockPos(32,80,4);var before=l.getBlockState(at);var data=GardenRegistry.get(l.getServer());var ledger=data.save(new CompoundTag(),l.registryAccess());
    put(l,at,Blocks.CHEST.defaultBlockState());var chest=(net.minecraft.world.level.block.entity.ChestBlockEntity)l.getBlockEntity(at);
    chest.setItem(7,new ItemStack(Items.DIAMOND,13));var payload=chest.saveWithFullMetadata(l.registryAccess());var turf=l.getBlockState(new BlockPos(0,79,0));
    try{
      boolean failed=false;try{GardenRooms025.ensure(l);}catch(IllegalStateException expected){failed=expected.getMessage().contains("Unknown block entity");}
      h.assertTrue(failed&&l.getBlockEntity(at)==chest&&payload.equals(chest.saveWithFullMetadata(l.registryAccess()))&&chest.getItem(7).getCount()==13,"Unknown inventory was cleared or mutated by a failed shrink");
      h.assertTrue(ledger.equals(data.save(new CompoundTag(),l.registryAccess()))&&l.getBlockState(new BlockPos(0,79,0)).equals(turf),"Unknown inventory refusal changed garden metadata/terrain");
    }finally{chest.clearContent();put(l,at,before);}h.succeed();
  }

  @GameTest(template="empty",batch="garden0252_water_tree",timeoutTicks=140)
  public static void waterToyAndCompleteFruitTreeKeepStatesWithoutDropsDuringMove(GameTestHelper h){
    var l=garden(h);var saved=new LinkedHashMap<BlockPos,BlockState>();var tree=new LinkedHashMap<BlockPos,BlockState>();
    var base=new BlockPos(-3,80,0);var crown=base.above(4);var duckAt=new BlockPos(35,79,1);
    for(int y=0;y<4;y++)tree.put(base.above(y),com.chaogarden.fruit.FruitContent.TRUNK.get().defaultBlockState());
    tree.put(crown,com.chaogarden.fruit.FruitContent.treeForFruit("garden_fruit").defaultBlockState());
    for(var d:Direction.Plane.HORIZONTAL)tree.put(crown.below().relative(d),com.chaogarden.fruit.FruitContent.podForFruit("garden_fruit").defaultBlockState().setValue(com.chaogarden.fruit.HangingFruitBlock.FACING,d.getOpposite()).setValue(com.chaogarden.fruit.HangingFruitBlock.AGE,2));
    var duck=ToyContent.block("chao_rubber_duck").defaultBlockState().setValue(ChaoToyBlock.WATERLOGGED,true).setValue(ChaoToyBlock.FACING,Direction.WEST);
    tree.forEach((at,state)->{saved.put(at,l.getBlockState(at));put(l,at,state);});saved.put(duckAt,l.getBlockState(duckAt));put(l,duckAt,duck);
    var itemIds=l.getEntitiesOfClass(ItemEntity.class,GardenRooms025.BOUNDS).stream().map(ItemEntity::getUUID).collect(java.util.stream.Collectors.toSet());
    var moved=new ArrayList<BlockPos>();var spawnedItems=new java.util.concurrent.atomic.AtomicInteger();
    java.util.function.Consumer<net.neoforged.neoforge.event.entity.EntityJoinLevelEvent> births=event->{if(event.getLevel()==l&&event.getEntity() instanceof ItemEntity)spawnedItems.incrementAndGet();};
    net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(births);
    try{
      for(var e:tree.entrySet())if(e.getValue().getBlock() instanceof com.chaogarden.fruit.HangingFruitBlock)h.assertTrue(e.getValue().canSurvive(l,e.getKey()),"Source fruit-tree assembly was not genuinely supported");
      h.assertTrue(tree.containsKey(new BlockPos(-4,83,0)),"Tree fixture must overlap a future authored-pod coordinate");
      GardenRooms025.ensure(l);
      var crowns=new ArrayList<BlockPos>();var ducks=new ArrayList<BlockPos>();
      for(var at:BlockPos.betweenClosed(-39,77,-22,24,95,18)){
        var state=l.getBlockState(at);if(state.equals(tree.get(crown)))crowns.add(at.immutable());if(state.equals(duck))ducks.add(at.immutable());
      }
      h.assertTrue(crowns.size()==1&&ducks.size()==1,"Owned tree/crown or water toy was lost/copied");var offset=crowns.getFirst().subtract(crown);
      for(var e:tree.entrySet()){var at=e.getKey().offset(offset);moved.add(at);h.assertTrue(l.getBlockState(at).equals(e.getValue()),"Moved tree changed exact block state at "+at);if(e.getValue().getBlock() instanceof com.chaogarden.fruit.HangingFruitBlock)h.assertTrue(l.getBlockState(at).canSurvive(l,at),"Moved pod lost genuine support");}
      moved.add(ducks.getFirst());h.assertTrue(GardenRooms025.pondCell(ducks.getFirst().getX(),ducks.getFirst().getZ())&&ducks.getFirst().getY()==GardenRooms025.POND_LEVEL&&l.getFluidState(ducks.getFirst()).isSource(),"Waterlogged toy was moved onto dry lawn or lost source water");
      h.assertTrue(spawnedItems.get()==0&&itemIds.equals(l.getEntitiesOfClass(ItemEntity.class,GardenRooms025.BOUNDS).stream().map(ItemEntity::getUUID).collect(java.util.stream.Collectors.toSet())),"Crown removal duplicated harvested fruit/items during relocation");
    }finally{
      net.neoforged.neoforge.common.NeoForge.EVENT_BUS.unregister(births);var authored=GardenRooms025.plan();
      moved.sort(java.util.Comparator.comparingInt(at->l.getBlockState(at).getBlock() instanceof com.chaogarden.fruit.HangingFruitBlock?0:l.getBlockState(at).getBlock() instanceof com.chaogarden.fruit.ChaoTreeCrownBlock?1:2));
      for(var at:moved)put(l,at,authored.blocks.getOrDefault(at,Blocks.AIR.defaultBlockState()));saved.forEach((at,state)->put(l,at,state));
    }h.succeed();
  }
  @GameTest(template="empty",batch="garden0252_body_capacity",timeoutTicks=400)
  public static void exhaustedBodyLandingsAbortBeforeProviderOrTerrainCommit(GameTestHelper h){
    var l=garden(h);var data=GardenRegistry.get(l.getServer());var keeper=TestConnections.makeSurvivalServerPlayer(h);var plan=GardenRooms025.plan();
    var providerAt=new BlockPos(34,82,15);var provider=GardenRegistry.Place.at(l,providerAt);var before=l.getBlockState(providerAt);
    h.assertTrue(!data.gardens.containsKey(provider),"Capacity fixture refuses another keeper's provider");
    put(l,providerAt,ActivityContent.GARDEN.get().defaultBlockState());keeper.teleportTo(l,5.5,80.1,-12.5,0,0);keeper.hasChangedDimension();data.register(keeper,providerAt);
    h.assertTrue(data.gardens.containsKey(provider)&&data.gardens.get(provider).owner().equals(keeper.getUUID()),"Fixture must register the real Garden-dimension legacy provider");
    var actors=new ArrayList<ItemEntity>();var poses=new LinkedHashMap<ItemEntity,Vec3>();var forced=new HashSet<net.minecraft.world.level.ChunkPos>();
    for(int cx=-2;cx<=2;cx++)for(int cz=-2;cz<=1;cz++){var chunk=new net.minecraft.world.level.ChunkPos(cx,cz);if(!l.getForcedChunks().contains(chunk.toLong())&&forced.add(chunk))l.setChunkForced(cx,cz,true);}
    for(int x=-7;x<=18;x++)for(int z=-11;z<=14;z++){
      int y=GardenRooms025.surface(x,z)+1;var at=new BlockPos(x,y,z);
      if(!plan.blocks.getOrDefault(at.below(),Blocks.AIR.defaultBlockState()).is(LobbyDecor025.TURF.get()))continue;
      var goods=new ItemStack(Items.DIAMOND);goods.set(net.minecraft.core.component.DataComponents.CUSTOM_NAME,net.minecraft.network.chat.Component.literal("Capacity "+x+","+z));
      var actor=new ItemEntity(l,x+.5,y,z+.5,goods);actor.setNoGravity(true);actor.setDeltaMovement(Vec3.ZERO);actor.setPickUpDelay(32767);actors.add(actor);poses.put(actor,actor.position());
    }
    var displaced=new ItemEntity(l,.5,80,.5,new ItemStack(Items.EMERALD));displaced.setNoGravity(true);displaced.setDeltaMovement(Vec3.ZERO);displaced.setPickUpDelay(32767);actors.add(displaced);
    for(var actor:actors){var chunk=new net.minecraft.world.level.ChunkPos(actor.blockPosition());if(!l.getForcedChunks().contains(chunk.toLong())&&forced.add(chunk))l.setChunkForced(chunk.x,chunk.z,true);h.assertTrue(l.addFreshEntity(actor),"Could not register real body-capacity actor");}
    boolean[] closed={false};Runnable cleanup=()->{if(closed[0])return;closed[0]=true;actors.forEach(ItemEntity::discard);removeKeeperClaims(data,keeper.getUUID());put(l,providerAt,before);for(var chunk:forced)l.setChunkForced(chunk.x,chunk.z,false);keeper.server.getPlayerList().remove(keeper);};h.runAtTickTime(399,cleanup);
    h.startSequence().thenWaitUntil(()->{
      for(var actor:actors)h.assertTrue(l.getEntity(actor.getUUID())==actor,"Waiting for genuine capacity actor publication");
      for(int cx=-2;cx<=2;cx++)for(int cz=-2;cz<=1;cz++)h.assertTrue(l.isPositionEntityTicking(new BlockPos(cx*16+8,80,cz*16+8)),"Waiting for native entity-ticking capacity lease "+cx+","+cz);
    })
      .thenExecute(()->{try{
        // Arrange every genuinely published blocker at its proposed turf center.
        // Some centers contain new authored obstacles: those original bodies
        // also need relocation, and must not free an unreserved landing hole.
        poses.forEach((actor,at)->{actor.setPos(at);actor.setDeltaMovement(Vec3.ZERO);});
        h.assertTrue(actors.size()>450,"Body-capacity fixture lost its coverage: "+actors.size());displaced.setPos(35.5,83,15.5);
        var visible=l.getEntitiesOfClass(ItemEntity.class,GardenRooms025.BOUNDS).stream().map(ItemEntity::getUUID).collect(java.util.stream.Collectors.toSet());
        for(var actor:actors)h.assertTrue(!actor.isRemoved()&&visible.contains(actor.getUUID())&&l.getEntity(actor.getUUID())==actor,"Capacity body spatial precondition: role="+(actor==displaced?"displaced":"blocker")+" uuid="+actor.getUUID()+" pos="+actor.position()+" source="+poses.get(actor)+" removed="+actor.isRemoved()+" reason="+actor.getRemovalReason()+" tracked="+(l.getEntity(actor.getUUID())==actor)+" inQuery="+visible.contains(actor.getUUID())+" chunkTicking="+l.isPositionEntityTicking(actor.blockPosition())+" visible="+visible.size()+" expected="+actors.size());
        h.assertTrue(data.gardens.containsKey(provider)&&data.gardens.get(provider).owner().equals(keeper.getUUID()),"Capacity fixture lost its actual Garden provider registration");
        var ledger=data.save(new CompoundTag(),l.registryAccess());var canonical=l.getBlockState(GardenRooms025.TRANSPORTER);var turf=l.getBlockState(new BlockPos(0,79,0));var old=displaced.position();
        String refusal=null;boolean failed=false;try{GardenRooms025.ensure(l);}catch(IllegalStateException expected){refusal=expected.getMessage();failed=refusal.contains("No clear garden upgrade landing");}
        h.assertTrue(failed,"Exhausted physical landing capacity did not reach the expected preflight refusal; actual="+refusal+" blockers="+actors.size()+" visible="+visible.size()+" displaced="+displaced.position());
        h.assertTrue(ledger.equals(data.save(new CompoundTag(),l.registryAccess()))&&l.getBlockState(providerAt).is(ActivityContent.GARDEN.get())&&l.getBlockState(GardenRooms025.TRANSPORTER).equals(canonical)&&l.getBlockState(new BlockPos(0,79,0)).equals(turf)&&displaced.position().equals(old),"Body-capacity refusal happened after a provider/registry/terrain/body mutation");
        cleanup.run();h.succeed();
      }catch(RuntimeException failure){cleanup.run();throw failure;}});
  }
  @GameTest(template="empty",batch="garden0252_expanded_tree",timeoutTicks=140)
  public static void genuinelyGrownExpandedTreeBranchesLeavesAndRipenessSurviveShrink(GameTestHelper h){
    var l=garden(h);var base=new BlockPos(32,80,-8);var crown=com.chaogarden.fruit.FruitContent.crownPosition(base);var before=new LinkedHashMap<BlockPos,BlockState>();
    for(var at:BlockPos.betweenClosed(base.offset(-2,-1,-2),base.offset(2,7,2)))before.put(at.immutable(),l.getBlockState(at));
    put(l,base.below(),LobbyDecor025.TURF.get().defaultBlockState());
    h.assertTrue(com.chaogarden.fruit.FruitContent.growTree(l,base,"hero_garden_fruit"),"Actual FruitContent.growTree could not grow expanded source fixture");
    var grown=new LinkedHashMap<BlockPos,BlockState>();
    for(var d:Direction.Plane.HORIZONTAL){var at=crown.below().relative(d);var state=l.getBlockState(at);h.assertTrue(state.is(com.chaogarden.fruit.FruitContent.podForFruit("hero_garden_fruit")),"Genuine grown tree lacks supported outer pod");put(l,at,state.setValue(com.chaogarden.fruit.HangingFruitBlock.AGE,d.ordinal()%4));}
    for(var at:before.keySet())if(GardenRooms025.placeable(l.getBlockState(at).getBlock()))grown.put(at,l.getBlockState(at));
    h.assertTrue(grown.size()==13&&grown.get(crown).getValue(com.chaogarden.fruit.ChaoTreeCrownBlock.GARDEN),"Expanded fixture did not retain full native canopy");
    var spawned=new java.util.concurrent.atomic.AtomicInteger();java.util.function.Consumer<net.neoforged.neoforge.event.entity.EntityJoinLevelEvent> births=event->{if(event.getLevel()==l&&event.getEntity() instanceof ItemEntity)spawned.incrementAndGet();};
    net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(births);var moved=new ArrayList<BlockPos>();
    try{
      GardenRooms025.ensure(l);var crowns=new ArrayList<BlockPos>();for(var at:BlockPos.betweenClosed(-39,80,-22,24,96,18))if(l.getBlockState(at).equals(grown.get(crown)))crowns.add(at.immutable());
      h.assertTrue(crowns.size()==1,"Expanded owned tree was lost or copied");var offset=crowns.getFirst().subtract(crown);
      for(var e:grown.entrySet()){var at=e.getKey().offset(offset);moved.add(at);h.assertTrue(l.getBlockState(at).equals(e.getValue()),"Expanded branch/leaf/ripeness state changed at "+at);if(e.getValue().getBlock() instanceof com.chaogarden.fruit.HangingFruitBlock)h.assertTrue(l.getBlockState(at).canSurvive(l,at),"Expanded moved pod lost exact branch/canopy support");}
      h.assertTrue(l.getBlockState(base.offset(offset).below()).is(LobbyDecor025.TURF.get())&&spawned.get()==0,"Expanded tree floats or duplicated fruit during old crown removal");
    }finally{
      net.neoforged.neoforge.common.NeoForge.EVENT_BUS.unregister(births);var authored=GardenRooms025.plan();moved.sort(java.util.Comparator.comparingInt(at->l.getBlockState(at).getBlock() instanceof com.chaogarden.fruit.HangingFruitBlock?0:l.getBlockState(at).getBlock() instanceof com.chaogarden.fruit.ChaoTreeCrownBlock?1:2));for(var at:moved)put(l,at,authored.blocks.getOrDefault(at,Blocks.AIR.defaultBlockState()));before.forEach((at,state)->put(l,at,state));
    }h.succeed();
  }
}
