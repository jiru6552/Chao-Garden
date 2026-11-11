package com.chaogarden;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Chao voices are separate events so resource packs can replace each reaction independently. */
public final class ChaoSounds {
  private static final DeferredRegister<SoundEvent> SOUNDS =
      DeferredRegister.create(Registries.SOUND_EVENT, ChaoGarden.MODID);

  public static final DeferredHolder<SoundEvent, SoundEvent> AMBIENT = register("chao.ambient");
  public static final DeferredHolder<SoundEvent, SoundEvent> HAPPY = register("chao.happy");
  public static final DeferredHolder<SoundEvent, SoundEvent> HURT = register("chao.hurt");
  public static final DeferredHolder<SoundEvent, SoundEvent> DEATH = register("chao.death");
  public static final DeferredHolder<SoundEvent, SoundEvent> EVOLVE = register("chao.evolve");
  public static final DeferredHolder<SoundEvent, SoundEvent> CLASSROOM_CHORUS = register("chao.classroom_chorus");
  public static final DeferredHolder<SoundEvent, SoundEvent> SINGING_A = register("chao.singing_a");
  public static final DeferredHolder<SoundEvent, SoundEvent> SINGING_B = register("chao.singing_b");

  public static final DeferredHolder<SoundEvent, SoundEvent> CYMBALS = register("toy.cymbals");
  public static final DeferredHolder<SoundEvent, SoundEvent> CASTANETS = register("toy.castanets");
  public static final DeferredHolder<SoundEvent, SoundEvent> TAMBOURINE = register("toy.tambourine");
  public static final DeferredHolder<SoundEvent, SoundEvent> HAND_BELL = register("toy.hand_bell");
  public static final DeferredHolder<SoundEvent, SoundEvent> MARACAS = register("toy.maracas");
  public static final DeferredHolder<SoundEvent, SoundEvent> RATTLE = register("toy.rattle");

  public static final DeferredHolder<SoundEvent, SoundEvent> ORGAN = register("toy.organ");

  public static final DeferredHolder<SoundEvent, SoundEvent> GUITAR = register("toy.guitar");
  public static final DeferredHolder<SoundEvent, SoundEvent> ACCORDION = register("toy.accordion");

  private ChaoSounds() {}

  private static DeferredHolder<SoundEvent, SoundEvent> register(String id) {
    return SOUNDS.register(
        id,
        () ->
            SoundEvent.createVariableRangeEvent(
                ResourceLocation.fromNamespaceAndPath(ChaoGarden.MODID, id)));
  }

  public static void register(IEventBus bus) {
    SOUNDS.register(bus);
  }
}
