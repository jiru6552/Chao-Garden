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
/** Mouth-aligned trumpet with two measured supports; the same transform drives both hands and prop. */
final class ChaoTrumpetAnimation {
 static final Vector3f TIP=new Vector3f(.91028982f,.41521558f,.29729682f);
 static final float SCALE=.34f,ANGLE=.15f,GRIP_Y=.185f,ALONG=.29f;
 static boolean active(ChaoEntity c){return InstrumentPerformance.active(c,TrumpetPlayRules.KIND);}
 static boolean happy(ChaoEntity c,float p){return active(c)&&TrumpetPlayRules.happy(InstrumentPerformance.phase(c,p));}
 static float propWeight(float w){return Math.max(0,Math.min(1,(w-.55f)/.45f));}
 static Matrix4f propMatrix(BiConsumer<String,PoseStack> anchor,float[] mouth,float w,float lean){
  var skull=new PoseStack();anchor.accept("skull",skull);
  return new Matrix4f(skull.last().pose()).translate(mouth[0]/16,(mouth[1]+2*(1-w))/16,(mouth[2]-.60f-1.3f*(1-w))/16)
    .rotateX(ANGLE+lean).rotateY((float)Math.toRadians(-75)).rotateX((float)Math.PI).scale(SCALE*propWeight(w)).translate(new Vector3f(.5f).sub(TIP));
 }
 static Vector3f grip(Matrix4f prop,BiConsumer<String,PoseStack> anchor,float[] palm,boolean left){
  var arm=new PoseStack();anchor.accept(left?"arm_left":"arm_right",arm);
  var shoulder=arm.last().pose().transformPosition(new Vector3f());
  float length=arm.last().pose().transformDirection(new Vector3f(palm).div(16)).length();
  // Right hand reaches the valve body; left supports its lower loop.
  // The authored trumpet's long axis is rotated fifteen degrees in X/Z.
  float side=(left?.065f:.09f)+(left?.02f:.05f)*Math.max(0,Math.min(1.4f,new Vector3f(palm).length()-1.8f));
  Vector3f best=null;float cost=Float.POSITIVE_INFINITY;
  for(int i=0;i<=80;i++){
   float along=ALONG+i*.12f/80, lateral=left?side:-side;
   var point=new Vector3f(TIP.x-.9659258f*along+.258819f*lateral,GRIP_Y,TIP.z+.258819f*along+.9659258f*lateral);
   var target=prop.transformPosition(point.sub(.5f,.5f,.5f));
   float error=Math.abs(target.distance(shoulder)-length)+.002f*Math.abs(along-(left?.29f:.21f));
   if(error<cost){cost=error;best=target;}
  }
  return best;
 }
 static float[] reachedPalm(float[] palm,float reach,boolean left){var result=palm.clone();result[0]+=left?reach:-reach;return result;}
 static void pose(Map<String,ChaoMeshAsset.Pose> poses,BiConsumer<String,PoseStack> anchor,float[] mouth,float[] right,float[] left,float w,float lean){
  ChaoInstrumentFocus.apply(poses,w);
  // Keep the skull at its normal joint; fit the instrument and hands to it.
  poses.get("skull").rx+=.28f*w;
  for(boolean l:new boolean[]{false,true}){var arm=poses.get(l?"arm_left":"arm_right");arm.x*=1-w;arm.y*=1-w;arm.z=arm.z*(1-w)-.9f*w;}
  // A shrinking reveal must not draw the hands into the mouth. Approach the
  // full-sized support points from below and ahead of the face instead.
  var prop=propMatrix(anchor,mouth,1,lean);float handWeight=w*w*(3-2*w);
  for(boolean l:new boolean[]{false,true}){
   float[] palm=l?left:right;String name=l?"arm_left":"arm_right";var arm=poses.get(name);
   var target=grip(prop,anchor,palm,l).add(0,2.5f*(1-w)/16,-2f*(1-w)/16);
   var shoulder=new PoseStack();anchor.accept(name,shoulder);
   float distance=shoulder.last().pose().transformPosition(new Vector3f()).distance(target)*16;
   float reach=Math.max(0,(float)Math.sqrt(Math.max(0,distance*distance-palm[1]*palm[1]-palm[2]*palm[2]))-Math.abs(palm[0])-.4f);
   arm.reach=reach*handWeight;
   ChaoTambourineAnimation.alignPalm(poses,anchor,reachedPalm(palm,arm.reach,l),handWeight,target,l,.65f);
  }
 }
 static void pose(ChaoEntity c,ChaoModel m,LionfishTrumpetClip clip,float partial){
  if(!active(c)||m.toyMouth()==null||m.toyPalm(false)==null||m.toyPalm(true)==null)return;
  float t=InstrumentPerformance.phase(c,partial),w=InstrumentPerformance.presence(c,partial);
  pose(m.officialPoses(),m::transformAccessoryAnchor,m.toyMouth(),m.baseToyPalm(false),m.baseToyPalm(true),w,clip.lean(t));
 }
 static boolean render(ChaoEntity c,ChaoModel m,PoseStack stack,MultiBufferSource buffers,int light,float partial){
  if(!active(c)||c.isInvisible()||!m.officialActive()||m.toyMouth()==null||m.toyPalm(false)==null||m.toyPalm(true)==null)return false;
  float w=InstrumentPerformance.presence(c,partial);if(propWeight(w)<=0)return true;
  stack.pushPose();try{stack.mulPose(propMatrix(m::transformAccessoryAnchor,m.toyMouth(),w,m.trumpetLean(InstrumentPerformance.phase(c,partial))));
   Minecraft.getInstance().getItemRenderer().renderStatic(c,new ItemStack(ChaoGarden.item(TrumpetPlayRules.KIND)),ItemDisplayContext.NONE,false,stack,buffers,c.level(),light,OverlayTexture.NO_OVERLAY,c.getId());
  }finally{stack.popPose();}return true;
 }
}
