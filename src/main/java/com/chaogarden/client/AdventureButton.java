package com.chaogarden.client;

import java.util.function.BooleanSupplier;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;

/** Rounded Adventure-style native button, including keyboard focus and narration. */
final class AdventureButton extends AbstractButton {
  private final Runnable action;
  private final BooleanSupplier selected;
  private final int accent;
  private final boolean chapter;

  AdventureButton(
      int x,
      int y,
      int w,
      int h,
      String label,
      int accent,
      BooleanSupplier selected,
      Runnable action) {
    this(x, y, w, h, label, accent, selected, action, false);
  }

  AdventureButton(
      int x,
      int y,
      int w,
      int h,
      String label,
      int accent,
      BooleanSupplier selected,
      Runnable action,
      boolean chapter) {
    super(x, y, w, h, Component.literal(label));
    this.action = action;
    this.selected = selected;
    this.accent = accent;
    this.chapter = chapter;
  }

  @Override
  public void onPress() {
    action.run();
  }

  @Override
  protected void renderWidget(GuiGraphics g, int mouseX, int mouseY, float partial) {
    boolean on = selected.getAsBoolean(), hover = isHoveredOrFocused();
    int fill = on ? accent : hover ? 0xffe5fbff : chapter ? 0x00ffffff : 0xff1675bd;
    int ink = on ? 0xff123b66 : hover ? 0xff174d7a : chapter ? 0xff426682 : 0xffe9fcff;
    if (!active) {
      fill = 0xff3a83ba;
      ink = 0xff94bdd9;
    }
    if (!chapter) {
      AdventureScreen.round(g, getX(), getY() + 2, width, height, 9, 0x60214177);
      AdventureScreen.round(g, getX(), getY(), width, height, 9, fill);
    } else if (on || hover) AdventureScreen.round(g, getX(), getY(), width, height, 5, fill);
    if (hover)
      AdventureScreen.round(
          g,
          getX() + 2,
          getY() + 1,
          width - 4,
          height - 2,
          chapter ? 4 : 7,
          on ? 0x26ffffff : 0x20b8fcff);
    var font = Minecraft.getInstance().font;
    String value = font.plainSubstrByWidth(getMessage().getString(), width - 12);
    int tx = chapter ? getX() + 6 : getX() + (width - font.width(value)) / 2;
    g.drawString(font, value, tx, getY() + (height - 8) / 2, ink, false);
    if (isFocused())
      g.fill(
          getX() + 7,
          getY() + height - 3,
          getX() + width - 7,
          getY() + height - 2,
          on ? 0xff1f5883 : 0xffb8f8ff);
  }

  @Override
  protected void updateWidgetNarration(NarrationElementOutput output) {
    defaultButtonNarrationText(output);
  }
}
