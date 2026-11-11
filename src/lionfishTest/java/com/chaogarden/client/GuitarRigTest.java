package com.chaogarden.client;

import com.chaogarden.core.ChaoState;
import com.chaogarden.toys.GuitarPlayRules;
import com.google.gson.*;
import com.mojang.blaze3d.vertex.PoseStack;
import java.nio.file.*;
import java.util.*;
import java.util.function.BiConsumer;
import org.joml.Matrix4f;
import org.joml.Vector3f;

/** Source-mesh contact, emitted palms and head/floor clearance, independent of GL. */
public final class GuitarRigTest {
  private static final Path MODELS=Path.of("src/main/resources/assets/chao_garden/models/entity/gameplay");
  private static final Vector3f STRING_START=new Vector3f(.1603256f,.1487173f,.4255777f);
  private static final Vector3f STRING_END=new Vector3f(.75974105f,.31281055f,.62002745f);
  private static final Vector3f FRONT=new Vector3f(.0257916f,.7233867f,-.6899612f);
  private static final Vector3f LONG=new Vector3f(STRING_END).sub(STRING_START).normalize();
  private static final Vector3f ACROSS=new Vector3f(FRONT).cross(LONG).normalize();
  private static int samples,renderedSamples;private static float contact,renderError,minHead=Float.POSITIVE_INFINITY,minFloor=Float.POSITIVE_INFINITY,maxReach;
  private static int failures;private static final Map<String,Integer> failureGroups=new TreeMap<>();
  private static final List<String> examples=new ArrayList<>();
  private static void check(boolean ok,String why){
    if(!ok){failures++;failureGroups.merge(why.split(" ")[0],1,Integer::sum);if(examples.size()<20)examples.add(why);}
  }
  record Hands(float[] right,float[] left,String label,ChaoMeshAsset renderMesh,int animal){}
  private static List<Vector3f[]> sourceFaces()throws Exception{
    var vertices=new ArrayList<Vector3f>();var faces=new ArrayList<Vector3f[]>();
    for(String line:Files.readAllLines(Path.of("src/main/resources/assets/chao_garden/models/cwe02516/chao_guitar.obj"))){
      var words=line.split("\\s+");
      if(line.startsWith("v "))vertices.add(new Vector3f(Float.parseFloat(words[1])-.5f,Float.parseFloat(words[2])-.5f,Float.parseFloat(words[3])-.5f));
      if(line.startsWith("f "))for(int i=2;i<words.length-1;i++)faces.add(new Vector3f[]{vertices.get(Integer.parseInt(words[1].split("/")[0])-1),vertices.get(Integer.parseInt(words[i].split("/")[0])-1),vertices.get(Integer.parseInt(words[i+1].split("/")[0])-1)});
    }
    check(!faces.isEmpty(),"actual source guitar triangles loaded");return faces;
  }
  private static ChaoMeshAsset handOnly(JsonObject source,int animal){
    var json=source.deepCopy();
    for(var element:json.getAsJsonArray("bones")){
      var bone=element.getAsJsonObject();String name=bone.get("name").getAsString();var keep=new JsonArray();
      if(bone.has("meshes"))for(var m:bone.getAsJsonArray("meshes")){
        var mesh=m.getAsJsonObject();String part=mesh.has("name")?mesh.get("name").getAsString():"";
        if(animal<0&&(name.equals("arm_left__cube_1")||name.equals("arm_right__cube_1")||name.equals("chaos_hand_arm_left")||name.equals("chaos_hand_arm_right"))
            ||animal>=0&&name.startsWith("animal_0_"+animal+"_")&&Set.of("gene_hand","paw","hoof","claw").contains(part))keep.add(mesh);
      }
      bone.add("meshes",keep);
    }
    return ChaoMeshAsset.parse(json);
  }
  private static void sourceContact(Matrix4f prop,float strum,float fret,String label){
    var inverse=new Matrix4f(prop).invert();
    var strike=inverse.transformPosition(ChaoGuitarAnimation.strumTarget(new Matrix4f(prop),strum)).add(.5f,.5f,.5f);
    var neck=inverse.transformPosition(ChaoGuitarAnimation.neckGrip(new Matrix4f(prop),fret)).add(.5f,.5f,.5f);
    var origin=new Vector3f(.3291302f,.19492848f,.48033774f);
    var displacement=new Vector3f(strike).sub(origin);
    check(Math.abs(displacement.dot(LONG))<.006f,"strum must stay over body strings "+label);
    check(Math.abs(displacement.dot(ACROSS))<=.092f,"strum stays on soundboard "+label);
    check(displacement.dot(FRONT)>=-.002f&&displacement.dot(FRONT)<=.06f,"strum follows outward front face "+label);
    float along=(neck.x-STRING_START.x)/(STRING_END.x-STRING_START.x);
    var string=new Vector3f(STRING_START).lerp(STRING_END,along);var padding=new Vector3f(neck).sub(string);
    check(neck.x>=.53f&&neck.x<=.68f,"left hand stays on actual neck "+label);
    check(Math.abs(padding.dot(ACROSS))<.025f&&padding.dot(FRONT)>=-.01f&&padding.dot(FRONT)<.08f,"left hand contacts neck front "+label);
  }
  public static void main(String[] args)throws Exception{
    var faces=sourceFaces();var clip=new LionfishGuitarClip();
    for(var path:Files.list(MODELS).filter(p->p.toString().endsWith(".json")&&!p.toString().contains("_glass")).sorted().toList()){
      var json=JsonParser.parseString(Files.readString(path)).getAsJsonObject();var mesh=ChaoMeshAsset.parse(json);String key=path.getFileName().toString();
      for(boolean adult:new boolean[]{false,true})for(float sf:new float[]{-1,0,1})for(float rp:new float[]{-1,0,1}){
        mesh.development(sf,rp,adult?1:.14f);var poses=mesh.createPoses();var hands=new ArrayList<Hands>();
        var palmJson=key.contains("chaos_")?JsonParser.parseString(Files.readString(path.resolveSibling(key.replace(".json","_glass.json")))).getAsJsonObject():json;
        var palmMesh=ChaoMeshAsset.parse(palmJson);palmMesh.development(sf,rp,adult?1:.14f);var palmPoses=palmMesh.createPoses();
        var skin=handOnly(palmJson,-1);skin.development(sf,rp,adult?1:.14f);
        hands.add(new Hands(palmMesh.visiblePalm(palmPoses,false),palmMesh.visiblePalm(palmPoses,true),"skin",skin,-1));
        if(sf==0&&rp==0&&!key.contains("chaos_")&&!key.contains("omochao"))for(int animal=0;animal<ChaoState.ANIMALS.length;animal++){
          if((ChaoState.animalSlots(animal)&1)==0)continue;
          var replacement=path.getParent().getParent().resolve((animal<21?"overlays/":"cwe_parts/")+key);if(!Files.exists(replacement))continue;
          var original=JsonParser.parseString(Files.readString(replacement)).getAsJsonObject();var asset=ChaoMeshAsset.parse(original);var ap=asset.createPoses();
          var right=ChaoToyGrip.animalPalm(asset,ap,animal,adult,false);var left=ChaoToyGrip.animalPalm(asset,ap,animal,adult,true);
          if(right!=null&&left!=null)hands.add(new Hands(right,left,"animal"+animal,handOnly(original,animal),animal));
        }
        for(var hand:hands){
          if(hand.right==null||hand.left==null)continue;
          for(int q=0;q<=320;q++){
            float t=q*.5f,w=GuitarPlayRules.presence(t);poses.values().forEach(ChaoMeshAsset.Pose::reset);
            poses.get("body").y=.8f;poses.get("body").rx=-.08f;poses.get("skull").y=.8f;
            BiConsumer<String,PoseStack> anchor=(name,stack)->mesh.transformBone(name,stack,poses);
            float strum=clip.strum(t),fret=clip.fret(t);
            ChaoGuitarAnimation.pose(poses,anchor,hand.right,hand.left,w,strum,fret);samples++;
            String label=key+" adult="+adult+" morph="+sf+","+rp+" "+hand.label+" t="+t;
            var prop=ChaoGuitarAnimation.propMatrix(anchor,w);
            for(var pose:poses.values())for(float value:new float[]{pose.x,pose.y,pose.z,pose.rx,pose.ry,pose.rz,pose.reach})check(Float.isFinite(value),"finite pose "+label);
            if(w==1){
              sourceContact(prop,strum,fret,label);
              var body=new PoseStack();anchor.accept("body",body);var local=new Matrix4f(body.last().pose()).invert().mul(prop);
              check(local.transformDirection(new Vector3f(LONG)).normalize().x>.85f,"neck lies to Chao left "+label);
              check(local.transformDirection(new Vector3f(FRONT)).normalize().z<-.85f,"strings face outward "+label);
              var skull=new PoseStack();anchor.accept("skull",skull);
              float head=PropMeshClearance.separation(faces,new Matrix4f(skull.last().pose()).invert().mul(prop),mesh.attachmentBounds("skull",poses));minHead=Math.min(minHead,head);check(head>.005f,"guitar clear of skull "+head+" "+label);
              for(var face:faces)for(var vertex:face){float floor=24-prop.transformPosition(new Vector3f(vertex)).y*16;minFloor=Math.min(minFloor,floor);check(floor>.01f,"guitar clear of floor "+label);}
              var renderedPoses=hand.renderMesh.createPoses();renderedPoses.forEach((name,p)->{var from=poses.get(name);if(from!=null)ChaoOverlayLayer.copy(from,p);});
              if(hand.animal>=0&&!adult&&hand.animal<21)for(boolean left:new boolean[]{false,true}){
                var root=renderedPoses.get("animal_0_"+hand.animal+(left?"_left":"_right"));root.sx=root.sy=root.sz=.92f;
              }
              for(boolean left:new boolean[]{false,true}){
                var arm=poses.get(left?"arm_left":"arm_right");maxReach=Math.max(maxReach,arm.reach);
                check(new Vector3f(arm.x,arm.y,arm.z).length()<.81f,"shoulder shift bounded "+label);
                var at=new PoseStack();anchor.accept(left?"arm_left":"arm_right",at);
                var palm=ChaoTrumpetAnimation.reachedPalm(left?hand.left:hand.right,arm.reach,left);
                var actual=at.last().pose().transformPosition(new Vector3f(palm).div(16));
                var target=left?ChaoGuitarAnimation.neckGrip(new Matrix4f(prop),fret):ChaoGuitarAnimation.strumTarget(new Matrix4f(prop),strum);
                float error=actual.distance(target);contact=Math.max(contact,error);check(error<.025f,"hand contact "+error+" "+label);
                var saved=new HashMap<String,Boolean>();renderedPoses.forEach((name,p)->{saved.put(name,p.visible);if(name.contains(left?"right":"left"))p.visible=false;});
                var sink=new RattleAnimationTest.Sink();hand.renderMesh.render(new PoseStack(),sink,0,0,-1,renderedPoses,false);renderedPoses.forEach((name,p)->p.visible=saved.get(name));
                check(!sink.vertices.isEmpty(),"actual visible hand emitted "+label);
                float vertexError=sink.center().distance(actual);renderError=Math.max(renderError,vertexError);
                check(vertexError<.035f,"rendered hand agrees with contact "+vertexError+" "+label);renderedSamples++;
              }
            }
          }
        }
      }
    }
    check(samples>0&&renderedSamples>0,"rig and emitted-hand cases executed");
    System.out.println("Guitar failure groups: "+failureGroups);
    System.out.println("Guitar mesh fit: "+samples+" pose samples; "+renderedSamples+" emitted-hand samples; max contact="+contact+" blocks; rendered center error="+renderError+"; min head="+minHead+" pixels; min floor="+minFloor+" pixels; max reach="+maxReach+" pixels");
    if(failures>0)throw new AssertionError(failures+" guitar rig failures: "+String.join("\n",examples));
  }
}
