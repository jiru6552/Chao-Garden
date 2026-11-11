package com.chaogarden.core;

import java.util.List;
import static com.chaogarden.core.LobbyApproachGeometry0255.*;

/** Independent bounds, native thin-floor coverage, open entry and joined rail invariants. */
public final class LobbyApproachGeometry0255Test {
  private static int checks;
  private static void check(boolean c,String m){checks++;if(!c)throw new AssertionError(m);}
  private static boolean solid(double wx,double y,double wz){
    int x=(int)Math.floor(wx),iy=(int)Math.floor(y),z=(int)Math.floor(wz);
    for(var b:nativeBoxes(x,iy,z))if(wx-x>=b.x0()-1e-8&&wx-x<=b.x1()+1e-8&&y-iy>=b.y0()-1e-8&&y-iy<=b.y1()+1e-8&&wz-z>=b.z0()-1e-8&&wz-z<=b.z1()+1e-8)return true;
    return false;
  }
  private static double distance(Point a,Point b){return Math.hypot(a.x()-b.x(),a.z()-b.z());}
  public static int run(){checks=0;
    check(STEPS==18&&OUTER_RADIUS-INNER_RADIUS==3,"Garden flights lost their three-block walking width");
    check(centre(true,START_ANGLE,80).z()>0&&centre(true,END_ANGLE,89).z()<0,"Hero stairs were not flipped to climb from south to north");
    check(centre(false,START_ANGLE,80).z()<0&&centre(false,END_ANGLE,71).z()>0,"Hero flip changed the Dark stair direction");
    for(boolean hero:new boolean[]{true,false}){
      for(int i=0;i<18;i++){
        double a=START_ANGLE+(END_ANGLE-START_ANGLE)*(i+.5)/18,expected=80+(hero?1:-1)*(i+1)*.5;
        var p=centre(hero,a,expected);
        check(Math.abs(Math.hypot(p.x(),p.z())-22)<1e-8,"Flight cuts across the lobby centre instead of following its wall");
        check(Math.abs(surface(hero,p.x(),p.z())-expected)<1e-8,"Curved tread has the wrong half-block elevation");
        check(solid(p.x()+.5,expected-.05,p.z()+.5),"Visible tread lacks native collision");
        if(i>1)check(!solid(p.x()+.5,expected-.25,p.z()+.5),"Bulky old glass block remains beneath a thin tread");
      }
      var edges=boundaryFor(hero);int entry=0;
      for(var e:edges){
        boolean joinsA=false,joinsB=false;
        for(var other:edges)if(other!=e){if(distance(e.a(),other.a())<1e-6||distance(e.a(),other.b())<1e-6)joinsA=true;if(distance(e.b(),other.a())<1e-6||distance(e.b(),other.b())<1e-6)joinsB=true;}
        check(joinsA&&joinsB,"Stair/landing perimeter has a disconnected turn");
        if(entryEdge(hero,e))entry++;
      }
      check(entry==1,"A stairwell entry is fenced closed or has multiple unguarded edges");
      check(rails(hero,false).size()==edges.size()-1,"An exposed stair/landing edge is missing its rail");
      for(var r:rails(hero,false)){
        check(r.skirt()>=(hero?.12:.5),"Descending glass guard leaves an open triangle below its half-step");
        double x=(r.edge().a().x()+r.edge().b().x())/2+.5,z=(r.edge().a().z()+r.edge().b().z())/2+.5,y=(r.y0()+r.y1())/2+.6;
        boolean hit=false;for(double dx:new double[]{-.05,0,.05})for(double dz:new double[]{-.05,0,.05})hit|=solid(x+dx,y,z+dz);
        check(hit,"Visible rail midpoint has no matching collision");
      }
      check(landingY(hero)+1==(hero?89:71),"Gate platform elevation changed");
    }
    for(var e:nativeCells().entrySet())for(var b:e.getValue())check(b.x0()>=-1e-8&&b.z0()>=-1e-8&&b.y0()>=-1e-8&&b.x1()<=1+1e-8&&b.y1()<=1+1e-8&&b.z1()<=1+1e-8,"A native collision cell leaks into a neighbouring cell");
    // The old central flights and their unsupported foundations are gone.
    for(int x:new int[]{-18,-12,12,18})check(nativeBoxes(x,78,7).isEmpty(),"Old straight stair foundation remains under the lobby floor");
    for(double x=-25;x<=25;x+=.4)for(double z=-20;z<=16;z+=.4){
      var q=List.of(new Point(x,0,z),new Point(x+.02,0,z),new Point(x+.02,0,z+.02),new Point(x,0,z+.02));
      boolean hole=footprint(false,x+.01,z+.01);var pieces=floorPieces(q);boolean drawn=false;for(var p:pieces)drawn|=inside(p,x+.01,z+.01);
      check(drawn!=hole,"The visible main floor covers the lower staircase or erases solid lobby floor");
    }
    return checks;
  }
  public static void main(String[] args){System.out.println("Lobby perimeter approaches0256: "+run()+" checks passed");}
}
