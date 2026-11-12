package com.chaogarden.client;

import com.google.gson.JsonParser;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import javax.imageio.ImageIO;

/** Actual original atlas checks through the production pure pixel policy. */
public final class HeroRunMaterialTest055 {
  private static final Path ROOT=Path.of("src/main/resources/assets/chao_garden");
  private static int checks,coats,protectedPixels,alphaPixels,maxStep;
  private static void check(boolean condition,String why){checks++;if(!condition)throw new AssertionError(why);}
  private static int rgba(BufferedImage im,int x,int y){int c=im.getRGB(x,y);return c&0xff00ff00|(c&255)<<16|c>>>16&255;}
  private static BufferedImage image(String name)throws Exception{return ImageIO.read(ROOT.resolve("textures/entity/chao/official/coats/baby/"+name+".png").toFile());}
  private static int[] layout()throws Exception {
    int[] f=new int[8192];String[] masks={"baby","baby_accent","baby_wings"};int[] tags={1,2,4};
    for(int i=0;i<3;i++){var im=ImageIO.read(ROOT.resolve("textures/entity/chao/gameplay/masks/"+masks[i]+".png").toFile());for(int y=0;y<64;y++)for(int x=0;x<128;x++)if(im.getRGB(x,y)>>>24!=0)f[y*128+x]|=tags[i];}
    var data=JsonParser.parseString(Files.readString(ROOT.resolve("models/entity/gameplay/chao_baby.json"))).getAsJsonObject();
    for(var be:data.getAsJsonArray("bones")) {
      var b=be.getAsJsonObject();String n=b.get("name").getAsString();int tag=0;
      if(n.equals("skull__cube_2")||n.equals("skull__cube_3")||n.startsWith("expression_")||n.startsWith("iris_"))tag|=64;
      if(n.startsWith("arm_")||n.startsWith("foot_"))tag|=8;if(n.startsWith("tail__"))tag|=16;if(n.equals("emotion_dot__cube_0"))tag|=32;
      if(tag==0)continue;
      for(var me:b.getAsJsonArray("meshes"))for(var fe:me.getAsJsonObject().getAsJsonArray("faces")) {
        float x0=999,y0=999,x1=-999,y1=-999;
        for(var pe:fe.getAsJsonObject().getAsJsonArray("uv")){var p=pe.getAsJsonArray();float x=p.get(0).getAsFloat()*2,y=p.get(1).getAsFloat()*2;x0=Math.min(x0,x);x1=Math.max(x1,x);y0=Math.min(y0,y);y1=Math.max(y1,y);}
        for(int y=Math.max(0,(int)Math.floor(y0));y<Math.min(64,(int)Math.ceil(y1));y++)for(int x=Math.max(0,(int)Math.floor(x0));x<Math.min(128,(int)Math.ceil(x1));x++)f[y*128+x]|=tag;
      }
    }
    return f;
  }
  private static int dominant(BufferedImage im,int[] flags,int tag){var counts=new HashMap<Integer,Integer>();for(int y=0;y<64;y++)for(int x=0;x<128;x++){int p=rgba(im,x,y);if(p>>>24!=0&&(flags[y*128+x]&tag)!=0)counts.merge(p&0xffffff,1,Integer::sum);}return counts.entrySet().stream().max(java.util.Map.Entry.comparingByValue()).map(java.util.Map.Entry::getKey).orElse(0xffffff)|0xff000000;}
  private static HeroRunRegionPaint055.Input input(BufferedImage source,BufferedImage hero,BufferedImage dark,int[] flags,boolean natural,boolean two,boolean adult,float h,float d,float r,float m,float g){return new HeroRunRegionPaint055.Input(natural,two,adult,h,d,r,m,g,rgba(source,12,0),rgba(source,48,0),dominant(source,flags,4),dominant(source,flags,32),rgba(hero,12,0),rgba(hero,48,0),rgba(dark,12,0),rgba(dark,48,0));}
  private static int paint(HeroRunRegionPaint055.Input in,HeroRunRegionPaint055.Colors c,BufferedImage s,BufferedImage h,BufferedImage d,int x,int y,int flags){int body=in.body(),accent=in.accent();if(in.natural()&&!in.adult()){body=HeroRunRegionPaint055.blend(HeroRunRegionPaint055.blend(body,in.heroBody(),in.hero()),in.darkBody(),in.dark());accent=HeroRunRegionPaint055.blend(HeroRunRegionPaint055.blend(accent,in.heroAccent(),in.hero()),in.darkAccent(),in.dark());}int ref=(flags&36)!=0?255:HeroRunRegionPaint055.maxChannel((flags&2)!=0?accent:body);return HeroRunRegionPaint055.pixel(in,c,x,y,rgba(s,x,y),rgba(h,x,y),rgba(d,x,y),flags,ref);}

  private static void atlas()throws Exception {
    int[] flags=layout();int protectedCount=0;for(int f:flags)if((f&64)!=0)protectedCount++;check(protectedCount>=64,"lost protected eye/expression regions");
    try(var paths=Files.list(ROOT.resolve("textures/entity/chao/official/coats/baby"))) {
      for(Path path:paths.filter(p->p.toString().endsWith(".png")).sorted().toList()) {
        String name=path.getFileName().toString().replace(".png","");boolean two=name.contains("_two"),natural=name.startsWith("normal_neutral_0_");String suffix=(two?"_two":"_mono")+(name.endsWith("_shiny")?"_shiny":"");
        var source=ImageIO.read(path.toFile());var hero=image("normal_hero_0"+suffix);var dark=image("normal_dark_0"+suffix);coats++;
        float[][] states={{0,0,0,0,0,0},{0,1,0,0,1,0},{0,0,0,1,1,0},{0,1,0,1,1,0},{0,.5f,0,.5f,.5f,0},{0,0,1,.5f,.5f,0},{1,1,0,0,0,0},{1,1,0,0,1,0},{1,1,0,0,1,1}};
        for(float[] a:states){var in=input(source,hero,dark,flags,natural,two,a[0]>0,a[1],a[2],a[3],a[4],a[5]);var colors=HeroRunRegionPaint055.colors(in);
          for(int y=0;y<64;y++)for(int x=0;x<128;x++){
            int p=rgba(source,x,y),f=flags[y*128+x],v=paint(in,colors,source,hero,dark,x,y,f),reserved=HeroRunRegionPaint055.reservedKind(x,y);
            if(reserved!=0){check(p>>>24==0,"reserved swatch overwrites used original alpha");check(v>>>24==255,"new feature swatch invisible");continue;}
            check(v>>>24==p>>>24,"original alpha changed "+name);alphaPixels++;
            if((f&64)!=0){check(v==p,"protected eye/expression color changed "+name);protectedPixels++;}
            if(a==states[0])check(v==p,"zero influence changes baseline "+name);
            if(!natural&&(f&1)!=0&&(f&(2|4|8|16|32|64))==0&&!(x>=10&&x<14&&y>=36&&y<40))check(v==p,"inherited body changed "+name);
          }
        }
      }
    }
    check(coats==172,"registered coat coverage changed: "+coats);
  }
  private static void continuityAndAxes()throws Exception {
    int[] flags=layout();BufferedImage s=image("normal_neutral_0_two"),h=image("normal_hero_0_two"),d=image("normal_dark_0_two");
    for(int path=0;path<3;path++){int[] previous=null;for(int i=0;i<=100;i++){float t=i/100f;var in=input(s,h,d,flags,true,true,false,path==1?t:path==2?1:0,0,path==0||path==2?t:0,1,0);var c=HeroRunRegionPaint055.colors(in);int[] now=new int[8192];for(int y=0;y<64;y++)for(int x=0;x<128;x++){int at=y*128+x;now[at]=paint(in,c,s,h,d,x,y,flags[at]);if(previous!=null)for(int channel=0;channel<3;channel++){int delta=Math.abs((now[at]>>>(channel*8)&255)-(previous[at]>>>(channel*8)&255));maxStep=Math.max(maxStep,delta);check(delta<=8,"pixel color popped along development: "+delta);}}previous=now;}}
    var data=JsonParser.parseString(Files.readString(ROOT.resolve("gradual_evolution_palettes_021.json"))).getAsJsonObject().getAsJsonObject("profiles").getAsJsonArray("hero_run");float[][][] p=new float[9][3][3];for(int i=0;i<9;i++)for(int k=0;k<3;k++)for(int v=0;v<3;v++)p[i][k][v]=data.get(i).getAsJsonObject().getAsJsonArray(new String[]{"body","accent","wings"}[k]).get(v).getAsFloat();
    var in=input(s,h,d,flags,true,true,true,1,0,0,1,1);var c=HeroRunRegionPaint055.colors(in);check(HeroRunRegionPaint055.axisColors(c,p,0,0,1,false).equals(c),"centered compatible axis changed approved colors");check(HeroRunRegionPaint055.axisColors(c,p,1,0,0,false).equals(c),"newborn fake maturity in compatible axis");
    for(float sf:new float[]{-1,1}){var a=HeroRunRegionPaint055.axisColors(c,p,sf,0,1,false);check(a.body()==c.body()&&a.featureBody()==c.featureBody()&&a.marker()==c.marker(),"compatible axis rewrote identity/body");check(a.wing()!=c.wing()||a.primary()!=c.primary(),"compatible axis visually discarded");}
    var young=HeroRunRegionPaint055.colors(input(s,h,d,flags,true,true,true,1,0,0,0,0));var old=HeroRunRegionPaint055.colors(input(s,h,d,flags,true,true,true,1,0,0,1,0));check(young.marker()!=old.marker(),"centered Run halo ignores age");check(c.marker()==young.marker(),"max Run halo does not return yellow");
  }
  public static void main(String[] args)throws Exception {atlas();continuityAndAxes();String report="Hero Run055 material: "+checks+" checks; "+coats+" original coats; "+alphaPixels+" preserved-alpha pixels; "+protectedPixels+" exact protected eye/expression pixels; maximum1% pixel change="+maxStep+"; shared pixel regions, fixed UVs, no baseline pop and compatible-axis accents passed.\n";Files.createDirectories(Path.of("build/reports/lionfish"));Files.writeString(Path.of("build/reports/lionfish/hero-run055-material-results.txt"),report);System.out.print(report);}
}
