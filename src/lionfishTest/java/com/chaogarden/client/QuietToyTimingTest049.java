package com.chaogarden.client;
import com.chaogarden.toys.*;
/** Real Lionfish sampling: audible cues, original-source page hinges and transient strokes. */
public final class QuietToyTimingTest049 {
 static void check(boolean b,String why){if(!b)throw new AssertionError(why);}
 public static void main(String[]args){
  var clip=new LionfishQuietToyClip049();int samples=0;int[] cues={0,0,0};float maxSweepStep=0,maxDrawStep=0,maxPageStep=0;
  for(int q=0;q<=640;q++){
   float t=q*.25f,s=clip.sweep(t),page=clip.page(t),stroke=clip.stroke(t),pen=clip.pen(t);samples++;
   check(Float.isFinite(s)&&s>=-1&&s<=1&&Float.isFinite(page)&&page>=0&&page<=1&&Float.isFinite(stroke)&&stroke>=0&&stroke<=1&&Float.isFinite(pen)&&pen>=0&&pen<=1,"finite bounded sample "+t);
   if(q>0){maxSweepStep=Math.max(maxSweepStep,Math.abs(s-clip.sweep(t-.25f)));maxDrawStep=Math.max(maxDrawStep,Math.abs(stroke-clip.stroke(t-.25f)));if(QuietToyPlayRules049.turning(t)&&QuietToyPlayRules049.turning(t-.25f))maxPageStep=Math.max(maxPageStep,Math.abs(page-clip.page(t-.25f)));}
   if(q%4!=0)continue;
   int tick=(int)t,index=0;
   for(String kind:new String[]{QuietToyPlayRules049.BROOM,QuietToyPlayRules049.BOOK,QuietToyPlayRules049.CRAYONS}){
    if(QuietToyPlayRules049.soundDue(kind,tick)){
     cues[index]++;check(QuietToyPlayRules049.presence(t)==1,"no sound during entry/exit");
     if(index==0){check(Math.abs(s)<.0001f,"sweep sound at fastest crossing "+tick);check(Math.abs(clip.sweep(t+2)-clip.sweep(t-2))>.3f,"brush actually moves on sound");}
     if(index==1)check(QuietToyPlayRules049.turning(t)&&page>.05f&&page<.95f,"paper sound while original leaf turns");
     if(index==2){check(stroke<.0001f&&pen>.999f,"scratch begins with crayon touching paper "+tick);check(QuietToyPlayRules049.inkStage(t)==(tick-24)/16,"drawing starts with previous completed strokes");check(QuietToyPlayRules049.inkStage(t+10)==(tick-24)/16+1,"ink appears after crayon completes its stroke");}
    }index++;
   }
  }
  check(cues[0]==8&&cues[1]==2&&cues[2]==8,"authored independent cue counts");check(maxSweepStep<.13&&maxPageStep<.08&&maxDrawStep<.25,"readable curves "+maxSweepStep+","+maxPageStep+","+maxDrawStep);
  check(Math.abs(clip.sweep(0))<1e-5f&&Math.abs(clip.sweep(160))<1e-5f&&clip.pen(160)==0&&clip.page(160)==0&&clip.stroke(160)==0,"rest endpoints sweep="+clip.sweep(0)+","+clip.sweep(160)+" pen="+clip.pen(160)+" page="+clip.page(160)+" stroke="+clip.stroke(160));check(clip.page(Float.NaN)==0&&clip.stroke(Float.POSITIVE_INFINITY)==0,"invalid time resets");check(QuietToyPlayRules049.inkStage(160)==8,"finished drawing retained until cleanup");
  System.out.println("Quiet Lionfish timing: "+samples+" quarter-tick samples; 8 sweep/2 page/8 scratch cues; completed-ink order and clean endpoints passed; max step="+maxSweepStep+","+maxPageStep+","+maxDrawStep);
 }
}
