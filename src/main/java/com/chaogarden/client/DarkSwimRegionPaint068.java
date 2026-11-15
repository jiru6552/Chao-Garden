package com.chaogarden.client;

/** Swim-FIRST Dark adult paint on the retained canonical atlas. Fresh live
 * viewer captures of all nine signed corners guide these authored approximate
 * colors and simple pixel regions. Original face pixels and source shading
 * remain intact; the floating Dark spike marker has its own pigment. */
final class DarkSwimRegionPaint068 {
  record Input(boolean natural,boolean twoTone,float maturity,float runPower,
               float swimFly,int body,int accent,int wing,int marker) {}
  record Colors(int body,int limb,int foot,int chest,int wing,int wingTip,
                int tail,int marker,int featureBody,int featureMid,
                int featureTip,int underside) {}
  private DarkSwimRegionPaint068() {}

  // Run / balanced / Power across; Swim / balanced / Fly down. Every corner
  // was independently viewed with first-Swim selected and full Dark alignment.
  private static final int[] BODY={
      rgb(41,62,56),rgb(62,67,47),rgb(39,67,36),
      rgb(22,59,64),rgb(21,64,60),rgb(17,64,24),
      rgb(35,54,69),rgb(48,43,68),rgb(29,57,46)};
  private static final int[] MID={
      rgb(31,146,193),rgb(41,149,168),rgb(18,151,100),
      rgb(17,102,231),rgb(18,134,190),rgb(5,171,9),
      rgb(30,95,245),rgb(37,66,244),rgb(21,112,129)};
  private static final int[] TIP={
      rgb(91,228,217),rgb(243,255,175),rgb(98,238,84),
      rgb(38,214,242),rgb(26,218,190),rgb(64,242,4),
      rgb(93,179,241),rgb(156,115,242),rgb(99,210,161)};
  private static final int[] CHEST={
      rgb(22,171,163),rgb(29,176,130),rgb(18,184,62),
      rgb(12,145,185),rgb(17,186,178),rgb(4,192,3),
      rgb(25,117,175),rgb(28,93,188),rgb(20,147,100)};
  private static final int[] WING={
      rgb(132,153,144),rgb(220,212,18),rgb(129,218,158),
      rgb(10,48,213),rgb(202,229,235),rgb(13,209,220),
      rgb(128,112,232),rgb(196,147,238),rgb(129,207,223)};
  private static final int[] MARKER={
      rgb(143,162,147),rgb(239,225,7),rgb(155,231,85),
      rgb(5,65,244),rgb(208,240,13),rgb(8,202,196),
      rgb(137,117,232),rgb(241,141,227),rgb(138,212,220)};
  // The separately observed half-age colors matter: Fly still has cream
  // points and a pale cyan marker before its final lavender/pink colors.
  private static final int[] HALF_TIP={
      rgb(177,250,153),rgb(239,254,155),rgb(153,248,93),
      rgb(109,244,175),rgb(141,249,158),rgb(111,244,38),
      rgb(169,247,178),rgb(223,244,178),rgb(171,249,128)};
  private static final int[] HALF_WING={
      rgb(55,190,205),rgb(126,205,75),rgb(41,204,176),
      rgb(12,142,222),rgb(54,201,211),rgb(15,210,219),
      rgb(71,175,222),rgb(134,199,217),rgb(69,209,215)};
  private static final int[] HALF_MARKER={
      rgb(72,201,191),rgb(136,236,142),rgb(71,220,155),
      rgb(5,158,238),rgb(112,237,143),rgb(10,213,198),
      rgb(72,183,228),rgb(139,212,222),rgb(76,210,201)};
  private static final int YOUNG_BODY=rgb(61,75,42),YOUNG_MID=rgb(28,158,130),
      YOUNG_TIP=rgb(225,254,124),YOUNG_CHEST=rgb(12,210,91),
      YOUNG_WING=rgb(36,207,218),YOUNG_MARKER=rgb(12,215,211);

  static Colors colors(Input in) {
    float age=unit(in.maturity),rp=axis(in.runPower),sf=axis(in.swimFly);
    int body=in.body,mid,tip,chest,wing,wingTip;
    if(in.natural&&in.twoTone) {
      body=blend(YOUNG_BODY,corner(BODY,rp,sf),age);
      mid=blend(YOUNG_MID,corner(MID,rp,sf),age);
      tip=stages(YOUNG_TIP,corner(HALF_TIP,rp,sf),corner(TIP,rp,sf),age);
      chest=blend(YOUNG_CHEST,corner(CHEST,rp,sf),age);
      wing=stages(YOUNG_WING,corner(HALF_WING,rp,sf),corner(WING,rp,sf),age);
      wingTip=lighten(wing,.12f);
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
   * and outside the UV footprint of all40 preceding gameplay models incl067.
   * They belong solely to this path; older reserved pixels are not reused. */
  static int reservedKind(int x,int y) {
    if(y!=63||x<106||x>111)return 0;
    return x==110||x==109?3:1;
  }
  static int reservedColor(Colors c,int x) {
    return switch(x){case 111->c.featureBody;case 110->c.featureMid;
      case 109->c.featureTip;case 108->c.underside;case 107->c.wing;
      case 106->c.wingTip;default->throw new IllegalArgumentException("Outside DarkSwim068 swatches");};
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

  /** Lower front muzzle and short adjacent cheeks; the rear cap stays dark.
   * All eye/expression islands are separately protected before this test. */
  static boolean muzzleUv(int x,int y) {
    return x>=12&&x<26&&y>=20&&y<24
        ||x>=26&&x<32&&y>=21&&y<24
        ||x>=6&&x<12&&y>=21&&y<24
        ||x>=26&&x<40&&y>=6&&y<12;
  }
  /** One compact round-edged chest patch; no painted back collar or bib. */
  static boolean torsoUv(int x,int y){return x>=0&&x<32&&y>=34&&y<42||x>=8&&x<24&&y>=26&&y<34;}
  static boolean chestUv(int x,int y) {
    return x>=10&&x<14&&y>=35&&y<41||x>=9&&x<15&&y>=36&&y<40;
  }
  static int limbColor(Colors c,int x,int y) {
    if(x>=24&&x<40&&y>=30&&y<34)return y==33?c.foot:y>=31?c.limb:c.body;
    if(x>=28&&x<32&&y>=26&&y<30)return c.body;
    if(x>=32&&x<36&&y>=26&&y<30)return c.foot;
    if(x>=44&&x<68&&y>=26&&y<30)return y==29?c.foot:y==28?c.limb:c.body;
    if(x>=52&&x<60&&y>=18&&y<26)return y==25?c.foot:y==24?c.limb:c.body;
    return c.body;
  }
  static boolean paletteProtected(String path,String layout,int x,int y,int width,int height) {
    return path.startsWith("dynamic/dark_swim068/")&&layout.equals("baby")&&width>0&&height>0
        &&HeroRunMarkerPaint056.markerUv(x*128/width,y*64/height);
  }
  static boolean paletteWing(String path,String layout,int x,int y,int width,int height) {
    if(!path.startsWith("dynamic/dark_swim068/")||!layout.equals("baby")||width<=0||height<=0)return false;
    int u=x*128/width,v=y*64/height;return v==63&&(u==106||u==107);
  }
  static int marker(float maturity,float rp,float sf){return stages(YOUNG_MARKER,corner(HALF_MARKER,rp,sf),corner(MARKER,rp,sf),maturity);}
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
