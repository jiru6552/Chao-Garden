package com.chaogarden.fruit;

import com.chaogarden.ChaoGarden;
import java.util.ArrayList;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.storage.loot.entries.LootPoolEntryType;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.ItemCost;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FireBlock;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.village.VillagerTradesEvent;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;

/** Renewable orchard content. Fruit behavior remains in the existing care system. */
public final class FruitContent {
  public static final int TREE_HEIGHT = 7;
  public static final int CANOPY_RADIUS = 2;
  public static final int CROWN_HEIGHT = 4;
  public static final List<String> FRUITS = FruitCatalog.TREES;
  public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(ChaoGarden.MODID);
  public static final DeferredBlock<ChaoFruitTrunkBlock> TRUNK = BLOCKS.register("chao_fruit_log",
      () -> new ChaoFruitTrunkBlock(BlockBehaviour.Properties.of().mapColor(MapColor.WOOD)
          .strength(2.0f).sound(SoundType.WOOD).noOcclusion()));
  public static final DeferredBlock<DarkFruitTrunkBlock> DARK_TRUNK = BLOCKS.register("dark_chao_fruit_log",
      () -> new DarkFruitTrunkBlock(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_BROWN)
          .strength(2.0f).sound(SoundType.WOOD).noOcclusion()));
  public static final DeferredBlock<LeavesBlock> LEAVES = BLOCKS.register("chao_fruit_leaves",
      () -> new LeavesBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_LEAVES)));
  private static final DeferredRegister<Feature<?>> FEATURES = DeferredRegister.create(Registries.FEATURE, ChaoGarden.MODID);
  public static final DeferredHolder<Feature<?>, FruitTreeFeature> WILD_TREE = FEATURES.register(
      "wild_fruit_tree", () -> new FruitTreeFeature(NoneFeatureConfiguration.CODEC));
  private static final DeferredRegister<LootPoolEntryType> LOOT_ENTRIES =
      DeferredRegister.create(Registries.LOOT_POOL_ENTRY_TYPE, ChaoGarden.MODID);
  static final DeferredHolder<LootPoolEntryType, LootPoolEntryType> REGIONAL_FRUIT =
      LOOT_ENTRIES.register("regional_fruit", () -> new LootPoolEntryType(WildFruit.CODEC));
  private enum Growth { PLANTED, LEGACY, WILD }
  private static final Map<String, DeferredBlock<ChaoSaplingBlock>> SAPLINGS = new LinkedHashMap<>();
  private static final Map<String, DeferredBlock<ChaoTreeCrownBlock>> CROWNS = new LinkedHashMap<>();
  private static final Map<String, DeferredBlock<HangingFruitBlock>> PODS = new LinkedHashMap<>();

  public static final DeferredBlock<Block> PALM_SUPPORT = BLOCKS.register("planted_palm_support",
      () -> new Block(BlockBehaviour.Properties.of().noCollission().noOcclusion().strength(.2f).noLootTable()) {
        @Override protected net.minecraft.world.level.block.RenderShape getRenderShape(net.minecraft.world.level.block.state.BlockState s) { return net.minecraft.world.level.block.RenderShape.INVISIBLE; }
      });
  private static final DeferredRegister<net.minecraft.world.level.block.entity.BlockEntityType<?>> ENTITIES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE,ChaoGarden.MODID);
  public static final DeferredHolder<net.minecraft.world.level.block.entity.BlockEntityType<?>,net.minecraft.world.level.block.entity.BlockEntityType<FruitCrownEntity>> CROWN_ENTITY = ENTITIES.register("planted_palm", () -> net.minecraft.world.level.block.entity.BlockEntityType.Builder.of(FruitCrownEntity::new,CROWNS.values().stream().map(DeferredBlock::get).toArray(Block[]::new)).build(null));

  static {
    ChaoGarden.add("chao_fruit_log", () -> new BlockItem(TRUNK.get(), new Item.Properties()));
    ChaoGarden.add("dark_chao_fruit_log", () -> new BlockItem(DARK_TRUNK.get(), new Item.Properties()));
    ChaoGarden.add("chao_fruit_leaves", () -> new BlockItem(LEAVES.get(), new Item.Properties()));
    for (String fruit : FRUITS) {
      PODS.put(fruit, BLOCKS.register(fruit + "_pod", () -> new HangingFruitBlock(fruit,
          BlockBehaviour.Properties.of().mapColor(MapColor.PLANT).strength(.2f)
              .sound(SoundType.CROP).noOcclusion().noCollission().randomTicks())));
      CROWNS.put(fruit, BLOCKS.register(fruit + "_tree_crown", () -> new ChaoTreeCrownBlock(fruit,
          BlockBehaviour.Properties.of().mapColor(MapColor.PLANT).strength(.5f)
              .sound(SoundType.GRASS).noOcclusion().randomTicks())));
      var sapling = BLOCKS.register(fruit + "_seedling", () -> new ChaoSaplingBlock(fruit,
          BlockBehaviour.Properties.of().mapColor(MapColor.PLANT).instabreak().noCollission()
              .noOcclusion().sound(SoundType.GRASS).randomTicks()));
      SAPLINGS.put(fruit, sapling);
      ChaoGarden.add(seedId(fruit), () -> new FruitSeedItem(sapling.get(), fruit));
    }
    // NeoForge aliases load old item stacks and block palettes as the matching
    // garden variant, retaining stack components and blockstate properties.
    for(var entry:FruitCatalog.REPLACEMENTS.entrySet()) {
      for(String suffix:List.of("", "_seed"))
        ChaoGarden.ITEMS.addAlias(id(entry.getKey()+suffix), id(entry.getValue()+suffix));
      for(String suffix:List.of("_seedling", "_tree_crown", "_pod"))
        BLOCKS.addAlias(id(entry.getKey()+suffix), id(entry.getValue()+suffix));
    }
  }

  private static net.minecraft.resources.ResourceLocation id(String path) {
    return net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(ChaoGarden.MODID,path);
  }

  private FruitContent() {}
  /** Forces deferred registrations before the central item register is attached. */
  public static void bootstrap() {}
  public static void register(IEventBus bus) {
    BLOCKS.register(bus);
    FEATURES.register(bus);
    LOOT_ENTRIES.register(bus);
    ENTITIES.register(bus);
    bus.addListener((FMLCommonSetupEvent event) -> event.enqueueWork(() -> {
      FireBlock fire = (FireBlock) Blocks.FIRE;
      fire.setFlammable(TRUNK.get(), 5, 5);
      fire.setFlammable(DARK_TRUNK.get(), 5, 5);
      fire.setFlammable(LEAVES.get(), 30, 60);
      CROWNS.values().forEach(crown -> fire.setFlammable(crown.get(), 5, 5));
    }));
    NeoForge.EVENT_BUS.addListener(FruitContent::farmerTrades);
  }
  public static List<String> seedIds() { return FruitCatalog.ACTIVE.stream().filter(fruit -> !FruitCatalog.isMushroom(fruit)).map(FruitContent::seedId).toList(); }
  public static List<String> naturalSeedIds() { return FruitCatalog.naturalSeeds(); }
  public static String seedId(String fruit) { return fruit + "_seed"; }
  public static ChaoSaplingBlock saplingForFruit(String fruit) { return SAPLINGS.get(fruit).get(); }
  public static ChaoTreeCrownBlock treeForFruit(String fruit) { return CROWNS.get(fruit).get(); }
  public static HangingFruitBlock podForFruit(String fruit) { return PODS.get(fruit).get(); }
  public static boolean isDark(String fruit){return "dark_garden_fruit".equals(fruit);}
  public static Block trunkForFruit(String fruit){return isDark(fruit)?DARK_TRUNK.get():TRUNK.get();}
  public static boolean isTrunk(net.minecraft.world.level.block.state.BlockState s){return s.is(TRUNK.get())||s.is(DARK_TRUNK.get());}

  public static BlockPos crownPosition(BlockPos base) { return base.above(CROWN_HEIGHT); }

  /** New crowns use the outer branch ends; old saved palms keep their original pods. */
  public static List<BlockPos> podPositions(LevelReader level, BlockPos crown, String fruit) {
    var state = level.getBlockState(crown);
    return podPositions(crown, state.is(treeForFruit(fruit))
        && state.getValue(ChaoTreeCrownBlock.EXPANDED) && !state.getValue(ChaoTreeCrownBlock.GARDEN));
  }

  public static List<BlockPos> podPositions(BlockPos crown, boolean expanded) {
    List<BlockPos> pods = new ArrayList<>(4);
    for (Direction side : Direction.Plane.HORIZONTAL)
      pods.add(crown.below().relative(side, expanded ? 2 : 1));
    return List.copyOf(pods);
  }

  /** Grow only into replaceable space; failed growth never deletes neighboring builds. */
  public static boolean growTree(ServerLevel level, BlockPos base, String fruit) {
    return growTree(level, base, fruit, Growth.PLANTED);
  }

  /** The worldgen entry point cannot introduce market-first crops. */
  public static boolean growNaturalTree(WorldGenLevel level, BlockPos base, String fruit) {
    return fruit != null && fruit.equals(WildFruit.forDimension(level.getLevel().dimension()))
        && growTree(level, base, fruit, Growth.WILD);
  }

  /** Bonemeal can upgrade an old saved palm without changing its fruit or ripeness. */
  public static boolean expandLegacyTree(ServerLevel level, BlockPos crown, String fruit) {
    var state = level.getBlockState(crown);
    return state.is(treeForFruit(fruit)) && !state.getValue(ChaoTreeCrownBlock.EXPANDED)
        && growTree(level, crown.below(3), fruit, Growth.LEGACY);
  }

  public static boolean canExpandLegacyTree(LevelReader level, BlockPos crown, String fruit) {
    var state = level.getBlockState(crown);
    return state.is(treeForFruit(fruit)) && !state.getValue(ChaoTreeCrownBlock.EXPANDED)
        && canGrowTree(level, crown.below(3), fruit, Growth.LEGACY);
  }

  public static boolean canPlantTree(LevelReader level, BlockPos base, String fruit) {
    return canGrowTree(level,base,fruit,Growth.PLANTED);
  }

  private static boolean canGrowTree(LevelReader level, BlockPos base, String fruit, Growth growth) {
    boolean legacy = growth == Growth.LEGACY;
    if (!FRUITS.contains(fruit) || !level.isAreaLoaded(base, CANOPY_RADIUS)
        || base.getY() < level.getMinBuildHeight()
        || base.getY() + TREE_HEIGHT > level.getMaxBuildHeight()) return false;
    if (growth == Growth.WILD && isDark(fruit)) {
      var ground = level.getBlockState(base.below());
      if (!(ground.is(BlockTags.BASE_STONE_NETHER) || ground.is(BlockTags.NYLIUM)
          || ground.is(Blocks.SOUL_SOIL) || ground.is(Blocks.SOUL_SAND) || ground.is(Blocks.GRAVEL))) return false;
    } else if (!saplingForFruit(fruit).defaultBlockState().canSurvive(level, base)) return false;
    if (legacy) {
      for (int y = 0; y < 3; y++) if (!level.getBlockState(base.above(y)).is(TRUNK.get())) return false;
      var old = level.getBlockState(base.above(3));
      if (!old.is(treeForFruit(fruit)) || old.getValue(ChaoTreeCrownBlock.EXPANDED)) return false;
    }
    for (int y = 0; y < TREE_HEIGHT; y++) {
      int radius = y >= 3 ? CANOPY_RADIUS : 0;
      for (int x = -radius; x <= radius; x++) for (int z = -radius; z <= radius; z++) {
        BlockPos at = base.offset(x, y, z);
        var existing = level.getBlockState(at);
        if (growth == Growth.WILD && !existing.getFluidState().isEmpty()) return false;
        boolean oldTrunk = legacy && x == 0 && z == 0 && y < 3 && existing.is(TRUNK.get());
        boolean oldCrown = legacy && x == 0 && z == 0 && y == 3
            && existing.is(treeForFruit(fruit)) && !existing.getValue(ChaoTreeCrownBlock.EXPANDED);
        boolean ownSeedling = y == 0 && x == 0 && z == 0 && existing.is(saplingForFruit(fruit));
        if (!oldTrunk && !oldCrown && !ownSeedling && !existing.canBeReplaced()) return false;
      }
    }
    return true;
  }

  private static boolean growTree(WorldGenLevel level, BlockPos base, String fruit, Growth growth) {
    boolean legacy = growth == Growth.LEGACY;
    if (!canGrowTree(level, base, fruit, growth)) return false;
    Map<Direction, Integer> ripeness = new LinkedHashMap<>();
    if (legacy) for (Direction side : Direction.Plane.HORIZONTAL) {
      BlockPos oldPod = base.above(2).relative(side);
      var state = level.getBlockState(oldPod);
      if (state.is(podForFruit(fruit))) {
        ripeness.put(side, state.getValue(HangingFruitBlock.AGE));
        // Remove before replacing the old crown: its removal hook must not drop
        // ripe fruit and then copy that same crop to the new branch.
        level.removeBlock(oldPod, false);
      }
    }
    for (int y = 0; y < CROWN_HEIGHT; y++) level.setBlock(base.above(y), trunkForFruit(fruit).defaultBlockState(), 3);
    BlockPos crown = crownPosition(base);
    level.setBlock(crown, treeForFruit(fruit).defaultBlockState()
        .setValue(ChaoTreeCrownBlock.EXPANDED, true), 3);
    level.setBlock(crown, level.getBlockState(crown).setValue(ChaoTreeCrownBlock.GARDEN,true),3);
    for (Direction side : Direction.Plane.HORIZONTAL)
      level.setBlock(crown.relative(side),PALM_SUPPORT.get().defaultBlockState(),3);
    if(isDark(fruit))makeBareTree(level,base);
    replenishPods(level, crown, fruit);
    for (var entry : ripeness.entrySet()) {
      BlockPos pod = crown.below().relative(entry.getKey());
      var state = level.getBlockState(pod);
      if (state.is(podForFruit(fruit))) level.setBlock(pod, state.setValue(HangingFruitBlock.AGE, entry.getValue()), 3);
    }
    if (growth != Growth.WILD && level instanceof ServerLevel server) GardenTreeData.get(server).grown(base);
    return true;
  }

  public static void replenishPods(LevelAccessor level, BlockPos crown, String fruit) {
    if(level instanceof ServerLevel server&&GardenTreeData.get(server).withered(crown.below(CROWN_HEIGHT)))return;
    if (!isTrunk(level.getBlockState(crown.below()))) return;
    boolean expanded = level.getBlockState(crown).is(treeForFruit(fruit))
        && level.getBlockState(crown).getValue(ChaoTreeCrownBlock.EXPANDED)
        && !level.getBlockState(crown).getValue(ChaoTreeCrownBlock.GARDEN);
    for (Direction side : Direction.Plane.HORIZONTAL) {
      BlockPos pos = crown.below().relative(side, expanded ? 2 : 1);
      var pod = podForFruit(fruit).defaultBlockState().setValue(HangingFruitBlock.FACING, side.getOpposite());
      if (level.getBlockState(pos).isAir() && pod.canSurvive(level, pos)) {
        level.setBlock(pos, pod, 3);
      }
    }
  }

  public static boolean migratePlantedPalm(ServerLevel level,BlockPos root,String fruit) {
    BlockPos crown=crownPosition(root);var state=level.getBlockState(crown);
    if(isDark(fruit)&&state.is(treeForFruit(fruit))&&state.getValue(ChaoTreeCrownBlock.GARDEN))
      return migrateDarkTree(level,root);
    if(!state.is(treeForFruit(fruit)) || state.getValue(ChaoTreeCrownBlock.GARDEN) || !state.getValue(ChaoTreeCrownBlock.EXPANDED))return false;
    for(Direction side:Direction.Plane.HORIZONTAL) {
      var arm=level.getBlockState(crown.relative(side));var pod=level.getBlockState(crown.below().relative(side));
      if(!(arm.isAir()||arm.is(TRUNK.get())||arm.is(LEAVES.get())) || !(pod.isAir()||pod.is(podForFruit(fruit))))return false;
    }
    Map<Direction,Integer> ages=new LinkedHashMap<>();
    for(Direction side:Direction.Plane.HORIZONTAL) {
      var at=crown.below().relative(side,2);var pod=level.getBlockState(at);
      if(pod.is(podForFruit(fruit))){ages.put(side,pod.getValue(HangingFruitBlock.AGE));level.removeBlock(at,false);}
    }
    for(int y=0;y<3;y++) {int radius=y==2?1:2;
      for(int x=-radius;x<=radius;x++)for(int z=-radius;z<=radius;z++) {
        if(Math.abs(x)==radius&&Math.abs(z)==radius)continue;
        var at=crown.offset(x,y,z);if(level.getBlockState(at).is(LEAVES.get())&&!level.getBlockState(at).getValue(LeavesBlock.PERSISTENT))level.removeBlock(at,false);
      }
    }
    level.setBlock(crown,state.setValue(ChaoTreeCrownBlock.GARDEN,true),3);
    for(Direction side:Direction.Plane.HORIZONTAL)level.setBlock(crown.relative(side),PALM_SUPPORT.get().defaultBlockState(),3);
    replenishPods(level,crown,fruit);
    for(var entry:ages.entrySet()){var at=crown.below().relative(entry.getKey());var pod=level.getBlockState(at);if(pod.is(podForFruit(fruit)))level.setBlock(at,pod.setValue(HangingFruitBlock.AGE,entry.getValue()),3);}
    if(isDark(fruit))migrateDarkTree(level,root);
    return true;
  }

  /** Exact sparse branch layout, using the same native meshes as the Dark Garden. */
  public static Map<BlockPos,net.minecraft.world.level.block.state.BlockState> darkWoodPlan(BlockPos root){
    var plan=new LinkedHashMap<BlockPos,net.minecraft.world.level.block.state.BlockState>();
    var wood=DARK_TRUNK.get().defaultBlockState();
    for(int y=0;y<CROWN_HEIGHT;y++)plan.put(root.above(y),wood);
    var crown=crownPosition(root);
    for(Direction side:Direction.Plane.HORIZONTAL)plan.put(crown.relative(side),wood.setValue(net.minecraft.world.level.block.RotatedPillarBlock.AXIS,side.getAxis()));
    plan.put(crown.above(),wood.setValue(com.chaogarden.hub.DarkGardenDecor0255.Deadwood.BRANCHED,true));
    plan.put(crown.above().east(),wood.setValue(com.chaogarden.hub.DarkGardenDecor0255.Deadwood.BRANCHED,true));
    plan.put(crown.above(2).east(),wood);
    plan.put(crown.above().west(),wood.setValue(com.chaogarden.hub.DarkGardenDecor0255.Deadwood.BRANCHED,true));
    return plan;
  }
  private static void makeBareTree(WorldGenLevel level,BlockPos root){
    darkWoodPlan(root).forEach((at,s)->level.setBlock(at,s,3));
    var crown=crownPosition(root);
    level.setBlock(crown,level.getBlockState(crown).setValue(ChaoTreeCrownBlock.BARE,true),3);
  }
  private static boolean migrateDarkTree(ServerLevel level,BlockPos root){
    var crown=crownPosition(root);var state=level.getBlockState(crown);
    if(state.getValue(ChaoTreeCrownBlock.BARE))return false;
    var plan=darkWoodPlan(root);
    for(var entry:plan.entrySet()){
      var at=entry.getKey();var old=level.getBlockState(at);
      boolean stem=at.getX()==root.getX()&&at.getZ()==root.getZ()&&at.getY()<crown.getY();
      boolean arm=at.getY()==crown.getY();
      if(!(old.canBeReplaced()||stem&&old.is(TRUNK.get())||arm&&old.is(PALM_SUPPORT.get())))return false;
    }
    // No fruit blocks or saved tree data are changed: preserve ripeness and watering.
    makeBareTree(level,root);return true;
  }

  private static void farmerTrades(VillagerTradesEvent event) {
    if (event.getType() != VillagerProfession.FARMER) return;
    for (int index = 0; index < FruitCatalog.FARMER_FRUITS.size(); index++) {
      String fruit = FruitCatalog.FARMER_FRUITS.get(index);
      int tier = Math.min(4, 1 + index / 3);
      int price = fruit.equals("garden_fruit") ? 1 : tier * 2;
      event.getTrades().computeIfAbsent(tier, key -> new java.util.ArrayList<>()).add((trader, random) -> new MerchantOffer(
          new ItemCost(Items.EMERALD, price), new ItemStack(ChaoGarden.item(seedId(fruit))),
          12, tier * 2, .05f));
    }
  }
}
