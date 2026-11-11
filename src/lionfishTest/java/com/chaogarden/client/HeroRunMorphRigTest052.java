package com.chaogarden.client;

import com.chaogarden.core.ChaoState;
import com.chaogarden.core.HeroRunMorphology052;
import com.google.gson.JsonParser;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.joml.Vector3f;

/** Samples the actual prototype vertex/joint path, not a duplicate demo mesh. */
public final class HeroRunMorphRigTest052 {
  private static final Path ROOT=Path.of("src/main/resources/assets/chao_garden");
  private static int checks,frames,vertices;
  private static float largestQuarterStep;
  private static void check(boolean value,String reason){checks++;if(!value)throw new AssertionError(reason);}
  private static ChaoMeshAsset asset(String name) {
    try{return ChaoMeshAsset.parse(JsonParser.parseString(Files.readString(ROOT.resolve("models/entity/"+name+".json"))).getAsJsonObject());}
    catch(Exception error){throw new IllegalStateException(name,error);}
  }
  private static ChaoState state(boolean adult,float progress) {
    var result=new ChaoState();result.adult=adult;result.align=adult?1:.5f*progress;result.primaryType=adult?ChaoState.RUN_TYPE:ChaoState.NORMAL;
    int bars=Math.round(progress*(adult?960:480));result.levels[ChaoState.RUN]=bars/100;result.progress[ChaoState.RUN]=bars%100;
    result.points[ChaoState.RUN]=bars;
    result.setEvolutionStrength(adult?1.2f:.8f);return result;
  }
  private static void sample(ChaoMeshAsset mesh,ChaoState state) {
    var s=HeroRunMorphology052.sample(state);
    mesh.development(state.swimFly,state.runPower,state.adult?state.gradualVisualStrength():state.gradualVisualStrength()*.14f);
    mesh.heroRunDevelopment(s,state.swimFly,state.runPower);
    mesh.heroRunTwoTone(state.baseColor()==0&&state.jewel()==0&&state.isTwoTone());
  }
  private static void visible(ChaoMeshAsset mesh,Map<String,ChaoMeshAsset.Pose> poses) {
    for(String symbol:List.of("emotion_exclamation","emotion_heart","emotion_question","emotion_dizzy","monster_crown"))mesh.setSubtreeVisible(symbol,poses,false);
  }
  private static float width(float[] bounds){return bounds[3]-bounds[0];}
  private static boolean finite(float[] values){if(values==null)return false;for(float v:values)if(!Float.isFinite(v))return false;return true;}
  private static void near(float[] a,float[] b,float tolerance,String reason) {
    check(a!=null&&b!=null&&a.length==b.length,reason+" missing bounds");
    for(int i=0;i<a.length;i++)check(Math.abs(a[i]-b[i])<=tolerance,reason+" axis "+i+" "+a[i]+"/"+b[i]);
  }
  private static final class Capture implements VertexConsumer {
    final List<float[]> points=new ArrayList<>();
    public VertexConsumer addVertex(float x,float y,float z){points.add(new float[]{x,y,z});return this;}
    public VertexConsumer setColor(int r,int g,int b,int a){return this;}
    public VertexConsumer setUv(float u,float v){check(Float.isFinite(u)&&Float.isFinite(v),"nonfinite UV");return this;}
    public VertexConsumer setUv1(int u,int v){return this;}
    public VertexConsumer setUv2(int u,int v){return this;}
    public VertexConsumer setNormal(float x,float y,float z){check(Float.isFinite(x)&&Float.isFinite(y)&&Float.isFinite(z),"nonfinite normal");return this;}
  }
  private static List<float[]> capture(ChaoMeshAsset mesh,Map<String,ChaoMeshAsset.Pose> poses) {
    var out=new Capture();mesh.render(new PoseStack(),out,0x00f000f0,0,0xffffffff,poses,true);vertices+=out.points.size();return out.points;
  }
  private static void stages() {
    var mesh=asset("gameplay/chao_hero_run_proto052");var poses=mesh.createPoses();visible(mesh,poses);
    Set<Long> babySignatures=new HashSet<>(),adultSignatures=new HashSet<>();
    float[] initialEye=null;float initialFoot=0,finalFoot=0;List<float[]> previous=null;
    for(boolean adult:new boolean[]{false,true})for(int step=0;step<=400;step++) {
      float progress=step/400f;var state=state(adult,progress);sample(mesh,state);frames++;
      var points=capture(mesh,poses);
      check(!points.isEmpty(),"prototype emitted no vertices");
      for(var point:points)check(finite(point),"nonfinite actual emitted point");
      for(String bone:List.of("skull","body","arm_left","arm_right","foot_left","foot_right"))
        check(finite(mesh.attachmentBounds(bone,poses)),"nonfinite attachment "+bone);
      check(finite(mesh.toyPalm(poses,false))&&finite(mesh.toyPalm(poses,true)),"palm disconnected");
      if(step%100==0)(adult?adultSignatures:babySignatures).add(mesh.shapeSignature());
      if(!adult&&step==0)initialEye=mesh.attachmentEyes(poses);
      near(initialEye,mesh.attachmentEyes(poses),.00001f,"face/eye panel moved locally");
      if(adult&&step==0)initialFoot=width(mesh.partBounds("foot_left",poses));
      if(adult&&step==400)finalFoot=width(mesh.partBounds("foot_left",poses));
      if(previous!=null&&step>0) {
        check(previous.size()==points.size(),"morph changed rendered topology");
        for(int i=0;i<points.size();i++) {
          float distance=new Vector3f(points.get(i)).distance(new Vector3f(previous.get(i)))*16;
          largestQuarterStep=Math.max(largestQuarterStep,distance);check(distance<.08f,"small progress step snapped a visible vertex: "+distance);
        }
      }
      previous=points;
    }
    check(babySignatures.size()==5,"baby quarter stages are not distinct");check(adultSignatures.size()==5,"adult quarter stages are not distinct");
    check(Math.abs(finalFoot/initialFoot-1.397f)<.002f,"source-informed full Run width differs");
    // The exact same canonical geometry and joints exist on either side of
    // first evolution. The cocoon hides the unchanged .64/.80 outer age scale.
    var baby=state(false,1);sample(mesh,baby);var before=capture(mesh,poses);var eyes=mesh.attachmentEyes(poses);
    var fresh=state(true,1);fresh.setEvolutionStrength(0);sample(mesh,fresh);var after=capture(mesh,poses);
    check(before.size()==after.size(),"first evolution changed topology");for(int i=0;i<before.size();i++)near(before.get(i),after.get(i),.000001f,"first evolution surface jumped");
    near(eyes,mesh.attachmentEyes(poses),.000001f,"first evolution eye position jumped");
    for(String bone:List.of("skull","body","arm_left","foot_left","tail","wing_left")) {
      sample(mesh,baby);var a=new PoseStack();mesh.transformBone(bone,a,poses);var av=a.last().pose().get(new float[16]);
      sample(mesh,fresh);var b=new PoseStack();mesh.transformBone(bone,b,poses);near(av,b.last().pose().get(new float[16]),.000001f,"first evolution joint jumped "+bone);
    }
    // At alignment zero, the baseline has the same actual head/body/limb bounds
    // as the old baby deformation. No threshold model replacement is visible.
    var neutral=state(false,1);neutral.align=0;sample(mesh,neutral);
    var old=asset("gameplay/chao_baby");old.development(neutral.swimFly,neutral.runPower,neutral.gradualVisualStrength()*.14f);var oldPoses=old.createPoses();visible(old,oldPoses);
    for(String bone:List.of("skull","body","arm_left","arm_right","foot_left","foot_right","wing_left","tail"))near(mesh.attachmentBounds(bone,poses),old.attachmentBounds(bone,oldPoses),.00001f,"alignment-zero entry changed "+bone);
  }
  private static void maturityAndOpposition() {
    var mesh=asset("gameplay/chao_hero_run_proto052");var poses=mesh.createPoses();visible(mesh,poses);
    var adult=state(true,1);sample(mesh,adult);float maturity=adult.evolutionStrength();long full=mesh.shapeSignature();
    adult.levels[ChaoState.RUN]=0;adult.progress[ChaoState.RUN]=0;sample(mesh,adult);long reduced=mesh.shapeSignature();
    check(full!=reduced,"Run loss did not reverse anatomy");check(adult.evolutionStrength()==maturity,"stat loss changed biological maturity");
    var body=mesh.attachmentBounds("body",poses);var young=state(true,0);young.setEvolutionStrength(0);sample(mesh,young);
    check(width(body)>width(mesh.attachmentBounds("body",poses))+.1f,"mature baseline reverted to young body with zero Run");
    var base=state(true,0);sample(mesh,base);long center=mesh.shapeSignature();
    base.swimFly=1;sample(mesh,base);check(center!=mesh.shapeSignature(),"Fly-side legacy influence lost");
    base.swimFly=-1;sample(mesh,base);check(center!=mesh.shapeSignature(),"Swim-side legacy influence lost");
    base.swimFly=0;base.runPower=1;sample(mesh,base);check(center!=mesh.shapeSignature(),"Power-side legacy influence lost");
  }
  private static void temporalSmoothing() {
    UUID id=UUID.randomUUID();var child=state(false,0);HeroRunAppearance052.prepare(id,child,0);
    child.align=.5f;child.levels[ChaoState.RUN]=4;child.progress[ChaoState.RUN]=80;
    String encoded=child.encode();var entry=HeroRunAppearance052.prepare(id,child,1);
    check(entry.babyHero()==0&&entry.babyRun()==0,"large edit snapped appearance");
    var halfway=HeroRunAppearance052.prepare(id,child,11);check(Math.abs(halfway.babyHero()-.5f)<.001f,"edit did not interpolate midway");
    var complete=HeroRunAppearance052.prepare(id,child,21);check(complete.babyHero()==1&&complete.babyRun()==1,"edit did not reach exact target");
    check(encoded.equals(child.encode()),"client interpolation changed saved state");
    child.align=0;child.levels[ChaoState.RUN]=0;child.progress[ChaoState.RUN]=0;
    HeroRunAppearance052.prepare(id,child,22);var reverse=HeroRunAppearance052.prepare(id,child,32);
    check(reverse.babyHero()>0&&reverse.babyHero()<1,"reversal did not interpolate");
    var reset=HeroRunAppearance052.prepare(id,child,150);check(reset.babyHero()==0,"stale actor retained old geometry");
    child.align=.5f;var backwards=HeroRunAppearance052.prepare(id,child,2);check(backwards.babyHero()==1,"age reset reused stale blend");
  }
  private static void separateAttachmentRigs() {
    var skin=asset("gameplay/chao_hero_run_proto052");var poses=skin.createPoses();
    for(String family:List.of("overlays/chao_baby","cwe_parts/chao_baby","hats/chao_baby","market_accessories")) {
      var target=asset(family);var attached=target.createPoses();
      for(boolean adult:new boolean[]{false,true})for(int quarter=0;quarter<=4;quarter++) {
        var state=state(adult,quarter/4f);sample(skin,state);
        poses.values().forEach(ChaoMeshAsset.Pose::reset);attached.values().forEach(ChaoMeshAsset.Pose::reset);
        // Rotating ancestors checks the complete hierarchy rather than only
        // comparing isolated local pivot numbers in the bind pose.
        poses.get("body").rx=.17f;poses.get("body").ry=-.23f;poses.get("skull").ry=.31f;
        for(var entry:poses.entrySet())if(attached.containsKey(entry.getKey()))skin.copyJointPoseTo(entry.getKey(),target,entry.getValue(),attached.get(entry.getKey()));
        for(String bone:List.of("body","skull","arm_left","arm_right","foot_left","foot_right","tail","wing_left","wing_right")) {
          if(!attached.containsKey(bone))continue;
          var sourceMatrix=new PoseStack();var targetMatrix=new PoseStack();
          skin.transformBone(bone,sourceMatrix,poses);target.transformBone(bone,targetMatrix,attached);
          near(sourceMatrix.last().pose().get(new float[16]),targetMatrix.last().pose().get(new float[16]),.000002f,"separate attachment joint diverged "+family+" "+bone);
        }
      }
    }
  }
  private static void reservedPaletteEffects() {
    String prototype="dynamic/hero_run052/check";
    check(ChaoPaletteMaterial.prototypeReservedKind(prototype,"baby",127,63,128,64)==1,"crown body not in prototype palette mask");
    check(ChaoPaletteMaterial.prototypeReservedKind(prototype,"baby",126,63,128,64)==3,"crown tip not in prototype accent mask");
    check(ChaoPaletteMaterial.prototypeReservedKind(prototype,"baby",104,56,128,64)==3,"belly omitted from prototype palette mask");
    for(int y=0;y<64;y++)for(int x=0;x<128;x++) {
      check(ChaoPaletteMaterial.prototypeReservedKind(prototype,"overlay",x,y,128,64)==0,"prototype skin additions leaked into animal overlay");
      check(ChaoPaletteMaterial.prototypeReservedKind("textures/entity/chao/official/baby.png","baby",x,y,128,64)==0,"prototype mask affected original source");
      check(ChaoPaletteMaterial.prototypeReservedKind("dynamic/palette/other","baby",x,y,128,64)==0,"prototype mask affected another dynamic material");
      if(x<8&&y<8)check(ChaoPaletteMaterial.prototypeReservedKind(prototype,"baby",x,y,128,64)==0,"prototype mask reached protected eye pixels");
    }
    int original=0xadc08040;
    for(boolean accent:new boolean[]{false,true})for(boolean brush:new boolean[]{false,true})for(boolean negative:new boolean[]{false,true})for(boolean bright:new boolean[]{false,true}) {
      int value=ChaoPaletteMaterial.paletteEffects(original,accent&&brush,brush?2:0,negative,bright);
      check((value>>>24)==(original>>>24),"optional palette changed alpha");
      if(!brush||!accent) {
        if(!negative&&!bright)check(value==original,"unmodified pixel changed");
        if(negative&&!bright)check(value==0xad3f7fbf,"negative reserved pixel differs");
        if(bright&&!negative)check((value&255)>(original&255)&&(value>>>8&255)>(original>>>8&255)&&(value>>>16&255)>(original>>>16&255),"bright reserved pixel unchanged");
      }else check(value!=original,"Hero brush omitted new accent");
    }
  }
  private static void motionAnchors()throws Exception {
    var clips=JsonParser.parseString(Files.readString(ROOT.resolve("animations/official/chao.animation.json"))).getAsJsonObject().getAsJsonObject("animations");
    var rig=new OfficialChaoRig(clips,HeroRunMorphRigTest052::asset);
    for(boolean adult:new boolean[]{false,true})for(int fraction=0;fraction<=4;fraction++) {
      var state=state(adult,fraction/4f);String geometry=OfficialChaoRig.geometryKey(state);
      for(var motion:List.of(OfficialChaoRig.Motion.CRAWL,OfficialChaoRig.Motion.WALK,OfficialChaoRig.Motion.RUN,OfficialChaoRig.Motion.SIT,OfficialChaoRig.Motion.SWIM,OfficialChaoRig.Motion.GLIDE,OfficialChaoRig.Motion.HELD))for(int tick=0;tick<=20;tick+=2) {
        rig.prepareDevelopment(geometry,state);rig.animateGameplay(geometry,new UUID(1,10+fraction),motion,tick,1,.05f,0,0,0,0,false);rig.development(state);frames++;
        check(rig.finite(),"motion morph pose nonfinite");check(rig.sourceGeometryKey().equals("baby"),"prototype UV/attachment skeleton mismatched");
        for(String bone:List.of("skull","body","arm_left","arm_right","foot_left","foot_right")) {
          var stack=new PoseStack();rig.transformBone(bone,stack);check(stack.last().pose().isFinite(),"motion anchor nonfinite "+bone);
          check(finite(rig.attachmentBounds(bone)),"motion attachment missing "+bone);
        }
        check(finite(rig.toyPalm(false))&&finite(rig.toyPalm(true)),"motion palm missing");
      }
    }
  }
  public static void main(String[] args)throws Exception {
    stages();maturityAndOpposition();temporalSmoothing();separateAttachmentRigs();reservedPaletteEffects();motionAnchors();
    String report="Hero Run morphology: "+checks+" checks; "+frames+" actual geometry/pose frames; "+vertices+" emitted vertices; max small-progress step="+largestQuarterStep+" model pixels; five baby/adult stages, exact first-evolution surface/joints, eyes, inherited axes, maturity and smoothing passed\n";
    Files.createDirectories(Path.of("build/reports/lionfish"));Files.writeString(Path.of("build/reports/lionfish/hero-run052-results.txt"),report);System.out.print(report);
  }
}
