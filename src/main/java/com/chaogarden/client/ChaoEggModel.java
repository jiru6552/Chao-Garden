package com.chaogarden.client;

import com.chaogarden.entity.ChaoEggEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.Map;
import net.minecraft.client.model.HierarchicalModel;
import net.minecraft.client.model.geom.*;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.resources.ResourceLocation;

/** Closed low-poly shell with unwrapped longitude UVs and its matching editable project. */
public final class ChaoEggModel extends HierarchicalModel<ChaoEggEntity> {
  public static final ModelLayerLocation LAYER =
      new ModelLayerLocation(
          ResourceLocation.fromNamespaceAndPath("chao_garden", "chao_egg"), "main");
  private final ModelPart root;
  private final ChaoMeshAsset geometry;
  private final Map<String, ChaoMeshAsset.Pose> poses;

  public ChaoEggModel(ModelPart root) {
    this.root = root;
    geometry = ChaoMeshAsset.load("egg_mesh");
    poses = geometry.createPoses();
  }

  @Override
  public ModelPart root() {
    return root;
  }

  public static LayerDefinition createBodyLayer() {
    return LayerDefinition.create(new MeshDefinition(), 64, 64);
  }

  @Override
  public void setupAnim(ChaoEggEntity egg, float f, float f1, float time, float f3, float f4) {
    poses.values().forEach(ChaoMeshAsset.Pose::reset);
    ChaoMeshAsset.Pose p =
        poses.containsKey("egg") ? poses.get("egg") : poses.values().iterator().next();
    if(egg.isThrown()) {
      p.rx=time*.42f;
      p.rz=(float)Math.sin(time*.12f)*.16f;
    } else {
      float crack=egg.crackProgress(time-egg.tickCount);
      p.rz=(float)Math.sin(time*.26f)*(.045f*egg.hatchProgress()+.12f*crack);
      p.rx=(float)Math.cos(time*.19f)*(.025f*egg.hatchProgress()+.07f*crack);
      float pulse=(float)Math.sin(time*.52f)*.025f*crack;
      p.sx=p.sz=1+pulse;p.sy=1-pulse;
    }
  }

  @Override
  public void renderToBuffer(
      PoseStack stack, VertexConsumer vertices, int light, int overlay, int color) {
    geometry.render(stack, vertices, light, overlay, color, poses);
  }
}
