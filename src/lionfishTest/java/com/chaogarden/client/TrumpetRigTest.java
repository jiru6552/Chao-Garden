package com.chaogarden.client;
import com.google.gson.*;
import com.mojang.blaze3d.vertex.PoseStack;
import com.chaogarden.core.ChaoState;
import java.nio.file.*;
import java.util.*;
import org.joml.Matrix4f;
import org.joml.Vector3f;
/** Mesh-based mouth, support-hand, skull and floor checks across ages and hand replacements. */
public final class TrumpetRigTest {
 record Hands(float[] right,float[] left,String label,float[] rightBox,float[] leftBox,ChaoMeshAsset asset,Map<String,ChaoMeshAsset.Pose> poses,int animal){}
 static int samples,failures,headChecks;static float head=Float.POSITIVE_INFINITY,ground=Float.POSITIVE_INFINITY,shoulder,contact,mouthGap,handHead=Float.POSITIVE_INFINITY;
 static final List<String> bad=new ArrayList<>();static final Map<String,Float> worst=new TreeMap<>();
 static final Map<String,Integer> reasons=new TreeMap<>();
 static void check(boolean ok,String label){if(!ok){reasons.merge(label.split(" ")[0],1,Integer::sum);failures++;if(bad.size()<24)bad.add(label);}}
 static List<Vector3f[]> boxTriangles(float[] box){
  var v=new Vector3f[8];for(int i=0;i<8;i++)v[i]=new Vector3f(box[(i&1)==0?0:3],box[(i&2)==0?1:4],box[(i&4)==0?2:5]).div(16);
  var out=new ArrayList<Vector3f[]>();for(int[] f:new int[][]{{0,1,3},{0,3,2},{4,6,7},{4,7,5},{0,4,5},{0,5,1},{2,3,7},{2,7,6},{0,2,6},{0,6,4},{1,5,7},{1,7,3}})out.add(new Vector3f[]{v[f[0]],v[f[1]],v[f[2]]});return out;
 }
 public static void main(String[] args)throws Exception{
  var clip=new LionfishTrumpetClip();var faces=PropMeshClearance.triangles("chao_trumpet");
  int notes=0;for(int t=0;t<=160;t++){if(com.chaogarden.toys.TrumpetPlayRules.soundDue(t)){notes++;check(com.chaogarden.toys.TrumpetPlayRules.blowing(t),"sound must fall inside a breath phrase");}check(Float.isFinite(clip.lean(t)),"finite Lionfish sample");}check(notes==8,"eight authored notes");
    double[] baseline={4,4,7,11,9,7,4,2};int noteIndex=0;double priorPitch=0;
    for(int tick=0;tick<=160;tick++)if(com.chaogarden.toys.TrumpetPlayRules.soundDue(tick)){
      double pitch=com.chaogarden.toys.TrumpetPlayRules.pitch(tick);
      check(pitch>.5&&pitch<1.95,"pitch headroom below Minecraft 2.0 clamp");
      check(Math.abs(12*Math.log(pitch)/Math.log(2)-baseline[noteIndex]-.5)<.00001,"uniform half-semitone transposition");
      if(noteIndex>0)check(Math.abs(12*Math.log(pitch/priorPitch)/Math.log(2)-(baseline[noteIndex]-baseline[noteIndex-1]))<.00001,"melody intervals preserved");
      priorPitch=pitch;noteIndex++;
    }
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
          float t=q*.25f,w=com.chaogarden.toys.TrumpetPlayRules.presence(t);
          for(float pitch:w>=1?new float[]{0}:new float[]{-.25f,0,.25f})for(float yaw:w>=1?new float[]{0}:new float[]{-.65f,0,.65f}){
            poses.values().forEach(ChaoMeshAsset.Pose::reset);poses.get("body").y=.8f;poses.get("body").rx=-.08f;poses.get("skull").y=.8f;poses.get("skull").rx=.025f+pitch;poses.get("skull").ry=yaw;
            java.util.function.BiConsumer<String,PoseStack> anchor=(n,s)->mesh.transformBone(n,s,poses);
            float[] mouth=mesh.attachmentMouth(poses);check(mouth!=null,"mouth landmark "+key);if(mouth==null)continue;
            if(Boolean.getBoolean("chao.posture.quick")&&sf==0&&rp==0&&h.label.equals("skin")&&q==97&&pitch==0&&yaw==0){
              var st=new PoseStack();anchor.accept("skull",st);var ar=new PoseStack();anchor.accept("arm_right",ar);System.out.println("LANDMARK "+key+" "+adult+" mouth="+st.last().pose().transformPosition(new Vector3f(mouth).div(16)).mul(16)+" shoulder="+ar.last().pose().transformPosition(new Vector3f()).mul(16)+" palm="+java.util.Arrays.toString(h.right));
            }
            float headX=poses.get("skull").x,headY=poses.get("skull").y,headZ=poses.get("skull").z;
            ChaoTrumpetAnimation.pose(poses,anchor,mouth,h.right,h.left,w,clip.lean(t));samples++;
            check(poses.get("skull").x==headX&&poses.get("skull").y==headY&&poses.get("skull").z==headZ,"headJoint unchanged");
            if(w==1)check(Math.abs(poses.get("skull").rx-.305f)<.0001f,"headTilt reduced");
            String label=key+" adult="+adult+" morph="+sf+","+rp+" "+h.label+" t="+t+" look="+pitch+","+yaw;
            var prop=ChaoTrumpetAnimation.propMatrix(anchor,mouth,w,clip.lean(t));
            var skull=new PoseStack();anchor.accept("skull",skull);
            if(ChaoTrumpetAnimation.propWeight(w)>0){
              float gap=PropMeshClearance.separation(faces,new Matrix4f(skull.last().pose()).invert().mul(prop),mesh.attachmentBounds("skull",poses));headChecks++;head=Math.min(head,gap);check(gap>.015f,"skull "+gap+" "+label);
              for(var f:faces)for(var v:f){float floor=24-prop.transformPosition(new Vector3f(v)).y*16;ground=Math.min(ground,floor);check(floor>.025f,"floor "+floor+" "+label);}
            }
            for(boolean left:new boolean[]{false,true}){
              var a=poses.get(left?"arm_left":"arm_right");float shift=new Vector3f(a.x,a.y,a.z).length();shoulder=Math.max(shoulder,shift);check(shift<=1.56f&&Math.abs(a.y)<=.651f,"shoulder "+shift+" "+label);
              var arm=new PoseStack();anchor.accept(left?"arm_left":"arm_right",arm);float[] original=left?h.left:h.right;float[] palm=ChaoTrumpetAnimation.reachedPalm(original,a.reach,left);
              h.poses.get(left?"arm_left":"arm_right").reach=a.reach;
              float[] box=h.animal<0?h.asset.visibleHandBounds(h.poses,left):h.asset.animalHandBounds(h.animal,h.poses,left);

                if(w>.999f){float faceGap=PropMeshClearance.separation(boxTriangles(box),new Matrix4f(skull.last().pose()).invert().mul(arm.last().pose()),mesh.attachmentBounds("skull",poses));if(faceGap<-.025f)faceGap=Toy02533PoseVisualSmoke.separation(Toy02533PoseVisualSmoke.headSurface(mesh,poses),new Matrix4f(arm.last().pose()).invert().mul(skull.last().pose()),box);handHead=Math.min(handHead,faceGap);check(faceGap>-.025f,"handHead "+faceGap+" left="+left+" "+label);}
              if(w>.999f){
                float gap=arm.last().pose().transformPosition(new Vector3f(palm).div(16)).distance(ChaoTrumpetAnimation.grip(new Matrix4f(prop),anchor,original,left));contact=Math.max(contact,gap);if(gap>.04f)worst.merge(key+" "+h.label+" left="+left,gap,Math::max);check(gap<.04f,"grip "+gap+" left="+left+" "+label);
                float touch=PropMeshClearance.separation(faces,new Matrix4f(arm.last().pose()).invert().mul(prop),box);check(touch<.025f,"hand surface "+touch+" left="+left+" "+label);
              }
            }
            if(w>.999f){float gap=prop.transformPosition(new Vector3f(ChaoTrumpetAnimation.TIP).sub(.5f,.5f,.5f)).distance(skull.last().pose().transformPosition(new Vector3f(mouth).div(16)));mouthGap=Math.max(mouthGap,gap);check(gap<.04f,"mouth "+gap+" "+label);}
          }
        }
      }
    }}
    String result="Trumpet samples="+samples+" headChecks="+headChecks+" failures="+failures+" headPixels="+head+" groundPixels="+ground+" shoulderPixels="+shoulder+" gripBlocks="+contact+" mouthBlocks="+mouthGap+" handHeadPixels="+handHead+"\n"+String.join("\n",bad)+"\n";
    System.out.println("FAILURE GROUPS "+reasons);System.out.println("WORST "+worst);System.out.print(result);Files.createDirectories(Path.of("build/reports/lionfish"));Files.writeString(Path.of("build/reports/lionfish/trumpet-results.txt"),result);if(failures>0)throw new AssertionError(result);
 }
}
