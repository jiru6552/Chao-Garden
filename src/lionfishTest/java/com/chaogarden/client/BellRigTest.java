package com.chaogarden.client;
import com.google.gson.*;
import com.mojang.blaze3d.vertex.PoseStack;
import java.nio.file.*;
import java.util.*;
import org.joml.Vector3f;
import org.joml.Matrix4f;
import com.chaogarden.toys.BellPlayRules;
final class BellRigTest {
  private static int checks,cases;private static float maxError;
  private static void check(boolean ok,String why){checks++;if(!ok)throw new AssertionError(why);}
  static void run()throws Exception{
    var clip=new LionfishBellClip();int sounds=0;
    for(boolean left:new boolean[]{false}){
      for(float tick:new float[]{-1,0,160,161,Float.NaN})check(clip.sample(tick).length()<1e-6,"clip returns to rest");
      var first=clip.sample(24);clip.sample(60);check(first.equals(clip.sample(24)),"no shared actor pose leak");
    }
    for(int tick=0;tick<=160;tick++)if(BellPlayRules.soundDue(tick)){
      sounds++;var r=clip.sample(tick);var l=clip.sample(tick);
      check(Math.abs(r.z-(float)Math.toRadians(62))<1e-5,"alternating shake extrema match server sounds");
      check(BellPlayRules.happy(tick),"all sound cues during happy shake");
    }check(sounds==8,"8 cues only");
    try(var files=Files.list(Path.of("src/main/resources/assets/chao_garden/models/entity/gameplay"))){
      for(var path:files.filter(p->p.toString().endsWith(".json")&&!p.toString().contains("_glass")).toList()){
        var data=JsonParser.parseString(Files.readString(path)).getAsJsonObject();var mesh=ChaoMeshAsset.parse(data);var poses=mesh.createPoses();
        for(float sf:new float[]{-1,0,1})for(float rp:new float[]{-1,0,1})for(boolean left:new boolean[]{false}){
          mesh.development(sf,rp,1);poses.values().forEach(ChaoMeshAsset.Pose::reset);
          float[] palm=mesh.toyPalm(poses,left);check(palm!=null,"both hands exist: "+path);cases++;
          String arm=left?"arm_left":"arm_right";boolean tip=path.getFileName().toString().startsWith("chao_chaos_");
          poses.forEach((n,p)->p.visible=n.equals(arm+(tip?"__cube_0":"__cube_1")));
          var sink=new RattleAnimationTest.Sink();mesh.render(new PoseStack(),sink,0,0,-1,poses,false);
          var transform=new PoseStack();mesh.transformBone(arm,transform,poses);var inverse=transform.last().pose().invert(new Matrix4f());
          for(var point:sink.vertices){var v=inverse.transformPosition(new Vector3f(point));point[0]=v.x;point[1]=v.y;point[2]=v.z;}
          var measured=sink.center();if(tip){float min=Float.POSITIVE_INFINITY,max=Float.NEGATIVE_INFINITY;for(var v:sink.vertices){min=Math.min(min,v[0]);max=Math.max(max,v[0]);}float inset=Math.min(.25f/16,(max-min)*.15f);measured.x=left?max-inset:min+inset;}
          check(measured.distance(new Vector3f(palm).div(16))<1e-6,"grip follows independently rendered hand/tip");
          poses.values().forEach(ChaoMeshAsset.Pose::reset);
          for(int q=0;q<=640;q++){
            float tick=q*.25f;var p=poses.get(arm);p.reset();ChaoBellAnimation.applyArm(p,clip,tick,BellPlayRules.presence(tick));
            check(clip.sample(tick).isFinite(),"finite Lionfish sample");
            var prop=new PoseStack();mesh.transformBone(arm,prop,poses);var hand=prop.last().pose().transformPosition(new Vector3f(palm).div(16));
            ChaoBellAnimation.propTransform(prop,palm,BellPlayRules.presence(tick));
            var actual=prop.last().pose().transformPosition(new Vector3f(0,.39f,0));float error=actual.distance(hand);maxError=Math.max(maxError,error);check(error<1e-6,"bell remain in their actual hand");
          }
        }
      }
    }
    String result="PASS: "+checks+" bell checks, "+cases+" rig/morph/hand cases, 641 subticks, max grip error="+maxError+"; visual review separate.\n";
    Files.writeString(Path.of("build/reports/lionfish/bell-results.txt"),result);System.out.print(result);
  }
}
