package com.chaogarden.music;

import com.chaogarden.ChaoGarden;
import com.chaogarden.core.AreaMusicRules0257;
import java.util.EnumMap;
import java.util.Map;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.JukeboxSong;
import net.minecraft.world.item.Rarity;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/** A normal 1.21.1 jukebox disc, including vanilla playback, ejection and redstone. */
public final class GardenMusicContent {
  private static final DeferredRegister<SoundEvent> SOUNDS =
      DeferredRegister.create(Registries.SOUND_EVENT, ChaoGarden.MODID);
  public static final ResourceKey<JukeboxSong> SONG = ResourceKey.create(
      Registries.JUKEBOX_SONG, ResourceLocation.fromNamespaceAndPath(ChaoGarden.MODID, "chao_garden"));
  public static final DeferredHolder<SoundEvent, SoundEvent> MUSIC = SOUNDS.register(
      "music.chao_garden", () -> SoundEvent.createVariableRangeEvent(
          ResourceLocation.fromNamespaceAndPath(ChaoGarden.MODID, "music.chao_garden")));
  private static final Map<AreaMusicRules0257.Track,DeferredHolder<SoundEvent,SoundEvent>> AREAS=new EnumMap<>(AreaMusicRules0257.Track.class);
  static {
    for(var track:AreaMusicRules0257.Track.values())if(track!=AreaMusicRules0257.Track.NONE){
      var id=ResourceLocation.fromNamespaceAndPath(ChaoGarden.MODID,track.event());
      AREAS.put(track,SOUNDS.register(track.event(),()->SoundEvent.createVariableRangeEvent(id)));
    }
  }
  private static boolean bootstrapped;

  private GardenMusicContent() {}

  public static void bootstrap() {
    if (bootstrapped) return;
    bootstrapped = true;
    ChaoGarden.add("chao_garden_music_disc", () -> new Item(new Item.Properties()
        .stacksTo(1).rarity(Rarity.UNCOMMON).jukeboxPlayable(SONG)));
  }

  public static void register(IEventBus bus) { SOUNDS.register(bus); }
  public static SoundEvent area(AreaMusicRules0257.Track track){
    var holder=AREAS.get(track);if(holder==null)throw new IllegalArgumentException("No area event for "+track);return holder.get();
  }
}
