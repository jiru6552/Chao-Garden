package com.chaogarden.core;

/**
 * Small, deterministic Minecraft behavior rules. The personality names come
 * from the modern Chao Doctor; thresholds and tick durations are adaptations,
 * not claims about the original game's internal emotion thresholds.
 */
public final class ChaoPreferences {
  /** Fifteen loaded seconds per fullness point; a full normal Chao wants another meal after about four minutes. */
  public static final int FULLNESS_DECAY_TICKS=300;
  public static final int NONE=0, GENTLE=1, NAUGHTY=2, ENERGETIC=3, QUIET=4,
      BIG_EATER=5, CHATTY=6, EASILY_BORED=7, CURIOUS=8, CAREFREE=9,
      CARELESS=10, SMART=11, CRY_BABY=12, LONELY=13, NAIVE=14;
  public static final String[] PERSONALITIES={"No personality", "Gentle", "Naughty",
      "Energetic", "Quiet", "Big eater", "Chatty", "Easily bored", "Curious",
      "Carefree", "Careless", "Smart", "Cry baby", "Lonely", "Naive"};
  private ChaoPreferences() {}

  public static String personalityName(ChaoState state) {
    return state.doctorPersonality(0);
  }

  public static boolean hasPersonality(ChaoState state,int label){return ChaoPersonality.has(state,label);}
  private static int slider(ChaoState state,int index,int legacyLabel,int legacyValue){
    return !state.personalityProfileInitialized&&state.personality==legacyLabel?legacyValue:state.personalitySlider(index);
  }
  /** Source-backed Carefree/Energetic animation weighting, bounded for Minecraft clips. */
  public static float animationSpeed(ChaoState state){
    return Math.max(.65f,Math.min(1.4f,1+slider(state,ChaoPersonality.ENERGETIC,ENERGETIC,65)*.0025f
        -Math.max(0,slider(state,ChaoPersonality.CAREFREE,CAREFREE,65))*.002f));
  }
  public static int trainingTicks(ChaoState state){return Math.max(18,40-Math.max(0,slider(state,ChaoPersonality.SKILLFUL,SMART,65))/4);}
  public static int recoveryTicks(ChaoState state,int baseline){return Math.max(8,Math.round(baseline*(1-state.personalitySlider(ChaoPersonality.REGAIN)/200f)));}
  public static int socialChanceDenominator(ChaoState state){return Math.max(2,12-state.personalitySlider(ChaoPersonality.CHATTY)/10);}

  /** Hunger is the existing mod's fullness meter: zero hungry, one hundred full. */
  public static int foodAcceptanceThreshold(ChaoState state, String fruit) {
    if(fruit==null)fruit="";
    fruit=com.chaogarden.fruit.FruitCatalog.canonical(fruit);
    boolean heroTaste=fruit.equals("hero_garden_fruit") || fruit.equals("rare_fruit") || fruit.equals("wonder_fruit");
    boolean darkTaste=fruit.equals("dark_garden_fruit") || fruit.equals("creepy_fruit")
        || fruit.equals("rough_fruit") || fruit.equals("forbidden_fruit");
    // These dislikes belong to evolved Hero/Dark forms. Children must be able
    // to eat either alignment fruit, including after an aligned reincarnation.
    if(state.adult && (state.align<=-.5f && heroTaste || state.align>=.5f && darkTaste))return 0;
    int shape=switch(fruit) {case "round_fruit"->0;case "square_fruit"->1;case "triangle_fruit"->2;default->-1;};
    boolean favorite=shape>=0 && shape==state.favoriteFruit;
    int threshold=favorite?95:85;
    // The documented Big eater exception uses ravenous eating even while full.
    if(hasPersonality(state,BIG_EATER))threshold=101;
    else if(!favorite && (hasPersonality(state,QUIET) || hasPersonality(state,CRY_BABY)))threshold=80;
    else threshold=Math.min(99,threshold+Math.max(0,state.personalitySlider(ChaoPersonality.CURIOUS))/20);
    return threshold;
  }

  public static boolean acceptsFood(ChaoState state, String fruit) {
    return !state.dead && !state.sleeping && state.cocoonTicks<=0
        && state.hunger<foodAcceptanceThreshold(state,fruit);
  }

  public static boolean acceptsTraining(ChaoState state) {
    return !state.dead && !state.sleeping && state.cocoonTicks<=0 && state.energy>=10;
  }

  public static int thinkingTicks(ChaoState state) {
    int baseline=hasPersonality(state,SMART)||hasPersonality(state,BIG_EATER)?24:
        hasPersonality(state,CURIOUS)?52:hasPersonality(state,QUIET)||hasPersonality(state,CRY_BABY)?48:
        hasPersonality(state,NAIVE)||hasPersonality(state,CARELESS)?28:36;
    return Math.max(18,Math.min(70,baseline+state.curiosityTendency/10));
  }

  /** Zero forbids starting a voluntary toy session; otherwise roll one in N. */
  public static int playChanceDenominator(ChaoState state) {
    if(state.dead || state.sleeping || state.cocoonTicks>0 || state.energy<15 || state.hunger<20)return 0;
    int chance=hasPersonality(state,ENERGETIC)?4:
        hasPersonality(state,EASILY_BORED)||hasPersonality(state,CURIOUS)?3:
        hasPersonality(state,QUIET)||hasPersonality(state,CRY_BABY)?9:6;
    if(state.boredom>=75)chance=Math.max(2,chance/2);
    else if(state.boredom<20)chance*=2;
    if(state.energeticTendency>0)chance=Math.max(2,chance-state.energeticTendency/25);
    if(state.energeticTendency<0)chance+=(-state.energeticTendency)/25;
    return chance;
  }
}
