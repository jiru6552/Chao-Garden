package com.chaogarden.client;
import com.google.gson.*;
import com.mojang.blaze3d.vertex.PoseStack;
import com.chaogarden.core.ChaoState;
import java.nio.file.*;
import java.util.*;
import org.joml.Matrix4f;
public final class TriangleTransitionRigTest {
 public static void main(String[] args)throws Exception {
  var root=Path.of("src/main/resources/assets/chao_garden");var clips=JsonParser.parseString(Files.readString(root.resolve("animations/official/chao.animation.json"))).getAsJsonObject().getAsJsonObject("animations");var assets=new HashMap<String,ChaoMeshAsset>();
  var rig=new OfficialChaoRig(clips,key->assets.computeIfAbsent(key,k->{try{return ChaoMeshAsset.parse(JsonParser.parseString(Files.readString(root.resolve("models/entity/"+k+".json"))).getAsJsonObject());}catch(Exception e){throw new RuntimeException(e);}}));
  var clip=new LionfishTriangleClip();
  int samples=0,failures=0;float minimum=Float.POSITIVE_INFINITY;var bad=new ArrayList<String>();var failByTick=new TreeMap<Integer,Integer>();
  for(String variant:new String[]{"adult_dark_type4","chaos_2","tails_adult","mixed_1_4"})for(float pitch:new float[]{-25,0,25})for(float yaw:new float[]{-12,0,12})for(int ageBase:new int[]{100,300})for(int seenAt:new int[]{0,6,10}) {
   var state=com.chaogarden.test.ToyVariantCases02522.all().stream().filter(v->v.name().equals(variant)).findFirst().orElseThrow().state();String model=OfficialChaoRig.geometryKey(state);var id=new UUID(1234,5678);
   for(int q=-40;q<=640;q++){float t=q/4f;if(t>=0&&t<seenAt)continue;
    rig.prepareDevelopment(model,state);rig.animateGameplay(model,id,t<0?OfficialChaoRig.Motion.WALK:OfficialChaoRig.Motion.PLAY,ageBase+t,11+t*.17f,t<0?.12f:0,0,0,yaw,pitch,false,false);rig.development(state);
    var mesh=assets.get("gameplay/chao_"+model);var poses=rig.poses();var handMesh=mesh;
    if(model.startsWith("chaos_")){String glass="gameplay/chao_"+model+"_glass";handMesh=assets.computeIfAbsent(glass,k->{try{return ChaoMeshAsset.parse(JsonParser.parseString(Files.readString(root.resolve("models/entity/"+k+".json"))).getAsJsonObject());}catch(Exception e){throw new RuntimeException(e);}});}
    var r=handMesh.visiblePalm(poses,false);var l=handMesh.visiblePalm(poses,true);float w=com.chaogarden.toys.TrianglePlayRules.presence(t);
    rig.finishGameplayFrame(id,t<0?0:11264,ageBase+t);
    if(t>=0){ChaoTriangleAnimation.pose(poses,rig::transformBone,r,l,w,clip.tap(t));rig.rememberGameplayFrame(id);}
    var skull=new PoseStack();rig.transformBone("skull",skull);
    for(boolean left:new boolean[]{false,true}){
     var b=handMesh.visibleHandBounds(poses,left);
     var arm=new PoseStack();rig.transformBone(left?"arm_left":"arm_right",arm);float gap=PropMeshClearance.separation(TrumpetRigTest.boxTriangles(b),new Matrix4f(skull.last().pose()).invert().mul(arm.last().pose()),rig.attachmentBounds("skull"));
     if(gap<-.025f)gap=Toy02533PoseVisualSmoke.separation(Toy02533PoseVisualSmoke.headSurface(mesh,poses),new Matrix4f(arm.last().pose()).invert().mul(skull.last().pose()),b);
     if(t>=0){samples++;minimum=Math.min(minimum,gap);if(gap<-.025f){failures++;failByTick.merge((int)t,1,Integer::sum);if(bad.size()<30)bad.add(variant+" pitch="+pitch+" yaw="+yaw+" age="+ageBase+" seenAt="+seenAt+" t="+t+" left="+left+" gap="+gap);}}
    }
   }
  }
  String result="Triangle transition samples="+samples+" failures="+failures+" minHandHeadPixels="+minimum+"\n"+String.join("\n",bad)+"\n";Files.createDirectories(Path.of("build/reports/lionfish"));Files.writeString(Path.of("build/reports/lionfish/triangle-transition-results.txt"),result);System.out.println("Failure tick distribution "+failByTick);System.out.print(result);if(failures>0)throw new AssertionError(result);
 }
}
