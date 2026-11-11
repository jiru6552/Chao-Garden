package com.chaogarden.client;

import com.chaogarden.cwe.CweGiftItem;
import com.chaogarden.entity.ChaoEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import java.util.*;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;

/** Real equipment stacks attach to sampled skull, torso and arm transforms. */
public final class CweAccessoryLayer extends RenderLayer<ChaoEntity,ChaoModel> {
  private static final ResourceLocation TEXTURE=ResourceLocation.fromNamespaceAndPath("chao_garden","textures/entity/chao/cwe_accessories.png");
  private record Mesh(ChaoMeshAsset geometry,Map<String,ChaoMeshAsset.Pose> poses){}
  private final Map<String,Mesh> meshes=new HashMap<>();
  public CweAccessoryLayer(RenderLayerParent<ChaoEntity,ChaoModel> parent){super(parent);}
  static String anchor(EquipmentSlot slot){return slot==EquipmentSlot.CHEST||slot==EquipmentSlot.LEGS?"body":"skull";}
  private Mesh mesh(String id){return meshes.computeIfAbsent(id,key->{var m=ChaoMeshAsset.load("cwe_accessories/"+key);return new Mesh(m,m.createPoses());});}
  private void draw(String id,String anchor,PoseStack stack,MultiBufferSource buffers,int light,int color){
    var part=mesh(id);stack.pushPose();getParentModel().transformAccessoryAnchor(anchor,stack);
    var fit=ChaoAccessoryFit.accessory(id,anchor,getParentModel());
    ChaoAccessoryFit.parts(id,getParentModel().accessoryBounds("skull"),fit,part.poses);
    fit.apply(stack);
    var texture=id.endsWith("_accessory")||id.startsWith("hoodie_")?ResourceLocation.fromNamespaceAndPath("chao_garden","textures/entity/chao/cwe_source02516.png"):TEXTURE;
    part.geometry.render(stack,buffers.getBuffer(RenderType.entityCutoutNoCull(texture)),light,OverlayTexture.NO_OVERLAY,color,part.poses);stack.popPose();
  }
  @Override public void render(PoseStack stack,MultiBufferSource buffers,int light,ChaoEntity chao,float swing,float amount,float partial,float age,float yaw,float pitch){
    if(chao.officialComparison()||!getParentModel().officialActive()||chao.isCocooned()||chao.isInvisible())return;
    for(var slot:List.of(EquipmentSlot.FEET,EquipmentSlot.OFFHAND,EquipmentSlot.LEGS,EquipmentSlot.CHEST)){
      ItemStack worn=chao.getItemBySlot(slot);if(!(worn.getItem() instanceof CweGiftItem item)||item.slot()!=slot)continue;
      String id=item.gift.id();int rgb=id.equals("blue_hoodie_accessory")?0x4678ca:id.equals("pink_hoodie_accessory")?0xee98b7:0xffffff;
      var dye=worn.get(DataComponents.DYED_COLOR);if(dye!=null&&item.gift.dyable())rgb=dye.rgb();int color=0xff000000|rgb;
      draw(id,anchor(slot),stack,buffers,light,color);
      if(slot==EquipmentSlot.CHEST){draw("hoodie_sleeve_left","arm_left",stack,buffers,light,color);draw("hoodie_sleeve_right","arm_right",stack,buffers,light,color);if(ChaoAccessoryFit.hoodVisible(chao))draw("hoodie_hood","skull",stack,buffers,light,color);}
    }
    String medal=chao.state().trait("medal.selected");
    if(medal.matches("(race|karate)_(bronze|silver|gold|champion)") && chao.state().intTrait("medal."+medal)>0){
      String tier=medal.substring(medal.indexOf('_')+1);
      int tint=switch(tier){case "bronze"->0xc98c5d;case "silver"->0xcdd7df;case "gold"->0xecc960;default->0xb596df;};
      draw(medal.startsWith("race_")?"medal_race":"medal_karate","body",stack,buffers,light,0xff000000|tint);
    }

  }
}
