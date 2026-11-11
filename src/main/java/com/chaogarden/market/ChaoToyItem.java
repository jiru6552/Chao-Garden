package com.chaogarden.market;

import com.chaogarden.entity.ChaoEntity;
import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

public final class ChaoToyItem extends BlockItem {
  public final String toy;

  public ChaoToyItem(String toy, Block block) {
    super(block, new Item.Properties().stacksTo(16));
    this.toy = toy;
  }

  @Override
  public InteractionResult interactLivingEntity(ItemStack stack, Player player,
                                               LivingEntity target, InteractionHand hand) {
    return target instanceof ChaoEntity chao
        ? MarketContent.interact(chao, player, hand) : InteractionResult.PASS;
  }

  @Override
  public void appendHoverText(ItemStack stack, TooltipContext context,
                              List<Component> lines, TooltipFlag flag) {
    lines.add(Component.literal(switch (toy) {
      case "chao_ball" -> "Play chase and bounce with your Chao";
      case "chao_rattle" -> "A cheerful rattle; especially fun for children";
      case "chao_trumpet" -> "A bright brass trumpet for a Chao music session";
      case "chao_drum" -> "A little drum for a Chao rhythm session";
      case "chao_jack_in_box" -> "A springy surprise for your Chao";
      case "chao_radio" -> "A musical radio to entertain your Chao";
      case "chao_tv" -> "A small television for a Chao play session";
      case "chao_rocking_horse" -> "A garden rocking horse that Chao can ride";
      case "chao_coffin" -> "A little coffin for a Chao's pretend nap";
      case "chao_lifesaver" -> "Place in shallow water beside a clear shore; a Chao can float in it";
      case "chao_rubber_duck" -> "A floating rubber duck to splash with from the shore";
      case "chao_piano", "chao_organ" -> "A garden keyboard for a voluntary music session";
      case "chao_shovel" -> "Give to a Chao to teach planting (50 Run points); place on sand for sandcastles";
      case "chao_bubble_wand" -> "A little bubble wand for a Chao play session";
      case "chao_triangle", "chao_guitar", "chao_accordion", "chao_bell", "chao_castanets",
          "chao_cymbals", "chao_flute", "chao_tambourine" -> "A classroom instrument; Chao must learn it before performing";
      default -> "Play a rhythm and let your Chao dance";
    }));
  }
}
