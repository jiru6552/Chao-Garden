package com.chaogarden.client;

import com.chaogarden.toys.GuitarPlayRules;

/** Sample the actual Lionfish strum channels against the shared production cue clock. */
public final class GuitarTimingTest {
  private static void check(boolean ok,String why){if(!ok)throw new AssertionError(why);}
  public static void main(String[] args){
    var clip=new LionfishGuitarClip();int cues=0,samples=0;float prior=clip.strum(0);
    for(int q=0;q<=640;q++){
      float t=q*.25f,strum=clip.strum(t),fret=clip.fret(t);
      check(Float.isFinite(strum)&&strum>=-1.0001f&&strum<=1.0001f,"bounded strum at "+t);
      check(Float.isFinite(fret)&&fret>=.5599f&&fret<=.6501f,"fretting stays on measured neck at "+t);
      if(q>0)check(Math.abs(strum-prior)<.26f,"no quarter-tick hand snap at "+t);prior=strum;samples++;
      if(q%4==0&&GuitarPlayRules.soundDue((int)t)){
        check(t>=24&&t<=144&&((int)t-24)%8==0,"cue outside authored phrase");
        check(Math.abs(strum)<.0001f,"sound must coincide with crossing strings at "+t);
        float before=clip.strum(t-1),after=clip.strum(t+1);
        check(before*after<0,"strum actually crosses strings at "+t);
        check((cues%2==0?after-before:before-after)>0,"alternating down/up stroke at "+t);
        float pitch=GuitarPlayRules.pitch((int)t);check(Float.isFinite(pitch)&&pitch>=.5f&&pitch<=2,"pitch headroom at "+t);
        cues++;
      }
    }
    check(cues==16,"sixteen authored strums");
    for(int t=-20;t<24;t++)check(!GuitarPlayRules.soundDue(t),"no cue during raising");
    for(int t=145;t<=180;t++)check(!GuitarPlayRules.soundDue(t),"no cue during lowering");
    check(GuitarPlayRules.presence(0)==0&&GuitarPlayRules.presence(160)==0,"natural rest endpoints");
    check(GuitarPlayRules.presence(24)==1&&GuitarPlayRules.presence(144)==1,"fully present at every contact");
    check(!GuitarPlayRules.happy(0)&&!GuitarPlayRules.happy(160),"expression yields at endpoints");
    System.out.println("Guitar Lionfish timing: "+samples+" quarter-tick samples; 16 alternating string crossings, bounded fret positions, cue/pitch limits and endpoints passed");
  }
}
