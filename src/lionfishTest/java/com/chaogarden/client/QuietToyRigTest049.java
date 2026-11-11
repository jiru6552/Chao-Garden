package com.chaogarden.client;
import com.chaogarden.core.ChaoState;
import com.chaogarden.toys.*;
import com.google.gson.*;
import com.mojang.blaze3d.vertex.PoseStack;
import java.nio.file.*;
import java.util.*;
import java.util.function.BiConsumer;
import org.joml.Matrix4f;
import org.joml.Vector3f;
/** Actual source parts plus canonical, morph and replacement-hand contacts. */
public final class QuietToyRigTest049 {
 static final Path ROOT=Path.of("src/main/resources/assets/chao_garden");
 static int samples,checks,emitted;static float minHandHead=Float.POSITIVE_INFINITY,minHandBody=Float.POSITIVE_INFINITY,emitError;static final Map<String,Float> handHeads=new TreeMap<>(),handBodies=new TreeMap<>();static final Map<String,String> handWorst=new TreeMap<>();static final Map<String,Integer> groups=new TreeMap<>();static final List<String> failures=new ArrayList<>();static final Map<String,String> worst=new TreeMap<>();static final Map<String,Float> maxError=new TreeMap<>(),maxReach=new TreeMap<>(),minHead=new TreeMap<>(),minBody=new TreeMap<>();
 record Hands(float[] right,float[] left,String label,ChaoMeshAsset renderMesh,int animal){}
 static ChaoMeshAsset handOnly(JsonObject source,int animal){var json=source.deepCopy();for(var e:json.getAsJsonArray("bones")){var b=e.getAsJsonObject();String n=b.get("name").getAsString();var keep=new JsonArray();if(b.has("meshes"))for(var t:b.getAsJsonArray("meshes")){var m=t.getAsJsonObject();String part=m.has("name")?m.get("name").getAsString():"";if(animal<0&&(n.equals("arm_left__cube_1")||n.equals("arm_right__cube_1")||n.equals("chaos_hand_arm_left")||n.equals("chaos_hand_arm_right"))||animal>=0&&n.startsWith("animal_0_"+animal+"_")&&Set.of("gene_hand","paw","hoof","claw").contains(part))keep.add(m);}b.add("meshes",keep);}return ChaoMeshAsset.parse(json);}
 static List<Vector3f[]> faces(String name)throws Exception{var vertices=new ArrayList<Vector3f>();var out=new ArrayList<Vector3f[]>();for(String line:Files.readAllLines(ROOT.resolve("models/quiet02549/hand_"+name+".obj"))){var w=line.split(" ");if(line.startsWith("v "))vertices.add(new Vector3f(Float.parseFloat(w[1])-.5f,Float.parseFloat(w[2])-.5f,Float.parseFloat(w[3])-.5f));else if(line.startsWith("f ")){var a=vertices.get(Integer.parseInt(w[1].split("/")[0])-1);for(int n=3;n<w.length;n++)out.add(new Vector3f[]{a,vertices.get(Integer.parseInt(w[n-1].split("/")[0])-1),vertices.get(Integer.parseInt(w[n].split("/")[0])-1)});}}return out;}
 static void fail(String s){checks++;var fields=s.split(" ");groups.merge(fields[0]+" "+fields[1]+" "+(fields.length>3?fields[3]:""),1,Integer::sum);if(failures.size()<30)failures.add(s);}
 public static void main(String[]args)throws Exception{
  var clip=new LionfishQuietToyClip049();var geometry=new HashMap<String,List<Vector3f[]>>();for(String part:List.of("broom","book_left","book_right","book_page","crayon_box","crayon","paper_0"))geometry.put(part,faces(part));
  for(var path:Files.list(ROOT.resolve("models/entity/gameplay")).filter(p->p.toString().endsWith(".json")&&!p.toString().contains("_glass")).sorted().toList()){
   var json=JsonParser.parseString(Files.readString(path)).getAsJsonObject();var mesh=ChaoMeshAsset.parse(json);String label=path.getFileName().toString();
   for(boolean adult:new boolean[]{false,true})for(float sf:new float[]{-1,0,1})for(float rp:new float[]{-1,0,1}){
    mesh.development(sf,rp,adult?1:.14f);var poses=mesh.createPoses();var hands=new ArrayList<Hands>();
    var handMesh=label.contains("chaos_")?ChaoMeshAsset.parse(JsonParser.parseString(Files.readString(path.resolveSibling(label.replace(".json","_glass.json")))).getAsJsonObject()):mesh;
    var handJson=label.contains("chaos_")?JsonParser.parseString(Files.readString(path.resolveSibling(label.replace(".json","_glass.json")))).getAsJsonObject():json;handMesh.development(sf,rp,adult?1:.14f);var handRender=handOnly(handJson,-1);handRender.development(sf,rp,adult?1:.14f);hands.add(new Hands(handMesh.visiblePalm(handMesh.createPoses(),false),handMesh.visiblePalm(handMesh.createPoses(),true),"skin",handRender,-1));
    if(sf==0&&rp==0&&!label.contains("chaos_")&&!label.contains("omochao"))for(int animal=0;animal<ChaoState.ANIMALS.length;animal++){
     if((ChaoState.animalSlots(animal)&1)==0)continue;var p=path.getParent().getParent().resolve((animal<21?"overlays/":"cwe_parts/")+label);if(!Files.exists(p))continue;
     var replacementJson=JsonParser.parseString(Files.readString(p)).getAsJsonObject();var m=ChaoMeshAsset.parse(replacementJson);var hp=m.createPoses();var r=ChaoToyGrip.animalPalm(m,hp,animal,adult,false);var l=ChaoToyGrip.animalPalm(m,hp,animal,adult,true);if(r!=null&&l!=null)hands.add(new Hands(r,l,"animal"+animal,handOnly(replacementJson,animal),animal));
    }
    for(String kind:List.of(QuietToyPlayRules049.BROOM,QuietToyPlayRules049.BOOK,QuietToyPlayRules049.CRAYONS))for(var h:hands)for(int tick=0;tick<=160;tick+=8){
     if(h.right==null||h.left==null)continue;poses.values().forEach(ChaoMeshAsset.Pose::reset);poses.get("body").y=kind.equals(QuietToyPlayRules049.BROOM)?0:.8f;poses.get("body").rx=kind.equals(QuietToyPlayRules049.BROOM)?0:-.08f;poses.get("skull").y=kind.equals(QuietToyPlayRules049.BROOM)?0:.8f;
     BiConsumer<String,PoseStack> anchor=(bone,stack)->mesh.transformBone(bone,stack,poses);float weight=QuietToyPlayRules049.presence(tick),scale=label.contains("chaos_")?.86f:adult?.8f:.64f;
     ChaoQuietToyAnimation049.pose(poses,anchor,h.right,h.left,kind,weight,clip.sweep(tick),clip.page(tick),clip.stroke(tick),clip.pen(tick),tick,scale);samples++;
     for(var p:poses.values())for(float f:new float[]{p.x,p.y,p.z,p.rx,p.ry,p.rz,p.reach})if(!Float.isFinite(f))fail("nonfinite "+kind+" "+label);
     if(weight!=1)continue;
     var matrix=kind.equals(QuietToyPlayRules049.BROOM)?ChaoQuietToyAnimation049.broomMatrix(weight,clip.sweep(tick),scale):kind.equals(QuietToyPlayRules049.BOOK)?ChaoQuietToyAnimation049.bookMatrix(anchor,weight):ChaoQuietToyAnimation049.paperMatrix(anchor,weight);
     var renderPoses=h.renderMesh.createPoses();renderPoses.forEach((n,p)->{var from=poses.get(n);if(from!=null)ChaoOverlayLayer.copy(from,p);});
     if(h.animal>=0&&!adult&&h.animal<21)for(boolean l:new boolean[]{false,true}){var part=renderPoses.get("animal_0_"+h.animal+(l?"_left":"_right"));part.sx=part.sy=part.sz=.92f;}
     for(boolean left:new boolean[]{false,true}){
      var arm=poses.get(left?"arm_left":"arm_right");var palm=ChaoTrumpetAnimation.reachedPalm(left?h.left:h.right,arm.reach,left);var at=ChaoTriangleAnimation.palm(anchor,palm,left);
      var target=kind.equals(QuietToyPlayRules049.BROOM)?ChaoQuietToyAnimation049.broomTarget(matrix,left):kind.equals(QuietToyPlayRules049.BOOK)?ChaoQuietToyAnimation049.bookTarget(matrix,left):left?ChaoQuietToyAnimation049.paperSupport(matrix):ChaoQuietToyAnimation049.drawingHand(matrix,tick,clip.stroke(tick),clip.pen(tick));
      float e=kind.equals(QuietToyPlayRules049.BROOM)&&left?0:at.distance(target);String key=kind+" "+left;if(e>maxError.getOrDefault(key,0f))worst.put(key,label+" "+h.label+" age="+adult+" morph="+sf+","+rp+" t="+tick);maxError.merge(key,e,Math::max);maxReach.merge(key,arm.reach,Math::max);
      if(e>.025f)fail("contact "+e+" "+key+" "+label+" "+h.label+" age="+adult+" morph="+sf+","+rp+" t="+tick);
      {
       var visibility=new HashMap<String,Boolean>();renderPoses.forEach((n,p)->{visibility.put(n,p.visible);if(n.contains(left?"right":"left"))p.visible=false;});
       var sink=new RattleAnimationTest.Sink();h.renderMesh.render(new PoseStack(),sink,0,0,-1,renderPoses,false);renderPoses.forEach((n,p)->p.visible=visibility.get(n));
       if(sink.vertices.isEmpty())fail("empty emitted hand "+label+" "+h.label);else{
        emitted++;float err=sink.center().distance(at);emitError=Math.max(emitError,err);if(err>.035f)fail("emitted mismatch "+err+" "+kind+" "+label+" "+h.label);
        var triangles=new ArrayList<Vector3f[]>();for(int n=0;n<sink.vertices.size();n+=4){triangles.add(new Vector3f[]{new Vector3f(sink.vertices.get(n)),new Vector3f(sink.vertices.get(n+1)),new Vector3f(sink.vertices.get(n+2))});if(new Vector3f(sink.vertices.get(n+2)).distanceSquared(new Vector3f(sink.vertices.get(n+3)))>1e-12f)triangles.add(new Vector3f[]{new Vector3f(sink.vertices.get(n)),new Vector3f(sink.vertices.get(n+2)),new Vector3f(sink.vertices.get(n+3))});}
        var ba=new PoseStack();anchor.accept("body",ba);var ha=new PoseStack();anchor.accept("skull",ha);
        float hh=PropMeshClearance.separation(triangles,new Matrix4f(ha.last().pose()).invert(),mesh.attachmentBounds("skull",poses));if(hh<handHeads.getOrDefault(key,Float.POSITIVE_INFINITY)){handHeads.put(key,hh);handWorst.put("head "+key,label+" "+h.label+" age="+adult+" morph="+sf+","+rp+" t="+tick);}minHandHead=Math.min(minHandHead,hh);
        float hb=PropMeshClearance.separation(triangles,new Matrix4f(ba.last().pose()).invert(),mesh.attachmentBounds("body",poses));if(hb<handBodies.getOrDefault(key,Float.POSITIVE_INFINITY)){handBodies.put(key,hb);handWorst.put("body "+key,label+" "+h.label+" age="+adult+" morph="+sf+","+rp+" t="+tick);}minHandBody=Math.min(minHandBody,hb);
        if(hh<=0)fail("hand head "+hh+" "+kind+" "+left+" "+label+" "+h.label+" age="+adult+" t="+tick);
        if(hb<=0)fail("hand torso "+hb+" "+kind+" "+left+" "+label+" "+h.label+" age="+adult+" t="+tick);
       }
      }
     }
     if(!h.label.equals("skin"))continue;
     var body=new PoseStack();anchor.accept("body",body);var skull=new PoseStack();anchor.accept("skull",skull);
     List<String> parts=kind.equals(QuietToyPlayRules049.BROOM)?List.of("broom"):kind.equals(QuietToyPlayRules049.BOOK)?List.of("book_left","book_right","book_page"):List.of("paper_0");
     for(String part:parts){var m=new Matrix4f(matrix);if(part.equals("book_page")){if(!QuietToyPlayRules049.turning(tick))continue;m.translate(0,-.012f,0).rotateZ(-(float)Math.PI*clip.page(tick));}
      float head=PropMeshClearance.separation(geometry.get(part),new Matrix4f(skull.last().pose()).invert().mul(m),mesh.attachmentBounds("skull",poses));minHead.merge(kind,head,Math::min);
      float torso=PropMeshClearance.separation(geometry.get(part),new Matrix4f(body.last().pose()).invert().mul(m),mesh.attachmentBounds("body",poses));minBody.merge(kind,torso,Math::min);
      if(head<=0)fail("head "+head+" "+kind+" "+part+" "+label+" age="+adult+" morph="+sf+","+rp+" t="+tick);
      if(torso<=0)fail("torso "+torso+" "+kind+" "+part+" "+label+" age="+adult+" t="+tick);
     }
    }
   }
  }
  System.out.println("Quiet samples="+samples+" maxError="+maxError+" reach="+maxReach+" head="+minHead+" body="+minBody+" emitted="+emitted+" emitError="+emitError+" handHead="+minHandHead+" handBody="+minHandBody+" worst="+worst+" handHeads="+handHeads+" handBodies="+handBodies+" handWorst="+handWorst+" groups="+groups+" failures="+checks);for(var f:failures)System.out.println(f);if(checks>0)throw new AssertionError("Quiet toy rig failures="+checks);
 }
}
