package com.chaogarden.client;

import com.chaogarden.ChaoGarden;
import com.chaogarden.core.ChaoState;
import com.mojang.blaze3d.platform.NativeImage;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
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

/** Shared original-atlas material for the nine Hero/Normal combinations. */
@EventBusSubscriber(modid=ChaoGarden.MODID,value=Dist.CLIENT)
public final class HeroNormalCoatMaterial057 {
  private static final Map<String,ResourceLocation> CACHE=new LinkedHashMap<>(128,.75f,true);
  private static final Set<ResourceLocation> RETIRED=new HashSet<>();
  private HeroNormalCoatMaterial057() {}
  private static ResourceLocation id(String path){return ResourceLocation.fromNamespaceAndPath(ChaoGarden.MODID,path);}
  private static boolean natural(ChaoState state){return state.baseColor()==0&&state.jewel()==0;}
  private static String coat(ChaoState state){return natural(state)?"normal_hero_0"+(state.isTwoTone()?"_two":"_mono")+(state.isShiny()?"_shiny":""):ChaoRenderer.geneticCoatKey(state);}
  static ResourceLocation textureOverlay(ChaoState state){return id("textures/entity/chao/"+coat(state)+".png");}

  static ResourceLocation texture(ChaoState state) {
    var sample=HeroNormalAppearance057.sample(state);
    int age=Math.round(sample.maturity()*128),rp=Math.round(sample.runPower()*64),sf=Math.round(sample.swimFly()*64);
    String key=coat(state)+":"+age+":"+rp+":"+sf;
    var cached=CACHE.get(key);if(cached!=null)return cached;
    NativeImage image=null;
    try(var stream=Minecraft.getInstance().getResourceManager().open(id("textures/entity/chao/official/coats/baby/"+coat(state)+".png"));
        var source=NativeImage.read(stream)) {
      int[] layout=HeroRunCoatMaterial055.layout();
      var input=new HeroNormalRegionPaint057.Input(natural(state),state.isTwoTone(),age/128f,rp/64f,sf/64f,
          source.getPixelRGBA(12*source.getWidth()/128,0),source.getPixelRGBA(48*source.getWidth()/128,0),
          HeroRunCoatMaterial055.dominant(source,layout,HeroRunRegionPaint055.WING),
          HeroRunCoatMaterial055.dominant(source,layout,HeroRunRegionPaint055.MARKER));
      var colors=HeroNormalRegionPaint057.colors(input);
      image=new NativeImage(source.getWidth(),source.getHeight(),false);
      for(int y=0;y<source.getHeight();y++)for(int x=0;x<source.getWidth();x++) {
        int u=x*128/source.getWidth(),v=y*64/source.getHeight();
        image.setPixelRGBA(x,y,HeroNormalRegionPaint057.pixel(input,colors,u,v,source.getPixelRGBA(x,y),layout[v*128+u]));
      }
      var target=id("dynamic/hero_normal057/"+UUID.nameUUIDFromBytes(key.getBytes(StandardCharsets.UTF_8)));
      var dynamic=new DynamicTexture(image);dynamic.setFilter(false,false);
      Minecraft.getInstance().getTextureManager().register(target,dynamic);image=null;
      CACHE.put(key,target);RETIRED.remove(target);
      if(CACHE.size()>512){var it=CACHE.entrySet().iterator();RETIRED.add(it.next().getValue());it.remove();}
      return target;
    }catch(IOException error){if(image!=null)image.close();throw new IllegalStateException("Cannot build HeroNormal057 material",error);}
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
