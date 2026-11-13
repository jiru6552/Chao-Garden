package com.chaogarden.core;

/** Deterministic tests for the full standalone care and activity rule set. */
public final class MechanicsTestSuite {
  public static void main(String[] args) {
    ChaoStateTest.main(args);
    AnalyzerStatRulesTest.main(args);
    com.chaogarden.client.AnalyzerAlignmentBar053Test.main(args);
    com.chaogarden.client.AnalyzerMaturityBar056Test.main(args);
    System.out.println("Creative edit rules056: "+CreativeEditingRules056Test.run()+" checks passed");
    System.out.println("Neutral morphology056: "+NeutralMorphology056Test.run()+" checks passed");
    System.out.println("Hero/Normal morphology057: "+HeroNormalMorphology057Test.run()+" checks passed");
    System.out.println("Neutral/Swim morphology059: "+NeutralSwimMorphology059Test.run()+" checks passed");
    System.out.println("Dark/Normal morphology060: "+DarkNormalMorphology060Test.run()+" checks passed");
    System.out.println("Neutral/Fly morphology061: "+NeutralFlyMorphology061Test.run()+" checks passed");
    System.out.println("Neutral/Run morphology062: "+NeutralRunMorphology062Test.run()+" checks passed");
    System.out.println("Neutral/Power morphology063: "+NeutralPowerMorphology063Test.run()+" checks passed");
    DriveRulesTest.main(args);
    com.chaogarden.toys.ToyInteractionRulesTest.main(args);
    int distribution = com.chaogarden.distribution.EggDistributionRulesTest.run();
    System.out.println("Egg distribution policy: " + distribution + " checks passed");
    System.out.println("Training supplies: "+com.chaogarden.genes.TrainingSupplyRulesTest.run()+" checks passed");
    System.out.println("Wardrobe choices: "+WardrobeChoiceTest.run()+" checks passed");
    ChaoAnimationRules025Test.main(args);
    PortalGeometry0251Test.main(args);
    LobbyGeometry0252Test.main(args);
    LobbyApproachGeometry0255Test.main(args);
    GardenGateGeometry0256Test.main(args);
    AreaMusicRules0257Test.main(args);
    SchoolSingingRules0258Test.main(args);
    com.chaogarden.client.StaffFaceMotion0251Test.main(args);
    int market = com.chaogarden.market.MarketStockTest.run();
    int activities = com.chaogarden.activities.ActivityRulesTest.run()
        + com.chaogarden.activities.ActivityRewardsTest.run();
    int companions = com.chaogarden.companion.CompanionRulesTest.run();
    int fruit = com.chaogarden.fruit.FruitEffectsTest.run();
    int movement = LocomotionRulesTest.run();
    System.out.println("Black Market stock: " + market + " checks passed");
    System.out.println("Kindergarten buyback: " + com.chaogarden.kindergarten.MarketBuybackRules025Test.run() + " checks passed");
    System.out.println("Activity rules: " + activities + " checks passed");
    System.out.println("Companion rules: " + companions + " checks passed");
    System.out.println("SA2 fruit effects: " + fruit + " checks passed");
    System.out.println("Locomotion rules: " + movement + " checks passed");
  }
}
