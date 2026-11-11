package com.chaogarden.cwe;

import java.util.*;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;

/** CWE additions: source names/prices where published, explicit Minecraft prices otherwise. */
public final class CweCatalog {
  public enum Kind { LENS, ACCESSORY, WINGS, MIRROR, POTION, BRUSH, PARTS }
  public record Gift(String id, Kind kind, int value, int rings, EquipmentSlot slot, boolean dyable) {}
  private static final Map<String,Gift> GIFTS;
  public static final List<String> GBA_FRUITS=List.of("gba_orange_fruit","gba_blue_fruit","gba_pink_fruit",
      "gba_green_fruit","gba_purple_fruit","gba_yellow_fruit","gba_red_fruit");
  public static final List<String> HYPER_FRUITS=List.of("hyper_swim_fruit","hyper_fly_fruit","hyper_run_fruit","hyper_power_fruit");
  public static final List<String> FRUITS;
  static {
    var fruit=new ArrayList<String>();fruit.add("orange_fruit");fruit.addAll(GBA_FRUITS);fruit.add("shiny_fruit");fruit.addAll(HYPER_FRUITS);FRUITS=List.copyOf(fruit);
    var gifts=new LinkedHashMap<String,Gift>();
    String[] lenses={"clear","blue","green","red","yellow","purple","pink","orange","cyan","hero","dark","chaos"};
    for(int i=0;i<lenses.length;i++)add(gifts,lenses[i]+"_lens",Kind.LENS,i,1000,null,false);
    add(gifts,"cosmetic_wings",Kind.WINGS,0,500,null,false);
    add(gifts,"reverse_mirror",Kind.MIRROR,0,2000,null,false);
    add(gifts,"suspicious_potion",Kind.POTION,0,50000,null,false);
    String[] brush={"normal","swim","fly","run","power"};
    for(int i=0;i<brush.length;i++)add(gifts,brush[i]+"_hero_brush",Kind.BRUSH,i,1000,null,false);
    add(gifts,"omochao_parts_box",Kind.PARTS,0,2000,null,false);
    accessory(gifts,"baseball_cap_accessory",EquipmentSlot.FEET,true);
    accessory(gifts,"bowtie_accessory",EquipmentSlot.LEGS,true);
    accessory(gifts,"round_glasses_accessory",EquipmentSlot.OFFHAND,false);
    accessory(gifts,"aviators_accessory",EquipmentSlot.OFFHAND,false);
    accessory(gifts,"headphones_accessory",EquipmentSlot.FEET,true);
    accessory(gifts,"pacifier_accessory",EquipmentSlot.OFFHAND,true);
    accessory(gifts,"press_hat_accessory",EquipmentSlot.FEET,true);
    accessory(gifts,"blue_hoodie_accessory",EquipmentSlot.CHEST,true);
    accessory(gifts,"pink_hoodie_accessory",EquipmentSlot.CHEST,true);
    accessory(gifts,"cheese_bowtie_accessory",EquipmentSlot.LEGS,true);
    GIFTS=Collections.unmodifiableMap(gifts);
  }
  private CweCatalog() {}
  private static void add(Map<String,Gift> map,String id,Kind kind,int value,int rings,EquipmentSlot slot,boolean dyable){map.put(id,new Gift(id,kind,value,rings,slot,dyable));}
  private static void accessory(Map<String,Gift> map,String id,EquipmentSlot slot,boolean dyable){add(map,id,Kind.ACCESSORY,0,2500,slot,dyable);}
  public static Map<String,Gift> gifts(){return GIFTS;}
  public static Gift gift(String id){return GIFTS.get(id);}
  public static EquipmentSlot slot(String id){var gift=GIFTS.get(id);return gift==null?null:gift.slot();}
  public static boolean isAccessory(ItemStack stack){return stack.getItem() instanceof CweGiftItem item && item.slot()!=null;}
  public static List<String> accessories(){return GIFTS.values().stream().filter(g->g.kind()==Kind.ACCESSORY).map(Gift::id).toList();}
  public static List<String> specialGifts(){return GIFTS.values().stream().filter(g->g.kind()!=Kind.ACCESSORY).map(Gift::id).toList();}
  public static int fruitRings(String id){if(id.equals("orange_fruit"))return 500;if(id.equals("shiny_fruit"))return 3000;
    int at=GBA_FRUITS.indexOf(id);return at>=0?new int[]{30,60,55,50,30,55,70}[at]:20000;}
}
