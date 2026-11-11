package com.chaogarden.core;

import java.util.List;
import java.util.ArrayList;
import static com.chaogarden.core.GardenGateGeometry0256.*;

/** Sweeps the actual emitted faces, including rear reveals, rather than a second gate model. */
public final class GardenGateGeometry0256Test {
  private static int checks;
  private static void check(boolean value,String message){checks++;if(!value)throw new AssertionError(message);}
  private static double side(Point a,Point b,double x,double y){return (b.x()-a.x())*(y-a.y())-(b.y()-a.y())*(x-a.x());}
  private static boolean triangle(Point a,Point b,Point c,double x,double y){
    double area=side(a,b,c.x(),c.y());if(Math.abs(area)<1e-9)return false;
    double s0=side(a,b,x,y),s1=side(b,c,x,y),s2=side(c,a,x,y);
    return area>0?s0>=-1e-8&&s1>=-1e-8&&s2>=-1e-8:s0<=1e-8&&s1<=1e-8&&s2<=1e-8;
  }
  private static boolean covers(Face f,double x,double y){
    // Exclude the forward pedestal sculpture when examining the clear doorway
    // plane; its support is deliberately in front of, and beside, the passage.
    if(Math.abs(f.a().z())>.60||Math.abs(f.b().z())>.60||Math.abs(f.c().z())>.60||Math.abs(f.d().z())>.60)return false;
    return triangle(f.a(),f.b(),f.c(),x,y)||triangle(f.a(),f.c(),f.d(),x,y);
  }
  private static boolean hits(List<Face> faces,double x,double y){for(var f:faces)if(covers(f,x,y))return true;return false;}
  public static int run(){checks=0;
    for(boolean hero:new boolean[]{true,false}){
      var faces=gate(hero);check(faces.size()>1000,"Gate lost its sculpted details");
      double maxY=0,minY=100,maxX=0;
      boolean brick=false,haloGold=false,flameBlue=false;
      for(var f:faces){
        for(var p:List.of(f.a(),f.b(),f.c(),f.d())){
          check(Double.isFinite(p.x())&&Double.isFinite(p.y())&&Double.isFinite(p.z()),"Non-finite gate coordinate");
          minY=Math.min(minY,p.y());maxY=Math.max(maxY,p.y());maxX=Math.max(maxX,Math.abs(p.x()));
        }
        if(f.material()==Material.DARK_BRICK)brick=true;
        if(f.a().y()>4.8&&(f.color()&0xffffff)==0xb99745)haloGold=true;
        if((f.color()&0xffffff)==0x514bdf)flameBlue=true;
      }
      check(minY==0,"Gate plinth floats above its authored landing");
      check(maxX>=2.7&&maxX<3,"Gate capitals/shoulders have the wrong envelope");
      check(maxY>(hero?5.3:4.8)&&maxY<5.7,"Gate height no longer matches the supplied reference proportions");
      check(brick!=hero,"Dark masonry material leaked into the Hero gate or vanished");
      check(haloGold==hero,"Hero's orb halos are missing or leaked into Dark");
      check(!flameBlue,"Rigid blue flame tongues returned to the gate mesh");
      // Keep every sample inside the real 3x4 native aperture open, from either
      // direction. This catches broad lintels or a hidden solid rear backing.
      int style=hero?PortalGeometry0251.HERO:PortalGeometry0251.DARK;
      for(double x=-1.475;x<1.5;x+=.05)for(double y=.025;y<4;y+=.05)
        if(PortalGeometry0251.contains(style,x,y))check(!hits(faces,x,y),"Gate "+hero+" masonry covers the native portal opening at "+x+","+y);
      for(double x:new double[]{-2.1,2.1}){
        check(hits(faces,x,.5),"Gate lost one of its grounded piers");
        check(hits(faces,x,3.8),"Gate lost one of its capitals/shoulders");
      }
      check(hits(faces,0,hero?4.64:4.7),"Gate lintel has a visible hole");
    }
    check(darkFlames().size()==2,"Gate needs exactly two live soul-fire sources");
    for(var flame:darkFlames()){
      check(Math.abs(Math.abs(flame.x())-PIER_X)<1e-9&&flame.y()==2.78&&flame.z()==.58,
          "A live flame is not positioned over its cup");
    }
    var blocked=new ArrayList<>(gate(true));
    blocked.add(new Face(new Point(-1,1,0),new Point(1,1,0),new Point(1,3,0),new Point(-1,3,0),0xffffffff,Material.PLAIN));
    check(hits(blocked,.1,2),"Portal sweep accepted an opaque backing mutation");
    return checks;
  }
  public static void main(String[]args){System.out.println("Garden gate geometry0256: "+run()+" checks passed");}
}
