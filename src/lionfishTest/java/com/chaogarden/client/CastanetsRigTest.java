package com.chaogarden.client;
import com.google.gson.*;
import com.mojang.blaze3d.vertex.PoseStack;
import java.nio.file.*;
import org.joml.Vector3f;
import com.chaogarden.toys.CastanetsPlayRules;
final class CastanetsRigTest {
  private static int checks,cases;private static float maxError,maxContact,maxShift;
  private static void check(boolean ok,String why){checks++;if(!ok)throw new AssertionError(why);}
  static void run()throws Exception{
    var identity=new org.joml.Matrix4f();var hinge=new Vector3f(ChaoCastanetsAnimation.HINGE).sub(.5f,.5f,.5f);
    for(float tap:new float[]{0,.25f,.5f,.75f,1})check(ChaoCastanetsAnimation.lidMatrix(identity,tap).transformPosition(new Vector3f(hinge)).distance(hinge)<1e-6,"lid rotates around a fixed hinge");
    check(ChaoCastanetsAnimation.lidMatrix(identity,1).equals(identity,1e-6f),"contact keyframe closes the shells");
    var clip=new LionfishCastanetsClip();int sounds=0;
    for(boolean left:new boolean[]{false,true}){
      for(float t:new float[]{-1,0,160,161,Float.NaN})check(clip.sample(t,left).length()<1e-6,"rest boundary");
      var first=clip.sample(24,left);clip.sample(60,!left);check(first.equals(clip.sample(24,left)),"no actor pose leak");
    }
    for(int t=0;t<=160;t++)if(CastanetsPlayRules.soundDue(t)){sounds++;check(Math.abs(clip.tap(t)-1)<1e-5,"sound occurs at contact");check(CastanetsPlayRules.happy(t),"happy during tap");}
    check(sounds==15,"fifteen taps");
    try(var files=Files.list(Path.of("src/main/resources/assets/chao_garden/models/entity/gameplay"))){
      for(var path:files.filter(p->p.toString().endsWith(".json")&&!p.toString().contains("_glass")).toList()){
        var mesh=ChaoMeshAsset.parse(JsonParser.parseString(Files.readString(path)).getAsJsonObject());var poses=mesh.createPoses();
        for(float sf:new float[]{-1,0,1})for(float rp:new float[]{-1,0,1}){
          mesh.development(sf,rp,1);poses.values().forEach(ChaoMeshAsset.Pose::reset);float[] right=mesh.toyPalm(poses,false),left=mesh.toyPalm(poses,true);check(right!=null&&left!=null,"both hands exist");cases++;
          java.util.function.BiConsumer<String,PoseStack> anchor=(name,stack)->mesh.transformBone(name,stack,poses);
          for(int q=0;q<=640;q++){
            float tick=q*.25f,w=CastanetsPlayRules.presence(tick);poses.values().forEach(ChaoMeshAsset.Pose::reset);
            for(boolean l:new boolean[]{false,true})ChaoCastanetsAnimation.applyArm(poses.get(l?"arm_left":"arm_right"),clip,tick,w,l);
            float shift=ChaoCastanetsAnimation.alignTap(poses,anchor,right,left,w,clip.tap(tick));if(w==1)maxShift=Math.max(maxShift,shift);
            var prop=ChaoCastanetsAnimation.propMatrix(anchor,right,w);var hand=new PoseStack();anchor.accept("arm_right",hand);
            float error=prop.transformPosition(new Vector3f(ChaoCastanetsAnimation.GRIP).sub(.5f,.5f,.5f)).distance(hand.last().pose().transformPosition(new Vector3f(right).div(16)));maxError=Math.max(maxError,error);check(error<1e-6,"right rim grip");
            for(var arm:java.util.List.of(poses.get("arm_left"),poses.get("arm_right")))check(Float.isFinite(arm.rx+arm.ry+arm.rz+arm.x+arm.y+arm.z),"finite arm");
            if(q%4==0&&CastanetsPlayRules.soundDue((int)tick)){
              var tapHand=new PoseStack();anchor.accept("arm_left",tapHand);float gap=tapHand.last().pose().transformPosition(new Vector3f(left).div(16)).distance(ChaoCastanetsAnimation.hitTarget(ChaoCastanetsAnimation.propMatrix(anchor,right,w),1));maxContact=Math.max(maxContact,gap);check(gap<.035f,"tap is within hand radius of head: "+path+" gap="+gap+" shift="+shift+" hand="+tapHand.last().pose().transformPosition(new Vector3f(left).div(16))+" target="+ChaoCastanetsAnimation.hitTarget(ChaoCastanetsAnimation.propMatrix(anchor,right,w),1));
            }
          }
        }
      }
    }
    String result="PASS: "+checks+" castanets checks, "+cases+" rig/morph cases, 641 subticks, maxGripError="+maxError+" maxContactGap="+maxContact+" maxRequestedShoulderShiftPixels="+maxShift+"; visual review separate.\n";
    Files.writeString(Path.of("build/reports/lionfish/castanets-results.txt"),result);System.out.print(result);
  }
}
