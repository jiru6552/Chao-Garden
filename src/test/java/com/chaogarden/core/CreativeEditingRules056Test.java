package com.chaogarden.core;

/** Editing transactions preserve the saved journey and use native level units. */
public final class CreativeEditingRules056Test {
  private static int checks;
  private static void check(boolean ok,String why) {checks++;if(!ok)throw new AssertionError(why);}
  public static int run() {
    checks=0;
    for(boolean adult:new boolean[]{false,true})for(int value:new int[]{0,250,500,750,990,1000}) {
      var s=new ChaoState();s.adult=adult;s.align=.8f;s.primaryType=ChaoState.RUN_TYPE;
      s.ageTicks=1234;s.adultAgeTicks=456;s.happiness=88;s.reincarnations=2;s.setEvolutionStrength(.137f);
      String before=s.encode();var expected=ChaoState.decode(before);
      if(!adult&&value==1000) {
        check(!MaturityEditRules056.applyMilli(s,value)&&s.encode().equals(before),"Child endpoint rejects evolution-triggering growth");continue;
      }
      expected.setEvolutionStrength(value/1000f*(adult?1.2f:1));
      check(MaturityEditRules056.applyMilli(s,value),"Stage growth editable");
      check(s.encode().equals(expected.encode()),"Only maturity and its checkpoint are affected");
      check(s.adult==adult&&s.ageTicks==1234&&s.adultAgeTicks==456&&s.cocoonTicks==0&&s.reincarnations==2,"No calendar, death, cocoon or duplicate evolution effects");
      check(MaturityEditRules056.displayMilli(s)==value,"Displayed stage percentage has exact milli round trip");
      check(ChaoState.decode(s.encode()).encode().equals(s.encode()),"Maturity persists through schema decode");
      check(!MaturityEditRules056.applyMilli(s,value),"Repeated same sample is no-op");
      check(s.tick(1_000_000,2_000_000,new java.util.Random(56))==ChaoState.Transition.NONE,"Dragging alone does not cause life transition");
    }
    for(int value:new int[]{-1,Integer.MIN_VALUE,1001,Integer.MAX_VALUE}) {
      var s=new ChaoState();String before=s.encode();
      check(!MaturityEditRules056.applyMilli(s,value)&&s.encode().equals(before),"Invalid growth is saved-state no-op");
    }
    for(boolean creative:new boolean[]{false,true})for(boolean spectator:new boolean[]{false,true})
      check(MaturityEditRules056.canEdit(new ChaoState(),creative,spectator)==(creative&&!spectator),"Maturity access rule");
    var dead=new ChaoState();dead.dead=true;check(!MaturityEditRules056.applyMilli(dead,500),"Dead Chao cannot be aged by editor");
    var chaos=new ChaoState();chaos.immortal=true;check(!MaturityEditRules056.applyMilli(chaos,500),"Special immutable forms not rewritten");
    check(!MaturityEditRules056.canEdit(null,true,false),"Missing target not editable");

    for(String name:CreativeChaoEditRules056.STAT_NAMES)for(int level:new int[]{0,1,25,50,99}) {
      int stat=CreativeChaoEditRules056.stat(name);var s=new ChaoState();s.align=.2f;s.ageTicks=500;s.setEvolutionStrength(.5f);
      s.setTrait("edit56.keep","saved");long entropy=s.entropy;
      check(CreativeChaoEditRules056.apply(s,100,stat,level),"Each requested actual level accepted");
      check(CreativeChaoEditRules056.level(s,stat)==level&&s.align==1,"Stat and baby alignment set");
      check(s.ageTicks==500&&s.evolutionStrength()==.5f&&s.entropy==entropy&&s.trait("edit56.keep").equals("saved"),"Testing edit keeps age, entropy and foreign data");
      check(s.hiddenProgress[0]==0&&s.hiddenProgress[1]==0,"Hidden units are 100-progress levels, no fabricated progress");
      check(ChaoState.decode(s.encode()).encode().equals(s.encode()),"Edited actual levels persist");
      if(stat<4&&level>0)check(s.lastTrainedStat()==stat&&s.prospectivePrimaryType()==stat+1,"Actual increase participates in ordinary first-type history");
      int[] history=s.trainingHistorySnapshot();
      check(CreativeChaoEditRules056.apply(s,100,stat,0),"Decrease accepted");
      check(java.util.Arrays.equals(history,s.trainingHistorySnapshot()),"Decrease cannot invent most recent raised tie preference");
      check(stat<4?s.points[stat]==0:stat==CreativeChaoEditRules056.LUCK?s.luck==0:s.intelligence==0,"Zero level removes corresponding earned points");
    }
    var tied=new ChaoState();
    CreativeChaoEditRules056.apply(tied,0,ChaoState.RUN,10);
    CreativeChaoEditRules056.apply(tied,0,ChaoState.FLY,10);
    check(tied.prospectivePrimaryType()==ChaoState.FLY_TYPE,"Latest actually raised tied points wins");
    CreativeChaoEditRules056.apply(tied,0,ChaoState.RUN,10);
    check(tied.prospectivePrimaryType()==ChaoState.FLY_TYPE,"Same level no-op cannot re-order training history");
    var highGrade=new ChaoState();highGrade.intelligenceGrade=255;
    check(CreativeChaoEditRules056.apply(highGrade,0,CreativeChaoEditRules056.INTELLIGENCE,99)&&highGrade.intelligence==4000,
        "Byte-grade Intelligence earned points saturate at actual cap independently of level99");
    check(CreativeChaoEditRules056.apply(highGrade,0,CreativeChaoEditRules056.LUCK,99)&&highGrade.luck==1287,
        "Luck uses its own grade and 13-point base gain, not 4000 as a level range");
    var adult=new ChaoState();adult.adult=true;adult.align=-.802f;adult.primaryType=ChaoState.RUN_TYPE;adult.setEvolutionStrength(.6f);
    adult=ChaoState.decode(adult.encode());String before=adult.encode();
    check(!CreativeChaoEditRules056.apply(adult,100,ChaoState.RUN,99)&&adult.encode().equals(before),"Conflicting adult alignment rejects entire transaction");
    check(CreativeChaoEditRules056.apply(adult,-80,ChaoState.RUN,99)&&adult.lockedAlignment()==-.802f&&adult.lockedPrimaryType()==ChaoState.RUN_TYPE,
        "Displayed locked adult alignment permits stat editing without changing precise saved identity");
    check(adult.evolutionStrength()==.6f,"Stat command cannot reduce biological age");
    for(int stat:new int[]{-1,4,7,Integer.MAX_VALUE}) {
      var s=new ChaoState();String encoded=s.encode();
      check(!CreativeChaoEditRules056.apply(s,0,stat,9)&&s.encode().equals(encoded),"Unlisted stat rejected atomically");
    }
    for(int level:new int[]{-1,100,Integer.MIN_VALUE,Integer.MAX_VALUE}) {
      var s=new ChaoState();String encoded=s.encode();
      check(!CreativeChaoEditRules056.apply(s,0,ChaoState.RUN,level)&&s.encode().equals(encoded),"Out of-range level rejected atomically");
    }
    check(CreativeChaoEditRules056.stat("pOwEr")==ChaoState.POWER&&CreativeChaoEditRules056.stat("stamina")==-1,"Supported names case-insensitive; Stamina intentionally absent");
    System.out.println("Creative editing056: "+checks+" checks passed");return checks;
  }
}
