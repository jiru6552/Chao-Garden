package com.chaogarden.hub;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

/** Server-owned return journals and completed scenery revisions, never inventories. */
public final class HubWorldData extends SavedData {
  final Map<UUID, CompoundTag> visits = new HashMap<>();
  final Map<String, Integer> revisions = new HashMap<>();
  private boolean heroGardenUnlocked, darkGardenUnlocked;
  /** Permanent server-wide acquisition history; independent from scene installation. */
  public boolean heroGardenUnlocked(){return heroGardenUnlocked;}
  public boolean darkGardenUnlocked(){return darkGardenUnlocked;}
  public boolean gardenUnlocked(boolean hero){return hero?heroGardenUnlocked:darkGardenUnlocked;}
  public boolean allGardensUnlocked(){return heroGardenUnlocked&&darkGardenUnlocked;}
  public boolean unlockGarden(boolean hero){
    if(gardenUnlocked(hero))return false;
    if(hero)heroGardenUnlocked=true;else darkGardenUnlocked=true;
    setDirty();return true;
  }
  public static HubWorldData get(MinecraftServer server) {
    return server.overworld().getDataStorage().computeIfAbsent(
        new Factory<>(HubWorldData::new, HubWorldData::load), "chao_hub_visits");
  }
  public CompoundTag visit(UUID player) {
    CompoundTag value=visits.get(player);return value==null?null:value.copy();
  }
  public void put(UUID player,CompoundTag visit) { visits.put(player,visit.copy());setDirty(); }
  public void remove(UUID player) { if(visits.remove(player)!=null)setDirty(); }
  public static HubWorldData load(CompoundTag root, HolderLookup.Provider lookup) {
    var result=new HubWorldData();
    var unlocks=root.getCompound("GardenUnlocks081");
    result.heroGardenUnlocked=unlocks.getBoolean("Hero");
    result.darkGardenUnlocked=unlocks.getBoolean("Dark");
    for(var item:root.getList("Visits",10)) {
      var tag=(CompoundTag)item;if(tag.hasUUID("Player"))result.visits.put(tag.getUUID("Player"),tag.getCompound("Return").copy());
    }
    var revisions=root.getCompound("Revisions");
    for(String key:revisions.getAllKeys())result.revisions.put(key,Math.max(0,revisions.getInt(key)));
    return result;
  }
  @Override public CompoundTag save(CompoundTag root,HolderLookup.Provider lookup) {
    var unlocks=new CompoundTag();unlocks.putBoolean("Hero",heroGardenUnlocked);unlocks.putBoolean("Dark",darkGardenUnlocked);root.put("GardenUnlocks081",unlocks);
    var list=new ListTag();
    visits.forEach((id,value)->{var item=new CompoundTag();item.putUUID("Player",id);item.put("Return",value.copy());list.add(item);});
    root.put("Visits",list);var rev=new CompoundTag();revisions.forEach(rev::putInt);root.put("Revisions",rev);return root;
  }
}
