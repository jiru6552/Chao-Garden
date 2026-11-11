package com.chaogarden.genes;

import java.util.Arrays;
import java.util.Random;

/** Exhaustive ticket accounting plus independent seeded selection checks. */
public final class TrainingSupplyRulesTest {
  private static int checks;
  private static void check(boolean pass,String message){checks++;if(!pass)throw new AssertionError(message);}
  public static int run(){
    checks=0;check(TrainingSupplyRules.MYTHICAL.equals(java.util.List.of("dragon","unicorn","phoenix")),"Exact mythical source catalog");check(TrainingSupplyRules.ANIMALS.size()==31,"Complete animal sample catalog");
    check(TrainingSupplyRules.ANIMALS.stream().distinct().count()==31,"No duplicate sample identities");
    check(TrainingSupplyRules.ORDINARY_GENES.size()==28&&TrainingSupplyRules.MYTHICAL_GENES.size()==3,"Ordinary/mythical categories partition all samples");
    int[] counts=new int[31];
    for(int t=0;t<90;t++){
      final int ticket=t;int index=TrainingSupplyRules.geneIndex(bound->{check(bound==90,"Weighted ticket bound");return ticket;});
      check(index>=0&&index<31,"Valid sample index");counts[index]++;
    }
    for(int i=0;i<31;i++)check(counts[i]==(TrainingSupplyRules.MYTHICAL.contains(TrainingSupplyRules.ANIMALS.get(i))?2:3),"Each mythical sample is mildly rarer than each ordinary sample");
    check(Arrays.stream(counts).sum()==90,"All tickets conserved");
    var random=new Random(24024);int[] observed=new int[31];
    for(int n=0;n<90000;n++)observed[TrainingSupplyRules.geneIndex(random::nextInt)]++;
    for(int i=0;i<31;i++){
      int expected=counts[i]*1000;
      check(observed[i]>expected*.85&&observed[i]<expected*1.15,"Every sample remains reachable with modest rarity");
    }
    check(TrainingSupplyRules.DRIVES.size()==4&&TrainingSupplyRules.DRIVES.stream().distinct().count()==4,"Four uniform drive choices");
    return checks;
  }
}
