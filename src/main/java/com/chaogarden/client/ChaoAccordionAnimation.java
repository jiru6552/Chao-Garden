package com.chaogarden.client;

import com.chaogarden.ChaoGarden;
import com.chaogarden.entity.ChaoEntity;
import com.chaogarden.toys.*;
import com.mojang.blaze3d.vertex.PoseStack;
import java.util.Map;
import java.util.function.BiConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.*;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ModelEvent;
import org.joml.Matrix4f;
import org.joml.Vector3f;

/** The existing source accordion: rigid keys/bass, flexible pleats and fitted visible palms. */
@EventBusSubscriber(modid=ChaoGarden.MODID,value=Dist.CLIENT,bus=EventBusSubscriber.Bus.MOD)
final class ChaoAccordionAnimation {
  static final float SCALE=.38f,REST_WIDTH=.32f,MAX_REACH=2.8f;
  // Canonical source contacts after the measured fan has been straightened.
  static final Vector3f KEY=new Vector3f(-.37228706f,.12f,.043575f),
      BASS=new Vector3f(.27123733f,.04771982f,-.000055356f),
      BASS_NORMAL=new Vector3f(.90967636f,0,-.41531785f);
  static final ModelResourceLocation KEYS=model("keys"),BASS_END=model("bass"),BELLOWS=model("bellows");
  private static ModelResourceLocation model(String part){
    return ModelResourceLocation.standalone(ResourceLocation.fromNamespaceAndPath(ChaoGarden.MODID,"item/chao_accordion_hand_"+part));
  }
  @SubscribeEvent public static void models(ModelEvent.RegisterAdditional event){event.register(KEYS);event.register(BASS_END);event.register(BELLOWS);}
  static boolean active(ChaoEntity chao){return InstrumentPerformance.active(chao,AccordionPlayRules.KIND);}
  static boolean happy(ChaoEntity chao,float partial){return active(chao)&&AccordionPlayRules.happy(InstrumentPerformance.phase(chao,partial));}
  static float propWeight(float weight){return Math.clamp((weight-.45f)/.55f,0,1);}
  static float width(float bellows){return REST_WIDTH*(.65f+.65f*Math.clamp(bellows,0,1));}
  static Matrix4f propMatrix(BiConsumer<String,PoseStack> anchor,float weight,float bellows,String part){
    var body=new PoseStack();anchor.accept("body",body);
    var matrix=new Matrix4f(body.last().pose()).translate(-.015f,-.15f+.04f*(1-weight),-.29f-.06f*(1-weight))
        .rotateZ(-.025f).scale(SCALE*propWeight(weight));
    float change=(width(bellows)-REST_WIDTH)*.5f;
    return switch(part){
      case "keys"->matrix.translate(-change,0,0);
      case "bass"->matrix.translate(change,0,0);
      case "bellows"->matrix.scale(width(bellows)/REST_WIDTH,1,1);
      default->throw new IllegalArgumentException("Unknown accordion part "+part);
    };
  }
  static Vector3f keyTarget(Matrix4f keys,float height,float press){
    // Palm centers sit just in front of the actual white/black key plane.
    return keys.transformPosition(new Vector3f(KEY).add(0,.53f-height,-.055f+.016f*press));
  }
  static Vector3f bassTarget(Matrix4f bass,float press){
    // Support the actual outer side of the plain bass end. This keeps the
    // wrist close to the body while the rigid end follows the bellows.
    return bass.transformPosition(new Vector3f(BASS).fma(.045f-.005f*press,BASS_NORMAL).add(0,.006f*press,0));
  }
  private static void fit(Map<String,ChaoMeshAsset.Pose> poses,BiConsumer<String,PoseStack> anchor,
                          float[] palm,boolean left,float weight,Vector3f target){
    var arm=poses.get(left?"arm_left":"arm_right");var shoulder=new PoseStack();
    anchor.accept(left?"arm_left":"arm_right",shoulder);
    float length=shoulder.last().pose().transformPosition(new Vector3f()).distance(target)*16;
    arm.reach=Math.clamp((float)Math.sqrt(Math.max(0,length*length-palm[1]*palm[1]-palm[2]*palm[2]))-Math.abs(palm[0])-.20f,0,MAX_REACH)*weight;
    ChaoTambourineAnimation.alignPalm(poses,anchor,ChaoTrumpetAnimation.reachedPalm(palm,arm.reach,left),weight,target,left,.30f);
  }
  static void pose(Map<String,ChaoMeshAsset.Pose> poses,BiConsumer<String,PoseStack> anchor,
                   float[] right,float[] left,float weight,float bellows,float keys,float bass,float height){
    ChaoInstrumentFocus.apply(poses,weight);
    poses.get("skull").rx+=.025f*weight;poses.get("skull").rz+=.015f*(bellows-.5f)*weight;
    for(boolean side:new boolean[]{false,true}){
      var arm=poses.get(side?"arm_left":"arm_right");
      arm.x*=1-weight;arm.y*=1-weight;arm.z=arm.z*(1-weight)-.25f*weight;
    }
    float ease=weight*weight*(3-2*weight);
    fit(poses,anchor,right,false,ease,keyTarget(propMatrix(anchor,weight,bellows,"keys"),height,keys));
    fit(poses,anchor,left,true,ease,bassTarget(propMatrix(anchor,weight,bellows,"bass"),bass));
  }
  static void pose(ChaoEntity chao,ChaoModel model,LionfishAccordionClip clip,float partial){
    if(!active(chao)||model.baseToyPalm(false)==null||model.baseToyPalm(true)==null)return;
    float tick=InstrumentPerformance.phase(chao,partial),weight=InstrumentPerformance.presence(chao,partial);
    pose(model.officialPoses(),model::transformAccessoryAnchor,model.baseToyPalm(false),model.baseToyPalm(true),
        weight,clip.bellows(tick),clip.keys(tick),clip.bass(tick),clip.keyHeight(tick));
  }
  static boolean render(ChaoEntity chao,ChaoModel model,PoseStack stack,MultiBufferSource buffers,int light,float partial){
    if(!active(chao)||chao.isInvisible()||!model.officialActive()||model.toyPalm(false)==null||model.toyPalm(true)==null)return false;
    float weight=InstrumentPerformance.presence(chao,partial);if(propWeight(weight)<=0)return true;
    float bellows=model.accordionBellows(InstrumentPerformance.phase(chao,partial));
    var minecraft=Minecraft.getInstance();var item=new ItemStack(ChaoGarden.item(AccordionPlayRules.KIND));
    String[] parts={"keys","bass","bellows"};ModelResourceLocation[] models={KEYS,BASS_END,BELLOWS};
    for(int i=0;i<parts.length;i++){
      stack.pushPose();try{
        stack.mulPose(propMatrix(model::transformAccessoryAnchor,weight,bellows,parts[i]));
        minecraft.getItemRenderer().render(item,ItemDisplayContext.NONE,false,stack,buffers,light,OverlayTexture.NO_OVERLAY,minecraft.getModelManager().getModel(models[i]));
      }finally{stack.popPose();}
    }
    return true;
  }
}
