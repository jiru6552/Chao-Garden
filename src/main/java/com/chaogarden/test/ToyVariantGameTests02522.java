package com.chaogarden.test;

import com.chaogarden.toys.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.level.block.RenderShape;
import net.neoforged.neoforge.gametest.*;

@GameTestHolder("chao_garden") @PrefixGameTestTemplate(false)
public final class ToyVariantGameTests02522 {
  @GameTest(template="activity_test",timeoutTicks=1800) public static void babyDamageInterruption(GameTestHelper h){RattleGameTests02520.interrupt(h,"hurt",false);}
  @GameTest(template="activity_test",timeoutTicks=1800) public static void adultDamageInterruption(GameTestHelper h){RattleGameTests02520.interrupt(h,"hurt",true);}
  @GameTest(template="activity_test",timeoutTicks=1800) public static void babyPettingInterruption(GameTestHelper h){RattleGameTests02520.interrupt(h,"pet",false);}
  @GameTest(template="activity_test",timeoutTicks=1800) public static void adultPettingInterruption(GameTestHelper h){RattleGameTests02520.interrupt(h,"pet",true);}
  @GameTestGenerator public static Collection<TestFunction> variants(){
    var tests=new ArrayList<TestFunction>();int index=0;
    for(var specimen:ToyVariantCases02522.all())for(String toy:List.of(RattlePlayRules.KIND,BubblePlayRules.KIND))for(boolean cancel:new boolean[]{false,true}){
      String name="toy_variant_"+specimen.name()+"_"+toy+"_"+(cancel?"cancel":"complete");
      tests.add(new TestFunction("toy_variants_"+(index++/12),name,"chao_garden:activity_test",1800,0,true,h->play(h,specimen,toy,cancel)));
    }
    return tests;
  }
  private static void play(GameTestHelper h,ToyVariantCases02522.Case specimen,String toy,boolean cancel){
    var p=RattleGameTests02520.floor(h);h.getLevel().setBlock(p,ToyContent.block(toy).defaultBlockState(),3);
    var s=specimen.state();var c=RattleGameTests02520.actor(h,p,s.adult,s.primaryType,false);c.setState(s);
    var start=new AtomicLong(-1);var interrupted=new AtomicLong(-1);
    h.onEachTick(()->{
      if(ToyContent.isPlaying(c)){
        start.compareAndSet(-1,c.getPersistentData().getLong("ChaoToyUntil")-160);
        h.assertTrue(c.toyPlayingKind().equals(toy),"selected real placed toy");
        h.assertTrue(h.getLevel().getBlockState(p).getRenderShape()==RenderShape.INVISIBLE,"variant uses one hidden recoverable source");
        h.assertTrue(c.getHeldChaoItem().isEmpty()&&c.getMainHandItem().isEmpty(),"no duplicated inventory prop");
        if(cancel&&interrupted.get()<0&&h.getLevel().getGameTime()-start.get()>=60){interrupted.set(h.getLevel().getGameTime());c.setStaying(true);}
      }
    });
    h.succeedWhen(()->{
      h.assertTrue(start.get()>=0&&h.getLevel().getGameTime()>start.get()+(cancel?85:175),"observe the complete interaction");
      h.assertTrue(!ToyContent.isPlaying(c)&&h.getLevel().getBlockState(p).getRenderShape()==RenderShape.MODEL,"source restored and ownership released");
      h.assertTrue(c.state().happiness==(cancel?20:20+(!s.adult&&toy.equals(RattlePlayRules.KIND)?7:4)),"one reward only on completion");
      h.assertTrue(c.state().character.equals(s.character)&&c.state().variant.equals(s.variant)&&c.state().immortal==s.immortal,"variant identity preserved");
    });
  }
}
