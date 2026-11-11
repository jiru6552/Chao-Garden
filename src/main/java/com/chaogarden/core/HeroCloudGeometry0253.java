package com.chaogarden.core;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/** Low cloud sea below the floating Hero lawn; no ceiling or player collision. */
public final class HeroCloudGeometry0253 {
  public record Vertex(float x,float y,float z,int color){}
  private static final List<Vertex> MESH=build();
  private HeroCloudGeometry0253(){}
  public static List<Vertex> vertices(){return MESH;}
  private static void vertex(List<Vertex> out,double x,double y,double z,int color){out.add(new Vertex((float)x,(float)y,(float)z,color));}
  private static void quad(List<Vertex> out,double[] a,double[] b,double[] c,double[] d,int color){
    vertex(out,a[0],a[1],a[2],color);vertex(out,b[0],b[1],b[2],color);vertex(out,c[0],c[1],c[2],color);vertex(out,d[0],d[1],d[2],color);
  }
  private static double[] point(double x,double y,double z,double rx,double ry,double rz,double lat,double lon){
    return new double[]{x+rx*Math.sin(lat)*Math.cos(lon),y+ry*Math.cos(lat),z+rz*Math.sin(lat)*Math.sin(lon)};
  }
  private static int shade(double brightness){
    int r=(int)Math.round(255*brightness),g=(int)Math.round(190*brightness),b=(int)Math.round(218*brightness);
    return 0xff000000|(r<<16)|(g<<8)|b;
  }
  private static List<Vertex> build(){
    var out=new ArrayList<Vertex>();
    // The low distant sheet prevents a void slit between the furthest puffs and
    // biome fog. It stays seventeen metres below the playable grass surface.
    quad(out,new double[]{-2048,62,-2048},new double[]{2048,62,-2048},new double[]{2048,62,2048},new double[]{-2048,62,2048},0xffe7a7ca);
    var random=new Random(253);
    for(int ix=-6;ix<=6;ix++)for(int iz=-6;iz<=6;iz++){
      double x=ix*32+random.nextDouble()*10-5,z=iz*32+random.nextDouble()*10-5;
      double y=66+random.nextDouble()*3,rx=23+random.nextDouble()*7,rz=23+random.nextDouble()*7,ry=5+random.nextDouble()*3;
      for(int j=0;j<6;j++)for(int i=0;i<12;i++){
        double lat=j*Math.PI/6,next=(j+1)*Math.PI/6,lon=i*Math.PI/6,last=(i+1)*Math.PI/6;
        double brightness=.82+.15*Math.max(0,Math.cos(lat))+.025*Math.cos(lon-.7);
        quad(out,point(x,y,z,rx,ry,rz,lat,lon),point(x,y,z,rx,ry,rz,lat,last),
            point(x,y,z,rx,ry,rz,next,last),point(x,y,z,rx,ry,rz,next,lon),shade(brightness));
      }
    }
    return List.copyOf(out);
  }
}
