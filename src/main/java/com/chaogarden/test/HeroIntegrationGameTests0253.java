package com.chaogarden.test;

import com.chaogarden.ChaoGarden;
import com.chaogarden.activities.ActivityContent;
import com.chaogarden.activities.GardenRegistry;
import com.chaogarden.core.ChaoState;
import com.chaogarden.entity.ChaoEggEntity;
import com.chaogarden.entity.ChaoEntity;
import com.chaogarden.fruit.FruitContent;
import com.chaogarden.hub.GardenRooms025;
import com.chaogarden.hub.HeroGardenRooms0253;
import com.chaogarden.hub.HubContent;
import com.chaogarden.hub.HubRooms;
import com.chaogarden.hub.HubWorldData;
import com.chaogarden.hub.LobbyDecor025;
import com.chaogarden.item.ChaoCarrierItem;
import com.chaogarden.toys.ToyContent;
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
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.EntityTravelToDimensionEvent;
import net.neoforged.neoforge.event.entity.player.UseItemOnBlockEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** Real Hero admission, native Adventure care and saved-body integration. */
@GameTestHolder(ChaoGarden.MODID)
@PrefixGameTestTemplate(false)
public final class HeroIntegrationGameTests0253 {
  private static final int FLAGS=Block.UPDATE_CLIENTS|Block.UPDATE_KNOWN_SHAPE;
  private HeroIntegrationGameTests0253(){}
  private static ServerLevel hero(GameTestHelper h){
    var level=h.getLevel().getServer().getLevel(HubContent.HERO_GARDEN);
    h.assertTrue(level!=null&&HubContent.ensure(level),"Hero dimension or first build unavailable in the fresh dimension fixture");return level;
  }
  private static void adventure(GameTestHelper h,ServerPlayer p){
    h.assertTrue(p.gameMode.getGameModeForPlayer()==GameType.ADVENTURE&&!p.mayBuild(),"Hero care leaked building permission or changed Adventure mode");
  }
  private static void put(ServerLevel level,BlockPos pos,BlockState state){level.setBlock(pos,state,FLAGS);}
  private static void removeClaims(GardenRegistry data,ServerPlayer p){
    for(var at:new ArrayList<>(data.gardens.keySet()))if(p.getUUID().equals(data.gardens.get(at).owner()))data.removeGarden(at);
    data.entries.values().removeIf(entry->p.getUUID().equals(entry.owner));data.setDirty();
  }
  private static boolean safeHeroBody(ServerLevel level,Entity body){
    var at=body.blockPosition();return body.level()==level&&!body.isRemoved()&&HeroGardenRooms0253.insideField(at.getX(),at.getZ())
        &&body.getY()>=79&&level.noCollision(body)&&level.getFluidState(at).isEmpty()
        &&level.getBlockState(at.below()).is(LobbyDecor025.TURF.get());
  }

  @GameTest(template="empty",batch="hero0253_origin",timeoutTicks=160)
  public static void heroRoundTripAndDeniedTravelPreserveEveryOriginalModeAndHomeJournal(GameTestHelper h){
    for(var original:GameType.values()){
      var p=GardenWorldGameTests025.player(h,original);var home=p.serverLevel();var origin=p.position();
      var keepsake=new ItemStack(Items.DIAMOND,7);p.getInventory().setItem(8,keepsake.copy());
      try{
        h.assertTrue(HubContent.enterHub(p),"Hub admission failed for original mode "+original);p.hasChangedDimension();
        var journal=HubWorldData.get(p.server).visit(p.getUUID()).copy();
        h.assertTrue(journal.getInt("GameMode")==original.getId(),"Hub journal did not retain original mode "+original);
        if(original==GameType.SURVIVAL){
          Consumer<EntityTravelToDimensionEvent> deny=event->{if(event.getEntity()==p&&event.getDimension().equals(HubContent.HERO_GARDEN))event.setCanceled(true);};
          NeoForge.EVENT_BUS.addListener(deny);
          try{h.assertTrue(!HubContent.visitHeroGarden(p),"Hero entry ignored canceled native dimension travel");}
          finally{NeoForge.EVENT_BUS.unregister(deny);}
          h.assertTrue(p.level().dimension().equals(HubContent.HUB)&&journal.equals(HubWorldData.get(p.server).visit(p.getUUID())),"Denied Hero travel stranded the visitor or changed the home journal");adventure(h,p);
        }
        h.assertTrue(HubContent.visitHeroGarden(p)&&p.level().dimension().equals(HubContent.HERO_GARDEN),"Hero admission failed for original mode "+original);p.hasChangedDimension();adventure(h,p);
        p.setGameMode(GameType.SURVIVAL);HubContent.tickPlayer(p);adventure(h,p);
        h.assertTrue(journal.equals(HubWorldData.get(p.server).visit(p.getUUID())),"Hero admission or mode repair overwrote the original home journal");
        h.assertTrue(HubContent.returnToHub(p)&&p.level().dimension().equals(HubContent.HUB),"Hero return did not reach the Hub");p.hasChangedDimension();adventure(h,p);
        var feet=p.blockPosition();h.assertTrue(p.getY()>88&&p.serverLevel().getBlockState(feet.below()).isFaceSturdy(p.serverLevel(),feet.below(),Direction.UP)
            &&p.serverLevel().noCollision(p)&&!HubContent.inHeroDoor(p),"Hero return did not land safely beside its elevated balcony gate");
        h.assertTrue(journal.equals(HubWorldData.get(p.server).visit(p.getUUID())),"Hero-to-Hub return replaced the real origin");
        h.assertTrue(HubContent.returnHome(p)&&p.level()==home&&p.position().distanceTo(origin)<5
            &&p.gameMode.getGameModeForPlayer()==original&&!HubContent.hasReturnJournal(p)
            &&ItemStack.matches(p.getInventory().getItem(8),keepsake),"Hero round trip changed original world/mode/inventory or retained a stale journal: "+original);
      }finally{GardenWorldGameTests025.finish(p);}
    }
    h.succeed();
  }

  @GameTest(template="empty",batch="hero0253_walking",timeoutTicks=180)
  public static void westFacingHeroGateUsesRealCrossingAndRearmsForItsReturn(GameTestHelper h){
    var p=GardenWorldGameTests025.player(h,GameType.SURVIVAL);var home=p.serverLevel();var origin=p.position();
    boolean[] done={false};Runnable cleanup=()->{if(done[0])return;done[0]=true;GardenWorldGameTests025.finish(p);};h.runAtTickTime(179,cleanup);
    try{
      h.assertTrue(HubContent.enterHub(p),"Hub unavailable for actual Hero walking route");p.hasChangedDimension();
      var journal=HubWorldData.get(p.server).visit(p.getUUID()).copy();var gate=HubRooms.HERO_DOOR;
      p.teleportTo(gate.getX()+1.5,gate.getY()+.1,gate.getZ()+.5);
      h.assertTrue(HubContent.inHeroDoor(p),"South-facing gate did not use X as its lateral coordinate");
      p.teleportTo(gate.getX()+.5,gate.getY()+.1,gate.getZ()+2.5);
      h.assertTrue(!HubContent.inHeroDoor(p),"South-facing gate accepted an approach two blocks in front of its plane");HubContent.tickPlayer(p);
      h.startSequence().thenIdle(45).thenExecute(()->{try{
        p.move(MoverType.SELF,new Vec3(0,0,-1.7));h.assertTrue(HubContent.inHeroDoor(p),"Real movement could not enter the south-facing Hero aperture");HubContent.tickPlayer(p);p.hasChangedDimension();
        h.assertTrue(p.level().dimension().equals(HubContent.HERO_GARDEN),"Walking through Hero gate returned home or remained in Hub");adventure(h,p);
        h.assertTrue(journal.equals(HubWorldData.get(p.server).visit(p.getUUID())),"Walking Hero entry changed original journal");
        var exit=HeroGardenRooms0253.EXIT_PORTAL;p.teleportTo(exit.getX()-1.5,exit.getY()+.1,exit.getZ()+.5);HubContent.tickPlayer(p);
        h.assertTrue(!HubContent.inHeroReturn(p),"Hero return gate has an oversized depth hitbox");
      }catch(RuntimeException failure){cleanup.run();throw failure;}}).thenIdle(45).thenExecute(()->{try{
        p.move(MoverType.SELF,new Vec3(1.7,0,0));h.assertTrue(HubContent.inHeroReturn(p),"Real movement could not enter the Hero return aperture");HubContent.tickPlayer(p);p.hasChangedDimension();
        h.assertTrue(p.level().dimension().equals(HubContent.HUB)&&p.getY()>88&&!HubContent.inHeroDoor(p),"Walking Hero return missed the elevated balcony landing");
        h.assertTrue(Math.abs(net.minecraft.util.Mth.wrapDegrees(p.getYRot()-HubRooms.HERO_FACING.toYRot()))<.001,"Hero return did not face outward from the flipped gate");
        HubContent.tickPlayer(p);h.assertTrue(p.level().dimension().equals(HubContent.HUB)&&journal.equals(HubWorldData.get(p.server).visit(p.getUUID())),"Arrival bounced back through Hero gate or lost HomeReturn");
        h.assertTrue(HubContent.returnHome(p)&&p.level()==home&&p.position().distanceTo(origin)<5&&!HubContent.hasReturnJournal(p),"Walking Hero journey could not finish at the original home");cleanup.run();h.succeed();
      }catch(RuntimeException failure){cleanup.run();throw failure;}});
    }catch(RuntimeException failure){cleanup.run();throw failure;}
  }

  @GameTest(template="empty",batch="hero0253_care",timeoutTicks=240)
  public static void nativeHeroCareKeepsEggChaoProviderAndPurchasesThroughRepeatedEnsure(GameTestHelper h){
    var p=GardenWorldGameTests025.player(h,GameType.ADVENTURE);var source=ChaoGarden.CHAO.get().create(p.serverLevel());
    source.owner=p.getUUID();source.setNoAi(true);source.setNoGravity(true);source.setState(ChaoState.egg(3,true,false,0,new Random(253)));
    var carrier=new ItemStack(ChaoGarden.item("chao_carrier"));h.assertTrue(ChaoCarrierItem.capture(carrier,source,p),"Could not prepare actual held Chao");var identity=ChaoCarrierItem.storedUUID(carrier);
    var level=hero(h);var registry=GardenRegistry.get(p.server);var eggRef=new AtomicReference<ChaoEggEntity>();var childRef=new AtomicReference<ChaoEntity>();
    var toyGround=new BlockPos(5,79,1);var seedGround=new BlockPos(5,79,4);var providerGround=new BlockPos(8,79,4);
    var outerGround=new BlockPos(26,HeroGardenRooms0253.surface(26,0),0);var deniedGround=new BlockPos(40,79,0);
    var before=new LinkedHashMap<BlockPos,BlockState>();for(var at:List.of(toyGround.above(),seedGround.above(),providerGround.above(),outerGround.above(),deniedGround,deniedGround.above()))before.put(at,level.getBlockState(at));
    boolean[] done={false};Runnable cleanup=()->{if(done[0])return;done[0]=true;
      if(eggRef.get()!=null)eggRef.get().discard();if(childRef.get()!=null)childRef.get().discard();
      for(var egg:level.getEntitiesOfClass(ChaoEggEntity.class,HeroGardenRooms0253.BOUNDS,e->p.getUUID().equals(e.owner)))egg.discard();
      for(var child:level.getEntitiesOfClass(ChaoEntity.class,HeroGardenRooms0253.BOUNDS,e->p.getUUID().equals(e.owner)))child.discard();
      for(var drop:level.getEntitiesOfClass(ItemEntity.class,new AABB(toyGround.above()).inflate(3),e->e.getItem().is(ChaoGarden.item("chao_tv"))))drop.discard();
      removeClaims(registry,p);before.forEach((at,state)->put(level,at,state));GardenWorldGameTests025.finish(p);
    };h.runAtTickTime(239,cleanup);
    try{
      h.assertTrue(HubContent.enterHub(p)&&HubContent.visitHeroGarden(p),"Hero unavailable for native Adventure care");p.hasChangedDimension();
      for(var ground:List.of(toyGround,seedGround,providerGround,outerGround))h.assertTrue(level.getBlockState(ground).is(LobbyDecor025.TURF.get())&&level.getBlockState(ground.above()).isAir(),"Care fixture lacks clear authored Hero lawn at "+ground);
      p.teleportTo(7.5,80.1,3.5);var eggItem=new ItemStack(ChaoGarden.item("normal_egg"));
      h.assertTrue(GardenWorldGameTests025.use(p,eggItem,new BlockPos(7,79,1)).consumesAction()&&eggItem.isEmpty(),"Native Hero egg placement failed");
      h.assertTrue(GardenWorldGameTests025.use(p,carrier,new BlockPos(9,79,1)).consumesAction()&&carrier.isEmpty(),"Native Hero carrier release failed");adventure(h,p);
      h.startSequence().thenWaitUntil(()->{
        var found=level.getEntitiesOfClass(ChaoEggEntity.class,new AABB(6,79,0,9,82,3),e->p.getUUID().equals(e.owner));
        h.assertTrue(found.size()==1&&level.getEntity(identity) instanceof ChaoEntity,"Native Hero egg/held Chao awaits genuine entity publication");eggRef.set(found.getFirst());eggRef.get().setNoGravity(true);childRef.set((ChaoEntity)level.getEntity(identity));
      }).thenExecute(()->{try{
        var toy=new ItemStack(ChaoGarden.item("chao_tv"));h.assertTrue(GardenWorldGameTests025.use(p,toy,toyGround).consumesAction()&&toy.isEmpty(),"Native Hero toy placement failed");
        var seed=new ItemStack(ChaoGarden.item("garden_fruit_seed"));h.assertTrue(GardenWorldGameTests025.use(p,seed,seedGround).consumesAction()&&seed.isEmpty()&&level.getBlockState(seedGround.above()).is(FruitContent.saplingForFruit("garden_fruit")),"Native Hero fruit seed planting failed");
        p.teleportTo(8.5,80.1,6.5);var providerItem=new ItemStack(ChaoGarden.item("garden_block"));
        h.assertTrue(GardenWorldGameTests025.use(p,providerItem,providerGround).consumesAction()&&providerItem.isEmpty(),"Native Hero provider placement failed");
        var bell=new ItemStack(TestConnections.legacyGardenBell());h.assertTrue(GardenWorldGameTests025.use(p,bell,providerGround.above()).consumesAction(),"Native Hero Garden Bell could not link a provider");
        var place=GardenRegistry.Place.at(level,providerGround.above());var link=bell.getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag().getCompound("GardenLink");
        h.assertTrue(link.getString("Dimension").equals(HubContent.HERO_GARDEN.location().toString())&&link.getLong("Pos")==providerGround.above().asLong()
            &&registry.gardens.get(place).owner().equals(p.getUUID()),"Hero Bell/provider used Neutral dimension or changed keeper ownership");
        p.teleportTo(10.5,80.1,2.5);h.assertTrue(p.interactOn(childRef.get(),InteractionHand.MAIN_HAND).consumesAction()&&registry.entries.get(identity).garden.equals(place),"Native Bell could not assign the released original Chao to its Hero provider");adventure(h,p);
        var toyState=level.getBlockState(toyGround.above());var seedState=level.getBlockState(seedGround.above());var providerState=level.getBlockState(providerGround.above());
        var ledger=registry.save(new CompoundTag(),p.registryAccess());var childState=childRef.get().state().encode();var eggState=eggRef.get().state().encode();
        HeroGardenRooms0253.ensure(level);HeroGardenRooms0253.ensure(level);
        h.assertTrue(toyState.equals(level.getBlockState(toyGround.above()))&&seedState.equals(level.getBlockState(seedGround.above()))&&providerState.equals(level.getBlockState(providerGround.above()))
            &&ledger.equals(registry.save(new CompoundTag(),p.registryAccess())),"Repeated Hero ensure changed actual care props, provider registration or membership ledger");
        h.assertTrue(level.getEntity(identity)==childRef.get()&&p.getUUID().equals(childRef.get().owner)&&childState.equals(childRef.get().state().encode())
            &&level.getEntity(eggRef.get().getUUID())==eggRef.get()&&p.getUUID().equals(eggRef.get().owner)&&eggState.equals(eggRef.get().state().encode()),"Hero ensure replaced or mutated a native released Chao/egg");
        p.teleportTo(25.5,outerGround.getY()+1.1,2.5);var highToy=new ItemStack(ChaoGarden.item("chao_tv"));
        h.assertTrue(HeroGardenRooms0253.insideField(26,0)&&!GardenRooms025.insideField(26,0)&&GardenWorldGameTests025.use(p,highToy,outerGround).consumesAction()
            &&level.getBlockState(outerGround.above()).is(ToyContent.block("chao_tv")),"Hero care incorrectly reused the smaller Neutral footprint");
        put(level,deniedGround,Blocks.STONE.defaultBlockState());p.teleportTo(40.5,80.1,2.5);var denied=new ItemStack(ChaoGarden.item("chao_tv"));
        h.assertTrue(!GardenWorldGameTests025.use(p,denied,deniedGround).consumesAction()&&denied.getCount()==1&&level.getBlockState(deniedGround.above()).isAir(),"Hero care allowed placement outside its own bounds");
        p.teleportTo(5.5,80.1,3.5);p.setShiftKeyDown(true);
        h.assertTrue(GardenWorldGameTests025.use(p,ItemStack.EMPTY,toyGround.above()).consumesAction()&&level.getBlockState(toyGround.above()).isAir(),"Adventure keeper could not recover a Hero toy by native sneak use");p.setShiftKeyDown(false);adventure(h,p);
        cleanup.run();h.succeed();
      }catch(RuntimeException failure){cleanup.run();throw failure;}});
    }catch(RuntimeException failure){cleanup.run();throw failure;}
  }

  @GameTest(template="empty",batch="hero0253_cancel",timeoutTicks=120)
  public static void nativeHeroItemAndPlacementCancellationCannotConsumeGoodsOrLeakBuildAbility(GameTestHelper h){
    var p=GardenWorldGameTests025.player(h,GameType.SURVIVAL);
    try{
      h.assertTrue(HubContent.enterHub(p)&&HubContent.visitHeroGarden(p),"Hero unavailable for native cancellation checks");p.hasChangedDimension();p.teleportTo(5.5,80.1,3.5);
      var level=p.serverLevel();var ground=new BlockPos(5,79,1);h.assertTrue(level.getBlockState(ground.above()).isAir(),"Cancellation fixture needs clear authored Hero turf");
      var toy=new ItemStack(ChaoGarden.item("chao_tv"));var before=toy.copy();boolean[] useSeen={false},placeSeen={false};
      Consumer<UseItemOnBlockEvent> denyUse=event->{if(event.getPlayer()==p&&event.getUsePhase()==UseItemOnBlockEvent.UsePhase.ITEM_AFTER_BLOCK){useSeen[0]=true;event.cancelWithResult(net.minecraft.world.ItemInteractionResult.FAIL);}};
      NeoForge.EVENT_BUS.addListener(denyUse);
      try{h.assertTrue(!GardenWorldGameTests025.use(p,toy,ground).consumesAction(),"Hero bypassed native item-use cancellation");}finally{NeoForge.EVENT_BUS.unregister(denyUse);}
      h.assertTrue(useSeen[0]&&ItemStack.matches(toy,before)&&level.getBlockState(ground.above()).isAir(),"Canceled Hero item use changed goods/world");adventure(h,p);
      Consumer<BlockEvent.EntityPlaceEvent> denyPlace=event->{if(event.getEntity()==p){placeSeen[0]=true;h.assertTrue(p.mayBuild(),"Hero placement did not run inside its scoped native permission");event.setCanceled(true);}};
      NeoForge.EVENT_BUS.addListener(denyPlace);
      try{h.assertTrue(!GardenWorldGameTests025.use(p,toy,ground).consumesAction(),"Hero bypassed native block-placement cancellation");}finally{NeoForge.EVENT_BUS.unregister(denyPlace);}
      h.assertTrue(placeSeen[0]&&ItemStack.matches(toy,before)&&level.getBlockState(ground.above()).isAir(),"Canceled Hero block placement consumed goods or changed terrain");adventure(h,p);
    }finally{GardenWorldGameTests025.finish(p);}
    h.succeed();
  }

  @GameTest(template="empty",batch="hero0253_wildlife",timeoutTicks=120)
  public static void heroRejectsNativeWildlifeAndHostilesWithoutChangingOrdinarySpawns(GameTestHelper h){
    var level=hero(h);
    for(var type:List.of(EntityType.SHEEP,EntityType.PIG,EntityType.ZOMBIE)){
      var mob=type.create(level);h.assertTrue(mob!=null,"Could not create native Hero wildlife fixture");mob.setPos(5.5,80.1,1.5);
      h.assertTrue(!level.addFreshEntity(mob),"Hero admitted native sheep/pig/zombie spawn: "+type);
    }
    var settings=level.getBiome(BlockPos.containing(HeroGardenRooms0253.SPAWN)).value().getMobSettings();
    for(var category:List.of(MobCategory.CREATURE,MobCategory.MONSTER,MobCategory.AMBIENT,MobCategory.WATER_CREATURE))h.assertTrue(settings.getMobs(category).isEmpty(),"Hero biome has a native spawn list: "+category);
    var ordinary=EntityType.SHEEP.create(h.getLevel());h.assertTrue(ordinary!=null&&h.getLevel().addFreshEntity(ordinary),"Hero wildlife restriction leaked into the ordinary world");ordinary.discard();h.succeed();
  }

  @GameTest(template="empty",batch="hero0253_rescue",timeoutTicks=240)
  public static void actualHeroTicksRescueKeeperChaoEggAndGoodsWithoutReplacingIdentity(GameTestHelper h){
    var level=hero(h);var p=GardenWorldGameTests025.player(h,GameType.SURVIVAL);var registry=GardenRegistry.get(p.server);
    var child=ChaoGarden.CHAO.get().create(level);child.owner=p.getUUID();child.setNoAi(true);child.setNoGravity(true);child.setState(ChaoState.egg(4,true,false,0,new Random(2531)));child.moveTo(9.5,80,2.5,0,0);
    var egg=ChaoGarden.CHAO_EGG.get().create(level);egg.owner=p.getUUID();egg.setNoGravity(true);egg.setState(ChaoState.egg(5,false,true,0,new Random(2532)));egg.moveTo(12.5,80,2.5,0,0);
    var goods=new ItemStack(Items.DIAMOND,3);var item=new ItemEntity(level,15.5,80,2.5,goods.copy());item.setNoGravity(true);item.setPickUpDelay(32767);
    var provider=new BlockPos(8,80,4);var oldProvider=level.getBlockState(provider);var forced=new ArrayList<net.minecraft.world.level.ChunkPos>();
    boolean[] done={false};Runnable cleanup=()->{if(done[0])return;done[0]=true;child.discard();egg.discard();item.discard();removeClaims(registry,p);put(level,provider,oldProvider);for(var chunk:forced)level.setChunkForced(chunk.x,chunk.z,false);GardenWorldGameTests025.finish(p);};h.runAtTickTime(239,cleanup);
    try{
      h.assertTrue(HubContent.enterHub(p)&&HubContent.visitHeroGarden(p),"Hero unavailable for real escape rescue");p.hasChangedDimension();
      for(var at:List.of(child.blockPosition(),egg.blockPosition(),item.blockPosition(),new BlockPos(29,65,4))){var chunk=new net.minecraft.world.level.ChunkPos(at);if(!level.getForcedChunks().contains(chunk.toLong())){level.setChunkForced(chunk.x,chunk.z,true);forced.add(chunk);}}
      h.assertTrue(level.addFreshEntity(child)&&level.addFreshEntity(egg)&&level.addFreshEntity(item),"Could not publish actual Hero rescue bodies");
      put(level,provider,ActivityContent.GARDEN.get().defaultBlockState());var garden=registry.register(p,provider);
      h.startSequence().thenWaitUntil(()->h.assertTrue(level.getEntity(child.getUUID())==child&&level.getEntity(egg.getUUID())==egg&&level.getEntity(item.getUUID())==item,"Hero rescue awaits genuine UUID tracking"))
      .thenExecute(()->{try{
        h.assertTrue(registry.bind(p,child,garden),"Could not bind rescue Chao to its actual Hero provider");
        for(var actor:List.of(child,egg,item))actor.setPos(29.5,65,4.5);
        p.teleportTo(29.5,65,4.5);HubContent.tickPlayer(p);adventure(h,p);
        h.assertTrue(safeHeroBody(level,p)&&HubContent.hasReturnJournal(p),"Keeper escape rescue used a different realm or lost its home journal");
      }catch(RuntimeException failure){cleanup.run();throw failure;}}).thenWaitUntil(()->h.assertTrue(safeHeroBody(level,child)&&safeHeroBody(level,egg)&&safeHeroBody(level,item),"Actual Hero tick rescue has not restored all out-of-bounds/void bodies"))
      .thenExecute(()->{try{
        h.assertTrue(level.getEntity(child.getUUID())==child&&p.getUUID().equals(child.owner)&&level.getEntity(egg.getUUID())==egg&&p.getUUID().equals(egg.owner)
            &&level.getEntity(item.getUUID())==item&&ItemStack.matches(item.getItem(),goods),"Hero rescue replaced an existing UUID, keeper or purchased goods");
        var entry=registry.entries.get(child.getUUID());h.assertTrue(entry!=null&&entry.garden.equals(garden.place())&&entry.last.equals(GardenRegistry.Place.at(level,child.blockPosition()))
            &&entry.last.dimension().equals(HubContent.HERO_GARDEN.location().toString()),"Hero rescue recorded Neutral location or changed provider membership");
        var actors=List.<Entity>of(p,child,egg,item);for(int i=0;i<actors.size();i++)for(int j=i+1;j<actors.size();j++)h.assertTrue(!actors.get(i).getBoundingBox().intersects(actors.get(j).getBoundingBox()),"Hero rescue stacked two bodies on one landing");
        cleanup.run();h.succeed();
      }catch(RuntimeException failure){cleanup.run();throw failure;}});
    }catch(RuntimeException failure){cleanup.run();throw failure;}
  }
}
