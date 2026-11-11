package com.chaogarden.toys;

import com.chaogarden.entity.ChaoEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.*;
/** One eight-second phrase, shared by the real keyboard, Lionfish hands and operator preview. */
public final class PianoPlayRules {
  public static final String KIND="chao_piano";
  private static final int[] NOTES={0,4,7,4,2,5,9,5,4,7,11,7,2,5,7,0};
  private PianoPlayRules(){}
  public static boolean soundDue(int t){return t>=24&&t<=144&&(t-24)%8==0;}
  public static boolean left(int t){return ((t-24)/8&1)==0;}
  public static float pitch(int t){return (float)Math.pow(2,(NOTES[Math.clamp((t-24)/8,0,15)]-6)/12.0);}
  public static float presence(float t){return MaracasPlayRules.presence(t);}
  public static int frame(int t){return t>=24&&t<=146&&(t-24)%8<3?1:0;}
  public static void cue(ChaoEntity c,int t){
    if(!soundDue(t)||!(c.level() instanceof ServerLevel level))return;
    var pos=c.toyTarget();if(pos==null)return;
    level.playSound(null,pos.getX()+.5,pos.getY()+.21,pos.getZ()+.5,SoundEvents.NOTE_BLOCK_HARP.value(),SoundSource.NEUTRAL,.30f,pitch(t));
  }
}
