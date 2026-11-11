package com.chaogarden.client;

import com.chaogarden.core.ChaoState;
import com.chaogarden.toys.MaracasPlayRules;
import com.google.gson.JsonParser;
import com.mojang.blaze3d.vertex.PoseStack;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Function;
import org.joml.Vector3f;

/** Production prone support at every quarter tick, plus actual glass/animal soles. */
public final class ProneToyRigTest051 {
  private static final Path ROOT=Path.of("src/main/resources/assets/chao_garden/models/entity");
  private record Profile(String name,float headAngle,float headLiftPixels,int transition){}
  private static final List<Profile> PROFILES=List.of(
      new Profile("car",.20f,.80f,17),new Profile("crayons",.25f,.60f,27));
  private static int checks,frames,cases,footCases;
  private static float maxStep,minHeadFloor=Float.POSITIVE_INFINITY,minSeparation=Float.POSITIVE_INFINITY;
  private static float maxFootY,maxBodyY;
  private static int exitFrames;
  private static float maxExitStep,maxExitLift;
  private static final Map<String,float[]> FOOTPRINTS=new HashMap<>();
  private static final Map<String,float[]> WORLD_FOOTPRINTS=new HashMap<>();
  private static final List<String> FAILURES=new ArrayList<>();
  private static int failures;
  private static void check(boolean condition,String reason){checks++;if(!condition){failures++;if(FAILURES.size()<25)FAILURES.add(reason);}}
  private static ChaoMeshAsset load(Path path)throws Exception{return ChaoMeshAsset.parse(JsonParser.parseString(Files.readString(path)).getAsJsonObject());}
  private static void union(Map<String,float[]> map,String kind,float[] b,float scale){
    var out=map.computeIfAbsent(kind,key->new float[]{Float.POSITIVE_INFINITY,Float.POSITIVE_INFINITY,Float.NEGATIVE_INFINITY,Float.NEGATIVE_INFINITY});
    out[0]=Math.min(out[0],b[0]*scale);out[1]=Math.min(out[1],b[2]*scale);out[2]=Math.max(out[2],b[3]*scale);out[3]=Math.max(out[3],b[5]*scale);
  }
  private static float[] snapshot(ChaoMeshAsset.Pose p){return new float[]{p.x,p.y,p.z,p.rx,p.ry,p.rz,p.sx,p.sy,p.sz,p.reach,p.proportionY,p.visible?1:0};}
  private static Function<String,float[]> bounds(ChaoMeshAsset source,Map<String,ChaoMeshAsset.Pose> poses,
                                               ChaoMeshAsset feet,Map<String,ChaoMeshAsset.Pose> footPoses,
                                               int animal,boolean adult){
    return bone->{
      if(!ChaoSittingPose.FEET.contains(bone))return source.attachmentBounds(bone,poses);
      if(feet==source)return source.partBounds(bone,poses);
      footPoses.forEach((name,p)->{
        p.reset();var from=poses.get(name);if(from!=null)ChaoOverlayLayer.copy(from,p);
        if(ChaoSittingPose.FEET.contains(name))p.visible=true;
        if(name.startsWith("animal_")){
          p.visible=animal>=0&&name.startsWith("animal_"+ChaoState.FEET+"_"+animal+"_");
          if(p.visible&&!adult&&animal<21)p.sx=p.sy=p.sz=.92f;
        }
      });
      if(bone.equals("feet_bone_right")){
        var alias=footPoses.get("foot_right");var from=poses.get(bone);
        if(alias!=null&&from!=null)ChaoOverlayLayer.copy(from,alias);
      }
      return feet.partBounds(bone.equals("feet_bone_right")?"foot_right":bone,footPoses);
    };
  }
  private static void verify(ChaoMeshAsset mesh,Map<String,ChaoMeshAsset.Pose> poses,
                             Function<String,float[]> bounds,boolean adult,boolean chaos,Profile profile,
                             String label,int[] ticks){
    String kind=profile.name;
    BiConsumer<String,PoseStack> anchor=(bone,at)->mesh.transformBone(bone,at,poses);
    var previous=new HashMap<String,Vector3f>();
    for(int n:ticks){
      float tick=n*.25f,w=MaracasPlayRules.presence(tick);
      poses.values().forEach(ChaoMeshAsset.Pose::reset);
      var before=new HashMap<String,float[]>();if(w==0)poses.forEach((name,p)->before.put(name,snapshot(p)));
      ChaoProneToyPose051.apply(poses,anchor,bounds,w,profile.headAngle,profile.headLiftPixels);frames++;
      if(w==0)poses.forEach((name,p)->check(Arrays.equals(before.get(name),snapshot(p)),"zero-presence changed "+label+" "+name));
      for(String bone:List.of("body","skull","foot_left","foot_right","feet_bone_right")){
        if(!poses.containsKey(bone))continue;
        for(float value:snapshot(poses.get(bone)))check(Float.isFinite(value),"nonfinite "+label+" "+bone+" t="+tick);
        var at=new PoseStack();anchor.accept(bone,at);var origin=at.last().pose().transformPosition(new Vector3f());
        var old=previous.put(bone,origin);
        if(old!=null&&ticks.length==641){float step=old.distance(origin)*16;maxStep=Math.max(maxStep,step);check(step<.55f,"quarter-tick joint jump "+step+" "+label+" "+bone+" t="+tick);}
        var b=ChaoProneToyPose051.worldBounds(anchor,bounds,bone);if(b==null)continue;
        if(w>0){
          union(FOOTPRINTS,kind,b,1);union(WORLD_FOOTPRINTS,kind,b,chaos?.86f:adult?.8f:.64f);
          if(bone.equals("body")){
            maxBodyY=Math.max(maxBodyY,b[4]);check(b[4]<=ChaoProneToyPose051.FLOOR_Y+.00001f,"belly entered floor "+label+" t="+tick);
            if(w==1)check(Math.abs(b[4]-(ChaoProneToyPose051.FLOOR_Y-.008f))<.00002f,"belly not supported "+label);
          }else if(bone.equals("skull")){
            minHeadFloor=Math.min(minHeadFloor,ChaoProneToyPose051.FLOOR_Y-b[4]);
            check(b[4]<=ChaoProneToyPose051.FLOOR_Y-.002f,"head entered floor "+label);
          }else{
            maxFootY=Math.max(maxFootY,b[4]);check(b[4]<=ChaoProneToyPose051.FLOOR_Y-.001f,"actual foot entered floor "+label+" "+bone+" t="+tick);
            if(w==1)check(Math.abs(b[4]-(ChaoProneToyPose051.FLOOR_Y-.002f))<.00003f,"actual foot not supported "+label+" "+bone);
          }
        }
      }
      if(w>0){float gap=ChaoProneToyPose051.separation(anchor,bounds);minSeparation=Math.min(minSeparation,gap);check(gap>=-.00001f,"head/torso overlap "+label+" t="+tick+" gap="+gap);}
      if(w==1)check(Math.abs(poses.get("body").rx-ChaoProneToyPose051.BODY_ANGLE)<.000001f,"body lost prone angle "+label);
    }
  }
  private static void actualTransitions()throws Exception{
    var clipPath=ROOT.getParent().getParent().resolve("animations/official/chao.animation.json");
    var clips=JsonParser.parseString(Files.readString(clipPath)).getAsJsonObject().getAsJsonObject("animations");
    var assets=new HashMap<String,ChaoMeshAsset>();
    var rig=new OfficialChaoRig(clips,key->assets.computeIfAbsent(key,k->{try{return load(ROOT.resolve(k+".json"));}catch(Exception e){throw new IllegalStateException(e);}}));
    int serial=0;
    for(var path:Files.list(ROOT.resolve("gameplay")).filter(p->p.toString().endsWith(".json")&&!p.toString().contains("_glass")).sorted().toList()){
      String model=path.getFileName().toString().replace("chao_","").replace(".json","");
      for(boolean adult:new boolean[]{false,true})for(var profile:PROFILES){
        var state=new ChaoState();state.adult=adult;state.setEvolutionStrength(1.2f);
        state.align=model.startsWith("hero")?1:model.startsWith("dark")?-1:0;
        if(model.endsWith("swim"))state.primaryType=ChaoState.SWIM_TYPE;
        if(model.endsWith("fly"))state.primaryType=ChaoState.FLY_TYPE;
        if(model.endsWith("run"))state.primaryType=ChaoState.RUN_TYPE;
        if(model.endsWith("power"))state.primaryType=ChaoState.POWER_TYPE;
        if(List.of("tails","knuckles","amy","omochao").contains(model))state.character=model;
        state.immortal=model.startsWith("chaos_");
        var id=new java.util.UUID(510051,++serial);
        Function<String,float[]> box=null;BiConsumer<String,PoseStack> anchor=rig::transformBone;
        var previous=new HashMap<String,Vector3f>();
        for(int n=0;n<=480;n++){
          float age=n*.25f,tick=age-20;boolean active=tick>=0&&tick<80;
          rig.prepareDevelopment(model,state);
          rig.animateGameplay(model,id,active||adult?OfficialChaoRig.Motion.WALK:OfficialChaoRig.Motion.SIT,
              age,0,0,0,0,0,0,false,false);rig.development(state);
          rig.finishGameplayFrame(id,active?1024*profile.transition:0,age);
          if(box==null){
            var mesh=assets.get("gameplay/chao_"+model);
            var soles=state.immortal?load(path.resolveSibling(path.getFileName().toString().replace(".json","_glass.json"))):mesh;
            var support=bounds(mesh,rig.poses(),soles,soles.createPoses(),-1,adult);
            box=bone->{
              if(!bone.startsWith("hand_"))return support.apply(bone);
              boolean left=bone.equals("hand_left");var b=rig.baseToyHandBounds(left);if(b==null)return null;
              var p=rig.pose(left?"arm_left":"arm_right");float reach=p.reach*(left?1:-1);b[0]+=reach;b[3]+=reach;return b;
            };
          }
          boolean recovering=rig.recoveringProne(id);float before=rig.pose("root").y;
          if(recovering&&ChaoProneToyPose051.clearExitFloor(rig.poses(),anchor,box))rig.rememberGameplayFrame(id);
          maxExitLift=Math.max(maxExitLift,before-rig.pose("root").y);
          if(active){ChaoProneToyPose051.apply(rig.poses(),anchor,box,MaracasPlayRules.presence(tick),profile.headAngle,profile.headLiftPixels);rig.rememberGameplayFrame(id);}
          rig.rememberProneFrame(id,active&&MaracasPlayRules.presence(tick)>0);
          if(tick>=80&&tick<88){
            check(recovering,"actual outgoing prone pose lost its recovery marker "+model);exitFrames++;
            for(String bone:List.of("body","skull","foot_left","foot_right","feet_bone_right","hand_left","hand_right")){
              if(!bone.startsWith("hand_")&&!rig.poses().containsKey(bone))continue;
              var b=ChaoProneToyPose051.worldBounds(anchor,box,bone);if(b==null)continue;
              check(b[4]<=ChaoProneToyPose051.FLOOR_Y+.00001f,"actual outgoing blend entered floor "+model+" "+bone+" age="+age);
              String joint=bone.startsWith("hand_")?bone.replace("hand_","arm_"):bone;
              var at=new PoseStack();anchor.accept(joint,at);var point=at.last().pose().transformPosition(new Vector3f());
              var old=previous.put(bone,point);
              if(old!=null){float step=old.distance(point)*16;maxExitStep=Math.max(maxExitStep,step);check(step<1.25f,"actual outgoing joint snapped "+model+" "+bone+" step="+step);}
            }
          }else if(tick>=88)check(!recovering,"prone recovery leaked beyond the ordinary eight-tick blend "+model);
        }
        // A culled actor or reset animation age must not replay a retained
        // prone frame, nor impose its ground constraint on an ordinary pose.
        for(float[] ages:new float[][]{{300,320},{400,390}}){
          rig.prepareDevelopment(model,state);rig.animateGameplay(model,id,OfficialChaoRig.Motion.WALK,ages[0],0,0,0,0,0,0,false,false);rig.development(state);rig.finishGameplayFrame(id,1024*profile.transition,ages[0]);
          ChaoProneToyPose051.apply(rig.poses(),anchor,box,1,profile.headAngle,profile.headLiftPixels);rig.rememberGameplayFrame(id);rig.rememberProneFrame(id,true);
          rig.animateGameplay(model,id,OfficialChaoRig.Motion.WALK,ages[1],0,0,0,0,0,0,false,false);rig.development(state);rig.finishGameplayFrame(id,0,ages[1]);
          check(!rig.recoveringProne(id),"stale/reset prone recovery leaked "+model);rig.rememberProneFrame(id,false);
        }
      }
    }
  }
  public static void main(String[] args)throws Exception{
    int[] all=new int[641];for(int i=0;i<all.length;i++)all[i]=i;
    int[] contacts={1,4,12,24,40,48,96,160,320,560,600,624,636,639};
    for(var path:Files.list(ROOT.resolve("gameplay")).filter(p->p.toString().endsWith(".json")&&!p.toString().contains("_glass")).sorted().toList()){
      String name=path.getFileName().toString();boolean chaos=name.contains("chaos_");
      var mesh=load(path);var glass=chaos?load(path.resolveSibling(name.replace(".json","_glass.json"))):mesh;
      for(boolean adult:new boolean[]{false,true})for(float sf:new float[]{-1,0,1})for(float rp:new float[]{-1,0,1}){
        mesh.development(sf,rp,adult?1:.14f);glass.development(sf,rp,adult?1:.14f);
        var poses=mesh.createPoses();var footPoses=glass.createPoses();var box=bounds(mesh,poses,glass,footPoses,-1,adult);
        for(var profile:PROFILES){cases++;verify(mesh,poses,box,adult,chaos,profile,name+" age="+adult+" morph="+sf+","+rp,all);}
        if(sf!=0||rp!=0||chaos||name.contains("omochao"))continue;
        for(int animal=0;animal<ChaoState.ANIMALS.length;animal++){
          if((ChaoState.animalSlots(animal)&(1<<ChaoState.FEET))==0)continue;
          Path feetPath=ROOT.resolve((animal<21?"overlays/":"cwe_parts/")+name);if(!Files.exists(feetPath))continue;
          var feet=load(feetPath);var fp=feet.createPoses();var actual=bounds(mesh,poses,feet,fp,animal,adult);
          for(var profile:PROFILES){footCases++;verify(mesh,poses,actual,adult,false,profile,name+" animal="+animal+" age="+adult,contacts);}
        }
      }
    }
    actualTransitions();
    StringBuilder out=new StringBuilder("Prone support: "+checks+" checks; "+cases+" source form/age/morph/head cases; "+frames+" quarter/contact frames; "+footCases+" actual replacement-foot cases; maxQuarterJointPixels="+maxStep+" minHeadFloor="+minHeadFloor+" minHeadBody="+minSeparation+" maxBellyY="+maxBodyY+" maxFootY="+maxFootY+" actualExitFrames="+exitFrames+" maxExitJointPixels="+maxExitStep+" maxExitRootLiftPixels="+maxExitLift+" failures="+failures+"\n");
    for(String kind:List.of("car","crayons"))out.append(kind).append(" model XZ[minX,minZ,maxX,maxZ]=").append(Arrays.toString(FOOTPRINTS.get(kind))).append(" world XZ=").append(Arrays.toString(WORLD_FOOTPRINTS.get(kind))).append('\n');
    Files.createDirectories(Path.of("build/reports/lionfish"));Files.writeString(Path.of("build/reports/lionfish/prone-toys051-results.txt"),out.toString());
    System.out.print(out);FAILURES.forEach(System.out::println);if(failures>0)throw new AssertionError("Prone support failures="+failures);
  }
}
