package com.chaogarden.client;

import com.chaogarden.ChaoGarden;
import com.mojang.blaze3d.platform.NativeImage;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterClientReloadListenersEvent;
import net.neoforged.neoforge.client.event.RenderFrameEvent;

/** Icy terminal surfaces inherit the selected coat, with continuous alpha and a pale edge. */
@EventBusSubscriber(modid=ChaoGarden.MODID,value=Dist.CLIENT)
public final class ChaoChaosGlassMaterial {
  private static final Map<String,ResourceLocation> CACHE=new LinkedHashMap<>(64,.75f,true);
  private static final Set<ResourceLocation> RETIRED=new HashSet<>();
  private ChaoChaosGlassMaterial() {}

  static ResourceLocation texture(ResourceLocation source,boolean transparent) {
    String key=source+":"+transparent;
    var cached=CACHE.get(key);if(cached!=null)return cached;
    var mc=Minecraft.getInstance();
    NativeImage gradient=null;
    try(var input=mc.getResourceManager().getResource(source).isPresent()
        ?mc.getResourceManager().open(source):null) {
      // Palette effects may already have produced a registered dynamic texture.
      NativeImage skin=null;
      boolean closeSkin=false;
      if(input!=null){skin=NativeImage.read(input);closeSkin=true;}
      else if(mc.getTextureManager().getTexture(source) instanceof DynamicTexture dynamic)skin=dynamic.getPixels();
      if(skin==null)throw new IOException("Missing selected Chaos coat "+source);
      try {
        int body=sample(skin,36,26),tip=sample(skin,50,24);
        gradient=new NativeImage(64,32,false);
        for(int y=0;y<32;y++)for(int x=0;x<64;x++) {
          float t=x/63f;
          float blend=Math.min(1,t*2.4f);
          int start=y<16?tip:body;
          float edge=Math.abs((y%16)/15f-.5f)*2;
          float frost=transparent?(float)Math.pow(edge,5)*.27f*t:0;
          int red=channel(start,tip,0,blend,frost);
          int green=channel(start,tip,8,blend,frost);
          int blue=channel(start,tip,16,blend,frost);
          // The proximal edge is fully opaque. No material cap sits behind this shell.
          int alpha=transparent?Math.round(255*(1-.62f*t*t*(3-2*t))):255;
          gradient.setPixelRGBA(x,y,(alpha<<24)|(blue<<16)|(green<<8)|red);
        }
      } finally {if(closeSkin)skin.close();}
      var id=ResourceLocation.fromNamespaceAndPath("chao_garden","dynamic/chaos_ice/"+
          UUID.nameUUIDFromBytes(key.getBytes(StandardCharsets.UTF_8)));
      mc.getTextureManager().register(id,new DynamicTexture(gradient));gradient=null;
      CACHE.put(key,id);RETIRED.remove(id);
      if(CACHE.size()>384){var old=CACHE.entrySet().iterator();RETIRED.add(old.next().getValue());old.remove();}
      return id;
    } catch(IOException error) {
      if(gradient!=null)gradient.close();
      throw new IllegalStateException("Cannot build Chaos ice material",error);
    }
  }

  private static int sample(NativeImage skin,float u,float v) {
    return skin.getPixelRGBA(Math.min(skin.getWidth()-1,(int)(u*skin.getWidth()/64)),
        Math.min(skin.getHeight()-1,(int)(v*skin.getHeight()/32)));
  }
  private static int channel(int start,int end,int shift,float t,float frost) {
    float value=((start>>>shift)&255)*(1-t)+((end>>>shift)&255)*t;
    return Math.max(0,Math.min(255,Math.round(value*(1-frost)+255*frost)));
  }
  @SubscribeEvent public static void endFrame(RenderFrameEvent.Post event) {
    var textures=Minecraft.getInstance().getTextureManager();
    for(var id:RETIRED)if(!CACHE.containsValue(id))textures.release(id);
    RETIRED.clear();
  }
  private static void clear() {
    var textures=Minecraft.getInstance().getTextureManager();
    for(var id:CACHE.values())textures.release(id);
    for(var id:RETIRED)textures.release(id);
    CACHE.clear();RETIRED.clear();
  }
  @EventBusSubscriber(modid=ChaoGarden.MODID,value=Dist.CLIENT,bus=EventBusSubscriber.Bus.MOD)
  public static final class Reload {
    @SubscribeEvent public static void register(RegisterClientReloadListenersEvent event) {
      event.registerReloadListener((ResourceManagerReloadListener)manager->Minecraft.getInstance().execute(ChaoChaosGlassMaterial::clear));
    }
  }
}
