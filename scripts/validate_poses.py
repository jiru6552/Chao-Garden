#!/usr/bin/env python3
"""Sweep the real ChaoModel.setupAnim against the shipped head/limb geometry.

The unmodified production Java model is compiled in a temporary directory with
small Minecraft API stubs. Those stubs supply entity state and record its Pose
objects; no animation formula is copied into this test. This is a geometry
regression check, not a substitute for rendering the mod in Minecraft.
"""
from __future__ import annotations

import argparse
import json
import math
import os
from pathlib import Path
import sys
sys.path.insert(0,str(Path(__file__).resolve().parents[1]/"art"))
from runtime_resource_contract_022 import prototype_path, ATTACHMENT_LAYOUTS
import shutil
import struct
import subprocess
import tempfile
from validate_head_morphs import profiles_for

ROOT = Path(__file__).resolve().parents[1]
ACTIVITIES = ("idle", "walk", "eat", "pet", "swim", "sleep", "punch")


STUBS = {
    "com/chaogarden/core/ChaoState.java": """package com.chaogarden.core;
public final class ChaoState {
 public static final int ARMS=0,FEET=4,TAIL=6,WINGS=7;
 public boolean adult,immortal,ghostBody,flameOrb;
 public String variant="normal";
 public float align; public int primaryType,secondaryType;
 public int[] animalParts={-1,-1,-1,-1,-1,-1,-1,-1};
}
""",
    "com/chaogarden/ChaoVariants.java": """package com.chaogarden;
import com.chaogarden.core.ChaoState;
public final class ChaoVariants {
 public static final String NORMAL="normal";
 private static final java.util.Set<String> MORPHED=__MORPHED__;
 private static final java.util.Set<String> KNOWN=__KNOWN__;
 public static boolean hasMorphedHead(String id){return MORPHED.contains(id);}
 // Availability is tested independently against the runtime catalog/ModList.
 // This geometry probe makes each catalog theme active without loading mods.
 public static String activeId(ChaoState state){return KNOWN.contains(state.variant)?state.variant:NORMAL;}
}
""",
    "com/chaogarden/entity/ChaoEntity.java": """package com.chaogarden.entity;
import com.chaogarden.core.ChaoState;
public final class ChaoEntity {
 public ChaoState state=new ChaoState();
 public boolean rest,eating,cocoon,petted,water;
 public int affection=20;
 public float attack;
 public ChaoState state(){return state;}
 public boolean isResting(){return rest;}
 public boolean isEating(){return eating;}
 public boolean isCocooned(){return cocoon;}
 public boolean isBeingPetted(){return petted;}
 public boolean isInWater(){return water;}
 public int getAffectionTicks(){return affection;}
 public float getAttackAnim(float partialTick){return attack;}
 public int getId(){return 7;}
}
""",
    "com/mojang/blaze3d/vertex/PoseStack.java": "package com.mojang.blaze3d.vertex; public final class PoseStack {}",
    "com/mojang/blaze3d/vertex/VertexConsumer.java": "package com.mojang.blaze3d.vertex; public interface VertexConsumer {}",
    "net/minecraft/client/model/geom/ModelPart.java": "package net.minecraft.client.model.geom; public final class ModelPart {}",
    "net/minecraft/resources/ResourceLocation.java": """package net.minecraft.resources;
public final class ResourceLocation {
 public static ResourceLocation fromNamespaceAndPath(String a,String b){return new ResourceLocation();}
}
""",
    "net/minecraft/client/model/geom/ModelLayerLocation.java": """package net.minecraft.client.model.geom;
import net.minecraft.resources.ResourceLocation;
public record ModelLayerLocation(ResourceLocation id,String layer){}
""",
    "net/minecraft/client/model/geom/builders/MeshDefinition.java": "package net.minecraft.client.model.geom.builders; public final class MeshDefinition {}",
    "net/minecraft/client/model/geom/builders/LayerDefinition.java": """package net.minecraft.client.model.geom.builders;
public final class LayerDefinition {
 public static LayerDefinition create(MeshDefinition m,int w,int h){return new LayerDefinition();}
}
""",
    "net/minecraft/client/model/HierarchicalModel.java": """package net.minecraft.client.model;
import net.minecraft.client.model.geom.ModelPart;
import com.mojang.blaze3d.vertex.*;
public abstract class HierarchicalModel<T> {
 public abstract ModelPart root();
 public abstract void setupAnim(T e,float swing,float amount,float age,float yaw,float pitch);
 public abstract void renderToBuffer(PoseStack p,VertexConsumer v,int light,int overlay,int color);
}
""",
    "net/minecraft/util/Mth.java": """package net.minecraft.util;
public final class Mth {
 public static final float PI=(float)Math.PI,DEG_TO_RAD=PI/180f;
 private static final float[] SIN=new float[65536];
 static {for(int i=0;i<SIN.length;i++)SIN[i]=(float)Math.sin(i*Math.PI*2/65536);}
 public static float sin(float x){return SIN[(int)(x*10430.378f)&65535];}
 public static float cos(float x){return SIN[(int)(x*10430.378f+16384f)&65535];}
 public static float clamp(float x,float lo,float hi){return Math.min(hi,Math.max(lo,x));}
}
""",
}


def dot(a, b):
    return sum(x*y for x, y in zip(a, b))


def sub(a, b):
    return tuple(x-y for x, y in zip(a, b))


def cross(a, b):
    return (a[1]*b[2]-a[2]*b[1], a[2]*b[0]-a[0]*b[2], a[0]*b[1]-a[1]*b[0])


def unit(v):
    length = math.sqrt(dot(v, v))
    return tuple(x/length for x in v) if length > 1e-9 else None


def matmul(a, b):
    return [[sum(a[i][k]*b[k][j] for k in range(4)) for j in range(4)] for i in range(4)]


def transform(m, p):
    return tuple(sum(m[i][j]*p[j] for j in range(3))+m[i][3] for i in range(3))


def bone_matrix(bone, pose):
    x, y, z, rx, ry, rz, sx, sy, sz, _ = pose
    rx, ry, rz = [r+math.radians(b) for r, b in zip((rx, ry, rz), bone.get("rotation", (0, 0, 0)))]
    cx, cy, cz, ax, ay, az = math.cos(rx), math.cos(ry), math.cos(rz), math.sin(rx), math.sin(ry), math.sin(rz)
    # Match ChaoMeshAsset.renderBone: translation, Z rotation, Y, X, scale.
    out = [[1, 0, 0, bone["pivot"][0]+x], [0, 1, 0, bone["pivot"][1]+y],
           [0, 0, 1, bone["pivot"][2]+z], [0, 0, 0, 1]]
    for other in (
        [[cz, -az, 0, 0], [az, cz, 0, 0], [0, 0, 1, 0], [0, 0, 0, 1]],
        [[cy, 0, ay, 0], [0, 1, 0, 0], [-ay, 0, cy, 0], [0, 0, 0, 1]],
        [[1, 0, 0, 0], [0, cx, -ax, 0], [0, ax, cx, 0], [0, 0, 0, 1]],
        [[sx, 0, 0, 0], [0, sy, 0, 0], [0, 0, sz, 0], [0, 0, 0, 1]],
    ):
        out = matmul(out, other)
    return out


def shape(mesh, matrix):
    """Convex support bounds around all vertices of a saved surface.

    Cuboids use their three independent edges. Sculpted heads use every face
    normal and edge direction; the resulting hull is conservative around a
    concave crown. Separation proves clearance of the actual saved geometry.
    """
    vertices = [transform(matrix, p) for p in mesh["vertices"]]
    if "_collision_edges" not in mesh:
        edges, triangles = set(), []
        for face in mesh["faces"]:
            indices = face["v"]
            edges.update(tuple(sorted((a, b))) for a, b in zip(indices, indices[1:]+indices[:1]))
            triangles.extend((indices[0], indices[index], indices[index+1])
                             for index in range(1, len(indices)-1))
        if mesh.get("primitive") == "cube":
            if len(vertices) != 8:
                raise RuntimeError(f"Invalid cuboid corner count: {mesh['name']}")
            neighbors = sorted(b if a == 0 else a for a, b in edges if a == 0 or b == 0)
            if len(neighbors) != 3:
                raise RuntimeError(f"Invalid cuboid corner topology: {mesh['name']}")
            edges = {(0, index) for index in neighbors}
        mesh["_collision_edges"] = sorted(edges)
        mesh["_collision_triangles"] = triangles
    edges = [sub(vertices[b], vertices[a]) for a, b in mesh["_collision_edges"]]
    normals = [cross(sub(vertices[b], vertices[a]), sub(vertices[c], vertices[a]))
               for a, b, c in mesh["_collision_triangles"]]
    bounds = [(min(p[i] for p in vertices), max(p[i] for p in vertices)) for i in range(3)]
    return vertices, edges, normals, bounds


def separation(a, b):
    """Convex SAT, conservative for the concave crown of an evolved head.

    Positive means separated along at least one separating axis; negative means
    the least translation needed to stop intersecting on one of those axes.
    This is a clearance lower bound, not Euclidean closest-point distance.
    """
    av, ae, an, ab = a
    bv, be, bn, bb = b
    # The normal resting hands sit below the head. A world-axis separation is
    # already a valid proof and avoids the more expensive oblique-axis sweep.
    broad_gap = max(max(bb[i][0]-ab[i][1], ab[i][0]-bb[i][1]) for i in range(3))
    if broad_gap > .05:
        return broad_gap
    raw_axes = an + bn + [cross(ea, eb) for ea in ae for eb in be]
    axes = [axis for candidate in raw_axes if (axis := unit(candidate))]
    return max(max(min(dot(p, axis) for p in bv)-max(dot(p, axis) for p in av),
                   min(dot(p, axis) for p in av)-max(dot(p, axis) for p in bv)) for axis in axes)


def collision_regressions():
    """Independent wedge/box cases exercise the new generic surface path."""
    identity = [[1, 0, 0, 0], [0, 1, 0, 0], [0, 0, 1, 0], [0, 0, 0, 1]]
    wedge = {"name": "independent triangular prism",
             "vertices": [[0, 0, -1], [2, 0, -1], [0, 2, -1],
                          [0, 0, 1], [2, 0, 1], [0, 2, 1]],
             "faces": [{"v": ids} for ids in
                       ([0, 2, 1], [3, 4, 5], [0, 1, 4, 3],
                        [1, 2, 5, 4], [2, 0, 3, 5])]}
    box = {"name": "independent small box", "primitive": "cube",
           "vertices": [[x, y, z] for z in (-.1, .1) for y in (-.1, .1) for x in (-.1, .1)],
           "faces": [{"v": ids} for ids in
                     ([0, 1, 3, 2], [4, 6, 7, 5], [0, 4, 5, 1],
                      [2, 3, 7, 6], [0, 2, 6, 4], [1, 5, 7, 3])]}
    head = shape(wedge, identity)
    for position, separated in (((.3, .3, 0), False), ((1.5, 1.5, 0), True), ((5, 0, 0), True)):
        matrix = [row[:] for row in identity]
        for axis, value in enumerate(position):
            matrix[axis][3] = value
        gap = separation(head, shape(box, matrix))
        if (gap > 0) != separated:
            raise RuntimeError(f"Generic head collision regression failed at {position}: {gap}")
    return 3


def java_executable(name):
    configured = Path(os.environ.get("JAVA_HOME", ""))/"bin"/name
    return str(configured) if configured.is_file() else shutil.which(name)


def sample_poses(root, bones, names, temporary, variants, selected_forms,
                 quick=False, activities=ACTIVITIES):
    """Compile the *current* production source, then return its sampled poses."""
    sources = dict(STUBS)
    sources["com/chaogarden/core/ChaoState.java"] = (root/"src/main/java/com/chaogarden/core/ChaoState.java").read_text()
    sources["com/chaogarden/ChaoVariants.java"] = sources["com/chaogarden/ChaoVariants.java"] \
        .replace("__MORPHED__", "java.util.Set.of("+",".join(json.dumps(n) for n in profiles_for(root))+")") \
        .replace("__KNOWN__", "java.util.Set.of("+",".join(json.dumps(n) for n in variants)+")")
    sources["com/chaogarden/client/ChaoModel.java"] = (root/"src/main/java/com/chaogarden/client/ChaoModel.java").read_text()
    sources["com/chaogarden/client/ChaoMeshAsset.java"] = """package com.chaogarden.client;
import java.util.*;import com.mojang.blaze3d.vertex.*;
final class ChaoMeshAsset {
 static final class Pose {
  float x,y,z,rx,ry,rz,sx=1,sy=1,sz=1;boolean visible=true;
  void reset(){x=y=z=rx=ry=rz=0;sx=sy=sz=1;visible=true;}
 }
 static ChaoMeshAsset load(String name){return new ChaoMeshAsset();}
 Map<String,Pose> createPoses(){var out=new LinkedHashMap<String,Pose>();
  for(String name:NAMES)out.put(name,new Pose());return out;}
 void render(PoseStack s,VertexConsumer v,int l,int o,int c,Map<String,Pose> p){}
 static final String[] NAMES=__NAMES__;
}
""".replace("__NAMES__", "new String[]{"+",".join(json.dumps(n) for n in bones)+"}")
    sources["com/chaogarden/client/PoseProbe.java"] = """package com.chaogarden.client;
import java.util.*;import java.io.*;import com.chaogarden.entity.*;
import net.minecraft.client.model.geom.ModelPart;
public final class PoseProbe {
 public static void main(String[] args)throws Exception {
  var out=new DataOutputStream(new BufferedOutputStream(new FileOutputStream(args[0])));
  var model=new ChaoModel(new ModelPart());
  var field=ChaoModel.class.getDeclaredField("poses");field.setAccessible(true);
  @SuppressWarnings("unchecked") var poses=(Map<String,ChaoMeshAsset.Pose>)field.get(model);
  String[] names=__NAMES__;
  String[] themes=__THEMES__;
  int[] forms=__FORMS__;
  int animalBase=19+themes.length;
  int mixedBase=animalBase+42;
  int[] times=Boolean.parseBoolean(args[1])?new int[]{0,2,6,10,18,23,24}:java.util.stream.IntStream.range(0,25).toArray();
  int[] modes=Arrays.stream(args[2].split(",")).mapToInt(Integer::parseInt).toArray();
  for(int form:forms)for(int mode:modes)
  for(int time:times)for(int yaw=-90;yaw<=90;yaw+=30)
  for(int pitch=-60;pitch<=60;pitch+=60){
   var e=new ChaoEntity();e.state.adult=form!=0;
   if(form>=1&&form<=15){e.state.primaryType=(form-1)%5;e.state.align=(form-1)/5-1;}
   if(form>=16&&form<19){e.state.immortal=true;e.state.align=form==17?1:form==18?-1:0;}
   if(form>=19&&form<animalBase){e.state.variant=themes[form-19];e.state.primaryType=4;
     e.state.secondaryType=1;e.state.align=(form-19)%3-1;}
   if(form>=animalBase){
     e.state.adult=form<mixedBase?form<animalBase+21:(form-mixedBase)%2==0;
     e.state.primaryType=4;e.state.secondaryType=1;
     if(form<mixedBase){for(int sample=0;sample<8;sample++)e.state.giveAnimal((form-animalBase)%21);}
     else {
       int[][] sequences={{6,3},{3,13},{3,6,15}};
       for(int gene:sequences[(form-mixedBase)/2])
         for(int sample=0;sample<8;sample++)e.state.giveAnimal(gene);
     }
   }
   e.rest=mode==5;e.eating=mode==2;e.petted=mode==3;e.water=mode==4;
   e.attack=mode==6?time/24f:0;
   e.affection=40-(time*3%40);
   float age=time*15f;model.setupAnim(e,time*.4f,mode==1?1:0,age,yaw,pitch);
   if(!poses.get("head").visible)
    throw new AssertionError("The full main head cube must remain visible beneath block crowns: "+e.state.variant);
   if(mode==6 && time==6 && (poses.get("right_arm").rx>-.9f
       ||poses.get("right_arm").z>-.9f||poses.get("right_arm").y<.7f))
    throw new AssertionError("Synchronized attack lost the forward, low-shoulder punch");
   if(form>=19&&form<animalBase){
    String active="variant_"+e.state.variant+"_";
    poses.forEach((name,p)->{
     boolean expected=name.startsWith(active)&&(!name.endsWith("_marker")||mode!=2&&mode!=3);
     if(name.startsWith("variant_")&&p.visible!=expected)
      throw new AssertionError("Themed group visibility differs from active heritage: "+name);
     if((name.equals("tuft")||name.equals("hero_crest")||name.equals("dark_crest")
         ||name.startsWith("crest_"))&&p.visible)
      throw new AssertionError("An ordinary crest overlaps a themed crest: "+name);
    });
   }
   out.writeInt(form);out.writeInt(mode);out.writeInt(time);out.writeInt(yaw);out.writeInt(pitch);
   for(String name:names){var p=poses.get(name);
    for(float f:new float[]{p.x,p.y,p.z,p.rx,p.ry,p.rz,p.sx,p.sy,p.sz})out.writeFloat(f);
    out.writeBoolean(p.visible);
   }
   // Probe each acquired gene through actual production state, including a
   // themed coat and a Chaos precedence pass. No visibility formula is copied.
   if(form>=animalBase){
    int gene=form<mixedBase?(form-animalBase)%21:-1;
    int[] suppliedBones={5,3,7,6,6,6,6,6,7,5,6,6,6,7,5,9,7,6,0,0,0};
    long visibleParts=poses.entrySet().stream().filter(entry->entry.getKey().startsWith("animal_")&&entry.getValue().visible).count();
    int expectedParts=gene>=0?suppliedBones[gene]:new int[]{9,10,10}[(form-mixedBase)/2];
    if(visibleParts!=expectedParts)throw new AssertionError("Gene supplied slots missing or fabricated: "+gene+" count="+visibleParts);
    if(gene<0){
      int[][] mixed={{6,6,3,-1,3,-1,3,3},{13,13,3,-1,13,13,13,3},{15,15,3,-1,15,15,15,15}};
      if(!Arrays.equals(e.state.animalParts,mixed[(form-mixedBase)/2]))
        throw new AssertionError("Progressive mixed-animal fixture lost its independently specified slots");
    }
    for(var entry:poses.entrySet())if(entry.getKey().startsWith("animal_")&&entry.getValue().visible){
      var p=entry.getValue();
      if(e.state.adult&&(p.sx!=1||p.sy!=1||p.sz!=1))
        throw new AssertionError("Adult animal part lost its full authored size: "+entry.getKey());
      if(!e.state.adult&&p.sx==1&&p.sy==1&&p.sz==1)
        throw new AssertionError("Child animal part is not less pronounced: "+entry.getKey());
    }
    if(gene==18 && poses.get("flame").visible!=(mode!=2&&mode!=3))
      throw new AssertionError("Half Fish flame did not replace the marker");
    if(gene==20 && (poses.get("left_foot").visible||poses.get("right_foot").visible))
      throw new AssertionError("Bat did not hide feet");
    e.state.variant="create";model.setupAnim(e,time*.4f,mode==1?1:0,age,yaw,pitch);
    if(!poses.get("head").visible)throw new AssertionError("Gene/themed coat lost its base head");
    if(!poses.get("variant_create_body").visible)throw new AssertionError("Animal gene erased the inherited themed coat");
    e.state.immortal=true;model.setupAnim(e,time*.4f,mode==1?1:0,age,yaw,pitch);
    poses.forEach((name,p)->{
     if(name.startsWith("animal_")&&p.visible)throw new AssertionError("Animal parts overlap Chaos: "+name);
    });
    if(poses.get("flame").visible)throw new AssertionError("Half Fish overrides Chaos marker");
    if(!poses.get("left_arm").visible||!poses.get("right_arm").visible
       ||!poses.get("left_foot").visible||!poses.get("right_foot").visible)
      throw new AssertionError("Chaos did not restore its own limbs after acquired genes");
   }
   if(form>=19&&form<animalBase){
    e.state.immortal=true;model.setupAnim(e,time*.4f,mode==1?1:0,age,yaw,pitch);
    poses.forEach((name,p)->{
     if(name.startsWith("variant_")&&p.visible)
      throw new AssertionError("A themed costume overlaps a Chaos evolution: "+name);
    });
    if(!poses.get("head").visible)
     throw new AssertionError("Chaos evolution lost the main head cube");
    e.state.immortal=false;e.state.variant="unavailable_theme";
    model.setupAnim(e,time*.4f,mode==1?1:0,age,yaw,pitch);
    if(!poses.get("head").visible)
     throw new AssertionError("Dormant heritage lost the main head cube");
    poses.forEach((name,p)->{
     if(name.startsWith("variant_")&&p.visible)
      throw new AssertionError("Dormant heritage retained a themed head: "+name);
    });
   }
  }
  out.close();
 }
}
""".replace("__NAMES__", "new String[]{"+",".join(json.dumps(n) for n in names)+"}") \
    .replace("__THEMES__", "new String[]{"+",".join(json.dumps(n) for n in variants)+"}") \
    .replace("__FORMS__", "new int[]{"+",".join(map(str, selected_forms))+"}")
    paths = []
    for name, content in sources.items():
        path = temporary/name
        path.parent.mkdir(parents=True, exist_ok=True)
        path.write_text(content)
        paths.append(str(path))
    javac, java = java_executable("javac"), java_executable("java")
    if not javac or not java:
        raise RuntimeError("A JDK is required to execute the production animation poses")
    subprocess.run([javac, "-d", str(temporary/"classes"), *paths], check=True, capture_output=True, text=True)
    output = temporary/"poses.bin"
    selected_modes = ",".join(str(ACTIVITIES.index(activity)) for activity in activities)
    subprocess.run([java, "-cp", str(temporary/"classes"), "com.chaogarden.client.PoseProbe", str(output), str(quick).lower(), selected_modes], check=True)
    record = struct.Struct(">5i"+"9f?"*len(names))
    if output.stat().st_size % record.size:
        raise RuntimeError("Incomplete animation probe output")
    # Stream the binary probe: all 67 forms otherwise produce hundreds of MB.
    with output.open("rb") as stream:
        while data := stream.read(record.size * 1024):
            yield from record.iter_unpack(data)


def validate(root, quick=False, activities=ACTIVITIES, form_set="all", theme_ids=None):
    collision_regressions()
    model = json.loads((prototype_path(root)).read_text())
    bones = {bone["name"]: bone for bone in model["bones"]}
    catalog = json.loads((root/"src/main/resources/assets/chao_garden/chao_variants.json").read_text())
    variants = [v["id"] for v in catalog["variants"]]
    forms = (["child"]+[f"{alignment}/{kind}" for alignment in ("Dark", "Neutral", "Hero")
                        for kind in ("Normal", "Swim", "Fly", "Run", "Power")]
             +["Light Chaos", "Angel Chaos", "Devil Chaos"]+variants
             +[prefix+animal for prefix in ("Gene/", "ChildGene/")
               for animal in ("penguin","seal","otter","peacock","parrot","condor","rabbit","cheetah","boar","bear","tiger","gorilla","skunk","sheep","raccoon","dragon","unicorn","phoenix","half_fish","skeleton_dog","bat")]
             +[prefix+combination for combination in ("RabbitPeacock", "SheepPeacock", "DragonPeacock")
               for prefix in ("Mixed/", "ChildMixed/")])
    animal_base = 19+len(variants)
    selected_forms = (list(range(19)) if form_set == "main" else list(range(19, animal_base))
                      if form_set == "themed" else list(range(animal_base, len(forms)))
                      if form_set == "animals" else list(range(len(forms))))
    if theme_ids:
        unknown = set(theme_ids)-set(variants)
        if unknown:
            raise RuntimeError("Unknown requested theme(s): "+", ".join(sorted(unknown)))
        selected_forms = [19+variants.index(theme) for theme in theme_ids]
    names = ["root", "body", "head", "left_arm", "right_arm"]
    # Include authored main-form replacement limbs, even when the base limb is
    # hidden; the renderer deliberately still traverses hidden parents.
    limbs, heads = [], []
    for name, bone in bones.items():
        if (name in ("head", "tuft", "hero_crest", "dark_crest") or name.startswith("crest_")
                or name.startswith("chaos_") and name.endswith("_crest")
                or name.startswith("variant_") and name.endswith("_head")
                or name.startswith("animal_") and name.split("_")[1] in ("1","2","3","5")):
            ancestor = name
            while ancestor:
                if ancestor == "head":
                    heads.append(name)
                    break
                ancestor = bones[ancestor].get("parent")
        ancestor = name
        while ancestor:
            if ancestor in ("left_arm", "right_arm"):
                limbs.append(name)
                break
            ancestor = bones[ancestor].get("parent")
    for name in limbs+heads:
        ancestor = name
        while ancestor:
            if ancestor not in names:
                names.append(ancestor)
            ancestor = bones[ancestor].get("parent")
    checks = samples = expression_checks = 0
    worst = (float("inf"), None)
    failures = []
    with tempfile.TemporaryDirectory(prefix="chao-pose-check-") as temp:
        for record in sample_poses(root, bones, names, Path(temp), variants,
                                   selected_forms, quick, activities):
            case, values = record[:5], record[5:]
            poses = {name: values[i*10:(i+1)*10] for i, name in enumerate(names)}
            # Check the actual adult/child animation transform before the head
            # itself rotates. Child scaling around the head center previously
            # pulled the Otter's cheek whiskers across the outer eye columns.
            # Compare shipped expression geometry, not copied eye dimensions.
            for muzzle_name in ('animal_3_2', 'animal_3_8'):
                if muzzle_name not in poses or not poses[muzzle_name][-1]:
                    continue
                matrix = bone_matrix(bones[muzzle_name], poses[muzzle_name])
                for mesh in bones[muzzle_name]['meshes']:
                    points = [transform(matrix, point) for point in mesh['vertices']]
                    xs = [point[0] for point in points]
                    ys = [point[1] for point in points]
                    for eye in bones['eyes']['meshes']:
                        eye_x = [point[0] for point in eye['vertices']]
                        if not (max(xs) < min(eye_x)-.01 or min(xs) > max(eye_x)+.01):
                            raise RuntimeError(f"Animal muzzle obscures eye after actual adult/child transform: {muzzle_name}/{mesh['name']}")
                        expression_checks += 1
                    mouth_top = min(point[1]+bones['mouth']['pivot'][1]
                                    for mouth in bones['mouth']['meshes'] for point in mouth['vertices'])
                    if max(ys) >= mouth_top-.01:
                        raise RuntimeError(f"Animal muzzle obscures mouth after actual adult/child transform: {muzzle_name}/{mesh['name']}")
                    expression_checks += 1
            matrices = {}
            def world(name):
                if name not in matrices:
                    b = bones[name]
                    local = bone_matrix(b, poses[name])
                    matrices[name] = matmul(world(b["parent"]), local) if b.get("parent") else local
                return matrices[name]
            head_shapes = [(name, mesh["name"], shape(mesh, world(name))) for name in heads if poses[name][-1]
                           for mesh in bones[name].get("meshes", [])]
            for name in limbs:
                if not poses[name][-1]:
                    continue
                for mesh in bones[name].get("meshes", []):
                    if mesh.get("primitive") != "cube":
                        raise RuntimeError(f"Collision check requires a convex cuboid limb: {name}/{mesh['name']}")
                    limb = shape(mesh, world(name))
                    for head_name, head_mesh, head in head_shapes:
                        gap = separation(head, limb)
                        checks += 1
                        if gap < worst[0]:
                            worst = (gap, (*case, name, mesh["name"], head_name, head_mesh))
                        # A tiny tolerance covers vanilla float matrices/table
                        # trigonometry; real model penetration must fail.
                        if gap < -.005 and len(failures) < 8:
                            failures.append((gap, *case, name, mesh["name"], head_name, head_mesh))
            samples += 1
            if len(failures) >= 8:
                break
    modes = ACTIVITIES
    if failures:
        for gap, form, mode, time, yaw, pitch, bone, mesh, head_name, head_mesh in failures:
            print(f"INTERSECTION {forms[form]}/{modes[mode]} age={time*15} yaw={yaw} pitch={pitch} {bone}/{mesh} into {head_name}/{head_mesh}: {-gap:.4f} units")
        raise RuntimeError(f"Head/arm collision: {len(failures)} or more sampled intersections")
    print(f"Animation clearance passed: {samples:,} actual setupAnim poses, {checks:,} conservative convex head/limb SAT checks; minimum separating clearance {worst[0]:.4f} model units.")
    form, mode, time, yaw, pitch, bone, mesh, head_name, head_mesh = worst[1]
    print(f"Closest conservative bound: {forms[form]}/{modes[mode]} age={time*15} yaw={yaw} pitch={pitch} {bone}/{mesh} versus {head_name}/{head_mesh}.")
    print(f"Covers {len(selected_forms)} selected forms ({len(variants)} catalog themes available); activities: "
          +", ".join(activities)+"; input head yaw ±90° and pitch ±60°.")
    print("Animal genes animate their supplied slots; ghost/half-fish effects and Chaos precedence are validated.")
    print(f"Animal muzzle expression clearance passed: {expression_checks:,} actual adult/child eye and mouth checks.")
    return samples, checks, worst


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--root", type=Path, default=ROOT, help="Project root to validate")
    parser.add_argument("--quick", action="store_true", help="Sample known animation extrema across every form before the full sweep")
    parser.add_argument("--activities", nargs="+", choices=ACTIVITIES, default=ACTIVITIES,
                        help="Recheck only changed activities after a localized animation fix")
    parser.add_argument("--forms", choices=("all", "main", "themed", "animals"), default="all",
                        help="Limit the sweep to unchanged main forms or newly authored themes")
    parser.add_argument("--themes", "--theme-id", dest="themes", nargs="+",
                        help="Recheck specific catalog IDs after a localized art fix")
    args = parser.parse_args()
    try:
        validate(args.root.resolve(), args.quick, args.activities, args.forms, args.themes)
    except subprocess.CalledProcessError as error:
        raise SystemExit((error.stderr or str(error)).strip())
    except RuntimeError as error:
        raise SystemExit(str(error))
