package com.chaogarden.client;

/** Power-FIRST Hero adult regional paint on the retained canonical atlas.
 * Actual independently operated viewer captures guide these authored colors
 * and flat patches. No viewer texture, geometry or timing formula is imported.
 * The original plain skull, facial pixels, shading and finish stay intact. */
final class HeroPowerRegionPaint067 {
  record Input(boolean natural,boolean twoTone,float maturity,float runPower,
               float swimFly,int body,int accent,int wing,int marker) {}
  record Colors(int body,int limb,int hand,int palm,int spot,int foot,
                int bib,int bibEdge,int wing,int wingTip,int tail,int marker,
                int featureBody,int featureMid,int featureTip) {}
  private HeroPowerRegionPaint067() {}

  // Run / balanced / Power across; Swim / balanced / Fly down. Natural
  // Power-first forms have spotted gloves/crowns and an upper bib, not stripes
  // or a large belly oval. Rounded spots become a few clean blocky patches.
  private static final int[] HAND={
      rgb(249,133,186),rgb(247,74,200),rgb(249,122,162),
      rgb(250,157,173),rgb(251,184,80),rgb(250,111,126),
      rgb(233,151,214),rgb(191,126,248),rgb(238,138,185)};
  private static final int[] SPOT={
      rgb(231,70,148),rgb(222,11,174),rgb(237,30,102),
      rgb(240,112,135),rgb(246,150,25),rgb(247,36,29),
      rgb(205,100,186),rgb(182,81,239),rgb(214,53,142)};
  private static final int[] PALM={
      rgb(172,115,144),rgb(190,69,149),rgb(214,124,130),
      rgb(214,172,114),rgb(219,160,43),rgb(202,130,11),
      rgb(158,128,160),rgb(139,114,197),rgb(185,137,116)};
  private static final int[] FOOT={
      rgb(204,145,178),rgb(206,87,183),rgb(230,145,164),
      rgb(242,215,163),rgb(245,200,65),rgb(251,176,29),
      rgb(180,155,189),rgb(153,154,223),rgb(214,161,139)};
  private static final int[] CAP_TIP={
      rgb(214,129,169),rgb(167,72,174),rgb(228,78,132),
      rgb(249,211,162),rgb(255,199,61),rgb(255,158,34),
      rgb(181,140,183),rgb(129,128,220),rgb(196,139,145)};
  private static final int[] BIB={
      rgb(246,84,166),rgb(250,9,169),rgb(249,39,113),
      rgb(254,137,157),rgb(255,158,14),rgb(252,34,38),
      rgb(218,125,193),rgb(188,82,248),rgb(218,52,136)};
  private static final int[] BIB_EDGE={
      rgb(229,194,220),rgb(113,122,248),rgb(168,164,115),
      rgb(252,218,156),rgb(255,231,40),rgb(247,216,17),
      rgb(231,193,226),rgb(224,153,248),rgb(244,168,144)};
  private static final int[] WING_ROOT={
      rgb(154,145,85),rgb(125,112,153),rgb(34,165,91),
      rgb(222,140,24),rgb(234,151,24),rgb(87,201,12),
      rgb(150,133,142),rgb(43,40,202),rgb(23,139,88)};
  private static final int[] WING_TIP={
      rgb(189,204,112),rgb(58,205,188),rgb(123,224,79),
      rgb(253,206,18),rgb(255,215,15),rgb(186,239,15),
      rgb(167,191,112),rgb(13,187,166),rgb(111,202,90)};
  private static final int[] HALO={
      rgb(159,196,169),rgb(22,142,245),rgb(102,212,158),
      rgb(255,247,96),rgb(254,232,8),rgb(160,236,12),
      rgb(156,198,171),rgb(25,145,245),rgb(98,209,151)};
  private static final int YOUNG_HAND=rgb(252,124,141),
      YOUNG_SPOT=rgb(244,52,65),YOUNG_PALM=rgb(199,82,88),
      YOUNG_FOOT=rgb(237,130,147),YOUNG_TIP=rgb(250,67,75),
      YOUNG_BIB=rgb(250,103,118),YOUNG_BIB_EDGE=rgb(255,166,172),
      YOUNG_WING_ROOT=rgb(89,205,11),YOUNG_WING_TIP=rgb(188,239,19),
      YOUNG_HALO=rgb(244,135,248);
  private static final int[] PATCH_KINDS={0,0,1,0, 1,0,0,0, 1,1,0,2, 0,0,2,2};

  static Colors colors(Input in) {
    float age=unit(in.maturity),rp=axis(in.runPower),sf=axis(in.swimFly);
    int body=in.body,hand,palm,spot,foot,bib,edge,wing,wingTip,tip;
    if(in.natural&&in.twoTone) {
      hand=blend(YOUNG_HAND,corner(HAND,rp,sf),age);
      palm=blend(YOUNG_PALM,corner(PALM,rp,sf),age);
      spot=blend(YOUNG_SPOT,corner(SPOT,rp,sf),age);
      foot=blend(YOUNG_FOOT,corner(FOOT,rp,sf),age);
      tip=blend(YOUNG_TIP,corner(CAP_TIP,rp,sf),age);
      bib=blend(YOUNG_BIB,corner(BIB,rp,sf),age);
      edge=blend(YOUNG_BIB_EDGE,corner(BIB_EDGE,rp,sf),age);
      // Actual half-age wings remain mostly green before mature Fly becomes
      // blue/teal. This delayed ramp is authored, not a recovered source clock.
      wing=blend(YOUNG_WING_ROOT,corner(WING_ROOT,rp,sf),age*age);
      wingTip=blend(YOUNG_WING_TIP,corner(WING_TIP,rp,sf),age*age);
    } else {
      boolean contrast=in.twoTone&&distance(body,in.accent)>=18;
      hand=contrast?in.accent:tone(body,.78f);
      palm=contrast?lighten(in.accent,.16f):tone(body,.88f);
      spot=contrast?tone(in.accent,.70f):tone(body,.62f);
      foot=contrast?lighten(in.accent,.22f):lighten(body,.10f);
      tip=contrast?lighten(in.accent,.25f):lighten(body,.17f);
      bib=hand;edge=contrast?lighten(in.accent,.30f):tone(body,.90f);
      wing=contrast?lighten(in.accent,.12f):tone(body,.82f);
      wingTip=contrast?in.accent:tone(body,.72f);
    }
    return new Colors(body,hand,hand,palm,spot,foot,bib,edge,wing,wingTip,
        bib,halo(age,rp,sf),body,hand,tip);
  }

  /** Four-by-four patch tile only on separate colored crown caps. The white
   * root parts and original head use their own unchanged UVs. */
  static boolean patchTile(int x,int y){return x>=116&&x<120&&y>=60&&y<64;}
  static int patchKind(int x,int y) {
    if(!patchTile(x,y))return -1;
    return PATCH_KINDS[(y-60)*4+x-116];
  }
  static int patchColor(Colors c,int x,int y) {
    return switch(patchKind(x,y)){case 0->c.featureMid;case 1->c.spot;case 2->c.featureTip;default->throw new IllegalArgumentException("Outside HeroPower067 patch tile");};
  }
  static int reservedKind(int x,int y) {
    if(patchTile(x,y))return 3;
    if(y!=63)return 0;
    return switch(x){case 127,123,124->1;case 125,126->3;default->0;};
  }
  static int pixel(Input in,Colors c,int x,int y,int source,int flags) {
    if(patchTile(x,y))return patchColor(c,x,y);
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
    else if(torsoUv(x,y))target=bibUv(x,y)?bibColor(c,x,y):c.body;
    else if((flags&HeroRunRegionPaint055.LIMB)!=0) {
      if(handUv(x,y))target=handColor(c,x,y);
      else if(footUv(x,y))target=footColor(c,x,y);
      else target=shoulderSpotUv(x,y)?c.spot:c.body;
    }
    return tint(source,target,reference);
  }
  static boolean torsoUv(int x,int y){return x>=0&&x<32&&y>=34&&y<42||x>=8&&x<24&&y>=26&&y<34;}
  static boolean bibUv(int x,int y) {
    if(y<34||y>38)return false;
    if(x>=8&&x<16){int half=y<=34?3:y==35?2:y<=37?1:0;return half>0&&x>=12-half&&x<12+half;}
    if(x>=24&&x<32){int half=Math.max(1,4-(y-34));return x>=28-half&&x<28+half;}
    return y==34&&(x<8||x>=16&&x<24);
  }
  static int bibColor(Colors c,int x,int y) {
    return y>=36&&((x>=8&&x<16&&y==37)||(x>=24&&x<32&&y==38))?c.bibEdge:c.bib;
  }
  static boolean handUv(int x,int y){return x>=24&&x<40&&y>=30&&y<34||x>=28&&x<36&&y>=26&&y<30;}
  static boolean shoulderSpotUv(int x,int y){return y==10&&(x==3||x==4||x==9||x==10);}
  static int handColor(Colors c,int x,int y) {
    int u=Math.floorMod(x,4),v=Math.floorMod(y-(y>=30?30:26),4);
    if(x>=28&&x<32&&y>=30&&u>=1&&u<=2&&v>=1&&v<=2)return c.palm;
    return regionalPatch(c.hand,c.spot,c.palm,u,v);
  }
  static boolean footUv(int x,int y){return x>=44&&x<68&&y>=26&&y<30||x>=52&&x<60&&y>=18&&y<26;}
  static boolean whiteHeelUv(int x,int y) {
    if(!footUv(x,y))return false;
    return x>=64&&y>=26||x>=44&&x<48&&y>=26
        ||x>=60&&x<64&&y>=26||y<22;
  }
  static int footColor(Colors c,int x,int y) {
    if(whiteHeelUv(x,y))return c.body;
    int u=Math.floorMod(x,4),v=Math.floorMod(y-(y>=26?26:22),4);
    if(x>=52&&x<56&&y>=26&&u>=1&&u<=2&&v>=1&&v<=2)return c.foot;
    return regionalPatch(c.hand,c.spot,c.foot,u,v);
  }
  private static int regionalPatch(int base,int spot,int inset,int u,int v) {
    if(u==0&&v<=1||u==3&&v>=2)return spot;
    if(u>=1&&u<=2&&v>=1&&v<=2)return inset;
    return base;
  }
  static boolean tailTipUv(int x,int y) {
    return x>=64&&x<68&&y>=34&&y<38||x>=62&&x<64&&y>=34&&y<38
        ||x>=52&&x<54&&y>=34&&y<38||x>=56&&x<64&&y>=30&&y<32;
  }
  static int tailColor(Colors c,int x,int y){return tailTipUv(x,y)?c.tail:c.body;}
  static float wingDistal(int x){return unit(Math.abs(x-37.5f)/5.5f);}
  static boolean paletteProtected(String path,String layout,int x,int y,int width,int height) {
    return path.startsWith("dynamic/hero_power067/")&&layout.equals("baby")&&width>0&&height>0
        &&HeroRunMarkerPaint056.markerUv(x*128/width,y*64/height);
  }
  static boolean paletteWing(String path,String layout,int x,int y,int width,int height) {
    if(!path.startsWith("dynamic/hero_power067/")||!layout.equals("baby")||width<=0||height<=0)return false;
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
