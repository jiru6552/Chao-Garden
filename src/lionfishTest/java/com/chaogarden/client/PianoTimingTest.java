package com.chaogarden.client;
import com.chaogarden.toys.PianoPlayRules;
public final class PianoTimingTest {
 public static void main(String[] args){
  var clip=new LionfishPianoClip();int checks=0,cues=0;float largestStep=0;
  for(int q=0;q<=640;q++){
   float t=q/4f;
   for(boolean left:new boolean[]{false,true}){
    float v=clip.tap(t,left);
    if(!Float.isFinite(v)||v<0||v>1)throw new AssertionError("Bad press "+t);
    if(q>0)largestStep=Math.max(largestStep,Math.abs(v-clip.tap(t-.25f,left)));
    checks++;
   }
   float presence=PianoPlayRules.presence(t);
   if(!Float.isFinite(presence)||presence<0||presence>1)throw new AssertionError("Bad transition "+t);
   if(q%4==0&&PianoPlayRules.soundDue((int)t)){
    boolean left=PianoPlayRules.left((int)t);
    if(clip.tap(t,left)<.999||clip.tap(t,!left)>.001)throw new AssertionError("Sound without alternating hand contact "+t);
    if(clip.tap(t-4,left)>.001||clip.tap(t+3,left)>.001)throw new AssertionError("Press did not prepare/release "+t);
    if(PianoPlayRules.frame((int)t)!=1||PianoPlayRules.pitch((int)t)<.5||PianoPlayRules.pitch((int)t)>2||presence!=1)throw new AssertionError("Key/pitch mismatch "+t);cues++;
   }
  }
  if(cues!=16||largestStep>.2f||clip.tap(Float.NaN,true)!=0||clip.tap(0,false)!=0||clip.tap(160,true)!=0||PianoPlayRules.presence(0)!=0||PianoPlayRules.presence(160)!=0)throw new AssertionError("Phrase endpoints/smoothness "+largestStep);
  System.out.println("Piano Lionfish timing: "+checks+" samples, 16 alternating contacts, key frames, pitch limits, smooth presses and endpoints passed; max quarter-tick step="+largestStep);
 }
}
