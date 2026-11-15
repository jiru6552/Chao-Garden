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
public final class HeroPowerMaterialTest067 {
  private static final Path ROOT=Path.of("src/main/resources/assets/chao_garden");
  private static final float[][] ENDPOINTS={{0,0},{-1,0},{1,0},{0,1},{0,-1},{-1,1},{-1,-1},{1,1},{1,-1}};
  private static long checks,alpha,eyes,halos,skullSamples,swatches,closedSamples,headInterior,wingInterior,tailSamples,paletteSamples;
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
  private static HeroPowerRegionPaint067.Input input(BufferedImage im,int[] flags,boolean natural,boolean two,float age,float rp,float sf){
    return new HeroPowerRegionPaint067.Input(natural,two,age,rp,sf,rgba(im,12,0),rgba(im,48,0),dominant(im,flags,4),dominant(im,flags,32));
  }
  private static float[][] uv(JsonObject face){
    float[][] uv=new float[4][];for(int i=0;i<4;i++){var q=face.getAsJsonArray("uv").get(i).getAsJsonArray();uv[i]=new float[]{q.get(0).getAsFloat(),q.get(1).getAsFloat()};}return uv;
  }
  private static int sample(float[][] uv,float a,float b){
    float[] weights={(1-a)*(1-b),a*(1-b),a*b,(1-a)*b};float u=0,v=0;
    for(int i=0;i<4;i++){u+=uv[i][0]*weights[i];v+=uv[i][1]*weights[i];}
    int x=(int)Math.floor(u*2),y=(int)Math.floor(v*2);check(x>=0&&x<128&&y>=0&&y<64,"actual sampled quad UV left canonical atlas");return y*128+x;
  }
  private static boolean headRegion(String region){return region.equals("crown_root")||region.equals("crown_tip");}
  private static void geometry()throws Exception{
    int boxes=0,roots=0,tips=0,wings=0,haloBars=0,patchFaces=0,wingFaces=0;
    var parts=new HashSet<String>();
    for(var e:model("chao_hero_power_proto067").getAsJsonArray("bones")){
      var bone=e.getAsJsonObject();String boneName=bone.get("name").getAsString();
      for(var m:bone.getAsJsonArray("meshes")){
        var mesh=m.getAsJsonObject();String name=mesh.get("name").getAsString();
        boolean skull=boneName.equals("skull__cube_0"),tail=boneName.startsWith("tail__");
        boolean added=mesh.has("hero_power_box_067");String region=added?mesh.get("hero_power_region_067").getAsString():"";
        if(added){boxes++;check(mesh.getAsJsonArray("faces").size()==6,"authored part is not closed: "+name);}
        if(headRegion(region))check(parts.add(name),"duplicate crown part");
        if(region.equals("crown_root"))roots++;if(region.equals("crown_tip"))tips++;
        if(region.startsWith("wing_"))wings++;if(region.equals("halo"))haloBars++;
        int partPatchFaces=0,partCapFaces=0;
        for(var f:mesh.getAsJsonArray("faces")){
          var face=f.getAsJsonObject();float[][] coordinates=uv(face);
          if(added)for(var q:coordinates)check(q[0]>=0&&q[0]*2<=128&&q[1]>=0&&q[1]*2<=64,"new face escaped original atlas");
          var tilePixels=new HashSet<Integer>();var wingColumns=new HashSet<Integer>();boolean patch=false,cap=false;
          for(int i=0;i<8;i++)for(int j=0;j<8;j++){
            if(!added&&!skull&&!tail)continue;
            int at=sample(coordinates,(i+.5f)/8,(j+.5f)/8),x=at%128,y=at/128;
            if(skull){SKULL.add(at);check(!HeroPowerRegionPaint067.patchTile(x,y),"patch entered original skull UV");}
            if(tail)TAIL.add(at);
            if(!added)continue;
            SURFACES.add(new Surface(name,at,region));
            if(region.equals("crown_root"))check(x==127&&y==63,"white/inherited crown root sampled pigment");
            if(region.equals("crown_tip")){
              if(HeroPowerRegionPaint067.patchTile(x,y)){patch=true;tilePixels.add(at);}
              else{cap=true;check(y==63&&(x==125||x==126),"cap terminal lost separate pigment");}
            }
            if(region.startsWith("wing_")){check(y==63&&(x==123||x==124),"wing sampled non-wing pigment");wingColumns.add(x);}
            if(region.equals("halo"))check(HeroRunMarkerPaint056.markerUv(x,y),"halo sampled non-marker texels");
          }
          if(region.equals("crown_tip")){
            check(patch!=cap,"cap face mixed spot tile with terminal UV");
            if(patch){partPatchFaces++;patchFaces++;check(tilePixels.size()==16,"actual cap face lost two-dimensional four-by-four patch coverage");}
            else partCapFaces++;
          }
          if(region.startsWith("wing_")&&wingColumns.size()==2){wingFaces++;check(wingColumns.contains(123)&&wingColumns.contains(124),"broad wing face lost both colors");}
        }
        if(region.equals("crown_tip"))check(partPatchFaces==4&&partCapFaces==2,"colored cap must have four patch faces and two solid endcaps");
      }
    }
    check(boxes==21&&roots==3&&tips==3&&wings==6&&haloBars==4,"expected manageable21-box Power-first inventory changed");
    check(patchFaces==12&&wingFaces==24,"crown/wing broad face coverage incomplete");
    check(SKULL.size()==384&&TAIL.size()==768,"original skull/tail sample inventory changed");
    int faces=0;
    for(var b:model("chao_baby").getAsJsonArray("bones"))for(var m:b.getAsJsonObject().getAsJsonArray("meshes"))for(var f:m.getAsJsonObject().getAsJsonArray("faces")){
      float[][] points=uv(f.getAsJsonObject());float x0=999,x1=-999,y0=999,y1=-999;
      for(var q:points){x0=Math.min(x0,q[0]*2);x1=Math.max(x1,q[0]*2);y0=Math.min(y0,q[1]*2);y1=Math.max(y1,q[1]*2);}
      int left=(int)Math.floor(x0),right=Math.max(left+1,(int)Math.ceil(x1)),top=(int)Math.floor(y0),bottom=Math.max(top+1,(int)Math.ceil(y1));
      for(int y=60;y<64;y++)for(int x=116;x<120;x++)check(x<left||x>=right||y<top||y>=bottom,"new patch tile touches retained canonical face");
      faces++;
    }
    check(faces>200,"canonical face scan incomplete");
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
          var in=input(image,flags,natural,two,age,e[0],e[1]);var c=HeroPowerRegionPaint067.colors(in);int[] painted=new int[8192];
          for(int y=0;y<64;y++)for(int x=0;x<128;x++){
            int at=y*128+x,p=rgba(image,x,y),result=HeroPowerRegionPaint067.pixel(in,c,x,y,p,flags[at]);painted[at]=result;
            if(HeroPowerRegionPaint067.reservedKind(x,y)!=0){check(p>>>24==0,"new swatch overwrites original art: "+name);check(result>>>24==255,"new feature transparent");swatches++;continue;}
            check(result>>>24==p>>>24,"original alpha changed: "+name);alpha++;
            if((flags[at]&64)!=0){check(result==p,"face/expression/iris changed: "+name);eyes++;}
            if((flags[at]&32)!=0){check(result==(p&0xff000000|HeroPowerRegionPaint067.halo(age,e[0],e[1])&0xffffff),"halo inherited coat/shading instead of development pigment");halos++;}
          }
          for(int at:SKULL){check(painted[at]==rgba(image,at%128,at/128),"original skull paint changed: "+name);skullSamples++;}
          for(int at:TAIL){int f=flags[at];shaded(rgba(image,at%128,at/128),(f&2)!=0?in.accent():in.body(),HeroPowerRegionPaint067.tailColor(c,at%128,at/128),painted[at],"canonical Hero Power tail regional pigment");tailSamples++;}
          for(var v:SURFACES){int p=painted[v.pixel];check(p>>>24==255,"actual closed-box face transparent: "+v.name);closedSamples++;
            int x=v.pixel%128;
            if(headRegion(v.region)){
              int y=v.pixel/128;
              int expected=v.region.equals("crown_root")?c.body():HeroPowerRegionPaint067.patchTile(x,y)?HeroPowerRegionPaint067.patchColor(c,x,y):x==125?c.featureMid():c.featureTip();
              check(p==expected,"actual crown face sampled wrong solid/patch pigment");headInterior++;
            }
            if(v.region.startsWith("wing_")){check(p==(x==124?c.wing():c.wingTip()),"actual wing face sampled wrong pigment");wingInterior++;}
          }
          for(int at:new int[]{34*128+11,36*128+11,37*128+11,35*128+27,38*128+27}){
            int f=flags[at];shaded(rgba(image,at%128,at/128),(f&2)!=0?in.accent():in.body(),HeroPowerRegionPaint067.bibColor(c,at%128,at/128),painted[at],"upper bib/back collar pigment");
          }
          for(int at:new int[]{40*128+11,40*128+28,38*128+4,28*128+12}){
            int f=flags[at];shaded(rgba(image,at%128,at/128),(f&2)!=0?in.accent():in.body(),c.body(),painted[at],"white/inherited belly and torso");
          }
          for(int at:new int[]{31*128+29,32*128+30}){
            int f=flags[at];shaded(rgba(image,at%128,at/128),(f&2)!=0?in.accent():in.body(),c.palm(),painted[at],"localized palm inset");
          }
          for(int at:new int[]{27*128+53,28*128+54}){
            int f=flags[at];shaded(rgba(image,at%128,at/128),(f&2)!=0?in.accent():in.body(),c.foot(),painted[at],"localized shoe-front inset");
          }
          for(int at:new int[]{27*128+65,28*128+45,20*128+53}){
            int f=flags[at];shaded(rgba(image,at%128,at/128),(f&2)!=0?in.accent():in.body(),c.body(),painted[at],"original white/inherited shoe heel");
          }
        }
      }
    }
    check(coats==172,"canonical coat count changed: "+coats);
  }
  private static HeroPowerRegionPaint067.Input natural(float age,float rp,float sf){
    return new HeroPowerRegionPaint067.Input(true,true,age,rp,sf,HeroRunRegionPaint055.rgb(237,246,245),HeroRunRegionPaint055.rgb(61,159,228),HeroRunRegionPaint055.rgb(234,169,227),0xff000000);
  }
  private static void referenceAndScope(){
    var fresh=HeroPowerRegionPaint067.colors(natural(0,0,0));var halos=new HashSet<Integer>();
    for(float[] e:ENDPOINTS){
      check(HeroPowerRegionPaint067.colors(natural(0,e[0],e[1])).equals(fresh),"training overrides common fresh Power-first palette");halos.add(HeroPowerRegionPaint067.halo(1,e[0],e[1]));
      for(float age:new float[]{0,.25f,.5f,.75f,1})for(int p:new int[]{0xff00ff00,0xff000000,0xffffffff,0xffed38ba,0x55001080}){
        var i=natural(age,e[0],e[1]);var c=HeroPowerRegionPaint067.colors(i);int result=HeroPowerRegionPaint067.pixel(i,c,117,3,p,32);
        check((result&0xffffff)==(HeroPowerRegionPaint067.halo(age,e[0],e[1])&0xffffff)&&result>>>24==p>>>24,"halo depends on source pigment/alpha");
      }
    }
    check(halos.size()>=6,"source halo corner distinctions collapsed");
    var balanced=HeroPowerRegionPaint067.colors(natural(1,0,0));var run=HeroPowerRegionPaint067.colors(natural(1,-1,0));
    var power=HeroPowerRegionPaint067.colors(natural(1,1,0));var fly=HeroPowerRegionPaint067.colors(natural(1,0,1));var swim=HeroPowerRegionPaint067.colors(natural(1,0,-1));
    var runFly=HeroPowerRegionPaint067.colors(natural(1,-1,1));var powerFly=HeroPowerRegionPaint067.colors(natural(1,1,1));
    check(red(fresh.marker())>220&&blue(fresh.marker())>220&&green(fresh.marker())<180,"fresh pink halo copied another family");
    check(red(fresh.hand())>220&&red(fresh.spot())>220&&green(fresh.spot())<80,"fresh pink/red Power patches missing");
    check(green(fresh.wing())>red(fresh.wing())+70&&blue(fresh.wing())<50,"fresh green wings replaced by Run-first orange");
    check(red(balanced.bib())>230&&green(balanced.bib())>120&&blue(balanced.bib())<60,"balanced mature orange bib missing");
    check(red(balanced.marker())>230&&green(balanced.marker())>210&&blue(balanced.marker())<60,"balanced mature yellow halo missing");
    check(red(run.featureTip())>230&&green(run.featureTip())>190&&blue(run.featureTip())>120,"opposite Run lost cream cap hue");
    check(red(power.bib())>230&&green(power.bib())<70&&blue(power.bib())<70&&green(power.marker())>200,"Power red bib/lime halo missing");
    check(blue(fly.hand())>220&&blue(fly.marker())>220&&red(fly.marker())<60,"Fly lavender limbs/blue halo missing");
    check(blue(fly.wing())>green(fly.wing())+100&&green(fly.wingTip())>red(fly.wingTip())+100,"Fly royal-blue root/teal tip reversed or missing");
    var flyHalf=HeroPowerRegionPaint067.colors(natural(.5f,0,1));
    check(green(flyHalf.wing())>blue(flyHalf.wing())+70,"half-age Fly wings reached mature blue too early");
    check(red(swim.bib())>220&&blue(swim.bib())>140&&green(swim.bib())<40,"Swim hot-pink bib missing");
    check(green(swim.wingTip())>170&&blue(swim.wingTip())>150,"compact Swim teal wing lost");
    check(red(runFly.marker())>140&&green(runFly.marker())>170&&blue(runFly.marker())>140,"RunFly sage-gray halo missing");
    check(red(runFly.wing())>120&&blue(runFly.wing())>120&&green(runFly.wing())<150&&green(runFly.wingTip())>green(runFly.wing())+30,"RunFly gray-mauve wing root/olive tip reversed or missing");
    check(green(powerFly.marker())>red(powerFly.marker())+80&&green(powerFly.wing())>red(powerFly.wing())+80,"PowerFly green halo/wings missing");
    for(float age:new float[]{0,.5f,1})for(float[] e:ENDPOINTS){
      var in=natural(age,e[0],e[1]);var c=HeroPowerRegionPaint067.colors(in);check(c.body()==in.body()&&c.featureBody()==c.body(),"white/inherited skull/root replaced");
      check(HeroPowerRegionPaint067.tailColor(c,50,32)==c.body(),"canonical tail root lost white/inherited pigment");
      check(HeroPowerRegionPaint067.tailColor(c,65,35)==c.tail(),"actual tail distal end lost colored cap");
      check(HeroPowerRegionPaint067.footColor(c,65,27)==c.body(),"white/inherited heel changed");
    }
    int ruby=HeroRunRegionPaint055.rgb(214,44,93);var mono=new HeroPowerRegionPaint067.Input(false,false,1,1,-1,ruby,ruby,ruby,ruby);var m=HeroPowerRegionPaint067.colors(mono);
    check(m.body()==ruby&&m.featureBody()==ruby,"inherited jewel/mono body/root replaced by white");
    check(m.hand()!=ruby&&m.spot()!=m.hand()&&m.featureMid()!=m.featureTip(),"inherited patch contrast collapsed");
    int[] kindCounts=new int[3];
    for(int y=60;y<64;y++){
      var row=new HashSet<Integer>();for(int x=116;x<120;x++){int k=HeroPowerRegionPaint067.patchKind(x,y);row.add(k);kindCounts[k]++;}
      check(row.size()>1,"patch tile became a horizontal band");
    }
    for(int x=116;x<120;x++){var column=new HashSet<Integer>();for(int y=60;y<64;y++)column.add(HeroPowerRegionPaint067.patchKind(x,y));check(column.size()>1,"patch tile became vertical stripes");}
    check(java.util.Arrays.equals(kindCounts,new int[]{9,4,3}),"compact two-dimensional patch inventory changed");
    for(int scale:new int[]{1,2,4})for(int y=0;y<64*scale;y++)for(int x=0;x<128*scale;x++){
      boolean marker=HeroRunMarkerPaint056.markerUv(x/scale,y/scale),wing=y/scale==63&&(x/scale==123||x/scale==124);
      check(HeroPowerRegionPaint067.paletteProtected("dynamic/hero_power067/example","baby",x,y,128*scale,64*scale)==marker,"halo palette mask differs from original faces");
      check(HeroPowerRegionPaint067.paletteWing("dynamic/hero_power067/example","baby",x,y,128*scale,64*scale)==wing,"new wing palette mask differs");paletteSamples++;
      for(String path:new String[]{"dynamic/hero_run066/example","dynamic/hero_fly065/example","dynamic/hero_swim064/example","textures/entity/chao/baby.png"})check(!HeroPowerRegionPaint067.paletteProtected(path,"baby",x,y,128*scale,64*scale)&&!HeroPowerRegionPaint067.paletteWing(path,"baby",x,y,128*scale,64*scale),"new masks changed earlier route");
      check(!HeroPowerRegionPaint067.paletteProtected("dynamic/hero_power067/example","overlay",x,y,128*scale,64*scale)&&!HeroPowerRegionPaint067.paletteWing("dynamic/hero_power067/example","overlay",x,y,128*scale,64*scale),"animal overlay treated as base atlas");
    }
    int exceptions=0;
    for(int y=0;y<64;y++)for(int x=0;x<128;x++){
      boolean expected=x>=116&&x<120&&y>=60||y==63&&x>=123;
      check((HeroPowerRegionPaint067.reservedKind(x,y)!=0)==expected,"reserved alpha escaped exact21-pixel footprint");if(expected)exceptions++;
    }
    check(exceptions==21,"Power patch/swatch count changed");
    check(HeroPowerRegionPaint067.reservedKind(120,60)==0,"old066 stripe tile was reused");
    check(!HeroPowerRegionPaint067.paletteProtected("dynamic/hero_power067/x","baby",0,0,0,0)&&!HeroPowerRegionPaint067.paletteWing("dynamic/hero_power067/x","baby",0,0,0,0),"zero-size palette accepted");
  }
  private static int red(int c){return c&255;}private static int green(int c){return c>>>8&255;}private static int blue(int c){return c>>>16&255;}
  private static void continuous()throws Exception{
    int[] flags=layout();var im=ImageIO.read(ROOT.resolve("textures/entity/chao/official/coats/baby/normal_hero_0_two.png").toFile());
    for(int path=0;path<8;path++){
      int[] previous=null;
      for(int i=0;i<=100;i++){
        float t=i/100f,age=path==0?t:path==7?1-t:1,rp=path==1?2*t-1:path==3?1-t:path==5?2*t-1:-1,sf=path==2?2*t-1:path==4?1-t:path==5?1-2*t:0;
        var in=input(im,flags,true,true,age,rp,sf);var c=HeroPowerRegionPaint067.colors(in);int[] now=new int[8192];
        for(int y=0;y<64;y++)for(int x=0;x<128;x++){
          int at=y*128+x;now[at]=HeroPowerRegionPaint067.pixel(in,c,x,y,rgba(im,x,y),flags[at]);
          if(previous!=null)for(int k=0;k<3;k++){int d=Math.abs((now[at]>>>(k*8)&255)-(previous[at]>>>(k*8)&255));maxStep=Math.max(maxStep,d);check(d<=9,"1% paint progression discontinuity: "+d);}
        }previous=now;
      }
    }
  }
  public static void main(String[] args)throws Exception{
    geometry();atlas();referenceAndScope();continuous();
    String report="HeroPower067 material: "+checks+" checks; "+coats+" real canonical coats; "+alpha+" preserved-alpha pixels; "+eyes+" exact face pixels; "+halos+" independent halo samples; "+skullSamples+" original-skull paint samples; "+swatches+" 21-texel alpha exceptions; "+closedSamples+" actual closed-box interior samples; "+headInterior+" actual crown root/patch/cap samples; "+wingInterior+" actual two-region wing samples; "+tailSamples+" canonical white-base/colored-tip tail samples; "+paletteSamples+" palette-mask samples; maximum1% RGB step="+maxStep+". Common fresh Power-first palette and nine signed mature corners; opposite Run; two-dimensional cap/glove/shoe patches; upper bib/back collar and white belly/heels/tailbase; delayed Fly wing hues; unchanged original skull/facial alpha/shading; inherited mono/jewel pigments and independent halo passed. Pixel checks are not visual/shader/multiplayer verification.\n";
    Path out=Path.of(System.getProperty("chao.materialReport","build/reports/lionfish/hero-power067-material-results.txt"));Files.createDirectories(out.toAbsolutePath().getParent());Files.writeString(out,report);System.out.print(report);
  }
}
