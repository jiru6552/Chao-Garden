package com.chaogarden.client;

import com.chaogarden.ChaoGarden;
import com.chaogarden.core.ChaoState;
import com.google.gson.JsonParser;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.logging.LogUtils;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

/** Development-only audit of the actual renderer-selected dynamic material. */
@EventBusSubscriber(modid=ChaoGarden.MODID,value=Dist.CLIENT)
public final class OmochaoVisualSmokePoseProbe {
  private static boolean done;
  private OmochaoVisualSmokePoseProbe(){}
  @SubscribeEvent public static void tick(ClientTickEvent.Post event){
    if(done||!Boolean.getBoolean("chao_garden.audit022Study")||Minecraft.getInstance().level==null)return;
    done=true;LogUtils.getLogger().info("CHAO_OMOCHAO_021_MATERIAL_PROBE_COMPLETE: {} actual renderer material samples; inherited pixels, gradual builder palette, fixed hardware, shine and cache identity verified",inspect());
  }
  private static void require(boolean condition,String message){if(!condition)throw new IllegalStateException("Omochao material: "+message);}
  private static ChaoState robot(int color,boolean tone,int jewel){
    var s=ChaoState.egg(color,false,tone,jewel,new Random(20));var creator=new ChaoState();
    com.chaogarden.core.OmochaoRules.initialize(s,creator,"Builder",2);
    s.setTrait("robot.source_character","normal");s.setTrait("robot.source_adult",1);return s;
  }
  private static NativeImage pixels(ChaoState s){
    var id=OfficialChaoRig.texture(s);var texture=Minecraft.getInstance().getTextureManager().getTexture(id);
    require(texture instanceof DynamicTexture,"robot still selects a fixed PNG");return ((DynamicTexture)texture).getPixels();
  }
  private static long hash(NativeImage image){
    long h=1;for(int y=0;y<image.getHeight();y++)for(int x=0;x<image.getWidth();x++)h=h*31+image.getPixelRGBA(x,y);return h;
  }
  public static int inspect(){
    var mc=Minecraft.getInstance();var samples=new ArrayList<ChaoState>();
    samples.add(robot(2,true,0));samples.add(robot(4,true,0));samples.add(robot(4,false,0));
    samples.add(robot(0,true,1));samples.add(robot(0,true,4));
    var nether=robot(0,true,0);nether.variant="nether";samples.add(nether);
    var end=robot(0,true,0);end.variant="end";samples.add(end);
    var negative=robot(2,true,0);negative.negativePalette=true;samples.add(negative);
    var bright=robot(2,true,0);bright.brightCoat=true;samples.add(bright);
    var hero=robot(0,true,0);hero.align=1;hero.heroBrushType=2;samples.add(hero);
    for(int alignment=-1;alignment<=1;alignment++){
      var aged=robot(0,true,0);aged.align=alignment;aged.primaryType=ChaoState.RUN_TYPE;
      aged.setTrait("robot.source_sf",Float.toHexString(1));aged.setTrait("robot.source_rp",Float.toHexString(-1));
      aged.setTrait("robot.source_strength",Float.toHexString(1.2f));samples.add(aged);
    }
    Set<Long> hashes=new HashSet<>();int sampledPixels=0;
    try(var input=mc.getResourceManager().open(ResourceLocation.fromNamespaceAndPath(ChaoGarden.MODID,"omochao_palette_020.json"));
        var reader=new InputStreamReader(input,StandardCharsets.UTF_8);
        var templateInput=mc.getResourceManager().open(ResourceLocation.fromNamespaceAndPath(ChaoGarden.MODID,"textures/entity/chao/gameplay/character_omochao.png"));
        var maskInput=mc.getResourceManager().open(ResourceLocation.fromNamespaceAndPath(ChaoGarden.MODID,"textures/entity/chao/gameplay/masks/baby.png"));
        var template=NativeImage.read(templateInput);var mask=NativeImage.read(maskInput)){
      var palette=JsonParser.parseReader(reader).getAsJsonObject();
      for(var sample:samples){
        require(OfficialChaoRig.geometryKey(sample).equals("omochao"),"inherited coat changed the mechanical skeleton");
        var actual=pixels(sample);require(hashes.add(hash(actual)),"different builder coats rendered identically");
        for(var entry:palette.getAsJsonObject("fixed_pixels").entrySet()){
          var point=entry.getValue().getAsJsonArray();int x=point.get(0).getAsInt(),y=point.get(1).getAsInt();
          require(actual.getPixelRGBA(x,y)==template.getPixelRGBA(x,y),"inherited pigment replaced hardware "+entry.getKey());
        }
      }
      // Compare every visible skin texel of one ordinary robot to the source
      // image selected by the runtime, not a test recreation of its palette.
      var sample=samples.getFirst();var actual=pixels(sample);
      try(var inherited=OmochaoMaterial.image(OmochaoMaterial.inheritedTexture(sample))){
        var coat=inherited.pixels();
        for(int y=0;y<actual.getHeight();y++)for(int x=0;x<actual.getWidth();x++)if((mask.getPixelRGBA(x,y)>>>24)!=0&&(coat.getPixelRGBA(x,y)>>>24)!=0){
          require(actual.getPixelRGBA(x,y)==coat.getPixelRGBA(x,y),"builder coat texel lost on mechanical skin");sampledPixels++;
        }
      }
      // One fully-aged ordinary builder must also keep its new dynamic palette
      // on the mechanical UV layout, without replacing lenses or hardware.
      var aged=samples.getLast();var agedPixels=pixels(aged);
      try(var inherited=OmochaoMaterial.image(OmochaoMaterial.inheritedTexture(aged))){
        var coat=inherited.pixels();
        for(int y=0;y<agedPixels.getHeight();y++)for(int x=0;x<agedPixels.getWidth();x++)if((mask.getPixelRGBA(x,y)>>>24)!=0&&(coat.getPixelRGBA(x,y)>>>24)!=0)
          require(agedPixels.getPixelRGBA(x,y)==coat.getPixelRGBA(x,y),"fully aged dynamic builder palette was lost");
      }
      require(sampledPixels>500,"too little actual chassis skin was checked");
      require(!ChaoShine.visible(samples.getFirst())&&ChaoShine.visible(samples.get(3)),"jewel material lost its real shine layer");
      var shiny=robot(2,true,0);shiny.shiny=true;require(ChaoShine.visible(shiny),"shiny robot lost reflection layer");pixels(shiny);
      require(OfficialChaoRig.texture(samples.getFirst()).equals(OfficialChaoRig.texture(samples.getFirst())),"material cache unstable");
      return samples.size()+1;
    }catch(java.io.IOException e){throw new IllegalStateException("Omochao material audit",e);}
  }
}
