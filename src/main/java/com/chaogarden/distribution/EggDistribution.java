package com.chaogarden.distribution;

import com.chaogarden.GardenConfig;
import net.neoforged.fml.ModList;

/** Optional space support uses identifiers without optional mod class linkage. */
public final class EggDistribution {
  private EggDistribution() {}
  public static boolean spaceModLoaded() {
    return GardenConfig.SPACE_MOD_IDS.get().stream().anyMatch(id->ModList.get().isLoaded(id));
  }
  public static boolean isMoon(String dimension) {
    return EggDistributionRules.moon(dimension,GardenConfig.MOON_DIMENSION_IDS.get());
  }
}
