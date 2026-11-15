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
public final class DarkPowerMaterialTest071 {
  private static final Path ROOT=Path.of("src/main/resources/assets/chao_garden");
  private static final float[][] CORNERS={{0,0},{-1,0},{1,0},{0,-1},{0,1},{-1,-1},{1,-1},{-1,1},{1,1}};
  private static long checks,alpha,eyes,markers,swatches,interiors,skullPixels,paletteSamples;
  private static int coats,maxStep,oldModels,boxes;
  private record Surface(String name,String region,int pixel,boolean forward) {}
  private static final List<Surface> SURFACES=new ArrayList<>();
  private static final HashMap<String,HashSet<Integer>> UV_SAMPLES=new HashMap<>();
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
  private static DarkPowerRegionPaint071.Input input(BufferedImage image,int[] flags,boolean natural,boolean two,float age,float rp,float sf){
    return new DarkPowerRegionPaint071.Input(natural,two,age,rp,sf,rgba(image,12,0),rgba(image,48,0),dominant(image,flags,4),dominant(image,flags,32));
  }
  private static int sample(JsonObject face,float a,float b){
    float[] w={(1-a)*(1-b),a*(1-b),a*b,(1-a)*b};float u=0,v=0;
    for(int i=0;i<4;i++){var q=face.getAsJsonArray("uv").get(i).getAsJsonArray();u+=q.get(0).getAsFloat()*w[i];v+=q.get(1).getAsFloat()*w[i];}
    int x=(int)Math.floor(u*2),y=(int)Math.floor(v*2);check(x>=0&&x<128&&y>=0&&y<64,"actual071 face left canonical atlas");return y*128+x;
  }
  private static void geometry()throws Exception {
    var names=new HashSet<String>();var regions=new HashSet<String>();
    for(var b:model("chao_dark_power_proto071").getAsJsonArray("bones"))for(var m:b.getAsJsonObject().getAsJsonArray("meshes")){
      var mesh=m.getAsJsonObject();if(!mesh.has("dark_power_box_071"))continue;
      boxes++;String name=mesh.get("name").getAsString(),region=mesh.get("dark_power_region_071").getAsString();regions.add(region);
      check(names.add(b.getAsJsonObject().get("name").getAsString()+":"+name),"duplicate071 authored mesh");check(mesh.getAsJsonArray("faces").size()==6,"new071 part is not closed: "+name);
      float frontZ=Float.POSITIVE_INFINITY;
      for(var vertex:mesh.getAsJsonArray("vertices"))frontZ=Math.min(frontZ,vertex.getAsJsonArray().get(2).getAsFloat());
      for(var f:mesh.getAsJsonArray("faces")){
        boolean forward=region.equals("hand")||region.equals("foot");
        for(var index:f.getAsJsonObject().getAsJsonArray("v"))
          forward&=Math.abs(mesh.getAsJsonArray("vertices").get(index.getAsInt()).getAsJsonArray().get(2).getAsFloat()-frontZ)<.00001f;
        for(int y=0;y<8;y++)for(int x=0;x<8;x++){
        int at=sample(f.getAsJsonObject(),(x+.5f)/8,(y+.5f)/8);SURFACES.add(new Surface(name,region,at,forward));UV_SAMPLES.computeIfAbsent(region,k->new HashSet<>()).add(at);int u=at%128,v=at/128;
        if(region.equals("marker"))check(HeroRunMarkerPaint056.markerUv(u,v),"Dark spike sampled non-marker pigment");
        else if(!region.equals("body")&&!region.equals("hand")&&!region.equals("foot")) {
          check(DarkPowerRegionPaint071.reservedKind(u,v)!=0,"new closed box sampled unreserved atlas: "+name);
          if(region.startsWith("wing_"))check(u==68||u==69,"wing lost its own reserved pigment");
          else if(region.equals("tail_tip")||region.equals("tail_root")||region.startsWith("horn_"))check(u>=60&&u<=67,"horn/tail lost its authored broad stripe strip");
          else if(region.equals("fork_tip"))check(u>=71&&u<=73,"fork lost base/middle/tip pigment");
          else check(u>=70&&u<=73,"feature root sampled wing pigment");
        }
        }
      }
    }
    check(SURFACES.stream().filter(Surface::forward).count()==4*64,"actual forward palm/toe faces incomplete");
    check(boxes==24,"unexpected071 closed part inventory: "+boxes);
    check(regions.containsAll(List.of("horn_root","horn_middle","horn_tip","fork_tip","tail_root","tail_tip")),"071 anatomical regions incomplete");
    var bandColors=DarkPowerRegionPaint071.colors(natural(1,0,0));
    for(String region:List.of("horn_root","horn_middle","horn_tip","tail_root","tail_tip")){
      var samples=UV_SAMPLES.get(region);check(samples.size()>=3,"actual broad-band face strip collapsed: "+region);
      check(samples.stream().mapToInt(at->DarkPowerRegionPaint071.reservedColor(bandColors,at%128)).distinct().count()>=2,"actual horn/tail strip lost contrasting bands: "+region);
    }
    try(var stream=Files.list(ROOT.resolve("models/entity/gameplay"))){
      for(var path:stream.filter(p->p.toString().endsWith(".json")&&!p.getFileName().toString().equals("chao_dark_power_proto071.json")).sorted().toList()){
        oldModels++;
        for(var b:read(path).getAsJsonArray("bones"))for(var m:b.getAsJsonObject().getAsJsonArray("meshes"))for(var f:m.getAsJsonObject().getAsJsonArray("faces")){
          float left=999,top=999,right=-999,bottom=-999;
          for(var point:f.getAsJsonObject().getAsJsonArray("uv")){var q=point.getAsJsonArray();float x=q.get(0).getAsFloat()*2,y=q.get(1).getAsFloat()*2;left=Math.min(left,x);right=Math.max(right,x);top=Math.min(top,y);bottom=Math.max(bottom,y);}
          int l=(int)Math.floor(left),r=Math.max(l+1,(int)Math.ceil(right)),t=(int)Math.floor(top),d=Math.max(t+1,(int)Math.ceil(bottom));
          for(int x=60;x<=73;x++)check(x<l||x>=r||63<t||63>=d,"071 reserved texel collides with old model: "+path);
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
          var in=input(image,flags,natural,two,age,corner[0],corner[1]);var colors=DarkPowerRegionPaint071.colors(in);int[] painted=new int[8192];
          if(!natural||!two)check(colors.body()==in.body()&&colors.featureBody()==in.body(),"inherited body pigment replaced: "+name);
          for(int y=0;y<64;y++)for(int x=0;x<128;x++){
            int at=y*128+x,p=rgba(image,x,y),result=DarkPowerRegionPaint071.pixel(in,colors,x,y,p,flags[at]);painted[at]=result;
            if(DarkPowerRegionPaint071.reservedKind(x,y)!=0){check(p>>>24==0,"071 overwrote source art: "+name);check(result>>>24==255,"071 reserved pixel transparent");swatches++;continue;}
            check(result>>>24==p>>>24,"source alpha changed: "+name);alpha++;
            if((flags[at]&64)!=0){check(result==p,"original face/iris/expression changed: "+name);eyes++;}
            if((flags[at]&32)!=0){check(result==(p&0xff000000|DarkPowerRegionPaint071.marker(age,corner[0],corner[1])&0xffffff),"Dark marker inherited coat/finish pigment");markers++;}
          }
          for(int at:SKULL){int f=flags[at];if((f&64)!=0)continue;int x=at%128,y=at/128;
            shading(rgba(image,x,y),(f&2)!=0?in.accent():in.body(),DarkPowerRegionPaint071.headColor(colors,x,y),painted[at],"retained fixed skull base pigment");skullPixels++;
          }
          for(var surface:SURFACES){int at=surface.pixel,p=painted[at];check(p>>>24==255,"actual closed071 interior transparent: "+surface.name);interiors++;
            if(DarkPowerRegionPaint071.reservedKind(at%128,at/128)!=0)check(p==DarkPowerRegionPaint071.reservedColor(colors,at%128),"actual071 surface sampled wrong semantic color");
            if(surface.region.equals("hand")||surface.region.equals("foot")) {
              int u=at%128,v=at/128;
              shading(rgba(image,u,v),(flags[at]&2)!=0?in.accent():in.body(),DarkPowerRegionPaint071.limbColor(colors,u,v),p,"actual closed palm/toe detail and base planes");
            }
          }
          for(int at:new int[]{35*128+11,36*128+10,39*128+12})shading(rgba(image,at%128,at/128),(flags[at]&2)!=0?in.accent():in.body(),colors.chest(),painted[at],"actual compact front-only belly");
          for(int at:new int[]{37*128+27,39*128+28,41*128+11})shading(rgba(image,at%128,at/128),(flags[at]&2)!=0?in.accent():in.body(),colors.body(),painted[at],"dark back/lower torso");
        }
      }
    }
    check(coats==172,"real canonical coat audit incomplete: "+coats);
  }
  private static DarkPowerRegionPaint071.Input natural(float age,float rp,float sf){return new DarkPowerRegionPaint071.Input(true,true,age,rp,sf,0xff3b362f,0xff655fe4,0xffb034b7,0xff000000);}
  private static int red(int c){return c&255;}private static int green(int c){return c>>>8&255;}private static int blue(int c){return c>>>16&255;}
  private static void referenceAndScope(){
    var young=DarkPowerRegionPaint071.colors(natural(0,0,0));var markers=new HashSet<Integer>();var palettes=new HashSet<DarkPowerRegionPaint071.Colors>();
    for(float[] corner:CORNERS){
      check(DarkPowerRegionPaint071.colors(natural(0,corner[0],corner[1])).equals(young),"axes replaced common fresh Power-first pigment");
      var colors=DarkPowerRegionPaint071.colors(natural(1,corner[0],corner[1]));markers.add(colors.marker());palettes.add(colors);
    }
    check(palettes.size()==9,"mature full-palette distinctions collapsed");
    check(markers.size()>=7,"observed distinct and close marker hue classes collapsed");
    check(red(young.body())>blue(young.body())+40&&green(young.body())>blue(young.body())+30,"fresh brown/olive body missing");
    check(red(young.featureMid())>210&&green(young.featureMid())>65&&blue(young.featureMid())<25,"fresh orange horn/palm bands missing");
    check(red(young.chest())>160&&green(young.chest())>155&&blue(young.chest())<35,"fresh yellow/olive belly missing");
    check(red(young.marker())>245&&green(young.marker())>205&&blue(young.marker())<30,"fresh independent yellow Dark marker missing");
    var balanced=DarkPowerRegionPaint071.colors(natural(1,0,0));var swim=DarkPowerRegionPaint071.colors(natural(1,0,-1));
    var run=DarkPowerRegionPaint071.colors(natural(1,-1,0));var power=DarkPowerRegionPaint071.colors(natural(1,1,0));var fly=DarkPowerRegionPaint071.colors(natural(1,0,1));
    var runFly=DarkPowerRegionPaint071.colors(natural(1,-1,1));var runSwim=DarkPowerRegionPaint071.colors(natural(1,-1,-1));
    var powerFly=DarkPowerRegionPaint071.colors(natural(1,1,1));var powerSwim=DarkPowerRegionPaint071.colors(natural(1,1,-1));
    check(blue(balanced.body())>green(balanced.body())+20&&red(balanced.body())>green(balanced.body())+15,"balanced dark plum body missing");
    check(blue(balanced.chest())>230&&red(balanced.chest())>135&&green(balanced.chest())<135,"balanced lavender belly missing");
    check(red(balanced.marker())>225&&blue(balanced.marker())>240&&green(balanced.marker())>150,"balanced pink/lavender marker missing");
    check(green(run.body())>blue(run.body())+40&&red(run.body())>blue(run.body())+40,"opposite Run olive body missing");
    check(green(run.featureTip())>220&&blue(run.featureTip())<35,"Run yellow/green fork caps missing");
    check(blue(run.wingTip())>225&&red(run.wingTip())<20,"Run blue wing rims missing");
    check(blue(run.marker())>225&&red(run.marker())<20,"Run blue marker missing");
    check(red(power.body())>blue(power.body())+45&&green(power.body())>blue(power.body())+30,"Power brown body missing");
    check(red(power.featureMid())>220&&green(power.featureMid())>65&&blue(power.featureMid())<30,"Power orange bands missing");
    check(red(power.marker())>245&&green(power.marker())>215&&blue(power.marker())<30,"Power yellow marker missing");
    check(blue(fly.body())>red(fly.body())+10&&blue(fly.chest())>225&&green(fly.chest())>140,"Fly slate/blue body and belly missing");
    check(blue(fly.featureTip())>225&&green(fly.featureTip())>150,"Fly blue drooping fork caps missing");
    check(green(fly.wingTip())>225&&red(fly.wingTip())>95&&blue(fly.wingTip())<40,"Fly green wing rims missing");
    check(green(fly.marker())>240&&red(fly.marker())>150&&blue(fly.marker())>125,"Fly pale green marker missing");
    check(green(swim.body())>red(swim.body())+45&&green(swim.featureMid())>90&&red(swim.featureMid())<20,"Swim deep green bands/body missing");
    check(green(swim.chest())>205&&blue(swim.chest())>150&&red(swim.chest())<20,"Swim emerald/cyan belly missing");
    check(red(swim.marker())>235&&green(swim.marker())>120&&blue(swim.marker())>140,"Swim salmon marker missing");
    check(green(runFly.chest())>180&&blue(runFly.chest())>165&&red(runFly.chest())>135,"RunFly mint belly missing");
    check(green(runFly.marker())>175&&blue(runFly.marker())>185&&red(runFly.marker())>75,"RunFly teal marker missing");
    check(green(runSwim.chest())>190&&red(runSwim.chest())<90,"RunSwim bright green belly missing");
    check(blue(runSwim.marker())>175&&green(runSwim.marker())>120&&red(runSwim.marker())>90,"RunSwim slate-blue marker missing");
    check(green(powerFly.chest())>175&&blue(powerFly.chest())>165&&red(powerFly.chest())>140,"PowerFly pale mint/gray belly missing");
    check(red(powerFly.marker())>210&&green(powerFly.marker())>245&&blue(powerFly.marker())<60,"PowerFly close lemon/yellow-green marker missing");
    check(green(powerSwim.chest())>185&&blue(powerSwim.chest())>100,"PowerSwim green belly missing");
    check(red(powerSwim.marker())>245&&green(powerSwim.marker())>165&&blue(powerSwim.marker())<80,"PowerSwim orange marker missing");
    var halfBalanced=DarkPowerRegionPaint071.colors(natural(.5f,0,0));var halfFly=DarkPowerRegionPaint071.colors(natural(.5f,0,1));
    var halfSwim=DarkPowerRegionPaint071.colors(natural(.5f,0,-1));var halfRunFly=DarkPowerRegionPaint071.colors(natural(.5f,-1,1));
    var halfRunSwim=DarkPowerRegionPaint071.colors(natural(.5f,-1,-1));var halfPowerSwim=DarkPowerRegionPaint071.colors(natural(.5f,1,-1));
    check(Math.abs(red(halfBalanced.chest())-green(halfBalanced.chest()))<10&&Math.abs(green(halfBalanced.chest())-blue(halfBalanced.chest()))<10,"half balanced skipped observed gray belly");
    check(red(halfBalanced.marker())>245&&green(halfBalanced.marker())>170&&blue(halfBalanced.marker())<130,"half balanced skipped peach/orange marker");
    check(red(halfFly.body())>blue(halfFly.body())&&red(halfFly.featureMid())>195&&green(halfFly.featureMid())>65,"half Fly skipped brown/russet phase");
    check(green(halfFly.chest())>160&&blue(halfFly.chest())>155,"half Fly gray/mint belly missing");
    check(red(halfSwim.marker())>245&&green(halfSwim.marker())>160&&blue(halfSwim.marker())<90,"half Swim marker became mature salmon too early");
    check(green(halfRunFly.marker())>210&&red(halfRunFly.marker())>165&&blue(halfRunFly.marker())<130,"half RunFly chartreuse marker missing");
    check(red(halfRunSwim.marker())>165&&green(halfRunSwim.marker())>160&&blue(halfRunSwim.marker())<90,"half RunSwim ochre marker missing");
    check(red(halfPowerSwim.marker())>245&&green(halfPowerSwim.marker())>210&&blue(halfPowerSwim.marker())<35,"half PowerSwim yellow marker missing");
    for(int y=0;y<24;y++)for(int x=0;x<40;x++)check(DarkPowerRegionPaint071.headColor(balanced,x,y)==balanced.body(),"head acquired painted horn bands");
    check(DarkPowerRegionPaint071.bellyUv(11,37)&&!DarkPowerRegionPaint071.bellyUv(27,37)&&!DarkPowerRegionPaint071.bellyUv(9,34),"belly spread to back/neck");
    check(DarkPowerRegionPaint071.handBandUv(25,31)&&!DarkPowerRegionPaint071.handBandUv(25,28),"compact palm bands escaped side islands");
    check(DarkPowerRegionPaint071.limbColor(balanced,25,30)==balanced.limb()&&DarkPowerRegionPaint071.limbColor(balanced,25,31)==balanced.body(),"palm stripes no longer alternate around closed sides");
    check(DarkPowerRegionPaint071.footSideUv(54,28)&&DarkPowerRegionPaint071.limbColor(balanced,54,28)==balanced.foot(),"lower toe pigment missing");
    for(int scale:new int[]{1,2,4})for(int y=0;y<64*scale;y++)for(int x=0;x<128*scale;x++){
      boolean marker=HeroRunMarkerPaint056.markerUv(x/scale,y/scale),wing=y/scale==63&&(x/scale==68||x/scale==69);
      check(DarkPowerRegionPaint071.paletteProtected("dynamic/dark_power071/x","baby",x,y,128*scale,64*scale)==marker,"071 palette marker mask incomplete");
      check(DarkPowerRegionPaint071.paletteWing("dynamic/dark_power071/x","baby",x,y,128*scale,64*scale)==wing,"071 cosmetic wing mask incomplete");paletteSamples++;
      for(String path:new String[]{"dynamic/dark_run070/x","dynamic/dark_fly069/x","dynamic/dark_swim068/x","dynamic/hero_power067/x","dynamic/hero_run066/x","dynamic/hero_fly065/x","dynamic/hero_swim064/x","dynamic/neutral_power063/x","dynamic/neutral_run062/x","dynamic/neutral_fly061/x","dynamic/dark_normal060/x","dynamic/neutral_swim059/x","dynamic/hero_normal057/x","dynamic/neutral056/x","dynamic/hero_run055/x","textures/entity/chao/baby.png"})check(!DarkPowerRegionPaint071.paletteProtected(path,"baby",x,y,128*scale,64*scale)&&!DarkPowerRegionPaint071.paletteWing(path,"baby",x,y,128*scale,64*scale),"071 masks changed older route");
      check(!DarkPowerRegionPaint071.paletteProtected("dynamic/dark_power071/x","overlay",x,y,128*scale,64*scale)&&!DarkPowerRegionPaint071.paletteWing("dynamic/dark_power071/x","overlay",x,y,128*scale,64*scale),"overlay acquired071 masks");
    }
    int count=0;for(int y=0;y<64;y++)for(int x=0;x<128;x++){boolean expected=y==63&&x>=60&&x<=73;check((DarkPowerRegionPaint071.reservedKind(x,y)!=0)==expected,"071 alpha exception escaped exact fourteen-pixel footprint");if(expected)count++;}
    check(count==14,"071 reserved texel count changed");
    for(int x=60;x<=67;x++)check(DarkPowerRegionPaint071.reservedColor(balanced,x)==(x==67?balanced.featureTip():(x&1)==0?balanced.featureBody():balanced.featureMid()),"horn/tail stripe order changed");
    check(!DarkPowerRegionPaint071.paletteProtected("dynamic/dark_power071/x","baby",0,0,0,0)&&!DarkPowerRegionPaint071.paletteWing("dynamic/dark_power071/x","baby",0,0,0,0),"zero dimensions accepted");
    check(DarkPowerRegionPaint071.colors(natural(Float.NaN,Float.NaN,Float.NaN)).equals(young),"nonfinite display inputs escaped clamping");
  }

  private static void continuous()throws Exception {
    int[] flags=layout();var image=ImageIO.read(ROOT.resolve("textures/entity/chao/official/coats/baby/normal_dark_0_two.png").toFile());
    // All nine age paths in both directions plus signed training crossings at
    // fixed adult maturity. This samples actual painted pixels, not just keys.
    for(int path=0;path<26;path++){
      int[] previous=null;
      for(int i=0;i<=100;i++){
        float t=i/100f,age,rp,sf;
        if(path<18){float[] corner=CORNERS[path%9];age=path<9?t:1-t;rp=corner[0];sf=corner[1];}
        else {age=path<22?1:.5f;int axis=(path-18)%4;rp=axis==0?2*t-1:axis==1?0:axis==2?2*t-1:1-2*t;sf=axis==0?0:axis==1?2*t-1:axis==2?1-2*t:2*t-1;}
        var in=input(image,flags,true,true,age,rp,sf);var colors=DarkPowerRegionPaint071.colors(in);int[] now=new int[8192];
        for(int y=0;y<64;y++)for(int x=0;x<128;x++){
          int at=y*128+x;now[at]=DarkPowerRegionPaint071.pixel(in,colors,x,y,rgba(image,x,y),flags[at]);
          if(previous!=null)for(int k=0;k<3;k++){int step=Math.abs((now[at]>>>(k*8)&255)-(previous[at]>>>(k*8)&255));maxStep=Math.max(maxStep,step);check(step<=10,"one-percent material progression jumped: "+step);}
        }
        previous=now;
      }
    }
  }

  public static void main(String[] args)throws Exception {
    geometry();atlas();referenceAndScope();continuous();
    String report="DarkPower071 material: "+checks+" checks; "+coats+" real canonical coats; "+oldModels+" preceding model UV footprints; "+boxes+" closed parts; "+alpha+" preserved-alpha pixels; "+eyes+" exact facial pixels; "+markers+" independent Dark marker samples; "+swatches+" fourteen-texel alpha exceptions; "+interiors+" actual closed-face interior samples; "+skullPixels+" retained fixed-skull base-pigment shading samples; "+paletteSamples+" scoped palette-mask samples; maximum one-percent RGB step="+maxStep+". Actual first-Power/full-Dark young, nine observed half-age stages and nine mature palettes, three broad horn/tail bands, compact striped palms, front-only belly, two-tone toes, inherited mono/named/jewel pigments and original facial alpha passed. All nine age paths in both directions and signed/mixed training crossings at fixed mature/half age were sampled. Pixel checks are not native visual/shader/multiplayer verification.\n";
    Path out=Path.of(System.getProperty("chao.materialReport","build/reports/lionfish/dark-power071-material-results.txt"));Files.createDirectories(out.toAbsolutePath().getParent());Files.writeString(out,report);System.out.print(report);
  }
}
