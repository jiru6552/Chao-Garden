package com.chaogarden.client;

/** Semantic paint on the original baby atlas for Neutral/Fly FIRST adults.
 * Independently viewed references guide the authored RGB approximations and
 * blocky boundaries. Original facial pixels, alpha and inherited finish stay
 * exact; genetic bodies retain their source pigment. */
final class NeutralFlyRegionPaint061 {
  record Input(boolean natural,boolean twoTone,float maturity,float runPower,
               float swimFly,int body,int accent,int wing,int marker) {}
  record Colors(int body,int limb,int hand,int foot,int belly,int wing,int wingTip,
                int tail,int marker,int featureBody,int featureTip,int fin,int underside) {}
  private NeutralFlyRegionPaint061() {}

  // Run/balanced/Power across; balanced/additional Fly down. All six remain
  // Fly FIRST type. Opposing Swim training recedes to balanced, never Swim.
  private static final int[] BODY={
      rgb(247,177,208),rgb(236,167,239),rgb(249,57,198),
      rgb(227,167,236),rgb(170,150,232),rgb(239,109,218)};
  private static final int[] CUFF={
      rgb(150,87,216),rgb(139,88,217),rgb(125,53,178),
      rgb(154,101,227),rgb(146,135,224),rgb(147,88,213)};
  private static final int[] HAND={
      rgb(82,124,243),rgb(101,116,232),rgb(93,108,228),
      rgb(78,123,233),rgb(64,94,212),rgb(80,106,231)};
  private static final int[] FOOT={
      rgb(241,183,71),rgb(249,173,68),rgb(243,61,22),
      rgb(249,179,68),rgb(247,187,37),rgb(252,118,37)};
  private static final int[] CHEST={
      rgb(244,184,81),rgb(247,174,80),rgb(246,63,37),
      rgb(244,176,92),rgb(246,184,63),rgb(240,139,56)};
  private static final int[] WING_ROOT={
      rgb(29,232,211),rgb(250,197,205),rgb(252,148,82),
      rgb(45,229,219),rgb(250,248,215),rgb(247,223,165)};
  private static final int[] WING_TIP={
      rgb(48,237,39),rgb(249,171,28),rgb(255,125,14),
      rgb(199,223,93),rgb(248,229,39),rgb(251,183,71)};
  private static final int[] ORB={
      rgb(251,172,171),rgb(255,167,110),rgb(255,134,113),
      rgb(255,176,92),rgb(255,233,52),rgb(249,172,91)};
  private static final int YOUNG_BODY=rgb(230,164,234),YOUNG_CUFF=rgb(146,77,224),
      YOUNG_TIP=rgb(91,125,236),YOUNG_FOOT=rgb(241,170,61),
      YOUNG_CHEST=rgb(248,176,71),YOUNG_WING=rgb(26,223,211),
      YOUNG_WING_TIP=rgb(53,239,36),YOUNG_ORB=rgb(255,173,142);

  static Colors colors(Input in) {
    float age=unit(in.maturity),rp=axis(in.runPower),fly=unit(in.swimFly);
    int body=in.body,limb,hand,foot,belly,wing,wingTip;
    if(in.natural&&in.twoTone) {
      body=blend(YOUNG_BODY,corner(BODY,rp,fly),age);
      limb=blend(YOUNG_CUFF,corner(CUFF,rp,fly),age);
      hand=blend(YOUNG_TIP,corner(HAND,rp,fly),age);
      foot=blend(YOUNG_FOOT,corner(FOOT,rp,fly),age);
      belly=blend(YOUNG_CHEST,corner(CHEST,rp,fly),age);
      wing=blend(YOUNG_WING,corner(WING_ROOT,rp,fly),age);
      wingTip=blend(YOUNG_WING_TIP,corner(WING_TIP,rp,fly),age);
    } else {
      // Named/mono/jewel bodies keep their source pigment. Existing inherited
      // accents or bounded value contrast describe the SAME semantic regions.
      boolean contrasting=in.twoTone&&distance(body,in.accent)>=18;
      int accent=contrasting?in.accent:tone(body,.78f);
      limb=accent;hand=blend(accent,tone(accent,.86f),age);
      foot=blend(accent,lighten(accent,.12f),age);
      belly=contrasting?accent:lighten(body,.14f);
      wing=contrasting?accent:tone(body,.76f);
      wingTip=lighten(wing,.12f);
    }
    // Smooth original orb, with a species pigment independent of all inherited
    // body/wing colors and shiny genes, including intermediate/receding axes.
    int marker=marker(age,rp,fly);
    return new Colors(body,limb,hand,foot,belly,wing,wingTip,body,marker,
        body,hand,wing,wingTip);
  }

  /** Only these four originally unused texels gain alpha in this new profile.
   * 127=body, 126=crest/hand tip, 125=wing root, 124=wing distal surface. */
  static int reservedKind(int x,int y) {
    if(y!=63)return 0;
    return switch(x){case 127,124->1;case 126,125->3;default->0;};
  }
  static int pixel(Input in,Colors c,int x,int y,int source,int flags) {
    if(reservedKind(x,y)!=0)return switch(x){case 127->c.featureBody;case 126->c.featureTip;case 125->c.fin;default->c.underside;};
    if(source>>>24==0||(flags&HeroRunRegionPaint055.PROTECTED)!=0)return source;
    if((flags&HeroRunRegionPaint055.MARKER)!=0)return source&0xff000000|c.marker&0xffffff;
    if((flags&HeroRunRegionPaint055.WING)!=0)
      return tint(source,blend(c.wing,c.wingTip,wingDistal(x)),in.wing);
    if((flags&HeroRunRegionPaint055.SKIN)==0)return source;
    boolean accent=(flags&HeroRunRegionPaint055.ACCENT)!=0;
    int target=accent?c.limb:c.body,reference=accent?in.accent:in.body;
    if((flags&HeroRunRegionPaint055.TAIL)!=0)target=c.tail;
    else if(accent&&(flags&HeroRunRegionPaint055.LIMB)!=0) {
      if(footUv(x,y))target=c.foot;
      else {
        int band=HeroNormalRegionPaint057.limbBand(x,y);
        if(band==1||band==3)target=c.hand;
      }
    } else if(bellyUv(x,y))target=c.belly;
    return tint(source,target,reference);
  }
  /** Compact flat gold/red chest patch on the unchanged torso front plane. */
  static boolean bellyUv(int x,int y) {
    return x>=10&&x<14&&y>=35&&y<41&&(y!=35&&y!=40||x==11||x==12);
  }
  static boolean footUv(int x,int y){return x>=44&&x<68&&y>=18&&y<30;}
  /** Original two-sided wing island: center root, mirrored distal ends. */
  static float wingDistal(int x){return unit(Math.abs(x-37.5f)/5.5f);}
  static boolean paletteProtected(String path,String layout,int x,int y,int width,int height) {
    return path.startsWith("dynamic/neutral_fly061/")&&layout.equals("baby")&&width>0&&height>0
        &&HeroRunMarkerPaint056.markerUv(x*128/width,y*64/height);
  }
  static boolean paletteWing(String path,String layout,int x,int y,int width,int height) {
    if(!path.startsWith("dynamic/neutral_fly061/")||!layout.equals("baby")||width<=0||height<=0)return false;
    int u=x*128/width,v=y*64/height;
    return v==63&&(u==124||u==125);
  }
  static int marker(float maturity,float rp,float sf){return blend(YOUNG_ORB,corner(ORB,rp,sf),maturity);}
  private static int corner(int[] values,float rp,float fly) {
    rp=axis(rp);fly=unit(fly);int result=0xff000000;
    for(int c=0;c<3;c++) {
      float v=0;
      for(int row=0;row<2;row++)for(int x=0;x<3;x++)
        v+=(values[row*3+x]>>>(c*8)&255)*Math.max(0,1-Math.abs(rp-(x-1)))*(row==0?1-fly:fly);
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
