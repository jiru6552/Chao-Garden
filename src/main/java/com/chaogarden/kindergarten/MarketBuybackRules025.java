package com.chaogarden.kindergarten;

/** Conservative Minecraft adaptation: only whole inventory lots, with no per-item rounding windfall. */
public final class MarketBuybackRules025 {
  private MarketBuybackRules025(){}
  public static int emeralds(int purchasePrice,int count){
    if(purchasePrice<1||purchasePrice>1152||count<1||count>64)return 0;
    return (int)((long)purchasePrice*count/4);
  }
  public static boolean forbidden(String id){
    return id==null||id.endsWith("_egg")||id.contains("carrier")||id.contains("robot")||id.contains("robo_")||id.equals("omochao_parts_box");
  }
}
