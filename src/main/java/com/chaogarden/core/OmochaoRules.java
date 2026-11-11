package com.chaogarden.core;

import java.util.Arrays;

/** Finite, adult-only robot life and the recovered heart's bounded rank/creator ledger. */
public final class OmochaoRules {
  public static final int C_RANK=2, S_RANK=5;
  public static final String VERSION="robot.lifecycle_version",RANK="robot.rank";
  private OmochaoRules(){}
  public static boolean isOmochao(ChaoState state){return state!=null&&"omochao".equals(state.character);}
  public static int rank(ChaoState state){
    int grade=S_RANK;for(int value:state.grades)grade=Math.min(grade,value);
    return Math.max(C_RANK,Math.min(S_RANK,grade));
  }
  public static boolean allSRank(ChaoState state){
    if(!isOmochao(state)||!state.adult||state.dead)return false;
    for(int grade:state.grades)if(grade<S_RANK)return false;return true;
  }
  public static int nextHeartRank(ChaoState state){return Math.min(S_RANK,rank(state)+1);}

  /** Old immortal robots get one safe finite adult life; later loads keep trained progress. */
  public static void normalize(ChaoState state){
    if(!isOmochao(state))return;
    if(!state.robotLifeInitialized()&&state.intTrait(VERSION)<1){
      state.adultAgeTicks=0;state.cocoonTicks=0;state.dead=false;state.sleeping=false;
      state.setEvolutionStrength(0);
      setRanks(state,C_RANK);state.setTrait(VERSION,1);
    }
    state.markRobotLifeInitialized();
    state.adult=true;state.immortal=false;
    state.animalMask=0;state.clearParts();state.flameOrb=false;state.ghostBody=false;
    Arrays.fill(state.illnessValues,0);Arrays.fill(state.illnessCooldown,0);
  }

  public static void setRanks(ChaoState state,int rank){
    int bounded=Math.max(C_RANK,Math.min(S_RANK,rank));
    Arrays.fill(state.grades,bounded);Arrays.fill(state.gradeA,bounded);Arrays.fill(state.gradeB,bounded);
    state.setTrait(RANK,bounded);
  }

  /** Construction/reconstruction always starts young-adult, with fresh skills and a new identity. */
  public static void initialize(ChaoState state,ChaoState creator,String creatorName,int rank){
    state.character="omochao";state.adult=true;state.immortal=false;state.dead=false;
    state.ageTicks=state.adultAgeTicks=state.cocoonTicks=state.reincarnations=0;
    state.setRobotPendingOutcome(0);
    state.setRobotOutputCommitted(false);state.setTrait("robot.expired","");state.setTrait("robot.output_committed","");
    state.setEvolutionStrength(0);
    state.swimFly=state.runPower=0;state.secondaryType=ChaoState.NORMAL;
    Arrays.fill(state.levels,0);Arrays.fill(state.progress,0);Arrays.fill(state.points,0);
    state.setTrait(VERSION,1);setRanks(state,rank);recordCreator(state,creator,creatorName);normalize(state);
  }

  /** Exactly one mechanical parent; a heart subsequently installs its former creator as grandparent. */
  public static void recordCreator(ChaoState state,ChaoState creator,String name){
    for(int i=0;i<2;i++)clearRelative(state,"family.parent."+i);
    for(int i=0;i<4;i++)clearRelative(state,"family.grandparent."+i);
    state.setTrait("family.parent.0.id",creator.lineageId());
    state.setTrait("family.parent.0.dna",creator.dnaSummary());
    state.setTrait("family.parent.0.name",ChaoState.encodeFamilyName(name));
    // A first build can show its creator's own recorded parents.
    for(int i=0;i<2;i++)copyRelative(creator,"family.parent."+i,state,"family.grandparent."+i);
  }
  public static void copyRelative(ChaoState from,String source,ChaoState to,String target){
    for(String field:new String[]{"id","dna","name"})to.setTrait(target+"."+field,from.trait(source+"."+field));
  }
  private static void clearRelative(ChaoState state,String prefix){for(String field:new String[]{"id","dna","name"})state.setTrait(prefix+"."+field,"");}

  /** Called once per loaded garden tick, before ordinary Chao's cocoon/reincarnation path. */
  public static ChaoState.Transition tickRobot(ChaoState state,int adultDuration){
    if(!isOmochao(state)||state.dead||state.intTrait("content.construction")>0)return ChaoState.Transition.NONE;
    normalize(state);
    if(state.adultAgeTicks<Integer.MAX_VALUE-1)state.adultAgeTicks++;
    if(state.adultAgeTicks<GradualEvolutionRules.durationTicks(adultDuration))return ChaoState.Transition.NONE;
    // Keep coat/grades/creator snapshots intact until the physical output commits.
    boolean heart=state.happiness>50;state.dead=true;
    state.setRobotPendingOutcome(heart?2:1);
    state.setTrait("robot.expired",heart?2:1);
    return heart?ChaoState.Transition.REINCARNATED:ChaoState.Transition.DIED;
  }
}
