package com.chaogarden.client;

import com.chaogarden.core.ChaoState;
import java.util.HashMap;
import java.util.Map;

/** Resolves the actual visible hand, without replacing the renderer or its layers. */
final class ChaoToyGrip {
  private record Hand(ChaoMeshAsset mesh,Map<String,ChaoMeshAsset.Pose> poses){}
  private final Map<String,Hand> hands=new HashMap<>();

  static int animalArm(ChaoState state) {
    int animal=state.animalParts[ChaoState.ARMS];
    return state.allowsAnimalParts()&&animal>=0&&(ChaoState.animalSlots(animal)&(1<<ChaoState.ARMS))!=0?animal:-1;
  }

  static float[] animalPalm(ChaoMeshAsset mesh,Map<String,ChaoMeshAsset.Pose> poses,int animal,boolean adult) {
    return animalPalm(mesh,poses,animal,adult,false);
  }
  static float[] animalPalm(ChaoMeshAsset mesh,Map<String,ChaoMeshAsset.Pose> poses,int animal,boolean adult,boolean left) {
    poses.values().forEach(ChaoMeshAsset.Pose::reset);
    var hand=poses.get("animal_0_"+animal+(left?"_left":"_right"));if(hand==null)return null;
    // This is the identical replacement-limb scale used by ChaoOverlayLayer.
    if(!adult&&animal<21)hand.sx=hand.sy=hand.sz=.92f;
    return mesh.animalPalm(animal,poses,left);
  }

  /** Visible terminal hand, including the separate Chaos ice layer. */
  float[] locateVisible(ChaoState state,OfficialChaoRig rig,boolean left) {
    if(!state.immortal||!"normal".equals(state.character)||!rig.geometryKey().startsWith("chaos_"))return locate(state,rig,left);
    String path="gameplay/chao_"+rig.geometryKey()+"_glass";
    Hand hand=hands.computeIfAbsent(path,p->{var mesh=ChaoMeshAsset.load(p);return new Hand(mesh,mesh.createPoses());});
    hand.poses.values().forEach(ChaoMeshAsset.Pose::reset);
    return hand.mesh.visiblePalm(hand.poses,left);
  }

  float[] locate(ChaoState state,OfficialChaoRig rig) {
    return locate(state,rig,false);
  }
  float[] locate(ChaoState state,OfficialChaoRig rig,boolean left) {
    int animal=animalArm(state);
    if(animal<0)return rig.toyPalm(left);
    String path=(animal<21?"overlays/":"cwe_parts/")+"chao_"+rig.sourceGeometryKey();
    Hand hand=hands.computeIfAbsent(path,p->{var mesh=ChaoMeshAsset.load(p);return new Hand(mesh,mesh.createPoses());});
    return animalPalm(hand.mesh,hand.poses,animal,state.adult,left);
  }
}
