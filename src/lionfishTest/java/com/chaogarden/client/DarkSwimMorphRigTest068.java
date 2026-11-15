package com.chaogarden.client;

import com.chaogarden.core.ChaoState;
import com.chaogarden.core.DriveRules;
import com.chaogarden.core.DarkSwimMorphology068;
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
public final class DarkSwimMorphRigTest068 {
  private static final Path ROOT=Path.of("src/main/resources/assets/chao_garden");
  private static final String MODEL="gameplay/chao_dark_swim_proto068";
  private static int checks,frames,solids,motionFrames,attachments,coatCases,markerPairs,wingSamples,hingeSamples,connectedFeatures,bookSurfaceFrames,baselineGuardFrames,surfacePairs,headTorsoFrames,palmHeadFrames,shoulderFrames,expressionFrames,wingUvSamples,tailSamples,featurePalmPairs,headCapUvSamples,lowHoodFrames,lowHoodPairs,wingLimbPairs;
  private static float maxStep,maxEdit,maxHinge,maxMaturity,maxWingPoseStep,maxTailPoseStep;
  private static int wingHeadFrames,wingHeadPairs,wingRootContacts,wingAnimalMatrices,wingTransitionFrames,tailTransitionFrames,posedInteriorFrames,tailScopeFrames,crawlPoseFrames,uprightStrideFrames;
  private static int featureBoxes,wingBoxesPerSide,totalBoxes,babyFrames,babyNubChecks;
  private static final String[] HEAD_FEATURE_NAMES={"dark_swim_upper_root","dark_swim_upper_tip","dark_swim_lower_root","dark_swim_lower_tip","dark_swim_side_left_spur","dark_swim_side_right_spur"};
  private static final Map<String,Vector3f> ORIGINAL_WING_HINGES=new HashMap<>();
  private static final java.util.Set<String> MORPH_BONES=new HashSet<>();
  private static Capture fixedDarkMarker;
  private static ChaoMeshAsset retainedTailReference;
  private static void check(boolean ok,String why){checks++;if(!ok)throw new AssertionError(why);}
  private static void near(float a,float b,float e,String why){check(Float.isFinite(a)&&Float.isFinite(b)&&Math.abs(a-b)<=e,why+": "+a+" / "+b);}
  private static JsonObject data(String key)throws Exception{return JsonParser.parseString(Files.readString(ROOT.resolve("models/entity/"+key+".json"))).getAsJsonObject();}
  private static ChaoMeshAsset asset(String key){try{var m=ChaoMeshAsset.parse(data(key));m.lens(0xff101820);m.expression(1);return m;}catch(Exception e){throw new IllegalStateException(key,e);}}
  private static ChaoState state(float rp,float sf,float age){
    var s=new ChaoState();s.adult=true;s.primaryType=ChaoState.SWIM_TYPE;s.align=-1;
    int r=Math.round(Math.abs(rp)*960),f=Math.round(Math.abs(sf)*960);
    int rs=rp<0?ChaoState.RUN:ChaoState.POWER,fs=sf<0?ChaoState.SWIM:ChaoState.FLY;
    s.levels[rs]=r/100;s.progress[rs]=r%100;s.points[rs]=r;
    s.levels[fs]=f/100;s.progress[fs]=f%100;s.points[fs]=f;
    s.setEvolutionStrength(age*1.2f);return ChaoState.decode(s.encode());
  }
  private static void sample(ChaoMeshAsset m,ChaoState s){m.development(s.swimFly,s.runPower,s.gradualVisualStrength());m.darkSwimDevelopment068(DarkSwimMorphology068.sample(s));}
  private static Map<String,ChaoMeshAsset.Pose> poses(ChaoMeshAsset m){var p=m.createPoses();for(String n:List.of("emotion_exclamation","emotion_heart","emotion_question","emotion_dizzy","monster_crown"))m.setSubtreeVisible(n,p,false);return p;}
  private static final class Capture implements VertexConsumer {
    float[] p=new float[256],uv=new float[256],normal=new float[256];int n,u,nn;
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
    try{p.values().forEach(v->v.visible=false);check(p.containsKey(name),"Missing sampled original head/feature bone "+name);p.get(name).visible=true;return capture(m,p);}
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
      for(int v=0;v<24;v++){int av=(ai+v)*3,bv=(bi+v)*3;float ap=a.p[av]*axis.x+a.p[av+1]*axis.y+a.p[av+2]*axis.z,bp=b.p[bv]*axis.x+b.p[bv+1]*axis.y+b.p[bv+2]*axis.z;alo=Math.min(alo,ap);ahi=Math.max(ahi,ap);blo=Math.min(blo,bp);bhi=Math.max(bhi,bp);}
      if(Math.min(ahi,bhi)-Math.max(alo,blo)<.000001f)return false;
    }return true;
  }
  private static void featureContact(ChaoMeshAsset m,Map<String,ChaoMeshAsset.Pose> p){
    var skull=select(m,p,"skull__cube_0");var features=select(m,p,"gradual_head_021");var marker=select(m,p,"emotion_dot__cube_0");
    check(skull.count()==24&&features.count()==featureBoxes*24,"Fixed skull or authored Dark Swim extension topology changed");
    surfacePlanes(inJointFrame(m,p,"skull",skull),inJointFrame(m,p,"skull",features));
    cuboids(inJointFrame(m,p,"gradual_head_021",features));
    var left=select(m,p,"skull__cube_2");var right=select(m,p,"skull__cube_3");
    var attached=new boolean[featureBoxes];int reached=0;
    for(int i=0;i<featureBoxes;i++){
      check(!intersects(features,i*24,left,0)&&!intersects(features,i*24,right,0),"Dark Swim head extension intersects an unchanged eye");
      for(int box=0;box<marker.count();box+=24){check(!intersects(features,i*24,marker,box),"Actual spiked Dark marker intersects a growing head extension");markerPairs++;}
      if(intersects(features,i*24,skull,0)){attached[i]=true;reached++;}
    }
    boolean changed=true;while(changed){changed=false;for(int i=0;i<featureBoxes;i++)if(!attached[i])for(int j=0;j<featureBoxes;j++)if(attached[j]&&intersects(features,i*24,features,j*24)){attached[i]=true;reached++;changed=true;break;}}
    check(reached==featureBoxes,"Growing Dark Swim extension disconnected from the original head or its adjoining root");connectedFeatures+=reached;
    for(int root:new int[]{0,2}){check(intersects(features,root*24,skull,0),"An axial Dark Swim extension root detached from the unchanged skull");check(intersects(features,root*24,features,(root+1)*24),"A Dark Swim distal tip detached from its own axial root");}
    for(int spur:new int[]{4,5})check(intersects(features,spur*24,features,2*24)||intersects(features,spur*24,skull,0),"A lateral Dark Swim spur detached from its hood/skull base");
    headCapFaces(features);
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
  /** Two axial root/tip extensions and a mirrored pair of lateral spurs. */
  private static void headExtensionsMirrored(Capture features,DarkSwimMorphology068.Sample sample){
    check(features.count()==featureBoxes*24,"Dark Swim head extension count changed");
    for(int root:new int[]{0,2})near(center(features,root).x,0,.00005f,"An axial Dark Swim root left its exact centerline");
    // The tiny1.5degree cap yaw separates differently painted side planes.
    for(int tip:new int[]{1,3})near(center(features,tip).x,0,.02f,"An axial Dark Swim cap moved materially away from its shared centerline");
    for(int i=0;i<24;i++){
      var a=features.point(4*24+i);boolean found=false;
      for(int j=0;j<24;j++){var b=features.point(5*24+j);if(Math.abs(a.x+b.x)<.00005f&&Math.abs(a.y-b.y)<.00005f&&Math.abs(a.z-b.z)<.00005f){found=true;break;}}
      check(found,"Dark Swim lateral spurs lost their mirror correspondence");
    }
  }
  private static void wings(ChaoMeshAsset m,Map<String,ChaoMeshAsset.Pose> p,boolean symmetric){
    var left=select(m,p,"wing_left__cube_0");var right=select(m,p,"wing_right__cube_0");
    check(left.count()==wingBoxesPerSide*24&&right.count()==wingBoxesPerSide*24,"Dark Swim wings lost their authored positive closed six-face plates");
    if(symmetric){cuboids(left);cuboids(right);}else{affineSurfaces(left);affineSurfaces(right);cuboids(inJointFrame(m,p,"wing_left",left));cuboids(inJointFrame(m,p,"wing_right",right));}
    wingSamples+=2*wingBoxesPerSide;if(symmetric)mirrored(left,right,"Dark Swim paired wing plates");
    wingHinge(m,p,"wing_left",left);wingHinge(m,p,"wing_right",right);
    wingSpanUv(m,p,"wing_left",left);wingSpanUv(m,p,"wing_right",right);
    var torso=select(m,p,"body__cube_0");
    for(int ray=0;ray<wingBoxesPerSide;ray++)check(intersects(left,ray*24,torso,0)&&intersects(right,ray*24,torso,0),"A rendered Dark Swim wing plate root detached from the actual torso");
  }
  private static void tailContact(ChaoMeshAsset mesh,Map<String,ChaoMeshAsset.Pose> poses){
    var tail=select(mesh,poses,"tail__cube_0");var base=select(mesh,poses,"tail__cube_1");
    check(tail.count()==48&&base.count()==24,"Dark Swim must keep its original tail base plus two closed root/tip sections");
    cuboids(inJointFrame(mesh,poses,"tail",tail));cuboids(inJointFrame(mesh,poses,"tail",base));
    if(retainedTailReference==null){retainedTailReference=asset("gameplay/chao_baby");retainedTailReference.development(0,0,0);}
    var referencePoses=retainedTailReference.createPoses();for(var entry:poses.entrySet())if(referencePoses.containsKey(entry.getKey()))mesh.copyJointPoseTo(entry.getKey(),retainedTailReference,entry.getValue(),referencePoses.get(entry.getKey()));
    equal(select(retainedTailReference,referencePoses,"tail__cube_1"),base,.00004f,"Original tail base geometry/UV changed");
    check(intersects(base,0,select(mesh,poses,"body__cube_0"),0),"Retained tail base detached from the actual posed torso");
    check(intersects(tail,0,base,0),"Dark Swim tail root detached from the retained actual base");
    check(intersects(tail,0,tail,24),"Dark Swim distal tail tip detached from its own root");
    for(String foot:List.of("foot_left__cube_0","foot_right__cube_0")){var sole=select(mesh,poses,foot);for(int box=0;box<2;box++)check(!intersects(tail,box*24,sole,0),"Actual posed Dark Swim tail intersects compact foot: "+foot+" tail box="+box);}
    tailSamples++;
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
    check(c.count()==wingBoxesPerSide*24,"Animated wing lost its closed plate boxes");
    for(int box=0;box<wingBoxesPerSide;box++){
      check(volume(c,box*24)>.00000005f,"Animated world wing ray collapsed/inverted");
      for(int i=box*24;i<(box+1)*24;i+=4){
        var a=c.point(i);var ab=c.point(i+1).sub(a);var bc=c.point(i+2).sub(c.point(i+1));var cd=c.point(i+3).sub(c.point(i+2));var da=a.sub(c.point(i+3));
        check(ab.lengthSquared()>1e-7f&&bc.lengthSquared()>1e-7f,"Animated wing ray lost an edge");
        check(new Vector3f(ab).add(cd).length()<.00005f&&new Vector3f(bc).add(da).length()<.00005f,"Animated wing ray lost opposite-edge correspondence");
        var normal=new Vector3f(ab).cross(bc).normalize();near(normal.dot(new Vector3f(c.normal[i*3],c.normal[i*3+1],c.normal[i*3+2])),1,.0002f,"Animated wing ray normal/winding differs from rendered face");
        near(new Vector3f(c.point(i+3)).sub(c.point(i)).dot(normal),0,.00005f,"Animated wing ray face is not planar");
      }
    }
  }
  private static void wingHinge(ChaoMeshAsset m,Map<String,ChaoMeshAsset.Pose> p,String joint,Capture wing){
    var expected=ORIGINAL_WING_HINGES.get(joint);check(expected!=null,"Missing original proximal wing edge");
    var local=inJointFrame(m,p,joint,wing);
    for(int box=0;box<wingBoxesPerSide;box++){
      float drift=Float.POSITIVE_INFINITY;
      for(int face=0;face<6;face++){var middle=new Vector3f();for(int v=0;v<4;v++)middle.add(local.point(box*24+face*4+v));drift=Math.min(drift,middle.div(4).distance(expected));}
      maxHinge=Math.max(maxHinge,drift);hingeSamples++;check(drift<.001f,"Rendered wing ray root face slid from its original proximal edge: "+drift);
    }
  }
  private static void wingUv(JsonObject bone,JsonObject original){
    check(bone.get("parent").equals(original.get("parent")),"Original wing parent changed");
    var hinge=new Vector3f();int hingeVertices=0;
    for(var value:original.getAsJsonArray("meshes").get(0).getAsJsonObject().getAsJsonArray("vertices")){
      var v=value.getAsJsonArray();if(Math.abs(v.get(0).getAsFloat())<.000001f){hinge.add(v.get(0).getAsFloat(),v.get(1).getAsFloat(),v.get(2).getAsFloat());hingeVertices++;}
    }
    check(hingeVertices==4,"Original wing no longer exposes canonical inner edge");ORIGINAL_WING_HINGES.put(original.get("parent").getAsString(),hinge.div(hingeVertices));
    var meshes=bone.getAsJsonArray("meshes");check(meshes.size()==wingBoxesPerSide,"Dark Swim wing lost its metadata-declared lobe count");
    String side=bone.get("name").getAsString().contains("left")?"left":"right";var names=new HashSet<String>();
    for(var value:meshes){
      var mesh=value.getAsJsonObject();String name=mesh.get("name").getAsString();check(name.startsWith("dark_swim_wing_"+side+"_")&&names.add(name),"Wing plate ownership/name is invalid or duplicated");
      check(mesh.has("dark_swim_box_068")&&mesh.getAsJsonArray("vertices").size()==8&&mesh.getAsJsonArray("faces").size()==6,"Wing plate lost its ten-target rigid closed-box construction");
      for(var element:mesh.getAsJsonArray("faces")){
        var face=element.getAsJsonObject();var uv=face.getAsJsonArray("uv");var ids=face.getAsJsonArray("v");check(uv.size()==4&&ids.size()==4,"Wing plate UV/corner correspondence changed");
        for(int i=0;i<4;i++){
          int id=ids.get(i).getAsInt();boolean positiveX=id==0||id==3||id==4||id==7;boolean distal=side.equals("right")==positiveX;
          var point=uv.get(i).getAsJsonArray();near(point.get(0).getAsFloat(),distal?53.25f:53.75f,0,"Wing root/distal UV does not follow the actual plate endpoint vertex");near(point.get(1).getAsFloat(),31.75f,0,"Wing face escaped the two adjacent opaque semantic texels");
        }
      }
    }
  }
  private static void wingSpanUv(ChaoMeshAsset mesh,Map<String,ChaoMeshAsset.Pose> poses,String joint,Capture wing){
    var local=inJointFrame(mesh,poses,joint,wing);var hinge=ORIGINAL_WING_HINGES.get(joint);
    for(int box=0;box<wingBoxesPerSide;box++){
      Vector3f root=null,tip=null;float nearest=Float.POSITIVE_INFINITY,farthest=-1;
      for(int face=0;face<6;face++){
        var center=new Vector3f();for(int i=0;i<4;i++)center.add(local.point(box*24+face*4+i));center.div(4);float distance=center.distanceSquared(hinge);
        if(distance<nearest){nearest=distance;root=center;}if(distance>farthest){farthest=distance;tip=center;}
      }
      check(root!=null&&tip!=null&&nearest<.000001f,"UV span did not start on the original physical wing hinge");var direction=new Vector3f(tip).sub(root);float span=direction.length();check(span>.5f,"UV span used a collapsed ray");direction.div(span);
      for(int i=0;i<24;i++){
        int vertex=box*24+i;float fraction=local.point(vertex).sub(root).dot(direction)/span;
        check(Math.abs(fraction)<.00002f||Math.abs(fraction-1)<.00002f,"Emitted box vertex no longer belongs to a ray endpoint");
        near(local.uv[vertex*2]*64,53.75f-.5f*fraction,.00005f,"Actual emitted wing UV does not follow its physical root-to-distal span");near(local.uv[vertex*2+1]*32,31.75f,.00005f,"Actual emitted wing UV leaves the opaque semantic row");
      }
      for(int face:new int[]{0,1,4,5}){
        var rootPoint=new Vector3f();var tipPoint=new Vector3f();float rootU=0,rootV=0,tipU=0,tipV=0;int roots=0,tips=0;
        for(int i=0;i<4;i++){
          int vertex=box*24+face*4+i;var point=local.point(vertex);float fraction=new Vector3f(point).sub(root).dot(direction)/span;
          if(fraction<.5f){rootPoint.add(point);rootU+=local.uv[vertex*2]*64;rootV+=local.uv[vertex*2+1]*32;roots++;}
          else{tipPoint.add(point);tipU+=local.uv[vertex*2]*64;tipV+=local.uv[vertex*2+1]*32;tips++;}
        }
        check(roots==2&&tips==2,"A long wing face lost root/distal endpoint correspondence");rootPoint.div(roots);tipPoint.div(tips);rootU/=roots;rootV/=roots;tipU/=tips;tipV/=tips;int distalSamples=0;
        for(int step=0;step<100;step++){
          float t=(step+.5f)/100;var point=new Vector3f(rootPoint).lerp(tipPoint,t);float physicalFraction=point.sub(root).dot(direction)/span;
          near(physicalFraction,t,.00003f,"Wing face UV sample does not follow the actual emitted span");float u=rootU+(tipU-rootU)*t,v=rootV+(tipV-rootV)*t;
          int x=(int)Math.floor(u*2),y=(int)Math.floor(v*2),expected=physicalFraction<.5f?107:106;
          check(x==expected&&y==63,"Actual wing span lost its broad root/distal color band or sampled another atlas region");
          if(x==106)distalSamples++;wingUvSamples++;
        }
        check(distalSamples==50,"Distal semantic pigment must cover exactly the outer half of every long plate face");
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
    var poses=m.createPoses();
    for(String name:List.of("arm_left__cube_1","arm_right__cube_1","foot_left__cube_0","foot_right__cube_0")){
      boolean foot=name.startsWith("foot");String joint=name.substring(0,name.indexOf("__"));var c=inJointFrame(m,poses,joint,solid(m,name));cuboids(c);var b=bounds(c,0,c.count());float min=999,max=0;
      for(int axis=0;axis<3;axis++){min=Math.min(min,b[axis+3]-b[axis]);max=Math.max(max,b[axis+3]-b[axis]);}check(max/min<(foot?2.6f:1.4f),"Stretched compact hand/foot");
      if(foot){near(b[4],2,.00005f,"Foot growth moved its fixed local sole plane");near(b[5],0,.00005f,"Foot growth moved its fixed rear heel plane");}
      else near(b[1],0,.00005f,"Hand growth must remain below its fixed original wrist upper plane");
    }
  }
  /** Byte-exact protected assets through completed local0.25.67, independent of the
   * generator and new profile's metadata. These are portable source checks. */
  private static void protectedAssets()throws Exception{
    // .72 deliberately added reciprocal Swim/Fly rows; only those two pins
    // advance. Historical rig/head/retained-target preservation was audited.
    var expected=Map.ofEntries(
        Map.entry("chao_baby.json","ae3d4da88b84905f8726b863bdb072e3a26eb404f38e565d0781b92b01e2cf9b"),
        Map.entry("chao_hero_run_proto055.json","77b41d4501cba585b83be91b4052e8c56e03bff02e47db2b209f146d36390f08"),
        Map.entry("chao_neutral_proto056.json","a354458cae5605b873464614b57f9f9bd04d4a4e01d1ed793cf949194c4d14db"),
        Map.entry("chao_hero_normal_proto057.json","d0052db7f401ecc545e7a3db2992933b5671387520c82429d4a8aa336f28232d"),
        Map.entry("chao_neutral_swim_proto059.json","de2972ea6780a4252a98bbbdd84d674dd25a9ea4f3c1ea04be7124e3283593b1"),
        Map.entry("chao_dark_normal_proto060.json","36ae27aed51674ea31310ab4af602e3e7127e1507c736ee9b6f75f3728acb514"),
        Map.entry("chao_neutral_fly_proto061.json","05ce4c9211bee32d055575864e27e7e1a925ca68a838481810a57df7e507e203"),
        Map.entry("chao_neutral_run_proto062.json","cfed60309de43bc854272a4869d455d9ec76281a37a1787ff223f69b7ed85881"),
        Map.entry("chao_neutral_power_proto063.json","c3bfcbb2fb1730bbd23b8799e35d9a2edf33b3f4fab06c11a4638bacd1db07f0"),
        Map.entry("chao_hero_swim_proto064.json","73a773cf29df2b8aa2e8e6d471428d8b3a7412a4ba1eab3809dace7f00a0b324"),
        Map.entry("chao_hero_fly_proto065.json","f5847fe9bc3429c03c5340f0cf65ff99649537759df9f5c993d25b53feb332cf"),
        Map.entry("chao_hero_run_proto066.json","6ee8f47a220d769fa1e66a60cade8b7e8d211b16bafeca78dcd77074a8ae0e17"),
        Map.entry("chao_hero_power_proto067.json","37d01b59876860b7ae11f895169a8a0a59f1b32dd716481a996d1a3d52e9d467"));
    for(var entry:expected.entrySet()){
      byte[] bytes=Files.readAllBytes(ROOT.resolve("models/entity/gameplay/"+entry.getKey()));
      String actual=java.util.HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256").digest(bytes));
      check(actual.equals(entry.getValue()),"Previously delivered model changed: "+entry.getKey());
    }
  }
  private static void topology()throws Exception{
    var d=data(MODEL);var original=data("gameplay/chao_baby");var metadata=JsonParser.parseString(Files.readString(ROOT.resolve("dark_swim068.json"))).getAsJsonObject();
    check(d.has("dark_swim_path_068"),"Dark Swim lacks its own admission profile");
    for(String old:List.of("hero_power_path_067","hero_run_path_066","hero_fly_path_065","hero_swim_path_064","neutral_power_path_063","neutral_run_path_062","neutral_fly_path_061","dark_normal_path_060","neutral_swim_path_059","hero_normal_path_057","neutral_path_056","blocky_run_path_055"))check(!d.has(old),"Dark Swim reused an earlier family admission flag: "+old);
    featureBoxes=6;wingBoxesPerSide=3;totalBoxes=25;
    check(d.getAsJsonObject("dark_swim_path_068").get("feature_count").getAsInt()==featureBoxes&&d.getAsJsonObject("dark_swim_path_068").get("wing_ray_count").getAsInt()==2*wingBoxesPerSide&&d.getAsJsonObject("dark_swim_path_068").get("positive_closed_cuboid_target_count").getAsInt()==totalBoxes,"Metadata disagrees with the independently counted Dark Swim construction");
    Map<String,JsonObject> old=new HashMap<>();for(var b:original.getAsJsonArray("bones"))old.put(b.getAsJsonObject().get("name").getAsString(),b.getAsJsonObject());
    check(old.size()==59&&d.getAsJsonArray("bones").size()==59,"All59 original shared joints are required without extra attachment joints");
    for(int i=0;i<59;i++)check(d.getAsJsonArray("bones").get(i).getAsJsonObject().get("name").equals(original.getAsJsonArray("bones").get(i).getAsJsonObject().get("name")),"Original59 shared-joint ordering changed");
    int boxes=0;
    for(var be:d.getAsJsonArray("bones")){
      var b=be.getAsJsonObject();String name=b.get("name").getAsString();check(old.containsKey(name),"Unexpected new rig joint: "+name);
      check(b.get("pivot").equals(old.get(name).get("pivot"))&&b.get("rotation").equals(old.get(name).get("rotation"))&&java.util.Objects.equals(b.get("parent"),old.get(name).get("parent")),"Original rig/attachment joint changed "+name);
      if(name.startsWith("skull")||name.startsWith("expression_")||name.equals("tail__cube_1")||name.startsWith("emotion_")&&!name.equals("emotion_dot__cube_0"))check(b.equals(old.get(name)),"Original head/face/reaction/tail base changed "+name);
      if(name.equals("wing_left__cube_0")||name.equals("wing_right__cube_0"))wingUv(b,old.get(name));
      for(var me:b.getAsJsonArray("meshes")){
        var part=me.getAsJsonObject();if(!part.has("dark_swim_box_068"))continue;boxes++;MORPH_BONES.add(name);
        check(part.getAsJsonArray("vertices").size()==8&&part.getAsJsonArray("faces").size()==6,"Authored part is not one closed cuboid");
        check(part.getAsJsonObject("dark_swim_box_068").keySet().equals(java.util.Set.of("centers","sizes","rotations")),"Closed cuboid parameter fields changed");
        for(var field:part.getAsJsonObject("dark_swim_box_068").entrySet()){check(field.getValue().getAsJsonArray().size()==10,"Part lost full ten-target correspondence");for(var vector:field.getValue().getAsJsonArray()){check(vector.getAsJsonArray().size()==3,"Target vector lost XYZ correspondence");for(var component:vector.getAsJsonArray())check(Float.isFinite(component.getAsFloat())&&(!field.getKey().equals("sizes")||component.getAsFloat()>0),"Nonfinite/zero/negative closed-cuboid parameter");}}
      }
    }
    check(boxes==totalBoxes,"Dark Swim must use6head+6wing+2tail+6marker+5body/limb target boxes");
    check(d.get("head_decorations").equals(original.get("head_decorations")),"Original hat hiding family changed");
    var profile=d.getAsJsonObject("dark_swim_path_068");check(profile.getAsJsonArray("targets").size()==10,"Wrong fresh-plus-full-signed-grid target count");
    var names=List.of("dark_swim_young","run_swim","swim","power_swim","run","balanced","power","run_fly","fly","power_fly");
    for(int i=0;i<10;i++)check(profile.getAsJsonArray("targets").get(i).getAsString().equals(names.get(i))&&metadata.getAsJsonArray("target_names").get(i).getAsString().equals(names.get(i)),"Signed-grid target order differs from normalized current-stat axes");
    var features=d.getAsJsonArray("bones").asList().stream().map(v->v.getAsJsonObject()).filter(v->v.get("name").getAsString().equals("gradual_head_021")).findFirst().orElseThrow().getAsJsonArray("meshes");
    check(features.size()==6,"Dark Swim must use two axial root/tip extensions plus two lateral spurs");
    for(int i=0;i<6;i++){check(features.get(i).getAsJsonObject().get("name").getAsString().equals(HEAD_FEATURE_NAMES[i])&&metadata.getAsJsonArray("head_parts").get(i).getAsString().equals(HEAD_FEATURE_NAMES[i]),"Dark Swim feature ownership/order changed");pointCapUv(features.get(i).getAsJsonObject(),i!=0&&i!=2);}
  }

  /** Opaque root surfaces and broad tip bands use the actual template IDs.
   * Tip U follows positive local Z before the authored target rotations. */
  private static void pointCapUv(JsonObject mesh,boolean tip){
    // The frozen template is +X at IDs0/3/4/7 and +Z at IDs4/5/6/7.
    // Use the actual face vertex IDs, before any authored target rotation.
    int faceIndex=0;for(var element:mesh.getAsJsonArray("faces")){
      var face=element.getAsJsonObject();var uv=face.getAsJsonArray("uv");var ids=face.getAsJsonArray("v");check(uv.size()==4&&ids.size()==4,"Head extension UV/corner correspondence changed");
      for(int i=0;i<4;i++){int id=ids.get(i).getAsInt();float expectedNative=tip?(id>=4?109.5f:110.5f):faceIndex==1||faceIndex==4?110.5f:faceIndex==5?108.5f:111.5f;var point=uv.get(i).getAsJsonArray();near(point.get(0).getAsFloat(),expectedNative*.5f,0,"Head extension UV does not follow its physical longitudinal endpoint/face ownership");near(point.get(1).getAsFloat(),31.75f,0,"Head extension escaped its scoped semantic texel row");}faceIndex++;
    }
  }
  /** Inspect actual emitted quads and their interpolated interiors.
   * Every long tip/spur face must carry the two physical half-span pigments. */
  private static void headCapFaces(Capture features){
    for(int box=0;box<featureBoxes;box++)for(int face=0;face<6;face++){
      boolean tip=box!=0&&box!=2;int proximalSamples=0,distalSamples=0;
      var rootFace=centerFace(features,box,0);var endFace=centerFace(features,box,1);var direction=new Vector3f(endFace).sub(rootFace);float span=direction.lengthSquared();check(span>1e-7f,"Head extension lost its physical longitudinal span");
      for(int row=0;row<10;row++)for(int column=0;column<10;column++){
        float x=(column+.5f)/10,y=(row+.5f)/10;float[] weights={(1-x)*(1-y),x*(1-y),x*y,(1-x)*y};float u=0,v=0;var point=new Vector3f();
        for(int i=0;i<4;i++){int at=box*24+face*4+i;u+=features.uv[at*2]*128*weights[i];v+=features.uv[at*2+1]*64*weights[i];point.fma(weights[i],features.point(at));}
        int px=(int)Math.floor(u),py=(int)Math.floor(v);float fraction=point.sub(rootFace).dot(direction)/span;
        int expected=tip?(fraction<.5f?110:109):face==1||face==4?110:face==5?108:111;
        check(px==expected&&py==63,"Actual emitted head extension paint does not follow its physical root/distal span or face region");if(px==110)proximalSamples++;if(px==109)distalSamples++;headCapUvSamples++;
      }
      if(tip&&face>=2)check(proximalSamples==50&&distalSamples==50,"Every long head tip/spur face must carry both broad semantic pigments over its physical halves");
    }
  }
  private static Vector3f centerFace(Capture capture,int box,int face){var center=new Vector3f();for(int i=0;i<4;i++)center.add(capture.point(box*24+face*4+i));return center.div(4);}

  /** Retain the approved055 baby construction and its separate front-side
   * brow nubs. The new Dark Swim adult route must not replace that anatomy. */
  private static void retainedBabyRoute()throws Exception{
    var legacy=data("gameplay/chao_hero_run_proto055");
    var features=legacy.getAsJsonArray("bones").asList().stream().map(v->v.getAsJsonObject()).filter(v->v.get("name").getAsString().equals("gradual_head_021")).findFirst().orElseThrow().getAsJsonArray("meshes");
    check(features.size()==16&&features.get(6).getAsJsonObject().get("name").getAsString().equals("neutral_run_front_nub_left")&&features.get(7).getAsJsonObject().get("name").getAsString().equals("neutral_run_front_nub_right"),"Approved baby front-side nubs must remain separate from its rear spikes");
    var clips=JsonParser.parseString(Files.readString(ROOT.resolve("animations/official/chao.animation.json"))).getAsJsonObject().getAsJsonObject("animations");
    for(float alignment:new float[]{-1,-.5f,0,.5f,1})for(float run:new float[]{0,.5f,1})for(float age:new float[]{0,.5f,.99f}){
      var state=new ChaoState();state.adult=false;state.align=alignment;state.primaryType=ChaoState.NORMAL;
      DriveRules.setTotal(state,ChaoState.RUN,Math.round(run*480),new Random(66));state.setEvolutionStrength(age);state=ChaoState.decode(state.encode());
      String saved=state.encode(),key=OfficialChaoRig.geometryKey(state);check(key.equals("baby"),"Baby logical identity changed during adult Dark Swim completion");
      var rig=new OfficialChaoRig(clips,DarkSwimMorphRigTest068::asset);rig.prepareDevelopment(key,state);
      check(rig.sampledMesh().heroRunPrototype()&&!rig.sampledMesh().darkSwimPrototype068()&&!DarkSwimMorphology068.sample(state).enabled(),"Current baby was rerouted to adult068 or lost055");
      var expected=asset("gameplay/chao_hero_run_proto055");expected.development(state.swimFly,state.runPower,state.gradualVisualStrength()*.14f);expected.heroRunDevelopment(com.chaogarden.core.HeroRunMorphology052.sample(state),state.swimFly,state.runPower);expected.heroRunTwoTone(state.baseColor()==0&&state.jewel()==0&&state.isTwoTone());
      // prepareDevelopment precedes frame symbol visibility. Normalize both
      // static captures to the same ordinary marker set; do not compare an
      // all-glyph authoring pose with the helper's hidden reaction glyphs.
      equal(capture(expected,poses(expected)),capture(rig.sampledMesh(),poses(rig.sampledMesh())),.00004f,"Retained baby055 anatomy or canonical UV changed");
      check(saved.equals(state.encode()),"Baby route sampling changed age, alignment, stats or first identity");babyFrames++;
      if(alignment==0&&run==1&&age==.99f){
        var mesh=rig.sampledMesh();var p=rig.poses();var head=inJointFrame(mesh,p,"skull",select(mesh,p,"skull__cube_0"));var growing=inJointFrame(mesh,p,"skull",select(mesh,p,"gradual_head_021"));
        var eyeLeft=inJointFrame(mesh,p,"skull",select(mesh,p,"skull__cube_2"));var eyeRight=inJointFrame(mesh,p,"skull",select(mesh,p,"skull__cube_3"));
        for(int nub:new int[]{6,7}){
          var c=center(growing,nub);var eye=nub==6?eyeLeft:eyeRight;
          check(Math.abs(c.x)>Math.abs(center(eye,0).x)&&c.y<center(eye,0).y&&c.z<0,"Run baby nub left its front-side position above/outside the eyes");
          check(intersects(growing,nub*24,head,0),"Run baby nub detached from the original head box");
          check(!intersects(growing,nub*24,eyeLeft,0)&&!intersects(growing,nub*24,eyeRight,0),"Run baby nub intersects an unchanged eye");babyNubChecks++;
        }
      }
    }
  }

  private static void development(){
    var m=asset(MODEL);var original=asset("gameplay/chao_baby");original.development(0,0,0);var p=poses(m);var head=solid(original,"skull__cube_0");var eyeL=solid(original,"skull__cube_2");var eyeR=solid(original,"skull__cube_3");
    var endpoints=new HashSet<Long>();
    for(float age:new float[]{0,.25f,.5f,.75f,1})for(int yi=0;yi<=10;yi++)for(int xi=0;xi<=10;xi++){
      var state=state(xi/5f-1,yi/5f-1,age);String saved=state.encode();sample(m,state);frames++;for(String bone:MORPH_BONES)cuboids(solid(m,bone));
      equal(head,solid(m,"skull__cube_0"),.00004f,"Original 7x6x6 head resized");equal(eyeL,solid(m,"skull__cube_2"),.00004f,"Left eye moved/distorted");equal(eyeR,solid(m,"skull__cube_3"),.00004f,"Right eye moved/distorted");marker(m,DarkSwimMorphology068.sample(state));
      cuboids(solid(m,"gradual_head_021"));headExtensionsMirrored(solid(m,"gradual_head_021"),DarkSwimMorphology068.sample(state));compact(m);featureContact(m,p);wings(m,p,true);tailContact(m,p);check(saved.equals(state.encode()),"Rendering changed saved data");
      if(age==1&&xi%5==0&&yi%5==0)endpoints.add(m.shapeSignature());
    }
    check(endpoints.size()==9,"Requested nine mature combinations collapsed into duplicate forms");
    for(float age:new float[]{0,.5f,1}){
      for(float sf:new float[]{-1,0,1}){Capture before=null;for(int i=0;i<=100;i++){sample(m,state(i/50f-1,sf,age));var now=capture(m,p);surfacePlanes(m,p);maxStep=Math.max(maxStep,step(before,now,.3f,"Small Run/Power change popped geometry"));before=now;frames++;}}
      for(float rp:new float[]{-1,0,1}){Capture before=null;for(int i=0;i<=100;i++){sample(m,state(rp,i/50f-1,age));var now=capture(m,p);surfacePlanes(m,p);maxStep=Math.max(maxStep,step(before,now,.3f,"Small Swim/Fly change popped geometry"));before=now;frames++;}}
    }
    for(float rp:new float[]{-1,0,1})for(float sf:new float[]{-1,0,1}){Capture before=null;for(int i=0;i<=100;i++){sample(m,state(rp,sf,i/100f));var now=capture(m,p);surfacePlanes(m,p);maxMaturity=Math.max(maxMaturity,step(before,now,.15f,"One-percent maturity change popped geometry"));before=now;frames++;}}
    sample(m,state(-1,1,0));long fresh=m.shapeSignature();sample(m,state(1,0,0));check(fresh==m.shapeSignature(),"Training overrides independent fresh-adult age");
    for(float rp:new float[]{-1,0,1}){sample(m,state(rp,0,1));long balanced=m.shapeSignature();sample(m,state(rp,-1,1));long swim=m.shapeSignature();sample(m,state(rp,1,1));long fly=m.shapeSignature();check(balanced!=swim&&balanced!=fly&&swim!=fly,"Dark Swim-first preserves distinct balanced, Swim and Fly surfaces on either side of the opposing axis");}
    var state=state(-1,1,1);sample(m,state);long before=m.shapeSignature();float maturity=state.evolutionStrength();DriveRules.setTotal(state,ChaoState.RUN,0,new Random(64));sample(m,state);check(before!=m.shapeSignature()&&maturity==state.evolutionStrength(),"Run loss does not reverse independently from age");
    var copy=ChaoState.decode(state.encode());sample(m,copy);long loaded=m.shapeSignature();sample(m,state);check(loaded==m.shapeSignature(),"Save/reload changed sampled geometry");m.hideHeadDecorations(p);check(!p.get("gradual_head_021").visible,"Hat does not hide new head features");
  }
  private static float step(Capture before,Capture now,float limit,String why){
    if(before==null)return 0;check(before.n==now.n&&before.u==now.u,"Transition swaps topology");float largest=0;
    for(int i=0;i<now.n;i+=3){float dx=now.p[i]-before.p[i],dy=now.p[i+1]-before.p[i+1],dz=now.p[i+2]-before.p[i+2];float amount=(float)Math.sqrt(dx*dx+dy*dy+dz*dz);largest=Math.max(largest,amount);check(amount<limit,why+": "+amount);}
    for(int i=0;i<now.u;i++)near(before.uv[i],now.uv[i],0,"Transition distorts canonical UVs");return largest;
  }
  /** Check recognizable directional differences on emitted, transformed
   * geometry. These test the authored blocky interpretation, not source-mesh
   * equality claims. In the unchanged head joint, +Z is rear and +Y is down. */
  private static void marker(ChaoMeshAsset mesh,DarkSwimMorphology068.Sample sample){
    if(fixedDarkMarker==null){var original=asset("gameplay/chao_dark_normal_proto060");fixedDarkMarker=inJointFrame(original,original.createPoses(),"emotion_dot",solid(original,"emotion_dot__cube_0"));}
    var actual=inJointFrame(mesh,mesh.createPoses(),"emotion_dot",solid(mesh,"emotion_dot__cube_0"));
    check(actual.count()==7*24,"Dark Swim must use the actual Dark central orb and six closed spike boxes");
    equal(fixedDarkMarker,actual,.00004f,"Existing source-appropriate Dark marker geometry/UV changed with age or training");cuboids(actual);
    for(int spike=24;spike<actual.count();spike+=24)check(intersects(actual,0,actual,spike),"Dark marker spike detached from its preserved center");
  }

  /** Temporary opt-in numerical diagnostic, using the same production WALK
   * clip/clock/renderer and independent emitted-box SAT as the assertion pass. */
  private static void diagnoseWalk()throws Exception{
    var clips=JsonParser.parseString(Files.readString(ROOT.resolve("animations/official/chao.animation.json"))).getAsJsonObject().getAsJsonObject("animations");
    var clocks=OfficialChaoRig.class.getDeclaredField("gaitClocks");clocks.setAccessible(true);var phase=com.chaogarden.core.ChaoGaitClock.class.getDeclaredField("phase");phase.setAccessible(true);
    int contacts=0,samples=0,shoulderMisses=0;float worst=0,worstShoulder=0;String detail="none",shoulderDetail="none";var shoulders=new StringBuilder("age,rp,sf,breathing,phase,yaw,pitch,hand,gap_px\n");var out=new StringBuilder("age,rp,sf,breathing,phase,yaw,pitch,hand,overlap_px\n");
    for(float age:new float[]{0,.5f,1})for(float sf:new float[]{-1,0,1})for(float rp:new float[]{-1,0,1}){
      var s=state(rp,sf,age);String key=OfficialChaoRig.geometryKey(s);var id=new UUID(64,600100);var rig=new OfficialChaoRig(clips,DarkSwimMorphRigTest068::asset);rig.prepareDevelopment(key,s);
      for(int breathing:new int[]{-1,0,1}){
        long seed=id.getMostSignificantBits()^id.getLeastSignificantBits();double wanted=breathing<0?Math.PI*1.5:breathing>0?Math.PI*.5:0;float at=(float)((wanted-(seed&1023)*.01+Math.PI*8)/.055);
        rig.animateGameplay(key,id,OfficialChaoRig.Motion.WALK,at,11,.05f,0,0,0,0,false,false);Object clock=((Map<?,?>)clocks.get(rig)).get(id);
        for(int step=0;step<=8;step++)for(float yaw:new float[]{-37.25f,-12,0,12,37.25f})for(float pitch:new float[]{-25,0,25}){
          phase.setDouble(clock,step/8.0);rig.animateGameplay(key,id,OfficialChaoRig.Motion.WALK,at,11,.05f,0,0,yaw,pitch,false,false);rig.development(s);
          var head=select(rig.sampledMesh(),rig.poses(),"skull__cube_0");
          for(String hand:List.of("arm_left__cube_1","arm_right__cube_1")){
            var palm=select(rig.sampledMesh(),rig.poses(),hand);float depth=overlapDepth(head,palm);samples++;
            var torso=select(rig.sampledMesh(),rig.poses(),"body__cube_0");var upper=select(rig.sampledMesh(),rig.poses(),hand.replace("__cube_1","__cube_0"));float shoulderGap=-overlapDepth(torso,upper);
            if(shoulderGap>.0001f){shoulderMisses++;shoulders.append(age+","+rp+","+sf+","+breathing+","+step+","+yaw+","+pitch+","+hand+","+shoulderGap+"\n");}
            if(shoulderGap>worstShoulder){worstShoulder=shoulderGap;shoulderDetail="age="+age+" rp="+rp+" sf="+sf+" breath="+breathing+" phase="+step+"/8 hand="+hand;}
            if(depth>0){contacts++;out.append(age+","+rp+","+sf+","+breathing+","+step+","+yaw+","+pitch+","+hand+","+depth+"\n");}
            if(depth>worst){worst=depth;detail="age="+age+" rp="+rp+" sf="+sf+" breath="+breathing+" phase="+step+"/8 yaw="+yaw+" pitch="+pitch+" hand="+hand+" headBounds="+Arrays.toString(bounds(head,0,24))+" palmBounds="+Arrays.toString(bounds(palm,0,24));}
          }
        }
      }
    }
    String report="DarkSwim068 production WALK palm diagnostic: contacts="+contacts+" / "+samples+"; maximum minimum-axis SAT overlap="+worst+" model pixels; "+detail+"; shoulder gaps="+shoulderMisses+"/"+samples+" maximum separating-axis gap="+worstShoulder+"px "+shoulderDetail+"\n";
    Files.createDirectories(Path.of("build/reports/lionfish"));Files.writeString(Path.of("build/reports/lionfish/dark-swim068-walk-diagnostic.csv"),out);Files.writeString(Path.of("build/reports/lionfish/dark-swim068-shoulder-diagnostic.csv"),shoulders);Files.writeString(Path.of("build/reports/lionfish/dark-swim068-walk-diagnostic.txt"),report);System.out.print(report);
  }
  private static float overlapDepth(Capture a,Capture b){
    var axes=new java.util.ArrayList<Vector3f>();for(int face=0;face<6;face++){
      axes.add(new Vector3f(a.normal[face*12],a.normal[face*12+1],a.normal[face*12+2]));axes.add(new Vector3f(b.normal[face*12],b.normal[face*12+1],b.normal[face*12+2]));
    }
    for(var x:boxEdges(a,0))for(var y:boxEdges(b,0))axes.add(new Vector3f(x).cross(y));float smallest=Float.POSITIVE_INFINITY;
    for(var axis:axes){if(axis.lengthSquared()<1e-12f)continue;axis.normalize();float al=99999,ah=-99999,bl=99999,bh=-99999;
      for(int v=0;v<24;v++){float ap=a.point(v).dot(axis),bp=b.point(v).dot(axis);al=Math.min(al,ap);ah=Math.max(ah,ap);bl=Math.min(bl,bp);bh=Math.max(bh,bp);}
      float overlap=Math.min(ah,bh)-Math.max(al,bl);if(overlap<=0)return overlap;smallest=Math.min(smallest,overlap);
    }return smallest;
  }
  private static Vector3f center(Capture capture,int box){var center=new Vector3f();for(int i=0;i<24;i++)center.add(capture.point(box*24+i));return center.div(24);}
  private static Capture crest(ChaoMeshAsset mesh,Map<String,ChaoMeshAsset.Pose> poses,float rp,float sf){sample(mesh,state(rp,sf,1));return inJointFrame(mesh,poses,"skull",solid(mesh,"gradual_head_021"));}
  /** Viewer-informed directional relationships for authored Dark Swim features.
   * Compare emitted surfaces, not generator parameter values. */
  private static float longestEdge(Capture mesh,int box){float length=0;for(var edge:boxEdges(mesh,box*24))length=Math.max(length,edge.length());return length;}
  private static Capture localShape(ChaoMeshAsset mesh,Map<String,ChaoMeshAsset.Pose> poses,String joint,String bone,float rp,float sf,float age){sample(mesh,state(rp,sf,age));return inJointFrame(mesh,poses,joint,solid(mesh,bone));}
  private static void silhouettes(){
    var mesh=asset(MODEL);var p=poses(mesh);var signatures=new HashSet<Long>();
    for(int sf=-1;sf<=1;sf++)for(int rp=-1;rp<=1;rp++){
      sample(mesh,state(rp,sf,1));signatures.add(mesh.shapeSignature());var features=solid(mesh,"gradual_head_021");headExtensionsMirrored(features,new DarkSwimMorphology068.Sample(true,1,rp,sf));
      var local=inJointFrame(mesh,p,"skull",features);check(width(local,2)>width(local,0)*2,"Dark Swim lower hood lost its broad transverse contrast with the narrow upper crest");
      for(int tip:new int[]{1,3})check(center(local,tip).z>center(local,tip-1).z,"An axial Dark Swim tip no longer trails its own root");
      var wing=inJointFrame(mesh,p,"wing_left",solid(mesh,"wing_left__cube_0"));for(int box=0;box<wingBoxesPerSide;box++)check(longestEdge(wing,box)<8,"Dark Swim back-wing ray became disproportionate to the fixed head");
    }
    check(signatures.size()==9,"Nine signed Dark Swim specializations collapsed into duplicate forms");darkSwimDirectionalSilhouettes(mesh,p);
  }

  /** Quantitative relationships from actual independently viewed Dark Swim-first
   * front/side/back references. +Z is rear and +Y is down in the fixed skull
   * joint. These broad silhouette directions do not equate source geometry. */

  private static float outerWing(ChaoMeshAsset mesh,Map<String,ChaoMeshAsset.Pose> p,float rp,float sf){
    var shape=localShape(mesh,p,"body","wing_left__cube_0",rp,sf,1);float span=0;
    for(int i=0;i<shape.count();i++)span=Math.max(span,Math.abs(shape.point(i).x));return span;
  }

  private static float width(Capture boxes,int box){float[] b=bounds(boxes,box*24,24);return b[3]-b[0];}
  /** Broad source relationships from personally opened actual Dark Swim-first
   * balanced mature side/back/above, balanced young side, and mature
   * Run/Power/Swim/Fly side controls. +Z is rear and +Y is down in the fixed
   * skull. These test the blocky interpretation, not source-mesh equality. */
  private static void darkSwimDirectionalSilhouettes(ChaoMeshAsset mesh,Map<String,ChaoMeshAsset.Pose> p){
    var fresh=localShape(mesh,p,"skull","gradual_head_021",0,0,0);var balanced=crest(mesh,p,0,0);var run=crest(mesh,p,-1,0);var power=crest(mesh,p,1,0);var swim=crest(mesh,p,0,-1);
    check(bounds(balanced,0,48)[5]>4&&bounds(balanced,48,48)[5]>4,"Balanced Dark Swim must retain two distinct rear extensions beyond the fixed head rear plane3");
    check(center(balanced,1).y<center(balanced,3).y-1,"Balanced Dark Swim upper hook must remain above its broad lower hood");
    check(bounds(swim,3*24,24)[4]>bounds(balanced,3*24,24)[4]+.5f,"Later Swim must develop a recognizably lower trailing hood tip");
    check(bounds(run,24,24)[1]<bounds(balanced,24,24)[1]-.8f&&bounds(run,72,24)[1]<bounds(balanced,72,24)[1]-1,"Later Run must lift both upper and lower rear hooks recognizably above balanced");
    check(width(power,2)>width(run,2)+.3f,"Later Power must broaden the lower hood relative to Run");
    check(outerWing(mesh,p,0,1)>outerWing(mesh,p,0,0)+.3f,"Later Fly must develop visibly broader back wings");
    check(bounds(fresh,0,48)[5]>3.5f&&bounds(fresh,48,48)[5]>3.5f,"Fresh Dark Swim must retain both already-visible rear extensions");
    check(maxPositionDelta(fresh,balanced)>.3f,"Biological maturity must develop the two-extension head beyond its common fresh form");
    for(float rp:new float[]{-1,1})for(float sf:new float[]{-1,1}){var mixed=crest(mesh,p,rp,sf);check(maxPositionDelta(mixed,crest(mesh,p,rp,0))>.15f&&maxPositionDelta(mixed,crest(mesh,p,0,sf))>.15f,"Compatible mixed Dark Swim head form collapsed into one pure influence");}
  }
  private static float maxPositionDelta(Capture a,Capture b){check(a.n==b.n,"Shape comparison changed topology");float difference=0;for(int i=0;i<a.n;i++)difference=Math.max(difference,Math.abs(a.p[i]-b.p[i]));return difference;}



  private static void temporal(){
    var m=asset(MODEL);var p=poses(m);var s=state(0,0,1);UUID id=new UUID(64,600060);
    int calendar=s.ageTicks,adultAge=s.adultAgeTicks;float biology=s.evolutionStrength();String lineage=s.lineageId();
    int duration=(int)DarkSwimAppearance068.CHANGE_TICKS;check(duration==30&&duration==DarkSwimAppearance068.CHANGE_TICKS,"The new Dark Swim features use the declared thirty-tick display interpolation");
    m.darkSwimDevelopment068(DarkSwimAppearance068.prepare(id,s,0));var original=capture(m,p);featureContact(m,p);Capture before=original;
    DriveRules.setTotal(s,ChaoState.RUN,960,new Random(64));DriveRules.setTotal(s,ChaoState.FLY,960,new Random(64));before=settleEdit(m,p,id,s,1,before);
    near(DarkSwimMorphology068.sample(s).runPower(),-1,0,"Run gain selects the current negative axis");near(DarkSwimMorphology068.sample(s).swimFly(),1,0,"Compatible Fly gain selects the other current axis");
    DriveRules.setTotal(s,ChaoState.POWER,1920,new Random(64));before=settleEdit(m,p,id,s,1+(duration+1),before);
    near(DarkSwimMorphology068.sample(s).runPower(),1,0,"Power retraining actually crosses back through balance into its own signed corner");
    DriveRules.setTotal(s,ChaoState.SWIM,1920,new Random(64));before=settleEdit(m,p,id,s,1+2*(duration+1),before);
    near(DarkSwimMorphology068.sample(s).swimFly(),-1,0,"Opposing Swim actually crosses balance into its own signed corner");
    for(int stat=0;stat<4;stat++)DriveRules.setTotal(s,stat,0,new Random(64));before=settleEdit(m,p,id,s,1+3*(duration+1),before);
    equal(original,before,.00004f,"Gain, both full opposite crossings and reduction failed to recover the balanced form");
    check(s.ageTicks==calendar&&s.adultAgeTicks==adultAge&&s.evolutionStrength()==biology&&s.lockedPrimaryType()==ChaoState.SWIM_TYPE&&s.lockedAlignment()==-1&&s.lineageId().equals(lineage),"Client specialization transitions fabricated biological youth, changed chronology or rewrote the first identity");
  }
  private static Capture settleEdit(ChaoMeshAsset mesh,Map<String,ChaoMeshAsset.Pose> poses,UUID id,ChaoState state,int tick,Capture previous){
    mesh.darkSwimDevelopment068(DarkSwimAppearance068.prepare(id,state,tick));equal(previous,capture(mesh,poses),.00004f,"Large actual bar edit snapped its first display frame");featureContact(mesh,poses);
    Capture before=previous;
    for(int t=tick+1;t<=tick+(int)DarkSwimAppearance068.CHANGE_TICKS;t++){
      mesh.darkSwimDevelopment068(DarkSwimAppearance068.prepare(id,state,t));var now=capture(mesh,poses);featureContact(mesh,poses);wings(mesh,poses,true);tailContact(mesh,poses);
      maxEdit=Math.max(maxEdit,step(before,now,1,"Configured actual edit interpolation jumped a model pixel"));before=now;frames++;
    }
    sample(mesh,state);equal(before,capture(mesh,poses),.00004f,"Client edit failed to reach the authoritative signed target within the declared display interval");return before;
  }

  private static void motion()throws Exception{
    var clips=JsonParser.parseString(Files.readString(ROOT.resolve("animations/official/chao.animation.json"))).getAsJsonObject().getAsJsonObject("animations");var rig=new OfficialChaoRig(clips,DarkSwimMorphRigTest068::asset);var expected=asset(MODEL);int serial=0;
    for(float age:new float[]{0,.5f,1})for(float sf:new float[]{-1,0,1})for(float rp:new float[]{-1,0,1})for(var mode:List.of(OfficialChaoRig.Motion.CRAWL,OfficialChaoRig.Motion.WALK,OfficialChaoRig.Motion.RUN,OfficialChaoRig.Motion.SIT,OfficialChaoRig.Motion.SWIM,OfficialChaoRig.Motion.GLIDE,OfficialChaoRig.Motion.HELD))for(int tick:new int[]{0,10,20}){
      var s=state(rp,sf,age);String saved=s.encode();String key=OfficialChaoRig.geometryKey(s);check(key.equals("dark_swim"),"Dark Swim adult logical identity changed");rig.prepareDevelopment(key,s);check(rig.sampledMesh().darkSwimPrototype068()&&!rig.sampledMesh().heroRunPrototype(),"Current Dark Swim adult must select real068 rather than historical055");rig.animateGameplay(key,new UUID(64,++serial),mode,tick,1,.05f,0,0,0,0,false);rig.development(s);sample(expected,s);check(rig.finite(),"Lionfish pose nonfinite");check(rig.sampledMesh().shapeSignature()==expected.shapeSignature(),"Production selection skipped DarkSwim068");check(rig.sourceGeometryKey().equals("baby"),"UV/attachment family changed");check(saved.equals(s.encode()),"Motion changed saved state");featureContact(rig.sampledMesh(),rig.poses());
      try{wings(rig.sampledMesh(),rig.poses(),false);}catch(AssertionError failure){var wingMatrix=new PoseStack();rig.sampledMesh().transformBone("wing_left",wingMatrix,rig.poses());var body=rig.poses().get("body");throw new AssertionError("Wing frame age="+age+" rp="+rp+" sf="+sf+" mode="+mode+" tick="+tick+" bodyScale="+body.sx+","+body.sy+","+body.sz+" leftWingMatrix="+Arrays.toString(wingMatrix.last().pose().get(new float[16]))+"; "+failure.getMessage(),failure);}
      tailContact(rig.sampledMesh(),rig.poses());motionFrames++;
      for(boolean left:new boolean[]{false,true}){float[] palm=rig.toyPalm(left);check(palm!=null&&palm.length==3,"Missing toy palm");for(float v:palm)check(Float.isFinite(v),"Toy palm nonfinite");var at=ChaoTriangleAnimation.palm(rig::transformBone,palm,left).mul(16);var emitted=select(rig.sampledMesh(),rig.poses(),left?"arm_left__cube_1":"arm_right__cube_1");check(emitted.count()==24,"Toy grip lost its actual compact hand mesh");near(at.distance(center(emitted,0)),0,.00004f,"Toy grip does not follow actual production-emitted palm");}
      for(String anchor:List.of("body","skull","arm_left","arm_right","foot_left","foot_right","tail","wing_left","wing_right")){var matrix=new PoseStack();rig.transformBone(anchor,matrix);check(matrix.last().pose().isFinite(),"Attachment matrix nonfinite");check(rig.attachmentBounds(anchor)!=null,"Missing attachment envelope");}
      if(tick==0)for(String family:List.of("overlays/chao_baby","cwe_parts/chao_baby","hats/chao_baby","market_accessories")){var donor=asset(family);var attached=donor.createPoses();for(var entry:rig.poses().entrySet())if(attached.containsKey(entry.getKey()))rig.sampledMesh().copyJointPoseTo(entry.getKey(),donor,entry.getValue(),attached.get(entry.getKey()));for(String anchor:List.of("body","skull","arm_left","arm_right","foot_left","foot_right","tail","wing_left","wing_right"))if(attached.containsKey(anchor)){var a=new PoseStack();var b=new PoseStack();rig.transformBone(anchor,a);donor.transformBone(anchor,b,attached);float[] av=a.last().pose().get(new float[16]),bv=b.last().pose().get(new float[16]);for(int i=0;i<16;i++)near(av[i],bv[i],.000008f,"Separate accessory/animal anchor detached");attachments++;}}
    }
  }
  /** Independent actual-emitter regression for the scoped water-wing path.
   * Use complete closed skull/wing
   * volumes, not projected bounds or selected texture faces. Keep every ray
   * rooted in the actual posed torso while checking both production APIs. */
  private static void wingHeadPose(OfficialChaoRig rig,String context){
    var mesh=rig.sampledMesh();var poses=rig.poses();
    var head=select(mesh,poses,"skull__cube_0");var torso=select(mesh,poses,"body__cube_0");
    var limbs=new HashMap<String,Capture>();for(String name:List.of("arm_left__cube_1","arm_right__cube_1","foot_left__cube_0","foot_right__cube_0"))limbs.put(name,select(mesh,poses,name));
    check(head.count()==24&&torso.count()==24,"Wing clearance changed fixed head/torso topology");
    var skull=poses.get("skull");near(skull.sx,1,.00001f,"Wing clearance resized the base head X");near(skull.sy,1,.00001f,"Wing clearance resized the base head Y");near(skull.sz,1,.00001f,"Wing clearance resized the base head Z");
    for(String side:List.of("left","right")){
      var wing=select(mesh,poses,"wing_"+side+"__cube_0");check(wing.count()==wingBoxesPerSide*24,"Wing clearance lost a closed ray");
      for(int ray=0;ray<wingBoxesPerSide;ray++){
        check(volume(wing,ray*24)>.00000005f,"Actual posed wing closed volume collapsed or inverted: "+context);
        check(!intersects(head,0,wing,ray*24),"Actual posed wing pierces unchanged head: "+context+" side="+side+" ray="+ray);wingHeadPairs++;
        check(intersects(torso,0,wing,ray*24),"Wing clearance detached a ray root from the actual torso: "+context+" side="+side+" ray="+ray);wingRootContacts++;
        for(var limb:limbs.entrySet()){check(!intersects(wing,ray*24,limb.getValue(),0),"Actual posed Dark Swim wing intersects compact palm/foot: "+context+" side="+side+" ray="+ray+" limb="+limb.getKey());wingLimbPairs++;}
      }
    }
    tailContact(mesh,poses);lowHoodPose(rig,context);
    check(rig.finite(),"Wing clearance produced a nonfinite production pose");wingHeadFrames++;
  }
  /** Unlike the higher Hero crowns, this source has a low trailing hood.
   * Inspect complete emitted feature volumes against the actual posed body,
   * both wings, palms, feet, both tail meshes and the Dark marker. Only skull/root and connected
   * head-feature overlaps are intentional; no blanket collision mask is used. */
  private static void lowHoodPose(OfficialChaoRig rig,String context){
    var mesh=rig.sampledMesh();var p=rig.poses();var features=select(mesh,p,"gradual_head_021");
    for(String bone:List.of("body__cube_0","wing_left__cube_0","wing_right__cube_0","arm_left__cube_1","arm_right__cube_1","foot_left__cube_0","foot_right__cube_0","tail__cube_0","tail__cube_1","emotion_dot__cube_0")){
      var other=select(mesh,p,bone);check(other.count()%24==0,"Low-hood clearance lost a complete closed comparison box");
      for(int feature=0;feature<featureBoxes;feature++)for(int box=0;box<other.count();box+=24){check(!intersects(features,feature*24,other,box),"Actual posed Dark Swim head feature intersects "+bone+": "+context+" feature="+feature+" box="+(box/24));lowHoodPairs++;}
    }
    lowHoodFrames++;
  }
  private static void wingAnimalAttachments(OfficialChaoRig rig){
    for(String family:List.of("overlays/chao_baby","cwe_parts/chao_baby")){
      var donor=asset(family);var attached=donor.createPoses();
      for(var entry:rig.poses().entrySet())if(attached.containsKey(entry.getKey()))rig.sampledMesh().copyJointPoseTo(entry.getKey(),donor,entry.getValue(),attached.get(entry.getKey()));
      for(String joint:List.of("wing_left","wing_right"))if(attached.containsKey(joint)){
        var actual=new PoseStack();var replacement=new PoseStack();rig.transformBone(joint,actual);donor.transformBone(joint,replacement,attached);
        float[] a=actual.last().pose().get(new float[16]),b=replacement.last().pose().get(new float[16]);for(int i=0;i<16;i++)near(a[i],b[i],.000008f,"Posed animal-wing attachment diverged from the shared production joint");wingAnimalMatrices++;
      }
    }
  }
  private static void wingHeadClearance()throws Exception{
    var clips=JsonParser.parseString(Files.readString(ROOT.resolve("animations/official/chao.animation.json"))).getAsJsonObject().getAsJsonObject("animations");
    var modes=List.of(OfficialChaoRig.Motion.SWIM,OfficialChaoRig.Motion.PADDLE,OfficialChaoRig.Motion.GLIDE,OfficialChaoRig.Motion.FLAIL,OfficialChaoRig.Motion.WALK,OfficialChaoRig.Motion.RUN,OfficialChaoRig.Motion.DASH,OfficialChaoRig.Motion.HELD,OfficialChaoRig.Motion.SIT,OfficialChaoRig.Motion.CRAWL,OfficialChaoRig.Motion.REST);
    int serial=0;
    for(float age:new float[]{0,.5f,1})for(float rp:new float[]{-1,0,1})for(float sf:new float[]{-1,0,1})for(var mode:modes)for(boolean deferred:new boolean[]{false,true})for(float crawlActivity:mode==OfficialChaoRig.Motion.CRAWL?new float[]{0,.08f}:new float[]{0}){
      var state=state(rp,sf,age);String saved=state.encode(),key=OfficialChaoRig.geometryKey(state);var id=new UUID(65,650074+serial++);var rig=new OfficialChaoRig(clips,DarkSwimMorphRigTest068::asset);rig.prepareDevelopment(key,state);
      for(int tick:new int[]{0,37,60,95,119,155,203,239})for(float yaw:new float[]{-37.25f,0,37.25f})for(float pitch:new float[]{-25,0,25}){
        float activity=mode==OfficialChaoRig.Motion.WALK||mode==OfficialChaoRig.Motion.RUN||mode==OfficialChaoRig.Motion.DASH?.05f:crawlActivity;float gait=mode==OfficialChaoRig.Motion.CRAWL&&crawlActivity>0?.035f*tick:1;
        rig.animateGameplay(key,id,mode,tick,gait,activity,0,0,yaw,pitch,false,!deferred);rig.development(state);
        if(deferred)rig.finishGameplayFrame(id,0,tick);
        wingHeadPose(rig,"age="+age+" rp="+rp+" sf="+sf+" mode="+mode+" tick="+tick+" yaw="+yaw+" pitch="+pitch+" activity="+activity+" deferred="+deferred);
        if(tick==119&&yaw==0&&pitch==0)wingAnimalAttachments(rig);
      }
      check(saved.equals(state.encode()),"Wing pose clearance changed saved training, age, alignment or first type");
    }
    // Actual renderer ordering, half-tick samples and the same entity across
    // entry/exit. No test-only remember call or alternate pose bypasses the
    // real finishGameplayFrame transition path used by ChaoModel.
    var sequence=List.of(OfficialChaoRig.Motion.WALK,OfficialChaoRig.Motion.SWIM,OfficialChaoRig.Motion.PADDLE,OfficialChaoRig.Motion.GLIDE,OfficialChaoRig.Motion.HELD,OfficialChaoRig.Motion.RUN,OfficialChaoRig.Motion.REST,OfficialChaoRig.Motion.SIT,OfficialChaoRig.Motion.WALK,OfficialChaoRig.Motion.CRAWL,OfficialChaoRig.Motion.DASH,OfficialChaoRig.Motion.CRAWL,OfficialChaoRig.Motion.PADDLE,OfficialChaoRig.Motion.CRAWL,OfficialChaoRig.Motion.FLAIL,OfficialChaoRig.Motion.CRAWL,OfficialChaoRig.Motion.SWIM,OfficialChaoRig.Motion.CRAWL,OfficialChaoRig.Motion.GLIDE,OfficialChaoRig.Motion.CRAWL,OfficialChaoRig.Motion.HELD,OfficialChaoRig.Motion.CRAWL,OfficialChaoRig.Motion.REST,OfficialChaoRig.Motion.CRAWL,OfficialChaoRig.Motion.SIT,OfficialChaoRig.Motion.CRAWL,OfficialChaoRig.Motion.RUN,OfficialChaoRig.Motion.CRAWL,OfficialChaoRig.Motion.WALK);
    for(float age:new float[]{0,.5f,1})for(float rp:new float[]{-1,0,1})for(float sf:new float[]{-1,0,1})for(float[] look:new float[][]{{0,0},{-37.25f,-25},{0,-25},{37.25f,-25}}){
      var state=state(rp,sf,age);String key=OfficialChaoRig.geometryKey(state);var id=new UUID(65,660000+serial++);var rig=new OfficialChaoRig(clips,DarkSwimMorphRigTest068::asset);rig.prepareDevelopment(key,state);Capture left=null,right=null,tail=null;
      for(int stage=0;stage<sequence.size();stage++)for(int half=0;half<=40;half++){
        float tick=600+stage*20+half*.5f;var mode=sequence.get(stage);
        rig.animateGameplay(key,id,mode,tick,mode==OfficialChaoRig.Motion.CRAWL||mode==OfficialChaoRig.Motion.WALK||mode==OfficialChaoRig.Motion.RUN||mode==OfficialChaoRig.Motion.DASH?.035f*tick:1,mode==OfficialChaoRig.Motion.WALK||mode==OfficialChaoRig.Motion.RUN||mode==OfficialChaoRig.Motion.DASH?.05f:mode==OfficialChaoRig.Motion.CRAWL?.08f:0,0,0,look[0],look[1],false,false);rig.development(state);rig.finishGameplayFrame(id,0,tick);
        wingHeadPose(rig,"transition age="+age+" rp="+rp+" sf="+sf+" stage="+stage+" tick="+tick+" yaw="+look[0]+" pitch="+look[1]);
        var nowLeft=select(rig.sampledMesh(),rig.poses(),"wing_left__cube_0");var nowRight=select(rig.sampledMesh(),rig.poses(),"wing_right__cube_0");
        maxWingPoseStep=Math.max(maxWingPoseStep,step(left,nowLeft,.8f,"Actual half-tick wing entry/exit popped"));maxWingPoseStep=Math.max(maxWingPoseStep,step(right,nowRight,.8f,"Actual half-tick wing entry/exit popped"));left=nowLeft;right=nowRight;wingTransitionFrames++;
        var nowTail=select(rig.sampledMesh(),rig.poses(),"tail__cube_0");
        if(half==0&&tail!=null)equal(tail,nowTail,.00004f,"Actual eight-tick motion transition snapped the tail on its first same-clock frame");
        maxTailPoseStep=Math.max(maxTailPoseStep,step(tail,nowTail,.8f,"Actual half-tick tail entry/exit popped"));tail=nowTail;tailTransitionFrames++;
      }
    }
  }

  /** Interpolated box angles can have extrema between the27 endpoint/age
   * fixtures. Deterministic current-stat interiors exercise actual production
   * motion, gaze, tail contacts and whole-head/hood clearances. */
  private static void posedInteriors()throws Exception{
    var clips=JsonParser.parseString(Files.readString(ROOT.resolve("animations/official/chao.animation.json"))).getAsJsonObject().getAsJsonObject("animations");
    var clocks=OfficialChaoRig.class.getDeclaredField("gaitClocks");clocks.setAccessible(true);var stridePhase=com.chaogarden.core.ChaoGaitClock.class.getDeclaredField("phase");stridePhase.setAccessible(true);
    var random=new Random(680068);var modes=List.of(OfficialChaoRig.Motion.SWIM,OfficialChaoRig.Motion.PADDLE,OfficialChaoRig.Motion.GLIDE,OfficialChaoRig.Motion.FLAIL,OfficialChaoRig.Motion.WALK,OfficialChaoRig.Motion.RUN,OfficialChaoRig.Motion.DASH,OfficialChaoRig.Motion.HELD,OfficialChaoRig.Motion.SIT,OfficialChaoRig.Motion.CRAWL,OfficialChaoRig.Motion.REST);
    for(int i=0;i<1000;i++){
      float age=random.nextFloat(),rp=random.nextFloat()*2-1,sf=random.nextFloat()*2-1,yaw=random.nextFloat()*74.5f-37.25f,pitch=random.nextFloat()*50-25,tick=random.nextFloat()*240;var mode=modes.get(random.nextInt(modes.size()));boolean deferred=random.nextBoolean();
      var state=state(rp,sf,age);String saved=state.encode(),key=OfficialChaoRig.geometryKey(state);var id=new UUID(68,680068+i);var rig=new OfficialChaoRig(clips,DarkSwimMorphRigTest068::asset);rig.prepareDevelopment(key,state);
      if(mode==OfficialChaoRig.Motion.WALK||mode==OfficialChaoRig.Motion.RUN||mode==OfficialChaoRig.Motion.DASH){rig.animateGameplay(key,id,mode,tick,.035f*tick,.05f,0,0,yaw,pitch,false,false);var clock=((Map<?,?>)clocks.get(rig)).get(id);check(clock!=null,"Interior upright gait clock missing");stridePhase.setDouble(clock,random.nextDouble());}
      rig.animateGameplay(key,id,mode,tick,mode==OfficialChaoRig.Motion.CRAWL||mode==OfficialChaoRig.Motion.WALK||mode==OfficialChaoRig.Motion.RUN||mode==OfficialChaoRig.Motion.DASH?.035f*tick:1,mode==OfficialChaoRig.Motion.WALK||mode==OfficialChaoRig.Motion.RUN||mode==OfficialChaoRig.Motion.DASH?.05f:mode==OfficialChaoRig.Motion.CRAWL&&random.nextBoolean()?.08f:0,0,0,yaw,pitch,false,!deferred);rig.development(state);if(deferred)rig.finishGameplayFrame(id,0,tick);
      wingHeadPose(rig,"interior="+i+" age="+age+" rp="+rp+" sf="+sf+" mode="+mode+" tick="+tick+" yaw="+yaw+" pitch="+pitch+" deferred="+deferred);
      featureContact(rig.sampledMesh(),rig.poses());check(saved.equals(state.encode()),"Interior pose sampling changed authoritative state");posedInteriorFrames++;
    }
  }

  /** The operator's actual crawl uses motionAmount.08 and a varying source
   * stride. Cover its complete cycle separately from still-crawl snapshots,
   * including all growth/grid corners and breathing extrema. */
  private static void crawlStrideClearance()throws Exception{
    var clips=JsonParser.parseString(Files.readString(ROOT.resolve("animations/official/chao.animation.json"))).getAsJsonObject().getAsJsonObject("animations");int serial=0;
    for(float age:new float[]{0,.5f,1})for(float rp:new float[]{-1,0,1})for(float sf:new float[]{-1,0,1}){
      var state=state(rp,sf,age);String saved=state.encode(),key=OfficialChaoRig.geometryKey(state);var id=new UUID(68,682000+serial++);var rig=new OfficialChaoRig(clips,DarkSwimMorphRigTest068::asset);rig.prepareDevelopment(key,state);
      for(int breathing:new int[]{-1,0,1}){
        long seed=id.getMostSignificantBits()^id.getLeastSignificantBits();double wanted=breathing<0?Math.PI*1.5:breathing>0?Math.PI*.5:0;float tick=(float)((wanted-(seed&1023)*.01+Math.PI*8)/.055);
        for(int stride=0;stride<=16;stride++)for(float[] gaze:new float[][]{{-37.25f,-25},{0,0},{37.25f,25}}){
          float gait=(float)(Math.PI*2*stride/16/7.5);rig.animateGameplay(key,id,OfficialChaoRig.Motion.CRAWL,tick,gait,.08f,0,0,gaze[0],gaze[1],false,false);rig.development(state);rig.finishGameplayFrame(id,0,tick);
          wingHeadPose(rig,"operator crawl age="+age+" rp="+rp+" sf="+sf+" breath="+breathing+" stride="+stride+" yaw="+gaze[0]+" pitch="+gaze[1]);crawlPoseFrames++;
        }
      }
      check(saved.equals(state.encode()),"Complete crawl stride sampling changed saved state");
    }
  }

  /** Initial motion clocks do not cover a moving RUN/DASH stride. Sample
   * actual source gait phases, without an outgoing CRAWL transition, so a
   * static first frame cannot conceal a wing/skull/palm/foot collision. */
  private static void uprightStrideClearance()throws Exception{
    var clips=JsonParser.parseString(Files.readString(ROOT.resolve("animations/official/chao.animation.json"))).getAsJsonObject().getAsJsonObject("animations");
    var clocks=OfficialChaoRig.class.getDeclaredField("gaitClocks");clocks.setAccessible(true);var phase=com.chaogarden.core.ChaoGaitClock.class.getDeclaredField("phase");phase.setAccessible(true);int serial=0;
    for(float age:new float[]{0,.5f,1})for(float rp:new float[]{-1,0,1})for(float sf:new float[]{-1,0,1})for(var mode:List.of(OfficialChaoRig.Motion.RUN,OfficialChaoRig.Motion.DASH)){
      var state=state(rp,sf,age);String saved=state.encode(),key=OfficialChaoRig.geometryKey(state);var id=new UUID(68,683000+serial++);var rig=new OfficialChaoRig(clips,DarkSwimMorphRigTest068::asset);rig.prepareDevelopment(key,state);
      for(int breathing:new int[]{-1,0,1}){
        long seed=id.getMostSignificantBits()^id.getLeastSignificantBits();double wanted=breathing<0?Math.PI*1.5:breathing>0?Math.PI*.5:0;float tick=(float)((wanted-(seed&1023)*.01+Math.PI*8)/.055);
        rig.animateGameplay(key,id,mode,tick,1,.05f,0,0,0,0,false,false);Object clock=((Map<?,?>)clocks.get(rig)).get(id);check(clock!=null,"Actual upright source gait clock was not initialized");
        for(int stride=0;stride<=16;stride++)for(float yaw:new float[]{-37.25f,0,37.25f})for(float pitch:new float[]{-25,0,25}){
          phase.setDouble(clock,stride/16.0);rig.animateGameplay(key,id,mode,tick,1,.05f,0,0,yaw,pitch,false,false);rig.development(state);rig.finishGameplayFrame(id,0,tick);
          wingHeadPose(rig,"upright stride age="+age+" rp="+rp+" sf="+sf+" mode="+mode+" breath="+breathing+" stride="+stride+" yaw="+yaw+" pitch="+pitch);uprightStrideFrames++;
        }
      }
      check(saved.equals(state.encode()),"Upright stride sampling changed saved state");
    }
  }

  /** The new mesh-only tail channel must never replay into another family
   * when one entity UUID changes saved identity or visual scope. Compare in
   * each actual semantic tail joint frame, so ordinary inherited body/joint
   * transition blending remains independent of the new mesh-only channel. */
  private static void tailScopeTransitions()throws Exception{
    var clips=JsonParser.parseString(Files.readString(ROOT.resolve("animations/official/chao.animation.json"))).getAsJsonObject().getAsJsonObject("animations");int serial=0;
    for(int fixture=0;fixture<4;fixture++)for(var mode:List.of(OfficialChaoRig.Motion.SWIM,OfficialChaoRig.Motion.WALK)){
      var id=new UUID(68,681000+serial++);var rig=new OfficialChaoRig(clips,DarkSwimMorphRigTest068::asset);var fresh=state(0,-1,1);String first=OfficialChaoRig.geometryKey(fresh);rig.prepareDevelopment(first,fresh);
      rig.animateGameplay(first,id,OfficialChaoRig.Motion.SWIM,480,1,0,0,0,0,0,false,false);rig.development(fresh);rig.finishGameplayFrame(id,0,480);
      var target=new ChaoState();target.adult=true;target.primaryType=fixture==0?ChaoState.NORMAL:fixture==1?ChaoState.POWER_TYPE:ChaoState.SWIM_TYPE;target.align=fixture==1?1:-1;target.setEvolutionStrength(1.2f);
      // A supported future theme ID takes the unchanged ordinary visual fallback
      // without requiring an initialized external-mod loader in this CPU harness.
      if(fixture==2)target.variant="future_tail_scope_probe";if(fixture==3)target.character="tails";target=ChaoState.decode(target.encode());String saved=target.encode(),key=OfficialChaoRig.geometryKey(target);check(!DarkSwimMorphology068.sample(target).enabled(),"Tail scope fixture unexpectedly admits068");rig.prepareDevelopment(key,target);
      var reference=new OfficialChaoRig(clips,DarkSwimMorphRigTest068::asset);reference.prepareDevelopment(key,target);
      for(int half=0;half<=16;half++){
        float tick=480+half*.5f;float activity=mode==OfficialChaoRig.Motion.WALK?.05f:0;
        rig.animateGameplay(key,id,mode,tick,1,activity,0,0,0,0,false,false);rig.development(target);rig.finishGameplayFrame(id,0,tick);
        reference.animateGameplay(key,id,mode,tick,1,activity,0,0,0,0,false,false);reference.development(target);reference.finishGameplayFrame(id,0,tick);
        check(!rig.sampledMesh().darkSwimPrototype068(),"An excluded or previously completed family retained the new068 mesh");
        var tailMeshes=fixture==3?List.of("tail_left__cube_0","tail_left__cube_1","tail_left__cube_2","tail_right__cube_0","tail_right__cube_1","tail_right__cube_2"):List.of("tail__cube_0");
        for(String name:tailMeshes){String joint=name.substring(0,name.indexOf("__"));var actual=inJointFrame(rig.sampledMesh(),rig.poses(),joint,select(rig.sampledMesh(),rig.poses(),name));var old=inJointFrame(reference.sampledMesh(),reference.poses(),joint,select(reference.sampledMesh(),reference.poses(),name));
          equal(old,actual,.00005f,"New068 mesh-only tail pose leaked into another family's ordinary transition: fixture="+fixture+" mode="+mode+" half="+half+" mesh="+name);
        }
        check(saved.equals(target.encode()),"Tail scope transition changed another family's saved state");tailScopeFrames++;
      }
    }
  }

  /** Exercise the actual quiet-book pose functions at the observed phase36
   * and representative entry/exit phases. No alternate demonstration pose is
   * authored here; this is the same production PLAY rig and quiet-toy solver. */
  private static void bookTipSurfaces()throws Exception{
    var clips=JsonParser.parseString(Files.readString(ROOT.resolve("animations/official/chao.animation.json"))).getAsJsonObject().getAsJsonObject("animations");
    var quiet=new LionfishQuietToyClip049();
    for(float[] profile:new float[][]{{1,1},{0,-1},{1,-1}})for(int tick:new int[]{12,36,72,132,156}){
      var s=state(profile[0],profile[1],1);var rig=new OfficialChaoRig(clips,DarkSwimMorphRigTest068::asset);String key=OfficialChaoRig.geometryKey(s);
      rig.prepareDevelopment(key,s);rig.animateGameplay(key,new UUID(64,600060+tick),OfficialChaoRig.Motion.PLAY,tick,0,0,0,0,0,0,false);rig.development(s);
      float weight=com.chaogarden.toys.QuietToyPlayRules049.presence(tick);
      ChaoQuietToyAnimation049.pose(rig.poses(),rig::transformBone,rig.toyPalm(false),rig.toyPalm(true),
          com.chaogarden.toys.QuietToyPlayRules049.BOOK,weight,quiet.sweep(tick),quiet.page(tick),quiet.stroke(tick),quiet.pen(tick),tick,.8f,rig::attachmentBounds);
      check(rig.finite(),"Production quiet-book pose is not finite");featureContact(rig.sampledMesh(),rig.poses());tailContact(rig.sampledMesh(),rig.poses());wingHeadPose(rig,"quiet book rp="+profile[0]+" sf="+profile[1]+" phase="+tick);bookSurfaceFrames++;
    }
  }
  private static void stableCache()throws Exception{
    var clips=JsonParser.parseString(Files.readString(ROOT.resolve("animations/official/chao.animation.json"))).getAsJsonObject().getAsJsonObject("animations");var revision=ChaoMeshAsset.class.getDeclaredField("pathRevision056");revision.setAccessible(true);
    var s=state(-1,1,.5f);var rig=new OfficialChaoRig(clips,DarkSwimMorphRigTest068::asset);rig.prepareDevelopment(OfficialChaoRig.geometryKey(s),s);var mesh=rig.sampledMesh();check(mesh.darkSwimPrototype068(),"Cache fixture selected wrong profile");
    int before=revision.getInt(mesh);long signature=mesh.shapeSignature();String saved=s.encode();
    for(int frame=0;frame<100;frame++){rig.development(s);check(revision.getInt(mesh)==before,"Unchanged state invalidated shape cache");check(mesh.shapeSignature()==signature,"Stable state changed emitted geometry");}check(saved.equals(s.encode()),"Repeated rendering changed saved state");
    DriveRules.setTotal(s,ChaoState.POWER,480,new Random(64));rig.development(s);check(revision.getInt(mesh)==before+1&&mesh.shapeSignature()!=signature,"Genuine training change did not invalidate the shape exactly once");int changed=revision.getInt(mesh);rig.development(s);check(revision.getInt(mesh)==changed,"New stable training kept invalidating cache");
    s=state(1,1,1);rig.prepareDevelopment(OfficialChaoRig.geometryKey(s),s);rig.animateGameplay(OfficialChaoRig.geometryKey(s),new UUID(64,600001),OfficialChaoRig.Motion.WALK,10,1,.05f,0,0,0,0,false);rig.development(s);
    long trained=rig.shapeSignature();float chronologicalGrowth=s.evolutionStrength();DriveRules.setTotal(s,ChaoState.POWER,0,new Random(64));rig.development(s);
    check(rig.shapeSignature()!=trained&&s.evolutionStrength()==chronologicalGrowth,"Actual Power loss changes the sampled form without changing biological growth");
    var guard=OfficialChaoRig.class.getDeclaredField("broadWalkHead");guard.setAccessible(true);check(guard.getBoolean(rig),"Losing Power must retain the full-size fixed-head walking guard");
  }
  /** The unchanged full-size skull needs its baseline walking clearance at
   * every growth/training corner. Use a test-only disabled-guard control to
   * measure the real production walking pose, independently of Power arms. */
  private static void baselineHeadGuard()throws Exception{
    var clips=JsonParser.parseString(Files.readString(ROOT.resolve("animations/official/chao.animation.json"))).getAsJsonObject().getAsJsonObject("animations");
    var guard=OfficialChaoRig.class.getDeclaredField("broadWalkHead");guard.setAccessible(true);var power=OfficialChaoRig.class.getDeclaredField("powerSwimWeight");power.setAccessible(true);
    for(float age:new float[]{0,.5f,1})for(float sf:new float[]{-1,0,1})for(float rp:new float[]{-1,0,1}){
      var state=state(rp,sf,age);String saved=state.encode(),key=OfficialChaoRig.geometryKey(state);var id=new UUID(64,600060);
      var actual=new OfficialChaoRig(clips,DarkSwimMorphRigTest068::asset);actual.prepareDevelopment(key,state);
      check(guard.getBoolean(actual),"Every Dark Swim-first corner retains baseline full-head walking clearance");
      actual.animateGameplay(key,id,OfficialChaoRig.Motion.WALK,300,11,.05f,0,0,0,0,false,false);actual.development(state);
      var control=new OfficialChaoRig(clips,DarkSwimMorphRigTest068::asset);control.prepareDevelopment(key,state);guard.setBoolean(control,false);power.setFloat(control,0);
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
    for(int target=0;target<27;target++){
      float rp=target%3-1,sf=(target%9)/3-1,age=(target/9)*.5f;var state=state(rp,sf,age);String saved=state.encode(),key=OfficialChaoRig.geometryKey(state);var id=new UUID(64,600100+target);
      var rig=new OfficialChaoRig(clips,DarkSwimMorphRigTest068::asset);rig.prepareDevelopment(key,state);check(guard.getBoolean(rig),"Fixed head needs its guard at every body-growth corner");
      for(int breathing:new int[]{-1,0,1}){
        long seed=id.getMostSignificantBits()^id.getLeastSignificantBits();double wanted=breathing<0?Math.PI*1.5:breathing>0?Math.PI*.5:0;float at=(float)((wanted-(seed&1023)*.01+Math.PI*8)/.055);
        rig.animateGameplay(key,id,OfficialChaoRig.Motion.WALK,at,11,.05f,0,0,0,0,false,false);
        Object clock=((Map<?,?>)clocks.get(rig)).get(id);check(clock!=null,"Actual stride clock was not initialized");
        for(int i=0;i<=1000;i++){
          phase.setDouble(clock,i/1000.0);rig.animateGameplay(key,id,OfficialChaoRig.Motion.WALK,at,11,.05f,0,0,0,0,false,false);rig.development(state);headTorsoConnected(rig,target,i,breathing,0,0,i%125==0);
        }
        for(int step=0;step<=8;step++)for(float yaw:new float[]{-37.25f,-12,0,12,37.25f})for(float pitch:new float[]{-25,0,25}){
          phase.setDouble(clock,step/8.0);rig.animateGameplay(key,id,OfficialChaoRig.Motion.WALK,at,11,.05f,0,0,yaw,pitch,false,false);rig.development(state);headTorsoConnected(rig,target,step,breathing,yaw,pitch,true);
        }
      }
      check(saved.equals(state.encode()),"Dense walking clearance changed saved age/training/identity");
    }

  }
  private static void headTorsoConnected(OfficialChaoRig rig,int target,int phase,int breathing,float yaw,float pitch,boolean examineFeatures){
    var head=select(rig.sampledMesh(),rig.poses(),"skull__cube_0");var body=select(rig.sampledMesh(),rig.poses(),"body__cube_0");check(head.count()==24&&body.count()==24,"Head/torso box topology changed");
    check(intersects(head,0,body,0),"Actual WALK disconnected whole fixed skull from torso: target="+target+" phase="+phase+" breath="+breathing+" yaw="+yaw+" pitch="+pitch);check(rig.finite(),"Walking head/torso continuity used nonfinite poses");headTorsoFrames++;
    for(String hand:List.of("arm_left__cube_1","arm_right__cube_1")){
      var palm=select(rig.sampledMesh(),rig.poses(),hand);check(palm.count()==24,"Compact palm lost its original single cuboid topology");
      var upper=select(rig.sampledMesh(),rig.poses(),hand.replace("__cube_1","__cube_0"));
      float shoulderOverlap=overlapDepth(body,upper),wristOverlap=overlapDepth(upper,palm);
      check(shoulderOverlap>=-.0001f,"Production WALK detached upper arm from torso: target="+target+" phase="+phase+" breath="+breathing+" yaw="+yaw+" pitch="+pitch+" hand="+hand+" signed SAT overlap="+shoulderOverlap);
      check(wristOverlap>=-.0001f,"Production WALK detached compact hand from its upper arm: target="+target+" phase="+phase+" hand="+hand+" signed SAT overlap="+wristOverlap);shoulderFrames++;
      check(!intersects(head,0,palm,0),"Production WALK palm intersects unchanged skull: target="+target+" phase="+phase+" breath="+breathing+" yaw="+yaw+" pitch="+pitch+" hand="+hand);palmHeadFrames++;
      // Every full-gaze phase tests all six growing features, including
      // the two axial root/tip extensions and emerging lateral spurs. The dense straight-gaze stride
      // retains its existing nine feature-contact checkpoints separately.
      if(examineFeatures){
        var features=select(rig.sampledMesh(),rig.poses(),"gradual_head_021");
        for(int feature=0;feature<featureBoxes;feature++){
          check(!intersects(features,feature*24,palm,0),"Production WALK palm intersects growing Dark Swim head extension: target="+target+" phase="+phase+" breath="+breathing+" yaw="+yaw+" pitch="+pitch+" feature="+feature+" hand="+hand);featurePalmPairs++;
        }
      }
    }
    if(examineFeatures)wingHeadPose(rig,"dense WALK target="+target+" phase="+phase+" breath="+breathing+" yaw="+yaw+" pitch="+pitch);
  }
  /** Keep current production visibility while isolating rendered groups.
   * Unlike select(), this helper never makes a hidden symbol visible. */
  private static Capture visibleGroup(ChaoMeshAsset mesh,Map<String,ChaoMeshAsset.Pose> poses,java.util.Set<String> selected){
    var before=new HashMap<String,Boolean>();poses.forEach((name,pose)->before.put(name,pose.visible));
    try{poses.forEach((name,pose)->{if(!selected.contains(name))pose.visible=false;});return capture(mesh,poses);}
    finally{poses.forEach((name,pose)->pose.visible=before.get(name));}
  }
  /** Actual emitter regression, not an in-game expression demonstration. The
   * same rig APIs called by ChaoModel select reaction clips, marker visibility
   * and expression materials; saved emotion values alone do not drive these. */
  private static void expressionVisibility()throws Exception{
    var clips=JsonParser.parseString(Files.readString(ROOT.resolve("animations/official/chao.animation.json"))).getAsJsonObject().getAsJsonObject("animations");
    var markers=new HashMap<String,java.util.Set<String>>();
    for(String symbol:List.of("dot","heart","exclamation","question","dizzy")){
      var names=new HashSet<String>();for(var element:data(MODEL).getAsJsonArray("bones")){String name=element.getAsJsonObject().get("name").getAsString();if(name.startsWith("emotion_"+symbol+"__"))names.add(name);}markers.put(symbol,names);
    }
    var face=new HashSet<String>(List.of("skull__cube_0","skull__cube_2","skull__cube_3"));var mouths=new HashSet<String>();for(int i=1;i<=6;i++){face.add("expression_mouth_"+i);mouths.add("expression_mouth_"+i);}
    int serial=0;
    for(float[] profile:new float[][]{{0,0,0},{-1,1,.5f},{1,1,1}})for(int emotion:new int[]{0,1,2,3}){
      var state=state(profile[0],profile[1],profile[2]);String saved=state.encode(),key=OfficialChaoRig.geometryKey(state);var rig=new OfficialChaoRig(clips,DarkSwimMorphRigTest068::asset);
      rig.prepareDevelopment(key,state);rig.animateGameplay(key,new UUID(64,601200+serial++),OfficialChaoRig.Motion.WALK,120,0,0,emotion,.6f,12,-10,false,false);rig.development(state);
      int expression=emotion==1?2:emotion==2?3:emotion==3?1:0;rig.expression(expression);rig.sampledMesh().lens(0xff101820);
      int dot=visibleGroup(rig.sampledMesh(),rig.poses(),markers.get("dot")).count();
      check(dot==(emotion==0?7*24:0),"All seven Dark marker boxes must hide with their dot parent during visible reactions");
      check(visibleGroup(rig.sampledMesh(),rig.poses(),markers.get("heart")).count()==(emotion==3?3*24:0),"Happy reaction must emit only the original heart symbol");
      check(visibleGroup(rig.sampledMesh(),rig.poses(),markers.get("exclamation")).count()==(emotion==2?2*24:0),"Alert reaction must emit only the original exclamation symbol");
      check(visibleGroup(rig.sampledMesh(),rig.poses(),markers.get("question")).count()==0&&visibleGroup(rig.sampledMesh(),rig.poses(),markers.get("dizzy")).count()==0,"Unrequested question/dizzy symbols leaked through the reaction hierarchy");
      int expectedMouth=expression==0?0:expression==3?24:72;
      check(visibleGroup(rig.sampledMesh(),rig.poses(),mouths).count()==expectedMouth,"Reaction material did not select the original expression-mouth meshes");
      var original=asset("gameplay/chao_baby");original.development(0,0,0);var originalPoses=original.createPoses();for(var entry:rig.poses().entrySet())if(originalPoses.containsKey(entry.getKey()))rig.sampledMesh().copyJointPoseTo(entry.getKey(),original,entry.getValue(),originalPoses.get(entry.getKey()));original.expression(expression);
      equal(visibleGroup(original,originalPoses,face),visibleGroup(rig.sampledMesh(),rig.poses(),face),.00004f,"Original head, eyes or expression geometry/UV changed in a real reaction pose");
      rig.eyesVisible(false);check(visibleGroup(rig.sampledMesh(),rig.poses(),java.util.Set.of("skull__cube_2","skull__cube_3")).count()==0,"Hiding eyes for a real blink/closed-eye expression left duplicate original eyes visible");
      check(saved.equals(state.encode()),"Rendering a reaction changed saved Chao data");expressionFrames++;
    }
  }
  private static void shapeDna(){
    var mesh=asset(MODEL);var poses=poses(mesh);
    for(float age:new float[]{0,.5f,1})for(float sf:new float[]{-1,0,1})for(float rp:new float[]{-1,0,1}){
      var state=state(rp,sf,age);sample(mesh,state);var reference=capture(mesh,poses);
      for(int gene=0;gene<ChaoState.ANIMALS.length;gene++){
        state.animalMask=1<<gene;state.colorGenes=new int[]{gene%ChaoState.COLORS.length,(gene+1)%ChaoState.COLORS.length};state.toneGenes=new int[]{gene%2,(gene+1)%2};state.shinyGenes=new int[]{1,0};state.jewelGenes=new int[]{gene%ChaoState.JEWELS.length,(gene+1)%ChaoState.JEWELS.length};state.setTrait("darkswim68.foreign.dna","retained "+gene);
        String saved=state.encode();sample(mesh,state);equal(reference,capture(mesh,poses),0,"Genetics or animal DNA rewrote the ordinary Dark Swim-first base shape/UV");check(saved.equals(state.encode()),"Rendering changed earned DNA, genetics or foreign traits");
      }
    }
  }
  private static void coats(){
    var m=asset(MODEL);var p=poses(m);
    for(float rp:new float[]{-1,0,1})for(float sf:new float[]{-1,0,1}){var s=state(rp,sf,1);sample(m,s);var reference=capture(m,p);for(int color=0;color<ChaoState.COLORS.length;color++)for(int jewel=0;jewel<ChaoState.JEWELS.length;jewel++)if(color==0||jewel==0)for(boolean shiny:new boolean[]{false,true})for(boolean twoTone:new boolean[]{false,true}){s.expressedColor=color;s.expressedJewel=jewel;s.shiny=shiny;s.twoTone=twoTone;sample(m,s);equal(reference,capture(m,p),0,"Color/shine changed form/UV");coatCases++;}}
  }
  public static void main(String[] args)throws Exception{
    if("true".equalsIgnoreCase(System.getenv("CHAO_DARKSWIM068_WALK_DIAGNOSTIC"))){topology();diagnoseWalk();return;}
    if("true".equalsIgnoreCase(System.getenv("CHAO_DARKSWIM068_WING_HEAD_DIAGNOSTIC"))){topology();wingHeadClearance();return;}
    long start=System.nanoTime();protectedAssets();topology();retainedBabyRoute();development();silhouettes();temporal();motion();wingHeadClearance();posedInteriors();crawlStrideClearance();uprightStrideClearance();tailScopeTransitions();bookTipSurfaces();stableCache();baselineHeadGuard();headTorsoContinuity();expressionVisibility();shapeDna();coats();String report="DarkSwim068: "+checks+" checks; "+babyFrames+" preserved currentbaby055 frames; "+babyNubChecks+" separate front-side baby nub samples; "+frames+" emitted age/axis/edit frames; "+solids+" closed rectangular-box samples; "+motionFrames+" production Lionfish motion frames; "+attachments+" separate attachment matrices; "+coatCases+" coat/shape cases; "+markerPairs+" actual spiked-Dark-marker/head/feature separation checks; "+connectedFeatures+" connected feature samples; "+wingSamples+" emitted closed-wing samples; "+tailSamples+" new root/tip and retained tail-base continuity checks; "+wingUvSamples+" actual wing-span UV samples; "+hingeSamples+" original-edge hinge samples; "+bookSurfaceFrames+" production quiet-book surface frames; "+baselineGuardFrames+" actual baseline walking-head guard comparisons; "+surfacePairs+" actual same-facing head/feature plane-pair checks; "+headTorsoFrames+" actual WALK head/torso SAT continuity frames; "+palmHeadFrames+" actual WALK palm/head SAT separation frames; "+featurePalmPairs+" actual WALK growing-feature/palm SAT separation pairs; "+headCapUvSamples+" actual head extension root/proximal/distal surface UV samples; "+shoulderFrames+" actual WALK shoulder/wrist continuity frames; "+expressionFrames+" production-emitter expression/marker visibility frames; "+wingHeadFrames+" actual wing/head clearance frames; "+wingHeadPairs+" complete wing/head SAT pairs; "+wingRootContacts+" actual wing-ray/torso root contacts; "+wingLimbPairs+" actual wing/palm/foot separation pairs; "+wingAnimalMatrices+" animal-wing joint matrices; "+wingTransitionFrames+" actual half-tick wing transition frames; "+tailTransitionFrames+" actual half-tick tail transition frames; "+posedInteriorFrames+" deterministic signed-interior production pose frames; "+tailScopeFrames+" same-UUID prior-family tail scope transition frames; "+crawlPoseFrames+" actual operator-crawl stride/gaze/breathing frames; "+uprightStrideFrames+" actual upright RUN/DASH stride/gaze/breathing frames; "+lowHoodFrames+" complete low-hood pose frames; "+lowHoodPairs+" head-feature/body/wing/palm/foot/tail SAT pairs; maximum half-tick wing step="+maxWingPoseStep+" pixels; maximum half-tick tail step="+maxTailPoseStep+" pixels; maximum hinge drift="+maxHinge+" model pixels; maximum axis step="+maxStep+" model pixels; maximum"+(int)DarkSwimAppearance068.CHANGE_TICKS+"tick edit step="+maxEdit+"; maximum1pct maturity step="+maxMaturity+"; seconds="+(System.nanoTime()-start)/1e9+". Fixed original head/face/59-joint rig with its source-appropriate Dark marker; nine distinct mature corners; reversible independent age/axes; canonical baby UV/attachment alias and current production selection. Geometry validation, not visual/multiplayer verification.\n";Files.createDirectories(Path.of("build/reports/lionfish"));Files.writeString(Path.of("build/reports/lionfish/dark-swim068-results.txt"),report);System.out.print(report);
  }
}
