package com.chaogarden.client;

import java.util.UUID;

/** Native rig contracts in addition to the visible real-action frame sequences. */
final class Baby025VisualSmokePoseProbe {
  private Baby025VisualSmokePoseProbe(){}
  private static void require(boolean condition,String message){if(!condition)throw new IllegalStateException("Baby025 pose: "+message);}
  static int inspect() {
    int count=LocomotionVisualSmokePoseProbe.inspect();
    var rig=new OfficialChaoRig();var pet=new UUID(25025,1);float prior=Float.NaN;
    for(float age=80;age<140;age+=.25f) {
      rig.animateGameplay("baby",pet,OfficialChaoRig.Motion.PET,age,0,0,3,(age-80)/20,0,0,false);
      var arm=rig.pose("arm_left");var foot=rig.pose("foot_left");var skull=rig.pose("skull");
      require(rig.finite(),"Pet frame is finite");
      require(Math.abs(arm.rx)<.12f&&Math.abs(arm.ry)<.12f,"Petting retained celebration arm flailing");
      require(Math.abs(arm.rz-.95f)<.001f,"Quiet petting hands must rest below the wide head, not in a T pose");
      require(Math.abs(foot.rx+.65f)<.001f,"Petting feet leave their planted seated pose");
      require(Math.abs(skull.rz)<.04f,"Petting head sway is too broad");
      if(Float.isFinite(prior))require(Math.abs(skull.rx-prior)<.005f,"Fractional pet frame jumps");
      prior=skull.rx;count++;
    }
    var held=new UUID(25025,2);float min=Float.MAX_VALUE,max=-Float.MAX_VALUE;
    for(int age=0;age<=80;age++) {
      rig.animateGameplay("baby",held,OfficialChaoRig.Motion.HELD,age,0,0,0,0,0,0,false);
      var foot=rig.pose("foot_left");min=Math.min(min,foot.rx);max=Math.max(max,foot.rx);
      require(foot.rx<-.50f&&foot.rx>-.66f,"Suspended feet must stay softly tucked");
      require(rig.finite(),"Held frame is finite");count++;
    }
    require(max-min>.10f,"Held child lacks its gentle independent kick cycle");
    var water=new UUID(25025,3);
    for(int age=0;age<80;age++) {
      rig.animateGameplay("baby",water,OfficialChaoRig.Motion.FLAIL,age,0,0,0,0,0,0,false);
      require(Math.abs(rig.pose("body").rx)<.11f,"Untrained water reaction must remain upright");
      require(Math.abs(rig.pose("arm_left").rx-rig.pose("arm_right").rx)<1,
          "Water scoop unexpectedly snaps outside its bounded range");
      require(rig.finite(),"Water frame is finite");count++;
    }
    // Hiding native panels must preserve their landmarks and suppress both
    // the authored white eye box and the iris, rather than double the eyes.
    var id=new UUID(25025,4);
    rig.animateGameplay("baby",id,OfficialChaoRig.Motion.SIT,100,0,0,0,0,0,0,false);
    float[] eyes=rig.attachmentEyes();require(eyes!=null&&eyes[3]>0,"Native eye landmarks missing");
    rig.eyesVisible(false);float[] hidden=rig.attachmentEyes();
    require(java.util.Arrays.equals(eyes,hidden),"Closed eye fit changed after hiding the open panels");
    require(!rig.pose("skull__cube_2").visible&&!rig.pose("skull__cube_3").visible,
        "Closed eyes retain overlapping open native panels");
    count+=3;return count;
  }
}
