package com.chaogarden.client;

/** Coat-independent region layout on the retained 128 x 64 Baby atlas.
 * Values are ABGR, matching NativeImage. Color targets are authored from the
 * observed viewer regions; they are not extracted original-game palettes. */
final class HeroRunRegionPaint054 {
  static final int BELLY_X=80,BELLY_Y=32,HAND_X=96,HAND_Y=32,
      FOOT_X=64,FOOT_Y=48,CROWN_X=80,CROWN_Y=48,SOLE_X=96,SOLE_Y=48,SIZE=16,SWATCH_Y=63;
  static final int BODY_X=112,LIMB_X=113,REAR_X=114,CROWN_TIP_X=115,MARKER_X=116,TAIL_TIP_X=117;

  record Input(boolean natural,boolean twoTone,boolean adult,float hero,float dark,
               float run,float growth,int body,int accent,int wing,
               int heroBody,int heroAccent,int darkBody,int darkAccent) {}
  record Colors(int body,int limb,int rear,int crownTip,int marker,int primary,
                int secondary,int pale,int bellyRim,int bellyCore,int wing,int tailTip,
                boolean adult,float bellyAmount) {}
  private HeroRunRegionPaint054() {}

  static Colors colors(Input input) {
    float h=unit(input.hero),d=unit(input.dark),r=unit(input.run),g=unit(input.growth);
    if(!input.natural||!input.twoTone) {
      int body=opaque(input.body),accent=opaque(input.accent),wing=opaque(input.wing);
      if(input.natural) {
        if(input.adult)body=rgb(244,246,241);
        else {
          int ordinary=blend(blend(body,opaque(input.heroBody),h),opaque(input.darkBody),d);
          int trained=blend(blend(rgb(20,230,220),rgb(244,246,242),h),opaque(input.darkBody),d);
          body=blend(ordinary,trained,r);
        }
        accent=body;wing=body;
      }
      // A mono or jewel coat keeps one hue, but retains the same legible
      // rings/insets through value changes. Two-tone also uses its inherited
      // accent. No named or jewel coat is replaced by the natural white/blue.
      int primary=tone(body,.62f),secondary=input.twoTone&&rgbDistance(body,accent)>18
          ?accent:lighten(body,.16f),pale=lighten(body,.18f);
      int limb=input.twoTone&&rgbDistance(body,accent)>18?accent:tone(body,.90f);
      int core=blend(tone(body,.54f),tone(body,.79f),g);
      int rim=blend(primary,secondary,g);
      int marker=input.adult?rgb(245,220,69):childMarker(h,d,r);
      return new Colors(body,input.adult?limb:body,body,input.adult?secondary:limb,marker,
          input.adult?primary:limb,input.adult?secondary:limb,input.adult?pale:body,rim,core,
          wing,input.adult?secondary:limb,input.adult,Math.max(h,r*.65f));
    }
    if(input.adult) {
      int body=rgb(244,246,241),blue=rgb(20,132,239),white=rgb(238,246,243);
      int band=blend(rgb(244,216,66),rgb(24,218,210),g);
      return new Colors(body,white,body,blend(rgb(119,191,228),rgb(33,205,204),g),
          rgb(246,221,62),blue,band,white,
          blend(rgb(69,155,221),rgb(241,218,50),g),
          blend(rgb(113,34,203),rgb(77,172,217),g),
          blend(rgb(246,205,48),rgb(28,177,165),g),rgb(244,222,86),true,1);
    }
    int ordinaryBody=blend(blend(opaque(input.body),opaque(input.heroBody),h),opaque(input.darkBody),d);
    int ordinaryLimb=blend(blend(opaque(input.accent),opaque(input.heroAccent),h),opaque(input.darkAccent),d);
    int trainedBody=blend(blend(rgb(20,230,220),rgb(244,246,242),h),opaque(input.darkBody),d);
    int trainedLimb=blend(blend(rgb(30,220,90),rgb(116,223,174),h),opaque(input.darkAccent),d);
    int body=blend(ordinaryBody,trainedBody,r),limb=blend(ordinaryLimb,trainedLimb,r);
    int heroBelly=blend(rgb(245,215,78),rgb(139,201,231),r);
    int neutralBelly=blend(opaque(input.body),rgb(20,230,220),r);
    int belly=blend(blend(neutralBelly,heroBelly,h),opaque(input.darkBody),d);
    int marker=childMarker(h,d,r);
    int ordinaryWing=blend(opaque(input.wing),rgb(244,215,93),h);
    int trainedWing=blend(rgb(245,210,55),rgb(242,166,113),h);
    int wing=blend(blend(ordinaryWing,trainedWing,r),rgb(142,65,89),d);
    int tail=blend(blend(rgb(238,226,111),rgb(244,220,92),h),opaque(input.darkAccent),d);
    return new Colors(body,body,body,limb,marker,limb,limb,body,belly,
        tone(belly,.91f),wing,tail,false,Math.max(h,r*.65f));
  }

  static boolean reserved(int x,int y) {
    return rectangle(x,y,BELLY_X,BELLY_Y)||rectangle(x,y,HAND_X,HAND_Y)
        ||rectangle(x,y,FOOT_X,FOOT_Y)||rectangle(x,y,CROWN_X,CROWN_Y)||rectangle(x,y,SOLE_X,SOLE_Y)
        ||y==SWATCH_Y&&x>=BODY_X&&x<=TAIL_TIP_X;
  }
  /** Same semantic mask for every coat. White surrounds remain body-colored;
   * brush paint affects the accent bands, not the entire torso rectangle. */
  static int reservedKind(int x,int y) {
    if(!reserved(x,y))return 0;
    if(y==SWATCH_Y&&x>=BODY_X&&x<=TAIL_TIP_X) {
      if(x==MARKER_X)return 0; // Existing palette effects do not repaint the orb.
      return x==BODY_X||x==REAR_X?1:3;
    }
    if(rectangle(x,y,BELLY_X,BELLY_Y))return bellyRadius(x-BELLY_X,y-BELLY_Y)>1?1:3;
    if(rectangle(x,y,HAND_X,HAND_Y))return bandKind(y-HAND_Y)==0?1:3;
    if(rectangle(x,y,CROWN_X,CROWN_Y))return bandKind(y-CROWN_Y)==0?1:3;
    if(rectangle(x,y,SOLE_X,SOLE_Y))return bandKind(y-SOLE_Y)==0?1:3;
    return footKind(x-FOOT_X,y-FOOT_Y)==0?1:3;
  }

  /** Only called for declared reserved texels; existing eye/wing texels retain
   * their source UVs and alpha. No adult stripe pattern is painted on babies. */
  static int pixel(Colors colors,int x,int y) {
    if(!reserved(x,y))throw new IllegalArgumentException("Outside Hero Run054 atlas regions");
    if(y==SWATCH_Y&&x>=BODY_X&&x<=TAIL_TIP_X)return switch(x) {
      case BODY_X->colors.body;case LIMB_X->colors.limb;case REAR_X->colors.rear;
      case CROWN_TIP_X->colors.crownTip;case MARKER_X->colors.marker;default->colors.tailTip;
    };
    if(rectangle(x,y,BELLY_X,BELLY_Y)) {
      float radius=bellyRadius(x-BELLY_X,y-BELLY_Y);
      if(radius>1)return colors.body;
      int inset=radius>.39f?colors.bellyRim:colors.bellyCore;
      return colors.adult?inset:blend(colors.body,inset,colors.bellyAmount);
    }
    if(!colors.adult) {
      float amount;
      if(rectangle(x,y,HAND_X,HAND_Y))amount=smooth(((y-HAND_Y+.5f)/SIZE-.28f)/.50f);
      else if(rectangle(x,y,CROWN_X,CROWN_Y))amount=smooth(((y-CROWN_Y+.5f)/SIZE-.55f)/.40f);
      else if(rectangle(x,y,SOLE_X,SOLE_Y))amount=1-smooth(((y-SOLE_Y+.5f)/SIZE-.05f)/.30f);
      else amount=1-smooth(((y-FOOT_Y+.5f)/SIZE-.16f)/.56f);
      return blend(colors.body,colors.primary,amount);
    }
    int kind=rectangle(x,y,HAND_X,HAND_Y)?bandKind(y-HAND_Y)
        :rectangle(x,y,CROWN_X,CROWN_Y)?bandKind(y-CROWN_Y)
        :rectangle(x,y,SOLE_X,SOLE_Y)?bandKind(y-SOLE_Y):footKind(x-FOOT_X,y-FOOT_Y);
    return switch(kind){case 0->colors.pale;case 1->colors.primary;default->colors.secondary;};
  }
  /** Production's original-atlas paint path, kept pure so full source images
   * can be checked without creating client textures or an OpenGL context. */
  static int originalPixel(Input input,Colors colors,int pixel,boolean skin,boolean wing,
                           boolean accent,int shadeReference) {
    if(!input.natural||pixel>>>24==0||!skin&&!wing)return pixel;
    if(!input.adult&&unit(input.hero)==0&&unit(input.dark)==0&&unit(input.run)==0)return pixel;
    int target=wing?colors.wing:accent?colors.limb:colors.body;
    float shade=Math.clamp(maxChannel(pixel)/(float)Math.max(1,shadeReference),.50f,1.25f);
    return pixel&0xff000000|tone(target,shade)&0xffffff;
  }
  private static boolean rectangle(int x,int y,int left,int top){return x>=left&&x<left+SIZE&&y>=top&&y<top+SIZE;}
  private static float bellyRadius(int x,int y){float u=((x+.5f)/SIZE-.5f)/.43f,v=((y+.5f)/SIZE-.62f)/.55f;return u*u+v*v;}
  private static int bandKind(int y){int row=(y/2)%4;return row==1?1:row==3?2:0;}
  private static int footKind(int x,int y){
    float u=Math.abs((x+.5f)/SIZE-.5f)/.5f,v=Math.abs((y+.5f)/SIZE-.5f)/.5f;
    float ring=(float)Math.sqrt(u*u+v*v);
    return ring<.31f?0:ring<.47f?1:ring<.61f?0:ring<.78f?2:ring<1.02f?1:2;
  }
  static int rgb(int r,int g,int b){return 0xff000000|Math.clamp(b,0,255)<<16|Math.clamp(g,0,255)<<8|Math.clamp(r,0,255);}
  static int opaque(int value){return value|0xff000000;}
  static int blend(int a,int b,float amount){float t=unit(amount);int value=a&0xff000000;for(int c=0;c<3;c++)value|=Math.round((a>>>(8*c)&255)*(1-t)+(b>>>(8*c)&255)*t)<<(8*c);return value;}
  static int tone(int value,float scale){return rgb(Math.round((value&255)*scale),Math.round((value>>>8&255)*scale),Math.round((value>>>16&255)*scale));}
  static int lighten(int value,float amount){return blend(value,rgb(255,255,255),amount);}
  private static float unit(float value){return Float.isFinite(value)?Math.clamp(value,0,1):0;}
  private static float smooth(float value){float t=unit(value);return t*t*(3-2*t);}
  private static int childMarker(float hero,float dark,float run){
    int neutral=blend(rgb(249,235,105),rgb(38,226,127),run);
    int aligned=blend(rgb(70,160,230),rgb(242,143,174),run);
    return blend(blend(neutral,aligned,hero),rgb(233,94,109),dark);
  }
  private static int maxChannel(int pixel){return Math.max(pixel&255,Math.max(pixel>>>8&255,pixel>>>16&255));}
  private static int rgbDistance(int a,int b){return Math.max(Math.abs((a&255)-(b&255)),Math.max(Math.abs((a>>>8&255)-(b>>>8&255)),Math.abs((a>>>16&255)-(b>>>16&255))));}
}
