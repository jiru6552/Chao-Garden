package com.chaogarden.client;

import com.chaogarden.core.ChaoState;
import com.chaogarden.core.HeroSwimMorphology064;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;

/** Display-only thirty-tick interpolation. No biological/stat field is saved here. */
final class HeroSwimAppearance064 {
  // Use the established thirty-tick adult display interval for this family.
  // Shape-emitter tests separately bound displacement; this clock smooths
  // visual changes without rewriting biological maturity or training.
  static final float CHANGE_TICKS=30;
  private static final Map<UUID,Blend> BLENDS=new LinkedHashMap<>(128,.75f,true);
  private static final Map<ChaoState,HeroSwimMorphology064.Sample> FRAME=new WeakHashMap<>();
  private static final class Blend {
    HeroSwimMorphology064.Sample from,target,current;
    float started,last;
  }
  private HeroSwimAppearance064(){}
  static HeroSwimMorphology064.Sample sample(ChaoState state){return FRAME.getOrDefault(state,HeroSwimMorphology064.sample(state));}
  static HeroSwimMorphology064.Sample prepare(UUID id,ChaoState state,float age) {
    var target=HeroSwimMorphology064.sample(state);var blend=BLENDS.get(id);
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
  private static boolean near(HeroSwimMorphology064.Sample a,HeroSwimMorphology064.Sample b) {
    return a.enabled()==b.enabled()&&Math.abs(a.maturity()-b.maturity())<.0001f
        &&Math.abs(a.runPower()-b.runPower())<.0001f&&Math.abs(a.swimFly()-b.swimFly())<.0001f;
  }
  private static HeroSwimMorphology064.Sample between(HeroSwimMorphology064.Sample a,HeroSwimMorphology064.Sample b,float t) {
    float w=t*t*(3-2*t);
    return new HeroSwimMorphology064.Sample(b.enabled(),lerp(a.maturity(),b.maturity(),w),
        lerp(a.runPower(),b.runPower(),w),lerp(a.swimFly(),b.swimFly(),w));
  }
  private static float lerp(float a,float b,float t){return a+(b-a)*t;}
}
