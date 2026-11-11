package com.chaogarden.test;

import com.chaogarden.ChaoGarden;
import com.chaogarden.core.ChaoPreferences;
import com.chaogarden.core.ChaoState;
import com.chaogarden.core.LocomotionRules;
import com.chaogarden.entity.ChaoEntity;
import com.chaogarden.toys.ChaoToyBlock;
import com.chaogarden.toys.ToyContent;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.level.pathfinder.PathType;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** A real slow newborn must route from the rear around a physical TV/alcove. */
@GameTestHolder(ChaoGarden.MODID)
@PrefixGameTestTemplate(false)
public final class ToyApproachGameTests025 {
  private ToyApproachGameTests025(){}
  private static void approach(GameTestHelper h,Direction facing,boolean alcove) {
    for(int x=1;x<=20;x++)for(int z=1;z<=20;z++) {
      h.setBlock(new BlockPos(x,0,z),Blocks.STONE);
      for(int y=1;y<=5;y++)h.setBlock(new BlockPos(x,y,z),Blocks.AIR);
    }
    var world=h.getLevel();var pos=h.absolutePos(new BlockPos(9,1,9));
    var tv=ToyContent.block("chao_tv").defaultBlockState().setValue(ChaoToyBlock.FACING,facing);
    world.setBlock(pos,tv,3);
    h.assertTrue(!tv.isPathfindable(PathComputationType.LAND),"A physical TV must not be an OPEN land node");
    // A two-high alcove blocks climbing over the TV and one side. The other
    // side and the actual screen-side standing cell remain reachable.
    var side=facing.getClockWise();
    if(alcove) {
      world.setBlock(pos.above(),Blocks.STONE.defaultBlockState(),3);
      for(var at:java.util.List.of(pos.relative(side),pos.relative(side).relative(facing.getOpposite())))
        for(int y=0;y<2;y++)world.setBlock(at.above(y),Blocks.STONE.defaultBlockState(),3);
    }
    var behind=pos.relative(facing.getOpposite(),alcove?3:2);
    var c=ChaoGarden.CHAO.get().create(world);h.assertTrue(c!=null,"Crawling baby creates");
    var state=new ChaoState();state.adult=false;state.hunger=90;state.energy=100;
    state.happiness=20;state.boredom=100;state.personality=ChaoPreferences.EASILY_BORED;
    // This healthy navigation fixture excludes unrelated random illness at
    // age1200 through the same treatment immunity used by the Chao clinic.
    for(int illness=0;illness<com.chaogarden.core.ChaoIllness.COUNT;illness++)state.setIllness(illness,100);
    c.setState(state);c.getRandom().setSeed(18);c.setInvulnerable(true);
    c.moveTo(Vec3.atBottomCenterOf(behind));world.addFreshEntity(c);
    h.assertTrue(tv.getBlockPathType(world,pos,c)==PathType.FENCE,
        "Tall physical furniture must exclude the failed jump-on-top route as well");
    c.goalSelector.removeAllGoals(g->true);c.targetSelector.removeAllGoals(g->true);
    c.goalSelector.addGoal(4,new ToyContent.PlayGoal(c));
    Vec3 start=c.position(),front=Vec3.atBottomCenterOf(pos.relative(facing));
    var reserved=new AtomicBoolean();var played=new AtomicBoolean();var previousPlaying=new AtomicBoolean();var detour=new AtomicReference<Double>(0d);
    h.onEachTick(()->{
      Vec3 delta=c.position().subtract(Vec3.atBottomCenterOf(pos));
      detour.set(Math.max(detour.get(),Math.abs(delta.x*side.getStepX()+delta.z*side.getStepZ())));
      var path=c.getNavigation().getPath();
      if(path!=null)for(int i=0;i<path.getNodeCount();i++) {
        var node=path.getNode(i);
        h.assertTrue(node.x!=pos.getX()||node.z!=pos.getZ()||node.y<pos.getY()||node.y>pos.getY()+1,
            "The selected approach path crosses the physical TV or its unreachable jump-up node");
      }
      if(h.getTick()%40==0) {
        var tracePath=c.getNavigation().getPath();
        var traceData=c.getPersistentData();
        String nodes=tracePath==null?"none":java.util.stream.IntStream.range(0,tracePath.getNodeCount()).mapToObj(i->{
          var n=tracePath.getNode(i);return i+":"+(n.x-pos.getX())+","+(n.y-pos.getY())+","+(n.z-pos.getZ())+":"+n.type;
        }).collect(java.util.stream.Collectors.joining(";"));
        com.mojang.logging.LogUtils.getLogger().info("CHAO_TV0254_APPROACH facing={} alcove={} testTick={} entityTick={} worldTick={} ticking={} pos={} rel={} start={} stand={} dist2={} velocity={} ground={} mayPlay={} eligible={} running={} target={} navDone={} pathReach={} pathIndex={} pathCount={} pathEnd={} nextNode={} moveWanted={} want={} neighbors={} nodes={}",
            facing,alcove,h.getTick(),c.tickCount,world.getGameTime(),world.isPositionEntityTicking(c.blockPosition()),c.position(),c.position().subtract(Vec3.atBottomCenterOf(pos)),start,front,c.distanceToSqr(front),c.getDeltaMovement(),c.onGround(),ToyContent.mayPlay(c),!c.isHandlingItem()&&!c.isBehaving()&&!c.isResting(),
            c.goalSelector.getAvailableGoals().stream().anyMatch(g->g.isRunning()&&g.getGoal() instanceof ToyContent.PlayGoal),traceData.contains("ChaoToyTarget")?ToyContent.target(c):"none",c.getNavigation().isDone(),
            tracePath!=null&&tracePath.canReach(),tracePath==null?-1:tracePath.getNextNodeIndex(),tracePath==null?0:tracePath.getNodeCount(),tracePath==null?"none":tracePath.getEndNode(),tracePath==null||tracePath.isDone()?"none":tracePath.getNextNodePos(),
            c.getMoveControl().hasWanted(),new Vec3(c.getMoveControl().getWantedX(),c.getMoveControl().getWantedY(),c.getMoveControl().getWantedZ()),world.getEntitiesOfClass(ChaoEntity.class,new net.minecraft.world.phys.AABB(pos).inflate(4),other->other!=c).size(),nodes);
      }
      if(!played.get()) {
        if(c.getPersistentData().contains("ChaoToyTarget"))reserved.set(true);
        else h.assertTrue(!reserved.get(),"A progressing newborn route must retain its reservation until play begins");
      }
      if(ToyContent.isPlaying(c)) {
        played.set(true);
        h.assertTrue(c.distanceToSqr(front)<.7,"Play begins only at the actual screen side");
      }
      boolean active=ToyContent.isPlaying(c),changed=previousPlaying.getAndSet(active)!=active;
      if(changed||played.get()&&c.tickCount%40==0) {
        var data=c.getPersistentData();
        com.mojang.logging.LogUtils.getLogger().info("CHAO_TOY025_SESSION facing={} tick={} worldTick={} until={} delta={} remaining={} active={} happiness={} boredom={} eligible={} ground={} ill={} sleep={} energy={} hunger={} behavior={} action={} goalRunning={} goalEveryTick={} target={}",
            facing,c.tickCount,world.getGameTime(),data.getLong("ChaoToyUntil"),data.getLong("ChaoToyUntil")-world.getGameTime(),
            c.toyPlayingTicks(),active,c.state().happiness,c.state().boredom,ToyContent.mayPlay(c),c.onGround(),c.state().isIll(),c.isResting(),c.state().energy,c.state().hunger,c.getBehavior(),c.getItemAction(),
            c.goalSelector.getAvailableGoals().stream().anyMatch(g->g.isRunning()&&g.getGoal() instanceof ToyContent.PlayGoal),
            c.goalSelector.getAvailableGoals().stream().filter(g->g.getGoal() instanceof ToyContent.PlayGoal).allMatch(g->g.requiresUpdateEveryTick()),
            data.contains("ChaoToyTarget")?ToyContent.target(c):"none");
      }
    });
    h.succeedWhen(()->{
      h.assertTrue(played.get(),"A naturally selected PlayGoal must reach the front from "+facing+" rear; pos="+c.position());
      h.assertTrue(detour.get()>.65&&c.distanceToSqr(start)>4,"Actual navigation must move around the blocked toy/side");
      h.assertTrue(!ToyContent.isPlaying(c)&&c.state().happiness>20,"The real session finishes before granting its reward");
      h.assertTrue(!c.state().adult&&LocomotionRules.groundGait(c.state())==LocomotionRules.GroundGait.CRAWL,
          "The fixture must use newborn speed, not fabricated running stats");
      h.assertTrue(world.getBlockState(pos).is(ToyContent.block("chao_tv")),"The one real toy stays placed");
    });
  }
  // Retain the original budget while requiring uninterrupted newborn approach
  // progress and its complete 160-tick voluntary session.
  @GameTest(template="activity_test",timeoutTicks=2000)
  public static void northFacingTvRoutesBabyAroundRearAndSideObstacles(GameTestHelper h){approach(h,Direction.NORTH,true);}
  @GameTest(template="activity_test",timeoutTicks=2000)
  public static void southFacingTvRoutesBabyAroundRearAndSideObstacles(GameTestHelper h){approach(h,Direction.SOUTH,true);}
  @GameTest(template="activity_test",timeoutTicks=2000)
  public static void eastFacingTvRoutesBabyAroundRearAndSideObstacles(GameTestHelper h){approach(h,Direction.EAST,true);}
  @GameTest(template="activity_test",timeoutTicks=2000)
  public static void westFacingTvRoutesBabyAroundRearAndSideObstacles(GameTestHelper h){approach(h,Direction.WEST,true);}
  @GameTest(template="activity_test",timeoutTicks=2000)
  public static void northFacingUncappedTvRoutesBabyAroundItsRear(GameTestHelper h){approach(h,Direction.NORTH,false);}
  @GameTest(template="activity_test",timeoutTicks=2000)
  public static void southFacingUncappedTvRoutesBabyAroundItsRear(GameTestHelper h){approach(h,Direction.SOUTH,false);}
  @GameTest(template="activity_test",timeoutTicks=2000)
  public static void eastFacingUncappedTvRoutesBabyAroundItsRear(GameTestHelper h){approach(h,Direction.EAST,false);}
  @GameTest(template="activity_test",timeoutTicks=2000)
  public static void westFacingUncappedTvRoutesBabyAroundItsRear(GameTestHelper h){approach(h,Direction.WEST,false);}
}
