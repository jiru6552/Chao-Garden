package com.chaogarden.client;

import com.google.gson.JsonParser;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import javax.imageio.ImageIO;

/** Real canonical atlases, semantic orb UVs, all inherited coats and gradual
 * Hero/Normal material paths. These are pixel checks, not a visual pass. */
public final class HeroNormalMaterialTest057 {
  private static final Path ROOT=Path.of("src/main/resources/assets/chao_garden");
  private static long checks,alpha,eyes,orbSamples,bodySamples,wingSamples,wingEdgeSamples;
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
    for(var boneElement:asset.getAsJsonArray("bones")) {
      var bone=boneElement.getAsJsonObject();String name=bone.get("name").getAsString();int tag=0;
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
    int count=0;
    for(int y=0;y<64;y++)for(int x=0;x<128;x++) {
      boolean original=(flags[y*128+x]&32)!=0,semantic=HeroRunMarkerPaint056.markerUv(x,y);
      check(original==semantic,"marker mask differs from actual six-face original UVs at "+x+","+y);
      if(semantic){count++;check((flags[y*128+x]&65)==0,"orb UV overlaps skin/eyes");}
    }
    check(count==96,"all six orb faces not covered");return flags;
  }
  private static int dominant(BufferedImage image,int[] flags,int tag){
    var counts=new HashMap<Integer,Integer>();for(int y=0;y<64;y++)for(int x=0;x<128;x++){
      int p=rgba(image,x,y);if(p>>>24!=0&&(flags[y*128+x]&tag)!=0)counts.merge(p&0xffffff,1,Integer::sum);
    }
    return counts.entrySet().stream().max(java.util.Map.Entry.comparingByValue()).map(java.util.Map.Entry::getKey).orElse(0xffffff)|0xff000000;
  }
  private static HeroNormalRegionPaint057.Input input(BufferedImage image,int[] flags,boolean natural,boolean two,float age,float rp,float sf){
    return new HeroNormalRegionPaint057.Input(natural,two,age,rp,sf,rgba(image,12,0),rgba(image,48,0),dominant(image,flags,4),dominant(image,flags,32));
  }
  private static boolean patch(int x,int y){return HeroNormalRegionPaint057.bellyUv(x,y)||HeroNormalRegionPaint057.backUv(x,y);}
  private static void atlas()throws Exception {
    int[] flags=layout();
    try(var stream=Files.list(ROOT.resolve("textures/entity/chao/official/coats/baby"))) {
      for(Path path:stream.filter(p->p.toString().endsWith(".png")).sorted().toList()) {
        String name=path.getFileName().toString();boolean natural=name.startsWith("normal_hero_0_"),two=name.contains("_two");
        var image=ImageIO.read(path.toFile());coats++;
        for(float age:new float[]{0,.5f,1})for(float sf:new float[]{-1,0,1})for(float rp:new float[]{-1,0,1}) {
          var in=input(image,flags,natural,two,age,rp,sf);var c=HeroNormalRegionPaint057.colors(in);
          for(int y=0;y<64;y++)for(int x=0;x<128;x++) {
            int at=y*128+x,p=rgba(image,x,y),f=flags[at];int result=HeroNormalRegionPaint057.pixel(in,c,x,y,p,f);
            if(HeroRunRegionPaint055.reservedKind(x,y)!=0){check(p>>>24==0,"feature swatch overwrites original "+name);check(result>>>24==255,"invisible new feature");continue;}
            check(result>>>24==p>>>24,"original alpha changed "+name);alpha++;
            if((f&64)!=0){check(result==p,"eye/expression changed "+name);eyes++;}
            if((f&32)!=0){check(result==(p&0xff000000|HeroNormalRegionPaint057.halo(age,rp,sf)&0xffffff),"Hero/Normal halo inherited coat or lost axis blend");orbSamples++;}
            else if(!patch(x,y)&&(f&4)==0&&age==0)check(result==p,"fresh Hero limb/skin pixels flattened "+name+" at "+x+","+y);
            if((f&1)!=0&&(f&(2|4|32|64))==0&&!patch(x,y)){check(result==p,"body/head pigment replaced "+name);bodySamples++;}
            if((f&4)!=0&&p>>>24!=0) {
              wingSamples++;
              check(HeroNormalRegionPaint057.wingAccentWeight(in,37)==0,"proximal wing root lost its base pigment "+name);
              check(HeroNormalRegionPaint057.wingAccentWeight(in,38)==0,"mirrored proximal wing root lost its base pigment "+name);
            }
          }
        }
      }
    }
    check(coats==172,"registered coat list changed: "+coats);
  }
  private static void markers() {
    var image=new BufferedImage(128,64,BufferedImage.TYPE_INT_ARGB);
    var sample=new HeroNormalRegionPaint057.Input(true,true,0,0,0,HeroRunRegionPaint055.rgb(237,246,245),HeroRunRegionPaint055.rgb(61,159,228),HeroRunRegionPaint055.rgb(234,169,227),0xff000000);
    var fresh=HeroNormalRegionPaint057.colors(sample);var matureHalos=new java.util.HashSet<Integer>();
    for(float sf:new float[]{-1,0,1})for(float rp:new float[]{-1,0,1}) {
      var q=new HeroNormalRegionPaint057.Input(true,true,0,rp,sf,sample.body(),sample.accent(),sample.wing(),sample.marker());
      check(HeroNormalRegionPaint057.colors(q).equals(fresh),"a training endpoint overrides the shared fresh Hero first form");
      matureHalos.add(HeroNormalRegionPaint057.halo(1,rp,sf));
      for(float age:new float[]{0,.25f,.5f,.75f,1})for(int source:new int[]{0xff00ff00,0xff000000,0xffffffff,0xffed38ba,0x55001080}) {
        var in=new HeroNormalRegionPaint057.Input(true,true,age,rp,sf,sample.body(),sample.accent(),sample.wing(),source);var c=HeroNormalRegionPaint057.colors(in);
        int p=HeroNormalRegionPaint057.pixel(in,c,117,3,source,32);
        check((p&0xffffff)==(HeroNormalRegionPaint057.halo(age,rp,sf)&0xffffff),"halo face inherited atlas tint/shade");
        check(p>>>24==source>>>24,"halo alpha changed");
      }
    }
    check(matureHalos.size()==9,"nine intended Hero halo endpoint hues collapsed");
    for(int scale:new int[]{1,2,4})for(int y=0;y<64*scale;y++)for(int x=0;x<128*scale;x++) {
      boolean expected=HeroRunMarkerPaint056.markerUv(x/scale,y/scale);
      check(HeroNormalRegionPaint057.paletteProtected("dynamic/hero_normal057/example","baby",x,y,128*scale,64*scale)==expected,"new Halo palette mask differs from original96UV islands");
      for(String path:new String[]{"dynamic/hero_run055/example","dynamic/neutral056/example","textures/entity/chao/baby.png"})
        check(!HeroNormalRegionPaint057.paletteProtected(path,"baby",x,y,128*scale,64*scale),"new Hero mask changes another profile");
      check(!HeroNormalRegionPaint057.paletteProtected("dynamic/hero_normal057/example","overlay",x,y,128*scale,64*scale),"animal overlay masked as halo");
    }
    check(!HeroNormalRegionPaint057.paletteProtected("dynamic/hero_normal057/example","baby",0,0,0,0),"zero-size palette input accepted");
    // Verified color regions: youthful pale cyan vs centered mature blue;
    // Run cyan, Power fuchsia, Fly salmon and Swim pale yellow are distinct.
    check(HeroNormalRegionPaint057.halo(0,0,0)!=HeroNormalRegionPaint057.halo(1,0,0),"independent Hero halo age development missing");
  }
  private static void regions()throws Exception {
    int[] flags=layout();int belly=0,back=0,bands=0;
    for(int y=0;y<64;y++)for(int x=0;x<128;x++) {
      int f=flags[y*128+x];
      if(patch(x,y)){check((f&1)!=0&&(f&(4|32|64))==0,"torso patch overlaps face/wing/halo or leaves skin");if(HeroNormalRegionPaint057.bellyUv(x,y))belly++;else back++;}
      if(HeroNormalRegionPaint057.limbBand(x,y)!=0&&(f&2)!=0){check((f&(4|32|64))==0,"limb rows touch protected nonlimb regions");bands++;}
    }
    check(belly==16&&back==16,"minimal canonical torso patch dimensions changed");check(bands>0,"no actual accent pixels receive Hero limb rows");
    // Markings survive low-contrast inherited/jewel coats by changing value,
    // never substituting the natural white/blue family for the inherited body.
    int ruby=HeroRunRegionPaint055.rgb(216,40,92);var i=new HeroNormalRegionPaint057.Input(false,true,1,-1,1,ruby,ruby,ruby,0xff000000);var c=HeroNormalRegionPaint057.colors(i);
    check(c.body()==ruby&&c.featureBody()==ruby,"jewel body becomes generic white");check(c.belly()!=ruby&&c.back()!=ruby&&c.featureTip()!=ruby,"inherited/jewel details vanish on equal body/accent colors");
    var mono=new HeroNormalRegionPaint057.Input(true,false,1,1,1,0xfff5f6ed,0xfff5f6ed,ruby,0xff000000);c=HeroNormalRegionPaint057.colors(mono);
    check(c.body()==mono.body()&&c.belly()!=mono.body(),"natural monocolor loses base/contrast distinction");
  }
  private static int red(int c){return c&255;}
  private static int green(int c){return c>>>8&255;}
  private static int blue(int c){return c>>>16&255;}
  private static int maximum(int c){return Math.max(red(c),Math.max(green(c),blue(c)));}
  private static int minimum(int c){return Math.min(red(c),Math.min(green(c),blue(c)));}
  private static HeroNormalRegionPaint057.Input natural(float age,float rp,float sf) {
    return new HeroNormalRegionPaint057.Input(true,true,age,rp,sf,HeroRunRegionPaint055.rgb(237,246,245),
        HeroRunRegionPaint055.rgb(61,159,228),HeroRunRegionPaint055.rgb(234,169,227),0xff000000);
  }
  private static void referencedRegions()throws Exception {
    // The user/reference distinguishes white lower stalks from colored upper
    // caps, and blue/violet Power extremities from the old broad pink rows.
    for(float age:new float[]{0,.25f,.5f,.75f,1})for(float rp:new float[]{-1,0,1})for(float sf:new float[]{-1,0,1}) {
      var in=natural(age,rp,sf);var c=HeroNormalRegionPaint057.colors(in);
      check(c.featureBody()==in.body(),"lower Hero head section no longer keeps its white base");
      check(HeroNormalRegionPaint057.wingAccentWeight(in,32)>.8f,"distal Hero wing loses its colored tip");
      check(HeroNormalRegionPaint057.wingAccentWeight(in,43)>.8f,"mirrored distal Hero wing loses its colored tip");
    }
    var balanced=HeroNormalRegionPaint057.colors(natural(1,0,0));
    var run=HeroNormalRegionPaint057.colors(natural(1,-1,0));
    var power=HeroNormalRegionPaint057.colors(natural(1,1,0));
    var fly=HeroNormalRegionPaint057.colors(natural(1,0,1));
    var swim=HeroNormalRegionPaint057.colors(natural(1,0,-1));
    check(blue(balanced.featureTip())>red(balanced.featureTip())+60&&green(balanced.featureTip())>red(balanced.featureTip())+60,"balanced Hero caps are not cyan-blue");
    check(blue(run.featureTip())>green(run.featureTip())+70,"Hero Run caps lost their blue family");
    check(blue(power.featureTip())>red(power.featureTip())+80&&red(power.featureTip())>green(power.featureTip())+20,"Hero Power cap is not violet-blue");
    check(blue(power.primary())>red(power.primary())+100&&blue(power.primary())>green(power.primary())+100,"Hero Power outer extremities are not deep blue");
    check(blue(power.secondary())>red(power.secondary())+40&&green(power.secondary())>110,"Hero Power middle rows are dominated by bright pink");
    check(red(fly.featureTip())>140&&green(fly.featureTip())>130&&blue(fly.featureTip())>180&&maximum(fly.featureTip())-minimum(fly.featureTip())<100,"Hero Fly caps lost muted lavender");
    check(green(swim.featureTip())>red(swim.featureTip())+140&&blue(swim.featureTip())>red(swim.featureTip())+120,"Hero Swim caps are not turquoise");
    check(HeroNormalRegionPaint057.wingAccentWeight(natural(1,-1,0),35)>HeroNormalRegionPaint057.wingAccentWeight(natural(1,0,0),35),"Run wing does not develop a larger colored span");
    check(HeroNormalRegionPaint057.wingAccentWeight(natural(1,0,0),35)>HeroNormalRegionPaint057.wingAccentWeight(natural(1,0,1),35),"Fly wing does not keep its broader white root");
    // Known opaque art texels and the proximal body pigment remain stable;
    // actual thin-edge UV/cutout coverage is checked separately below.
    int[] flags=layout();
    try(var paths=Files.list(ROOT.resolve("textures/entity/chao/official/coats/baby"))) {
      for(var path:paths.filter(p->p.toString().endsWith(".png")).toList()) {
        var image=ImageIO.read(path.toFile());String name=path.getFileName().toString();
        boolean isNatural=name.startsWith("normal_hero_0_"),two=name.contains("_two");
        for(float rp:new float[]{-1,0,1})for(float sf:new float[]{-1,0,1}) {
          var in=input(image,flags,isNatural,two,1,rp,sf);var c=HeroNormalRegionPaint057.colors(in);
          int source=rgba(image,37,35),root=HeroNormalRegionPaint057.pixel(in,c,37,35,source,flags[35*128+37]);
          check(source>>>24==255&&rgba(image,33,35)>>>24==255,"known canonical wing art texel is transparent "+name);
          int[] b={red(in.body()),green(in.body()),blue(in.body())},p={red(root),green(root),blue(root)};
          for(int a=0;a<3;a++)for(int k=0;k<3;k++)
            check(Math.abs(p[a]*b[k]-p[k]*b[a])<=Math.max(1,b[a]+b[k]),"wing root replaces inherited body hue "+name);
          check(root>>>24==255,"wing root alpha changed "+name);
        }
      }
    }
  }
  private static void wingEdges()throws Exception {
    var data=JsonParser.parseString(Files.readString(ROOT.resolve("models/entity/gameplay/chao_hero_normal_proto057.json"))).getAsJsonObject();
    var wings=new java.util.ArrayList<com.google.gson.JsonObject>();
    for(var element:data.getAsJsonArray("bones")) {
      var bone=element.getAsJsonObject();String name=bone.get("name").getAsString();
      if(name.equals("wing_left__cube_0")||name.equals("wing_right__cube_0"))
        wings.add(bone.getAsJsonArray("meshes").get(0).getAsJsonObject());
    }
    check(wings.size()==2,"both actual Hero wing meshes must be inspected");int[] flags=layout();
    try(var paths=Files.list(ROOT.resolve("textures/entity/chao/official/coats/baby"))) {
      for(var path:paths.filter(p->p.toString().endsWith(".png")).toList()) {
        var image=ImageIO.read(path.toFile());String name=path.getFileName().toString();
        var in=input(image,flags,name.startsWith("normal_hero_0_"),name.contains("_two"),1,-1,0);var colors=HeroNormalRegionPaint057.colors(in);
        for(var wing:wings) {
          var faces=wing.getAsJsonArray("faces");check(faces.size()==6,"new Hero wing is not a closed six-face plate");
          int partial=0,solid=0;
          for(int face=2;face<6;face++) {
            var uv=faces.get(face).getAsJsonObject().getAsJsonArray("uv");check(uv.size()==4,"wing side edge has an invalid UV quad");
            var a=uv.get(0).getAsJsonArray();var b=uv.get(1).getAsJsonArray();
            float au=a.get(0).getAsFloat(),av=a.get(1).getAsFloat(),bu=b.get(0).getAsFloat(),bv=b.get(1).getAsFloat();
            check(Math.abs(au-bu)+Math.abs(av-bv)>.01f,"constant opaque wing edge would frame the transparent art cutout");
            boolean opaque=false,transparent=false;
            for(int step=0;step<=16;step++) {
              float t=step/16f;int x=(int)Math.floor((au+(bu-au)*t)*2),y=(int)Math.floor((av+(bv-av)*t)*2);
              check(x>=32&&x<38&&y>=34&&y<38,"wing thickness samples outside its visible source artwork");
              int source=rgba(image,x,y),result=HeroNormalRegionPaint057.pixel(in,colors,x,y,source,flags[y*128+x]);wingEdgeSamples++;
              check(result>>>24==source>>>24,"wing thickness fills a source cutout "+name);
              opaque|=source>>>24!=0;transparent|=source>>>24==0;
            }
            check(opaque,"whole wing side edge unexpectedly vanished "+name);
            if(transparent)partial++;else solid++;
          }
          // The old art has one clipped corner. Its two adjoining side
          // edges must inherit that hole; opaque constant UVs drew a hollow
          // rectangular border around it in the actual client preflight.
          check(partial==2&&solid==2,"wing edges do not follow the authored clipped corner "+name+": partial="+partial+" solid="+solid);
        }
      }
    }
  }
  private static void continuous()throws Exception {
    int[] flags=layout();var im=ImageIO.read(ROOT.resolve("textures/entity/chao/official/coats/baby/normal_hero_0_two.png").toFile());
    for(int path=0;path<6;path++) {
      int[] previous=null;
      for(int i=0;i<=100;i++) {
        float t=i/100f,age=path==0?t:path==5?1-t:1,rp=path==1?2*t-1:path==3?1-t:-1,sf=path==2?2*t-1:path==4?1-t:0;
        var in=input(im,flags,true,true,age,rp,sf);var colors=HeroNormalRegionPaint057.colors(in);int[] now=new int[8192];
        for(int y=0;y<64;y++)for(int x=0;x<128;x++) {
          int at=y*128+x;now[at]=HeroNormalRegionPaint057.pixel(in,colors,x,y,rgba(im,x,y),flags[at]);
          if(previous!=null)for(int c=0;c<3;c++){int delta=Math.abs((now[at]>>>(c*8)&255)-(previous[at]>>>(c*8)&255));maxStep=Math.max(maxStep,delta);check(delta<=8,"HeroNormal paint discontinuity "+delta);}
        }
        previous=now;
      }
    }
  }
  public static void main(String[] args)throws Exception {
    atlas();markers();regions();referencedRegions();wingEdges();continuous();
    String report="HeroNormal057 material: "+checks+" checks; "+coats+" original coats; "+alpha+" preserved alpha pixels; "+eyes+" exact face pixels; "+orbSamples+" halo samples; "+bodySamples+" exact body/head pixels; "+wingSamples+" regional wing samples; "+wingEdgeSamples+" actual wing-edge alpha samples; maximum1% color step="+maxStep+". Nine independently referenced color corners, white/inherited lower stalks and wing roots, distinct upper caps, regional wing span, cutout-following thickness, shared fresh form, gradual age/axes/reversal, inherited coat contrast and profile-scoped halo protection passed. Pixel policy checks, not a visual/shader/multiplayer claim.\n";
    Path output=Path.of(System.getProperty("chao.materialReport","build/reports/lionfish/hero-normal057-material-results.txt"));Files.createDirectories(output.toAbsolutePath().getParent());Files.writeString(output,report);System.out.print(report);
  }
}
