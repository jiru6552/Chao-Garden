package com.chaogarden.core;

/** One classroom call-and-response cycle; values match the authored vocal score. */
public final class SchoolSingingRules0258 {
  public static final int CYCLE_TICKS=120;
  private static final int[] NOTE_ONSETS={0,8,16,24,36,44};
  private SchoolSingingRules0258(){}
  public static int localTick(int slot,long gameTime){
    if(slot!=2&&slot!=3)return -1;
    return Math.floorMod((int)Math.floorMod(gameTime,CYCLE_TICKS)-(slot==3?60:0),CYCLE_TICKS);
  }
  /** -1 means no phrase; A and B each start only once per six-second cycle. */
  public static int phraseAt(int slot,long gameTime){return localTick(slot,gameTime)==0?slot-2:-1;}
  public static boolean noteAt(int slot,long gameTime){
    int phase=localTick(slot,gameTime);if(phase<0)return false;
    for(int onset:NOTE_ONSETS)if(phase==onset)return true;
    return false;
  }
}
