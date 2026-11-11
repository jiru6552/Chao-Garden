package com.chaogarden.item;

import java.util.List;
import com.chaogarden.genes.GeneContent;
import com.chaogarden.core.ChaoState;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

public class CareItem extends Item {
  public enum Kind {
    FRUIT,
    DRIVE,
    ANIMAL
  }

  public final Kind kind;
  public final String effect;
  public final int index;

  public CareItem(Kind kind, String effect, int index) {
    super(new Item.Properties());
    this.kind = kind;
    this.effect = effect;
    this.index = index;
  }

  @Override
  public void appendHoverText(
      ItemStack stack, TooltipContext context, List<Component> tips, TooltipFlag flag) {
    if (kind == Kind.ANIMAL) {
      GeneContent.appendBenefits(index, tips);

    } else if (kind == Kind.DRIVE) {
      tips.add(Component.literal(ChaoState.STATS[index] + " +24% training progress")
          .withStyle(ChatFormatting.AQUA));
      tips.add(Component.literal("+1 happiness, +40% luck and +20% intelligence training; shifts evolution tendency.")
          .withStyle(ChatFormatting.DARK_AQUA));
      tips.add(Component.literal("Trains without changing animal parts or lowering other skills.")
          .withStyle(ChatFormatting.GRAY));

    } else {
      boolean stronger=com.chaogarden.GardenConfig.SPEC.isLoaded()
          &&com.chaogarden.GardenConfig.CWE_FRUIT_REBALANCE.get();
      tips.add(Component.literal(com.chaogarden.fruit.FruitEffects.benefit(effect,stronger)).withStyle(ChatFormatting.AQUA));

    }
  }
}
