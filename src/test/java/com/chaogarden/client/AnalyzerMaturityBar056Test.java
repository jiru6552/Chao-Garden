package com.chaogarden.client;

/** Actual canvas contracts: narrow-view tap/drag input, endpoint bounds and control separation. */
public final class AnalyzerMaturityBar056Test {
  private static int checks;
  private static void check(boolean value,String why){checks++;if(!value)throw new AssertionError(why);}
  private static int edge(int value,float scale,int offset){return offset+Math.round(value*scale);}
  public static int run() {
    checks=0;
    for(float scale:new float[]{.2f,.3f,.41f,.5f,.625f,.75f,1f})for(int ox:new int[]{0,7,83,-21})for(int oy:new int[]{0,5,31,-9}) {
      int left=edge(318,scale,ox),right=edge(562,scale,ox),top=edge(287,scale,oy),bottom=edge(314,scale,oy);
      double y=(top+bottom)/2.0;
      check(AnalyzerMaturityBar056.hit((left+right)/2.0,y,scale,ox,oy),"Track centre not clickable");
      check(AnalyzerMaturityBar056.hit((left+right)/2.0,edge(309,scale,oy),scale,ox,oy),"Expanded touch area not clickable");
      check(!AnalyzerMaturityBar056.hit(edge(315,scale,ox)-1,y,scale,ox,oy),"Captured left label");
      check(!AnalyzerMaturityBar056.hit(edge(565,scale,ox)+1,y,scale,ox,oy),"Captured outside right");
      check(!AnalyzerMaturityBar056.hit(left,edge(286,scale,oy)-1,scale,ox,oy),"Captured care meters above");
      check(!AnalyzerMaturityBar056.hit(left,edge(324,scale,oy),scale,ox,oy),"Captured navigation below");
      for(boolean adult:new boolean[]{false,true})for(int expected:new int[]{0,1,250,500,750,990,1000}) {
        double pointer=left+(right-left)*expected/1000.0;
        check(AnalyzerMaturityBar056.value(pointer,scale,ox,adult)==Math.min(adult?1000:990,expected),"Scaled drag value changed");
      }
      check(AnalyzerMaturityBar056.value(left-10000,scale,ox,false)==0,"Child lower bound");
      check(AnalyzerMaturityBar056.value(right+10000,scale,ox,false)==990,"Child safe endpoint");
      check(AnalyzerMaturityBar056.value(right+10000,scale,ox,true)==1000,"Adult endpoint");
      // Labels and the complete track are tappable; tapping profile footer cannot edit alignment.
      check(AnalyzerAlignmentBar053.hit(edge(479,scale,ox),edge(293,scale,oy),scale,ox,oy),"Alignment end labels not tappable");
      check(!AnalyzerAlignmentBar053.hit(edge(479,scale,ox),edge(301,scale,oy),scale,ox,oy),"Alignment captures footer");
    }
    for(float invalid:new float[]{0,-1,Float.NaN,Float.POSITIVE_INFINITY}) {
      check(!AnalyzerMaturityBar056.hit(450,295,invalid,0,0),"Invalid scale click");
      check(AnalyzerMaturityBar056.value(450,invalid,0,true)==null,"Invalid scale edit");
    }
    for(double invalid:new double[]{Double.NaN,Double.POSITIVE_INFINITY,Double.NEGATIVE_INFINITY}) {
      check(!AnalyzerMaturityBar056.hit(invalid,295,1,0,0),"Nonfinite x captured");
      check(!AnalyzerMaturityBar056.hit(450,invalid,1,0,0),"Nonfinite y captured");
      check(AnalyzerMaturityBar056.value(invalid,1,0,false)==null,"Nonfinite edit");
    }
    check(AnalyzerMaturityBar056.milli(1.2f,true)==1000,"Adult normalization");
    check(AnalyzerMaturityBar056.milli(.6f,true)==500,"Adult half maturity");
    check(AnalyzerMaturityBar056.milli(.99f,false)==990,"Child boundary");
    check(AnalyzerMaturityBar056.milli(-1,false)==0&&AnalyzerMaturityBar056.milli(5,true)==1000,"Display range");
    check(AnalyzerMaturityBar056.milli(Float.NaN,false)==0,"Nonfinite display");
    check(AnalyzerMaturityBar056.marker(0)==318&&AnalyzerMaturityBar056.marker(1000)==562,"Marker endpoints");
    check(AnalyzerMaturityBar056.percent(990).equals("99.0%")&&AnalyzerMaturityBar056.percent(1000).equals("100.0%"),"Responsive percentage label");
    return checks;
  }
  public static void main(String[] args){System.out.println("Analyzer maturity bar056: "+run()+" geometry/input checks passed");}
}
