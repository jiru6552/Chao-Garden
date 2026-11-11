package com.chaogarden.core;

/** Creative edits use the existing grade-dependent mean point gain, without rerolling earned points. */
public final class AnalyzerStatRules {
  public static final int GRADE=0,LEVEL=1,PROGRESS=2;
  private AnalyzerStatRules() {}
  public record Edit(int stat,int action,int value) {}
  public static int levelDelta(boolean shift,boolean control){return (shift?-1:1)*(control?10:1);}
  /** Coordinates are in the Analyzer's 600 by 360 design canvas. */
  public static Edit click(double x,double y,boolean shift,boolean control){
    return clickScaled(x,y,shift,control,1,0,0);
  }
  /** Round each visible edge exactly as the canvas does, before hit testing. */
  public static Edit clickScaled(double x,double y,boolean shift,boolean control,float scale,int ox,int oy){
    if(!Float.isFinite(scale)||scale<=0)return null;
    for(int i=0;i<5;i++){
      int row=109+i*26;
      if(x>=edge(549,scale,ox)&&x<edge(568,scale,ox)&&y>=edge(row-4,scale,oy)&&y<edge(row+15,scale,oy))return new Edit(i,GRADE,0);
      if(x>=edge(322,scale,ox)&&x<edge(365,scale,ox)&&y>=edge(row-2,scale,oy)&&y<edge(row+10,scale,oy))return new Edit(i,LEVEL,levelDelta(shift,control));
      int start=edge(242,scale,ox),end=edge(498,scale,ox);
      if(x>=start&&x<edge(499,scale,ox)&&y>=edge(row+12,scale,oy)&&y<edge(row+20,scale,oy))
        return new Edit(i,PROGRESS,(int)Math.round((x-start)*99/Math.max(1,end-start)));
    }
    return null;
  }
  private static int edge(float value,float scale,int offset){return offset+Math.round(value*scale);}
  public static boolean apply(ChaoState s,int stat,int action,int value){
    if(s.dead||stat<0||stat>=5)return false;
    int oldLevel=s.levels[stat],oldProgress=s.progress[stat];
    switch(action){
      case GRADE -> {if(value!=0)return false;s.grades[stat]=(s.grades[stat]+1)%7;}
      case LEVEL -> {
        if(value!=1&&value!=-1&&value!=10&&value!=-10)return false;
        int next=Math.clamp(oldLevel+value,0,99);
        s.points[stat]=Math.clamp(s.points[stat]+(next-oldLevel)*(13+3*s.grades[stat]),0,9999);
        s.levels[stat]=next;if(next==99)s.progress[stat]=0;
      }
      case PROGRESS -> s.progress[stat]=s.levels[stat]>=99?0:Math.clamp(value,0,99);
      default -> {return false;}
    }
    if(s.levels[stat]>oldLevel||s.progress[stat]>oldProgress)
      s.recordCreativeTraining(stat);
    return true;
  }
}
