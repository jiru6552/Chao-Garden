package com.chaogarden;

import com.chaogarden.entity.ChaoEggEntity;
import com.chaogarden.entity.ChaoEntity;
import com.chaogarden.item.*;
import java.util.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.*;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.registries.*;

@Mod(ChaoGarden.MODID)
public final class ChaoGarden {
  public static final String MODID = "chao_garden";
  public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(MODID);
  public static final DeferredRegister<EntityType<?>> ENTITIES =
      DeferredRegister.create(Registries.ENTITY_TYPE, MODID);
  public static final DeferredRegister<CreativeModeTab> TABS =
      DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MODID);
  public static final DeferredRegister<RecipeSerializer<?>> RECIPE_SERIALIZERS =
      DeferredRegister.create(Registries.RECIPE_SERIALIZER, MODID);
  public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<VariantEggRecipe>> THEMED_EGG_RECIPE =
      RECIPE_SERIALIZERS.register("themed_egg", VariantEggRecipe.Serializer::new);
  public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<KeeperJournalRecipe>> JOURNAL_RECIPE =
      RECIPE_SERIALIZERS.register("keeper_journal", KeeperJournalRecipe.Serializer::new);
  public static final DeferredHolder<EntityType<?>, EntityType<ChaoEntity>> CHAO =
      ENTITIES.register(
          "chao",
          () ->
              EntityType.Builder.of(ChaoEntity::new, MobCategory.CREATURE)
                  .sized(0.55f, 0.85f)
                  .eyeHeight(0.63f)
                  .clientTrackingRange(10)
                  .build("chao"));
  public static final DeferredHolder<EntityType<?>, EntityType<ChaoEggEntity>> CHAO_EGG =
      ENTITIES.register(
          "chao_egg",
          () ->
              EntityType.Builder.<ChaoEggEntity>of(ChaoEggEntity::new, MobCategory.MISC)
                  .sized(0.5f, 0.65f)
                  .clientTrackingRange(10)
                  .updateInterval(1)
                  .build("chao_egg"));
  public static final Map<String, DeferredItem<? extends Item>> CONTENT = new LinkedHashMap<>();
  public static final String[] COLORS = {
    "normal",
    "white",
    "red",
    "yellow",
    "blue",
    "aqua",
    "pink",
    "orange",
    "purple",
    "brown",
    "green",
    "grey",
    "lime",
    "black",
    "sky_blue",
    "cyan"
  };
  public static final String[] JEWELS = {
    "none",
    "silver",
    "gold",
    "ruby",
    "sapphire",
    "emerald",
    "amethyst",
    "aquamarine",
    "garnet",
    "onyx",
    "diamond",
    "peridot",
    "topaz",
    "moon"
  };
  public static final String[] ANIMALS = {
    "penguin",
    "seal",
    "otter",
    "peacock",
    "parrot",
    "condor",
    "rabbit",
    "cheetah",
    "warthog",
    "bear",
    "tiger",
    "gorilla",
    "skunk",
    "sheep",
    "raccoon",
    "dragon",
    "unicorn",
    "phoenix",
    "half_fish",
    "skeleton_dog",
    "bat", "bee", "ladybug", "scorpion", "deer", "elephant", "kangaroo",
    "koala", "lion", "mole", "swallow"
  };

  static {
    for (int c = 0; c < COLORS.length; c++) {
      final int color = c;
      add(COLORS[c] + "_egg", () -> new ChaoEggItem(color, false, 0));
      if (c > 0) add("shiny_" + COLORS[c] + "_egg", () -> new ChaoEggItem(color, true, 0));
    }
    add("shiny_egg", () -> new ChaoEggItem(0, true, 0));
    for (int j = 1; j < JEWELS.length; j++) {
      final int jewel = j;
      add(JEWELS[j] + "_jewel_egg", () -> new ChaoEggItem(0, false, jewel));
    }
    for (var variant : ChaoVariants.all())
      add(ChaoVariants.itemId(variant.id()), () -> new ChaoEggItem(0, false, 0, variant.id()));
    for (String retired : ChaoVariants.retiredIds())
      add(ChaoVariants.itemId(retired), () -> new RetiredChaoEggItem());
    for (String fruit : com.chaogarden.fruit.FruitCatalog.registeredFoods())
      add(fruit, () -> new CareItem(CareItem.Kind.FRUIT, fruit, 0));
    String[] stats = {"swim", "fly", "run", "power"};
    for (int i = 0; i < 4; i++) {
      final int stat = i;
      add(stats[i] + "_drive", () -> new CareItem(CareItem.Kind.DRIVE, stats[stat], stat));
    }
    for (var tier : com.chaogarden.core.DriveRules.Tier.values()) {
      String prefix=tier.name().toLowerCase(java.util.Locale.ROOT);
      if (com.chaogarden.core.DriveRules.typed(tier)) {
        for (int i=0;i<4;i++) { final int stat=i;
          add(prefix+"_"+stats[i]+"_drive", () -> new com.chaogarden.item.CombinedDriveItem(tier,stat));
        }
      } else add(prefix+"_drive", () -> new com.chaogarden.item.CombinedDriveItem(tier,-1));
    }
    // Only the original21 samples shipped with compatibility _animal IDs.
    for (int i = 0; i < 21; i++) {
      final int animal = i;
      add(
          ANIMALS[i] + "_animal",
          () -> new CareItem(CareItem.Kind.ANIMAL, ANIMALS[animal], animal));
    }
    add("chao_key", ChaoKeyItem::new);
    add("chao_analyzer", () -> new Item(new Item.Properties().stacksTo(1)));
    add("chao_carrier", ChaoCarrierItem::new);
    add("garden_guide", GardenGuideItem::new);
    add(
        "chao_spawn_egg",
        () ->
            new net.neoforged.neoforge.common.DeferredSpawnEggItem(
                CHAO, 0x68cde8, 0xffd75d, new Item.Properties()));
  }

  public static final DeferredHolder<CreativeModeTab, CreativeModeTab> TAB =
      TABS.register(
          "garden",
          () ->
              CreativeModeTab.builder()
                  .title(Component.literal("Chao Garden"))
                  .icon(() -> item("normal_egg").getDefaultInstance())
                  .displayItems((p, out) -> CONTENT.values().forEach(i -> {
                    Item item = i.get();
                    if (visibleInCreative(item)) out.accept(item);
                  }))
                  .build());
  public static final DeferredHolder<CreativeModeTab, CreativeModeTab> SCENERY_TAB =
      TABS.register("scenery", () -> CreativeModeTab.builder()
          .title(Component.translatable("itemGroup.chao_garden.scenery"))
          .icon(() -> com.chaogarden.kindergarten.KindergartenDecor025.item("school_globe").getDefaultInstance())
          .displayItems((parameters, output) -> {
            com.chaogarden.kindergarten.KindergartenDecor025.ids()
                .forEach(id -> output.accept(com.chaogarden.kindergarten.KindergartenDecor025.item(id)));
            com.chaogarden.kindergarten.KindergartenHallThemes0252.ids()
                .forEach(id -> output.accept(com.chaogarden.kindergarten.KindergartenHallThemes0252.item(id)));
            com.chaogarden.hub.HeroGardenDecor0253.ids()
                .forEach(id -> output.accept(com.chaogarden.hub.HeroGardenDecor0253.item(id)));
            com.chaogarden.hub.DarkGardenDecor0255.ids()
                .forEach(id -> output.accept(com.chaogarden.hub.DarkGardenDecor0255.item(id)));
          })
          .build());

  public static void add(String id, java.util.function.Supplier<? extends Item> factory) {
    if (CONTENT.containsKey(id)) throw new IllegalArgumentException("Duplicate Chao item " + id);
    CONTENT.put(id, ITEMS.register(id, factory));
  }

  public static Item item(String id) {
    return CONTENT.get(com.chaogarden.fruit.FruitCatalog.canonical(id.equals("robot_component")?"omochao_parts_box":id.equals("garden_bell")?"garden_block":id)).get();
  }

  public static boolean visibleInCreative(Item item) {
    if(item instanceof com.chaogarden.market.LegacyMelonHatItem)return false;
    if(item instanceof com.chaogarden.activities.GardenBellItem || item instanceof com.chaogarden.toys.RetiredCoffinItem)return false;
    if(item instanceof BlockItem block && block.getBlock() instanceof com.chaogarden.care.ChaoSchoolBlock)return false;
    if (item instanceof RetiredChaoEggItem || item instanceof ChaoCarrierItem
        || item instanceof com.chaogarden.cwe.RetiredCweGiftItem) return false;
    if (com.chaogarden.fruit.FruitCatalog.isLegacyItem(
        net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(item).getPath())) return false;
    if (item instanceof CareItem care && care.kind == CareItem.Kind.ANIMAL
        && !(item instanceof com.chaogarden.genes.GeneItem)) return false;
    return !(item instanceof ChaoEggItem egg) || (ChaoVariants.obtainable(egg.variant)&&ChaoVariants.available(egg.variant));
  }

  public ChaoGarden(IEventBus bus, ModContainer container) {
    com.chaogarden.genes.GeneContent.bootstrap();
    com.chaogarden.fruit.FruitContent.bootstrap();
    com.chaogarden.market.MarketContent.bootstrap();
    com.chaogarden.cwe.CweContent.bootstrap();
    com.chaogarden.care.DoctorContent.bootstrap();
    com.chaogarden.activities.ActivityContent.bootstrap();
    com.chaogarden.companion.CompanionContent.bootstrap();
    com.chaogarden.music.GardenMusicContent.bootstrap();
    com.chaogarden.hub.HubContent.bootstrap();
    ITEMS.register(bus);
    ENTITIES.register(bus);
    TABS.register(bus);
    RECIPE_SERIALIZERS.register(bus);
    ChaoSounds.register(bus);
    com.chaogarden.toys.ChaoToyParticles.register(bus);
    com.chaogarden.hub.HubContent.register(bus);
    com.chaogarden.hub.LobbyDecor025.register(bus);
    com.chaogarden.hub.NeutralGardenDecor02510.register(bus);
    com.chaogarden.hub.HeroGardenDecor0253.register(bus);
    com.chaogarden.hub.DarkGardenDecor0255.register(bus);
    com.chaogarden.hub.LobbyApproachDecor0255.register(bus);
    com.chaogarden.kindergarten.KindergartenDecor025.register(bus);
    com.chaogarden.kindergarten.KindergartenHallThemes0252.register(bus);
    com.chaogarden.genes.GeneContent.register(bus);
    com.chaogarden.fruit.FruitContent.register(bus);
    com.chaogarden.market.MarketContent.register(bus);
    com.chaogarden.market.MarketNetwork.register(bus);
    com.chaogarden.cwe.CweContent.register();
    com.chaogarden.care.DoctorContent.register(bus);
    com.chaogarden.kindergarten.KindergartenContent.register(bus);
    com.chaogarden.activities.ActivityContent.register(bus);
    com.chaogarden.companion.CompanionContent.register(bus);
    com.chaogarden.music.GardenMusicContent.register(bus);
    bus.addListener(
        (EntityAttributeCreationEvent e) -> e.put(CHAO.get(), ChaoEntity.attributes().build()));
    container.registerConfig(ModConfig.Type.SERVER, GardenConfig.SPEC);
    GardenCommands.register();
  }
}
