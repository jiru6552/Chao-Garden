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
public final class DarkRunMaterialTest070 {
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
  private static DarkRunRegionPaint070.Input input(BufferedImage image,int[] flags,boolean natural,boolean two,float age,float rp,float sf){
    return new DarkRunRegionPaint070.Input(natural,two,age,rp,sf,rgba(image,12,0),rgba(image,48,0),dominant(image,flags,4),dominant(image,flags,32));
  }
  private static int sample(JsonObject face,float a,float b){
    float[] w={(1-a)*(1-b),a*(1-b),a*b,(1-a)*b};float u=0,v=0;
    for(int i=0;i<4;i++){var q=face.getAsJsonArray("uv").get(i).getAsJsonArray();u+=q.get(0).getAsFloat()*w[i];v+=q.get(1).getAsFloat()*w[i];}
    int x=(int)Math.floor(u*2),y=(int)Math.floor(v*2);check(x>=0&&x<128&&y>=0&&y<64,"actual070 face left canonical atlas");return y*128+x;
  }
  private static void geometry()throws Exception {
    var names=new HashSet<String>();var regions=new HashSet<String>();
    for(var b:model("chao_dark_run_proto070").getAsJsonArray("bones"))for(var m:b.getAsJsonObject().getAsJsonArray("meshes")){
      var mesh=m.getAsJsonObject();if(!mesh.has("dark_run_box_070"))continue;
      boxes++;String name=mesh.get("name").getAsString(),region=mesh.get("dark_run_region_070").getAsString();regions.add(region);
      check(names.add(b.getAsJsonObject().get("name").getAsString()+":"+name),"duplicate070 authored mesh");check(mesh.getAsJsonArray("faces").size()==6,"new070 part is not closed: "+name);
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
          check(DarkRunRegionPaint070.reservedKind(u,v)!=0,"new closed box sampled unreserved atlas: "+name);
          if(region.startsWith("wing_"))check(u==74||u==75,"wing lost its own reserved pigment");
          else if(region.equals("tail_tip"))check(u==77||u==78,"tail tip lost its two panel pigments");
          else if(region.startsWith("crest_")||region.startsWith("rear_")||region.equals("front_nub"))
            check(u==77||u==78||u==79,"head feature lost base/panel pigment");
          else check(u>=76&&u<=79,"feature root sampled wing pigment");
        }
        }
      }
    }
    check(SURFACES.stream().filter(Surface::forward).count()==4*64,"actual forward palm/toe faces incomplete");
    check(boxes==27,"unexpected070 closed part inventory: "+boxes);
    check(regions.containsAll(List.of("crest_root","crest_tip","rear_root","rear_tip","front_nub","tail_root","tail_tip")),"070 anatomical regions incomplete");
    try(var stream=Files.list(ROOT.resolve("models/entity/gameplay"))){
      for(var path:stream.filter(p->p.toString().endsWith(".json")&&!p.getFileName().toString().equals("chao_dark_run_proto070.json")).sorted().toList()){
        oldModels++;
        for(var b:read(path).getAsJsonArray("bones"))for(var m:b.getAsJsonObject().getAsJsonArray("meshes"))for(var f:m.getAsJsonObject().getAsJsonArray("faces")){
          float left=999,top=999,right=-999,bottom=-999;
          for(var point:f.getAsJsonObject().getAsJsonArray("uv")){var q=point.getAsJsonArray();float x=q.get(0).getAsFloat()*2,y=q.get(1).getAsFloat()*2;left=Math.min(left,x);right=Math.max(right,x);top=Math.min(top,y);bottom=Math.max(bottom,y);}
          int l=(int)Math.floor(left),r=Math.max(l+1,(int)Math.ceil(right)),t=(int)Math.floor(top),d=Math.max(t+1,(int)Math.ceil(bottom));
          for(int x=74;x<=79;x++)check(x<l||x>=r||63<t||63>=d,"070 reserved texel collides with old model: "+path);
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
          var in=input(image,flags,natural,two,age,corner[0],corner[1]);var colors=DarkRunRegionPaint070.colors(in);int[] painted=new int[8192];
          if(!natural||!two)check(colors.body()==in.body()&&colors.featureBody()==in.body(),"inherited body pigment replaced: "+name);
          for(int y=0;y<64;y++)for(int x=0;x<128;x++){
            int at=y*128+x,p=rgba(image,x,y),result=DarkRunRegionPaint070.pixel(in,colors,x,y,p,flags[at]);painted[at]=result;
            if(DarkRunRegionPaint070.reservedKind(x,y)!=0){check(p>>>24==0,"070 overwrote source art: "+name);check(result>>>24==255,"070 reserved pixel transparent");swatches++;continue;}
            check(result>>>24==p>>>24,"source alpha changed: "+name);alpha++;
            if((flags[at]&64)!=0){check(result==p,"original face/iris/expression changed: "+name);eyes++;}
            if((flags[at]&32)!=0){check(result==(p&0xff000000|DarkRunRegionPaint070.marker(age,corner[0],corner[1])&0xffffff),"Dark marker inherited coat/finish pigment");markers++;}
          }
          for(int at:SKULL){int f=flags[at];if((f&64)!=0)continue;int x=at%128,y=at/128;
            shading(rgba(image,x,y),(f&2)!=0?in.accent():in.body(),DarkRunRegionPaint070.headColor(colors,x,y),painted[at],"retained fixed skull and crown/cheek panels");skullPixels++;
          }
          for(var surface:SURFACES){int at=surface.pixel,p=painted[at];check(p>>>24==255,"actual closed070 interior transparent: "+surface.name);interiors++;
            if(DarkRunRegionPaint070.reservedKind(at%128,at/128)!=0)check(p==DarkRunRegionPaint070.reservedColor(colors,at%128),"actual070 surface sampled wrong semantic color");
            if(surface.region.equals("hand")||surface.region.equals("foot")) {
              int u=at%128,v=at/128;
              shading(rgba(image,u,v),(flags[at]&2)!=0?in.accent():in.body(),DarkRunRegionPaint070.limbColor(colors,u,v),p,"actual closed palm/toe detail and base planes");
            }
          }
          for(int at:new int[]{34*128+11,35*128+10,36*128+12})shading(rgba(image,at%128,at/128),(flags[at]&2)!=0?in.accent():in.body(),DarkRunRegionPaint070.collarColor(colors,at%128,at/128),painted[at],"actual slim front neck trim");
          for(int at:new int[]{37*128+11,39*128+28,41*128+11})shading(rgba(image,at%128,at/128),(flags[at]&2)!=0?in.accent():in.body(),colors.body(),painted[at],"dark back/lower torso");
        }
      }
    }
    check(coats==172,"real canonical coat audit incomplete: "+coats);
  }
  private static DarkRunRegionPaint070.Input natural(float age,float rp,float sf){return new DarkRunRegionPaint070.Input(true,true,age,rp,sf,0xff3b362f,0xff655fe4,0xffb034b7,0xff000000);}
  private static int red(int c){return c&255;}private static int green(int c){return c>>>8&255;}private static int blue(int c){return c>>>16&255;}
  private static void referenceAndScope(){
    var young=DarkRunRegionPaint070.colors(natural(0,0,0));var markers=new HashSet<Integer>();var palettes=new HashSet<DarkRunRegionPaint070.Colors>();
    for(float[] corner:CORNERS){
      check(DarkRunRegionPaint070.colors(natural(0,corner[0],corner[1])).equals(young),"axes replaced common fresh Run-first pigment");
      var colors=DarkRunRegionPaint070.colors(natural(1,corner[0],corner[1]));markers.add(colors.marker());palettes.add(colors);
    }
    check(palettes.size()==9,"mature full-palette distinctions collapsed");
    check(markers.size()==5,"observed shared marker classes were replaced with invented corner hues");
    check(green(young.body())>red(young.body())+35&&blue(young.body())>red(young.body())+40,"fresh dark teal body missing");
    check(green(young.featureMid())>225&&red(young.featureMid())<15&&blue(young.featureMid())<40,"fresh green panels missing");
    check(green(young.featureTip())>230&&blue(young.featureTip())>160,"fresh cyan panel ends missing");
    check(blue(young.marker())>235&&green(young.marker())>130&&red(young.marker())<10,"fresh independent blue Dark marker missing");
    var balanced=DarkRunRegionPaint070.colors(natural(1,0,0));var swim=DarkRunRegionPaint070.colors(natural(1,0,-1));
    var run=DarkRunRegionPaint070.colors(natural(1,-1,0));var power=DarkRunRegionPaint070.colors(natural(1,1,0));var fly=DarkRunRegionPaint070.colors(natural(1,0,1));
    var runFly=DarkRunRegionPaint070.colors(natural(1,-1,1));var runSwim=DarkRunRegionPaint070.colors(natural(1,-1,-1));
    var powerFly=DarkRunRegionPaint070.colors(natural(1,1,1));var powerSwim=DarkRunRegionPaint070.colors(natural(1,1,-1));
    check(balanced.marker()==fly.marker()&&fly.marker()==swim.marker(),"observed common cyan marker classes diverged");
    check(green(balanced.marker())>220&&blue(balanced.marker())>210&&red(balanced.marker())<10,"balanced cyan marker missing");
    check(red(run.body())>green(run.body())+15&&red(run.featureMid())>245&&green(run.featureMid())<30&&blue(run.featureMid())<20,"mature Run nearblack maroon/red panels missing");
    check(red(run.wingTip())>235&&green(run.wingTip())<30&&blue(run.wingTip())<20,"Run red wings missing");
    check(blue(power.body())>red(power.body())+50&&Math.max(red(power.featureMid()),Math.max(green(power.featureMid()),blue(power.featureMid())))<35,"opposite Power indigo/black panels missing");
    check(blue(power.featureTip())>225&&red(power.featureTip())<35&&green(power.featureTip())<35,"Power vivid blue panel ends missing");
    check(red(power.wingTip())>220&&blue(power.wingTip())>225&&green(power.wingTip())<30,"Power magenta wings missing");
    check(red(power.marker())>230&&blue(power.marker())>230&&green(power.marker())>120,"Power pale pink marker missing");
    check(green(fly.body())>red(fly.body())+10&&red(fly.featureMid())>225&&green(fly.featureMid())>220&&blue(fly.featureMid())<40,"Fly olive/yellow panels missing");
    check(green(fly.wingTip())>210&&blue(fly.wingTip())>210&&red(fly.wingTip())<10,"Fly cyan inner wings missing");
    check(red(swim.featureMid())>200&&green(swim.featureMid())>100&&blue(swim.featureMid())<25,"Swim orange panels missing");
    check(blue(swim.featureTip())>225&&red(swim.featureTip())>185&&green(swim.featureTip())<115,"Swim lavender panel ends missing");
    check(red(runFly.featureMid())>230&&green(runFly.featureMid())>110&&blue(runFly.featureMid())<25,"RunFly orange panel missing");
    check(runFly.marker()==runSwim.marker()&&Math.abs(red(runFly.marker())-blue(runFly.marker()))<15,"mixed Run gray marker class missing");
    check(Math.abs(red(runFly.wingTip())-green(runFly.wingTip()))<10&&Math.abs(green(runFly.wingTip())-blue(runFly.wingTip()))<10,"RunFly gray wing interior missing");
    check(red(powerFly.featureMid())>120&&green(powerFly.featureMid())>120&&blue(powerFly.featureMid())<40,"PowerFly olive panel missing");
    check(powerFly.marker()==powerSwim.marker()&&blue(powerFly.marker())>225&&green(powerFly.marker())>185&&red(powerFly.marker())>125,"mixed Power pale blue marker class missing");
    var halfRun=DarkRunRegionPaint070.colors(natural(.5f,-1,0));var halfPower=DarkRunRegionPaint070.colors(natural(.5f,1,0));var halfFly=DarkRunRegionPaint070.colors(natural(.5f,0,1));
    check(red(halfRun.featureMid())>140&&green(halfRun.featureMid())>130&&blue(halfRun.featureMid())<25,"half Run skipped observed mustard panels");
    check(Math.abs(red(halfRun.featureTip())-green(halfRun.featureTip()))<10&&blue(halfRun.featureTip())>120,"half Run gray ends missing");
    check(red(halfRun.wing())>90&&blue(halfRun.wing())>115&&green(halfRun.wing())<35,"half Run skipped observed plum wings");
    check(red(halfRun.marker())>130&&blue(halfRun.marker())>130&&green(halfRun.marker())>75,"half Run muted purple marker missing");
    check(green(halfPower.featureMid())>115&&red(halfPower.featureMid())<45&&blue(halfPower.featureMid())<40,"half Power darkened panels too early");
    check(green(halfPower.featureTip())>130&&blue(halfPower.featureTip())>210,"half Power green/cyan stage missing");
    check(green(halfFly.featureMid())>240&&red(halfFly.featureMid())>150&&blue(halfFly.featureMid())<35,"half Fly lime panels missing");
    check(green(halfFly.featureTip())>240&&blue(halfFly.featureTip())>165,"half Fly mint ends missing");
    for(int y=12;y<24;y++)for(int x=12;x<26;x++)check(!DarkRunRegionPaint070.crownUv(x,y)&&!DarkRunRegionPaint070.cheekUv(x,y),"paint crossed original forehead/front face");
    check(DarkRunRegionPaint070.crownUv(19,5)&&!DarkRunRegionPaint070.crownUv(18,12),"top patch escaped original crown or missed center");
    check(DarkRunRegionPaint070.cheekUv(31,19)&&DarkRunRegionPaint070.cheekUv(6,19)&&!DarkRunRegionPaint070.cheekUv(18,19),"lateral cheek panels lost placement");
    check(DarkRunRegionPaint070.collarUv(11,35)&&!DarkRunRegionPaint070.collarUv(27,35)&&!DarkRunRegionPaint070.collarUv(12,39),"neck trim spread across chest/back");
    check(DarkRunRegionPaint070.handDropUv(25,31)&&!DarkRunRegionPaint070.handDropUv(29,31),"outer hand drop repainted front palm");
    check(DarkRunRegionPaint070.footStripeUv(54,28)&&!DarkRunRegionPaint070.footStripeUv(66,29)&&!DarkRunRegionPaint070.footStripeUv(54,27),"thin toe trim spread to heel/top");
    for(int scale:new int[]{1,2,4})for(int y=0;y<64*scale;y++)for(int x=0;x<128*scale;x++){
      boolean marker=HeroRunMarkerPaint056.markerUv(x/scale,y/scale),wing=y/scale==63&&(x/scale==74||x/scale==75);
      check(DarkRunRegionPaint070.paletteProtected("dynamic/dark_run070/x","baby",x,y,128*scale,64*scale)==marker,"070 palette marker mask incomplete");
      check(DarkRunRegionPaint070.paletteWing("dynamic/dark_run070/x","baby",x,y,128*scale,64*scale)==wing,"070 cosmetic wing mask incomplete");paletteSamples++;
      for(String path:new String[]{"dynamic/dark_fly069/x","dynamic/dark_swim068/x","dynamic/hero_power067/x","dynamic/hero_run066/x","dynamic/hero_fly065/x","dynamic/hero_swim064/x","dynamic/neutral_power063/x","dynamic/neutral_run062/x","dynamic/neutral_fly061/x","dynamic/dark_normal060/x","dynamic/neutral_swim059/x","dynamic/hero_normal057/x","dynamic/neutral056/x","dynamic/hero_run055/x","textures/entity/chao/baby.png"})check(!DarkRunRegionPaint070.paletteProtected(path,"baby",x,y,128*scale,64*scale)&&!DarkRunRegionPaint070.paletteWing(path,"baby",x,y,128*scale,64*scale),"070 masks changed older route");
      check(!DarkRunRegionPaint070.paletteProtected("dynamic/dark_run070/x","overlay",x,y,128*scale,64*scale)&&!DarkRunRegionPaint070.paletteWing("dynamic/dark_run070/x","overlay",x,y,128*scale,64*scale),"overlay acquired070 masks");
    }
    int count=0;for(int y=0;y<64;y++)for(int x=0;x<128;x++){boolean expected=y==63&&x>=74&&x<=79;check((DarkRunRegionPaint070.reservedKind(x,y)!=0)==expected,"070 alpha exception escaped exact six-pixel footprint");if(expected)count++;}
    check(count==6,"070 reserved texel count changed");
    check(!DarkRunRegionPaint070.paletteProtected("dynamic/dark_run070/x","baby",0,0,0,0)&&!DarkRunRegionPaint070.paletteWing("dynamic/dark_run070/x","baby",0,0,0,0),"zero dimensions accepted");
    check(DarkRunRegionPaint070.colors(natural(Float.NaN,Float.NaN,Float.NaN)).equals(young),"nonfinite display inputs escaped clamping");
  }

  private static void continuous()throws Exception {
    int[] flags=layout();var image=ImageIO.read(ROOT.resolve("textures/entity/chao/official/coats/baby/normal_dark_0_two.png").toFile());
    for(int path=0;path<8;path++){
      int[] previous=null;
      for(int i=0;i<=100;i++){
        float t=i/100f,age=path==0?t:path==7?1-t:1,rp=path==1?2*t-1:path==3?1-t:path==5?2*t-1:-1,sf=path==2?2*t-1:path==4?1-t:path==5?1-2*t:0;
        var in=input(image,flags,true,true,age,rp,sf);var colors=DarkRunRegionPaint070.colors(in);int[] now=new int[8192];
        for(int y=0;y<64;y++)for(int x=0;x<128;x++){
          int at=y*128+x;now[at]=DarkRunRegionPaint070.pixel(in,colors,x,y,rgba(image,x,y),flags[at]);
          if(previous!=null)for(int k=0;k<3;k++){int step=Math.abs((now[at]>>>(k*8)&255)-(previous[at]>>>(k*8)&255));maxStep=Math.max(maxStep,step);check(step<=10,"one-percent material progression jumped: "+step);}
        }
        previous=now;
      }
    }
  }
  public static void main(String[] args)throws Exception {
    geometry();atlas();referenceAndScope();continuous();
    String report="DarkRun070 material: "+checks+" checks; "+coats+" real canonical coats; "+oldModels+" preceding model UV footprints; "+boxes+" closed parts; "+alpha+" preserved-alpha pixels; "+eyes+" exact facial pixels; "+markers+" independent Dark marker samples; "+swatches+" six-texel alpha exceptions; "+interiors+" actual closed-face interior samples; "+skullPixels+" retained fixed skull and crown/cheek panels shading samples; "+paletteSamples+" scoped palette-mask samples; maximum one-percent RGB step="+maxStep+". Actual first-Run/full-Dark young, nine observed half-age palette stages and all nine mature palettes, crown-only patch and lateral cheek panels, slim neck/toe trim and outer-hand drops, inherited mono/named/jewel pigments, exact original facial art and alpha, and opaque new geometry passed. Pixel checks are not native visual/shader/multiplayer verification.\n";
    Path out=Path.of(System.getProperty("chao.materialReport","build/reports/lionfish/dark-run070-material-results.txt"));Files.createDirectories(out.toAbsolutePath().getParent());Files.writeString(out,report);System.out.print(report);
  }
}
