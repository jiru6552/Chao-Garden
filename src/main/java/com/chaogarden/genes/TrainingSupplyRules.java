package com.chaogarden.genes;

import java.util.List;
import java.util.function.IntUnaryOperator;

/** Stable training-supply catalogs and mildly reduced mythical sample weights. */
public final class TrainingSupplyRules {
  public static final List<String> DRIVES=List.of("swim_drive","fly_drive","run_drive","power_drive");
  public static final List<String> ANIMALS=List.of("penguin","seal","otter","peacock","parrot","condor",
      "rabbit","cheetah","warthog","bear","tiger","gorilla","skunk","sheep","raccoon",
      "dragon","unicorn","phoenix","half_fish","skeleton_dog","bat","bee","ladybug","scorpion",
      "deer","elephant","kangaroo","koala","lion","mole","swallow");
  public static final List<String> MYTHICAL=List.of("dragon","unicorn","phoenix");
  public static final List<String> ORDINARY_GENES=ANIMALS.stream().filter(n->!MYTHICAL.contains(n)).map(n->n+"_animal_gene").toList();
  public static final List<String> MYTHICAL_GENES=MYTHICAL.stream().map(n->n+"_animal_gene").toList();
  public static final int ORDINARY_WEIGHT=3, MYTHICAL_WEIGHT=2;
  public static final int TOTAL_GENE_WEIGHT=90;
  private TrainingSupplyRules() {}
  public static boolean mythical(String animal){return MYTHICAL.contains(animal);}
  public static boolean mythicalGene(String item){return MYTHICAL_GENES.contains(item);}
  public static int geneWeight(int index){return mythical(ANIMALS.get(index))?MYTHICAL_WEIGHT:ORDINARY_WEIGHT;}
  public static int geneIndex(IntUnaryOperator random){
    int ticket=random.applyAsInt(TOTAL_GENE_WEIGHT);
    for(int i=0;i<ANIMALS.size();i++){ticket-=geneWeight(i);if(ticket<0)return i;}
    throw new IllegalArgumentException("Gene selector returned an out-of-range ticket");
  }
}
