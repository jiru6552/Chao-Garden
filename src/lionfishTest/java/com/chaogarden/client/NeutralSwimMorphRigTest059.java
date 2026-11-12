package com.chaogarden.client;

import com.chaogarden.core.ChaoState;
import com.chaogarden.core.DriveRules;
import com.chaogarden.core.NeutralSwimMorphology059;
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
import org.joml.Vector2f;

/** Emitted production geometry/animation checks, separate from visual inspection. */
public final class NeutralSwimMorphRigTest059 {
  private static final Path ROOT=Path.of("src/main/resources/assets/chao_garden");
  private static final String MODEL="gameplay/chao_neutral_swim_proto059";
  private static int checks,frames,solids,motionFrames,attachments,coatCases,orbPairs,wingSamples,hingeSamples,connectedFeatures,bookSurfaceFrames,baselineGuardFrames,surfacePairs,headTorsoFrames;
  private static float maxStep,maxEdit,maxHinge,maxMaturity;
  private static int featureBoxes;
  private static final Map<String,Vector3f> ORIGINAL_WING_HINGES=new HashMap<>();
  private static void check(boolean ok,String why){checks++;if(!ok)throw new AssertionError(why);}
  private static void near(float a,float b,float e,String why){check(Float.isFinite(a)&&Float.isFinite(b)&&Math.abs(a-b)<=e,why+": "+a+" / "+b);}
  private static JsonObject data(String key)throws Exception{return JsonParser.parseString(Files.readString(ROOT.resolve("models/entity/"+key+".json"))).getAsJsonObject();}
  private static ChaoMeshAsset asset(String key){try{var m=ChaoMeshAsset.parse(data(key));m.lens(0xff101820);m.expression(1);return m;}catch(Exception e){throw new IllegalStateException(key,e);}}
  private static ChaoState state(float rp,float sf,float age){
    var s=new ChaoState();s.adult=true;s.primaryType=ChaoState.SWIM_TYPE;s.align=0;
    int r=Math.round(Math.abs(rp)*960),f=Math.round(Math.abs(sf)*960);
    int rs=rp<0?ChaoState.RUN:ChaoState.POWER,fs=sf<0?ChaoState.SWIM:ChaoState.FLY;
    s.levels[rs]=r/100;s.progress[rs]=r%100;s.points[rs]=r;
    s.levels[fs]=f/100;s.progress[fs]=f%100;s.points[fs]=f;
    s.setEvolutionStrength(age*1.2f);return s;
  }
  private static void sample(ChaoMeshAsset m,ChaoState s){m.development(s.swimFly,s.runPower,s.gradualVisualStrength());m.neutralSwimDevelopment(NeutralSwimMorphology059.sample(s));}
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
  private static void featureContact(ChaoMeshAsset m,Map<String,ChaoMeshAsset.Pose> p){
    var skull=select(m,p,"skull__cube_0");var features=select(m,p,"gradual_head_021");var orb=select(m,p,"emotion_dot__cube_0");
    check(skull.count()==24&&features.count()==featureBoxes*24,"Fixed skull or authored feature topology changed");
    surfacePlanes(inJointFrame(m,p,"skull",skull),inJointFrame(m,p,"skull",features));
    cuboids(inJointFrame(m,p,"gradual_head_021",features));
    var left=select(m,p,"skull__cube_2");var right=select(m,p,"skull__cube_3");
    var attached=new boolean[featureBoxes];int reached=0;
    for(int i=0;i<featureBoxes;i++){
      check(!intersects(features,i*24,left,0)&&!intersects(features,i*24,right,0),"Added head feature intersects an unchanged eye");
      check(!intersects(features,i*24,orb,0),"Actual orb intersects growing head features");orbPairs++;
      if(intersects(features,i*24,skull,0)){attached[i]=true;reached++;}
    }
    boolean changed=true;while(changed){changed=false;for(int i=0;i<featureBoxes;i++)if(!attached[i])for(int j=0;j<featureBoxes;j++)if(attached[j]&&intersects(features,i*24,features,j*24)){attached[i]=true;reached++;changed=true;break;}}
    check(reached==featureBoxes,"Growing feature disconnected from original head or adjoining authored feature");connectedFeatures+=reached;
  }
  /** Independent emitted-face coplanarity check. Box closure/volume alone
   * misses two differently painted, same-facing surfaces occupying one plane.
   * Work in the shared skull frame, then clip convex projected face polygons. */
  private static void surfacePlanes(ChaoMeshAsset mesh,Map<String,ChaoMeshAsset.Pose> poses){
    surfacePlanes(inJointFrame(mesh,poses,"skull",select(mesh,poses,"skull__cube_0")),inJointFrame(mesh,poses,"skull",select(mesh,poses,"gradual_head_021")));
  }
  private static void surfacePlanes(Capture head,Capture features){
    for(int a=-1;a<featureBoxes;a++)for(int b=a+1;b<featureBoxes;b++){
      var first=a<0?head:features;int offset=a<0?0:a*24;
      for(int fa=0;fa<6;fa++)for(int fb=0;fb<6;fb++){
        int ai=offset+fa*4,bi=b*24+fb*4;surfacePairs++;
        var normal=new Vector3f(first.normal[ai*3],first.normal[ai*3+1],first.normal[ai*3+2]);var other=new Vector3f(features.normal[bi*3],features.normal[bi*3+1],features.normal[bi*3+2]);
        if(normal.dot(other)<.999999f)continue;
        var anchor=first.point(ai);boolean coplanar=true;for(int i=0;i<4;i++)if(Math.abs(features.point(bi+i).sub(anchor).dot(normal))>.00005f){coplanar=false;break;}if(!coplanar)continue;
        int drop=Math.abs(normal.x)>Math.abs(normal.y)?0:1;if(Math.abs(normal.z)>Math.abs(drop==0?normal.x:normal.y))drop=2;
        var polygon=new java.util.ArrayList<Vector2f>();var boundary=new java.util.ArrayList<Vector2f>();for(int i=0;i<4;i++){polygon.add(project(first.point(ai+i),drop));boundary.add(project(features.point(bi+i),drop));}
        float winding=polygonAreaSigned(boundary)>=0?1:-1;
        for(int edge=0;edge<4&&!polygon.isEmpty();edge++){
          var start=boundary.get(edge);var end=boundary.get((edge+1)%4);var input=polygon;polygon=new java.util.ArrayList<>();
          for(int i=0;i<input.size();i++){
            var from=input.get((i+input.size()-1)%input.size());var to=input.get(i);float ds=winding*cross2(new Vector2f(end).sub(start),new Vector2f(from).sub(start)),de=winding*cross2(new Vector2f(end).sub(start),new Vector2f(to).sub(start));boolean insideFrom=ds>=-1e-6f,insideTo=de>=-1e-6f;
            if(insideFrom!=insideTo)polygon.add(new Vector2f(from).lerp(to,ds/(ds-de)));if(insideTo)polygon.add(new Vector2f(to));
          }
        }
        float overlap=Math.abs(polygonAreaSigned(polygon));check(overlap<.00001f,"Same-facing emitted head/feature surfaces can z-fight: box"+a+" face"+fa+" / box"+b+" face"+fb+" overlap="+overlap+"px2 plane="+anchor.dot(normal));
      }
    }
  }
  private static Vector2f project(Vector3f point,int drop){return drop==0?new Vector2f(point.y,point.z):drop==1?new Vector2f(point.x,point.z):new Vector2f(point.x,point.y);}
  private static float cross2(Vector2f a,Vector2f b){return a.x*b.y-a.y*b.x;}
  private static float polygonAreaSigned(List<Vector2f> points){float area=0;for(int i=0;i<points.size();i++)area+=cross2(points.get(i),points.get((i+1)%points.size()));return area*.5f;}
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
    check(left.count()==24&&right.count()==24,"Neutral Swim wings must be two positive six-face boxes");
    // Inherited breathing squashes the BODY nonuniformly. Combined with a
    // wing flap that valid parent affine transform can shear a world box.
    // Test intrinsic box rigidity in the actual wing-joint frame, while
    // keeping world-space volume, planarity, winding and attachment checks.
    if(symmetric){cuboids(left);cuboids(right);}
    else{affineSurfaces(left);affineSurfaces(right);cuboids(inJointFrame(m,p,"wing_left",left));cuboids(inJointFrame(m,p,"wing_right",right));}
    wingSamples+=2;
    if(symmetric)mirrored(left,right,"Neutral Swim wing pair");
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
    check(mesh.has("neutral_swim_box_059")&&mesh.getAsJsonArray("vertices").size()==8,"Wing does not share the ten-target rigid-box evaluator");
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
    var d=data(MODEL);var original=data("gameplay/chao_baby");check(d.has("neutral_swim_path_059")&&!d.has("blocky_run_path_055")&&!d.has("hero_normal_path_057"),"Swim-first asset reused another morphology admission flag");
    Map<String,JsonObject> old=new HashMap<>();for(var b:original.getAsJsonArray("bones"))old.put(b.getAsJsonObject().get("name").getAsString(),b.getAsJsonObject());
    check(d.getAsJsonArray("bones").size()==old.size()&&old.size()==59,"Shared rig must retain exactly its original59 joints");
    for(var be:d.getAsJsonArray("bones")){
      var b=be.getAsJsonObject();String name=b.get("name").getAsString();check(old.containsKey(name),"Unexpected new joint family");
      check(b.get("pivot").equals(old.get(name).get("pivot"))&&b.get("rotation").equals(old.get(name).get("rotation"))&&java.util.Objects.equals(b.get("parent"),old.get(name).get("parent")),"Original rig/attachment pivot changed "+name);
      if(name.startsWith("skull")||name.startsWith("expression_")||name.startsWith("emotion_"))check(b.equals(old.get(name)),"Original head/face/reaction/orb record changed "+name);
      if(name.equals("wing_left__cube_0")||name.equals("wing_right__cube_0"))wingUv(b,old.get(name));
    }
    check(d.get("head_decorations").equals(original.get("head_decorations")),"Hat hiding family changed");
    var profile=d.getAsJsonObject("neutral_swim_path_059");check(profile.getAsJsonArray("targets").size()==10,"Wrong fresh-plus-grid target count");
    var crown=d.getAsJsonArray("bones").asList().stream().map(v->v.getAsJsonObject()).filter(v->v.get("name").getAsString().equals("gradual_head_021")).findFirst().orElseThrow().getAsJsonArray("meshes");
    featureBoxes=crown.size();check(featureBoxes==7,"Swim-first head must retain three cap pieces and paired two-piece side fins");
    String[] names={"swim_cap_roof","swim_cap_upper","swim_cap_rear","swim_side_fin_left_root","swim_side_fin_left_tip","swim_side_fin_right_root","swim_side_fin_right_tip"};for(int i=0;i<names.length;i++)check(crown.get(i).getAsJsonObject().get("name").getAsString().equals(names[i]),"Head construction ownership changed");
    int boxes=0;
    for(var be:d.getAsJsonArray("bones"))for(var me:be.getAsJsonObject().getAsJsonArray("meshes")){
      var part=me.getAsJsonObject();if(!part.has("neutral_swim_box_059"))continue;
      check(part.getAsJsonArray("vertices").size()==8&&part.getAsJsonArray("faces").size()==6,"Authored morph part is not one closed cuboid");
      boxes++;var target=part.getAsJsonObject("neutral_swim_box_059");
      for(var field:target.entrySet()){var values=field.getValue().getAsJsonArray();check(values.size()==10,"Part lost ten-target correspondence");for(int x=0;x<3;x++)check(values.get(7+x).equals(values.get(4+x)),"Unused positive Fly target differs from balanced guard row");}
    }
    check(boxes==14,"Swim-first box budget/topology changed");
  }
  private static void development(){
    var m=asset(MODEL);var original=asset("gameplay/chao_baby");var p=poses(m);var head=solid(original,"skull__cube_0");var eyeL=solid(original,"skull__cube_2");var eyeR=solid(original,"skull__cube_3");var orb=solid(original,"emotion_dot__cube_0");
    var endpoints=new HashSet<Long>();
    for(float age:new float[]{0,.25f,.5f,.75f,1})for(int yi=0;yi<=5;yi++)for(int xi=0;xi<=10;xi++){
      var state=state(xi/5f-1,yi/5f-1,age);String saved=state.encode();sample(m,state);frames++;
      equal(head,solid(m,"skull__cube_0"),.00004f,"Original 7x6x6 head resized");equal(eyeL,solid(m,"skull__cube_2"),.00004f,"Left eye moved/distorted");equal(eyeR,solid(m,"skull__cube_3"),.00004f,"Right eye moved/distorted");equal(orb,solid(m,"emotion_dot__cube_0"),.00004f,"Original Neutral orb changed");
      cuboids(solid(m,"gradual_head_021"));mirrored(solid(m,"gradual_head_021"),solid(m,"gradual_head_021"),"Paired Swim fins/cap");compact(m);featureContact(m,p);wings(m,p,true);check(saved.equals(state.encode()),"Rendering changed saved data");
      if(age==1&&xi%5==0&&yi%5==0)endpoints.add(m.shapeSignature());
    }
    check(endpoints.size()==6,"Requested six mature combinations collapsed into duplicate forms");
    for(float age:new float[]{0,.5f,1}){
      for(float sf:new float[]{-1,0}){Capture before=null;for(int i=0;i<=100;i++){sample(m,state(i/50f-1,sf,age));var now=capture(m,p);surfacePlanes(m,p);maxStep=Math.max(maxStep,step(before,now,.3f,"Small Run/Power change popped geometry"));before=now;frames++;}}
      for(float rp:new float[]{-1,0,1}){Capture before=null;for(int i=0;i<=100;i++){sample(m,state(rp,i/100f-1,age));var now=capture(m,p);surfacePlanes(m,p);maxStep=Math.max(maxStep,step(before,now,.3f,"Small Swim/Fly change popped geometry"));before=now;frames++;}}
    }
    for(float rp:new float[]{-1,0,1})for(float sf:new float[]{-1,0}){Capture before=null;for(int i=0;i<=100;i++){sample(m,state(rp,sf,i/100f));var now=capture(m,p);surfacePlanes(m,p);maxMaturity=Math.max(maxMaturity,step(before,now,.15f,"One-percent maturity change popped geometry"));before=now;frames++;}}
    sample(m,state(-1,-1,0));long fresh=m.shapeSignature();sample(m,state(1,0,0));check(fresh==m.shapeSignature(),"Training overrides independent fresh-adult age");
    for(float rp:new float[]{-1,0,1}){sample(m,state(rp,0,1));long balanced=m.shapeSignature();sample(m,state(rp,1,1));check(balanced==m.shapeSignature(),"Positive Fly silently creates an unsupported secondary shape");}
    var state=state(-1,-1,1);sample(m,state);long before=m.shapeSignature();float maturity=state.evolutionStrength();DriveRules.setTotal(state,ChaoState.RUN,0,new Random(59));sample(m,state);check(before!=m.shapeSignature()&&maturity==state.evolutionStrength(),"Run loss does not reverse independently from age");
    var copy=ChaoState.decode(state.encode());sample(m,copy);long loaded=m.shapeSignature();sample(m,state);check(loaded==m.shapeSignature(),"Save/reload changed sampled geometry");m.hideHeadDecorations(p);check(!p.get("gradual_head_021").visible,"Hat does not hide new head features");
  }
  private static float step(Capture before,Capture now,float limit,String why){
    if(before==null)return 0;check(before.n==now.n&&before.u==now.u,"Transition swaps topology");float largest=0;
    for(int i=0;i<now.n;i+=3){float dx=now.p[i]-before.p[i],dy=now.p[i+1]-before.p[i+1],dz=now.p[i+2]-before.p[i+2];float amount=(float)Math.sqrt(dx*dx+dy*dy+dz*dz);largest=Math.max(largest,amount);check(amount<limit,why+": "+amount);}
    for(int i=0;i<now.u;i++)near(before.uv[i],now.uv[i],0,"Transition distorts canonical UVs");return largest;
  }
  /** Check recognizable directional differences on emitted, transformed
   * geometry. These are the approved blocky interpretation, not source-mesh
   * equality claims. In the unchanged head joint, +Z is rear and +Y is down. */
  private static void silhouettes(){
    var mesh=asset(MODEL);var poses=poses(mesh);
    sample(mesh,state(0,0,0));var fresh=inJointFrame(mesh,poses,"gradual_head_021",solid(mesh,"gradual_head_021"));var freshRear=boxEdges(fresh,48)[2];
    check(freshRear.y>.5f&&freshRear.z>1,"Newly evolved Swim already has a visible rear/down cap");
    sample(mesh,state(-1,0,1));var run=inJointFrame(mesh,poses,"gradual_head_021",solid(mesh,"gradual_head_021"));var runRear=boxEdges(run,48)[2];
    check(runRear.y<-.5f&&runRear.z>4,"Run must raise and lengthen the rear cap instead of retaining a dropped Swim hem");
    sample(mesh,state(1,0,1));var power=inJointFrame(mesh,poses,"gradual_head_021",solid(mesh,"gradual_head_021"));var powerRear=boxEdges(power,48)[2];
    check(powerRear.length()<runRear.length()*.75f&&powerRear.y>0,"Power hood must remain short, blunt and down/back");
    sample(mesh,state(0,-1,1));var swim=inJointFrame(mesh,poses,"gradual_head_021",solid(mesh,"gradual_head_021"));var swimRear=boxEdges(swim,48)[2];
    check(swimRear.y>2.5f&&swimRear.z>1.5f&&swimRear.length()>freshRear.length()*1.5f,"Mature Swim loses the long dropped rear hood");
    var freshFin=bounds(fresh,96,24);var runFin=bounds(run,96,24);var swimFin=bounds(swim,96,24);var powerFin=bounds(power,96,24);
    check((runFin[1]+runFin[4])*.5f<(freshFin[1]+freshFin[4])*.5f-.4f,"Run side fins do not rise above the fresh temple fins");
    check((swimFin[1]+swimFin[4])*.5f>(freshFin[1]+freshFin[4])*.5f+1.5f,"Swim side fins do not develop into hanging flippers");
    check(boxEdges(power,96)[0].length()>boxEdges(run,96)[0].length()*1.4f,"Power side fins are not intrinsically thicker and blunter than Run");
  }
  private static void temporal(){
    var m=asset(MODEL);var p=poses(m);var s=state(0,0,1);UUID id=new UUID(59,590059);m.neutralSwimDevelopment(NeutralSwimAppearance059.prepare(id,s,0));var original=capture(m,p);featureContact(m,p);DriveRules.setTotal(s,ChaoState.RUN,960,new Random(59));DriveRules.setTotal(s,ChaoState.SWIM,960,new Random(59));
    m.neutralSwimDevelopment(NeutralSwimAppearance059.prepare(id,s,1));equal(original,capture(m,p),.00004f,"Large packet snapped the first frame");featureContact(m,p);Capture before=original;
    for(int t=2;t<=21;t++){m.neutralSwimDevelopment(NeutralSwimAppearance059.prepare(id,s,t));var now=capture(m,p);featureContact(m,p);check(now.n==before.n,"Edit swapped visible topology");for(int n=0;n<now.n;n+=3){float dx=now.p[n]-before.p[n],dy=now.p[n+1]-before.p[n+1],dz=now.p[n+2]-before.p[n+2];float step=(float)Math.sqrt(dx*dx+dy*dy+dz*dz);maxEdit=Math.max(maxEdit,step);check(step<1,"Edit interpolation jumped a model pixel");}before=now;frames++;}
    sample(m,s);equal(before,capture(m,p),.00004f,"Client edit failed to reach authoritative geometry");
    DriveRules.setTotal(s,ChaoState.RUN,0,new Random(59));DriveRules.setTotal(s,ChaoState.SWIM,0,new Random(59));m.neutralSwimDevelopment(NeutralSwimAppearance059.prepare(id,s,22));equal(before,capture(m,p),.00004f,"Reversal used a stale display frame");featureContact(m,p);
    for(int t=23;t<=42;t++){m.neutralSwimDevelopment(NeutralSwimAppearance059.prepare(id,s,t));featureContact(m,p);}equal(original,capture(m,p),.00004f,"Reversal failed to recover centered form");
  }
  private static void motion()throws Exception{
    var clips=JsonParser.parseString(Files.readString(ROOT.resolve("animations/official/chao.animation.json"))).getAsJsonObject().getAsJsonObject("animations");var rig=new OfficialChaoRig(clips,NeutralSwimMorphRigTest059::asset);var expected=asset(MODEL);int serial=0;
    for(float age:new float[]{0,.5f,1})for(float sf:new float[]{-1,0})for(float rp:new float[]{-1,0,1})for(var mode:List.of(OfficialChaoRig.Motion.CRAWL,OfficialChaoRig.Motion.WALK,OfficialChaoRig.Motion.RUN,OfficialChaoRig.Motion.SIT,OfficialChaoRig.Motion.SWIM,OfficialChaoRig.Motion.GLIDE,OfficialChaoRig.Motion.HELD))for(int tick:new int[]{0,10,20}){
      var s=state(rp,sf,age);String saved=s.encode();String key=OfficialChaoRig.geometryKey(s);rig.prepareDevelopment(key,s);rig.animateGameplay(key,new UUID(59,++serial),mode,tick,1,.05f,0,0,0,0,false);rig.development(s);sample(expected,s);check(rig.finite(),"Lionfish pose nonfinite");check(rig.sampledMesh().shapeSignature()==expected.shapeSignature(),"Production selection skipped NeutralSwim059");check(rig.sourceGeometryKey().equals("baby"),"UV/attachment family changed");check(saved.equals(s.encode()),"Motion changed saved state");featureContact(rig.sampledMesh(),rig.poses());
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
      var s=state(1,-1,1);var rig=new OfficialChaoRig(clips,NeutralSwimMorphRigTest059::asset);String key=OfficialChaoRig.geometryKey(s);
      rig.prepareDevelopment(key,s);rig.animateGameplay(key,new UUID(59,590059+tick),OfficialChaoRig.Motion.PLAY,tick,0,0,0,0,0,0,false);rig.development(s);
      float weight=com.chaogarden.toys.QuietToyPlayRules049.presence(tick);
      ChaoQuietToyAnimation049.pose(rig.poses(),rig::transformBone,rig.toyPalm(false),rig.toyPalm(true),
          com.chaogarden.toys.QuietToyPlayRules049.BOOK,weight,quiet.sweep(tick),quiet.page(tick),quiet.stroke(tick),quiet.pen(tick),tick,.8f,rig::attachmentBounds);
      check(rig.finite(),"Production quiet-book pose is not finite");featureContact(rig.sampledMesh(),rig.poses());bookSurfaceFrames++;
    }
  }
  private static void stableCache()throws Exception{
    var clips=JsonParser.parseString(Files.readString(ROOT.resolve("animations/official/chao.animation.json"))).getAsJsonObject().getAsJsonObject("animations");var revision=ChaoMeshAsset.class.getDeclaredField("pathRevision056");revision.setAccessible(true);
    var s=state(-1,-1,.5f);var rig=new OfficialChaoRig(clips,NeutralSwimMorphRigTest059::asset);rig.prepareDevelopment(OfficialChaoRig.geometryKey(s),s);var mesh=rig.sampledMesh();check(mesh.neutralSwimPrototype059(),"Cache fixture selected wrong profile");
    int before=revision.getInt(mesh);long signature=mesh.shapeSignature();String saved=s.encode();
    for(int frame=0;frame<100;frame++){rig.development(s);check(revision.getInt(mesh)==before,"Unchanged state invalidated shape cache");check(mesh.shapeSignature()==signature,"Stable state changed emitted geometry");}check(saved.equals(s.encode()),"Repeated rendering changed saved state");
    DriveRules.setTotal(s,ChaoState.POWER,480,new Random(59));rig.development(s);check(revision.getInt(mesh)==before+1&&mesh.shapeSignature()!=signature,"Genuine training change did not invalidate the shape exactly once");int changed=revision.getInt(mesh);rig.development(s);check(revision.getInt(mesh)==changed,"New stable training kept invalidating cache");
    s=state(1,-1,1);rig.prepareDevelopment(OfficialChaoRig.geometryKey(s),s);rig.animateGameplay(OfficialChaoRig.geometryKey(s),new UUID(59,599001),OfficialChaoRig.Motion.WALK,10,1,.05f,0,0,0,0,false);rig.development(s);
    var gait=OfficialChaoRig.class.getDeclaredField("powerSwimWeight");gait.setAccessible(true);near(gait.getFloat(rig),1,.000001f,"Current Power specialization enables existing broad gait");DriveRules.setTotal(s,ChaoState.POWER,0,new Random(59));rig.development(s);near(gait.getFloat(rig),0,.000001f,"Actual Power loss clears broad gait rather than following stale legacy fields");
    var guard=OfficialChaoRig.class.getDeclaredField("broadWalkHead");guard.setAccessible(true);check(guard.getBoolean(rig),"Losing Power must retain the full-size fixed-head walking guard");
  }
  /** The unchanged full-size skull needs its baseline walking clearance at
   * every growth/training corner. Use a test-only disabled-guard control to
   * measure the real production walking pose, independently of Power arms. */
  private static void baselineHeadGuard()throws Exception{
    var clips=JsonParser.parseString(Files.readString(ROOT.resolve("animations/official/chao.animation.json"))).getAsJsonObject().getAsJsonObject("animations");
    var guard=OfficialChaoRig.class.getDeclaredField("broadWalkHead");guard.setAccessible(true);var power=OfficialChaoRig.class.getDeclaredField("powerSwimWeight");power.setAccessible(true);
    for(float age:new float[]{0,.5f,1})for(float sf:new float[]{-1,0})for(float rp:new float[]{-1,0,1}){
      var state=state(rp,sf,age);String saved=state.encode(),key=OfficialChaoRig.geometryKey(state);var id=new UUID(59,599059);
      var actual=new OfficialChaoRig(clips,NeutralSwimMorphRigTest059::asset);actual.prepareDevelopment(key,state);
      check(guard.getBoolean(actual),"Every Swim-first corner retains baseline full-head walking clearance");near(power.getFloat(actual),Math.max(0,rp)*age,.000001f,"Reversible Power arm spread remains independent of baseline head clearance");
      actual.animateGameplay(key,id,OfficialChaoRig.Motion.WALK,300,11,.05f,0,0,0,0,false,false);actual.development(state);
      var control=new OfficialChaoRig(clips,NeutralSwimMorphRigTest059::asset);control.prepareDevelopment(key,state);guard.setBoolean(control,false);power.setFloat(control,0);
      control.animateGameplay(key,id,OfficialChaoRig.Motion.WALK,300,11,.05f,0,0,0,0,false,false);
      near(control.poses().get("skull").y-actual.poses().get("skull").y,.35f,.00001f,"Actual full walking pose must keep its independent0.35px skull lift");
      check(actual.finite()&&saved.equals(state.encode()),"Walking clearance guard changes no saved age/training/identity");baselineGuardFrames++;
    }
  }
  /** Sample the production WALK emitter over its complete source stride,
   * including all breathing extrema, then the camera/look angles used by
   * transition regressions. Only the private stride phase is controlled;
   * actual clip sampling, guarding, breathing, hierarchy and boxes are real. */
  private static void headTorsoContinuity()throws Exception{
    var clips=JsonParser.parseString(Files.readString(ROOT.resolve("animations/official/chao.animation.json"))).getAsJsonObject().getAsJsonObject("animations");
    var clocks=OfficialChaoRig.class.getDeclaredField("gaitClocks");clocks.setAccessible(true);var phase=com.chaogarden.core.ChaoGaitClock.class.getDeclaredField("phase");phase.setAccessible(true);
    var guard=OfficialChaoRig.class.getDeclaredField("broadWalkHead");guard.setAccessible(true);
    for(int target=0;target<7;target++){
      float rp=target==0?0:(target-1)%3-1,sf=target==0?0:(target-1)/3-1,age=target==0?0:1;var state=state(rp,sf,age);String saved=state.encode(),key=OfficialChaoRig.geometryKey(state);var id=new UUID(59,599100+target);
      var rig=new OfficialChaoRig(clips,NeutralSwimMorphRigTest059::asset);rig.prepareDevelopment(key,state);check(guard.getBoolean(rig),"Fixed head needs its guard at every body-growth corner");
      for(int breathing:new int[]{-1,0,1}){
        long seed=id.getMostSignificantBits()^id.getLeastSignificantBits();double wanted=breathing<0?Math.PI*1.5:breathing>0?Math.PI*.5:0;float at=(float)((wanted-(seed&1023)*.01+Math.PI*8)/.055);
        rig.animateGameplay(key,id,OfficialChaoRig.Motion.WALK,at,11,.05f,0,0,0,0,false,false);
        Object clock=((Map<?,?>)clocks.get(rig)).get(id);check(clock!=null,"Actual stride clock was not initialized");
        for(int i=0;i<=1000;i++){
          phase.setDouble(clock,i/1000.0);rig.animateGameplay(key,id,OfficialChaoRig.Motion.WALK,at,11,.05f,0,0,0,0,false,false);rig.development(state);headTorsoConnected(rig,target,i,breathing,0,0);
        }
        for(int step=0;step<=8;step++)for(float yaw:new float[]{-12,0,12})for(float pitch:new float[]{-25,0,25}){
          phase.setDouble(clock,step/8.0);rig.animateGameplay(key,id,OfficialChaoRig.Motion.WALK,at,11,.05f,0,0,yaw,pitch,false,false);rig.development(state);headTorsoConnected(rig,target,step,breathing,yaw,pitch);
        }
      }
      check(saved.equals(state.encode()),"Dense walking clearance changed saved age/training/identity");
    }
    var body=data(MODEL).getAsJsonArray("bones").asList().stream().map(v->v.getAsJsonObject()).filter(v->v.get("name").getAsString().equals("body__cube_0")).findFirst().orElseThrow().getAsJsonArray("meshes").get(0).getAsJsonObject().getAsJsonObject("neutral_swim_box_059");
    float[] bottoms={.5f,.53f,.53f,.58f,.53f,.53f,.58f,.53f,.53f,.58f};for(int i=0;i<10;i++)near(body.getAsJsonArray("centers").get(i).getAsJsonArray().get(1).getAsFloat()+body.getAsJsonArray("sizes").get(i).getAsJsonArray().get(1).getAsFloat()/2,bottoms[i],.000001f,"Top-only body closure moved the inherited torso bottom");
  }
  private static void headTorsoConnected(OfficialChaoRig rig,int target,int phase,int breathing,float yaw,float pitch){
    var head=select(rig.sampledMesh(),rig.poses(),"skull__cube_0");var body=select(rig.sampledMesh(),rig.poses(),"body__cube_0");check(head.count()==24&&body.count()==24,"Head/torso box topology changed");
    check(intersects(head,0,body,0),"Actual WALK disconnected whole fixed skull from torso: target="+target+" phase="+phase+" breath="+breathing+" yaw="+yaw+" pitch="+pitch);check(rig.finite(),"Walking head/torso continuity used nonfinite poses");headTorsoFrames++;
  }
  private static void coats(){
    var m=asset(MODEL);var p=poses(m);
    for(float rp:new float[]{-1,0,1})for(float sf:new float[]{-1,0}){var s=state(rp,sf,1);sample(m,s);var reference=capture(m,p);for(int color=0;color<ChaoState.COLORS.length;color++)for(int jewel=0;jewel<ChaoState.JEWELS.length;jewel++)if(color==0||jewel==0)for(boolean shiny:new boolean[]{false,true})for(boolean twoTone:new boolean[]{false,true}){s.expressedColor=color;s.expressedJewel=jewel;s.shiny=shiny;s.twoTone=twoTone;sample(m,s);equal(reference,capture(m,p),0,"Color/shine changed form/UV");coatCases++;}}
  }
  public static void main(String[] args)throws Exception{
    long start=System.nanoTime();topology();development();silhouettes();temporal();motion();bookTipSurfaces();stableCache();baselineHeadGuard();headTorsoContinuity();coats();String report="NeutralSwim059: "+checks+" checks; "+frames+" emitted age/axis/edit frames; "+solids+" closed rectangular-box samples; "+motionFrames+" production Lionfish motion frames; "+attachments+" separate attachment matrices; "+coatCases+" coat/shape cases; "+orbPairs+" actual orb/feature separation checks; "+connectedFeatures+" connected feature samples; "+wingSamples+" emitted closed-wing samples; "+hingeSamples+" original-edge hinge samples; "+bookSurfaceFrames+" production quiet-book surface frames; "+baselineGuardFrames+" actual baseline walking-head guard comparisons; "+surfacePairs+" actual same-facing head/feature plane-pair checks; "+headTorsoFrames+" actual WALK head/torso SAT continuity frames; maximum hinge drift="+maxHinge+" model pixels; maximum axis step="+maxStep+" model pixels; maximum20tick edit step="+maxEdit+"; maximum1pct maturity step="+maxMaturity+"; seconds="+(System.nanoTime()-start)/1e9+". Fixed original head/face/orb/59-joint rig; six distinct mature corners; reversible independent age/axes; canonical baby UV/attachment alias and current production selection. Geometry validation, not visual/multiplayer verification.\n";Files.createDirectories(Path.of("build/reports/lionfish"));Files.writeString(Path.of("build/reports/lionfish/neutral-swim059-results.txt"),report);System.out.print(report);
  }
}
