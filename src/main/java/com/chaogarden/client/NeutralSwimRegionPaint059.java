package com.chaogarden.client;

/** Small semantic regions on the original baby atlas for Neutral/Swim FIRST
 * type adults only. Viewer images guide placement and relative hue; the blocky
 * pixels/RGB approximations and inherited-coat contrasts are authored here.
 * The retained skull, facial atlas, source alpha and inherited finish stay put. */
final class NeutralSwimRegionPaint059 {
  record Input(boolean natural,boolean twoTone,float maturity,float runPower,
               float swimFly,int body,int accent,int wing,int marker) {}
  record Colors(int body,int limb,int foot,int face,int belly,int forehead,
                int wing,int tail,int marker,int featureBody,int featureTip,
                int fin,int underside) {}
  private NeutralSwimRegionPaint059() {}

  // Grid is Run/balanced/Power across, additional Swim/balanced down.
  // All endpoints remain Swim FIRST type. No Fly, Run-first or Power-first
  // palette can leak into this scoped material by raising an opposing stat.
  private static final int[] BODY={
      rgb(64,239,35),rgb(118,244,24),rgb(153,239,28),
      rgb(18,238,39),rgb(157,247,28),rgb(167,239,20)};
  private static final int[] FACE={
      rgb(87,227,222),rgb(189,246,233),rgb(219,235,166),
      rgb(26,218,221),rgb(239,233,161),rgb(249,225,34)};
  private static final int[] LIMB={
      rgb(222,231,59),rgb(223,235,61),rgb(242,226,51),
      rgb(29,222,211),rgb(252,239,44),rgb(251,224,26)};
  private static final int[] FOOT={
      rgb(234,226,66),rgb(249,236,42),rgb(255,193,57),
      rgb(151,156,221),rgb(250,235,43),rgb(255,133,24)};
  private static final int[] FOREHEAD={
      rgb(32,236,208),rgb(31,234,179),rgb(48,229,30),
      rgb(28,241,219),rgb(36,235,175),rgb(40,225,24)};
  private static final int[] FIN={
      rgb(193,237,46),rgb(204,240,41),rgb(228,235,58),
      rgb(28,232,56),rgb(250,237,75),rgb(250,228,36)};
  private static final int[] WING={
      rgb(85,148,194),rgb(42,169,214),rgb(112,151,177),
      rgb(154,157,214),rgb(49,169,214),rgb(224,140,154)};
  private static final int[] TAIL={
      rgb(63,152,201),rgb(31,172,218),rgb(172,204,129),
      rgb(150,160,222),rgb(42,175,216),rgb(252,221,36)};
  private static final int[] ORB={
      rgb(131,241,233),rgb(195,253,244),rgb(221,248,167),
      rgb(33,216,228),rgb(190,249,163),rgb(220,250,39)};
  private static final int YOUNG_BODY=rgb(159,246,28),YOUNG_FACE=rgb(239,234,158),
      YOUNG_LIMB=rgb(249,237,49),YOUNG_FOREHEAD=rgb(37,237,174),
      YOUNG_WING=rgb(48,169,213),YOUNG_ORB=rgb(29,229,135);

  static Colors colors(Input in) {
    float age=unit(in.maturity),rp=axis(in.runPower),swim=swim(in.swimFly);
    boolean natural=in.natural&&in.twoTone;
    int body=in.body,limb,foot,face,belly,forehead,wing,tail,fin;
    if(natural) {
      body=blend(YOUNG_BODY,corner(BODY,rp,swim),age);
      face=blend(YOUNG_FACE,corner(FACE,rp,swim),age);
      belly=face;
      limb=blend(YOUNG_LIMB,corner(LIMB,rp,swim),age);
      foot=blend(YOUNG_LIMB,corner(FOOT,rp,swim),age);
      forehead=blend(YOUNG_FOREHEAD,corner(FOREHEAD,rp,swim),age);
      wing=blend(YOUNG_WING,corner(WING,rp,swim),age);
      tail=blend(YOUNG_WING,corner(TAIL,rp,swim),age);
      fin=blend(YOUNG_LIMB,corner(FIN,rp,swim),age);
    } else {
      // Mono/jewel/named bodies retain their inherited pigment. The SAME UV
      // boundaries use their accent or bounded brightness, never generic lime.
      boolean contrasting=in.twoTone&&distance(body,in.accent)>=18;
      int accent=contrasting?in.accent:tone(body,.78f);
      limb=accent;foot=blend(accent,lighten(accent,.12f),age);
      face=contrasting?lighten(accent,.22f):lighten(body,.16f);
      belly=face;forehead=contrasting?accent:tone(body,.72f);
      wing=contrasting?accent:tone(body,.76f);
      tail=wing;fin=contrasting?accent:tone(body,.84f);
    }
    int marker=blend(YOUNG_ORB,corner(ORB,rp,swim),age);
    return new Colors(body,limb,foot,face,belly,forehead,wing,tail,marker,
        body,forehead,fin,face);
  }

  /** Four previously unused texels, scoped to this new profile. Existing
   * .55-.58 profiles retain exactly their original two-swatch interpretation. */
  static int reservedKind(int x,int y) {
    if(y!=63)return 0;
    return switch(x){case 127->1;case 126,125->3;case 124->1;default->0;};
  }
  static int pixel(Input in,Colors c,int x,int y,int source,int flags) {
    if(reservedKind(x,y)!=0)return switch(x){case 127->c.featureBody;case 126->c.featureTip;case 125->c.fin;default->c.underside;};
    if(source>>>24==0||(flags&HeroRunRegionPaint055.PROTECTED)!=0)return source;
    if((flags&HeroRunRegionPaint055.MARKER)!=0)return source&0xff000000|c.marker&0xffffff;
    if((flags&HeroRunRegionPaint055.WING)!=0)return tint(source,c.wing,in.wing);
    if((flags&HeroRunRegionPaint055.SKIN)==0)return source;
    boolean accent=(flags&HeroRunRegionPaint055.ACCENT)!=0;
    int target=accent?c.limb:c.body,reference=accent?in.accent:in.body;
    if((flags&HeroRunRegionPaint055.TAIL)!=0)target=c.tail;
    else if(accent&&(flags&HeroRunRegionPaint055.LIMB)!=0&&footUv(x,y))target=c.foot;
    else if(lowerHeadUv(x,y))target=c.face;
    else if(foreheadUv(x,y))target=c.forehead;
    else if(bellyUv(x,y))target=c.belly;
    return tint(source,target,reference);
  }
  /** Original skull's four side faces share the twelve pixel vertical range;
   * the bottom face occupies its own closed original cube UV island. The
   * face/iris overlay uses disjoint protected coordinates and is never painted. */
  static boolean lowerHeadUv(int x,int y) {
    return x>=0&&x<52&&y>=20&&y<24||x>=26&&x<40&&y>=0&&y<12;
  }
  /** A compact pixel V on the unchanged front plane, not extra geometry. */
  static boolean foreheadUv(int x,int y) {
    if(x<13||x>=25||y<13||y>=18)return false;
    int row=17-y;float distance=Math.abs(x-18.5f);
    return distance>=row-.5f&&distance<row+1.5f;
  }
  static boolean bellyUv(int x,int y){return x>=10&&x<14&&y>=36&&y<41;}
  static boolean footUv(int x,int y){return x>=44&&x<68&&y>=18&&y<30;}
  static boolean paletteProtected(String path,String layout,int x,int y,int width,int height) {
    return path.startsWith("dynamic/neutral_swim059/")&&layout.equals("baby")&&width>0&&height>0
        &&HeroRunMarkerPaint056.markerUv(x*128/width,y*64/height);
  }
  private static int corner(int[] values,float rp,float swim) {
    rp=axis(rp);swim=unit(swim);int result=0xff000000;
    for(int c=0;c<3;c++) {
      float v=0;
      for(int row=0;row<2;row++)for(int x=0;x<3;x++)
        v+=(values[row*3+x]>>>(c*8)&255)*Math.max(0,1-Math.abs(rp-(x-1)))*(row==0?swim:1-swim);
      result|=Math.clamp(Math.round(v),0,255)<<(c*8);
    }
    return result;
  }
  /** Per-channel ratios preserve the authored light/dark source pixels. */
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
  private static int blend(int a,int b,float t){return HeroRunRegionPaint055.blend(a,b,t);}
  private static int tone(int p,float f){return HeroRunRegionPaint055.tone(p,f);}
  private static int lighten(int p,float f){return HeroRunRegionPaint055.lighten(p,f);}
  private static int max(int p){return HeroRunRegionPaint055.maxChannel(p);}
  private static int distance(int a,int b){return Math.max(Math.abs((a&255)-(b&255)),Math.max(Math.abs((a>>>8&255)-(b>>>8&255)),Math.abs((a>>>16&255)-(b>>>16&255))));}
  private static float unit(float v){return Float.isFinite(v)?Math.clamp(v,0,1):0;}
  private static float axis(float v){return Float.isFinite(v)?Math.clamp(v,-1,1):0;}
  private static float swim(float v){return Float.isFinite(v)?Math.clamp(-v,0,1):0;}
}
