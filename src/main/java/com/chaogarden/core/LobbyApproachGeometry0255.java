package com.chaogarden.core;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Shared curved perimeter treads, complete balustrades and the exact Dark floor opening. */
public final class LobbyApproachGeometry0255 {
  public static final int STEPS=18;
  public static final double INNER_RADIUS=20.5,OUTER_RADIUS=23.5,START_ANGLE=Math.toRadians(-40),END_ANGLE=Math.toRadians(20);
  public static final double TREAD_THICKNESS=.125,RAIL_HEIGHT=1.25,RAIL_HALF_WIDTH=.10;
  public record Point(double x,double y,double z){}
  public record Face(Point a,Point b,Point c,Point d){}
  public record Box(double x0,double y0,double z0,double x1,double y1,double z1){}
  public record Cell(int x,int y,int z){}
  public record Edge(Point a,Point b){}
  private static final List<Point> HERO_LANDING=landingPolygon(true),DARK_LANDING=landingPolygon(false);
  private static final List<List<Point>> HERO_HOLES=footprints(true),DARK_HOLES=footprints(false);
  private static final java.util.Set<Cell> FLOOR_CELLS=floorCells();
  private static final List<Edge> HERO_EDGES=boundary(HERO_HOLES),DARK_EDGES=boundary(DARK_HOLES);
  private static final Map<Cell,List<Box>> COLLISION=collisionCells();
  private LobbyApproachGeometry0255(){}
  public static int landingY(boolean hero){return hero?88:70;}
  public static double angle(int step){return START_ANGLE+(END_ANGLE-START_ANGLE)*step/STEPS;}
  public static double treadTop(boolean hero,int step){return hero?80+.5*(step+1):80-.5*(step+1);}
  /** Canonical travel angle is reflected only for Hero: its entrance is south and its landing north. */
  public static double sceneZ(boolean hero,double z){return hero?-z:z;}
  public static double travelAngle(boolean hero,double x,double z){return Math.atan2(sceneZ(hero,z),(hero?1:-1)*x);}
  public static Point centre(boolean hero,double angle,double y){return new Point((hero?1:-1)*22*Math.cos(angle),y,sceneZ(hero,22*Math.sin(angle)));}
  public static Point walkPoint(boolean hero,double progress){
    double a=START_ANGLE+(END_ANGLE-START_ANGLE)*progress;
    return centre(hero,a,80+(hero?9:-9)*progress);
  }
  private static Point radial(boolean hero,double radius,double angle,double y){return new Point((hero?1:-1)*radius*Math.cos(angle),y,sceneZ(hero,radius*Math.sin(angle)));}
  private static List<Point> sector(boolean hero,double a,double b,double y){return List.of(radial(hero,INNER_RADIUS,a,y),radial(hero,OUTER_RADIUS,a,y),radial(hero,OUTER_RADIUS,b,y),radial(hero,INNER_RADIUS,b,y));}
  private static List<Point> landingPolygon(boolean hero){
    var polygon=new ArrayList<>(List.of(new Point(15,0,6.5),new Point(22.5,0,6.5),new Point(22.5,0,14.5),new Point(15,0,14.5)));
    // The platform follows the same round lobby perimeter; no outer corner
    // penetrates the star-wall or leaves a square projection outside it.
    for(int i=0;i<192;i++){
      double a=i*Math.PI/96,b=(i+1)*Math.PI/96;
      polygon=new ArrayList<>(clip(polygon,new Point(24.5*Math.cos(a),0,24.5*Math.sin(a)),new Point(24.5*Math.cos(b),0,24.5*Math.sin(b)),true));
    }
    for(int i=0;i<polygon.size();i++){var q=polygon.get(i);polygon.set(i,new Point(hero?q.x:-q.x,0,sceneZ(hero,q.z)));}
    return List.copyOf(polygon);
  }
  private static List<List<Point>> footprints(boolean hero){
    var result=new ArrayList<List<Point>>();
    for(int i=0;i<STEPS*4;i++)result.add(sector(hero,START_ANGLE+(END_ANGLE-START_ANGLE)*i/(STEPS*4),START_ANGLE+(END_ANGLE-START_ANGLE)*(i+1)/(STEPS*4),0));
    result.add(hero?HERO_LANDING:DARK_LANDING);return List.copyOf(result);
  }
  public static List<List<Point>> footprintsFor(boolean hero){return hero?HERO_HOLES:DARK_HOLES;}
  private static double cross(Point a,Point b,Point p){return (b.x-a.x)*(p.z-a.z)-(b.z-a.z)*(p.x-a.x);}
  public static boolean inside(List<Point> polygon,double x,double z){
    var p=new Point(x,0,z);double sign=0;
    for(int i=0;i<polygon.size();i++){double c=cross(polygon.get(i),polygon.get((i+1)%polygon.size()),p);if(Math.abs(c)<1e-8)continue;if(sign==0)sign=Math.signum(c);else if(c*sign<0)return false;}
    return polygon.size()>=3;
  }
  public static boolean footprint(boolean hero,double x,double z){
    for(var polygon:footprintsFor(hero))if(inside(polygon,x,z))return true;return false;
  }
  public static boolean landing(boolean hero,double sceneX,double sceneZ){return inside(hero?HERO_LANDING:DARK_LANDING,sceneX,sceneZ);}
  public static double surface(boolean hero,double x,double z){
    if(landing(hero,x,z))return hero?89:71;
    double a=travelAngle(hero,x,z),u=(a-START_ANGLE)/(END_ANGLE-START_ANGLE);
    if(u< -1e-8||u>1+1e-8||Math.hypot(x,z)<INNER_RADIUS-1e-8||Math.hypot(x,z)>OUTER_RADIUS+1e-8)return Double.NaN;
    return treadTop(hero,Math.min(STEPS-1,Math.max(0,(int)Math.floor(u*STEPS))));
  }
  private static List<Point> clip(List<Point> polygon,Point a,Point b,boolean positive){
    var result=new ArrayList<Point>();if(polygon.isEmpty())return result;
    var previous=polygon.getLast();double pc=cross(a,b,previous);boolean was=positive?pc>=-1e-10:pc<=1e-10;
    for(var q:polygon){double qc=cross(a,b,q);boolean now=positive?qc>=-1e-10:qc<=1e-10;
      if(now!=was){double t=pc/(pc-qc);result.add(new Point(previous.x+(q.x-previous.x)*t,previous.y+(q.y-previous.y)*t,previous.z+(q.z-previous.z)*t));}
      if(now)result.add(q);previous=q;pc=qc;was=now;
    }return List.copyOf(result);
  }
  /** Subtraction of the union's finite convex pieces also clips all floor grid lines. */
  public static List<List<Point>> floorPieces(List<Point> polygon){
    List<List<Point>> pieces=List.of(polygon);
    for(var raw:DARK_HOLES){
      var hole=new ArrayList<>(raw);if(signedArea(hole)<0)java.util.Collections.reverse(hole);
      var next=new ArrayList<List<Point>>();
      for(var piece:pieces){var middle=piece;for(int i=0;i<hole.size()&&!middle.isEmpty();i++){
        var a=hole.get(i);var b=hole.get((i+1)%hole.size());var outside=clip(middle,a,b,false);
        if(outside.size()>=3&&Math.abs(signedArea(outside))>1e-9)next.add(outside);middle=clip(middle,a,b,true);
      }}pieces=List.copyOf(next);
    }return pieces;
  }
  private static double signedArea(List<Point> polygon){double v=0;for(int i=0;i<polygon.size();i++){var a=polygon.get(i);var b=polygon.get((i+1)%polygon.size());v+=a.x*b.z-b.x*a.z;}return v/2;}
  /** A main-floor cell touching the cut-out is supplied by the finely clipped native shape. */
  public static boolean floorOpening(int worldX,int worldZ){return FLOOR_CELLS.contains(new Cell(worldX,79,worldZ));}
  private static java.util.Set<Cell> floorCells(){var result=new java.util.HashSet<Cell>();for(int x=-26;x<=26;x++)for(int z=-26;z<=26;z++)if(touchesOpening(x,z))result.add(new Cell(x,79,z));return java.util.Set.copyOf(result);}
  private static boolean touchesOpening(int worldX,int worldZ){
    double x0=worldX-.5,z0=worldZ-.5;
    for(var polygon:DARK_HOLES){var p=polygon;for(var edge:List.of(new Edge(new Point(x0,0,z0),new Point(x0+1,0,z0)),new Edge(new Point(x0+1,0,z0),new Point(x0+1,0,z0+1)),new Edge(new Point(x0+1,0,z0+1),new Point(x0,0,z0+1)),new Edge(new Point(x0,0,z0+1),new Point(x0,0,z0))))p=clip(p,edge.a,edge.b,true);
      if(p.size()>=3&&Math.abs(signedArea(p))>1e-8)return true;
    }return false;
  }
  private static Point mix(Point a,Point b,double t){return new Point(a.x+(b.x-a.x)*t,0,a.z+(b.z-a.z)*t);}
  private static List<Edge> boundary(List<List<Point>> polygons){
    var result=new ArrayList<Edge>();
    for(int owner=0;owner<polygons.size();owner++){var polygon=polygons.get(owner);
      for(int j=0;j<polygon.size();j++){var a=polygon.get(j);var b=polygon.get((j+1)%polygon.size());var cuts=new ArrayList<Double>(List.of(0d,1d));
        for(var other:polygons)for(int k=0;k<other.size();k++){
          var c=other.get(k);var d=other.get((k+1)%other.size());double dx=b.x-a.x,dz=b.z-a.z,ex=d.x-c.x,ez=d.z-c.z,den=dx*ez-dz*ex;
          if(Math.abs(den)<1e-10)continue;double t=((c.x-a.x)*ez-(c.z-a.z)*ex)/den,u=((c.x-a.x)*dz-(c.z-a.z)*dx)/den;
          if(t>1e-8&&t<1-1e-8&&u>=-1e-8&&u<=1+1e-8)cuts.add(t);
        }cuts.sort(Double::compare);
        for(int k=0;k<cuts.size()-1;k++){double lo=cuts.get(k),hi=cuts.get(k+1);if(hi-lo<1e-8)continue;var middle=mix(a,b,(lo+hi)/2);boolean hidden=false;
          for(int n=0;n<polygons.size();n++)if(n!=owner&&inside(polygons.get(n),middle.x,middle.z)){hidden=true;break;}
          if(!hidden)result.add(new Edge(mix(a,b,lo),mix(a,b,hi)));
        }
      }
    }return List.copyOf(result);
  }
  public static List<Edge> boundaryFor(boolean hero){return hero?HERO_EDGES:DARK_EDGES;}
  public static boolean entryEdge(boolean hero,Edge edge){
    double a=travelAngle(hero,edge.a.x,edge.a.z),b=travelAngle(hero,edge.b.x,edge.b.z);
    return Math.abs(a-START_ANGLE)<1e-6&&Math.abs(b-START_ANGLE)<1e-6;
  }
  private static double railGround(boolean hero,Point point){
    if(landing(hero,point.x,point.z))return hero?89:71;
    double u=(travelAngle(hero,point.x,point.z)-START_ANGLE)/(END_ANGLE-START_ANGLE);
    return 80+(hero?9:-9)*Math.max(0,Math.min(1,u));
  }
  public record Rail(Edge edge,double y0,double y1,double skirt){}
  public static List<Rail> rails(boolean hero,boolean upper){
    var result=new ArrayList<Rail>();for(var edge:boundaryFor(hero))if(!entryEdge(hero,edge))result.add(new Rail(edge,upper?80:railGround(hero,edge.a),upper?80:railGround(hero,edge.b),upper||hero?.12:.65));
    return List.copyOf(result);
  }
  private static double distance(Edge e,double x,double z){double dx=e.b.x-e.a.x,dz=e.b.z-e.a.z;double t=Math.max(0,Math.min(1,((x-e.a.x)*dx+(z-e.a.z)*dz)/(dx*dx+dz*dz)));return Math.hypot(x-e.a.x-t*dx,z-e.a.z-t*dz);}
  private static void add(Map<Cell,List<Box>> cells,double x,double bottom,double z,double top){
    if(top-bottom<1e-8)return;int wx=(int)Math.floor(x+.5),wz=(int)Math.floor(z+.5);
    for(int y=(int)Math.floor(bottom+1e-8);y<Math.ceil(top-1e-8);y++){
      double y0=Math.max(bottom,y),y1=Math.min(top,y+1);if(y1-y0<1e-8)continue;
      cells.computeIfAbsent(new Cell(wx,y,wz),k->new ArrayList<>()).add(new Box(x+.5-wx,y0-y,z+.5-wz,x+.625-wx,y1-y,z+.625-wz));
    }
  }
  private static Map<Cell,List<Box>> collisionCells(){
    var result=new LinkedHashMap<Cell,List<Box>>();
    var heroRails=rails(true,false);var darkRails=rails(false,false);var upperRails=rails(false,true);
    // A 1/8-block horizontal grid gives subpixel-to-model native fidelity while
    // each tread remains only 1/8 block thick. There is no old glass foundation.
    for(int ix=-200;ix<200;ix++)for(int iz=-144;iz<128;iz++){
      double x=ix*.125,z=iz*.125,cx=x+.0625,cz=z+.0625;
      for(boolean hero:new boolean[]{true,false}){
        if(footprint(hero,cx,cz)){double top=surface(hero,cx,cz);if(Double.isFinite(top))add(result,x,top-TREAD_THICKNESS,z,top);}
        for(var rail:hero?heroRails:darkRails)if(distance(rail.edge,cx,cz)<=RAIL_HALF_WIDTH){
          double dx=rail.edge.b.x-rail.edge.a.x,dz=rail.edge.b.z-rail.edge.a.z,d=dx*dx+dz*dz,t=d<1e-12?0:Math.max(0,Math.min(1,((cx-rail.edge.a.x)*dx+(cz-rail.edge.a.z)*dz)/d));
          double ground=rail.y0+(rail.y1-rail.y0)*t;add(result,x,ground-rail.skirt,z,ground+RAIL_HEIGHT);
        }
      }
      for(var rail:upperRails)if(distance(rail.edge,cx,cz)<=RAIL_HALF_WIDTH)add(result,x,80-rail.skirt,z,80+RAIL_HEIGHT);
      int wx=(int)Math.floor(x+.5),wz=(int)Math.floor(z+.5);
      if(Math.hypot(wx,wz)<=25&&floorOpening(wx,wz)&&!footprint(false,cx,cz))add(result,x,79.875,z,80);
    }
    var frozen=new LinkedHashMap<Cell,List<Box>>();for(var entry:result.entrySet())frozen.put(entry.getKey(),List.copyOf(entry.getValue()));return java.util.Collections.unmodifiableMap(frozen);
  }
  public static Map<Cell,List<Box>> nativeCells(){return COLLISION;}
  public static List<Box> nativeBoxes(int x,int y,int z){return COLLISION.getOrDefault(new Cell(x,y,z),List.of());}
  private static Face face(Point a,Point b,Point c,Point d){return new Face(a,b,c,d);}
  public static List<Face> stairFaces(boolean hero){
    var result=new ArrayList<Face>();
    for(int step=0;step<STEPS;step++)for(int part=0;part<4;part++){
      double a=angle(step)+(angle(step+1)-angle(step))*part/4,b=angle(step)+(angle(step+1)-angle(step))*(part+1)/4,top=treadTop(hero,step)-80;
      var q=sector(hero,a,b,top);var bottom=sector(hero,a,b,top-TREAD_THICKNESS);
      result.add(face(q.get(0),q.get(1),q.get(2),q.get(3)));result.add(face(bottom.get(3),bottom.get(2),bottom.get(1),bottom.get(0)));
      result.add(face(q.get(0),q.get(3),bottom.get(3),bottom.get(0)));result.add(face(q.get(2),q.get(1),bottom.get(1),bottom.get(2)));
      if(part==0)result.add(face(q.get(1),q.get(0),bottom.get(0),bottom.get(1)));
      if(part==3)result.add(face(q.get(3),q.get(2),bottom.get(2),bottom.get(3)));
    }
    var q=hero?HERO_LANDING:DARK_LANDING;double top=(hero?89:71)-80,bottom=top-TREAD_THICKNESS;
    for(int i=1;i<q.size()-1;i++){
      result.add(face(new Point(q.get(0).x,top,q.get(0).z),new Point(q.get(i).x,top,q.get(i).z),new Point(q.get(i+1).x,top,q.get(i+1).z),new Point(q.get(i+1).x,top,q.get(i+1).z)));
      result.add(face(new Point(q.get(i+1).x,bottom,q.get(i+1).z),new Point(q.get(i).x,bottom,q.get(i).z),new Point(q.get(0).x,bottom,q.get(0).z),new Point(q.get(0).x,bottom,q.get(0).z)));
    }
    for(int i=0;i<q.size();i++){var a=q.get(i);var b=q.get((i+1)%q.size());result.add(face(new Point(a.x,top,a.z),new Point(b.x,top,b.z),new Point(b.x,bottom,b.z),new Point(a.x,bottom,a.z)));}
    return List.copyOf(result);
  }
}
