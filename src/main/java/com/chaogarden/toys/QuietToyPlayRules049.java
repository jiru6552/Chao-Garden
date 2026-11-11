package com.chaogarden.toys;

/** Authored 160-tick quiet-toy sessions; no inventory or terrain mutation. */
public final class QuietToyPlayRules049 {
  public static final String BROOM="chao_broom",BOOK="chao_picture_book",CRAYONS="chao_crayons";
  private QuietToyPlayRules049(){}
  public static boolean supports(String kind){return kind.equals(BROOM)||kind.equals(BOOK)||kind.equals(CRAYONS);}
  public static int index(String kind){return kind.equals(BROOM)?0:kind.equals(BOOK)?1:kind.equals(CRAYONS)?2:-1;}
  public static float presence(float tick){return MaracasPlayRules.presence(tick);}
  public static boolean happy(String kind,float tick){return supports(kind)&&tick>=16&&tick<148;}
  public static boolean soundDue(String kind,int tick){
    return kind.equals(BROOM)&&tick>=24&&tick<=136&&(tick-24)%16==0;
  }
  public static String soundKey(String kind){return kind.equals(BROOM)?"toy.broom":kind.equals(BOOK)?"toy.book_page":"toy.crayon";}
  public static float volume(String kind){return kind.equals(BOOK)?.14f:.18f;}
  public static int stroke(float tick){return Math.clamp((int)((tick-24)/16),0,7);}
  public static int inkStage(float tick){return tick<34?0:Math.clamp((int)((tick-34)/16)+1,0,8);}
  public static boolean turning(float tick){return tick>=44&&tick<78||tick>=108&&tick<142;}
}
