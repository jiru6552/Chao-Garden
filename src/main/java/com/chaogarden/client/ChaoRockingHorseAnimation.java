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
import net.minecraft.util.Mth;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ModelEvent;
import org.joml.Matrix4f;
import org.joml.Vector3f;

/** Seat, hands and straddling feet fit the actual SA2 horse; the world source stays reserved. */
@EventBusSubscriber(modid=ChaoGarden.MODID,value=Dist.CLIENT,bus=EventBusSubscriber.Bus.MOD)
final class ChaoRockingHorseAnimation {
  private static final ModelResourceLocation HORSE=ModelResourceLocation.standalone(ResourceLocation.fromNamespaceAndPath(ChaoGarden.MODID,"item/chao_rocking_horse_motion"));
  static final float MAX_REACH=3.2f;
  @SubscribeEvent public static void models(ModelEvent.RegisterAdditional event){event.register(HORSE);}
  static boolean active(ChaoEntity c){return c.toyPlayingKind().equals(RockingHorsePlayRules.KIND)&&c.toyTarget()!=null
      &&c.hurtTime<=0&&!c.isCocooned()&&!c.isCompanionGripping()&&!c.officialComparison()
      &&c.getHeldChaoItem().isEmpty()&&c.getMainHandItem().isEmpty()
      &&!c.isEating()&&!c.isHandlingItem()&&!c.isBeingPetted()&&!c.isResting()&&c.getBehavior()==ChaoEntity.Behavior.NONE;}
  static float phase(ChaoEntity c,float partial){return 160-c.toyPlayingTicks()+partial;}
  static boolean happy(ChaoEntity c,float partial){return active(c)&&RockingHorsePlayRules.happy(phase(c,partial));}
  static Vector3f modelPoint(float x,float y,float z,float tick,float scale,float actorX,float actorY,float actorZ){
    return new Vector3f((x-actorX)/scale,1.501f-(RockingHorsePlayRules.pointY(y,z,tick)-actorY)/scale,
        -(RockingHorsePlayRules.pointZ(y,z,tick)-actorZ)/scale);
  }
  static float seatZ(float[] belly,float scale){
    // Keep the front of a broad adult belly behind the rising wooden neck.
    return RockingHorsePlayRules.SEAT_Z-Math.max(0,(belly[5]-belly[2])*scale/32-.095f);
  }
  static void fit(Map<String,ChaoMeshAsset.Pose> poses,BiConsumer<String,PoseStack> anchor,
                  java.util.function.Function<String,float[]> bounds,float[] right,float[] left,
                  float tick,float weight,float angle,float scale,float actorX,float actorY,float actorZ){
    var root=poses.get("root");var body=poses.get("body");var head=poses.get("skull");
    if(root==null||body==null)return;
    root.rx=root.rx*(1-weight)+angle;body.rx*=1-weight;
    if(head!=null){head.rx=head.rx*(1-weight)+.025f*weight;head.ry*=1-weight;head.z+=.5f*weight;}
    var belly=bounds.apply("body");
    if(belly!=null){
      var torso=new PoseStack();anchor.accept("body",torso);
      var hip=torso.last().pose().transformPosition(new Vector3f((belly[0]+belly[3])/32,belly[4]/16,(belly[2]+belly[5])/32));
      var target=modelPoint(0,RockingHorsePlayRules.SEAT_Y+.006f,seatZ(belly,scale),tick,scale,actorX,actorY,actorZ);
      // A tilted belly is supported by its lowest edge, not its centre. Lift
      // along the horse's own up vector so no corner clips through the saddle.
      var up=new Vector3f(0,-(float)Math.cos(angle),-(float)Math.sin(angle));float drop=0;
      for(int corner=0;corner<8;corner++){
        var point=torso.last().pose().transformPosition(new Vector3f(belly[(corner&1)==0?0:3],belly[(corner&2)==0?1:4],belly[(corner&4)==0?2:5]).div(16));
        drop=Math.max(drop,new Vector3f(hip).sub(point).dot(up));
      }
      var shift=target.fma(drop,up).sub(hip).mul(16*weight);root.x+=shift.x;root.y+=shift.y;root.z+=shift.z;
    }
    for(String name:ChaoSittingPose.FEET){
      var foot=poses.get(name);if(foot==null)continue;
      boolean side=name.contains("left");foot.rx=foot.rx*(1-weight)-.55f*weight;
      foot.rz=(side?.28f:-.28f)*weight;
      // Keep ordinary ankles connected; only flippers need a wider stance.
      // Long running toes move back slightly instead of passing through the bar.
      var footBounds=bounds.apply(name);float spread=2.2f,back=0;
      if(footBounds!=null){spread=1.2f+Math.min(1f,Math.max(0,side?-footBounds[0]:footBounds[3]));back=Math.max(0,-footBounds[2]-4)*.5f;}
      foot.x+=(side?spread:-spread)*weight;foot.z+=(back-.25f)*weight;
    }
    for(boolean side:new boolean[]{false,true}){
      var palm=side?left:right;if(palm==null)continue;
      var target=modelPoint(side?RockingHorsePlayRules.GRIP_X:-RockingHorsePlayRules.GRIP_X,
          RockingHorsePlayRules.GRIP_Y+.005f,RockingHorsePlayRules.GRIP_Z-.025f,tick,scale,actorX,actorY,actorZ);
      var arm=poses.get(side?"arm_left":"arm_right");if(arm==null)continue;
      arm.z-=.4f*weight;
      var shoulder=new PoseStack();anchor.accept(side?"arm_left":"arm_right",shoulder);
      float length=shoulder.last().pose().transformPosition(new Vector3f()).distance(target)*16;
      arm.reach=Math.clamp((float)Math.sqrt(Math.max(0,length*length-palm[1]*palm[1]-palm[2]*palm[2]))-Math.abs(palm[0])-.20f,0,MAX_REACH)*weight;
      ChaoTambourineAnimation.alignPalm(poses,anchor,ChaoTrumpetAnimation.reachedPalm(palm,arm.reach,side),weight,target,side,.90f);
    }
  }
  static void pose(ChaoEntity c,ChaoModel model,LionfishRockingHorseClip clip,float partial){
    if(!active(c))return;
    float tick=phase(c,partial),scale=c.state().immortal?.86f:c.state().adult?.80f:.64f;
    var at=c.toyTarget();var state=c.level().getBlockState(at);
    if(!state.is(ToyContent.block(RockingHorsePlayRules.KIND)))return;
    var forward=state.getValue(ChaoToyBlock.FACING).getOpposite();
    var position=c.getPosition(partial);
    float dx=(float)(position.x-at.getX()-.5),dz=(float)(position.z-at.getZ()-.5);
    float actorX=dx*forward.getStepZ()-dz*forward.getStepX(),actorZ=dx*forward.getStepX()+dz*forward.getStepZ();
    fit(model.officialPoses(),model::transformAccessoryAnchor,model::accessoryBounds,model.baseToyPalm(false),model.baseToyPalm(true),
        tick,RockingHorsePlayRules.occupancy(tick),clip.angle(tick),scale,actorX,(float)position.y-at.getY(),actorZ);
    applyYaw(model.officialPoses(),yawCorrection(c,partial)*RockingHorsePlayRules.occupancy(tick));
  }
  static float yawCorrection(ChaoEntity c,float partial){
    var at=c.toyTarget();if(at==null)return 0;var state=c.level().getBlockState(at);
    if(!state.is(ToyContent.block(RockingHorsePlayRules.KIND)))return 0;
    float expected=state.getValue(ChaoToyBlock.FACING).getOpposite().toYRot();
    return Mth.wrapDegrees(expected-Mth.rotLerp(partial,c.yBodyRotO,c.yBodyRot))*Mth.DEG_TO_RAD;
  }
  static void applyYaw(Map<String,ChaoMeshAsset.Pose> poses,float angle){
    var root=poses.get("root");if(root==null)return;
    var translation=new Matrix4f().rotateY(angle).transformDirection(new Vector3f(root.x,0,root.z));
    root.x=translation.x;root.z=translation.z;root.ry+=angle;
  }
  static Matrix4f horseMatrix(float scale,float actorX,float actorY,float actorZ,float angle){
    return new Matrix4f().translate(-actorX/scale,1.501f-(.5f-actorY)/scale,actorZ/scale)
        .rotateX((float)Math.PI).scale(1/scale).translate(0,-.495f,0).rotateX(angle).translate(0,.495f,0);
  }
  static boolean render(ChaoEntity c,ChaoModel model,PoseStack stack,MultiBufferSource buffers,int light,float partial){
    if(!active(c)||!model.officialActive())return false;
    var at=c.toyTarget();var state=c.level().getBlockState(at);if(!state.is(ToyContent.block(RockingHorsePlayRules.KIND)))return false;
    var forward=state.getValue(ChaoToyBlock.FACING).getOpposite();
    var position=c.getPosition(partial);
    float dx=(float)(position.x-at.getX()-.5),dz=(float)(position.z-at.getZ()-.5);
    float actorX=dx*forward.getStepZ()-dz*forward.getStepX(),actorZ=dx*forward.getStepX()+dz*forward.getStepZ();
    float scale=c.state().immortal?.86f:c.state().adult?.80f:.64f;
    var minecraft=Minecraft.getInstance();stack.pushPose();try{
      stack.mulPose(new Matrix4f().rotateY(yawCorrection(c,partial)).mul(horseMatrix(scale,actorX,(float)position.y-at.getY(),actorZ,RockingHorsePlayRules.angle(phase(c,partial)))));
      minecraft.getItemRenderer().render(new ItemStack(ChaoGarden.item(RockingHorsePlayRules.KIND)),ItemDisplayContext.NONE,false,
          stack,buffers,light,OverlayTexture.NO_OVERLAY,minecraft.getModelManager().getModel(HORSE));
    }finally{stack.popPose();}
    return true;
  }
}
