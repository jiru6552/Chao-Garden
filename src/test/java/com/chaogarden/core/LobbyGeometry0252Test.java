package com.chaogarden.core;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import static com.chaogarden.core.LobbyGeometry0252.*;

/** Solid support, pointed head, open pot and real plant-clearance regressions. */
public final class LobbyGeometry0252Test {
  private static int checks;
  private static void check(boolean ok,String message){checks++;if(!ok)throw new AssertionError(message);}
  private static String key(Point p){return Math.round(p.x()*1e8)+","+Math.round(p.y()*1e8)+","+Math.round(p.z()*1e8);}
  private static List<Point> points(Face f){return List.of(f.a(),f.b(),f.c(),f.d());}
  private static boolean closed(List<Face> mesh){
    var edges=new HashMap<String,Integer>();
    for(var face:mesh){
      var vertices=new ArrayList<String>();
      for(var p:points(face)){String point=key(p);if(vertices.isEmpty()||!vertices.getLast().equals(point))vertices.add(point);}
      if(vertices.size()>1&&vertices.getFirst().equals(vertices.getLast()))vertices.removeLast();
      if(vertices.size()<3)continue;
      for(int i=0;i<vertices.size();i++){
        String a=vertices.get(i),b=vertices.get((i+1)%vertices.size());
        edges.merge(a.compareTo(b)<0?a+"|"+b:b+"|"+a,1,Integer::sum);
      }
    }
    return !edges.isEmpty()&&edges.values().stream().allMatch(n->n==2);
  }
  private static Point mix(Point a,Point b,Point c,double u,double v){return new Point(a.x()*(1-u-v)+b.x()*u+c.x()*v,a.y()*(1-u-v)+b.y()*u+c.y()*v,a.z()*(1-u-v)+b.z()*u+c.z()*v);}
  private static void clear(Point p,double base,double size){
    double y=(p.y()-base)/size,r=Math.hypot(p.x(),p.z())/size;
    if(y>=.62-1e-9&&y<=URN_LIP+1e-9)check(r<URN_OPENING_RADIUS-1e-7||r>.52+1e-7,"Leaf intersects solid upper pot/rim: "+r+","+y);
  }
  public static int run(){checks=0;
    check(PLINTH_BOTTOM==0&&PLINTH_TOP>PLINTH_BOTTOM,"Gate statue support starts above the actual floor");
    check(STATUE_PLATFORM_TOP>PLINTH_TOP&&Math.abs((.69-.14)-STATUE_PLATFORM_TOP)<1e-9,"Feet no longer meet the supported platform");
    var head=statueHead(0,0,0);check(closed(head),"Statue head has an open seam");
    var tips=new HashSet<String>();double top=head.stream().flatMap(f->points(f).stream()).mapToDouble(Point::y).max().orElseThrow();
    for(var f:head)for(var p:points(f)){
      check(Double.isFinite(p.x()+p.y()+p.z()),"Non-finite statue point");
      if(Math.abs(p.y()-top)<1e-9)tips.add(key(p));
    }
    check(tips.size()==1&&Math.abs(top-.64)<1e-9,"Head apex is a flat cap or an extruded ridge");
    for(int i=0;i<head.size();i+=11){var damaged=new ArrayList<>(head);damaged.remove(i);check(!closed(damaged),"An open head surface passed closure");}
    for(double size:new double[]{.7,1})for(double base:new double[]{PLINTH_TOP,4.35}){
      var bowl=urn(0,base,0,size);check(closed(bowl),"Urn shell has an open underside or rim seam");
      check(Math.abs(bowl.stream().flatMap(f->points(f).stream()).mapToDouble(Point::y).min().orElseThrow()-base)<1e-9,"Urn floats above its plinth");
      long lipPoints=bowl.stream().flatMap(f->points(f).stream()).filter(p->Math.abs(p.y()-(base+URN_LIP*size))<1e-8&&Math.abs(Math.hypot(p.x(),p.z())-URN_OPENING_RADIUS*size)<1e-8).count();
      check(lipPoints>=16,"Urn lacks a real inner lip/opening");
      check(bowl.stream().flatMap(f->points(f).stream()).noneMatch(p->p.y()>base+.7*size&&Math.hypot(p.x(),p.z())<.34*size),"A solid vase top obstructs its plant");
      for(var f:leaves(0,base,0,size))for(int triangle=0;triangle<2;triangle++)for(int i=0;i<=30;i++)for(int j=0;j<=30-i;j++){
        clear(mix(f.a(),triangle==0?f.b():f.c(),triangle==0?f.c():f.d(),i/30.0,j/30.0),base,size);
      }
      var flowers=flowers(0,base,0,size);check(flowers.size()==5,"Foreground urn lost a blossom");
      for(var stem:flowers){
        check(stem.root().y()>=base+SOIL_Y*size&&stem.root().y()<=base+(SOIL_Y+.012)*size,"Flower stalk is not rooted in the visible soil");
        check(stem.bloom().y()>base+URN_LIP*size+.3*size,"Flower is hidden in the urn");
        check(Math.hypot(stem.root().x(),stem.root().z())<.27*size,"Stalk root lies outside the soil");
        for(int i=0;i<=30;i++){
          double t=i/30.0;clear(new Point(stem.root().x()*(1-t)+stem.bloom().x()*t,stem.root().y()*(1-t)+stem.bloom().y()*t,stem.root().z()*(1-t)+stem.bloom().z()*t),base,size);
        }
      }
    }
    return checks;
  }
  public static void main(String[] args){System.out.println("Lobby geometry0252: "+run()+" checks passed");}
}
