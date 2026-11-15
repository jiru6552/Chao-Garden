package com.chaogarden.hub;

import com.chaogarden.core.GardenProgressionRules081.Garden;
import net.minecraft.world.level.Level;

/** Resolves the existing actual dimension, rather than owner alignment or a remembered home. */
public final class GardenCare081 {
  private GardenCare081() {}
  public static Garden garden(Level level) {
    if(level.dimension().equals(HubContent.HERO_GARDEN))return Garden.HERO;
    if(level.dimension().equals(HubContent.DARK_GARDEN))return Garden.DARK;
    return Garden.NORMAL;
  }
}
