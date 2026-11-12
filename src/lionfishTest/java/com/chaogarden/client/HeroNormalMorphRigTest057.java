package com.chaogarden.client;

import com.chaogarden.core.ChaoState;
import com.chaogarden.core.DriveRules;
import com.chaogarden.core.HeroNormalMorphology057;
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
public final class HeroNormalMorphRigTest057 {
  private static final Path ROOT=Path.of("src/main/resources/assets/chao_garden");
  private static final String MODEL="gameplay/chao_hero_normal_proto057";
  private static int checks,frames,solids,motionFrames,attachments,coatCases,haloPairs,wingSamples,hingeSamples,tipPlanePairs,bookSurfaceFrames;
  private static float maxStep,maxEdit,maxHinge;
  private static float minTipFaceGap=Float.POSITIVE_INFINITY;
  private static final Map<String,Vector3f> ORIGINAL_WING_HINGES=new HashMap<>();
  private static void check(boolean ok,String why){checks++;if(!ok)throw new AssertionError(why);}
  private static void near(float a,float b,float e,String why){check(Float.isFinite(a)&&Float.isFinite(b)&&Math.abs(a-b)<=e,why+": "+a+" / "+b);}
  private static JsonObject data(String key)throws Exception{return JsonParser.parseString(Files.readString(ROOT.resolve("models/entity/"+key+".json"))).getAsJsonObject();}
  private static ChaoMeshAsset asset(String key){try{var m=ChaoMeshAsset.parse(data(key));m.lens(0xff101820);m.expression(1);return m;}catch(Exception e){throw new IllegalStateException(key,e);}}
  private static ChaoState state(float rp,float sf,float age){
    var s=new ChaoState();s.adult=true;s.primaryType=ChaoState.NORMAL;s.align=1;
    int r=Math.round(Math.abs(rp)*960),f=Math.round(Math.abs(sf)*960);
    int rs=rp<0?ChaoState.RUN:ChaoState.POWER,fs=sf<0?ChaoState.SWIM:ChaoState.FLY;
    s.levels[rs]=r/100;s.progress[rs]=r%100;s.points[rs]=r;
    s.levels[fs]=f/100;s.progress[fs]=f%100;s.points[fs]=f;
    s.setEvolutionStrength(age*1.2f);return s;
  }
  private static void sample(ChaoMeshAsset m,ChaoState s){m.development(s.swimFly,s.runPower,s.gradualVisualStrength());m.heroNormalDevelopment(HeroNormalMorphology057.sample(s));}
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
  private static Capture select(ChaoMeshAsset m,Map<String,ChaoMeshAsset.Pose> p,String name){
    Map<String,Boolean> visibility=new HashMap<>();p.forEach((n,v)->visibility.put(n,v.visible));
    try{p.values().forEach(v->v.visible=false);check(p.containsKey(name),"Missing sampled Halo/head bone "+name);p.get(name).visible=true;return capture(m,p);}
    finally{p.forEach((n,v)->v.visible=visibility.get(n));}
  }
  /** Independent convex-box SAT. Actual edge directions, rather than face
   * normals treated as edges, also cover inherited nonuniform parent scales. */
  private static Vector3f[] boxEdges(Capture c,int start){
    var front=new Vector3f();var back=new Vector3f();
    for(int i=0;i<4;i++){front.add(c.point(start+i));back.add(c.point(start+4+i));}
    return new Vector3f[]{c.point(start+1).sub(c.point(start)),c.point(start+2).sub(c.point(start+1)),back.sub(front).div(4)};
  }
  private static boolean intersects(Capture a,int ai,Capture b,int bi){
    var axes=new java.util.ArrayList<Vector3f>();
    for(int face=0;face<6;face++){
      int an=(ai+face*4)*3,bn=(bi+face*4)*3;
      axes.add(new Vector3f(a.normal[an],a.normal[an+1],a.normal[an+2]));axes.add(new Vector3f(b.normal[bn],b.normal[bn+1],b.normal[bn+2]));
    }
    for(var x:boxEdges(a,ai))for(var y:boxEdges(b,bi))axes.add(new Vector3f(x).cross(y));
    for(var axis:axes){if(axis.lengthSquared()<1e-12f)continue;axis.normalize();float alo=99999,ahi=-99999,blo=99999,bhi=-99999;
      for(int v=0;v<24;v++){float ap=a.point(ai+v).dot(axis),bp=b.point(bi+v).dot(axis);alo=Math.min(alo,ap);ahi=Math.max(ahi,ap);blo=Math.min(blo,bp);bhi=Math.max(bhi,bp);}
      if(Math.min(ahi,bhi)-Math.max(alo,blo)<.000001f)return false;
    }return true;
  }
  private static void haloClearance(ChaoMeshAsset m,Map<String,ChaoMeshAsset.Pose> p){
    var head=select(m,p,"skull__cube_0");var crowns=select(m,p,"gradual_head_021");check(head.count()==24&&crowns.count()==96,"Paired root/cap topology changed");
    tipFaceSeparation(m,p,crowns);
    for(String name:List.of("emotion_dot__cube_0","hero_normal_halo_057_1","hero_normal_halo_057_2","hero_normal_halo_057_3")){
      var bar=select(m,p,name);check(bar.count()==24,"Halo bar is not one closed box");cuboids(bar);
      check(!intersects(bar,0,head,0),"Actual tilted halo intersects fixed skull");haloPairs++;
      for(int cap=0;cap<4;cap++){check(!intersects(bar,0,crowns,cap*24),"Actual tilted halo intersects paired root/cap");haloPairs++;}
    }
  }
  private static float[] bounds(Capture c,int start,int count){
    float[] b={Float.POSITIVE_INFINITY,Float.POSITIVE_INFINITY,Float.POSITIVE_INFINITY,
        Float.NEGATIVE_INFINITY,Float.NEGATIVE_INFINITY,Float.NEGATIVE_INFINITY};
    for(int v=start;v<start+count;v++)for(int axis=0;axis<3;axis++){
      float value=c.p[v*3+axis];b[axis]=Math.min(b[axis],value);b[axis+3]=Math.max(b[axis+3],value);
    }return b;
  }
  private static void mirrored(Capture left,Capture right,String why){
    check(left.count()==right.count(),why+" topology differs");
    for(int i=0;i<left.count();i++){
      var p=left.point(i);boolean found=false;
      for(int j=0;j<right.count();j++){var q=right.point(j);if(Math.abs(p.x+q.x)<.00005f&&Math.abs(p.y-q.y)<.00005f&&Math.abs(p.z-q.z)<.00005f){found=true;break;}}
      check(found,why+" has an unpaired emitted corner");
    }
  }
  private static void wings(ChaoMeshAsset m,Map<String,ChaoMeshAsset.Pose> p,boolean symmetric){
    var left=select(m,p,"wing_left__cube_0");var right=select(m,p,"wing_right__cube_0");
    check(left.count()==24&&right.count()==24,"Hero wings must be two positive six-face boxes");
    // Inherited breathing squashes the BODY nonuniformly. Combined with a
    // wing flap that valid parent affine transform can shear a world box.
    // Test intrinsic box rigidity in the actual wing-joint frame, while
    // keeping world-space volume, planarity, winding and attachment checks.
    if(symmetric){cuboids(left);cuboids(right);}
    else{affineSurfaces(left);affineSurfaces(right);cuboids(inJointFrame(m,p,"wing_left",left));cuboids(inJointFrame(m,p,"wing_right",right));}
    wingSamples+=2;
    if(symmetric)mirrored(left,right,"Hero wing pair");
    wingHinge(m,p,"wing_left",left);wingHinge(m,p,"wing_right",right);
    var torso=select(m,p,"body__cube_0");
    check(intersects(left,0,torso,0)&&intersects(right,0,torso,0),"A rendered wing root detached from the actual torso");
  }
  private static Capture inJointFrame(ChaoMeshAsset m,Map<String,ChaoMeshAsset.Pose> p,String joint,Capture world){
    var transform=new PoseStack();m.transformBone(joint,transform,p);
    var inverse=new org.joml.Matrix4f(transform.last().pose()).invert();
    // n_world=A^-T n_local, so returning normals to local needs A^T.
    var normalBack=new org.joml.Matrix3f(transform.last().pose()).transpose();
    var local=new Capture();local.n=world.n;local.u=world.u;local.nn=world.nn;
    local.p=Arrays.copyOf(world.p,world.n);local.uv=Arrays.copyOf(world.uv,world.u);local.normal=Arrays.copyOf(world.normal,world.nn);
    for(int i=0;i<world.count();i++){
      var point=world.point(i).div(16);inverse.transformPosition(point);point.mul(16);local.p[i*3]=point.x;local.p[i*3+1]=point.y;local.p[i*3+2]=point.z;
      var normal=new Vector3f(world.normal[i*3],world.normal[i*3+1],world.normal[i*3+2]);normalBack.transform(normal).normalize();local.normal[i*3]=normal.x;local.normal[i*3+1]=normal.y;local.normal[i*3+2]=normal.z;
    }return local;
  }
  /** Front/back root/cap surfaces must remain distinct after the production
   * emitter and any actual parent transform. Equal-depth overlapping faces
   * visibly z-fought in the Power+Swim book view; checking target JSON alone
   * would miss a sampler/cache or transform regression. */
  private static void tipFaceSeparation(ChaoMeshAsset m,Map<String,ChaoMeshAsset.Pose> p){
    tipFaceSeparation(m,p,select(m,p,"gradual_head_021"));
  }
  private static void tipFaceSeparation(ChaoMeshAsset m,Map<String,ChaoMeshAsset.Pose> p,Capture world){
    check(world.count()==96,"Root/cap depth check lost its two original feature pairs");
    var local=inJointFrame(m,p,"gradual_head_021",world);
    for(int root:new int[]{0,48}){
      var lower=emittedDepthPlanes(local,root);var upper=emittedDepthPlanes(local,root+24);
      float frontGap=lower[0]-upper[0],backGap=upper[1]-lower[1];
      float excess=(upper[1]-upper[0])-(lower[1]-lower[0]);
      // The authored .08px thickness margin yields .04px per face. These
      // margins allow only inverse-transform float noise, not coplanarity.
      check(excess>=.0798f,"Emitted upper cap is no thicker than its lower root: "+excess);
      check(frontGap>=.0399f&&backGap>=.0399f,
          "Overlapping root/cap front/back surfaces can z-fight: "+frontGap+" / "+backGap);
      minTipFaceGap=Math.min(minTipFaceGap,Math.min(frontGap,backGap));tipPlanePairs++;
    }
  }
  private static float[] emittedDepthPlanes(Capture local,int start){
    float low=Float.POSITIVE_INFINITY,high=Float.NEGATIVE_INFINITY;int faces=0;
    for(int face=0;face<6;face++){
      int first=start+face*4,at=first*3;
      var normal=new Vector3f(local.normal[at],local.normal[at+1],local.normal[at+2]);
      if(Math.abs(normal.z)<.9999f)continue;
      check(Math.abs(normal.x)<.0001f&&Math.abs(normal.y)<.0001f,
          "Root/cap fore-back surface no longer follows the shared local depth axis");
      float plane=local.point(first).z;
      for(int v=1;v<4;v++)near(local.point(first+v).z,plane,.00005f,
          "Root/cap fore-back surface is not planar after rendering");
      low=Math.min(low,plane);high=Math.max(high,plane);faces++;
    }
    check(faces==2&&high-low>.01f,"Missing positive front/back feature surfaces");
    return new float[]{low,high};
  }
  private static void affineSurfaces(Capture c){
    check(c.count()==24&&volume(c,0)>.00000005f,"Animated world wing collapsed/inverted");
    for(int i=0;i<24;i+=4){
      var a=c.point(i);var ab=c.point(i+1).sub(a);var bc=c.point(i+2).sub(c.point(i+1));var cd=c.point(i+3).sub(c.point(i+2));var da=a.sub(c.point(i+3));
      check(ab.lengthSquared()>1e-7f&&bc.lengthSquared()>1e-7f,"Animated world wing lost an edge");
      check(new Vector3f(ab).add(cd).length()<.00005f&&new Vector3f(bc).add(da).length()<.00005f,"Animated world wing lost opposite-edge correspondence");
      var normal=new Vector3f(ab).cross(bc).normalize();
      near(normal.dot(new Vector3f(c.normal[i*3],c.normal[i*3+1],c.normal[i*3+2])),1,.0002f,"Animated world wing normal/winding differs from rendered face");
      near(new Vector3f(c.point(i+3)).sub(c.point(i)).dot(normal),0,.00005f,"Animated world wing face is not planar");
    }
  }
  private static void wingHinge(ChaoMeshAsset m,Map<String,ChaoMeshAsset.Pose> p,String joint,Capture wing){
    var expected=ORIGINAL_WING_HINGES.get(joint);check(expected!=null,"Missing original proximal wing edge");
    var transform=new PoseStack();m.transformBone(joint,transform,p);var inverse=new org.joml.Matrix4f(transform.last().pose()).invert();
    var points=new java.util.ArrayList<Vector3f>();
    for(int i=0;i<wing.count();i++){var point=wing.point(i).div(16);inverse.transformPosition(point);points.add(point.mul(16));}
    points.sort(java.util.Comparator.comparingDouble(v->v.distanceSquared(expected)));
    // Each of the four proximal corners occurs on three emitted faces. Their
    // center must stay on the old source edge while the distal span grows.
    var actual=new Vector3f();for(int i=0;i<12;i++)actual.add(points.get(i));actual.div(12);
    float drift=actual.distance(expected);maxHinge=Math.max(maxHinge,drift);hingeSamples++;
    check(drift<.001f,"Visible wing root slid away from its original proximal edge: "+drift);
  }
  private static void wingUv(JsonObject bone,JsonObject original){
    check(bone.get("parent").equals(original.get("parent")),"Original wing parent changed");
    var meshes=bone.getAsJsonArray("meshes");check(meshes.size()==1,"Wing became a segmented construction");
    var mesh=meshes.get(0).getAsJsonObject();var old=original.getAsJsonArray("meshes").get(0).getAsJsonObject();
    var hinge=new Vector3f();int hingeVertices=0;
    for(var value:old.getAsJsonArray("vertices")){var v=value.getAsJsonArray();if(Math.abs(v.get(0).getAsFloat())<.000001f){hinge.add(v.get(0).getAsFloat(),v.get(1).getAsFloat(),v.get(2).getAsFloat());hingeVertices++;}}
    check(hingeVertices==4,"Original wing no longer exposes its canonical inner edge");
    ORIGINAL_WING_HINGES.put(original.get("parent").getAsString(),hinge.div(hingeVertices));
    check(mesh.has("hero_normal_box_057")&&mesh.getAsJsonArray("vertices").size()==8,"Wing does not share the ten-target rigid-box evaluator");
    var faces=mesh.getAsJsonArray("faces");check(faces.size()==6,"Wing back/edge surfaces missing");
    var front=old.getAsJsonArray("faces").get(0).getAsJsonObject().getAsJsonArray("uv");
    check(faces.get(0).getAsJsonObject().get("uv").equals(front),"Original visible wing front UVs changed");
    float minU=999,minV=999,maxU=-999,maxV=-999;
    for(var value:front){var uv=value.getAsJsonArray();minU=Math.min(minU,uv.get(0).getAsFloat());maxU=Math.max(maxU,uv.get(0).getAsFloat());minV=Math.min(minV,uv.get(1).getAsFloat());maxV=Math.max(maxV,uv.get(1).getAsFloat());}
    // The old reverse island is fully transparent. This family intentionally
    // mirrors its back into the existing visible island, without editing alpha.
    Map<Integer,com.google.gson.JsonArray> frontByVertex=new HashMap<>();var frontIds=old.getAsJsonArray("faces").get(0).getAsJsonObject().getAsJsonArray("v");
    for(int i=0;i<front.size();i++)frontByVertex.put(frontIds.get(i).getAsInt(),front.get(i).getAsJsonArray());
    for(int i=1;i<faces.size();i++){
      var face=faces.get(i).getAsJsonObject();var values=face.getAsJsonArray("uv");var ids=face.getAsJsonArray("v");
      check(values.size()==ids.size(),"Wing edge has no per-corner UV correspondence");
      for(int j=0;j<values.size();j++){
        var uv=values.get(j).getAsJsonArray();float u=uv.get(0).getAsFloat(),v=uv.get(1).getAsFloat();
        check(u>=minU&&u<=maxU&&v>=minV&&v<=maxV,"Wing reverse/edge UV escaped the original visible island");
        var adjacent=frontByVertex.get(ids.get(j).getAsInt()%4);check(adjacent!=null,"Wing side no longer corresponds to its adjacent front corner");
        // A small inside inset is allowed at a nearest-filtered texel edge,
        // but a constant opaque side sample would frame the front cutout.
        check(Math.abs(u-adjacent.get(0).getAsFloat())<=.126f&&Math.abs(v-adjacent.get(1).getAsFloat())<=.126f,"Wing reverse/edge UV no longer follows the adjacent front alpha cutout");
      }
    }
  }
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
    var d=data(MODEL);var original=data("gameplay/chao_baby");check(d.has("hero_normal_path_057")&&!d.has("blocky_run_path_055"),"Hero/Normal path reused Hero selection");
    Map<String,JsonObject> old=new HashMap<>();for(var b:original.getAsJsonArray("bones"))old.put(b.getAsJsonObject().get("name").getAsString(),b.getAsJsonObject());
    for(var be:d.getAsJsonArray("bones")){
      var b=be.getAsJsonObject();String name=b.get("name").getAsString();
      if(old.containsKey(name))check(b.get("pivot").equals(old.get(name).get("pivot"))&&b.get("rotation").equals(old.get(name).get("rotation")),"Shared rig/attachment pivot changed");
      else check(name.startsWith("hero_normal_halo_057_")&&b.get("parent").getAsString().equals("emotion_dot"),"Unexpected new joint/attachment family");
      if(name.startsWith("skull")||name.startsWith("expression_")||name.startsWith("emotion_")&&!name.equals("emotion_dot__cube_0"))check(b.equals(old.get(name)),"Original head/face/reaction record changed "+name);
      if(name.equals("wing_left__cube_0")||name.equals("wing_right__cube_0"))wingUv(b,old.get(name));
    }
    check(d.get("head_decorations").equals(original.get("head_decorations")),"Hat hiding family changed");
    var profile=d.getAsJsonObject("hero_normal_path_057");
    check(profile.getAsJsonArray("targets").size()==10,"Wrong Hero/Normal corner count");
    var crown=d.getAsJsonArray("bones").asList().stream().map(v->v.getAsJsonObject()).filter(v->v.get("name").getAsString().equals("gradual_head_021")).findFirst().orElseThrow().getAsJsonArray("meshes");
    check(crown.size()==4,"Hero tips must retain one lower root and one upper cap per side");
    String[] names={"hero_normal_root_left","hero_normal_cap_left","hero_normal_root_right","hero_normal_cap_right"};
    for(int i=0;i<4;i++){
      var part=crown.get(i).getAsJsonObject();boolean cap=i%2==1;
      check(part.get("name").getAsString().equals(names[i]),"Paired lower/upper tip ownership changed");
      check(part.get("hero_normal_region_057").getAsString().equals(cap?"crown_tip":"crown_root"),"Lower root and upper cap use the same pigment region");
      var expected=profile.get(cap?"feature_tip_uv":"feature_body_uv");
      for(var face:part.getAsJsonArray("faces"))for(var uv:face.getAsJsonObject().getAsJsonArray("uv"))check(uv.equals(expected),"Tip section no longer uses its own canonical color swatch");
    }
  }
  private static void development(){
    var m=asset(MODEL);var original=asset("gameplay/chao_baby");var p=poses(m);var head=solid(original,"skull__cube_0");var eyesLeft=solid(original,"skull__cube_2");var eyesRight=solid(original,"skull__cube_3");
    var endpointShapes=new HashSet<Long>();
    for(float age:new float[]{0,.25f,.5f,.75f,1})for(int yi=0;yi<=10;yi++)for(int xi=0;xi<=10;xi++){
      var s=state(xi/5f-1,yi/5f-1,age);String saved=s.encode();sample(m,s);frames++;
      equal(head,solid(m,"skull__cube_0"),.00004f,"Original head resized");equal(eyesLeft,solid(m,"skull__cube_2"),.00004f,"Left eye moved/distorted");equal(eyesRight,solid(m,"skull__cube_3"),.00004f,"Right eye moved/distorted");
      cuboids(solid(m,"gradual_head_021"));compact(m);haloClearance(m,p);wings(m,p,true);check(saved.equals(s.encode()),"Renderer changed saved data");
      if(age==1&&xi%5==0&&yi%5==0)endpointShapes.add(m.shapeSignature());
    }
    check(endpointShapes.size()==9,"Requested nine mature combinations collapsed into duplicate forms");
    for(float age:new float[]{0,.5f,1})for(float sf:new float[]{-1,0,1}){
      Capture previous=null;
      for(int i=0;i<=100;i++){sample(m,state(i/50f-1,sf,age));var now=capture(m,p);tipFaceSeparation(m,p);if(previous!=null){check(previous.n==now.n,"Axis sweep swapped topology");for(int n=0;n<now.n;n+=3){float dx=now.p[n]-previous.p[n],dy=now.p[n+1]-previous.p[n+1],dz=now.p[n+2]-previous.p[n+2];float step=(float)Math.sqrt(dx*dx+dy*dy+dz*dz);maxStep=Math.max(maxStep,step);check(step<.3f,"Small axis change popped geometry");}}previous=now;frames++;}
    }
    sample(m,state(-1,-1,0));long fresh=m.shapeSignature();sample(m,state(1,1,0));check(fresh==m.shapeSignature(),"Training overrode independent fresh-adult age");
    var s=state(-1,-1,1);sample(m,s);long before=m.shapeSignature();float maturity=s.evolutionStrength();DriveRules.setTotal(s,ChaoState.RUN,0,new Random(57));sample(m,s);check(before!=m.shapeSignature()&&maturity==s.evolutionStrength(),"Run loss did not reverse independently of maturity");
    var loaded=ChaoState.decode(s.encode());sample(m,loaded);long restored=m.shapeSignature();sample(m,s);check(restored==m.shapeSignature(),"Save/reload changed sampled geometry");
    m.hideHeadDecorations(p);check(!p.get("gradual_head_021").visible,"Hat did not hide crown additions");
  }
  private static float height(Capture c,int start){return c.point(start).distance(c.point(start+1));}
  private static float width(Capture c,int start){return c.point(start+1).distance(c.point(start+2));}
  /** Distinguish geometry before colors: long narrow Run caps, broad Power
   * caps, thin-stemmed Fly bulbs and broad Swim roots; actual source-box edges
   * are measured after the renderer's rotations, not read from target JSON. */
  private static void refinedSilhouettes(){
    var m=asset(MODEL);
    sample(m,state(0,0,1));var normal=solid(m,"gradual_head_021");float normalCap=width(normal,24),normalRoot=width(normal,0),normalHeight=height(normal,24);
    sample(m,state(-1,0,1));var run=solid(m,"gradual_head_021");float runWidth=width(run,24),runHeight=height(run,24),runRoot=width(run,0);
    check(runHeight>normalHeight*1.5f&&runHeight>runWidth*2.5f,"Hero Run upper section lost its long narrow silhouette");
    sample(m,state(1,0,1));var power=solid(m,"gradual_head_021");
    check(width(power,24)>runWidth*2&&width(power,24)>height(power,24)*1.5f,"Hero Power upper section is no broader/blunter than Run");
    sample(m,state(0,1,1));var fly=solid(m,"gradual_head_021");
    check(width(fly,24)>width(fly,0)*3&&width(fly,0)<normalRoot*.4f,"Hero Fly lost its thin lower stalk and separate broad bulb");
    check(width(fly,24)>normalCap*.85f&&width(fly,24)<normalCap*1.25f,"Hero Fly bulb is no longer comparable to the balanced bulb width");
    var fixedHead=bounds(solid(m,"skull__cube_0"),0,24);var flyCap=bounds(fly,24,24);var otherFlyCap=bounds(fly,72,24);
    check(fixedHead[1]-flyCap[4]>.5f&&fixedHead[1]-otherFlyCap[4]>.5f,"Hero Fly caps hide their thin white lower sections against the unchanged head");
    sample(m,state(0,-1,1));var swim=solid(m,"gradual_head_021");
    check(width(swim,24)>runWidth*1.8f&&width(swim,0)>runRoot*1.4f,"Hero Swim upper/lower sections lost their broader horn silhouette");
    float runSpan=0,runWingHeight=0,powerSpan=0,powerWingHeight=0,flyWingHeight=0,swimSpan=0,normalSpan=0;
    var wingShapes=new HashSet<String>();
    for(int sf=-1;sf<=1;sf++)for(int rp=-1;rp<=1;rp++){
      sample(m,state(rp,sf,1));var wing=solid(m,"wing_right__cube_0");float span=width(wing,0),high=height(wing,0);
      wingShapes.add(Math.round(span*1000)+":"+Math.round(high*1000));
      if(sf==0&&rp==-1){runSpan=span;runWingHeight=high;}if(sf==0&&rp==1){powerSpan=span;powerWingHeight=high;}
      if(sf==1&&rp==0)flyWingHeight=high;if(sf==-1&&rp==0)swimSpan=span;if(sf==0&&rp==0)normalSpan=span;
    }
    check(wingShapes.size()==9,"Hero specializations recolor identical wing geometry");
    check(runSpan>powerSpan*1.4f&&runWingHeight<powerWingHeight*.75f,"Hero Run/Power wings lost their long-slender versus compact-broad distinction");
    check(flyWingHeight>runWingHeight*1.5f&&swimSpan>normalSpan*1.1f,"Hero Fly/Swim wings lost their broader reference silhouettes");
  }
  private static void temporal(){
    var m=asset(MODEL);var p=poses(m);var s=state(0,0,1);UUID id=new UUID(57,570057);m.heroNormalDevelopment(HeroNormalAppearance057.prepare(id,s,0));var original=capture(m,p);tipFaceSeparation(m,p);DriveRules.setTotal(s,ChaoState.RUN,960,new Random(57));DriveRules.setTotal(s,ChaoState.SWIM,960,new Random(57));
    m.heroNormalDevelopment(HeroNormalAppearance057.prepare(id,s,1));equal(original,capture(m,p),.00004f,"Large packet snapped the first frame");tipFaceSeparation(m,p);Capture before=original;
    for(int t=2;t<=21;t++){m.heroNormalDevelopment(HeroNormalAppearance057.prepare(id,s,t));var now=capture(m,p);tipFaceSeparation(m,p);check(now.n==before.n,"Edit swapped visible topology");for(int n=0;n<now.n;n+=3){float dx=now.p[n]-before.p[n],dy=now.p[n+1]-before.p[n+1],dz=now.p[n+2]-before.p[n+2];float step=(float)Math.sqrt(dx*dx+dy*dy+dz*dz);maxEdit=Math.max(maxEdit,step);check(step<1,"Edit interpolation jumped a model pixel");}before=now;frames++;}
    sample(m,s);equal(before,capture(m,p),.00004f,"Client edit failed to reach authoritative geometry");
    DriveRules.setTotal(s,ChaoState.RUN,0,new Random(57));DriveRules.setTotal(s,ChaoState.SWIM,0,new Random(57));m.heroNormalDevelopment(HeroNormalAppearance057.prepare(id,s,22));equal(before,capture(m,p),.00004f,"Reversal used a stale display frame");tipFaceSeparation(m,p);
    for(int t=23;t<=42;t++){m.heroNormalDevelopment(HeroNormalAppearance057.prepare(id,s,t));tipFaceSeparation(m,p);}equal(original,capture(m,p),.00004f,"Reversal failed to recover centered form");
  }
  private static void motion()throws Exception{
    var clips=JsonParser.parseString(Files.readString(ROOT.resolve("animations/official/chao.animation.json"))).getAsJsonObject().getAsJsonObject("animations");var rig=new OfficialChaoRig(clips,HeroNormalMorphRigTest057::asset);var expected=asset(MODEL);int serial=0;
    for(float age:new float[]{0,.5f,1})for(float sf:new float[]{-1,0,1})for(float rp:new float[]{-1,0,1})for(var mode:List.of(OfficialChaoRig.Motion.CRAWL,OfficialChaoRig.Motion.WALK,OfficialChaoRig.Motion.RUN,OfficialChaoRig.Motion.SIT,OfficialChaoRig.Motion.SWIM,OfficialChaoRig.Motion.GLIDE,OfficialChaoRig.Motion.HELD))for(int tick:new int[]{0,10,20}){
      var s=state(rp,sf,age);String saved=s.encode();String key=OfficialChaoRig.geometryKey(s);rig.prepareDevelopment(key,s);rig.animateGameplay(key,new UUID(57,++serial),mode,tick,1,.05f,0,0,0,0,false);rig.development(s);sample(expected,s);check(rig.finite(),"Lionfish pose nonfinite");check(rig.sampledMesh().shapeSignature()==expected.shapeSignature(),"Production selection skipped HeroNormal057");check(rig.sourceGeometryKey().equals("baby"),"UV/attachment family changed");check(saved.equals(s.encode()),"Motion changed saved state");haloClearance(rig.sampledMesh(),rig.poses());
      try{wings(rig.sampledMesh(),rig.poses(),false);}catch(AssertionError failure){var wingMatrix=new PoseStack();rig.sampledMesh().transformBone("wing_left",wingMatrix,rig.poses());var body=rig.poses().get("body");throw new AssertionError("Wing frame age="+age+" rp="+rp+" sf="+sf+" mode="+mode+" tick="+tick+" bodyScale="+body.sx+","+body.sy+","+body.sz+" leftWingMatrix="+Arrays.toString(wingMatrix.last().pose().get(new float[16]))+"; "+failure.getMessage(),failure);}
      motionFrames++;
      for(boolean left:new boolean[]{false,true}){float[] palm=rig.toyPalm(left);check(palm!=null&&palm.length==3,"Missing toy palm");for(float v:palm)check(Float.isFinite(v),"Toy palm nonfinite");}
      for(String anchor:List.of("body","skull","arm_left","arm_right","foot_left","foot_right","tail","wing_left","wing_right")){var matrix=new PoseStack();rig.transformBone(anchor,matrix);check(matrix.last().pose().isFinite(),"Attachment matrix nonfinite");check(rig.attachmentBounds(anchor)!=null,"Missing attachment envelope");}
      if(tick==0)for(String family:List.of("overlays/chao_baby","cwe_parts/chao_baby","hats/chao_baby","market_accessories")){var donor=asset(family);var attached=donor.createPoses();for(var entry:rig.poses().entrySet())if(attached.containsKey(entry.getKey()))rig.sampledMesh().copyJointPoseTo(entry.getKey(),donor,entry.getValue(),attached.get(entry.getKey()));for(String anchor:List.of("body","skull","arm_left","arm_right","foot_left","foot_right","tail","wing_left","wing_right"))if(attached.containsKey(anchor)){var a=new PoseStack();var b=new PoseStack();rig.transformBone(anchor,a);donor.transformBone(anchor,b,attached);float[] av=a.last().pose().get(new float[16]),bv=b.last().pose().get(new float[16]);for(int i=0;i<16;i++)near(av[i],bv[i],.000008f,"Separate accessory/animal anchor detached");attachments++;}}
    }
  }
  /** Exercise the actual quiet-book pose functions at the observed phase36
   * and representative entry/exit phases. No alternate demonstration pose is
   * authored here; this is the same production PLAY rig and quiet-toy solver. */
  private static void bookTipSurfaces()throws Exception{
    var clips=JsonParser.parseString(Files.readString(ROOT.resolve("animations/official/chao.animation.json"))).getAsJsonObject().getAsJsonObject("animations");
    var quiet=new LionfishQuietToyClip049();
    for(int tick:new int[]{12,36,72,132,156}){
      var s=state(1,-1,1);var rig=new OfficialChaoRig(clips,HeroNormalMorphRigTest057::asset);String key=OfficialChaoRig.geometryKey(s);
      rig.prepareDevelopment(key,s);rig.animateGameplay(key,new UUID(58,580053+tick),OfficialChaoRig.Motion.PLAY,tick,0,0,0,0,0,0,false);rig.development(s);
      float weight=com.chaogarden.toys.QuietToyPlayRules049.presence(tick);
      ChaoQuietToyAnimation049.pose(rig.poses(),rig::transformBone,rig.toyPalm(false),rig.toyPalm(true),
          com.chaogarden.toys.QuietToyPlayRules049.BOOK,weight,quiet.sweep(tick),quiet.page(tick),quiet.stroke(tick),quiet.pen(tick),tick,.8f,rig::attachmentBounds);
      check(rig.finite(),"Production quiet-book pose is not finite");tipFaceSeparation(rig.sampledMesh(),rig.poses());bookSurfaceFrames++;
    }
  }
  private static void stableCache()throws Exception {
    var clips=JsonParser.parseString(Files.readString(ROOT.resolve("animations/official/chao.animation.json"))).getAsJsonObject().getAsJsonObject("animations");
    var revision=ChaoMeshAsset.class.getDeclaredField("pathRevision056");revision.setAccessible(true);
    for(float alignment:new float[]{0,1}){
      var s=state(-1,1,.5f);s.align=alignment;
      var rig=new OfficialChaoRig(clips,HeroNormalMorphRigTest057::asset);rig.prepareDevelopment(OfficialChaoRig.geometryKey(s),s);
      var mesh=rig.sampledMesh();check(alignment==0?mesh.neutralPrototype056():mesh.heroNormalPrototype057(),"Cache fixture selected wrong Normal profile");
      int before=revision.getInt(mesh);long signature=mesh.shapeSignature();String encoded=s.encode();
      for(int frame=0;frame<100;frame++){
        rig.development(s);
        check(revision.getInt(mesh)==before,"Unchanged state reset the isolated ten-corner cache each frame");
        check(mesh.shapeSignature()==signature,"Unchanged state changed its emitted geometry");
      }
      check(s.encode().equals(encoded),"Repeated rendering changed saved state");
      DriveRules.setTotal(s,ChaoState.POWER,480,new Random(57));rig.development(s);
      check(revision.getInt(mesh)==before+1,"A genuine training change must invalidate this geometry cache exactly once");
      check(mesh.shapeSignature()!=signature,"A genuine training change left a stale cached shape");
      int changed=revision.getInt(mesh);rig.development(s);check(revision.getInt(mesh)==changed,"New stable training state kept invalidating the cache");
    }
  }
  private static void coats(){
    var m=asset(MODEL);var p=poses(m);
    for(float rp:new float[]{-1,0,1})for(float sf:new float[]{-1,0,1}){var s=state(rp,sf,1);sample(m,s);var reference=capture(m,p);for(int color=0;color<ChaoState.COLORS.length;color++)for(int jewel=0;jewel<ChaoState.JEWELS.length;jewel++)if(color==0||jewel==0)for(boolean shiny:new boolean[]{false,true})for(boolean twoTone:new boolean[]{false,true}){s.expressedColor=color;s.expressedJewel=jewel;s.shiny=shiny;s.twoTone=twoTone;sample(m,s);equal(reference,capture(m,p),0,"Color/shine changed form/UV");coatCases++;}}
  }
  public static void main(String[] args)throws Exception{
    long start=System.nanoTime();topology();development();refinedSilhouettes();temporal();motion();bookTipSurfaces();stableCache();coats();String report="HeroNormal057: "+checks+" checks; "+frames+" emitted age/axis/edit frames; "+solids+" closed rectangular-box samples; "+motionFrames+" production Lionfish motion frames; "+attachments+" separate attachment matrices; "+coatCases+" coat/shape cases; "+haloPairs+" actual tilted-halo/head-feature separation checks; "+wingSamples+" emitted closed-wing samples; "+hingeSamples+" original-edge hinge samples; "+tipPlanePairs+" emitted root/cap depth-plane pairs; "+bookSurfaceFrames+" production quiet-book surface frames; minimum tip face gap="+minTipFaceGap+" model pixels; maximum hinge drift="+maxHinge+" model pixels; maximum axis step="+maxStep+" model pixels; maximum20tick edit step="+maxEdit+"; seconds="+(System.nanoTime()-start)/1e9+". Fixed original head/face/rig; two-section paired features, differentiated closed wings and four-bar halo; nine distinct mature corners; reversible independent age/axes; current production selection. Geometry validation, not visual/multiplayer verification.\n";Files.createDirectories(Path.of("build/reports/lionfish"));Files.writeString(Path.of("build/reports/lionfish/hero-normal057-results.txt"),report);System.out.print(report);
  }
}
