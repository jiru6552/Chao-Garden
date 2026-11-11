package com.chaogarden.toys;

/** Shared production clocks for the car, two plush dolls and the pogo stick. */
public final class ActiveToyPlayRules049 {
  public static final String CAR="chao_toy_car",SONIC="chao_sonic_doll",SHADOW="chao_shadow_doll",POGO="chao_pogo_stick";
  public static final float POGO_SCALE=.70f,POGO_PEDAL=1.2f/2.75f;
  private ActiveToyPlayRules049(){}
  public static boolean supports(String kind){return kind.equals(CAR)||doll(kind)||kind.equals(POGO);}
  public static boolean doll(String kind){return kind.equals(SONIC)||kind.equals(SHADOW);}
  public static boolean soundDue(String kind,int tick){
    if(kind.equals(CAR))return tick>=32&&tick<=128&&(tick-32)%32==0;
    if(doll(kind))return false;
    return kind.equals(POGO)&&tick>=24&&tick<=136&&(tick-24)%16==0;
  }
  public static float presence(float tick){return MaracasPlayRules.presence(tick);}
  public static boolean happy(float tick){return tick>=12&&tick<146;}
  public static float pogoBounce(float tick){
    if(!Float.isFinite(tick)||tick<24||tick>=136)return 0;
    float s=(float)Math.sin(Math.PI*(tick-24)/16);return s*s;
  }
  /** Actor soles sit on the real pedals; the spring's tip meets the ground at landings. */
  public static float pogoHeight(float tick,float actorScale){return POGO_SCALE*POGO_PEDAL*actorScale+.008f+.22f*pogoBounce(tick);}
}
