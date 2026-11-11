package com.chaogarden.client;

import com.chaogarden.ChaoGarden;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.BookViewScreen;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

/** Restyles only this mod's field guide; ordinary written books keep their own reader. */
@EventBusSubscriber(modid = "chao_garden", value = Dist.CLIENT)
public final class ChaoGuideInteractions {
  private static InteractionHand pendingHand;
  private static long pendingUntil;

  private static boolean guide(ItemStack stack) {
    return stack.is(ChaoGarden.item("garden_guide"));
  }

  @SubscribeEvent
  public static void use(PlayerInteractEvent.RightClickItem event) {
    if (event.getLevel().isClientSide && guide(event.getItemStack())) {
      pendingHand = event.getHand();
      pendingUntil = event.getLevel().getGameTime() + 40;
    }
  }

  @SubscribeEvent
  public static void open(ScreenEvent.Opening event) {
    Minecraft mc = Minecraft.getInstance();
    if (!(event.getNewScreen() instanceof BookViewScreen) || mc.player == null || mc.level == null)
      return;
    ItemStack selected = ItemStack.EMPTY;
    if (pendingHand != null && mc.level.getGameTime() <= pendingUntil) {
      var pending = mc.player.getItemInHand(pendingHand);
      if (guide(pending)) selected = pending;
    }
    // Also covers server-opened guides, without mistaking a different held book for this one.
    if (selected.isEmpty()) {
      var main = mc.player.getMainHandItem();
      var off = mc.player.getOffhandItem();
      if (guide(main)
          && off.get(DataComponents.WRITTEN_BOOK_CONTENT) == null
          && off.get(DataComponents.WRITABLE_BOOK_CONTENT) == null) selected = main;
      else if (guide(off)
          && main.get(DataComponents.WRITTEN_BOOK_CONTENT) == null
          && main.get(DataComponents.WRITABLE_BOOK_CONTENT) == null) selected = off;
    }
    pendingHand = null;
    var content = selected.get(DataComponents.WRITTEN_BOOK_CONTENT);
    if (content != null && !content.pages().isEmpty())
      event.setNewScreen(new AdventureGuideScreen(content));
  }
}
