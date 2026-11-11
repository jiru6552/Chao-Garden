package com.chaogarden.client.activities;

import com.chaogarden.activities.ActivityNetwork;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;

/** Shared, server-backed garden roster and contest lobby. */
public final class ActivityScreen extends Screen {
  private static final int W = 420, H = 304, PAGE = 6;
  private static final int INK = 0xff204777, MUTED = 0xff64849f;
  private final BlockPos position;
  private JsonObject snapshot;
  private final List<JsonObject> entries = new ArrayList<>();
  private int left, top, page, age;
  private float canvasScale = 1;
  private int offsetX, offsetY;
  private boolean awaiting;
  private String search = "";
  private net.minecraft.client.gui.components.EditBox filter;

  private ActivityScreen(BlockPos position, JsonObject snapshot) {
    super(Component.literal("Chao activities"));
    this.position = position.immutable();
    accept(snapshot);
  }

  public static void receive(BlockPos position, String json) {
    JsonObject data;
    try {
      data = JsonParser.parseString(json).getAsJsonObject();
    } catch (RuntimeException invalid) {
      return;
    }
    if(text(data,"mode","").equals("garden")){GardenScreen.receive(position,data);return;}
    Minecraft mc = Minecraft.getInstance();
    if (mc.screen instanceof ActivityScreen current && current.position.equals(position)) {
      current.accept(data);
      current.refreshWidgets();
    } else if (mc.screen == null && flag(data, "open")) {
      mc.setScreen(new ActivityScreen(position, data));
    }
  }

  private void accept(JsonObject data) {
    snapshot = data;
    awaiting = false;
    entries.clear();
    JsonArray list =
        data.has("entries") && data.get("entries").isJsonArray()
            ? data.getAsJsonArray("entries")
            : new JsonArray();
    for (var item : list) if (item.isJsonObject()) entries.add(item.getAsJsonObject());
  }

  private static String text(JsonObject data, String key, String fallback) {
    try {
      return data.has(key) ? data.get(key).getAsString() : fallback;
    } catch (RuntimeException ignored) {
      return fallback;
    }
  }

  private static boolean flag(JsonObject data, String key) {
    try {
      return data.has(key) && data.get(key).getAsBoolean();
    } catch (RuntimeException ignored) {
      return false;
    }
  }

  private List<JsonObject> shown() {
    String query = search.toLowerCase(java.util.Locale.ROOT).strip();
    return entries.stream()
        .filter(
            e ->
                (text(e, "name", "Chao") + " " + text(e, "garden", "Garden"))
                    .toLowerCase(java.util.Locale.ROOT)
                    .contains(query))
        .toList();
  }

  @Override
  protected void init() {
    canvasScale = Math.min(1f, Math.min((width - 12f) / W, (height - 12f) / H));
    offsetX = Math.round((width - W * canvasScale) / 2);
    offsetY = Math.round((height - H * canvasScale) / 2);
    left = top = 0;
    filter =
        new net.minecraft.client.gui.components.EditBox(
            font, left + 12, top + 48, 236, 18, Component.literal("Find a Chao or garden"));
    filter.setMaxLength(48);
    filter.setValue(search);
    filter.setHint(Component.literal("Find a Chao or garden"));
    filter.setResponder(
        value -> {
          if (value.equals(search)) return;
          search = value;
          page = 0;
          refreshWidgets();
        });
    addRenderableWidget(filter);
    addRenderableWidget(
        Button.builder(Component.literal("Refresh"), b -> send("refresh", null))
            .bounds(left + W - 92, top + 47, 80, 20)
            .build());

    List<JsonObject> visible = shown();
    int pages = Math.max(1, (visible.size() + PAGE - 1) / PAGE);
    page = Math.max(0, Math.min(page, pages - 1));
    for (int row = 0; row < PAGE; row++) {
      int index = page * PAGE + row;
      if (index >= visible.size()) break;
      JsonObject entry = visible.get(index);
      boolean mine = flag(entry, "mine"), selected = flag(entry, "selected");
      boolean garden = text(snapshot, "mode", "garden").equals("garden");
      String label =
          selected
              ? (mine ? garden ? "Unassign" : "Remove" : "Joined")
              : mine ? garden ? "Move here" : "Enter" : "Other keeper";
      Button choose =
          Button.builder(
                  Component.literal(label),
                  b -> {
                    try {
                      send("toggle", UUID.fromString(text(entry, "id", "")));
                    } catch (IllegalArgumentException ignored) {
                    }
                  })
              .bounds(left + W - 80, top + 78 + row * 26, 66, 20)
              .build();
      choose.active =
          !awaiting && mine && (selected || flag(entry, "available")) && !flag(snapshot, "running");
      addRenderableWidget(choose);
      Button care=Button.builder(Component.literal("Care"),b->{
        try{send("care",UUID.fromString(text(entry,"id","")));}catch(IllegalArgumentException ignored){}
      }).bounds(left+W-126,top+78+row*26,42,20).build();
      care.active=!awaiting&&flag(entry,"careAvailable");addRenderableWidget(care);
    }
    Button previous =
        Button.builder(
                Component.literal("<"),
                b -> {
                  page--;
                  refreshWidgets();
                })
            .bounds(left + 12, top + 239, 26, 20)
            .build();
    previous.active = page > 0;
    addRenderableWidget(previous);
    Button next =
        Button.builder(
                Component.literal(">"),
                b -> {
                  page++;
                  refreshWidgets();
                })
            .bounds(left + 84, top + 239, 26, 20)
            .build();
    next.active = page + 1 < pages;
    addRenderableWidget(next);
    if (!text(snapshot, "mode", "garden").equals("garden")) {
      Button start =
          Button.builder(Component.literal("Start activity"), b -> send("start", null))
              .bounds(left + 154, top + 238, 118, 22)
              .build();
      start.active = !awaiting && flag(snapshot, "canStart");
      addRenderableWidget(start);
      Button cancel =
          Button.builder(Component.literal("Cancel"), b -> send("cancel", null))
              .bounds(left + 280, top + 238, 80, 22)
              .build();
      cancel.active = !awaiting && flag(snapshot, "canCancel");
      addRenderableWidget(cancel);
    }
    addRenderableWidget(
        Button.builder(Component.literal("Done"), b -> onClose())
            .bounds(left + W - 68, top + H - 29, 56, 19)
            .build());
  }

  private void refreshWidgets() {
    boolean editing = filter != null && filter.isFocused();
    int cursor = filter == null ? search.length() : filter.getCursorPosition();
    rebuildWidgets();
    if (editing) {
      setFocused(filter);
      filter.setFocused(true);
      filter.setCursorPosition(Math.min(cursor, search.length()));
    }
  }

  private void send(String action, UUID chao) {
    if (minecraft == null || minecraft.getConnection() == null) return;
    awaiting = true;
    ActivityNetwork.request(position, action, chao);
    refreshWidgets();
  }

  @Override
  public void tick() {
    if (++age % 40 == 0) send("refresh", null);
    if (minecraft == null
        || minecraft.player == null
        || minecraft.player.distanceToSqr(
                position.getX() + .5, position.getY() + .5, position.getZ() + .5)
            > 144) onClose();
  }

  @Override
  public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
    renderBackground(g, mouseX, mouseY, partialTick);
    g.fillGradient(0, 0, width, height, 0x70123e7c, 0x8860baca);
    g.pose().pushPose();
    g.pose().translate(offsetX, offsetY, 0);
    g.pose().scale(canvasScale, canvasScale, 1);
    g.fill(left - 2, top - 2, left + W + 2, top + H + 2, 0xff2479b3);
    g.fill(left, top, left + W, top + H, 0xffeffafd);
    g.fill(left, top, left + W, top + 39, 0xffd7f0f4);
    g.drawString(font, text(snapshot, "title", "Chao activities"), left + 13, top + 10, INK, false);
    long selected = entries.stream().filter(e -> flag(e, "selected")).count();
    String mode = text(snapshot, "mode", "garden");
    String subtitle =
        mode.equals("garden")
            ? entries.size() + " Chao across your gardens"
            : selected
                + " / "
                + text(snapshot, "capacity", "8")
                + " places filled • other keepers can join";
    g.drawString(
        font, font.plainSubstrByWidth(subtitle, W - 24), left + 13, top + 25, MUTED, false);
    List<JsonObject> visible = shown();
    for (int row = 0; row < PAGE && page * PAGE + row < visible.size(); row++) {
      JsonObject entry = visible.get(page * PAGE + row);
      int y = top + 76 + row * 26;
      g.fill(
          left + 10,
          y,
          left + W - 10,
          y + 24,
          flag(entry, "selected") ? 0xffd9f1df : row % 2 == 0 ? 0xffe5f2f6 : 0xfff5fcff);
      String name = text(entry, "name", "Chao");
      String garden = text(entry, "garden", "Garden").split(" · ", 2)[0];
      name += " • " + garden;
      if (!flag(entry, "mine")) name += " • guest";
      g.drawString(font, font.plainSubstrByWidth(name, W - 124), left + 16, y + 3, INK, false);
      String details = text(entry, "labelstats", "");
      g.drawString(
          font,
          font.plainSubstrByWidth(details, W - 124),
          left + 16,
          y + 14,
          flag(entry, "available") || flag(entry, "selected") ? MUTED : 0xff98674c,
          false);
    }
    if (visible.isEmpty())
      g.drawString(font, "No registered Chao match this view.", left + 16, top + 92, MUTED, false);
    int pages = Math.max(1, (visible.size() + PAGE - 1) / PAGE);
    g.drawCenteredString(font, (page + 1) + " / " + pages, left + 61, top + 245, INK);
    String status = text(snapshot, "status", "Select your Chao to enter.");
    g.drawWordWrap(
        font,
        Component.literal(status.length() > 140 ? status.substring(0, 137) + "..." : status),
        left + 14,
        top + 267,
        W - 100,
        INK);
    // Screen.render also renders (and blurs) its background in 1.21.1. The artboard
    // has already been drawn, so render its widgets directly to keep text crisp.
    for (var widget : renderables) widget.render(g, localX(mouseX), localY(mouseY), partialTick);
    g.pose().popPose();
  }

  private int localX(double x) {
    return (int) ((x - offsetX) / canvasScale);
  }

  private int localY(double y) {
    return (int) ((y - offsetY) / canvasScale);
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
  public boolean mouseScrolled(double x, double y, double horizontal, double vertical) {
    int mx = localX(x), my = localY(y);
    if (mx >= 10 && mx <= W - 10 && my >= 76 && my < 234 && vertical != 0) {
      int pages = Math.max(1, (shown().size() + PAGE - 1) / PAGE);
      page = Math.max(0, Math.min(pages - 1, page + (vertical < 0 ? 1 : -1)));
      refreshWidgets();
      return true;
    }
    return super.mouseScrolled(mx, my, horizontal, vertical);
  }

  @Override
  public boolean isPauseScreen() {
    return false;
  }
}
