package com.chaogarden.client;

import com.chaogarden.ChaoGarden;
import com.chaogarden.toys.ChaoToyParticles;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.*;
import net.minecraft.core.particles.SimpleParticleType;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent;

/** Air-safe soap bubbles; vanilla water bubbles remove themselves outside water. */
@EventBusSubscriber(modid=ChaoGarden.MODID,value=Dist.CLIENT,bus=EventBusSubscriber.Bus.MOD)
public final class SoapBubbleParticle extends TextureSheetParticle {
  private SoapBubbleParticle(ClientLevel level,double x,double y,double z,double vx,double vy,double vz,SpriteSet sprites) {
    super(level,x,y,z,vx,vy,vz);pickSprite(sprites);
    xd=vx;yd=vy+.014;zd=vz;friction=.98f;gravity=-.018f;hasPhysics=true;
    lifetime=32+random.nextInt(20);quadSize=.045f+random.nextFloat()*.035f;
    setColor(.85f,.96f,1);setAlpha(.85f);
  }
  @Override public ParticleRenderType getRenderType(){return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;}
  @Override public void tick(){super.tick();setAlpha(.85f*Math.min(1f,(lifetime-age)/8f));if(onGround)remove();}
  @SubscribeEvent public static void register(RegisterParticleProvidersEvent event){
    event.registerSpriteSet(ChaoToyParticles.SOAP_BUBBLE.get(),sprites->
        (SimpleParticleType type,ClientLevel level,double x,double y,double z,double vx,double vy,double vz)->
            new SoapBubbleParticle(level,x,y,z,vx,vy,vz,sprites));
  }
}
