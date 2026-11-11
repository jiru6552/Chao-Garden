package com.chaogarden.client;

import com.chaogarden.core.ChaoState;
import com.google.gson.*;
import java.nio.file.*;
import java.util.*;

/** The real meshes retain visible strides and bounded per-frame motion at high Run. */
public final class RunningGaitRigTest {
  private static final String[] MODELS={"baby","swim","fly","run","power","hero","hero_swim","hero_fly","hero_run","hero_power","dark","dark_swim","dark_fly","dark_run","dark_power","amy","omochao","tails","knuckles","chaos_light","chaos_angel","chaos_devil"};
  private static int checks;
  private static void check(boolean ok,String why){checks++;if(!ok)throw new AssertionError(why);}
  public static void main(String[] args)throws Exception {
    var root=Path.of("src/main/resources/assets/chao_garden");
    var clips=JsonParser.parseString(Files.readString(root.resolve("animations/official/chao.animation.json"))).getAsJsonObject().getAsJsonObject("animations");
    var assets=new HashMap<String,ChaoMeshAsset>();
    var rig=new OfficialChaoRig(clips,key->assets.computeIfAbsent(key,k->{try{return ChaoMeshAsset.parse(JsonParser.parseString(Files.readString(root.resolve("models/entity/"+k+".json"))).getAsJsonObject());}catch(Exception e){throw new RuntimeException(e);}}));
    int cases=0, poses=0;
    for(String model:MODELS)for(boolean adult:new boolean[]{false,true}) {
      var state=new ChaoState();state.adult=adult;state.setEvolutionStrength(1.2f);
      state.align=model.startsWith("dark")?-1:model.startsWith("hero")?1:0;
      state.immortal=model.startsWith("chaos_");
      if(Set.of("amy","omochao","tails","knuckles").contains(model))state.character=model;
      if(model.endsWith("swim")||model.equals("swim"))state.primaryType=ChaoState.SWIM_TYPE;
      if(model.endsWith("power")||model.equals("power"))state.primaryType=ChaoState.POWER_TYPE;
      int lowPeaks=0;
      for(int speed=0;speed<3;speed++) {
        float rate=new float[]{.08f,.32f,1}[speed]*(adult?1:1.25f);
        var mode=new OfficialChaoRig.Motion[]{OfficialChaoRig.Motion.WALK,OfficialChaoRig.Motion.RUN,OfficialChaoRig.Motion.DASH}[speed];
        var id=new UUID(4618,++cases);
        float previousFoot=0,previousBody=0,maxFoot=0,minFoot=0;int peaks=0;
        for(int frame=0;frame<=240;frame++) {
          float age=frame/3f;
          rig.prepareDevelopment(model,state);
          rig.animateGameplay(model,id,mode,age,age*rate,rate,0,0,0,0,false);
          rig.development(state);
          float foot=rig.pose("foot_left").rx,body=rig.pose("body").ry;
          check(rig.finite(),"non-finite "+model+" "+adult+" "+mode);
          if(frame>0) {
            check(Math.abs(foot-previousFoot)<.16f,"foot snapped at "+model+" "+adult+" "+mode+" frame="+frame);
            check(Math.abs(body-previousBody)<.08f,"torso snapped at "+model+" "+adult+" "+mode+" frame="+frame);
          }
          if(foot>.15f&&previousFoot<=.15f)peaks++;
          previousFoot=foot;previousBody=body;maxFoot=Math.max(maxFoot,foot);minFoot=Math.min(minFoot,foot);poses++;
        }
        check(maxFoot>.28f&&minFoot<-.25f,"stride lost its readable contact/recovery "+model+" "+mode);
        check(peaks>=2&&peaks<=9,"stride cadence outside readable range "+model+" "+mode+" peaks="+peaks);
        if(speed==0)lowPeaks=peaks;
        if(speed==2)check(peaks>lowPeaks&&peaks<=lowPeaks*3,"dash cadence did not compress smoothly "+model);
        // Stat unlocks switch posture without resetting the shared gait phase.
        float foot=rig.pose("foot_left").rx;
        var next=mode==OfficialChaoRig.Motion.RUN?OfficialChaoRig.Motion.DASH:OfficialChaoRig.Motion.RUN;
        rig.animateGameplay(model,id,next,80.1f,80.1f*rate,rate,0,0,0,0,false);
        check(Math.abs(rig.pose("foot_left").rx-foot)<.00001f,"gait unlock bypassed outgoing-pose blend");
        for(int frame=1;frame<=30;frame++) {
          float age=80.1f+frame/3f;
          rig.animateGameplay(model,id,next,age,age*rate,rate,0,0,0,0,false);
          check(Math.abs(rig.pose("foot_left").rx-foot)<.16f,"gait unlock caused a visible foot jump");
          foot=rig.pose("foot_left").rx;
        }
      }
    }
    String result="PASS: "+checks+" running pose checks across "+cases+" form/age/speed cases and "+poses+" real-mesh frames; bounded cadence, retained foot travel and gait-unlock continuity\n";
    Files.createDirectories(Path.of("build/reports/lionfish"));Files.writeString(Path.of("build/reports/lionfish/running-gait-results.txt"),result);System.out.print(result);
  }
}
