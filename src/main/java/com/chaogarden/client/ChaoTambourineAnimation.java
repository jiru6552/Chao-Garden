package com.chaogarden.client;
import com.chaogarden.ChaoGarden;
import com.chaogarden.entity.ChaoEntity;
import com.chaogarden.toys.*;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.item.*;
import org.joml.Vector3f;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import java.util.Map;
import java.util.function.BiConsumer;
/** Source drumhead/rim landmarks; object stays on the actual right palm. */
final class ChaoTambourineAnimation {
  static final Vector3f CENTER=new Vector3f(.42413276f,.30883664f,.51089611f);
  static final Vector3f NORMAL=new Vector3f(.7369601f,.66935792f,-.09407325f).normalize();
  static final Vector3f TANGENT=new Vector3f(NORMAL.y,-NORMAL.x,0).normalize();
  static final Vector3f GRIP=new Vector3f(CENTER).fma(-.342f,TANGENT);
  static final Vector3f HIT=new Vector3f(CENTER).fma(.30f,TANGENT);
  static final float SCALE=.42f;
  static final Quaternionf ORIGINAL_ORIENTATION=orientation();
  static final Quaternionf ORIENTATION=new Quaternionf().rotationX((float)Math.PI/2).mul(ORIGINAL_ORIENTATION);
  private static Quaternionf orientation(){
    var source=new Matrix3f().setColumn(0,TANGENT).setColumn(1,new Vector3f(NORMAL).cross(TANGENT)).setColumn(2,NORMAL);
    var normal=new Vector3f(0,-.15f,-1).normalize();var tangent=new Vector3f(1,0,0);
    return new Quaternionf().setFromNormalized(new Matrix3f().setColumn(0,tangent).setColumn(1,new Vector3f(normal).cross(tangent)).setColumn(2,normal).mul(source.transpose()));
  }
  static boolean active(ChaoEntity c){return InstrumentPerformance.active(c,TambourinePlayRules.KIND);}
  static boolean happy(ChaoEntity c,float p){return active(c)&&TambourinePlayRules.happy(InstrumentPerformance.phase(c,p));}
  static void applyArm(ChaoMeshAsset.Pose arm,LionfishTambourineClip clip,float t,float w,boolean left){
    var v=clip.sample(t,left);arm.rx=arm.rx*(1-w)+v.x*w;arm.ry=arm.ry*(1-w)+v.y*w;arm.rz=arm.rz*(1-w)+v.z*w;
    arm.x+=(left?.3f:.55f)*w;arm.z-=.65f*w;
  }
  static Matrix4f propMatrix(BiConsumer<String,PoseStack> anchor,float[] palm,float weight){
    var right=new PoseStack();anchor.accept("arm_right",right);var hand=right.last().pose().transformPosition(new Vector3f(palm).div(16));
    var body=new PoseStack();anchor.accept("body",body);var local=new Matrix4f(body.last().pose()).invert().transformPosition(hand);
    return new Matrix4f(body.last().pose()).translate(local).rotate(ORIENTATION).scale(SCALE*weight).translate(new Vector3f(.5f).sub(GRIP));
  }
  static Vector3f hitTarget(Matrix4f prop,float tap){
    // Palm centre remains outside the skin; only the rounded hand's face taps it.
    return prop.transformPosition(new Vector3f(HIT).sub(.5f,.5f,.5f).fma(-.055f-(1-tap)*.20f,NORMAL));
  }
  static float alignTap(Map<String,ChaoMeshAsset.Pose> poses,BiConsumer<String,PoseStack> anchor,float[] rightPalm,float[] leftPalm,float weight,float tap){
    return alignPalm(poses,anchor,leftPalm,weight,hitTarget(propMatrix(anchor,rightPalm,weight),tap));
  }
  static float alignPalm(Map<String,ChaoMeshAsset.Pose> poses,BiConsumer<String,PoseStack> anchor,float[] leftPalm,float weight,Vector3f worldTarget){
    return alignPalm(poses,anchor,leftPalm,weight,worldTarget,true);
  }
  static float alignPalm(Map<String,ChaoMeshAsset.Pose> poses,BiConsumer<String,PoseStack> anchor,float[] palm,float weight,Vector3f worldTarget,boolean left){
    return alignPalm(poses,anchor,palm,weight,worldTarget,left,1.25f);
  }
  static float alignPalm(Map<String,ChaoMeshAsset.Pose> poses,BiConsumer<String,PoseStack> anchor,float[] palm,float weight,Vector3f worldTarget,boolean left,float maxShift){
    if(weight<=0)return 0;
    var body=new PoseStack();anchor.accept("body",body);var inverse=new Matrix4f(body.last().pose()).invert();
    var armStack=new PoseStack();anchor.accept(left?"arm_left":"arm_right",armStack);var arm=poses.get(left?"arm_left":"arm_right");
    var shoulder=inverse.transformPosition(armStack.last().pose().transformPosition(new Vector3f()));
    var target=inverse.transformPosition(new Vector3f(worldTarget));
    var vector=new Vector3f(palm).div(16).mul(arm.sx,arm.sy,arm.sz);float length=vector.length();
    var desired=new Vector3f(target).sub(shoulder);if(length<1e-6||desired.lengthSquared()<1e-8)return 0;
    var current=new Quaternionf().rotationZYX(arm.rz,arm.ry,arm.rx);
    var goal=new Quaternionf().rotationTo(current.transform(new Vector3f(vector)),desired).mul(current);
    current.slerp(goal,weight).normalize();
    // Explicit Rz * Ry * Rx decomposition, matching ChaoMeshAsset.applyTransform.
    float qx=current.x,qy=current.y,qz=current.z,qw=current.w;
    arm.rx=(float)Math.atan2(2*(qw*qx+qy*qz),1-2*(qx*qx+qy*qy));
    arm.ry=(float)Math.asin(Math.max(-1,Math.min(1,2*(qw*qy-qz*qx))));
    arm.rz=(float)Math.atan2(2*(qw*qz+qx*qy),1-2*(qy*qy+qz*qz));
    // Small shoulder compensation handles differing animal-hand proportions.
    var shift=new Vector3f(desired).normalize().mul(desired.length()-length);
    float pixels=shift.length()*16;if(pixels>maxShift)shift.mul(maxShift/pixels);
    arm.x+=shift.x*16*weight;arm.y+=shift.y*16*weight;arm.z+=shift.z*16*weight;

    return pixels;
  }
  static void pose(ChaoEntity c,ChaoModel m,LionfishTambourineClip clip,float partial){
    if(!active(c)||m.toyPalm(false)==null||m.toyPalm(true)==null)return;
    float t=InstrumentPerformance.phase(c,partial),w=InstrumentPerformance.presence(c,partial);
    for(boolean left:new boolean[]{false,true})applyArm(m.officialPoses().get(left?"arm_left":"arm_right"),clip,t,w,left);
    alignTap(m.officialPoses(),m::transformAccessoryAnchor,m.toyPalm(false),m.toyPalm(true),w,clip.tap(t));
  }
  static boolean render(ChaoEntity c,ChaoModel m,PoseStack stack,MultiBufferSource buffers,int light,float partial){
    if(!active(c)||c.isInvisible()||!m.officialActive()||m.toyPalm(false)==null||m.toyPalm(true)==null)return false;
    float w=InstrumentPerformance.presence(c,partial);if(w<=0)return true;
    stack.pushPose();try{stack.mulPose(propMatrix(m::transformAccessoryAnchor,m.toyPalm(false),w));
      Minecraft.getInstance().getItemRenderer().renderStatic(c,new ItemStack(ChaoGarden.item(TambourinePlayRules.KIND)),ItemDisplayContext.NONE,false,stack,buffers,c.level(),light,OverlayTexture.NO_OVERLAY,c.getId());
    }finally{stack.popPose();}return true;
  }
}
