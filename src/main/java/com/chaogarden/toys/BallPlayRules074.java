package com.chaogarden.toys;

import com.chaogarden.core.ChaoState;
import com.chaogarden.core.LocomotionRules;

/** Authored ball contact/rolling timings, shared by real play and operator playback. */
public final class BallPlayRules074 {
  public static final String KIND="chao_ball";
  public static final int ACTION_TICKS=20, CONTACT_TICK=10, ROLL_TICKS=6, ACTION_INTERVAL=40;
  public static final int PRESENTATION_TICKS=ROLL_TICKS+4;
  public static final double RADIUS=3.0/16.0;
  /** Contains every rotated corner of the shipped faceted ball (maximum3.270 model pixels). */
  public static final double SWEEP_RADIUS=.21;
  private BallPlayRules074() {}

  /** Root placement is based on rendered contact, not the Chao's oversized rectangular hitbox. */
  public static boolean nudge(ChaoState state){return !state.adult||LocomotionRules.groundGait(state)==LocomotionRules.GroundGait.CRAWL;}
  public static double contactDistance(ChaoState state){return contactDistance(state,"");}
  public static double contactDistance(ChaoState state,String hat){
    double scale=state.immortal?.86:state.adult?.80:.64;
    if(!nudge(state))return .4375*scale;
    return .4*scale+(scale<=.70?RADIUS:.140625)+.0125+headPaddingPixels(hat)*scale/16;
  }
  public static double headPaddingPixels(String hat){return switch(hat){case "eggshell_hat"->1.15;case "vanilla_carved_pumpkin","vanilla_skeleton_skull"->.30;default->0;};}
  public static float actionPresence(float elapsed){
    if(!Float.isFinite(elapsed)||elapsed<=0||elapsed>=ACTION_TICKS)return 0;
    return smooth(Math.min(1,Math.min(elapsed/6f,(ACTION_TICKS-elapsed)/6f)));
  }
  public static double rollProgress(double elapsed){
    if(!Double.isFinite(elapsed))return 1;
    return Math.max(0,Math.min(1,elapsed/ROLL_TICKS));
  }
  /** One full authored turn parks on the same facets, avoiding a final geometry/color snap. */
  public static double rollAngle(double elapsed){return Math.PI*2*rollProgress(elapsed);}
  private static float smooth(float x){return x*x*(3-2*x);}
}
