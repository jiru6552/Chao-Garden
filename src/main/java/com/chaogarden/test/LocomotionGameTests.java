package com.chaogarden.test;

import com.chaogarden.ChaoGarden;
import com.chaogarden.core.ChaoState;
import com.chaogarden.core.LocomotionRules;
import com.chaogarden.entity.ChaoEntity;
import java.util.EnumSet;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** Real navigation, water safety and bounded ledge-flight integration. */
@GameTestHolder(ChaoGarden.MODID)
@PrefixGameTestTemplate(false)
public final class LocomotionGameTests {
  private static ChaoEntity create(GameTestHelper helper, int x, int y, int z, boolean noAi) {
    var chao=ChaoGarden.CHAO.get().create(helper.getLevel());
    helper.assertTrue(chao!=null,"Could not create movement test Chao");
    chao.setState(new ChaoState());chao.setNoAi(noAi);chao.setInvulnerable(true);
    var at=helper.absolutePos(new BlockPos(x,y,z));
    chao.moveTo(at.getX()+.5,at.getY(),at.getZ()+.5,0,0);
    helper.getLevel().addFreshEntity(chao);return chao;
  }
  private static void floor(GameTestHelper helper) {
    for(int x=1;x<=27;x++)for(int z=1;z<=27;z++) {
      helper.setBlock(new BlockPos(x,0,z),Blocks.STONE);
      for(int y=1;y<=4;y++)helper.setBlock(new BlockPos(x,y,z),Blocks.AIR);
    }
  }

  @GameTest(template="empty",timeoutTicks=40)
  public static void pointChangesAndReloadImmediatelyUpdateRealMovementAttribute(GameTestHelper helper) {
    var chao=create(helper,2,2,2,true);
    double crawl=chao.getAttributeValue(Attributes.MOVEMENT_SPEED);
    chao.state().levels[ChaoState.RUN]=99;chao.sync();
    helper.assertTrue(chao.getAttributeValue(Attributes.MOVEMENT_SPEED)==crawl,
        "A displayed level bypassed point-based walking");
    chao.state().points[ChaoState.RUN]=50;chao.sync();
    helper.assertTrue(chao.getAttributeValue(Attributes.MOVEMENT_SPEED)>crawl*1.8,
        "Walking did not update movement immediately after training sync");
    chao.state().points[ChaoState.RUN]=1334;chao.sync();
    double dash=chao.getAttributeValue(Attributes.MOVEMENT_SPEED);
    var data=new CompoundTag();chao.saveWithoutId(data);
    var loaded=ChaoGarden.CHAO.get().create(helper.getLevel());loaded.load(data);
    helper.assertTrue(loaded.state().points[ChaoState.RUN]==1334
        && loaded.getAttributeValue(Attributes.MOVEMENT_SPEED)==dash,
        "Reloaded Chao temporarily reverted to a default speed");
    chao.discard();loaded.discard();helper.succeed();
  }

  @GameTest(template="activity_test",timeoutTicks=100)
  public static void actualNavigationUsesCrawlWalkRunAndDashSpeeds(GameTestHelper helper) {
    floor(helper);
    ChaoEntity[] chao=new ChaoEntity[4];double[] start=new double[4];
    int[] points={0,50,667,1334};
    for(int i=0;i<chao.length;i++) {
      var current=create(helper,3+i*5,1,3,false);chao[i]=current;start[i]=current.getZ();
      current.state().points[ChaoState.RUN]=points[i];current.sync();
      current.goalSelector.removeAllGoals(goal->true);
      var destination=helper.absolutePos(new BlockPos(3+i*5,1,24));
      current.goalSelector.addGoal(0,new Goal() {
        {setFlags(EnumSet.of(Flag.MOVE));}
        @Override public boolean canUse(){return true;}
        @Override public boolean canContinueToUse(){return true;}
        @Override public void tick(){current.getNavigation().moveTo(destination.getX()+.5,destination.getY(),destination.getZ()+.5,1);}
      });
    }
    helper.runAfterDelay(50,()->{
      double previous=0;
      for(int i=0;i<chao.length;i++) {
        double traveled=chao[i].getZ()-start[i];
        helper.assertTrue(traveled>previous+.5,"Real navigation did not distinguish successive movement milestones");
        previous=traveled;chao[i].discard();
      }
      helper.succeed();
    });
  }

  @GameTest(template="activity_test",timeoutTicks=50)
  public static void novicesFlailSafelyAndSwimTrainingChangesActualWaterSpeed(GameTestHelper helper) {
    floor(helper);
    for(int x=2;x<=12;x++)for(int z=2;z<=7;z++)for(int y=1;y<=4;y++)
      helper.setBlock(new BlockPos(x,y,z),Blocks.WATER);
    ChaoEntity[] chao=new ChaoEntity[3];int[] points={0,100,667};
    for(int i=0;i<chao.length;i++) {
      chao[i]=create(helper,3+i*4,2,4,true);chao[i].setNoGravity(true);
      chao[i].state().points[ChaoState.SWIM]=points[i];chao[i].sync();chao[i].setAirSupply(0);
    }
    helper.runAfterDelay(5,()->{
      double previous=0;
      for(var current:chao) {
        helper.assertTrue(current.isInWater(),"Swimming fixture left the water");
        helper.assertTrue(current.getAirSupply()==current.getMaxAirSupply(),
            "An untrained flailing Chao was exposed to a drowning penalty");
        double speed=current.getAttributeValue(Attributes.MOVEMENT_SPEED);
        helper.assertTrue(speed>previous,"Water propulsion ignored Swim progression or used Run instead");
        previous=speed;current.discard();
      }
      helper.succeed();
    });
  }

  @GameTest(template="activity_test",timeoutTicks=100)
  public static void ledgeFlightRequiresFlyPointsAndCannotRestartWithoutLanding(GameTestHelper helper) {
    floor(helper);
    var novice=create(helper,6,24,6,false);var trained=create(helper,12,24,6,false);
    novice.state().points[ChaoState.FLY]=134;novice.state().levels[ChaoState.FLY]=99;novice.sync();
    trained.state().points[ChaoState.FLY]=135;trained.sync();
    for(var current:new ChaoEntity[]{novice,trained}) {
      // NoAi also disables vanilla travel/gravity. Keep AI enabled but remove
      // goals so this is a real falling body with no autonomous navigation.
      current.goalSelector.removeAllGoals(goal->true);
      current.targetSelector.removeAllGoals(goal->true);
      current.setOnGround(false);
      current.setDeltaMovement(new Vec3(.12,-.3,0));current.fallDistance=2;
    }
    helper.runAfterDelay(4,()->{
      helper.assertTrue(!novice.isChaoGliding() && trained.isChaoGliding(),
          "Ledge flight did not require135 Fly points independently of level");
      helper.assertTrue(trained.getDeltaMovement().y>novice.getDeltaMovement().y
          && trained.fallDistance<.1,"Unlocked glide did not slow the actual fall");
    });
    helper.runAfterDelay(LocomotionRules.MAX_GLIDE_TICKS+8,()->{
      helper.assertTrue(!trained.onGround() && !trained.isInWater()
          && trained.getY()>helper.absolutePos(new BlockPos(12,2,6)).getY(),
          "Glide budget fixture landed before its midair-expiry assertion");
      helper.assertTrue(!trained.isChaoGliding(),"Garden flight exceeded its finite ledge-glide budget");
      trained.setDeltaMovement(.12,-.3,0);
      helper.runAfterDelay(3,()->{
        helper.assertTrue(!trained.onGround() && !trained.isChaoGliding(),
            "Glide restarted in midair without a landing");
        novice.discard();trained.discard();helper.succeed();
      });
    });
  }
}
