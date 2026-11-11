package com.chaogarden.test;

import com.chaogarden.ChaoGarden;
import com.chaogarden.activities.ActivityContent;
import com.chaogarden.activities.GardenRegistry;
import com.chaogarden.activities.GardenTransporterBlock;
import com.chaogarden.core.ChaoState;
import com.chaogarden.fruit.FruitContent;
import com.chaogarden.fruit.HangingFruitBlock;
import com.chaogarden.hub.DarkGardenDecor0255;
import com.chaogarden.hub.DarkGardenRooms0255;
import com.chaogarden.hub.HubContent;
import com.chaogarden.hub.LobbyDecor025;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** The actual new realm: native water/care fruit and lossless purchased-provider preparation. */
@GameTestHolder(ChaoGarden.MODID)
@PrefixGameTestTemplate(false)
public final class DarkGardenGameTests0255 {
  private static final int FLAGS=Block.UPDATE_CLIENTS|Block.UPDATE_KNOWN_SHAPE;
  private static net.minecraft.server.level.ServerLevel dark(GameTestHelper h){
    var level=h.getLevel().getServer().getLevel(HubContent.DARK_GARDEN);
    h.assertTrue(level!=null&&HubContent.ensure(level),"Actual Dark Garden is unavailable");return level;
  }
  private static void put(net.minecraft.server.level.ServerLevel level,BlockPos at,net.minecraft.world.level.block.state.BlockState state){level.setBlock(at,state,FLAGS);}

  @GameTest(template="empty",batch="dark0255_geometry",timeoutTicks=300)
  public static void darkGardenHasPaleShoreNativeRedPoolBareFruitAndSeparateDistantEffigy(GameTestHelper h){
    var level=dark(h);var leased=new ArrayList<net.minecraft.world.level.ChunkPos>();boolean[] done={false};
    Runnable cleanup=()->{if(done[0])return;done[0]=true;for(var c:leased)level.setChunkForced(c.x,c.z,false);};h.runAtTickTime(299,cleanup);
    for(int x=-2;x<=1;x++)for(int z=-3;z<=1;z++){
      var c=new net.minecraft.world.level.ChunkPos(x,z);if(!level.getForcedChunks().contains(c.toLong())){level.setChunkForced(x,z,true);leased.add(c);}
    }
    h.startSequence().thenWaitUntil(()->h.assertTrue(level.isPositionEntityTicking(DarkGardenRooms0255.WATERFALL)
        &&level.isPositionEntityTicking(DarkGardenRooms0255.TRANSPORTER),"Dark landmark fixture awaits native entity-ticking chunks"))
      .thenIdle(80).thenExecute(()->{try{
        int sources=0;
        for(int x=-24;x<=24;x++)for(int z=-20;z<=20;z++)if(DarkGardenRooms0255.poolCell(x,z)){
          var at=new BlockPos(x,79,z);var water=level.getFluidState(at);
          h.assertTrue(water.is(Fluids.WATER)&&water.isSource(),"Dark red pool has a non-water/partial cell at "+at);
          h.assertTrue(level.getBlockState(at.below(2)).is(DarkGardenDecor0255.block("dark_garden_rock")),"Dark pool has no solid native basin at "+at);sources++;
        }
        h.assertTrue(sources==294,"Dark reference-sized native pool/channel footprint changed: "+sources);
        for(int x=9;x<=11;x++)for(int y=79;y<=89;y++)h.assertTrue(level.getFluidState(new BlockPos(x,y,-16)).is(Fluids.WATER),"Back-cliff waterfall is not native water");
        for(var grave:DarkGardenRooms0255.GRAVES){var at=new BlockPos(grave[0],DarkGardenRooms0255.surface(grave[0],grave[1])+1,grave[1]);
          h.assertTrue(level.getBlockState(at).is(DarkGardenDecor0255.block("dark_garden_gravestone"))&&level.getBlockState(at.below()).isFaceSturdy(level,at.below(),Direction.UP),"Dark engraved headstone floats or is missing");}
        int tiles=0;
        for(int row=0;row<3;row++)for(int col=0;col<3;col++){
          var at=new BlockPos(col-1,107-row,-32);var state=level.getBlockState(at);
          h.assertTrue(state.is(DarkGardenDecor0255.block("dark_garden_effigy"))&&state.getValue(DarkGardenDecor0255.Effigy.TILE)==1+row*3+col
              &&level.getBlockState(at.north()).is(DarkGardenDecor0255.block("dark_garden_effigy")),"Distant carved Chao crag has a repeated/missing/floating face tile");tiles++;
        }
        h.assertTrue(tiles==9&&!DarkGardenRooms0255.insideField(0,-32),"Distant effigy incorrectly occupies the playable care shore");
        var portal=level.getBlockState(DarkGardenRooms0255.EXIT_PORTAL);
        h.assertTrue(portal.is(LobbyDecor025.SCENE_ANCHOR.get())&&portal.getValue(LobbyDecor025.SceneAnchor.PORTAL)
            &&portal.getValue(LobbyDecor025.SceneAnchor.STYLE)==DarkGardenRooms0255.EXIT_STYLE,"Dark return tunnel lost its real portal anchor");
        int attached=0;
        for(var tree:DarkGardenRooms0255.FRUIT_TREES)for(Direction side:Direction.Plane.HORIZONTAL){
          var at=new BlockPos(tree[0],82,tree[1]).relative(side);var fruit=level.getBlockState(at);var branch=level.getBlockState(at.above());
          h.assertTrue(fruit.is(FruitContent.podForFruit("dark_garden_fruit"))&&fruit.canSurvive(level,at),"Dark fruit is floating or missing at "+at);
          h.assertTrue(branch.is(DarkGardenDecor0255.block("dark_garden_deadwood"))
              &&branch.getValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.AXIS)==side.getAxis()
              &&!branch.getValue(DarkGardenDecor0255.Deadwood.BRANCHED),"Dark fruit is not directly below a real horizontal wood arm at "+at);
          // Native stem top and physical branch underside occupy the same height.
          h.assertTrue(Math.abs(fruit.getShape(level,at).max(Direction.Axis.Y)-1-branch.getShape(level,at.above()).min(Direction.Axis.Y))<1.e-8,
              "Dark fruit stem stops short of its visible branch at "+at);
          attached++;
        }
        h.assertTrue(attached==12,"Not all four pods on every Dark tree were checked");
        for(var authored:DarkGardenRooms0255.FRUIT_TREES){
          var tip=new BlockPos(authored[0]+1,84+authored[2]/2,authored[1]);
          h.assertTrue(level.getBlockState(tip).is(DarkGardenDecor0255.block("dark_garden_deadwood"))
              &&level.getBlockState(tip.above()).isAir(),"Dark tree's upper branches were not raised exactly one block");
        }
        // Replay a saved0257 tree: migrate each real pod upward without
        // refilling harvested stages or dropping an extra item. The new tree
        // still starts at the same ground/root coordinate.
        var tree=DarkGardenRooms0255.FRUIT_TREES[0];var center=new BlockPos(tree[0],82,tree[1]);
        var centerBefore=level.getBlockState(center);
        var wood=DarkGardenDecor0255.block("dark_garden_deadwood").defaultBlockState();
        var joint=wood.setValue(DarkGardenDecor0255.Deadwood.BRANCHED,true);
        var blocked=new BlockPos(tree[0],81,tree[1]).west();var destination=blocked.above();
        var savedDestination=level.getBlockState(destination);
        var oldFruit=savedDestination.setValue(HangingFruitBlock.AGE,1);
        put(level,center,joint);put(level,blocked,oldFruit);put(level,destination,Blocks.CHEST.defaultBlockState());
        boolean refused=false;
        try{DarkGardenRooms0255.ensure(level);}catch(IllegalStateException expected){refused=true;}
        h.assertTrue(refused&&level.getBlockState(blocked).equals(oldFruit)
            &&level.getBlockState(destination).is(Blocks.CHEST)&&level.getBlockState(center).equals(joint),
            "Blocked raised-fruit migration partially wrote terrain, discarded a saved stage or erased a block entity");
        put(level,blocked,Blocks.AIR.defaultBlockState());put(level,destination,savedDestination);put(level,center,centerBefore);
        var migrationDrops=new HashSet<java.util.UUID>();
        level.getEntitiesOfClass(ItemEntity.class,new AABB(center).inflate(4)).forEach(e->migrationDrops.add(e.getUUID()));
        put(level,center,joint);
        for(Direction side:Direction.Plane.HORIZONTAL){
          var oldAt=new BlockPos(tree[0],81,tree[1]).relative(side);var next=oldAt.above();
          int age=side==Direction.EAST?3:side==Direction.NORTH?0:side==Direction.SOUTH?1:2;
          var fruit=level.getBlockState(next).setValue(HangingFruitBlock.AGE,age);
          put(level,oldAt,fruit);put(level,next,wood.setValue(BlockStateProperties.AXIS,side.getAxis()));
        }
        DarkGardenRooms0255.ensure(level);DarkGardenRooms0255.ensure(level);
        for(Direction side:Direction.Plane.HORIZONTAL){
          var oldAt=new BlockPos(tree[0],81,tree[1]).relative(side);var next=oldAt.above();
          int age=side==Direction.EAST?3:side==Direction.NORTH?0:side==Direction.SOUTH?1:2;
          var moved=level.getBlockState(next);
          h.assertTrue(level.getBlockState(oldAt).isAir()&&moved.is(FruitContent.podForFruit("dark_garden_fruit"))
              &&moved.getValue(HangingFruitBlock.AGE)==age&&moved.canSurvive(level,next),
              "Raised0257 fruit changed stage, duplicated its old cell or lost branch support: "+side);
        }
        var afterMigration=new HashSet<java.util.UUID>();
        level.getEntitiesOfClass(ItemEntity.class,new AABB(center).inflate(4)).forEach(e->afterMigration.add(e.getUUID()));
        h.assertTrue(afterMigration.equals(migrationDrops),"Moving the authored fruit dropped extra items");
        h.assertTrue(level.getBlockState(new BlockPos(tree[0],80,tree[1])).is(DarkGardenDecor0255.block("dark_garden_deadwood"))
            &&level.getBlockState(new BlockPos(tree[0],79,tree[1])).is(DarkGardenDecor0255.block("dark_garden_soil")),
            "Raising the tree lifted its roots off the ground");
        var pod=new BlockPos(DarkGardenRooms0255.FRUIT_TREES[0][0],82,DarkGardenRooms0255.FRUIT_TREES[0][1]).east();var original=level.getBlockState(pod);
        var arm=level.getBlockState(pod.above());
        try{
          put(level,pod.above(),Blocks.AIR.defaultBlockState());
          h.assertTrue(!original.canSurvive(level,pod),"Dark pod survives with no visible supporting arm");
          put(level,pod.above(),arm.setValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.AXIS,Direction.Axis.Z));
          h.assertTrue(!original.canSurvive(level,pod),"Dark pod accepts a disconnected perpendicular arm");
        }finally{put(level,pod.above(),arm);}
        h.assertTrue(original.is(FruitContent.podForFruit("dark_garden_fruit"))&&original.canSurvive(level,pod),"Bare Dark fruit is unsupported or uses the wrong actual fruit item");
        var before=new HashSet<java.util.UUID>();level.getEntitiesOfClass(ItemEntity.class,new AABB(pod).inflate(3)).forEach(e->before.add(e.getUUID()));
        h.assertTrue(((HangingFruitBlock)original.getBlock()).harvest(level,pod,original),"Real Dark Garden Fruit could not harvest");
        var drops=level.getEntitiesOfClass(ItemEntity.class,new AABB(pod).inflate(3),e->!before.contains(e.getUUID()));
        h.assertTrue(drops.size()==1&&drops.getFirst().getItem().is(ChaoGarden.item("dark_garden_fruit"))&&drops.getFirst().getItem().getCount()==1,"Dark bare fruit harvest created the wrong or duplicate item");
        DarkGardenRooms0255.ensure(level);
        h.assertTrue(level.getBlockState(pod).getValue(HangingFruitBlock.AGE)==0,"Scene preparation refilled a harvested Dark fruit");
        // Real growth after harvest still ripens this same native pod.
        var fruitBlock=(HangingFruitBlock)original.getBlock();
        for(int age=1;age<=3;age++){
          fruitBlock.performBonemeal(level,level.random,pod,level.getBlockState(pod));
          h.assertTrue(level.getBlockState(pod).getValue(HangingFruitBlock.AGE)==age&&level.getBlockState(pod).canSurvive(level,pod),"Attached Dark fruit lost native regrowth after harvest");
        }
        drops.forEach(ItemEntity::discard);put(level,pod,original);
        h.assertTrue(level.getBlockState(new BlockPos(0,120,21)).is(Blocks.BARRIER),"Dark outer cliff has no high invisible containment");
        for(var at:BlockPos.betweenClosed(-10,125,-10,10,125,10))h.assertTrue(level.getBlockState(at).isAir(),"Dark storm-cloud sky became a visible solid ceiling");
        cleanup.run();h.succeed();
      }catch(RuntimeException failure){cleanup.run();throw failure;}});
  }

  @GameTest(template="empty",batch="dark0255_preserve",timeoutTicks=300)
  public static void darkSceneKeepsOwnedProviderBodiesAndRejectsUnsupportedBlockEntitiesBeforeAnyWrite(GameTestHelper h){
    var level=dark(h);var p=TestConnections.makeSurvivalServerPlayer(h);p.teleportTo(level,-18.5,80.1,8.5,-125,0);p.hasChangedDimension();p.setNoGravity(true);
    var provider=new BlockPos(-6,80,10);var data=GardenRegistry.get(p.server);var key=GardenRegistry.Place.at(level,provider);
    var originals=new java.util.LinkedHashMap<BlockPos,net.minecraft.world.level.block.state.BlockState>();
    GardenTransporterBlock.footprint(provider).forEach(at->originals.put(at,level.getBlockState(at)));
    var child=ChaoGarden.CHAO.get().create(level);child.owner=p.getUUID();child.setNoAi(true);child.setNoGravity(true);child.setState(ChaoState.egg(4,true,false,0,new Random(255)));child.moveTo(-4.5,80,6.5,0,0);
    var egg=ChaoGarden.CHAO_EGG.get().create(level);egg.owner=p.getUUID();egg.setNoGravity(true);egg.setState(ChaoState.egg(5,false,true,0,new Random(2551)));egg.moveTo(-2.5,80,6.5,0,0);
    var goods=new ItemStack(Items.DIAMOND,3);var item=new ItemEntity(level,-.5,80,6.5,goods.copy());item.setNoGravity(true);item.setPickUpDelay(32767);
    var alias=GardenRegistry.Place.at(level,new BlockPos(-9,80,10));var probe=new BlockPos(-3,80,15);var obstruction=new BlockPos(-4,80,15);
    originals.put(probe,level.getBlockState(probe));originals.put(obstruction,level.getBlockState(obstruction));
    var leased=new ArrayList<net.minecraft.world.level.ChunkPos>();
    for(var at:List.of(provider,child.blockPosition(),egg.blockPosition(),item.blockPosition())){var c=new net.minecraft.world.level.ChunkPos(at);if(!level.getForcedChunks().contains(c.toLong())){level.setChunkForced(c.x,c.z,true);if(!leased.contains(c))leased.add(c);}}
    boolean[] done={false};Runnable cleanup=()->{if(done[0])return;done[0]=true;child.discard();egg.discard();item.discard();data.entries.remove(child.getUUID());data.links.remove(alias);data.nodeOwners.remove(alias);var registered=data.gardens.get(key);if(registered!=null&&registered.owner().equals(p.getUUID()))data.removeGarden(key);data.setDirty();originals.forEach((at,state)->put(level,at,state));for(var c:leased)level.setChunkForced(c.x,c.z,false);p.server.getPlayerList().remove(p);};h.runAtTickTime(299,cleanup);
    h.startSequence().thenWaitUntil(()->h.assertTrue(level.isPositionEntityTicking(provider)&&level.isPositionEntityTicking(item.blockPosition()),"Owned Dark fixture awaits leased entity-ticking chunks"))
      .thenExecute(()->{try{
        h.assertTrue(level.addFreshEntity(child)&&level.addFreshEntity(egg)&&level.addFreshEntity(item),"Dark fixture could not publish its real bodies");
      }catch(RuntimeException failure){cleanup.run();throw failure;}})
      .thenWaitUntil(()->h.assertTrue(level.getEntity(child.getUUID())==child&&level.getEntity(egg.getUUID())==egg&&level.getEntity(item.getUUID())==item,"Dark fixture awaits same-UUID body publication"))
      .thenExecute(()->{try{
        h.assertTrue(!data.gardens.containsKey(key),"Dark owned fixture refuses to replace a previous owner");
        for(int part=0;part<8;part++){var at=provider.offset(GardenTransporterBlock.offset(part));h.assertTrue(level.getBlockState(at).isAir(),"Dark provider fixture needs clear actual shore");put(level,at,GardenTransporterBlock.assembledState(Direction.EAST,part));}
        var garden=data.register(p,provider);h.assertTrue(data.bind(p,child,garden),"Dark provider could not bind its real Chao");data.nodeOwners.put(alias,p.getUUID());data.links.put(alias,key);
        var ledger=data.save(new CompoundTag(),level.registryAccess()).copy();var state=child.state().encode();var incubation=egg.state().encode();
        DarkGardenRooms0255.ensure(level);
        h.assertTrue(data.save(new CompoundTag(),level.registryAccess()).equals(ledger),"Dark preparation changed owner, provider, roster or Bell link data");
        for(int part=0;part<8;part++)h.assertTrue(level.getBlockState(provider.offset(GardenTransporterBlock.offset(part))).equals(GardenTransporterBlock.assembledState(Direction.EAST,part)),"Dark preparation changed a purchased provider part/facing");
        h.assertTrue(level.getEntity(child.getUUID())==child&&p.getUUID().equals(child.owner)&&state.equals(child.state().encode())
            &&level.getEntity(egg.getUUID())==egg&&p.getUUID().equals(egg.owner)&&incubation.equals(egg.state().encode())
            &&level.getEntity(item.getUUID())==item&&ItemStack.matches(goods,item.getItem()),"Dark preparation recreated or changed a Chao, egg or purchased goods");
        put(level,obstruction,Blocks.CHEST.defaultBlockState());var chest=level.getBlockEntity(obstruction);put(level,probe,Blocks.DIAMOND_BLOCK.defaultBlockState());boolean refused=false;
        try{DarkGardenRooms0255.ensure(level);}catch(IllegalStateException expected){refused=true;}
        h.assertTrue(refused&&level.getBlockEntity(obstruction)==chest&&level.getBlockState(probe).is(Blocks.DIAMOND_BLOCK)
            &&data.save(new CompoundTag(),level.registryAccess()).equals(ledger),"Obstructed Dark preparation partially rewrote terrain, block entity or ownership");
        cleanup.run();h.succeed();
      }catch(RuntimeException failure){cleanup.run();throw failure;}});
  }
  private DarkGardenGameTests0255(){}
}
