package com.chaogarden.client;

import com.chaogarden.core.ChaoState;
import com.chaogarden.core.NeutralFlyMorphology061;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;

/** Display-only twenty-tick interpolation. No biological/stat field is saved here. */
final class NeutralFlyAppearance061 {
  static final float CHANGE_TICKS=20;
  private static final Map<UUID,Blend> BLENDS=new LinkedHashMap<>(128,.75f,true);
  private static final Map<ChaoState,NeutralFlyMorphology061.Sample> FRAME=new WeakHashMap<>();
  private static final class Blend {
    NeutralFlyMorphology061.Sample from,target,current;
    float started,last;
  }
  private NeutralFlyAppearance061(){}
  static NeutralFlyMorphology061.Sample sample(ChaoState state){return FRAME.getOrDefault(state,NeutralFlyMorphology061.sample(state));}
  static NeutralFlyMorphology061.Sample prepare(UUID id,ChaoState state,float age) {
    var target=NeutralFlyMorphology061.sample(state);var blend=BLENDS.get(id);
    if(blend==null||!Float.isFinite(age)||age<blend.last||age-blend.last>80||target.enabled()!=blend.target.enabled()) {
      blend=new Blend();blend.from=blend.target=blend.current=target;blend.started=blend.last=age;BLENDS.put(id,blend);
    } else {
      blend.current=between(blend.from,blend.target,Math.clamp((age-blend.started)/CHANGE_TICKS,0,1));
      if(!near(blend.target,target)){blend.from=blend.current;blend.target=target;blend.started=age;}
      blend.last=age;
    }
    FRAME.put(state,blend.current);
    while(BLENDS.size()>128)BLENDS.remove(BLENDS.keySet().iterator().next());
    return blend.current;
  }
  private static boolean near(NeutralFlyMorphology061.Sample a,NeutralFlyMorphology061.Sample b) {
    return a.enabled()==b.enabled()&&Math.abs(a.maturity()-b.maturity())<.0001f
        &&Math.abs(a.runPower()-b.runPower())<.0001f&&Math.abs(a.swimFly()-b.swimFly())<.0001f;
  }
  private static NeutralFlyMorphology061.Sample between(NeutralFlyMorphology061.Sample a,NeutralFlyMorphology061.Sample b,float t) {
    float w=t*t*(3-2*t);
    return new NeutralFlyMorphology061.Sample(b.enabled(),lerp(a.maturity(),b.maturity(),w),
        lerp(a.runPower(),b.runPower(),w),lerp(a.swimFly(),b.swimFly(),w));
  }
  private static float lerp(float a,float b,float t){return a+(b-a)*t;}
}
