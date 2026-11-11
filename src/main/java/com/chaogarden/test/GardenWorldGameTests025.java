package com.chaogarden.test;

import com.chaogarden.ChaoGarden;
import com.chaogarden.core.ChaoState;
import com.chaogarden.entity.ChaoEggEntity;
import com.chaogarden.entity.ChaoEntity;
import com.chaogarden.fruit.FruitContent;
import com.chaogarden.fruit.HangingFruitBlock;
import com.chaogarden.hub.GardenRooms025;
import com.chaogarden.hub.HubContent;
import com.chaogarden.hub.HubWorldData;
import com.chaogarden.hub.LobbyDecor025;
import com.chaogarden.item.ChaoCarrierItem;
import com.chaogarden.toys.ToyContent;
import com.mojang.authlib.GameProfile;
import io.netty.channel.embedded.EmbeddedChannel;
import java.util.Random;
import java.util.UUID;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.EntityTravelToDimensionEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.network.registration.NetworkRegistry;

/** Real useOn/event paths in the protected, playable first garden. No fake item bypasses. */
@GameTestHolder(ChaoGarden.MODID)
@PrefixGameTestTemplate(false)
public final class GardenWorldGameTests025 {
  static ServerPlayer player(GameTestHelper h,GameType mode){
    var cookie=CommonListenerCookie.createInitial(new GameProfile(UUID.randomUUID(),"garden-keeper"),false);
    var p=new ServerPlayer(h.getLevel().getServer(),h.getLevel(),cookie.gameProfile(),cookie.clientInformation());
    var connection=new Connection(PacketFlow.SERVERBOUND);new EmbeddedChannel(connection);NetworkRegistry.configureMockConnection(connection);
    p.server.getPlayerList().placeNewPlayer(connection,p,cookie);p.setGameMode(mode);
    var at=h.absolutePos(new BlockPos(2,2,2));h.getLevel().setBlockAndUpdate(at.below(),Blocks.STONE.defaultBlockState());
    p.teleportTo(at.getX()+.5,at.getY()+.1,at.getZ()+.5);p.setNoGravity(true);return p;
  }
  static void finish(ServerPlayer p){if(HubContent.isHubDimension(p.level()))HubContent.returnHome(p);p.server.getPlayerList().remove(p);}
  static InteractionResult use(ServerPlayer p,ItemStack item,BlockPos ground){
    p.setItemInHand(InteractionHand.MAIN_HAND,item);
    return p.gameMode.useItemOn(p,p.serverLevel(),item,InteractionHand.MAIN_HAND,
        new BlockHitResult(Vec3.atCenterOf(ground).add(0,.5,0),Direction.UP,ground,false));
  }

  @GameTest(template="empty",timeoutTicks=500)
  public static void gardenWalkingRoutesAllowCareAndRestoreOrigin(GameTestHelper h){
    var p=player(h,GameType.ADVENTURE);var home=p.serverLevel();var origin=p.position();
    var carried=ChaoGarden.CHAO.get().create(home);carried.owner=p.getUUID();carried.setNoAi(true);carried.setState(ChaoState.egg(3,false,true,0,new Random(25)));
    var carrier=new ItemStack(ChaoGarden.item("chao_carrier"));h.assertTrue(ChaoCarrierItem.capture(carrier,carried,p),"Could not prepare genuine held Chao");
    UUID identity=ChaoCarrierItem.storedUUID(carrier);p.getInventory().setItem(5,carrier);
    h.assertTrue(HubContent.enterHub(p),"Hub unavailable for garden journey");
    var garden=p.server.getLevel(HubContent.GARDEN);h.assertTrue(garden!=null,"Garden destination was not prebuilt by real Hub entry");
    var eggGround=new BlockPos(-2,79,5);var carrierGround=new BlockPos(2,79,5);var toyGround=new BlockPos(2,79,8);
    var oldToy=garden.getBlockState(toyGround.above());
    var eggs=new java.util.concurrent.atomic.AtomicReference<java.util.List<ChaoEggEntity>>(java.util.List.of());
    var released=new java.util.concurrent.atomic.AtomicReference<ChaoEntity>();
    var forced=new java.util.ArrayList<net.minecraft.world.level.ChunkPos>();
    boolean[] done={false};String[] phase={"setup"};long[] phaseAt={h.getTick()};
    java.util.function.Supplier<String> status=()->"phase="+phase[0]+" tick="+h.getTick()+" elapsed="+(h.getTick()-phaseAt[0])
        +" player="+p.level().dimension().location()+"/"+p.position()
        +" eggLoaded="+garden.areEntitiesLoaded(net.minecraft.world.level.ChunkPos.asLong(eggGround))+" eggTicking="+garden.isPositionEntityTicking(eggGround)
        +" careLoaded="+garden.areEntitiesLoaded(net.minecraft.world.level.ChunkPos.asLong(carrierGround))+" careTicking="+garden.isPositionEntityTicking(carrierGround)
        +" eggs="+garden.getEntitiesOfClass(ChaoEggEntity.class,new AABB(-3,79,4,0,82,7),e->p.getUUID().equals(e.owner)).size()
        +" released="+(garden.getEntity(identity)!=null)+" carrierCount="+carrier.getCount();
    Runnable cleanup=()->{if(done[0])return;done[0]=true;
      if(released.get()!=null)released.get().discard();for(var e:eggs.get())e.discard();
      var actual=garden.getEntity(identity);if(actual!=null)actual.discard();
      for(var e:garden.getEntitiesOfClass(ChaoEggEntity.class,GardenRooms025.BOUNDS,e->p.getUUID().equals(e.owner)))e.discard();
      garden.setBlockAndUpdate(toyGround.above(),oldToy);
      for(var chunk:forced)garden.setChunkForced(chunk.x,chunk.z,false);finish(p);
    };
    Consumer<String> begin=next->{
      com.mojang.logging.LogUtils.getLogger().info("CHAO_GARDEN_ROUTE_PHASE_READY {}",status.get());
      phase[0]=next;phaseAt[0]=h.getTick();long began=phaseAt[0];
      com.mojang.logging.LogUtils.getLogger().info("CHAO_GARDEN_ROUTE_PHASE_START {} budget=120",status.get());
      h.runAtTickTime(began+120,()->{if(!done[0]&&phase[0].equals(next)&&phaseAt[0]==began){
        String state=status.get();com.mojang.logging.LogUtils.getLogger().error("CHAO_GARDEN_ROUTE_PHASE_DEADLINE {}",state);cleanup.run();h.fail("Garden journey exceeded its120tick phase budget: "+state);
      }});
    };
    h.runAtTickTime(499,()->{if(!done[0]){String state=status.get();cleanup.run();h.fail("Garden journey exceeded its bounded publication and two real cooldown budgets: "+state);}});
    begin.accept("hub-cooldown");
    // The two real gate cooldowns consume90 ticks. Each asynchronous publication
    // phase now has its own120tick bound, rather than sharing the old remaining70.
    h.startSequence().thenIdle(45).thenExecute(()->{try{
      p.teleportTo(.5,80.1,18.5);HubContent.tickPlayer(p);p.teleportTo(.5,80.1,23.5);HubContent.tickPlayer(p);p.hasChangedDimension();
      h.assertTrue(p.level().dimension().equals(HubContent.GARDEN)&&p.gameMode.getGameModeForPlayer()==GameType.ADVENTURE&&!p.mayBuild(),"Walking through stone garden arch did not retain Adventure protection");
      p.teleportTo(.5,80.1,5.5);
      // Egg placement crosses the X=0 chunk boundary. Keep exactly both native
      // care chunks ticking until their actual actors have been observed.
      for(var at:java.util.List.of(eggGround,carrierGround)){
        var chunk=new net.minecraft.world.level.ChunkPos(at);
        if(!garden.getForcedChunks().contains(chunk.toLong())){garden.setChunkForced(chunk.x,chunk.z,true);forced.add(chunk);}
      }
      begin.accept("care-chunks");
    }catch(RuntimeException failure){cleanup.run();throw failure;}})
      .thenWaitUntil(()->h.assertTrue(garden.isPositionEntityTicking(eggGround)&&garden.isPositionEntityTicking(carrierGround),"Awaiting actual native care chunk ticking: "+status.get()))
      .thenExecute(()->{try{
        begin.accept("egg-publication");var egg=new ItemStack(ChaoGarden.item("normal_egg"));h.assertTrue(use(p,egg,eggGround).consumesAction()&&egg.isEmpty(),"Actual gameMode egg placement failed in first garden");
      }catch(RuntimeException failure){cleanup.run();throw failure;}}).thenWaitUntil(()->{
        // addFreshEntity can succeed before a newly loaded realm publishes the
        // entity section. Observe genuine tracking; never substitute a fixture egg.
        var found=garden.getEntitiesOfClass(ChaoEggEntity.class,new AABB(-3,79,4,0,82,7),e->p.getUUID().equals(e.owner));
        h.assertTrue(found.size()==1,"Placed egg is not yet tracked in its garden section: "+status.get());eggs.set(java.util.List.copyOf(found));
      }).thenExecute(()->{try{
        begin.accept("carrier-publication");h.assertTrue(use(p,carrier,carrierGround).consumesAction()&&carrier.isEmpty(),"Actual gameMode held Chao release failed");
      }catch(RuntimeException failure){cleanup.run();throw failure;}})
      .thenWaitUntil(()->{
        var entity=garden.getEntity(identity);h.assertTrue(entity instanceof ChaoEntity&&p.getUUID().equals(((ChaoEntity)entity).owner),"Released original UUID/keeper is not yet tracked: "+status.get());released.set((ChaoEntity)entity);
      }).thenExecute(()->{try{
        begin.accept("native-care-and-ensure");var toy=new ItemStack(ChaoGarden.item("chao_tv"));garden.setBlockAndUpdate(toyGround.above(),Blocks.AIR.defaultBlockState());
        h.assertTrue(use(p,toy,toyGround).consumesAction()&&garden.getBlockState(toyGround.above()).is(ToyContent.block("chao_tv")),"Actual gameMode toy BlockItem placement failed");
        h.assertTrue(!p.mayBuild()&&p.gameMode.getGameModeForPlayer()==GameType.ADVENTURE,"Care item placement leaked build permission or changed game mode");
        GardenRooms025.ensure(garden);
        h.assertTrue(garden.getBlockState(toyGround.above()).is(ToyContent.block("chao_tv"))&&garden.getEntity(identity)==released.get()
            &&eggs.get().stream().allMatch(e->garden.getEntity(e.getUUID())==e),"Scenery revision erased a keeper's toy or creature/egg identity");
        var dirt=new ItemStack(Items.DIRT);var forbidden=new BlockPos(4,79,8);
        h.assertTrue(use(p,dirt,forbidden)==InteractionResult.FAIL&&dirt.getCount()==1&&garden.getBlockState(forbidden.above()).isAir(),"Unrelated block placement bypassed authored garden protection");
        p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Items.DIAMOND_PICKAXE));
        h.assertTrue(!p.gameMode.destroyBlock(forbidden)&&garden.getBlockState(forbidden).is(LobbyDecor025.TURF.get()),"Garden terrain could be destroyed");
        p.setItemInHand(InteractionHand.MAIN_HAND,ItemStack.EMPTY);p.setShiftKeyDown(true);
        h.assertTrue(use(p,ItemStack.EMPTY,toyGround.above()).consumesAction()&&garden.getBlockState(toyGround.above()).isAir(),"Adventure keeper could not recover a placed toy with empty-hand sneak use");
        p.setShiftKeyDown(false);h.assertTrue(!p.mayBuild(),"Prop recovery leaked Adventure build permission");
        h.assertTrue(HubWorldData.get(p.server).visit(p.getUUID()).getInt("GameMode")==GameType.ADVENTURE.getId(),"Garden admission overwrote original Adventure mode");
        begin.accept("return-cooldown");
      }catch(RuntimeException failure){cleanup.run();throw failure;}}).thenIdle(45).thenExecute(()->{try{
        begin.accept("walking-home");
        p.teleportTo(5.5,80.1,-12.5);HubContent.tickPlayer(p);p.teleportTo(5.5,80.1,-17.5);HubContent.tickPlayer(p);p.hasChangedDimension();
        h.assertTrue(p.level().dimension().equals(HubContent.HUB)&&p.gameMode.getGameModeForPlayer()==GameType.ADVENTURE,"Walking through cave did not return to protected hub");
        h.assertTrue(HubContent.returnHome(p)&&p.level()==home&&p.position().distanceTo(origin)<5&&p.gameMode.getGameModeForPlayer()==GameType.ADVENTURE&&!HubContent.hasReturnJournal(p),"Garden route failed original mode/location/journal restoration");
        com.mojang.logging.LogUtils.getLogger().info("CHAO_GARDEN_ROUTE_COMPLETE {}",status.get());cleanup.run();
      }catch(RuntimeException failure){cleanup.run();throw failure;}}).thenSucceed();
  }

  @GameTest(template="empty",timeoutTicks=80)
  public static void canceledGardenTravelKeepsHubModeAndOrigin(GameTestHelper h){
    var p=player(h,GameType.SURVIVAL);h.assertTrue(HubContent.enterHub(p),"Hub unavailable");
    var journal=HubWorldData.get(p.server).visit(p.getUUID()).copy();
    Consumer<EntityTravelToDimensionEvent> deny=e->{if(e.getEntity()==p&&e.getDimension().equals(HubContent.GARDEN))e.setCanceled(true);};
    NeoForge.EVENT_BUS.addListener(deny);
    try{h.assertTrue(!HubContent.visitGarden(p),"Canceled garden travel reported success");}finally{NeoForge.EVENT_BUS.unregister(deny);}
    h.assertTrue(p.level().dimension().equals(HubContent.HUB)&&p.gameMode.getGameModeForPlayer()==GameType.ADVENTURE&&journal.equals(HubWorldData.get(p.server).visit(p.getUUID())),"Canceled garden trip changed location/mode/original journal");
    h.assertTrue(HubContent.visitGarden(p)&&p.gameMode.getGameModeForPlayer()==GameType.ADVENTURE&&HubContent.returnToHub(p)&&HubContent.returnHome(p)&&p.gameMode.getGameModeForPlayer()==GameType.SURVIVAL,"Retry and complete return failed to retain Adventure then restore original Survival");
    finish(p);h.succeed();
  }

  @GameTest(template="empty",timeoutTicks=80)
  public static void authoredPalmFruitUsesRealSupportRipeningAndHarvest(GameTestHelper h){
    var garden=h.getLevel().getServer().getLevel(HubContent.GARDEN);h.assertTrue(garden!=null&&HubContent.ensure(garden),"Garden dimension unavailable");
    var tree=GardenRooms025.PALMS[0];var crown=new BlockPos(tree[0],GardenRooms025.surface(tree[0],tree[1])+tree[2]+1,tree[1]);
    var pod=crown.below().north();var state=garden.getBlockState(pod);var fruit=FruitContent.podForFruit("garden_fruit");
    h.assertTrue(state.is(fruit)&&state.canSurvive(garden,pod),"Authored palm fruit lacks real supported pod");
    h.assertTrue(!GardenRooms025.breakable(pod,state)&&GardenRooms025.placeable(ToyContent.block("chao_tv")),"Fixed fruit protection or playable garden props wrong");
    var trunk=crown.below();h.assertTrue(!garden.getBlockState(trunk).isPathfindable(net.minecraft.world.level.pathfinder.PathComputationType.LAND),"Solid partial palm trunk is incorrectly considered walkable");
    int startX=tree[0]-2,startZ=tree[1],startY=GardenRooms025.surface(startX,startZ)+1;
    var goal=new BlockPos(tree[0]+3,GardenRooms025.surface(tree[0]+3,tree[1])+1,tree[1]);
    h.assertTrue(garden.getBlockState(new BlockPos(startX,startY-1,startZ)).is(LobbyDecor025.TURF.get())&&garden.getBlockState(goal.below()).is(LobbyDecor025.TURF.get()),"Native palm detour start/goal do not stand on actual authored turf");
    int chunkX=tree[0]>>4,chunkZ=tree[1]>>4;boolean wasForced=garden.getForcedChunks().contains(net.minecraft.world.level.ChunkPos.asLong(chunkX,chunkZ));
    if(!wasForced)garden.setChunkForced(chunkX,chunkZ,true);
    var walker=ChaoGarden.CHAO.get().create(garden);walker.moveTo(startX+.5,startY,startZ+.5,0,0);walker.setNoAi(true);
    h.assertTrue(garden.addFreshEntity(walker),"Could not create native palm path fixture");
    h.startSequence().thenWaitUntil(()->h.assertTrue(garden.getEntity(walker.getUUID())==walker&&garden.getChunkSource().getChunkNow(chunkX,chunkZ)!=null,
        "Palm navigation fixture awaits real actor/chunk tracking in newly loaded Garden"))
    .thenExecute(()->{
      walker.move(net.minecraft.world.entity.MoverType.SELF,new Vec3(0,-.1,0));
      var route=walker.getNavigation().createPath(goal,0);
      h.assertTrue(route!=null&&route.canReach(),"Native Chao navigation cannot route around authored palm; position="+walker.position()+", onGround="+walker.onGround()+", startFloor="+garden.getBlockState(new BlockPos(startX,startY-1,startZ))+", goal="+goal+", goalFloor="+garden.getBlockState(goal.below())+", path="+(route==null?"none":route.getEndNode()));
      boolean detoured=false;for(int i=0;i<route.getNodeCount();i++){var node=route.getNode(i);h.assertTrue(node.x!=tree[0]||node.z!=tree[1],"Native Chao path enters the solid palm trunk");detoured|=node.z!=tree[1];}
      h.assertTrue(detoured,"Native Chao path failed to detour around the palm footprint");walker.discard();
      garden.setBlockAndUpdate(pod,state.setValue(HangingFruitBlock.AGE,3));
      h.assertTrue(fruit.harvest(garden,pod,garden.getBlockState(pod))&&garden.getBlockState(pod).getValue(HangingFruitBlock.AGE)==0,"Palm did not harvest/reset real ripening stage");
      var drops=garden.getEntitiesOfClass(ItemEntity.class,new AABB(pod).inflate(2));
      h.assertTrue(drops.stream().anyMatch(e->e.getItem().is(ChaoGarden.item("garden_fruit"))),"Palm produced no genuine Garden Fruit item");
      h.assertTrue(garden.getEntitiesOfClass(ChaoEggEntity.class,GardenRooms025.BOUNDS).stream().noneMatch(e->e.owner==null),"Authored scene added unsolicited free eggs");
      if(!wasForced)garden.setChunkForced(chunkX,chunkZ,false);
    }).thenSucceed();
  }
}
