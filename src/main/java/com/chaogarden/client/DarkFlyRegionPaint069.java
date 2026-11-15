package com.chaogarden.client;

/** Fly-FIRST Dark adult paint on the retained canonical atlas. Actual fresh
 * and nine mature source views guide these authored approximate colors and
 * simple pixel regions. Original face pixels and source shading remain intact;
 * the floating Dark spike marker has its own pigment. Intermediate RGB blends
 * are authoring choices and do not copy the viewer's internal morph formulas. */
final class DarkFlyRegionPaint069 {
  record Input(boolean natural,boolean twoTone,float maturity,float runPower,
               float swimFly,int body,int accent,int wing,int marker) {}
  record Colors(int body,int limb,int foot,int chest,int wing,int wingTip,
                int tail,int marker,int featureBody,int featureMid,
                int featureTip,int underside) {}
  private DarkFlyRegionPaint069() {}

  // Run / balanced / Power across; Swim / balanced / Fly down. The actual
  // full-Dark first-Fly source was inspected at all nine signed endpoints.
  // These authored RGBs simplify the shaded source, not its texture data.
  private static final int[] BODY={
      rgb(42,44,70),rgb(55,66,82),rgb(65,53,63),
      rgb(39,31,76),rgb(79,28,61),rgb(79,43,42),
      rgb(48,31,66),rgb(60,35,72),rgb(77,37,55)};
  private static final int[] MID={
      rgb(115,7,194),rgb(123,7,211),rgb(158,4,126),
      rgb(102,4,205),rgb(190,4,136),rgb(184,5,113),
      rgb(135,2,193),rgb(163,5,193),rgb(185,4,126)};
  private static final int[] TIP={
      rgb(92,198,242),rgb(115,231,233),rgb(174,218,202),
      rgb(97,125,244),rgb(181,88,177),rgb(217,173,125),
      rgb(170,127,242),rgb(194,149,243),rgb(211,169,190)};
  private static final int[] CHEST={
      rgb(26,28,47),rgb(28,43,48),rgb(35,31,37),
      rgb(24,22,47),rgb(39,17,36),rgb(40,27,25),
      rgb(29,21,42),rgb(33,22,44),rgb(40,24,33)};
  private static final int[] WING={
      rgb(213,172,37),rgb(90,170,36),rgb(207,179,15),
      rgb(225,130,65),rgb(8,84,126),rgb(225,153,2),
      rgb(222,82,90),rgb(196,4,138),rgb(229,54,48)};
  private static final int[] WING_TIP={
      rgb(237,224,130),rgb(168,239,83),rgb(247,236,49),
      rgb(255,191,136),rgb(28,153,159),rgb(252,215,25),
      rgb(255,143,123),rgb(252,32,178),rgb(255,120,74)};
  private static final int[] MARKER={
      rgb(223,229,157),rgb(150,243,125),rgb(199,237,65),
      rgb(247,150,164),rgb(13,130,239),rgb(252,219,1),
      rgb(252,58,193),rgb(232,2,248),rgb(251,76,121)};
  // The actual half-age sheet retains a softer plum body and green/blue
  // wings. Pure Fly passes through lavender with olive edges before pink;
  // the mixed Fly wings similarly pass through muted olive/blue or tan.
  private static final int[] HALF_BODY={
      rgb(65,55,87),rgb(70,60,88),rgb(75,48,77),
      rgb(72,43,99),rgb(84,37,86),rgb(85,47,69),
      rgb(72,37,92),rgb(77,42,94),rgb(84,40,79)};
  private static final int[] HALF_TIP={
      rgb(157,146,225),rgb(165,171,228),rgb(183,159,204),
      rgb(172,115,232),rgb(187,101,188),rgb(196,133,158),
      rgb(181,112,230),rgb(185,120,231),rgb(193,120,194)};
  private static final int[] HALF_WING={
      rgb(64,178,139),rgb(27,187,96),rgb(72,185,115),
      rgb(147,163,86),rgb(0,153,168),rgb(153,185,46),
      rgb(131,145,98),rgb(118,93,69),rgb(147,131,69)};
  private static final int[] HALF_WING_TIP={
      rgb(90,218,140),rgb(70,236,117),rgb(90,225,126),
      rgb(127,183,151),rgb(24,230,111),rgb(123,190,139),
      rgb(149,159,193),rgb(138,152,220),rgb(156,176,158)};
  private static final int YOUNG_BODY=rgb(82,42,96),YOUNG_MID=rgb(192,3,184),
      YOUNG_TIP=rgb(190,113,211),YOUNG_CHEST=rgb(41,27,46),
      YOUNG_WING=rgb(0,157,156),YOUNG_WING_TIP=rgb(38,238,115),
      YOUNG_MARKER=rgb(4,203,239);

  static Colors colors(Input in) {
    float age=unit(in.maturity),rp=axis(in.runPower),sf=axis(in.swimFly);
    int body=in.body,mid,tip,chest,wing,wingTip;
    if(in.natural&&in.twoTone) {
      body=stages(YOUNG_BODY,corner(HALF_BODY,rp,sf),corner(BODY,rp,sf),age);
      mid=blend(YOUNG_MID,corner(MID,rp,sf),age);
      tip=stages(YOUNG_TIP,corner(HALF_TIP,rp,sf),corner(TIP,rp,sf),age);
      chest=blend(YOUNG_CHEST,corner(CHEST,rp,sf),age);
      wing=stages(YOUNG_WING,corner(HALF_WING,rp,sf),corner(WING,rp,sf),age);
      wingTip=stages(YOUNG_WING_TIP,corner(HALF_WING_TIP,rp,sf),corner(WING_TIP,rp,sf),age);
    } else {
      // Mono, named and jewel coats retain inherited body pigment. The same
      // anatomy and region boundaries use inherited hue/value contrast.
      boolean contrast=in.twoTone&&distance(body,in.accent)>=18;
      mid=contrast?in.accent:tone(body,.73f);
      tip=contrast?lighten(in.accent,.25f):lighten(body,.18f);
      chest=contrast?in.accent:tone(body,.80f);
      wing=contrast?in.accent:tone(body,.76f);
      wingTip=lighten(wing,.12f);
    }
    return new Colors(body,mid,tip,chest,wing,wingTip,tip,
        marker(age,rp,sf),body,mid,tip,chest);
  }

  /** Six new solid pixels, verified transparent in all172 canonical coats
   * and outside the UV footprint of all41 preceding gameplay models incl068.
   * They belong solely to this path; older reserved pixels are not reused. */
  static int reservedKind(int x,int y) {
    if(y!=63||x<100||x>105)return 0;
    return x==104||x==103?3:1;
  }
  static int reservedColor(Colors c,int x) {
    return switch(x){case 105->c.featureBody;case 104->c.featureMid;
      case 103->c.featureTip;case 102->c.underside;case 101->c.wing;
      case 100->c.wingTip;default->throw new IllegalArgumentException("Outside DarkFly069 swatches");};
  }
  static int pixel(Input in,Colors c,int x,int y,int source,int flags) {
    if(reservedKind(x,y)!=0)return reservedColor(c,x);
    if(source>>>24==0||(flags&HeroRunRegionPaint055.PROTECTED)!=0)return source;
    if((flags&HeroRunRegionPaint055.MARKER)!=0)return source&0xff000000|c.marker&0xffffff;
    if((flags&HeroRunRegionPaint055.WING)!=0)return tint(source,c.wing,in.wing);
    if((flags&HeroRunRegionPaint055.SKIN)==0)return source;
    boolean accent=(flags&HeroRunRegionPaint055.ACCENT)!=0;
    int target=accent?c.limb:c.body,reference=accent?in.accent:in.body;
    if((flags&HeroRunRegionPaint055.TAIL)!=0)target=c.body;
    else if(muzzleUv(x,y))target=c.chest;
    else if(torsoUv(x,y))target=chestUv(x,y)?c.chest:c.body;
    else if((flags&HeroRunRegionPaint055.LIMB)!=0)target=limbColor(c,x,y);
    return tint(source,target,reference);
  }

  /** Broad near-black front muzzle on the unchanged skull. Two rectangular
   * regions approximate the source oval; the rear and crown stay plum. The
   * original eye/expression art is protected before these paint boundaries. */
  static boolean muzzleUv(int x,int y) {
    return x>=14&&x<24&&y>=16&&y<24
        ||x>=12&&x<26&&y>=18&&y<23
        ||x>=26&&x<32&&y>=18&&y<24
        ||x>=6&&x<12&&y>=18&&y<24
        ||x>=26&&x<40&&y>=6&&y<12;
  }
  /** One compact round-edged chest patch; no painted back collar or bib. */
  static boolean torsoUv(int x,int y){return x>=0&&x<32&&y>=34&&y<42||x>=8&&x<24&&y>=26&&y<34;}
  static boolean chestUv(int x,int y) {
    return x>=10&&x<14&&y>=35&&y<41||x>=9&&x<15&&y>=36&&y<40;
  }
  static int limbColor(Colors c,int x,int y) {
    // Actual forward palm/toe faces retain a pale front inside a thin cuff.
    // Their upper local-Y edge is the highest V row on the canonical atlas;
    // the wrist and sole planes use their unchanged, separate islands below.
    if(x>=28&&x<32&&y>=30&&y<34)return y==33?c.limb:c.foot;
    if(x>=52&&x<56&&y>=26&&y<30)return y==29?c.limb:c.foot;
    if(x>=24&&x<40&&y>=30&&y<34)return y==33?c.foot:y>=31?c.limb:c.body;
    if(x>=28&&x<32&&y>=26&&y<30)return c.body;
    if(x>=32&&x<36&&y>=26&&y<30)return c.foot;
    if(x>=44&&x<68&&y>=26&&y<30)return y==29?c.foot:y==28?c.limb:c.body;
    if(x>=52&&x<60&&y>=18&&y<26)return y==25?c.foot:y==24?c.limb:c.body;
    return c.body;
  }
  static boolean paletteProtected(String path,String layout,int x,int y,int width,int height) {
    return path.startsWith("dynamic/dark_fly069/")&&layout.equals("baby")&&width>0&&height>0
        &&HeroRunMarkerPaint056.markerUv(x*128/width,y*64/height);
  }
  static boolean paletteWing(String path,String layout,int x,int y,int width,int height) {
    if(!path.startsWith("dynamic/dark_fly069/")||!layout.equals("baby")||width<=0||height<=0)return false;
    int u=x*128/width,v=y*64/height;return v==63&&(u==100||u==101);
  }
  static int marker(float maturity,float rp,float sf){return blend(YOUNG_MARKER,corner(MARKER,rp,sf),maturity);}
  private static int stages(int young,int half,int mature,float age){age=unit(age);return age<=.5f?blend(young,half,age*2):blend(half,mature,(age-.5f)*2);}
  private static int corner(int[] values,float rp,float sf) {
    rp=axis(rp);sf=axis(sf);int result=0xff000000;
    for(int c=0;c<3;c++) {
      float value=0;
      for(int y=0;y<3;y++)for(int x=0;x<3;x++)
        value+=(values[y*3+x]>>>(c*8)&255)*Math.max(0,1-Math.abs(rp-(x-1)))*Math.max(0,1-Math.abs(sf-(y-1)));
      result|=Math.clamp(Math.round(value),0,255)<<(c*8);
    }
    return result;
  }
  /** Per-channel ratios retain the original light/dark pixel artwork. */
  private static int tint(int source,int target,int reference) {
    int result=source&0xff000000;
    for(int c=0;c<3;c++) {
      int original=source>>>(c*8)&255,base=reference>>>(c*8)&255,next=target>>>(c*8)&255;
      int value=base==0?(next==0?original:Math.round(original*next/(float)Math.max(1,HeroRunRegionPaint055.maxChannel(reference)))):Math.round(original*next/(float)base);
      result|=Math.clamp(value,0,255)<<(c*8);
    }
    return result;
  }
  private static int rgb(int r,int g,int b){return HeroRunRegionPaint055.rgb(r,g,b);}
  private static int blend(int a,int b,float t){return HeroRunRegionPaint055.blend(a,b,unit(t));}
  private static int tone(int p,float f){return HeroRunRegionPaint055.tone(p,f);}
  private static int lighten(int p,float f){return HeroRunRegionPaint055.lighten(p,f);}
  private static int distance(int a,int b){return Math.max(Math.abs((a&255)-(b&255)),Math.max(Math.abs((a>>>8&255)-(b>>>8&255)),Math.abs((a>>>16&255)-(b>>>16&255))));}
  private static float unit(float v){return Float.isFinite(v)?Math.clamp(v,0,1):0;}
  private static float axis(float v){return Float.isFinite(v)?Math.clamp(v,-1,1):0;}
}
