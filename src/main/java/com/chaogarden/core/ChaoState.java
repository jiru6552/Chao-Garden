package com.chaogarden.core;

import java.util.Arrays;
import java.util.Base64;
import java.util.Map;
import java.util.TreeMap;
import java.util.Collections;
import java.nio.charset.StandardCharsets;
import java.util.Random;
import java.util.random.RandomGenerator;

/**
 * Server-authoritative, Minecraft-independent Chao life simulation.
 *
 * <p>Visible stats and evolution tendencies are deliberately separate. Genes are diploid, but
 * expressed alleles are selected at birth and saved, so a reload never changes a Chao's colour or
 * grade. Tick durations are supplied by the host rather than baking a real-time game clock into the
 * simulation.
 */
public final class ChaoState {
  public static final int SWIM = 0, FLY = 1, RUN = 2, POWER = 3, STAMINA = 4;
  public static final int NORMAL = 0, SWIM_TYPE = 1, FLY_TYPE = 2, RUN_TYPE = 3, POWER_TYPE = 4;
  public static final int ARMS = 0,
      EARS = 1,
      FOREHEAD = 2,
      FACE = 3,
      FEET = 4,
      HORNS = 5,
      TAIL = 6,
      WINGS = 7;
  public static final String[] STATS = {"Swim", "Fly", "Run", "Power", "Stamina"};
  public static final String[] TYPES = {"Normal", "Swim", "Fly", "Run", "Power"};
  public static final String[] COLORS = {
    "normal",
    "white",
    "red",
    "yellow",
    "blue",
    "aqua",
    "pink",
    "orange",
    "purple",
    "brown",
    "green",
    "grey",
    "lime",
    "black",
    "sky_blue",
    "cyan"
  };
  public static final String[] JEWELS = {
    "none",
    "silver",
    "gold",
    "ruby",
    "sapphire",
    "emerald",
    "amethyst",
    "aquamarine",
    "garnet",
    "onyx",
    "diamond",
    "peridot",
    "topaz",
    "moon"
  };
  public static final String[] ANIMALS = {
    "penguin",
    "seal",
    "otter",
    "peacock",
    "parrot",
    "condor",
    "rabbit",
    "cheetah",
    "boar",
    "bear",
    "tiger",
    "gorilla",
    "skunk",
    "sheep",
    "raccoon",
    "dragon",
    "unicorn",
    "phoenix",
    "half_fish",
    "skeleton_dog",
    "bat",
    "bee", "ladybug", "scorpion", "deer", "elephant", "kangaroo", "koala", "lion", "mole", "swallow"
  };
  /** The saved checklist uses the stable indices of every registered DNA species. */
  public static final int ALL_ANIMALS = registeredAnimalMask();

  private static int registeredAnimalMask() {
    // The compact save format reserves the sign bit. Refuse a future expansion
    // until its persistence is migrated rather than silently wrapping shifts.
    if (ANIMALS.length < 1 || ANIMALS.length > 31)
      throw new IllegalStateException("Animal DNA checklist requires a save-format migration");
    return (int) ((1L << ANIMALS.length) - 1);
  }
  private static final int[][] ANIMAL_STATS = {
    {36, 12, 8, -24},
    {40, -16, 0, 8},
    {44, -4, 8, -16},
    {12, 48, -16, -12},
    {0, 48, -20, 4},
    {20, 60, -24, 16},
    {0, 8, 40, -16},
    {-8, -8, 40, 8},
    {-4, -12, 32, 16},
    {8, -16, 4, 36},
    {-8, -16, 20, 36},
    {-4, -8, 4, 40},
    {8, 12, 12, 8},
    {8, 12, 20, 20},
    {20, 12, 4, 8},
    {20, 4, 8, 32},
    {16, 12, 36, 0},
    {12, 32, 4, 16},
    {32, 0, 8, 24},
    {8, 8, 32, 16},
    {8, 40, 8, 8},
    {0, 250, 0, 0}, {0, 250, 0, 0}, {0, 0, 0, 250},
    {-4, -12, 32, 16}, {-8, -16, 20, 36}, {-8, -8, 40, 8},
    {20, 16, 4, 8}, {8, -16, 4, 36}, {8, 8, 16, 16}, {20, 60, -24, 16}
  };
  private static final int[] PART_SLOTS = {
    slots(ARMS, FOREHEAD, FEET),
    slots(ARMS, TAIL),
    slots(ARMS, EARS, FACE, FEET, TAIL),
    slots(FOREHEAD, FEET, TAIL, WINGS),
    slots(FOREHEAD, FEET, TAIL, WINGS),
    slots(EARS, FEET, TAIL, WINGS),
    slots(ARMS, EARS, FEET, TAIL),
    slots(ARMS, EARS, FEET, TAIL),
    slots(ARMS, EARS, FACE, FEET, TAIL),
    slots(ARMS, EARS, FEET),
    slots(ARMS, EARS, FEET, TAIL),
    slots(ARMS, EARS, FOREHEAD, FEET),
    slots(ARMS, FOREHEAD, FEET, TAIL),
    slots(ARMS, EARS, FEET, HORNS, TAIL),
    slots(ARMS, EARS, FEET),
    slots(ARMS, EARS, FEET, HORNS, TAIL, WINGS),
    slots(ARMS, EARS, FOREHEAD, FEET, TAIL),
    slots(FOREHEAD, FEET, TAIL, WINGS),
    0,
    0,
    0,
    slots(HORNS, WINGS, FEET), slots(HORNS, WINGS, FOREHEAD), slots(ARMS, FEET, TAIL),
    slots(ARMS, EARS, FEET, HORNS, TAIL), slots(ARMS, EARS, FEET, TAIL),
    slots(ARMS, EARS, FEET, TAIL), slots(ARMS, EARS, FEET, TAIL), slots(ARMS, EARS, FOREHEAD, FEET, TAIL),
    slots(ARMS, FEET, TAIL), slots(FEET, TAIL, WINGS)
  };

  public enum Transition {
    NONE,
    EVOLVED,
    REINCARNATED,
    DIED
  }

  // Genotype. Arrays intentionally remain available to persistence/debug tools.
  public int[] colorGenes = {0, 0};
  public int[] toneGenes = {1, 1};
  public int[] shinyGenes = {0, 0};
  public int[] jewelGenes = {0, 0};
  public int[] gradeA = {2, 2, 2, 2, 2};
  public int[] gradeB = {2, 2, 2, 2, 2};
  public int favoriteFruit = 0;
  public int[] favoriteFruitGenes = {0, 0};

  // Phenotype is not recalculated after a load.
  public int expressedColor = 0, expressedJewel = 0;
  public boolean twoTone = true, shiny = false;
  public int[] grades = {2, 2, 2, 2, 2};
  public int[] levels = new int[5], progress = new int[5], points = new int[5];
  public int intelligence = 0, luck = 0;
  public int ageTicks = 0, adultAgeTicks = 0, reincarnations = 0;
  public boolean adult = false, immortal = false, dead = false;
  public int primaryType = NORMAL, secondaryType = NORMAL;
  public float align = 0, swimFly = 0, runPower = 0;
  public int happiness = 20, bond = 0, hunger = 80, energy = 100;
  /** Modern Doctor personality label; behavior weights use Minecraft-scale rules. */
  public int personality = ChaoPreferences.NONE, boredom = 0;
  public int curiosityTendency = 0, energeticTendency = 0, naiveTendency = 0, carefreeTendency = 0;
  public boolean sleeping = false;
  public int animalMask = 0, animalPartsMask = 0;
  public int[] animalParts = {-1, -1, -1, -1, -1, -1, -1, -1};
  public boolean flameOrb = false, ghostBody = false, hatsUnlocked = false;
  // Theme identity is independent of genes and remains saved if its source mod is removed.
  // Runtime admission and appearance gates are deliberately outside this game-independent state.
  public String variant = "normal";
  public int fruitCount = 0, driveCount = 0, petCount = 0, cocoonTicks = 0;
  public long entropy = 0x4c48414f47415244L;
  // Identity must not follow the mutable PRNG entropy. Kept outside extension
  // traits so a full feature map can never make ancestry identity unsaveable.
  private long lineageIdentity=entropy;
  private boolean monsterEnabled=false,monsterTransparencyFix=false;
  // A signed direction is not an age/maturity magnitude. These values must
  // persist outside the bounded feature map, just like lineageIdentity.
  private double gradualStrength=0;
  private int evolutionAgeCheckpoint=0;
  private boolean evolutionClockInitialized=true,firstEvolutionLocked=false;
  private float firstEvolutionAlignment=0,monsterSwimFly=0,monsterRunPower=0;
  private int firstEvolutionType=NORMAL;
  private int[] trainingRecency={-1,-1,-1,-1,-1};
  private boolean robotLifeInitialized=false;
  private int robotPendingOutcome=0;
  private boolean robotOutputCommitted=false;

  public static final int HIDDEN_LUCK=0, HIDDEN_INTELLIGENCE=1;
  public int[] luckGenes={0,0}, intelligenceGenes={0,0};
  public int luckGrade=0, intelligenceGrade=0;
  public int[] hiddenLevels={0,0}, hiddenProgress={0,0};
  public int[] personalitySliders=new int[ChaoPersonality.COUNT];
  public int[] personalityGenesA=neutralPersonalityGenes(), personalityGenesB=neutralPersonalityGenes();
  public int[] kindnessGenes={2,2},aggressiveGenes={2,2};
  private int lastKindA=2,lastKindB=2,lastAggressiveA=2,lastAggressiveB=2;
  public boolean personalityProfileInitialized=false;
  public int legacyProfileLabel=-1;
  int[] emotions=new int[ChaoEmotion.COUNT];
  int[] illnessValues=new int[ChaoIllness.COUNT], illnessCooldown=new int[ChaoIllness.COUNT];
  private int lastFullness=80,lastEnergy=100,lastBoredom=0;
  public int eyeColor=0,heroBrushType=0;
  public String character="normal";
  public boolean cosmeticWings=false,negativePalette=false,brightCoat=false;
  private final TreeMap<String,String> traits=new TreeMap<>();
  private static final int MAX_TRAITS=256,MAX_TRAIT_BYTES=20000;


  public ChaoState() {emotions[ChaoEmotion.HUNGRY]=2000;emotions[ChaoEmotion.ENERGY]=10000;emotions[ChaoEmotion.NOURISH]=10000;}
  private static int[] neutralPersonalityGenes(){int[] a=new int[ChaoPersonality.COUNT];Arrays.fill(a,2);return a;}


  /** A fresh shop/loot egg is homozygous; bred eggs inherit two parental alleles. */
  public static ChaoState random(RandomGenerator random) {return random(random,0);}
  public static ChaoState random(RandomGenerator random,int newbornEyeChance) {
    ChaoState result = new ChaoState();
    result.entropy = random.nextLong();
    result.lineageIdentity=result.entropy;
    ChaoPersonality.initialize(result,random);
    result.favoriteFruit = random.nextInt(3);
    result.favoriteFruitGenes = new int[] {result.favoriteFruit, result.favoriteFruit};
    for (int i = 0; i < 5; i++) {
      result.gradeA[i] = random.nextInt(6);
      result.gradeB[i] = result.gradeA[i];
      result.grades[i] = result.gradeA[i];
    }
    // Hidden stat alleles are byte ranks, not E-S letters. The shop distribution
    // of 0-9 is a bounded Minecraft balance choice; inherited alleles allow 255.
    result.luckGrade=random.nextInt(10);result.intelligenceGrade=random.nextInt(10);
    result.luckGenes=new int[]{result.luckGrade,result.luckGrade};
    result.intelligenceGenes=new int[]{result.intelligenceGrade,result.intelligenceGrade};
    result.selectNewbornEyes(newbornEyeChance,random);
    return result;
  }

  /** A loot egg fixes appearance while leaving stat genes randomized. */
  public static ChaoState egg(
      int color, boolean shiny, boolean twoTone, int jewel, RandomGenerator random) {
    return egg(color,shiny,twoTone,jewel,random,0);
  }
  public static ChaoState egg(int color,boolean shiny,boolean twoTone,int jewel,RandomGenerator random,int newbornEyeChance) {
    ChaoState result = random(random,newbornEyeChance);
    result.expressedColor = clamp(color, 0, COLORS.length - 1);
    result.colorGenes = new int[] {result.expressedColor, result.expressedColor};
    result.twoTone = twoTone;
    result.toneGenes = new int[] {twoTone ? 1 : 0, twoTone ? 1 : 0};
    result.shiny = shiny;
    result.shinyGenes = new int[] {shiny ? 1 : 0, shiny ? 1 : 0};
    result.expressedJewel = clamp(jewel, 0, JEWELS.length - 1);
    result.jewelGenes = new int[] {result.expressedJewel, result.expressedJewel};
    return result;
  }

  /** Genderless Mendelian breeding. Acquired parts, care and training do not pass on. */
  public static ChaoState breed(ChaoState a, ChaoState b, RandomGenerator random) {
    return breed(a,b,random,false);
  }
  public static ChaoState breed(ChaoState a,ChaoState b,RandomGenerator random,boolean allowRare) {
    return breed(a,b,random,allowRare,0);
  }
  public static ChaoState breed(ChaoState a,ChaoState b,RandomGenerator random,boolean allowRare,int newbornEyeChance) {
    if (!a.canBreed(allowRare) || !b.canBreed(allowRare))
      throw new IllegalArgumentException("Only living adult non-Chaos Chao can breed");
    ChaoState child = random(random);
    child.colorGenes = inherit(a.colorGenes, b.colorGenes, random);
    child.toneGenes = inherit(a.toneGenes, b.toneGenes, random);
    child.shinyGenes = inherit(a.shinyGenes, b.shinyGenes, random);
    child.jewelGenes = inherit(a.jewelGenes, b.jewelGenes, random);
    child.expressedColor = dominant(child.colorGenes, random);
    child.twoTone = child.toneGenes[random.nextInt(2)] != 0;
    child.shiny = child.shinyGenes[0] != 0 || child.shinyGenes[1] != 0;
    child.expressedJewel = dominant(child.jewelGenes, random);
    for (int stat = 0; stat < 5; stat++) {
      child.gradeA[stat] = random.nextBoolean() ? a.gradeA[stat] : a.gradeB[stat];
      child.gradeB[stat] = random.nextBoolean() ? b.gradeA[stat] : b.gradeB[stat];
      int first=child.gradeA[stat], second=child.gradeB[stat];
      child.grades[stat] = first==second ? first
          : random.nextInt(10)<7 ? Math.max(first,second) : Math.min(first,second);
    }
    child.favoriteFruitGenes = inherit(a.favoriteFruitGenes, b.favoriteFruitGenes, random);
    child.favoriteFruit = child.favoriteFruitGenes[random.nextInt(2)];
    child.luckGenes=inherit(a.luckGenes,b.luckGenes,random);
    child.intelligenceGenes=inherit(a.intelligenceGenes,b.intelligenceGenes,random);
    child.luckGrade=expressHidden(child.luckGenes,random);
    child.intelligenceGrade=expressHidden(child.intelligenceGenes,random);
    ChaoPersonality.inherit(child,a,b,random);
    child.recordParentage(a,b);
    String firstVariant = normalizeVariant(a.variant), secondVariant = normalizeVariant(b.variant);
    child.variant =
        firstVariant.equals(secondVariant)
            ? firstVariant
            : random.nextBoolean() ? firstVariant : secondVariant;
    child.selectNewbornEyes(newbornEyeChance,random);
    return child;
  }

  public boolean canBreed() { return canBreed(false); }
  public boolean canBreed(boolean allowRare) {
    if(OmochaoRules.isOmochao(this))return false;
    retireRemovedCharacters();
    return adult && (allowRare || !immortal && "normal".equals(character)) && !dead && cocoonTicks <= 0;
  }
  public boolean allowsAnimalParts(){return !immortal&&"normal".equals(character)||intTrait("rare.parts")==1;}


  public int baseColor() {
    return expressedColor;
  }

  public String baseColorName() {
    return title(COLORS[expressedColor]);
  }

  public boolean isShiny() {
    return shiny;
  }

  public boolean isTwoTone() {
    return twoTone;
  }

  public int jewel() {
    return expressedJewel;
  }

  public String alignmentName() {
    return align >= .5f ? "Hero" : align <= -.5f ? "Dark" : "Neutral";
  }

  public String stageName() {
    return dead ? "Passed away" : immortal && characterDisplayName().isEmpty() ? "Chaos Chao" : adult ? "Adult" : "Child";
  }

  public float evolutionStrength(){return (float)GradualEvolutionRules.strength(gradualStrength,adult);}
  public float gradualVisualStrength(){return Math.min(1,evolutionStrength()/GradualEvolutionRules.ADULT_STRENGTH_MAX);}
  public void setEvolutionStrength(float value){
    gradualStrength=GradualEvolutionRules.strength(value,adult);
    evolutionAgeCheckpoint=adult?adultAgeTicks:ageTicks;evolutionClockInitialized=true;
  }
  /** Called once after one successfully eaten whole four-bite fruit. */
  public void onFruitEaten(){if(!dead)gradualStrength=GradualEvolutionRules.strength(gradualStrength+(adult?.005:.02),adult);}
  void recordCreativeTraining(int stat){trainingRecency=GradualEvolutionRules.recordIncrease(trainingRecency,stat);}
  int[] trainingHistorySnapshot(){return trainingRecency.clone();}
  void recordTrainingTransaction(int[] previous,int[] beforeTotals,int[] beforePoints){
    boolean[] increased=new boolean[5];
    for(int stat=0;stat<5;stat++)increased[stat]=HeroRunMorphology052.totalTraining(this,stat)>beforeTotals[stat]||points[stat]>beforePoints[stat];
    trainingRecency=GradualEvolutionRules.recordIncreases(previous,increased);
  }
  public int lastTrainedStat(){return trainingRecency[0];}
  public boolean robotLifeInitialized(){return robotLifeInitialized;}
  public void markRobotLifeInitialized(){robotLifeInitialized=true;}
  public int robotPendingOutcome(){return robotPendingOutcome;}
  public void setRobotPendingOutcome(int outcome){robotPendingOutcome=clamp(outcome,0,2);}
  public boolean robotOutputCommitted(){return robotOutputCommitted;}
  public void markRobotOutputCommitted(){robotOutputCommitted=true;}
  public void setRobotOutputCommitted(boolean committed){robotOutputCommitted=committed;}
  public int prospectivePrimaryType(){return GradualEvolutionRules.firstType(points,trainingRecency);}
  /** Legacy Normal admission is retained only for the existing rare Chaos route. */
  public boolean chaosNormalCandidate(){return GradualEvolutionRules.legacyNormalForChaos(points,trainingRecency);}
  /** Read-only view: rendering must not create a new identity lock or mutate axes. */
  public int lockedPrimaryType(){return adult?(firstEvolutionLocked?firstEvolutionType:clamp(primaryType,0,4)):prospectivePrimaryType();}
  public float lockedAlignment(){return adult&&firstEvolutionLocked?firstEvolutionAlignment:Float.isFinite(align)?clamp(align,-1,1):0;}
  public String maturityName(){
    if(dead)return "Passed away";if(!adult)return "Child";
    if(immortal)return characterDisplayName().isEmpty()?"Chaos Chao":"Adult";
    return evolutionStrength()>=1.199999f?"Fully Adult":"Young Adult";
  }
  /** Restores locked first-evolution identity after external/public-field edits. */
  private void normalizeEvolutionState(){
    swimFly=GradualEvolutionRules.ordinaryAxis(swimFly);runPower=GradualEvolutionRules.ordinaryAxis(runPower);
    if(adult){
      if(!firstEvolutionLocked){firstEvolutionAlignment=clamp(align,-1,1);firstEvolutionType=clamp(primaryType,0,4);firstEvolutionLocked=true;}
      align=firstEvolutionAlignment;primaryType=firstEvolutionType;
    }else firstEvolutionLocked=false;
  }
  private void advanceEvolutionClock(int childDuration){
    int age=adult?adultAgeTicks:ageTicks;double divisor=GradualEvolutionRules.durationTicks(childDuration)*(adult?4.0:1.0);
    if(!evolutionClockInitialized){gradualStrength=GradualEvolutionRules.strength(gradualStrength+age/divisor,adult);evolutionAgeCheckpoint=age;evolutionClockInitialized=true;}
    else {
      gradualStrength=GradualEvolutionRules.strength(gradualStrength+Math.max(0L,(long)age-evolutionAgeCheckpoint)/divisor,adult);
      evolutionAgeCheckpoint=age;
    }
  }

  public String typeName() {
    String namedCharacter=characterDisplayName();
    if(!namedCharacter.isEmpty())return namedCharacter;
    return immortal
        ? (align >= .5f ? "Angel" : align <= -.5f ? "Devil" : "Light")
        : TYPES[clamp(primaryType, 0, 4)] + (adult ? "/" + secondaryTypeName() : "");
  }

  /** A rare character can be immortal without being a genetic Chaos Chao. */
  private String characterDisplayName() {
    String id=normalizeCharacter(character);
    return "normal".equals(id)?"":"omochao".equals(id)?"Omochao":title(id)+" Chao";
  }

  /** Both adult influence axes can be expressed together; this is not a second cocoon. */
  public int[] secondaryTypes() {
    if (!adult || immortal) return new int[]{NORMAL};
    // The scoped Neutral/Hero first-type foundations read current training,
    // including reductions and absolute Creative edits. Labels use their
    // renderer's axes without rewriting older saved influence fields or paths.
    var neutral=NeutralMorphology056.sample(this);
    var heroNormal=HeroNormalMorphology057.sample(this);
    var neutralSwim=NeutralSwimMorphology059.sample(this);
    float sfAxis=neutralSwim.enabled()?neutralSwim.swimFly():neutral.enabled()?neutral.swimFly():heroNormal.enabled()?heroNormal.swimFly():swimFly;
    float rpAxis=neutralSwim.enabled()?neutralSwim.runPower():neutral.enabled()?neutral.runPower():heroNormal.enabled()?heroNormal.runPower():runPower;
    int sf=Math.abs(sfAxis)>=.5f ? sfAxis<0 ? SWIM_TYPE : FLY_TYPE : NORMAL;
    int rp=Math.abs(rpAxis)>=.5f ? rpAxis<0 ? RUN_TYPE : POWER_TYPE : NORMAL;
    if(sf!=NORMAL && rp!=NORMAL)return new int[]{sf,rp};
    if(sf!=NORMAL)return new int[]{sf};
    if(rp!=NORMAL)return new int[]{rp};
    return new int[]{NORMAL};
  }

  public String secondaryFormKey() {
    int[] types=secondaryTypes();
    String first=TYPES[types[0]].toLowerCase(java.util.Locale.ROOT);
    return types.length==1 ? first : first+"_"+TYPES[types[1]].toLowerCase(java.util.Locale.ROOT);
  }

  private String secondaryTypeName() {
    int[] types=secondaryTypes();
    return TYPES[types[0]]+(types.length==2?"+"+TYPES[types[1]]:"");
  }

  public String colorName() {
    return (shiny ? "Shiny " : "")
        + (expressedJewel > 0
            ? title(JEWELS[expressedJewel]) + " Jewel"
            : title(COLORS[expressedColor]))
        + (twoTone ? " Two-tone" : " Mono-tone");
  }

  public String statSummary(int stat) {
    return STATS[stat]
        + " "
        + gradeName(grades[stat])
        + " Lv."
        + levels[stat]
        + " ("
        + points[stat]
        + ")";
  }

  public static String gradeName(int grade) {
    return new String[] {"E", "D", "C", "B", "A", "S", "X"}[clamp(grade, 0, 6)];
  }

  /** Only the live equipped Chao runs this clock. Its lifespan and birthdays stay paused. */
  public void tickEquippedCare(int careTick, int childDuration) {
    if (dead || cocoonTicks > 0) return;
    if (adult) {
    advanceEvolutionClock(childDuration); // Initialize old saves from their last real garden age.
    double delta = 1.0 / (4.0 * GradualEvolutionRules.durationTicks(childDuration));
    gradualStrength = GradualEvolutionRules.strength(gradualStrength + delta, true);
    } // Babies retain their paused childhood clock while being carried.
    if (careTick % ChaoPreferences.FULLNESS_DECAY_TICKS == 0) hunger = Math.max(0, hunger - 1);
    if (careTick % 600 == 0) energy = Math.max(0, energy - 1);
    if (careTick == 6000) {
      if (hunger < 10) happiness = Math.max(-100, happiness - 2);
      else if (energy > 5) bond = Math.min(100, bond + 1);
    }
  }

  /** A tick is one loaded Minecraft tick; offline time intentionally does not count. */
  public Transition tick(int childDuration, int adultDuration, RandomGenerator random) {
    OmochaoRules.normalize(this);normalizeEvolutionState();
    if (dead) return Transition.NONE;
    if (cocoonTicks > 0) {
      cocoonTicks--;
      return Transition.NONE;
    }
    if (ageTicks < Integer.MAX_VALUE - 1) ageTicks++;
    if (ageTicks % ChaoPreferences.FULLNESS_DECAY_TICKS == 0) hunger = Math.max(0, hunger - 1);
    ChaoEmotion.tick(this);
    if(!OmochaoRules.isOmochao(this))ChaoIllness.tick(this,random);
    if (!sleeping && ageTicks % 600 == 0) energy = Math.max(0, energy - 1);
    if (ageTicks % 6000 == 0 && hunger < 10) happiness = Math.max(-100, happiness - 2);
    if(OmochaoRules.isOmochao(this)){
      Transition transition=OmochaoRules.tickRobot(this,adultDuration);advanceEvolutionClock(childDuration);return transition;
    }
    if (immortal) {
      // Adult calendar time also drives birthdays. Immortality suppresses the
      // lifespan transition, not this loaded-time counter or its persistence.
      if(adult&&adultAgeTicks<Integer.MAX_VALUE-1)adultAgeTicks++;
      advanceEvolutionClock(childDuration);
      return Transition.NONE;
    }
    if(!adult)advanceEvolutionClock(childDuration);
    if (!adult && (ageTicks >= GradualEvolutionRules.durationTicks(childDuration)||gradualStrength>=1)) {
      boolean chaosNormal=chaosNormalCandidate();
      primaryType = prospectivePrimaryType();
      if (reincarnations >= 2
          && happiness > 80
          && (animalMask & ALL_ANIMALS) == ALL_ANIMALS
          && chaosNormal) {
        immortal = true;
        primaryType = NORMAL;
        clearParts();
        flameOrb = ghostBody = false;
      }
      adult = true;
      adultAgeTicks = 0;
      gradualStrength=0;evolutionAgeCheckpoint=0;evolutionClockInitialized=true;
      firstEvolutionLocked=false;normalizeEvolutionState();
      secondaryType = NORMAL;
      int stat = typeStat(primaryType);
      if (stat >= 0 && grades[stat] < 5) {
        int expressedGrade = grades[stat];
        grades[stat]++;
        // Upgrade only the displayed allele; the hidden allele is unchanged.
        // When both genes match, either allele is an equivalent displayed copy.
        if (gradeA[stat] == expressedGrade) gradeA[stat] = grades[stat];
        else if (gradeB[stat] == expressedGrade) gradeB[stat] = grades[stat];
        else gradeA[stat] = grades[stat]; // Repair inconsistent external state.
      }
      swimFly = runPower = 0;
      monsterSwimFly=monsterRunPower=0;
      cocoonTicks = 200;
      return Transition.EVOLVED;
    }
    if (adult) {
      if (adultAgeTicks < Integer.MAX_VALUE - 1) adultAgeTicks++;
      advanceEvolutionClock(childDuration);
      // Adult tendencies remain reversible as drives/genes move either axis.
      // Their visual magnitude is continuous; there is no additional cocoon.
      secondaryType = tendencyType();
      if (adultAgeTicks >= GradualEvolutionRules.durationTicks(adultDuration)) {
        if (happiness > 50) {
          reincarnate();
          return Transition.REINCARNATED;
        }
        dead = true;
        return Transition.DIED;
      }
    }
    return Transition.NONE;
  }

  public void reincarnate() {
    if(OmochaoRules.isOmochao(this)){OmochaoRules.normalize(this);return;}
    float rebornAlignment=align>=.5f?1:align<=-.5f?-1:0;
    reincarnations=Math.min(9999,clamp(reincarnations,0,9999)+1);
    adult = immortal = dead = sleeping = false;
    ageTicks = adultAgeTicks = 0;
    gradualStrength=0;evolutionAgeCheckpoint=0;evolutionClockInitialized=true;firstEvolutionLocked=false;
    trainingRecency=new int[]{-1,-1,-1,-1,-1};monsterSwimFly=monsterRunPower=0;
    primaryType = secondaryType = NORMAL;
    swimFly = runPower = 0;
    align = rebornAlignment;
    for (int i = 0; i < 5; i++) {
      points[i] = clamp(points[i],0,9999) / 10;
      levels[i] = 1;
      progress[i] = 0;
    }
    hunger = 80;
    energy = 100;
    boredom = 0;
    ChaoPersonality.initialize(this,nextRandom());
    animalMask = 0;
    // Modern sources preserve a random subset. The documented SADX 30% per-slot
    // removal is used as the explicit Minecraft probability for both families.
    var rebirthRandom=nextRandom();
    for(int i=0;i<animalParts.length;i++)if(animalParts[i]>=0&&rebirthRandom.nextInt(10)<3)animalParts[i]=-1;
    rebuildPartsMask();
    luck=clamp(luck,0,4000)/10;intelligence=clamp(intelligence,0,4000)/10;
    Arrays.fill(hiddenLevels,1);Arrays.fill(hiddenProgress,0);
    for(int i=0;i<5;i++){
      if(grades[i]==6)grades[i]=5;if(gradeA[i]==6)gradeA[i]=5;if(gradeB[i]==6)gradeB[i]=5;
    }
    Arrays.fill(emotions,0);emotions[ChaoEmotion.NOURISH]=10000;
    emotions[ChaoEmotion.HUNGRY]=2000;emotions[ChaoEmotion.ENERGY]=10000;mirrorLegacyMeters();
    Arrays.fill(illnessValues,0);Arrays.fill(illnessCooldown,0);
    traits.remove("lesson.active");traits.remove("lesson.remaining");traits.remove("lesson.desk");traits.remove("lesson.paused");traits.remove("doctor.upgrades");traits.remove("birthday.year");traits.remove("birthday.party");
    fruitCount = driveCount = petCount = 0;
    cocoonTicks = 200;
  }

  /** Whole-fruit feeding; stat amounts use a 100-point progress bar per level. */
  public void feed(int stat, int amount, float alignmentDelta) {
    if (dead) return;
    train(stat, amount);
    hunger = clamp(hunger + 25, 0, 100);
    happiness = clamp(happiness + 3, -100, 100);
    bond = clamp(bond + 1, -100, 100);
    addEmotion(ChaoEmotion.SLEEPINESS,100);addEmotion(ChaoEmotion.JOY,3);
    addEmotion(ChaoEmotion.FEAR,-5);addEmotion(ChaoEmotion.RELAX,5);
    if (!adult) align = clamp(align + alignmentDelta, -1, 1);
    if(fruitCount<Integer.MAX_VALUE)fruitCount++;
    boredom = Math.max(0, boredom - 5);
  }

  /** Call after feeding a shaped fruit: 0 round, 1 square, 2 triangle. */
  public void feedFruit(int shape) {
    if (!dead && shape >= 0 && shape < 3 && shape == favoriteFruit)
      happiness = clamp(happiness + 5, -100, 100);
  }

  /** CA2 Energy/Peace fruit move the four documented tendencies in opposite directions. */
  public void shiftAdventurePersonality(int amount) {
    curiosityTendency=clamp(curiosityTendency+amount,-100,100);
    energeticTendency=clamp(energeticTendency+amount,-100,100);
    // The game's Naive-Normal slider is reversed: lower means more Naive.
    naiveTendency=clamp(naiveTendency-amount,-100,100);
    carefreeTendency=clamp(carefreeTendency-amount,-100,100);
    personalityProfileInitialized=true;legacyProfileLabel=-1;
    ChaoPersonality.refreshLegacy(this);
  }


  public void pet(float alignmentDelta) {
    if (dead) return;
    happiness = clamp(happiness + 2, -100, 100);
    bond = clamp(bond + 2, -100, 100);
    addEmotion(ChaoEmotion.RELAX,Math.max(5,30-personalitySlider(ChaoPersonality.FICKLE)/5));
    addEmotion(ChaoEmotion.JOY,10);addEmotion(ChaoEmotion.FEAR,-10);addEmotion(ChaoEmotion.CRY,-8);
    if (!adult) align = clamp(align + alignmentDelta, -1, 1);
    if(petCount<Integer.MAX_VALUE)petCount++;
    boredom = Math.max(0, boredom - 8);
  }

  public void train(int stat, int amount) {
    train(stat, amount, nextRandom());
  }

  public void train(int stat, int amount, RandomGenerator random) {
    if (stat < 0 || stat >= 5 || dead) return;
    int beforeLevel=levels[stat],beforeProgress=progress[stat],beforePoints=points[stat];
    int remaining = (int) Math.max(0L, Math.min(1_000_000L, (long) progress[stat] + amount));
    while (remaining >= 100 && levels[stat] < 99) {
      remaining -= 100;
      levels[stat]++;
      points[stat] = clamp(points[stat] + 13 + grades[stat] * 3 + random.nextInt(5) - 2, 0, 9999);
    }
    progress[stat] = levels[stat] == 99 ? 0 : Math.min(99, remaining);
    if(amount>0&&(levels[stat]>beforeLevel||progress[stat]>beforeProgress||points[stat]>beforePoints))
      trainingRecency=GradualEvolutionRules.recordIncrease(trainingRecency,stat);
  }

  public void giveDrive(int stat) {giveDrive(stat,false);}
  public void giveDrive(int stat,boolean allowMonster) {
    monsterEnabled=allowMonster;
    if (dead || stat < 0 || stat > 3) return;
    train(stat, 24);
    influence(stat, adult ? .025f : .05f,allowMonster);
    if(driveCount<Integer.MAX_VALUE)driveCount++;
    trainHidden(HIDDEN_LUCK,40);trainHidden(HIDDEN_INTELLIGENCE,20);
    happiness = clamp(happiness + 1, -100, 100);
  }

  public void giveAnimal(int id) {giveAnimal(id,false);}
  public void giveAnimal(int id,boolean allowRare) {giveAnimal(id,allowRare,false);}
  public void giveAnimal(int id,boolean allowRare,boolean allowMonster) {
    monsterEnabled=allowMonster;
    if (dead || OmochaoRules.isOmochao(this) || id < 0 || id >= ANIMALS.length) return;
    animalMask |= 1 << id;
    for (int i = 0; i < 4; i++) train(i, ANIMAL_STATS[id][i]);
    trainHidden(HIDDEN_LUCK,20);trainHidden(HIDDEN_INTELLIGENCE,20);
    float weight = adult ? .05f : .1f;
    if (id < 3) influence(SWIM, weight,allowMonster);
    else if (id < 6) influence(FLY, weight,allowMonster);
    else if (id < 9) influence(RUN, weight,allowMonster);
    else if (id < 12) influence(POWER, weight,allowMonster);
    else if(id==21||id==22||id==30)influence(FLY,weight,allowMonster);
    else if(id==23||id==25||id==28||id==29)influence(POWER,weight,allowMonster);
    else if(id==24||id==26)influence(RUN,weight,allowMonster);
    else influence(nextRandom().nextInt(4), weight,allowMonster);
    happiness = clamp(happiness + 2, -100, 100);
    if ((immortal||!"normal".equals(character))&&!allowRare) return;
    if(allowRare)setTrait("rare.parts",1);
    if (id == 18) flameOrb = true;
    if (id == 19) {
      clearParts();
      hatsUnlocked = true;
      ghostBody = false;
    }
    if (id == 20) {
      ghostBody = true;
      animalParts[FEET] = animalParts[TAIL] = animalParts[WINGS] = -1;
    }
    // A sample expresses one new part, rather than replacing a whole animal
    // set at once. This bounded Minecraft rule keeps each use visibly useful
    // until that set is complete, and leaves unrelated acquired parts intact.
    int supplied = PART_SLOTS[id], available = 0;
    for (int part = 0; part < animalParts.length; part++)
      if ((supplied & (1 << part)) != 0 && animalParts[part] != id) available++;
    if (available > 0) {
      int selected = nextRandom().nextInt(available);
      for (int part = 0; part < animalParts.length; part++) {
        if ((supplied & (1 << part)) == 0 || animalParts[part] == id) continue;
        if (selected-- != 0) continue;
        animalParts[part] = id;
        // Sheep and Dragon supply both ears and horns. Their own matching
        // pieces coexist; only another animal's opposite head part conflicts.
        if (part == HORNS && animalParts[EARS] != id) animalParts[EARS] = -1;
        if (part == EARS && animalParts[HORNS] != id) animalParts[HORNS] = -1;
        break;
      }
    }
    rebuildPartsMask();
  }

  /** Modern Info Center training percentages, in the simulation's 100-point bar units. */
  public static int[] animalTraining(int id) {
    return id >= 0 && id < ANIMAL_STATS.length ? ANIMAL_STATS[id].clone() : new int[4];
  }

  /** Read-only slot mask for tooltips, authored appearances and validation. */
  public static int animalSlots(int id) {
    return id >= 0 && id < PART_SLOTS.length ? PART_SLOTS[id] : 0;
  }

  public void influence(int stat,float amount) {influence(stat,amount,false);}
  public void influence(int stat,float amount,boolean allowMonster) {
    if(!Float.isFinite(amount)||dead||stat<0||stat>3)return;
    float delta=(stat==SWIM||stat==RUN)?-amount:amount;
    boolean sf=stat==SWIM||stat==FLY;
    float old=sf?swimFly:runPower;
    if(allowMonster&&adult&&!immortal&&points[stat]>=2000){
      if(sf)monsterSwimFly=clamp((Math.abs(monsterSwimFly)>1?monsterSwimFly:old)+delta,-3,3);
      else monsterRunPower=clamp((Math.abs(monsterRunPower)>1?monsterRunPower:old)+delta,-3,3);
    }else if(sf)monsterSwimFly=0;else monsterRunPower=0;
    if(sf)swimFly=GradualEvolutionRules.ordinaryAxis(old+delta);else runPower=GradualEvolutionRules.ordinaryAxis(old+delta);
  }
  public float optionalMonsterSwimFly(){return monsterSwimFly;}
  public float optionalMonsterRunPower(){return monsterRunPower;}
  /** Server options are copied to saved state so all clients render the same optional form. */
  public void refreshMonsterEvolution(boolean enabled,boolean fixTransparency){monsterEnabled=enabled;monsterTransparencyFix=fixTransparency;}
  public void refreshMonsterEvolution(boolean enabled){refreshMonsterEvolution(enabled,monsterTransparencyFix);}
  private int monsterStat(){
    if(!monsterEnabled||!adult||immortal||dead||!"normal".equals(character))return -1;
    int chosen=-1;float magnitude=1.00001f;
    int sf=monsterSwimFly<0?SWIM:FLY,rp=monsterRunPower<0?RUN:POWER;
    if(Math.abs(monsterSwimFly)>magnitude&&points[sf]>=2000){chosen=sf;magnitude=Math.abs(monsterSwimFly);}
    if(Math.abs(monsterRunPower)>magnitude&&points[rp]>=2000)chosen=rp;
    // Source says high Stamina extends Normal development, but gives no exact
    // magnitude equation. Balanced Normal +2000/3000 is the declared projection.
    if(chosen<0&&primaryType==NORMAL&&Math.max(Math.abs(swimFly),Math.abs(runPower))<.5f&&points[STAMINA]>=2000)return STAMINA;
    return chosen;
  }
  public int monsterStage(){int stat=monsterStat();return stat<0?0:points[stat]>=3000?2:1;}
  public int monsterType(){int stat=monsterStat();return stat<0?NORMAL:stat==STAMINA?NORMAL:stat+1;}
  public float monsterInfluence(){
    int stat=monsterStat();if(stat<0)return 1;
    if(stat==STAMINA)return clamp(1.01f+(points[STAMINA]-2000)/1000f,1,3);
    return clamp(stat==SWIM||stat==FLY?Math.abs(monsterSwimFly):Math.abs(monsterRunPower),1,3);
  }
  public float monsterAlpha(){
    int stage=monsterStage();return stage==0||monsterType()==NORMAL||expressedColor!=0||expressedJewel!=0||monsterTransparencyFix?1:stage==1?.78f:.58f;
  }
  private void selectNewbornEyes(int chance,RandomGenerator random){if(chance>0&&random.nextInt(100)<clamp(chance,0,100))eyeColor=random.nextInt(9);}


  public int tendencyType() {
    float sf = Math.abs(swimFly), rp = Math.abs(runPower);
    if (Math.max(sf, rp) + .00001f < .5f) return NORMAL;
    // Swim wins exact ties, followed by Run/Power, then Fly.
    if (swimFly < 0 && sf + .00001f >= rp) return SWIM_TYPE;
    if (rp + .00001f >= sf) return runPower < 0 ? RUN_TYPE : POWER_TYPE;
    return FLY_TYPE;
  }

  public void clearParts() {
    Arrays.fill(animalParts, -1);
    animalPartsMask = 0;
  }

  private void rebuildPartsMask() {
    animalPartsMask = 0;
    for (int animal : animalParts) if (animal >= 0 && animal < ANIMALS.length) animalPartsMask |= 1 << animal;
  }

  /** Compact bounded persistence, safe to embed in NBT or entity data. */
  public String encode() {
    retireRemovedCharacters();OmochaoRules.normalize(this);normalizeEvolutionState();
    syncLegacyMeters();syncPersonalityGeneAliases();
    StringBuilder result = new StringBuilder("6");
    append(result, colorGenes);
    append(result, toneGenes);
    append(result, shinyGenes);
    append(result, jewelGenes);
    append(result, gradeA);
    append(result, gradeB);
    append(result, grades);
    append(result, levels);
    append(result, progress);
    append(result, points);
    append(
        result,
        new int[] {
          expressedColor,
          expressedJewel,
          twoTone ? 1 : 0,
          shiny ? 1 : 0,
          favoriteFruit,
          intelligence,
          luck,
          ageTicks,
          adultAgeTicks,
          reincarnations,
          adult ? 1 : 0,
          immortal ? 1 : 0,
          dead ? 1 : 0,
          primaryType,
          secondaryType,
          happiness,
          bond,
          hunger,
          energy,
          sleeping ? 1 : 0,
          animalMask,
          animalPartsMask,
          flameOrb ? 1 : 0,
          ghostBody ? 1 : 0,
          hatsUnlocked ? 1 : 0,
          fruitCount,
          driveCount,
          petCount,
          cocoonTicks,
          personality,
          boredom,
          curiosityTendency,
          energeticTendency,
          naiveTendency,
          carefreeTendency,
          favoriteFruitGenes[0],
          favoriteFruitGenes[1]
        });
    append(result, animalParts);
    result
        .append(';')
        .append(Float.toHexString(align))
        .append(',')
        .append(Float.toHexString(swimFly))
        .append(',')
        .append(Float.toHexString(runPower));
    result.append(';').append(entropy);
    append(result,ChaoPersonality.sliders(this));append(result,personalityGenesA);append(result,personalityGenesB);
    append(result,emotions);append(result,illnessValues);append(result,illnessCooldown);
    append(result,new int[]{luckGenes[0],luckGenes[1],intelligenceGenes[0],intelligenceGenes[1],luckGrade,intelligenceGrade,
      hiddenLevels[0],hiddenLevels[1],hiddenProgress[0],hiddenProgress[1],personalityProfileInitialized?1:0,
      eyeColor,cosmeticWings?1:0,negativePalette?1:0,brightCoat?1:0,heroBrushType,legacyProfileLabel+1,
      (int)(lineageIdentity>>>32),(int)lineageIdentity,monsterEnabled?1:0,monsterTransparencyFix?1:0,
      (int)(Double.doubleToLongBits(gradualStrength)>>>32),(int)Double.doubleToLongBits(gradualStrength),
      trainingRecency[0],trainingRecency[1],trainingRecency[2],trainingRecency[3],trainingRecency[4],
      firstEvolutionLocked?1:0,Float.floatToIntBits(firstEvolutionAlignment),firstEvolutionType,
      Float.floatToIntBits(monsterSwimFly),Float.floatToIntBits(monsterRunPower),
      evolutionAgeCheckpoint,evolutionClockInitialized?1:0,robotLifeInitialized?1:0,robotPendingOutcome,robotOutputCommitted?1:0});
    result.append(';').append(encodeTraits());
    result.append(';').append(normalizeCharacter(character));
    result.append(';').append(normalizeVariant(variant));
    return result.toString();
  }

  /** Versions2..5 migrate without rerolling genes, identity, care or fixed adult phenotype. */
  public static ChaoState decode(String encoded) {
    ChaoState result = new ChaoState();
    if (encoded == null || encoded.length() > 32767) return result;
    try {
      // Limit the split so a corrupt theme suffix cannot damage otherwise valid numeric state.
      boolean expanded=encoded.startsWith("5;")||encoded.startsWith("6;");
      String[] fields = encoded.split(";", expanded?25:16);
      boolean oldVersion = fields.length == 15 && "2".equals(fields[0]);
      boolean priorVersion = fields.length == 16 && "3".equals(fields[0]);
      boolean currentVersion = fields.length == 16 && "4".equals(fields[0]);
      boolean expandedVersion=fields.length==25&&("5".equals(fields[0])||"6".equals(fields[0]));
      boolean gradualVersion=expandedVersion&&"6".equals(fields[0]);
      if (!oldVersion && !priorVersion && !currentVersion && !expandedVersion) return result;
      result.colorGenes = readArray(fields[1], 2, 0, COLORS.length - 1);
      result.toneGenes = readArray(fields[2], 2, 0, 1);
      result.shinyGenes = readArray(fields[3], 2, 0, 1);
      result.jewelGenes = readArray(fields[4], 2, 0, JEWELS.length - 1);
      result.gradeA = readArray(fields[5], 5, 0, expandedVersion?6:5);
      result.gradeB = readArray(fields[6], 5, 0, expandedVersion?6:5);
      result.grades = readArray(fields[7], 5, 0, expandedVersion?6:5);
      result.levels = readArray(fields[8], 5, 0, 99);
      result.progress = readArray(fields[9], 5, 0, 99);
      result.points = readArray(fields[10], 5, 0, 9999);
      int[] v = readArray(fields[11], currentVersion || expandedVersion ? 37 : 29, -Integer.MAX_VALUE, Integer.MAX_VALUE);
      result.expressedColor = clamp(v[0], 0, COLORS.length - 1);
      result.expressedJewel = clamp(v[1], 0, JEWELS.length - 1);
      result.twoTone = v[2] != 0;
      result.shiny = v[3] != 0;
      result.favoriteFruit = clamp(v[4], 0, 2);
      result.intelligence = clamp(v[5], 0, 4000);
      result.luck = clamp(v[6], 0, 4000);
      result.ageTicks = Math.max(0, v[7]);
      result.adultAgeTicks = Math.max(0, v[8]);
      result.reincarnations = clamp(v[9], 0, 9999);
      result.adult = v[10] != 0;
      result.immortal = v[11] != 0;
      result.dead = v[12] != 0;
      result.primaryType = clamp(v[13], 0, 4);
      result.secondaryType = clamp(v[14], 0, 4);
      result.happiness = clamp(v[15], -100, 100);
      result.bond = clamp(v[16], -100, 100);
      result.hunger = clamp(v[17], 0, 100);
      result.energy = clamp(v[18], 0, 100);
      result.sleeping = v[19] != 0;
      result.animalMask = v[20] & Integer.MAX_VALUE;
      result.flameOrb = v[22] != 0;
      result.ghostBody = v[23] != 0;
      result.hatsUnlocked = v[24] != 0;
      result.fruitCount = Math.max(0, v[25]);
      result.driveCount = Math.max(0, v[26]);
      result.petCount = Math.max(0, v[27]);
      result.cocoonTicks = clamp(v[28], 0, 1200);
      if (currentVersion || expandedVersion) {
        result.personality = clamp(v[29], 0, ChaoPreferences.PERSONALITIES.length-1);
        result.boredom = clamp(v[30], 0, 100);
        result.curiosityTendency=clamp(v[31],-100,100);
        result.energeticTendency=clamp(v[32],-100,100);
        result.naiveTendency=clamp(v[33],-100,100);
        result.carefreeTendency=clamp(v[34],-100,100);
        result.favoriteFruitGenes=new int[]{clamp(v[35],0,2),clamp(v[36],0,2)};
      } else {
        result.favoriteFruitGenes=new int[]{result.favoriteFruit,result.favoriteFruit};
      }
      result.animalParts = readArray(fields[12], 8, -1, ANIMALS.length-1);
      result.rebuildPartsMask();
      String[] axes = fields[13].split(",", -1);
      if (axes.length != 3) throw new IllegalArgumentException("Invalid axes");
      result.align = finiteAxis(axes[0]);
      result.swimFly = finiteAxis(axes[1],expandedVersion?3:1);
      result.runPower = finiteAxis(axes[2],expandedVersion?3:1);
      result.entropy = Long.parseLong(fields[14]);
      result.lineageIdentity=result.entropy;
      if(expandedVersion){
        result.personalitySliders=readArray(fields[15],ChaoPersonality.COUNT,-100,100);
        result.personalityGenesA=readArray(fields[16],ChaoPersonality.COUNT,0,4);
        result.personalityGenesB=readArray(fields[17],ChaoPersonality.COUNT,0,4);
        result.emotions=readArray(fields[18],ChaoEmotion.COUNT,0,10000);
        for(int i=0;i<ChaoEmotion.COUNT;i++)result.emotions[i]=Math.min(result.emotions[i],ChaoEmotion.maximum(i));
        result.illnessValues=readArray(fields[19],ChaoIllness.COUNT,-100,100);
        result.illnessCooldown=readArray(fields[20],ChaoIllness.COUNT,0,ChaoIllness.IMMUNITY_TICKS);
        int extraCount=fields[21].split(",",-1).length;
        if(extraCount!=17&&extraCount!=19&&extraCount!=21&&extraCount!=36&&extraCount!=37&&extraCount!=38)throw new IllegalArgumentException("Invalid expanded care length");
        int[] extra=readArray(fields[21],extraCount,Integer.MIN_VALUE,Integer.MAX_VALUE);
        if(extraCount>=19)result.lineageIdentity=((long)extra[17]<<32)|(extra[18]&0xffffffffL);
        if(extraCount>=21){result.monsterEnabled=extra[19]!=0;result.monsterTransparencyFix=extra[20]!=0;}
        if(extraCount>=36&&gradualVersion){
          result.gradualStrength=GradualEvolutionRules.strength(Double.longBitsToDouble(((long)extra[21]<<32)|(extra[22]&0xffffffffL)),result.adult);
          int[] recent={extra[23],extra[24],extra[25],extra[26],extra[27]};
          boolean[] seen=new boolean[5];int at=0;Arrays.fill(result.trainingRecency,-1);
          for(int stat:recent)if(stat>=0&&stat<5&&!seen[stat]){seen[stat]=true;result.trainingRecency[at++]=stat;}
          result.firstEvolutionLocked=extra[28]!=0&&result.adult;
          float firstAlign=Float.intBitsToFloat(extra[29]);result.firstEvolutionAlignment=Float.isFinite(firstAlign)?clamp(firstAlign,-1,1):result.align;
          result.firstEvolutionType=clamp(extra[30],0,4);
          float msf=Float.intBitsToFloat(extra[31]),mrp=Float.intBitsToFloat(extra[32]);
          result.monsterSwimFly=Float.isFinite(msf)?clamp(msf,-3,3):0;result.monsterRunPower=Float.isFinite(mrp)?clamp(mrp,-3,3):0;
          result.evolutionAgeCheckpoint=clamp(extra[33],0,result.adult?result.adultAgeTicks:result.ageTicks);
          result.evolutionClockInitialized=extra[34]!=0;result.robotLifeInitialized=extra[35]!=0;
          if(extraCount>=37)result.robotPendingOutcome=clamp(extra[36],0,2);
          if(extraCount>=38)result.robotOutputCommitted=extra[37]!=0;
        }else{
          result.evolutionClockInitialized=false;result.evolutionAgeCheckpoint=result.adult?result.adultAgeTicks:result.ageTicks;
        }
        result.luckGenes=new int[]{clamp(extra[0],0,255),clamp(extra[1],0,255)};
        result.intelligenceGenes=new int[]{clamp(extra[2],0,255),clamp(extra[3],0,255)};
        result.luckGrade=clamp(extra[4],0,255);result.intelligenceGrade=clamp(extra[5],0,255);
        result.hiddenLevels=new int[]{clamp(extra[6],0,99),clamp(extra[7],0,99)};
        result.hiddenProgress=new int[]{clamp(extra[8],0,99),clamp(extra[9],0,99)};
        result.personalityProfileInitialized=extra[10]!=0;result.eyeColor=clamp(extra[11],0,12);
        result.cosmeticWings=extra[12]!=0;result.negativePalette=extra[13]!=0;result.brightCoat=extra[14]!=0;
        result.heroBrushType=clamp(extra[15],0,4);result.legacyProfileLabel=clamp(extra[16],0,15)-1;
        for(int i=0;i<ChaoPersonality.COUNT;i++){
          int value=result.personalitySliders[i];
          switch(i){case ChaoPersonality.CURIOUS->result.curiosityTendency=value;
            case ChaoPersonality.ENERGETIC->result.energeticTendency=value;
            case ChaoPersonality.NAIVE->result.naiveTendency=value;case ChaoPersonality.CAREFREE->result.carefreeTendency=value;default->{}}
        }
        result.mirrorLegacyMeters();result.decodeTraits(fields[22]);
        // Read the retired label before normalizing so its former character-only
        // immortality cannot accidentally turn a Spartoi save into a Chaos Chao.
        result.character=isRetiredCharacter(fields[23])?fields[23]:normalizeCharacter(fields[23]);
        result.retireRemovedCharacters();result.variant=normalizeVariant(fields[24]);
      }else{
        result.evolutionClockInitialized=false;result.evolutionAgeCheckpoint=result.adult?result.adultAgeTicks:result.ageTicks;
        result.syncLegacyMeters();ChaoPersonality.migrateLegacy(result);
        if(priorVersion||currentVersion)result.variant=normalizeVariant(fields[15]);
      }
      result.syncPersonalityGeneAliases();
      OmochaoRules.normalize(result);result.normalizeEvolutionState();
      if (result.immortal&&!result.allowsAnimalParts()) result.clearParts();
      return result;
    } catch (RuntimeException ignored) {
      return new ChaoState();
    }
  }

  public int personalitySlider(int index){return ChaoPersonality.get(this,index);}
  public void setPersonalitySlider(int index,int value){legacyProfileLabel=-1;ChaoPersonality.set(this,index,value);ChaoPersonality.refreshLegacy(this);}
  public int[] personalityProfile(){return ChaoPersonality.profile(this);}
  public String doctorPersonality(long visitIndex){return ChaoPersonality.doctor(this,visitIndex);}
  public void applyHatchingMethod(boolean gentle){ChaoPersonality.applyHatchingMethod(this,gentle);}
  /** Gene snapshots for egg care; hatching changes heredity, never this creature's fixed sliders. */
  public int[] kindnessGenes(){syncPersonalityGeneAliases();return kindnessGenes.clone();}
  public int[] aggressiveGenes(){syncPersonalityGeneAliases();return aggressiveGenes.clone();}
  void syncPersonalityGeneAliases(){
    if(kindnessGenes==null||kindnessGenes.length!=2)kindnessGenes=new int[]{2,2};
    if(aggressiveGenes==null||aggressiveGenes.length!=2)aggressiveGenes=new int[]{2,2};
    if(kindnessGenes[0]!=lastKindA||kindnessGenes[1]!=lastKindB){
      personalityGenesA[ChaoPersonality.KINDNESS]=clamp(kindnessGenes[0],0,4);personalityGenesB[ChaoPersonality.KINDNESS]=clamp(kindnessGenes[1],0,4);
    }
    if(aggressiveGenes[0]!=lastAggressiveA||aggressiveGenes[1]!=lastAggressiveB){
      personalityGenesA[ChaoPersonality.ENERGETIC]=clamp(aggressiveGenes[0],0,4);personalityGenesB[ChaoPersonality.ENERGETIC]=clamp(aggressiveGenes[1],0,4);
    }
    kindnessGenes[0]=lastKindA=personalityGenesA[ChaoPersonality.KINDNESS];kindnessGenes[1]=lastKindB=personalityGenesB[ChaoPersonality.KINDNESS];
    aggressiveGenes[0]=lastAggressiveA=personalityGenesA[ChaoPersonality.ENERGETIC];aggressiveGenes[1]=lastAggressiveB=personalityGenesB[ChaoPersonality.ENERGETIC];
  }
  public int emotion(int index){return ChaoEmotion.get(this,index);}
  public void setEmotion(int index,int value){ChaoEmotion.set(this,index,value);}
  public void addEmotion(int index,int amount){ChaoEmotion.add(this,index,amount);}
  void syncLegacyMeters(){
    if(lastFullness!=hunger)emotions[ChaoEmotion.HUNGRY]=(100-clamp(hunger,0,100))*100;
    if(lastEnergy!=energy)emotions[ChaoEmotion.ENERGY]=clamp(energy,0,100)*100;
    if(lastBoredom!=boredom)emotions[ChaoEmotion.BOREDOM]=clamp(boredom,0,100)*100;
    lastFullness=hunger;lastEnergy=energy;lastBoredom=boredom;
  }
  void mirrorLegacyMeters(){
    hunger=100-emotions[ChaoEmotion.HUNGRY]/100;energy=emotions[ChaoEmotion.ENERGY]/100;boredom=emotions[ChaoEmotion.BOREDOM]/100;
    lastFullness=hunger;lastEnergy=energy;lastBoredom=boredom;
  }
  public boolean wantsSleep(){return emotion(ChaoEmotion.SLEEPINESS)>=8000||energy<15;}
  public boolean readyToWake(){return emotion(ChaoEmotion.SLEEPINESS)<1000&&emotion(ChaoEmotion.TIREDNESS)<1000&&energy>=90;}
  /** Host supplies motion because the pure simulation has no world/navigation dependency. */
  public void onGardenMotion(boolean moving){
    if(dead||cocoonTicks>0||ageTicks<=0||ageTicks%20!=0)return;
    int nourish=emotion(ChaoEmotion.NOURISH);
    if(nourish<2500)addEmotion(ChaoEmotion.CONDITION,-30);
    else if(!moving&&nourish<9900)addEmotion(ChaoEmotion.CONDITION,4);
  }
  public void onHurt(){
    if(dead)return;
    int carefree=Math.max(0,personalitySlider(ChaoPersonality.CAREFREE));
    int dislike=Math.max(1,5-carefree/25);
    happiness=clamp(happiness-dislike,-100,100);bond=clamp(bond-dislike,-100,100);
    addEmotion(ChaoEmotion.ANGER,Math.max(3,20-carefree/8));
    addEmotion(ChaoEmotion.CRY,Math.max(3,20-personalitySlider(ChaoPersonality.ENERGETIC)/8));
    addEmotion(ChaoEmotion.FEAR,Math.max(3,18+personalitySlider(ChaoPersonality.NAIVE)/10));
    addEmotion(ChaoEmotion.SURPRISE,30);addEmotion(ChaoEmotion.RELAX,-50);
  }
  public void onFoodRejected(){addEmotion(ChaoEmotion.ANGER,3);addEmotion(ChaoEmotion.CRY,3);}
  public void onSleepDisturbed(){sleeping=false;addEmotion(ChaoEmotion.TIREDNESS,3000);addEmotion(ChaoEmotion.ANGER,10);addEmotion(ChaoEmotion.SURPRISE,20);}
  public void onSocialInteraction(boolean gentle){
    addEmotion(ChaoEmotion.LONELY,-2500);addEmotion(ChaoEmotion.BOREDOM,-1000);
    if(gentle){addEmotion(ChaoEmotion.JOY,30);addEmotion(ChaoEmotion.FEAR,-10);}
    else{addEmotion(ChaoEmotion.ANGER,15);addEmotion(ChaoEmotion.CRY,10);}
  }
  public int illness(int index){return !OmochaoRules.isOmochao(this)&&index>=0&&index<ChaoIllness.COUNT?illnessValues[index]:0;}
  public int illnessImmunityTicks(int index){return !OmochaoRules.isOmochao(this)&&index>=0&&index<ChaoIllness.COUNT?illnessCooldown[index]:0;}
  public void setIllness(int index,int value){if(OmochaoRules.isOmochao(this)||index<0||index>=ChaoIllness.COUNT)return;illnessValues[index]=clamp(value,-100,100);illnessCooldown[index]=value>0?ChaoIllness.IMMUNITY_TICKS:0;}
  public boolean isIll(){if(OmochaoRules.isOmochao(this))return false;for(int i:illnessValues)if(i<0)return true;return false;}
  public String illnessName(){if(OmochaoRules.isOmochao(this))return "Healthy";for(int i=0;i<ChaoIllness.COUNT;i++)if(illnessValues[i]<0)return ChaoIllness.NAMES[i];return "Healthy";}
  public boolean cureIllness(int index){if(index<0||index>=ChaoIllness.COUNT||illnessValues[index]>=0)return false;illnessValues[index]=100;illnessCooldown[index]=ChaoIllness.IMMUNITY_TICKS;return true;}
  public int cureAllIllnesses(){int n=0;for(int i=0;i<ChaoIllness.COUNT;i++)if(cureIllness(i))n++;return n;}
  public boolean riskIllness(int index,int denominator,RandomGenerator random){return ChaoIllness.risk(this,index,denominator,random);}
  public boolean canTrainHidden(int stat){return stat>=0&&stat<2&&hiddenLevels[stat]<99&&(stat==HIDDEN_LUCK?luck:intelligence)<4000;}
  public void trainHidden(int stat,int amount){trainHidden(stat,amount,nextRandom());}
  public void trainHidden(int stat,int amount,RandomGenerator random){
    if(stat<0||stat>1||dead)return;int rest=(int)Math.max(0L,Math.min(1000000L,(long)hiddenProgress[stat]+amount));
    int points=stat==HIDDEN_LUCK?luck:intelligence,grade=stat==HIDDEN_LUCK?luckGrade:intelligenceGrade;
    while(rest>=100&&hiddenLevels[stat]<99){rest-=100;hiddenLevels[stat]++;points=clamp(points+13+3*clamp(grade,0,255)+random.nextInt(5)-2,0,4000);}
    hiddenProgress[stat]=hiddenLevels[stat]>=99?0:Math.min(99,rest);
    if(stat==HIDDEN_LUCK)luck=points;else intelligence=points;
  }
  public boolean hasDriveBenefit(int stat){return hasDriveBenefit(stat,false);}
  public boolean hasDriveBenefit(int stat,boolean allowMonster){
    if(stat<0||stat>3||dead)return false;
    float cap=allowMonster&&adult&&!immortal&&points[stat]>=2000?3:1;
    float sf=cap>1&&Math.abs(monsterSwimFly)>1?monsterSwimFly:swimFly,rp=cap>1&&Math.abs(monsterRunPower)>1?monsterRunPower:runPower;
    boolean axis=switch(stat){case SWIM->sf> -cap;case FLY->sf<cap;case RUN->rp> -cap;default->rp<cap;};
    return levels[stat]<99||axis||canTrainHidden(HIDDEN_LUCK)||canTrainHidden(HIDDEN_INTELLIGENCE)||happiness<100;
  }
  private static int expressHidden(int[] genes,RandomGenerator random){
    // The source specifies a diploid byte grade; higher-grade bias follows the
    // common stat-grade rule. It never averages the parents' trained points.
    return genes[0]==genes[1]?genes[0]:random.nextInt(10)<7?Math.max(genes[0],genes[1]):Math.min(genes[0],genes[1]);
  }
  public boolean canUpgradeGrade(int stat){return !OmochaoRules.isOmochao(this)&&stat>=0&&stat<5&&!dead&&levels[stat]==99&&grades[stat]<4&&intTrait("doctor.upgrades")<3;}
  public int gradeUpgradeCostRings(int stat){return stat>=0&&stat<5?10000*(clamp(grades[stat],0,5)+1):0;}
  public boolean upgradeGrade(int stat){
    if(!canUpgradeGrade(stat)||!setTrait("doctor.upgrades",intTrait("doctor.upgrades")+1))return false;
    upgradeExpressedGrade(stat,grades[stat]+1);levels[stat]=progress[stat]=points[stat]=0;return true;
  }
  private void upgradeExpressedGrade(int stat,int grade){int old=grades[stat];grades[stat]=grade;if(gradeA[stat]==old)gradeA[stat]=grade;else if(gradeB[stat]==old)gradeB[stat]=grade;else gradeA[stat]=grade;}
  public boolean useHyperFruit(int stat){
    if(dead||OmochaoRules.isOmochao(this)||stat<0||stat>3||grades[stat]!=5)return false;
    for(int grade:grades)if(grade==6)return false;
    // X is a temporary per-life boost. Do not invent hereditary X alleles.
    grades[stat]=6;return true;
  }
  /** Stable save-local identity for the bounded family ledger, not a world entity UUID. */
  public String lineageId(){return String.format(java.util.Locale.ROOT,"%016x",lineageIdentity);}
  /** Compact genotype only: acquired training, parts, and care never enter a descendant's ledger. */
  public String dnaSummary(){
    syncPersonalityGeneAliases();
    return "C:"+pair(colorGenes)+" T:"+pair(toneGenes)+" S:"+pair(shinyGenes)+" J:"+pair(jewelGenes)
        +" G:"+digits(gradeA)+"/"+digits(gradeB)+" L:"+pair(luckGenes)+" I:"+pair(intelligenceGenes)
        +" F:"+pair(favoriteFruitGenes)+" P:"+digits(personalityGenesA)+"/"+digits(personalityGenesB);
  }
  /** Human-readable alleles for a stored ancestor; this never changes a genotype. */
  public static java.util.List<String> describeDnaSummary(String summary){
    if(summary==null||summary.isEmpty()||summary.length()>128)return java.util.List.of("Ancestral DNA unknown");
    var result=new java.util.ArrayList<String>();
    for(String field:summary.split(" ",12)){
      if(field.length()<3||field.charAt(1)!=':')continue;
      String value=field.substring(2),text="";
      try {
        text=switch(field.charAt(0)){
          case 'C'->"Colour alleles: "+namedPair(value,COLORS);
          case 'T'->"Tone alleles: "+namedPair(value,new String[]{"mono-tone","two-tone"});
          case 'S'->"Shine alleles: "+namedPair(value,new String[]{"ordinary","shiny"});
          case 'J'->"Jewel alleles: "+namedPair(value,JEWELS);
          case 'G'->"Ability grade alleles: "+gradePairs(value);
          case 'L'->"Luck rank alleles: "+numericPair(value,255);
          case 'I'->"Intelligence rank alleles: "+numericPair(value,255);
          case 'F'->"Favourite fruit alleles: "+namedPair(value,new String[]{"round","square","triangle"});
          case 'P'->"Personality allele pairs: "+personalityPairs(value);
          default->"";
        };
      }catch(RuntimeException invalid){text="Genetic field "+field.charAt(0)+": not recorded";}
      if(!text.isEmpty())result.add(text);
    }
    return result.isEmpty()?java.util.List.of("Ancestral DNA unknown"):java.util.List.copyOf(result);
  }
  private static String[] allelePair(String value){
    String[] pair=value.split("/",-1);if(pair.length!=2)throw new IllegalArgumentException("Invalid pair");return pair;
  }
  private static int boundedAllele(String value,int max){
    int n=Integer.parseInt(value);if(n<0||n>max)throw new IllegalArgumentException("Invalid allele");return n;
  }
  private static String namedPair(String value,String[] labels){
    String[] p=allelePair(value);return title(labels[boundedAllele(p[0],labels.length-1)])+" / "+title(labels[boundedAllele(p[1],labels.length-1)]);
  }
  private static String numericPair(String value,int max){
    String[] p=allelePair(value);return boundedAllele(p[0],max)+" / "+boundedAllele(p[1],max);
  }
  private static String gradePairs(String value){
    String[] p=allelePair(value);if(p[0].length()!=5||p[1].length()!=5)throw new IllegalArgumentException("Invalid grade pairs");
    var out=new java.util.ArrayList<String>();for(int i=0;i<5;i++)out.add(STATS[i]+" "+gradeName(boundedAllele(p[0].substring(i,i+1),6))+"/"+gradeName(boundedAllele(p[1].substring(i,i+1),6)));return String.join(", ",out);
  }
  private static String personalityPairs(String value){
    String[] p=allelePair(value);if(p[0].length()!=ChaoPersonality.COUNT||p[1].length()!=ChaoPersonality.COUNT)throw new IllegalArgumentException("Invalid personality pairs");
    var out=new java.util.ArrayList<String>();for(int i=0;i<ChaoPersonality.COUNT;i++)out.add(ChaoPersonality.NAMES[i]+" "+boundedAllele(p[0].substring(i,i+1),4)+"/"+boundedAllele(p[1].substring(i,i+1),4));return String.join(", ",out);
  }

  /** Names are optional birth snapshots, kept separately from stable lineage IDs. */
  public void recordParentNames(String first,String second){
    String[] names={first,second};for(int i=0;i<2;i++)if(!trait("family.parent."+i+".id").isEmpty())
      setTrait("family.parent."+i+".name",encodeFamilyName(names[i]));
  }
  public String familyName(String prefix){return decodeFamilyName(trait(prefix+".name"));}
  /** The existing trait codec is ASCII; base64 retains Unicode names without a schema change. */
  public static String encodeFamilyName(String name){
    if(name==null)return "";String clean=name.codePoints().filter(c->!Character.isISOControl(c)).collect(StringBuilder::new,StringBuilder::appendCodePoint,StringBuilder::append).toString().strip();
    while(clean.getBytes(StandardCharsets.UTF_8).length>96)clean=clean.substring(0,clean.offsetByCodePoints(0,clean.codePointCount(0,clean.length())-1));
    return Base64.getUrlEncoder().withoutPadding().encodeToString(clean.getBytes(StandardCharsets.UTF_8));
  }
  public static String decodeFamilyName(String encoded){
    if(encoded==null||encoded.isEmpty()||encoded.length()>128)return "";
    try {
      byte[] bytes=Base64.getUrlDecoder().decode(encoded);if(bytes.length>96)return "";
      String name=new String(bytes,StandardCharsets.UTF_8);return encodeFamilyName(name).equals(encoded)?name:"";
    }catch(IllegalArgumentException invalid){return "";}
  }
  private static String pair(int[] value){return value[0]+"/"+value[1];}
  private static String digits(int[] value){StringBuilder b=new StringBuilder();for(int v:value)b.append(v);return b.toString();}
  private void recordParentage(ChaoState a,ChaoState b){
    ChaoState[] parents={a,b};
    for(int i=0;i<parents.length;i++){
      ChaoState parent=parents[i];String prefix="family.parent."+i;
      setTrait(prefix+".id",parent.lineageId());setTrait(prefix+".dna",parent.dnaSummary());
      for(int j=0;j<2;j++){
        String inherited="family.parent."+j,target="family.grandparent."+(i*2+j);
        String id=parent.trait(inherited+".id"),dna=parent.trait(inherited+".dna"),name=parent.trait(inherited+".name");
        if(!id.isEmpty())setTrait(target+".id",id);
        if(!dna.isEmpty())setTrait(target+".dna",dna);
        if(!decodeFamilyName(name).isEmpty())setTrait(target+".name",name);
      }
    }
  }
  public String trait(String key){return traits.getOrDefault(key,"");}
  public int traitInt(String key,int fallback){try{return Integer.parseInt(trait(key));}catch(RuntimeException e){return fallback;}}
  public int intTrait(String key){return traitInt(key,0);}
  public boolean setTrait(String key,int value){return setTrait(key,Integer.toString(value));}
  public boolean setTraitInt(String key,int value){return setTrait(key,value);}
  public boolean setTrait(String key,String value){
    if(key==null||!key.matches("[a-z][a-z0-9_.-]{0,47}")||value==null||value.length()>128)return false;
    for(int i=0;i<value.length();i++)if(value.charAt(i)<32||value.charAt(i)>126)return false;
    if(value.isEmpty()){traits.remove(key);return true;}
    if(!traits.containsKey(key)&&traits.size()>=MAX_TRAITS)return false;
    String old=traits.put(key,value);
    if(encodeTraits().length()>MAX_TRAIT_BYTES){if(old==null)traits.remove(key);else traits.put(key,old);return false;}return true;
  }
  public Map<String,String> traitEntries(){return Collections.unmodifiableMap(traits);}
  private String encodeTraits(){
    StringBuilder b=new StringBuilder();var encoder=Base64.getUrlEncoder().withoutPadding();
    for(var e:traits.entrySet()){if(b.length()>0)b.append(',');b.append(e.getKey()).append('=')
        .append(encoder.encodeToString(e.getValue().getBytes(StandardCharsets.US_ASCII)));}return b.toString();
  }
  private void decodeTraits(String value){
    if(value.length()>MAX_TRAIT_BYTES)throw new IllegalArgumentException("Trait budget exceeded");
    if(value.isEmpty())return;for(String entry:value.split(",",-1)){
      int i=entry.indexOf('=');if(i<1)throw new IllegalArgumentException("Invalid trait");
      String key=entry.substring(0,i),v=new String(Base64.getUrlDecoder().decode(entry.substring(i+1)),StandardCharsets.US_ASCII);
      if(!setTrait(key,v))throw new IllegalArgumentException("Invalid trait value");
    }
  }
  public static String normalizeCharacter(String value){
    if(value==null||value.isEmpty()||value.length()>32||!value.matches("[a-z][a-z0-9_]*")||isRetiredCharacter(value))return "normal";return value;
  }
  public static boolean isRetiredCharacter(String value){return "spartoi".equals(value)||"egg".equals(value);}
  /** One-time0.20 compatibility migration; ordinary egg items are unrelated. */
  public void retireRemovedCharacters(){
    if("spartoi".equals(character)){
      character="normal";
      // A former immortal character gets a safe ordinary adult lifetime rather
      // than dying immediately because its old age exceeded the normal limit.
      if(immortal){immortal=false;adultAgeTicks=0;}
    }else if("egg".equals(character))character="normal";
    if(eyeColor==12)eyeColor=0;
  }

  /** Bounded, future-compatible theme IDs; source-mod availability is checked by the host. */
  public static String normalizeVariant(String value) {
    if (value == null || value.isEmpty() || value.length() > 48) return "normal";
    if (value.charAt(0) < 'a' || value.charAt(0) > 'z') return "normal";
    for (int i = 1; i < value.length(); i++) {
      char c = value.charAt(i);
      if (!(c >= 'a' && c <= 'z' || c >= '0' && c <= '9' || c == '_')) return "normal";
    }
    return value;
  }

  private static float finiteAxis(String value) {return finiteAxis(value,1);}
  private static float finiteAxis(String value,float maximum) {
    float axis = Float.parseFloat(value);
    return Float.isFinite(axis) ? clamp(axis,-maximum,maximum) : 0;
  }

  private static int[] readArray(String value, int size, int min, int max) {
    String[] parts = value.split(",", -1);
    if (parts.length != size) throw new IllegalArgumentException("Invalid array length");
    int[] values = new int[size];
    for (int i = 0; i < size; i++) values[i] = clamp(Integer.parseInt(parts[i]), min, max);
    return values;
  }

  private static void append(StringBuilder builder, int[] values) {
    builder.append(';');
    for (int i = 0; i < values.length; i++) {
      if (i > 0) builder.append(',');
      builder.append(values[i]);
    }
  }

  private RandomGenerator nextRandom() {
    entropy = entropy * 6364136223846793005L + 1442695040888963407L;
    return new Random(entropy);
  }

  private static int[] inherit(int[] a, int[] b, RandomGenerator random) {
    return new int[] {a[random.nextInt(2)], b[random.nextInt(2)]};
  }

  private static int dominant(int[] genes, RandomGenerator random) {
    if (genes[0] == 0) return genes[1];
    if (genes[1] == 0) return genes[0];
    return genes[random.nextInt(2)];
  }

  private static int typeStat(int type) {
    return switch (type) {
      case NORMAL -> STAMINA;
      case SWIM_TYPE -> SWIM;
      case FLY_TYPE -> FLY;
      case RUN_TYPE -> RUN;
      case POWER_TYPE -> POWER;
      default -> -1;
    };
  }

  private static int slots(int... slots) {
    int result = 0;
    for (int slot : slots) result |= 1 << slot;
    return result;
  }

  private static int clamp(int value, int min, int max) {
    return Math.max(min, Math.min(max, value));
  }

  private static float clamp(float value, float min, float max) {
    return Math.max(min, Math.min(max, value));
  }

  private static String title(String value) {
    String text = value.replace('_', ' ');
    return Character.toUpperCase(text.charAt(0)) + text.substring(1);
  }
}
