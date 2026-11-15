package com.chaogarden.client;

import com.chaogarden.core.ChaoState;
import com.chaogarden.toys.ActiveToyPlayRules049;
import com.chaogarden.toys.MaracasPlayRules;
import com.chaogarden.toys.QuietToyPlayRules049;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.function.Function;
import org.joml.Matrix4f;
import org.joml.Vector3f;

/** Actual state-selected car/crayon frames, including the two reciprocal .72 rows.
 * The maximum neck gap is measured between emitted visible base-head/torso faces,
 * rather than accepting a small separation of an enclosing crown/hat envelope. */
public final class ProneHeadConnectionTest073 {
  private static final Path ROOT=Path.of("src/main/resources/assets/chao_garden");
  private static final Map<String,ChaoMeshAsset> ASSETS=new HashMap<>();
  private static int checks,states,frames,contacts,hatFrames,attachmentFrames;
  private static float maxGap,maxHeadStep,maxExitStep,minHeadFloor=Float.POSITIVE_INFINITY;
  private static float minCarLower=Float.POSITIVE_INFINITY,maxCarLower,minCrayonLower=Float.POSITIVE_INFINITY,maxCrayonLower;
  private record Profile(String kind,float angle,float oldLift,float lift,float maxGap,int transition){}
  private static final List<Profile> PROFILES=List.of(
      new Profile(ActiveToyPlayRules049.CAR,.10f,.40f,.10f,.25f,17),
      new Profile(QuietToyPlayRules049.CRAYONS,.20f,.15f,0,.25f,27));
  private static void check(boolean value,String message){checks++;if(!value)throw new AssertionError(message);}
  private static ChaoMeshAsset asset(String key){
    return ASSETS.computeIfAbsent(key,k->{try{return ChaoMeshAsset.parse(JsonParser.parseString(Files.readString(ROOT.resolve("models/entity/"+k+".json"))).getAsJsonObject());}catch(Exception e){throw new IllegalStateException(k,e);}});
  }
  private static ChaoState state(boolean adult,float alignment,int first,float rp,float sf,float age){
    var s=new ChaoState();s.adult=adult;s.align=alignment;s.primaryType=first;
    int r=Math.round(Math.abs(rp)*(adult?960:480)),f=Math.round(Math.abs(sf)*(adult?960:480));
    int rs=rp<0?ChaoState.RUN:ChaoState.POWER,fs=sf<0?ChaoState.SWIM:ChaoState.FLY;
    s.levels[rs]=r/100;s.progress[rs]=r%100;s.points[rs]=r;
    s.levels[fs]=f/100;s.progress[fs]=f%100;s.points[fs]=f;
    s.setEvolutionStrength(age*(adult?1.2f:.8f));return s;
  }
  private static final class Surface implements VertexConsumer {
    final List<Vector3f> vertices=new ArrayList<>();
    public VertexConsumer addVertex(float x,float y,float z){vertices.add(new Vector3f(x,y,z).mul(16));return this;}
    public VertexConsumer setColor(int r,int g,int b,int a){return this;}
    public VertexConsumer setUv(float u,float v){return this;}
    public VertexConsumer setUv1(int u,int v){return this;}
    public VertexConsumer setUv2(int u,int v){return this;}
    public VertexConsumer setNormal(float x,float y,float z){return this;}
  }
  private static Surface surface(OfficialChaoRig rig,String bone){
    var saved=new HashMap<String,Boolean>();rig.poses().forEach((n,p)->{saved.put(n,p.visible);p.visible=n.equals(bone);});
    try{var result=new Surface();rig.sampledMesh().render(new PoseStack(),result,0xf000f0,0,-1,rig.poses(),true);check(result.vertices.size()==24,"Visible core must remain a closed six-face cuboid: "+bone);return result;}
    finally{rig.poses().forEach((n,p)->p.visible=saved.get(n));}
  }
  private static double length(double x,double y,double z){return x*x+y*y+z*z;}
  /** Closest point on the actual emitted triangle (all Voronoi regions). */
  private static double pointTriangle(Vector3f p,Vector3f a,Vector3f b,Vector3f c){
    double abx=b.x-a.x,aby=b.y-a.y,abz=b.z-a.z,acx=c.x-a.x,acy=c.y-a.y,acz=c.z-a.z;
    double apx=p.x-a.x,apy=p.y-a.y,apz=p.z-a.z,d1=abx*apx+aby*apy+abz*apz,d2=acx*apx+acy*apy+acz*apz;
    if(d1<=0&&d2<=0)return length(apx,apy,apz);
    double bpx=p.x-b.x,bpy=p.y-b.y,bpz=p.z-b.z,d3=abx*bpx+aby*bpy+abz*bpz,d4=acx*bpx+acy*bpy+acz*bpz;
    if(d3>=0&&d4<=d3)return length(bpx,bpy,bpz);
    double vc=d1*d4-d3*d2;
    if(vc<=0&&d1>=0&&d3<=0){double v=d1/(d1-d3);return length(apx-v*abx,apy-v*aby,apz-v*abz);}
    double cpx=p.x-c.x,cpy=p.y-c.y,cpz=p.z-c.z,d5=abx*cpx+aby*cpy+abz*cpz,d6=acx*cpx+acy*cpy+acz*cpz;
    if(d6>=0&&d5<=d6)return length(cpx,cpy,cpz);
    double vb=d5*d2-d1*d6;
    if(vb<=0&&d2>=0&&d6<=0){double w=d2/(d2-d6);return length(apx-w*acx,apy-w*acy,apz-w*acz);}
    double va=d3*d6-d5*d4;
    if(va<=0&&d4-d3>=0&&d5-d6>=0){double w=(d4-d3)/((d4-d3)+(d5-d6));return length(bpx-w*(c.x-b.x),bpy-w*(c.y-b.y),bpz-w*(c.z-b.z));}
    double inverse=1/(va+vb+vc),v=vb*inverse,w=vc*inverse;
    return length(apx-v*abx-w*acx,apy-v*aby-w*acy,apz-v*abz-w*acz);
  }
  private static double segmentDistance(Vector3f p,Vector3f q,Vector3f a,Vector3f b){
    double ux=q.x-p.x,uy=q.y-p.y,uz=q.z-p.z,vx=b.x-a.x,vy=b.y-a.y,vz=b.z-a.z,rx=p.x-a.x,ry=p.y-a.y,rz=p.z-a.z;
    double aa=length(ux,uy,uz),ee=length(vx,vy,vz),bb=ux*vx+uy*vy+uz*vz,cc=ux*rx+uy*ry+uz*rz,ff=vx*rx+vy*ry+vz*rz;
    double denominator=aa*ee-bb*bb,s=denominator>1e-12?Math.clamp((bb*ff-cc*ee)/denominator,0,1):0,t=(bb*s+ff)/ee;
    if(t<0){t=0;s=Math.clamp(-cc/aa,0,1);}else if(t>1){t=1;s=Math.clamp((bb-cc)/aa,0,1);}
    return length(rx+s*ux-t*vx,ry+s*uy-t*vy,rz+s*uz-t*vz);
  }
  private static float faceDistance(Surface a,Surface b){
    double distance=Double.POSITIVE_INFINITY;
    for(var p:a.vertices)for(int n=0;n<24;n+=4){distance=Math.min(distance,pointTriangle(p,b.vertices.get(n),b.vertices.get(n+1),b.vertices.get(n+2)));distance=Math.min(distance,pointTriangle(p,b.vertices.get(n),b.vertices.get(n+2),b.vertices.get(n+3)));}
    for(var p:b.vertices)for(int n=0;n<24;n+=4){distance=Math.min(distance,pointTriangle(p,a.vertices.get(n),a.vertices.get(n+1),a.vertices.get(n+2)));distance=Math.min(distance,pointTriangle(p,a.vertices.get(n),a.vertices.get(n+2),a.vertices.get(n+3)));}
    for(int af=0;af<24;af+=4)for(int ae=0;ae<4;ae++)for(int bf=0;bf<24;bf+=4)for(int be=0;be<4;be++)distance=Math.min(distance,segmentDistance(a.vertices.get(af+ae),a.vertices.get(af+(ae+1)%4),b.vertices.get(bf+be),b.vertices.get(bf+(be+1)%4)));
    return(float)Math.sqrt(Math.max(0,distance));
  }
  private static Matrix4f at(OfficialChaoRig rig,String bone){var p=new PoseStack();rig.transformBone(bone,p);return new Matrix4f(p.last().pose());}
  private static List<Float> phases(int end){
    var result=new ArrayList<Float>();for(int n=-8;n<=48;n++)result.add(n*.25f);
    result.addAll(end==160?List.of(16f,40f,80f,120f,144f):List.of(16f,40f));
    for(int n=(end==160?146:end-4)*4;n<=(end+10)*4;n++)result.add(n*.25f);result.add(end+16f);return result;
  }
  private static void profile(JsonObject clips,ChaoState state,Profile profile,boolean hat,int serial){
    profile(clips,state,profile,hat,serial,160,OfficialChaoRig.Motion.SIT);
  }
  private static void profile(JsonObject clips,ChaoState state,Profile profile,boolean hat,int serial,int end,OfficialChaoRig.Motion rest){
    String saved=state.encode(),key=OfficialChaoRig.geometryKey(state),context="adult="+state.adult+" align="+state.align+" first="+state.primaryType+" toy="+profile.kind+" hat="+hat+" end="+end+" rest="+rest+" id="+serial;
    var rig=new OfficialChaoRig(clips,ProneHeadConnectionTest073::asset);var id=new UUID(730051,serial);var grip=new ChaoToyGrip();
    var activeClip=new LionfishActiveToyClip049();var quietClip=new LionfishQuietToyClip049();
    BiConsumer<String,PoseStack> anchor=rig::transformBone;
    Function<String,float[]> bounds=bone->{
      if(bone.startsWith("hand_")){boolean left=bone.endsWith("left");var b=grip.boundsVisible(state,rig,left);if(b==null)return null;var arm=rig.pose(left?"arm_left":"arm_right");b[0]+=arm.reach*(left?1:-1);b[3]+=arm.reach*(left?1:-1);return b;}
      var b=ChaoSittingPose.FEET.contains(bone)?rig.sampledMesh().partBounds(bone,rig.poses()):rig.attachmentBounds(bone);
      return hat&&bone.equals("skull")?VanillaHatRenderer050.fittedBounds(b):b;
    };
    Vector3f previous=null;float previousPhase=Float.NaN;
    for(float phase:phases(end)){
      float tick=phase+20;boolean active=phase>=0&&phase<end;float weight=active?MaracasPlayRules.presence(phase):0;
      var motion=active?OfficialChaoRig.Motion.WALK:rest;
      rig.prepareDevelopment(key,state);rig.animateGameplay(key,id,motion,tick,motion==OfficialChaoRig.Motion.CRAWL?.035f*tick:0,motion==OfficialChaoRig.Motion.CRAWL?.08f:0,0,0,0,0,false,false);rig.development(state);rig.expression(0);
      float[] right=grip.locateVisible(state,rig,false),left=grip.locateVisible(state,rig,true);
      var wing=rig.floorToyWingBasis069(id,active);rig.finishGameplayFrame(id,active?1024*profile.transition:0,tick);
      if(rig.recoveringProne(id)&&ChaoProneToyPose051.clearExitFloor(rig.poses(),anchor,bounds))rig.rememberGameplayFrame(id);
      rig.floorToyWingSource069(id,wing);
      var relative=new HashMap<String,Matrix4f>();var inverse=at(rig,"skull").invert();
      for(String bone:List.of("skull__cube_0","emotion_dots","expression_mouth_1"))if(rig.pose(bone)!=null)relative.put(bone,new Matrix4f(inverse).mul(at(rig,bone)));
      float legacyY=Float.NaN;
      if(weight==1){
        var copy=rig.sampledMesh().createPoses();copy.forEach((name,p)->ChaoOverlayLayer.copy(rig.pose(name),p));
        BiConsumer<String,PoseStack> oldAnchor=(name,p)->rig.sampledMesh().transformBone(name,p,copy);
        Function<String,float[]> oldBounds=bone->{var b=ChaoSittingPose.FEET.contains(bone)?rig.sampledMesh().partBounds(bone,copy):rig.sampledMesh().attachmentBounds(bone,copy);return hat&&bone.equals("skull")?VanillaHatRenderer050.fittedBounds(b):b;};
        ChaoProneToyPose051.apply(copy,oldAnchor,oldBounds,1,profile.angle,profile.oldLift);var oldAt=new PoseStack();oldAnchor.accept("skull",oldAt);legacyY=oldAt.last().pose().transformPosition(new Vector3f()).y;
      }
      if(active){
        if(profile.kind.equals(ActiveToyPlayRules049.CAR))ChaoActiveToyAnimation049.pose(rig.poses(),anchor,right,left,profile.kind,phase,weight,activeClip,bounds);
        else ChaoQuietToyAnimation049.pose(rig.poses(),anchor,right,left,profile.kind,weight,quietClip.sweep(phase),quietClip.page(phase),quietClip.stroke(phase),quietClip.pen(phase),phase,state.adult?.8f:.64f,bounds);
        rig.rememberGameplayFrame(id);
      }
      if(rig.floorToyWings069(wing,weight))rig.rememberGameplayFrame(id);rig.rememberProneFrame(id,active&&weight>0);frames++;
      var skull=rig.pose("skull");check(skull.sx==1&&skull.sy==1&&skull.sz==1,"Head scale changed: "+context);
      var core=rig.sampledMesh().partBounds("skull__cube_0",rig.poses());check(core!=null&&Math.abs(core[3]-core[0]-7)<.0001f&&Math.abs(core[4]-core[1]-6)<.0001f&&Math.abs(core[5]-core[2]-6)<.0001f,"Original head dimensions changed: "+context);
      var nowInverse=at(rig,"skull").invert();for(var item:relative.entrySet()){check(new Matrix4f(nowInverse).mul(at(rig,item.getKey())).equals(item.getValue(),.00003f),"Head child/accessory moved relative to its head: "+context+" "+item.getKey());attachmentFrames++;}
      var origin=at(rig,"skull").transformPosition(new Vector3f());
      // This includes the whole incoming SIT/WALK blend, not just the isolated
      // prone helper. Keep the original helper's .55px check in ProneToyRigTest;
      // use its existing complete interruption-pose bound for this domain.
      if(previous!=null&&Math.abs(phase-previousPhase-.25f)<.00001f){float step=origin.distance(previous)*16;if(active){maxHeadStep=Math.max(maxHeadStep,step);check(step<1.25f,"Head quarter-tick entry/outro jump: "+context+" phase="+phase+" step="+step);}else if(phase>=end&&phase<end+8){maxExitStep=Math.max(maxExitStep,step);check(step<1.25f,"Head return-to-idle jump: "+context+" phase="+phase);}}
      previous=origin;previousPhase=phase;
      if(active&&weight>0){
        var box=ChaoProneToyPose051.worldBounds(anchor,bounds,"skull");float clearance=(ChaoProneToyPose051.FLOOR_Y-box[4])*16;minHeadFloor=Math.min(minHeadFloor,clearance);check(clearance>=.032f-.0001f,"Visible head/hat clips floor: "+context);
        check(ChaoProneToyPose051.separation(anchor,bounds)>=-.00001f,"Visible head/hat clips torso: "+context);
      }
      if(!active&&phase>=end&&rig.recoveringProne(id))for(String bone:List.of("body","skull","foot_left","foot_right","feet_bone_right","hand_left","hand_right")){
        if(!bone.startsWith("hand_")&&rig.pose(bone)==null)continue;
        var box=ChaoProneToyPose051.worldBounds(anchor,bounds,bone);if(box!=null)check(box[4]<=ChaoProneToyPose051.FLOOR_Y+.00001f,"Return/interruption clips floor: "+context+" "+bone);
      }
      if(weight==1){
        float lower=(origin.y-legacyY)*16;check(Math.abs(lower-(profile.oldLift-profile.lift))<.0003f,"Head was not lowered by previous surplus lift: "+context+" got="+lower);
        if(profile.kind.equals(ActiveToyPlayRules049.CAR)){minCarLower=Math.min(minCarLower,lower);maxCarLower=Math.max(maxCarLower,lower);}else{minCrayonLower=Math.min(minCrayonLower,lower);maxCrayonLower=Math.max(maxCrayonLower,lower);}
        if(hat)hatFrames++;else{float gap=faceDistance(surface(rig,"skull__cube_0"),surface(rig,"body__cube_0"));check(Float.isFinite(gap)&&gap<=profile.maxGap,"Visible base head floats away from torso: "+context+" faceGapPixels="+gap);maxGap=Math.max(maxGap,gap);contacts++;}
      }
      check(saved.equals(state.encode()),"Prone renderer mutated saved state: "+context);
    }
  }
  public static void run()throws Exception{
    var clips=JsonParser.parseString(Files.readString(ROOT.resolve("animations/official/chao.animation.json"))).getAsJsonObject().getAsJsonObject("animations");int serial=0;
    for(float alignment:new float[]{-1,0,1})for(int first=0;first<=4;first++)for(float rp:new float[]{-1,0,1})for(float sf:new float[]{-1,0,1})for(float age:new float[]{0,.5f,1}){
      var state=state(true,alignment,first,rp,sf,age);states++;for(var p:PROFILES)profile(clips,state,p,false,++serial);
    }
    for(float alignment:new float[]{-1,0,1})for(float rp:new float[]{-1,0,1})for(float sf:new float[]{-1,0,1})for(float age:new float[]{0,.5f,1}){
      var state=state(false,alignment,0,rp,sf,age);states++;for(var p:PROFILES)profile(clips,state,p,false,++serial);
    }
    for(float alignment:new float[]{-1,0,1})for(int first=0;first<=4;first++)for(float rp:new float[]{-.5f,.5f})for(float sf:new float[]{-.5f,.5f}){
      var state=state(true,alignment,first,rp,sf,.5f);states++;for(var p:PROFILES)profile(clips,state,p,false,++serial);
    }
    for(float alignment:new float[]{-1,0,1})for(int first=0;first<=4;first++)for(var p:PROFILES)profile(clips,state(true,alignment,first,0,0,1),p,true,++serial);
    // Actual crawling baby starts/returns, including early and developed Run
    // silhouettes. Also interrupt mature first-type families at full presence.
    for(float alignment:new float[]{-1,0,1})for(float rp:new float[]{-1,0,1})for(float sf:new float[]{-1,0,1}){
      var state=state(false,alignment,0,rp,sf,.5f);states++;for(var p:PROFILES)profile(clips,state,p,false,++serial,160,OfficialChaoRig.Motion.CRAWL);
    }
    for(float alignment:new float[]{-1,0,1})for(int first=0;first<=4;first++){
      var state=state(true,alignment,first,-1,1,1);states++;for(var p:PROFILES)profile(clips,state,p,false,++serial,80,OfficialChaoRig.Motion.CRAWL);
    }
    System.out.println("Prone head connection073: "+checks+" checks; "+states+" actual state-selected adult/baby configurations; "+frames+" production entry/activity/exit frames; "+contacts+" emitted core face contacts; "+hatFrames+" fitted-hat contacts; "+attachmentFrames+" head-child matrices; maxVisibleNeckGapPixels="+maxGap+" maxQuarterHeadStepPixels="+maxHeadStep+" maxExitHeadStepPixels="+maxExitStep+" minHeadFloorPixels="+minHeadFloor+" carLowerPixels="+minCarLower+".."+maxCarLower+" crayonLowerPixels="+minCrayonLower+".."+maxCrayonLower);
  }
  public static void main(String[]args)throws Exception{run();}
}
