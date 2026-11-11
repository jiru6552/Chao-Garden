package com.chaogarden.core;

/** Actual compact play-city model bounds, in native model pixels. */
public final class ClassroomToyGeometry0258 {
  private ClassroomToyGeometry0258(){}
  public static double[] bounds(String id){return switch(id){
    case "school_toy_house_red","school_toy_house_blue","school_toy_house_yellow"->new double[]{0,0,.5,16,28,15.5};
    case "school_toy_tower","school_toy_tree"->new double[]{1,0,1,15,30,15};
    case "school_toy_ball"->new double[]{1,0,1,15,14,15};
    default->throw new IllegalArgumentException("Unknown classroom toy: "+id);
  };}
}
