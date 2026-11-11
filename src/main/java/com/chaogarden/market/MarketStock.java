package com.chaogarden.market;

import com.chaogarden.fruit.FruitCatalog;
import com.chaogarden.genes.TrainingSupplyRules;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/** Source ring prices adapted at 100 rings per emerald; stock remains a rotating selection. */
public final class MarketStock {
  public record Listing(String itemId, int emeralds, int count, int uses) {
    public Listing {
      if (!itemId.matches("[a-z][a-z0-9_]*") || emeralds < 1 || emeralds > 1152
          || count < 1 || count > 64 || uses < 1 || (emeralds > 576 && emeralds % 9 != 0))
        throw new IllegalArgumentException("Invalid Black Market listing");
    }
  }
  /** Every vanilla merchant input is at most one stack. Blocks are ordinary emerald currency. */
  public record Payment(boolean blocks, int count) {
    public Payment { if (count < 1 || count > 64) throw new IllegalArgumentException("Invalid payment stack"); }
    public int emeraldValue() { return count * (blocks ? 9 : 1); }
  }
  public record Product(String itemId, int sourceRings, int uses) {
    public Listing listing() { return new Listing(itemId, emeraldPrice(sourceRings), 1, uses); }
  }
  public static final List<String> TOYS = List.of("chao_trumpet", "chao_drum", "chao_jack_in_box", "chao_radio", "chao_tv",
      "chao_rocking_horse", "chao_piano", "chao_organ", "chao_rubber_duck", "chao_lifesaver",
      "chao_coffin", "chao_triangle", "chao_guitar", "chao_accordion", "chao_bubble_wand");
  public static final List<String> GARDEN_TOOLS=List.of("chao_shovel","chao_watering_can");
  public static final List<String> LEGACY_TOYS = List.of("chao_ball", "chao_rattle", "chao_maracas");
  /** Classroom rewards: source has no buy price, so these never enter market stock. */
  public static final List<String> LESSON_TOYS=List.of("chao_bell","chao_castanets","chao_cymbals","chao_flute","chao_tambourine");
  /** All four training drives are always available at a low, fixed price. */
  public static final List<String> DRIVES = List.of("swim_drive", "fly_drive", "run_drive", "power_drive");
  public static final List<String> HATS = List.of("apple_hat", "cardboard_box_hat", "pan_hat", "paper_bag_hat",
      "empty_can_hat", "stump_hat", "flower_pot_hat", "bucket_hat", "watermelon_hat", "red_wool_beanie",
      "blue_wool_beanie", "black_white_wool_beanie");
  public static final List<String> COLORED_EGGS = List.of("white_egg", "red_egg", "yellow_egg", "blue_egg",
      "sky_blue_egg", "pink_egg", "orange_egg", "brown_egg", "purple_egg", "green_egg", "grey_egg", "lime_egg", "black_egg", "aqua_egg", "cyan_egg");
  public static final List<String> BASE_EGGS = java.util.stream.Stream.concat(
      java.util.stream.Stream.of("normal_egg"), COLORED_EGGS.stream()).toList();
  public static final List<String> SHINY_EGGS = java.util.stream.Stream.concat(
      java.util.stream.Stream.of("shiny_egg"), COLORED_EGGS.stream().map(x -> "shiny_" + x)).toList();
  public static final List<String> JEWEL_EGGS = List.of("silver_jewel_egg", "gold_jewel_egg", "ruby_jewel_egg",
      "sapphire_jewel_egg", "amethyst_jewel_egg", "emerald_jewel_egg", "moon_jewel_egg",
      "aquamarine_jewel_egg", "garnet_jewel_egg", "onyx_jewel_egg", "diamond_jewel_egg", "peridot_jewel_egg", "topaz_jewel_egg");
  public static final List<String> SHAPED_FRUITS = List.of("round_fruit", "square_fruit", "triangle_fruit");
  public static final List<String> OTHER_FRUITS = List.of("hero_garden_fruit", "dark_garden_fruit", "chao_fruit", "heart_fruit", "mushroom", "mushroom_alt");
  public static final List<String> SEEDS = List.of("tasty_fruit_seed", "round_fruit_seed", "square_fruit_seed",
      "triangle_fruit_seed", "dark_garden_fruit_seed", "hero_garden_fruit_seed", "strong_fruit_seed");
  public static final List<String> ACCESSORIES=List.of("baseball_cap_accessory","bowtie_accessory","round_glasses_accessory",
      "aviators_accessory","headphones_accessory","pacifier_accessory","press_hat_accessory","blue_hoodie_accessory","pink_hoodie_accessory","cheese_bowtie_accessory");
  public static final List<String> GIFTS=List.of("clear_lens","blue_lens","green_lens","red_lens","yellow_lens","purple_lens","pink_lens","orange_lens","cyan_lens","hero_lens","dark_lens","chaos_lens",
      "cosmetic_wings","reverse_mirror","suspicious_potion","normal_hero_brush","swim_hero_brush","fly_hero_brush","run_hero_brush","power_hero_brush","omochao_parts_box");
  public static final List<String> ORDINARY_GENES=TrainingSupplyRules.ORDINARY_GENES;
  public static final List<String> MYTHICAL_GENES=TrainingSupplyRules.MYTHICAL_GENES;
  public static final int OFFER_COUNT=42;
  public static final int GEM_RESTOCK_DENOMINATOR=1000;
  private static final Map<String, Product> CATALOG;
  static {
    var p = new LinkedHashMap<String, Product>();
    add(p,"chao_key",400,Integer.MAX_VALUE); // Four emeralds; permanent survival access.
    for(String id:DRIVES)add(p,id,100,Integer.MAX_VALUE);
    for(String id:ORDINARY_GENES)add(p,id,100,4);
    for(String id:MYTHICAL_GENES)add(p,id,200,2);
    add(p, "normal_egg", 200, Integer.MAX_VALUE); // Explicit exception requested; source has no purchase price.
    int[] eggs = {400,500,500,500,600,600,600,800,600,800,1000,1500,2000,600,800};
    int[] shiny = {4000,5000,5000,5000,6000,6000,6000,8000,8000,8000,10000,15000,20000,6000,8000};
    add(p,"shiny_egg",1000,1);
    for (int i=0;i<COLORED_EGGS.size();i++) { add(p,COLORED_EGGS.get(i),eggs[i],Integer.MAX_VALUE); add(p,"shiny_"+COLORED_EGGS.get(i),shiny[i],1); }
    // Dreamcast prices were Emblems, not rings. These are explicit Minecraft rarity prices.
    int[] jewels = {5000,7500,10000,10000,10000,10000,15000,10000,10000,10000,15000,10000,10000};
    for (int i=0;i<JEWEL_EGGS.size();i++) add(p,JEWEL_EGGS.get(i),jewels[i],1);
    for (String fruit : SHAPED_FRUITS) add(p,fruit,80,8);
    add(p,"hero_garden_fruit",120,8); add(p,"dark_garden_fruit",120,8); add(p,"chao_fruit",200,6);
    add(p,"heart_fruit",300,4); add(p,"mushroom",300,6); add(p,"mushroom_alt",300,2);
    // Portable Chao Adventure 2 foods have no SA2 Battle shop ring price.
    // Eight emeralds is an explicit Minecraft discovery price for this pool.
    for (String fruit : FruitCatalog.ADVENTURE) add(p,fruit,800,4);
    int[] seeds = {200,300,300,300,400,400,500};
    for (int i=0;i<SEEDS.size();i++) add(p,SEEDS.get(i),seeds[i],8);
    int[] hats = {2000,2000,2000,4000,4000,4000,6000,6000,6000,8000,10000,20000};
    for (int i=0;i<HATS.size();i++) add(p,HATS.get(i),hats[i],2);
    // Additional toys have explicit Minecraft prices; the source unlocks these as rewards.
    int[] toys = {5000,15000,30000,60000,100000, 15000,20000,25000,3000,4000,20000,5000,10000,10000,3000};
    add(p,"chao_shovel",2000,Integer.MAX_VALUE);
    add(p,"chao_watering_can",2500,Integer.MAX_VALUE);
    for (int i=0;i<TOYS.size();i++) add(p,TOYS.get(i),toys[i],1);
    int[] cwePrices={500,30,60,55,50,30,55,70,3000,20000,20000,20000,20000};
    for(int i=0;i<FruitCatalog.CWE.size();i++)add(p,FruitCatalog.CWE.get(i),cwePrices[i],i>=9?1:4);
    for(String id:ACCESSORIES)add(p,id,2500,2);
    for(String id:GIFTS)add(p,id,id.equals("cosmetic_wings")?500:id.equals("reverse_mirror")?2000:id.equals("suspicious_potion")?50000:id.equals("omochao_parts_box")?2000:1000,2);
    CATALOG = Collections.unmodifiableMap(p);
  }
  private MarketStock() {}
  private static void add(Map<String, Product> p, String id, int rings, int uses) { p.put(id,new Product(id,rings,uses)); }
  public static Map<String, Product> catalog() { return CATALOG; }
  public static int emeraldPrice(int rings) {
    int price = (int)Math.max(1,((long)rings+99)/100);
    // Beyond 64 blocks, two block stacks avoid requiring an impossible third payment input.
    return price > 576 ? ((price+8)/9)*9 : price;
  }
  public static List<Payment> payment(int emeralds) {
    if (emeralds < 1 || emeralds > 1152 || (emeralds > 576 && emeralds % 9 != 0))
      throw new IllegalArgumentException("Unrepresentable emerald payment");
    if (emeralds <= 64) return List.of(new Payment(false,emeralds));
    int blocks=emeralds/9, remainder=emeralds%9;
    if (blocks > 64) return List.of(new Payment(true,64),new Payment(true,blocks-64));
    return remainder == 0 ? List.of(new Payment(true,blocks))
        : List.of(new Payment(true,blocks),new Payment(false,remainder));
  }
  public static List<Listing> select(long seed, List<String> ignoredSeeds, List<String> ignoredGenes) {
    return select(seed,ignoredSeeds,ignoredGenes,false);
  }
  public static List<Listing> select(long seed,List<String> ignoredSeeds,List<String> ignoredGenes,boolean moonOnlyOnMoon) {
    var random = new Random(seed);
    var ids = new ArrayList<String>(BASE_EGGS);
    ids.addAll(DRIVES);
    ids.addAll(GARDEN_TOOLS);
    ids.add("chao_key");
    ids.addAll(sample(ORDINARY_GENES,3,random));
    ids.addAll(sample(MYTHICAL_GENES,1,random));
    ids.addAll(sample(SHINY_EGGS,1,random));
    if(random.nextInt(GEM_RESTOCK_DENOMINATOR)==0)
      ids.addAll(sample(JEWEL_EGGS.stream().filter(x->!moonOnlyOnMoon || !x.equals("moon_jewel_egg")).toList(),1,random));
    ids.addAll(sample(SHAPED_FRUITS,2,random)); ids.addAll(sample(OTHER_FRUITS,1,random));
    ids.add("strong_fruit_seed");
    ids.addAll(sample(SEEDS.stream().filter(x->!x.equals("strong_fruit_seed")).toList(),2,random));
    ids.addAll(sample(HATS,2,random)); ids.addAll(sample(TOYS,2,random));
    ids.addAll(sample(FruitCatalog.ADVENTURE,1,random));
    ids.addAll(sample(FruitCatalog.CWE,1,random));
    ids.addAll(sample(ACCESSORIES,1,random));ids.addAll(sample(GIFTS,1,random));
    return ids.stream().map(x->CATALOG.get(x).listing()).toList();
  }
  public static boolean permanent(String itemId) { return itemId.equals("chao_key")||BASE_EGGS.contains(itemId)||DRIVES.contains(itemId)||GARDEN_TOOLS.contains(itemId); }

  /** Saved current-version stock must also obey newly loaded space policy. */
  public static boolean validStock(List<String> ids,boolean moonOnlyOnMoon) {
    if(ids==null || (ids.size()!=OFFER_COUNT && ids.size()!=OFFER_COUNT+1)
        ||ids.stream().distinct().count()!=ids.size() || !ids.containsAll(BASE_EGGS)
        ||!ids.contains("chao_key")||!ids.containsAll(GARDEN_TOOLS)||!ids.containsAll(DRIVES) || !ids.contains("strong_fruit_seed") || !ids.stream().allMatch(CATALOG::containsKey)
        ||(moonOnlyOnMoon && ids.contains("moon_jewel_egg")))return false;
    return count(ids,SHINY_EGGS)==1 && count(ids,JEWEL_EGGS)==ids.size()-OFFER_COUNT
        &&count(ids,SHAPED_FRUITS)==2 &&count(ids,OTHER_FRUITS)==1 &&count(ids,SEEDS)==3
        &&count(ids,ORDINARY_GENES)==3 && count(ids,MYTHICAL_GENES)==1
        &&count(ids,HATS)==2 &&count(ids,TOYS)==2 &&count(ids,FruitCatalog.ADVENTURE)==1
        &&count(ids,FruitCatalog.CWE)==1 &&count(ids,ACCESSORIES)==1 &&count(ids,GIFTS)==1;
  }
  private static long count(List<String> ids,List<String> category) {
    return ids.stream().filter(category::contains).count();
  }
  private static List<String> sample(List<String> ids, int count, Random random) {
    var pool = new ArrayList<>(ids); Collections.shuffle(pool,random);
    return pool.subList(0,Math.min(count,pool.size()));
  }
}
