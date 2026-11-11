package com.chaogarden.client;

import com.chaogarden.core.ChaoState;
import com.chaogarden.toys.AccordionPlayRules;
import com.google.gson.*;
import com.mojang.blaze3d.vertex.PoseStack;
import java.nio.file.*;
import java.util.*;
import java.util.function.BiConsumer;
import org.joml.Matrix4f;
import org.joml.Vector3f;

/** Real source-part surfaces, native/replacement palms, and head/torso/floor clearance. */
public final class AccordionRigTest {
  private static final Path MODELS=Path.of("src/main/resources/assets/chao_garden/models/entity/gameplay");
  private static final String[] PARTS={"keys","bass","bellows"};
  private static int failures,samples,renderedSamples;private static float contact,renderError,maxReach,maxRightReach,maxLeftReach,minRightHead=Float.POSITIVE_INFINITY,minLeftHead=Float.POSITIVE_INFINITY,minHandHead=Float.POSITIVE_INFINITY,minHandBody=Float.POSITIVE_INFINITY,minHead=Float.POSITIVE_INFINITY,minBody=Float.POSITIVE_INFINITY,minFloor=Float.POSITIVE_INFINITY;
  private static final Map<String,Integer> groups=new TreeMap<>();private static final List<String> examples=new ArrayList<>();
  private static void check(boolean ok,String why){if(!ok){failures++;groups.merge(why.split(" ")[0],1,Integer::sum);if(examples.size()<18)examples.add(why);}}
  record Hands(float[] right,float[] left,String label,ChaoMeshAsset renderMesh,int animal){}
  private static List<Vector3f[]> faces(String part)throws Exception{
    var vertices=new ArrayList<Vector3f>();var out=new ArrayList<Vector3f[]>();
    for(String line:Files.readAllLines(Path.of("src/main/resources/assets/chao_garden/models/accordion02548/"+part+".obj"))){
      var words=line.split("\\s+");
      if(line.startsWith("v "))vertices.add(new Vector3f(Float.parseFloat(words[1])-.5f,Float.parseFloat(words[2])-.5f,Float.parseFloat(words[3])-.5f));
      else if(line.startsWith("f "))out.add(new Vector3f[]{vertices.get(Integer.parseInt(words[1].split("/")[0])-1),vertices.get(Integer.parseInt(words[2].split("/")[0])-1),vertices.get(Integer.parseInt(words[3].split("/")[0])-1)});
    }return out;
  }
  private static boolean contains(Vector3f[] face,Vector3f point){
    var u=new Vector3f(face[1]).sub(face[0]);var v=new Vector3f(face[2]).sub(face[0]);var p=new Vector3f(point).sub(face[0]);
    var normal=new Vector3f(u).cross(v).normalize();if(Math.abs(normal.dot(p))>.000001f)return false;
    float uu=u.dot(u),vv=v.dot(v),uv=u.dot(v),pu=p.dot(u),pv=p.dot(v),denominator=uu*vv-uv*uv;
    float a=(vv*pu-uv*pv)/denominator,b=(uu*pv-uv*pu)/denominator;
    return a>=-.00001f&&b>=-.00001f&&a+b<=1.00001f;
  }
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
    var geometry=new HashMap<String,List<Vector3f[]>>();for(String part:PARTS)geometry.put(part,faces(part));
    check(geometry.values().stream().mapToInt(List::size).sum()==619,"all actual source triangles retained");
    boolean actualBassSide=false;
    for(var face:geometry.get("bass")){
      var normal=new Vector3f(face[1]).sub(face[0]).cross(new Vector3f(face[2]).sub(face[0])).normalize();
      if(contains(face,ChaoAccordionAnimation.BASS)&&normal.x>.9f&&normal.z<-.4f){
        actualBassSide=true;check(normal.distance(ChaoAccordionAnimation.BASS_NORMAL)<.00001f,"bass grip normal follows actual outer surface");
      }
    }
    check(actualBassSide,"bass grip is inside an actual authored side triangle");
    var clip=new LionfishAccordionClip();
    for(int quarter=0;quarter<=640;quarter++){
      var keyPoint=new Vector3f(ChaoAccordionAnimation.KEY).add(0,.53f-clip.keyHeight(quarter*.25f),0);boolean onKeys=false;
      for(var face:geometry.get("keys"))if(contains(face,keyPoint)){
        var normal=new Vector3f(face[1]).sub(face[0]).cross(new Vector3f(face[2]).sub(face[0])).normalize();if(normal.z<-.999f)onKeys=true;
      }
      check(onKeys,"whole played register remains on real source key surface");
    }
    for(var path:Files.list(MODELS).filter(p->p.toString().endsWith(".json")&&!p.toString().contains("_glass")).sorted().toList()){
      var json=JsonParser.parseString(Files.readString(path)).getAsJsonObject();var mesh=ChaoMeshAsset.parse(json);String key=path.getFileName().toString();
      for(boolean adult:new boolean[]{false,true})for(float sf:new float[]{-1,0,1})for(float rp:new float[]{-1,0,1}){
        mesh.development(sf,rp,adult?1:.14f);var poses=mesh.createPoses();var hands=new ArrayList<Hands>();
        var handJson=key.contains("chaos_")?JsonParser.parseString(Files.readString(path.resolveSibling(key.replace(".json","_glass.json")))).getAsJsonObject():json;
        var palmMesh=ChaoMeshAsset.parse(handJson);palmMesh.development(sf,rp,adult?1:.14f);var palmPoses=palmMesh.createPoses();
        var skin=handOnly(handJson,-1);skin.development(sf,rp,adult?1:.14f);
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
          for(int tick=0;tick<=160;tick+=4){
            float weight=AccordionPlayRules.presence(tick),bellows=clip.bellows(tick);poses.values().forEach(ChaoMeshAsset.Pose::reset);
            poses.get("body").y=.8f;poses.get("body").rx=-.08f;poses.get("skull").y=.8f;
            BiConsumer<String,PoseStack> anchor=(name,stack)->mesh.transformBone(name,stack,poses);
            ChaoAccordionAnimation.pose(poses,anchor,hand.right,hand.left,weight,bellows,clip.keys(tick),clip.bass(tick),clip.keyHeight(tick));samples++;
            String label=key+" adult="+adult+" morph="+sf+","+rp+" "+hand.label+" t="+tick;
            for(var pose:poses.values())for(float value:new float[]{pose.x,pose.y,pose.z,pose.rx,pose.ry,pose.rz,pose.reach})check(Float.isFinite(value),"finite pose "+label);
            if(weight!=1)continue;
            var rendered=hand.renderMesh.createPoses();rendered.forEach((name,p)->{var from=poses.get(name);if(from!=null)ChaoOverlayLayer.copy(from,p);});
            if(hand.animal>=0&&!adult&&hand.animal<21)for(boolean left:new boolean[]{false,true}){var root=rendered.get("animal_0_"+hand.animal+(left?"_left":"_right"));root.sx=root.sy=root.sz=.92f;}
            for(boolean left:new boolean[]{false,true}){
              var arm=poses.get(left?"arm_left":"arm_right");maxReach=Math.max(maxReach,arm.reach);
              if(left)maxLeftReach=Math.max(maxLeftReach,arm.reach);else maxRightReach=Math.max(maxRightReach,arm.reach);
              check(arm.reach<2.8f,"compact arm reach "+arm.reach+" "+label);
              check(new Vector3f(arm.x,arm.y,arm.z).length()<.61f,"bounded shoulder compensation "+label);
              var at=new PoseStack();anchor.accept(left?"arm_left":"arm_right",at);
              var palm=ChaoTrumpetAnimation.reachedPalm(left?hand.left:hand.right,arm.reach,left);
              var actual=at.last().pose().transformPosition(new Vector3f(palm).div(16));
              var target=left?ChaoAccordionAnimation.bassTarget(ChaoAccordionAnimation.propMatrix(anchor,weight,bellows,"bass"),clip.bass(tick)):
                  ChaoAccordionAnimation.keyTarget(ChaoAccordionAnimation.propMatrix(anchor,weight,bellows,"keys"),clip.keyHeight(tick),clip.keys(tick));
              float error=actual.distance(target);contact=Math.max(contact,error);check(error<.025f,"hand contacts real end surface "+error+" "+label);
              {
                var saved=new HashMap<String,Boolean>();rendered.forEach((name,p)->{saved.put(name,p.visible);if(name.contains(left?"right":"left"))p.visible=false;});
                var sink=new RattleAnimationTest.Sink();hand.renderMesh.render(new PoseStack(),sink,0,0,-1,rendered,false);rendered.forEach((name,p)->p.visible=saved.get(name));
                check(!sink.vertices.isEmpty(),"actual hand mesh emitted "+label);float emitted=sink.center().distance(actual);renderError=Math.max(renderError,emitted);
                check(emitted<.035f,"visible hand agrees with fitted palm "+emitted+" "+label);renderedSamples++;
                var triangles=new ArrayList<Vector3f[]>();
                for(int index=0;index<sink.vertices.size();index+=4){
                  triangles.add(new Vector3f[]{new Vector3f(sink.vertices.get(index)),new Vector3f(sink.vertices.get(index+1)),new Vector3f(sink.vertices.get(index+2))});
                  if(new Vector3f(sink.vertices.get(index+2)).distanceSquared(new Vector3f(sink.vertices.get(index+3)))>1e-12f)
                    triangles.add(new Vector3f[]{new Vector3f(sink.vertices.get(index)),new Vector3f(sink.vertices.get(index+2)),new Vector3f(sink.vertices.get(index+3))});
                }
                var bodyAnchor=new PoseStack();anchor.accept("body",bodyAnchor);var skullAnchor=new PoseStack();anchor.accept("skull",skullAnchor);
                float handHead=PropMeshClearance.separation(triangles,new Matrix4f(skullAnchor.last().pose()).invert(),mesh.attachmentBounds("skull",poses));minHandHead=Math.min(minHandHead,handHead);
                if(left)minLeftHead=Math.min(minLeftHead,handHead);else minRightHead=Math.min(minRightHead,handHead);
                float handBody=PropMeshClearance.separation(triangles,new Matrix4f(bodyAnchor.last().pose()).invert(),mesh.attachmentBounds("body",poses));minHandBody=Math.min(minHandBody,handBody);
                check(handHead>0,"actual hand clears skull "+handHead+" side="+(left?"left":"right")+" "+label);check(handBody>0,"actual hand clears torso "+handBody+" side="+(left?"left":"right")+" "+label);
              }
            }
            if(tick%16==0||tick==24||tick==136){
              var body=new PoseStack();anchor.accept("body",body);var skull=new PoseStack();anchor.accept("skull",skull);
              for(String part:PARTS){
                var matrix=ChaoAccordionAnimation.propMatrix(anchor,weight,bellows,part);
                var local=new Matrix4f(body.last().pose()).invert().mul(matrix);
                check(local.transformDirection(new Vector3f(0,0,-1)).normalize().z<-.99f,"actual fronts face outward "+label);
                if(!part.equals("bellows"))check(Math.abs(local.getScale(new Vector3f()).x-ChaoAccordionAnimation.SCALE)<.0001f,"rigid ends keep authored shape "+label);
                float torso=PropMeshClearance.separation(geometry.get(part),new Matrix4f(local),mesh.attachmentBounds("body",poses));minBody=Math.min(minBody,torso);
                check(torso>.05f,"torso clearance "+torso+" "+part+" "+label);
                float head=PropMeshClearance.separation(geometry.get(part),new Matrix4f(skull.last().pose()).invert().mul(matrix),mesh.attachmentBounds("skull",poses));minHead=Math.min(minHead,head);
                check(head>.15f,"head clearance "+head+" "+part+" "+label);
                for(var face:geometry.get(part))for(var vertex:face){float floor=24-matrix.transformPosition(new Vector3f(vertex)).y*16;minFloor=Math.min(minFloor,floor);check(floor>.1f,"floor clearance "+label);}
              }
            }
          }
        }
      }
    }
    check(samples>0&&renderedSamples>0,"native and replacement cases executed");
    System.out.println("Accordion failure groups: "+groups);
    System.out.println("Accordion mesh fit: "+samples+" poses; "+renderedSamples+" emitted hands; max contact="+contact+" blocks; rendered error="+renderError+"; min head="+minHead+" pixels; min torso="+minBody+" pixels; min floor="+minFloor+" pixels; max reach="+maxReach+" pixels (right="+maxRightReach+", left="+maxLeftReach+"); emitted-hand head="+minHandHead+" (right="+minRightHead+", left="+minLeftHead+"), torso="+minHandBody+" pixels");
    if(failures>0)throw new AssertionError(failures+" accordion rig failures: "+String.join("\n",examples));
  }
}
