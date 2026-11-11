package com.chaogarden.client;
import java.nio.file.*;
import java.util.*;
import org.joml.Matrix4f;
import org.joml.Vector3f;
/** Triangle/box separating-axis checks; a tilted round disc's enclosing box is not its surface. */
final class Toy02535PoseVisualSmoke {
  static List<Vector3f[]> triangles(String part)throws Exception{
    String json;
    try(var input=Toy02535PoseVisualSmoke.class.getResourceAsStream("/assets/chao_garden/models/item/chao_triangle_hand_"+part+".json")){
      json=new String(Objects.requireNonNull(input,"triangle model resource").readAllBytes(),java.nio.charset.StandardCharsets.UTF_8);
    }
    var obj=com.google.gson.JsonParser.parseString(json).getAsJsonObject();
    var out=new ArrayList<Vector3f[]>();
    for(var el:obj.getAsJsonArray("elements")){
      var e=el.getAsJsonObject();float[] lo=new float[3],hi=new float[3];for(int a=0;a<3;a++){lo[a]=e.getAsJsonArray("from").get(a).getAsFloat();hi[a]=e.getAsJsonArray("to").get(a).getAsFloat();}
      var transform=new Matrix4f();if(e.has("rotation")){var rot=e.getAsJsonObject("rotation");var at=rot.getAsJsonArray("origin");var origin=new Vector3f(at.get(0).getAsFloat(),at.get(1).getAsFloat(),at.get(2).getAsFloat());float angle=(float)Math.toRadians(rot.get("angle").getAsFloat());transform.translate(origin);switch(rot.get("axis").getAsString()){case "x"->transform.rotateX(angle);case "y"->transform.rotateY(angle);default->transform.rotateZ(angle);}transform.translate(new Vector3f(origin).negate());}
      var v=new Vector3f[8];for(int a=0;a<8;a++)v[a]=transform.transformPosition(new Vector3f((a&1)==0?lo[0]:hi[0],(a&2)==0?lo[1]:hi[1],(a&4)==0?lo[2]:hi[2])).div(16).sub(.5f,.5f,.5f);
      for(int[] f:new int[][]{{0,1,3},{0,3,2},{4,6,7},{4,7,5},{0,4,5},{0,5,1},{2,3,7},{2,7,6},{0,2,6},{0,6,4},{1,5,7},{1,7,3}})out.add(new Vector3f[]{v[f[0]],v[f[1]],v[f[2]]});
    }return out;
  }
  static float separation(List<Vector3f[]> faces,Matrix4f transform,float[] box){
    var center=new Vector3f(box[0]+box[3],box[1]+box[4],box[2]+box[5]).mul(.5f);
    var half=new Vector3f(box[3]-box[0],box[4]-box[1],box[5]-box[2]).mul(.5f);float result=Float.POSITIVE_INFINITY;
    for(var f:faces){
      var a=transform.transformPosition(new Vector3f(f[0])).mul(16).sub(center);
      var b=transform.transformPosition(new Vector3f(f[1])).mul(16).sub(center);
      var c=transform.transformPosition(new Vector3f(f[2])).mul(16).sub(center);
      var ab=new Vector3f(b).sub(a);var bc=new Vector3f(c).sub(b);var ca=new Vector3f(a).sub(c);
      float gap=Math.max(axis(new Vector3f(1,0,0),a,b,c,half),Math.max(axis(new Vector3f(0,1,0),a,b,c,half),axis(new Vector3f(0,0,1),a,b,c,half)));
      gap=Math.max(gap,axis(new Vector3f(ab).cross(bc),a,b,c,half));
      for(var edge:new Vector3f[]{ab,bc,ca})for(var basis:new Vector3f[]{new Vector3f(1,0,0),new Vector3f(0,1,0),new Vector3f(0,0,1)})gap=Math.max(gap,axis(new Vector3f(edge).cross(basis),a,b,c,half));
      result=Math.min(result,gap);
    }return result;
  }
  private static float axis(Vector3f n,Vector3f a,Vector3f b,Vector3f c,Vector3f half){
    if(n.lengthSquared()<1e-12)return Float.NEGATIVE_INFINITY;n.normalize();
    float r=Math.abs(n.x)*half.x+Math.abs(n.y)*half.y+Math.abs(n.z)*half.z;
    float pa=n.dot(a),pb=n.dot(b),pc=n.dot(c);return Math.max(Math.min(pa,Math.min(pb,pc))-r,-r-Math.max(pa,Math.max(pb,pc)));
  }
  private record HandAsset(ChaoMeshAsset mesh,Map<String,ChaoMeshAsset.Pose> poses){}
  private static final Map<String,HandAsset> HANDS=new HashMap<>();
  static float[] handBox(com.chaogarden.core.ChaoState state,boolean left){return handBox(state,left,0);}
  static float[] handBox(com.chaogarden.core.ChaoState state,boolean left,float reach){
    String key=OfficialChaoRig.geometryKey(state);int animal=ChaoToyGrip.animalArm(state);
    String path=animal<0?"gameplay/chao_"+key+(key.startsWith("chaos_")?"_glass":""):(animal<21?"overlays/":"cwe_parts/")+"chao_"+OfficialChaoRig.sourceGeometryKey(key);
    var asset=HANDS.computeIfAbsent(path,p->{var mesh=ChaoMeshAsset.load(p);return new HandAsset(mesh,mesh.createPoses());});
    if(animal>=0){ChaoToyGrip.animalPalm(asset.mesh,asset.poses,animal,state.adult,left);asset.poses.get(left?"arm_left":"arm_right").reach=reach;return asset.mesh.animalHandBounds(animal,asset.poses,left);}
    boolean morph=!state.immortal&&(state.character==null||state.character.equals("normal"));
    asset.mesh.development(morph?state.swimFly:0,morph?state.runPower:0,morph?state.gradualVisualStrength()*(state.adult?1:.14f):0);
    asset.poses.get(left?"arm_left":"arm_right").reach=reach;
    var box=asset.mesh.visibleHandBounds(asset.poses,left);
    return box;
  }
  static List<Vector3f[]> boxTriangles(float[] box){
    var v=new Vector3f[8];for(int i=0;i<8;i++)v[i]=new Vector3f(box[(i&1)==0?0:3],box[(i&2)==0?1:4],box[(i&4)==0?2:5]).div(16);
    var out=new ArrayList<Vector3f[]>();for(int[] f:new int[][]{{0,1,3},{0,3,2},{4,6,7},{4,7,5},{0,4,5},{0,5,1},{2,3,7},{2,7,6},{0,2,6},{0,6,4},{1,5,7},{1,7,3}})out.add(new Vector3f[]{v[f[0]],v[f[1]],v[f[2]]});return out;
  }
  // A Chaos head is a faceted mesh. Its enclosing box contains empty corners;
  // refine overlaps against the actual rendered skin before declaring clipping.
  static Object field(Object o,String n)throws Exception{var f=o.getClass().getDeclaredField(n);f.setAccessible(true);return f.get(o);}
  static List<Vector3f[]> headSurface(com.chaogarden.core.ChaoState state,Map<String,ChaoMeshAsset.Pose> poses){
    try{
      String path="gameplay/chao_"+OfficialChaoRig.geometryKey(state);
      var asset=HANDS.computeIfAbsent(path,p->{var mesh=ChaoMeshAsset.load(p);return new HandAsset(mesh,mesh.createPoses());});
      boolean morph=!state.immortal&&(state.character==null||state.character.equals("normal"));
      asset.mesh.development(morph?state.swimFly:0,morph?state.runPower:0,morph?state.gradualVisualStrength()*(state.adult?1:.14f):0);
      return headSurface(asset.mesh,poses);
    }catch(Exception e){throw new RuntimeException(e);}
  }
  static List<Vector3f[]> headSurface(ChaoMeshAsset assetMesh,Map<String,ChaoMeshAsset.Pose> poses){
    try{
      var skull=new com.mojang.blaze3d.vertex.PoseStack();assetMesh.transformBone("skull",skull,poses);var inverse=new Matrix4f(skull.last().pose()).invert();
      var out=new ArrayList<Vector3f[]>();
      for(var entry:((Map<String,Object>)field(assetMesh,"bones")).entrySet()){
        String name=entry.getKey();if(!name.startsWith("skull__cube_")||!poses.get(name).visible)continue;
        var stack=new com.mojang.blaze3d.vertex.PoseStack();assetMesh.transformBone(name,stack,poses);var transform=new Matrix4f(inverse).mul(stack.last().pose());
        for(Object mesh:(List<?>)field(entry.getValue(),"meshes")){
          var coordinate=ChaoMeshAsset.class.getDeclaredMethod("coordinate",mesh.getClass(),int.class,int.class);coordinate.setAccessible(true);
          for(Object face:(List<?>)field(mesh,"faces")){
            int[] indices=(int[])field(face,"indices");var vertices=new Vector3f[indices.length];
            for(int i=0;i<indices.length;i++)vertices[i]=transform.transformPosition(new Vector3f((float)coordinate.invoke(assetMesh,mesh,indices[i],0),(float)coordinate.invoke(assetMesh,mesh,indices[i],1),(float)coordinate.invoke(assetMesh,mesh,indices[i],2)).div(16));
            for(int i=2;i<vertices.length;i++)out.add(new Vector3f[]{vertices[0],vertices[i-1],vertices[i]});
          }
        }
      }
      if(out.isEmpty())throw new IllegalStateException("missing head surface");return out;
    }catch(Exception e){throw new RuntimeException(e);}
  }

  static int refined;
  static int samples,failures,handSamples;static float handHead=Float.POSITIVE_INFINITY;static float head=Float.POSITIVE_INFINITY,ground=Float.POSITIVE_INFINITY;
  static final List<String> bad=new ArrayList<>();static final Map<String,Integer> groups=new TreeMap<>();static List<Vector3f[]> castBase,castLid,cym;
  static void observe(com.chaogarden.entity.ChaoEntity c,ChaoModel model){
    if(!ChaoTriangleAnimation.active(c))return;
    if(cym==null)try{cym=triangles("body");castBase=triangles("beater");}catch(Exception e){throw new RuntimeException(e);}
    float partial=net.minecraft.client.Minecraft.getInstance().getTimer().getGameTimeDeltaPartialTick(true);
    float w=com.chaogarden.toys.InstrumentPerformance.presence(c,partial),phase=com.chaogarden.toys.InstrumentPerformance.phase(c,partial);

    var skull=new com.mojang.blaze3d.vertex.PoseStack();model.transformAccessoryAnchor("skull",skull);var inv=new Matrix4f(skull.last().pose()).invert();
    for(boolean left:new boolean[]{false,true}){
      var box=handBox(c.state(),left,model.officialPoses().get(left?"arm_left":"arm_right").reach);if(box==null)continue;
      var arm=new com.mojang.blaze3d.vertex.PoseStack();model.transformAccessoryAnchor(left?"arm_left":"arm_right",arm);
      float gap=separation(boxTriangles(box),new Matrix4f(inv).mul(arm.last().pose()),model.accessoryBounds("skull"));
      if(gap<-.025f){refined++;gap=separation(headSurface(c.state(),model.officialPoses()),new Matrix4f(arm.last().pose()).invert().mul(skull.last().pose()),box);}
      handSamples++;handHead=Math.min(handHead,gap);
      if(gap<-.025f){failures++;groups.merge("hand "+c.getName().getString(),1,Integer::sum);if(bad.size()<40)bad.add("hand "+c.getName().getString()+" left="+left+" phase="+phase+" gap="+gap);}
    }
    if(ChaoTriangleAnimation.propWeight(w)<=0)return;
    for(int i=0;i<2;i++){
      var faces=i==0?cym:castBase;float tap=model.triangleTap(phase);var prop=ChaoTriangleAnimation.triangleMatrix(model::transformAccessoryAnchor,model.toyPalm(true),w,tap);if(i==1)prop=ChaoTriangleAnimation.beaterMatrix(model::transformAccessoryAnchor,model.toyPalm(false),prop,w,tap);float gap=separation(faces,new Matrix4f(inv).mul(prop),model.accessoryBounds("skull"));
      float floor=Float.POSITIVE_INFINITY;for(var f:faces)for(var v:f)floor=Math.min(floor,24-prop.transformPosition(new Vector3f(v)).y*16);
      head=Math.min(head,gap);ground=Math.min(ground,floor);samples++;
      if(gap<-.025f||floor<-.025f){failures++;groups.merge(c.getName().getString()+" "+c.toyPlayingKind(),1,Integer::sum);if(bad.size()<40)bad.add(c.getName().getString()+" "+c.toyPlayingKind()+" phase="+phase+" head="+gap+" floor="+floor+" part="+i);}
    }
  }
  static void finish(Path dir){
    String result="Actual client pose samples="+samples+" failures="+failures+" minHeadPixels="+head+" minGroundPixels="+ground+" narrowPhaseChecks="+refined+" handSamples="+handSamples+" minHandHeadPixels="+handHead+"\n"+"groups="+groups+"\n"+String.join("\n",bad)+"\n";
    try{Files.createDirectories(dir);Files.writeString(dir.resolve("live-geometry.txt"),result);}catch(Exception e){throw new RuntimeException(e);}
    com.mojang.logging.LogUtils.getLogger().info("TOY02535_LIVE_GEOMETRY {}",result);
    if(failures>0)throw new IllegalStateException(result);
  }
}
