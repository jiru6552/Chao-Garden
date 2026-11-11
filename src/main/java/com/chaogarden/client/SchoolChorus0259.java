package com.chaogarden.client;

import com.chaogarden.ChaoGarden;
import com.chaogarden.ChaoSounds;
import com.chaogarden.entity.ChaoEntity;
import com.chaogarden.kindergarten.KindergartenContent;
import com.chaogarden.kindergarten.KindergartenPupil0257;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.AABB;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.sound.PlaySoundEvent;

/** Optional full-length game chorus, one stream per classroom, without six-second overlap. */
@EventBusSubscriber(modid=ChaoGarden.MODID,value=Dist.CLIENT)
public final class SchoolChorus0259 {
  private static final ResourceLocation EVENT=ResourceLocation.fromNamespaceAndPath(ChaoGarden.MODID,"chao.classroom_chorus");
  private static final AABB ROOM=new AABB(-25,79,-22,-12,88,-8);
  private static Chorus current;
  private static Object previousLevel;
  private static int ticks,nextStart;
  private static boolean selected;
  private SchoolChorus0259(){}
  /** Reserve the classroom's music space through the gaps between full songs. */
  public static boolean selected(){return selected;}
  public static boolean available(Minecraft mc){
    var event=mc.getSoundManager().getSoundEvent(EVENT);
    return event!=null&&event.getWeight()>0;
  }
  @SubscribeEvent public static void sound(PlaySoundEvent event){
    var sound=event.getSound();
    if(!selected||sound==null)return;
    var id=sound.getLocation();
    if(id.getNamespace().equals(ChaoGarden.MODID)
        &&(id.getPath().equals("chao.singing_a")||id.getPath().equals("chao.singing_b")))event.setSound(null);
  }
  @SubscribeEvent public static void tick(ClientTickEvent.Post event){
    var mc=Minecraft.getInstance();ticks++;
    if(previousLevel!=mc.level){clear(mc);previousLevel=mc.level;}
    if(mc.level==null||mc.player==null){clear(mc);return;}
    if(mc.isPaused())return;
    boolean desired=KindergartenContent.inKindergarten(mc.level)&&available(mc)
        &&ROOM.inflate(5).contains(mc.player.position())
        &&!mc.level.getEntitiesOfClass(KindergartenPupil0257.class,ROOM,
            pupil->pupil.isAlive()&&pupil.getBehavior()==ChaoEntity.Behavior.SING
                &&pupil.behaviorDetail().equals("song")).isEmpty();
    if(!desired){clear(mc);return;}
    selected=true;
    if(current!=null&&ticks-current.started>40&&!mc.getSoundManager().isActive(current)){
      mc.getSoundManager().stop(current);current=null;nextStart=ticks+40;
    }
    if(current==null&&ticks>=nextStart){current=new Chorus(ticks);mc.getSoundManager().play(current);}
  }
  private static void clear(Minecraft mc){
    if(current!=null)mc.getSoundManager().stop(current);
    current=null;selected=false;nextStart=ticks;
  }
  private static final class Chorus extends AbstractTickableSoundInstance {
    final int started;
    Chorus(int started){
      super(ChaoSounds.CLASSROOM_CHORUS.get(),SoundSource.NEUTRAL,SoundInstance.createUnseededRandom());
      this.started=started;this.volume=.75f;this.pitch=1;
      this.x=-19;this.y=80.5;this.z=-17;
      this.looping=false;this.relative=false;this.attenuation=SoundInstance.Attenuation.LINEAR;
    }
    @Override public void tick(){}
  }
}
