package com.chaogarden.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.HashMap;
import java.util.Map;

/** Skull-attached accessories share the exact current body hierarchy and pose. */
public final class ChaoHeadwear {
  private record Rig(ChaoMeshAsset geometry, Map<String, ChaoMeshAsset.Pose> poses) {}
  private final Map<String, Rig> rigs = new HashMap<>();

  private Rig rig(String path) {
    return rigs.computeIfAbsent(path, name -> {
      var geometry = ChaoMeshAsset.load(name);
      return new Rig(geometry, geometry.createPoses());
    });
  }

  @FunctionalInterface private interface JointCopy {void apply(String name,ChaoMeshAsset target,ChaoMeshAsset.Pose pose);}

  private void render(String path, String selected, Map<String, ChaoMeshAsset.Pose> source,JointCopy copier,ChaoAccessoryFit.Fit fit,
                      PoseStack stack, VertexConsumer vertices, int light, int overlay) {
    var rig = rig(path);
    rig.poses.values().forEach(ChaoMeshAsset.Pose::reset);
    for (var entry : rig.poses.entrySet()) {
      var pose = entry.getValue();
      var from = source.get(entry.getKey());
      if (from != null) {
        copier.apply(entry.getKey(),rig.geometry,pose);
      } else {
        // The source mesh's hidden base pieces do not hide replacement objects.
        pose.visible = entry.getKey().equals(selected);
      }
    }
    var accessory=rig.poses.get(selected);
    if(accessory!=null)fit.apply(accessory);
    rig.geometry.render(stack, vertices, light, overlay, -1, rig.poses);
  }

  public void renderMerchant(OfficialChaoRig body, PoseStack stack, VertexConsumer vertices,
                             int light, int overlay) {
    render("market_accessories", "merchant_accessories", body.poses(),body::copyAttachmentPoseTo,
        ChaoAccessoryFit.legacyHead(body.attachmentBounds("skull"),"baby"),
        stack, vertices, light, overlay);
  }

  void renderHat(ChaoModel model, String hat, PoseStack stack, VertexConsumer vertices,
                 int light, int overlay) {
    String key = model.officialGeometryKey();
    if (!key.startsWith("chao_")) key = "chao_" + key;
    render("hats/" + key, hat, model.officialPoses(),model::copyAccessoryPoseTo,
        ChaoAccessoryFit.legacyHead(model.accessoryBounds("skull"),key),stack, vertices, light, overlay);
  }
}
