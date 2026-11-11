package com.chaogarden.client;

import com.google.gson.*;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import java.io.IOException;
import java.util.*;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import org.joml.Vector3f;
import org.joml.Matrix4f;

/** Rigged, low-poly geometry exported from the matching editable Blockbench project. */
final class ChaoMeshAsset {
  static final class Pose {
    float x, y, z, rx, ry, rz, sx = 1, sy = 1, sz = 1;
    float proportionY;
    // Forearm reach in model pixels; hand geometry keeps its authored size.
    float reach;
    boolean visible = true;

    void reset() {
      x = y = z = rx = ry = rz = 0;
      proportionY = reach = 0;
      sx = sy = sz = 1;
      visible = true;
    }
  }

  private record Face(int[] indices, float[][] uv, float[][] gameplayUv, Vector3f normal) {}

  private record Mesh(
      String name,
      String material,
      float[][] vertices,
      float[][][] morphVertices,
      List<Face> faces,
      Vector3f[] vertexNormals) {}

  private static final class Bone {
    final String name, parent;
    final float[] pivot, rotation;
    final List<Mesh> meshes;
    final List<Bone> children = new ArrayList<>();

    Bone(String name, String parent, float[] pivot, float[] rotation, List<Mesh> meshes) {
      this.name = name;
      this.parent = parent;
      this.pivot = pivot;
      this.rotation = rotation;
      this.meshes = meshes;
    }
  }

  private final List<Bone> roots;
  private final LinkedHashMap<String, Bone> bones;
  private final float textureWidth, textureHeight;
  private List<String> headDecorations = List.of();
  private List<String> eyeBones = List.of();
  private final Map<String, Float> forwardReaches = new HashMap<>();
  private final float[] developmentWeights = {0,0,0,0,1,0,0,0,0};
  private float developmentStrength=1;
  private int lensColor,expression;
  private final Map<String,float[]> uvOverrides=new HashMap<>();
  private final Map<String,float[]> attachmentLandmarks=new HashMap<>();
  private final Map<String,List<String>> attachmentExtraBones=new HashMap<>();
  private float[] activeUvOverride;
  private String naturalHeroCore;
  private float[] naturalHeroCoreUv;
  private boolean naturalHeroCoreActive;
  private float proportionMarkerYoung;
  private float[] proportionMarkerTargets = new float[9];

  private ChaoMeshAsset(float width, float height, LinkedHashMap<String, Bone> bones) {
    textureWidth = width;
    textureHeight = height;
    this.bones = bones;
    eyeBones=bones.values().stream()
        .filter(bone->bone.meshes.stream().anyMatch(mesh->mesh.material.equals("lens")))
        .map(bone->bone.name).toList();
    roots = new ArrayList<>();
    for(Bone bone:bones.values()) {
      var ancestors=new HashSet<String>();
      for(Bone at=bone;at!=null;at=at.parent==null?null:bones.get(at.parent))
        if(!ancestors.add(at.name))throw new IllegalStateException("Cyclic mesh bone "+at.name);
      for(float value:bone.pivot)if(!Float.isFinite(value))throw new IllegalStateException("Non-finite mesh pivot");
      for(float value:bone.rotation)if(!Float.isFinite(value))throw new IllegalStateException("Non-finite mesh rotation");
    }
    for (Bone b : bones.values()) {
      if (b.parent == null || b.parent.isBlank()) roots.add(b);
      else {
        Bone parent = bones.get(b.parent);
        if (parent == null) throw new IllegalStateException("Unknown mesh bone " + b.parent);
        parent.children.add(b);
      }
    }
  }

  static ChaoMeshAsset load(String file) {
    var id =
        ResourceLocation.fromNamespaceAndPath("chao_garden", "models/entity/" + file + ".json");
    try (var reader = Minecraft.getInstance().getResourceManager().openAsReader(id)) {
      return parse(JsonParser.parseReader(reader).getAsJsonObject());
    } catch (IOException | RuntimeException error) {
      throw new IllegalStateException("Cannot load Chao mesh " + id, error);
    }
  }

  static ChaoMeshAsset parse(JsonObject data) {
      var bones = new LinkedHashMap<String, Bone>();
      for (JsonElement element : data.getAsJsonArray("bones")) {
        JsonObject bone = element.getAsJsonObject();
        String name = bone.get("name").getAsString();
        String parent =
            !bone.has("parent") || bone.get("parent").isJsonNull()
                ? null
                : bone.get("parent").getAsString();
        float[] pivot = vector(bone.getAsJsonArray("pivot"), 3);
        List<Mesh> meshes = new ArrayList<>();
        if (bone.has("meshes"))
          for (JsonElement me : bone.getAsJsonArray("meshes")) {
            JsonObject mesh = me.getAsJsonObject();
            float[][] vertices = vectors(mesh.getAsJsonArray("vertices"), 3);
            float[][][] morphs=null;
            if(mesh.has("morph_vertices")) {
              var values=mesh.getAsJsonArray("morph_vertices");
              if(values.size()!=9)throw new IllegalStateException("Expected nine development endpoints");
              morphs=new float[9][][];
              for(int target=0;target<9;target++) {
                morphs[target]=vectors(values.get(target).getAsJsonArray(),3);
                if(morphs[target].length!=vertices.length)throw new IllegalStateException("Development topology changed");
                for(float[] point:morphs[target])for(float value:point)
                  if(!Float.isFinite(value))throw new IllegalStateException("Non-finite development vertex");
              }
            }
            List<Face> faces = new ArrayList<>();
            for (JsonElement fe : mesh.getAsJsonArray("faces")) {
              JsonObject face = fe.getAsJsonObject();
              JsonArray indices = face.getAsJsonArray("v");
              int[] vi = new int[indices.size()];
              for (int i = 0; i < vi.length; i++) vi[i] = indices.get(i).getAsInt();
              if (vi.length < 3) continue;
              for (int index : vi)
                if (index < 0 || index >= vertices.length)
                  throw new IllegalStateException("Invalid mesh vertex");
              float[][] uv = vectors(face.getAsJsonArray("uv"), 2);
              if (uv.length != vi.length)
                throw new IllegalStateException("Mesh UV/vertex count differs");
              Vector3f a = new Vector3f(vertices[vi[1]]).sub(new Vector3f(vertices[vi[0]]));
              Vector3f b = new Vector3f(vertices[vi[2]]).sub(new Vector3f(vertices[vi[0]]));
              Vector3f normal = a.cross(b);
              if (normal.lengthSquared() < .0000001f) normal.set(0, 0, -1);
              else normal.normalize();
              float[][] gameplayUv=uv;
              if(name.startsWith("emotion_question__")||name.startsWith("emotion_dizzy__")) {
                gameplayUv=new float[uv.length][2];
                for(float[] point:gameplayUv){point[0]=60.5f;point[1]=1.5f;}
              }
              faces.add(new Face(vi, uv, gameplayUv, normal));
            }
            meshes.add(
                new Mesh(
                    mesh.has("name") ? mesh.get("name").getAsString() : name,
                    mesh.has("material") ? mesh.get("material").getAsString() : "body",
                    vertices,
                    morphs,
                    List.copyOf(faces),
                    mesh.has("shading") && mesh.get("shading").getAsString().equals("flat")
                        ? null
                        : smoothNormals(vertices, faces)));
          }
        float[] rotation =
            bone.has("rotation") ? vector(bone.getAsJsonArray("rotation"), 3) : new float[3];
        for (int i = 0; i < 3; i++) rotation[i] *= (float) Math.PI / 180f;
        if(bones.put(name, new Bone(name, parent, pivot, rotation, List.copyOf(meshes)))!=null)
          throw new IllegalStateException("Duplicate mesh bone "+name);
      }
      var result = new ChaoMeshAsset(
          data.get("texture_width").getAsFloat(), data.get("texture_height").getAsFloat(), bones);
      if (data.has("head_decorations")) {
        var values = new ArrayList<String>();
        for (var value : data.getAsJsonArray("head_decorations")) values.add(value.getAsString());
        result.headDecorations = List.copyOf(values);
      }
      if(data.has("attachment_landmarks"))for(var entry:data.getAsJsonObject("attachment_landmarks").entrySet()) {
        var values=entry.getValue().getAsJsonArray();float[] landmark=vector(values,values.size());
        for(float value:landmark)if(!Float.isFinite(value))throw new IllegalStateException("Non-finite attachment landmark");
        result.attachmentLandmarks.put(entry.getKey(),landmark);
      }
      if(data.has("attachment_extra_bones"))for(var entry:data.getAsJsonObject("attachment_extra_bones").entrySet()) {
        var extra=new ArrayList<String>();
        for(var value:entry.getValue().getAsJsonArray()) {
          String name=value.getAsString();
          if(!bones.containsKey(name))throw new IllegalStateException("Unknown attachment envelope bone "+name);
          extra.add(name);
        }
        result.attachmentExtraBones.put(entry.getKey(),List.copyOf(extra));
      }
      if(data.has("natural_hero_core")) {
        var special=data.getAsJsonObject("natural_hero_core");result.naturalHeroCore=special.get("bone").getAsString();
        if(!bones.containsKey(result.naturalHeroCore))throw new IllegalStateException("Unknown natural Hero core");
        result.naturalHeroCoreUv=vector(special.getAsJsonArray("uv"),2);
      }
      if(data.has("proportion_adjustment_022")) {
        var adjustment=data.getAsJsonObject("proportion_adjustment_022");
        result.proportionMarkerYoung=adjustment.get("marker_young_delta").getAsFloat();
        var targets=adjustment.getAsJsonArray("marker_target_deltas");
        if(targets.size()!=9)throw new IllegalStateException("Expected nine crown marker offsets");
        for(int i=0;i<9;i++)result.proportionMarkerTargets[i]=targets.get(i).getAsFloat();
        if(!Float.isFinite(result.proportionMarkerYoung))throw new IllegalStateException("Non-finite crown marker offset");
        for(float offset:result.proportionMarkerTargets)if(!Float.isFinite(offset))throw new IllegalStateException("Non-finite crown marker target");
      }
      return result;
  }

  private static Vector3f[] smoothNormals(float[][] vertices, List<Face> faces) {
    Vector3f[] normals = new Vector3f[vertices.length];
    for (int i = 0; i < normals.length; i++) normals[i] = new Vector3f();
    for (Face face : faces) {
      Vector3f base = new Vector3f(vertices[face.indices[0]]);
      Vector3f weighted =
          new Vector3f(vertices[face.indices[1]])
              .sub(base)
              .cross(new Vector3f(vertices[face.indices[2]]).sub(base));
      for (int index : face.indices) normals[index].add(weighted);
    }
    for (Vector3f normal : normals) {
      if (normal.lengthSquared() > .00000001f) normal.normalize();
      else normal.set(0, 0, -1);
    }
    return normals;
  }

  private static float[] vector(JsonArray a, int size) {
    float[] out = new float[size];
    for (int i = 0; i < size; i++) out[i] = a.get(i).getAsFloat();
    return out;
  }

  private static float[][] vectors(JsonArray a, int size) {
    float[][] out = new float[a.size()][];
    for (int i = 0; i < a.size(); i++) out[i] = vector(a.get(i).getAsJsonArray(), size);
    return out;
  }


  /** Legacy inspection overload samples the fully aged endpoint lattice. */
  void development(float swimFly,float runPower) { development(swimFly,runPower,1); }

  /** Age and reversible opposing training axes are independent blend dimensions. */
  void development(float swimFly,float runPower,float strength) {
    float sf=Float.isFinite(swimFly)?Math.max(-1,Math.min(1,swimFly)):0;
    float rp=Float.isFinite(runPower)?Math.max(-1,Math.min(1,runPower)):0;
    float age=Float.isFinite(strength)?Math.max(0,Math.min(1,strength)):0;
    boolean changed=developmentStrength!=age;developmentStrength=age;
    for(int y=0;y<3;y++)for(int x=0;x<3;x++) {
      int at=y*3+x;float weight=Math.max(0,1-Math.abs(sf-(y-1)))*Math.max(0,1-Math.abs(rp-(x-1)));
      changed|=developmentWeights[at]!=weight;developmentWeights[at]=weight;
    }
    if(changed)forwardReaches.clear();
  }

  private float coordinate(Mesh mesh,int vertex,int axis) {
    float young=mesh.vertices[vertex][axis];
    if(mesh.morphVertices==null||developmentStrength==0)return young;
    float aged=0;for(int target=0;target<9;target++)aged+=mesh.morphVertices[target][vertex][axis]*developmentWeights[target];
    return young+(aged-young)*developmentStrength;
  }

  void naturalHeroCore(boolean enabled) {
    naturalHeroCoreActive=enabled&&naturalHeroCore!=null;
    if(naturalHeroCore!=null){if(naturalHeroCoreActive)uvOverrides.put(naturalHeroCore,naturalHeroCoreUv);else uvOverrides.remove(naturalHeroCore);}
  }
  boolean naturalHeroCoreActive(){return naturalHeroCoreActive;}

  /** Follows the normalized crown using the same independent age/axis blend. */
  float proportionMarkerOffset() {
    float aged=0;
    for(int i=0;i<9;i++)aged+=proportionMarkerTargets[i]*developmentWeights[i];
    return proportionMarkerYoung+(aged-proportionMarkerYoung)*developmentStrength;
  }

  void lens(int color) { lensColor=color; }
  void expression(int value) { expression=value; }
  void clearUvOverrides(){uvOverrides.clear();}
  void overrideBoneUv(String name,float u,float v){uvOverrides.put(name,new float[]{u,v});}

  /** Development probe fingerprint evaluates the very same vertices as rendering. */
  long shapeSignature() {
    long hash=0xcbf29ce484222325L;
    for(Bone bone:bones.values())for(Mesh mesh:bone.meshes)for(int i=0;i<mesh.vertices.length;i++)
      for(int axis=0;axis<3;axis++) {
        float value=coordinate(mesh,i,axis);
        hash=(hash^Float.floatToIntBits(value))*0x100000001b3L;
      }
    return hash;
  }

  Map<String, Pose> createPoses() {
    Map<String, Pose> out = new LinkedHashMap<>();
    bones.keySet().forEach(n -> out.put(n, new Pose()));
    return out;
  }

  float pivot(String name, int axis) {
    Bone bone = bones.get(name);
    return bone == null ? 0 : bone.pivot[axis];
  }

  /** Authored toe reach in the ankle's local frame, cached for gait fitting. */
  float forwardReach(String name) {
    return forwardReaches.computeIfAbsent(name, key -> {
      Bone bone = bones.get(key);
      if (bone == null) return 0f;
      float[] minimum = {0};
      for (Mesh mesh : bone.meshes)
        for (int i=0;i<mesh.vertices.length;i++)minimum[0]=Math.min(minimum[0],coordinate(mesh,i,2));
      for (Bone child : bone.children) forwardReach(child, new Matrix4f(), minimum);
      return -minimum[0];
    });
  }

  private void forwardReach(Bone bone, Matrix4f parent, float[] minimum) {
    var transform = new Matrix4f(parent).translate(bone.pivot[0], bone.pivot[1], bone.pivot[2])
        .rotateZ(bone.rotation[2]).rotateY(bone.rotation[1]).rotateX(bone.rotation[0]);
    var point = new Vector3f();
    for (Mesh mesh : bone.meshes) for (int i=0;i<mesh.vertices.length;i++) {
      transform.transformPosition(coordinate(mesh,i,0),coordinate(mesh,i,1),coordinate(mesh,i,2),point);
      minimum[0] = Math.min(minimum[0], point.z);
    }
    for (Bone child : bone.children) forwardReach(child, transform, minimum);
  }

  void hideHeadDecorations(Map<String,Pose> poses) {
    for (String name : headDecorations) setSubtreeVisible(name, poses, false);
  }

  List<String> headDecorations() { return headDecorations; }

  void setSubtreeVisible(String name, Map<String, Pose> poses, boolean visible) {
    Bone bone = bones.get(name);
    if (bone != null) setSubtreeVisible(bone, poses, visible);
  }

  private static void setSubtreeVisible(Bone bone, Map<String, Pose> poses, boolean visible) {
    poses.get(bone.name).visible = visible;
    for (Bone child : bone.children) setSubtreeVisible(child, poses, visible);
  }

  void render(
      PoseStack stack,
      VertexConsumer consumer,
      int light,
      int overlay,
      int color,
      Map<String, Pose> poses) {
    render(stack,consumer,light,overlay,color,poses,false);
  }

  void render(PoseStack stack,VertexConsumer consumer,int light,int overlay,int color,
              Map<String,Pose> poses,boolean gameplayUv) {
    Vector3f normal=new Vector3f(),vertex=new Vector3f();
    for(Bone root:roots)renderBone(root,stack,consumer,light,overlay,color,poses,normal,vertex,gameplayUv);
  }

  /** Apply an attachment's complete ancestor chain using the same transforms as rendering. */
  void transformBone(String name, PoseStack stack, Map<String,Pose> poses) {
    Bone bone=bones.get(name);
    if(bone==null)return;
    var chain=new ArrayDeque<Bone>();
    for(Bone at=bone;at!=null;at=at.parent==null?null:bones.get(at.parent))chain.push(at);
    while(!chain.isEmpty())applyTransform(chain.pop(),stack,poses);
  }

  /** Actual deformed palm, including the hand cube's local transform; arm-local pixels. */
  float[] rattlePalm(Map<String,Pose> poses) { return toyPalm(poses,false); }
  float[] toyPalm(Map<String,Pose> poses,boolean left) {
    String name=left?"arm_left":"arm_right";
    Bone arm=bones.get(name), hand=bones.get(name+"__cube_1");
    if(arm==null||hand==null||!poses.get(arm.name).visible||!poses.get(hand.name).visible)return null;
    float[] bounds=localBounds(hand,arm,poses);
    if(bounds==null) {
      // Authored Chaos forms end at the visible arm; their old hand node is
      // intentionally empty. Place the grip inside that actual distal tip.
      Bone tip=bones.get(name+"__cube_0");
      if(tip==null||!poses.get(tip.name).visible)return null;
      bounds=localBounds(tip,arm,poses);if(bounds==null)return null;
      float inset=Math.min(.25f,(bounds[3]-bounds[0])*.15f);
      return new float[]{left?bounds[3]-inset:bounds[0]+inset,
          (bounds[1]+bounds[4])*.5f,(bounds[2]+bounds[5])*.5f};
    }
    return new float[]{(bounds[0]+bounds[3])*.5f,(bounds[1]+bounds[4])*.5f,(bounds[2]+bounds[5])*.5f};
  }

  /** Measured visible hand when a terminal material layer supplies it. */
  float[] visiblePalm(Map<String,Pose> poses,boolean left) {
    String name=left?"arm_left":"arm_right";Bone hand=bones.get("chaos_hand_"+name),arm=bones.get(name);
    float[] b=hand==null||arm==null?null:localBounds(hand,arm,poses);
    return b==null?toyPalm(poses,left):new float[]{(b[0]+b[3])*.5f,(b[1]+b[4])*.5f,(b[2]+b[5])*.5f};
  }
  float[] visibleHandBounds(Map<String,Pose> poses,boolean left) {
    String name=left?"arm_left":"arm_right";Bone hand=bones.get("chaos_hand_"+name),arm=bones.get(name);
    float[] b=hand==null||arm==null?null:localBounds(hand,arm,poses);
    return b==null?toyHandBounds(poses,left):b;
  }

  /** Rendered palm volume in the arm's local space, for attachment fitting and QA. */
  float[] toyHandBounds(Map<String,Pose> poses,boolean left){
    String name=left?"arm_left":"arm_right";Bone arm=bones.get(name),hand=bones.get(name+"__cube_1");
    if(arm==null||hand==null)return null;float[] bounds=localBounds(hand,arm,poses);
    return bounds!=null?bounds:localBounds(bones.get(name+"__cube_0"),arm,poses);
  }
  float[] animalHandBounds(int animal,Map<String,Pose> poses,boolean left){
    String side=left?"left":"right";Bone hand=bones.get("animal_0_"+animal+"_"+side),arm=bones.get("arm_"+side);
    return hand==null||arm==null?null:localBounds(hand,arm,poses,null,Set.of("gene_hand","paw","hoof","claw"));
  }

  /** Replacement hand centre in the same arm-local space as the skin socket. */
  float[] animalPalm(int animal,Map<String,Pose> poses) { return animalPalm(animal,poses,false); }
  float[] animalPalm(int animal,Map<String,Pose> poses,boolean left) {
    String side=left?"left":"right";
    Bone hand=bones.get("animal_0_"+animal+"_"+side),arm=bones.get("arm_"+side);
    if(hand==null||arm==null||!poses.get(hand.name).visible)return null;
    float[] bounds=localBounds(hand,arm,poses,null,Set.of("gene_hand","paw","hoof","claw"));
    return bounds==null?null:new float[]{(bounds[0]+bounds[3])*.5f,(bounds[1]+bounds[4])*.5f,(bounds[2]+bounds[5])*.5f};
  }

  /** Complete visible geometry below a joint, including authored Chaos feet. */
  float[] partBounds(String anchor,Map<String,Pose> poses) {
    Bone joint=bones.get(anchor);if(joint==null)return null;float[] result=null;
    for(Bone bone:bones.values()) {
      boolean belongs=false,visible=true;
      for(Bone at=bone;at!=null;at=at.parent==null?null:bones.get(at.parent)) {
        if(!poses.get(at.name).visible)visible=false;
        if(at==joint){belongs=true;break;}
      }
      if(!belongs||!visible)continue;
      float[] b=localBounds(bone,joint,poses,null);if(b==null)continue;
      if(result==null)result=b;else for(int a=0;a<3;a++){result[a]=Math.min(result[a],b[a]);result[a+3]=Math.max(result[a+3],b[a+3]);}
    }
    return result;
  }

  /** Current skin bounds in the requested joint's local pixels, not model/world coordinates. */
  float[] attachmentBounds(String anchor,Map<String,Pose> poses) {
    Bone joint=bones.get(anchor);if(joint==null)return null;
    Bone core=null;double largest=-1;
    for(Bone candidate:bones.values()) {
      if(!candidate.name.startsWith(anchor+"__cube_"))continue;
      // Sleeves cover the upper arm, never the larger hand at cube1.
      if(anchor.startsWith("arm_")&&!candidate.name.equals(anchor+"__cube_0"))continue;
      float[] bounds=localBounds(candidate,joint,poses);
      if(bounds==null)continue;
      double volume=(bounds[3]-bounds[0])*(bounds[4]-bounds[1])*(bounds[5]-bounds[2]);
      if(volume>largest){largest=volume;core=candidate;}
    }
    float[] bounds=core==null?null:localBounds(core,joint,poses);
    for(String name:attachmentExtraBones.getOrDefault(anchor,List.of())) {
      float[] extra=localBounds(bones.get(name),joint,poses);if(extra==null)continue;
      if(bounds==null)bounds=extra;
      else for(int axis=0;axis<3;axis++) {
        bounds[axis]=Math.min(bounds[axis],extra[axis]);bounds[axis+3]=Math.max(bounds[axis+3],extra[axis+3]);
      }
    }
    return bounds;
  }

  /** Mouth centre measured from this form's actual expression geometry. */
  float[] attachmentMouth(Map<String,Pose> poses) {
    Bone skull=bones.get("skull");if(skull==null)return null;
    for(String name:List.of("expression_mouth_6","expression_mouth_1","character_robot_mouth")) {
      Bone mouth=bones.get(name);if(mouth==null)continue;
      float[] b=localBounds(mouth,skull,poses);
      if(b!=null)return new float[]{(b[0]+b[3])*.5f,(b[1]+b[4])*.5f,b[2]};
    }
    // Chaos forms have no separate mouth mesh; use their authored face plane.
    float[] eyes=attachmentEyes(poses);
    return eyes==null?null:new float[]{eyes[0],eyes[1]+1.6f,eyes[2]-.15f};
  }

  /** Eye centre, front plane and half-spacing in skull-local pixels. */
  void eyesVisible(Map<String,Pose> poses,boolean visible) {
    for(String name:eyeBones)setSubtreeVisible(name,poses,visible);
  }

  /** Eye centre, front plane and half-spacing in skull-local pixels. */
  float[] attachmentEyes(Map<String,Pose> poses) {
    var authored=attachmentLandmarks.get("eyes");
    if(authored!=null&&authored.length==4)return authored.clone();
    Bone joint=bones.get("skull");if(joint==null)return null;
    var eyes=new ArrayList<float[]>();
    for(Bone bone:bones.values()) {
      if(bone.meshes.stream().noneMatch(mesh->mesh.material.equals("lens")))continue;
      float[] bounds=localBounds(bone,joint,poses,"lens");if(bounds!=null)eyes.add(bounds);
    }
    if(eyes.isEmpty())return null;
    float x=0,y=0,z=Float.POSITIVE_INFINITY;
    for(float[] bounds:eyes){x+=(bounds[0]+bounds[3])*.5f;y+=(bounds[1]+bounds[4])*.5f;z=Math.min(z,bounds[2]);}
    x/=eyes.size();y/=eyes.size();float half=0;
    for(float[] bounds:eyes)half+=Math.abs((bounds[0]+bounds[3])*.5f-x);
    return new float[]{x,y,z,half/eyes.size()};
  }

  private float[] localBounds(Bone core,Bone joint,Map<String,Pose> poses) {
    return localBounds(core,joint,poses,null);
  }

  private float[] localBounds(Bone core,Bone joint,Map<String,Pose> poses,String material) {
    return localBounds(core,joint,poses,material,null);
  }

  private float[] localBounds(Bone core,Bone joint,Map<String,Pose> poses,String material,Set<String> meshNames) {
    var chain=new ArrayDeque<Bone>();
    for(Bone at=core;at!=joint;at=at.parent==null?null:bones.get(at.parent)) {
      if(at==null)return null;chain.push(at);
    }
    Matrix4f matrix=new Matrix4f();
    while(!chain.isEmpty()) {
      Bone bone=chain.pop();Pose p=poses.get(bone.name);if(p==null)p=new Pose();
      matrix.translate(bone.pivot[0]+p.x,bone.pivot[1]+p.y,bone.pivot[2]+p.z)
          .rotateZ(bone.rotation[2]+p.rz).rotateY(bone.rotation[1]+p.ry).rotateX(bone.rotation[0]+p.rx)
          .scale(p.sx,p.sy,p.sz);
    }
    float[] bounds={Float.POSITIVE_INFINITY,Float.POSITIVE_INFINITY,Float.POSITIVE_INFINITY,
        Float.NEGATIVE_INFINITY,Float.NEGATIVE_INFINITY,Float.NEGATIVE_INFINITY};
    boolean any=false;Vector3f point=new Vector3f();
    for(Mesh mesh:core.meshes) {
      if(material!=null&&!mesh.material.equals(material))continue;
      if(meshNames!=null&&!meshNames.contains(mesh.name))continue;
      Matrix4f fitted=new Matrix4f(matrix);
      Matrix4f reach=armReachTransform(core,mesh,poses);if(reach!=null)fitted.mul(reach);
      for(int i=0;i<mesh.vertices.length;i++) {
      float x=coordinate(mesh,i,0),y=coordinate(mesh,i,1),z=coordinate(mesh,i,2);
      fitted.transformPosition(x,y,z,point);any=true;
      for(int axis=0;axis<3;axis++) {
        float value=axis==0?point.x:axis==1?point.y:point.z;
        bounds[axis]=Math.min(bounds[axis],value);bounds[axis+3]=Math.max(bounds[axis+3],value);
      }
    }
    }
    return any?bounds:null;
  }

  /** Extend only the forearm; move the separate palm without changing its shape. */
  private Matrix4f armReachTransform(Bone bone,Mesh mesh,Map<String,Pose> poses) {
    Bone arm=bone;var chain=new ArrayDeque<Bone>();
    while(arm!=null&&!arm.name.equals("arm_left")&&!arm.name.equals("arm_right")) {
      chain.push(arm);arm=arm.parent==null?null:bones.get(arm.parent);
    }
    if(arm==null||poses.get(arm.name).reach<=0)return null;
    boolean hand=bone.name.equals(arm.name+"__cube_1")||bone.name.equals("chaos_hand_"+arm.name)||Set.of("gene_hand","paw","hoof","claw").contains(mesh.name);
    boolean forearm=bone.name.equals(arm.name+"__cube_0")||mesh.name.equals("gene_arm_connector")||mesh.name.equals("arm");
    if(!hand&&!forearm)return null;
    Matrix4f toArm=new Matrix4f();
    while(!chain.isEmpty()) {
      Bone at=chain.pop();Pose p=poses.get(at.name);
      toArm.translate(at.pivot[0]+p.x,at.pivot[1]+p.y,at.pivot[2]+p.z)
          .rotateZ(at.rotation[2]+p.rz).rotateY(at.rotation[1]+p.ry).rotateX(at.rotation[0]+p.rx).scale(p.sx,p.sy,p.sz);
    }
    float reach=poses.get(arm.name).reach;Matrix4f stretch=new Matrix4f();
    if(hand)stretch.translate(arm.name.equals("arm_left")?reach:-reach,0,0);
    else {
      float end=0;Vector3f v=new Vector3f();
      for(int i=0;i<mesh.vertices.length;i++) {toArm.transformPosition(coordinate(mesh,i,0),coordinate(mesh,i,1),coordinate(mesh,i,2),v);end=Math.max(end,Math.abs(v.x));}
      if(end<.01f)return null;stretch.scale(1+reach/end,1,1);
    }
    return new Matrix4f(toArm).invert().mul(stretch).mul(toArm);
  }

  private static void applyTransform(Bone bone,PoseStack stack,Map<String,Pose> poses) {
    Pose p=poses.get(bone.name);
    stack.translate((bone.pivot[0]+p.x)/16f,(bone.pivot[1]+p.y)/16f,(bone.pivot[2]+p.z)/16f);
    float rz=p.rz+bone.rotation[2],ry=p.ry+bone.rotation[1],rx=p.rx+bone.rotation[0];
    if(rz!=0)stack.mulPose(Axis.ZP.rotation(rz));
    if(ry!=0)stack.mulPose(Axis.YP.rotation(ry));
    if(rx!=0)stack.mulPose(Axis.XP.rotation(rx));
    if(p.sx!=1||p.sy!=1||p.sz!=1)stack.scale(p.sx,p.sy,p.sz);
  }

  private void renderBone(
      Bone bone,
      PoseStack stack,
      VertexConsumer consumer,
      int light,
      int overlay,
      int color,
      Map<String, Pose> poses,
      Vector3f normal,
      Vector3f vertex,
      boolean gameplayUv) {
    Pose p = poses.get(bone.name);
    if ((!p.visible && bone.children.isEmpty()) || p.sx == 0 || p.sy == 0 || p.sz == 0) return;
    stack.pushPose();
    applyTransform(bone,stack,poses);
    if (p.visible) {
      for (Mesh mesh : bone.meshes) {
        activeUvOverride=uvOverrides.get(bone.name);
        if(mesh.material.equals("lens") && lensColor==0)continue;
        if(mesh.material.startsWith("expression_") && !mesh.material.equals("expression_"+expression))continue;
        stack.pushPose();
        Matrix4f reach=armReachTransform(bone,mesh,poses);
        if(reach!=null){reach.m30(reach.m30()/16).m31(reach.m31()/16).m32(reach.m32()/16);stack.mulPose(reach);}
        var matrix=stack.last();
        int meshColor=mesh.material.equals("lens")?lensColor:color;
        int meshLight = isLuminous(mesh.material) ? 0x00f000f0 : light;
        for (Face face : mesh.faces) {
          matrix.transformNormal(face.normal, normal);
          // Minecraft entity buffers use quads. A repeated last vertex encodes a triangle.
          if (face.indices.length <= 4)
            emitFace(
                face,
                mesh,
                0,
                1,
                2,
                face.indices.length == 4 ? 3 : 2,
                matrix,
                consumer,
                meshLight,
                overlay,
                meshColor,
                normal,
                vertex,
                gameplayUv);
          else
            for (int i = 1; i < face.indices.length - 1; i++)
              emitFace(
                  face, mesh, 0, i, i + 1, i + 1, matrix, consumer, meshLight, overlay, meshColor,
                  normal, vertex, gameplayUv);
        }
        stack.popPose();
      }
    }
    // A replacement limb can remain visible when its parent's base mesh is hidden.
    for (Bone child : bone.children)
      renderBone(child, stack, consumer, light, overlay, color, poses, normal, vertex, gameplayUv);
    stack.popPose();
  }

  private static boolean isLuminous(String material) {
    return material.equals("orb")
        || material.equals("halo")
        || material.equals("dark_orb")
        || material.equals("heart")
        || material.equals("flame");
  }

  private void emitFace(
      Face face,
      Mesh mesh,
      int a,
      int b,
      int c,
      int d,
      PoseStack.Pose pose,
      VertexConsumer consumer,
      int light,
      int overlay,
      int color,
      Vector3f normal,
      Vector3f vertex,
      boolean gameplayUv) {
    emitVertex(face, mesh, a, pose, consumer, light, overlay, color, normal, vertex, gameplayUv);
    emitVertex(face, mesh, b, pose, consumer, light, overlay, color, normal, vertex, gameplayUv);
    emitVertex(face, mesh, c, pose, consumer, light, overlay, color, normal, vertex, gameplayUv);
    emitVertex(face, mesh, d, pose, consumer, light, overlay, color, normal, vertex, gameplayUv);
  }

  private void emitVertex(
      Face face,
      Mesh mesh,
      int index,
      PoseStack.Pose pose,
      VertexConsumer consumer,
      int light,
      int overlay,
      int color,
      Vector3f normal,
      Vector3f vertex,
      boolean gameplayUv) {
    float[] v = mesh.vertices[face.indices[index]];
    if (mesh.vertexNormals != null)
      pose.transformNormal(mesh.vertexNormals[face.indices[index]], normal);
    int at=face.indices[index];
    pose.pose().transformPosition(coordinate(mesh,at,0)/16f,coordinate(mesh,at,1)/16f,coordinate(mesh,at,2)/16f,vertex);
    float[] uv = activeUvOverride!=null?activeUvOverride:(gameplayUv?face.gameplayUv:face.uv)[index];
    consumer.addVertex(
        vertex.x,
        vertex.y,
        vertex.z,
        color,
        uv[0] / textureWidth,
        uv[1] / textureHeight,
        overlay,
        light,
        normal.x,
        normal.y,
        normal.z);
  }
}
