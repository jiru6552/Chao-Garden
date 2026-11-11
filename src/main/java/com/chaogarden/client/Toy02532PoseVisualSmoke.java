package com.chaogarden.client;
import java.nio.file.*;
import java.util.*;
import org.joml.Matrix4f;
import org.joml.Vector3f;
/** Triangle/box separating-axis checks; a tilted round disc's enclosing box is not its surface. */
final class Toy02532PoseVisualSmoke {
  static List<Vector3f[]> triangles(String name)throws Exception{
    var v=new ArrayList<Vector3f>();var faces=new ArrayList<Vector3f[]>();
    for(String line:Files.readAllLines(Path.of("/tmp/chao-flute/source/src/main/resources/assets/chao_garden/models/sa2_02515/"+name+".obj"))){
      var a=line.split("\\s+");
      if(line.startsWith("v "))v.add(new Vector3f(Float.parseFloat(a[1])-.5f,Float.parseFloat(a[2])-.5f,Float.parseFloat(a[3])-.5f));
      else if(line.startsWith("f "))for(int i=2;i<a.length-1;i++)faces.add(new Vector3f[]{v.get(Integer.parseInt(a[1].split("/")[0])-1),v.get(Integer.parseInt(a[i].split("/")[0])-1),v.get(Integer.parseInt(a[i+1].split("/")[0])-1)});
    }return faces;
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
  static int samples,failures;static float head=Float.POSITIVE_INFINITY,ground=Float.POSITIVE_INFINITY;
  static final List<String> bad=new ArrayList<>();static final Map<String,Integer> groups=new TreeMap<>();static List<Vector3f[]> castBase,castLid,cym;
  static void observe(com.chaogarden.entity.ChaoEntity c,ChaoModel model){
    if(!ChaoFluteAnimation.active(c))return;
    if(cym==null)try{cym=triangles("chao_flute");}catch(Exception e){throw new RuntimeException(e);}
    float partial=net.minecraft.client.Minecraft.getInstance().getTimer().getGameTimeDeltaPartialTick(true);
    float w=com.chaogarden.toys.InstrumentPerformance.presence(c,partial),phase=com.chaogarden.toys.InstrumentPerformance.phase(c,partial);
    if(ChaoFluteAnimation.propWeight(w)<=0)return;
    var skull=new com.mojang.blaze3d.vertex.PoseStack();model.transformAccessoryAnchor("skull",skull);var inv=new Matrix4f(skull.last().pose()).invert();
    for(int i=0;i<1;i++){
      var faces=cym;var prop=ChaoFluteAnimation.propMatrix(model::transformAccessoryAnchor,model.toyMouth(),w,model.fluteLean(phase));float gap=separation(faces,new Matrix4f(inv).mul(prop),model.accessoryBounds("skull"));
      float floor=Float.POSITIVE_INFINITY;for(var f:faces)for(var v:f)floor=Math.min(floor,24-prop.transformPosition(new Vector3f(v)).y*16);
      head=Math.min(head,gap);ground=Math.min(ground,floor);samples++;
      if(gap<-.025f||floor<-.025f){failures++;groups.merge(c.getName().getString()+" "+c.toyPlayingKind(),1,Integer::sum);if(bad.size()<40)bad.add(c.getName().getString()+" "+c.toyPlayingKind()+" phase="+phase+" head="+gap+" floor="+floor+" part="+i);}
    }
  }
  static void finish(Path dir){
    String result="Actual client pose samples="+samples+" failures="+failures+" minHeadPixels="+head+" minGroundPixels="+ground+"\n"+"groups="+groups+"\n"+String.join("\n",bad)+"\n";
    try{Files.createDirectories(dir);Files.writeString(dir.resolve("live-geometry.txt"),result);}catch(Exception e){throw new RuntimeException(e);}
    com.mojang.logging.LogUtils.getLogger().info("TOY02532_LIVE_GEOMETRY {}",result);
    if(failures>0)throw new IllegalStateException(result);
  }
}
