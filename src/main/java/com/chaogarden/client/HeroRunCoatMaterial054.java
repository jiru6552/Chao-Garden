package com.chaogarden.client;

import com.chaogarden.ChaoGarden;
import com.chaogarden.core.ChaoState;
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

/** Unified semantic skin regions; genetic color and reflection do not select
 * different anatomy or omit the crown, limb rings or belly inset. */
@EventBusSubscriber(modid=ChaoGarden.MODID,value=Dist.CLIENT)
public final class HeroRunCoatMaterial054 {
  private static final Map<String,ResourceLocation> CACHE=new LinkedHashMap<>(128,.75f,true);
  private static final Set<ResourceLocation> RETIRED=new HashSet<>();
  private HeroRunCoatMaterial054() {}
  private static ResourceLocation id(String path){return ResourceLocation.fromNamespaceAndPath(ChaoGarden.MODID,path);}
  private static NativeImage read(String path)throws IOException{try(var input=Minecraft.getInstance().getResourceManager().open(id(path))){return NativeImage.read(input);}}
  private static String suffix(ChaoState state){return (state.isTwoTone()?"_two":"_mono")+(state.isShiny()?"_shiny":"");}
  private static boolean natural(ChaoState state){return state.baseColor()==0&&state.jewel()==0;}
  private static String coat(ChaoState state){return natural(state)?"normal_neutral_0"+suffix(state):ChaoRenderer.geneticCoatKey(state);}

  /** Ordinary overlay geometry contains fixed animal additions and flame, not
   * the new skin. Keep those original atlas pixels and expression colors. */
  static ResourceLocation textureOverlay(ChaoState state){return id("textures/entity/chao/"+coat(state)+".png");}

  static ResourceLocation texture(ChaoState state) {
    var sample=HeroRunAppearance052.sample(state);
    int alignment=Math.round(Math.clamp(HeroRunAppearance052.signedAlignment(state),-1,1)*256);
    int run=Math.round(sample.babyRun()*128),growth=Math.round(sample.adultGrowth()*128);
    String key=coat(state)+":"+sample.adult()+":"+alignment+":"+run+":"+growth;
    var cached=CACHE.get(key);if(cached!=null)return cached;
    NativeImage result=null;
    try(var source=read("textures/entity/chao/official/coats/baby/"+coat(state)+".png");
        var hero=read("textures/entity/chao/official/coats/baby/normal_hero_0"+suffix(state)+".png");
        var dark=read("textures/entity/chao/official/coats/baby/normal_dark_0"+suffix(state)+".png");
        var skin=read("textures/entity/chao/gameplay/masks/baby.png");
        var accents=read("textures/entity/chao/gameplay/masks/baby_accent.png");
        var wings=read("textures/entity/chao/gameplay/masks/baby_wings.png")) {
      result=new NativeImage(source.getWidth(),source.getHeight(),false);result.copyFrom(source);
      float h=Math.clamp(alignment/256f/.5f,0,1),d=Math.clamp(-alignment/256f/.5f,0,1),r=run/128f,g=growth/128f;
      int body=sample(source,12,0),accent=sample(source,48,0),wing=dominant(source,wings);
      var input=new HeroRunRegionPaint054.Input(natural(state),state.isTwoTone(),sample.adult(),h,d,r,g,
          body,accent,wing,sample(hero,12,0),sample(hero,48,0),sample(dark,12,0),sample(dark,48,0));
      var colors=HeroRunRegionPaint054.colors(input);
      int[] shades={maxChannel(body),maxChannel(accent),maxChannel(wing)};
      for(int y=0;y<result.getHeight();y++)for(int x=0;x<result.getWidth();x++) {
        int u=x*128/result.getWidth(),v=y*64/result.getHeight();
        if(HeroRunRegionPaint054.reserved(u,v)) {
          result.setPixelRGBA(x,y,HeroRunRegionPaint054.pixel(colors,u,v));continue;
        }
        int mx=x*skin.getWidth()/source.getWidth(),my=y*skin.getHeight()/source.getHeight();
        boolean isWing=(wings.getPixelRGBA(mx,my)>>>24)!=0,isSkin=(skin.getPixelRGBA(mx,my)>>>24)!=0;
        int pixel=source.getPixelRGBA(x,y);
        int kind=isWing?2:(accents.getPixelRGBA(mx,my)>>>24)!=0?1:0;
        result.setPixelRGBA(x,y,HeroRunRegionPaint054.originalPixel(input,colors,pixel,isSkin,isWing,kind==1,shades[kind]));
      }
      var target=id("dynamic/hero_run054/"+UUID.nameUUIDFromBytes(key.getBytes(StandardCharsets.UTF_8)));
      Minecraft.getInstance().getTextureManager().register(target,new DynamicTexture(result));result=null;
      CACHE.put(key,target);RETIRED.remove(target);
      if(CACHE.size()>512){var iterator=CACHE.entrySet().iterator();RETIRED.add(iterator.next().getValue());iterator.remove();}
      return target;
    }catch(IOException error){if(result!=null)result.close();throw new IllegalStateException("Cannot build Hero Run054 regions",error);}
  }
  private static int sample(NativeImage image,int x,int y){return image.getPixelRGBA(x*image.getWidth()/128,y*image.getHeight()/64);}
  private static int maxChannel(int pixel){return Math.max(pixel&255,Math.max(pixel>>>8&255,pixel>>>16&255));}
  private static int dominant(NativeImage image,NativeImage mask) {
    Map<Integer,Integer> values=new HashMap<>();
    for(int y=0;y<image.getHeight();y++)for(int x=0;x<image.getWidth();x++) {
      int pixel=image.getPixelRGBA(x,y);
      if(pixel>>>24!=0&&mask.getPixelRGBA(x*mask.getWidth()/image.getWidth(),y*mask.getHeight()/image.getHeight())>>>24!=0)
        values.merge(pixel&0xffffff,1,Integer::sum);
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
        CACHE.clear();RETIRED.clear();
      }));
    }
  }
}
