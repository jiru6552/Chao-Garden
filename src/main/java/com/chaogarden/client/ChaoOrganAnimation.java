package com.chaogarden.client;
import com.chaogarden.entity.ChaoEntity;
import com.chaogarden.toys.*;
import com.mojang.blaze3d.vertex.PoseStack;
import org.joml.Vector3f;
/** Hands fit the placed keyboard in world space, including the actual visible replacement palms. */
final class ChaoOrganAnimation {
 static boolean active(ChaoEntity c){return c.toyPlayingKind().equals(OrganPlayRules.KIND)&&c.toyTarget()!=null
   &&c.hurtTime<=0&&!c.isCocooned()&&!c.isCompanionGripping()&&!c.officialComparison()
   &&!c.isEating()&&!c.isHandlingItem()&&!c.isBeingPetted()&&!c.isResting()&&c.getBehavior()==ChaoEntity.Behavior.NONE;}
 static float phase(ChaoEntity c,float p){return 160-c.toyPlayingTicks()+p;}
 static boolean happy(ChaoEntity c,float p){float t=phase(c,p);return active(c)&&t>=18&&t<148;}
 static void pose(ChaoEntity c,ChaoModel m,LionfishOrganClip clip,float partial){
  if(!active(c)||m.baseToyPalm(false)==null||m.baseToyPalm(true)==null)return;
  float t=phase(c,partial),w=OrganPlayRules.presence(t);
  float scale=c.state().immortal?.86f:c.state().adult?.80f:.64f;
  var at=c.toyTarget();var state=c.level().getBlockState(at);
  if(!state.is(ToyContent.block(OrganPlayRules.KIND)))return;
  var front=state.getValue(ChaoToyBlock.FACING);
  float distance=(float)((c.getX()-at.getX()-.5)*front.getStepX()+(c.getZ()-at.getZ()-.5)*front.getStepZ());
  fit(m.officialPoses(),m::transformAccessoryAnchor,m.baseToyPalm(false),m.baseToyPalm(true),clip,t,w,scale,distance,at.getY()-(float)c.getY());
 }
 static void fit(java.util.Map<String,ChaoMeshAsset.Pose> poses,java.util.function.BiConsumer<String,PoseStack> anchor,float[] right,float[] leftPalm,LionfishOrganClip clip,float t,float w,float scale,float distance,float dy){
  float ease=w*w*(3-2*w);
  ChaoInstrumentFocus.apply(poses,w);poses.get("skull").rx+=.07f*w;
  poses.get("skull").rz+=.018f*w*(float)Math.sin(t*Math.PI/32);
  for(boolean left:new boolean[]{false,true}){
   float tap=clip.tap(t,left);var palm=left?leftPalm:right;var arm=poses.get(left?"arm_left":"arm_right");
   // The organ manual is behind its narrow front cabinet, not at the piano contact plane.
   float keyX=OrganPlayRules.keyX(t,left);
   float keyY=OrganPlayRules.KEY_Y+.025f+.045f*(1-tap);
   var target=new Vector3f(keyX/scale,1.501f-(dy+keyY)/scale,-(distance-OrganPlayRules.KEY_FRONT)/scale);
   var shoulder=new PoseStack();anchor.accept(left?"arm_left":"arm_right",shoulder);
   float length=shoulder.last().pose().transformPosition(new Vector3f()).distance(target)*16;
   arm.reach=Math.max(0,(float)Math.sqrt(Math.max(0,length*length-palm[1]*palm[1]-palm[2]*palm[2]))-Math.abs(palm[0])-.20f)*ease;
   ChaoTambourineAnimation.alignPalm(poses,anchor,ChaoTrumpetAnimation.reachedPalm(palm,arm.reach,left),ease,target,left,.30f);
  }
 }
}
