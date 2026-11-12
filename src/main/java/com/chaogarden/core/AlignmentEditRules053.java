package com.chaogarden.core;

/** Bounded Creative Analyzer alignment edits; persistence remains in ChaoState. */
public final class AlignmentEditRules053 {
  public static final int MAX_MILLI=1000;
  private AlignmentEditRules053() {}

  /** Adults and special immortal forms retain their existing alignment identity. */
  public static boolean canEdit(ChaoState state,boolean creative,boolean spectator) {
    return creative&&!spectator&&state!=null&&!state.dead&&!state.adult&&!state.immortal;
  }

  /** The wire carries integers, so NaN and infinity cannot be encoded as alignment. */
  public static boolean validMilli(int value) {
    return value>=-MAX_MILLI&&value<=MAX_MILLI;
  }

  public static boolean applyMilli(ChaoState state,int value) {
    return validMilli(value)&&apply(state,value/(float)MAX_MILLI);
  }

  /** Only alignment changes. Rejection, including an adult edit, has no side effects. */
  public static boolean apply(ChaoState state,float value) {
    if(state==null||state.dead||state.adult||state.immortal||!Float.isFinite(value)
        ||value< -1||value>1||Float.floatToIntBits(state.align)==Float.floatToIntBits(value))return false;
    state.align=value;return true;
  }

  /** Use the locked adult identity even if an external tool changed its public mirror. */
  public static int displayMilli(ChaoState state) {
    if(state==null)return 0;
    float value=state.lockedAlignment();
    return Float.isFinite(value)?Math.round(Math.clamp(value,-1,1)*MAX_MILLI):0;
  }
}
