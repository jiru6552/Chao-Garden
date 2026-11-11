package com.chaogarden.client;
import com.chaogarden.toys.PianoPlayRules;
public final class PianoTimingTest {
 public static void main(String[] args){
  var clip=new LionfishPianoClip();int checks=0,cues=0;
  for(int t=0;t<=160;t++){
   for(boolean left:new boolean[]{false,true}){float v=clip.tap(t,left);if(!Float.isFinite(v)||v<0||v>1)throw new AssertionError("Bad press "+t);checks++;}
   if(PianoPlayRules.soundDue(t)){
    if(clip.tap(t,PianoPlayRules.left(t))<.999||clip.tap(t,!PianoPlayRules.left(t))>.001)throw new AssertionError("Sound without alternating hand contact "+t);
    if(PianoPlayRules.frame(t)!=1||PianoPlayRules.pitch(t)<.5||PianoPlayRules.pitch(t)>2)throw new AssertionError("Key/pitch mismatch "+t);cues++;
   }
  }
  if(cues!=16||clip.tap(Float.NaN,true)!=0||PianoPlayRules.presence(0)!=0||PianoPlayRules.presence(160)!=0)throw new AssertionError("Phrase endpoints");
  System.out.println("Piano Lionfish timing: "+checks+" samples, 16 alternating contacts, key frames, pitch limits and endpoints passed");
 }
}
