package com.chaogarden.client;

/** Semantic paint on the original baby atlas for ordinary Neutral/Run FIRST
 * adults. The independently operated viewer guides these authored color and
 * placement approximations. Original eyes, alpha and inherited finish stay
 * exact, while named/mono/jewel bodies retain their source pigment. */
final class NeutralRunRegionPaint062 {
  record Input(boolean natural,boolean twoTone,float maturity,float runPower,
               float swimFly,int body,int accent,int wing,int marker) {}
  record Colors(int body,int limb,int hand,int foot,int belly,int wing,int wingTip,
                int tail,int marker,int featureBody,int featureTip,int fin,int underside) {}
  private NeutralRunRegionPaint062() {}

  // Full signed lattice: Run/balanced/Power across, Swim/balanced/Fly down.
  // Power is a valid opposite secondary influence: every entry remains Run
  // FIRST type. These colors are eye-estimated, not sampled viewer textures.
  private static final int[] BODY={
      rgb(48,169,247),rgb(18,235,229),rgb(133,171,247),
      rgb(61,47,247),rgb(28,180,239),rgb(233,99,245),
      rgb(98,117,248),rgb(162,178,248),rgb(192,145,243)};
  private static final int[] HAND={
      rgb(26,161,54),rgb(43,205,47),rgb(177,181,47),
      rgb(54,52,38),rgb(28,157,67),rgb(207,98,32),
      rgb(119,116,34),rgb(172,169,48),rgb(190,130,37)};
  private static final int[] FOOT={
      rgb(37,112,241),rgb(25,161,239),rgb(174,115,242),
      rgb(64,49,245),rgb(37,115,240),rgb(230,86,237),
      rgb(135,73,242),rgb(165,76,239),rgb(194,95,239)};
  private static final int[] CHEST={
      rgb(29,169,54),rgb(35,216,32),rgb(170,178,42),
      rgb(53,51,36),rgb(25,162,56),rgb(208,96,31),
      rgb(115,112,29),rgb(167,166,40),rgb(190,127,31)};
  private static final int[] MARKING={
      rgb(35,148,232),rgb(24,216,213),rgb(28,158,234),
      rgb(43,43,229),rgb(17,157,225),rgb(68,94,236),
      rgb(46,101,238),rgb(39,161,236),rgb(46,129,234)};
  private static final int[] WING_ROOT={
      rgb(47,158,232),rgb(24,220,212),rgb(126,157,231),
      rgb(53,47,226),rgb(31,162,220),rgb(220,99,226),
      rgb(98,107,231),rgb(150,166,232),rgb(180,131,226)};
  private static final int[] WING_TIP={
      rgb(39,136,222),rgb(43,203,206),rgb(108,141,227),
      rgb(51,49,208),rgb(42,142,213),rgb(192,91,219),
      rgb(83,98,218),rgb(124,151,225),rgb(160,122,220)};
  private static final int[] TAIL={
      rgb(45,155,229),rgb(62,217,45),rgb(152,173,231),
      rgb(51,45,228),rgb(35,160,224),rgb(211,94,230),
      rgb(85,114,231),rgb(136,168,235),rgb(167,143,231)};
  private static final int[] ORB={
      rgb(47,145,241),rgb(26,219,225),rgb(149,160,244),
      rgb(70,44,243),rgb(25,148,239),rgb(211,76,237),
      rgb(107,90,244),rgb(153,146,244),rgb(191,115,239)};
  private static final int YOUNG_BODY=rgb(150,245,21),YOUNG_HAND=rgb(124,197,27),
      YOUNG_FOOT=rgb(175,171,32),YOUNG_CHEST=rgb(123,195,24),
      YOUNG_MARKING=rgb(72,223,26),YOUNG_WING=rgb(130,219,25),
      YOUNG_WING_TIP=rgb(102,201,22),YOUNG_TAIL=rgb(126,211,28),
      YOUNG_ORB=rgb(161,239,22);

  static Colors colors(Input in) {
    float age=unit(in.maturity),rp=axis(in.runPower),sf=axis(in.swimFly);
    int body=in.body,hand,foot,belly,wing,wingTip,tail,marking;
    if(in.natural&&in.twoTone) {
      body=blend(YOUNG_BODY,corner(BODY,rp,sf),age);
      hand=blend(YOUNG_HAND,corner(HAND,rp,sf),age);
      foot=blend(YOUNG_FOOT,corner(FOOT,rp,sf),age);
      belly=blend(YOUNG_CHEST,corner(CHEST,rp,sf),age);
      marking=blend(YOUNG_MARKING,corner(MARKING,rp,sf),age);
      wing=blend(YOUNG_WING,corner(WING_ROOT,rp,sf),age);
      wingTip=blend(YOUNG_WING_TIP,corner(WING_TIP,rp,sf),age);
      tail=blend(YOUNG_TAIL,corner(TAIL,rp,sf),age);
    } else {
      // The same small markings and extremity regions use inherited accents
      // or bounded value contrast; no named body is repainted lime/blue/pink.
      boolean contrasting=in.twoTone&&distance(body,in.accent)>=18;
      int accent=contrasting?in.accent:tone(body,.78f);
      hand=accent;foot=lighten(accent,.10f);
      belly=contrasting?accent:tone(body,.77f);
      marking=contrasting?blend(body,accent,.65f):tone(body,.68f);
      wing=contrasting?accent:tone(body,.76f);
      wingTip=lighten(wing,.12f);tail=contrasting?accent:tone(body,.80f);
    }
    int marker=marker(age,rp,sf);
    return new Colors(body,hand,hand,foot,belly,wing,wingTip,tail,marker,
        body,marking,wing,wingTip);
  }

  /** Four point swatches plus a verified-unused 7x2 longitudinal stripe tile.
   * Front nubs and tail ends retain their separate original hand/tail islands.
   * All 18 added-alpha pixels are scoped to this new material only. */
  static int reservedKind(int x,int y) {
    if(stripeTile(x,y))return 1;
    if(y!=63)return 0;
    return switch(x){case 127,124->1;case 126,125->3;default->0;};
  }
  static int pixel(Input in,Colors c,int x,int y,int source,int flags) {
    if(stripeTile(x,y))return stripeMark(x,y)?c.featureTip:c.featureBody;
    if(reservedKind(x,y)!=0)return switch(x){case 127->c.featureBody;case 126->c.featureTip;case 125->c.fin;default->c.underside;};
    if(source>>>24==0||(flags&HeroRunRegionPaint055.PROTECTED)!=0)return source;
    if((flags&HeroRunRegionPaint055.MARKER)!=0)return source&0xff000000|c.marker&0xffffff;
    if((flags&HeroRunRegionPaint055.WING)!=0)
      return tint(source,blend(c.wing,c.wingTip,wingDistal(x)),in.wing);
    if((flags&HeroRunRegionPaint055.SKIN)==0)return source;
    boolean accent=(flags&HeroRunRegionPaint055.ACCENT)!=0;
    int target=accent?c.limb:c.body,reference=accent?in.accent:in.body;
    if((flags&HeroRunRegionPaint055.TAIL)!=0)target=c.tail;
    else if((flags&HeroRunRegionPaint055.LIMB)!=0&&footUv(x,y))
      target=footHeelUv(x,y)?c.hand:footToeUv(x,y)?c.foot:c.body;
    else if(accent&&(flags&HeroRunRegionPaint055.LIMB)!=0) {
      int band=HeroNormalRegionPaint057.limbBand(x,y);
      if(band==1||band==3)target=c.hand;
    } else if(headTopMarkingUv(x,y))target=c.featureTip;
    else if(bellyUv(x,y))target=c.belly;
    return tint(source,target,reference);
  }
  /** Three simple longitudinal strokes on the original skull TOP only.
   * Front and side faces keep their normal body paint. The separate central
   * crest continues the same marking color through its small opaque UV tile. */
  static boolean headTopMarkingUv(int x,int y) {
    return y>=0&&y<12&&(x==17||x==19||x==21);
  }
  static boolean stripeTile(int x,int y){return x>=112&&x<119&&y>=32&&y<34;}
  static boolean stripeMark(int x,int y){return stripeTile(x,y)&&((x-112)&1)==1;}
  static boolean bellyUv(int x,int y) {
    return x>=9&&x<15&&y>=35&&y<41&&(y!=35&&y!=40||x>=10&&x<14);
  }
  static boolean footUv(int x,int y){return x>=44&&x<68&&y>=18&&y<30;}
  /** Original closed foot's front plane plus the short top/sole/side toe band. */
  static boolean footToeUv(int x,int y) {
    return x>=52&&x<56&&y>=26&&y<30
        ||x>=52&&x<60&&y>=24&&y<26
        ||y>=26&&y<30&&(x>=50&&x<52||x>=56&&x<58);
  }
  static boolean footHeelUv(int x,int y){return x>=65&&x<67&&y>=27&&y<29;}
  static float wingDistal(int x){return unit(Math.abs(x-37.5f)/5.5f);}
  static boolean paletteProtected(String path,String layout,int x,int y,int width,int height) {
    return path.startsWith("dynamic/neutral_run062/")&&layout.equals("baby")&&width>0&&height>0
        &&HeroRunMarkerPaint056.markerUv(x*128/width,y*64/height);
  }
  static boolean paletteWing(String path,String layout,int x,int y,int width,int height) {
    if(!path.startsWith("dynamic/neutral_run062/")||!layout.equals("baby")||width<=0||height<=0)return false;
    int u=x*128/width,v=y*64/height;return v==63&&(u==124||u==125);
  }
  static int marker(float maturity,float rp,float sf){return blend(YOUNG_ORB,corner(ORB,rp,sf),maturity);}
  private static int corner(int[] values,float rp,float sf) {
    rp=axis(rp);sf=axis(sf);int result=0xff000000;
    for(int c=0;c<3;c++) {
      float v=0;
      for(int row=0;row<3;row++)for(int x=0;x<3;x++)
        v+=(values[row*3+x]>>>(c*8)&255)*Math.max(0,1-Math.abs(rp-(x-1)))*Math.max(0,1-Math.abs(sf-(row-1)));
      result|=Math.clamp(Math.round(v),0,255)<<(c*8);
    }
    return result;
  }
  /** Per-channel ratios preserve the source atlas's light/dark pixel detail. */
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
