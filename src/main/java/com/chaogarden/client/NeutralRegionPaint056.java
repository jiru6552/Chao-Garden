package com.chaogarden.client;

/** Pure region paint for the first-type Neutral adult foundation. The viewer
 * supplies the cyan/green/pink placement, not these approximate RGB values or
 * the inherited color/shiny policy. Original face pixels and alpha stay exact. */
final class NeutralRegionPaint056 {
  record Input(boolean natural,boolean twoTone,float maturity,float runPower,
               float swimFly,int body,int accent,int wing,int marker) {}
  record Colors(int body,int limb,int wing,int marker,int featureBody,int featureTip) {}
  private NeutralRegionPaint056() {}

  static Colors colors(Input in) {
    float age=unit(in.maturity),fly=Math.max(0,axis(in.swimFly)),run=Math.max(0,-axis(in.runPower));
    int body=in.body,limb=in.accent,wing=in.wing;
    if(in.natural) {
      // These nine specializations share the Neutral first form's cyan body,
      // green extremities and pink wings. Run-primary lime and Power-primary
      // red palettes belong to other first forms and are deliberately absent.
      body=blend(body,rgb(42,231,224),age);
      limb=blend(limb,rgb(54,226,91),age);
      wing=blend(wing,rgb(239,153,193),age);
    } else if(!in.twoTone||distance(body,limb)<18) {
      // Same feature placement for jewel/monocolor coats, with bounded value
      // contrast instead of painting their inherited body cyan or green.
      limb=blend(limb,tone(body,.79f),age);
    }
    if(!in.twoTone)wing=in.wing;
    // The Run+Fly crown loses most of its green cap; the pure Fly hook and
    // Power+Fly stub retain their green tips in the viewer.
    int tip=blend(limb,body,fly*run*.88f);
    // All nine second influences share the viewer's peach young marker,
    // becoming yellow with biological maturity. It is not a coat region.
    return new Colors(body,limb,wing,HeroRunMarkerPaint056.neutralAdult(age),body,tip);
  }

  static int pixel(Input in,Colors c,int x,int y,int source,int flags) {
    int reserved=HeroRunRegionPaint055.reservedKind(x,y);
    if(reserved!=0)return reserved==1?c.featureBody:c.featureTip;
    if(source>>>24==0||(flags&HeroRunRegionPaint055.PROTECTED)!=0)return source;
    if((flags&HeroRunRegionPaint055.MARKER)!=0)return source&0xff000000|c.marker&0xffffff;
    if((flags&HeroRunRegionPaint055.WING)!=0) {
      return in.natural?tint(source,c.wing,in.wing):source;
    }
    if((flags&HeroRunRegionPaint055.SKIN)==0)return source;
    boolean accent=(flags&HeroRunRegionPaint055.ACCENT)!=0;
    if(!in.natural&&!accent)return source;
    int reference=accent?in.accent:in.body;
    return tint(source,accent?c.limb:c.body,reference);
  }
  /** Per-channel ratios retain authored pixels exactly at zero maturity; a
   * max-channel replacement would flatten colored shading even at age zero. */
  private static int tint(int source,int target,int reference){
    int result=source&0xff000000;
    for(int c=0;c<3;c++) {
      int original=source>>>(c*8)&255,base=reference>>>(c*8)&255,next=target>>>(c*8)&255;
      int value=base==0?(next==0?original:Math.round(original*next/(float)Math.max(1,max(reference)))):Math.round(original*next/(float)base);
      result|=Math.clamp(value,0,255)<<(c*8);
    }
    return result;
  }
  private static float axis(float v){return Float.isFinite(v)?Math.clamp(v,-1,1):0;}
  private static float unit(float v){return Float.isFinite(v)?Math.clamp(v,0,1):0;}
  private static int max(int p){return HeroRunRegionPaint055.maxChannel(p);}
  private static int rgb(int r,int g,int b){return HeroRunRegionPaint055.rgb(r,g,b);}
  private static int blend(int a,int b,float t){return HeroRunRegionPaint055.blend(a,b,t);}
  private static int tone(int p,float s){return HeroRunRegionPaint055.tone(p,s);}
  private static int distance(int a,int b){return Math.max(Math.abs((a&255)-(b&255)),Math.max(Math.abs((a>>>8&255)-(b>>>8&255)),Math.abs((a>>>16&255)-(b>>>16&255))));}
}
