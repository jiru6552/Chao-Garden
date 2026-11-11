package com.chaogarden.client;

import com.chaogarden.core.ChaoState;
import com.chaogarden.toys.RockingHorsePlayRules;
import com.google.gson.*;
import com.mojang.blaze3d.vertex.PoseStack;
import java.nio.file.*;
import java.util.*;
import org.joml.Matrix4f;
import org.joml.Vector3f;

/** Production fitter on the actual imported bodies, visible palms and original horse triangles. */
public final class RockingHorseRigTest {
  record Hands(float[] right,float[] left,String label,ChaoMeshAsset renderMesh,int animal){}
  static int failures;static final Map<String,Integer> groups=new TreeMap<>();static final List<String> examples=new ArrayList<>();
  private static void check(boolean ok,String group,String detail){if(!ok){failures++;groups.merge(group,1,Integer::sum);if(groups.get(group)<=4)examples.add(group+" "+detail);}}
  private static ChaoMeshAsset handOnly(JsonObject source,int animal){
    var json=source.deepCopy();
    for(var element:json.getAsJsonArray("bones")){
      var bone=element.getAsJsonObject();String name=bone.get("name").getAsString();var keep=new JsonArray();
      if(bone.has("meshes"))for(var entry:bone.getAsJsonArray("meshes")){
        var mesh=entry.getAsJsonObject();String part=mesh.has("name")?mesh.get("name").getAsString():"";
        if(animal<0&&(name.equals("arm_left__cube_1")||name.equals("arm_right__cube_1")||name.equals("chaos_hand_arm_left")||name.equals("chaos_hand_arm_right"))
            ||animal>=0&&name.startsWith("animal_0_"+animal+"_")&&Set.of("gene_hand","paw","hoof","claw").contains(part))keep.add(mesh);
      }bone.add("meshes",keep);
    }return ChaoMeshAsset.parse(json);
  }
  public static void main(String[] args)throws Exception{
    var clip=new LionfishRockingHorseClip();var horse=PropMeshClearance.triangles("chao_rocking_horse_idle");
    int posesChecked=0,contacts=0,clearance=0,emittedHands=0;float maxSeat=0,maxGrip=0,maxReach=0,minBody=999,minHead=999,minFeet=999,minHandHead=999,minHandBody=999,maxBarGap=-999,maxEmittedError=0;
    var bar=horse.stream().filter(f->java.util.Arrays.stream(f).allMatch(v->v.y>.1f)&&java.util.Arrays.stream(f).anyMatch(v->Math.abs(v.x)>.16f)).toList();check(!bar.isEmpty(),"bar_geometry","real crossbar triangles");
    try(var paths=Files.list(Path.of("src/main/resources/assets/chao_garden/models/entity/gameplay"))){
      for(var path:paths.filter(p->p.toString().endsWith(".json")&&!p.toString().contains("_glass")).toList()){
        String key=path.getFileName().toString();var mesh=ChaoMeshAsset.parse(JsonParser.parseString(Files.readString(path)).getAsJsonObject());var poses=mesh.createPoses();
        for(boolean adult:new boolean[]{false,true})for(float sf:new float[]{-1,0,1})for(float rp:new float[]{-1,0,1}){
          mesh.development(sf,rp,adult?1:.14f);poses.values().forEach(ChaoMeshAsset.Pose::reset);
          var palmMesh=key.contains("chaos_")?ChaoMeshAsset.parse(JsonParser.parseString(Files.readString(path.resolveSibling(key.replace(".json","_glass.json")))).getAsJsonObject()):mesh;
          if(palmMesh!=mesh)palmMesh.development(sf,rp,adult?1:.14f);
          var palmPoses=palmMesh==mesh?poses:palmMesh.createPoses();var skinJson=JsonParser.parseString(Files.readString(key.contains("chaos_")?path.resolveSibling(key.replace(".json","_glass.json")):path)).getAsJsonObject();
          var skin=handOnly(skinJson,-1);skin.development(sf,rp,adult?1:.14f);
          var hands=new ArrayList<Hands>();hands.add(new Hands(palmMesh.visiblePalm(palmPoses,false),palmMesh.visiblePalm(palmPoses,true),"skin",skin,-1));
          if(sf==0&&rp==0&&!key.contains("chaos_")&&!key.contains("omochao"))for(int animal=0;animal<ChaoState.ANIMALS.length;animal++){
            if((ChaoState.animalSlots(animal)&1)==0)continue;var part=path.getParent().getParent().resolve((animal<21?"overlays/":"cwe_parts/")+key);if(!Files.exists(part))continue;
            var overlay=ChaoMeshAsset.parse(JsonParser.parseString(Files.readString(part)).getAsJsonObject());var op=overlay.createPoses();var right=ChaoToyGrip.animalPalm(overlay,op,animal,adult,false);var left=ChaoToyGrip.animalPalm(overlay,op,animal,adult,true);
            if(right!=null&&left!=null)hands.add(new Hands(right,left,"animal"+animal,handOnly(JsonParser.parseString(Files.readString(part)).getAsJsonObject(),animal),animal));
          }
          float scale=key.contains("chaos_")?.86f:adult?.8f:.64f;
          for(var hand:hands)for(int tick=0;tick<=160;tick+=4){
            poses.values().forEach(ChaoMeshAsset.Pose::reset);poses.get("body").y=.8f;poses.get("body").rx=-.08f;poses.get("body").sy=1+.004f*(float)Math.sin(tick*.055f);poses.get("skull").y=.8f+.02f*(float)Math.sin(tick*.055f);ChaoSittingPose.apply(mesh,poses,adult);
            java.util.function.BiConsumer<String,PoseStack> anchor=(name,stack)->mesh.transformBone(name,stack,poses);
            float y=RockingHorsePlayRules.riderY(tick),z=RockingHorsePlayRules.riderZ(tick),weight=RockingHorsePlayRules.occupancy(tick);
            ChaoRockingHorseAnimation.fit(poses,anchor,name->mesh.attachmentBounds(name,poses),hand.right,hand.left,tick,weight,clip.angle(tick),scale,0,y,z);posesChecked++;
            String label=key+" adult="+adult+" morph="+sf+","+rp+" "+hand.label+" t="+tick;
            for(var p:poses.values())for(float value:new float[]{p.x,p.y,p.z,p.rx,p.ry,p.rz,p.reach})check(Float.isFinite(value),"finite",label);
            if(weight!=1)continue;
            var belly=mesh.attachmentBounds("body",poses);if(belly!=null){
              var at=new PoseStack();anchor.accept("body",at);var hip=at.last().pose().transformPosition(new Vector3f((belly[0]+belly[3])/32,belly[4]/16,(belly[2]+belly[5])/32));
              var target=ChaoRockingHorseAnimation.modelPoint(0,RockingHorsePlayRules.SEAT_Y+.006f,ChaoRockingHorseAnimation.seatZ(belly,scale),tick,scale,0,y,z);
              var up=new Vector3f(0,-(float)Math.cos(clip.angle(tick)),-(float)Math.sin(clip.angle(tick)));
              var forward=new Vector3f(0,(float)Math.sin(clip.angle(tick)),-(float)Math.cos(clip.angle(tick)));
              float lowest=Float.POSITIVE_INFINITY;
              for(int corner=0;corner<8;corner++){
                var p=at.last().pose().transformPosition(new Vector3f(belly[(corner&1)==0?0:3],belly[(corner&2)==0?1:4],belly[(corner&4)==0?2:5]).div(16));lowest=Math.min(lowest,p.dot(up));
              }
              float seatError=Math.max(Math.abs(lowest-target.dot(up)),Math.abs(new Vector3f(hip).sub(target).dot(forward)));
              maxSeat=Math.max(maxSeat,seatError);check(seatError<.0001f,"seat",label+" error="+seatError);
            }
            for(boolean left:new boolean[]{false,true}){
              var palm=left?hand.left:hand.right;if(palm==null)continue;var arm=poses.get(left?"arm_left":"arm_right");maxReach=Math.max(maxReach,arm.reach);
              var actual=ChaoTriangleAnimation.palm(anchor,ChaoTrumpetAnimation.reachedPalm(palm,arm.reach,left),left);
              var target=ChaoRockingHorseAnimation.modelPoint(left?RockingHorsePlayRules.GRIP_X:-RockingHorsePlayRules.GRIP_X,RockingHorsePlayRules.GRIP_Y+.005f,RockingHorsePlayRules.GRIP_Z-.025f,tick,scale,0,y,z);
              maxGrip=Math.max(maxGrip,actual.distance(target));check(actual.distance(target)<.03,"grip",label+" error="+actual.distance(target));check(arm.reach<=3.201,"reach",label);contacts++;
              if(tick%8==0){
                var rendered=hand.renderMesh.createPoses();rendered.forEach((name,p)->{var from=poses.get(name);if(from!=null)ChaoOverlayLayer.copy(from,p);if(name.contains(left?"right":"left"))p.visible=false;});
                if(hand.animal>=0&&!adult&&hand.animal<21){var gene=rendered.get("animal_0_"+hand.animal+(left?"_left":"_right"));gene.sx=gene.sy=gene.sz=.92f;}
                var sink=new RattleAnimationTest.Sink();hand.renderMesh.render(new PoseStack(),sink,0,0,-1,rendered,false);
                check(!sink.vertices.isEmpty(),"hand_render",label);if(sink.vertices.isEmpty())continue;
                maxEmittedError=Math.max(maxEmittedError,sink.center().distance(actual));check(sink.center().distance(actual)<.035,"hand_center",label+" error="+sink.center().distance(actual));
                var triangles=new ArrayList<Vector3f[]>();
                for(int n=0;n<sink.vertices.size();n+=4){triangles.add(new Vector3f[]{new Vector3f(sink.vertices.get(n)),new Vector3f(sink.vertices.get(n+1)),new Vector3f(sink.vertices.get(n+2))});if(new Vector3f(sink.vertices.get(n+2)).distanceSquared(new Vector3f(sink.vertices.get(n+3)))>1e-12)triangles.add(new Vector3f[]{new Vector3f(sink.vertices.get(n)),new Vector3f(sink.vertices.get(n+2)),new Vector3f(sink.vertices.get(n+3))});}
                var skull=new PoseStack();anchor.accept("skull",skull);var torso=new PoseStack();anchor.accept("body",torso);
                float headGap=PropMeshClearance.separation(triangles,new Matrix4f(skull.last().pose()).invert(),mesh.attachmentBounds("skull",poses));minHandHead=Math.min(minHandHead,headGap);check(headGap>0,"hand_head",label+" gap="+headGap);
                float bodyGap=PropMeshClearance.separation(triangles,new Matrix4f(torso.last().pose()).invert(),mesh.attachmentBounds("body",poses));minHandBody=Math.min(minHandBody,bodyGap);check(bodyGap>0,"hand_body",label+" gap="+bodyGap);
                var armFrame=new PoseStack();anchor.accept(left?"arm_left":"arm_right",armFrame);
                float[] box=hand.animal<0?hand.renderMesh.visibleHandBounds(rendered,left):hand.renderMesh.animalHandBounds(hand.animal,rendered,left);
                float barGap=PropMeshClearance.separation(bar,new Matrix4f(armFrame.last().pose()).invert().mul(ChaoRockingHorseAnimation.horseMatrix(scale,0,y,z,RockingHorsePlayRules.angle(tick))),box);
                maxBarGap=Math.max(maxBarGap,barGap);check(barGap<.30,"bar_contact",label+" gap="+barGap);emittedHands++;
              }
            }
            if(key.equals("chao_baby.json")&&hand.label.equals("skin")){
              var torso=new PoseStack();anchor.accept("body",torso);var inverse=new Matrix4f(torso.last().pose()).invert();
              for(String name:new String[]{"foot_left","foot_right"}){
                var ankle=new PoseStack();anchor.accept(name,ankle);var point=inverse.transformPosition(ankle.last().pose().transformPosition(new Vector3f())).mul(16);
                for(int axis=0;axis<3;axis++)check(point.get(axis)>=belly[axis]-.05f&&point.get(axis)<=belly[axis+3]+.05f,"ankle_connected",label+" "+name+" "+point);
              }
            }
            if(hand.label.equals("skin")&&tick%16==0){
              var horseFrame=ChaoRockingHorseAnimation.horseMatrix(scale,0,y,z,RockingHorsePlayRules.angle(tick));
              for(String bone:new String[]{"body","skull","foot_left","foot_right"}){
                var bounds=bone.startsWith("foot")?mesh.partBounds(bone,poses):mesh.attachmentBounds(bone,poses);if(bounds==null)continue;
                var frame=new PoseStack();anchor.accept(bone,frame);float gap=PropMeshClearance.separation(horse,new Matrix4f(frame.last().pose()).invert().mul(horseFrame),bounds);
                if(bone.equals("body"))minBody=Math.min(minBody,gap);else if(bone.equals("skull"))minHead=Math.min(minHead,gap);else minFeet=Math.min(minFeet,gap);
                check(gap>=-.001f,"horse_"+bone,label+" gap="+gap);clearance++;
              }
            }
          }
        }
      }
    }
    System.out.println("Rocking horse failure groups: "+groups);
    System.out.println("Rocking horse mesh fit: "+posesChecked+" poses; "+contacts+" hands; "+clearance+" original horse/body/head/feet checks; max hip="+maxSeat+" blocks; max grip="+maxGrip+" blocks; max arm extension="+maxReach+" pixels; min body="+minBody+", head="+minHead+", feet="+minFeet+" pixels; emitted hands="+emittedHands+" max rendered palm error="+maxEmittedError+"; min hand/head="+minHandHead+", hand/body="+minHandBody+"; max real bar gap="+maxBarGap+" pixels");
    if(failures>0)throw new AssertionError(failures+" rocking horse failures:\n"+String.join("\n",examples));
  }
}
