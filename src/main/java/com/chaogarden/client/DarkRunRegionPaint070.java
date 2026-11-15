package com.chaogarden.client;

/** Run-FIRST Dark pigments on the retained canonical atlas. Fresh actual
 * young, half-developed and nine mature viewer targets guide these authored
 * RGB and pixel-region approximations. The original head box, facial art,
 * source shading and inherited coat/finish remain intact. */
final class DarkRunRegionPaint070 {
  record Input(boolean natural,boolean twoTone,float maturity,float runPower,
               float swimFly,int body,int accent,int wing,int marker) {}
  record Colors(int body,int limb,int foot,int chest,int wing,int wingTip,
                int tail,int marker,int featureBody,int featureMid,
                int featureTip,int underside) {}
  private DarkRunRegionPaint070() {}

  // Run / balanced / Power across; Swim / balanced / Fly down. All entries
  // remain Run FIRST. The two panel hues simplify the source's gradients;
  // these are eye-estimated colors, not imported texture or morph data.
  private static final int[] BODY={
      rgb(48,28,62),rgb(44,37,83),rgb(27,23,78),
      rgb(46,22,24),rgb(20,47,79),rgb(22,22,84),
      rgb(44,43,40),rgb(43,61,51),rgb(34,45,69)};
  private static final int[] MID={
      rgb(238,55,18),rgb(215,114,11),rgb(114,79,26),
      rgb(255,13,3),rgb(9,230,20),rgb(7,11,27),
      rgb(249,122,13),rgb(238,229,24),rgb(136,133,29)};
  private static final int[] TIP={
      rgb(218,54,172),rgb(199,91,237),rgb(114,53,246),
      rgb(255,26,10),rgb(0,215,242),rgb(23,20,235),
      rgb(255,150,76),rgb(255,255,127),rgb(163,167,211)};
  private static final int[] DETAIL={
      rgb(77,26,133),rgb(54,28,157),rgb(45,24,171),
      rgb(169,9,8),rgb(2,41,181),rgb(23,11,205),
      rgb(99,69,131),rgb(3,80,166),rgb(67,43,181)};
  private static final int[] WING={
      rgb(75,32,107),rgb(0,48,182),rgb(93,40,193),
      rgb(126,5,2),rgb(1,37,165),rgb(134,4,193),
      rgb(83,41,110),rgb(0,43,176),rgb(87,38,193)};
  private static final int[] WING_TIP={
      rgb(130,136,129),rgb(0,220,194),rgb(169,136,234),
      rgb(250,17,3),rgb(0,221,174),rgb(233,18,238),
      rgb(135,134,132),rgb(0,224,218),rgb(178,143,241)};
  // The observed source shares marker classes: the balanced/Fly/Swim targets
  // are cyan, both Run mixtures gray, and both Power mixtures pale blue.
  // Do not invent nine different marker colors merely to separate targets.
  private static final int[] MARKER={
      rgb(138,143,147),rgb(0,228,220),rgb(137,198,237),
      rgb(255,17,3),rgb(0,228,220),rgb(246,141,245),
      rgb(138,143,147),rgb(0,228,220),rgb(137,198,237)};

  // Actual half-age views differ from a straight young-to-mature color blend.
  // Run passes through mustard/gray panels and plum wings; Power still has
  // green/blue panels before its dark/blue mature appearance. Compatible
  // mixed panels likewise retain green/olive while their source markers blue.
  private static final int[] HALF_BODY={
      rgb(30,48,65),rgb(28,49,77),rgb(24,43,76),
      rgb(31,43,48),rgb(19,61,80),rgb(21,43,81),
      rgb(28,53,53),rgb(29,68,64),rgb(24,49,68)};
  private static final int[] HALF_MID={
      rgb(135,163,24),rgb(120,166,33),rgb(48,136,24),
      rgb(154,140,12),rgb(8,232,22),rgb(29,129,17),
      rgb(142,196,30),rgb(165,252,18),rgb(71,182,20)};
  private static final int[] HALF_TIP={
      rgb(158,195,191),rgb(149,211,243),rgb(72,162,219),
      rgb(157,158,132),rgb(0,224,236),rgb(17,151,224),
      rgb(167,224,165),rgb(153,255,181),rgb(90,213,210)};
  private static final int[] HALF_DETAIL={
      rgb(21,47,139),rgb(27,44,163),rgb(20,33,170),
      rgb(21,58,124),rgb(3,40,172),rgb(5,30,178),
      rgb(19,52,135),rgb(4,63,158),rgb(16,43,163)};
  private static final int[] HALF_WING={
      rgb(37,35,143),rgb(13,50,176),rgb(57,43,169),
      rgb(104,20,128),rgb(3,38,168),rgb(67,15,176),
      rgb(46,36,128),rgb(2,44,169),rgb(39,40,166)};
  private static final int[] HALF_WING_TIP={
      rgb(69,123,206),rgb(1,193,215),rgb(83,118,219),
      rgb(139,27,128),rgb(4,177,222),rgb(116,9,225),
      rgb(79,113,211),rgb(0,190,212),rgb(67,125,220)};
  private static final int[] HALF_MARKER={
      rgb(100,149,177),rgb(0,188,228),rgb(70,162,218),
      rgb(147,94,151),rgb(0,188,228),rgb(133,143,239),
      rgb(100,149,177),rgb(0,188,228),rgb(70,162,218)};
  private static final int YOUNG_BODY=rgb(20,71,79),
      YOUNG_MID=rgb(4,239,26),YOUNG_TIP=rgb(0,249,184),
      YOUNG_DETAIL=rgb(2,44,177),YOUNG_WING=rgb(1,31,169),
      YOUNG_WING_TIP=rgb(6,88,221),YOUNG_MARKER=rgb(0,142,247);

  static Colors colors(Input in) {
    float age=unit(in.maturity),rp=axis(in.runPower),sf=axis(in.swimFly);
    int body=in.body,mid,tip,detail,wing,wingTip;
    if(in.natural&&in.twoTone) {
      body=stages(YOUNG_BODY,corner(HALF_BODY,rp,sf),corner(BODY,rp,sf),age);
      mid=stages(YOUNG_MID,corner(HALF_MID,rp,sf),corner(MID,rp,sf),age);
      tip=stages(YOUNG_TIP,corner(HALF_TIP,rp,sf),corner(TIP,rp,sf),age);
      detail=stages(YOUNG_DETAIL,corner(HALF_DETAIL,rp,sf),corner(DETAIL,rp,sf),age);
      wing=stages(YOUNG_WING,corner(HALF_WING,rp,sf),corner(WING,rp,sf),age);
      wingTip=stages(YOUNG_WING_TIP,corner(HALF_WING_TIP,rp,sf),corner(WING_TIP,rp,sf),age);
    } else {
      // Named, mono and jewel coats keep their inherited body/finish. The
      // same small panel boundaries use inherited hue or value contrast.
      boolean contrast=in.twoTone&&distance(body,in.accent)>=18;
      mid=contrast?in.accent:tone(body,.65f);
      tip=contrast?lighten(in.accent,.25f):lighten(body,.18f);
      detail=contrast?blend(body,in.accent,.72f):tone(body,.75f);
      wing=contrast?in.accent:tone(body,.76f);
      wingTip=lighten(wing,.15f);
    }
    return new Colors(body,mid,tip,detail,wing,wingTip,tip,
        marker(age,rp,sf),body,mid,tip,detail);
  }

  /** Six route-specific point swatches, source-transparent in all172 coats
   * and clear of all42 preceding gameplay UV footprints. No old reservation
   * is reused; the canonical source atlases themselves are never edited. */
  static int reservedKind(int x,int y) {
    if(y!=63||x<74||x>79)return 0;
    return x==77||x==78?3:1;
  }
  static int reservedColor(Colors c,int x) {
    return switch(x){case 79->c.featureBody;case 78->c.featureMid;
      case 77->c.featureTip;case 76->c.underside;case 75->c.wing;
      case 74->c.wingTip;default->throw new IllegalArgumentException("Outside DarkRun070 swatches");};
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
    else if(crownUv(x,y)||cheekUv(x,y))target=headColor(c,x,y);
    else if(torsoUv(x,y))target=collarUv(x,y)?collarColor(c,x,y):c.body;
    else if((flags&HeroRunRegionPaint055.LIMB)!=0)target=limbColor(c,x,y);
    return tint(source,target,reference);
  }

  /** The approved patch lies on the original skull TOP only. It stops before
   * the front edge, so no stripe or source teardrop is painted on the forehead.
   * A stepped outline and two pigments replace the source's smooth gradient. */
  static boolean crownUv(int x,int y) {
    if(y<1||y>10)return false;
    int half=y==1||y==10?0:y<=3||y>=8?1:2;
    return x>=19-half&&x<=19+half;
  }
  /** Two small cheek panels use only the original lateral skull islands.
   * The original front face and protected eye/expression art stay intact. */
  static boolean cheekUv(int x,int y) {
    int depth=x>=26&&x<38?x-26:x>=0&&x<12?11-x:-1;
    if(depth<0)return false;
    return y==17&&depth>=7&&depth<=9
        ||y==18&&depth>=5&&depth<=9
        ||y==19&&depth>=3&&depth<=9
        ||y==20&&depth>=2&&depth<=8
        ||y==21&&depth>=2&&depth<=5;
  }
  static int headColor(Colors c,int x,int y) {
    if(crownUv(x,y))return blend(c.featureMid,c.featureTip,(y-1)/9f);
    if(cheekUv(x,y)) {
      int depth=x>=26?x-26:11-x;
      return blend(c.featureMid,c.featureTip,(9-depth)/7f);
    }
    return c.body;
  }
  static boolean torsoUv(int x,int y){return x>=0&&x<32&&y>=34&&y<42||x>=8&&x<24&&y>=26&&y<34;}
  /** Slim stepped front neck trim; the chest/back remain base colored. */
  static boolean collarUv(int x,int y) {
    return y==34&&x>=9&&x<15||y==35&&x>=9&&x<15
        ||y==36&&x>=10&&x<14;
  }
  static int collarColor(Colors c,int x,int y){return y==34?c.chest:y==35?c.limb:c.foot;}
  /** The small outer-palm drop and two narrow toe lines are paint, never
   * extra rings or stretched geometry. Other palm/toe surfaces stay dark. */
  static int limbColor(Colors c,int x,int y) {
    if(handDropUv(x,y))return y==30?c.foot:y==33?c.chest:c.limb;
    if(footStripeUv(x,y))return y==28?c.limb:c.chest;
    return c.body;
  }
  static boolean handDropUv(int x,int y) {
    return x==25&&y==30||x>=25&&x<27&&y>=31&&y<34;
  }
  static boolean footStripeUv(int x,int y) {
    return (y==28||y==29)&&(x>=48&&x<60);
  }
  static boolean paletteProtected(String path,String layout,int x,int y,int width,int height) {
    return path.startsWith("dynamic/dark_run070/")&&layout.equals("baby")&&width>0&&height>0
        &&HeroRunMarkerPaint056.markerUv(x*128/width,y*64/height);
  }
  static boolean paletteWing(String path,String layout,int x,int y,int width,int height) {
    if(!path.startsWith("dynamic/dark_run070/")||!layout.equals("baby")||width<=0||height<=0)return false;
    int u=x*128/width,v=y*64/height;return v==63&&(u==74||u==75);
  }
  static int marker(float maturity,float rp,float sf) {
    return stages(YOUNG_MARKER,corner(HALF_MARKER,rp,sf),corner(MARKER,rp,sf),maturity);
  }
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
  /** Per-channel ratios retain the canonical light/dark pixel artwork. */
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
