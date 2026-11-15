package com.chaogarden.client;

import com.chaogarden.core.ChaoState;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.joml.Matrix4f;
import org.joml.Vector3f;

/** Actual selected rigs and emitted equipment surfaces. No Minecraft singleton or client is started. */
public final class WearableRigTest079 {
  private static final Path ROOT=Path.of("src/main/resources/assets/chao_garden");
  private static final List<String> ITEMS=List.of("baseball_cap_accessory","press_hat_accessory","headphones_accessory",
      "round_glasses_accessory","aviators_accessory","pacifier_accessory","bowtie_accessory","cheese_bowtie_accessory",
      "blue_hoodie_accessory","pink_hoodie_accessory","hoodie_sleeve_left","hoodie_sleeve_right","hoodie_hood",
      "hoodie_hood_folded","medal_race","medal_karate");
  private static final Map<String,ChaoMeshAsset> RIGS=new HashMap<>();
  private static final Map<String,Item> EQUIPMENT=new HashMap<>();
  private static long checks,vertices,fits,seams,portRays,crownRays;
  private static int states,frames;
  private static float minLensClearance=Float.POSITIVE_INFINITY,minTempleClearance=Float.POSITIVE_INFINITY;
  private static float minCrownClearance=Float.POSITIVE_INFINITY;
  private record Item(JsonObject data,ChaoMeshAsset mesh,ChaoAccessoryFit.Source source,List<String> untinted,Map<String,JsonObject> bones){}
  private static void check(boolean value,String message){checks++;if(!value)throw new AssertionError(message);}
  private static void near(float a,float b,float epsilon,String message){check(Float.isFinite(a)&&Math.abs(a-b)<=epsilon,message+" actual="+a+" expected="+b);}
  private static JsonObject json(Path p){try{return JsonParser.parseString(Files.readString(p)).getAsJsonObject();}catch(Exception e){throw new IllegalStateException(p.toString(),e);}}
  private static ChaoMeshAsset rigAsset(String key){return RIGS.computeIfAbsent(key,k->ChaoMeshAsset.parse(json(ROOT.resolve("models/entity/"+k+".json"))));}
  private static Item item(String id){return EQUIPMENT.computeIfAbsent(id,k->{
    var data=json(ROOT.resolve("models/entity/cwe_accessories/"+k+".json"));var untinted=new ArrayList<String>();
    if(data.has("accessory_untinted_bones"))for(var e:data.getAsJsonArray("accessory_untinted_bones"))untinted.add(e.getAsString());
    var bones=new HashMap<String,JsonObject>();for(var e:data.getAsJsonArray("bones")){var b=e.getAsJsonObject();bones.put(b.get("name").getAsString(),b);}
    return new Item(data,ChaoMeshAsset.parse(data),ChaoAccessoryFit.Source.read(data),untinted,bones);
  });}
  private static float f(com.google.gson.JsonArray a,int i){return a.get(i).getAsFloat();}
  private static Vector3f vector(com.google.gson.JsonArray a){return new Vector3f(f(a,0),f(a,1),f(a,2));}
  private static Matrix4f chain(Item item,String name,Map<String,ChaoMeshAsset.Pose> poses){
    var path=new ArrayList<JsonObject>();
    for(String n=name;n!=null;){var b=item.bones.get(n);path.add(b);n=b.has("parent")?b.get("parent").getAsString():null;}
    var result=new Matrix4f();
    for(int n=path.size()-1;n>=0;n--){var b=path.get(n);var p=poses.get(b.get("name").getAsString());var v=vector(b.getAsJsonArray("pivot"));var r=vector(b.getAsJsonArray("rotation")).mul((float)Math.PI/180);
      result.translate(v.x+p.x,v.y+p.y,v.z+p.z).rotateZ(r.z+p.rz).rotateY(r.y+p.ry).rotateX(r.x+p.rx).scale(p.sx,p.sy,p.sz);}
    return result;
  }
  private static final class Capture implements VertexConsumer {
    final List<Vector3f> points=new ArrayList<>();final List<Integer> colors=new ArrayList<>();
    public VertexConsumer addVertex(float x,float y,float z){points.add(new Vector3f(x,y,z).mul(16));vertices++;return this;}
    public VertexConsumer setColor(int r,int g,int b,int a){colors.add((a<<24)|(r<<16)|(g<<8)|b);return this;}
    public VertexConsumer setUv(float u,float v){check(Float.isFinite(u)&&Float.isFinite(v),"Finite source UVs");return this;}
    public VertexConsumer setUv1(int a,int b){return this;}
    public VertexConsumer setUv2(int a,int b){return this;}
    public VertexConsumer setNormal(float x,float y,float z){check(Float.isFinite(x)&&Float.isFinite(y)&&Float.isFinite(z),"Finite fitted source normals");return this;}
  }
  private static Capture emit(Item item,Map<String,ChaoMeshAsset.Pose> poses,ChaoAccessoryFit.Fit fit){
    var stack=new PoseStack();fit.apply(stack);var out=new Capture();
    CweAccessoryLayer.renderMesh(item.mesh,poses,item.untinted,stack,out,0xf000f0,0xff4678ca);return out;
  }
  /** Intersect actual emitted triangles along a cardinal ray. Roof/brim bounds
   * cannot substitute for a wall at the sampled forehead height. */
  private static float outerRay(Capture emitted,int axis,float u,float v,boolean minimum){
    int ua=(axis+1)%3,va=(axis+2)%3;float result=minimum?Float.POSITIVE_INFINITY:Float.NEGATIVE_INFINITY;
    for(int n=0;n<emitted.points.size();n+=4)for(int triangle=0;triangle<2;triangle++){
      var a=emitted.points.get(n);var b=emitted.points.get(n+(triangle==0?1:2));var c=emitted.points.get(n+(triangle==0?2:3));
      float bu=b.get(ua)-a.get(ua),bv=b.get(va)-a.get(va),cu=c.get(ua)-a.get(ua),cv=c.get(va)-a.get(va);
      float denominator=bu*cv-bv*cu;if(Math.abs(denominator)<.000001f)continue;
      float pu=u-a.get(ua),pv=v-a.get(va),wb=(pu*cv-pv*cu)/denominator,wc=(bu*pv-bv*pu)/denominator,wa=1-wb-wc;
      if(wa<-.00001f||wb<-.00001f||wc<-.00001f)continue;
      float hit=wa*a.get(axis)+wb*b.get(axis)+wc*c.get(axis);result=minimum?Math.min(result,hit):Math.max(result,hit);
    }return result;
  }
  private static void crownWalls(String id,Capture emitted,float[] head,String context){
    check(emitted.points.size()%4==0,"Actual crown emitter supplies complete faces "+id);
    float width=(head[3]-head[0])/7,height=(head[4]-head[1])/6,depth=(head[5]-head[2])/6;
    float centerX=(head[0]+head[3])*.5f,centerZ=(head[2]+head[5])*.5f;
    // All samples lie below the roof and above both hats' brims. Edge samples
    // catch a paper/logo panel concealing an otherwise buried front wall.
    for(float y:new float[]{head[1]+.35f*height,head[1]+1.35f*height,head[1]+2.34f*height}){
      for(float x:new float[]{-3.1f,-1.55f,0,1.55f,3.1f}){
        float hit=outerRay(emitted,2,centerX+x*width,y,true),clearance=head[2]-hit;
        check(Float.isFinite(hit)&&clearance>=.05f-.001f,"Actual forehead crown wall must clear skin by0.05px "+id+" "+context+" x="+x+" y="+y+" hitZ="+hit);
        minCrownClearance=Math.min(minCrownClearance,clearance);crownRays++;
      }
      for(float z:new float[]{-2.1f,0,2.1f})for(boolean left:new boolean[]{true,false}){
        float hit=outerRay(emitted,0,y,centerZ+z*depth,left),clearance=left?head[0]-hit:hit-head[3];
        check(Float.isFinite(hit)&&clearance>=.05f-.001f,"Actual side crown wall must clear skin by0.05px "+id+" "+context+" y="+y+" z="+z+" hitX="+hit);
        minCrownClearance=Math.min(minCrownClearance,clearance);crownRays++;
      }
    }
  }
  private static List<Vector3f[]> triangles(Item item,Map<String,ChaoMeshAsset.Pose> poses,ChaoAccessoryFit.Fit fit,String onlyBone,String onlyMesh){
    var out=new ArrayList<Vector3f[]>();
    for(var entry:item.bones.entrySet()){
      if(onlyBone!=null&&!onlyBone.equals(entry.getKey()))continue;var bone=entry.getValue();if(!bone.has("meshes"))continue;
      Matrix4f transform=new Matrix4f().translate(fit.x(),fit.y(),fit.z()).scale(fit.sx(),fit.sy(),fit.sz()).mul(chain(item,entry.getKey(),poses));
      for(var element:bone.getAsJsonArray("meshes")){var mesh=element.getAsJsonObject();if(onlyMesh!=null&&!onlyMesh.equals(mesh.get("name").getAsString()))continue;
        for(var faceElement:mesh.getAsJsonArray("faces")){var face=faceElement.getAsJsonObject().getAsJsonArray("v");
          for(int n=1;n<face.size()-1;n++){var triangle=new Vector3f[3];int[] ids={face.get(0).getAsInt(),face.get(n).getAsInt(),face.get(n+1).getAsInt()};
            for(int a=0;a<3;a++)triangle[a]=transform.transformPosition(vector(mesh.getAsJsonArray("vertices").get(ids[a]).getAsJsonArray()));out.add(triangle);}
        }
      }
    }return out;
  }
  private static float[] bounds(List<Vector3f[]> triangles){
    float[] b={Float.POSITIVE_INFINITY,Float.POSITIVE_INFINITY,Float.POSITIVE_INFINITY,Float.NEGATIVE_INFINITY,Float.NEGATIVE_INFINITY,Float.NEGATIVE_INFINITY};
    for(var t:triangles)for(var p:t)for(int a=0;a<3;a++){b[a]=Math.min(b[a],p.get(a));b[a+3]=Math.max(b[a+3],p.get(a));}return b;
  }
  private static List<Vector3f> clip(List<Vector3f> input,int axis,float plane,boolean positive){
    var out=new ArrayList<Vector3f>();if(input.isEmpty())return out;
    for(int n=0;n<input.size();n++){
      var a=input.get(n);var b=input.get((n+1)%input.size());boolean ai=positive?a.get(axis)>=plane:a.get(axis)<=plane,bi=positive?b.get(axis)>=plane:b.get(axis)<=plane;
      if(ai)out.add(a);if(ai!=bi)out.add(new Vector3f(a).lerp(b,(plane-a.get(axis))/(b.get(axis)-a.get(axis))));
    }return out;
  }
  private static boolean enters(Vector3f[] triangle,float[] box,float inset){
    List<Vector3f> polygon=new ArrayList<>(List.of(triangle));
    for(int a=0;a<3;a++){polygon=clip(polygon,a,box[a]+inset,true);polygon=clip(polygon,a,box[a+3]-inset,false);}return !polygon.isEmpty();
  }
  private static void glasses(String id,Item item,float[] head,float[] eyes,float[] body,float[] mouth,String context){
    check(item.source!=null&&item.source.eyes()!=null,"Spectacles require measured coherent source profile: "+id);
    var fit=ChaoAccessoryFit.accessory(id,head,eyes,body,null,mouth,item.source);var poses=item.mesh.createPoses();ChaoAccessoryFit.parts(id,head,fit,poses,item.source);
    float[] left=bounds(triangles(item,poses,fit,"accessory","frame_left")),right=bounds(triangles(item,poses,fit,"accessory","frame_right"));
    near((left[0]+left[3])*.5f,eyes[0]-eyes[3],.002f,"Complete left frame iris center "+context);
    near((right[0]+right[3])*.5f,eyes[0]+eyes[3],.002f,"Complete right frame iris center "+context);
    near((left[1]+left[4])*.5f,eyes[1],.002f,"Source frame eye height "+context);
    for(var triangle:triangles(item,poses,fit,null,null))check(!enters(triangle,head,.015f),"Spectacle source surface intersects actual head/cover "+id+" "+context);
    var front=triangles(item,poses,fit,"accessory",null);float back=bounds(front)[5];
    float gap=Math.min(head[2],eyes[2])-back;minLensClearance=Math.min(minLensClearance,gap);
    check(gap>=ChaoAccessoryFit.FACE_CLEARANCE-.002f,"All lens/frame/bridge backs clear face plane "+context);
    for(var temple:item.source.temples()){
      var root=poses.get(temple.hinge());var arm=poses.get(temple.bone());check(root.sx>0&&root.sz>0&&arm.sx>0&&arm.sz>0,"Positive source hinge scales "+context);
      var matrix=chain(item,temple.strip(),poses);var rear=chain(item,temple.bone(),poses);
      // The complete cut cross-section stays joined, not merely one numerical pivot.
      var strip=item.bones.get(temple.strip());float length=temple.join()[2]-temple.inner()[2];
      for(var mesh:strip.getAsJsonArray("meshes"))for(var value:mesh.getAsJsonObject().getAsJsonArray("vertices")){
        var point=vector(value.getAsJsonArray());var actual=matrix.transformPosition(new Vector3f(point));
        if(Math.abs(point.z)<.00001f){var expected=new Vector3f(point).add(temple.inner()[0],temple.inner()[1],temple.inner()[2]);check(actual.distance(expected)<.0002f,"Exact source frame/hinge seam "+context);seams++;}
        if(Math.abs(point.z-length)<.00001f){var expected=rear.transformPosition(new Vector3f(point.x,point.y,0));check(actual.distance(expected)<.0002f,"Exact source hinge/rear-arm seam "+context);seams++;}
      }
      check(matrix.determinant()>0,"Hinge affine never reflects/flattens source connector "+context);
      near(rear.determinant(),1,.0003f,"Rear source arm preserves its full cross-section "+context);
      for(var triangle:triangles(item,poses,fit,temple.bone(),null)){
        List<Vector3f> polygon=new ArrayList<>(List.of(triangle));polygon=clip(polygon,2,head[2],true);polygon=clip(polygon,2,head[5],false);
        for(var point:polygon){float distance=temple.side()<0?head[0]-point.x:point.x-head[3];minTempleClearance=Math.min(minTempleClearance,distance);check(distance>=ChaoAccessoryFit.TEMPLE_CLEARANCE-.002f,"Actual inner temple surface clears cover "+context);}
      }
    }
  }
  private static void hoodPorts(Item item,Map<String,ChaoMeshAsset.Pose> poses,ChaoAccessoryFit.Fit fit,float[] eyes,String context){
    var surface=triangles(item,poses,fit,null,null);float scale=Math.min(1,eyes[3]*.5f);
    for(int side:new int[]{-1,1})for(int y=0;y<=14;y++)for(int x=0;x<=10;x++){
      // Full emitted native-sized iris envelope, including backing, with a small margin.
      float px=eyes[0]+side*eyes[3]+(x-5)*.2f*scale,py=eyes[1]+(y-7)*.15f*scale;
      float[] ray={px-.002f,py-.002f,-20,px+.002f,py+.002f,eyes[2]};
      for(var triangle:surface)check(!enters(triangle,ray,0),"Raised hood blocks visible iris panel "+context+" "+px+","+py);portRays++;
    }
  }
  private static ChaoState state(boolean adult,int alignment,int first,float rp,float sf,float strength){
    var state=new ChaoState();state.adult=adult;state.align=alignment;state.primaryType=first;
    int r=Math.round(Math.abs(rp)*(adult?960:480)),f=Math.round(Math.abs(sf)*(adult?960:480));
    int rs=rp<0?ChaoState.RUN:ChaoState.POWER,fs=sf<0?ChaoState.SWIM:ChaoState.FLY;
    state.levels[rs]=r/100;state.progress[rs]=r%100;state.points[rs]=r;state.levels[fs]=f/100;state.progress[fs]=f%100;state.points[fs]=f;
    state.setEvolutionStrength(strength*(adult?1.2f:.8f));state.setBaselineFace076(0,0);return state;
  }
  private static void inspect(JsonObject clips,ChaoState state,int serial){
    String saved=state.encode(),key=OfficialChaoRig.geometryKey(state),context="adult="+state.adult+" align="+state.align+" first="+state.primaryType+" #"+serial;
    var rig=new OfficialChaoRig(clips,WearableRigTest079::rigAsset);states++;
    for(var motion:List.of(OfficialChaoRig.Motion.WALK,OfficialChaoRig.Motion.CRAWL,OfficialChaoRig.Motion.RUN,OfficialChaoRig.Motion.SIT,OfficialChaoRig.Motion.HELD)){
      rig.prepareDevelopment(key,state);rig.animateGameplay(key,new UUID(790079,serial),motion,120,1.3f,.04f,0,0,31,-18,false,false);rig.development(state);frames++;
      var head=rig.attachmentBounds("skull");var body=rig.attachmentBounds("body");var eyes=rig.attachmentEyes();var mouth=rig.attachmentMouth();mouth=mouth.clone();mouth[1]+=ChaoExpressionLayer025.MOUTH_DROP076;
      near(head[3]-head[0],7,.0001f,"Original fixed head width");near(head[4]-head[1],6,.0001f,"Original fixed head height");near(head[5]-head[2],6,.0001f,"Original fixed head depth");
      for(String id:ITEMS){
        var item=item(id);String anchor=id.startsWith("hoodie_sleeve_")?(id.endsWith("left")?"arm_left":"arm_right"):
            id.contains("bowtie")||id.endsWith("hoodie_accessory")||id.startsWith("medal_")||id.equals("hoodie_hood_folded")?"body":"skull";
        var arm=anchor.startsWith("arm_")?rig.attachmentBounds(anchor):null;
        var fit=ChaoAccessoryFit.accessory(id,head,eyes,body,arm,mouth,item.source);var poses=item.mesh.createPoses();ChaoAccessoryFit.parts(id,head,fit,poses,item.source);
        var emitted=emit(item,poses,fit);check(!emitted.points.isEmpty(),"Equipped source mesh is visible "+id);fits++;
        if(id.equals("baseball_cap_accessory")||id.equals("press_hat_accessory"))crownWalls(id,emitted,head,context);
        for(var p:emitted.points)check(Float.isFinite(p.x)&&Float.isFinite(p.y)&&Float.isFinite(p.z),"Finite actual fitted source vertices");
        var independent=bounds(triangles(item,poses,fit,null,null));var actual=bounds(emitted.points.stream().map(p->new Vector3f[]{p}).toList());
        for(int a=0;a<6;a++)near(actual[a],independent[a],.0002f,"Actual renderer vs independent source bone chain "+id+" "+context);
        if(!item.untinted.isEmpty()){
          check(emitted.colors.stream().anyMatch(c->c==0xffffffff),"Source white trim survives garment dye "+id);
          check(emitted.colors.stream().anyMatch(c->c==0xff4678ca),"Source cloth still accepts dye "+id);
          check(poses.values().stream().allMatch(p->p.visible),"Trim render restores source visibility "+id);
        }
        if(motion==OfficialChaoRig.Motion.WALK&&id.equals("hoodie_hood"))hoodPorts(item,poses,fit,eyes,context);
        if(id.contains("pacifier")){
          near((actual[1]+actual[4])*.5f,mouth[1],.002f,"Pacifier follows actual visible mouth");
          near(actual[5],Math.min(head[2],mouth[2])+.08f,.002f,"Only source nipple seats intentionally inside mouth");
          check(actual[2]<mouth[2]-.8f,"Pacifier guard/ring visibly project from mouth");
        }
        // Apply the complete moving sampled anchor to a real production-emitted point.
        var stack=new PoseStack();rig.transformBone(anchor,stack);var matrix=new Matrix4f(stack.last().pose());
        var local=emitted.points.get(0);var expected=matrix.transformPosition(new Vector3f(local).div(16));
        var moving=new Capture();fit.apply(stack);CweAccessoryLayer.renderMesh(item.mesh,poses,item.untinted,stack,moving,0xf000f0,0xff4678ca);
        check(moving.points.get(0).distance(expected.mul(16))<.001f,"Moving source attachment retains full sampled ancestor chain "+id);
      }
      if(motion==OfficialChaoRig.Motion.WALK){
        var ports=ChaoHeadCover079.eyePorts(head,eyes);float halfWidth=Math.min(1,eyes[3]*.5f),halfHeight=1.05f*halfWidth;
        check(ports.left0()<=eyes[0]-eyes[3]-halfWidth&&ports.left1()>=eyes[0]-eyes[3]+halfWidth,"Shared left mask port exposes whole iris");
        check(ports.right0()<=eyes[0]+eyes[3]-halfWidth&&ports.right1()>=eyes[0]+eyes[3]+halfWidth,"Shared right mask port exposes whole iris");
        check(ports.top()<=eyes[1]-halfHeight&&ports.bottom()>=eyes[1]+halfHeight,"Shared mask ports expose full iris height");
        var shell=EggshellMask.fittedBounds(head,eyes);var shellVertices=new EggshellMask().inspectGeometry(head,eyes);
        for(int n=0;n<shellVertices.length;n+=8)for(int a=0;a<3;a++)check(shellVertices[n+a]>=shell[a]-.0001f&&shellVertices[n+a]<=shell[a+3]+.0001f,"Eggshell attachment surface includes every actual source vertex");
        for(String id:List.of("round_glasses_accessory","aviators_accessory"))for(float[] surface:List.of(head,ChaoAccessoryFit.coveredHead(head),ChaoAccessoryFit.hoodedHead(head),shell))glasses(id,item(id),surface,eyes,body,mouth,context);
        var clothed=ChaoAccessoryFit.clothedBody(body);
        for(String id:List.of("bowtie_accessory","cheese_bowtie_accessory","medal_race","medal_karate")){
          var equipment=item(id);var fit=ChaoAccessoryFit.accessory(id,head,eyes,clothed,null,mouth,equipment.source);var poses=equipment.mesh.createPoses();
          var front=bounds(triangles(equipment,poses,fit,null,null));check(front[5]<clothed[2]-.025f,"Neckwear/medal remains outside hoodie cloth/trim "+id);
        }
      }
      check(state.encode().equals(saved),"Equipment presentation preserves saved care/genetics/items "+context);
    }
  }
  public static void main(String[] args){
    var clips=json(ROOT.resolve("animations/official/chao.animation.json")).getAsJsonObject("animations");int serial=0;
    for(boolean adult:new boolean[]{false,true})for(int alignment=-1;alignment<=1;alignment++)for(int first=0;first<=(adult?4:0);first++)
      for(float strength:new float[]{0,.5f,1})for(int sf=-1;sf<=1;sf++)for(int rp=-1;rp<=1;rp++)inspect(clips,state(adult,alignment,first,rp,sf,strength),serial++);
    System.out.println("Wearable rig079: "+checks+" checks, "+states+" selected baby/adult states, "+frames+" moving frames, "+fits+" equipment fits, "+vertices+" actual emitted vertices, "+seams+" exact source seam vertices, "+portRays+" full-iris hood rays, "+crownRays+" emitted forehead/side crown rays; minimum front clearance="+minLensClearance+"px, temple clearance="+minTempleClearance+"px, crown clearance="+minCrownClearance+"px. All3alignments/all5adult firsttypes/nine development endpoints/three strengths, bare-head+full-mask+raised-hood spectacles, coherent source frames and positive hinge matrices verified numerically. Client visual review is separate.");
  }
}
