package com.chaogarden.core;

import java.util.Locale;

/** Absolute testing edits use the existing levels and grade-dependent mean earned-point increments. */
public final class CreativeChaoEditRules056 {
  public static final int INTELLIGENCE=5, LUCK=6;
  public static final String[] STAT_NAMES={"Swim","Fly","Run","Power","Intelligence","Luck"};
  private CreativeChaoEditRules056() {}

  public static int stat(String name) {
    if(name==null)return -1;
    return switch(name.toLowerCase(Locale.ROOT)) {
      case "swim"->ChaoState.SWIM;case "fly"->ChaoState.FLY;
      case "run"->ChaoState.RUN;case "power"->ChaoState.POWER;
      case "intelligence"->INTELLIGENCE;case "luck"->LUCK;default->-1;
    };
  }
  public static String statName(int stat) {
    return switch(stat) {
      case ChaoState.SWIM->"Swim";case ChaoState.FLY->"Fly";
      case ChaoState.RUN->"Run";case ChaoState.POWER->"Power";
      case INTELLIGENCE->"Intelligence";case LUCK->"Luck";default->"Unknown";
    };
  }
  public static int alignmentCenti(ChaoState s) {return Math.round(s.lockedAlignment()*100);}
  public static boolean alignmentAllowed(ChaoState s,int alignment) {
    return s!=null&&!s.dead&&alignment>=-100&&alignment<=100
        &&(s.adult?alignment==alignmentCenti(s):!s.immortal&&!OmochaoRules.isOmochao(s));
  }
  public static int level(ChaoState s,int stat) {
    return stat==INTELLIGENCE?s.hiddenLevels[ChaoState.HIDDEN_INTELLIGENCE]
        :stat==LUCK?s.hiddenLevels[ChaoState.HIDDEN_LUCK]:s.levels[stat];
  }
  /** Reject the entire transaction when adult alignment conflicts; callers must provide current locked value. */
  public static boolean apply(ChaoState s,int alignment,int stat,int level) {
    if(!alignmentAllowed(s,alignment)||level<0||level>99||!((stat>=0&&stat<4)||stat==INTELLIGENCE||stat==LUCK))return false;
    if(stat>=INTELLIGENCE) {
      int hidden=stat==LUCK?ChaoState.HIDDEN_LUCK:ChaoState.HIDDEN_INTELLIGENCE;
      int grade=hidden==ChaoState.HIDDEN_LUCK?s.luckGrade:s.intelligenceGrade;
      int before=hidden==ChaoState.HIDDEN_LUCK?s.luck:s.intelligence;
      int points=level==0?0:Math.clamp(before+(level-s.hiddenLevels[hidden])*(13+3*Math.clamp(grade,0,255)),0,4000);
      s.hiddenLevels[hidden]=level;s.hiddenProgress[hidden]=0;
      if(hidden==ChaoState.HIDDEN_LUCK)s.luck=points;else s.intelligence=points;
    } else {
      int oldTotal=HeroRunMorphology052.totalTraining(s,stat);
      s.points[stat]=level==0?0:Math.clamp(s.points[stat]+(level-s.levels[stat])*(13+3*Math.clamp(s.grades[stat],0,6)),0,9999);
      s.levels[stat]=level;s.progress[stat]=0;
      if(HeroRunMorphology052.totalTraining(s,stat)>oldTotal)s.recordCreativeTraining(stat);
    }
    if(!s.adult)s.align=alignment/100f;
    return true;
  }
}
