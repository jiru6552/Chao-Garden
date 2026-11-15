package com.chaogarden.core;

import java.util.List;

/** Pure presentation projection: fixed identity, temporary state, and independent face channels. */
public final class ChaoFaceRules076 {
  public enum Eyes { NORMAL, HALF_CLOSED, HAPPY_CLOSED, MEAN, CHAOS, DISTRESS, SLEEP, BLINK, SURPRISED }
  public enum Mouth { NONE, SMILE, FROWN, GRIN, WAVY, OPEN, FLAT }
  public enum Style { NEUTRAL, HERO, DARK, HERO_LENS, DARK_LENS, CHAOS_LENS, NATIVE }
  public enum Source { NATIVE, DAMAGE, DANGER, SLEEP, SYMPTOM, ILLNESS, ACTIVITY, CARE, BEHAVIOR, MOOD, BLINK, BASELINE, IDLE }
  public enum Symptom { NONE, COUGH, SNEEZE, SCRATCH, HICCUP, STOMACH_ACHE, RUNNY_NOSE }
  public static final List<String> FACE_PREVIEW_IDS=List.of("happy","distress","tired","sleep",
      "cough","cold","rash","runny_nose","hiccups","stomach_ache");

  /** Null actionMouth reserves neither channel; behaviorMouth uses the existing native0..6 IDs. */
  public record Signals(boolean nativeExcluded,boolean hurt,boolean danger,boolean sleeping,
      boolean groggy,Symptom symptom,boolean illness,boolean activityHappy,boolean affectionate,
      Mouth actionMouth,int behaviorMouth,boolean blink) {
    public Signals {
      symptom=symptom==null?Symptom.NONE:symptom;
      behaviorMouth=Math.max(-1,Math.min(6,behaviorMouth));
    }
    public static Signals quiet() {
      return new Signals(false,false,false,false,false,Symptom.NONE,false,false,false,null,-1,false);
    }
    public static Signals nativeFace() {
      return new Signals(true,false,false,false,false,Symptom.NONE,false,false,false,null,-1,false);
    }
  }

  public record Resolved(int baselineEyes,int baselineMouth,Eyes eyes,Mouth mouth,Style style,
      Source eyeSource,Source mouthSource,boolean nativeExcluded,int irisColor) {
    /** Compatibility observations used by older real-client animation probes. */
    public int legacyEyeExpression() {
      return switch(eyes) {case HAPPY_CLOSED->1;case DISTRESS->2;case SLEEP->3;case BLINK->4;default->0;};
    }
  }

  private ChaoFaceRules076() {}

  public static boolean isFacePreviewKind(String kind) {
    return kind!=null&&kind.startsWith("preview_face_")&&FACE_PREVIEW_IDS.contains(kind.substring(13));
  }

  /** The existing160-tick ToyPreview clock exercises these same production inputs. */
  public static Signals previewSignals(Signals natural,String kind,float elapsed) {
    if(!isFacePreviewKind(kind)||!Float.isFinite(elapsed)||elapsed<20||elapsed>=140)return natural;
    String id=kind.substring(13);
    Symptom symptom=switch(id) {
      case "cough"->Symptom.COUGH;case "cold"->Symptom.SNEEZE;case "rash"->Symptom.SCRATCH;
      case "runny_nose"->Symptom.RUNNY_NOSE;case "hiccups"->Symptom.HICCUP;
      case "stomach_ache"->Symptom.STOMACH_ACHE;default->natural.symptom();
    };
    return new Signals(natural.nativeExcluded(),natural.hurt(),natural.danger()||id.equals("distress"),
        natural.sleeping()||id.equals("sleep"),natural.groggy()||id.equals("tired"),symptom,
        natural.illness()||symptom!=Symptom.NONE,natural.activityHappy()||id.equals("happy"),
        natural.affectionate(),natural.actionMouth(),natural.behaviorMouth(),natural.blink());
  }

  /**
   * No random draws or gameplay writes. Public baseline getters perform only their existing
   * one-time legacy initialization. Emotion reads below avoid mutating legacy-meter adapters.
   * Thresholds are Minecraft presentation choices on the existing0..200 and0..10000 scales.
   */
  public static Resolved resolve(ChaoState state,Signals signal) {
    return resolve(state,signal,"",0);
  }

  /** Idle affect shares this resolver's existing higher-priority care/illness rules. */
  public static Resolved resolve(ChaoState state,Signals signal,String idle,float idleElapsed) {
    int baseEyes=state.baselineEyes(),baseMouth=state.baselineMouth();
    int iris=state.eyeColor>=0&&state.eyeColor<=11?state.eyeColor:0;
    Eyes eyes=baselineEyes(baseEyes);Mouth mouth=baselineMouth(baseMouth);
    boolean excluded=signal.nativeExcluded()||state.immortal||!"normal".equals(state.character);
    Style style=style(state,iris,excluded);
    Source eyeSource=Source.BASELINE,mouthSource=Source.BASELINE;
    if(excluded)return new Resolved(baseEyes,baseMouth,eyes,mouth,style,Source.NATIVE,Source.NATIVE,true,iris);
    if(signal.hurt()) {eyes=Eyes.DISTRESS;mouth=Mouth.FROWN;eyeSource=mouthSource=Source.DAMAGE;}
    else if(signal.danger()) {eyes=Eyes.DISTRESS;mouth=Mouth.OPEN;eyeSource=mouthSource=Source.DANGER;}
    else if(signal.sleeping()||state.sleeping) {eyes=Eyes.SLEEP;mouth=Mouth.NONE;eyeSource=mouthSource=Source.SLEEP;}
    else if(signal.symptom()!=Symptom.NONE) {
      boolean acute=signal.symptom()==Symptom.COUGH||signal.symptom()==Symptom.SNEEZE||signal.symptom()==Symptom.HICCUP;
      eyes=acute?Eyes.DISTRESS:Eyes.HALF_CLOSED;mouth=acute?Mouth.OPEN:Mouth.WAVY;
      eyeSource=mouthSource=Source.SYMPTOM;
    } else if(signal.illness()||state.isIll()) {eyes=Eyes.HALF_CLOSED;mouth=Mouth.WAVY;eyeSource=mouthSource=Source.ILLNESS;}
    else {
      if(signal.activityHappy()) {eyes=Eyes.HAPPY_CLOSED;mouth=Mouth.SMILE;eyeSource=mouthSource=Source.ACTIVITY;}
      else if(signal.affectionate()) {eyes=Eyes.HAPPY_CLOSED;mouth=Mouth.SMILE;eyeSource=mouthSource=Source.CARE;}
      else if(signal.groggy()) {eyes=Eyes.HALF_CLOSED;mouth=Mouth.WAVY;eyeSource=mouthSource=Source.MOOD;}
      else if(signal.behaviorMouth()==2) {eyes=Eyes.DISTRESS;mouth=Mouth.FROWN;eyeSource=mouthSource=Source.BEHAVIOR;}
      else if(meter(state,ChaoEmotion.FEAR)>=115) {eyes=Eyes.SURPRISED;mouth=Mouth.OPEN;eyeSource=mouthSource=Source.MOOD;}
      else if(meter(state,ChaoEmotion.CRY)>=100) {eyes=Eyes.DISTRESS;mouth=Mouth.FROWN;eyeSource=mouthSource=Source.MOOD;}
      else if(meter(state,ChaoEmotion.ANGER)>=105) {eyes=Eyes.MEAN;mouth=Mouth.FROWN;eyeSource=mouthSource=Source.MOOD;}
      else if(meter(state,ChaoEmotion.SLEEPINESS)>=8000||meter(state,ChaoEmotion.TIREDNESS)>=6000
          ||meter(state,ChaoEmotion.TIRE)>=8000||Math.max(0,Math.min(100,state.energy))<=15) {
        eyes=Eyes.HALF_CLOSED;mouth=Mouth.WAVY;eyeSource=mouthSource=Source.MOOD;
      } else if(meter(state,ChaoEmotion.JOY)>=115) {eyes=Eyes.HAPPY_CLOSED;mouth=Mouth.SMILE;eyeSource=mouthSource=Source.MOOD;}
      else if(meter(state,ChaoEmotion.RELAX)>=70) {eyes=Eyes.HALF_CLOSED;mouth=Mouth.SMILE;eyeSource=mouthSource=Source.MOOD;}
      else if(meter(state,ChaoEmotion.LONELY)>=4000) {eyes=Eyes.HALF_CLOSED;mouth=Mouth.FROWN;eyeSource=mouthSource=Source.MOOD;}
      boolean calm=meter(state,ChaoEmotion.FEAR)<115&&meter(state,ChaoEmotion.CRY)<100
          &&meter(state,ChaoEmotion.ANGER)<105&&meter(state,ChaoEmotion.LONELY)<4000;
      if(calm&&ChaoIdleRules078.weight(idle,idleElapsed)>.65f
          &&(eyeSource==Source.BASELINE||eyeSource==Source.MOOD)&&!signal.groggy()) {
        boolean expressive=true;
        switch(idle) {
          case "think" -> {eyes=baseEyes==4?Eyes.CHAOS:Eyes.HALF_CLOSED;mouth=Mouth.FLAT;}
          case "bashful" -> {eyes=baseEyes==4?Eyes.CHAOS:Eyes.HALF_CLOSED;mouth=Mouth.SMILE;}
          case "chuckle" -> {eyes=baseEyes==4?Eyes.CHAOS:Eyes.HAPPY_CLOSED;
            mouth=idleElapsed>=22&&idleElapsed<32||idleElapsed>=42&&idleElapsed<50?Mouth.OPEN:Mouth.SMILE;}
          case "tired" -> {eyes=baseEyes==4?Eyes.CHAOS:Eyes.HALF_CLOSED;
            mouth=idleElapsed>=24&&idleElapsed<48?Mouth.OPEN:Mouth.NONE;}
          case "ground_play" -> {mouth=Mouth.SMILE;}
          default -> expressive=false;
        }
        if(expressive){eyeSource=mouthSource=Source.IDLE;}
      }
      if(signal.behaviorMouth()>0&&eyeSource!=Source.ACTIVITY&&eyeSource!=Source.CARE) {
        mouth=authoredMouth(signal.behaviorMouth());mouthSource=Source.BEHAVIOR;
      }
      // Eating and breath reserve the mouth independently from activity/mood eyes.
      if(signal.actionMouth()!=null) {mouth=signal.actionMouth();mouthSource=Source.ACTIVITY;}
      boolean mayBlink=(eyeSource==Source.BASELINE||eyeSource==Source.MOOD)
          &&(eyes==Eyes.NORMAL||eyes==Eyes.HALF_CLOSED||eyes==Eyes.MEAN);
      if(signal.blink()&&mayBlink) {eyes=Eyes.BLINK;eyeSource=Source.BLINK;}
    }
    return new Resolved(baseEyes,baseMouth,eyes,mouth,style,eyeSource,mouthSource,false,iris);
  }

  private static int meter(ChaoState state,int index) {
    return Math.max(0,Math.min(ChaoEmotion.maximum(index),state.emotions[index]));
  }
  public static Eyes baselineEyes(int value) {
    return switch(value) {case 1->Eyes.HALF_CLOSED;case 2->Eyes.HAPPY_CLOSED;case 3->Eyes.MEAN;case 4->Eyes.CHAOS;default->Eyes.NORMAL;};
  }
  public static Mouth baselineMouth(int value) {
    return switch(value) {case 1->Mouth.SMILE;case 2->Mouth.FROWN;case 3->Mouth.GRIN;case 4->Mouth.WAVY;default->Mouth.NONE;};
  }
  public static Mouth authoredMouth(int value) {
    return switch(value) {case 1->Mouth.SMILE;case 2->Mouth.FROWN;case 3->Mouth.FLAT;case 4,6->Mouth.OPEN;case 5->Mouth.WAVY;default->Mouth.NONE;};
  }
  private static Style style(ChaoState state,int iris,boolean excluded) {
    if(excluded)return Style.NATIVE;
    if(iris==9)return Style.HERO_LENS;
    if(iris==10)return Style.DARK_LENS;
    if(iris==11)return Style.CHAOS_LENS;
    float alignment=state.lockedAlignment();
    return state.adult&&alignment>=.5f?Style.HERO:state.adult&&alignment<=-.5f?Style.DARK:Style.NEUTRAL;
  }
}
