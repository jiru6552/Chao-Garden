package com.chaogarden.test;

import com.chaogarden.ChaoGarden;
import com.chaogarden.core.ChaoState;
import com.chaogarden.entity.ChaoEntity;
import com.chaogarden.fruit.FruitContent;
import com.chaogarden.fruit.HangingFruitBlock;
import com.chaogarden.hub.GardenRooms025;
import com.chaogarden.hub.HubContent;
import com.chaogarden.hub.LobbyDecor025;
import com.chaogarden.hub.PortalScenes0251;
import com.chaogarden.toys.ToyContent;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** Physical geometry regressions for the user's reported pool, plant, cliff and upgrade faults. */
@GameTestHolder(ChaoGarden.MODID)
@PrefixGameTestTemplate(false)
public final class GardenGeometryGameTests0251 {
  private GardenGeometryGameTests0251(){}
  private static ServerLevel garden(GameTestHelper h){
    var level=h.getLevel().getServer().getLevel(HubContent.GARDEN);
    h.assertTrue(level!=null&&HubContent.ensure(level),"First garden could not be built");return level;
  }
  private static boolean flower(Block block){return block==Blocks.BLUE_ORCHID||block==Blocks.ALLIUM||block==Blocks.PINK_TULIP;}
  private static void source(GameTestHelper h,ServerLevel level,BlockPos pos){
    var fluid=level.getFluidState(pos);h.assertTrue(fluid.is(Fluids.WATER)&&fluid.isSource(),"Pool contains air/flowing partial water at "+pos+": "+fluid);
  }
  /** Independent reference transform: quarter-turn the old north feature, then move fifteen blocks west. */
  private static BlockPos quarterTurn(int x,int y,int z){return new BlockPos(z-19,y,-x-21);}
  private static String fingerprint(Entity entity){
    if(entity instanceof ChaoEntity child)return child.owner+"|"+child.state().encode();
    if(entity instanceof com.chaogarden.entity.ChaoEggEntity egg)return egg.owner+"|"+egg.state().encode();
    return ((ItemEntity)entity).getItem().save(entity.level().registryAccess()).toString();
  }
  private static String bodyStatus(ServerLevel level,Entity entity){
    var overlaps=level.getEntities(entity,entity.getBoundingBox()).stream().filter(other->other.getBoundingBox().intersects(entity.getBoundingBox()))
        .map(other->other.getType()+"/"+other.getUUID()+"/"+other.position()+" collidable="+other.canBeCollidedWith()).toList();
    return entity.getType()+"/"+entity.getUUID()+" pos="+entity.position()+" box="+entity.getBoundingBox()+" tick="+entity.tickCount+" removed="+entity.isRemoved()+" reason="+entity.getRemovalReason()
        +" tracked="+(level.getEntity(entity.getUUID())==entity)+" chunkTicking="+level.isPositionEntityTicking(entity.blockPosition())+" collisionFree="+level.noCollision(entity)
        +" fluid="+level.getFluidState(entity.blockPosition())+" floor="+level.getBlockState(entity.blockPosition().below())+" overlaps="+overlaps;
  }
  @GameTest(template="empty",batch="garden0251_pool",timeoutTicks=400)
  public static void allPoolCellsAndReceivingBasinsStayFullWithSupportedPlants(GameTestHelper h){
    var level=garden(h);var forced=new ArrayList<net.minecraft.world.level.ChunkPos>();boolean[] done={false};
    Runnable cleanup=()->{if(done[0])return;done[0]=true;for(var chunk:forced)level.setChunkForced(chunk.x,chunk.z,false);};
    h.runAtTickTime(399,cleanup);
    for(int x=-3;x<=0;x++)for(int z=-2;z<=1;z++)if(!level.getForcedChunks().contains(net.minecraft.world.level.ChunkPos.asLong(x,z))){level.setChunkForced(x,z,true);forced.add(new net.minecraft.world.level.ChunkPos(x,z));}
    h.startSequence().thenWaitUntil(()->h.assertTrue(level.isPositionEntityTicking(new BlockPos(-20,79,-8))&&level.isPositionEntityTicking(new BlockPos(-36,91,-6)),"Pool/fall fixture awaits actual ticking chunks"))
      .thenIdle(100).thenExecute(()->{try{
        int columns=0;
        for(int x=-20;x<=-6;x++)for(int z=-6;z<=9;z++){
          double ellipse=Math.pow((x+13)/7.0,2)+Math.pow((z+1)/10.0,2);if(ellipse>=1)continue;
          columns++;source(h,level,quarterTurn(x,79,z));
          int bed=ellipse>.60?78:77;
          h.assertTrue(level.getBlockState(quarterTurn(x,bed,z)).is(LobbyDecor025.SAND.get()),"Quarter-turned receiving pool has no continuous solid basin at "+quarterTurn(x,bed,z));
          if(bed==77)source(h,level,quarterTurn(x,78,z));
        }
        h.assertTrue(columns==179,"Compact reference-scale pond coverage changed: "+columns+" columns");
        int[] fronts={-7,-10,-13,-16},levels={82,85,88,91};
        for(int step=0;step<4;step++)for(int x=-17;x<=-14;x++)for(int z=fronts[step]-2;z<fronts[step];z++){
          source(h,level,quarterTurn(x,levels[step],z));
          h.assertTrue(level.getBlockState(quarterTurn(x,levels[step]-1,z)).is(com.chaogarden.hub.NeutralGardenDecor02510.CASCADE_ROCK.get()),"Quarter-turned waterfall shelf has a void under its water");
        }
        for(int step=0;step<4;step++)for(int oldX=-17;oldX<=-14;oldX++){
          int lower=step==0?79:levels[step]-3;
          for(int y=lower;y<=levels[step];y++)source(h,level,quarterTurn(oldX,y,fronts[step]));
          source(h,level,quarterTurn(oldX,lower,fronts[step]).east());
        }
        h.assertTrue(GardenRooms025.WATERFALL_FLOW==Direction.EAST,"Waterfall no longer faces into the pond from the west");
        for(int y=80;y<=81;y++)h.assertTrue(level.getBlockState(quarterTurn(-15,y,-8)).is(com.chaogarden.hub.NeutralGardenDecor02510.CASCADE_ROCK.get()),"Unwanted waterfall cave remains open");
        int flowers=0,palms=0;
        for(int x=-39;x<=24;x++)for(int z=-22;z<=18;z++)for(int y=79;y<=100;y++){
          var pos=new BlockPos(x,y,z);var state=level.getBlockState(pos);
          if(flower(state.getBlock())){
            flowers++;h.assertTrue(state.canSurvive(level,pos)&&level.getBlockState(pos.below()).is(LobbyDecor025.TURF.get())&&level.getFluidState(pos).isEmpty(),"Authored flower floats over water/air at "+pos);
          }
          if(state.is(LobbyDecor025.PALM_CROWN.get()))palms++;
          if(x>=-40&&x<=-24&&z>=-13&&z<=1)h.assertTrue(!state.is(LobbyDecor025.PALM_CROWN.get())&&!state.is(LobbyDecor025.PALM_TRUNK.get()),"A palm or its broad fronds intrude into the western waterfall clearance");
        }
        h.assertTrue(flowers>=45&&palms==8,"Garden lost intended dry flower clusters or eight lawn palms: "+flowers+" / "+palms);
        var anchor=level.getBlockState(GardenRooms025.EXIT_PORTAL);
        h.assertTrue(anchor.is(LobbyDecor025.SCENE_ANCHOR.get())&&anchor.getValue(LobbyDecor025.SceneAnchor.PORTAL)&&anchor.getValue(LobbyDecor025.SceneAnchor.STYLE)==PortalScenes0251.TUNNEL,"Compact exit does not use the unframed 3x4 animated tunnel portal anchor");
        for(int x=4;x<=6;x++){
          for(int z=-18;z<=-17;z++){
            h.assertTrue(level.getBlockState(new BlockPos(x,84,z)).is(LobbyDecor025.CLIFF.get()),"Two-cell tunnel has no solid mountain roof");
            h.assertTrue(x==5?level.getBlockState(new BlockPos(x,83,z)).getCollisionShape(level,new BlockPos(x,83,z)).isEmpty():level.getBlockState(new BlockPos(x,83,z)).is(LobbyDecor025.CLIFF.get()),"Tunnel's centre/side height silhouette is wrong");
          }
          for(int y=80;y<=83;y++)h.assertTrue(level.getBlockState(new BlockPos(x,y,-19)).is(LobbyDecor025.CLIFF.get()),"Cave portal lacks a closed mountain behind it");
          for(int y=80;y<=82;y++)h.assertTrue(level.getBlockState(new BlockPos(x,y,-16)).getCollisionShape(level,new BlockPos(x,y,-16)).isEmpty(),"Tunnel depth exceeds the requested two mountain blocks");
        }
        for(var pos:BlockPos.betweenClosed(2,80,-20,8,88,-12))h.assertTrue(!(level.getBlockState(pos).getBlock() instanceof net.minecraft.world.level.block.SignBlock),"A wooden sign remains in the compact cave entrance");
        cleanup.run();h.succeed();
      }catch(RuntimeException failure){cleanup.run();throw failure;}});
  }

  @GameTest(template="empty",batch="garden0251_barrier",timeoutTicks=200)
  public static void openSkyEdgePhysicallyContainsKeepersBabiesAndDroppedItems(GameTestHelper h){
    var level=garden(h);var player=TestConnections.makeSurvivalServerPlayer(h);player.setGameMode(GameType.ADVENTURE);player.setNoGravity(true);
    var child=ChaoGarden.CHAO.get().create(level);child.setNoAi(true);child.setNoGravity(true);
    // The foreground hummock's turf occupies Y80. Starting an item inside that
    // block makes vanilla ItemEntity.tick enable temporary noPhysics escape.
    // Exercise the wall with ordinary actors standing on its actual Y81 top.
    var item=new ItemEntity(level,.5,81,16.5,new ItemStack(Items.APPLE));item.setNoGravity(true);item.setPickUpDelay(32767);
    boolean[] done={false};Runnable cleanup=()->{if(done[0])return;done[0]=true;child.discard();item.discard();if(HubContent.hasReturnJournal(player))HubContent.returnHome(player);player.server.getPlayerList().remove(player);};
    h.runAtTickTime(199,cleanup);
    try{
      player.teleportTo(level,.5,81,16.5,0,0);child.moveTo(1.5,81,16.5,0,0);
      h.assertTrue(level.addFreshEntity(child)&&level.addFreshEntity(item),"Could not register physical edge actors");
      h.startSequence().thenWaitUntil(()->h.assertTrue(level.getEntity(child.getUUID())==child&&level.getEntity(item.getUUID())==item,"Barrier fixture awaits real entity publication"))
        .thenExecute(()->{try{
          for(int y:new int[]{80,110,140,200,255})h.assertTrue(level.getBlockState(new BlockPos(0,y,19)).is(Blocks.BARRIER),"Open cliff has a gap in its invisible vertical limit at "+y);
          for(Entity actor:List.of(player,child,item)){
            h.assertTrue(level.noCollision(actor)&&!actor.noPhysics,"Barrier actor must start clear with native collision enabled: "+bodyStatus(level,actor)+" noPhysics="+actor.noPhysics);
            actor.move(MoverType.SELF,new Vec3(0,0,8));
            h.assertTrue(actor.getBoundingBox().maxZ<=19.000001&&actor.getZ()<19,"Real actor crossed the open cliff barrier: "+bodyStatus(level,actor)+" noPhysics="+actor.noPhysics);
            actor.setPos(actor.getX(),130,16.5);actor.move(MoverType.SELF,new Vec3(0,0,8));
            h.assertTrue(actor.getBoundingBox().maxZ<=19.000001,"Actor could fly/throw over a short cliff fence: "+bodyStatus(level,actor)+" noPhysics="+actor.noPhysics);
          }
          h.assertTrue(level.getBlockState(new BlockPos(0,80,18)).is(LobbyDecor025.TURF.get())&&level.getBlockState(new BlockPos(0,80,22)).isAir(),"Open horizon was replaced by a visible retaining wall or land extension");
          cleanup.run();h.succeed();
        }catch(RuntimeException failure){cleanup.run();throw failure;}});
    }catch(RuntimeException failure){cleanup.run();throw failure;}
  }

  @GameTest(template="empty",batch="garden0251_upgrade",timeoutTicks=400)
  public static void boundedUpgradeClearsLegacyAuthoredPropsAndPreservesActualOwnedBodies(GameTestHelper h){
    var level=garden(h);var keeper=TestConnections.makeSurvivalServerPlayer(h);keeper.setNoGravity(true);
    var child=ChaoGarden.CHAO.get().create(level);child.owner=keeper.getUUID();child.setNoAi(true);child.setNoGravity(true);child.setState(ChaoState.egg(3,false,true,0,new Random(251)));
    var egg=ChaoGarden.CHAO_EGG.get().create(level);egg.owner=keeper.getUUID();egg.setState(ChaoState.egg(4,true,false,0,new Random(252)));egg.setNoGravity(true);
    var item=new ItemEntity(level,19.5,80,-35.5,new ItemStack(Items.DIAMOND,3));item.setNoGravity(true);item.setPickUpDelay(32767);
    var toyPos=new BlockPos(6,80,8);var oldPod=new BlockPos(27,106,-39);var oldTransporter=new BlockPos(-33,82,15);
    boolean[] done={false};Runnable cleanup=()->{if(done[0])return;done[0]=true;child.discard();egg.discard();item.discard();for(var at:List.of(toyPos,oldPod,oldPod.east(),oldPod.above(),oldPod.east().above(),oldTransporter))level.setBlock(at,Blocks.AIR.defaultBlockState(),Block.UPDATE_CLIENTS|Block.UPDATE_KNOWN_SHAPE);if(HubContent.hasReturnJournal(keeper))HubContent.returnHome(keeper);keeper.server.getPlayerList().remove(keeper);};
    h.runAtTickTime(399,cleanup);
    try{
      for(var pos:BlockPos.betweenClosed(17,80,-37,22,83,-33))level.setBlock(pos,Blocks.AIR.defaultBlockState(),Block.UPDATE_CLIENTS|Block.UPDATE_KNOWN_SHAPE);
      child.moveTo(18.5,80,-35.5,0,0);egg.moveTo(21.5,80,-35.5,0,0);keeper.teleportTo(level,20.5,80,-35.5,0,0);
      h.assertTrue(level.addFreshEntity(child)&&level.addFreshEntity(egg)&&level.addFreshEntity(item),"Could not prepare real upgrade bodies");
      level.setBlock(toyPos,ToyContent.block("chao_tv").defaultBlockState(),Block.UPDATE_CLIENTS|Block.UPDATE_KNOWN_SHAPE);
      level.setBlock(oldPod.east(),LobbyDecor025.PALM_TRUNK.get().defaultBlockState(),Block.UPDATE_CLIENTS|Block.UPDATE_KNOWN_SHAPE);
      level.setBlock(oldPod.above(),LobbyDecor025.PALM_SUPPORT.get().defaultBlockState(),Block.UPDATE_CLIENTS|Block.UPDATE_KNOWN_SHAPE);
      level.setBlock(oldPod.east().above(),LobbyDecor025.PALM_CROWN.get().defaultBlockState(),Block.UPDATE_CLIENTS|Block.UPDATE_KNOWN_SHAPE);
      level.setBlock(oldPod,FruitContent.podForFruit("garden_fruit").defaultBlockState().setValue(HangingFruitBlock.AGE,3).setValue(HangingFruitBlock.FACING,Direction.EAST),Block.UPDATE_CLIENTS|Block.UPDATE_KNOWN_SHAPE);
      level.setBlock(oldTransporter,com.chaogarden.activities.ActivityContent.GARDEN.get().defaultBlockState(),Block.UPDATE_CLIENTS|Block.UPDATE_KNOWN_SHAPE);
      h.startSequence().thenWaitUntil(()->h.assertTrue(level.getEntity(child.getUUID())==child&&level.getEntity(egg.getUUID())==egg&&level.getEntity(item.getUUID())==item,"Upgrade fixture awaits same actual UUID publication"))
        .thenExecute(()->{try{
          h.assertTrue(level.getBlockState(oldPod).is(FruitContent.podForFruit("garden_fruit"))&&level.getBlockState(oldPod).canSurvive(level,oldPod)&&level.getBlockState(oldTransporter).is(com.chaogarden.activities.ActivityContent.GARDEN.get()),"Legacy upgrade fixtures did not remain genuinely present and supported");
          String childState=child.state().encode(),eggState=egg.state().encode();var goods=item.getItem().copy();UUID keeperId=keeper.getUUID();
          GardenRooms025.ensure(level);
          h.assertTrue(level.getEntity(child.getUUID())==child&&level.getEntity(egg.getUUID())==egg&&level.getEntity(item.getUUID())==item&&keeper.getUUID().equals(keeperId),"Bounded upgrade deleted/replaced an existing actor");
          h.assertTrue(child.state().encode().equals(childState)&&egg.state().encode().equals(eggState)&&keeper.getUUID().equals(child.owner)&&keeper.getUUID().equals(egg.owner),"Upgrade mutated Chao/egg state or keeper identity");
          h.assertTrue(item.getItem().getCount()==goods.getCount()&&ItemStack.isSameItemSameComponents(item.getItem(),goods),"Upgrade lost or replaced a dropped item");
          for(Entity actor:List.of(keeper,child,egg,item))com.mojang.logging.LogUtils.getLogger().info("CHAO_GARDEN_UPGRADE_BODY {}",bodyStatus(level,actor));
          for(Entity actor:List.of(keeper,child,egg,item)){
            h.assertTrue(level.noCollision(actor),"Upgrade leaves a physical collision: "+bodyStatus(level,actor));
            h.assertTrue(level.getFluidState(actor.blockPosition()).isEmpty(),"Upgrade leaves an immersed body: "+bodyStatus(level,actor));
            h.assertTrue(level.getBlockState(actor.blockPosition().below()).is(LobbyDecor025.TURF.get()),"Upgrade lacks actual turf support: "+bodyStatus(level,actor));
          }
          h.assertTrue(level.getBlockState(toyPos).is(ToyContent.block("chao_tv")),"Bounded scene update erased a keeper's placed toy");
          h.assertTrue(!level.getBlockState(oldPod).is(FruitContent.podForFruit("garden_fruit"))&&!level.getBlockState(oldTransporter).is(com.chaogarden.activities.ActivityContent.GARDEN.get()),"Old authored floating pods/transporter were incorrectly preserved as user placements");
          cleanup.run();h.succeed();
        }catch(RuntimeException failure){cleanup.run();throw failure;}});
    }catch(RuntimeException failure){cleanup.run();throw failure;}
  }

  @GameTest(template="empty",batch="garden0251_late_publication",timeoutTicks=300)
  public static void bodiesPublishedAfterRewriteEscapeAuthoredRockWithoutChangingTheirState(GameTestHelper h){
    var level=garden(h);var owner=UUID.randomUUID();
    var child=ChaoGarden.CHAO.get().create(level);child.owner=owner;child.setNoAi(true);child.setNoGravity(true);child.setState(ChaoState.egg(3,true,false,0,new Random(253)));
    var egg=ChaoGarden.CHAO_EGG.get().create(level);egg.owner=owner;egg.setNoGravity(true);egg.setState(ChaoState.egg(5,false,true,0,new Random(254)));
    var item=new ItemEntity(level,24.5,80,-23.5,new ItemStack(Items.DIAMOND,3));item.setNoGravity(true);item.setPickUpDelay(32767);item.setDeltaMovement(Vec3.ZERO);
    var swimmer=ChaoGarden.CHAO.get().create(level);swimmer.owner=owner;swimmer.setNoAi(true);swimmer.setNoGravity(true);swimmer.setDeltaMovement(Vec3.ZERO);
    child.moveTo(10.5,80,-18.5,0,0);egg.moveTo(12.5,80,-18.5,0,0);swimmer.moveTo(-20.5,79.05,-8.5,0,0);
    var bodies=List.<Entity>of(child,egg,item,swimmer);var ids=bodies.stream().map(Entity::getUUID).collect(java.util.stream.Collectors.toSet());
    Map<UUID,String> before=new HashMap<>(),after=new HashMap<>();Map<UUID,Vec3> beforePosition=new HashMap<>(),afterPosition=new HashMap<>();
    Consumer<EntityTickEvent.Post> pre=event->{var actor=event.getEntity();if(ids.contains(actor.getUUID())&&actor.tickCount%10==0&&!before.containsKey(actor.getUUID())){before.put(actor.getUUID(),fingerprint(actor));beforePosition.put(actor.getUUID(),actor.position());}};
    Consumer<EntityTickEvent.Post> post=event->{var actor=event.getEntity();if(ids.contains(actor.getUUID())&&before.containsKey(actor.getUUID())&&!after.containsKey(actor.getUUID())&&(!(actor instanceof ItemEntity)||GardenRooms025.insideField(actor.blockPosition().getX(),actor.blockPosition().getZ()))){after.put(actor.getUUID(),fingerprint(actor));afterPosition.put(actor.getUUID(),actor.position());}};
    // Outside-field bodies can be rescued by the ordinary global server tick
    // before their own tenth EntityPost tick. Observe that real event boundary too.
    Consumer<net.neoforged.neoforge.event.tick.ServerTickEvent.Post> serverPre=event->{if(event.getServer()==level.getServer()&&level.getEntity(item.getUUID())==item&&!GardenRooms025.insideField(item.blockPosition().getX(),item.blockPosition().getZ())&&!before.containsKey(item.getUUID())){before.put(item.getUUID(),fingerprint(item));beforePosition.put(item.getUUID(),item.position());}};
    Consumer<net.neoforged.neoforge.event.tick.ServerTickEvent.Post> serverPost=event->{if(event.getServer()==level.getServer()&&before.containsKey(item.getUUID())&&!after.containsKey(item.getUUID())&&GardenRooms025.insideField(item.blockPosition().getX(),item.blockPosition().getZ())){after.put(item.getUUID(),fingerprint(item));afterPosition.put(item.getUUID(),item.position());}};
    var forced=new ArrayList<net.minecraft.world.level.ChunkPos>();boolean[] done={false};
    Runnable cleanup=()->{if(done[0])return;done[0]=true;NeoForge.EVENT_BUS.unregister(pre);NeoForge.EVENT_BUS.unregister(post);NeoForge.EVENT_BUS.unregister(serverPre);NeoForge.EVENT_BUS.unregister(serverPost);bodies.forEach(Entity::discard);for(var chunk:forced)level.setChunkForced(chunk.x,chunk.z,false);};
    h.runAtTickTime(299,()->{if(!done[0]){var status=bodies.stream().map(body->bodyStatus(level,body)).toList();cleanup.run();h.fail("Late publication deadline: before="+before.keySet()+" after="+after.keySet()+" bodies="+status);}});
    for(int tick:new int[]{30,100,250})h.runAtTickTime(tick,()->{if(!done[0])for(var body:bodies)com.mojang.logging.LogUtils.getLogger().info("CHAO_GARDEN_LATE tick={} before={} after={} {}",h.getTick(),before.containsKey(body.getUUID()),after.containsKey(body.getUUID()),bodyStatus(level,body));});
    try{
      NeoForge.EVENT_BUS.addListener(EventPriority.HIGHEST,pre);NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST,post);NeoForge.EVENT_BUS.addListener(EventPriority.HIGHEST,serverPre);NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST,serverPost);
      // Lease all possible compact destinations as well as the source chunks;
      // a correct relocation must keep receiving genuine entity ticks here.
      for(int cx=-2;cx<=1;cx++)for(int cz=-2;cz<=1;cz++){var chunk=new net.minecraft.world.level.ChunkPos(cx,cz);if(!level.getForcedChunks().contains(chunk.toLong())){level.setChunkForced(chunk.x,chunk.z,true);forced.add(chunk);}}
      h.assertTrue(!level.noCollision(child)&&!level.noCollision(egg)&&!GardenRooms025.insideField(item.blockPosition().getX(),item.blockPosition().getZ()),"Late-publication fixtures must include actual cliff rock and a body in the removed outer area");
      for(var body:bodies)h.assertTrue(level.addFreshEntity(body),"Could not naturally publish late actor "+body.getType());
      h.startSequence().thenWaitUntil(()->{
        for(var body:bodies)h.assertTrue(level.getEntity(body.getUUID())==body,"Late actor UUID is not yet published: "+bodyStatus(level,body));
        h.assertTrue(before.size()==4&&after.size()==4,"Awaiting genuine entity/global Post ticks; no fixture invokes entity/service ticks");
        h.assertTrue(level.noCollision(child)&&level.noCollision(egg)&&level.noCollision(item),"Late body remains trapped in authored terrain after natural ticks");
      }).thenExecute(()->{try{
        for(var body:bodies)h.assertTrue(before.get(body.getUUID()).equals(after.get(body.getUUID())),"Terrain-only rescue mutated a Chao/egg owner/state or item payload");
        for(var body:List.<Entity>of(child,egg,item))h.assertTrue(!beforePosition.get(body.getUUID()).equals(afterPosition.get(body.getUUID()))&&level.getBlockState(body.blockPosition().below()).is(LobbyDecor025.TURF.get()),"Late body was not moved to actual dry supported turf");
        h.assertTrue(beforePosition.get(swimmer.getUUID()).equals(afterPosition.get(swimmer.getUUID()))&&!level.getFluidState(swimmer.blockPosition()).isEmpty(),"Rescue interfered with an ordinary unblocked swimmer");
        h.assertTrue(child.owner.equals(owner)&&egg.owner.equals(owner),"Late publication lost keeper ownership");cleanup.run();h.succeed();
      }catch(RuntimeException failure){cleanup.run();throw failure;}});
    }catch(RuntimeException failure){cleanup.run();throw failure;}
  }
}
