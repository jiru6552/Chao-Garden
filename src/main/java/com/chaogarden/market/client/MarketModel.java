package com.chaogarden.market.client;

import com.chaogarden.client.OfficialChaoRig;
import com.chaogarden.client.ChaoHeadwear;
import com.chaogarden.market.BlackMarketTrader;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.HierarchicalModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.resources.ResourceLocation;

/** The same imported Chao anatomy and idle/walk clips used by garden Chao. */
public final class MarketModel extends HierarchicalModel<BlackMarketTrader> {
  public static final ModelLayerLocation LAYER = new ModelLayerLocation(
      ResourceLocation.fromNamespaceAndPath("chao_garden", "black_market_trader"), "main");
  private final ModelPart root;
  private final OfficialChaoRig rig = new OfficialChaoRig();
  private final ChaoHeadwear accessories = new ChaoHeadwear();
  public MarketModel(ModelPart root){this.root=root;}
  public static LayerDefinition createLayer(){return LayerDefinition.create(new MeshDefinition(),64,32);}
  @Override public ModelPart root(){return root;}
  @Override public void setupAnim(BlackMarketTrader trader,float swing,float amount,float age,float yaw,float pitch){
    rig.animateGameplay("baby",trader.getUUID(),OfficialChaoRig.Motion.WALK,
        age,swing,amount,0,0,yaw,pitch,true);
    // The shopkeeper's small round spectacles replace the normal raised eye
    // panels. Leaving those panels visible would produce white rims around
    // the dark lenses, unlike the supplied SA2 reference.
    rig.hideCube("skull",2);
    rig.hideCube("skull",3);
  }
  @Override public void renderToBuffer(PoseStack pose,VertexConsumer vertices,int light,int overlay,int color){
    rig.render(pose,vertices,light,overlay,color);
  }
  public void renderAccessories(PoseStack pose,VertexConsumer vertices,int light,int overlay){
    accessories.renderMerchant(rig,pose,vertices,light,overlay);
  }
}
