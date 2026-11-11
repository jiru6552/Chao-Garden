package com.chaogarden.distribution;

import com.chaogarden.core.ChaoState;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Exhaustive category selection and eligibility boundaries without a game bootstrap. */
public final class EggDistributionRulesTest {
  private static int checks;
  private static void check(boolean value,String message){checks++;if(!value)throw new AssertionError(message);}
  public static int run() {
    checks=0;
    check(EggDistributionRules.COLORS.equals(Arrays.asList(ChaoState.COLORS)),"Base catalog drifted from native coats");
    check(EggDistributionRules.GEMS.equals(Arrays.asList(ChaoState.JEWELS).subList(1,14)),"Gem catalog drifted from native jewels");
    check(EggDistributionRules.BASE_CHANCE==.0025&&EggDistributionRules.SHINY_CHANCE==.001
        &&EggDistributionRules.GEM_CHANCE==.0002&&EggDistributionRules.DIMENSION_CHANCE==.01,"Rarity policy changed");
    Map<String,Integer> tickets=new HashMap<>();
    for(int i=0;i<81;i++){
      final int draw=i;
      String egg=EggDistributionRules.baseEgg(bound->{check(bound==81,"Base draw bound");return draw;});
      tickets.merge(egg,1,Integer::sum);
    }
    check(tickets.size()==16,"Every base color must be discoverable");
    for(String color:ChaoState.COLORS)check(tickets.get(color+"_egg")== (color.equals("normal")?6:5),"Normal must be slightly more frequent than each color");
    for(int i=0;i<16;i++){
      final int draw=i;
      String egg=EggDistributionRules.shinyEgg(bound->{check(bound==16,"Shiny bound");return draw;});
      check(egg.equals(i==0?"shiny_egg":"shiny_"+ChaoState.COLORS[i]+"_egg"),"Every shiny is reachable");
    }
    for(boolean space:new boolean[]{false,true}){
      int count=space?12:13;
      for(int i=0;i<count;i++){
        final int draw=i;
        String egg=EggDistributionRules.gemEgg(space,false,bound->{check(bound==count,"Gem bound excludes only Moon");return draw;});
        check(egg.equals(ChaoState.JEWELS[i+1]+"_jewel_egg"),"Gem catalog coverage");
        if(space)check(!egg.equals("moon_jewel_egg"),"Space policy leaked Moon to ordinary structures");
      }
    }
    check(EggDistributionRules.gemEgg(true,true,bound->{throw new AssertionError("Moon roll must not pick another gem");}).equals("moon_jewel_egg"),"Moon is exclusive on configured moon");
    var shiny=List.of("chests/buried_treasure","chests/ancient_city","chests/stronghold_corridor","chests/stronghold_library",
        "chests/stronghold_crossing","chests/trial_chambers/reward","chests/trial_chambers/corridor","chests/desert_pyramid","chests/jungle_temple");
    for(String table:shiny){check(EggDistributionRules.shinySite("minecraft:"+table),"Missing shiny site "+table);
      check(EggDistributionRules.shinyEligible("minecraft:overworld","minecraft:"+table),"Overworld shiny site");}
    for(String table:List.of("chests/village/village_tannery","chests/simple_dungeon","chests/shipwreck_treasure","gameplay/fishing"))
      check(!EggDistributionRules.shinyEligible("minecraft:overworld","minecraft:"+table),"Overworld shiny leakage");
    for(String table:List.of("chests/ancient_city","chests/desert_pyramid","chests/stronghold_library","chests/stronghold_crossing",
        "chests/bastion_other","chests/bastion_treasure","chests/bastion_bridge","chests/end_city_treasure"))
      check(EggDistributionRules.gemSite("minecraft:"+table),"Missing gem site");
    for(String table:List.of("chests/jungle_temple","chests/buried_treasure","chests/nether_bridge","chests/trial_chambers/reward","chests/simple_dungeon"))
      check(!EggDistributionRules.gemSite("minecraft:"+table),"Gems leaked to unlisted site");
    for(String dimension:List.of("minecraft:the_nether","minecraft:the_end","example:custom_dimension"))
      check(EggDistributionRules.shinyEligible(dimension,"example:any_id"),"Every other dimension permits chest shinies");
    for(String moon:EggDistributionRules.DEFAULT_MOON_DIMENSIONS)check(EggDistributionRules.moon(moon,EggDistributionRules.DEFAULT_MOON_DIMENSIONS),"Known surface Moon");
    for(String notMoon:List.of("ad_astra:moon_orbit","northstar:moon_orbit","stellaris:mars","other:moon","minecraft:overworld"))
      check(!EggDistributionRules.moon(notMoon,EggDistributionRules.DEFAULT_MOON_DIMENSIONS),"Moon match must be exact and configurable");
    check(EggDistributionRules.moon("custom:luna",List.of("custom:luna")),"Unknown space mods have exact configurable support");
    return checks;
  }
}
