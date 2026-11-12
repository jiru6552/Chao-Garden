package com.chaogarden.client;

import com.chaogarden.ChaoGarden;
import com.chaogarden.core.ChaoState;
import com.google.gson.JsonParser;
import com.mojang.blaze3d.platform.NativeImage;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterClientReloadListenersEvent;
import net.neoforged.neoforge.client.event.RenderFrameEvent;

/** Continuous family coloration on one UV layout; eye/alpha pixels stay intact. */
@EventBusSubscriber(modid=ChaoGarden.MODID,value=Dist.CLIENT)
public final class HeroRunCoatMaterial052 {
  private static final Map<String,ResourceLocation> CACHE=new LinkedHashMap<>(128,.75f,true);
  private static final Set<ResourceLocation> RETIRED=new HashSet<>();
  private static float[][][] palettes;
  private static final int[][] BABY_RUN_NEUTRAL_053={{143,229,30},{196,232,61},{71,178,40}};
  private static final int[][] BABY_RUN_HERO_053={{242,242,237},{101,157,202},{164,155,204}};
  private HeroRunCoatMaterial052(){}
  private static ResourceLocation id(String path){return ResourceLocation.fromNamespaceAndPath(ChaoGarden.MODID,path);}
  private static NativeImage read(String path)throws IOException {
    try(var input=Minecraft.getInstance().getResourceManager().open(id(path))){return NativeImage.read(input);}
  }

  static ResourceLocation texture(ChaoState state) {return texture(state,false);}
  static ResourceLocation textureOverlay(ChaoState state) {return texture(state,true);}
  private static ResourceLocation texture(ChaoState state,boolean overlay) {
    if(!HeroRunAppearance052.sample(state).adult())return babyTexture053(state,overlay);
    return adultTexture052(state,overlay);
  }

  /** The adult prototype keeps its 0.25.52 material path unchanged. */
  private static ResourceLocation adultTexture052(ChaoState state,boolean overlay) {
    String prefix=overlay?"textures/entity/chao/":"textures/entity/chao/official/coats/baby/";
    String layout=overlay?"overlay":"baby";
    var sample=HeroRunAppearance052.sample(state);
    int hero=Math.round(unit(sample.babyHero())*128),run=Math.round(unit(sample.babyRun())*128);
    int maturity=Math.round(unit(sample.maturity())*128),growth=Math.round(unit(sample.adultGrowth())*128);
    int sf=Math.round(Math.clamp(state.swimFly,-1,1)*32),power=Math.round(Math.clamp(state.runPower,0,1)*32);
    String suffix=(state.isTwoTone()?"_two":"_mono")+(state.isShiny()?"_shiny":"");
    boolean natural=state.baseColor()==0&&state.jewel()==0;
    String coat=natural?"normal_neutral_0"+suffix:ChaoRenderer.geneticCoatKey(state);
    String key=layout+":"+coat+":"+sample.adult()+":"+hero+":"+run+":"+maturity+":"+growth+":"+sf+":"+power;
    var cached=CACHE.get(key);if(cached!=null)return cached;
    NativeImage result=null;
    try(var source=read(prefix+coat+".png");
        var herobaby=read(prefix+"normal_hero_0"+suffix+".png");
        var herorun=read(prefix+"normal_hero_3"+suffix+".png");
        var mask=read("textures/entity/chao/gameplay/masks/"+layout+".png");
        var accents=read("textures/entity/chao/gameplay/masks/"+layout+"_accent.png");
        var wings=read("textures/entity/chao/gameplay/masks/"+layout+"_wings.png")) {
      result=new NativeImage(source.getWidth(),source.getHeight(),false);result.copyFrom(source);
      float h=hero/128f,r=run/128f,m=maturity/128f,g=growth/128f;
      float[][] palette=palette(sf/32f,power/32f,g,m);
      float[] shades=shades(herorun,mask,accents,wings,state.isTwoTone());
      for(int y=0;y<result.getHeight();y++)for(int x=0;x<result.getWidth();x++) {
        int mx=x*mask.getWidth()/result.getWidth(),my=y*mask.getHeight()/result.getHeight();
        boolean skin=(mask.getPixelRGBA(mx,my)>>>24)!=0,wing=(wings.getPixelRGBA(mx,my)>>>24)!=0;
        if(!natural||!skin&&!wing)continue;
        int pixel=source.getPixelRGBA(x,y);if((pixel>>>24)==0)continue;
        int kind=state.isTwoTone()&&wing?2:state.isTwoTone()&&(accents.getPixelRGBA(mx,my)>>>24)!=0?1:0;
        int hb=herobaby.getPixelRGBA(x,y),hr=herorun.getPixelRGBA(x,y);
        // The old generic Hero/Run coat has green limbs. The supplied gallery
        // verifies a white family with blue markings; correct only this
        // prototype's natural baseline, never the shared inherited coats.
        float baseShade=Math.clamp(maxChannel(hr)/shades[kind],.50f,1.25f);
        int[] first=kind==0?new int[]{242,242,237}:kind==1?new int[]{101,157,202}:new int[]{164,155,204};
        hr=rgb(first[0]*baseShade,first[1]*baseShade,first[2]*baseShade,hr>>>24);
        if(!sample.adult())pixel=blend(pixel,blend(hb,hr,r),h);
        else {
          float shade=baseShade;
          int target=rgb(palette[kind][0]*shade,palette[kind][1]*shade,palette[kind][2]*shade,hr>>>24);
          pixel=blend(hr,target,m);
        }
        result.setPixelRGBA(x,y,pixel);
      }
      // These texels are exclusively used by the prototype crown. They carry
      // its original neutral stem/tip colors at zero Hero influence and the
      // family accent at full influence, including inherited colored coats.
      if(!overlay) {
        int body=result.getPixelRGBA(result.getWidth()*12/128,0);
        int accent=result.getPixelRGBA(result.getWidth()*48/128,0);
        float family=sample.adult()?1:h*r;
        int paleBlue=rgb(148,190,219,255);
        int crestBody=body;
        int crestTip=natural&&state.isTwoTone()?blend(accent,paleBlue,family):accent;
        result.setPixelRGBA(result.getWidth()-1,result.getHeight()-1,crestBody|0xff000000);
        result.setPixelRGBA(result.getWidth()-2,result.getHeight()-1,crestTip|0xff000000);
        if(natural&&state.isTwoTone()) {
          float spec=m>0?unit(g/m):0;
          int blue=rgb(80,143,194,255),yellow=rgb(236,224,109,255),violet=rgb(137,116,181,255),white=rgb(242,242,237,255);
          int legacyHand=source.getPixelRGBA(source.getWidth()*15/64,source.getHeight()*16/32);
          int legacyFoot=source.getPixelRGBA(source.getWidth()*27/64,source.getHeight()*14/32);
          int belly=blend(body,violet,family);
          result.setPixelRGBA(result.getWidth()*104/128,result.getHeight()*56/64,belly);
          for(int y=0;y<8;y++)for(int x=0;x<8;x++) {
            int band=y<3||y>=5?blue:blend(yellow,blue,spec);
            int hand=blend(legacyHand,band,family);
            result.setPixelRGBA(result.getWidth()*(112+x)/128,result.getHeight()*(40+y)/64,hand);
            int footBand=y<2?paleBlue:y==2?blend(violet,blue,spec):white;
            int foot=blend(legacyFoot,footBand,family);
            result.setPixelRGBA(result.getWidth()*(112+x)/128,result.getHeight()*(52+y)/64,foot);
          }
        }
      }
      var target=id("dynamic/hero_run052/"+UUID.nameUUIDFromBytes(key.getBytes(StandardCharsets.UTF_8)));
      Minecraft.getInstance().getTextureManager().register(target,new DynamicTexture(result));result=null;
      CACHE.put(key,target);RETIRED.remove(target);
      if(CACHE.size()>512){var it=CACHE.entrySet().iterator();RETIRED.add(it.next().getValue());it.remove();}
      return target;
    }catch(IOException error){if(result!=null)result.close();throw new IllegalStateException("Cannot build Hero Run prototype coat",error);}
  }

  /** Run and alignment are independent baby influences. No limb stripe atlas
   * is authored here: the compact baby asset retains its original limb UVs. */
  private static ResourceLocation babyTexture053(ChaoState state,boolean overlay) {
    String prefix=overlay?"textures/entity/chao/":"textures/entity/chao/official/coats/baby/";
    String layout=overlay?"overlay":"baby";
    var sample=HeroRunAppearance052.sample(state);
    int alignment=Math.round(Math.clamp(HeroRunAppearance052.signedAlignment(state),-1,1)*256);
    int run=Math.round(unit(sample.babyRun())*128);
    float h=unit(alignment/256f/.5f),d=unit(-alignment/256f/.5f),r=run/128f;
    String suffix=(state.isTwoTone()?"_two":"_mono")+(state.isShiny()?"_shiny":"");
    boolean natural=state.baseColor()==0&&state.jewel()==0;
    String coat=natural?"normal_neutral_0"+suffix:ChaoRenderer.geneticCoatKey(state);
    String key="baby053:"+layout+":"+coat+(natural?":"+alignment+":"+run:":inherited");
    var cached=CACHE.get(key);if(cached!=null)return cached;
    NativeImage result=null;
    try(var source=read(prefix+coat+".png");
        var heroBaby=read(prefix+"normal_hero_0"+suffix+".png");
        var darkBaby=read(prefix+"normal_dark_0"+suffix+".png");
        var mask=read("textures/entity/chao/gameplay/masks/"+layout+".png");
        var accents=read("textures/entity/chao/gameplay/masks/"+layout+"_accent.png");
        var wings=read("textures/entity/chao/gameplay/masks/"+layout+"_wings.png")) {
      result=new NativeImage(source.getWidth(),source.getHeight(),false);result.copyFrom(source);
      float[] baseShades=shades(source,mask,accents,wings,state.isTwoTone());
      for(int y=0;y<result.getHeight();y++)for(int x=0;x<result.getWidth();x++) {
        int mx=x*mask.getWidth()/result.getWidth(),my=y*mask.getHeight()/result.getHeight();
        boolean skin=(mask.getPixelRGBA(mx,my)>>>24)!=0,wing=(wings.getPixelRGBA(mx,my)>>>24)!=0;
        if(!natural||!skin&&!wing)continue;
        int pixel=source.getPixelRGBA(x,y);if((pixel>>>24)==0)continue;
        int kind=state.isTwoTone()&&wing?2:state.isTwoTone()&&(accents.getPixelRGBA(mx,my)>>>24)!=0?1:0;
        float shade=Math.clamp(maxChannel(pixel)/baseShades[kind],.50f,1.25f);
        result.setPixelRGBA(x,y,babySkinColor053(pixel,heroBaby.getPixelRGBA(x,y),darkBaby.getPixelRGBA(x,y),shade,h,d,r,kind));
      }
      if(!overlay) {
        int body=result.getPixelRGBA(result.getWidth()*12/128,0);
        int accent=result.getPixelRGBA(result.getWidth()*48/128,0);
        int tip=accent;
        if(natural&&state.isTwoTone()) {
          int neutralTip=rgb(44,162,27,255),heroTip=rgb(148,190,219,255);
          int runTip=blend(blend(neutralTip,heroTip,h),accent,d);
          tip=blend(accent,runTip,r);
          // This is the sole baby face override. Hands and feet use their
          // coherent original atlas faces, not the adult band rectangles.
          int belly=blend(body,rgb(137,116,181,255),h*r);
          result.setPixelRGBA(result.getWidth()*104/128,result.getHeight()*56/64,belly);
        }
        result.setPixelRGBA(result.getWidth()-1,result.getHeight()-1,body|0xff000000);
        result.setPixelRGBA(result.getWidth()-2,result.getHeight()-1,tip|0xff000000);
      }
      var target=id("dynamic/hero_run052/baby053/"+UUID.nameUUIDFromBytes(key.getBytes(StandardCharsets.UTF_8)));
      Minecraft.getInstance().getTextureManager().register(target,new DynamicTexture(result));result=null;
      CACHE.put(key,target);RETIRED.remove(target);
      if(CACHE.size()>512){var it=CACHE.entrySet().iterator();RETIRED.add(it.next().getValue());it.remove();}
      return target;
    }catch(IOException error){if(result!=null)result.close();throw new IllegalStateException("Cannot build baby Run prototype coat",error);}
  }

  /** Pure palette sample used by the actual texture builder and focused checks. */
  static int babySkinColor053(int neutral,int heroBaby,int darkBaby,float shade,float hero,float dark,float run,int kind) {
    float h=unit(hero),d=unit(dark),r=unit(run);
    int[] neutralRun=BABY_RUN_NEUTRAL_053[Math.clamp(kind,0,2)];
    int[] heroRun=BABY_RUN_HERO_053[Math.clamp(kind,0,2)];
    int nr=rgb(neutralRun[0]*shade,neutralRun[1]*shade,neutralRun[2]*shade,neutral>>>24);
    int hr=rgb(heroRun[0]*shade,heroRun[1]*shade,heroRun[2]*shade,heroBaby>>>24);
    int ordinary=blend(blend(neutral,heroBaby,h),darkBaby,d);
    // Dark keeps the existing child family's colors while retaining the new
    // Run silhouette; this is not an adult Dark specialization expansion.
    int trained=blend(blend(nr,hr,h),darkBaby,d);
    return blend(ordinary,trained,r);
  }

  private static float[] shades(NativeImage image,NativeImage mask,NativeImage accents,NativeImage wings,boolean twoTone) {
    @SuppressWarnings("unchecked") Map<Integer,Integer>[] counts=new Map[]{new HashMap<>(),new HashMap<>(),new HashMap<>()};
    for(int y=0;y<image.getHeight();y++)for(int x=0;x<image.getWidth();x++) {
      int mx=x*mask.getWidth()/image.getWidth(),my=y*mask.getHeight()/image.getHeight();
      boolean wing=(wings.getPixelRGBA(mx,my)>>>24)!=0;
      if((mask.getPixelRGBA(mx,my)>>>24)==0&&!wing)continue;
      int pixel=image.getPixelRGBA(x,y);if((pixel>>>24)==0)continue;
      int kind=twoTone&&wing?2:twoTone&&(accents.getPixelRGBA(mx,my)>>>24)!=0?1:0;
      counts[kind].merge(pixel&0xffffff,1,Integer::sum);
    }
    float[] result=new float[3];for(int i=0;i<3;i++)result[i]=Math.max(1,maxChannel(counts[i].entrySet().stream().max(Map.Entry.comparingByValue()).map(Map.Entry::getKey).orElse(0xffffff)));
    return result;
  }
  private static float[][] palette(float sf,float power,float growth,float maturity)throws IOException {
    if(palettes==null) {
      try(var reader=Minecraft.getInstance().getResourceManager().openAsReader(id("gradual_evolution_palettes_021.json"))) {
        var source=JsonParser.parseReader(reader).getAsJsonObject().getAsJsonObject("profiles").getAsJsonArray("hero_run");
        palettes=new float[9][3][3];
        for(int i=0;i<9;i++)for(int k=0;k<3;k++) {
          var colors=source.get(i).getAsJsonObject().getAsJsonArray(new String[]{"body","accent","wings"}[k]);
          for(int c=0;c<3;c++)palettes[i][k][c]=colors.get(c).getAsFloat();
        }
      }
    }
    float spec=maturity>0?Math.clamp(growth/maturity,0,1):0;
    float[][] result=new float[3][3];
    for(int k=0;k<3;k++)for(int c=0;c<3;c++) {
      float center=palettes[4][k][c],inherited=0;
      for(int y=0;y<3;y++)for(int x=0;x<3;x++)inherited+=palettes[y*3+x][k][c]*Math.max(0,1-Math.abs(sf-(y-1)))*Math.max(0,1-Math.abs(power-(x-1)));
      result[k][c]=Math.clamp(inherited+(palettes[3][k][c]-center)*spec,0,255);
    }
    return result;
  }
  private static int blend(int a,int b,float t) {
    int result=a&0xff000000;for(int c=0;c<3;c++)result|=Math.round((a>>>(8*c)&255)*(1-t)+(b>>>(8*c)&255)*t)<<(8*c);return result;
  }
  private static int rgb(float r,float g,float b,int alpha){return alpha<<24|Math.round(Math.clamp(b,0,255))<<16|Math.round(Math.clamp(g,0,255))<<8|Math.round(Math.clamp(r,0,255));}
  private static float maxChannel(int pixel){return Math.max(pixel&255,Math.max(pixel>>>8&255,pixel>>>16&255));}
  private static float unit(float value){return Math.clamp(value,0,1);}
  @SubscribeEvent public static void endFrame(RenderFrameEvent.Post event) {
    var manager=Minecraft.getInstance().getTextureManager();for(var id:RETIRED)if(!CACHE.containsValue(id))manager.release(id);RETIRED.clear();
  }
  @EventBusSubscriber(modid=ChaoGarden.MODID,value=Dist.CLIENT,bus=EventBusSubscriber.Bus.MOD)
  public static final class Reload {
    @SubscribeEvent public static void register(RegisterClientReloadListenersEvent event) {
      event.registerReloadListener((ResourceManagerReloadListener)manager->Minecraft.getInstance().execute(()->{
        var textures=Minecraft.getInstance().getTextureManager();for(var id:CACHE.values())textures.release(id);for(var id:RETIRED)textures.release(id);
        CACHE.clear();RETIRED.clear();palettes=null;
      }));
    }
  }
}
