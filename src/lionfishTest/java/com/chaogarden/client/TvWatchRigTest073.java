package com.chaogarden.client;

import com.chaogarden.core.ChaoState;
import com.chaogarden.core.LocomotionRules;
import com.chaogarden.toys.TvWatchRules073;
import com.google.gson.JsonParser;
import com.mojang.blaze3d.vertex.PoseStack;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.joml.Vector3f;

/** Actual shared rig, all signed family corners, TV entry/exit and interruption. */
public final class TvWatchRigTest073 {
  private static final Path ROOT=Path.of("src/main/resources/assets/chao_garden/models/entity");
  private static int checks,frames,cases,exits;
  private static float maxStep,minHeadFloor=Float.POSITIVE_INFINITY;
  private record Named(String name,ChaoState state){}
  private static void check(boolean ok,String why){checks++;if(!ok)throw new AssertionError(why);}
  private static ChaoMeshAsset load(String key){try{return ChaoMeshAsset.parse(JsonParser.parseString(Files.readString(ROOT.resolve(key+".json"))).getAsJsonObject());}catch(Exception e){throw new IllegalStateException(key,e);}}
  private static float[] data(ChaoMeshAsset.Pose p){return new float[]{p.x,p.y,p.z,p.rx,p.ry,p.rz,p.sx,p.sy,p.sz,p.reach,p.proportionY};}
  private static List<Named> states(){
    var out=new ArrayList<Named>();
    for(int alignment=-1;alignment<=1;alignment++)for(int first=0;first<=4;first++)for(int rp=-1;rp<=1;rp++)for(int sf=-1;sf<=1;sf++)for(float maturity:new float[]{0,.5f,1}){
      var s=new ChaoState();s.adult=true;s.align=alignment;s.primaryType=first;s.setEvolutionStrength(maturity*1.2f);
      if(rp!=0){int stat=rp<0?ChaoState.RUN:ChaoState.POWER;s.levels[stat]=9;s.progress[stat]=60;}
      if(sf!=0){int stat=sf<0?ChaoState.SWIM:ChaoState.FLY;s.levels[stat]=9;s.progress[stat]=60;}
      out.add(new Named(alignment+"/"+first+"/"+rp+"/"+sf+"/"+maturity,ChaoState.decode(s.encode())));
    }
    for(float alignment:new float[]{-1,0,1}){var s=new ChaoState();s.align=alignment;s.setEvolutionStrength(.8f);out.add(new Named("baby/"+alignment,s));}
    for(String character:List.of("tails","knuckles","amy","omochao"))for(boolean adult:new boolean[]{false,true}){var s=new ChaoState();s.character=character;s.adult=adult;out.add(new Named(character+"/"+adult,s));}
    return out;
  }
  private static void screenGeometry()throws Exception{
    var vertices=new ArrayList<double[]>();var screen=new ArrayList<double[]>();String material="";
    for(String line:Files.readAllLines(ROOT.getParent().getParent().resolve("models/sa2_02515/chao_tv_idle.obj"))){
      if(line.startsWith("v ")){var p=line.split("\\s+");vertices.add(new double[]{Double.parseDouble(p[1]),Double.parseDouble(p[2]),Double.parseDouble(p[3])});}
      else if(line.startsWith("usemtl "))material=line.substring(7);
      else if(line.startsWith("f ")&&material.equals("sa2_channel_a_01_52"))for(String p:line.substring(2).split("\\s+"))screen.add(vertices.get(Integer.parseInt(p.split("/")[0])-1));
    }
    check(screen.size()==24,"Measure the actual shipped screen material faces");
    double[] b={Double.POSITIVE_INFINITY,Double.POSITIVE_INFINITY,Double.POSITIVE_INFINITY,Double.NEGATIVE_INFINITY,Double.NEGATIVE_INFINITY,Double.NEGATIVE_INFINITY};
    for(var p:screen)for(int i=0;i<3;i++){b[i]=Math.min(b[i],p[i]);b[i+3]=Math.max(b[i+3],p[i]);}
    check(Math.abs((b[1]+b[4])/2-TvWatchRules073.SCREEN_HEIGHT)<1e-7,"Gaze height is the rendered screen center, not the cabinet top");
    check(Math.abs(.5-(b[2]+b[5])/2-TvWatchRules073.SCREEN_FORWARD)<1e-7,"Actual front screen plane offset");
    check(Math.abs((b[0]+b[3])/2-.5-TvWatchRules073.SCREEN_RIGHT)<1e-7,"Actual asymmetric screen horizontal center");
  }
  public static void main(String[] args)throws Exception{
    screenGeometry();var clips=JsonParser.parseString(Files.readString(ROOT.getParent().getParent().resolve("animations/official/chao.animation.json"))).getAsJsonObject().getAsJsonObject("animations");
    var assets=new HashMap<String,ChaoMeshAsset>();var rig=new OfficialChaoRig(clips,k->assets.computeIfAbsent(k,TvWatchRigTest073::load));int serial=0;
    for(var named:states()){
      var s=named.state;String model=OfficialChaoRig.geometryKey(s);cases++;
      for(float cancel:new float[]{74,160}){
        var id=new UUID(730073,++serial);var previous=new HashMap<String,Vector3f>();
        for(int q=0;q<=760;q+=2){
          float age=q*.25f,t=age-20;boolean tv=t>=0&&t<cancel;
          var idle=LocomotionRules.groundGait(s)==LocomotionRules.GroundGait.CRAWL?OfficialChaoRig.Motion.SIT:OfficialChaoRig.Motion.WALK;
          var motion=tv&&(TvWatchRules073.seated(t)||idle==OfficialChaoRig.Motion.SIT)?OfficialChaoRig.Motion.SIT:idle;
          rig.prepareDevelopment(model,s);rig.animateGameplay(model,id,motion,age,0,0,0,0,0,0,false,false);rig.development(s);
          var before=new HashMap<String,float[]>();rig.poses().forEach((name,p)->before.put(name,data(p)));
          if(tv)ChaoTvWatchAnimation073.pose(rig,t);
          for(var e:rig.poses().entrySet()){
            float[] old=before.get(e.getKey()),now=data(e.getValue());
            for(int n:new int[]{0,1,2,6,7,8,9,10})check(old[n]==now[n],"Watching cannot move/resize/extend a joint or distort anatomy "+named.name+" "+e.getKey());
          }
          rig.finishGameplayFrame(id,tv?41*1024:0,age);frames++;
          check(rig.finite(),"Finite actual TV/idle blend "+named.name);
          for(String joint:List.of("skull","body","arm_left","arm_right","foot_left","foot_right","feet_bone_right")){
            if(!rig.poses().containsKey(joint))continue;
            var at=new PoseStack();rig.transformBone(joint,at);var p=at.last().pose().transformPosition(new Vector3f());var old=previous.put(joint,p);
            if(old!=null){float step=old.distance(p)*16;maxStep=Math.max(maxStep,step);check(step<.7f,"No half-tick joint pop through TV entry/exit "+named.name+" "+joint+" t="+t+" step="+step);}
          }
          var head=rig.sampledMesh().attachmentBounds("skull",rig.poses());if(head!=null){
            var at=new PoseStack();rig.transformBone("skull",at);float bottom=Float.NEGATIVE_INFINITY;
            for(int n=0;n<8;n++)bottom=Math.max(bottom,at.last().pose().transformPosition(new Vector3f(head[(n&1)==0?0:3],head[(n&2)==0?1:4],head[(n&4)==0?2:5]).div(16)).y);
            minHeadFloor=Math.min(minHeadFloor,1.5f-bottom);check(bottom<1.49f,"Head stays clear of floor while watching "+named.name);
          }
          if(t>=cancel&&t<cancel+8)exits++;
          if(t>=cancel+8){
            check(!rig.recoveringProne(id),"TV must not introduce a prone-recovery marker");
            for(String joint:List.of("skull","body","arm_left","arm_right")){
              var pose=rig.pose(joint);if(pose==null)continue;
              float[] target=before.get(joint),actual=data(pose);
              for(int n=0;n<11;n++)check(Math.abs(target[n]-actual[n])<.00001f,"TV-touched joints restore ordinary idle after the shared eight-tick exit "+named.name+" "+joint+" index="+n);
            }
          }
        }
      }
    }
    System.out.println("TV shared rig073: "+checks+" checks; "+cases+" anatomy/stage/axis cases, "+frames+" actual frames, "+exits+" cancellation/end exit samples; max half-tick joint step="+maxStep+", min skull-floor gap="+minHeadFloor+" model blocks. Original head/joint scales and saved state are not edited; visual quality requires native client review.");
  }
}
