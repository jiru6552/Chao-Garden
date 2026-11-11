package com.chaogarden.core;

import java.util.ArrayList;
import java.util.List;

/** Shared native gate geometry, including the actual pot opening and plant paths. */
public final class LobbyGeometry0252 {
  public static final double PLINTH_BOTTOM=0,PLINTH_TOP=.34,STATUE_PLATFORM_TOP=.55;
  public static final double URN_LIP=.75,URN_OPENING_RADIUS=.35,URN_RIM_RADIUS=.46,SOIL_Y=.50;
  private static final int SIDES=16;
  public record Point(double x,double y,double z){}
  public record Face(Point a,Point b,Point c,Point d,double shade){}
  public record Stem(Point root,Point bloom){}
  private LobbyGeometry0252(){}
  private static Point point(double x,double y,double z){return new Point(x,y,z);}
  private static Face face(Point a,Point b,Point c,Point d,double tone){return new Face(a,b,c,d,tone);}

  /** A closed three-dimensional teardrop, with a single apex rather than an extruded ridge. */
  public static List<Face> statueHead(double x,double y,double z){
    double[] heights={-.48,-.40,-.22,0,.20,.37,.64},radii={0,.62,.92,1,.80,.48,0};
    var faces=new ArrayList<Face>();
    for(int j=0;j+1<heights.length;j++)for(int i=0;i<SIDES;i++){
      double a=i*Math.PI*2/SIDES,b=(i+1)*Math.PI*2/SIDES;
      faces.add(face(headPoint(x,y,z,a,heights[j],radii[j]),headPoint(x,y,z,a,heights[j+1],radii[j+1]),
          headPoint(x,y,z,b,heights[j+1],radii[j+1]),headPoint(x,y,z,b,heights[j],radii[j]),.84+.13*Math.cos(a-.7)));
    }
    return List.copyOf(faces);
  }
  private static Point headPoint(double x,double y,double z,double angle,double h,double radius){
    double lean=.04*Math.max(0,h)/.64;
    return point(x+lean+Math.cos(angle)*radius*.51,y+h,z+Math.sin(angle)*radius*.43);
  }

  /** Closed bowl shell: outside, annular lip and visible inner wall; no filled top disk. */
  public static List<Face> urn(double x,double base,double z,double size){
    double[][] profile={{0,0},{0,.22},{.06,.23},{.15,.36},{.32,.50},{.48,.52},{.62,.42},
        {.69,URN_RIM_RADIUS},{URN_LIP,URN_RIM_RADIUS},{URN_LIP,URN_OPENING_RADIUS},
        {.65,.35},{.51,.29},{.44,.15},{.43,0}};
    var faces=new ArrayList<Face>();
    for(int j=0;j+1<profile.length;j++)for(int i=0;i<SIDES;i++){
      double a=i*Math.PI*2/SIDES,b=(i+1)*Math.PI*2/SIDES;
      faces.add(face(polar(x,base,z,size,profile[j],a),polar(x,base,z,size,profile[j+1],a),
          polar(x,base,z,size,profile[j+1],b),polar(x,base,z,size,profile[j],b),
          j>=9?.63:.82+.14*Math.cos(a-.7)));
    }
    return List.copyOf(faces);
  }
  private static Point polar(double x,double y,double z,double size,double[] p,double a){
    return point(x+Math.cos(a)*p[1]*size,y+p[0]*size,z+Math.sin(a)*p[1]*size);
  }

  /** Leaves rise through the empty opening before arching outside and drooping past the rim. */
  public static List<Face> leaves(double x,double base,double z,double size){
    var faces=new ArrayList<Face>();
    double[][] profile={{0,SOIL_Y+.012,.015},{.22,.92,.10},{.58,1.0,.18},{1,.63,0}};
    for(int i=0;i<7;i++){
      double a=i*Math.PI*2/7;
      for(int j=0;j+1<profile.length;j++){
        faces.add(face(leafPoint(x,base,z,size,a,profile[j],1),leafPoint(x,base,z,size,a,profile[j+1],1),
            leafPoint(x,base,z,size,a,profile[j+1],-1),leafPoint(x,base,z,size,a,profile[j],-1),i%2==0?.88:1));
      }
    }
    return List.copyOf(faces);
  }
  private static Point leafPoint(double x,double y,double z,double size,double a,double[] row,int side){
    double dx=Math.cos(a),dz=Math.sin(a),width=row[2]*side;
    return point(x+(dx*row[0]-dz*width)*size,y+row[1]*size,z+(dz*row[0]+dx*width)*size);
  }
  /** Every visible blossom has a continuous green stalk rooted in the soil. */
  public static List<Stem> flowers(double x,double base,double z,double size){
    var stems=new ArrayList<Stem>();
    for(int i=0;i<5;i++){
      double a=i*Math.PI*2/5;
      stems.add(new Stem(point(x+Math.cos(a)*.06*size,base+(SOIL_Y+.01)*size,z+Math.sin(a)*.06*size),
          point(x+Math.cos(a)*.26*size,base+(1.22+.08*(i%2))*size,z+Math.sin(a)*.26*size)));
    }
    return List.copyOf(stems);
  }
}
