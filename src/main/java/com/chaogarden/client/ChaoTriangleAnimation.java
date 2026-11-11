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
import org.joml.Quaternionf;
import java.util.Map;
import java.util.function.BiConsumer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ModelEvent;
/** Left hand suspends the triangle; the separate right-hand beater strikes its lower rail. */
@EventBusSubscriber(modid=ChaoGarden.MODID,value=Dist.CLIENT,bus=EventBusSubscriber.Bus.MOD)
final class ChaoTriangleAnimation {
 static final Vector3f GRIP=new Vector3f(7.9f,12.5f,6.25f).div(16),HIT=new Vector3f(5.5f,2.35f,7.2f).div(16);
 static final Vector3f STICK_GRIP=new Vector3f(5,1.3f,10.3f).div(16),STICK_TIP=new Vector3f(12,1.3f,10.3f).div(16);
 static final float SCALE=.30f,STICK_SCALE=.45f;
 static final ModelResourceLocation BODY=model("body"),BEATER=model("beater");
 private static ModelResourceLocation model(String part){return ModelResourceLocation.standalone(ResourceLocation.fromNamespaceAndPath(ChaoGarden.MODID,"item/chao_triangle_hand_"+part));}
 @SubscribeEvent public static void models(ModelEvent.RegisterAdditional e){e.register(BODY);e.register(BEATER);}
 static boolean active(ChaoEntity c){return InstrumentPerformance.active(c,TrianglePlayRules.KIND);}
 static boolean happy(ChaoEntity c,float p){return active(c)&&TrianglePlayRules.happy(InstrumentPerformance.phase(c,p));}
 static float propWeight(float w){return Math.max(0,Math.min(1,(w-.6f)/.4f));}
 static Vector3f palm(BiConsumer<String,PoseStack> anchor,float[] p,boolean left){var s=new PoseStack();anchor.accept(left?"arm_left":"arm_right",s);return s.last().pose().transformPosition(new Vector3f(p).div(16));}
 static Matrix4f triangleMatrix(BiConsumer<String,PoseStack> anchor,float[] left,float w,float tap){
  var body=new PoseStack();anchor.accept("body",body);var at=new Matrix4f(body.last().pose()).invert().transformPosition(palm(anchor,left,true));
  return new Matrix4f(body.last().pose()).translate(at).rotateZ(.025f*tap).rotateX((float)Math.PI).scale(SCALE*propWeight(w)).translate(new Vector3f(.5f).sub(GRIP));
 }
 static Vector3f hit(Matrix4f triangle){return triangle.transformPosition(new Vector3f(HIT).sub(.5f,.5f,.5f));}
 static Vector3f strikeTarget(BiConsumer<String,PoseStack> anchor,Matrix4f triangle,float tap){var b=new PoseStack();anchor.accept("body",b);return hit(triangle).add(b.last().pose().transformDirection(new Vector3f(0,0,-.8f*(1-tap)/16)));}
 static Matrix4f beaterMatrix(BiConsumer<String,PoseStack> anchor,float[] right,Matrix4f triangle,float w,float tap){
  var hand=palm(anchor,right,false);var direction=strikeTarget(anchor,triangle,tap).sub(hand);
  if(direction.lengthSquared()<1e-10f)direction.set(1,0,0);
  return new Matrix4f().translate(hand).rotate(new Quaternionf().rotationTo(new Vector3f(1,0,0),direction.normalize())).scale(STICK_SCALE*propWeight(w)).translate(new Vector3f(.5f).sub(STICK_GRIP));
 }
 private static void fit(Map<String,ChaoMeshAsset.Pose> poses,BiConsumer<String,PoseStack> anchor,float[] palm,boolean left,float w,Vector3f target){
  String name=left?"arm_left":"arm_right";var arm=poses.get(name);var s=new PoseStack();anchor.accept(name,s);
  float distance=s.last().pose().transformPosition(new Vector3f()).distance(target)*16;
  float reach=Math.max(0,(float)Math.sqrt(Math.max(0,distance*distance-palm[1]*palm[1]-palm[2]*palm[2]))-Math.abs(palm[0])-.25f);
  arm.reach=reach*w;
  ChaoTambourineAnimation.alignPalm(poses,anchor,ChaoTrumpetAnimation.reachedPalm(palm,arm.reach,left),w,target,left,.35f);
 }
 static void pose(Map<String,ChaoMeshAsset.Pose> poses,BiConsumer<String,PoseStack> anchor,float[] right,float[] left,float w,float tap){
  ChaoInstrumentFocus.apply(poses,w);poses.get("skull").rx+=.015f*tap*w;
  for(boolean l:new boolean[]{false,true}){var arm=poses.get(l?"arm_left":"arm_right");arm.x*=1-w;arm.y*=1-w;arm.z=arm.z*(1-w)-.55f*w;}
  var body=new PoseStack();anchor.accept("body",body);var inv=new Matrix4f(body.last().pose()).invert();
  var shoulder=new PoseStack();anchor.accept("arm_left",shoulder);var target=inv.transformPosition(shoulder.last().pose().transformPosition(new Vector3f())).add(-.75f/16,(.35f+2.5f*(1-w))/16,(-4.65f-2f*(1-w))/16);
  float ease=w*w*(3-2*w);fit(poses,anchor,left,true,ease,body.last().pose().transformPosition(target));
  var leftResolved=ChaoTrumpetAnimation.reachedPalm(left,poses.get("arm_left").reach,true);
  var triangle=triangleMatrix(anchor,leftResolved,1,tap);
  var offset=new Vector3f(-2,-2,-.45f-1.2f*(1-tap)).normalize().mul(STICK_SCALE*(STICK_TIP.x-STICK_GRIP.x));
  var grip=strikeTarget(anchor,triangle,tap).add(body.last().pose().transformDirection(offset));
  fit(poses,anchor,right,false,ease,grip);
 }
 static void pose(ChaoEntity c,ChaoModel m,LionfishTriangleClip clip,float partial){
  if(!active(c)||m.baseToyPalm(false)==null||m.baseToyPalm(true)==null)return;
  float t=InstrumentPerformance.phase(c,partial),w=InstrumentPerformance.presence(c,partial);
  pose(m.officialPoses(),m::transformAccessoryAnchor,m.baseToyPalm(false),m.baseToyPalm(true),w,clip.tap(t));
 }
 static boolean render(ChaoEntity c,ChaoModel m,PoseStack stack,MultiBufferSource buffers,int light,float partial){
  if(!active(c)||c.isInvisible()||!m.officialActive()||m.toyPalm(false)==null||m.toyPalm(true)==null)return false;
  float w=InstrumentPerformance.presence(c,partial);if(propWeight(w)<=0)return true;
  var triangle=triangleMatrix(m::transformAccessoryAnchor,m.toyPalm(true),w,m.triangleTap(InstrumentPerformance.phase(c,partial)));
  var beater=beaterMatrix(m::transformAccessoryAnchor,m.toyPalm(false),triangle,w,m.triangleTap(InstrumentPerformance.phase(c,partial)));var mc=Minecraft.getInstance();var item=new ItemStack(ChaoGarden.item(TrianglePlayRules.KIND));
  for(boolean stick:new boolean[]{false,true}){stack.pushPose();try{stack.mulPose(stick?beater:triangle);mc.getItemRenderer().render(item,ItemDisplayContext.NONE,false,stack,buffers,light,OverlayTexture.NO_OVERLAY,mc.getModelManager().getModel(stick?BEATER:BODY));}finally{stack.popPose();}}
  return true;
 }
}
