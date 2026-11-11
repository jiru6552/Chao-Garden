package com.chaogarden.fruit;

import com.chaogarden.core.ChaoState;

/** Four-bite SA2 fruit totals, applied once when Minecraft consumes one item. */
public final class FruitEffects {
  private FruitEffects() {}
  public static final String[] HYPER={"hyper_swim_fruit","hyper_fly_fruit","hyper_run_fruit","hyper_power_fruit"};
  // Tiny Chao Garden per-bite bars from Chao Island, multiplied by four for a console-sized item.
  private static final int[][] GBA={{30,-20,-20,30,10},{20,50,-10,-10,30},{40,-30,40,-30,20},
      {0,-10,30,40,20},{-20,30,30,-20,10},{-30,40,-30,40,20},{30,10,30,20,-50}};
  public static boolean canUse(ChaoState state,String fruit){
    fruit=FruitCatalog.canonical(fruit);
    if(state.dead)return false;
    if(fruit.equals("shiny_fruit"))return state.shiny&&state.twoTone;
    for(int i=0;i<HYPER.length;i++)if(HYPER[i].equals(fruit)){
      if("omochao".equals(state.character))return false;
      if(state.grades[i]!=5)return false;for(int j=0;j<state.grades.length;j++)if(state.grades[j]==6)return false;return true;
    }
    return true;
  }
  /** CWE's optional stronger-food code; ordinary SA2/CA2 effects remain the default. */
  public static void apply(ChaoState state,String fruit,boolean cweRebalance){
    fruit=FruitCatalog.canonical(fruit);
    if(!cweRebalance){apply(state,fruit);return;}
    if(!canUse(state,fruit))return;
    int gba=FruitCatalog.CWE.indexOf(fruit)-1;
    if(gba>=0&&gba<GBA.length){for(int stat=0;stat<4;stat++)state.train(stat,GBA[gba][stat]*8);state.feed(ChaoState.STAMINA,GBA[gba][4]*8,0);return;}
    switch(fruit){
      case "strong_fruit" -> state.feed(ChaoState.STAMINA,60,0);
      case "tasty_fruit" -> {state.feed(ChaoState.STAMINA,30,0);state.trainHidden(ChaoState.HIDDEN_LUCK,60);}
      case "round_fruit","square_fruit","triangle_fruit" -> {state.feed(ChaoState.STAMINA,80,0);state.feedFruit(fruit.equals("round_fruit")?0:fruit.equals("square_fruit")?1:2);}
      case "smart_fruit" -> {state.feed(ChaoState.STAMINA,30,0);state.trainHidden(ChaoState.HIDDEN_INTELLIGENCE,320);}
      case "mushroom","mushroom_alt" -> {state.feed(ChaoState.STAMINA,fruit.equals("mushroom")?160:320,0);state.trainHidden(ChaoState.HIDDEN_LUCK,fruit.equals("mushroom")?50:100);}
      case "mint_candy" -> {for(int stat=0;stat<4;stat++)state.train(stat,200);state.feed(ChaoState.STAMINA,50,0);}
      default -> apply(state,fruit);
    }
  }
  public static void apply(ChaoState state, String fruit) {
    fruit=FruitCatalog.canonical(fruit);
    if(!canUse(state,fruit))return;
    int gba=FruitCatalog.CWE.indexOf(fruit)-1;
    if(gba>=0&&gba<GBA.length){
      for(int stat=0;stat<4;stat++)state.train(stat,GBA[gba][stat]*4);
      state.feed(ChaoState.STAMINA,GBA[gba][4]*4,0);return;
    }
    for(int i=0;i<HYPER.length;i++)if(HYPER[i].equals(fruit)){state.useHyperFruit(i);state.feed(ChaoState.STAMINA,40,0);return;}

    switch (fruit) {
      case "orange_fruit" -> state.feed(ChaoState.STAMINA,40,0);
      case "shiny_fruit" -> {state.brightCoat=!state.brightCoat;state.feed(ChaoState.STAMINA,40,0);}
      case "strong_fruit" -> state.feed(ChaoState.STAMINA, 50, 0);
      case "tasty_fruit" -> state.feed(ChaoState.STAMINA, 30, 0);
      case "hero_garden_fruit" -> state.feed(ChaoState.STAMINA, 40, .15f);
      case "dark_garden_fruit" -> state.feed(ChaoState.STAMINA, 40, -.15f);
      case "round_fruit", "square_fruit", "triangle_fruit" -> {
        state.feed(ChaoState.STAMINA, 40, 0);
        state.feedFruit(fruit.equals("round_fruit") ? 0 : fruit.equals("square_fruit") ? 1 : 2);
      }
      case "chao_fruit" -> {
        for (int i = 0; i < ChaoState.STAMINA; i++) state.train(i, 50);
        state.feed(ChaoState.STAMINA, 50, 0);
      }
      case "mushroom", "mushroom_alt" -> {
        boolean alt = fruit.equals("mushroom_alt");
        state.feed(ChaoState.STAMINA, alt ? 80 : 50, 0);
        state.trainHidden(ChaoState.HIDDEN_LUCK,alt ? 100 : 50);
      }
      case "energy_fruit", "peace_fruit" -> {
        state.feed(ChaoState.STAMINA, 40, 0);
        state.shiftAdventurePersonality(fruit.equals("energy_fruit") ? 10 : -10);
      }
      // Existing inventories keep their prior effects; these items are no longer introduced.
      case "stamina_fruit" -> state.feed(ChaoState.STAMINA, 60, 0);
      case "smart_fruit" -> {
        state.feed(ChaoState.STAMINA, 40, 0);
        state.intelligence = Math.min(4000, state.intelligence + 10);
      }
      case "mint_candy" -> {
        state.feed(ChaoState.STAMINA, 15, 0);
        state.energy = Math.min(100, state.energy + 40);
      }
      default -> state.feed(ChaoState.STAMINA, 40, 0);
    }
  }
  private static String signed(int v){return (v>0?"+":"")+v;}
  /** Describes the same synchronized server food profile that consumption uses. */
  public static String benefit(String fruit,boolean cweRebalance) {
    fruit=FruitCatalog.canonical(fruit);
    if(!cweRebalance)return benefit(fruit);
    int gba=FruitCatalog.CWE.indexOf(fruit)-1;
    if(gba>=0&&gba<GBA.length){var b=GBA[gba];return "Swim "+signed(b[0]*8)+"%, Fly "+signed(b[1]*8)+"%, Run "+signed(b[2]*8)+"%, Power "+signed(b[3]*8)+"%, Stamina "+signed(b[4]*8)+"%; attracts DNA samples on the ground";}
    return switch(fruit) {
      case "strong_fruit" -> "Stamina +60% per whole fruit";
      case "tasty_fruit" -> "Stamina +30%, luck +60% training per whole fruit";
      case "round_fruit","square_fruit","triangle_fruit" -> "Stamina +80%; matching personality fruit";
      case "smart_fruit" -> "Stamina +30%; intelligence +320% training";
      case "mushroom" -> "Stamina +160%, luck +50% training per whole mushroom";
      case "mushroom_alt" -> "Stamina +320%, luck +100% training per whole mushroom";
      case "mint_candy" -> "Swim, Fly, Run and Power +200%; Stamina +50%";
      default -> benefit(fruit);
    };
  }
  public static String benefit(String fruit) {
    fruit=FruitCatalog.canonical(fruit);
    int gba=FruitCatalog.CWE.indexOf(fruit)-1;
    if(gba>=0&&gba<GBA.length){var b=GBA[gba];return "Swim "+signed(b[0]*4)+"%, Fly "+signed(b[1]*4)+"%, Run "+signed(b[2]*4)+"%, Power "+signed(b[3]*4)+"%, Stamina "+signed(b[4]*4)+"%; attracts DNA samples on the ground";}
    for(int i=0;i<HYPER.length;i++)if(HYPER[i].equals(fruit))return "Upgrades "+ChaoState.STATS[i]+" S to X; one X grade per life";
    return switch (fruit) {
      case "orange_fruit" -> "Stamina +40%; attracts bee, ladybug or scorpion DNA on the ground";
      case "shiny_fruit" -> "Toggles the Bright coat of a shiny two-tone Chao";
      case "strong_fruit" -> "Stamina +50% per whole fruit";
      case "tasty_fruit" -> "Stamina +30% per whole fruit";
      case "chao_fruit" -> "All five abilities +50% per whole fruit";
      case "mushroom" -> "Stamina +50%, luck +50% per whole mushroom";
      case "mushroom_alt" -> "Stamina +80%, luck +100% per whole mushroom";
      case "hero_garden_fruit" -> "Stamina +40%; shifts children toward Hero";
      case "dark_garden_fruit" -> "Stamina +40%; shifts children toward Dark";
      case "heart_fruit" -> "Stamina +40%; enables mating for eligible adults";
      case "stamina_fruit" -> "Legacy fruit: Stamina +60%";
      case "smart_fruit" -> "Stamina +40%; intelligence +10";
      case "energy_fruit" -> "Stamina +40%; encourages curiosity, activity and naivety";
      case "peace_fruit" -> "Stamina +40%; encourages a calmer, carefree disposition";
      case "rare_fruit", "wonder_fruit" -> "Stamina +40%; Dark-aligned Chao dislike this fruit";
      case "creepy_fruit", "rough_fruit", "forbidden_fruit" -> "Stamina +40%; Hero-aligned Chao dislike this fruit";
      case "tall_fruit", "vitality_fruit" -> "Stamina +40%; ordinary nourishment without an extra ability bonus";
      case "mint_candy" -> "Legacy candy: Stamina +15%, energy +40";
      default -> "Stamina +40% per whole fruit";
    };
  }
}
