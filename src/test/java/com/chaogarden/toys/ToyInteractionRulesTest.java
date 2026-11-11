package com.chaogarden.toys;

public final class ToyInteractionRulesTest {
  private static int checks;
  private static void check(boolean value,String message){checks++;if(!value)throw new AssertionError(message);}
  public static void main(String[] args) {
    for(String kind:new String[]{"chao_piano","chao_organ"}) {
      check(ToyInteractionRules.frontOnly(kind),"Keyboard must be approached from its keys");
      for(double width:new double[]{.35,.55,.8}) {
        double distance=ToyInteractionRules.keyboardDistance(kind,width);
        double clearance=kind.equals("chao_organ")?.015:.38125+.025;
        check(Math.abs(distance-width/2-clearance)<1e-9,"Collision-safe keyboard clearance");
        if(kind.equals("chao_organ"))check(distance<width/2+.04,"Organ uses under-manual clearance instead of extending arms across the old cabinet gap");
      }
    }
    check(ToyInteractionRules.frontOnly("chao_tv"),"TV remains directional");
    check(!ToyInteractionRules.frontOnly("chao_ball"),"Ball may be approached from any side");
    int openingStarts=0,previous=0;
    for(int tick=0;tick<=160;tick++) {
      int frame=ToyInteractionRules.coffinFrame(tick);
      double occupancy=ToyInteractionRules.coffinOccupancy(tick);
      check(frame>=0&&frame<=3,"Valid blockstate frame");
      check(occupancy>=0&&occupancy<=1,"Bounded occupancy");
      if(frame==1&&previous==0)openingStarts++;
      if(occupancy>0&&occupancy<1)check(frame==3,"Enter and exit only through fully open lid");
      if(tick>=40&&tick<116)check(frame==3&&occupancy==1,"Quiet open rest, not repeated flutter");
      if(tick>=144)check(occupancy==0,"Chao exits before lid closes");
      if(tick>0)check(Math.abs(occupancy-ToyInteractionRules.coffinOccupancy(tick-1))<.13,"No mount teleport");
      previous=frame;
    }
    check(openingStarts==1,"Exactly one opening, held until the actor exits");
    check(ToyInteractionRules.coffinFrame(-1)==0&&ToyInteractionRules.coffinFrame(200)==0,"Inactive lid closed");
    check(ToyInteractionRules.coffinOccupancy(0)==0&&ToyInteractionRules.coffinOccupancy(160)==0,"Safe adjacent endpoints");
    for(double width:new double[]{.35,.44,.55,.8}) {
      double contact=ToyInteractionRules.ballContactDistance(width);
      check(ToyInteractionRules.ballWithinReach(contact*contact,0,width),"Contact anchor is kickable");
      check(!ToyInteractionRules.ballWithinReach(1.7*1.7,0,width),"No remote ball kicks");
      check(!ToyInteractionRules.ballWithinReach(contact*contact,.5,width),"No kicking a ball on another level");
    }
    check(ToyInteractionRules.ballKickSwing(0)==0&&ToyInteractionRules.ballKickSwing(8)==0,"Kick has quiet endpoints");
    check(Math.abs(ToyInteractionRules.ballKickSwing(ToyInteractionRules.BALL_CONTACT_TICK)-1)<1e-9,"Contact at maximum foot extension");
    check(ToyInteractionRules.movingBallPlayer("chao_ball",-1),"Ball pursuit retains gait");
    check(!ToyInteractionRules.movingBallPlayer("chao_ball",4),"Kick pauses locomotion");
    check(!ToyInteractionRules.movingBallPlayer("chao_tv",-1),"Other toys stay seated");
    check(ToyInteractionRules.supportedFloat("chao_lifesaver"),"Ring overrides unsupported swim/flail");
    check(!ToyInteractionRules.supportedFloat("chao_rubber_duck"),"Duck does not support the actor");
    for(int tick=0;tick<160;tick++) {
      int frame=ToyInteractionRules.mountedFrame(tick);
      check(frame==(tick/8)%4,"Shared prop/rider frame clock");
      check(Math.abs(ToyInteractionRules.horseAngle(frame))<=Math.toRadians(6),"Small horse rock");
      check(Math.abs(ToyInteractionRules.ringBob(frame))<=.025,"Gentle fixed-scale ring bob");
      double y=ToyInteractionRules.horseRiderY(frame)-.005,z=ToyInteractionRules.horseRiderZ(frame);
      check(Math.abs(y*y+z*z-((.35-.005)*(.35-.005)+.0625*.0625))<1e-9,"Rider follows rigid rocker pivot");
      double occupied=ToyInteractionRules.mountOccupancy(tick);
      check(occupied>=0&&occupied<=1,"Bounded mounted entry/exit");
      if(tick>0)check(Math.abs(occupied-ToyInteractionRules.mountOccupancy(tick-1))<.13,"No mount/dismount snap");
    }
    for(int step=0;step<=100;step++) {
      double u=step/100.0;
      check(ToyInteractionRules.ringEntryLift(u)>=-1e-9,"Entry arc never dips into shore");
      if(u<=.775)check(1+(.78-.025-1)*u+ToyInteractionRules.ringEntryLift(u)>=1-1e-9,"Full adult bounds clear shore before lowering into water");
    }
    check(ToyInteractionRules.mountOccupancy(0)==0&&ToyInteractionRules.mountOccupancy(160)==0,"Mounted adjacent endpoints");
    String[] stationary={"chao_trumpet","chao_drum","chao_jack_in_box","chao_radio","chao_tv",
        "chao_rubber_duck","chao_triangle","chao_guitar","chao_accordion","chao_bubble_wand",
        "chao_rattle","chao_maracas","chao_bell","chao_castanets","chao_cymbals","chao_flute","chao_tambourine","chao_shovel"};
    for(String kind:stationary) {
      check(ToyInteractionRules.stationary(kind),"Stationary catalog classification");
      for(double width:new double[]{.35,.44,.55,.8})for(double extent:new double[]{.12,.3125,.4375}) {
        double d=ToyInteractionRules.stationaryDistance(kind,extent,width);
        check(d+1e-9>=extent+width*.5+.06,"Contact stays outside collision envelope");
        if(ToyInteractionRules.viewing(kind))check(d>=1,"Viewing keeps adjacent-cell distance");
        else check(Math.abs(d-(extent+width*.5+.06))<1e-9,"Contact does not keep viewing gap");
      }
      check(ToyInteractionRules.stationaryWithinReach(kind,0),"Exact anchor valid");
      check(!ToyInteractionRules.stationaryWithinReach(kind,1),"Displaced stationary player invalid");
      check(ToyInteractionRules.stationaryLookHeight(kind)>0&&ToyInteractionRules.stationaryLookHeight(kind)<1,"Look toward toy, not air");
    }
    for(String kind:new String[]{"chao_ball","chao_piano","chao_organ","chao_coffin","chao_rocking_horse","chao_lifesaver"})
      check(!ToyInteractionRules.stationary(kind),"Dedicated interaction preserved");
    check(ToyInteractionRules.environmentValid("chao_shovel",false,false,true),"Sand enables shovel");
    check(!ToyInteractionRules.environmentValid("chao_shovel",false,false,false),"Sand loss invalidates shovel");
    check(ToyInteractionRules.environmentValid("chao_rubber_duck",false,false,false),"Dry duck supported");
    check(ToyInteractionRules.environmentValid("chao_rubber_duck",true,true,false),"Wet duck supported");
    check(!ToyInteractionRules.environmentValid("chao_rubber_duck",false,true,false),"Draining duck interrupts");
    check(!ToyInteractionRules.environmentValid("chao_rubber_duck",true,false,false),"Flooding duck interrupts");
    check(!ToyInteractionRules.environmentValid("chao_lifesaver",false,true,false),"Ring still requires water");
    int reveals=0,previousJack=0;
    for(int elapsed=-4;elapsed<=164;elapsed++) {
      int frame=ToyInteractionRules.jackFrame(elapsed);
      check(frame>=0&&frame<=3,"Jack frame remains valid");
      if(previousJack==0&&frame!=0)reveals++;
      if(elapsed<40||elapsed>=144)check(frame==0,"Jack closed before reveal and after retreat");
      if(elapsed>=44&&elapsed<128)check(frame==2,"Jack holds full reveal without loop");
      double reaction=ToyInteractionRules.jackReaction(elapsed);
      check(reaction>=0&&reaction<=1,"Jack bounded reaction");
      if(elapsed<=40||elapsed>=64)check(reaction==0,"No surprise before pop or repeated after reaction");
      check(ToyInteractionRules.jackReactionDue(elapsed)==(elapsed>=48),"Fear choice follows visible reveal");
      previousJack=frame;
    }
    check(reveals==1,"Exactly one jack reveal per session");
    check(ToyInteractionRules.jackFrame(40)==1&&ToyInteractionRules.jackFrame(128)==3,"Emergence and retreat use authored short pose");
    System.out.println("Toy interaction rules: "+checks+" checks passed");
  }
}
