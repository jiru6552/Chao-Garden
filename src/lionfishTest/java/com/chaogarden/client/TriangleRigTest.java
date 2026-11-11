package com.chaogarden.client;
import com.google.gson.*;
import com.mojang.blaze3d.vertex.PoseStack;
import com.chaogarden.core.ChaoState;
import com.chaogarden.toys.TrianglePlayRules;
import java.nio.file.*;
import java.util.*;
import org.joml.Matrix4f;
import org.joml.Vector3f;
/** Independent surface and landmark checks for both props, both hands and every authored strike. */
public final class TriangleRigTest {
 record Hands(float[] right,float[] left,String label,float[] rightBox,float[] leftBox,ChaoMeshAsset asset,Map<String,ChaoMeshAsset.Pose> poses,int animal){}
 static int samples,failures,headChecks;static float head=Float.POSITIVE_INFINITY,ground=Float.POSITIVE_INFINITY,shoulder,tipGap,handHead=Float.POSITIVE_INFINITY,reach;
 static final List<String> bad=new ArrayList<>();static final Map<String,Integer> reasons=new TreeMap<>();
 static void check(boolean ok,String label){if(!ok){reasons.merge(label.split(" ")[0],1,Integer::sum);failures++;if(bad.size()<30)bad.add(label);}}
 public static void main(String[] args)throws Exception{
  var clip=new LionfishTriangleClip();var bodyFaces=Toy02535PoseVisualSmoke.triangles("body");var stickFaces=Toy02535PoseVisualSmoke.triangles("beater");
  int beats=0;for(int t=0;t<=160;t++){float tap=clip.tap(t);check(Float.isFinite(tap),"finite clip");if(TrianglePlayRules.soundDue(t)){beats++;check(tap>.9999f,"sound coincides with impact "+t);}}check(beats==8,"eight separated taps");
    try(var files=Files.list(Path.of("src/main/resources/assets/chao_garden/models/entity/gameplay"))){for(var path:files.filter(p->p.toString().endsWith(".json")&&!p.toString().contains("_glass")).toList()){
      var mesh=ChaoMeshAsset.parse(JsonParser.parseString(Files.readString(path)).getAsJsonObject());var poses=mesh.createPoses();
      for(boolean adult:new boolean[]{false,true})for(float sf:new float[]{-1,0,1})for(float rp:new float[]{-1,0,1}){
        mesh.development(sf,rp,adult?1:.14f);poses.values().forEach(ChaoMeshAsset.Pose::reset);var hands=new ArrayList<Hands>();
        var handMesh=path.getFileName().toString().contains("chaos_")?ChaoMeshAsset.parse(JsonParser.parseString(Files.readString(path.resolveSibling(path.getFileName().toString().replace(".json","_glass.json")))).getAsJsonObject()):mesh;
        var handPoses=handMesh==mesh?poses:handMesh.createPoses();
        hands.add(new Hands(handMesh.visiblePalm(handPoses,false),handMesh.visiblePalm(handPoses,true),"skin",handMesh.visibleHandBounds(handPoses,false),handMesh.visibleHandBounds(handPoses,true),handMesh,handPoses,-1));
        String key=path.getFileName().toString();if(sf==0&&rp==0&&!key.contains("chaos_")&&!key.contains("omochao"))for(int animal=0;animal<ChaoState.ANIMALS.length;animal++){
          if((ChaoState.animalSlots(animal)&1)==0)continue;var p=path.getParent().getParent().resolve((animal<21?"overlays/":"cwe_parts/")+key);if(!Files.exists(p))continue;
          var repl=ChaoMeshAsset.parse(JsonParser.parseString(Files.readString(p)).getAsJsonObject());var ps=repl.createPoses();var r=ChaoToyGrip.animalPalm(repl,ps,animal,adult,false);var l=ChaoToyGrip.animalPalm(repl,ps,animal,adult,true);if(r!=null&&l!=null)hands.add(new Hands(r,l,"animal"+animal,repl.animalHandBounds(animal,ps,false),repl.animalHandBounds(animal,ps,true),repl,ps,animal));
        }

        for(var h:hands)for(int q=1;q<640;q+=Boolean.getBoolean("chao.posture.quick")?32:1){
          float t=q*.25f,w=TrianglePlayRules.presence(t),tap=clip.tap(t);
          for(float pitch:w>=1?new float[]{0}:new float[]{-.25f,0,.25f})for(float yaw:w>=1?new float[]{0}:new float[]{-.65f,0,.65f}){
            poses.values().forEach(ChaoMeshAsset.Pose::reset);poses.get("body").y=.8f;poses.get("body").rx=-.08f;poses.get("skull").y=.8f;poses.get("skull").rx=.025f+pitch;poses.get("skull").ry=yaw;
            java.util.function.BiConsumer<String,PoseStack> anchor=(n,s)->mesh.transformBone(n,s,poses);
            float hx=poses.get("skull").x,hy=poses.get("skull").y,hz=poses.get("skull").z;
            ChaoTriangleAnimation.pose(poses,anchor,h.right,h.left,w,tap);samples++;
            check(poses.get("skull").x==hx&&poses.get("skull").y==hy&&poses.get("skull").z==hz,"headJoint unchanged");
            String label=key+" adult="+adult+" morph="+sf+","+rp+" "+h.label+" t="+t;
            var leftPalm=ChaoTrumpetAnimation.reachedPalm(h.left,poses.get("arm_left").reach,true);var rightPalm=ChaoTrumpetAnimation.reachedPalm(h.right,poses.get("arm_right").reach,false);
            var triangle=ChaoTriangleAnimation.triangleMatrix(anchor,leftPalm,w,tap);var stick=ChaoTriangleAnimation.beaterMatrix(anchor,rightPalm,triangle,w,tap);
            var skull=new PoseStack();anchor.accept("skull",skull);
            if(Boolean.getBoolean("chao.posture.quick")&&sf==0&&rp==0&&h.label.equals("skin")&&q==97&&pitch==0&&yaw==0){var b=new PoseStack();anchor.accept("body",b);System.out.println("LANDMARK "+key+" adult="+adult+" leftBody="+new Matrix4f(b.last().pose()).invert().transformPosition(ChaoTriangleAnimation.palm(anchor,leftPalm,true)).mul(16)+" rightBody="+new Matrix4f(b.last().pose()).invert().transformPosition(ChaoTriangleAnimation.palm(anchor,rightPalm,false)).mul(16)+" headBox="+Arrays.toString(mesh.attachmentBounds("skull",poses)));}
            if(ChaoTriangleAnimation.propWeight(w)>0){for(int n=0;n<2;n++){
              var faces=n==0?bodyFaces:stickFaces;var prop=n==0?triangle:stick;
              float gap=PropMeshClearance.separation(faces,new Matrix4f(skull.last().pose()).invert().mul(prop),mesh.attachmentBounds("skull",poses));headChecks++;head=Math.min(head,gap);check(gap>.015f,"skull "+gap+" part="+n+" "+label);
              for(var f:faces)for(var v:f){float floor=24-prop.transformPosition(new Vector3f(v)).y*16;ground=Math.min(ground,floor);check(floor>.025f,"floor "+floor+" part="+n+" "+label);}
            }}
            for(boolean left:new boolean[]{false,true}){
              var a=poses.get(left?"arm_left":"arm_right");float shift=new Vector3f(a.x,a.y,a.z).length();shoulder=Math.max(shoulder,shift);reach=Math.max(reach,a.reach);check(shift<=.91f&&Math.abs(a.y)<=.351f,"shoulder "+shift+" "+label);
              var arm=new PoseStack();anchor.accept(left?"arm_left":"arm_right",arm);h.poses.get(left?"arm_left":"arm_right").reach=a.reach;
              float[] box=h.animal<0?h.asset.visibleHandBounds(h.poses,left):h.asset.animalHandBounds(h.animal,h.poses,left);
              if(w>.999f){
                float faceGap=PropMeshClearance.separation(TrumpetRigTest.boxTriangles(box),new Matrix4f(skull.last().pose()).invert().mul(arm.last().pose()),mesh.attachmentBounds("skull",poses));if(faceGap<-.025f)faceGap=Toy02535PoseVisualSmoke.separation(Toy02535PoseVisualSmoke.headSurface(mesh,poses),new Matrix4f(arm.last().pose()).invert().mul(skull.last().pose()),box);handHead=Math.min(handHead,faceGap);check(faceGap>-.025f,"handHead "+faceGap+" left="+left+" "+label);
                float touch=PropMeshClearance.separation(left?bodyFaces:stickFaces,new Matrix4f(arm.last().pose()).invert().mul(left?triangle:stick),box);check(touch<.025f,"handSurface "+touch+" left="+left+" "+label);
              }
            }
            if(w>.999f){
              var tip=stick.transformPosition(new Vector3f(ChaoTriangleAnimation.STICK_TIP).sub(.5f,.5f,.5f));float gap=tip.distance(ChaoTriangleAnimation.strikeTarget(anchor,triangle,tap));tipGap=Math.max(tipGap,gap);check(gap<.005f,"tipTrack "+gap+" "+label);
              float toRail=tip.distance(ChaoTriangleAnimation.hit(triangle))*16;if(tap>.999f)check(toRail<.025f,"strikeContact "+toRail+" "+label);if(tap<.001f)check(toRail>.65f,"beaterRetracts "+toRail+" "+label);
            }
          }
        }
      }
    }}
    String result="Triangle samples="+samples+" headChecks="+headChecks+" failures="+failures+" headPixels="+head+" groundPixels="+ground+" shoulderPixels="+shoulder+" tipBlocks="+tipGap+" handHeadPixels="+handHead+" maxReachPixels="+reach+"\n"+String.join("\n",bad)+"\n";
    System.out.println("FAILURE GROUPS "+reasons);System.out.print(result);Files.createDirectories(Path.of("build/reports/lionfish"));Files.writeString(Path.of("build/reports/lionfish/triangle-results.txt"),result);if(failures>0)throw new AssertionError(result);
 }
}
