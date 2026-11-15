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
public final class HeroRunMaterialTest066 {
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
  private static HeroRunRegionPaint066.Input input(BufferedImage im,int[] flags,boolean natural,boolean two,float age,float rp,float sf){
    return new HeroRunRegionPaint066.Input(natural,two,age,rp,sf,rgba(im,12,0),rgba(im,48,0),dominant(im,flags,4),dominant(im,flags,32));
  }
  private static float[][] uv(JsonObject face){
    float[][] uv=new float[4][];for(int i=0;i<4;i++){var q=face.getAsJsonArray("uv").get(i).getAsJsonArray();uv[i]=new float[]{q.get(0).getAsFloat(),q.get(1).getAsFloat()};}return uv;
  }
  private static int sample(float[][] uv,float a,float b){
    float[] weights={(1-a)*(1-b),a*(1-b),a*b,(1-a)*b};float u=0,v=0;
    for(int i=0;i<4;i++){u+=uv[i][0]*weights[i];v+=uv[i][1]*weights[i];}
    int x=(int)Math.floor(u*2),y=(int)Math.floor(v*2);check(x>=0&&x<128&&y>=0&&y<64,"actual sampled quad UV left canonical atlas");return y*128+x;
  }
  private static boolean headRegion(String region){return region.startsWith("stem_")||region.startsWith("branch_");}
  private static void geometry()throws Exception{
    int boxes=0,headParts=0,wings=0,haloBars=0,stripeFaces=0,wingFaces=0;
    var parts=new HashSet<String>();
    for(var e:model("chao_hero_run_proto066").getAsJsonArray("bones")){
      var bone=e.getAsJsonObject();String boneName=bone.get("name").getAsString();
      for(var m:bone.getAsJsonArray("meshes")){
        var mesh=m.getAsJsonObject();String name=mesh.get("name").getAsString();
        boolean skull=boneName.equals("skull__cube_0"),tail=boneName.startsWith("tail__");
        boolean added=mesh.has("hero_run_box_066");String region=added?mesh.get("hero_run_region_066").getAsString():"";
        if(added){boxes++;check(mesh.getAsJsonArray("faces").size()==6,"authored part is not a closed cuboid: "+name);}
        if(headRegion(region)){headParts++;check(parts.add(name),"duplicate head part name");}
        if(region.startsWith("wing_"))wings++;if(region.equals("halo"))haloBars++;
        int localStripeFaces=0,localCapFaces=0;
        for(var f:mesh.getAsJsonArray("faces")){
          var face=f.getAsJsonObject();float[][] coordinates=uv(face);var stripeColumns=new HashSet<Integer>();var wingColumns=new HashSet<Integer>();
          if(added)for(var q:coordinates)check(q[0]>=0&&q[0]*2<=128&&q[1]>=0&&q[1]*2<=64,"new face corner escaped original atlas boundary");
          boolean isStripe=false,isCap=false;
          for(int i=0;i<8;i++)for(int j=0;j<8;j++){
            // Inherited question/dizzy UVs extend beyond the source atlas at
            // y0..6. They do not overlap the new tile. Canonical skull/tail
            // and each new closed face are sampled at actual quad interiors.
            if(!added&&!skull&&!tail)continue;
            int at=sample(coordinates,(i+.5f)/8,(j+.5f)/8),x=at%128,y=at/128;
            if(skull){SKULL.add(at);check(!HeroRunRegionPaint066.stripeTile(x,y),"new stripes entered original skull UV");}
            if(tail)TAIL.add(at);
            if(added){SURFACES.add(new Surface(name,at,region));
              if(headRegion(region)){
                if(HeroRunRegionPaint066.stripeTile(x,y)){isStripe=true;stripeColumns.add(x);}
                else{isCap=true;check(y==63&&(x==125||x==126||x==127),"head terminal sampled non-cap pigment");}
              }
              if(region.startsWith("wing_")){check(y==63&&(x==123||x==124),"wing lost proximal/distal pigment");wingColumns.add(x);}
              if(region.equals("halo"))check(HeroRunMarkerPaint056.markerUv(x,y),"halo bar sampled non-marker pigment");
            }
          }
          if(headRegion(region)){
            check(isStripe!=isCap,"head face mixes stripe tile and solid cap UVs");
            if(isStripe){localStripeFaces++;stripeFaces++;check(stripeColumns.equals(java.util.Set.of(120,121,122,123)),"actual head side lost four physical stripe lanes");}
            else localCapFaces++;
          }
          if(region.startsWith("wing_")&&wingColumns.size()==2){wingFaces++;check(wingColumns.contains(123)&&wingColumns.contains(124),"actual wing broad face lacks both pigments");}
        }
        if(headRegion(region))check(localStripeFaces==4&&localCapFaces==2,"head part is not four banded sides plus two closed caps: "+name);
      }
    }
    check(boxes==21&&headParts==6&&wings==6&&haloBars==4,"expected manageable21-box authored inventory changed");
    check(stripeFaces==24&&wingFaces==24,"painted head stripes or two-color wings lack longitudinal face coverage");
    check(SKULL.size()==384&&TAIL.size()==768,"original skull/tail six-face interior inventories changed");
    unusedSourceTileFootprints();
  }
  private static void unusedSourceTileFootprints()throws Exception{
    // Every inherited canonical face remains compatible, including dormant
    // head decorations and original expressions; no new alpha on their UVs.
    int faces=0;
    for(var b:model("chao_baby").getAsJsonArray("bones"))for(var m:b.getAsJsonObject().getAsJsonArray("meshes"))for(var f:m.getAsJsonObject().getAsJsonArray("faces")){
      float[][] points=uv(f.getAsJsonObject());float x0=999,x1=-999,y0=999,y1=-999;
      for(var q:points){x0=Math.min(x0,q[0]*2);x1=Math.max(x1,q[0]*2);y0=Math.min(y0,q[1]*2);y1=Math.max(y1,q[1]*2);}
      int left=(int)Math.floor(x0),right=Math.max(left+1,(int)Math.ceil(x1)),top=(int)Math.floor(y0),bottom=Math.max(top+1,(int)Math.ceil(y1));
      for(int x=120;x<124;x++)check(x<left||x>=right||60<top||60>=bottom,"new stripe tile touches retained canonical face");
      faces++;
    }
    check(faces>200,"canonical source face scan unexpectedly incomplete");
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
          var in=input(image,flags,natural,two,age,e[0],e[1]);var c=HeroRunRegionPaint066.colors(in);int[] painted=new int[8192];
          for(int y=0;y<64;y++)for(int x=0;x<128;x++){
            int at=y*128+x,p=rgba(image,x,y),result=HeroRunRegionPaint066.pixel(in,c,x,y,p,flags[at]);painted[at]=result;
            if(HeroRunRegionPaint066.reservedKind(x,y)!=0){check(p>>>24==0,"new swatch overwrites original art: "+name);check(result>>>24==255,"new feature transparent");swatches++;continue;}
            check(result>>>24==p>>>24,"original alpha changed: "+name);alpha++;
            if((flags[at]&64)!=0){check(result==p,"face/expression/iris changed: "+name);eyes++;}
            if((flags[at]&32)!=0){check(result==(p&0xff000000|HeroRunRegionPaint066.halo(age,e[0],e[1])&0xffffff),"halo inherited coat/shading instead of development pigment");halos++;}
          }
          for(int at:SKULL){check(painted[at]==rgba(image,at%128,at/128),"original skull paint changed: "+name);skullSamples++;}
          for(int at:TAIL){int f=flags[at];shaded(rgba(image,at%128,at/128),(f&2)!=0?in.accent():in.body(),HeroRunRegionPaint066.tailColor(c,at%128,at/128),painted[at],"canonical Hero Run tail regional pigment");tailSamples++;}
          for(var v:SURFACES){int p=painted[v.pixel];check(p>>>24==255,"actual closed-box face transparent: "+v.name);closedSamples++;
            int x=v.pixel%128;
            if(headRegion(v.region)){int y=v.pixel/128;int expected=HeroRunRegionPaint066.stripeTile(x,y)?HeroRunRegionPaint066.stripeColor(c,x,y):x==127?c.featureBody():x==125?c.featureMid():c.featureTip();check(p==expected,"actual striped head/cap face sampled wrong pigment");headInterior++;}
            if(v.region.startsWith("wing_")){check(p==(x==124?c.wing():c.wingTip()),"actual wing face sampled wrong pigment");wingInterior++;}
          }
          // The compact chest blush is flat pixels on the unchanged torso;
          // its white/inherited border and the original skull remain intact.
          for(int at:new int[]{35*128+11,37*128+9,37*128+11,39*128+12}){int f=flags[at];shaded(rgba(image,at%128,at/128),(f&2)!=0?in.accent():in.body(),HeroRunRegionPaint066.bellyColor(c,at%128,at/128),painted[at],"compact canonical chest pigment");}
          for(int at:new int[]{36*128+8,39*128+16,40*128+27}){int f=flags[at];shaded(rgba(image,at%128,at/128),(f&2)!=0?in.accent():in.body(),c.body(),painted[at],"white/inherited chest border and torso");}
        }
      }
    }
    check(coats==172,"canonical coat count changed: "+coats);
  }
  private static HeroRunRegionPaint066.Input natural(float age,float rp,float sf){
    return new HeroRunRegionPaint066.Input(true,true,age,rp,sf,HeroRunRegionPaint055.rgb(237,246,245),HeroRunRegionPaint055.rgb(61,159,228),HeroRunRegionPaint055.rgb(234,169,227),0xff000000);
  }
  private static void referenceAndScope(){
    var fresh=HeroRunRegionPaint066.colors(natural(0,0,0));var halos=new HashSet<Integer>();
    for(float[] e:ENDPOINTS){
      check(HeroRunRegionPaint066.colors(natural(0,e[0],e[1])).equals(fresh),"training overrides common newly evolved Run-first palette");halos.add(HeroRunRegionPaint066.halo(1,e[0],e[1]));
      for(float age:new float[]{0,.25f,.5f,.75f,1})for(int p:new int[]{0xff00ff00,0xff000000,0xffffffff,0xffed38ba,0x55001080}){
        var in=natural(age,e[0],e[1]);var colors=HeroRunRegionPaint066.colors(in);int result=HeroRunRegionPaint066.pixel(in,colors,117,3,p,32);
        check((result&0xffffff)==(HeroRunRegionPaint066.halo(age,e[0],e[1])&0xffffff)&&result>>>24==p>>>24,"halo depends on source body hue or alpha");
      }
    }
    check(halos.size()==9,"nine independently observed adult halo targets collapsed");
    var balanced=HeroRunRegionPaint066.colors(natural(1,0,0));var run=HeroRunRegionPaint066.colors(natural(1,-1,0));var power=HeroRunRegionPaint066.colors(natural(1,1,0));
    var fly=HeroRunRegionPaint066.colors(natural(1,0,1));var swim=HeroRunRegionPaint066.colors(natural(1,0,-1));var runFly=HeroRunRegionPaint066.colors(natural(1,-1,1));var powerFly=HeroRunRegionPaint066.colors(natural(1,1,1));
    check(red(fresh.marker())>230&&green(fresh.marker())>220&&blue(fresh.marker())<70,"newly evolved source yellow halo lost");
    check(green(balanced.marker())>red(balanced.marker())+50&&blue(balanced.marker())<50,"balanced source lime halo lost");
    check(blue(fresh.primary())>200&&green(fresh.primary())>140&&red(fresh.secondary())>220&&green(fresh.secondary())>200,"newly evolved blue/gold stripes lost");
    check(green(run.secondary())>220&&blue(run.secondary())>180&&blue(run.primary())>200,"mature Run blue/cyan striping lost");
    check(red(run.bellyRim())>230&&green(run.bellyRim())>220&&blue(run.bellyRim())<60&&blue(run.bellyCore())>red(run.bellyCore()),"mature Run yellow/cyan chest lost");
    check(red(power.marker())>230&&green(power.marker())>120&&green(power.marker())<180&&blue(power.marker())<50,"opposite Power orange halo lost");
    check(blue(power.primary())>200&&red(power.secondary())>220&&green(power.secondary())>200,"opposite Power lavender/gold stripes lost");
    check(blue(fly.marker())>red(fly.marker())+120&&red(fly.secondary())>200&&blue(fly.secondary())>200,"Fly royal-blue halo/pink stripes lost");
    check(blue(swim.primary())>200&&green(swim.primary())<60&&green(swim.secondary())>230&&blue(swim.secondary())<140,"Swim purple/green stripes lost");
    check(Math.abs(red(runFly.marker())-blue(runFly.marker()))<40&&green(runFly.marker())>170,"RunFly gray halo lost");
    check(red(powerFly.marker())>green(powerFly.marker())+35&&blue(powerFly.marker())>130,"PowerFly muted rose halo lost");
    check(red(balanced.wing())>green(balanced.wing())+60&&green(balanced.wingTip())>green(balanced.wing())+45,"gold wing root/tip direction reversed from source back view");
    check(green(fly.wing())>red(fly.wing())+70&&red(fly.wingTip())>green(fly.wingTip())+60,"Fly lost teal inner wing and pink outer wing placement");
    check(green(powerFly.wing())>red(powerFly.wing())&&red(powerFly.wingTip())>green(powerFly.wingTip())+60,"PowerFly lost muted green inner and salmon outer wing placement");
    check(HeroRunRegionPaint066.tailColor(balanced,56,31)==balanced.bellyRim()&&HeroRunRegionPaint066.tailColor(balanced,57,31)==balanced.tail(),"canonical tail lost the observed blue rim/purple core association");
    for(float age:new float[]{0,.5f,1})for(float[] e:ENDPOINTS){var c=HeroRunRegionPaint066.colors(natural(age,e[0],e[1]));check(c.body()==natural(age,e[0],e[1]).body()&&c.featureBody()==c.body(),"original white/inherited skull/body pigment replaced");
      for(int y=18;y<34;y++)for(int x=24;x<68;x++)check(HeroRunRegionPaint066.limbColor(c,x,y)==switch((y/2)%3){case 0->c.body();case 1->c.primary();default->c.secondary();},"limb bands changed their original pixel positions");
      for(int x=120;x<124;x++)check(HeroRunRegionPaint066.stripeColor(c,x,60)==switch(x){case 120,122->c.body();case 121->c.primary();default->c.secondary();},"head tile lost white/primary/white/secondary lane order");
    }
    int ruby=HeroRunRegionPaint055.rgb(214,44,93);var mono=new HeroRunRegionPaint066.Input(false,false,1,1,-1,ruby,ruby,ruby,ruby);var inherited=HeroRunRegionPaint066.colors(mono);
    check(inherited.body()==ruby&&inherited.featureBody()==ruby,"jewel/mono inherited head/body replaced by natural white");
    check(inherited.primary()!=ruby&&inherited.secondary()!=inherited.primary(),"jewel/mono striped detail lost value contrast");
    for(int scale:new int[]{1,2,4})for(int y=0;y<64*scale;y++)for(int x=0;x<128*scale;x++){
      boolean marker=HeroRunMarkerPaint056.markerUv(x/scale,y/scale),wing=y/scale==63&&(x/scale==123||x/scale==124);
      check(HeroRunRegionPaint066.paletteProtected("dynamic/hero_run066/example","baby",x,y,128*scale,64*scale)==marker,"halo palette protection differs from source faces");
      check(HeroRunRegionPaint066.paletteWing("dynamic/hero_run066/example","baby",x,y,128*scale,64*scale)==wing,"new wing palette mask differs");paletteSamples++;
      for(String path:new String[]{"dynamic/hero_run055/example","dynamic/hero_fly065/example","dynamic/hero_swim064/example","dynamic/neutral_power063/example","dynamic/hero_normal057/example","textures/entity/chao/baby.png"})check(!HeroRunRegionPaint066.paletteProtected(path,"baby",x,y,128*scale,64*scale)&&!HeroRunRegionPaint066.paletteWing(path,"baby",x,y,128*scale,64*scale),"new mask changes an old profile");
      check(!HeroRunRegionPaint066.paletteProtected("dynamic/hero_run066/example","overlay",x,y,128*scale,64*scale)&&!HeroRunRegionPaint066.paletteWing("dynamic/hero_run066/example","overlay",x,y,128*scale,64*scale),"animal overlay treated as new base marker/wing");
    }
    int exceptions=0;
    for(int y=0;y<64;y++)for(int x=0;x<128;x++){boolean expected=y==63&&x>=123||y==60&&x>=120&&x<124;check((HeroRunRegionPaint066.reservedKind(x,y)!=0)==expected,"new alpha escaped exact nine-texel footprint");if(expected)exceptions++;}
    check(exceptions==9,"nine-swatch count changed");
    check(!HeroRunRegionPaint066.paletteProtected("dynamic/hero_run066/x","baby",0,0,0,0)&&!HeroRunRegionPaint066.paletteWing("dynamic/hero_run066/x","baby",0,0,0,0),"zero-dimension palette input accepted");
  }
  private static int red(int c){return c&255;}private static int green(int c){return c>>>8&255;}private static int blue(int c){return c>>>16&255;}
  private static void continuous()throws Exception{
    int[] flags=layout();var im=ImageIO.read(ROOT.resolve("textures/entity/chao/official/coats/baby/normal_hero_0_two.png").toFile());
    for(int path=0;path<8;path++){
      int[] previous=null;
      for(int i=0;i<=100;i++){
        float t=i/100f,age=path==0?t:path==7?1-t:1,rp=path==1?2*t-1:path==3?1-t:path==5?2*t-1:-1,sf=path==2?2*t-1:path==4?1-t:path==5?1-2*t:0;
        var in=input(im,flags,true,true,age,rp,sf);var c=HeroRunRegionPaint066.colors(in);int[] now=new int[8192];
        for(int y=0;y<64;y++)for(int x=0;x<128;x++){
          int at=y*128+x;now[at]=HeroRunRegionPaint066.pixel(in,c,x,y,rgba(im,x,y),flags[at]);
          if(previous!=null)for(int k=0;k<3;k++){int d=Math.abs((now[at]>>>(k*8)&255)-(previous[at]>>>(k*8)&255));maxStep=Math.max(maxStep,d);check(d<=9,"1% paint progression discontinuity: "+d);}
        }previous=now;
      }
    }
  }
  public static void main(String[] args)throws Exception{
    geometry();atlas();referenceAndScope();continuous();
    String report="HeroRun066 material: "+checks+" checks; "+coats+" real canonical coats; "+alpha+" preserved-alpha pixels; "+eyes+" exact face pixels; "+halos+" independent halo samples; "+skullSamples+" original-skull paint samples; "+swatches+" nine-texel alpha exceptions; "+closedSamples+" actual closed-box interior samples; "+headInterior+" actual longitudinal head stripe/cap samples; "+wingInterior+" actual two-region wing samples; "+tailSamples+" canonical tail regional-pigment samples; "+paletteSamples+" palette-mask samples; maximum1% RGB step="+maxStep+". Nine mature corners and common fresh Run-first palette, signed opposite Power, unchanged white/inherited skull and body, blue/gold-to-corner limb stripes and stepped chest regions, four-lane longitudinal head stripes and two-color wing faces, unchanged eyes and original alpha/shading, inherited mono/jewel hue and source-independent halo passed. Pixel policy checks, not a visual/shader/multiplayer claim.\n";
    Path out=Path.of(System.getProperty("chao.materialReport","build/reports/lionfish/hero-run066-material-results.txt"));Files.createDirectories(out.toAbsolutePath().getParent());Files.writeString(out,report);System.out.print(report);
  }
}
