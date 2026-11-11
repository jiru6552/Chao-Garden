package com.chaogarden.client;

import com.chaogarden.ChaoGarden;
import com.chaogarden.entity.ChaoEntity;
import com.chaogarden.toys.*;
import com.mojang.blaze3d.vertex.PoseStack;
import java.util.Map;
import java.util.HashMap;
import java.util.function.Function;
import com.chaogarden.core.ChaoState;
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
import org.joml.Vector3f;

/** Original small toys, held by visible palms; pogo soles follow the actual pedals. */
@EventBusSubscriber(modid=ChaoGarden.MODID,value=Dist.CLIENT,bus=EventBusSubscriber.Bus.MOD)
final class ChaoActiveToyAnimation049 {
  private static final String[] KINDS={ActiveToyPlayRules049.CAR,ActiveToyPlayRules049.SONIC,ActiveToyPlayRules049.SHADOW,ActiveToyPlayRules049.POGO};
  private static final LionfishActiveToyClip049 RENDER_CLIP=new LionfishActiveToyClip049();
  static final float MAX_REACH=3.2f;
  private static ModelResourceLocation model(String kind){return ModelResourceLocation.standalone(ResourceLocation.fromNamespaceAndPath(ChaoGarden.MODID,"item/"+kind+"_hand"));}
  @SubscribeEvent public static void models(ModelEvent.RegisterAdditional event){for(String kind:KINDS)event.register(model(kind));}
  static boolean active(ChaoEntity c){return ActiveToyPlayRules049.supports(c.toyPlayingKind())&&GardenToyPerformance049.active(c,c.toyPlayingKind());}
  static boolean happy(ChaoEntity c,float p){return active(c)&&ActiveToyPlayRules049.happy(GardenToyPerformance049.phase(c,p));}
  static float propWeight(float w){return Math.clamp((w-.45f)/.55f,0,1);}
  static Matrix4f propMatrix(BiConsumer<String,PoseStack> anchor,String kind,float t,float w,LionfishActiveToyClip049 clip){
    float visible=propWeight(w);
    if(kind.equals(ActiveToyPlayRules049.POGO)){
      float s=ActiveToyPlayRules049.POGO_SCALE*visible;
      return new Matrix4f().translate(0,1.501f+s*(ActiveToyPlayRules049.POGO_PEDAL-.5f),-.24f).scale(s,-s,-s);
    }
    if(kind.equals(ActiveToyPlayRules049.CAR)){
      float s=.60f*visible;
      return new Matrix4f().translate(0,1.501f-.5f*s,-.45f-.065f*clip.push(t)).scale(s,-s,-s);
    }
    var body=new PoseStack();anchor.accept("body",body);
    return new Matrix4f(body.last().pose()).translate(0,.04f-.035f*clip.lift(t),-.29f)
        .rotateZ(.035f*(float)Math.sin(t*.10f)*w).scale(.38f*visible,-.38f*visible,-.38f*visible);
  }
  static Vector3f gripTarget(Matrix4f prop,String kind,boolean left){
    Vector3f p,n;
    if(kind.equals(ActiveToyPlayRules049.POGO)){
      p=new Vector3f(left?.273f:-.273f,.4182036f,.0652465f);n=new Vector3f(0,-.7071068f,.7071068f);
    }else if(kind.equals(ActiveToyPlayRules049.CAR)){
      p=new Vector3f(left?.13f:-.13f,-.02163597f,-.1931909f);n=new Vector3f(0,.8526014f,-.52256185f);
    }else if(left){p=new Vector3f(.19482157f,-.06478134f,.06651182f);n=new Vector3f(.22753964f,.64948946f,-.7255268f);}
    else {p=new Vector3f(-.19523951f,-.06416742f,.06582883f);n=new Vector3f(-.2186724f,.65077925f,-.7270961f);}
    return prop.transformPosition(p.fma(.035f,n));
  }
  private static void fit(Map<String,ChaoMeshAsset.Pose> poses,BiConsumer<String,PoseStack> anchor,float[] palm,boolean left,float w,Vector3f target){
    var arm=poses.get(left?"arm_left":"arm_right");var at=new PoseStack();anchor.accept(left?"arm_left":"arm_right",at);
    float length=at.last().pose().transformPosition(new Vector3f()).distance(target)*16;
    arm.reach=Math.clamp((float)Math.sqrt(Math.max(0,length*length-palm[1]*palm[1]-palm[2]*palm[2]))-Math.abs(palm[0])-.20f,0,MAX_REACH)*w;
    ChaoTambourineAnimation.alignPalm(poses,anchor,ChaoTrumpetAnimation.reachedPalm(palm,arm.reach,left),w,target,left,.90f);
  }
  static void pose(Map<String,ChaoMeshAsset.Pose> poses,BiConsumer<String,PoseStack> anchor,float[] right,float[] left,String kind,float t,float w,LionfishActiveToyClip049 clip){
    ChaoInstrumentFocus.apply(poses,w);
    var head=poses.get("skull");head.rx+=w*(kind.equals(ActiveToyPlayRules049.CAR)?.07f*clip.push(t):.015f);
    if(kind.equals(ActiveToyPlayRules049.CAR))poses.get("body").z-=.05f*clip.push(t)*w;
    if(kind.equals(ActiveToyPlayRules049.POGO)){var body=poses.get("body");body.y+=.55f*clip.flex(t)*w;head.y+=.55f*clip.flex(t)*w;}
    var prop=propMatrix(anchor,kind,t,w,clip);float ease=w*w*(3-2*w);
    fit(poses,anchor,right,false,ease,gripTarget(new Matrix4f(prop),kind,false));fit(poses,anchor,left,true,ease,gripTarget(new Matrix4f(prop),kind,true));
  }
  private record FootRig(ChaoMeshAsset mesh,Map<String,ChaoMeshAsset.Pose> poses){}
  private static final Map<String,FootRig> FOOT_RIGS=new HashMap<>();
  private static float[] footBounds(ChaoEntity c,ChaoModel model,String name){
    int animal=c.state().animalParts[ChaoState.FEET];
    boolean replacement=c.state().allowsAnimalParts()&&animal>=0&&(ChaoState.animalSlots(animal)&(1<<ChaoState.FEET))!=0;
    boolean glass=c.state().immortal&&c.state().character.equals("normal");
    if(!replacement&&!glass)return model.accessoryBounds(name);
    String path=replacement?(animal<21?"overlays/":"cwe_parts/")+"chao_"+model.officialGeometryKey():"gameplay/chao_"+model.gameplayFormKey()+"_glass";
    var rig=FOOT_RIGS.computeIfAbsent(path,key->{var mesh=ChaoMeshAsset.load(key);return new FootRig(mesh,mesh.createPoses());});
    rig.poses.forEach((key,p)->{p.reset();var from=model.officialPoses().get(key);if(from!=null)ChaoOverlayLayer.copy(from,p);
      if(key.startsWith("animal_")){p.visible=replacement&&key.startsWith("animal_"+ChaoState.FEET+"_"+animal+"_");if(p.visible&&!c.state().adult&&animal<21)p.sx=p.sy=p.sz=.92f;}
    });
    if(name.equals("feet_bone_right")){var alias=rig.poses.get("foot_right");var from=model.officialPoses().get(name);if(alias!=null&&from!=null)ChaoOverlayLayer.copy(from,alias);}
    return rig.mesh.partBounds(name.equals("feet_bone_right")?"foot_right":name,rig.poses);
  }
  static void fitFeet(Map<String,ChaoMeshAsset.Pose> poses,BiConsumer<String,PoseStack> anchor,Function<String,float[]> bounds,float weight){
    var body=new PoseStack();anchor.accept("body",body);var inverse=new Matrix4f(body.last().pose()).invert();
    for(boolean left:new boolean[]{false,true}){
      String name=left?"foot_left":poses.containsKey("foot_right")?"foot_right":"feet_bone_right";var pose=poses.get(name);var box=bounds.apply(name);
      if(pose==null||box==null)continue;
      var at=new PoseStack();anchor.accept(name,at);
      var low=new Vector3f(Float.POSITIVE_INFINITY);var high=new Vector3f(Float.NEGATIVE_INFINITY);
      for(int corner=0;corner<8;corner++){
        var v=at.last().pose().transformPosition(new Vector3f(box[(corner&1)==0?0:3],box[(corner&2)==0?1:4],box[(corner&4)==0?2:5]).div(16));low.min(v);high.max(v);
      }
      var sole=new Vector3f((low.x+high.x)*.5f,high.y,(low.z+high.z)*.5f);
      var target=new Vector3f((poses.containsKey("foot_right")||poses.containsKey("feet_bone_right"))?(left?.14f:-.14f):0,1.501f,-.228f);
      var delta=inverse.transformDirection(target.sub(sole)).mul(16*weight);
      pose.x+=Math.clamp(delta.x,-5,5);pose.y+=Math.clamp(delta.y,-5,5);pose.z+=Math.clamp(delta.z,-5,5);
    }
  }
  static void pose(ChaoEntity c,ChaoModel m,LionfishActiveToyClip049 clip,float partial){
    if(!active(c)||m.baseToyPalm(false)==null||m.baseToyPalm(true)==null)return;
    String kind=c.toyPlayingKind();float t=GardenToyPerformance049.phase(c,partial),w=GardenToyPerformance049.presence(c,partial);
    pose(m.officialPoses(),m::transformAccessoryAnchor,m.baseToyPalm(false),m.baseToyPalm(true),kind,t,w,clip);
    if(kind.equals(ActiveToyPlayRules049.POGO))fitFeet(m.officialPoses(),m::transformAccessoryAnchor,name->footBounds(c,m,name),w);
  }
  static boolean render(ChaoEntity c,ChaoModel m,PoseStack stack,MultiBufferSource buffers,int light,float partial){
    if(!active(c)||c.isInvisible()||!m.officialActive()||m.toyPalm(false)==null||m.toyPalm(true)==null)return false;
    String kind=c.toyPlayingKind();float t=GardenToyPerformance049.phase(c,partial),w=GardenToyPerformance049.presence(c,partial);if(propWeight(w)<=0)return true;
    stack.pushPose();try{
      stack.mulPose(propMatrix(m::transformAccessoryAnchor,kind,t,w,RENDER_CLIP));var mc=Minecraft.getInstance();
      mc.getItemRenderer().render(new ItemStack(ChaoGarden.item(kind)),ItemDisplayContext.NONE,false,stack,buffers,light,OverlayTexture.NO_OVERLAY,mc.getModelManager().getModel(model(kind)));
    }finally{stack.popPose();}return true;
  }
}
