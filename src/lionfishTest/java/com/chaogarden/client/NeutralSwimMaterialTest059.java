package com.chaogarden.client;

import com.google.gson.JsonParser;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import javax.imageio.ImageIO;

/** Actual canonical coat pixels and .59-only semantic paint. No renderer or
 * native context is created, and these checks are not a visual inspection. */
public final class NeutralSwimMaterialTest059 {
  private static final Path ROOT=Path.of("src/main/resources/assets/chao_garden");
  private static final float[][] ENDPOINTS={{0,0},{-1,0},{1,0},{0,-1},{-1,-1},{1,-1},{0,1},{-1,1},{1,1}};
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
  private static NeutralSwimRegionPaint059.Input input(BufferedImage image,int[] flags,boolean natural,boolean two,float age,float rp,float sf) {
    return new NeutralSwimRegionPaint059.Input(natural,two,age,rp,sf,rgba(image,12,0),rgba(image,48,0),dominant(image,flags,4),dominant(image,flags,32));
  }
  private static void atlas()throws Exception {
    int[] flags=layout();
    try(var files=Files.list(ROOT.resolve("textures/entity/chao/official/coats/baby"))) {
      for(Path path:files.filter(p->p.toString().endsWith(".png")).sorted().toList()) {
        String name=path.getFileName().toString();boolean natural=name.startsWith("normal_neutral_0_"),two=name.contains("_two");
        var image=ImageIO.read(path.toFile());coats++;
        for(float age:new float[]{0,.5f,1})for(float[] endpoint:ENDPOINTS) {
          var in=input(image,flags,natural,two,age,endpoint[0],endpoint[1]);var c=NeutralSwimRegionPaint059.colors(in);
          for(int y=0;y<64;y++)for(int x=0;x<128;x++) {
            int p=rgba(image,x,y),f=flags[y*128+x],result=NeutralSwimRegionPaint059.pixel(in,c,x,y,p,f);
            if(NeutralSwimRegionPaint059.reservedKind(x,y)!=0) {
              check(p>>>24==0,"new swatch overwrites an inherited pixel "+name);
              check(result>>>24==255,"new feature became invisible "+name);swatches++;continue;
            }
            check(result>>>24==p>>>24,"original source alpha changed "+name);alpha++;
            if((f&64)!=0){check(result==p,"face/iris/expression pixel changed "+name);eyes++;}
            if((f&32)!=0){check(result==(p&0xff000000|c.marker()&0xffffff),"orb inherited coat shade");orb++;}
            if((!natural||!two)&&(f&1)!=0&&(f&(2|4|16|32|64))==0
                &&!NeutralSwimRegionPaint059.lowerHeadUv(x,y)&&!NeutralSwimRegionPaint059.foreheadUv(x,y)&&!NeutralSwimRegionPaint059.bellyUv(x,y))
              check(result==p,"named/mono/jewel base hue changed "+name+" at "+x+","+y);
          }
          // Top of the ORIGINAL fixed skull is still a body-pigment region.
          if(!natural||!two)check(NeutralSwimRegionPaint059.pixel(in,c,12,0,rgba(image,12,0),flags[12])==rgba(image,12,0),"inherited skull pigment replaced");
        }
      }
    }
    check(coats==172,"canonical coat inventory changed: "+coats);
    check(swatches==172L*3*ENDPOINTS.length*4,"not exactly four .59-only reserved texels");
  }
  private static void regions()throws Exception {
    int[] flags=layout();var image=ImageIO.read(ROOT.resolve("textures/entity/chao/official/coats/baby/normal_neutral_0_two.png").toFile());
    var fresh=NeutralSwimRegionPaint059.colors(input(image,flags,true,true,0,0,0));
    for(float[] e:ENDPOINTS)check(fresh.equals(NeutralSwimRegionPaint059.colors(input(image,flags,true,true,0,e[0],e[1]))),"training prematurely recolored the newly evolved Swim first form");
    var balanced=NeutralSwimRegionPaint059.colors(input(image,flags,true,true,1,0,0));
    var run=NeutralSwimRegionPaint059.colors(input(image,flags,true,true,1,-1,0));
    var power=NeutralSwimRegionPaint059.colors(input(image,flags,true,true,1,1,0));
    var swim=NeutralSwimRegionPaint059.colors(input(image,flags,true,true,1,0,-1));
    check(green(balanced.body())>red(balanced.body())&&green(balanced.body())>blue(balanced.body()),"Swim first form lost green upper head/body");
    check(red(balanced.face())>180&&green(balanced.face())>180&&blue(balanced.face())<190,"balanced lower face no longer cream");
    check(green(run.face())>180&&blue(run.face())>180&&red(run.face())<80,"Run secondary lower face no longer cyan");
    check(red(power.foot())>240&&green(power.foot())<170&&blue(power.foot())<70,"Power secondary feet no longer orange");
    check(green(swim.face())>225&&blue(swim.face())>220,"additional Swim lower face no longer pale cyan");
    check(run.marker()!=power.marker()&&swim.marker()!=balanced.marker(),"specializations lost independent orb pigments");
    var fly=NeutralSwimRegionPaint059.colors(input(image,flags,true,true,1,0,1));
    var runFly=NeutralSwimRegionPaint059.colors(input(image,flags,true,true,1,-1,1));
    var powerFly=NeutralSwimRegionPaint059.colors(input(image,flags,true,true,1,1,1));
    var opposing=NeutralSwimRegionPaint059.colors(input(image,flags,true,true,1,.35f,.9f));
    var center=NeutralSwimRegionPaint059.colors(input(image,flags,true,true,1,.35f,0));
    check(!opposing.equals(center),"opposite Fly training still collapsed to balanced paint");
    check(red(fly.face())>210&&blue(fly.face())>170&&green(fly.face())<210,"opposite Fly underside lost pink");
    check(green(runFly.body())>220&&blue(runFly.face())>200&&red(runFly.face())<190,"Run+Fly lost green/cyan colors");
    check(red(powerFly.face())>220&&green(powerFly.face())>190&&blue(powerFly.face())<170,"Power+Fly underside lost warm gold");
    for(var c:new NeutralSwimRegionPaint059.Colors[]{fly,runFly,powerFly})
      check(blue(c.wing())>170&&red(c.wing())>110&&green(c.wing())<180,"opposite mature wings lost lavender/purple");
    for(float rp:new float[]{-1,0,1}) {
      var half=NeutralSwimRegionPaint059.colors(input(image,flags,true,true,.5f,rp,1));
      var end=NeutralSwimRegionPaint059.colors(input(image,flags,true,true,1,rp,1));
      check(green(half.body())>225&&blue(half.body())<60,"opposite half-age upper body matured before its lime stage");
      check(blue(half.wing())>170&&red(half.wing())<100,"opposite half-age wings lost their blue stage");
      check(red(half.wing())<red(end.wing()),"opposite wing stage keys no longer develop toward lavender");
      check(!half.equals(end),"opposite form prematurely received its mature palette");
    }
    var unique=new java.util.HashSet<NeutralSwimRegionPaint059.Colors>();
    for(float[] e:ENDPOINTS)unique.add(NeutralSwimRegionPaint059.colors(input(image,flags,true,true,1,e[0],e[1])));
    check(unique.size()==9,"mature Swim-first paint still contains a duplicate guard row");
    for(boolean natural:new boolean[]{true,false})for(boolean two:new boolean[]{true,false})for(float[] e:ENDPOINTS) {
      var in=input(image,flags,natural,two,1,e[0],e[1]);var c=NeutralSwimRegionPaint059.colors(in);
      check(c.marker()==NeutralSwimRegionPaint059.colors(input(image,flags,true,true,1,e[0],e[1])).marker(),"orb changed with inherited coat policy");
    }
    for(int scale:new int[]{1,2,4})for(int y=0;y<64*scale;y++)for(int x=0;x<128*scale;x++) {
      boolean semantic=HeroRunMarkerPaint056.markerUv(x/scale,y/scale);
      check(NeutralSwimRegionPaint059.paletteProtected("dynamic/neutral_swim059/test","baby",x,y,128*scale,64*scale)==semantic,"palette orb guard does not match six-face UVs");
      check(!NeutralSwimRegionPaint059.paletteProtected("dynamic/neutral056/test","baby",x,y,128*scale,64*scale),"old Neutral material accidentally routed through .59");
      check(!NeutralSwimRegionPaint059.paletteProtected("dynamic/neutral_swim059/test","overlay",x,y,128*scale,64*scale),"overlay accidentally masked as orb");
    }
    check(NeutralSwimRegionPaint059.reservedKind(125,63)==3&&NeutralSwimRegionPaint059.reservedKind(124,63)==1,"new fin/underside swatches missing");
    check(HeroRunRegionPaint055.reservedKind(125,63)==0&&HeroRunRegionPaint055.reservedKind(124,63)==0,"old family swatch interpretation changed");
    check(ChaoPaletteMaterial.prototypeReservedKind("dynamic/neutral_swim059/test","baby",125,63,128,64)==3,"shared palette did not route new fin swatch");
    check(ChaoPaletteMaterial.prototypeReservedKind("dynamic/neutral056/test","baby",125,63,128,64)==0,"shared palette changed old Neutral extra swatch");
    check(ChaoPaletteMaterial.prototypeReservedKind("dynamic/neutral_swim059/test","overlay",125,63,128,64)==0,"shared palette changed animal overlay swatches");
    check(ChaoPaletteMaterial.protectedMarker("dynamic/neutral_swim059/test","baby",true,118,2,128,64),"shared palette failed to protect new orb");
    check(!ChaoPaletteMaterial.protectedMarker("dynamic/neutral_swim059/test","overlay",true,118,2,128,64),"shared palette changed animal overlay marker");
    check(!NeutralSwimRegionPaint059.lowerHeadUv(12,0)&&NeutralSwimRegionPaint059.lowerHeadUv(20,22),"fixed head color boundary moved to wrong face");
  }
  private static void continuous()throws Exception {
    int[] flags=layout();var image=ImageIO.read(ROOT.resolve("textures/entity/chao/official/coats/baby/normal_neutral_0_two.png").toFile());
    for(int path=0;path<16;path++) {
      int[] previous=null;
      for(int i=0;i<=100;i++) {
        float t=i/100f;
        // Preserve all five old paths; append actual opposite crossings,
        // mixed corners, reversed paths and the new half-age color keys.
        float age=path==0||path>=11&&path<=13?t:1;
        float rp=path<5?(path==1?2*t-1:path==4?1-t:-1):switch(path){
          case 7->2*t-1;case 8->-t;case 9->t;case 10->1-t;case 12->-1;case 13->1;case 15->t-1;default->0;};
        float sf=path<5?(path==2?-t:path==3?t-1:0):switch(path){
          case 5->2*t-1;case 6->1-2*t;case 8,9,14->t;case 10,15->1-t;default->1;};
        var in=input(image,flags,true,true,age,rp,sf);
        var c=NeutralSwimRegionPaint059.colors(in);int[] now=new int[8192];
        for(int y=0;y<64;y++)for(int x=0;x<128;x++) {
          int at=y*128+x;now[at]=NeutralSwimRegionPaint059.pixel(in,c,x,y,rgba(image,x,y),flags[at]);
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
    String report="NeutralSwim059 material: "+checks+" checks; "+coats+" real canonical coats; "+alpha+" preserved alpha pixels; "+eyes+" exact facial pixels; "+orb+" independent orb samples; "+swatches+" four-swatch samples; maximum 1% RGB step="+maxStep+". Nine mature endpoints with the unchanged fresh palette, original six retained, opposite crossings, half-age keys and intermediate/receding training, inherited hue/alpha, semantic head/fin regions, .59-only palette protection passed. Pixel checks are not in-game visual verification.\n";
    Files.createDirectories(Path.of("build/reports/lionfish"));Files.writeString(Path.of("build/reports/lionfish/neutral-swim059-material-results.txt"),report);System.out.print(report);
  }
}
