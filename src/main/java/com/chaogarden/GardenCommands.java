package com.chaogarden;

import com.chaogarden.entity.ChaoEntity;
import com.chaogarden.item.ChaoEggItem;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

public final class GardenCommands {
  private static final java.util.List<String> SOURCE_RIGS = java.util.List.of(
      "baby", "swim", "fly", "run", "power", "hero", "hero_fly", "hero_power",
      "dark", "dark_run", "dark_swim", "golden", "baghead", "dad", "dragon",
      "ender", "tails", "knuckles", "emerald", "creeper");
  public static void register() {
    NeoForge.EVENT_BUS.addListener(GardenCommands::commands);
  }

  private static void commands(RegisterCommandsEvent event) {
    event
        .getDispatcher()
        .register(
            Commands.literal("chao")
                .then(Commands.literal("appearance")
                    .requires(s -> s.hasPermission(2))
                    .then(Commands.literal("official")
                        .executes(c -> appearance(c.getSource(), true, ""))
                        .then(Commands.argument("rig", StringArgumentType.word())
                            .suggests((c, builder) -> SharedSuggestionProvider.suggest(SOURCE_RIGS, builder))
                            .executes(c -> appearance(c.getSource(), true,
                                StringArgumentType.getString(c, "rig")))))
                    .then(Commands.literal("garden")
                        .executes(c -> appearance(c.getSource(), false, ""))))
                .then(
                    Commands.literal("inspect")
                        .executes(
                            c -> {
                              var chao = nearest(c.getSource());
                              if (chao == null) return 0;
                              var s = chao.state();
                              c.getSource()
                                  .sendSuccess(
                                      () ->
                                          Component.literal(
                                              chao.getName().getString()
                                                  + ": "
                                                  + s.stageName()
                                                  + ", "
                                                  + s.alignmentName()
                                                  + " "
                                                  + s.typeName()
                                                  + ", "
                                                  + s.colorName()
                                                  + (ChaoVariants.NORMAL.equals(s.variant) ? ""
                                                      : " • " + ChaoVariants.name(s.variant)
                                                          + (ChaoVariants.available(s.variant)
                                                              ? "" : " (dormant)"))),
                                      false);
                              for (int i = 0; i < 5; i++) {
                                final int stat = i;
                                c.getSource()
                                    .sendSuccess(
                                        () ->
                                            Component.literal(
                                                s.statSummary(stat)
                                                    + " • "
                                                    + s.progress[stat]
                                                    + "/100"),
                                        false);
                              }
                              c.getSource()
                                  .sendSuccess(
                                      () ->
                                          Component.literal(
                                              "Happiness "
                                                  + s.happiness
                                                  + " • Hunger "
                                                  + s.hunger
                                                  + " • Energy "
                                                  + s.energy
                                                  + " • Bond "
                                                  + s.bond
                                                  + " • Rebirths "
                                                  + s.reincarnations
                                                  + " • Animals "
                                                  + Integer.bitCount(s.animalMask)
                                                  + "/21"),
                                      false);
                              c.getSource()
                                  .sendSuccess(
                                      () ->
                                          Component.literal(
                                              "Active age: "
                                                  + (s.ageTicks / 1200)
                                                  + " minutes • Intelligence "
                                                  + s.intelligence
                                                  + " • Luck "
                                                  + s.luck
                                                  + " • Favorite: "
                                                  + new String[] {"Round", "Square", "Triangle"}
                                                      [s.favoriteFruit]
                                                  + " Fruit"),
                                      false);
                              return 1;
                            }))
                .then(
                    Commands.literal("variants")
                        .executes(c -> {
                          var enabled = ChaoVariants.all().stream()
                              .filter(ChaoVariants::obtainable).toList();
                          c.getSource().sendSuccess(() -> Component.literal(
                              "Available Chao appearances (" + enabled.size() + "): "
                                  + enabled.stream().map(v -> v.name() + " [" + ChaoVariants.commandId(v.id()) + "]")
                                      .collect(java.util.stream.Collectors.joining(", "))), false);
                          return enabled.size();
                        }))
                .then(
                    Commands.literal("variantegg")
                        .requires(s -> s.hasPermission(2))
                        .then(Commands.argument("id", StringArgumentType.word())
                            .suggests((c, builder) -> SharedSuggestionProvider.suggest(
                                ChaoVariants.all().stream().filter(ChaoVariants::obtainable)
                                    .map(v -> ChaoVariants.commandId(v.id())), builder))
                            .executes(c -> {
                              String id = ChaoVariants.canonicalId(StringArgumentType.getString(c, "id"));
                              if (ChaoVariants.find(id).isEmpty() || !ChaoVariants.obtainable(id) || !ChaoVariants.available(id)) {
                                c.getSource().sendFailure(Component.literal(
                                    ChaoVariants.lockedMessage(id)));
                                return 0;
                              }
                              var player = c.getSource().getPlayerOrException();
                              var stack = new ItemStack(ChaoGarden.item(ChaoVariants.itemId(id)));
                              if (!player.getInventory().add(stack)) player.drop(stack, false);
                              c.getSource().sendSuccess(() -> Component.literal(
                                  "Supplied " + ChaoVariants.name(id) + " Chao Egg."), false);
                              return 1;
                            })))
                .then(
                    Commands.literal("testkit")
                        .requires(s -> s.hasPermission(2))
                        .executes(
                            c -> {
                              var player = c.getSource().getPlayerOrException();
                              for (String id : ChaoGarden.CONTENT.keySet()) {
                                if (!ChaoGarden.visibleInCreative(ChaoGarden.item(id))) continue;
                                if (ChaoGarden.item(id) instanceof ChaoEggItem egg
                                    && (!ChaoVariants.obtainable(egg.variant)||!ChaoVariants.available(egg.variant))) continue;
                                if (id.endsWith("_egg") && !id.equals("normal_egg")) continue;
                                var stack =
                                    new ItemStack(
                                        ChaoGarden.item(id),
                                        id.endsWith("fruit")
                                                || id.endsWith("drive")
                                                || id.endsWith("animal")
                                            ? 16
                                            : 1);
                                if (!player.getInventory().add(stack)) player.drop(stack, false);
                              }
                              c.getSource()
                                  .sendSuccess(
                                      () ->
                                          Component.literal(
                                              "Garden test kit supplied. /chao mature evolves the"
                                                  + " nearest Chao."),
                                      false);
                              return 1;
                            }))
                .then(
                    Commands.literal("mature")
                        .requires(s -> s.hasPermission(2))
                        .executes(
                            c -> {
                              var chao = nearest(c.getSource());
                              if (chao == null) return 0;
                              if (!chao.state().adult)
                                chao.state().ageTicks = GardenConfig.CHILD_TICKS.get();
                              chao.sync();
                              return 1;
                            }))
                .then(
                    Commands.literal("rebirth")
                        .requires(s -> s.hasPermission(2))
                        .executes(
                            c -> {
                              var chao = nearest(c.getSource());
                              if (chao == null) return 0;
                              chao.state().happiness = 100;
                              chao.state().adult = true;
                              chao.state().adultAgeTicks = GardenConfig.ADULT_TICKS.get();
                              chao.sync();
                              return 1;
                            }))
                .then(
                    Commands.literal("clearparts")
                        .requires(s -> s.hasPermission(2))
                        .executes(
                            c -> {
                              var chao = nearest(c.getSource());
                              if (chao == null) return 0;
                              chao.state().giveAnimal(19);
                              chao.sync();
                              return 1;
                            })));
  }

  private static int appearance(net.minecraft.commands.CommandSourceStack source,
                                boolean official, String rig) {
    if (!rig.isEmpty() && !SOURCE_RIGS.contains(rig)) {
      source.sendFailure(Component.literal("Unknown source rig. Use the command suggestions."));
      return 0;
    }
    var chao = nearest(source);
    if (chao == null) return 0;
    chao.setSourceReferenceKey(rig);
    chao.setOfficialComparison(official);
    source.sendSuccess(() -> Component.literal(chao.getName().getString() + ": "
        + (official ? "original official model, texture and clips"
            + (rig.isEmpty() ? " (form selected from its state)" : " [" + rig + "]")
          : "garden appearance and gameplay animations")
        + ". Saved care and genetics are preserved."), false);
    return 1;
  }

  private static ChaoEntity nearest(net.minecraft.commands.CommandSourceStack source) {
    var found =
        source
            .getLevel()
            .getEntitiesOfClass(
                ChaoEntity.class,
                new net.minecraft.world.phys.AABB(source.getPosition(), source.getPosition())
                    .inflate(12));
    ChaoEntity result = null;
    double best = Double.MAX_VALUE;
    for (var c : found) {
      if(c instanceof com.chaogarden.kindergarten.KindergartenPupil0257)continue;
      double d = c.position().distanceToSqr(source.getPosition());
      if (d < best) {
        best = d;
        result = c;
      }
    }
    if (result == null) source.sendFailure(Component.literal("No Chao within 12 blocks."));
    return result;
  }
}
