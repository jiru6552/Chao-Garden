package com.chaogarden.client;

import com.google.gson.*;
import com.mojang.blaze3d.vertex.*;
import java.nio.file.*;
import java.util.*;
import org.joml.Vector3f;

/** Real Lionfish 3.1 sampler + real Chao mesh parser/deformation/render transforms, without a GL window. */
public final class RattleAnimationTest {
  private static int assertions;
  private static float maxGripError;
  private static void check(boolean value,String message) {assertions++;if(!value)throw new AssertionError(message);}
  private static void near(Vector3f a,Vector3f b,String message) {check(a.distance(b)<1e-6f,message+": "+a+" vs "+b);}
  static final class Sink implements VertexConsumer {
    final List<float[]> vertices=new ArrayList<>();
    public VertexConsumer addVertex(float x,float y,float z){vertices.add(new float[]{x,y,z});return this;}
    public VertexConsumer setColor(int r,int g,int b,int a){return this;}
    public VertexConsumer setUv(float u,float v){return this;}
    public VertexConsumer setUv1(int u,int v){return this;}
    public VertexConsumer setUv2(int u,int v){return this;}
    public VertexConsumer setNormal(float x,float y,float z){return this;}
    Vector3f center() {
      var min=new Vector3f(Float.POSITIVE_INFINITY);var max=new Vector3f(Float.NEGATIVE_INFINITY);
      for(var p:vertices){min.min(new Vector3f(p));max.max(new Vector3f(p));}
      return min.add(max).mul(.5f);
    }
  }
  public static void main(String[] args) throws Exception {
    var clip=new LionfishRattleClip();
    var bubbles=new LionfishBubbleClip();
    near(bubbles.sample(0),new Vector3f(),"bubble starts at rest");near(bubbles.sample(160),new Vector3f(),"bubble returns to rest");
    near(bubbles.sample(Float.NaN),new Vector3f(),"invalid bubble time resets");
    int puffs=0;
    for(int tick=0;tick<=160;tick++)if(com.chaogarden.toys.BubblePlayRules.puffDue(tick)){
      puffs++;near(bubbles.sample(tick),new Vector3f(0,(float)Math.toRadians(-92),(float)Math.toRadians(4)),"bubbles occur only at authored blow pose");
      check(com.chaogarden.toys.BubblePlayRules.blowing(tick),"blow state matches particle cue");
    }
    check(puffs==12,"three groups of four bubble cues");
    near(clip.sample(0),new Vector3f(),"starts at rest");near(clip.sample(160),new Vector3f(),"returns to rest");
    near(clip.sample(10),new Vector3f(0,(float)Math.toRadians(-60),(float)Math.toRadians(22)),"authored hold endpoint");
    near(clip.sample(-10),new Vector3f(),"negative time clamps");near(clip.sample(170),new Vector3f(),"past-end clamps");
    near(clip.sample(Float.NaN),new Vector3f(),"invalid time resets");
    near(clip.sample(21),new Vector3f(0,(float)Math.toRadians(-67.5),(float)Math.toRadians(26)),"half-tick interpolation between shake extrema");
    var hold=clip.sample(10);clip.sample(24);near(clip.sample(10),hold,"shared model does not leak last actor pose");
    var wrist=new PoseStack();ChaoRattleAnimation.propTransform(wrist,new float[]{0,0,0},1);
    var up=wrist.last().pose().transformDirection(new Vector3f(0,1,0)).normalize();
    check(up.x<0&&up.y<-.5f,"rattle points outward and more upright than the -65 degree prototype");
    int soundCount=0;
    for(int tick=-10;tick<=170;tick++) {
      boolean due=com.chaogarden.toys.RattlePlayRules.soundDue(tick);
      if(due) {
        soundCount++;
        check(com.chaogarden.toys.RattlePlayRules.happy(tick),"every shake has a happy expression");
        float yaw=clip.sample(tick).y;
        check(Math.abs(yaw-(float)Math.toRadians(soundCount%2==1?-74:-48))<1e-5,"sound lands on the real Lionfish shake extremum");
      }
    }
    check(soundCount==29,"one sound per authored shake, no raise/lower sounds");
    check(!com.chaogarden.toys.RattlePlayRules.happy(0)&&!com.chaogarden.toys.RattlePlayRules.happy(160),"expression clears at both boundaries");
    var frames=new JsonArray();
    var models=new ArrayList<String>();models.add("baby");
    try(var paths=Files.list(Path.of("src/main/resources/assets/chao_garden/models/entity/gameplay"))) {
      paths.filter(p->p.getFileName().toString().matches("chao_.*\\.json"))
          .map(p->p.getFileName().toString().replace("chao_", "").replace(".json", ""))
          .sorted().forEach(models::add);
    }
    int handRigs=0,emptyHandRigs=0;
    for(int form=0;form<models.size();form++) {
      String model=models.get(form),label=form==0?"baby":model.equals("baby")?"adult_normal":model.equals("run")?"adult_run":model;
      var json=JsonParser.parseString(Files.readString(Path.of("src/main/resources/assets/chao_garden/models/entity/gameplay/chao_"+model+".json"))).getAsJsonObject();
      var mesh=ChaoMeshAsset.parse(json);var poses=mesh.createPoses();
      if(model.endsWith("_glass")) {
        check(mesh.rattlePalm(poses)==null,"material-only glass overlay has no independent hand");
        var state=new com.chaogarden.core.ChaoState();state.immortal=true;
        check(com.chaogarden.toys.RattlePlayRules.handheld(state),"Chaos base rig supports hand-held toys");
        emptyHandRigs++;continue;
      }
      handRigs++;
      for(float sf:new float[]{-1,0,1})for(float rp:new float[]{-1,0,1}) {
        mesh.development(sf,rp,form==0?.14f:1);
        poses.values().forEach(p->p.reset());
        var palm=mesh.rattlePalm(poses);check(palm!=null,label+" has palm");
        // Independent check: emitted hand vertices, with its arm inverse, locate the same socket.
        boolean tip=model.startsWith("chaos_");
        poses.forEach((name,p)->p.visible=name.equals(tip?"arm_right__cube_0":"arm_right__cube_1"));
        var hand=new Sink();mesh.render(new PoseStack(),hand,0,0,-1,poses,false);
        var local=new PoseStack();mesh.transformBone("arm_right",local,poses);
        var inv=local.last().pose().invert(new org.joml.Matrix4f());
        for(var point:hand.vertices){var v=inv.transformPosition(new Vector3f(point));point[0]=v.x;point[1]=v.y;point[2]=v.z;}
        var measured=hand.center();
        if(tip){float min=Float.POSITIVE_INFINITY,max=Float.NEGATIVE_INFINITY;for(var v:hand.vertices){min=Math.min(min,v[0]);max=Math.max(max,v[0]);}measured.x=min+Math.min(.25f/16,(max-min)*.15f);}
        near(measured,new Vector3f(palm).div(16),label+" socket follows emitted morphed hand or arm tip");
        poses.values().forEach(p->p.reset());
        for(int step=0;step<=640;step++) {
          float tick=step*.25f;var angle=clip.sample(tick);
          check(angle.isFinite(),"finite angles");check(angle.x==0&&angle.y>=-1.30f&&angle.y<=0&&angle.z>=0&&angle.z<.53f,"bounded clip");
          var arm=poses.get("arm_right");arm.reset();ChaoRattleAnimation.applyArm(arm,clip,tick);
          var base=new PoseStack();float ageScale=form==0?.64f:.80f;base.scale(ageScale,ageScale,ageScale);
          mesh.transformBone("arm_right",base,poses);
          var expected=base.last().pose().transformPosition(new Vector3f(palm).div(16));
          ChaoRattleAnimation.propTransform(base,palm,Math.max(.001f,com.chaogarden.toys.RattlePlayRules.presence(tick)));
          base.translate(-.5,-.5,-.5); // Actual ItemRenderer's centering operation.
          var grip=base.last().pose().transformPosition(new Vector3f(.5f,.30f,.5f));
          float error=grip.distance(expected);maxGripError=Math.max(maxGripError,error);
          check(error<1e-6f,label+" grip stays in palm across animation and age scale");
          check(arm.sx==1&&arm.sy==1&&arm.sz==1,"clip does not rescale rig");
          var saved=new Vector3f(arm.rx,arm.ry,arm.rz);float savedX=arm.x;
          arm.reset();ChaoBubbleAnimation.applyArm(arm,bubbles,tick);
          check(bubbles.sample(tick).isFinite(),"finite bubble clip for every age/morph");
          var bubbleBase=new PoseStack();bubbleBase.scale(ageScale,ageScale,ageScale);mesh.transformBone("arm_right",bubbleBase,poses);
          var bubblePalm=bubbleBase.last().pose().transformPosition(new Vector3f(palm).div(16));
          ChaoBubbleAnimation.propTransform(bubbleBase,palm,Math.max(.001f,com.chaogarden.toys.BubblePlayRules.presence(tick)));
          bubbleBase.translate(-.5,-.5,-.5);
          near(bubbleBase.last().pose().transformPosition(new Vector3f(.5f,.28f,.5f)),bubblePalm,label+" bubble grip stays in deformed palm");
          arm.rx=saved.x;arm.ry=saved.y;arm.rz=saved.z;arm.x=savedX;
          if(sf==0&&rp==0&&step%16==0&&List.of("baby","adult_normal","adult_run").contains(label)) {
            // Match neutral marker visibility for the offline rest-rig preview.
            for(String symbol:List.of("emotion_heart","emotion_exclamation","emotion_question","emotion_dizzy","monster_crown"))
              mesh.setSubtreeVisible(symbol,poses,false);
            var capture=new Sink();var root=new PoseStack();root.scale(ageScale,ageScale,ageScale);
            mesh.render(root,capture,0,0,-1,poses,true);
            var frame=new JsonObject();frame.addProperty("form",label);frame.addProperty("tick",tick);
            frame.add("quads",new Gson().toJsonTree(capture.vertices));
            frame.add("propMatrix",new Gson().toJsonTree(base.last().pose().get(new float[16])));
            frames.add(frame);
          }
        }
      }
      poses.get("arm_right__cube_1").visible=false;check(mesh.rattlePalm(poses)==null,"hidden hand rejects socket");
    }
    Files.createDirectories(Path.of("build/reports/lionfish"));
    Files.writeString(Path.of("build/reports/lionfish/geometry-frames.json"),new Gson().toJson(frames));
    ToyVariantRigTest.run();
    MaracasRigTest.run();BellRigTest.run();TambourineRigTest.run();TambourineReplacementRigTest.run();CastanetsRigTest.run();
    HeadClearanceRigTest.run();
    CymbalsRigTest.run();CastanetsReplacementRigTest.run();
    String result="PASS: "+assertions+" assertions; "+handRigs+" hand rigs/ages; "+emptyHandRigs+" material-only glass overlays; 9 morphology endpoints; 641 sub-tick samples each; max grip error="+maxGripError+" blocks. Real Lionfish 3.1; offline rig checks, not an in-game visual pass.";
    Files.writeString(Path.of("build/reports/lionfish/results.txt"),result+"\n");System.out.println(result);
  }
}
