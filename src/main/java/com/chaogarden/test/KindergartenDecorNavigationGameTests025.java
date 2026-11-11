package com.chaogarden.test;

import com.chaogarden.ChaoGarden;
import com.chaogarden.core.ChaoState;
import com.chaogarden.core.LocomotionRules;
import com.chaogarden.kindergarten.KindergartenDecor025;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.ai.goal.TemptGoal;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** The ordinary registered fruit-temptation goal must physically route around school furniture. */
@GameTestHolder(ChaoGarden.MODID)
@PrefixGameTestTemplate(false)
public final class KindergartenDecorNavigationGameTests025 {
  @GameTest(template="activity_test",timeoutTicks=400)
  public static void trainedChaoActuallyWalksAroundChairAndDeskToItsKeeper(GameTestHelper h){
    for(int x=1;x<=18;x++)for(int z=1;z<=18;z++){
      h.setBlock(new BlockPos(x,0,z),Blocks.STONE);
      for(int y=1;y<=5;y++)h.setBlock(new BlockPos(x,y,z),Blocks.AIR);
    }
    var world=h.getLevel();var chair=h.absolutePos(new BlockPos(9,1,9));var desk=chair.south();
    var furniture=List.of(chair,desk);
    world.setBlock(chair,KindergartenDecor025.state("school_pink_chair",Direction.SOUTH),3);
    world.setBlock(desk,KindergartenDecor025.state("school_principal_desk",Direction.SOUTH),3);
    for(var at:furniture){
      h.assertTrue(!world.getBlockState(at).isPathfindable(PathComputationType.LAND),"A solid furniture cell advertised an open land path");
      // Block stepping onto the low cushion: the useful route is around it.
      world.setBlock(at.above(),Blocks.STONE.defaultBlockState(),3);
    }
    var keeper=TestConnections.makeSurvivalServerPlayer(h);var destination=Vec3.atBottomCenterOf(h.absolutePos(new BlockPos(13,1,9)));
    keeper.teleportTo(world,destination.x,destination.y,destination.z,0,0);keeper.setNoGravity(true);
    keeper.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(ChaoGarden.item("garden_fruit")));
    var chao=ChaoGarden.CHAO.get().create(world);h.assertTrue(chao!=null,"Cannot create ordinary navigation Chao");
    var state=new ChaoState();state.adult=true;state.points[ChaoState.RUN]=LocomotionRules.WALK_POINTS;state.hunger=95;state.energy=100;state.happiness=80;
    chao.owner=keeper.getUUID();chao.setState(state);chao.setInvulnerable(true);chao.getRandom().setSeed(25);
    Vec3 start=Vec3.atBottomCenterOf(h.absolutePos(new BlockPos(5,1,9)));chao.moveTo(start);world.addFreshEntity(chao);
    chao.goalSelector.removeAllGoals(g->true);chao.targetSelector.removeAllGoals(g->true);
    // This is the same vanilla goal and food used by ordinary Chao registration.
    chao.goalSelector.addGoal(3,new TemptGoal(chao,1,Ingredient.of(ChaoGarden.item("garden_fruit")),false));
    var detour=new AtomicReference<Double>(0d);
    h.onEachTick(()->{
      detour.set(Math.max(detour.get(),Math.abs(chao.getZ()-start.z)));
      var path=chao.getNavigation().getPath();if(path!=null)for(int i=0;i<path.getNodeCount();i++){
        var node=path.getNode(i);for(var at:furniture)
          h.assertTrue(node.x!=at.getX()||node.y!=at.getY()||node.z!=at.getZ(),"Selected navigation path entered solid chair/desk cell");
      }
    });
    h.succeedWhen(()->{
      h.assertTrue(chao.getX()>chair.getX()+1&&chao.distanceToSqr(keeper)<6.5,"Chao did not walk around furniture to its keeper: "+chao.position());
      h.assertTrue(detour.get()>.6&&chao.distanceToSqr(start)>16,"Chao did not make a real lateral detour around chair and desk");
      h.assertTrue(!chao.isNoAi()&&LocomotionRules.groundGait(chao.state())==LocomotionRules.GroundGait.WALK,"Navigation fixture lost its ordinary, minimally trained walking gait");
      world.getServer().getPlayerList().remove(keeper);chao.discard();
    });
  }
}
