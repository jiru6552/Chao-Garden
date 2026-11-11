package com.chaogarden.client;

import com.chaogarden.ChaoGarden;
import com.chaogarden.core.ChaoState;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.logging.LogUtils;
import java.util.*;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

/** Actual selected texture pixels, kept outside the production JAR. */
@EventBusSubscriber(modid=ChaoGarden.MODID,value=Dist.CLIENT)
public final class GradualVisualSmokePoseProbe {
  private static boolean done;
  private GradualVisualSmokePoseProbe(){}
  private static void require(boolean ok,String why){if(!ok)throw new IllegalStateException("Gradual coat audit: "+why);}
  private static long hash(NativeImage image){long h=1;for(int y=0;y<image.getHeight();y++)for(int x=0;x<image.getWidth();x++)h=h*31+image.getPixelRGBA(x,y);return h;}
  @SubscribeEvent public static void tick(ClientTickEvent.Post event){
    if(done||!Boolean.getBoolean("chao_garden.audit022Study")||Minecraft.getInstance().level==null)return;
    done=true;int count=inspect();LogUtils.getLogger().info("CHAO_GRADUAL_COAT_021_PROBE_COMPLETE: {} actual material cases; 15 primary palettes x 9 secondary endpoints x 3 ages, protected eyes/alpha, inherited colored/jewel coats and effect-cache isolation verified",count);
  }
  public static int inspect(){
    var mc=Minecraft.getInstance();int count=0;Set<Long> aged=new HashSet<>();
    for(int alignment=-1;alignment<=1;alignment++)for(int primary=0;primary<5;primary++){
      var s=new ChaoState();s.adult=true;s.align=alignment;s.primaryType=primary;s.twoTone=true;s.setEvolutionStrength(0);
      String layout=OfficialChaoRig.sourceGeometryKey(OfficialChaoRig.geometryKey(s));
      var maskId=ResourceLocation.fromNamespaceAndPath(ChaoGarden.MODID,"textures/entity/chao/gameplay/masks/"+layout+".png");
      var wingId=ResourceLocation.fromNamespaceAndPath(ChaoGarden.MODID,"textures/entity/chao/gameplay/masks/"+layout+"_wings.png");
      try(var input=mc.getResourceManager().open(maskId);var wingInput=mc.getResourceManager().open(wingId);var mask=NativeImage.read(input);var wings=NativeImage.read(wingInput);var original=OmochaoMaterial.image(OfficialChaoRig.texture(s))){
        var young=original.pixels();long youngHash=hash(young);
        for(int sf=-1;sf<=1;sf++)for(int rp=-1;rp<=1;rp++){
          s.swimFly=sf;s.runPower=rp;long middle=0,full=0;
          for(float strength:new float[]{0,.6f,1.2f}){
            s.setEvolutionStrength(strength);
            try(var selected=OmochaoMaterial.image(OfficialChaoRig.texture(s))){
              var pixels=selected.pixels();long fingerprint=hash(pixels);
              if(strength==0)require(fingerprint==youngHash,"young color depends on future secondary");
              if(strength==.6f)middle=fingerprint;if(strength==1.2f)full=fingerprint;
              for(int y=0;y<pixels.getHeight();y++)for(int x=0;x<pixels.getWidth();x++){
                int mx=x*mask.getWidth()/pixels.getWidth(),my=y*mask.getHeight()/pixels.getHeight();
                int a=pixels.getPixelRGBA(x,y),b=young.getPixelRGBA(x,y);
                require((a>>>24)==(b>>>24),"age changed inherited texture alpha");
                if((mask.getPixelRGBA(mx,my)>>>24)==0&&(wings.getPixelRGBA(mx,my)>>>24)==0)require(a==b,"age changed protected eye/non-skin texel");
              }
              if(alignment==0&&primary==ChaoState.FLY_TYPE&&sf==1&&rp==0&&strength==1.2f){
                long red=0,green=0,blue=0;int seen=0;
                for(int y=0;y<pixels.getHeight();y++)for(int x=0;x<pixels.getWidth();x++){
                  int mx=x*wings.getWidth()/pixels.getWidth(),my=y*wings.getHeight()/pixels.getHeight();int a=pixels.getPixelRGBA(x,y);
                  if((wings.getPixelRGBA(mx,my)>>>24)!=0&&(a>>>24)!=0){red+=a&255;green+=a>>>8&255;blue+=a>>>16&255;seen++;}
                }
                require(seen>20&&red>blue*2&&green>blue*2,"mature NiGHTS wings remained green instead of yellow");
              }count++;
            }
          }
          require(full!=youngHash&&middle!=youngHash&&middle!=full,"natural coat failed to grow gradually");aged.add(full);
        }
      }catch(java.io.IOException e){throw new IllegalStateException("Missing gradual texture",e);}
    }
    require(aged.size()>=100,"primary-only textures reused across secondary matrix");
    for(int color:new int[]{2,4,6})for(int jewel:new int[]{0,1,13}){
      var s=new ChaoState();s.adult=true;s.expressedColor=color;s.expressedJewel=jewel;s.twoTone=true;s.primaryType=ChaoState.RUN_TYPE;
      s.setEvolutionStrength(0);var a=OfficialChaoRig.texture(s);s.setEvolutionStrength(1.2f);s.runPower=-1;
      require(a.equals(OfficialChaoRig.texture(s)),"gradual palette overwrote inherited colored/jewel coat");count++;
    }
    var s=new ChaoState();s.adult=true;s.twoTone=true;s.primaryType=ChaoState.RUN_TYPE;s.runPower=-1;s.setEvolutionStrength(1.2f);
    var base=OfficialChaoRig.texture(s);
    try(var image=OmochaoMaterial.image(base)){
      long before=hash(image.pixels());s.negativePalette=true;var negative=OfficialChaoRig.texture(s);
      require(!negative.equals(base),"negative effect failed on dynamic aged coat");
      s.negativePalette=false;s.brightCoat=true;var bright=OfficialChaoRig.texture(s);
      require(!bright.equals(base)&&!bright.equals(negative),"Bright effect failed on dynamic aged coat");
      require(hash(image.pixels())==before,"palette effects mutated shared gradual texture");count+=2;
    }catch(java.io.IOException e){throw new IllegalStateException(e);}
    // Verify the actual natural-only core UV override and its isolated palette
    // swatch, including effect order; inherited coats must retain their UVs.
    try(var reader=mc.getResourceManager().openAsReader(ResourceLocation.fromNamespaceAndPath(ChaoGarden.MODID,"models/entity/gameplay/chao_hero_swim.json"))){
      var metadata=com.google.gson.JsonParser.parseReader(reader).getAsJsonObject().getAsJsonObject("natural_hero_core");
      var point=metadata.getAsJsonArray("pixel");int x=point.get(0).getAsInt(),y=point.get(1).getAsInt();
      var h=new ChaoState();h.adult=true;h.align=1;h.primaryType=ChaoState.SWIM_TYPE;h.twoTone=true;h.setEvolutionStrength(0);
      var rig=new OfficialChaoRig();rig.prepareDevelopment("hero_swim",h);require(rig.naturalHeroCoreActive(),"natural Hero Swim retained donor teal headband");
      try(var pixels=OmochaoMaterial.image(OfficialChaoRig.texture(h))){require(pixels.pixels().getPixelRGBA(x,y)==0xffffffff,"natural Hero Swim core is not white");}count++;
      h.negativePalette=true;
      try(var pixels=OmochaoMaterial.image(OfficialChaoRig.texture(h))){require(pixels.pixels().getPixelRGBA(x,y)==0xff000000,"negative core effect applied incorrectly");}count++;
      h.brightCoat=true;
      try(var pixels=OmochaoMaterial.image(OfficialChaoRig.texture(h))){require(pixels.pixels().getPixelRGBA(x,y)==0xff1c1c1c,"Bright after negative core effect applied incorrectly");}count++;
      h.expressedColor=2;rig.prepareDevelopment("hero_swim",h);require(!rig.naturalHeroCoreActive(),"white core overwrote colored genes");count++;
      h.expressedColor=0;h.expressedJewel=1;rig.prepareDevelopment("hero_swim",h);require(!rig.naturalHeroCoreActive(),"white core overwrote jewel genes");count++;
    }catch(java.io.IOException e){throw new IllegalStateException(e);}
    return count;
  }
}
