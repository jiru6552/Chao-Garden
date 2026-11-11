package com.chaogarden.core;

import java.util.ArrayList;
import java.util.List;

/** Physical stepped arch leaves. Coordinates are model pixels in each owning voxel. */
public final class SchoolDoorGeometry0254 {
  private SchoolDoorGeometry0254() {}
  public static final double LEFT_HINGE=-13.5, RIGHT_HINGE=29.5, WIDTH=21.5;
  public static final double ART_MARGIN=.75;
  // Global X bands and their clearance-adjusted top, measured from the door sill.
  private static final double[][] PROFILE={
      {-13.5,-11,29.75},{-11,-6,36.75},{-6,0,41.75},
      {0,16,45.75},{16,22,41.75},{22,27,36.75},{27,29.5,29.75}};
  public record Box(double x0,double y0,double z0,double x1,double y1,double z1,boolean left) {}

  /** section: 0 leaf, 1 center, 2 tip. No mesh or selection exists in inactive cells. */
  public static List<Box> boxes(boolean left,boolean open,int section,int segment) {
    List<Box> out=new ArrayList<>();
    if((open&&section==1)||(!open&&section==2))return out;
    for(boolean physicalLeft:new boolean[]{true,false}) {
      if(section!=1&&physicalLeft!=left)continue;
      for(double[] stripe:PROFILE) {
        double x0=Math.max(stripe[0],physicalLeft?LEFT_HINGE:8);
        double x1=Math.min(stripe[1],physicalLeft?8:RIGHT_HINGE);
        double y1=Math.min(16,stripe[2]-segment*16);
        if(x0>=x1||y1<=0)continue;
        if(open) {
          double d0=physicalLeft?x0-LEFT_HINGE:RIGHT_HINGE-x1;
          double d1=physicalLeft?x1-LEFT_HINGE:RIGHT_HINGE-x0;
          double z0=Math.max(0,8+d0-(section==2?16:0));
          double z1=Math.min(16,8+d1-(section==2?16:0));
          if(z0<z1)out.add(new Box(physicalLeft?1.5:12.5,0,z0,physicalLeft?3.5:14.5,y1,z1,physicalLeft));
        } else {
          double offset=section==1?0:physicalLeft?-16:16;
          double a=Math.max(0,x0-offset),b=Math.min(16,x1-offset);
          if(a<b)out.add(new Box(a,0,7,b,y1,9,physicalLeft));
        }
      }
    }
    return out;
  }

  public static double heightAtDistance(boolean left,double distance) {
    double x=left?LEFT_HINGE+distance:RIGHT_HINGE-distance;
    for(double[] stripe:PROFILE)if(x>=stripe[0]&&x<=stripe[1])return stripe[2];
    return 0;
  }
}
