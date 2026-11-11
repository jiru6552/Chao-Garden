package com.chaogarden.core;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/** Open undersides, duplicate sheets and cracked rim seams must fail the mesh gate. */
public final class PortalGeometry0251Test {
  private static int checks;
  private static void check(boolean condition,String message){checks++;if(!condition)throw new AssertionError(message);}
  private static String point(PortalGeometry0251.Vertex v){return v.x()+","+v.y()+","+v.z();}
  private static boolean closed(List<PortalGeometry0251.Quad> mesh){
    var edges=new HashMap<String,Integer>();
    for(var q:mesh){var vs=List.of(q.a(),q.b(),q.c(),q.d());for(int i=0;i<4;i++){
      String a=point(vs.get(i)),b=point(vs.get((i+1)%4));if(a.equals(b))return false;
      String key=a.compareTo(b)<0?a+"|"+b:b+"|"+a;edges.merge(key,1,Integer::sum);
    }}
    return !edges.isEmpty()&&edges.values().stream().allMatch(n->n==2);
  }
  public static int run(){checks=0;
    for(int style=0;style<=2;style++){
      var mesh=PortalGeometry0251.frame(style);check(closed(mesh),"Quartz rim has an open underside/reveal");
      var outline=PortalGeometry0251.aperture(style);double area=0;
      for(int i=0;i<outline.size();i++){var a=outline.get(i);var b=outline.get((i+1)%outline.size());area+=a.x()*b.y()-a.y()*b.x();
        check(Double.isFinite(a.x())&&Double.isFinite(a.y()),"Portal outline is nonfinite");
        check(PortalGeometry0251.contains(style,a.x(),a.y()),"Real aperture point rejected by shared trigger geometry");
      }
      check(area>0,"Portal winding must face one real side; renderer emits its reverse too");
      for(double y=0;y<=1.8;y+=.1)for(double x=-.6;x<=.6;x+=.1)check(PortalGeometry0251.contains(style,x,y),"Full standing player center corridor is obstructed");
      check(!PortalGeometry0251.contains(style,0,-.01)&&!PortalGeometry0251.contains(style,0,PortalGeometry0251.height(style)+.01),"Portal allows travel beyond visible vertical aperture");
      check(!PortalGeometry0251.contains(style,PortalGeometry0251.width(style)/2+.01,.5),"Portal allows travel beside quartz frame");
      check(!PortalGeometry0251.contains(style,Double.NaN,1)&&!PortalGeometry0251.contains(style,0,Double.POSITIVE_INFINITY),"Nonfinite travel input accepted");
      for(int i=0;i<mesh.size();i++){var open=new ArrayList<>(mesh);open.remove(i);check(!closed(open),"Missing frame face not detected");}
      var duplicate=new ArrayList<>(mesh);duplicate.add(mesh.getFirst());check(!closed(duplicate),"Duplicate frame sheet not detected");
      var cracked=new ArrayList<>(mesh);var q=cracked.getFirst();cracked.set(0,new PortalGeometry0251.Quad(new PortalGeometry0251.Vertex(q.a().x()+.001,q.a().y(),q.a().z()),q.b(),q.c(),q.d(),q.color()));
      check(!closed(cracked),"Rim seam crack not detected");
    }
    int tunnel=PortalGeometry0251.TUNNEL;
    check(PortalGeometry0251.width(tunnel)==3&&PortalGeometry0251.height(tunnel)==4,"Tunnel portal must remain exactly3x4");
    check(!PortalGeometry0251.hasFrame(tunnel)&&PortalGeometry0251.frame(tunnel).isEmpty(),"Tunnel acquired unwanted quartz framing");
    check(PortalGeometry0251.aperture(tunnel).size()==4,"Tunnel aperture is not rectangular");
    for(double x:new double[]{-1.5,0,1.5})for(double y:new double[]{0,3,4})check(PortalGeometry0251.contains(tunnel,x,y),"Rectangular tunnel edge clipped");
    check(!PortalGeometry0251.contains(tunnel,1.5001,2)&&!PortalGeometry0251.contains(tunnel,0,4.0001),"Tunnel trigger exceeds its actual rectangle");
    for(int style:new int[]{PortalGeometry0251.HERO,PortalGeometry0251.DARK}){
      check(PortalGeometry0251.width(style)==3&&PortalGeometry0251.height(style)==4,"Aligned garden gate aperture changed");
      check(!PortalGeometry0251.hasFrame(style)&&PortalGeometry0251.frame(style).isEmpty(),"Dedicated Hero/Dark gateway acquired a second quartz rim");
      check(PortalGeometry0251.aperture(style).size()>20,"Garden gateway crown is not curved");
      check(PortalGeometry0251.contains(style,0,4)&&PortalGeometry0251.contains(style,1.49,2.5),"Gate rejects clear entry/crown");
      check(!PortalGeometry0251.contains(style,1.49,3.9)&&!PortalGeometry0251.contains(style,1.51,1),"Curved gate admits traffic through solid ornamental corners");
    }
    boolean rejected=false;try{PortalGeometry0251.aperture(6);}catch(IllegalArgumentException expected){rejected=true;}check(rejected,"Unknown portal style accepted");
    return checks;
  }
  public static void main(String[] args){System.out.println("Portal geometry: "+run()+" checks passed");}
}
