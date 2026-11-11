package com.chaogarden.client.activities;

import com.chaogarden.activities.ActivityBlock;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;

/** Live, depth-tested link from the selected course endpoint to the proposed flag. */
@EventBusSubscriber(modid = "chao_garden", value = Dist.CLIENT)
public final class CoursePreview {
  private record Link(Vec3 start, Vec3 end, double length, boolean valid) {}

  private static Link current() {
    var mc = Minecraft.getInstance();
    if (mc.player == null
        || mc.level == null
        || !(mc.hitResult instanceof BlockHitResult hit)
        || hit.getType() != HitResult.Type.BLOCK) return null;
    ItemStack item = mc.player.getMainHandItem();
    if (!BuiltInRegistries.ITEM
        .getKey(item.getItem())
        .toString()
        .equals("chao_garden:race_marker")) {
      item = mc.player.getOffhandItem();
      if (!BuiltInRegistries.ITEM
          .getKey(item.getItem())
          .toString()
          .equals("chao_garden:race_marker")) return null;
    }
    CompoundTag data = item.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
    if (!data.contains("RaceLink")) return null;
    CompoundTag source = data.getCompound("RaceLink");
    if (!source.getString("Dimension").equals(mc.level.dimension().location().toString()))
      return null;
    BlockPos from = BlockPos.of(source.getLong("Pos"));
    var clicked = mc.level.getBlockState(hit.getBlockPos());
    boolean finish =
        clicked.getBlock() instanceof ActivityBlock endpoint
            && endpoint.kind == ActivityBlock.Kind.FINISH;
    if (clicked.getBlock() instanceof ActivityBlock endpoint
        && (endpoint.kind == ActivityBlock.Kind.START || endpoint.kind == ActivityBlock.Kind.FLAG))
      return null; // This click selects a new source instead of placing a flag.
    BlockPos to =
        finish
            ? hit.getBlockPos()
            : clicked.canBeReplaced()
                ? hit.getBlockPos()
                : hit.getBlockPos().relative(hit.getDirection());
    if (finish) {
      Direction backwards =
          clicked.getValue(ActivityBlock.AXIS) == Direction.Axis.X
              ? Direction.WEST
              : Direction.NORTH;
      for (int i = 0; i < 8; i++) {
        var neighbor = mc.level.getBlockState(to.relative(backwards));
        if (!(neighbor.getBlock() instanceof ActivityBlock endpoint)
            || endpoint.kind != ActivityBlock.Kind.FINISH
            || neighbor.getValue(ActivityBlock.AXIS) != clicked.getValue(ActivityBlock.AXIS)) break;
        to = to.relative(backwards);
      }
    }
    Vec3 a = Vec3.atCenterOf(from).add(0, .65, 0), b = Vec3.atCenterOf(to).add(0, .65, 0);
    double length = a.distanceTo(b);
    return new Link(
        a,
        b,
        length,
        length > .1 && length <= 32.0001 && (finish || mc.level.getBlockState(to).canBeReplaced()));
  }

  @SubscribeEvent
  public static void line(RenderLevelStageEvent event) {
    if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES) return;
    Link link = current();
    if (link == null || link.length < .0001) return;
    var mc = Minecraft.getInstance();
    var pose = event.getPoseStack();
    var camera = event.getCamera().getPosition();
    pose.pushPose();
    pose.translate(-camera.x, -camera.y, -camera.z);
    VertexConsumer out = mc.renderBuffers().bufferSource().getBuffer(RenderType.lines());
    Vec3 normal = link.end.subtract(link.start).normalize();
    int r = link.valid ? 65 : 239, g = link.valid ? 224 : 73, b = link.valid ? 154 : 66;
    out.addVertex(
            pose.last().pose(), (float) link.start.x, (float) link.start.y, (float) link.start.z)
        .setColor(r, g, b, 235)
        .setNormal(pose.last(), (float) normal.x, (float) normal.y, (float) normal.z);
    out.addVertex(pose.last().pose(), (float) link.end.x, (float) link.end.y, (float) link.end.z)
        .setColor(r, g, b, 235)
        .setNormal(pose.last(), (float) normal.x, (float) normal.y, (float) normal.z);
    mc.renderBuffers().bufferSource().endBatch(RenderType.lines());
    pose.popPose();
  }

  @SubscribeEvent
  public static void label(RenderGuiEvent.Post event) {
    var mc = Minecraft.getInstance();
    if (mc.options.hideGui) return;
    Link link = current();
    if (link == null) return;
    String label =
        String.format(
            java.util.Locale.ROOT,
            "Race link: %.1f / 32 blocks%s",
            link.length,
            link.valid ? "" : " — too far or blocked");
    event
        .getGuiGraphics()
        .drawCenteredString(
            mc.font,
            label,
            mc.getWindow().getGuiScaledWidth() / 2,
            mc.getWindow().getGuiScaledHeight() - 65,
            link.valid ? 0xff8ff2bf : 0xffff897d);
  }
}
