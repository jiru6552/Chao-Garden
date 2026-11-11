package com.chaogarden.toys;

import com.chaogarden.ChaoSounds;
import com.chaogarden.entity.ChaoEntity;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.IntPredicate;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;

/** One catalog and cue implementation for real placed play and operator previews. */
public final class HandToyEffects {
  public record Clip(String id, String kind, IntPredicate due, BiConsumer<ChaoEntity,Integer> cue) {}
  private static final List<Clip> CLIPS=List.of(
      new Clip("rocking_horse",RockingHorsePlayRules.KIND,RockingHorsePlayRules::soundDue,RockingHorsePlayRules::cue),
      new Clip("toy_car","chao_toy_car",t->ActiveToyPlayRules049.soundDue("chao_toy_car",t),(c,t)->GardenToyPerformance049.cue(c,"chao_toy_car")),
      new Clip("sonic_doll","chao_sonic_doll",t->ActiveToyPlayRules049.soundDue("chao_sonic_doll",t),(c,t)->GardenToyPerformance049.cue(c,"chao_sonic_doll")),
      new Clip("shadow_doll","chao_shadow_doll",t->ActiveToyPlayRules049.soundDue("chao_shadow_doll",t),(c,t)->GardenToyPerformance049.cue(c,"chao_shadow_doll")),
      new Clip("pogo_stick","chao_pogo_stick",t->ActiveToyPlayRules049.soundDue("chao_pogo_stick",t),(c,t)->GardenToyPerformance049.cue(c,"chao_pogo_stick")),
      new Clip("broom","chao_broom",t->QuietToyPlayRules049.soundDue("chao_broom",t),(c,t)->GardenToyPerformance049.cue(c,"chao_broom")),
      new Clip("picture_book","chao_picture_book",t->QuietToyPlayRules049.soundDue("chao_picture_book",t),(c,t)->GardenToyPerformance049.cue(c,"chao_picture_book")),
      new Clip("crayons","chao_crayons",t->QuietToyPlayRules049.soundDue("chao_crayons",t),(c,t)->GardenToyPerformance049.cue(c,"chao_crayons")),
      new Clip("rattle",RattlePlayRules.KIND,RattlePlayRules::soundDue,(c,t)->sound(c,ChaoSounds.RATTLE.get(),.35f,1)),
      new Clip("bubble_wand",BubblePlayRules.KIND,BubblePlayRules::puffDue,(c,t)->{
        ToyCueNetwork.bubble(c,t);
        if((t-30)%48==0){var mouth=ToyContent.bubbleOrigin(c);((ServerLevel)c.level()).playSound(null,mouth.x,mouth.y,mouth.z,SoundEvents.BUBBLE_COLUMN_BUBBLE_POP,SoundSource.NEUTRAL,.12f,1.4f);}
      }),
      new Clip("maracas",MaracasPlayRules.KIND,MaracasPlayRules::soundDue,(c,t)->InstrumentPerformance.cue(c,MaracasPlayRules.KIND,t,1)),
      new Clip("bell",BellPlayRules.KIND,BellPlayRules::soundDue,(c,t)->InstrumentPerformance.cue(c,BellPlayRules.KIND,t,1)),
      new Clip("tambourine",TambourinePlayRules.KIND,TambourinePlayRules::soundDue,(c,t)->InstrumentPerformance.cue(c,TambourinePlayRules.KIND,t,1)),
      new Clip("triangle",TrianglePlayRules.KIND,TrianglePlayRules::soundDue,(c,t)->InstrumentPerformance.cue(c,TrianglePlayRules.KIND,t,1)),
      new Clip("trumpet",TrumpetPlayRules.KIND,TrumpetPlayRules::soundDue,(c,t)->InstrumentPerformance.cue(c,TrumpetPlayRules.KIND,t,1)),
      new Clip("accordion",AccordionPlayRules.KIND,AccordionPlayRules::soundDue,(c,t)->InstrumentPerformance.cue(c,AccordionPlayRules.KIND,t,1)),
      new Clip("guitar",GuitarPlayRules.KIND,GuitarPlayRules::soundDue,(c,t)->InstrumentPerformance.cue(c,GuitarPlayRules.KIND,t,1)),
      new Clip("flute",FlutePlayRules.KIND,FlutePlayRules::soundDue,(c,t)->InstrumentPerformance.cue(c,FlutePlayRules.KIND,t,1)),
      new Clip("drum",DrumPlayRules.KIND,DrumPlayRules::soundDue,(c,t)->InstrumentPerformance.cue(c,DrumPlayRules.KIND,t,1)),
      new Clip("cymbals",CymbalsPlayRules.KIND,CymbalsPlayRules::soundDue,(c,t)->InstrumentPerformance.cue(c,CymbalsPlayRules.KIND,t,1)),
      new Clip("castanets",CastanetsPlayRules.KIND,CastanetsPlayRules::soundDue,(c,t)->InstrumentPerformance.cue(c,CastanetsPlayRules.KIND,t,1)));
  private HandToyEffects() {}
  public static List<Clip> clips(){return CLIPS;}
  public static Clip find(String id){return CLIPS.stream().filter(c->c.id().equals(id)).findFirst().orElse(null);}
  public static void tick(ChaoEntity c,String kind,int elapsed){
    if(!(c.level() instanceof ServerLevel))return;
    CLIPS.stream().filter(clip->clip.kind().equals(kind)&&clip.due().test(elapsed)).findFirst().ifPresent(clip->clip.cue().accept(c,elapsed));
  }
  private static void sound(ChaoEntity c,net.minecraft.sounds.SoundEvent sound,float volume,float pitch){
    ((ServerLevel)c.level()).playSound(null,c.getX(),c.getY()+c.getBbHeight()*.55,c.getZ(),sound,SoundSource.NEUTRAL,volume,pitch);
  }
}
