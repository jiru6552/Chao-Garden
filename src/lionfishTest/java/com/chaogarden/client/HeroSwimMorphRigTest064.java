package com.chaogarden.client;

import com.chaogarden.core.ChaoState;
import com.chaogarden.core.DriveRules;
import com.chaogarden.core.HeroSwimMorphology064;
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
public final class HeroSwimMorphRigTest064 {
  private static final Path ROOT=Path.of("src/main/resources/assets/chao_garden");
  private static final String MODEL="gameplay/chao_hero_swim_proto064";
  private static int checks,frames,solids,motionFrames,attachments,coatCases,haloPairs,wingSamples,hingeSamples,connectedFeatures,bookSurfaceFrames,baselineGuardFrames,surfacePairs,headTorsoFrames,palmHeadFrames,shoulderFrames,expressionFrames,wingUvSamples,tailSamples,featurePalmPairs,headCapUvSamples;
  private static float maxStep,maxEdit,maxHinge,maxMaturity,maxTailReferenceGapDelta;
  private static int featureBoxes,wingBoxesPerSide,totalBoxes;
  private static final String[] HEAD_FEATURE_NAMES={"hero_swim_crown_left_root","hero_swim_crown_left_tip","hero_swim_crown_right_root","hero_swim_crown_right_tip"};
  private static final Map<String,Vector3f> ORIGINAL_WING_HINGES=new HashMap<>();
  private static final java.util.Set<String> MORPH_BONES=new HashSet<>();
  private static final Map<String,Capture> FIXED_HALO=new HashMap<>();
  private static ChaoMeshAsset retainedTailReference;
  private static void check(boolean ok,String why){checks++;if(!ok)throw new AssertionError(why);}
  private static void near(float a,float b,float e,String why){check(Float.isFinite(a)&&Float.isFinite(b)&&Math.abs(a-b)<=e,why+": "+a+" / "+b);}
  private static JsonObject data(String key)throws Exception{return JsonParser.parseString(Files.readString(ROOT.resolve("models/entity/"+key+".json"))).getAsJsonObject();}
  private static ChaoMeshAsset asset(String key){try{var m=ChaoMeshAsset.parse(data(key));m.lens(0xff101820);m.expression(1);return m;}catch(Exception e){throw new IllegalStateException(key,e);}}
  private static ChaoState state(float rp,float sf,float age){
    var s=new ChaoState();s.adult=true;s.primaryType=ChaoState.SWIM_TYPE;s.align=1;
    int r=Math.round(Math.abs(rp)*960),f=Math.round(Math.abs(sf)*960);
    int rs=rp<0?ChaoState.RUN:ChaoState.POWER,fs=sf<0?ChaoState.SWIM:ChaoState.FLY;
    s.levels[rs]=r/100;s.progress[rs]=r%100;s.points[rs]=r;
    s.levels[fs]=f/100;s.progress[fs]=f%100;s.points[fs]=f;
    s.setEvolutionStrength(age*1.2f);return ChaoState.decode(s.encode());
  }
  private static void sample(ChaoMeshAsset m,ChaoState s){m.development(s.swimFly,s.runPower,s.gradualVisualStrength());m.heroSwimDevelopment(HeroSwimMorphology064.sample(s));}
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
      for(int v=0;v<24;v++){float ap=a.point(ai+v).dot(axis),bp=b.point(bi+v).dot(axis);alo=Math.min(alo,ap);ahi=Math.max(ahi,ap);blo=Math.min(blo,bp);bhi=Math.max(bhi,bp);}
      if(Math.min(ahi,bhi)-Math.max(alo,blo)<.000001f)return false;
    }return true;
  }
  private static void featureContact(ChaoMeshAsset m,Map<String,ChaoMeshAsset.Pose> p){
    var skull=select(m,p,"skull__cube_0");var features=select(m,p,"gradual_head_021");
    check(skull.count()==24&&features.count()==featureBoxes*24,"Fixed skull or authored paired crown topology changed");
    surfacePlanes(inJointFrame(m,p,"skull",skull),inJointFrame(m,p,"skull",features));
    cuboids(inJointFrame(m,p,"gradual_head_021",features));
    var left=select(m,p,"skull__cube_2");var right=select(m,p,"skull__cube_3");
    var attached=new boolean[featureBoxes];int reached=0;
    for(int i=0;i<featureBoxes;i++){
      check(!intersects(features,i*24,left,0)&&!intersects(features,i*24,right,0),"Draping crown intersects an unchanged eye");
      if(intersects(features,i*24,skull,0)){attached[i]=true;reached++;}
    }
    boolean changed=true;while(changed){changed=false;for(int i=0;i<featureBoxes;i++)if(!attached[i])for(int j=0;j<featureBoxes;j++)if(attached[j]&&intersects(features,i*24,features,j*24)){attached[i]=true;reached++;changed=true;break;}}
    check(reached==featureBoxes,"Growing paired crown disconnected from original head or adjoining root");connectedFeatures+=reached;
    for(String name:List.of("emotion_dot__cube_0","hero_swim_halo_064_1","hero_swim_halo_064_2","hero_swim_halo_064_3")){
      var halo=select(m,p,name);check(halo.count()==24,"Hero halo lost a closed bar");cuboids(inJointFrame(m,p,"emotion_dot",halo));
      check(!intersects(halo,0,skull,0),"Tilted halo intersects fixed skull");haloPairs++;
      for(int i=0;i<featureBoxes;i++){check(!intersects(halo,0,features,i*24),"Tilted halo intersects growing Hero Swim crown");haloPairs++;}
    }
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
  private static void wings(ChaoMeshAsset m,Map<String,ChaoMeshAsset.Pose> p,boolean symmetric){
    var left=select(m,p,"wing_left__cube_0");var right=select(m,p,"wing_right__cube_0");
    check(left.count()==wingBoxesPerSide*24&&right.count()==wingBoxesPerSide*24,"Hero Swim wings lost their authored positive closed six-face plates");
    if(symmetric){cuboids(left);cuboids(right);}else{affineSurfaces(left);affineSurfaces(right);cuboids(inJointFrame(m,p,"wing_left",left));cuboids(inJointFrame(m,p,"wing_right",right));}
    wingSamples+=2*wingBoxesPerSide;if(symmetric)mirrored(left,right,"Hero Swim paired wing plates");
    wingHinge(m,p,"wing_left",left);wingHinge(m,p,"wing_right",right);
    wingSpanUv(m,p,"wing_left",left);wingSpanUv(m,p,"wing_right",right);
    var torso=select(m,p,"body__cube_0");
    for(int ray=0;ray<wingBoxesPerSide;ray++)check(intersects(left,ray*24,torso,0)&&intersects(right,ray*24,torso,0),"A rendered Hero Swim wing plate root detached from the actual torso");
  }
  private static void tailContact(ChaoMeshAsset mesh,Map<String,ChaoMeshAsset.Pose> poses){
    var tail=select(mesh,poses,"tail__cube_0");var base=select(mesh,poses,"tail__cube_1");
    check(tail.count()==24&&base.count()==24,"Retained original two-box tail topology changed");
    cuboids(inJointFrame(mesh,poses,"tail",tail));cuboids(inJointFrame(mesh,poses,"tail",base));
    // This family retains the original tail, whose boxes need not have the
    // positive-volume overlap authored for .63's new tail. Compare the actual
    // emitted surfaces and signed gap against the unchanged canonical baby rest
    // under identical copied joint poses, rather than inventing a new seam.
    if(retainedTailReference==null){
      retainedTailReference=asset("gameplay/chao_baby");
      // The canonical reference must use its authored rest vertices. Legacy
      // baby assets default to fully aged morph_vertices, including a3.5%
      // tail-depth scale, while every fixed-box profile uses base vertices
      // for un-authored parts. Turning that separate legacy morph off does
      // not alter a source record or relax the exact emitted-tail invariant.
      retainedTailReference.development(0,0,0);
    }
    var referencePoses=retainedTailReference.createPoses();for(var entry:poses.entrySet())if(referencePoses.containsKey(entry.getKey()))mesh.copyJointPoseTo(entry.getKey(),retainedTailReference,entry.getValue(),referencePoses.get(entry.getKey()));
    var oldTip=select(retainedTailReference,referencePoses,"tail__cube_0");var oldBase=select(retainedTailReference,referencePoses,"tail__cube_1");
    equal(oldTip,tail,.00004f,"Retained original tail tip geometry or UV changed");equal(oldBase,base,.00004f,"Retained original tail base geometry or UV changed");
    float actualGap=overlapDepth(tail,base),referenceGap=overlapDepth(oldTip,oldBase);maxTailReferenceGapDelta=Math.max(maxTailReferenceGapDelta,Math.abs(actualGap-referenceGap));
    near(actualGap,referenceGap,.00004f,"Retained original tail joint gap changed");if(tailSamples==0)System.out.println("HeroSwim064 unchanged tail baseline: actual signed SAT overlap="+actualGap+" pixels, copied canonical baby rest reference="+referenceGap+" pixels; strict positive-volume contact is not an inherited guarantee.");tailSamples++;
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
    var meshes=bone.getAsJsonArray("meshes");check(meshes.size()==wingBoxesPerSide,"Hero Swim wing lost its metadata-declared lobe count");
    String side=bone.get("name").getAsString().contains("left")?"left":"right";var names=new HashSet<String>();
    for(var value:meshes){
      var mesh=value.getAsJsonObject();String name=mesh.get("name").getAsString();check(name.startsWith("hero_swim_wing_"+side+"_")&&names.add(name),"Wing plate ownership/name is invalid or duplicated");
      check(mesh.has("hero_swim_box_064")&&mesh.getAsJsonArray("vertices").size()==8&&mesh.getAsJsonArray("faces").size()==6,"Wing plate lost its ten-target rigid closed-box construction");
      for(var element:mesh.getAsJsonArray("faces")){
        var face=element.getAsJsonObject();var uv=face.getAsJsonArray("uv");var ids=face.getAsJsonArray("v");check(uv.size()==4&&ids.size()==4,"Wing plate UV/corner correspondence changed");
        for(int i=0;i<4;i++){
          int id=ids.get(i).getAsInt();boolean positiveX=id==0||id==3||id==4||id==7;boolean distal=side.equals("right")==positiveX;
          var point=uv.get(i).getAsJsonArray();near(point.get(0).getAsFloat(),distal?61.75f:62.25f,0,"Wing root/distal UV does not follow the actual plate endpoint vertex");near(point.get(1).getAsFloat(),31.75f,0,"Wing face escaped the two adjacent opaque semantic texels");
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
        near(local.uv[vertex*2]*64,62.25f-.5f*fraction,.00005f,"Actual emitted wing UV does not follow its physical root-to-distal span");near(local.uv[vertex*2+1]*32,31.75f,.00005f,"Actual emitted wing UV leaves the opaque semantic row");
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
          int x=(int)Math.floor(u*2),y=(int)Math.floor(v*2),expected=physicalFraction<.5f?124:123;
          check(x==expected&&y==63,"Actual wing span lost its broad root/distal color band or sampled another atlas region");
          if(x==123)distalSamples++;wingUvSamples++;
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
    for(String name:List.of("arm_left__cube_1","arm_right__cube_1","foot_left__cube_0","foot_right__cube_0")){
      var c=solid(m,name);cuboids(c);float[] lo={999,999,999},hi={-999,-999,-999};for(int i=0;i<c.n;i++){int a=i%3;lo[a]=Math.min(lo[a],c.p[i]);hi[a]=Math.max(hi[a],c.p[i]);}boolean foot=name.startsWith("foot");float min=999,max=0;for(int a=0;a<3;a++){min=Math.min(min,hi[a]-lo[a]);max=Math.max(max,hi[a]-lo[a]);}check(max/min<(foot?2.6f:1.4f),"Stretched hand/foot");if(foot)near(hi[1],24,.00005f,"Sole support plane moved");
    }
  }
  /** Byte-exact protected assets from delivered0.25.63, independent of the
   * generator and new profile's metadata. These are portable source checks. */
  private static void protectedAssets()throws Exception{
    var expected=Map.ofEntries(
        Map.entry("chao_baby.json","ae3d4da88b84905f8726b863bdb072e3a26eb404f38e565d0781b92b01e2cf9b"),
        Map.entry("chao_hero_run_proto055.json","77b41d4501cba585b83be91b4052e8c56e03bff02e47db2b209f146d36390f08"),
        Map.entry("chao_neutral_proto056.json","a354458cae5605b873464614b57f9f9bd04d4a4e01d1ed793cf949194c4d14db"),
        Map.entry("chao_hero_normal_proto057.json","d0052db7f401ecc545e7a3db2992933b5671387520c82429d4a8aa336f28232d"),
        Map.entry("chao_neutral_swim_proto059.json","06c6c2372669adb2fcf0fb4d529cbf9d0866b20e4b0d23b002e84c9fc8a9a1a8"),
        Map.entry("chao_dark_normal_proto060.json","36ae27aed51674ea31310ab4af602e3e7127e1507c736ee9b6f75f3728acb514"),
        Map.entry("chao_neutral_fly_proto061.json","2524c85c7c121531233b03eee36cdb3df6c8a39d568fbe9b0faea09ba887a885"),
        Map.entry("chao_neutral_run_proto062.json","cfed60309de43bc854272a4869d455d9ec76281a37a1787ff223f69b7ed85881"),
        Map.entry("chao_neutral_power_proto063.json","c3bfcbb2fb1730bbd23b8799e35d9a2edf33b3f4fab06c11a4638bacd1db07f0"));
    for(var entry:expected.entrySet()){
      byte[] bytes=Files.readAllBytes(ROOT.resolve("models/entity/gameplay/"+entry.getKey()));
      String actual=java.util.HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256").digest(bytes));
      check(actual.equals(entry.getValue()),"Previously delivered model changed: "+entry.getKey());
    }
  }
  private static void topology()throws Exception{
    var d=data(MODEL);var original=data("gameplay/chao_baby");var metadata=JsonParser.parseString(Files.readString(ROOT.resolve("hero_swim064.json"))).getAsJsonObject();
    check(d.has("hero_swim_path_064")&&!d.has("hero_normal_path_057")&&!d.has("neutral_swim_path_059")&&!d.has("neutral_power_path_063"),"Hero Swim reused a prior family's admission flag");
    featureBoxes=metadata.get("head_feature_count").getAsInt();int wingBoxes=metadata.get("wing_ray_count").getAsInt();totalBoxes=metadata.get("closed_box_count").getAsInt();
    check(featureBoxes==4&&wingBoxes==4&&totalBoxes==17,"Hero Swim must use four paired crown boxes, four halo bars, four wing lobes and five body/limb boxes");wingBoxesPerSide=wingBoxes/2;
    Map<String,JsonObject> old=new HashMap<>();for(var b:original.getAsJsonArray("bones"))old.put(b.getAsJsonObject().get("name").getAsString(),b.getAsJsonObject());
    check(old.size()==59&&d.getAsJsonArray("bones").size()==62,"All59 original joints plus three mesh-only halo children are required");
    for(int i=0;i<59;i++)check(d.getAsJsonArray("bones").get(i).getAsJsonObject().get("name").equals(original.getAsJsonArray("bones").get(i).getAsJsonObject().get("name")),"Original59 shared-joint ordering changed");
    for(var be:d.getAsJsonArray("bones")){
      var b=be.getAsJsonObject();String name=b.get("name").getAsString();
      if(old.containsKey(name)){
        check(b.get("pivot").equals(old.get(name).get("pivot"))&&b.get("rotation").equals(old.get(name).get("rotation"))&&java.util.Objects.equals(b.get("parent"),old.get(name).get("parent")),"Original rig/attachment joint changed "+name);
        if(name.startsWith("skull")||name.startsWith("expression_")||name.startsWith("tail")||name.startsWith("emotion_")&&!name.equals("emotion_dot__cube_0"))check(b.equals(old.get(name)),"Original head/face/reaction/tail changed "+name);
        if(name.equals("wing_left__cube_0")||name.equals("wing_right__cube_0"))wingUv(b,old.get(name));
      }else check(List.of("hero_swim_halo_064_1","hero_swim_halo_064_2","hero_swim_halo_064_3").contains(name)&&b.get("parent").getAsString().equals("emotion_dot"),"Unexpected new attachment joint outside three halo mesh children");
    }
    check(d.get("head_decorations").equals(original.get("head_decorations")),"Original hat hiding family changed");
    var profile=d.getAsJsonObject("hero_swim_path_064");check(profile.getAsJsonArray("targets").size()==10,"Wrong fresh-plus-full-signed-grid target count");
    var names=List.of("hero_swim_young","run_swim","swim","power_swim","run","balanced","power","run_fly","fly","power_fly");
    for(int i=0;i<10;i++)check(profile.getAsJsonArray("targets").get(i).getAsString().equals(names.get(i))&&metadata.getAsJsonArray("target_names").get(i).getAsString().equals(names.get(i)),"Signed-grid target order differs from normalized current-stat axes");
    var crown=d.getAsJsonArray("bones").asList().stream().map(v->v.getAsJsonObject()).filter(v->v.get("name").getAsString().equals("gradual_head_021")).findFirst().orElseThrow().getAsJsonArray("meshes");
    check(crown.size()==4,"Draping crown lost its single root and tip per side");
    for(int i=0;i<4;i++){check(crown.get(i).getAsJsonObject().get("name").getAsString().equals(HEAD_FEATURE_NAMES[i]),"Paired crown ownership/order changed");pointCapUv(crown.get(i).getAsJsonObject(),i%2==1);}
    int boxes=0;for(var be:d.getAsJsonArray("bones"))for(var me:be.getAsJsonObject().getAsJsonArray("meshes")){
      var part=me.getAsJsonObject();if(!part.has("hero_swim_box_064"))continue;boxes++;MORPH_BONES.add(be.getAsJsonObject().get("name").getAsString());
      check(part.getAsJsonArray("vertices").size()==8&&part.getAsJsonArray("faces").size()==6,"Authored part is not one closed cuboid");
      for(var field:part.getAsJsonObject("hero_swim_box_064").entrySet())check(field.getValue().getAsJsonArray().size()==10,"Part lost full ten-target correspondence");
    }
    check(boxes==totalBoxes,"Hero Swim lost its metadata-declared closed construction");
  }

  /** Validate each actual white root and two-pigment distal crown box.
   * Longitudinal cap pigment uses two adjacent texels across one closed box;
   * no extra band geometry or Run stripe tile is introduced. */
  private static void pointCapUv(JsonObject mesh,boolean tip){
    for(var f:mesh.getAsJsonArray("faces")){
      var face=f.getAsJsonObject();var uv=face.getAsJsonArray("uv");var ids=face.getAsJsonArray("v");
      for(int i=0;i<4;i++){
        int id=ids.get(i).getAsInt();boolean distal=id==2||id==3||id==6||id==7;
        var p=uv.get(i).getAsJsonArray();near(p.get(0).getAsFloat(),tip?(distal?63.25f:62.75f):63.75f,0,"Crown UV no longer follows root/proximal/distal vertex ownership");near(p.get(1).getAsFloat(),31.75f,0,"Crown UV escaped the scoped semantic row");
      }
    }
  }

  /** Independent bilinear samples on actual production-emitted head surfaces.
   * Every complete distal box must carry opaque cap pigment on all six faces. */
  private static void headCapFaces(Capture features){
    for(int box=0;box<featureBoxes;box++)for(int face=0;face<6;face++)for(int row=0;row<4;row++)for(int column=0;column<4;column++){
      float x=(column+.5f)/4,y=(row+.5f)/4;float[] weights={(1-x)*(1-y),x*(1-y),x*y,(1-x)*y};float u=0,v=0;
      for(int i=0;i<4;i++){int vertex=box*24+face*4+i;u+=features.uv[vertex*2]*128*weights[i];v+=features.uv[vertex*2+1]*64*weights[i];}
      int px=(int)Math.floor(u),py=(int)Math.floor(v);boolean tip=box%2==1;
      check((tip?(px==125||px==126):px==127)&&py==63,"Actual Hero Swim crown left its root/proximal/distal swatches");headCapUvSamples++;
    }
  }

  private static void development(){
    var m=asset(MODEL);var original=asset("gameplay/chao_baby");original.development(0,0,0);var p=poses(m);var head=solid(original,"skull__cube_0");var eyeL=solid(original,"skull__cube_2");var eyeR=solid(original,"skull__cube_3");
    var endpoints=new HashSet<Long>();
    for(float age:new float[]{0,.25f,.5f,.75f,1})for(int yi=0;yi<=10;yi++)for(int xi=0;xi<=10;xi++){
      var state=state(xi/5f-1,yi/5f-1,age);String saved=state.encode();sample(m,state);frames++;for(String bone:MORPH_BONES)cuboids(solid(m,bone));
      equal(head,solid(m,"skull__cube_0"),.00004f,"Original 7x6x6 head resized");equal(eyeL,solid(m,"skull__cube_2"),.00004f,"Left eye moved/distorted");equal(eyeR,solid(m,"skull__cube_3"),.00004f,"Right eye moved/distorted");marker(m,HeroSwimMorphology064.sample(state));
      cuboids(solid(m,"gradual_head_021"));mirrored(solid(m,"gradual_head_021"),solid(m,"gradual_head_021"),"Paired Hero Swim crown features");compact(m);featureContact(m,p);wings(m,p,true);tailContact(m,p);check(saved.equals(state.encode()),"Rendering changed saved data");
      if(age==1&&xi%5==0&&yi%5==0)endpoints.add(m.shapeSignature());
    }
    check(endpoints.size()==9,"Requested nine mature combinations collapsed into duplicate forms");
    for(float age:new float[]{0,.5f,1}){
      for(float sf:new float[]{-1,0,1}){Capture before=null;for(int i=0;i<=100;i++){sample(m,state(i/50f-1,sf,age));var now=capture(m,p);surfacePlanes(m,p);maxStep=Math.max(maxStep,step(before,now,.3f,"Small Run/Power change popped geometry"));before=now;frames++;}}
      for(float rp:new float[]{-1,0,1}){Capture before=null;for(int i=0;i<=100;i++){sample(m,state(rp,i/50f-1,age));var now=capture(m,p);surfacePlanes(m,p);maxStep=Math.max(maxStep,step(before,now,.3f,"Small Swim/Fly change popped geometry"));before=now;frames++;}}
    }
    for(float rp:new float[]{-1,0,1})for(float sf:new float[]{-1,0,1}){Capture before=null;for(int i=0;i<=100;i++){sample(m,state(rp,sf,i/100f));var now=capture(m,p);surfacePlanes(m,p);maxMaturity=Math.max(maxMaturity,step(before,now,.15f,"One-percent maturity change popped geometry"));before=now;frames++;}}
    sample(m,state(-1,1,0));long fresh=m.shapeSignature();sample(m,state(1,0,0));check(fresh==m.shapeSignature(),"Training overrides independent fresh-adult age");
    for(float rp:new float[]{-1,0,1}){sample(m,state(rp,0,1));long balanced=m.shapeSignature();sample(m,state(rp,-1,1));long swim=m.shapeSignature();sample(m,state(rp,1,1));long fly=m.shapeSignature();check(balanced!=swim&&balanced!=fly&&swim!=fly,"Hero Swim-first preserves distinct balanced, Swim and Fly surfaces on either side of the opposing axis");}
    var state=state(-1,1,1);sample(m,state);long before=m.shapeSignature();float maturity=state.evolutionStrength();DriveRules.setTotal(state,ChaoState.RUN,0,new Random(64));sample(m,state);check(before!=m.shapeSignature()&&maturity==state.evolutionStrength(),"Run loss does not reverse independently from age");
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
  private static void marker(ChaoMeshAsset mesh,HeroSwimMorphology064.Sample sample){
    // These four bars intentionally share one small authored local-Y shift
    // as the crown develops. Preserve the rigid ring, original ancestor tilt,
    // UVs and X/Z placement, and check the independently declared ten heights.
    String[] names={"emotion_dot__cube_0","hero_swim_halo_064_1","hero_swim_halo_064_2","hero_swim_halo_064_3"};
    if(FIXED_HALO.isEmpty()){
      var reference=asset(MODEL);var referencePoses=reference.createPoses();
      for(String name:names)FIXED_HALO.put(name,inJointFrame(reference,referencePoses,"emotion_dot",solid(reference,name)));
    }
    float[] heights={.65f,.60f,.60f,.55f,.65f,.70f,.55f,.65f,.50f,.55f};
    float height=0;var weights=HeroSwimMorphology064.weights(sample);for(int i=0;i<10;i++)height+=weights[i]*heights[i];
    check(height>=.49999f&&height<=.70001f,"Hero Swim halo translation escaped its authored0.2px range");float shift=height-.65f;
    var p=mesh.createPoses();float firstShift=Float.NaN;
    for(String name:names){
      var original=FIXED_HALO.get(name);var actual=inJointFrame(mesh,p,"emotion_dot",solid(mesh,name));
      check(original.n==actual.n&&original.u==actual.u&&actual.count()==24,"Hero halo bar topology changed");cuboids(actual);
      for(int i=0;i<actual.n;i++)near(actual.p[i],original.p[i]+(i%3==1?shift:0),.00004f,"Rigid Hero halo changed size/rotation/XZ or failed its shared authored local-Y shift");
      for(int i=0;i<actual.u;i++)near(actual.uv[i],original.uv[i],0,"Hero halo UV changed while the crown developed");
      float translated=center(actual,0).y-center(original,0).y;
      if(Float.isNaN(firstShift))firstShift=translated;else near(translated,firstShift,.00002f,"Hero halo bars moved independently and opened the ring");
    }
  }

  /** Temporary opt-in numerical diagnostic, using the same production WALK
   * clip/clock/renderer and independent emitted-box SAT as the assertion pass. */
  private static void diagnoseWalk()throws Exception{
    var clips=JsonParser.parseString(Files.readString(ROOT.resolve("animations/official/chao.animation.json"))).getAsJsonObject().getAsJsonObject("animations");
    var clocks=OfficialChaoRig.class.getDeclaredField("gaitClocks");clocks.setAccessible(true);var phase=com.chaogarden.core.ChaoGaitClock.class.getDeclaredField("phase");phase.setAccessible(true);
    int contacts=0,samples=0,shoulderMisses=0;float worst=0,worstShoulder=0;String detail="none",shoulderDetail="none";var shoulders=new StringBuilder("age,rp,sf,breathing,phase,yaw,pitch,hand,gap_px\n");var out=new StringBuilder("age,rp,sf,breathing,phase,yaw,pitch,hand,overlap_px\n");
    for(float age:new float[]{0,.5f,1})for(float sf:new float[]{-1,0,1})for(float rp:new float[]{-1,0,1}){
      var s=state(rp,sf,age);String key=OfficialChaoRig.geometryKey(s);var id=new UUID(64,600100);var rig=new OfficialChaoRig(clips,HeroSwimMorphRigTest064::asset);rig.prepareDevelopment(key,s);
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
    String report="HeroSwim064 production WALK palm diagnostic: contacts="+contacts+" / "+samples+"; maximum minimum-axis SAT overlap="+worst+" model pixels; "+detail+"; shoulder gaps="+shoulderMisses+"/"+samples+" maximum separating-axis gap="+worstShoulder+"px "+shoulderDetail+"\n";
    Files.createDirectories(Path.of("build/reports/lionfish"));Files.writeString(Path.of("build/reports/lionfish/hero-swim064-walk-diagnostic.csv"),out);Files.writeString(Path.of("build/reports/lionfish/hero-swim064-shoulder-diagnostic.csv"),shoulders);Files.writeString(Path.of("build/reports/lionfish/hero-swim064-walk-diagnostic.txt"),report);System.out.print(report);
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
  /** Viewer-informed directional relationships for authored Run features.
   * Compare emitted surfaces, not generator parameter values. */
  private static float longestEdge(Capture mesh,int box){float length=0;for(var edge:boxEdges(mesh,box*24))length=Math.max(length,edge.length());return length;}
  private static Capture localShape(ChaoMeshAsset mesh,Map<String,ChaoMeshAsset.Pose> poses,String joint,String bone,float rp,float sf,float age){sample(mesh,state(rp,sf,age));return inJointFrame(mesh,poses,joint,solid(mesh,bone));}
  private static void silhouettes(){
    var mesh=asset(MODEL);var p=poses(mesh);var signatures=new HashSet<Long>();
    for(int sf=-1;sf<=1;sf++)for(int rp=-1;rp<=1;rp++){
      sample(mesh,state(rp,sf,1));signatures.add(mesh.shapeSignature());var features=solid(mesh,"gradual_head_021");
      check(features.count()==4*24,"Hero Swim crown root/tip topology changed");mirrored(features,features,"Hero Swim mirrored paired draping crown");
      var local=inJointFrame(mesh,p,"skull",features);
      check(center(local,0).x<0&&center(local,2).x>0,"Crown roots no longer form a mirrored pair across fixed head");
      var wing=inJointFrame(mesh,p,"wing_left",solid(mesh,"wing_left__cube_0"));check(wing.count()==wingBoxesPerSide*24,"Hero Swim wing topology changed");
      for(int box=0;box<wingBoxesPerSide;box++)check(longestEdge(wing,box)<4,"Hero Swim retained small back wings became disproportionate");
    }
    check(signatures.size()==9,"Nine signed Hero Swim specializations collapsed into duplicate forms");
    heroSwimDirectionalSilhouettes(mesh,p);
  }

  /** Quantitative relationships from actual independently viewed Hero Swim-first
   * front/side/back references. +Z is rear and +Y is down in the fixed skull
   * joint. These broad silhouette directions do not equate source geometry. */
  private static Vector3f terminal(Capture features,int box){
    // The cap's actual distal endpoint is authored at native texel126. Its
    // four closed-box corners each occur on three faces after emission.
    var c=new Vector3f();int count=0;for(int i=box*24;i<(box+1)*24;i++)if(Math.abs(features.uv[i*2]*128-126.5f)<.0001f){c.add(features.point(i));count++;}
    check(count==12,"Distal crown face no longer follows actual cap UV ownership");return c.div(count);
  }
  private static void heroSwimDirectionalSilhouettes(ChaoMeshAsset mesh,Map<String,ChaoMeshAsset.Pose> p){
    // +Z is back and +Y is down in the fixed original skull frame. These
    // broad directional relationships come from independently operated viewer
    // forms; they do not assert equality with its source mesh geometry.
    var fresh=localShape(mesh,p,"skull","gradual_head_021",0,0,0);var balanced=crest(mesh,p,0,0);var run=crest(mesh,p,-1,0);var power=crest(mesh,p,1,0);var fly=crest(mesh,p,0,1);var swim=crest(mesh,p,0,-1);
    check(fresh.count()==balanced.count()&&balanced.count()==run.count(),"Age/training replaced the shared crown topology");
    for(int tip:new int[]{1,3}){
      check(terminal(run,tip).z<terminal(balanced,tip).z-.2f,"Hero Swim Run crown should drape forward of balanced");
      check(terminal(swim,tip).z>terminal(balanced,tip).z+.2f,"Hero Swim Swim crown should extend rearward of balanced");
      check(terminal(fly,tip).y<terminal(balanced,tip).y-.2f,"Opposite Fly terminal fins should rise above the balanced downturned tips");
      check(terminal(power,tip).y<terminal(balanced,tip).y-.2f,"Hero Swim Power should raise its broader upper crown");
    }
  }


  private static void temporal(){
    var m=asset(MODEL);var p=poses(m);var s=state(0,0,1);UUID id=new UUID(64,600060);
    int calendar=s.ageTicks,adultAge=s.adultAgeTicks;float biology=s.evolutionStrength();String lineage=s.lineageId();
    int duration=(int)HeroSwimAppearance064.CHANGE_TICKS;check(duration==30&&duration==HeroSwimAppearance064.CHANGE_TICKS,"The new Hero Swim features use the declared thirty-tick display interpolation");
    m.heroSwimDevelopment(HeroSwimAppearance064.prepare(id,s,0));var original=capture(m,p);featureContact(m,p);Capture before=original;
    DriveRules.setTotal(s,ChaoState.RUN,960,new Random(64));DriveRules.setTotal(s,ChaoState.FLY,960,new Random(64));before=settleEdit(m,p,id,s,1,before);
    near(HeroSwimMorphology064.sample(s).runPower(),-1,0,"Run gain selects the current negative axis");near(HeroSwimMorphology064.sample(s).swimFly(),1,0,"Compatible Fly gain selects the other current axis");
    DriveRules.setTotal(s,ChaoState.POWER,1920,new Random(64));before=settleEdit(m,p,id,s,1+(duration+1),before);
    near(HeroSwimMorphology064.sample(s).runPower(),1,0,"Power retraining actually crosses back through balance into its own signed corner");
    DriveRules.setTotal(s,ChaoState.SWIM,1920,new Random(64));before=settleEdit(m,p,id,s,1+2*(duration+1),before);
    near(HeroSwimMorphology064.sample(s).swimFly(),-1,0,"Opposing Swim actually crosses balance into its own signed corner");
    for(int stat=0;stat<4;stat++)DriveRules.setTotal(s,stat,0,new Random(64));before=settleEdit(m,p,id,s,1+3*(duration+1),before);
    equal(original,before,.00004f,"Gain, both full opposite crossings and reduction failed to recover the balanced form");
    check(s.ageTicks==calendar&&s.adultAgeTicks==adultAge&&s.evolutionStrength()==biology&&s.lockedPrimaryType()==ChaoState.SWIM_TYPE&&s.lockedAlignment()==1&&s.lineageId().equals(lineage),"Client specialization transitions fabricated biological youth, changed chronology or rewrote the first identity");
  }
  private static Capture settleEdit(ChaoMeshAsset mesh,Map<String,ChaoMeshAsset.Pose> poses,UUID id,ChaoState state,int tick,Capture previous){
    mesh.heroSwimDevelopment(HeroSwimAppearance064.prepare(id,state,tick));equal(previous,capture(mesh,poses),.00004f,"Large actual bar edit snapped its first display frame");featureContact(mesh,poses);
    Capture before=previous;
    for(int t=tick+1;t<=tick+(int)HeroSwimAppearance064.CHANGE_TICKS;t++){
      mesh.heroSwimDevelopment(HeroSwimAppearance064.prepare(id,state,t));var now=capture(mesh,poses);featureContact(mesh,poses);wings(mesh,poses,true);tailContact(mesh,poses);
      maxEdit=Math.max(maxEdit,step(before,now,1,"Configured actual edit interpolation jumped a model pixel"));before=now;frames++;
    }
    sample(mesh,state);equal(before,capture(mesh,poses),.00004f,"Client edit failed to reach the authoritative signed target within the declared display interval");return before;
  }

  private static void motion()throws Exception{
    var clips=JsonParser.parseString(Files.readString(ROOT.resolve("animations/official/chao.animation.json"))).getAsJsonObject().getAsJsonObject("animations");var rig=new OfficialChaoRig(clips,HeroSwimMorphRigTest064::asset);var expected=asset(MODEL);int serial=0;
    for(float age:new float[]{0,.5f,1})for(float sf:new float[]{-1,0,1})for(float rp:new float[]{-1,0,1})for(var mode:List.of(OfficialChaoRig.Motion.CRAWL,OfficialChaoRig.Motion.WALK,OfficialChaoRig.Motion.RUN,OfficialChaoRig.Motion.SIT,OfficialChaoRig.Motion.SWIM,OfficialChaoRig.Motion.GLIDE,OfficialChaoRig.Motion.HELD))for(int tick:new int[]{0,10,20}){
      var s=state(rp,sf,age);String saved=s.encode();String key=OfficialChaoRig.geometryKey(s);rig.prepareDevelopment(key,s);rig.animateGameplay(key,new UUID(64,++serial),mode,tick,1,.05f,0,0,0,0,false);rig.development(s);sample(expected,s);check(rig.finite(),"Lionfish pose nonfinite");check(rig.sampledMesh().shapeSignature()==expected.shapeSignature(),"Production selection skipped HeroSwim064");check(rig.sourceGeometryKey().equals("baby"),"UV/attachment family changed");check(saved.equals(s.encode()),"Motion changed saved state");featureContact(rig.sampledMesh(),rig.poses());
      try{wings(rig.sampledMesh(),rig.poses(),false);}catch(AssertionError failure){var wingMatrix=new PoseStack();rig.sampledMesh().transformBone("wing_left",wingMatrix,rig.poses());var body=rig.poses().get("body");throw new AssertionError("Wing frame age="+age+" rp="+rp+" sf="+sf+" mode="+mode+" tick="+tick+" bodyScale="+body.sx+","+body.sy+","+body.sz+" leftWingMatrix="+Arrays.toString(wingMatrix.last().pose().get(new float[16]))+"; "+failure.getMessage(),failure);}
      tailContact(rig.sampledMesh(),rig.poses());motionFrames++;
      for(boolean left:new boolean[]{false,true}){float[] palm=rig.toyPalm(left);check(palm!=null&&palm.length==3,"Missing toy palm");for(float v:palm)check(Float.isFinite(v),"Toy palm nonfinite");var at=ChaoTriangleAnimation.palm(rig::transformBone,palm,left).mul(16);var emitted=select(rig.sampledMesh(),rig.poses(),left?"arm_left__cube_1":"arm_right__cube_1");check(emitted.count()==24,"Toy grip lost its actual compact hand mesh");near(at.distance(center(emitted,0)),0,.00004f,"Toy grip does not follow actual production-emitted palm");}
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
      var s=state(1,1,1);var rig=new OfficialChaoRig(clips,HeroSwimMorphRigTest064::asset);String key=OfficialChaoRig.geometryKey(s);
      rig.prepareDevelopment(key,s);rig.animateGameplay(key,new UUID(64,600060+tick),OfficialChaoRig.Motion.PLAY,tick,0,0,0,0,0,0,false);rig.development(s);
      float weight=com.chaogarden.toys.QuietToyPlayRules049.presence(tick);
      ChaoQuietToyAnimation049.pose(rig.poses(),rig::transformBone,rig.toyPalm(false),rig.toyPalm(true),
          com.chaogarden.toys.QuietToyPlayRules049.BOOK,weight,quiet.sweep(tick),quiet.page(tick),quiet.stroke(tick),quiet.pen(tick),tick,.8f,rig::attachmentBounds);
      check(rig.finite(),"Production quiet-book pose is not finite");featureContact(rig.sampledMesh(),rig.poses());tailContact(rig.sampledMesh(),rig.poses());bookSurfaceFrames++;
    }
  }
  private static void stableCache()throws Exception{
    var clips=JsonParser.parseString(Files.readString(ROOT.resolve("animations/official/chao.animation.json"))).getAsJsonObject().getAsJsonObject("animations");var revision=ChaoMeshAsset.class.getDeclaredField("pathRevision056");revision.setAccessible(true);
    var s=state(-1,1,.5f);var rig=new OfficialChaoRig(clips,HeroSwimMorphRigTest064::asset);rig.prepareDevelopment(OfficialChaoRig.geometryKey(s),s);var mesh=rig.sampledMesh();check(mesh.heroSwimPrototype064(),"Cache fixture selected wrong profile");
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
      var actual=new OfficialChaoRig(clips,HeroSwimMorphRigTest064::asset);actual.prepareDevelopment(key,state);
      check(guard.getBoolean(actual),"Every Neutral/Hero Swim-first corner retains baseline full-head walking clearance");
      actual.animateGameplay(key,id,OfficialChaoRig.Motion.WALK,300,11,.05f,0,0,0,0,false,false);actual.development(state);
      var control=new OfficialChaoRig(clips,HeroSwimMorphRigTest064::asset);control.prepareDevelopment(key,state);guard.setBoolean(control,false);power.setFloat(control,0);
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
      var rig=new OfficialChaoRig(clips,HeroSwimMorphRigTest064::asset);rig.prepareDevelopment(key,state);check(guard.getBoolean(rig),"Fixed head needs its guard at every body-growth corner");
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
      // Every full-gaze phase tests all four growing features, including
      // the low paired crown roots and distal caps. The dense straight-gaze stride
      // retains its existing nine feature-contact checkpoints separately.
      if(examineFeatures){
        var features=select(rig.sampledMesh(),rig.poses(),"gradual_head_021");
        for(int feature=0;feature<featureBoxes;feature++){
          check(!intersects(features,feature*24,palm,0),"Production WALK palm intersects growing Hero Swim crown feature: target="+target+" phase="+phase+" breath="+breathing+" yaw="+yaw+" pitch="+pitch+" feature="+feature+" hand="+hand);featurePalmPairs++;
        }
      }
    }
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
      var names=new HashSet<String>();for(var element:data(MODEL).getAsJsonArray("bones")){String name=element.getAsJsonObject().get("name").getAsString();if(name.startsWith("emotion_"+symbol+"__")||symbol.equals("dot")&&name.startsWith("hero_swim_halo_064_"))names.add(name);}markers.put(symbol,names);
    }
    var face=new HashSet<String>(List.of("skull__cube_0","skull__cube_2","skull__cube_3"));var mouths=new HashSet<String>();for(int i=1;i<=6;i++){face.add("expression_mouth_"+i);mouths.add("expression_mouth_"+i);}
    int serial=0;
    for(float[] profile:new float[][]{{0,0,0},{-1,1,.5f},{1,1,1}})for(int emotion:new int[]{0,1,2,3}){
      var state=state(profile[0],profile[1],profile[2]);String saved=state.encode(),key=OfficialChaoRig.geometryKey(state);var rig=new OfficialChaoRig(clips,HeroSwimMorphRigTest064::asset);
      rig.prepareDevelopment(key,state);rig.animateGameplay(key,new UUID(64,601200+serial++),OfficialChaoRig.Motion.WALK,120,0,0,emotion,.6f,12,-10,false,false);rig.development(state);
      int expression=emotion==1?2:emotion==2?3:emotion==3?1:0;rig.expression(expression);rig.sampledMesh().lens(0xff101820);
      int dot=visibleGroup(rig.sampledMesh(),rig.poses(),markers.get("dot")).count();
      check(dot==(emotion==0?96:0),"All four Hero halo bars must hide with its dot parent during visible reactions");
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
        state.animalMask=1<<gene;state.colorGenes=new int[]{gene%ChaoState.COLORS.length,(gene+1)%ChaoState.COLORS.length};state.toneGenes=new int[]{gene%2,(gene+1)%2};state.shinyGenes=new int[]{1,0};state.jewelGenes=new int[]{gene%ChaoState.JEWELS.length,(gene+1)%ChaoState.JEWELS.length};state.setTrait("heroswim64.foreign.dna","retained "+gene);
        String saved=state.encode();sample(mesh,state);equal(reference,capture(mesh,poses),0,"Genetics or animal DNA rewrote the ordinary Hero Swim-first base shape/UV");check(saved.equals(state.encode()),"Rendering changed earned DNA, genetics or foreign traits");
      }
    }
  }
  private static void coats(){
    var m=asset(MODEL);var p=poses(m);
    for(float rp:new float[]{-1,0,1})for(float sf:new float[]{-1,0,1}){var s=state(rp,sf,1);sample(m,s);var reference=capture(m,p);for(int color=0;color<ChaoState.COLORS.length;color++)for(int jewel=0;jewel<ChaoState.JEWELS.length;jewel++)if(color==0||jewel==0)for(boolean shiny:new boolean[]{false,true})for(boolean twoTone:new boolean[]{false,true}){s.expressedColor=color;s.expressedJewel=jewel;s.shiny=shiny;s.twoTone=twoTone;sample(m,s);equal(reference,capture(m,p),0,"Color/shine changed form/UV");coatCases++;}}
  }
  public static void main(String[] args)throws Exception{
    if("true".equalsIgnoreCase(System.getenv("CHAO_HEROSWIM064_WALK_DIAGNOSTIC"))){topology();diagnoseWalk();return;}
    long start=System.nanoTime();protectedAssets();topology();development();silhouettes();temporal();motion();bookTipSurfaces();stableCache();baselineHeadGuard();headTorsoContinuity();expressionVisibility();shapeDna();coats();String report="HeroSwim064: "+checks+" checks; "+frames+" emitted age/axis/edit frames; "+solids+" closed rectangular-box samples; "+motionFrames+" production Lionfish motion frames; "+attachments+" separate attachment matrices; "+coatCases+" coat/shape cases; "+haloPairs+" actual tilted-halo/head/feature separation checks; "+connectedFeatures+" connected feature samples; "+wingSamples+" emitted closed-wing samples; "+tailSamples+" retained original tail continuity checks; "+wingUvSamples+" actual wing-span UV samples; "+hingeSamples+" original-edge hinge samples; "+bookSurfaceFrames+" production quiet-book surface frames; "+baselineGuardFrames+" actual baseline walking-head guard comparisons; "+surfacePairs+" actual same-facing head/feature plane-pair checks; "+headTorsoFrames+" actual WALK head/torso SAT continuity frames; "+palmHeadFrames+" actual WALK palm/head SAT separation frames; "+featurePalmPairs+" actual WALK growing-feature/palm SAT separation pairs; "+headCapUvSamples+" actual crown root/proximal/distal surface UV samples; "+shoulderFrames+" actual WALK shoulder/wrist continuity frames; "+expressionFrames+" production-emitter expression/marker visibility frames; maximum retained-tail signed-gap deviation="+maxTailReferenceGapDelta+" model pixels; maximum hinge drift="+maxHinge+" model pixels; maximum axis step="+maxStep+" model pixels; maximum"+(int)HeroSwimAppearance064.CHANGE_TICKS+"tick edit step="+maxEdit+"; maximum1pct maturity step="+maxMaturity+"; seconds="+(System.nanoTime()-start)/1e9+". Fixed original head/face/59-joint rig plus three halo mesh children; nine distinct mature corners; reversible independent age/axes; canonical baby UV/attachment alias and current production selection. Geometry validation, not visual/multiplayer verification.\n";Files.createDirectories(Path.of("build/reports/lionfish"));Files.writeString(Path.of("build/reports/lionfish/hero-swim064-results.txt"),report);System.out.print(report);
  }
}
