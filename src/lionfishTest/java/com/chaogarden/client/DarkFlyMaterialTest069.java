package com.chaogarden.client;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import javax.imageio.ImageIO;

/** Real canonical coats and actual authored face interiors. These checks
 * establish pixel/UV policy; they do not imply a native visual/shader pass. */
public final class DarkFlyMaterialTest069 {
  private static final Path ROOT=Path.of("src/main/resources/assets/chao_garden");
  private static final float[][] CORNERS={{0,0},{-1,0},{1,0},{0,-1},{0,1},{-1,-1},{1,-1},{-1,1},{1,1}};
  private static long checks,alpha,eyes,markers,swatches,interiors,skullPixels,paletteSamples;
  private static int coats,maxStep,oldModels,boxes;
  private record Surface(String name,String region,int pixel,boolean forward) {}
  private static final List<Surface> SURFACES=new ArrayList<>();
  private static final HashSet<Integer> SKULL=new HashSet<>();
  private static void check(boolean value,String why){checks++;if(!value)throw new AssertionError(why);}
  private static int rgba(BufferedImage image,int x,int y){int p=image.getRGB(x,y);return p&0xff00ff00|(p&255)<<16|p>>>16&255;}
  private static JsonObject read(Path path)throws Exception{return JsonParser.parseString(Files.readString(path)).getAsJsonObject();}
  private static JsonObject model(String name)throws Exception{return read(ROOT.resolve("models/entity/gameplay/"+name+".json"));}
  private static int[] layout()throws Exception {
    int[] flags=new int[8192];String[] masks={"baby","baby_accent","baby_wings"};
    for(int i=0;i<3;i++){
      var image=ImageIO.read(ROOT.resolve("textures/entity/chao/gameplay/masks/"+masks[i]+".png").toFile());
      for(int y=0;y<64;y++)for(int x=0;x<128;x++)if(image.getRGB(x,y)>>>24!=0)flags[y*128+x]|=1<<i;
    }
    for(var element:model("chao_baby").getAsJsonArray("bones")){
      var bone=element.getAsJsonObject();String name=bone.get("name").getAsString();int tag=0;
      if(name.equals("skull__cube_2")||name.equals("skull__cube_3")||name.startsWith("expression_")||name.startsWith("iris_"))tag|=64;
      if(name.startsWith("arm_")||name.startsWith("foot_"))tag|=8;
      if(name.startsWith("tail__"))tag|=16;
      if(name.equals("emotion_dot__cube_0"))tag|=32;
      for(var mesh:bone.getAsJsonArray("meshes"))for(var face:mesh.getAsJsonObject().getAsJsonArray("faces")){
        var uv=face.getAsJsonObject().getAsJsonArray("uv");float left=999,top=999,right=-999,bottom=-999;
        for(var point:uv){var q=point.getAsJsonArray();float x=q.get(0).getAsFloat()*2,y=q.get(1).getAsFloat()*2;left=Math.min(left,x);right=Math.max(right,x);top=Math.min(top,y);bottom=Math.max(bottom,y);}
        for(int y=Math.max(0,(int)Math.floor(top));y<Math.min(64,(int)Math.ceil(bottom));y++)for(int x=Math.max(0,(int)Math.floor(left));x<Math.min(128,(int)Math.ceil(right));x++){
          flags[y*128+x]|=tag;if(name.equals("skull__cube_0"))SKULL.add(y*128+x);
        }
      }
    }
    int count=0;
    for(int y=0;y<64;y++)for(int x=0;x<128;x++){
      boolean marker=(flags[y*128+x]&32)!=0;
      check(marker==HeroRunMarkerPaint056.markerUv(x,y),"Dark marker mask differs from original six faces");
      if(marker){count++;check((flags[y*128+x]&65)==0,"Dark marker overlaps original skin/eyes");}
    }
    check(count==96,"original Dark marker lost a face");return flags;
  }
  private static int dominant(BufferedImage image,int[] flags,int tag){
    var values=new HashMap<Integer,Integer>();
    for(int y=0;y<64;y++)for(int x=0;x<128;x++){int p=rgba(image,x,y);if(p>>>24!=0&&(flags[y*128+x]&tag)!=0)values.merge(p&0xffffff,1,Integer::sum);}
    return values.entrySet().stream().max(java.util.Map.Entry.comparingByValue()).map(java.util.Map.Entry::getKey).orElse(0xffffff)|0xff000000;
  }
  private static DarkFlyRegionPaint069.Input input(BufferedImage image,int[] flags,boolean natural,boolean two,float age,float rp,float sf){
    return new DarkFlyRegionPaint069.Input(natural,two,age,rp,sf,rgba(image,12,0),rgba(image,48,0),dominant(image,flags,4),dominant(image,flags,32));
  }
  private static int sample(JsonObject face,float a,float b){
    float[] w={(1-a)*(1-b),a*(1-b),a*b,(1-a)*b};float u=0,v=0;
    for(int i=0;i<4;i++){var q=face.getAsJsonArray("uv").get(i).getAsJsonArray();u+=q.get(0).getAsFloat()*w[i];v+=q.get(1).getAsFloat()*w[i];}
    int x=(int)Math.floor(u*2),y=(int)Math.floor(v*2);check(x>=0&&x<128&&y>=0&&y<64,"actual069 face left canonical atlas");return y*128+x;
  }
  private static void geometry()throws Exception {
    var names=new HashSet<String>();var regions=new HashSet<String>();
    for(var b:model("chao_dark_fly_proto069").getAsJsonArray("bones"))for(var m:b.getAsJsonObject().getAsJsonArray("meshes")){
      var mesh=m.getAsJsonObject();if(!mesh.has("dark_fly_box_069"))continue;
      boxes++;String name=mesh.get("name").getAsString(),region=mesh.get("dark_fly_region_069").getAsString();regions.add(region);
      check(names.add(b.getAsJsonObject().get("name").getAsString()+":"+name),"duplicate069 authored mesh");check(mesh.getAsJsonArray("faces").size()==6,"new069 part is not closed: "+name);
      float frontZ=Float.POSITIVE_INFINITY;
      for(var vertex:mesh.getAsJsonArray("vertices"))frontZ=Math.min(frontZ,vertex.getAsJsonArray().get(2).getAsFloat());
      for(var f:mesh.getAsJsonArray("faces")){
        boolean forward=region.equals("hand")||region.equals("foot");
        for(var index:f.getAsJsonObject().getAsJsonArray("v"))
          forward&=Math.abs(mesh.getAsJsonArray("vertices").get(index.getAsInt()).getAsJsonArray().get(2).getAsFloat()-frontZ)<.00001f;
        for(int y=0;y<8;y++)for(int x=0;x<8;x++){
        int at=sample(f.getAsJsonObject(),(x+.5f)/8,(y+.5f)/8);SURFACES.add(new Surface(name,region,at,forward));int u=at%128,v=at/128;
        if(region.equals("marker"))check(HeroRunMarkerPaint056.markerUv(u,v),"Dark spike sampled non-marker pigment");
        else if(!region.equals("body")&&!region.equals("hand")&&!region.equals("foot")&&!region.equals("tail_root")) {
          check(DarkFlyRegionPaint069.reservedKind(u,v)!=0,"new closed box sampled unreserved atlas: "+name);
          if(region.startsWith("wing_"))check(u==100||u==101,"wing lost its own reserved pigment");
          else if(region.equals("horn_root"))check(u==105,"horn root lost inherited/base pigment");
          else if(region.equals("horn_cap")||region.equals("tail_tip"))check(u==103||u==104,"colored cap/tail sampled unrelated pigment");
          else check(u>=102&&u<=105,"feature root sampled wing pigment");
        }
        }
      }
    }
    check(SURFACES.stream().filter(Surface::forward).count()==4*64,"actual forward palm/toe faces incomplete");
    check(boxes==23,"unexpected069 closed part inventory: "+boxes);
    check(regions.containsAll(List.of("horn_root","horn_cap","tail_root","tail_tip")),"069 anatomical regions incomplete");
    try(var stream=Files.list(ROOT.resolve("models/entity/gameplay"))){
      for(var path:stream.filter(p->p.toString().endsWith(".json")&&!p.getFileName().toString().equals("chao_dark_fly_proto069.json")).sorted().toList()){
        oldModels++;
        for(var b:read(path).getAsJsonArray("bones"))for(var m:b.getAsJsonObject().getAsJsonArray("meshes"))for(var f:m.getAsJsonObject().getAsJsonArray("faces")){
          float left=999,top=999,right=-999,bottom=-999;
          for(var point:f.getAsJsonObject().getAsJsonArray("uv")){var q=point.getAsJsonArray();float x=q.get(0).getAsFloat()*2,y=q.get(1).getAsFloat()*2;left=Math.min(left,x);right=Math.max(right,x);top=Math.min(top,y);bottom=Math.max(bottom,y);}
          int l=(int)Math.floor(left),r=Math.max(l+1,(int)Math.ceil(right)),t=(int)Math.floor(top),d=Math.max(t+1,(int)Math.ceil(bottom));
          for(int x=100;x<=105;x++)check(x<l||x>=r||63<t||63>=d,"069 reserved texel collides with old model: "+path);
        }
      }
    }
    check(oldModels==43,"old model audit incomplete: "+oldModels);
  }
  private static void shading(int source,int ref,int target,int actual,String why){
    check(actual>>>24==source>>>24,why+" changed alpha");
    for(int k=0;k<3;k++){
      int original=source>>>(k*8)&255,base=ref>>>(k*8)&255,to=target>>>(k*8)&255,got=actual>>>(k*8)&255;
      if(base==0)continue;
      long wanted=(long)original*to;
      check(wanted>=255L*base?got==255:Math.abs((long)got*base-wanted)<=base*.5+.001,why+" lost original shading");
    }
  }
  private static void atlas()throws Exception {
    int[] flags=layout();
    try(var stream=Files.list(ROOT.resolve("textures/entity/chao/official/coats/baby"))){
      for(var path:stream.filter(p->p.toString().endsWith(".png")).sorted().toList()){
        coats++;String name=path.getFileName().toString();var image=ImageIO.read(path.toFile());check(image.getWidth()==128&&image.getHeight()==64,"canonical coat dimensions changed");
        boolean natural=name.startsWith("normal_dark_0_"),two=name.contains("_two");
        for(float age:new float[]{0,.5f,1})for(float[] corner:CORNERS){
          var in=input(image,flags,natural,two,age,corner[0],corner[1]);var colors=DarkFlyRegionPaint069.colors(in);int[] painted=new int[8192];
          if(!natural||!two)check(colors.body()==in.body()&&colors.featureBody()==in.body(),"inherited body pigment replaced: "+name);
          for(int y=0;y<64;y++)for(int x=0;x<128;x++){
            int at=y*128+x,p=rgba(image,x,y),result=DarkFlyRegionPaint069.pixel(in,colors,x,y,p,flags[at]);painted[at]=result;
            if(DarkFlyRegionPaint069.reservedKind(x,y)!=0){check(p>>>24==0,"069 overwrote source art: "+name);check(result>>>24==255,"069 reserved pixel transparent");swatches++;continue;}
            check(result>>>24==p>>>24,"source alpha changed: "+name);alpha++;
            if((flags[at]&64)!=0){check(result==p,"original face/iris/expression changed: "+name);eyes++;}
            if((flags[at]&32)!=0){check(result==(p&0xff000000|DarkFlyRegionPaint069.marker(age,corner[0],corner[1])&0xffffff),"Dark marker inherited coat/finish pigment");markers++;}
          }
          for(int at:SKULL){int f=flags[at];if((f&64)!=0)continue;int x=at%128,y=at/128;
            shading(rgba(image,x,y),(f&2)!=0?in.accent():in.body(),DarkFlyRegionPaint069.muzzleUv(x,y)?colors.chest():colors.body(),painted[at],"retained skull/muzzle");skullPixels++;
          }
          for(var surface:SURFACES){int at=surface.pixel,p=painted[at];check(p>>>24==255,"actual closed069 interior transparent: "+surface.name);interiors++;
            if(DarkFlyRegionPaint069.reservedKind(at%128,at/128)!=0)check(p==DarkFlyRegionPaint069.reservedColor(colors,at%128),"actual069 surface sampled wrong semantic color");
            if(surface.forward){
              int v=at/128;boolean cuff=surface.region.equals("hand")?v==33:v==29;
              shading(rgba(image,at%128,v),(flags[at]&2)!=0?in.accent():in.body(),cuff?colors.limb():colors.foot(),p,"actual forward palm/toe pale plane and cuff");
            }
          }
          for(int at:new int[]{35*128+11,37*128+10,39*128+13})shading(rgba(image,at%128,at/128),(flags[at]&2)!=0?in.accent():in.body(),colors.chest(),painted[at],"actual front chest");
          for(int at:new int[]{36*128+27,39*128+28,41*128+11})shading(rgba(image,at%128,at/128),(flags[at]&2)!=0?in.accent():in.body(),colors.body(),painted[at],"dark back/lower torso");
        }
      }
    }
    check(coats==172,"real canonical coat audit incomplete: "+coats);
  }
  private static DarkFlyRegionPaint069.Input natural(float age,float rp,float sf){return new DarkFlyRegionPaint069.Input(true,true,age,rp,sf,0xff3b362f,0xff655fe4,0xffb034b7,0xff000000);}
  private static int red(int c){return c&255;}private static int green(int c){return c>>>8&255;}private static int blue(int c){return c>>>16&255;}
  private static void referenceAndScope(){
    var young=DarkFlyRegionPaint069.colors(natural(0,0,0));var markers=new HashSet<Integer>();var palettes=new HashSet<DarkFlyRegionPaint069.Colors>();
    for(float[] corner:CORNERS){
      check(DarkFlyRegionPaint069.colors(natural(0,corner[0],corner[1])).equals(young),"axes replaced common fresh Fly-first pigment");
      var colors=DarkFlyRegionPaint069.colors(natural(1,corner[0],corner[1]));markers.add(colors.marker());palettes.add(colors);
    }
    check(markers.size()==9&&palettes.size()==9,"mature corner distinctions collapsed");
    check(red(young.body())>green(young.body())+25&&blue(young.body())>green(young.body())+35,"fresh plum DarkFly body missing");
    check(red(young.chest())<red(young.body())&&green(young.chest())<green(young.body())&&blue(young.chest())<blue(young.body()),"fresh dark muzzle/chest missing");
    check(blue(young.marker())>220&&green(young.marker())>180&&red(young.marker())<20,"fresh independent cyan Dark marker missing");
    check(green(young.wingTip())>200&&red(young.wingTip())<60,"fresh green/cyan bat-wing pigment missing");
    var balanced=DarkFlyRegionPaint069.colors(natural(1,0,0));var swim=DarkFlyRegionPaint069.colors(natural(1,0,-1));
    var run=DarkFlyRegionPaint069.colors(natural(1,-1,0));var power=DarkFlyRegionPaint069.colors(natural(1,1,0));var fly=DarkFlyRegionPaint069.colors(natural(1,0,1));
    check(blue(balanced.marker())>green(balanced.marker())+80&&red(balanced.marker())<30,"balanced blue marker missing");
    check(red(balanced.featureMid())>green(balanced.featureMid())+150&&blue(balanced.featureMid())>100,"balanced magenta horn caps missing");
    check(green(swim.featureTip())>210&&blue(swim.featureTip())>210&&red(swim.featureTip())<150,"later Swim cyan tip/hand pigment missing");
    check(green(swim.marker())>220&&blue(swim.marker())<160,"later Swim lime marker missing");
    check(blue(run.body())>red(run.body())+25&&red(run.marker())>220&&green(run.marker())>120,"Run indigo/peach palette missing");
    check(red(run.wing())>green(run.wing())&&green(run.wing())>blue(run.wing()),"Run orange bat-wing pigment missing");
    check(red(power.body())>blue(power.body())+25&&red(power.foot())>blue(power.foot())+60,"Power warm body/tan hand-toe fronts missing");
    check(red(power.marker())>240&&green(power.marker())>200&&blue(power.marker())<20,"Power yellow marker missing");
    check(red(fly.marker())>220&&blue(fly.marker())>220&&green(fly.marker())<30,"Fly magenta marker missing");
    check(red(fly.wing())>170&&blue(fly.wing())>100&&green(fly.wing())<30,"mature Fly pink/red wings missing");
    var halfFly=DarkFlyRegionPaint069.colors(natural(.5f,0,1));
    check(blue(halfFly.marker())>200&&red(halfFly.marker())>80&&green(halfFly.marker())>70,"half-age Fly blue-violet marker missing");
    check(blue(halfFly.wingTip())>200&&blue(halfFly.wingTip())>red(halfFly.wingTip())+60,"half-age Fly wings skipped observed lavender stage");
    check(red(halfFly.wing())>blue(halfFly.wing())+30,"half-age Fly wings lost observed olive/brown roots");
    check(!DarkFlyRegionPaint069.muzzleUv(46,19)&&DarkFlyRegionPaint069.muzzleUv(18,19),"dark muzzle leaked to rear or missed front");
    check(!DarkFlyRegionPaint069.chestUv(28,37)&&DarkFlyRegionPaint069.chestUv(12,37),"chest leaked to back");
    for(int scale:new int[]{1,2,4})for(int y=0;y<64*scale;y++)for(int x=0;x<128*scale;x++){
      boolean marker=HeroRunMarkerPaint056.markerUv(x/scale,y/scale),wing=y/scale==63&&(x/scale==100||x/scale==101);
      check(DarkFlyRegionPaint069.paletteProtected("dynamic/dark_fly069/x","baby",x,y,128*scale,64*scale)==marker,"069 palette marker mask incomplete");
      check(DarkFlyRegionPaint069.paletteWing("dynamic/dark_fly069/x","baby",x,y,128*scale,64*scale)==wing,"069 cosmetic wing mask incomplete");paletteSamples++;
      for(String path:new String[]{"dynamic/dark_swim068/x","dynamic/hero_power067/x","dynamic/hero_run066/x","dynamic/dark_normal060/x","textures/entity/chao/baby.png"})check(!DarkFlyRegionPaint069.paletteProtected(path,"baby",x,y,128*scale,64*scale)&&!DarkFlyRegionPaint069.paletteWing(path,"baby",x,y,128*scale,64*scale),"069 masks changed older route");
      check(!DarkFlyRegionPaint069.paletteProtected("dynamic/dark_fly069/x","overlay",x,y,128*scale,64*scale)&&!DarkFlyRegionPaint069.paletteWing("dynamic/dark_fly069/x","overlay",x,y,128*scale,64*scale),"overlay acquired069 masks");
    }
    int count=0;for(int y=0;y<64;y++)for(int x=0;x<128;x++){boolean expected=y==63&&x>=100&&x<=105;check((DarkFlyRegionPaint069.reservedKind(x,y)!=0)==expected,"069 alpha exception escaped exact six-pixel footprint");if(expected)count++;}
    check(count==6,"069 reserved texel count changed");
    check(!DarkFlyRegionPaint069.paletteProtected("dynamic/dark_fly069/x","baby",0,0,0,0)&&!DarkFlyRegionPaint069.paletteWing("dynamic/dark_fly069/x","baby",0,0,0,0),"zero dimensions accepted");
  }
  private static void continuous()throws Exception {
    int[] flags=layout();var image=ImageIO.read(ROOT.resolve("textures/entity/chao/official/coats/baby/normal_dark_0_two.png").toFile());
    for(int path=0;path<8;path++){
      int[] previous=null;
      for(int i=0;i<=100;i++){
        float t=i/100f,age=path==0?t:path==7?1-t:1,rp=path==1?2*t-1:path==3?1-t:path==5?2*t-1:-1,sf=path==2?2*t-1:path==4?1-t:path==5?1-2*t:0;
        var in=input(image,flags,true,true,age,rp,sf);var colors=DarkFlyRegionPaint069.colors(in);int[] now=new int[8192];
        for(int y=0;y<64;y++)for(int x=0;x<128;x++){
          int at=y*128+x;now[at]=DarkFlyRegionPaint069.pixel(in,colors,x,y,rgba(image,x,y),flags[at]);
          if(previous!=null)for(int k=0;k<3;k++){int step=Math.abs((now[at]>>>(k*8)&255)-(previous[at]>>>(k*8)&255));maxStep=Math.max(maxStep,step);check(step<=10,"one-percent material progression jumped: "+step);}
        }
        previous=now;
      }
    }
  }
  public static void main(String[] args)throws Exception {
    geometry();atlas();referenceAndScope();continuous();
    String report="DarkFly069 material: "+checks+" checks; "+coats+" real canonical coats; "+oldModels+" preceding model UV footprints; "+boxes+" closed parts; "+alpha+" preserved-alpha pixels; "+eyes+" exact facial pixels; "+markers+" independent Dark marker samples; "+swatches+" six-texel alpha exceptions; "+interiors+" actual closed-face interior samples; "+skullPixels+" retained skull/muzzle shading samples; "+paletteSamples+" scoped palette-mask samples; maximum one-percent RGB step="+maxStep+". Actual first-Fly/full-Dark young, observed half-age Fly wing pigments and all nine mature palettes, front-only chest, dark front muzzle, simple limb cuffs/tips, inherited mono/named/jewel pigments, exact original facial art and alpha, and opaque new geometry passed. Pixel checks are not native visual/shader/multiplayer verification.\n";
    Path out=Path.of(System.getProperty("chao.materialReport","build/reports/lionfish/dark-fly069-material-results.txt"));Files.createDirectories(out.toAbsolutePath().getParent());Files.writeString(out,report);System.out.print(report);
  }
}
