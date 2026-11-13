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

/** Actual canonical coat pixels and .63-only semantic paint. No renderer or
 * native context is created, and these checks are not a visual inspection. */
public final class NeutralPowerMaterialTest063 {
  private static final Path ROOT=Path.of("src/main/resources/assets/chao_garden");
  private static final float[][] ENDPOINTS={{0,0},{-1,0},{1,0},{0,1},{0,-1},{-1,1},{-1,-1},{1,1},{1,-1}};
  private static long checks,alpha,eyes,orb,swatches,paletteOrb,featureUv,wingInterior,tailBaseInterior,tailTipPixels,tailRootPixels;
  private record WingSample(String face,int pixel,boolean root) {}
  private static final List<WingSample> WING_SAMPLES=new ArrayList<>();
  private static final List<Integer> TAIL_BASE_PIXELS=new ArrayList<>();
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
  private static NeutralPowerRegionPaint063.Input input(BufferedImage image,int[] flags,boolean natural,boolean two,float age,float rp,float sf) {
    return new NeutralPowerRegionPaint063.Input(natural,two,age,rp,sf,rgba(image,12,0),rgba(image,48,0),dominant(image,flags,4),dominant(image,flags,32));
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
  /** Power uses complete pale/tinted caps, not Run's longitudinal stripes.
   * Confirm all original head faces retain plain skin through the actual UVs. */
  private static void originalSkullPaintPlacement()throws Exception {
    var asset=JsonParser.parseString(Files.readString(ROOT.resolve("models/entity/gameplay/chao_neutral_power_proto063.json"))).getAsJsonObject();
    JsonObject skull=null;
    for(var b:asset.getAsJsonArray("bones"))if(b.getAsJsonObject().get("name").getAsString().equals("skull__cube_0"))
      skull=b.getAsJsonObject().getAsJsonArray("meshes").get(0).getAsJsonObject();
    check(skull!=null,"original skull missing");
    int[] flags=layout();var image=ImageIO.read(ROOT.resolve("textures/entity/chao/official/coats/baby/normal_neutral_0_two.png").toFile());
    for(float age:new float[]{0,.5f,1})for(float[] endpoint:ENDPOINTS){
      var in=input(image,flags,true,true,age,endpoint[0],endpoint[1]);var c=NeutralPowerRegionPaint063.colors(in);
      for(var f:skull.getAsJsonArray("faces")){
        float[][] uv=new float[4][];for(int i=0;i<4;i++){var q=f.getAsJsonObject().getAsJsonArray("uv").get(i).getAsJsonArray();uv[i]=new float[]{q.get(0).getAsFloat(),q.get(1).getAsFloat()};}
        for(int i=0;i<28;i++)for(int k=0;k<24;k++){
          int pixel=interpolatedPixel(uv,(i+.5f)/28,(k+.5f)/24),x=pixel%128,y=pixel/128,source=rgba(image,x,y);
          int painted=NeutralPowerRegionPaint063.pixel(in,c,x,y,source,flags[pixel]);
          int plain=NeutralPowerRegionPaint063.pixel(in,c,12,0,source,flags[pixel]);
          check(painted==plain,"Power forehead, cheek, head top or underside acquired a stripe/back/chest patch instead of its original plain body region");
        }
      }
    }
  }
  private static void pointCapUv(JsonObject mesh,boolean tip){
    for(var f:mesh.getAsJsonArray("faces"))for(var q:f.getAsJsonObject().getAsJsonArray("uv")){
      var uv=q.getAsJsonArray();check((int)Math.floor(uv.get(0).getAsFloat()*2)==(tip?126:127)&&(int)Math.floor(uv.get(1).getAsFloat()*2)==63,"Complete Power cap/body box escaped its independent semantic texel");
    }
  }
  private static void semanticAlphaScope(){
    for(int y=0;y<64;y++)for(int x=0;x<128;x++){
      int expected=y==63&&x>=124&&x<128?(x==127||x==124?1:3):0;
      check(NeutralPowerRegionPaint063.reservedKind(x,y)==expected,"Power reserved alpha escaped the four declared semantic point swatches");
    }
  }
  /** Read the unchanged torso UVs rather than accepting a back-colored front
   * swatch. Chest and back patches must cover different physical box faces. */
  private static void bodyPatchPlacement()throws Exception{
    var asset=JsonParser.parseString(Files.readString(ROOT.resolve("models/entity/gameplay/chao_neutral_power_proto063.json"))).getAsJsonObject();JsonObject body=null;
    for(var b:asset.getAsJsonArray("bones"))if(b.getAsJsonObject().get("name").getAsString().equals("body__cube_0"))body=b.getAsJsonObject().getAsJsonArray("meshes").get(0).getAsJsonObject();
    check(body!=null,"Power torso missing");int front=0,back=0,plain=0;
    for(int face=0;face<6;face++){
      float[][] uv=new float[4][];for(int i=0;i<4;i++){var q=body.getAsJsonArray("faces").get(face).getAsJsonObject().getAsJsonArray("uv").get(i).getAsJsonArray();uv[i]=new float[]{q.get(0).getAsFloat(),q.get(1).getAsFloat()};}
      for(int i=0;i<32;i++)for(int j=0;j<32;j++){
        int pixel=interpolatedPixel(uv,(i+.5f)/32,(j+.5f)/32);boolean chest=NeutralPowerRegionPaint063.bellyUv(pixel%128,pixel/128),rear=NeutralPowerRegionPaint063.backUv(pixel%128,pixel/128);
        check(!chest||face==0,"Chest patch escaped the actual front torso face");check(!rear||face==1,"Back patch escaped the actual rear torso face");check(!(chest&&rear),"Chest and back paint overlap");
        if(chest)front++;else if(rear)back++;else plain++;
      }
    }
    check(front>0&&back>0&&plain>0,"Torso needs separate inset front/back patches and a body-colored frame");
  }
  /** Derive the retained broad tail base from all six actual original quads.
   * Its skin is body-colored; only the separate distal tip retains tail color.
   * Face-interior sampling avoids inherited atlas-edge raster ambiguity. */
  private static void tailBaseLayout()throws Exception {
    var asset=JsonParser.parseString(Files.readString(ROOT.resolve("models/entity/gameplay/chao_neutral_power_proto063.json"))).getAsJsonObject();
    TAIL_BASE_PIXELS.clear();int faces=0,rootFaces=0,tipFaces=0;var unique=new java.util.HashSet<Integer>();
    for(var b:asset.getAsJsonArray("bones")){
      var bone=b.getAsJsonObject();boolean base=bone.get("name").getAsString().equals("tail__cube_1");
      for(var m:bone.getAsJsonArray("meshes")){
        var mesh=m.getAsJsonObject();String name=mesh.get("name").getAsString();
        boolean root=name.equals("power_tail_root"),tip=name.equals("power_tail_tip");
        for(var f:mesh.getAsJsonArray("faces")){
          float[][] uv=new float[4][];for(int i=0;i<4;i++){var q=f.getAsJsonObject().getAsJsonArray("uv").get(i).getAsJsonArray();uv[i]=new float[]{q.get(0).getAsFloat(),q.get(1).getAsFloat()};}
          for(int row=0;row<8;row++)for(int column=0;column<8;column++){
            float along=(column+.5f)/8,across=(row+.5f)/8,u=0,v=0;float[] weights={(1-along)*(1-across),along*(1-across),along*across,(1-along)*across};
            for(int i=0;i<4;i++){u+=uv[i][0]*weights[i];v+=uv[i][1]*weights[i];}
            int x=(int)Math.floor(u*2),y=(int)Math.floor(v*2);
            // Original question/dizzy symbol UVs can extend beyond this skin
            // atlas. This new routing check must not impose wing-only bounds
            // on those byte-exact inherited meshes. Actual tail/new parts
            // retain strict atlas checks, and raw coordinates retain identity.
            if(base||root||tip)check(x>=0&&x<128&&y>=0&&y<64,"Actual tail base/root/tip UV left the canonical atlas");
            if(base){int pixel=y*128+x;check(NeutralPowerRegionPaint063.tailBaseUv(x,y),"Actual retained tail-base face escaped its body-pigment island");TAIL_BASE_PIXELS.add(pixel);unique.add(pixel);}
            else check(!NeutralPowerRegionPaint063.tailBaseUv(x,y),"Tail-base paint leaks into a non-base model surface: "+bone.get("name")+"/"+name);
            if(root)check(x==127&&y==63,"New tail root stopped using the full body-pigment semantic texel");
            if(tip)check(x==50&&y==32,"Separate distal tail tip lost its original tail-pigment texel");
          }
          if(base)faces++;if(root)rootFaces++;if(tip)tipFaces++;
        }
      }
    }
    check(faces==6&&TAIL_BASE_PIXELS.size()==6*8*8&&unique.size()==96,"Retained tail base lost complete six-face coverage");
    check(rootFaces==6&&tipFaces==6&&!NeutralPowerRegionPaint063.tailBaseUv(50,32),"New root/distal tip was merged with the broad retained base");
  }
  /** Independent channel-ratio relation preserves source texture shade. All
   * canonical source reference channels are positive; saturation is explicit. */
  private static void sourceShadedPigment(int source,int reference,int target,int actual,String why){
    check(source>>>24==255&&actual>>>24==255,why+" lost original opacity");
    for(int channel=0;channel<3;channel++){
      int from=source>>>(channel*8)&255,ref=reference>>>(channel*8)&255,to=target>>>(channel*8)&255,got=actual>>>(channel*8)&255;
      check(ref>0,why+" source reference channel became zero");
      int numerator=from*to;
      if(numerator>=255*ref)check(got==255,why+" lost saturated source-shaded channel");
      else check(Math.abs(got*ref-numerator)<=ref*.5f+.0001f,why+" pigment/source shade differs channel="+channel+" actual="+got+" source="+from+" reference="+ref+" target="+to);
    }
  }
  private static void paintedTailSurfaces(BufferedImage image,int[] flags,int[] painted,NeutralPowerRegionPaint063.Input in,NeutralPowerRegionPaint063.Colors colors){
    for(int pixel:TAIL_BASE_PIXELS){
      int f=flags[pixel],source=rgba(image,pixel%128,pixel/128);
      check((f&HeroRunRegionPaint055.TAIL)!=0&&(f&HeroRunRegionPaint055.SKIN)!=0,"Retained broad base pixel left its actual skin/tail mask");
      int reference=(f&HeroRunRegionPaint055.ACCENT)!=0?in.accent():in.body();
      sourceShadedPigment(source,reference,colors.body(),painted[pixel],"Actual retained tail base must carry body pigment at"+pixel%128+","+pixel/128);tailBaseInterior++;
    }
    int at=32*128+50,f=flags[at];check((f&HeroRunRegionPaint055.TAIL)!=0&&!NeutralPowerRegionPaint063.tailBaseUv(50,32),"Distal tail texel lost its disjoint tail semantics");
    sourceShadedPigment(rgba(image,50,32),(f&HeroRunRegionPaint055.ACCENT)!=0?in.accent():in.body(),colors.tail(),painted[at],"Actual separate distal tail tip");tailTipPixels++;
    check(painted[63*128+127]==colors.body()&&colors.featureBody()==colors.body(),"Full new tail-root swatch no longer matches body pigment");tailRootPixels++;
    if(in.natural()&&in.twoTone())check(painted[at]!=painted[63*128+127],"Natural distal tail accent collapsed into the body-colored root");
  }
  private static boolean[] opacityMask()throws Exception {
    boolean[] mask=new boolean[8192];int meshes=0,wingFaces=0,broadFaces=0,pointRoots=0,pointTips=0,tailTips=0;WING_SAMPLES.clear();
    var asset=JsonParser.parseString(Files.readString(ROOT.resolve("models/entity/gameplay/chao_neutral_power_proto063.json"))).getAsJsonObject();
    for(var b:asset.getAsJsonArray("bones"))for(var m:b.getAsJsonObject().getAsJsonArray("meshes")) {
      var mesh=m.getAsJsonObject();if(!mesh.has("neutral_power_box_063"))continue;meshes++;
      String region=mesh.get("neutral_power_region_063").getAsString();
      boolean wing=region.startsWith("wing_"),head=b.getAsJsonObject().get("name").getAsString().equals("gradual_head_021"),tailTip=region.equals("tail_tip");
      if(tailTip)tailTips++;
      if(head){boolean tip=mesh.get("name").getAsString().endsWith("_tip");pointCapUv(mesh,tip);if(tip)pointTips++;else pointRoots++;}
      if(wing)broadFaces+=wingInteriorSamples(mesh);
      for(var f:mesh.getAsJsonArray("faces")) {
        if(wing)wingFaces++;
        float minX=999,minY=999,maxX=-999,maxY=-999;
        for(var point:f.getAsJsonObject().getAsJsonArray("uv")) {
          var uv=point.getAsJsonArray();float x=uv.get(0).getAsFloat()*2,y=uv.get(1).getAsFloat()*2;
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
    check(pointRoots==4&&pointTips==4&&tailTips==1,"Power crown/rear/horn body-cap pairs or independent tail tip are missing");
    check(broadFaces==16&&WING_SAMPLES.size()==16*40*9,"actual longitudinal wing interior coverage incomplete");
    check(wingFaces==24,"four closed six-face wing plates missing");return mask;
  }
  private static void atlas()throws Exception {
    int[] flags=layout();boolean[] opaque=opacityMask();
    check((flags[32*128+28]&10)==10,"actual palm UV is not an original accent/limb region");
    check((flags[32*128+50]&16)!=0,"actual tail-tip UV is not an original tail region");
    var reference=ImageIO.read(ROOT.resolve("textures/entity/chao/official/coats/baby/normal_neutral_0_two.png").toFile());
    float[] ages={0,.5f,1};expectedOrb=new int[ages.length][ENDPOINTS.length];
    for(int a=0;a<ages.length;a++)for(int e=0;e<ENDPOINTS.length;e++) {
      var in=input(reference,flags,true,true,ages[a],ENDPOINTS[e][0],ENDPOINTS[e][1]);
      check((flags[2*128+118]&32)!=0&&rgba(reference,118,2)>>>24==255,"reference probe is not opaque actual orb art");
      expectedOrb[a][e]=NeutralPowerRegionPaint063.pixel(in,NeutralPowerRegionPaint063.colors(in),118,2,rgba(reference,118,2),flags[2*128+118]);
    }
    try(var files=Files.list(ROOT.resolve("textures/entity/chao/official/coats/baby"))) {
      for(Path path:files.filter(p->p.toString().endsWith(".png")).sorted().toList()) {
        String name=path.getFileName().toString();boolean natural=name.startsWith("normal_neutral_0_"),two=name.contains("_two");
        var image=ImageIO.read(path.toFile());coats++;
        for(int a=0;a<ages.length;a++)for(int e=0;e<ENDPOINTS.length;e++) {
          float[] endpoint=ENDPOINTS[e];
          var in=input(image,flags,natural,two,ages[a],endpoint[0],endpoint[1]);var c=NeutralPowerRegionPaint063.colors(in);
          int[] painted=new int[8192];
          for(int y=0;y<64;y++)for(int x=0;x<128;x++) {
            int p=rgba(image,x,y),f=flags[y*128+x],result=NeutralPowerRegionPaint063.pixel(in,c,x,y,p,f);
            painted[y*128+x]=result;
            if(opaque[y*128+x]){check(result>>>24==255,"actual .63 box face sampled transparency");featureUv++;}
            if(NeutralPowerRegionPaint063.reservedKind(x,y)!=0) {
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
                int after=ChaoPaletteMaterial.protectedMarker("dynamic/neutral_power063/test","baby",true,x,y,128,64)?result
                    :ChaoPaletteMaterial.paletteEffects(result,false,0,(mode&1)!=0,(mode&2)!=0);
                check(after==expectedOrb[a][e],"negative/bright/cosmetic mode recolored actual orb");paletteOrb++;
              }
            }
            if((!natural||!two)&&(f&1)!=0&&(f&(2|4|16|32|64))==0
                &&!NeutralPowerRegionPaint063.bellyUv(x,y)&&!NeutralPowerRegionPaint063.backUv(x,y)&&!NeutralPowerRegionPaint063.footHeelUv(x,y))
              {checks++;if(result!=p)throw new AssertionError("named/mono/jewel base hue changed "+name+" at "+x+","+y);}
          }
          paintedWingInteriors(painted);
          paintedTailSurfaces(image,flags,painted,in,c);
          if(natural&&two)check(painted[63*128+125]!=painted[63*128+124],"natural wing root/distal pigments collapsed");
          // Top of the ORIGINAL fixed skull is still a body-pigment region.
          if(!natural||!two)check(NeutralPowerRegionPaint063.pixel(in,c,12,0,rgba(image,12,0),flags[12])==rgba(image,12,0),"inherited skull pigment replaced");
        }
      }
    }
    check(coats==172,"canonical coat inventory changed: "+coats);
    check(swatches==172L*3*9*4,"not exactly four .63-only alpha exceptions");
    check(wingInterior==172L*3*9*16*40*9,"not every actual coat/age/corner sampled all broad wing interiors");
    check(tailBaseInterior==172L*3*9*6*8*8&&tailTipPixels==172L*3*9&&tailRootPixels==172L*3*9,"Not every coat/age/corner sampled the original broad tail base and separate root/tip");
  }
  private static void regions()throws Exception {
    int[] flags=layout();var image=ImageIO.read(ROOT.resolve("textures/entity/chao/official/coats/baby/normal_neutral_0_two.png").toFile());
    var fresh=NeutralPowerRegionPaint063.colors(input(image,flags,true,true,0,0,0));
    for(float[] e:ENDPOINTS)check(fresh.equals(NeutralPowerRegionPaint063.colors(input(image,flags,true,true,0,e[0],e[1]))),"training prematurely recolored the newly evolved Power form");
    var balanced=NeutralPowerRegionPaint063.colors(input(image,flags,true,true,1,0,0));
    var run=NeutralPowerRegionPaint063.colors(input(image,flags,true,true,1,-1,0));
    var power=NeutralPowerRegionPaint063.colors(input(image,flags,true,true,1,1,0));
    var fly=NeutralPowerRegionPaint063.colors(input(image,flags,true,true,1,0,1));
    var runFly=NeutralPowerRegionPaint063.colors(input(image,flags,true,true,1,-1,1));
    var powerFly=NeutralPowerRegionPaint063.colors(input(image,flags,true,true,1,1,1));
    var swim=NeutralPowerRegionPaint063.colors(input(image,flags,true,true,1,0,-1));
    var runSwim=NeutralPowerRegionPaint063.colors(input(image,flags,true,true,1,-1,-1));
    var powerSwim=NeutralPowerRegionPaint063.colors(input(image,flags,true,true,1,1,-1));
    // Qualitative viewer hue assertions are deliberately broader than authored
    // RGB approximations. Actual atlas pixels below establish implementation.
    viewerHues(fresh,balanced,run,power,fly,swim,runFly,runSwim,powerFly,powerSwim);
    // Paint actual original atlas probes, rather than checking only palette records.
    for(float[] e:ENDPOINTS)for(float age:new float[]{0,.5f,1}) {
      var in=input(image,flags,true,true,age,e[0],e[1]);var c=NeutralPowerRegionPaint063.colors(in);
      int[][] probes={{12,0,c.body()},{12,37,c.belly()},{28,32,c.hand()},{54,24,c.foot()},{65,27,c.body()},{19,14,c.body()},{3,15,c.body()},{19,6,c.body()}};
      for(int[] p:probes){
        int actual=NeutralPowerRegionPaint063.pixel(in,c,p[0],p[1],rgba(image,p[0],p[1]),flags[p[1]*128+p[0]]);
        check(actual==p[2],"actual body/chest/hand/foot UV pigment differs at "+p[0]+","+p[1]+" maturity="+age+" rp="+e[0]+" sf="+e[1]+" flags="+flags[p[1]*128+p[0]]+" actual="+Integer.toHexString(actual)+" expected="+Integer.toHexString(p[2]));
      }
      // The actual torso-back pixel is a shaded accent208/227/113 rather
      // than the input's236/255/141 accent reference. Preserve that shade.
      // Cross-multiplied channels independently test the source/target ratio
      // without replacing the authored texture detail with flat palette RGB.
      int sourceBack=rgba(image,28,37),back=NeutralPowerRegionPaint063.pixel(in,c,28,37,sourceBack,flags[37*128+28]);
      check(flags[37*128+28]==3,"Back probe left its actual shaded skin/accent region");
      for(int channel=0;channel<3;channel++){
        int source=sourceBack>>>(channel*8)&255,reference=in.accent()>>>(channel*8)&255,target=c.back()>>>(channel*8)&255,actual=back>>>(channel*8)&255;
        check(source<reference,"Independent back shading probe must remain below its source accent reference");
        check(Math.abs(actual*reference-source*target)<=reference*.5f+.0001f,"Actual shaded torso-back pigment differs at28,37 maturity="+age+" rp="+e[0]+" sf="+e[1]+" channel="+channel+" actual="+actual+" source="+source+" reference="+reference+" target="+target);
      }
      if(age==0)check((back&0xffffff)==(123|200<<8|83<<16),"Fresh actual shaded back no longer matches independently measured RGB123,200,83");
    }
    var opposing=NeutralPowerRegionPaint063.colors(input(image,flags,true,true,1,.35f,-.9f));
    var balancedSf=NeutralPowerRegionPaint063.colors(input(image,flags,true,true,1,.35f,0));
    check(!opposing.equals(balancedSf),"valid Swim influence was silently clamped to balanced");
    check(!run.equals(power),"Opposite Run retained the pure Power specialization palette");
    check(!runFly.equals(powerFly)&&!runSwim.equals(powerSwim),"opposite combinations collapsed");
    var seen=new java.util.HashSet<NeutralPowerRegionPaint063.Colors>();
    for(float[] e:ENDPOINTS)seen.add(NeutralPowerRegionPaint063.colors(input(image,flags,true,true,1,e[0],e[1])));
    check(seen.size()==9,"nine observed mature complete palettes are not distinct");
    for(float[] e:ENDPOINTS) {
      var in=input(image,flags,true,true,1,e[0],e[1]);
      var end=NeutralPowerRegionPaint063.colors(in);
      var mid=NeutralPowerRegionPaint063.colors(input(image,flags,true,true,.5f,e[0],e[1]));
      for(int channel=0;channel<3;channel++) {
        int expected=Math.round(((fresh.marker()>>>(channel*8)&255)+(end.marker()>>>(channel*8)&255))*.5f);
        check(Math.abs((mid.marker()>>>(channel*8)&255)-expected)<=1,"mid-age orb is not gradual");
      }
      check(end.marker()==NeutralPowerRegionPaint063.marker(1,e[0],e[1]),"marker accessor differs from actual paint");
    }
    for(int scale:new int[]{1,2,4})for(int y=0;y<64*scale;y++)for(int x=0;x<128*scale;x++) {
      boolean semantic=HeroRunMarkerPaint056.markerUv(x/scale,y/scale);
      check(NeutralPowerRegionPaint063.paletteProtected("dynamic/neutral_power063/test","baby",x,y,128*scale,64*scale)==semantic,"palette orb guard differs from actual six-face marker UVs");
      check(!NeutralPowerRegionPaint063.paletteProtected("dynamic/neutral_swim059/test","baby",x,y,128*scale,64*scale),"old Swim material routed through .63");
      check(!NeutralPowerRegionPaint063.paletteProtected("dynamic/neutral_power063/test","overlay",x,y,128*scale,64*scale),"animal overlay masked as orb");
      boolean wing=y/scale==63&&(x/scale==124||x/scale==125);
      check(NeutralPowerRegionPaint063.paletteWing("dynamic/neutral_power063/test","baby",x,y,128*scale,64*scale)==wing,"new wing swatch cosmetic semantics differ");
      check(!NeutralPowerRegionPaint063.paletteWing("dynamic/neutral_swim059/test","baby",x,y,128*scale,64*scale),"old Swim gained new wing swatches");
      check(!NeutralPowerRegionPaint063.paletteWing("dynamic/neutral_power063/test","overlay",x,y,128*scale,64*scale),"animal overlay gained new wing swatches");
      if(x/scale>=112&&x/scale<119&&y/scale>=32&&y/scale<34){
        check(ChaoPaletteMaterial.prototypeReservedKind("dynamic/neutral_power063/test","baby",x,y,128*scale,64*scale)==0,"Power imported the earlier Run stripe tile");
        check(ChaoPaletteMaterial.prototypeReservedKind("dynamic/neutral_run062/test","baby",x,y,128*scale,64*scale)==1,"Earlier Run stripe tile semantics changed");
      }
    }
    check(NeutralPowerRegionPaint063.reservedKind(125,63)==3&&NeutralPowerRegionPaint063.reservedKind(124,63)==1,"new wing-root/underside swatches missing");
    check(HeroRunRegionPaint055.reservedKind(125,63)==0&&HeroRunRegionPaint055.reservedKind(124,63)==0,"old family swatches changed");
    check(ChaoPaletteMaterial.prototypeReservedKind("dynamic/neutral_power063/test","baby",125,63,128,64)==3,"shared palette missed wing-root swatch");
    check(ChaoPaletteMaterial.prototypeReservedKind("dynamic/neutral_fly061/test","baby",125,63,128,64)==3,"retained Fly wing-root semantics changed");
    check(!NeutralPowerRegionPaint063.paletteProtected("dynamic/neutral_fly061/test","baby",118,2,128,64),"retained Fly entered new Run orb guard");
    check(!NeutralPowerRegionPaint063.paletteWing("dynamic/neutral_fly061/test","baby",125,63,128,64),"retained Fly entered new Run wing guard");
    for(String old:new String[]{"neutral056","hero_normal057","hero_run055","hero_run054"})
      check(ChaoPaletteMaterial.prototypeReservedKind("dynamic/"+old+"/test","baby",125,63,128,64)==0,"shared palette changed old "+old+" swatch");
    check(ChaoPaletteMaterial.prototypeReservedKind("dynamic/neutral_power063/test","overlay",125,63,128,64)==0,"shared palette changed animal overlay");
    check(ChaoPaletteMaterial.protectedMarker("dynamic/neutral_power063/test","baby",true,118,2,128,64),"shared palette failed to protect new orb");
    check(!ChaoPaletteMaterial.protectedMarker("dynamic/neutral_power063/test","overlay",true,118,2,128,64),"shared palette changed animal overlay marker");
    check(fresh.equals(NeutralPowerRegionPaint063.colors(input(image,flags,true,true,Float.NaN,Float.NaN,Float.NaN))),"non-finite values leaked into material");
  }
  private static void viewerHues(NeutralPowerRegionPaint063.Colors fresh,NeutralPowerRegionPaint063.Colors balanced,
      NeutralPowerRegionPaint063.Colors run,NeutralPowerRegionPaint063.Colors power,NeutralPowerRegionPaint063.Colors fly,
      NeutralPowerRegionPaint063.Colors swim,NeutralPowerRegionPaint063.Colors runFly,NeutralPowerRegionPaint063.Colors runSwim,
      NeutralPowerRegionPaint063.Colors powerFly,NeutralPowerRegionPaint063.Colors powerSwim) {
    // Broad qualitative relationships from the actual independently operated
    // Power-first viewer, not copies of authored RGB tables or imported pixels.
    check(red(fresh.body())>230&&green(fresh.body())>100&&blue(fresh.body())<80,"fresh Power body lost orange");
    check(red(fresh.hand())>220&&green(fresh.hand())>190&&blue(fresh.hand())<90,"fresh fists lost yellow");
    check(red(fresh.foot())>220&&green(fresh.foot())>200&&blue(fresh.foot())>100,"fresh toes lost pale cream");
    check(red(fresh.marker())>230&&green(fresh.marker())>150&&blue(fresh.marker())<110,"fresh independent orb lost gold");
    check(red(fresh.belly())>220&&blue(fresh.belly())>100&&green(fresh.belly())<90,"fresh chest lost hot pink");
    for(var c:List.of(balanced,run,power,fly,swim,runFly,runSwim,powerFly,powerSwim)){
      check(red(c.body())>220&&blue(c.body())<90,"Power-first adult departed from the source orange/red body family");
      check(c.featureBody()==c.body(),"Lower Power head point stopped retaining its body pigment");
      check(c.featureTip()!=c.featureBody(),"Substantial upper Power cap lost its contrasting tip pigment");
      check(c.foot()==c.featureTip(),"Toe and head-cap pigment no longer share the observed specialization accent");
      check(c.back()!=c.belly()&&c.back()!=c.body(),"Distinct cool/dark torso-back patch collapsed into front or body pigment");
    }
    check(green(run.featureTip())>150&&blue(run.featureTip())<90,"Opposite Run caps lost yellow/gold");
    check(green(power.body())<90&&green(power.body())<green(balanced.body()),"Pure Power must become visibly redder than the balanced orange");
    check(blue(balanced.featureTip())>120&&red(balanced.featureTip())>210,"Balanced head/toe caps lost pale pink");
    check(blue(fly.featureTip())>140&&red(fly.featureTip())>210,"Fly head/toe caps lost pink");
    check(blue(swim.featureTip())>200&&red(swim.featureTip())>210,"Swim head/toe caps lost magenta");
    check(green(run.back())>red(run.back())&&green(run.back())>blue(run.back()),"Opposite Run back patch lost green");
    check(blue(fly.back())>red(fly.back())&&blue(swim.back())>red(swim.back()),"Fly/Swim back patches lost their cool lavender family");
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
          var c=NeutralPowerRegionPaint063.colors(in);int[] now=new int[8192];
          for(int y=0;y<64;y++)for(int x=0;x<128;x++) {
            int at=y*128+x;now[at]=NeutralPowerRegionPaint063.pixel(in,c,x,y,rgba(image,x,y),flags[at]);
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
    semanticAlphaScope();bodyPatchPlacement();originalSkullPaintPlacement();tailBaseLayout();atlas();regions();continuous();
    String report="NeutralPower063 material: "+checks+" checks; "+coats+" real canonical coats; "+alpha+" preserved alpha pixels; "+eyes+" exact facial pixels; "+orb+" independent orb samples; "+paletteOrb+" palette-protected orb samples; "+swatches+" prototype alpha-exception samples; "+featureUv+" opaque prototype-face UV samples; "+wingInterior+" actual interpolated broad-wing paint samples; "+tailBaseInterior+" actual retained tail-base body-pigment interior samples; "+tailTipPixels+" separate distal-tail pigment samples; "+tailRootPixels+" new tail-root body-swatch samples; maximum 1% RGB step="+maxStep+". Nine mature endpoints with identical fresh palette, intermediate/receding training, inherited hue/alpha, semantic complete Power caps/back/chest/wing regions, unchanged plain head skin and .63-only palette protection passed. Actual quad interiors carry root/distal pigments over each half of all 16 longitudinal faces. Pixel checks are not in-game visual verification.\n";
    Files.createDirectories(Path.of("build/reports/lionfish"));Files.writeString(Path.of("build/reports/lionfish/neutral-power063-material-results.txt"),report);System.out.print(report);
  }
}
