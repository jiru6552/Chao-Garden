package com.chaogarden.core;

import java.util.Arrays;
import java.util.Random;

/** Independent robot lifecycle/grade contracts, including an actual save between generations. */
public final class OmochaoRulesTest {
  private static int checks;
  private static void check(boolean value,String message){checks++;if(!value)throw new AssertionError(message);}
  public static int run(){
    checks=0;var creator=ChaoState.egg(4,true,true,13,new Random(71));creator.adult=true;
    String unchanged=creator.encode();
    for(int rank=2;rank<=5;rank++){
      final int wantedRank=rank;
      var robot=ChaoState.egg(creator.baseColor(),creator.shiny,creator.twoTone,creator.jewel(),new Random(90+rank));
      OmochaoRules.initialize(robot,creator,"カオス 🌟",rank);
      check(robot.adult&&!robot.immortal&&!robot.dead&&robot.ageTicks==0&&robot.adultAgeTicks==0&&robot.cocoonTicks==0&&robot.evolutionStrength()==0,"Construction has no child/cocoon or immortal phase");
      check(Arrays.stream(robot.grades).allMatch(g->g==wantedRank)&&Arrays.equals(robot.gradeA,robot.grades)&&Arrays.equals(robot.gradeB,robot.grades),"Every robot skill has the same coherent earned tier");
      check(Arrays.stream(robot.points).sum()==0&&Arrays.stream(robot.levels).sum()==0,"Ranks never clone the builder's trained levels");
      check(robot.trait("family.parent.0.id").equals(creator.lineageId())&&robot.familyName("family.parent.0").equals("カオス 🌟")&&robot.trait("family.parent.1.id").isEmpty(),"Only the real creator is the mechanical parent");
      var loaded=ChaoState.decode(robot.encode());
      check(loaded.grades[0]==rank&&loaded.familyName("family.parent.0").equals("カオス 🌟")&&loaded.adult&&!loaded.immortal,"Robot rank/creator survive the actual state codec");
      check(OmochaoRules.nextHeartRank(loaded)==Math.min(5,rank+1),"Recovered heart advances exactly one uniform grade and caps at S");
      check(OmochaoRules.allSRank(loaded)==(rank==5),"Only all-S mechanical ranks enable boosted elytra");
      loaded.adultAgeTicks=7;loaded.levels[0]=23;OmochaoRules.normalize(loaded);OmochaoRules.normalize(loaded);
      check(loaded.adultAgeTicks==7&&loaded.levels[0]==23&&loaded.grades[0]==rank,"Repeated normalization cannot reset later age, training or earned ranks");
      loaded.giveAnimal(6,true,true);check(loaded.animalMask==0&&loaded.animalPartsMask==0&&loaded.levels[0]==23,"Even opt-in rare-animal gifts cannot train a robot");
      for(int i=0;i<ChaoIllness.COUNT;i++){loaded.setIllness(i,-100);check(loaded.illness(i)==0&&!ChaoIllness.susceptible(loaded,i),"Robot illness immunity covers direct setters and infection risk");}
      loaded.levels[1]=99;check(!loaded.upgradeGrade(1)&&!loaded.useHyperFruit(1)&&loaded.grades[1]==rank,"Doctor/Hyper gifts cannot bypass heart-investment grade progression");
      int before=loaded.progress[2];loaded.giveDrive(2);check(loaded.progress[2]!=before||loaded.levels[2]>0,"Drives still train a robot's ordinary skill levels");
    }
    check(creator.encode().equals(unchanged),"Building only reads the creator's genetics/progress");
    for(int happiness:new int[]{-100,0,50,51,100}){
      var robot=new ChaoState();OmochaoRules.initialize(robot,creator,"Maple",2);robot.happiness=happiness;
      String parent=robot.trait("family.parent.0.id");
      for(int i=0;i<19;i++)check(OmochaoRules.tickRobot(robot,20)==ChaoState.Transition.NONE,"Robot lives its finite adult interval before yielding a part");
      var expected=happiness>50?ChaoState.Transition.REINCARNATED:ChaoState.Transition.DIED;
      check(OmochaoRules.tickRobot(robot,20)==expected&&robot.dead&&robot.adult&&robot.reincarnations==0,"Care selects heart/component without creating a baby or ordinary rebirth egg");
      check(robot.trait("family.parent.0.id").equals(parent)&&robot.grades[0]==2,"Terminal transition retains rank and creator for item serialization");
      check(OmochaoRules.tickRobot(robot,20)==ChaoState.Transition.NONE,"A stopped terminal robot cannot emit another lifecycle transition");
      var loaded=ChaoState.decode(robot.encode());check(loaded.dead&&loaded.intTrait("robot.expired")== (happiness>50?2:1),"A pending output survives save/reload");
    }
    var legacy=new ChaoState();legacy.character="omochao";legacy.adult=true;legacy.immortal=true;legacy.adultAgeTicks=Integer.MAX_VALUE-2;Arrays.fill(legacy.grades,6);legacy.levels[0]=42;legacy.setEvolutionStrength(1.2f);
    OmochaoRules.normalize(legacy);check(legacy.adult&&!legacy.immortal&&!legacy.dead&&legacy.adultAgeTicks==0&&legacy.evolutionStrength()==0&&legacy.levels[0]==42&&legacy.grades[0]==2,"A legacy immortal robot starts one safe C-rank adult life without losing trained progress");
    legacy.adultAgeTicks=9;OmochaoRules.normalize(legacy);check(legacy.adultAgeTicks==9,"Legacy migration runs only once");
    var saturated=new ChaoState();saturated.character="omochao";saturated.immortal=true;for(int i=0;i<256;i++)saturated.setTrait("record.v"+i,i);
    OmochaoRules.normalize(saturated);saturated.adultAgeTicks=11;saturated=ChaoState.decode(saturated.encode());OmochaoRules.normalize(saturated);
    check(saturated.adultAgeTicks==11&&saturated.robotLifeInitialized(),"Codec-backed migration marker survives a full trait map without endless age resets");
    saturated.adultAgeTicks=19;saturated.happiness=100;check(OmochaoRules.tickRobot(saturated,20)==ChaoState.Transition.REINCARNATED,"A full feature map cannot prevent finite robot expiry");
    saturated=ChaoState.decode(saturated.encode());check(saturated.robotPendingOutcome()==2&&saturated.dead,"Pending heart output remains recoverable even when no extension trait can be added");
    OmochaoRules.setRanks(saturated,4);check(OmochaoRules.rank(saturated)==4&&OmochaoRules.nextHeartRank(saturated)==5,"Actual grades remain authoritative when the rank trait cannot fit");
    saturated.setRobotOutputCommitted(true);saturated=ChaoState.decode(saturated.encode());check(saturated.robotOutputCommitted()&&saturated.robotPendingOutcome()==2,"Committed output survives a full-map save separately from optional feature flags");
    return checks;
  }
  public static void main(String[] args){System.out.println("Omochao rules: "+run()+" checks passed");}
}
