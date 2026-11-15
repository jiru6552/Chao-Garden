package com.chaogarden.client;

/** Power-FIRST Dark pigments on the retained canonical atlas. Fresh actual
 * young, half-age and nine mature viewer targets guide these eye-estimated RGBs and
 * simple pixel regions. Source face art, skull geometry and inherited coat
 * finish remain intact. Blend timing is authored, not a viewer formula. */
final class DarkPowerRegionPaint071 {
  record Input(boolean natural,boolean twoTone,float maturity,float runPower,
               float swimFly,int body,int accent,int wing,int marker) {}
  record Colors(int body,int limb,int foot,int chest,int wing,int wingTip,
                int tail,int marker,int featureBody,int featureMid,
                int featureTip,int underside) {}
  private DarkPowerRegionPaint071() {}

  // Run / balanced / Power across; Swim / balanced / Fly down. Every entry
  // retains first Power, including the opposite Run specialization. The
  // pointed/forked tips and compact palms share the observed stripe pigments.
  private static final int[] BODY={
      rgb(44,64,33),rgb(9,71,51),rgb(44,61,33),
      rgb(76,77,22),rgb(72,52,80),rgb(75,55,18),
      rgb(54,62,47),rgb(51,53,69),rgb(59,52,47)};
  private static final int[] MID={
      rgb(131,117,34),rgb(4,105,45),rgb(136,101,24),
      rgb(210,103,15),rgb(229,65,54),rgb(229,79,14),
      rgb(190,91,32),rgb(148,78,57),rgb(198,77,33)};
  private static final int[] TIP={
      rgb(115,171,25),rgb(8,134,48),rgb(142,166,25),
      rgb(190,232,19),rgb(247,132,142),rgb(246,130,32),
      rgb(160,198,117),rgb(103,166,239),rgb(148,188,176)};
  private static final int[] BELLY={
      rgb(73,207,96),rgb(4,217,169),rgb(79,197,113),
      rgb(165,220,15),rgb(150,119,249),rgb(175,168,27),
      rgb(148,195,178),rgb(102,154,243),rgb(153,188,180)};
  private static final int[] FOOT={
      rgb(104,156,31),rgb(17,187,134),rgb(111,158,39),
      rgb(173,181,24),rgb(136,116,217),rgb(150,149,25),
      rgb(134,168,137),rgb(84,134,203),rgb(142,168,147)};
  private static final int[] WING={
      rgb(42,29,58),rgb(35,27,39),rgb(73,107,31),
      rgb(5,28,70),rgb(34,28,41),rgb(96,145,23),
      rgb(18,51,98),rgb(32,34,41),rgb(113,172,25)};
  private static final int[] WING_TIP={
      rgb(133,54,180),rgb(237,40,116),rgb(251,58,58),
      rgb(8,103,242),rgb(180,62,208),rgb(241,64,30),
      rgb(8,175,179),rgb(117,238,22),rgb(243,78,30)};
  // Some source marker hues are close (yellow Power versus lemon Power/Fly),
  // so their identity does not depend on inventing a large hue difference.
  private static final int[] MARKER={
      rgb(104,134,186),rgb(250,137,157),rgb(255,180,60),
      rgb(0,136,242),rgb(240,172,255),rgb(255,225,12),
      rgb(93,192,204),rgb(168,254,141),rgb(225,255,42)};
  // The actual half-age source retains orange/russet bands on most forms.
  // Gray/mint bellies and peach/ochre/chartreuse markers precede the mature
  // lavender/blue/teal colors; these middle keys are not endpoint averages.
  private static final int[] HALF_BODY={
      rgb(59,69,31),rgb(39,64,39),rgb(60,64,30),
      rgb(75,68,23),rgb(69,54,50),rgb(75,59,21),
      rgb(64,62,41),rgb(60,53,46),rgb(68,57,35)};
  private static final int[] HALF_MID={
      rgb(178,113,20),rgb(115,102,32),rgb(185,108,20),
      rgb(222,88,10),rgb(231,75,26),rgb(236,83,13),
      rgb(223,89,15),rgb(207,82,34),rgb(229,82,23)};
  private static final int[] HALF_TIP={
      rgb(173,168,22),rgb(115,109,26),rgb(208,172,19),
      rgb(222,190,21),rgb(240,106,42),rgb(246,127,22),
      rgb(202,178,64),rgb(139,169,175),rgb(183,179,114)};
  private static final int[] HALF_BELLY={
      rgb(133,207,47),rgb(77,203,128),rgb(130,196,81),
      rgb(174,195,19),rgb(152,148,149),rgb(177,169,24),
      rgb(164,185,108),rgb(138,172,170),rgb(156,179,114)};
  private static final int[] HALF_FOOT={
      rgb(148,171,25),rgb(100,178,68),rgb(150,170,25),
      rgb(169,178,23),rgb(169,128,94),rgb(156,155,24),
      rgb(159,166,82),rgb(144,161,136),rgb(155,166,89)};
  private static final int[] HALF_WING={
      rgb(38,29,43),rgb(40,26,39),rgb(54,48,32),
      rgb(33,25,47),rgb(37,27,42),rgb(57,63,31),
      rgb(37,28,45),rgb(36,28,38),rgb(60,62,31)};
  private static final int[] HALF_WING_TIP={
      rgb(159,47,172),rgb(221,39,133),rgb(236,60,97),
      rgb(135,58,181),rgb(201,49,174),rgb(237,65,83),
      rgb(131,69,174),rgb(202,47,160),rgb(229,61,103)};
  private static final int[] HALF_MARKER={
      rgb(181,176,69),rgb(255,178,64),rgb(255,221,17),
      rgb(133,183,154),rgb(255,185,111),rgb(255,224,12),
      rgb(181,221,112),rgb(224,250,68),rgb(255,241,29)};
  private static final int YOUNG_BODY=rgb(75,61,24),
      YOUNG_MID=rgb(226,82,8),YOUNG_TIP=rgb(235,147,15),
      YOUNG_BELLY=rgb(177,169,22),YOUNG_FOOT=rgb(161,166,22),
      YOUNG_WING=rgb(40,27,42),YOUNG_WING_TIP=rgb(205,37,138),
      YOUNG_MARKER=rgb(255,221,12);

  static Colors colors(Input in) {
    float age=unit(in.maturity),rp=axis(in.runPower),sf=axis(in.swimFly);
    int body=in.body,mid,tip,belly,foot,wing,wingTip;
    if(in.natural&&in.twoTone) {
      body=stages(YOUNG_BODY,corner(HALF_BODY,rp,sf),corner(BODY,rp,sf),age);
      mid=stages(YOUNG_MID,corner(HALF_MID,rp,sf),corner(MID,rp,sf),age);
      tip=stages(YOUNG_TIP,corner(HALF_TIP,rp,sf),corner(TIP,rp,sf),age);
      belly=stages(YOUNG_BELLY,corner(HALF_BELLY,rp,sf),corner(BELLY,rp,sf),age);
      foot=stages(YOUNG_FOOT,corner(HALF_FOOT,rp,sf),corner(FOOT,rp,sf),age);
      wing=stages(YOUNG_WING,corner(HALF_WING,rp,sf),corner(WING,rp,sf),age);
      wingTip=stages(YOUNG_WING_TIP,corner(HALF_WING_TIP,rp,sf),corner(WING_TIP,rp,sf),age);
    } else {
      // Named, mono and jewel coats retain their inherited body/finish.
      // Bands use inherited accent or bounded value contrast on the same UVs.
      boolean contrast=in.twoTone&&distance(body,in.accent)>=18;
      mid=contrast?in.accent:tone(body,.65f);
      tip=contrast?lighten(in.accent,.25f):lighten(body,.18f);
      belly=contrast?in.accent:lighten(body,.10f);foot=tip;
      wing=contrast?in.accent:tone(body,.76f);
      wingTip=lighten(wing,.15f);
    }
    return new Colors(body,mid,foot,belly,wing,wingTip,tip,
        marker(age,rp,sf),body,mid,tip,tone(body,.68f));
  }

  /** Fourteen path-specific pixels are transparent in all172 canonical coats
   * and outside all43 prior model UV footprints. Eight make three broad
   * stripe bands; six are separate cap/root/wing point swatches. */
  static int reservedKind(int x,int y) {
    if(y!=63||x<60||x>73)return 0;
    return x==61||x==63||x==65||x==67||x==71||x==72?3:1;
  }
  static int reservedColor(Colors c,int x) {
    return switch(x){case 60,62,64,66,73->c.featureBody;
      case 61,63,65,72->c.featureMid;case 67,71->c.featureTip;
      case 70->c.underside;case 69->c.wing;case 68->c.wingTip;
      default->throw new IllegalArgumentException("Outside DarkPower071 swatches");};
  }
  static int pixel(Input in,Colors c,int x,int y,int source,int flags) {
    if(reservedKind(x,y)!=0)return reservedColor(c,x);
    if(source>>>24==0||(flags&HeroRunRegionPaint055.PROTECTED)!=0)return source;
    if((flags&HeroRunRegionPaint055.MARKER)!=0)return source&0xff000000|c.marker&0xffffff;
    if((flags&HeroRunRegionPaint055.WING)!=0)return tint(source,c.wing,in.wing);
    if((flags&HeroRunRegionPaint055.SKIN)==0)return source;
    boolean accent=(flags&HeroRunRegionPaint055.ACCENT)!=0;
    int target=c.body,reference=accent?in.accent:in.body;
    if((flags&HeroRunRegionPaint055.TAIL)!=0)target=c.body;
    else if(torsoUv(x,y))target=bellyUv(x,y)?c.chest:c.body;
    else if((flags&HeroRunRegionPaint055.LIMB)!=0)target=limbColor(c,x,y);
    return tint(source,target,reference);
  }
  /** The skull is one base pigment. Bands belong only to the separate horn,
   * never the original forehead, eyes or head box. */
  static int headColor(Colors c,int x,int y){return c.body;}
  static boolean torsoUv(int x,int y){return x>=0&&x<32&&y>=34&&y<42||x>=8&&x<24&&y>=26&&y<34;}
  /** A compact front-only belly patch; the back and side planes stay base. */
  static boolean bellyUv(int x,int y) {
    return x>=9&&x<15&&y>=35&&y<41&&(y!=35&&y!=40||x>=10&&x<14);
  }
  /** Horizontal stripes wrap the original compact palm sides. No wrist or
   * limb is stretched and no extra ring geometry is introduced. */
  static int limbColor(Colors c,int x,int y) {
    if(handBandUv(x,y))return (y&1)==0?c.limb:c.body;
    if(footSideUv(x,y))return y>=28?c.foot:c.limb;
    if(footTopUv(x,y))return y>=24?c.limb:c.body;
    return c.body;
  }
  static boolean handBandUv(int x,int y){return x>=24&&x<40&&y>=30&&y<34;}
  static boolean footSideUv(int x,int y){return x>=44&&x<68&&y>=26&&y<30;}
  static boolean footTopUv(int x,int y){return x>=52&&x<56&&y>=18&&y<26;}
  static boolean paletteProtected(String path,String layout,int x,int y,int width,int height) {
    return path.startsWith("dynamic/dark_power071/")&&layout.equals("baby")&&width>0&&height>0
        &&HeroRunMarkerPaint056.markerUv(x*128/width,y*64/height);
  }
  static boolean paletteWing(String path,String layout,int x,int y,int width,int height) {
    if(!path.startsWith("dynamic/dark_power071/")||!layout.equals("baby")||width<=0||height<=0)return false;
    int u=x*128/width,v=y*64/height;return v==63&&(u==68||u==69);
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
  /** Per-channel ratios retain canonical light/dark pixel artwork and alpha. */
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
