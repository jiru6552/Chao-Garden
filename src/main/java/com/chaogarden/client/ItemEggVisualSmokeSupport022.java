package com.chaogarden.client;

import com.chaogarden.ChaoGarden;
import com.chaogarden.ChaoVariants;
import com.chaogarden.core.ChaoState;
import com.chaogarden.entity.ChaoEggEntity;
import com.chaogarden.item.ChaoCarrierItem;
import com.mojang.logging.LogUtils;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/** Actual registry/egg capture fixtures, excluded from the installable JAR. */
public final class ItemEggVisualSmokeSupport022 {
  public static final int PAGE_SIZE=36;
  public record InventoryPage(int page,int pageCount,int itemCount,List<String> ids) {
    public InventoryPage {ids=List.copyOf(ids);}
  }
  private record EggCase(String label,ChaoState state) {}
  private ItemEggVisualSmokeSupport022() {}

  private static List<ResourceLocation> itemIds() {
    return BuiltInRegistries.ITEM.keySet().stream()
        .filter(id->id.getNamespace().equals(ChaoGarden.MODID))
        .sorted(Comparator.comparing(ResourceLocation::toString)).toList();
  }
  public static int itemCount() {return itemIds().size();}
  public static int pageCount() {return (itemCount()+PAGE_SIZE-1)/PAGE_SIZE;}
  private static int visibleSlot(int index) {return index<27?index+9:index-27;}

  private static ItemStack inventoryStack(ServerPlayer keeper,ResourceLocation id) {
    ItemStack stack=new ItemStack(BuiltInRegistries.ITEM.get(id));
    if(stack.getItem() instanceof ChaoCarrierItem) {
      // Bare legacy carriers correctly delete themselves in inventoryTick.
      // Preview the real held-Chao item with genuine serialized entity data.
      var body=ChaoGarden.CHAO.get().create(keeper.serverLevel());
      if(body==null)throw new IllegalStateException("No real Chao for carrier inventory fixture");
      body.setUUID(new UUID(22022,1));body.owner=keeper.getUUID();
      body.setState(ChaoState.egg(0,false,true,0,new Random(22022)));
      ChaoCarrierItem.snapshot(stack,body);
      if(!ChaoCarrierItem.filled(stack))throw new IllegalStateException("Carrier fixture did not retain its real snapshot");
    }
    return stack;
  }

  /** Main27 slots followed by hotbar9; a short final page has actual empty slots. */
  public static InventoryPage prepareInventory(ServerPlayer player,int page) {
    var all=itemIds();int pages=(all.size()+PAGE_SIZE-1)/PAGE_SIZE;
    if(page<0||page>=pages)throw new IllegalArgumentException("Inventory audit page "+page+" outside actual "+pages+" pages / "+all.size()+" items");
    var ids=all.subList(page*PAGE_SIZE,Math.min(all.size(),(page+1)*PAGE_SIZE));
    player.getInventory().clearContent();
    for(int i=0;i<ids.size();i++)player.getInventory().setItem(visibleSlot(i),inventoryStack(player,ids.get(i)));
    player.getInventory().selected=8;player.getInventory().setChanged();player.inventoryMenu.broadcastChanges();
    var result=new InventoryPage(page,pages,all.size(),ids.stream().map(ResourceLocation::toString).toList());
    LogUtils.getLogger().info("CHAO_AUDIT022_ITEM_CATALOG itemCount={} pageCount={} page={} displayed={} ids={}",
        result.itemCount(),result.pageCount(),page,result.ids().size(),result.ids());
    return result;
  }

  public static boolean inventoryMatches(Player player,InventoryPage page) {
    if(player==null||page==null)return false;
    for(int i=0;i<PAGE_SIZE;i++) {
      ItemStack actual=player.getInventory().getItem(visibleSlot(i));
      if(i>=page.ids().size()){if(!actual.isEmpty())return false;}
      else if(actual.isEmpty()||!BuiltInRegistries.ITEM.getKey(actual.getItem()).toString().equals(page.ids().get(i)))return false;
    }
    return true;
  }

  private static ChaoState egg(int color,boolean shiny,boolean two,int jewel,long seed) {
    return ChaoState.egg(color,shiny,two,jewel,new Random(seed));
  }
  private static String title(String value) {
    StringBuilder out=new StringBuilder();
    for(String word:value.split("_")){if(!out.isEmpty())out.append(' ');out.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1));}
    return out.toString();
  }
  private static EggCase plain(String label,int color,boolean shiny,boolean two,int jewel,long seed) {
    return new EggCase(label,egg(color,shiny,two,jewel,seed));
  }
  private static EggCase theme(String id,boolean shiny,long seed) {
    if(!ChaoVariants.available(id))throw new IllegalStateException("Themed egg fixture requires its identity fixture/mod: "+id);
    ChaoState state=egg(0,shiny,true,0,seed);state.variant=id;
    return new EggCase(ChaoVariants.name(id)+(shiny?" / Shiny":""),state);
  }

  private static List<EggCase> eggCases(String scene) {
    var cases=new ArrayList<EggCase>();
    switch(scene) {
      case "eggaudit-colors" -> {
        for(int i=0;i<ChaoState.COLORS.length;i++)cases.add(plain(title(ChaoState.COLORS[i]),i,false,i==0,0,22000+i));
      }
      case "eggaudit-jewels" -> {
        for(int i=1;i<ChaoState.JEWELS.length;i++)cases.add(plain(title(ChaoState.JEWELS[i]),0,false,false,i,22100+i));
      }
      case "eggaudit-themes-0","eggaudit-themes-1","eggaudit-themes-shiny-0","eggaudit-themes-shiny-1" -> {
        boolean shiny=scene.contains("shiny");int page=scene.endsWith("0")?0:1;
        var themes=ChaoVariants.all();
        for(int i=page*10;i<Math.min(themes.size(),(page+1)*10);i++)cases.add(theme(themes.get(i).id(),shiny,22200+i));
      }
      case "eggaudit-special" -> {
        cases.add(plain("Normal two-tone",0,false,true,0,22300));
        cases.add(plain("Shiny normal",0,true,true,0,22301));
        var hero=egg(0,false,true,0,22302);hero.adult=true;hero.align=1;hero.primaryType=ChaoState.NORMAL;cases.add(new EggCase("Hero natural",hero));
        var dark=egg(0,false,true,0,22303);dark.adult=true;dark.align=-1;dark.primaryType=ChaoState.NORMAL;cases.add(new EggCase("Dark natural",dark));
        cases.add(plain("Red two-tone",2,false,true,0,22304));
        cases.add(plain("Shiny red",2,true,false,0,22305));
        cases.add(plain("Moon",0,false,false,13,22306));
        cases.add(plain("Shiny moon",0,true,false,13,22307));
      }
      case "eggaudit-close-nether" -> cases.add(theme("nether",false,22400));
      case "eggaudit-close-end" -> cases.add(theme("end",false,22401));
      case "eggaudit-close-shiny" -> cases.add(plain("Shiny aqua",5,true,false,0,22402));
      case "eggaudit-close-moon" -> cases.add(plain("Moon",0,false,false,13,22403));
      default -> throw new IllegalArgumentException("Unknown actual egg audit scene "+scene);
    }
    return List.copyOf(cases);
  }

  public static List<String> eggSceneLabels(String scene) {return eggCases(scene).stream().map(EggCase::label).toList();}

  public static List<Entity> prepareEggScene(ServerPlayer keeper,String scene) {
    var cases=eggCases(scene);int columns=scene.contains("themes")?5:4;
    int rows=(cases.size()+columns-1)/columns;double spacing=1.3;
    var entities=new ArrayList<Entity>();
    for(int i=0;i<cases.size();i++) {
      EggCase sample=cases.get(i);
      ChaoEggEntity body=ChaoGarden.CHAO_EGG.get().create(keeper.serverLevel());
      if(body==null)throw new IllegalStateException("Missing actual Chao egg type");
      body.setState(sample.state());body.setIncubation(0);body.setNoGravity(true);body.setInvulnerable(true);body.owner=keeper.getUUID();
      double x=cases.size()==1?0:((columns-1)/2.0-i%columns)*spacing;
      double z=cases.size()==1?0:(i/columns-(rows-1)/2.0)*spacing;
      body.moveTo(x,64,z,180,0);
      if(!keeper.serverLevel().addFreshEntity(body))throw new IllegalStateException("Actual egg fixture insertion denied");
      entities.add(body);
    }
    LogUtils.getLogger().info("CHAO_AUDIT022_EGG_SCENE scene={} eggs={} labels={}",scene,entities.size(),cases.stream().map(EggCase::label).toList());
    return List.copyOf(entities);
  }
}
