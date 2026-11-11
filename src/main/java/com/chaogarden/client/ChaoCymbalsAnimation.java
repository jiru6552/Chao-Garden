package com.chaogarden.client;
import com.chaogarden.ChaoGarden;
import com.chaogarden.entity.ChaoEntity;
import com.chaogarden.toys.*;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.*;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import java.util.Map;
import java.util.function.BiConsumer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ModelEvent;
@EventBusSubscriber(modid=ChaoGarden.MODID,value=Dist.CLIENT,bus=EventBusSubscriber.Bus.MOD)
final class ChaoCymbalsAnimation {
 static final Vector3f GRIP=new Vector3f(.65f,.20f,.90f),RIM=new Vector3f(.50000006f,.01851532f,.5f);
 static final float SCALE=.24f;
 static final ModelResourceLocation HAND=ModelResourceLocation.standalone(ResourceLocation.fromNamespaceAndPath(ChaoGarden.MODID,"item/sa2_02515/chao_cymbal_hand"));
 @SubscribeEvent public static void models(ModelEvent.RegisterAdditional e){e.register(HAND);}
 static boolean active(ChaoEntity c){return InstrumentPerformance.active(c,CymbalsPlayRules.KIND);}
 static boolean happy(ChaoEntity c,float p){return active(c)&&CymbalsPlayRules.happy(InstrumentPerformance.phase(c,p));}
 static void applyArm(ChaoMeshAsset.Pose arm,LionfishCymbalsClip clip,float t,float w,boolean left){
  var v=clip.sample(t,left);arm.rx=arm.rx*(1-w)+v.x*w;arm.ry=arm.ry*(1-w)+v.y*w;arm.rz=arm.rz*(1-w)+v.z*w;
  arm.x*=1-w;arm.y*=1-w;arm.z=arm.z*(1-w)-.85f*w;
 }
 static Vector3f palm(BiConsumer<String,PoseStack> anchor,float[] socket,boolean left){var s=new PoseStack();anchor.accept(left?"arm_left":"arm_right",s);return s.last().pose().transformPosition(new Vector3f(socket).div(16));}
 static Matrix4f orientation(boolean left){return new Matrix4f().rotateZ((float)Math.toRadians(left?-70:70)).rotateY(left?0:(float)Math.PI);}
 static float alignPair(Map<String,ChaoMeshAsset.Pose> poses,BiConsumer<String,PoseStack> anchor,float[] right,float[] left,float w,float contact){
  if(w<=0)return 0;var body=new PoseStack();anchor.accept("body",body);var matrix=body.last().pose();
  var inverse=new Matrix4f(matrix).invert();
  var shoulders=new Vector3f[2];float forward=Float.POSITIVE_INFINITY;
  float half=orientation(true).transformDirection(new Vector3f(GRIP).sub(RIM)).x*SCALE*propWeight(w)+.002f+.08f*(1-contact);
  float height=0;
  for(int i=0;i<2;i++){
   var arm=new PoseStack();anchor.accept(i==1?"arm_left":"arm_right",arm);shoulders[i]=inverse.transformPosition(arm.last().pose().transformPosition(new Vector3f()));
   height+=shoulders[i].y*.5f;
  }
  height+=1.05f/16;
  for(int i=0;i<2;i++){
   float length=new Vector3f(i==1?left:right).length()/16,dx=(i==1?half:-half)-shoulders[i].x,dy=height-shoulders[i].y;
   forward=Math.min(forward,(float)Math.sqrt(Math.max(.0001f,length*length-dx*dx-dy*dy)));
  }
  var center=new Vector3f(0,height,(shoulders[0].z+shoulders[1].z)*.5f-forward);
  float max=0;for(boolean side:new boolean[]{false,true}){
   var target=matrix.transformPosition(new Vector3f(center).add(side?half:-half,0,0));
   max=Math.max(max,ChaoTambourineAnimation.alignPalm(poses,anchor,side?left:right,1-(1-w)*(1-w)*(1-w),target,side,.4f));
  }return max;
 }
 static Vector3f grip(boolean left){return new Vector3f(GRIP.x,GRIP.y,left?GRIP.z:1-GRIP.z);}
 // The hands move clear of the face before the discs appear; reverse on exit.
 static float propWeight(float w){return Math.max(0,Math.min(1,(w-.75f)/.25f));}
 static Matrix4f propMatrix(BiConsumer<String,PoseStack> anchor,float[] socket,float w,boolean left){
  var body=new PoseStack();anchor.accept("body",body);var local=new Matrix4f(body.last().pose()).invert().transformPosition(palm(anchor,socket,left));
  return new Matrix4f(body.last().pose()).translate(local).mul(orientation(left)).scale(SCALE*propWeight(w)).translate(new Vector3f(.5f).sub(grip(left)));
 }
 static void pose(ChaoEntity c,ChaoModel m,LionfishCymbalsClip clip,float partial){
  if(!active(c)||m.toyPalm(false)==null||m.toyPalm(true)==null)return;float t=InstrumentPerformance.phase(c,partial),w=InstrumentPerformance.presence(c,partial);
  ChaoInstrumentFocus.apply(m.officialPoses(),w);
    for(boolean left:new boolean[]{false,true})applyArm(m.officialPoses().get(left?"arm_left":"arm_right"),clip,t,w,left);
  alignPair(m.officialPoses(),m::transformAccessoryAnchor,m.toyPalm(false),m.toyPalm(true),w,clip.contact(t));
 }
 static boolean render(ChaoEntity c,ChaoModel m,PoseStack stack,MultiBufferSource buffers,int light,float partial){
  if(!active(c)||c.isInvisible()||!m.officialActive()||m.toyPalm(false)==null||m.toyPalm(true)==null)return false;
  float w=InstrumentPerformance.presence(c,partial);if(w<=0)return true;var mc=Minecraft.getInstance();var item=new ItemStack(ChaoGarden.item(CymbalsPlayRules.KIND));
  for(boolean left:new boolean[]{false,true}){stack.pushPose();try{stack.mulPose(propMatrix(m::transformAccessoryAnchor,m.toyPalm(left),w,left));mc.getItemRenderer().render(item,ItemDisplayContext.NONE,false,stack,buffers,light,OverlayTexture.NO_OVERLAY,mc.getModelManager().getModel(HAND));}finally{stack.popPose();}}
  return true;
 }
}
