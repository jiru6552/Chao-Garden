package com.chaogarden.client;
import com.google.gson.*;
import com.mojang.blaze3d.vertex.PoseStack;
import com.chaogarden.core.ChaoState;
import java.nio.file.*;
import java.util.*;
import org.joml.Matrix4f;
import org.joml.Vector3f;
/** Physical posture constraints independent of whether the IK reaches its own target. */
public final class DrumRigTest {
  record Hands(float[] right,float[] left,String label,float[] rightBox,float[] leftBox){}
  static int samples,headChecks,failures;static float head=Float.POSITIVE_INFINITY,ground=Float.POSITIVE_INFINITY,shoulder,contact,highestHand=Float.POSITIVE_INFINITY,lowestHand=Float.NEGATIVE_INFINITY;
  static final List<String> bad=new ArrayList<>();
  static final Map<String,Float> worst=new TreeMap<>();
  static void check(boolean ok,String label){if(!ok){failures++;if(bad.size()<25)bad.add(label);}}
  public static void main(String[] args)throws Exception{run();}
  static void run()throws Exception{
    var casts=new LionfishDrumClip();
    int beats=0;for(int t=0;t<=160;t++){boolean due=com.chaogarden.toys.DrumPlayRules.soundDue(t);if(due){beats++;check(casts.tap(t)>.999f,"strike must meet the authored audio tick "+t);}}check(beats==16,"sixteen synchronized strikes");
    var base=PropMeshClearance.triangles("chao_drum");
    try(var files=Files.list(Path.of("src/main/resources/assets/chao_garden/models/entity/gameplay"))){for(var path:files.filter(p->p.toString().endsWith(".json")&&!p.toString().contains("_glass")).toList()){
      var mesh=ChaoMeshAsset.parse(JsonParser.parseString(Files.readString(path)).getAsJsonObject());var poses=mesh.createPoses();
      for(boolean adult:new boolean[]{false,true})for(float sf:new float[]{-1,0,1})for(float rp:new float[]{-1,0,1}){
        mesh.development(sf,rp,adult?1:.14f);poses.values().forEach(ChaoMeshAsset.Pose::reset);var hands=new ArrayList<Hands>();hands.add(new Hands(mesh.toyPalm(poses,false),mesh.toyPalm(poses,true),"skin",mesh.toyHandBounds(poses,false),mesh.toyHandBounds(poses,true)));
        String key=path.getFileName().toString();if(sf==0&&rp==0&&!key.contains("chaos_")&&!key.contains("omochao"))for(int animal=0;animal<ChaoState.ANIMALS.length;animal++){
          if((ChaoState.animalSlots(animal)&1)==0)continue;var p=path.getParent().getParent().resolve((animal<21?"overlays/":"cwe_parts/")+key);if(!Files.exists(p))continue;
          var repl=ChaoMeshAsset.parse(JsonParser.parseString(Files.readString(p)).getAsJsonObject());var ps=repl.createPoses();var r=ChaoToyGrip.animalPalm(repl,ps,animal,adult,false);var l=ChaoToyGrip.animalPalm(repl,ps,animal,adult,true);if(r!=null&&l!=null)hands.add(new Hands(r,l,"animal"+animal,repl.animalHandBounds(animal,ps,false),repl.animalHandBounds(animal,ps,true)));
        }
        for(var h:hands)for(boolean cast:new boolean[]{true})for(int q=1;q<640;q+=Boolean.getBoolean("chao.posture.quick")?16:1){
          float t=q*.25f,w=com.chaogarden.toys.DrumPlayRules.presence(t);poses.values().forEach(ChaoMeshAsset.Pose::reset);
          poses.get("body").y=.8f;poses.get("body").rx=-.08f;poses.get("skull").y=.8f;poses.get("skull").rx=.025f;
          java.util.function.BiConsumer<String,PoseStack> anchor=(n,s)->mesh.transformBone(n,s,poses);
          for(boolean left:new boolean[]{false,true})ChaoDrumAnimation.applyArm(poses.get(left?"arm_left":"arm_right"),casts,t,w,left);
          ChaoDrumAnimation.alignTap(poses,anchor,h.right,h.left,w,casts.tap(t));
          String label=key+" adult="+adult+" morph="+sf+","+rp+" "+h.label+" "+(cast?"cast":"cym")+" t="+t;samples++;
          var skull=new PoseStack();anchor.accept("skull",skull);var inv=new Matrix4f(skull.last().pose()).invert();float[] bounds=mesh.attachmentBounds("skull",poses);
          var body=new PoseStack();anchor.accept("body",body);var inverseBody=new Matrix4f(body.last().pose()).invert();
          var props=List.of(ChaoDrumAnimation.propMatrix(anchor,h.right,w));
          for(int n=0;n<1&&(cast?ChaoDrumAnimation.propWeight(w)>0:ChaoDrumAnimation.propWeight(w)>0);n++){
            var faces=base;var prop=props.get(n);
            // Include the gameplay head-look limits, not just a level head.
            for(float pitch:w>=1?new float[]{0}:new float[]{-.25f,0,.25f})for(float yaw:w>=1?new float[]{0}:new float[]{-.65f,0,.65f}){
              poses.get("skull").rx=.025f+pitch;poses.get("skull").ry=yaw;ChaoInstrumentFocus.apply(poses,w);var looked=new PoseStack();anchor.accept("skull",looked);
              float gap=PropMeshClearance.separation(faces,new Matrix4f(looked.last().pose()).invert().mul(prop),bounds);headChecks++;head=Math.min(head,gap);
              if(gap<=.025f)worst.merge((cast?"cast":"cym")+" "+key+" "+h.label+" part="+n+" look="+pitch+","+yaw,gap,Math::min);
              check(gap>.025f,"head "+gap+" part="+n+" look="+pitch+","+yaw+" "+label);
            }
            poses.get("skull").rx=.025f;poses.get("skull").ry=0;
            for(var f:faces)for(var v:f){float clearance=24-prop.transformPosition(new Vector3f(v)).y*16;ground=Math.min(ground,clearance);check(clearance>.025f,"ground "+clearance+" "+label);}
          }
          for(boolean left:new boolean[]{false,true}){
            var arm=poses.get(left?"arm_left":"arm_right");float shift=new Vector3f(arm.x,arm.y,arm.z).length();shoulder=Math.max(shoulder,shift);check(shift<=1.11f&&Math.abs(arm.x)<=.401f&&Math.abs(arm.y)<=.401f&&Math.abs(arm.z)<=1.11f,"shoulder "+shift+" "+label);
            if(w>.999f){float y=inverseBody.transformPosition(ChaoCymbalsAnimation.palm(anchor,left?h.left:h.right,left)).y*16;highestHand=Math.min(highestHand,y);lowestHand=Math.max(lowestHand,y);check(y>=-5&&y<=-.8f,"chest height "+y+" "+label);}
          }
          if(w>.999f){
            for(boolean left:new boolean[]{false,true}){
              if(cast&&left&&casts.tap(t)<.99f)continue;
              var arm=new PoseStack();anchor.accept(left?"arm_left":"arm_right",arm);
              var prop=props.get(0);var faces=base;
              float touch=PropMeshClearance.separation(faces,new Matrix4f(arm.last().pose()).invert().mul(prop),left?h.leftBox:h.rightBox);
              if(touch>=.025f)worst.merge("TOUCH "+(cast?"cast":"cym")+" "+key+" "+h.label+" left="+left,touch,Math::max);
              check(touch<.025f,"actual hand surface contact "+touch+" left="+left+" "+label);
            }
          }
          if(w>.999f&&cast){float gap=ChaoCymbalsAnimation.palm(anchor,h.left,true).distance(ChaoDrumAnimation.hitTarget(props.get(0),casts.tap(t)));contact=Math.max(contact,gap);check(gap<.035f,"continuous tap contact "+gap+" "+label);}
        }
      }
    }}
    String result="Drum samples="+samples+" headChecks="+headChecks+" failures="+failures+" headPixels="+head+" groundPixels="+ground+" shoulderPixels="+shoulder+" contactBlocks="+contact+" handHeight="+highestHand+".."+lowestHand+"\n"+String.join("\n",bad)+"\n";System.out.println("WORST "+worst);System.out.print(result);Files.createDirectories(Path.of("build/reports/lionfish"));Files.writeString(Path.of("build/reports/lionfish/drum-results.txt"),result);if(failures>0)throw new AssertionError(result);
  }
}
