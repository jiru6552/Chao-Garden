package com.chaogarden.toys;

import com.chaogarden.entity.ChaoEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;

/** One shared ride clock and the measured SA2 horse's ground, saddle and grip frame. */
public final class RockingHorsePlayRules {
  public static final String KIND="chao_rocking_horse";
  public static final float PIVOT_Y=.005f, RIDER_Y=.35f, SEAT_Y=.4283479f, SEAT_Z=-.10f;
  public static final float GRIP_X=.13f,GRIP_Y=.6377f,GRIP_Z=.14625f;
  public static final float MAX_ANGLE=(float)Math.toRadians(6);
  private RockingHorsePlayRules(){}
  public static float key(int tick){return (float)Math.sin((tick-16)*Math.PI/16);}
  private static float smooth(float x){x=Math.clamp(x,0,1);return x*x*(3-2*x);}
  public static float rockWeight(float tick){return smooth((tick-16)/8)*smooth((144-tick)/8);}
  public static float occupancy(float tick){return (float)ToyInteractionRules.mountOccupancy(tick);}
  /** The same four-tick Catmull-Rom samples used by the actual Lionfish clip. */
  public static float angle(float tick){
    if(!Float.isFinite(tick))return 0;
    float time=Math.round(Math.clamp(tick,0,160)*50)/200f;int frame=Math.min(39,(int)time);float u=time-frame;
    float a=key(Math.max(0,frame-1)*4),b=key(frame*4),c=key((frame+1)*4),d=key(Math.min(40,frame+2)*4);
    float wave=.5f*(2*b+(c-a)*u+(2*a-5*b+4*c-d)*u*u+(-a+3*b-3*c+d)*u*u*u);
    return wave*MAX_ANGLE*rockWeight(tick);
  }
  public static float pointY(float y,float z,float tick){float a=angle(tick);return PIVOT_Y+(y-PIVOT_Y)*(float)Math.cos(a)-z*(float)Math.sin(a);}
  public static float pointZ(float y,float z,float tick){float a=angle(tick);return (y-PIVOT_Y)*(float)Math.sin(a)+z*(float)Math.cos(a);}
  public static double riderY(double tick,float scale){return riderY((float)tick);}
  public static double riderZ(double tick,float scale){return riderZ((float)tick);}
  public static float riderY(float tick){return pointY(RIDER_Y,SEAT_Z,tick);}
  public static float riderZ(float tick){return pointZ(RIDER_Y,SEAT_Z,tick);}
  public static float seatY(float tick){return pointY(SEAT_Y,SEAT_Z,tick);}
  public static float seatZ(float tick){return pointZ(SEAT_Y,SEAT_Z,tick);}
  public static boolean happy(float tick){return tick>=12&&tick<148;}
  public static boolean soundDue(int tick){return tick>=24&&tick<=136&&(tick-24)%16==0;}
  public static void cue(ChaoEntity chao,int tick){
    if(!soundDue(tick)||!(chao.level() instanceof ServerLevel level)||chao.toyTarget()==null)return;
    level.playSound(null,chao.toyTarget(),SoundEvents.WOOD_STEP,SoundSource.NEUTRAL,.16f,((tick-24)/16&1)==0?.83f:.90f);
  }
}
