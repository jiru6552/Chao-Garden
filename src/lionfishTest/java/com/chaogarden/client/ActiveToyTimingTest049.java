package com.chaogarden.client;
import com.chaogarden.toys.ActiveToyPlayRules049;
/** The actual Lionfish impulses meet the same production ticks as cues and pogo landings. */
public final class ActiveToyTimingTest049 {
 public static void main(String[] args){
  var clip=new LionfishActiveToyClip049();int samples=0,car=0,doll=0,pogo=0,lifts=0;
  for(int q=0;q<=640;q++){
   float t=q/4f;float[] values={clip.push(t),clip.lift(t),clip.flex(t),ActiveToyPlayRules049.presence(t),ActiveToyPlayRules049.pogoBounce(t)};
   if(!Float.isFinite(clip.carSweep(t))||Math.abs(clip.carSweep(t))>1)throw new AssertionError("bounded lateral car track "+t);
   for(float value:values)if(!Float.isFinite(value)||value<0||value>1)throw new AssertionError("bounded track "+t);
   for(float scale:new float[]{.64f,.80f,.86f}){
    float base=.70f*(1.2f/2.75f)*scale+.008f,at=ActiveToyPlayRules049.pogoHeight(t,scale);
    if(at<base-.00001||at>base+.22001)throw new AssertionError("pogo floor/height "+t);
   }
   if(q%4==0){
    int tick=q/4;
    if(ActiveToyPlayRules049.soundDue(ActiveToyPlayRules049.SHADOW,tick))throw new AssertionError("Shadow doll must be silent");
    if(tick==36||tick==84||tick==132){lifts++;if(clip.lift(t)<.999)throw new AssertionError("doll lift lost without its removed sound");}
    if(ActiveToyPlayRules049.soundDue(ActiveToyPlayRules049.CAR,tick)){car++;if(clip.push(t)<.999||Math.abs(clip.carSweep(t))<.999)throw new AssertionError("car click not on push");}
    if(ActiveToyPlayRules049.soundDue(ActiveToyPlayRules049.SONIC,tick)){doll++;if(clip.lift(t)<.999)throw new AssertionError("plush sound not on lift");}
    if(ActiveToyPlayRules049.soundDue(ActiveToyPlayRules049.POGO,tick)){pogo++;if(clip.flex(t)<.999||ActiveToyPlayRules049.pogoBounce(t)>.0001)throw new AssertionError("pogo sound not on ground compression");}
   }
   samples++;
  }
  if(car!=4||doll!=0||lifts!=3||pogo!=8||clip.carSweep(0)!=0||clip.carSweep(160)!=0||clip.carSweep(Float.NaN)!=0||clip.push(Float.NaN)!=0||clip.lift(0)!=0||clip.flex(160)!=0)throw new AssertionError("complete cue/endpoints");
  System.out.println("Active toys timing: "+samples+" quarterticks; four lateral car reversals, three silent doll lifts, eight pogo landings; all ages/scales bounded and clean endpoints passed");
 }
}
