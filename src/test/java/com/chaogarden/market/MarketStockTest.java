package com.chaogarden.market;

import java.util.HashSet;
import java.util.List;
import java.math.BigDecimal;
import java.math.RoundingMode;

public final class MarketStockTest {
  private static int checks;
  private static void check(boolean condition, String message) {
    checks++; if (!condition) throw new AssertionError(message);
  }
  public static int run() {
    checks=0;
    var seen=new HashSet<String>();
    for (long seed=0;seed<1000;seed++) {
      var stock=MarketStock.select(seed,List.of("mushroom_seed","garden_fruit_seed"),List.of("rabbit_animal_gene"));
      check(stock.equals(MarketStock.select(seed,List.of(),List.of())),"Deterministic stock ignores untrusted obsolete category inputs");
      check(stock.size()==MarketStock.OFFER_COUNT || stock.size()==MarketStock.OFFER_COUNT+1,"Forty-one offers plus an optional rare gem");
      check(MarketStock.validStock(stock.stream().map(MarketStock.Listing::itemId).toList(),false),"Current selection satisfies saved-stock contract");
      check(stock.stream().map(MarketStock.Listing::itemId).toList().containsAll(MarketStock.BASE_EGGS),"All sixteen ordinary colors are available together");
      check(stock.stream().map(MarketStock.Listing::itemId).toList().containsAll(MarketStock.GARDEN_TOOLS),"Both gardening tools are always available");
      check(stock.stream().filter(x->MarketStock.SHINY_EGGS.contains(x.itemId())).count()==1,"Exactly one shiny color rotates");
      check(stock.stream().map(MarketStock.Listing::itemId).toList().containsAll(MarketStock.DRIVES),"All four inexpensive drives are always offered");
      check(stock.stream().filter(x->MarketStock.ORDINARY_GENES.contains(x.itemId())).count()==3,"Three finite ordinary gene offers");
      check(stock.stream().filter(x->MarketStock.MYTHICAL_GENES.contains(x.itemId())).count()==1,"One finite mythical gene offer");
      check(stock.stream().map(MarketStock.Listing::itemId).distinct().count()==stock.size(),"No duplicate offers");
      check(stock.stream().anyMatch(x->x.itemId().equals("normal_egg")),"Regular egg exception always offered");
      check(stock.stream().anyMatch(x->x.itemId().equals("strong_fruit_seed")),"Strong seed always offered");
      check(stock.stream().filter(x->MarketStock.SHAPED_FRUITS.contains(x.itemId())).count()==2,"Two shaped fruits, third unavailable this restock");
      check(stock.stream().filter(x->MarketStock.OTHER_FRUITS.contains(x.itemId())).count()==1,"One other fruit discovery");
      check(stock.stream().filter(x->com.chaogarden.fruit.FruitCatalog.ADVENTURE.contains(x.itemId())).count()==1,"One portable adventure fruit discovery");
      check(stock.stream().filter(x->MarketStock.SEEDS.contains(x.itemId())).count()==3,"Three priced seeds");
      check(stock.stream().filter(x->MarketStock.TOYS.contains(x.itemId())).count()==2,"Two rotating toys");
      check(stock.stream().filter(x->MarketStock.HATS.contains(x.itemId())).count()==2,"Two rotating hats");
      check(stock.stream().filter(x->com.chaogarden.fruit.FruitCatalog.CWE.contains(x.itemId())).count()==1,"One CWE fruit discovery");
      check(stock.stream().filter(x->MarketStock.ACCESSORIES.contains(x.itemId())).count()==1,"One real accessory");
      check(stock.stream().filter(x->MarketStock.GIFTS.contains(x.itemId())).count()==1,"One lens or special gift");
      for (var listing:stock) {
        seen.add(listing.itemId());
        check(MarketStock.catalog().containsKey(listing.itemId()),"Only explicitly priced source products");
        check(!listing.itemId().endsWith("_animal_gene") || MarketStock.ORDINARY_GENES.contains(listing.itemId()) || MarketStock.MYTHICAL_GENES.contains(listing.itemId()),"Only known sample products");
        check(!listing.itemId().contains("mushroom_seed") && !listing.itemId().contains("mushroom_alt_seed"),"No mushroom seeds");
        check(listing.emeralds()==MarketStock.catalog().get(listing.itemId()).listing().emeralds(),"Prices never reroll");
        var payments=MarketStock.payment(listing.emeralds());
        check(payments.size()<=2,"Vanilla merchant has at most two input slots");
        check(payments.stream().mapToInt(MarketStock.Payment::emeraldValue).sum()==listing.emeralds(),"Displayed payable price equals actual currency");
        check(payments.stream().allMatch(p->p.count()>0 && p.count()<=64),"Every payment stack fits");
      }
    }
    var gemsSeen=new HashSet<String>();int gemVisits=0;
    for(long seed=0;seed<200000;seed++) {
      var stock=MarketStock.select(seed,List.of(),List.of());
      var restricted=MarketStock.select(seed,List.of(),List.of(),true);
      var gems=stock.stream().filter(x->MarketStock.JEWEL_EGGS.contains(x.itemId())).toList();
      check(gems.size()<=1 && stock.size()==MarketStock.OFFER_COUNT+gems.size(),"Gem is optional extra stock");
      check(restricted.stream().noneMatch(x->x.itemId().equals("moon_jewel_egg")),"Moon cannot enter a space-enabled market");
      check(restricted.size()==stock.size(),"Moon restriction must not change gem-offer probability");
      if(!gems.isEmpty()){gemVisits++;gemsSeen.add(gems.getFirst().itemId());}
      stock.forEach(x->seen.add(x.itemId()));
    }
    check(gemVisits>=120 && gemVisits<=300,"Rare gem visit frequency remains approximately one in a thousand");
    check(gemsSeen.equals(new HashSet<>(MarketStock.JEWEL_EGGS)),"All thirteen gems can reach the rare market pool");
    check(seen.equals(MarketStock.catalog().keySet()),"Every allowed item can appear across restocks");
    check(MarketStock.BASE_EGGS.size()==16 && MarketStock.SHINY_EGGS.size()==16 && MarketStock.JEWEL_EGGS.size()==13,"Complete registered egg categories");
    int normalPrice=MarketStock.catalog().get("normal_egg").listing().emeralds();
    check(normalPrice==2,"Normal egg remains the least expensive entry");
    for(String id:MarketStock.BASE_EGGS){
      var listing=MarketStock.catalog().get(id).listing();
      check(listing.uses()==Integer.MAX_VALUE && MarketStock.permanent(id),"Base egg unlimited stock metadata");
      check(id.equals("normal_egg") || listing.emeralds()>normalPrice,"Every color costs more than Normal");
    }
    for(String id:MarketStock.DRIVES){
      var product=MarketStock.catalog().get(id);check(MarketStock.permanent(id)&&product.uses()==Integer.MAX_VALUE&&product.listing().emeralds()==1,"Drive stock is cheap and unlimited");
    }
    for(String id:MarketStock.ORDINARY_GENES){var product=MarketStock.catalog().get(id);check(!MarketStock.permanent(id)&&product.uses()==4&&product.listing().emeralds()==1,"Ordinary DNA costs one emerald and has four uses");}
    for(String id:MarketStock.MYTHICAL_GENES){var product=MarketStock.catalog().get(id);check(!MarketStock.permanent(id)&&product.uses()==2&&product.listing().emeralds()==2,"Mythical DNA costs slightly more and has two uses");}
    for(String id:MarketStock.SHINY_EGGS)check(!MarketStock.permanent(id) && MarketStock.catalog().get(id).uses()==1,"Shiny stock stays finite");
    var ids=MarketStock.select(0,List.of(),List.of()).stream().map(MarketStock.Listing::itemId).toList();
    check(!MarketStock.validStock(ids.subList(1,ids.size()),false),"Saved stock cannot omit a permanent color");
    var duplicate=new java.util.ArrayList<>(ids);duplicate.set(1,duplicate.getFirst());
    check(!MarketStock.validStock(duplicate,false),"Duplicate saved offers must be migrated");
    var foreign=new java.util.ArrayList<>(ids);foreign.set(0,"create_chao_egg");
    check(!MarketStock.validStock(foreign,false),"Paused addon stock must be rejected");
    check(MarketStock.catalog().get("white_egg").listing().emeralds()==4,"400 ring white egg costs4emeralds");
    check(MarketStock.catalog().get("shiny_white_egg").listing().emeralds()==40,"Shiny egg preserves10xprice");
    check(MarketStock.catalog().get("chao_radio").listing().emeralds()==603,"Radio rounded up to67blocks");
    check(MarketStock.catalog().get("chao_tv").listing().emeralds()==1008,"TV rounded up to112blocks");
    check(MarketStock.payment(200).equals(List.of(new MarketStock.Payment(true,22),new MarketStock.Payment(false,2))),"High egg cost usesblocks plusremainder");
    for(int invalid:new int[]{0,577,1000,1153}) try {
      MarketStock.payment(invalid);throw new AssertionError("Accepted impossible currency");
    } catch(IllegalArgumentException expected) { checks++; }
    // Decimal rounding provides an independent oracle for the integer conversion,
    // including the former rings+99 overflow boundary.
    for(int price=1;price<=1153;price++)for(int offset:new int[]{-1,0,1}) {
      int rings=price*100+offset;
      int expected=Math.max(1,BigDecimal.valueOf(rings).movePointLeft(2).setScale(0,RoundingMode.CEILING).intValueExact());
      if(expected>576)expected=BigDecimal.valueOf(expected).divide(BigDecimal.valueOf(9),0,RoundingMode.CEILING).multiply(BigDecimal.valueOf(9)).intValueExact();
      check(MarketStock.emeraldPrice(rings)==expected,"Decimal ring/emerald ceiling differs at boundary");
    }
    for(int rings:new int[]{Integer.MIN_VALUE,-101,-1,0,1,Integer.MAX_VALUE-99,Integer.MAX_VALUE-1,Integer.MAX_VALUE}) {
      int expected=Math.max(1,BigDecimal.valueOf(rings).movePointLeft(2).setScale(0,RoundingMode.CEILING).intValueExact());
      if(expected>576)expected=BigDecimal.valueOf(expected).divide(BigDecimal.valueOf(9),0,RoundingMode.CEILING).multiply(BigDecimal.valueOf(9)).intValueExact();
      check(MarketStock.emeraldPrice(rings)==expected,"Ring arithmetic overflow at integer boundary");
    }
    for(int value=1;value<=1152;value++) {
      boolean representable=value<=576||value%9==0;
      try {
        var payment=MarketStock.payment(value);check(representable,"Impossible two-slot payment admitted");
        check(payment.size()<=2&&payment.stream().allMatch(p->p.count()>=1&&p.count()<=64),"Payment stack bound");
        check(payment.stream().mapToInt(MarketStock.Payment::emeraldValue).sum()==value,"Currency changes during block conversion");
      }catch(IllegalArgumentException expected){check(!representable,"Representable payment refused");}
    }
    for(var product:MarketStock.catalog().values()) {
      int cost=product.listing().emeralds();
      check(product.sourceRings()>0&&cost>0&&cost<=1152,"Every active product has a payable positive price");
      check(MarketStock.payment(cost).stream().mapToInt(MarketStock.Payment::emeraldValue).sum()==cost,"Every product uses exact payable currency");
    }
    return checks;
  }
  public static void main(String[] args) { System.out.println("Black Market stock: "+run()+" checks passed"); }
}
