package com.chaogarden.client;

import com.chaogarden.core.AnalyzerStatRules;
import com.chaogarden.core.ChaoState;
import com.chaogarden.core.DriveRules;
import com.chaogarden.core.HeroRunMorphology052;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import org.joml.Vector3f;

/** Actual production box interpolation, fixed original face and shared rig.
 * These assertions verify emitted surfaces/anchors, not a visual approval. */
public final class BlockyRunMorphRigTest055 {
  private static final Path ROOT=Path.of("src/main/resources/assets/chao_garden");
  private static final String MODEL="gameplay/chao_hero_run_proto055";
  private static final float EPS=.00003f;
  private static final float[] QUARTERS={0,.25f,.5f,.75f,1};
  private static final List<String> LIMBS=List.of("arm_left__cube_1","arm_right__cube_1","foot_left__cube_0","foot_right__cube_0");
  private static final List<String> PROTECTED=List.of("skull__cube_0","skull__cube_2","skull__cube_3");
  private static int checks,frames,boxSolids,limbSolids,motionFrames,coats,attachments;
  private static float maximumStep,maximumEditStep;

  private static void check(boolean ok,String why){checks++;if(!ok)throw new AssertionError(why);}
  private static void near(float a,float b,float epsilon,String why){check(Float.isFinite(a)&&Float.isFinite(b)&&Math.abs(a-b)<=epsilon,why+": "+a+" / "+b);}
  private static void near(float[] a,float[] b,float epsilon,String why){check(a!=null&&b!=null&&a.length==b.length,why+" missing");for(int i=0;i<a.length;i++)near(a[i],b[i],epsilon,why+" component "+i);}
  private static JsonObject data(String name)throws Exception{return JsonParser.parseString(Files.readString(ROOT.resolve("models/entity/"+name+".json"))).getAsJsonObject();}
  private static ChaoMeshAsset asset(String name){try{var m=ChaoMeshAsset.parse(data(name));m.lens(0xff101820);m.expression(1);return m;}catch(Exception e){throw new IllegalStateException(name,e);}}
  private static ChaoState state(boolean adult,float training,float age,float alignment){
    var s=new ChaoState();s.adult=adult;s.align=alignment;s.primaryType=adult?ChaoState.RUN_TYPE:ChaoState.NORMAL;
    int total=Math.round(training*(adult?960:480));s.levels[ChaoState.RUN]=total/100;s.progress[ChaoState.RUN]=total%100;s.points[ChaoState.RUN]=total;
    s.setEvolutionStrength(age*(adult?1.2f:1));return s;
  }
  private static void sample(ChaoMeshAsset mesh,ChaoState s){
    mesh.development(s.swimFly,s.runPower,s.gradualVisualStrength());
    mesh.heroRunDevelopment(HeroRunMorphology052.sample(s),s.swimFly,s.runPower);
    mesh.heroRunTwoTone(s.baseColor()==0&&s.jewel()==0&&s.isTwoTone());
  }
  private static Map<String,ChaoMeshAsset.Pose> poses(ChaoMeshAsset mesh){
    var p=mesh.createPoses();for(String name:List.of("emotion_exclamation","emotion_heart","emotion_question","emotion_dizzy","monster_crown"))mesh.setSubtreeVisible(name,p,false);return p;
  }

  /** Primitive arrays keep the dense emitted-geometry sweeps bounded. */
  private static final class Capture implements VertexConsumer {
    float[] points=new float[12288],normals=new float[12288],uv=new float[8192];int n,nn,nu;
    public VertexConsumer addVertex(float x,float y,float z){check(Float.isFinite(x)&&Float.isFinite(y)&&Float.isFinite(z),"Non-finite emitted vertex");if(n+3>points.length)points=Arrays.copyOf(points,points.length*2);points[n++]=x*16;points[n++]=y*16;points[n++]=z*16;return this;}
    public VertexConsumer setColor(int r,int g,int b,int a){return this;}
    public VertexConsumer setUv(float u,float v){check(Float.isFinite(u)&&Float.isFinite(v),"Non-finite emitted UV");if(nu+2>uv.length)uv=Arrays.copyOf(uv,uv.length*2);uv[nu++]=u;uv[nu++]=v;return this;}
    public VertexConsumer setUv1(int u,int v){return this;}public VertexConsumer setUv2(int u,int v){return this;}
    public VertexConsumer setNormal(float x,float y,float z){check(Float.isFinite(x)&&Float.isFinite(y)&&Float.isFinite(z),"Non-finite emitted normal");if(nn+3>normals.length)normals=Arrays.copyOf(normals,normals.length*2);normals[nn++]=x;normals[nn++]=y;normals[nn++]=z;return this;}
    Vector3f point(int i){return new Vector3f(points[i*3],points[i*3+1],points[i*3+2]);}
    int vertices(){return n/3;}
  }
  private static Capture capture(ChaoMeshAsset mesh,Map<String,ChaoMeshAsset.Pose> p){var c=new Capture();mesh.render(new PoseStack(),c,0xf000f0,0,0xffffffff,p,true);check(c.n==c.nn&&c.nu==c.vertices()*2,"Emitter omitted a UV or normal");return c;}
  private static Capture solid(ChaoMeshAsset mesh,String name){var p=mesh.createPoses();p.values().forEach(v->v.visible=false);check(p.containsKey(name),"Missing source bone "+name);p.get(name).visible=true;return capture(mesh,p);}
  private static float[] bounds(Capture c,int start,int count){float[] b={Float.POSITIVE_INFINITY,Float.POSITIVE_INFINITY,Float.POSITIVE_INFINITY,Float.NEGATIVE_INFINITY,Float.NEGATIVE_INFINITY,Float.NEGATIVE_INFINITY};for(int i=start*3;i<(start+count)*3;i++) {int a=i%3;b[a]=Math.min(b[a],c.points[i]);b[a+3]=Math.max(b[a+3],c.points[i]);}return b;}
  private static void equal(Capture a,Capture b,float epsilon,String why){check(a.n==b.n&&a.nu==b.nu,why+" changed topology");for(int i=0;i<a.n;i++)near(a.points[i],b.points[i],epsilon,why);for(int i=0;i<a.nu;i++)near(a.uv[i],b.uv[i],0,why+" changed UV");}
  private static boolean same(Capture a,Capture b){if(a.n!=b.n||a.nu!=b.nu||a.nn!=b.nn)return false;for(int i=0;i<a.n;i++)if(a.points[i]!=b.points[i]||a.normals[i]!=b.normals[i])return false;for(int i=0;i<a.nu;i++)if(a.uv[i]!=b.uv[i])return false;return true;}
  private static void normals(Capture c){
    for(int i=0;i<c.vertices();i+=4){var a=c.point(i);var normal=c.point(i+1).sub(a).cross(c.point(i+2).sub(a));if(normal.lengthSquared()<1e-10f)normal=c.point(i+2).sub(a).cross(c.point(i+3).sub(a));if(normal.lengthSquared()<1e-10f)continue;normal.normalize();float dot=normal.x*c.normals[i*3]+normal.y*c.normals[i*3+1]+normal.z*c.normals[i*3+2];check(dot>.999f,"Emitted normal does not follow deformed face");}
  }
  private static float volume(Capture c,int start,int count){
    var b=bounds(c,start,count);var center=new Vector3f((b[0]+b[3])/2,(b[1]+b[4])/2,(b[2]+b[5])/2);float value=0;
    for(int i=start;i<start+count;i+=4){var a=c.point(i).sub(center);var q=c.point(i+1).sub(center);var r=c.point(i+2).sub(center);var d=c.point(i+3).sub(center);value+=a.dot(new Vector3f(q).cross(r))/6+a.dot(new Vector3f(r).cross(d))/6;}return value;
  }
  private static void cuboids(Capture c){
    check(c.vertices()%24==0,"Closed added boxes changed face count");
    for(int start=0;start<c.vertices();start+=24){
      check(volume(c,start,24)>.00000005f,"Added box collapsed or inverted");
      for(int i=start;i<start+24;i+=4){
        var a=c.point(i);var b=c.point(i+1);var q=c.point(i+2);var d=c.point(i+3);
        var ab=new Vector3f(b).sub(a);var bc=new Vector3f(q).sub(b);var cd=new Vector3f(d).sub(q);var da=new Vector3f(a).sub(d);
        check(ab.lengthSquared()>1e-7f&&bc.lengthSquared()>1e-7f,"Added box has a zero edge");
        check(Math.abs(ab.dot(bc))<=.00002f*Math.max(1,ab.length()*bc.length()),"Interpolated box sheared");
        check(new Vector3f(ab).add(cd).length()<.00003f&&new Vector3f(bc).add(da).length()<.00003f,"Rectangular face lost opposite edges");
      }
      boxSolids++;
    }
    normals(c);
  }
  private static void compactLimbs(ChaoMeshAsset mesh,boolean exactFloor){
    for(String name:LIMBS){var c=solid(mesh,name);check(c.vertices()==24,"Original compact limb changed topology");var b=bounds(c,0,c.vertices());float x=b[3]-b[0],y=b[4]-b[1],z=b[5]-b[2];boolean foot=name.startsWith("foot");
      check(volume(c,0,24)>(foot?12:5),"Original limb lost coherent positive volume");
      check(Math.max(x,Math.max(y,z))/Math.min(x,Math.min(y,z))<(foot?2.6f:1.5f),"Hand or foot stretched into segments");
      if(foot)near(b[4],24,exactFloor?.00004f:.27f,"Resting sole lost the shared support plane");normals(c);limbSolids++;
    }
  }

  private static void topology()throws Exception{
    var d=data(MODEL);var original=data("gameplay/chao_baby");check(d.has("blocky_run_path_055")&&!d.has("hero_run_prototype_054"),"Production asset still selects paused rounded path");
    check(d.getAsJsonObject("blocky_run_path_055").getAsJsonArray("targets").size()==7,"Blocky path corner count changed");
    Map<String,JsonObject> old=new HashMap<>();for(var b:original.getAsJsonArray("bones"))old.put(b.getAsJsonObject().get("name").getAsString(),b.getAsJsonObject());
    Set<String> features=new HashSet<>();
    for(var be:d.getAsJsonArray("bones")){var bone=be.getAsJsonObject();String name=bone.get("name").getAsString();
      if(PROTECTED.contains(name)||name.startsWith("expression_mouth_"))check(bone.equals(old.get(name)),"Original head/face record changed "+name);
      if(bone.has("morphology_pivots_055"))check(name.equals("blocky_legacy_tuft_055")&&bone.getAsJsonArray("morphology_pivots_055").size()==7,"A shared attachment pivot was changed");
      for(var me:bone.getAsJsonArray("meshes")){var m=me.getAsJsonObject();if(!m.has("blocky_box_055"))continue;
        check(m.getAsJsonArray("vertices").size()==8&&m.getAsJsonArray("faces").size()==6,"Blocky part is no longer an eight-vertex six-face box");
        var box=m.getAsJsonObject("blocky_box_055");for(String key:List.of("centers","sizes","rotations")){var values=box.getAsJsonArray(key);check(values.size()==7,"Missing corresponding box transforms");for(var v:values){check(v.getAsJsonArray().size()==3,"Invalid box vector");for(var a:v.getAsJsonArray())check(Float.isFinite(a.getAsFloat())&&(!key.equals("sizes")||a.getAsFloat()>=.01f),"Nonpositive or nonfinite box pose");}}
        Map<String,Integer> edges=new HashMap<>();for(var fe:m.getAsJsonArray("faces")){var face=fe.getAsJsonObject();check(face.getAsJsonArray("v").size()==4,"Added box gained a triangulated surface");check(!face.has("hero_run_uv_052"),"Genetic coat selected a separate UV path");var v=face.getAsJsonArray("v");for(int i=0;i<4;i++){int a=v.get(i).getAsInt(),b=v.get((i+1)%4).getAsInt();edges.merge(Math.min(a,b)+":"+Math.max(a,b),1,Integer::sum);}}
        check(edges.size()==12&&edges.values().stream().allMatch(n->n==2),"Added box is not a closed manifold");if(name.equals("gradual_head_021"))features.add(m.get("name").getAsString());
      }
    }
    check(features.size()==16&&features.containsAll(List.of("neutral_run_front_nub_left","neutral_run_front_nub_right","hero_run_fan_left_tip","hero_run_fan_right_tip")),"Approved baby nubs or later Hero fan missing");
    check(d.getAsJsonArray("head_decorations").equals(original.getAsJsonArray("head_decorations")),"Closed-hat decoration hiding changed");
  }

  private static final class ProtectedFace {
    final Map<String,Capture> bones=new HashMap<>();final float[] eyes,mouth,head;
    ProtectedFace(){var original=asset("gameplay/chao_baby");original.development(0,0,0);var p=poses(original);eyes=original.attachmentEyes(p);mouth=original.attachmentMouth(p);head=original.attachmentBounds("skull",p);for(String name:PROTECTED)bones.put(name,solid(original,name));}
    void verify(ChaoMeshAsset mesh,Map<String,ChaoMeshAsset.Pose> p){near(eyes,mesh.attachmentEyes(p),EPS,"Eyes moved in skull space");near(mouth,mesh.attachmentMouth(p),EPS,"Mouth moved in skull space");near(head,mesh.attachmentBounds("skull",p),EPS,"Original head/hat bounds changed");for(var entry:bones.entrySet())equal(entry.getValue(),solid(mesh,entry.getKey()),EPS,"Original emitted head/front/back/eye changed");}
  }

  private static void denseSweep(ChaoMeshAsset mesh,ProtectedFace face,boolean adult,float fixedRun,float fixedAge,float fixedHero,int varying){
    var p=poses(mesh);Capture before=null;Set<Long> quarter=new HashSet<>();
    for(int i=0;i<=100;i++){
      float q=i/100f;var s=state(adult,varying==0?q:fixedRun,varying==1?q:fixedAge,varying==2?q:fixedHero);String saved=s.encode();sample(mesh,s);var now=capture(mesh,p);frames++;
      check(saved.equals(s.encode()),"Rendering modified saved state");
      if(before!=null){check(before.n==now.n,"Development switched emitted topology");for(int a=0;a<now.n;a+=3){float dx=now.points[a]-before.points[a],dy=now.points[a+1]-before.points[a+1],dz=now.points[a+2]-before.points[a+2];float distance=(float)Math.sqrt(dx*dx+dy*dy+dz*dz);maximumStep=Math.max(maximumStep,distance);check(distance<.18f,"Small development step snapped geometry");}}
      if(i%25==0){face.verify(mesh,p);cuboids(solid(mesh,"gradual_head_021"));compactLimbs(mesh,true);normals(now);quarter.add(mesh.shapeSignature());}
      before=now;
    }
    if(varying==0&&fixedAge>0)check(quarter.size()==5,"Run quarter targets are not distinct");
  }

  private static void development(){
    var mesh=asset(MODEL);var face=new ProtectedFace();var p=poses(mesh);
    for(float age:QUARTERS)for(float run:QUARTERS)for(float alignment:new float[]{-1,0,.25f,.5f,1}){
      var s=state(false,run,age,alignment);s.swimFly=1;s.runPower=1;sample(mesh,s);face.verify(mesh,p);cuboids(solid(mesh,"gradual_head_021"));compactLimbs(mesh,true);frames++;
    }
    for(float age:new float[]{0,.5f,1})for(float hero:new float[]{0,.25f,1})denseSweep(mesh,face,false,0,age,hero,0);
    for(float run:new float[]{0,.5f,1})denseSweep(mesh,face,false,run,1,0,2);
    for(float run:new float[]{.5f,1})for(float hero:new float[]{0,1})denseSweep(mesh,face,false,run,0,hero,1);
    for(float age:QUARTERS)denseSweep(mesh,face,true,0,age,1,0);
    for(float run:new float[]{0,1})denseSweep(mesh,face,true,run,0,1,1);
    sample(mesh,state(false,1,0,1));long young=mesh.shapeSignature();sample(mesh,state(false,0,0,1));check(young==mesh.shapeSignature(),"A young baby ignored the independent visual-age gate");
    var adult=state(true,1,1,1);sample(mesh,adult);long full=mesh.shapeSignature();float[] matureBody=mesh.attachmentBounds("body",p);float maturity=adult.evolutionStrength();int age=adult.adultAgeTicks;
    DriveRules.setTotal(adult,ChaoState.RUN,0,new Random(1));sample(mesh,adult);check(full!=mesh.shapeSignature(),"Run loss did not reverse the specialized appearance");near(matureBody,mesh.attachmentBounds("body",p),EPS,"Run loss de-aged the mature body");check(adult.evolutionStrength()==maturity&&adult.adultAgeTicks==age,"Training loss changed biological maturity");
    var fresh=state(true,0,0,1);sample(mesh,fresh);check(mesh.attachmentBounds("body",p)[3]-mesh.attachmentBounds("body",p)[0]<matureBody[3]-matureBody[0],"Fresh and mature adult proportions collapsed into one form");
    var compatible=state(true,1,1,1);sample(mesh,compatible);long center=mesh.shapeSignature();var head=solid(mesh,"skull__cube_0");var crest=solid(mesh,"gradual_head_021");
    for(float sf:new float[]{-1,0,1})for(float power:new float[]{0,.5f,1}){compatible.swimFly=sf;compatible.runPower=power;sample(mesh,compatible);if(sf!=0||power!=0)check(center!=mesh.shapeSignature(),"Compatible/opposed legacy body-axis detail disappeared");equal(head,solid(mesh,"skull__cube_0"),EPS,"An opposing axis resized the head");equal(crest,solid(mesh,"gradual_head_021"),EPS,"Legacy body axes distorted the approved head features");compactLimbs(mesh,false);}
    mesh.hideHeadDecorations(p);near(face.head,mesh.attachmentBounds("skull",p),EPS,"Closed hat altered original head envelope");check(!p.get("gradual_head_021").visible&&!p.get("blocky_legacy_tuft_055").visible,"Closed hat left old/new crest visible");
  }

  private static void mutationRoutes(){
    var mesh=asset(MODEL);var p=poses(mesh);
    for(boolean adult:new boolean[]{false,true}){
      var direct=state(adult,0,.75f,1);var ordinary=state(adult,0,.75f,1);var combined=state(adult,0,.75f,1);var analyzer=state(adult,0,.75f,1);
      direct.train(ChaoState.RUN,240,new Random(1));for(int i=0;i<10;i++)ordinary.giveDrive(ChaoState.RUN);
      DriveRules.apply(combined,DriveRules.Tier.SOLID,ChaoState.RUN,new Random(1));DriveRules.change(combined,ChaoState.RUN,140,new Random(1));
      AnalyzerStatRules.apply(analyzer,ChaoState.RUN,AnalyzerStatRules.LEVEL,1);AnalyzerStatRules.apply(analyzer,ChaoState.RUN,AnalyzerStatRules.LEVEL,1);AnalyzerStatRules.apply(analyzer,ChaoState.RUN,AnalyzerStatRules.PROGRESS,40);
      sample(mesh,direct);var expected=capture(mesh,p);
      for(var s:List.of(direct,ordinary,combined,analyzer)){sample(mesh,s);equal(expected,capture(mesh,p),EPS,"Stat mutation routes selected different anatomy");String saved=s.encode();var loaded=ChaoState.decode(saved);sample(mesh,loaded);equal(expected,capture(mesh,p),EPS,"Save/reload changed emitted anatomy");check(saved.equals(loaded.encode()),"Read-only morphology introduced save data");}
      sample(mesh,analyzer);long prior=mesh.shapeSignature();float age=analyzer.evolutionStrength();AnalyzerStatRules.apply(analyzer,ChaoState.RUN,AnalyzerStatRules.LEVEL,-1);sample(mesh,analyzer);check(prior!=mesh.shapeSignature()&&age==analyzer.evolutionStrength(),"Creative decrease did not reverse geometry independently of age");
    }
  }

  private static void temporalEdits(){
    var mesh=asset(MODEL);var p=poses(mesh);var state=state(false,0,1,0);UUID actor=new UUID(55,550055);
    var first=HeroRunAppearance052.prepare(actor,state,0);mesh.heroRunDevelopment(first,0,0);var original=capture(mesh,p);
    state.align=1;DriveRules.setTotal(state,ChaoState.RUN,480,new Random(2));String saved=state.encode();
    var edit=HeroRunAppearance052.prepare(actor,state,1);mesh.heroRunDevelopment(edit,0,0);equal(original,capture(mesh,p),EPS,"A large alignment/training packet snapped the first display frame");
    Capture before=original;
    for(int tick=2;tick<=21;tick++){
      mesh.heroRunDevelopment(HeroRunAppearance052.prepare(actor,state,tick),0,0);var now=capture(mesh,p);editStep(before,now);before=now;frames++;
    }
    sample(mesh,state);equal(before,capture(mesh,p),EPS,"Client edit blend never reached the exact authoritative target");
    float age=state.evolutionStrength();state.align=0;DriveRules.setTotal(state,ChaoState.RUN,0,new Random(2));String reducedSave=state.encode();
    var reverse=HeroRunAppearance052.prepare(actor,state,22);mesh.heroRunDevelopment(reverse,0,0);equal(before,capture(mesh,p),EPS,"A reversing packet jumped back to a stale endpoint");
    for(int tick=23;tick<=42;tick++){
      mesh.heroRunDevelopment(HeroRunAppearance052.prepare(actor,state,tick),0,0);var now=capture(mesh,p);editStep(before,now);before=now;frames++;
    }
    sample(mesh,state);equal(before,capture(mesh,p),EPS,"Reversed client edit did not reach the exact lower target");equal(original,before,EPS,"Returning alignment/training did not recover the normal baby surface");
    check(state.evolutionStrength()==age&&reducedSave.equals(state.encode()),"Presentation interpolation changed saved maturity or training");
    check(!saved.equals(reducedSave),"Reversal fixture did not change authoritative training/alignment");
  }
  private static void editStep(Capture before,Capture now){
    check(before.n==now.n,"A packet edit swapped visible topology");
    for(int i=0;i<now.n;i+=3){float dx=now.points[i]-before.points[i],dy=now.points[i+1]-before.points[i+1],dz=now.points[i+2]-before.points[i+2];float distance=(float)Math.sqrt(dx*dx+dy*dy+dz*dz);maximumEditStep=Math.max(maximumEditStep,distance);check(distance<2,"Large client edit blend jumped more than two model pixels in one tick");}
  }

  private static void motionAndAttachments()throws Exception{
    var clips=JsonParser.parseString(Files.readString(ROOT.resolve("animations/official/chao.animation.json"))).getAsJsonObject().getAsJsonObject("animations");
    var rig=new OfficialChaoRig(clips,BlockyRunMorphRigTest055::asset);var expected=asset(MODEL);int serial=0;
    for(boolean adult:new boolean[]{false,true})for(float run:QUARTERS)for(var motion:List.of(OfficialChaoRig.Motion.CRAWL,OfficialChaoRig.Motion.WALK,OfficialChaoRig.Motion.RUN,OfficialChaoRig.Motion.SIT,OfficialChaoRig.Motion.SWIM,OfficialChaoRig.Motion.GLIDE,OfficialChaoRig.Motion.HELD))for(int tick=0;tick<=20;tick+=5){
      var s=state(adult,run,adult?run:1,1);String geometry=OfficialChaoRig.geometryKey(s);String saved=s.encode();rig.prepareDevelopment(geometry,s);rig.animateGameplay(geometry,new UUID(55,++serial),motion,tick,1,.05f,0,0,0,0,false);rig.development(s);sample(expected,s);motionFrames++;
      check(rig.finite(),"Actual Lionfish motion became nonfinite");check(rig.sampledMesh().shapeSignature()==expected.shapeSignature(),"Production selection did not use the approved055 asset");check(rig.sourceGeometryKey().equals("baby"),"Production UV/attachment family changed");check(saved.equals(s.encode()),"Motion sampling changed saved state");
      for(boolean left:new boolean[]{false,true}){float[] palm=rig.toyPalm(left);check(palm!=null&&palm.length==3,"Toy palm disappeared");for(float v:palm)check(Float.isFinite(v),"Toy grip became nonfinite");}
      for(String anchor:List.of("body","skull","arm_left","arm_right","foot_left","foot_right","tail","wing_left","wing_right")){var matrix=new PoseStack();rig.transformBone(anchor,matrix);check(matrix.last().pose().isFinite(),"Sampled animation anchor nonfinite");check(rig.attachmentBounds(anchor)!=null,"Sampled attachment bounds missing");}
      if(tick==0)for(String family:List.of("overlays/chao_baby","cwe_parts/chao_baby","hats/chao_baby","market_accessories")){
        var donor=asset(family);var attached=donor.createPoses();for(var entry:rig.poses().entrySet())if(attached.containsKey(entry.getKey()))rig.sampledMesh().copyJointPoseTo(entry.getKey(),donor,entry.getValue(),attached.get(entry.getKey()));
        for(String anchor:List.of("body","skull","arm_left","arm_right","foot_left","foot_right","tail","wing_left","wing_right"))if(attached.containsKey(anchor)){var a=new PoseStack();var b=new PoseStack();rig.transformBone(anchor,a);donor.transformBone(anchor,b,attached);near(a.last().pose().get(new float[16]),b.last().pose().get(new float[16]),.000008f,"Separate animal/hat/garment attachment detached "+family+" / "+anchor);attachments++;}
      }
    }
  }

  private static void coats(){
    var mesh=asset(MODEL);var p=poses(mesh);var face=new ProtectedFace();
    for(boolean adult:new boolean[]{false,true})for(float run:new float[]{0,.5f,1}){
      sample(mesh,state(adult,run,1,1));var reference=capture(mesh,p);
      for(int color=0;color<ChaoState.COLORS.length;color++)for(int jewel=0;jewel<ChaoState.JEWELS.length;jewel++)if(color==0||jewel==0)for(boolean shiny:new boolean[]{false,true})for(boolean twoTone:new boolean[]{false,true}){
        var s=state(adult,run,1,1);s.expressedColor=color;s.expressedJewel=jewel;s.shiny=shiny;s.twoTone=twoTone;sample(mesh,s);check(same(reference,capture(mesh,p)),"Color/jewel/mono/shiny changed anatomy, UVs or normals");face.verify(mesh,p);coats++;
      }
    }
  }

  public static void main(String[] args)throws Exception{
    long started=System.nanoTime();topology();development();mutationRoutes();temporalEdits();motionAndAttachments();coats();
    check(AdultMorphSnapshot053.snapshot().equals("da472d55f399960b560f03b95b6df96f3de76b26bd6af78f01c60a0492dc8782"),"Historical delivered052 adult asset/sampler changed");
    String report="Blocky Run055: "+checks+" checks; "+frames+" emitted development frames; "+boxSolids+" closed rectangular box samples; "+limbSolids+" compact limb samples; "+motionFrames+" production Lionfish motion frames; "+attachments+" separate attachment matrices; "+coats+" coat/shape cases; max1% step="+maximumStep+" model pixels; max20-tick-edit step="+maximumEditStep+" model pixels; seconds="+(System.nanoTime()-started)/1e9+". Original head/face/UVs and hat/eye/mouth anchors fixed; continuous rigid boxes; age/training reversal; current production selection; compatible axes; historical052 fingerprint passed. This is geometry/rig validation, not visual or multiplayer verification.\n";
    Files.createDirectories(Path.of("build/reports/lionfish"));Files.writeString(Path.of("build/reports/lionfish/blocky-run055-results.txt"),report);System.out.print(report);
  }
}
