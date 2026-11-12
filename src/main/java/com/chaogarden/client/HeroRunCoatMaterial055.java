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

/** Small pixel regions on the retained original atlas. All coat/finish
 * variants use the same blocky anatomy and texture coordinates. */
@EventBusSubscriber(modid=ChaoGarden.MODID,value=Dist.CLIENT)
public final class HeroRunCoatMaterial055 {
  private static final Map<String,ResourceLocation> CACHE=new LinkedHashMap<>(128,.75f,true);
  private static final Set<ResourceLocation> RETIRED=new HashSet<>();
  private static int[] layout;
  private static float[][][] axisPalettes;
  private HeroRunCoatMaterial055() {}
  private static ResourceLocation id(String path){return ResourceLocation.fromNamespaceAndPath(ChaoGarden.MODID,path);}
  private static NativeImage read(String path)throws IOException{try(var input=Minecraft.getInstance().getResourceManager().open(id(path))){return NativeImage.read(input);}}
  private static String suffix(ChaoState state){return (state.isTwoTone()?"_two":"_mono")+(state.isShiny()?"_shiny":"");}
  private static boolean natural(ChaoState state){return state.baseColor()==0&&state.jewel()==0;}
  private static String coat(ChaoState state){return natural(state)?"normal_neutral_0"+suffix(state):ChaoRenderer.geneticCoatKey(state);}

  /** Animal additions and flames retain their existing atlas and alpha. */
  static ResourceLocation textureOverlay(ChaoState state){return id("textures/entity/chao/"+coat(state)+".png");}

  static ResourceLocation texture(ChaoState state) {
    var sample=HeroRunAppearance052.sample(state);
    int alignment=Math.round(Math.clamp(HeroRunAppearance052.signedAlignment(state),-1,1)*256);
    int heroWeight=Math.round(sample.babyHero()*128),run=Math.round(sample.babyGrowth()*128),maturity=Math.round(sample.maturity()*128),growth=Math.round(sample.adultGrowth()*128);
    int sf=Math.round(Math.clamp(state.swimFly,-1,1)*32),power=Math.round(Math.clamp(state.runPower,-1,1)*32);
    String key=coat(state)+":"+sample.adult()+":"+alignment+":"+heroWeight+":"+run+":"+maturity+":"+growth+":"+sf+":"+power;
    var cached=CACHE.get(key);if(cached!=null)return cached;
    NativeImage result=null;
    try(var source=read("textures/entity/chao/official/coats/baby/"+coat(state)+".png");
        var hero=read("textures/entity/chao/official/coats/baby/normal_hero_0"+suffix(state)+".png");
        var dark=read("textures/entity/chao/official/coats/baby/normal_dark_0"+suffix(state)+".png")) {
      int[] regions=layout();
      int body=sample(source,12,0),accent=sample(source,48,0);
      var input=new HeroRunRegionPaint055.Input(natural(state),state.isTwoTone(),sample.adult(),
          heroWeight/128f,Math.clamp(-alignment/256f/.5f,0,1),run/128f,maturity/128f,growth/128f,
          body,accent,dominant(source,regions,HeroRunRegionPaint055.WING),dominant(source,regions,HeroRunRegionPaint055.MARKER),
          sample(hero,12,0),sample(hero,48,0),sample(dark,12,0),sample(dark,48,0));
      var colors=HeroRunRegionPaint055.colors(input);
      if(sample.adult())colors=HeroRunRegionPaint055.axisColors(colors,axisPalettes(),sf/32f,power/32f,maturity/128f,!input.natural()||!input.twoTone());
      int bodyReference=body,accentReference=accent;
      if(input.natural()&&!input.adult()) {
        bodyReference=HeroRunRegionPaint055.blend(HeroRunRegionPaint055.blend(body,input.heroBody(),input.hero()),input.darkBody(),input.dark());
        accentReference=HeroRunRegionPaint055.blend(HeroRunRegionPaint055.blend(accent,input.heroAccent(),input.hero()),input.darkAccent(),input.dark());
      }
      result=new NativeImage(source.getWidth(),source.getHeight(),false);
      for(int y=0;y<source.getHeight();y++)for(int x=0;x<source.getWidth();x++) {
        int u=x*128/source.getWidth(),v=y*64/source.getHeight(),flags=regions[v*128+u];
        int reference=(flags&(HeroRunRegionPaint055.WING|HeroRunRegionPaint055.MARKER))!=0?255
            :HeroRunRegionPaint055.maxChannel((flags&HeroRunRegionPaint055.ACCENT)!=0?accentReference:bodyReference);
        result.setPixelRGBA(x,y,HeroRunRegionPaint055.pixel(input,colors,u,v,source.getPixelRGBA(x,y),
            sample(hero,u,v),sample(dark,u,v),flags,reference));
      }
      var target=id("dynamic/hero_run055/"+UUID.nameUUIDFromBytes(key.getBytes(StandardCharsets.UTF_8)));
      var dynamic=new DynamicTexture(result);dynamic.setFilter(false,false);
      Minecraft.getInstance().getTextureManager().register(target,dynamic);result=null;
      CACHE.put(key,target);RETIRED.remove(target);
      if(CACHE.size()>512){var iterator=CACHE.entrySet().iterator();RETIRED.add(iterator.next().getValue());iterator.remove();}
      return target;
    }catch(IOException error){if(result!=null)result.close();throw new IllegalStateException("Cannot build blocky Hero Run055 material",error);}
  }

  // Shared canonical baby-atlas semantics; the Neutral adult additions keep
  // this same original UV layout rather than inventing a second mapping.
  static int[] layout()throws IOException {
    if(layout!=null)return layout;
    var flags=new int[128*64];
    for(String kind:new String[]{"baby","baby_accent","baby_wings"}) {
      int tag=kind.equals("baby")?HeroRunRegionPaint055.SKIN:kind.endsWith("accent")?HeroRunRegionPaint055.ACCENT:HeroRunRegionPaint055.WING;
      try(var mask=read("textures/entity/chao/gameplay/masks/"+kind+".png")) {
        for(int y=0;y<64;y++)for(int x=0;x<128;x++)if(mask.getPixelRGBA(x*mask.getWidth()/128,y*mask.getHeight()/64)>>>24!=0)flags[y*128+x]|=tag;
      }
    }
    // Original named semantic parts establish the used texels. Cross-asset
    // cube indices are deliberately never used for correspondence.
    try(var reader=Minecraft.getInstance().getResourceManager().openAsReader(id("models/entity/gameplay/chao_baby.json"))) {
      var data=JsonParser.parseReader(reader).getAsJsonObject();
      for(var boneElement:data.getAsJsonArray("bones")) {
        var bone=boneElement.getAsJsonObject();String name=bone.get("name").getAsString();int tag=0;
        if(name.equals("skull__cube_2")||name.equals("skull__cube_3")||name.startsWith("expression_")||name.startsWith("iris_"))tag|=HeroRunRegionPaint055.PROTECTED;
        if(name.startsWith("arm_")||name.startsWith("foot_"))tag|=HeroRunRegionPaint055.LIMB;
        if(name.startsWith("tail__"))tag|=HeroRunRegionPaint055.TAIL;
        if(name.equals("emotion_dot__cube_0"))tag|=HeroRunRegionPaint055.MARKER;
        if(tag==0||!bone.has("meshes"))continue;
        for(var mesh:bone.getAsJsonArray("meshes"))for(var face:mesh.getAsJsonObject().getAsJsonArray("faces")) {
          float x0=Float.POSITIVE_INFINITY,y0=x0,x1=Float.NEGATIVE_INFINITY,y1=x1;
          for(var point:face.getAsJsonObject().getAsJsonArray("uv")) {
            var uv=point.getAsJsonArray();float x=uv.get(0).getAsFloat()*2,y=uv.get(1).getAsFloat()*2;
            x0=Math.min(x0,x);x1=Math.max(x1,x);y0=Math.min(y0,y);y1=Math.max(y1,y);
          }
          for(int y=Math.max(0,(int)Math.floor(y0));y<Math.min(64,(int)Math.ceil(y1));y++)
            for(int x=Math.max(0,(int)Math.floor(x0));x<Math.min(128,(int)Math.ceil(x1));x++)flags[y*128+x]|=tag;
        }
      }
    }
    layout=flags;return layout;
  }
  private static int sample(NativeImage image,int x,int y){return image.getPixelRGBA(x*image.getWidth()/128,y*image.getHeight()/64);}
  private static float[][][] axisPalettes()throws IOException {
    if(axisPalettes!=null)return axisPalettes;
    try(var reader=Minecraft.getInstance().getResourceManager().openAsReader(id("gradual_evolution_palettes_021.json"))) {
      var source=JsonParser.parseReader(reader).getAsJsonObject().getAsJsonObject("profiles").getAsJsonArray("hero_run");
      var result=new float[9][3][3];
      for(int i=0;i<9;i++)for(int k=0;k<3;k++) {
        var channels=source.get(i).getAsJsonObject().getAsJsonArray(new String[]{"body","accent","wings"}[k]);
        for(int c=0;c<3;c++)result[i][k][c]=channels.get(c).getAsFloat();
      }
      axisPalettes=result;return result;
    }
  }
  static int dominant(NativeImage image,int[] regions,int tag) {
    Map<Integer,Integer> values=new HashMap<>();
    for(int y=0;y<image.getHeight();y++)for(int x=0;x<image.getWidth();x++) {
      int pixel=image.getPixelRGBA(x,y),flags=regions[y*64/image.getHeight()*128+x*128/image.getWidth()];
      if(pixel>>>24!=0&&(flags&tag)!=0)values.merge(pixel&0xffffff,1,Integer::sum);
    }
    return values.entrySet().stream().max(Map.Entry.comparingByValue()).map(Map.Entry::getKey).orElse(0xffffff)|0xff000000;
  }
  @SubscribeEvent public static void endFrame(RenderFrameEvent.Post event) {
    if(RETIRED.isEmpty())return;
    var textures=Minecraft.getInstance().getTextureManager();for(var location:RETIRED)if(!CACHE.containsValue(location))textures.release(location);RETIRED.clear();
  }
  @EventBusSubscriber(modid=ChaoGarden.MODID,value=Dist.CLIENT,bus=EventBusSubscriber.Bus.MOD)
  public static final class Reload {
    @SubscribeEvent public static void register(RegisterClientReloadListenersEvent event) {
      event.registerReloadListener((ResourceManagerReloadListener)manager->Minecraft.getInstance().execute(()->{
        var textures=Minecraft.getInstance().getTextureManager();for(var location:CACHE.values())textures.release(location);for(var location:RETIRED)textures.release(location);
        CACHE.clear();RETIRED.clear();layout=null;axisPalettes=null;
      }));
    }
  }
}
