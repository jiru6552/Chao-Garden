package com.chaogarden.client;

/** Semantic paint on the original baby atlas for ordinary Neutral/Power FIRST
 * adults. Fresh, mature and opposing-training colors are authored estimates
 * from the independently operated viewer. Original facial pixels and alpha
 * stay exact; inherited coats and finish keep their existing source pigment. */
final class NeutralPowerRegionPaint063 {
  record Input(boolean natural,boolean twoTone,float maturity,float runPower,
               float swimFly,int body,int accent,int wing,int marker) {}
  record Colors(int body,int limb,int hand,int foot,int belly,int wing,int wingTip,
                int tail,int marker,int featureBody,int featureTip,int fin,int underside,int back) {}
  private NeutralPowerRegionPaint063() {}

  // Run/balanced/Power across, Swim/balanced/Fly down. All nine entries remain
  // Power FIRST type, including opposite Run. Body colors deliberately stay
  // within the orange/red family: caps, extremities and shape do much of the
  // visual work in the actual Power-first reference.
  private static final int[] BODY={
      rgb(255,102,21),rgb(255,75,22),rgb(255,64,24),
      rgb(255,113,19),rgb(255,108,20),rgb(255,46,24),
      rgb(255,100,17),rgb(255,75,18),rgb(255,59,23)};
  private static final int[] HAND={
      rgb(247,177,27),rgb(236,146,20),rgb(234,107,23),
      rgb(255,191,26),rgb(245,166,24),rgb(234,81,15),
      rgb(245,169,27),rgb(232,142,21),rgb(235,110,18)};
  private static final int[] CAP={
      rgb(255,157,150),rgb(255,106,245),rgb(251,91,164),
      rgb(255,196,24),rgb(255,166,166),rgb(255,105,120),
      rgb(255,164,137),rgb(255,130,197),rgb(255,107,149)};
  private static final int[] CHEST={
      rgb(251,15,125),rgb(245,0,211),rgb(251,0,119),
      rgb(255,51,9),rgb(250,16,131),rgb(238,8,56),
      rgb(250,10,112),rgb(250,8,143),rgb(247,6,91)};
  private static final int[] BACK={
      rgb(145,157,141),rgb(131,109,191),rgb(112,102,154),
      rgb(134,190,37),rgb(141,184,174),rgb(90,73,76),
      rgb(150,171,132),rgb(129,133,176),rgb(116,107,152)};
  private static final int[] WING_ROOT={
      rgb(255,184,55),rgb(255,142,36),rgb(255,113,30),
      rgb(255,205,45),rgb(255,193,47),rgb(255,68,24),
      rgb(255,171,52),rgb(255,143,49),rgb(255,112,36)};
  private static final int[] ORB={
      rgb(255,135,174),rgb(255,94,247),rgb(251,72,147),
      rgb(255,162,36),rgb(255,141,147),rgb(255,44,49),
      rgb(255,139,145),rgb(255,94,163),rgb(255,76,122)};
  private static final int YOUNG_BODY=rgb(255,160,20),YOUNG_HAND=rgb(255,237,30),
      YOUNG_CAP=rgb(255,246,166),YOUNG_CHEST=rgb(248,25,145),
      YOUNG_BACK=rgb(140,225,104),YOUNG_WING=rgb(255,227,78),
      YOUNG_TAIL=rgb(255,40,134),YOUNG_ORB=rgb(255,220,69);

  static Colors colors(Input in) {
    float age=unit(in.maturity),rp=axis(in.runPower),sf=axis(in.swimFly);
    int body=in.body,hand,foot,belly,wing,wingTip,tail,cap,back;
    if(in.natural&&in.twoTone) {
      body=blend(YOUNG_BODY,corner(BODY,rp,sf),age);
      hand=blend(YOUNG_HAND,corner(HAND,rp,sf),age);
      cap=blend(YOUNG_CAP,corner(CAP,rp,sf),age);foot=cap;
      belly=blend(YOUNG_CHEST,corner(CHEST,rp,sf),age);
      back=blend(YOUNG_BACK,corner(BACK,rp,sf),age);
      wing=blend(YOUNG_WING,corner(WING_ROOT,rp,sf),age);wingTip=cap;
      tail=blend(YOUNG_TAIL,corner(CHEST,rp,sf),age);
    } else {
      // Named, mono and jewel bodies keep their underlying color. The same
      // capped feature/hand/foot regions use inherited accents or bounded
      // light/dark contrast; they are never forced to the natural orange.
      boolean contrasting=in.twoTone&&distance(body,in.accent)>=18;
      int accent=contrasting?in.accent:tone(body,.78f);
      hand=accent;cap=lighten(accent,.16f);foot=cap;
      belly=contrasting?accent:tone(body,.76f);
      back=contrasting?blend(body,accent,.72f):tone(body,.70f);
      wing=contrasting?accent:tone(body,.78f);wingTip=cap;
      tail=contrasting?accent:tone(body,.76f);
    }
    // The species orb is independent of inherited body/accent/wing pigment
    // and shiny genes, including intermediate or receding specialization.
    int marker=marker(age,rp,sf);
    return new Colors(body,body,hand,foot,belly,wing,wingTip,tail,marker,
        body,cap,wing,wingTip,back);
  }

  /** Only four source-transparent point swatches gain alpha in this profile.
   * 127=feature body, 126=distal head cap, 125=wing root, 124=wing distal.
   * Power references use capped points, not Run's longitudinal stripe tile. */
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
    // Keep the retained broad tail base body-colored. The new distal box
    // uses its separate original tail texel(50,32), outside this base island.
    if((flags&HeroRunRegionPaint055.TAIL)!=0)target=tailBaseUv(x,y)?c.body:c.tail;
    else if((flags&HeroRunRegionPaint055.LIMB)!=0&&footUv(x,y))
      target=footToeUv(x,y)?c.foot:c.body;
    else if(accent&&(flags&HeroRunRegionPaint055.LIMB)!=0) {
      int band=HeroNormalRegionPaint057.limbBand(x,y);
      if(band==1||band==3)target=c.hand;
    } else if(bellyUv(x,y))target=c.belly;
    else if(backUv(x,y))target=c.back;
    return tint(source,target,reference);
  }
  /** Flat compact front chest patch; no additional sculpted body mesh. */
  static boolean bellyUv(int x,int y) {
    return x>=9&&x<15&&y>=35&&y<41&&(y!=35&&y!=40||x>=10&&x<14);
  }
  /** Inset original torso-back plane; head and limb islands are disjoint. */
  static boolean backUv(int x,int y) {
    return x>=25&&x<31&&y>=34&&y<42&&(y!=34&&y!=41||x>=26&&x<30);
  }
  /** Six original tail-base faces; disjoint from distal sample(50,32),
   * the head, torso, hand and open-interior foot UVs. Source shade stays exact. */
  static boolean tailBaseUv(int x,int y){return x>=52&&x<68&&y>=30&&y<38;}
  static boolean footUv(int x,int y){return x>=44&&x<68&&y>=18&&y<30;}
  /** Broad pale/pink toe caps with a body-colored heel and a simple pixel edge.
   * Source zigzag transitions are simplified rather than adding band geometry. */
  static boolean footToeUv(int x,int y) {
    return x>=52&&x<56&&y>=26&&y<30
        ||x>=52&&x<60&&y>=23&&y<26
        ||y>=26&&y<30&&(x>=49&&x<52||x>=56&&x<59);
  }
  static boolean footHeelUv(int x,int y){return x>=65&&x<67&&y>=27&&y<29;}
  static float wingDistal(int x){return unit(Math.abs(x-37.5f)/5.5f);}
  static boolean paletteProtected(String path,String layout,int x,int y,int width,int height) {
    return path.startsWith("dynamic/neutral_power063/")&&layout.equals("baby")&&width>0&&height>0
        &&HeroRunMarkerPaint056.markerUv(x*128/width,y*64/height);
  }
  static boolean paletteWing(String path,String layout,int x,int y,int width,int height) {
    if(!path.startsWith("dynamic/neutral_power063/")||!layout.equals("baby")||width<=0||height<=0)return false;
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
  /** Channel ratios retain the original atlas's light/dark pixel details. */
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
