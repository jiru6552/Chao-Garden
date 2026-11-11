package com.chaogarden.core;
import java.util.ArrayList;
import java.util.List;
/** Fine curved leaves: twenty-one owning voxels and rigid hinge motion as saved pairs. */
public final class SchoolDoorGeometry0255 {
  private SchoolDoorGeometry0255(){}
  public static final double LEFT_HINGE=-14.5,RIGHT_HINGE=30.5,WIDTH=22.5,ART_MARGIN=.25,SEAM=0,HINGE_DEPTH=15,SWING_DEPTH=16;
  private static final double[][] PROFILE={
      {-14.5,-14.487953,23.99},
      {-14.487953,-14.451826,24.726186},
      {-14.451826,-14.391656,25.461567},
      {-14.391656,-14.307509,26.195389},
      {-14.307509,-14.199475,26.926842},
      {-14.199475,-14.067669,27.655148},
      {-14.067669,-13.912232,28.379531},
      {-13.912232,-13.733331,29.099216},
      {-13.733331,-13.531158,29.813429},
      {-13.531158,-13.305928,30.521404},
      {-13.305928,-13.057883,31.222388},
      {-13.057883,-12.787289,31.915627},
      {-12.787289,-12.494436,32.600378},
      {-12.494436,-12.179637,33.275908},
      {-12.179637,-11.843228,33.941495},
      {-11.843228,-11.485572,34.596427},
      {-11.485572,-11.107049,35.239999},
      {-11.107049,-10.708066,35.871527},
      {-10.708066,-10.28905,36.490331},
      {-10.28905,-9.85045,37.095749},
      {-9.85045,-9.392735,37.687132},
      {-9.392735,-8.916396,38.263849},
      {-8.916396,-8.421942,38.82528},
      {-8.421942,-7.909903,39.370826},
      {-7.909903,-7.380827,39.899902},
      {-7.380827,-6.835281,40.411941},
      {-6.835281,-6.273849,40.906396},
      {-6.273849,-5.697132,41.382735},
      {-5.697132,-5.105748,41.84045},
      {-5.105748,-4.50033,42.279051},
      {-4.50033,-3.881527,42.698066},
      {-3.881527,-3.25,43.097049},
      {-3.25,-2.606427,43.475572},
      {-2.606427,-1.951496,43.833228},
      {-1.951496,-1.285908,44.169636},
      {-1.285908,-0.610377,44.484436},
      {-0.610377,0.0,44.77729},
      {0.0,0.074374,45.019741},
      {0.074374,0.767612,45.047883},
      {0.767612,1.468595,45.295928},
      {1.468595,2.176571,45.521158},
      {2.176571,2.890784,45.723331},
      {2.890784,3.610468,45.902232},
      {3.610468,4.334852,46.057669},
      {4.334852,5.063161,46.189475},
      {5.063161,5.794614,46.297509},
      {5.794614,6.52843,46.381656},
      {6.52843,7.263821,46.441826},
      {7.263821,8.0,46.477953},
      {8.0,8.736179,46.477953},
      {8.736179,9.47157,46.441826},
      {9.47157,10.205386,46.381656},
      {10.205386,10.936839,46.297509},
      {10.936839,11.665148,46.189475},
      {11.665148,12.389532,46.057669},
      {12.389532,13.109216,45.902232},
      {13.109216,13.823429,45.723331},
      {13.823429,14.531405,45.521158},
      {14.531405,15.232388,45.295928},
      {15.232388,15.925626,45.047883},
      {15.925626,16.0,45.019741},
      {16.0,16.610377,44.77729},
      {16.610377,17.285908,44.484436},
      {17.285908,17.951496,44.169636},
      {17.951496,18.606427,43.833228},
      {18.606427,19.25,43.475572},
      {19.25,19.881527,43.097049},
      {19.881527,20.50033,42.698066},
      {20.50033,21.105748,42.279051},
      {21.105748,21.697132,41.84045},
      {21.697132,22.273849,41.382735},
      {22.273849,22.835281,40.906396},
      {22.835281,23.380827,40.411941},
      {23.380827,23.909903,39.899902},
      {23.909903,24.421942,39.370826},
      {24.421942,24.916396,38.82528},
      {24.916396,25.392735,38.263849},
      {25.392735,25.85045,37.687132},
      {25.85045,26.28905,37.095749},
      {26.28905,26.708066,36.490331},
      {26.708066,27.107049,35.871527},
      {27.107049,27.485572,35.239999},
      {27.485572,27.843228,34.596427},
      {27.843228,28.179637,33.941495},
      {28.179637,28.494436,33.275908},
      {28.494436,28.787289,32.600378},
      {28.787289,29.057883,31.915627},
      {29.057883,29.305928,31.222388},
      {29.305928,29.531158,30.521404},
      {29.531158,29.733331,29.813429},
      {29.733331,29.912232,29.099216},
      {29.912232,30.067669,28.379531},
      {30.067669,30.199475,27.655148},
      {30.199475,30.307509,26.926842},
      {30.307509,30.391656,26.195389},
      {30.391656,30.451826,25.461567},
      {30.451826,30.487953,24.726186},
      {30.487953,30.5,23.99}
  };
  public record Box(double x0,double y0,double z0,double x1,double y1,double z1,boolean left){}
  public static List<Box> boxes(boolean left,boolean open,int section,int segment){
    List<Box> out=new ArrayList<>();
    if((open&&section==1)||(!open&&section>=2))return out;
    for(boolean physicalLeft:new boolean[]{true,false}){
      if(section!=1&&physicalLeft!=left)continue;
      for(double[] stripe:PROFILE){
        double x0=Math.max(stripe[0],physicalLeft?LEFT_HINGE:8+SEAM/2),x1=Math.min(stripe[1],physicalLeft?8-SEAM/2:RIGHT_HINGE);
        double y1=Math.min(16,stripe[2]-segment*16);if(x0>=x1||y1<=0)continue;
        Box next;
        if(open){
          double d0=physicalLeft?x0-LEFT_HINGE:RIGHT_HINGE-x1,d1=physicalLeft?x1-LEFT_HINGE:RIGHT_HINGE-x0;
          double offset=section==3?32:section==2?16:0;
          double z0=Math.max(0,SWING_DEPTH+d0-offset),z1=Math.min(16,SWING_DEPTH+d1-offset);if(z0>=z1)continue;
          next=new Box(physicalLeft?1.5:12.5,0,round(z0),physicalLeft?3.5:14.5,round(y1),round(z1),physicalLeft);
        }else{
          double offset=section==1?0:physicalLeft?-16:16,a=Math.max(0,x0-offset),b=Math.min(16,x1-offset);if(a>=b)continue;
          next=new Box(round(a),0,HINGE_DEPTH-1,round(b),round(y1),HINGE_DEPTH+1,physicalLeft);
        }
        if(!out.isEmpty()){
          var old=out.getLast();
          if(old.left==next.left&&old.y0==next.y0&&old.y1==next.y1&&(open?old.x0==next.x0&&old.x1==next.x1&&(old.z1==next.z0||old.z0==next.z1):old.z0==next.z0&&old.z1==next.z1&&(old.x1==next.x0||old.x0==next.x1))){
            out.set(out.size()-1,new Box(Math.min(old.x0,next.x0),old.y0,Math.min(old.z0,next.z0),Math.max(old.x1,next.x1),old.y1,Math.max(old.z1,next.z1),old.left));continue;
          }
        }
        out.add(next);
      }
    }
    return List.copyOf(out);
  }
  private static double round(double value){return Math.rint(value*1000000)/1000000;}
  public static double heightAtDistance(boolean left,double distance){
    double x=left?LEFT_HINGE+distance:RIGHT_HINGE-distance;
    for(double[] stripe:PROFILE)if(x>=stripe[0]&&x<=stripe[1])return stripe[2];return 0;
  }
}
