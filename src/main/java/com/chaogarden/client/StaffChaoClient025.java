package com.chaogarden.client;

import com.chaogarden.kindergarten.KindergartenContent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

/** Invoked from the ordinary mod client registration, with no optional library dependency. */
public final class StaffChaoClient025 {
  private StaffChaoClient025(){}
  public static void registerLayers(EntityRenderersEvent.RegisterLayerDefinitions event){
    event.registerLayerDefinition(StaffChaoModel025.LAYER,StaffChaoModel025::createLayer);
  }
  public static void registerRenderer(EntityRenderersEvent.RegisterRenderers event){
    event.registerEntityRenderer(KindergartenContent.NPC.get(),StaffChaoRenderer025::new);
  }
}
