package com.chaogarden.client;
import com.chaogarden.toys.*;
import com.chaogarden.core.ChaoState;
import com.google.gson.*;
import com.mojang.blaze3d.vertex.PoseStack;
import java.nio.file.*;import java.util.*;import java.util.function.BiConsumer;
import org.joml.Matrix4f;import org.joml.Vector3f;
/** Contacts are tested against the actual supplied source surfaces and visible hand centers. */
public final class ActiveToyRigTest049 {
 record Hands(float[] right,float[] left,String label,ChaoMeshAsset renderMesh,int animal){}
 static int failures,samples,emitted;static float maxRendered,minHandHead=Float.POSITIVE_INFINITY,minHandBody=Float.POSITIVE_INFINITY;static float maxError,maxReach,minHead=Float.POSITIVE_INFINITY,minBody=Float.POSITIVE_INFINITY;
 static Map<String,Integer> groups=new TreeMap<>();static List<String> examples=new ArrayList<>();
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
 public static void main(String[] args)throws Exception{
  String[] kinds={ActiveToyPlayRules049.CAR,ActiveToyPlayRules049.SONIC,ActiveToyPlayRules049.POGO};var geometry=new HashMap<String,List<Vector3f[]>>();for(String k:kinds)geometry.put(k,triangles(k));
  var clip=new LionfishActiveToyClip049();Path folder=Path.of("src/main/resources/assets/chao_garden/models/entity/gameplay");
  for(var path:Files.list(folder).filter(p->p.toString().endsWith(".json")&&!p.toString().contains("_glass")).toList()){
   var mesh=ChaoMeshAsset.parse(JsonParser.parseString(Files.readString(path)).getAsJsonObject());String key=path.getFileName().toString();
   for(boolean adult:new boolean[]{false,true})for(float morph:new float[]{-1,0,1}){
    mesh.development(morph,-morph,adult?1:.14f);var poses=mesh.createPoses();var pm=key.contains("chaos_")?ChaoMeshAsset.parse(JsonParser.parseString(Files.readString(path.resolveSibling(key.replace(".json","_glass.json")))).getAsJsonObject()):mesh;
    var pp=pm==mesh?poses:pm.createPoses();var hands=new ArrayList<Hands>();var skin=handOnly(JsonParser.parseString(Files.readString(key.contains("chaos_")?path.resolveSibling(key.replace(".json","_glass.json")):path)).getAsJsonObject(),-1);if(!key.contains("chaos_"))skin.development(morph,-morph,adult?1:.14f);hands.add(new Hands(pm.visiblePalm(pp,false),pm.visiblePalm(pp,true),"skin",skin,-1));
    if(morph==0&&!key.contains("chaos_")&&!key.contains("omochao"))for(int animal=0;animal<ChaoState.ANIMALS.length;animal++){
     if((ChaoState.animalSlots(animal)&1)==0)continue;var p=folder.getParent().resolve((animal<21?"overlays/":"cwe_parts/")+key);if(!Files.exists(p))continue;
     var replacement=ChaoMeshAsset.parse(JsonParser.parseString(Files.readString(p)).getAsJsonObject());var rp=replacement.createPoses();var r=ChaoToyGrip.animalPalm(replacement,rp,animal,adult,false);var l=ChaoToyGrip.animalPalm(replacement,rp,animal,adult,true);if(r!=null&&l!=null)hands.add(new Hands(r,l,"animal"+animal,handOnly(JsonParser.parseString(Files.readString(p)).getAsJsonObject(),animal),animal));
    }
    for(var h:hands)for(String kind:kinds)for(int t:new int[]{0,6,12,24,32,36,48,64,84,100,120,132,144,152,160}){
     if(h.right==null||h.left==null)continue;poses.values().forEach(ChaoMeshAsset.Pose::reset);poses.get("body").y=.8f;poses.get("body").rx=-.08f;poses.get("skull").y=.8f;
     BiConsumer<String,PoseStack> anchor=(name,stack)->mesh.transformBone(name,stack,poses);float w=ActiveToyPlayRules049.presence(t);
     ChaoActiveToyAnimation049.pose(poses,anchor,h.right,h.left,kind,t,w,clip);samples++;
     String label=kind+" "+key+" adult="+adult+" morph="+morph+" "+h.label+" t="+t;
     for(var p:poses.values())for(float v:new float[]{p.x,p.y,p.z,p.rx,p.ry,p.rz,p.reach})check(Float.isFinite(v),"finite "+label);
     if(kind.equals(ActiveToyPlayRules049.POGO)){
      var fp=pm==mesh?poses:pm.createPoses();if(pm!=mesh)fp.forEach((name,p)->{var from=poses.get(name);if(from!=null)ChaoOverlayLayer.copy(from,p);});
      ChaoActiveToyAnimation049.fitFeet(poses,anchor,name->pm.partBounds(name,fp),w);
      if(w==1)for(String foot:new String[]{"foot_left","foot_right"}){
        var box=pm.partBounds(foot,fp);if(box==null||!poses.containsKey(foot))continue;var frame=new PoseStack();anchor.accept(foot,frame);float bottom=Float.NEGATIVE_INFINITY;
        for(int corner=0;corner<8;corner++)bottom=Math.max(bottom,frame.last().pose().transformPosition(new Vector3f(box[(corner&1)==0?0:3],box[(corner&2)==0?1:4],box[(corner&4)==0?2:5]).div(16)).y);
        check(Math.abs(bottom-1.501f)<.015f,"pedal "+bottom+" "+label);
      }
     }
     if(w!=1)continue;var prop=ChaoActiveToyAnimation049.propMatrix(anchor,kind,t,w,clip);
     for(boolean left:new boolean[]{false,true}){
      var arm=poses.get(left?"arm_left":"arm_right");maxReach=Math.max(maxReach,arm.reach);check(arm.reach<=ChaoActiveToyAnimation049.MAX_REACH+.001,"reach "+label);
      var palm=ChaoTrumpetAnimation.reachedPalm(left?h.left:h.right,arm.reach,left);var actual=ChaoTriangleAnimation.palm(anchor,palm,left);var target=ChaoActiveToyAnimation049.gripTarget(new Matrix4f(prop),kind,left);float error=actual.distance(target);maxError=Math.max(maxError,error);check(error<.035f,"contact "+error+" "+label);
      {
        var rendered=h.renderMesh.createPoses();rendered.forEach((name,p)->{var from=poses.get(name);if(from!=null)ChaoOverlayLayer.copy(from,p);if(name.contains(left?"right":"left"))p.visible=false;});
        if(h.animal>=0&&!adult&&h.animal<21){var root=rendered.get("animal_0_"+h.animal+(left?"_left":"_right"));if(root!=null)root.sx=root.sy=root.sz=.92f;}
        var sink=new RattleAnimationTest.Sink();h.renderMesh.render(new PoseStack(),sink,0,0,-1,rendered,false);check(!sink.vertices.isEmpty(),"emitted "+label);
        if(!sink.vertices.isEmpty()){
          float visible=sink.center().distance(actual);maxRendered=Math.max(maxRendered,visible);check(visible<.035f,"visible "+visible+" "+label);emitted++;
          var triangles=new ArrayList<Vector3f[]>();
          for(int i=0;i<sink.vertices.size();i+=4){
            triangles.add(new Vector3f[]{new Vector3f(sink.vertices.get(i)),new Vector3f(sink.vertices.get(i+1)),new Vector3f(sink.vertices.get(i+2))});
            if(new Vector3f(sink.vertices.get(i+2)).distanceSquared(new Vector3f(sink.vertices.get(i+3)))>1e-12)triangles.add(new Vector3f[]{new Vector3f(sink.vertices.get(i)),new Vector3f(sink.vertices.get(i+2)),new Vector3f(sink.vertices.get(i+3))});
          }
          var sh=new PoseStack();anchor.accept("skull",sh);var bd=new PoseStack();anchor.accept("body",bd);
          float hh=PropMeshClearance.separation(triangles,new Matrix4f(sh.last().pose()).invert(),mesh.attachmentBounds("skull",poses));
          float hb=PropMeshClearance.separation(triangles,new Matrix4f(bd.last().pose()).invert(),mesh.attachmentBounds("body",poses));
          minHandHead=Math.min(minHandHead,hh);minHandBody=Math.min(minHandBody,hb);
          check(hh>0,"hand-head "+hh+" "+label);check(hb>0,"hand-body "+hb+" "+label);
        }
      }
     }
     var skull=new PoseStack();anchor.accept("skull",skull);float head=PropMeshClearance.separation(geometry.get(kind),new Matrix4f(skull.last().pose()).invert().mul(prop),mesh.attachmentBounds("skull",poses));minHead=Math.min(minHead,head);check(head>.03f,"head "+head+" "+label);
     var body=new PoseStack();anchor.accept("body",body);float torso=PropMeshClearance.separation(geometry.get(kind),new Matrix4f(body.last().pose()).invert().mul(prop),mesh.attachmentBounds("body",poses));minBody=Math.min(minBody,torso);check(torso>.01f,"torso "+torso+" "+label);
    }
   }
  }
  System.out.println("Active toys rig: "+samples+" poses; "+emitted+" emitted hands; maxRendered="+maxRendered+" minHandHead="+minHandHead+" minHandBody="+minHandBody+" maxContact="+maxError+" maxReach="+maxReach+" minHead="+minHead+" minBody="+minBody+" failures="+failures+" groups="+groups);
  if(failures>0)throw new AssertionError(String.join("\n",examples));
 }
}
