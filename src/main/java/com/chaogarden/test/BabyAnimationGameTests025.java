package com.chaogarden.test;

import com.chaogarden.ChaoGarden;
import com.chaogarden.core.ChaoAnimationRules;
import com.chaogarden.core.ChaoState;
import com.chaogarden.entity.ChaoEntity;
import java.util.Arrays;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** Real server triggers, a saved newborn reaction, and a petting navigation hold. */
@GameTestHolder(ChaoGarden.MODID)
@PrefixGameTestTemplate(false)
public final class BabyAnimationGameTests025 {
  private static ChaoEntity baby(GameTestHelper h,int x) {
    for(int xx=1;xx<=9;xx++)for(int z=1;z<=6;z++)h.setBlock(new BlockPos(xx,0,z),Blocks.STONE);
    var c=ChaoGarden.CHAO.get().create(h.getLevel());h.assertTrue(c!=null,"Baby creates");
    var state=new ChaoState();state.hunger=80;state.energy=100;state.happiness=30;
    c.setState(state);c.moveTo(Vec3.atBottomCenterOf(h.absolutePos(new BlockPos(x,1,3))));
    c.setNoAi(true);h.getLevel().addFreshEntity(c);return c;
  }

  @GameTest(template="activity_test",timeoutTicks=100)
  public static void newbornSettlingResumesAfterSaveWithoutChangingInheritedStats(GameTestHelper h) {
    var robot=baby(h,7);robot.state().adult=true;robot.state().character="omochao";robot.sync();
    robot.hatchReaction(false,null);
    h.assertTrue(!robot.isBehaving()&&robot.state().adult,"An adult robot must never acquire the newborn settling phase");
    robot.discard();
    var c=baby(h,3);int[] grades=c.state().grades.clone(),points=c.state().points.clone();
    c.hatchReaction(false,null);
    h.assertTrue(c.getBehavior()==ChaoEntity.Behavior.HATCH
        &&c.getBehaviorTicks()==ChaoAnimationRules.HATCH_TICKS,"Gentle/natural hatch begins the real newborn reaction");
    var live=new ChaoEntity[]{c};
    h.runAfterDelay(23,()->{
      var saved=new CompoundTag();live[0].saveWithoutId(saved);
      int remaining=live[0].getBehaviorTicks();live[0].discard();
      var restored=ChaoGarden.CHAO.get().create(h.getLevel());restored.load(saved);h.getLevel().addFreshEntity(restored);live[0]=restored;
      h.assertTrue(restored.getBehavior()==ChaoEntity.Behavior.HATCH&&restored.getBehaviorTicks()==remaining,
          "Reload preserves the remaining synced reaction, not a restarted whole animation");
    });
    h.runAfterDelay(75,()->{
      h.assertTrue(!live[0].isBehaving()&&!live[0].state().adult,"The reaction finishes and leaves an actual baby");
      h.assertTrue(Arrays.equals(grades,live[0].state().grades)&&Arrays.equals(points,live[0].state().points),
          "The visible hatch callback must not apply personality/training rewards again");
      live[0].discard();h.succeed();
    });
  }

  @GameTest(template="activity_test",timeoutTicks=110)
  public static void petReactionStopsWalkingAndRepeatedClicksDoNotDuplicateCare(GameTestHelper h) {
    var c=baby(h,3);c.setNoAi(false);
    c.goalSelector.removeAllGoals(g->true);c.targetSelector.removeAllGoals(g->true);
    var p=h.makeMockPlayer(GameType.SURVIVAL);c.owner=p.getUUID();
    p.setItemInHand(InteractionHand.MAIN_HAND,ItemStack.EMPTY);
    c.getNavigation().moveTo(h.absolutePos(new BlockPos(8,1,3)).getX(),c.getY(),c.getZ(),1);
    c.mobInteract(p,InteractionHand.MAIN_HAND);int happiness=c.state().happiness,bond=c.state().bond;
    h.assertTrue(c.isBeingPetted()&&c.getAffectionTicks()==ChaoAnimationRules.PET_TICKS
        &&c.getNavigation().isDone(),"Actual petting immediately stops the old walking path");
    h.runAfterDelay(15,()->{
      int remaining=c.getAffectionTicks();c.mobInteract(p,InteractionHand.MAIN_HAND);
      h.assertTrue(c.getAffectionTicks()==remaining&&c.state().happiness==happiness&&c.state().bond==bond,
          "Spam during the existing cooldown neither restarts the gesture nor grants duplicate care");
    });
    h.runAfterDelay(65,()->{
      h.assertTrue(!c.isBeingPetted(),"The quiet pet response has a finite end");
      c.mobInteract(p,InteractionHand.MAIN_HAND);
      h.assertTrue(c.isBeingPetted(),"The keeper can pet again after the normal care cooldown");
      c.discard();h.succeed();
    });
  }
}
