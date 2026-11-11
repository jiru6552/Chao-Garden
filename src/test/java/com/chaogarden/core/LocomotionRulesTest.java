package com.chaogarden.core;

import java.util.Arrays;
import java.util.Random;

/** Ability boundaries, independent skill axes, and inherited point persistence. */
public final class LocomotionRulesTest {
  private static int checks;
  private static void check(boolean condition, String message) {
    checks++; if (!condition) throw new AssertionError(message);
  }
  public static int run() {
    checks = 0;
    var state = new ChaoState();
    check(LocomotionRules.groundGait(state)==LocomotionRules.GroundGait.CRAWL, "Newborns crawl");
    for (boolean adult : new boolean[]{false,true}) {
      state.adult=adult;
      for (int[] boundary : new int[][]{{49,0},{50,1},{666,1},{667,2},{1333,2},{1334,3}}) {
        state.points[ChaoState.RUN]=boundary[0];
        check(LocomotionRules.groundGait(state).ordinal()==boundary[1], "Run point boundary, independent of age");
      }
    }
    for (int[] boundary : new int[][]{{99,0},{100,1},{666,1},{667,2}}) {
      state.points[ChaoState.SWIM]=boundary[0];
      int mode=LocomotionRules.advancedSwim(state)?2:LocomotionRules.canSwim(state)?1:0;
      check(mode==boundary[1], "Swim point boundary");
    }
    state.points[ChaoState.FLY]=134;
    check(!LocomotionRules.canFly(state), "134 Fly cannot glide");
    state.points[ChaoState.FLY]=135;
    check(LocomotionRules.canFly(state), "135 Fly unlocks garden glide");
    Arrays.fill(state.points,0);Arrays.fill(state.levels,99);Arrays.fill(state.progress,99);
    check(!LocomotionRules.canWalk(state)&&!LocomotionRules.canSwim(state)&&!LocomotionRules.canFly(state),
        "Displayed levels/progress never replace earned stat points");
    Arrays.fill(state.points,1400);Arrays.fill(state.levels,0);Arrays.fill(state.progress,0);
    check(LocomotionRules.canDash(state)&&LocomotionRules.advancedSwim(state)&&LocomotionRules.canFly(state),
        "Earned points retain capabilities without a level approximation");
    var saved=ChaoState.decode(state.encode());
    check(LocomotionRules.groundGait(saved)==LocomotionRules.GroundGait.DASH&&LocomotionRules.canFly(saved),
        "Save roundtrip retains point-derived movement");
    saved.reincarnate();
    check(!saved.adult&&LocomotionRules.canWalk(saved)&&!LocomotionRules.canRun(saved)&&LocomotionRules.canFly(saved),
        "Retained reincarnation points can give an experienced child abilities");
    var young=new ChaoState();young.points[ChaoState.RUN]=49;young.points[ChaoState.FLY]=134;
    young.reincarnate();
    check(!LocomotionRules.canWalk(young)&&!LocomotionRules.canFly(young), "Reincarnation loss can remove movement milestones");
    var swimmer=new ChaoState();swimmer.points[ChaoState.SWIM]=667;
    check(LocomotionRules.waterMovementSpeed(swimmer)>LocomotionRules.waterMovementSpeed(new ChaoState()),
        "Swim training changes real water speed even with zero Run");
    check(LocomotionRules.movementSpeed(swimmer)==LocomotionRules.movementSpeed(new ChaoState()),
        "Swim training does not unlock land walking");
    double priorLand=0,priorWater=0;
    for(int p=0;p<=9999;p+=17) {
      var sample=new ChaoState();sample.points[ChaoState.RUN]=sample.points[ChaoState.SWIM]=p;
      double land=LocomotionRules.movementSpeed(sample),water=LocomotionRules.waterMovementSpeed(sample);
      check(land>=priorLand&&land<=.440001, "Land movement stays bounded and grows with training");
      check(water>=priorWater&&water<=.360001, "Water movement stays bounded and grows with training");
      priorLand=land;priorWater=water;
    }
    // Feed/training updates, rather than fabricated level numbers, cross the gate.
    var trained=new ChaoState();trained.grades[ChaoState.RUN]=2;
    trained.train(ChaoState.RUN,400,new Random(16));
    check(trained.points[ChaoState.RUN]>=50&&LocomotionRules.canWalk(trained), "Ordinary training unlocks walking");
    var robot=new ChaoState();robot.character="omochao";robot.adult=true;
    check(LocomotionRules.groundGait(robot)==LocomotionRules.GroundGait.WALK,
        "A newly assembled young-adult robot walks without a baby crawling phase");
    check(!LocomotionRules.canRun(robot)&&!LocomotionRules.canDash(robot)
        &&!LocomotionRules.canFly(robot)&&!LocomotionRules.canSwim(robot),
        "Robot walking does not grant other garden movement milestones");
    check(Arrays.stream(robot.points).allMatch(p->p==0)&&Arrays.stream(robot.levels).allMatch(p->p==0),
        "Intrinsic robot walking leaves all earned stats untouched");
    check(LocomotionRules.movementSpeed(robot)==.17,
        "Zero-point robot walking starts at the ordinary walking speed");
    return checks;
  }
  public static void main(String[] args) { System.out.println("Locomotion rules: "+run()+" checks passed"); }
}
