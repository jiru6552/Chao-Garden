package com.chaogarden.distribution;

import java.util.List;
import java.util.function.IntUnaryOperator;

/** Pure acquisition policy. Rates apply once per qualifying chest generation. */
public final class EggDistributionRules {
  public static final double BASE_CHANCE=.0025, SHINY_CHANCE=.001, GEM_CHANCE=.03, DIMENSION_CHANCE=.01;
  public static final List<String> COLORS=List.of("normal","white","red","yellow","blue","aqua",
      "pink","orange","purple","brown","green","grey","lime","black","sky_blue","cyan");
  public static final List<String> GEMS=List.of("silver","gold","ruby","sapphire","emerald","amethyst",
      "aquamarine","garnet","onyx","diamond","peridot","topaz","moon");
  public static final List<String> DEFAULT_SPACE_MODS=List.of("ad_astra","northstar","stellaris");
  public static final List<String> DEFAULT_MOON_DIMENSIONS=List.of("ad_astra:moon","northstar:moon","stellaris:moon");
  private EggDistributionRules() {}

  public static boolean shinySite(String table) {
    if(!"minecraft".equals(namespace(table)))return false;
    String path=path(table);
    return path.equals("chests/buried_treasure")||path.equals("chests/ancient_city")
        ||path.startsWith("chests/stronghold_")||path.startsWith("chests/trial_chambers/")
        ||path.equals("chests/desert_pyramid")||path.equals("chests/jungle_temple");
  }
  public static boolean gemSite(String table) {
    if(!"minecraft".equals(namespace(table)))return false;
    String path=path(table);
    return path.equals("chests/ancient_city")||path.equals("chests/desert_pyramid")
        ||path.startsWith("chests/stronghold_")||path.startsWith("chests/bastion_")
        ||path.equals("chests/end_city_treasure");
  }
  public static boolean shinyEligible(String dimension,String table) {
    return !"minecraft:overworld".equals(dimension)||shinySite(table);
  }
  public static boolean moon(String dimension,List<? extends String> moons) {return moons.contains(dimension);}
  public static String baseEgg(IntUnaryOperator random) {
    int ticket=random.applyAsInt(6+5*(COLORS.size()-1));
    return COLORS.get(ticket<6?0:1+(ticket-6)/5)+"_egg";
  }
  public static String shinyEgg(IntUnaryOperator random) {
    int color=random.applyAsInt(COLORS.size());
    return color==0?"shiny_egg":"shiny_"+COLORS.get(color)+"_egg";
  }
  public static String gemEgg(boolean moonOnlyOnMoon,boolean onMoon,IntUnaryOperator random) {
    if(moonOnlyOnMoon&&onMoon)return "moon_jewel_egg";
    return GEMS.get(random.applyAsInt(GEMS.size()-(moonOnlyOnMoon?1:0)))+"_jewel_egg";
  }
  private static String namespace(String id) {int c=id==null?-1:id.indexOf(':');return c<0?"":id.substring(0,c);}
  private static String path(String id) {int c=id==null?-1:id.indexOf(':');return c<0?"":id.substring(c+1);}
}
