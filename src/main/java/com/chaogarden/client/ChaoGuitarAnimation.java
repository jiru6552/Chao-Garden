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
/** One torso-relative guitar frame drives the real rendered mesh and both visible hands. */
final class ChaoGuitarAnimation {
 static final float SCALE=.60f;
 static final Vector3f SOURCE_STRUM=new Vector3f(.3291302f,.19492848f,.48033774f);
 static final Vector3f SOURCE_NECK=new Vector3f(.60f,.26908057f,.56820762f);
 static final Vector3f NECK_AXIS=new Vector3f(.92050569f,.25199345f,.29861109f);
 // Outward normal of the actual string-strip triangles, not the opposite
 // wood/soundhole decal. Rotating about the neck keeps the mesh unmirrored.
 static final Vector3f FRONT=new Vector3f(-.0257916f,-.7233867f,.6899612f);
 static final Vector3f ACROSS=new Vector3f(FRONT).cross(NECK_AXIS).normalize();
 private static Matrix4f basis(){
  var v=new Vector3f(ACROSS).negate();return new Matrix4f()
   .m00(NECK_AXIS.x).m10(NECK_AXIS.y).m20(NECK_AXIS.z)
   .m01(v.x).m11(v.y).m21(v.z)
   .m02(-FRONT.x).m12(-FRONT.y).m22(-FRONT.z);
 }
 static boolean active(ChaoEntity c){return InstrumentPerformance.active(c,GuitarPlayRules.KIND);}
 static boolean happy(ChaoEntity c,float p){return active(c)&&GuitarPlayRules.happy(InstrumentPerformance.phase(c,p));}
 static float propWeight(float w){return Math.clamp((w-.45f)/.55f,0,1);}
 static Matrix4f propMatrix(BiConsumer<String,PoseStack> anchor,float w){
  var body=new PoseStack();anchor.accept("body",body);
  return new Matrix4f(body.last().pose()).translate(-.02f,-.045f,-.215f-.06f*(1-w))
   .rotateZ(-.18f*w).scale(SCALE*propWeight(w)).mul(basis()).translate(new Vector3f(.5f).sub(SOURCE_STRUM));
 }
 static Vector3f neckGrip(Matrix4f prop,float fret){
  var p=new Vector3f(SOURCE_NECK).fma((fret-SOURCE_NECK.x)/NECK_AXIS.x,NECK_AXIS).fma(.035f,FRONT);
  return prop.transformPosition(p.sub(.5f,.5f,.5f));
 }
 static Vector3f strumTarget(Matrix4f prop,float strum){return prop.transformPosition(new Vector3f(SOURCE_STRUM).fma(.09f*strum,ACROSS).fma(.035f,FRONT).sub(.5f,.5f,.5f));}
 static void pose(Map<String,ChaoMeshAsset.Pose> poses,BiConsumer<String,PoseStack> anchor,float[] right,float[] left,float w,float strum,float fret){
  ChaoInstrumentFocus.apply(poses,w);poses.get("skull").rx+=.10f*w;
  var prop=propMatrix(anchor,w);float ease=w*w*(3-2*w);
  for(boolean l:new boolean[]{false,true}){
   var palm=l?left:right;var target=l?neckGrip(prop,fret):strumTarget(prop,strum);var arm=poses.get(l?"arm_left":"arm_right");
   var shoulder=new PoseStack();anchor.accept(l?"arm_left":"arm_right",shoulder);
   float length=shoulder.last().pose().transformPosition(new Vector3f()).distance(target)*16;
   arm.reach=Math.max(0,(float)Math.sqrt(Math.max(0,length*length-palm[1]*palm[1]-palm[2]*palm[2]))-Math.abs(palm[0])-.20f)*ease;
   ChaoTambourineAnimation.alignPalm(poses,anchor,ChaoTrumpetAnimation.reachedPalm(palm,arm.reach,l),ease,target,l,.30f);
  }
 }
 static void pose(ChaoEntity c,ChaoModel m,LionfishGuitarClip clip,float partial){
  if(!active(c)||m.baseToyPalm(false)==null||m.baseToyPalm(true)==null)return;
  float t=InstrumentPerformance.phase(c,partial),w=InstrumentPerformance.presence(c,partial);
  pose(m.officialPoses(),m::transformAccessoryAnchor,m.baseToyPalm(false),m.baseToyPalm(true),w,clip.strum(t),clip.fret(t));
 }
 static boolean render(ChaoEntity c,ChaoModel m,PoseStack stack,MultiBufferSource buffers,int light,float partial){
  if(!active(c)||c.isInvisible()||!m.officialActive()||m.baseToyPalm(false)==null||m.baseToyPalm(true)==null)return false;
  float w=InstrumentPerformance.presence(c,partial);if(propWeight(w)<=0)return true;
  stack.pushPose();try{stack.mulPose(propMatrix(m::transformAccessoryAnchor,w));
   Minecraft.getInstance().getItemRenderer().renderStatic(c,new ItemStack(ChaoGarden.item(GuitarPlayRules.KIND)),ItemDisplayContext.NONE,false,stack,buffers,c.level(),light,OverlayTexture.NO_OVERLAY,c.getId());
  }finally{stack.popPose();}return true;
 }
}
