package com.chaogarden.test;

import com.chaogarden.ChaoGarden;
import com.chaogarden.core.ChaoPreferences;
import com.chaogarden.core.ChaoState;
import com.chaogarden.entity.ChaoEntity;
import com.chaogarden.toys.ChaoToyBlock;
import com.chaogarden.toys.ToyContent;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** Actual placement, navigation, voluntary needs checks and lifecycle ownership of toys. */
@GameTestHolder(ChaoGarden.MODID)
@PrefixGameTestTemplate(false)
public final class ToyGameTests {
  private static void floor(GameTestHelper helper) {
    for(int x=1;x<=20;x++)for(int z=1;z<=20;z++) {
      helper.setBlock(new BlockPos(x,0,z),Blocks.STONE);
      for(int y=1;y<=4;y++)helper.setBlock(new BlockPos(x,y,z),Blocks.AIR);
    }
  }
  private static ChaoEntity chao(GameTestHelper helper,int x,int z) {
    var c=ChaoGarden.CHAO.get().create(helper.getLevel());
    helper.assertTrue(c!=null,"Chao creates");
    var s=new ChaoState();s.adult=true;s.points[ChaoState.RUN]=667;
    s.happiness=20;s.hunger=90;s.energy=100;s.boredom=100;s.personality=ChaoPreferences.EASILY_BORED;
    for(String id:java.util.List.of("trumpet","drum","maracas","triangle","guitar","accordion","bell",
        "castanets","cymbals","flute","tambourine"))s.setTraitInt("learned."+id,1);
    c.setState(s);c.setInvulnerable(true);c.getRandom().setSeed(18);
    var at=helper.absolutePos(new BlockPos(x,1,z));c.moveTo(at.getX()+.5,at.getY(),at.getZ()+.5,0,0);
    helper.getLevel().addFreshEntity(c);
    c.goalSelector.removeAllGoals(g->true);c.targetSelector.removeAllGoals(g->true);
    c.goalSelector.addGoal(4,new ToyContent.PlayGoal(c));
    return c;
  }
  private static BlockPos toy(GameTestHelper helper,String id,int x,int z) {
    var at=helper.absolutePos(new BlockPos(x,1,z));
    helper.getLevel().setBlock(at,ToyContent.block(id).defaultBlockState(),3);return at;
  }
  private static long drops(GameTestHelper helper,BlockPos at,String id) {
    return helper.getLevel().getEntitiesOfClass(ItemEntity.class,new AABB(at).inflate(3),
        e->e.getItem().is(ChaoGarden.item(id))).stream().mapToLong(e->e.getItem().getCount()).sum();
  }
  @GameTest(template="activity_test",timeoutTicks=60)
  public static void allRegisteredToysPlaceFaceTheKeeperAndDropExactlyOnce(GameTestHelper helper) {
    floor(helper);var world=helper.getLevel();var keeper=helper.makeMockPlayer(GameType.SURVIVAL);
    keeper.setYRot(90);
    int index=0;
    for(String id:ToyContent.ids()) {
      var ground=helper.absolutePos(new BlockPos(2+(index%6)*3,0,2+(index/6)*4));
      keeper.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(ChaoGarden.item(id),2));
      var hit=new BlockHitResult(Vec3.atCenterOf(ground).add(0,.5,0),Direction.UP,ground,false);
      helper.assertTrue(keeper.getMainHandItem().getItem().useOn(new UseOnContext(keeper,InteractionHand.MAIN_HAND,hit)).consumesAction(),
          "Real BlockItem places "+id);
      var at=ground.above();var state=world.getBlockState(at);
      helper.assertTrue(state.is(ToyContent.block(id))&&state.getValue(ChaoToyBlock.FACING)==Direction.EAST,
          "Placed toy faces its keeper "+id);
      helper.assertTrue(keeper.getMainHandItem().getCount()==1,"Exactly one inventory item became a block "+id);
      helper.assertTrue(!state.getValue(ChaoToyBlock.PLAYING),"Placement does not force play");
      world.destroyBlock(at,true);
      helper.assertTrue(drops(helper,at,id)==1,"Breaking returns exactly one physical toy "+id);
      world.destroyBlock(at,true);
      helper.assertTrue(drops(helper,at,id)==1,"Repeated destruction cannot duplicate the toy "+id);
      index++;
    }
    helper.succeed();
  }
  @GameTest(template="activity_test",timeoutTicks=1200)
  public static void boredChaoNavigatesChoosesAPlacedTvAndCompletesPlay(GameTestHelper helper) {
    floor(helper);var c=chao(helper,2,5);var start=c.position();var pos=toy(helper,"chao_tv",6,5);
    var observed=new AtomicBoolean();var frames=new AtomicInteger();
    helper.onEachTick(()->{
      if(c.tickCount%160==0) {
        var path=c.getNavigation().getPath();
        com.mojang.logging.LogUtils.getLogger().info("CHAO_TOY_NAVIGATION tick={} pos={} velocity={} ground={} noAi={} gravity={} mayPlay={} rest={} eat={} item={} stay={} target={} path={} nextNode={} goalRunning={}",
            c.tickCount,c.position(),c.getDeltaMovement(),c.onGround(),c.isNoAi(),!c.isNoGravity(),ToyContent.mayPlay(c),c.isResting(),c.isEating(),c.isHandlingItem(),c.isStaying(),
            c.getPersistentData().contains("ChaoToyTarget")?ToyContent.target(c):"none",path==null?"none":path.getEndNode(),path==null?-1:path.getNextNodeIndex(),
            c.goalSelector.getAvailableGoals().stream().anyMatch(g->g.isRunning()&&g.getGoal() instanceof ToyContent.PlayGoal));
      }
      if(ToyContent.isPlaying(c)) {
        observed.set(true);var state=helper.getLevel().getBlockState(pos);
        if(state.is(ToyContent.block("chao_tv"))&&state.getValue(ChaoToyBlock.PLAYING))
          frames.getAndUpdate(bits->bits|(1<<state.getValue(ChaoToyBlock.FRAME)));
      }
    });
    helper.succeedWhen(()->{
      helper.assertTrue(observed.get(),"The Chao voluntarily started a real play session; position="+c.position()+", start="+start+", eligible="+ToyContent.mayPlay(c)+", target="+c.getPersistentData().getLong("ChaoToyTarget"));
      helper.assertTrue(!ToyContent.isPlaying(c)&&c.state().happiness>20,"Reward occurs after finishing, not selection");
      helper.assertTrue(c.distanceToSqr(start)>2,"Actual navigation approached the toy");
      helper.assertTrue(Integer.bitCount(frames.get())>=2,"The placed toy changed physical animation frames");
      helper.assertTrue(c.state().boredom<75,"Completed play relieves boredom");
      helper.assertTrue(c.getPersistentData().getLong("ChaoToyNextPlay")>helper.getLevel().getGameTime(),"A personal rest cooldown is active");
      helper.assertTrue(helper.getLevel().getBlockState(pos).is(ToyContent.block("chao_tv"))&&drops(helper,pos,"chao_tv")==0,
          "The toy remains placed without item copies");
    });
  }
  @GameTest(template="activity_test",timeoutTicks=1200)
  public static void hungryChaoDeclinesToysUntilItsNeedsAreMet(GameTestHelper helper) {
    floor(helper);var c=chao(helper,2,5);var pos=toy(helper,"chao_drum",3,5);
    c.state().hunger=10;c.sync();
    helper.runAfterDelay(180,()->{
      helper.assertTrue(!ToyContent.isPlaying(c)&&c.state().happiness==20,"Hungry Chao is not forced to play");
      helper.assertTrue(!helper.getLevel().getBlockState(pos).getValue(ChaoToyBlock.PLAYING),"Unchosen toy remains quiet");
      c.state().hunger=90;c.state().energy=100;c.state().sleeping=false;c.sync();
      helper.succeedWhen(()->helper.assertTrue(c.state().happiness>20,"A fed Chao can later choose and finish play"));
    });
  }
  @GameTest(template="activity_test",timeoutTicks=1200)
  public static void breakingAnOccupiedToyCancelsPlayAndReturnsOnlyItsBlockItem(GameTestHelper helper) {
    floor(helper);var c=chao(helper,2,5);var pos=toy(helper,"chao_jack_in_box",3,5);var broken=new AtomicBoolean();
    helper.onEachTick(()->{
      if(!broken.get()&&ToyContent.isPlaying(c)) {
        broken.set(true);helper.getLevel().destroyBlock(pos,true);
        helper.runAfterDelay(ToyContent.PLAY_TICKS+20,()->{
          helper.assertTrue(!ToyContent.isPlaying(c)&&c.state().happiness==20,"Interrupted play grants no completion reward");
          helper.assertTrue(helper.getLevel().getBlockState(pos).isAir()&&drops(helper,pos,"chao_jack_in_box")==1,
              "Only the real block item is recovered");
          helper.assertTrue(helper.getLevel().getEntitiesOfClass(Display.ItemDisplay.class,new AABB(pos).inflate(3)).isEmpty(),
              "No legacy display props are created");
          helper.succeed();
        });
      }
    });
  }
  @GameTest(template="activity_test",timeoutTicks=1200)
  public static void savingAnUnfinishedSessionDoesNotDuplicateAToyOrGrantAReward(GameTestHelper helper) {
    floor(helper);var c=chao(helper,2,5);var pos=toy(helper,"chao_radio",3,5);var reloaded=new AtomicBoolean();
    helper.onEachTick(()->{
      if(!reloaded.get()&&ToyContent.isPlaying(c)) {
        reloaded.set(true);var data=new CompoundTag();c.saveWithoutId(data);long cooldown=c.getPersistentData().getLong("ChaoToyNextPlay");
        c.discard();var loaded=ChaoGarden.CHAO.get().create(helper.getLevel());loaded.load(data);helper.getLevel().addFreshEntity(loaded);
        helper.runAfterDelay(12,()->{
          helper.assertTrue(!ToyContent.isPlaying(loaded)&&loaded.state().happiness==20,"Reload safely cancels unfinished play");
          helper.assertTrue(loaded.getPersistentData().getLong("ChaoToyNextPlay")==cooldown,"Reload retains the saved rest cooldown");
          helper.assertTrue(helper.getLevel().getBlockState(pos).is(ToyContent.block("chao_radio"))
              &&!helper.getLevel().getBlockState(pos).getValue(ChaoToyBlock.PLAYING)&&drops(helper,pos,"chao_radio")==0,
              "The block survives and returns to idle without extra items");
          helper.succeed();
        });
      }
    });
  }
  @GameTest(template="activity_test",timeoutTicks=1200)
  public static void ballIsOnePhysicalBlockThatActuallyRollsAndIsChased(GameTestHelper h) {
    floor(h);var c=chao(h,2,5);var original=toy(h,"chao_ball",3,5);
    var rolled=new AtomicBoolean();var approached=new AtomicBoolean();Vec3 start=c.position();
    h.onEachTick(()->{
      if(ToyContent.isPlaying(c)&&!ToyContent.target(c).equals(original))rolled.set(true);
      if(c.distanceToSqr(start)>1)approached.set(true);
    });
    h.succeedWhen(()->{
      h.assertTrue(rolled.get()&&approached.get(),"The actual ball changes cells and the Chao follows it");
      h.assertTrue(c.state().happiness>20&&!ToyContent.isPlaying(c),"A complete chase earns one play reward");
      int count=0;for(var pos:BlockPos.betweenClosed(h.absolutePos(new BlockPos(1,1,1)),h.absolutePos(new BlockPos(20,1,20))))
        if(h.getLevel().getBlockState(pos).is(ToyContent.block("chao_ball")))count++;
      h.assertTrue(count==1&&drops(h,original,"chao_ball")==0,"Rolling moves one real block without drops or copies");
    });
  }
  @GameTest(template="activity_test",timeoutTicks=1200)
  public static void horseIsActuallyRiddenAndBreakingItSafelyDismounts(GameTestHelper h) {
    floor(h);var c=chao(h,2,5);var pos=toy(h,"chao_rocking_horse",3,5);var seen=new AtomicBoolean();
    h.onEachTick(()->{
      if(!seen.get()&&ToyContent.isPlaying(c)&&c.isNoGravity()
          &&c.position().subtract(Vec3.atCenterOf(pos)).horizontalDistanceSqr()<.2) {
        seen.set(true);h.getLevel().destroyBlock(pos,true);
        h.runAfterDelay(12,()->{
          h.assertTrue(!ToyContent.isPlaying(c)&&!c.isNoGravity()&&c.state().happiness==20,
              "Interrupted riding releases the mount and awards no completion");
          h.assertTrue(c.onGround()&&c.distanceToSqr(Vec3.atCenterOf(pos))<9,"Dismount returns to a safe adjacent standing cell");
          h.assertTrue(drops(h,pos,"chao_rocking_horse")==1,"Breaking recovers exactly the one real horse");h.succeed();
        });
      }
    });
  }
  @GameTest(template="activity_test",timeoutTicks=1200)
  public static void lifesaverFloatsInWaterAndRestoresNormalGravityAfterPlay(GameTestHelper h) {
    floor(h);var c=chao(h,2,5);var pos=h.absolutePos(new BlockPos(3,0,5));
    h.getLevel().setBlock(pos,ToyContent.block("chao_lifesaver").defaultBlockState().setValue(ChaoToyBlock.WATERLOGGED,true),3);
    var floated=new AtomicBoolean();h.onEachTick(()->{
      if(ToyContent.isPlaying(c)&&c.isNoGravity()&&c.isInWater())floated.set(true);
    });
    h.succeedWhen(()->{
      h.assertTrue(floated.get(),"Chao really enters the floating lifesaver in water");
      h.assertTrue(!ToyContent.isPlaying(c)&&!c.isNoGravity()&&c.state().happiness>20,
          "A completed float releases the Chao and grants one play reward");
      h.assertTrue(h.getLevel().getBlockState(pos).getValue(ChaoToyBlock.WATERLOGGED),"The placed ring retains its water");
    });
  }
}
