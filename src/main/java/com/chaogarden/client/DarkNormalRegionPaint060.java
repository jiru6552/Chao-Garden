package com.chaogarden.client;

/** Small semantic pixel regions on the original baby atlas for Dark/Normal
 * FIRST-type adults. Independently operated viewer references supply regional
 * hue and placement; these RGBs and blocky paint boundaries are approximations,
 * not extracted game materials. Original face pixels and alpha remain exact. */
final class DarkNormalRegionPaint060 {
  record Input(boolean natural,boolean twoTone,float maturity,float runPower,
               float swimFly,int body,int accent,int wing,int marker) {}
  record Colors(int body,int limb,int foot,int belly,int wing,int tail,int marker,
                int featureBody,int featureTip,int fin,int underside) {}
  private DarkNormalRegionPaint060() {}

  // Run/center/Power across; Swim/center/Fly down. The nine corners were
  // individually viewed. In particular pure Swim is brown/peach, Run+Swim
  // stays plum/pink, and Fly is slate/lavender rather than another plum coat.
  private static final int[] BODY={
      rgb(58,31,53),rgb(67,50,40),rgb(69,48,28),
      rgb(58,24,62),rgb(54,28,46),rgb(70,41,24),
      rgb(61,35,69),rgb(61,57,72),rgb(67,50,51)};
  private static final int[] CUFF={
      rgb(102,57,160),rgb(92,98,105),rgb(77,92,55),
      rgb(95,16,211),rgb(87,33,141),rgb(95,89,31),
      rgb(109,38,225),rgb(104,132,218),rgb(92,115,175)};
  private static final int[] TIP={
      rgb(239,89,204),rgb(224,145,88),rgb(235,147,72),
      rgb(249,10,225),rgb(237,20,156),rgb(246,112,26),
      rgb(232,103,251),rgb(246,149,247),rgb(237,150,156)};
  private static final int[] CHEST={
      rgb(220,64,170),rgb(198,129,87),rgb(211,132,69),
      rgb(224,9,235),rgb(216,38,155),rgb(233,114,26),
      rgb(191,68,237),rgb(202,142,237),rgb(193,132,155)};
  private static final int[] ROOT={
      rgb(108,49,186),rgb(99,99,108),rgb(89,99,56),
      rgb(107,26,215),rgb(104,35,189),rgb(131,98,27),
      rgb(118,58,208),rgb(130,154,223),rgb(110,125,180)};
  private static final int[] WING={
      rgb(91,75,212),rgb(176,14,225),rgb(229,66,178),
      rgb(23,174,214),rgb(111,27,207),rgb(242,183,30),
      rgb(137,167,93),rgb(239,181,28),rgb(245,188,37)};
  private static final int[] MARKER={
      rgb(198,123,142),rgb(137,7,237),rgb(189,88,151),
      rgb(246,231,37),rgb(96,19,219),rgb(255,191,26),
      rgb(250,190,124),rgb(246,129,244),rgb(255,173,151)};
  private static final int YOUNG_BODY=rgb(66,22,25),YOUNG_CUFF=rgb(125,19,32),
      YOUNG_TIP=rgb(245,16,24),YOUNG_CHEST=rgb(215,29,42),
      YOUNG_WING=rgb(123,39,206),YOUNG_MARKER=rgb(137,139,234);

  static Colors colors(Input in) {
    float age=unit(in.maturity),rp=axis(in.runPower),sf=axis(in.swimFly);
    int body=in.body,limb,foot,belly,wing,tip,root;
    if(in.natural&&in.twoTone) {
      body=blend(YOUNG_BODY,corner(BODY,rp,sf),age);
      limb=blend(YOUNG_CUFF,corner(CUFF,rp,sf),age);
      foot=blend(YOUNG_TIP,corner(TIP,rp,sf),age);
      belly=blend(YOUNG_CHEST,corner(CHEST,rp,sf),age);
      wing=blend(YOUNG_WING,corner(WING,rp,sf),age);
      tip=foot;root=blend(YOUNG_BODY,corner(ROOT,rp,sf),age);
    } else {
      // Named, monocolor and jewel bodies keep their inherited pigment.
      // Identical features and UV boundaries use inherited accent/value
      // contrast instead of turning every colored Chao maroon or violet.
      boolean contrasting=in.twoTone&&distance(body,in.accent)>=18;
      int accent=contrasting?in.accent:tone(body,.76f);
      limb=accent;foot=blend(accent,lighten(accent,.15f),age);
      belly=contrasting?accent:tone(body,.70f);
      wing=contrasting?accent:tone(body,.74f);
      tip=foot;root=contrasting?blend(body,accent,.40f):tone(body,.82f);
    }
    // The spike marker has its own species pigment, independent of all coat
    // and shiny genes, including when either specialization recedes.
    int marker=blend(YOUNG_MARKER,corner(MARKER,rp,sf),age);
    return new Colors(body,limb,foot,belly,wing,tip,marker,body,tip,root,limb);
  }

  /** These four otherwise transparent texels are this profile's only alpha
   * exception; older profiles retain their exact original swatch semantics. */
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
    if((flags&HeroRunRegionPaint055.TAIL)!=0)target=c.body;
    else if(accent&&(flags&HeroRunRegionPaint055.LIMB)!=0) {
      // Original hand/toe islands keep compact block shapes. Their two shades
      // describe the cuff and distal tip without adding decorative geometry.
      int band=HeroNormalRegionPaint057.limbBand(x,y);
      if(band==1)target=c.foot;
      else if(band==3)target=c.body;
    }
    else if(bellyUv(x,y))target=c.belly;
    return tint(source,target,reference);
  }
  /** A small flat U on the original torso front stands for the viewer's chest
   * crescent. It does not alter the body or any accessory attachment. */
  static boolean bellyUv(int x,int y) {
    return x>=10&&x<14&&y>=35&&y<40&&(y>=38||x==10||x==13);
  }
  static boolean paletteProtected(String path,String layout,int x,int y,int width,int height) {
    return path.startsWith("dynamic/dark_normal060/")&&layout.equals("baby")&&width>0&&height>0
        &&HeroRunMarkerPaint056.markerUv(x*128/width,y*64/height);
  }
  static int marker(float maturity,float rp,float sf){return blend(YOUNG_MARKER,corner(MARKER,rp,sf),maturity);}
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
