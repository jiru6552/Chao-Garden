package com.chaogarden.core;

import java.util.ArrayList;
import java.util.List;

/** Shared, closed quartz portal rims and their real two-sided aperture geometry. */
public final class PortalGeometry0251 {
  public static final int RETURN=0,SCHOOL=1,GARDEN=2,TUNNEL=3,HERO=4,DARK=5;
  public record Point(double x,double y){}
  public record Vertex(double x,double y,double z){}
  public record Quad(Vertex a,Vertex b,Vertex c,Vertex d,int color){}
  private static final List<List<Point>> OPENINGS=List.of(outline(RETURN),outline(SCHOOL),outline(GARDEN),outline(TUNNEL),outline(HERO),outline(DARK));
  private PortalGeometry0251(){}
  public static double width(int style){return switch(style){case TUNNEL,HERO,DARK->3;case SCHOOL->5.6;case GARDEN->4.9;default->2.4;};}
  public static double height(int style){return switch(style){case TUNNEL,HERO,DARK->4;case SCHOOL->6.15;case GARDEN->5.4;default->3.0;};}
  public static boolean hasFrame(int style){checkStyle(style);return style!=TUNNEL&&style!=HERO&&style!=DARK;}
  /** Counterclockwise outline, with a level walk-through floor and rounded crown. */
  public static List<Point> aperture(int style){checkStyle(style);return OPENINGS.get(style);}
  private static List<Point> outline(int style){
    if(style==TUNNEL)return List.of(new Point(-1.5,0),new Point(1.5,0),new Point(1.5,4),new Point(-1.5,4));
    checkStyle(style);double half=width(style)/2,top=height(style),rise=style==RETURN||style==HERO?.68:style==SCHOOL?1.8:1.35,spring=top-rise;
    var points=new ArrayList<Point>();points.add(new Point(-half,0));points.add(new Point(half,0));
    for(int i=0;i<=24;i++){double a=i*Math.PI/24;points.add(new Point(Math.cos(a)*half,spring+Math.sin(a)*rise));}
    return List.copyOf(points);
  }
  public static boolean contains(int style,double x,double y){
    if(!Double.isFinite(x)||!Double.isFinite(y))return false;var outline=aperture(style);
    for(int i=0;i<outline.size();i++){var a=outline.get(i);var b=outline.get((i+1)%outline.size());if((b.x-a.x)*(y-a.y)-(b.y-a.y)*(x-a.x)<-1e-8)return false;}
    return true;
  }
  /** Every rim edge has front, back, inner reveal and outer reveal faces. */
  public static List<Quad> frame(int style){
    if(!hasFrame(style))return List.of();
    var points=aperture(style);var faces=new ArrayList<Quad>();double w=width(style),h=height(style),trim=style==RETURN?.28:.32,z=.24;
    for(int i=0;i<points.size();i++){
      var a=points.get(i);var b=points.get((i+1)%points.size());
      var ao=new Point(a.x*(w+2*trim)/w,(a.y-h/2)*(h+2*trim)/h+h/2);var bo=new Point(b.x*(w+2*trim)/w,(b.y-h/2)*(h+2*trim)/h+h/2);
      faces.add(new Quad(v(a,z),v(b,z),v(bo,z),v(ao,z),0xfff9f2e7));
      faces.add(new Quad(v(ao,-z),v(bo,-z),v(b,-z),v(a,-z),0xffe4e1d9));
      faces.add(new Quad(v(a,-z),v(b,-z),v(b,z),v(a,z),0xffd7d4cc));
      faces.add(new Quad(v(ao,z),v(bo,z),v(bo,-z),v(ao,-z),0xffebe6dc));
    }
    return List.copyOf(faces);
  }
  private static Vertex v(Point p,double z){return new Vertex(p.x,p.y,z);}
  private static void checkStyle(int style){if(style<RETURN||style>DARK)throw new IllegalArgumentException("Unknown Chao portal style");}
}
