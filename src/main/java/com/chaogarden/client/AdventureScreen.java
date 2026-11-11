package com.chaogarden.client;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

/** A fixed artboard with consistent native widget and mouse scaling at any GUI size. */
abstract class AdventureScreen extends Screen {
  static final int DESIGN_W = 600, DESIGN_H = 360;
  static final int INK = 0xff204777, MUTED = 0xff64849f, BLUE = 0xff248dc8, ORANGE = 0xffffbc40;
  protected float canvasScale = 1;
  protected int offsetX, offsetY;
  private static final ResourceLocation SYMBOLS =
      ResourceLocation.fromNamespaceAndPath("chao_garden", "textures/gui/adventure_symbols.png");

  protected AdventureScreen(Component title) {
    super(title);
  }

  @Override
  protected final void init() {
    canvasScale = Math.min(1f, Math.min((width - 12f) / DESIGN_W, (height - 12f) / DESIGN_H));
    offsetX = Math.round((width - DESIGN_W * canvasScale) / 2f);
    offsetY = Math.round((height - DESIGN_H * canvasScale) / 2f);
    initAdventure();
  }

  protected abstract void initAdventure();

  protected int localX(double mouse) {
    return (int) ((mouse - offsetX) / canvasScale);
  }

  protected int localY(double mouse) {
    return (int) ((mouse - offsetY) / canvasScale);
  }

  protected int screenX(float x) {
    return offsetX + Math.round(x * canvasScale);
  }

  protected int screenY(float y) {
    return offsetY + Math.round(y * canvasScale);
  }

  protected void begin(GuiGraphics g, int mouseX, int mouseY, float partial, String theme) {
    renderBackground(g, mouseX, mouseY, partial);
    g.fillGradient(0, 0, width, height, 0x88123e7c, 0x9960baca);
    g.pose().pushPose();
    g.pose().translate(offsetX, offsetY, 0);
    g.pose().scale(canvasScale, canvasScale, 1);
    ResourceLocation panel =
        ResourceLocation.fromNamespaceAndPath(
            "chao_garden", "textures/gui/adventure_" + theme + ".png");
    // These textures have antialiased translucent edges. The plain blit overload
    // does not enable blending; without it even near-transparent fringe texels
    // become opaque specks along the rounded panel and circular symbol edges.
    RenderSystem.enableBlend();
    RenderSystem.defaultBlendFunc();
    g.blit(panel, 0, 0, 0f, 0f, DESIGN_W, DESIGN_H, DESIGN_W, DESIGN_H);
    RenderSystem.disableBlend();
  }

  protected void end(GuiGraphics g, int mouseX, int mouseY, float partial) {
    int mx = localX(mouseX), my = localY(mouseY);
    for (var widget : renderables) widget.render(g, mx, my, partial);
    g.pose().popPose();
  }

  static void round(GuiGraphics g, int x, int y, int w, int h, int radius, int color) {
    for (int yy = 0; yy < h; yy++) {
      int inset = 0;
      if (yy < radius)
        inset = radius - (int) Math.sqrt(radius * radius - (radius - yy - 1) * (radius - yy - 1));
      else if (yy >= h - radius)
        inset =
            radius - (int) Math.sqrt(radius * radius - (yy - (h - radius)) * (yy - (h - radius)));
      g.fill(x + inset, y + yy, x + w - inset, y + yy + 1, color);
    }
  }

  void text(GuiGraphics g, String s, float x, float y, float scale, int color) {
    g.pose().pushPose();
    g.pose().translate(x, y, 0);
    g.pose().scale(scale, scale, 1);
    g.drawString(font, s, 0, 0, color, false);
    g.pose().popPose();
  }

  void text(GuiGraphics g, String s, int x, int y, int color) {
    g.drawString(font, s, x, y, color, false);
  }

  void right(GuiGraphics g, String s, int x, int y, int color) {
    text(g, s, x - font.width(s), y, color);
  }

  void centered(GuiGraphics g, String s, int x, int y, int color) {
    text(g, s, x - font.width(s) / 2, y, color);
  }

  void logo(GuiGraphics g, String subtitle) {
    text(g, "CHAO", 49, 11, 1.8f, 0xff123d6b);
    text(g, subtitle, 50, 34, .78f, 0xff234f79);
  }

  void symbol(GuiGraphics g, int index, int x, int y, int size) {
    RenderSystem.enableBlend();
    RenderSystem.defaultBlendFunc();
    g.blit(SYMBOLS, x, y, size, size, index * 32f, 0, 32, 32, 320, 32);
    RenderSystem.disableBlend();
  }

  void segmented(GuiGraphics g, int x, int y, int w, int h, float amount, int color, int count) {
    amount = Math.max(0, Math.min(1, amount));
    for (int i = 0; i < count; i++) {
      int a = x + (w * i) / count, b = x + (w * (i + 1)) / count - 1;
      round(g, a, y, b - a, h, 2, 0xffd4e7ec);
      int n = Math.min(b - a, Math.max(0, (int) (w * amount) - (a - x)));
      if (n > 0) round(g, a, y, n, h, 2, color);
    }
  }

  @Override
  public boolean mouseClicked(double x, double y, int button) {
    return super.mouseClicked(localX(x), localY(y), button);
  }

  @Override
  public boolean mouseReleased(double x, double y, int button) {
    return super.mouseReleased(localX(x), localY(y), button);
  }

  @Override
  public boolean mouseDragged(double x, double y, int button, double dx, double dy) {
    return super.mouseDragged(localX(x), localY(y), button, dx / canvasScale, dy / canvasScale);
  }

  @Override
  public void mouseMoved(double x, double y) {
    super.mouseMoved(localX(x), localY(y));
  }

  @Override
  public boolean isPauseScreen() {
    return false;
  }
}
