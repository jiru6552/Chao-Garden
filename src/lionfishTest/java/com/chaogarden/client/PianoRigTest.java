package com.chaogarden.client;
import com.google.gson.*;
import com.mojang.blaze3d.vertex.PoseStack;
import com.chaogarden.core.ChaoState;
import com.chaogarden.toys.*;
import java.nio.file.*;
import java.util.*;
import org.joml.Vector3f;
public final class PianoRigTest {
 record Hands(float[] right,float[] left,String label){}
 public static void main(String[] args)throws Exception{
  var clip=new LionfishPianoClip();int checks=0;float error=0,maxReach=0;
  try(var files=Files.list(Path.of("src/main/resources/assets/chao_garden/models/entity/gameplay"))){for(var path:files.filter(p->p.toString().endsWith(".json")&&!p.toString().contains("_glass")).toList()){
   var mesh=ChaoMeshAsset.parse(JsonParser.parseString(Files.readString(path)).getAsJsonObject());var poses=mesh.createPoses();String key=path.getFileName().toString();
   for(boolean adult:new boolean[]{false,true})for(float morph:new float[]{-1,0,1}){
    mesh.development(morph,-morph,adult?1:.14f);poses.values().forEach(ChaoMeshAsset.Pose::reset);
    var handMesh=key.contains("chaos_")?ChaoMeshAsset.parse(JsonParser.parseString(Files.readString(path.resolveSibling(key.replace(".json","_glass.json")))).getAsJsonObject()):mesh;
    var hp=handMesh==mesh?poses:handMesh.createPoses();var hands=new ArrayList<Hands>();hands.add(new Hands(handMesh.visiblePalm(hp,false),handMesh.visiblePalm(hp,true),"skin"));
    if(morph==0&&!key.contains("chaos_")&&!key.contains("omochao"))for(int animal=0;animal<ChaoState.ANIMALS.length;animal++){
     if((ChaoState.animalSlots(animal)&1)==0)continue;var p=path.getParent().getParent().resolve((animal<21?"overlays/":"cwe_parts/")+key);if(!Files.exists(p))continue;
     var repl=ChaoMeshAsset.parse(JsonParser.parseString(Files.readString(p)).getAsJsonObject());var rp=repl.createPoses();var right=ChaoToyGrip.animalPalm(repl,rp,animal,adult,false);var left=ChaoToyGrip.animalPalm(repl,rp,animal,adult,true);if(right!=null&&left!=null)hands.add(new Hands(right,left,"animal"+animal));
    }
    float scale=key.contains("chaos_")?.86f:adult?.80f:.64f;
    float distance=(float)ToyInteractionRules.keyboardDistance(PianoPlayRules.KIND,adult?.55:.44);
    for(var h:hands)for(int t:new int[]{0,4,12,24,28,32,64,96,128,144,156,160}){
     if(h.right==null||h.left==null)continue;
     poses.values().forEach(ChaoMeshAsset.Pose::reset);poses.get("body").y=.8f;poses.get("body").rx=-.08f;poses.get("skull").y=.8f;
     java.util.function.BiConsumer<String,PoseStack> anchor=(name,stack)->mesh.transformBone(name,stack,poses);
     float w=PianoPlayRules.presence(t);ChaoPianoAnimation.fit(poses,anchor,h.right,h.left,clip,t,w,scale,distance,0);
     for(boolean left:new boolean[]{false,true}){
      var arm=poses.get(left?"arm_left":"arm_right");var palm=ChaoTrumpetAnimation.reachedPalm(left?h.left:h.right,arm.reach,left);var at=ChaoTriangleAnimation.palm(anchor,palm,left);
      if(!at.isFinite()||Math.abs(arm.y)>.301)throw new AssertionError("bad joint "+key+" "+h.label);
      if(w==1){
       float worldX=at.x*scale,worldY=(1.501f-at.y)*scale;
       if((left?worldX:-worldX)<.05||Math.abs(worldX)>.42)throw new AssertionError("crossed/off keyboard "+key+" "+h.label+" "+at);
       float expected=.2325f+.052f*(1-clip.tap(t,left));error=Math.max(error,Math.abs(worldY-expected));
       if(Math.abs(worldY-expected)>.035)throw new AssertionError("key clearance "+key+" "+h.label+" "+worldY+" expected "+expected);
      }
      maxReach=Math.max(maxReach,arm.reach);checks++;
     }
    }
   }
  }}
  System.out.println("Piano mesh fit: "+checks+" hand samples; no crossed hands; max key-height error="+error+" blocks; max reach="+maxReach+" pixels");
 }
}
