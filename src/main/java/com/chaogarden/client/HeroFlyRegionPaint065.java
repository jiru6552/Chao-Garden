package com.chaogarden.client;

/** Authored semantic paint on the canonical baby atlas for Hero/Fly FIRST
 * adults. The independently operated viewer guides regional placement and hue;
 * these flat pixels and RGB estimates do not import its textures or formulas.
 * White/inherited head skin, faces, source shading and finish stay intact. */
final class HeroFlyRegionPaint065 {
  record Input(boolean natural,boolean twoTone,float maturity,float runPower,
               float swimFly,int body,int accent,int wing,int marker) {}
  record Colors(int body,int torso,int limb,int hand,int foot,int band,int belly,int bellyLower,
                int wing,int wingTip,int tail,int marker,int featureBody,
                int featureMid,int featureTip) {}
  private HeroFlyRegionPaint065() {}

  // Run/balanced/Power across; Swim/balanced/Fly down. The independent
  // viewer keeps the white body while changing limb pigment, small chest blush,
  // crown ribbons, wing interiors and halo. These are authored flat pixels;
  // the source's smooth blush and fine silver zigzags are simplified.
  private static final int[] HAND={
      rgb(147,139,245),rgb(129,129,249),rgb(165,122,240),
      rgb(212,144,247),rgb(183,12,240),rgb(211,140,246),
      rgb(217,149,247),rgb(216,145,247),rgb(216,146,245)};
  private static final int[] BAND={
      rgb(210,250,108),rgb(212,252,81),rgb(219,248,112),
      rgb(169,236,243),rgb(10,233,245),rgb(170,238,243),
      rgb(146,236,244),rgb(137,236,246),rgb(147,237,243)};
  private static final int[] BELLY_UPPER={
      rgb(117,177,214),rgb(86,226,208),rgb(118,177,212),
      rgb(216,165,222),rgb(93,194,220),rgb(215,164,221),
      rgb(215,164,222),rgb(216,164,222),rgb(215,165,222)};
  private static final int[] BELLY_LOWER={
      rgb(192,160,109),rgb(214,211,20),rgb(195,163,114),
      rgb(173,57,185),rgb(177,45,175),rgb(173,57,184),
      rgb(174,60,182),rgb(173,59,181),rgb(174,60,182)};
  private static final int[] CAP_MID={
      rgb(218,238,137),rgb(214,244,96),rgb(219,239,139),
      rgb(210,235,231),rgb(9,228,239),rgb(211,232,231),
      rgb(210,232,232),rgb(211,231,233),rgb(210,233,232)};
  private static final int[] CAP_TIP={
      rgb(148,137,243),rgb(129,129,248),rgb(168,120,240),
      rgb(214,138,248),rgb(184,5,240),rgb(214,138,246),
      rgb(215,144,247),rgb(215,143,247),rgb(215,145,245)};
  private static final int[] WING_ROOT={
      rgb(149,138,241),rgb(141,131,245),rgb(165,124,237),
      rgb(220,237,235),rgb(174,14,237),rgb(218,237,235),
      rgb(220,238,234),rgb(219,239,234),rgb(220,238,234)};
  private static final int[] WING_TIP={
      rgb(7,225,225),rgb(8,227,230),rgb(8,225,228),
      rgb(13,220,239),rgb(10,220,235),rgb(13,220,238),
      rgb(12,221,238),rgb(12,221,239),rgb(13,221,237)};
  private static final int[] HALO={
      rgb(249,251,70),rgb(224,253,156),rgb(252,244,70),
      rgb(251,241,24),rgb(19,145,244),rgb(255,204,13),
      rgb(251,239,30),rgb(251,240,28),rgb(255,220,22)};
  private static final int YOUNG_HAND=rgb(213,146,245),
      YOUNG_BAND=rgb(163,234,242),YOUNG_BELLY_UPPER=rgb(218,168,224),
      YOUNG_BELLY_LOWER=rgb(174,70,184),YOUNG_MID=rgb(209,231,232),
      YOUNG_TIP=rgb(215,148,246),YOUNG_WING_ROOT=rgb(218,236,235),
      YOUNG_WING_TIP=rgb(16,221,236),YOUNG_HALO=rgb(246,142,248);

  static Colors colors(Input in) {
    float age=unit(in.maturity),rp=axis(in.runPower),sf=axis(in.swimFly);
    int body=in.body,hand,band,wing,wingTip,mid,tip,belly,bellyLower;
    if(in.natural&&in.twoTone) {
      hand=blend(YOUNG_HAND,corner(HAND,rp,sf),age);
      band=blend(YOUNG_BAND,corner(BAND,rp,sf),age);
      mid=blend(YOUNG_MID,corner(CAP_MID,rp,sf),age);
      tip=blend(YOUNG_TIP,corner(CAP_TIP,rp,sf),age);
      belly=blend(YOUNG_BELLY_UPPER,corner(BELLY_UPPER,rp,sf),age);
      bellyLower=blend(YOUNG_BELLY_LOWER,corner(BELLY_LOWER,rp,sf),age);
      wing=blend(YOUNG_WING_ROOT,corner(WING_ROOT,rp,sf),age);
      wingTip=blend(YOUNG_WING_TIP,corner(WING_TIP,rp,sf),age);
    } else {
      // Named, mono and jewel coats use the identical regional anatomy while
      // retaining inherited pigments and finish, never natural white/purple.
      boolean contrasting=in.twoTone&&distance(body,in.accent)>=18;
      int accent=contrasting?in.accent:tone(body,.78f);
      hand=accent;band=lighten(accent,.30f);
      mid=lighten(accent,.18f);tip=tone(accent,.82f);
      belly=contrasting?blend(body,accent,.55f):tone(body,.88f);
      bellyLower=contrasting?accent:tone(body,.78f);
      wing=contrasting?lighten(accent,.12f):tone(body,.82f);
      wingTip=contrasting?accent:tone(body,.72f);
    }
    int halo=halo(age,rp,sf);
    return new Colors(body,body,hand,hand,body,band,belly,bellyLower,
        wing,wingTip,hand,halo,body,mid,tip);
  }

  /** Five source-transparent texels only: body127, cap proximal125/distal126,
   * wing proximal124/distal123. Actual broad faces interpolate these adjacent
   * texels; two color regions do not require extra geometry or band meshes. */
  static int reservedKind(int x,int y) {
    if(y!=63)return 0;
    return switch(x){case 127,123,124->1;case 125,126->3;default->0;};
  }
  static int pixel(Input in,Colors c,int x,int y,int source,int flags) {
    if(reservedKind(x,y)!=0)return switch(x){
      case 127->c.featureBody;case 126->c.featureTip;case 125->c.featureMid;
      case 124->c.wing;default->c.wingTip;
    };
    if(source>>>24==0||(flags&HeroRunRegionPaint055.PROTECTED)!=0)return source;
    if((flags&HeroRunRegionPaint055.MARKER)!=0)return source&0xff000000|c.marker&0xffffff;
    if((flags&HeroRunRegionPaint055.WING)!=0)
      return tint(source,blend(c.wing,c.wingTip,wingDistal(x)),in.wing);
    if((flags&HeroRunRegionPaint055.SKIN)==0)return source;
    boolean accent=(flags&HeroRunRegionPaint055.ACCENT)!=0;
    int target=accent?c.limb:c.body,reference=accent?in.accent:in.body;
    if((flags&HeroRunRegionPaint055.TAIL)!=0)target=tailColor(c,x,y);
    else if(torsoUv(x,y))target=bellyUv(x,y)?bellyColor(c,x,y):c.body;
    else if((flags&HeroRunRegionPaint055.LIMB)!=0&&footUv(x,y))
      target=footSideUv(x,y)?footBandUv(x,y)?c.band:c.hand:c.foot;
    else if(accent&&(flags&HeroRunRegionPaint055.LIMB)!=0)
      target=handBandUv(x,y)?c.band:handTipUv(x,y)?c.body:c.hand;
    return tint(source,target,reference);
  }
  static boolean torsoUv(int x,int y) {
    return x>=0&&x<32&&y>=34&&y<42||x>=8&&x<24&&y>=26&&y<34;
  }
  /** Small painted chest blush; the surrounding original torso remains white
   * or inherited. The source's soft gradient becomes a few flat pixel rows. */
  static boolean bellyUv(int x,int y){return x>=10&&x<14&&y>=35&&y<41&&(y!=35&&y!=40||x==11||x==12);}
  static int bellyColor(Colors c,int x,int y) {
    int pigment=blend(c.belly,c.bellyLower,unit((y-36)/4f));
    return blend(c.body,pigment,x==10||x==13?.48f:.82f);
  }
  static boolean footUv(int x,int y){return x>=44&&x<68&&y>=18&&y<30;}
  /** Canonical white toe/front and heel planes stay white. Purple and a thin
   * cyan/lime edge occupy only the original side/top color islands. */
  static boolean footSideUv(int x,int y) {
    return x>=44&&x<52&&y>=26&&y<30||x>=56&&x<64&&y>=26&&y<30
        ||x>=52&&x<56&&y>=18&&y<23;
  }
  static boolean footBandUv(int x,int y){return y==26||x>=52&&x<56&&y==22;}
  static boolean handBandUv(int x,int y){return x>=24&&x<40&&y==32;}
  static boolean handTipUv(int x,int y){return x>=24&&x<40&&y==33||x>=32&&x<36&&y>=26&&y<30;}
  static int tailColor(Colors c,int x,int y) {
    // Existing canonical tail only: top/upper-side pigment and a narrow edge,
    // white underside and tip. No added fin, altered UV or extra tail box.
    if(x>=56&&x<60&&y>=30&&y<34)return x==56||x==59?c.band:c.tail;
    if(y==34&&(x>=52&&x<64))return c.band;
    if(y==35&&(x>=52&&x<56||x>=60&&x<64))return c.tail;
    return c.body;
  }
  static float wingDistal(int x){return unit(Math.abs(x-37.5f)/5.5f);}
  static boolean paletteProtected(String path,String layout,int x,int y,int width,int height) {
    return path.startsWith("dynamic/hero_fly065/")&&layout.equals("baby")&&width>0&&height>0
        &&HeroRunMarkerPaint056.markerUv(x*128/width,y*64/height);
  }
  static boolean paletteWing(String path,String layout,int x,int y,int width,int height) {
    if(!path.startsWith("dynamic/hero_fly065/")||!layout.equals("baby")||width<=0||height<=0)return false;
    int u=x*128/width,v=y*64/height;return v==63&&(u==123||u==124);
  }
  static int marker(float maturity,float rp,float sf){return halo(maturity,rp,sf);}
  static int halo(float maturity,float rp,float sf){return blend(YOUNG_HALO,corner(HALO,rp,sf),maturity);}
  private static int corner(int[] values,float rp,float sf) {
    rp=axis(rp);sf=axis(sf);int result=0xff000000;
    for(int c=0;c<3;c++) {
      float v=0;
      for(int y=0;y<3;y++)for(int x=0;x<3;x++)
        v+=(values[y*3+x]>>>(c*8)&255)*Math.max(0,1-Math.abs(rp-(x-1)))*Math.max(0,1-Math.abs(sf-(y-1)));
      result|=Math.clamp(Math.round(v),0,255)<<(c*8);
    }
    return result;
  }
  /** Channel ratios retain original pixel-to-pixel shading. */
  private static int tint(int source,int target,int reference) {
    int result=source&0xff000000;
    for(int c=0;c<3;c++) {
      int original=source>>>(c*8)&255,base=reference>>>(c*8)&255,next=target>>>(c*8)&255;
      int value=base==0?(next==0?original:Math.round(original*next/(float)Math.max(1,max(reference)))):Math.round(original*next/(float)base);
      result|=Math.clamp(value,0,255)<<(c*8);
    }
    return result;
  }
  private static int rgb(int r,int g,int b){return HeroRunRegionPaint055.rgb(r,g,b);}
  private static int blend(int a,int b,float t){return HeroRunRegionPaint055.blend(a,b,unit(t));}
  private static int tone(int p,float f){return HeroRunRegionPaint055.tone(p,f);}
  private static int lighten(int p,float f){return HeroRunRegionPaint055.lighten(p,f);}
  private static int max(int p){return HeroRunRegionPaint055.maxChannel(p);}
  private static int distance(int a,int b){return Math.max(Math.abs((a&255)-(b&255)),Math.max(Math.abs((a>>>8&255)-(b>>>8&255)),Math.abs((a>>>16&255)-(b>>>16&255))));}
  private static float unit(float v){return Float.isFinite(v)?Math.clamp(v,0,1):0;}
  private static float axis(float v){return Float.isFinite(v)?Math.clamp(v,-1,1):0;}
}
