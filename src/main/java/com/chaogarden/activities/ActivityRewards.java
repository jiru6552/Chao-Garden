package com.chaogarden.activities;

import com.chaogarden.core.ChaoState;
import java.util.*;

/** Durable, once-only milestones for player-built courses and karate brackets. */
public final class ActivityRewards {
  public static final String[] TIERS={"bronze","silver","gold","champion"};
  private static final int[] WINS={1,3,5,10};
  private ActivityRewards() {}

  /** The source total comes from the server's existing victory journal. Replays do not grant duplicates. */
  public static List<String> recordVictory(ChaoState state,String mode,int sourceTotal){
    // Preflight the bounded save journal. A full journal must not record a win
    // while silently losing its medal or physical prize forever.
    ChaoState trial=ChaoState.decode(state.encode());
    List<String> prizes;
    try { prizes=recordChecked(trial,mode,sourceTotal); }
    catch (JournalFull ex) { return List.of(); }
    for(var entry:trial.traitEntries().entrySet())
      if(!entry.getValue().equals(state.trait(entry.getKey()))
          && !state.setTrait(entry.getKey(),entry.getValue()))
        throw new IllegalStateException("Preflighted contest journal failed");
    return prizes;
  }
  private static final class JournalFull extends RuntimeException {}
  private static void put(ChaoState state,String key,String value){
    if(!state.setTrait(key,value))throw new JournalFull();
  }
  private static void put(ChaoState state,String key,int value){put(state,key,Integer.toString(value));}
  private static List<String> recordChecked(ChaoState state,String mode,int sourceTotal){
    if(!mode.equals("race")&&!mode.equals("karate"))throw new IllegalArgumentException("Unknown contest");
    String key="contest."+mode+".wins";int wins=Math.max(0,Math.min(1_000_000,sourceTotal));
    if(wins<=state.intTrait(key))return List.of();
    put(state,key,wins);
    String newest="";
    for(int i=0;i<WINS.length;i++)if(wins>=WINS[i]){
      String medal=mode+"_"+TIERS[i];
      put(state,"medal."+medal,1);newest=medal;
    }
    if(state.trait("medal.selected").isBlank()&&!newest.isEmpty())put(state,"medal.selected",newest);
    var items=new ArrayList<String>();
    if(mode.equals("race")){
      grant(state,items,wins>=1,"rattle","chao_rattle");
      grant(state,items,wins>=2,"shovel","chao_shovel");
      grant(state,items,wins>=3,"rubber_duck","chao_rubber_duck");
      grant(state,items,wins>=4,"lifesaver","chao_lifesaver");
      grant(state,items,wins>=5,"rocking_horse","chao_rocking_horse");
      // Wrench is a learned assembly ability, used with four actual parts boxes.
      if(wins>=5)put(state,"reward.wrench",1);
      grant(state,items,wins>=6&&state.align>-.5f,"piano","chao_piano");
      grant(state,items,wins>=6&&state.align<=-.5f,"organ","chao_organ");
    }else{
      grant(state,items,wins>=1,"bubble_wand","chao_bubble_wand");
      grant(state,items,wins>=3,"drum","chao_drum");
      grant(state,items,wins>=5,"trumpet","chao_trumpet");
    }
    return List.copyOf(items);
  }
  private static void grant(ChaoState s,List<String> out,boolean eligible,String reward,String item){
    if(eligible&&s.intTrait("reward."+reward)==0){put(s,"reward."+reward,1);out.add(item);}
  }
  public static List<String> medals(ChaoState state){
    var result=new ArrayList<String>();for(String mode:List.of("race","karate"))for(String tier:TIERS)
      if(state.intTrait("medal."+mode+"_"+tier)>0)result.add(mode+"_"+tier);
    return List.copyOf(result);
  }
}
