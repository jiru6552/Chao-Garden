package com.chaogarden.client;

import com.chaogarden.entity.ChaoEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import java.util.*;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;

/** Separate terminal geometry makes the world visible through two-tone Chaos ice. */
public final class ChaoChaosGlassLayer extends RenderLayer<ChaoEntity,ChaoModel> {
  private record Rig(ChaoMeshAsset geometry,Map<String,ChaoMeshAsset.Pose> poses) {}
  private final Map<String,Rig> rigs=new HashMap<>();
  public ChaoChaosGlassLayer(RenderLayerParent<ChaoEntity,ChaoModel> parent){super(parent);}

  @Override public void render(PoseStack stack,MultiBufferSource buffers,int light,
      ChaoEntity entity,float swing,float amount,float partialTick,float age,float yaw,float pitch) {
    var state=entity.state();var model=getParentModel();
    if(entity.officialComparison()||!model.officialActive()||entity.isCocooned()||entity.isInvisible()
        ||!state.immortal||!state.character.equals("normal")||!model.gameplayFormKey().startsWith("chaos_"))return;
    Rig rig=rigs.computeIfAbsent(model.gameplayFormKey(),key->{
      var mesh=ChaoMeshAsset.load("gameplay/chao_"+key+"_glass");return new Rig(mesh,mesh.createPoses());
    });
    rig.poses.values().forEach(ChaoMeshAsset.Pose::reset);
    for(var entry:model.officialPoses().entrySet()) {
      var to=rig.poses.get(entry.getKey());if(to==null)continue;
      var from=entry.getValue();
      to.x=from.x;to.y=from.y;to.z=from.z;to.rx=from.rx;to.ry=from.ry;to.rz=from.rz;
      to.sx=from.sx;to.sy=from.sy;to.sz=from.sz;to.reach=from.reach;to.visible=from.visible;
    }
    boolean transparent=state.isTwoTone();
    var texture=ChaoChaosGlassMaterial.texture(OfficialChaoRig.texture(state),transparent);
    // Culling avoids doubled front/back alpha. Vanilla's translucent quad buffer sorts
    // the exposed shell faces while opaque body/eyes retain their normal depth pass.
    var type=transparent?RenderType.entityTranslucentCull(texture):RenderType.entityCutoutNoCull(texture);
    rig.geometry.render(stack,buffers.getBuffer(type),light,OverlayTexture.NO_OVERLAY,-1,rig.poses);
  }
}
