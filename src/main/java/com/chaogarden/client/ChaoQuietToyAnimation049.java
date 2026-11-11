package com.chaogarden.client;

import com.chaogarden.ChaoGarden;
import com.chaogarden.entity.ChaoEntity;
import com.chaogarden.toys.*;
import com.mojang.blaze3d.vertex.PoseStack;
import java.util.*;
import java.util.function.BiConsumer;
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
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/** Supplied SA2B quiet toys fitted to real visible palms; paper marks are transient. */
@EventBusSubscriber(modid=ChaoGarden.MODID,value=Dist.CLIENT,bus=EventBusSubscriber.Bus.MOD)
final class ChaoQuietToyAnimation049 {
  static final float BROOM_SCALE=.23f,BOOK_SCALE=.15f,PAPER_SCALE=.19f,CRAYON_SCALE=.18f,MAX_REACH=3.2f;
  static final Vector3f CRAYON_GRIP=new Vector3f(.00964f,-.35f,0),CRAYON_TIP=new Vector3f(.00964f,-.843995f,0);
  private static final Map<String,ModelResourceLocation> MODELS=new LinkedHashMap<>();
  private static final LionfishQuietToyClip049 RENDER_CLIP=new LionfishQuietToyClip049();
  private static final float[][] STROKES={{-.495f,-.1375f,.495f,-.1375f},{-.495f,.121f,.495f,.121f},{-.315f,.055f,-.315f,-.055f},{-.315f,-.055f,.18f,-.055f},{.18f,-.055f,.18f,.0715f},{.18f,.0715f,-.315f,.0715f},{-.045f,-.055f,.162f,-.187f},{.162f,-.187f,.288f,-.099f}};
  static{
    for(String name:List.of("broom","book_left","book_right","book_page","crayon_box","crayon"))MODELS.put(name,model(name));
    for(int i=0;i<=8;i++)MODELS.put("paper_"+i,model("paper_"+i));
  }
  private static ModelResourceLocation model(String name){return ModelResourceLocation.standalone(ResourceLocation.fromNamespaceAndPath(ChaoGarden.MODID,"item/chao_quiet_hand_"+name));}
  @SubscribeEvent public static void models(ModelEvent.RegisterAdditional event){MODELS.values().forEach(event::register);}
  static boolean active(ChaoEntity c){return QuietToyPlayRules049.supports(c.toyPlayingKind())&&GardenToyPerformance049.active(c,c.toyPlayingKind());}
  static boolean happy(ChaoEntity c,float partial){return active(c)&&QuietToyPlayRules049.happy(c.toyPlayingKind(),GardenToyPerformance049.phase(c,partial));}
  static float propWeight(float weight){return Math.clamp((weight-.45f)/.55f,0,1);}
  static Matrix4f body(BiConsumer<String,PoseStack> anchor){var stack=new PoseStack();anchor.accept("body",stack);return new Matrix4f(stack.last().pose());}
  static Matrix4f bookMatrix(BiConsumer<String,PoseStack> anchor,float weight){return body(anchor).translate(0,.06f+.04f*(1-weight),-.33f).rotateX(-.22f).scale(BOOK_SCALE*propWeight(weight));}
  static Matrix4f bookCover(Matrix4f book,float weight,boolean left){return new Matrix4f(book).rotateZ((left?1:-1)*(1-weight)*(float)Math.PI*.5f);}
  static Matrix4f paperMatrix(BiConsumer<String,PoseStack> anchor,float weight){return body(anchor).translate(-.105f,.02f+.04f*(1-weight),-.31f).rotateX(.12f).scale(PAPER_SCALE*propWeight(weight));}
  static Matrix4f broomMatrix(float weight,float sweep,float scale){
    float angle=.10f*sweep,support=1.8833337f*(float)Math.cos(angle)+.5665011f*Math.abs((float)Math.sin(angle));
    return new Matrix4f().translate(.035f*sweep/scale,1.501f-.008f/scale,-(.235f+Math.max(0,scale-.80f)*.75f)/scale).rotateY(.15f*sweep).rotateZ(angle).scale(BROOM_SCALE*propWeight(weight)).translate(0,-support,0);
  }
  static Matrix4f boxMatrix(float weight,float scale){return new Matrix4f().translate(.35f/scale,1.501f-.10f*.18f*propWeight(weight),-.08f/scale).scale(.18f*propWeight(weight));}
  /** The right palm grips the lower real wooden shaft; the left arm balances freely. */
  static Vector3f broomTarget(Matrix4f broom,boolean left){return broom.transformPosition(new Vector3f(left?.07f:-.07f,.60f,0));}
  static Vector3f bookTarget(Matrix4f book,boolean left){return book.transformPosition(new Vector3f(left?1.05f:-1.05f,.085f,.42f));}
  static Vector3f paperSupport(Matrix4f paper){return paper.transformPosition(new Vector3f(.68f,.065f,.10f));}
  static Vector3f penTip(Matrix4f paper,float tick,float stroke,float pen){var line=STROKES[QuietToyPlayRules049.stroke(tick)];return paper.transformPosition(new Vector3f(line[0]+(line[2]-line[0])*stroke,-.008f-.13f*(1-pen),line[1]+(line[3]-line[1])*stroke));}
  static Vector3f drawingHand(Matrix4f paper,float tick,float stroke,float pen){return penTip(paper,tick,stroke,pen).add(0,-.080f,-.040f);}
  static Matrix4f crayonMatrix(BiConsumer<String,PoseStack> anchor,float[] palm,Matrix4f paper,float tick,float stroke,float pen,float weight){
    var hand=ChaoTriangleAnimation.palm(anchor,palm,false);var direction=penTip(paper,tick,stroke,pen).sub(hand);
    if(direction.lengthSquared()<1e-9f)direction.set(0,1,0);
    return new Matrix4f().translate(hand).rotate(new Quaternionf().rotationTo(new Vector3f(0,-1,0),direction.normalize())).scale(CRAYON_SCALE*propWeight(weight));
  }
  private static void fit(Map<String,ChaoMeshAsset.Pose> poses,BiConsumer<String,PoseStack> anchor,float[] palm,boolean left,float weight,Vector3f target){
    var arm=poses.get(left?"arm_left":"arm_right");var stack=new PoseStack();anchor.accept(left?"arm_left":"arm_right",stack);
    float length=stack.last().pose().transformPosition(new Vector3f()).distance(target)*16;
    arm.reach=Math.clamp((float)Math.sqrt(Math.max(0,length*length-palm[1]*palm[1]-palm[2]*palm[2]))-Math.abs(palm[0])-.20f,0,MAX_REACH)*weight;
    ChaoTambourineAnimation.alignPalm(poses,anchor,ChaoTrumpetAnimation.reachedPalm(palm,arm.reach,left),weight,target,left,Math.abs(palm[0])<2.5f?1.10f:.90f);
  }
  static void pose(Map<String,ChaoMeshAsset.Pose> poses,BiConsumer<String,PoseStack> anchor,float[] right,float[] left,String kind,float weight,float sweep,float page,float stroke,float pen,float tick,float scale){
    ChaoInstrumentFocus.apply(poses,weight);poses.get("skull").rx+=.025f*weight;
    for(boolean side:new boolean[]{false,true}){if(side&&kind.equals(QuietToyPlayRules049.BROOM))continue;var arm=poses.get(side?"arm_left":"arm_right");arm.x*=1-weight;arm.y*=1-weight;arm.z=arm.z*(1-weight)-.2f*weight;}
    float ease=weight*weight*(3-2*weight);Vector3f a,b;
    if(kind.equals(QuietToyPlayRules049.BROOM)){var matrix=broomMatrix(weight,sweep,scale);a=broomTarget(matrix,false);b=broomTarget(matrix,true);}
    else if(kind.equals(QuietToyPlayRules049.BOOK)){var matrix=bookMatrix(anchor,weight);a=bookTarget(bookCover(matrix,weight,true),false);b=bookTarget(bookCover(matrix,weight,false),true);}
    else{var matrix=paperMatrix(anchor,weight);a=drawingHand(matrix,tick,stroke,pen);b=paperSupport(matrix);}
    fit(poses,anchor,right,false,ease,a);
    if(kind.equals(QuietToyPlayRules049.BROOM)){var free=poses.get("arm_left");free.x+=.9f*weight;free.z-=.30f*weight;free.rz+=.60f*weight;free.rx+=.10f*sweep*weight;}
    else fit(poses,anchor,left,true,ease,b);
  }
  static void pose(ChaoEntity c,ChaoModel model,LionfishQuietToyClip049 clip,float partial){
    if(!active(c)||model.baseToyPalm(false)==null||model.baseToyPalm(true)==null)return;
    float tick=GardenToyPerformance049.phase(c,partial),weight=GardenToyPerformance049.presence(c,partial),scale=c.state().immortal?.86f:c.state().adult?.80f:.64f;
    pose(model.officialPoses(),model::transformAccessoryAnchor,model.baseToyPalm(false),model.baseToyPalm(true),c.toyPlayingKind(),weight,clip.sweep(tick),clip.page(tick),clip.stroke(tick),clip.pen(tick),tick,scale);
  }
  private static void draw(ChaoEntity c,String part,Matrix4f matrix,PoseStack stack,MultiBufferSource buffers,int light){
    stack.pushPose();try{
      stack.mulPose(matrix);var mc=Minecraft.getInstance();mc.getItemRenderer().render(new ItemStack(ChaoGarden.item(c.toyPlayingKind())),ItemDisplayContext.NONE,false,stack,buffers,light,OverlayTexture.NO_OVERLAY,mc.getModelManager().getModel(MODELS.get(part)));
    }finally{stack.popPose();}
  }
  static boolean render(ChaoEntity c,ChaoModel model,PoseStack stack,MultiBufferSource buffers,int light,float partial){
    if(!active(c)||c.isInvisible()||!model.officialActive()||model.toyPalm(false)==null||model.toyPalm(true)==null)return false;
    float weight=GardenToyPerformance049.presence(c,partial);if(propWeight(weight)<=0)return true;
    float tick=GardenToyPerformance049.phase(c,partial),scale=c.state().immortal?.86f:c.state().adult?.80f:.64f;String kind=c.toyPlayingKind();
    if(kind.equals(QuietToyPlayRules049.BROOM))draw(c,"broom",new Matrix4f(broomMatrix(weight,RENDER_CLIP.sweep(tick),scale)),stack,buffers,light);
    else if(kind.equals(QuietToyPlayRules049.BOOK)){
      var matrix=bookMatrix(model::transformAccessoryAnchor,weight);
      draw(c,"book_left",bookCover(matrix,weight,true),stack,buffers,light);draw(c,"book_right",bookCover(matrix,weight,false),stack,buffers,light);
      if(QuietToyPlayRules049.turning(tick))draw(c,"book_page",new Matrix4f(matrix).translate(0,-.012f,0).rotateZ(-(float)Math.PI*RENDER_CLIP.page(tick)),stack,buffers,light);
    }else{
      var paper=paperMatrix(model::transformAccessoryAnchor,weight);
      draw(c,"crayon_box",boxMatrix(weight,scale),stack,buffers,light);
      draw(c,"paper_"+QuietToyPlayRules049.inkStage(tick),new Matrix4f(paper),stack,buffers,light);
      var crayon=crayonMatrix(model::transformAccessoryAnchor,model.toyPalm(false),paper,tick,RENDER_CLIP.stroke(tick),RENDER_CLIP.pen(tick),weight).translate(new Vector3f(CRAYON_GRIP).negate());
      draw(c,"crayon",crayon,stack,buffers,light);
    }
    return true;
  }
}
