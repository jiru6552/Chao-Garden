package com.chaogarden.client;

import com.chaogarden.ChaoGarden;
import com.google.gson.JsonParser;
import com.mojang.logging.LogUtils;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.sound.PlaySoundEvent;

/** Development-only observation of the supplied recordings through native SoundManager. */
@EventBusSubscriber(modid=ChaoGarden.MODID,value=Dist.CLIENT)
public final class Audio02511VisualSmoke {
  private static final class Observation {
    final int tick;final long wall=System.nanoTime();boolean active;String asset;
    Observation(int tick){this.tick=tick;}
  }
  private static final Map<SoundInstance,Observation> sounds=new IdentityHashMap<>();
  private static final Map<String,Double> durations=new HashMap<>();
  private static final Set<String> areas=new HashSet<>(),chorusAssets=new HashSet<>();
  private static int tick,chorusStarts,chorusActiveTicks,naturalCompletions;
  private static boolean ducked,restored,stoppedAfterExit;
  private static boolean enabled(){return Boolean.getBoolean("chao_garden.audio02511Probe");}
  private static void load(){
    if(!durations.isEmpty())return;
    try(var reader=Files.newBufferedReader(Path.of(System.getProperty("chao_garden.audio02511Manifest")))){
      for(var entry:JsonParser.parseReader(reader).getAsJsonObject().getAsJsonArray("recordings")){
        var row=entry.getAsJsonObject();String asset=row.get("asset").getAsString().replace("assets/chao_garden/sounds/","chao_garden:").replace(".ogg","");
        durations.put(asset,row.get("duration_seconds").getAsDouble());
      }
      if(durations.size()!=11)throw new IllegalStateException("Expected eleven supplied recordings");
    }catch(Exception error){throw new IllegalStateException("Cannot load supplied audio evidence",error);}
  }
  @SubscribeEvent public static void sound(PlaySoundEvent event){
    if(!enabled()||event.getSound()==null)return;
    var sound=event.getSound();var id=sound.getLocation();
    if(id.getNamespace().equals(ChaoGarden.MODID)&&(id.getPath().startsWith("music.area.")||id.getPath().equals("chao.classroom_chorus"))){
      load();sounds.put(sound,new Observation(tick));
      if(id.getPath().equals("chao.classroom_chorus"))chorusStarts++;
    }
  }
  @SubscribeEvent public static void tick(ClientTickEvent.Post event){
    if(!enabled())return;
    var mc=Minecraft.getInstance();if(mc.level==null||mc.player==null||mc.isPaused())return;tick++;
    int activeChorus=0;
    var iterator=sounds.entrySet().iterator();
    while(iterator.hasNext()){
      var row=iterator.next();var sound=row.getKey();var observation=row.getValue();
      String eventId=sound.getLocation().getPath();boolean chorus=eventId.equals("chao.classroom_chorus");
      if(mc.getSoundManager().isActive(sound)){
        if(!observation.active){
          observation.asset=sound.getSound().getLocation().toString();
          if(!durations.containsKey(observation.asset)||!sound.getSound().shouldStream())throw new IllegalStateException("Not a supplied streaming recording: "+observation.asset);
          observation.active=true;
          LogUtils.getLogger().info("CHAO_AUDIO02511_ACTIVE event={} asset={} sourceSeconds={}",eventId,observation.asset,durations.get(observation.asset));
        }
        if(chorus){activeChorus++;chorusAssets.add(observation.asset);}
        else if(tick-observation.tick>10){
          areas.add(eventId.substring("music.area.".length()));
          if(eventId.equals("music.area.kindergarten")){
            if(SchoolChorus0259.selected()&&sound.getVolume()<.01f)ducked=true;
            if(ducked&&!SchoolChorus0259.selected()&&sound.getVolume()>.5f)restored=true;
          }
        }
      }else if(tick-observation.tick>40){
        if(chorus&&observation.active&&SchoolChorus0259.selected()){
          double elapsed=(System.nanoTime()-observation.wall)/1.e9,expected=durations.get(observation.asset);
          if(elapsed<expected-.6)throw new IllegalStateException("Supplied chorus ended early: "+elapsed+" / "+expected);
          naturalCompletions++;
          LogUtils.getLogger().info("CHAO_AUDIO02511_COMPLETE asset={} elapsedSeconds={} sourceSeconds={} naturalEnd=true",observation.asset,elapsed,expected);
        }
        iterator.remove();
      }
    }
    if(activeChorus>1)throw new IllegalStateException("Overlapping full classroom recordings");
    if(activeChorus==1)chorusActiveTicks++;
    if(chorusActiveTicks>20&&!mc.level.dimension().location().toString().equals("chao_garden:kindergarten")&&activeChorus==0)stoppedAfterExit=true;
  }
  public static boolean chorusObserved(){return chorusActiveTicks>10;}
  public static void complete(){
    if(!enabled())return;
    if(!areas.containsAll(Set.of("lobby","neutral","hero","dark","kindergarten"))||chorusStarts<2||naturalCompletions<1||chorusActiveTicks<360||!ducked||!restored||!stoppedAfterExit)
      throw new IllegalStateException("Incomplete supplied-audio lifecycle: areas="+areas+" starts="+chorusStarts+" completed="+naturalCompletions+" ticks="+chorusActiveTicks+" duck="+ducked+" restore="+restored+" exit="+stoppedAfterExit);
    LogUtils.getLogger().info("CHAO_AUDIO02511_PASS areas={} chorusStarts={} naturalCompletions={} activeTicks={} selectedChorusAssets={} noOverlap=true backgroundDucked=true backgroundRestored=true stoppedAfterExit=true suppliedADX=true audibleListeningClaim=false",areas,chorusStarts,naturalCompletions,chorusActiveTicks,chorusAssets);
  }
}
