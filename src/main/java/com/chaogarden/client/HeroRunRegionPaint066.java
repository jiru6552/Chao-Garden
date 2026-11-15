package com.chaogarden.client;

/** Run-FIRST Hero adult pigments on the retained canonical baby atlas.
 * The independently operated viewer supplies visual hue/region guidance, not
 * texture data or morph formulas. Original skull/face texels stay unchanged. */
final class HeroRunRegionPaint066 {
  record Input(boolean natural,boolean twoTone,float maturity,float runPower,
               float swimFly,int body,int accent,int wing,int marker) {}
  record Colors(int body,int limb,int primary,int secondary,int bellyRim,int bellyCore,
                int wing,int wingTip,int tail,int marker,int featureBody,
                int featureMid,int featureTip) {}
  private HeroRunRegionPaint066() {}

  // Run / balanced / Power across; Swim / balanced / Fly down. These are
  // authored estimates from fresh actual Run-checkbox viewer captures.
  private static final int[] PRIMARY={
      rgb(68,69,237),rgb(132,27,235),rgb(159,110,233),
      rgb(25,139,241),rgb(31,166,243),rgb(158,143,242),
      rgb(76,108,243),rgb(175,122,228),rgb(167,131,238)};
  private static final int[] SECONDARY={
      rgb(8,240,186),rgb(18,251,91),rgb(163,235,105),
      rgb(7,236,218),rgb(245,225,106),rgb(246,223,140),
      rgb(142,215,250),rgb(236,150,226),rgb(246,176,177)};
  private static final int[] BELLY_RIM={
      rgb(248,236,57),rgb(246,233,86),rgb(240,231,127),
      rgb(250,234,11),rgb(66,176,246),rgb(246,226,127),
      rgb(252,208,149),rgb(245,179,236),rgb(248,194,188)};
  private static final int[] BELLY_CORE={
      rgb(66,196,176),rgb(78,213,165),rgb(111,192,145),
      rgb(80,185,203),rgb(91,24,184),rgb(205,176,110),
      rgb(163,164,188),rgb(207,115,151),rgb(194,131,124)};
  private static final int[] WING_ROOT={
      rgb(17,180,120),rgb(12,216,64),rgb(146,200,91),
      rgb(31,126,123),rgb(244,146,28),rgb(240,162,56),
      rgb(78,128,176),rgb(91,192,191),rgb(144,179,153)};
  private static final int[] WING_TIP={
      rgb(24,226,152),rgb(24,250,115),rgb(198,232,107),
      rgb(65,174,146),rgb(253,208,14),rgb(252,193,68),
      rgb(109,166,194),rgb(190,103,183),rgb(226,142,112)};
  private static final int[] HALO={
      rgb(240,246,104),rgb(178,239,8),rgb(239,209,16),
      rgb(253,250,141),rgb(164,239,9),rgb(250,147,15),
      rgb(184,196,207),rgb(93,108,249),rgb(183,129,152)};
  private static final int YOUNG_PRIMARY=rgb(32,165,243),
      YOUNG_SECONDARY=rgb(245,224,109),YOUNG_BELLY_RIM=rgb(65,176,245),
      YOUNG_BELLY_CORE=rgb(93,24,184),YOUNG_WING_ROOT=rgb(245,147,29),
      YOUNG_WING_TIP=rgb(254,207,16),YOUNG_HALO=rgb(249,235,35);

  static Colors colors(Input in) {
    float age=unit(in.maturity),rp=axis(in.runPower),sf=axis(in.swimFly);
    int body=in.body,primary,secondary,rim,core,wing,wingTip;
    if(in.natural&&in.twoTone) {
      primary=blend(YOUNG_PRIMARY,corner(PRIMARY,rp,sf),age);
      secondary=blend(YOUNG_SECONDARY,corner(SECONDARY,rp,sf),age);
      rim=blend(YOUNG_BELLY_RIM,corner(BELLY_RIM,rp,sf),age);
      core=blend(YOUNG_BELLY_CORE,corner(BELLY_CORE,rp,sf),age);
      wing=blend(YOUNG_WING_ROOT,corner(WING_ROOT,rp,sf),age);
      wingTip=blend(YOUNG_WING_TIP,corner(WING_TIP,rp,sf),age);
    } else {
      // Same stripes/regions for every coat; inherited color and finish are
      // retained rather than replacing jewel or mono skin with natural white.
      boolean contrast=in.twoTone&&distance(body,in.accent)>=18;
      primary=contrast?in.accent:tone(body,.64f);
      secondary=contrast?lighten(in.accent,.25f):lighten(body,.17f);
      rim=primary;core=contrast?blend(body,in.accent,.72f):tone(body,.78f);
      wing=contrast?lighten(in.accent,.12f):tone(body,.82f);
      wingTip=contrast?in.accent:tone(body,.72f);
    }
    return new Colors(body,primary,primary,secondary,rim,core,wing,wingTip,
        core,halo(age,rp,sf),body,primary,secondary);
  }

  /** Four longitudinal stripe texels plus five point swatches. The tile is
   * source-transparent and used only by this profile's separate head parts.
   * Original atlas files and the original head UVs are never changed. */
  static boolean stripeTile(int x,int y){return y==60&&x>=120&&x<124;}
  static int stripeKind(int x,int y){return !stripeTile(x,y)?-1:(x-120)%2==0?0:x==121?1:2;}
  static int stripeColor(Colors c,int x,int y) {
    return switch(stripeKind(x,y)){case 0->c.body;case 1->c.primary;case 2->c.secondary;default->throw new IllegalArgumentException("Outside HeroRun066 stripe tile");};
  }
  static int reservedKind(int x,int y) {
    if(stripeTile(x,y))return stripeKind(x,y)==0?1:3;
    if(y!=63)return 0;
    return switch(x){case 127,123,124->1;case 125,126->3;default->0;};
  }
  static int pixel(Input in,Colors c,int x,int y,int source,int flags) {
    if(stripeTile(x,y))return stripeColor(c,x,y);
    if(reservedKind(x,y)!=0)return switch(x){
      case 127->c.featureBody;case 125->c.featureMid;case 126->c.featureTip;
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
    else if((flags&HeroRunRegionPaint055.LIMB)!=0)target=limbColor(c,x,y);
    return tint(source,target,reference);
  }
  static boolean torsoUv(int x,int y){return x>=0&&x<32&&y>=34&&y<42||x>=8&&x<24&&y>=26&&y<34;}
  /** Broad blue/purple (or corner-specific) chest region, painted within the
   * retained torso. A small stepped silhouette replaces the source gradient. */
  static boolean bellyUv(int x,int y) {
    if(y<35||y>40)return false;
    int half=y==35?0:y==36?1:2;
    return x>=11-half&&x<=12+half;
  }
  static int bellyColor(Colors c,int x,int y) {
    boolean core=y>=37&&(x==11||x==12)||y>=39&&x>=10&&x<=13;
    return core?c.bellyCore:c.bellyRim;
  }
  /** Three clean two-pixel bands on the original limb UVs. Hands and feet
   * remain original closed shapes; no extra rings or distorted UVs are added. */
  static int limbColor(Colors c,int x,int y) {
    return switch((y/2)%3){case 0->c.body;case 1->c.primary;default->c.secondary;};
  }
  static int tailColor(Colors c,int x,int y) {
    if(x>=56&&x<60&&y>=30&&y<34)return x==56||x==59?c.bellyRim:c.tail;
    if(y==34&&x>=52&&x<64)return c.bellyRim;
    if(y==35&&(x>=52&&x<56||x>=60&&x<64))return c.tail;
    return c.body;
  }
  static float wingDistal(int x){return unit(Math.abs(x-37.5f)/5.5f);}
  static boolean paletteProtected(String path,String layout,int x,int y,int width,int height) {
    return path.startsWith("dynamic/hero_run066/")&&layout.equals("baby")&&width>0&&height>0
        &&HeroRunMarkerPaint056.markerUv(x*128/width,y*64/height);
  }
  static boolean paletteWing(String path,String layout,int x,int y,int width,int height) {
    if(!path.startsWith("dynamic/hero_run066/")||!layout.equals("baby")||width<=0||height<=0)return false;
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
