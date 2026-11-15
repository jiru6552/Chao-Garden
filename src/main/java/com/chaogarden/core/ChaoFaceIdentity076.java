package com.chaogarden.core;

/**
 * Fixed face identity using the existing personality genes and expressed profile.
 *
 * <p>The weights are Minecraft presentation choices, not a recovered Sonic face-gene formula.
 * Every choice retains variety within a Doctor label. Assignment hashes saved birth identity and
 * personality only: it consumes no random draws and never uses care, mood, alignment or age.
 */
public final class ChaoFaceIdentity076 {
  // Stable persisted IDs. Do not reorder when adding future appearances.
  public static final int NORMAL=0, HALF_CLOSED=1, HAPPY_CLOSED=2, MEAN=3, CHAOS=4;
  public static final int NONE=0, SMILE=1, FROWN=2, GRIN=3, WAVY=4;
  public static final int EYE_COUNT=5, MOUTH_COUNT=5;
  public static final String[] EYE_NAMES={"normal","half_closed","happy_closed","mean","chaos"};
  public static final String[] MOUTH_NAMES={"none","smile","frown","grin","wavy"};

  private ChaoFaceIdentity076() {}

  /** Birth/migration assignment; callers control the one-time persisted initialization guard. */
  static void initialize(ChaoState state,long birthIdentity) {
    long seed=mix(birthIdentity^0x464143453037364cL);
    for(int i=0;i<ChaoPersonality.COUNT;i++) {
      seed=mix(seed^((long)(i+1)<<32)^state.personalityGenesA[i]);
      seed=mix(seed^((long)(i+1)<<40)^((long)state.personalityGenesB[i]<<8));
      seed=mix(seed^((long)(state.personalitySlider(i)+100)<<16));
    }
    int[] profile=state.personalityProfile();
    int[] eyes={55,16,12,12,5},mouth={45,25,12,10,8};
    for(int label:profile) {
      seed=mix(seed^((long)(label+1)*0x9e3779b97f4a7c15L));
      // These are gentle biases, never a one-label-to-one-face assignment.
      switch(label) {
        case ChaoPreferences.GENTLE -> {eyes[HAPPY_CLOSED]+=12;mouth[SMILE]+=20;}
        case ChaoPreferences.NAUGHTY -> {eyes[MEAN]+=24;mouth[GRIN]+=20;}
        case ChaoPreferences.ENERGETIC -> {eyes[NORMAL]+=12;eyes[HAPPY_CLOSED]+=8;mouth[GRIN]+=12;}
        case ChaoPreferences.QUIET,ChaoPreferences.CAREFREE -> {eyes[HALF_CLOSED]+=22;mouth[NONE]+=10;}
        case ChaoPreferences.CRY_BABY,ChaoPreferences.LONELY -> {eyes[HALF_CLOSED]+=8;mouth[FROWN]+=18;mouth[WAVY]+=8;}
        case ChaoPreferences.BIG_EATER -> mouth[GRIN]+=16;
        case ChaoPreferences.CHATTY -> {eyes[HAPPY_CLOSED]+=8;mouth[SMILE]+=12;}
        case ChaoPreferences.CURIOUS -> {eyes[NORMAL]+=12;mouth[WAVY]+=8;}
        case ChaoPreferences.EASILY_BORED -> {eyes[HALF_CLOSED]+=12;mouth[FROWN]+=8;}
        case ChaoPreferences.CARELESS,ChaoPreferences.NAIVE -> mouth[WAVY]+=12;
        case ChaoPreferences.SMART -> {eyes[HALF_CLOSED]+=8;mouth[SMILE]+=8;}
        default -> {}
      }
    }
    state.setBaselineFace076(select(seed^0x455945533037364cL,eyes),
        select(seed^0x4d4f555448303736L,mouth));
  }

  static int boundEyes(int value) {return Math.max(0,Math.min(EYE_COUNT-1,value));}
  static int boundMouth(int value) {return Math.max(0,Math.min(MOUTH_COUNT-1,value));}

  private static int select(long seed,int[] weights) {
    int total=0;for(int weight:weights)total+=weight;
    int choice=(int)Long.remainderUnsigned(mix(seed),total);
    for(int i=0;i<weights.length;i++) {choice-=weights[i];if(choice<0)return i;}
    return 0;
  }

  private static long mix(long value) {
    value=(value^(value>>>30))*0xbf58476d1ce4e5b9L;
    value=(value^(value>>>27))*0x94d049bb133111ebL;
    return value^(value>>>31);
  }
}
