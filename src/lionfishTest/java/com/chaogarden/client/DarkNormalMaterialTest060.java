package com.chaogarden.client;

import com.google.gson.JsonParser;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import javax.imageio.ImageIO;

/** Actual canonical coat pixels and .60-only semantic paint. No renderer or
 * native context is created, and these checks are not a visual inspection. */
public final class DarkNormalMaterialTest060 {
  private static final Path ROOT=Path.of("src/main/resources/assets/chao_garden");
  private static final float[][] ENDPOINTS={{0,0},{-1,0},{1,0},{0,-1},{0,1},{-1,-1},{-1,1},{1,-1},{1,1}};
  private static long checks,alpha,eyes,orb,swatches;
  private static int coats,maxStep;
  private static void check(boolean pass,String why){checks++;if(!pass)throw new AssertionError(why);}
  private static int rgba(BufferedImage image,int x,int y){int p=image.getRGB(x,y);return p&0xff00ff00|(p&255)<<16|p>>>16&255;}
  private static int[] layout()throws Exception {
    int[] flags=new int[8192];String[] masks={"baby","baby_accent","baby_wings"};int[] tags={1,2,4};
    for(int i=0;i<3;i++) {
      var image=ImageIO.read(ROOT.resolve("textures/entity/chao/gameplay/masks/"+masks[i]+".png").toFile());
      for(int y=0;y<64;y++)for(int x=0;x<128;x++)if(image.getRGB(x,y)>>>24!=0)flags[y*128+x]|=tags[i];
    }
    var asset=JsonParser.parseString(Files.readString(ROOT.resolve("models/entity/gameplay/chao_baby.json"))).getAsJsonObject();
    for(var element:asset.getAsJsonArray("bones")) {
      var bone=element.getAsJsonObject();String name=bone.get("name").getAsString();int tag=0;
      if(name.equals("skull__cube_2")||name.equals("skull__cube_3")||name.startsWith("expression_")||name.startsWith("iris_"))tag|=64;
      if(name.startsWith("arm_")||name.startsWith("foot_"))tag|=8;
      if(name.startsWith("tail__"))tag|=16;
      if(name.equals("emotion_dot__cube_0"))tag|=32;
      if(tag==0)continue;
      for(var mesh:bone.getAsJsonArray("meshes"))for(var face:mesh.getAsJsonObject().getAsJsonArray("faces")) {
        float x0=999,y0=999,x1=-999,y1=-999;
        for(var point:face.getAsJsonObject().getAsJsonArray("uv")) {
          var pair=point.getAsJsonArray();float x=pair.get(0).getAsFloat()*2,y=pair.get(1).getAsFloat()*2;
          x0=Math.min(x0,x);x1=Math.max(x1,x);y0=Math.min(y0,y);y1=Math.max(y1,y);
        }
        for(int y=Math.max(0,(int)Math.floor(y0));y<Math.min(64,(int)Math.ceil(y1));y++)
          for(int x=Math.max(0,(int)Math.floor(x0));x<Math.min(128,(int)Math.ceil(x1));x++)flags[y*128+x]|=tag;
      }
    }
    int markers=0;
    for(int y=0;y<64;y++)for(int x=0;x<128;x++) {
      boolean actual=(flags[y*128+x]&32)!=0,semantic=HeroRunMarkerPaint056.markerUv(x,y);
      check(actual==semantic,"marker mask differs from six original orb UV faces");
      if(semantic){markers++;check((flags[y*128+x]&65)==0,"marker overlaps head skin/face pixels");}
    }
    check(markers==96,"incomplete six-face orb mask");return flags;
  }
  private static int dominant(BufferedImage image,int[] flags,int tag) {
    var counts=new HashMap<Integer,Integer>();
    for(int y=0;y<64;y++)for(int x=0;x<128;x++) {
      int p=rgba(image,x,y);if(p>>>24!=0&&(flags[y*128+x]&tag)!=0)counts.merge(p&0xffffff,1,Integer::sum);
    }
    return counts.entrySet().stream().max(java.util.Map.Entry.comparingByValue()).map(java.util.Map.Entry::getKey).orElse(0xffffff)|0xff000000;
  }
  private static DarkNormalRegionPaint060.Input input(BufferedImage image,int[] flags,boolean natural,boolean two,float age,float rp,float sf) {
    return new DarkNormalRegionPaint060.Input(natural,two,age,rp,sf,rgba(image,12,0),rgba(image,48,0),dominant(image,flags,4),dominant(image,flags,32));
  }
  private static void atlas()throws Exception {
    int[] flags=layout();
    try(var files=Files.list(ROOT.resolve("textures/entity/chao/official/coats/baby"))) {
      for(Path path:files.filter(p->p.toString().endsWith(".png")).sorted().toList()) {
        String name=path.getFileName().toString();boolean natural=name.startsWith("normal_dark_0_"),two=name.contains("_two");
        var image=ImageIO.read(path.toFile());coats++;
        for(float age:new float[]{0,.5f,1})for(float[] endpoint:ENDPOINTS) {
          var in=input(image,flags,natural,two,age,endpoint[0],endpoint[1]);var c=DarkNormalRegionPaint060.colors(in);
          for(int y=0;y<64;y++)for(int x=0;x<128;x++) {
            int p=rgba(image,x,y),f=flags[y*128+x],result=DarkNormalRegionPaint060.pixel(in,c,x,y,p,f);
            if(DarkNormalRegionPaint060.reservedKind(x,y)!=0) {
              check(p>>>24==0,"new swatch overwrites an inherited pixel "+name);
              check(result>>>24==255,"new feature became invisible "+name);swatches++;continue;
            }
            check(result>>>24==p>>>24,"original source alpha changed "+name);alpha++;
            if((f&64)!=0){check(result==p,"face/iris/expression pixel changed "+name);eyes++;}
            if((f&32)!=0){check(result==(p&0xff000000|c.marker()&0xffffff),"orb inherited coat shade");orb++;}
            if((!natural||!two)&&(f&1)!=0&&(f&(2|4|16|32|64))==0
                &&!DarkNormalRegionPaint060.bellyUv(x,y))
              check(result==p,"named/mono/jewel base hue changed "+name+" at "+x+","+y);
          }
          // Top of the ORIGINAL fixed skull is still a body-pigment region.
          if(!natural||!two)check(DarkNormalRegionPaint060.pixel(in,c,12,0,rgba(image,12,0),flags[12])==rgba(image,12,0),"inherited skull pigment replaced");
        }
      }
    }
    check(coats==172,"canonical coat inventory changed: "+coats);
    check(swatches==172L*3*9*4,"not exactly four .60-only reserved texels");
  }
  private static void regions()throws Exception {
    int[] flags=layout();var image=ImageIO.read(ROOT.resolve("textures/entity/chao/official/coats/baby/normal_dark_0_two.png").toFile());
    var fresh=DarkNormalRegionPaint060.colors(input(image,flags,true,true,0,0,0));
    for(float[] e:ENDPOINTS)check(fresh.equals(DarkNormalRegionPaint060.colors(input(image,flags,true,true,0,e[0],e[1]))),"training prematurely recolored the newly evolved Dark/Normal form");
    check(red(fresh.body())>green(fresh.body())*2&&red(fresh.foot())>220&&green(fresh.foot())<40,"fresh Dark form lost maroon/red palette");
    check(blue(fresh.marker())>200&&red(fresh.marker())>100,"fresh Dark spikeball lost lavender pigment");
    var balanced=DarkNormalRegionPaint060.colors(input(image,flags,true,true,1,0,0));
    var run=DarkNormalRegionPaint060.colors(input(image,flags,true,true,1,-1,0));
    var power=DarkNormalRegionPaint060.colors(input(image,flags,true,true,1,1,0));
    var swim=DarkNormalRegionPaint060.colors(input(image,flags,true,true,1,0,-1));
    var fly=DarkNormalRegionPaint060.colors(input(image,flags,true,true,1,0,1));
    var runSwim=DarkNormalRegionPaint060.colors(input(image,flags,true,true,1,-1,-1));
    var runFly=DarkNormalRegionPaint060.colors(input(image,flags,true,true,1,-1,1));
    var powerSwim=DarkNormalRegionPaint060.colors(input(image,flags,true,true,1,1,-1));
    var powerFly=DarkNormalRegionPaint060.colors(input(image,flags,true,true,1,1,1));
    check(blue(balanced.body())>green(balanced.body())&&red(balanced.body())>green(balanced.body()),"balanced body lost plum tint");
    check(red(run.marker())>220&&green(run.marker())>210&&blue(run.marker())<60,"Run spikeball no longer yellow");
    check(red(power.body())>blue(power.body())*2&&red(power.foot())>220&&green(power.foot())<140,"Power body/feet lost brown/orange palette");
    check(Math.abs(red(fly.body())-green(fly.body()))<12&&blue(fly.body())>red(fly.body()),"Fly body lost slate tint");
    check(red(fly.foot())>210&&blue(fly.foot())>230&&green(fly.foot())>120,"Fly extremities lost pale pink/lavender hue");
    check(red(swim.body())>blue(swim.body())&&red(swim.foot())>green(swim.foot())&&green(swim.foot())>blue(swim.foot()),"Swim body/extremities lost brown/peach hue");
    check(blue(runSwim.body())>green(runSwim.body())&&blue(runSwim.foot())>green(runSwim.foot()),"Run+Swim incorrectly reused pure Swim brown/orange");
    check(red(runFly.marker())>230&&green(runFly.marker())>150&&blue(runFly.marker())<150,"Run+Fly independent peach marker missing");
    check(blue(powerSwim.wing())>120&&red(powerSwim.wing())>180,"Power+Swim pink wings missing");
    check(red(powerFly.foot())>200&&green(powerFly.foot())>120&&blue(powerFly.foot())>120,"Power+Fly salmon extremities missing");
    var pigments=new java.util.HashSet<Integer>();
    for(float[] e:ENDPOINTS) {
      var natural=DarkNormalRegionPaint060.colors(input(image,flags,true,true,1,e[0],e[1]));pigments.add(natural.marker());
      for(boolean genetic:new boolean[]{true,false})for(boolean two:new boolean[]{true,false}) {
        var c=DarkNormalRegionPaint060.colors(input(image,flags,genetic,two,1,e[0],e[1]));
        check(c.marker()==natural.marker(),"spikeball changed with coat/finish policy");
      }
      var in=input(image,flags,true,true,1,e[0],e[1]);
      check(DarkNormalRegionPaint060.colors(in).marker()==DarkNormalRegionPaint060.marker(1,e[0],e[1]),"marker accessor disagrees with actual paint");
    }
    check(pigments.size()==9,"nine independently viewed marker pigments collapsed");
    // Age is an independent blend: raising/reducing training never rewinds
    // biological maturity, and the middle age retains both endpoint pigments.
    for(float[] e:ENDPOINTS) {
      var mid=DarkNormalRegionPaint060.colors(input(image,flags,true,true,.5f,e[0],e[1]));
      var end=DarkNormalRegionPaint060.colors(input(image,flags,true,true,1,e[0],e[1]));
      for(int channel=0;channel<3;channel++) {
        int expected=Math.round(((fresh.marker()>>>(channel*8)&255)+(end.marker()>>>(channel*8)&255))*.5f);
        check(Math.abs((mid.marker()>>>(channel*8)&255)-expected)<=1,"mid-age spikeball is not gradual");
      }
    }
    for(int scale:new int[]{1,2,4})for(int y=0;y<64*scale;y++)for(int x=0;x<128*scale;x++) {
      boolean semantic=HeroRunMarkerPaint056.markerUv(x/scale,y/scale);
      check(DarkNormalRegionPaint060.paletteProtected("dynamic/dark_normal060/test","baby",x,y,128*scale,64*scale)==semantic,"palette spikeball guard differs from six-face marker UVs");
      check(!DarkNormalRegionPaint060.paletteProtected("dynamic/neutral_swim059/test","baby",x,y,128*scale,64*scale),"old Swim material routed through .60");
      check(!DarkNormalRegionPaint060.paletteProtected("dynamic/dark_normal060/test","overlay",x,y,128*scale,64*scale),"animal overlay masked as marker");
    }
    check(DarkNormalRegionPaint060.reservedKind(125,63)==3&&DarkNormalRegionPaint060.reservedKind(124,63)==1,"new crest-root/underside swatches missing");
    check(HeroRunRegionPaint055.reservedKind(125,63)==0&&HeroRunRegionPaint055.reservedKind(124,63)==0,"old family swatches changed");
    check(ChaoPaletteMaterial.prototypeReservedKind("dynamic/dark_normal060/test","baby",125,63,128,64)==3,"shared palette missed new root swatch");
    check(ChaoPaletteMaterial.prototypeReservedKind("dynamic/neutral056/test","baby",125,63,128,64)==0,"shared palette changed old Neutral swatch");
    check(ChaoPaletteMaterial.prototypeReservedKind("dynamic/dark_normal060/test","overlay",125,63,128,64)==0,"shared palette changed animal overlay");
    check(ChaoPaletteMaterial.protectedMarker("dynamic/dark_normal060/test","baby",true,118,2,128,64),"shared palette failed to protect new spikeball");
    check(!ChaoPaletteMaterial.protectedMarker("dynamic/dark_normal060/test","overlay",true,118,2,128,64),"shared palette changed animal overlay marker");
    check(DarkNormalRegionPaint060.bellyUv(10,35)&&DarkNormalRegionPaint060.bellyUv(13,35)&&DarkNormalRegionPaint060.bellyUv(11,39)&&!DarkNormalRegionPaint060.bellyUv(11,35),"flat pixel crescent lost its U shape");
    var zero=DarkNormalRegionPaint060.colors(input(image,flags,true,true,0,0,0));
    check(zero.equals(DarkNormalRegionPaint060.colors(input(image,flags,true,true,Float.NaN,Float.NaN,Float.NaN))),"non-finite values leaked into material");
  }
  private static void continuous()throws Exception {
    int[] flags=layout();var image=ImageIO.read(ROOT.resolve("textures/entity/chao/official/coats/baby/normal_dark_0_two.png").toFile());
    for(int path=0;path<8;path++) {
      int[] previous=null;
      for(int i=0;i<=100;i++) {
        float t=i/100f;
        var in=input(image,flags,true,true,path==0?t:1,path==1?2*t-1:path==4?1-t:path==5?t:path==6?-t:-1,path==2?2*t-1:path==3?t:path==4?-1:path==5?t:path==6?-t:path==7?1-t:0);
        var c=DarkNormalRegionPaint060.colors(in);int[] now=new int[8192];
        for(int y=0;y<64;y++)for(int x=0;x<128;x++) {
          int at=y*128+x;now[at]=DarkNormalRegionPaint060.pixel(in,c,x,y,rgba(image,x,y),flags[at]);
          if(previous!=null)for(int channel=0;channel<3;channel++) {
            int step=Math.abs((now[at]>>>(channel*8)&255)-(previous[at]>>>(channel*8)&255));
            maxStep=Math.max(maxStep,step);check(step<=8,"1% material path jumped "+step+" at "+x+","+y);
          }
        }
        previous=now;
      }
    }
  }
  private static int red(int p){return p&255;}
  private static int green(int p){return p>>>8&255;}
  private static int blue(int p){return p>>>16&255;}
  public static void main(String[] args)throws Exception {
    atlas();regions();continuous();
    String report="DarkNormal060 material: "+checks+" checks; "+coats+" real canonical coats; "+alpha+" preserved alpha pixels; "+eyes+" exact facial pixels; "+orb+" independent orb samples; "+swatches+" four-swatch samples; maximum 1% RGB step="+maxStep+". Nine mature endpoints with identical fresh palette, intermediate/receding training, inherited hue/alpha, semantic crest/crescent/wing regions, .60-only palette protection passed. Pixel checks are not in-game visual verification.\n";
    Files.createDirectories(Path.of("build/reports/lionfish"));Files.writeString(Path.of("build/reports/lionfish/dark-normal060-material-results.txt"),report);System.out.print(report);
  }
}
