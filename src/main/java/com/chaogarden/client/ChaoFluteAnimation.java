package com.chaogarden.client;
import com.chaogarden.ChaoGarden;
import com.chaogarden.entity.ChaoEntity;
import com.chaogarden.toys.*;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.item.*;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import java.util.Map;
import java.util.function.BiConsumer;
/** Mouth-aligned recorder with two measured supports; the same transform drives both hands and prop. */
final class ChaoFluteAnimation {
 static final Vector3f TIP=new Vector3f(.5f,.9375f,.526f);
 static final float SCALE=.30f;
 static boolean active(ChaoEntity c){return InstrumentPerformance.active(c,FlutePlayRules.KIND);}
 static boolean happy(ChaoEntity c,float p){return active(c)&&FlutePlayRules.happy(InstrumentPerformance.phase(c,p));}
 static float propWeight(float w){return Math.max(0,Math.min(1,(w-.55f)/.45f));}
 static Matrix4f propMatrix(BiConsumer<String,PoseStack> anchor,float[] mouth,float w,float lean){
  var skull=new PoseStack();anchor.accept("skull",skull);
  return new Matrix4f(skull.last().pose()).translate(mouth[0]/16,(mouth[1]+2*(1-w))/16,(mouth[2]-.60f-1.3f*(1-w))/16)
    .rotateX((float)Math.PI-(float)Math.toRadians(7)+lean).scale(SCALE*propWeight(w)).translate(new Vector3f(.5f).sub(TIP));
 }
 static Vector3f grip(Matrix4f prop,BiConsumer<String,PoseStack> anchor,float[] palm,boolean left){
  var arm=new PoseStack();anchor.accept(left?"arm_left":"arm_right",arm);
  var shoulder=arm.last().pose().transformPosition(new Vector3f());
  float length=arm.last().pose().transformDirection(new Vector3f(palm).div(16)).length();
  // Keep larger palms outside the tube, with separate upper and lower supports.
  // Short Chaos arms use a narrower grip on the same authored instrument.
  float side=.10f+.12f*Math.max(0,Math.min(1.4f,new Vector3f(palm).length()-1.8f));
  Vector3f best=null;float cost=Float.POSITIVE_INFINITY;
  for(int i=0;i<=80;i++){
   float y=(left?.64f:.24f)+i*(left?.16f:.41f)/80;
   var target=prop.transformPosition(new Vector3f(.5f+(left?side:-side),y,.51f).sub(.5f,.5f,.5f));
   float error=Math.abs(target.distance(shoulder)-length)+.002f*Math.abs(y-(left?.60f:.36f));
   if(error<cost){cost=error;best=target;}
  }
  return best;
 }
 static void pose(Map<String,ChaoMeshAsset.Pose> poses,BiConsumer<String,PoseStack> anchor,float[] mouth,float[] right,float[] left,float w,float lean){
  ChaoInstrumentFocus.apply(poses,w);
  var head=poses.get("skull");head.rx+=.35f*w;head.z+=(.6f+Math.max(0,2.4f-Math.min(new Vector3f(right).length(),new Vector3f(left).length())))*w;
  for(boolean l:new boolean[]{false,true}){var arm=poses.get(l?"arm_left":"arm_right");arm.x*=1-w;arm.y*=1-w;arm.z=arm.z*(1-w)-.9f*w;}
  var prop=propMatrix(anchor,mouth,w,lean);
  for(boolean l:new boolean[]{false,true})ChaoTambourineAnimation.alignPalm(poses,anchor,l?left:right,w,grip(prop,anchor,l?left:right,l),l,.65f);
 }
 static void pose(ChaoEntity c,ChaoModel m,LionfishFluteClip clip,float partial){
  if(!active(c)||m.toyMouth()==null||m.toyPalm(false)==null||m.toyPalm(true)==null)return;
  float t=InstrumentPerformance.phase(c,partial),w=InstrumentPerformance.presence(c,partial);
  pose(m.officialPoses(),m::transformAccessoryAnchor,m.toyMouth(),m.toyPalm(false),m.toyPalm(true),w,clip.lean(t));
 }
 static boolean render(ChaoEntity c,ChaoModel m,PoseStack stack,MultiBufferSource buffers,int light,float partial){
  if(!active(c)||c.isInvisible()||!m.officialActive()||m.toyMouth()==null||m.toyPalm(false)==null||m.toyPalm(true)==null)return false;
  float w=InstrumentPerformance.presence(c,partial);if(propWeight(w)<=0)return true;
  stack.pushPose();try{stack.mulPose(propMatrix(m::transformAccessoryAnchor,m.toyMouth(),w,m.fluteLean(InstrumentPerformance.phase(c,partial))));
   Minecraft.getInstance().getItemRenderer().renderStatic(c,new ItemStack(ChaoGarden.item(FlutePlayRules.KIND)),ItemDisplayContext.NONE,false,stack,buffers,c.level(),light,OverlayTexture.NO_OVERLAY,c.getId());
  }finally{stack.popPose();}return true;
 }
}
