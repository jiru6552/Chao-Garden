package com.chaogarden.market;

import com.chaogarden.ChaoGarden;
import com.chaogarden.entity.ChaoEntity;
import com.chaogarden.toys.ToyContent;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.DeferredSpawnEggItem;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

@EventBusSubscriber(modid = ChaoGarden.MODID)
public final class MarketContent {
  private static final DeferredRegister<EntityType<?>> ENTITIES =
      DeferredRegister.create(Registries.ENTITY_TYPE, ChaoGarden.MODID);
  public static final DeferredHolder<EntityType<?>, EntityType<BlackMarketTrader>> TRADER =
      ENTITIES.register("black_market_trader", () -> EntityType.Builder
          .of(BlackMarketTrader::new, MobCategory.CREATURE).sized(.65f, 1.1f)
          .eyeHeight(.78f).clientTrackingRange(10).build("black_market_trader"));
  private static boolean bootstrapped;
  private MarketContent() {}

  public static void bootstrap() {
    if (bootstrapped) return;
    bootstrapped = true;
    // Retain this removed ID as an inert item so old containers keep their data.
    ChaoGarden.add("chao_coffin",com.chaogarden.toys.RetiredCoffinItem::new);
    for (String toy : ToyContent.ids())
      ChaoGarden.add(toy, () -> new ChaoToyItem(toy, ToyContent.block(toy)));
    ChaoGarden.add("chao_watering_can",com.chaogarden.fruit.WateringCanItem::new);
    ChaoGarden.add("eggshell_hat", () -> new ChaoHatItem("eggshell_hat"));
    ChaoGarden.add("watermelon_hat",LegacyMelonHatItem::new);
    for (String hat : MarketStock.HATS) ChaoGarden.add(hat, () -> new ChaoHatItem(hat));
    ChaoGarden.add("black_market_trader_spawn_egg", () -> new DeferredSpawnEggItem(
        TRADER, 0x526a58, 0xdec070, new Item.Properties()));
  }

  public static void register(IEventBus bus) {
    bootstrap();
    ENTITIES.register(bus);
    ToyContent.register(bus);
    bus.addListener((EntityAttributeCreationEvent event) ->
        event.put(TRADER.get(), BlackMarketTrader.attributes().build()));
  }

  /** A toy is a physical garden object; clicking a Chao never forces a play session. */
  public static InteractionResult interact(ChaoEntity chao, net.minecraft.world.entity.player.Player player,
                                            InteractionHand hand) {
    if(com.chaogarden.fruit.GardenTools.isTool(player.getItemInHand(hand)))return chao.offerItem(player,hand);
    if (!(player.getItemInHand(hand).getItem() instanceof ChaoToyItem)) return InteractionResult.PASS;
    if (!chao.level().isClientSide)
      player.displayClientMessage(Component.literal("Place this toy in the garden. Chao choose when to play."), true);
    return InteractionResult.sidedSuccess(chao.level().isClientSide);
  }

  @SubscribeEvent
  public static void visit(LevelTickEvent.Post event) {
    if (!(event.getLevel() instanceof ServerLevel level) || !level.dimension().equals(Level.OVERWORLD)
        || level.getGameTime() < 1200 || level.getGameTime() % 1200 != 0
        || !level.getGameRules().getBoolean(GameRules.RULE_DO_TRADER_SPAWNING)
        || !level.getGameRules().getBoolean(GameRules.RULE_DOMOBSPAWNING)) return;
    var players = level.players().stream().filter(p -> p.isAlive() && !p.isSpectator()).toList();
    if (players.isEmpty()) return;
    long now = level.getGameTime(), day = now / 24000;
    var data = MarketVisitData.get(level);
    if (!data.mayAttempt(day, now)) return;
    data.attempted(day);
    if (level.random.nextInt(100) >= 60) return;
    var player = players.get(level.random.nextInt(players.size()));
    if (!level.getEntitiesOfClass(BlackMarketTrader.class,
        new AABB(player.blockPosition()).inflate(64)).isEmpty()) return;
    for (int attempt = 0; attempt < 12; attempt++) {
      double angle = level.random.nextDouble() * Math.PI * 2;
      double radius = 18 + level.random.nextInt(19);
      int x = player.getBlockX() + (int)Math.round(Math.cos(angle)*radius);
      int z = player.getBlockZ() + (int)Math.round(Math.sin(angle)*radius);
      BlockPos column = new BlockPos(x, 0, z);
      if (!level.hasChunkAt(column)) continue;
      BlockPos pos = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, column);
      if (!level.getWorldBorder().isWithinBounds(pos) || !level.getBlockState(pos.below()).isFaceSturdy(level, pos.below(), Direction.UP)
          || !level.getBlockState(pos).isAir() || !level.getBlockState(pos.above()).isAir()) continue;
      var trader = TRADER.get().create(level);
      if (trader == null) return;
      trader.moveTo(pos.getX()+.5, pos.getY(), pos.getZ()+.5, level.random.nextFloat()*360, 0);
      if (!level.noCollision(trader)) continue;
      trader.beginVisit(now);
      trader.setWanderTarget(player.blockPosition());
      trader.restrictTo(player.blockPosition(), 24);
      if (level.addFreshEntity(trader)) data.occupied(trader.visitExpiry());
      return;
    }
  }

}
