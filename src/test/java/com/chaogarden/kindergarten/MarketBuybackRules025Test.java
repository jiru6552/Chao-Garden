package com.chaogarden.kindergarten;

/** Exhaustive whole-lot accounting and hard deny-list boundaries. */
public final class MarketBuybackRules025Test {
  private static int checks;
  private static void check(boolean pass,String why){checks++;if(!pass)throw new AssertionError(why);}
  public static int run(){
    checks=0;
    for(int price=1;price<=1152;price++)for(int count=1;count<=64;count++){
      int pay=MarketBuybackRules025.emeralds(price,count);
      check(pay==(long)price*count/4,"Whole-lot quarter-value rounding");
      check((long)pay*4<=(long)price*count,"Resale never creates value");
      if(count>1)check(pay>=MarketBuybackRules025.emeralds(price,count-1),"Lot quote is monotonic");
    }
    check(MarketBuybackRules025.emeralds(1,1)==0&&MarketBuybackRules025.emeralds(1,3)==0&&MarketBuybackRules025.emeralds(1,4)==1,"Cheap goods need a four-item lot");
    for(int price:new int[]{Integer.MIN_VALUE,-1,0,1153,Integer.MAX_VALUE})check(MarketBuybackRules025.emeralds(price,64)==0,"Reject invalid catalog prices");
    for(int count:new int[]{Integer.MIN_VALUE,-1,0,65,Integer.MAX_VALUE})check(MarketBuybackRules025.emeralds(1152,count)==0,"Reject invalid quantities");
    for(String id:new String[]{"normal_egg","shiny_red_egg","moon_egg","chao_carrier","robot_component","robo_heart","omochao_parts_box"})check(MarketBuybackRules025.forbidden(id),"No sale of living/robot identity items");
    check(MarketBuybackRules025.forbidden(null),"Null catalog key is denied");
    check(!MarketBuybackRules025.forbidden("swim_drive")&&!MarketBuybackRules025.forbidden("round_fruit"),"Plain recognized goods pass only the pure identity filter");
    return checks;
  }
}
