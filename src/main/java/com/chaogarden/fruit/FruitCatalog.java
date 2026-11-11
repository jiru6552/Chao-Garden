package com.chaogarden.fruit;

import java.util.ArrayList;
import java.util.List;

/** SA2 fruit sources. World discovery is deliberately adapted to Minecraft. */
public final class FruitCatalog {
  public static final List<String> NATURAL = List.of("garden_fruit", "hero_garden_fruit", "dark_garden_fruit");
  public static final List<String> FARMER_FRUITS = List.of("garden_fruit", "hero_garden_fruit",
      "dark_garden_fruit", "strong_fruit", "tasty_fruit");
  public static final List<String> MARKET = List.of("round_fruit",
      "triangle_fruit", "square_fruit", "heart_fruit", "chao_fruit", "mushroom", "mushroom_alt");
  /** Confirmed additional Chao Adventure 2 profiles; overlapping modern foods keep SA2B effects. */
  public static final List<String> ADVENTURE = List.of("energy_fruit", "peace_fruit", "smart_fruit",
      "tall_fruit", "vitality_fruit", "rare_fruit", "wonder_fruit", "creepy_fruit",
      "rough_fruit", "forbidden_fruit");
  /** CWE and Tiny Chao Garden foods are discovered through rotating market stock. */
  public static final List<String> CWE = List.of("orange_fruit", "gba_orange_fruit", "gba_blue_fruit",
      "gba_pink_fruit", "gba_green_fruit", "gba_purple_fruit", "gba_yellow_fruit", "gba_red_fruit",
      "shiny_fruit", "hyper_swim_fruit", "hyper_fly_fruit", "hyper_run_fruit", "hyper_power_fruit");
  public static final List<String> LEGACY = List.of("mint_candy", "stamina_fruit");
  /** Renamed foods/trees resolve at load time, without duplicate registry entries. */
  public static final java.util.Map<String,String> REPLACEMENTS = java.util.Map.of(
      "hero_fruit", "hero_garden_fruit", "dark_fruit", "dark_garden_fruit");
  public static String canonical(String id) {
    if(id==null)return null;
    for(var entry:REPLACEMENTS.entrySet())
      if(id.equals(entry.getKey())||id.equals(entry.getKey()+"_seed"))
        return entry.getValue()+id.substring(entry.getKey().length());
    return id;
  }
  public static final List<String> ACTIVE;
  /** Saved orchard blocks remain loadable even when their fruit is no longer introduced. */
  public static final List<String> TREES;
  static {
    var active = new ArrayList<>(FARMER_FRUITS); active.addAll(MARKET); active.addAll(ADVENTURE); active.addAll(CWE); ACTIVE = List.copyOf(active);
    var trees = new ArrayList<>(ACTIVE); trees.add("stamina_fruit");
    TREES = List.copyOf(trees);
  }
  private FruitCatalog() {}
  public static boolean isNatural(String fruit) { return NATURAL.contains(fruit); }
  public static boolean isMarket(String fruit) { return MARKET.contains(fruit) || ADVENTURE.contains(fruit) || CWE.contains(fruit); }
  public static boolean isMushroom(String fruit) { return fruit.equals("mushroom") || fruit.equals("mushroom_alt"); }
  public static boolean isLegacyItem(String id) {
    if (id.equals("mushroom_seed") || id.equals("mushroom_alt_seed")) return true;
    return LEGACY.stream().anyMatch(fruit -> id.equals(fruit) || id.equals(fruit+"_seed"));
  }
  public static List<String> naturalSeeds() { return NATURAL.stream().map(x -> x+"_seed").toList(); }
  public static List<String> registeredFoods() {
    var foods=new ArrayList<>(ACTIVE); foods.addAll(LEGACY); return List.copyOf(foods);
  }
}
