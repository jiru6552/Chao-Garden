package com.chaogarden.client;

import com.chaogarden.ChaoGarden;
import com.chaogarden.core.AreaMusicRules0257;
import com.chaogarden.core.AreaMusicRules0257.Track;
import com.chaogarden.music.GardenMusicContent;
import java.util.EnumSet;
import java.util.IdentityHashMap;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundSource;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.sound.PlaySoundEvent;

/** Client-local streaming music. Missing optional recordings leave vanilla music alone. */
@EventBusSubscriber(modid=ChaoGarden.MODID,value=Dist.CLIENT)
public final class AreaMusic0257 {
  private static final Map<SoundInstance,Integer> RECORDS=new IdentityHashMap<>();
  private static Loop current,tail;
  private static int ticks;
  private static Object previousLevel;
  private AreaMusic0257(){}

  public static Track currentTrack(){return current==null?Track.NONE:current.track;}
  public static boolean available(Minecraft mc,Track track){
    if(track==Track.NONE)return false;
    var asset=ResourceLocation.fromNamespaceAndPath(ChaoGarden.MODID,"sounds/"+track.asset()+".ogg");
    if(mc.getResourceManager().getResource(asset).isEmpty())return false;
    var event=mc.getSoundManager().getSoundEvent(ResourceLocation.fromNamespaceAndPath(ChaoGarden.MODID,track.event()));
    return event!=null&&event.getWeight()>0;
  }
  @SubscribeEvent public static void sound(PlaySoundEvent event){
    var sound=event.getSound();if(sound==null)return;
    if(sound.getSource()==SoundSource.RECORDS)RECORDS.put(sound,ticks);
    // Only ordinary Minecraft music is suppressed, and only while an actual
    // available regional recording is selected. Jukeboxes keep their own audio.
    var mc=Minecraft.getInstance();
    if(mc.level!=null&&mc.player!=null&&current!=null&&!current.isStopped()
        &&AreaMusicRules0257.region(mc.level.dimension().location().toString(),mc.player.getX(),mc.player.getZ())==current.track
        &&sound.getSource()==SoundSource.MUSIC
        &&sound.getLocation().getNamespace().equals("minecraft"))event.setSound(null);
  }
  @SubscribeEvent public static void tick(ClientTickEvent.Post event){
    var mc=Minecraft.getInstance();ticks++;
    if(mc.level==null||mc.player==null){clear(mc);previousLevel=null;return;}
    if(previousLevel!=mc.level){clear(mc);previousLevel=mc.level;}
    if(mc.isPaused())return;
    var available=EnumSet.noneOf(Track.class);
    for(var track:Track.values())if(available(mc,track))available.add(track);
    var desired=AreaMusicRules0257.select(mc.level.dimension().location().toString(),mc.player.getX(),mc.player.getZ(),available);
    if(current!=null&&current.track!=desired){
      if(tail!=null)mc.getSoundManager().stop(tail);
      tail=current;tail.fadeOut();current=null;
    }
    if(desired!=Track.NONE&&current==null){
      mc.getMusicManager().stopPlaying();
      current=new Loop(desired,ticks);mc.getSoundManager().play(current);
    }
    RECORDS.entrySet().removeIf(row->ticks-row.getValue()>20&&!mc.getSoundManager().isActive(row.getKey()));
    if(current!=null){
      current.target=RECORDS.isEmpty()?.65f:0;
      if(current.track==Track.KINDERGARTEN&&SchoolChorus0259.selected())current.target=0;
      // Resource reload and an audio-device reset can discard queued sounds.
      // Restart only the selected available recording, with a bounded grace.
      if(ticks-current.started>60&&!mc.getSoundManager().isActive(current)){
        mc.getSoundManager().stop(current);current=new Loop(desired,ticks);mc.getSoundManager().play(current);
      }
    }
    if(tail!=null&&(tail.isStopped()||ticks-tail.started>60&&!mc.getSoundManager().isActive(tail)))tail=null;
  }
  private static void clear(Minecraft mc){
    if(current!=null)mc.getSoundManager().stop(current);
    if(tail!=null)mc.getSoundManager().stop(tail);
    current=null;tail=null;RECORDS.clear();
  }
  private static final class Loop extends AbstractTickableSoundInstance {
    final Track track;final int started;float target=.65f;boolean ending;
    Loop(Track track,int started){
      super(GardenMusicContent.area(track),SoundSource.MUSIC,SoundInstance.createUnseededRandom());
      this.track=track;this.started=started;this.looping=true;this.delay=0;
      this.relative=true;this.attenuation=SoundInstance.Attenuation.NONE;this.volume=0;this.pitch=1;
    }
    void fadeOut(){ending=true;target=0;}
    @Override public boolean canStartSilent(){return true;}
    @Override public void tick(){volume=AreaMusicRules0257.fade(volume,target);if(ending&&volume==0)stop();}
  }
}
