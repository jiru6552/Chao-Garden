package com.chaogarden.client;

/** Baby-only Power pigments on the retained atlas; facial alpha and genes stay intact. */
final class BabyPowerRegionPaint084 {
  record Colors(int body,int accent,int wing,int marker,int belly,int featureBody,int featureTip) {}
  private BabyPowerRegionPaint084() {}

  static Colors colors(HeroRunRegionPaint055.Input input,HeroRunRegionPaint055.Colors existing) {
    float hero=unit(input.hero()),dark=unit(input.dark());
    int marker=aligned(rgb(255,186,70),rgb(249,223,50),rgb(250,215,38),hero,dark);
    if(!input.natural()||!input.twoTone())
      return new Colors(existing.body(),existing.limb(),existing.wing(),marker,
          HeroRunRegionPaint055.tone(existing.body(),.72f),existing.featureBody(),existing.featureTip());
    int body=aligned(rgb(249,125,140),rgb(247,249,247),rgb(68,49,44),hero,dark);
    int accent=aligned(rgb(244,167,67),rgb(243,129,113),rgb(179,138,72),hero,dark);
    return new Colors(body,accent,
        aligned(rgb(63,183,146),rgb(214,157,107),rgb(226,198,67),hero,dark),marker,
        aligned(rgb(116,97,128),rgb(248,215,73),rgb(214,46,52),hero,dark),
        body,aligned(rgb(244,167,67),rgb(243,128,83),rgb(184,136,55),hero,dark));
  }

  static int pixel(HeroRunRegionPaint055.Input input,Colors colors,float growth,
                   int x,int y,int previous,int flags,int shadeReference) {
    float amount=input.adult()?0:unit(growth);
    if(amount==0)return previous;
    int reserved=HeroRunRegionPaint055.reservedKind(x,y);
    if(reserved!=0)return blend(previous,x==HeroRunRegionPaint055.FEATURE_BODY_X?colors.featureBody():colors.featureTip(),amount);
    if(previous>>>24==0||(flags&HeroRunRegionPaint055.PROTECTED)!=0)return previous;
    int target=previous;
    boolean natural=input.natural()&&input.twoTone();
    if((flags&HeroRunRegionPaint055.MARKER)!=0)target=shade(previous,colors.marker(),255);
    else if((flags&HeroRunRegionPaint055.WING)!=0){if(natural)target=shade(previous,colors.wing(),255);}
    else if((flags&HeroRunRegionPaint055.SKIN)!=0) {
      if(natural) {
        int pigment=(flags&HeroRunRegionPaint055.ACCENT)!=0?colors.accent():colors.body();
        // The Hero child reference has broad warm limb bands; preserve white
        // proximal patches and use the existing UV rows, without limb resizing.
        if((flags&HeroRunRegionPaint055.LIMB)!=0&&input.hero()>0) {
          int band=(y/2)%3;
          int heroBand=band==0?rgb(247,249,247):band==1?rgb(243,129,113):rgb(238,195,102);
          pigment=blend(pigment,heroBand,input.hero());
        }
        target=shade(previous,pigment,shadeReference);
      }
      if(x>=10&&x<14&&y>=36&&y<40)target=shade(previous,colors.belly(),shadeReference);
    }
    return previous&0xff000000|blend(previous,target,amount)&0xffffff;
  }

  private static int shade(int source,int target,int reference) {
    float shade=Math.clamp(HeroRunRegionPaint055.maxChannel(source)/(float)Math.max(1,reference),.5f,1.25f);
    return source&0xff000000|HeroRunRegionPaint055.tone(target,shade)&0xffffff;
  }
  private static int aligned(int neutral,int hero,int dark,float h,float d){return blend(blend(neutral,hero,h),dark,d);}
  private static int rgb(int r,int g,int b){return HeroRunRegionPaint055.rgb(r,g,b);}
  private static int blend(int a,int b,float w){return HeroRunRegionPaint055.blend(a,b,w);}
  private static float unit(float value){return Float.isFinite(value)?Math.clamp(value,0,1):0;}
}
