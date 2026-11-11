package com.chaogarden.market;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

/** Persistent world-wide spawn budget; restarting the server cannot reroll a visit. */
public final class MarketVisitData extends SavedData {
  private static final Factory<MarketVisitData> FACTORY =
      new Factory<>(MarketVisitData::new, MarketVisitData::load);
  private long lastAttemptDay = -1;
  private long occupiedUntil;

  public static MarketVisitData get(ServerLevel level) {
    return level.getDataStorage().computeIfAbsent(FACTORY, "chao_black_market_visits");
  }

  public boolean mayAttempt(long day, long now) {
    return day > lastAttemptDay && now >= occupiedUntil;
  }

  public void attempted(long day) { lastAttemptDay = day; setDirty(); }
  public void occupied(long until) { occupiedUntil = until; setDirty(); }

  public static MarketVisitData load(CompoundTag tag, HolderLookup.Provider registries) {
    var data = new MarketVisitData();
    data.lastAttemptDay = tag.contains("LastAttemptDay") ? tag.getLong("LastAttemptDay") : -1;
    data.occupiedUntil = tag.getLong("OccupiedUntil");
    return data;
  }

  @Override
  public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
    tag.putLong("LastAttemptDay", lastAttemptDay);
    tag.putLong("OccupiedUntil", occupiedUntil);
    return tag;
  }
}
