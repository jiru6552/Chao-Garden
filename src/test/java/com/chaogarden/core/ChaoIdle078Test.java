package com.chaogarden.core;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

/** Quiet gestures reflect saved temperament without changing care or inventing fatigue. */
public final class ChaoIdle078Test {
  private static int checks;

  private static void check(boolean value,String message) {
    checks++;
    if(!value)throw new AssertionError(message);
  }

  public static int run() {
    checks=0;
    catalogueAndClock();
    urgentNeedsInterrupt();
    fatigueAndJoyGates();
    variedSelection();
    reloadAndReadOnlySelection();
    return checks;
  }

  private static ChaoState quiet() {
    var state=new ChaoState();
    state.hunger=80;state.energy=100;state.happiness=20;
    return state;
  }

  private static void catalogueAndClock() {
    var ids=List.of("look_around","look_up","think","bashful","chuckle","tired","ground_play");
    check(ChaoIdleRules078.IDS.equals(ids),"The operator and production idle catalogue use the seven stable IDs");
    int[] durations={72,64,80,68,68,84,104};
    for(int i=0;i<ids.size();i++) {
      String id=ids.get(i);int duration=durations[i];
      check(ChaoIdleRules078.valid(id)&&ChaoIdleRules078.duration(id)==duration,"Literal production duration for "+id);
      check(ChaoIdleRules078.seated(id)==id.equals("ground_play"),"Only ground play requires a seated gesture");
      for(float elapsed:new float[]{-1,0,duration,duration+1,Float.NaN,Float.NEGATIVE_INFINITY,Float.POSITIVE_INFINITY})
        check(ChaoIdleRules078.weight(id,elapsed)==0,"Outside or at clip endpoints returns the calm pose for "+id);
      float peak=0;
      for(int step=0;step<=duration*10;step++) {
        float elapsed=step/10f,weight=ChaoIdleRules078.weight(id,elapsed);
        check(Float.isFinite(weight)&&weight>=0&&weight<=1,"Bounded finite clip envelope for "+id);
        peak=Math.max(peak,weight);
      }
      check(peak>.99f,"Gesture reaches its authored pose for "+id);
      check(ChaoIdleRules078.weight(id,.01f)<.0001f
          &&ChaoIdleRules078.weight(id,duration-.01f)<.0001f,"Both calm transitions have a smooth endpoint for "+id);
    }
    for(String invalid:new String[]{"","idle_look_up","scratch","LOOK_UP","unknown"}) {
      check(!ChaoIdleRules078.valid(invalid)&&!ChaoIdleRules078.seated(invalid),"Unknown IDs cannot reserve an idle posture");
      check(ChaoIdleRules078.weight(invalid,30)==0,"Unknown IDs never create a pose");
    }
    check(!ChaoIdleRules078.valid(null)&&!ChaoIdleRules078.seated(null)
        &&ChaoIdleRules078.weight(null,30)==0,"Missing idle detail safely means calm");
    var gaps=new HashSet<Integer>();var random=new Random(78);
    for(int i=0;i<2000;i++) {
      int gap=ChaoIdleRules078.nextGap(random);gaps.add(gap);
      check(gap>=400&&gap<=900,"Quiet interval stays between twenty and forty-five seconds");
    }
    check(gaps.size()>100,"Individuals receive varied quiet intervals");
  }

  private static void blocked(ChaoState state,String reason) {
    check(!ChaoIdleRules078.eligible(state),reason+" interrupts quiet gestures");
    for(String id:ChaoIdleRules078.IDS)
      check(!ChaoIdleRules078.allowed(state,id,true),reason+" excludes every natural idle clip");
    check(ChaoIdleRules078.choose(state,new Random(78),true).isEmpty(),reason+" cannot choose an idle");
  }

  private static void urgentNeedsInterrupt() {
    check(ChaoIdleRules078.eligible(quiet()),"A healthy quiet Chao can gesture");
    var dead=quiet();dead.dead=true;blocked(dead,"Death");
    var sleeping=quiet();sleeping.sleeping=true;blocked(sleeping,"Actual sleep");
    var cocoon=quiet();cocoon.cocoonTicks=1;blocked(cocoon,"Lifecycle cocoon");
    var hungry=quiet();hungry.hunger=34;blocked(hungry,"Orchard hunger");
    var exhausted=quiet();exhausted.energy=14;blocked(exhausted,"Exhaustion");
    var ill=quiet();ill.setIllness(ChaoIllness.RASH,-1);blocked(ill,"Genuine rash");
    check(ill.illness(ChaoIllness.RASH)==-1,"Suppressing an idle preserves the actual rash symptom");
    record Boundary(int emotion,int below,int threshold) {}
    Boundary[] boundaries={
        new Boundary(ChaoEmotion.FEAR,114,115),new Boundary(ChaoEmotion.CRY,99,100),
        new Boundary(ChaoEmotion.ANGER,104,105),new Boundary(ChaoEmotion.SURPRISE,120,121),
        new Boundary(ChaoEmotion.TIREDNESS,6000,6001),new Boundary(ChaoEmotion.JOY,115,116),
        new Boundary(ChaoEmotion.RELAX,69,70),new Boundary(ChaoEmotion.SLEEPINESS,7999,8000),
        new Boundary(ChaoEmotion.TIRE,8999,9000)};
    for(var boundary:boundaries) {
      var state=quiet();state.setEmotion(boundary.emotion(),boundary.below());
      check(ChaoIdleRules078.eligible(state),"Below urgent threshold remains available: "+boundary.emotion());
      state.setEmotion(boundary.emotion(),boundary.threshold());
      blocked(state,"Urgent emotion "+boundary.emotion());
    }
    var edge=quiet();edge.hunger=35;edge.energy=15;
    check(ChaoIdleRules078.eligible(edge),"Only an actual urgent need stops the boundary state");
  }

  private static void fatigueAndJoyGates() {
    var healthy=quiet();
    for(int slider=0;slider<ChaoPersonality.COUNT;slider++)healthy.setPersonalitySlider(slider,-100);
    check(!ChaoIdleRules078.allowed(healthy,"tired",false),"A quiet temperament alone never invents fatigue");
    check(!ChaoIdleRules078.allowed(healthy,"chuckle",false),"A quiet ordinary mood alone never invents laughter");
    record Fatigue(int emotion,int below,int threshold) {}
    for(var fatigue:new Fatigue[]{new Fatigue(ChaoEmotion.SLEEPINESS,4999,5000),
        new Fatigue(ChaoEmotion.TIREDNESS,2999,3000),new Fatigue(ChaoEmotion.TIRE,4999,5000)}) {
      var state=quiet();state.setEmotion(fatigue.emotion(),fatigue.below());
      check(!ChaoIdleRules078.allowed(state,"tired",false),"No tired clip below actual fatigue threshold");
      state.setEmotion(fatigue.emotion(),fatigue.threshold());
      check(ChaoIdleRules078.allowed(state,"tired",false),"Saved fatigue permits the tired gesture");
    }
    var lowEnergy=quiet();lowEnergy.energy=36;
    check(!ChaoIdleRules078.allowed(lowEnergy,"tired",false),"Energy thirty-six is above the fatigue gate");
    lowEnergy.energy=35;
    check(ChaoIdleRules078.allowed(lowEnergy,"tired",false),"Actual low energy permits the tired gesture");
    var joyful=quiet();joyful.setEmotion(ChaoEmotion.JOY,54);
    check(!ChaoIdleRules078.allowed(joyful,"chuckle",false),"Joy below fifty-five does not author laughter");
    joyful.setEmotion(ChaoEmotion.JOY,55);
    check(ChaoIdleRules078.allowed(joyful,"chuckle",false),"Genuine positive joy can author a chuckle");
    var happy=quiet();happy.happiness=44;
    check(!ChaoIdleRules078.allowed(happy,"chuckle",false),"Happiness below forty-five does not author laughter");
    happy.happiness=45;
    check(ChaoIdleRules078.allowed(happy,"chuckle",false),"A happy Chao may chuckle without a high joy pulse");
    for(var mood:new int[][]{{ChaoEmotion.ANGER,55},{ChaoEmotion.CRY,50},{ChaoEmotion.FEAR,60}}) {
      var mixed=quiet();mixed.happiness=100;mixed.setEmotion(mood[0],mood[1]);
      check(!ChaoIdleRules078.allowed(mixed,"chuckle",false),"Meaningful distress overrides a happy chuckle");
    }
    joyful.energy=35;
    check(!ChaoIdleRules078.allowed(joyful,"chuckle",false)
        &&ChaoIdleRules078.allowed(joyful,"tired",false),"Actual fatigue owns mixed joy and tiredness");
    check(!ChaoIdleRules078.allowed(healthy,"ground_play",false)
        &&ChaoIdleRules078.allowed(healthy,"ground_play",true),"Ground play follows the real seated posture");
    check(!ChaoIdleRules078.allowed(healthy,"scratch",true),"Illness scratching is never disguised as a decorative idle");
  }

  private static Set<String> sample(ChaoState state,boolean sitting,long seed) {
    var random=new Random(seed);Set<String> selected=new HashSet<>();
    for(int i=0;i<5000;i++) {
      String id=ChaoIdleRules078.choose(state,random,sitting);selected.add(id);
      check(ChaoIdleRules078.valid(id)&&ChaoIdleRules078.allowed(state,id,sitting),"Every sampled choice obeys production gates");
    }
    return selected;
  }

  private static void variedSelection() {
    check(sample(quiet(),false,78).equals(Set.of("look_around","look_up","think","bashful")),
        "Healthy standing Chao retain all four ordinary gestures without unsupported fatigue or laughter");
    check(sample(quiet(),true,78).equals(Set.of("look_around","look_up","think","bashful","ground_play")),
        "Seated quiet Chao add ground play without losing the ordinary mix");
    var happy=quiet();happy.happiness=80;
    check(sample(happy,true,79).contains("chuckle"),"An appropriately happy Chao can naturally choose a chuckle");
    var tired=quiet();tired.energy=35;
    check(sample(tired,true,80).contains("tired"),"Actual fatigue can naturally choose the tired clip");
    for(int direction:new int[]{-100,100}) {
      var extreme=quiet();extreme.happiness=80;
      for(int slider=0;slider<ChaoPersonality.COUNT;slider++)extreme.setPersonalitySlider(slider,direction);
      Set<String> selected=sample(extreme,true,81+direction);
      check(selected.size()>=5,"Even extreme temperaments keep a varied mix instead of one prescribed animation");
    }
    var curious=quiet();var incurious=quiet();
    curious.setPersonalitySlider(ChaoPersonality.CURIOUS,100);
    incurious.setPersonalitySlider(ChaoPersonality.CURIOUS,-100);
    int curiousLooks=0,incuriousLooks=0;var first=new Random(7820);var second=new Random(7820);
    for(int i=0;i<10000;i++) {
      String high=ChaoIdleRules078.choose(curious,first,false);
      String low=ChaoIdleRules078.choose(incurious,second,false);
      if(high.equals("look_around")||high.equals("look_up"))curiousLooks++;
      if(low.equals("look_around")||low.equals("look_up"))incuriousLooks++;
    }
    check(curiousLooks>incuriousLooks+700,"Saved curiosity measurably favors looking gestures while leaving random alternatives");
  }

  private static void reloadAndReadOnlySelection() {
    var state=ChaoState.random(new Random(78));state.happiness=80;state.hunger=75;state.energy=90;
    state.setEmotion(ChaoEmotion.JOY,70);state.setTrait("lesson.song","3");
    String saved=state.encode();var restored=ChaoState.decode(saved);
    var first=new Random(7800);var second=new Random(7800);
    for(int i=0;i<500;i++)
      check(ChaoIdleRules078.choose(state,first,true).equals(ChaoIdleRules078.choose(restored,second,true)),
          "Reload preserves the same saved temperament and mood selection distribution");
    check(state.encode().equals(saved)&&restored.encode().equals(saved),
        "Idle selection never changes age, care, lessons, rewards, genes or the saved state schema");
    var hero=ChaoState.decode(saved);var dark=ChaoState.decode(saved);hero.align=1;dark.align=-1;
    first=new Random(7810);second=new Random(7810);
    int[] heroPersonality=ChaoPersonality.sliders(hero),darkPersonality=ChaoPersonality.sliders(dark);
    for(int i=0;i<500;i++)
      check(ChaoIdleRules078.choose(hero,first,true).equals(ChaoIdleRules078.choose(dark,second,true)),
          "Alignment never substitutes a fixed personality gesture");
    check(Arrays.equals(heroPersonality,ChaoPersonality.sliders(hero))
        &&Arrays.equals(darkPersonality,ChaoPersonality.sliders(dark)),"Alignment and idle projection preserve expressed personality");
    var raw=quiet();raw.energy=82;raw.hunger=79;
    raw.emotions[ChaoEmotion.ENERGY]=8111;raw.emotions[ChaoEmotion.HUNGRY]=2178;
    int[] emotions=raw.emotions.clone(),genesA=raw.personalityGenesA.clone(),genesB=raw.personalityGenesB.clone();
    long entropy=raw.entropy;
    for(int i=0;i<50;i++) {
      ChaoIdleRules078.eligible(raw);
      for(String id:ChaoIdleRules078.IDS)ChaoIdleRules078.allowed(raw,id,true);
      ChaoIdleRules078.choose(raw,new Random(i),true);
    }
    check(Arrays.equals(raw.emotions,emotions)&&raw.energy==82&&raw.hunger==79,
        "Pure idle rules do not synchronize or round authoritative fractional emotion storage");
    check(Arrays.equals(raw.personalityGenesA,genesA)&&Arrays.equals(raw.personalityGenesB,genesB)&&raw.entropy==entropy,
        "Idle projection never advances genetics or saved simulation entropy");
  }

  public static void main(String[] args){System.out.println("Chao idle078: "+run()+" checks passed");}
}
