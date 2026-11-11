package com.chaogarden.core;
import java.util.*;
import static com.chaogarden.core.DriveRules.Tier.*;
public final class DriveRulesTest {
  static ChaoState state(){var s=new ChaoState();s.happiness=0;Arrays.fill(s.levels,26);Arrays.fill(s.progress,43);Arrays.fill(s.points,650);Arrays.fill(s.grades,4);s.hiddenLevels=new int[]{10,10};s.hiddenProgress=new int[]{80,90};s.luck=s.intelligence=300;return s;}
  static class Fixed extends Random {final int percent,thousand;final boolean sign;Fixed(int p,int t,boolean b){percent=p;thousand=t;sign=b;}
    @Override public int nextInt(int bound){return bound==100?percent:bound==1000?thousand:bound==5?2:bound==201?100:bound==21?10:0;}
    @Override public boolean nextBoolean(){return sign;}}
  public static void main(String[] args){
    var random=new Random(430043);var s=state();
    assert DriveRules.apply(s,SOLID,0,random)&&s.levels[0]==27&&s.progress[0]==43;
    assert s.points[0]>=673&&s.points[0]<=677&&s.happiness==1&&s.driveCount==1;
    assert s.hiddenLevels[0]==11&&s.hiddenProgress[0]==20&&s.hiddenLevels[1]==11&&s.hiddenProgress[1]==10;
    s=state();DriveRules.apply(s,SUPER_SOLID,3,random);assert s.levels[3]==36&&s.progress[3]==43;
    s=state();DriveRules.apply(s,CHAOTIC,-1,random);for(int i=0;i<4;i++)assert s.levels[i]==26&&s.progress[i]==67;assert s.progress[4]==43;
    Set<String> permutations=new HashSet<>();
    for(int n=0;n<2400;n++){
      s=state();DriveRules.apply(s,SUPER_CHAOTIC,-1,random);int[] deltas=new int[4];
      for(int i=0;i<4;i++)deltas[i]=DriveRules.total(s,i)-2643;
      permutations.add(Arrays.toString(deltas));Arrays.sort(deltas);assert Arrays.equals(deltas,new int[]{-200,24,100,500});assert s.levels[4]==26;
    }assert permutations.size()==24;
    s=state();s.levels[0]=98;DriveRules.apply(s,SUPER_SOLID,0,random);assert s.levels[0]==99&&s.progress[0]==0;
    DriveRules.setTotal(s,0,0,random);assert s.points[0]==0&&s.levels[0]==0&&s.progress[0]==0;
    s=state();DriveRules.change(s,0,-200,random);assert s.levels[0]==24&&s.progress[0]==43&&s.points[0]==600;
    DriveRules.change(s,0,-44,random);assert s.levels[0]==23&&s.progress[0]==99&&s.points[0]==575;
    DriveRules.change(s,0,Integer.MIN_VALUE,random);assert DriveRules.total(s,0)==0;
    DriveRules.change(s,0,Integer.MAX_VALUE,random);assert DriveRules.total(s,0)==9900&&s.points[0]<=9999;
    s=state();s.hiddenProgress[0]=10;DriveRules.adjustHidden(s,0,-40,random);assert s.hiddenLevels[0]==9&&s.hiddenProgress[0]==70&&s.luck==287;
    s=state();s.grades[0]=5;int[] alleles=s.gradeA.clone();DriveRules.apply(s,UNSTABLE_CHAOTIC,-1,new Fixed(0,6,true));assert s.levels[0]==99&&s.grades[0]==6&&Arrays.equals(alleles,s.gradeA);
    DriveRules.apply(s,UNSTABLE_CHAOTIC,-1,new Fixed(0,6,true));assert s.grades[0]==6;
    s.grades[0]=0;DriveRules.apply(s,UNSTABLE_CHAOTIC,-1,new Fixed(0,0,false));assert s.levels[0]==0&&s.grades[0]==0;
    s=state();Arrays.fill(s.levels,99);Arrays.fill(s.progress,0);s.levels[4]=98;
    assert DriveRules.apply(s,PERFECT,-1,random)&&s.levels[4]==99;String maxed=s.encode();
    assert !DriveRules.apply(s,PERFECT,-1,random)&&maxed.equals(s.encode());
    int max=0,min=0,rank=0,middle=0,edge=0,solidMax=0;int runs=50000;
    for(int n=0;n<runs;n++){
      s=state();DriveRules.apply(s,UNSTABLE_CHAOTIC,-1,random);
      for(int i=0;i<4;i++){int t=DriveRules.total(s,i);if(t!=2643){if(t==9900)max++;else if(t==0)min++;else if(t>=4000&&t<6000)middle++;else if(t<1000||t>=8900)edge++;}if(s.grades[i]!=4)rank++;}
      assert s.happiness>=-10&&s.happiness<=10&&s.luck==300;
      String encoded=s.encode();assert ChaoState.decode(encoded).encode().equals(encoded);
      s=state();DriveRules.apply(s,UNSTABLE_SOLID,2,random);if(s.levels[2]==99)solidMax++;else assert s.levels[2]==0;
      assert s.happiness==1||s.happiness==-1;assert s.progress[2]==0&&s.grades[2]==4;
    }
    assert max>180&&max<320&&min>180&&min<320&&rank>850&&rank<1150&&middle>edge*4;
    assert solidMax>2200&&solidMax<2800;
    s=state();s.dead=true;String dead=s.encode();assert !DriveRules.apply(s,CHAOTIC,0,random)&&dead.equals(s.encode());
    System.out.println("Drive rules: arithmetic, all 24 permutations, caps, hidden losses, X/rank limits, Perfect no-op, 50,000 persisted rolls; endpoints="+min+"/"+max+", rank="+rank+", Solid max="+solidMax+", middle/edge="+middle+"/"+edge);
  }
}
