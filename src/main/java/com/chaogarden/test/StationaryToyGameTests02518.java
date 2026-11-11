package com.chaogarden.test;

import com.chaogarden.ChaoGarden;
import com.chaogarden.core.ChaoPreferences;
import com.chaogarden.core.ChaoState;
import com.chaogarden.entity.ChaoEntity;
import com.chaogarden.toys.ChaoToyBlock;
import com.chaogarden.toys.ToyContent;
import com.chaogarden.toys.ToyInteractionRules;
import java.util.concurrent.atomic.AtomicBoolean;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** Free-play stationary contact/viewing and continued environment validation. */
@GameTestHolder(ChaoGarden.MODID)
@PrefixGameTestTemplate(false)
public final class StationaryToyGameTests02518 {
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
    for(String id:java.util.List.of("trumpet","drum","maracas","triangle","guitar","accordion","bell","castanets","cymbals","flute","tambourine"))state.setTraitInt("learned."+id,1);
    state.setTraitInt("reward.shovel",1);c.setState(state);c.setInvulnerable(true);c.getRandom().setSeed(18);
    c.moveTo(at.getX()+.5,at.getY(),at.getZ()+.5,0,0);h.getLevel().addFreshEntity(c);
    c.goalSelector.removeAllGoals(g->true);c.targetSelector.removeAllGoals(g->true);c.goalSelector.addGoal(4,new ToyContent.PlayGoal(c));return c;
  }
  private static BlockPos toy(GameTestHelper h,String kind,boolean wet) {
    var pos=h.absolutePos(new BlockPos(9,wet?0:1,9));
    if(kind.equals("chao_shovel")){h.getLevel().setBlock(pos.below(2),Blocks.STONE.defaultBlockState(),3);h.getLevel().setBlock(pos.below(),Blocks.SAND.defaultBlockState(),3);}
    h.getLevel().setBlock(pos,ToyContent.block(kind).defaultBlockState().setValue(ChaoToyBlock.WATERLOGGED,wet),3);return pos;
  }
  private static void complete(GameTestHelper h,String kind) {
    floor(h);var pos=toy(h,kind,false);var c=actor(h,pos.west(2));var seen=new AtomicBoolean();
    h.onEachTick(()->{
      if(!ToyContent.isPlaying(c))return;seen.set(true);
      Direction side=Direction.getNearest(c.getX()-pos.getX()-.5,0,c.getZ()-pos.getZ()-.5);
      Vec3 anchor=ToyContent.stationaryAnchor(c,pos,pos.relative(side));
      h.assertTrue(c.distanceToSqr(anchor)<.007,"Stationary session begins/continues at precise anchor");
      h.assertTrue(h.getLevel().noCollision(c,c.getBoundingBox()),"Actor stays outside physical prop");
      if(ToyInteractionRules.viewing(kind))h.assertTrue(c.position().subtract(Vec3.atBottomCenterOf(pos)).horizontalDistanceSqr()>.9,"Viewing does not collapse into instrument contact");
      if(ToyInteractionRules.frontOnly(kind))h.assertTrue(side==Direction.NORTH,"Directional prop remains front-only");
    });
    h.succeedWhen(()->{
      h.assertTrue(seen.get()&&!ToyContent.isPlaying(c),"Stationary play completes or authored surprise ends it");
      if(!kind.equals("chao_jack_in_box"))h.assertTrue(c.state().happiness>20,"Completed stationary play earns reward");
    });
  }
  @GameTest(template="activity_test",timeoutTicks=1600) public static void trumpetAnchor(GameTestHelper h){complete(h,"chao_trumpet");}
  @GameTest(template="activity_test",timeoutTicks=1600) public static void drumAnchor(GameTestHelper h){complete(h,"chao_drum");}
  @GameTest(template="activity_test",timeoutTicks=1600) public static void jackInBoxAnchor(GameTestHelper h){complete(h,"chao_jack_in_box");}
  @GameTest(template="activity_test",timeoutTicks=1600) public static void radioAnchor(GameTestHelper h){complete(h,"chao_radio");}
  @GameTest(template="activity_test",timeoutTicks=1600) public static void tvAnchor(GameTestHelper h){complete(h,"chao_tv");}
  @GameTest(template="activity_test",timeoutTicks=1600) public static void rubberDuckAnchor(GameTestHelper h){complete(h,"chao_rubber_duck");}
  @GameTest(template="activity_test",timeoutTicks=1600) public static void triangleAnchor(GameTestHelper h){complete(h,"chao_triangle");}
  @GameTest(template="activity_test",timeoutTicks=1600) public static void guitarAnchor(GameTestHelper h){complete(h,"chao_guitar");}
  @GameTest(template="activity_test",timeoutTicks=1600) public static void accordionAnchor(GameTestHelper h){complete(h,"chao_accordion");}
  @GameTest(template="activity_test",timeoutTicks=1600) public static void bubbleWandAnchor(GameTestHelper h){complete(h,"chao_bubble_wand");}
  @GameTest(template="activity_test",timeoutTicks=1600) public static void rattleAnchor(GameTestHelper h){complete(h,"chao_rattle");}
  @GameTest(template="activity_test",timeoutTicks=1600) public static void maracasAnchor(GameTestHelper h){complete(h,"chao_maracas");}
  @GameTest(template="activity_test",timeoutTicks=1600) public static void bellAnchor(GameTestHelper h){complete(h,"chao_bell");}
  @GameTest(template="activity_test",timeoutTicks=1600) public static void castanetsAnchor(GameTestHelper h){complete(h,"chao_castanets");}
  @GameTest(template="activity_test",timeoutTicks=1600) public static void cymbalsAnchor(GameTestHelper h){complete(h,"chao_cymbals");}
  @GameTest(template="activity_test",timeoutTicks=1600) public static void fluteAnchor(GameTestHelper h){complete(h,"chao_flute");}
  @GameTest(template="activity_test",timeoutTicks=1600) public static void tambourineAnchor(GameTestHelper h){complete(h,"chao_tambourine");}
  @GameTest(template="activity_test",timeoutTicks=1600) public static void shovelAnchor(GameTestHelper h){complete(h,"chao_shovel");}
  private static void interrupted(GameTestHelper h,String kind,String change) {
    floor(h);boolean wet=change.equals("drain");var pos=toy(h,kind,wet);var c=actor(h,wet?pos.north(2).above():pos.west(2));var changed=new AtomicBoolean();
    h.onEachTick(()->{
      if(changed.get()||!ToyContent.isPlaying(c)||ToyContent.sessionElapsed(h.getLevel(),pos)<12)return;changed.set(true);
      switch(change) {
        case "move"->c.setPos(c.getX()+2,c.getY(),c.getZ());
        case "sand"->h.getLevel().setBlock(pos.below(),Blocks.STONE.defaultBlockState(),3);
        case "drain"->h.getLevel().setBlock(pos,h.getLevel().getBlockState(pos).setValue(ChaoToyBlock.WATERLOGGED,false),3);
        case "rotate"->h.getLevel().setBlock(pos,h.getLevel().getBlockState(pos).setValue(ChaoToyBlock.FACING,Direction.SOUTH),3);
        case "floor"->{for(int x=-1;x<=1;x++)for(int z=-1;z<=1;z++)h.getLevel().setBlock(pos.offset(x,-1,z),Blocks.AIR.defaultBlockState(),3);}
        case "obstruct"->h.getLevel().setBlock(c.blockPosition(),Blocks.STONE.defaultBlockState(),3);
        default->throw new AssertionError(change);
      }
      h.runAfterDelay(8,()->{
        h.assertTrue(!ToyContent.isPlaying(c)&&c.state().happiness==20,"Invalid reach/environment cancels without completion reward");
        h.assertTrue(!h.getLevel().getBlockState(pos).getValue(ChaoToyBlock.PLAYING),"Interrupted stationary block returns to idle");h.succeed();
      });
    });
  }
  @GameTest(template="activity_test",timeoutTicks=1400) public static void displacedRadioListenerCancels(GameTestHelper h){interrupted(h,"chao_radio","move");}
  @GameTest(template="activity_test",timeoutTicks=1400) public static void displacedTrumpetPlayerCancels(GameTestHelper h){interrupted(h,"chao_trumpet","move");}
  @GameTest(template="activity_test",timeoutTicks=1400) public static void shovelRequiresSandThroughoutPlay(GameTestHelper h){interrupted(h,"chao_shovel","sand");}
  @GameTest(template="activity_test",timeoutTicks=1400) public static void drainedDuckEndsShoreSession(GameTestHelper h){interrupted(h,"chao_rubber_duck","drain");}
  @GameTest(template="activity_test",timeoutTicks=1400) public static void rotatedTvCancelsOldViewingSide(GameTestHelper h){interrupted(h,"chao_tv","rotate");}
  @GameTest(template="activity_test",timeoutTicks=1400) public static void lostFloorCancelsDrumming(GameTestHelper h){interrupted(h,"chao_drum","floor");}
  @GameTest(template="activity_test",timeoutTicks=1400) public static void obstructedAnchorCancelsBell(GameTestHelper h){interrupted(h,"chao_bell","obstruct");}
  private static void deadline(GameTestHelper h,boolean removeToy) {
    floor(h);var pos=toy(h,"chao_radio",false);var c=actor(h,pos.west(2));
    var reached=new AtomicBoolean();var terminalTick=new java.util.concurrent.atomic.AtomicLong(-1);
    h.onEachTick(()->{
      long until=c.getPersistentData().getLong("ChaoToyUntil");
      if(!reached.get()&&until>0&&h.getLevel().getGameTime()>=until-1) {
        reached.set(true);terminalTick.set(c.tickCount);
        h.assertTrue(c.state().happiness==20,"No reward before terminal session processing");
        // The pose is physically supported, even if a zero-motion tick cleared vanilla's flag.
        c.setOnGround(false);
        if(removeToy)h.getLevel().destroyBlock(pos,false);
        else h.assertTrue(ToyContent.mayPlay(c),"Owned terminal session remains eligible for its completion tick");
      }
    });
    h.succeedWhen(()->{
      h.assertTrue(reached.get()&&c.tickCount>=terminalTick.get()+12,"Observe completion deadline and following ticks");
      h.assertTrue(!ToyContent.isPlaying(c)&&!c.getPersistentData().contains("ChaoToyUntil"),"Session ownership cleared");
      h.assertTrue(c.state().happiness==(removeToy?20:24),"Exactly one valid reward; removed toy never rewards");
    });
  }
  @GameTest(template="activity_test",timeoutTicks=1600) public static void stationaryDeadlinePaysExactlyOnce(GameTestHelper h){deadline(h,false);}
  @GameTest(template="activity_test",timeoutTicks=1600) public static void removedToyBeforeDeadlineCannotReward(GameTestHelper h){deadline(h,true);}

  @GameTest(template="activity_test",timeoutTicks=700)
  public static void blockedApproachReleasesItsReservation(GameTestHelper h) {
    floor(h);var pos=toy(h,"chao_tv",false);var c=actor(h,pos.west(6));
    var trapped=new AtomicBoolean();var trappedAt=new java.util.concurrent.atomic.AtomicLong(-1);
    h.onEachTick(()->{
      if(trapped.get()||!c.getPersistentData().contains("ChaoToyTarget")||ToyContent.isPlaying(c))return;
      trapped.set(true);trappedAt.set(c.tickCount);var cell=c.blockPosition();
      for(int dx=-1;dx<=1;dx++)for(int dz=-1;dz<=1;dz++)if(dx!=0||dz!=0)
        for(int dy=0;dy<2;dy++)h.getLevel().setBlock(cell.offset(dx,dy,dz),Blocks.STONE.defaultBlockState(),3);
      h.getLevel().setBlock(cell.above(2),Blocks.STONE.defaultBlockState(),3);
    });
    h.succeedWhen(()->{
      h.assertTrue(trapped.get()&&c.tickCount>=trappedAt.get()+400,"Observe bounded blocked approach");
      h.assertTrue(!c.getPersistentData().contains("ChaoToyTarget")&&!ToyContent.isPlaying(c),"Blocked route releases toy reservation");
      h.assertTrue(c.state().happiness==20,"Blocked route earns no reward");
    });
  }

}
