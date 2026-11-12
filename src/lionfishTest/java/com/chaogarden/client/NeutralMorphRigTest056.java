package com.chaogarden.client;

import com.chaogarden.core.ChaoState;
import com.chaogarden.core.DriveRules;
import com.chaogarden.core.NeutralMorphology056;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import org.joml.Vector3f;

/** Emitted production geometry/animation checks, separate from visual inspection. */
public final class NeutralMorphRigTest056 {
  private static final Path ROOT=Path.of("src/main/resources/assets/chao_garden");
  private static final String MODEL="gameplay/chao_neutral_proto056";
  private static int checks,frames,solids,motionFrames,attachments,coatCases;
  private static float maxStep,maxEdit;
  private static void check(boolean ok,String why){checks++;if(!ok)throw new AssertionError(why);}
  private static void near(float a,float b,float e,String why){check(Float.isFinite(a)&&Float.isFinite(b)&&Math.abs(a-b)<=e,why+": "+a+" / "+b);}
  private static JsonObject data(String key)throws Exception{return JsonParser.parseString(Files.readString(ROOT.resolve("models/entity/"+key+".json"))).getAsJsonObject();}
  private static ChaoMeshAsset asset(String key){try{var m=ChaoMeshAsset.parse(data(key));m.lens(0xff101820);m.expression(1);return m;}catch(Exception e){throw new IllegalStateException(key,e);}}
  private static ChaoState state(float rp,float sf,float age){
    var s=new ChaoState();s.adult=true;s.primaryType=ChaoState.NORMAL;s.align=0;
    int r=Math.round(Math.abs(rp)*960),f=Math.round(Math.abs(sf)*960);
    int rs=rp<0?ChaoState.RUN:ChaoState.POWER,fs=sf<0?ChaoState.SWIM:ChaoState.FLY;
    s.levels[rs]=r/100;s.progress[rs]=r%100;s.points[rs]=r;
    s.levels[fs]=f/100;s.progress[fs]=f%100;s.points[fs]=f;
    s.setEvolutionStrength(age*1.2f);return s;
  }
  private static void sample(ChaoMeshAsset m,ChaoState s){m.development(s.swimFly,s.runPower,s.gradualVisualStrength());m.neutralDevelopment(NeutralMorphology056.sample(s));}
  private static Map<String,ChaoMeshAsset.Pose> poses(ChaoMeshAsset m){var p=m.createPoses();for(String n:List.of("emotion_exclamation","emotion_heart","emotion_question","emotion_dizzy","monster_crown"))m.setSubtreeVisible(n,p,false);return p;}
  private static final class Capture implements VertexConsumer {
    float[] p=new float[4096],uv=new float[4096],normal=new float[4096];int n,u,nn;
    public VertexConsumer addVertex(float x,float y,float z){check(Float.isFinite(x)&&Float.isFinite(y)&&Float.isFinite(z),"Nonfinite emitted position");if(n+3>p.length)p=Arrays.copyOf(p,p.length*2);p[n++]=x*16;p[n++]=y*16;p[n++]=z*16;return this;}
    public VertexConsumer setColor(int r,int g,int b,int a){return this;}
    public VertexConsumer setUv(float a,float b){check(Float.isFinite(a)&&Float.isFinite(b),"Nonfinite UV");if(u+2>uv.length)uv=Arrays.copyOf(uv,uv.length*2);uv[u++]=a;uv[u++]=b;return this;}
    public VertexConsumer setUv1(int a,int b){return this;}public VertexConsumer setUv2(int a,int b){return this;}
    public VertexConsumer setNormal(float x,float y,float z){check(Float.isFinite(x)&&Float.isFinite(y)&&Float.isFinite(z),"Nonfinite normal");if(nn+3>normal.length)normal=Arrays.copyOf(normal,normal.length*2);normal[nn++]=x;normal[nn++]=y;normal[nn++]=z;return this;}
    Vector3f point(int i){return new Vector3f(p[i*3],p[i*3+1],p[i*3+2]);}
    int count(){return n/3;}
  }
  private static Capture capture(ChaoMeshAsset m,Map<String,ChaoMeshAsset.Pose> p){var c=new Capture();m.render(new PoseStack(),c,0xf000f0,0,0xffffffff,p,true);check(c.n==c.nn&&c.u==c.count()*2,"Missing emitted UV/normal");return c;}
  private static Capture solid(ChaoMeshAsset m,String name){var p=m.createPoses();p.values().forEach(v->v.visible=false);check(p.containsKey(name),"Missing bone "+name);p.get(name).visible=true;return capture(m,p);}
  private static void equal(Capture a,Capture b,float epsilon,String why){check(a.n==b.n&&a.u==b.u,why+" topology changed");for(int i=0;i<a.n;i++)near(a.p[i],b.p[i],epsilon,why);for(int i=0;i<a.u;i++)near(a.uv[i],b.uv[i],0,why+" UV changed");}
  private static float volume(Capture c,int start){var center=new Vector3f();for(int i=start;i<start+24;i++)center.add(c.point(i));center.div(24);float sum=0;for(int i=start;i<start+24;i+=4){var a=c.point(i).sub(center);var b=c.point(i+1).sub(center);var q=c.point(i+2).sub(center);var d=c.point(i+3).sub(center);sum+=a.dot(new Vector3f(b).cross(q))/6+a.dot(new Vector3f(q).cross(d))/6;}return sum;}
  private static void cuboids(Capture c){
    check(c.count()%24==0,"Closed-box topology changed");
    for(int start=0;start<c.count();start+=24){check(volume(c,start)>.00000005f,"Cuboid collapsed/inverted");
      for(int i=start;i<start+24;i+=4){var a=c.point(i);var b=c.point(i+1);var q=c.point(i+2);var d=c.point(i+3);var ab=b.sub(a);var bc=q.sub(c.point(i+1));var cd=d.sub(c.point(i+2));var da=c.point(i).sub(c.point(i+3));
        check(ab.lengthSquared()>1e-7f&&bc.lengthSquared()>1e-7f,"Zero cuboid edge");
        check(Math.abs(ab.dot(bc))<.00005f*Math.max(1,ab.length()*bc.length()),"Rigid cuboid sheared");
        check(new Vector3f(ab).add(cd).length()<.00005f&&new Vector3f(bc).add(da).length()<.00005f,"Opposite box edges differ");
        var normal=new Vector3f(ab).cross(bc).normalize();near(normal.dot(new Vector3f(c.normal[i*3],c.normal[i*3+1],c.normal[i*3+2])),1,.0002f,"Normal differs from actual face");
      }solids++;
    }
  }
  private static void compact(ChaoMeshAsset m){
    for(String name:List.of("arm_left__cube_1","arm_right__cube_1","foot_left__cube_0","foot_right__cube_0")){
      var c=solid(m,name);cuboids(c);float[] lo={999,999,999},hi={-999,-999,-999};for(int i=0;i<c.n;i++){int a=i%3;lo[a]=Math.min(lo[a],c.p[i]);hi[a]=Math.max(hi[a],c.p[i]);}boolean foot=name.startsWith("foot");float min=999,max=0;for(int a=0;a<3;a++){min=Math.min(min,hi[a]-lo[a]);max=Math.max(max,hi[a]-lo[a]);}check(max/min<(foot?2.6f:1.4f),"Stretched hand/foot");if(foot)near(hi[1],24,.00005f,"Sole support plane moved");
    }
  }
  private static void topology()throws Exception{
    var d=data(MODEL);var original=data("gameplay/chao_baby");check(d.has("neutral_path_056")&&!d.has("blocky_run_path_055"),"Neutral path reused Hero selection");
    Map<String,JsonObject> old=new HashMap<>();for(var b:original.getAsJsonArray("bones"))old.put(b.getAsJsonObject().get("name").getAsString(),b.getAsJsonObject());
    for(var be:d.getAsJsonArray("bones")){var b=be.getAsJsonObject();String name=b.get("name").getAsString();check(b.get("pivot").equals(old.get(name).get("pivot"))&&b.get("rotation").equals(old.get(name).get("rotation")),"Shared rig/attachment pivot changed");if(name.startsWith("skull")||name.startsWith("expression_")||name.startsWith("emotion_"))check(b.equals(old.get(name)),"Original head/face/marker record changed "+name);}
    check(d.get("head_decorations").equals(original.get("head_decorations")),"Hat hiding family changed");
    check(d.getAsJsonObject("neutral_path_056").getAsJsonArray("targets").size()==10,"Wrong Neutral corner count");
  }
  private static void development(){
    var m=asset(MODEL);var original=asset("gameplay/chao_baby");var p=poses(m);var head=solid(original,"skull__cube_0");var eyesLeft=solid(original,"skull__cube_2");var eyesRight=solid(original,"skull__cube_3");
    var endpointShapes=new HashSet<Long>();
    for(float age:new float[]{0,.25f,.5f,.75f,1})for(int yi=0;yi<=10;yi++)for(int xi=0;xi<=10;xi++){
      var s=state(xi/5f-1,yi/5f-1,age);String saved=s.encode();sample(m,s);frames++;
      equal(head,solid(m,"skull__cube_0"),.00004f,"Original head resized");equal(eyesLeft,solid(m,"skull__cube_2"),.00004f,"Left eye moved/distorted");equal(eyesRight,solid(m,"skull__cube_3"),.00004f,"Right eye moved/distorted");
      cuboids(solid(m,"gradual_head_021"));compact(m);check(saved.equals(s.encode()),"Renderer changed saved data");
      if(age==1&&xi%5==0&&yi%5==0)endpointShapes.add(m.shapeSignature());
    }
    check(endpointShapes.size()==9,"Requested nine mature combinations collapsed into duplicate forms");
    for(float age:new float[]{0,.5f,1})for(float sf:new float[]{-1,0,1}){
      Capture previous=null;
      for(int i=0;i<=100;i++){sample(m,state(i/50f-1,sf,age));var now=capture(m,p);if(previous!=null){check(previous.n==now.n,"Axis sweep swapped topology");for(int n=0;n<now.n;n+=3){float dx=now.p[n]-previous.p[n],dy=now.p[n+1]-previous.p[n+1],dz=now.p[n+2]-previous.p[n+2];float step=(float)Math.sqrt(dx*dx+dy*dy+dz*dz);maxStep=Math.max(maxStep,step);check(step<.3f,"Small axis change popped geometry");}}previous=now;frames++;}
    }
    sample(m,state(-1,-1,0));long fresh=m.shapeSignature();sample(m,state(1,1,0));check(fresh==m.shapeSignature(),"Training overrode independent fresh-adult age");
    var s=state(-1,-1,1);sample(m,s);long before=m.shapeSignature();float maturity=s.evolutionStrength();DriveRules.setTotal(s,ChaoState.RUN,0,new Random(56));sample(m,s);check(before!=m.shapeSignature()&&maturity==s.evolutionStrength(),"Run loss did not reverse independently of maturity");
    var loaded=ChaoState.decode(s.encode());sample(m,loaded);long restored=m.shapeSignature();sample(m,s);check(restored==m.shapeSignature(),"Save/reload changed sampled geometry");
    m.hideHeadDecorations(p);check(!p.get("gradual_head_021").visible,"Hat did not hide crown additions");
  }
  private static void temporal(){
    var m=asset(MODEL);var p=poses(m);var s=state(0,0,1);UUID id=new UUID(56,560056);m.neutralDevelopment(NeutralAppearance056.prepare(id,s,0));var original=capture(m,p);DriveRules.setTotal(s,ChaoState.RUN,960,new Random(56));DriveRules.setTotal(s,ChaoState.SWIM,960,new Random(56));
    m.neutralDevelopment(NeutralAppearance056.prepare(id,s,1));equal(original,capture(m,p),.00004f,"Large packet snapped the first frame");Capture before=original;
    for(int t=2;t<=21;t++){m.neutralDevelopment(NeutralAppearance056.prepare(id,s,t));var now=capture(m,p);check(now.n==before.n,"Edit swapped visible topology");for(int n=0;n<now.n;n+=3){float dx=now.p[n]-before.p[n],dy=now.p[n+1]-before.p[n+1],dz=now.p[n+2]-before.p[n+2];float step=(float)Math.sqrt(dx*dx+dy*dy+dz*dz);maxEdit=Math.max(maxEdit,step);check(step<1,"Edit interpolation jumped a model pixel");}before=now;frames++;}
    sample(m,s);equal(before,capture(m,p),.00004f,"Client edit failed to reach authoritative geometry");
    DriveRules.setTotal(s,ChaoState.RUN,0,new Random(56));DriveRules.setTotal(s,ChaoState.SWIM,0,new Random(56));m.neutralDevelopment(NeutralAppearance056.prepare(id,s,22));equal(before,capture(m,p),.00004f,"Reversal used a stale display frame");
    for(int t=23;t<=42;t++)m.neutralDevelopment(NeutralAppearance056.prepare(id,s,t));equal(original,capture(m,p),.00004f,"Reversal failed to recover centered form");
  }
  private static void motion()throws Exception{
    var clips=JsonParser.parseString(Files.readString(ROOT.resolve("animations/official/chao.animation.json"))).getAsJsonObject().getAsJsonObject("animations");var rig=new OfficialChaoRig(clips,NeutralMorphRigTest056::asset);var expected=asset(MODEL);int serial=0;
    for(float age:new float[]{0,.5f,1})for(float sf:new float[]{-1,0,1})for(float rp:new float[]{-1,0,1})for(var mode:List.of(OfficialChaoRig.Motion.CRAWL,OfficialChaoRig.Motion.WALK,OfficialChaoRig.Motion.RUN,OfficialChaoRig.Motion.SIT,OfficialChaoRig.Motion.SWIM,OfficialChaoRig.Motion.GLIDE,OfficialChaoRig.Motion.HELD))for(int tick:new int[]{0,10,20}){
      var s=state(rp,sf,age);String saved=s.encode();String key=OfficialChaoRig.geometryKey(s);rig.prepareDevelopment(key,s);rig.animateGameplay(key,new UUID(56,++serial),mode,tick,1,.05f,0,0,0,0,false);rig.development(s);sample(expected,s);check(rig.finite(),"Lionfish pose nonfinite");check(rig.sampledMesh().shapeSignature()==expected.shapeSignature(),"Production selection skipped Neutral056");check(rig.sourceGeometryKey().equals("baby"),"UV/attachment family changed");check(saved.equals(s.encode()),"Motion changed saved state");motionFrames++;
      for(boolean left:new boolean[]{false,true}){float[] palm=rig.toyPalm(left);check(palm!=null&&palm.length==3,"Missing toy palm");for(float v:palm)check(Float.isFinite(v),"Toy palm nonfinite");}
      for(String anchor:List.of("body","skull","arm_left","arm_right","foot_left","foot_right","tail","wing_left","wing_right")){var matrix=new PoseStack();rig.transformBone(anchor,matrix);check(matrix.last().pose().isFinite(),"Attachment matrix nonfinite");check(rig.attachmentBounds(anchor)!=null,"Missing attachment envelope");}
      if(tick==0)for(String family:List.of("overlays/chao_baby","cwe_parts/chao_baby","hats/chao_baby","market_accessories")){var donor=asset(family);var attached=donor.createPoses();for(var entry:rig.poses().entrySet())if(attached.containsKey(entry.getKey()))rig.sampledMesh().copyJointPoseTo(entry.getKey(),donor,entry.getValue(),attached.get(entry.getKey()));for(String anchor:List.of("body","skull","arm_left","arm_right","foot_left","foot_right","tail","wing_left","wing_right"))if(attached.containsKey(anchor)){var a=new PoseStack();var b=new PoseStack();rig.transformBone(anchor,a);donor.transformBone(anchor,b,attached);float[] av=a.last().pose().get(new float[16]),bv=b.last().pose().get(new float[16]);for(int i=0;i<16;i++)near(av[i],bv[i],.000008f,"Separate accessory/animal anchor detached");attachments++;}}
    }
  }
  private static void coats(){
    var m=asset(MODEL);var p=poses(m);
    for(float rp:new float[]{-1,0,1})for(float sf:new float[]{-1,0,1}){var s=state(rp,sf,1);sample(m,s);var reference=capture(m,p);for(int color=0;color<ChaoState.COLORS.length;color++)for(int jewel=0;jewel<ChaoState.JEWELS.length;jewel++)if(color==0||jewel==0)for(boolean shiny:new boolean[]{false,true})for(boolean twoTone:new boolean[]{false,true}){s.expressedColor=color;s.expressedJewel=jewel;s.shiny=shiny;s.twoTone=twoTone;sample(m,s);equal(reference,capture(m,p),0,"Color/shine changed form/UV");coatCases++;}}
  }
  public static void main(String[] args)throws Exception{
    long start=System.nanoTime();topology();development();temporal();motion();coats();String report="Neutral056: "+checks+" checks; "+frames+" emitted age/axis/edit frames; "+solids+" closed rectangular-box samples; "+motionFrames+" production Lionfish motion frames; "+attachments+" separate attachment matrices; "+coatCases+" coat/shape cases; maximum axis step="+maxStep+" model pixels; maximum20tick edit step="+maxEdit+"; seconds="+(System.nanoTime()-start)/1e9+". Fixed original head/face/marker/rig; nine distinct mature corners; reversible independent age/axes; current production selection. Geometry validation, not visual/multiplayer verification.\n";Files.createDirectories(Path.of("build/reports/lionfish"));Files.writeString(Path.of("build/reports/lionfish/neutral056-results.txt"),report);System.out.print(report);
  }
}
