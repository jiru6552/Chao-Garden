package com.chaogarden.client;

/** Authored semantic paint on the canonical baby atlas for Hero/Swim FIRST
 * adults. The independently operated viewer guides regional placement and hue;
 * these flat pixels and RGB estimates do not import its textures or formulas.
 * White/inherited head skin, faces, source shading and finish stay intact. */
final class HeroSwimRegionPaint064 {
  record Input(boolean natural,boolean twoTone,float maturity,float runPower,
               float swimFly,int body,int accent,int wing,int marker) {}
  record Colors(int body,int torso,int limb,int hand,int foot,int band,int belly,
                int wing,int wingTip,int tail,int marker,int featureBody,
                int featureMid,int featureTip) {}
  private HeroSwimRegionPaint064() {}

  // Run/balanced/Power across; Swim/balanced/Fly down. Every endpoint is still
  // Hero Swim FIRST, including the opposite Fly row. Natural head and belly
  // remain white while torso, mitts, toe caps, paired crown and halo develop.
  private static final int[] TORSO={
      rgb(64,135,251),rgb(111,66,247),rgb(134,163,200),
      rgb(3,201,242),rgb(3,218,187),rgb(140,245,184),
      rgb(8,205,199),rgb(4,208,146),rgb(85,232,166)};
  private static final int[] HAND={
      rgb(71,167,233),rgb(126,149,244),rgb(204,190,144),
      rgb(4,172,224),rgb(4,198,179),rgb(255,219,10),
      rgb(5,166,183),rgb(3,192,146),rgb(148,211,67)};
  private static final int[] BAND={
      rgb(60,255,221),rgb(11,250,223),rgb(196,248,163),
      rgb(46,250,206),rgb(250,245,132),rgb(249,244,156),
      rgb(105,249,80),rgb(154,247,13),rgb(201,245,106)};
  private static final int[] CAP_MID={
      rgb(123,255,206),rgb(140,255,213),rgb(147,246,202),
      rgb(109,255,199),rgb(246,244,164),rgb(151,246,180),
      rgb(135,250,107),rgb(161,248,12),rgb(147,244,94)};
  private static final int[] CAP_TIP={
      rgb(58,143,234),rgb(120,137,249),rgb(200,186,139),
      rgb(3,172,228),rgb(4,190,158),rgb(254,218,12),
      rgb(0,183,166),rgb(1,194,144),rgb(135,207,67)};
  private static final int[] WING_ROOT={
      rgb(185,241,194),rgb(242,244,167),rgb(162,249,197),
      rgb(120,232,229),rgb(136,117,228),rgb(142,244,181),
      rgb(213,243,142),rgb(254,241,136),rgb(206,247,154)};
  private static final int[] WING_TIP={
      rgb(83,177,207),rgb(2,165,204),rgb(39,170,171),
      rgb(48,159,220),rgb(94,220,230),rgb(54,189,180),
      rgb(160,197,138),rgb(246,221,36),rgb(167,200,105)};
  private static final int[] HALO={
      rgb(210,220,195),rgb(159,125,252),rgb(219,177,193),
      rgb(204,253,93),rgb(14,158,244),rgb(255,194,81),
      rgb(248,244,31),rgb(255,242,7),rgb(255,219,43)};
  private static final int YOUNG_TORSO=rgb(137,248,194),YOUNG_HAND=rgb(104,234,191),
      YOUNG_BAND=rgb(242,248,165),YOUNG_MID=rgb(186,246,177),
      YOUNG_TIP=rgb(103,218,166),YOUNG_WING_ROOT=rgb(150,218,247),
      YOUNG_WING_TIP=rgb(78,162,224),YOUNG_HALO=rgb(253,245,87);

  static Colors colors(Input in) {
    float age=unit(in.maturity),rp=axis(in.runPower),sf=axis(in.swimFly);
    int body=in.body,torso,hand,foot,band,wing,wingTip,mid,tip;
    if(in.natural&&in.twoTone) {
      torso=blend(YOUNG_TORSO,corner(TORSO,rp,sf),age);
      hand=blend(YOUNG_HAND,corner(HAND,rp,sf),age);foot=hand;
      band=blend(YOUNG_BAND,corner(BAND,rp,sf),age);
      mid=blend(YOUNG_MID,corner(CAP_MID,rp,sf),age);
      tip=blend(YOUNG_TIP,corner(CAP_TIP,rp,sf),age);
      wing=blend(YOUNG_WING_ROOT,corner(WING_ROOT,rp,sf),age);
      wingTip=blend(YOUNG_WING_TIP,corner(WING_TIP,rp,sf),age);
    } else {
      // Named, mono and jewel coats retain the same skin and finish. Their
      // anatomy has identical regions with inherited pigments/value contrast;
      // white or mint natural-family colors never replace their base hue.
      boolean contrasting=in.twoTone&&distance(body,in.accent)>=18;
      int accent=contrasting?in.accent:tone(body,.78f);
      torso=contrasting?blend(body,accent,.68f):tone(body,.85f);
      hand=accent;foot=accent;band=lighten(accent,.30f);
      mid=lighten(accent,.18f);tip=tone(accent,.82f);
      wing=contrasting?lighten(accent,.12f):tone(body,.82f);
      wingTip=contrasting?accent:tone(body,.72f);
    }
    int halo=halo(age,rp,sf);
    return new Colors(body,torso,hand,hand,foot,band,body,wing,wingTip,body,
        halo,body,mid,tip);
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
    if((flags&HeroRunRegionPaint055.TAIL)!=0)target=c.tail;
    else if(torsoUv(x,y)) {
      target=bellyUv(x,y)||backWaistUv(x,y)?c.belly:bellyRimUv(x,y)?c.band:c.torso;
    } else if((flags&HeroRunRegionPaint055.LIMB)!=0&&footUv(x,y)) {
      target=footToeUv(x,y)?c.foot:footEdgeUv(x,y)?c.band:c.body;
    } else if(accent&&(flags&HeroRunRegionPaint055.LIMB)!=0) {
      target=handBandUv(x,y)?c.band:c.hand;
    }
    return tint(source,target,reference);
  }
  static boolean torsoUv(int x,int y) {
    return x>=0&&x<32&&y>=34&&y<42||x>=8&&x<24&&y>=26&&y<34;
  }
  static boolean bellyUv(int x,int y){return x>=10&&x<14&&y>=36&&y<41;}
  static boolean bellyRimUv(int x,int y){return x>=9&&x<15&&y>=35&&y<41&&!bellyUv(x,y);}
  static boolean backWaistUv(int x,int y){return x>=27&&x<29&&y>=40&&y<42;}
  static boolean footUv(int x,int y){return x>=44&&x<68&&y>=18&&y<30;}
  static boolean footToeUv(int x,int y) {
    return x>=52&&x<56&&y>=26&&y<30
        ||x>=52&&x<60&&y>=23&&y<25
        ||y>=26&&y<28&&(x>=49&&x<52||x>=56&&x<59);
  }
  static boolean footEdgeUv(int x,int y) {
    return x>=52&&x<60&&y==25||y==28&&(x>=49&&x<52||x>=56&&x<59);
  }
  static boolean handBandUv(int x,int y){return x>=24&&x<40&&y==30;}
  static float wingDistal(int x){return unit(Math.abs(x-37.5f)/5.5f);}
  static boolean paletteProtected(String path,String layout,int x,int y,int width,int height) {
    return path.startsWith("dynamic/hero_swim064/")&&layout.equals("baby")&&width>0&&height>0
        &&HeroRunMarkerPaint056.markerUv(x*128/width,y*64/height);
  }
  static boolean paletteWing(String path,String layout,int x,int y,int width,int height) {
    if(!path.startsWith("dynamic/hero_swim064/")||!layout.equals("baby")||width<=0||height<=0)return false;
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
