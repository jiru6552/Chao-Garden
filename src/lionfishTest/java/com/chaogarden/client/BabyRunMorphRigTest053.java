package com.chaogarden.client;

import com.chaogarden.core.ChaoState;
import com.chaogarden.core.HeroRunMorphology052;
import com.google.gson.JsonParser;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.nio.file.*;
import java.util.*;
import org.joml.Vector3f;

/** Actual emitted solids and shared anchors, rather than a second morph implementation. */
public final class BabyRunMorphRigTest053 {
  static final Path ROOT=Path.of("src/main/resources/assets/chao_garden");
  static int checks,frames,limbSolids;static float largestStep;
  static void check(boolean value,String reason){checks++;if(!value)throw new AssertionError(reason);}
  static ChaoMeshAsset asset(String name){try{return ChaoMeshAsset.parse(JsonParser.parseString(Files.readString(ROOT.resolve("models/entity/"+name+".json"))).getAsJsonObject());}catch(Exception e){throw new IllegalStateException(name,e);}}
  static ChaoState state(float run,float alignment){var s=new ChaoState();s.align=alignment;int total=Math.round(run*480);s.levels[2]=total/100;s.progress[2]=total%100;s.points[2]=total;s.setEvolutionStrength(.8f);return s;}
  static void sample(ChaoMeshAsset mesh,ChaoState s){mesh.development(s.swimFly,s.runPower,s.gradualVisualStrength()*.14f);mesh.heroRunDevelopment(HeroRunMorphology052.sample(s),s.swimFly,s.runPower);mesh.heroRunTwoTone(s.baseColor()==0&&s.jewel()==0&&s.isTwoTone());}
  static Map<String,ChaoMeshAsset.Pose> poses(ChaoMeshAsset mesh){var p=mesh.createPoses();for(String n:List.of("emotion_exclamation","emotion_heart","emotion_question","emotion_dizzy","monster_crown"))mesh.setSubtreeVisible(n,p,false);return p;}
  static final class Capture implements VertexConsumer {
    final List<Vector3f> points=new ArrayList<>();
    public VertexConsumer addVertex(float x,float y,float z){check(Float.isFinite(x)&&Float.isFinite(y)&&Float.isFinite(z),"nonfinite emitted vertex");points.add(new Vector3f(x*16,y*16,z*16));return this;}
    public VertexConsumer setColor(int r,int g,int b,int a){return this;}
    public VertexConsumer setUv(float u,float v){check(Float.isFinite(u)&&Float.isFinite(v),"nonfinite UV");return this;}
    public VertexConsumer setUv1(int u,int v){return this;}public VertexConsumer setUv2(int u,int v){return this;}
    public VertexConsumer setNormal(float x,float y,float z){check(Float.isFinite(x)&&Float.isFinite(y)&&Float.isFinite(z),"nonfinite surface normal");return this;}
  }
  static List<Vector3f> capture(ChaoMeshAsset mesh,Map<String,ChaoMeshAsset.Pose> p){var c=new Capture();mesh.render(new PoseStack(),c,0xf000f0,0,0xffffffff,p,true);return c.points;}
  static List<Vector3f> solid(ChaoMeshAsset mesh,String bone){var p=mesh.createPoses();p.values().forEach(v->v.visible=false);p.get(bone).visible=true;return capture(mesh,p);}
  static float[] bounds(List<Vector3f> points){float[] b={Float.POSITIVE_INFINITY,Float.POSITIVE_INFINITY,Float.POSITIVE_INFINITY,Float.NEGATIVE_INFINITY,Float.NEGATIVE_INFINITY,Float.NEGATIVE_INFINITY};for(var p:points){b[0]=Math.min(b[0],p.x);b[1]=Math.min(b[1],p.y);b[2]=Math.min(b[2],p.z);b[3]=Math.max(b[3],p.x);b[4]=Math.max(b[4],p.y);b[5]=Math.max(b[5],p.z);}return b;}
  static void near(float[] a,float[] b,float tolerance,String why){check(a!=null&&b!=null&&a.length==b.length,why+" missing");for(int i=0;i<a.length;i++)check(Math.abs(a[i]-b[i])<=tolerance,why+" axis"+i+" "+a[i]+"/"+b[i]);}
  static void coherentSolid(List<Vector3f> points,boolean foot,String name){
    check(points.size()==24,"canonical solid topology changed "+name);float[] b=bounds(points);var center=new Vector3f((b[0]+b[3])/2,(b[1]+b[4])/2,(b[2]+b[5])/2);float volume=0,orientation=0;
    for(int i=0;i<points.size();i+=4){var a=points.get(i);var q=points.get(i+1);var c=points.get(i+2);var d=points.get(i+3);var n=new Vector3f(q).sub(a).cross(new Vector3f(c).sub(a));check(n.length()>.99f,"thin/crossed face "+name);var opposite=new Vector3f(c).sub(a).cross(new Vector3f(d).sub(a));check(n.dot(opposite)>0,"bow-tie quad "+name);float facing=n.dot(new Vector3f(a).add(q).add(c).add(d).mul(.25f).sub(center));if(orientation==0)orientation=Math.signum(facing);check(facing*orientation>.5f,"inverted/non-convex solid "+name);volume+=a.dot(new Vector3f(q).cross(c))/6+a.dot(new Vector3f(c).cross(d))/6;}
    volume=Math.abs(volume);float x=b[3]-b[0],y=b[4]-b[1],z=b[5]-b[2],aspect=Math.max(x,Math.max(y,z))/Math.min(x,Math.min(y,z));
    check(volume>(foot?15.9f:7.9f),"solid lost volume "+name+" "+volume);check(aspect<(foot?2.01f:1.16f),"limb stretched "+name+" "+aspect);check(Math.abs(volume-x*y*z)<.003f,"limb is not coherent closed cuboid "+name);
    if(foot)check(Math.abs(b[4]-24)<.00002f,"sole moved off world support plane "+name+" "+b[4]);limbSolids++;
  }
  static void development(){
    var mesh=asset("gameplay/chao_baby_run_proto053");var p=poses(mesh);var baseline=asset("gameplay/chao_baby");baseline.development(0,0,0);var bp=poses(baseline);float[] eyes=baseline.attachmentEyes(bp);
    for(float alignment:new float[]{-1,0,.25f,.5f,1}){
      List<Vector3f> previous=null;Set<Long> signatures=new HashSet<>();
      for(int step=0;step<=400;step++){
        float run=step/400f;var s=state(run,alignment);s.runPower=1;s.swimFly=-1;sample(mesh,s);frames++;
        check(HeroRunMorphology052.sample(s).enabled(),"ordinary baby was excluded at alignment "+alignment);
        var now=capture(mesh,p);if(step%100==0)signatures.add(mesh.shapeSignature());
        near(eyes,mesh.attachmentEyes(p),.00001f,"eye plane wandered");
        for(String n:List.of("arm_left__cube_1","arm_right__cube_1","foot_left__cube_0","foot_right__cube_0"))coherentSolid(solid(mesh,n),n.startsWith("foot"),n+" "+step+"/"+alignment);
        if(previous!=null){check(now.size()==previous.size(),"morph topology changed");for(int i=0;i<now.size();i++){float d=now.get(i).distance(previous.get(i));largestStep=Math.max(largestStep,d);check(d<.06f,"small Run increment snapped "+d);}}
        previous=now;
        if(step==0)for(String n:List.of("body","skull","arm_left","arm_right","foot_left","foot_right"))near(mesh.attachmentBounds(n,p),baseline.attachmentBounds(n,bp),.00002f,"zero Run retained donor deformation "+n);
      }
      check(signatures.size()==5,"quarter stages not independently visible "+alignment);
    }
    var neutral=state(1,0);sample(mesh,neutral);float[] nr=mesh.attachmentBounds("skull",p);var hero=state(1,1);sample(mesh,hero);near(nr,mesh.attachmentBounds("skull",p),.00001f,"Hero suppressed independent Run head");
    check(nr[3]-nr[0]>10.3f&&nr[5]-nr[2]>10.7f,"maximum head did not gain broad swept silhouette "+Arrays.toString(nr));
    mesh.hideHeadDecorations(p);var hatHead=mesh.attachmentBounds("skull",p);check(hatHead[5]-hatHead[2]<7.2f,"closed hat retained swept decorations");near(eyes,mesh.attachmentEyes(p),.00001f,"hat suppression moved eyes");
  }
  static void attachmentsAndMotion()throws Exception{
    var mesh=asset("gameplay/chao_baby_run_proto053");
    for(String family:List.of("overlays/chao_baby","cwe_parts/chao_baby","hats/chao_baby","market_accessories")){
      var donor=asset(family);for(float alignment:new float[]{-1,0,1})for(int q=0;q<5;q++){
        sample(mesh,state(q/4f,alignment));var p=poses(mesh);var d=donor.createPoses();p.get("body").rx=.2f;p.get("body").ry=-.3f;p.get("skull").ry=.3f;
        for(var e:p.entrySet())if(d.containsKey(e.getKey()))mesh.copyJointPoseTo(e.getKey(),donor,e.getValue(),d.get(e.getKey()));
        for(String n:List.of("body","skull","arm_left","arm_right","foot_left","foot_right","tail","wing_left","wing_right"))if(d.containsKey(n)){var a=new PoseStack();var b=new PoseStack();mesh.transformBone(n,a,p);donor.transformBone(n,b,d);near(a.last().pose().get(new float[16]),b.last().pose().get(new float[16]),.000003f,"attachment joint separated "+family+" "+n);}
      }
    }
    var clips=JsonParser.parseString(Files.readString(ROOT.resolve("animations/official/chao.animation.json"))).getAsJsonObject().getAsJsonObject("animations");var rig=new OfficialChaoRig(clips,BabyRunMorphRigTest053::asset);int serial=0;
    for(float a:new float[]{-1,0,1})for(int q=0;q<5;q++)for(var motion:List.of(OfficialChaoRig.Motion.CRAWL,OfficialChaoRig.Motion.WALK,OfficialChaoRig.Motion.RUN,OfficialChaoRig.Motion.SIT,OfficialChaoRig.Motion.SWIM,OfficialChaoRig.Motion.GLIDE,OfficialChaoRig.Motion.HELD))for(int tick=0;tick<=40;tick+=2){var s=state(q/4f,a);String geometry=OfficialChaoRig.geometryKey(s);rig.prepareDevelopment(geometry,s);rig.animateGameplay(geometry,new UUID(530053,++serial),motion,tick,1,.05f,0,0,0,0,false);rig.development(s);frames++;check(rig.finite(),"motion nonfinite");check(rig.sourceGeometryKey().equals("baby"),"attachment UV family changed");sample(mesh,s);check(rig.sampledMesh().shapeSignature()==mesh.shapeSignature(),"runtime selected another baby surface");for(String n:List.of("skull","body","arm_left","foot_left")){var t=new PoseStack();rig.transformBone(n,t);check(t.last().pose().isFinite(),"motion joint nonfinite");check(rig.attachmentBounds(n)!=null,"motion attachment absent");}check(rig.toyPalm(false)!=null&&rig.toyPalm(true)!=null,"motion palm absent");}
  }
  static void signedAlignment(){var s=state(.75f,0);UUID id=UUID.randomUUID();HeroRunAppearance052.prepare(id,s,0);s.align=-1;String saved=s.encode();HeroRunAppearance052.prepare(id,s,1);HeroRunAppearance052.prepare(id,s,11);check(Math.abs(HeroRunAppearance052.signedAlignment(s)+.5f)<.00001f,"Dark edit did not interpolate");HeroRunAppearance052.prepare(id,s,21);check(HeroRunAppearance052.signedAlignment(s)==-1,"Dark edit did not reach endpoint");check(saved.equals(s.encode()),"presentation altered save");HeroRunAppearance052.prepare(id,s,150);check(HeroRunAppearance052.signedAlignment(s)==-1,"stale signed channel corrupted");}
  static void colors(){
    int source=0xccd3c569,hero=0xccf2f2ed,dark=0xcc503060;
    check(HeroRunCoatMaterial052.babySkinColor053(source,hero,dark,1,0,0,0,0)==source,"neutral zero color changed");
    check(HeroRunCoatMaterial052.babySkinColor053(source,hero,dark,1,0,0,1,0)==0xcc1ee58f,"Neutral Run body palette differs from reference family");
    check(HeroRunCoatMaterial052.babySkinColor053(source,hero,dark,1,1,0,1,0)==0xccedf2f2,"Hero Run body palette differs");
    check(HeroRunCoatMaterial052.babySkinColor053(source,hero,dark,1,0,0,1,1)==0xcc3de8c4,"Neutral Run accent differs");
    check(HeroRunCoatMaterial052.babySkinColor053(source,hero,dark,1,1,0,1,1)==0xccca9d65,"Hero Run accent differs");
    for(float run:new float[]{0,.25f,.5f,.75f,1})check(HeroRunCoatMaterial052.babySkinColor053(source,hero,dark,1,0,1,run,0)==dark,"Dark family was replaced by Neutral or Hero");
  }
  public static void main(String[] args)throws Exception{
    development();attachmentsAndMotion();signedAlignment();colors();
    check(AdultMorphSnapshot053.snapshot().equals("da472d55f399960b560f03b95b6df96f3de76b26bd6af78f01c60a0492dc8782"),"adult vertices/UVs/normals/joint matrices changed from delivered .52 JAR");
    String report="Baby Run053 morphology: "+checks+" checks; "+frames+" emitted-geometry/motion frames; "+limbSolids+" closed compact limb solids; max small-progress step="+largestStep+" model pixels; independent Run/alignment, swept head, protected eyes, attachments, grounded soles, signed interpolation and delivered052 adult fingerprint passed\n";
    Files.createDirectories(Path.of("build/reports/lionfish"));Files.writeString(Path.of("build/reports/lionfish/baby-run053-results.txt"),report);System.out.print(report);
  }
}
