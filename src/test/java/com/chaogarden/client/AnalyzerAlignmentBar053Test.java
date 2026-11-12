package com.chaogarden.client;

/** Input/geometry contracts independent of a Minecraft client or packet handler. */
public final class AnalyzerAlignmentBar053Test {
  private static int checks;
  private static void check(boolean value,String why){checks++;if(!value)throw new AssertionError(why);}
  private static int edge(int value,float scale,int offset){return offset+Math.round(value*scale);}
  private static boolean overlap(int ax,int ay,int ar,int ab,int bx,int by,int br,int bb){return ax<br&&ar>bx&&ay<bb&&ab>by;}
  public static int run(){
    checks=0;
    // The last left personality row is beside this control; the final right
    // row ends at260. The profile summary begins301; bottom navigation324.
    for(int i=0;i<13;i++){
      int x=220+i/7*176,y=111+i%7*26;
      check(!overlap(396,264,563,298,x,y,x+159,y+20),"Alignment covers personality row "+i);
    }
    check(298<301&&298<324,"Control covers profile/footer");
    check(396>=204&&563<584,"Control leaves the right information panel");
    for(float scale:new float[]{.2f,.3f,.41f,.5f,.625f,.75f,1f})for(int ox:new int[]{0,7,83,-21})for(int oy:new int[]{0,5,31,-9}){
      int left=edge(398,scale,ox),right=edge(560,scale,ox),top=edge(274,scale,oy),bottom=edge(290,scale,oy);
      double y=(top+bottom)/2.0;
      check(AnalyzerAlignmentBar053.hit((left+right)/2.0,y,scale,ox,oy),"Scaled track centre not clickable");
      check(!AnalyzerAlignmentBar053.hit(edge(395,scale,ox)-1,y,scale,ox,oy),"Outside-left click captured");
      check(!AnalyzerAlignmentBar053.hit(edge(563,scale,ox)+1,y,scale,ox,oy),"Outside-right click captured");
      check(!AnalyzerAlignmentBar053.hit(left,edge(301,scale,oy),scale,ox,oy),"Profile text click captured");
      check(!AnalyzerAlignmentBar053.hit(left,edge(324,scale,oy),scale,ox,oy),"Navigation click captured");
      for(int expected:new int[]{-1000,-750,-500,-1,0,1,500,750,1000}){
        double mouse=left+(right-left)*(expected+1000)/2000.0;
        check(AnalyzerAlignmentBar053.value(mouse,scale,ox)==expected,"Scaled alignment value drifted: "+expected);
      }
      check(AnalyzerAlignmentBar053.value(left-10000,scale,ox)==-1000,"Captured drag escaped Dark endpoint");
      check(AnalyzerAlignmentBar053.value(right+10000,scale,ox)==1000,"Captured drag escaped Hero endpoint");
    }
    for(float invalid:new float[]{0,-1,Float.NaN,Float.POSITIVE_INFINITY}){
      check(!AnalyzerAlignmentBar053.hit(479,280,invalid,0,0),"Invalid scale accepted");
      check(AnalyzerAlignmentBar053.value(479,invalid,0)==null,"Invalid scale produced a request");
    }
    for(double invalid:new double[]{Double.NaN,Double.NEGATIVE_INFINITY,Double.POSITIVE_INFINITY}){
      check(!AnalyzerAlignmentBar053.hit(invalid,280,1,0,0),"Non-finite pointer captured");
      check(!AnalyzerAlignmentBar053.hit(479,invalid,1,0,0),"Non-finite pointer Y captured");
      check(AnalyzerAlignmentBar053.value(invalid,1,0)==null,"Non-finite pointer produced request");
    }
    check(AnalyzerAlignmentBar053.marker(-1000)==398,"Dark marker position");
    check(AnalyzerAlignmentBar053.marker(0)==479,"Neutral marker position");
    check(AnalyzerAlignmentBar053.marker(1000)==560,"Hero marker position");
    check(AnalyzerAlignmentBar053.marker(Integer.MIN_VALUE)==398&&AnalyzerAlignmentBar053.marker(Integer.MAX_VALUE)==560,"Marker escaped track");
    check(AnalyzerAlignmentBar053.category(-500).equals("Dark"),"Dark threshold");
    check(AnalyzerAlignmentBar053.category(-499).equals("Neutral"),"Neutral lower edge");
    check(AnalyzerAlignmentBar053.category(499).equals("Neutral"),"Neutral upper edge");
    check(AnalyzerAlignmentBar053.category(500).equals("Hero"),"Hero threshold");
    check(AnalyzerAlignmentBar053.category(Math.nextDown(.5f)).equals("Neutral"),"Rounding invented Hero identity");
    check(AnalyzerAlignmentBar053.category(Math.nextUp(-.5f)).equals("Neutral"),"Rounding invented Dark identity");
    check(AnalyzerAlignmentBar053.milli(Float.NaN)==0&&AnalyzerAlignmentBar053.milli(2)==1000&&AnalyzerAlignmentBar053.milli(-2)==-1000,"Display bound sanitization");
    return checks;
  }
  public static void main(String[] args){System.out.println("Analyzer alignment bar053: "+run()+" geometry/input checks passed");}
}
