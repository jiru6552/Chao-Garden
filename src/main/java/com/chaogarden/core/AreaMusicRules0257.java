package com.chaogarden.core;

import java.util.Set;

/** Area selection is independent of the client and never invents a substitute soundtrack. */
public final class AreaMusicRules0257 {
  public enum Track {
    NONE(""), LOBBY("lobby"), NEUTRAL("neutral"), HERO("hero"), DARK("dark"),
    KINDERGARTEN("kindergarten"), BLACK_MARKET("black_market");
    public final String id;
    Track(String id){this.id=id;}
    public String event(){return "music.area."+id;}
    public String asset(){return "music/area/"+id;}
  }
  private AreaMusicRules0257(){}
  public static Track region(String dimension,double x,double z){
    if(dimension==null||!Double.isFinite(x)||!Double.isFinite(z))return Track.NONE;
    return switch(dimension){
      case "chao_garden:hub"->Track.LOBBY;
      case "chao_garden:garden"->Track.NEUTRAL;
      case "chao_garden:hero_garden"->Track.HERO;
      case "chao_garden:dark_garden"->Track.DARK;
      // The cupboard's front/lobby does not reveal the hidden destination by
      // changing the music. Only the passage beyond it and the shop qualify.
      case "chao_garden:kindergarten"->
          (x< -8&&z>=3&&z<=7||x< -16&&x>=-26&&z>=0&&z<=10)
              ?Track.BLACK_MARKET:Track.KINDERGARTEN;
      default->Track.NONE;
    };
  }
  public static Track select(String dimension,double x,double z,Set<Track> available){
    var region=region(dimension,x,z);return available.contains(region)?region:Track.NONE;
  }
  public static float fade(float value,float target){
    value=Math.max(0,Math.min(1,value));target=Math.max(0,Math.min(1,target));
    return value<target?Math.min(target,value+.025f):Math.max(target,value-.025f);
  }
}
