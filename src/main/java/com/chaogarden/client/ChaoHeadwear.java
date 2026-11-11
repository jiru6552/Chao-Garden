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

  private void render(String path, String selected, Map<String, ChaoMeshAsset.Pose> source,ChaoAccessoryFit.Fit fit,
                      PoseStack stack, VertexConsumer vertices, int light, int overlay) {
    var rig = rig(path);
    rig.poses.values().forEach(ChaoMeshAsset.Pose::reset);
    for (var entry : rig.poses.entrySet()) {
      var pose = entry.getValue();
      var from = source.get(entry.getKey());
      if (from != null) {
        pose.x = from.x; pose.y = from.y; pose.z = from.z;
        pose.rx = from.rx; pose.ry = from.ry; pose.rz = from.rz;
        pose.sx = from.sx; pose.sy = from.sy; pose.sz = from.sz;
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
    render("market_accessories", "merchant_accessories", body.poses(),
        ChaoAccessoryFit.legacyHead(body.attachmentBounds("skull"),"baby"),
        stack, vertices, light, overlay);
  }

  void renderHat(ChaoModel model, String hat, PoseStack stack, VertexConsumer vertices,
                 int light, int overlay) {
    String key = model.officialGeometryKey();
    if (!key.startsWith("chao_")) key = "chao_" + key;
    render("hats/" + key, hat, model.officialPoses(),
        ChaoAccessoryFit.legacyHead(model.accessoryBounds("skull"),key),stack, vertices, light, overlay);
  }
}
