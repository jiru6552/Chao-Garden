package com.chaogarden.client;

import com.google.gson.JsonParser;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.List;
import javax.imageio.ImageIO;

/** Actual canonical coat pixels and .61-only semantic paint. No renderer or
 * native context is created, and these checks are not a visual inspection. */
public final class NeutralFlyMaterialTest061 {
  private static final Path ROOT=Path.of("src/main/resources/assets/chao_garden");
  private static final float[][] ENDPOINTS={{0,0},{-1,0},{1,0},{0,1},{-1,1},{1,1}};
  private static long checks,alpha,eyes,orb,swatches,paletteOrb,featureUv,wingInterior;
  private record WingSample(String face,int pixel,boolean root) {}
  private static final List<WingSample> WING_SAMPLES=new ArrayList<>();
  private static int[][] expectedOrb;
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
  private static NeutralFlyRegionPaint061.Input input(BufferedImage image,int[] flags,boolean natural,boolean two,float age,float rp,float sf) {
    return new NeutralFlyRegionPaint061.Input(natural,two,age,rp,sf,rgba(image,12,0),rgba(image,48,0),dominant(image,flags,4),dominant(image,flags,32));
  }
  /** Derive each longitudinal face from actual geometry, orient its long edge
   * from the wing-bone root, and interpolate the actual quad UVs in its interior.
   * A constant root swatch on broad faces must fail even with opaque corners. */
  private static int wingInteriorSamples(JsonObject mesh) {
    JsonArray vertices=mesh.getAsJsonArray("vertices"),faces=mesh.getAsJsonArray("faces");
    float longest=0;
    for(var f:faces)longest=Math.max(longest,longestEdge(vertices,f.getAsJsonObject().getAsJsonArray("v")));
    int broad=0;
    for(int faceNumber=0;faceNumber<faces.size();faceNumber++) {
      var face=faces.get(faceNumber).getAsJsonObject();var ids=face.getAsJsonArray("v");
      if(longestEdge(vertices,ids)<longest*.9f)continue;
      broad++;int start=0;float edge=-1;
      for(int i=0;i<4;i++) {
        float length=distanceSquared(point(vertices,ids.get(i).getAsInt()),point(vertices,ids.get((i+1)%4).getAsInt()));
        if(length>edge){edge=length;start=i;}
      }
      float[][] positions=new float[4][],uv=new float[4][];
      for(int i=0;i<4;i++) {
        int at=(start+i)%4;positions[i]=point(vertices,ids.get(at).getAsInt());
        var pair=face.getAsJsonArray("uv").get(at).getAsJsonArray();
        uv[i]=new float[]{pair.get(0).getAsFloat(),pair.get(1).getAsFloat()};
      }
      if(centerRadiusSquared(positions[0],positions[3])>centerRadiusSquared(positions[1],positions[2]))
        uv=new float[][]{uv[1],uv[0],uv[3],uv[2]};
      String label=mesh.get("name").getAsString()+" face "+faceNumber;
      for(float along:new float[]{.25f,.49f,.51f,.75f})for(float across:new float[]{.1f,.5f,.9f}) {
        int pixel=interpolatedPixel(uv,along,across);
        check(pixel==63*128+(along<.5f?125:124),"actual broad-face root/distal paint misplaced: "+label);
      }
      int root=0,distal=0;
      // Interior midpoint grid avoids shared-edge rasterization ambiguity.
      for(int i=0;i<40;i++)for(int j=0;j<9;j++) {
        float along=(i+.5f)/40,across=(j+.5f)/9;
        int pixel=interpolatedPixel(uv,along,across);
        check(pixel==63*128+(along<.5f?125:124),"wing interior sampled wrong atlas region: "+label);
        boolean isRoot=pixel%128==125;if(isRoot)root++;else distal++;
        WING_SAMPLES.add(new WingSample(label,pixel,isRoot));
      }
      check(root>=144&&root<=216&&distal>=144&&distal<=216,"root/distal colors do not cover broad wing faces: "+label);
    }
    check(broad==4,"wing ray does not have four longitudinal faces");return broad;
  }
  private static float[] point(JsonArray vertices,int id) {
    var p=vertices.get(id).getAsJsonArray();return new float[]{p.get(0).getAsFloat(),p.get(1).getAsFloat(),p.get(2).getAsFloat()};
  }
  private static float distanceSquared(float[] a,float[] b) {
    float result=0;for(int i=0;i<3;i++)result+=(a[i]-b[i])*(a[i]-b[i]);return result;
  }
  private static float centerRadiusSquared(float[] a,float[] b) {
    float result=0;for(int i=0;i<3;i++)result+=(a[i]+b[i])*(a[i]+b[i])*.25f;return result;
  }
  private static float longestEdge(JsonArray vertices,JsonArray ids) {
    float result=0;for(int i=0;i<4;i++)result=Math.max(result,distanceSquared(point(vertices,ids.get(i).getAsInt()),point(vertices,ids.get((i+1)%4).getAsInt())));return result;
  }
  private static int interpolatedPixel(float[][] uv,float along,float across) {
    float u=0,v=0;float[] weights={(1-along)*(1-across),along*(1-across),along*across,(1-along)*across};
    for(int i=0;i<4;i++){u+=uv[i][0]*weights[i];v+=uv[i][1]*weights[i];}
    int x=(int)Math.floor(u*2),y=(int)Math.floor(v*2);
    check(x>=0&&x<128&&y>=0&&y<64,"interpolated wing UV left actual atlas");return y*128+x;
  }
  private static void paintedWingInteriors(int[] painted) {
    int root=painted[63*128+125],distal=painted[63*128+124];
    for(var sample:WING_SAMPLES) {
      int p=painted[sample.pixel];checks+=2;wingInterior++;
      if(p>>>24!=255)throw new AssertionError("interpolated wing face is transparent: "+sample.face);
      if(p!=(sample.root?root:distal))throw new AssertionError("actual painted wing interior lost its root/distal pigment: "+sample.face);
    }
  }
  private static boolean[] opacityMask()throws Exception {
    boolean[] mask=new boolean[8192];int meshes=0,wingFaces=0,broadFaces=0;WING_SAMPLES.clear();
    var asset=JsonParser.parseString(Files.readString(ROOT.resolve("models/entity/gameplay/chao_neutral_fly_proto061.json"))).getAsJsonObject();
    for(var b:asset.getAsJsonArray("bones"))for(var m:b.getAsJsonObject().getAsJsonArray("meshes")) {
      var mesh=m.getAsJsonObject();if(!mesh.has("neutral_fly_box_061"))continue;meshes++;
      boolean wing=mesh.get("neutral_fly_region_061").getAsString().startsWith("wing_");
      if(wing)broadFaces+=wingInteriorSamples(mesh);
      for(var f:mesh.getAsJsonArray("faces")) {
        if(wing)wingFaces++;
        float minX=999,minY=999,maxX=-999,maxY=-999;
        for(var point:f.getAsJsonObject().getAsJsonArray("uv")) {
          var uv=point.getAsJsonArray();float x=uv.get(0).getAsFloat()*2,y=uv.get(1).getAsFloat()*2;
          minX=Math.min(minX,x);maxX=Math.max(maxX,x);minY=Math.min(minY,y);maxY=Math.max(maxY,y);
        }
        check(minX>=0&&minY>=0&&maxX<128&&maxY<64,"new box UV is out of original atlas");
        int x0=(int)Math.floor(minX),y0=(int)Math.floor(minY);
        int x1=Math.max(x0+1,(int)Math.ceil(maxX)),y1=Math.max(y0+1,(int)Math.ceil(maxY));
        for(int y=y0;y<y1;y++)for(int x=x0;x<x1;x++) {
          mask[y*128+x]=true;
          if(wing)check(y==63&&(x==124||x==125),"closed wing sampled inherited transparent artwork");
          else check(!(y==63&&(x==124||x==125)),"wing green/yellow pigment leaked into crown or torso");
        }
      }
    }
    check(meshes==17,"new opaque-box inventory changed: "+meshes);
    check(broadFaces==24&&WING_SAMPLES.size()==24*40*9,"actual longitudinal wing interior coverage incomplete");
    check(wingFaces==36,"six closed six-face wing rays missing");return mask;
  }
  private static void atlas()throws Exception {
    int[] flags=layout();boolean[] opaque=opacityMask();
    var reference=ImageIO.read(ROOT.resolve("textures/entity/chao/official/coats/baby/normal_neutral_0_two.png").toFile());
    float[] ages={0,.5f,1};expectedOrb=new int[ages.length][ENDPOINTS.length];
    for(int a=0;a<ages.length;a++)for(int e=0;e<ENDPOINTS.length;e++) {
      var in=input(reference,flags,true,true,ages[a],ENDPOINTS[e][0],ENDPOINTS[e][1]);
      check((flags[2*128+118]&32)!=0&&rgba(reference,118,2)>>>24==255,"reference probe is not opaque actual orb art");
      expectedOrb[a][e]=NeutralFlyRegionPaint061.pixel(in,NeutralFlyRegionPaint061.colors(in),118,2,rgba(reference,118,2),flags[2*128+118]);
    }
    try(var files=Files.list(ROOT.resolve("textures/entity/chao/official/coats/baby"))) {
      for(Path path:files.filter(p->p.toString().endsWith(".png")).sorted().toList()) {
        String name=path.getFileName().toString();boolean natural=name.startsWith("normal_neutral_0_"),two=name.contains("_two");
        var image=ImageIO.read(path.toFile());coats++;
        for(int a=0;a<ages.length;a++)for(int e=0;e<ENDPOINTS.length;e++) {
          float[] endpoint=ENDPOINTS[e];
          var in=input(image,flags,natural,two,ages[a],endpoint[0],endpoint[1]);var c=NeutralFlyRegionPaint061.colors(in);
          int[] painted=new int[8192];
          for(int y=0;y<64;y++)for(int x=0;x<128;x++) {
            int p=rgba(image,x,y),f=flags[y*128+x],result=NeutralFlyRegionPaint061.pixel(in,c,x,y,p,f);
            painted[y*128+x]=result;
            if(opaque[y*128+x]){check(result>>>24==255,"actual .61 box face sampled transparency");featureUv++;}
            if(NeutralFlyRegionPaint061.reservedKind(x,y)!=0) {
              check(p>>>24==0,"new swatch overwrites an inherited pixel "+name);
              check(result>>>24==255,"new feature became invisible "+name);swatches++;continue;
            }
            checks++;if(result>>>24!=p>>>24)throw new AssertionError("original source alpha changed "+name);alpha++;
            if((f&64)!=0){
              checks++;if(result!=p)throw new AssertionError("face/iris/expression pixel changed "+name);eyes++;
              check((f&7)==0,"original facial art overlaps a palette mask");
            }
            if((f&32)!=0){
              check(result==expectedOrb[a][e],"actual orb pixel changed with inherited gene/shiny source");orb++;
              for(int mode=0;mode<8;mode++) {
                int after=ChaoPaletteMaterial.protectedMarker("dynamic/neutral_fly061/test","baby",true,x,y,128,64)?result
                    :ChaoPaletteMaterial.paletteEffects(result,false,0,(mode&1)!=0,(mode&2)!=0);
                check(after==expectedOrb[a][e],"negative/bright/cosmetic mode recolored actual orb");paletteOrb++;
              }
            }
            if((!natural||!two)&&(f&1)!=0&&(f&(2|4|16|32|64))==0
                &&!NeutralFlyRegionPaint061.bellyUv(x,y))
              {checks++;if(result!=p)throw new AssertionError("named/mono/jewel base hue changed "+name+" at "+x+","+y);}
          }
          paintedWingInteriors(painted);
          if(natural&&two)check(painted[63*128+125]!=painted[63*128+124],"natural wing root/distal pigments collapsed");
          // Top of the ORIGINAL fixed skull is still a body-pigment region.
          if(!natural||!two)check(NeutralFlyRegionPaint061.pixel(in,c,12,0,rgba(image,12,0),flags[12])==rgba(image,12,0),"inherited skull pigment replaced");
        }
      }
    }
    check(coats==172,"canonical coat inventory changed: "+coats);
    check(swatches==172L*3*6*4,"not exactly four .61-only reserved texels");
    check(wingInterior==172L*3*6*24*40*9,"not every actual coat/age/corner sampled all broad wing interiors");
  }
  private static void regions()throws Exception {
    int[] flags=layout();var image=ImageIO.read(ROOT.resolve("textures/entity/chao/official/coats/baby/normal_neutral_0_two.png").toFile());
    var fresh=NeutralFlyRegionPaint061.colors(input(image,flags,true,true,0,0,0));
    for(float[] e:ENDPOINTS)check(fresh.equals(NeutralFlyRegionPaint061.colors(input(image,flags,true,true,0,e[0],e[1]))),"training prematurely recolored the newly evolved Fly form");
    var balanced=NeutralFlyRegionPaint061.colors(input(image,flags,true,true,1,0,0));
    var run=NeutralFlyRegionPaint061.colors(input(image,flags,true,true,1,-1,0));
    var power=NeutralFlyRegionPaint061.colors(input(image,flags,true,true,1,1,0));
    var fly=NeutralFlyRegionPaint061.colors(input(image,flags,true,true,1,0,1));
    var runFly=NeutralFlyRegionPaint061.colors(input(image,flags,true,true,1,-1,1));
    var powerFly=NeutralFlyRegionPaint061.colors(input(image,flags,true,true,1,1,1));
    // Qualitative viewer hue assertions are deliberately broader than authored
    // RGB approximations. Actual atlas pixels below establish implementation.
    viewerHues(fresh,balanced,run,power,fly,runFly,powerFly);
    // Paint actual original atlas probes, rather than checking only palette records.
    for(float[] e:ENDPOINTS)for(float age:new float[]{0,.5f,1}) {
      var in=input(image,flags,true,true,age,e[0],e[1]);var c=NeutralFlyRegionPaint061.colors(in);
      int[][] probes={{12,0,c.body()},{12,37,c.belly()},{28,32,c.hand()},{54,24,c.foot()}};
      for(int[] p:probes)check(NeutralFlyRegionPaint061.pixel(in,c,p[0],p[1],rgba(image,p[0],p[1]),flags[p[1]*128+p[0]])==p[2],"actual body/chest/hand/foot UV pigment differs");
    }
    var opposing=NeutralFlyRegionPaint061.colors(input(image,flags,true,true,1,.35f,-.9f));
    var clamped=NeutralFlyRegionPaint061.colors(input(image,flags,true,true,1,.35f,0));
    check(opposing.equals(clamped),"Swim endpoint entered the Fly-first material");
    for(float[] e:ENDPOINTS) {
      var in=input(image,flags,true,true,1,e[0],e[1]);
      var end=NeutralFlyRegionPaint061.colors(in);
      var mid=NeutralFlyRegionPaint061.colors(input(image,flags,true,true,.5f,e[0],e[1]));
      for(int channel=0;channel<3;channel++) {
        int expected=Math.round(((fresh.marker()>>>(channel*8)&255)+(end.marker()>>>(channel*8)&255))*.5f);
        check(Math.abs((mid.marker()>>>(channel*8)&255)-expected)<=1,"mid-age orb is not gradual");
      }
      check(end.marker()==NeutralFlyRegionPaint061.marker(1,e[0],e[1]),"marker accessor differs from actual paint");
    }
    for(int scale:new int[]{1,2,4})for(int y=0;y<64*scale;y++)for(int x=0;x<128*scale;x++) {
      boolean semantic=HeroRunMarkerPaint056.markerUv(x/scale,y/scale);
      check(NeutralFlyRegionPaint061.paletteProtected("dynamic/neutral_fly061/test","baby",x,y,128*scale,64*scale)==semantic,"palette orb guard differs from actual six-face marker UVs");
      check(!NeutralFlyRegionPaint061.paletteProtected("dynamic/neutral_swim059/test","baby",x,y,128*scale,64*scale),"old Swim material routed through .61");
      check(!NeutralFlyRegionPaint061.paletteProtected("dynamic/neutral_fly061/test","overlay",x,y,128*scale,64*scale),"animal overlay masked as orb");
      boolean wing=y/scale==63&&(x/scale==124||x/scale==125);
      check(NeutralFlyRegionPaint061.paletteWing("dynamic/neutral_fly061/test","baby",x,y,128*scale,64*scale)==wing,"new wing swatch cosmetic semantics differ");
      check(!NeutralFlyRegionPaint061.paletteWing("dynamic/neutral_swim059/test","baby",x,y,128*scale,64*scale),"old Swim gained new wing swatches");
      check(!NeutralFlyRegionPaint061.paletteWing("dynamic/neutral_fly061/test","overlay",x,y,128*scale,64*scale),"animal overlay gained new wing swatches");
    }
    check(NeutralFlyRegionPaint061.reservedKind(125,63)==3&&NeutralFlyRegionPaint061.reservedKind(124,63)==1,"new wing-root/underside swatches missing");
    check(HeroRunRegionPaint055.reservedKind(125,63)==0&&HeroRunRegionPaint055.reservedKind(124,63)==0,"old family swatches changed");
    check(ChaoPaletteMaterial.prototypeReservedKind("dynamic/neutral_fly061/test","baby",125,63,128,64)==3,"shared palette missed wing-root swatch");
    for(String old:new String[]{"neutral056","hero_normal057","hero_run055","hero_run054"})
      check(ChaoPaletteMaterial.prototypeReservedKind("dynamic/"+old+"/test","baby",125,63,128,64)==0,"shared palette changed old "+old+" swatch");
    check(ChaoPaletteMaterial.prototypeReservedKind("dynamic/neutral_fly061/test","overlay",125,63,128,64)==0,"shared palette changed animal overlay");
    check(ChaoPaletteMaterial.protectedMarker("dynamic/neutral_fly061/test","baby",true,118,2,128,64),"shared palette failed to protect new orb");
    check(!ChaoPaletteMaterial.protectedMarker("dynamic/neutral_fly061/test","overlay",true,118,2,128,64),"shared palette changed animal overlay marker");
    check(fresh.equals(NeutralFlyRegionPaint061.colors(input(image,flags,true,true,Float.NaN,Float.NaN,Float.NaN))),"non-finite values leaked into material");
  }
  private static void viewerHues(NeutralFlyRegionPaint061.Colors fresh,NeutralFlyRegionPaint061.Colors balanced,
      NeutralFlyRegionPaint061.Colors run,NeutralFlyRegionPaint061.Colors power,NeutralFlyRegionPaint061.Colors fly,
      NeutralFlyRegionPaint061.Colors runFly,NeutralFlyRegionPaint061.Colors powerFly) {
    check(red(fresh.body())>200&&blue(fresh.body())>210&&green(fresh.body())<190,"fresh body lost lilac/pink");
    check(blue(fresh.hand())>200&&red(fresh.hand())<130,"fresh hand/crest ends lost blue");
    check(red(fresh.foot())>220&&green(fresh.foot())>130&&blue(fresh.foot())<100,"fresh feet lost gold");
    check(green(fresh.wing())>180&&blue(fresh.wing())>170&&red(fresh.wing())<80,"fresh wing roots lost cyan");
    check(green(fresh.wingTip())>210&&red(fresh.wingTip())<100&&blue(fresh.wingTip())<100,"fresh wing ends lost green");
    check(red(fresh.marker())>230&&green(fresh.marker())>130&&blue(fresh.marker())>100,"fresh orb lost peach/salmon");
    check(red(balanced.body())>200&&blue(balanced.body())>210,"mature balanced body lost pink/lilac");
    check(red(balanced.wing())>220&&blue(balanced.wing())>160&&blue(balanced.wingTip())<80,"balanced wings lost pink-to-orange");
    check(red(run.body())>green(run.body())&&blue(run.body())>170,"Run body lost peach-pink");
    check(green(run.wing())>210&&blue(run.wing())>180&&green(run.wingTip())>210,"Run wings lost green/cyan");
    check(red(power.body())>230&&blue(power.body())>150&&green(power.body())<100,"Power body lost hot pink/magenta");
    check(red(power.foot())>230&&green(power.foot())<100&&blue(power.foot())<60,"Power chest/feet lost red");
    check(red(power.wing())>230&&green(power.wing())>100&&blue(power.wing())<110,"Power wings lost orange");
    check(blue(fly.body())>red(fly.body())&&blue(fly.body())>green(fly.body()),"Fly body lost lavender");
    check(red(fly.wing())>230&&green(fly.wing())>230&&blue(fly.wing())>190,"Fly wing roots lost white/cream");
    check(red(fly.wingTip())>220&&green(fly.wingTip())>200&&blue(fly.wingTip())<90,"Fly wing ends lost yellow");
    check(red(fly.marker())>230&&green(fly.marker())>210&&blue(fly.marker())<90,"Fly orb lost gold/yellow");
    check(green(runFly.wing())>200&&blue(runFly.wing())>180&&red(runFly.wing())<100,"Run+Fly wing roots lost cyan");
    check(red(runFly.wingTip())>160&&green(runFly.wingTip())>190&&blue(runFly.wingTip())<130,"Run+Fly wing ends lost chartreuse");
    check(red(powerFly.body())>210&&blue(powerFly.body())>180&&green(powerFly.body())<150,"Power+Fly body lost magenta");
    check(red(powerFly.foot())>230&&green(powerFly.foot())<160&&blue(powerFly.foot())<80,"Power+Fly feet lost orange-red");
    check(red(powerFly.wing())>220&&green(powerFly.wing())>190&&blue(powerFly.wingTip())<120,"Power+Fly wings lost warm gold/orange");
  }
  private static void continuous()throws Exception {
    int[] flags=layout();
    String[] inventory={"normal_neutral_0_two","normal_neutral_0_two_shiny","black_mono","white_two","red_two","blue_mono_shiny","yellow_two","jewel_amethyst_two","jewel_gold_mono_shiny"};
    for(String coat:inventory) {
      var image=ImageIO.read(ROOT.resolve("textures/entity/chao/official/coats/baby/"+coat+".png").toFile());
      boolean natural=coat.startsWith("normal_neutral_0_"),two=coat.contains("_two");
      // Age, both training axes, four diagonals, and reversed specialization.
      for(int path=0;path<9;path++) {
        int[] previous=null;
        for(int i=0;i<=100;i++) {
          float t=i/100f;
          float age=path==0?t:1;
          float rp=switch(path){case 1->2*t-1;case 3,4->-t;case 5,6->t;case 7->1-t;case 8->t-1;default->0;};
          float sf=switch(path){case 2,3,5,8->t;case 4,6,7->1-t;default->0;};
          var in=input(image,flags,natural,two,age,rp,sf);
          var c=NeutralFlyRegionPaint061.colors(in);int[] now=new int[8192];
          for(int y=0;y<64;y++)for(int x=0;x<128;x++) {
            int at=y*128+x;now[at]=NeutralFlyRegionPaint061.pixel(in,c,x,y,rgba(image,x,y),flags[at]);
            if(previous!=null)for(int channel=0;channel<3;channel++) {
              int step=Math.abs((now[at]>>>(channel*8)&255)-(previous[at]>>>(channel*8)&255));
              maxStep=Math.max(maxStep,step);checks++;
              if(step>8)throw new AssertionError("1% material path jumped "+step+" in "+coat+" at "+x+","+y);
            }
          }
          previous=now;
        }
      }
    }
  }
  private static int red(int p){return p&255;}
  private static int green(int p){return p>>>8&255;}
  private static int blue(int p){return p>>>16&255;}
  public static void main(String[] args)throws Exception {
    atlas();regions();continuous();
    String report="NeutralFly061 material: "+checks+" checks; "+coats+" real canonical coats; "+alpha+" preserved alpha pixels; "+eyes+" exact facial pixels; "+orb+" independent orb samples; "+paletteOrb+" palette-protected orb samples; "+swatches+" four-swatch samples; "+featureUv+" opaque prototype-face UV samples; "+wingInterior+" actual interpolated broad-wing paint samples; maximum 1% RGB step="+maxStep+". Six mature endpoints with identical fresh palette, intermediate/receding training, inherited hue/alpha, semantic crown/wing regions, .61-only palette protection passed. Actual quad interiors carry root/distal pigments over each half of all24 longitudinal faces. Pixel checks are not in-game visual verification.\n";
    Files.createDirectories(Path.of("build/reports/lionfish"));Files.writeString(Path.of("build/reports/lionfish/neutral-fly061-material-results.txt"),report);System.out.print(report);
  }
}
