package com.chaogarden.client;
import com.google.gson.*;
import com.mojang.blaze3d.vertex.PoseStack;
import java.nio.file.*;
import org.joml.Vector3f;
import com.chaogarden.toys.TambourinePlayRules;
final class TambourineRigTest {
  private static int checks,cases;private static float maxError,maxContact,maxShift;
  private static void check(boolean ok,String why){checks++;if(!ok)throw new AssertionError(why);}
  static void run()throws Exception{
    var original=ChaoTambourineAnimation.ORIGINAL_ORIENTATION.transform(new Vector3f(ChaoTambourineAnimation.NORMAL));
    var actual=ChaoTambourineAnimation.ORIENTATION.transform(new Vector3f(ChaoTambourineAnimation.NORMAL));
    check(Math.abs(original.dot(actual))<1e-6&&actual.y>.98f,"corrected quarter-turn faces the authored skin upward (opposite mesh normal)");
    var vertices=new java.util.ArrayList<Vector3f>();var decorated=new java.util.ArrayList<Vector3f>();var underside=new java.util.ArrayList<Vector3f>();String material="";
    for(String line:Files.readAllLines(Path.of("src/main/resources/assets/chao_garden/models/sa2_02515/chao_tambourine.obj"))){
      if(line.startsWith("v ")){var a=line.split("\\s+");vertices.add(new Vector3f(Float.parseFloat(a[1]),Float.parseFloat(a[2]),Float.parseFloat(a[3])));}
      if(line.startsWith("usemtl "))material=line.substring(7);
      if(line.startsWith("f ")&&(material.equals("sa2_star01_52")||material.equals("sa2_tanba01_52")))for(String a:line.substring(2).split(" "))(material.equals("sa2_star01_52")?decorated:underside).add(vertices.get(Integer.parseInt(a.split("/")[0])-1));
    }
    check(!decorated.isEmpty()&&!underside.isEmpty(),"authored decorated and underside surfaces found");
    float decoratedY=0,undersideY=0;for(var v:decorated)decoratedY+=ChaoTambourineAnimation.ORIENTATION.transform(new Vector3f(v)).y/decorated.size();for(var v:underside)undersideY+=ChaoTambourineAnimation.ORIENTATION.transform(new Vector3f(v)).y/underside.size();
    check(decoratedY<undersideY-.10f,"actual decorated star face is above open underside in Y-down model coordinates");
    var oriented=new org.joml.Matrix4f().rotate(ChaoTambourineAnimation.ORIENTATION);check(ChaoTambourineAnimation.hitTarget(oriented,1).y<oriented.transformPosition(new Vector3f(ChaoTambourineAnimation.HIT).sub(.5f,.5f,.5f)).y,"striking palm approaches the decorated side");
    var clip=new LionfishTambourineClip();int sounds=0;
    for(boolean left:new boolean[]{false,true}){
      for(float t:new float[]{-1,0,160,161,Float.NaN})check(clip.sample(t,left).length()<1e-6,"rest boundary");
      var first=clip.sample(24,left);clip.sample(60,!left);check(first.equals(clip.sample(24,left)),"no actor pose leak");
    }
    for(int t=0;t<=160;t++)if(TambourinePlayRules.soundDue(t)){sounds++;check(Math.abs(clip.tap(t)-1)<1e-5,"sound occurs at contact");check(TambourinePlayRules.happy(t),"happy during tap");}
    check(sounds==8,"eight taps");
    try(var files=Files.list(Path.of("src/main/resources/assets/chao_garden/models/entity/gameplay"))){
      for(var path:files.filter(p->p.toString().endsWith(".json")&&!p.toString().contains("_glass")).toList()){
        var mesh=ChaoMeshAsset.parse(JsonParser.parseString(Files.readString(path)).getAsJsonObject());var poses=mesh.createPoses();
        for(float sf:new float[]{-1,0,1})for(float rp:new float[]{-1,0,1}){
          mesh.development(sf,rp,1);poses.values().forEach(ChaoMeshAsset.Pose::reset);float[] right=mesh.toyPalm(poses,false),left=mesh.toyPalm(poses,true);check(right!=null&&left!=null,"both hands exist");cases++;
          java.util.function.BiConsumer<String,PoseStack> anchor=(name,stack)->mesh.transformBone(name,stack,poses);
          for(int q=0;q<=640;q++){
            float tick=q*.25f,w=TambourinePlayRules.presence(tick);poses.values().forEach(ChaoMeshAsset.Pose::reset);
            for(boolean l:new boolean[]{false,true})ChaoTambourineAnimation.applyArm(poses.get(l?"arm_left":"arm_right"),clip,tick,w,l);
            float shift=ChaoTambourineAnimation.alignTap(poses,anchor,right,left,w,clip.tap(tick));if(w==1)maxShift=Math.max(maxShift,shift);
            var prop=ChaoTambourineAnimation.propMatrix(anchor,right,w);var hand=new PoseStack();anchor.accept("arm_right",hand);
            float error=prop.transformPosition(new Vector3f(ChaoTambourineAnimation.GRIP).sub(.5f,.5f,.5f)).distance(hand.last().pose().transformPosition(new Vector3f(right).div(16)));maxError=Math.max(maxError,error);check(error<1e-6,"right rim grip");
            for(var arm:java.util.List.of(poses.get("arm_left"),poses.get("arm_right")))check(Float.isFinite(arm.rx+arm.ry+arm.rz+arm.x+arm.y+arm.z),"finite arm");
            if(q%4==0&&TambourinePlayRules.soundDue((int)tick)){
              var tapHand=new PoseStack();anchor.accept("arm_left",tapHand);float gap=tapHand.last().pose().transformPosition(new Vector3f(left).div(16)).distance(ChaoTambourineAnimation.hitTarget(ChaoTambourineAnimation.propMatrix(anchor,right,w),1));maxContact=Math.max(maxContact,gap);check(gap<.10f,"tap is within hand radius of head: "+path+" gap="+gap+" shift="+shift+" hand="+tapHand.last().pose().transformPosition(new Vector3f(left).div(16))+" target="+ChaoTambourineAnimation.hitTarget(ChaoTambourineAnimation.propMatrix(anchor,right,w),1));
            }
          }
        }
      }
    }
    String result="PASS: "+checks+" tambourine checks, "+cases+" rig/morph cases, 641 subticks, maxGripError="+maxError+" maxContactGap="+maxContact+" maxRequestedShoulderShiftPixels="+maxShift+"; visual review separate.\n";
    Files.writeString(Path.of("build/reports/lionfish/tambourine-results.txt"),result);System.out.print(result);
  }
}
