package com.chaogarden.client;
import com.google.gson.*;
import com.mojang.blaze3d.vertex.PoseStack;
import java.nio.file.*;
import java.util.*;
import org.joml.Vector3f;
import org.joml.Matrix4f;
import com.chaogarden.toys.CastanetsPlayRules;
/** Conservative separation of every animated shell vertex from the deformed head AABB. */
final class HeadClearanceRigTest {
 static List<Vector3f> obj(String name)throws Exception{
  var out=new ArrayList<Vector3f>();for(String line:Files.readAllLines(Path.of("src/main/resources/assets/chao_garden/models/sa2_02515/"+name+".obj")))if(line.startsWith("v ")){var a=line.split("\\s+");out.add(new Vector3f(Float.parseFloat(a[1])-.5f,Float.parseFloat(a[2])-.5f,Float.parseFloat(a[3])-.5f));}return out;
 }
 static void run()throws Exception{
  var base=PropMeshClearance.triangles("chao_castanets_hand_base");var lid=PropMeshClearance.triangles("chao_castanets_hand_lid");var clip=new LionfishCastanetsClip();int checks=0;float worst=Float.POSITIVE_INFINITY;String bad="";int failures=0;
  try(var files=Files.list(Path.of("src/main/resources/assets/chao_garden/models/entity/gameplay"))){for(var path:files.filter(p->p.toString().endsWith(".json")&&!p.toString().contains("_glass")).toList()){
   var mesh=ChaoMeshAsset.parse(JsonParser.parseString(Files.readString(path)).getAsJsonObject());var poses=mesh.createPoses();
   for(float age:new float[]{0,.14f,1})for(float sf:new float[]{-1,0,1})for(float rp:new float[]{-1,0,1}){
    mesh.development(sf,rp,age);poses.values().forEach(ChaoMeshAsset.Pose::reset);var palms=new ArrayList<float[]>();palms.add(mesh.toyPalm(poses,false));
    String key=path.getFileName().toString();if(sf==0&&rp==0&&!key.contains("chaos_")&&!key.contains("omochao"))for(int animal=0;animal<com.chaogarden.core.ChaoState.ANIMALS.length;animal++){
      if((com.chaogarden.core.ChaoState.animalSlots(animal)&1)==0)continue;var overlay=path.getParent().getParent().resolve((animal<21?"overlays/":"cwe_parts/")+key);if(!Files.exists(overlay))continue;
      var replacement=ChaoMeshAsset.parse(JsonParser.parseString(Files.readString(overlay)).getAsJsonObject());var socket=ChaoToyGrip.animalPalm(replacement,replacement.createPoses(),animal,age==1,false);if(socket!=null)palms.add(socket);
    }
    for(var palm:palms){var bounds=mesh.attachmentBounds("skull",poses);if(bounds==null)throw new AssertionError("Missing head bounds "+path);
    java.util.function.BiConsumer<String,PoseStack> anchor=(n,s)->mesh.transformBone(n,s,poses);
    for(int q=1;q<640;q++){
     float t=q*.25f,w=CastanetsPlayRules.presence(t);poses.values().forEach(ChaoMeshAsset.Pose::reset);
     for(boolean left:new boolean[]{false,true})ChaoCastanetsAnimation.applyArm(poses.get(left?"arm_left":"arm_right"),clip,t,w,left);
     poses.get("body").y=.8f;poses.get("body").rx=-.08f;poses.get("skull").y=.8f;poses.get("skull").rx=.025f;
     float[] leftPalm={-palm[0],palm[1],palm[2]};ChaoCastanetsAnimation.alignTap(poses,anchor,palm,leftPalm,w,clip.tap(t));
     var skull=new PoseStack();anchor.accept("skull",skull);var inverse=new Matrix4f(skull.last().pose()).invert();var prop=ChaoCastanetsAnimation.propMatrix(anchor,palm,w);
     for(boolean upper:new boolean[]{false,true}){if(ChaoCastanetsAnimation.propWeight(w)<=0)continue;
      var transform=new Matrix4f(inverse).mul(upper?ChaoCastanetsAnimation.lidMatrix(prop,clip.tap(t)):prop);
      float gap=PropMeshClearance.separation(upper?lid:base,transform,bounds);
      checks++;if(gap<worst){worst=gap;bad=path.getFileName()+" age="+age+" morph="+sf+","+rp+" tick="+t+" lid="+upper+" bounds="+Arrays.toString(bounds);}
      if(gap<.025f)failures++;
     }
    }
   }
  }}}
  String result="Castanet head separation checks="+checks+" failures="+failures+" minimumPixels="+worst+" worst="+bad+"\n";System.out.print(result);Files.writeString(Path.of("build/reports/lionfish/head-clearance-results.txt"),result);if(failures>0)throw new AssertionError(result);
 }
}
