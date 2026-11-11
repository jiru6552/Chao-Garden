package com.chaogarden.client;
import com.google.gson.*;
import com.chaogarden.core.ChaoState;
import java.nio.file.*;
import java.util.*;
/** A Chao rendered again after an offscreen interval must use its current pose. */
public final class VisibleTransitionRigTest {
  static int checks;static void check(boolean ok,String why){checks++;if(!ok)throw new AssertionError(why);}
  public static void main(String[] args)throws Exception{
    var root=Path.of("src/main/resources/assets/chao_garden");
    var clips=JsonParser.parseString(Files.readString(root.resolve("animations/official/chao.animation.json"))).getAsJsonObject().getAsJsonObject("animations");
    var assets=new HashMap<String,ChaoMeshAsset>();
    var rig=new OfficialChaoRig(clips,key->assets.computeIfAbsent(key,k->{try{return ChaoMeshAsset.parse(JsonParser.parseString(Files.readString(root.resolve("models/entity/"+k+".json"))).getAsJsonObject());}catch(Exception e){throw new RuntimeException(e);}}));
    int cases=0;
    try(var files=Files.list(root.resolve("models/entity/gameplay"))){for(var path:files.filter(p->p.toString().endsWith(".json")&&!p.toString().contains("_glass")).toList()){
      String model=path.getFileName().toString().replace("chao_","").replace(".json","");
      for(boolean adult:new boolean[]{false,true})for(boolean sameMode:new boolean[]{false,true}){
        var s=new ChaoState();s.adult=adult;var id=new UUID(cases++,913);
        frame(rig,model,s,id,OfficialChaoRig.Motion.WALK,10,0,0);
        // A real nearby frame retains the normal outgoing pose and completes its blend.
        frame(rig,model,s,id,OfficialChaoRig.Motion.PLAY,11,8192,1);
        check(Math.abs(rig.poses().get("arm_right").rx)<.01f,"nearby-frame transition must start from visible arm");
        frame(rig,model,s,id,OfficialChaoRig.Motion.PLAY,20,8192,1);
        check(Math.abs(rig.poses().get("arm_right").rx-1)<.01f,"visible transition completes");
        if(!sameMode)frame(rig,model,s,id,OfficialChaoRig.Motion.WALK,21,0,0);
        // The next camera view arrives well after the eight-tick transition window.
        frame(rig,model,s,id,OfficialChaoRig.Motion.PLAY,60,8192,1.4f);
        check(Math.abs(rig.poses().get("arm_right").rx-1.4f)<.001f,"offscreen pose must not replay stale arm: "+model+" adult="+adult+" same="+sameMode+" actual="+rig.poses().get("arm_right").rx);
      }
    }}
    String result="PASS: "+checks+" visible transition checks across "+cases+" form/age/history cases; nearby blending preserved, stale offscreen pose discarded\n";
    Files.createDirectories(Path.of("build/reports/lionfish"));Files.writeString(Path.of("build/reports/lionfish/visible-transition-results.txt"),result);System.out.print(result);
  }
  static void frame(OfficialChaoRig rig,String model,ChaoState s,UUID id,OfficialChaoRig.Motion motion,float age,int emotion,float arm){
    rig.prepareDevelopment(model,s);rig.animateGameplay(model,id,motion,age,0,0,0,0,0,0,false,false);rig.development(s);rig.poses().get("arm_right").rx=arm;rig.finishGameplayFrame(id,emotion,age);
  }
}
