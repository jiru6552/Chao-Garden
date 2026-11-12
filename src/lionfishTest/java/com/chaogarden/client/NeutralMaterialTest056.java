package com.chaogarden.client;

import com.google.gson.JsonParser;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import javax.imageio.ImageIO;

/** Real canonical atlases, semantic orb UVs, all inherited coats and gradual
 * first-type Neutral material paths. These are pixel checks, not a visual pass. */
public final class NeutralMaterialTest056 {
  private static final Path ROOT=Path.of("src/main/resources/assets/chao_garden");
  private static long checks,alpha,eyes,orbSamples;
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
  private static NeutralRegionPaint056.Input input(BufferedImage image,int[] flags,boolean natural,boolean two,float age,float rp,float sf){
    return new NeutralRegionPaint056.Input(natural,two,age,rp,sf,rgba(image,12,0),rgba(image,48,0),dominant(image,flags,4),dominant(image,flags,32));
  }
  private static void atlas()throws Exception {
    int[] flags=layout();
    try(var stream=Files.list(ROOT.resolve("textures/entity/chao/official/coats/baby"))) {
      for(Path path:stream.filter(p->p.toString().endsWith(".png")).sorted().toList()) {
        String name=path.getFileName().toString();boolean natural=name.startsWith("normal_neutral_0_"),two=name.contains("_two");
        var image=ImageIO.read(path.toFile());coats++;
        for(float age:new float[]{0,.5f,1})for(float sf:new float[]{-1,0,1})for(float rp:new float[]{-1,0,1}) {
          var in=input(image,flags,natural,two,age,rp,sf);var c=NeutralRegionPaint056.colors(in);
          for(int y=0;y<64;y++)for(int x=0;x<128;x++) {
            int at=y*128+x,p=rgba(image,x,y),f=flags[at];int result=NeutralRegionPaint056.pixel(in,c,x,y,p,f);
            if(HeroRunRegionPaint055.reservedKind(x,y)!=0){check(p>>>24==0,"feature swatch overwrites original "+name);check(result>>>24==255,"invisible new feature");continue;}
            check(result>>>24==p>>>24,"original alpha changed "+name);alpha++;
            if((f&64)!=0){check(result==p,"eye/expression changed "+name);eyes++;}
            if((f&32)!=0){check(result==(p&0xff000000|HeroRunMarkerPaint056.neutralAdult(age)&0xffffff),"Neutral orb inherited coat/axes");orbSamples++;}
            else if(age==0)check(result==p,"fresh Neutral state flattens original pixels "+name+" at "+x+","+y);
            if(!natural&&(f&1)!=0&&(f&(2|4|32|64))==0)check(result==p,"named/jewel body hue replaced "+name);
          }
        }
      }
    }
    check(coats==172,"registered coat list changed: "+coats);
  }
  private static void markers() {
    int blue=HeroRunRegionPaint055.rgb(70,160,230),pink=HeroRunRegionPaint055.rgb(240,145,177);
    check(HeroRunMarkerPaint056.child(1,0,0)==blue,"young Hero orb not blue");
    check(HeroRunMarkerPaint056.child(1,0,1)==pink,"developed Run Hero orb lost viewer's intended pink");
    for(int scale:new int[]{1,2,4})for(int y=0;y<64*scale;y++)for(int x=0;x<128*scale;x++) {
      boolean marker=HeroRunMarkerPaint056.markerUv(x/scale,y/scale);
      check(HeroRunMarkerPaint056.paletteProtected("dynamic/hero_run055/example","baby",false,x,y,128*scale,64*scale)==marker,"baby marker cosmetic protection differs from actual UVs");
      check(HeroRunMarkerPaint056.paletteProtected("dynamic/neutral056/example","baby",true,x,y,128*scale,64*scale)==marker,"Neutral marker cosmetic protection differs from actual UVs");
      check(!HeroRunMarkerPaint056.paletteProtected("dynamic/hero_run055/example","baby",true,x,y,128*scale,64*scale),"unrelated adult Hero brush behavior changed");
      check(!HeroRunMarkerPaint056.paletteProtected("textures/entity/chao/baby.png","baby",false,x,y,128*scale,64*scale),"ordinary legacy material changed");
      check(!HeroRunMarkerPaint056.paletteProtected("dynamic/hero_run055/example","overlay",false,x,y,128*scale,64*scale),"animal overlay palette masked as orb");
    }
    int[] previous=null;
    for(int i=0;i<=100;i++) {
      float t=i/100f;int[] now={HeroRunMarkerPaint056.child(t,0,t),HeroRunMarkerPaint056.child(1,0,t),HeroRunMarkerPaint056.child(1-t,0,1-t),HeroRunMarkerPaint056.child(0,t,t)};
      for(int path=0;path<now.length;path++) {
        // Deliberately simulate wildly different source hues/shades and alpha;
        // no face or inherited coat is allowed to tint the semantic pigment.
        for(int source:new int[]{0xff00ff00,0xff000000,0xffffffff,0xffed38ba,0x55001080}) {
          float h=path==0?t:path==1?1:path==2?1-t:0,d=path==3?t:0,r=path==2?1-t:t;
          int painted=HeroRunMarkerPaint056.paintChild(source,h,d,r);
          check((painted&0xffffff)==(now[path]&0xffffff),"Hero orb face inherited source hue");
          check(painted>>>24==source>>>24,"orb alpha changed");
        }
        if(previous!=null)for(int c=0;c<3;c++){int step=Math.abs((now[path]>>>(c*8)&255)-(previous[path]>>>(c*8)&255));maxStep=Math.max(maxStep,step);check(step<=8,"orb color pops along alignment/age path");}
      }
      previous=now;
    }
  }
  private static void continuous()throws Exception {
    int[] flags=layout();var im=ImageIO.read(ROOT.resolve("textures/entity/chao/official/coats/baby/normal_neutral_0_two.png").toFile());
    for(int path=0;path<4;path++) {
      int[] previous=null;
      for(int i=0;i<=100;i++) {
        float t=i/100f;var in=input(im,flags,true,true,path==0?t:1,path==1?2*t-1:-1,path==2?2*t-1:path==3?1-t:0);var colors=NeutralRegionPaint056.colors(in);int[] now=new int[8192];
        for(int y=0;y<64;y++)for(int x=0;x<128;x++) {
          int at=y*128+x;now[at]=NeutralRegionPaint056.pixel(in,colors,x,y,rgba(im,x,y),flags[at]);
          if(previous!=null)for(int c=0;c<3;c++){int delta=Math.abs((now[at]>>>(c*8)&255)-(previous[at]>>>(c*8)&255));maxStep=Math.max(maxStep,delta);check(delta<=8,"Neutral paint discontinuity "+delta);}
        }
        previous=now;
      }
    }
  }
  public static void main(String[] args)throws Exception {
    atlas();markers();continuous();
    String report="Neutral056/marker material: "+checks+" checks; "+coats+" original coats; "+alpha+" preserved alpha pixels; "+eyes+" exact face pixels; "+orbSamples+" Neutral orb samples; maximum1% color step="+maxStep+". Nine endpoint combinations, fresh/mid/mature, inherited hues, exact original fresh pixels and canonical blue-to-pink Hero orb passed.\n";
    Files.createDirectories(Path.of("build/reports/lionfish"));Files.writeString(Path.of("build/reports/lionfish/neutral056-material-results.txt"),report);System.out.print(report);
  }
}
