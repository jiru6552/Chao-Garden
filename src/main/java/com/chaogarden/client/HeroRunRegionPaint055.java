package com.chaogarden.client;

/** Pure pixel policy for the original baby atlas. New anatomy never changes
 * an inherited coat's hue or finish. Only two previously unused swatches are
 * added for the blocky features; eyes and original alpha stay untouched. */
final class HeroRunRegionPaint055 {
  static final int SKIN=1,ACCENT=2,WING=4,LIMB=8,TAIL=16,MARKER=32,PROTECTED=64;
  static final int FEATURE_BODY_X=127,FEATURE_TIP_X=126,FEATURE_Y=63;
  record Input(boolean natural,boolean twoTone,boolean adult,float hero,float dark,
               float run,float maturity,float growth,int body,int accent,int wing,int marker,
               int heroBody,int heroAccent,int darkBody,int darkAccent) {}
  record Colors(int body,int limb,int primary,int secondary,int bellyRim,int bellyCore,
                int wing,int marker,int featureBody,int featureTip,float bellyAmount) {}
  private HeroRunRegionPaint055() {}

  static Colors colors(Input input) {
    float h=unit(input.hero),d=unit(input.dark),r=unit(input.run),m=unit(input.maturity),g=unit(input.growth);
    int body=opaque(input.body),limb=opaque(input.accent),wing=opaque(input.wing);
    if(!input.natural&&rgbDistance(body,limb)<18)limb=tone(body,.72f);
    if(input.natural) {
      int ordinary=blend(blend(body,opaque(input.heroBody),h),opaque(input.darkBody),d);
      int trained=blend(blend(rgb(20,230,220),opaque(input.heroBody),h),opaque(input.darkBody),d);
      body=blend(ordinary,trained,r);
      int ordinaryLimb=blend(blend(limb,opaque(input.heroAccent),h),opaque(input.darkAccent),d);
      int trainedLimb=blend(blend(rgb(30,220,90),rgb(116,218,162),h),opaque(input.darkAccent),d);
      limb=blend(ordinaryLimb,trainedLimb,r);
      wing=blend(blend(opaque(input.wing),rgb(244,205,84),h),rgb(142,65,89),d);
      wing=blend(wing,blend(blend(rgb(245,209,66),rgb(242,166,113),h),rgb(142,65,89),d),r);
    }
    int childMarker=blend(blend(blend(opaque(input.marker),rgb(38,226,127),r),
        blend(rgb(70,160,230),rgb(240,145,177),r),h),rgb(233,94,109),d);
    if(!input.twoTone) {
      limb=tone(body,.88f);wing=lighten(body,.08f);
    }
    if(input.adult) {
      // The old-age Run-centered halo is lime; specializing Run returns it
      // toward yellow. This uses independent maturity and specialization.
      int marker=blend(rgb(246,221,62),rgb(154,224,48),Math.max(0,m-g));
      if(input.natural&&input.twoTone) {
        body=opaque(input.heroBody);
        int blue=rgb(35,143,230),secondary=blend(rgb(245,215,78),rgb(38,211,194),g);
        return new Colors(body,blue,blue,secondary,
            blend(rgb(68,154,222),rgb(241,216,60),g),
            blend(rgb(111,49,186),rgb(76,169,216),g),
            blend(rgb(244,205,84),rgb(72,183,157),g),marker,blue,secondary,1);
      }
      // Colored/jewel/mono coats retain their inherited body. The same small
      // pixel regions use value contrast, rather than becoming white/blue.
      int primary=input.twoTone?limb:tone(body,.62f);
      int secondary=input.twoTone?lighten(limb,.25f):lighten(body,.17f);
      return new Colors(body,limb,primary,secondary,primary,
          blend(tone(body,.55f),tone(body,.79f),g),wing,marker,primary,secondary,1);
    }
    int belly;
    if(input.natural&&input.twoTone) {
      int heroBelly=blend(rgb(245,215,78),rgb(142,202,230),r);
      belly=blend(blend(body,heroBelly,h),opaque(input.darkBody),d);
    } else belly=input.twoTone?limb:tone(body,.77f);
    return new Colors(body,limb,limb,lighten(limb,.18f),belly,belly,wing,childMarker,
        body,limb,Math.max(h,r*.65f));
  }

  /** Palette/brush metadata for exactly the two new swatches. */
  static int reservedKind(int x,int y) {
    if(y!=FEATURE_Y)return 0;
    return x==FEATURE_BODY_X?1:x==FEATURE_TIP_X?3:0;
  }

  /** Preserve the old compatible Swim/Fly color axis as a bounded accent
   * delta. At the centered axis this is exactly identity. Body/face and Run
   * growth stay independent; inherited mono/jewel hues use value only. */
  static Colors axisColors(Colors c,float[][][] palette,float sf,float power,float maturity,boolean preserveHue) {
    if(!Float.isFinite(sf)||Math.abs(sf)<.00001f||maturity<=0)return c;
    sf=Math.clamp(sf,-1,1);power=Math.clamp(power,-1,1);
    float amount=.25f*unit(maturity)*Math.abs(sf);int row=sf<0?0:2;
    float[][] delta=new float[3][3];
    for(int k=0;k<3;k++)for(int channel=0;channel<3;channel++)for(int x=0;x<3;x++) {
      float w=Math.max(0,1-Math.abs(power-(x-1)));
      delta[k][channel]+=(palette[row*3+x][k][channel]-palette[3+x][k][channel])*w;
    }
    return new Colors(c.body,axis(c.limb,delta[1],amount,preserveHue),
        axis(c.primary,delta[1],amount,preserveHue),axis(c.secondary,delta[1],amount,preserveHue),
        c.bellyRim,c.bellyCore,axis(c.wing,delta[2],amount,preserveHue),c.marker,
        c.featureBody,c.featureTip,c.bellyAmount);
  }
  private static int axis(int color,float[] delta,float amount,boolean preserveHue) {
    if(preserveHue)return tone(color,Math.max(.5f,1+(delta[0]+delta[1]+delta[2])*amount/(3*255)));
    return rgb(Math.round((color&255)+delta[0]*amount),Math.round((color>>>8&255)+delta[1]*amount),Math.round((color>>>16&255)+delta[2]*amount));
  }

  /** Original-atlas paint. Coordinates use the canonical128x64 layout. */
  static int pixel(Input input,Colors colors,int x,int y,int pixel,int heroPixel,
                   int darkPixel,int flags,int shadeReference) {
    int reserved=reservedKind(x,y);
    if(reserved!=0)return x==FEATURE_BODY_X?colors.featureBody:colors.featureTip;
    if(pixel>>>24==0||(flags&PROTECTED)!=0)return pixel;
    boolean natural=input.natural;
    boolean neutralBaseline=!input.adult&&unit(input.hero)==0&&unit(input.dark)==0&&unit(input.run)==0;
    if(neutralBaseline)return pixel;
    if((flags&MARKER)!=0) {
      if(input.adult)return shaded(pixel,colors.marker,shadeReference);
      int ordinary=blend(blend(pixel,shaded(pixel,rgb(70,160,230),255),input.hero),
          shaded(pixel,rgb(233,94,109),255),input.dark);
      int trained=blend(blend(rgb(38,226,127),rgb(240,145,177),input.hero),rgb(233,94,109),input.dark);
      return blend(ordinary,shaded(pixel,trained,255),input.run);
    }
    if((flags&WING)!=0) {
      if(!natural)return input.adult?pixel&0xff000000|tone(pixel,maxChannel(colors.wing)/(float)Math.max(1,maxChannel(input.wing)))&0xffffff:pixel;
      if(input.adult)return shaded(pixel,colors.wing,shadeReference);
      int ordinary=blend(blend(pixel,shaded(pixel,rgb(244,205,84),255),input.hero),
          shaded(pixel,rgb(142,65,89),255),input.dark);
      int trained=blend(blend(rgb(245,209,66),rgb(242,166,113),input.hero),rgb(142,65,89),input.dark);
      return blend(ordinary,shaded(pixel,trained,255),input.run);
    }
    if((flags&SKIN)==0)return pixel;
    int base=pixel;
    if(natural&&!input.adult) {
      base=blend(blend(pixel,heroPixel,unit(input.hero)),darkPixel,unit(input.dark));
    }
    int target=(flags&ACCENT)!=0?colors.limb:colors.body;
    int result=natural?shaded(base,target,shadeReference):base;
    if(natural&&!input.adult) {
      int trained=(flags&ACCENT)!=0
          ?blend(blend(rgb(30,220,90),rgb(116,218,162),input.hero),opaque(input.darkAccent),input.dark)
          :blend(blend(rgb(20,230,220),opaque(input.heroBody),input.hero),opaque(input.darkBody),input.dark);
      if(!input.twoTone)trained=tone(blend(blend(rgb(20,230,220),opaque(input.heroBody),input.hero),opaque(input.darkBody),input.dark),(flags&ACCENT)!=0?.88f:1);
      result=blend(base,shaded(base,trained,shadeReference),input.run);
    }
    if(input.adult&&(flags&LIMB)!=0) {
      // Two broad two-pixel bands on the retained UVs. No rings, remapped
      // skin or new mesh surfaces are needed for the adult markings.
      int band=(y/2)%3;
      int stripe=band==0?colors.body:band==1?colors.primary:colors.secondary;
      result=shaded(base,stripe,shadeReference);
    } else if(!natural&&(flags&ACCENT)!=0) {
      float amount=Math.max(unit(input.hero),unit(input.run));
      result=blend(base,shaded(base,colors.limb,shadeReference),amount);
    }
    if(x>=10&&x<14&&y>=36&&y<40) {
      int belly=input.adult&&(x==10||x==13||y==36||y==39)?colors.bellyRim:colors.bellyCore;
      result=blend(result,belly,colors.bellyAmount);
    }
    return pixel&0xff000000|result&0xffffff;
  }
  private static int shaded(int source,int target,int reference) {
    float shade=Math.clamp(maxChannel(source)/(float)Math.max(1,reference),.50f,1.25f);
    return source&0xff000000|tone(target,shade)&0xffffff;
  }
  static int rgb(int r,int g,int b){return 0xff000000|Math.clamp(b,0,255)<<16|Math.clamp(g,0,255)<<8|Math.clamp(r,0,255);}
  static int opaque(int value){return value|0xff000000;}
  static int blend(int a,int b,float amount){float t=unit(amount);int value=a&0xff000000;for(int c=0;c<3;c++)value|=Math.round((a>>>(8*c)&255)*(1-t)+(b>>>(8*c)&255)*t)<<(8*c);return value;}
  static int tone(int value,float scale){return rgb(Math.round((value&255)*scale),Math.round((value>>>8&255)*scale),Math.round((value>>>16&255)*scale));}
  static int lighten(int value,float amount){return blend(value,rgb(255,255,255),amount);}
  static int maxChannel(int pixel){return Math.max(pixel&255,Math.max(pixel>>>8&255,pixel>>>16&255));}
  private static int rgbDistance(int a,int b){return Math.max(Math.abs((a&255)-(b&255)),Math.max(Math.abs((a>>>8&255)-(b>>>8&255)),Math.abs((a>>>16&255)-(b>>>16&255))));}
  private static float unit(float value){return Float.isFinite(value)?Math.clamp(value,0,1):0;}
}
