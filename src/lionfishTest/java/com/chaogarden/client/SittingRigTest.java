package com.chaogarden.client;
import com.google.gson.*;
import com.chaogarden.core.ChaoState;
import com.mojang.blaze3d.vertex.PoseStack;
import java.nio.file.*;
import java.util.*;
import org.joml.Matrix4f;
import org.joml.Vector3f;
/** Uses the production animator and its real transition history, plus the current morphed feet. */
public final class SittingRigTest {
  static int checks,cases;static float maxFloorError,maxStep,minHead=Float.POSITIVE_INFINITY,maxAdultTilt,maxBabyTilt,maxTransitionPenetration;
  static void check(boolean ok,String why){checks++;if(!ok)throw new AssertionError(why);}
  public static void main(String[] args)throws Exception{run();}
  static void run()throws Exception{
    var root=Path.of("src/main/resources/assets/chao_garden");
    var clips=JsonParser.parseString(Files.readString(root.resolve("animations/official/chao.animation.json"))).getAsJsonObject().getAsJsonObject("animations");
    var assets=new HashMap<String,ChaoMeshAsset>();
    var rig=new OfficialChaoRig(clips,key->assets.computeIfAbsent(key,k->{try{return ChaoMeshAsset.parse(JsonParser.parseString(Files.readString(root.resolve("models/entity/"+k+".json"))).getAsJsonObject());}catch(Exception e){throw new RuntimeException(e);}}));
    try(var files=Files.list(root.resolve("models/entity/gameplay"))){for(var path:files.filter(p->p.toString().endsWith(".json")&&!p.toString().contains("_glass")).toList()){
      String nominal=path.getFileName().toString().replace("chao_","").replace(".json","");
      boolean prototype=nominal.equals("hero_run_proto052");
      for(boolean adult:new boolean[]{false,true})for(float sf:new float[]{-1,0,1})for(float rp:new float[]{-1,0,1})for(int stage=0;stage<(prototype?5:1);stage++){
        String model=prototype?(adult?"hero_run":"baby"):nominal;
        var state=new ChaoState();state.adult=adult;state.swimFly=sf;state.runPower=rp;state.setEvolutionStrength(adult?1.5f:1);var id=new UUID(cases++,83);var previous=new HashMap<String,Vector3f>();
        if(prototype){
          float fraction=stage/4f;state.align=adult?1:.5f*fraction;state.primaryType=adult?ChaoState.RUN_TYPE:ChaoState.NORMAL;
          int bars=Math.round(fraction*(adult?960:480));state.levels[ChaoState.RUN]=bars/100;state.progress[ChaoState.RUN]=bars%100;state.points[ChaoState.RUN]=bars;state.setEvolutionStrength(adult?1.2f:.8f);
        }
        for(int q=0;q<=440;q++){
          float t=q*.5f;boolean seated=t>=20&&t<100||t>=125&&t<180;
          rig.prepareDevelopment(model,state);rig.animateGameplay(model,id,seated?OfficialChaoRig.Motion.SIT:OfficialChaoRig.Motion.WALK,t,0,0,0,0,0,0,false,false);rig.development(state);rig.finishGameplayFrame(id,0,t);
          var mesh=rig.sampledMesh();var poses=rig.poses();var skull=new PoseStack();rig.transformBone("skull",skull);var inverse=new Matrix4f(skull.last().pose()).invert();var head=mesh.attachmentBounds("skull",poses);
          for(String foot:ChaoSittingPose.FEET){
            var box=mesh.partBounds(foot,poses);if(box==null)continue;var stack=new PoseStack();rig.transformBone(foot,stack);
            var min=new Vector3f(Float.POSITIVE_INFINITY);var max=new Vector3f(Float.NEGATIVE_INFINITY);float bottom=Float.NEGATIVE_INFINITY;
            for(int corner=0;corner<8;corner++){
              var p=stack.last().pose().transformPosition(new Vector3f(box[(corner&1)==0?0:3],box[(corner&2)==0?1:4],box[(corner&4)==0?2:5]).div(16));bottom=Math.max(bottom,p.y*16);
              var local=inverse.transformPosition(new Vector3f(p)).mul(16);min.min(local);max.max(local);
            }
            maxTransitionPenetration=Math.max(maxTransitionPenetration,bottom-24);
            String label=model+" adult="+adult+" morph="+sf+","+rp+" t="+t+" "+foot;
            float gap=Math.max(Math.max(head[0]-max.x,min.x-head[3]),Math.max(Math.max(head[1]-max.y,min.y-head[4]),Math.max(head[2]-max.z,min.z-head[5])));minHead=Math.min(minHead,gap);check(gap>.05f,"foot/head "+gap+" "+label);
            var origin=stack.last().pose().transformPosition(new Vector3f());var last=previous.put(foot,origin);if(last!=null){float delta=last.distance(origin)*16;maxStep=Math.max(maxStep,delta);check(delta<.65f,"smooth sitting transition "+delta+" "+label);}
            if(t>=29&&t<100||t>=134&&t<180){
              float error=Math.abs(24-bottom);maxFloorError=Math.max(maxFloorError,error);check(error<.08f,"seated floor "+error+" "+label);
              var forward=stack.last().pose().transformDirection(new Vector3f(0,0,-1));float tilt=(float)Math.toDegrees(Math.atan2(-forward.y,Math.sqrt(forward.x*forward.x+forward.z*forward.z)));
              if(adult)maxAdultTilt=Math.max(maxAdultTilt,Math.abs(tilt));else maxBabyTilt=Math.max(maxBabyTilt,Math.abs(tilt));check(Math.abs(tilt)<(adult?5:9),"shallow foot tilt "+tilt+" "+label);
            }
          }
        }
      }
    }}
    String result="PASS: "+checks+" sitting checks; "+cases+" form/age/morph cases with two real entry/exit transitions; maxFloorPixels="+maxFloorError+" minHeadPixels="+minHead+" maxHalfTickJointStepPixels="+maxStep+" adultTiltDegrees="+maxAdultTilt+" babyTiltDegrees="+maxBabyTilt+" maxTransitionPenetrationPixels="+maxTransitionPenetration+"\n";System.out.print(result);Files.writeString(Path.of("build/reports/lionfish/sitting-results.txt"),result);
  }
}
