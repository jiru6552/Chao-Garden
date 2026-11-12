package com.chaogarden.client;

import com.chaogarden.ChaoGarden;
import com.chaogarden.core.ChaoState;
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

/** Cached authored palette effects preserve source alpha and protected eye texels. */
@EventBusSubscriber(modid=ChaoGarden.MODID,value=Dist.CLIENT)
public final class ChaoPaletteMaterial {
  private static final Map<String,ResourceLocation> CACHE=new LinkedHashMap<>(128,.75f,true);
  private static final Set<ResourceLocation> RETIRED=new HashSet<>();
  private ChaoPaletteMaterial() {}

  public static ResourceLocation texture(ResourceLocation source,ChaoState state,String uvLayout) {
    int brush=state.adult&&state.align>=.5f&&!state.immortal&&state.character.equals("normal")?Math.max(0,Math.min(4,state.heroBrushType)):0;
    if(!state.negativePalette&&!state.brightCoat&&!state.cosmeticWings&&brush==0)return source;
    String key=source+":"+uvLayout+":"+state.negativePalette+":"+state.brightCoat+":"+state.cosmeticWings+":"+brush;
    var cached=CACHE.get(key);if(cached!=null)return cached;
    var mc=Minecraft.getInstance();
    var mask=ResourceLocation.fromNamespaceAndPath("chao_garden","textures/entity/chao/gameplay/masks/"+uvLayout+".png");
    var wingMask=ResourceLocation.fromNamespaceAndPath("chao_garden","textures/entity/chao/gameplay/masks/"+uvLayout+"_wings.png");
    var accentMask=ResourceLocation.fromNamespaceAndPath("chao_garden","textures/entity/chao/gameplay/masks/"+uvLayout+"_accent.png");
    NativeImage image=null;
    try(var input=mc.getResourceManager().getResource(source).isPresent()?mc.getResourceManager().open(source):null;
        var maskInput=mc.getResourceManager().open(mask);
        var wingsInput=mc.getResourceManager().open(wingMask);
        var accentInput=mc.getResourceManager().open(accentMask);
        var skin=NativeImage.read(maskInput);var wings=NativeImage.read(wingsInput);var accents=NativeImage.read(accentInput)) {
      if(input!=null)image=NativeImage.read(input);
      else if(mc.getTextureManager().getTexture(source) instanceof DynamicTexture dynamic&&dynamic.getPixels()!=null){
        var pixels=dynamic.getPixels();image=new NativeImage(pixels.getWidth(),pixels.getHeight(),false);image.copyFrom(pixels);
      }else throw new IOException("Missing selected Chao material "+source);
      Map<Integer,Integer> counts=new HashMap<>();
      for(int y=0;y<image.getHeight();y++)for(int x=0;x<image.getWidth();x++) {
        int mx=x*skin.getWidth()/image.getWidth(),my=y*skin.getHeight()/image.getHeight();
        if(protectedMarker(source.getPath(),uvLayout,state.adult,x,y,image.getWidth(),image.getHeight()))continue;
        int rgba=image.getPixelRGBA(x,y);
        if((skin.getPixelRGBA(mx,my)>>>24)!=0&&(rgba>>>24)!=0)counts.merge(rgba&0xffffff,1,Integer::sum);
      }
      int coat=counts.entrySet().stream().max(Map.Entry.comparingByValue()).map(Map.Entry::getKey).orElse(0xeff09d);
      for(int y=0;y<image.getHeight();y++)for(int x=0;x<image.getWidth();x++) {
        int mx=x*skin.getWidth()/image.getWidth(),my=y*skin.getHeight()/image.getHeight();
        if(protectedMarker(source.getPath(),uvLayout,state.adult,x,y,image.getWidth(),image.getHeight()))continue;
        int reserved=prototypeReservedKind(source.getPath(),uvLayout,x,y,image.getWidth(),image.getHeight());
        boolean body=(skin.getPixelRGBA(mx,my)>>>24)!=0||(reserved&1)!=0;
        boolean wing=state.cosmeticWings&&(wings.getPixelRGBA(mx,my)>>>24)!=0;
        boolean accent=brush!=0&&((accents.getPixelRGBA(mx,my)>>>24)!=0||(reserved&2)!=0);
        int rgba=image.getPixelRGBA(x,y);if((rgba>>>24)==0||!body&&!wing&&!accent)continue;
        int red=rgba&255,green=(rgba>>>8)&255,blue=(rgba>>>16)&255;
        if(wing) {
          float shade=.76f+Math.max(red,Math.max(green,blue))/255f*.24f;
          red=Math.round((coat&255)*shade);green=Math.round(((coat>>>8)&255)*shade);blue=Math.round(((coat>>>16)&255)*shade);
        }
        image.setPixelRGBA(x,y,paletteEffects((rgba&0xff000000)|(blue<<16)|(green<<8)|red,
            accent,brush,state.negativePalette,state.brightCoat));
      }
      var id=ResourceLocation.fromNamespaceAndPath("chao_garden","dynamic/palette/"+UUID.nameUUIDFromBytes(key.getBytes(StandardCharsets.UTF_8)));
      mc.getTextureManager().register(id,new DynamicTexture(image));image=null;
      CACHE.put(key,id);RETIRED.remove(id);
      if(CACHE.size()>512) {
        var old=CACHE.entrySet().iterator();RETIRED.add(old.next().getValue());old.remove();
      }
      return id;
    } catch(IOException error) {
      if(image!=null)image.close();
      throw new IllegalStateException("Cannot build Chao palette from "+source,error);
    }
  }

  /** Prototype-only UV additions. Overlay/ordinary materials retain their exact
   * old masks; protected eye texels are outside every listed rectangle. */
  static boolean protectedMarker(String sourcePath,String layout,boolean adult,int x,int y,int width,int height) {
    return HeroNormalRegionPaint057.paletteProtected(sourcePath,layout,x,y,width,height)
        ||HeroRunMarkerPaint056.paletteProtected(sourcePath,layout,adult,x,y,width,height);
  }
  static int prototypeReservedKind(String sourcePath,String layout,int x,int y,int width,int height) {
    if(sourcePath.startsWith("dynamic/hero_normal057/")&&layout.equals("baby")&&width>0&&height>0)
      return HeroRunRegionPaint055.reservedKind(x*128/width,y*64/height);
    if(sourcePath.startsWith("dynamic/neutral056/")&&layout.equals("baby")&&width>0&&height>0)
      return HeroRunRegionPaint055.reservedKind(x*128/width,y*64/height);
    if(sourcePath.startsWith("dynamic/hero_run055/")&&layout.equals("baby")&&width>0&&height>0)
      return HeroRunRegionPaint055.reservedKind(x*128/width,y*64/height);
    if(sourcePath.startsWith("dynamic/hero_run054/")&&layout.equals("baby")&&width>0&&height>0)
      return HeroRunRegionPaint054.reservedKind(x*128/width,y*64/height);
    if(!sourcePath.startsWith("dynamic/hero_run052/")||!layout.equals("baby")||width<=0||height<=0)return 0;
    int u=x*128/width,v=y*64/height;
    if(u==127&&v==63)return 1;
    if(u==126&&v==63||u==104&&v==56)return 3;
    if(u>=112&&u<120&&v>=40&&v<48)return 3;
    if(u>=112&&u<120&&v>=52&&v<60)return v<55?3:1;
    return 0;
  }
  /** Shared actual pixel implementation, also sampled by focused pure checks. */
  static int paletteEffects(int rgba,boolean accent,int brush,boolean negative,boolean bright) {
    int red=rgba&255,green=rgba>>>8&255,blue=rgba>>>16&255;
    if(accent) {
      int[] rgb=switch(brush){case 1->new int[]{246,214,79};case 2->new int[]{161,91,222};case 3->new int[]{80,192,115};default->new int[]{222,73,83};};
      float shade=.7f+Math.max(red,Math.max(green,blue))/255f*.3f;
      red=Math.round(rgb[0]*shade);green=Math.round(rgb[1]*shade);blue=Math.round(rgb[2]*shade);
    }
    if(negative){red=255-red;green=255-green;blue=255-blue;}
    if(bright){red=Math.min(255,Math.round(28+red*1.18f));green=Math.min(255,Math.round(28+green*1.18f));blue=Math.min(255,Math.round(28+blue*1.18f));}
    return rgba&0xff000000|blue<<16|green<<8|red;
  }

  /** Release evicted textures only after queued entity batches have rendered. */
  @SubscribeEvent public static void endFrame(RenderFrameEvent.Post event) {
    if(RETIRED.isEmpty())return;
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
      event.registerReloadListener((ResourceManagerReloadListener)manager->Minecraft.getInstance().execute(ChaoPaletteMaterial::clear));
    }
  }
}
