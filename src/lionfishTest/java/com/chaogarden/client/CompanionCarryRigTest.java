package com.chaogarden.client;

import com.chaogarden.core.ChaoState;
import com.google.gson.JsonParser;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.UUID;
import org.joml.Matrix4f;
import org.joml.Vector3f;

/** Checks actual morphed chest geometry against the player's back in torso space. */
public final class CompanionCarryRigTest {
  private static int checks,cases;
  private static float minGap=Float.POSITIVE_INFINITY,maxGap=Float.NEGATIVE_INFINITY,maxStep;
  private static void check(boolean ok,String message) { checks++;if(!ok)throw new AssertionError(message); }
  public static void main(String[] args)throws Exception {
    var root=Path.of("src/main/resources/assets/chao_garden");
    var clips=JsonParser.parseString(Files.readString(root.resolve("animations/official/chao.animation.json")))
        .getAsJsonObject().getAsJsonObject("animations");
    var assets=new HashMap<String,ChaoMeshAsset>();
    var rig=new OfficialChaoRig(clips,key->assets.computeIfAbsent(key,k->{
      try{return ChaoMeshAsset.parse(JsonParser.parseString(Files.readString(root.resolve("models/entity/"+k+".json"))).getAsJsonObject());}
      catch(Exception error){throw new RuntimeException(error);}
    }));
    try(var models=Files.list(root.resolve("models/entity/gameplay"))) {
      for(var path:models.filter(p->p.toString().endsWith(".json")&&!p.toString().contains("_glass")).sorted().toList()) {
        String model=path.getFileName().toString().replace("chao_","").replace(".json","");
        for(boolean adult:new boolean[]{false,true})for(float sf:new float[]{-1,0,1})for(float rp:new float[]{-1,0,1}) {
          var state=new ChaoState();state.adult=adult;state.swimFly=sf;state.runPower=rp;state.setEvolutionStrength(1);
          var id=new UUID(cases++,46);rig.prepareDevelopment(model,state);
          rig.animateGameplay(model,id,OfficialChaoRig.Motion.SIT,120,0,0,0,0,0,0,false,false);
          rig.development(state);rig.applyCompanionCarry(1);
          var chest=rig.attachmentBounds("body");check(chest!=null,"missing chest "+model);
          var chestAnchor=new PoseStack();rig.transformBone("body",chestAnchor);
          var attached=new PoseStack();CompanionBackLayer.carryTransform(attached,1);
          float scale=adult?1:.8f;attached.scale(scale,scale,scale);
          attached.mulPose(Axis.YP.rotationDegrees(180));attached.scale(-1,-1,1);attached.translate(0,-1.501f,0);
          var matrix=new Matrix4f(attached.last().pose()).mul(chestAnchor.last().pose());
          var middle=new Vector3f((chest[0]+chest[3])/32,(chest[1]+chest[4])/32,chest[2]/16);
          var front=matrix.transformPosition(middle);
          float gap=front.z-.125f;minGap=Math.min(minGap,gap);maxGap=Math.max(maxGap,gap);
          String label=model+" adult="+adult+" morph="+sf+","+rp;
          check(gap>=-.025f&&gap<=.075f,"chest must meet the keeper's back: gap="+gap+" "+label);
          check(front.y>=.10f&&front.y<=.45f,"chest must sit on upper/middle torso: y="+front.y+" "+label);
          // The same anchor must remain against the skin through yaw, crouch,
          // swimming and elytra pitch rather than trail in world coordinates.
          for(float yaw:new float[]{0,90,180,270})for(float pitch:new float[]{-120,-90,-45,0,35})for(boolean crouch:new boolean[]{false,true}) {
            var torso=new Matrix4f().translate(7,13,-11).rotateY((float)Math.toRadians(yaw)).rotateX((float)Math.toRadians(pitch));
            if(crouch)torso.translate(0,3.2f/16,0).rotateX(.5f);
            var world=torso.transformPosition(new Vector3f(front));
            var skin=torso.transformPosition(new Vector3f(front.x,front.y,.125f));
            var normal=torso.transformDirection(new Vector3f(0,0,1));
            check(Math.abs(new Vector3f(world).sub(skin).dot(normal)-gap)<.00001f,"lost torso contact "+label);
          }
        }
      }
    }
    Vector3f previous=null;
    for(int tick=0;tick<=128;tick++) {
      float blend=tick<=64?tick/64f:(128-tick)/64f;
      var stack=new PoseStack();CompanionBackLayer.carryTransform(stack,blend);
      var at=stack.last().pose().transformPosition(new Vector3f());
      check(Float.isFinite(at.y)&&Float.isFinite(at.z),"non-finite carry transition");
      if(previous!=null){float step=at.distance(previous);maxStep=Math.max(maxStep,step);check(step<.04f,"carry transition jump "+step);}
      previous=at;
    }
    check(cases>=300,"full source model/age/morph matrix executed");
    String result="PASS: "+checks+" carry checks; "+cases+" form/age/morph cases; back skin gap="+minGap+".."+maxGap+" blocks; max transition step="+maxStep+" blocks\n";
    Files.createDirectories(Path.of("build/reports/lionfish"));Files.writeString(Path.of("build/reports/lionfish/companion-carry-results.txt"),result);System.out.print(result);
  }
}
