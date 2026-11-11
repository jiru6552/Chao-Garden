package com.chaogarden.toys;

import com.chaogarden.ChaoSounds;
import com.chaogarden.care.ChaoCareServices;
import com.chaogarden.entity.ChaoEntity;
import com.chaogarden.kindergarten.*;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;

/** Shared server/client presentation clock for learned music, classroom practice and placed toys. */
public final class InstrumentPerformance {
  private InstrumentPerformance(){}
  public static boolean supports(String lesson){return lesson.equals("maracas")||lesson.equals("bell")||lesson.equals("tambourine")||lesson.equals("castanets")||lesson.equals("cymbals")||lesson.equals("drum")||lesson.equals("flute")||lesson.equals("triangle")||lesson.equals("trumpet")||lesson.equals("guitar");}
  public static String kind(ChaoEntity c){
    String toy=c.toyPlayingKind();
    if(toy.equals(MaracasPlayRules.KIND)||toy.equals(BellPlayRules.KIND)||toy.equals(TambourinePlayRules.KIND)||toy.equals(CastanetsPlayRules.KIND)||toy.equals(CymbalsPlayRules.KIND)||toy.equals(DrumPlayRules.KIND)||toy.equals(FlutePlayRules.KIND)||toy.equals(TrianglePlayRules.KIND)||toy.equals(TrumpetPlayRules.KIND)||toy.equals(GuitarPlayRules.KIND))return toy;
    if(c.getBehavior()==ChaoEntity.Behavior.INSTRUMENT&&supports(c.behaviorDetail())
        &&(c instanceof KindergartenPupil0257||ChaoCareServices.knowsLesson(c,c.behaviorDetail())))return "chao_"+c.behaviorDetail();
    String lesson=c.state().trait("lesson.active");
    if(c.getBehavior()==ChaoEntity.Behavior.LEARN&&supports(lesson)&&ChaoCareServices.isLearning(c)
        &&KindergartenContent.inKindergarten(c.level())
        &&(c.level().isClientSide||KindergartenContent.isBoarded(c)))return "chao_"+lesson;
    return "";
  }
  public static boolean active(ChaoEntity c,String kind){
    if(!kind.equals(kind(c))||c.hurtTime>0||c.isCocooned()||c.officialComparison()||c.isCompanionGripping()
        ||c.isResting()||c.isEating()||c.isBeingPetted()||c.isHandlingItem()||c.isPassenger()
        ||!c.getHeldChaoItem().isEmpty()||!c.getMainHandItem().isEmpty()||!RattlePlayRules.handheld(c.state()))return false;
    if(c.toyPlayingTicks()>0)return c.getBehavior()==ChaoEntity.Behavior.NONE;
    return c.getBehavior()==ChaoEntity.Behavior.LEARN||!c.isStaying()&&c.getBehavior()==ChaoEntity.Behavior.INSTRUMENT;
  }
  public static float phase(ChaoEntity c,float partial){
    if(c.toyPlayingTicks()>0)return 160-c.toyPlayingTicks()+partial;
    float elapsed=c.getBehaviorElapsed(partial);
    // A joining musician shares the leader's ending/beat clock. Entry weight
    // below still gives that new player a twelve-tick raise into the current beat.
    if(c.getBehavior()==ChaoEntity.Behavior.INSTRUMENT&&c.getBehaviorDuration()<=160)
      return Math.max(0,Math.min(160,160-c.getBehaviorTicks()+partial));
    return elapsed%160;
  }
  public static float presence(ChaoEntity c,float partial){
    float weight=MaracasPlayRules.presence(phase(c,partial));
    if(c.toyPlayingTicks()>0)return weight;
    return Math.max(0,Math.min(weight,Math.min(c.getBehaviorElapsed(partial)/12,(c.getBehaviorTicks()-partial)/14)));
  }
  public static boolean due(String kind,int phase){return kind.equals(GuitarPlayRules.KIND)?GuitarPlayRules.soundDue(phase):kind.equals(MaracasPlayRules.KIND)?MaracasPlayRules.soundDue(phase):kind.equals(BellPlayRules.KIND)?BellPlayRules.soundDue(phase):kind.equals(TambourinePlayRules.KIND)?TambourinePlayRules.soundDue(phase):kind.equals(CastanetsPlayRules.KIND)?CastanetsPlayRules.soundDue(phase):kind.equals(CymbalsPlayRules.KIND)?CymbalsPlayRules.soundDue(phase):kind.equals(DrumPlayRules.KIND)?DrumPlayRules.soundDue(phase):kind.equals(FlutePlayRules.KIND)?FlutePlayRules.soundDue(phase):kind.equals(TrianglePlayRules.KIND)?TrianglePlayRules.soundDue(phase):kind.equals(TrumpetPlayRules.KIND)&&TrumpetPlayRules.soundDue(phase);}
  public static void cue(ChaoEntity c,String kind,int phase,float volumeScale){
    if(!(c.level() instanceof ServerLevel level))return;
    boolean maracas=kind.equals(MaracasPlayRules.KIND);
    level.playSound(null,c.getX(),c.getY()+c.getBbHeight()*.55,c.getZ(),
        kind.equals(GuitarPlayRules.KIND)?ChaoSounds.GUITAR.get():kind.equals(TrianglePlayRules.KIND)?net.minecraft.sounds.SoundEvents.NOTE_BLOCK_BELL.value():kind.equals(TrumpetPlayRules.KIND)?net.minecraft.sounds.SoundEvents.NOTE_BLOCK_DIDGERIDOO.value():kind.equals(FlutePlayRules.KIND)?net.minecraft.sounds.SoundEvents.NOTE_BLOCK_FLUTE.value():kind.equals(DrumPlayRules.KIND)?net.minecraft.sounds.SoundEvents.NOTE_BLOCK_BASEDRUM.value():maracas?ChaoSounds.MARACAS.get():kind.equals(TambourinePlayRules.KIND)?ChaoSounds.TAMBOURINE.get():kind.equals(CastanetsPlayRules.KIND)?ChaoSounds.CASTANETS.get():kind.equals(CymbalsPlayRules.KIND)?ChaoSounds.CYMBALS.get():ChaoSounds.HAND_BELL.get(),SoundSource.NEUTRAL,
        volume(kind)*volumeScale,kind.equals(GuitarPlayRules.KIND)?GuitarPlayRules.pitch(phase):maracas?((phase-20)/4)%2==0?1.02f:.98f:kind.equals(TrumpetPlayRules.KIND)?TrumpetPlayRules.pitch(phase):kind.equals(FlutePlayRules.KIND)?FlutePlayRules.pitch(phase):kind.equals(DrumPlayRules.KIND)?.9f:1f);
  }
  /** Modest mixer gain only; source samples, pitch, cadence and classroom attenuation stay intact. */
  public static float volume(String kind){
    return kind.equals(GuitarPlayRules.KIND)?.24f:kind.equals(CymbalsPlayRules.KIND)?.36f:kind.equals(DrumPlayRules.KIND)?.28f
        :kind.equals(MaracasPlayRules.KIND)?.30f:kind.equals(TrumpetPlayRules.KIND)?.20f
        :kind.equals(FlutePlayRules.KIND)?.18f:.24f;
  }
  /** Called by the real behavior goal or classroom owner, once per loaded server tick. */
  public static void tick(ChaoEntity c){
    if(!(c.level() instanceof ServerLevel level)||c.toyPlayingTicks()>0)return;
    String kind=kind(c);if(kind.isEmpty()||!active(c,kind)||c.getBehaviorElapsed(0)<12||c.getBehaviorTicks()<=14)return;
    int phase=(int)phase(c,0);if(!due(kind,phase)||!c.claimInstrumentCue((int)c.getBehaviorElapsed(0)))return;
    boolean school=c instanceof KindergartenPupil0257||c.getBehavior()==ChaoEntity.Behavior.LEARN;
    cue(c,kind,phase,school?.55f:1f);
    if(!kind.equals(MaracasPlayRules.KIND)||(phase-20)%16==0)
      level.sendParticles(ParticleTypes.NOTE,c.getX(),c.getY()+.8,c.getZ(),1,0,0,0,0);
  }
}
