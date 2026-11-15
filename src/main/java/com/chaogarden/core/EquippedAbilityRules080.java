package com.chaogarden.core;

/** Development policy for the hidden equipped abilities. No player-facing text. */
public final class EquippedAbilityRules080 {
  public static final int MAX_PERCENT=1000,STEP_PERCENT=100;
  public static final String RUN_SETTING="equipped.run.percent",POWER_SETTING="equipped.power.percent";
  private EquippedAbilityRules080() {}

  /** Use the Analyzer's expressed rank and current exact specialization label.
   * Inherited X genes, a stale secondaryType cache, age strength and a highest
   * stat are not substitutes for these distinct live properties. */
  public static boolean eligible(ChaoState s,int stat) {
    if(s==null||!s.adult||s.dead||s.cocoonTicks>0||(stat!=ChaoState.RUN&&stat!=ChaoState.POWER)
        ||s.grades[stat]!=6||s.levels[stat]!=99||s.lockedPrimaryType()!=stat+1)return false;
    int[] forms=s.secondaryTypes();return forms.length==1&&forms[0]==stat+1;
  }
  public static int eligibleStat(ChaoState s) {
    if(eligible(s,ChaoState.RUN))return ChaoState.RUN;
    return eligible(s,ChaoState.POWER)?ChaoState.POWER:-1;
  }
  private static String key(int stat) {
    if(stat==ChaoState.RUN)return RUN_SETTING;
    if(stat==ChaoState.POWER)return POWER_SETTING;
    throw new IllegalArgumentException("Unsupported equipped ability stat");
  }
  public static int percent(ChaoState s,int stat) {
    int raw=s.traitInt(key(stat),MAX_PERCENT);
    return raw>=0&&raw<=MAX_PERCENT&&raw%STEP_PERCENT==0?raw:MAX_PERCENT;
  }
  public static int nextPercent(int value) {return value<=0?MAX_PERCENT:value-STEP_PERCENT;}
  /** Existing save-local traits supply a stable default without a schema or reroll.
   * A saturated existing trait map fails atomically instead of applying an unsaved value. */
  public static boolean cycle(ChaoState s,int stat) {
    if(!eligible(s,stat))return false;
    int next=nextPercent(percent(s,stat));
    return s.setTrait(key(stat),next==MAX_PERCENT?"":Integer.toString(next));
  }
  public static double bonus(ChaoState s,int stat) {return eligible(s,stat)?percent(s,stat)/100.0:0;}
  public static double strength(double original,double bonus) {
    if(!(original>0)||!Double.isFinite(original)||!Double.isFinite(bonus)||!(bonus>0))return original;
    return original*(1+bonus);
  }
}
