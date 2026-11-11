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
final class ChaoDrumAnimation {
  static final Vector3f GRIP=new Vector3f(.28f,.43f,.13f),HIT=new Vector3f(.75f,.470f,.25f);
  static final float SCALE=.34f;
  static boolean active(ChaoEntity c){return InstrumentPerformance.active(c,DrumPlayRules.KIND);}
  static boolean happy(ChaoEntity c,float p){return active(c)&&DrumPlayRules.happy(InstrumentPerformance.phase(c,p));}
  static void applyArm(ChaoMeshAsset.Pose arm,LionfishDrumClip clip,float t,float w,boolean left){
    var v=clip.sample(t,left);arm.rx=arm.rx*(1-w)+v.x*w;arm.ry=arm.ry*(1-w)+v.y*w;arm.rz=arm.rz*(1-w)+v.z*w;arm.x*=1-w;arm.y*=1-w;arm.z=arm.z*(1-w)-.70f*w;
  }
  static float propWeight(float weight){return Math.max(0,Math.min(1,(weight-.65f)/.35f));}
  static Matrix4f propMatrix(BiConsumer<String,PoseStack> anchor,float[] palm,float weight){
    var right=new PoseStack();anchor.accept("arm_right",right);var hand=right.last().pose().transformPosition(new Vector3f(palm).div(16));
    var body=new PoseStack();anchor.accept("body",body);var local=new Matrix4f(body.last().pose()).invert().transformPosition(hand);
    return new Matrix4f(body.last().pose()).translate(local).rotateX((float)Math.PI).scale(SCALE*propWeight(weight)).translate(new Vector3f(.5f).sub(GRIP));
  }
  static Vector3f hitTarget(Matrix4f prop,float tap){return prop.transformPosition(new Vector3f(HIT).sub(.5f,.5f,.5f).add(0,.065f+(1-tap)*.14f,0));}
  static float alignTap(Map<String,ChaoMeshAsset.Pose> poses,BiConsumer<String,PoseStack> anchor,float[] rightPalm,float[] leftPalm,float weight,float tap){
    if(weight<=0)return 0;
    var body=new PoseStack();anchor.accept("body",body);var armStack=new PoseStack();anchor.accept("arm_right",armStack);
    var shoulder=new Matrix4f(body.last().pose()).invert().transformPosition(armStack.last().pose().transformPosition(new Vector3f()));
    float length=new Vector3f(rightPalm).length();float across=Math.min(.95f,length*.4f),drop=Math.min(1.5f,length*.62f);
    var target=new Vector3f(shoulder).add(across/16,drop/16,-(float)Math.sqrt(Math.max(.1f,length*length-across*across-drop*drop))/16);
    ChaoTambourineAnimation.alignPalm(poses,anchor,rightPalm,weight,body.last().pose().transformPosition(target),false,0);
    return ChaoTambourineAnimation.alignPalm(poses,anchor,leftPalm,weight,hitTarget(propMatrix(anchor,rightPalm,weight),tap),true,.4f);
  }
  static void pose(ChaoEntity c,ChaoModel m,LionfishDrumClip clip,float partial){
    if(!active(c)||m.toyPalm(false)==null||m.toyPalm(true)==null)return;
    float t=InstrumentPerformance.phase(c,partial),w=InstrumentPerformance.presence(c,partial);
    ChaoInstrumentFocus.apply(m.officialPoses(),w);
    for(boolean left:new boolean[]{false,true})applyArm(m.officialPoses().get(left?"arm_left":"arm_right"),clip,t,w,left);
    alignTap(m.officialPoses(),m::transformAccessoryAnchor,m.toyPalm(false),m.toyPalm(true),w,clip.tap(t));
  }
  static boolean render(ChaoEntity c,ChaoModel m,PoseStack stack,MultiBufferSource buffers,int light,float partial){
    if(!active(c)||c.isInvisible()||!m.officialActive()||m.toyPalm(false)==null||m.toyPalm(true)==null)return false;
    float w=InstrumentPerformance.presence(c,partial);if(w<=0)return true;
    var prop=propMatrix(m::transformAccessoryAnchor,m.toyPalm(false),w);float tap=m.drumTap(InstrumentPerformance.phase(c,partial));
    stack.pushPose();try{stack.mulPose(prop);
      Minecraft.getInstance().getItemRenderer().renderStatic(c,new ItemStack(ChaoGarden.item(DrumPlayRules.KIND)),ItemDisplayContext.NONE,false,stack,buffers,c.level(),light,OverlayTexture.NO_OVERLAY,c.getId());
    }finally{stack.popPose();}return true;
  }
}
