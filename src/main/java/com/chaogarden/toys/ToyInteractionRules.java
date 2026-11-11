package com.chaogarden.toys;

/** Shared, deterministic contact and coffin choreography. Times are session ticks. */
public final class ToyInteractionRules {
  private ToyInteractionRules() {}
  public static final int SESSION_TICKS=160;
  public static int remainingTicks(String kind,long until,long now) {
    if(kind.isEmpty()||until<=0||until<=now)return 0;
    return (int)Math.min(SESSION_TICKS,until-now);
  }
  public static boolean keyboard(String kind) {
    return kind.equals("chao_piano")||kind.equals("chao_organ");
  }
  public static boolean frontOnly(String kind) {
    return keyboard(kind)||kind.equals("chao_tv")||kind.equals("chao_jack_in_box")||kind.equals("chao_coffin");
  }
  /** Distance from the toy center; cabinet front plus the actor's collision radius. */
  public static double keyboardDistance(String kind,double actorWidth) {
    // The organ's manual projects in front of its rear cabinet. Its broad
    // outline is not a wall: a Chao can sit in the low clearance below it.
    return kind.equals("chao_organ")?actorWidth*.5+.015:.38125+actorWidth*.5+.025;
  }
  /** Ground-placeable stationary play; dedicated keyboard/ball/mount controllers stay separate. */
  public static boolean stationary(String kind) {
    return !kind.isEmpty()&&!keyboard(kind)&&!kind.equals("chao_ball")
        &&!kind.equals("chao_rocking_horse")&&!kind.equals("chao_coffin")&&!kind.equals("chao_lifesaver");
  }
  public static boolean viewing(String kind) {
    return kind.equals("chao_tv")||kind.equals("chao_radio")
        ||kind.equals("chao_jack_in_box")||kind.equals("chao_rubber_duck");
  }
  public static double stationaryDistance(String kind,double collisionExtent,double actorWidth) {
    double contact=Math.max(0,collisionExtent)+actorWidth*.5+.06;
    return viewing(kind)?Math.max(1,contact):contact;
  }
  public static boolean stationaryWithinReach(String kind,double distanceSquared) {
    double tolerance=viewing(kind)?.25:.16;return distanceSquared<=tolerance*tolerance;
  }
  public static double stationaryLookHeight(String kind) {
    return switch(kind) {
      case "chao_tv","chao_jack_in_box"->.65;
      case "chao_rubber_duck","chao_triangle"->.45;
      case "chao_accordion"->.35;
      case "chao_drum","chao_bell","chao_castanets","chao_tambourine"->.30;
      case "chao_radio"->.28;
      case "chao_cymbals","chao_maracas"->.12;
      case "chao_shovel"->.15;
      default->.20;
    };
  }
  public static boolean environmentValid(String kind,boolean waterlogged,boolean selectedWaterlogged,boolean sand) {
    if(kind.equals("chao_shovel")&&!sand)return false;
    if(kind.equals("chao_lifesaver"))return waterlogged;
    return !kind.equals("chao_rubber_duck")||waterlogged==selectedWaterlogged;
  }
  /** Measured union of shipped idle/active geometry, in six model-unit bounds.
   * Stable envelopes avoid moving a contact anchor as a prop animates. */
  public static double[] collisionEnvelope(String kind) {
    return switch(kind) {
      case "chao_radio"->new double[]{2,0,5.75,14,5.63,10.25};
      case "chao_guitar"->new double[]{3,0,5.84,13,4.89,10.16};
      case "chao_triangle"->new double[]{4,0,6,12,14,10.6};
      case "chao_rubber_duck"->new double[]{1,0,3,16,11,13};
      case "chao_jack_in_box"->new double[]{3.8,0,3.8,14,16,12.5};
      case "chao_rattle"->new double[]{6.25,0,4.74,9.61,3.96,11.26};
      case "chao_drum"->new double[]{2.72,0,3,13.28,6.08,13};
      default->null;
    };
  }
  /** Match blockstate model rotation about the cell center, including asymmetric props. */
  public static double[] rotateEnvelope(double[] b,int quarterTurns) {
    return switch(Math.floorMod(quarterTurns,4)) {
      case 1->new double[]{16-b[5],b[1],b[0],16-b[2],b[4],b[3]};
      case 2->new double[]{16-b[3],b[1],16-b[5],16-b[0],b[4],16-b[2]};
      case 3->new double[]{b[2],b[1],16-b[3],b[5],b[4],16-b[0]};
      default->b.clone();
    };
  }
  public static final int JACK_POP_TICK=40;
  public static final int JACK_REACTION_TICK=48;
  /** One closed wind-up, emergence, held-open reveal, retreat and closure per session. */
  public static int jackFrame(int elapsed) {
    if(elapsed<JACK_POP_TICK||elapsed>=144)return 0;
    if(elapsed<44)return 1;
    return elapsed<128?2:3;
  }
  /** Brief reaction follows the visible pop, never the entity's unrelated age clock. */
  public static double jackReaction(double elapsed) {
    if(elapsed<JACK_POP_TICK||elapsed>=64)return 0;
    return Math.sin(Math.PI*(elapsed-JACK_POP_TICK)/24);
  }
  public static boolean jackReactionDue(int elapsed) { return elapsed>=JACK_REACTION_TICK; }
  public static int coffinFrame(int elapsed) {
    if(elapsed<0||elapsed>=SESSION_TICKS)return 0;
    if(elapsed<12)return elapsed/4;
    // This shallow source prop stays open while occupied; close only after exit.
    if(elapsed<144)return 3;
    if(elapsed<156)return 2-(elapsed-144)/4;
    return 0;
  }
  /** Only enter/leave while the lid is fully open. Smooth endpoints avoid snapping. */
  public static double coffinOccupancy(double elapsed) {
    if(elapsed<16||elapsed>=140)return 0;
    if(elapsed<28)return smooth((elapsed-16)/12);
    if(elapsed<128)return 1;
    return 1-smooth((elapsed-128)/12);
  }
  public static final int BALL_KICK_TICKS=10, BALL_CONTACT_TICK=4;
  public static double ballContactDistance(double actorWidth){return .25+actorWidth*.5+.06;}
  public static boolean ballWithinReach(double horizontalSquared,double verticalDelta,double actorWidth) {
    double reach=ballContactDistance(actorWidth)+.08;
    return horizontalSquared<=reach*reach&&Math.abs(verticalDelta)<.25;
  }
  public static double ballKickSwing(double elapsed) {
    return elapsed<0||elapsed>=8?0:Math.sin(elapsed*Math.PI/8);
  }
  public static boolean movingBallPlayer(String kind,int kickElapsed) {
    return kind.equals("chao_ball")&&kickElapsed<0;
  }
  public static boolean supportedFloat(String kind){return kind.equals("chao_lifesaver");}
  public static int mountedFrame(int elapsed){return Math.floorMod(Math.max(0,elapsed)/8,4);}
  public static double horseAngle(int frame){return switch(frame&3){case 1->Math.toRadians(6);case 3->Math.toRadians(-6);default->0;};}
  public static double ringBob(int frame){return switch(frame&3){case 1->.025;case 3->-.025;default->0;};}
  /** Common measured contact frame: horse ground pivot (.5,.005,.5), facing local +Z. */
  public static double horseRiderY(int frame) {
    double a=horseAngle(frame);return .005+(.35-.005)*Math.cos(a)-(-.0625)*Math.sin(a);
  }
  public static double horseRiderZ(int frame) {
    double a=horseAngle(frame);return (.35-.005)*Math.sin(a)+(-.0625)*Math.cos(a);
  }
  public static double ringEntryLift(double occupancy){return Math.sin(Math.PI*occupancy)*.35;}
  public static double mountOccupancy(double elapsed) {
    if(elapsed<=0||elapsed>=SESSION_TICKS)return 0;
    if(elapsed<12)return smooth(elapsed/12);
    if(elapsed<=148)return 1;
    return smooth((SESSION_TICKS-elapsed)/12);
  }
  private static double smooth(double x){return x*x*(3-2*x);}
}
