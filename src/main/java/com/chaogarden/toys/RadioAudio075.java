package com.chaogarden.toys;

import java.util.HashMap;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;

/** Server-only admission for nearby radio notes. Nothing is persisted or replayed after load. */
final class RadioAudio075 {
  private static final Map<ServerLevel,Map<BlockPos,Long>> LEASES=new WeakHashMap<>();
  private RadioAudio075() {}
  static boolean admit(ServerLevel level,BlockPos at) {
    long now=level.getGameTime();var leases=LEASES.computeIfAbsent(level,unused->new HashMap<>());
    leases.entrySet().removeIf(entry->entry.getValue()<=now);
    for(var source:leases.keySet())if(source.distSqr(at)<=RadioPlayRules075.AUDIO_RADIUS*RadioPlayRules075.AUDIO_RADIUS)return false;
    leases.put(at.immutable(),now+RadioPlayRules075.EXCLUSIVE_TICKS);return true;
  }
}
