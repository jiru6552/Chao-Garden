package com.chaogarden.test;

import com.chaogarden.ChaoGarden;
import com.chaogarden.activities.ActivityContent;
import com.chaogarden.activities.ActivityManager;
import com.chaogarden.activities.GardenRegistry;
import com.chaogarden.activities.GardenTransporterBlock;
import com.chaogarden.core.ChaoIllness;
import com.chaogarden.core.ChaoState;
import com.chaogarden.entity.ChaoEggEntity;
import com.chaogarden.entity.ChaoEntity;
import com.chaogarden.hub.DarkGardenRooms0255;
import com.chaogarden.hub.HubContent;
import com.chaogarden.item.ChaoCarrierItem;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** Real Adventure item care, eight-part ownership and single-item recovery in the Dark realm. */
@GameTestHolder(ChaoGarden.MODID)
@PrefixGameTestTemplate(false)
public final class DarkGardenCareGameTests0255 {
  @GameTest(template="empty",batch="dark0255_care",timeoutTicks=600)
  public static void darkAdventureCareUsesItsOwnRealmAndEveryTerminalPartKeepsOneOwnedProvider(GameTestHelper h){
    var p=GardenWorldGameTests025.player(h,GameType.SURVIVAL);
    var source=ChaoGarden.CHAO.get().create(p.serverLevel());source.owner=p.getUUID();source.setNoAi(true);source.setNoGravity(true);
    var healthy=ChaoState.egg(3,true,false,0,new Random(255));
    for(int i=0;i<ChaoIllness.COUNT;i++)healthy.setIllness(i,100);
    source.setState(healthy);
    var carrier=new ItemStack(ChaoGarden.item("chao_carrier"));
    h.assertTrue(ChaoCarrierItem.capture(carrier,source,p),"Could not prepare the real held Dark-care Chao");
    var identity=ChaoCarrierItem.storedUUID(carrier);
    var level=p.server.getLevel(HubContent.DARK_GARDEN);
    h.assertTrue(level!=null&&HubContent.ensure(level),"Dark care dimension was not registered/built");
    var root=new BlockPos(-3,80,10);var eggGround=new BlockPos(-5,79,8);var childGround=new BlockPos(-1,79,8);var toyGround=new BlockPos(-5,79,11);
    var registry=GardenRegistry.get(p.server);var place=GardenRegistry.Place.at(level,root);
    var before=new LinkedHashMap<BlockPos,BlockState>();
    for(var at:GardenTransporterBlock.footprint(root))before.put(at,level.getBlockState(at));
    before.put(toyGround.above(),level.getBlockState(toyGround.above()));
    var forced=new ArrayList<ChunkPos>();var egg=new AtomicReference<ChaoEggEntity>();var child=new AtomicReference<ChaoEntity>();
    boolean[] done={false};String[] phase={"admission"};long[] started={h.getTick()};
    var providerItem=ChaoGarden.item("garden_block");var toyItem=ChaoGarden.item("chao_tv");
    Runnable cleanup=()->{
      if(done[0])return;done[0]=true;
      for(var actor:level.getEntitiesOfClass(ChaoEggEntity.class,DarkGardenRooms0255.BOUNDS,e->p.getUUID().equals(e.owner)))actor.discard();
      for(var actor:level.getEntitiesOfClass(ChaoEntity.class,DarkGardenRooms0255.BOUNDS,e->p.getUUID().equals(e.owner)))actor.discard();
      for(var drop:level.getEntitiesOfClass(ItemEntity.class,new AABB(root).inflate(6),e->e.getItem().is(providerItem)||e.getItem().is(toyItem)))drop.discard();
      for(var key:new ArrayList<>(registry.gardens.keySet()))if(p.getUUID().equals(registry.gardens.get(key).owner()))registry.removeGarden(key);
      registry.entries.remove(identity);registry.nodeOwners.remove(place);registry.links.remove(place);registry.links.values().removeIf(place::equals);registry.setDirty();
      before.forEach((at,state)->level.setBlock(at,state,Block.UPDATE_CLIENTS|Block.UPDATE_KNOWN_SHAPE));
      for(var chunk:forced)level.setChunkForced(chunk.x,chunk.z,false);
      GardenWorldGameTests025.finish(p);
    };
    Consumer<String> begin=next->{
      phase[0]=next;started[0]=h.getTick();long at=started[0];
      com.mojang.logging.LogUtils.getLogger().info("CHAO_DARK0255_CARE_PHASE_START phase={} tick={} budget=120",next,at);
      h.runAtTickTime(at+120,()->{if(!done[0]&&phase[0].equals(next)&&started[0]==at){
        String message="Dark care phase "+next+" exceeded120ticks; rootLoaded="+level.areEntitiesLoaded(ChunkPos.asLong(root))+" rootTicking="+level.isPositionEntityTicking(root)+" child="+(level.getEntity(identity)!=null);
        cleanup.run();h.fail(message);
      }});
    };
    h.runAtTickTime(599,()->{if(!done[0]){String current=phase[0];cleanup.run();h.fail("Dark native-care fixture exceeded its bounded phase budget: "+current);}});
    try{
      h.assertTrue(HubContent.enterHub(p)&&HubContent.visitDarkGarden(p),"Real Hub→Dark admission failed for care");p.hasChangedDimension();
      h.assertTrue(p.level()==level&&p.gameMode.getGameModeForPlayer()==GameType.ADVENTURE&&!p.mayBuild(),"Dark admission leaked ordinary build permission");
      for(var ground:List.of(root.below(),eggGround,childGround,toyGround)){
        h.assertTrue(DarkGardenRooms0255.insideField(ground.getX(),ground.getZ())&&level.getBlockState(ground).isFaceSturdy(level,ground,Direction.UP)&&level.getBlockState(ground.above()).isAir(),"Native-care fixture lacks clear authored Dark land at "+ground);
        var chunk=new ChunkPos(ground);if(!level.getForcedChunks().contains(chunk.toLong())){level.setChunkForced(chunk.x,chunk.z,true);forced.add(chunk);}
      }
      p.teleportTo(-2.5,80.1,9.5);begin.accept("actual-care-chunk-ticking");
      h.startSequence().thenWaitUntil(()->{
        for(var ground:List.of(root.below(),eggGround,childGround,toyGround))
          h.assertTrue(level.areEntitiesLoaded(ChunkPos.asLong(ground))&&level.isPositionEntityTicking(ground),"Waiting for actual leased Dark-care chunk publication/ticking");
      }).thenExecute(()->{try{
        begin.accept("egg-and-original-chao-publication");
        var eggItem=new ItemStack(ChaoGarden.item("normal_egg"));
        h.assertTrue(GardenWorldGameTests025.use(p,eggItem,eggGround).consumesAction()&&eggItem.isEmpty(),"Actual Adventure Dark egg placement failed or duplicated its item");
        h.assertTrue(GardenWorldGameTests025.use(p,carrier,childGround).consumesAction()&&carrier.isEmpty(),"Actual Adventure Dark carrier release failed or duplicated its item");
      }catch(RuntimeException failure){cleanup.run();throw failure;}}).thenWaitUntil(()->{
        var eggs=level.getEntitiesOfClass(ChaoEggEntity.class,new AABB(eggGround.above()).inflate(2),e->p.getUUID().equals(e.owner));
        h.assertTrue(eggs.size()==1&&level.getEntity(identity) instanceof ChaoEntity,"Native Dark egg and original Chao await actual UUID publication");
        egg.set(eggs.getFirst());egg.get().setNoGravity(true);child.set((ChaoEntity)level.getEntity(identity));child.get().setNoAi(true);child.get().setNoGravity(true);
      }).thenExecute(()->{try{
        var terminal=new ItemStack(providerItem);p.setYRot(180);
        h.assertTrue(GardenWorldGameTests025.use(p,terminal,root.below()).consumesAction()&&terminal.isEmpty(),"Native Dark2x2x2 terminal did not consume exactly one item");
        for(int part=0;part<8;part++){
          var at=root.offset(GardenTransporterBlock.offset(part));var state=level.getBlockState(at);
          h.assertTrue(DarkGardenRooms0255.insideField(at.getX(),at.getZ())&&DarkGardenRooms0255.ACTIVE_BOUNDS.contains(at.getCenter())&&state.is(ActivityContent.GARDEN.get())&&state.getValue(GardenTransporterBlock.PART)==part,"Native Dark terminal omitted or placed a part beyond its own care mask");
          h.assertTrue(ActivityManager.controller(level,at).equals(root),"Dark terminal child did not resolve the canonical provider");
        }
        var bell=new ItemStack(TestConnections.legacyGardenBell());
        h.assertTrue(GardenWorldGameTests025.use(p,bell,root.offset(1,1,1)).consumesAction(),"Dark Bell failed through an actual upper terminal part");
        var link=bell.getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag().getCompound("GardenLink");
        h.assertTrue(link.getString("Dimension").equals(HubContent.DARK_GARDEN.location().toString())&&link.getLong("Pos")==root.asLong()&&registry.gardens.get(place).owner().equals(p.getUUID()),"Dark Bell used the wrong dimension, child coordinate or owner");
        h.assertTrue(p.interactOn(child.get(),InteractionHand.MAIN_HAND).consumesAction()&&registry.entries.get(identity).garden.equals(place),"Native Dark Bell did not enroll its released original Chao");
        var toy=new ItemStack(toyItem);h.assertTrue(GardenWorldGameTests025.use(p,toy,toyGround).consumesAction()&&toy.isEmpty(),"Native Adventure Dark toy placement failed");
        var ledger=registry.save(new CompoundTag(),level.registryAccess()).copy();var childState=child.get().state().encode();var eggState=egg.get().state().encode();
        DarkGardenRooms0255.ensure(level);
        h.assertTrue(ledger.equals(registry.save(new CompoundTag(),level.registryAccess()))&&level.getEntity(identity)==child.get()&&childState.equals(child.get().state().encode())&&level.getEntity(egg.get().getUUID())==egg.get()&&eggState.equals(egg.get().state().encode()),"Dark scenery ensure changed real care identity, payload or owned membership");
        for(var at:GardenTransporterBlock.footprint(DarkGardenRooms0255.TRANSPORTER))h.assertTrue(!DarkGardenRooms0255.breakable(at,level.getBlockState(at)),"Authored Dark terminal part became recoverable scenery");
        p.setShiftKeyDown(true);
        try{h.assertTrue(GardenWorldGameTests025.use(p,ItemStack.EMPTY,toyGround.above()).consumesAction()&&level.getBlockState(toyGround.above()).isAir(),"Adventure Dark toy recovery failed");
          h.assertTrue(GardenWorldGameTests025.use(p,ItemStack.EMPTY,root.offset(1,1,1)).consumesAction(),"Adventure Dark terminal recovery did not canonicalize its upper part");}
        finally{p.setShiftKeyDown(false);}
        for(var at:GardenTransporterBlock.footprint(root))h.assertTrue(level.getBlockState(at).isAir(),"Dark recovery left an orphan terminal part");
        h.assertTrue(!registry.gardens.containsKey(place)&&!registry.entries.containsKey(identity)&&level.getEntity(identity)==child.get()&&p.getUUID().equals(child.get().owner),"Removing the purchased Dark provider left a phantom claim or replaced its owned Chao");
        h.assertTrue(p.gameMode.getGameModeForPlayer()==GameType.ADVENTURE&&!p.mayBuild(),"Native Dark item placement/recovery leaked build permission");
        begin.accept("single-terminal-item-publication");
      }catch(RuntimeException failure){cleanup.run();throw failure;}}).thenWaitUntil(()->{
        int count=level.getEntitiesOfClass(ItemEntity.class,new AABB(root).inflate(4),e->e.getItem().is(providerItem)).stream().mapToInt(e->e.getItem().getCount()).sum();
        h.assertTrue(count==1,"Native Dark terminal recovery has not published exactly one genuine item; count="+count);
      }).thenExecute(()->{cleanup.run();h.succeed();});
    }catch(RuntimeException failure){cleanup.run();throw failure;}
  }
  private DarkGardenCareGameTests0255(){}
}
