package com.chaogarden.client;

import com.chaogarden.ChaoVariants;
import com.chaogarden.core.ChaoState;
import java.util.Random;
import java.util.UUID;

/** Runtime pose coverage, invoked only by the excluded developer study. */
final class BackGripVisualSmokePoseProbe {
  private BackGripVisualSmokePoseProbe() {}
  static int inspect() {
    int checked=0;var rig=new OfficialChaoRig();
    var themes=new java.util.ArrayList<String>();themes.add(ChaoVariants.NORMAL);
    for(var definition:ChaoVariants.all())themes.add(definition.id());
    for(String theme:themes)for(int family=0;family<15;family++)
      for(float age:new float[]{0,.6f,1.2f})for(int sf=-1;sf<=1;sf++)for(int rp=-1;rp<=1;rp++) {
        var state=ChaoState.egg(4,true,true,0,new Random(23));
        state.adult=true;state.variant=theme;state.align=family<5?0:family<10?1:-1;
        state.primaryType=family%5;state.swimFly=sf;state.runPower=rp;state.setEvolutionStrength(age);
        String key=OfficialChaoRig.geometryKey(state);rig.prepareDevelopment(key,state);
        rig.animateGameplay(key,new UUID(23,++checked),OfficialChaoRig.Motion.GRIP,120,3,.14f,0,0,0,0,false);
        rig.development(state);
        require(rig.finite(),key+" / "+theme+" non-finite pose");
        require(rig.motion()==OfficialChaoRig.Motion.GRIP,"grip changed gait");
        require(rig.idleGestureWeight()==0,"grip waved its hands");
        for(String arm:java.util.List.of("arm_left","arm_right")) {
          var p=rig.pose(arm);require(p!=null,"missing grip arm "+key);
          require(Math.abs(Math.abs(p.ry)-.85f)<.0001f,"palm does not reach forward");
          require(p.z< -1.7f&&p.y>=0,"palm retained idle shoulder/skull position");
        }
      }
    for(String key:new String[]{"chaos_light","chaos_angel","chaos_devil","amy","omochao","tails","knuckles"}) {
      rig.animateGameplay(key,new UUID(23,++checked),OfficialChaoRig.Motion.GRIP,120,0,0,0,0,0,0,false);
      require(rig.finite(),"special grip "+key);
    }
    for(String key:OfficialChaoRig.REFERENCE_MODELS) {
      rig.animate(key,120,0,0,0,0,0,0);rig.applyCompanionGrip();
      require(rig.finite()&&rig.motion()==OfficialChaoRig.Motion.GRIP,"reference grip "+key);checked++;
    }
    rig.animateGameplay("baby",new UUID(23,99999),OfficialChaoRig.Motion.GRIP,120,0,0,0,0,0,0,false);
    var arm=rig.pose("arm_left");float saved=arm.rx;arm.rx=Float.NaN;
    require(!rig.finite(),"non-finite negative fixture accepted");arm.rx=saved;
    return checked;
  }
  private static void require(boolean pass,String message) {
    if(!pass)throw new IllegalStateException("Back-grip pose: "+message);
  }
}
