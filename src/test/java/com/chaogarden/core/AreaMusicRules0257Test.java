package com.chaogarden.core;

import java.util.EnumSet;
import java.util.Set;
import static com.chaogarden.core.AreaMusicRules0257.*;

/** Physical area boundaries, unavailable assets, and bounded transitions. */
public final class AreaMusicRules0257Test {
  private static int checks;
  private static void check(boolean value,String message){checks++;if(!value)throw new AssertionError(message);}
  public static void main(String[]args){checks=0;
    var all=EnumSet.allOf(Track.class);all.remove(Track.NONE);
    String[] realms={"hub","garden","hero_garden","dark_garden","kindergarten"};
    Track[] tracks={Track.LOBBY,Track.NEUTRAL,Track.HERO,Track.DARK,Track.KINDERGARTEN};
    for(int i=0;i<realms.length;i++){
      check(select("chao_garden:"+realms[i],0,0,all)==tracks[i],"Wrong area soundtrack");
      check(select("chao_garden:"+realms[i],0,0,Set.of())==Track.NONE,"Missing recording suppresses vanilla music");
    }
    check(select("chao_garden:garden",0,0,Set.of(Track.NEUTRAL))==Track.NEUTRAL,"Bundled user recording is unreachable");
    for(var realm: new String[]{"minecraft:overworld","minecraft:the_nether","minecraft:the_end","other:hub"})
      check(region(realm,0,0)==Track.NONE,"Area music leaked outside authored realms");
    check(region(null,0,0)==Track.NONE&&region("chao_garden:hub",Double.NaN,0)==Track.NONE,"Malformed realm/body accepted");
    for(double x:new double[]{-5.5,-6,-6.5,-7,-8})
      check(region("chao_garden:kindergarten",x,5)==Track.KINDERGARTEN,"Cupboard exterior reveals hidden shop with music");
    for(double x:new double[]{-8.1,-10,-15,-18,-22})
      check(region("chao_garden:kindergarten",x,5)==Track.BLACK_MARKET,"Hidden passage does not select shop soundtrack");
    check(region("chao_garden:kindergarten",-22,1)==Track.BLACK_MARKET&&region("chao_garden:kindergarten",-22,9)==Track.BLACK_MARKET,"Shop corners use another room soundtrack");
    for(double[] p:new double[][]{{-18,-15},{18,-15},{0,-21},{22,5},{0,40}})
      check(region("chao_garden:kindergarten",p[0],p[1])==Track.KINDERGARTEN,"A public school room selected the shop theme");
    check(select("chao_garden:kindergarten",-22,5,Set.of(Track.KINDERGARTEN))==Track.NONE,"Missing shop theme silently substitutes school music");
    float volume=0;
    for(int i=0;i<50;i++){float next=fade(volume,.65f);check(next>=volume&&next<=.65f&&next-volume<=.02501f,"Fade-in jumps or overshoots");volume=next;}
    check(Math.abs(volume-.65f)<.0001,"Fade-in never reaches music level");
    for(int i=0;i<50;i++){float next=fade(volume,0);check(next<=volume&&next>=0&&volume-next<=.02501f,"Fade-out jumps or goes negative");volume=next;}
    check(volume==0,"Fade-out leaves a live audible tail");
    check(fade(2,2)==1&&fade(-2,-2)==0,"Invalid levels escape music volume bounds");
    System.out.println("Area music rules0257: "+checks+" checks passed");
  }
}
