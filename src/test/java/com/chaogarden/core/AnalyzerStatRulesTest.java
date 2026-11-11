package com.chaogarden.core;
import java.util.Arrays;
public final class AnalyzerStatRulesTest {
 public static void main(String[] args){
  var s=new ChaoState();s.grades[0]=0;s.gradeA[0]=2;s.gradeB[0]=3;s.points[0]=333;
  for(int i=1;i<=7;i++){assert AnalyzerStatRules.apply(s,0,0,0);assert s.grades[0]==i%7;assert s.points[0]==333;}
  assert s.gradeA[0]==2&&s.gradeB[0]==3;
  for(boolean shift:new boolean[]{false,true})for(boolean control:new boolean[]{false,true}){
   s.levels[1]=50;s.grades[1]=4;s.points[1]=1500;s.progress[1]=27;
   var edit=AnalyzerStatRules.click(340,135,shift,control);assert edit.stat()==1&&edit.action()==1;
   int delta=(shift?-1:1)*(control?10:1);assert edit.value()==delta;
   AnalyzerStatRules.apply(s,edit.stat(),edit.action(),edit.value());assert s.levels[1]==50+delta;assert s.points[1]==1500+delta*25;assert s.progress[1]==27;
  }
  for(int i=0;i<5;i++)for(int x:new int[]{242,370,498}){
   var edit=AnalyzerStatRules.click(x,123+26*i,false,false);assert edit.stat()==i;
   assert edit.value()==(x==242?0:x==498?99:50);
  }
  s.levels[0]=98;s.grades[0]=6;s.points[0]=9980;s.progress[0]=99;
  AnalyzerStatRules.apply(s,0,1,10);assert s.levels[0]==99&&s.points[0]==9999&&s.progress[0]==0;
  AnalyzerStatRules.apply(s,0,2,Integer.MAX_VALUE);assert s.progress[0]==0;
  s.levels[0]=2;s.points[0]=5;AnalyzerStatRules.apply(s,0,1,-10);assert s.levels[0]==0&&s.points[0]==0;
  AnalyzerStatRules.apply(s,0,2,Integer.MIN_VALUE);assert s.progress[0]==0;
  AnalyzerStatRules.apply(s,0,2,Integer.MAX_VALUE);assert s.progress[0]==99&&s.lastTrainedStat()==0;
  String before=s.encode();for(int stat:new int[]{-1,5,Integer.MAX_VALUE})assert !AnalyzerStatRules.apply(s,stat,1,1);
  assert !AnalyzerStatRules.apply(s,0,1,Integer.MAX_VALUE);assert !AnalyzerStatRules.apply(s,0,17,0);assert before.equals(s.encode());
  assert AnalyzerStatRules.click(200,122,false,false)==null;assert AnalyzerStatRules.click(499,123,false,false)==null;
  for(float scale:new float[]{1,.6333333f,.4f})for(int i=0;i<5;i++){
   int start=10+Math.round(242*scale),end=10+Math.round(498*scale),y=8+Math.round((123+26*i)*scale);
   assert AnalyzerStatRules.clickScaled(start,y,false,false,scale,10,8).value()==0;
   assert AnalyzerStatRules.clickScaled((start+end)/2.0,y,false,false,scale,10,8).value()==50;
   assert AnalyzerStatRules.clickScaled(end,y,false,false,scale,10,8).value()==99;
  }
  var copy=ChaoState.decode(s.encode());assert Arrays.equals(copy.grades,s.grades)&&Arrays.equals(copy.levels,s.levels)&&Arrays.equals(copy.points,s.points)&&Arrays.equals(copy.progress,s.progress);
  System.out.println("Analyzer editing: rank cycle, four modifiers, five hit rows, endpoints, caps, invalid requests and round trip passed");
 }
}
