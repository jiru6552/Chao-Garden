package com.chaogarden.client;

import com.chaogarden.cwe.CweGiftItem;
import com.chaogarden.entity.ChaoEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.util.*;
import net.minecraft.client.Minecraft;
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
  private record Mesh(ChaoMeshAsset geometry,Map<String,ChaoMeshAsset.Pose> poses,ChaoAccessoryFit.Source fit,List<String> untinted){}
  private final Map<String,Mesh> meshes=new HashMap<>();
  public CweAccessoryLayer(RenderLayerParent<ChaoEntity,ChaoModel> parent){super(parent);}
  static String anchor(EquipmentSlot slot){return slot==EquipmentSlot.CHEST||slot==EquipmentSlot.LEGS?"body":"skull";}
  private Mesh mesh(String id){return meshes.computeIfAbsent(id,key->{
    var location=ResourceLocation.fromNamespaceAndPath("chao_garden","models/entity/cwe_accessories/"+key+".json");
    try(var reader=Minecraft.getInstance().getResourceManager().openAsReader(location)) {
      var data=JsonParser.parseReader(reader).getAsJsonObject();var m=ChaoMeshAsset.parse(data);
      var untinted=new ArrayList<String>();
      if(data.has("accessory_untinted_bones"))for(var name:data.getAsJsonArray("accessory_untinted_bones"))untinted.add(name.getAsString());
      return new Mesh(m,m.createPoses(),ChaoAccessoryFit.Source.read(data),List.copyOf(untinted));
    }catch(IOException|RuntimeException error){throw new IllegalStateException("Cannot load Chao accessory "+location,error);}
  });}
  private void draw(String id,String anchor,float[] headSurface,float[] bodySurface,PoseStack stack,MultiBufferSource buffers,int light,int color){
    var part=mesh(id);stack.pushPose();getParentModel().transformAccessoryAnchor(anchor,stack);
    var fit=ChaoAccessoryFit.accessory(id,anchor,getParentModel(),part.fit,headSurface,bodySurface);
    ChaoAccessoryFit.parts(id,headSurface,fit,part.poses,part.fit);
    fit.apply(stack);
    var texture=id.endsWith("_accessory")||id.startsWith("hoodie_")?ResourceLocation.fromNamespaceAndPath("chao_garden","textures/entity/chao/cwe_source02516.png"):TEXTURE;
    renderMesh(part.geometry,part.poses,part.untinted,stack,buffers.getBuffer(RenderType.entityCutoutNoCull(texture)),light,color);stack.popPose();
  }
  /** Source white trim keeps its original color while the cloth accepts dye.
   * Invisible parent own meshes still carry their children's sampled transforms. */
  static void renderMesh(ChaoMeshAsset geometry,Map<String,ChaoMeshAsset.Pose> poses,List<String> untinted,
      PoseStack stack,VertexConsumer vertices,int light,int color) {
    if(untinted.isEmpty()){geometry.render(stack,vertices,light,OverlayTexture.NO_OVERLAY,color,poses);return;}
    var visible=new HashMap<String,Boolean>();poses.forEach((name,pose)->visible.put(name,pose.visible));
    try {
      for(String name:untinted){var pose=poses.get(name);if(pose==null)throw new IllegalArgumentException("Missing source trim "+name);pose.visible=false;}
      geometry.render(stack,vertices,light,OverlayTexture.NO_OVERLAY,color,poses);
      poses.values().forEach(pose->pose.visible=false);
      for(String name:untinted)poses.get(name).visible=visible.get(name);
      geometry.render(stack,vertices,light,OverlayTexture.NO_OVERLAY,(color&0xff000000)|0xffffff,poses);
    }finally{poses.forEach((name,pose)->pose.visible=visible.get(name));}
  }
  @Override public void render(PoseStack stack,MultiBufferSource buffers,int light,ChaoEntity chao,float swing,float amount,float partial,float age,float yaw,float pitch){
    if(chao.officialComparison()||!getParentModel().officialActive()||chao.isCocooned()||chao.isInvisible())return;
    float[] headSurface=getParentModel().accessoryBounds("skull");
    if(chao.getChaoHat().equals("eggshell_hat"))headSurface=EggshellMask.fittedBounds(headSurface,getParentModel().accessoryEyes());
    else if(!chao.getChaoHat().isBlank())headSurface=ChaoAccessoryFit.coveredHead(headSurface);
    else if(ChaoAccessoryFit.hoodVisible(chao)){
      var hood=mesh("hoodie_hood");headSurface=ChaoAccessoryFit.hoodedHead(headSurface,hood.geometry.partBounds("accessory",hood.poses));
    }
    float[] bodySurface=getParentModel().accessoryBounds("body");
    if(chao.getItemBySlot(EquipmentSlot.CHEST).getItem() instanceof CweGiftItem garment&&garment.gift.id().endsWith("hoodie_accessory")){
      var body=mesh(garment.gift.id());bodySurface=ChaoAccessoryFit.clothedBody(bodySurface,body.geometry.partBounds("accessory",body.poses));
    }
    for(var slot:List.of(EquipmentSlot.FEET,EquipmentSlot.OFFHAND,EquipmentSlot.LEGS,EquipmentSlot.CHEST)){
      ItemStack worn=chao.getItemBySlot(slot);if(!(worn.getItem() instanceof CweGiftItem item)||item.slot()!=slot)continue;
      String id=item.gift.id();int rgb=id.equals("blue_hoodie_accessory")?0x4678ca:id.equals("pink_hoodie_accessory")?0xee98b7:0xffffff;
      var dye=worn.get(DataComponents.DYED_COLOR);if(dye!=null&&item.gift.dyable())rgb=dye.rgb();int color=0xff000000|rgb;
      draw(id,anchor(slot),headSurface,bodySurface,stack,buffers,light,color);
      if(slot==EquipmentSlot.CHEST){
        draw("hoodie_sleeve_left","arm_left",headSurface,bodySurface,stack,buffers,light,color);draw("hoodie_sleeve_right","arm_right",headSurface,bodySurface,stack,buffers,light,color);
        if(ChaoAccessoryFit.hoodVisible(chao))draw("hoodie_hood","skull",headSurface,bodySurface,stack,buffers,light,color);
        else draw("hoodie_hood_folded","body",headSurface,bodySurface,stack,buffers,light,color);
      }
    }
    String medal=chao.state().trait("medal.selected");
    if(medal.matches("(race|karate)_(bronze|silver|gold|champion)") && chao.state().intTrait("medal."+medal)>0){
      String tier=medal.substring(medal.indexOf('_')+1);
      int tint=switch(tier){case "bronze"->0xc98c5d;case "silver"->0xcdd7df;case "gold"->0xecc960;default->0xb596df;};
      draw(medal.startsWith("race_")?"medal_race":"medal_karate","body",headSurface,bodySurface,stack,buffers,light,0xff000000|tint);
    }

  }
}
