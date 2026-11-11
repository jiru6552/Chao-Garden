package com.chaogarden.test;

import com.chaogarden.core.ChaoState;
import com.chaogarden.toys.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.level.block.RenderShape;
import net.neoforged.neoforge.gametest.*;

/** Construction is an explicit AI lockout, not a toy-playing variant. */
@GameTestHolder("chao_garden") @PrefixGameTestTemplate(false)
public final class BubbleGroundGameTests02523 {
  private static void locked(GameTestHelper h,int stage){
    var p=RattleGameTests02520.floor(h);h.getLevel().setBlock(p,ToyContent.block(BubblePlayRules.KIND).defaultBlockState(),3);
    var c=RattleGameTests02520.actor(h,p,true,ChaoState.NORMAL,false);
    var s=c.state();s.character="omochao";s.setTrait("content.construction",stage);c.setState(s);
    h.onEachTick(()->{
      h.assertTrue(!ToyContent.isPlaying(c)&&!h.getLevel().getBlockState(p).getValue(ChaoToyBlock.PLAYING),"assembly must not reserve or play a placed toy");
      h.assertTrue(h.getLevel().getBlockState(p).getRenderShape()==RenderShape.MODEL,"physical wand stays visible");
    });
    h.succeedWhen(()->{
      h.assertTrue(c.tickCount>=100,"observe an actually ticking entity through 100 ticks");
      h.assertTrue(c.state().happiness==20&&c.state().intTrait("content.construction")==stage,"assembly state and care remain unchanged");
    });
  }
  @GameTest(template="activity_test",timeoutTicks=400,batch="bubble_construction") public static void unfinishedRobotStageOneWaitsForAssembly(GameTestHelper h){locked(h,1);}
  @GameTest(template="activity_test",timeoutTicks=400,batch="bubble_construction") public static void unfinishedRobotStageTwoWaitsForAssembly(GameTestHelper h){locked(h,2);}
}
