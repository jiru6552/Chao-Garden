package com.chaogarden.client;

/** Small authored pixel regions on the original baby atlas. The independently
 * operated viewer supplies Hero/Normal regional placement and relative hues;
 * these RGB values and inherited-coat contrasts are a blocky mod interpretation,
 * not copied game materials. Facial pixels and source alpha remain exact. */
final class HeroNormalRegionPaint057 {
  record Input(boolean natural,boolean twoTone,float maturity,float runPower,
               float swimFly,int body,int accent,int wing,int marker) {}
  record Colors(int body,int limb,int primary,int secondary,int wing,int marker,
                int featureBody,int featureTip,int belly,int back) {}
  private HeroNormalRegionPaint057() {}

  // Mature grid: Run/center/Power across; Swim/center/Fly down. Every corner
  // keeps the white body. Changes belong to the cap, extremities, small torso
  // markings, wings and halo; never a Run-first-type green or Power red coat.
  private static final int[] PRIMARY={
      rgb(42,169,223),rgb(55,184,224),rgb(72,155,235),
      rgb(54,136,236),rgb(67,172,228),rgb(102,91,238),
      rgb(120,151,229),rgb(164,153,212),rgb(135,126,231)};
  private static final int[] SECONDARY={
      rgb(56,230,161),rgb(88,222,171),rgb(113,221,223),
      rgb(53,221,170),rgb(113,207,231),rgb(220,101,219),
      rgb(134,210,211),rgb(198,183,226),rgb(220,125,216)};
  private static final int[] BELLY={
      rgb(86,215,221),rgb(158,229,201),rgb(172,232,128),
      rgb(116,155,232),rgb(246,220,72),rgb(239,213,74),
      rgb(180,176,201),rgb(237,214,199),rgb(220,183,111)};
  private static final int[] WING={
      rgb(207,235,168),rgb(239,226,165),rgb(244,226,112),
      rgb(139,228,206),rgb(246,216,81),rgb(244,200,77),
      rgb(211,226,188),rgb(238,218,162),rgb(245,199,85)};
  private static final int[] HALO={
      rgb(159,239,209),rgb(242,235,175),rgb(246,166,219),
      rgb(46,220,219),rgb(73,173,225),rgb(226,42,237),
      rgb(157,197,178),rgb(249,162,145),rgb(243,81,187)};
  private static final int YOUNG_HALO=rgb(150,224,229),YOUNG_BELLY=rgb(246,221,79);

  static Colors colors(Input in) {
    float age=unit(in.maturity),rp=axis(in.runPower),sf=axis(in.swimFly);
    int naturalPrimary=corner(PRIMARY,rp,sf),naturalSecondary=corner(SECONDARY,rp,sf);
    int body=in.body,primary,secondary,wing=in.wing,belly,back;
    boolean contrastingCoat=in.twoTone&&distance(body,in.accent)>=18;
    if(in.natural&&in.twoTone) {
      primary=blend(in.accent,naturalPrimary,age);
      secondary=blend(in.accent,naturalSecondary,age);
      // Canonical baby atlases retain pink wings even for their Hero coat;
      // the actual Hero/Normal young reference has yellow wings.
      wing=blend(rgb(244,205,84),corner(WING,rp,sf),age);
      belly=blend(YOUNG_BELLY,corner(BELLY,rp,sf),age);
      back=primary;
    } else {
      // The same UV markings on named, jewel and monocolor Chao use the
      // inherited body/accent pigments and bounded value contrast. Their
      // skin never becomes white, nor do their features become generic blue.
      int accent=contrastingCoat?in.accent:tone(body,.74f);
      float value=.84f+.16f*max(naturalPrimary)/255f;
      primary=blend(in.accent,tone(accent,value),age);
      secondary=blend(in.accent,lighten(accent,.22f),age);
      belly=contrastingCoat?in.accent:tone(body,.75f);
      back=contrastingCoat?in.accent:tone(body,.82f);
    }
    // Hero/Normal uses a pale-cyan fresh halo; mature hue depends on BOTH
    // compatible/opposing influences. This pigment is independent of coat.
    int halo=blend(YOUNG_HALO,corner(HALO,rp,sf),age);
    int tip=in.natural&&in.twoTone?blend(rgb(97,202,224),naturalPrimary,age)
        :contrastingCoat?primary:tone(body,.75f);
    return new Colors(body,primary,primary,secondary,wing,halo,body,tip,belly,back);
  }

  static int pixel(Input in,Colors c,int x,int y,int source,int flags) {
    int reserved=HeroRunRegionPaint055.reservedKind(x,y);
    if(reserved!=0)return reserved==1?c.featureBody:c.featureTip;
    if(source>>>24==0||(flags&HeroRunRegionPaint055.PROTECTED)!=0)return source;
    if((flags&HeroRunRegionPaint055.MARKER)!=0)return source&0xff000000|c.marker&0xffffff;
    if((flags&HeroRunRegionPaint055.WING)!=0)
      return in.natural&&in.twoTone?tint(source,c.wing,in.wing):source;
    if((flags&HeroRunRegionPaint055.SKIN)==0)return source;
    int result=source;
    boolean accent=(flags&HeroRunRegionPaint055.ACCENT)!=0;
    if(accent)result=tint(source,c.limb,in.accent);
    if(accent&&(flags&HeroRunRegionPaint055.LIMB)!=0) {
      int band=limbBand(x,y);
      if(band!=0) {
        int target=band==1?c.primary:band==2?c.secondary:c.body;
        result=blend(result,tint(source,target,in.accent),unit(in.maturity));
      }
    }
    // Two small, flat pixel patches on the retained front/rear torso UVs.
    // These never add decorative rings or shift a face/hand attachment.
    if(bellyUv(x,y))result=tint(source,c.belly,in.body);
    else if(backUv(x,y))result=tint(source,c.back,in.body);
    return source&0xff000000|result&0xffffff;
  }

  static boolean bellyUv(int x,int y){return x>=10&&x<14&&y>=36&&y<40;}
  static boolean backUv(int x,int y){return x>=26&&x<30&&y>=36&&y<40;}
  /** Authored broad rows on actual original hand/toe UV islands. White heel
   * pixels remain untouched because they are outside the accent mask. */
  static int limbBand(int x,int y) {
    if(x>=24&&x<40&&y>=30&&y<34)return y<32?2:1;
    if(x>=28&&x<32&&y>=26&&y<30)return 2; // hand upper face
    if(x>=32&&x<36&&y>=26&&y<30)return 3; // hand lower face
    if(x>=44&&x<68&&y>=26&&y<30)return y<28?1:2;
    if(x>=52&&x<56&&y>=18&&y<26)return y>=24?2:1;
    if(x>=56&&x<60&&y>=18&&y<26)return y>=24?2:1;
    return 0;
  }
  static boolean paletteProtected(String sourcePath,String layout,int x,int y,int width,int height) {
    return sourcePath.startsWith("dynamic/hero_normal057/")&&layout.equals("baby")&&width>0&&height>0
        &&HeroRunMarkerPaint056.markerUv(x*128/width,y*64/height);
  }
  static int halo(float maturity,float runPower,float swimFly){return blend(YOUNG_HALO,corner(HALO,runPower,swimFly),maturity);}
  private static int corner(int[] values,float rp,float sf){
    rp=axis(rp);sf=axis(sf);int result=0xff000000;
    for(int c=0;c<3;c++) {
      float v=0;for(int y=0;y<3;y++)for(int x=0;x<3;x++)
        v+=(values[y*3+x]>>>(c*8)&255)*Math.max(0,1-Math.abs(rp-(x-1)))*Math.max(0,1-Math.abs(sf-(y-1)));
      result|=Math.clamp(Math.round(v),0,255)<<(c*8);
    }
    return result;
  }
  /** Exact identity when target equals its source reference. Keep original
   * authored brightness without a generic max-channel flattening operation. */
  private static int tint(int source,int target,int reference){
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
