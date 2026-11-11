package com.chaogarden.client;

import com.chaogarden.ChaoGarden;
import com.chaogarden.entity.ChaoEntity;
import com.chaogarden.toys.*;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
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

@EventBusSubscriber(modid=ChaoGarden.MODID,value=Dist.CLIENT,bus=EventBusSubscriber.Bus.MOD)
final class ChaoMaracasAnimation {
  static final ModelResourceLocation HAND=ModelResourceLocation.standalone(ResourceLocation.fromNamespaceAndPath(ChaoGarden.MODID,"item/sa2_02515/chao_maraca_hand"));
  @SubscribeEvent public static void models(ModelEvent.RegisterAdditional event){event.register(HAND);}
  static boolean active(ChaoEntity chao){return InstrumentPerformance.active(chao,MaracasPlayRules.KIND);}
  static float elapsed(ChaoEntity chao,float partial){return InstrumentPerformance.phase(chao,partial);}
  static boolean happy(ChaoEntity chao,float partial){return active(chao)&&MaracasPlayRules.happy(elapsed(chao,partial));}
  static void applyArm(ChaoMeshAsset.Pose arm,LionfishMaracasClip clip,float tick,boolean left){
    applyArm(arm,clip,tick,left,MaracasPlayRules.presence(tick));
  }
  static void applyArm(ChaoMeshAsset.Pose arm,LionfishMaracasClip clip,float tick,boolean left,float weight){
    var value=clip.sample(tick,left);
    arm.rx=arm.rx*(1-weight)+value.x*weight;arm.ry=arm.ry*(1-weight)+value.y*weight;arm.rz=arm.rz*(1-weight)+value.z*weight;
    arm.x+=(left?1:-1)*.5f*weight;arm.z-=.65f*weight;
  }
  static void pose(ChaoEntity chao,ChaoModel model,LionfishMaracasClip clip,float partial){
    if(!active(chao))return;
    for(boolean left:new boolean[]{false,true})if(model.toyPalm(left)!=null)
      applyArm(model.officialPoses().get(left?"arm_left":"arm_right"),clip,elapsed(chao,partial),left,InstrumentPerformance.presence(chao,partial));
  }
  static void propTransform(PoseStack stack,float[] palm,float presence,boolean left){
    stack.translate(palm[0]/16f,palm[1]/16f,palm[2]/16f);
    stack.mulPose(Axis.ZP.rotationDegrees(left?35:-35));stack.mulPose(Axis.XP.rotationDegrees(180));
    float scale=.65f*presence;stack.scale(scale,scale,scale);stack.translate(0,.20f,0);
  }
  static boolean render(ChaoEntity chao,ChaoModel model,PoseStack stack,MultiBufferSource buffers,int light,float partial){
    if(!active(chao)||chao.isInvisible()||!model.officialActive())return false;
    float weight=InstrumentPerformance.presence(chao,partial);if(weight<=0)return true;
    var mc=Minecraft.getInstance();var baked=mc.getModelManager().getModel(HAND);var item=new ItemStack(ChaoGarden.item(MaracasPlayRules.KIND));
    for(boolean left:new boolean[]{false,true}){
      float[] palm=model.toyPalm(left);if(palm==null)continue;stack.pushPose();
      try{model.transformAccessoryAnchor(left?"arm_left":"arm_right",stack);propTransform(stack,palm,weight,left);
        mc.getItemRenderer().render(item,ItemDisplayContext.NONE,false,stack,buffers,light,OverlayTexture.NO_OVERLAY,baked);
      }finally{stack.popPose();}
    }return true;
  }
}
