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
      rgb(42,170,227),rgb(43,205,193),rgb(61,165,231),
      rgb(40,140,243),rgb(56,173,228),rgb(78,89,239),
      rgb(124,153,225),rgb(170,157,215),rgb(126,120,225)};
  private static final int[] SECONDARY={
      rgb(48,224,182),rgb(45,179,171),rgb(61,214,211),
      rgb(40,216,197),rgb(108,207,235),rgb(158,128,235),
      rgb(133,205,205),rgb(199,184,228),rgb(177,155,229)};
  // The colored upper tip is independent of the white lower stalk. A broad
  // violet Power cap must not become the old bright-pink hand stripe.
  private static final int[] UPPER={
      rgb(47,181,226),rgb(49,224,207),rgb(75,171,237),
      rgb(49,142,246),rgb(66,178,232),rgb(116,78,237),
      rgb(125,155,228),rgb(171,158,219),rgb(135,124,231)};
  private static final int[] BELLY={
      rgb(86,215,221),rgb(158,229,201),rgb(172,232,128),
      rgb(116,155,232),rgb(246,220,72),rgb(239,213,74),
      rgb(180,176,201),rgb(237,214,199),rgb(220,183,111)};
  private static final int[] WING={
      rgb(214,232,153),rgb(247,229,89),rgb(250,207,51),
      rgb(145,229,205),rgb(248,216,47),rgb(251,195,39),
      rgb(183,207,177),rgb(235,203,139),rgb(239,184,63)};
  // Approximate colored distal span, not an extracted viewer/game formula.
  // The actual side/back references show a mostly colored Run/Power blade,
  // but broad white Fly/Swim roots and a much smaller colored outer region.
  private static final float[] WING_SPAN={.60f,.35f,.80f,.85f,.50f,.82f,.70f,.25f,.80f};
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
      wing=blend(rgb(248,216,47),corner(WING,rp,sf),age);
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
      wing=contrastingCoat?in.accent:tone(body,.72f);
      belly=contrastingCoat?in.accent:tone(body,.75f);
      back=contrastingCoat?in.accent:tone(body,.82f);
    }
    // Hero/Normal uses a pale-cyan fresh halo; mature hue depends on BOTH
    // compatible/opposing influences. This pigment is independent of coat.
    int halo=blend(YOUNG_HALO,corner(HALO,rp,sf),age);
    int tip=in.natural&&in.twoTone?blend(rgb(97,202,224),corner(UPPER,rp,sf),age)
        :contrastingCoat?primary:tone(body,.75f);
    return new Colors(body,primary,primary,secondary,wing,halo,body,tip,belly,back);
  }

  static int pixel(Input in,Colors c,int x,int y,int source,int flags) {
    int reserved=HeroRunRegionPaint055.reservedKind(x,y);
    if(reserved!=0)return reserved==1?c.featureBody:c.featureTip;
    if(source>>>24==0||(flags&HeroRunRegionPaint055.PROTECTED)!=0)return source;
    if((flags&HeroRunRegionPaint055.MARKER)!=0)return source&0xff000000|c.marker&0xffffff;
    if((flags&HeroRunRegionPaint055.WING)!=0) {
      // Both original wing faces share these semantic root-to-tip regions.
      // Source baby coats all have pink wing texels, including colored coats;
      // preserving that hue made the Hero references look like solid plates.
      // Keep their two pixel shades/alpha, but use the inherited body at the
      // broad root and the variant's pigment toward the outer tip.
      int target=blend(c.body,c.wing,wingAccentWeight(in,x));
      float shade=max(source)/(float)Math.max(1,max(in.wing));
      return source&0xff000000|tone(target,Math.clamp(shade,.6f,1.25f))&0xffffff;
    }
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
  static float wingAccentWeight(Input in,int x) {
    // Canonical front: outer u32 -> root u38. Back is mirrored u38 -> u44.
    float distance=x<38?(37.5f-x)/6f:(x-37.5f)/6f;
    float span=.45f+(corner(WING_SPAN,in.runPower,in.swimFly)-.45f)*unit(in.maturity);
    float edge=unit((distance-(1-span))/.40f+.5f);
    // The proximal root always retains its inherited base pigment. The soft
    // boundary moves continuously rather than switching entire pixel bands.
    return edge*unit((distance-.15f)/.20f);
  }
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
  private static float corner(float[] values,float rp,float sf) {
    rp=axis(rp);sf=axis(sf);float value=0;
    for(int y=0;y<3;y++)for(int x=0;x<3;x++)
      value+=values[y*3+x]*Math.max(0,1-Math.abs(rp-(x-1)))*Math.max(0,1-Math.abs(sf-(y-1)));
    return value;
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
