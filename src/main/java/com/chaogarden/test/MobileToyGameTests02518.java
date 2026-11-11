package com.chaogarden.test;

import com.chaogarden.ChaoGarden;
import com.chaogarden.core.ChaoPreferences;
import com.chaogarden.core.ChaoState;
import com.chaogarden.entity.ChaoEntity;
import com.chaogarden.toys.ChaoToyBlock;
import com.chaogarden.toys.ToyContent;
import com.chaogarden.toys.ToyInteractionRules;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** Native physics/contact checks; rendering still needs an actual client review. */
@GameTestHolder(ChaoGarden.MODID)
@PrefixGameTestTemplate(false)
public final class MobileToyGameTests02518 {
  private static void floor(GameTestHelper h) {
    for(int x=1;x<=20;x++)for(int z=1;z<=20;z++) {
      h.setBlock(new BlockPos(x,0,z),Blocks.STONE);
      for(int y=1;y<=5;y++)h.setBlock(new BlockPos(x,y,z),Blocks.AIR);
    }
  }
  private static ChaoEntity actor(GameTestHelper h,BlockPos at,boolean baby,boolean swimmer) {
    var c=ChaoGarden.CHAO.get().create(h.getLevel());h.assertTrue(c!=null,"Chao creates");
    var state=new ChaoState();state.adult=!baby;state.points[ChaoState.RUN]=baby?0:667;state.points[ChaoState.SWIM]=swimmer?667:0;
    state.happiness=20;state.hunger=90;state.energy=100;state.boredom=100;state.personality=ChaoPreferences.EASILY_BORED;
    c.setState(state);c.setInvulnerable(true);c.getRandom().setSeed(18);
    c.moveTo(at.getX()+.5,at.getY(),at.getZ()+.5,0,0);h.getLevel().addFreshEntity(c);
    // Keep the production FloatGoal so supported-ring tests exercise real arbitration.
    c.goalSelector.removeAllGoals(g->!(g instanceof net.minecraft.world.entity.ai.goal.FloatGoal));c.targetSelector.removeAllGoals(g->true);
    c.goalSelector.addGoal(4,new ToyContent.PlayGoal(c));return c;
  }
  private static void ball(GameTestHelper h,boolean baby) {
    floor(h);var pos=h.absolutePos(new BlockPos(9,1,9));h.getLevel().setBlock(pos,ToyContent.block("chao_ball").defaultBlockState(),3);
    var c=actor(h,pos.west(),baby,false);var previous=new AtomicReference<>(pos);var moved=new AtomicInteger();var chased=new AtomicBoolean();var last=new AtomicReference<>(c.position());
    h.onEachTick(()->{
      if(!ToyContent.isPlaying(c))return;
      BlockPos current=ToyContent.target(c),old=previous.getAndSet(current);
      if(!current.equals(old)) {
        moved.incrementAndGet();int kick=c.toyKickElapsed();
        h.assertTrue(kick>=ToyInteractionRules.BALL_CONTACT_TICK&&kick<ToyInteractionRules.BALL_KICK_TICKS,"Ball moves only at synchronized kick contact");
        Vec3 delta=c.position().subtract(Vec3.atBottomCenterOf(old));
        h.assertTrue(ToyInteractionRules.ballWithinReach(delta.horizontalDistanceSqr(),delta.y,c.getBbWidth()),"No remote kicks");
        h.assertTrue(h.getLevel().getBlockState(old).isAir(),"Old physical ball removed exactly once");
      }
      if(moved.get()>0&&c.toyKickElapsed()<0&&c.distanceToSqr(last.get())>.00001)chased.set(true);
      last.set(c.position());
    });
    h.succeedWhen(()->{
      h.assertTrue(moved.get()>0&&chased.get()&&!ToyContent.isPlaying(c)&&c.state().happiness>20,"Kick then actually pursue before completion");
      int count=0;for(BlockPos at:BlockPos.betweenClosed(pos.offset(-7,0,-7),pos.offset(7,0,7)))if(h.getLevel().getBlockState(at).is(ToyContent.block("chao_ball")))count++;
      h.assertTrue(count==1,"Only one real ball after contact/chase cycles");
    });
  }
  @GameTest(template="activity_test",timeoutTicks=1600) public static void adultBallContactAndChase(GameTestHelper h){ball(h,false);}
  @GameTest(template="activity_test",timeoutTicks=2000) public static void newbornBallContactAndChase(GameTestHelper h){ball(h,true);}
  @GameTest(template="activity_test",timeoutTicks=1400)
  public static void displacedBallPlayerCancelsWithoutRemoteKick(GameTestHelper h) {
    floor(h);var pos=h.absolutePos(new BlockPos(9,1,9));h.getLevel().setBlock(pos,ToyContent.block("chao_ball").defaultBlockState(),3);
    var c=actor(h,pos.west(),false,false);var done=new AtomicBoolean();
    h.onEachTick(()->{if(done.get()||!ToyContent.isPlaying(c))return;done.set(true);Vec3 rescue=Vec3.atBottomCenterOf(pos.west(6));c.setPos(rescue.x,rescue.y,rescue.z);
      h.runAfterDelay(8,()->{h.assertTrue(!ToyContent.isPlaying(c)&&c.state().happiness==20&&c.toyKickElapsed()<0,"Displacement clears kick/session without reward");h.assertTrue(h.getLevel().getBlockState(pos).is(ToyContent.block("chao_ball")),"No kick from remote location");h.succeed();});});
  }
  private static void horse(GameTestHelper h,Direction facing) {
    floor(h);var pos=h.absolutePos(new BlockPos(9,1,9));h.getLevel().setBlock(pos,ToyContent.block("chao_rocking_horse").defaultBlockState().setValue(ChaoToyBlock.FACING,facing),3);
    var c=actor(h,pos.relative(facing.getClockWise(),2),false,false);var seen=new AtomicBoolean();var frames=new AtomicInteger();
    h.onEachTick(()->{
      if(!ToyContent.isPlaying(c))return;int elapsed=ToyContent.sessionElapsed(h.getLevel(),pos);
      if(elapsed<16||elapsed>140||elapsed%8<2)return;
      seen.set(true);int frame=ToyInteractionRules.mountedFrame(elapsed);frames.getAndUpdate(v->v|(1<<frame));
      Vec3 expected=ToyContent.mountedAnchor(c,pos,h.getLevel().getBlockState(pos),elapsed);
      h.assertTrue(c.distanceToSqr(expected)<.001,"Rider follows measured saddle pivot");
      h.assertTrue(Math.abs(net.minecraft.util.Mth.wrapDegrees(c.getYRot()-facing.getOpposite().toYRot()))<1,"Rider faces source muzzle");
      h.assertTrue(h.getLevel().getBlockState(pos).getValue(ChaoToyBlock.FRAME)==frame,"Prop and rider share session phase");
    });
    h.succeedWhen(()->h.assertTrue(seen.get()&&Integer.bitCount(frames.get())==4&&!ToyContent.isPlaying(c)&&!c.isNoGravity()&&c.state().happiness>20,"Coordinated ride completes and dismounts"));
  }
  @GameTest(template="activity_test",timeoutTicks=1600) public static void horseNorth(GameTestHelper h){horse(h,Direction.NORTH);}
  @GameTest(template="activity_test",timeoutTicks=1600) public static void horseEast(GameTestHelper h){horse(h,Direction.EAST);}
  @GameTest(template="activity_test",timeoutTicks=1600) public static void horseSouth(GameTestHelper h){horse(h,Direction.SOUTH);}
  @GameTest(template="activity_test",timeoutTicks=1600) public static void horseWest(GameTestHelper h){horse(h,Direction.WEST);}
  private static void ring(GameTestHelper h,boolean swimmer) {
    floor(h);var pos=h.absolutePos(new BlockPos(9,0,9));h.getLevel().setBlock(pos,ToyContent.block("chao_lifesaver").defaultBlockState().setValue(ChaoToyBlock.WATERLOGGED,true),3);
    var c=actor(h,pos.north(2).above(),false,swimmer);var seen=new AtomicBoolean();var frames=new AtomicInteger();
    h.onEachTick(()->{
      if(!ToyContent.isPlaying(c))return;int elapsed=ToyContent.sessionElapsed(h.getLevel(),pos);
      if(elapsed<16||elapsed>140||elapsed%8<2)return;
      seen.set(true);int frame=ToyInteractionRules.mountedFrame(elapsed);frames.getAndUpdate(v->v|(1<<frame));
      h.assertTrue(Math.abs(c.getY()-(pos.getY()+.78+ToyInteractionRules.ringBob(frame)))<.02,"Actor rides water-surface ring, not submerged floor");
      h.assertTrue(c.isInWater()&&c.isNoGravity()&&c.getEyeY()>pos.getY()+1,"Ring supports actor with head above water");
      h.assertTrue(h.getLevel().getBlockState(pos).getValue(ChaoToyBlock.FRAME)==frame,"Floating mesh and actor bob share phase");
      h.assertTrue(ToyInteractionRules.supportedFloat(c.toyPlayingKind()),"Swim ability cannot select unsupported flailing for ring play");
      var floats=c.goalSelector.getAvailableGoals().stream().filter(g->g.getGoal() instanceof net.minecraft.world.entity.ai.goal.FloatGoal).toList();
      h.assertTrue(!floats.isEmpty()&&floats.stream().noneMatch(g->g.isRunning()||g.getGoal().canUse()),"Production FloatGoal cannot compete with supported floating");
    });
    h.succeedWhen(()->h.assertTrue(seen.get()&&Integer.bitCount(frames.get())==4&&!ToyContent.isPlaying(c)&&!c.isNoGravity()&&c.state().happiness>20,"Supported float completes for either swim skill"));
  }
  @GameTest(template="activity_test",timeoutTicks=1600) public static void lifesaverSupportsNonSwimmer(GameTestHelper h){ring(h,false);}
  @GameTest(template="activity_test",timeoutTicks=1600) public static void lifesaverSupportsSwimmer(GameTestHelper h){ring(h,true);}
  @GameTest(template="activity_test",timeoutTicks=1400)
  public static void drainedLifesaverCancelsAndRestoresGravity(GameTestHelper h) {
    floor(h);var pos=h.absolutePos(new BlockPos(9,0,9));h.getLevel().setBlock(pos,ToyContent.block("chao_lifesaver").defaultBlockState().setValue(ChaoToyBlock.WATERLOGGED,true),3);
    var c=actor(h,pos.north(2).above(),false,false);var done=new AtomicBoolean();
    h.onEachTick(()->{if(done.get()||!ToyContent.isPlaying(c)||ToyContent.sessionElapsed(h.getLevel(),pos)<40)return;done.set(true);
      h.getLevel().setBlock(pos,h.getLevel().getBlockState(pos).setValue(ChaoToyBlock.WATERLOGGED,false),3);
      h.runAfterDelay(8,()->{h.assertTrue(!ToyContent.isPlaying(c)&&!c.isNoGravity()&&c.state().happiness==20,"Drain interrupts supported float without reward");h.assertTrue(c.onGround()&&h.getLevel().noCollision(c,c.getBoundingBox()),"Drain returns actor to supported clear shore");h.succeed();});});
  }

  @GameTest(template="activity_test",timeoutTicks=1600)
  public static void ballNeverRollsIntoAnotherActor(GameTestHelper h) {
    floor(h);var pos=h.absolutePos(new BlockPos(9,1,9));h.getLevel().setBlock(pos,ToyContent.block("chao_ball").defaultBlockState(),3);
    var c=actor(h,pos.west(),false,false);var blocker=actor(h,pos.east(),false,false);blocker.setNoAi(true);
    var checked=new AtomicBoolean();var rolled=new AtomicBoolean();
    h.onEachTick(()->{
      if(!ToyContent.isPlaying(c))return;
      if(!ToyContent.target(c).equals(pos))rolled.set(true);
      if(!checked.get()&&c.toyKickElapsed()>=ToyInteractionRules.BALL_CONTACT_TICK) {
        checked.set(true);
        h.runAfterDelay(8,()->{
          h.assertTrue(h.getLevel().getBlockState(pos).is(ToyContent.block("chao_ball"))&&h.getLevel().getBlockState(pos.east()).isAir(),"Occupied destination cannot receive the solid ball");
          h.assertTrue(h.getLevel().noCollision(blocker,blocker.getBoundingBox()),"Blocker was not embedded in ball collision");blocker.discard();
        });
      }
    });
    h.succeedWhen(()->h.assertTrue(checked.get()&&rolled.get()&&!ToyContent.isPlaying(c)&&c.state().happiness>20,"Later clear destination permits normal contact/chase"));
  }
  private static void mountedObstruction(GameTestHelper h,String kind,boolean during) {
    floor(h);boolean water=kind.equals("chao_lifesaver");var pos=h.absolutePos(new BlockPos(9,water?0:1,9));
    var state=ToyContent.block(kind).defaultBlockState();if(water)state=state.setValue(ChaoToyBlock.WATERLOGGED,true);
    h.getLevel().setBlock(pos,state,3);if(!during)h.getLevel().setBlock(pos.above(),Blocks.STONE.defaultBlockState(),3);
    var c=actor(h,water?pos.north(2).above():pos.north(2),false,false);var done=new AtomicBoolean();
    h.onEachTick(()->{
      if(!during){h.assertTrue(!ToyContent.isPlaying(c),"Insufficient full-body headroom prevents mounting");return;}
      if(done.get()||!ToyContent.isPlaying(c)||ToyContent.sessionElapsed(h.getLevel(),pos)<40)return;done.set(true);
      h.getLevel().setBlock(pos.above(),Blocks.STONE.defaultBlockState(),3);
      h.runAfterDelay(8,()->{
        h.assertTrue(!ToyContent.isPlaying(c)&&!c.isNoGravity()&&c.state().happiness==20,"New obstacle cancels mounted session safely");
        h.assertTrue(c.onGround()&&h.getLevel().noCollision(c,c.getBoundingBox()),"Mounted movement never pins actor inside new obstacle");h.succeed();
      });
    });
    if(!during)h.runAfterDelay(450,h::succeed);
  }
  @GameTest(template="activity_test",timeoutTicks=500) public static void horseRejectsLowRoof(GameTestHelper h){mountedObstruction(h,"chao_rocking_horse",false);}
  @GameTest(template="activity_test",timeoutTicks=500) public static void ringRejectsLowRoof(GameTestHelper h){mountedObstruction(h,"chao_lifesaver",false);}
  @GameTest(template="activity_test",timeoutTicks=1400) public static void horseCancelsNewRoof(GameTestHelper h){mountedObstruction(h,"chao_rocking_horse",true);}
  @GameTest(template="activity_test",timeoutTicks=1400) public static void ringCancelsNewRoof(GameTestHelper h){mountedObstruction(h,"chao_lifesaver",true);}
}
