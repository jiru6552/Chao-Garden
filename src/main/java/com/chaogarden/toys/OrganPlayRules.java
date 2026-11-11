package com.chaogarden.toys;

import com.chaogarden.ChaoSounds;
import com.chaogarden.entity.ChaoEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;

/** Eight sustained two-hand chords on the existing eight-second placed-toy clock. */
public final class OrganPlayRules {
  public static final String KIND="chao_organ";
  // Shipped idle OBJ, material sa2_piano01_20: x=.23512..79223, z=.39713..48796.
  // Contact the front quarter of the white keys, rather than reaching to the rear.
  public static final float KEY_Y=.18667f, KEY_FRONT=.085f, KEY_CENTER_X=.013675f;
  private static final int[] UPPER={7,9,12,11,9,7,4,7}, LOWER={0,2,5,4,2,0,-3,0};
  private OrganPlayRules(){}
  public static boolean soundDue(int t){return t>=24&&t<=136&&(t-24)%16==0;}
  public static float presence(float t){return MaracasPlayRules.presence(t);}
  public static int frame(int t){return t>=24&&t<148&&(t-24)%16<=10?1:0;}
  public static float pitch(int t,boolean left){
    int i=Math.clamp((t-24)/16,0,7);return (float)Math.pow(2,((left?LOWER:UPPER)[i]-6)/12.0);
  }
  /** Travel between keys only while the hands are lifted, then hold each chord. */
  public static float keyX(float t,boolean left){
    float phrase=Math.clamp((t-20)/16,0,7);int i=(int)phrase;
    float blend=Math.clamp((phrase-i)*4,0,1);blend=blend*blend*(3-2*blend);
    int previous=Math.max(0,i-1);int[] notes=left?LOWER:UPPER;
    float note=notes[previous]+(notes[i]-notes[previous])*blend;
    return KEY_CENTER_X+(left?.12f:-.12f)+.003f*(note-(left?0:7));
  }
  public static void cue(ChaoEntity c,int t){
    if(!soundDue(t)||!(c.level() instanceof ServerLevel level))return;
    var p=c.toyTarget();if(p==null)return;
    for(boolean left:new boolean[]{true,false})level.playSound(null,p.getX()+.5,p.getY()+KEY_Y,p.getZ()+.5,
        ChaoSounds.ORGAN.get(),SoundSource.NEUTRAL,left?.16f:.20f,pitch(t,left));
  }
}
