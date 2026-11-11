package com.chaogarden.toys;

import com.chaogarden.ChaoSounds;
import com.chaogarden.entity.ChaoEntity;
import java.util.List;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;

/** The real reserved session and operator preview use one clock and presentation. */
public final class GardenToyPerformance049 {
  public static final List<String> KINDS=List.of("chao_toy_car","chao_sonic_doll","chao_shadow_doll",
      "chao_broom","chao_picture_book","chao_pogo_stick","chao_crayons");
  private GardenToyPerformance049(){}
  public static boolean supports(String kind){return KINDS.contains(kind);}
  public static String kind(ChaoEntity c){return supports(c.toyPlayingKind())?c.toyPlayingKind():"";}
  public static boolean active(ChaoEntity c,String kind){
    return supports(kind)&&kind.equals(c.toyPlayingKind())&&c.isAlive()&&c.hurtTime<=0
        &&!c.isCocooned()&&!c.isCompanionGripping()&&!c.officialComparison()&&!c.isResting()
        &&!c.isEating()&&!c.isBeingPetted()&&!c.isHandlingItem()&&!c.isPassenger()
        &&c.getHeldChaoItem().isEmpty()&&c.getMainHandItem().isEmpty()
        &&c.getBehavior()==ChaoEntity.Behavior.NONE&&RattlePlayRules.handheld(c.state());
  }
  public static float phase(ChaoEntity c,float partial){return 160-c.toyPlayingTicks()+partial;}
  public static float presence(ChaoEntity c,float partial){return MaracasPlayRules.presence(phase(c,partial));}
  public static void cue(ChaoEntity c,String kind){
    if(!(c.level() instanceof ServerLevel level))return;
    var sound=switch(kind){
      case "chao_toy_car"->ChaoSounds.TOY_CAR;
      case "chao_broom"->ChaoSounds.TOY_BROOM;
      case "chao_pogo_stick"->ChaoSounds.TOY_POGO;
      default->null;
    };
    if(sound!=null)level.playSound(null,c.getX(),c.getY()+.25,c.getZ(),sound.get(),SoundSource.NEUTRAL,
        kind.equals("chao_pogo_stick")?.22f:.18f,1f);
  }
}
