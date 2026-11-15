package com.chaogarden.test;

import com.chaogarden.ChaoGarden;
import com.chaogarden.core.ChaoState;
import com.chaogarden.entity.ChaoEntity;
import com.chaogarden.entity.ChaoEntity.Behavior;
import com.chaogarden.fruit.FruitContent;
import com.chaogarden.fruit.HangingFruitBlock;
import com.chaogarden.interaction.ChaoFruitSeekingGoal;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestGenerator;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestFunction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** Real orchard work and deterministic failed-path counts, with no item invention. */
@GameTestHolder("chao_maintenance83")
@PrefixGameTestTemplate(false)
public final class MaintenanceBehaviorGameTests083 {
  @GameTestGenerator public static Collection<TestFunction> tests(){return List.of(
      new TestFunction("maintenance83_orchard","baby_tree_shake_stops_after_out_of_range_shove","chao_maintenance83:activity_test",80,0,true,MaintenanceBehaviorGameTests083::babyTreeShakeStopsAfterOutOfRangeShove),
      new TestFunction("maintenance83_orchard","adult_tree_shake_stops_after_out_of_range_shove","chao_maintenance83:activity_test",80,0,true,MaintenanceBehaviorGameTests083::adultTreeShakeStopsAfterOutOfRangeShove),
      new TestFunction("maintenance83_orchard","baby_tree_shake_retains_four_real_harvests_in_range","chao_maintenance83:activity_test",80,0,true,MaintenanceBehaviorGameTests083::babyTreeShakeRetainsFourRealHarvestsInRange),
      new TestFunction("maintenance83_orchard","adult_tree_shake_retains_four_real_harvests_in_range","chao_maintenance83:activity_test",80,0,true,MaintenanceBehaviorGameTests083::adultTreeShakeRetainsFourRealHarvestsInRange),
      new TestFunction("maintenance83_orchard","four_pods_on_one_unreachable_tree_probe_four_sides_only_once","chao_maintenance83:activity_test",30,0,true,MaintenanceBehaviorGameTests083::fourPodsOnOneUnreachableTreeProbeFourSidesOnlyOnce),
      new TestFunction("maintenance83_orchard","displaced_tree_shake_does_not_load_its_stale_remote_chunk","chao_maintenance83:activity_test",30,0,true,MaintenanceBehaviorGameTests083::displacedTreeShakeDoesNotLoadItsStaleRemoteChunk));}
  private record Orchard(BlockPos root,List<BlockPos> pods) {}
  private static Orchard orchard(GameTestHelper h) {
    for(int x=1;x<=17;x++)for(int z=1;z<=14;z++) {
      h.setBlock(new BlockPos(x,0,z),Blocks.STONE);
      for(int y=1;y<=10;y++)h.setBlock(new BlockPos(x,y,z),Blocks.AIR);
    }
    BlockPos root=h.absolutePos(new BlockPos(9,1,7));
    h.getLevel().setBlock(root.below(),Blocks.DIRT.defaultBlockState(),3);
    h.assertTrue(FruitContent.growTree(h.getLevel(),root,"garden_fruit"),"Real orchard grows");
    List<BlockPos> pods=FruitContent.podPositions(h.getLevel(),FruitContent.crownPosition(root),"garden_fruit");
    for(BlockPos p:pods)h.getLevel().setBlock(p,h.getLevel().getBlockState(p).setValue(HangingFruitBlock.AGE,3),3);
    h.assertTrue(pods.size()==4,"Four ripe pods share the same orchard root");
    return new Orchard(root,pods);
  }
  private static ChaoEntity chao(GameTestHelper h,Orchard orchard,boolean adult) {
    ChaoEntity c=ChaoGarden.CHAO.get().create(h.getLevel());h.assertTrue(c!=null,"Chao creates");
    ChaoState state=new ChaoState();state.adult=adult;state.hunger=15;state.energy=100;
    state.points[ChaoState.RUN]=667;c.setState(state);c.setNoAi(false);
    c.moveTo(Vec3.atBottomCenterOf(orchard.root().west()));c.setOnGround(true);
    h.getLevel().addFreshEntity(c);c.goalSelector.removeAllGoals(g->true);c.targetSelector.removeAllGoals(g->true);
    return c;
  }
  private static void shake(GameTestHelper h,boolean adult,boolean interrupt) {
    Orchard orchard=orchard(h);ChaoEntity c=chao(h,orchard,adult);
    ChaoFruitSeekingGoal goal=new ChaoFruitSeekingGoal(c);AtomicBoolean started=new AtomicBoolean();
    h.runAtTickTime(5,()->{
      h.assertTrue(c.onGround()&&!c.isNoGravity(),"Real gravity keeps the Chao supported before ordinary ground navigation");
      h.assertTrue(goal.canUse(),"Reachable real ripe tree is selected");
      goal.start();goal.tick();started.set(true);
      h.assertTrue(c.getBehavior()==Behavior.SHAKE_TREE,"Chao starts shaking from the real west stand");
    });
    h.onEachTick(()->{if(started.get())goal.tick();});
    if(interrupt)h.runAtTickTime(10,()->{
      c.moveTo(Vec3.atBottomCenterOf(orchard.root().west(7)));c.setOnGround(true);
      h.assertTrue(!goal.canContinueToUse(),"Out-of-range shove interrupts active orchard work");
      // The transaction also checks reach itself, rather than trusting selector order.
      goal.tick();goal.stop();started.set(false);
      h.assertTrue(c.getBehavior()!=Behavior.SHAKE_TREE,"Interrupted shaking returns to idle");
    });
    h.runAtTickTime(55,()->{
      for(BlockPos p:orchard.pods())h.assertTrue(h.getLevel().getBlockState(p).getValue(HangingFruitBlock.AGE)==(interrupt?3:0),
          interrupt?"Remote tree keeps each real ripe pod":"In-range shaking harvests each real ripe pod");
      int drops=h.getLevel().getEntitiesOfClass(ItemEntity.class,new AABB(orchard.root()).inflate(8),
          e->e.getItem().is(ChaoGarden.item("garden_fruit"))).stream().mapToInt(e->e.getItem().getCount()).sum();
      h.assertTrue(drops==(interrupt?0:4),"No remote reward, or exactly one real fruit per harvested pod");
      h.assertTrue(c.state().fruitCount==0&&c.getMainHandItem().isEmpty(),"Tree work does not invent a held item or meal");
      goal.stop();h.succeed();
    });
  }
  public static void babyTreeShakeStopsAfterOutOfRangeShove(GameTestHelper h){shake(h,false,true);}
  public static void adultTreeShakeStopsAfterOutOfRangeShove(GameTestHelper h){shake(h,true,true);}
  public static void babyTreeShakeRetainsFourRealHarvestsInRange(GameTestHelper h){shake(h,false,false);}
  public static void adultTreeShakeRetainsFourRealHarvestsInRange(GameTestHelper h){shake(h,true,false);}

  private static final class PathObservedChao extends ChaoEntity {
    private final FailedPathNavigation paths;
    private PathObservedChao(ServerLevel world) {
      super(ChaoGarden.CHAO.get(),world);paths=new FailedPathNavigation(this,world);navigation=paths;
    }
  }
  private static final class FailedPathNavigation extends GroundPathNavigation {
    private int attempts;
    private FailedPathNavigation(ChaoEntity chao,ServerLevel world){super(chao,world);}
    @Override public Path createPath(BlockPos at,int accuracy){attempts++;return null;}
  }
  public static void fourPodsOnOneUnreachableTreeProbeFourSidesOnlyOnce(GameTestHelper h) {
    Orchard orchard=orchard(h);PathObservedChao c=new PathObservedChao(h.getLevel());
    ChaoState state=new ChaoState();state.adult=true;state.hunger=15;state.energy=100;c.setState(state);
    c.moveTo(Vec3.atBottomCenterOf(orchard.root().west(3)));c.setNoGravity(true);c.setOnGround(true);
    h.getLevel().addFreshEntity(c);c.goalSelector.removeAllGoals(g->true);c.targetSelector.removeAllGoals(g->true);
    h.runAtTickTime(5,()->{
      ChaoFruitSeekingGoal goal=new ChaoFruitSeekingGoal(c);
      h.assertTrue(!goal.canUse(),"An unreachable tree is never selected for remote work");
      h.assertTrue(c.paths.attempts==4,"Four-pod orchard probes four unreachable sides once, not sixteen: "+c.paths.attempts);
      for(BlockPos p:orchard.pods())h.assertTrue(h.getLevel().getBlockState(p).getValue(HangingFruitBlock.AGE)==3,"Search leaves all fruit intact");
      h.succeed();
    });
  }
  public static void displacedTreeShakeDoesNotLoadItsStaleRemoteChunk(GameTestHelper h) {
    Orchard orchard=orchard(h);ChaoEntity c=chao(h,orchard,true);
    ChaoFruitSeekingGoal goal=new ChaoFruitSeekingGoal(c);
    h.runAtTickTime(5,()->{
      h.assertTrue(c.onGround()&&!c.isNoGravity(),"Real gravity keeps the Chao supported before ordinary ground navigation");
      h.assertTrue(goal.canUse(),"Reachable orchard starts the real goal");goal.start();goal.tick();
      h.assertTrue(c.getBehavior()==Behavior.SHAKE_TREE,"Fixture reached active shaking");
      h.assertTrue(goal.canContinueToUse(),"Active in-range work is otherwise eligible before the stale-target change");
      // The real GameTest structure is force-loaded. Move only the goal's stale
      // target far outside it to exercise the same post-teleport continuation
      // without depending on asynchronous structure-chunk unloading.
      BlockPos absent=orchard.root().offset(160000,0,160000);
      h.assertTrue(!h.getLevel().hasChunkAt(absent),"Stale distant target is initially unloaded");
      try {
        var field=ChaoFruitSeekingGoal.class.getDeclaredField("tree");field.setAccessible(true);field.set(goal,absent);
      } catch(ReflectiveOperationException e){throw new AssertionError("Cannot construct stale-target fixture",e);}
      h.assertTrue(!goal.canContinueToUse(),"Displaced active work stops before reading its stale tree block");
      h.assertTrue(!h.getLevel().hasChunkAt(absent),"Continuation never synchronously loads the old remote target");
      goal.tick();goal.stop();
      h.assertTrue(!h.getLevel().hasChunkAt(absent),"Transaction/cleanup also keep the old chunk unloaded");h.succeed();
    });
  }
}
