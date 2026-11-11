package com.chaogarden.client;

import com.chaogarden.entity.ChaoEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import java.util.*;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;

/** Temporary paper hat follows the skull; actual equipped headwear has priority. */
public final class ChaoPartyLayer extends RenderLayer<ChaoEntity,ChaoModel> {
  private static final ResourceLocation TEXTURE=ResourceLocation.fromNamespaceAndPath("chao_garden","textures/entity/chao/cwe_parts.png");
  private record Hat(ChaoMeshAsset mesh,Map<String,ChaoMeshAsset.Pose> poses){}
  private final Map<String,Hat> hats=new HashMap<>();
  public ChaoPartyLayer(RenderLayerParent<ChaoEntity,ChaoModel> parent){super(parent);}
  @Override public void render(PoseStack stack,MultiBufferSource buffers,int light,ChaoEntity chao,float swing,float amount,float partial,float age,float yaw,float pitch){
    if(chao.officialComparison()||!getParentModel().officialActive()||chao.isCocooned()||chao.isInvisible()
        ||chao.getBehavior()!=ChaoEntity.Behavior.BIRTHDAY||!chao.getItemBySlot(EquipmentSlot.HEAD).isEmpty()
        ||!chao.getItemBySlot(EquipmentSlot.FEET).isEmpty())return;
    var hat=hats.computeIfAbsent(getParentModel().officialGeometryKey(),key->{var mesh=ChaoMeshAsset.load("party_hats/chao_"+key);return new Hat(mesh,mesh.createPoses());});
    stack.pushPose();getParentModel().transformAccessoryAnchor("skull",stack);
    ChaoAccessoryFit.legacyHead(getParentModel().accessoryBounds("skull"),getParentModel().officialGeometryKey()).apply(stack);
    hat.mesh.render(stack,buffers.getBuffer(RenderType.entityCutoutNoCull(TEXTURE)),light,OverlayTexture.NO_OVERLAY,-1,hat.poses);
    stack.popPose();
  }
}
