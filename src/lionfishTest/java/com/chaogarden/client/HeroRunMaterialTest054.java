package com.chaogarden.client;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.awt.image.BufferedImage;
import java.awt.Color;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.imageio.ImageIO;

/** Source-image and actual material policy checks; no OpenGL texture is
 * manufactured here and a mathematical pass is not a client visual claim. */
public final class HeroRunMaterialTest054 {
  private static final Path ROOT=Path.of("src/main/resources/assets/chao_garden");
  private static int checks,coats,paintedPixels,protectedPixels,patternSamples;
  private static void check(boolean condition,String reason){checks++;if(!condition)throw new AssertionError(reason);}
  private static int rgba(BufferedImage image,int x,int y){int argb=image.getRGB(x,y);return argb&0xff000000|(argb>>>16&255)|(argb&255)<<16|argb&0xff00;}
  private static BufferedImage image(Path path)throws Exception{return ImageIO.read(path.toFile());}
  private static JsonObject json(String path)throws Exception{return JsonParser.parseString(Files.readString(ROOT.resolve(path))).getAsJsonObject();}
  private static int max(int pixel){return Math.max(pixel&255,Math.max(pixel>>>8&255,pixel>>>16&255));}
  private static boolean mask(BufferedImage image,int x,int y){return image.getRGB(x,y)>>>24!=0;}
  private static int sample(BufferedImage image,int x,int y){return rgba(image,x*image.getWidth()/128,y*image.getHeight()/64);}
  private static HeroRunRegionPaint054.Input input(boolean natural,boolean two,boolean adult,float h,float d,float r,float g,int body,int accent,int wing) {
    return new HeroRunRegionPaint054.Input(natural,two,adult,h,d,r,g,body,accent,wing,
        HeroRunRegionPaint054.rgb(237,246,245),HeroRunRegionPaint054.rgb(61,159,228),
        HeroRunRegionPaint054.rgb(47,54,59),HeroRunRegionPaint054.rgb(228,95,101));
  }
  private static void contract()throws Exception {
    var model=json("models/entity/gameplay/chao_hero_run_proto054.json");
    check(model.get("texture_width").getAsInt()==64&&model.get("texture_height").getAsInt()==32,"new material changed retained atlas scale");
    var regions=model.getAsJsonObject("hero_run_prototype_054").getAsJsonObject("semantic_regions_pixels");
    Map<String,List<Integer>> expected=Map.ofEntries(Map.entry("belly",List.of(80,32,96,48)),Map.entry("hand",List.of(96,32,112,48)),
        Map.entry("foot",List.of(64,48,80,64)),Map.entry("crown",List.of(80,48,96,64)),Map.entry("sole",List.of(96,48,112,64)),Map.entry("body",List.of(112,63)),
        Map.entry("limb",List.of(113,63)),Map.entry("rear_head",List.of(114,63)),Map.entry("crown_tip",List.of(115,63)),
        Map.entry("marker",List.of(116,63)),Map.entry("tail_tip",List.of(117,63)));
    check(regions.keySet().equals(expected.keySet()),"semantic region names diverged from material contract");
    Set<String> used=new HashSet<>();
    for(var entry:expected.entrySet()) {
      List<Integer> actual=new ArrayList<>();for(var value:regions.getAsJsonArray(entry.getKey()))actual.add(value.getAsInt());
      check(actual.equals(entry.getValue()),"semantic rectangle differs: "+entry.getKey());
    }
    for(var bone:model.getAsJsonArray("bones"))for(var mesh:bone.getAsJsonObject().getAsJsonArray("meshes")) {
      var part=mesh.getAsJsonObject();
      for(var face:part.getAsJsonArray("faces")) {
        var surface=face.getAsJsonObject();check(!surface.has("hero_run_uv_052"),"coat-gated UV override survived in unified model");
        float u=0,v=0;var uv=surface.getAsJsonArray("uv");
        for(var point:uv){u+=point.getAsJsonArray().get(0).getAsFloat()*2;v+=point.getAsJsonArray().get(1).getAsFloat()*2;}
        u/=uv.size();v/=uv.size();
        for(var entry:expected.entrySet()) {
          var r=entry.getValue();
          if(r.size()==2&&u>=r.get(0)&&u<r.get(0)+1&&v>=r.get(1)&&v<r.get(1)+1
              ||r.size()==4&&u>=r.get(0)&&u<r.get(2)&&v>=r.get(1)&&v<r.get(3))used.add(entry.getKey());
        }
      }
    }
    check(used.containsAll(List.of("belly","hand","foot","crown","marker")),"a visible anatomy region is not mapped: "+used);
    int reserved=0;
    for(int y=0;y<64;y++)for(int x=0;x<128;x++) {
      boolean declared=false;
      for(var r:expected.values())declared|=r.size()==2?x==r.get(0)&&y==r.get(1):x>=r.get(0)&&x<r.get(2)&&y>=r.get(1)&&y<r.get(3);
      check(HeroRunRegionPaint054.reserved(x,y)==declared,"material paints outside declared region at "+x+","+y);
      if(declared)reserved++;
      else check(HeroRunRegionPaint054.reservedKind(x,y)==0,"unreserved source pixel entered palette mask");
      check(ChaoPaletteMaterial.prototypeReservedKind("dynamic/hero_run054/test","baby",x,y,128,64)==HeroRunRegionPaint054.reservedKind(x,y),"runtime palette mask diverges");
      check(ChaoPaletteMaterial.prototypeReservedKind("textures/entity/chao/official/coats/baby/red_two.png","baby",x,y,128,64)==0,"new mask touches static source");
      check(ChaoPaletteMaterial.prototypeReservedKind("dynamic/hero_run054/test","overlay",x,y,128,64)==0,"new mask touches animal overlay");
    }
    check(reserved==1286,"unexpected semantic atlas area");
    check(HeroRunRegionPaint054.reservedKind(116,63)==0,"orb was enrolled in skin brush/palette effects");
    // Facial and wing UVs keep the exact source mapping/alpha silhouettes.
    var source=json("models/entity/gameplay/chao_baby.json");Map<String,JsonArray> protectedUv=new HashMap<>();
    for(var bone:source.getAsJsonArray("bones"))for(var mesh:bone.getAsJsonObject().getAsJsonArray("meshes")) {
      var b=bone.getAsJsonObject();var m=mesh.getAsJsonObject();
      if(m.get("material").getAsString().equals("lens")||b.get("name").getAsString().startsWith("wing_"))
        protectedUv.put(b.get("name").getAsString()+":"+m.get("name").getAsString(),m.getAsJsonArray("faces"));
    }
    for(var bone:model.getAsJsonArray("bones"))for(var mesh:bone.getAsJsonObject().getAsJsonArray("meshes")) {
      var b=bone.getAsJsonObject();var m=mesh.getAsJsonObject();var original=protectedUv.remove(b.get("name").getAsString()+":"+m.get("name").getAsString());
      if(original==null)continue;var faces=m.getAsJsonArray("faces");check(original.size()==faces.size(),"protected panel topology changed");
      for(int i=0;i<faces.size();i++)check(original.get(i).getAsJsonObject().get("uv").equals(faces.get(i).getAsJsonObject().get("uv")),"eye/wing source UV changed");
    }
    check(protectedUv.isEmpty(),"protected panel disappeared: "+protectedUv.keySet());
  }
  private static void textures()throws Exception {
    var directory=ROOT.resolve("textures/entity/chao/official/coats/baby");
    var skin=image(ROOT.resolve("textures/entity/chao/gameplay/masks/baby.png"));
    var wingMask=image(ROOT.resolve("textures/entity/chao/gameplay/masks/baby_wings.png"));
    var accentMask=image(ROOT.resolve("textures/entity/chao/gameplay/masks/baby_accent.png"));
    try(var paths=Files.list(directory)) {
      for(var path:paths.filter(p->p.toString().endsWith(".png")).sorted().toList()) {
        var source=image(path);check(source.getWidth()==128&&source.getHeight()==64,"unexpected source coat dimensions");coats++;
        String name=path.getFileName().toString();boolean natural=name.startsWith("normal_"),two=name.contains("_two");
        int body=sample(source,12,0),accent=sample(source,48,0),wing=body;
        for(int y=0;y<64;y++)for(int x=0;x<128;x++)if(mask(wingMask,x,y)&&rgba(source,x,y)>>>24!=0){wing=rgba(source,x,y);break;}
        // Full source images, both ages and zero/intermediate/maximum targets.
        for(boolean adult:new boolean[]{false,true})for(int stage=0;stage<=4;stage++) {
          float f=stage/4f;var input=input(natural,two,adult,1,0,f,f,body,accent,wing);var colors=HeroRunRegionPaint054.colors(input);
          if(!natural)check(colors.body()==body,"inherited base color changed: "+name);
          boolean oneHue=!natural&&(!two||name.startsWith("jewel_")&&hueDistance(body,accent)<.04f);
          for(int y=0;y<64;y++)for(int x=0;x<128;x++) {
            int pixel=rgba(source,x,y),actual;
            if(HeroRunRegionPaint054.reserved(x,y)) {
              check(pixel>>>24==0,"new region overwrites an occupied source pixel: "+name+" "+x+","+y);
              actual=HeroRunRegionPaint054.pixel(colors,x,y);check(actual>>>24==255,"new region has a transparent hole");patternSamples++;
              if(oneHue&&!(x==116&&y==63))check(hueDistance(body,actual)<.04f,"mono/jewel detail acquired another hue: "+name);
            }else {
              boolean w=mask(wingMask,x,y),s=mask(skin,x,y),a=mask(accentMask,x,y);
              actual=HeroRunRegionPaint054.originalPixel(input,colors,pixel,s,w,a,max(w?wing:a?accent:body));
              check(actual>>>24==pixel>>>24,"source alpha changed: "+name);paintedPixels++;
              if(!s&&!w||pixel>>>24==0||!natural){check(actual==pixel,"protected inherited/face/alpha pixel changed: "+name);protectedPixels++;}
            }
          }
          // Every coat, including a mono/jewel, retains a visible adult pattern.
          if(adult) {
            Set<Integer> bands=new HashSet<>();for(int y=32;y<48;y++)bands.add(HeroRunRegionPaint054.pixel(colors,100,y));
            check(bands.size()>=2,"adult limb detail blanked for "+name);
            check(HeroRunRegionPaint054.pixel(colors,80,32)==colors.body(),"belly color spread over white outer torso");
          }else {
            check(HeroRunRegionPaint054.pixel(colors,100,32)==colors.body(),"baby proximal hand lost body color "+name);
            check(HeroRunRegionPaint054.pixel(colors,100,47)==colors.primary(),"baby distal hand cap missing "+name);
            check(HeroRunRegionPaint054.pixel(colors,70,63)==colors.body(),"baby foot back lost body color "+name);
            check(HeroRunRegionPaint054.pixel(colors,100,63)==colors.body(),"baby sole back lost body color "+name);
            // A cap is a monotonic gradient, not alternating adult stripes.
            int first=HeroRunRegionPaint054.pixel(colors,100,32),last=HeroRunRegionPaint054.pixel(colors,100,47);
            for(int c=0;c<3;c++) {
              int previous=first>>>(8*c)&255,endpoint=last>>>(8*c)&255;boolean increasing=endpoint>=previous;
              for(int y=33;y<48;y++) {
                int current=HeroRunRegionPaint054.pixel(colors,100,y)>>>(8*c)&255;
                check(increasing?current>=previous:current<=previous,"adult stripes leaked onto baby "+name);previous=current;
              }
            }
          }
        }
      }
    }
    check(coats==172,"source coat coverage changed; review every registered coat");
  }
  private static float hueDistance(int a,int b) {
    float[] source=Color.RGBtoHSB(a&255,a>>>8&255,a>>>16&255,null);
    float[] target=Color.RGBtoHSB(b&255,b>>>8&255,b>>>16&255,null);
    if(source[1]<.035f)return target[1]<.04f?0:1;
    float distance=Math.abs(source[0]-target[0]);return Math.min(distance,1-distance);
  }
  private static void endpointsAndContinuity() {
    int body=HeroRunRegionPaint054.rgb(138,239,255),accent=HeroRunRegionPaint054.rgb(236,255,141),wing=HeroRunRegionPaint054.rgb(200,158,222);
    var normal=HeroRunRegionPaint054.colors(input(true,true,false,0,0,0,0,body,accent,wing));
    var hero=HeroRunRegionPaint054.colors(input(true,true,false,1,0,0,0,body,accent,wing));
    var run=HeroRunRegionPaint054.colors(input(true,true,false,1,0,1,0,body,accent,wing));
    var neutralRun=HeroRunRegionPaint054.colors(input(true,true,false,0,0,1,0,body,accent,wing));
    check(normal.body()==body,"normal source body not retained");
    check(neutralRun.body()==HeroRunRegionPaint054.rgb(20,230,220),"Neutral Run child body must remain cyan");
    check(neutralRun.bellyRim()==neutralRun.body(),"Neutral Run child acquired a yellow Hero belly");
    check(neutralRun.wing()==HeroRunRegionPaint054.rgb(245,210,55),"Neutral Run child wings must be gold-yellow");
    check(hero.marker()==HeroRunRegionPaint054.rgb(70,160,230),"Hero child zero-Run orb is not blue");
    check(run.marker()==HeroRunRegionPaint054.rgb(242,143,174),"Hero child max-Run orb is not pink");
    check(hero.bellyRim()==HeroRunRegionPaint054.rgb(245,215,78),"Hero child zero-Run belly is not yellow");
    check(run.bellyRim()==HeroRunRegionPaint054.rgb(139,201,231),"Hero child max-Run belly is not pale blue");
    check(run.primary()==HeroRunRegionPaint054.rgb(116,223,174),"Hero child max-Run limb cap is not green");
    check(run.limb()==run.body(),"Hero child upper arm should remain white");
    var young=HeroRunRegionPaint054.colors(input(true,true,true,1,0,0,0,body,accent,wing));
    var full=HeroRunRegionPaint054.colors(input(true,true,true,1,0,1,1,body,accent,wing));
    check(young.bellyCore()==HeroRunRegionPaint054.rgb(113,34,203),"young adult violet inset missing");
    check(full.bellyRim()==HeroRunRegionPaint054.rgb(241,218,50),"mature Run yellow rim missing");
    check(full.wing()==HeroRunRegionPaint054.rgb(28,177,165),"mature Run teal wing missing");
    for(boolean adult:new boolean[]{false,true}) {
      int[] previous=null;
      for(int step=0;step<=256;step++) {
        float f=step/256f;var colors=HeroRunRegionPaint054.colors(input(true,true,adult,.25f+.75f*f,0,f,f,body,accent,wing));
        int[] pixels=new int[1286];int at=0;
        for(int y=0;y<64;y++)for(int x=0;x<128;x++)if(HeroRunRegionPaint054.reserved(x,y)) {
          int pixel=HeroRunRegionPaint054.pixel(colors,x,y);pixels[at]=pixel;
          if(previous!=null)for(int c=0;c<3;c++)check(Math.abs((pixel>>>(8*c)&255)-(previous[at]>>>(8*c)&255))<=3,"small material target step snapped");
          at++;
        }
        previous=pixels;
      }
    }
  }
  public static void main(String[] args)throws Exception {
    contract();textures();endpointsAndContinuity();
    String result="Hero Run054 material: "+checks+" checks; "+coats+" source coats; "+paintedPixels+" original pixels; "+protectedPixels+" exact protected pixels; "+patternSamples+" semantic pattern samples; source alpha/eye/wing UV, inherited color/mono/jewel detail, child/adult regions and dense continuity passed\n";
    var report=Path.of("build/reports/lionfish/hero-run-material054-results.txt");Files.createDirectories(report.getParent());Files.writeString(report,result);System.out.print(result);
  }
}
