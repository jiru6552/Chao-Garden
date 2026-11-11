package com.chaogarden.kindergarten;

import com.chaogarden.ChaoSounds;
import com.chaogarden.core.SchoolSingingRules0258;
import com.chaogarden.entity.ChaoEntity;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;

/** Localized vocal phrases are emitted by the two real song-lesson pupils. */
public final class SchoolSinging0258 {
  private static final java.util.Map<KindergartenPupil0257,Long> LAST_TICK=new java.util.WeakHashMap<>();
  private SchoolSinging0258(){}
  public static void tick(KindergartenPupil0257 pupil,String lesson){
    if(!(pupil.level() instanceof ServerLevel level)||!KindergartenContent.inKindergarten(level)
        ||!lesson.equals("song")||pupil.getBehavior()!=ChaoEntity.Behavior.SING
        ||!pupil.behaviorDetail().equals("song")||!KindergartenPupils0257.withinHome(pupil))return;
    long now=level.getGameTime();
    Long previous=LAST_TICK.put(pupil,now);if(previous!=null&&previous==now)return;
    int phrase=SchoolSingingRules0258.phraseAt(pupil.schoolSlot(),now);
    if(phrase>=0)pupil.playSound(phrase==0?ChaoSounds.SINGING_A.get():ChaoSounds.SINGING_B.get(),.75f,1);
    if(SchoolSingingRules0258.noteAt(pupil.schoolSlot(),now))
      level.sendParticles(ParticleTypes.NOTE,pupil.getX(),pupil.getY()+.8,pupil.getZ(),1,0,0,0,0);
  }
}
