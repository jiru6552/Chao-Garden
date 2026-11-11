package com.chaogarden.activities;

import com.chaogarden.core.ChaoState;
import java.util.*;

/** Saved victory journals must conserve physical prizes across reload and replay. */
public final class ActivityRewardsTest {
  private static int checks;
  private static void check(boolean value,String message){checks++;if(!value)throw new AssertionError(message);}
  public static int run(){
    checks=0;
    for(String mode:List.of("race","karate")){
      ChaoState state=new ChaoState();Set<String> issued=new HashSet<>();
      for(int wins=1;wins<=15;wins++){
        var prizes=ActivityRewards.recordVictory(state,mode,wins);
        for(String prize:prizes)check(issued.add(prize),"A saved milestone duplicated "+prize);
        check(state.intTrait("contest."+mode+".wins")==wins,"Victory count was not committed");
        check(ActivityRewards.recordVictory(state,mode,wins).isEmpty(),"Victory replay duplicated prizes");
        state=ChaoState.decode(state.encode());
        check(ActivityRewards.recordVictory(state,mode,wins-1).isEmpty(),"Stale total duplicated prizes after reload");
        check(ActivityRewards.medals(state).size()==(wins>=10?4:wins>=5?3:wins>=3?2:1),"Incorrect medal tier count");
        check(state.trait("medal.selected").equals(mode+"_bronze"),"An earned medal silently replaced the selected medal");
      }
      state.reincarnate();
      check(ActivityRewards.recordVictory(state,mode,15).isEmpty(),"Rebirth replayed lifetime milestones");
      check(ActivityRewards.medals(state).size()==4,"Rebirth lost medals");
    }
    ChaoState dark=new ChaoState();dark.align=-1;
    var darkPrizes=ActivityRewards.recordVictory(dark,"race",10);
    check(darkPrizes.containsAll(List.of("chao_organ","chao_coffin")),"Dark milestone props missing");
    check(!darkPrizes.contains("chao_piano"),"Dark Chao received the ordinary piano");
    check(dark.intTrait("reward.wrench")==1,"Assembly ability not unlocked");
    check(ActivityRewards.medals(dark).size()==4,"Migrated ten-win journal did not credit all medal tiers");
    ChaoState full=new ChaoState();int index=0;
    while(full.setTrait("fixture."+index,"x"))index++;
    String before=full.encode();
    check(ActivityRewards.recordVictory(full,"race",10).isEmpty(),"Full journal issued unrecorded prizes");
    check(full.encode().equals(before),"Full journal partially committed a milestone");
    for(int i=0;i<20;i++)full.setTrait("fixture."+i,"");
    check(!ActivityRewards.recordVictory(full,"race",10).isEmpty(),"Recoverable journal lost pending credit");
    check(ActivityRewards.recordVictory(full,"race",0).isEmpty(),"Zero wins granted prizes");
    boolean rejected=false;try{ActivityRewards.recordVictory(full,"unknown",1);}catch(IllegalArgumentException expected){rejected=true;}
    check(rejected,"Unknown activity accepted");
    return checks;
  }
}
