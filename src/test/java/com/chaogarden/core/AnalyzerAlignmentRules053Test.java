package com.chaogarden.core;

/** Pure policy boundaries; real server permission and sync are native GameTests. */
public final class AnalyzerAlignmentRules053Test {
  private static int checks;
  private static void check(boolean value,String why){checks++;if(!value)throw new AssertionError(why);}
  private static ChaoState baby() {
    var state=new ChaoState();state.ageTicks=12345;state.setEvolutionStrength(.65f);
    state.points[ChaoState.RUN]=444;state.levels[ChaoState.RUN]=4;state.progress[ChaoState.RUN]=80;
    state.happiness=73;state.bond=44;state.hunger=61;state.energy=89;
    state.setTrait("alignment53.foreign","preserve care, genes and recency");
    return state;
  }
  public static int run() {
    checks=0;
    for(boolean creative:new boolean[]{false,true})for(boolean spectator:new boolean[]{false,true}) {
      var state=baby();
      check(AlignmentEditRules053.canEdit(state,creative,spectator)==(creative&&!spectator),"Server permission inputs");
      state.adult=true;check(!AlignmentEditRules053.canEdit(state,creative,spectator),"Adults cannot be edited");
      state.adult=false;state.dead=true;check(!AlignmentEditRules053.canEdit(state,creative,spectator),"Dead state cannot be edited");
      state.dead=false;state.immortal=true;check(!AlignmentEditRules053.canEdit(state,creative,spectator),"Special immortal state is read only");
    }
    check(!AlignmentEditRules053.canEdit(null,true,false),"Missing target cannot be edited");

    for(int milli=-1000;milli<=1000;milli+=25) {
      var actual=baby();actual.align=.137f;String before=actual.encode();
      var expected=ChaoState.decode(before);expected.align=milli/1000f;
      float maturity=actual.evolutionStrength(),run=HeroRunMorphology052.sample(actual).babyRun();
      check(AlignmentEditRules053.validMilli(milli),"Legal fixed-point endpoint or intermediate");
      check(AlignmentEditRules053.applyMilli(actual,milli),"Valid baby change is accepted");
      check(Float.floatToIntBits(actual.align)==Float.floatToIntBits(milli/1000f),"Exactly one fixed-point conversion");
      check(actual.encode().equals(expected.encode()),"Only alignment may change in the full saved record");
      check(actual.ageTicks==12345&&actual.evolutionStrength()==maturity,"Alignment cannot change biological age or maturity");
      check(HeroRunMorphology052.sample(actual).babyRun()==run,"Run development is independent of the alignment control");
      check(AlignmentEditRules053.displayMilli(actual)==milli,"Displayed bar position round-trips");
      String applied=actual.encode();
      check(!AlignmentEditRules053.applyMilli(actual,milli)&&actual.encode().equals(applied),"Repeated drag sample has no side effects");
      var loaded=ChaoState.decode(actual.encode());
      check(loaded.encode().equals(actual.encode())&&AlignmentEditRules053.displayMilli(loaded)==milli,"Alignment persists without a format migration");
    }

    var unchanged=baby();unchanged.align=.3f;String original=unchanged.encode();
    for(int invalid:new int[]{Integer.MIN_VALUE,-1001,1001,Integer.MAX_VALUE}) {
      check(!AlignmentEditRules053.validMilli(invalid),"Malformed fixed-point request rejected");
      check(!AlignmentEditRules053.applyMilli(unchanged,invalid)&&unchanged.encode().equals(original),"Malformed request cannot mutate state");
    }
    for(float invalid:new float[]{Float.NaN,Float.NEGATIVE_INFINITY,Float.POSITIVE_INFINITY,-1.0001f,1.0001f,-Float.MAX_VALUE,Float.MAX_VALUE})
      check(!AlignmentEditRules053.apply(unchanged,invalid)&&unchanged.encode().equals(original),"Nonfinite/out-of-range core request rejected without mutation");
    check(!AlignmentEditRules053.apply(null,0)&&!AlignmentEditRules053.applyMilli(null,0),"Missing state has no write path");

    for(float alignment:new float[]{-.8f,0,.8f}) {
      var adult=baby();adult.adult=true;adult.primaryType=ChaoState.RUN_TYPE;adult.align=alignment;
      adult=ChaoState.decode(adult.encode());String locked=adult.encode();
      for(int attempted:new int[]{-1000,0,1000})
        check(!AlignmentEditRules053.applyMilli(adult,attempted)&&adult.encode().equals(locked),"Creative cannot alter any adult alignment category");
      check(adult.lockedAlignment()==alignment,"Original first-evolution identity remains locked");
      var reloaded=ChaoState.decode(adult.encode());
      check(!AlignmentEditRules053.apply(reloaded,-alignment)&&reloaded.encode().equals(locked),"Reload cannot unlock alignment");
      // The display must not trust an external public-field write, and refusal
      // must not normalize unrelated fields as a hidden side effect.
      adult.align=alignment==.8f?-1:1;int raw=Float.floatToIntBits(adult.align);
      check(AlignmentEditRules053.displayMilli(adult)==Math.round(alignment*1000),"Adult bar shows the locked identity");
      check(!AlignmentEditRules053.apply(adult,0)&&Float.floatToIntBits(adult.align)==raw,"Adult rejection is read only even after an external mirror edit");
    }
    var dead=baby();dead.dead=true;String deadBefore=dead.encode();
    check(!AlignmentEditRules053.apply(dead,1)&&dead.encode().equals(deadBefore),"Dead target is unchanged");
    var immortal=baby();immortal.immortal=true;String specialBefore=immortal.encode();
    check(!AlignmentEditRules053.apply(immortal,1)&&immortal.encode().equals(specialBefore),"Immortal target is unchanged");
    return checks;
  }
}
