package com.chaogarden.client;

import com.chaogarden.core.ChaoState;
import com.chaogarden.core.LocomotionRules;
import com.chaogarden.toys.JackPlayRules080;
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

/** Actual shared jack rig: signed family corners, box-to-reveal gaze, entry,
 * interrupted exit, repeated loop boundaries and representative attachments. */
public final class JackToyRigTest080 {
  private static final Path ROOT=Path.of("src/main/resources/assets/chao_garden/models/entity");
  private static final List<String> JOINTS=List.of("body","skull","arm_left","arm_right","foot_left","foot_right","feet_bone_right");
  private static final Map<String,ChaoMeshAsset> ASSETS=new HashMap<>();
  private static JsonObject clips;
  private static long checks,frames,headChildren,attachmentCopies;
  private static int cases,episodes,exits;
  private static float maxStep,maxFloorError,minHeadFloor=Float.POSITIVE_INFINITY,minHandHead=Float.POSITIVE_INFINITY,maxAnchorError;
  private record Named(String name,ChaoState state){}
  private static void check(boolean ok,String why){checks++;if(!ok)throw new AssertionError(why);}
  private static ChaoMeshAsset load(String key){
    return ASSETS.computeIfAbsent(key,k->{try{return ChaoMeshAsset.parse(JsonParser.parseString(Files.readString(ROOT.resolve(k+".json"))).getAsJsonObject());}catch(Exception e){throw new IllegalStateException(k,e);}});
  }
  private static OfficialChaoRig rig(){return new OfficialChaoRig(clips,JackToyRigTest080::load);}
  private static Matrix4f at(OfficialChaoRig rig,String name){var s=new PoseStack();rig.transformBone(name,s);return new Matrix4f(s.last().pose());}
  private static float[] data(ChaoMeshAsset.Pose p){return new float[]{p.x,p.y,p.z,p.rx,p.ry,p.rz,p.sx,p.sy,p.sz,p.reach,p.proportionY};}
  private static ChaoState state(boolean adult,int alignment,int first,int rp,int sf,float maturity){
    var s=new ChaoState();s.adult=adult;s.align=alignment;s.primaryType=first;s.setEvolutionStrength(maturity*1.2f);
    if(adult){s.levels[ChaoState.RUN]=12;s.levels[ChaoState.POWER]=12;}
    if(rp!=0){int stat=rp<0?ChaoState.RUN:ChaoState.POWER;int total=s.levels[stat]*100+s.progress[stat]+960;s.levels[stat]=total/100;s.progress[stat]=total%100;}
    if(sf!=0){int stat=sf<0?ChaoState.SWIM:ChaoState.FLY;s.levels[stat]=9;s.progress[stat]=60;}
    return ChaoState.decode(s.encode());
  }
  private static List<Named> states(){
    var out=new ArrayList<Named>();
    for(int align=-1;align<=1;align++)for(int first=0;first<=4;first++)for(float age:new float[]{0,.5f,1}){
      int rp=(first+align+4)%3-1,sf=(first*2+align+4)%3-1;
      out.add(new Named(align+"/"+first+"/"+rp+"/"+sf+"/"+age,state(true,align,first,rp,sf,age)));
    }
    for(int rp=-1;rp<=1;rp++)for(int sf=-1;sf<=1;sf++)out.add(new Named("mixed/"+rp+"/"+sf,state(true,0,3,rp,sf,1)));
    for(int align=-1;align<=1;align++)for(boolean trained:new boolean[]{false,true}){
      var s=state(false,align,0,0,0,.65f);if(trained)s.levels[ChaoState.RUN]=12;
      out.add(new Named("baby/"+align+"/trained="+trained,s));
    }
    for(String character:List.of("tails","knuckles","amy","omochao"))for(boolean adult:new boolean[]{false,true}){
      var s=new ChaoState();s.character=character;s.adult=adult;s.levels[ChaoState.RUN]=12;out.add(new Named(character+"/"+adult,s));
    }
    return out;
  }
  private static Map<String,float[]> snapshot(OfficialChaoRig rig){
    var out=new HashMap<String,float[]>();for(String n:JOINTS)if(rig.pose(n)!=null)out.put(n,data(rig.pose(n)));return out;
  }
  private static Map<String,Matrix4f> headChildren(OfficialChaoRig rig){
    var out=new HashMap<String,Matrix4f>();var inverse=at(rig,"skull").invert();
    for(String n:List.of("skull__cube_0","skull__cube_1","expression_mouth_1","emotion_dots"))if(rig.pose(n)!=null)out.put(n,new Matrix4f(inverse).mul(at(rig,n)));
    return out;
  }
  private static void anatomy(OfficialChaoRig rig,ChaoState state,Map<String,float[]> before,Map<String,Matrix4f> children,String label){
    for(var e:before.entrySet()){
      float[] old=e.getValue(),now=data(rig.pose(e.getKey()));boolean rotates=e.getKey().equals("skull")||e.getKey().startsWith("arm_");
      for(int i=0;i<old.length;i++)if(!rotates||i<3||i>5)check(old[i]==now[i],"Jack cannot translate, stretch or reshape a joint "+label+" "+e.getKey()+" field="+i);
    }
    var core=rig.sampledMesh().partBounds("skull__cube_0",rig.poses());
    if(state.character.equals("normal")&&!state.immortal)check(core!=null&&Math.abs(core[3]-core[0]-7)<.00001f&&Math.abs(core[4]-core[1]-6)<.00001f&&Math.abs(core[5]-core[2]-6)<.00001f,"Original7x6x6 head remains fixed "+label);
    if(children!=null){var inverse=at(rig,"skull").invert();for(var e:children.entrySet()){check(new Matrix4f(inverse).mul(at(rig,e.getKey())).equals(e.getValue(),.00005f),"Head/face/orb child detached "+label+" "+e.getKey());headChildren++;}}
  }
  private static float bottom(OfficialChaoRig rig,String name,float[] box){
    var matrix=at(rig,name);float result=Float.NEGATIVE_INFINITY;
    for(int n=0;n<8;n++)result=Math.max(result,matrix.transformPosition(new Vector3f(box[(n&1)==0?0:3],box[(n&2)==0?1:4],box[(n&4)==0?2:5]).div(16)).y*16);
    return result;
  }
  /** Conservative separation of the actual visible palm envelope in skull space. */
  private static float handHead(OfficialChaoRig rig,String hand,float[] head){
    String palm=rig.pose(hand+"__cube_1")!=null?hand+"__cube_1":hand;
    var box=rig.sampledMesh().partBounds(palm,rig.poses());if(box==null)return Float.POSITIVE_INFINITY;
    var matrix=new Matrix4f(at(rig,"skull")).invert().mul(at(rig,palm));var low=new Vector3f(Float.POSITIVE_INFINITY);var high=new Vector3f(Float.NEGATIVE_INFINITY);
    for(int n=0;n<8;n++){var p=matrix.transformPosition(new Vector3f(box[(n&1)==0?0:3],box[(n&2)==0?1:4],box[(n&4)==0?2:5]).div(16)).mul(16);low.min(p);high.max(p);}
    return Math.max(Math.max(head[0]-high.x,low.x-head[3]),Math.max(Math.max(head[1]-high.y,low.y-head[4]),Math.max(head[2]-high.z,low.z-head[5])));
  }
  private static void episode(Named named,float cancel,boolean loop,boolean moving,float step,int serial){
    var s=named.state;String saved=s.encode();var rig=rig();var id=new UUID(800080,serial);var previous=new HashMap<String,Vector3f>();
    boolean crawl=LocomotionRules.groundGait(s)==LocomotionRules.GroundGait.CRAWL;float yaw=serial%2==0?20:-20,pitch=serial%2==0?65:40;
    int samples=Math.round((cancel+36)/step);episodes++;
    for(int q=0;q<=samples;q++){
      float t=q*step-20,age=t+20,phase=loop&&t>=0?t%160:t;boolean active=t>=0&&t<cancel;
      var idle=crawl?OfficialChaoRig.Motion.SIT:OfficialChaoRig.Motion.WALK;
      var motion=active&&(JackPlayRules080.seated(phase)||crawl)?OfficialChaoRig.Motion.SIT:idle;
      String model=OfficialChaoRig.geometryKey(s);rig.prepareDevelopment(model,s);
      rig.animateGameplay(model,id,motion,age,.035f*age,moving&&t<0?.06f:0,0,0,
          ChaoJackAnimation080.gazeYaw(active,yaw),ChaoJackAnimation080.gazePitch(active,pitch),false,false);rig.development(s);
      var before=snapshot(rig);var children=q%16==0?headChildren(rig):null;
      if(active)ChaoJackAnimation080.pose(rig,phase);
      anatomy(rig,s,before,children,named.name+" t="+t);rig.finishGameplayFrame(id,active?51*1024:0,age);frames++;
      check(rig.finite(),"Finite shared production pose "+named.name+" t="+t);
      for(String n:JOINTS){if(rig.pose(n)==null)continue;var p=at(rig,n).transformPosition(new Vector3f());var old=previous.put(n,p);if(old!=null){float pixels=p.distance(old)*16;maxStep=Math.max(maxStep,pixels);check(pixels<1.4f*step+.0001f,"No joint pop through entry/loop/exit "+named.name+" "+n+" t="+t+" step="+pixels);}}
      var head=rig.attachmentBounds("skull");if(head!=null){float floor=24-bottom(rig,"skull",head);minHeadFloor=Math.min(minHeadFloor,floor);check(floor>2,"Head stays above floor at actual box-to-reveal gaze "+named.name+" t="+t);}
      for(String n:ChaoSittingPose.FEET){var box=rig.sampledMesh().partBounds(n,rig.poses());if(box==null)continue;float y=bottom(rig,n,box);if(active&&phase>=28&&phase<132){float error=Math.abs(24-y);maxFloorError=Math.max(maxFloorError,error);check(error<.08f,"Jack keeps seated feet grounded "+named.name+" "+n+" t="+t+" error="+error);}}
      if(active&&phase>=28&&phase<132&&q%16==0&&head!=null)for(String hand:List.of("arm_left","arm_right")){float gap=handHead(rig,hand,head);minHandHead=Math.min(minHandHead,gap);check(gap>=-.02f,"Relaxed hands enter head "+named.name+" "+hand+" t="+t+" gap="+gap);}
      if(t>=cancel&&t<cancel+8)exits++;
      if(t>=cancel+8){
        check(!rig.recoveringProne(id),"Jack must not set a prone-recovery flag");
        for(String n:List.of("body","skull","arm_left","arm_right"))if(rig.pose(n)!=null){float[] actual=data(rig.pose(n)),target=before.get(n);for(int i=0;i<11;i++)check(Math.abs(actual[i]-target[i])<.00001f,"Ordinary pose restored after8ticks "+named.name+" "+n+" field="+i);}
      }
    }
    check(saved.equals(s.encode()),"Jack animation cannot mutate care/age/locks/training/color "+named.name);
  }
  /** Real legacy attachment rigs inherit the exact transformed semantic joint.
   * This checks binding, not every hat or animal mesh's collision envelope. */
  private static void attachments(ChaoState state,int serial)throws Exception{
    var rig=rig();var id=new UUID(800081,serial);String model=OfficialChaoRig.geometryKey(state);
    for(float t:new float[]{0,28,42,76,116,139,160}){
      rig.prepareDevelopment(model,state);rig.animateGameplay(model,id,OfficialChaoRig.Motion.SIT,t,0,0,0,0,0,8,false,false);rig.development(state);ChaoJackAnimation080.pose(rig,t);
      String source=rig.sourceGeometryKey();
      for(String folder:List.of("hats/","overlays/")){
        var asset=load(folder+"chao_"+source);var poses=asset.createPoses();
        for(var e:poses.entrySet())if(rig.pose(e.getKey())!=null)rig.copyAttachmentPoseTo(e.getKey(),asset,e.getValue());
        for(String joint:List.of("skull","arm_left","arm_right","foot_left")){
          if(rig.pose(joint)==null||!poses.containsKey(joint))continue;
          var at=new PoseStack();asset.transformBone(joint,at,poses);var delta=new Matrix4f(at(rig,joint)).invert().mul(at.last().pose());float error=new Vector3f(delta.m30(),delta.m31(),delta.m32()).length()*16;
          maxAnchorError=Math.max(maxAnchorError,error);check(error<.0001f,"Hat/animal semantic anchor shifted "+model+" "+folder+joint+" error="+error);attachmentCopies++;
        }
      }
    }
  }
  private static void facePriority(){
    var state=new ChaoState();state.setBaselineFace076(1,2);
    var baseline=com.chaogarden.core.ChaoFaceRules076.resolve(state,com.chaogarden.core.ChaoFaceRules076.Signals.quiet());
    for(float t:new float[]{0,39,65,120,160})check(ChaoJackAnimation080.face(baseline,true,t).equals(baseline),"Baseline outside surprise returns unchanged");
    var surprised=ChaoJackAnimation080.face(baseline,true,48);
    check(surprised.eyes()==com.chaogarden.core.ChaoFaceRules076.Eyes.SURPRISED&&surprised.mouth()==com.chaogarden.core.ChaoFaceRules076.Mouth.OPEN,"Visible reveal has actual temporary face");
    check(surprised.baselineEyes()==baseline.baselineEyes()&&surprised.baselineMouth()==baseline.baselineMouth(),"Transient face cannot reroll identity");
    for(var source:com.chaogarden.core.ChaoFaceRules076.Source.values()){
      if(java.util.List.of(com.chaogarden.core.ChaoFaceRules076.Source.BASELINE,com.chaogarden.core.ChaoFaceRules076.Source.MOOD,com.chaogarden.core.ChaoFaceRules076.Source.BLINK,com.chaogarden.core.ChaoFaceRules076.Source.IDLE).contains(source))continue;
      var high=new com.chaogarden.core.ChaoFaceRules076.Resolved(1,2,baseline.eyes(),baseline.mouth(),baseline.style(),source,source,false,0);
      check(ChaoJackAnimation080.face(high,true,48).equals(high),"Higher priority source remains intact: "+source);
    }
  }
  public static void main(String[] args)throws Exception{
    clips=JsonParser.parseString(Files.readString(ROOT.getParent().getParent().resolve("animations/official/chao.animation.json"))).getAsJsonObject().getAsJsonObject("animations");
    check(ChaoJackAnimation080.gazePitch(false,65)==65&&ChaoJackAnimation080.gazeYaw(false,20)==20,"Other activities retain ordinary gaze");
    check(ChaoJackAnimation080.gazePitch(true,65)==12&&ChaoJackAnimation080.gazeYaw(true,-20)==-12,"Actual box-to-reveal gaze remains calm and bounded");
    facePriority();
    int serial=0;for(var named:states()){cases++;episode(named,74,false,false,.5f,++serial);episode(named,160,false,false,.5f,++serial);}
    for(int align=-1;align<=1;align++)for(int first=0;first<=4;first++){
      var s=state(true,align,first,-1,1,1);var named=new Named("loop/"+align+"/"+first,s);episode(named,8,false,true,.25f,++serial);episode(named,330,true,true,.25f,++serial);attachments(s,serial);
    }
    for(int align=-1;align<=1;align++){var s=state(false,align,0,0,0,.65f);episode(new Named("baby-loop/"+align,s),330,true,true,.25f,++serial);attachments(s,serial);}
    for(float invalid:new float[]{Float.NaN,Float.POSITIVE_INFINITY,Float.NEGATIVE_INFINITY,-1,160,200}){
      var rig=rig();var state=state(false,0,0,0,0,0);String model=OfficialChaoRig.geometryKey(state);rig.prepareDevelopment(model,state);rig.animateGameplay(model,new UUID(800082,serial++),OfficialChaoRig.Motion.SIT,50,0,0,0,0,0,0,false,false);var before=snapshot(rig);ChaoJackAnimation080.pose(rig,invalid);for(var e:before.entrySet())check(Arrays.equals(e.getValue(),data(rig.pose(e.getKey()))),"Invalid/outside clock cannot disturb pose "+invalid);
    }
    System.out.println("Jack shared rig080: "+checks+" checks; "+cases+" anatomy/stage/axis cases, "+episodes+" entry/interruption/loop episodes, "+frames+" sampledframes, "+exits+" exit samples; maxsamplejointstepPixels="+maxStep+", maxseatedfloorerrorPixels="+maxFloorError+", minheadfloorPixels="+minHeadFloor+", minhandheadPixels="+minHandHead+", headchildmatrices="+headChildren+", attachmentcopies="+attachmentCopies+", maxattachmentanchorerrorPixels="+maxAnchorError+". Original head/limb shape and saved state preserved. Numerical checks do not replace actual client visual/listening/multiplayer review.");
  }
}
