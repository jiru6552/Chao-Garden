package com.chaogarden.client;
import com.google.gson.*;
import com.mojang.blaze3d.vertex.PoseStack;
import com.chaogarden.core.ChaoState;
import com.chaogarden.core.HeroRunMorphology052;
import com.chaogarden.core.NeutralMorphology056;
import com.chaogarden.core.HeroNormalMorphology057;
import com.chaogarden.core.NeutralSwimMorphology059;
import com.chaogarden.core.DarkNormalMorphology060;
import com.chaogarden.core.NeutralFlyMorphology061;
import com.chaogarden.core.NeutralRunMorphology062;
import com.chaogarden.core.NeutralPowerMorphology063;
import com.chaogarden.core.HeroSwimMorphology064;
import com.chaogarden.core.HeroFlyMorphology065;
import com.chaogarden.core.HeroRunMorphology066;
import com.chaogarden.core.HeroPowerMorphology067;
import com.chaogarden.core.DarkSwimMorphology068;
import com.chaogarden.core.DarkFlyMorphology069;
import com.chaogarden.core.DarkRunMorphology070;
import com.chaogarden.core.DarkPowerMorphology071;
import com.chaogarden.toys.*;
import java.nio.file.*;
import java.util.*;
import org.joml.Matrix4f;
import org.joml.Vector3f;
public final class OrganRigTest {
 record Hands(float[] right,float[] left,String label){}
 public static void main(String[] args)throws Exception{
  var clip=new LionfishOrganClip();var faces=PropMeshClearance.triangles("chao_organ_idle");
  int checks=0,clearanceChecks=0,blockyConfigurations=0,neutralConfigurations=0,heroNormalConfigurations=0,neutralSwimConfigurations=0,darkNormalConfigurations=0,neutralFlyConfigurations=0,neutralRunConfigurations=0,neutralPowerConfigurations=0,heroSwimConfigurations=0,heroFlyConfigurations=0,heroRunConfigurations066=0,heroPowerConfigurations067=0,darkSwimConfigurations068=0,darkFlyConfigurations069=0,darkRunConfigurations070=0,darkPowerConfigurations071=0;float error=0,depthError=0,maxReach=0,minClearance=Float.POSITIVE_INFINITY;
  // The unshipped rounded .54 experiment is archived, not a current actor.
  // Keep retained .52/.53 fixtures and all existing production blocky paths and the completed Hero Swim/Fly/Run families plus full Hero Power067 and Dark Swim068 families covered.
  try(var files=Files.list(Path.of("src/main/resources/assets/chao_garden/models/entity/gameplay"))){for(var path:files.filter(p->p.toString().endsWith(".json")&&!p.toString().contains("_glass")&&!p.getFileName().toString().equals("chao_hero_run_proto054.json")).toList()){
   var mesh=ChaoMeshAsset.parse(JsonParser.parseString(Files.readString(path)).getAsJsonObject());var poses=mesh.createPoses();String key=path.getFileName().toString();
   boolean blocky=key.equals("chao_hero_run_proto055.json"),neutral=key.equals("chao_neutral_proto056.json"),heroNormal=key.equals("chao_hero_normal_proto057.json"),neutralSwim=key.equals("chao_neutral_swim_proto059.json"),darkNormal=key.equals("chao_dark_normal_proto060.json"),neutralFly=key.equals("chao_neutral_fly_proto061.json"),neutralRun=key.equals("chao_neutral_run_proto062.json"),neutralPower=key.equals("chao_neutral_power_proto063.json"),heroSwim=key.equals("chao_hero_swim_proto064.json"),heroFly=key.equals("chao_hero_fly_proto065.json"),heroRun066=key.equals("chao_hero_run_proto066.json"),heroPower067=key.equals("chao_hero_power_proto067.json"),darkSwim068=key.equals("chao_dark_swim_proto068.json"),darkFly069=key.equals("chao_dark_fly_proto069.json"),darkRun070=key.equals("chao_dark_run_proto070.json"),darkPower071=key.equals("chao_dark_power_proto071.json");
   for(boolean adult:neutral||heroNormal||neutralSwim||darkNormal||neutralFly||neutralRun||neutralPower||heroSwim||heroFly||heroRun066||heroPower067||darkSwim068||darkFly069||darkRun070||darkPower071?new boolean[]{true}:new boolean[]{false,true})for(float swimFly:new float[]{-1,0,1})for(float runPower:new float[]{-1,0,1})for(float growth:blocky||neutral||heroNormal||neutralSwim||darkNormal||neutralFly||neutralRun||neutralPower||heroSwim||heroFly||heroRun066||heroPower067||darkSwim068||darkFly069||darkRun070||darkPower071?new float[]{0,.5f,1}:new float[]{0}){
    mesh.development(swimFly,runPower,adult?1:.14f);poses.values().forEach(ChaoMeshAsset.Pose::reset);
    if(blocky){
     var state=new ChaoState();state.adult=adult;state.align=1;state.primaryType=adult?ChaoState.RUN_TYPE:ChaoState.NORMAL;
     int training=Math.round(growth*(adult?960:480));state.levels[ChaoState.RUN]=training/100;state.progress[ChaoState.RUN]=training%100;
     state.setEvolutionStrength(adult?1.2f:1);state.swimFly=swimFly;state.runPower=runPower;
     mesh.heroRunDevelopment(HeroRunMorphology052.sample(state),swimFly,runPower);blockyConfigurations++;
    }else if(neutral||heroNormal||neutralSwim||darkNormal||neutralFly||neutralRun||neutralPower||heroSwim||heroFly||heroRun066||heroPower067||darkSwim068||darkFly069||darkRun070||darkPower071){
     // Mature corners and the young/half-grown blends must retain the same
     // actual keyboard surface/contact constraints, including mixed axes.
     var state=new ChaoState();state.adult=true;state.align=darkNormal||darkSwim068||darkFly069||darkRun070||darkPower071?-1:heroNormal||heroSwim||heroFly||heroRun066||heroPower067?1:0;state.primaryType=neutralSwim||heroSwim||darkSwim068?ChaoState.SWIM_TYPE:neutralFly||heroFly||darkFly069?ChaoState.FLY_TYPE:neutralRun||heroRun066||darkRun070?ChaoState.RUN_TYPE:neutralPower||heroPower067||darkPower071?ChaoState.POWER_TYPE:ChaoState.NORMAL;
     int rp=Math.round(runPower*960),sf=Math.round(swimFly*960);
     if(rp!=0){int stat=rp>0?ChaoState.POWER:ChaoState.RUN,total=Math.abs(rp);state.levels[stat]=total/100;state.progress[stat]=total%100;}
     if(sf!=0){int stat=sf>0?ChaoState.FLY:ChaoState.SWIM,total=Math.abs(sf);state.levels[stat]=total/100;state.progress[stat]=total%100;}
     state.setEvolutionStrength(growth*1.2f);
     if(darkPower071){
      var profile=DarkPowerMorphology071.sample(state);
      if(!profile.enabled()||state.lockedPrimaryType()!=ChaoState.POWER_TYPE||state.lockedAlignment()!=-1||Math.abs(profile.runPower()-runPower)>.00001f||Math.abs(profile.swimFly()-swimFly)>.00001f||Math.abs(profile.maturity()-growth)>.00001f)throw new AssertionError("Production071 keyboard fixture must sample its actual locked Dark Power identity and signed current bars");
      mesh.darkPowerDevelopment071(profile);darkPowerConfigurations071++;
     }
     else if(darkRun070){
      var profile=DarkRunMorphology070.sample(state);
      if(!profile.enabled()||state.lockedPrimaryType()!=ChaoState.RUN_TYPE||state.lockedAlignment()!=-1||Math.abs(profile.runPower()-runPower)>.00001f||Math.abs(profile.swimFly()-swimFly)>.00001f||Math.abs(profile.maturity()-growth)>.00001f)throw new AssertionError("Production070 keyboard fixture must sample its actual locked Dark Run identity and signed current bars");
      mesh.darkRunDevelopment070(profile);darkRunConfigurations070++;
     }
     else if(darkFly069){
      var profile=DarkFlyMorphology069.sample(state);
      if(!profile.enabled()||state.lockedPrimaryType()!=ChaoState.FLY_TYPE||state.lockedAlignment()!=-1||Math.abs(profile.runPower()-runPower)>.00001f||Math.abs(profile.swimFly()-swimFly)>.00001f||Math.abs(profile.maturity()-growth)>.00001f)throw new AssertionError("Production069 keyboard fixture must sample its actual locked Dark Fly identity and signed current bars");
      mesh.darkFlyDevelopment069(profile);darkFlyConfigurations069++;
     }
     else if(darkSwim068){
      var profile=DarkSwimMorphology068.sample(state);
      if(!profile.enabled()||state.lockedPrimaryType()!=ChaoState.SWIM_TYPE||state.lockedAlignment()!=-1||Math.abs(profile.runPower()-runPower)>.00001f||Math.abs(profile.swimFly()-swimFly)>.00001f||Math.abs(profile.maturity()-growth)>.00001f)throw new AssertionError("Production068 keyboard fixture must sample its actual locked Dark Swim identity and signed current bars");
      mesh.darkSwimDevelopment068(profile);darkSwimConfigurations068++;
     }
     else if(heroPower067){
      var profile=HeroPowerMorphology067.sample(state);
      if(!profile.enabled()||state.lockedPrimaryType()!=ChaoState.POWER_TYPE||Math.abs(profile.runPower()-runPower)>.00001f||Math.abs(profile.swimFly()-swimFly)>.00001f||Math.abs(profile.maturity()-growth)>.00001f)throw new AssertionError("Production067 keyboard fixture must sample its actual locked Power identity and signed current bars");
      mesh.heroPowerDevelopment067(profile);heroPowerConfigurations067++;
     }
     else if(heroRun066){
      var profile=HeroRunMorphology066.sample(state);
      if(!profile.enabled()||state.lockedPrimaryType()!=ChaoState.RUN_TYPE||Math.abs(profile.runPower()-runPower)>.00001f||Math.abs(profile.swimFly()-swimFly)>.00001f||Math.abs(profile.maturity()-growth)>.00001f)throw new AssertionError("Production066 keyboard fixture must sample its actual locked Run identity and signed current bars");
      mesh.heroRunDevelopment066(profile);heroRunConfigurations066++;
     }
     else if(heroFly){mesh.heroFlyDevelopment(HeroFlyMorphology065.sample(state));heroFlyConfigurations++;}
     else if(heroSwim){mesh.heroSwimDevelopment(HeroSwimMorphology064.sample(state));heroSwimConfigurations++;}
     else if(neutralPower){mesh.neutralPowerDevelopment(NeutralPowerMorphology063.sample(state));neutralPowerConfigurations++;}
     else if(neutralRun){mesh.neutralRunDevelopment(NeutralRunMorphology062.sample(state));neutralRunConfigurations++;}
     else if(neutralFly){
      var profile=NeutralFlyMorphology061.sample(state);
      if(!profile.enabled()||state.lockedPrimaryType()!=ChaoState.FLY_TYPE||state.lockedAlignment()!=0||Math.abs(profile.runPower()-runPower)>.00001f||Math.abs(profile.swimFly()-swimFly)>.00001f||Math.abs(profile.maturity()-growth)>.00001f)throw new AssertionError("Production061 keyboard fixture must retain its locked first identity and both signed current bars");
      mesh.neutralFlyDevelopment(profile);neutralFlyConfigurations++;
     }
     else if(darkNormal){mesh.darkNormalDevelopment(DarkNormalMorphology060.sample(state));darkNormalConfigurations++;}
     else if(neutralSwim){
      var profile=NeutralSwimMorphology059.sample(state);
      if(!profile.enabled()||state.lockedPrimaryType()!=ChaoState.SWIM_TYPE||state.lockedAlignment()!=0||Math.abs(profile.runPower()-runPower)>.00001f||Math.abs(profile.swimFly()-swimFly)>.00001f||Math.abs(profile.maturity()-growth)>.00001f)throw new AssertionError("Production059 keyboard fixture must retain its locked first identity and both signed current bars");
      mesh.neutralSwimDevelopment(profile);neutralSwimConfigurations++;
     }
     else if(heroNormal){mesh.heroNormalDevelopment(HeroNormalMorphology057.sample(state));heroNormalConfigurations++;}
     else{mesh.neutralDevelopment(NeutralMorphology056.sample(state));neutralConfigurations++;}
    }
    var handMesh=key.contains("chaos_")?ChaoMeshAsset.parse(JsonParser.parseString(Files.readString(path.resolveSibling(key.replace(".json","_glass.json")))).getAsJsonObject()):mesh;
    var hp=handMesh==mesh?poses:handMesh.createPoses();var hands=new ArrayList<Hands>();hands.add(new Hands(handMesh.visiblePalm(hp,false),handMesh.visiblePalm(hp,true),"skin"));
    if(swimFly==0&&runPower==0&&!key.contains("chaos_")&&!key.contains("omochao"))for(int animal=0;animal<ChaoState.ANIMALS.length;animal++){
     if((ChaoState.animalSlots(animal)&1)==0)continue;var p=path.getParent().getParent().resolve((animal<21?"overlays/":"cwe_parts/")+(neutralSwim||darkNormal||neutralFly||neutralRun||neutralPower||heroSwim||heroFly||heroRun066||heroPower067||darkSwim068||darkFly069||darkRun070||darkPower071?"chao_baby.json":key));if(!Files.exists(p))continue;
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
  if(neutralFlyConfigurations!=27)throw new AssertionError("Expected nine signed production061 forms at three independent ages, got "+neutralFlyConfigurations);
  if(neutralSwimConfigurations!=27)throw new AssertionError("Expected nine signed production059 forms at three independent ages, got "+neutralSwimConfigurations);
  if(neutralRunConfigurations!=27)throw new AssertionError("Expected nine production062 signed-axis forms at three independent ages, got "+neutralRunConfigurations);
  if(neutralPowerConfigurations!=27)throw new AssertionError("Expected nine production063 signed-axis forms at three independent ages, got "+neutralPowerConfigurations);
  if(heroSwimConfigurations!=27)throw new AssertionError("Expected nine production064 signed-axis forms at three independent ages, got "+heroSwimConfigurations);
  if(heroFlyConfigurations!=27)throw new AssertionError("Expected nine production065 signed-axis forms at three independent ages, got "+heroFlyConfigurations);
  if(heroRunConfigurations066!=27)throw new AssertionError("Expected nine production066 signed-axis forms at three independent ages, got "+heroRunConfigurations066);
  if(heroPowerConfigurations067!=27)throw new AssertionError("Expected nine production067 signed-axis forms at three independent ages, got "+heroPowerConfigurations067);
  if(darkSwimConfigurations068!=27)throw new AssertionError("Expected nine production068 signed-axis forms at three independent ages, got "+darkSwimConfigurations068);
  if(darkFlyConfigurations069!=27)throw new AssertionError("Expected nine production069 signed-axis forms at three independent ages, got "+darkFlyConfigurations069);
  if(darkPowerConfigurations071!=27)throw new AssertionError("Expected nine production071 signed-axis forms at three independent ages, got "+darkPowerConfigurations071);
  if(darkRunConfigurations070!=27)throw new AssertionError("Expected nine production070 signed-axis forms at three independent ages, got "+darkRunConfigurations070);
  System.out.println("Organ mesh fit: "+checks+" hand samples; "+clearanceChecks+" body/skull surface checks; "+blockyConfigurations+" production055 age/axis/growth configurations; "+neutralConfigurations+" production056 age/axis configurations; "+heroNormalConfigurations+" production057 age/axis configurations; "+neutralSwimConfigurations+" production059 legal age/axis configurations; "+darkNormalConfigurations+" production060 age/axis configurations; "+neutralFlyConfigurations+" production061 legal age/axis configurations; "+neutralRunConfigurations+" production062 signed age/axis configurations; "+neutralPowerConfigurations+" production063 signed age/axis configurations; "+heroSwimConfigurations+" production064 signed age/axis configurations; "+heroFlyConfigurations+" production065 signed age/axis configurations; "+heroRunConfigurations066+" production066 signed age/axis configurations; "+heroPowerConfigurations067+" production067 signed age/axis configurations; "+darkSwimConfigurations068+" production068 signed age/axis configurations; "+darkFlyConfigurations069+" production069 signed age/axis configurations; "+darkRunConfigurations070+" production070 signed age/axis configurations; "+darkPowerConfigurations071+" production071 signed age/axis configurations; no crossed hands; max key-height error="+error+" blocks; max key-depth error="+depthError+" blocks; max reach="+maxReach+" pixels; min body/skull clearance="+minClearance+" pixels");
 }
}
