package com.chaogarden.client;

import com.chaogarden.core.ChaoFaceRules076;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Small opaque pixel quads fitted to the original skull landmarks; no new head geometry or atlas. */
final class ChaoFaceGlyphs076 {
  record Pixel(float x,float y,float width,float height,int color,float depth) {
    Pixel(float x,float y,float width,float height,int color) {this(x,y,width,height,color,0);}
  }
  // Positive skull-local z is farther into the face. Keep contrasting backing
  // behind ink, avoiding coplanar raster seams while retaining every toy anchor.
  private static final float BACKING_DEPTH=.002f;
  private record EyeKey(ChaoFaceRules076.Eyes eyes,ChaoFaceRules076.Style style,int iris,int side) {}
  // Keys are bounded by9 eye states ×7 styles ×12 pigments ×2 sides.
  private static final Map<EyeKey,List<Pixel>> EYE_CACHE=new HashMap<>();
  private static final Map<ChaoFaceRules076.Mouth,List<Pixel>> MOUTH_CACHE=new EnumMap<>(ChaoFaceRules076.Mouth.class);
  private static final int INK=0xff17232b,WHITE=0xfff8fbff;
  private static final int[] IRIS={INK,0xff4a9df1,0xff48bd67,0xffdf505c,0xffffd852,
      0xff9b68d9,0xffee8ab5,0xffef9b47,0xff48d5d2};
  private static final String[] OVAL={"..######..",".########.","##########","##########",
      "##########","##########","##########","##########","##########","##########",
      "##########","##########",".########.","..######.."};
  private ChaoFaceGlyphs076() {}

  /** Coordinates are normalized to one eye's centre; positive y follows the skull's downward axis. */
  static List<Pixel> eye(ChaoFaceRules076.Resolved face,int side) {
    int iris=face.irisColor()>=0&&face.irisColor()<=11?face.irisColor():0;
    var key=new EyeKey(face.eyes(),face.style(),iris,side<0?-1:1);
    return EYE_CACHE.computeIfAbsent(key,ChaoFaceGlyphs076::buildEye);
  }

  private static List<Pixel> buildEye(EyeKey key) {
    var out=new ArrayList<Pixel>();var eyes=key.eyes();int side=key.side();
    // Neutral's white closed-eye panel is source-observed. Sharing that backing
    // for Hero/Dark closed marks is an authored readability choice, not an inversion.
    if(eyes==ChaoFaceRules076.Eyes.HAPPY_CLOSED||eyes==ChaoFaceRules076.Eyes.DISTRESS
        ||eyes==ChaoFaceRules076.Eyes.SLEEP||eyes==ChaoFaceRules076.Eyes.BLINK)addOval(out,WHITE);
    if(eyes==ChaoFaceRules076.Eyes.HAPPY_CLOSED) {
      pattern(out,new String[]{"..........","..........","..........","..######..",
          ".##....##.","##......##"},-.10f,INK);return List.copyOf(out);
    }
    if(eyes==ChaoFaceRules076.Eyes.DISTRESS) {
      String[] squeeze=side<0?new String[]{"##......","..##....","....##..","......##","....##..","..##....","##......"}
          :new String[]{"......##","....##..","..##....","##......","..##....","....##..","......##"};
      pattern(out,squeeze,0,INK);return List.copyOf(out);
    }
    if(eyes==ChaoFaceRules076.Eyes.SLEEP||eyes==ChaoFaceRules076.Eyes.BLINK) {
      pattern(out,eyes==ChaoFaceRules076.Eyes.SLEEP
          ?new String[]{"##......##",".########.","..######.."}
          :new String[]{"##########","##########"},.05f,INK);return List.copyOf(out);
    }
    boolean hero=key.style()==ChaoFaceRules076.Style.HERO||key.style()==ChaoFaceRules076.Style.HERO_LENS;
    boolean dark=key.style()==ChaoFaceRules076.Style.DARK||key.style()==ChaoFaceRules076.Style.DARK_LENS;
    boolean solid=key.style()==ChaoFaceRules076.Style.CHAOS_LENS
        ||eyes==ChaoFaceRules076.Eyes.CHAOS&&key.iris()<9;
    int iris=key.iris(),pupil=iris>=1&&iris<=8?IRIS[iris]:hero||dark?WHITE:INK;
    int sclera=solid?(iris>=1&&iris<=8?IRIS[iris]:0xff48d979):hero?0xff101820:dark?0xff35afea:WHITE;
    // A Dark resting face has the inward lid; temporary surprise has a clear round opening.
    boolean mean=(eyes==ChaoFaceRules076.Eyes.MEAN||dark&&eyes==ChaoFaceRules076.Eyes.NORMAL)&&!hero;
    boolean half=eyes==ChaoFaceRules076.Eyes.HALF_CLOSED;
    boolean surprised=eyes==ChaoFaceRules076.Eyes.SURPRISED;
    for(int y=0;y<OVAL.length;y++)for(int x=0;x<10;x++) {
      if(OVAL[y].charAt(x)!='#')continue;
      int inner=side<0?x:9-x;
      int lid=mean?2+inner/2:half?6:0;
      if(y<lid)continue;
      boolean pupilPixel=!solid&&x>=3&&x<=6&&y>=3&&y<=10;
      if(surprised)pupilPixel=!solid&&x>=4&&x<=5&&y>=4&&y<=9;
      if(pupilPixel&&(x==3||x==6)&&(y==3||y==10))pupilPixel=false;
      int color=pupilPixel?pupil:sclera;
      // Outline only the Dark blue oval, independent of any body/negative palette.
      if(dark&&!solid&&!pupilPixel&&(x==0||x==9||y==0||y==13))color=0xff207ebd;
      out.add(new Pixel((x-5)*.20f,(y-7)*.15f,.20f,.15f,color));
    }
    return List.copyOf(out);
  }

  static List<Pixel> mouth(ChaoFaceRules076.Mouth mouth) {
    return MOUTH_CACHE.computeIfAbsent(mouth,ChaoFaceGlyphs076::buildMouth);
  }

  private static List<Pixel> buildMouth(ChaoFaceRules076.Mouth mouth) {
    var out=new ArrayList<Pixel>();
    String[] pixels=switch(mouth) {
      case NONE->new String[0];
      case SMILE->new String[]{"##........##",".##......##.","..########..","...######..."};
      case FROWN->new String[]{"...######...","..########..",".##......##.","##........##"};
      case FLAT->new String[]{"############","############"};
      case GRIN->new String[]{"############","#WWW#WW#WWW#",".##########.","..########.."};
      case WAVY->new String[]{".###....###.","##.##..##.##","....####...."};
      case OPEN->new String[]{"..######..",".########.","##########","##########",".########.","..######.."};
    };
    if(pixels.length==0)return List.of();
    float pixel=.14f;
    // A thin light dilation makes the ink readable on inherited black coats,
    // without repainting the face or changing the original mouth anchor.
    for(int y=0;y<pixels.length;y++)for(int x=0;x<pixels[y].length();x++)if(pixels[y].charAt(x)!='.')
      out.add(new Pixel((x-pixels[y].length()/2f)*pixel-.04f,
          (y-pixels.length/2f)*pixel-.04f,pixel+.08f,pixel+.08f,WHITE,BACKING_DEPTH));
    for(int y=0;y<pixels.length;y++)for(int x=0;x<pixels[y].length();x++) {
      char c=pixels[y].charAt(x);if(c=='.')continue;
      out.add(new Pixel((x-pixels[y].length()/2f)*pixel,(y-pixels.length/2f)*pixel,pixel,pixel,c=='W'?WHITE:INK));
    }
    return List.copyOf(out);
  }

  private static void addOval(List<Pixel> out,int color) {
    for(int y=0;y<OVAL.length;y++)for(int x=0;x<10;x++)if(OVAL[y].charAt(x)=='#')
      out.add(new Pixel((x-5)*.20f,(y-7)*.15f,.20f,.15f,color,BACKING_DEPTH));
  }

  private static void pattern(List<Pixel> out,String[] rows,float offset,int color) {
    float pixel=.20f;
    for(int y=0;y<rows.length;y++)for(int x=0;x<rows[y].length();x++)if(rows[y].charAt(x)=='#')
      out.add(new Pixel((x-rows[y].length()/2f)*pixel,(y-rows.length/2f)*pixel+offset,pixel,pixel,color));
  }
}
