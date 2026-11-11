package com.chaogarden.core;

import java.util.ArrayList;
import java.util.List;

/** Original meshes traced from the two supplied SA2 entrance reference pictures.
 * Local +Z is the visitor side; the three-by-four passage remains genuinely open. */
public final class GardenGateGeometry0256 {
  public enum Material { PLAIN, DARK_BRICK }
  public record Point(double x,double y,double z) {}
  public record Face(Point a,Point b,Point c,Point d,int color,Material material) {}
  public static final double PIER_X=2.22, HERO_TOP=5.42, DARK_TOP=4.91;
  private static final int IVORY=0xffeee9df, SHADOW=0xffbcb8ba, GOLD=0xffe2c36c,
      GOLD_SHADOW=0xffb59653, DARK=0xffb7a3ca, PALE=0xffcbc5d5, CARVING=0xff87818d;
  private static final List<Face> HERO=build(true),DARK_GATE=build(false);
  private GardenGateGeometry0256() {}
  public static List<Face> gate(boolean hero) {return hero?HERO:DARK_GATE;}
  /** Native animated soul fire uses these cup centres; no sculpted flame faces are emitted. */
  public static List<Point> darkFlames(){return List.of(new Point(-PIER_X,2.78,.58),new Point(PIER_X,2.78,.58));}

  private static List<Face> build(boolean hero) {
    var m=new Builder();
    if(hero) hero(m); else dark(m);
    return List.copyOf(m.faces);
  }

  private static void hero(Builder m) {
    // Tall plain gold shafts and square, inset capitals are the reference's
    // defining silhouette. There are no flowerpots or fluted Ionic columns here.
    for(int side:new int[]{-1,1}) {
      double x=side*PIER_X;
      m.box(x-.52,0,-.48,x+.52,.15,.48,IVORY);
      m.box(x-.46,.15,-.43,x+.46,.28,.43,SHADOW);
      m.cylinder(x,.28,0,.32,3.14,24,GOLD);
      m.cylinder(x,.28,0,.36,.12,24,GOLD_SHADOW);
      m.cylinder(x,3.28,0,.37,.14,24,GOLD_SHADOW);
      m.box(x-.52,3.42,-.48,x+.52,3.57,.48,SHADOW);
      m.box(x-.61,3.57,-.54,x+.61,3.68,.54,IVORY);
      m.box(x-.48,3.68,-.43,x+.48,4.43,.43,IVORY);
      // Recessed rectangular border and a raised Chao head medallion, on both faces.
      for(int facing:new int[]{-1,1}) {
        double z=facing*.438;
        m.box(x-.36,3.79,z-.016,x+.36,3.84,z+.016,SHADOW);
        m.box(x-.36,4.28,z-.016,x+.36,4.33,z+.016,SHADOW);
        m.box(x-.36,3.84,z-.016,x-.31,4.28,z+.016,SHADOW);
        m.box(x+.31,3.84,z-.016,x+.36,4.28,z+.016,SHADOW);
        m.ellipsoid(x,4.04,facing*.467,.19,.20,.045,0xffe1d5ac);
        m.cone(x,4.19,facing*.467,.09,.12,8,0xffe1d5ac);
        for(int eye:new int[]{-1,1})m.ellipsoid(x+eye*.066,4.06,facing*.512,.024,.038,.012,0xff9e967f);
        m.ellipsoid(x,4.34,facing*.467,.046,.046,.025,0xffe1d5ac);
      }
      m.box(x-.54,4.43,-.49,x+.54,4.55,.49,SHADOW);
      m.box(x-.65,4.55,-.57,x+.65,4.70,.57,IVORY);
      m.ellipsoid(x,5.09,0,.33,.33,.33,0xfff5f1e9);
      // A real tilted torus surrounds each white orb, rather than a flat disc.
      m.torus(x,5.11,.015,.43,.052,Math.toRadians(side*22),0xffb99745);
      statuePedestal(m,x,true);
      statue(m,x,1.03,.94,true,side);
    }
    // Shallow arched underside under a rectangular ivory frieze. Both faces,
    // reveals and the top are closed; the portal never has an opaque backing.
    for(int i=0;i<24;i++) {
      double a=i*Math.PI/24,b=(i+1)*Math.PI/24;
      double x0=Math.cos(a)*1.5,x1=Math.cos(b)*1.5;
      // Match the native portal's 24 angular crown chords, with .04 clearance.
      m.prism(new double[][]{{x0,3.36+Math.sin(a)*.68},{x1,3.36+Math.sin(b)*.68},
          {x1,4.59},{x0,4.59}},-.39,.39,IVORY);
    }
    m.box(-1.62,3.36,-.39,-1.5,4.59,.39,IVORY);
    m.box(1.5,3.36,-.39,1.62,4.59,.39,IVORY);
    m.box(-1.65,4.59,-.44,1.65,4.76,.44,IVORY);
    for(int front:new int[]{-1,1}) {
      double z=front*.448;
      m.box(-1.56,4.49,z-.014,1.56,4.54,z+.014,SHADOW);
      // Only the upper straight trim spans the opening; no straight lower
      // line crosses the curved cut-out.
      m.box(-1.56,4.65,z-.014,1.56,4.69,z+.014,0xfffaf5ed);
    }
  }
  public static double heroUnderside(double x) {
    return Math.abs(x)>1.5?3.36:3.36+.68*Math.sqrt(Math.max(0,1-x*x/2.25));
  }

  private static void dark(Builder m) {
    m.material=Material.DARK_BRICK;
    for(int side:new int[]{-1,1}) {
      double x=side*PIER_X;
      // Staggered stone courses, with wide raked shoulders above. Minecraft's
      // actual stone-brick material is tinted purple; the whites stay separate.
      for(int course=0;course<7;course++) {
        double y=.12+course*.39;
        m.box(x-.59,y,-.40,x+.59,y+.39,.40,tint(DARK,.94+(course%3)*.035));
      }
      m.prism(new double[][]{{x-.60,2.68},{x+.60,2.68},
          {x+.73,3.55},{x-.73,3.55}},-.40,.40,DARK);
    }
    for(int i=0;i<24;i++) {
      double a=i*Math.PI/24,b=(i+1)*Math.PI/24;
      double x0=Math.cos(a)*1.5,x1=Math.cos(b)*1.5;
      m.prism(new double[][]{{x0,2.69+Math.sin(a)*1.35},{x1,2.69+Math.sin(b)*1.35},
          {x1,darkCrown(x1)},{x0,darkCrown(x0)}},-.43,.43,tint(DARK,.95+(i%3)*.025));
    }
    for(int side:new int[]{-1,1}) {
      // Flared ends extend beyond the uprights, as in the supplied screenshot.
      double a=side*1.5,b=side*2.92;
      m.prism(new double[][]{{a,2.69},{b,3.29},{b,3.75},{a,darkCrown(a)}},-.43,.43,DARK);
    }
    m.material=Material.PLAIN;
    for(int side:new int[]{-1,1}) {
      double x=side*PIER_X;
      m.box(x-.65,0,-.49,x+.65,.14,.49,PALE);
      m.box(x-.68,1.03,-.52,x+.68,1.24,.52,PALE);
      m.box(x-.66,1.24,-.49,x+.66,1.35,.49,0xffaaa3b5);
      statuePedestal(m,x,false);
      statue(m,x,1.01,.94,false,side);
      // The blue-violet fires sit above the statues on the front of each pier.
      m.ellipsoid(x,2.65,.53,.22,.14,.21,0xffaaa4b8);
      m.box(x-.16,2.58,.40,x+.16,2.68,.66,0xffa9a3b2);
    }
  }
  public static double darkUnderside(double x) {
    return 2.69+1.35*Math.sqrt(Math.max(0,1-x*x/2.25));
  }
  private static double darkCrown(double x) {return DARK_TOP-.17*x*x;}

  private static void statuePedestal(Builder m,double x,boolean hero) {
    int stone=hero?IVORY:PALE;
    m.box(x-.48,.12,.36,x+.48,.79,1.40,hero?0xffc7c3b8:0xff645c70);
    m.box(x-.58,.10,.27,x+.58,.22,1.49,stone);
    m.box(x-.55,.78,.29,x+.55,.91,1.47,stone);
    m.box(x-.48,.91,.35,x+.48,1.02,1.41,hero?SHADOW:0xffa69eaf);
  }

  private static void statue(Builder m,double x,double base,double z,boolean hero,int side) {
    int body=hero?0xffe8e5db:0xffbbb6c2, detail=hero?0xff77757b:0xff64616e;
    // Rounded body, seated feet, little arms and an unmistakable Chao head.
    m.ellipsoid(x,base+.38,z,.29,.34,.25,body);
    m.ellipsoid(x,base+.88,z,.40,.34,.32,body);
    for(int limb:new int[]{-1,1}) {
      m.ellipsoid(x+limb*.23,base+.10,z+.19,.20,.13,.24,body);
      m.ellipsoid(x+limb*.34,base+.40,z+.13,.115,.24,.12,body);
      m.ellipsoid(x+limb*.14,base+.89,z+.304,.055,.09,.023,detail);
    }
    if(hero) {
      m.cone(x,base+1.13,z,.16,.22,12,body);
      // Small feather wings, raised from the back, and the Hero halo.
      for(int wing:new int[]{-1,1}) {
        for(int feather=0;feather<3;feather++) {
          double a=x+wing*(.28+feather*.10),y=base+.41+feather*.07;
          m.prism(new double[][]{{a,y},{a+wing*.18,y+.27},{a+wing*.06,y+.01}},z-.23,z-.16,body);
        }
      }
      m.torus(x,base+1.48,z,.16,.026,Math.toRadians(side*12),0xffd8d3c4);
    } else {
      // The dark head is horned, and its wing webbing and trident give it the
      // crouched devil-Chao silhouette seen beside the original entrance.
      m.cone(x-.29,base+1.02,z-.03,.12,.32,10,body);
      m.cone(x+.29,base+1.02,z-.03,.12,.32,10,body);
      m.cone(x,base+1.16,z,.13,.18,10,body);
      m.ellipsoid(x,base+1.46,z,.065,.065,.065,body);
      for(int wing:new int[]{-1,1}) {
        double u=x+wing*.28;
        m.prism(new double[][]{{u,base+.44},{u+wing*.29,base+.75},
            {u+wing*.24,base+.44},{u+wing*.14,base+.52},{u+wing*.07,base+.35}},z-.24,z-.20,0xffa9a3b1);
      }
      double tx=x+side*.48;
      m.cylinder(tx,base+.08,z+.12,.025,1.25,8,detail);
      m.box(tx-.15,base+1.17,z+.09,tx+.15,base+1.22,z+.15,detail);
      for(int prong:new int[]{-1,0,1}) {
        m.cylinder(tx+prong*.14,base+1.18,z+.12,.018,.18,6,detail);
        m.cone(tx+prong*.14,base+1.36,z+.12,.045,.11,6,detail);
      }
    }
  }

  private static int tint(int color,double value) {
    return 0xff000000|Math.min(255,(int)((color>>16&255)*value))<<16|
        Math.min(255,(int)((color>>8&255)*value))<<8|Math.min(255,(int)((color&255)*value));
  }

  private static final class Builder {
    final List<Face> faces=new ArrayList<>(); Material material=Material.PLAIN;
    Point p(double x,double y,double z) {return new Point(x,y,z);}
    void quad(Point a,Point b,Point c,Point d,int color) {faces.add(new Face(a,b,c,d,color,material));}
    void box(double x0,double y0,double z0,double x1,double y1,double z1,int color) {
      quad(p(x0,y0,z1),p(x1,y0,z1),p(x1,y1,z1),p(x0,y1,z1),color);
      quad(p(x1,y0,z0),p(x0,y0,z0),p(x0,y1,z0),p(x1,y1,z0),tint(color,.86));
      quad(p(x0,y0,z0),p(x0,y0,z1),p(x0,y1,z1),p(x0,y1,z0),tint(color,.86));
      quad(p(x1,y0,z1),p(x1,y0,z0),p(x1,y1,z0),p(x1,y1,z1),tint(color,.94));
      quad(p(x0,y1,z1),p(x1,y1,z1),p(x1,y1,z0),p(x0,y1,z0),color);
      quad(p(x0,y0,z0),p(x1,y0,z0),p(x1,y0,z1),p(x0,y0,z1),tint(color,.74));
    }
    void prism(double[][] polygon,double z0,double z1,int color) {
      // All authoring outlines are convex, except the bat wing, which is emitted
      // as its own visible planar fan with a shallow edge reveal.
      for(int i=1;i<polygon.length-1;i++) {
        var a=polygon[0];var b=polygon[i];var c=polygon[i+1];
        quad(p(a[0],a[1],z1),p(b[0],b[1],z1),p(c[0],c[1],z1),p(c[0],c[1],z1),color);
        quad(p(c[0],c[1],z0),p(b[0],b[1],z0),p(a[0],a[1],z0),p(a[0],a[1],z0),tint(color,.85));
      }
      for(int i=0;i<polygon.length;i++) {
        var a=polygon[i];var b=polygon[(i+1)%polygon.length];
        quad(p(a[0],a[1],z0),p(b[0],b[1],z0),p(b[0],b[1],z1),p(a[0],a[1],z1),tint(color,.91));
      }
    }
    void cylinder(double x,double y,double z,double radius,double height,int segments,int color) {
      frustum(x,y,z,radius,radius,height,segments,color);
    }
    void cone(double x,double y,double z,double radius,double height,int segments,int color) {
      frustum(x,y,z,radius,0,height,segments,color);
    }
    void frustum(double x,double y,double z,double low,double high,double height,int n,int color) {
      for(int i=0;i<n;i++) {
        double a=i*Math.PI*2/n,b=(i+1)*Math.PI*2/n;
        var p0=p(x+Math.cos(a)*low,y,z+Math.sin(a)*low);
        var p1=p(x+Math.cos(b)*low,y,z+Math.sin(b)*low);
        var p2=p(x+Math.cos(b)*high,y+height,z+Math.sin(b)*high);
        var p3=p(x+Math.cos(a)*high,y+height,z+Math.sin(a)*high);
        quad(p0,p1,p2,p3,tint(color,.82+.18*(Math.cos((a+b)/2)+1)/2));
        quad(p(x,y,z),p1,p0,p0,tint(color,.75));
        if(high>0)quad(p(x,y+height,z),p3,p2,p2,color);
      }
    }
    void ellipsoid(double x,double y,double z,double rx,double ry,double rz,int color) {
      for(int row=0;row<8;row++)for(int col=0;col<16;col++) {
        double a=row*Math.PI/8,b=(row+1)*Math.PI/8,c=col*Math.PI/8,d=(col+1)*Math.PI/8;
        quad(sphere(x,y,z,rx,ry,rz,a,c),sphere(x,y,z,rx,ry,rz,a,d),
            sphere(x,y,z,rx,ry,rz,b,d),sphere(x,y,z,rx,ry,rz,b,c),tint(color,.80+.20*Math.sin((a+b)/2)));
      }
    }
    Point sphere(double x,double y,double z,double rx,double ry,double rz,double a,double b) {
      return p(x+rx*Math.sin(a)*Math.cos(b),y+ry*Math.cos(a),z+rz*Math.sin(a)*Math.sin(b));
    }
    void torus(double x,double y,double z,double radius,double tube,double tilt,int color) {
      for(int a=0;a<32;a++)for(int b=0;b<8;b++) {
        double u=a*Math.PI/16,v=(a+1)*Math.PI/16,s=b*Math.PI/4,t=(b+1)*Math.PI/4;
        quad(torusPoint(x,y,z,radius,tube,tilt,u,s),torusPoint(x,y,z,radius,tube,tilt,v,s),
            torusPoint(x,y,z,radius,tube,tilt,v,t),torusPoint(x,y,z,radius,tube,tilt,u,t),color);
      }
    }
    Point torusPoint(double x,double y,double z,double r,double tube,double tilt,double u,double v) {
      double a=(r+tube*Math.cos(v))*Math.cos(u),b=tube*Math.sin(v),c=(r+tube*Math.cos(v))*Math.sin(u);
      return p(x+a*Math.cos(tilt)-b*Math.sin(tilt),y+a*Math.sin(tilt)+b*Math.cos(tilt),z+c);
    }
  }
}
