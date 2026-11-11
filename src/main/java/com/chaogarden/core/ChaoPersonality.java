package com.chaogarden.core;

import java.util.*;
import java.util.random.RandomGenerator;

/** Thirteen inherited sliders. Uncertain original label projections are explicit Minecraft rules. */
public final class ChaoPersonality {
  public static final int CURIOUS=0, ENERGETIC=1, NAIVE=2, BIG_EATER=3, CAREFREE=4,
      KINDNESS=5, SOLITUDE=6, VITALITY=7, REGAIN=8, SKILLFUL=9, CHARM=10, CHATTY=11, FICKLE=12;
  public static final int COUNT=13;
  public static final String[] NAMES={"Normal / Curious","Cry baby / Energetic","Naive / Normal",
      "Normal / Big eater","Normal / Carefree","Kindness","Solitude","Vitality","Regain",
      "Skillful","Charm","Chatty","Fickle"};
  private ChaoPersonality() {}
  public static int get(ChaoState s,int index){
    return clamp(switch(index){case CURIOUS->s.curiosityTendency;case ENERGETIC->s.energeticTendency;
      case NAIVE->s.naiveTendency;case CAREFREE->s.carefreeTendency;
      default->index>=0&&index<COUNT?s.personalitySliders[index]:0;});
  }
  public static void set(ChaoState s,int index,int value){
    if(index<0||index>=COUNT)return;value=clamp(value);s.personalitySliders[index]=value;
    switch(index){case CURIOUS->s.curiosityTendency=value;case ENERGETIC->s.energeticTendency=value;
      case NAIVE->s.naiveTendency=value;case CAREFREE->s.carefreeTendency=value;default->{}}
    s.personalityProfileInitialized=true;
  }
  public static void initialize(ChaoState s,RandomGenerator r){
    s.legacyProfileLabel=-1;
    for(int i=0;i<COUNT;i++){int allele=r.nextInt(5);s.personalityGenesA[i]=s.personalityGenesB[i]=allele;set(s,i,(allele-2)*40);}
    s.syncPersonalityGeneAliases();refreshLegacy(s);
  }
  public static void inherit(ChaoState child,ChaoState a,ChaoState b,RandomGenerator r){
    a.syncPersonalityGeneAliases();b.syncPersonalityGeneAliases();
    for(int i=0;i<COUNT;i++){
      int first=r.nextBoolean()?a.personalityGenesA[i]:a.personalityGenesB[i];
      int second=r.nextBoolean()?b.personalityGenesA[i]:b.personalityGenesB[i];
      child.personalityGenesA[i]=first;child.personalityGenesB[i]=second;
      set(child,i,((r.nextBoolean()?first:second)-2)*40);
    }child.syncPersonalityGeneAliases();refreshLegacy(child);
  }
  public static void migrateLegacy(ChaoState s){
    int old=s.personality;
    // Old four tendencies remain exact, including fruit-induced non-40 values.
    int[] first={s.curiosityTendency,s.energeticTendency,s.naiveTendency,s.carefreeTendency};
    switch(old){case ChaoPreferences.GENTLE->set(s,KINDNESS,65);case ChaoPreferences.NAUGHTY->set(s,KINDNESS,-65);
      case ChaoPreferences.BIG_EATER->set(s,BIG_EATER,65);case ChaoPreferences.CHATTY->set(s,CHATTY,65);
      case ChaoPreferences.EASILY_BORED->set(s,CURIOUS,-65);case ChaoPreferences.QUIET->set(s,VITALITY,-65);
      case ChaoPreferences.CARELESS->set(s,SKILLFUL,-65);case ChaoPreferences.SMART->set(s,SKILLFUL,65);
      case ChaoPreferences.LONELY->set(s,SOLITUDE,-65);default->{}}
    set(s,CURIOUS,first[0]);set(s,ENERGETIC,first[1]);set(s,NAIVE,first[2]);set(s,CAREFREE,first[3]);
    for(int i=0;i<COUNT;i++){int gene=Math.max(0,Math.min(4,Math.round(get(s,i)/40f)+2));s.personalityGenesA[i]=s.personalityGenesB[i]=gene;}
    s.personality=old;s.legacyProfileLabel=old;
    s.syncPersonalityGeneAliases();
  }
  public static int[] profile(ChaoState s){
    if(!s.personalityProfileInitialized)return new int[]{Math.max(0,Math.min(14,s.personality))};
    var candidates=new ArrayList<int[]>();
    if(s.legacyProfileLabel>=0&&s.legacyProfileLabel<ChaoPreferences.PERSONALITIES.length)
      candidates.add(new int[]{s.legacyProfileLabel,1000});
    add(candidates,s,CURIOUS,ChaoPreferences.EASILY_BORED,ChaoPreferences.CURIOUS);
    add(candidates,s,ENERGETIC,ChaoPreferences.CRY_BABY,ChaoPreferences.ENERGETIC);
    add(candidates,s,NAIVE,ChaoPreferences.NAIVE,-1);
    add(candidates,s,BIG_EATER,-1,ChaoPreferences.BIG_EATER);
    add(candidates,s,CAREFREE,-1,ChaoPreferences.CAREFREE);
    add(candidates,s,KINDNESS,ChaoPreferences.NAUGHTY,ChaoPreferences.GENTLE);
    add(candidates,s,SOLITUDE,ChaoPreferences.LONELY,-1);
    add(candidates,s,VITALITY,ChaoPreferences.QUIET,-1);
    add(candidates,s,SKILLFUL,ChaoPreferences.CARELESS,ChaoPreferences.SMART);
    add(candidates,s,CHATTY,-1,ChaoPreferences.CHATTY);
    candidates.sort(Comparator.<int[]>comparingInt(a->-a[1]).thenComparingInt(a->a[0]));
    return candidates.isEmpty()?new int[]{ChaoPreferences.NONE}:candidates.stream().mapToInt(a->a[0]).distinct().limit(3).toArray();
  }
  private static void add(List<int[]> result,ChaoState s,int index,int low,int high){
    int value=get(s,index),label=value>54?high:value< -54?low:-1;
    if(label>=0)result.add(new int[]{label,Math.abs(value)});
  }
  public static boolean has(ChaoState s,int label){
    if(!s.personalityProfileInitialized)return s.personality==label;
    if(s.legacyProfileLabel==label)return true;
    return switch(label){
      case ChaoPreferences.CURIOUS->get(s,CURIOUS)>54;
      case ChaoPreferences.EASILY_BORED->get(s,CURIOUS)< -54;
      case ChaoPreferences.ENERGETIC->get(s,ENERGETIC)>54;
      case ChaoPreferences.CRY_BABY->get(s,ENERGETIC)< -54;
      case ChaoPreferences.NAIVE->get(s,NAIVE)< -54;
      case ChaoPreferences.BIG_EATER->get(s,BIG_EATER)>54;
      case ChaoPreferences.CAREFREE->get(s,CAREFREE)>54;
      case ChaoPreferences.GENTLE->get(s,KINDNESS)>54;
      case ChaoPreferences.NAUGHTY->get(s,KINDNESS)< -54;
      case ChaoPreferences.LONELY->get(s,SOLITUDE)< -54;
      case ChaoPreferences.QUIET->get(s,VITALITY)< -54;
      case ChaoPreferences.SMART->get(s,SKILLFUL)>54;
      case ChaoPreferences.CARELESS->get(s,SKILLFUL)< -54;
      case ChaoPreferences.CHATTY->get(s,CHATTY)>54;
      default->profile(s)[0]==ChaoPreferences.NONE;
    };
  }
  public static String doctor(ChaoState s,long visit){int[] p=profile(s);return ChaoPreferences.PERSONALITIES[p[Math.floorMod(visit,p.length)]];}
  public static void refreshLegacy(ChaoState s){s.personality=profile(s)[0];}
  public static void applyHatchingMethod(ChaoState s,boolean gentle){
    s.syncPersonalityGeneAliases();int change=gentle?1:-1;
    for(int i:new int[]{KINDNESS,ENERGETIC}){
      int d=i==KINDNESS?change:-change;
      s.personalityGenesA[i]=Math.max(0,Math.min(4,s.personalityGenesA[i]+d));
      s.personalityGenesB[i]=Math.max(0,Math.min(4,s.personalityGenesB[i]+d));
    }s.syncPersonalityGeneAliases();
  }
  public static int[] sliders(ChaoState s){int[] a=new int[COUNT];for(int i=0;i<COUNT;i++)a[i]=get(s,i);return a;}
  private static int clamp(int v){return Math.max(-100,Math.min(100,v));}
}
