package com.chaogarden.toys;

import com.chaogarden.ChaoGarden;
import net.minecraft.core.particles.SimpleParticleType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ChaoToyParticles {
  private static final DeferredRegister<net.minecraft.core.particles.ParticleType<?>> TYPES=
      DeferredRegister.create(net.minecraft.core.registries.Registries.PARTICLE_TYPE,ChaoGarden.MODID);
  public static final net.neoforged.neoforge.registries.DeferredHolder<net.minecraft.core.particles.ParticleType<?>,SimpleParticleType>
      SOAP_BUBBLE=TYPES.register("soap_bubble",()->new SimpleParticleType(false));
  private ChaoToyParticles(){}
  public static void register(IEventBus bus){TYPES.register(bus);}
}
