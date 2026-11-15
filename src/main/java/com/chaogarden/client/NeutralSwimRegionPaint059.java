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

  // Full signed grid: Run/balanced/Power across; Swim/balanced/Fly down.
  // First identity stays Swim when later training passes through balance.
  // The original six palettes and their old-range arithmetic remain exact.
  private static final int[] BODY={
      rgb(64,239,35),rgb(118,244,24),rgb(153,239,28),
      rgb(18,238,39),rgb(157,247,28),rgb(167,239,20),
      rgb(50,236,32),rgb(160,195,30),rgb(177,220,28)};
  private static final int[] FACE={
      rgb(87,227,222),rgb(189,246,233),rgb(219,235,166),
      rgb(26,218,221),rgb(239,233,161),rgb(249,225,34),
      rgb(162,231,222),rgb(232,172,193),rgb(241,213,131)};
  private static final int[] LIMB={
      rgb(222,231,59),rgb(223,235,61),rgb(242,226,51),
      rgb(29,222,211),rgb(252,239,44),rgb(251,224,26),
      rgb(222,234,89),rgb(246,196,162),rgb(245,214,61)};
  private static final int[] FOOT={
      rgb(234,226,66),rgb(249,236,42),rgb(255,193,57),
      rgb(151,156,221),rgb(250,235,43),rgb(255,133,24),
      rgb(250,174,78),rgb(255,135,60),rgb(255,121,34)};
  private static final int[] FOREHEAD={
      rgb(32,236,208),rgb(31,234,179),rgb(48,229,30),
      rgb(28,241,219),rgb(36,235,175),rgb(40,225,24),
      rgb(25,240,214),rgb(55,226,193),rgb(35,225,128)};
  private static final int[] FIN={
      rgb(193,237,46),rgb(204,240,41),rgb(228,235,58),
      rgb(28,232,56),rgb(250,237,75),rgb(250,228,36),
      rgb(173,237,190),rgb(250,186,185),rgb(247,218,97)};
  private static final int[] WING={
      rgb(85,148,194),rgb(42,169,214),rgb(112,151,177),
      rgb(154,157,214),rgb(49,169,214),rgb(224,140,154),
      rgb(143,151,214),rgb(150,145,211),rgb(190,132,187)};
  private static final int[] TAIL={
      rgb(63,152,201),rgb(31,172,218),rgb(172,204,129),
      rgb(150,160,222),rgb(42,175,216),rgb(252,221,36),
      rgb(88,159,218),rgb(122,154,218),rgb(212,220,115)};
  private static final int[] ORB={
      rgb(131,241,233),rgb(195,253,244),rgb(221,248,167),
      rgb(33,216,228),rgb(190,249,163),rgb(220,250,39),
      rgb(58,197,173),rgb(178,212,43),rgb(191,232,31)};
  // Independently observed half-age opposite-Fly row: still lime/cream with
  // blue wings before its mature pink/lavender accents. These keys apply only
  // above balance; all six earlier target paths retain their exact old paint.
  private static final int[] MID_BODY={rgb(82,244,31),rgb(153,242,27),rgb(165,242,26)};
  private static final int[] MID_FACE={rgb(197,226,198),rgb(238,224,175),rgb(242,225,128)};
  private static final int[] MID_LIMB={rgb(233,242,107),rgb(244,236,91),rgb(246,231,75)};
  private static final int[] MID_FOOT={rgb(248,199,81),rgb(250,197,74),rgb(252,191,60)};
  private static final int[] MID_FOREHEAD={rgb(25,240,190),rgb(34,238,166),rgb(33,235,102)};
  private static final int[] MID_FIN={rgb(230,246,145),rgb(252,239,124),rgb(251,235,90)};
  private static final int[] MID_WING={rgb(43,143,197),rgb(55,145,200),rgb(75,145,190)};
  private static final int[] MID_TAIL={rgb(39,148,204),rgb(34,147,204),rgb(122,188,168)};
  private static final int[] MID_ORB={rgb(36,224,153),rgb(68,239,94),rgb(85,241,67)};
  private static final int YOUNG_BODY=rgb(159,246,28),YOUNG_FACE=rgb(239,234,158),
      YOUNG_LIMB=rgb(249,237,49),YOUNG_FOREHEAD=rgb(37,237,174),
      YOUNG_WING=rgb(48,169,213),YOUNG_ORB=rgb(29,229,135);

  static Colors colors(Input in) {
    float age=unit(in.maturity),rp=axis(in.runPower),sf=axis(in.swimFly);
    boolean natural=in.natural&&in.twoTone;
    int body=in.body,limb,foot,face,belly,forehead,wing,tail,fin;
    if(natural) {
      body=develop(YOUNG_BODY,BODY,MID_BODY,age,rp,sf);
      face=develop(YOUNG_FACE,FACE,MID_FACE,age,rp,sf);
      belly=face;
      limb=develop(YOUNG_LIMB,LIMB,MID_LIMB,age,rp,sf);
      foot=develop(YOUNG_LIMB,FOOT,MID_FOOT,age,rp,sf);
      forehead=develop(YOUNG_FOREHEAD,FOREHEAD,MID_FOREHEAD,age,rp,sf);
      wing=develop(YOUNG_WING,WING,MID_WING,age,rp,sf);
      tail=develop(YOUNG_WING,TAIL,MID_TAIL,age,rp,sf);
      fin=develop(YOUNG_LIMB,FIN,MID_FIN,age,rp,sf);
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
    int marker=develop(YOUNG_ORB,ORB,MID_ORB,age,rp,sf);
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
  /** Piecewise two-row interpolation keeps the original six-target float
   * operation order exact while allowing the previously absent positive row. */
  private static int corner(int[] values,float rp,float sf) {
    rp=axis(rp);sf=axis(sf);float amount=sf<=0?-sf:sf;
    int offset=sf<=0?0:3,result=0xff000000;
    for(int c=0;c<3;c++) {
      float v=0;
      for(int row=0;row<2;row++)for(int x=0;x<3;x++) {
        float weight=sf<=0?(row==0?amount:1-amount):(row==0?1-amount:amount);
        v+=(values[offset+row*3+x]>>>(c*8)&255)*Math.max(0,1-Math.abs(rp-(x-1)))*weight;
      }
      result|=Math.clamp(Math.round(v),0,255)<<(c*8);
    }
    return result;
  }
  private static int middle(int[] values,float rp) {
    rp=axis(rp);int result=0xff000000;
    for(int c=0;c<3;c++) {
      float v=0;
      for(int x=0;x<3;x++)v+=(values[x]>>>(c*8)&255)*Math.max(0,1-Math.abs(rp-(x-1)));
      result|=Math.clamp(Math.round(v),0,255)<<(c*8);
    }
    return result;
  }
  private static int develop(int young,int[] values,int[] mid,float age,float rp,float sf) {
    // This branch is the old expression, with identical inputs and operation
    // order, throughout the previously implemented negative half-grid.
    if(sf<=0)return blend(young,corner(values,rp,sf),age);
    int center=blend(young,corner(values,rp,0),age),half=middle(mid,rp);
    int opposite=age<=.5f?blend(young,half,age*2):blend(half,corner(values,rp,1),(age-.5f)*2);
    return blend(center,opposite,sf);
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
}
