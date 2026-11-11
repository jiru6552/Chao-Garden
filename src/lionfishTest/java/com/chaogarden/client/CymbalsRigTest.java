package com.chaogarden.client;
import com.google.gson.*;
import com.mojang.blaze3d.vertex.PoseStack;
import java.nio.file.*;
import java.util.*;
import org.joml.Vector3f;
import org.joml.Matrix4f;
import com.chaogarden.core.ChaoState;
import com.chaogarden.toys.CymbalsPlayRules;
/** Real Lionfish motion, both actual palms, paired rims and full prop/head bounds. */
final class CymbalsRigTest {
 record Hands(float[] right,float[] left,String label){}
 static int checks,cases;static float maxGrip,maxContact,maxShift,minHead=Float.POSITIVE_INFINITY;
 static void check(boolean ok,String why){checks++;if(!ok)throw new AssertionError(why);}
 static void run()throws Exception{
  var clip=new LionfishCymbalsClip();var vertices=PropMeshClearance.triangles("chao_cymbal_hand");int cues=0;
  for(boolean left:new boolean[]{false,true})for(float t:new float[]{-1,0,160,170,Float.NaN})check(clip.sample(t,left).length()<1e-6,"cymbal rest/invalid boundary");
  for(int t=0;t<=160;t++)if(CymbalsPlayRules.soundDue(t)){cues++;check(Math.abs(clip.contact(t)-1)<1e-5,"sound at paired clash");check(CymbalsPlayRules.happy(t),"happy at each clash");}
  check(cues==4,"four deliberate clashes");
  try(var files=Files.list(Path.of("src/main/resources/assets/chao_garden/models/entity/gameplay"))){for(var path:files.filter(p->p.toString().endsWith(".json")&&!p.toString().contains("_glass")).toList()){
   var mesh=ChaoMeshAsset.parse(JsonParser.parseString(Files.readString(path)).getAsJsonObject());var poses=mesh.createPoses();
   for(float age:new float[]{0,.14f,1})for(float sf:new float[]{-1,0,1})for(float rp:new float[]{-1,0,1}){
    mesh.development(sf,rp,age);poses.values().forEach(ChaoMeshAsset.Pose::reset);var hands=new ArrayList<Hands>();hands.add(new Hands(mesh.toyPalm(poses,false),mesh.toyPalm(poses,true),"skin"));
    String key=path.getFileName().toString();if(sf==0&&rp==0&&!key.contains("chaos_")&&!key.contains("omochao"))for(int animal=0;animal<ChaoState.ANIMALS.length;animal++){
     if((ChaoState.animalSlots(animal)&1)==0)continue;var overlay=path.getParent().getParent().resolve((animal<21?"overlays/":"cwe_parts/")+key);if(!Files.exists(overlay))continue;
     var repl=ChaoMeshAsset.parse(JsonParser.parseString(Files.readString(overlay)).getAsJsonObject());var ps=repl.createPoses();var right=ChaoToyGrip.animalPalm(repl,ps,animal,age==1,false);var left=ChaoToyGrip.animalPalm(repl,ps,animal,age==1,true);if(right!=null&&left!=null)hands.add(new Hands(right,left,"animal"+animal));
    }
    for(var hand:hands){cases++;String label=key+" age="+age+" morph="+sf+","+rp+" "+hand.label;var bounds=mesh.attachmentBounds("skull",poses);check(bounds!=null,"head exists "+label);
     java.util.function.BiConsumer<String,PoseStack> anchor=(n,s)->mesh.transformBone(n,s,poses);
     for(int q=0;q<=640;q++){
      float t=q*.25f,w=CymbalsPlayRules.presence(t);poses.values().forEach(ChaoMeshAsset.Pose::reset);
      for(boolean left:new boolean[]{false,true})ChaoCymbalsAnimation.applyArm(poses.get(left?"arm_left":"arm_right"),clip,t,w,left);
      poses.get("body").y=.8f;poses.get("body").rx=-.08f;poses.get("skull").y=.8f;poses.get("skull").rx=.025f;
      maxShift=Math.max(maxShift,ChaoCymbalsAnimation.alignPair(poses,anchor,hand.right,hand.left,w,clip.contact(t)));
      var skull=new PoseStack();anchor.accept("skull",skull);var inv=new Matrix4f(skull.last().pose()).invert();var rims=new ArrayList<Vector3f>();
      for(boolean left:new boolean[]{false,true}){
       var socket=left?hand.left:hand.right;var prop=ChaoCymbalsAnimation.propMatrix(anchor,socket,w,left);var expected=ChaoCymbalsAnimation.palm(anchor,socket,left);
       float grip=prop.transformPosition(ChaoCymbalsAnimation.grip(left).sub(.5f,.5f,.5f)).distance(expected);maxGrip=Math.max(maxGrip,grip);check(grip<1e-6,"actual paired grip "+label);
       var arm=poses.get(left?"arm_left":"arm_right");check(Float.isFinite(arm.rx+arm.ry+arm.rz+arm.x+arm.y+arm.z),"finite arm "+label);
       rims.add(prop.transformPosition(new Vector3f(ChaoCymbalsAnimation.RIM).sub(.5f,.5f,.5f)));
       if(ChaoCymbalsAnimation.propWeight(w)>0){var transform=new Matrix4f(inv).mul(prop);
        float gap=PropMeshClearance.separation(vertices,transform,bounds);minHead=Math.min(minHead,gap);
        check(gap>=.025f,"cymbal head separation "+label+" t="+t+" left="+left+" gapPixels="+gap);
       }
      }
      if(q%4==0&&CymbalsPlayRules.soundDue((int)t)){float gap=rims.get(0).distance(rims.get(1));maxContact=Math.max(maxContact,gap);check(gap<.015f,"paired rims meet "+label+" t="+t+" gap="+gap+" shift="+maxShift);}
     }
    }
   }
  }}
  String result="PASS: "+checks+" cymbal checks; "+cases+" rig/age/morph/replacement cases, 641 subticks; maxGrip="+maxGrip+" maxRimGap="+maxContact+" minHeadPixels="+minHead+" maxRequestedShoulderShiftPixels="+maxShift+"; visual and listening review separate.\n";
  Files.writeString(Path.of("build/reports/lionfish/cymbals-results.txt"),result);System.out.print(result);
 }
}
