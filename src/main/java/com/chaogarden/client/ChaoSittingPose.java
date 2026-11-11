package com.chaogarden.client;

import com.mojang.blaze3d.vertex.PoseStack;
import java.util.List;
import java.util.Map;
import org.joml.Vector3f;

/** A shallow toe lift fitted to this form's deformed feet and the floor plane. */
final class ChaoSittingPose {
  static final List<String> FEET=List.of("foot_left","foot_right","feet_bone_right","foot_left2","foot_right2");
  static void apply(ChaoMeshAsset mesh,Map<String,ChaoMeshAsset.Pose> poses,boolean adult){
    var body=poses.get("body");if(body==null)return;
    for(String name:FEET){
      var foot=poses.get(name);if(foot==null)continue;
      // Compensate the seated torso lean; long adult toes should remain low.
      foot.rx=-(adult?.065f:.13f)-body.rx;
      foot.ry*=.3f;foot.rz=0;foot.z-=.5f;
      ground(mesh,poses,name);
    }
  }
  static void ground(ChaoMeshAsset mesh,Map<String,ChaoMeshAsset.Pose> poses,String name){
    var bounds=mesh.partBounds(name,poses);if(bounds==null)return;
    var footStack=new PoseStack();mesh.transformBone(name,footStack,poses);
    float bottom=Float.NEGATIVE_INFINITY;
    for(int corner=0;corner<8;corner++){
      var p=new Vector3f(bounds[(corner&1)==0?0:3],bounds[(corner&2)==0?1:4],bounds[(corner&4)==0?2:5]).div(16);
      bottom=Math.max(bottom,footStack.last().pose().transformPosition(p).y);
    }
    var torso=new PoseStack();mesh.transformBone("body",torso,poses);
    float vertical=torso.last().pose().transformDirection(new Vector3f(0,1,0)).y;
    if(Math.abs(vertical)>.2f)poses.get(name).y+=(24f/16-.025f/16-bottom)*16/vertical;
  }
}
