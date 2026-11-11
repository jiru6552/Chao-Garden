package com.chaogarden.test;

import com.chaogarden.companion.AccessoriesBridge;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** Existing equipped behavior probes, requiring the real optional runtime instead of skipping. */
@GameTestHolder("chao_equipped46") @PrefixGameTestTemplate(false)
public final class EquippedGameTests046 {
  private static void realRuntime(GameTestHelper h){
    h.assertTrue(AccessoriesBridge.available(),"Real Accessories runtime required; this suite must not silently skip");
  }

  @GameTest(template="empty",timeoutTicks=60,batch="equipped46_skills")
  public static void nativeElytraAndArmor(GameTestHelper h){
    realRuntime(h);CompanionSkillGameTests.nativeElytraPreservedAndArmorAloneCannotFly(h);
  }
  @GameTest(template="empty",timeoutTicks=100,batch="equipped46_skills")
  public static void actualSlotFlightAndFiniteBudget(GameTestHelper h){
    realRuntime(h);CompanionSkillGameTests.actualSlotFlightUsesVanillaPhysicsAndTransitionsToFiniteBudget(h);
  }
  @GameTest(template="empty",timeoutTicks=110,batch="equipped46_skills")
  public static void waterBonusesAndPotionPreservation(GameTestHelper h){
    realRuntime(h);CompanionSkillGameTests.actualWaterBonusesPreservePotionsAndClearOnExit(h);
  }
  @GameTest(template="empty",timeoutTicks=190,batch="equipped46_skills")
  public static void robotBoostReplayAndCooldown(GameTestHelper h){
    realRuntime(h);CompanionSkillGameTests.actualSRobotPayloadBoostRejectsReplayAndRetainsCooldown(h);
  }
  @GameTest(template="empty",timeoutTicks=110,batch="equipped46_skills")
  public static void finiteGlideAndLandingReset(GameTestHelper h){
    realRuntime(h);CompanionSkillGameTests.actualFiniteGlideDoesNotRenewMidairAndResetsOnLanding(h);
  }
  @GameTest(template="empty",timeoutTicks=100,batch="equipped46_skills")
  public static void packetMovementCannotRenewBudget(GameTestHelper h){
    realRuntime(h);CompanionSkillGameTests.actualZeroVelocityMovementPacketsCannotRenewFiniteGlide(h);
  }
  @GameTest(template="empty",timeoutTicks=110,batch="equipped46_skills")
  public static void repeatedLaunchesCannotExtendBudget(GameTestHelper h){
    realRuntime(h);CompanionSkillGameTests.twentyMidairLaunchCyclesCannotExtendFiniteElytra(h);
  }
  @GameTest(template="empty",timeoutTicks=80,batch="equipped46_skills")
  public static void rotatingRobotsCannotBypassCooldown(GameTestHelper h){
    realRuntime(h);CompanionSkillGameTests.actualTwoSRobotsCannotCyclePastPlayerBoostDeadline(h);
  }
  @GameTest(template="empty",timeoutTicks=80,batch="equipped46_skills")
  public static void companionCombatLevels(GameTestHelper h){
    realRuntime(h);CompanionSkillGameTests.actualUntrainedAndMaximumCompanionCombatDamage(h);
  }
  @GameTest(template="empty",timeoutTicks=160,batch="equipped46_recovery")
  public static void equipDefeatRecoveryAndUnequip(GameTestHelper h)throws Exception{
    realRuntime(h);CompanionGameTests.realAccessoriesEquipDefeatRecoveryAndUnequip(h);
  }
  @GameTest(template="empty",timeoutTicks=1500,batch="equipped46_care")
  public static void equippedCareFlightAndFullRecovery(GameTestHelper h){
    realRuntime(h);CompanionCareUpdateGameTests.careFlightAndFullRecovery(h);
  }

  @GameTest(template="empty",timeoutTicks=80,batch="equipped46_baby")
  public static void babyActualSlotAndReleasePreserveIdentity(GameTestHelper h){
    realRuntime(h);
    var state=new com.chaogarden.core.ChaoState();state.hunger=71;state.energy=83;
    state.happiness=47;state.boredom=26;state.ageTicks=123;state.setEvolutionStrength(.2f);
    var e=CompanionSkillGameTests.equip(h,state);
    h.runAfterDelay(12,()->{
      var baby=CompanionSkillGameTests.body(h,e);
      h.assertTrue(!baby.state().adult&&baby.state().ageTicks==123,"Actual equipped baby changed age");
      h.assertTrue(baby.companionCarryPosition()==com.chaogarden.companion.CarryRules.HEAD
          &&baby.companionKeeperId()==e.keeper().getId(),"Untrained baby did not perch on its keeper");
      h.assertFalse(com.chaogarden.companion.CompanionContent.flightEligible(e.keeper()),"Equipped baby granted adult flight");
      String expected=baby.state().encode();
      var current=e.inventory().getItem(0);
      com.chaogarden.companion.CompanionContent.beforeUnequip(e.keeper(),current);
      var held=current.copy();
      com.chaogarden.companion.CompanionContent.unequip(e.keeper(),current);
      e.inventory().setItem(0,net.minecraft.world.item.ItemStack.EMPTY);
      e.keeper().setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,held);
      h.runAfterDelay(4,()->{
        h.assertTrue(baby.isRemoved()&&expected.equals(com.chaogarden.item.ChaoCarrierItem.state(held).encode()),
            "Baby unequip lost its full saved state");
        h.assertTrue(e.chao().equals(com.chaogarden.item.ChaoCarrierItem.storedUUID(held)),"Baby unequip changed UUID");
        var centre=new net.minecraft.core.BlockPos(2,2,2);h.setBlock(centre.below(),net.minecraft.world.level.block.Blocks.STONE);
        var at=h.absolutePos(centre);
        var hit=new net.minecraft.world.phys.BlockHitResult(net.minecraft.world.phys.Vec3.atCenterOf(at.below()),
            net.minecraft.core.Direction.UP,at.below(),false);
        var use=new net.minecraft.world.item.context.UseOnContext(e.keeper(),net.minecraft.world.InteractionHand.MAIN_HAND,hit);
        h.assertTrue(held.getItem().useOn(use).consumesAction()&&held.isEmpty(),"Actual baby release did not consume its one carrier");
        var released=h.getLevel().getEntity(e.chao());
        h.assertTrue(released instanceof com.chaogarden.entity.ChaoEntity,"Baby release lost its actual UUID");
        var chao=(com.chaogarden.entity.ChaoEntity)released;
        h.assertTrue(expected.equals(chao.state().encode())&&!com.chaogarden.companion.CompanionContent.isManaged(chao)
            &&!chao.isCompanionAttached()&&!chao.noPhysics&&!chao.isNoGravity(),"Baby release changed care or retained equipped physics");
        chao.discard();CompanionSkillGameTests.finish(h,e);
      });
    });
  }
}
