package com.chaogarden.client;

import com.chaogarden.core.ChaoState;
import com.chaogarden.core.DarkPowerMorphology071;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;

/** Display-only thirty-tick interpolation. No biological/stat field is saved here. */
final class DarkPowerAppearance071 {
  static final float CHANGE_TICKS=30;
  private static final Map<UUID,Blend> BLENDS=new LinkedHashMap<>(128,.75f,true);
  private static final Map<ChaoState,DarkPowerMorphology071.Sample> FRAME=new WeakHashMap<>();
  private static final class Blend {
    DarkPowerMorphology071.Sample from,target,current;
    float started,last;
  }
  private DarkPowerAppearance071(){}
  static DarkPowerMorphology071.Sample sample(ChaoState state){var prepared=FRAME.get(state);
    return prepared!=null?prepared:DarkPowerMorphology071.sample(state);}
  static DarkPowerMorphology071.Sample prepare(UUID id,ChaoState state,float age) {
    var target=DarkPowerMorphology071.sample(state);var blend=BLENDS.get(id);
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
  private static boolean near(DarkPowerMorphology071.Sample a,DarkPowerMorphology071.Sample b) {
    return a.enabled()==b.enabled()&&Math.abs(a.maturity()-b.maturity())<.0001f
        &&Math.abs(a.runPower()-b.runPower())<.0001f&&Math.abs(a.swimFly()-b.swimFly())<.0001f;
  }
  private static DarkPowerMorphology071.Sample between(DarkPowerMorphology071.Sample a,DarkPowerMorphology071.Sample b,float t) {
    float w=t*t*(3-2*t);
    return new DarkPowerMorphology071.Sample(b.enabled(),lerp(a.maturity(),b.maturity(),w),
        lerp(a.runPower(),b.runPower(),w),lerp(a.swimFly(),b.swimFly(),w));
  }
  private static float lerp(float a,float b,float t){return a+(b-a)*t;}
}
