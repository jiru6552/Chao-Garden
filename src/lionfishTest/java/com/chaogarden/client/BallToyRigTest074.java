package com.chaogarden.client;

import com.chaogarden.core.ChaoState;
import com.chaogarden.core.LocomotionRules;
import com.chaogarden.toys.BallPlayRules074;
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
import org.joml.Matrix4f;
import org.joml.Vector3f;

/** Actual shared-rig contact, rigid-part preservation and repeated-action recovery.
 * This numerical domain complements native routing and actual client observations. */
public final class BallToyRigTest074 {
  private static final Path ROOT=Path.of("src/main/resources/assets/chao_garden");
  private static final Map<String,ChaoMeshAsset> ASSETS=new HashMap<>();
  private static JsonObject clips;
  private static float[][] ball;
  private static long checks,frames,contacts,episodes,headChildren,hiddenFootProfiles,hiddenFootFrames;
  private static float maxContactGap,maxHeadPixels,maxToeAngle,maxQuarterStep,minHeadGap=Float.POSITIVE_INFINITY,minFloor=Float.POSITIVE_INFINITY;
  private record Named(String name,ChaoState state){}
  private static void check(boolean okay,String message){checks++;if(!okay)throw new AssertionError(message);}
  private static ChaoMeshAsset load(String key){return ASSETS.computeIfAbsent(key,k->{try{return ChaoMeshAsset.parse(JsonParser.parseString(Files.readString(ROOT.resolve("models/entity/"+k+".json"))).getAsJsonObject());}catch(Exception e){throw new IllegalStateException(k,e);}});}
  private static ChaoState state(boolean adult,int alignment,int first,float rp,float sf,float maturity,boolean walking){
    var s=new ChaoState();s.adult=adult;s.align=alignment;s.primaryType=first;s.setEvolutionStrength(maturity*(adult?1.2f:.8f));
    int ri=rp<0?ChaoState.RUN:ChaoState.POWER,fi=sf<0?ChaoState.SWIM:ChaoState.FLY;
    int r=Math.round(Math.abs(rp)*(adult?960:480)),f=Math.round(Math.abs(sf)*(adult?960:480));
    s.levels[ri]=r/100;s.progress[ri]=r%100;s.levels[fi]=f/100;s.progress[fi]=f%100;s.points[ChaoState.RUN]=walking?50:0;
    return ChaoState.decode(s.encode());
  }
  private static List<Named> states(){
    var list=new ArrayList<Named>();
    for(boolean adult:new boolean[]{false,true})for(int alignment=-1;alignment<=1;alignment++)for(int first=0;first<=(adult?4:0);first++)for(int rp=-1;rp<=1;rp++)for(int sf=-1;sf<=1;sf++)for(float age:new float[]{0,.5f,1})for(boolean walking:new boolean[]{false,true})
      list.add(new Named(adult+"/"+alignment+"/"+first+"/"+rp+"/"+sf+"/"+age+"/"+walking,state(adult,alignment,first,rp,sf,age,walking)));
    for(int alignment=-1;alignment<=1;alignment++)for(int first=0;first<=4;first++)for(float age:new float[]{.25f,.75f})for(float rp:new float[]{-.5f,.5f})
      list.add(new Named("interior/"+alignment+"/"+first+"/"+age+"/"+rp,state(true,alignment,first,rp,-rp,age,true)));
    return list;
  }
  private static OfficialChaoRig rig(){return new OfficialChaoRig(clips,BallToyRigTest074::load);}
  private static Matrix4f at(OfficialChaoRig rig,String bone){var p=new PoseStack();rig.transformBone(bone,p);return new Matrix4f(p.last().pose());}
  private static float[] invariant(ChaoMeshAsset.Pose p){return new float[]{p.sx,p.sy,p.sz,p.reach,p.proportionY};}
  private static void prepare(OfficialChaoRig rig,ChaoState s,UUID id,OfficialChaoRig.Motion motion,float age,float movement,int token){
    String key=OfficialChaoRig.geometryKey(s);rig.prepareDevelopment(key,s);
    // Ball focus owns a neutral head orientation even if external LookControl
    // input changes; ordinary gaze remains the existing renderer's behavior.
    rig.animateGameplay(key,id,motion,age,.035f*age,movement,0,0,ChaoBallAnimation074.gazeYaw(token!=0,s,23),ChaoBallAnimation074.gazePitch(token!=0,17),false,false);rig.development(s);rig.finishGameplayFrame(id,token,age);
  }
  private static void anatomy(OfficialChaoRig rig,ChaoState s,Map<String,float[]> before,Map<String,Matrix4f> children,String context){
    for(var e:before.entrySet())check(Arrays.equals(e.getValue(),invariant(rig.pose(e.getKey()))),"Ball must not resize/extend a part "+context+" "+e.getKey());
    float[] core=rig.sampledMesh().partBounds("skull__cube_0",rig.poses());
    check(core!=null&&Math.abs(core[3]-core[0]-7)<.00001f&&Math.abs(core[4]-core[1]-6)<.00001f&&Math.abs(core[5]-core[2]-6)<.00001f,"Fixed original7x6x6 head "+context);
    var inverse=at(rig,"skull").invert();for(var e:children.entrySet()){check(new Matrix4f(inverse).mul(at(rig,e.getKey())).equals(e.getValue(),.00004f),"Head child moved independently "+context+" "+e.getKey());headChildren++;}
    check(rig.finite(),"Finite production pose "+context);
  }
  private static Map<String,float[]> invariants(OfficialChaoRig rig){var out=new HashMap<String,float[]>();rig.poses().forEach((n,p)->out.put(n,invariant(p)));return out;}
  private static Map<String,Matrix4f> children(OfficialChaoRig rig){var out=new HashMap<String,Matrix4f>();var inverse=at(rig,"skull").invert();for(String name:List.of("skull__cube_0","expression_mouth_1","emotion_dots"))if(rig.pose(name)!=null)out.put(name,new Matrix4f(inverse).mul(at(rig,name)));return out;}
  private static final class Surface implements VertexConsumer {
    final List<Vector3f> points=new ArrayList<>();final float size;Surface(float size){this.size=size;}
    public VertexConsumer addVertex(float x,float y,float z){points.add(new Vector3f(x*size,(1.5f-y)*size,-z*size));return this;}
    public VertexConsumer setColor(int r,int g,int b,int a){return this;}public VertexConsumer setUv(float u,float v){return this;}public VertexConsumer setUv1(int u,int v){return this;}public VertexConsumer setUv2(int u,int v){return this;}public VertexConsumer setNormal(float x,float y,float z){return this;}
  }
  private static void actualCuboid(OfficialChaoRig rig,ChaoState s,String bone,String context){
    var visible=new HashMap<String,Boolean>();rig.poses().forEach((n,p)->{visible.put(n,p.visible);p.visible=n.equals(bone)||n.startsWith(bone+"__");});
    Surface emitted=new Surface(ChaoBallAnimation074.scale(s));try{rig.sampledMesh().render(new PoseStack(),emitted,0xf000f0,0,-1,rig.poses(),true);}finally{rig.poses().forEach((n,p)->p.visible=visible.get(n));}
    var corners=ChaoBallAnimation074.part(rig,bone,ChaoBallAnimation074.scale(s));
    check(emitted.points.size()>=24,"Actual contact faces emitted "+context);
    for(var corner:corners){float nearest=Float.POSITIVE_INFINITY;for(var point:emitted.points)nearest=Math.min(nearest,point.distance(corner));check(nearest<.00002f,"Contact box corner must exist in visible surface, not hidden geometry "+context+" "+bone+" gap="+nearest);}
  }
  private static float minHeight(Vector3f[] points){float result=Float.POSITIVE_INFINITY;for(var p:points)result=Math.min(result,p.y);return result;}
  private static void contact(Named named,String hat,int serial){
    var s=named.state;String saved=s.encode(),context=named.name+" hat="+hat;var rig=rig();var id=new UUID(740074,serial);
    var mode=ChaoBallAnimation074.nudge(s)?OfficialChaoRig.Motion.CRAWL:OfficialChaoRig.Motion.WALK;
    prepare(rig,s,id,mode,80,0,42*1024);var before=invariants(rig);var children=children(rig);
    float headRest=ChaoBallAnimation074.contactSeparation(ChaoBallAnimation074.head(rig,s,hat),ball,(float)ChaoBallAnimation074.contactDistance(s,hat));minHeadGap=Math.min(minHeadGap,headRest);check(headRest>=-.00003f,"Parked ball enters head/headwear "+context+" gap="+headRest);
    var fit=ChaoBallAnimation074.pose(rig,s,hat,BallPlayRules074.CONTACT_TICK,ball);check(fit.solved(),"Bounded rigid contact not reached "+context+" fit="+fit);
    check(fit.remainingSeparation()>=-.00003f&&fit.remainingSeparation()<.0001f,"Actual first-contact gap "+context+" "+fit.remainingSeparation());maxContactGap=Math.max(maxContactGap,fit.remainingSeparation());
    if(ChaoBallAnimation074.nudge(s)){maxHeadPixels=Math.max(maxHeadPixels,fit.rigidAmount());check(fit.rigidAmount()<=1.00001f,"No extended neck "+context);}else{maxToeAngle=Math.max(maxToeAngle,fit.rigidAmount());check(fit.rigidAmount()<=.80001f,"Small rigid toe range "+context);}
    anatomy(rig,s,before,children,context);
    if(hat.isBlank())actualCuboid(rig,s,ChaoBallAnimation074.nudge(s)?"skull__cube_0":rig.pose("foot_right")!=null?"foot_right":"feet_bone_right",context);
    for(String foot:ChaoSittingPose.FEET)if(rig.pose(foot)!=null){float y=minHeight(ChaoBallAnimation074.part(rig,foot,ChaoBallAnimation074.scale(s)));minFloor=Math.min(minFloor,y);check(y>=-.0001f,"Foot floor contact "+context+" "+foot+" y="+y);}
    check(saved.equals(s.encode()),"Numerical pose changed saved care/age/stats "+context);contacts++;
  }
  /** Replacement overlays inherit a hidden ankle. This verifies that the
   * gesture survives, with base-envelope clearance explicitly kept separate
   * from an exact replacement-mesh contact claim. */
  private static void hiddenFootGesture(ChaoState s,int animal,int serial){
    check((ChaoState.animalSlots(animal)&(1<<ChaoState.FEET))!=0,"Chosen animal has replacement feet");
    s.animalParts[ChaoState.FEET]=animal;String saved=s.encode();var rig=rig();var id=new UUID(740076,serial);
    String context="hidden-base-foot/"+s.align+"/"+s.primaryType+"/"+s.evolutionStrength()+" animal="+animal;
    for(int tick=0;tick<=BallPlayRules074.ACTION_TICKS;tick++){
      prepare(rig,s,id,OfficialChaoRig.Motion.WALK,80+tick,0,42*1024);
      for(String foot:ChaoSittingPose.FEET)if(rig.pose(foot)!=null)rig.hidePart(foot);
      String foot=rig.pose("foot_right")!=null?"foot_right":"feet_bone_right";
      check(rig.sampledMesh().partBounds(foot,rig.poses())==null,"Profile genuinely hides the source foot "+context);
      var visibility=new HashMap<String,Boolean>();rig.poses().forEach((n,p)->visibility.put(n,p.visible));
      var before=invariants(rig);var children=children(rig);float prior=rig.pose(foot).rx;
      var partBefore=at(rig,foot);var tipBefore=partBefore.transformDirection(new Vector3f(0,0,-1));
      var fit=ChaoBallAnimation074.pose(rig,s,"",tick,ball);
      check(fit.solved(),"Hidden source envelope must still produce a finite rigid gesture "+context+" tick="+tick);
      anatomy(rig,s,before,children,context+" tick="+tick);
      for(var e:visibility.entrySet())check(rig.pose(e.getKey()).visible==e.getValue(),"Ball cannot reveal a hidden skin foot or hide its overlay "+context+" "+e.getKey());
      check(ChaoBallAnimation074.part(rig,foot,ChaoBallAnimation074.scale(s)).length==8,"Base attachment fallback exists "+context);
      if(tick==BallPlayRules074.CONTACT_TICK){
        check(Math.abs(rig.pose(foot).rx-prior)>.02f,"Hidden foot's ankle must actually rotate "+context);
        check(at(rig,foot).transformDirection(new Vector3f(0,0,-1)).distance(tipBefore)>.02f,"Replacement overlay's inherited ankle transform must move "+context);
        check(fit.remainingSeparation()>=-.00003f&&fit.remainingSeparation()<.0001f,"Base-envelope approximation has bounded contact "+context);
      }
      for(String name:ChaoSittingPose.FEET)if(rig.pose(name)!=null)check(minHeight(ChaoBallAnimation074.part(rig,name,ChaoBallAnimation074.scale(s)))>=-.0001f,"Base-envelope sole clearance "+context);
      check(saved.equals(s.encode()),"Replacement foot preview cannot edit state or equipment "+context);
      rig.rememberGameplayFrame(id);hiddenFootFrames++;
    }hiddenFootProfiles++;
  }
  private static void episode(ChaoState s,String hat,boolean blocked,float cancel,int serial){
    var rig=rig();var id=new UUID(740075,serial);var previous=new HashMap<String,Vector3f>();String context="episode="+serial+" baby="+!s.adult+" type="+s.primaryType+" align="+s.align+" hat="+hat+" blocked="+blocked+" cancel="+cancel;
    for(int q=-32;q<=4*196;q++){
      float t=q*.25f,age=t+20;boolean active=t>=0&&t<cancel;
      float action=t<20?t:t>=40&&t<60?t-40:t>=150&&t<170?t-150:-1;
      // The150..170 action explicitly crosses a160tick preview loop renewal.
      if(action<0||action>=20)action=-1;
      var ordinary=ChaoBallAnimation074.crawler(s)?OfficialChaoRig.Motion.SIT:OfficialChaoRig.Motion.WALK;
      var motion=active&&(ChaoBallAnimation074.crawler(s)||action>=0&&ChaoBallAnimation074.nudge(s))?OfficialChaoRig.Motion.CRAWL:ordinary;
      prepare(rig,s,id,motion,age,0,active?42*1024:0);var before=invariants(rig);var children=children(rig);
      if(active)ChaoBallAnimation074.pose(rig,s,hat,action,ball);
      else if(t>=cancel&&t<cancel+8)ChaoBallAnimation074.clearFootFloor(rig,s);
      if(active)rig.rememberGameplayFrame(id);
      anatomy(rig,s,before,children,context+" t="+t);frames++;
      float head=ChaoBallAnimation074.contactSeparation(ChaoBallAnimation074.head(rig,s,hat),ball,(float)ChaoBallAnimation074.contactDistance(s,hat));
      // Blocked pushes retain their original ball; this is also the closest
      // possible ball during entry/recovery. A successful roll moves it away.
      if(blocked||t<BallPlayRules074.CONTACT_TICK)check(head>=-.00005f,"Head/hat clips unmoved ball through entry/blocked recovery "+context+" t="+t+" gap="+head);
      if(active||t>=cancel&&t<cancel+8)for(String foot:ChaoSittingPose.FEET)if(rig.pose(foot)!=null){float floor=minHeight(ChaoBallAnimation074.part(rig,foot,ChaoBallAnimation074.scale(s)));check(floor>=-.0001f,"Active/outgoing sole clips floor "+context+" t="+t+" foot="+foot+" y="+floor);}
      if(t>=cancel+8)check(Math.abs(rig.pose("skull").ry)>.05f,"Ordinary external head gaze restored after cleanup "+context+" t="+t);
      if(active&&action>=8&&action<=10){String part=ChaoBallAnimation074.nudge(s)?"skull__cube_0":rig.pose("foot_right")!=null?"foot_right":"feet_bone_right";check(rig.pose(part)!=null,"Real contact part present "+context);}
      for(String name:List.of("skull","body","arm_left","arm_right","foot_left","foot_right","feet_bone_right")){
        if(rig.pose(name)==null)continue;var p=at(rig,name).transformPosition(new Vector3f());var prior=previous.put(name,p);
        if(prior!=null){float step=prior.distance(p)*16;maxQuarterStep=Math.max(maxQuarterStep,step);check(step<1.25f,"Quarter-tick compound pose pop "+context+" t="+t+" joint="+name+" step="+step);}
      }
    }episodes++;
  }
  public static void main(String[] args)throws Exception{
    clips=JsonParser.parseString(Files.readString(ROOT.resolve("animations/official/chao.animation.json"))).getAsJsonObject().getAsJsonObject("animations");ball=ChaoBallAnimation074.parseGeometry(JsonParser.parseString(Files.readString(ROOT.resolve("models/block/chao_ball_idle.json"))).getAsJsonObject());check(ball.length==23,"Actual shipped23cuboid ball contact geometry");
    int serial=0;for(var named:states())for(String hat:ChaoBallAnimation074.nudge(named.state)?List.of("","vanilla_carved_pumpkin","eggshell_hat"):List.of(""))contact(named,hat,++serial);
    for(int align=-1;align<=1;align++)for(int first=0;first<=4;first++)for(float age:new float[]{0,1})for(int animal:new int[]{0,6,9})hiddenFootGesture(state(true,align,first,.5f,-.5f,age,true),animal,++serial);
    for(int align=-1;align<=1;align++)for(int first=0;first<=4;first++)for(boolean walking:new boolean[]{false,true}){
      var s=state(true,align,first,.5f,-.5f,.5f,walking);episode(s,"",true,184,++serial);
    }
    for(int align=-1;align<=1;align++)for(boolean walking:new boolean[]{false,true})for(String hat:List.of("","vanilla_carved_pumpkin","eggshell_hat")){
      var s=state(false,align,0,.5f,-.5f,.5f,walking);episode(s,hat,true,184,++serial);episode(s,hat,false,8,++serial);episode(s,hat,false,12,++serial);
    }
    System.out.println("Ball rigid contact074: "+checks+" checks; "+contacts+" state/contact profiles, "+episodes+" repeated-action/loop/cancel episodes, "+frames+" sampledquarterframes; maxcontactSATgap="+maxContactGap+" blocks, maxrigidheadshift="+maxHeadPixels+" modelpixels, maxtoerotation="+maxToeAngle+" radians, maxquarterjointstep="+maxQuarterStep+" modelpixels, minparkedheadgap="+minHeadGap+" blocks, mincontactfootfloor="+minFloor+" blocks, headchildmatrices="+headChildren+"; hidden-foot base-envelope approximation profiles="+hiddenFootProfiles+", frames="+hiddenFootFrames+" (exact replacement-mesh contact is not claimed). Numerical bounds do not replace actual in-game motion/listening/multiplayer review.");
  }
}
