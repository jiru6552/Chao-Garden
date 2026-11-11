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
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
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
  public static void coffinOpensRestsAndExitsBeforeClosing(GameTestHelper h) {
    floor(h);var pos=h.absolutePos(new BlockPos(9,1,9));h.getLevel().setBlock(pos,ToyContent.block("chao_coffin").defaultBlockState(),3);
    var c=actor(h,pos.north(2));var open=new AtomicBoolean();var rest=new AtomicBoolean();var exit=new AtomicBoolean();
    var previousFrame=new AtomicInteger();var openings=new AtomicInteger();
    h.onEachTick(()->{
      if(!ToyContent.isPlaying(c))return;
      int elapsed=ToyContent.sessionElapsed(h.getLevel(),pos),frame=h.getLevel().getBlockState(pos).getValue(ChaoToyBlock.FRAME);
      if(previousFrame.getAndSet(frame)==0&&frame==1)openings.incrementAndGet();
      if(frame==3)open.set(true);
      if(elapsed>48&&elapsed<110){rest.set(true);h.assertTrue(frame==3,"Coffin rests open without flutter");h.assertTrue(c.isNoGravity(),"Occupied coffin owns gravity");}
      if(elapsed>145){exit.set(true);h.assertTrue(c.position().subtract(Vec3.atCenterOf(pos)).horizontalDistanceSqr()>.8,"Exit before final lid closure");}
    });
    h.succeedWhen(()->{h.assertTrue(open.get()&&rest.get()&&exit.get()&&openings.get()==1,"Observe one opening and complete coffin choreography");h.assertTrue(!ToyContent.isPlaying(c)&&!c.isNoGravity()&&c.state().happiness>20,"Completion restores gravity and grants reward");});
  }
  @GameTest(template="activity_test",timeoutTicks=1400)
  public static void coffinReloadCancelsMountWithoutReward(GameTestHelper h) {
    floor(h);var pos=h.absolutePos(new BlockPos(9,1,9));h.getLevel().setBlock(pos,ToyContent.block("chao_coffin").defaultBlockState(),3);
    var c=actor(h,pos.north(2));var done=new AtomicBoolean();
    h.onEachTick(()->{
      if(done.get()||!ToyContent.isPlaying(c)||ToyContent.sessionElapsed(h.getLevel(),pos)<60)return;done.set(true);
      var save=new CompoundTag();c.saveWithoutId(save);c.discard();
      var loaded=ChaoGarden.CHAO.get().create(h.getLevel());h.assertTrue(loaded!=null,"Reload actor creates");loaded.load(save);h.getLevel().addFreshEntity(loaded);
      h.runAfterDelay(8,()->{h.assertTrue(!ToyContent.isPlaying(loaded)&&!loaded.isNoGravity()&&loaded.state().happiness==20,"Reload releases mount without completion reward");h.assertTrue(!h.getLevel().getBlockState(pos).getValue(ChaoToyBlock.PLAYING),"Orphan lid closes");h.succeed();});
    });
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
  public static void breakingOccupiedCoffinSafelyReleasesChao(GameTestHelper h) {
    floor(h);var pos=h.absolutePos(new BlockPos(9,1,9));h.getLevel().setBlock(pos,ToyContent.block("chao_coffin").defaultBlockState(),3);
    var c=actor(h,pos.north(2));var done=new AtomicBoolean();
    h.onEachTick(()->{
      if(done.get()||!ToyContent.isPlaying(c)||ToyContent.sessionElapsed(h.getLevel(),pos)<60)return;done.set(true);
      h.getLevel().destroyBlock(pos,true);
      h.runAfterDelay(8,()->{h.assertTrue(!ToyContent.isPlaying(c)&&!c.isNoGravity()&&c.state().happiness==20,"Broken coffin releases without reward");h.assertTrue(h.getLevel().noCollision(c,c.getBoundingBox())&&c.onGround(),"Safe grounded dismount");h.succeed();});
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

  @GameTest(template="activity_test",timeoutTicks=1400)
  public static void obstructedCoffinExitUsesSafeAlternativeWithoutReward(GameTestHelper h) {
    floor(h);var pos=h.absolutePos(new BlockPos(9,1,9));h.getLevel().setBlock(pos,ToyContent.block("chao_coffin").defaultBlockState(),3);
    var c=actor(h,pos.north(2));var done=new AtomicBoolean();
    h.onEachTick(()->{
      if(done.get()||!ToyContent.isPlaying(c)||ToyContent.sessionElapsed(h.getLevel(),pos)<60)return;done.set(true);
      h.getLevel().setBlock(pos.north(),Blocks.STONE.defaultBlockState(),3);
      h.runAfterDelay(100,()->{
        h.assertTrue(!ToyContent.isPlaying(c)&&!c.isNoGravity()&&c.state().happiness==20,"Obstructed exit interrupts safely without reward");
        h.assertTrue(h.getLevel().noCollision(c,c.getBoundingBox())&&c.onGround(),"Fallback dismount is supported and clear");
        h.assertTrue(c.distanceToSqr(Vec3.atBottomCenterOf(pos.north()))>.5,"Never return inside blocked stand");h.succeed();
      });
    });
  }

  @GameTest(template="activity_test",timeoutTicks=1400)
  public static void allImmediateCoffinExitsBlockedFindsValidatedWiderFallback(GameTestHelper h) {
    floor(h);var pos=h.absolutePos(new BlockPos(9,1,9));h.getLevel().setBlock(pos,ToyContent.block("chao_coffin").defaultBlockState(),3);
    var c=actor(h,pos.north(2));var done=new AtomicBoolean();
    h.onEachTick(()->{
      if(done.get()||!ToyContent.isPlaying(c)||ToyContent.sessionElapsed(h.getLevel(),pos)<60)return;done.set(true);
      for(int dx=-1;dx<=1;dx++)for(int dz=-1;dz<=1;dz++)for(int dy=0;dy<=4;dy++)
        if(dx!=0||dz!=0||dy!=0)h.getLevel().setBlock(pos.offset(dx,dy,dz),Blocks.STONE.defaultBlockState(),3);
      ToyContent.endPlaying(c,false);
      h.assertTrue(!c.getPersistentData().getBoolean("ChaoToyMounted")&&!c.isNoGravity(),"Wider validated fallback completes dismount");
      h.assertTrue(h.getLevel().noCollision(c,c.getBoundingBox()),"All cardinal exits blocked: never clear mount inside coffin");
      h.assertTrue(c.position().subtract(Vec3.atBottomCenterOf(pos)).horizontalDistanceSqr()>=4,"Use wider clear stand when adjacent and top are blocked");
      h.runAfterDelay(8,()->{h.assertTrue(c.onGround()&&c.state().happiness==20,"Fallback is supported and interruption unrewarded");h.succeed();});
    });
  }
  @GameTest(template="activity_test",timeoutTicks=1400)
  public static void fullySealedCoffinRetainsRecoveryAcrossReloadUntilSafeSpaceOpens(GameTestHelper h) {
    floor(h);var pos=h.absolutePos(new BlockPos(9,1,9));h.getLevel().setBlock(pos,ToyContent.block("chao_coffin").defaultBlockState(),3);
    var c=actor(h,pos.north(2));c.setInvulnerable(false);var done=new AtomicBoolean();
    h.onEachTick(()->{
      if(done.get()||!ToyContent.isPlaying(c)||ToyContent.sessionElapsed(h.getLevel(),pos)<60)return;done.set(true);
      for(int dx=-4;dx<=4;dx++)for(int dz=-4;dz<=4;dz++)for(int dy=0;dy<=4;dy++)
        if(dx!=0||dz!=0||dy!=0)h.getLevel().setBlock(pos.offset(dx,dy,dz),Blocks.STONE.defaultBlockState(),3);
      ToyContent.endPlaying(c,false);
      h.assertTrue(c.getPersistentData().getBoolean("ChaoToyDismountWait")&&c.getPersistentData().getBoolean("ChaoToyMounted"),"No safe position: preserve recovery metadata");
      h.assertTrue(c.isNoGravity()&&c.isNoAi()&&!ToyContent.mayPlay(c)&&!ToyContent.isPlaying(c),"Suspend movement and forbid new play while sealed");
      h.getLevel().setBlock(pos,Blocks.STONE.defaultBlockState(),3);
      float health=c.getHealth();c.hurt(c.damageSources().inWall(),2);
      h.assertTrue(c.getHealth()==health,"Pending recovery suppresses wall suffocation, not just movement");
      var save=new CompoundTag();c.saveWithoutId(save);c.discard();
      var loaded=ChaoGarden.CHAO.get().create(h.getLevel());h.assertTrue(loaded!=null,"Reload recovery actor creates");loaded.load(save);h.getLevel().addFreshEntity(loaded);
      h.runAfterDelay(5,()->{
        h.assertTrue(loaded.isAlive()&&loaded.getHealth()==health&&!loaded.isInvulnerable(),"Non-invulnerable actor survives sealed overlap across reload");
        h.assertTrue(loaded.getPersistentData().getBoolean("ChaoToyDismountWait")&&loaded.isNoGravity()&&loaded.isNoAi(),"Reload retains original recovery ownership");
        h.getLevel().setBlock(pos.east(3),Blocks.AIR.defaultBlockState(),3);
        h.getLevel().setBlock(pos.east(3).above(),Blocks.AIR.defaultBlockState(),3);
        h.runAfterDelay(25,()->{
          h.assertTrue(!loaded.getPersistentData().getBoolean("ChaoToyMounted")&&!loaded.getPersistentData().contains("ChaoToyDismountWait"),"Clear recovery only after validated relocation");
          h.assertTrue(!loaded.isNoGravity()&&!loaded.isNoAi()&&loaded.onGround(),"Restore original gravity and AI exactly once");
          h.assertTrue(h.getLevel().noCollision(loaded,loaded.getBoundingBox())&&loaded.state().happiness==20,"Recovery is collision-free and never grants completion reward");h.succeed();
        });
      });
    });
  }

  @GameTest(template="activity_test",timeoutTicks=1400)
  public static void externallyRescuedPendingDismountRestoresStateFarFromOldToy(GameTestHelper h) {
    floor(h);var pos=h.absolutePos(new BlockPos(9,1,9));h.getLevel().setBlock(pos,ToyContent.block("chao_coffin").defaultBlockState(),3);
    var c=actor(h,pos.north(2));var done=new AtomicBoolean();
    h.onEachTick(()->{
      if(done.get()||!ToyContent.isPlaying(c)||ToyContent.sessionElapsed(h.getLevel(),pos)<60)return;done.set(true);
      for(int dx=-4;dx<=4;dx++)for(int dz=-4;dz<=4;dz++)for(int dy=0;dy<=4;dy++)
        if(dx!=0||dz!=0||dy!=0)h.getLevel().setBlock(pos.offset(dx,dy,dz),Blocks.STONE.defaultBlockState(),3);
      ToyContent.endPlaying(c,false);
      h.assertTrue(c.getPersistentData().getBoolean("ChaoToyDismountWait"),"Fixture enters pending recovery");
      Vec3 rescue=Vec3.atBottomCenterOf(pos.east(7));c.setPos(rescue.x,rescue.y,rescue.z);
      h.runAfterDelay(15,()->{
        h.assertTrue(!c.getPersistentData().getBoolean("ChaoToyMounted")&&!c.isNoAi()&&!c.isNoGravity(),"Safe external rescue restores original state even beyond original search radius");
        h.assertTrue(c.distanceToSqr(rescue)<.1&&h.getLevel().noCollision(c,c.getBoundingBox())&&c.onGround(),"Keep externally rescued actor at safe destination");h.succeed();
      });
    });
  }
}
