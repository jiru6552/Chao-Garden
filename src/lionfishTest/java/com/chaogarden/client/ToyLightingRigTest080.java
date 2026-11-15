package com.chaogarden.client;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Transformation;
import java.io.ByteArrayInputStream;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.client.model.IQuadTransformer;
import net.neoforged.neoforge.client.model.obj.ObjMaterialLibrary;
import net.neoforged.neoforge.client.model.obj.ObjModel;
import net.neoforged.neoforge.client.model.obj.ObjTokenizer;
import net.neoforged.neoforge.client.textures.UnitTextureAtlasSprite;
import org.apache.commons.lang3.tuple.Pair;
import org.joml.Vector4f;

/** Real NeoForge OBJ quad baking and Minecraft bulk-vertex lighting; no game/client launch. */
public final class ToyLightingRigTest080 {
  private static final Path ROOT=Path.of("src/main/resources/assets/chao_garden");
  private static long checks,quads,renderedVertices;
  private static Method bake;
  private record Face(int[][] indices,String material){}
  private record Model(ObjModel model,List<Face> faces){}
  private static void check(boolean okay,String why){checks++;if(!okay)throw new AssertionError(why);}
  private static JsonObject json(Path path)throws Exception{return JsonParser.parseString(Files.readString(path)).getAsJsonObject();}
  private static Model model(JsonObject settings,String text,boolean emissive)throws Exception {
    var filtered=new StringBuilder();var faces=new ArrayList<Face>();String material="";
    for(String line:text.lines().toList()){
      if(line.startsWith("usemtl ")){material=line.substring(7);continue;}
      if(line.startsWith("mtllib "))continue;
      filtered.append(line).append('\n');
      if(line.startsWith("f ")){
        var words=line.substring(2).trim().split("\\s+");int[][] indices=new int[words.length][];
        for(int i=0;i<words.length;i++){
          var fields=words[i].split("/");indices[i]=new int[fields.length];
          for(int j=0;j<fields.length;j++){int value=Integer.parseInt(fields[j]);check(value>0,"Generated OBJ has explicit positive indices");indices[i][j]=value-1;}
        }
        faces.add(new Face(indices,material));
      }
    }
    var config=new ObjModel.ModelSettings(ResourceLocation.parse(settings.get("model").getAsString()),
        settings.get("automatic_culling").getAsBoolean(),settings.get("shade_quads").getAsBoolean(),
        settings.get("flip_v").getAsBoolean(),emissive,null);
    try(var reader=new ObjTokenizer(new ByteArrayInputStream(filtered.toString().getBytes(StandardCharsets.UTF_8)))){
      return new Model(ObjModel.parse(reader,config),faces);
    }
  }
  @SuppressWarnings("unchecked")
  private static BakedQuad quad(ObjModel model,Face face,Vector4f ambient)throws Exception{
    return ((Pair<BakedQuad,?>)bake.invoke(model,face.indices,-1,new Vector4f(1),ambient,
        UnitTextureAtlasSprite.INSTANCE,Transformation.identity())).getLeft();
  }
  private static final class Capture implements VertexConsumer {
    final List<Integer> lights=new ArrayList<>();int count;
    public VertexConsumer addVertex(float x,float y,float z){check(Float.isFinite(x)&&Float.isFinite(y)&&Float.isFinite(z),"Finite baked vertex");count++;renderedVertices++;return this;}
    public VertexConsumer setColor(int r,int g,int b,int a){return this;}
    public VertexConsumer setUv(float u,float v){check(Float.isFinite(u)&&Float.isFinite(v),"Finite baked UV");return this;}
    public VertexConsumer setUv1(int u,int v){return this;}
    public VertexConsumer setUv2(int u,int v){lights.add(u|(v<<16));return this;}
    public VertexConsumer setNormal(float x,float y,float z){return this;}
  }
  private static Capture render(BakedQuad quad,int incoming){
    var capture=new Capture();capture.putBulkData(new PoseStack().last(),quad,1,1,1,1,incoming,0);
    check(capture.count==4&&capture.lights.size()==4,"Complete actual bulk quad rendering");return capture;
  }
  private static void inspect(String kind,String context,ObjMaterialLibrary materials)throws Exception {
    var data=json(ROOT.resolve("models/"+(context.equals("held")?"item/"+kind+"_hand.json":"block/"+kind+"_idle.json")));
    check(data.has("emissive_ambient")&&!data.get("emissive_ambient").getAsBoolean(),"Explicitly opt out of NeoForge ambient emissivity: "+kind+"/"+context);
    check(data.get("shade_quads").getAsBoolean(),"Ordinary directional shading retained");
    String text=Files.readString(ROOT.resolve(data.get("model").getAsString().split(":",2)[1]));
    var model=model(data,text,false);check(!model.faces.isEmpty(),"Actual model contains surfaces");
    for(var face:model.faces){
      var material=materials.getMaterial(face.material);
      check(material.ambientColor.x()==0&&material.ambientColor.y()==0&&material.ambientColor.z()==0,"Source MTL cannot request emissive ambient: "+face.material);
      var quad=quad(model.model,face,material.ambientColor);quads++;
      check(quad.isShade(),"Actual NeoForge baked quad uses scene shading");
      for(int v=0;v<4;v++)check(quad.getVertices()[v*IQuadTransformer.STRIDE+IQuadTransformer.UV2]==0,"No pre-baked fullbright UV2");
      for(int light:new int[]{0,LightTexture.pack(7,0),LightTexture.pack(0,15),LightTexture.FULL_BRIGHT})
        for(int actual:render(quad,light).lights)check(actual==light,"Actual bulk rendering preserves input light for held/placed model: "+kind+"/"+context);
    }
    // Regression controls run the exact old NeoForge behavior, not a copied arithmetic formula.
    var first=model.faces.getFirst();var old=model(data,text,true);var glowing=quad(old.model,first,new Vector4f(1));
    check(!glowing.isShade(),"Control reproduces old unshaded MTL ambient behavior");
    for(int actual:render(glowing,0).lights)check(actual==LightTexture.FULL_BRIGHT,"Old Ka1/default-emissivity control glows in complete darkness");
    var protectedQuad=quad(model.model,first,new Vector4f(1));
    for(int actual:render(protectedQuad,0).lights)check(actual==0,"Explicit opt-out protects against accidental future Ka1");
  }
  public static void main(String[] args)throws Exception{
    bake=ObjModel.class.getDeclaredMethod("makeQuad",int[][].class,int.class,Vector4f.class,Vector4f.class,TextureAtlasSprite.class,Transformation.class);bake.setAccessible(true);
    try(var tokenizer=new ObjTokenizer(Files.newInputStream(ROOT.resolve("models/active02549/materials.mtl")))){
      var materials=new ObjMaterialLibrary(tokenizer);
      for(String kind:List.of("chao_toy_car","chao_sonic_doll","chao_shadow_doll","chao_pogo_stick"))for(String context:List.of("held","placed"))inspect(kind,context,materials);
    }
    System.out.println("Toy lighting080: "+checks+" checks, "+quads+" real NeoForge baked surfaces, "+renderedVertices+" actual Minecraft bulk vertices. All four active toys, held/placed geometry, darkness/lamp/day/fullbright inputs and old Ka1/default-emissivity regression controls passed. This is a numeric engine check, not a visual shader/listening pass.");
  }
}
