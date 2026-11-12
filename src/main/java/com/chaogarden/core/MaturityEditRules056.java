package com.chaogarden.core;

/** Creative morphology testing changes current-stage growth, never calendar/lifespan counters. */
public final class MaturityEditRules056 {
  public static final int MAX_MILLI=1000, CHILD_MAX_MILLI=990;
  private MaturityEditRules056() {}

  public static boolean canEdit(ChaoState s,boolean creative,boolean spectator) {
    return creative&&!spectator&&s!=null&&!s.dead&&!s.immortal&&!OmochaoRules.isOmochao(s);
  }
  public static int displayMilli(ChaoState s) {
    if(s==null)return 0;
    float max=s.adult?GradualEvolutionRules.ADULT_STRENGTH_MAX:1;
    return Math.clamp(Math.round(s.evolutionStrength()/max*MAX_MILLI),0,MAX_MILLI);
  }
  public static boolean validMilli(ChaoState s,int value) {
    return s!=null&&value>=0&&value<=(s.adult?MAX_MILLI:CHILD_MAX_MILLI);
  }
  /** The baby cap leaves growth below the natural evolution threshold. Natural time resumes normally. */
  public static boolean applyMilli(ChaoState s,int value) {
    if(!canEdit(s,true,false)||!validMilli(s,value))return false;
    float target=value/(float)MAX_MILLI*(s.adult?GradualEvolutionRules.ADULT_STRENGTH_MAX:1);
    if(Float.floatToIntBits(s.evolutionStrength())==Float.floatToIntBits(target))return false;
    s.setEvolutionStrength(target);return true;
  }
}
