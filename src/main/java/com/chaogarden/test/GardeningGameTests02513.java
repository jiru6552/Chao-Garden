package com.chaogarden.test;

import com.chaogarden.ChaoGarden;
import com.chaogarden.core.ChaoState;
import com.chaogarden.entity.ChaoEntity;
import com.chaogarden.fruit.*;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.gametest.*;

@GameTestHolder(ChaoGarden.MODID)
@PrefixGameTestTemplate(false)
public final class GardeningGameTests02513 {
  private static ChaoEntity setup(GameTestHelper h,BlockPos at) {
    for(var p:BlockPos.betweenClosed(at.offset(-2,0,-2),at.offset(2,5,2)))h.getLevel().setBlock(p,Blocks.AIR.defaultBlockState(),3);
    for(var p:BlockPos.betweenClosed(at.offset(-2,-1,-2),at.offset(2,-1,2)))h.getLevel().setBlock(p,Blocks.DIRT.defaultBlockState(),3);
    h.getLevel().setBlock(at,FruitContent.saplingForFruit("round_fruit").defaultBlockState(),3);
    GardenTreeData.get(h.getLevel()).planted(at,"round_fruit");
    var c=ChaoGarden.CHAO.get().create(h.getLevel());var s=new ChaoState();s.hunger=80;s.energy=90;s.points[ChaoState.RUN]=50;s.setTrait("reward.watering_can",1);
    c.setState(s);c.moveTo(at.getX()+1.5,at.getY(),at.getZ()+.5);h.getLevel().addFreshEntity(c);return c;
  }
  // The route and the planting clearance must fit inside the forced test structure,
  // including when its origin lies beside a chunk boundary.
  @GameTest(template="activity_test",timeoutTicks=220)
  public static void gardenerReachesTheExactPlantingStandFromSeveralBlocksAway(GameTestHelper h) {
    var at=h.absolutePos(new BlockPos(9,1,9));var level=h.getLevel();level.setDayTime(6000);
    for(var p:BlockPos.betweenClosed(at.offset(-3,0,-3),at.offset(3,8,4)))level.setBlock(p,Blocks.AIR.defaultBlockState(),3);
    for(var p:BlockPos.betweenClosed(at.offset(-3,-1,-3),at.offset(3,-1,4)))level.setBlock(p,Blocks.SMOOTH_STONE.defaultBlockState(),3);
    level.setBlock(at.below(),Blocks.DIRT.defaultBlockState(),3);
    var c=ChaoGarden.CHAO.get().create(level);var s=new ChaoState();s.hunger=80;s.energy=90;s.points[ChaoState.RUN]=50;s.setTrait("reward.shovel",1);
    c.setState(s);c.moveTo(at.getX()+.5,at.getY(),at.getZ()+3.5);level.addFreshEntity(c);
    var p=h.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);p.getAbilities().instabuild=false;
    var seed=new net.minecraft.world.item.ItemStack(ChaoGarden.item("round_fruit_seed"));p.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,seed);c.offerItem(p,net.minecraft.world.InteractionHand.MAIN_HAND);
    h.runAtTickTime(180,()->{
      h.assertTrue(level.getBlockState(at).is(FruitContent.saplingForFruit("round_fruit")),"Gardener stopped a block short of its only plantable plot");
      h.assertTrue(seed.isEmpty()&&c.getMainHandItem().isEmpty(),"Planting did not consume exactly the offered seed");c.discard();h.succeed();
    });
  }
  @GameTest(template="empty",timeoutTicks=180)
  public static void wateringRetainsRecoveryAfterSingleCommit(GameTestHelper h) {
    var at=h.absolutePos(new BlockPos(3,2,3));var c=setup(h,at);var started=new long[]{-1};var committed=new boolean[]{false};
    h.onEachTick(()->{
      long now=h.getLevel().getGameTime();
      if(started[0]<0&&c.getBehavior()==ChaoEntity.Behavior.WATER)started[0]=now;
      if(started[0]<0)return;long elapsed=now-started[0];
      var state=h.getLevel().getBlockState(at);
      if(elapsed>=43&&elapsed<=48) {
        h.assertTrue(c.getBehavior()==ChaoEntity.Behavior.WATER,"Can disappeared before lowering recovery");
        h.assertTrue(state.is(FruitContent.saplingForFruit("round_fruit"))&&state.getValue(ChaoSaplingBlock.STAGE)==1,"Watering failed or advanced more than once");committed[0]=true;
      }
      if(elapsed>=58) {
        h.assertTrue(committed[0]&&c.getBehavior()!=ChaoEntity.Behavior.WATER&&c.getMainHandItem().isEmpty(),"Watering failed to finish without creating a tool");
        c.discard();h.succeed();
      }
    });
  }
  @GameTest(template="empty",timeoutTicks=180)
  public static void displacedGardenerCannotWaterFromAcrossTheGarden(GameTestHelper h) {
    var at=h.absolutePos(new BlockPos(3,2,3));var c=setup(h,at);var moved=new long[]{-1};
    h.onEachTick(()->{
      long now=h.getLevel().getGameTime();
      if(moved[0]<0&&c.getBehavior()==ChaoEntity.Behavior.WATER&&c.getBehaviorProgress(0)>.25f) {
        c.teleportTo(at.getX()+20,at.getY()+2,at.getZ());moved[0]=now;
      }
      if(moved[0]>=0&&now-moved[0]>=45) {
        var state=h.getLevel().getBlockState(at);
        h.assertTrue(state.is(FruitContent.saplingForFruit("round_fruit"))&&state.getValue(ChaoSaplingBlock.STAGE)==0,"Displaced Chao remotely watered its old target");
        h.assertTrue(c.getBehavior()!=ChaoEntity.Behavior.WATER&&c.getMainHandItem().isEmpty(),"Interrupted watering leaked behavior or tool");c.discard();h.succeed();
      }
    });
  }
}
