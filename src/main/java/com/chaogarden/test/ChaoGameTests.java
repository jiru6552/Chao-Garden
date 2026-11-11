package com.chaogarden.test;

import com.chaogarden.ChaoGarden;
import com.chaogarden.ChaoVariants;
import com.chaogarden.GardenConfig;
import com.chaogarden.core.ChaoState;
import com.chaogarden.entity.ChaoEggEntity;
import com.chaogarden.entity.ChaoEntity;
import com.chaogarden.item.CareItem;
import com.chaogarden.item.ChaoCarrierItem;
import com.chaogarden.item.ChaoEggItem;
import com.chaogarden.item.VariantEggRecipe;
import io.netty.buffer.Unpooled;
import java.util.Arrays;
import java.util.Random;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** Runs only in a GameTest-enabled development server. */
@GameTestHolder(ChaoGarden.MODID)
@PrefixGameTestTemplate(false)
public final class ChaoGameTests {
  private static final BlockPos CENTER = new BlockPos(2, 2, 2);

  @GameTest(template = "empty", timeoutTicks = 40)
  public static void legacyRecipeSynchronizationCannotReenableCrafting(GameTestHelper helper) {
    var world=helper.getLevel();
    var ingredients=net.minecraft.core.NonNullList.of(net.minecraft.world.item.crafting.Ingredient.EMPTY,
        net.minecraft.world.item.crafting.Ingredient.of(ChaoGarden.item("normal_egg")));
    var base=new net.minecraft.world.item.crafting.ShapelessRecipe("",net.minecraft.world.item.crafting.CraftingBookCategory.MISC,
        new ItemStack(ChaoGarden.item("nether_chao_egg")),ingredients);
    var recipe=new VariantEggRecipe("nether",base);
    var source=ChaoEggItem.containing(ChaoState.egg(4,true,false,3,new Random(700)),231);
    var untouched=source.copy();
    var input=net.minecraft.world.item.crafting.CraftingInput.of(1,1,java.util.List.of(source));
    helper.assertTrue(!recipe.matches(input,world)&&recipe.assemble(input,world.registryAccess()).isEmpty(),"Direct legacy assembly reenabled an egg conversion");
    var buffer=new RegistryFriendlyByteBuf(Unpooled.buffer(),world.registryAccess());
    try {
      var serializer=ChaoGarden.THEMED_EGG_RECIPE.get();serializer.streamCodec().encode(buffer,recipe);
      var synchronizedRecipe=serializer.streamCodec().decode(buffer);
      helper.assertTrue(!synchronizedRecipe.matches(input,world)&&synchronizedRecipe.assemble(input,world.registryAccess()).isEmpty(),"Client recipe synchronization reenabled crafting");
      helper.assertTrue(ItemStack.matches(source,untouched),"Rejected recipe mutated inherited egg components");
    } finally {buffer.release();}
    helper.succeed();
  }

  @GameTest(template = "empty", timeoutTicks = 40)
  public static void variantEggsRespectInstalledMods(GameTestHelper helper) {
    var world = helper.getLevel();
    var keeper = helper.makeMockPlayer(GameType.SURVIVAL);
    keeper.getAbilities().mayBuild = true;
    keeper.getAbilities().instabuild = false;
    helper.setBlock(CENTER.below(), Blocks.STONE);
    BlockPos absolute = helper.absolutePos(CENTER);
    BlockPos floor = absolute.below();
    var hit = new BlockHitResult(Vec3.atCenterOf(floor).add(0, .5, 0), Direction.UP, floor, false);
    int enabledCount = 0;
    for (var definition : ChaoVariants.all()) {
      var item = ChaoGarden.item(ChaoVariants.itemId(definition.id()));
      boolean enabled = ChaoVariants.obtainable(definition)&&ChaoVariants.available(definition);
      if (enabled) enabledCount++;
      var recipe = world.getRecipeManager().byKey(
          ResourceLocation.fromNamespaceAndPath(ChaoGarden.MODID, ChaoVariants.itemId(definition.id())));
      helper.assertTrue(recipe.isEmpty(),"Egg conversion must remain absent even with its mod loaded: "+definition.id());
      helper.assertTrue(ChaoGarden.visibleInCreative(item) == enabled,
          "Creative gate disagrees with the actual mod list for " + definition.id());
      ItemStack stack = new ItemStack(item);
      keeper.setItemInHand(InteractionHand.MAIN_HAND, stack);
      var result = item.useOn(new UseOnContext(keeper, InteractionHand.MAIN_HAND, hit));
      var eggs = world.getEntitiesOfClass(ChaoEggEntity.class, new AABB(absolute).inflate(.5),
          e -> keeper.getUUID().equals(e.owner));
      if (enabled) {
        helper.assertTrue(result.consumesAction() && eggs.size() == 1,
            "Enabled themed egg failed to place: " + definition.id());
        helper.assertTrue(stack.isEmpty(), "Survival themed egg was not consumed");
        helper.assertTrue(eggs.getFirst().state().variant.equals(definition.id()),
            "Placed themed egg lost its heritage");
      } else {
        helper.assertTrue(result == net.minecraft.world.InteractionResult.FAIL && eggs.isEmpty(),
            "Absent mod leaked a placeable themed egg: " + definition.id());
        helper.assertTrue(stack.getCount() == 1, "Rejected egg was consumed");
        // Custom data on a normal shell must obey the same availability gate.
        ChaoState embedded = new ChaoState();
        embedded.variant = definition.id();
        var hidden = new ItemStack(ChaoGarden.item("normal_egg"));
        CompoundTag payload = new CompoundTag();
        payload.putString("Chao", embedded.encode());
        hidden.set(DataComponents.CUSTOM_DATA, CustomData.of(payload));
        keeper.setItemInHand(InteractionHand.MAIN_HAND, hidden);
        var hiddenResult = hidden.getItem().useOn(
            new UseOnContext(keeper, InteractionHand.MAIN_HAND, hit));
        helper.assertTrue(hiddenResult == net.minecraft.world.InteractionResult.FAIL,
            "Normal-shell custom data bypassed absent-mod gate: " + definition.id());
        helper.assertTrue(hidden.getCount() == 1
                && hidden.get(DataComponents.CUSTOM_DATA).copyTag().getString("Chao")
                    .equals(embedded.encode()),
            "Rejected custom egg lost its saved heritage");
      }
      eggs.forEach(net.minecraft.world.entity.Entity::discard);
    }
    if (net.neoforged.fml.ModList.get().isLoaded("chao_variant_fixtures"))
      helper.assertTrue(enabledCount == 2,
          "Loaded integrations bypassed the addon acquisition pause");
    keeper.getInventory().clearContent();
    helper.succeed();
  }

  @GameTest(template = "empty", timeoutTicks = 40)
  public static void chaoSaveLoadPreservesState(GameTestHelper helper) {
    var world = helper.getLevel();
    ChaoEntity original = ChaoGarden.CHAO.get().create(world);
    ChaoState state = ChaoState.egg(8, true, true, 3, new Random(412));
    state.train(ChaoState.RUN, 1450, new Random(413));
    state.giveAnimal(6);
    state.happiness = 81;
    state.bond = 57;
    state.reincarnations = 1;
    original.setState(state);
    original.owner = UUID.fromString("00000000-0000-0000-0000-000000000041");
    original.setCustomName(Component.literal("Persistence Chao"));
    CompoundTag saved = new CompoundTag();
    original.saveWithoutId(saved);
    ChaoEntity restored = ChaoGarden.CHAO.get().create(world);
    restored.load(saved);
    helper.assertTrue(
        restored.state().encode().equals(state.encode()),
        "Chao simulation changed during NBT save/load");
    helper.assertTrue(original.owner.equals(restored.owner), "Keeper was not restored");
    helper.assertTrue(
        restored.getName().getString().equals("Persistence Chao"), "Chao name was not restored");
    original.discard();
    restored.discard();
    helper.succeed();
  }

  @GameTest(template = "empty", timeoutTicks = 40)
  public static void eggSaveLoadPreservesIncubationAndIdentity(GameTestHelper helper) {
    var world = helper.getLevel();
    ChaoEggEntity original = ChaoGarden.CHAO_EGG.get().create(world);
    ChaoState state = ChaoState.egg(2, true, false, 5, new Random(414));
    original.setState(state);
    original.setIncubation(217);
    original.owner = UUID.fromString("00000000-0000-0000-0000-000000000042");
    original.chaoName = Component.literal("Second Life");
    CompoundTag saved = new CompoundTag();
    original.saveWithoutId(saved);
    ChaoEggEntity restored = ChaoGarden.CHAO_EGG.get().create(world);
    restored.load(saved);
    helper.assertTrue(
        restored.state().encode().equals(state.encode()), "Egg genes changed during save/load");
    helper.assertTrue(original.owner.equals(restored.owner), "Egg keeper was not restored");
    helper.assertTrue(
        restored.chaoName != null && restored.chaoName.getString().equals("Second Life"),
        "Reincarnated name was not restored");
    helper.assertTrue(
        Math.abs(original.hatchProgress() - restored.hatchProgress()) < .0001,
        "Incubation progress was not restored");
    original.discard();
    restored.discard();
    helper.succeed();
  }

  @GameTest(template = "empty", timeoutTicks = 60)
  public static void eggHatchesIntoSameChao(GameTestHelper helper) {
    var world = helper.getLevel();
    helper.setBlock(CENTER.below(), Blocks.STONE);
    BlockPos absolute = helper.absolutePos(CENTER);
    ChaoState state = ChaoState.egg(7, true, true, 4, new Random(415));
    state.reincarnations = 2;
    state.points[ChaoState.POWER] = 34;
    UUID keeper = UUID.fromString("00000000-0000-0000-0000-000000000043");
    ChaoEggEntity egg = ChaoGarden.CHAO_EGG.get().create(world);
    egg.setState(state);
    egg.owner = keeper;
    egg.chaoName = Component.literal("Hatch Test");
    egg.setIncubation(GardenConfig.HATCH_TICKS.get() - 1);
    egg.setPos(absolute.getX() + .5, absolute.getY(), absolute.getZ() + .5);
    world.addFreshEntity(egg);
    helper.runAfterDelay(
        4,
        () -> {
          var nearby =
              world.getEntitiesOfClass(
                  ChaoEntity.class, new AABB(absolute).inflate(2), c -> keeper.equals(c.owner));
          helper.assertTrue(egg.isRemoved(), "Hatched egg remains in world");
          helper.assertTrue(nearby.size() == 1, "Egg did not create exactly one Chao");
          ChaoEntity baby = nearby.get(0);
          helper.assertTrue(
              Arrays.equals(baby.state().colorGenes, state.colorGenes),
              "Hatching changed color genes");
          helper.assertTrue(
              Arrays.equals(baby.state().jewelGenes, state.jewelGenes),
              "Hatching changed jewel genes");
          helper.assertTrue(
              baby.state().isShiny() && baby.state().isTwoTone(),
              "Hatching lost texture phenotype");
          helper.assertTrue(
              baby.state().reincarnations == 2 && baby.state().points[ChaoState.POWER] == 34,
              "Hatching reset life history");
          helper.assertTrue(baby.getName().getString().equals("Hatch Test"), "Hatching lost name");
          baby.discard();
          helper.succeed();
        });
  }

  @GameTest(template = "empty", timeoutTicks = 40)
  public static void breedingProducesInheritedEgg(GameTestHelper helper) {
    var world = helper.getLevel();
    BlockPos absolute = helper.absolutePos(CENTER);
    ChaoEntity red = ChaoGarden.CHAO.get().create(world);
    ChaoEntity blue = ChaoGarden.CHAO.get().create(world);
    var a = ChaoState.egg(2, true, false, 0, new Random(416));
    var b = ChaoState.egg(4, false, true, 0, new Random(417));
    a.adult = b.adult = true;
    a.variant=b.variant="create";
    a.gradeA = a.gradeB = new int[] {5, 4, 3, 2, 1};
    b.gradeA = b.gradeB = new int[] {0, 1, 2, 3, 4};
    red.setState(a);
    blue.setState(b);
    red.owner = UUID.fromString("00000000-0000-0000-0000-000000000044");
    red.setPos(absolute.getX() + .5, absolute.getY(), absolute.getZ() + .5);
    blue.setPos(absolute.getX() + 1.5, absolute.getY(), absolute.getZ() + .5);
    red.setNoAi(true);
    blue.setNoAi(true);
    world.addFreshEntity(red);
    world.addFreshEntity(blue);
    red.setInLove(null);
    blue.setInLove(null);
    var direct=(ChaoEntity)red.getBreedOffspring(world,blue);
    helper.assertTrue(direct!=null&&direct.state().variant.equals("normal"),"Direct offspring inherited a paused addon theme");
    direct.discard();
    red.spawnChildFromBreeding(world, blue);
    var eggs =
        world.getEntitiesOfClass(
            ChaoEggEntity.class, new AABB(absolute).inflate(2), e -> red.owner.equals(e.owner));
    helper.assertTrue(eggs.size() == 1, "Breeding did not produce exactly one egg");
    ChaoState child = eggs.get(0).state();
    helper.assertTrue(child.variant.equals("normal")&&red.state().variant.equals("create")&&blue.state().variant.equals("create"),
        "Egg inheritance bypassed addon pause or erased parent heritage");
    helper.assertTrue(
        child.colorGenes[0] == 2 && child.colorGenes[1] == 4,
        "Offspring color allele did not come from each parent");
    helper.assertTrue(
        child.shinyGenes[0] == 1 && child.shinyGenes[1] == 0 && child.isShiny(),
        "Shiny inheritance is incorrect");
    helper.assertTrue(
        child.toneGenes[0] == 0 && child.toneGenes[1] == 1, "Tone inheritance is incorrect");
    for (int stat = 0; stat < 5; stat++) {
      helper.assertTrue(
          child.gradeA[stat] == a.gradeA[stat] && child.gradeB[stat] == b.gradeA[stat],
          "Stat grade allele inheritance is incorrect");
    }
    helper.assertTrue(!red.isInLove() && !blue.isInLove(), "Parents stayed ready after breeding");
    helper.assertTrue(!red.canMate(blue), "Parents may immediately breed again");
    eggs.forEach(ChaoEggEntity::discard);
    red.discard();
    blue.discard();
    helper.succeed();
  }

  @GameTest(template = "empty", timeoutTicks = 40)
  public static void survivalLootTablesResolveAndProduceItems(GameTestHelper helper) {
    var world = helper.getLevel();
    var params =
        new LootParams.Builder(world)
            .withParameter(LootContextParams.ORIGIN, Vec3.atCenterOf(helper.absolutePos(CENTER)))
            .create(LootContextParamSets.CHEST);
    String[] tables = {"enemy_supplies","rescued_animals"};
    for (String tableName : tables) {
      var key =
          ResourceKey.create(
              Registries.LOOT_TABLE,
              ResourceLocation.fromNamespaceAndPath(ChaoGarden.MODID, "bonus/" + tableName));
      var table = world.getServer().reloadableRegistries().getLootTable(key);
      boolean produced = false;
      // Nearby integer seeds have correlated first floats in Minecraft's
      // LegacyRandomSource. Mix them before testing low-probability pools.
      Random seeds = new Random(0x4348414fL + tableName.hashCode());
      for (int seed = 0; seed < 256 && !produced; seed++) {
        var drops = table.getRandomItems(params, seeds.nextLong());
        for (var drop : drops) {
          helper.assertTrue(
              BuiltInRegistries.ITEM.getKey(drop.getItem()).getNamespace().equals(ChaoGarden.MODID),
              "Bonus table produced invalid item");
          produced = true;
        }
      }
      helper.assertTrue(produced, "Missing or nonfunctional survival loot table: " + tableName);
    }
    helper.succeed();
  }

  @GameTest(template = "empty", timeoutTicks = 40)
  public static void vanillaLootRetainsDropsAndAddsChaoItems(GameTestHelper helper) {
    var world = helper.getLevel();
    Vec3 origin = Vec3.atCenterOf(helper.absolutePos(CENTER));
    var chestParams =
        new LootParams.Builder(world)
            .withParameter(LootContextParams.ORIGIN, origin)
            .create(LootContextParamSets.CHEST);
    var templeKey =
        ResourceKey.create(
            Registries.LOOT_TABLE, ResourceLocation.withDefaultNamespace("chests/desert_pyramid"));
    var temple = world.getServer().reloadableRegistries().getLootTable(templeKey);
    Random seeds = new Random(0x54454d504c45L);
    boolean vanillaChestLoot = false, chaoEgg = false;
    for (int sample = 0; sample < 4096 && !(vanillaChestLoot && chaoEgg); sample++) {
      for (var stack : temple.getRandomItems(chestParams, seeds.nextLong())) {
        vanillaChestLoot |=
            BuiltInRegistries.ITEM.getKey(stack.getItem()).getNamespace().equals("minecraft");
        chaoEgg |= stack.getItem() instanceof ChaoEggItem;
      }
    }
    helper.assertTrue(vanillaChestLoot, "Desert pyramid lost its original vanilla loot");
    helper.assertTrue(chaoEgg, "Desert pyramid global modifier did not add a Chao egg");

    var zombie = EntityType.ZOMBIE.create(world);
    zombie.setPos(origin);
    var entityParams =
        new LootParams.Builder(world)
            .withParameter(LootContextParams.ORIGIN, origin)
            .withParameter(LootContextParams.THIS_ENTITY, zombie)
            .withParameter(LootContextParams.DAMAGE_SOURCE, world.damageSources().generic())
            .create(LootContextParamSets.ENTITY);
    var zombieKey =
        ResourceKey.create(
            Registries.LOOT_TABLE, ResourceLocation.withDefaultNamespace("entities/zombie"));
    var zombieTable = world.getServer().reloadableRegistries().getLootTable(zombieKey);
    seeds = new Random(0x5a4f4d424945L);
    boolean vanillaMobLoot = false, careSupply = false;
    for (int sample = 0; sample < 256 && !(vanillaMobLoot && careSupply); sample++) {
      for (var stack : zombieTable.getRandomItems(entityParams, seeds.nextLong())) {
        vanillaMobLoot |=
            BuiltInRegistries.ITEM.getKey(stack.getItem()).getNamespace().equals("minecraft");
        careSupply |= stack.getItem() instanceof CareItem;
      }
    }
    helper.assertTrue(vanillaMobLoot, "Zombie lost its original vanilla loot");
    helper.assertTrue(careSupply, "Zombie global modifier did not add Chao care supplies");
    zombie.discard();
    helper.succeed();
  }

  @GameTest(template = "empty", timeoutTicks = 40)
  public static void recoveredEggCanBePlacedWithoutLosingHistory(GameTestHelper helper) {
    var world = helper.getLevel();
    var keeper = helper.makeMockPlayer(GameType.SURVIVAL);
    keeper.getAbilities().mayBuild = true;
    keeper.getAbilities().instabuild = false;
    helper.setBlock(CENTER.below(), Blocks.STONE);
    BlockPos absolute = helper.absolutePos(CENTER);
    ChaoState state = ChaoState.egg(12, true, true, 6, new Random(418));
    state.reincarnations = 2;
    state.points[ChaoState.FLY] = 59;
    ChaoEggEntity egg = ChaoGarden.CHAO_EGG.get().create(world);
    egg.setState(state);
    egg.owner = keeper.getUUID();
    egg.chaoName = Component.literal("Remember Me");
    egg.setIncubation(231);
    egg.setPos(Vec3.atBottomCenterOf(absolute));
    world.addFreshEntity(egg);

    helper.assertTrue(
        egg.interact(keeper, InteractionHand.MAIN_HAND).consumesAction(),
        "Egg recovery interaction was rejected");
    helper.assertTrue(egg.isRemoved(), "Recovered egg remains in world");
    ItemStack recovered = ItemStack.EMPTY;
    int count = 0;
    for (int slot = 0; slot < keeper.getInventory().getContainerSize(); slot++) {
      ItemStack stack = keeper.getInventory().getItem(slot);
      if (stack.getItem() instanceof ChaoEggItem) {
        recovered = stack.copy();
        count += stack.getCount();
      }
    }
    helper.assertTrue(count == 1, "Egg recovery did not yield exactly one egg item");
    CompoundTag payload =
        recovered.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
    helper.assertTrue(
        payload.getString("Chao").equals(state.encode()),
        "Recovered egg item lost its simulation state");
    helper.assertTrue(
        payload.getInt("HatchTicks") == 231, "Recovered egg item lost incubation progress");
    Component name =
        Component.Serializer.fromJson(payload.getString("ChaoName"), world.registryAccess());
    helper.assertTrue(
        name != null && name.getString().equals("Remember Me"),
        "Recovered egg item lost reincarnated name");

    keeper.getInventory().clearContent();
    keeper.setItemInHand(InteractionHand.MAIN_HAND, recovered);
    BlockPos floor = absolute.below();
    var hit = new BlockHitResult(Vec3.atCenterOf(floor).add(0, .5, 0), Direction.UP, floor, false);
    var result =
        recovered.getItem().useOn(new UseOnContext(keeper, InteractionHand.MAIN_HAND, hit));
    helper.assertTrue(result.consumesAction(), "Recovered egg item could not be placed");
    helper.assertTrue(
        keeper.getMainHandItem().isEmpty(),
        "Survival placement did not consume recovered egg item");
    var eggs =
        world.getEntitiesOfClass(
            ChaoEggEntity.class,
            new AABB(absolute).inflate(1),
            e -> keeper.getUUID().equals(e.owner));
    helper.assertTrue(eggs.size() == 1, "Recovered egg placement did not create exactly one egg");
    ChaoEggEntity placed = eggs.get(0);
    helper.assertTrue(
        placed.state().encode().equals(state.encode()),
        "Egg placement changed inherited genes or life history");
    helper.assertTrue(
        placed.chaoName != null && placed.chaoName.getString().equals("Remember Me"),
        "Egg placement lost reincarnated name");
    CompoundTag placedTag = new CompoundTag();
    placed.saveWithoutId(placedTag);
    helper.assertTrue(placedTag.getInt("Incubation") == 231, "Egg placement reset incubation");
    placed.discard();
    helper.succeed();
  }

  @GameTest(template = "empty", timeoutTicks = 40)
  public static void carrierRoundTripPreservesChaoAndClearsItem(GameTestHelper helper) {
    var world = helper.getLevel();
    var keeper = helper.makeMockPlayer(GameType.SURVIVAL);
    keeper.getAbilities().mayBuild = true;
    helper.setBlock(CENTER.below(), Blocks.STONE);
    BlockPos absolute = helper.absolutePos(CENTER);
    ChaoState state = ChaoState.egg(4, true, false, 2, new Random(419));
    state.adult = true;
    state.primaryType = ChaoState.FLY_TYPE;
    state.train(ChaoState.FLY, 1740, new Random(420));
    state.giveAnimal(5);
    state.bond = 84;
    ChaoEntity chao = ChaoGarden.CHAO.get().create(world);
    chao.setState(state);
    chao.owner = keeper.getUUID();
    chao.setCustomName(Component.literal("Travel Friend"));
    chao.setPos(Vec3.atBottomCenterOf(absolute));
    world.addFreshEntity(chao);
    UUID originalId = chao.getUUID();
    var carrier = new ItemStack(ChaoGarden.item("chao_carrier"));

    helper.assertTrue(
        ChaoCarrierItem.capture(carrier, chao, keeper), "Keeper could not capture Chao");
    helper.assertTrue(
        chao.isRemoved() && ChaoCarrierItem.filled(carrier),
        "Capture did not remove Chao and fill carrier");
    helper.assertTrue(
        !ChaoCarrierItem.capture(carrier, chao, keeper), "Filled carrier accepted another capture");
    keeper.setItemInHand(InteractionHand.MAIN_HAND, carrier);
    BlockPos floor = absolute.below();
    var hit = new BlockHitResult(Vec3.atCenterOf(floor).add(0, .5, 0), Direction.UP, floor, false);
    var result = carrier.getItem().useOn(new UseOnContext(keeper, InteractionHand.MAIN_HAND, hit));
    helper.assertTrue(result.consumesAction(), "Filled carrier could not release Chao");
    helper.assertTrue(carrier.isEmpty() && !ChaoCarrierItem.filled(carrier),
        "Release left an empty carrier item behind");
    helper.assertTrue(
        !carrier.has(DataComponents.CUSTOM_NAME), "Empty carrier retained occupied name");
    var released =
        world.getEntitiesOfClass(
            ChaoEntity.class, new AABB(absolute).inflate(1), c -> keeper.getUUID().equals(c.owner));
    helper.assertTrue(released.size() == 1, "Carrier did not release exactly one Chao");
    ChaoEntity friend = released.get(0);
    helper.assertTrue(
        friend.state().encode().equals(state.encode()),
        "Carrier changed stats, genes, animal parts or care history");
    helper.assertTrue(
        friend.getName().getString().equals("Travel Friend"), "Carrier lost Chao name");
    helper.assertTrue(
        friend.getUUID().equals(originalId), "Released Chao lost its stable garden identity");
    friend.discard();
    helper.succeed();
  }

  @GameTest(template = "empty", timeoutTicks = 40)
  public static void chaosChaoResistsHazardsButAllowsAdministrativeRemoval(GameTestHelper helper) {
    var world = helper.getLevel();
    ChaoEntity chao = ChaoGarden.CHAO.get().create(world);
    var state = ChaoState.random(new Random(421));
    state.adult = state.immortal = true;
    chao.setState(state);
    float health = chao.getHealth();
    helper.assertTrue(
        !chao.hurt(world.damageSources().generic(), 100), "Immortal Chao took ordinary damage");
    helper.assertTrue(
        !chao.hurt(world.damageSources().lava(), 100), "Immortal Chao took lava damage");
    helper.assertTrue(
        !chao.hurt(world.damageSources().drown(), 100), "Immortal Chao took drowning damage");
    helper.assertTrue(chao.getHealth() == health, "Hazards changed immortal Chao health");
    helper.assertTrue(
        chao.hurt(world.damageSources().genericKill(), 1000), "Administrative removal was blocked");
    helper.assertTrue(chao.getHealth() <= 0, "Administrative damage did not remove immortal Chao");
    chao.discard();
    helper.succeed();
  }

  @GameTest(template = "empty", timeoutTicks = 40)
  public static void spawnEggSelfParentSafe(GameTestHelper helper) {
    var world = helper.getLevel();
    ChaoEntity child = ChaoGarden.CHAO.get().create(world);
    ChaoEntity otherChild = ChaoGarden.CHAO.get().create(world);
    ChaoEntity chaos = ChaoGarden.CHAO.get().create(world);
    var chaosState = ChaoState.egg(9, true, true, 3, new Random(422));
    chaosState.adult = chaosState.immortal = true;
    chaosState.reincarnations = 2;
    chaos.setState(chaosState);

    // Vanilla SpawnEggItem passes the clicked entity as both parents,
    // including when it is too young or immortal and cannot breed.
    var fromChild = child.getBreedOffspring(world, child);
    helper.assertTrue(
        fromChild instanceof ChaoEntity, "Spawn egg self-parent path failed for child");
    ChaoEntity first = (ChaoEntity) fromChild;
    helper.assertTrue(
        first != child && !first.state().adult && !first.state().immortal,
        "Spawn egg child path did not create a fresh child");
    var fromChaos = chaos.getBreedOffspring(world, chaos);
    helper.assertTrue(
        fromChaos instanceof ChaoEntity, "Spawn egg self-parent path failed for Chaos Chao");
    ChaoEntity second = (ChaoEntity) fromChaos;
    helper.assertTrue(
        second != chaos
            && !second.state().adult
            && !second.state().immortal
            && second.state().reincarnations == 0,
        "Spawn egg inherited immortal or adult lifecycle from clicked Chaos Chao");

    helper.assertTrue(
        child.getBreedOffspring(world, otherChild) == null,
        "Two child parents produced offspring instead of being rejected");
    helper.assertTrue(
        chaos.getBreedOffspring(world, child) == null,
        "Chaos Chao bred with an ineligible partner");
    first.discard();
    second.discard();
    child.discard();
    otherChild.discard();
    chaos.discard();
    helper.succeed();
  }
}
