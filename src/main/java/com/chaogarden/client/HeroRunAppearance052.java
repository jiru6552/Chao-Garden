package com.chaogarden.client;

import com.chaogarden.core.ChaoState;
import com.chaogarden.core.HeroRunMorphology052;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;

/** Client-only bounded interpolation; no saved value is changed. */
final class HeroRunAppearance052 {
  static final float CHANGE_TICKS=20;
  private static final Map<UUID,Blend> BLENDS=new LinkedHashMap<>(128,.75f,true);
  private static final Map<ChaoState,HeroRunMorphology052.Sample> FRAME=new WeakHashMap<>();
  private static final Map<ChaoState,Float> FRAME_ALIGNMENT=new WeakHashMap<>();
  private static final class Blend {
    HeroRunMorphology052.Sample from,target,current;
    float started,last,alignmentFrom,alignmentTarget,alignmentCurrent;
  }
  private HeroRunAppearance052(){}

  static HeroRunMorphology052.Sample sample(ChaoState state) {
    return FRAME.getOrDefault(state,HeroRunMorphology052.sample(state));
  }
  /** Signed alignment uses the same display clock, including Neutral-to-Dark edits. */
  static float signedAlignment(ChaoState state) {
    return FRAME_ALIGNMENT.getOrDefault(state,state.lockedAlignment());
  }
  static HeroRunMorphology052.Sample prepare(UUID id,ChaoState state,float age) {
    var target=HeroRunMorphology052.sample(state);float alignment=state.lockedAlignment();var blend=BLENDS.get(id);
    if(blend==null||!Float.isFinite(age)||age<blend.last||age-blend.last>80||target.adult()!=blend.target.adult()) {
      blend=new Blend();blend.from=blend.target=blend.current=target;blend.started=blend.last=age;BLENDS.put(id,blend);
      blend.alignmentFrom=blend.alignmentTarget=blend.alignmentCurrent=alignment;
    } else {
      float t=unit((age-blend.started)/CHANGE_TICKS),w=t*t*(3-2*t);
      blend.current=between(blend.from,blend.target,t);
      blend.alignmentCurrent=lerp(blend.alignmentFrom,blend.alignmentTarget,w);
      if(!near(blend.target,target)||Math.abs(blend.alignmentTarget-alignment)>.0001f) {
        blend.from=blend.current;blend.target=target;blend.started=age;
        blend.alignmentFrom=blend.alignmentCurrent;blend.alignmentTarget=alignment;
      }
      blend.last=age;
    }
    FRAME.put(state,blend.current);
    FRAME_ALIGNMENT.put(state,blend.alignmentCurrent);
    while(BLENDS.size()>128)BLENDS.remove(BLENDS.keySet().iterator().next());
    return blend.current;
  }
  private static boolean near(HeroRunMorphology052.Sample a,HeroRunMorphology052.Sample b) {
    return a.enabled()==b.enabled()&&a.adult()==b.adult()&&Math.abs(a.babyHero()-b.babyHero())<.0001f
        &&Math.abs(a.babyRun()-b.babyRun())<.0001f&&Math.abs(a.specialization()-b.specialization())<.0001f
        &&Math.abs(a.maturity()-b.maturity())<.0001f&&Math.abs(a.adultGrowth()-b.adultGrowth())<.0001f;
  }
  private static HeroRunMorphology052.Sample between(HeroRunMorphology052.Sample a,HeroRunMorphology052.Sample b,float t) {
    // A smooth step has zero velocity at an isolated edit's endpoints. New
    // packets restart from the currently displayed pose, never the old target.
    float w=t*t*(3-2*t);
    return new HeroRunMorphology052.Sample(t<1?a.enabled()||b.enabled():b.enabled(),b.adult(),
        lerp(a.babyHero(),b.babyHero(),w),lerp(a.babyRun(),b.babyRun(),w),
        lerp(a.specialization(),b.specialization(),w),lerp(a.maturity(),b.maturity(),w),lerp(a.adultGrowth(),b.adultGrowth(),w));
  }
  private static float lerp(float a,float b,float t){return a+(b-a)*t;}
  private static float unit(float value){return Math.clamp(value,0,1);}
}
