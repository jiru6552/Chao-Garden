package com.chaogarden.client;

import com.chaogarden.ChaoGarden;
import com.chaogarden.ChaoVariants;
import com.chaogarden.core.ChaoState;
import com.google.gson.*;
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

/** Authored aged natural palettes; inherited colored/jewel coats and eye pixels stay intact. */
@EventBusSubscriber(modid=ChaoGarden.MODID,value=Dist.CLIENT)
public final class GradualCoatMaterial {
  private static final Map<String,ResourceLocation> CACHE=new LinkedHashMap<>(128,.75f,true);
  private static final Set<ResourceLocation> RETIRED=new HashSet<>();
  private static Map<String,float[][][]> profiles;
  private static Map<String,int[]> coreSwatches;
  private GradualCoatMaterial(){}
  private static ResourceLocation id(String s){return ResourceLocation.fromNamespaceAndPath(ChaoGarden.MODID,s);}

  /** The UV layout is independent of the builder's fixed primary; robots use baby UVs. */
  public static ResourceLocation texture(ResourceLocation source,ChaoState state,String uvLayout) {
    if(!state.adult||state.baseColor()!=0||state.jewel()!=0||state.immortal
        ||!"normal".equals(state.character)||!ChaoVariants.NORMAL.equals(ChaoVariants.activeId(state)))return source;
    int age=Math.round(Math.max(0,Math.min(1,state.evolutionStrength()/1.2f))*64);
    String profile=OfficialChaoRig.geometryKey(state);
    boolean whiteCore=profile.equals("hero_swim")&&uvLayout.equals("swim");
    if(age==0&&!whiteCore)return source;
    int sf=Math.round(Math.max(-1,Math.min(1,state.swimFly))*32),rp=Math.round(Math.max(-1,Math.min(1,state.runPower))*32);
    String key=source+":"+uvLayout+":"+profile+":"+sf+":"+rp+":"+age+":"+state.isTwoTone()+(whiteCore?":"+state.negativePalette+":"+state.brightCoat:"");
    var cached=CACHE.get(key);if(cached!=null)return cached;
    var mc=Minecraft.getInstance();NativeImage result=null;
    try(var input=mc.getResourceManager().open(source);
        var skinInput=mc.getResourceManager().open(id("textures/entity/chao/gameplay/masks/"+uvLayout+".png"));
        var accentInput=mc.getResourceManager().open(id("textures/entity/chao/gameplay/masks/"+uvLayout+"_accent.png"));
        var wingInput=mc.getResourceManager().open(id("textures/entity/chao/gameplay/masks/"+uvLayout+"_wings.png"));
        var skin=NativeImage.read(skinInput);var accent=NativeImage.read(accentInput);var wings=NativeImage.read(wingInput)) {
      result=NativeImage.read(input);float[][] palette=palette(profile,sf/32f,rp/32f);float t=age/64f;
      @SuppressWarnings("unchecked") Map<Integer,Integer>[] counts=new Map[]{new HashMap<>(),new HashMap<>(),new HashMap<>()};
      for(int y=0;y<result.getHeight();y++)for(int x=0;x<result.getWidth();x++) {
        int mx=x*skin.getWidth()/result.getWidth(),my=y*skin.getHeight()/result.getHeight();
        if((skin.getPixelRGBA(mx,my)>>>24)==0&&(wings.getPixelRGBA(mx,my)>>>24)==0)continue;
        int pixel=result.getPixelRGBA(x,y);if((pixel>>>24)==0)continue;
        int kind=state.isTwoTone()&&(wings.getPixelRGBA(mx,my)>>>24)!=0?2:
            state.isTwoTone()&&(accent.getPixelRGBA(mx,my)>>>24)!=0?1:0;
        counts[kind].merge(pixel&0xffffff,1,Integer::sum);
      }
      float[] shades=new float[3];
      for(int k=0;k<3;k++) {
        int main=counts[k].entrySet().stream().max(Map.Entry.comparingByValue()).map(Map.Entry::getKey).orElse(0xffffff);
        shades[k]=Math.max(1,Math.max(main&255,Math.max(main>>>8&255,main>>>16&255)));
      }
      for(int y=0;y<result.getHeight();y++)for(int x=0;x<result.getWidth();x++) {
        int mx=x*skin.getWidth()/result.getWidth(),my=y*skin.getHeight()/result.getHeight();
        if((skin.getPixelRGBA(mx,my)>>>24)==0&&(wings.getPixelRGBA(mx,my)>>>24)==0)continue;
        int pixel=result.getPixelRGBA(x,y);if((pixel>>>24)==0)continue;
        int kind=state.isTwoTone()&&(wings.getPixelRGBA(mx,my)>>>24)!=0?2:
            state.isTwoTone()&&(accent.getPixelRGBA(mx,my)>>>24)!=0?1:0;
        float shade=Math.max(.50f,Math.min(1.25f,Math.max(pixel&255,Math.max(pixel>>>8&255,pixel>>>16&255))/shades[kind]));
        int[] rgb=new int[3];for(int c=0;c<3;c++) {
          float target=Math.min(255,palette[kind][c]*shade);
          rgb[c]=Math.max(0,Math.min(255,Math.round((pixel>>>(c*8)&255)*(1-t)+target*t)));
        }
        result.setPixelRGBA(x,y,pixel&0xff000000|rgb[2]<<16|rgb[1]<<8|rgb[0]);
      }
      if(whiteCore) {
        var point=coreSwatches.get(profile);if(point==null)throw new IOException("Missing Hero natural core swatch");
        int c=state.negativePalette?0:255;if(state.brightCoat)c=Math.min(255,Math.round(28+c*1.18f));
        result.setPixelRGBA(point[0],point[1],0xff000000|c<<16|c<<8|c);
      }
      var target=id("dynamic/gradual_coat/"+UUID.nameUUIDFromBytes(key.getBytes(StandardCharsets.UTF_8)));
      mc.getTextureManager().register(target,new DynamicTexture(result));result=null;CACHE.put(key,target);RETIRED.remove(target);
      if(CACHE.size()>512){var oldest=CACHE.entrySet().iterator();RETIRED.add(oldest.next().getValue());oldest.remove();}
      return target;
    }catch(IOException error){if(result!=null)result.close();throw new IllegalStateException("Cannot build gradual coat "+profile,error);}
  }

  private static float[][] palette(String key,float sf,float rp)throws IOException {
    if(profiles==null){
      profiles=new HashMap<>();coreSwatches=new HashMap<>();
      try(var reader=Minecraft.getInstance().getResourceManager().openAsReader(id("gradual_evolution_palettes_021.json"))){
        var data=JsonParser.parseReader(reader).getAsJsonObject();
        if(data.has("hero_core_swatch"))for(var entry:data.getAsJsonObject("hero_core_swatch").entrySet()) {
          var pixel=entry.getValue().getAsJsonObject().getAsJsonArray("pixel");coreSwatches.put(entry.getKey(),new int[]{pixel.get(0).getAsInt(),pixel.get(1).getAsInt()});
        }
        for(var entry:data.getAsJsonObject("profiles").entrySet()) {
          float[][][] colors=new float[9][3][3];var values=entry.getValue().getAsJsonArray();
          for(int i=0;i<9;i++)for(int kind=0;kind<3;kind++) {
            var rgb=values.get(i).getAsJsonObject().getAsJsonArray(new String[]{"body","accent","wings"}[kind]);
            for(int c=0;c<3;c++)colors[i][kind][c]=rgb.get(c).getAsFloat();
          }profiles.put(entry.getKey(),colors);
        }
      }
    }
    var source=profiles.get(key);if(source==null)throw new IOException("Missing gradual palette "+key);
    float[][] out=new float[3][3];
    for(int y=0;y<3;y++)for(int x=0;x<3;x++){
      float weight=Math.max(0,1-Math.abs(sf-(y-1)))*Math.max(0,1-Math.abs(rp-(x-1)));
      for(int kind=0;kind<3;kind++)for(int c=0;c<3;c++)out[kind][c]+=source[y*3+x][kind][c]*weight;
    }return out;
  }
  @SubscribeEvent public static void endFrame(RenderFrameEvent.Post event){
    var textures=Minecraft.getInstance().getTextureManager();for(var id:RETIRED)if(!CACHE.containsValue(id))textures.release(id);RETIRED.clear();
  }
  private static void clear(){
    var textures=Minecraft.getInstance().getTextureManager();for(var id:CACHE.values())textures.release(id);for(var id:RETIRED)textures.release(id);
    CACHE.clear();RETIRED.clear();profiles=null;coreSwatches=null;
  }
  @EventBusSubscriber(modid=ChaoGarden.MODID,value=Dist.CLIENT,bus=EventBusSubscriber.Bus.MOD)
  public static final class Reload {
    @SubscribeEvent public static void register(RegisterClientReloadListenersEvent event){event.registerReloadListener((ResourceManagerReloadListener)manager->Minecraft.getInstance().execute(GradualCoatMaterial::clear));}
  }
}
