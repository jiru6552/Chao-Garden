package com.chaogarden.toys;

/** Deterministic choreography boundaries; actual sound and contact need native tests. */
public final class JackPlayRules080Test {
  private static int checks;
  private static void check(boolean b,String why){checks++;if(!b)throw new AssertionError(why);}
  public static void main(String[] args){
    for(float t=-4;t<=164;t+=.125f){
      float p=JackPlayRules080.presence(t);double y=JackPlayRules080.lookHeight(t);
      check(Float.isFinite(p)&&p>=0&&p<=1,"Finite bounded entry/exit");
      check(Double.isFinite(y)&&y>=.4-1e-6&&y<=.83+1e-6,"Gaze follows box then head within bounded height");
      if(t>=0&&t<159.875f)check(Math.abs(p-JackPlayRules080.presence(t+.125f))<.013,"No presence discontinuity");
      check(!JackPlayRules080.startled(t)||t>=40&&t<65,"Startle never anticipates the reveal");
      check(!JackPlayRules080.happy(t)||t>=84&&t<112,"Amusement follows recovery");
    }
    for(float t:new float[]{Float.NaN,Float.NEGATIVE_INFINITY,Float.POSITIVE_INFINITY}){
      check(JackPlayRules080.presence(t)==0&&!JackPlayRules080.startled(t)&&!JackPlayRules080.happy(t),"Invalid clock cannot select expression");
      check(Double.isFinite(JackPlayRules080.lookHeight(t)),"Invalid clock cannot poison gaze");
    }
    check(JackPlayRules080.presence(0)==0&&JackPlayRules080.presence(160)==0,"Quiet episode endpoints");
    check(JackPlayRules080.POP_VOLUME>0&&JackPlayRules080.POP_VOLUME<.3f,"Quiet single reveal cue");
    check(ToyInteractionRules.jackFrame(39)==0&&ToyInteractionRules.jackFrame(40)==1&&ToyInteractionRules.jackFrame(44)==2
        &&ToyInteractionRules.jackFrame(128)==3&&ToyInteractionRules.jackFrame(144)==0,"Preserved single-pop four-stage clock");
    check(ToyInteractionRules.frontOnly(JackPlayRules080.KIND),"Jack retains safe front viewing approach");
    System.out.println("Jack choreography080: "+checks+" checks passed");
  }
}
