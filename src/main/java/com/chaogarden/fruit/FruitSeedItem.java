package com.chaogarden.fruit;

import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.block.Block;

public final class FruitSeedItem extends BlockItem {
  public final String fruit;
  public FruitSeedItem(Block sapling, String fruit) {
    super(sapling, new Item.Properties());
    this.fruit = fruit;
  }
  @Override public net.minecraft.world.InteractionResult useOn(net.minecraft.world.item.context.UseOnContext context) {
    if (FruitCatalog.isMushroom(fruit)) return net.minecraft.world.InteractionResult.FAIL;
    return super.useOn(context);
  }
  @Override public net.minecraft.world.InteractionResult place(net.minecraft.world.item.context.BlockPlaceContext context) {
    if(context.getLevel() instanceof net.minecraft.server.level.ServerLevel server
        && !GardenTreeData.get(server).canPlant(server,context.getClickedPos())) {
      if(context.getPlayer()!=null)context.getPlayer().displayClientMessage(Component.literal(
          "Leave space between planted trees; each garden has four planting slots."),true);
      return net.minecraft.world.InteractionResult.FAIL;
    }
    var result=super.place(context);
    if(result.consumesAction()&&context.getLevel() instanceof net.minecraft.server.level.ServerLevel server
        &&server.getBlockState(context.getClickedPos()).is(getBlock()))
      GardenTreeData.get(server).planted(context.getClickedPos(),fruit);
    return result;
  }
  @Override public void appendHoverText(ItemStack stack, TooltipContext context,
      List<Component> tips, TooltipFlag flag) {
    if (FruitCatalog.isMushroom(fruit)) {
      tips.add(Component.literal("Retired mushroom seed; kept only for old saved inventories."));
      return;
    }
    tips.add(Component.literal("Plant on dirt or farmland in a bright place"));
    tips.add(Component.literal("Needs a 5×5 canopy area and seven blocks of height"));
    tips.add(Component.literal("Or give to a Chao with a Shovel and 50 Run points"));
    tips.add(Component.literal("Four planted trees per garden"));
  }
}
