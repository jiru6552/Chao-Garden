package com.chaogarden.distribution;

import java.util.function.Supplier;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.LootTable;

/** Exact temporary provenance for an actual physical loot-container transaction. */
public final class ChestFillWitness {
  private record Fill(LootTable table,LootContext context) {}
  private static final ThreadLocal<Fill> CURRENT=new ThreadLocal<>();
  private ChestFillWitness() {}
  public static <T> T generate(LootTable table,LootContext context,Supplier<T> generation) {
    Fill previous=CURRENT.get();CURRENT.set(new Fill(table,context));
    try{return generation.get();}
    finally{if(previous==null)CURRENT.remove();else CURRENT.set(previous);}
  }
  public static boolean matches(LootContext context,LootTable table) {
    Fill fill=CURRENT.get();
    return fill!=null&&fill.table()==table&&fill.context()==context&&table.getLootTableId()!=null
        &&table.getLootTableId().equals(context.getQueriedLootTableId());
  }
}
