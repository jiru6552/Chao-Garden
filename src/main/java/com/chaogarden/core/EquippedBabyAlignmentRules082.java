package com.chaogarden.core;

/** Baby-only alignment arithmetic and eligible-time clocks; server ownership,
 * kill credit and persistence remain in the existing companion implementation. */
public final class EquippedBabyAlignmentRules082 {
  public static final int TICKS_PER_MINUTE=1200;
  public static final String NETHER="minecraft:the_nether", END="minecraft:the_end", AETHER="aether:the_aether";
  public enum Influence { NONE, HERO, DARK }

  /** Separate clocks pause independently. Corrupt or obsolete counters cannot
   * manufacture a nearly completed minute on the next equipped tick. */
  public record Clock(int darkTicks,int heroTicks) {
    public static final Clock EMPTY=new Clock(0,0);
    public Clock {darkTicks=remainder(darkTicks);heroTicks=remainder(heroTicks);}
    private static int remainder(int value){return value>=0&&value<TICKS_PER_MINUTE?value:0;}
  }
  public record TickResult(Clock clock,boolean alignmentChanged) {}

  private EquippedBabyAlignmentRules082() {}

  public static boolean eligible(ChaoState state) {
    return state!=null&&!state.dead&&!state.adult&&state.cocoonTicks<=0
        &&!OmochaoRules.isOmochao(state)&&state.intTrait("content.construction")<=0;
  }

  /** A displayed point is 0.01 on the saved -1..1 scale. Only alignment changes;
   * a rejected adult retains even its public alignment mirror byte-for-byte. */
  public static boolean applyPoints(ChaoState state,double signedPoints) {
    if(!eligible(state)||!Double.isFinite(signedPoints)||signedPoints==0)return false;
    double before=Float.isFinite(state.align)?Math.clamp(state.align,-1,1):0;
    float next=(float)Math.clamp(before+Math.clamp(signedPoints,-100,100)/100.0,-1,1);
    if(Float.floatToIntBits(next)==Float.floatToIntBits(state.align))return false;
    state.align=next;return true;
  }

  /** Installation, not whether the player has visited it, substitutes the
   * Aether for the End. Other namespaces and dimensions receive no bonus. */
  public static Influence passiveInfluence(String dimensionId,boolean aetherLoaded) {
    if(NETHER.equals(dimensionId))return Influence.DARK;
    return (aetherLoaded?AETHER:END).equals(dimensionId)?Influence.HERO:Influence.NONE;
  }

  /** Exactly one caller-validated equipped tick, without wall-time catchup.
   * A minute at a clamped endpoint is consumed instead of being banked. */
  public static TickResult tickPassive(ChaoState state,Clock previous,Influence influence,boolean eligibleContext) {
    var clock=previous==null?Clock.EMPTY:previous;
    if(!eligibleContext||!eligible(state)||influence==null||influence==Influence.NONE)return new TickResult(clock,false);
    int dark=clock.darkTicks(),hero=clock.heroTicks();boolean changed=false;
    if(influence==Influence.DARK){if(++dark==TICKS_PER_MINUTE){dark=0;changed=applyPoints(state,-1);}}
    else if(++hero==TICKS_PER_MINUTE){hero=0;changed=applyPoints(state,1);}
    return new TickResult(new Clock(dark,hero),changed);
  }
}
