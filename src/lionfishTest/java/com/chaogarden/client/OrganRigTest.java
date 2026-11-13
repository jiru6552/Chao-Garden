package com.chaogarden.client;
import com.google.gson.*;
import com.mojang.blaze3d.vertex.PoseStack;
import com.chaogarden.core.ChaoState;
import com.chaogarden.core.HeroRunMorphology052;
import com.chaogarden.core.NeutralMorphology056;
import com.chaogarden.core.HeroNormalMorphology057;
import com.chaogarden.core.NeutralSwimMorphology059;
import com.chaogarden.core.DarkNormalMorphology060;
import com.chaogarden.toys.*;
import java.nio.file.*;
import java.util.*;
import org.joml.Matrix4f;
import org.joml.Vector3f;
public final class OrganRigTest {
 record Hands(float[] right,float[] left,String label){}
 public static void main(String[] args)throws Exception{
  var clip=new LionfishOrganClip();var faces=PropMeshClearance.triangles("chao_organ_idle");
  int checks=0,clearanceChecks=0,blockyConfigurations=0,neutralConfigurations=0,heroNormalConfigurations=0,neutralSwimConfigurations=0,darkNormalConfigurations=0;float error=0,depthError=0,maxReach=0,minClearance=Float.POSITIVE_INFINITY;
  // The unshipped rounded .54 experiment is archived, not a current actor.
  // Keep retained .52/.53 fixtures and all existing production blocky paths and the scoped Swim-first family covered.
  try(var files=Files.list(Path.of("src/main/resources/assets/chao_garden/models/entity/gameplay"))){for(var path:files.filter(p->p.toString().endsWith(".json")&&!p.toString().contains("_glass")&&!p.getFileName().toString().equals("chao_hero_run_proto054.json")).toList()){
   var mesh=ChaoMeshAsset.parse(JsonParser.parseString(Files.readString(path)).getAsJsonObject());var poses=mesh.createPoses();String key=path.getFileName().toString();
   boolean blocky=key.equals("chao_hero_run_proto055.json"),neutral=key.equals("chao_neutral_proto056.json"),heroNormal=key.equals("chao_hero_normal_proto057.json"),neutralSwim=key.equals("chao_neutral_swim_proto059.json"),darkNormal=key.equals("chao_dark_normal_proto060.json");
   for(boolean adult:neutral||heroNormal||neutralSwim||darkNormal?new boolean[]{true}:new boolean[]{false,true})for(float swimFly:neutralSwim?new float[]{-1,0}:new float[]{-1,0,1})for(float runPower:new float[]{-1,0,1})for(float growth:blocky||neutral||heroNormal||neutralSwim||darkNormal?new float[]{0,.5f,1}:new float[]{0}){
    mesh.development(swimFly,runPower,adult?1:.14f);poses.values().forEach(ChaoMeshAsset.Pose::reset);
    if(blocky){
     var state=new ChaoState();state.adult=adult;state.align=1;state.primaryType=adult?ChaoState.RUN_TYPE:ChaoState.NORMAL;
     int training=Math.round(growth*(adult?960:480));state.levels[ChaoState.RUN]=training/100;state.progress[ChaoState.RUN]=training%100;
     state.setEvolutionStrength(adult?1.2f:1);state.swimFly=swimFly;state.runPower=runPower;
     mesh.heroRunDevelopment(HeroRunMorphology052.sample(state),swimFly,runPower);blockyConfigurations++;
    }else if(neutral||heroNormal||neutralSwim||darkNormal){
     // Mature corners and the young/half-grown blends must retain the same
     // actual keyboard surface/contact constraints, including mixed axes.
     var state=new ChaoState();state.adult=true;state.align=darkNormal?-1:heroNormal?1:0;state.primaryType=neutralSwim?ChaoState.SWIM_TYPE:ChaoState.NORMAL;
     int rp=Math.round(runPower*960),sf=Math.round(swimFly*960);
     if(rp!=0){int stat=rp>0?ChaoState.POWER:ChaoState.RUN,total=Math.abs(rp);state.levels[stat]=total/100;state.progress[stat]=total%100;}
     if(sf!=0){int stat=sf>0?ChaoState.FLY:ChaoState.SWIM,total=Math.abs(sf);state.levels[stat]=total/100;state.progress[stat]=total%100;}
     state.setEvolutionStrength(growth*1.2f);
     if(darkNormal){mesh.darkNormalDevelopment(DarkNormalMorphology060.sample(state));darkNormalConfigurations++;}
     else if(neutralSwim){mesh.neutralSwimDevelopment(NeutralSwimMorphology059.sample(state));neutralSwimConfigurations++;}
     else if(heroNormal){mesh.heroNormalDevelopment(HeroNormalMorphology057.sample(state));heroNormalConfigurations++;}
     else{mesh.neutralDevelopment(NeutralMorphology056.sample(state));neutralConfigurations++;}
    }
    var handMesh=key.contains("chaos_")?ChaoMeshAsset.parse(JsonParser.parseString(Files.readString(path.resolveSibling(key.replace(".json","_glass.json")))).getAsJsonObject()):mesh;
    var hp=handMesh==mesh?poses:handMesh.createPoses();var hands=new ArrayList<Hands>();hands.add(new Hands(handMesh.visiblePalm(hp,false),handMesh.visiblePalm(hp,true),"skin"));
    if(swimFly==0&&runPower==0&&!key.contains("chaos_")&&!key.contains("omochao"))for(int animal=0;animal<ChaoState.ANIMALS.length;animal++){
     if((ChaoState.animalSlots(animal)&1)==0)continue;var p=path.getParent().getParent().resolve((animal<21?"overlays/":"cwe_parts/")+(neutralSwim||darkNormal?"chao_baby.json":key));if(!Files.exists(p))continue;
     var repl=ChaoMeshAsset.parse(JsonParser.parseString(Files.readString(p)).getAsJsonObject());var rp=repl.createPoses();var right=ChaoToyGrip.animalPalm(repl,rp,animal,adult,false);var left=ChaoToyGrip.animalPalm(repl,rp,animal,adult,true);if(right!=null&&left!=null)hands.add(new Hands(right,left,"animal"+animal));
    }
    float scale=key.contains("chaos_")?.86f:adult?.80f:.64f;
    float distance=(float)ToyInteractionRules.keyboardDistance(OrganPlayRules.KIND,adult?.55:.44);
    for(var h:hands)for(int t:java.util.stream.IntStream.rangeClosed(0,80).map(n->n*2).toArray()){
     if(h.right==null||h.left==null)continue;
     poses.values().forEach(ChaoMeshAsset.Pose::reset);poses.get("body").y=.8f;poses.get("body").rx=-.08f;poses.get("skull").y=.8f;
     java.util.function.BiConsumer<String,PoseStack> anchor=(name,stack)->mesh.transformBone(name,stack,poses);
     float w=OrganPlayRules.presence(t);ChaoOrganAnimation.fit(poses,anchor,h.right,h.left,clip,t,w,scale,distance,0);
     if(h.label.equals("skin")&&(t==24||t==64||t==104||t==144)){
      // Transform the shipped organ triangles into the actual body/skull
      // frames. A smaller entity collision proxy must not hide mesh clipping.
      var organ=new Matrix4f().translate(0,1.501f-.5f/scale,-distance/scale).scale(1/scale,-1/scale,-1/scale);
      for(String bone:new String[]{"body","skull"}){
       var joint=new PoseStack();anchor.accept(bone,joint);
       float gap=PropMeshClearance.separation(faces,new Matrix4f(joint.last().pose()).invert().mul(organ),mesh.attachmentBounds(bone,poses));
       minClearance=Math.min(minClearance,gap);clearanceChecks++;
       if(gap<=.05f)throw new AssertionError("body intersects organ mesh "+key+" "+bone+" t="+t+" gap="+gap);
      }
     }
     for(boolean left:new boolean[]{false,true}){
      var arm=poses.get(left?"arm_left":"arm_right");var palm=ChaoTrumpetAnimation.reachedPalm(left?h.left:h.right,arm.reach,left);var at=ChaoTriangleAnimation.palm(anchor,palm,left);
      if(!at.isFinite()||Math.abs(arm.y)>.301)throw new AssertionError("bad joint "+key+" "+h.label);
      // The old fit passed contact checks by growing the arm up to 7.67px.
      // Keep the rendered limb correction short while still reaching the mesh.
      if(arm.reach>2.001f||arm.reach<0)throw new AssertionError("elongated arm "+key+" "+h.label+" reach="+arm.reach);
      if(w==1){
       if(h.label.equals("skin")&&!key.contains("chaos_")){
        var visibility=new java.util.HashMap<String,Boolean>();poses.forEach((name,p)->visibility.put(name,p.visible));
        poses.forEach((name,p)->p.visible=name.equals(left?"arm_left__cube_1":"arm_right__cube_1"));
        var rendered=new RattleAnimationTest.Sink();mesh.render(new PoseStack(),rendered,0,0,-1,poses,false);
        poses.forEach((name,p)->p.visible=visibility.get(name));
        if(!rendered.vertices.isEmpty()&&rendered.center().distance(at)>.001f)throw new AssertionError("actual rendered hand differs from keyboard contact "+key+" t="+t+" "+rendered.center()+" vs "+at);
       }
       float worldX=at.x*scale,worldY=(1.501f-at.y)*scale;
       if((left?worldX:-worldX)<.05||Math.abs(worldX)>.42)throw new AssertionError("crossed/off keyboard "+key+" "+h.label+" "+at);
       float expected=.21167f+.045f*(1-clip.tap(t,left));
       float sourceZ=.5f-(distance+at.z*scale),sourceX=.5f+worldX;
       if(sourceZ<.39713f||sourceZ>.48796f||sourceX<.23512f||sourceX>.79223f)throw new AssertionError("off actual organ key mesh "+key+" "+h.label+" "+sourceX+","+sourceZ);error=Math.max(error,Math.abs(worldY-expected));
       // Independently measured front quarter of the white manual. Contact
       // must not drift backwards again to compensate for a distant actor.
       depthError=Math.max(depthError,Math.abs(sourceZ-.415f));
       if(Math.abs(sourceZ-.415f)>.015f)throw new AssertionError("keys too far behind natural hand reach "+key+" "+h.label+" "+sourceZ);
       if(Math.abs(worldY-expected)>.035)throw new AssertionError("key clearance "+key+" "+h.label+" "+worldY+" expected "+expected);
      }
      maxReach=Math.max(maxReach,arm.reach);checks++;
     }
    }
   }
  }}
  System.out.println("Organ mesh fit: "+checks+" hand samples; "+clearanceChecks+" body/skull surface checks; "+blockyConfigurations+" production055 age/axis/growth configurations; "+neutralConfigurations+" production056 age/axis configurations; "+heroNormalConfigurations+" production057 age/axis configurations; "+neutralSwimConfigurations+" production059 legal age/axis configurations; "+darkNormalConfigurations+" production060 age/axis configurations; no crossed hands; max key-height error="+error+" blocks; max key-depth error="+depthError+" blocks; max reach="+maxReach+" pixels; min body/skull clearance="+minClearance+" pixels");
 }
}
