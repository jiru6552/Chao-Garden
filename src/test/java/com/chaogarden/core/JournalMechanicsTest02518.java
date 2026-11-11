package com.chaogarden.core;

import java.util.Random;

/** Executable examples supporting the journal's points, growth and eligibility teaching. */
public final class JournalMechanicsTest02518 {
  private static int checks;
  private static void check(boolean condition, String message) {
    checks++;
    if (!condition) throw new AssertionError(message);
  }
  public static void main(String[] args) {
    var child = new ChaoState();
    child.levels[ChaoState.RUN] = 99;
    check(!LocomotionRules.canWalk(child), "Level 99 alone does not unlock walking");
    child.points[ChaoState.RUN] = 50;
    check(LocomotionRules.canWalk(child), "50 Run points unlock walking");
    child.points[ChaoState.RUN] = 667;
    check(LocomotionRules.canRun(child), "667 Run points unlock running");
    child.points[ChaoState.RUN] = 1334;
    check(LocomotionRules.canDash(child), "1334 Run points unlock dashing");
    child.points[ChaoState.SWIM] = 100;
    check(LocomotionRules.canSwim(child), "100 Swim points unlock paddling");
    child.points[ChaoState.SWIM] = 667;
    check(LocomotionRules.advancedSwim(child), "667 Swim points unlock stronger swimming");
    child.points[ChaoState.FLY] = 135;
    check(LocomotionRules.canFly(child), "135 Fly points unlock gliding");
    check(child.prospectivePrimaryType() == ChaoState.RUN_TYPE, "Highest points select Run");
    child.points[ChaoState.STAMINA] = 1335;
    check(child.prospectivePrimaryType() == ChaoState.NORMAL, "Highest Stamina points select Normal");

    var tied = new ChaoState();
    tied.train(ChaoState.RUN, 1);
    tied.train(ChaoState.FLY, 1);
    tied.points[ChaoState.RUN] = tied.points[ChaoState.FLY] = 100;
    check(tied.prospectivePrimaryType() == ChaoState.FLY_TYPE, "Latest trained tied ability wins");
    tied.train(ChaoState.RUN, 1);
    check(tied.prospectivePrimaryType() == ChaoState.RUN_TYPE, "Training reverses a tied preference");

    var fed = new ChaoState();
    fed.setEvolutionStrength(.99f);
    fed.onFruitEaten();
    check(fed.tick(216000, 864000, new Random(1)) == ChaoState.Transition.EVOLVED,
        "Eating a whole fruit can cause evolution before the default three hours");
    check(fed.ageTicks < 216000, "Fruit growth does not require three hours of age");
    var adult = new ChaoState(); adult.adult = true;
    check(adult.canBreed(), "Ordinary adult is eligible to breed");
    adult.immortal = true;
    check(!adult.canBreed(), "Immortal adults cannot breed by default");
    check(adult.canBreed(true), "Optional rare rules can allow an immortal adult");
    adult.character = "omochao";
    check(!adult.canBreed(true), "Omochao cannot breed even with rare rules enabled");
    adult.happiness = 51;
    OmochaoRules.normalize(adult);
    adult.adultAgeTicks = 19;
    check(OmochaoRules.tickRobot(adult, 20) == ChaoState.Transition.REINCARNATED,
        "Robot happiness above 50 selects the heart outcome");
    var unhappyRobot = new ChaoState();
    unhappyRobot.character = "omochao";
    OmochaoRules.normalize(unhappyRobot);
    unhappyRobot.adultAgeTicks = 19;
    unhappyRobot.happiness = 50;
    check(OmochaoRules.tickRobot(unhappyRobot, 20) == ChaoState.Transition.DIED,
        "Happiness exactly 50 selects the parts-box outcome, not a heart");
    System.out.println("Journal mechanics: " + checks + " checks passed.");
  }
}
