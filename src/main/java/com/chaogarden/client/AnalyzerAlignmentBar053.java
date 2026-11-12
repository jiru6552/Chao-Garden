package com.chaogarden.client;

/** Shared design-canvas geometry for the Analyzer's alignment display and drag input. */
public final class AnalyzerAlignmentBar053 {
  public static final int X=398,Y=279,WIDTH=162,HEIGHT=7;
  // Include the caption/end labels so a tap need not land on the seven-pixel track.
  // The preceding personality row ends at260 and the profile footer starts301.
  public static final int HIT_LEFT=396,HIT_RIGHT=562,HIT_TOP=264,HIT_BOTTOM=300;
  public static final int MIN=-1000,MAX=1000;
  private AnalyzerAlignmentBar053() {}

  private static boolean scale(float scale) {return Float.isFinite(scale)&&scale>0;}
  private static int edge(int value,float scale,int offset) {return offset+Math.round(value*scale);}

  /** Match the rounded canvas edges used by the actual screen at every GUI scale. */
  public static boolean hit(double x,double y,float scale,int ox,int oy) {
    return scale(scale)&&Double.isFinite(x)&&Double.isFinite(y)
        &&x>=edge(HIT_LEFT,scale,ox)&&x<edge(HIT_RIGHT,scale,ox)
        &&y>=edge(HIT_TOP,scale,oy)&&y<edge(HIT_BOTTOM,scale,oy);
  }

  /** A captured drag can leave the track; its value stops at the real Dark/Hero endpoints. */
  public static Integer value(double x,float scale,int ox) {
    if(!scale(scale)||!Double.isFinite(x))return null;
    int left=edge(X,scale,ox),right=edge(X+WIDTH,scale,ox);
    if(right<=left)return null;
    double fraction=Math.max(0,Math.min(1,(x-left)/(right-left)));
    return (int)Math.round(MIN+(MAX-MIN)*fraction);
  }

  public static int milli(float alignment) {
    return Float.isFinite(alignment)?Math.round(Math.max(-1,Math.min(1,alignment))*1000):0;
  }
  public static int marker(int value) {
    return X+Math.round((Math.max(MIN,Math.min(MAX,value))-MIN)*WIDTH/(float)(MAX-MIN));
  }
  public static String category(int value) {return value<=-500?"Dark":value>=500?"Hero":"Neutral";}
  public static String category(float value) {return value<=-.5f?"Dark":value>=.5f?"Hero":"Neutral";}
}
