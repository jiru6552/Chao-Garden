package com.chaogarden.companion;

import com.chaogarden.core.ChaoState;
import java.util.Arrays;
import java.util.Random;

/** Independent companion defeat/admission invariants; no Minecraft bootstrap. */
public final class CompanionRulesTest {
  private static int checks;
  private static void check(boolean result, String message) {
    checks++; if (!result) throw new AssertionError(message);
  }

  public static int run() {
    checks = 0;
    ChaoState young = new ChaoState();
    check(CompanionRules.equippable(young) && !CompanionRules.adult(young), "Babies equip but receive no adult benefits");
    young.adult = true;
    check(CompanionRules.adult(young), "A healthy adult is eligible");
    young.cocoonTicks = 1;
    check(!CompanionRules.adult(young), "Cocooned adults cannot be equipped");
    young.cocoonTicks = 0; young.dead = true;
    check(!CompanionRules.adult(young), "Dead state cannot enter an accessory");
    check(CompanionRules.RECOVERY_TICKS == 60*20, "Recovery is one minute at 20 TPS");
    check(CompanionRules.powerDamage(0)==1,"An untrained adult can defend with one punch");
    check(CompanionRules.glideTicks(0)==40,"An untrained adult helps for two seconds");
    double previousRun=-1,previousSwim=-1;float previousPower=0;int previousGlide=0;
    for(int level=0;level<=99;level++) {
      double run=CompanionRules.runSpeedBonus(level),swim=CompanionRules.swimSpeedBonus(level);
      float power=CompanionRules.powerDamage(level);int glide=CompanionRules.glideTicks(level);
      check(Double.isFinite(run)&&run>=previousRun&&run>=0&&run<=1,"Running is finite, bounded and monotone");
      check(Double.isFinite(swim)&&swim>=previousSwim&&swim>=0&&swim<=2,"Swimming is finite, bounded and monotone");
      check(power>=previousPower&&power>=1&&power<=11,"Power rises monotonically from punch to SharpnessV sword");
      check(glide>=previousGlide&&glide>=40&&glide<=242,"Finite glide grows without shrinking");
      check(CompanionRules.waterNightVision(level)==(level>=70),"Water vision threshold");
      check(CompanionRules.waterBreathing(level)==(level==99),"Water breathing requires maximum level");
      ChaoState helper=new ChaoState();helper.adult=true;helper.levels[ChaoState.FLY]=level;
      check(CompanionRules.canElytra(helper)==(level==99),"True elytra requires maximum Fly level");
      previousRun=run;previousSwim=swim;previousPower=power;previousGlide=glide;
    }
    check(CompanionRules.runSpeedBonus(99)==1&&CompanionRules.swimSpeedBonus(99)==2,"Maximum speed bonuses");
    check(CompanionRules.runSpeedBonus(1)<.01&&CompanionRules.swimSpeedBonus(1)<.02,"Low skill bonuses remain slight");
    check(CompanionRules.glideTicks(70)==200&&CompanionRules.glideTicks(98)==240,"High finite glide reaches ten to twelve seconds");
    check(CompanionRules.powerDamage(99)==11,"MaximumPower equals vanilla fully charged NetheriteSharpnessV");
    check(CompanionRules.runSpeedBonus(-50)==0&&CompanionRules.powerDamage(-50)==1,"Invalid low levels clamp safely");
    check(CompanionRules.runSpeedBonus(10000)==1&&CompanionRules.powerDamage(10000)==11,"Invalid high levels clamp safely");
    ChaoState robot=new ChaoState();robot.character="omochao";robot.adult=true;Arrays.fill(robot.grades,5);
    check(CompanionRules.canElytra(robot)&&CompanionRules.robotBoost(robot),"AllS robot has elytra and boost even beforetraining");
    for(int stat=0;stat<5;stat++){robot.grades[stat]=4;check(!CompanionRules.robotBoost(robot),"Every robotgrade must reachS");robot.grades[stat]=5;}
    robot.character="normal";check(!CompanionRules.robotBoost(robot)&&!CompanionRules.canElytra(robot),"Ordinary Sgenes do not bypass skilltraining");
    check(CompanionRules.BOOST_COOLDOWN_TICKS==5*20,"Robot boost cooldown is five seconds");
    Random random = new Random(1880);
    for (int trained = 0; trained < 5; trained++) {
      for (int seed = 0; seed < 60; seed++) {
        ChaoState state = new ChaoState();
        state.levels[trained] = 3; state.points[trained] = 80; state.progress[trained] = 70;
        state.happiness = 40;
        int lost = CompanionRules.defeat(state, random);
        check(lost == trained, "A single trained skill is always the penalized skill");
        check(Arrays.stream(state.levels).sum() == 2, "Exactly one trained level is lost");
        check(state.happiness == 30, "Defeat loses ten happiness");
        check(state.progress[trained] == 0 && state.points[trained] < 80,
            "Stat progress and points follow the level penalty");
      }
    }
    ChaoState trained = new ChaoState();
    Arrays.fill(trained.levels, 5); Arrays.fill(trained.points, 100);
    for (int n = 0; n < 25; n++) {
      int oldLevels = Arrays.stream(trained.levels).sum();
      int chosen = CompanionRules.defeat(trained, random);
      check(chosen >= 0 && trained.levels[chosen] >= 0, "Only a positive skill is selected");
      check(Arrays.stream(trained.levels).sum() == oldLevels-1, "One level per defeat until exhausted");
    }
    check(CompanionRules.defeat(trained, random) == -1, "Zero-level Chao have no negative skill penalty");
    check(Arrays.stream(trained.levels).allMatch(level -> level == 0), "Levels clamp at zero");
    check(trained.happiness == -100, "Happiness clamps at its existing lower bound");
    ChaoState tiny = new ChaoState(); tiny.levels[0] = 1; tiny.points[0] = 1;
    CompanionRules.defeat(tiny, random);
    check(tiny.points[0] == 0, "Points cannot go negative");
    int previousInterval=25;
    for(int level=0;level<=99;level++) {
      int interval=CompanionRules.attackCooldown(level);
      check(interval>=12&&interval<=24&&interval<=previousInterval,"Companion attack interval is finite/bounded/monotone");previousInterval=interval;
    }
    for(int extreme:new int[]{Integer.MIN_VALUE,-1,100,Integer.MAX_VALUE}) {
      int bounded=Math.max(0,Math.min(99,extreme));
      check(CompanionRules.runSpeedBonus(extreme)==CompanionRules.runSpeedBonus(bounded),"Run clamps corrupted external levels");
      check(CompanionRules.swimSpeedBonus(extreme)==CompanionRules.swimSpeedBonus(bounded),"Swim clamps corrupted external levels");
      check(CompanionRules.powerDamage(extreme)==CompanionRules.powerDamage(bounded)&&CompanionRules.attackCooldown(extreme)==CompanionRules.attackCooldown(bounded),"Combat clamps corrupted external levels");
      check(CompanionRules.glideTicks(extreme)==CompanionRules.glideTicks(bounded),"Glide clamps corrupted external levels");
    }
    for (boolean wellFed : new boolean[]{false,true}) {
      var cared = new ChaoState();
      cared.adult = true;
      cared.ageTicks = 12345;
      cared.adultAgeTicks = 19999;
      cared.hunger = wellFed ? 80 : 4;
      cared.energy = 80;
      cared.happiness = wellFed ? 51 : 50;
      cared.bond = 11;
      cared.setEvolutionStrength(.25f);
      for (int tick = 1; tick <= 6000; tick++) {
        cared.tickEquippedCare(tick, 6000);
        if (tick == 3217) cared = ChaoState.decode(cared.encode());
      }
      check(cared.energy == 70 && cared.hunger == (wellFed ? 60 : 0), "Loaded care costs across serialization");
      check(cared.bond == (wellFed ? 12 : 11) && cared.happiness == (wellFed ? 51 : 48), "Fed company and neglect");
      check(Math.abs(cared.evolutionStrength() - .5f) < 1e-6, "Adult maturity advances at the garden rate");
      check(!cared.dead && cared.reincarnations == 0 && cared.ageTicks == 12345 && cared.adultAgeTicks == 19999,
          "Equipped maturity advanced lifespan");
      var outcome = cared.tick(6000,20000,new Random(1));
      check(outcome == (wellFed ? ChaoState.Transition.REINCARNATED : ChaoState.Transition.DIED),
          "Unequipped Chao must cross its original lifespan boundary exactly once");
    }
    var babyCare=new ChaoState();babyCare.ageTicks=999;babyCare.hunger=80;babyCare.energy=80;babyCare.bond=10;
    float babyMaturity=babyCare.evolutionStrength();
    for(int tick=1;tick<=6000;tick++){
      babyCare.tickEquippedCare(tick,6000);if(tick==3000)babyCare=ChaoState.decode(babyCare.encode());
    }
    check(babyCare.ageTicks==999&&!babyCare.adult&&babyCare.evolutionStrength()==babyMaturity,"Carrying silently advanced childhood");
    check(babyCare.hunger==60&&babyCare.energy==70&&babyCare.bond==11,"Baby care did not persist across save");
    return checks;
  }
}
