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

/** Actual canonical coat pixels and .62-only semantic paint. No renderer or
 * native context is created, and these checks are not a visual inspection. */
public final class NeutralRunMaterialTest062 {
  private static final Path ROOT=Path.of("src/main/resources/assets/chao_garden");
  private static final float[][] ENDPOINTS={{0,0},{-1,0},{1,0},{0,1},{0,-1},{-1,1},{-1,-1},{1,1},{1,-1}};
  private static long checks,alpha,eyes,orb,swatches,paletteOrb,featureUv,wingInterior,crestInterior;
  private record WingSample(String face,int pixel,boolean root) {}
  private static final List<WingSample> WING_SAMPLES=new ArrayList<>();
  private record StripeSample(String face,int pixel,boolean marking) {}
  private static final List<StripeSample> CREST_SAMPLES=new ArrayList<>();
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
  private static NeutralRunRegionPaint062.Input input(BufferedImage image,int[] flags,boolean natural,boolean two,float age,float rp,float sf) {
    return new NeutralRunRegionPaint062.Input(natural,two,age,rp,sf,rgba(image,12,0),rgba(image,48,0),dominant(image,flags,4),dominant(image,flags,32));
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
    check(broad==4,"wing plate does not have four longitudinal faces");return broad;
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
  /** Read actual central crest top quads. Three width lanes must stay constant
   * at every longitudinal sample; a transverse band or point swatch must fail. */
  private static void crestInteriorSamples(JsonObject mesh) {
    var vertices=mesh.getAsJsonArray("vertices");
    var face=mesh.getAsJsonArray("faces").get(4).getAsJsonObject();
    var ids=face.getAsJsonArray("v");int start=0;float longest=-1;
    for(int i=0;i<4;i++) {
      float length=distanceSquared(point(vertices,ids.get(i).getAsInt()),point(vertices,ids.get((i+1)%4).getAsInt()));
      if(length>longest){longest=length;start=i;}
    }
    float[][] uv=new float[4][];
    for(int i=0;i<4;i++) {
      var pair=face.getAsJsonArray("uv").get((start+i)%4).getAsJsonArray();
      uv[i]=new float[]{pair.get(0).getAsFloat(),pair.get(1).getAsFloat()};
    }
    int markings=0,body=0;
    for(int length=0;length<16;length++)for(int width=0;width<35;width++) {
      float along=(length+.5f)/16,across=(width+.5f)/35;
      int pixel=interpolatedPixel(uv,along,across),x=pixel%128,y=pixel/128;
      boolean expected=(((int)Math.floor(.05f+6.9f*across))&1)==1;
      check(NeutralRunRegionPaint062.stripeTile(x,y),"central crest top left its dedicated opaque tile");
      check(NeutralRunRegionPaint062.stripeMark(x,y)==expected,"crest stripe lanes do not follow the physical long axis");
      if(expected)markings++;else body++;
      CREST_SAMPLES.add(new StripeSample(mesh.get("name").getAsString(),pixel,expected));
    }
    check(markings>0&&body>0,"central crest top lost marking/body separation");
  }
  private static void paintedCrestInteriors(int[] painted,NeutralRunRegionPaint062.Colors c) {
    for(var sample:CREST_SAMPLES) {
      int color=painted[sample.pixel];crestInterior++;
      check(color>>>24==255,"actual interpolated central crest top is transparent");
      check(color==(sample.marking?c.featureTip():c.featureBody()),"actual central crest stripe pigment differs");
    }
  }
  /** Independently retain all existing model UV footprints, not just seven
   * protected family JSONs, before permitting fourteen new alpha pixels. */
  private static void unusedTileFootprints()throws Exception {
    int models=0;
    try(var files=Files.list(ROOT.resolve("models/entity/gameplay"))) {
      for(var path:files.filter(p->p.toString().endsWith(".json")&&!p.getFileName().toString().equals("chao_neutral_run_proto062.json")).sorted().toList()) {
        var asset=JsonParser.parseString(Files.readString(path)).getAsJsonObject();models++;
        float sx=128f/asset.get("texture_width").getAsFloat(),sy=64f/asset.get("texture_height").getAsFloat();
        for(var b:asset.getAsJsonArray("bones"))for(var m:b.getAsJsonObject().getAsJsonArray("meshes"))
          for(var f:m.getAsJsonObject().getAsJsonArray("faces")) {
            float x0=999,y0=999,x1=-999,y1=-999;
            for(var p:f.getAsJsonObject().getAsJsonArray("uv")) {
              var uv=p.getAsJsonArray();float x=uv.get(0).getAsFloat()*sx,y=uv.get(1).getAsFloat()*sy;
              x0=Math.min(x0,x);x1=Math.max(x1,x);y0=Math.min(y0,y);y1=Math.max(y1,y);
            }
            int left=(int)Math.floor(x0),right=Math.max(left+1,(int)Math.ceil(x1));
            int top=(int)Math.floor(y0),bottom=Math.max(top+1,(int)Math.ceil(y1));
            for(int y=32;y<34;y++)for(int x=112;x<119;x++)
              check(x<left||x>=right||y<top||y>=bottom,"stripe tile overlaps retained UVs: "+path.getFileName());
          }
      }
    }
    check(models==34,"retained gameplay model inventory changed: "+models);
  }
  private static void originalSkullStripePlacement()throws Exception {
    var asset=JsonParser.parseString(Files.readString(ROOT.resolve("models/entity/gameplay/chao_neutral_run_proto062.json"))).getAsJsonObject();
    JsonObject skull=null;
    for(var b:asset.getAsJsonArray("bones"))if(b.getAsJsonObject().get("name").getAsString().equals("skull__cube_0"))
      skull=b.getAsJsonObject().getAsJsonArray("meshes").get(0).getAsJsonObject();
    check(skull!=null,"original skull missing");
    int topMarks=0,topBody=0;boolean[] topLanes=new boolean[24];
    for(int faceIndex:new int[]{0,2,3,4}) {
      var face=skull.getAsJsonArray("faces").get(faceIndex).getAsJsonObject();float[][] uv=new float[4][];
      for(int i=0;i<4;i++){var p=face.getAsJsonArray("uv").get(i).getAsJsonArray();uv[i]=new float[]{p.get(0).getAsFloat(),p.get(1).getAsFloat()};}
      for(int i=0;i<28;i++)for(int k=0;k<24;k++) {
        int pixel=interpolatedPixel(uv,(i+.5f)/28,(k+.5f)/24);boolean marking=NeutralRunRegionPaint062.headTopMarkingUv(pixel%128,pixel/128);
        if(faceIndex==4){
          if(marking)topMarks++;else topBody++;
          if(i==0)topLanes[k]=marking;
          else check(topLanes[k]==marking,"actual original skull-top stripes run across width instead of back along Z");
        } else check(!marking,"stripe paint leaked onto the original forehead or cheek face");
      }
    }
    check(topMarks>0&&topBody>0,"actual skull top lost its separated longitudinal stripes");
    int runs=0;boolean previous=false;for(boolean lane:topLanes){if(lane&&!previous)runs++;previous=lane;}
    check(runs==3,"actual original skull top does not have three body-separated longitudinal stripes");
  }
  private static boolean[] opacityMask()throws Exception {
    boolean[] mask=new boolean[8192];int meshes=0,wingFaces=0,broadFaces=0,nubs=0,tailTips=0,crests=0;WING_SAMPLES.clear();CREST_SAMPLES.clear();
    var asset=JsonParser.parseString(Files.readString(ROOT.resolve("models/entity/gameplay/chao_neutral_run_proto062.json"))).getAsJsonObject();
    for(var b:asset.getAsJsonArray("bones"))for(var m:b.getAsJsonObject().getAsJsonArray("meshes")) {
      var mesh=m.getAsJsonObject();if(!mesh.has("neutral_run_box_062"))continue;meshes++;
      String region=mesh.get("neutral_run_region_062").getAsString();
      boolean wing=region.startsWith("wing_"),nub=region.equals("front_nub"),tailTip=region.equals("tail_tip");
      if(nub)nubs++;if(tailTip)tailTips++;
      if(region.equals("crown_root")||region.equals("crown_tip")){crests++;crestInteriorSamples(mesh);}
      if(wing)broadFaces+=wingInteriorSamples(mesh);
      for(var f:mesh.getAsJsonArray("faces")) {
        if(wing)wingFaces++;
        float minX=999,minY=999,maxX=-999,maxY=-999;
        for(var point:f.getAsJsonObject().getAsJsonArray("uv")) {
          var uv=point.getAsJsonArray();float x=uv.get(0).getAsFloat()*2,y=uv.get(1).getAsFloat()*2;
          if(nub)check((int)Math.floor(x)==28&&(int)Math.floor(y)==32,"front nub lost its independent original hand pigment");
          if(tailTip)check((int)Math.floor(x)==50&&(int)Math.floor(y)==32,"tail tip lost its independent original tail pigment");
          minX=Math.min(minX,x);maxX=Math.max(maxX,x);minY=Math.min(minY,y);maxY=Math.max(maxY,y);
        }
        check(minX>=0&&minY>=0&&maxX<128&&maxY<64,"new box UV is out of original atlas");
        int x0=(int)Math.floor(minX),y0=(int)Math.floor(minY);
        int x1=Math.max(x0+1,(int)Math.ceil(maxX)),y1=Math.max(y0+1,(int)Math.ceil(maxY));
        for(int y=y0;y<y1;y++)for(int x=x0;x<x1;x++) {
          mask[y*128+x]=true;
          if(wing)check(y==63&&(x==124||x==125),"closed wing plate sampled inherited transparent artwork");
          else check(!(y==63&&(x==124||x==125)),"wing green/yellow pigment leaked into crown or torso");
        }
      }
    }
    check(meshes==19,"new opaque-box inventory changed: "+meshes);
    check(nubs==2&&tailTips==1,"independent nub/tail color regions missing");
    check(crests==2&&CREST_SAMPLES.size()==2*16*35,"central root/tip physical stripe coverage incomplete");
    check(broadFaces==16&&WING_SAMPLES.size()==16*40*9,"actual longitudinal wing interior coverage incomplete");
    check(wingFaces==24,"four closed six-face wing plates missing");return mask;
  }
  private static void atlas()throws Exception {
    int[] flags=layout();boolean[] opaque=opacityMask();
    check((flags[32*128+28]&10)==10,"actual nub UV is not an original accent/limb region");
    check((flags[32*128+50]&16)!=0,"actual tail-tip UV is not an original tail region");
    var reference=ImageIO.read(ROOT.resolve("textures/entity/chao/official/coats/baby/normal_neutral_0_two.png").toFile());
    float[] ages={0,.5f,1};expectedOrb=new int[ages.length][ENDPOINTS.length];
    for(int a=0;a<ages.length;a++)for(int e=0;e<ENDPOINTS.length;e++) {
      var in=input(reference,flags,true,true,ages[a],ENDPOINTS[e][0],ENDPOINTS[e][1]);
      check((flags[2*128+118]&32)!=0&&rgba(reference,118,2)>>>24==255,"reference probe is not opaque actual orb art");
      expectedOrb[a][e]=NeutralRunRegionPaint062.pixel(in,NeutralRunRegionPaint062.colors(in),118,2,rgba(reference,118,2),flags[2*128+118]);
    }
    try(var files=Files.list(ROOT.resolve("textures/entity/chao/official/coats/baby"))) {
      for(Path path:files.filter(p->p.toString().endsWith(".png")).sorted().toList()) {
        String name=path.getFileName().toString();boolean natural=name.startsWith("normal_neutral_0_"),two=name.contains("_two");
        var image=ImageIO.read(path.toFile());coats++;
        for(int a=0;a<ages.length;a++)for(int e=0;e<ENDPOINTS.length;e++) {
          float[] endpoint=ENDPOINTS[e];
          var in=input(image,flags,natural,two,ages[a],endpoint[0],endpoint[1]);var c=NeutralRunRegionPaint062.colors(in);
          int[] painted=new int[8192];
          for(int y=0;y<64;y++)for(int x=0;x<128;x++) {
            int p=rgba(image,x,y),f=flags[y*128+x],result=NeutralRunRegionPaint062.pixel(in,c,x,y,p,f);
            painted[y*128+x]=result;
            if(opaque[y*128+x]){check(result>>>24==255,"actual .62 box face sampled transparency");featureUv++;}
            if(NeutralRunRegionPaint062.reservedKind(x,y)!=0) {
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
                int after=ChaoPaletteMaterial.protectedMarker("dynamic/neutral_run062/test","baby",true,x,y,128,64)?result
                    :ChaoPaletteMaterial.paletteEffects(result,false,0,(mode&1)!=0,(mode&2)!=0);
                check(after==expectedOrb[a][e],"negative/bright/cosmetic mode recolored actual orb");paletteOrb++;
              }
            }
            if((!natural||!two)&&(f&1)!=0&&(f&(2|4|16|32|64))==0
                &&!NeutralRunRegionPaint062.bellyUv(x,y)&&!NeutralRunRegionPaint062.headTopMarkingUv(x,y)&&!NeutralRunRegionPaint062.footHeelUv(x,y))
              {checks++;if(result!=p)throw new AssertionError("named/mono/jewel base hue changed "+name+" at "+x+","+y);}
          }
          paintedWingInteriors(painted);
          paintedCrestInteriors(painted,c);
          if(natural&&two)check(painted[63*128+125]!=painted[63*128+124],"natural wing root/distal pigments collapsed");
          // Top of the ORIGINAL fixed skull is still a body-pigment region.
          if(!natural||!two)check(NeutralRunRegionPaint062.pixel(in,c,12,0,rgba(image,12,0),flags[12])==rgba(image,12,0),"inherited skull pigment replaced");
        }
      }
    }
    check(coats==172,"canonical coat inventory changed: "+coats);
    check(swatches==172L*3*9*18,"not exactly eighteen .62-only alpha exceptions");
    check(wingInterior==172L*3*9*16*40*9,"not every actual coat/age/corner sampled all broad wing interiors");
    check(crestInterior==172L*3*9*2*16*35,"not every coat/age/corner sampled both longitudinal stripe surfaces");
  }
  private static void regions()throws Exception {
    int[] flags=layout();var image=ImageIO.read(ROOT.resolve("textures/entity/chao/official/coats/baby/normal_neutral_0_two.png").toFile());
    var fresh=NeutralRunRegionPaint062.colors(input(image,flags,true,true,0,0,0));
    for(float[] e:ENDPOINTS)check(fresh.equals(NeutralRunRegionPaint062.colors(input(image,flags,true,true,0,e[0],e[1]))),"training prematurely recolored the newly evolved Run form");
    var balanced=NeutralRunRegionPaint062.colors(input(image,flags,true,true,1,0,0));
    var run=NeutralRunRegionPaint062.colors(input(image,flags,true,true,1,-1,0));
    var power=NeutralRunRegionPaint062.colors(input(image,flags,true,true,1,1,0));
    var fly=NeutralRunRegionPaint062.colors(input(image,flags,true,true,1,0,1));
    var runFly=NeutralRunRegionPaint062.colors(input(image,flags,true,true,1,-1,1));
    var powerFly=NeutralRunRegionPaint062.colors(input(image,flags,true,true,1,1,1));
    var swim=NeutralRunRegionPaint062.colors(input(image,flags,true,true,1,0,-1));
    var runSwim=NeutralRunRegionPaint062.colors(input(image,flags,true,true,1,-1,-1));
    var powerSwim=NeutralRunRegionPaint062.colors(input(image,flags,true,true,1,1,-1));
    // Qualitative viewer hue assertions are deliberately broader than authored
    // RGB approximations. Actual atlas pixels below establish implementation.
    viewerHues(fresh,balanced,run,power,fly,swim,runFly,runSwim,powerFly,powerSwim);
    // Paint actual original atlas probes, rather than checking only palette records.
    for(float[] e:ENDPOINTS)for(float age:new float[]{0,.5f,1}) {
      var in=input(image,flags,true,true,age,e[0],e[1]);var c=NeutralRunRegionPaint062.colors(in);
      int[][] probes={{12,0,c.body()},{12,37,c.belly()},{28,32,c.hand()},{54,24,c.foot()},{65,27,c.hand()},{19,14,c.body()},{3,15,c.body()},{19,6,c.featureTip()}};
      for(int[] p:probes)check(NeutralRunRegionPaint062.pixel(in,c,p[0],p[1],rgba(image,p[0],p[1]),flags[p[1]*128+p[0]])==p[2],"actual body/chest/hand/foot UV pigment differs");
    }
    var opposing=NeutralRunRegionPaint062.colors(input(image,flags,true,true,1,.35f,-.9f));
    var balancedSf=NeutralRunRegionPaint062.colors(input(image,flags,true,true,1,.35f,0));
    check(!opposing.equals(balancedSf),"valid Swim influence was silently clamped to balanced");
    check(!run.equals(power),"opposite Power retained the Run specialization palette");
    check(!runFly.equals(powerFly)&&!runSwim.equals(powerSwim),"opposite combinations collapsed");
    var seen=new java.util.HashSet<Integer>();
    for(float[] e:ENDPOINTS)seen.add(NeutralRunRegionPaint062.colors(input(image,flags,true,true,1,e[0],e[1])).body());
    check(seen.size()==9,"nine observed mature body palettes are not distinct");
    for(float[] e:ENDPOINTS) {
      var in=input(image,flags,true,true,1,e[0],e[1]);
      var end=NeutralRunRegionPaint062.colors(in);
      var mid=NeutralRunRegionPaint062.colors(input(image,flags,true,true,.5f,e[0],e[1]));
      for(int channel=0;channel<3;channel++) {
        int expected=Math.round(((fresh.marker()>>>(channel*8)&255)+(end.marker()>>>(channel*8)&255))*.5f);
        check(Math.abs((mid.marker()>>>(channel*8)&255)-expected)<=1,"mid-age orb is not gradual");
      }
      check(end.marker()==NeutralRunRegionPaint062.marker(1,e[0],e[1]),"marker accessor differs from actual paint");
    }
    for(int scale:new int[]{1,2,4})for(int y=0;y<64*scale;y++)for(int x=0;x<128*scale;x++) {
      boolean semantic=HeroRunMarkerPaint056.markerUv(x/scale,y/scale);
      check(NeutralRunRegionPaint062.paletteProtected("dynamic/neutral_run062/test","baby",x,y,128*scale,64*scale)==semantic,"palette orb guard differs from actual six-face marker UVs");
      check(!NeutralRunRegionPaint062.paletteProtected("dynamic/neutral_swim059/test","baby",x,y,128*scale,64*scale),"old Swim material routed through .62");
      check(!NeutralRunRegionPaint062.paletteProtected("dynamic/neutral_run062/test","overlay",x,y,128*scale,64*scale),"animal overlay masked as orb");
      boolean wing=y/scale==63&&(x/scale==124||x/scale==125);
      check(NeutralRunRegionPaint062.paletteWing("dynamic/neutral_run062/test","baby",x,y,128*scale,64*scale)==wing,"new wing swatch cosmetic semantics differ");
      check(!NeutralRunRegionPaint062.paletteWing("dynamic/neutral_swim059/test","baby",x,y,128*scale,64*scale),"old Swim gained new wing swatches");
      check(!NeutralRunRegionPaint062.paletteWing("dynamic/neutral_run062/test","overlay",x,y,128*scale,64*scale),"animal overlay gained new wing swatches");
      if(NeutralRunRegionPaint062.stripeTile(x/scale,y/scale)) {
        check(ChaoPaletteMaterial.prototypeReservedKind("dynamic/neutral_run062/test","baby",x,y,128*scale,64*scale)==1,"shared palette missed body-semantic stripe tile");
        check(!semantic&&!wing,"central stripe tile overlaps marker/wing semantics");
        check(ChaoPaletteMaterial.prototypeReservedKind("dynamic/neutral_run062/test","overlay",x,y,128*scale,64*scale)==0,"stripe tile leaked into animal overlay");
        check(ChaoPaletteMaterial.prototypeReservedKind("dynamic/neutral_fly061/test","baby",x,y,128*scale,64*scale)==0,"new stripe tile changed prior Fly material");
      }
    }
    check(NeutralRunRegionPaint062.reservedKind(125,63)==3&&NeutralRunRegionPaint062.reservedKind(124,63)==1,"new wing-root/underside swatches missing");
    check(HeroRunRegionPaint055.reservedKind(125,63)==0&&HeroRunRegionPaint055.reservedKind(124,63)==0,"old family swatches changed");
    check(ChaoPaletteMaterial.prototypeReservedKind("dynamic/neutral_run062/test","baby",125,63,128,64)==3,"shared palette missed wing-root swatch");
    check(ChaoPaletteMaterial.prototypeReservedKind("dynamic/neutral_fly061/test","baby",125,63,128,64)==3,"retained Fly wing-root semantics changed");
    check(!NeutralRunRegionPaint062.paletteProtected("dynamic/neutral_fly061/test","baby",118,2,128,64),"retained Fly entered new Run orb guard");
    check(!NeutralRunRegionPaint062.paletteWing("dynamic/neutral_fly061/test","baby",125,63,128,64),"retained Fly entered new Run wing guard");
    for(String old:new String[]{"neutral056","hero_normal057","hero_run055","hero_run054"})
      check(ChaoPaletteMaterial.prototypeReservedKind("dynamic/"+old+"/test","baby",125,63,128,64)==0,"shared palette changed old "+old+" swatch");
    check(ChaoPaletteMaterial.prototypeReservedKind("dynamic/neutral_run062/test","overlay",125,63,128,64)==0,"shared palette changed animal overlay");
    check(ChaoPaletteMaterial.protectedMarker("dynamic/neutral_run062/test","baby",true,118,2,128,64),"shared palette failed to protect new orb");
    check(!ChaoPaletteMaterial.protectedMarker("dynamic/neutral_run062/test","overlay",true,118,2,128,64),"shared palette changed animal overlay marker");
    check(fresh.equals(NeutralRunRegionPaint062.colors(input(image,flags,true,true,Float.NaN,Float.NaN,Float.NaN))),"non-finite values leaked into material");
  }
  private static void viewerHues(NeutralRunRegionPaint062.Colors fresh,NeutralRunRegionPaint062.Colors balanced,
      NeutralRunRegionPaint062.Colors run,NeutralRunRegionPaint062.Colors power,NeutralRunRegionPaint062.Colors fly,
      NeutralRunRegionPaint062.Colors swim,NeutralRunRegionPaint062.Colors runFly,NeutralRunRegionPaint062.Colors runSwim,
      NeutralRunRegionPaint062.Colors powerFly,NeutralRunRegionPaint062.Colors powerSwim) {
    // Broad qualitative assertions from personally viewed independent originals,
    // deliberately not exact authored RGB tables or a viewer-texture import.
    check(green(fresh.body())>220&&red(fresh.body())>110&&blue(fresh.body())<60,"fresh body lost lime");
    check(green(fresh.hand())>red(fresh.hand())&&blue(fresh.hand())<60,"fresh palms lost green");
    check(red(fresh.foot())>130&&green(fresh.foot())>130&&blue(fresh.foot())<80,"fresh toe region lost gold/olive");
    check(green(fresh.marker())>210&&blue(fresh.marker())<60,"fresh independent orb lost lime");
    check(blue(balanced.body())>210&&green(balanced.body())>140&&red(balanced.body())<80,"balanced body lost cyan-blue");
    check(green(balanced.hand())>100&&green(balanced.hand())>red(balanced.hand())*2,"balanced palms lost green");
    check(blue(run.body())>220&&red(run.body())<100&&green(run.body())<90,"Run body lost cobalt");
    check(red(run.hand())<80&&green(run.hand())<80&&blue(run.hand())<70,"Run palms/belly lost near-black");
    check(red(power.body())>210&&blue(power.body())>210&&green(power.body())<140,"opposite Power body lost magenta");
    check(red(power.hand())>170&&green(power.hand())>60&&blue(power.hand())<80,"Power palms lost orange/rust");
    check(blue(power.featureTip())>200&&red(power.featureTip())<110,"Power head markings lost blue");
    check(blue(fly.body())>220&&red(fly.body())>130&&green(fly.body())>130,"Fly body lost pale periwinkle");
    check(red(fly.hand())>130&&green(fly.hand())>130&&blue(fly.hand())<80,"Fly palms lost olive/yellow");
    check(red(fly.foot())>120&&blue(fly.foot())>210&&green(fly.foot())<120,"Fly toe region lost purple");
    check(green(swim.body())>210&&blue(swim.body())>200&&red(swim.body())<80,"Swim body lost bright cyan");
    check(green(swim.hand())>170&&red(swim.hand())<90&&blue(swim.hand())<100,"Swim palms lost green");
    check(green(swim.tail())>170&&blue(swim.tail())<100,"Swim tail end lost green");
    check(blue(runFly.body())>220&&red(runFly.body())<140,"Run+Fly body lost medium periwinkle/cobalt");
    check(red(runFly.hand())>80&&green(runFly.hand())>80&&blue(runFly.hand())<80,"Run+Fly palms lost dark olive");
    check(blue(runSwim.body())>210&&green(runSwim.body())>130&&red(runSwim.body())<90,"Run+Swim body lost bright blue");
    check(green(runSwim.hand())>120&&blue(runSwim.hand())<110,"Run+Swim palms lost green");
    check(red(powerFly.body())>150&&blue(powerFly.body())>210&&green(powerFly.body())>100,"Power+Fly body lost warm lavender");
    check(red(powerFly.hand())>150&&green(powerFly.hand())>90&&blue(powerFly.hand())<80,"Power+Fly palms lost ochre");
    check(blue(powerSwim.body())>220&&red(powerSwim.body())>100&&green(powerSwim.body())>130,"Power+Swim body lost pale blue");
    check(red(powerSwim.hand())>130&&green(powerSwim.hand())>130&&blue(powerSwim.hand())<90,"Power+Swim palms lost yellow-green/olive");
    check(blue(powerSwim.foot())>210&&red(powerSwim.foot())>130&&green(powerSwim.foot())<160,"Power+Swim toes lost lavender purple");
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
          float rp=switch(path){case 1->2*t-1;case 3,4->-t;case 5,6->t;case 7->t-1;case 8->1-t;default->0;};
          float sf=switch(path){case 2->2*t-1;case 3,5->-t;case 4,6->t;case 7->1-t;case 8->t-1;default->0;};
          var in=input(image,flags,natural,two,age,rp,sf);
          var c=NeutralRunRegionPaint062.colors(in);int[] now=new int[8192];
          for(int y=0;y<64;y++)for(int x=0;x<128;x++) {
            int at=y*128+x;now[at]=NeutralRunRegionPaint062.pixel(in,c,x,y,rgba(image,x,y),flags[at]);
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
    unusedTileFootprints();originalSkullStripePlacement();atlas();regions();continuous();
    String report="NeutralRun062 material: "+checks+" checks; "+coats+" real canonical coats; "+alpha+" preserved alpha pixels; "+eyes+" exact facial pixels; "+orb+" independent orb samples; "+paletteOrb+" palette-protected orb samples; "+swatches+" prototype alpha-exception samples; "+featureUv+" opaque prototype-face UV samples; "+wingInterior+" actual interpolated broad-wing paint samples; "+crestInterior+" actual central-crest stripe paint samples; maximum 1% RGB step="+maxStep+". Nine mature endpoints with identical fresh palette, intermediate/receding training, inherited hue/alpha, semantic top-head/central-crest/wing regions, clear forehead/cheeks and .62-only palette protection passed. Actual quad interiors carry root/distal pigments over each half of all 16 longitudinal faces. Pixel checks are not in-game visual verification.\n";
    Files.createDirectories(Path.of("build/reports/lionfish"));Files.writeString(Path.of("build/reports/lionfish/neutral-run062-material-results.txt"),report);System.out.print(report);
  }
}
