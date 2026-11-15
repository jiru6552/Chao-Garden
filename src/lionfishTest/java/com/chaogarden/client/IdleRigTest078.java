package com.chaogarden.client;

import com.chaogarden.core.ChaoIdleRules078;
import com.chaogarden.core.ChaoState;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.blaze3d.vertex.PoseStack;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.joml.Matrix4f;
import org.joml.Vector3f;

/** Production Lionfish/registered-rig samples, fixed anatomy and interruption recovery. */
public final class IdleRigTest078 {
  private static final Path ROOT=Path.of("src/main/resources/assets/chao_garden");
  private static final Map<String,ChaoMeshAsset> ASSETS=new HashMap<>();
  private static JsonObject clips;
  private static long checks,frames,episodes,children;
  private static int gazeTraces,gazeFrames;
  private static float maxGazeSurfaceStep,maxGazeAdmissionStep;
  private static float minHeadHand=Float.POSITIVE_INFINITY,minHandBody=Float.POSITIVE_INFINITY;
  private static float minInheritedHandBody=Float.POSITIVE_INFINITY,maxAddedHandBodyPenetration;
  private static float maxInheritedFootFloorPenetration,maxAddedFootFloorPenetration,maxFootBaselineError;
  private static float minFloor=Float.POSITIVE_INFINITY,maxQuarterStep;
  private static final Map<String,Integer> contactFailures=new java.util.TreeMap<>();
  private static final Map<String,Float> worstContact=new java.util.TreeMap<>();
  private static final Map<String,String> worstContext=new java.util.TreeMap<>();
  private record Named(String name,ChaoState state) {}
  private static void check(boolean value,String message){checks++;if(!value)throw new AssertionError(message);}
  private static void clearance(float gap,String category,String context) {
    checks++;if(gap>=-.0001f)return;
    int cut=context.indexOf(" cancel=");String clip=cut<0?"illness_scratch":context.substring(0,cut);
    category+="/"+(cut<0?clip:clip.substring(clip.lastIndexOf('/')+1));
    contactFailures.merge(category,1,Integer::sum);
    if(gap<worstContact.getOrDefault(category,Float.POSITIVE_INFINITY)){
      worstContact.put(category,gap);worstContext.put(category,context);
    }
  }
  private static ChaoMeshAsset asset(String key) {
    return ASSETS.computeIfAbsent(key,k->{try{return ChaoMeshAsset.parse(JsonParser.parseString(
        Files.readString(ROOT.resolve("models/entity/"+k+".json"))).getAsJsonObject());
      }catch(Exception error){throw new IllegalStateException(k,error);}});
  }
  private static ChaoState state(boolean adult,int alignment,int first,float maturity,float rp,float sf) {
    var s=new ChaoState();s.adult=adult;s.align=alignment;s.primaryType=first;
    s.setEvolutionStrength(maturity*(adult?1.2f:.8f));
    for(int pair=0;pair<2;pair++){
      float value=pair==0?rp:sf;int stat=pair==0?value<0?ChaoState.RUN:ChaoState.POWER:value<0?ChaoState.SWIM:ChaoState.FLY;
      int amount=Math.round(Math.abs(value)*(adult?960:480));s.levels[stat]=amount/100;s.progress[stat]=amount%100;
    }
    return ChaoState.decode(s.encode());
  }
  private static List<Named> states() {
    var result=new ArrayList<Named>();
    for(boolean adult:new boolean[]{false,true})for(int alignment=-1;alignment<=1;alignment++)
      for(int first=0;first<=(adult?4:0);first++)for(float maturity:new float[]{0,.5f,1}){
        float rp=maturity==0?0:maturity==.5f?-.5f:1,sf=maturity==0?0:maturity==.5f?.5f:-1;
        result.add(new Named(adult+"/"+alignment+"/"+first+"/"+maturity,state(adult,alignment,first,maturity,rp,sf)));
      }
    return result;
  }
  private static Matrix4f at(OfficialChaoRig rig,String bone){var stack=new PoseStack();rig.transformBone(bone,stack);return new Matrix4f(stack.last().pose());}
  private static Vector3f[] box(OfficialChaoRig rig,String bone,float[] bounds) {
    check(bounds!=null,"Actual visible bounds "+bone);var matrix=at(rig,bone);var points=new Vector3f[8];
    for(int i=0;i<8;i++)points[i]=matrix.transformPosition(new Vector3f(
        bounds[(i&1)==0?0:3],bounds[(i&2)==0?1:4],bounds[(i&4)==0?2:5]).div(16)).mul(16);
    return points;
  }
  private static Vector3f[] part(OfficialChaoRig rig,String bone){return box(rig,bone,rig.sampledMesh().partBounds(bone,rig.poses()));}
  private static Vector3f[] hand(OfficialChaoRig rig,boolean left){return box(rig,left?"arm_left":"arm_right",rig.baseToyHandBounds(left));}
  private static Vector3f[] axes(Vector3f[] points){return new Vector3f[]{new Vector3f(points[1]).sub(points[0]).normalize(),new Vector3f(points[2]).sub(points[0]).normalize(),new Vector3f(points[4]).sub(points[0]).normalize()};}
  /** Exact cuboid SAT: positive means disjoint; negative means penetration. */
  private static float separation(Vector3f[] a,Vector3f[] b) {
    var aa=axes(a);var bb=axes(b);var axes=new ArrayList<Vector3f>();axes.addAll(List.of(aa));axes.addAll(List.of(bb));
    for(var x:aa)for(var y:bb){var cross=new Vector3f(x).cross(y);if(cross.lengthSquared()>1e-10f)axes.add(cross.normalize());}
    float largest=Float.NEGATIVE_INFINITY;
    for(var axis:axes){float amin=Float.POSITIVE_INFINITY,amax=Float.NEGATIVE_INFINITY,bmin=Float.POSITIVE_INFINITY,bmax=Float.NEGATIVE_INFINITY;
      for(var p:a){float d=axis.dot(p);amin=Math.min(amin,d);amax=Math.max(amax,d);}
      for(var p:b){float d=axis.dot(p);bmin=Math.min(bmin,d);bmax=Math.max(bmax,d);}
      largest=Math.max(largest,Math.max(bmin-amax,amin-bmax));
    }
    return largest;
  }
  private static float[] invariant(ChaoMeshAsset.Pose p){return new float[]{p.sx,p.sy,p.sz,p.reach,p.proportionY};}
  private static Map<String,float[]> invariants(OfficialChaoRig rig){var out=new HashMap<String,float[]>();rig.poses().forEach((n,p)->out.put(n,invariant(p)));return out;}
  private static Map<String,Matrix4f> headChildren(OfficialChaoRig rig) {
    var out=new HashMap<String,Matrix4f>();var inverse=at(rig,"skull").invert();
    for(String name:List.of("skull__cube_0","expression_mouth_1","emotion_dots"))
      if(rig.pose(name)!=null)out.put(name,new Matrix4f(inverse).mul(at(rig,name)));
    return out;
  }
  private static void prepare(OfficialChaoRig rig,ChaoState s,UUID id,OfficialChaoRig.Motion motion,float age) {
    float yaw=(id.getLeastSignificantBits()%3-1)*8,pitch=(id.getLeastSignificantBits()/3%3-1)*5;
    prepare(rig,s,id,motion,age,yaw,pitch);
  }
  private static void prepare(OfficialChaoRig rig,ChaoState s,UUID id,OfficialChaoRig.Motion motion,float age,float yaw,float pitch) {
    String key=OfficialChaoRig.geometryKey(s);rig.prepareDevelopment(key,s);
    rig.animateGameplay(key,id,motion,age,0,0,0,0,yaw,pitch,true,false);rig.development(s);
  }
  private static void anatomy(OfficialChaoRig rig,OfficialChaoRig calm,Map<String,Matrix4f> attached,String context) {
    check(rig.finite(),"Finite production pose "+context);
    var bounds=rig.sampledMesh().partBounds("skull__cube_0",rig.poses());
    check(Math.abs(bounds[3]-bounds[0]-7)<.00001f&&Math.abs(bounds[4]-bounds[1]-6)<.00001f&&Math.abs(bounds[5]-bounds[2]-6)<.00001f,"Original7x6x6 head "+context);
    var inverse=at(rig,"skull").invert();for(var e:attached.entrySet()){
      check(new Matrix4f(inverse).mul(at(rig,e.getKey())).equals(e.getValue(),.00004f),"Skull child remains attached "+context+" "+e.getKey());children++;
    }
    var head=part(rig,"skull__cube_0");var torso=part(rig,"body__cube_0");
    for(var point:head){float floor=24-point.y;minFloor=Math.min(minFloor,floor);clearance(floor,"head_floor",context);}
    for(boolean left:new boolean[]{false,true}){
      var hand=hand(rig,left);float headGap=separation(head,hand),bodyGap=separation(torso,hand);
      float inherited=separation(part(calm,"body__cube_0"),hand(calm,left));
      minInheritedHandBody=Math.min(minInheritedHandBody,inherited);
      maxAddedHandBodyPenetration=Math.max(maxAddedHandBodyPenetration,Math.max(0,-bodyGap)-Math.max(0,-inherited));
      minHeadHand=Math.min(minHeadHand,headGap);minHandBody=Math.min(minHandBody,bodyGap);
      clearance(headGap,"head_hand/"+left,context);
      // The original50degree left resting roll has an inherited0.247px
      // terminal-palm corner overlap. Compare the same production rig, state,
      // motion and transition without an idle: no new/deeper intrusion is
      // permitted; clear inherited poses retain a strict zero bound.
      clearance(bodyGap-Math.min(0,inherited),"added_torso_hand/"+left,context+" inherited="+inherited);
      for(var point:hand){float floor=24-point.y;minFloor=Math.min(minFloor,floor);clearance(floor,"hand_floor/"+left,context);}
    }
    for(String foot:ChaoSittingPose.FEET)if(rig.pose(foot)!=null&&rig.sampledMesh().partBounds(foot,rig.poses())!=null){
      check(at(rig,foot).equals(at(calm,foot),.000001f),"Idle changes an unaffected foot matrix "+context+" "+foot);
      var points=part(rig,foot);var baseline=part(calm,foot);
      for(int i=0;i<points.length;i++){
        float error=points[i].distance(baseline[i]);maxFootBaselineError=Math.max(maxFootBaselineError,error);
        check(error<=.0001f,"Idle changes an unaffected visible foot corner "+context+" "+foot+" corner="+i+" error="+error);
        float floor=24-points[i].y,inherited=24-baseline[i].y;minFloor=Math.min(minFloor,floor);
        maxInheritedFootFloorPenetration=Math.max(maxInheritedFootFloorPenetration,Math.max(0,-inherited));
        maxAddedFootFloorPenetration=Math.max(maxAddedFootFloorPenetration,Math.max(0,-floor)-Math.max(0,-inherited));
        clearance(floor-Math.min(0,inherited),"added_foot_floor/"+foot,context+" inherited="+inherited);
      }
    }
  }
  private static void episode(Named named,String idle,float cancel,int serial) {
    var s=named.state;String saved=s.encode();var rig=new OfficialChaoRig(clips,IdleRigTest078::asset);var calm=new OfficialChaoRig(clips,IdleRigTest078::asset);var id=new UUID(780078,serial);
    var previous=new HashMap<String,Vector3f>();boolean complete=cancel>=160;
    for(int quarter=-32;quarter<=4*176;quarter++){
      float elapsed=quarter*.25f,age=40+elapsed;boolean active=elapsed>=0&&elapsed<cancel;
      float weight=active?ChaoIdleRules078.weight(idle,elapsed):0;
      var motion=ChaoIdleRules078.seated(idle)&&weight>0?OfficialChaoRig.Motion.SIT:OfficialChaoRig.Motion.WALK;
      prepare(rig,s,id,motion,age);var invariant=invariants(rig);var attached=headChildren(rig);
      var restAngles=new HashMap<String,float[]>();for(String bone:List.of("skull","arm_left","arm_right")){var p=rig.pose(bone);restAngles.put(bone,new float[]{p.rx,p.ry,p.rz});}
      if(active)ChaoIdleAnimation078.pose(rig,idle,elapsed);
      for(var e:invariant.entrySet())check(Arrays.equals(e.getValue(),invariant(rig.pose(e.getKey()))),"No part scale, stretch or proportion change "+named.name+"/"+idle+" t="+elapsed+" "+e.getKey());
      if(weight==0)for(var entry:restAngles.entrySet()){
        var p=rig.pose(entry.getKey());check(Arrays.equals(entry.getValue(),new float[]{p.rx,p.ry,p.rz}),"Zero endpoint or calm preview tail must preserve exact resting pose "+idle+" t="+elapsed);
      }
      // Production previews admit their activity token at elapsed0, including
      // the zero-weight first frame and calm tail: gaze is already clamped.
      int token=active?1024*(44+ChaoIdleRules078.IDS.indexOf(idle)):0;
      rig.finishGameplayFrame(id,token,age);
      prepare(calm,s,id,motion,age);calm.finishGameplayFrame(id,token,age);
      String context=named.name+"/"+idle+" cancel="+cancel+" t="+elapsed;
      anatomy(rig,calm,attached,context);check(rig.idleGestureWeight()==0,"Ambiguous imported sweep must remain disabled in gameplay");
      for(String bone:List.of("skull","arm_left","arm_right","foot_left","foot_right","feet_bone_right")){
        if(rig.pose(bone)==null)continue;var point=at(rig,bone).transformPosition(new Vector3f()).mul(16);var prior=previous.put(bone,point);
        if(prior!=null){float step=point.distance(prior);maxQuarterStep=Math.max(maxQuarterStep,step);check(step<1.25f,"Quarter-tick joint pop "+context+" "+bone+" step="+step);}
      }
      check(saved.equals(s.encode()),"Idle altered saved care/genetics/age "+context);frames++;
    }
    check(complete||cancel<ChaoIdleRules078.duration(idle),"Cancellation must interrupt the actual clip");episodes++;
  }
  private static void authored() {
    var clip=new LionfishIdleClip078();
    for(String id:ChaoIdleRules078.IDS){
      var start=clip.sample(id,0);var end=clip.sample(id,ChaoIdleRules078.duration(id));
      check(start.head().length()<.00001f&&start.left().length()<.00001f&&start.right().length()<.00001f,"Authored zero start "+id);
      check(end.head().length()<.00001f&&end.left().length()<.00001f&&end.right().length()<.00001f,"Authored zero end "+id);
      check(clip.sample(id,ChaoIdleRules078.duration(id)*.4f).head().length()>.05f,"Real Lionfish joint movement "+id);
    }
    check(clip.sample("look_around",20).head().y<-.1f&&clip.sample("look_around",50).head().y>.1f,"Look around visits both sides");
    check(clip.sample("look_up",24).head().x<-.2f,"Look up raises the fixed head");
    check(clip.sample("ground_play",35).left().distance(clip.sample("ground_play",51).left())>.1f,"Ground paw follows an authored tracing stroke");
  }
  /** A large ordinary LookControl gaze must enter, cancel and release through
   * the same transition as the clip, even when its first key has weight0.
   * Surface corners expose rotation snaps that a skull-pivot check cannot. */
  private static void gazeAdmission(Named named,String idle,int cancel,float ordinaryYaw,int serial) {
    var s=named.state;var rig=new OfficialChaoRig(clips,IdleRigTest078::asset);var id=new UUID(780080,serial);
    Vector3f[] previous=null;
    for(int quarter=-32;quarter<=4*(cancel+16);quarter++){
      float elapsed=quarter*.25f,age=300+elapsed;boolean active=elapsed>=0&&elapsed<cancel;
      float weight=active?ChaoIdleRules078.weight(idle,elapsed):0;
      var motion=active&&ChaoIdleRules078.seated(idle)&&weight>0?OfficialChaoRig.Motion.SIT:OfficialChaoRig.Motion.WALK;
      prepare(rig,s,id,motion,age,active?Math.clamp(ordinaryYaw,-8,8):ordinaryYaw,active?5:18);
      if(active)ChaoIdleAnimation078.pose(rig,idle,elapsed);
      var head=rig.pose("skull");float targetYaw=head.ry,targetPitch=head.rx;
      int token=active?1024*(44+ChaoIdleRules078.IDS.indexOf(idle)):0;
      rig.finishGameplayFrame(id,token,age);
      var points=part(rig,"skull__cube_0");String context=named.name+"/"+idle+" cancel="+cancel+" yaw="+ordinaryYaw+" t="+elapsed;
      if(previous!=null)for(int corner=0;corner<points.length;corner++){
        float step=points[corner].distance(previous[corner]);maxGazeSurfaceStep=Math.max(maxGazeSurfaceStep,step);
        check(step<.35f,"Gaze admission/release surface pop "+context+" corner="+corner+" step="+step);
        if(elapsed==0||elapsed==cancel){
          maxGazeAdmissionStep=Math.max(maxGazeAdmissionStep,step);
          check(step<.02f,"First admitted/cancelled frame must retain the outgoing head "+context+" corner="+corner+" step="+step);
        }
      }
      if(!active&&elapsed>=cancel+8){
        check(Math.abs(head.ry-targetYaw)<.000001f&&Math.abs(head.rx-targetPitch)<.000001f,"Ordinary gaze fully restored after activity transition "+context);
      }
      previous=points;gazeFrames++;
    }
    gazeTraces++;
  }
  public static void main(String[] args)throws Exception {
    clips=JsonParser.parseString(Files.readString(ROOT.resolve("animations/official/chao.animation.json"))).getAsJsonObject().getAsJsonObject("animations");authored();
    int serial=0;for(var named:states())for(String idle:ChaoIdleRules078.IDS)episode(named,idle,160,++serial);
    for(var named:List.of(new Named("baby_cancel",state(false,0,0,.5f,-.5f,.5f)),new Named("adult_cancel",state(true,-1,4,1,1,-1))))
      for(String idle:ChaoIdleRules078.IDS)for(float fraction:new float[]{.35f,.70f})episode(named,idle,ChaoIdleRules078.duration(idle)*fraction,++serial);
    for(var named:List.of(new Named("baby_gaze",state(false,0,0,.5f,-.5f,.5f)),new Named("hero_power_gaze",state(true,1,4,1,1,-1))))
      for(String idle:List.of("look_around","ground_play"))for(int cancel:new int[]{1,24,160})for(float yaw:new float[]{-37,37})gazeAdmission(named,idle,cancel,yaw,++serial);
    System.out.println("Idle078 actual registered rigs: "+checks+" checks; "+episodes+" episodes, "+frames+" quarter frames, "+children+" attached head-child matrices; min head/hand="+minHeadHand+"px, min torso/hand="+minHandBody+"px, matching inherited torso/hand="+minInheritedHandBody+"px, maximum added torso penetration="+maxAddedHandBodyPenetration+"px, min floor="+minFloor+"px, inherited foot/floor penetration="+maxInheritedFootFloorPenetration+"px, maximum added foot/floor penetration="+maxAddedFootFloorPenetration+"px, maximum foot/baseline corner error="+maxFootBaselineError+"px, max quarter-joint step="+maxQuarterStep+"px. Numerical samples complement actual client motion review.");
    System.out.println("Idle078 production gaze admission: "+gazeTraces+" entry/early-cancel/mid-cancel/release traces, "+gazeFrames+" quarter frames; max head-surface step="+maxGazeSurfaceStep+"px, max first admitted/cancelled head-surface step="+maxGazeAdmissionStep+"px.");
    if(!contactFailures.isEmpty()){
      for(String category:contactFailures.keySet())System.err.println("Idle078 clearance failure "+category+" count="+contactFailures.get(category)+" worst="+worstContact.get(category)+"px at "+worstContext.get(category));
      throw new AssertionError("Idle078 clearance violations "+contactFailures);
    }
  }
}
