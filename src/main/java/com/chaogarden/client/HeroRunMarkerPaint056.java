package com.chaogarden.client;

/** Marker pigment is independent of inherited skin colors and finish. The
 * actual viewer shows a blue young Hero orb and a pink developed Run Hero
 * orb; both are intentional. Block lighting/reflections remain renderer work. */
final class HeroRunMarkerPaint056 {
  private HeroRunMarkerPaint056() {}
  static int neutral(){return rgb(236,255,141);}
  static int neutralAdult(float maturity){return blend(rgb(255,215,143),rgb(246,228,62),maturity);}
  static int child(float hero,float dark,float run) {
    int ordinary=blend(blend(neutral(),rgb(70,160,230),hero),rgb(233,94,109),dark);
    int trained=blend(blend(rgb(38,226,127),rgb(240,145,177),hero),rgb(233,94,109),dark);
    return blend(ordinary,trained,run);
  }
  static int paintChild(int source,float hero,float dark,float run){return source&0xff000000|child(hero,dark,run)&0xffffff;}
  /** Canonical emotion-dot UV islands, excluding eyes and reaction symbols. */
  static boolean markerUv(int x,int y){return y>=0&&y<8&&x>=112&&x<128&&(y>=4||x>=116&&x<124);}
  static boolean paletteProtected(String sourcePath,String layout,boolean adult,int x,int y,int width,int height) {
    return layout.equals("baby")&&width>0&&height>0
        &&(sourcePath.startsWith("dynamic/neutral056/")||!adult&&sourcePath.startsWith("dynamic/hero_run055/"))
        &&markerUv(x*128/width,y*64/height);
  }
  private static int rgb(int r,int g,int b){return HeroRunRegionPaint055.rgb(r,g,b);}
  private static int blend(int a,int b,float t){return HeroRunRegionPaint055.blend(a,b,t);}
}
