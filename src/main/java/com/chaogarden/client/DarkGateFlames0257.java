package com.chaogarden.client;

import com.chaogarden.ChaoGarden;
import com.chaogarden.core.GardenGateGeometry0256;
import com.chaogarden.hub.HubContent;
import com.chaogarden.hub.HubRooms;
import net.minecraft.client.Minecraft;
import net.minecraft.core.particles.ParticleTypes;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

/** Native blue soul flames and wisps at the gate cups; no fire block or server damage is created. */
@EventBusSubscriber(modid=ChaoGarden.MODID,value=Dist.CLIENT)
public final class DarkGateFlames0257 {
  private DarkGateFlames0257(){}
  @SubscribeEvent public static void tick(ClientTickEvent.Post event){
    var mc=Minecraft.getInstance();var level=mc.level;
    if(level==null||mc.player==null||mc.isPaused()||!level.dimension().equals(HubContent.HUB))return;
    for(var local:GardenGateGeometry0256.darkFlames()){
      var at=HubRooms.gateWorldPoint(HubRooms.DARK_DOOR,HubRooms.DARK_FACING,local);
      if(mc.player.distanceToSqr(at.x,at.y,at.z)>48*48)continue;
      for(int i=0;i<3;i++){
        double x=at.x+(level.random.nextDouble()-.5)*.18,z=at.z+(level.random.nextDouble()-.5)*.18;
        level.addParticle(ParticleTypes.SOUL_FIRE_FLAME,x,at.y+.03,z,0,.012+level.random.nextDouble()*.012,0);
      }
      if(level.getGameTime()%6==0)level.addParticle(ParticleTypes.SMOKE,at.x,at.y+.30,at.z,0,.018,0);
    }
  }
}
