package com.chaogarden.client;

import com.chaogarden.care.ChaoCareNetwork;
import com.chaogarden.entity.ChaoEntity;
import net.minecraft.client.Minecraft;

/** Client-only screen construction keeps common protocol registration dedicated-server safe. */
public final class ChaoCareClient {
  private ChaoCareClient() {}
  public static void receive(ChaoCareNetwork.Open message) {
    var mc=Minecraft.getInstance();
    if(mc.level!=null && mc.player!=null && mc.level.getEntity(message.entity()) instanceof ChaoEntity chao
        && mc.player.distanceToSqr(chao)<=64) {
      var screen=new ChaoInspectionScreen(chao,Math.max(0,Math.min(4,message.page())));
      if(message.page()==4)screen.showCare(message.section());
      else if(message.page()==3)screen.showMind(message.section());
      mc.setScreen(screen);
    }
  }
}
