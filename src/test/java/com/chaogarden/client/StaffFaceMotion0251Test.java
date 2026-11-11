package com.chaogarden.client;

/** Permanently closed professions must never pass through an ordinary blink pose. */
public final class StaffFaceMotion0251Test {
  private static int checks;
  private static void check(boolean ok,String label){checks++;if(!ok)throw new AssertionError(label);}
  public static int run(){
    checks=0;long[] seeds={0,1,150,151,-1,Long.MIN_VALUE,Long.MAX_VALUE,
      java.util.UUID.nameUUIDFromBytes("chao_garden:kindergarten:FORTUNE".getBytes(java.nio.charset.StandardCharsets.UTF_8)).getLeastSignificantBits()};
    for(long seed:seeds){
      int ordinaryBlinks=0;
      for(int step=0;step<=173*16*4;step++){
        float age=step/4f;
        check(StaffFaceMotion0251.eyelidScale(true,age,seed)==1,"Fortune lid changed over sixteen former blink periods");
        float ordinary=StaffFaceMotion0251.eyelidScale(false,age,seed);
        check(Float.isFinite(ordinary)&&ordinary>=.08f&&ordinary<=1,"Ordinary staff blink range");
        if(ordinary<1)ordinaryBlinks++;
      }
      check(ordinaryBlinks>0,"Other staff lost their occasional blink");
      float oldPeak=170.5f-Math.floorMod(seed,151);if(oldPeak<0)oldPeak+=173;
      check(StaffFaceMotion0251.eyelidScale(false,oldPeak,seed)==.08f,"Old ordinary blink peak changed");
      check(StaffFaceMotion0251.eyelidScale(true,oldPeak,seed)==1,"Fortune adopted the old blink peak");
    }
    for(float age:new float[]{Float.NaN,Float.NEGATIVE_INFINITY,Float.POSITIVE_INFINITY})
      check(StaffFaceMotion0251.eyelidScale(true,age,0)==1,"Invalid frame time hid authored closed lids");
    return checks;
  }
  public static void main(String[] args){System.out.println("Staff face timing0251: "+run()+" checks passed");}
}
