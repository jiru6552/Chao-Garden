package com.chaogarden.client;

import com.chaogarden.core.*;
import com.google.gson.*;
import com.mojang.blaze3d.vertex.*;
import java.nio.file.*;
import java.util.*;
import org.joml.Vector3f;

/** Production geometry, source-derived ratios and shared rig anchors. */
public final class HeroRunMorphRigTest054 {
  static final Path ROOT=Path.of("src/main/resources/assets/chao_garden");
  static int checks,frames,solids,coats;static float maxStep;
  static void check(boolean ok,String why){checks++;if(!ok)throw new AssertionError(why);}
  static ChaoMeshAsset asset(String name){try{return ChaoMeshAsset.parse(JsonParser.parseString(Files.readString(ROOT.resolve("models/entity/"+name+".json"))).getAsJsonObject());}catch(Exception e){throw new IllegalStateException(name,e);}}
  static ChaoState state(boolean adult,float run,float maturity,float alignment){
    var s=new ChaoState();s.adult=adult;s.align=alignment;s.primaryType=adult?ChaoState.RUN_TYPE:ChaoState.NORMAL;
    int total=Math.round(run*(adult?960:480));s.levels[ChaoState.RUN]=total/100;s.progress[ChaoState.RUN]=total%100;s.points[ChaoState.RUN]=total;
    s.setEvolutionStrength(maturity*1.2f);return s;
  }
  static void sample(ChaoMeshAsset m,ChaoState s){m.development(s.swimFly,s.runPower,s.gradualVisualStrength());m.heroRunDevelopment(HeroRunMorphology052.sample(s),s.swimFly,s.runPower);m.heroRunTwoTone(s.baseColor()==0&&s.jewel()==0&&s.isTwoTone());}
  static Map<String,ChaoMeshAsset.Pose> poses(ChaoMeshAsset m){var p=m.createPoses();for(String n:List.of("emotion_exclamation","emotion_heart","emotion_question","emotion_dizzy","monster_crown"))m.setSubtreeVisible(n,p,false);return p;}
  static class Capture implements VertexConsumer {
    final List<Vector3f> points=new ArrayList<>(),normals=new ArrayList<>();final List<Float> uv=new ArrayList<>();
    public VertexConsumer addVertex(float x,float y,float z){check(Float.isFinite(x)&&Float.isFinite(y)&&Float.isFinite(z),"nonfinite position");points.add(new Vector3f(x*16,y*16,z*16));return this;}
    public VertexConsumer setColor(int r,int g,int b,int a){return this;}
    public VertexConsumer setUv(float u,float v){check(Float.isFinite(u)&&Float.isFinite(v),"nonfinite UV");uv.add(u);uv.add(v);return this;}
    public VertexConsumer setUv1(int u,int v){return this;}public VertexConsumer setUv2(int u,int v){return this;}
    public VertexConsumer setNormal(float x,float y,float z){check(Float.isFinite(x)&&Float.isFinite(y)&&Float.isFinite(z),"nonfinite normal");normals.add(new Vector3f(x,y,z));return this;}
  }
  static Capture capture(ChaoMeshAsset m,Map<String,ChaoMeshAsset.Pose> p){var c=new Capture();m.render(new PoseStack(),c,0xf000f0,0,0xffffffff,p,true);return c;}
  static Capture solid(ChaoMeshAsset m,String bone){var p=m.createPoses();p.values().forEach(v->v.visible=false);p.get(bone).visible=true;return capture(m,p);}
  static float[] bounds(List<Vector3f> points){float[] b={Float.POSITIVE_INFINITY,Float.POSITIVE_INFINITY,Float.POSITIVE_INFINITY,Float.NEGATIVE_INFINITY,Float.NEGATIVE_INFINITY,Float.NEGATIVE_INFINITY};for(var p:points)for(int a=0;a<3;a++){b[a]=Math.min(b[a],p.get(a));b[a+3]=Math.max(b[a+3],p.get(a));}return b;}
  static void near(float[] a,float[] b,float eps,String why){check(a!=null&&b!=null&&a.length==b.length,why);for(int i=0;i<a.length;i++)check(Math.abs(a[i]-b[i])<eps,why+" "+i+" "+a[i]+"/"+b[i]);}
  static void normals(Capture c){for(int i=0;i<c.points.size();i+=4){var a=c.points.get(i);var n=new Vector3f(c.points.get(i+1)).sub(a).cross(new Vector3f(c.points.get(i+2)).sub(a));if(n.lengthSquared()<1e-7)n=new Vector3f(c.points.get(i+2)).sub(a).cross(new Vector3f(c.points.get(i+3)).sub(a));if(n.lengthSquared()<1e-7)continue;n.normalize();check(n.dot(c.normals.get(i))>.9995f,"deformed normal still describes old cube quad="+i/4+" dot="+n.dot(c.normals.get(i))+" pts="+c.points.subList(i,i+4)+" normal="+c.normals.get(i));}}
  static void coherent(Capture c,boolean foot,String name){
    var b=bounds(c.points);float volume=0;
    for(int i=0;i<c.points.size();i+=4){var a=c.points.get(i);var p=c.points.get(i+1);var q=c.points.get(i+2);var d=c.points.get(i+3);volume+=a.dot(new Vector3f(p).cross(q))/6+a.dot(new Vector3f(q).cross(d))/6;}
    float x=b[3]-b[0],y=b[4]-b[1],z=b[5]-b[2];
    check(volume>(foot?9:3),"solid lost outward volume "+name+" "+volume);
    check(Math.max(x,Math.max(y,z))/Math.min(x,Math.min(y,z))<(foot?2.6f:1.7f),"stretched limb "+name);
    if(foot)check(Math.abs(b[4]-24)<.00003f,"sole lost floor contact "+name);
    normals(c);solids++;
  }
  static void topology()throws Exception{
    var d=JsonParser.parseString(Files.readString(ROOT.resolve("models/entity/gameplay/chao_hero_run_proto054.json"))).getAsJsonObject();
    check(d.getAsJsonObject("hero_run_prototype_054").getAsJsonArray("endpoints").size()==7,"path endpoint count");
    for(var be:d.getAsJsonArray("bones"))for(var me:be.getAsJsonObject().getAsJsonArray("meshes")){
      var m=me.getAsJsonObject();var targets=m.getAsJsonArray("morphology_vertices_054");check(targets.size()==7,"missing common targets");for(var t:targets)check(t.getAsJsonArray().size()==m.getAsJsonArray("vertices").size(),"target correspondence changed");
      if(!m.get("name").getAsString().contains("054"))continue;
      Map<String,Integer> edges=new HashMap<>();
      for(var fe:m.getAsJsonArray("faces")){var f=fe.getAsJsonObject();check(!f.has("hero_run_uv_052"),"natural-only geometry UV path remains");var v=f.getAsJsonArray("v");for(int i=0;i<v.size();i++){int a=v.get(i).getAsInt(),b=v.get((i+1)%v.size()).getAsInt();String k=Math.min(a,b)+":"+Math.max(a,b);edges.merge(k,1,Integer::sum);
        String bone=be.getAsJsonObject().get("name").getAsString();
        if(bone.equals("arm_left__cube_1")||bone.equals("arm_right__cube_1")){
          float vv=f.getAsJsonArray("uv").get(i).getAsJsonArray().get(1).getAsFloat();
          if(a<8)check(vv<16.5f,"proximal hand cap no longer reads white/base end of longitudinal bands");
          if(a>=24)check(vv>23.5f,"distal hand cap no longer reads colored end of longitudinal bands");
        }
      }}
      if(!m.get("name").getAsString().equals("orb_054"))for(int count:edges.values())check(count==2,"open/nonmanifold canonical surface");
    }
  }
  static void stages(){
    var mesh=asset("gameplay/chao_hero_run_proto054");var p=poses(mesh);var old=asset("gameplay/chao_baby");float[] eyes=old.attachmentEyes(poses(old));
    for(boolean adult:new boolean[]{false,true})for(float align:adult?new float[]{1}:new float[]{-1,0,.25f,.5f,1})for(float maturity:adult?new float[]{0,.25f,.5f,.75f,1}:new float[]{.5f}){
      Capture previous=null;Set<Long> quarter=new HashSet<>();
      for(int step=0;step<=100;step++){
        var s=state(adult,step/100f,maturity,align);s.runPower=1;s.swimFly=-1;String save=s.encode();sample(mesh,s);frames++;
        var now=capture(mesh,p);near(eyes,mesh.attachmentEyes(p),.00001f,"face/eyes shifted");
        check(save.equals(s.encode()),"rendering mutated save");if(step%25==0)quarter.add(mesh.shapeSignature());
        for(String n:List.of("arm_left__cube_1","arm_right__cube_1","foot_left__cube_0","foot_right__cube_0"))coherent(solid(mesh,n),n.startsWith("foot"),n);
        if(previous!=null){check(now.points.size()==previous.points.size(),"topology switched with progress");for(int i=0;i<now.points.size();i++){float distance=now.points.get(i).distance(previous.points.get(i));maxStep=Math.max(maxStep,distance);check(distance<.15f,"development snapped");}}
        if(step%25==0)normals(now);previous=now;
      }
      if(!adult||maturity>0)check(quarter.size()==5,"quarter stages indistinguishable");
    }
    sample(mesh,state(true,0,0,1));var young=mesh.attachmentBounds("skull",p);var youngFoot=bounds(solid(mesh,"foot_left__cube_0").points);var youngHand=bounds(solid(mesh,"arm_left__cube_1").points);
    sample(mesh,state(true,1,1,1));var full=mesh.attachmentBounds("skull",p);var fullFoot=bounds(solid(mesh,"foot_left__cube_0").points);var fullHand=bounds(solid(mesh,"arm_left__cube_1").points);
    check(Math.abs((full[5]-full[2])/(young[5]-young[2])-1.3020478f)<.002f,"source rear depth ratio lost");
    check(Math.abs((fullFoot[3]-fullFoot[0])/(youngFoot[3]-youngFoot[0])-1.397f)<.002f,"source foot widening lost");
    check(Math.abs((fullFoot[5]-fullFoot[2])/(youngFoot[5]-youngFoot[2])-1.178f)<.002f,"source foot depth growth lost");
    check(Math.abs((fullHand[4]-fullHand[1])/(youngHand[4]-youngHand[1])-1.1329f)<.002f,"source compact hand growth lost");
    mesh.hideHeadDecorations(p);check(mesh.attachmentBounds("skull",p)!=null,"covered head envelope absent");
  }
  static void motionAndAttachments()throws Exception{
    var clips=JsonParser.parseString(Files.readString(ROOT.resolve("animations/official/chao.animation.json"))).getAsJsonObject().getAsJsonObject("animations");
    var rig=new OfficialChaoRig(clips,HeroRunMorphRigTest054::asset);int serial=0;
    for(boolean adult:new boolean[]{false,true})for(float r:new float[]{0,.25f,.5f,.75f,1})for(float a:adult?new float[]{1}:new float[]{-1,0,1})for(var motion:List.of(OfficialChaoRig.Motion.CRAWL,OfficialChaoRig.Motion.WALK,OfficialChaoRig.Motion.RUN,OfficialChaoRig.Motion.SIT,OfficialChaoRig.Motion.SWIM,OfficialChaoRig.Motion.GLIDE,OfficialChaoRig.Motion.HELD))for(int tick=0;tick<=30;tick+=5){
      var s=state(adult,r,adult?r:.5f,a);String geometry=OfficialChaoRig.geometryKey(s);rig.prepareDevelopment(geometry,s);rig.animateGameplay(geometry,new UUID(54,++serial),motion,tick,1,.05f,0,0,0,0,false);rig.development(s);frames++;
      check(rig.finite(),"nonfinite motion");var expected=asset("gameplay/chao_hero_run_proto054");sample(expected,s);check(rig.sampledMesh().shapeSignature()==expected.shapeSignature(),"production did not select unified54 mesh");
      check(rig.toyPalm(false)!=null&&rig.toyPalm(true)!=null,"toy palms absent");
      if(tick==0)for(String family:List.of("overlays/chao_baby","cwe_parts/chao_baby","hats/chao_baby","market_accessories")){
        var donor=asset(family);var dp=donor.createPoses();for(var e:rig.poses().entrySet())if(dp.containsKey(e.getKey()))rig.sampledMesh().copyJointPoseTo(e.getKey(),donor,e.getValue(),dp.get(e.getKey()));
        for(String n:List.of("body","skull","arm_left","foot_left","tail","wing_left"))if(dp.containsKey(n)){var x=new PoseStack();var y=new PoseStack();rig.transformBone(n,x);donor.transformBone(n,y,dp);near(x.last().pose().get(new float[16]),y.last().pose().get(new float[16]),.000004f,"replacement/accessory bind detached "+family+" "+n);}
      }
    }
  }
  static void coats(){
    var m=asset("gameplay/chao_hero_run_proto054");var p=poses(m);
    for(boolean adult:new boolean[]{false,true})for(float r:new float[]{0,1}){
      var reference=state(adult,r,1,1);sample(m,reference);var expected=capture(m,p);
      for(int color=0;color<ChaoState.COLORS.length;color++)for(int jewel=0;jewel<ChaoState.JEWELS.length;jewel++)if(color==0||jewel==0)for(boolean shiny:new boolean[]{false,true})for(boolean two:new boolean[]{false,true}){
        var s=state(adult,r,1,1);s.expressedColor=color;s.expressedJewel=jewel;s.shiny=shiny;s.twoTone=two;sample(m,s);var actual=capture(m,p);check(expected.points.equals(actual.points)&&expected.uv.equals(actual.uv)&&expected.normals.equals(actual.normals),"coat selected different geometry or UV mapping");coats++;
      }
    }
  }
  public static void main(String[] args)throws Exception{
    topology();stages();motionAndAttachments();coats();
    check(AdultMorphSnapshot053.snapshot().equals("da472d55f399960b560f03b95b6df96f3de76b26bd6af78f01c60a0492dc8782"),"historical adult052 fixture changed");
    String report="Hero Run054: "+checks+" checks; "+frames+" actual emitted geometry/motion frames; "+solids+" closed compact limb samples; "+coats+" coat/shape cases; maximum 1% step="+maxStep+" model pixels; unified production path, source ratios, deformed normals, protected eyes, save invariance, soles and shared attachment matrices passed.\n";
    Files.createDirectories(Path.of("build/reports/lionfish"));Files.writeString(Path.of("build/reports/lionfish/hero-run054-results.txt"),report);System.out.print(report);
  }
}
