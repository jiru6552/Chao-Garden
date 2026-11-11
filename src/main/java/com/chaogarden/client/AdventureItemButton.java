package com.chaogarden.client;

import java.util.function.Supplier;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

/** A real inventory stack selector, with native keyboard focus and narration. */
final class AdventureItemButton extends AbstractButton {
  private final Supplier<ItemStack> item;
  private final Runnable press;
  AdventureItemButton(int x,int y,Supplier<ItemStack> item,Runnable press) {
    super(x,y,26,24,Component.literal("Equip an inventory accessory"));this.item=item;this.press=press;
  }
  @Override public void onPress() {press.run();}
  @Override protected void renderWidget(GuiGraphics g,int mouseX,int mouseY,float partial) {
    var stack=item.get();setMessage(stack.isEmpty()?Component.literal("Empty inventory choice"):stack.getHoverName());
    AdventureScreen.round(g,getX(),getY(),width,height,5,!active?0xffb9d7e3:isHoveredOrFocused()?0xffffdf85:0xffd8edf4);
    if(!stack.isEmpty())g.renderItem(stack,getX()+5,getY()+4);
    if(isFocused())g.fill(getX()+4,getY()+height-3,getX()+width-4,getY()+height-2,0xff2175ae);
  }
  @Override protected void updateWidgetNarration(NarrationElementOutput output) {defaultButtonNarrationText(output);}
}
