package com.chaogarden.test;

import com.chaogarden.ChaoGarden;
import com.chaogarden.core.ChaoPreferences;
import com.chaogarden.core.ChaoState;
import com.chaogarden.entity.ChaoEntity;
import com.chaogarden.toys.ChaoToyBlock;
import com.chaogarden.toys.ToyContent;
import java.util.concurrent.atomic.AtomicBoolean;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** Real voluntary navigation/contact and mounted lifecycle; run on a fresh GameTest world. */
@GameTestHolder(ChaoGarden.MODID)
@PrefixGameTestTemplate(false)
public final class ToyInteractionGameTests02518 {
  private static void floor(GameTestHelper h) {
    for(int x=1;x<=20;x++)for(int z=1;z<=20;z++) {
      h.setBlock(new BlockPos(x,0,z),Blocks.STONE);
      for(int y=1;y<=5;y++)h.setBlock(new BlockPos(x,y,z),Blocks.AIR);
    }
  }
  private static ChaoEntity actor(GameTestHelper h,BlockPos at) {
    var c=ChaoGarden.CHAO.get().create(h.getLevel());h.assertTrue(c!=null,"Chao creates");
    var state=new ChaoState();state.adult=true;state.points[ChaoState.RUN]=667;
    state.happiness=20;state.hunger=90;state.energy=100;state.boredom=100;state.personality=ChaoPreferences.EASILY_BORED;
    c.setState(state);c.setInvulnerable(true);c.getRandom().setSeed(18);
    c.moveTo(at.getX()+.5,at.getY(),at.getZ()+.5,0,0);h.getLevel().addFreshEntity(c);
    c.goalSelector.removeAllGoals(g->true);c.targetSelector.removeAllGoals(g->true);
    c.goalSelector.addGoal(4,new ToyContent.PlayGoal(c));return c;
  }
  private static void keyboard(GameTestHelper h,String kind,Direction facing) {
    floor(h);var pos=h.absolutePos(new BlockPos(9,1,9));
    h.getLevel().setBlock(pos,ToyContent.block(kind).defaultBlockState().setValue(ChaoToyBlock.FACING,facing),3);
    var c=actor(h,pos.relative(facing.getOpposite(),3));var seen=new AtomicBoolean();
    h.onEachTick(()->{
      if(!ToyContent.isPlaying(c))return;seen.set(true);
      var anchor=ToyContent.keyboardAnchor(c,pos);
      h.assertTrue(c.distanceToSqr(anchor)<.007,"Keyboard session stays at precise front contact point");
      h.assertTrue(h.getLevel().noCollision(c,c.getBoundingBox()),"Contact point is outside cabinet collision");
      h.assertTrue(c.position().subtract(Vec3.atBottomCenterOf(pos)).dot(new Vec3(facing.getStepX(),0,facing.getStepZ()))>0,"Never play from rear");
      if(ToyContent.sessionElapsed(h.getLevel(),pos)>1)
        h.assertTrue(Math.abs(net.minecraft.util.Mth.wrapDegrees(c.getYRot()-facing.getOpposite().toYRot()))<1,"Body faces keys");
    });
    h.succeedWhen(()->{h.assertTrue(seen.get()&&!ToyContent.isPlaying(c)&&c.state().happiness>20,"Complete precise keyboard play");});
  }
  @GameTest(template="activity_test",timeoutTicks=1800) public static void pianoNorth(GameTestHelper h){keyboard(h,"chao_piano",Direction.NORTH);}
  @GameTest(template="activity_test",timeoutTicks=1800) public static void pianoEast(GameTestHelper h){keyboard(h,"chao_piano",Direction.EAST);}
  @GameTest(template="activity_test",timeoutTicks=1800) public static void pianoSouth(GameTestHelper h){keyboard(h,"chao_piano",Direction.SOUTH);}
  @GameTest(template="activity_test",timeoutTicks=1800) public static void pianoWest(GameTestHelper h){keyboard(h,"chao_piano",Direction.WEST);}
  @GameTest(template="activity_test",timeoutTicks=1800) public static void organNorth(GameTestHelper h){keyboard(h,"chao_organ",Direction.NORTH);}
  @GameTest(template="activity_test",timeoutTicks=1800) public static void organEast(GameTestHelper h){keyboard(h,"chao_organ",Direction.EAST);}
  @GameTest(template="activity_test",timeoutTicks=1800) public static void organSouth(GameTestHelper h){keyboard(h,"chao_organ",Direction.SOUTH);}
  @GameTest(template="activity_test",timeoutTicks=1800) public static void organWest(GameTestHelper h){keyboard(h,"chao_organ",Direction.WEST);}
  @GameTest(template="activity_test",timeoutTicks=500)
  public static void blockedKeyboardFrontCannotFallBackToRear(GameTestHelper h) {
    floor(h);var pos=h.absolutePos(new BlockPos(9,1,9));h.getLevel().setBlock(pos,ToyContent.block("chao_piano").defaultBlockState(),3);
    h.getLevel().setBlock(pos.north(),Blocks.STONE.defaultBlockState(),3);
    var c=actor(h,pos.south(2));h.onEachTick(()->h.assertTrue(!ToyContent.isPlaying(c),"Blocked keys must prevent selection"));
    h.runAfterDelay(450,h::succeed);
  }



  @GameTest(template="activity_test",timeoutTicks=1400)
  public static void displacedKeyboardPlayerCancelsInsteadOfPlayingRemotely(GameTestHelper h) {
    floor(h);var pos=h.absolutePos(new BlockPos(9,1,9));h.getLevel().setBlock(pos,ToyContent.block("chao_piano").defaultBlockState(),3);
    var c=actor(h,pos.north(2));var done=new AtomicBoolean();
    h.onEachTick(()->{
      if(done.get()||!ToyContent.isPlaying(c))return;done.set(true);c.setPos(c.getX()+2,c.getY(),c.getZ());
      h.runAfterDelay(8,()->{h.assertTrue(!ToyContent.isPlaying(c)&&c.state().happiness==20,"Displacement cancels without reward");h.assertTrue(!h.getLevel().getBlockState(pos).getValue(ChaoToyBlock.PLAYING),"Keyboard no longer animates");h.succeed();});
    });
  }

  @GameTest(template="activity_test",timeoutTicks=1400)
  public static void keyboardPrecisionApproachRetainsExclusiveReservation(GameTestHelper h) {
    floor(h);var pos=h.absolutePos(new BlockPos(9,1,9));h.getLevel().setBlock(pos,ToyContent.block("chao_organ").defaultBlockState(),3);
    var first=actor(h,pos.north(2).west());var second=actor(h,pos.north(2).east());var seen=new AtomicBoolean();
    h.onEachTick(()->{
      h.assertTrue(!(ToyContent.isPlaying(first)&&ToyContent.isPlaying(second)),"Exactly one keyboard performer");
      if(ToyContent.isPlaying(first)||ToyContent.isPlaying(second))seen.set(true);
    });
    h.succeedWhen(()->h.assertTrue(seen.get()&&(first.state().happiness>20||second.state().happiness>20),"Reserved precision approach still completes"));
  }







}
