package com.chaogarden.client;
import com.chaogarden.toys.*;
import com.chaogarden.core.ChaoState;
import com.google.gson.*;
import com.mojang.blaze3d.vertex.PoseStack;
import java.nio.file.*;import java.util.*;import java.util.function.BiConsumer;
import org.joml.Matrix4f;import org.joml.Vector3f;
/** Contacts are tested against the actual supplied source surfaces and visible hand centers. */
public final class ActiveToyRigTest049 {
 record Hands(float[] right,float[] left,String label,ChaoMeshAsset renderMesh,int animal,ChaoMeshAsset parts){}
 static int failures,samples,emitted;static float maxRendered,maxFloorError;static float minHandHead=Float.POSITIVE_INFINITY,minHandBody=Float.POSITIVE_INFINITY;static float minFeet=Float.POSITIVE_INFINITY,minHandFeet=Float.POSITIVE_INFINITY;static float maxError,maxReach,maxCarReach,minHead=Float.POSITIVE_INFINITY,minBody=Float.POSITIVE_INFINITY;
 static Map<String,Float> headsByKind=new TreeMap<>(),feetByKind=new TreeMap<>();static Map<String,Integer> groups=new TreeMap<>();static List<String> examples=new ArrayList<>();
 static void check(boolean okay,String why){if(!okay){failures++;groups.merge(why.split(" ")[0]+" "+(why.contains("chao_toy_car")?"car":why.contains("chao_pogo_stick")?"pogo":"doll")+" "+(why.contains("skin")?"skin":"animal"),1,Integer::sum);if(examples.size()<20)examples.add(why);}}
 static ChaoMeshAsset handOnly(JsonObject original,int animal){
  var json=original.deepCopy();for(var element:json.getAsJsonArray("bones")){
   var bone=element.getAsJsonObject();String name=bone.get("name").getAsString();var keep=new JsonArray();
   if(bone.has("meshes"))for(var value:bone.getAsJsonArray("meshes")){
    var m=value.getAsJsonObject();String part=m.has("name")?m.get("name").getAsString():"";
    if(animal<0&&(name.equals("arm_left__cube_1")||name.equals("arm_right__cube_1")||name.equals("chaos_hand_arm_left")||name.equals("chaos_hand_arm_right"))||animal>=0&&name.startsWith("animal_0_"+animal+"_")&&Set.of("gene_hand","paw","hoof","claw").contains(part))keep.add(m);
   }bone.add("meshes",keep);
  }return ChaoMeshAsset.parse(json);
 }
 static List<Vector3f[]> triangles(String kind)throws Exception{
  var vertices=new ArrayList<Vector3f>();var faces=new ArrayList<Vector3f[]>();
  for(String line:Files.readAllLines(Path.of("src/main/resources/assets/chao_garden/models/active02549/"+kind+"_hand.obj"))){var w=line.split("\\s+");
   if(line.startsWith("v "))vertices.add(new Vector3f(Float.parseFloat(w[1])-.5f,Float.parseFloat(w[2])-.5f,Float.parseFloat(w[3])-.5f));
   if(line.startsWith("f "))for(int i=2;i<w.length-1;i++)faces.add(new Vector3f[]{vertices.get(Integer.parseInt(w[1].split("/")[0])-1),vertices.get(Integer.parseInt(w[i].split("/")[0])-1),vertices.get(Integer.parseInt(w[i+1].split("/")[0])-1)});
  }return faces;
 }
 static float[] bounds(Hands h,ChaoMeshAsset mesh,Map<String,ChaoMeshAsset.Pose> poses,boolean adult,String name){
  if(name.startsWith("hand_")){
   boolean left=name.endsWith("left");var rp=h.renderMesh.createPoses();rp.forEach((n,p)->{var from=poses.get(n);if(from!=null)ChaoOverlayLayer.copy(from,p);});
   if(h.animal>=0&&!adult&&h.animal<21){var part=rp.get("animal_0_"+h.animal+(left?"_left":"_right"));if(part!=null)part.sx=part.sy=part.sz=.92f;}
   return h.animal<0?h.renderMesh.visibleHandBounds(rp,left):h.renderMesh.animalHandBounds(h.animal,rp,left);
  }
  if(name.startsWith("foot")||name.equals("feet_bone_right")){
   boolean replacement=h.animal>=0&&(ChaoState.animalSlots(h.animal)&(1<<ChaoState.FEET))!=0;
   var use=replacement||h.animal<0?h.parts:mesh;
   if(use==mesh)return mesh.partBounds(name,poses);
   var fp=use.createPoses();fp.forEach((n,p)->{var from=poses.get(n);if(from!=null)ChaoOverlayLayer.copy(from,p);
    if(n.startsWith("animal_")){p.visible=replacement&&n.startsWith("animal_"+ChaoState.FEET+"_"+h.animal+"_");if(p.visible&&!adult&&h.animal<21)p.sx=p.sy=p.sz=.92f;}});
   if(name.equals("feet_bone_right")){var alias=fp.get("foot_right");var from=poses.get(name);if(alias!=null&&from!=null)ChaoOverlayLayer.copy(from,alias);}
   return use.partBounds(name.equals("feet_bone_right")?"foot_right":name,fp);
  }
  return mesh.attachmentBounds(name,poses);
 }
 public static void main(String[] args)throws Exception{
  String[] kinds={ActiveToyPlayRules049.CAR,ActiveToyPlayRules049.SONIC,ActiveToyPlayRules049.SHADOW,ActiveToyPlayRules049.POGO};var geometry=new HashMap<String,List<Vector3f[]>>();for(String k:kinds)geometry.put(k,triangles(k));
  var clip=new LionfishActiveToyClip049();Path folder=Path.of("src/main/resources/assets/chao_garden/models/entity/gameplay");
  for(var path:Files.list(folder).filter(p->p.toString().endsWith(".json")&&!p.toString().contains("_glass")).toList()){
   var mesh=ChaoMeshAsset.parse(JsonParser.parseString(Files.readString(path)).getAsJsonObject());String key=path.getFileName().toString();
   for(boolean adult:new boolean[]{false,true})for(float morph:new float[]{-1,0,1})for(float power:new float[]{-1,0,1}){
    mesh.development(morph,power,adult?1:.14f);var poses=mesh.createPoses();var pm=key.contains("chaos_")?ChaoMeshAsset.parse(JsonParser.parseString(Files.readString(path.resolveSibling(key.replace(".json","_glass.json")))).getAsJsonObject()):mesh;
    var pp=pm==mesh?poses:pm.createPoses();var hands=new ArrayList<Hands>();var skin=handOnly(JsonParser.parseString(Files.readString(key.contains("chaos_")?path.resolveSibling(key.replace(".json","_glass.json")):path)).getAsJsonObject(),-1);if(!key.contains("chaos_"))skin.development(morph,power,adult?1:.14f);hands.add(new Hands(pm.visiblePalm(pp,false),pm.visiblePalm(pp,true),"skin",skin,-1,pm));
    if(morph==0&&power==0&&!key.contains("chaos_")&&!key.contains("omochao"))for(int animal=0;animal<ChaoState.ANIMALS.length;animal++){
     if((ChaoState.animalSlots(animal)&1)==0)continue;var p=folder.getParent().resolve((animal<21?"overlays/":"cwe_parts/")+key);if(!Files.exists(p))continue;
     var replacement=ChaoMeshAsset.parse(JsonParser.parseString(Files.readString(p)).getAsJsonObject());var rp=replacement.createPoses();var r=ChaoToyGrip.animalPalm(replacement,rp,animal,adult,false);var l=ChaoToyGrip.animalPalm(replacement,rp,animal,adult,true);if(r!=null&&l!=null)hands.add(new Hands(r,l,"animal"+animal,handOnly(JsonParser.parseString(Files.readString(p)).getAsJsonObject(),animal),animal,replacement));
    }
    for(var h:hands)for(String kind:kinds)for(int t:new int[]{0,2,4,6,8,10,12,16,24,32,36,48,64,84,100,120,132,140,144,146,148,150,152,154,156,158,160}){
     if(h.right==null||h.left==null)continue;poses.values().forEach(ChaoMeshAsset.Pose::reset);
     if(ActiveToyPlayRules049.doll(kind)){poses.get("body").y=.8f;poses.get("body").rx=-.08f;poses.get("skull").y=.8f;ChaoSittingPose.apply(mesh,poses,adult);}
     BiConsumer<String,PoseStack> anchor=(name,stack)->mesh.transformBone(name,stack,poses);float w=ActiveToyPlayRules049.presence(t);
     ChaoActiveToyAnimation049.pose(poses,anchor,h.right,h.left,kind,t,w,clip,name->bounds(h,mesh,poses,adult,name));samples++;
     String label=kind+" "+key+" adult="+adult+" morph="+morph+","+power+" "+h.label+" t="+t;
     for(var p:poses.values())for(float v:new float[]{p.x,p.y,p.z,p.rx,p.ry,p.rz,p.reach})check(Float.isFinite(v),"finite "+label);
     if(kind.equals(ActiveToyPlayRules049.POGO)){
      ChaoActiveToyAnimation049.fitFeet(poses,anchor,name->bounds(h,mesh,poses,adult,name),w);
      if(w==1)for(String foot:new String[]{"foot_left","foot_right"}){
        var box=bounds(h,mesh,poses,adult,foot);if(box==null||!poses.containsKey(foot))continue;var frame=new PoseStack();anchor.accept(foot,frame);float bottom=Float.NEGATIVE_INFINITY;
        for(int corner=0;corner<8;corner++)bottom=Math.max(bottom,frame.last().pose().transformPosition(new Vector3f(box[(corner&1)==0?0:3],box[(corner&2)==0?1:4],box[(corner&4)==0?2:5]).div(16)).y);
        check(Math.abs(bottom-1.501f)<.015f,"pedal "+bottom+" "+label);
      }
     }
     if(kind.equals(ActiveToyPlayRules049.POGO)&&w!=1)continue;
     if((kind.equals(ActiveToyPlayRules049.CAR)?ChaoActiveToyAnimation049.carPropWeight(w):ChaoActiveToyAnimation049.propWeight(w))<=0)continue;var prop=ChaoActiveToyAnimation049.propMatrix(anchor,kind,t,w,clip);
     for(boolean left:new boolean[]{false,true}){
      if(kind.equals(ActiveToyPlayRules049.CAR)&&w<.80f)continue;
      var arm=poses.get(left?"arm_left":"arm_right");maxReach=Math.max(maxReach,arm.reach);if(kind.equals(ActiveToyPlayRules049.CAR))maxCarReach=Math.max(maxCarReach,arm.reach);check(arm.reach<=(kind.equals(ActiveToyPlayRules049.CAR)?ChaoActiveToyAnimation049.CAR_REACH:ChaoActiveToyAnimation049.MAX_REACH)+.001,"reach "+label);
      var palm=ChaoTrumpetAnimation.reachedPalm(left?h.left:h.right,arm.reach,left);var actual=ChaoTriangleAnimation.palm(anchor,palm,left);var target=left&&kind.equals(ActiveToyPlayRules049.CAR)?ChaoActiveToyAnimation049.carSupportTarget(anchor,name->bounds(h,mesh,poses,adult,name)):ChaoActiveToyAnimation049.gripTarget(new Matrix4f(prop),kind,left);float error=actual.distance(target);maxError=Math.max(maxError,error);check(error<(kind.equals(ActiveToyPlayRules049.POGO)?.035f:.01f),"contact "+error+" "+label);
      {
        var rendered=h.renderMesh.createPoses();rendered.forEach((name,p)->{var from=poses.get(name);if(from!=null)ChaoOverlayLayer.copy(from,p);if(name.contains(left?"right":"left"))p.visible=false;});
        if(h.animal>=0&&!adult&&h.animal<21){var root=rendered.get("animal_0_"+h.animal+(left?"_left":"_right"));if(root!=null)root.sx=root.sy=root.sz=.92f;}
        var sink=new RattleAnimationTest.Sink();h.renderMesh.render(new PoseStack(),sink,0,0,-1,rendered,false);check(!sink.vertices.isEmpty(),"emitted "+label);
        if(!sink.vertices.isEmpty()){
          if(kind.equals(ActiveToyPlayRules049.CAR)){
           float bottom=Float.NEGATIVE_INFINITY;for(var v:sink.vertices)bottom=Math.max(bottom,v[1]);
           check(bottom<=1.505f,"hand-floor "+bottom+" "+left+" "+label);
           if(left&&w==1){maxFloorError=Math.max(maxFloorError,Math.abs(bottom-1.499f));check(Math.abs(bottom-1.499f)<.012f,"support-floor "+bottom+" "+label);}
          }
          float visible=sink.center().distance(actual);maxRendered=Math.max(maxRendered,visible);check(visible<.035f,"visible "+visible+" "+label);emitted++;
          var triangles=new ArrayList<Vector3f[]>();
          for(int i=0;i<sink.vertices.size();i+=4){
            triangles.add(new Vector3f[]{new Vector3f(sink.vertices.get(i)),new Vector3f(sink.vertices.get(i+1)),new Vector3f(sink.vertices.get(i+2))});
            if(new Vector3f(sink.vertices.get(i+2)).distanceSquared(new Vector3f(sink.vertices.get(i+3)))>1e-12)triangles.add(new Vector3f[]{new Vector3f(sink.vertices.get(i)),new Vector3f(sink.vertices.get(i+2)),new Vector3f(sink.vertices.get(i+3))});
          }
          var sh=new PoseStack();anchor.accept("skull",sh);var bd=new PoseStack();anchor.accept("body",bd);
          float hh=PropMeshClearance.separation(triangles,new Matrix4f(sh.last().pose()).invert(),mesh.attachmentBounds("skull",poses));
          float hb=PropMeshClearance.separation(triangles,new Matrix4f(bd.last().pose()).invert(),mesh.attachmentBounds("body",poses));
          headsByKind.merge(kind,hh,Math::min);minHandHead=Math.min(minHandHead,hh);minHandBody=Math.min(minHandBody,hb);
          if(!kind.equals(ActiveToyPlayRules049.POGO))for(String foot:ChaoSittingPose.FEET){var box=bounds(h,mesh,poses,adult,foot);if(box==null||!poses.containsKey(foot))continue;var at=new PoseStack();anchor.accept(foot,at);float gap=PropMeshClearance.separation(triangles,new Matrix4f(at.last().pose()).invert(),box);feetByKind.merge(kind,gap,Math::min);minHandFeet=Math.min(minHandFeet,gap);check(gap>0,"hand-feet "+gap+" "+label);}
          check(hh>0,"hand-head "+hh+" side="+left+" "+label);check(hb>0,"hand-body "+hb+" "+label);
        }
      }
     }
     if(kind.equals(ActiveToyPlayRules049.CAR)){
      float floor=Float.NEGATIVE_INFINITY;for(var triangle:geometry.get(kind))for(var vertex:triangle)floor=Math.max(floor,prop.transformPosition(new Vector3f(vertex)).y);
      maxFloorError=Math.max(maxFloorError,Math.abs(floor-1.501f));check(Math.abs(floor-1.501f)<.00001f,"car-floor "+floor+" "+label);
     }
     var skull=new PoseStack();anchor.accept("skull",skull);float head=PropMeshClearance.separation(geometry.get(kind),new Matrix4f(skull.last().pose()).invert().mul(prop),mesh.attachmentBounds("skull",poses));minHead=Math.min(minHead,head);check(head>.03f,"head "+head+" "+label);
     var body=new PoseStack();anchor.accept("body",body);float torso=PropMeshClearance.separation(geometry.get(kind),new Matrix4f(body.last().pose()).invert().mul(prop),mesh.attachmentBounds("body",poses));minBody=Math.min(minBody,torso);check(torso>.01f,"torso "+torso+" "+label);
     if(!kind.equals(ActiveToyPlayRules049.POGO))for(String foot:ChaoSittingPose.FEET){var box=bounds(h,mesh,poses,adult,foot);if(box==null||!poses.containsKey(foot))continue;var at=new PoseStack();anchor.accept(foot,at);float gap=PropMeshClearance.separation(geometry.get(kind),new Matrix4f(at.last().pose()).invert().mul(prop),box);minFeet=Math.min(minFeet,gap);check(gap>0,"prop-feet "+gap+" "+label);}
    }
   }
  }
  System.out.println("Active toys rig: "+samples+" poses; "+emitted+" emitted hands; maxRendered="+maxRendered+" minHandHead="+minHandHead+" minHandBody="+minHandBody+" maxContact="+maxError+" maxReach="+maxReach+" maxCarReach="+maxCarReach+" minHead="+minHead+" minBody="+minBody+" floorError="+maxFloorError+" propFeet="+minFeet+" handFeet="+minHandFeet+" headsByKind="+headsByKind+" feetByKind="+feetByKind+" failures="+failures+" groups="+groups);
  if(failures>0)throw new AssertionError(String.join("\n",examples));
 }
}
