package com.chaogarden.test;

import com.chaogarden.ChaoGarden;
import com.chaogarden.distribution.ChestEggLootModifier;
import com.chaogarden.distribution.EggDistributionRules;
import com.chaogarden.item.ChaoEggItem;
import com.google.gson.JsonObject;
import com.mojang.serialization.JsonOps;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import java.lang.reflect.Proxy;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.minecraft.world.level.levelgen.PositionalRandomFactory;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraft.world.phys.Vec3;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.common.NeoForgeEventHandler;
import net.neoforged.neoforge.common.loot.LootModifierManager;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** Exact probability boundaries, the loaded GLM pipeline, and optional real Lootr persistence. */
@GameTestHolder("chao_eggs50")
@PrefixGameTestTemplate(false)
public final class GemEggGameTests050 {
  private static final org.slf4j.Logger LOGGER=com.mojang.logging.LogUtils.getLogger();
  private static final ResourceLocation ANCIENT=ResourceLocation.withDefaultNamespace("chests/ancient_city");
  private GemEggGameTests050() {}
  private static LootParams params(GameTestHelper h) {
    return new LootParams.Builder(h.getLevel()).withParameter(LootContextParams.ORIGIN,Vec3.atCenterOf(h.absolutePos(new BlockPos(2,2,2))))
        .create(LootContextParamSets.CHEST);
  }
  private static LootContext context(GameTestHelper h,ResourceLocation id,RandomSource random) {
    return new LootContext.Builder(params(h)).withOptionalRandomSource(random).withQueriedLootTableId(id).create(Optional.empty());
  }
  private static int gems(Container inventory) {
    int count=0;
    for(int slot=0;slot<inventory.getContainerSize();slot++) {
      ItemStack stack=inventory.getItem(slot);
      if(stack.getItem() instanceof ChaoEggItem egg&&egg.jewel>0)count+=stack.getCount();
    }
    return count;
  }
  private static int gems(ObjectArrayList<ItemStack> loot) {
    return loot.stream().filter(s->s.getItem() instanceof ChaoEggItem egg&&egg.jewel>0).mapToInt(ItemStack::getCount).sum();
  }
  private static CompoundTag contents(Container container,HolderLookup.Provider provider) {
    var items=net.minecraft.core.NonNullList.withSize(container.getContainerSize(),ItemStack.EMPTY);
    for(int i=0;i<items.size();i++)items.set(i,container.getItem(i).copy());
    return ContainerHelper.saveAllItems(new CompoundTag(),items,provider);
  }
  private static ChestEggLootModifier loaded(GameTestHelper h) throws ReflectiveOperationException {
    var method=NeoForgeEventHandler.class.getDeclaredMethod("getLootModifierManager");method.setAccessible(true);
    var manager=(LootModifierManager)method.invoke(null);
    var matches=manager.getAllLootMods().stream().filter(m->m instanceof ChestEggLootModifier).toList();
    h.assertTrue(matches.size()==1,"Expected exactly one loaded Chao chest egg modifier, got "+matches.size());
    return (ChestEggLootModifier)matches.getFirst();
  }

  @GameTest(template="empty",timeoutTicks=40)
  public static void singleLoadedChestModifierUsesThreePercentAndCodecDefaults(GameTestHelper h) throws ReflectiveOperationException {
    var modifier=loaded(h);
    JsonObject encoded=ChestEggLootModifier.CODEC.codec().encodeStart(JsonOps.INSTANCE,modifier).getOrThrow().getAsJsonObject();
    // Optional fields at their defaults may be omitted by the codec.
    double chance=encoded.has("gem_chance")?encoded.get("gem_chance").getAsDouble():EggDistributionRules.GEM_CHANCE;
    h.assertTrue(chance==.03,"Loaded data pack did not apply the 3% gem chance");
    JsonObject minimal=new JsonObject();minimal.add("conditions",new com.google.gson.JsonArray());
    var defaults=ChestEggLootModifier.CODEC.codec().parse(JsonOps.INSTANCE,minimal).getOrThrow();
    var random=new FixedRoll(Math.nextDown(.03));
    var loot=new ObjectArrayList<ItemStack>();defaults.apply(loot,context(h,ANCIENT,random));
    h.assertTrue(gems(loot)==1&&random.doubles==3,"Codec default does not grant exactly one gem roll below 3%");
    h.succeed();
  }

  @GameTest(template="empty",timeoutTicks=40)
  public static void gemProbabilityBoundaryConsumesExactlyOneRoll(GameTestHelper h) throws ReflectiveOperationException {
    var modifier=loaded(h);
    for(double chance:new double[]{0,Math.nextDown(.03),.03,Math.nextUp(.03),.999999}) {
      var random=new FixedRoll(chance);var loot=new ObjectArrayList<ItemStack>();
      ItemStack original=new ItemStack(Items.DIAMOND,3);loot.add(original);
      modifier.apply(loot,context(h,ANCIENT,random));
      h.assertTrue(gems(loot)==(chance<.03?1:0),"Wrong gem boundary at "+chance);
      h.assertTrue(random.doubles==3,"Eligible chest performed extra probability rolls: "+random.doubles);
      h.assertTrue(random.ints==(chance<.03?1:0),"Gem selection was rerolled or chosen on failure");
      h.assertTrue(loot.getFirst()==original&&original.getCount()==3,"Existing loot changed");
    }
    // The distinct egg categories are independent. One gem can coexist with the old base/shiny categories.
    var all=new ChestEggLootModifier(new LootItemCondition[0],1,1,.03,0);
    var random=new FixedRoll(0);var loot=new ObjectArrayList<ItemStack>();all.apply(loot,context(h,ANCIENT,random));
    h.assertTrue(loot.size()==3&&gems(loot)==1,"Independent categories were replaced or a gem roll duplicated");
    h.succeed();
  }

  @GameTest(template="empty",timeoutTicks=40)
  public static void unlistedChestAndNonChestDoNotRollGems(GameTestHelper h) throws ReflectiveOperationException {
    var modifier=loaded(h);
    for(String id:new String[]{"minecraft:chests/simple_dungeon","minecraft:chests/jungle_temple","minecraft:chests/trial_chambers/reward"}) {
      var random=new FixedRoll(0);var loot=new ObjectArrayList<ItemStack>();modifier.apply(loot,context(h,ResourceLocation.parse(id),random));
      int expectedDoubles=EggDistributionRules.shinySite(id)?2:1;
      h.assertTrue(gems(loot)==0&&random.doubles==expectedDoubles,"Gem roll leaked to unlisted chest "+id);
    }
    var random=new FixedRoll(0);var loot=new ObjectArrayList<ItemStack>();
    modifier.apply(loot,context(h,ResourceLocation.withDefaultNamespace("entities/zombie"),random));
    h.assertTrue(loot.isEmpty()&&random.doubles==0,"Nonchest context consumed egg rolls");h.succeed();
  }

  @GameTest(template="empty",timeoutTicks=100)
  public static void actualGeneratedAncientChestsHaveOneGemOpportunityAndReopeningDoesNotRefill(GameTestHelper h) throws ReflectiveOperationException {
    BlockPos at=new BlockPos(2,2,2);h.setBlock(at,Blocks.CHEST);
    var chest=(RandomizableContainerBlockEntity)h.getLevel().getBlockEntity(h.absolutePos(at));
    // Lootr intentionally cancels vanilla unpackLootTable before generation to
    // queue conversion. Exercise the ordinary vanilla path with only conversion
    // temporarily disabled, then restore its exact setting synchronously. The
    // separate integration test exercises the enabled, real per-player path.
    net.neoforged.neoforge.common.ModConfigSpec.BooleanValue conversion=null;boolean previous=false;
    if(ModList.get().isLoaded("lootr")) {
      conversion=(net.neoforged.neoforge.common.ModConfigSpec.BooleanValue)
          Class.forName("noobanidus.mods.lootr.neoforge.config.ConfigManager").getField("DISABLE").get(null);
      previous=conversion.get();conversion.set(true);
    }
    try {
      if(conversion!=null)h.assertTrue((boolean)Class.forName("noobanidus.mods.lootr.common.api.LootrAPI")
          .getMethod("isDisabled").invoke(null),"Lootr conversion still intercepts the vanilla fixture");
      var key=ResourceKey.create(Registries.LOOT_TABLE,ANCIENT);int successes=0;
      for(int sample=0;sample<1024;sample++) {
        chest.clearContent();chest.setLootTable(key,0x50E66000L+sample);chest.unpackLootTable(null);
        h.assertTrue(chest.getLootTable()==null,"Vanilla chest did not generate at seed "+sample+"; class="+chest.getClass().getName());
        int eggs=gems(chest);h.assertTrue(eggs<=1,"A generated chest received duplicate gem rolls at seed "+sample);
        successes+=eggs;
        var before=contents(chest,h.getLevel().registryAccess());chest.unpackLootTable(null);
        h.assertTrue(chest.getLootTable()==null&&contents(chest,h.getLevel().registryAccess()).equals(before),"Opening a generated chest rerolled its contents at seed "+sample);
      }
      h.assertTrue(successes>=15&&successes<=55,"Loaded pipeline is inconsistent with 3% over 1024 fixed seeds: "+successes);
      LOGGER.info("GEM050 actual ancient chest generations=1024 gem successes={} max gems per generation=1 vanilla conversion disabled only during fixture",successes);
    } finally {
      chest.setLootTable(null);chest.clearContent();
      if(conversion!=null)conversion.set(previous);
    }
    h.succeed();
  }

  @GameTest(template="empty",timeoutTicks=100)
  public static void realLootrFillsPerPlayerOnceAndPreservesInventoriesAcrossReload(GameTestHelper h) throws ReflectiveOperationException {
    h.assertTrue(ModList.get().isLoaded("lootr"),"This integration test requires the opt-in real Lootr runtime");
    Class<?> info=Class.forName("noobanidus.mods.lootr.common.api.data.ILootrInfoProvider");
    Class<?> filler=Class.forName("noobanidus.mods.lootr.common.api.data.LootFiller");
    Class<?> api=Class.forName("noobanidus.mods.lootr.common.api.LootrAPI");
    Class<?> saved=Class.forName("noobanidus.mods.lootr.common.data.LootrSavedData");
    Class<?> defaults=Class.forName("noobanidus.mods.lootr.common.api.data.DefaultLootFiller");
    var actualFiller=defaults.getMethod("getInstance").invoke(null);AtomicInteger calls=new AtomicInteger();
    Object counting=Proxy.newProxyInstance(filler.getClassLoader(),new Class<?>[]{filler},(proxy,method,args)-> {
      if(method.getName().equals("unpackLootTable"))calls.incrementAndGet();
      return method.invoke(actualFiller,args);
    });
    BlockPos at=new BlockPos(3,2,3);
    var block=BuiltInRegistries.BLOCK.get(ResourceLocation.parse("lootr:lootr_chest"));
    h.assertTrue(block!=Blocks.AIR,"Actual Lootr block missing");h.setBlock(at,block);
    var chest=(RandomizableContainerBlockEntity)h.getLevel().getBlockEntity(h.absolutePos(at));
    chest.setLootTable(ResourceKey.create(Registries.LOOT_TABLE,ANCIENT),0x50E66050L);
    Object provider=info.getMethod("of",BlockPos.class,net.minecraft.world.level.Level.class).invoke(null,h.absolutePos(at),h.getLevel());
    h.assertTrue(provider!=null,"Actual Lootr provider missing");
    var playerA=TestConnections.makeSurvivalServerPlayer(h);var playerB=TestConnections.makeSurvivalServerPlayer(h);
    try {
      var get=api.getMethod("getInventory",info,ServerPlayer.class,filler);
      var inventoryA=(Container)get.invoke(null,provider,playerA,counting);
      var inventoryB=(Container)get.invoke(null,provider,playerB,counting);
      h.assertTrue(inventoryA!=null&&inventoryB!=null&&inventoryA!=inventoryB&&calls.get()==2,"Lootr did not generate one distinct inventory for each player");
      h.assertTrue(!inventoryA.isEmpty()&&!inventoryB.isEmpty()&&gems(inventoryA)<=1&&gems(inventoryB)<=1,"Lootr missed actual chest generation or doubled gem opportunities");
      inventoryA.setItem(0,new ItemStack(ChaoGarden.item("ruby_jewel_egg")));
      inventoryB.setItem(0,new ItemStack(Items.DIAMOND,7));
      var beforeA=contents(inventoryA,h.getLevel().registryAccess());var beforeB=contents(inventoryB,h.getLevel().registryAccess());
      for(int repeat=0;repeat<20;repeat++) {
        h.assertTrue(get.invoke(null,provider,playerA,counting)==inventoryA&&get.invoke(null,provider,playerB,counting)==inventoryB,"Lootr replaced cached inventory");
      }
      h.assertTrue(calls.get()==2,"Opening cached Lootr inventory rerolled loot");
      Object data=api.getMethod("getData",info).invoke(null,provider);
      CompoundTag encoded=((SavedData)data).save(new CompoundTag(),h.getLevel().registryAccess());
      Object restored=saved.getMethod("load",CompoundTag.class,HolderLookup.Provider.class).invoke(null,encoded,h.getLevel().registryAccess());
      var getRestored=saved.getMethod("getOrCreateInventory",info,ServerPlayer.class,filler);
      var restoredA=(Container)getRestored.invoke(restored,provider,playerA,counting);
      var restoredB=(Container)getRestored.invoke(restored,provider,playerB,counting);
      h.assertTrue(contents(restoredA,h.getLevel().registryAccess()).equals(beforeA)&&contents(restoredB,h.getLevel().registryAccess()).equals(beforeB),"Reload changed or mixed saved Lootr player inventories");
      h.assertTrue(calls.get()==2,"Reloading existing Lootr inventories performed new loot rolls");
      LOGGER.info("GEM050 actual Lootr inventories=2 initial fills=2 cached opens=40 reload fills=0");
    } finally {
      h.getLevel().getServer().getPlayerList().remove(playerA);h.getLevel().getServer().getPlayerList().remove(playerB);
    }
    h.succeed();
  }

  private static final class FixedRoll implements RandomSource {
    private final RandomSource delegate=RandomSource.create(50);
    private final double gem;
    int doubles,ints;
    FixedRoll(double gem){this.gem=gem;}
    @Override public RandomSource fork(){return delegate.fork();}
    @Override public PositionalRandomFactory forkPositional(){return delegate.forkPositional();}
    @Override public void setSeed(long seed){delegate.setSeed(seed);}
    @Override public int nextInt(){return delegate.nextInt();}
    @Override public int nextInt(int bound){ints++;return 0;}
    @Override public long nextLong(){return delegate.nextLong();}
    @Override public boolean nextBoolean(){return delegate.nextBoolean();}
    @Override public float nextFloat(){return delegate.nextFloat();}
    @Override public double nextDouble(){doubles++;return doubles<=2?.9:gem;}
    @Override public double nextGaussian(){return delegate.nextGaussian();}
  }
}
