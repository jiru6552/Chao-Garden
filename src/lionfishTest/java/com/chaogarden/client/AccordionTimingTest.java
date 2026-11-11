package com.chaogarden.client;

import com.chaogarden.toys.AccordionPlayRules;

/** Sample real Lionfish channels against the production sound clock, including sub-tick motion. */
public final class AccordionTimingTest {
  private static void check(boolean ok,String why){if(!ok)throw new AssertionError(why);}
  public static void main(String[] args){
    var clip=new LionfishAccordionClip();int cues=0,samples=0;float prior=clip.bellows(0),priorKey=clip.keyHeight(0);
    for(int quarter=0;quarter<=640;quarter++){
      float tick=quarter*.25f,bellows=clip.bellows(tick),keys=clip.keys(tick),bass=clip.bass(tick),height=clip.keyHeight(tick);
      for(float value:new float[]{bellows,keys,bass})check(Float.isFinite(value)&&value>=0&&value<=1,"bounded channel at "+tick);
      check(Float.isFinite(height)&&height>=.455f&&height<=.605f,"hand remains within actual keybed at "+tick);
      if(quarter>0){check(Math.abs(bellows-prior)<.026f,"no bellows snap at "+tick);check(Math.abs(height-priorKey)<.008f,"no keyboard jump at "+tick);}
      prior=bellows;priorKey=height;samples++;
      if(quarter%4==0&&AccordionPlayRules.soundDue((int)tick)){
        check(Math.abs(bellows-.5f)<.0001f,"reed attack at the middle of the bellows stroke "+tick);
        check(keys>.999f&&bass>.999f,"both hands contact on reed attack "+tick);
        float velocity=clip.bellows(tick+1)-clip.bellows(tick-1);
        check((cues%2==0?velocity:-velocity)>.12f,"alternating outward/inward airflow at "+tick);
        float pitch=AccordionPlayRules.pitch((int)tick);check(pitch>=1&&pitch<1.6f,"safe reed pitch at "+tick);
        check(.68f/pitch<.8f,"reed release completes before next phrase attack "+tick);
        cues++;
      }
    }
    check(cues==8,"eight authored reed notes");
    for(int tick=-20;tick<24;tick++)check(!AccordionPlayRules.soundDue(tick),"silent raising");
    for(int tick=137;tick<181;tick++)check(!AccordionPlayRules.soundDue(tick),"silent lowering");
    for(int tick=32;tick<=128;tick+=32)check(Math.abs(clip.bellows(tick)-1)<.00001f,"fully expanded pleats "+tick);
    for(int tick=48;tick<=144;tick+=32)check(clip.bellows(tick)<.00001f,"compressed pleats "+tick);
    check(AccordionPlayRules.presence(0)==0&&AccordionPlayRules.presence(160)==0,"natural entry and exit");
    check(!AccordionPlayRules.happy(0)&&!AccordionPlayRules.happy(160),"expression yields at rest");
    float before=clip.bellows(40);clip.bellows(128);check(before==clip.bellows(40),"shared clip has no actor pose leak");
    check(clip.bellows(Float.NaN)==0&&clip.keys(Float.NaN)==0&&clip.bass(Float.NaN)==0,"invalid samples reset channels");
    System.out.println("Accordion Lionfish timing: "+samples+" quarter-tick samples; eight alternating airflow/key attacks, safe pitch/release and clean rest endpoints passed");
  }
}
