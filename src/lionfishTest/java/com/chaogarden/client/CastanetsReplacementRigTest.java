package com.chaogarden.client;
import com.chaogarden.core.ChaoState;
import com.chaogarden.toys.CastanetsPlayRules;
import com.google.gson.*;
import com.mojang.blaze3d.vertex.PoseStack;
import java.nio.file.*;
import java.util.*;
import org.joml.Vector3f;
/** Two-hand contact against the real replacement-hand mesh socket, both ages. */
final class CastanetsReplacementRigTest {
  static void run()throws Exception{
    int checks=0,cases=0;float maxGrip=0,maxContact=0;var clip=new LionfishCastanetsClip();
    Path root=Path.of("src/main/resources/assets/chao_garden/models/entity");
    for(String key:List.of("baby","swim","fly","run","power","hero","hero_fly","hero_power","dark","dark_run","dark_swim","tails","knuckles"))
      for(boolean adult:new boolean[]{false,true})for(int animal=0;animal<ChaoState.ANIMALS.length;animal++){
        if((ChaoState.animalSlots(animal)&1)==0)continue;
        var data=JsonParser.parseString(Files.readString(root.resolve((animal<21?"overlays/":"cwe_parts/")+"chao_"+key+".json"))).getAsJsonObject();
        var mesh=ChaoMeshAsset.parse(data);var poses=mesh.createPoses();
        float[] right=ChaoToyGrip.animalPalm(mesh,poses,animal,adult,false),left=ChaoToyGrip.animalPalm(mesh,poses,animal,adult,true);cases++;
        java.util.function.BiConsumer<String,PoseStack> anchor=(n,s)->mesh.transformBone(n,s,poses);
        for(float tick:new float[]{0,2,6,12,20,24,29,40,56,72,88,104,120,136,141,146,153,159,160}){
          poses.values().forEach(ChaoMeshAsset.Pose::reset);float w=CastanetsPlayRules.presence(tick);
          for(boolean l:new boolean[]{false,true})ChaoCastanetsAnimation.applyArm(poses.get(l?"arm_left":"arm_right"),clip,tick,w,l);
          ChaoCastanetsAnimation.alignTap(poses,anchor,right,left,w,clip.tap(tick));
          var prop=ChaoCastanetsAnimation.propMatrix(anchor,right,w);var hand=new PoseStack();anchor.accept("arm_right",hand);
          float grip=prop.transformPosition(new Vector3f(ChaoCastanetsAnimation.GRIP).sub(.5f,.5f,.5f)).distance(hand.last().pose().transformPosition(new Vector3f(right).div(16)));maxGrip=Math.max(maxGrip,grip);
          if(!(grip<1e-6))throw new AssertionError("replacement grip "+key+" "+animal);checks++;
          if(CastanetsPlayRules.soundDue((int)tick)){
            var tap=new PoseStack();anchor.accept("arm_left",tap);float gap=tap.last().pose().transformPosition(new Vector3f(left).div(16)).distance(ChaoCastanetsAnimation.hitTarget(ChaoCastanetsAnimation.propMatrix(anchor,right,w),1));maxContact=Math.max(maxContact,gap);
            if(!(gap<.035f))throw new AssertionError("replacement contact "+key+" "+animal+" adult="+adult+" gap="+gap);checks++;
          }
        }
      }
    String text="PASS: "+checks+" castanets replacement checks; "+cases+" two-hand rig/age/animal cases; maxGripError="+maxGrip+" maxContactGap="+maxContact+"; visual review separate.\n";
    Files.writeString(Path.of("build/reports/lionfish/castanets-replacement-results.txt"),text);System.out.print(text);
  }
}
