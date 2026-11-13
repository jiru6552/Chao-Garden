package com.chaogarden.client;

import com.google.gson.JsonArray;
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

/** Actual original atlases and current authored quad interiors. Pixel policy
 * checks only: no native renderer, screenshot or visual acceptance is implied. */
public final class HeroSwimMaterialTest064 {
  private static final Path ROOT=Path.of("src/main/resources/assets/chao_garden");
  private static final float[][] ENDPOINTS={{0,0},{-1,0},{1,0},{0,1},{0,-1},{-1,1},{-1,-1},{1,1},{1,-1}};
  private static long checks,alpha,eyes,halos,skullSamples,swatches,closedSamples,capInterior,wingInterior,tailSamples,paletteSamples;
  private static int coats,maxStep;
  private record Surface(String name,int pixel,String region) {}
  private static final List<Surface> SURFACES=new ArrayList<>();
  private static final List<Integer> SKULL=new ArrayList<>(),TAIL=new ArrayList<>();
  private static void check(boolean p,String why){checks++;if(!p)throw new AssertionError(why);}
  private static int rgba(BufferedImage image,int x,int y){int p=image.getRGB(x,y);return p&0xff00ff00|(p&255)<<16|p>>>16&255;}
  private static JsonObject model(String name)throws Exception{return JsonParser.parseString(Files.readString(ROOT.resolve("models/entity/gameplay/"+name+".json"))).getAsJsonObject();}
  private static int[] layout()throws Exception {
    int[] flags=new int[8192];String[] masks={"baby","baby_accent","baby_wings"};int[] tags={1,2,4};
    for(int i=0;i<3;i++){
      var image=ImageIO.read(ROOT.resolve("textures/entity/chao/gameplay/masks/"+masks[i]+".png").toFile());
      for(int y=0;y<64;y++)for(int x=0;x<128;x++)if(image.getRGB(x,y)>>>24!=0)flags[y*128+x]|=tags[i];
    }
    for(var element:model("chao_baby").getAsJsonArray("bones")){
      var bone=element.getAsJsonObject();String name=bone.get("name").getAsString();int tag=0;
      if(name.equals("skull__cube_2")||name.equals("skull__cube_3")||name.startsWith("expression_")||name.startsWith("iris_"))tag|=64;
      if(name.startsWith("arm_")||name.startsWith("foot_"))tag|=8;
      if(name.startsWith("tail__"))tag|=16;
      if(name.equals("emotion_dot__cube_0"))tag|=32;
      if(tag==0)continue;
      for(var m:bone.getAsJsonArray("meshes"))for(var f:m.getAsJsonObject().getAsJsonArray("faces")){
        float x0=999,y0=999,x1=-999,y1=-999;
        for(var point:f.getAsJsonObject().getAsJsonArray("uv")){
          var q=point.getAsJsonArray();float x=q.get(0).getAsFloat()*2,y=q.get(1).getAsFloat()*2;
          x0=Math.min(x0,x);x1=Math.max(x1,x);y0=Math.min(y0,y);y1=Math.max(y1,y);
        }
        for(int y=Math.max(0,(int)Math.floor(y0));y<Math.min(64,(int)Math.ceil(y1));y++)for(int x=Math.max(0,(int)Math.floor(x0));x<Math.min(128,(int)Math.ceil(x1));x++)flags[y*128+x]|=tag;
      }
    }
    int count=0;
    for(int y=0;y<64;y++)for(int x=0;x<128;x++){
      boolean marker=(flags[y*128+x]&32)!=0;
      check(marker==HeroRunMarkerPaint056.markerUv(x,y),"semantic halo mask differs from actual six original marker UV faces");
      if(marker){count++;check((flags[y*128+x]&65)==0,"halo overlaps head or face");}
    }
    check(count==96,"incomplete original six-face marker coverage");return flags;
  }
  private static int dominant(BufferedImage im,int[] flags,int tag){
    var counts=new HashMap<Integer,Integer>();for(int y=0;y<64;y++)for(int x=0;x<128;x++){
      int p=rgba(im,x,y);if(p>>>24!=0&&(flags[y*128+x]&tag)!=0)counts.merge(p&0xffffff,1,Integer::sum);
    }
    return counts.entrySet().stream().max(java.util.Map.Entry.comparingByValue()).map(java.util.Map.Entry::getKey).orElse(0xffffff)|0xff000000;
  }
  private static HeroSwimRegionPaint064.Input input(BufferedImage im,int[] flags,boolean natural,boolean two,float age,float rp,float sf){
    return new HeroSwimRegionPaint064.Input(natural,two,age,rp,sf,rgba(im,12,0),rgba(im,48,0),dominant(im,flags,4),dominant(im,flags,32));
  }
  private static float[][] uv(JsonObject face){
    float[][] uv=new float[4][];for(int i=0;i<4;i++){var q=face.getAsJsonArray("uv").get(i).getAsJsonArray();uv[i]=new float[]{q.get(0).getAsFloat(),q.get(1).getAsFloat()};}return uv;
  }
  private static int sample(float[][] uv,float a,float b){
    float[] weights={(1-a)*(1-b),a*(1-b),a*b,(1-a)*b};float u=0,v=0;
    for(int i=0;i<4;i++){u+=uv[i][0]*weights[i];v+=uv[i][1]*weights[i];}
    int x=(int)Math.floor(u*2),y=(int)Math.floor(v*2);check(x>=0&&x<128&&y>=0&&y<64,"actual sampled quad UV left canonical atlas");return y*128+x;
  }
  private static void geometry()throws Exception{
    int boxes=0,crownRoots=0,crownTips=0,wings=0,haloBars=0,capFaces=0,wingFaces=0;
    for(var e:model("chao_hero_swim_proto064").getAsJsonArray("bones")){
      var bone=e.getAsJsonObject();String boneName=bone.get("name").getAsString();
      for(var m:bone.getAsJsonArray("meshes")){
        var mesh=m.getAsJsonObject();String name=mesh.get("name").getAsString();
        boolean skull=boneName.equals("skull__cube_0"),tail=boneName.startsWith("tail__");
        boolean added=mesh.has("hero_swim_box_064");String region=added?mesh.get("hero_swim_region_064").getAsString():"";
        if(added){boxes++;check(mesh.getAsJsonArray("faces").size()==6,"authored part is not a closed cuboid: "+name);}
        if(region.equals("crown_root"))crownRoots++;if(region.equals("crown_tip"))crownTips++;
        if(region.startsWith("wing_"))wings++;if(region.equals("halo"))haloBars++;
        for(var f:mesh.getAsJsonArray("faces")){
          var face=f.getAsJsonObject();float[][] coordinates=uv(face);
          if(added)for(var q:coordinates)check(q[0]>=0&&q[0]*2<=128&&q[1]>=0&&q[1]*2<=64,"new face corner escaped original atlas boundary");
          var colors=new HashSet<Integer>();
          for(int i=0;i<8;i++)for(int j=0;j<8;j++){
            // Old question/dizzy UVs intentionally extend outside the canonical
            // skin atlas; only the actual skull, tail and new closed parts are
            // subject to this new-profile interior sample inventory.
            if(!added&&!skull&&!tail)continue;
            int at=sample(coordinates,(i+.5f)/8,(j+.5f)/8),x=at%128,y=at/128;
            if(skull)SKULL.add(at);if(tail)TAIL.add(at);
            if(added){SURFACES.add(new Surface(name,at,region));colors.add(x);
              if(region.equals("crown_root"))check(x==127&&y==63,"white/inherited crown root sampled cap or wing pigment");
              if(region.equals("crown_tip"))check(y==63&&(x==125||x==126),"crown cap lost separate proximal/distal pigment");
              if(region.startsWith("wing_"))check(y==63&&(x==123||x==124),"wing lost separate proximal/distal pigment");
              if(region.equals("halo"))check(HeroRunMarkerPaint056.markerUv(x,y),"halo bar sampled non-marker pigment");
            }
          }
          if(region.equals("crown_tip")&&colors.size()==2){capFaces++;check(colors.contains(125)&&colors.contains(126),"actual cap broad face lacks both colors");}
          if(region.startsWith("wing_")&&colors.size()==2){wingFaces++;check(colors.contains(123)&&colors.contains(124),"actual wing broad face lacks both colors");}
        }
      }
    }
    check(boxes==17&&crownRoots==2&&crownTips==2&&wings==4&&haloBars==4,"expected manageable 17-box authored rig inventory changed");
    check(capFaces==8&&wingFaces==16,"two-color paint is not on all actual longitudinal crown/wing faces");
    check(SKULL.size()==384&&TAIL.size()==768,"original skull/tail six-face interior inventories changed");
  }
  private static void shaded(int source,int ref,int target,int actual,String why){
    check(actual>>>24==source>>>24,why+" alpha changed");
    for(int k=0;k<3;k++){
      int from=source>>>(k*8)&255,base=ref>>>(k*8)&255,to=target>>>(k*8)&255,got=actual>>>(k*8)&255;
      check(base>0,why+" reference channel is zero");
      long wanted=(long)from*to;
      check(wanted>=255L*base?got==255:Math.abs((long)got*base-wanted)<=base*.5+.001,why+" source shading or pigment changed");
    }
  }
  private static void atlas()throws Exception{
    int[] flags=layout();
    try(var stream=Files.list(ROOT.resolve("textures/entity/chao/official/coats/baby"))){
      for(var path:stream.filter(p->p.toString().endsWith(".png")).sorted().toList()){
        String name=path.getFileName().toString();var image=ImageIO.read(path.toFile());coats++;
        boolean natural=name.startsWith("normal_hero_0_"),two=name.contains("_two");
        for(float age:new float[]{0,.5f,1})for(float[] e:ENDPOINTS){
          var in=input(image,flags,natural,two,age,e[0],e[1]);var c=HeroSwimRegionPaint064.colors(in);int[] painted=new int[8192];
          for(int y=0;y<64;y++)for(int x=0;x<128;x++){
            int at=y*128+x,p=rgba(image,x,y),result=HeroSwimRegionPaint064.pixel(in,c,x,y,p,flags[at]);painted[at]=result;
            if(HeroSwimRegionPaint064.reservedKind(x,y)!=0){check(p>>>24==0,"new swatch overwrites original art: "+name);check(result>>>24==255,"new feature transparent");swatches++;continue;}
            check(result>>>24==p>>>24,"original alpha changed: "+name);alpha++;
            if((flags[at]&64)!=0){check(result==p,"face/expression/iris changed: "+name);eyes++;}
            if((flags[at]&32)!=0){check(result==(p&0xff000000|HeroSwimRegionPaint064.halo(age,e[0],e[1])&0xffffff),"halo inherited coat/shading instead of development pigment");halos++;}
          }
          for(int at:SKULL){check(painted[at]==rgba(image,at%128,at/128),"original skull paint changed: "+name);skullSamples++;}
          for(int at:TAIL){int f=flags[at];shaded(rgba(image,at%128,at/128),(f&2)!=0?in.accent():in.body(),c.body(),painted[at],"original Hero Swim white/inherited tail");tailSamples++;}
          for(var v:SURFACES){int p=painted[v.pixel];check(p>>>24==255,"actual closed-box face transparent: "+v.name);closedSamples++;
            int x=v.pixel%128;
            if(v.region.equals("crown_tip")){check(p==(x==125?c.featureMid():c.featureTip()),"actual cap face sampled wrong pigment");capInterior++;}
            if(v.region.startsWith("wing_")){check(p==(x==124?c.wing():c.wingTip()),"actual wing face sampled wrong pigment");wingInterior++;}
          }
          // Actual torso-front core stays white/inherited; mint/slate/purple
          // belongs to its surrounding torso, never the original head box.
          for(int at:new int[]{37*128+11,39*128+12}){int f=flags[at];shaded(rgba(image,at%128,at/128),(f&2)!=0?in.accent():in.body(),c.belly(),painted[at],"white/inherited belly core");}
        }
      }
    }
    check(coats==172,"canonical coat count changed: "+coats);
  }
  private static HeroSwimRegionPaint064.Input natural(float age,float rp,float sf){
    return new HeroSwimRegionPaint064.Input(true,true,age,rp,sf,HeroRunRegionPaint055.rgb(237,246,245),HeroRunRegionPaint055.rgb(61,159,228),HeroRunRegionPaint055.rgb(234,169,227),0xff000000);
  }
  private static void referenceAndScope(){
    var fresh=HeroSwimRegionPaint064.colors(natural(0,0,0));var halos=new HashSet<Integer>();
    for(float[] e:ENDPOINTS){
      check(HeroSwimRegionPaint064.colors(natural(0,e[0],e[1])).equals(fresh),"training overrides common fresh Swim-first form");halos.add(HeroSwimRegionPaint064.halo(1,e[0],e[1]));
      for(float age:new float[]{0,.25f,.5f,.75f,1})for(int p:new int[]{0xff00ff00,0xff000000,0xffffffff,0xffed38ba,0x55001080}){
        var i=natural(age,e[0],e[1]);var c=HeroSwimRegionPaint064.colors(i);int result=HeroSwimRegionPaint064.pixel(i,c,117,3,p,32);
        check((result&0xffffff)==(HeroSwimRegionPaint064.halo(age,e[0],e[1])&0xffffff)&&result>>>24==p>>>24,"halo tint or alpha depends on source pigment");
      }
    }
    check(halos.size()==9,"nine referenced halo corners collapsed");
    var balanced=HeroSwimRegionPaint064.colors(natural(1,0,0));var swim=HeroSwimRegionPaint064.colors(natural(1,0,-1));var fly=HeroSwimRegionPaint064.colors(natural(1,0,1));var power=HeroSwimRegionPaint064.colors(natural(1,1,0));var mixed=HeroSwimRegionPaint064.colors(natural(1,1,-1));
    check(red(fresh.marker())>220&&green(fresh.marker())>220&&blue(fresh.marker())<130,"fresh yellow halo replaced by HeroNormal cyan");
    check(blue(balanced.marker())>red(balanced.marker())+150,"mature balanced halo not blue");
    check(blue(swim.torso())>green(swim.torso())+120&&blue(swim.marker())>green(swim.marker())+100,"Swim specialization lost violet body/halo");
    check(green(fly.torso())>red(fly.torso())+150&&green(fly.featureMid())>blue(fly.featureMid())+150,"opposite Fly does not develop distinct green/lime regions");
    check(red(power.hand())>220&&green(power.hand())>180&&blue(power.hand())<60,"Power mitts lost gold pigment");
    check(blue(mixed.torso())>red(mixed.torso())&&red(mixed.marker())>green(mixed.marker()),"Power+Swim lost slate torso/dusty pink halo");
    for(float age:new float[]{0,.5f,1})for(float[] e:ENDPOINTS){var c=HeroSwimRegionPaint064.colors(natural(age,e[0],e[1]));check(c.body()==natural(age,e[0],e[1]).body()&&c.featureBody()==c.body()&&c.belly()==c.body(),"white skull/root/belly replaced by colored torso");}
    int ruby=HeroRunRegionPaint055.rgb(214,44,93);var mono=new HeroSwimRegionPaint064.Input(false,false,1,1,-1,ruby,ruby,ruby,ruby);var m=HeroSwimRegionPaint064.colors(mono);
    check(m.body()==ruby&&m.featureBody()==ruby&&m.belly()==ruby,"inherited jewel/mono body replaced by natural white");check(m.torso()!=ruby&&m.featureTip()!=ruby&&m.featureMid()!=m.featureTip(),"low-contrast inherited details disappeared");
    for(int scale:new int[]{1,2,4})for(int y=0;y<64*scale;y++)for(int x=0;x<128*scale;x++){
      boolean marker=HeroRunMarkerPaint056.markerUv(x/scale,y/scale),wing=y/scale==63&&(x/scale==123||x/scale==124);
      check(HeroSwimRegionPaint064.paletteProtected("dynamic/hero_swim064/example","baby",x,y,128*scale,64*scale)==marker,"halo palette protection differs from source faces");
      check(HeroSwimRegionPaint064.paletteWing("dynamic/hero_swim064/example","baby",x,y,128*scale,64*scale)==wing,"added wing palette mask differs");paletteSamples++;
      for(String path:new String[]{"dynamic/neutral_power063/example","dynamic/hero_normal057/example","textures/entity/chao/baby.png"})check(!HeroSwimRegionPaint064.paletteProtected(path,"baby",x,y,128*scale,64*scale)&&!HeroSwimRegionPaint064.paletteWing(path,"baby",x,y,128*scale,64*scale),"new masks changed an older profile");
      check(!HeroSwimRegionPaint064.paletteProtected("dynamic/hero_swim064/example","overlay",x,y,128*scale,64*scale)&&!HeroSwimRegionPaint064.paletteWing("dynamic/hero_swim064/example","overlay",x,y,128*scale,64*scale),"animal overlay treated as base marker/wing");
    }
    for(int y=0;y<64;y++)for(int x=0;x<128;x++)check((HeroSwimRegionPaint064.reservedKind(x,y)!=0)==(y==63&&x>=123),"reserved alpha escaped exactly five transparent swatches");
    check(!HeroSwimRegionPaint064.paletteProtected("dynamic/hero_swim064/x","baby",0,0,0,0)&&!HeroSwimRegionPaint064.paletteWing("dynamic/hero_swim064/x","baby",0,0,0,0),"zero-dimension palette input accepted");
  }
  private static int red(int c){return c&255;}private static int green(int c){return c>>>8&255;}private static int blue(int c){return c>>>16&255;}
  private static void continuous()throws Exception{
    int[] flags=layout();var im=ImageIO.read(ROOT.resolve("textures/entity/chao/official/coats/baby/normal_hero_0_two.png").toFile());
    for(int path=0;path<8;path++){
      int[] previous=null;
      for(int i=0;i<=100;i++){
        float t=i/100f,age=path==0?t:path==7?1-t:1,rp=path==1?2*t-1:path==3?1-t:path==5?2*t-1:-1,sf=path==2?2*t-1:path==4?1-t:path==5?1-2*t:0;
        var in=input(im,flags,true,true,age,rp,sf);var c=HeroSwimRegionPaint064.colors(in);int[] now=new int[8192];
        for(int y=0;y<64;y++)for(int x=0;x<128;x++){
          int at=y*128+x;now[at]=HeroSwimRegionPaint064.pixel(in,c,x,y,rgba(im,x,y),flags[at]);
          if(previous!=null)for(int k=0;k<3;k++){int d=Math.abs((now[at]>>>(k*8)&255)-(previous[at]>>>(k*8)&255));maxStep=Math.max(maxStep,d);check(d<=9,"1% paint progression discontinuity: "+d);}
        }previous=now;
      }
    }
  }
  public static void main(String[] args)throws Exception{
    geometry();atlas();referenceAndScope();continuous();
    String report="HeroSwim064 material: "+checks+" checks; "+coats+" real canonical coats; "+alpha+" preserved-alpha pixels; "+eyes+" exact face pixels; "+halos+" independent halo samples; "+skullSamples+" original-skull paint samples; "+swatches+" five-swatch alpha exceptions; "+closedSamples+" actual closed-box interior samples; "+capInterior+" actual two-region cap samples; "+wingInterior+" actual two-region wing samples; "+tailSamples+" retained white/inherited tail samples; "+paletteSamples+" palette-mask samples; maximum1% RGB step="+maxStep+". Nine mature corners and common fresh Swim-first palette, signed opposite Fly, white/inherited skull/belly, colored torso, two-color cap/wing broad faces, unchanged eyes/alpha/shading, inherited mono/jewel hue and source-independent halo passed. Pixel policy checks, not a visual/shader/multiplayer claim.\n";
    Path out=Path.of(System.getProperty("chao.materialReport","build/reports/lionfish/hero-swim064-material-results.txt"));Files.createDirectories(out.toAbsolutePath().getParent());Files.writeString(out,report);System.out.print(report);
  }
}
