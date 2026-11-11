package com.chaogarden.client;

import com.chaogarden.ChaoGarden;
import com.chaogarden.core.ChaoState;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.logging.LogUtils;
import java.io.IOException;
import java.util.List;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

/** Actual resource/renderer contracts; excluded from the installed mod. */
@EventBusSubscriber(modid=ChaoGarden.MODID,value=Dist.CLIENT)
public final class ProportionsVisualSmokePoseProbe {
  private static final String[] KEYS={"baby","swim","fly","run","power","hero","hero_swim","hero_fly","hero_run","hero_power","dark","dark_swim","dark_fly","dark_run","dark_power"};
  private static boolean done;
  private ProportionsVisualSmokePoseProbe(){}

  @SubscribeEvent public static void tick(ClientTickEvent.Post event){
    if(done||!Boolean.getBoolean("chao_garden.audit022Study")||Minecraft.getInstance().level==null)return;
    done=true;
    LogUtils.getLogger().info("CHAO_PROPORTIONS_022_PROBE_COMPLETE: {} checks; 15 families x 9 endpoints x 3 ages x 5 poses; fixed eyes/native source rigs, reversible age-aware marker gaps and nonaccumulating offsets",inspect());
  }

  private static void require(boolean value,String why){
    if(!value)throw new IllegalStateException("Chao proportions: "+why);
  }
  private static void near(float actual,float expected,String why){
    require(Math.abs(actual-expected)<.0002f,why+" actual="+actual+" expected="+expected);
  }
  private static JsonObject resource(String path){
    var id=ResourceLocation.fromNamespaceAndPath(ChaoGarden.MODID,"models/entity/"+path+".json");
    try(var reader=Minecraft.getInstance().getResourceManager().openAsReader(id)){
      return JsonParser.parseReader(reader).getAsJsonObject();
    }catch(IOException error){throw new IllegalStateException(error);}
  }
  private static float[] coreBounds(JsonObject data){
    float best=-1;float[] result=null;
    for(var element:data.getAsJsonArray("bones")){
      var bone=element.getAsJsonObject();if(!bone.get("name").getAsString().startsWith("skull__cube_"))continue;
      for(var value:bone.getAsJsonArray("meshes")){
        var mesh=value.getAsJsonObject();float[] low={Float.POSITIVE_INFINITY,Float.POSITIVE_INFINITY,Float.POSITIVE_INFINITY},high={Float.NEGATIVE_INFINITY,Float.NEGATIVE_INFINITY,Float.NEGATIVE_INFINITY};
        for(var p:mesh.getAsJsonArray("vertices"))for(int i=0;i<3;i++){
          float n=p.getAsJsonArray().get(i).getAsFloat();low[i]=Math.min(low[i],n);high[i]=Math.max(high[i],n);
        }
        float volume=(high[0]-low[0])*(high[1]-low[1])*(high[2]-low[2]);
        if(volume>best){best=volume;result=new float[]{low[0],low[1],low[2],high[0],high[1],high[2]};}
      }
    }
    require(best>0,"missing actual skull");return result;
  }

  static int inspect(){
    int checks=0;var rig=new OfficialChaoRig();
    var modes=List.of(OfficialChaoRig.Motion.CRAWL,OfficialChaoRig.Motion.WALK,
        OfficialChaoRig.Motion.SIT,OfficialChaoRig.Motion.HELD,OfficialChaoRig.Motion.GLIDE);
    for(int key=0;key<KEYS.length;key++){
      String model=KEYS[key],donor=OfficialChaoRig.sourceGeometryKey(model);
      float[] originalBounds=coreBounds(resource("official/chao_"+donor));
      float original=originalBounds[4]-originalBounds[1];
      float target=Math.min(6,original),baseDrop=original-target;
      for(int sf=-1;sf<=1;sf++)for(int rp=-1;rp<=1;rp++)for(float age:new float[]{0,.6f,1.2f})for(var mode:modes){
        var state=new ChaoState();state.adult=true;state.align=key/5==1?1:key/5==2?-1:0;
        state.primaryType=key%5;state.swimFly=sf;state.runPower=rp;state.setEvolutionStrength(age);
        rig.prepareDevelopment(model,state);
        rig.animateGameplay(model,new UUID(2200+key,(sf+1)*1000L+(rp+1)*100L+mode.ordinal()),mode,120,2,.08f,0,0,0,0,false);
        float baseline=rig.pose("emotion_dots").y;
        rig.development(state);
        float[] head=rig.attachmentBounds("skull");float height=head[4]-head[1];
        near(head[4],originalBounds[4],model+" lower head edge shifted");checks++;
        near(head[2],originalBounds[2],model+" fixed face plane shifted");checks++;
        require(height<=target*1.1101f&&height>=target*.9199f,model+" excessive age scaling");checks++;
        float expectedDrop=baseDrop*height/target;
        near(rig.pose("emotion_dots").y-baseline,expectedDrop,model+" marker follows corrected crown");checks++;
        float once=rig.pose("emotion_dots").y;
        rig.development(state);near(rig.pose("emotion_dots").y,once,model+" marker offset accumulated");checks++;
        state.setEvolutionStrength(age==0?1.2f:0);rig.development(state);
        state.setEvolutionStrength(age);rig.development(state);
        near(rig.pose("emotion_dots").y,once,model+" marker failed reversible age change");checks++;
        require(rig.finite(),model+" non-finite pose");checks++;
      }
    }
    // Raw comparison meshes carry no normalization metadata.
    for(String source:OfficialChaoRig.REFERENCE_MODELS){
      rig.animate(source,120,0,0,0,0,0,0);long before=rig.shapeSignature();var markerPose=rig.pose("emotion_dots");float marker=markerPose==null?0:markerPose.y;
      var state=new ChaoState();state.adult=true;state.setEvolutionStrength(1.2f);rig.development(state);
      require(before==rig.shapeSignature(),source+" raw comparison geometry altered");checks++;
      if(markerPose!=null){near(rig.pose("emotion_dots").y,marker,source+" raw comparison marker altered");checks++;}
    }
    return checks;
  }
}
