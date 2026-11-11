package com.chaogarden.client;

import com.chaogarden.entity.ChaoEntity;
import net.minecraft.util.Mth;

/** Authored gestures layered on the supplied skeleton, never on comparison mode. */
final class ChaoBehaviorPoses {
  private ChaoBehaviorPoses() {}
  private static void turn(OfficialChaoRig rig,String name,float x,float y,float z) {
    var p=rig.pose(name);if(p==null&&name.equals("foot_right"))p=rig.pose("feet_bone_right");if(p!=null){p.rx+=x;p.ry+=y;p.rz+=z;}
  }
  private static void move(OfficialChaoRig rig,String name,float x,float y,float z) {
    var p=rig.pose(name);if(p!=null){p.x+=x;p.y+=y;p.z+=z;}
  }
  private static void hands(OfficialChaoRig r,float lift,float open) {
    turn(r,"arm_left",-lift,.25f,open);turn(r,"arm_right",-lift,-.25f,-open);
  }
  static boolean seated(ChaoEntity.Behavior action) {
    return switch(action) {
      case CRY,COWER,GROGGY,STOMACH_ACHE,SANDCASTLE,DRAW,LEARN,RELAX,HATCH -> true;
      default -> false;
    };
  }
  /** Returns the authored mouth expression, with zero retaining a quiet face. */
  static int apply(OfficialChaoRig r,ChaoEntity.Behavior action,String detail,float progress,float age) {
    float slow=Mth.sin(age*.16f),quick=Mth.sin(age*.55f),beat=Mth.sin(age*.30f);
    switch(action) {
      case NONE -> {return 0;}
      case HATCH -> {
        // The provided camera returns to a seated newborn looking around.
        // The unseen shell break is not embellished with an invented leap.
        float settle=Mth.sin(progress*Mth.PI);
        turn(r,"skull",-.025f,Mth.sin(progress*Mth.PI*2)*.18f,settle*.025f);
        hands(r,.025f,.07f);return 0;
      }
      case BIRTHDAY -> {hands(r,.25f,.45f);turn(r,"skull",-.04f,slow*.08f,beat*.04f);return 1;}
      case PARTY -> {hands(r,.45f,.25f+Math.max(0,beat)*.3f);turn(r,"skull",-.04f,0,slow*.07f);return 1;}
      case RELAX -> {turn(r,"skull",.08f,0,slow*.035f);hands(r,.03f,.08f);return 1;}
      case GREET -> {turn(r,"arm_left",-.85f,0,1.05f+quick*.25f);turn(r,"skull",0,0,-.08f);return 1;}
      case NUZZLE -> {turn(r,"skull",.20f,slow*.12f,-.18f);hands(r,.25f,.22f);return 1;}
      case CHEER -> {hands(r,.8f,1.0f+quick*.12f);move(r,"body",0,-Math.max(0,beat)*.35f,0);return 1;}
      case CRY -> {hands(r,.95f,.18f);turn(r,"skull",.19f,0,quick*.035f);move(r,"body",0,quick*.06f,0);return 2;}
      case COWER -> {hands(r,.9f,.25f);turn(r,"skull",.28f,0,quick*.06f);turn(r,"body",.12f,0,0);return 4;}
      case TANTRUM -> {hands(r,-.1f,.75f);turn(r,"foot_left",Math.max(0,quick)*-.55f,0,0);turn(r,"foot_right",Math.max(0,-quick)*-.55f,0,0);turn(r,"skull",-.12f,slow*.22f,0);return 3;}
      case DIZZY -> {turn(r,"skull",slow*.16f,beat*.35f,quick*.12f);turn(r,"body",0,0,slow*.12f);hands(r,.1f,.60f);return 5;}
      case GROGGY -> {turn(r,"skull",.28f+slow*.06f,0,-.12f);hands(r,.08f,.10f);return 5;}
      case COUGH -> {turn(r,"arm_right",-.65f,-.55f,-.2f);turn(r,"skull",.17f+Math.max(0,quick)*.14f,0,0);turn(r,"body",Math.max(0,quick)*.08f,0,0);return 5;}
      case SNEEZE -> {float sneeze=Math.max(0,Mth.sin(progress*Mth.PI*3));hands(r,.7f,.2f);turn(r,"skull",-.1f+sneeze*.4f,0,0);turn(r,"body",sneeze*.16f,0,0);return 4;}
      case SCRATCH -> {turn(r,"arm_left",-.75f,.7f,.4f+quick*.12f);turn(r,"skull",0,0,-.12f);return 5;}
      case HICCUP -> {float hiccup=Math.max(0,quick);move(r,"body",0,-hiccup*.16f,0);turn(r,"skull",-hiccup*.11f,0,0);hands(r,.25f,.18f);return 4;}
      case STOMACH_ACHE -> {hands(r,.45f,.12f);turn(r,"body",.20f,0,0);turn(r,"skull",.2f,0,slow*.035f);return 5;}
      case RUNNY_NOSE -> {turn(r,"arm_right",-.85f,-.5f,-.20f);turn(r,"skull",.08f,slow*.10f,0);return 5;}
      case TALK,GOSSIP -> {turn(r,"skull",-.04f,slow*.12f,beat*.045f);turn(r,"arm_left",-.15f,0,.35f+beat*.18f);turn(r,"arm_right",-.1f,0,-.25f+beat*.1f);return beat>0?6:1;}
      case HUG -> {hands(r,.30f,.35f);turn(r,"arm_left",0,.85f,0);turn(r,"arm_right",0,-.85f,0);turn(r,"skull",.07f,0,-.10f);return 1;}
      case HOLD_HANDS -> {turn(r,"arm_left",-.2f,0,.7f);return 1;}
      case WATER -> {hands(r,.55f,.18f);turn(r,"arm_right",-.2f+slow*.08f,0,-.18f);turn(r,"skull",.18f,0,0);return 1;}
      case SHAKE_TREE -> {hands(r,.55f,.22f);turn(r,"arm_left",quick*.15f,.5f,0);turn(r,"arm_right",quick*.15f,-.5f,0);turn(r,"body",quick*.045f,0,0);return 0;}
      case SANDCASTLE -> {hands(r,.35f,.2f);turn(r,"arm_left",beat*.3f,.3f,0);turn(r,"arm_right",-beat*.3f,-.3f,0);turn(r,"skull",.23f,0,0);return 0;}
      case DANCE -> {
        if(detail.equals("spin_dance")){turn(r,"body",0,(age*.16f)%(Mth.PI*2),0);hands(r,.15f,.8f);}
        else if(detail.equals("gogo_dance")){hands(r,.45f,.8f);turn(r,"arm_left",beat*.5f,0,0);turn(r,"arm_right",-beat*.5f,0,0);turn(r,"body",0,0,slow*.13f);}
        else if(detail.equals("step_dance")||detail.equals("exercise")){turn(r,"foot_left",beat*.55f,0,0);turn(r,"foot_right",-beat*.55f,0,0);hands(r,.2f,.4f);}
        else{turn(r,"body",0,beat*.16f,beat*.10f);hands(r,.2f,.45f);}
        return 1;
      }
      case LEARN -> {turn(r,"skull",.08f,slow*.06f,0);hands(r,.25f,.16f);return 0;}
      case FLEE -> {turn(r,"skull",-.1f,slow*.12f,0);return 4;}
      case BULLY -> {turn(r,"arm_right",-.25f-Math.max(0,quick)*.45f,-.4f,-.2f);turn(r,"skull",-.08f,.12f,0);return 3;}
      case INTERVENE -> {hands(r,.18f,.75f);turn(r,"skull",-.04f,0,0);return 3;}
      case BREAK_DANCE -> {turn(r,"body",.25f,(age*.22f)%(Mth.PI*2),.7f);hands(r,.2f,.95f);turn(r,"foot_left",-.7f,0,.35f);turn(r,"foot_right",.3f,0,-.35f);return 1;}
      case DRAW -> {turn(r,"skull",.30f,0,0);turn(r,"arm_right",-.3f,-.35f,beat*.2f);return 0;}
      case SING -> {hands(r,.15f,.3f);turn(r,"skull",-.15f,0,slow*.06f);return 6;}
      case INSTRUMENT -> {
        float lift=switch(detail){case "flute","trumpet"->.85f;case "guitar","accordion"->.4f;default->.55f;};
        hands(r,lift,.3f);turn(r,"arm_left",beat*.13f,.65f,0);turn(r,"arm_right",-beat*.13f,-.65f,0);
        turn(r,"skull",detail.equals("flute")||detail.equals("trumpet")?-.08f:.10f,0,slow*.04f);return 1;
      }
    }
    return 0;
  }
}
