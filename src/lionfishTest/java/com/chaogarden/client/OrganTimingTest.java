package com.chaogarden.client;
import com.chaogarden.toys.OrganPlayRules;
public final class OrganTimingTest {
 public static void main(String[] args){
  var clip=new LionfishOrganClip();int samples=0,chords=0;
  for(int q=0;q<=640;q++){
   float t=q/4f;
   for(boolean left:new boolean[]{false,true}){
    float press=clip.tap(t,left);
    if(!Float.isFinite(press)||press<0||press>1)throw new AssertionError("invalid press "+t);
    if(t>=24&&t<=146&&(t-24)%16<=10&&press<.999f)throw new AssertionError("sustained chord released early "+t);
    samples++;
   }
   if(q%4==0&&OrganPlayRules.soundDue((int)t)){
    for(boolean left:new boolean[]{true,false})if(clip.tap(t,left)<.999f||OrganPlayRules.pitch((int)t,left)<.5f||OrganPlayRules.pitch((int)t,left)>2)throw new AssertionError("cue without contact or invalid pitch "+t);
    if(OrganPlayRules.frame((int)t)!=1)throw new AssertionError("keys not depressed at cue");chords++;
   }
  }
  if(chords!=8||clip.tap(Float.NaN,true)!=0||clip.tap(0,false)!=0||clip.tap(160,true)!=0||OrganPlayRules.presence(0)!=0||OrganPlayRules.presence(160)!=0)throw new AssertionError("phrase endpoints");
  System.out.println("Organ timing: "+samples+" real Lionfish samples, eight two-hand sustained chords, contact/cues and endpoints passed");
 }
}
