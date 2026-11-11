package com.chaogarden.core;

import java.util.*;

/** Independent source boundaries and observable outcomes for the expanded save/life system. */
public final class ChaoLifeSystem019Test {
  private static int checks;
  private static void check(boolean v,String message){checks++;if(!v)throw new AssertionError(message);}
  private static String legacy(int label,int curious,int energetic,int naive,int carefree){
    return "4;2,14;1,0;0,1;0,12;0,1,2,3,4;5,4,3,2,1;5,1,3,2,4;99,3,5,6,7;9,8,7,6,5;9999,321,456,222,987;"
      +"14,12,1,1,2,320,411,6178,2222,3,1,0,0,2,3,88,66,77,55,1,294912,32768,1,0,1,11,12,13,44,"
      +label+",72,"+curious+","+energetic+","+naive+","+carefree+",2,0;15,-1,15,-1,-1,-1,-1,-1;"
      +"0x1.8p-1,-0x1.0p-2,0x1.0p-1;-123456789012345678;create";
  }
  public static int run(){checks=0;migration();profiles();hiddenStats();hatching();emotionsAndIllness();rebirth();sourceAnimalsAndEggBirth();genealogy();monsterEvolution();newbornEyes();immortalAdultCalendar();traitsAndExtras();return checks;}
  private static void migration(){
    for(int label=0;label<15;label++)for(int[] changes:new int[][]{{0,0,0,0},{31,-19,-77,45}}){
      ChaoState s=ChaoState.decode(legacy(label,changes[0],changes[1],changes[2],changes[3]));
      check(s.variant.equals("create")&&s.colorGenes[1]==14&&s.jewelGenes[1]==12,"v4 keeps hidden appearance alleles/theme");
      check(s.personality==label&&s.personalityProfile()[0]==label,"Every old Doctor label remains visible after migration");
      check(s.curiosityTendency==changes[0]&&s.energeticTendency==changes[1]&&s.naiveTendency==changes[2]&&s.carefreeTendency==changes[3],"v4 actual tendency deltas are never reconstructed away");
      check(s.intelligence==320&&s.luck==411&&s.hunger==77&&s.energy==55&&s.boredom==72,"v4 trained hidden points and needs remain exact");
      check(Arrays.equals(s.favoriteFruitGenes,new int[]{2,0})&&s.animalParts[0]==15&&s.hatsUnlocked,"v4 acquired parts, unlocks and hidden preference survive");
      check(s.encode().startsWith("6;")&&s.encode().equals(ChaoState.decode(s.encode()).encode()),"Migrated v6 save is stable, with no phenotype reroll");
      s.shiftAdventurePersonality(10);
      check(s.legacyProfileLabel==-1&&s.curiosityTendency==Math.min(100,changes[0]+10),"New intentional fruit care derives profile from stored sliders");
    }
    ChaoState s=ChaoState.decode(legacy(ChaoPreferences.NAIVE,0,0,0,0));
    s.setPersonalitySlider(ChaoPersonality.NAIVE,-80);
    check(s.legacyProfileLabel==-1&&s.personalitySlider(ChaoPersonality.NAIVE)==-80,"Explicit slider edit ends legacy-label overlay");
  }
  private static void profiles(){
    ChaoState s=new ChaoState();s.setPersonalitySlider(ChaoPersonality.KINDNESS,54);
    check(Arrays.equals(s.personalityProfile(),new int[]{0}),"Threshold54 has no expressed personality label");
    s.setPersonalitySlider(ChaoPersonality.KINDNESS,55);s.setPersonalitySlider(ChaoPersonality.ENERGETIC,-55);s.setPersonalitySlider(ChaoPersonality.CURIOUS,55);
    int[] profile=s.personalityProfile();check(profile.length==3,"Doctor cycles a profile of at most three labels");
    for(int i=0;i<3;i++)check(s.doctorPersonality(i).equals(ChaoPreferences.PERSONALITIES[profile[i]]),"Doctor visits cycle all displayed profile labels");
    s.setPersonalitySlider(ChaoPersonality.BIG_EATER,80);s.hunger=100;
    check(ChaoPreferences.acceptsFood(s,"garden_fruit"),"Raw Big eater slider remains effective even when three other Doctor labels are shown");
    for(int seed=0;seed<128;seed++){
      var a=ChaoState.random(new Random(seed));
      for(int i=0;i<13;i++){
        check(a.personalityGenesA[i]>=0&&a.personalityGenesA[i]<=4&&a.personalityGenesA[i]==a.personalityGenesB[i],"New generated personality alleles are pure0..4");
        check(a.personalitySlider(i)==(a.personalityGenesA[i]-2)*40,"Source allele-to-slider formula is exact");
      }
      check(a.encode().equals(ChaoState.decode(a.encode()).encode()),"All thirteen sliders/genotypes save stably");
    }
  }
  private static void hiddenStats(){
    var a=new ChaoState();var b=new ChaoState();a.adult=b.adult=true;a.luck=b.luck=4000;a.intelligence=b.intelligence=4000;
    a.luckGenes=new int[]{0,255};b.luckGenes=new int[]{10,20};a.intelligenceGenes=new int[]{3,11};b.intelligenceGenes=new int[]{7,245};
    for(int seed=0;seed<256;seed++){
      var c=ChaoState.breed(a,b,new Random(seed));
      check((c.luckGenes[0]==0||c.luckGenes[0]==255)&&(c.luckGenes[1]==10||c.luckGenes[1]==20),"Hidden luck inherits one unaveraged allele from each parent");
      check(c.luckGrade==c.luckGenes[0]||c.luckGrade==c.luckGenes[1],"Hidden phenotype selects an actual inherited byte grade");
      check(c.intelligenceGrade==c.intelligenceGenes[0]||c.intelligenceGrade==c.intelligenceGenes[1],"Hidden intelligence expresses an inherited allele");
      check(c.luck==0&&c.intelligence==0&&c.hiddenLevels[0]==0,"Children do not inherit parents' trained hidden points");
    }
    var s=new ChaoState();s.luckGrade=255;s.trainHidden(0,100,new Random(1));
    check(s.hiddenLevels[0]==1&&s.luck>=776&&s.luck<=780,"Byte255 uses source13+3grade±2 formula");
    s.trainHidden(0,500,new Random(2));check(s.luck==4000&&!s.canTrainHidden(0),"Hidden points stop at4000 independently of level");
    s=new ChaoState();for(int i=0;i<5;i++)s.giveAnimal(i);
    check(s.hiddenLevels[0]==1&&s.hiddenLevels[1]==1,"Five animals fill each hidden level bar");
    s=new ChaoState();s.giveDrive(0);s.giveDrive(0);s.giveDrive(0);
    check(s.hiddenLevels[0]==1&&s.hiddenProgress[0]==20&&s.hiddenProgress[1]==60,"Drive trains40Luck/20Intelligence, not directpointincrements");
  }
  private static void hatching(){
    var s=ChaoState.random(new Random(9));s.kindnessGenes=new int[]{0,4};s.aggressiveGenes=new int[]{0,4};
    int[] sliders=ChaoPersonality.sliders(s);int happy=s.happiness;s.applyHatchingMethod(true);
    check(Arrays.equals(s.kindnessGenes,new int[]{1,4})&&Arrays.equals(s.aggressiveGenes,new int[]{0,3}),"Gentle hatching alters both alleles with0..4bounds");
    check(Arrays.equals(sliders,ChaoPersonality.sliders(s))&&s.happiness==happy,"Hatching does not reroll fixed phenotype or change happiness");
    s.applyHatchingMethod(false);
    check(Arrays.equals(s.kindnessGenes,new int[]{0,3})&&Arrays.equals(s.aggressiveGenes,new int[]{1,4}),"Thrown hatching applies the opposite genotype shift");
    check(s.encode().equals(ChaoState.decode(s.encode()).encode()),"Hatch-adjusted hidden personality alleles persist");
  }
  private static void emotionsAndIllness(){
    var s=new ChaoState();s.hunger=37;s.energy=61;s.boredom=42;
    check(s.emotion(ChaoEmotion.HUNGRY)==6300&&s.emotion(ChaoEmotion.ENERGY)==6100&&s.emotion(ChaoEmotion.BOREDOM)==4200,"Legacy fullness is correctly inverted into source hunger scale");
    s.setEmotion(ChaoEmotion.HUNGRY,5101);check(s.hunger==49&&s.emotion(ChaoEmotion.HUNGRY)==5101,"Emotion meter fractions survive the integer HUD mirror");
    s.onHurt();check(s.emotion(ChaoEmotion.ANGER)>0&&s.emotion(ChaoEmotion.CRY)>0&&s.emotion(ChaoEmotion.FEAR)>0,"Abuse causes observable emotion pressures");
    check(s.happiness==15&&s.bond==-5,"Abuse core hook applies happiness and signed bond costs exactly once");
    int fear=s.emotion(ChaoEmotion.FEAR);s.pet(0);check(s.emotion(ChaoEmotion.RELAX)>0&&s.emotion(ChaoEmotion.FEAR)<fear,"Petting calms fear and increases relaxation");
    for(int i=0;i<6;i++)s.setIllness(i,-100);
    check(s.isIll()&&s.cureAllIllnesses()==6&&!s.isIll(),"Doctor cures every active illness once");
    for(int i=0;i<6;i++)check(s.illness(i)==100&&s.illnessImmunityTicks(i)==24000&&!s.riskIllness(i,1,new Random(1)),"Treatment produces persistent immunity rather than immediate repeated sickness");
    check(s.encode().equals(ChaoState.decode(s.encode()).encode()),"Emotion fractions and every illness/immunity meter persist");
    var calm=new ChaoState();var carefree=new ChaoState();carefree.setPersonalitySlider(ChaoPersonality.CAREFREE,80);
    var naive=new ChaoState();naive.setPersonalitySlider(ChaoPersonality.NAIVE,-80);
    for(int i=0;i<400;i++){calm.tick(1000000,1000000,new Random(1));carefree.tick(1000000,1000000,new Random(1));naive.tick(1000000,1000000,new Random(1));}
    int calmHappy=calm.happiness,carefreeHappy=carefree.happiness;calm.onHurt();carefree.onHurt();
    check(calmHappy-calm.happiness>carefreeHappy-carefree.happiness,"Carefree loses liking more slowly after abuse");
    check(carefree.emotion(ChaoEmotion.BOREDOM)<calm.emotion(ChaoEmotion.BOREDOM),"Carefree actually accumulates boredom more slowly");
    check(naive.emotion(ChaoEmotion.SLEEPINESS)>calm.emotion(ChaoEmotion.SLEEPINESS),"Naive actually becomes sleepy sooner");
    calm.sleeping=naive.sleeping=true;calm.setEmotion(ChaoEmotion.SLEEPINESS,9000);naive.setEmotion(ChaoEmotion.SLEEPINESS,9000);
    for(int i=0;i<400;i++){calm.tick(1000000,1000000,new Random(1));naive.tick(1000000,1000000,new Random(1));}
    check(naive.emotion(ChaoEmotion.SLEEPINESS)>calm.emotion(ChaoEmotion.SLEEPINESS),"Naive needs a longer nap to recover");
    var resting=new ChaoState();var moving=new ChaoState();resting.ageTicks=moving.ageTicks=20;
    resting.setEmotion(ChaoEmotion.NOURISH,8000);moving.setEmotion(ChaoEmotion.NOURISH,8000);
    resting.onGardenMotion(false);moving.onGardenMotion(true);
    check(resting.emotion(ChaoEmotion.CONDITION)>moving.emotion(ChaoEmotion.CONDITION),"Condition grows only in the source observed stationary context");
    resting.setEmotion(ChaoEmotion.NOURISH,1000);resting.onGardenMotion(false);
    check(resting.emotion(ChaoEmotion.CONDITION)==0,"Condition rapidly falls late in Nourish decay");
    s=new ChaoState();s.adult=true;s.primaryType=ChaoState.SWIM_TYPE;check(!s.riskIllness(ChaoIllness.COLD,1,new Random(1)),"Swim type resists cold");
    s.primaryType=ChaoState.POWER_TYPE;check(!s.riskIllness(ChaoIllness.STOMACH_ACHE,1,new Random(1)),"Power type resists stomach ache");
  }
  private static void rebirth(){
    int retained=0,removed=0;
    for(int seed=0;seed<100;seed++){
      var s=new ChaoState();s.entropy=seed;Arrays.fill(s.animalParts,15);s.animalMask=ChaoState.ALL_ANIMALS;s.hatsUnlocked=s.flameOrb=s.ghostBody=true;
      s.luck=3999;s.intelligence=1999;s.hiddenLevels=new int[]{99,99};s.setTrait("learned.guitar",1);s.setTrait("reward.hero",1);s.setTrait("lesson.active","guitar");s.setTrait("birthday.year",3);s.setTrait("birthday.party",1);
      s.reincarnate();for(int part:s.animalParts){if(part==15)retained++;else if(part==-1)removed++;else check(false,"No invented part on rebirth");}
      check(s.animalMask==0&&s.hatsUnlocked&&s.flameOrb&&s.ghostBody,"Current-life animal checklist resets while earned unlocks/special appearance remain");
      check(s.luck==399&&s.intelligence==199&&s.hiddenLevels[0]==1,"Hidden points and levels follow rebirth retention rule");
      check(s.intTrait("learned.guitar")==1&&s.intTrait("reward.hero")==1&&s.trait("lesson.active").isEmpty()&&s.trait("birthday.year").isEmpty()&&s.trait("birthday.party").isEmpty(),"Learned lessons/rewards survive; unfinished lesson cancels");
    }
    check(retained>450&&removed>150&&retained+removed==800,"Rebirth retains a real random subset instead of always all or none");
    var s=new ChaoState();s.reincarnations=2;s.happiness=90;s.animalMask=Integer.MAX_VALUE;s.ageTicks=19;s.tick(20,100,new Random(1));
    check(s.immortal,"Extra CWE animal bits cannot block the original21-gene Chaos requirement");
    s.cocoonTicks=0;s.giveAnimal(21,true);check(s.animalPartsMask!=0&&s.allowsAnimalParts()&&s.canBreed(true)&&!s.canBreed(),"Expanded rare actions are explicitly opt-in and persistable");
    check(s.encode().equals(ChaoState.decode(s.encode()).encode()),"Opt-in rare parts survive serialization");
  }
  private static void sourceAnimalsAndEggBirth(){
    int[][] source={{-4,-12,32,16},{-8,-16,20,36},{-8,-8,40,8},{20,16,4,8},{8,-16,4,36},{8,8,16,16},{20,60,-24,16}};
    int[] masks={115,83,83,83,87,81,208};
    for(int i=0;i<source.length;i++){
      check(Arrays.equals(ChaoState.animalTraining(24+i),source[i]),"SADX sample trains the source Modern small-animal values");
      check(ChaoState.animalSlots(24+i)==masks[i],"SADX sample exposes only source animal-part slots");
    }
    var a=new ChaoState();var b=new ChaoState();a.adult=b.adult=true;
    var egg=ChaoState.breed(a,b,new Random(2){@Override public int nextInt(int bound){return bound==100?0:super.nextInt(bound);}});
    check(egg.character.equals("normal")&&ChaoState.decode(egg.encode()).character.equals("normal"),"Retired Egg character is never selected at birth or on loading");
    egg.adult=true;check(egg.canBreed(),"Ordinary offspring retains the normal breeding lifecycle");
  }
  private static void genealogy(){
    var a=ChaoState.random(new Random(10));var b=ChaoState.random(new Random(11));a.adult=b.adult=true;
    String stable=a.lineageId();a.giveDrive(0);a.giveAnimal(3);
    check(a.lineageId().equals(stable)&&ChaoState.decode(a.encode()).lineageId().equals(stable),"Training entropy changes never alter persistent ancestry identity");
    a.reincarnate();a.adult=true;a.cocoonTicks=0;
    check(a.lineageId().equals(stable),"Reincarnation retains the same lineage identity despite random personality reset");
    a.luckGenes=new int[]{0,255};b.intelligenceGenes=new int[]{255,0};a.points[0]=9999;a.animalParts[0]=15;
    var child=ChaoState.breed(a,b,new Random(12));
    check(child.trait("family.parent.0.id").equals(a.lineageId())&&child.trait("family.parent.1.dna").equals(b.dnaSummary()),"Offspring records stable parent IDs and actual genotype summaries");
    check(child.points[0]==0&&child.animalParts[0]==-1,"Family ledger never transmits acquired training or parts");
    var other=ChaoState.random(new Random(13));other.adult=child.adult=true;
    var grandchild=ChaoState.breed(child,other,new Random(14));
    check(grandchild.trait("family.grandparent.0.id").equals(a.lineageId())&&grandchild.trait("family.grandparent.1.dna").equals(b.dnaSummary()),"Second generation retains four bounded grandparent positions without recursion");
    check(grandchild.trait("family.grandparent.2.id").isEmpty()&&grandchild.traitEntries().size()==8,"Unknown relatives remain unknown instead of inventing an ancestor");
    check(grandchild.encode().equals(ChaoState.decode(grandchild.encode()).encode()),"Two-generation ancestry survives saves without full state copies");
    var saturated=ChaoState.random(new Random(99));String fullIdentity=saturated.lineageId();
    for(int i=0;i<256;i++)saturated.setTrait("record.f"+i,i);
    saturated.giveDrive(0);check(ChaoState.decode(saturated.encode()).lineageId().equals(fullIdentity),"Identity persists independently of saturated extension traits");
    String[] older=saturated.encode().split(";",-1);String[] extra=older[21].split(",",-1);
    older[21]=String.join(",",Arrays.copyOf(extra,17));
    var migrated=ChaoState.decode(String.join(";",older));String migratedIdentity=migrated.lineageId();migrated.giveDrive(1);
    check(migrated.lineageId().equals(migratedIdentity)&&ChaoState.decode(migrated.encode()).lineageId().equals(migratedIdentity),"Early schema5 records acquire a dedicated identity once without consuming a trait slot");
    older[21]=String.join(",",Arrays.copyOf(extra,17))+",-2147483648,-2147483648";
    var signed=ChaoState.decode(String.join(";",older));
    check(signed.lineageId().equals("8000000080000000")&&ChaoState.decode(signed.encode()).lineageId().equals(signed.lineageId()),"Identity serialization supports full signed32-bit halves without clamping");
    a.colorGenes=new int[]{15,15};a.jewelGenes=new int[]{13,13};a.luckGenes=a.intelligenceGenes=new int[]{255,255};Arrays.fill(a.personalityGenesA,4);Arrays.fill(a.personalityGenesB,4);
    check(a.dnaSummary().length()<=128,"Maximum supported genotype fits one bounded trait value");
    check(ChaoState.describeDnaSummary(a.dnaSummary()).size()==9&&ChaoState.describeDnaSummary(a.dnaSummary()).get(0).startsWith("Colour alleles:"),"Family display explains all nine compact genetic fields");
  }
  private static void monsterEvolution(){
    var s=new ChaoState();s.adult=true;s.primaryType=ChaoState.FLY_TYPE;s.points[ChaoState.FLY]=1999;s.swimFly=1;
    s.refreshMonsterEvolution(true,false);s.influence(ChaoState.FLY,.5f,true);
    check(s.swimFly==1&&s.monsterStage()==0,"Monster influence cannot exceed the normal cap below2000 points");
    s.points[ChaoState.FLY]=2000;check(s.monsterStage()==0,"2000 points alone do not turn an exact1.0 influence into a monster");
    s.giveDrive(ChaoState.FLY,true);
    check(s.swimFly==1&&s.optionalMonsterSwimFly()>1&&s.monsterStage()==1&&s.monsterType()==ChaoState.FLY_TYPE,"Optional development is separate from the bounded ordinary axis");
    check(s.monsterAlpha()<1,"Ordinary-color typed monsters express source transparency");
    s.points[ChaoState.FLY]=3000;check(s.monsterStage()==2&&s.monsterAlpha()<.7f,"3000 points advances the same developed type to stage2");
    String saved=s.encode();var restored=ChaoState.decode(saved);
    check(restored.encode().equals(saved)&&restored.swimFly==1&&restored.optionalMonsterSwimFly()>1&&restored.monsterStage()==2,"Separate extended influence, stage options and opacity survive serialization");
    s.refreshMonsterEvolution(true,true);check(s.monsterAlpha()==1,"Optional transparency fix restores full opacity without removing development");
    s.expressedColor=3;s.refreshMonsterEvolution(true,false);check(s.monsterAlpha()==1,"Colored monsters stay opaque under the source restriction");
    s.expressedColor=0;s.expressedJewel=1;check(s.monsterAlpha()==1,"Jewel monsters stay opaque under the source restriction");
    s.refreshMonsterEvolution(false,false);check(s.monsterStage()==0&&s.monsterAlpha()==1,"Disabling the optional code disables all monster rendering");
    s.swimFly=1;s.points[ChaoState.FLY]=3000;s.giveDrive(ChaoState.FLY);
    check(s.swimFly==1&&s.monsterStage()==0,"The existing default care API retains original influence caps");
    Arrays.fill(s.levels,99);s.luck=s.intelligence=4000;s.hiddenLevels=new int[]{99,99};s.happiness=100;
    check(!s.hasDriveBenefit(ChaoState.FLY)&&s.hasDriveBenefit(ChaoState.FLY,true),"Max-level Chao still accept useful drives only when optional extra development is available");
    s.refreshMonsterEvolution(true,false);s.influence(ChaoState.FLY,100,true);
    check(s.swimFly==1&&s.optionalMonsterSwimFly()==3&&!s.hasDriveBenefit(ChaoState.FLY,true),"Only optional extension reaches3; ordinary axis stays1 and redundant maxed drives stop");
    var balanced=new ChaoState();balanced.adult=true;balanced.points[ChaoState.STAMINA]=2000;balanced.refreshMonsterEvolution(true,false);
    check(balanced.monsterStage()==1&&balanced.monsterType()==ChaoState.NORMAL&&balanced.monsterAlpha()==1,"Normal development uses the documented high-Stamina projection and remains opaque");
    balanced.points[ChaoState.STAMINA]=3000;check(balanced.monsterStage()==2&&balanced.monsterInfluence()>2,"Normal development has a distinct second Stamina stage");
    balanced.reincarnate();check(balanced.monsterStage()==0&&Math.abs(balanced.swimFly)<=1,"Rebirth returns optional development to ordinary child growth");
  }
  private static void newbornEyes(){
    var normal=ChaoState.random(new Random(17));var disabled=ChaoState.random(new Random(17),0);
    check(normal.encode().equals(disabled.encode()),"Disabled eye code consumes no extra randomness and preserves ordinary births");
    boolean[] seen=new boolean[9];var a=new ChaoState();var b=new ChaoState();a.adult=b.adult=true;
    for(int seed=0;seed<256;seed++){
      var newborn=ChaoState.egg(2,false,false,0,new Random(seed),100);
      check(newborn.eyeColor>=0&&newborn.eyeColor<=8,"Enabled newborn eye code cannot generate special lens-only indices9..11");
      seen[newborn.eyeColor]=true;int eye=newborn.eyeColor;newborn.giveDrive(0);
      check(ChaoState.decode(newborn.encode()).eyeColor==eye,"Birth iris choice is saved rather than rerolled by care or reload");
      var child=ChaoState.breed(a,b,new Random(seed),false,100);
      check(child.eyeColor>=0&&child.eyeColor<=8,"The actual bred-birth overload obeys the same ordinary-color range");
    }
    for(boolean present:seen)check(present,"All nine permitted ordinary iris choices are reachable");
    var lens=new ChaoState();lens.eyeColor=11;lens.reincarnate();
    check(lens.eyeColor==11,"Rebirth retains a previously applied special lens instead of performing a newborn reroll");
  }
  private static void immortalAdultCalendar(){
    int year=24000;var s=new ChaoState();s.adult=s.immortal=true;s.ageTicks=216000;s.adultAgeTicks=year-2;
    check(s.tick(20,20,new Random(1))==ChaoState.Transition.NONE&&s.adultAgeTicks==year-1&&s.adultAgeTicks/year==0,"Immortal adult calendar reaches the tick before birthday without aging out or using its child age");
    s=ChaoState.decode(s.encode());
    check(s.adultAgeTicks==year-1&&s.immortal&&!s.dead,"Reload preserves the immortal adult's pending birthday boundary");
    check(s.tick(20,20,new Random(1))==ChaoState.Transition.NONE&&s.adultAgeTicks==year&&s.adultAgeTicks/year==1&&s.immortal&&!s.dead,"Immortal calendar crosses the birthday boundary while suppressing lifespan transitions");
    s.adultAgeTicks=Integer.MAX_VALUE-1;s.tick(20,20,new Random(1));
    check(s.adultAgeTicks==Integer.MAX_VALUE-1,"Immortal calendar saturates without integer wraparound");
    var evolved=new ChaoState();evolved.ageTicks=19;evolved.reincarnations=2;evolved.animalMask=ChaoState.ALL_ANIMALS;evolved.happiness=90;
    check(evolved.tick(20,20,new Random(1))==ChaoState.Transition.EVOLVED&&evolved.immortal&&evolved.adultAgeTicks==0,"Newly evolved Chaos starts its adult birthday calendar at zero");
    for(int i=0;i<200;i++)evolved.tick(20,20,new Random(1));
    evolved.tick(20,20,new Random(1));
    check(evolved.adultAgeTicks==1&&evolved.adultAgeTicks/year==0,"Evolution cocoon and prior child lifetime cannot create an immediate Chaos birthday");
  }
  private static void traitsAndExtras(){
    var s=new ChaoState();for(int i=0;i<256;i++)check(s.setTraitInt("record.v"+i,i),"All256 small working trait slots fit");
    check(!s.setTrait("overflow",1)&&s.intTrait("record.v255")==255,"Capacity rejects explicitly without evicting prior state");
    check(s.setTrait("record.v0",99)&&!s.setTrait("bad;key","x")&&!s.setTrait("abc","x".repeat(129)),"Existing updates work at capacity; malformed/oversized traits refuse");
    check(s.encode().length()<32767&&s.encode().equals(ChaoState.decode(s.encode()).encode()),"Trait store remains below entity-string network bound and roundtrips");
    s=new ChaoState();s.eyeColor=11;s.character="omochao";s.cosmeticWings=s.negativePalette=s.brightCoat=true;s.heroBrushType=4;
    check(s.encode().equals(ChaoState.decode(s.encode()).encode()),"CWE appearance fields persist independently of mod theme");
    s=new ChaoState();s.levels[0]=99;s.grades[0]=s.gradeA[0]=0;s.gradeB[0]=4;
    check(s.gradeUpgradeCostRings(0)==10000&&s.upgradeGrade(0)&&s.grades[0]==1&&s.gradeB[0]==4&&s.levels[0]==0,"Doctor upgrade changes only expressed allele and resets stat for full growth");
    s.grades[1]=s.gradeA[1]=5;check(s.useHyperFruit(1)&&s.grades[1]==6&&s.gradeA[1]==5,"Hyper temporarily boosts phenotype while hereditary grade remains S");
    s.grades[2]=5;check(!s.useHyperFruit(2),"Only one X grade per life");s.reincarnate();check(s.grades[1]==5&&s.gradeA[1]==5,"X resets to S on rebirth");
  }
  public static void main(String[] args){System.out.println("Chao life019: "+run()+" checks passed");}
}
