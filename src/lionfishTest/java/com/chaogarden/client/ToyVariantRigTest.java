package com.chaogarden.client;

import com.chaogarden.core.ChaoState;
import com.chaogarden.toys.RattlePlayRules;
import com.google.gson.*;
import com.mojang.blaze3d.vertex.PoseStack;
import java.nio.file.*;
import java.util.*;
import org.joml.Vector3f;

/** Replacement-limb grips measured from independently emitted hand vertices. */
final class ToyVariantRigTest {
  private static int checks,cases;
  private static void check(boolean ok,String why){checks++;if(!ok)throw new AssertionError(why);}
  static void run() throws Exception {
    var bell=new LionfishBellClip();var maracas=new LionfishMaracasClip();var rattle=new LionfishRattleClip();var bubbles=new LionfishBubbleClip();
    Path root=Path.of("src/main/resources/assets/chao_garden/models/entity");
    for(String key:List.of("baby","swim","fly","run","power","hero","hero_fly","hero_power","dark","dark_run","dark_swim","tails","knuckles")) {
      for(boolean adult:new boolean[]{false,true})for(int animal=0;animal<ChaoState.ANIMALS.length;animal++) {
        if((ChaoState.animalSlots(animal)&1)==0)continue;
        for(boolean left:new boolean[]{false,true}){
        var data=JsonParser.parseString(Files.readString(root.resolve((animal<21?"overlays/":"cwe_parts/")+"chao_"+key+".json"))).getAsJsonObject();
        var mesh=ChaoMeshAsset.parse(data);var poses=mesh.createPoses();
        float[] palm=ChaoToyGrip.animalPalm(mesh,poses,animal,adult,left);
        check(palm!=null,key+" "+animal+" replacement hand exists");cases++;
        // Render the actual hand meshes, omitting the connector and unrelated
        // decorations, to establish the grip independently of socket lookup.
        var measuredData=data.deepCopy();String bone="animal_0_"+animal+(left?"_left":"_right");
        for(var e:measuredData.getAsJsonArray("bones")) {
          var b=e.getAsJsonObject();var selected=new JsonArray();
          if(b.get("name").getAsString().equals(bone))for(var m:b.getAsJsonArray("meshes"))
            if(Set.of("gene_hand","paw","hoof","claw").contains(m.getAsJsonObject().get("name").getAsString()))selected.add(m);
          b.add("meshes",selected);
        }
        var emitted=ChaoMeshAsset.parse(measuredData);
        for(float tick:new float[]{0,2,5,10,20,22.5f,30,42,60,78,90,120,138,146,153,159,160})for(int kind=0;kind<4;kind++) {
          if(left&&kind!=2)continue;
          boolean bubble=kind==1;
          ChaoToyGrip.animalPalm(mesh,poses,animal,adult,left);
          String anchor=left?"arm_left":"arm_right";var arm=poses.get(anchor);
          if(kind==3)ChaoBellAnimation.applyArm(arm,bell,tick,com.chaogarden.toys.BellPlayRules.presence(tick));else if(kind==2)ChaoMaracasAnimation.applyArm(arm,maracas,tick,left);else if(bubble)ChaoBubbleAnimation.applyArm(arm,bubbles,tick);else ChaoRattleAnimation.applyArm(arm,rattle,tick);
          var rootPose=new PoseStack();float scale=adult?.8f:.64f;rootPose.scale(scale,scale,scale);
          var sink=new RattleAnimationTest.Sink();emitted.render(rootPose,sink,0,0,-1,poses,false);
          check(!sink.vertices.isEmpty(),"replacement hand emits vertices");
          var prop=new PoseStack();prop.scale(scale,scale,scale);mesh.transformBone(anchor,prop,poses);
          float presence=Math.max(.001f,kind==2?com.chaogarden.toys.MaracasPlayRules.presence(tick):RattlePlayRules.presence(tick));
          if(kind==3)ChaoBellAnimation.propTransform(prop,palm,presence);else if(kind==2)ChaoMaracasAnimation.propTransform(prop,palm,presence,left);else if(bubble)ChaoBubbleAnimation.propTransform(prop,palm,presence);else ChaoRattleAnimation.propTransform(prop,palm,presence);
          prop.translate(-.5,-.5,-.5);
          var grip=prop.last().pose().transformPosition(new Vector3f(.5f,kind==3?.89f:bubble?.28f:.30f,.5f));
          check(grip.distance(sink.center())<1e-6f,"toy stays inside rendered replacement hand: "+key+" "+animal+" "+adult+" "+tick);
        }
      }
    }
    }
    for(String character:List.of("normal","tails","knuckles","amy","omochao"))for(boolean adult:new boolean[]{false,true})
      for(float align:new float[]{-1,0,1})for(int type=0;type<=4;type++)for(int color=0;color<ChaoState.COLORS.length;color++) {
        var s=new ChaoState();s.character=character;s.adult=adult;s.align=align;s.primaryType=type;s.expressedColor=color;
        check(RattlePlayRules.handheld(s),"age/alignment/type/color cannot remove a completed hand");
      }
    for(int stage=0;stage<=3;stage++){
      var s=new ChaoState();s.character="omochao";s.setTrait("content.construction",stage);
      check(RattlePlayRules.handheld(s)==(stage==0||stage==3),"unfinished Omochao keeps its visible ground fallback");
    }
    Files.createDirectories(Path.of("build/reports/lionfish"));
    String result="PASS: "+checks+" variant checks; "+cases+" replacement-hand rig/age/animal cases, all four clips, both maracas hands measured against rendered vertices. Visual quality requires the separate client study.\n";
    Files.writeString(Path.of("build/reports/lionfish/variant-results.txt"),result);System.out.print(result);
  }
}
