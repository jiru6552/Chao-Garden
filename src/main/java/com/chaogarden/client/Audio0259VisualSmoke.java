package com.chaogarden.client;

import com.chaogarden.ChaoGarden;
import com.chaogarden.core.AreaMusicRules0257;
import com.mojang.logging.LogUtils;
import java.util.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.sound.PlaySoundEvent;

/** Development-only observer. Synthetic audio does not establish original-song provenance. */
@EventBusSubscriber(modid=ChaoGarden.MODID,value=Dist.CLIENT)
public final class Audio0259VisualSmoke {
  private static final Map<SoundInstance,Integer> sounds=new IdentityHashMap<>();
  private static final Set<String> areas=new HashSet<>();
  private static int tick,chorusStarts,chorusActiveTicks,lastChorusStart=-10000;
  private static boolean stoppedAfterExit;
  private static boolean enabled(){return Boolean.getBoolean("chao_garden.audio0259Probe");}
  @SubscribeEvent public static void sound(PlaySoundEvent event){
    if(!enabled()||event.getSound()==null)return;
    var sound=event.getSound();var id=sound.getLocation();
    if(id.getNamespace().equals(ChaoGarden.MODID)&&(id.getPath().startsWith("music.area.")||id.getPath().equals("chao.classroom_chorus"))){
      sounds.put(sound,tick);
      if(id.getPath().equals("chao.classroom_chorus")){
        if(tick-lastChorusStart<170)throw new IllegalStateException("Full classroom chorus restarted before the nine-second fixture ended");
        lastChorusStart=tick;chorusStarts++;
      }
    }
  }
  @SubscribeEvent public static void tick(ClientTickEvent.Post event){
    if(!enabled())return;
    var mc=Minecraft.getInstance();if(mc.level==null||mc.player==null||mc.isPaused())return;tick++;
    int activeChorus=0;
    for(var row:sounds.entrySet()){
      var sound=row.getKey();if(!mc.getSoundManager().isActive(sound))continue;
      var id=sound.getLocation().getPath();
      if(id.equals("chao.classroom_chorus"))activeChorus++;
      if(tick-row.getValue()>10&&id.startsWith("music.area."))areas.add(id.substring("music.area.".length()));
    }
    if(activeChorus>1)throw new IllegalStateException("Overlapping classroom chorus streams");
    if(activeChorus==1)chorusActiveTicks++;
    if(chorusActiveTicks>20&&!mc.level.dimension().location().toString().equals("chao_garden:kindergarten")&&activeChorus==0)stoppedAfterExit=true;
    sounds.entrySet().removeIf(row->tick-row.getValue()>40&&!mc.getSoundManager().isActive(row.getKey()));
  }
  public static boolean chorusObserved(){return chorusActiveTicks>10;}
  public static void complete(){
    if(!enabled())return;
    if(!areas.containsAll(Set.of("lobby","hero","dark","kindergarten"))||chorusStarts<2||chorusActiveTicks<180||!stoppedAfterExit)
      throw new IllegalStateException("Incomplete actual audio lifecycle: areas="+areas+" chorusStarts="+chorusStarts+" activeTicks="+chorusActiveTicks+" exit="+stoppedAfterExit);
    LogUtils.getLogger().info("CHAO_AUDIO0259_PASS areas={} chorusStarts={} activeTicks={} noOverlap=true stoppedAfterExit=true syntheticNineSecondAudio=true audibleListeningClaim=false",areas,chorusStarts,chorusActiveTicks);
  }
}
