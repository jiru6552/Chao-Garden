package com.chaogarden.fruit;

import com.chaogarden.core.ChaoState;
import java.util.Arrays;
import java.util.Random;

/** Whole-item conversion and acquisition boundaries, independent of Minecraft. */
public final class FruitEffectsTest {
  private static int checks;
  private static void check(boolean v, String message) {
    checks++; if (!v) throw new AssertionError(message);
  }
  private static ChaoState fresh() {
    var s = ChaoState.egg(0, false, false, 0, new Random(57));
    Arrays.fill(s.progress, 0); Arrays.fill(s.levels, 1); s.fruitCount = 0;
    s.align = 0; s.luck = 0; return s;
  }
  public static int run() {
    checks = 0;
    check(FruitCatalog.ACTIVE.size()==35 && FruitCatalog.NATURAL.equals(java.util.List.of("garden_fruit","hero_garden_fruit","dark_garden_fruit")) && FruitCatalog.MARKET.size()==7 && FruitCatalog.ADVENTURE.size()==10,
        "SA2 active source categories");
    check(FruitCatalog.NATURAL.stream().noneMatch(FruitCatalog.MARKET::contains), "Wild and market sources do not overlap");
    check(FruitCatalog.LEGACY.stream().noneMatch(FruitCatalog.ACTIVE::contains), "Unused/old fruits are compatibility-only");
    for (String id : FruitCatalog.LEGACY) check(FruitCatalog.isLegacyItem(id), "Legacy item visibility");
    for (String fruit : FruitCatalog.FARMER_FRUITS) {
      var s = fresh(); FruitEffects.apply(s, fruit);
      int expected = fruit.equals("strong_fruit") ? 50 : fruit.equals("tasty_fruit") ? 30 : 40;
      check(s.progress[ChaoState.STAMINA]==expected, "Four-bite whole-fruit stamina total: "+fruit);
      check(s.fruitCount==1, "One whole item records one care event");
      float affinity=fruit.equals("hero_garden_fruit")?.15f:fruit.equals("dark_garden_fruit")?-.15f:0;
      check(Math.abs(s.align-affinity)<.00001f, "Natural garden fruit affinity: "+fruit);
    }
    for(boolean stronger:new boolean[]{false,true})for(String fruit:new String[]{"hero_garden_fruit","dark_garden_fruit"}){
      float delta=fruit.startsWith("hero")?.15f:-.15f;
      var child=fresh();child.align=-Math.signum(delta);
      FruitEffects.apply(child,fruit,stronger);
      check(Math.abs(child.align-(-Math.signum(delta)+delta))<.00001f,"Opposing fruit shifts a child back toward neutral");
      check(child.progress[4]==40&&child.fruitCount==1,"Merged garden fruit preserves nourishment and single care event under both profiles");
      var grown=fresh();grown.adult=true;grown.align=Math.signum(delta);
      FruitEffects.apply(grown,fruit,stronger);
      check(grown.align==Math.signum(delta),"Garden fruit cannot rewrite an evolved alignment");
      child.align=Math.signum(delta)*.98f;FruitEffects.apply(child,fruit,stronger);
      check(child.align==Math.signum(delta),"Garden fruit alignment clamps at its boundary");
      check(FruitEffects.benefit(fruit,stronger).contains(fruit.startsWith("hero")?"Hero":"Dark"),"Garden tooltip exposes affinity effect");
    }
    check(!FruitCatalog.ACTIVE.contains("hero_fruit")&&!FruitCatalog.ACTIVE.contains("dark_fruit"),"Duplicate fruits returned to active acquisition");
    var s = fresh(); FruitEffects.apply(s, "chao_fruit");
    for (int value : s.progress) check(value==50, "Chao fruit trains all five visible abilities");
    check(s.fruitCount==1, "Chao fruit must not count five feeding events");
    s = fresh(); FruitEffects.apply(s, "mushroom");
    check(s.progress[4]==50 && s.hiddenProgress[0]==50, "Ordinary mushroom total");
    s = fresh(); FruitEffects.apply(s, "mushroom_alt");
    check(s.progress[4]==80 && s.hiddenLevels[0]==1 && s.hiddenProgress[0]==0, "Nutritious mushroom total");
    s = fresh(); FruitEffects.apply(s, "hero_garden_fruit");
    check(Math.abs(s.align-.15f)<.00001f, "Hero fruit alignment");
    s = fresh(); FruitEffects.apply(s, "dark_garden_fruit");
    check(Math.abs(s.align+.15f)<.00001f, "Dark fruit alignment");
    s = fresh(); FruitEffects.apply(s, "heart_fruit");
    check(s.progress[4]==40 && s.fruitCount==1, "Heart fruit uses full-fruit stamina conversion");
    for(String fruit:FruitCatalog.ADVENTURE){
      s=fresh();int before=s.intelligence;FruitEffects.apply(s,fruit);
      check(s.progress[4]==40&&s.fruitCount==1,"Adventure fruit gives one ordinary nourishment event: "+fruit);
      check(FruitCatalog.isMarket(fruit)&&!FruitCatalog.isNatural(fruit),"Adventure fruit begins in market, not wild generation");
      if(fruit.equals("smart_fruit"))check(s.intelligence==Math.min(4000,before+10),"CA2 Smart Fruit intelligence effect");
    }
    s=fresh();s.curiosityTendency=s.energeticTendency=s.naiveTendency=s.carefreeTendency=0;
    FruitEffects.apply(s,"energy_fruit");
    check(s.curiosityTendency==10&&s.energeticTendency==10&&s.naiveTendency==-10&&s.carefreeTendency==-10,"Energy Fruit documented four-slider effect");
    FruitEffects.apply(s,"peace_fruit");
    check(s.curiosityTendency==0&&s.energeticTendency==0&&s.naiveTendency==0&&s.carefreeTendency==0,"Peace Fruit reverses Energy tendencies");
    check(!FruitCatalog.isLegacyItem("smart_fruit")&&!FruitCatalog.isLegacyItem("smart_fruit_seed"),"Confirmed CA2 Smart Fruit is active again");
    check(FruitCatalog.CWE.size()==13,"Thirteen distinct CWE/TCG foods");
    for(int stat=0;stat<4;stat++){
      s=fresh();s.grades[stat]=5;FruitEffects.apply(s,FruitEffects.HYPER[stat]);
      check(s.grades[stat]==6,"Hyper fruit raises only its S grade");
      check(!FruitEffects.canUse(s,FruitEffects.HYPER[(stat+1)%4]),"Second X grade refused");
    }
    s=fresh();check(!FruitEffects.canUse(s,"shiny_fruit"),"Shiny fruit refuses an ordinary coat");
    s.shiny=true;s.twoTone=true;FruitEffects.apply(s,"shiny_fruit");check(s.brightCoat,"Shiny fruit switches to Bright");
    FruitEffects.apply(s,"shiny_fruit");check(!s.brightCoat,"Shiny fruit restores intended shiny coat");
    for(String id:FruitCatalog.CWE)check(FruitCatalog.isMarket(id)&&!FruitCatalog.isNatural(id),"CWE market-first crop");
    s=fresh();FruitEffects.apply(s,"smart_fruit",true);
    check(s.progress[4]==30&&s.hiddenLevels[1]==3&&s.hiddenProgress[1]==20,"Opt-in CWE Smart rebalance uses320% hidden bars");
    s=fresh();FruitEffects.apply(s,"tasty_fruit",true);check(s.progress[4]==30&&s.hiddenProgress[0]==60,"Opt-in Tasty fruit luck bars");
    s=fresh();FruitEffects.apply(s,"strong_fruit",true);check(s.progress[4]==60,"Opt-in stronger fruit");
    s=fresh();FruitEffects.apply(s,"strong_fruit",false);check(s.progress[4]==50,"Default SA2 fruit unchanged");
    // Independent source table: whole-fruit XP, in Swim/Fly/Run/Power/Stamina order.
    String[] portable={"gba_orange_fruit","gba_blue_fruit","gba_pink_fruit","gba_green_fruit","gba_purple_fruit","gba_yellow_fruit","gba_red_fruit"};
    int[][] perBite={{30,-20,-20,30,10},{20,50,-10,-10,30},{40,-30,40,-30,20},{0,-10,30,40,20},{-20,30,30,-20,10},{-30,40,-30,40,20},{30,10,30,20,-50}};
    for(boolean stronger:new boolean[]{false,true})for(int fruit=0;fruit<portable.length;fruit++) {
      s=fresh();Arrays.fill(s.progress,50);Arrays.fill(s.levels,1);
      FruitEffects.apply(s,portable[fruit],stronger);
      int multiplier=stronger?8:4;String tooltip=FruitEffects.benefit(portable[fruit],stronger);
      for(int stat=0;stat<5;stat++) {
        int delta=perBite[fruit][stat]*multiplier;
        int expected=100+Math.max(0,50+delta);
        check(s.levels[stat]*100+s.progress[stat]==expected,"Portable fruit XP differs from independent signed table");
        String percent=(delta>0?"+":"")+delta+"%";
        check(tooltip.contains(ChaoState.STATS[stat]+" "+percent),"Tooltip shows wrong profile/stat amount");
      }
      check(s.fruitCount==1,"Signed multi-stat food records one care event");
    }
    String[] profiled={"strong_fruit","tasty_fruit","hero_garden_fruit","dark_garden_fruit","round_fruit","square_fruit","triangle_fruit","smart_fruit","mushroom","mushroom_alt","mint_candy"};
    int[][] stamina={{50,30,40,40,40,40,40,40,50,80,15},{60,30,40,40,80,80,80,30,160,320,50}};
    for(int profile=0;profile<2;profile++)for(int index=0;index<profiled.length;index++) {
      s=fresh();FruitEffects.apply(s,profiled[index],profile==1);
      int gain=stamina[profile][index];
      check(s.levels[4]*100+s.progress[4]==100+gain,"Whole-food stamina conversion/profile mismatch");
      check(FruitEffects.benefit(profiled[index],profile==1).contains("Stamina +"+gain+"%"),"Tooltip stamina differs from consumption");
      check(s.fruitCount==1,"Profiled food must record one whole item");
    }
    // All registered foods at boundary levels/progress, under both profiles.
    for(String food:FruitCatalog.registeredFoods())for(boolean stronger:new boolean[]{false,true})for(int level:new int[]{0,98,99})for(int progress:new int[]{0,99}) {
      s=fresh();Arrays.fill(s.levels,level);Arrays.fill(s.progress,progress);s.shiny=true;s.twoTone=true;
      Arrays.fill(s.grades,5);
      check(FruitEffects.canUse(s,food),"Prepared valid food unexpectedly refused");
      FruitEffects.apply(s,food,stronger);
      check(s.fruitCount==1,"Boundary food recorded duplicate/missing care");
      check(s.hunger>=0&&s.hunger<=100&&s.happiness>=-100&&s.happiness<=100&&s.energy>=0&&s.energy<=100,"Food care values bounded");
      for(int stat=0;stat<5;stat++)check(s.levels[stat]>=level&&s.levels[stat]<=99&&s.progress[stat]>=0&&s.progress[stat]<100&&s.points[stat]>=0&&s.points[stat]<=9999,"Food cannot corrupt or decrease trained level");
      check(s.luck>=0&&s.luck<=4000&&s.intelligence>=0&&s.intelligence<=4000,"Hidden food values bounded");
      check(!FruitEffects.benefit(food,stronger).isBlank(),"Registered food has no benefit description");
    }
    return checks;
  }
  public static void main(String[] args) { System.out.println("Fruit rules: "+run()+" checks passed"); }
}
