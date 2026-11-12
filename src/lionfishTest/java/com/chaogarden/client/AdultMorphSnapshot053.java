// Historical explicit052-asset fingerprint independently measured from the
// delivered .52 JAR. Production selection is now tested by the .54 rig test.
package com.chaogarden.client;
import com.chaogarden.core.*;
import com.google.gson.JsonParser;
import com.mojang.blaze3d.vertex.*;
import java.nio.*;import java.nio.file.*;import java.security.*;import java.util.*;
final class AdultMorphSnapshot053 {
 static final Path ROOT=Path.of("src/main/resources/assets/chao_garden");
 static MessageDigest hash;static int serial;
 static void f(float value){hash.update(ByteBuffer.allocate(4).order(ByteOrder.BIG_ENDIAN).putInt(Float.floatToRawIntBits(value)).array());}
 static void floats(float[] values){if(values==null){hash.update((byte)0);return;}hash.update((byte)1);for(float v:values)f(v);}
 static ChaoMeshAsset asset(String name){try{return ChaoMeshAsset.parse(JsonParser.parseString(Files.readString(ROOT.resolve("models/entity/"+name+".json"))).getAsJsonObject());}catch(Exception e){throw new IllegalStateException(e);}}
 static final class Capture implements VertexConsumer {
  public VertexConsumer addVertex(float x,float y,float z){f(x);f(y);f(z);return this;}
  public VertexConsumer setColor(int r,int g,int b,int a){hash.update((byte)r);hash.update((byte)g);hash.update((byte)b);hash.update((byte)a);return this;}
  public VertexConsumer setUv(float u,float v){f(u);f(v);return this;}
  public VertexConsumer setUv1(int u,int v){return this;}public VertexConsumer setUv2(int u,int v){return this;}
  public VertexConsumer setNormal(float x,float y,float z){f(x);f(y);f(z);return this;}
 }
 public static String snapshot()throws Exception {
  hash=MessageDigest.getInstance("SHA-256");serial=0;
  var mesh=asset("gameplay/chao_hero_run_proto052");var poses=mesh.createPoses();
  for(float maturity:new float[]{0,.25f,.5f,.75f,1})for(float run:new float[]{0,.25f,.5f,.75f,1})for(float sf:new float[]{-1,0,1})for(float power:new float[]{0,1}) {
   var s=new ChaoState();s.adult=true;s.align=1;s.primaryType=ChaoState.RUN_TYPE;int total=Math.round(run*960);s.levels[2]=total/100;s.progress[2]=total%100;s.swimFly=sf;s.runPower=power;s.setEvolutionStrength(maturity*1.2f);
   mesh.development(s.swimFly,s.runPower,s.gradualVisualStrength());mesh.heroRunDevelopment(HeroRunMorphology052.sample(s),sf,power);mesh.heroRunTwoTone(true);
   poses.values().forEach(ChaoMeshAsset.Pose::reset);mesh.render(new PoseStack(),new Capture(),0xf000f0,0,0xffffffff,poses,true);
   for(String bone:List.of("body","skull","arm_left","arm_right","foot_left","foot_right","tail","wing_left","wing_right")){var stack=new PoseStack();mesh.transformBone(bone,stack,poses);floats(stack.last().pose().get(new float[16]));floats(mesh.attachmentBounds(bone,poses));}
  }
  return HexFormat.of().formatHex(hash.digest());
 }
 
}
