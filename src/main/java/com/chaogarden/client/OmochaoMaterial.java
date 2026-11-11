package com.chaogarden.client;

import com.chaogarden.ChaoGarden;
import com.chaogarden.ChaoVariants;
import com.chaogarden.core.ChaoState;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.blaze3d.platform.NativeImage;
import java.io.IOException;
import java.io.InputStreamReader;
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

/** Builder coat pixels on a mechanical chassis; gold lenses and metal hardware stay fixed. */
@EventBusSubscriber(modid=ChaoGarden.MODID,value=Dist.CLIENT)
public final class OmochaoMaterial {
  private static final Map<String,ResourceLocation> CACHE=new LinkedHashMap<>(128,.75f,true);
  private static final Set<ResourceLocation> RETIRED=new HashSet<>();
  private OmochaoMaterial(){}
  private static ResourceLocation id(String path){return ResourceLocation.fromNamespaceAndPath(ChaoGarden.MODID,path);}

  /** Exposed for the visual audit: this is the resource actually sampled by the material. */
  public static ResourceLocation inheritedTexture(ChaoState state){
    String character=state.trait("robot.source_character");
    if(character.equals("amy"))return id("textures/entity/chao/gameplay/robot_coat_amy.png");
    if(character.equals("tails")||character.equals("knuckles"))
      return id("textures/entity/chao/official/coats/baby/normal_neutral_0_two.png");
    // Match the builder renderer's precedence: a Chaos form keeps its family
    // coat even if a dormant mod-theme identity is still present in its DNA.
    if(state.intTrait("robot.source_chaos")==1){
      var source=coatState(state);source.immortal=true;
      return id("textures/entity/chao/official/"+ChaoRenderer.textureKey(source)+".png");
    }
    String theme=ChaoVariants.activeId(state);
    if(!ChaoVariants.NORMAL.equals(theme))return id("textures/entity/chao/official/variant_"+theme+".png");
    // Rare character builders have their own fixed character palette. Amy uses
    // baby UVs; Tails/Knuckles are remapped to the robot's semantic skin below.
    var inherited=coatState(state);
    var raw=id("textures/entity/chao/official/coats/baby/"+ChaoRenderer.textureKey(inherited)+".png");
    return GradualCoatMaterial.texture(raw,inherited,"baby");
  }

  private static ChaoState coatState(ChaoState state){
    var source=new ChaoState();source.expressedColor=state.baseColor();source.expressedJewel=state.jewel();
    source.shiny=state.isShiny();source.twoTone=state.isTwoTone();source.align=state.align;
    source.adult=state.intTrait("robot.source_adult")==1;source.primaryType=state.primaryType;
    source.character="normal";source.immortal=false;source.variant="normal";
    source.swimFly=savedFloat(state,"robot.source_sf",-1,1);
    source.runPower=savedFloat(state,"robot.source_rp",-1,1);
    source.setEvolutionStrength(savedFloat(state,"robot.source_strength",0,1.2f));return source;
  }

  private static float savedFloat(ChaoState state,String key,float min,float max){
    try{float value=Float.parseFloat(state.trait(key));return Float.isFinite(value)?Math.max(min,Math.min(max,value)):0;}
    catch(RuntimeException invalid){return 0;}
  }
  record ImageSource(NativeImage pixels,boolean owned) implements AutoCloseable {
    @Override public void close(){if(owned)pixels.close();}
  }
  static ImageSource image(ResourceLocation source) throws IOException {
    var mc=Minecraft.getInstance();
    if(mc.getResourceManager().getResource(source).isPresent())try(var input=mc.getResourceManager().open(source)){
      return new ImageSource(NativeImage.read(input),true);
    }
    if(mc.getTextureManager().getTexture(source) instanceof DynamicTexture dynamic&&dynamic.getPixels()!=null)
      return new ImageSource(dynamic.getPixels(),false);
    throw new IOException("Missing inherited robot coat "+source);
  }

  public static ResourceLocation texture(ChaoState state){
    ResourceLocation source=inheritedTexture(state);
    String character=state.trait("robot.source_character");
    int brush=state.intTrait("robot.source_adult")==1&&state.align>=.5f&&state.intTrait("robot.source_chaos")==0
        &&character.equals("normal")?Math.max(0,Math.min(4,state.heroBrushType)):0;
    String key=source+":"+state.twoTone+":"+state.negativePalette+":"+state.brightCoat+":"+brush+":"+character;
    var cached=CACHE.get(key);if(cached!=null)return cached;
    var mc=Minecraft.getInstance();NativeImage image=null;
    try(var templateInput=mc.getResourceManager().open(id("textures/entity/chao/gameplay/character_omochao.png"));
        var inherited=image(source);
        var maskInput=mc.getResourceManager().open(id("textures/entity/chao/gameplay/masks/baby.png"));
        var accentInput=mc.getResourceManager().open(id("textures/entity/chao/gameplay/masks/baby_accent.png"));
        var paletteInput=mc.getResourceManager().open(id("omochao_palette_020.json"));
        var reader=new InputStreamReader(paletteInput,StandardCharsets.UTF_8);
        var mask=NativeImage.read(maskInput);var accents=NativeImage.read(accentInput)){
      var coat=inherited.pixels();
      image=NativeImage.read(templateInput);JsonObject palette=JsonParser.parseReader(reader).getAsJsonObject();
      Map<Integer,Integer> counts=new HashMap<>();
      int characterCoat=character.equals("tails")?0xff31a0eb:character.equals("knuckles")?0xff4440c8:0;
      for(int y=0;y<image.getHeight();y++)for(int x=0;x<image.getWidth();x++){
        int mx=x*mask.getWidth()/image.getWidth(),my=y*mask.getHeight()/image.getHeight();
        if((mask.getPixelRGBA(mx,my)>>>24)==0)continue;
        int rgba=coat.getPixelRGBA(x*coat.getWidth()/image.getWidth(),y*coat.getHeight()/image.getHeight());
        if((rgba>>>24)==0)continue;
        if(characterCoat!=0)rgba=state.twoTone&&(rgba&255)>(rgba>>>16&255)?0xffd8e8ef:characterCoat;
        boolean accent=brush!=0&&(accents.getPixelRGBA(mx,my)>>>24)!=0;
        rgba=effect(rgba,state.negativePalette,state.brightCoat,accent?brush:0);
        image.setPixelRGBA(x,y,rgba);counts.merge(rgba&0xffffff,1,Integer::sum);
      }
      int primary=counts.entrySet().stream().max(Map.Entry.comparingByValue()).map(Map.Entry::getKey).orElse(0xd8ae53);
      for(var entry:palette.getAsJsonObject("variable_pixels").entrySet()){
        var xy=entry.getValue().getAsJsonArray();float shade=switch(entry.getKey()){
          case "plate_dark"->state.twoTone?.57f:.91f;case "plate_light"->1.13f;default->1f;};
        image.setPixelRGBA(xy.get(0).getAsInt(),xy.get(1).getAsInt(),shade(primary,shade));
      }
      var result=id("dynamic/omochao/"+UUID.nameUUIDFromBytes(key.getBytes(StandardCharsets.UTF_8)));
      mc.getTextureManager().register(result,new DynamicTexture(image));image=null;
      CACHE.put(key,result);RETIRED.remove(result);
      if(CACHE.size()>256){var oldest=CACHE.entrySet().iterator();RETIRED.add(oldest.next().getValue());oldest.remove();}
      return result;
    }catch(IOException|RuntimeException error){
      if(image!=null)image.close();throw new IllegalStateException("Cannot inherit Omochao coat from "+source,error);
    }
  }

  private static int effect(int rgba,boolean negative,boolean bright,int brush){
    int red=rgba&255,green=rgba>>>8&255,blue=rgba>>>16&255;
    if(brush!=0){
      int[] rgb=switch(brush){case 1->new int[]{246,214,79};case 2->new int[]{161,91,222};case 3->new int[]{80,192,115};default->new int[]{222,73,83};};
      float light=.7f+Math.max(red,Math.max(green,blue))/255f*.3f;
      red=Math.round(rgb[0]*light);green=Math.round(rgb[1]*light);blue=Math.round(rgb[2]*light);
    }
    if(negative){red=255-red;green=255-green;blue=255-blue;}
    if(bright){red=Math.min(255,Math.round(28+red*1.18f));green=Math.min(255,Math.round(28+green*1.18f));blue=Math.min(255,Math.round(28+blue*1.18f));}
    return rgba&0xff000000|blue<<16|green<<8|red;
  }
  private static int shade(int color,float amount){
    int red=Math.min(255,Math.round((color&255)*amount)),green=Math.min(255,Math.round((color>>>8&255)*amount)),blue=Math.min(255,Math.round((color>>>16&255)*amount));
    return 0xff000000|blue<<16|green<<8|red;
  }
  @SubscribeEvent public static void endFrame(RenderFrameEvent.Post event){
    var textures=Minecraft.getInstance().getTextureManager();
    for(var id:RETIRED)if(!CACHE.containsValue(id))textures.release(id);RETIRED.clear();
  }
  private static void clear(){
    var textures=Minecraft.getInstance().getTextureManager();for(var id:CACHE.values())textures.release(id);for(var id:RETIRED)textures.release(id);
    CACHE.clear();RETIRED.clear();
  }
  @EventBusSubscriber(modid=ChaoGarden.MODID,value=Dist.CLIENT,bus=EventBusSubscriber.Bus.MOD)
  public static final class Reload{
    @SubscribeEvent public static void register(RegisterClientReloadListenersEvent event){
      event.registerReloadListener((ResourceManagerReloadListener)manager->Minecraft.getInstance().execute(OmochaoMaterial::clear));
    }
  }
}
