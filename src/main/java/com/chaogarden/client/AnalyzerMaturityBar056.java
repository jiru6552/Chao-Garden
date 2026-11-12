package com.chaogarden.client;

/** Scaled pointer mapping for the Analyzer's current-life-stage maturity scrubber. */
public final class AnalyzerMaturityBar056 {
  public static final int X=318,Y=293,WIDTH=244,HEIGHT=5;
  public static final int HIT_LEFT=316,HIT_RIGHT=564,HIT_TOP=287,HIT_BOTTOM=314;
  public static final int CHILD_MAX=990,ADULT_MAX=1000;
  private AnalyzerMaturityBar056() {}

  private static boolean validScale(float scale) {return Float.isFinite(scale)&&scale>0;}
  private static int edge(int value,float scale,int offset) {return offset+Math.round(value*scale);}
  public static boolean hit(double x,double y,float scale,int ox,int oy) {
    return validScale(scale)&&Double.isFinite(x)&&Double.isFinite(y)
        &&x>=edge(HIT_LEFT,scale,ox)&&x<edge(HIT_RIGHT,scale,ox)
        &&y>=edge(HIT_TOP,scale,oy)&&y<edge(HIT_BOTTOM,scale,oy);
  }
  /** Captured pointer movement is clamped, including taps beyond either track end. */
  public static Integer value(double x,float scale,int ox,boolean adult) {
    if(!validScale(scale)||!Double.isFinite(x))return null;
    int left=edge(X,scale,ox),right=edge(X+WIDTH,scale,ox);
    if(right<=left)return null;
    int value=(int)Math.round(1000*Math.max(0,Math.min(1,(x-left)/(right-left))));
    return Math.min(adult?ADULT_MAX:CHILD_MAX,value);
  }
  public static int milli(float strength,boolean adult) {
    return Float.isFinite(strength)?Math.round(Math.max(0,Math.min(1,strength/(adult?1.2f:1f)))*1000):0;
  }
  public static int marker(int milli) {return X+Math.round(Math.max(0,Math.min(1000,milli))*WIDTH/1000f);}
  public static String percent(int milli) {return String.format(java.util.Locale.ROOT,"%.1f%%",Math.max(0,Math.min(1000,milli))/10f);}
}
