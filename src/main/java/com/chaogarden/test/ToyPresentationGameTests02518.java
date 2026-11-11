package com.chaogarden.test;

import com.chaogarden.ChaoGarden;
import com.chaogarden.core.ChaoState;
import com.chaogarden.entity.ChaoEntity;
import com.chaogarden.toys.ChaoToyBlock;
import com.chaogarden.toys.ToyContent;
import com.chaogarden.toys.ToyInteractionRules;
import java.util.concurrent.atomic.AtomicInteger;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** Native integration fixtures. Geometry/rules tests do not substitute for running these. */
@GameTestHolder(ChaoGarden.MODID)
@PrefixGameTestTemplate(false)
public final class ToyPresentationGameTests02518 {
  private static BlockPos floor(GameTestHelper h) {
    for(int x=4;x<=14;x++)for(int z=4;z<=14;z++) {
      h.setBlock(new BlockPos(x,0,z),Blocks.STONE);
      for(int y=1;y<=4;y++)h.setBlock(new BlockPos(x,y,z),Blocks.AIR);
    }
    return h.absolutePos(new BlockPos(9,1,9));
  }
  private static ChaoEntity actor(GameTestHelper h,BlockPos pos,boolean adult) {
    var c=ChaoGarden.CHAO.get().create(h.getLevel());h.assertTrue(c!=null,"Chao creates");
    var state=new ChaoState();state.adult=adult;state.hunger=90;state.energy=100;c.setState(state);
    c.moveTo(pos.getX()+.5,pos.getY(),pos.getZ()+.5,0,0);c.setNoAi(true);c.setInvulnerable(true);
    h.getLevel().addFreshEntity(c);return c;
  }
  private static void jack(GameTestHelper h,boolean cancel) {
    var pos=floor(h);var block=ToyContent.block("chao_jack_in_box");
    h.getLevel().setBlock(pos,block.defaultBlockState().setValue(ChaoToyBlock.PLAYING,true),3);
    var c=actor(h,pos.north(),true);long start=h.getLevel().getGameTime();
    // Seed a real owned session to exercise scheduled block ticks without a random fear interruption.
    c.getPersistentData().putLong("ChaoToyTarget",pos.asLong());
    c.getPersistentData().putLong("ChaoToyUntil",start+160);
    c.getPersistentData().putString("ChaoToyKind","chao_jack_in_box");c.setToyPlaying("chao_jack_in_box",160);
    h.getLevel().scheduleTick(pos,block,1);var reveals=new AtomicInteger();var previous=new AtomicInteger();
    h.onEachTick(()->{
      int elapsed=(int)(h.getLevel().getGameTime()-start);var s=h.getLevel().getBlockState(pos);
      int frame=s.getValue(ChaoToyBlock.FRAME);
      if(previous.get()==0&&frame!=0)reveals.incrementAndGet();previous.set(frame);
      // Interior ranges tolerate scheduler/observer ordering by one tick at phase boundaries.
      if(elapsed>2&&elapsed<39)h.assertTrue(frame==0,"Closed wind-up, no early repeated pop");
      if(elapsed>45&&elapsed<59)h.assertTrue(frame==2,"Full reveal precedes fear-choice deadline");
      if(cancel&&elapsed==60)ToyContent.endPlaying(c,false);
      if(cancel&&elapsed>62)h.assertTrue(frame==0&&!s.getValue(ChaoToyBlock.PLAYING),"Cancellation restores idle promptly");
      if(!cancel&&elapsed>66&&elapsed<127)h.assertTrue(frame==2,"Held-open reveal does not cycle");
      if(!cancel&&elapsed>145)h.assertTrue(frame==0,"Jack closes after retreat");
    });
    h.runAtTickTime(cancel?70:165,()->{
      h.assertTrue(reveals.get()==1,"Exactly one scheduled reveal");
      h.assertTrue(h.getLevel().getBlockState(pos).is(block),"Original collectible remains");h.succeed();
    });
  }
  @GameTest(template="activity_test",timeoutTicks=190) public static void jackSingleReveal(GameTestHelper h){jack(h,false);}
  @GameTest(template="activity_test",timeoutTicks=100) public static void jackCancellationCloses(GameTestHelper h){jack(h,true);}

  @GameTest(template="activity_test",timeoutTicks=1600)
  public static void jackFearFollowsReveal(GameTestHelper h) {
    var pos=floor(h);var block=ToyContent.block("chao_jack_in_box");
    h.getLevel().setBlock(pos,block.defaultBlockState(),3);var c=actor(h,pos.north(2),true);
    c.setNoAi(false);c.state().boredom=100;
    c.state().personality=com.chaogarden.core.ChaoPreferences.EASILY_BORED;c.getRandom().setSeed(18);
    c.goalSelector.removeAllGoals(g->true);c.targetSelector.removeAllGoals(g->true);
    c.goalSelector.addGoal(4,new ToyContent.PlayGoal(c));
    var started=new java.util.concurrent.atomic.AtomicLong(-1);var sawFull=new java.util.concurrent.atomic.AtomicBoolean();
    h.onEachTick(()->{
      if(ToyContent.isPlaying(c)) {
        int elapsed=ToyContent.sessionElapsed(h.getLevel(),pos);
        started.compareAndSet(-1,h.getLevel().getGameTime()-elapsed);
        if(h.getLevel().getBlockState(pos).getValue(ChaoToyBlock.FRAME)==2)sawFull.set(true);
      } else if(started.get()>=0) {
        h.assertTrue(h.getLevel().getGameTime()-started.get()>=ToyInteractionRules.JACK_REACTION_TICK,
            "Voluntary jack session never ends in surprise before the visible reveal");
      }
    });
    h.succeedWhen(()->{
      h.assertTrue(started.get()>=0&&!ToyContent.isPlaying(c),"Real jack session completed or reacted");
      h.assertTrue(sawFull.get(),"Actor saw a full reveal before either outcome");
    });
  }

  private static void envelopes(GameTestHelper h,boolean adult) {
    var pos=floor(h);var c=actor(h,pos.north(2),adult);
    for(String kind:new String[]{"chao_radio","chao_guitar","chao_triangle","chao_rubber_duck","chao_jack_in_box","chao_rattle","chao_drum"}) {
      for(Direction facing:Direction.Plane.HORIZONTAL) {
        int turns=switch(facing){case EAST->1;case SOUTH->2;case WEST->3;default->0;};
        var expected=ToyInteractionRules.rotateEnvelope(ToyInteractionRules.collisionEnvelope(kind),turns);
        for(boolean active:new boolean[]{false,true})for(int frame=0;frame<4;frame++) {
          var s=ToyContent.block(kind).defaultBlockState().setValue(ChaoToyBlock.FACING,facing)
              .setValue(ChaoToyBlock.PLAYING,active).setValue(ChaoToyBlock.FRAME,frame);
          h.getLevel().setBlock(pos,s,3);var actual=s.getCollisionShape(h.getLevel(),pos).bounds();
          double[] values={actual.minX,actual.minY,actual.minZ,actual.maxX,actual.maxY,actual.maxZ};
          for(int a=0;a<6;a++)h.assertTrue(Math.abs(values[a]-expected[a]/16)<1e-7,"Native shape matches measured rotated envelope");
          for(Direction side:Direction.Plane.HORIZONTAL) {
            var anchor=ToyContent.stationaryAnchor(c,pos,pos.relative(side));c.setPos(anchor.x,anchor.y,anchor.z);
            h.assertTrue(h.getLevel().noCollision(c,c.getBoundingBox()),"All age/facing/frame contact anchors clear the prop");
          }
        }
      }
    }
    h.succeed();
  }
  @GameTest(template="activity_test",timeoutTicks=40) public static void childMeasuredEnvelopes(GameTestHelper h){envelopes(h,false);}
  @GameTest(template="activity_test",timeoutTicks=40) public static void adultMeasuredEnvelopes(GameTestHelper h){envelopes(h,true);}
}
